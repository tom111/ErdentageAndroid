// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.DayCountSnapshot
import de.eigenraum.erdentage.domain.DayCountLogic
import de.eigenraum.erdentage.domain.MilestoneCandidate
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonName
import de.eigenraum.erdentage.app.PersonPageUiState
import de.eigenraum.erdentage.platform.CalendarEventDraft
import de.eigenraum.erdentage.ui.theme.PersonTintBackground
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.util.Locale

@Composable
fun PersonPage(
    state: PersonPageUiState,
    onShare: (String) -> Unit,
    onCalendarHandoff: (MilestoneCalendarAction) -> Unit,
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
            val contentModifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = if (compactHeight) 1_080.dp else 840.dp)
            if (compactHeight) {
                CompactPersonContent(
                    person = person,
                    snapshot = state.snapshot,
                    todayKinds = state.todayKinds,
                    onShare = onShare,
                    onCalendarHandoff = onCalendarHandoff,
                    modifier = contentModifier,
                )
            } else {
                PortraitPersonContent(
                    person = person,
                    snapshot = state.snapshot,
                    todayKinds = state.todayKinds,
                    onShare = onShare,
                    onCalendarHandoff = onCalendarHandoff,
                    modifier = contentModifier,
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
    onShare: (String) -> Unit,
    onCalendarHandoff: (MilestoneCalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
            person = person,
            milestones = snapshot.milestones,
            onShare = onShare,
            onCalendarHandoff = onCalendarHandoff,
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
    onShare: (String) -> Unit,
    onCalendarHandoff: (MilestoneCalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
                person = person,
                milestones = snapshot.milestones,
                onShare = onShare,
                onCalendarHandoff = onCalendarHandoff,
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
            fontFamily = FontFamily.SansSerif,
            lineHeight = countSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFeatureSettings = "tnum",
            ),
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
    person: Person,
    milestones: List<MilestoneCandidate>,
    onShare: (String) -> Unit,
    onCalendarHandoff: (MilestoneCalendarAction) -> Unit,
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
        val visibleKeys = milestones.mapTo(mutableSetOf()) { milestone ->
            DayCountLogic.milestoneKey(milestone.target, milestone.date, person.hasTime)
        }
        val recordedKeys = person.addedMilestones.mapTo(mutableSetOf()) { it.key }
        items(milestones, key = MilestoneCandidate::target) { milestone ->
            MilestoneRow(
                person = person,
                milestone = milestone,
                visibleKeys = visibleKeys,
                recorded = DayCountLogic.milestoneKey(
                    milestone.target,
                    milestone.date,
                    person.hasTime,
                ) in recordedKeys,
                onShare = onShare,
                onCalendarHandoff = onCalendarHandoff,
            )
        }
    }
}

@Composable
private fun MilestoneRow(
    person: Person,
    milestone: MilestoneCandidate,
    visibleKeys: Set<String>,
    recorded: Boolean,
    onShare: (String) -> Unit,
    onCalendarHandoff: (MilestoneCalendarAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val resources = LocalResources.current
    val count = NumberFormat.getIntegerInstance(Locale.GERMANY).format(milestone.target)
    val date = milestone.date.format(GermanDateFormatter)
    val longDate = milestone.date.format(GermanLongDateFormatter)
    val tags = milestone.kinds.mapNotNull { kind ->
        if (kind == de.eigenraum.erdentage.domain.MilestoneKind.TAUSENDER ||
            kind == de.eigenraum.erdentage.domain.MilestoneKind.ZEHNTAUSENDER
        ) {
            null
        } else {
            resources.getString(kind.todayLabelResource)
        }
    }
    val captionKinds = milestone.kinds
    val tagSuffix = tags.takeIf(List<String>::isNotEmpty)
        ?.joinToString(", ", prefix = " (", postfix = ")")
        .orEmpty()
    val shareText = if (PersonName.isOwner(person.name)) {
        stringResource(R.string.share_upcoming_owner, longDate, count, tagSuffix)
    } else {
        stringResource(
            R.string.share_upcoming_named,
            longDate,
            person.displayName,
            count,
            tagSuffix,
        )
    }
    val baseTitle = if (tags.isEmpty()) {
        stringResource(R.string.calendar_title_base, count)
    } else {
        stringResource(R.string.calendar_title_tagged, count, tags.joinToString(", "))
    }
    val eventTitle = if (PersonName.isOwner(person.name)) {
        baseTitle
    } else {
        stringResource(R.string.calendar_title_named, person.displayName, baseTitle)
    }
    val milestoneKey = DayCountLogic.milestoneKey(
        milestone.target,
        milestone.date,
        person.hasTime,
    )
    val event = remember(eventTitle, milestone, person.hasTime) {
        if (person.hasTime) {
            val start = milestone.date.toInstant()
            CalendarEventDraft(
                title = eventTitle,
                startMillis = start.toEpochMilli(),
                endMillis = start.plusSeconds(3_600).toEpochMilli(),
                allDay = false,
            )
        } else {
            val start = milestone.date.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
            CalendarEventDraft(
                title = eventTitle,
                startMillis = start.toEpochMilli(),
                endMillis = start.plusSeconds(86_400).toEpochMilli(),
                allDay = true,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (captionKinds.isEmpty()) {
                Text(" ", style = MaterialTheme.typography.bodySmall)
            } else {
                captionKinds.forEach { kind ->
                    Text(
                        text = stringResource(kind.captionResource),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = stringResource(R.string.milestone_date, count, date),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (recorded) {
                Text(
                    text = stringResource(R.string.calendar_handoff_recorded),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.milestone_actions, count),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_milestone)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onShare(shareText)
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (recorded) R.string.calendar_add_again else R.string.calendar_add,
                            ),
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onCalendarHandoff(
                            MilestoneCalendarAction(
                                personId = person.id,
                                milestoneKey = milestoneKey,
                                visibleKeys = visibleKeys,
                                event = event,
                            ),
                        )
                    },
                )
            }
        }
    }
}

data class MilestoneCalendarAction(
    val personId: String,
    val milestoneKey: String,
    val visibleKeys: Set<String>,
    val event: CalendarEventDraft,
)

private val GermanDateFormatter = DateTimeFormatter
    .ofPattern("EEE d. MMM uuuu", Locale.GERMANY)

private val GermanLongDateFormatter = DateTimeFormatter
    .ofPattern("EEEE, d. MMMM uuuu", Locale.GERMANY)
