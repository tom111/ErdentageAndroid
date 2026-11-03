// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Wechselwirkungen: relations *between* tracked people, ported from iOS
 * `TogetherLogic.swift` at tag `v1.4.2-build19`.
 *
 * The governing rule is continuous crossing. Every relation is evaluated on
 * continuous ages and reported as the calendar day *during* which the
 * equation is satisfied. With `c` a person's birth day index shifted by
 * `daysOffEarth`, the displayed count on day `t` is `t - c`, and the
 * milestone day is `floor(t*)` for the `t*` solving the relation. Each
 * relation is a strictly monotone line, so every pair or subset has exactly
 * one such day.
 *
 * Time of day is ignored throughout: all arithmetic runs on date-only day
 * indices. `daysOffEarth` is folded in, since it is part of the displayed
 * count.
 *
 * The German phrasing lives here, not in string resources, so the shared
 * fixture in `shared-fixtures/together-math.json` can pin it to the
 * character on both platforms.
 */
data class TogetherAnchor(
    val id: String,
    val name: String,
    /** Birth day index plus `daysOffEarth`: the displayed count on day `t` is `t - c`. */
    val c: Long,
)

/** One dated Wechselwirkung with its German sentences baked in. */
data class TogetherEvent(
    val kind: TogetherKind,
    val date: LocalDate,
    val dayIndex: Long,
    val participantIds: List<String>,
    val headline: String,
    val caption: String?,
    /** Ready-made sentence for the share sheet, verb-second after the fronted date. */
    val shareText: String,
    /** Title for the calendar entry, self-explanatory months later. */
    val calendarTitle: String,
    /** Identity from the sorted participant UUIDs, stable under reordering. */
    val id: String,
    /** Set when the day is a tracked person's birthday; attached after ranking. */
    val birthdayNote: BirthdayNote? = null,
) {
    /** What „Teilen…“ sends: the sentence plus the Geburtstags-Zufall, if any. */
    val shareMessage: String
        get() = if (birthdayNote == null) shareText else "$shareText ${birthdayNote.sentence}"

    /**
     * Identity for the „already added“ marker. Keyed by the civil date, never
     * the day index, so it survives a time-zone change and drops when an
     * edited birth date moves the day.
     */
    val addedKey: String
        get() = "$id@${TogetherMath.civilDayKey(date)}"
}

/** An undated, permanent property of a pair; the empty state of the page. */
data class Constellation(
    val olderId: String,
    val youngerId: String,
    val gap: Long,
    /** The pair in the order the headline names them (owner last). */
    val displayIds: List<String>,
    val headline: String,
    val detail: String,
    /** Milestone kinds the gap itself satisfies, round kinds excluded. */
    val tags: List<MilestoneKind>,
    val id: String,
)

/**
 * Human distance rather than a raw day count: „in 8.762 Tagen“ is
 * unreadable, „in 24 Jahren“ is not. The UI maps each case to its string.
 */
sealed interface TogetherDistance {
    data object Today : TogetherDistance
    data object Tomorrow : TogetherDistance
    data class Days(val days: Int) : TogetherDistance
    data class Months(val months: Int) : TogetherDistance
    data class Years(val years: Int) : TogetherDistance
}

object TogetherMath {
    /** 100 × 365,25: the Julian-year convention the feature came from. */
    const val DAYS_PER_CENTURY = 36_525L

    /** Backstop across all categories, above the per-category budgets. */
    const val MAX_EVENTS = 30

    /** Constellations grow as C(n, 2); shown closest pair first, capped. */
    const val MAX_CONSTELLATIONS = 20

    /** Below this many people every subset is enumerated for century sums. */
    const val FULL_ENUMERATION_PEOPLE_LIMIT = 12
    const val CAPPED_SUBSET_SIZE = 3

    /** One person is not a Wechselwirkung. */
    const val MIN_SUBSET_SIZE = 2

    /** Largest group on the left-hand side of „zusammen so alt wie“, at every family size. */
    const val MAX_SUM_GROUP_SIZE = 3

    private val germanLocale: Locale = Locale.GERMANY
    private val longDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d. MMMM uuuu", germanLocale)

    // Day-index arithmetic

    /** `LocalDate.toEpochDay()`: zone-independent, but never persisted. */
    fun dayIndex(date: LocalDate): Long = date.toEpochDay()

    fun date(dayIndex: Long): LocalDate = LocalDate.ofEpochDay(dayIndex)

    /** Floor division; Kotlin's `/` truncates toward zero. */
    fun floorDiv(a: Long, b: Long): Long {
        require(b > 0) { "floorDiv expects a positive divisor" }
        return Math.floorDiv(a, b)
    }

    /** Ceiling division, the mirror of [floorDiv]. */
    fun ceilDiv(a: Long, b: Long): Long {
        require(b > 0) { "ceilDiv expects a positive divisor" }
        return -floorDiv(-a, b)
    }

    /** Same buckets as the iOS row: days below 60, months below two years, else years. */
    fun distance(dayIndex: Long, today: Long): TogetherDistance {
        val days = dayIndex - today
        return when {
            days <= 0 -> TogetherDistance.Today
            days == 1L -> TogetherDistance.Tomorrow
            days < 60 -> TogetherDistance.Days(days.toInt())
            days < 365 * 2 -> TogetherDistance.Months(maxOf(2, Math.round(days / 30.44).toInt()))
            else -> TogetherDistance.Years(Math.round(days / 365.25).toInt())
        }
    }

    /** `yyyymmdd`, the same spelling `DayCountLogic.milestoneKey` uses. */
    fun civilDayKey(date: LocalDate): String =
        String.format(Locale.ROOT, "%04d%02d%02d", date.year, date.monthValue, date.dayOfMonth)

    fun anchors(people: List<Person>): List<TogetherAnchor> = people.mapNotNull { person ->
        val birth = person.civilBirthDate() ?: return@mapNotNull null
        val off = ErdentageLimits.normalizeDaysOffEarth(person.daysOffEarth)
        TogetherAnchor(id = person.id, name = person.displayName, c = dayIndex(birth) + off)
    }

    // Entry points

    /**
     * The dated rows for [people] as of [today]: rarest kind first,
     * chronological within a kind, each kind cut to its budget from [limit]
     * (zero switches it off) and the whole list to [MAX_EVENTS]. The
     * Geburtstags-Zufälle are attached only to the rows that survive.
     */
    fun events(
        people: List<Person>,
        today: LocalDate,
        limit: (TogetherKind) -> Int = TogetherKind::defaultLimit,
    ): List<TogetherEvent> {
        val list = anchors(people)
        if (list.size < 2) return emptyList()
        val todayIndex = dayIndex(today)
        val out = mutableListOf<TogetherEvent>()
        for (kind in TogetherKind.entries) {
            val budget = TogetherKind.clampLimit(limit(kind))
            if (budget <= 0) continue
            // The limit goes *into* the generator: each one ranks cheap
            // integer candidates first and phrases only the survivors.
            out += when (kind) {
                TogetherKind.DOPPELT -> doubleEvents(list, todayIndex, budget)
                TogetherKind.ZUSAMMEN_SO_ALT -> sumAsOldEvents(list, todayIndex, budget)
                TogetherKind.JAHRHUNDERT -> centuryEvents(list, todayIndex, budget)
            }
        }
        return out
            .sortedWith(compareBy({ it.kind.rarityRank }, { it.dayIndex }, { it.id }))
            .take(MAX_EVENTS)
            .map { event ->
                event.copy(birthdayNote = BirthdayCoincidence.note(event.date, people))
            }
    }

    fun constellations(people: List<Person>): List<Constellation> {
        val list = anchors(people)
        if (list.size < 2) return emptyList()
        // Keyed by id: anchors drop anyone whose stored components do not
        // reconstruct, so the two lists need not line up.
        val offsets = people.associate { it.id to ErdentageLimits.normalizeDaysOffEarth(it.daysOffEarth) }
        val out = mutableListOf<Constellation>()
        for (i in list.indices) {
            for (j in i + 1 until list.size) {
                val (olderIndex, youngerIndex) = ordered(list, i, j)
                val older = list[olderIndex]
                val younger = list[youngerIndex]
                val gap = younger.c - older.c
                // Two people born on the same day have no gap to talk about.
                if (gap <= 0) continue
                val displayOrder = anchorsOwnerLast(older, younger)
                out += Constellation(
                    olderId = older.id,
                    youngerId = younger.id,
                    gap = gap,
                    displayIds = displayOrder.map(TogetherAnchor::id),
                    headline = pairHeadline(displayOrder),
                    detail = constellationDetail(
                        older = older.name,
                        younger = younger.name,
                        gap = gap,
                        youngerDaysOffEarth = offsets[younger.id] ?: 0,
                    ),
                    tags = gapTags(gap),
                    id = "konstellation-${older.id}-${younger.id}",
                )
            }
        }
        return out.sortedWith(compareBy({ it.gap }, { it.id })).take(MAX_CONSTELLATIONS)
    }

    /**
     * Every twin prime is a prime, so asking for all kinds tags such a gap
     * twice; keep the rarer of the pair. Round kinds carry no tag, as on iOS
     * (`eventTitleTag` is nil for them).
     */
    private fun gapTags(gap: Long): List<MilestoneKind> {
        if (gap > ErdentageLimits.MAX_DISPLAYED_DAYS) return emptyList()
        val kinds = MilestoneMath.currentMilestoneKinds(gap.toInt(), MilestoneKind.entries.toSet())
            .toMutableList()
        if (MilestoneKind.ZWILLINGSPRIMZAHL in kinds) kinds.remove(MilestoneKind.PRIMZAHL)
        kinds.remove(MilestoneKind.TAUSENDER)
        kinds.remove(MilestoneKind.ZEHNTAUSENDER)
        return kinds
    }

    // A. Doppelt so alt

    /** `a_A = 2·a_B` ⇒ `t = 2·c_B − c_A`, always a whole day, exactly once per pair. */
    fun doubleEvents(
        anchors: List<TogetherAnchor>,
        today: Long,
        limit: Int = Int.MAX_VALUE,
    ): List<TogetherEvent> {
        data class Candidate(val t: Long, val older: Int, val younger: Int)

        val candidates = mutableListOf<Candidate>()
        for (i in anchors.indices) {
            for (j in i + 1 until anchors.size) {
                val (older, younger) = ordered(anchors, i, j)
                if (anchors[younger].c <= anchors[older].c) continue
                val t = 2 * anchors[younger].c - anchors[older].c
                if (t < today) continue
                candidates += Candidate(t, older, younger)
            }
        }
        return candidates
            .sortedWith(compareBy({ it.t }, { it.older }, { it.younger }))
            .take(limit)
            .map { candidate ->
                val older = anchors[candidate.older]
                val younger = anchors[candidate.younger]
                val whenDate = date(candidate.t)
                TogetherEvent(
                    kind = TogetherKind.DOPPELT,
                    date = whenDate,
                    dayIndex = candidate.t,
                    participantIds = listOf(older.id, younger.id),
                    headline = doubleHeadline(older.name, younger.name),
                    caption = doubleCaption(older.name, younger.name),
                    shareText = doubleShare(older.name, younger.name, whenDate),
                    calendarTitle = doubleHeadline(older.name, younger.name),
                    id = "doppelt-${older.id}-${younger.id}",
                )
            }
    }

    // B. Zusammen so alt wie

    /** `Σ a_i = a_j` ⇒ `t = (Σ c_i − c_j) / (m − 1)`, floored for groups of three. */
    fun sumAsOldEvents(
        anchors: List<TogetherAnchor>,
        today: Long,
        limit: Int = Int.MAX_VALUE,
    ): List<TogetherEvent> {
        data class Candidate(val t: Long, val group: List<Int>, val other: Int)

        val n = anchors.size
        val candidates = mutableListOf<Candidate>()
        // No people-count fallback: group size is a promise to the reader.
        for (size in 2..MAX_SUM_GROUP_SIZE) {
            if (size >= n) continue
            for (group in combinations(n, size)) {
                val sumC = group.sumOf { anchors[it].c }
                for (j in anchors.indices) {
                    if (j in group) continue
                    val t = floorDiv(sumC - anchors[j].c, (size - 1).toLong())
                    if (t < today) continue
                    candidates += Candidate(t, group, j)
                }
            }
        }
        return candidates
            .sortedWith(
                compareBy<Candidate> { it.t }
                    .thenComparator { a, b -> compareLexicographically(a.group, b.group) }
                    .thenBy { it.other },
            )
            .take(limit)
            .map { candidate ->
                val names = candidate.group.map { anchors[it].name }
                val other = anchors[candidate.other]
                val whenDate = date(candidate.t)
                TogetherEvent(
                    kind = TogetherKind.ZUSAMMEN_SO_ALT,
                    date = whenDate,
                    dayIndex = candidate.t,
                    participantIds = candidate.group.map { anchors[it].id } + other.id,
                    headline = sumHeadline(names, other.name),
                    caption = null,
                    shareText = sumShare(names, other.name, whenDate),
                    calendarTitle = sumHeadline(names, other.name),
                    id = "summe-" + subsetId(candidate.group, anchors) + "-" + other.id,
                )
            }
    }

    // C. Jahrhundert-Summen

    /** `Σ a_i = k · 36.525` ⇒ `t = (T_k + Σ c_i) / m`, floored; only the next multiple per subset. */
    fun centuryEvents(
        anchors: List<TogetherAnchor>,
        today: Long,
        limit: Int = Int.MAX_VALUE,
    ): List<TogetherEvent> {
        data class Candidate(val t: Long, val group: List<Int>, val k: Long)

        val candidates = mutableListOf<Candidate>()
        for (group in centurySubsets(anchors.size)) {
            val m = group.size.toLong()
            val sumC = group.sumOf { anchors[it].c }
            val currentSum = m * today - sumC
            // Smallest multiple whose crossing day has not yet passed: a
            // ceiling, not `currentSum / 36 525 + 1`, which dropped the row on
            // the one day it exists for whenever the target divided evenly.
            val k = maxOf(1L, ceilDiv(currentSum, DAYS_PER_CENTURY))
            val target = k * DAYS_PER_CENTURY
            val t = floorDiv(target + sumC, m)
            if (t < today) continue
            candidates += Candidate(t, group, k)
        }
        return candidates
            .sortedWith(
                compareBy<Candidate> { it.t }
                    .thenComparator { a, b -> compareLexicographically(a.group, b.group) }
                    .thenBy { it.k },
            )
            .take(limit)
            .map { candidate ->
                val years = (candidate.k * 100).toInt()
                val target = candidate.k * DAYS_PER_CENTURY
                val whenDate = date(candidate.t)
                TogetherEvent(
                    kind = TogetherKind.JAHRHUNDERT,
                    date = whenDate,
                    dayIndex = candidate.t,
                    participantIds = candidate.group.map { anchors[it].id },
                    headline = centuryHeadline(candidate.group, anchors, years),
                    // Target only, never the current sum, which on this day can
                    // still be up to `m - 1` short.
                    caption = "${formatNumber(target)} Erdentage",
                    shareText = centuryShare(candidate.group, anchors, years, whenDate),
                    calendarTitle = centuryCalendarTitle(candidate.group, anchors, years, target),
                    id = "jahrhundert-${candidate.k}-" + subsetId(candidate.group, anchors),
                )
            }
    }

    /** The members' UUIDs sorted, so the id names the set rather than the order. */
    fun subsetId(group: List<Int>, anchors: List<TogetherAnchor>): String =
        group.map { anchors[it].id }.sorted().joinToString("-")

    /** Every subset while the family is small enough; otherwise small subsets plus the full group. */
    fun centurySubsets(n: Int): List<List<Int>> {
        if (n < MIN_SUBSET_SIZE) return emptyList()
        val out = mutableListOf<List<Int>>()
        if (n <= FULL_ENUMERATION_PEOPLE_LIMIT) {
            for (size in MIN_SUBSET_SIZE..n) out += combinations(n, size)
            return out
        }
        for (size in MIN_SUBSET_SIZE..CAPPED_SUBSET_SIZE) out += combinations(n, size)
        out += (0 until n).toList()
        return out
    }

    /** Index combinations, ascending, with a remaining-count prune. */
    fun combinations(n: Int, choose: Int): List<List<Int>> {
        if (choose <= 0 || choose > n) return emptyList()
        val result = mutableListOf<List<Int>>()
        val current = ArrayDeque<Int>()
        fun walk(start: Int) {
            if (current.size == choose) {
                result += current.toList()
                return
            }
            var i = start
            while (i < n) {
                if (n - i < choose - current.size) break
                current.addLast(i)
                walk(i + 1)
                current.removeLast()
                i++
            }
        }
        walk(0)
        return result
    }

    private fun compareLexicographically(a: List<Int>, b: List<Int>): Int {
        for (index in 0 until minOf(a.size, b.size)) {
            val cmp = a[index].compareTo(b[index])
            if (cmp != 0) return cmp
        }
        return a.size.compareTo(b.size)
    }

    // German phrasing

    /** Older first, as anchor indices; ties keep the argument order. */
    private fun ordered(anchors: List<TogetherAnchor>, i: Int, j: Int): Pair<Int, Int> =
        if (anchors[i].c <= anchors[j].c) i to j else j to i

    /** The owner speaks in the first person, nominative in comparisons. */
    private fun selfOrName(name: String): String =
        if (PersonName.isOwner(name)) "ich" else PersonName.display(name)

    /** Dative, for „Alle außer **mir**“. */
    private fun selfOrNameDative(name: String): String =
        if (PersonName.isOwner(name)) "mir" else PersonName.display(name)

    private fun sentence(s: String): String =
        if (s.isEmpty()) s else s.substring(0, 1).uppercase(germanLocale) + s.substring(1)

    fun ownerLast(names: List<String>): List<String> = PersonName.ownerLast(names)

    private fun anchorsOwnerLast(a: TogetherAnchor, b: TogetherAnchor): List<TogetherAnchor> =
        if (PersonName.isOwner(a.name)) listOf(b, a) else listOf(a, b)

    /** „Anne & ich“: a label, first name capitalized, owner lowercase in second position. */
    private fun pairHeadline(pair: List<TogetherAnchor>): String =
        sentence(pair.joinToString(" & ") { selfOrName(it.name) })

    fun joinNames(names: List<String>): String {
        val mapped = ownerLast(names).map(::selfOrName)
        if (mapped.size <= 1) return mapped.firstOrNull().orEmpty()
        return mapped.dropLast(1).joinToString(", ") + " und " + mapped.last()
    }

    /** Names and no family framing: the identity holds for uncle and niece too. */
    fun doubleHeadline(older: String, younger: String): String {
        val subject = if (PersonName.isOwner(older)) "ich bin" else "${PersonName.display(older)} ist"
        return sentence("$subject doppelt so alt wie ${selfOrName(younger)}")
    }

    fun doubleCaption(older: String, younger: String): String =
        "so alt, wie ${selfOrName(older)} war, als ${selfOrName(younger)} geboren wurde"

    fun sumHeadline(group: List<String>, other: String): String =
        sentence("${joinNames(group)} sind zusammen so alt wie ${selfOrName(other)}")

    /**
     * The gap is also the older person's Erdentage on the day the younger
     * was born, but only while the younger person has no `daysOffEarth`;
     * where the second reading would be false the clause is dropped.
     */
    fun constellationDetail(
        older: String,
        younger: String,
        gap: Long,
        youngerDaysOffEarth: Int = 0,
    ): String {
        val unit = if (gap == 1L) "Tag" else "Tage"
        val distance = "${formatNumber(gap)} $unit auseinander"
        if (youngerDaysOffEarth != 0) return "$distance."
        return "$distance — so alt war ${selfOrName(older)}, als ${selfOrName(younger)} geboren wurde."
    }

    /** „zusammen“ is dropped: the page is called „Zusammen“ and the word wrapped every row. */
    fun centuryHeadline(group: List<Int>, anchors: List<TogetherAnchor>, years: Int): String =
        sentence("${centuryLabel(group, anchors)}: $years Jahre")

    private fun centuryLabel(group: List<Int>, anchors: List<TogetherAnchor>): String {
        if (group.size == anchors.size) return "Alle zusammen"
        if (group.size == anchors.size - 1) {
            val missing = anchors.indices.first { it !in group }
            return "Alle außer ${selfOrNameDative(anchors[missing].name)}"
        }
        return joinNames(group.map { anchors[it].name })
    }

    /** Inside a running sentence the full group becomes a bare „alle“. */
    private fun centurySubject(group: List<Int>, anchors: List<TogetherAnchor>): String {
        if (group.size == anchors.size) return "alle"
        if (group.size == anchors.size - 1) {
            val missing = anchors.indices.first { it !in group }
            return "alle außer ${selfOrNameDative(anchors[missing].name)}"
        }
        return joinNames(group.map { anchors[it].name })
    }

    // Share and calendar text

    private fun longDate(date: LocalDate): String = date.format(longDateFormatter)

    private fun formatNumber(value: Long): String =
        NumberFormat.getIntegerInstance(germanLocale).format(value)

    /** Verb-second inversion after the fronted date: „Am Dienstag … **bin ich** doppelt so alt“. */
    fun doubleShare(older: String, younger: String, date: LocalDate): String {
        val verb = if (PersonName.isOwner(older)) "bin" else "ist"
        return "Am ${longDate(date)} $verb ${selfOrName(older)} doppelt so alt wie ${selfOrName(younger)}. 🌍"
    }

    fun sumShare(group: List<String>, other: String, date: LocalDate): String =
        "Am ${longDate(date)} sind ${joinNames(group)} zusammen so alt wie ${selfOrName(other)}. 🌍"

    fun centuryShare(group: List<Int>, anchors: List<TogetherAnchor>, years: Int, date: LocalDate): String =
        "Am ${longDate(date)} sind ${centurySubject(group, anchors)} zusammen $years Jahre alt. 🌍"

    /** „Alle zusammen: 100 Jahre“ alone would be cryptic in a calendar, so the day count comes along. */
    fun centuryCalendarTitle(group: List<Int>, anchors: List<TogetherAnchor>, years: Int, target: Long): String =
        "${centuryHeadline(group, anchors, years)} (${formatNumber(target)} Erdentage)"
}
