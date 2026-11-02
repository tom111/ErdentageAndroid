// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.theme

import androidx.compose.ui.graphics.Color
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.domain.Rgb

val InkSlateLight = Color(red = 0.20f, green = 0.25f, blue = 0.35f)
val InkSlateDark = Color(red = 0.97f, green = 0.94f, blue = 0.88f)

fun PersonTint.surfaceColor(darkTheme: Boolean): Color =
    (if (darkTheme) darkSurface else lightSurface).toComposeColor()

fun PersonTint.lanternColor(darkTheme: Boolean): Color =
    (if (darkTheme) darkLantern else lightSurface).toComposeColor()

fun inkSlateColor(darkTheme: Boolean): Color =
    if (darkTheme) InkSlateDark else InkSlateLight

private fun Rgb.toComposeColor(): Color = Color(
    red = red.toFloat(),
    green = green.toFloat(),
    blue = blue.toFloat(),
)
