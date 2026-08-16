// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.DayCountLogic
import de.eigenraum.erdentage.domain.DayCountSnapshot
import de.eigenraum.erdentage.domain.MilestoneCandidate
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.MilestoneMath
import de.eigenraum.erdentage.domain.Person
import java.time.Instant
import java.time.ZoneId

data class PersonPageUiState(
    val person: Person,
    val personIndex: Int,
    val snapshot: DayCountSnapshot,
    val todayKinds: List<MilestoneKind>,
)

data class OverviewEventUiState(
    val person: Person,
    val personIndex: Int,
    val milestone: MilestoneCandidate,
)

data class ErdentageUiState(
    val persisted: PersistedAppState,
    val personPages: List<PersonPageUiState>,
    val overviewEvents: List<OverviewEventUiState>,
) {
    companion object {
        fun create(
            persisted: PersistedAppState,
            at: Instant,
            zoneId: ZoneId,
        ): ErdentageUiState {
            val pages = persisted.people.mapIndexed { index, person ->
                val snapshot = DayCountLogic.snapshotAt(at, zoneId, person, index)
                PersonPageUiState(
                    person = person,
                    personIndex = index,
                    snapshot = snapshot,
                    todayKinds = MilestoneMath.currentMilestoneKinds(
                        days = snapshot.days,
                        enabled = person.enabledMilestones,
                    ),
                )
            }
            val events = pages
                .flatMap { page ->
                    page.snapshot.milestones.map { milestone ->
                        OverviewEventUiState(
                            person = page.person,
                            personIndex = page.personIndex,
                            milestone = milestone,
                        )
                    }
                }
                .sortedBy { it.milestone.date.toInstant() }
                .take(30)
            return ErdentageUiState(
                persisted = persisted,
                personPages = pages,
                overviewEvents = events,
            )
        }
    }
}
