package com.example.picosbsvrplayer.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = VrBlueDark,
    onPrimary = Color(0xFF001B3D),
    primaryContainer = Color(0xFF173C68),
    onPrimaryContainer = Color(0xFFD9E9FF),
    secondary = VrCyan,
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF164755),
    onSecondaryContainer = Color(0xFFD2F5FF),
    tertiary = VrViolet,
    background = VrDarkBackground,
    onBackground = VrDarkText,
    surface = VrDarkSurface,
    onSurface = VrDarkText,
    surfaceVariant = VrDarkSurfaceRaised,
    onSurfaceVariant = VrDarkTextMuted,
    surfaceContainer = VrDarkSurface,
    surfaceContainerHigh = VrDarkSurfaceRaised,
    surfaceContainerHighest = VrDarkSurfaceMuted,
    outline = Color(0xFF667082),
    outlineVariant = Color(0xFF303846),
    error = VrRed,
    errorContainer = Color(0xFF51211F),
    onErrorContainer = Color(0xFFFFDAD6),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VrBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E9FF),
    onPrimaryContainer = Color(0xFF002E5F),
    secondary = Color(0xFF007A9A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F1FF),
    onSecondaryContainer = Color(0xFF003542),
    tertiary = Color(0xFF7A3EB1),
    background = VrLightBackground,
    onBackground = VrLightText,
    surface = VrLightSurface,
    onSurface = VrLightText,
    surfaceVariant = VrLightSurfaceMuted,
    onSurfaceVariant = VrLightTextMuted,
    surfaceContainer = VrLightSurface,
    surfaceContainerHigh = VrLightSurfaceMuted,
    surfaceContainerHighest = Color(0xFFDDE3EC),
    outline = Color(0xFF747B87),
    outlineVariant = Color(0xFFD0D6DF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
  )

private val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
