package com.willykez.repomaster.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The repo-name + current-branch title used in every repo-scoped tab's top bar
 * (Changes, History, Tools, Files). Having the same two-line block everywhere means
 * you can always tell which repo — and which branch — a screen is acting on at a
 * glance, no matter how many taps deep you are, without needing to remember it from
 * whichever screen you opened the repo from.
 */
@Composable
fun RepoTitleBlock(
    name: String,
    branch: String? = null,
    onBranchLongPress: (() -> Unit)? = null,
) {
    Column {
        Text(name, fontWeight = FontWeight.SemiBold)
        if (!branch.isNullOrBlank()) {
            Text(
                branch,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (onBranchLongPress != null) {
                    Modifier.combinedClickable(onClick = {}, onLongClick = onBranchLongPress)
                } else Modifier,
            )
        }
    }
}
