// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.eigenraum.erdentage.data.PeopleRepository
import de.eigenraum.erdentage.data.PersistedAppState
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.domain.PersonTint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ErdentageViewModel(
    private val repository: PeopleRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ErdentageUiState?>(null)
    val state: StateFlow<ErdentageUiState?> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collectLatest(::publishLiveUiState)
        }
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
            repository.updatePerson(person.id) { person }
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

    private suspend fun publishLiveUiState(persisted: PersistedAppState) {
        while (currentCoroutineContext().isActive) {
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            mutableState.value = ErdentageUiState.create(persisted, now, zone)

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
