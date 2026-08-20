// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.ui.person.explanationResource
import de.eigenraum.erdentage.ui.person.labelResource
import de.eigenraum.erdentage.ui.icons.ErdentageIcons
import de.eigenraum.erdentage.ui.theme.LocalDarkTheme
import de.eigenraum.erdentage.ui.theme.surfaceColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSettingsDialog(
    person: Person,
    onDismiss: () -> Unit,
    onSave: (Person) -> Unit,
) {
    var draft by remember(person.id) { mutableStateOf(person) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    onSave(draft.normalizedForStorage())
                                },
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.save),
                                )
                            }
                        },
                    )
                },
            ) { contentPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .testTag("person_settings_list")
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .imePadding(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            top = 8.dp,
                            end = 20.dp,
                            bottom = 40.dp,
                        ),
                    ) {
                        item { SettingsSectionTitle(R.string.settings_name_section) }
                        item {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.settings_name_label)) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            singleLine = true,
                        )
                    }

                        item { SettingsSectionTitle(R.string.settings_birth_section) }
                        item {
                        SettingsValueRow(
                            label = stringResource(R.string.settings_date_label),
                            value = validBirthDate(draft).format(SettingsDateFormatter),
                            onClick = {
                                showBirthDatePicker(
                                    context = context,
                                    person = draft,
                                    onSelected = { date ->
                                        val zone = ZoneId.systemDefault()
                                        draft = PersonSettingsLogic.setBirthDate(
                                            person = draft,
                                            date = date,
                                            today = LocalDate.now(zone),
                                            now = LocalTime.now(zone),
                                        )
                                    },
                                )
                            },
                        )
                    }
                        item {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.settings_time_known))
                            },
                            trailingContent = {
                                Switch(
                                    checked = draft.hasTime,
                                    onCheckedChange = { enabled ->
                                        val zone = ZoneId.systemDefault()
                                        draft = PersonSettingsLogic.setTimeKnown(
                                            person = draft,
                                            enabled = enabled,
                                            today = LocalDate.now(zone),
                                            now = LocalTime.now(zone),
                                        )
                                    },
                                )
                            },
                        )
                    }
                        if (draft.hasTime) {
                            item {
                            SettingsValueRow(
                                label = stringResource(R.string.settings_time_label),
                                value = LocalTime.of(draft.birthHour, draft.birthMinute)
                                    .format(SettingsTimeFormatter),
                                onClick = {
                                    showBirthTimePicker(
                                        context = context,
                                        person = draft,
                                        onSelected = { time ->
                                            val zone = ZoneId.systemDefault()
                                            draft = PersonSettingsLogic.setBirthTime(
                                                person = draft,
                                                time = time,
                                                today = LocalDate.now(zone),
                                                now = LocalTime.now(zone),
                                            )
                                        },
                                    )
                                },
                            )
                            }
                        }

                        item { SettingsSectionTitle(R.string.settings_astronaut_section) }
                        item {
                        DaysOffEarthControl(
                            value = draft.daysOffEarth,
                            onValueChange = { draft = draft.copy(daysOffEarth = it) },
                        )
                    }
                        item {
                        SettingsSupportingText(R.string.settings_astronaut_explanation)
                    }

                        item { SettingsSectionTitle(R.string.settings_color_section) }
                        item {
                        TintPicker(
                            selection = draft.tint ?: PersonTint.SAND,
                            onSelected = { draft = draft.copy(tint = it) },
                        )
                    }

                        item { SettingsSectionTitle(R.string.settings_milestones_section) }
                        items(TopLevelMilestones, key = MilestoneKind::name) { kind ->
                        if (kind == MilestoneKind.PRIMZAHL) {
                            MilestoneToggle(
                                kind = kind,
                                checked = PersonSettingsLogic.primesEnabled(draft),
                                onCheckedChange = { enabled ->
                                    draft = PersonSettingsLogic.setPrimesEnabled(draft, enabled)
                                },
                            )
                            if (PersonSettingsLogic.primesEnabled(draft)) {
                                MilestoneToggle(
                                    kind = MilestoneKind.ZWILLINGSPRIMZAHL,
                                    title = stringResource(R.string.settings_twin_only),
                                    checked = MilestoneKind.ZWILLINGSPRIMZAHL in
                                        draft.enabledMilestones,
                                    onCheckedChange = { enabled ->
                                        draft = PersonSettingsLogic.setTwinOnly(draft, enabled)
                                    },
                                    modifier = Modifier.padding(start = 20.dp),
                                )
                            }
                        } else {
                            MilestoneToggle(
                                kind = kind,
                                checked = kind in draft.enabledMilestones,
                                onCheckedChange = { enabled ->
                                    draft = PersonSettingsLogic.setMilestone(
                                        draft,
                                        kind,
                                        enabled,
                                    )
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                        item { SettingsSupportingText(R.string.settings_milestones_footer) }

                        item { SettingsSectionTitle(R.string.settings_about_section) }
                        item { AboutText() }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(resource: Int) {
    Text(
        text = stringResource(resource),
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SettingsSupportingText(resource: Int) {
    Text(
        text = stringResource(resource),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        ),
    )
}

@Composable
private fun DaysOffEarthControl(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val decreaseDescription = stringResource(R.string.settings_decrease_days)
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { raw ->
                if (raw.all(Char::isDigit)) {
                    onValueChange(
                        raw.toIntOrNull()
                            ?.coerceIn(0, ErdentageLimits.MAX_DAYS_OFF_EARTH)
                            ?: 0,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_days_off_earth)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = { onValueChange((value - 1).coerceAtLeast(0)) },
                enabled = value > 0,
                modifier = Modifier.semantics {
                    contentDescription = decreaseDescription
                },
            ) {
                Icon(
                    ErdentageIcons.Remove,
                    contentDescription = null,
                )
            }
            IconButton(
                onClick = {
                    onValueChange(
                        (value + 1).coerceAtMost(ErdentageLimits.MAX_DAYS_OFF_EARTH),
                    )
                },
                enabled = value < ErdentageLimits.MAX_DAYS_OFF_EARTH,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.settings_increase_days),
                )
            }
        }
    }
}

@Composable
private fun TintPicker(
    selection: PersonTint,
    onSelected: (PersonTint) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(PersonTint.entries, key = PersonTint::name) { tint ->
            val selected = tint == selection
            val description = stringResource(tint.labelResource)
            IconButton(
                onClick = { onSelected(tint) },
                modifier = Modifier.semantics {
                    contentDescription = description
                    this.selected = selected
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(tint.surfaceColor(darkTheme), CircleShape)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else if (tint == PersonTint.WHITE) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun MilestoneToggle(
    kind: MilestoneKind,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val visibleTitle = title ?: stringResource(kind.labelResource)
    val explanation = milestoneExplanation(kind)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = visibleTitle, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = explanation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AboutText() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionCode = if (Build.VERSION.SDK_INT >= 28) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val linkColor = MaterialTheme.colorScheme.primary
    val feedback = buildAnnotatedString {
        append(stringResource(R.string.settings_feedback_prefix))
        append(' ')
        withLink(
            LinkAnnotation.Url(
                url = FeedbackMailUri,
                linkInteractionListener = { uriHandler.openUri(FeedbackMailUri) },
            ),
        ) {
            pushStyle(SpanStyle(color = linkColor))
            append(stringResource(R.string.settings_feedback_mail))
            pop()
        }
        append(' ')
        append(stringResource(R.string.settings_feedback_or))
        append(' ')
        withLink(
            LinkAnnotation.Url(
                url = MastodonUri,
                linkInteractionListener = { uriHandler.openUri(MastodonUri) },
            ),
        ) {
            pushStyle(SpanStyle(color = linkColor))
            append(stringResource(R.string.settings_feedback_mastodon))
            pop()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                R.string.settings_about,
                packageInfo.versionName ?: "?",
                versionCode,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = feedback,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun milestoneExplanation(kind: MilestoneKind): androidx.compose.ui.text.AnnotatedString {
    val source = stringResource(kind.explanationResource)
    val markerAndUrl = when (kind) {
        MilestoneKind.CATALAN -> "OEIS A000108" to "https://oeis.org/A000108"
        MilestoneKind.FIBONACCI -> "OEIS A000045" to "https://oeis.org/A000045"
        else -> null
    } ?: return androidx.compose.ui.text.AnnotatedString(source)
    val (marker, url) = markerAndUrl
    val markerStart = source.indexOf(marker)
    if (markerStart < 0) return androidx.compose.ui.text.AnnotatedString(source)
    val linkColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        append(source.substring(0, markerStart))
        withLink(LinkAnnotation.Url(url)) {
            pushStyle(SpanStyle(color = linkColor))
            append(marker)
            pop()
        }
        append(source.substring(markerStart + marker.length))
    }
}

private fun showBirthDatePicker(
    context: android.content.Context,
    person: Person,
    onSelected: (LocalDate) -> Unit,
) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val current = validBirthDate(person)
    DatePickerDialog(
        context,
        R.style.ThemeOverlay_Erdentage_DatePicker,
        { _, year, month, day -> onSelected(LocalDate.of(year, month + 1, day)) },
        current.year,
        current.monthValue - 1,
        current.dayOfMonth,
    ).apply {
        datePicker.maxDate = today
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1
        setOnShowListener {
            getButton(DialogInterface.BUTTON_NEGATIVE).setText(R.string.cancel)
            getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.apply)
        }
    }.show()
}

private fun showBirthTimePicker(
    context: android.content.Context,
    person: Person,
    onSelected: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) },
        person.birthHour,
        person.birthMinute,
        true,
    ).apply {
        setOnShowListener {
            getButton(DialogInterface.BUTTON_NEGATIVE).setText(R.string.cancel)
            getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.apply)
        }
    }.show()
}

private fun validBirthDate(person: Person): LocalDate =
    PersonSettingsLogic.birthDate(person)
        ?: LocalDate.now(ZoneId.systemDefault()).minusDays(5_000)

private val TopLevelMilestones = listOf(
    MilestoneKind.TAUSENDER,
    MilestoneKind.ZEHNTAUSENDER,
    MilestoneKind.PRIMZAHL,
    MilestoneKind.PALINDROM,
    MilestoneKind.QUADRAT,
    MilestoneKind.ZWEIERPOTENZ,
    MilestoneKind.ZIFFERNFOLGE,
    MilestoneKind.CATALAN,
    MilestoneKind.FIBONACCI,
)

private val SettingsDateFormatter = DateTimeFormatter
    .ofPattern("d. MMMM uuuu", Locale.GERMANY)

private val SettingsTimeFormatter = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.SHORT)
    .withLocale(Locale.GERMANY)

private const val FeedbackMailUri =
    "mailto:thomas.kahle@jpberlin.de?subject=Feedback%20Erdentage"
private const val MastodonUri = "https://machteburch.social/@tomkalei"
