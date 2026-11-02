// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
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
