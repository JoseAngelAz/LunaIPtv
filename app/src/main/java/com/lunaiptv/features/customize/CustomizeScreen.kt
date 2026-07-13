package com.lunaiptv.features.customize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.R
import com.lunaiptv.features.profiles.PinDialog
import com.lunaiptv.ui.components.FocusableSurface
import com.lunaiptv.ui.components.LunaIPtvButton
import com.lunaiptv.ui.components.LunaIPtvButtonStyle
import com.lunaiptv.ui.components.TextInputDialog
import com.lunaiptv.ui.components.roundedPanel
import com.lunaiptv.ui.theme.LunaIPtvTheme

/**
 * Settings ? Customize & Hidden Items: hide / rename / reorder categories per section, and unhide
 * hidden channels, movies and series. Everything is per-profile and survives source re-syncs.
 * Optionally locked behind a PIN (set from this screen's top-right) so hidden items can't be
 * unhidden by someone else — the PIN is asked on every entry.
 */
@Composable
fun CustomizeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: CustomizeViewModel = koinViewModel()
    val section by vm.section.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val hiddenChannels by vm.hiddenChannels.collectAsStateWithLifecycle()
    val rangeAnchorKey by vm.rangeAnchorKey.collectAsStateWithLifecycle()
    val pinLock by vm.pinLock.collectAsStateWithLifecycle()
    val colors = LunaIPtvTheme.colors
    var renaming by remember { mutableStateOf<CustomizeCatRow?>(null) }
    // The category whose Hide button was clicked to close a range — opens the Show/Hide/Cancel prompt.
    var rangeEnd by remember { mutableStateOf<CustomizeCatRow?>(null) }
    // PIN gate: asked on every entry (state is per-composition, so leaving the screen re-locks it).
    var unlocked by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }
    // Set/Change/Remove PIN flow (only reachable once unlocked).
    var editingPin by remember { mutableStateOf<PinEdit?>(null) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPinStage by remember { mutableStateOf(false) }
    var pinMismatch by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }

    // Wait for the stored lock state before showing anything (no unlocked flash).
    if (!pinLock.loaded) {
        Column(modifier.fillMaxSize().roundedPanel()) {}
        return
    }
    if (pinLock.pin != null && !unlocked) {
        Column(
            modifier = modifier.fillMaxSize().roundedPanel().padding(horizontal = 40.dp, vertical = 28.dp),
        ) {
            Text(stringResource(R.string.customize_title), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.customize_locked),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        PinDialog(
            title = if (pinError) stringResource(R.string.profile_wrong_pin) else stringResource(R.string.profile_enter_pin),
            onSubmit = { entered ->
                if (entered == pinLock.pin) {
                    unlocked = true
                    pinError = false
                } else {
                    pinError = true
                }
            },
            onDismiss = onBack,
        )
        return
    }

    LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { firstFocus.requestFocus() } }

    // While a span selection is in progress, Back cancels the selection instead of leaving the screen.
    BackHandler { if (rangeAnchorKey != null) vm.cancelRange() else onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel()
            // Spatial D-pad entry from the sidebar would land mid-list — route it to the first chip.
            // onEnter fires only for directional entry from outside (internal moves don't re-trigger it).
            .focusProperties { onEnter = { runCatching { firstFocus.requestFocus() } } }
            .focusGroup()
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.customize_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            // Optional PIN lock on this screen, controlled from here. Per-profile and NOT exported in
            // backups. Only reachable once unlocked — to be here with a PIN set the user already
            // entered it, so changing or removing it needs no further verification.
            if (pinLock.pin == null) {
                LunaIPtvButton(
                    stringResource(R.string.customize_set_pin),
                    onClick = { firstPin = ""; confirmPinStage = false; pinMismatch = false; editingPin = PinEdit.SET },
                )
            } else {
                LunaIPtvButton(
                    stringResource(R.string.customize_change_pin),
                    onClick = { firstPin = ""; confirmPinStage = false; pinMismatch = false; editingPin = PinEdit.CHANGE },
                    style = LunaIPtvButtonStyle.SECONDARY,
                )
                Spacer(Modifier.width(10.dp))
                LunaIPtvButton(
                    stringResource(R.string.customize_remove_lock),
                    onClick = { editingPin = PinEdit.REMOVE },
                    style = LunaIPtvButtonStyle.SECONDARY,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.customize_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // Section picker
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionChip(stringResource(R.string.source_live_tv), section == MediaType.LIVE, Modifier.focusRequester(firstFocus)) { vm.selectSection(MediaType.LIVE) }
            SectionChip(stringResource(R.string.source_movies), section == MediaType.MOVIE) { vm.selectSection(MediaType.MOVIE) }
            SectionChip(stringResource(R.string.source_series), section == MediaType.SERIES) { vm.selectSection(MediaType.SERIES) }
        }
        Spacer(Modifier.height(16.dp))

        if (rangeAnchorKey != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Span selection started. Press Show/Hide on the end item to select the span.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                LunaIPtvButton(stringResource(R.string.cancel), onClick = { vm.cancelRange() }, style = LunaIPtvButtonStyle.SECONDARY)
            }
            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            // Hidden items of this section first (hidden via each section's long-press menu) — kept on
            // top so they're findable even when a provider has hundreds of categories below.
            if (hiddenChannels.isNotEmpty()) {
                item {
                    Text(
                        when (section) {
                            MediaType.LIVE -> stringResource(R.string.customize_hidden_channels)
                            MediaType.MOVIE -> stringResource(R.string.customize_hidden_movies)
                            else -> stringResource(R.string.customize_hidden_series)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.customize_unhide_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(hiddenChannels.entries.sortedBy { it.value.lowercase() }, key = { "hid:${it.key}" }) { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label.ifBlank { key },
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        LunaIPtvButton(stringResource(R.string.customize_unhide), onClick = { vm.unhideChannel(key) }, style = LunaIPtvButtonStyle.SECONDARY)
                    }
                }
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.customize_categories), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.customize_no_categories),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(rows, key = { it.key }) { row ->
                CategoryRow(
                    row = row,
                    inRangeMode = rangeAnchorKey != null,
                    isAnchor = row.key == rangeAnchorKey,
                    onMoveUp = { vm.move(row, up = true) },
                    onMoveDown = { vm.move(row, up = false) },
                    onMoveTop = { vm.moveToEdge(row, top = true) },
                    onMoveBottom = { vm.moveToEdge(row, top = false) },
                    onRename = { renaming = row },
                    onToggleHidden = { vm.setCategoryHidden(row, !row.hidden) },
                    onHideLongPress = { vm.beginRange(row) },
                    onPickRangeEnd = { if (row.key == rangeAnchorKey) vm.cancelRange() else rangeEnd = row },
                )
            }
        }
    }

    renaming?.let { row ->
        TextInputDialog(
            title = stringResource(R.string.customize_rename_category),
            initial = row.displayName,
            hint = stringResource(R.string.customize_rename_hint, row.originalName),
            onConfirm = { vm.renameCategory(row, it.takeIf { t -> t.isNotBlank() }); renaming = null },
            onDismiss = { renaming = null },
        )
    }

    rangeEnd?.let { row ->
        val count = vm.keysInRange(row)?.size ?: 0
        RangeHideDialog(
            count = count,
            onHide = { vm.applyRange(row, hidden = true); rangeEnd = null },
            onShow = { vm.applyRange(row, hidden = false); rangeEnd = null },
            onDismiss = { vm.cancelRange(); rangeEnd = null },
        )
    }

    editingPin?.let { mode ->
        when (mode) {
            PinEdit.REMOVE -> PinConfirmDialog(
                title = stringResource(R.string.customize_remove_pin_title),
                message = stringResource(R.string.customize_remove_pin_desc),
                confirmLabel = stringResource(R.string.customize_remove),
                onConfirm = { vm.setPin(null); editingPin = null },
                onDismiss = { editingPin = null },
            )
            // SET and CHANGE are the same flow (enter a new PIN, then confirm). To reach here with a
            // PIN already set the user unlocked the screen, so neither verifies the old PIN.
            PinEdit.SET, PinEdit.CHANGE -> {
                if (confirmPinStage) {
                    key("confirm", pinMismatch) {
                        PinDialog(
                            title = if (pinMismatch) stringResource(R.string.customize_pin_mismatch) else stringResource(R.string.customize_confirm_pin),
                            onSubmit = { entered ->
                                if (entered == firstPin) {
                                    vm.setPin(entered); editingPin = null
                                } else {
                                    pinMismatch = true
                                }
                            },
                            onDismiss = { editingPin = null },
                        )
                    }
                } else {
                    key("first") {
                        PinDialog(
                            title = stringResource(R.string.customize_new_pin),
                            onSubmit = { entered ->
                                firstPin = entered
                                confirmPinStage = true
                                pinMismatch = false
                            },
                            onDismiss = { editingPin = null },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirms a range select: hide or show every category in the chosen span (or cancel). [count] is
 * the number of categories the span covers, inclusive.
 */
@Composable
private fun RangeHideDialog(count: Int, onHide: () -> Unit, onShow: () -> Unit, onDismiss: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    val hideFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { hideFocus.requestFocus() } }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(480.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
        ) {
            Text(stringResource(R.string.customize_hide_show), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.customize_categories_selected, count),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaIPtvButton(stringResource(R.string.cancel), onClick = onDismiss, style = LunaIPtvButtonStyle.SECONDARY)
                Spacer(Modifier.weight(1f))
                LunaIPtvButton(stringResource(R.string.show), onClick = onShow, style = LunaIPtvButtonStyle.SECONDARY)
                LunaIPtvButton(stringResource(R.string.hide), onClick = onHide, modifier = Modifier.focusRequester(hideFocus))
            }
        }
    }
}

@Composable
private fun SectionChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    FocusableSurface(
        onClick = onClick,
        selected = selected,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        selectedContainerColor = colors.primaryContainer,
        contentAlignment = Alignment.Center,
    ) { focused ->
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = when {
                selected -> colors.onPrimaryContainer
                focused -> colors.primary
                else -> colors.onSurface
            },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CategoryRow(
    row: CustomizeCatRow,
    inRangeMode: Boolean,
    isAnchor: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    onRename: () -> Unit,
    onToggleHidden: () -> Unit,
    onHideLongPress: () -> Unit,
    onPickRangeEnd: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    val wasLabel = stringResource(R.string.customize_was)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            // Tint the anchor while a range is in progress so its starting point is obvious.
            .background(if (isAnchor) colors.primaryContainer else colors.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (row.hidden) colors.onSurfaceVariant else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.hidden || row.renamed) {
                Text(
                    buildString {
                        if (row.hidden) append("Hidden")
                        if (row.renamed) {
                            if (row.hidden) append("  ·  ")
                            append(wasLabel)
                            append(" \u201c${row.originalName}\u201d")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        LunaIPtvButton("\u2912", onClick = onMoveTop, style = LunaIPtvButtonStyle.SECONDARY)
        Spacer(Modifier.width(6.dp))
        LunaIPtvButton("\u2191", onClick = onMoveUp, style = LunaIPtvButtonStyle.SECONDARY)
        Spacer(Modifier.width(6.dp))
        LunaIPtvButton("\u2193", onClick = onMoveDown, style = LunaIPtvButtonStyle.SECONDARY)
        Spacer(Modifier.width(6.dp))
        LunaIPtvButton("\u2913", onClick = onMoveBottom, style = LunaIPtvButtonStyle.SECONDARY)
        Spacer(Modifier.width(6.dp))
        LunaIPtvButton(stringResource(R.string.customize_rename), onClick = onRename, style = LunaIPtvButtonStyle.SECONDARY)
        Spacer(Modifier.width(6.dp))
        LunaIPtvButton(
            label = if (row.hidden) stringResource(R.string.show) else stringResource(R.string.hide),
            // Long-press anchors a range; a normal press picks the span end while a range is active,
            // otherwise it toggles just this category.
            onClick = { if (inRangeMode) onPickRangeEnd() else onToggleHidden() },
            onLongClick = onHideLongPress,
            style = LunaIPtvButtonStyle.SECONDARY,
        )
    }
}

/** PIN lock editing flow opened from the Customize header. */
private enum class PinEdit { SET, CHANGE, REMOVE }

/**
 * Generic Yes/No confirmation scrim used to remove the Customize PIN lock. Mirrors [RangeHideDialog]'s
 * structure so D-pad focus and the back button behave the same way as the other scrim dialogs here.
 */
@Composable
private fun PinConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(480.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(28.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaIPtvButton(stringResource(R.string.cancel), onClick = onDismiss, style = LunaIPtvButtonStyle.SECONDARY)
                Spacer(Modifier.weight(1f))
                LunaIPtvButton(confirmLabel, onClick = onConfirm, modifier = Modifier.focusRequester(confirmFocus))
            }
        }
    }
}
