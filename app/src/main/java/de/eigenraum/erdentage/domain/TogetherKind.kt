// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The three Wechselwirkungs-Kategorien of the „Zusammen“ page. Serial names
 * match the iOS `TogetherKind` raw values so the family-level row budgets are
 * spelled the same way in both apps' stored data.
 *
 * [defaultLimit] is how many rows of this kind reach the page when the user
 * has not chosen a budget; zero switches the category off (the count is the
 * switch, there is no separate toggle). [rarityRank] orders the page's
 * sections, rarest kind first; rows inside a section stay chronological.
 */
@Serializable
enum class TogetherKind(val defaultLimit: Int, val rarityRank: Int) {
    @SerialName("doppelt")
    DOPPELT(defaultLimit = 4, rarityRank = 0),

    @SerialName("zusammenSoAlt")
    ZUSAMMEN_SO_ALT(defaultLimit = 4, rarityRank = 1),

    @SerialName("jahrhundert")
    JAHRHUNDERT(defaultLimit = 3, rarityRank = 2),
    ;

    companion object {
        /** Zero means off. */
        val LIMIT_RANGE: IntRange = 0..10

        fun clampLimit(value: Int): Int = value.coerceIn(LIMIT_RANGE)
    }
}
