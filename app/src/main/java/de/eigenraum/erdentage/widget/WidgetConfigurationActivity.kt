// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import de.eigenraum.erdentage.ErdentageApplication
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.platform.withGermanLocale
import de.eigenraum.erdentage.ui.theme.ErdentageTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class WidgetConfigurationActivity : ComponentActivity() {
    private val appWidgetId: Int
        get() = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withGermanLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        val application = application as ErdentageApplication
        setContent {
            ErdentageTheme {
                val state by application.container.peopleRepository.state
                    .collectAsStateWithLifecycle(initialValue = PersistedAppState())
                WidgetConfigurationScreen(
                    state = state,
                    currentSelection = WidgetSelectionStore(this)
                        .read(appWidgetId),
                    onCancel = ::finish,
                    onSelect = ::completeConfiguration,
                )
            }
        }
    }

    private fun completeConfiguration(selection: WidgetSelection) {
        WidgetSelectionStore(this).write(appWidgetId, selection)
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity)
                .getGlanceIdBy(appWidgetId)
            ErdentageGlanceWidget().update(this@WidgetConfigurationActivity, glanceId)
            val state = (application as ErdentageApplication)
                .container.peopleRepository.state
                .first()
            WidgetUpdateScheduler.scheduleNext(this@WidgetConfigurationActivity, state)
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigurationScreen(
    state: PersistedAppState,
    currentSelection: WidgetSelection,
    onCancel: () -> Unit,
    onSelect: (WidgetSelection) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_configuration_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                WidgetSelectionRow(
                    title = stringResource(R.string.widget_follow_current),
                    explanation = stringResource(R.string.widget_follow_current_explanation),
                    selected = currentSelection.followsCurrentPerson,
                    onClick = { onSelect(WidgetSelection.FollowCurrent) },
                )
                HorizontalDivider()
            }
            items(state.people, key = Person::id) { person ->
                WidgetSelectionRow(
                    title = person.displayName,
                    explanation = stringResource(
                        R.string.widget_fixed_person_explanation,
                        person.displayName,
                    ),
                    selected = currentSelection.personId == person.id,
                    onClick = { onSelect(WidgetSelection.fixed(person.id)) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WidgetSelectionRow(
    title: String,
    explanation: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(explanation) },
        trailingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
