package com.lunaiptv.phone.di

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.Surface
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import okhttp3.OkHttpClient

@UnstableApi
class PhoneLivePlayer(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val TAG = "PhoneLivePlayer"
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

    private var player: ExoPlayer? = null
    private var surface: Surface? = null
    private var lastUrl: String? = null
    private var lastUserAgent: String? = null
    private var lastStartPositionMs: Long = 0L
    private var httpRetried = false
    private val audio: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _videoSize = MutableStateFlow<Pair<Int, Int>?>(null)
    val videoSize: StateFlow<Pair<Int, Int>?> = _videoSize.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _zoomMode = MutableStateFlow(ZoomMode.FIT)
    val zoomMode: StateFlow<ZoomMode> = _zoomMode.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    private val _audioTrackCount = MutableStateFlow(0)
    val audioTrackCount: StateFlow<Int> = _audioTrackCount.asStateFlow()

    private val _subtitleTrackCount = MutableStateFlow(0)
    val subtitleTrackCount: StateFlow<Int> = _subtitleTrackCount.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrack>> = _subtitleTracks.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isLiveContent = MutableStateFlow(false)
    val isLiveContent: StateFlow<Boolean> = _isLiveContent.asStateFlow()

    private val _cues = MutableStateFlow(CueGroup.EMPTY_TIME_ZERO)
    val cues: StateFlow<CueGroup> = _cues.asStateFlow()

    fun getCurrentCues(): CueGroup = _cues.value

    private var selectedAudioIndex: Int = -1
    private var selectedSubtitleIndex: Int = -1

    fun setZoomMode(mode: ZoomMode) { _zoomMode.value = mode }
    fun cycleZoom() {
        val modes = ZoomMode.entries
        val idx = modes.indexOf(_zoomMode.value)
        _zoomMode.value = modes[(idx + 1) % modes.size]
    }

    fun setSpeed(s: Float) {
        _speed.value = s
        player?.setPlaybackSpeed(s)
    }
    fun cycleSpeed() {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = speeds.indexOf(_speed.value).coerceAtLeast(0)
        setSpeed(speeds[(idx + 1) % speeds.size])
    }

    fun setVolume(v: Int) {
        val clamped = v.coerceIn(0, 150)
        _volume.value = clamped
        _isMuted.value = false
        player?.volume = clamped / 100f
    }
    fun adjustVolume(delta: Int) { setVolume(_volume.value + delta) }
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        player?.volume = if (_isMuted.value) 0f else _volume.value / 100f
    }

    fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceIn(0, p.duration.coerceAtLeast(0))
        p.seekTo(target)
    }

    fun selectAudioTrack(index: Int) {
        val p = player ?: return
        val groups = p.currentTracks.groups.withIndex()
            .filter { it.value.type == C.TRACK_TYPE_AUDIO }
        val target = groups.getOrNull(index) ?: return
        selectedAudioIndex = index
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(target.value.mediaTrackGroup, listOf(0))
            )
            .build()
        _audioTracks.value = _audioTracks.value.mapIndexed { i, t -> t.copy(isSelected = i == index) }
    }

    fun selectSubtitleTrack(index: Int) {
        val p = player ?: return
        val groups = p.currentTracks.groups.withIndex()
            .filter { it.value.type == C.TRACK_TYPE_TEXT }
        val target = groups.getOrNull(index) ?: return
        selectedSubtitleIndex = index
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(target.value.mediaTrackGroup, listOf(0))
            )
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        _subtitleTracks.value = _subtitleTracks.value.mapIndexed { i, t -> t.copy(isSelected = i == index) }
    }

    fun disableSubtitles() {
        player?.trackSelectionParameters = player?.trackSelectionParameters?.buildUpon()
            ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            ?.build() ?: return
    }

    fun retry() {
        val url = lastUrl ?: return
        httpRetried = false
        play(url, lastUserAgent, lastStartPositionMs)
    }

    fun setSurface(surface: Surface?) {
        this.surface = surface
        player?.setVideoSurface(surface)
    }

    fun play(url: String, userAgent: String? = null, startPositionMs: Long = 0, isLive: Boolean = false) {
        lastUrl = url
        lastUserAgent = userAgent
        lastStartPositionMs = startPositionMs
        httpRetried = false
        _isLiveContent.value = isLive
        selectedAudioIndex = -1
        selectedSubtitleIndex = -1
        internalPlay(url, userAgent, startPositionMs)
    }

    private fun internalPlay(url: String, userAgent: String?, startPositionMs: Long) {
        val dsFactory = buildDataSourceFactory(userAgent)
        val mediaSource = DefaultMediaSourceFactory(dsFactory)
            .createMediaSource(MediaItem.fromUri(url))

        val exo = getOrCreatePlayer()
        exo.setMediaSource(mediaSource)
        exo.prepare()
        if (startPositionMs > 0) exo.seekTo(startPositionMs)
        exo.playWhenReady = true
        if (_speed.value != 1.0f) exo.setPlaybackSpeed(_speed.value)
        _state.value = State.LOADING
    }

    fun seekTo(positionMs: Long) { player?.seekTo(positionMs) }
    fun currentPositionMs(): Long = player?.currentPosition ?: 0L
    fun durationMs(): Long {
        val d = player?.duration ?: 0L
        return if (d == C.TIME_UNSET) 0L else d
    }

    fun stop() {
        player?.let { p -> p.stop(); p.clearMediaItems() }
        _state.value = State.IDLE
        _isPlaying.value = false
        _error.value = null
        _videoSize.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
        _buffering.value = false
    }

    fun release() {
        player?.release()
        player = null
        _state.value = State.IDLE
        _isPlaying.value = false
    }

    fun onAppBackgrounded() { player?.playWhenReady = false }
    fun onAppForegrounded() { if (_state.value == State.PLAYING) player?.playWhenReady = true }

    fun pause() { player?.playWhenReady = false }
    fun resume() { player?.playWhenReady = true }

    private fun buildDataSourceFactory(userAgent: String?): OkHttpDataSource.Factory {
        val client = if (userAgent != null) {
            okHttpClient.newBuilder().addInterceptor { chain ->
                val req = chain.request().newBuilder().header("User-Agent", userAgent).build()
                chain.proceed(req)
            }.build()
        } else okHttpClient
        return OkHttpDataSource.Factory(client)
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        player?.let { return it }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(5000, 15000, 1000, 3000)
            .build()
        val exo = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()

        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> { _state.value = State.LOADING; _buffering.value = true }
                    Player.STATE_READY -> { _state.value = State.PLAYING; _isPlaying.value = true; _buffering.value = false }
                    Player.STATE_ENDED -> { _state.value = State.IDLE; _isPlaying.value = false; _buffering.value = false }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
            override fun onPlayerError(error: PlaybackException) {
                val msg = error.message ?: "Playback error"
                Log.e(TAG, "Playback error: $msg", error)

                // Auto-retry: if HTTPS fails, retry as HTTP (many IPTV providers use broken TLS)
                val url = lastUrl
                if (!httpRetried && url != null && url.startsWith("https://")) {
                    val httpUrl = url.replaceFirst("https://", "http://")
                    Log.w(TAG, "Retrying as HTTP: $httpUrl")
                    httpRetried = true
                    _state.value = State.LOADING
                    internalPlay(httpUrl, lastUserAgent, lastStartPositionMs)
                    return
                }

                _state.value = State.ERROR; _isPlaying.value = false
                _error.value = msg
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) _videoSize.value = videoSize.width to videoSize.height
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) { updatePosition() }
            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (isLoading) { _state.value = State.LOADING; _buffering.value = true }
            }
            override fun onTracksChanged(tracks: Tracks) {
                val audioGroups = tracks.groups.withIndex().filter { it.value.type == C.TRACK_TYPE_AUDIO }
                val subGroups = tracks.groups.withIndex().filter { it.value.type == C.TRACK_TYPE_TEXT }
                _audioTrackCount.value = audioGroups.size
                _subtitleTrackCount.value = subGroups.size
                _audioTracks.value = audioGroups.mapIndexed { listIdx, (trackIdx, g) ->
                    val lang = g.getTrackFormat(0).language
                    val label = if (!lang.isNullOrEmpty()) {
                        try { Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() } } catch (_: Exception) { "Audio ${listIdx + 1}" }
                    } else "Audio ${listIdx + 1}"
                    val isSel = if (selectedAudioIndex >= 0) listIdx == selectedAudioIndex else g.isSelected
                    AudioTrack(trackIdx, label, isSel)
                }
                _subtitleTracks.value = subGroups.mapIndexed { listIdx, (trackIdx, g) ->
                    val lang = g.getTrackFormat(0).language
                    val label = if (!lang.isNullOrEmpty()) {
                        try { Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() } } catch (_: Exception) { "Subtitle ${listIdx + 1}" }
                    } else "Subtitle ${listIdx + 1}"
                    val isSel = if (selectedSubtitleIndex >= 0) listIdx == selectedSubtitleIndex else g.isSelected
                    SubtitleTrack(trackIdx, label, isSel)
                }
            }
            override fun onCues(cueGroup: CueGroup) {
                _cues.value = cueGroup
            }
            override fun onEvents(player: Player, events: Player.Events) {
                updatePosition()
            }
        })

        surface?.let { exo.setVideoSurface(it) }
        player = exo
        return exo
    }

    private fun updatePosition() {
        val p = player ?: return
        _positionMs.value = p.currentPosition
        val d = p.duration
        _durationMs.value = if (d == C.TIME_UNSET) 0L else d
    }
}
