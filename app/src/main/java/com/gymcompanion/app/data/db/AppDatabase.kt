package com.gymcompanion.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymcompanion.app.data.model.*

/** Version unique du schéma — référencée aussi par BackupManager (meta sauvegarde). */
const val DB_VERSION = 9

@Database(
    entities = [
        FoodEntry::class,
        WorkoutSession::class,
        StepRecord::class,
        BodyRecord::class,
        Streak::class,
        DailyGoal::class,
        UserProfile::class,
        Goal::class,
        ProgressPhoto::class,
        ExerciseSet::class,
        FoodItem::class,
        PetState::class,
        GymVisit::class,
        CreatineLog::class
    ],
    version = DB_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun stepRecordDao(): StepRecordDao
    abstract fun bodyRecordDao(): BodyRecordDao
    abstract fun streakDao(): StreakDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalDao(): GoalDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun petDao(): PetDao
    abstract fun gymVisitDao(): GymVisitDao
    abstract fun creatineLogDao(): CreatineLogDao
    abstract fun backupDao(): BackupDao
}
