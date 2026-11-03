// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErdentageLimitsTest {
    @Test
    fun pageKindMapsTheTwoTrailingSentinelsDistinctly() {
        assertEquals(PageKind.Person(0), ErdentageLimits.pageKind(0, 3))
        assertEquals(PageKind.Person(2), ErdentageLimits.pageKind(2, 3))
        assertEquals(PageKind.Demnaechst, ErdentageLimits.pageKind(3, 3))
        assertEquals(PageKind.Zusammen, ErdentageLimits.pageKind(4, 3))
        // Out of range falls back to the last person, never crashes.
        assertEquals(PageKind.Person(2), ErdentageLimits.pageKind(9, 3))
        assertEquals(PageKind.Person(0), ErdentageLimits.pageKind(-1, 3))
        // A single person has no Zusammen page.
        assertEquals(PageKind.Demnaechst, ErdentageLimits.pageKind(1, 1))
        assertEquals(PageKind.Person(0), ErdentageLimits.pageKind(2, 1))
        assertEquals(PageKind.Person(0), ErdentageLimits.pageKind(0, 0))
    }

    @Test
    fun lastPageIndexGrowsByTheTogetherPageFromTwoPeopleOnward() {
        assertEquals(0, ErdentageLimits.lastPageIndex(0))
        assertEquals(1, ErdentageLimits.lastPageIndex(1))
        assertEquals(3, ErdentageLimits.lastPageIndex(2))
        assertEquals(21, ErdentageLimits.lastPageIndex(ErdentageLimits.MAX_PEOPLE))
    }

    @Test
    fun normalizingPreservesBothTrailingPages() {
        assertEquals(3, ErdentageLimits.normalizeCurrentPersonIndex(3, 3))
        assertEquals(4, ErdentageLimits.normalizeCurrentPersonIndex(4, 3))
        assertEquals(2, ErdentageLimits.normalizeCurrentPersonIndex(5, 3))
        // Zusammen does not exist for one person, so its index is invalid.
        assertEquals(1, ErdentageLimits.normalizeCurrentPersonIndex(1, 1))
        assertEquals(0, ErdentageLimits.normalizeCurrentPersonIndex(2, 1))
        assertEquals(0, ErdentageLimits.normalizeCurrentPersonIndex(-3, 3))
        assertEquals(0, ErdentageLimits.normalizeCurrentPersonIndex(5, 0))
    }

    @Test
    fun restoredTrailingIndexDegradesZusammenToDemnaechstBelowTwoPeople() {
        assertNull(ErdentageLimits.restoredTrailingIndex(PageKind.Person(1), 3))
        assertEquals(3, ErdentageLimits.restoredTrailingIndex(PageKind.Demnaechst, 3))
        assertEquals(4, ErdentageLimits.restoredTrailingIndex(PageKind.Zusammen, 3))
        assertEquals(1, ErdentageLimits.restoredTrailingIndex(PageKind.Zusammen, 1))
        assertEquals(1, ErdentageLimits.restoredTrailingIndex(PageKind.Demnaechst, 1))
        assertEquals(0, ErdentageLimits.restoredTrailingIndex(PageKind.Zusammen, 0))
    }
}
