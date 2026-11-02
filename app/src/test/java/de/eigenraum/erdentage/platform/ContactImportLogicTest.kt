// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

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
        assertEquals(
            ContactBirthday(1984, 2, 9),
            ContactImportLogic.parseBirthday("1984-2-9T00:00:00Z"),
        )
        assertEquals(
            ContactBirthday(month = 7, day = 2),
            ContactImportLogic.parseBirthday("--7-2 00:00:00"),
        )
        assertNull(ContactImportLogic.parseBirthday("not-a-date"))
    }

    @Test
    fun completeDateNeverInventsOrNormalizesComponents() {
        assertEquals(
            LocalDate.of(1984, 2, 29),
            ContactImportLogic.completeDate(
                ContactBirthday(1984, 2, 29),
                today = LocalDate.of(2026, 8, 20),
            ),
        )
        assertNull(
            ContactImportLogic.completeDate(
                ContactBirthday(month = 2, day = 29),
                today = LocalDate.of(2026, 8, 20),
            ),
        )
        assertNull(
            ContactImportLogic.completeDate(
                ContactBirthday(1983, 2, 29),
                today = LocalDate.of(2026, 8, 20),
            ),
        )
    }

    @Test
    fun completeDateRejectsFutureBirthdays() {
        val today = LocalDate.of(2026, 8, 20)
        assertNull(
            ContactImportLogic.completeDate(
                ContactBirthday(2030, 1, 1),
                today = today,
            ),
        )
        val initial = ContactImportLogic.initialDate(ContactBirthday(2030, 1, 1), today)
        assertEquals(1, initial.monthValue)
        assertEquals(1, initial.dayOfMonth)
        assert(initial <= today)
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
