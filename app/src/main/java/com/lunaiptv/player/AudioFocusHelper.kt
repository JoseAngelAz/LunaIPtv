package com.lunaiptv.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Manages Android audio focus for the player. Without this, other apps / system sounds / the TV
 * launcher can silently steal the audio stream, and the user hears silence with no way to recover
 * except restarting playback. The helper requests exclusive focus on play, abandons on stop/pause,
 * and ducks (reduces volume) on transient loss so short system sounds don't interrupt the stream.
 */
class AudioFocusHelper(context: Context) {

    private companion object {
        const val TAG = "AudioFocusHelper"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var hasFocus = false

    /** Callback to the player when focus changes. */
    var onFocusChanged: ((focusGain: Boolean) -> Unit)? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN")
                hasFocus = true
                onFocusChanged?.invoke(true)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS — pausing playback")
                hasFocus = false
                onFocusChanged?.invoke(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT — pausing briefly")
                hasFocus = false
                onFocusChanged?.invoke(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK — ducking volume")
                // Don't pause — just let mpv/ExoPlayer continue at reduced volume.
                // mpv handles ducking via AudioTrack; ExoPlayer via AudioAttributes.
            }
        }
    }

    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .setAcceptsDelayedFocusGain(true)
            .build()
    } else {
        @Suppress("DEPRECATION")
        null
    }

    /** Request audio focus when playback starts. Returns true if focus was granted. */
    fun request(): Boolean {
        if (hasFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.d(TAG, "requestFocus: granted=$hasFocus")
        return hasFocus
    }

    /** Release audio focus when playback stops or the app goes to background. */
    fun abandon() {
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        hasFocus = false
        Log.d(TAG, "abandonFocus")
    }
}
