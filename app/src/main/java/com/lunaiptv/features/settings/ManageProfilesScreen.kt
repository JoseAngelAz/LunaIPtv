package com.lunaiptv.features.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lunaiptv.R
import com.lunaiptv.core.database.entity.ProfileEntity
import com.lunaiptv.features.profiles.ProfileEditorDialog
import com.lunaiptv.features.profiles.ProfilesViewModel
import com.lunaiptv.ui.components.OwnTVAvatar
import com.lunaiptv.ui.components.OwnTVButton
import com.lunaiptv.ui.components.OwnTVButtonStyle
import com.lunaiptv.ui.components.OwnTVIcon
import com.lunaiptv.ui.components.roundedPanel
import com.lunaiptv.ui.theme.OwnTVTheme

/** Phase 13 — create / edit / delete viewer profiles. */
@Composable
fun ManageProfilesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: ProfilesViewModel = koinViewModel()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val colors = OwnTVTheme.colors

    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ProfileEntity?>(null) }
    val addFocus = remember { FocusRequester() }
    LaunchedEffect(editing, creating, confirmDelete) {
        if (editing == null && !creating && confirmDelete == null) {
            kotlinx.coroutines.delay(50) // let the screen lay out after the tab swap before grabbing focus
            runCatching { addFocus.requestFocus() }
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel()
            // Spatial D-pad entry from the sidebar would land mid-list — route it to "Add Profile".
            // onEnter fires only for directional entry from outside (internal moves don't re-trigger it).
            .focusProperties { onEnter = { runCatching { addFocus.requestFocus() } } }
            .focusGroup()
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
            Spacer(Modifier.weight(1f))
            OwnTVButton(stringResource(R.string.profiles_add), onClick = { creating = true }, icon = OwnTVIcon.ADD, modifier = Modifier.focusRequester(addFocus))
        }
        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(profiles, key = { it.id }) { p ->
                ProfileRow(
                    profile = p,
                    canDelete = profiles.size > 1,
                    onEdit = { editing = p },
                    onDelete = { confirmDelete = p },
                )
            }
        }
    }

    if (creating) {
        ProfileEditorDialog(
            initial = null,
            onConfirm = { name, avatarId, isKids, pin -> vm.create(name, avatarId, isKids, pin); creating = false },
            onDismiss = { creating = false },
        )
    }
    editing?.let { p ->
        ProfileEditorDialog(
            initial = p,
            onConfirm = { name, avatarId, isKids, pin -> vm.edit(p, name, avatarId, isKids, pin); editing = null },
            onDismiss = { editing = null },
        )
    }
    confirmDelete?.let { p ->
        ConfirmDialog(
            title = stringResource(R.string.profiles_delete_confirm, p.name),
            message = stringResource(R.string.profiles_delete_desc),
            onConfirm = { vm.delete(p); confirmDelete = null },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun ProfileRow(profile: ProfileEntity, canDelete: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = OwnTVTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerHigh).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OwnTVAvatar(avatarId = profile.avatarId, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            val tags = buildList {
                if (profile.isKids) add(stringResource(R.string.profile_kids))
                if (profile.pinHash != null) add(stringResource(R.string.profiles_pin_locked))
            }
            if (tags.isNotEmpty()) {
                Text(tags.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        OwnTVButton(stringResource(R.string.edit), onClick = onEdit, style = OwnTVButtonStyle.SECONDARY)
        if (canDelete) {
            Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(R.string.delete), onClick = onDelete, style = OwnTVButtonStyle.SECONDARY)
        }
    }
}
