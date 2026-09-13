package de.filefly.feature.invite

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Admin-Screen: Invite anlegen (Rolle/Verzeichnis/Ablauf/Nutzungslimit), Ergebnis als
// Code + QR + Teilen-Link, darunter Liste bestehender Invites mit Revoke.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(
    viewModel: InviteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Einladungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
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
            InviteForm(viewModel = viewModel, state = state)

            state.error?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp))
                }
            }

            state.created?.let { created ->
                CreatedInviteCard(created = created, onDismiss = viewModel::dismissCreated)
            }

            if (state.invites.isNotEmpty()) {
                HorizontalDivider()
                Text("Aktive Einladungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                state.invites.forEach { invite ->
                    InviteRow(
                        role = invite.role,
                        codeHint = invite.codeHint,
                        basePath = invite.basePath,
                        usedCount = invite.usedCount,
                        maxUses = invite.maxUses,
                        onRevoke = { viewModel.revoke(invite.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteForm(
    viewModel: InviteViewModel,
    state: InviteViewModel.UiState,
) {
    Text("Neue Einladung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    var roleExpanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { roleExpanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Rolle: ${state.role.name.lowercase()}")
    }
    DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
        viewModel.assignableRoles.forEach { role ->
            DropdownMenuItem(
                text = { Text(role.name.lowercase()) },
                onClick = {
                    viewModel.onRoleChange(role)
                    roleExpanded = false
                },
            )
        }
    }

    OutlinedTextField(
        value = state.basePath,
        onValueChange = viewModel::onBasePathChange,
        label = { Text("Basisverzeichnis (z.B. /familie/gast)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.expiresInHours,
        onValueChange = viewModel::onExpiresChange,
        label = { Text("Gültig für Stunden (leer = unbegrenzt)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.maxUses,
        onValueChange = viewModel::onMaxUsesChange,
        label = { Text("Max. Nutzungen (0 = unbegrenzt)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )

    Button(
        onClick = viewModel::createInvite,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Einladung erstellen")
    }
    if (state.busy) CircularProgressIndicator()
}

@Composable
private fun CreatedInviteCard(
    created: InviteViewModel.Created,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val qr = remember(created.shareUri) { encodeQr(created.shareUri) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Einladung erstellt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Der Code wird nur EINMAL angezeigt. Teile ihn jetzt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            qr?.let {
                Image(bitmap = it, contentDescription = "QR-Code", modifier = Modifier.size(220.dp))
            }

            Text(
                created.response.code,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(created.response.code)) }) {
                    Text("Code kopieren")
                }
                Button(onClick = { context.shareText(created.shareUri) }) {
                    Text("Link teilen")
                }
            }
            OutlinedButton(onClick = onDismiss) { Text("Schließen") }
        }
    }
}

@Composable
private fun InviteRow(
    role: String,
    codeHint: String,
    basePath: String,
    usedCount: Int,
    maxUses: Int,
    onRevoke: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$role · …$codeHint", fontWeight = FontWeight.Bold)
                Text(
                    "Pfad: ${basePath.ifBlank { "/" }} · genutzt $usedCount${if (maxUses > 0) "/$maxUses" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRevoke) {
                Icon(Icons.Filled.Delete, contentDescription = "Widerrufen")
            }
        }
    }
}

private fun android.content.Context.shareText(text: String) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    startActivity(Intent.createChooser(intent, "Einladung teilen"))
}
