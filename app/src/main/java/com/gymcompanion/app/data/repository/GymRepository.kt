package com.gymcompanion.app.data.repository

import androidx.room.withTransaction
import com.gymcompanion.app.data.db.*
import com.gymcompanion.app.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymRepository @Inject constructor(
    private val db: AppDatabase,
    private val foodEntryDao: FoodEntryDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val stepRecordDao: StepRecordDao,
    private val bodyRecordDao: BodyRecordDao,
    private val streakDao: StreakDao,
    private val userProfileDao: UserProfileDao,
    private val goalDao: GoalDao,
    private val progressPhotoDao: ProgressPhotoDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val foodItemDao: FoodItemDao,
    private val petDao: PetDao,
    private val gymVisitDao: GymVisitDao,
    private val creatineLogDao: CreatineLogDao
) {
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayStr() = LocalDate.now().format(fmt)
    fun dateStr(date: LocalDate) = date.format(fmt)

    // Nutrition
    fun getFoodEntriesForDate(date: String) = foodEntryDao.getEntriesForDate(date)
    fun getDailySummary(date: String) = foodEntryDao.getDailySummary(date)
    suspend fun addFoodEntry(entry: FoodEntry) = foodEntryDao.insert(entry)
    suspend fun updateFoodEntry(entry: FoodEntry) = foodEntryDao.update(entry)
    suspend fun deleteFoodEntry(entry: FoodEntry) = foodEntryDao.delete(entry)
    fun getFoodEntriesSince(date: String) = foodEntryDao.getEntriesSince(date)
    fun getRecentDistinctFoods(limit: Int = 10) = foodEntryDao.getRecentDistinctFoods(limit)
    fun getFavoriteFoods() = foodEntryDao.getFavoriteFoods()
    suspend fun setFavoriteFood(name: String, fav: Boolean) = foodEntryDao.setFavoriteByName(name, fav)

    // Food catalogue (alimenté par l'API Open Food Facts — scan + recherche)
    fun getFoodItems() = foodItemDao.getAll()
    fun searchFoodItems(q: String) = foodItemDao.search(q)
    suspend fun addFoodItem(item: FoodItem) = foodItemDao.insert(item)

    /** Ajoute au catalogue seulement si le nom exact n'existe pas déjà (atomique). */
    suspend fun addFoodItemIfAbsent(item: FoodItem) {
        foodItemDao.upsertIfAbsent(item)
    }

    suspend fun deleteFoodItem(item: FoodItem) = foodItemDao.delete(item)

    // Compagnon (Tamagotchi)
    fun getPet(): Flow<PetState?> = petDao.getPet()
    suspend fun upsertPet(pet: PetState) = petDao.upsert(pet)

    // Workout
    fun getWorkoutsForDate(date: String) = workoutSessionDao.getSessionsForDate(date)
    fun getRecentWorkouts() = workoutSessionDao.getRecentSessions()
    suspend fun addWorkout(session: WorkoutSession) = workoutSessionDao.insert(session)
    suspend fun deleteWorkout(session: WorkoutSession) = workoutSessionDao.delete(session)
    suspend fun addWorkoutWithSets(session: WorkoutSession, sets: List<ExerciseSet>): Long =
        db.withTransaction {
            val sessionId = workoutSessionDao.insert(session)
            if (sets.isNotEmpty()) exerciseSetDao.insertAll(sets.map { it.copy(sessionId = sessionId) })
            sessionId
        }

    suspend fun deleteWorkoutWithSets(session: WorkoutSession) = db.withTransaction {
        exerciseSetDao.deleteForSession(session.id)
        workoutSessionDao.delete(session)
    }
    fun getWorkoutsSince(date: String) = workoutSessionDao.getSessionsSince(date)

    // Exercise sets (séries/reps/charge)
    suspend fun addExerciseSets(sets: List<ExerciseSet>) = exerciseSetDao.insertAll(sets)
    suspend fun deleteExerciseSetsForSession(sessionId: Long) = exerciseSetDao.deleteForSession(sessionId)
    fun getSetsForSession(sessionId: Long) = exerciseSetDao.getSetsForSession(sessionId)
    fun getSetsForExercise(name: String) = exerciseSetDao.getSetsForExercise(name)
    fun getDistinctExerciseNames() = exerciseSetDao.getDistinctExerciseNames()
    fun getAllExerciseSets() = exerciseSetDao.getAllSets()

    // Steps
    fun getStepsForDate(date: String) = stepRecordDao.getStepsForDate(date)
    fun getRecentSteps() = stepRecordDao.getRecentSteps()
    fun getStepsSince(date: String) = stepRecordDao.getStepsSince(date)
    suspend fun updateSteps(record: StepRecord) = stepRecordDao.insert(record)

    // Body
    fun getAllBodyRecords() = bodyRecordDao.getAllRecords()
    fun getLatestBodyRecord() = bodyRecordDao.getLatestRecord()
    fun getBodyRecordsSince(date: String) = bodyRecordDao.getRecordsSince(date)
    suspend fun addBodyRecord(record: BodyRecord) = bodyRecordDao.insert(record)
    suspend fun deleteBodyRecord(record: BodyRecord) = bodyRecordDao.delete(record)

    // Streaks
    fun getStreak(type: String) = streakDao.getStreak(type)

    /** Incrémente/reset/crée le streak de [type] de façon atomique. */
    suspend fun recordStreakActivity(type: String) {
        val today = LocalDate.now()
        streakDao.recordActivity(
            type = type,
            today = today.format(fmt),
            yesterday = today.minusDays(1).format(fmt)
        )
    }

    // DailyGoals (table conservée pour le backup — plus d'écriture applicative)

    // User profile
    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()
    suspend fun saveUserProfile(profile: UserProfile) = userProfileDao.upsert(profile)

    // Goals (checklist)
    fun getAllGoals(): Flow<List<Goal>> = goalDao.getAllGoals()
    fun getActiveGoals(limit: Int = 5): Flow<List<Goal>> = goalDao.getActiveGoals(limit)
    suspend fun upsertGoal(goal: Goal) = goalDao.upsert(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.delete(goal)
    suspend fun setGoalCompleted(id: Long, completed: Boolean) = goalDao.setCompleted(id, completed)

    // Progress photos
    fun getAllProgressPhotos() = progressPhotoDao.getAllPhotos()
    fun getPhotosForDate(date: String) = progressPhotoDao.getPhotosForDate(date)
    suspend fun addProgressPhoto(photo: ProgressPhoto) = progressPhotoDao.insert(photo)
    suspend fun deleteProgressPhoto(photo: ProgressPhoto) = progressPhotoDao.delete(photo)

    // Gym Visits (Basic-Fit attendance)
    fun getAllGymVisits() = gymVisitDao.getAllVisits()
    fun getLatestGymVisit() = gymVisitDao.getLatestVisit()
    suspend fun addGymVisits(visits: List<GymVisit>) = gymVisitDao.insertAll(visits)
    suspend fun addGymVisit(visit: GymVisit) = gymVisitDao.insert(visit)
    suspend fun deleteGymVisit(visit: GymVisit) = gymVisitDao.delete(visit)

    // Créatine
    fun getCreatineForDate(date: String) = creatineLogDao.getForDate(date)
    fun getRecentCreatine() = creatineLogDao.getRecent()
    suspend fun addCreatineLog(log: CreatineLog) = creatineLogDao.insert(log)
    suspend fun updateCreatineLog(log: CreatineLog) = creatineLogDao.update(log)
    suspend fun deleteCreatineLog(log: CreatineLog) = creatineLogDao.delete(log)
}
