package com.willykez.repomaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Repo Master's signature card, redesigned for Material 3 Expressive.
 *
 * The old identity was a translucent "pane of glass": a subtle fill plus a
 * thin hairline border. Expressive drops the border in favor of a solid
 * tonal surface, a much bigger corner radius, and real tonal elevation —
 * the card reads as a soft, tactile object rather than a sheet of glass.
 *
 * The call signature is unchanged on purpose: every screen (repos, commits,
 * branches, tags, stash entries…) already builds on top of [GlassCard], so
 * repainting this one component repaints all of them consistently.
 *
 * [accent], when set, now renders as a bold rounded bar rather than a thin
 * line — same "signal at a glance" purpose (branch/status color), scaled up
 * to match Expressive's bigger, more confident shape language.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
    ) {
        if (accent != null) {
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .padding(vertical = 10.dp)
                        .background(accent, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)),
                )
                Box(Modifier.weight(1f)) { content() }
            }
        } else {
            content()
        }
    }
}
