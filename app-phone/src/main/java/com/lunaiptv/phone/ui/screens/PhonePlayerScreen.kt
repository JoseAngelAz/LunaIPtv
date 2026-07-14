package com.lunaiptv.phone.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.phone.di.PhoneLivePlayer
import kotlinx.coroutines.delay

@Composable
fun PhonePlayerScreen(
    player: PhoneLivePlayer,
    title: String,
    onBack: () -> Unit,
) {
    val playerState by player.state.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val error by player.error.collectAsStateWithLifecycle()

    var userSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tick by remember { mutableLongStateOf(0L) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (isPlaying && controlsVisible) {
            delay(4000)
            if (System.currentTimeMillis() - lastInteraction >= 3800) {
                controlsVisible = false
            }
        }
    }

    // Periodic tick for seek bar
    LaunchedEffect(isPlaying) {
        while (true) {
            delay(500)
            tick++
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val positionMs = if (userSeeking) seekPosition.toLong() else player.currentPositionMs()
    val durationMs = player.durationMs()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Video surface
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            player.setSurface(holder.surface)
                        }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                            player.setSurface(holder.surface)
                        }
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            player.setSurface(null)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Tap area to toggle controls
        if (!controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        controlsVisible = true
                        lastInteraction = System.currentTimeMillis()
                    },
            )
        }

        // Loading indicator
        if (playerState == PhoneLivePlayer.State.LOADING) {
            Text(
                "Loading…",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Error
        if (playerState == PhoneLivePlayer.State.ERROR) {
            Text(
                error ?: "Playback error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Controls overlay
        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable {
                        lastInteraction = System.currentTimeMillis()
                    },
            ) {
                // Top bar: back + title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.weight(1f))

                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    val dur = if (durationMs > 0) durationMs else 1L
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, dur.toFloat()),
                        onValueChange = { v ->
                            userSeeking = true
                            seekPosition = v
                            lastInteraction = System.currentTimeMillis()
                        },
                        onValueChangeFinished = {
                            player.seekTo(seekPosition.toLong())
                            userSeeking = false
                            lastInteraction = System.currentTimeMillis()
                        },
                        valueRange = 0f..dur.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        IconButton(onClick = {
                            if (isPlaying) player.onAppBackgrounded() else player.onAppForegrounded()
                            lastInteraction = System.currentTimeMillis()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(if (isPlaying) Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)) else Modifier),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun mutableLongStateOf(value: Long) = mutableStateOf(value)

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
