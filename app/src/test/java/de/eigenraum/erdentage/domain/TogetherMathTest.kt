// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ported from the iOS „Together — Wechselwirkungen“ suite at tag `v1.4.2-build19`. */
class TogetherMathTest {
    // floorDiv / ceilDiv

    @Test
    fun floorDivRoundsTowardNegativeInfinity() {
        assertEquals(3L, TogetherMath.floorDiv(7, 2))
        assertEquals(-4L, TogetherMath.floorDiv(-7, 2))
        assertEquals(-4L, TogetherMath.floorDiv(-8, 2))
        assertEquals(0L, TogetherMath.floorDiv(0, 3))
    }

    @Test
    fun ceilDivRoundsTowardPositiveInfinity() {
        assertEquals(4L, TogetherMath.ceilDiv(7, 2))
        assertEquals(4L, TogetherMath.ceilDiv(8, 2))
        assertEquals(-3L, TogetherMath.ceilDiv(-7, 2))
        assertEquals(0L, TogetherMath.ceilDiv(0, 3))
        // An exact multiple stays on that multiple rather than advancing.
        assertEquals(1L, TogetherMath.ceilDiv(36_525, 36_525))
        assertEquals(2L, TogetherMath.ceilDiv(36_526, 36_525))
    }

    // A. Doppelt-Tag

    @Test
    fun theDoublingDayIsTheDayTheYoungerReachesTheAgeGap() {
        val list = TogetherMath.anchors(listOf(person("Anne", 1970, 1, 1), person("Bernd", 2000, 1, 1)))
        val events = TogetherMath.doubleEvents(list, today(2026, 8, 15))

        assertEquals(1, events.size)
        val event = events.single()
        val gap = list[1].c - list[0].c
        val motherDays = event.dayIndex - list[0].c
        val childDays = event.dayIndex - list[1].c
        assertEquals(gap, childDays)
        assertEquals(2 * childDays, motherDays)
    }

    @Test
    fun theDoublingDayExistsExactlyOnceAndNeverInThePast() {
        val list = TogetherMath.anchors(listOf(person("Anne", 1970, 1, 1), person("Bernd", 1975, 1, 1)))
        assertTrue(TogetherMath.doubleEvents(list, today(2026, 8, 15)).isEmpty())
    }

    @Test
    fun daysOffEarthShiftsTheEffectiveGap() {
        val plain = TogetherMath.anchors(listOf(person("A", 1970, 1, 1), person("B", 2000, 1, 1)))
        val shifted = TogetherMath.anchors(listOf(person("A", 1970, 1, 1), person("B", 2000, 1, 1, off = 10)))
        val today = today(2026, 8, 15)
        val a = TogetherMath.doubleEvents(plain, today).single().dayIndex
        val b = TogetherMath.doubleEvents(shifted, today).single().dayIndex
        assertEquals(20L, b - a)
    }

    // B. Zusammen so alt wie

    @Test
    fun twoPeopleTogetherAsOldAsAThirdHasTheClosedForm() {
        val mother = person("Anne", 1990, 1, 1)
        val kid1 = person("Mia", 2015, 1, 1)
        val kid2 = person("Ben", 2018, 1, 1)
        val list = TogetherMath.anchors(listOf(mother, kid1, kid2))
        val events = TogetherMath.sumAsOldEvents(list, today(2026, 8, 15))

        val e = events.first { it.participantIds == listOf(list[1].id, list[2].id, list[0].id) }
        val motherDays = e.dayIndex - list[0].c
        val sumKids = (e.dayIndex - list[1].c) + (e.dayIndex - list[2].c)
        assertEquals(sumKids, motherDays)
        assertEquals((list[1].c - list[0].c) + (list[2].c - list[0].c), motherDays)
    }

    // C. Jahrhundert-Summen

    private val threePeople = listOf(
        person("A", 1980, 5, 4),
        person("B", 1982, 11, 17),
        person("C", 2012, 3, 9),
    )

    private fun sumOn(list: List<TogetherAnchor>, day: Long): Long = list.sumOf { day - it.c }

    @Test
    fun theCenturyDayIsTheDayTheContinuousSumCrossesTheTarget() {
        val list = TogetherMath.anchors(threePeople)
        val today = today(2026, 8, 15)
        val events = TogetherMath.centuryEvents(list, today).filter { it.participantIds.size == 3 }

        assertEquals(1, events.size)
        val t = events.single().dayIndex
        val target = (sumOn(list, today) / TogetherMath.DAYS_PER_CENTURY + 1) * TogetherMath.DAYS_PER_CENTURY
        assertTrue(sumOn(list, t) <= target)
        assertTrue(sumOn(list, t + 1) > target)
        assertTrue(target - sumOn(list, t) < list.size)
    }

    @Test
    fun everySubsetGetsExactlyOneCenturyDay() {
        val list = TogetherMath.anchors(threePeople)
        val today = today(2026, 8, 15)
        val events = TogetherMath.centuryEvents(list, today)
        assertEquals(4, events.size)
        assertTrue(events.all { it.dayIndex >= today })
        assertTrue(events.all { it.participantIds.size >= 2 })
    }

    @Test
    fun aGroupPastOneHundredYearsGetsItsTwoHundredYearDay() {
        val list = TogetherMath.anchors(listOf(person("A", 1930, 1, 1), person("B", 1935, 1, 1)))
        val today = today(2026, 8, 15)
        val events = TogetherMath.centuryEvents(list, today).filter { it.participantIds.size == 2 }

        assertEquals(1, events.size)
        val event = events.single()
        assertTrue(sumOn(list, today) > TogetherMath.DAYS_PER_CENTURY)
        assertTrue(sumOn(list, event.dayIndex) > TogetherMath.DAYS_PER_CENTURY)
        assertTrue(sumOn(list, event.dayIndex) <= 2 * TogetherMath.DAYS_PER_CENTURY)
        assertEquals("Alle zusammen: 200 Jahre", event.headline)
        assertEquals("73.050 Erdentage", event.caption)
    }

    @Test
    fun aCenturyRowIsStillShownOnTheDayItHappens() {
        val list = TogetherMath.anchors(threePeople)
        for (ahead in TogetherMath.centuryEvents(list, today(2026, 8, 15))) {
            val onTheDay = TogetherMath.centuryEvents(list, ahead.dayIndex)
                .first { it.participantIds == ahead.participantIds }
            assertEquals(ahead.dayIndex, onTheDay.dayIndex)
            assertEquals(ahead.id, onTheDay.id)
            assertEquals(ahead.caption, onTheDay.caption)
        }
    }

    @Test
    fun theCrossingDaySurvivesBothParitiesOfTheTarget() {
        for (shift in 0..1) {
            val list = TogetherMath.anchors(
                listOf(person("A", 1980, 5, 4), person("B", 1982, 11, 17 + shift)),
            )
            val ahead = TogetherMath.centuryEvents(list, today(2026, 8, 15)).first()
            val onTheDay = TogetherMath.centuryEvents(list, ahead.dayIndex).first()
            assertEquals(ahead.dayIndex, onTheDay.dayIndex)
            assertEquals(ahead.id, onTheDay.id)
            val after = TogetherMath.centuryEvents(list, ahead.dayIndex + 1).first()
            assertTrue(after.dayIndex > ahead.dayIndex)
        }
    }

    // Enumeration caps

    @Test
    fun allSubsetsAreEnumeratedUpToThePeopleLimitAndCappedAbove() {
        assertEquals(11, TogetherMath.centurySubsets(4).size)
        assertEquals(4_095 - 12, TogetherMath.centurySubsets(12).size)
        assertTrue(TogetherMath.centurySubsets(1).isEmpty())
        assertTrue(TogetherMath.centurySubsets(4).all { it.size >= 2 })
        val big = TogetherMath.centurySubsets(20)
        assertEquals(190 + 1_140 + 1, big.size)
        assertTrue(big.any { it.size == 20 })
        assertFalse(big.any { it.size == 4 })
    }

    @Test
    fun combinationsAreAscendingAndComplete() {
        assertEquals(6, TogetherMath.combinations(4, 2).size)
        assertTrue(TogetherMath.combinations(4, 2).all { it == it.sorted() })
        assertTrue(TogetherMath.combinations(3, 4).isEmpty())
        assertTrue(TogetherMath.combinations(3, 0).isEmpty())
    }

    // Konstellationen

    @Test
    fun aTwinPrimeGapIsTaggedOnceNotAsBothPrimeKinds() {
        val constellation = TogetherMath.constellations(
            listOf(person("A", 2000, 1, 1), person("B", 2000, 4, 11)),
        ).single()
        assertEquals(101L, constellation.gap)
        assertTrue(MilestoneKind.ZWILLINGSPRIMZAHL in constellation.tags)
        assertFalse(MilestoneKind.PRIMZAHL in constellation.tags)
    }

    @Test
    fun eventIdsNameTheSetOfParticipantsNotTheirOrder() {
        val anne = person("Anne", 1985, 4, 12)
        val bernd = person("Bernd", 2015, 7, 3)
        val mia = person("Mia", 2018, 11, 20)
        val now = LocalDate.of(2026, 8, 20)
        val tenEach = { _: TogetherKind -> 10 }

        val forward = TogetherMath.events(listOf(anne, bernd, mia), now, tenEach)
        val shuffled = TogetherMath.events(listOf(mia, anne, bernd), now, tenEach)

        assertTrue(forward.isNotEmpty())
        assertEquals(forward.map { it.id }.toSet(), shuffled.map { it.id }.toSet())
        assertEquals(forward.map { it.addedKey }.toSet(), shuffled.map { it.addedKey }.toSet())
        assertEquals(TogetherKind.entries.toSet(), forward.map { it.kind }.toSet())
    }

    @Test
    fun aOneDayGapSaysTagNotTage() {
        val constellation = TogetherMath.constellations(
            listOf(person("Anne", 1990, 5, 1), person("Bernd", 1990, 5, 2)),
        ).single()
        assertEquals(1L, constellation.gap)
        assertTrue(constellation.detail.startsWith("1 Tag auseinander"))
        assertTrue(TogetherMath.constellationDetail("Anne", "Bernd", 2).startsWith("2 Tage auseinander"))
    }

    @Test
    fun samedayBirthsProduceNoConstellationAndNoDoublingDay() {
        val twins = listOf(person("Anne", 2000, 1, 1), person("Bernd", 2000, 1, 1))
        assertTrue(TogetherMath.constellations(twins).isEmpty())
        assertTrue(TogetherMath.doubleEvents(TogetherMath.anchors(twins), today(2026, 1, 1)).isEmpty())
    }

    // Phrasing

    @Test
    fun theOwnerSpeaksInTheFirstPersonNominativeInComparisons() {
        assertEquals("Ich bin doppelt so alt wie Bernd", TogetherMath.doubleHeadline("Ich", "Bernd"))
        assertEquals("Anne ist doppelt so alt wie ich", TogetherMath.doubleHeadline("Anne", ""))
        assertEquals("so alt, wie Anne war, als ich geboren wurde", TogetherMath.doubleCaption("Anne", "Ich"))
        assertEquals("Mia und ich sind zusammen so alt wie Anne", TogetherMath.sumHeadline(listOf("Mia", "Ich"), "Anne"))
    }

    @Test
    fun enumerationsNameTheOwnerLastWhateverOrderTheyArriveIn() {
        assertEquals("Mia und ich sind zusammen so alt wie Anne", TogetherMath.sumHeadline(listOf("Ich", "Mia"), "Anne"))
        assertEquals("Mia und ich sind zusammen so alt wie Anne", TogetherMath.sumHeadline(listOf("", "Mia"), "Anne"))
        assertEquals("Mia, Bernd und ich", TogetherMath.joinNames(listOf("Ich", "Mia", "Bernd")))
        assertEquals("Mia und Bernd", TogetherMath.joinNames(listOf("Mia", "Bernd")))
        assertTrue(
            TogetherMath.sumShare(listOf("Ich", "Mia"), "Anne", LocalDate.of(2030, 5, 1))
                .contains("sind Mia und ich zusammen so alt wie Anne"),
        )
    }

    @Test
    fun aConstellationWithTheOwnerNamesThemLastAndOrdersItsDotsToMatch() {
        val people = listOf(person("", 1980, 1, 1), person("Anne", 1990, 1, 1))
        val constellation = TogetherMath.constellations(people).single()
        assertEquals("Anne & ich", constellation.headline)
        assertEquals(listOf(people[1].id, people[0].id), constellation.displayIds)
        assertEquals(people[0].id, constellation.olderId)
        assertTrue(constellation.detail.endsWith("so alt war ich, als Anne geboren wurde."))

        val named = listOf(person("Bernd", 1980, 1, 1), person("Mia", 1990, 1, 1))
        val plain = TogetherMath.constellations(named).single()
        assertEquals("Bernd & Mia", plain.headline)
        assertEquals(listOf(named[0].id, named[1].id), plain.displayIds)
    }

    @Test
    fun groupLabelsCollapseToAlleAndAlleAusser() {
        val list = TogetherMath.anchors(
            listOf(person("Anne", 1980, 1, 1), person("Bernd", 1982, 1, 1), person("Mia", 2012, 1, 1)),
        )
        assertEquals("Alle zusammen: 100 Jahre", TogetherMath.centuryHeadline(listOf(0, 1, 2), list, 100))
        assertEquals("Alle außer Mia: 200 Jahre", TogetherMath.centuryHeadline(listOf(0, 1), list, 200))
        val four = TogetherMath.anchors(
            listOf(
                person("Anne", 1980, 1, 1),
                person("Bernd", 1982, 1, 1),
                person("Mia", 2012, 1, 1),
                person("Opa", 1955, 1, 1),
            ),
        )
        assertEquals("Bernd und Mia: 100 Jahre", TogetherMath.centuryHeadline(listOf(1, 2), four, 100))
        val withOwner = TogetherMath.anchors(
            listOf(person("Anne", 1980, 1, 1), person("Bernd", 1982, 1, 1), person("", 2012, 1, 1)),
        )
        assertEquals("Alle außer mir: 100 Jahre", TogetherMath.centuryHeadline(listOf(0, 1), withOwner, 100))
    }

    private val family = listOf(
        person("", 1990, 3, 15),
        person("Bernd", 2015, 6, 2),
        person("Mia", 2018, 11, 20),
        person("Opa", 1955, 1, 10),
    )

    @Test
    fun eventsAreOrderedRarestKindFirstAndCappedPerKind() {
        val events = TogetherMath.events(family, LocalDate.of(2026, 8, 15))
        assertTrue(events.isNotEmpty())
        val ranks = events.map { it.kind.rarityRank }
        assertEquals(ranks.sorted(), ranks)
        for (kind in TogetherKind.entries) {
            val days = events.filter { it.kind == kind }.map { it.dayIndex }
            assertEquals(days.sorted(), days)
            assertTrue(days.size <= kind.defaultLimit)
        }
        assertTrue(events.count { it.kind == TogetherKind.JAHRHUNDERT } <= 3)
        assertTrue(events.size <= TogetherMath.MAX_EVENTS)
    }

    @Test
    fun perCategoryLimitsAreHonouredAndZeroSwitchesACategoryOff() {
        val now = LocalDate.of(2026, 8, 15)
        val one = TogetherMath.events(family, now) { 1 }
        for (kind in TogetherKind.entries) {
            assertTrue(one.count { it.kind == kind } <= 1)
        }

        val noCenturies = TogetherMath.events(family, now) { kind ->
            if (kind == TogetherKind.JAHRHUNDERT) 0 else 4
        }
        assertFalse(noCenturies.any { it.kind == TogetherKind.JAHRHUNDERT })
        assertTrue(noCenturies.any { it.kind == TogetherKind.DOPPELT })

        assertTrue(TogetherMath.events(family, now) { 0 }.isEmpty())

        val absurd = TogetherMath.events(family, now) { kind ->
            when (kind) {
                TogetherKind.DOPPELT -> 999
                TogetherKind.ZUSAMMEN_SO_ALT -> -5
                TogetherKind.JAHRHUNDERT -> 999
            }
        }
        assertFalse(absurd.any { it.kind == TogetherKind.ZUSAMMEN_SO_ALT })
        assertTrue(absurd.count { it.kind == TogetherKind.DOPPELT } <= TogetherKind.LIMIT_RANGE.last)
        assertTrue(absurd.size <= TogetherMath.MAX_EVENTS)
    }

    @Test
    fun shareSentencesInvertTheVerbAfterTheFrontedDate() {
        val date = LocalDate.of(2040, 8, 19)
        assertTrue(
            TogetherMath.doubleShare("Ich", "Bernd", date)
                .startsWith("Am Sonntag, 19. August 2040 bin ich doppelt so alt wie Bernd"),
        )
        assertTrue(TogetherMath.doubleShare("Opa", "", date).contains("ist Opa doppelt so alt wie ich"))
        assertTrue(
            TogetherMath.sumShare(listOf("Mia", "Ben"), "", date)
                .contains("sind Mia und Ben zusammen so alt wie ich"),
        )

        val list = TogetherMath.anchors(
            listOf(person("", 1980, 1, 1), person("Bernd", 1982, 1, 1), person("Mia", 2012, 1, 1)),
        )
        assertTrue(
            TogetherMath.centuryShare(listOf(0, 1, 2), list, 100, date)
                .contains("sind alle zusammen 100 Jahre alt"),
        )
        assertTrue(
            TogetherMath.centuryShare(listOf(1, 2), list, 100, date)
                .contains("sind alle außer mir zusammen 100 Jahre alt"),
        )
        val named = TogetherMath.anchors(
            listOf(person("Anne", 1980, 1, 1), person("Bernd", 1982, 1, 1), person("Mia", 2012, 1, 1)),
        )
        val ownerless = TogetherMath.centuryShare(listOf(0, 1, 2), named, 100, date)
        assertTrue(ownerless.contains("sind alle zusammen 100 Jahre alt"))
        assertFalse(ownerless.contains("wir"))

        assertEquals(
            "Alle zusammen: 100 Jahre (36.525 Erdentage)",
            TogetherMath.centuryCalendarTitle(listOf(0, 1, 2), list, 100, 36_525),
        )
    }

    @Test
    fun addedMarkerKeysChangeWhenTheDayMoves() {
        val owner = person("", 1990, 3, 15)
        val bernd = person("Bernd", 2015, 6, 2)
        val now = LocalDate.of(2026, 8, 15)
        val a = TogetherMath.events(listOf(owner, bernd), now).first { it.kind == TogetherKind.DOPPELT }
        val b = TogetherMath.events(listOf(owner, bernd.copy(birthDay = 3)), now)
            .first { it.kind == TogetherKind.DOPPELT }
        assertEquals(a.id, b.id)
        assertNotEquals(a.addedKey, b.addedKey)
    }

    @Test
    fun aThreePersonSumCrossesInsideTheDay() {
        val people = listOf(
            person("Anne", 1984, 5, 22),
            person("Bernd", 2012, 9, 3),
            person("Caro", 2016, 4, 1),
            person("Mia", 2019, 7, 15),
        )
        val c = TogetherMath.anchors(people).associate { it.id to it.c }
        val event = TogetherMath.events(people, LocalDate.of(2026, 8, 15))
            .first { it.kind == TogetherKind.ZUSAMMEN_SO_ALT && it.participantIds.size == 4 }

        val group = event.participantIds.dropLast(1)
        val other = event.participantIds.last()
        fun counts(day: Long) = group.sumOf { day - c.getValue(it) } to (day - c.getValue(other))
        val onDay = counts(event.dayIndex)
        val nextDay = counts(event.dayIndex + 1)
        assertEquals(1L, onDay.second - onDay.first)
        assertEquals(1L, nextDay.first - nextDay.second)
    }

    @Test
    fun theSecondReadingIsDroppedWhenAnOffsetWouldMakeItFalse() {
        assertTrue(TogetherMath.constellationDetail("Anne", "Bernd", 987).contains("so alt war Anne"))
        assertEquals("987 Tage auseinander.", TogetherMath.constellationDetail("Anne", "Bernd", 987, 12))

        val anne = person("Anne", 1984, 5, 22)
        val bernd = person("Bernd", 1987, 1, 30)
        val withOffset = TogetherMath.constellations(listOf(anne, bernd.copy(daysOffEarth = 12))).single()
        assertFalse(withOffset.detail.contains("so alt war"))
        val olderOnly = TogetherMath.constellations(listOf(anne.copy(daysOffEarth = 12), bernd)).single()
        assertTrue(olderOnly.detail.contains("so alt war"))
    }

    @Test
    fun aThirteenthPersonDoesNotDeleteThreePersonSums() {
        fun family(count: Int): List<Person> =
            listOf(person("Oma", 1960, 1, 1)) + (1 until count).map {
                person("P$it", 2010 + (it % 12), 1 + (it % 12), 1 + (it % 27))
            }
        fun tripleCount(people: List<Person>): Int = TogetherMath.sumAsOldEvents(
            TogetherMath.anchors(people),
            today(2026, 8, 15),
        ).count { it.participantIds.size == 4 }

        val twelve = tripleCount(family(12))
        assertTrue(twelve > 0)
        assertTrue(tripleCount(family(13)) >= twelve)
        assertTrue(tripleCount(family(20)) > 0)
    }

    @Test
    fun addedMarkerKeysUseTheCivilDayAndSpellItLikePersonalMilestones() {
        val event = TogetherEvent(
            kind = TogetherKind.DOPPELT,
            date = LocalDate.of(2029, 2, 15),
            dayIndex = 21_595,
            participantIds = emptyList(),
            headline = "",
            caption = null,
            shareText = "",
            calendarTitle = "",
            id = "pair",
        )
        assertEquals("pair@20290215", event.addedKey)
        assertEquals("20260305", TogetherMath.civilDayKey(LocalDate.of(2026, 3, 5)))
        assertEquals(
            "13000-20260305",
            DayCountLogic.milestoneKey(13_000, LocalDate.of(2026, 3, 5).atStartOfDay(java.time.ZoneId.of("Europe/Berlin")), false),
        )
    }

    @Test
    fun peopleWithoutAValidBirthDateAreLeftOut() {
        val ghost = person("Geist", 0, 11, 29)
        val anne = person("Anne", 1980, 1, 1)
        assertTrue(TogetherMath.events(listOf(ghost, anne), LocalDate.of(2026, 8, 15)).isEmpty())
        assertTrue(TogetherMath.constellations(listOf(ghost, anne)).isEmpty())
    }

    @Test
    fun twentyPeopleStayFastEnough() {
        val people = (0 until ErdentageLimits.MAX_PEOPLE).map {
            person("P$it", 1950 + it * 3, 1 + (it % 12), 1 + (it % 27))
        }
        val started = System.nanoTime()
        val events = TogetherMath.events(people, LocalDate.of(2026, 8, 15)) { 10 }
        val constellations = TogetherMath.constellations(people)
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        assertTrue(events.size <= TogetherMath.MAX_EVENTS)
        assertEquals(TogetherMath.MAX_CONSTELLATIONS, constellations.size)
        assertTrue("took $elapsedMillis ms", elapsedMillis < 2_000)
    }

    @Test
    fun distanceBucketsMatchTheIosRow() {
        val today = 20_000L
        assertEquals(TogetherDistance.Today, TogetherMath.distance(today, today))
        assertEquals(TogetherDistance.Today, TogetherMath.distance(today - 5, today))
        assertEquals(TogetherDistance.Tomorrow, TogetherMath.distance(today + 1, today))
        assertEquals(TogetherDistance.Days(59), TogetherMath.distance(today + 59, today))
        assertEquals(TogetherDistance.Months(2), TogetherMath.distance(today + 60, today))
        assertEquals(TogetherDistance.Months(3), TogetherMath.distance(today + 91, today))
        assertEquals(TogetherDistance.Months(24), TogetherMath.distance(today + 729, today))
        assertEquals(TogetherDistance.Years(2), TogetherMath.distance(today + 730, today))
        assertEquals(TogetherDistance.Years(24), TogetherMath.distance(today + 8_762, today))
    }

    // Geburtstags-Zufälle on the rows

    @Test
    fun aWechselwirkungLandingOnABirthdaySaysWhoseItIs() {
        val kai = person("Kai", 1975, 3, 11)
        val albert = person("Albert", 2012, 7, 20)
        val nurDoppelt = { kind: TogetherKind -> if (kind == TogetherKind.DOPPELT) 5 else 0 }
        val heute = LocalDate.of(2026, 8, 23)

        val paar = TogetherMath.events(listOf(kai, albert), heute, nurDoppelt)
        assertEquals(1, paar.size)
        val verdopplung = paar.single()
        assertEquals("20491129", TogetherMath.civilDayKey(verdopplung.date))
        assertNull(verdopplung.birthdayNote)
        assertEquals(verdopplung.shareText, verdopplung.shareMessage)

        val oskar = person("Oskar", 2018, 11, 29)
        val familie = TogetherMath.events(listOf(kai, albert, oskar), heute, nurDoppelt)
        val gleicheZeile = familie.first { it.id == verdopplung.id }
        assertEquals(verdopplung.date, gleicheZeile.date)
        assertEquals("🎂 an Oskars 31. Geburtstag", gleicheZeile.birthdayNote?.caption)
        assertTrue(gleicheZeile.shareMessage.endsWith("Es ist Oskars 31. Geburtstag. 🎂"))
    }

    @Test
    fun switchingItOffSilencesTheNoteWithoutMovingARow() {
        val kai = person("Kai", 1975, 3, 11)
        val albert = person("Albert", 2012, 7, 20)
        val oskar = person("Oskar", 2018, 11, 29)
        val heute = LocalDate.of(2026, 8, 23)

        val mit = TogetherMath.events(listOf(kai, albert, oskar), heute)
        val ohne = TogetherMath.events(listOf(kai, albert, oskar.copy(mentionsBirthday = false)), heute)

        assertEquals(mit.map { it.id }, ohne.map { it.id })
        assertEquals(mit.map { it.date }, ohne.map { it.date })
        assertTrue(mit.any { it.birthdayNote?.caption?.contains("Oskars") == true })
        assertTrue(ohne.none { it.birthdayNote?.caption?.contains("Oskars") == true })
        assertNotNull(ohne.firstOrNull { it.birthdayNote?.caption?.contains("Alberts") == true })
    }

    private fun person(
        name: String,
        year: Int,
        month: Int,
        day: Int,
        off: Int = 0,
    ): Person = Person(
        id = UUID.nameUUIDFromBytes("$name-$year-$month-$day".toByteArray()).toString(),
        name = name,
        birthYear = year,
        birthMonth = month,
        birthDay = day,
        daysOffEarth = off,
    )

    private fun today(year: Int, month: Int, day: Int): Long =
        TogetherMath.dayIndex(LocalDate.of(year, month, day))
}
