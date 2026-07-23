package com.willykez.repomaster.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.repomaster.ui.theme.StatusClean
import com.willykez.repomaster.ui.theme.StatusDeleted

private val INTERVAL_OPTIONS = listOf(1L, 3L, 6L, 12L, 24L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    var notificationsDenied by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsDenied = !granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("Background sync", style = MaterialTheme.typography.titleMedium)
            Text(
                "Periodically checks every repo for new commits on the remote (fetch only — " +
                    "it never merges or changes your working tree on its own). Needs network access. " +
                    "When a check finds new commits, you'll get a notification.",
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
                    onCheckedChange = { enabled ->
                        vm.setBackgroundSyncEnabled(enabled)
                        // POST_NOTIFICATIONS is a runtime permission on API 33+ — this is the
                        // only place in the app with an Activity to prompt from, since
                        // SyncWorker itself runs headless in the background. Harmless to call
                        // when already granted (the launcher just no-ops without a dialog).
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }

            if (notificationsDenied) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Notifications are off, so background sync will still run but won't alert you when it finds new commits. " +
                        "You can turn them back on from this app's system notification settings.",
                    style = MaterialTheme.typography.bodySmall, color = StatusDeleted,
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
        }
    }
}
