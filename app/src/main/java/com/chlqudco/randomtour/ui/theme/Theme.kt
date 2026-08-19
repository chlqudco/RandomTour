package com.chlqudco.randomtour.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ExplorerOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCCB),
    onPrimaryContainer = Color(0xFF3B1000),
    secondary = ExplorerNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE4FF),
    onSecondaryContainer = ExplorerNavy,
    tertiary = ExplorerGreen,
    background = ExplorerCream,
    onBackground = ExplorerInk,
    surface = Color.White,
    onSurface = ExplorerInk,
    surfaceVariant = Color(0xFFF4EEE7),
    onSurfaceVariant = Color(0xFF565C68),
    error = ExplorerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5B1B00),
    primaryContainer = ExplorerOrangeDark,
    secondary = Color(0xFFB8C6EF),
    onSecondary = ExplorerNavy,
    tertiary = Color(0xFF80D5B6),
    background = Color(0xFF101522),
    onBackground = Color(0xFFF0F2F8),
    surface = ExplorerDarkSurface,
    onSurface = Color(0xFFF0F2F8),
    surfaceVariant = Color(0xFF303A51),
    onSurfaceVariant = Color(0xFFD2D7E3)
)

@Composable
fun RandomTourTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
