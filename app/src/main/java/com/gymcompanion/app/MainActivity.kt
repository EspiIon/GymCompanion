package com.gymcompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.ui.components.AppPetBar
import com.gymcompanion.app.ui.components.AppPetSheet
import com.gymcompanion.app.viewmodel.PetViewModel
import com.gymcompanion.app.ui.navigation.*
import com.gymcompanion.app.ui.screens.ai.AiScreen
import com.gymcompanion.app.ui.screens.body.BodyScreen
import com.gymcompanion.app.ui.screens.body.BodyDetailScreen
import com.gymcompanion.app.ui.screens.calendar.CalendarScreen
import com.gymcompanion.app.ui.screens.dashboard.DashboardScreen
import com.gymcompanion.app.ui.screens.nutrition.NutritionScreen
import com.gymcompanion.app.ui.screens.goals.GoalsScreen
import com.gymcompanion.app.ui.screens.progress.ProgressPhotoScreen
import com.gymcompanion.app.ui.screens.settings.SettingsScreen
import com.gymcompanion.app.ui.screens.steps.StepsScreen
import com.gymcompanion.app.ui.screens.workout.WorkoutScreen
import com.gymcompanion.app.ui.screens.workout.ExerciseProgressScreen
import com.gymcompanion.app.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymCompanionTheme {
                GymCompanionAppUI()
            }
        }
    }
}

@Composable
fun GymCompanionAppUI() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val petViewModel: PetViewModel = hiltViewModel()
    val petState by petViewModel.state.collectAsStateWithLifecycle()
    var showPetSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            AppPetBar(
                state = petState,
                onClick = { showPetSheet = true },
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            GymBottomNav(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // Shared helper: same popUpTo logic as bottom nav so the back stack stays clean
        fun navTo(route: String) = navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition  = { fadeOut(tween(150)) }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToNutrition = { navTo(Screen.Nutrition.route) },
                    onNavigateToWorkout   = { navTo(Screen.Workout.route) },
                    onNavigateToSteps     = { navTo(Screen.Steps.route) },
                    onNavigateToBody      = { navTo(Screen.Body.route) },
                    onNavigateToAi        = { navTo(Screen.Ai.route) },
                    onNavigateToCalendar  = { navTo(Screen.Calendar.route) },
                    onNavigateToGoals     = { navTo(Screen.Goals.route) },
                    onNavigateToPet       = { showPetSheet = true },
                    onNavigateToMenu      = { navController.navigate(Screen.Workout.route) }
                )
            }
            composable(Screen.Nutrition.route) { NutritionScreen() }
            composable(Screen.Workout.route) {
                WorkoutScreen(
                    onNavigateToAi = { message ->
                        navTo(aiRouteWithMessage(message))
                    },
                    onNavigateToProgress = { navTo(Screen.ExerciseProgress.route) }
                )
            }
            composable(Screen.ExerciseProgress.route) { ExerciseProgressScreen() }
            composable(Screen.Steps.route) { StepsScreen() }
            composable(Screen.Body.route) {
                BodyScreen(
                    onNavigateToProgress = { navTo(Screen.ProgressPhoto.route) },
                    onNavigateToDetail = { type -> navTo(bodyDetailRoute(type)) }
                )
            }
            composable(
                route = "body_detail/{type}",
                arguments = listOf(navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "muscle"
                })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "muscle"
                BodyDetailScreen(dataType = type, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.ProgressPhoto.route) { ProgressPhotoScreen() }
            composable(Screen.Goals.route) { GoalsScreen() }
            composable(
                route = "ai?message={message}",
                arguments = listOf(navArgument("message") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val msg = backStackEntry.arguments?.getString("message").orEmpty()
                AiScreen(initialMessage = msg)
            }
        }
    }

    if (showPetSheet) {
        AppPetSheet(
            state = petState,
            onDismiss = { showPetSheet = false },
            onPet = petViewModel::pet,
            onRename = petViewModel::rename,
            onVariant = petViewModel::setVariant,
            onColor = petViewModel::setColor
        )
    }
}

@Composable
fun GymBottomNav(currentRoute: String?, onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF171717))
        ) {
            NavigationBar(
                containerColor  = Color.Transparent,
                tonalElevation  = 0.dp,
                modifier        = Modifier.height(72.dp)
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route

                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier.size(width = 36.dp, height = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier
                                            .size(width = 16.dp, height = 2.5.dp)
                                            .background(
                                                color = if (isSelected) NothingRed else Color.Transparent,
                                                shape = RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }
                        },
                        label = null,
                        selected = isSelected,
                        onClick = { onNavigate(screen.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = NothingWhite,
                            unselectedIconColor = NothingGrey3,
                            indicatorColor      = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
