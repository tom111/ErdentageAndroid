// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.DayCountSnapshot
import de.eigenraum.erdentage.domain.MilestoneCandidate
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.app.PersonPageUiState
import de.eigenraum.erdentage.ui.theme.PersonTintBackground
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PersonPage(
    state: PersonPageUiState,
    modifier: Modifier = Modifier,
) {
    val person = state.person
    val tint = person.resolvedTint(state.personIndex)

    PersonTintBackground(tint = tint, modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(bottom = 44.dp),
        ) {
            val compactHeight = maxHeight < 500.dp
            if (compactHeight) {
                CompactPersonContent(
                    person = person,
                    snapshot = state.snapshot,
                    todayKinds = state.todayKinds,
                )
            } else {
                PortraitPersonContent(
                    person = person,
                    snapshot = state.snapshot,
                    todayKinds = state.todayKinds,
                )
            }
        }
    }
}

@Composable
private fun PortraitPersonContent(
    person: Person,
    snapshot: DayCountSnapshot,
    todayKinds: List<de.eigenraum.erdentage.domain.MilestoneKind>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))
        PersonHeadline(person)
        Spacer(Modifier.height(36.dp))
        CountCluster(snapshot, todayKinds, compact = false)
        Spacer(Modifier.height(24.dp))
        MilestoneList(
            milestones = snapshot.milestones,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun CompactPersonContent(
    person: Person,
    snapshot: DayCountSnapshot,
    todayKinds: List<de.eigenraum.erdentage.domain.MilestoneKind>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PersonHeadline(person)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CountCluster(snapshot, todayKinds, compact = true)
            }
            MilestoneList(
                milestones = snapshot.milestones,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun PersonHeadline(person: Person) {
    val title = if (person.daysOffEarth > 0) {
        stringResource(R.string.erdentage_adjusted)
    } else {
        stringResource(R.string.app_name)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Text(
            text = person.displayName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun CountCluster(
    snapshot: DayCountSnapshot,
    todayKinds: List<de.eigenraum.erdentage.domain.MilestoneKind>,
    compact: Boolean,
) {
    val resources = LocalResources.current
    val count = NumberFormat.getIntegerInstance(Locale.GERMANY).format(snapshot.days)
    val todayLabels = todayKinds.joinToString(" · ") {
        resources.getString(it.todayLabelResource)
    }
    val accessibility = buildList {
        add(stringResource(R.string.day_count_accessibility, count))
        if (todayLabels.isNotEmpty()) {
            add(stringResource(R.string.today_milestones_accessibility, todayLabels))
        }
        if (snapshot.showTime) {
            add(
                stringResource(
                    R.string.residual_time_accessibility,
                    snapshot.hours,
                    snapshot.minutes,
                ),
            )
        }
    }.joinToString(", ")
    val countSize = when {
        compact -> 54.sp
        count.length > 9 -> 48.sp
        count.length > 7 -> 58.sp
        else -> 72.sp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = accessibility
        },
    ) {
        Text(
            text = count,
            modifier = Modifier.fillMaxWidth(),
            fontSize = countSize,
            fontWeight = FontWeight.Bold,
            lineHeight = countSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (todayLabels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.today_milestones, todayLabels),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (snapshot.showTime) {
            Text(
                text = stringResource(
                    R.string.residual_time,
                    snapshot.hours,
                    snapshot.minutes,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun MilestoneList(
    milestones: List<MilestoneCandidate>,
    modifier: Modifier = Modifier,
) {
    if (milestones.isEmpty()) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.no_milestones))
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(milestones, key = MilestoneCandidate::target) { milestone ->
            MilestoneRow(milestone)
        }
    }
}

@Composable
private fun MilestoneRow(milestone: MilestoneCandidate) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        milestone.kinds.forEach { kind ->
            Text(
                text = stringResource(kind.captionResource),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(
                R.string.milestone_date,
                NumberFormat.getIntegerInstance(Locale.GERMANY).format(milestone.target),
                milestone.date.format(GermanDateFormatter),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private val GermanDateFormatter = DateTimeFormatter
    .ofPattern("EEE d. MMM uuuu", Locale.GERMANY)
