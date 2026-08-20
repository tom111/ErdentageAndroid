// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.data

import androidx.datastore.core.DataStore
import de.eigenraum.erdentage.domain.AddedMilestoneHistory
import de.eigenraum.erdentage.domain.AddedMilestoneRecord
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface PeopleRepository {
    val state: Flow<PersistedAppState>

    suspend fun initialize(initialPeople: List<Person> = emptyList())
    suspend fun addPerson(person: Person): Boolean
    suspend fun updatePerson(personId: String, transform: (Person) -> Person): Boolean
    suspend fun deletePerson(personId: String): Boolean
    suspend fun movePerson(fromIndex: Int, toIndex: Int): Boolean
    suspend fun setCurrentPersonIndex(index: Int)
    suspend fun recordCalendarHandoff(
        personId: String,
        milestoneKey: String,
        launchedAt: Instant,
        visibleKeys: Set<String>,
    ): Boolean
}

class DataStorePeopleRepository(
    private val dataStore: DataStore<PersistedAppState>,
) : PeopleRepository {
    override val state: Flow<PersistedAppState> = dataStore.data
        .map(PersistedAppState::normalized)
        .distinctUntilChanged()

    override suspend fun initialize(initialPeople: List<Person>) {
        dataStore.updateData { stored ->
            val current = stored.normalized()
            if (current.people.isNotEmpty() || initialPeople.isEmpty()) {
                current
            } else {
                current.copy(
                    people = initialPeople
                        .take(ErdentageLimits.MAX_PEOPLE)
                        .map(Person::normalizedForStorage),
                    currentPersonIndex = 0,
                ).normalized()
            }
        }
    }

    override suspend fun addPerson(person: Person): Boolean {
        var added = false
        dataStore.updateData { stored ->
            val current = stored.normalized()
            if (current.people.size >= ErdentageLimits.MAX_PEOPLE ||
                current.people.any { it.id == person.id }
            ) {
                return@updateData current
            }

            val tint = person.tint ?: PersonTint.firstUnused(current.people)
            val people = current.people + person.copy(tint = tint).normalizedForStorage()
            added = true
            current.copy(
                people = people,
                currentPersonIndex = people.lastIndex,
            ).normalized()
        }
        return added
    }

    override suspend fun updatePerson(
        personId: String,
        transform: (Person) -> Person,
    ): Boolean {
        var updated = false
        dataStore.updateData { stored ->
            val current = stored.normalized()
            val index = current.people.indexOfFirst { it.id == personId }
            if (index < 0) return@updateData current

            val people = current.people.toMutableList()
            people[index] = transform(people[index])
                .copy(id = personId)
                .normalizedForStorage()
            updated = true
            current.copy(people = people).normalized()
        }
        return updated
    }

    override suspend fun deletePerson(personId: String): Boolean {
        var deleted = false
        dataStore.updateData { stored ->
            val current = stored.normalized()
            val removedIndex = current.people.indexOfFirst { it.id == personId }
            if (removedIndex < 0) return@updateData current

            val wasOverview = current.currentPersonIndex == current.people.size
            val selectedId = current.people.getOrNull(current.currentPersonIndex)?.id
            val people = current.people.toMutableList().apply { removeAt(removedIndex) }
            val newIndex = when {
                people.isEmpty() -> 0
                wasOverview -> people.size
                selectedId != null && people.any { it.id == selectedId } ->
                    people.indexOfFirst { it.id == selectedId }
                else -> current.currentPersonIndex.coerceIn(0, people.lastIndex)
            }
            deleted = true
            current.copy(people = people, currentPersonIndex = newIndex).normalized()
        }
        return deleted
    }

    override suspend fun movePerson(fromIndex: Int, toIndex: Int): Boolean {
        var moved = false
        dataStore.updateData { stored ->
            val current = stored.normalized()
            if (fromIndex !in current.people.indices || toIndex !in current.people.indices) {
                return@updateData current
            }

            val wasOverview = current.currentPersonIndex == current.people.size
            val selectedId = current.people.getOrNull(current.currentPersonIndex)?.id
            val people = current.people.toMutableList()
            val person = people.removeAt(fromIndex)
            people.add(toIndex, person)
            val newIndex = when {
                wasOverview -> people.size
                selectedId != null -> people.indexOfFirst { it.id == selectedId }
                else -> 0
            }
            moved = true
            current.copy(people = people, currentPersonIndex = newIndex).normalized()
        }
        return moved
    }

    override suspend fun setCurrentPersonIndex(index: Int) {
        dataStore.updateData { stored ->
            val current = stored.normalized()
            current.copy(
                currentPersonIndex = ErdentageLimits.normalizeCurrentPersonIndex(
                    index,
                    current.people.size,
                ),
            )
        }
    }

    override suspend fun recordCalendarHandoff(
        personId: String,
        milestoneKey: String,
        launchedAt: Instant,
        visibleKeys: Set<String>,
    ): Boolean = updatePerson(personId) { person ->
        person.copy(
            addedMilestones = AddedMilestoneHistory.prune(
                records = person.addedMilestones + AddedMilestoneRecord(milestoneKey, launchedAt),
                visibleKeys = visibleKeys + milestoneKey,
            ),
        )
    }
}
