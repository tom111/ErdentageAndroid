// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

object ErdentageLimits {
    const val MAX_PEOPLE = 20
    const val MAX_DAYS_OFF_EARTH = 100_000
    const val MAX_DISPLAYED_DAYS = 600_000_000
    const val ADDED_MILESTONES_CAP = 200
    const val MILESTONE_BACKFILL_VERSION = 2

    /** „Zusammen“ exists only from this many people onward. */
    const val MIN_PEOPLE_FOR_TOGETHER = 2

    val milestoneBackfillKindsByVersion: Map<Int, Set<MilestoneKind>> = mapOf(
        1 to setOf(MilestoneKind.ZWEIERPOTENZ, MilestoneKind.ZIFFERNFOLGE),
        2 to setOf(MilestoneKind.FIBONACCI),
    )

    fun normalizeDaysOffEarth(value: Int): Int = value.coerceIn(0, MAX_DAYS_OFF_EARTH)

    /** Index of the last valid page for a given people count. */
    fun lastPageIndex(peopleCount: Int): Int {
        val count = peopleCount.coerceAtLeast(0)
        if (count == 0) return 0
        return if (count >= MIN_PEOPLE_FOR_TOGETHER) count + 1 else count
    }

    /**
     * The single interpreter of a stored page index. Out-of-range values fall
     * back to the last person; a single person has no „Zusammen“ page.
     */
    fun pageKind(index: Int, peopleCount: Int): PageKind {
        val count = peopleCount.coerceAtLeast(0)
        if (count == 0 || index < 0) return PageKind.Person(0)
        if (index < count) return PageKind.Person(index)
        if (index == count) return PageKind.Demnaechst
        if (count >= MIN_PEOPLE_FOR_TOGETHER && index == count + 1) return PageKind.Zusammen
        return PageKind.Person(count - 1)
    }

    /**
     * Clamp a page index against a people count while preserving both
     * trailing sentinels as valid selections.
     */
    fun normalizeCurrentPersonIndex(index: Int, peopleCount: Int): Int {
        val count = peopleCount.coerceAtLeast(0)
        if (count == 0 || index < 0) return 0
        return if (index <= lastPageIndex(count)) index else count - 1
    }

    /**
     * The index of the same trailing page after the people list changed:
     * „Zusammen“ degrades to „Demnächst“ once fewer than two people remain.
     */
    fun restoredTrailingIndex(kind: PageKind, peopleCount: Int): Int? {
        val count = peopleCount.coerceAtLeast(0)
        if (count == 0) return 0
        return when (kind) {
            is PageKind.Person -> null
            PageKind.Demnaechst -> count
            PageKind.Zusammen -> if (count >= MIN_PEOPLE_FOR_TOGETHER) count + 1 else count
        }
    }

    fun milestoneKindsIntroducedAfter(version: Int): Set<MilestoneKind> =
        milestoneBackfillKindsByVersion
            .filterKeys { it > version }
            .values
            .flatten()
            .toSet()
}
