package com.gymcompanion.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.model.ExerciseSet
import com.gymcompanion.app.data.model.estimatedOneRepMax
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/** Résumé d'un exercice pour la liste de progression. */
data class ExerciseSummary(
    val name: String,
    val best1rm: Float,
    val sessionCount: Int,
    val oneRmTrend: List<Float>   // 1RM par séance (date asc) pour mini-sparkline
)

/** Détail d'un exercice sélectionné. */
data class ExerciseDetail(
    val name: String,
    val oneRmByDate: List<Float>,   // 1RM (meilleure série) par date, asc
    val best1rm: Float,
    val bestSetLabel: String,       // ex. "85 kg × 5"
    val totalVolumeKg: Float,
    val sessionCount: Int
)

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    private val repo: GymRepository
) : ViewModel() {

    /** Tous les exercices avec leur meilleur 1RM + tendance. */
    val exercises: StateFlow<List<ExerciseSummary>> =
        repo.getAllExerciseSets()
            .map { all -> buildSummaries(all) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildSummaries(all: List<ExerciseSet>): List<ExerciseSummary> =
        all.groupBy { it.exerciseName }
            .map { (name, sets) ->
                val byDate = sets.groupBy { it.date }.toSortedMap()
                val trend = byDate.map { (_, daySets) -> daySets.maxOf { it.estimatedOneRepMax() } }
                ExerciseSummary(
                    name = name,
                    best1rm = sets.maxOf { it.estimatedOneRepMax() },
                    sessionCount = byDate.size,
                    oneRmTrend = trend
                )
            }
            .sortedByDescending { it.best1rm }

    /** Détail d'un exercice sélectionné. Le nom est défini via [selectExercise]. */
    val selectedExerciseName = MutableStateFlow<String?>(null)

    val exerciseDetail: StateFlow<ExerciseDetail?> =
        selectedExerciseName.flatMapLatest { name ->
            if (name == null) flowOf(null)
            else repo.getAllExerciseSets().map { all -> buildDetail(name, all.filter { it.exerciseName == name }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectExercise(name: String?) {
        selectedExerciseName.value = name
    }

    fun detailFor(name: String): Flow<ExerciseDetail?> =
        repo.getAllExerciseSets().map { all ->
            buildDetail(name, all.filter { it.exerciseName == name })
        }

    private fun buildDetail(name: String, sets: List<ExerciseSet>): ExerciseDetail? {
        if (sets.isEmpty()) return null
        val byDate = sets.groupBy { it.date }.toSortedMap()
        val oneRmByDate = byDate.map { (_, daySets) -> daySets.maxOf { it.estimatedOneRepMax() } }
        val bestSet = sets.maxByOrNull { it.estimatedOneRepMax() }!!
        return ExerciseDetail(
            name = name,
            oneRmByDate = oneRmByDate,
            best1rm = bestSet.estimatedOneRepMax(),
            bestSetLabel = "${numStr(bestSet.weightKg)} kg × ${bestSet.reps}",
            totalVolumeKg = sets.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat(),
            sessionCount = byDate.size
        )
    }

    private fun numStr(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString()
        else String.format(java.util.Locale.US, "%.1f", v)
}
