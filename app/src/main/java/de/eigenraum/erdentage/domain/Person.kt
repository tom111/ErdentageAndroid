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

    fun birthDateTime(zoneId: ZoneId): ZonedDateTime? {
        if (birthYear <= 0) return null
        return try {
            val date = LocalDate.of(birthYear, birthMonth, birthDay)
            val time = if (hasTime) {
                LocalTime.of(birthHour.coerceIn(0, 23), birthMinute.coerceIn(0, 59))
            } else {
                LocalTime.MIDNIGHT
            }
            val localDateTime = LocalDateTime.of(date, time)
            val zoned = localDateTime.atZone(zoneId)
            zoned.takeIf { it.toLocalDateTime() == localDateTime }
        } catch (_: DateTimeException) {
            null
        }
    }

    private companion object {
        fun isCanonicalUuid(value: String): Boolean = runCatching {
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
}
