package com.gymcompanion.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.data.model.StepRecord
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StepsViewModel @Inject constructor(
    private val repo: GymRepository
) : ViewModel() {

    /** Suit le changement de jour : réémet la requête au passage de minuit. */
    val todayRecord: StateFlow<StepRecord?> =
        todayFlow().flatMapLatest { date -> repo.getStepsForDate(date) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weekHistory: StateFlow<List<StepRecord>> =
        repo.getRecentSteps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Mise à jour atomique des pas + objectif en UN SEUL insert.
     * (L'ancien couple updateSteps() + setGoal() lisait un StateFlow périmé
     * et pouvait écraser les pas fraîchement saisis avec steps = 0.)
     */
    fun updateSteps(steps: Int, goal: Int? = null) = viewModelScope.launch {
        val current = todayRecord.value
        val effectiveGoal = goal ?: current?.goal ?: 10000
        repo.updateSteps(
            StepRecord(
                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                steps = steps,
                goal = effectiveGoal,
                distanceKm = steps * 0.000762f,
                caloriesBurned = (steps * 0.04f).toInt()
            )
        )
    }
}
