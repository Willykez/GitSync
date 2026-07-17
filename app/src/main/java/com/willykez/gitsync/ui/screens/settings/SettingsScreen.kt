package com.willykez.gitsync.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.gitsync.data.repository.DecryptedSigningKey
import com.willykez.gitsync.ui.theme.StatusClean
import com.willykez.gitsync.ui.theme.StatusDeleted

private val INTERVAL_OPTIONS = listOf(1L, 3L, 6L, 12L, 24L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); vm.dismissMessage() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Background sync", style = MaterialTheme.typography.titleMedium)
            Text(
                "Periodically checks every repo for new commits on the remote (fetch only — " +
                    "it never merges or changes your working tree on its own). Needs network access.",
                style = MaterialTheme.typography.bodySmall,
                color = StatusClean,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enable background sync")
                Switch(
                    checked = state.backgroundSyncEnabled,
                    onCheckedChange = vm::setBackgroundSyncEnabled,
                )
            }

            if (state.backgroundSyncEnabled) {
                Spacer(Modifier.height(16.dp))
                Text("Check every", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    INTERVAL_OPTIONS.forEach { hours ->
                        FilterChip(
                            selected = state.intervalHours == hours,
                            onClick = { vm.setIntervalHours(hours) },
                            label = { Text(if (hours < 24) "${hours}h" else "1d") },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Commit signing (GPG)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Signs new commits with a GPG key so they show as \"Verified\" on GitHub/GitLab. " +
                    "Depends on JGit's Bouncy Castle signer finding the key on-device — treat this " +
                    "as best-effort and check a commit actually verifies after enabling it.",
                style = MaterialTheme.typography.bodySmall,
                color = StatusClean,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sign commits")
                Switch(
                    checked = state.signingEnabled,
                    onCheckedChange = vm::setSigningEnabled,
                    enabled = state.signingKeys.isNotEmpty() || state.signingEnabled,
                )
            }

            if (state.signingKeys.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("No GPG keys imported yet.", style = MaterialTheme.typography.bodySmall, color = StatusClean)
            } else {
                Spacer(Modifier.height(12.dp))
                Text("Active key", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                state.signingKeys.forEach { key ->
                    SigningKeyRow(
                        key = key,
                        selected = key.id == state.activeSigningKeyId,
                        onSelect = { vm.setActiveKey(key.id) },
                        onDelete = { vm.deleteSigningKey(key) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = vm::openKeyImporter) { Text("Import GPG private key") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.showKeyImporter) {
        SigningKeyImporterSheet(onDismiss = vm::dismissKeyImporter, onImport = vm::importSigningKey)
    }
}

@Composable
private fun SigningKeyRow(
    key: DecryptedSigningKey, selected: Boolean,
    onSelect: () -> Unit, onDelete: () -> Unit,
) {
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(key.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (key.userId.isNotBlank()) "${key.userId} · ${key.keyId}" else key.keyId,
                    style = MaterialTheme.typography.bodySmall, color = StatusClean, maxLines = 1,
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = StatusDeleted) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SigningKeyImporterSheet(onDismiss: () -> Unit, onImport: (name: String, armored: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var armored by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text("Import GPG private key", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Export one with \"gpg --export-secret-keys --armor <key-id>\" and paste the whole block below.",
                style = MaterialTheme.typography.bodySmall, color = StatusClean,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = armored, onValueChange = { armored = it },
                label = { Text("Armored private key") },
                placeholder = { Text("-----BEGIN PGP PRIVATE KEY BLOCK-----") },
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onImport(name, armored) },
                    enabled = armored.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Import") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
