// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.eigenraum.erdentage.ErdentageApplication
import de.eigenraum.erdentage.data.PeopleRepository
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.data.ignoringNewerSchema
import de.eigenraum.erdentage.domain.DayCountLogic
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "ErdentageWidget"

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as ErdentageApplication
        val state = ignoringNewerSchema(onIgnored = { Log.w(TAG, it.message, it) }) {
            application.container.peopleRepository.state.first()
        } ?: return Result.success()
        ErdentageGlanceWidget().updateAll(applicationContext)
        WidgetUpdateScheduler.scheduleNext(applicationContext, state)
        return Result.success()
    }
}

object WidgetUpdateScheduler {
    private const val UNIQUE_WORK = "erdentage-widget-rollover"

    fun enqueueImmediate(context: Context) {
        if (!hasWidgets(context)) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build(),
        )
    }

    fun scheduleNext(
        context: Context,
        state: PersistedAppState,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        if (!hasWidgets(context)) {
            cancel(context)
            return
        }
        val next = state.people.minOfOrNull { person ->
            DayCountLogic.nextRolloverAfter(now, zoneId, person)
        } ?: DayCountLogic.nextRolloverAfter(now, zoneId, null)
        val delayMillis = Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    fun hasWidgets(context: Context): Boolean = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, ErdentageWidgetReceiver::class.java))
        .isNotEmpty()
}

object WidgetStateObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context, repository: PeopleRepository) {
        val applicationContext = context.applicationContext
        scope.launch {
            // Never let a newer-schema file kill the process from here.
            repository.state
                .ignoringNewerSchema(onIgnored = { Log.w(TAG, it.message, it) })
                .collectLatest { state ->
                if (WidgetUpdateScheduler.hasWidgets(applicationContext)) {
                    ErdentageGlanceWidget().updateAll(applicationContext)
                    WidgetUpdateScheduler.scheduleNext(applicationContext, state)
                }
            }
        }
    }
}

class WidgetTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in SystemTimeActions) return
        if (!WidgetUpdateScheduler.hasWidgets(context)) return
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val application = applicationContext as ErdentageApplication
                val state = ignoringNewerSchema(onIgnored = { Log.w(TAG, it.message, it) }) {
                    application.container.peopleRepository.state.first()
                }
                if (state != null) {
                    ErdentageGlanceWidget().updateAll(applicationContext)
                    WidgetUpdateScheduler.scheduleNext(applicationContext, state)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val SystemTimeActions = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
        )
    }
}
