package com.willykez.repomaster.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.willykez.repomaster.ui.components.GlassCard
import com.willykez.repomaster.ui.theme.CommandBlue
import com.willykez.repomaster.ui.theme.Emerald
import com.willykez.repomaster.ui.theme.StatusClean

private data class ToolTile(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    repoId: Long?,
    onOpenBranches: () -> Unit,
    onOpenStash: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenGitignore: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenConflicts: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val repoTools = listOf(
        ToolTile("Branches", Icons.Filled.AccountTree, onOpenBranches),
        ToolTile("Tags", Icons.Filled.Label, onOpenTags),
        ToolTile("Stash", Icons.Filled.Inventory2, onOpenStash),
        ToolTile("Remotes", Icons.Filled.Cloud, onOpenRemote),
        ToolTile("Files", Icons.Filled.Folder, onOpenFiles),
        ToolTile("Conflicts", Icons.Filled.Warning, onOpenConflicts),
        ToolTile(".gitignore", Icons.Filled.Block, onOpenGitignore),
    )
    val appTools = listOf(
        ToolTile("Discover on GitHub", Icons.Filled.Explore, onOpenDiscover),
        ToolTile("Credentials", Icons.Filled.Key, onOpenCredentials),
        ToolTile("Settings", Icons.Filled.Settings, onOpenSettings),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("More", fontWeight = FontWeight.Bold) }) },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel(if (repoId != null) "This repo" else "Pick a repo in Repos to unlock these")
            }
            items(repoTools) { tool ->
                ToolCard(tool, enabled = repoId != null, accent = CommandBlue)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(4.dp)); SectionLabel("App")
            }
            items(appTools) { tool ->
                ToolCard(tool, enabled = true, accent = Emerald)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = StatusClean,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun ToolCard(tool: ToolTile, enabled: Boolean, accent: Color) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = tool.onClick) else Modifier),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                tool.icon, null,
                tint = if (enabled) accent else StatusClean.copy(alpha = 0.4f),
                modifier = Modifier.height(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tool.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else StatusClean.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
