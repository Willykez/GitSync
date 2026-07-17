package com.willykez.repomaster.ui.screens.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.willykez.repomaster.App
import com.willykez.repomaster.ui.components.GlassCard
import com.willykez.repomaster.ui.theme.CommandBlue
import com.willykez.repomaster.ui.theme.Emerald
import com.willykez.repomaster.ui.theme.SignalGold
import com.willykez.repomaster.ui.theme.StatusClean

private data class ToolTile(val label: String, val icon: ImageVector, val onClick: () -> Unit)
private data class ToolCategory(val title: String, val accent: Color, val tools: List<ToolTile>)

/**
 * The "Tools" tab — a single, predictable home for every repo-scoped git
 * action (branches, tags, stash, remotes, files, conflicts, .gitignore).
 *
 * Previously this screen duplicated navigation that also lived in the
 * Changes screen's icon row *and* its overflow sheet, and mixed in
 * app-level destinations (Discover/Credentials/Settings) that already live
 * on the Repos tab. Now there's exactly one path to each tool, this screen
 * only holds things that act on the *current repo*, and a header always
 * shows which repo that is.
 */
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
) {
    val context = LocalContext.current
    val repoInfo by produceState<Pair<String, String>?>(initialValue = null, repoId) {
        value = if (repoId == null) null else {
            val app = context.applicationContext as App
            val repo = app.repoRepository.getById(repoId)
            repo?.let { it.name to it.branch }
        }
    }

    val categories = listOf(
        ToolCategory(
            title = "Branching",
            accent = CommandBlue,
            tools = listOf(
                ToolTile("Branches", Icons.Filled.AccountTree, onOpenBranches),
                ToolTile("Tags", Icons.Filled.Label, onOpenTags),
            ),
        ),
        ToolCategory(
            title = "Working tree",
            accent = SignalGold,
            tools = listOf(
                ToolTile("Stash", Icons.Filled.Inventory2, onOpenStash),
                ToolTile("Conflicts", Icons.Filled.Warning, onOpenConflicts),
                ToolTile(".gitignore", Icons.Filled.Block, onOpenGitignore),
            ),
        ),
        ToolCategory(
            title = "Repository",
            accent = Emerald,
            tools = listOf(
                ToolTile("Files", Icons.Filled.FolderOpen, onOpenFiles),
                ToolTile("Remotes", Icons.Filled.Cloud, onOpenRemote),
            ),
        ),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tools", fontWeight = FontWeight.Bold) }) },
    ) { pad ->
        if (repoId == null) {
            NoRepoSelected(Modifier.padding(pad))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(pad),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedVisibility(
                        visible = repoInfo != null,
                        enter = fadeIn(tween(200)) + expandVertically(),
                    ) {
                        ActiveRepoHeader(name = repoInfo?.first, branch = repoInfo?.second)
                    }
                }
                categories.forEach { category ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel(category.title)
                    }
                    items(category.tools) { tool ->
                        ToolCard(tool, accent = category.accent)
                    }
                }
            }
        }
    }
}

/** Always-visible reminder of which repo these tools act on — the whole
 *  reason this screen used to feel confusing was that it was silent about
 *  that until you tapped something. */
@Composable
private fun ActiveRepoHeader(name: String?, branch: String?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CommandBlue.copy(alpha = 0.18f), shape = CircleShape) {
                Icon(
                    Icons.Filled.FolderOpen, null, tint = CommandBlue,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    name ?: "Loading…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!branch.isNullOrBlank()) {
                    Text(branch, style = MaterialTheme.typography.labelSmall, color = StatusClean)
                }
            }
        }
    }
}

@Composable
private fun NoRepoSelected(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.FolderOff, null, tint = StatusClean, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("No repo selected", style = MaterialTheme.typography.titleMedium)
        Text(
            "Pick a repo from the Repos tab to see its tools here.",
            style = MaterialTheme.typography.bodyMedium,
            color = StatusClean,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = StatusClean,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToolCard(tool: ToolTile, accent: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "toolCardScale",
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = tool.onClick),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(tool.icon, null, tint = accent, modifier = Modifier.height(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                tool.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
