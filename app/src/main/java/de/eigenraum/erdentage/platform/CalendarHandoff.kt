// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

data class CalendarEventDraft(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
)

fun launchCalendarHandoff(
    context: Context,
    event: CalendarEventDraft,
): Boolean {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, event.title)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
