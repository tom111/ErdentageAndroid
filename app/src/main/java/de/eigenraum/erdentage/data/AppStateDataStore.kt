// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope

object AppStateDataStore {
    private const val FILE_NAME = "erdentage-state.json"

    fun create(context: Context, scope: CoroutineScope): DataStore<PersistedAppState> =
        DataStoreFactory.create(
            serializer = AppStateSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler {
                AppStateSerializer.defaultValue
            },
            scope = scope,
            produceFile = { context.dataStoreFile(FILE_NAME) },
        )
}
