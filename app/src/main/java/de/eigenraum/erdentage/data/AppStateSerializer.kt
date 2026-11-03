// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The stored file was written by a newer release of the app. It is not
 * corrupt, so it must not be replaced: an upgrade will read it again. The
 * repository does not retry on this, and the UI explains it instead of
 * showing an empty family.
 */
class NewerSchemaException(val storedSchemaVersion: Int) : IOException(
    "App state schema $storedSchemaVersion is newer than the supported " +
        "schema ${PersistedAppState.CURRENT_SCHEMA_VERSION}",
)

object AppStateSerializer : Serializer<PersistedAppState> {
    override val defaultValue: PersistedAppState = PersistedAppState()

    override suspend fun readFrom(input: InputStream): PersistedAppState {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        try {
            // Check the version before decoding the rest: a newer release may
            // have changed a field's shape, and that must read as "newer", not
            // as corruption.
            val element = ErdentageJson.parseToJsonElement(text)
            val schemaVersion = element.jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull
                ?: throw CorruptionException("App state has no schema version")
            if (schemaVersion > PersistedAppState.CURRENT_SCHEMA_VERSION) {
                throw NewerSchemaException(schemaVersion)
            }
            if (schemaVersion < OLDEST_SUPPORTED_SCHEMA_VERSION) {
                throw CorruptionException("Unsupported app-state schema $schemaVersion")
            }
            // Older schemas are additive: every later field has a default, and
            // `normalized()` stamps the current version on the next write.
            return ErdentageJson.decodeFromJsonElement<PersistedAppState>(element)
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

    private const val OLDEST_SUPPORTED_SCHEMA_VERSION = 1
}
