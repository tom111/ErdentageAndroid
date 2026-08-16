// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.settings

import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime

internal object PersonSettingsLogic {
    fun birthDate(person: Person): LocalDate? = try {
        LocalDate.of(person.birthYear, person.birthMonth, person.birthDay)
    } catch (_: DateTimeException) {
        null
    }

    fun setBirthDate(
        person: Person,
        date: LocalDate,
        today: LocalDate,
        now: LocalTime,
    ): Person = clampToNow(
        person.copy(
            birthYear = date.year,
            birthMonth = date.monthValue,
            birthDay = date.dayOfMonth,
        ),
        today,
        now,
    )

    fun setTimeKnown(
        person: Person,
        enabled: Boolean,
        today: LocalDate,
        now: LocalTime,
    ): Person = clampToNow(person.copy(hasTime = enabled), today, now)

    fun setBirthTime(
        person: Person,
        time: LocalTime,
        today: LocalDate,
        now: LocalTime,
    ): Person = clampToNow(
        person.copy(birthHour = time.hour, birthMinute = time.minute),
        today,
        now,
    )

    fun primesEnabled(person: Person): Boolean =
        MilestoneKind.PRIMZAHL in person.enabledMilestones ||
            MilestoneKind.ZWILLINGSPRIMZAHL in person.enabledMilestones

    fun setMilestone(person: Person, kind: MilestoneKind, enabled: Boolean): Person = person.copy(
        enabledMilestones = if (enabled) {
            person.enabledMilestones + kind
        } else {
            person.enabledMilestones - kind
        },
    )

    fun setPrimesEnabled(person: Person, enabled: Boolean): Person = person.copy(
        enabledMilestones = if (enabled) {
            person.enabledMilestones +
                MilestoneKind.PRIMZAHL -
                MilestoneKind.ZWILLINGSPRIMZAHL
        } else {
            person.enabledMilestones -
                MilestoneKind.PRIMZAHL -
                MilestoneKind.ZWILLINGSPRIMZAHL
        },
    )

    fun setTwinOnly(person: Person, enabled: Boolean): Person = person.copy(
        enabledMilestones = if (enabled) {
            person.enabledMilestones -
                MilestoneKind.PRIMZAHL +
                MilestoneKind.ZWILLINGSPRIMZAHL
        } else {
            person.enabledMilestones -
                MilestoneKind.ZWILLINGSPRIMZAHL +
                MilestoneKind.PRIMZAHL
        },
    )

    private fun clampToNow(person: Person, today: LocalDate, now: LocalTime): Person {
        if (!person.hasTime || birthDate(person) != today) return person
        val selected = LocalTime.of(person.birthHour, person.birthMinute)
        return if (selected > now) {
            person.copy(birthHour = now.hour, birthMinute = now.minute)
        } else {
            person
        }
    }
}
