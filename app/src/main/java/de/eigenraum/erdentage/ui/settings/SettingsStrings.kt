// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.settings

import androidx.annotation.StringRes
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.PersonTint

@get:StringRes
internal val PersonTint.labelResource: Int
    get() = when (this) {
        PersonTint.SAND -> R.string.tint_sand
        PersonTint.CORAL -> R.string.tint_coral
        PersonTint.ROSE -> R.string.tint_rose
        PersonTint.LAVENDER -> R.string.tint_lavender
        PersonTint.SKY -> R.string.tint_sky
        PersonTint.MINT -> R.string.tint_mint
        PersonTint.FOREST -> R.string.tint_forest
        PersonTint.OCEAN -> R.string.tint_ocean
        PersonTint.SLATE -> R.string.tint_slate
        PersonTint.STONE -> R.string.tint_stone
        PersonTint.WHITE -> R.string.tint_white
    }
