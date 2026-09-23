package com.gymcompanion.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Accueil",    Icons.Rounded.Home)
    object Nutrition : Screen("nutrition", "Nutrition",  Icons.Rounded.Restaurant)
    object Workout   : Screen("workout",   "Séances",    Icons.Rounded.FitnessCenter)
    object Steps     : Screen("steps",     "Pas",        Icons.Rounded.DirectionsWalk)
    object Body      : Screen("body",      "Corps",      Icons.Rounded.MonitorWeight)
    object Ai        : Screen("ai",        "Coach IA",   Icons.Rounded.AutoAwesome)
    object Settings  : Screen("settings",  "Config",     Icons.Rounded.Settings)
    object Calendar  : Screen("calendar",  "Calendrier", Icons.Rounded.CalendarMonth)
    object ProgressPhoto : Screen("progress_photo", "Photos",      Icons.Rounded.PhotoCameraBack)
    object Goals : Screen("goals", "Objectifs", Icons.Rounded.CheckCircle)
    object ExerciseProgress : Screen("exercise_progress", "Progression", Icons.Rounded.TrendingUp)
    object BodyDetail : Screen("body_detail/{type}", "Détail Corps", Icons.Rounded.MonitorWeight)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Nutrition,
    Screen.Body,
    Screen.ProgressPhoto,
    Screen.Settings
)

fun aiRouteWithMessage(message: String): String =
    "ai?message=${android.net.Uri.encode(message)}"

fun bodyDetailRoute(type: String): String = "body_detail/$type"
