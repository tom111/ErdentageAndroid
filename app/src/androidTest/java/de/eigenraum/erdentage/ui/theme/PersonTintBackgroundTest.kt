// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.eigenraum.erdentage.domain.PersonTint
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonTintBackgroundTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gradientReachesBackgroundAtMidpoint() {
        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                PersonTintBackground(
                    tint = PersonTint.SKY,
                    modifier = Modifier
                        .size(width = 100.dp, height = 200.dp)
                        .testTag("person-gradient"),
                )
            }
        }

        val image = composeRule.onNodeWithTag("person-gradient").captureToImage()
        val pixels = image.toPixelMap()
        val centerX = image.width / 2

        assertColorClose(
            expected = PersonTint.SKY.surfaceColor(darkTheme = false),
            actual = pixels[centerX, 1],
        )
        assertColorClose(
            expected = Color.White,
            actual = pixels[centerX, image.height / 2],
        )
        assertColorClose(
            expected = Color.White,
            actual = pixels[centerX, image.height - 2],
        )
    }

    private fun assertColorClose(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.02f)
        assertEquals(expected.green, actual.green, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
    }
}
