// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import androidx.datastore.core.DataStore
import de.eigenraum.erdentage.domain.AddedMilestoneHistory
import de.eigenraum.erdentage.domain.AddedMilestoneRecord
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.domain.TogetherKind
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

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
    suspend fun setTogetherLimit(kind: TogetherKind, limit: Int)
    suspend fun recordTogetherCalendarHandoff(
        key: String,
        launchedAt: Instant,
        visibleKeys: Set<String>,
    )
}

class DataStorePeopleRepository(
    private val dataStore: DataStore<PersistedAppState>,
) : PeopleRepository {
    override val state: Flow<PersistedAppState> = dataStore.data
        // DataStore wraps a serializer failure in its own IOException; surface
        // the newer-schema case unwrapped so nobody downstream retries it.
        .catch { cause -> throw cause.newerSchemaOrSelf() }
        .retryWhen { cause, attempt ->
            // A file from a newer release is not transient; let it surface.
            if (cause !is IOException || cause is NewerSchemaException) return@retryWhen false
            delay(readRetryDelayMillis(attempt))
            true
        }
        .map(PersistedAppState::normalized)
        .distinctUntilChanged()

    override suspend fun initialize(initialPeople: List<Person>) {
        try {
            initializeStored(initialPeople)
        } catch (cause: IOException) {
            // updateData reads first; surface a newer-schema file the same
            // way `state` does, so callers can recognise it.
            throw cause.newerSchemaOrSelf()
        }
    }

    private suspend fun initializeStored(initialPeople: List<Person>) {
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

            val pageKind = ErdentageLimits.pageKind(current.currentPersonIndex, current.people.size)
            val selectedId = current.people.getOrNull(current.currentPersonIndex)?.id
            val people = current.people.toMutableList().apply { removeAt(removedIndex) }
            val trailingIndex = ErdentageLimits.restoredTrailingIndex(pageKind, people.size)
            val newIndex = when {
                people.isEmpty() -> 0
                trailingIndex != null -> trailingIndex
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

            val pageKind = ErdentageLimits.pageKind(current.currentPersonIndex, current.people.size)
            val selectedId = current.people.getOrNull(current.currentPersonIndex)?.id
            val people = current.people.toMutableList()
            val person = people.removeAt(fromIndex)
            people.add(toIndex, person)
            val trailingIndex = ErdentageLimits.restoredTrailingIndex(pageKind, people.size)
            val newIndex = when {
                trailingIndex != null -> trailingIndex
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
            val normalizedIndex = ErdentageLimits.normalizeCurrentPersonIndex(
                index,
                current.people.size,
            )
            if (current.currentPersonIndex == normalizedIndex) {
                return@updateData current
            }
            current.copy(
                currentPersonIndex = normalizedIndex,
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

    override suspend fun setTogetherLimit(kind: TogetherKind, limit: Int) {
        dataStore.updateData { stored ->
            val current = stored.normalized()
            val clamped = TogetherKind.clampLimit(limit)
            if (current.togetherLimit(kind) == clamped && kind in current.togetherLimits) {
                return@updateData current
            }
            current.copy(togetherLimits = current.togetherLimits + (kind to clamped))
        }
    }

    override suspend fun recordTogetherCalendarHandoff(
        key: String,
        launchedAt: Instant,
        visibleKeys: Set<String>,
    ) {
        dataStore.updateData { stored ->
            val current = stored.normalized()
            current.copy(
                togetherAddedEvents = AddedMilestoneHistory.prune(
                    records = current.togetherAddedEvents + AddedMilestoneRecord(key, launchedAt),
                    visibleKeys = visibleKeys + key,
                ),
            )
        }
    }
}

/**
 * The [NewerSchemaException] in this throwable's cause chain, if any.
 * DataStore wraps serializer failures in its own IOException.
 */
fun Throwable.newerSchemaCause(): NewerSchemaException? =
    generateSequence(this) { it.cause }.filterIsInstance<NewerSchemaException>().firstOrNull()

private fun Throwable.newerSchemaOrSelf(): Throwable = newerSchemaCause() ?: this

/**
 * For consumers that cannot explain a newer-schema file to the user
 * (application start-up, widgets, receivers): the flow completes quietly
 * instead of throwing, and the explanation is left to the main UI, which
 * reads [PeopleRepository.state] directly and shows `state_newer_schema`.
 * Every other failure still propagates.
 */
fun Flow<PersistedAppState>.ignoringNewerSchema(
    onIgnored: (NewerSchemaException) -> Unit = {},
): Flow<PersistedAppState> = catch { cause ->
    val newer = cause.newerSchemaCause() ?: throw cause
    onIgnored(newer)
}

/**
 * Runs [block]; returns null instead of throwing when the repository
 * reports a newer-schema file. See [ignoringNewerSchema].
 */
suspend fun <T> ignoringNewerSchema(
    onIgnored: (NewerSchemaException) -> Unit = {},
    block: suspend () -> T,
): T? = try {
    block()
} catch (cause: IOException) {
    val newer = cause.newerSchemaCause() ?: throw cause
    onIgnored(newer)
    null
}

private fun readRetryDelayMillis(attempt: Long): Long =
    (100L shl attempt.coerceAtMost(8).toInt()).coerceAtMost(30_000L)
