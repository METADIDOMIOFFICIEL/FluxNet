package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FluxNetDarkColorScheme = darkColorScheme(
    primary = DownloadCyan,
    onPrimary = Color(0xFF002430),
    primaryContainer = Color(0xFF003B4D),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = UploadEmerald,
    onSecondary = Color(0xFF00291B),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = MobileAmber,
    onTertiary = Color(0xFF2D1900),
    tertiaryContainer = Color(0xFF452700),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = BlockedCoral,
    onError = Color(0xFF3B0606),
    errorContainer = Color(0xFF5C1111),
    onErrorContainer = Color(0xFFFECACA),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val FluxNetLightColorScheme = lightColorScheme(
    primary = DownloadCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = UploadEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = MobileAmberDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = BlockedCoralDark,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun FluxNetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FluxNetDarkColorScheme else FluxNetLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FluxNetTypography,
        content = content
    )
}
