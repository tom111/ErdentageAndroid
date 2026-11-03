// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import java.io.File
import kotlinx.coroutines.CoroutineScope

object AppStateDataStore {
    private const val FILE_NAME = "erdentage-state.json"

    /** Where an unreadable state file is kept before it is replaced. */
    const val CORRUPT_COPY_NAME = "erdentage-state.corrupt.json"

    fun create(context: Context, scope: CoroutineScope): DataStore<PersistedAppState> {
        val file = context.dataStoreFile(FILE_NAME)
        return DataStoreFactory.create(
            serializer = AppStateSerializer,
            corruptionHandler = corruptionHandler(file),
            scope = scope,
            produceFile = { file },
        )
    }

    /**
     * Replaces an unreadable file with the empty state, but keeps a copy of
     * the bytes next to it first. The app never deletes data it could not
     * read without leaving that copy behind.
     */
    fun corruptionHandler(file: File): ReplaceFileCorruptionHandler<PersistedAppState> =
        ReplaceFileCorruptionHandler {
            preserveCorruptCopy(file)
            AppStateSerializer.defaultValue
        }

    private fun preserveCorruptCopy(file: File) {
        runCatching {
            if (file.isFile) {
                file.copyTo(File(file.parentFile, CORRUPT_COPY_NAME), overwrite = true)
            }
        }
    }
}
