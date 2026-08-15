// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.domain

import de.eigenraum.erdentage.data.ErdentageJson
import de.eigenraum.erdentage.data.PersistedAppState
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun civilBirthDateIgnoresStoredTimeWhenTimeIsUnknown() {
        val person = person(birthHour = 23, birthMinute = 59, hasTime = false)

        assertEquals(0, person.birthDateTime(berlin)?.hour)
        assertEquals(0, person.birthDateTime(berlin)?.minute)
    }

    @Test
    fun civilBirthDateUsesStoredTimeWhenKnown() {
        val person = person(birthHour = 14, birthMinute = 30, hasTime = true)

        assertEquals(14, person.birthDateTime(berlin)?.hour)
        assertEquals(30, person.birthDateTime(berlin)?.minute)
    }

    @Test
    fun invalidCivilComponentsAreRejectedInsteadOfNormalized() {
        assertNull(person(birthMonth = 13, birthDay = 32).birthDateTime(berlin))
        assertNull(person(birthYear = 2024, birthMonth = 2, birthDay = 31).birthDateTime(berlin))
        assertNull(person(birthYear = 2023, birthMonth = 2, birthDay = 29).birthDateTime(berlin))
        assertNull(person(birthYear = 0).birthDateTime(berlin))
        assertEquals(29, person(birthYear = 2024, birthMonth = 2, birthDay = 29).birthDateTime(berlin)?.dayOfMonth)
    }

    @Test
    fun nonexistentDstWallTimeIsRejected() {
        val skippedTime = person(
            birthYear = 2024,
            birthMonth = 3,
            birthDay = 31,
            birthHour = 2,
            birthMinute = 30,
            hasTime = true,
        )

        assertNull(skippedTime.birthDateTime(berlin))
    }

    @Test
    fun normalizationClampsScalarRangesButDoesNotRepairCivilDate() {
        val normalized = person(
            birthMonth = 13,
            birthDay = 32,
            birthHour = 99,
            birthMinute = -5,
            hasTime = true,
            daysOffEarth = -12,
        ).normalizedForStorage()

        assertEquals(13, normalized.birthMonth)
        assertEquals(32, normalized.birthDay)
        assertEquals(23, normalized.birthHour)
        assertEquals(0, normalized.birthMinute)
        assertEquals(0, normalized.daysOffEarth)
        assertNull(normalized.birthDateTime(berlin))
    }

    @Test
    fun ownerNameAndDisplayRulesMatchIos() {
        assertEquals("Ich", PersonName.display(" \n "))
        assertEquals("Lisa", PersonName.display("Lisa"))
        assertTrue(PersonName.isOwner(""))
        assertTrue(PersonName.isOwner("  ich  "))
        assertFalse(PersonName.isOwner("Lisa"))
    }

    @Test
    fun automaticTintCyclesDefensivelyAndNeverUsesWhite() {
        val person = person()
        assertEquals(PersonTint.SAND, person.resolvedTint(0))
        assertEquals(PersonTint.STONE, person.resolvedTint(-1))
        assertEquals(PersonTint.SAND, person.resolvedTint(PersonTint.automaticEntries.size))
        assertFalse(PersonTint.WHITE in PersonTint.automaticEntries)
        assertTrue(PersonTint.WHITE in PersonTint.entries)

        val explicit = person.copy(tint = PersonTint.OCEAN)
        assertEquals(PersonTint.OCEAN, explicit.resolvedTint(0))
        assertEquals(PersonTint.OCEAN, explicit.resolvedTint(9))
    }

    @Test
    fun firstUnusedTintSkipsUsedColorsAndFallsBackWithoutWhite() {
        val people = PersonTint.automaticEntries.mapIndexed { index, tint ->
            person(id = uuid(index)).copy(tint = tint)
        }
        assertEquals(PersonTint.CORAL, PersonTint.firstUnused(listOf(people.first())))
        assertEquals(PersonTint.SAND, PersonTint.firstUnused(people))
    }

    @Test
    fun versionedJsonRoundTripPreservesModelAndStableStringId() {
        val original = PersistedAppState(
            people = listOf(
                person(
                    id = "11111111-2222-3333-4444-555555555555",
                    birthHour = 14,
                    birthMinute = 30,
                    hasTime = true,
                    daysOffEarth = 7,
                ).copy(
                    addedMilestones = listOf(
                        AddedMilestoneRecord("lisa-10000", Instant.parse("2026-08-15T12:00:00Z")),
                    ),
                    enabledMilestones = setOf(MilestoneKind.TAUSENDER, MilestoneKind.CATALAN),
                    tint = PersonTint.LAVENDER,
                ),
            ),
        )

        val encoded = ErdentageJson.encodeToString(original)
        val decoded = ErdentageJson.decodeFromString<PersistedAppState>(encoded)

        assertEquals(original, decoded)
        assertEquals(1, decoded.schemaVersion)
        assertEquals("11111111-2222-3333-4444-555555555555", decoded.people.single().id)
    }

    @Test
    fun persistedStateCapsPeopleAndPreservesOverviewSentinel() {
        val people = (0..ErdentageLimits.MAX_PEOPLE).map { person(id = uuid(it)) }
        val state = PersistedAppState(
            people = people,
            currentPersonIndex = ErdentageLimits.MAX_PEOPLE,
        ).normalized()

        assertEquals(ErdentageLimits.MAX_PEOPLE, state.people.size)
        assertEquals(state.people.size, state.currentPersonIndex)
        assertEquals(19, state.copy(currentPersonIndex = 999).normalized().currentPersonIndex)
        assertEquals(0, PersistedAppState(currentPersonIndex = 9).normalized().currentPersonIndex)
    }

    @Test
    fun milestoneBackfillReturnsOnlyVersionsNotYetApplied() {
        assertEquals(
            setOf(MilestoneKind.ZWEIERPOTENZ, MilestoneKind.ZIFFERNFOLGE, MilestoneKind.FIBONACCI),
            ErdentageLimits.milestoneKindsIntroducedAfter(0),
        )
        assertEquals(
            setOf(MilestoneKind.FIBONACCI),
            ErdentageLimits.milestoneKindsIntroducedAfter(1),
        )
        assertTrue(ErdentageLimits.milestoneKindsIntroducedAfter(2).isEmpty())
    }

    @Test
    fun persistedBackfillAppliesOnlyDeltaAndPreservesEarlierOptOut() {
        val stored = person().copy(
            enabledMilestones = MilestoneKind.defaultEnabled -
                MilestoneKind.ZWEIERPOTENZ -
                MilestoneKind.FIBONACCI,
        )

        val normalized = PersistedAppState(
            people = listOf(stored),
            milestoneBackfillVersion = 1,
        ).normalized()

        assertEquals(2, normalized.milestoneBackfillVersion)
        assertTrue(MilestoneKind.FIBONACCI in normalized.people.single().enabledMilestones)
        assertFalse(MilestoneKind.ZWEIERPOTENZ in normalized.people.single().enabledMilestones)
    }

    private fun person(
        id: String = "11111111-2222-3333-4444-555555555555",
        birthYear: Int = 2000,
        birthMonth: Int = 6,
        birthDay: Int = 1,
        birthHour: Int = 0,
        birthMinute: Int = 0,
        hasTime: Boolean = false,
        daysOffEarth: Int = 0,
    ): Person = Person(
        id = id,
        name = "Lisa",
        birthYear = birthYear,
        birthMonth = birthMonth,
        birthDay = birthDay,
        birthHour = birthHour,
        birthMinute = birthMinute,
        hasTime = hasTime,
        daysOffEarth = daysOffEarth,
    )

    private fun uuid(index: Int): String = "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"
}
