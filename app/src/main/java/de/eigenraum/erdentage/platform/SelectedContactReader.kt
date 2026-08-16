// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.platform

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import java.time.DateTimeException
import java.time.LocalDate
import java.util.UUID

data class ContactBirthday(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)

data class SelectedContact(
    val name: String,
    val birthday: ContactBirthday?,
)

data class ContactPersonDraft(
    val name: String,
    val birthDate: LocalDate,
    val token: String = UUID.randomUUID().toString(),
)

object ContactImportLogic {
    fun completeDate(birthday: ContactBirthday?): LocalDate? {
        val year = birthday?.year?.takeIf { it > 0 } ?: return null
        val month = birthday.month ?: return null
        val day = birthday.day ?: return null
        return validDate(year, month, day)
    }

    fun initialDate(birthday: ContactBirthday?, today: LocalDate): LocalDate {
        val preset = today.minusDays(5_000)
        val month = birthday?.month?.takeIf { it in 1..12 } ?: preset.monthValue
        val day = birthday?.day?.takeIf { it in 1..31 } ?: preset.dayOfMonth
        val explicitYear = birthday?.year?.takeIf { it > 0 }
        if (explicitYear != null) {
            return validDate(explicitYear, month, day) ?: preset
        }
        for (distance in 0..4) {
            val earlier = validDate(preset.year - distance, month, day)
            if (earlier != null) return earlier
            val later = validDate(preset.year + distance, month, day)
            if (later != null && later <= today) return later
        }
        return preset
    }

    fun parseBirthday(raw: String?): ContactBirthday? {
        val value = raw?.trim().orEmpty()
        val full = FullBirthday.matchEntire(value)
        if (full != null) {
            return ContactBirthday(
                year = full.groupValues[1].toIntOrNull(),
                month = full.groupValues[2].toIntOrNull(),
                day = full.groupValues[3].toIntOrNull(),
            )
        }
        val partial = PartialBirthday.matchEntire(value)
        if (partial != null) {
            return ContactBirthday(
                month = partial.groupValues[1].toIntOrNull(),
                day = partial.groupValues[2].toIntOrNull(),
            )
        }
        return null
    }

    private fun validDate(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        null
    }

    private val FullBirthday = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
    private val PartialBirthday = Regex("--(\\d{2})-(\\d{2})")
}

class SelectedContactReader(
    private val resolver: ContentResolver,
) {
    fun read(uri: Uri): SelectedContact {
        val name = queryName(uri)
        val birthday = queryBirthday(uri)
        return SelectedContact(name = name, birthday = birthday)
    }

    private fun queryName(uri: Uri): String = safely {
        resolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ""
            cursor.getString(0)?.trim().orEmpty()
        }.orEmpty()
    }.orEmpty()

    private fun queryBirthday(uri: Uri): ContactBirthday? = safely {
        val entityUri = Uri.withAppendedPath(
            uri,
            ContactsContract.Contacts.Entity.CONTENT_DIRECTORY,
        )
        resolver.query(
            entityUri,
            arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val mimeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val dateIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
            val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.TYPE)
            while (cursor.moveToNext()) {
                val isBirthday = mimeIndex >= 0 && typeIndex >= 0 &&
                    cursor.getString(mimeIndex) == ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE &&
                    cursor.getInt(typeIndex) == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                if (isBirthday && dateIndex >= 0) {
                    ContactImportLogic.parseBirthday(cursor.getString(dateIndex))?.let { return@use it }
                }
            }
            null
        }
    }

    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    }
}
