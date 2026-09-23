package com.gymcompanion.app.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.components.AppPet
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.common.todayFlow
import com.gymcompanion.app.viewmodel.DashboardViewModel
import com.gymcompanion.app.viewmodel.GoalViewModel
import com.gymcompanion.app.viewmodel.PetUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PAD = 24.dp

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    goalVm: GoalViewModel = hiltViewModel(),
    petState: PetUiState = PetUiState(),
    onNavigateToNutrition: () -> Unit = {},
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToSteps: () -> Unit = {},
    onNavigateToBody: () -> Unit = {},
    onNavigateToAi: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToPet: () -> Unit = {},
    onNavigateToMenu: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH) }
    val todayStr by todayFlow().collectAsStateWithLifecycle(initialValue = java.time.LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val today = remember(todayStr) { java.time.LocalDate.parse(todayStr) }
    val dateStr = remember(today) { today.format(dateFormatter).uppercase() }

    val burned    = remember(state.todayWorkouts) { state.todayWorkouts.sumOf { it.caloriesBurned } }
    val remaining = (state.calorieGoal - state.todayCalories).coerceAtLeast(0)
    val calProgress  = if (state.calorieGoal > 0)
        (state.todayCalories.toFloat() / state.calorieGoal).coerceIn(0f, 1f) else 0f
    val stepProgress = if (state.stepGoal > 0)
        (state.todaySteps.toFloat() / state.stepGoal).coerceIn(0f, 1f) else 0f
    val stepPct = (stepProgress * 100).toInt()

    val greeting = remember(state.userProfile, todayStr) {
        val name = state.userProfile?.name?.takeIf { it.isNotBlank() }
        val hour = java.time.LocalTime.now().hour
        val salut = when {
            hour < 12 -> "Bonjour"
            hour < 18 -> "Bon après-midi"
            else -> "Bonsoir"
        }
        if (name != null) "$salut, $name" else salut
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NothingBlack),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // ── HEADER ──────────────────────────────────────────────────────────────
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Lbl(dateStr)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = greeting,
                        fontFamily = LocalNumericFont.current,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        letterSpacing = 0.5.sp,
                        color = NothingWhite
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Calendar quick-access
                    Box(
                        Modifier
                            .clickable(onClick = onNavigateToCalendar)
                            .border(1.dp, NothingBorderMid, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Calendrier",
                            tint = NothingGrey1,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Menu quick-access (Workout / Steps)
                    Box(
                        Modifier
                            .clickable { showMenu = true }
                            .border(1.dp, NothingBorderMid, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = "Menu",
                            tint = NothingGrey1,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // AI quick-access button
                    Box(
                        Modifier
                            .clickable(onClick = onNavigateToAi)
                            .border(1.dp, NothingYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "Coach IA",
                            tint = NothingYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (state.workoutStreak > 0) {
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Lbl("SÉRIE")
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(NothingRed, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                NumText("${state.workoutStreak}", fontSize = 20.sp, color = NothingRed)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── HERO · CALORIES ─────────────────────────────────────────────────────
        item(key = "calories") {
            WidgetForm(
                modifier = Modifier.fillMaxWidth(),
                title = "CALORIES RESTANTES",
                onClick = onNavigateToNutrition,
                pet = petState
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(128.dp)) {
                        SegmentedArc(progress = calProgress, color = DataOrange, modifier = Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NumText("${state.todayCalories}", fontSize = 34.sp)
                            Spacer(Modifier.height(4.dp))
                            Lbl("/ ${state.calorieGoal} KCAL", size = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        NumText("$remaining", fontSize = 54.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Lbl(if (burned > 0) "KCAL · $burned BRÛLÉES" else "KCAL DISPONIBLES")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // ── MACROS ──────────────────────────────────────────────────────────────
        state.todayNutrition?.let { n ->
            if (n.totalProtein + n.totalCarbs + n.totalFat > 0) {
                item(key = "macros") {
                    WidgetForm(
                        modifier = Modifier.fillMaxWidth(),
                        title = "MACROS · NUTRITION"
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            MacroCol("PROT", n.totalProtein.toInt(), "g",
                                if (state.proteinGoal > 0) n.totalProtein / state.proteinGoal else 0f, Modifier.weight(1f))
                            VDivider()
                            MacroCol("GLUC", n.totalCarbs.toInt(), "g",
                                if (state.carbsGoal > 0) n.totalCarbs / state.carbsGoal else 0f, Modifier.weight(1f))
                            VDivider()
                            MacroCol("LIP", n.totalFat.toInt(), "g",
                                if (state.fatGoal > 0) n.totalFat / state.fatGoal else 0f, Modifier.weight(1f))
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        // ── STEPS + SESSIONS ────────────────────────────────────────────────────
        item(key = "steps_sessions") {
            WidgetForm(
                modifier = Modifier.fillMaxWidth(),
                title = "ACTIVITÉ QUOTIDIENNE"
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).clickable(onClick = onNavigateToSteps)) {
                        Lbl("PAS")
                        Spacer(Modifier.height(10.dp))
                        NumText("${state.todaySteps}", fontSize = 34.sp)
                        Spacer(Modifier.height(12.dp))
                        GlyphSegmentBar(progress = stepProgress, color = DataBlue, segmentCount = 14, segmentHeight = 6f)
                        Spacer(Modifier.height(8.dp))
                        Lbl("$stepPct% · ${state.stepGoal}")
                    }
                    VDivider(height = 92.dp)
                    Column(Modifier.weight(1f).clickable(onClick = onNavigateToWorkout)) {
                        Lbl("SÉANCES · SEM")
                        Spacer(Modifier.height(10.dp))
                        NumText("${state.recentWorkouts.size}", fontSize = 34.sp)
                        Spacer(Modifier.height(12.dp))
                        GlyphSegmentBar(
                            progress = (state.recentWorkouts.size / 7f).coerceIn(0f, 1f),
                            color = DataLavender, segmentCount = 7, segmentHeight = 6f
                        )
                        Spacer(Modifier.height(8.dp))
                        Lbl("L M M J V S D")
                    }
                }
            }
        }

        // ── BODY WIDGET ─────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item(key = "body") {
            WidgetForm(
                modifier = Modifier.fillMaxWidth(),
                title = "COMPOSITION CORPORELLE",
                onClick = onNavigateToBody
            ) {
                if (state.latestBodyRecord != null) {
                    // Contenu body widget large et centré dans WidgetForm
                    BodyWidget(rec = state.latestBodyRecord!!, history = state.bodyHistory, onClick = onNavigateToBody)
                } else {
                    BodyEmptyWidget(onClick = onNavigateToBody)
                }
            }
        }

        // ── GOALS WIDGET ─────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item(key = "goals") {
            WidgetForm(
                modifier = Modifier.fillMaxWidth(),
                title = "OBJECTIFS",
                onClick = onNavigateToGoals
            ) {
                GoalsWidget(goals = state.activeGoals, goalVm = goalVm, onSeeAll = onNavigateToGoals)
            }
        }

        // ── RECENT WORKOUTS ─────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item(key = "rw_head") {
            WidgetForm(
                modifier = Modifier.fillMaxWidth(),
                title = "SÉANCES RÉCENTES"
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Lbl("SÉANCES RÉCENTES")
                    Lbl("VOIR TOUT", color = NothingGrey1)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }

        if (state.recentWorkouts.isEmpty()) {
            item(key = "rw_empty") {
                WidgetForm(
                    modifier = Modifier.fillMaxWidth(),
                    title = "SÉANCES RÉCENTES"
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Lbl("AUCUNE SÉANCE")
                    }
                }
            }
        } else {
            itemsIndexed(state.recentWorkouts.take(4), key = { _, w -> w.id }) { i, w ->
                WidgetForm(
                    modifier = Modifier.fillMaxWidth(),
                    title = "SÉANCE · ${w.name}"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = remember(w.category) { categoryIcon(w.category) }
                        Icon(icon, contentDescription = null, tint = NothingGrey1, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, color = NothingWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(3.dp))
                            Lbl("${w.durationMinutes} MIN · ${w.date}", size = 12.sp)
                        }
                        NumText("${w.caloriesBurned}", fontSize = 16.sp)
                        Spacer(Modifier.width(3.dp))
                        Lbl("KCAL", size = 12.sp)
                    }
                }
                if (i > 0 && i < 3) Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            containerColor = NothingDark,
            title = { Text("Menu", style = MaterialTheme.typography.titleMedium, color = NothingWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { showMenu = false; onNavigateToWorkout() }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = NothingWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Séances", color = NothingWhite, fontSize = 14.sp)
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { showMenu = false; onNavigateToSteps() }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.DirectionsWalk, null, tint = NothingWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Pas", color = NothingWhite, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Fermer", color = NothingGrey2)
                }
            }
        )
    }
}

// ── Building blocks ──────────────────────────────────────────────────────────

@Composable
private fun Lbl(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NothingGrey2,
    size: TextUnit = 12.sp
) = Text(
    text = text,
    modifier = modifier,
    color = color,
    fontFamily = MonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = size,
    letterSpacing = 0.8.sp,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)

@Composable
private fun VDivider(height: Dp = 40.dp) =
    Box(Modifier.padding(horizontal = 18.dp).width(1.dp).height(height).background(NothingDivider))

@Composable
private fun SolidLine() =
    Box(Modifier.fillMaxWidth().padding(horizontal = PAD).height(1.dp).background(NothingDivider))

@Composable
private fun MacroCol(label: String, value: Int, unit: String, progress: Float, modifier: Modifier = Modifier) {
    Column(modifier) {
        Lbl(label)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            NumText("$value", fontSize = 22.sp)
            Spacer(Modifier.width(2.dp))
            Lbl(unit, size = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(10.dp))
        GlyphSegmentBar(progress = progress.coerceIn(0f, 1f), color = NothingWhite, segmentCount = 7, segmentHeight = 3f)
    }
}

// ── Body empty state ──────────────────────────────────────────────────────────

@Composable
private fun BodyEmptyWidget(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = PAD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Lbl("COMPOSITION CORPORELLE")
            Spacer(Modifier.height(12.dp))
            Text(
                "Aucune donnée",
                color = NothingGrey3, fontSize = 28.sp,
                fontFamily = LocalNumericFont.current, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = NothingGrey2, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Lbl("AJOUTER MA PREMIÈRE MESURE", color = NothingGrey2)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = NothingGrey3, modifier = Modifier.size(20.dp))
    }
}

// ── Body widget ───────────────────────────────────────────────────────────────

@Composable
private fun BodyWidget(rec: BodyRecord, history: List<BodyRecord>, onClick: () -> Unit) {
    val weights = remember(history) {
        history.filter { it.weightKg != null }.sortedBy { it.date }.mapNotNull { it.weightKg }
    }
    val delta = remember(weights) {
        if (weights.size >= 2) weights.last() - weights.first() else null
    }

    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = PAD)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Lbl("COMPOSITION CORPORELLE")
            Box(Modifier.clickable(onClick = onClick).padding(2.dp)) {
                Icon(Icons.Rounded.ChevronRight, null, tint = NothingGrey3, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    rec.weightKg?.let {
                        NumText(numStr(it), fontSize = 48.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(5.dp))
                        Lbl("KG", size = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                    }
                }
                if (delta != null) {
                    Spacer(Modifier.height(4.dp))
                    val sign = if (delta <= 0f) "−" else "+"
                    Lbl("$sign${numStr(kotlin.math.abs(delta))} KG · 30 J", color = NothingGrey1)
                }
            }
            Spacer(Modifier.width(20.dp))
            if (weights.size >= 2) {
                Sparkline(
                    values = weights.takeLast(14),
                    color = NothingBlue, dotColor = NothingWhite,
                    modifier = Modifier.weight(1f).height(44.dp)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(18.dp))

        // fat / muscle / bone / water gauges
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            rec.bodyFatPercent?.let {
                BodyMetric("GRAISSE", "${numStr(it)}%", (it / 40f), Modifier.weight(1f))
            }
            rec.muscleMassKg?.let {
                if (rec.bodyFatPercent != null) VDivider(height = 40.dp)
                BodyMetric("MUSCLE", "${numStr(it)} KG", (it / 80f), Modifier.weight(1f))
            }
            rec.bonePercent?.let {
                VDivider(height = 40.dp)
                BodyMetric("OS", "${numStr(it)}%", (it / 6f), Modifier.weight(1f))
            }
            rec.waterPercent?.let {
                VDivider(height = 40.dp)
                BodyMetric("EAU", "${numStr(it)}%", (it / 70f), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BodyMetric(label: String, value: String, progress: Float, modifier: Modifier = Modifier) {
    Column(modifier) {
        Lbl(label, size = 12.sp)
        Spacer(Modifier.height(6.dp))
        NumText(value, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        GlyphSegmentBar(progress = progress.coerceIn(0f, 1f), color = NothingWhite, segmentCount = 6, segmentHeight = 3f)
    }
}

// ── Goals widget ──────────────────────────────────────────────────────────────

@Composable
private fun GoalsWidget(goals: List<Goal>, goalVm: GoalViewModel, onSeeAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = PAD)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Lbl("OBJECTIFS")
            Box(Modifier.clickable(onClick = onSeeAll).padding(2.dp)) {
                Icon(Icons.Rounded.ChevronRight, null, tint = NothingGrey3, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        if (goals.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onSeeAll).padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, null, tint = NothingGrey2, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Lbl("AJOUTER UN OBJECTIF", color = NothingGrey2)
            }
        } else {
            goals.forEach { goal ->
                GoalRow(goal = goal, onToggle = { goalVm.toggleGoal(goal) })
                Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onSeeAll).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, null, tint = NothingGrey3, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Lbl("AJOUTER / VOIR TOUT", color = NothingGrey3)
            }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox-style dot
        Box(
            Modifier
                .size(16.dp)
                .border(1.dp, if (goal.isCompleted) NothingBlue else NothingBorderMid, CircleShape)
                .background(if (goal.isCompleted) NothingBlue else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (goal.isCompleted) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = NothingWhite,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = goal.title,
                color = if (goal.isCompleted) NothingGrey2 else NothingWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(2.dp))
            Lbl(goal.category.label.uppercase(), size = 12.sp,
                color = if (goal.isCompleted) NothingGrey3 else NothingGrey2)
        }
    }
}

// ── Workout row ───────────────────────────────────────────────────────────────

@Composable
private fun WorkoutRow(w: WorkoutSession) {
    val icon = remember(w.category) { categoryIcon(w.category) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PAD, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NothingGrey1, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(w.name, color = NothingWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Lbl("${w.durationMinutes} MIN · ${w.date}", size = 12.sp)
        }
        NumText("${w.caloriesBurned}", fontSize = 16.sp)
        Spacer(Modifier.width(3.dp))
        Lbl("KCAL", size = 12.sp)
    }
}

private fun categoryIcon(c: WorkoutCategory): ImageVector = when (c) {
    WorkoutCategory.STRENGTH -> Icons.Rounded.FitnessCenter
    WorkoutCategory.CARDIO   -> Icons.Rounded.MonitorHeart
    WorkoutCategory.HIIT     -> Icons.Rounded.Bolt
    WorkoutCategory.YOGA     -> Icons.Rounded.SelfImprovement
    WorkoutCategory.CYCLING  -> Icons.Rounded.DirectionsBike
    WorkoutCategory.RUNNING  -> Icons.Rounded.DirectionsRun
    WorkoutCategory.SWIMMING -> Icons.Rounded.Pool
    WorkoutCategory.OTHER    -> Icons.Rounded.FitnessCenter
}

private fun numStr(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
