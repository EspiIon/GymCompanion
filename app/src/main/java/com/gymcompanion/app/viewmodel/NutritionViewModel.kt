package com.gymcompanion.app.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.data.remote.OpenFoodFactsApi
import com.gymcompanion.app.data.remote.ScannedProduct
import com.gymcompanion.app.data.repository.GymRepository
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.AUTO_MACRO_GOALS_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CALORIE_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.PROTEIN_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CARBS_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.FAT_GOAL_PREF
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data object Loading : ScanState()
    data class Success(val product: ScannedProduct) : ScanState()
    data class NotFound(val barcode: String) : ScanState()
    data class Error(val message: String) : ScanState()
}

sealed class FoodSearchState {
    data object Idle : FoodSearchState()
    data object Loading : FoodSearchState()
    data class Results(val products: List<ScannedProduct>) : FoodSearchState()
    data object Empty : FoodSearchState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repo: GymRepository,
    @ApplicationContext private val appContext: Context,
    private val dataStore: DataStore<Preferences>,
    private val foodApi: OpenFoodFactsApi
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    init {
        viewModelScope.launch {
            var previousToday = LocalDate.now()
            todayFlow().collect { today ->
                val newToday = LocalDate.parse(today)
                if (_selectedDate.value == previousToday) _selectedDate.value = newToday
                previousToday = newToday
            }
        }
    }

    val entries: StateFlow<List<FoodEntry>> = _selectedDate.flatMapLatest { date ->
        repo.getFoodEntriesForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Totaux macro dérivés de [entries] — se met à jour exactement en même temps
     *  que la liste des aliments, sans dépendre d'une query d'agrégation séparée. */
    val macroTotals: StateFlow<DailyNutritionSummary> = entries.map { list ->
        DailyNutritionSummary(
            totalCalories = list.sumOf { it.calories },
            totalProtein  = list.sumOf { it.protein.toDouble() }.toFloat(),
            totalCarbs    = list.sumOf { it.carbs.toDouble() }.toFloat(),
            totalFat      = list.sumOf { it.fat.toDouble() }.toFloat()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        DailyNutritionSummary(0, 0f, 0f, 0f))

    // ── Récents / favoris (quick-add) ───────────────────────────────────────────
    val recentFoods: StateFlow<List<FoodEntry>> =
        repo.getRecentDistinctFoods(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFoods: StateFlow<List<FoodEntry>> =
        repo.getFavoriteFoods()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(name: String, fav: Boolean) = viewModelScope.launch {
        repo.setFavoriteFood(name, fav)
    }

    // ── Catalogue d'aliments (alimenté par l'API Open Food Facts) ──────────────
    val foodItems: StateFlow<List<FoodItem>> =
        repo.getFoodItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFoodItem(item: FoodItem) = viewModelScope.launch { repo.addFoodItem(item) }

    /** Sauvegarde auto d'un produit issu du scan / de la recherche en ligne. */
    fun saveToCatalogueIfAbsent(item: FoodItem) = viewModelScope.launch {
        repo.addFoodItemIfAbsent(item)
    }

    fun deleteFoodItem(item: FoodItem) = viewModelScope.launch { repo.deleteFoodItem(item) }

    // ── Recherche en ligne (OpenFoodFacts par nom) ──────────────────────────────
    private val _searchState = MutableStateFlow<FoodSearchState>(FoodSearchState.Idle)
    val searchState: StateFlow<FoodSearchState> = _searchState

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchOnline(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) { _searchState.value = FoodSearchState.Idle; return }
        _searchState.value = FoodSearchState.Loading
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350) // léger debounce
            val results = foodApi.searchByName(q)
            _searchState.value =
                if (results.isEmpty()) FoodSearchState.Empty
                else FoodSearchState.Results(results)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.value = FoodSearchState.Idle
    }

    // ── Barcode scan state ────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    fun startScanning() {
        _scanState.value = ScanState.Scanning
    }

    fun cancelScan() {
        _scanState.value = ScanState.Idle
    }

    /** Called when ML Kit detects a barcode string → look up OpenFoodFacts */
    fun onBarcodeDetected(barcode: String) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            val product = foodApi.fetchByBarcode(barcode)
            _scanState.value = if (product != null) {
                ScanState.Success(product)
            } else {
                ScanState.NotFound(barcode)
            }
        }
    }

    fun clearScanResult() {
        _scanState.value = ScanState.Idle
    }

    // ── Food entry CRUD ───────────────────────────────────────────────────────
    fun changeDate(date: LocalDate) { _selectedDate.value = date }

    val nutritionGoals: StateFlow<NutritionGoals> =
        dataStore.data.combine(
            repo.getUserProfile().combine(repo.getLatestBodyRecord()) { profile, body ->
                Pair(profile, body)
            }
        ) { prefs, (profile, body) ->
            val weight = body?.weightKg ?: 70f
            val autoMacros = prefs[AUTO_MACRO_GOALS_PREF] ?: true

            val calories = prefs[CALORIE_GOAL_PREF] ?: (profile?.tdeeKcal(weight) ?: 2000)

            if (autoMacros) {
                // Auto macros: standard 30% protein, 45% carbs, 25% fat
                val proteinCals = calories * 0.30f
                val carbsCals = calories * 0.45f
                val fatCals = calories * 0.25f
                NutritionGoals(
                    calories = calories,
                    protein = proteinCals / 4f,
                    carbs = carbsCals / 4f,
                    fat = fatCals / 9f
                )
            } else {
                NutritionGoals(
                    calories = calories,
                    protein = prefs[PROTEIN_GOAL_PREF] ?: 150f,
                    carbs = prefs[CARBS_GOAL_PREF] ?: 200f,
                    fat = prefs[FAT_GOAL_PREF] ?: 70f
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            NutritionGoals(2000, 150f, 200f, 70f))

    fun addEntry(
        name: String, calories: Int,
        protein: Float, carbs: Float, fat: Float,
        mealType: MealType,
        grams: Float? = null,
        quantity: Float? = null,
        unitLabel: String? = null,
        unit: String = "g",
        barcode: String = ""
    ) = viewModelScope.launch {
        if (calories < 0 || protein < 0f || carbs < 0f || fat < 0f) return@launch
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        repo.addFoodEntry(
            FoodEntry(
                date = dateStr,
                name = name,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                mealType = mealType,
                grams = grams,
                quantity = quantity,
                unitLabel = unitLabel,
                unit = unit,
                barcode = barcode
            )
        )
        if (dateStr == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) {
            updateNutritionStreak()
        }
    }

    fun deleteEntry(entry: FoodEntry) = viewModelScope.launch {
        repo.deleteFoodEntry(entry)
    }

    /**
     * Édition d'une entrée existante (portion / macros / repas).
     * Préserve id, date, timestamp et isFavorite ; ne touche pas au streak.
     */
    fun updateEntry(entry: FoodEntry) = viewModelScope.launch {
        repo.updateFoodEntry(entry)
    }

    /** Quick-add 1-tap : repas déduit de l'heure, portion d'origine conservée (gr/qty optionnels). */
    fun quickAdd(
        source: FoodEntry,
        grams: Float? = null,
        quantity: Float? = null
    ) = viewModelScope.launch {
        val hour = java.time.LocalTime.now().hour
        val meal = when {
            hour < 11 -> MealType.BREAKFAST
            hour < 14 -> MealType.LUNCH
            hour < 18 -> MealType.SNACK
            hour < 22 -> MealType.DINNER
            else      -> MealType.SNACK
        }
        val ratio = when {
            grams != null && (source.grams ?: 0f) > 0f -> grams / source.grams!!
            quantity != null && (source.quantity ?: 0f) > 0f -> quantity / source.quantity!!
            else -> 1f
        }
        if (!ratio.isFinite() || ratio <= 0f) return@launch
        val finalGrams = grams ?: if (quantity != null && source.quantity != null && source.quantity > 0f) {
            source.grams?.times(ratio)
        } else source.grams
        val finalQuantity = quantity ?: if (grams != null && source.grams != null && source.grams > 0f) {
            source.quantity?.times(ratio)
        } else source.quantity
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        repo.addFoodEntry(
            FoodEntry(
                date = dateStr,
                name = source.name,
                calories = (source.calories * ratio).toInt(),
                protein = source.protein * ratio,
                carbs = source.carbs * ratio,
                fat = source.fat * ratio,
                mealType = meal,
                grams = finalGrams,
                quantity = finalQuantity,
                unitLabel = source.unitLabel,
                unit = source.unit,
                barcode = source.barcode
            )
        )
        if (dateStr == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) {
            updateNutritionStreak()
        }
    }

    // ── Créatine ─────────────────────────────────────────────────────────────────
    val creatineToday: StateFlow<List<CreatineLog>> = _selectedDate.flatMapLatest { date ->
        repo.getCreatineForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCreatine(taken: Boolean = true, grams: Float = 5.0f, notes: String = "") = viewModelScope.launch {
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        repo.addCreatineLog(CreatineLog(date = dateStr, taken = taken, grams = grams, notes = notes))
    }

    /** Streak nutrition : incrément atomique (no-op si déjà compté aujourd'hui). */
    private suspend fun updateNutritionStreak() {
        repo.recordStreakActivity("nutrition")
    }
}
