// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.domain

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AddedMilestoneRecord(
    val key: String,
    @Serializable(with = InstantSerializer::class)
    val addedAt: Instant,
)

object AddedMilestoneHistory {
    fun prune(
        records: List<AddedMilestoneRecord>,
        visibleKeys: Set<String>,
        cap: Int = ErdentageLimits.ADDED_MILESTONES_CAP,
    ): List<AddedMilestoneRecord> {
        val newestByKey = mutableMapOf<String, AddedMilestoneRecord>()
        records.forEach { record ->
            val existing = newestByKey[record.key]
            if (existing == null || record.addedAt > existing.addedAt) {
                newestByKey[record.key] = record
            }
        }

        val newestFirst = compareByDescending<AddedMilestoneRecord> { it.addedAt }
        val deduplicated = newestByKey.values
        if (deduplicated.size <= cap) return deduplicated.sortedWith(newestFirst)

        val visible = deduplicated.filter { it.key in visibleKeys }.sortedWith(newestFirst)
        val historical = deduplicated.filterNot { it.key in visibleKeys }.sortedWith(newestFirst)
        val historicalSlots = (cap - visible.size).coerceAtLeast(0)
        return visible + historical.take(historicalSlots)
    }
}
