package com.gymcompanion.app.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sauvegarde automatique quotidienne : écrit un instantané dans le stockage externe
 * spécifique à l'app (survit aux mises à jour) et, si Drive est connecté, l'upload.
 *
 * Récupère ses dépendances via un Hilt [EntryPoint] (pas besoin de hilt-work).
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun backupManager(): BackupManager
        fun driveSyncManager(): DriveSyncManager
        fun dataStore(): DataStore<Preferences>
    }

    override suspend fun doWork(): Result {
        return try {
            val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
            val enabled = deps.dataStore().data.first()[androidx.datastore.preferences.core.booleanPreferencesKey("auto_backup_enabled")] ?: true
            if (!enabled) return Result.success()

            val dir = applicationContext.getExternalFilesDir("backups") ?: applicationContext.filesDir
            val file = File(dir, "gymcompanion_backup.zip")
            deps.backupManager().snapshotToFile(file)
            val password = deps.dataStore().data.first()[stringPreferencesKey("backup_password")]
            if (password != null && password.isNotBlank()) {
                val plain = file.readBytes()
                file.writeBytes(encrypt(plain, password))
            }
            if (deps.driveSyncManager().isConnected()) {
                deps.driveSyncManager().uploadBackup(file)
            }
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE = "daily_backup"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE)
        }
    }
}
