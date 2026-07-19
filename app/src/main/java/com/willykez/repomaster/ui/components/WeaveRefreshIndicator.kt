package com.willykez.repomaster.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * A WeaveMask/Magisk-manager style pull-to-refresh indicator: a plain outlined pill that
 * grows taller the further you pull — instead of a fixed circular spinner overlaid on top
 * of the content — then springs back down with a bounce when you let go. Meant to be
 * placed as a normal (non-overlay) item at the top of a Column, above the scrollable
 * content, so the content is genuinely pushed down as it grows, matching the reference
 * screenshots, rather than floated over the list.
 *
 * @param progress the drag progress from [androidx.compose.material.pullrefresh.PullRefreshState],
 *   0 at rest and >=1 once past the release threshold.
 */
@Composable
fun WeaveRefreshIndicator(
    refreshing: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    // Smooths out the raw drag value so releasing early/late eases rather than snapping,
    // and gives the collapse-back-to-rest its bounce.
    val target = if (refreshing) 1f else progress.coerceIn(0f, 1.4f)
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (refreshing || target > 0f) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        } else {
            tween(150)
        },
        label = "weaveRefreshProgress",
    )

    if (animatedProgress <= 0.001f && !refreshing) return

    val pillWidth = 24.dp
    val maxHeight = 56.dp
    val pillHeight = lerp(0.dp, maxHeight, animatedProgress.coerceIn(0f, 1f))
    val labelAlpha = ((animatedProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)

    // A gentle continuous spin for the dot inside the pill once actually refreshing —
    // the pull gesture itself communicates "loading" via stretch; this communicates
    // "still working" once you've let go.
    val infiniteTransition = rememberInfiniteTransition(label = "weaveRefreshSpin")
    val spin by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "weaveRefreshSpinAngle",
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = pillWidth, height = pillHeight.coerceAtLeast(1.dp))
                .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            if (refreshing && pillHeight > pillWidth * 0.8f) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = spin },
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (labelAlpha > 0f) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (refreshing) "Refreshing…" else "Release to refresh",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = labelAlpha },
            )
        }
    }
}
