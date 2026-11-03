// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.BirthdayCoincidence
import de.eigenraum.erdentage.domain.BirthdayNote
import de.eigenraum.erdentage.domain.Constellation
import de.eigenraum.erdentage.domain.DayCountLogic
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.DayCountSnapshot
import de.eigenraum.erdentage.domain.MilestoneCandidate
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.MilestoneMath
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.TogetherEvent
import de.eigenraum.erdentage.domain.TogetherKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PersonPageUiState(
    val person: Person,
    val personIndex: Int,
    val snapshot: DayCountSnapshot,
    val todayKinds: List<MilestoneKind>,
    /** Geburtstags-Zufälle by milestone target, for the rows that have one. */
    val birthdayNotes: Map<Int, BirthdayNote> = emptyMap(),
)

data class OverviewEventUiState(
    val person: Person,
    val personIndex: Int,
    val milestone: MilestoneCandidate,
    /** Set when the milestone lands on a tracked person's birthday. */
    val birthday: BirthdayNote? = null,
)

data class ErdentageUiState(
    val persisted: PersistedAppState,
    val personPages: List<PersonPageUiState>,
    val overviewEvents: List<OverviewEventUiState>,
    /** The civil day the state was built for; the „Zusammen“ rows measure their distance from it. */
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val togetherEvents: List<TogetherEvent> = emptyList(),
    val togetherConstellations: List<Constellation> = emptyList(),
) {
    companion object {
        fun create(
            persisted: PersistedAppState,
            at: Instant,
            zoneId: ZoneId,
            togetherCache: TogetherContentCache = TogetherContentCache(),
        ): ErdentageUiState {
            val today = at.atZone(zoneId).toLocalDate()
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
                    birthdayNotes = snapshot.milestones
                        .mapNotNull { milestone ->
                            BirthdayCoincidence.note(milestone.date.toLocalDate(), persisted.people)
                                ?.let { milestone.target to it }
                        }
                        .toMap(),
                )
            }
            val events = pages
                .flatMap { page ->
                    page.snapshot.milestones.map { milestone ->
                        OverviewEventUiState(
                            person = page.person,
                            personIndex = page.personIndex,
                            milestone = milestone,
                            birthday = page.birthdayNotes[milestone.target],
                        )
                    }
                }
                .sortedBy { it.milestone.date.toInstant() }
                .take(30)
            val together = persisted.people.size >= ErdentageLimits.MIN_PEOPLE_FOR_TOGETHER
            return ErdentageUiState(
                persisted = persisted,
                personPages = pages,
                overviewEvents = events,
                today = today,
                togetherEvents = if (together) {
                    togetherCache.events(
                        people = persisted.people,
                        today = today,
                        limits = TogetherKind.entries.associateWith(persisted::togetherLimit),
                    )
                } else {
                    emptyList()
                },
                togetherConstellations = if (together) {
                    togetherCache.constellations(persisted.people)
                } else {
                    emptyList()
                },
            )
        }
    }
}
