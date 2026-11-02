// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppStateSerializerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun currentStateRoundTripsAsCanonicalJson() = runTest {
        val state = PersistedAppState(
            people = listOf(person(daysOffEarth = 999_999, birthHour = 99)),
            currentPersonIndex = 1,
        )
        val output = ByteArrayOutputStream()

        AppStateSerializer.writeTo(state, output)
        val decoded = AppStateSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(100_000, decoded.people.single().daysOffEarth)
        assertEquals(23, decoded.people.single().birthHour)
        assertEquals(decoded.people.size, decoded.currentPersonIndex)
        assertEquals(PersistedAppState.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
    }

    @Test
    fun unknownFieldsAreIgnoredForForwardCompatibleAdditions() = runTest {
        val json = """
            {
              "schemaVersion": 1,
              "people": [],
              "currentPersonIndex": 0,
              "milestoneBackfillVersion": 2,
              "futureField": { "enabled": true }
            }
        """.trimIndent()

        val decoded = AppStateSerializer.readFrom(ByteArrayInputStream(json.toByteArray()))

        assertEquals(PersistedAppState(), decoded)
    }

    @Test
    fun malformedJsonAndUnsupportedSchemaAreCorruption() = runTest {
        assertCorruption("not json")
        assertCorruption(
            """
                {
                  "schemaVersion": 99,
                  "people": [],
                  "currentPersonIndex": 0,
                  "milestoneBackfillVersion": 2
                }
            """.trimIndent(),
        )
    }

    @Test
    fun dataStoreCorruptionHandlerReplacesBrokenFileWithDefault() = runTest {
        val file = temporaryFolder.newFile("corrupt-state.json").apply {
            writeText("broken")
        }
        val dataStore = DataStoreFactory.create(
            serializer = AppStateSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { PersistedAppState() },
            scope = backgroundScope,
            produceFile = { file },
        )

        assertEquals(PersistedAppState(), dataStore.data.first())
        assertTrue(file.readText().contains("\"schemaVersion\":1"))
    }

    @Test
    fun oldBackfillMarkerDecodesRawAndNormalizesOnlyMissingVersionDelta() = runTest {
        val storedPerson = person().copy(
            enabledMilestones = MilestoneKind.defaultEnabled -
                MilestoneKind.ZWEIERPOTENZ -
                MilestoneKind.FIBONACCI,
        )
        val oldState = PersistedAppState(
            people = listOf(storedPerson),
            milestoneBackfillVersion = 1,
        )
        val decoded = AppStateSerializer.readFrom(
            ByteArrayInputStream(ErdentageJson.encodeToString(oldState).toByteArray()),
        )
        val normalized = decoded.normalized()

        assertEquals(1, decoded.milestoneBackfillVersion)
        assertEquals(2, normalized.milestoneBackfillVersion)
        assertTrue(MilestoneKind.FIBONACCI in normalized.people.single().enabledMilestones)
        assertFalse(MilestoneKind.ZWEIERPOTENZ in normalized.people.single().enabledMilestones)
    }

    private suspend fun assertCorruption(json: String) {
        try {
            AppStateSerializer.readFrom(ByteArrayInputStream(json.toByteArray()))
            throw AssertionError("Expected CorruptionException")
        } catch (_: CorruptionException) {
            // Expected.
        }
    }

    private fun person(daysOffEarth: Int = 0, birthHour: Int = 0): Person = Person(
        id = "11111111-2222-3333-4444-555555555555",
        name = "Lisa",
        birthYear = 1990,
        birthMonth = 3,
        birthDay = 15,
        birthHour = birthHour,
        hasTime = true,
        daysOffEarth = daysOffEarth,
    )
}
