// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.preview

import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint

internal val PreviewPeople = listOf(
    Person(
        id = "11111111-1111-4111-8111-111111111111",
        name = "Anne",
        birthYear = 1980,
        birthMonth = 7,
        birthDay = 12,
        tint = PersonTint.SKY,
    ),
    Person(
        id = "22222222-2222-4222-8222-222222222222",
        name = "Bernd",
        birthYear = 1992,
        birthMonth = 3,
        birthDay = 4,
        birthHour = 8,
        birthMinute = 35,
        hasTime = true,
        tint = PersonTint.CORAL,
    ),
    Person(
        id = "33333333-3333-4333-8333-333333333333",
        name = "Caro",
        birthYear = 2012,
        birthMonth = 10,
        birthDay = 28,
        tint = PersonTint.MINT,
    ),
)
