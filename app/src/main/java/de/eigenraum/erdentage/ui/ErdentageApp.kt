// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.app.ErdentageUiState
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.ui.manage.ManagePeopleDialog
import de.eigenraum.erdentage.ui.overview.OverviewPage
import de.eigenraum.erdentage.ui.person.PersonPage
import de.eigenraum.erdentage.ui.settings.PersonSettingsDialog
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
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val persisted = state.persisted
    if (persisted.people.isEmpty()) {
        EmptyStartScreen(
            onBirthDateSelected = onAddFirstPerson,
            modifier = modifier,
        )
        return
    }

    val pageCount = state.personPages.size + 1
    val selectedPage = persisted.currentPersonIndex.coerceIn(0, state.personPages.size)
    val pagerState = rememberPagerState(initialPage = selectedPage) { pageCount }
    var managePeopleVisible by rememberSaveable { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var editorCreatesPerson by remember { mutableStateOf(false) }
    var returnToManager by remember { mutableStateOf(false) }

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
            key = { page -> state.personPages.getOrNull(page)?.person?.id ?: "overview" },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val personPage = state.personPages.getOrNull(page)
            if (personPage == null) {
                OverviewPage(events = state.overviewEvents)
            } else {
                PersonPage(state = personPage)
            }
        }

        state.personPages.getOrNull(pagerState.currentPage)?.let { personPage ->
            PersonTopControls(
                state = personPage,
                onManagePeople = { managePeopleVisible = true },
                onEditPerson = {
                    editingPerson = personPage.person
                    editorCreatesPerson = false
                    returnToManager = false
                },
                onShare = onShare,
                modifier = Modifier.align(Alignment.TopCenter),
            )
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
            onAdd = {
                val preset = LocalDate.now(ZoneId.systemDefault()).minusDays(5_000)
                editingPerson = Person(
                    name = "",
                    birthYear = preset.year,
                    birthMonth = preset.monthValue,
                    birthDay = preset.dayOfMonth,
                    tint = PersonTint.firstUnused(persisted.people),
                )
                editorCreatesPerson = true
                returnToManager = true
                managePeopleVisible = false
            },
            onEdit = { person ->
                editingPerson = person
                editorCreatesPerson = false
                returnToManager = true
                managePeopleVisible = false
            },
            onSelect = onPageSelected,
            onDelete = onDeletePerson,
            onMove = onMovePerson,
        )
    }

    editingPerson?.let { person ->
        PersonSettingsDialog(
            person = person,
            onDismiss = {
                editingPerson = null
                if (returnToManager) managePeopleVisible = true
            },
            onSave = { updated ->
                if (editorCreatesPerson) {
                    onAddPerson(updated)
                } else {
                    onUpdatePerson(updated)
                }
                editingPerson = null
                if (returnToManager) managePeopleVisible = true
            },
        )
    }
}
