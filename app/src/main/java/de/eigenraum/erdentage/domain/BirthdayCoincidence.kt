// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import java.time.LocalDate
import java.util.Locale

/**
 * One Geburtstags-Zufall, pre-phrased for the three shapes the app renders
 * it in. German phrasing is baked into the model so the view layer can never
 * re-derive it into a second wording.
 */
data class BirthdayNote(
    /** Row caption: „🎂 an Oskars 24. Geburtstag“. */
    val caption: String,
    /** Share sentence, standing on its own after the row's: „Es ist Oskars 24. Geburtstag. 🎂“. */
    val sentence: String,
    /** For the „Demnächst“ clouds, one more badge line: „🎂 Oskar“. */
    val short: String,
)

/**
 * A dated row landing on somebody's birthday. Strictly an annotation: it
 * never adds, removes or reorders a row. Ported from iOS
 * `BirthdayCoincidence` (tag `v1.4.2-build19`).
 *
 * Deliberately not done by the matching:
 * - `daysOffEarth` is ignored: the offset moves a count, not a birthday.
 * - the birth time is ignored: a birthday is a civil date.
 * - stored components match exactly, so 29 February is a birthday only in
 *   leap years.
 */
object BirthdayCoincidence {
    /**
     * The tracked people whose birthday falls on [date] and who want it
     * mentioned, owner last. The switch is filtered here rather than at the
     * call sites, so no surface can forget it.
     */
    fun matches(date: LocalDate, people: List<Person>): List<Person> {
        val hits = people.filter { person ->
            person.mentionsBirthday &&
                // A corrupted blob whose components do not form a real date
                // shows as „kein Geburtsdatum“ everywhere else, so it must
                // not claim a birthday here either.
                person.civilBirthDate() != null &&
                person.birthMonth == date.monthValue &&
                person.birthDay == date.dayOfMonth
        }
        return PersonName.ownerLast(hits, Person::name)
    }

    /** `null` when the day is nobody's birthday, the overwhelming majority of calls. */
    fun note(date: LocalDate, people: List<Person>): BirthdayNote? {
        val hits = matches(date, people)
        if (hits.isEmpty()) return null

        // One ordinal, or none: two people sharing the day drop both, because
        // „an Annes 40. und Oskars 24. Geburtstag“ stops being readable.
        val ordinal = if (hits.size == 1) age(hits[0], date.year) else null

        return BirthdayNote(
            caption = "🎂 an ${possessives(hits, ordinal, dative = true)} Geburtstag",
            sentence = "Es ist ${possessives(hits, ordinal, dative = false)} Geburtstag. 🎂",
            short = "🎂 " + hits.joinToString(", ") { it.displayName },
        )
    }

    private fun possessives(people: List<Person>, ordinal: Int?, dative: Boolean): String {
        val parts = people.map { possessive(it.name, dative) }
        val joined = if (parts.size > 1) {
            parts.dropLast(1).joinToString(", ") + " und " + parts.last()
        } else {
            parts.firstOrNull().orEmpty()
        }
        return if (ordinal == null) joined else "$joined $ordinal."
    }

    /**
     * The owner's possessive is the one word here that declines: „an
     * **meinem** Geburtstag“ but „Es ist **mein** Geburtstag“. A name already
     * ending in an s-sound takes a bare apostrophe („Lars’ Geburtstag“).
     */
    private fun possessive(raw: String, dative: Boolean): String {
        if (PersonName.isOwner(raw)) return if (dative) "meinem" else "mein"
        val name = PersonName.display(raw)
        val lowered = name.lowercase(Locale.GERMANY)
        val sibilant = listOf("s", "ß", "x", "z").any(lowered::endsWith)
        return if (sibilant) "$name’" else "${name}s"
    }

    /**
     * Which birthday it is, or `null` when the date is not after the birth
     * year: the birth day itself is no „0. Geburtstag“, and a future birth
     * date would otherwise produce a negative one.
     */
    private fun age(person: Person, year: Int): Int? =
        (year - person.birthYear).takeIf { it > 0 }
}
