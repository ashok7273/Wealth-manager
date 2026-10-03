package com.example.money_manager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand
val Violet = Color(0xFF6C63FF)
val VioletDark = Color(0xFF8F88FF)

// Semantic money colors
val IncomeLight = Color(0xFF0E9F6E)
val IncomeDark = Color(0xFF34D399)
val ExpenseLight = Color(0xFFD64550)
val ExpenseDark = Color(0xFFFF6B81)

// Dark surfaces
val DarkBackground = Color(0xFF0E1116)
val DarkSurface = Color(0xFF161B22)
val DarkSurfaceVariant = Color(0xFF1E242E)
val DarkOutline = Color(0xFF2A313C)
val DarkOnSurface = Color(0xFFE6E9EF)
val DarkOnSurfaceVariant = Color(0xFF98A2B3)

// Light surfaces
val LightBackground = Color(0xFFF5F6FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF0F6)
val LightOutline = Color(0xFFE0E3EB)
val LightOnSurface = Color(0xFF161B22)
val LightOnSurfaceVariant = Color(0xFF6B7280)

val DarkScheme = darkColorScheme(
    primary = VioletDark,
    onPrimary = Color(0xFF14121F),
    primaryContainer = Violet,
    onPrimaryContainer = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

val LightScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E3FF),
    onPrimaryContainer = Color(0xFF241F63),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline
)

/** Colors Material 3 has no slot for. */
@Immutable
data class MoneyColors(
    val income: Color,
    val expense: Color
)

val LocalMoneyColors = staticCompositionLocalOf {
    MoneyColors(IncomeLight, ExpenseLight)
}
