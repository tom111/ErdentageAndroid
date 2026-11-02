// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.settings

import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonSettingsLogicTest {
    @Test
    fun enablingTimeClampsATodayBirthToNow() {
        val today = LocalDate.of(2026, 8, 16)
        val person = person(today).copy(birthHour = 23, birthMinute = 45)

        val updated = PersonSettingsLogic.setTimeKnown(
            person = person,
            enabled = true,
            today = today,
            now = LocalTime.of(12, 30),
        )

        assertTrue(updated.hasTime)
        assertEquals(12, updated.birthHour)
        assertEquals(30, updated.birthMinute)
    }

    @Test
    fun futureClockTimeIsAllowedForAPastBirthDate() {
        val today = LocalDate.of(2026, 8, 16)
        val person = person(today.minusDays(1)).copy(hasTime = true)

        val updated = PersonSettingsLogic.setBirthTime(
            person = person,
            time = LocalTime.of(23, 45),
            today = today,
            now = LocalTime.of(12, 30),
        )

        assertEquals(23, updated.birthHour)
        assertEquals(45, updated.birthMinute)
    }

    @Test
    fun twinOnlyAndOrdinaryPrimesRemainMutuallyExclusive() {
        val original = person(LocalDate.of(1990, 3, 15))

        val twins = PersonSettingsLogic.setTwinOnly(original, true)
        assertTrue(MilestoneKind.ZWILLINGSPRIMZAHL in twins.enabledMilestones)
        assertFalse(MilestoneKind.PRIMZAHL in twins.enabledMilestones)

        val ordinary = PersonSettingsLogic.setTwinOnly(twins, false)
        assertTrue(MilestoneKind.PRIMZAHL in ordinary.enabledMilestones)
        assertFalse(MilestoneKind.ZWILLINGSPRIMZAHL in ordinary.enabledMilestones)
    }

    @Test
    fun disablingPrimeParentRemovesBothPrimeKinds() {
        val twins = PersonSettingsLogic.setTwinOnly(
            person(LocalDate.of(1990, 3, 15)),
            true,
        )

        val disabled = PersonSettingsLogic.setPrimesEnabled(twins, false)

        assertFalse(PersonSettingsLogic.primesEnabled(disabled))
        assertFalse(MilestoneKind.PRIMZAHL in disabled.enabledMilestones)
        assertFalse(MilestoneKind.ZWILLINGSPRIMZAHL in disabled.enabledMilestones)
    }

    private fun person(date: LocalDate) = Person(
        id = "11111111-1111-4111-8111-111111111111",
        name = "Anne",
        birthYear = date.year,
        birthMonth = date.monthValue,
        birthDay = date.dayOfMonth,
    )
}
