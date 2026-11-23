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

/* ---- iOS-inspired glossy palette ---------------------------------------- */
val Indigo = Color(0xFF0A84FF)      // system blue
val IndigoDeep = Color(0xFF0060DF)
val Violet = Color(0xFF5E5CE6)
val Mint = Color(0xFF34C759)
val Amber = Color(0xFFFF9F0A)
val Rose = Color(0xFFFF3B30)
val Teal = Color(0xFF32ADE6)
val Pink = Color(0xFFFF2D55)

/* office brand colors — same words people already know */
val WordBlue = Color(0xFF2B579A)
val ExcelGreen = Color(0xFF1F7A4D)
val PptOrange = Color(0xFFD24726)
val PdfRed = Color(0xFFE5322D)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF00325C),
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E4FF),
    onSecondaryContainer = Color(0xFF2A2870),
    tertiary = Mint,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFEAEAF0),
    onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFD3D3DB),
    outlineVariant = Color(0xFFE4E4EA),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4CA6FF),
    onPrimary = Color(0xFF00254A),
    primaryContainer = Color(0xFF123A63),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF9F9DFF),
    onSecondary = Color(0xFF1B1A45),
    secondaryContainer = Color(0xFF2A2952),
    onSecondaryContainer = Color(0xFFE6E4FF),
    tertiary = Color(0xFF4CD97B),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF9E9EA4),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    error = Color(0xFFFF6961),
    onError = Color(0xFF3A0000)
)

private val VellumTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        letterSpacing = 1.1.sp
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
