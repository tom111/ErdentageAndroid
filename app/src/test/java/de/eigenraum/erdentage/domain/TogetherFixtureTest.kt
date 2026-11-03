// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Zusammen calculations and their German sentences to
 * `shared-fixtures/together-math.json`, character for character.
 */
class TogetherFixtureTest {
    private val fixture: TogetherFixture by lazy {
        val json = requireNotNull(javaClass.getResourceAsStream("/together-math.json")) {
            "Shared together fixture is missing"
        }.bufferedReader().use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString<TogetherFixture>(json)
    }

    @Test
    fun sharedFixtureMatchesPhaseTwoIosReference() {
        assertEquals(1, fixture.schemaVersion)
        assertEquals("089cf5a960f4040d03a78cc052738479d4416bc1", fixture.referenceIosCommit)
        assertTrue(fixture.cases.isNotEmpty())
    }

    @Test
    fun eventsMatchSharedFixture() {
        fixture.cases.forEach { case ->
            val people = case.people.map(FixturePerson::toPerson)
            val limits = case.limits
            val events = TogetherMath.events(people, LocalDate.parse(case.today)) { kind ->
                limits?.get(kind.serialName) ?: kind.defaultLimit
            }

            assertEquals(case.name, case.events.size, events.size)
            case.events.zip(events).forEach { (expected, actual) ->
                val label = "${case.name}: ${expected.id}"
                assertEquals(label, expected.kind, actual.kind.serialName)
                assertEquals(label, expected.id, actual.id)
                assertEquals(label, expected.date, actual.date.toString())
                assertEquals(label, expected.participantIds, actual.participantIds)
                assertEquals(label, expected.headline, actual.headline)
                assertEquals(label, expected.caption, actual.caption)
                assertEquals(label, expected.shareText, actual.shareText)
                assertEquals(label, expected.calendarTitle, actual.calendarTitle)
                assertEquals(label, expected.addedKey, actual.addedKey)
                assertEquals(label, expected.birthdayCaption, actual.birthdayNote?.caption)
                assertEquals(label, expected.shareMessage, actual.shareMessage)
            }
        }
    }

    @Test
    fun constellationsMatchSharedFixture() {
        fixture.cases.forEach { case ->
            val people = case.people.map(FixturePerson::toPerson)
            val constellations = TogetherMath.constellations(people)

            assertEquals(case.name, case.constellations.size, constellations.size)
            case.constellations.zip(constellations).forEach { (expected, actual) ->
                val label = "${case.name}: ${expected.id}"
                assertEquals(label, expected.id, actual.id)
                assertEquals(label, expected.olderId, actual.olderId)
                assertEquals(label, expected.youngerId, actual.youngerId)
                assertEquals(label, expected.gap, actual.gap)
                assertEquals(label, expected.displayIds, actual.displayIds)
                assertEquals(label, expected.headline, actual.headline)
                assertEquals(label, expected.detail, actual.detail)
                assertEquals(label, expected.tags, actual.tags.map(MilestoneKind::serialName))
            }
        }
    }
}

private val TogetherKind.serialName: String
    get() = when (this) {
        TogetherKind.DOPPELT -> "doppelt"
        TogetherKind.ZUSAMMEN_SO_ALT -> "zusammenSoAlt"
        TogetherKind.JAHRHUNDERT -> "jahrhundert"
    }

private val MilestoneKind.serialName: String
    get() = Json.encodeToString(MilestoneKind.serializer(), this).trim('"')

@Serializable
private data class TogetherFixture(
    val schemaVersion: Int,
    val referenceIosCommit: String,
    val cases: List<FixtureCase>,
)

@Serializable
private data class FixtureCase(
    val name: String,
    val today: String,
    val limits: Map<String, Int>? = null,
    val people: List<FixturePerson>,
    val events: List<FixtureEvent>,
    val constellations: List<FixtureConstellation>,
)

@Serializable
private data class FixturePerson(
    val id: String,
    val name: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val daysOffEarth: Int = 0,
    val mentionsBirthday: Boolean = true,
) {
    fun toPerson(): Person = Person(
        id = id,
        name = name,
        birthYear = birthYear,
        birthMonth = birthMonth,
        birthDay = birthDay,
        daysOffEarth = daysOffEarth,
        mentionsBirthday = mentionsBirthday,
    )
}

@Serializable
private data class FixtureEvent(
    val kind: String,
    val date: String,
    val participantIds: List<String>,
    val headline: String,
    val caption: String? = null,
    val shareText: String,
    val calendarTitle: String,
    val id: String,
    val addedKey: String,
    val birthdayCaption: String? = null,
    val shareMessage: String,
)

@Serializable
private data class FixtureConstellation(
    val olderId: String,
    val youngerId: String,
    val gap: Long,
    val displayIds: List<String>,
    val headline: String,
    val detail: String,
    val tags: List<String>,
    val id: String,
)
