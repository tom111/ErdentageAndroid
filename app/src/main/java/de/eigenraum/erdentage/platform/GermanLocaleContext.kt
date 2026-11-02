// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.platform

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.withGermanLocale(): Context {
    val locale = Locale.forLanguageTag("de-DE")
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}
