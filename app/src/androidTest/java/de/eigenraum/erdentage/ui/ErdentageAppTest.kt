// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.eigenraum.erdentage.app.ErdentageUiState
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.ui.theme.ErdentageTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale
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
                    onCalendarHandoff = {},
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
                    onCalendarHandoff = {},
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
    fun personTopControlsShareAndOpenSettings() {
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
                    onCalendarHandoff = {},
                    onShare = { sharedText = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Erdentage teilen").performClick()
        composeRule.runOnIdle {
            check(sharedText?.contains("Anne ist heute") == true)
        }

        composeRule.onNodeWithContentDescription("Einstellungen").performClick()
        composeRule.onNodeWithText("Geburt").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abbrechen").performClick()
    }

    @Test
    fun settingsFeedbackPreservesSpacesAroundLinks() {
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
                    onCalendarHandoff = {},
                    onShare = {},
                )
            }
        }

        val feedback = "Feedback gerne per Mail oder Mastodon"
        composeRule.onNodeWithContentDescription("Einstellungen").performClick()
        composeRule.onNodeWithTag("person_settings_list")
            .performScrollToNode(hasText(feedback))
        composeRule.onNodeWithText(feedback).assertIsDisplayed()
    }

    @Test
    fun roundMilestonesShowBothCaptions() {
        val persisted = PersistedAppState(
            people = listOf(
                Person(
                    id = "11111111-1111-4111-8111-111111111111",
                    name = "Anne",
                    birthYear = 1973,
                    birthMonth = 1,
                    birthDay = 1,
                    tint = PersonTint.SKY,
                    enabledMilestones = setOf(
                        MilestoneKind.TAUSENDER,
                        MilestoneKind.ZEHNTAUSENDER,
                    ),
                ),
            ),
        )
        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = uiState(persisted),
                    onAddFirstPerson = {},
                    onAddPerson = {},
                    onUpdatePerson = {},
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onCalendarHandoff = {},
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithText("nächster Tausender").assertIsDisplayed()
        composeRule.onNodeWithText("nächster Zehntausender").assertIsDisplayed()
    }

    @Test
    fun milestoneMenuSharesAndHandsOffCalendarDraft() {
        var sharedText: String? = null
        var calendarAction: de.eigenraum.erdentage.ui.person.MilestoneCalendarAction? = null
        val state = uiState(onePersonState())
        val firstMilestone = state.personPages.single().snapshot.milestones.first()
        val count = NumberFormat.getIntegerInstance(Locale.GERMANY)
            .format(firstMilestone.target)

        composeRule.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = state,
                    onAddFirstPerson = {},
                    onAddPerson = {},
                    onUpdatePerson = {},
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onCalendarHandoff = { calendarAction = it },
                    onShare = { sharedText = it },
                )
            }
        }

        val actionsDescription = "Aktionen für $count Erdentage"
        composeRule.onAllNodesWithContentDescription(actionsDescription)[0].performClick()
        composeRule.onNodeWithText("Meilenstein teilen").performClick()
        composeRule.runOnIdle {
            val text = checkNotNull(sharedText)
            check(text.startsWith("Am "))
            check(text.contains("hat Anne $count Erdentage"))
        }

        composeRule.onAllNodesWithContentDescription(actionsDescription)[0].performClick()
        composeRule.onNodeWithText("Zum Kalender hinzufügen").performClick()
        composeRule.runOnIdle {
            val action = checkNotNull(calendarAction)
            check(action.personId == onePersonState().people.single().id)
            check(action.event.title.contains("$count Erdentage"))
            check(action.event.allDay)
            check(action.visibleKeys.contains(action.milestoneKey))
        }
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
                    onCalendarHandoff = {},
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Personen verwalten").performClick()
        composeRule.onNodeWithText("Personen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Person hinzufügen").performClick()
        composeRule.onNodeWithText("Neue Person").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Name")).performTextInput("Clara")
        composeRule.onNodeWithContentDescription("Speichern").performClick()

        composeRule.runOnIdle {
            val saved = checkNotNull(added)
            check(saved.name == "Clara")
            check(saved.id != onePersonState().people.single().id)
        }
    }

    @Test
    fun managementOffersContactPickerAndImportsCompletedDraftIntoEditor() {
        var pickerRequested = false
        var consumed = false
        val imported = de.eigenraum.erdentage.platform.ContactPersonDraft(
            name = "Clara",
            birthDate = LocalDate.of(1992, 4, 3),
            token = "33333333-3333-4333-8333-333333333333",
        )

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
                    onCalendarHandoff = {},
                    onShare = {},
                    contactDraft = imported,
                    onPickContact = { pickerRequested = true },
                    onContactDraftConsumed = { consumed = true },
                )
            }
        }

        composeRule.onNode(hasSetTextAction() and hasText("Clara")).assertIsDisplayed()
        composeRule.runOnIdle { check(consumed) }
        composeRule.onNodeWithContentDescription("Abbrechen").performClick()

        composeRule.onNodeWithText("Personen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Person hinzufügen").performClick()
        composeRule.onNodeWithText("Aus Kontakten…").performClick()
        composeRule.runOnIdle { check(pickerRequested) }
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
                    onCalendarHandoff = {},
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

    @Test
    fun editorDraftAndManagerReturnSurviveStateRestoration() {
        var updated: Person? = null
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            ErdentageTheme(darkTheme = false) {
                ErdentageApp(
                    state = uiState(onePersonState()),
                    onAddFirstPerson = {},
                    onAddPerson = {},
                    onUpdatePerson = { updated = it },
                    onDeletePerson = {},
                    onMovePerson = { _, _ -> },
                    onPageSelected = {},
                    onCalendarHandoff = {},
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Personen verwalten").performClick()
        composeRule.onNodeWithContentDescription("Listenoptionen für Anne").performClick()
        composeRule.onNodeWithText("Einstellungen").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Anne"))
            .performTextReplacement("Anne Neu")
        composeRule.onNodeWithTag("person_settings_list")
            .performScrollToNode(hasTestTag("days_off_earth_input"))
        composeRule.onNodeWithTag("days_off_earth_input").performTextClearance()
        composeRule.onNodeWithTag("days_off_earth_input").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString(""),
            ),
        )
        composeRule.onNodeWithTag("days_off_earth_input").performTextInput("42")

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("days_off_earth_input").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString("42"),
            ),
        )
        composeRule.onNodeWithContentDescription("Speichern").performClick()
        composeRule.runOnIdle {
            val saved = checkNotNull(updated)
            check(saved.name == "Anne Neu")
            check(saved.daysOffEarth == 42)
        }
        composeRule.onNodeWithText("Personen").assertIsDisplayed()
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
