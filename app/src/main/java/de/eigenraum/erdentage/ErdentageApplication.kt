// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import de.eigenraum.erdentage.app.AppContainer
import java.util.Locale

class ErdentageApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        val locale = Locale.forLanguageTag("de-DE")
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
