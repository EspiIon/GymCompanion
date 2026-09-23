package com.gymcompanion.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CalendarViewModel @Inject constructor(
    private val repo: GymRepository
) : ViewModel() {

    val selectedMonth = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch {
            todayFlow().collect { todayStr ->
                val todayMonth = YearMonth.parse(todayStr)
                selectedMonth.update { current ->
                    if (current != todayMonth) todayMonth else current
                }
            }
        }
    }

    fun prevMonth() { selectedMonth.update { it.minusMonths(1) } }
    fun nextMonth() { selectedMonth.update { m -> if (m < YearMonth.now()) m.plusMonths(1) else m } }

    /** Fenêtre glissante de 6 mois, recalculée au changement de jour. */
    private val sinceSixMonths = todayFlow().map {
        LocalDate.parse(it).minusMonths(6).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    }.distinctUntilChanged()

    val workoutDates: StateFlow<Set<String>> =
        sinceSixMonths.flatMapLatest { repo.getWorkoutsSince(it) }
            .map { sessions -> sessions.map { it.date }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val nutritionDates: StateFlow<Set<String>> =
        sinceSixMonths.flatMapLatest { repo.getFoodEntriesSince(it) }
            .map { entries -> entries.map { it.date }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Vraie fenêtre 6 mois (l'ancien LIMIT 30 laissait les anciens mois sans marqueurs)
    val stepDates: StateFlow<Set<String>> =
        sinceSixMonths.flatMapLatest { repo.getStepsSince(it) }
            .map { records -> records.filter { it.steps > 0 }.map { it.date }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}
