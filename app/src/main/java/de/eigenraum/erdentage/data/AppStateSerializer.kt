// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object AppStateSerializer : Serializer<PersistedAppState> {
    override val defaultValue: PersistedAppState = PersistedAppState()

    override suspend fun readFrom(input: InputStream): PersistedAppState {
        try {
            val state = ErdentageJson.decodeFromString<PersistedAppState>(
                input.bufferedReader(Charsets.UTF_8).use { it.readText() },
            )
            if (state.schemaVersion != PersistedAppState.CURRENT_SCHEMA_VERSION) {
                throw CorruptionException("Unsupported app-state schema ${state.schemaVersion}")
            }
            return state
        } catch (error: CorruptionException) {
            throw error
        } catch (error: SerializationException) {
            throw CorruptionException("Cannot decode app state", error)
        } catch (error: IllegalArgumentException) {
            throw CorruptionException("App state violates its data contract", error)
        }
    }

    override suspend fun writeTo(t: PersistedAppState, output: OutputStream) {
        val encoded = ErdentageJson.encodeToString(t.normalized())
        output.write(encoded.toByteArray(Charsets.UTF_8))
    }
}
