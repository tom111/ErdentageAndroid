// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.domain.TogetherKind
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PeopleRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun initializeExposesDefaultState() = runTest {
        val (_, repository) = repository("default")

        repository.initialize()

        assertEquals(PersistedAppState(), repository.state.first())
    }

    @Test
    fun transientReadFailureIsRetried() = runTest {
        val expected = PersistedAppState(people = listOf(person(0)))
        var subscriptions = 0
        val dataStore = object : DataStore<PersistedAppState> {
            override val data = flow {
                if (subscriptions++ == 0) throw IOException("temporary")
                emit(expected)
            }

            override suspend fun updateData(
                transform: suspend (t: PersistedAppState) -> PersistedAppState,
            ): PersistedAppState = error("not used")
        }
        val repository = DataStorePeopleRepository(dataStore)

        assertEquals(expected, repository.state.first())
        assertEquals(2, subscriptions)
    }

    @Test
    fun initializeSeedsOnlyAnEmptyRepository() = runTest {
        val (_, repository) = repository("initial-people")
        val initialPeople = listOf(person(0), person(1), person(2))

        repository.initialize(initialPeople)

        var state = repository.state.first()
        assertEquals(initialPeople, state.people)
        assertEquals(0, state.currentPersonIndex)

        repository.updatePerson(initialPeople.first().id) { it.copy(name = "Geändert") }
        repository.initialize(listOf(person(3), person(4), person(5)))

        state = repository.state.first()
        assertEquals(listOf("Geändert", "Person 1", "Person 2"), state.people.map(Person::name))
    }

    @Test
    fun addAssignsDistinctTintSelectsNewPersonAndRejectsDuplicateOrOverflow() = runTest {
        val (_, repository) = repository("add")

        assertTrue(repository.addPerson(person(0)))
        assertTrue(repository.addPerson(person(1)))
        assertFalse(repository.addPerson(person(1)))

        var state = repository.state.first()
        assertEquals(listOf(PersonTint.SAND, PersonTint.CORAL), state.people.map { it.tint })
        assertEquals(1, state.currentPersonIndex)

        for (index in 2 until ErdentageLimits.MAX_PEOPLE) {
            assertTrue(repository.addPerson(person(index)))
        }
        assertFalse(repository.addPerson(person(ErdentageLimits.MAX_PEOPLE)))
        state = repository.state.first()
        assertEquals(ErdentageLimits.MAX_PEOPLE, state.people.size)
    }

    @Test
    fun concurrentAddsAreSerializedWithoutLostUpdates() = runTest {
        val (_, repository) = repository("concurrent")

        val results = (0 until ErdentageLimits.MAX_PEOPLE)
            .map { index -> async { repository.addPerson(person(index)) } }
            .awaitAll()

        assertTrue(results.all { it })
        assertEquals(ErdentageLimits.MAX_PEOPLE, repository.state.first().people.size)
    }

    @Test
    fun initializePhysicallyPersistsBackfillMarkerAndDelta() = runTest {
        val file = File(temporaryFolder.root, "backfill.json")
        val oldPerson = person(0).copy(
            enabledMilestones = MilestoneKind.defaultEnabled -
                MilestoneKind.ZWEIERPOTENZ -
                MilestoneKind.FIBONACCI,
        )
        file.writeText(
            ErdentageJson.encodeToString(
                PersistedAppState(
                    people = listOf(oldPerson),
                    milestoneBackfillVersion = 1,
                ),
            ),
        )
        val dataStore = DataStoreFactory.create(
            serializer = AppStateSerializer,
            scope = backgroundScope,
            produceFile = { file },
        )
        val repository = DataStorePeopleRepository(dataStore)

        repository.initialize()

        val persisted = ErdentageJson.decodeFromString<PersistedAppState>(file.readText())
        assertEquals(2, persisted.milestoneBackfillVersion)
        assertTrue(
            MilestoneKind.FIBONACCI in persisted.people.single().enabledMilestones,
        )
        assertFalse(
            MilestoneKind.ZWEIERPOTENZ in persisted.people.single().enabledMilestones,
        )
    }

    @Test
    fun updateKeepsStableIdAndNormalizesScalarRanges() = runTest {
        val (_, repository) = repository("update")
        val original = person(0)
        repository.addPerson(original)

        assertTrue(
            repository.updatePerson(original.id) { current ->
                current.copy(
                    id = uuid(99),
                    name = "Neu",
                    birthHour = 99,
                    daysOffEarth = -1,
                )
            },
        )

        val updated = repository.state.first().people.single()
        assertEquals(original.id, updated.id)
        assertEquals("Neu", updated.name)
        assertEquals(23, updated.birthHour)
        assertEquals(0, updated.daysOffEarth)
        assertFalse(repository.updatePerson(uuid(99)) { it })
    }

    @Test
    fun moveAndDeletePreserveSelectedUuid() = runTest {
        val (_, repository) = repository("selection")
        repeat(3) { repository.addPerson(person(it)) }
        repository.setCurrentPersonIndex(1)

        assertTrue(repository.movePerson(fromIndex = 1, toIndex = 0))
        var state = repository.state.first()
        assertEquals(uuid(1), state.people[state.currentPersonIndex].id)
        assertEquals(0, state.currentPersonIndex)

        assertTrue(repository.deletePerson(uuid(2)))
        state = repository.state.first()
        assertEquals(uuid(1), state.people[state.currentPersonIndex].id)

        assertTrue(repository.deletePerson(uuid(1)))
        state = repository.state.first()
        assertEquals(0, state.currentPersonIndex)
        assertEquals(uuid(0), state.people.single().id)
    }

    @Test
    fun moveAndDeletePreserveSingleOverviewSentinel() = runTest {
        val (_, repository) = repository("overview")
        repeat(3) { repository.addPerson(person(it)) }
        repository.setCurrentPersonIndex(3)

        assertTrue(repository.movePerson(fromIndex = 0, toIndex = 2))
        assertEquals(3, repository.state.first().currentPersonIndex)

        assertTrue(repository.deletePerson(uuid(1)))
        val state = repository.state.first()
        assertEquals(state.people.size, state.currentPersonIndex)
    }

    @Test
    fun moveAndDeletePreserveZusammenSentinelAndDegradeItBelowTwoPeople() = runTest {
        val (_, repository) = repository("together-page")
        repeat(3) { repository.addPerson(person(it)) }
        repository.setCurrentPersonIndex(4)
        assertEquals(4, repository.state.first().currentPersonIndex)

        assertTrue(repository.movePerson(fromIndex = 2, toIndex = 0))
        assertEquals(4, repository.state.first().currentPersonIndex)

        assertTrue(repository.deletePerson(uuid(1)))
        var state = repository.state.first()
        assertEquals(2, state.people.size)
        assertEquals(3, state.currentPersonIndex)

        // Dropping below two people degrades Zusammen to Demnächst.
        assertTrue(repository.deletePerson(uuid(2)))
        state = repository.state.first()
        assertEquals(1, state.people.size)
        assertEquals(1, state.currentPersonIndex)

        // A single person cannot select Zusammen at all.
        repository.setCurrentPersonIndex(2)
        assertEquals(0, repository.state.first().currentPersonIndex)
        assertTrue(repository.addPerson(person(5)))
        repository.setCurrentPersonIndex(3)
        assertEquals(3, repository.state.first().currentPersonIndex)
    }

    @Test
    fun togetherLimitsClampToRangeAndMissingMeansDefault() = runTest {
        val (_, repository) = repository("together-limits")

        var state = repository.state.first()
        for (kind in TogetherKind.entries) {
            assertEquals(kind.defaultLimit, state.togetherLimit(kind))
        }

        repository.setTogetherLimit(TogetherKind.JAHRHUNDERT, 0)
        repository.setTogetherLimit(TogetherKind.DOPPELT, 99)
        repository.setTogetherLimit(TogetherKind.ZUSAMMEN_SO_ALT, -1)
        state = repository.state.first()
        assertEquals(0, state.togetherLimit(TogetherKind.JAHRHUNDERT))
        assertEquals(TogetherKind.LIMIT_RANGE.last, state.togetherLimit(TogetherKind.DOPPELT))
        assertEquals(0, state.togetherLimit(TogetherKind.ZUSAMMEN_SO_ALT))
    }

    @Test
    fun togetherCalendarHandoffHistoryCapsRecordsAndPreservesVisibleKeys() = runTest {
        val (_, repository) = repository("together-history")

        for (index in 0..201) {
            repository.recordTogetherCalendarHandoff(
                key = "pair-$index@20490101",
                launchedAt = Instant.ofEpochSecond(index.toLong()),
                visibleKeys = emptySet(),
            )
        }
        var records = repository.state.first().togetherAddedEvents
        assertEquals(ErdentageLimits.ADDED_MILESTONES_CAP, records.size)
        assertFalse(records.any { it.key == "pair-0@20490101" })

        repository.recordTogetherCalendarHandoff(
            key = "pair-202@20490101",
            launchedAt = Instant.ofEpochSecond(202),
            visibleKeys = setOf("pair-2@20490101"),
        )
        records = repository.state.first().togetherAddedEvents
        assertEquals(ErdentageLimits.ADDED_MILESTONES_CAP, records.size)
        assertTrue(records.any { it.key == "pair-2@20490101" })
        assertTrue(records.any { it.key == "pair-202@20490101" })
    }

    @Test
    fun newerSchemaIsNotRetriedAsATransientFailure() = runTest {
        var subscriptions = 0
        val dataStore = object : DataStore<PersistedAppState> {
            override val data = flow<PersistedAppState> {
                subscriptions++
                // DataStore wraps the serializer's exception in its own IOException.
                throw IOException("Inoperable file", NewerSchemaException(99))
            }

            override suspend fun updateData(
                transform: suspend (t: PersistedAppState) -> PersistedAppState,
            ): PersistedAppState = error("not used")
        }
        val repository = DataStorePeopleRepository(dataStore)

        try {
            repository.state.first()
            fail("Expected NewerSchemaException")
        } catch (_: NewerSchemaException) {
            // Expected.
        }
        assertEquals(1, subscriptions)
    }

    @Test
    fun invalidMoveAndSelectionAreHandledDefensively() = runTest {
        val (_, repository) = repository("invalid")
        repeat(2) { repository.addPerson(person(it)) }

        assertFalse(repository.movePerson(-1, 1))
        assertFalse(repository.movePerson(0, 2))
        repository.setCurrentPersonIndex(99)

        assertEquals(1, repository.state.first().currentPersonIndex)
    }

    @Test
    fun calendarHandoffHistoryCapsRecordsAndPreservesVisibleKeys() = runTest {
        val (_, repository) = repository("history")
        val person = person(0)
        repository.addPerson(person)

        for (index in 0..201) {
            assertTrue(
                repository.recordCalendarHandoff(
                    personId = person.id,
                    milestoneKey = "key-$index",
                    launchedAt = Instant.ofEpochSecond(index.toLong()),
                    visibleKeys = emptySet(),
                ),
            )
        }
        var records = repository.state.first().people.single().addedMilestones
        assertEquals(ErdentageLimits.ADDED_MILESTONES_CAP, records.size)
        assertFalse(records.any { it.key == "key-0" })

        repository.recordCalendarHandoff(
            personId = person.id,
            milestoneKey = "key-202",
            launchedAt = Instant.ofEpochSecond(202),
            visibleKeys = setOf("key-2"),
        )
        records = repository.state.first().people.single().addedMilestones
        assertEquals(ErdentageLimits.ADDED_MILESTONES_CAP, records.size)
        assertTrue(records.any { it.key == "key-2" })
        assertTrue(records.any { it.key == "key-202" })
        assertFalse(repository.recordCalendarHandoff(uuid(9), "missing", Instant.EPOCH, emptySet()))
    }

    private fun TestScope.repository(name: String): Pair<DataStore<PersistedAppState>, PeopleRepository> {
        val file = File(temporaryFolder.root, "$name.json")
        val dataStore = DataStoreFactory.create(
            serializer = AppStateSerializer,
            scope = backgroundScope,
            produceFile = { file },
        )
        return dataStore to DataStorePeopleRepository(dataStore)
    }

    private fun person(index: Int): Person = Person(
        id = uuid(index),
        name = "Person $index",
        birthYear = 1990,
        birthMonth = 3,
        birthDay = 15,
    )

    private fun uuid(index: Int): String = "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"
}
