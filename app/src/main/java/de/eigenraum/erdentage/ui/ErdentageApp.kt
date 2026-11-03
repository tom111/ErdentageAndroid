// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.app.ErdentageUiState
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.PageKind
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.domain.TogetherKind
import de.eigenraum.erdentage.platform.ContactPersonDraft
import de.eigenraum.erdentage.ui.manage.ManagePeopleDialog
import de.eigenraum.erdentage.ui.overview.OverviewPage
import de.eigenraum.erdentage.ui.person.PersonPage
import de.eigenraum.erdentage.ui.person.MilestoneCalendarAction
import de.eigenraum.erdentage.ui.settings.PersonSettingsDialog
import de.eigenraum.erdentage.ui.together.TogetherCalendarAction
import de.eigenraum.erdentage.ui.together.TogetherPage
import de.eigenraum.erdentage.ui.together.TogetherSettingsDialog
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErdentageApp(
    state: ErdentageUiState,
    onAddFirstPerson: (LocalDate) -> Unit,
    onAddPerson: (Person) -> Unit,
    onUpdatePerson: (Person) -> Unit,
    onDeletePerson: (String) -> Unit,
    onMovePerson: (Int, Int) -> Unit,
    onPageSelected: (Int) -> Unit,
    onCalendarHandoff: (MilestoneCalendarAction) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
    contactDraft: ContactPersonDraft? = null,
    onPickContact: () -> Unit = {},
    onContactDraftConsumed: () -> Unit = {},
    onTogetherCalendarHandoff: (TogetherCalendarAction) -> Unit = {},
    onSetTogetherLimit: (TogetherKind, Int) -> Unit = { _, _ -> },
) {
    val persisted = state.persisted
    var editFirstPersonAfterCreation by rememberSaveable { mutableStateOf(false) }
    if (persisted.people.isEmpty()) {
        EmptyStartScreen(
            onBirthDateSelected = { date ->
                editFirstPersonAfterCreation = true
                onAddFirstPerson(date)
            },
            modifier = modifier,
        )
        return
    }

    val peopleCount = state.personPages.size
    val lastPage = ErdentageLimits.lastPageIndex(peopleCount)
    val pageCount = lastPage + 1
    val selectedPage = persisted.currentPersonIndex.coerceIn(0, lastPage)
    val pagerState = rememberPagerState(initialPage = selectedPage) { pageCount }
    var managePeopleVisible by rememberSaveable { mutableStateOf(false) }
    var editingPersonState by rememberSaveable { mutableStateOf<String?>(null) }
    val editingPerson = remember(editingPersonState) {
        PersonEditorStateCodec.decodeOrNull(editingPersonState)
    }
    var editorCreatesPerson by rememberSaveable { mutableStateOf(false) }
    var returnToManager by rememberSaveable { mutableStateOf(false) }
    var togetherSettingsVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editFirstPersonAfterCreation, persisted.people) {
        if (editFirstPersonAfterCreation) {
            persisted.people.firstOrNull()?.let { firstPerson ->
                editingPersonState = PersonEditorStateCodec.encode(firstPerson)
                editorCreatesPerson = false
                returnToManager = false
                editFirstPersonAfterCreation = false
            }
        }
    }
    LaunchedEffect(contactDraft?.token) {
        contactDraft?.let { imported ->
            editingPersonState = PersonEditorStateCodec.encode(
                Person(
                    name = imported.name,
                    birthYear = imported.birthDate.year,
                    birthMonth = imported.birthDate.monthValue,
                    birthDay = imported.birthDate.dayOfMonth,
                    tint = PersonTint.firstUnused(persisted.people),
                ),
            )
            editorCreatesPerson = true
            returnToManager = true
            managePeopleVisible = false
            onContactDraftConsumed()
        }
    }

    LaunchedEffect(selectedPage, pageCount) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.scrollToPage(selectedPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect(onPageSelected)
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            key = { page ->
                when (val kind = ErdentageLimits.pageKind(page, peopleCount)) {
                    is PageKind.Person -> state.personPages[kind.index].person.id
                    PageKind.Demnaechst -> "overview"
                    PageKind.Zusammen -> "together"
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (val kind = ErdentageLimits.pageKind(page, peopleCount)) {
                is PageKind.Person -> PersonPage(
                    state = state.personPages[kind.index],
                    onShare = onShare,
                    onCalendarHandoff = onCalendarHandoff,
                )
                PageKind.Demnaechst -> OverviewPage(events = state.overviewEvents)
                PageKind.Zusammen -> TogetherPage(
                    people = persisted.people,
                    events = state.togetherEvents,
                    constellations = state.togetherConstellations,
                    addedKeys = persisted.togetherAddedEvents.mapTo(mutableSetOf()) { it.key },
                    today = state.today,
                    onShare = onShare,
                    onCalendarHandoff = onTogetherCalendarHandoff,
                )
            }
        }

        state.personPages.getOrNull(pagerState.currentPage)?.let { personPage ->
            PersonTopControls(
                state = personPage,
                onManagePeople = { managePeopleVisible = true },
                onEditPerson = {
                    editingPersonState = PersonEditorStateCodec.encode(personPage.person)
                    editorCreatesPerson = false
                    returnToManager = false
                },
                onShare = onShare,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        // Page-specific chrome: Zusammen gets its own settings button,
        // „Demnächst“ keeps none.
        if (ErdentageLimits.pageKind(pagerState.currentPage, peopleCount) == PageKind.Zusammen) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { togetherSettingsVisible = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.together_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        PageIndicator(
            pageCount = pageCount,
            selectedPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }

    if (managePeopleVisible) {
        ManagePeopleDialog(
            people = persisted.people,
            selectedPersonId = persisted.people
                .getOrNull(persisted.currentPersonIndex)
                ?.id,
            onDismiss = { managePeopleVisible = false },
            onAddManual = {
                val preset = LocalDate.now(ZoneId.systemDefault()).minusDays(5_000)
                editingPersonState = PersonEditorStateCodec.encode(
                    Person(
                        name = "",
                        birthYear = preset.year,
                        birthMonth = preset.monthValue,
                        birthDay = preset.dayOfMonth,
                        tint = PersonTint.firstUnused(persisted.people),
                    ),
                )
                editorCreatesPerson = true
                returnToManager = true
                managePeopleVisible = false
            },
            onImportContact = onPickContact,
            onEdit = { person ->
                editingPersonState = PersonEditorStateCodec.encode(person)
                editorCreatesPerson = false
                returnToManager = true
                managePeopleVisible = false
            },
            onSelect = onPageSelected,
            onDelete = onDeletePerson,
            onMove = onMovePerson,
        )
    }

    if (togetherSettingsVisible) {
        TogetherSettingsDialog(
            limits = TogetherKind.entries.associateWith(persisted::togetherLimit),
            onLimitChange = onSetTogetherLimit,
            onDismiss = { togetherSettingsVisible = false },
        )
    }

    editingPerson?.let { person ->
        PersonSettingsDialog(
            person = person,
            onDismiss = {
                editingPersonState = null
                if (returnToManager) managePeopleVisible = true
            },
            onSave = { updated ->
                if (editorCreatesPerson) {
                    onAddPerson(updated)
                } else {
                    onUpdatePerson(updated)
                }
                editingPersonState = null
                if (returnToManager) managePeopleVisible = true
            },
            // A person being created does not exist yet; cancel is their delete.
            onDelete = if (editorCreatesPerson) {
                null
            } else {
                {
                    editingPersonState = null
                    onDeletePerson(person.id)
                    if (returnToManager) managePeopleVisible = true
                }
            },
        )
    }
}
