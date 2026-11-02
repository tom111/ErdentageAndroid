// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.Person
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PersistedAppState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val people: List<Person> = emptyList(),
    val currentPersonIndex: Int = 0,
    val milestoneBackfillVersion: Int = ErdentageLimits.MILESTONE_BACKFILL_VERSION,
) {
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
            people = normalizedPeople,
            currentPersonIndex = ErdentageLimits.normalizeCurrentPersonIndex(
                currentPersonIndex,
                normalizedPeople.size,
            ),
            milestoneBackfillVersion = maxOf(
                milestoneBackfillVersion,
                ErdentageLimits.MILESTONE_BACKFILL_VERSION,
            ),
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

val ErdentageJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
