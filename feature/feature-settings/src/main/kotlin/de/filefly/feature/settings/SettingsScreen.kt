package de.filefly.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.filefly.core.data.SettingsStore

// Einstellungs-Screen (Tab-Root). Server-URL, Chunk-Größe, Verbindung testen,
// Rolle/Rechte-Info, Update-Check und Abmelden.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenUpdates: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Einstellungen") }) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Server ---
            Text("Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                label = { Text("Server-URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::testConnection, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("Verbindung testen")
            }
            state.connectionResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            HorizontalDivider()

            // --- Upload ---
            Text("Upload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Chunk-Größe: ${state.chunkSizeKb / 1024f} MB", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = state.chunkSizeKb.toFloat(),
                onValueChange = { viewModel.onChunkSizeChange(it.toInt()) },
                valueRange = SettingsStore.MIN_CHUNK_KB.toFloat()..SettingsStore.MAX_CHUNK_KB.toFloat(),
                steps = 9,
            )

            HorizontalDivider()

            // --- Zugang ---
            Text("Zugang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Rolle: ${state.role.name.lowercase()}", style = MaterialTheme.typography.bodyMedium)
            Text("Basisverzeichnis: ${state.basePath}", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            // --- App ---
            OutlinedButton(onClick = onOpenUpdates, modifier = Modifier.fillMaxWidth()) {
                Text("Auf Updates prüfen")
            }
            Button(
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
            ) {
                Text("Abmelden")
            }
        }
    }
}
