// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.domain.Constellation
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.TogetherEvent
import de.eigenraum.erdentage.domain.TogetherKind
import de.eigenraum.erdentage.domain.TogetherMath
import java.time.LocalDate

/**
 * Single-slot memo in front of [TogetherMath]. The generators enumerate
 * every subset of the family, and the UI state is rebuilt on every clock
 * tick while a timed person is tracked. Their inputs only move when the
 * people change, the budgets change or the day rolls over, so the key is
 * exactly those three.
 *
 * Unlike iOS, the zone is not part of the key: [TogetherEvent.date] is a
 * civil `LocalDate`, not an instant, so a cached row prints the same day in
 * every zone. The civil `today` already carries the zone's influence.
 */
class TogetherContentCache {
    private data class EventKey(
        val people: List<Person>,
        val today: LocalDate,
        val limits: Map<TogetherKind, Int>,
    )

    private val lock = Any()
    private var eventKey: EventKey? = null
    private var cachedEvents: List<TogetherEvent> = emptyList()
    private var constellationPeople: List<Person>? = null
    private var cachedConstellations: List<Constellation> = emptyList()

    fun events(
        people: List<Person>,
        today: LocalDate,
        limits: Map<TogetherKind, Int>,
    ): List<TogetherEvent> = synchronized(lock) {
        val key = EventKey(people, today, limits)
        if (eventKey != key) {
            eventKey = key
            cachedEvents = TogetherMath.events(people, today) { kind -> limits.getValue(kind) }
        }
        cachedEvents
    }

    fun constellations(people: List<Person>): List<Constellation> = synchronized(lock) {
        if (constellationPeople != people) {
            constellationPeople = people
            cachedConstellations = TogetherMath.constellations(people)
        }
        cachedConstellations
    }
}
