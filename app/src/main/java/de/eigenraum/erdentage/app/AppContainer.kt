// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import de.eigenraum.erdentage.data.AppStateDataStore
import de.eigenraum.erdentage.data.DataStorePeopleRepository
import de.eigenraum.erdentage.data.PeopleRepository
import de.eigenraum.erdentage.data.ignoringNewerSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private companion object {
        const val TAG = "AppContainer"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val peopleRepository: PeopleRepository = DataStorePeopleRepository(
        AppStateDataStore.create(context.applicationContext, applicationScope),
    )

    init {
        applicationScope.launch {
            val isDebuggable = context.applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE != 0
            // A state file from a newer release must not crash the process
            // here, before any UI exists; MainActivity explains it instead.
            ignoringNewerSchema(onIgnored = { Log.w(TAG, it.message, it) }) {
                peopleRepository.initialize(
                    initialPeople = initialPeopleForBuild(isDebuggable),
                )
            }
        }
    }
}
