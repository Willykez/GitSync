package com.willykez.repomaster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.expressiveDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Dark is still Repo Master's default identity — a cockpit, not a
// document — but the visual language is now Material 3 Expressive: bigger
// shape rhythm, a three-color accent system (violet/coral/emerald), and
// spring-based motion instead of flat tweens.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val DarkScheme = expressiveDarkColorScheme(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val LightScheme = expressiveLightColorScheme(
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

// M3 Expressive's shape scale is "more like the type scale" — a wider range
// of roundedness used deliberately, not just one radius everywhere. Cards
// and sheets get the bigger, softer "increased" corners; chips and small
// controls stay tighter.
val RepoMasterShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepoMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You: derive the scheme from the user's wallpaper on Android 12+.
    // Off by default would make every install look identical; on by default
    // is the whole point of "Expressive" personalization — flip to false if
    // you want Repo Master's violet/coral identity to be non-negotiable.
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

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = RepoMasterTypography,
        shapes = RepoMasterShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
