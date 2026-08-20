// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

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
