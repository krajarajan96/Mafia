package com.mafia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MafiaRed = Color(0xFFDC2626)
val MafiaPurple = Color(0xFF7C3AED)
val MafiaGold = Color(0xFFF59E0B)
val NightBlueSurface = Color(0xFF1E293B)
val DawnOrange = Color(0xFFFB923C)
val TownGreen = Color(0xFF16A34A)
val DeadGray = Color(0xFF6B7280)

val DarkColorScheme = darkColorScheme(
    primary = MafiaPurple, onPrimary = Color.White, secondary = MafiaRed,
    onSecondary = Color.White, tertiary = MafiaGold, background = Color(0xFF0F172A),
    surface = NightBlueSurface, onBackground = Color(0xFFF1F5F9), onSurface = Color(0xFFE2E8F0),
    error = MafiaRed, surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun MafiaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}
