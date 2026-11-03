// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.eigenraum.erdentage.data.NewerSchemaException
import de.eigenraum.erdentage.data.PeopleRepository
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.domain.TogetherKind
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ErdentageViewModel(
    private val repository: PeopleRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ErdentageUiState?>(null)
    val state: StateFlow<ErdentageUiState?> = mutableState.asStateFlow()
    private val mutableLoadError = MutableStateFlow<NewerSchemaException?>(null)

    /**
     * Set when the stored file was written by a newer release. The data is
     * left untouched; the UI explains the situation instead of showing an
     * empty family.
     */
    val loadError: StateFlow<NewerSchemaException?> = mutableLoadError.asStateFlow()
    private val uiActive = MutableStateFlow(false)
    private val refreshCounter = MutableStateFlow(0L)
    private val togetherCache = TogetherContentCache()

    init {
        viewModelScope.launch {
            combine(repository.state, uiActive, refreshCounter) { persisted, active, _ ->
                persisted to active
            }.catch { cause ->
                if (cause is NewerSchemaException) mutableLoadError.value = cause else throw cause
            }.collectLatest { (persisted, active) ->
                if (active) {
                    publishLiveUiState(persisted)
                } else {
                    publishSnapshot(persisted)
                }
            }
        }
    }

    fun onUiStarted() {
        uiActive.value = true
        refreshNow()
    }

    fun onUiStopped() {
        uiActive.value = false
    }

    fun refreshNow() {
        refreshCounter.value += 1
    }

    fun addFirstPerson(birthDate: LocalDate) {
        addPerson(
            Person(
                name = "Ich",
                birthYear = birthDate.year,
                birthMonth = birthDate.monthValue,
                birthDay = birthDate.dayOfMonth,
                tint = PersonTint.SAND,
            ),
        )
    }

    fun addPerson(person: Person) {
        viewModelScope.launch { repository.addPerson(person) }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person.id) { stored ->
                stored.withEditorValues(person)
            }
        }
    }

    fun deletePerson(personId: String) {
        viewModelScope.launch { repository.deletePerson(personId) }
    }

    fun movePerson(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { repository.movePerson(fromIndex, toIndex) }
    }

    fun selectPage(index: Int) {
        viewModelScope.launch {
            repository.setCurrentPersonIndex(index)
        }
    }

    fun recordCalendarHandoff(
        personId: String,
        milestoneKey: String,
        visibleKeys: Set<String>,
    ) {
        viewModelScope.launch {
            repository.recordCalendarHandoff(
                personId = personId,
                milestoneKey = milestoneKey,
                launchedAt = Instant.now(),
                visibleKeys = visibleKeys,
            )
        }
    }

    fun setTogetherLimit(kind: TogetherKind, limit: Int) {
        viewModelScope.launch { repository.setTogetherLimit(kind, limit) }
    }

    fun recordTogetherCalendarHandoff(key: String, visibleKeys: Set<String>) {
        viewModelScope.launch {
            repository.recordTogetherCalendarHandoff(
                key = key,
                launchedAt = Instant.now(),
                visibleKeys = visibleKeys,
            )
        }
    }

    private suspend fun publishLiveUiState(persisted: PersistedAppState) {
        while (true) {
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            mutableState.value = createUiState(persisted, now, zone)

            val next = if (persisted.people.any(Person::hasTime)) {
                now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
            } else {
                de.eigenraum.erdentage.domain.DayCountLogic.nextRolloverAfter(
                    after = now,
                    zoneId = zone,
                    person = null,
                )
            }
            delay(Duration.between(now, next).toMillis().coerceAtLeast(250L))
        }
    }

    private suspend fun publishSnapshot(persisted: PersistedAppState) {
        mutableState.value = createUiState(
            persisted = persisted,
            at = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        )
    }

    private suspend fun createUiState(
        persisted: PersistedAppState,
        at: Instant,
        zoneId: ZoneId,
    ): ErdentageUiState = withContext(Dispatchers.Default) {
        ErdentageUiState.create(persisted, at, zoneId, togetherCache)
    }

    class Factory(
        private val repository: PeopleRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ErdentageViewModel::class.java))
            return ErdentageViewModel(repository) as T
        }
    }
}

internal fun Person.withEditorValues(edited: Person): Person = copy(
    name = edited.name,
    birthYear = edited.birthYear,
    birthMonth = edited.birthMonth,
    birthDay = edited.birthDay,
    birthHour = edited.birthHour,
    birthMinute = edited.birthMinute,
    hasTime = edited.hasTime,
    daysOffEarth = edited.daysOffEarth,
    enabledMilestones = edited.enabledMilestones,
    tint = edited.tint,
    mentionsBirthday = edited.mentionsBirthday,
)
