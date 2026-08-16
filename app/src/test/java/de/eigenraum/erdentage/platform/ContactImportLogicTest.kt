// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.platform

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactImportLogicTest {
    @Test
    fun parsesCompleteAndYearlessAndroidBirthdayFormats() {
        assertEquals(
            ContactBirthday(1984, 2, 29),
            ContactImportLogic.parseBirthday("1984-02-29"),
        )
        assertEquals(
            ContactBirthday(month = 7, day = 12),
            ContactImportLogic.parseBirthday("--07-12"),
        )
        assertNull(ContactImportLogic.parseBirthday("not-a-date"))
    }

    @Test
    fun completeDateNeverInventsOrNormalizesComponents() {
        assertEquals(
            LocalDate.of(1984, 2, 29),
            ContactImportLogic.completeDate(ContactBirthday(1984, 2, 29)),
        )
        assertNull(ContactImportLogic.completeDate(ContactBirthday(month = 2, day = 29)))
        assertNull(ContactImportLogic.completeDate(ContactBirthday(1983, 2, 29)))
    }

    @Test
    fun yearlessLeapBirthdayKeepsKnownMonthAndDay() {
        val initial = ContactImportLogic.initialDate(
            birthday = ContactBirthday(month = 2, day = 29),
            today = LocalDate.of(2026, 8, 16),
        )
        assertEquals(2, initial.monthValue)
        assertEquals(29, initial.dayOfMonth)
        assert(initial <= LocalDate.of(2026, 8, 16))
    }
}
