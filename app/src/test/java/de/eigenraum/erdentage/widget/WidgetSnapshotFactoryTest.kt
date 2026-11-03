// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.widget

import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSnapshotFactoryTest {
    private val people = listOf(
        Person(
            id = "11111111-1111-4111-8111-111111111111",
            name = "Anne",
            birthYear = 2000,
            birthMonth = 1,
            birthDay = 1,
        ),
        Person(
            id = "22222222-2222-4222-8222-222222222222",
            name = "Bernd",
            birthYear = 1990,
            birthMonth = 1,
            birthDay = 1,
        ),
    )

    @Test
    fun fixedSelectionSurvivesCurrentPageChanges() {
        val snapshot = WidgetSnapshotFactory.create(
            state = PersistedAppState(people = people, currentPersonIndex = 1),
            selection = WidgetSelection.fixed(people.first().id),
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )
        assertEquals("Anne", snapshot.personName)
    }

    @Test
    fun followSelectionUsesCurrentPersonAndOverviewFallsBackToFirst() {
        val current = WidgetSnapshotFactory.create(
            state = PersistedAppState(people = people, currentPersonIndex = 1),
            selection = WidgetSelection.FollowCurrent,
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )
        val overview = WidgetSnapshotFactory.create(
            state = PersistedAppState(people = people, currentPersonIndex = people.size),
            selection = WidgetSelection.FollowCurrent,
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )
        val together = WidgetSnapshotFactory.create(
            state = PersistedAppState(people = people, currentPersonIndex = people.size + 1),
            selection = WidgetSelection.FollowCurrent,
            at = Instant.parse("2026-08-16T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Berlin"),
        )
        assertEquals("Bernd", current.personName)
        assertEquals("Anne", overview.personName)
        assertEquals("Anne", together.personName)
    }
}
