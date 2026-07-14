package com.lunaiptv.phone.di

import android.content.Context
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Call

@UnstableApi
class PhoneLivePlayer(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    enum class State { IDLE, LOADING, PLAYING, ERROR }

    private var player: ExoPlayer? = null
    private var surface: Surface? = null

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

    fun setSurface(surface: Surface?) {
        this.surface = surface
        player?.setVideoSurface(surface)
    }

    fun play(url: String, userAgent: String? = null, startPositionMs: Long = 0) {
        val dsFactory = buildDataSourceFactory(userAgent)
        val mediaSource = DefaultMediaSourceFactory(dsFactory)
            .createMediaSource(MediaItem.fromUri(url))

        val exo = getOrCreatePlayer()
        exo.setMediaSource(mediaSource)
        exo.prepare()
        if (startPositionMs > 0) exo.seekTo(startPositionMs)
        exo.playWhenReady = true
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
    }

    fun release() {
        player?.release()
        player = null
        _state.value = State.IDLE
        _isPlaying.value = false
    }

    fun onAppBackgrounded() { player?.playWhenReady = false }
    fun onAppForegrounded() { if (_state.value == State.PLAYING) player?.playWhenReady = true }

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
                    Player.STATE_BUFFERING -> _state.value = State.LOADING
                    Player.STATE_READY -> { _state.value = State.PLAYING; _isPlaying.value = true }
                    Player.STATE_ENDED -> { _state.value = State.IDLE; _isPlaying.value = false }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
            override fun onPlayerError(error: PlaybackException) {
                _state.value = State.ERROR; _isPlaying.value = false
                _error.value = error.message ?: "Playback error"
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
                if (isLoading) _state.value = State.LOADING
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
