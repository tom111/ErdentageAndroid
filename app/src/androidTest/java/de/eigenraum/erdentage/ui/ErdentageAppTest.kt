// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
                    onAddPerson = {},
                    onUpdatePerson = {},
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onShare = {},
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
                    onAddPerson = {},
                    onUpdatePerson = {},
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onShare = {},
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

    @Test
    fun personOptionsOpenSettingsAndShareCurrentCount() {
        var sharedText: String? = null
        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = uiState(onePersonState()),
                    onAddFirstPerson = {},
                    onAddPerson = {},
                    onUpdatePerson = {},
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onShare = { sharedText = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Optionen für Anne").performClick()
        composeRule.onNodeWithText("Erdentage teilen").performClick()
        composeRule.runOnIdle {
            check(sharedText?.contains("Anne ist heute") == true)
        }

        composeRule.onNodeWithContentDescription("Optionen für Anne").performClick()
        composeRule.onNodeWithText("Einstellungen").performClick()
        composeRule.onNodeWithText("Geburt").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abbrechen").performClick()
    }

    @Test
    fun managementAddsNamedPersonThroughEditor() {
        var added: Person? = null
        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = uiState(onePersonState()),
                    onAddFirstPerson = {},
                    onAddPerson = { added = it },
                    onUpdatePerson = {},
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Personen verwalten").performClick()
        composeRule.onNodeWithText("Personen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Person hinzufügen").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Name")).performTextInput("Clara")
        composeRule.onNodeWithContentDescription("Speichern").performClick()

        composeRule.runOnIdle {
            val saved = checkNotNull(added)
            check(saved.name == "Clara")
            check(saved.id != onePersonState().people.single().id)
        }
    }

    @Test
    fun managementExposesReorderAndDeleteActions() {
        var move: Pair<Int, Int>? = null
        var deletedId: String? = null
        val persisted = onePersonState().copy(
            people = onePersonState().people + Person(
                id = "22222222-2222-4222-8222-222222222222",
                name = "Bernd",
                birthYear = 1990,
                birthMonth = 3,
                birthDay = 15,
                tint = PersonTint.CORAL,
            ),
        )
        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = uiState(persisted),
                    onAddFirstPerson = {},
                    onAddPerson = {},
                    onUpdatePerson = {},
                    onDeletePerson = { deletedId = it },
                    onMovePerson = { from, to -> move = from to to },
                    onPageSelected = {},
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Personen verwalten").performClick()
        composeRule.onNodeWithContentDescription("Listenoptionen für Anne").performClick()
        composeRule.onNodeWithText("Nach unten").performClick()
        composeRule.runOnIdle { check(move == (0 to 1)) }

        composeRule.onNodeWithContentDescription("Listenoptionen für Bernd").performClick()
        composeRule.onNodeWithText("Person löschen").performClick()
        composeRule.onNodeWithText("Löschen").performClick()
        composeRule.runOnIdle {
            check(deletedId == "22222222-2222-4222-8222-222222222222")
        }
    }

    private fun onePersonState() = PersistedAppState(
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

    private fun uiState(persisted: PersistedAppState): ErdentageUiState =
        ErdentageUiState.create(
            persisted = persisted,
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )
}
