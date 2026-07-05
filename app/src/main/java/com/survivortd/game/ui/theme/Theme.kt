package com.survivortd.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF0A0E1A),
    secondary = Color(0xFF42A5F5),
    background = Color(0xFF0A0E1A),
    surface = Color(0xFF1E1E2E),
    onSurface = Color.White,
    error = Color(0xFFFF1744),
    onBackground = Color.White
)

@Composable
fun SurvivorTDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
