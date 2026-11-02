// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.widget

import android.content.Context
import androidx.core.content.edit
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.DayCountLogic
import de.eigenraum.erdentage.domain.DayCountSnapshot
import java.time.Instant
import java.time.ZoneId

data class WidgetSelection(val personId: String?) {
    val followsCurrentPerson: Boolean
        get() = personId == null

    companion object {
        val FollowCurrent = WidgetSelection(personId = null)
        fun fixed(personId: String): WidgetSelection = WidgetSelection(personId)
    }
}

class WidgetSelectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(appWidgetId: Int): WidgetSelection {
        val value = preferences.getString(key(appWidgetId), FOLLOW_VALUE) ?: FOLLOW_VALUE
        return if (value == FOLLOW_VALUE) WidgetSelection.FollowCurrent else WidgetSelection.fixed(value)
    }

    fun write(appWidgetId: Int, selection: WidgetSelection) {
        preferences.edit {
            putString(key(appWidgetId), selection.personId ?: FOLLOW_VALUE)
        }
    }

    fun delete(appWidgetIds: IntArray) {
        preferences.edit {
            appWidgetIds.forEach { remove(key(it)) }
        }
    }

    private fun key(appWidgetId: Int): String = "selection-$appWidgetId"

    companion object {
        const val PREFERENCES_NAME = "erdentage-widget-configuration"
        private const val FOLLOW_VALUE = "__follow_current__"
    }
}

object WidgetSnapshotFactory {
    fun create(
        state: PersistedAppState,
        selection: WidgetSelection,
        at: Instant,
        zoneId: ZoneId,
    ): DayCountSnapshot {
        val resolved = DayCountLogic.resolve(
            personId = selection.personId,
            people = state.people,
            currentPersonIndex = state.currentPersonIndex,
        )
        return DayCountLogic.snapshotAt(
            at = at,
            zoneId = zoneId,
            person = resolved?.first,
            index = resolved?.second ?: 0,
        )
    }
}
