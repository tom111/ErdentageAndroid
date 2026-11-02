// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayCountLogicTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun dateOnlyCountChangesAtLocalMidnight() {
        val person = person(hasTime = false)
        val before = ZonedDateTime.of(2026, 5, 25, 23, 59, 59, 0, berlin).toInstant()
        val after = ZonedDateTime.of(2026, 5, 26, 0, 0, 0, 0, berlin).toInstant()

        val beforeSnapshot = DayCountLogic.snapshotAt(before, berlin, person, 0)
        val afterSnapshot = DayCountLogic.snapshotAt(after, berlin, person, 0)

        assertEquals(beforeSnapshot.days + 1, afterSnapshot.days)
        assertFalse(beforeSnapshot.showTime)
    }

    @Test
    fun timedCountChangesAtStoredBirthTimeAndReportsResidualTime() {
        val person = person(hasTime = true, birthHour = 14, birthMinute = 30)
        val before = ZonedDateTime.of(2026, 5, 25, 14, 29, 0, 0, berlin).toInstant()
        val boundary = ZonedDateTime.of(2026, 5, 25, 14, 30, 0, 0, berlin).toInstant()
        val residual = ZonedDateTime.of(2026, 5, 25, 15, 45, 0, 0, berlin).toInstant()

        val beforeSnapshot = DayCountLogic.snapshotAt(before, berlin, person, 0)
        val boundarySnapshot = DayCountLogic.snapshotAt(boundary, berlin, person, 0)
        val residualSnapshot = DayCountLogic.snapshotAt(residual, berlin, person, 0)

        assertEquals(beforeSnapshot.days + 1, boundarySnapshot.days)
        assertEquals(1, residualSnapshot.hours)
        assertEquals(15, residualSnapshot.minutes)
        assertTrue(residualSnapshot.showTime)
    }

    @Test
    fun daysOffEarthChangesDisplayedCountWithoutChangingResidualTime() {
        val at = ZonedDateTime.of(2026, 5, 25, 15, 45, 0, 0, berlin).toInstant()
        val earthbound = DayCountLogic.snapshotAt(
            at,
            berlin,
            person(hasTime = true, birthHour = 14, birthMinute = 30),
            0,
        )
        val adjusted = DayCountLogic.snapshotAt(
            at,
            berlin,
            person(hasTime = true, birthHour = 14, birthMinute = 30, daysOffEarth = 7),
            0,
        )

        assertEquals(earthbound.days - 7, adjusted.days)
        assertEquals(earthbound.hours, adjusted.hours)
        assertEquals(earthbound.minutes, adjusted.minutes)
    }

    @Test
    fun elapsedPartsMatchFoundationAcrossSpringDstGap() {
        val person = person(
            birthYear = 2024,
            birthMonth = 3,
            birthDay = 30,
            birthHour = 2,
            birthMinute = 30,
            hasTime = true,
        )
        val at = ZonedDateTime.of(2024, 3, 31, 3, 0, 0, 0, berlin).toInstant()

        val snapshot = DayCountLogic.snapshotAt(at, berlin, person, 0)

        assertEquals(0, snapshot.days)
        assertEquals(23, snapshot.hours)
        assertEquals(30, snapshot.minutes)
    }

    @Test
    fun futureAndInvalidBirthDatesProduceDefensiveSnapshots() {
        val at = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, berlin).toInstant()
        val future = person(birthYear = 2030)
        val invalid = person(birthMonth = 2, birthDay = 31)

        val futureSnapshot = DayCountLogic.snapshotAt(at, berlin, future, 0)
        val invalidSnapshot = DayCountLogic.snapshotAt(at, berlin, invalid, 0)

        assertEquals(0, futureSnapshot.days)
        assertEquals(0, futureSnapshot.hours)
        assertEquals(0, futureSnapshot.minutes)
        assertTrue(futureSnapshot.hasBirthDate)
        assertFalse(invalidSnapshot.hasBirthDate)
    }

    @Test
    fun civilBirthdayDoesNotShiftWhenZoneChanges() {
        val person = person(
            birthYear = 1990,
            birthMonth = 3,
            birthDay = 15,
            birthHour = 14,
            birthMinute = 30,
            hasTime = true,
        )

        val berlinBirth = requireNotNull(person.birthDateTime(berlin))
        val losAngelesBirth = requireNotNull(person.birthDateTime(ZoneId.of("America/Los_Angeles")))

        assertEquals(LocalDate.of(1990, 3, 15), berlinBirth.toLocalDate())
        assertEquals(LocalDate.of(1990, 3, 15), losAngelesBirth.toLocalDate())
        assertEquals(14, berlinBirth.hour)
        assertEquals(14, losAngelesBirth.hour)
    }

    @Test
    fun nextRolloverUsesBirthTimeAndHandlesDstGapAndOverlap() {
        val person = person(hasTime = true, birthHour = 2, birthMinute = 30)

        val ordinary = DayCountLogic.nextRolloverAfter(
            ZonedDateTime.of(2026, 5, 25, 1, 0, 0, 0, berlin).toInstant(),
            berlin,
            person,
        )
        assertEquals(
            ZonedDateTime.of(2026, 5, 25, 2, 30, 0, 0, berlin).toInstant(),
            ordinary,
        )

        val springGap = DayCountLogic.nextRolloverAfter(
            Instant.parse("2024-03-31T00:00:00Z"),
            berlin,
            person,
        )
        assertEquals(Instant.parse("2024-03-31T01:00:00Z"), springGap)

        val autumnSecondOccurrence = DayCountLogic.nextRolloverAfter(
            Instant.parse("2024-10-27T00:45:00Z"),
            berlin,
            person,
        )
        assertEquals(Instant.parse("2024-10-27T01:30:00Z"), autumnSecondOccurrence)
    }

    @Test
    fun dateOnlyRolloverUsesNextLocalMidnight() {
        val after = ZonedDateTime.of(2026, 5, 25, 13, 0, 0, 0, berlin).toInstant()

        assertEquals(
            ZonedDateTime.of(2026, 5, 26, 0, 0, 0, 0, berlin).toInstant(),
            DayCountLogic.nextRolloverAfter(after, berlin, person(hasTime = false)),
        )
    }

    @Test
    fun configuredPersonResolutionPreservesOverviewFallbackRule() {
        val firstId = "00000000-0000-0000-0000-000000000000"
        val secondId = "00000000-0000-0000-0000-000000000001"
        val people = listOf(person(id = firstId), person(id = secondId, birthYear = 2001))

        val configured = DayCountLogic.resolve(secondId, people, currentPersonIndex = people.size)
        val fallback = DayCountLogic.resolve("missing", people, currentPersonIndex = people.size)

        assertEquals(secondId, configured?.first?.id)
        assertEquals(1, configured?.second)
        assertEquals(firstId, fallback?.first?.id)
        assertEquals(0, fallback?.second)
        assertNull(DayCountLogic.resolve(null, emptyList(), 0))
    }

    @Test
    fun injectedClockAndZoneDriveSnapshotDeterministically() {
        val instant = Instant.parse("2026-05-25T13:45:00Z")
        val logic = DayCountLogic(Clock.fixed(instant, ZoneOffset.UTC)) { berlin }

        val snapshot = logic.snapshot(person(hasTime = true, birthHour = 14, birthMinute = 30), 0)

        assertEquals(instant, snapshot.date)
        assertEquals(1, snapshot.hours)
        assertEquals(15, snapshot.minutes)
        assertNotNull(logic.nextRollover(person(hasTime = true, birthHour = 14, birthMinute = 30)))
    }

    @Test
    fun milestoneKeysUseCivilDateAndOptionalTime() {
        val date = ZonedDateTime.of(2026, 5, 5, 14, 30, 0, 0, berlin)

        assertEquals("13000-20260505", DayCountLogic.milestoneKey(13_000, date, false))
        assertEquals("13000-20260505-1430", DayCountLogic.milestoneKey(13_000, date, true))
    }

    private fun person(
        id: String = "11111111-2222-3333-4444-555555555555",
        birthYear: Int = 2000,
        birthMonth: Int = 1,
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
}
