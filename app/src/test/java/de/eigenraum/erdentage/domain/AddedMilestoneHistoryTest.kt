// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AddedMilestoneHistoryTest {
    @Test
    fun duplicateKeysKeepNewestRecord() {
        val result = AddedMilestoneHistory.prune(
            records = listOf(record("same", 10), record("other", 15), record("same", 20)),
            visibleKeys = emptySet(),
            cap = 10,
        )

        assertEquals(listOf("same", "other"), result.map { it.key })
        assertEquals(Instant.ofEpochSecond(20), result.first().addedAt)
    }

    @Test
    fun historicalRecordsAreCappedByRecency() {
        val result = AddedMilestoneHistory.prune(
            records = listOf(record("old", 10), record("middle", 20), record("new", 30)),
            visibleKeys = emptySet(),
            cap = 2,
        )

        assertEquals(listOf("new", "middle"), result.map { it.key })
    }

    @Test
    fun visibleRecordsSurviveEvenWhenTheyExceedCap() {
        val result = AddedMilestoneHistory.prune(
            records = listOf(
                record("visible-old", 10),
                record("historical-new", 30),
                record("visible-middle", 20),
            ),
            visibleKeys = setOf("visible-old", "visible-middle"),
            cap = 1,
        )

        assertEquals(listOf("visible-middle", "visible-old"), result.map { it.key })
    }

    @Test
    fun visibleRecordsComeBeforeNewestAvailableHistory() {
        val result = AddedMilestoneHistory.prune(
            records = listOf(
                record("visible-old", 10),
                record("historical-middle", 20),
                record("historical-new", 30),
            ),
            visibleKeys = setOf("visible-old"),
            cap = 2,
        )

        assertEquals(listOf("visible-old", "historical-new"), result.map { it.key })
    }

    private fun record(key: String, epochSecond: Long): AddedMilestoneRecord =
        AddedMilestoneRecord(key, Instant.ofEpochSecond(epochSecond))
}
