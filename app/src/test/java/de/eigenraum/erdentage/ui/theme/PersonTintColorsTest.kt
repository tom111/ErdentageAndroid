// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.theme

import androidx.compose.ui.graphics.Color
import de.eigenraum.erdentage.domain.PersonTint
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonTintColorsTest {
    @Test
    fun surfaceColorsUseExactIosLightAndDarkValues() {
        assertColor(
            expected = Color(red = 0.710f, green = 0.847f, blue = 0.941f),
            actual = PersonTint.SKY.surfaceColor(darkTheme = false),
        )
        assertColor(
            expected = Color(red = 0.14f, green = 0.34f, blue = 0.22f),
            actual = PersonTint.MINT.surfaceColor(darkTheme = true),
        )
    }

    @Test
    fun lanternUsesSurfaceInLightModeAndBrighterVariantInDarkMode() {
        assertColor(
            expected = PersonTint.CORAL.surfaceColor(darkTheme = false),
            actual = PersonTint.CORAL.lanternColor(darkTheme = false),
        )
        assertColor(
            expected = Color(red = 0.82f, green = 0.45f, blue = 0.38f),
            actual = PersonTint.CORAL.lanternColor(darkTheme = true),
        )
    }

    @Test
    fun inkSlateAdaptsForDayAndNightContrast() {
        assertColor(InkSlateLight, inkSlateColor(darkTheme = false))
        assertColor(InkSlateDark, inkSlateColor(darkTheme = true))
    }

    private fun assertColor(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.0001f)
        assertEquals(expected.green, actual.green, 0.0001f)
        assertEquals(expected.blue, actual.blue, 0.0001f)
        assertEquals(expected.alpha, actual.alpha, 0.0001f)
    }
}
