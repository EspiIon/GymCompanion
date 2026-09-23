package com.gymcompanion.app.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.backup.BackupManager
import com.gymcompanion.app.data.backup.BackupWorker
import com.gymcompanion.app.data.backup.DriveSyncManager
import com.gymcompanion.app.data.model.UserProfile
import com.gymcompanion.app.data.repository.GymRepository
import com.gymcompanion.app.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repo: GymRepository,
    private val dataStore: DataStore<Preferences>,
    private val backupManager: BackupManager,
    private val driveSync: DriveSyncManager
) : ViewModel() {

    companion object {
        val AI_MODEL_PREF            = stringPreferencesKey("ai_model")
        val TARGET_WEIGHT_PREF       = floatPreferencesKey("target_weight_kg")
        val REMINDER_ENABLED_PREF    = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR_PREF       = intPreferencesKey("reminder_hour")
        val REMINDER_MIN_PREF        = intPreferencesKey("reminder_min")
        val CALORIE_GOAL_PREF        = intPreferencesKey("calorie_goal")
        val PROTEIN_GOAL_PREF        = floatPreferencesKey("protein_goal")
        val CARBS_GOAL_PREF          = floatPreferencesKey("carbs_goal")
        val FAT_GOAL_PREF            = floatPreferencesKey("fat_goal")
        val AUTO_MACRO_GOALS_PREF    = booleanPreferencesKey("auto_macro_goals")
        val AUTO_BACKUP_PREF         = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_AT_PREF      = longPreferencesKey("last_backup_at")
        val PET_STYLE_PREF           = intPreferencesKey("pet_trame_style")
    }

    val userProfile: StateFlow<UserProfile?> =
        repo.getUserProfile()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiModel: StateFlow<String> =
        dataStore.data.map { it[AI_MODEL_PREF] ?: "meta-llama/llama-3.3-70b-instruct:free" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "meta-llama/llama-3.3-70b-instruct:free")

    val targetWeightKg: StateFlow<Float?> =
        dataStore.data.map { it[TARGET_WEIGHT_PREF] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminderEnabled: StateFlow<Boolean> =
        dataStore.data.map { it[REMINDER_ENABLED_PREF] ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderHour: StateFlow<Int> =
        dataStore.data.map { it[REMINDER_HOUR_PREF] ?: 20 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val reminderMin: StateFlow<Int> =
        dataStore.data.map { it[REMINDER_MIN_PREF] ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val calorieGoal: StateFlow<Int?> =
        dataStore.data.map { it[CALORIE_GOAL_PREF] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val proteinGoal: StateFlow<Float?> =
        dataStore.data.map { it[PROTEIN_GOAL_PREF] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val carbsGoal: StateFlow<Float?> =
        dataStore.data.map { it[CARBS_GOAL_PREF] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fatGoal: StateFlow<Float?> =
        dataStore.data.map { it[FAT_GOAL_PREF] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoMacroGoals: StateFlow<Boolean> =
        dataStore.data.map { it[AUTO_MACRO_GOALS_PREF] ?: true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { repo.saveUserProfile(profile) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { dataStore.edit { it[AI_MODEL_PREF] = model } }
    }

    fun setTargetWeight(kg: Float?) {
        viewModelScope.launch {
            dataStore.edit {
                if (kg != null) it[TARGET_WEIGHT_PREF] = kg
                else it.remove(TARGET_WEIGHT_PREF)
            }
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, min: Int) {
        viewModelScope.launch {
            dataStore.edit {
                it[REMINDER_ENABLED_PREF] = enabled
                it[REMINDER_HOUR_PREF] = hour
                it[REMINDER_MIN_PREF] = min
            }
            if (enabled) NotificationHelper.scheduleDaily(appContext, hour, min)
            else NotificationHelper.cancelReminder(appContext)
        }
    }

    fun setNutritionGoals(calories: Int?, protein: Float?, carbs: Float?, fat: Float?, auto: Boolean) {
        viewModelScope.launch {
            dataStore.edit {
                it[AUTO_MACRO_GOALS_PREF] = auto
                if (calories != null) it[CALORIE_GOAL_PREF] = calories
                else it.remove(CALORIE_GOAL_PREF)
                if (protein != null) it[PROTEIN_GOAL_PREF] = protein
                else it.remove(PROTEIN_GOAL_PREF)
                if (carbs != null) it[CARBS_GOAL_PREF] = carbs
                else it.remove(CARBS_GOAL_PREF)
                if (fat != null) it[FAT_GOAL_PREF] = fat
                else it.remove(FAT_GOAL_PREF)
            }
        }
    }

    // ── Sauvegarde / synchronisation ─────────────────────────────────────────────
    val autoBackup: StateFlow<Boolean> =
        dataStore.data.map { it[AUTO_BACKUP_PREF] ?: true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lastBackupAt: StateFlow<Long?> =
        dataStore.data.map { it[LAST_BACKUP_AT_PREF] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _driveEmail = MutableStateFlow(driveSync.connectedEmail())
    val driveEmail: StateFlow<String?> = _driveEmail

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage
    fun clearBackupMessage() { _backupMessage.value = null }

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy

    /** Nom de fichier suggéré pour l'export manuel. */
    fun suggestedFileName(): String =
        "gymcompanion_${java.time.LocalDate.now()}.zip"

    fun exportTo(uri: Uri) = viewModelScope.launch {
        _backupBusy.value = true
        try {
            val out = appContext.contentResolver.openOutputStream(uri)
            if (out == null) {
                _backupMessage.value = "Impossible d'ouvrir la destination"
            } else {
                out.use { backupManager.exportTo(it) }
                _backupMessage.value = "Sauvegarde exportée"
            }
        } catch (e: Exception) {
            _backupMessage.value = "Échec de l'export : ${e.message?.take(60) ?: "erreur inconnue"}"
        } finally {
            _backupBusy.value = false
        }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        _backupBusy.value = true
        try {
            val input = appContext.contentResolver.openInputStream(uri)
            if (input == null) {
                _backupMessage.value = "Impossible de lire le fichier"
            } else {
                input.use { backupManager.importFrom(it) }
                _backupMessage.value = "Sauvegarde restaurée"
            }
        } catch (e: Exception) {
            _backupMessage.value = "Échec de la restauration : ${e.message?.take(60) ?: "erreur inconnue"}"
        } finally {
            _backupBusy.value = false
        }
    }

    fun setAutoBackup(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[AUTO_BACKUP_PREF] = enabled }
        if (enabled) BackupWorker.schedule(appContext) else BackupWorker.cancel(appContext)
    }

    fun syncNow() = viewModelScope.launch {
        _backupBusy.value = true
        runCatching {
            val dir = appContext.getExternalFilesDir("backups") ?: appContext.filesDir
            val file = File(dir, "gymcompanion_backup.zip")
            backupManager.snapshotToFile(file)
            if (driveSync.isConnected()) {
                val uploaded = driveSync.uploadBackup(file)
                if (uploaded) "✓ Synchronisé sur Google Drive"
                else "Sauvegarde locale créée (échec upload Drive)"
            } else {
                "✓ Sauvegarde locale créée · Drive non connecté"
            }
        }.onSuccess { msg -> _backupMessage.value = msg }
            .onFailure { e -> _backupMessage.value = "Échec : ${e.message?.take(60) ?: "erreur inconnue"}" }
        _backupBusy.value = false
    }

    fun restoreFromDrive() = viewModelScope.launch {
        _backupBusy.value = true
        runCatching {
            val f = driveSync.downloadLatestBackup()
            if (f != null) {
                f.inputStream().use { backupManager.importFrom(it) }
                "✓ Données restaurées depuis Google Drive"
            } else {
                "Aucune sauvegarde trouvée sur Drive"
            }
        }.onSuccess { msg -> _backupMessage.value = msg }
            .onFailure { e -> _backupMessage.value = "Échec de la restauration : ${e.message?.take(60) ?: "erreur inconnue"}" }
        _backupBusy.value = false
    }

    fun driveSignInIntent(): Intent = driveSync.signInIntent()

    fun onDriveSignInResult(data: Intent?) {
        viewModelScope.launch {
            if (data == null) {
                _backupMessage.value = "Connexion annulée"
                return@launch
            }
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                _driveEmail.value = account?.email
                _backupMessage.value = if (account?.email != null)
                    "✓ Google Drive connecté · ${account.email}"
                else
                    "Connexion échouée — compte non obtenu"
            } catch (e: ApiException) {
                _driveEmail.value = driveSync.connectedEmail()
                _backupMessage.value = when (e.statusCode) {
                    10   -> "Erreur OAuth (DEVELOPER_ERROR) — vérifiez le Client ID Android et le SHA-1 dans Google Cloud Console"
                    12501 -> "Connexion annulée"
                    12500 -> "Connexion échouée — vérifiez que l'API Drive est activée dans Google Cloud Console"
                    else -> "Erreur de connexion Google (code ${e.statusCode})"
                }
            }
        }
    }

    fun signOutDrive() {
        driveSync.signOut()
        _driveEmail.value = null
        _backupMessage.value = "Google Drive déconnecté"
    }
}
