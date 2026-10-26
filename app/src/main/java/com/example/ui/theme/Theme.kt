package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MejanDarkPrimary,
    onPrimary = Color(0xFF003830),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFF80E5D7),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00332C),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFF7DD3FC),
    background = MejanDarkBackground,
    surface = MejanDarkSurface,
    surfaceVariant = MejanDarkSurfaceVariant,
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = MejanPrimary,
    onPrimary = MejanOnPrimary,
    primaryContainer = MejanPrimaryContainer,
    onPrimaryContainer = MejanOnPrimaryContainer,
    secondary = MejanSecondary,
    secondaryContainer = MejanSecondaryContainer,
    onSecondaryContainer = MejanOnSecondaryContainer,
    tertiary = MejanTertiary,
    background = MejanBackground,
    surface = MejanSurface,
    surfaceVariant = MejanSurfaceVariant,
    outline = MejanOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve distinctive Mejan Drugs corporate brand
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
