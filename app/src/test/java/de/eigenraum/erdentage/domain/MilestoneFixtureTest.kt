// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MilestoneFixtureTest {
    private val fixture: Fixture by lazy {
        val json = requireNotNull(javaClass.getResourceAsStream("/milestone-math.json")) {
            "Shared milestone fixture is missing"
        }.bufferedReader().use { it.readText() }
        Json.decodeFromString<Fixture>(json)
    }

    @Test
    fun sharedFixtureMatchesCurrentIosReference() {
        assertEquals(1, fixture.schemaVersion)
        assertEquals("4f3ddc22f0b49d14ecf56d7af382ece0a3ca5f99", fixture.referenceIosCommit)
    }

    @Test
    fun strictlyNextFunctionsMatchSharedFixture() {
        fixture.nextMultiple.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextMultiple(it.step, it.after))
        }
        fixture.nextPrime.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextPrime(it.after))
        }
        fixture.nextTwinPrime.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextTwinPrime(it.after))
        }
        fixture.nextPalindrome.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextPalindrome(it.after))
        }
        fixture.nextPerfectSquare.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextPerfectSquare(it.after))
        }
        fixture.nextCatalan.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextCatalan(it.after))
        }
        fixture.nextPowerOfTwo.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextPowerOfTwo(it.after))
        }
        fixture.nextDigitSequence.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextDigitSequence(it.after))
        }
        fixture.nextFibonacci.forEach {
            assertEquals(it.toString(), it.expected, MilestoneMath.nextFibonacci(it.after))
        }
    }
}

@Serializable
private data class Fixture(
    val schemaVersion: Int,
    val referenceIosCommit: String,
    val nextMultiple: List<MultipleCase>,
    val nextPrime: List<NextCase>,
    val nextTwinPrime: List<NextCase>,
    val nextPalindrome: List<NextCase>,
    val nextPerfectSquare: List<NextCase>,
    val nextCatalan: List<NullableNextCase>,
    val nextPowerOfTwo: List<NextCase>,
    val nextDigitSequence: List<NextCase>,
    val nextFibonacci: List<NextCase>,
)

@Serializable
private data class MultipleCase(val step: Int, val after: Int, val expected: Int)

@Serializable
private data class NextCase(val after: Int, val expected: Int)

@Serializable
private data class NullableNextCase(val after: Int, val expected: Int?)
