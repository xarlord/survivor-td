package com.survivortd.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PremiumDarkScheme = darkColorScheme(
    primary = StdColors.Cyan,
    onPrimary = StdColors.TextInverse,
    secondary = StdColors.Amber,
    onSecondary = StdColors.TextInverse,
    tertiary = StdColors.Violet,
    background = StdColors.Void,
    onBackground = StdColors.TextPrimary,
    surface = StdColors.Surface,
    onSurface = StdColors.TextPrimary,
    surfaceVariant = StdColors.SurfaceHigh,
    onSurfaceVariant = StdColors.TextSecondary,
    error = StdColors.Coral,
    onError = StdColors.TextPrimary,
    outline = StdColors.Border
)

@Composable
fun SurvivorTDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumDarkScheme,
        content = content
    )
}
