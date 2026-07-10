package com.lunaiptv.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import com.lunaiptv.R
import com.lunaiptv.ui.components.OwnTVButton
import com.lunaiptv.ui.components.OwnTVButtonStyle
import com.lunaiptv.ui.components.OwnTVIcon
import com.lunaiptv.ui.components.OwnTVSpinner
import com.lunaiptv.ui.components.formatCount
import com.lunaiptv.ui.components.trapAllFocusExit
import com.lunaiptv.ui.theme.OwnTVTheme

/** Semi-automatic EPG flow after a playlist import: ask → sync with a live programme count → done. */
sealed interface EpgSyncUi {
    data object Hidden : EpgSyncUi
    data class Ask(val sourceName: String) : EpgSyncUi
    data class Syncing(val count: Int) : EpgSyncUi
    data object Done : EpgSyncUi
    data class Failed(val message: String) : EpgSyncUi
}

/**
 * After a playlist imports, ask whether to sync its TV guide now (the old behaviour synced it automatically,
 * which was slow). "Sync now" runs in the foreground and shows a **live programme count** — exactly like the
 * playlist import — then a brief "Done", and closes itself.
 */
@Composable
fun EpgSyncDialog(state: EpgSyncUi, onSync: () -> Unit, onDismiss: () -> Unit) {
    if (state is EpgSyncUi.Hidden) return
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(state::class) {
        if (state !is EpgSyncUi.Syncing) {
            delay(50)
            runCatching { focus.requestFocus() }
        }
    }
    BackHandler(enabled = state !is EpgSyncUi.Syncing) { onDismiss() }
    if (state is EpgSyncUi.Done) LaunchedEffect(Unit) { delay(1_800); onDismiss() } // auto-close

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)).trapAllFocusExit().focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(480.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is EpgSyncUi.Ask -> {
                    Text(stringResource(R.string.epg_sync_prompt), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.epg_sync_download_desc, state.sourceName),
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OwnTVButton(stringResource(R.string.epg_not_now), onClick = onDismiss, style = OwnTVButtonStyle.SECONDARY)
                        OwnTVButton(stringResource(R.string.epg_sync_now), onClick = onSync, modifier = Modifier.focusRequester(focus))
                    }
                }
                is EpgSyncUi.Syncing -> {
                    OwnTVSpinner(sizeDp = 48)
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.epg_syncing), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.count > 0) formatCount(state.count) else "Connecting…",
                        style = MaterialTheme.typography.headlineLarge, color = colors.primary,
                    )
                }
                is EpgSyncUi.Done -> {
                    OwnTVIcon(OwnTVIcon.EPG, tint = colors.primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.epg_synced), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    OwnTVButton(stringResource(R.string.done), onClick = onDismiss, modifier = Modifier.focusRequester(focus))
                }
                is EpgSyncUi.Failed -> {
                    Text(stringResource(R.string.epg_sync_failed), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    OwnTVButton(stringResource(R.string.close), onClick = onDismiss, modifier = Modifier.focusRequester(focus))
                }
                EpgSyncUi.Hidden -> Unit
            }
        }
    }
}
