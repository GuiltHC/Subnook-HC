package com.subnook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E8FF),
    onPrimaryContainer = Color(0xFF1E1B6B),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF0F6),
    onSecondaryContainer = Color(0xFF1F2937),
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFF4F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF4F5F9),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F2F7),
    surfaceContainerHigh = Color(0xFFE9ECF3),
    surfaceContainerHighest = Color(0xFFE2E6EE),
    outline = Color(0xFFCBD2DE),
    outlineVariant = Color(0xFFE5E8EF),
    error = Color(0xFFE5484D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF2E2A6E),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF22252D),
    onSecondaryContainer = Color(0xFFE5E7EB),
    tertiary = Color(0xFFC4B5FD),
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFE8EAEE),
    surface = Color(0xFF0B0C10),
    onSurface = Color(0xFFE8EAEE),
    surfaceVariant = Color(0xFF1C1F26),
    onSurfaceVariant = Color(0xFF9AA1AD),
    surfaceContainerLowest = Color(0xFF111318),
    surfaceContainerLow = Color(0xFF15171D),
    surfaceContainer = Color(0xFF1A1D23),
    surfaceContainerHigh = Color(0xFF22252D),
    surfaceContainerHighest = Color(0xFF2A2E37),
    outline = Color(0xFF3A3F4A),
    outlineVariant = Color(0xFF262A32),
    error = Color(0xFFFF6B6F),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Immutable
data class ExtraColors(
    val heroBrush: Brush,
    val accentBrush: Brush,
    val cardShadow: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val dark: Boolean,
)

val LocalExtra = staticCompositionLocalOf {
    ExtraColors(Brush.linearGradient(listOf(Color.Black, Color.Black)), Brush.linearGradient(listOf(Color.Black, Color.Black)), Color.Black, Color.Green, Color.Yellow, Color.Red, false)
}

val OverdueRed = Color(0xFFE5484D)
val SoonOrange = Color(0xFFF59E0B)

/** 数字等宽，金额对齐更整齐 */
val TabularNums = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun SubnookTheme(mode: com.subnook.app.data.ThemeMode = com.subnook.app.data.ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        com.subnook.app.data.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        com.subnook.app.data.ThemeMode.LIGHT -> false
        com.subnook.app.data.ThemeMode.DARK -> true
    }
    val activity = androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity
    androidx.compose.runtime.LaunchedEffect(dark) {
        // 状态栏/导航栏图标颜色跟随应用主题，而不是系统主题
        val style = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
        activity?.enableEdgeToEdge(style, style)
    }
    val extra = if (dark) {
        ExtraColors(
            heroBrush = Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF4C1D95), Color(0xFF6D28D9))),
            accentBrush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
            cardShadow = Color.Transparent,
            success = Color(0xFF34D399), warning = Color(0xFFFBBF24), danger = Color(0xFFFF6B6F), dark = true,
        )
    } else {
        ExtraColors(
            heroBrush = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFF9333EA))),
            accentBrush = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))),
            cardShadow = Color(0xFF1E1B4B).copy(alpha = 0.10f),
            success = Color(0xFF10B981), warning = Color(0xFFF59E0B), danger = Color(0xFFE5484D), dark = false,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
