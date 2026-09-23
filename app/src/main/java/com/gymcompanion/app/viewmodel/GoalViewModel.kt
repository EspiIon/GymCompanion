package com.gymcompanion.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.model.Goal
import com.gymcompanion.app.data.model.GoalCategory
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val repo: GymRepository
) : ViewModel() {

    val allGoals: StateFlow<List<Goal>> =
        repo.getAllGoals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Top 5 active goals for dashboard widget */
    val activeGoals: StateFlow<List<Goal>> =
        repo.getActiveGoals(5)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(title: String, category: GoalCategory) = viewModelScope.launch {
        repo.upsertGoal(Goal(title = title, category = category))
    }

    fun toggleGoal(goal: Goal) = viewModelScope.launch {
        repo.setGoalCompleted(goal.id, !goal.isCompleted)
    }

    fun deleteGoal(goal: Goal) = viewModelScope.launch {
        repo.deleteGoal(goal)
    }
}
