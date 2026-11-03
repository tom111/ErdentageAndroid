// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.together

import androidx.annotation.StringRes
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.TogetherKind

@get:StringRes
internal val TogetherKind.labelResource: Int
    get() = when (this) {
        TogetherKind.DOPPELT -> R.string.together_kind_double
        TogetherKind.ZUSAMMEN_SO_ALT -> R.string.together_kind_sum
        TogetherKind.JAHRHUNDERT -> R.string.together_kind_century
    }

/** One line under the section heading: what the category means. */
@get:StringRes
internal val TogetherKind.pageHintResource: Int
    get() = when (this) {
        TogetherKind.DOPPELT -> R.string.together_hint_double
        TogetherKind.ZUSAMMEN_SO_ALT -> R.string.together_hint_sum
        TogetherKind.JAHRHUNDERT -> R.string.together_hint_century
    }

/** The longer explanation, for the budget dialog. */
@get:StringRes
internal val TogetherKind.explanationResource: Int
    get() = when (this) {
        TogetherKind.DOPPELT -> R.string.together_explanation_double
        TogetherKind.ZUSAMMEN_SO_ALT -> R.string.together_explanation_sum
        TogetherKind.JAHRHUNDERT -> R.string.together_explanation_century
    }

/**
 * Whether a row of this kind shows its own caption: only where it carries
 * data that differs per row (the century target). Elsewhere the page hint
 * says it once.
 */
internal val TogetherKind.showsRowCaption: Boolean
    get() = this == TogetherKind.JAHRHUNDERT
