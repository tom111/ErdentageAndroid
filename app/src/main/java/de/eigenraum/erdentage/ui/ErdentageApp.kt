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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.app.ErdentageUiState
import de.eigenraum.erdentage.ui.overview.OverviewPage
import de.eigenraum.erdentage.ui.person.PersonPage
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErdentageApp(
    state: ErdentageUiState,
    onAddFirstPerson: (java.time.LocalDate) -> Unit,
    onPageSelected: (Int) -> Unit,
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

        PageIndicator(
            pageCount = pageCount,
            selectedPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }
}
