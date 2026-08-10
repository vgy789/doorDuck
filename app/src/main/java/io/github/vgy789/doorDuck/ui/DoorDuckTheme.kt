package io.github.vgy789.doorDuck.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DoorDuckLightColors = lightColorScheme(
    primary = Color(0xFFF2C64D),
    onPrimary = Color(0xFF2D220F),
    primaryContainer = Color(0xFFFFF0C2),
    onPrimaryContainer = Color(0xFF332400),
    secondary = Color(0xFF8A6200),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC1),
    onSecondaryContainer = Color(0xFF301400),
    tertiary = Color(0xFF4A7942),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6F5CB),
    onTertiaryContainer = Color(0xFF0F200A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1E1B18),
    surface = Color.White,
    onSurface = Color(0xFF1E1B18),
    surfaceVariant = Color(0xFFF0ECE7),
    onSurfaceVariant = Color(0xFF5D574F),
    outline = Color(0xFF888077),
    outlineVariant = Color(0xFFE0DDD7),
    scrim = Color.Black,
    inverseSurface = Color(0xFF332F2A),
    inverseOnSurface = Color(0xFFF7F0E8),
    inversePrimary = Color(0xFFF2C64D),
    surfaceDim = Color(0xFFE5E2DC),
    surfaceBright = Color(0xFFFAF8F5),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F3EE),
    surfaceContainer = Color(0xFFF1EEE8),
    surfaceContainerHigh = Color(0xFFECE8E2),
    surfaceContainerHighest = Color(0xFFE6E2DC),
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

internal val LocalDoorDuckDarkTheme = staticCompositionLocalOf { false }

@Composable
fun DoorDuckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDoorDuckDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DoorDuckDarkColors else DoorDuckLightColors,
            content = content,
        )
    }
}
