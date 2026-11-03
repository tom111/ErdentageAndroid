// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

/**
 * What a stored `currentPersonIndex` refers to.
 *
 * The pager is `people.size` person pages, then „Demnächst“ at
 * `index == people.size`, then „Zusammen“ at `index == people.size + 1`, the
 * latter present only from two people onward. Interpret a stored index only
 * through [ErdentageLimits.pageKind]: with two trailing pages a bare
 * `index >= people.size` test can no longer tell them apart.
 */
sealed interface PageKind {
    data class Person(val index: Int) : PageKind
    data object Demnaechst : PageKind
    data object Zusammen : PageKind
}
