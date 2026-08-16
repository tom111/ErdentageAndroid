// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.app.OverviewEventUiState
import de.eigenraum.erdentage.ui.person.captionResource
import de.eigenraum.erdentage.ui.theme.LocalDarkTheme
import de.eigenraum.erdentage.ui.theme.inkSlateColor
import de.eigenraum.erdentage.ui.theme.surfaceColor
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OverviewPage(
    events: List<OverviewEventUiState>,
    modifier: Modifier = Modifier,
) {
    val darkTheme = LocalDarkTheme.current

    val sky = if (darkTheme) {
        listOf(
            androidx.compose.ui.graphics.Color(0xFF0A0F2E),
            androidx.compose.ui.graphics.Color(0xFF121738),
            androidx.compose.ui.graphics.Color(0xFF2E2138),
        )
    } else {
        listOf(
            androidx.compose.ui.graphics.Color(0xFFBDDFF7),
            androidx.compose.ui.graphics.Color(0xFFE0F0FF),
            androidx.compose.ui.graphics.Color(0xFFFCF7ED),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to sky[0],
                    0.55f to sky[1],
                    0.85f to sky[2],
                    1.0f to sky[2],
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 48.dp,
                end = 16.dp,
                bottom = 72.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.upcoming_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .semantics { heading() },
                    color = inkSlateColor(darkTheme),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            if (events.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_upcoming_milestones),
                        modifier = Modifier.fillMaxWidth(),
                        color = inkSlateColor(darkTheme),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                itemsIndexed(
                    items = events,
                    key = { _, event ->
                        "${event.person.id}-${event.milestone.target}"
                    },
                ) { index, event ->
                    OverviewEventRow(
                        event = event,
                        alignEnd = index % 2 == 1,
                        darkTheme = darkTheme,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewEventRow(
    event: OverviewEventUiState,
    alignEnd: Boolean,
    darkTheme: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(8.dp),
            color = event.person.resolvedTint(event.personIndex).surfaceColor(darkTheme),
            contentColor = inkSlateColor(darkTheme),
            shadowElevation = if (darkTheme) 4.dp else 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = event.person.displayName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                event.milestone.kinds.forEach { kind ->
                    Text(
                        text = stringResource(kind.captionResource),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.milestone_date,
                        NumberFormat.getIntegerInstance(Locale.GERMANY)
                            .format(event.milestone.target),
                        event.milestone.date.format(OverviewDateFormatter),
                    ),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private val OverviewDateFormatter = DateTimeFormatter
    .ofPattern("EEE d. MMM uuuu", Locale.GERMANY)
