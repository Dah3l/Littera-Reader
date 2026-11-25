package com.littera.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta "papel y tinta": cálida, pensada para leer.
private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A2B),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF6EF),
    onBackground = Color(0xFF211A13),
    surface = Color(0xFFF3EDE2),
    onSurface = Color(0xFF211A13),
    surfaceVariant = Color(0xFFE7DCC9),
    onSurfaceVariant = Color(0xFF57493A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9A05B),
    onPrimary = Color(0xFF2A1B08),
    background = Color(0xFF17140F),
    onBackground = Color(0xFFEDE4D3),
    surface = Color(0xFF211D16),
    onSurface = Color(0xFFEDE4D3),
    surfaceVariant = Color(0xFF38312A),
    onSurfaceVariant = Color(0xFFCFC0A8)
)

@Composable
fun LitteraTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
