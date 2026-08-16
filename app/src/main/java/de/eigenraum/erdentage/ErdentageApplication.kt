// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage

import android.app.Application
import android.content.Context
import de.eigenraum.erdentage.app.AppContainer
import de.eigenraum.erdentage.platform.withGermanLocale
import de.eigenraum.erdentage.widget.WidgetStateObserver

class ErdentageApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withGermanLocale())
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetStateObserver.start(this, container.peopleRepository)
    }
}
