// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.app

import android.content.Context
import de.eigenraum.erdentage.data.AppStateDataStore
import de.eigenraum.erdentage.data.DataStorePeopleRepository
import de.eigenraum.erdentage.data.PeopleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val peopleRepository: PeopleRepository = DataStorePeopleRepository(
        AppStateDataStore.create(context.applicationContext, applicationScope),
    )

    init {
        applicationScope.launch {
            peopleRepository.initialize()
        }
    }
}
