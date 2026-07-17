package com.willykez.repomaster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Dark is still Repo Master's default identity — a cockpit, not a document —
// with a three-color accent system (violet/coral/emerald) and a wider shape
// rhythm than the old flat design.
//
// NOTE: this previously built on Material 3 *Expressive* (MaterialExpressiveTheme,
// expressiveLightColorScheme/expressiveDarkColorScheme, MotionScheme). That tier
// is compiled as internal-only in the material3 build this project's pinned
// compose-bom actually resolves, so no @OptIn can reach it and the build fails.
// This uses the standard, always-public M3 color-scheme/theme API instead —
// same palette, same shapes, same typography, just without the Expressive
// motion system.
private val DarkScheme = darkColorScheme(
    primary = CommandBlue,
    onPrimary = Color(0xFF2B1259),
    primaryContainer = CommandBlueDeep,
    onPrimaryContainer = Color.White,
    secondary = SignalGold,
    onSecondary = Color(0xFF3D1400),
    secondaryContainer = SignalGoldDeep,
    onSecondaryContainer = Color.White,
    tertiary = Emerald,
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = EmeraldDeep,
    onTertiaryContainer = Color.White,
    background = Void,
    onBackground = Color(0xFFEDE7E0),
    surface = Hull,
    onSurface = Color(0xFFEDE7E0),
    surfaceVariant = Deck,
    onSurfaceVariant = StatusMuted,
    surfaceContainer = Deck,
    surfaceContainerHigh = DeckRaised,
    surfaceContainerHighest = DeckRaised,
    outline = HullBorder,
    error = StatusDeleted,
    onError = Color.White,
)

private val LightScheme = lightColorScheme(
    primary = CommandBlueDeep,
    onPrimary = Color.White,
    primaryContainer = CommandBlueDim,
    onPrimaryContainer = CommandBlueDeep,
    secondary = SignalGoldDeep,
    onSecondary = Color.White,
    tertiary = EmeraldDeep,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = PaperDim,
    onSurfaceVariant = StatusMuted,
    outline = Color(0x1F1F1B16),
    error = StatusDeleted,
    onError = Color.White,
)

// A wider range of roundedness used deliberately, not just one radius
// everywhere — cards and sheets get bigger, softer corners; chips and small
// controls stay tighter.
val RepoMasterShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun RepoMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You: derive the scheme from the user's wallpaper on Android 12+.
    // Off by default would make every install look identical; on by default
    // is the whole point of personalization — flip to false if you want Repo
    // Master's violet/coral identity to be non-negotiable.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RepoMasterTypography,
        shapes = RepoMasterShapes,
        content = content,
    )
}
