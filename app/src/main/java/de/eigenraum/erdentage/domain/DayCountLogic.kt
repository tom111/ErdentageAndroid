// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.zone.ZoneOffsetTransition
import java.util.Locale

data class DayCountSnapshot(
    val date: Instant,
    val personName: String,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val showTime: Boolean,
    val milestones: List<MilestoneCandidate>,
    val tint: PersonTint?,
    val hasBirthDate: Boolean,
) {
    companion object {
        fun empty(at: Instant): DayCountSnapshot = DayCountSnapshot(
            date = at,
            personName = "",
            days = 0,
            hours = 0,
            minutes = 0,
            showTime = false,
            milestones = emptyList(),
            tint = null,
            hasBirthDate = false,
        )
    }
}

class DayCountLogic(
    private val clock: Clock,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
) {
    fun snapshot(person: Person?, index: Int): DayCountSnapshot =
        snapshotAt(clock.instant(), zoneProvider(), person, index)

    fun nextRollover(person: Person?): Instant =
        nextRolloverAfter(clock.instant(), zoneProvider(), person)

    companion object {
        fun resolve(
            personId: String?,
            people: List<Person>,
            currentPersonIndex: Int,
        ): Pair<Person, Int>? {
            if (people.isEmpty()) return null
            if (personId != null) {
                val configuredIndex = people.indexOfFirst { it.id == personId }
                if (configuredIndex >= 0) return people[configuredIndex] to configuredIndex
            }
            val fallbackIndex = currentPersonIndex.takeIf(people.indices::contains) ?: 0
            return people[fallbackIndex] to fallbackIndex
        }

        fun snapshotAt(
            at: Instant,
            zoneId: ZoneId,
            person: Person?,
            index: Int,
        ): DayCountSnapshot {
            val birth = person?.birthDateTime(zoneId) ?: return DayCountSnapshot.empty(at)
            val now = at.atZone(zoneId)
            val elapsed = elapsedCalendarParts(birth, now)
            val daysOffEarth = ErdentageLimits.normalizeDaysOffEarth(person.daysOffEarth)
            val displayedDays = (elapsed.days - daysOffEarth)
                .coerceIn(0, ErdentageLimits.MAX_DISPLAYED_DAYS)

            return DayCountSnapshot(
                date = at,
                personName = person.displayName,
                days = displayedDays,
                hours = elapsed.hours,
                minutes = elapsed.minutes,
                showTime = person.hasTime,
                milestones = MilestoneMath.upcomingMilestones(
                    displayedDays = displayedDays,
                    birth = birth,
                    daysOffEarth = daysOffEarth,
                    enabled = person.enabledMilestones,
                ),
                tint = person.resolvedTint(index),
                hasBirthDate = true,
            )
        }

        fun nextRolloverAfter(
            after: Instant,
            zoneId: ZoneId,
            person: Person?,
        ): Instant {
            val validTimedPerson = person?.takeIf { it.hasTime && it.birthDateTime(zoneId) != null }
            if (validTimedPerson == null) return nextStartOfDay(after, zoneId)
            val time = LocalTime.of(
                validTimedPerson.birthHour.coerceIn(0, 23),
                validTimedPerson.birthMinute.coerceIn(0, 59),
            )
            return nextLocalTime(after, zoneId, time)
        }

        fun milestoneKey(target: Int, date: ZonedDateTime, hasTime: Boolean): String =
            if (hasTime) {
                String.format(
                    Locale.ROOT,
                    "%d-%04d%02d%02d-%02d%02d",
                    target,
                    date.year,
                    date.monthValue,
                    date.dayOfMonth,
                    date.hour,
                    date.minute,
                )
            } else {
                String.format(
                    Locale.ROOT,
                    "%d-%04d%02d%02d",
                    target,
                    date.year,
                    date.monthValue,
                    date.dayOfMonth,
                )
            }

        private data class ElapsedParts(val days: Int, val hours: Int, val minutes: Int)

        private fun elapsedCalendarParts(birth: ZonedDateTime, now: ZonedDateTime): ElapsedParts {
            if (now.isBefore(birth)) return ElapsedParts(0, 0, 0)

            var days = java.time.temporal.ChronoUnit.DAYS.between(
                birth.toLocalDate(),
                now.toLocalDate(),
            ).toInt()
            var cursor = birth.plusDays(days.toLong())
            while (cursor.isAfter(now)) {
                days--
                cursor = birth.plusDays(days.toLong())
            }
            while (!birth.plusDays(days.toLong() + 1).isAfter(now)) {
                days++
                cursor = birth.plusDays(days.toLong())
            }

            val remainder = Duration.between(cursor, now)
            val hours = remainder.toHours().coerceAtLeast(0).toInt()
            val minutes = remainder.minusHours(hours.toLong()).toMinutes().coerceAtLeast(0).toInt()
            return ElapsedParts(days, hours, minutes)
        }

        private fun nextStartOfDay(after: Instant, zoneId: ZoneId): Instant {
            var date = after.atZone(zoneId).toLocalDate()
            repeat(370) {
                val candidate = date.atStartOfDay(zoneId).toInstant()
                if (candidate > after) return candidate
                date = date.plusDays(1)
            }
            return after.plus(Duration.ofDays(1))
        }

        private fun nextLocalTime(after: Instant, zoneId: ZoneId, time: LocalTime): Instant {
            var date = after.atZone(zoneId).toLocalDate()
            repeat(370) {
                val localDateTime = LocalDateTime.of(date, time)
                val candidates = validInstants(localDateTime, zoneId)
                candidates.firstOrNull { it > after }?.let { return it }
                date = date.plusDays(1)
            }
            return after.plus(Duration.ofDays(1))
        }

        private fun validInstants(localDateTime: LocalDateTime, zoneId: ZoneId): List<Instant> {
            val rules = zoneId.rules
            val offsets = rules.getValidOffsets(localDateTime)
            if (offsets.isNotEmpty()) {
                return offsets
                    .map { offset -> ZonedDateTime.ofLocal(localDateTime, zoneId, offset).toInstant() }
                    .sorted()
            }
            val transition: ZoneOffsetTransition = rules.getTransition(localDateTime)
                ?: return listOf(localDateTime.atZone(zoneId).toInstant())
            return listOf(transition.instant)
        }
    }
}
