package de.filefly.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// Onboarding-Screen: Server-URL + Invite-Code. Ruft onDone, sobald die Session steht.
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FileFly einrichten", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Verbinde dich mit deinem FileFly-Server im lokalen Netz und löse deinen " +
                "Einladungs-Code ein.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = viewModel::onServerUrlChange,
            label = { Text("Server-URL") },
            placeholder = { Text("http://192.168.1.50:8000") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
            onClick = viewModel::testConnection,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Verbindung testen")
        }

        state.connectionOk?.let { ok ->
            Text(
                if (ok) "✓ Server erreichbar" else "✗ Server nicht erreichbar",
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedTextField(
            value = state.inviteCode,
            onValueChange = viewModel::onInviteChange,
            label = { Text("Invite-Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ScanQrButton(enabled = !state.busy, onScanned = viewModel::applyInviteUri)

        Button(
            onClick = viewModel::redeemInvite,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Einladung einlösen")
        }

        if (state.busy) {
            CircularProgressIndicator()
        }

        state.error?.let { msg -> ErrorCard(msg) }
    }
}

// QR-Scan-Button (zxing-embedded). ScanContract fragt die CAMERA-Berechtigung selbst ab
// und liefert den gescannten Inhalt (die filefly://invite-URI) zurück.
@Composable
private fun ScanQrButton(
    enabled: Boolean,
    onScanned: (String) -> Unit,
) {
    val scanLauncher =
        rememberLauncherForActivityResult(ScanContract()) { result ->
            result.contents?.let(onScanned)
        }
    OutlinedButton(
        onClick = {
            scanLauncher.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Einladungs-QR scannen")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false),
            )
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("QR-Code scannen")
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(message, modifier = Modifier.padding(12.dp))
    }
}
