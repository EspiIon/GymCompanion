package com.gymcompanion.app.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.gymcompanion.app.data.db.AppDatabase
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.AI_MODEL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.AUTO_MACRO_GOALS_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CALORIE_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.CARBS_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.FAT_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.LAST_BACKUP_AT_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.PROTEIN_GOAL_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.REMINDER_ENABLED_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.REMINDER_HOUR_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.REMINDER_MIN_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.TARGET_WEIGHT_PREF
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// ── Modèle sérialisable de la sauvegarde ───────────────────────────────────────
data class BackupMeta(
    val schemaVersion: Int,
    val appVersionCode: Long,
    val appVersionName: String,
    val exportedAt: Long
)

data class BackupSettings(
    val aiModel: String? = null,
    val targetWeightKg: Float? = null,
    val reminderEnabled: Boolean? = null,
    val reminderHour: Int? = null,
    val reminderMin: Int? = null,
    val calorieGoal: Int? = null,
    val proteinGoal: Float? = null,
    val carbsGoal: Float? = null,
    val fatGoal: Float? = null,
    val autoMacroGoals: Boolean? = null
)

data class BackupData(
    val meta: BackupMeta,
    val settings: BackupSettings,
    val foodEntries: List<FoodEntry> = emptyList(),
    val foodItems: List<FoodItem> = emptyList(),
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val exerciseSets: List<ExerciseSet> = emptyList(),
    val stepRecords: List<StepRecord> = emptyList(),
    val bodyRecords: List<BodyRecord> = emptyList(),
    val streaks: List<Streak> = emptyList(),
    val dailyGoals: List<DailyGoal> = emptyList(),
    val userProfiles: List<UserProfile> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val progressPhotos: List<ProgressPhoto> = emptyList(),
    val petStates: List<PetState> = emptyList(),
    val gymVisits: List<GymVisit> = emptyList(),
    val creatineLogs: List<CreatineLog> = emptyList()
)

/**
 * Sauvegarde / restauration de TOUTES les données utilisateur dans un fichier `.zip`
 * autonome : `backup.json` (toutes les tables + réglages + méta) + dossier `photos/`.
 *
 * Les photos sont stockées par nom de fichier (pas de chemin absolu) ; à la restauration
 * elles sont recopiées dans `filesDir/progress_photos` et `imagePath` est réécrit.
 */
@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val dataStore: DataStore<Preferences>
) {
    private val gson = Gson()

    private fun photoDir(): File = File(context.filesDir, "progress_photos").apply { mkdirs() }

    // ── Export ──────────────────────────────────────────────────────────────────
    suspend fun exportTo(out: OutputStream) = withContext(Dispatchers.IO) {
        val d = db.backupDao()
        val photos = d.progressPhotos()
        val photoFiles = photos.map { File(it.imagePath) }
        require(photoFiles.all { it.isFile }) { "Une photo de progression est introuvable" }
        require(photoFiles.map { it.name }.distinct().size == photoFiles.size) {
            "Plusieurs photos ont le même nom de fichier"
        }
        // Dans le JSON, imagePath = nom de fichier seul.
        val photosForJson = photos.map { it.copy(imagePath = File(it.imagePath).name) }

        val data = BackupData(
            meta = buildMeta(),
            settings = readSettings(),
            foodEntries = d.foodEntries(),
            foodItems = d.foodItems(),
            workoutSessions = d.workoutSessions(),
            exerciseSets = d.exerciseSets(),
            stepRecords = d.stepRecords(),
            bodyRecords = d.bodyRecords(),
            streaks = d.streaks(),
            dailyGoals = d.dailyGoals(),
            userProfiles = d.userProfiles(),
            goals = d.goals(),
            progressPhotos = photosForJson,
            petStates = d.petStates(),
            gymVisits = d.gymVisits(),
            creatineLogs = d.creatineLogs()
        )

        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(gson.toJson(data).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            photoFiles.forEach { f ->
                zos.putNextEntry(ZipEntry("photos/${f.name}"))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        markBackupNow()
    }

    /** Écrit un instantané dans [file] (sauvegarde auto / Drive). */
    suspend fun snapshotToFile(file: File) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.outputStream().use { exportTo(it) }
    }

    // ── Import / restauration ────────────────────────────────────────────────────
    suspend fun importFrom(input: InputStream) = withContext(Dispatchers.IO) {
        val password = dataStore.data.first()[stringPreferencesKey("backup_password")]
        val archiveInput: InputStream = if (!password.isNullOrBlank()) {
            val bytes = input.readBytes()
            ByteArrayInputStream(try { decrypt(bytes, password) } catch (_: Exception) { bytes })
        } else input
        val stage = Files.createTempDirectory(context.cacheDir.toPath(), "restore_").toFile()
        val copiedPhotos = mutableListOf<File>()
        var databaseRestored = false
        try {
            var jsonBytes: ByteArray? = null
            var extractedBytes = 0L
            var entryCount = 0
            val stagedPhotos = mutableMapOf<String, File>()
            ZipInputStream(archiveInput).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    require(entryCount <= 10_000) { "Sauvegarde invalide : trop de fichiers" }
                    val name = entry.name
                    if (name == "backup.json") {
                        require(jsonBytes == null) { "Sauvegarde invalide : plusieurs fichiers de données" }
                        val out = ByteArrayOutputStream()
                        extractedBytes += copyEntry(zis, out, 16L * 1024 * 1024)
                        jsonBytes = out.toByteArray()
                    } else if (name.startsWith("photos/") && !entry.isDirectory) {
                        val base = name.substringAfter("photos/")
                        require(safePhotoName(base) && base !in stagedPhotos) {
                            "Sauvegarde invalide : nom de photo incorrect"
                        }
                        val staged = File(stage, base)
                        staged.outputStream().use {
                            extractedBytes += copyEntry(zis, it, 50L * 1024 * 1024)
                        }
                        stagedPhotos[base] = staged
                    } else if (!(entry.isDirectory && name == "photos/")) {
                        throw IllegalArgumentException("Sauvegarde invalide : entrée inattendue")
                    }
                    require(extractedBytes <= 1024L * 1024 * 1024) {
                        "Sauvegarde trop volumineuse"
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val json = requireNotNull(jsonBytes) { "Sauvegarde invalide : backup.json absent" }
                .toString(Charsets.UTF_8)
            val root = JsonParser.parseString(json).asJsonObject
            val meta = root.getAsJsonObject("meta")
                ?: throw IllegalArgumentException("Sauvegarde invalide : métadonnées absentes")
            val version = meta.get("schemaVersion")?.asInt
                ?: throw IllegalArgumentException("Sauvegarde invalide : version absente")
            require(version in 6..com.gymcompanion.app.data.db.DB_VERSION) {
                "Version de sauvegarde non prise en charge : $version"
            }
            require(root.has("settings") && root.has("foodEntries") && root.has("workoutSessions")) {
                "Sauvegarde invalide : données essentielles absentes"
            }
            require(root.getAsJsonObject("settings") != null) {
                "Sauvegarde invalide : réglages incorrects"
            }
            // Les anciennes versions ne contiennent pas les tables ajoutées ensuite.
            listOf(
                "foodEntries", "foodItems", "workoutSessions", "exerciseSets", "stepRecords",
                "bodyRecords", "streaks", "dailyGoals", "userProfiles", "goals",
                "progressPhotos", "petStates", "gymVisits", "creatineLogs"
            ).forEach { field ->
                if (!root.has(field)) root.add(field, JsonArray())
                require(root.getAsJsonArray(field) != null) {
                    "Sauvegarde invalide : $field incorrect"
                }
            }
            val data = gson.fromJson(root, BackupData::class.java)
                ?: throw IllegalArgumentException("Sauvegarde invalide")
            val dir = photoDir()
            val restoredPhotos = data.progressPhotos.map { photo ->
                val base = photo.imagePath
                require(safePhotoName(base) && stagedPhotos.containsKey(base)) {
                    "Sauvegarde invalide : photo manquante ou nom incorrect"
                }
                val target = File(dir, "restore_${UUID.randomUUID()}_$base")
                photo.copy(imagePath = target.absolutePath)
            }

            val oldPhotos = db.backupDao().progressPhotos()
            restoredPhotos.forEachIndexed { index, photo ->
                val target = File(photo.imagePath)
                copiedPhotos += target
                stagedPhotos.getValue(data.progressPhotos[index].imagePath).copyTo(target)
            }

            db.withTransaction {
                val d = db.backupDao()
                d.clearFoodEntries(); d.clearFoodItems(); d.clearWorkoutSessions()
                d.clearExerciseSets(); d.clearStepRecords(); d.clearBodyRecords()
                d.clearStreaks(); d.clearDailyGoals(); d.clearUserProfiles()
                d.clearGoals(); d.clearProgressPhotos(); d.clearPetStates(); d.clearGymVisits()
                d.clearCreatineLogs()

                d.insertFoodEntries(data.foodEntries)
                d.insertFoodItems(data.foodItems)
                d.insertWorkoutSessions(data.workoutSessions)
                d.insertExerciseSets(data.exerciseSets)
                d.insertStepRecords(data.stepRecords)
                d.insertBodyRecords(data.bodyRecords)
                d.insertStreaks(data.streaks)
                d.insertDailyGoals(data.dailyGoals)
                d.insertUserProfiles(data.userProfiles)
                d.insertGoals(data.goals)
                d.insertProgressPhotos(restoredPhotos)
                d.insertPetStates(data.petStates)
                d.insertGymVisits(data.gymVisits)
                d.insertCreatineLogs(data.creatineLogs)
            }
            databaseRestored = true
            applySettings(data.settings)
            oldPhotos.forEach { photo ->
                File(photo.imagePath).takeIf { it.parentFile == dir }?.delete()
            }
        } catch (e: Exception) {
            if (!databaseRestored) copiedPhotos.forEach { it.delete() }
            throw e
        } finally {
            stage.deleteRecursively()
        }
    }

    // ── Réglages (DataStore) ─────────────────────────────────────────────────────
    private suspend fun readSettings(): BackupSettings {
        val p = dataStore.data.first()
        return BackupSettings(
            aiModel = p[AI_MODEL_PREF],
            targetWeightKg = p[TARGET_WEIGHT_PREF],
            reminderEnabled = p[REMINDER_ENABLED_PREF],
            reminderHour = p[REMINDER_HOUR_PREF],
            reminderMin = p[REMINDER_MIN_PREF],
            calorieGoal = p[CALORIE_GOAL_PREF],
            proteinGoal = p[PROTEIN_GOAL_PREF],
            carbsGoal = p[CARBS_GOAL_PREF],
            fatGoal = p[FAT_GOAL_PREF],
            autoMacroGoals = p[AUTO_MACRO_GOALS_PREF]
        )
    }

    private suspend fun applySettings(s: BackupSettings) {
        dataStore.edit { p ->
            s.aiModel?.let { p[AI_MODEL_PREF] = it }
            s.targetWeightKg?.let { p[TARGET_WEIGHT_PREF] = it }
            s.reminderEnabled?.let { p[REMINDER_ENABLED_PREF] = it }
            s.reminderHour?.let { p[REMINDER_HOUR_PREF] = it }
            s.reminderMin?.let { p[REMINDER_MIN_PREF] = it }
            s.calorieGoal?.let { p[CALORIE_GOAL_PREF] = it }
            s.proteinGoal?.let { p[PROTEIN_GOAL_PREF] = it }
            s.carbsGoal?.let { p[CARBS_GOAL_PREF] = it }
            s.fatGoal?.let { p[FAT_GOAL_PREF] = it }
            s.autoMacroGoals?.let { p[AUTO_MACRO_GOALS_PREF] = it }
        }
    }

    private suspend fun markBackupNow() {
        dataStore.edit { it[LAST_BACKUP_AT_PREF] = System.currentTimeMillis() }
    }

    private fun buildMeta(): BackupMeta {
        val (code, name) = try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            (pi.longVersionCode) to (pi.versionName ?: "")
        } catch (_: Exception) { 0L to "" }
        return BackupMeta(
            schemaVersion = com.gymcompanion.app.data.db.DB_VERSION,
            appVersionCode = code,
            appVersionName = name,
            exportedAt = System.currentTimeMillis()
        )
    }

    /** Neutralise tout chemin : ne garde que le nom de fichier (anti Zip-Slip). */
    private fun sanitizedFileName(path: String): String = File(path).name

    private fun safePhotoName(name: String): Boolean =
        name == sanitizedFileName(name) && name.matches(Regex("[A-Za-z0-9._-]+")) &&
            name != "." && name != ".."

    private fun copyEntry(input: InputStream, output: OutputStream, limit: Long): Long {
        val buffer = ByteArray(8192)
        var copied = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            copied += count
            require(copied <= limit) { "Entrée de sauvegarde trop volumineuse" }
            output.write(buffer, 0, count)
        }
        return copied
    }
}
