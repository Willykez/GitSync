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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }

        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RepoMasterTypography,
        shapes = RepoMasterShapes,
        content = content
    )
}