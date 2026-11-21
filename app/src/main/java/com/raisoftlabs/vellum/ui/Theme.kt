package com.raisoftlabs.vellum.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Indigo = Color(0xFF4F46E5)
val IndigoDeep = Color(0xFF4338CA)
val Violet = Color(0xFF7C3AED)
val Mint = Color(0xFF10B981)
val Amber = Color(0xFFF59E0B)
val Rose = Color(0xFFE11D48)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E9FE),
    onPrimaryContainer = Color(0xFF23236B),
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E9FE),
    onSecondaryContainer = Color(0xFF3B1D6E),
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEDEFF5),
    onSurfaceVariant = Color(0xFF5B6377),
    outline = Color(0xFFDDE1EA),
    outlineVariant = Color(0xFFE8EBF2),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CA3FF),
    onPrimary = Color(0xFF14163A),
    primaryContainer = Color(0xFF2A2F6B),
    onPrimaryContainer = Color(0xFFE0E2FF),
    secondary = Color(0xFFC4B5FD),
    onSecondary = Color(0xFF23124A),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1F232C),
    onSurfaceVariant = Color(0xFF9BA3B4),
    outline = Color(0xFF2A2F3A),
    outlineVariant = Color(0xFF232732),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0A0A)
)

private val VellumTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp
    )
)

@Composable
fun VellumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = VellumTypography,
        content = content
    )
}
