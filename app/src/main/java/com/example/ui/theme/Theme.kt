package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = Color.Black,
    primaryContainer = DeepAzure,
    onPrimaryContainer = Color.White,
    secondary = CyanBright,
    onSecondary = Color.Black,
    secondaryContainer = JarvisSurfaceVariant,
    onSecondaryContainer = CyanBright,
    tertiary = ElectricBlue,
    background = JarvisBackground,
    onBackground = TextPrimary,
    surface = JarvisSurface,
    onSurface = TextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineCyan,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
