// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import de.eigenraum.erdentage.domain.AddedMilestoneHistory
import de.eigenraum.erdentage.domain.AddedMilestoneRecord
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.TogetherKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Everything the app persists.
 *
 * Schema history:
 * - 1: people, current page index, milestone backfill marker.
 * - 2 (phase two, iOS 1.4 parity): `Person.mentionsBirthday`, the
 *   family-level „Zusammen“ row budgets and the family-level „already in the
 *   calendar“ records. Every addition has a default, so a schema-1 file reads
 *   as is and is rewritten at schema 2 on its next save.
 */
@Serializable
data class PersistedAppState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val people: List<Person> = emptyList(),
    val currentPersonIndex: Int = 0,
    val milestoneBackfillVersion: Int = ErdentageLimits.MILESTONE_BACKFILL_VERSION,
    /**
     * Rows per Wechselwirkungs-Kategorie on the „Zusammen“ page. A missing
     * entry means the kind's default; zero switches the category off.
     */
    val togetherLimits: Map<TogetherKind, Int> = emptyMap(),
    /**
     * Calendar handoffs launched for „Zusammen“ rows. A Wechselwirkung has no
     * owning person, so these live at family level with the same record type,
     * cap and pruning as a person's `addedMilestones`.
     */
    val togetherAddedEvents: List<AddedMilestoneRecord> = emptyList(),
) {
    fun togetherLimit(kind: TogetherKind): Int =
        TogetherKind.clampLimit(togetherLimits[kind] ?: kind.defaultLimit)

    fun normalized(): PersistedAppState {
        val backfillKinds = ErdentageLimits.milestoneKindsIntroducedAfter(
            milestoneBackfillVersion,
        )
        val normalizedPeople = people
            .take(ErdentageLimits.MAX_PEOPLE)
            .map { person ->
                person.copy(enabledMilestones = person.enabledMilestones + backfillKinds)
            }
            .map(Person::normalizedForStorage)
        return copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            people = normalizedPeople,
            currentPersonIndex = ErdentageLimits.normalizeCurrentPersonIndex(
                currentPersonIndex,
                normalizedPeople.size,
            ),
            milestoneBackfillVersion = maxOf(
                milestoneBackfillVersion,
                ErdentageLimits.MILESTONE_BACKFILL_VERSION,
            ),
            togetherLimits = togetherLimits.mapValues { (_, limit) -> TogetherKind.clampLimit(limit) },
            togetherAddedEvents = AddedMilestoneHistory.prune(
                records = togetherAddedEvents,
                visibleKeys = emptySet(),
            ),
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

val ErdentageJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
