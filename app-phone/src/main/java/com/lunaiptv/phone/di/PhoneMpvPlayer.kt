package com.lunaiptv.phone.di

import android.content.Context
import android.util.Log
import android.view.Surface
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.Locale

class PhoneMpvPlayer(
    private val context: Context,
    @Suppress("UNUSED") private val okHttpClient: OkHttpClient,
) : MPVLib.EventObserver {

    companion object {
        private const val TAG = "PhoneMpvPlayer"
    }

    enum class State { IDLE, LOADING, PLAYING, ERROR }

    enum class ZoomMode(val label: String) {
        FIT("Fit"),
        CROP("Fill"),
        STRETCH("Stretch"),
        ORIGINAL("Original"),
        FORCE_16_9("16:9"),
        FORCE_4_3("4:3"),
    }

    data class AudioTrack(val index: Int, val label: String, val isSelected: Boolean)
    data class SubtitleTrack(val index: Int, val label: String, val isSelected: Boolean)

    private var mpv: MPVLib? = null
    private var initialized = false
    private var surface: Surface? = null
    private var lastUrl: String? = null
    private var lastUserAgent: String? = null
    private var lastStartPositionMs: Long = 0L
    private var httpRetried = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _videoSize = MutableStateFlow<Pair<Int, Int>?>(null)
    val videoSize: StateFlow<Pair<Int, Int>?> = _videoSize.asStateFlow()

    private val _zoomMode = MutableStateFlow(ZoomMode.FIT)
    val zoomMode: StateFlow<ZoomMode> = _zoomMode.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrack>> = _subtitleTracks.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isLiveContent = MutableStateFlow(false)
    val isLiveContent: StateFlow<Boolean> = _isLiveContent.asStateFlow()

    private val _cues = MutableStateFlow(androidx.media3.common.text.CueGroup.EMPTY_TIME_ZERO)
    val cues: StateFlow<androidx.media3.common.text.CueGroup> = _cues.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    fun getCurrentCues(): androidx.media3.common.text.CueGroup = _cues.value

    private fun mpvAsync(block: MPVLib.() -> Unit) {
        val m = mpv ?: return
        try { m.block() } catch (t: Throwable) { Log.e(TAG, "mpvAsync error", t) }
    }

    private fun ensureInit() {
        if (initialized) return
        mpv = MPVLib.create(context)?.apply {
            setOptionString("vo", "gpu")
            setOptionString("gpu-context", "android")
            setOptionString("hwdec", "no")
            setOptionString("ao", "audiotrack")
            setOptionString("audio-channels", "stereo")
            setOptionString("force-window", "no")
            setOptionString("idle", "yes")
            setOptionString("ytdl", "no")
            setOptionString("volume-max", "150")
            setOptionString("af", "dynaudnorm=f=20:g=31:p=0.9")
            setOptionString("framedrop", "decoder+vo")
            setOptionString("msg-level", "all=warn")
            setOptionString("cache", "yes")
            setOptionString("demuxer-max-bytes", "50MiB")
            setOptionString("demuxer-max-back-bytes", "25MiB")
            setOptionString("network-timeout", "60")
            setOptionString("stream-lavf-o", "reconnect=1,reconnect_streamed=1,reconnect_delay_max=8,reconnect_on_http_error=5xx")
            setOptionString("user-agent", "LunaIPtv/1.0.0")
            setOptionString("sub-scale-with-window", "yes")
            init()
            observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            observeProperty("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            observeProperty("width", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            observeProperty("height", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            observeProperty("speed", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            addObserver(this@PhoneMpvPlayer)
            Log.i(TAG, "mpv initialized")
        }
        initialized = true
    }

    fun setSurface(surface: Surface?) {
        this.surface = surface
        if (initialized && surface != null) {
            mpvAsync { attachSurface(surface) }
        }
    }

    fun play(url: String, userAgent: String? = null, startPositionMs: Long = 0, isLive: Boolean = false) {
        lastUrl = url
        lastUserAgent = userAgent
        lastStartPositionMs = startPositionMs
        httpRetried = false
        _isLiveContent.value = isLive
        ensureInit()
        _state.value = State.LOADING
        mpvAsync {
            surface?.let { attachSurface(it) }
            if (userAgent != null) setPropertyString("user-agent", userAgent)
            command(arrayOf("loadfile", url))
            if (startPositionMs > 0) {
                command(arrayOf("seek", (startPositionMs / 1000.0).toString(), "absolute"))
            }
        }
    }

    fun stop() {
        mpvAsync { command(arrayOf("stop")) }
        _state.value = State.IDLE
        _isPlaying.value = false
        _error.value = null
        _videoSize.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
        _buffering.value = false
    }

    fun release() {
        mpv?.let { m ->
            m.removeObserver(this)
            m.destroy()
        }
        mpv = null
        initialized = false
        scope.cancel()
        _state.value = State.IDLE
        _isPlaying.value = false
    }

    fun pause() { mpvAsync { setPropertyString("pause", "yes") } }
    fun resume() { mpvAsync { setPropertyString("pause", "no") } }

    fun seekBy(deltaMs: Long) {
        mpvAsync {
            val cur = _positionMs.value / 1000.0
            val target = (cur + deltaMs / 1000.0).coerceAtLeast(0.0)
            command(arrayOf("seek", target.toString(), "absolute"))
        }
    }

    fun seekTo(positionMs: Long) {
        mpvAsync { command(arrayOf("seek", (positionMs / 1000.0).toString(), "absolute")) }
    }

    fun currentPositionMs(): Long = _positionMs.value
    fun durationMs(): Long = _durationMs.value

    fun setSpeed(s: Float) {
        _speed.value = s
        mpvAsync { setPropertyString("speed", s.toString()) }
    }

    fun cycleSpeed() {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = speeds.indexOf(_speed.value).coerceAtLeast(0)
        setSpeed(speeds[(idx + 1) % speeds.size])
    }

    fun setZoomMode(mode: ZoomMode) { _zoomMode.value = mode }
    fun cycleZoom() {
        val modes = ZoomMode.entries
        val idx = modes.indexOf(_zoomMode.value)
        _zoomMode.value = modes[(idx + 1) % modes.size]
    }

    fun setVolume(v: Int) {
        val clamped = v.coerceIn(0, 150)
        _volume.value = clamped
        _isMuted.value = false
        mpvAsync { setPropertyDouble("volume", clamped.toDouble()) }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        mpvAsync { setPropertyBoolean("mute", _isMuted.value) }
    }

    fun selectAudioTrack(index: Int) {
        mpvAsync { setPropertyInt("aid", index + 1) }
        _audioTracks.value = _audioTracks.value.mapIndexed { i, t -> t.copy(isSelected = i == index) }
    }

    fun selectSubtitleTrack(index: Int) {
        mpvAsync { setPropertyInt("sid", index + 1) }
        _subtitleTracks.value = _subtitleTracks.value.mapIndexed { i, t -> t.copy(isSelected = i == index) }
    }

    fun disableSubtitles() {
        mpvAsync { setPropertyInt("sid", 0) }
        _subtitleTracks.value = _subtitleTracks.value.map { it.copy(isSelected = false) }
    }

    fun retry() {
        val url = lastUrl ?: return
        httpRetried = false
        play(url, lastUserAgent, lastStartPositionMs, _isLiveContent.value)
    }

    fun onAppBackgrounded() { mpvAsync { setPropertyString("pause", "yes") } }
    fun onAppForegrounded() { if (_state.value == State.PLAYING) mpvAsync { setPropertyString("pause", "no") } }

    // ── MPVLib.EventObserver ────────────────────────────────────────────

    override fun event(eventId: Int) {
        when (eventId) {
            MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                _state.value = State.PLAYING
                _isPlaying.value = true
                _buffering.value = false
                scope.launch { updateTrackLists() }
                val w = try { mpv?.getPropertyInt("width") } catch (_: Exception) { null }
                val h = try { mpv?.getPropertyInt("height") } catch (_: Exception) { null }
                if (w != null && h != null && w > 0 && h > 0) _videoSize.value = w to h
            }
            MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                _state.value = State.IDLE
                _isPlaying.value = false
                _buffering.value = false
            }
        }
    }

    override fun eventProperty(property: String) {
        // Property became null/unavailable
    }

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "time-pos" -> _positionMs.value = value * 1000
            "duration" -> _durationMs.value = value * 1000
            "width" -> {
                val h = _videoSize.value?.second ?: return
                if (value > 0) _videoSize.value = value.toInt() to h
            }
            "height" -> {
                val w = _videoSize.value?.first ?: 0
                if (value > 0) _videoSize.value = w to value.toInt()
            }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> _isPlaying.value = !value
            "paused-for-cache" -> _buffering.value = value
        }
    }

    override fun eventProperty(property: String, value: Double) {
        if (property == "speed") _speed.value = value.toFloat()
    }

    override fun eventProperty(property: String, value: String) {}

    private fun updateTrackLists() {
        mpvAsync {
            val audioList = mutableListOf<AudioTrack>()
            var i = 1
            while (true) {
                val lang = try { getPropertyString("track-list/$i/lang") } catch (_: Exception) { null }
                val type = try { getPropertyString("track-list/$i/type") } catch (_: Exception) { null }
                if (type == null) break
                if (type == "audio") {
                    val label = if (!lang.isNullOrEmpty()) {
                        try { Locale.forLanguageTag(lang).displayLanguage.replaceFirstChar { c -> c.uppercase() } } catch (_: Exception) { "Audio ${audioList.size + 1}" }
                    } else "Audio ${audioList.size + 1}"
                    val selected = try { getPropertyInt("aid") == i } catch (_: Exception) { false }
                    audioList.add(AudioTrack(i - 1, label, selected))
                }
                i++
            }
            _audioTracks.value = audioList

            val subList = mutableListOf<SubtitleTrack>()
            i = 1
            while (true) {
                val lang = try { getPropertyString("track-list/$i/lang") } catch (_: Exception) { null }
                val type = try { getPropertyString("track-list/$i/type") } catch (_: Exception) { null }
                if (type == null) break
                if (type == "sub") {
                    val label = if (!lang.isNullOrEmpty()) {
                        try { Locale.forLanguageTag(lang).displayLanguage.replaceFirstChar { c -> c.uppercase() } } catch (_: Exception) { "Subtitle ${subList.size + 1}" }
                    } else "Subtitle ${subList.size + 1}"
                    val selected = try { getPropertyInt("sid") == i } catch (_: Exception) { false }
                    subList.add(SubtitleTrack(i - 1, label, selected))
                }
                i++
            }
            _subtitleTracks.value = subList
        }
    }
}
