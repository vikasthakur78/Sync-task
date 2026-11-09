package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF283593),
    onPrimaryContainer = Color(0xFFE8EAF6),
    secondary = Color(0xFF80D8FF),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFFE65100),
    background = SurfaceDark,
    surface = CardDark,
    surfaceVariant = Color(0xFF282C42),
    onBackground = Color(0xFFEDE7F6),
    onSurface = Color(0xFFEDE7F6)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = Color(0xFF01579B),
    tertiary = CoralTertiary,
    onTertiary = Color.White,
    background = SurfaceLight,
    surface = CardLight,
    surfaceVariant = Color(0xFFEEF0FA),
    onBackground = Color(0xFF1A1C24),
    onSurface = Color(0xFF1A1C24)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false by default to retain custom branded palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
