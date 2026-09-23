package com.gymcompanion.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.data.importer.BasicFitImporter
import com.gymcompanion.app.data.model.BodyRecord
import com.gymcompanion.app.data.repository.GymRepository
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.TARGET_WEIGHT_PREF
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class ImportState {
    data object Idle : ImportState()
    data object Loading : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

/** Projection d'atteinte du poids cible basée sur la tendance récente. */
sealed class WeightProjection {
    /** Pas assez de données / pas de cible définie. */
    data object Insufficient : WeightProjection()
    /** La tendance s'éloigne de la cible. */
    data class WrongTrend(val targetKg: Float, val gainingAway: Boolean) : WeightProjection()
    /** Cible déjà atteinte. */
    data class Reached(val targetKg: Float) : WeightProjection()
    /** En route vers la cible. */
    data class OnTrack(
        val targetKg: Float,
        val currentKg: Float,
        val etaDate: LocalDate,
        val weeks: Int,
        val losing: Boolean      // true = perte de poids, false = prise
    ) : WeightProjection()
}

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BodyViewModel @Inject constructor(
    private val repo: GymRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val latestRecord: StateFlow<BodyRecord?> =
        repo.getLatestBodyRecord()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allRecords: StateFlow<List<BodyRecord>> =
        repo.getAllBodyRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGymVisits: StateFlow<List<com.gymcompanion.app.data.model.GymVisit>> =
        repo.getAllGymVisits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Fenêtre 30 jours recalculée au fil de l'eau (suit le changement de jour). */
    private val last30Flow = todayFlow().flatMapLatest { today ->
        val since = LocalDate.parse(today).minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
        repo.getBodyRecordsSince(since)
    }

    val last30Days: StateFlow<List<BodyRecord>> =
        last30Flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Projection d'objectif poids ─────────────────────────────────────────────
    val weightProjection: StateFlow<WeightProjection> =
        combine(
            last30Flow,
            dataStore.data.map { it[TARGET_WEIGHT_PREF] }
        ) { records, target ->
            computeProjection(records, target)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightProjection.Insufficient)

    private fun computeProjection(records: List<BodyRecord>, targetKg: Float?): WeightProjection {
        if (targetKg == null) return WeightProjection.Insufficient
        // Points (jour epoch, poids) triés par date
        val points = records
            .mapNotNull { r -> r.weightKg?.let { LocalDate.parse(r.date).toEpochDay() to it } }
            .sortedBy { it.first }
        if (points.size < 2) return WeightProjection.Insufficient

        val currentKg = points.last().second
        // Déjà atteint (à 0.3 kg près)
        if (kotlin.math.abs(currentKg - targetKg) <= 0.3f) return WeightProjection.Reached(targetKg)

        // Régression linéaire (moindres carrés) : poids = a + b·jour
        val n = points.size
        val xs = points.map { it.first.toDouble() }
        val ys = points.map { it.second.toDouble() }
        val meanX = xs.average(); val meanY = ys.average()
        var sxx = 0.0; var sxy = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - meanX
            sxx += dx * dx
            sxy += dx * (ys[i] - meanY)
        }
        if (sxx == 0.0) return WeightProjection.Insufficient
        val slopeKgPerDay = sxy / sxx   // > 0 = prise, < 0 = perte

        val needLoss = targetKg < currentKg
        // Pente trop plate pour projeter
        if (kotlin.math.abs(slopeKgPerDay) < 0.0015) // ~ <0.01 kg/semaine
            return WeightProjection.WrongTrend(targetKg, gainingAway = !needLoss)

        val movingTowardTarget = (needLoss && slopeKgPerDay < 0) || (!needLoss && slopeKgPerDay > 0)
        if (!movingTowardTarget)
            return WeightProjection.WrongTrend(targetKg, gainingAway = slopeKgPerDay > 0)

        val days = ((targetKg - currentKg) / slopeKgPerDay).toLong().coerceIn(1, 3650)
        val eta = LocalDate.now().plusDays(days)
        val weeks = ((days + 3) / 7).toInt().coerceAtLeast(1)
        return WeightProjection.OnTrack(
            targetKg = targetKg,
            currentKg = currentKg,
            etaDate = eta,
            weeks = weeks,
            losing = needLoss
        )
    }

    // ── Basic Fit import ──────────────────────────────────────────────────────
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    fun importFromBasicFit(context: Context, uri: Uri) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                BasicFitImporter.import(context, uri)
            }
            if (result.records.isEmpty() && result.visits.isEmpty()) {
                val msg = result.errors.firstOrNull() ?: "Aucune donnée trouvée"
                _importState.value = ImportState.Error(msg)
            } else {
                result.records.forEach { repo.addBodyRecord(it) }
                if (result.visits.isNotEmpty()) {
                    repo.addGymVisits(result.visits)
                }
                val total = result.records.size + result.visits.size
                _importState.value = ImportState.Success(total)
            }
        }
    }

    fun clearImportState() {
        _importState.value = ImportState.Idle
    }

    // ── Manual record ─────────────────────────────────────────────────────────
    fun addRecord(
        weightKg: Float? = null,
        bodyFatPercent: Float? = null,
        muscleMassKg: Float? = null,
        bonePercent: Float? = null,
        waterPercent: Float? = null,
        waistCm: Float? = null,
        chestCm: Float? = null,
        armCm: Float? = null
    ) = viewModelScope.launch {
        repo.addBodyRecord(
            BodyRecord(
                date           = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                weightKg       = weightKg,
                bodyFatPercent = bodyFatPercent,
                muscleMassKg   = muscleMassKg,
                bonePercent    = bonePercent,
                waterPercent   = waterPercent,
                waistCm        = waistCm,
                chestCm        = chestCm,
                armCm          = armCm
            )
        )
    }

    fun deleteRecord(record: BodyRecord) = viewModelScope.launch {
        repo.deleteBodyRecord(record)
    }
}
