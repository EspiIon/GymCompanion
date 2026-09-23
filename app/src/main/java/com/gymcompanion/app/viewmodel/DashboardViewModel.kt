package com.gymcompanion.app.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.data.repository.GymRepository
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.AUTO_MACRO_GOALS_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CALORIE_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CARBS_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.FAT_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.PROTEIN_GOAL_PREF
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardState(
    val todayCalories: Int = 0,
    val calorieGoal: Int = 2000,
    val proteinGoal: Float = 150f,
    val carbsGoal: Float = 225f,
    val fatGoal: Float = 56f,
    val todaySteps: Int = 0,
    val stepGoal: Int = 10000,
    val workoutStreak: Int = 0,
    val nutritionStreak: Int = 0,
    val todayWorkouts: List<WorkoutSession> = emptyList(),
    val latestBodyRecord: BodyRecord? = null,
    val bodyHistory: List<BodyRecord> = emptyList(),
    val todayNutrition: DailyNutritionSummary? = null,
    val recentWorkouts: List<WorkoutSession> = emptyList(),
    val userProfile: UserProfile? = null,
    val activeGoals: List<Goal> = emptyList()
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val repo: GymRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    /** Tout le pipeline dépend de la date du jour → bascule automatiquement à minuit. */
    val state: StateFlow<DashboardState> = todayFlow().flatMapLatest { today ->
        val since30 = LocalDate.parse(today).minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)

        combine(
            combine(
                repo.getDailySummary(today),
                repo.getStepsForDate(today),
                repo.getStreak("workout"),
                repo.getStreak("nutrition")
            ) { summary, steps, ws, ns -> Pair(Pair(summary, steps), Pair(ws, ns)) },

            combine(
                repo.getWorkoutsForDate(today),
                repo.getLatestBodyRecord(),
                repo.getRecentWorkouts(),
                repo.getBodyRecordsSince(since30)
            ) { workouts, body, recent, hist -> Pair(Pair(workouts, body), Pair(recent, hist)) },

            combine(
                repo.getUserProfile(),
                repo.getActiveGoals(5)
            ) { profile, goals -> Pair(profile, goals) },

            dataStore.data.distinctUntilChanged()

        ) { res1, res2, res3, prefs ->
            val (p1, p2) = res1
            val (summary, steps) = p1
            val (workoutStreak, nutritionStreak) = p2

            val (p3, p4) = res2
            val (workouts, body) = p3
            val (recent, bodyHistory) = p4

            val (profile, goals) = res3

            val weight     = body?.weightKg ?: 70f
            val autoMacros = prefs[AUTO_MACRO_GOALS_PREF] ?: true
            val calories   = prefs[CALORIE_GOAL_PREF] ?: (profile?.tdeeKcal(weight) ?: 2000)

            val (protein, carbs, fat) = if (autoMacros) {
                Triple(
                    calories * 0.30f / 4f,
                    calories * 0.45f / 4f,
                    calories * 0.25f / 9f
                )
            } else {
                Triple(
                    prefs[PROTEIN_GOAL_PREF] ?: 150f,
                    prefs[CARBS_GOAL_PREF]   ?: 225f,
                    prefs[FAT_GOAL_PREF]     ?: 56f
                )
            }

            DashboardState(
                todayCalories    = summary?.totalCalories ?: 0,
                calorieGoal      = calories,
                proteinGoal      = protein,
                carbsGoal        = carbs,
                fatGoal          = fat,
                todaySteps       = steps?.steps ?: 0,
                stepGoal         = steps?.goal ?: 10000,
                workoutStreak    = workoutStreak?.currentStreak ?: 0,
                nutritionStreak  = nutritionStreak?.currentStreak ?: 0,
                todayWorkouts    = workouts,
                latestBodyRecord = body,
                bodyHistory      = bodyHistory,
                todayNutrition   = summary,
                recentWorkouts   = recent,
                userProfile      = profile,
                activeGoals      = goals
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}


