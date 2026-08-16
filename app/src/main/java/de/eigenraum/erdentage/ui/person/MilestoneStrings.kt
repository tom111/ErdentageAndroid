// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.person

import androidx.annotation.StringRes
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.MilestoneKind

@get:StringRes
internal val MilestoneKind.captionResource: Int
    get() = when (this) {
        MilestoneKind.TAUSENDER -> R.string.caption_next_thousand
        MilestoneKind.ZEHNTAUSENDER -> R.string.caption_next_ten_thousand
        MilestoneKind.PRIMZAHL -> R.string.caption_next_prime
        MilestoneKind.ZWILLINGSPRIMZAHL -> R.string.caption_next_twin_prime
        MilestoneKind.PALINDROM -> R.string.caption_next_palindrome
        MilestoneKind.QUADRAT -> R.string.caption_next_square
        MilestoneKind.ZWEIERPOTENZ -> R.string.caption_next_power_two
        MilestoneKind.ZIFFERNFOLGE -> R.string.caption_next_digit_sequence
        MilestoneKind.CATALAN -> R.string.caption_next_catalan
        MilestoneKind.FIBONACCI -> R.string.caption_next_fibonacci
    }

@get:StringRes
internal val MilestoneKind.todayLabelResource: Int
    get() = when (this) {
        MilestoneKind.TAUSENDER -> R.string.milestone_thousand
        MilestoneKind.ZEHNTAUSENDER -> R.string.milestone_ten_thousand
        MilestoneKind.PRIMZAHL -> R.string.milestone_prime
        MilestoneKind.ZWILLINGSPRIMZAHL -> R.string.milestone_twin_prime
        MilestoneKind.PALINDROM -> R.string.milestone_palindrome
        MilestoneKind.QUADRAT -> R.string.milestone_square
        MilestoneKind.ZWEIERPOTENZ -> R.string.milestone_power_two
        MilestoneKind.ZIFFERNFOLGE -> R.string.milestone_digit_sequence
        MilestoneKind.CATALAN -> R.string.milestone_catalan
        MilestoneKind.FIBONACCI -> R.string.milestone_fibonacci
    }

@get:StringRes
internal val MilestoneKind.labelResource: Int
    get() = when (this) {
        MilestoneKind.TAUSENDER -> R.string.milestone_label_thousand
        MilestoneKind.ZEHNTAUSENDER -> R.string.milestone_label_ten_thousand
        MilestoneKind.PRIMZAHL -> R.string.milestone_label_prime
        MilestoneKind.ZWILLINGSPRIMZAHL -> R.string.milestone_label_twin_prime
        MilestoneKind.PALINDROM -> R.string.milestone_label_palindrome
        MilestoneKind.QUADRAT -> R.string.milestone_label_square
        MilestoneKind.ZWEIERPOTENZ -> R.string.milestone_label_power_two
        MilestoneKind.ZIFFERNFOLGE -> R.string.milestone_label_digit_sequence
        MilestoneKind.CATALAN -> R.string.milestone_label_catalan
        MilestoneKind.FIBONACCI -> R.string.milestone_label_fibonacci
    }

@get:StringRes
internal val MilestoneKind.explanationResource: Int
    get() = when (this) {
        MilestoneKind.TAUSENDER -> R.string.milestone_explanation_thousand
        MilestoneKind.ZEHNTAUSENDER -> R.string.milestone_explanation_ten_thousand
        MilestoneKind.PRIMZAHL -> R.string.milestone_explanation_prime
        MilestoneKind.ZWILLINGSPRIMZAHL -> R.string.milestone_explanation_twin_prime
        MilestoneKind.PALINDROM -> R.string.milestone_explanation_palindrome
        MilestoneKind.QUADRAT -> R.string.milestone_explanation_square
        MilestoneKind.ZWEIERPOTENZ -> R.string.milestone_explanation_power_two
        MilestoneKind.ZIFFERNFOLGE -> R.string.milestone_explanation_digit_sequence
        MilestoneKind.CATALAN -> R.string.milestone_explanation_catalan
        MilestoneKind.FIBONACCI -> R.string.milestone_explanation_fibonacci
    }
