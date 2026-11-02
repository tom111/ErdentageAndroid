// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = InkSlateLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E9F1),
    onPrimaryContainer = Color(0xFF171E2B),
    secondary = Color(0xFF5E626B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E6E9),
    onSecondaryContainer = Color(0xFF202126),
    tertiary = Color(0xFF665F42),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE5BD),
    onTertiaryContainer = Color(0xFF211E0A),
    background = Color.White,
    onBackground = Color(0xFF171717),
    surface = Color.White,
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFF0F0F2),
    onSurfaceVariant = Color(0xFF62636A),
    outline = Color(0xFF77787E),
)

private val DarkColors = darkColorScheme(
    primary = InkSlateDark,
    onPrimary = Color(0xFF29261F),
    primaryContainer = Color(0xFF3A3B42),
    onPrimaryContainer = InkSlateDark,
    secondary = Color(0xFFC7C4C0),
    onSecondary = Color(0xFF2E2D2B),
    secondaryContainer = Color(0xFF454549),
    onSecondaryContainer = Color(0xFFE9E7E4),
    tertiary = Color(0xFFD0C68F),
    onTertiary = Color(0xFF37320E),
    tertiaryContainer = Color(0xFF4D4825),
    onTertiaryContainer = Color(0xFFEDE5BD),
    background = Color.Black,
    onBackground = Color(0xFFF5F3EF),
    surface = Color.Black,
    onSurface = Color(0xFFF5F3EF),
    surfaceVariant = Color(0xFF242428),
    onSurfaceVariant = Color(0xFFAAA8AA),
    outline = Color(0xFF919094),
)

private val DefaultTypography = Typography()

private val ErdentageTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 78.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = DefaultTypography.displayMedium.copy(letterSpacing = 0.sp),
    displaySmall = DefaultTypography.displaySmall.copy(letterSpacing = 0.sp),
    headlineMedium = DefaultTypography.headlineMedium.copy(letterSpacing = 0.sp),
    headlineSmall = DefaultTypography.headlineSmall.copy(letterSpacing = 0.sp),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(letterSpacing = 0.sp),
    titleSmall = DefaultTypography.titleSmall.copy(letterSpacing = 0.sp),
    bodyLarge = DefaultTypography.bodyLarge.copy(letterSpacing = 0.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(letterSpacing = 0.sp),
    bodySmall = DefaultTypography.bodySmall.copy(letterSpacing = 0.sp),
    labelLarge = DefaultTypography.labelLarge.copy(letterSpacing = 0.sp),
    labelMedium = DefaultTypography.labelMedium.copy(letterSpacing = 0.sp),
    labelSmall = DefaultTypography.labelSmall.copy(letterSpacing = 0.sp),
)

private val ErdentageShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
)

internal val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun ErdentageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ErdentageTypography,
            shapes = ErdentageShapes,
            content = content,
        )
    }
}
