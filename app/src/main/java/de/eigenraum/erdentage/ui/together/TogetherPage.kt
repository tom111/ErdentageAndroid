// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.together

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.Constellation
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.domain.TogetherDistance
import de.eigenraum.erdentage.domain.TogetherEvent
import de.eigenraum.erdentage.domain.TogetherKind
import de.eigenraum.erdentage.domain.TogetherMath
import de.eigenraum.erdentage.platform.CalendarEventDraft
import de.eigenraum.erdentage.ui.overview.OverviewCloudMath
import de.eigenraum.erdentage.ui.overview.cloudShape
import de.eigenraum.erdentage.ui.overview.overviewSky
import de.eigenraum.erdentage.ui.person.todayLabelResource
import de.eigenraum.erdentage.ui.theme.LocalDarkTheme
import de.eigenraum.erdentage.ui.theme.inkSlateColor
import de.eigenraum.erdentage.ui.theme.surfaceColor
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A „Zusammen“ row's calendar handoff; the record means it was launched, not saved. */
data class TogetherCalendarAction(
    val addedKey: String,
    val visibleKeys: Set<String>,
    val event: CalendarEventDraft,
)

/**
 * The „Zusammen“ page: the second trailing page, present from two people
 * onward. One section per Wechselwirkungs-Kategorie, rarest first and
 * chronological within, then the undated „Konstellationen“, which double as
 * the empty state. Shares the „Demnächst“ sky, so a swipe between the two
 * trailing pages does not cut to a different world.
 */
@Composable
fun TogetherPage(
    people: List<Person>,
    events: List<TogetherEvent>,
    constellations: List<Constellation>,
    addedKeys: Set<String>,
    today: LocalDate,
    onShare: (String) -> Unit,
    onCalendarHandoff: (TogetherCalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = LocalDarkTheme.current
    val ink = inkSlateColor(darkTheme)
    val tints = remember(people) {
        people.mapIndexed { index, person -> person.id to person.resolvedTint(index) }.toMap()
    }
    val visibleKeys = remember(events) { events.mapTo(mutableSetOf()) { it.addedKey } }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(overviewSky(darkTheme)),
    ) {
        val compactHeight = maxHeight < 500.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.together_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (compactHeight) 12.dp else 48.dp, bottom = 20.dp)
                    .semantics { heading() },
                color = ink,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            // Reading column: rows are left-aligned text with the emblem at
            // one end and the menu at the other, so a full-width card on a
            // tablet would leave a third of the display empty in between.
            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TogetherKind.entries.sortedBy { it.rarityRank }.forEach { kind ->
                    val ofKind = events.filter { it.kind == kind }
                    if (ofKind.isEmpty()) return@forEach
                    SectionHeader(
                        title = stringResource(kind.labelResource),
                        hint = stringResource(kind.pageHintResource),
                        ink = ink,
                    )
                    ofKind.forEach { event ->
                        TogetherEventRow(
                            event = event,
                            tints = tints,
                            added = event.addedKey in addedKeys,
                            visibleKeys = visibleKeys,
                            today = today,
                            darkTheme = darkTheme,
                            onShare = onShare,
                            onCalendarHandoff = onCalendarHandoff,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
                if (constellations.isEmpty()) {
                    if (events.isEmpty()) {
                        Text(
                            text = stringResource(R.string.together_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            color = ink.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    SectionHeader(
                        title = stringResource(R.string.together_constellations),
                        hint = null,
                        ink = ink,
                    )
                    constellations.forEach { constellation ->
                        ConstellationRow(
                            constellation = constellation,
                            tints = tints,
                            darkTheme = darkTheme,
                        )
                    }
                }
            }
        }
    }
}

/** Heading plus, optionally, the one line that says what the category means. */
@Composable
private fun SectionHeader(title: String, hint: String?, ink: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.uppercase(Locale.GERMANY),
            modifier = Modifier.semantics { heading() },
            color = ink.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
        )
        if (hint != null) {
            Text(
                text = hint,
                modifier = Modifier.padding(horizontal = 12.dp),
                color = ink.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TogetherCard(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = if (darkTheme) 0.07f else 0.45f),
        contentColor = inkSlateColor(darkTheme),
    ) {
        Box(modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)) {
            content()
        }
    }
}

/**
 * One dated Wechselwirkung: the overlapping-cloud emblem, the sentence,
 * its caption where one carries data, the distance and date, and one
 * trailing actions target, like the personal milestone rows.
 */
@Composable
private fun TogetherEventRow(
    event: TogetherEvent,
    tints: Map<String, PersonTint>,
    added: Boolean,
    visibleKeys: Set<String>,
    today: LocalDate,
    darkTheme: Boolean,
    onShare: (String) -> Unit,
    onCalendarHandoff: (TogetherCalendarAction) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val ink = inkSlateColor(darkTheme)
    val resources = LocalResources.current
    val distance = distanceString(
        TogetherMath.distance(event.dayIndex, TogetherMath.dayIndex(today)),
    )
    val dateText = event.date.format(TogetherDateFormatter)
    val caption = event.caption?.takeIf { event.kind.showsRowCaption }
    val spokenDate = event.date.format(TogetherSpokenDateFormatter)
    val addedDescription = stringResource(R.string.together_added)
    // What TalkBack reads, in the order the eye takes the row: the sentence,
    // its caption, then when. The cake is dropped from the spoken sentence.
    val spokenLabel = buildList {
        add(event.headline)
        if (caption != null) add(caption)
        add(resources.getString(R.string.together_spoken_when, distance, spokenDate))
        event.birthdayNote?.let { add(it.sentence.replace(" 🎂", "")) }
    }.joinToString(". ")
    val calendarEvent = remember(event) {
        // Always all-day: the interaction math ignores time of day.
        val start = event.date.atStartOfDay(ZoneOffset.UTC).toInstant()
        CalendarEventDraft(
            title = event.calendarTitle,
            startMillis = start.toEpochMilli(),
            endMillis = start.plusSeconds(86_400).toEpochMilli(),
            allDay = true,
        )
    }

    TogetherCard(darkTheme = darkTheme) {
        Row(verticalAlignment = Alignment.Top) {
            CloudEmblem(
                seedBase = event.id,
                tints = event.participantIds.map { tints[it] ?: PersonTint.SKY },
                darkTheme = darkTheme,
                modifier = Modifier.padding(top = 2.dp, end = 14.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = spokenLabel
                        if (added) stateDescription = addedDescription
                    },
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = event.headline,
                    color = ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (caption != null) {
                    Text(
                        text = caption,
                        color = ink.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                // The Geburtstags-Zufall survives on every kind: it is a
                // coincidence of this one day, not a property of the category.
                event.birthdayNote?.let { birthday ->
                    Text(
                        text = birthday.caption,
                        color = ink.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "$distance · $dateText",
                        color = ink.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (added) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E9E4F),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.together_actions),
                        tint = ink.copy(alpha = 0.7f),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.together_share)) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShare(event.shareMessage)
                        },
                    )
                    // Stays available when already added: the handoff cannot
                    // tell whether the event was saved or deleted afterwards.
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (added) R.string.calendar_add_again else R.string.calendar_add,
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCalendarHandoff(
                                TogetherCalendarAction(
                                    addedKey = event.addedKey,
                                    visibleKeys = visibleKeys,
                                    event = calendarEvent,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun distanceString(distance: TogetherDistance): String = when (distance) {
    TogetherDistance.Today -> stringResource(R.string.together_distance_today)
    TogetherDistance.Tomorrow -> stringResource(R.string.together_distance_tomorrow)
    is TogetherDistance.Days -> stringResource(R.string.together_distance_days, distance.days)
    is TogetherDistance.Months -> stringResource(R.string.together_distance_months, distance.months)
    is TogetherDistance.Years -> stringResource(R.string.together_distance_years, distance.years)
}

/**
 * The overlapping clouds, reduced to a row glyph. At most four are drawn;
 * fills stay below full opacity so the intersection blends both tints. The
 * overlap is the concept, not decoration.
 */
@Composable
private fun CloudEmblem(
    seedBase: String,
    tints: List<PersonTint>,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val ink = inkSlateColor(darkTheme)
    val drawn = tints.take(4)
    val cloudWidth = 28.dp
    val cloudHeight = 23.dp
    val step = 13.dp
    Box(modifier = modifier.width(70.dp).height(cloudHeight)) {
        drawn.forEachIndexed { index, tint ->
            val shape = remember(seedBase, index) {
                cloudShape(OverviewCloudMath.fnv1a("$seedBase-$index"))
            }
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .size(width = cloudWidth, height = cloudHeight)
                    .background(tint.surfaceColor(darkTheme).copy(alpha = 0.85f), shape)
                    .border(
                        BorderStroke(1.1.dp, ink.copy(alpha = if (darkTheme) 0.35f else 0.7f)),
                        shape,
                    ),
            )
        }
    }
}

/** An undated pair fact: a plain card with two tint dots rather than clouds. */
@Composable
private fun ConstellationRow(
    constellation: Constellation,
    tints: Map<String, PersonTint>,
    darkTheme: Boolean,
) {
    val ink = inkSlateColor(darkTheme)
    val resources = LocalResources.current
    val tagText = constellation.tags
        .joinToString(" · ") { resources.getString(it.todayLabelResource) }
    TogetherCard(darkTheme = darkTheme) {
        Row(
            modifier = Modifier
                .padding(end = 10.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top,
        ) {
            // Dots follow the order the headline names the two.
            Box(modifier = Modifier.padding(top = 2.dp, end = 14.dp).width(30.dp).height(18.dp)) {
                constellation.displayIds.forEachIndexed { index, id ->
                    Box(
                        modifier = Modifier
                            .offset(x = if (index == 0) 0.dp else 11.dp)
                            .size(18.dp)
                            .background((tints[id] ?: PersonTint.SKY).surfaceColor(darkTheme), CircleShape)
                            .border(
                                BorderStroke(1.dp, ink.copy(alpha = if (darkTheme) 0.3f else 0.6f)),
                                CircleShape,
                            ),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = constellation.headline,
                    color = ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = constellation.detail,
                    color = ink.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (tagText.isNotEmpty()) {
                    Text(
                        text = tagText,
                        color = ink.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private val TogetherDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)

private val TogetherSpokenDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d. MMMM uuuu", Locale.GERMANY)
