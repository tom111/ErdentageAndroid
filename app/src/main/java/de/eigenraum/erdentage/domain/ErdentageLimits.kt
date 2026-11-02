// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

object ErdentageLimits {
    const val MAX_PEOPLE = 20
    const val MAX_DAYS_OFF_EARTH = 100_000
    const val MAX_DISPLAYED_DAYS = 600_000_000
    const val ADDED_MILESTONES_CAP = 200
    const val MILESTONE_BACKFILL_VERSION = 2

    val milestoneBackfillKindsByVersion: Map<Int, Set<MilestoneKind>> = mapOf(
        1 to setOf(MilestoneKind.ZWEIERPOTENZ, MilestoneKind.ZIFFERNFOLGE),
        2 to setOf(MilestoneKind.FIBONACCI),
    )

    fun normalizeDaysOffEarth(value: Int): Int = value.coerceIn(0, MAX_DAYS_OFF_EARTH)

    fun normalizeCurrentPersonIndex(index: Int, peopleCount: Int): Int {
        val count = peopleCount.coerceAtLeast(0)
        if (count == 0 || index < 0) return 0
        return if (index <= count) index else count - 1
    }

    fun milestoneKindsIntroducedAfter(version: Int): Set<MilestoneKind> =
        milestoneBackfillKindsByVersion
            .filterKeys { it > version }
            .values
            .flatten()
            .toSet()
}
