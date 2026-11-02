// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Rgb(val red: Double, val green: Double, val blue: Double)

@Serializable
enum class PersonTint(
    val lightSurface: Rgb,
    val darkSurface: Rgb,
    val darkLantern: Rgb,
) {
    @SerialName("sand")
    SAND(Rgb(0.953, 0.894, 0.714), Rgb(0.42, 0.30, 0.13), Rgb(0.78, 0.63, 0.32)),

    @SerialName("coral")
    CORAL(Rgb(0.949, 0.710, 0.627), Rgb(0.46, 0.20, 0.16), Rgb(0.82, 0.45, 0.38)),

    @SerialName("rose")
    ROSE(Rgb(0.941, 0.761, 0.784), Rgb(0.42, 0.18, 0.24), Rgb(0.78, 0.42, 0.50)),

    @SerialName("lavender")
    LAVENDER(Rgb(0.788, 0.753, 0.910), Rgb(0.26, 0.22, 0.42), Rgb(0.55, 0.48, 0.80)),

    @SerialName("sky")
    SKY(Rgb(0.710, 0.847, 0.941), Rgb(0.14, 0.26, 0.42), Rgb(0.34, 0.58, 0.84)),

    @SerialName("mint")
    MINT(Rgb(0.710, 0.898, 0.761), Rgb(0.14, 0.34, 0.22), Rgb(0.32, 0.72, 0.50)),

    @SerialName("forest")
    FOREST(Rgb(0.718, 0.831, 0.624), Rgb(0.20, 0.32, 0.13), Rgb(0.42, 0.66, 0.34)),

    @SerialName("ocean")
    OCEAN(Rgb(0.620, 0.804, 0.851), Rgb(0.12, 0.28, 0.34), Rgb(0.28, 0.60, 0.70)),

    @SerialName("slate")
    SLATE(Rgb(0.737, 0.769, 0.800), Rgb(0.24, 0.26, 0.32), Rgb(0.50, 0.54, 0.62)),

    @SerialName("stone")
    STONE(Rgb(0.847, 0.812, 0.757), Rgb(0.30, 0.25, 0.20), Rgb(0.62, 0.55, 0.46)),

    @SerialName("white")
    WHITE(Rgb(1.0, 1.0, 1.0), Rgb(0.13, 0.13, 0.16), Rgb(0.18, 0.13, 0.22)),
    ;

    companion object {
        val automaticEntries: List<PersonTint> = entries.filterNot { it == WHITE }

        fun defaultedAt(index: Int): PersonTint =
            automaticEntries[Math.floorMod(index, automaticEntries.size)]

        fun firstUnused(people: List<Person>): PersonTint {
            val used = people.mapNotNull(Person::tint).toSet()
            return automaticEntries.firstOrNull { it !in used } ?: defaultedAt(people.size)
        }
    }
}
