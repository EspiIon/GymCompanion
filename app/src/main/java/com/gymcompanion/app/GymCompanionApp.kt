package com.gymcompanion.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.gymcompanion.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "gymcompanion_settings")

@HiltAndroidApp
class GymCompanionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.gymcompanion.app.notification.NotificationHelper.createChannel(this)
            com.gymcompanion.app.data.backup.BackupWorker.schedule(this)
            // Le worker capteur ne persiste pas encore les pas : arrêter les tâches anciennes.
            androidx.work.WorkManager.getInstance(this).cancelUniqueWork("step_sync")
        } catch (_: Exception) {
            // Échec non bloquant au démarrage : le channel / backup ne sont pas critiques
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(app, AppDatabase::class.java, "gymcompanion.db")
            // Vraies migrations → aucune perte de données lors des mises à jour.
            .addMigrations(*com.gymcompanion.app.data.db.AppMigrations.ALL)
            // Filet de sécurité uniquement en cas de downgrade (dev).
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()

    @Provides @Singleton fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    /** Client HTTP partagé (Open Food Facts, Coach IA…) — timeouts garantis. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton fun provideFoodEntryDao(db: AppDatabase) = db.foodEntryDao()
    @Provides @Singleton fun provideWorkoutSessionDao(db: AppDatabase) = db.workoutSessionDao()
    @Provides @Singleton fun provideStepRecordDao(db: AppDatabase) = db.stepRecordDao()
    @Provides @Singleton fun provideBodyRecordDao(db: AppDatabase) = db.bodyRecordDao()
    @Provides @Singleton fun provideStreakDao(db: AppDatabase) = db.streakDao()
    @Provides @Singleton fun provideUserProfileDao(db: AppDatabase) = db.userProfileDao()
    @Provides @Singleton fun provideGoalDao(db: AppDatabase) = db.goalDao()
    @Provides @Singleton fun provideProgressPhotoDao(db: AppDatabase) = db.progressPhotoDao()
    @Provides @Singleton fun provideExerciseSetDao(db: AppDatabase) = db.exerciseSetDao()
    @Provides @Singleton fun provideFoodItemDao(db: AppDatabase) = db.foodItemDao()
    @Provides @Singleton fun providePetDao(db: AppDatabase) = db.petDao()
    @Provides @Singleton fun provideGymVisitDao(db: AppDatabase) = db.gymVisitDao()
    @Provides @Singleton fun provideBackupDao(db: AppDatabase) = db.backupDao()
    @Provides @Singleton fun provideCreatineLogDao(db: AppDatabase) = db.creatineLogDao()
}
