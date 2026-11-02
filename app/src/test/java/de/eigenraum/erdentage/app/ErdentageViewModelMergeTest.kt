// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.domain.AddedMilestoneRecord
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ErdentageViewModelMergeTest {
    @Test
    fun editorValuesPreserveConcurrentCalendarHistory() {
        val history = listOf(
            AddedMilestoneRecord("milestone", Instant.parse("2026-08-20T12:00:00Z")),
        )
        val stored = person(name = "Vorher").copy(addedMilestones = history)
        val edited = person(name = "Nachher").copy(
            birthYear = 1990,
            birthMonth = 3,
            birthDay = 4,
            birthHour = 12,
            birthMinute = 34,
            hasTime = true,
            daysOffEarth = 7,
            enabledMilestones = setOf(MilestoneKind.PALINDROM),
            tint = PersonTint.CORAL,
            addedMilestones = emptyList(),
        )

        val merged = stored.withEditorValues(edited)

        assertEquals("Nachher", merged.name)
        assertEquals(1990, merged.birthYear)
        assertEquals(3, merged.birthMonth)
        assertEquals(4, merged.birthDay)
        assertEquals(12, merged.birthHour)
        assertEquals(34, merged.birthMinute)
        assertEquals(true, merged.hasTime)
        assertEquals(7, merged.daysOffEarth)
        assertEquals(setOf(MilestoneKind.PALINDROM), merged.enabledMilestones)
        assertEquals(PersonTint.CORAL, merged.tint)
        assertEquals(history, merged.addedMilestones)
        assertEquals(stored.id, merged.id)
    }

    private fun person(name: String) = Person(
        id = "11111111-1111-4111-8111-111111111111",
        name = name,
        birthYear = 1980,
        birthMonth = 7,
        birthDay = 12,
    )
}
