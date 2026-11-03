// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import java.time.Instant
import java.time.ZoneId
import de.eigenraum.erdentage.domain.TogetherKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ErdentageUiStateTest {
    @Test
    fun createsOneSnapshotPerPersonAndChronologicalOverview() {
        val persisted = PersistedAppState(
            people = listOf(
                person("11111111-1111-4111-8111-111111111111", "Anne", 1980),
                person("22222222-2222-4222-8222-222222222222", "Bernd", 1990),
            ),
        )

        val state = ErdentageUiState.create(
            persisted = persisted,
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )

        assertEquals(listOf("Anne", "Bernd"), state.personPages.map { it.person.displayName })
        assertTrue(state.overviewEvents.size <= 30)
        assertEquals(
            state.overviewEvents.sortedBy { it.milestone.date.toInstant() },
            state.overviewEvents,
        )
    }

    @Test
    fun milestonesLandingOnATrackedBirthdayCarryTheNoteOnPageAndOverview() {
        // Anne's 17.000th day is 2026-07-19; Bernd was born on a 19 July.
        val anne = Person(
            id = "11111111-1111-4111-8111-111111111111",
            name = "Anne",
            birthYear = 1980,
            birthMonth = 1,
            birthDay = 2,
        )
        val bernd = Person(
            id = "22222222-2222-4222-8222-222222222222",
            name = "Bernd",
            birthYear = 2010,
            birthMonth = 7,
            birthDay = 19,
        )
        val persisted = PersistedAppState(people = listOf(anne, bernd))
        val at = Instant.parse("2026-07-01T12:00:00Z")
        val zone = ZoneId.of("Europe/Berlin")

        val state = ErdentageUiState.create(persisted, at, zone)

        val annePage = state.personPages.first()
        val thousand = annePage.snapshot.milestones.first { it.target == 17_000 }
        assertEquals("2026-07-19", thousand.date.toLocalDate().toString())
        assertEquals("🎂 an Bernds 16. Geburtstag", annePage.birthdayNotes[17_000]?.caption)
        val overviewRow = state.overviewEvents.first { it.person.id == anne.id && it.milestone.target == 17_000 }
        assertEquals("🎂 Bernd", overviewRow.birthday?.short)

        // The switch belongs to Bernd, and only gates the note.
        val muted = ErdentageUiState.create(
            persisted.copy(people = listOf(anne, bernd.copy(mentionsBirthday = false))),
            at,
            zone,
        )
        assertNull(muted.personPages.first().birthdayNotes[17_000])
        assertEquals(
            state.personPages.first().snapshot.milestones,
            muted.personPages.first().snapshot.milestones,
        )
    }

    @Test
    fun togetherContentExistsFromTwoPeopleOnwardAndHonoursTheBudgets() {
        val anne = person("11111111-1111-4111-8111-111111111111", "Anne", 1980)
        val bernd = person("22222222-2222-4222-8222-222222222222", "Bernd", 2010)
        val at = Instant.parse("2026-08-16T12:00:00Z")
        val zone = ZoneId.of("Europe/Berlin")

        val single = ErdentageUiState.create(PersistedAppState(people = listOf(anne)), at, zone)
        assertTrue(single.togetherEvents.isEmpty())
        assertTrue(single.togetherConstellations.isEmpty())

        val pair = ErdentageUiState.create(PersistedAppState(people = listOf(anne, bernd)), at, zone)
        assertEquals(java.time.LocalDate.of(2026, 8, 16), pair.today)
        assertTrue(pair.togetherEvents.any { it.kind == TogetherKind.DOPPELT })
        assertEquals(1, pair.togetherConstellations.size)

        val noDoubling = ErdentageUiState.create(
            PersistedAppState(
                people = listOf(anne, bernd),
                togetherLimits = mapOf(TogetherKind.DOPPELT to 0),
            ),
            at,
            zone,
        )
        assertTrue(noDoubling.togetherEvents.none { it.kind == TogetherKind.DOPPELT })
    }

    @Test
    fun togetherCacheRecomputesOnlyWhenPeopleDayOrBudgetsChange() {
        val anne = person("11111111-1111-4111-8111-111111111111", "Anne", 1980)
        val bernd = person("22222222-2222-4222-8222-222222222222", "Bernd", 2010)
        val persisted = PersistedAppState(people = listOf(anne, bernd))
        val zone = ZoneId.of("Europe/Berlin")
        val cache = TogetherContentCache()

        val noon = ErdentageUiState.create(persisted, Instant.parse("2026-08-16T10:00:00Z"), zone, cache)
        val evening = ErdentageUiState.create(persisted, Instant.parse("2026-08-16T18:00:00Z"), zone, cache)
        assertSame(noon.togetherEvents, evening.togetherEvents)
        assertSame(noon.togetherConstellations, evening.togetherConstellations)

        val nextDay = ErdentageUiState.create(persisted, Instant.parse("2026-08-17T10:00:00Z"), zone, cache)
        assertTrue(noon.togetherEvents !== nextDay.togetherEvents)
        assertSame(noon.togetherConstellations, nextDay.togetherConstellations)

        val budgeted = ErdentageUiState.create(
            persisted.copy(togetherLimits = mapOf(TogetherKind.JAHRHUNDERT to 1)),
            Instant.parse("2026-08-17T10:00:00Z"),
            zone,
            cache,
        )
        assertTrue(nextDay.togetherEvents !== budgeted.togetherEvents)
    }

    @Test
    fun emptyPersistedStateProducesNoPagesOrEvents() {
        val state = ErdentageUiState.create(
            persisted = PersistedAppState(),
            at = Instant.EPOCH,
            zoneId = ZoneId.of("Europe/Berlin"),
        )

        assertTrue(state.personPages.isEmpty())
        assertTrue(state.overviewEvents.isEmpty())
    }

    private fun person(id: String, name: String, year: Int) = Person(
        id = id,
        name = name,
        birthYear = year,
        birthMonth = 1,
        birthDay = 1,
    )
}
