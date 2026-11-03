// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Ported from the iOS „Geburtstags-Zufälle“ suite at tag `v1.4.2-build19`. */
class BirthdayCoincidenceTest {
    @Test
    fun onlyTheDayAndMonthHaveToLineUp() {
        val oskar = person("Oskar", 2018, 11, 29)
        assertNotNull(BirthdayCoincidence.note(day(2049, 11, 29), listOf(oskar)))
        assertNull(BirthdayCoincidence.note(day(2049, 11, 28), listOf(oskar)))
        assertNull(BirthdayCoincidence.note(day(2049, 12, 29), listOf(oskar)))
    }

    @Test
    fun anOrdinaryDayCarriesNoNote() {
        val family = listOf(person("Anne", 1984, 3, 2), person("Bernd", 1996, 7, 19))
        assertNull(BirthdayCoincidence.note(day(2026, 8, 23), family))
        assertNull(BirthdayCoincidence.note(day(2026, 8, 23), emptyList()))
    }

    @Test
    fun aPersonWhoseStoredDateIsNotARealDateClaimsNoBirthday() {
        val ghost = person("Geist", 0, 11, 29)
        assertNull(ghost.civilBirthDate())
        assertNull(BirthdayCoincidence.note(day(2049, 11, 29), listOf(ghost)))
        val february31 = person("Geist", 2000, 2, 31)
        assertNull(BirthdayCoincidence.note(day(2049, 2, 28), listOf(february31)))
    }

    @Test
    fun leapDayIsABirthdayOnlyInLeapYears() {
        val vera = person("Vera", 2000, 2, 29)
        assertNotNull(BirthdayCoincidence.note(day(2048, 2, 29), listOf(vera)))
        assertNull(BirthdayCoincidence.note(day(2049, 2, 28), listOf(vera)))
        assertNull(BirthdayCoincidence.note(day(2049, 3, 1), listOf(vera)))
    }

    @Test
    fun daysOffEarthMovesTheCountNeverTheBirthday() {
        val without = person("Anne", 1984, 3, 2)
        val with = person("Anne", 1984, 3, 2, off = 500)
        val target = day(2030, 3, 2)
        assertEquals(
            BirthdayCoincidence.note(target, listOf(without))?.caption,
            BirthdayCoincidence.note(target, listOf(with))?.caption,
        )
    }

    @Test
    fun theBirthTimeIsIgnored() {
        val timed = person("Anne", 1984, 3, 2).copy(hasTime = true, birthHour = 23, birthMinute = 59)
        assertEquals("🎂 an Annes 46. Geburtstag", BirthdayCoincidence.note(day(2030, 3, 2), listOf(timed))?.caption)
    }

    @Test
    fun theOrdinalIsTheAgeThatDay() {
        val oskar = person("Oskar", 2018, 11, 29)
        val note = BirthdayCoincidence.note(day(2049, 11, 29), listOf(oskar))
        assertEquals("🎂 an Oskars 31. Geburtstag", note?.caption)
        assertEquals("Es ist Oskars 31. Geburtstag. 🎂", note?.sentence)
        assertEquals("🎂 Oskar", note?.short)
    }

    @Test
    fun theDayOfBirthItselfGetsNoOrdinal() {
        val oskar = person("Oskar", 2018, 11, 29)
        assertEquals("🎂 an Oskars Geburtstag", BirthdayCoincidence.note(day(2018, 11, 29), listOf(oskar))?.caption)
        assertEquals("🎂 an Oskars Geburtstag", BirthdayCoincidence.note(day(2017, 11, 29), listOf(oskar))?.caption)
    }

    @Test
    fun theOwnerSpeaksInTheFirstPersonAndDeclines() {
        val ich = person("", 1990, 6, 4)
        val note = BirthdayCoincidence.note(day(2030, 6, 4), listOf(ich))
        assertEquals("🎂 an meinem 40. Geburtstag", note?.caption)
        assertEquals("Es ist mein 40. Geburtstag. 🎂", note?.sentence)
        assertEquals("🎂 Ich", note?.short)
        val typed = person("Ich", 1990, 6, 4)
        assertEquals("🎂 an meinem 40. Geburtstag", BirthdayCoincidence.note(day(2030, 6, 4), listOf(typed))?.caption)
    }

    @Test
    fun aNameEndingInAnSSoundTakesABareApostrophe() {
        val target = day(2030, 6, 4)
        for (name in listOf("Lars", "Max", "Franz", "Hans", "Weiß")) {
            val note = BirthdayCoincidence.note(target, listOf(person(name, 1990, 6, 4)))
            assertEquals("🎂 an $name’ 40. Geburtstag", note?.caption)
        }
        assertEquals("🎂 an Mias 40. Geburtstag", BirthdayCoincidence.note(target, listOf(person("Mia", 1990, 6, 4)))?.caption)
    }

    @Test
    fun twoPeopleSharingTheDayDropTheOrdinalsAndPutIchLast() {
        val ich = person("", 1996, 6, 4)
        val anne = person("Anne", 1990, 6, 4)
        val note = BirthdayCoincidence.note(day(2030, 6, 4), listOf(ich, anne))
        assertEquals("🎂 an Annes und meinem Geburtstag", note?.caption)
        assertEquals("Es ist Annes und mein Geburtstag. 🎂", note?.sentence)
        assertEquals("🎂 Anne, Ich", note?.short)

        val three = listOf(person("Mia", 2001, 6, 4), ich, anne)
        assertEquals("🎂 an Mias, Annes und meinem Geburtstag", BirthdayCoincidence.note(day(2030, 6, 4), three)?.caption)
    }

    @Test
    fun theSwitchBelongsToWhoeverBirthdayWouldBeNamed() {
        val without = person("Oskar", 2018, 11, 29, mentions = false)
        val with = person("Oskar", 2018, 11, 29)
        val day = day(2049, 11, 29)
        assertNull(BirthdayCoincidence.note(day, listOf(without)))
        assertNotNull(BirthdayCoincidence.note(day, listOf(with)))
        // An opt-out does not silence the others sharing the day.
        val anne = person("Anne", 1990, 11, 29)
        assertEquals("🎂 an Annes 59. Geburtstag", BirthdayCoincidence.note(day, listOf(without, anne))?.caption)
    }

    @Test
    fun ownerLastKeepsTheOrderAmongNamedPeople() {
        assertEquals(listOf("Mia", "Bernd", "Ich"), PersonName.ownerLast(listOf("Ich", "Mia", "Bernd")))
        assertEquals(listOf("Mia", "Bernd", ""), PersonName.ownerLast(listOf("Mia", "", "Bernd")))
        assertEquals(listOf("Mia", "Bernd"), PersonName.ownerLast(listOf("Mia", "Bernd")))
    }

    private fun person(
        name: String,
        year: Int,
        month: Int,
        day: Int,
        off: Int = 0,
        mentions: Boolean = true,
    ): Person = Person(
        id = UUID.nameUUIDFromBytes("$name-$year-$month-$day".toByteArray()).toString(),
        name = name,
        birthYear = year,
        birthMonth = month,
        birthDay = day,
        daysOffEarth = off,
        mentionsBirthday = mentions,
    )

    private fun day(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)
}
