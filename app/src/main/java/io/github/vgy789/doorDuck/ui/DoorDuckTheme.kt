package io.github.vgy789.doorDuck.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DoorDuckLightColors = lightColorScheme(
    primary = Color(0xFFE1AA22),
    secondary = Color(0xFFE78A36),
    tertiary = Color(0xFF5EAD77),
    background = Color(0xFFFFFBF3),
    surface = Color(0xFFFFFEFB),
    onPrimary = Color(0xFF32240F),
    onSurface = Color(0xFF2E2419),
    onSurfaceVariant = Color(0xFF6D5B46),
)

private val DoorDuckDarkColors = darkColorScheme(
    primary = Color(0xFFF2C64D),
    secondary = Color(0xFFE88C3A),
    tertiary = Color(0xFF8CD7A3),
    background = Color(0xFF17130F),
    surface = Color(0xFF231C15),
    onPrimary = Color(0xFF2D220F),
    onSurface = Color(0xFFF9F0E2),
    onSurfaceVariant = Color(0xFFD4C4AF),
)

@Composable
fun DoorDuckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DoorDuckDarkColors else DoorDuckLightColors,
        content = content,
    )
}
