package com.raisoftlabs.rsoffice.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ---- RS Office light palette (original, office-familiar but distinct) ---- */
val Indigo = Color(0xFF2F6BFF)      // primary electric blue
val IndigoDeep = Color(0xFF1746C8)
val Violet = Color(0xFF7B5CFF)
val Mint = Color(0xFF12A06A)
val Amber = Color(0xFFF59E0B)
val Rose = Color(0xFFF04438)
val Teal = Color(0xFF0EA5A4)
val Pink = Color(0xFFEC4899)

/* document-type colors — familiar categories, our own hues */
val WordBlue = Color(0xFF3B6EF6)
val ExcelGreen = Color(0xFF12A06A)
val PptOrange = Color(0xFFF2762E)
val PdfRed = Color(0xFFE5484D)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EBFF),
    onPrimaryContainer = Color(0xFF173E9E),
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE8FF),
    onSecondaryContainer = Color(0xFF3B2B8C),
    tertiary = Mint,
    background = Color(0xFFFAFBFF),
    onBackground = Color(0xFF151A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151A2E),
    surfaceVariant = Color(0xFFEEF1F9),
    onSurfaceVariant = Color(0xFF606A85),
    outline = Color(0xFFD7DEEE),
    outlineVariant = Color(0xFFE8ECF6),
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

private val RSOfficeTypography = Typography(
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
fun RSOfficeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = RSOfficeTypography,
        content = content
    )
}
