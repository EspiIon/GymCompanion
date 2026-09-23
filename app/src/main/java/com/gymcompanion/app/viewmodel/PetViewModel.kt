package com.gymcompanion.app.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.data.repository.GymRepository
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.AUTO_MACRO_GOALS_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CALORIE_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.PET_STYLE_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.PROTEIN_GOAL_PREF
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Un objectif quotidien du compagnon + son état d'atteinte. */
data class PetGoal(val label: String, val met: Boolean)

data class PetUiState(
    val name: String = "Pixel",
    val happiness: Int = 60,
    val mood: PetMood = PetMood.NEUTRAL,
    val goals: List<PetGoal> = emptyList(),
    val message: String = ""
) {
    val goalsMet get() = goals.count { it.met }
    val goalsTotal get() = goals.size
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PetViewModel @Inject constructor(
    private val repo: GymRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // Décroissance nocturne : chaque nouveau jour, le compagnon « a faim » et perd un peu d'humeur.
    private val overnightDecay = 12

    /** Écritures sérialisées : une seule coroutine consomme, pas de courses d'upsert. */
    private val petWrites = MutableSharedFlow<PetState>(extraBufferCapacity = 16)

    init {
        viewModelScope.launch {
            petWrites.collect { repo.upsertPet(it) }
        }
    }

    /** Aliments récents pour le bouton Nourrir. */
    val recentFoods: StateFlow<List<FoodEntry>> =
        repo.getRecentDistinctFoods(6)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Variante de trame du compagnon : 0=points · 1=glyph rows · 2=aucune. */
    val petStyle: StateFlow<Int> =
        dataStore.data.map { it[PET_STYLE_PREF] ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setStyle(style: Int) = viewModelScope.launch {
        dataStore.edit { it[PET_STYLE_PREF] = style.coerceIn(0, 2) }
    }

    /**
     * Nourrir : logge l'aliment en collation aujourd'hui, compte le streak
     * nutrition et ajoute +3 de bonheur.
     */
    fun feed(source: FoodEntry) = viewModelScope.launch {
        repo.addFoodEntry(
            FoodEntry(
                date = todayStr(),
                name = source.name,
                calories = source.calories,
                protein = source.protein,
                carbs = source.carbs,
                fat = source.fat,
                mealType = MealType.SNACK,
                grams = source.grams,
                quantity = source.quantity,
                unitLabel = source.unitLabel
            )
        )
        repo.recordStreakActivity("nutrition")
        val current = repo.getPet().first() ?: PetState()
        petWrites.tryEmit(
            current.copy(
                happiness = (current.happiness + 3).coerceAtMost(100),
                lastEvalDate = todayStr()
            )
        )
    }

    /** Les requêtes « du jour » suivent le passage de minuit. */
    private val todayData = todayFlow().flatMapLatest { date ->
        combine(
            repo.getDailySummary(date),
            repo.getStepsForDate(date),
            repo.getWorkoutsForDate(date)
        ) { summary, steps, workouts -> Triple(summary, steps, workouts) }
    }

    val state: StateFlow<PetUiState> = combine(
        todayData,
        combine(
            repo.getUserProfile(),
            repo.getLatestBodyRecord(),
            repo.getPet()
        ) { profile, body, pet -> Triple(profile, body, pet) },
        dataStore.data
    ) { (summary, steps, workouts), (profile, body, pet), prefs ->
        val dateStr = todayStr()

        // ── Cibles du jour (même logique que DashboardViewModel) ────────────────
        val weight      = body?.weightKg ?: 70f
        val autoMacros  = prefs[AUTO_MACRO_GOALS_PREF] ?: true
        val calorieGoal = prefs[CALORIE_GOAL_PREF] ?: (profile?.tdeeKcal(weight) ?: 2000)
        val proteinGoal = if (autoMacros) calorieGoal * 0.30f / 4f
                          else (prefs[PROTEIN_GOAL_PREF] ?: 150f)
        val stepGoal    = steps?.goal ?: 10000

        val cals      = summary?.totalCalories ?: 0
        val prot      = summary?.totalProtein ?: 0f
        val stepCount = steps?.steps ?: 0

        // ── Objectifs atteints aujourd'hui ──────────────────────────────────────
        val goals = listOf(
            PetGoal("Nutrition",    cals > 0),
            PetGoal("Entraînement", workouts.isNotEmpty()),
            PetGoal("Calories",     cals in (calorieGoal * 0.85f).toInt()..(calorieGoal * 1.10f).toInt()),
            PetGoal("Protéines",    prot >= proteinGoal * 0.9f),
            PetGoal("Pas",          stepCount >= stepGoal)
        )
        val met = goals.count { it.met }
        val scoreTarget = (met.toFloat() / goals.size * 100f).toInt()

        // ── Humeur persistée + décroissance nocturne idempotente ────────────────
        val stored  = pet ?: PetState()
        val newDay  = stored.lastEvalDate != dateStr
        val baseline = if (newDay) (stored.happiness - overnightDecay).coerceIn(0, 100)
                       else stored.happiness
        // L'humeur monte avec les objectifs du jour, sans jamais redescendre sous le plancher du jour.
        val happiness = maxOf(baseline, scoreTarget)

        // Persiste si la valeur a changé ou si on passe un nouveau jour (convergent).
        // Écriture via SharedFlow → sérialisée, jamais de boucle de re-emission bloquante.
        if (newDay || happiness != stored.happiness) {
            petWrites.tryEmit(stored.copy(happiness = happiness, lastEvalDate = dateStr))
        }

        val message = when {
            met == goals.size -> "Tous les objectifs sont remplis. Bravo !"
            met == 0          -> "Occupe-toi de moi : logge un repas ou un entraînement."
            else -> "Il te manque : " + goals.filterNot { it.met }.joinToString(", ") { it.label }
        }

        PetUiState(
            name = stored.name,
            happiness = happiness,
            mood = moodFor(happiness),
            goals = goals,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetUiState())

    fun rename(name: String) = viewModelScope.launch {
        val current = repo.getPet().first() ?: PetState()
        petWrites.tryEmit(current.copy(name = name.ifBlank { "Pixel" }))
    }

    /** Caresse : +5 bonheur (max 100), réinitialise la date pour éviter le decay immédiat. */
    fun pet() = viewModelScope.launch {
        val current = repo.getPet().first() ?: PetState()
        val boosted = (current.happiness + 5).coerceAtMost(100)
        petWrites.tryEmit(current.copy(happiness = boosted, lastEvalDate = todayStr()))
    }

    private companion object {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        fun todayStr() = java.time.LocalDate.now().format(fmt)
    }
}
