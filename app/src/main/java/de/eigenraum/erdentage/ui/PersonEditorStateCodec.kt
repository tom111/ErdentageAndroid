// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui

import androidx.compose.runtime.saveable.Saver
import de.eigenraum.erdentage.data.ErdentageJson
import de.eigenraum.erdentage.domain.Person
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal object PersonEditorStateCodec {
    val saver = Saver<Person, String>(
        save = { person -> encode(person) },
        restore = ::decodeOrNull,
    )

    fun encode(person: Person): String = ErdentageJson.encodeToString(
        person.copy(addedMilestones = emptyList()),
    )

    fun decodeOrNull(encoded: String?): Person? = encoded?.let { value ->
        runCatching {
            ErdentageJson.decodeFromString<Person>(value)
        }.getOrNull()
    }
}
