// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MilestoneKind {
    @SerialName("tausender")
    TAUSENDER,

    @SerialName("zehntausender")
    ZEHNTAUSENDER,

    @SerialName("primzahl")
    PRIMZAHL,

    @SerialName("palindrom")
    PALINDROM,

    @SerialName("quadrat")
    QUADRAT,

    @SerialName("zweierpotenz")
    ZWEIERPOTENZ,

    @SerialName("ziffernfolge")
    ZIFFERNFOLGE,

    @SerialName("catalan")
    CATALAN,

    @SerialName("fibonacci")
    FIBONACCI,

    @SerialName("zwillingsprimzahl")
    ZWILLINGSPRIMZAHL,
    ;

    companion object {
        val defaultEnabled: Set<MilestoneKind> = entries
            .filterNot { it == ZWILLINGSPRIMZAHL }
            .toSet()
    }
}
