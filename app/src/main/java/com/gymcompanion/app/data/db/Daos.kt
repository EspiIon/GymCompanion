package com.gymcompanion.app.data.db

import androidx.room.*
import com.gymcompanion.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT COALESCE(SUM(calories),0) as totalCalories, COALESCE(SUM(protein),0) as totalProtein, COALESCE(SUM(carbs),0) as totalCarbs, COALESCE(SUM(fat),0) as totalFat FROM food_entries WHERE date = :date")
    fun getDailySummary(date: String): Flow<DailyNutritionSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("SELECT * FROM food_entries WHERE date >= :startDate ORDER BY date DESC")
    fun getEntriesSince(startDate: String): Flow<List<FoodEntry>>

    // ── Récents / favoris (quick-add) ──────────────────────────────────────────
    @Query("SELECT * FROM food_entries WHERE timestamp IN (SELECT MAX(timestamp) FROM food_entries GROUP BY name) ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDistinctFoods(limit: Int): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE isFavorite = 1 AND timestamp IN (SELECT MAX(timestamp) FROM food_entries WHERE isFavorite = 1 GROUP BY name) ORDER BY name ASC")
    fun getFavoriteFoods(): Flow<List<FoodEntry>>

    @Query("UPDATE food_entries SET isFavorite = :fav WHERE name = :name")
    suspend fun setFavoriteByName(name: String, fav: Boolean)
}

// ── Catalogue d'aliments réutilisable ──────────────────────────────────────────
@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAll(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :q || '%' ORDER BY name ASC")
    fun search(q: String): Flow<List<FoodItem>>

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun count(): Int

    @Query("SELECT * FROM food_items WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItem): Long

    @Transaction
    open suspend fun upsertIfAbsent(item: FoodItem) {
        if (getByName(item.name) == null) insert(item)
    }

    @Delete
    suspend fun delete(item: FoodItem)
}

// ── Compagnon (Tamagotchi) ─────────────────────────────────────────────────────
@Dao
interface PetDao {
    @Query("SELECT * FROM pet_state WHERE id = 1")
    fun getPet(): Flow<PetState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pet: PetState)
}

// ── Sauvegarde / restauration (lecture & écriture one-shot de toutes les tables) ──
@Dao
interface BackupDao {
    // Lectures
    @Query("SELECT * FROM food_entries")    suspend fun foodEntries(): List<FoodEntry>
    @Query("SELECT * FROM food_items")      suspend fun foodItems(): List<FoodItem>
    @Query("SELECT * FROM workout_sessions") suspend fun workoutSessions(): List<WorkoutSession>
    @Query("SELECT * FROM exercise_sets")   suspend fun exerciseSets(): List<ExerciseSet>
    @Query("SELECT * FROM step_records")    suspend fun stepRecords(): List<StepRecord>
    @Query("SELECT * FROM body_records")    suspend fun bodyRecords(): List<BodyRecord>
    @Query("SELECT * FROM streaks")         suspend fun streaks(): List<Streak>
    @Query("SELECT * FROM daily_goals")     suspend fun dailyGoals(): List<DailyGoal>
    @Query("SELECT * FROM user_profile")    suspend fun userProfiles(): List<UserProfile>
    @Query("SELECT * FROM goals")           suspend fun goals(): List<Goal>
    @Query("SELECT * FROM progress_photos") suspend fun progressPhotos(): List<ProgressPhoto>
    @Query("SELECT * FROM pet_state")       suspend fun petStates(): List<PetState>
    @Query("SELECT * FROM gym_visits")      suspend fun gymVisits(): List<GymVisit>
    @Query("SELECT * FROM creatine_log")    suspend fun creatineLogs(): List<CreatineLog>

    // Insertions
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFoodEntries(x: List<FoodEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFoodItems(x: List<FoodItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkoutSessions(x: List<WorkoutSession>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExerciseSets(x: List<ExerciseSet>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStepRecords(x: List<StepRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBodyRecords(x: List<BodyRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStreaks(x: List<Streak>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDailyGoals(x: List<DailyGoal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUserProfiles(x: List<UserProfile>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGoals(x: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProgressPhotos(x: List<ProgressPhoto>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPetStates(x: List<PetState>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGymVisits(x: List<GymVisit>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCreatineLogs(x: List<CreatineLog>)

    // Effacements
    @Query("DELETE FROM food_entries")    suspend fun clearFoodEntries()
    @Query("DELETE FROM food_items")      suspend fun clearFoodItems()
    @Query("DELETE FROM workout_sessions") suspend fun clearWorkoutSessions()
    @Query("DELETE FROM exercise_sets")   suspend fun clearExerciseSets()
    @Query("DELETE FROM step_records")    suspend fun clearStepRecords()
    @Query("DELETE FROM body_records")    suspend fun clearBodyRecords()
    @Query("DELETE FROM streaks")         suspend fun clearStreaks()
    @Query("DELETE FROM daily_goals")     suspend fun clearDailyGoals()
    @Query("DELETE FROM user_profile")    suspend fun clearUserProfiles()
    @Query("DELETE FROM goals")           suspend fun clearGoals()
    @Query("DELETE FROM progress_photos") suspend fun clearProgressPhotos()
    @Query("DELETE FROM pet_state")       suspend fun clearPetStates()
    @Query("DELETE FROM gym_visits")      suspend fun clearGymVisits()
    @Query("DELETE FROM creatine_log")    suspend fun clearCreatineLogs()
}

@Dao
interface ExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: ExerciseSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<ExerciseSet>)

    @Query("DELETE FROM exercise_sets WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<ExerciseSet>>

    @Query("SELECT * FROM exercise_sets WHERE exerciseName = :name ORDER BY date ASC, setNumber ASC")
    fun getSetsForExercise(name: String): Flow<List<ExerciseSet>>

    @Query("SELECT exerciseName FROM exercise_sets GROUP BY exerciseName ORDER BY MAX(timestamp) DESC")
    fun getDistinctExerciseNames(): Flow<List<String>>

    @Query("SELECT * FROM exercise_sets ORDER BY date DESC, setNumber ASC")
    fun getAllSets(): Flow<List<ExerciseSet>>
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getSessionsForDate(date: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC LIMIT 50")
    fun getRecentSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date = :date")
    suspend fun countForDate(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Delete
    suspend fun delete(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE date >= :startDate ORDER BY date DESC")
    fun getSessionsSince(startDate: String): Flow<List<WorkoutSession>>
}

@Dao
interface StepRecordDao {
    @Query("SELECT * FROM step_records WHERE date = :date")
    fun getStepsForDate(date: String): Flow<StepRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: StepRecord)

    @Query("SELECT * FROM step_records ORDER BY date DESC LIMIT 30")
    fun getRecentSteps(): Flow<List<StepRecord>>

    @Query("SELECT * FROM step_records WHERE date >= :startDate ORDER BY date DESC")
    fun getStepsSince(startDate: String): Flow<List<StepRecord>>
}

@Dao
interface BodyRecordDao {
    @Query("SELECT * FROM body_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<BodyRecord>>

    @Query("SELECT * FROM body_records ORDER BY date DESC LIMIT 1")
    fun getLatestRecord(): Flow<BodyRecord?>

    @Query("SELECT * FROM body_records WHERE date >= :startDate ORDER BY date ASC")
    fun getRecordsSince(startDate: String): Flow<List<BodyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BodyRecord): Long

    @Delete
    suspend fun delete(record: BodyRecord)
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks WHERE type = :type")
    fun getStreak(type: String): Flow<Streak?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: Streak)

    @Query(
        """UPDATE streaks SET
            currentStreak = CASE WHEN lastActivityDate = :yesterday THEN currentStreak + 1 ELSE 1 END,
            longestStreak = MAX(CASE WHEN lastActivityDate = :yesterday THEN currentStreak + 1 ELSE 1 END, longestStreak),
            lastActivityDate = :today
        WHERE type = :type AND lastActivityDate != :today"""
    )
    suspend fun recordActivityRaw(type: String, today: String, yesterday: String): Int

    @Query("SELECT COUNT(*) FROM streaks WHERE type = :type")
    suspend fun countForType(type: String): Int

    /**
     * Enregistrement d'activité atomique (pas de read-modify-write racy) :
     * - déjà enregistré aujourd'hui → no-op ;
     * - hier → incrément, sinon reset à 1 ;
     * - absent → création à 1.
     */
    @Transaction
    suspend fun recordActivity(type: String, today: String, yesterday: String) {
        val updated = recordActivityRaw(type, today, yesterday)
        if (updated == 0 && countForType(type) == 0) {
            upsert(Streak(type, 1, 1, today))
        }
    }
}

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals")
    fun getAllGoals(): Flow<List<DailyGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoal)
}

// ── NEW: UserProfileDao ────────────────────────────────────────────────────────
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)
}

// ── NEW: GoalDao ───────────────────────────────────────────────────────────────
@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, displayOrder ASC, createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY displayOrder ASC, createdAt DESC LIMIT :limit")
    fun getActiveGoals(limit: Int): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal): Long

    @Delete
    suspend fun delete(goal: Goal)

    @Query("UPDATE goals SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)
}

@Dao
interface ProgressPhotoDao {
    @Query("SELECT * FROM progress_photos ORDER BY date DESC")
    fun getAllPhotos(): Flow<List<ProgressPhoto>>

    @Query("SELECT * FROM progress_photos WHERE date = :date")
    fun getPhotosForDate(date: String): Flow<List<ProgressPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: ProgressPhoto): Long

    @Delete
    suspend fun delete(photo: ProgressPhoto)
}

@Dao
interface GymVisitDao {
    @Query("SELECT * FROM gym_visits ORDER BY timestamp DESC")
    fun getAllVisits(): Flow<List<GymVisit>>

    @Query("SELECT * FROM gym_visits ORDER BY timestamp DESC LIMIT 1")
    fun getLatestVisit(): Flow<GymVisit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: GymVisit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<GymVisit>)

    @Delete
    suspend fun delete(visit: GymVisit)

    @Query("DELETE FROM gym_visits")
    suspend fun clearAll()
}

@Dao
interface CreatineLogDao {
    @Query("SELECT * FROM creatine_log WHERE date = :date ORDER BY timestamp DESC")
    fun getForDate(date: String): Flow<List<CreatineLog>>

    @Query("SELECT * FROM creatine_log ORDER BY date DESC LIMIT 30")
    fun getRecent(): Flow<List<CreatineLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CreatineLog): Long

    @Update
    suspend fun update(log: CreatineLog)

    @Delete
    suspend fun delete(log: CreatineLog)
}
