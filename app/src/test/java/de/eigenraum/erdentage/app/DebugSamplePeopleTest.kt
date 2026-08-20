// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.domain.PersonName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugSamplePeopleTest {
    @Test
    fun debugBuildGetsExactlyThreeRepresentativePeople() {
        val people = initialPeopleForBuild(debug = true)

        assertEquals(3, people.size)
        assertEquals(3, people.map { it.id }.distinct().size)
        assertEquals(3, people.map { it.tint }.distinct().size)
        assertTrue(people.any { PersonName.isOwner(it.name) })
        assertTrue(people.any { it.hasTime })
        assertTrue(people.any { it.name.length > 16 })
    }

    @Test
    fun releaseBuildGetsNoSamplePeople() {
        assertTrue(initialPeopleForBuild(debug = false).isEmpty())
    }
}
