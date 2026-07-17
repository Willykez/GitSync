package com.willykez.gitsync.ui.screens.sshkeys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.gitsync.data.repository.DecryptedSshKey
import com.willykez.gitsync.ui.theme.StatusClean
import com.willykez.gitsync.ui.theme.StatusDeleted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshKeysScreen(
    onBack: () -> Unit,
    vm: SshKeysViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()
    val snack = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); vm.dismissMessage() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Keys", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::openGenerator,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New Key") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.lastGeneratedPublicKey?.let { pub ->
                GeneratedKeyBanner(pub, context, onDismiss = vm::dismissGeneratedKeyBanner)
            }
            TextButton(onClick = vm::openImporter, modifier = Modifier.padding(start = 12.dp)) {
                Text("Or import a key generated elsewhere (ssh-keygen)")
            }
            if (state.keys.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Key, null, tint = StatusClean, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No SSH keys yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Generate a key here, or import one, to clone/push over SSH instead of HTTPS.",
                            style = MaterialTheme.typography.bodyMedium, color = StatusClean,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.keys, key = { it.id }) { key ->
                        SshKeyCard(key, context, onDelete = { vm.delete(key) })
                    }
                }
            }
        }
    }

    if (state.showGenerator) GeneratorSheet(onDismiss = vm::dismissSheets, onGenerate = vm::generate)
    if (state.showImporter) ImporterSheet(onDismiss = vm::dismissSheets, onImport = vm::import)
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
private fun GeneratedKeyBanner(publicKey: String, context: Context, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("New public key — add this to GitHub/GitLab", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(publicKey, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { copyToClipboard(context, "SSH public key", publicKey) }) {
                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Copy")
                }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun SshKeyCard(key: DecryptedSshKey, context: Context, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
                Column(Modifier.weight(1f)) {
                    Text(key.name, style = MaterialTheme.typography.titleMedium)
                    Text(key.publicKeyLine.take(48) + "…", style = MaterialTheme.typography.bodySmall, color = StatusClean, maxLines = 1)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete", tint = StatusDeleted) }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(key.publicKeyLine, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { copyToClipboard(context, "SSH public key", key.publicKeyLine) }) {
                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp)); Text("Copy public key")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorSheet(onDismiss: () -> Unit, onGenerate: (name: String, comment: String, passphrase: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text("Generate SSH key", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Ed25519 — the modern default, same as \"ssh-keygen -t ed25519\"", style = MaterialTheme.typography.bodySmall, color = StatusClean)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Label") }, placeholder = { Text("GitHub - Willykez") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comment (optional)") }, placeholder = { Text("you@example.com") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = passphrase, onValueChange = { passphrase = it }, label = { Text("Passphrase (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onGenerate(name, comment, passphrase) }, modifier = Modifier.weight(1f)) { Text("Generate") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImporterSheet(onDismiss: () -> Unit, onImport: (name: String, privateKeyPem: String, publicKeyLine: String, passphrase: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text("Import SSH key", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = privateKey, onValueChange = { privateKey = it },
                label = { Text("Private key (id_ed25519 / id_rsa contents)") },
                placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = publicKey, onValueChange = { publicKey = it },
                label = { Text("Public key (id_ed25519.pub contents)") },
                placeholder = { Text("ssh-ed25519 AAAA...") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = passphrase, onValueChange = { passphrase = it }, label = { Text("Passphrase (if the key has one)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onImport(name, privateKey, publicKey, passphrase) },
                    enabled = privateKey.isNotBlank() && publicKey.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Import") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
