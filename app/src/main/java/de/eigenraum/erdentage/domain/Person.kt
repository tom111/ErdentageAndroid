// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val birthHour: Int = 0,
    val birthMinute: Int = 0,
    val hasTime: Boolean = false,
    val daysOffEarth: Int = 0,
    val addedMilestones: List<AddedMilestoneRecord> = emptyList(),
    val enabledMilestones: Set<MilestoneKind> = MilestoneKind.defaultEnabled,
    val tint: PersonTint? = null,
    /**
     * Whether rows may say so when their day is also this person's birthday
     * (the Geburtstags-Zufälle). Absent in a blob written before phase two
     * means on: nobody opted out of something they had not seen. The switch
     * belongs to the person whose birthday would be named, not to the row
     * doing the naming.
     */
    val mentionsBirthday: Boolean = true,
) {
    init {
        require(isCanonicalUuid(id)) { "Person id must be a UUID string" }
    }

    val displayName: String
        get() = PersonName.display(name)

    fun normalizedForStorage(): Person = copy(
        birthHour = birthHour.coerceIn(0, 23),
        birthMinute = birthMinute.coerceIn(0, 59),
        daysOffEarth = ErdentageLimits.normalizeDaysOffEarth(daysOffEarth),
        enabledMilestones = MilestoneKind.entries.filter { it in enabledMilestones }.toSet(),
    )

    fun resolvedTint(index: Int): PersonTint = tint ?: PersonTint.defaultedAt(index)

    /**
     * The stored civil birth date, or `null` when the components do not form
     * a real date. Never normalized: 31 February stays rejected.
     */
    fun civilBirthDate(): LocalDate? {
        if (birthYear <= 0) return null
        return try {
            LocalDate.of(birthYear, birthMonth, birthDay)
        } catch (_: DateTimeException) {
            null
        }
    }

    fun birthDateTime(zoneId: ZoneId): ZonedDateTime? {
        if (birthYear <= 0) return null
        return try {
            val date = LocalDate.of(birthYear, birthMonth, birthDay)
            val time = if (hasTime) {
                LocalTime.of(birthHour.coerceIn(0, 23), birthMinute.coerceIn(0, 59))
            } else {
                LocalTime.MIDNIGHT
            }
            // The day decides validity; the time is allowed to degrade. A
            // birth time inside a DST gap (Berlin skipped 02:00–03:00 on
            // 2026-03-29) is a property of the zone, not of the data, so
            // java.time's forward shift (02:30 → 03:30) is what the wall
            // clock did that night. Only if that pushes the date off its day
            // is the time dropped and the bare birthday kept.
            val zoned = LocalDateTime.of(date, time).atZone(zoneId)
            if (zoned.toLocalDate() == date) zoned else date.atStartOfDay(zoneId)
        } catch (_: DateTimeException) {
            null
        }
    }

    companion object {
        private fun isCanonicalUuid(value: String): Boolean = runCatching {
            UUID.fromString(value).toString().equals(value, ignoreCase = true)
        }.getOrDefault(false)
    }
}

object PersonName {
    fun display(raw: String): String = raw.trim().ifEmpty { "Ich" }

    fun isOwner(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.isEmpty() || trimmed.lowercase(Locale.ROOT) == "ich"
    }

    /**
     * Enumerations of people put the speaker last: „Mia und ich“, never
     * „Ich und Mia“. Applied to the raw names, so a person literally named
     * „Ich“ is treated like a blank one. Order among the named people is
     * untouched; it carries meaning.
     */
    fun <T> ownerLast(items: List<T>, name: (T) -> String): List<T> =
        items.filterNot { isOwner(name(it)) } + items.filter { isOwner(name(it)) }

    fun ownerLast(names: List<String>): List<String> = ownerLast(names) { it }
}
