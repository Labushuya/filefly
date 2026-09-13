package de.filefly.feature.upload

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.filefly.common.ConflictStrategy

// Kern-Screen: Zielverzeichnis wählen (respektiert base_path + Rechte) und die
// vorgemerkten Medien gechunked hochladen. Zeigt Fortschritt pro Datei, Konflikt-
// Dialog mit "für alle", und Fehler pro Datei.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showMkdir by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Upload nach ${state.currentPath.ifEmpty { "/" }}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (state.role.canMkdir && state.writable) {
                        IconButton(onClick = { showMkdir = true }) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = "Ordner anlegen")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.items.isNotEmpty() && state.role.canUpload && !state.uploading) {
                FloatingActionButton(onClick = viewModel::uploadAll) {
                    Icon(Icons.Filled.UploadFile, contentDescription = "Hochladen")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Verzeichnis-Navigation
            if (state.currentPath != state.basePath) {
                TextButton(onClick = viewModel::navigateUp) { Text("⬆ Eine Ebene höher") }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        "Ordner",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                items(state.entries) { dir ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateInto(dir) }
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                        Text(dir.name)
                    }
                }

                item {
                    Text(
                        "Dateien (${state.items.size})",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(state.items) { progress ->
                    UploadItemRow(progress)
                }
            }

            state.error?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }
        }
    }

    // Konflikt-Dialog
    state.pendingConflict?.let { conflict ->
        ConflictDialog(
            fileName = conflict.item.displayName,
            onResolve = conflict.onResolve,
        )
    }

    // Ordner-anlegen-Dialog
    if (showMkdir) {
        MkdirDialog(
            onConfirm = { name ->
                viewModel.createDirectory(name)
                showMkdir = false
            },
            onDismiss = { showMkdir = false },
        )
    }
}

@Composable
private fun UploadItemRow(progress: UploadProgress) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(progress.item.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(statusLabel(progress), style = MaterialTheme.typography.labelSmall)
        }
        if (progress.status == UploadProgress.Status.UPLOADING) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
        progress.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun statusLabel(p: UploadProgress): String =
    when (p.status) {
        UploadProgress.Status.PENDING -> "wartet"
        UploadProgress.Status.UPLOADING -> "${(p.fraction * 100).toInt()} %"
        UploadProgress.Status.SUCCESS -> "✓ ${p.serverStatus ?: "fertig"}"
        UploadProgress.Status.SKIPPED -> "übersprungen"
        UploadProgress.Status.FAILED -> "✗ Fehler"
    }

@Composable
private fun ConflictDialog(
    fileName: String,
    onResolve: (ConflictStrategy, Boolean) -> Unit,
) {
    var applyToAll by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onResolve(ConflictStrategy.SKIP, applyToAll) },
        title = { Text("Datei existiert bereits") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("„$fileName“ existiert schon im Zielverzeichnis. Was tun?")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    Text("Für alle weiteren Konflikte übernehmen")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onResolve(ConflictStrategy.OVERWRITE, applyToAll) }) {
                Text("Überschreiben")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onResolve(ConflictStrategy.RENAME, applyToAll) }) {
                    Text("Umbenennen")
                }
                TextButton(onClick = { onResolve(ConflictStrategy.SKIP, applyToAll) }) {
                    Text("Überspringen")
                }
            }
        },
    )
}

@Composable
private fun MkdirDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Ordner") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ordnername") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Anlegen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
