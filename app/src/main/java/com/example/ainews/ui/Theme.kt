package com.example.ainews.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ainews.data.Category

val Background = Color(0xFF0A0E13)
val Surface = Color(0xFF111720)
val Card = Color(0xFF141B26)
val Outline = Color(0xFF232E3F)
val Accent = Color(0xFF5EEAD4)
val AccentAlt = Color(0xFF8B9DFF)
val TextPrimary = Color(0xFFE6EDF3)
val TextMuted = Color(0xFF8B9BB0)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF04211D),
    primaryContainer = Color(0xFF123B38),
    onPrimaryContainer = Accent,
    secondary = AccentAlt,
    onSecondary = Color(0xFF0B1030),
    secondaryContainer = Color(0xFF16302F),
    onSecondaryContainer = Accent,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = Card,
    onSurfaceVariant = TextMuted,
    surfaceContainer = Surface,
    surfaceContainerHigh = Card,
    surfaceContainerHighest = Card,
    surfaceContainerLow = Surface,
    outline = Outline,
    outlineVariant = Outline,
    error = Color(0xFFFF8A80),
    inverseSurface = Color(0xFF1E2836),
    inverseOnSurface = TextPrimary,
    inversePrimary = Accent,
)

/** Small monospace text used for labels: the "tech" touch. */
val MonoLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.6.sp,
)

fun Category.color(): Color = when (this) {
    Category.LOCAL -> Accent
    Category.MODELS -> Color(0xFFA78BFA)
    Category.TOOLS -> Color(0xFFFBBF24)
    Category.RESEARCH -> Color(0xFF60A5FA)
    Category.INDUSTRY -> Color(0xFF94A3B8)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
