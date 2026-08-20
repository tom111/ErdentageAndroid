// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.overview

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewCloudMathTest {
    @Test
    fun seededJitterIsStableAndBounded() {
        val first = OverviewCloudMath.jitter("person-target-x", 18.dp)
        val repeated = OverviewCloudMath.jitter("person-target-x", 18.dp)
        val other = OverviewCloudMath.jitter("other-target-x", 18.dp)

        assertEquals(first, repeated)
        assertNotEquals(first, other)
        assertTrue(first.value in -18f..18f)
        assertTrue(other.value in -18f..18f)
    }

    @Test
    fun seededJitterUsesTheAvailableRange() {
        val factors = (0 until 256).map { index ->
            OverviewCloudMath.jitter("cloud-$index-x", 1.dp).value
        }

        assertTrue(factors.min() < -0.75f)
        assertTrue(factors.max() > 0.75f)
        assertTrue(factors.distinct().size > 200)
    }
}
