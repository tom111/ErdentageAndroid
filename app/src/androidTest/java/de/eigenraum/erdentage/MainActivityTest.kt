// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appContentIsDisplayed() {
        // The three-person debug seed yields five pages (three people,
        // „Demnächst“, „Zusammen“). The app restores the last page, so any
        // of them may be current when data from an earlier run survives.
        val fivePages = SemanticsMatcher("page indicator of five pages") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)
                ?.any { it.matches(Regex("Seite [1-5] von 5")) } == true
        }
        composeRule.onRoot().assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(fivePages).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(fivePages).assertIsDisplayed()
    }
}
