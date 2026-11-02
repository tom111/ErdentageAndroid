// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.ZonedDateTime
import kotlin.math.sqrt

data class MilestoneCandidate(
    val target: Int,
    val kinds: List<MilestoneKind>,
    val date: ZonedDateTime,
)

object MilestoneMath {
    val catalanNumbers: List<Int> = listOf(
        1,
        2,
        5,
        14,
        42,
        132,
        429,
        1_430,
        4_862,
        16_796,
        58_786,
        208_012,
        742_900,
        2_674_440,
        9_694_845,
        35_357_670,
        129_644_790,
        477_638_700,
    )

    fun nextMultiple(step: Int, after: Int): Int {
        require(step > 0) { "step must be positive" }
        return (after / step + 1) * step
    }

    fun isPrime(value: Int): Boolean {
        if (value < 2) return false
        if (value == 2 || value == 3) return true
        if (value % 2 == 0 || value % 3 == 0) return false
        var divisor = 5
        while (divisor <= value / divisor) {
            if (value % divisor == 0 || value % (divisor + 2) == 0) return false
            divisor += 6
        }
        return true
    }

    fun nextPrime(after: Int): Int {
        if (after < 1) return 2
        var candidate = after + 1
        if (candidate > 2 && candidate % 2 == 0) candidate++
        while (!isPrime(candidate)) candidate += 2
        return candidate
    }

    fun isTwinPrime(value: Int): Boolean =
        isPrime(value) && (isPrime(value - 2) || isPrime(value + 2))

    fun nextTwinPrime(after: Int): Int {
        var candidate = nextPrime(after)
        while (!isTwinPrime(candidate)) candidate = nextPrime(candidate)
        return candidate
    }

    fun isPalindrome(value: Int): Boolean {
        val text = value.toString()
        return text == text.reversed()
    }

    fun nextPalindrome(after: Int): Int {
        if (after < 0) return 0
        val target = after + 1
        val digits = target.toString().map(Char::digitToInt).toMutableList()
        val half = digits.size / 2

        fun recompose(): Int = digits.fold(0) { result, digit -> result * 10 + digit }

        repeat(half) { index -> digits[digits.lastIndex - index] = digits[index] }
        val mirrored = recompose()
        if (mirrored >= target) return mirrored

        var index = (digits.size - 1) / 2
        while (index >= 0) {
            if (digits[index] == 9) {
                digits[index] = 0
                index--
            } else {
                digits[index]++
                break
            }
        }
        repeat(half) { mirrorIndex ->
            digits[digits.lastIndex - mirrorIndex] = digits[mirrorIndex]
        }
        return recompose()
    }

    fun nextPerfectSquare(after: Int): Int {
        if (after < 1) return 1
        val root = sqrt(after.toDouble()).toInt()
        for (candidateRoot in root..(root + 2)) {
            val candidate = candidateRoot.toLong() * candidateRoot
            if (candidate > after) return candidate.toInt()
        }
        return ((root + 2).toLong() * (root + 2)).toInt()
    }

    fun nextCatalan(after: Int): Int? = catalanNumbers.firstOrNull { it > after }

    fun isPowerOfTwo(value: Int): Boolean = value > 0 && (value and (value - 1)) == 0

    fun nextPowerOfTwo(after: Int): Int {
        if (after < 1) return 1
        var candidate = 1
        while (candidate <= after) candidate = candidate shl 1
        return candidate
    }

    fun isDigitSequence(value: Int): Boolean {
        if (value < 10) return false
        val digits = value.toString().map(Char::digitToInt)
        val pairs = digits.zipWithNext()
        val ascending = pairs.all { (first, second) -> second == first + 1 }
        val descending = pairs.all { (first, second) -> second == first - 1 }
        val repeated = digits.all { it == digits.first() }
        return ascending || descending || repeated
    }

    fun nextDigitSequence(after: Int): Int {
        var length = maxOf(2, maxOf(after.toLong() + 1, 10).toString().length)
        while (length <= 10) {
            var best: Long? = null
            fun consider(value: Long) {
                if (value > after && value <= Int.MAX_VALUE && (best == null || value < best!!)) {
                    best = value
                }
            }

            var repunit = 0L
            repeat(length) { repunit = repunit * 10 + 1 }
            for (digit in 1..9) consider(digit * repunit)

            if (length <= 9) {
                for (start in 1..(10 - length)) {
                    var value = 0L
                    repeat(length) { offset -> value = value * 10 + start + offset }
                    consider(value)
                }
            }

            if (length <= 10) {
                for (high in maxOf(1, length - 1)..9) {
                    var value = 0L
                    repeat(length) { offset -> value = value * 10 + high - offset }
                    consider(value)
                }
            }

            best?.let { return it.toInt() }
            length++
        }
        error("No Int digit sequence exists after $after")
    }

    fun isFibonacci(value: Int): Boolean {
        if (value < 1) return false
        var first = 1
        var second = 2
        while (first < value) {
            val next = first + second
            first = second
            second = next
        }
        return first == value
    }

    fun nextFibonacci(after: Int): Int {
        var first = 1
        var second = 2
        while (first <= after) {
            val next = first.toLong() + second
            first = second
            second = next.toInt()
        }
        return first
    }

    fun currentMilestoneKinds(
        days: Int,
        enabled: Set<MilestoneKind>,
    ): List<MilestoneKind> {
        if (days <= 0 || enabled.isEmpty()) return emptyList()
        return MilestoneKind.entries.filter { kind ->
            kind in enabled && when (kind) {
                MilestoneKind.TAUSENDER -> days % 1_000 == 0
                MilestoneKind.ZEHNTAUSENDER -> days % 10_000 == 0
                MilestoneKind.PRIMZAHL -> isPrime(days)
                MilestoneKind.ZWILLINGSPRIMZAHL -> isTwinPrime(days)
                MilestoneKind.PALINDROM -> isPalindrome(days)
                MilestoneKind.QUADRAT -> {
                    val root = sqrt(days.toDouble()).toInt()
                    (root..(root + 1)).any { it > 0 && it.toLong() * it == days.toLong() }
                }
                MilestoneKind.ZWEIERPOTENZ -> isPowerOfTwo(days)
                MilestoneKind.ZIFFERNFOLGE -> isDigitSequence(days)
                MilestoneKind.CATALAN -> days in catalanNumbers
                MilestoneKind.FIBONACCI -> isFibonacci(days)
            }
        }
    }

    fun upcomingMilestones(
        displayedDays: Int,
        birth: ZonedDateTime,
        daysOffEarth: Int,
        enabled: Set<MilestoneKind>,
    ): List<MilestoneCandidate> {
        if (enabled.isEmpty()) return emptyList()
        val raw = buildList {
            if (MilestoneKind.TAUSENDER in enabled) {
                add(nextMultiple(1_000, displayedDays) to MilestoneKind.TAUSENDER)
            }
            if (MilestoneKind.ZEHNTAUSENDER in enabled) {
                add(nextMultiple(10_000, displayedDays) to MilestoneKind.ZEHNTAUSENDER)
            }
            if (MilestoneKind.PRIMZAHL in enabled) {
                add(nextPrime(displayedDays) to MilestoneKind.PRIMZAHL)
            }
            if (MilestoneKind.ZWILLINGSPRIMZAHL in enabled) {
                add(nextTwinPrime(displayedDays) to MilestoneKind.ZWILLINGSPRIMZAHL)
            }
            if (MilestoneKind.PALINDROM in enabled) {
                add(nextPalindrome(displayedDays) to MilestoneKind.PALINDROM)
            }
            if (MilestoneKind.QUADRAT in enabled) {
                add(nextPerfectSquare(displayedDays) to MilestoneKind.QUADRAT)
            }
            if (MilestoneKind.ZWEIERPOTENZ in enabled) {
                add(nextPowerOfTwo(displayedDays) to MilestoneKind.ZWEIERPOTENZ)
            }
            if (MilestoneKind.ZIFFERNFOLGE in enabled) {
                add(nextDigitSequence(displayedDays) to MilestoneKind.ZIFFERNFOLGE)
            }
            if (MilestoneKind.CATALAN in enabled) {
                nextCatalan(displayedDays)?.let { add(it to MilestoneKind.CATALAN) }
            }
            if (MilestoneKind.FIBONACCI in enabled) {
                add(nextFibonacci(displayedDays) to MilestoneKind.FIBONACCI)
            }
        }

        val kindsByTarget = linkedMapOf<Int, MutableList<MilestoneKind>>()
        raw.forEach { (target, kind) -> kindsByTarget.getOrPut(target, ::mutableListOf).add(kind) }
        val offset = ErdentageLimits.normalizeDaysOffEarth(daysOffEarth)
        return kindsByTarget.entries
            .sortedBy { it.key }
            .map { (target, kinds) ->
                MilestoneCandidate(
                    target = target,
                    kinds = kinds,
                    date = birth.plusDays(target.toLong() + offset),
                )
            }
    }
}
