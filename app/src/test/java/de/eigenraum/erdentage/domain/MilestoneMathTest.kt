// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneMathTest {
    @Test
    fun primeAndTwinPrimeMembershipMatchesSwiftCorpus() {
        listOf(-1, 0, 1, 4, 12_345).forEach { assertFalse("$it", MilestoneMath.isPrime(it)) }
        listOf(2, 3, 12_347).forEach { assertTrue("$it", MilestoneMath.isPrime(it)) }

        listOf(3, 5, 7, 11, 13, 17, 19, 29, 31, 41, 43).forEach {
            assertTrue("$it", MilestoneMath.isTwinPrime(it))
        }
        listOf(2, 4, 9, 23, 37, 47, 53, 67, 79, 83, 89, 97).forEach {
            assertFalse("$it", MilestoneMath.isTwinPrime(it))
        }
    }

    @Test
    fun palindromeMembershipMatchesSwiftCorpus() {
        listOf(0, 7, 11, 12_321, 10_301).forEach {
            assertTrue("$it", MilestoneMath.isPalindrome(it))
        }
        listOf(12, 12_345).forEach { assertFalse("$it", MilestoneMath.isPalindrome(it)) }
    }

    @Test
    fun catalanTableMatchesPublishedFiniteCatalog() {
        assertEquals(
            listOf(
                1, 2, 5, 14, 42, 132, 429, 1_430, 4_862, 16_796, 58_786,
                208_012, 742_900, 2_674_440, 9_694_845, 35_357_670,
                129_644_790, 477_638_700,
            ),
            MilestoneMath.catalanNumbers,
        )
        assertTrue(MilestoneMath.catalanNumbers.zipWithNext().all { (a, b) -> a < b })
        assertNull(MilestoneMath.nextCatalan(477_638_700))
    }

    @Test
    fun powerOfTwoDigitSequenceAndFibonacciMembershipMatchSwiftCorpus() {
        listOf(1, 2, 4, 8, 16, 1_024, 8_192, 65_536).forEach {
            assertTrue("power $it", MilestoneMath.isPowerOfTwo(it))
        }
        listOf(-2, 0, 3, 6, 12, 1_000, 1_023, 1_025).forEach {
            assertFalse("power $it", MilestoneMath.isPowerOfTwo(it))
        }

        listOf(10, 12, 98, 123, 1_234, 4_567, 6_789, 9_876, 4_321, 3_210, 7_777, 99_999)
            .forEach { assertTrue("digits $it", MilestoneMath.isDigitSequence(it)) }
        listOf(1, 13, 1_000, 1_235, 1_243, 1_357, 4_568, 12_340)
            .forEach { assertFalse("digits $it", MilestoneMath.isDigitSequence(it)) }

        listOf(1, 2, 3, 5, 8, 13, 21, 144, 6_765, 832_040, 433_494_437)
            .forEach { assertTrue("fibonacci $it", MilestoneMath.isFibonacci(it)) }
        listOf(-3, 0, 4, 6, 7, 9, 145, 1_000, 6_764, 6_766)
            .forEach { assertFalse("fibonacci $it", MilestoneMath.isFibonacci(it)) }
    }

    @Test
    fun currentKindsAreFilteredOrderedAndSuppressDayZero() {
        val all = MilestoneKind.entries.toSet()
        assertEquals(emptyList<MilestoneKind>(), MilestoneMath.currentMilestoneKinds(0, all))
        assertEquals(
            listOf(MilestoneKind.TAUSENDER, MilestoneKind.ZEHNTAUSENDER, MilestoneKind.QUADRAT),
            MilestoneMath.currentMilestoneKinds(10_000, all),
        )
        assertEquals(
            listOf(MilestoneKind.PRIMZAHL, MilestoneKind.PALINDROM),
            MilestoneMath.currentMilestoneKinds(
                11,
                setOf(MilestoneKind.PRIMZAHL, MilestoneKind.PALINDROM),
            ),
        )
        assertEquals(
            emptyList<MilestoneKind>(),
            MilestoneMath.currentMilestoneKinds(1_000, setOf(MilestoneKind.PRIMZAHL)),
        )
    }

    @Test
    fun upcomingKindsCollapseAtSameTargetAndRemainStrictlyOrdered() {
        val birth = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val result = MilestoneMath.upcomingMilestones(
            displayedDays = 10,
            birth = birth,
            daysOffEarth = 2,
            enabled = setOf(
                MilestoneKind.PRIMZAHL,
                MilestoneKind.PALINDROM,
                MilestoneKind.QUADRAT,
                MilestoneKind.CATALAN,
            ),
        )

        assertEquals(listOf(11, 14, 16), result.map { it.target })
        assertEquals(
            listOf(MilestoneKind.PRIMZAHL, MilestoneKind.PALINDROM),
            result.first().kinds,
        )
        assertEquals(birth.plusDays(13), result.first().date)
    }

    @Test
    fun defaultSetContainsEveryTopLevelKindExceptTwinPrime() {
        assertEquals(
            MilestoneKind.entries.toSet() - MilestoneKind.ZWILLINGSPRIMZAHL,
            MilestoneKind.defaultEnabled,
        )
    }

    @Test
    fun boundedGeneratorsRemainStrictAndValidAtDisplayClamp() {
        val clamp = ErdentageLimits.MAX_DISPLAYED_DAYS
        val prime = MilestoneMath.nextPrime(clamp)
        val twinPrime = MilestoneMath.nextTwinPrime(clamp)
        val palindrome = MilestoneMath.nextPalindrome(clamp)
        val square = MilestoneMath.nextPerfectSquare(clamp)
        val power = MilestoneMath.nextPowerOfTwo(clamp)
        val digits = MilestoneMath.nextDigitSequence(clamp)
        val fibonacci = MilestoneMath.nextFibonacci(clamp)

        listOf(prime, twinPrime, palindrome, square, power, digits, fibonacci).forEach {
            assertTrue("$it must be strictly greater than $clamp", it > clamp)
        }
        assertTrue(MilestoneMath.isPrime(prime))
        assertTrue(MilestoneMath.isTwinPrime(twinPrime))
        assertTrue(MilestoneMath.isPalindrome(palindrome))
        assertEquals(square, MilestoneMath.nextPerfectSquare(square - 1))
        assertTrue(MilestoneMath.isPowerOfTwo(power))
        assertTrue(MilestoneMath.isDigitSequence(digits))
        assertTrue(MilestoneMath.isFibonacci(fibonacci))
        assertNull(MilestoneMath.nextCatalan(clamp))
    }
}
