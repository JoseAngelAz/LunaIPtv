package com.lunaiptv.phone.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.SurfaceHolder.Callback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import androidx.compose.ui.res.stringResource
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneLivePlayer
import kotlinx.coroutines.delay

private val TEAL = Color(0xFF52DBC8)

@UnstableApi
@Composable
fun PhonePlayerScreen(
    player: PhoneLivePlayer,
    title: String,
    url: String? = null,
    isLive: Boolean = false,
    userAgent: String? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Force landscape while player is visible
    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (original != null) activity.requestedOrientation = original
        }
    }

    val playerState by player.state.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val error by player.error.collectAsStateWithLifecycle()
    val buffering by player.buffering.collectAsStateWithLifecycle()
    val zoomMode by player.zoomMode.collectAsStateWithLifecycle()
    val speed by player.speed.collectAsStateWithLifecycle()
    val volume by player.volume.collectAsStateWithLifecycle()
    val isMuted by player.isMuted.collectAsStateWithLifecycle()
    val videoSize by player.videoSize.collectAsStateWithLifecycle()
    val audioTracks by player.audioTracks.collectAsStateWithLifecycle()
    val subtitleTracks by player.subtitleTracks.collectAsStateWithLifecycle()
    val isLiveContent by player.isLiveContent.collectAsStateWithLifecycle()
    val cueGroup by player.cues.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tick by remember { mutableLongStateOf(0L) }
    var userSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var showDialog by remember { mutableStateOf<DialogType?>(null) }

    BackHandler {
        if (showDialog != null) showDialog = null
        else if (controlsVisible) controlsVisible = false
        else onBack()
    }

    // Auto-hide
    LaunchedEffect(controlsVisible, isPlaying, showDialog) {
        if (isPlaying && controlsVisible && showDialog == null) {
            delay(5000)
            if (System.currentTimeMillis() - lastInteraction >= 4800 && showDialog == null) {
                controlsVisible = false
            }
        }
    }

    // Position tick
    LaunchedEffect(isPlaying) {
        while (true) { delay(500); tick++ }
    }

    // Auto-play when URL provided
    LaunchedEffect(url) {
        if (url != null && url.isNotBlank() && player.state.value != PhoneLivePlayer.State.PLAYING) {
            player.play(url, userAgent = userAgent, isLive = isLive)
        }
    }

    DisposableEffect(Unit) { onDispose { player.stop() } }

    val positionMs = if (userSeeking) seekPosition.toLong() else player.currentPositionMs()
    val durationMs = player.durationMs()

    val (scaleX, scaleY) = when (zoomMode) {
        PhoneLivePlayer.ZoomMode.FIT -> 1f to 1f
        PhoneLivePlayer.ZoomMode.CROP -> 1.25f to 1.25f
        PhoneLivePlayer.ZoomMode.STRETCH -> {
            val (vw, vh) = videoSize ?: (16 to 9)
            val aspectRatio = vw.toFloat() / vh.toFloat().coerceAtLeast(1f)
            val screenAspect = 16f / 9f
            (aspectRatio / screenAspect) to 1f
        }
        PhoneLivePlayer.ZoomMode.ORIGINAL -> 1f to 1f
        PhoneLivePlayer.ZoomMode.FORCE_16_9 -> 1f to (9f / 16f * (videoSize?.first?.toFloat()?.div(videoSize?.second?.toFloat()?.coerceAtLeast(1f) ?: 1f) ?: 1f))
        PhoneLivePlayer.ZoomMode.FORCE_4_3 -> 1f to 1f
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures {
                if (!controlsVisible) {
                    controlsVisible = true
                    lastInteraction = System.currentTimeMillis()
                } else if (showDialog == null) {
                    controlsVisible = false
                }
            }
        }
    ) {
        // Video surface + subtitle view
        AndroidView(
            factory = { ctx ->
                val container = FrameLayout(ctx)
                val surfaceView = SurfaceView(ctx).apply {
                    isClickable = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    holder.addCallback(object : Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) { player.setSurface(holder.surface) }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) { player.setSurface(holder.surface) }
                        override fun surfaceDestroyed(holder: SurfaceHolder) { player.setSurface(null) }
                    })
                }
                val subtitleView = SubtitleView(ctx).apply {
                    setPadding(0, 0, 0, 0)
                }
                container.addView(surfaceView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                container.addView(subtitleView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                container.tag = subtitleView
                container
            },
            modifier = Modifier.fillMaxSize(),
            update = { container ->
                val sv = container.getChildAt(0) as? SurfaceView
                sv?.scaleX = scaleX
                sv?.scaleY = scaleY
                val subView = container.tag as? SubtitleView
                if (subView != null) {
                    @Suppress("UnstableApi")
                    subView.setCues(cueGroup.cues)
                }
            },
        )

        // Gradients
        if (controlsVisible) {
            Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(200.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
        }

        // Loading
        if (buffering && controlsVisible) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        // Error
        if (playerState == PhoneLivePlayer.State.ERROR) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.playback_error), style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { player.retry() }) {
                    Text(stringResource(R.string.retry), color = TEAL, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (controlsVisible) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                }
                if (isLiveContent) {
                    Row(
                        Modifier.padding(end = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color.Red).padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.live), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Center controls
            Row(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.7f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { player.seekBy(-10_000) }) {
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("-10", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = {
                    if (isPlaying) player.pause() else player.resume()
                    lastInteraction = System.currentTimeMillis()
                }, modifier = Modifier.size(64.dp).background(
                    if (isPlaying) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(32.dp)
                )) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (isPlaying) stringResource(R.string.paused) else stringResource(R.string.play), tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { player.seekBy(10_000) }) {
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+10", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom: seekbar + controls
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (!isLiveContent && durationMs > 0) {
                    val dur = durationMs.toFloat()
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, dur),
                        onValueChange = { v -> userSeeking = true; seekPosition = v; lastInteraction = System.currentTimeMillis() },
                        onValueChangeFinished = { player.seekTo(seekPosition.toLong()); userSeeking = false; lastInteraction = System.currentTimeMillis() },
                        valueRange = 0f..dur,
                        colors = SliderDefaults.colors(
                            thumbColor = TEAL, activeTrackColor = TEAL,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(positionMs), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlChip(
                        label = if (isMuted || volume == 0) stringResource(R.string.muted) else "${volume}%",
                        active = isMuted,
                        onClick = { showDialog = DialogType.VOLUME; lastInteraction = System.currentTimeMillis() },
                    )
                    ControlChip(
                        label = if (speed == 1.0f) "1\u00d7" else "${speed}\u00d7",
                        active = speed != 1.0f,
                        onClick = { showDialog = DialogType.SPEED; lastInteraction = System.currentTimeMillis() },
                    )
                    if (subtitleTracks.isNotEmpty()) {
                        ControlChip(
                            label = "CC",
                            badge = subtitleTracks.size,
                            active = subtitleTracks.any { it.isSelected },
                            onClick = { showDialog = DialogType.SUBS; lastInteraction = System.currentTimeMillis() },
                        )
                    }
                    if (audioTracks.size > 1) {
                        ControlChip(
                            label = stringResource(R.string.audio_track),
                            badge = audioTracks.size,
                            onClick = { showDialog = DialogType.AUDIO; lastInteraction = System.currentTimeMillis() },
                        )
                    }
                    ControlChip(
                        label = zoomMode.label,
                        active = zoomMode != PhoneLivePlayer.ZoomMode.FIT,
                        onClick = { showDialog = DialogType.ZOOM; lastInteraction = System.currentTimeMillis() },
                    )
                }
            }
        }

        // Dialogs
        when (showDialog) {
            DialogType.SPEED -> SpeedDialog(speed = speed, onSelect = { player.setSpeed(it) }, onDismiss = { showDialog = null })
            DialogType.ZOOM -> ZoomDialog(mode = zoomMode, onSelect = { player.setZoomMode(it) }, onDismiss = { showDialog = null })
            DialogType.VOLUME -> VolumeDialog(volume = volume, isMuted = isMuted, onSetVolume = { player.setVolume(it) }, onToggleMute = { player.toggleMute() }, onDismiss = { showDialog = null })
            DialogType.AUDIO -> AudioTrackDialog(tracks = audioTracks, onSelect = { player.selectAudioTrack(it) }, onDismiss = { showDialog = null })
            DialogType.SUBS -> SubtitleTrackDialog(tracks = subtitleTracks, onSelect = { player.selectSubtitleTrack(it) }, onOff = { player.disableSubtitles() }, onDismiss = { showDialog = null })
            null -> {}
        }
    }
}

private enum class DialogType { SPEED, ZOOM, VOLUME, AUDIO, SUBS }

@Composable
private fun ControlChip(
    label: String,
    badge: Int = 0,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (active) TEAL.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)
    val fg = if (active) TEAL else Color.White
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
        if (badge > 0) {
            Box(
                Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(TEAL),
                contentAlignment = Alignment.Center,
            ) {
                Text("$badge", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpeedDialog(speed: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.speed)) },
        text = {
            LazyColumn {
                items(speeds) { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(s); onDismiss() }.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (s == speed) Icon(Icons.Filled.Check, null, tint = TEAL, modifier = Modifier.size(20.dp))
                        else Spacer(Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (s == 1.0f) stringResource(R.string.normal) else "${s}\u00d7",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (s == speed) TEAL else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (s == speed) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ZoomDialog(mode: PhoneLivePlayer.ZoomMode, onSelect: (PhoneLivePlayer.ZoomMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.zoom)) },
        text = {
            LazyColumn {
                items(PhoneLivePlayer.ZoomMode.entries) { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(m); onDismiss() }.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (m == mode) Icon(Icons.Filled.Check, null, tint = TEAL, modifier = Modifier.size(20.dp))
                        else Spacer(Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            m.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (m == mode) TEAL else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (m == mode) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun VolumeDialog(volume: Int, isMuted: Boolean, onSetVolume: (Int) -> Unit, onToggleMute: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.volume)) },
        text = {
            Column {
                Text("${if (isMuted) stringResource(R.string.muted) else "$volume%"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = (if (isMuted) 0 else volume).toFloat(),
                    onValueChange = { onSetVolume(it.toInt()) },
                    valueRange = 0f..150f,
                    colors = SliderDefaults.colors(thumbColor = TEAL, activeTrackColor = TEAL),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { onSetVolume((volume - 5).coerceAtLeast(0)) }) {
                        Text("\u22125%", color = TEAL)
                    }
                    TextButton(onClick = onToggleMute) {
                        Text(if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute), color = TEAL)
                    }
                    TextButton(onClick = { onSetVolume((volume + 5).coerceAtMost(150)) }) {
                        Text("+5%", color = TEAL)
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun AudioTrackDialog(tracks: List<PhoneLivePlayer.AudioTrack>, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audio_track)) },
        text = {
            if (tracks.isEmpty()) {
                Text(stringResource(R.string.no_audio_tracks), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(tracks) { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(t.index); onDismiss() }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (t.isSelected) Icon(Icons.Filled.Check, null, tint = TEAL, modifier = Modifier.size(20.dp))
                            else Spacer(Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(t.label, style = MaterialTheme.typography.bodyLarge, color = if (t.isSelected) TEAL else MaterialTheme.colorScheme.onSurface, fontWeight = if (t.isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun SubtitleTrackDialog(tracks: List<PhoneLivePlayer.SubtitleTrack>, onSelect: (Int) -> Unit, onOff: () -> Unit, onDismiss: () -> Unit) {
    val anySelected = tracks.any { it.isSelected }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subtitles)) },
        text = {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOff(); onDismiss() }.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!anySelected) Icon(Icons.Filled.Check, null, tint = TEAL, modifier = Modifier.size(20.dp))
                        else Spacer(Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.subtitle_off), style = MaterialTheme.typography.bodyLarge, color = if (!anySelected) TEAL else MaterialTheme.colorScheme.onSurface, fontWeight = if (!anySelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                items(tracks) { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(t.index); onDismiss() }.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (t.isSelected) Icon(Icons.Filled.Check, null, tint = TEAL, modifier = Modifier.size(20.dp))
                        else Spacer(Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(t.label, style = MaterialTheme.typography.bodyLarge, color = if (t.isSelected) TEAL else MaterialTheme.colorScheme.onSurface, fontWeight = if (t.isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {},
    )
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
