package com.willykez.repomaster.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.willykez.repomaster.ui.theme.StatusClean

/** Shown on the Changes/History tabs before any repo has been picked from the Repos tab. */
@Composable
fun EmptyTabState(message: String, onGoToRepos: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.FolderOpen, null, tint = StatusClean)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGoToRepos) { Text("Go to Repos") }
    }
}
