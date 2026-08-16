// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.eigenraum.erdentage.app.ErdentageUiState
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.ui.theme.ErdentageTheme
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErdentageAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateOffersBirthDateAction() {
        composeRule.setContent {
            ErdentageTheme {
                ErdentageApp(
                    state = uiState(PersistedAppState()),
                    onAddFirstPerson = {},
                    onPageSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Noch kein Geburtsdatum festgelegt.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Geburtsdatum festlegen").assertIsDisplayed()
    }

    @Test
    fun personPageSwipesToSingleOverviewPage() {
        val state = PersistedAppState(
            people = listOf(
                Person(
                    id = "11111111-1111-4111-8111-111111111111",
                    name = "Anne",
                    birthYear = 1980,
                    birthMonth = 7,
                    birthDay = 12,
                    tint = PersonTint.SKY,
                ),
            ),
        )
        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = uiState(state),
                    onAddFirstPerson = {},
                    onPageSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Erdentage").assertIsDisplayed()
        composeRule.onNodeWithText("Anne").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Seite 1 von 2").assertIsDisplayed()

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Demnächst").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Seite 2 von 2").assertIsDisplayed()
    }

    private fun uiState(persisted: PersistedAppState): ErdentageUiState =
        ErdentageUiState.create(
            persisted = persisted,
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )
}
