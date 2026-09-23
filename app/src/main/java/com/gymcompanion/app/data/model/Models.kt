package com.gymcompanion.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Entities ──────────────────────────────────────────────────────────────────

@Entity(tableName = "food_entries", indices = [Index("date"), Index("mealType")])
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val mealType: MealType,
    val isFavorite: Boolean = false,
    // ── Portion (affichage) — macros finales déjà calculées dans les champs ci-dessus ──
    val grams: Float? = null,        // poids saisi (g) si applicable
    val quantity: Float? = null,     // nombre d'unités si saisi en ×N (ex. 4 œufs)
    val unitLabel: String? = null,   // "œuf" / "tranche"… pour afficher "4 œufs"
    val unit: String = "g",          // "g" ou "ml"
    val minerals: String = "",       // ex. "300mg potassium, 50mg magnésium"
    val barcode: String = "",         // code-barres pour scan rapide
    val timestamp: Long = System.currentTimeMillis()
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

// ── Catalogue d'aliments réutilisable (valeurs pour 100 g) ─────────────────────
// Alimenté par l'API Open Food Facts (scan + recherche) — pas de liste codée en dur.
@Entity(tableName = "food_items", indices = [Index("name")])
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val gramsPerUnit: Float? = null,  // ex. 1 œuf = 50g ; null = pas d'unité discrète
    val unitLabel: String? = null,    // ex. "œuf", "tranche", "portion"
    val category: String = "",        // catégorie simplifiée issue de l'API (v7)
    val unit: String = "g",           // "g" ou "ml"
    val minerals: String = "",        // ex. "300mg potassium, 50mg magnésium"
    val barcode: String = "",         // code-barres pour scan rapide
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_sessions", indices = [Index("date"), Index("category")])
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val category: WorkoutCategory,
    val notes: String = "",
    val muscleGroups: String = "",   // comma-separated MuscleGroup names
    val timestamp: Long = System.currentTimeMillis()
)

enum class WorkoutCategory {
    STRENGTH, CARDIO, HIIT, YOGA, CYCLING, RUNNING, SWIMMING, OTHER
}

// ── Exercise sets (musculation : séries × reps × charge par exercice) ───────────
@Entity(
    tableName = "exercise_sets",
    indices = [Index("sessionId"), Index("exerciseName"), Index("date")]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,        // FK logique vers WorkoutSession.id
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val date: String,           // dénormalisé (= session.date) pour requêtes progression
    val timestamp: Long = System.currentTimeMillis()
)

// Epley : 1RM = poids × (1 + reps/30)
fun ExerciseSet.estimatedOneRepMax(): Float =
    if (reps <= 1) weightKg else weightKg * (1f + reps / 30f)

// Muscle groups for workout planner / radar chart
enum class MuscleGroup(val label: String) {
    CHEST("Pectoraux"),
    BACK("Dos"),
    SHOULDERS("Épaules"),
    ARMS("Bras"),
    ABS("Abdos"),
    LEGS("Jambes"),
    GLUTES("Fessiers"),
    CARDIO("Cardio")
}

@Entity(tableName = "step_records")
data class StepRecord(
    @PrimaryKey val date: String,
    val steps: Int,
    val goal: Int = 10000,
    val distanceKm: Float = 0f,
    val caloriesBurned: Int = 0
)

@Entity(tableName = "body_records", indices = [Index("date")])
data class BodyRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Float? = null,
    val bodyFatPercent: Float? = null,
    val muscleMassKg: Float? = null,
    val bonePercent: Float? = null,   // % of body weight (from scale)
    val waterPercent: Float? = null,  // % total body water
    val waistCm: Float? = null,
    val chestCm: Float? = null,
    val armCm: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey val type: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: String = ""
)

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey val type: String,
    val target: Float,
    val unit: String
)

// ── User profile (single row id=1) ─────────────────────────────────────────────
// Stores personal data for TDEE calculation & greeting
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val birthYear: Int = 0,
    val heightCm: Float = 0f,
    val genderCode: String = "M",        // "M" or "F"
    val activityLevel: Int = 2,          // 1=Sedentary→5=Very active
    val objectiveCode: String = "maintain" // "cut" / "bulk" / "maintain"
)

fun UserProfile.tdeeKcal(currentWeightKg: Float): Int {
    if (birthYear == 0 || heightCm == 0f) return 2000
    val age = java.time.LocalDate.now().year - birthYear
    if (age !in 10..120 || currentWeightKg <= 0f) return 2000
    // Mifflin-St Jeor BMR
    val bmr = if (genderCode == "M") {
        (10 * currentWeightKg + 6.25f * heightCm - 5 * age + 5).toInt()
    } else {
        (10 * currentWeightKg + 6.25f * heightCm - 5 * age - 161).toInt()
    }
    val multiplier = when (activityLevel) {
        1 -> 1.2f
        2 -> 1.375f
        3 -> 1.55f
        4 -> 1.725f
        5 -> 1.9f
        else -> 1.375f
    }
    val tdee = (bmr * multiplier).toInt()
    return when (objectiveCode) {
        "cut"  -> tdee - 500
        "bulk" -> tdee + 300
        else   -> tdee
    }
}

// ── Goals (checklist widget) ────────────────────────────────────────────────────
enum class GoalCategory(val label: String) {
    WORKOUT("Entraînement"),
    NUTRITION("Nutrition"),
    BODY("Corps"),
    HABIT("Habitude")
}

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val category: GoalCategory = GoalCategory.HABIT,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Nutrition summary (non-entity, DAO projection) ─────────────────────────────
data class DailyNutritionSummary(
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float
)

@Entity(tableName = "progress_photos", indices = [Index("date")])
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val imagePath: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ── Nutrition goals (user preferences) ─────────────────────────────────────────
data class NutritionGoals(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

// ── Compagnon (Tamagotchi) ─────────────────────────────────────────────────────
// Ligne unique (id=1). L'humeur dépend du respect des objectifs quotidiens.
@Entity(tableName = "pet_state")
data class PetState(
    @PrimaryKey val id: Int = 1,
    val name: String = "Pixel",
    val happiness: Int = 60,        // 0..100
    val lastEvalDate: String = ""   // dernière date d'évaluation quotidienne persistée
)

enum class PetMood { MISERABLE, SAD, NEUTRAL, HAPPY, ECSTATIC }

fun moodFor(happiness: Int): PetMood = when {
    happiness >= 85 -> PetMood.ECSTATIC
    happiness >= 65 -> PetMood.HAPPY
    happiness >= 40 -> PetMood.NEUTRAL
    happiness >= 20 -> PetMood.SAD
    else            -> PetMood.MISERABLE
}

fun PetState.mood(): PetMood = moodFor(happiness)

@Entity(tableName = "gym_visits", indices = [Index("date")])
data class GymVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val club: String,
    val date: String,
    val time: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Suivi créatine ─────────────────────────────────────────────────────────────
@Entity(tableName = "creatine_log", indices = [Index("date")])
data class CreatineLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val taken: Boolean = true,
    val grams: Float = 5.0f,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

