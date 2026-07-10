package com.lunaiptv.features.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lunaiptv.R
import com.lunaiptv.core.database.entity.SourceEntity
import com.lunaiptv.core.sync.importProgressDisplay
import com.lunaiptv.features.profiles.ProfileEditorDialog
import com.lunaiptv.ui.components.BrowseMode
import com.lunaiptv.ui.components.FocusableSurface
import com.lunaiptv.ui.components.LunaIPtvButton
import com.lunaiptv.ui.components.LunaIPtvButtonStyle
import com.lunaiptv.ui.components.LunaIPtvTextField
import com.lunaiptv.ui.components.LunaIPtvIcon
import com.lunaiptv.ui.components.LunaIPtvSpinner
import com.lunaiptv.features.settings.EpgSyncDialog
import com.lunaiptv.ui.components.StorageBrowser
import com.lunaiptv.ui.theme.LunaIPtvTheme

private enum class Step { WELCOME, DISCLAIMER, SETUP_CHOICE, CREATE_PROFILE, ADD_CONTENT, ADD_SOURCE, IMPORTING, EXISTING, IMPORT_BACKUP }

/**
 * Onboarding for one profile. [firstRun] shows the welcome/disclaimer; otherwise it starts at profile
 * creation (used by "Add profile"). [onDone] enters the app; [onCancel] backs out (to the gate).
 */
@Composable
fun Onboarding(firstRun: Boolean, onDone: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val vm: SetupViewModel = koinViewModel()
    var step by remember { mutableStateOf(if (firstRun) Step.WELCOME else Step.CREATE_PROFILE) }
    val importState by vm.state.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val epgSync by vm.epgSync.collectAsStateWithLifecycle()
    var existing by remember { mutableStateOf<List<SourceEntity>>(emptyList()) }
    // Where "Try Again" returns to when an import fails (new source vs. linking existing).
    var importOrigin by remember { mutableStateOf(Step.ADD_SOURCE) }
    // Where Back from the backup-restore picker returns to (first-run choice vs. add-content step).
    var backupOrigin by remember { mutableStateOf(Step.ADD_CONTENT) }

    // Refresh the "existing playlists" availability whenever we land on the add-content step.
    LaunchedEffect(step) { if (step == Step.ADD_CONTENT) existing = runCatching { vm.availableExistingSources() }.getOrDefault(emptyList()) }

    Box(modifier = modifier.fillMaxSize().background(LunaIPtvTheme.colors.background)) {
        when (step) {
            Step.WELCOME -> WelcomeScreen(onNext = { step = Step.DISCLAIMER })
            Step.DISCLAIMER -> DisclaimerScreen(onAgree = { step = Step.SETUP_CHOICE }, onBack = { step = Step.WELCOME })
            // First decision: start fresh or bring everything back from a backup (profiles included —
            // no point creating a profile first that the restore would replace).
            Step.SETUP_CHOICE -> SetupChoiceScreen(
                onCreate = { step = Step.CREATE_PROFILE },
                onRestore = { backupOrigin = Step.SETUP_CHOICE; step = Step.IMPORT_BACKUP },
                onBack = { step = Step.DISCLAIMER },
            )
            Step.CREATE_PROFILE -> ProfileEditorDialog(
                initial = null,
                onConfirm = { name, avatar, kids, pin -> vm.createProfile(name, avatar, kids, pin) { step = Step.ADD_CONTENT } },
                onDismiss = { if (firstRun) step = Step.SETUP_CHOICE else onCancel() },
            )
            Step.ADD_CONTENT -> AddContentScreen(
                hasExisting = existing.isNotEmpty(),
                onNew = { step = Step.ADD_SOURCE },
                onExisting = { step = Step.EXISTING },
                onImport = { backupOrigin = Step.ADD_CONTENT; step = Step.IMPORT_BACKUP },
                onSkip = { vm.finish(onDone) },
            )
            Step.ADD_SOURCE -> AddSourceScreen(
                onStartXtream = { name, server, user, pass, ua, epg, refresh, live, movies, series, _ ->
                    vm.startXtream(name, server, user, pass, ua, epg, refresh, live, movies, series)
                    importOrigin = Step.ADD_SOURCE
                    step = Step.IMPORTING
                },
                onStartM3u = { name, url, ua, epg, refresh, _ -> vm.startM3u(name, url, ua, epg, refresh); importOrigin = Step.ADD_SOURCE; step = Step.IMPORTING },
                onBack = { step = Step.ADD_CONTENT },
                initial = vm.lastFailedSource, // pre-fill on retry after failed import
                showDefaultToggle = false, // first playlist in setup: nothing to be "default" over yet
            )
            Step.IMPORTING -> ImportProgressScreen(
                state = importState,
                progress = progress,
                onContinue = { vm.finish(onDone) }, // playlist + its EPG synced (auto)
                onRetry = { vm.reset(); step = importOrigin },
                onCancel = { vm.cancelImport(); step = importOrigin },
            )
            Step.EXISTING -> ExistingSourcesScreen(
                sources = existing,
                onAdd = { ids -> vm.linkExisting(ids); importOrigin = Step.EXISTING; step = Step.IMPORTING },
                onBack = { step = Step.ADD_CONTENT },
            )
            Step.IMPORT_BACKUP -> ImportBackupScreen(
                state = importState,
                onPick = { file -> vm.importBackup(file) { onDone() } }, // restore activates a profile itself
                onPassword = { file, pass -> vm.restoreWithPassword(file, pass) { onDone() } },
                onBack = { vm.reset(); step = backupOrigin },
            )
        }
        // Semi-auto EPG: after the first playlist imports, ask ? sync (live count) ? done (overlays "All set!").
        EpgSyncDialog(state = epgSync, onSync = vm::syncPendingEpg, onDismiss = vm::dismissPendingEpg)
    }
}

@Composable
private fun WelcomeScreen(onNext: () -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    Centered {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "LunaIPtv",
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.setup_welcome_desc), style = MaterialTheme.typography.titleMedium, color = LunaIPtvTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))
        LunaIPtvButton(stringResource(R.string.setup_get_started), onClick = onNext, icon = LunaIPtvIcon.PLAY, modifier = Modifier.focusRequester(fr))
    }
}

@Composable
private fun DisclaimerScreen(onAgree: () -> Unit, onBack: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    Centered {
        Text(stringResource(R.string.setup_before_start), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.setup_disclaimer_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp),
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LunaIPtvButton(stringResource(R.string.back), onClick = onBack, style = LunaIPtvButtonStyle.SECONDARY)
            LunaIPtvButton(stringResource(R.string.setup_i_understand), onClick = onAgree, modifier = Modifier.focusRequester(fr))
        }
    }
}

@Composable
private fun SetupChoiceScreen(onCreate: () -> Unit, onRestore: () -> Unit, onBack: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    BackHandler { onBack() }
    Centered {
        Text(stringResource(R.string.setup_own_tv), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.setup_choice_desc),
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChoiceCard(icon = LunaIPtvIcon.PERSON, title = stringResource(R.string.setup_new_profile), desc = stringResource(R.string.setup_new_profile_desc), modifier = Modifier.focusRequester(fr), onClick = onCreate)
            ChoiceCard(icon = LunaIPtvIcon.DOWNLOADS, title = stringResource(R.string.setup_restore_backup), desc = stringResource(R.string.setup_restore_backup_desc), onClick = onRestore)
        }
    }
}

@Composable
private fun AddContentScreen(hasExisting: Boolean, onNew: () -> Unit, onExisting: () -> Unit, onImport: () -> Unit, onSkip: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    Centered {
        Text(stringResource(R.string.setup_add_playlist), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.setup_add_playlist_desc), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChoiceCard(icon = LunaIPtvIcon.ADD, title = stringResource(R.string.setup_new), desc = stringResource(R.string.setup_new_desc), modifier = Modifier.focusRequester(fr), onClick = onNew)
            if (hasExisting) {
                ChoiceCard(icon = LunaIPtvIcon.PLAYLIST, title = stringResource(R.string.setup_existing), desc = stringResource(R.string.setup_existing_playlists_desc), onClick = onExisting)
            }
            ChoiceCard(icon = LunaIPtvIcon.DOWNLOADS, title = stringResource(R.string.setup_import), desc = stringResource(R.string.setup_import_desc), onClick = onImport)
        }
        Spacer(Modifier.height(24.dp))
        LunaIPtvButton(stringResource(R.string.setup_skip_for_now), onClick = onSkip, style = LunaIPtvButtonStyle.SECONDARY)
    }
}

@Composable
private fun ExistingSourcesScreen(sources: List<SourceEntity>, onAdd: (Set<Long>) -> Unit, onBack: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 620.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.setup_existing_playlists), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.setup_existing_playlists_desc), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.id }) { src ->
                    val checked = src.id in selected
                    FocusableSurface(
                        onClick = { selected = if (checked) selected - src.id else selected + src.id },
                        modifier = if (src.id == sources.firstOrNull()?.id) Modifier.fillMaxWidth().focusRequester(fr) else Modifier.fillMaxWidth(),
                        selected = checked,
                        shape = RoundedCornerShape(12.dp),
                        selectedContainerColor = colors.primaryContainer,
                        contentAlignment = Alignment.CenterStart,
                    ) { _ ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(src.name, style = MaterialTheme.typography.titleMedium, color = if (checked) colors.onPrimaryContainer else colors.onSurface)
                                Text(src.url, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
                            }
                            if (checked) LunaIPtvIcon(LunaIPtvIcon.STAR, tint = colors.onPrimaryContainer, filled = true, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaIPtvButton(stringResource(R.string.back), onClick = onBack, style = LunaIPtvButtonStyle.SECONDARY)
                LunaIPtvButton(stringResource(R.string.setup_add_count, selected.size), onClick = { onAdd(selected) }, enabled = selected.isNotEmpty())
            }
        }
    }
}

@Composable
private fun ImportBackupScreen(
    state: SetupViewModel.ImportState,
    onPick: (java.io.File) -> Unit,
    onPassword: (java.io.File, String?) -> Unit,
    onBack: () -> Unit,
) {
    when (state) {
        SetupViewModel.ImportState.Idle -> Centered {
            LunaIPtvSpinner(sizeDp = 56); Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.setup_restoring), style = MaterialTheme.typography.titleMedium, color = LunaIPtvTheme.colors.onSurface)
        }
        SetupViewModel.ImportState.Running -> Centered {
            LunaIPtvSpinner(sizeDp = 56); Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.setup_restoring), style = MaterialTheme.typography.titleMedium, color = LunaIPtvTheme.colors.onSurface)
        }
        is SetupViewModel.ImportState.NeedPassword -> Centered {
            var password by remember { mutableStateOf("") }
            val firstFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
            Text(
                if (state.retry) stringResource(R.string.setup_backup_wrong_password) else stringResource(R.string.setup_backup_enter_password),
                style = MaterialTheme.typography.headlineLarge, color = LunaIPtvTheme.colors.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.retry) stringResource(R.string.setup_backup_wrong_desc)
                else stringResource(R.string.setup_backup_encrypted_desc),
                style = MaterialTheme.typography.bodyMedium, color = LunaIPtvTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 520.dp),
            )
            Spacer(Modifier.height(20.dp))
            LunaIPtvTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.setup_backup_password),
                isPassword = true,
                focusRequester = firstFocus,
                modifier = Modifier.widthIn(max = 420.dp),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LunaIPtvButton(stringResource(R.string.back), onClick = onBack, style = LunaIPtvButtonStyle.SECONDARY)
                LunaIPtvButton(stringResource(R.string.setup_skip_no_passwords), onClick = { onPassword(state.file, null) }, style = LunaIPtvButtonStyle.SECONDARY)
                LunaIPtvButton(stringResource(R.string.setup_restore_button), onClick = { onPassword(state.file, password) }, enabled = password.isNotBlank())
            }
        }
        is SetupViewModel.ImportState.Failed -> Centered {
            Text(stringResource(R.string.setup_restore_failed), style = MaterialTheme.typography.headlineLarge, color = LunaIPtvTheme.colors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = LunaIPtvTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 520.dp))
            Spacer(Modifier.height(20.dp))
            LunaIPtvButton(stringResource(R.string.back), onClick = onBack)
            StorageBrowser(
                title = stringResource(R.string.setup_pick_backup),
                mode = BrowseMode.FILE,
                fileExtensions = setOf("json"),
                onPick = onPick,
                onDismiss = onBack,
            )
        }
        is SetupViewModel.ImportState.Success -> Centered {
            Text(stringResource(R.string.setup_all_set), style = MaterialTheme.typography.headlineLarge, color = LunaIPtvTheme.colors.onSurface)
        }
    }
}

@Composable
private fun ChoiceCard(icon: LunaIPtvIcon, title: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LunaIPtvTheme.colors
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.size(width = 220.dp, height = 170.dp),
        shape = RoundedCornerShape(22.dp),
        focusedContainerColor = colors.surfaceContainerHighest,
        unfocusedContainerColor = colors.surfaceContainerHigh,
        selectedContainerColor = colors.surfaceContainerHigh,
        contentAlignment = Alignment.Center,
    ) { focused ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                LunaIPtvIcon(icon, tint = colors.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = if (focused) colors.primary else colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ImportProgressScreen(
    state: SetupViewModel.ImportState,
    progress: com.lunaiptv.core.sync.ImportStage?,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LunaIPtvTheme.colors
    val context = LocalContext.current
    val fr = remember { FocusRequester() }
    LaunchedEffect(state) {
        if (state is SetupViewModel.ImportState.Success || state is SetupViewModel.ImportState.Failed) runCatching { fr.requestFocus() }
    }
    BackHandler(enabled = state is SetupViewModel.ImportState.Running || state is SetupViewModel.ImportState.Idle) { onCancel() }
    Centered {
        when (state) {
            SetupViewModel.ImportState.Running, SetupViewModel.ImportState.Idle,
            is SetupViewModel.ImportState.NeedPassword -> {
                val display = progress?.importProgressDisplay(context)
                LunaIPtvSpinner(sizeDp = 56)
                Spacer(Modifier.height(20.dp))
                Text(display?.title ?: stringResource(R.string.setup_importing_catalog), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    display?.primaryText ?: stringResource(R.string.setup_preparing_catalog),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    display?.detail ?: stringResource(R.string.setup_preparing_catalog),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                LunaIPtvButton(stringResource(R.string.cancel), onClick = onCancel, style = LunaIPtvButtonStyle.SECONDARY)
            }
            is SetupViewModel.ImportState.Success -> {
                Text(stringResource(R.string.setup_all_set), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                Spacer(Modifier.height(10.dp))
                Text(state.summary, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 560.dp))
                Spacer(Modifier.height(28.dp))
                LunaIPtvButton(stringResource(R.string.setup_continue), onClick = onContinue, icon = LunaIPtvIcon.PLAY, modifier = Modifier.focusRequester(fr))
            }
            is SetupViewModel.ImportState.Failed -> {
                Text(stringResource(R.string.setup_import_failed), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                Spacer(Modifier.height(10.dp))
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 520.dp))
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LunaIPtvButton(stringResource(R.string.back), onClick = onCancel, style = LunaIPtvButtonStyle.SECONDARY)
                    LunaIPtvButton(stringResource(R.string.try_again), onClick = onRetry, modifier = Modifier.focusRequester(fr))
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}
