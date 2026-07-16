package com.lunaiptv.phone.ui.screens

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.core.backup.BackupManager
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneBackupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneBackupScreen(
    vm: PhoneBackupViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showExportPicker by remember { mutableStateOf(false) }
    var exportSections by remember { mutableStateOf(BackupManager.Section.entries.toSet()) }
    var pendingFolderUri by remember { mutableStateOf<Uri?>(null) }
    var showPasswordForExport by remember { mutableStateOf(false) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    // ── SAF launchers ──────────────────────────────────────────────
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            pendingFolderUri = uri
            showPasswordForExport = true
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val cached = uri.copyToCache(context, "lunaiptv-restore.json")
            if (cached != null) vm.inspect(cached)
        }
    }

    // After export completes, copy the backup JSON from cache to the SAF tree URI
    val currentState = state
    LaunchedEffect(currentState, lastExportedFile, pendingFolderUri) {
        if (currentState is PhoneBackupViewModel.State.Done && lastExportedFile != null && pendingFolderUri != null) {
            val file = lastExportedFile!!
            val treeUri = pendingFolderUri!!
            withContext(Dispatchers.IO) {
                file.copyToSafTree(context, treeUri)
            }
            lastExportedFile = null
            pendingFolderUri = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.backup_restore_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.backup_description_long),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showExportPicker = true },
                        enabled = state !is PhoneBackupViewModel.State.Working,
                    ) {
                        Text(stringResource(R.string.export))
                    }
                    OutlinedButton(
                        onClick = {
                            filePicker.launch(arrayOf("application/json"))
                        },
                        enabled = state !is PhoneBackupViewModel.State.Working,
                    ) {
                        Text(stringResource(R.string.restore_action))
                    }
                }
                Spacer(Modifier.height(24.dp))

                // ── Status ──────────────────────────────────────────
                when (val s = state) {
                    is PhoneBackupViewModel.State.Working -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.working), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is PhoneBackupViewModel.State.Done -> {
                        Text(s.message, color = MaterialTheme.colorScheme.primary)
                    }
                    is PhoneBackupViewModel.State.Error -> {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                    else -> Unit
                }
            }
        }
    }

    // ── Export: section picker ──────────────────────────────────────
    if (showExportPicker) {
        SectionPickerDialog(
            title = stringResource(R.string.what_to_back_up),
            sections = BackupManager.Section.entries,
            initial = BackupManager.Section.entries.toSet(),
            confirmLabel = stringResource(R.string.choose_folder),
            onConfirm = { chosen ->
                exportSections = chosen
                showExportPicker = false
                folderPicker.launch(null)
            },
            onDismiss = { showExportPicker = false },
        )
    }

    // ── Export: password protection ─────────────────────────────────
    if (showPasswordForExport && pendingFolderUri != null) {
        val exportFolder = File(context.cacheDir, "lunaiptv-export").also { it.mkdirs() }
        BackupPasswordDialog(
            title = stringResource(R.string.protect_password),
            message = stringResource(R.string.encrypt_backup_message),
            confirmLabel = stringResource(R.string.encrypt_export),
            skipLabel = stringResource(R.string.export_without_passwords),
            onConfirm = { pass ->
                showPasswordForExport = false
                lastExportedFile = File(exportFolder, "LunaIPtv-backup.json")
                vm.export(exportFolder, exportSections, pass)
            },
            onSkip = {
                showPasswordForExport = false
                lastExportedFile = File(exportFolder, "LunaIPtv-backup.json")
                vm.export(exportFolder, exportSections, null)
            },
            onDismiss = {
                pendingFolderUri = null
                showPasswordForExport = false
            },
        )
    }

    // ── Restore: section picker ─────────────────────────────────────
    (state as? PhoneBackupViewModel.State.ChooseRestore)?.let { choose ->
        SectionPickerDialog(
            title = stringResource(R.string.what_to_restore),
            sections = BackupManager.Section.entries.filter { it in choose.available },
            initial = choose.available,
            confirmLabel = stringResource(R.string.restore_action),
            onConfirm = { chosen -> vm.beginImport(choose.file, chosen, choose.encrypted) },
            onDismiss = { vm.reset() },
        )
    }

    // ── Restore: password prompt ────────────────────────────────────
    (state as? PhoneBackupViewModel.State.NeedPassword)?.let { need ->
        BackupPasswordDialog(
            title = stringResource(if (need.retry) R.string.wrong_password else R.string.enter_backup_password),
            message = stringResource(if (need.retry) R.string.wrong_password_retry_message else R.string.encrypted_backup_message),
            confirmLabel = stringResource(R.string.restore_action),
            skipLabel = stringResource(R.string.skip_no_passwords),
            onConfirm = { pass -> vm.import(need.file, need.sections, pass) },
            onSkip = { vm.import(need.file, need.sections, null) },
            onDismiss = { vm.reset() },
        )
    }
}

// ── Section Picker Dialog ──────────────────────────────────────────────

@Composable
private fun SectionPickerDialog(
    title: String,
    sections: List<BackupManager.Section>,
    initial: Set<BackupManager.Section>,
    confirmLabel: String,
    onConfirm: (Set<BackupManager.Section>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    val everything = selected.size == sections.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Everything toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = everything,
                        onCheckedChange = { checked ->
                            selected = if (checked) sections.toSet() else emptySet()
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.everything), style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
                sections.forEach { section ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = section in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + section else selected - section
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(section.label, style = MaterialTheme.typography.bodyLarge)
                            Text(section.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotEmpty(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ── Password Dialog ────────────────────────────────────────────────────

@Composable
private fun BackupPasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    skipLabel: String,
    onConfirm: (String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text(skipLabel) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

// ── SAF helpers ────────────────────────────────────────────────────────

private fun Uri.copyToCache(context: android.content.Context, fileName: String): File? {
    return try {
        val cacheFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(this)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }
        cacheFile
    } catch (_: Exception) {
        null
    }
}

private fun File.copyToSafTree(context: android.content.Context, treeUri: Uri) {
    val docUri = DocumentsContract.createDocument(
        context.contentResolver,
        treeUri,
        "application/json",
        name,
    ) ?: return
    context.contentResolver.openOutputStream(docUri)?.use { output ->
        inputStream().use { input -> input.copyTo(output) }
    }
}
