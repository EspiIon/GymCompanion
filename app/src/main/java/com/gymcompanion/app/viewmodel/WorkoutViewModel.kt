package com.gymcompanion.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Brouillon d'exercice saisi dans le dialog (avant persistance). */
data class ExerciseDraft(
    val name: String,
    val sets: List<SetDraft>
)
data class SetDraft(val reps: Int, val weightKg: Float)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repo: GymRepository
) : ViewModel() {

    val recentWorkouts: StateFlow<List<WorkoutSession>> =
        repo.getRecentWorkouts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutStreak: StateFlow<Streak?> =
        repo.getStreak("workout")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Tous les sets, groupés par séance (pour déplier l'historique). */
    val setsBySession: StateFlow<Map<Long, List<ExerciseSet>>> =
        repo.getAllExerciseSets()
            .map { all -> all.groupBy { it.sessionId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Noms d'exercices déjà utilisés (suggestions de saisie). */
    val distinctExerciseNames: StateFlow<List<String>> =
        repo.getDistinctExerciseNames()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastPrInfo = MutableStateFlow<String?>(null)
    val lastPrInfo: StateFlow<String?> = _lastPrInfo.asStateFlow()
    fun clearPr() { _lastPrInfo.value = null }

    fun addWorkout(
        name: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        category: WorkoutCategory,
        muscleGroups: String = "",
        notes: String = "",
        exercises: List<ExerciseDraft> = emptyList()
    ) = viewModelScope.launch {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val prs = mutableListOf<String>()

        // ── PR séance : lecture fraîche en base (jamais le cache WhileSubscribed) ──
        val existing = repo.getRecentWorkouts().first().filter { it.name.equals(name, ignoreCase = true) }
        if (existing.isNotEmpty()) {
            val maxCal = existing.maxOf { it.caloriesBurned }
            val maxDur = existing.maxOf { it.durationMinutes }
            val isPrCal = caloriesBurned > 0 && caloriesBurned > maxCal
            val isPrDur = durationMinutes > maxDur
            if (isPrCal && isPrDur) prs += "PR — ${durationMinutes} min · ${caloriesBurned} kcal !"
            else if (isPrCal) prs += "Record — ${caloriesBurned} kcal !"
            else if (isPrDur) prs += "Record — ${durationMinutes} min !"
        }

        // ── PR de 1RM : meilleurs 1RM des nouveaux exercices vs historique ────────
        val prevSets = repo.getAllExerciseSets().first()
        exercises.forEach { ex ->
            val bestNew = ex.sets.maxOfOrNull { ExerciseSet(0, 0, ex.name, 0, it.reps, it.weightKg, today).estimatedOneRepMax() } ?: 0f
            val bestOld = prevSets.filter { it.exerciseName.equals(ex.name, ignoreCase = true) }
                .maxOfOrNull { it.estimatedOneRepMax() } ?: 0f
            if (bestNew > 0f && bestNew > bestOld && bestOld > 0f) {
                prs += "PR ${ex.name} — 1RM ${bestNew.toInt()} kg !"
            }
        }
        if (prs.isNotEmpty()) _lastPrInfo.value = prs.joinToString(" · ")

        val session = WorkoutSession(
            date = today,
            name = name,
            durationMinutes = durationMinutes,
            caloriesBurned = caloriesBurned,
            category = category,
            muscleGroups = muscleGroups,
            notes = notes
        )

        // Persistance des séries
        val sets = mutableListOf<ExerciseSet>()
        exercises.forEach { ex ->
            ex.sets.forEachIndexed { i, s ->
                sets += ExerciseSet(
                    sessionId = 0,
                    exerciseName = ex.name.trim(),
                    setNumber = i + 1,
                    reps = s.reps,
                    weightKg = s.weightKg,
                    date = today
                )
            }
        }
        repo.addWorkoutWithSets(session, sets)

        updateWorkoutStreak()
    }

    fun deleteWorkout(session: WorkoutSession) = viewModelScope.launch {
        repo.deleteWorkoutWithSets(session)
    }

    /** Streak entraînement : incrément atomique (no-op si déjà compté aujourd'hui). */
    private suspend fun updateWorkoutStreak() {
        repo.recordStreakActivity("workout")
    }
}
