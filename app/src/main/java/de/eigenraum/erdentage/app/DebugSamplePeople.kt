// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint

/**
 * Deterministic data used only by debuggable application builds. Release builds
 * still begin with an empty repository.
 */
internal val DebugSamplePeople = listOf(
    Person(
        id = "10000000-0000-4000-8000-000000000001",
        name = "Ich",
        birthYear = 1980,
        birthMonth = 9,
        birthDay = 1,
        tint = PersonTint.CORAL,
    ),
    Person(
        id = "10000000-0000-4000-8000-000000000002",
        name = "Lina",
        birthYear = 1992,
        birthMonth = 2,
        birthDay = 29,
        birthHour = 8,
        birthMinute = 35,
        hasTime = true,
        tint = PersonTint.SKY,
    ),
    Person(
        id = "10000000-0000-4000-8000-000000000003",
        name = "Egon-Steffen-Mike",
        birthYear = 2012,
        birthMonth = 10,
        birthDay = 28,
        daysOffEarth = 12,
        tint = PersonTint.MINT,
    ),
)

internal fun initialPeopleForBuild(debug: Boolean): List<Person> =
    if (debug) DebugSamplePeople else emptyList()
