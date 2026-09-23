package com.gymcompanion.app.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.WorkoutViewModel
import com.gymcompanion.app.viewmodel.ExerciseDraft
import com.gymcompanion.app.viewmodel.SetDraft
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private val PAD = 24.dp

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = hiltViewModel(),
    onNavigateToAi: (String) -> Unit = {},
    onNavigateToProgress: () -> Unit = {}
) {
    val sessions by viewModel.recentWorkouts.collectAsStateWithLifecycle()
    val streak   by viewModel.workoutStreak.collectAsStateWithLifecycle()
    val lastPr   by viewModel.lastPrInfo.collectAsStateWithLifecycle()
    val setsBySession by viewModel.setsBySession.collectAsStateWithLifecycle()
    val exerciseNames by viewModel.distinctExerciseNames.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }

    // Countdown tick
    LaunchedEffect(timerRunning) {
        while (timerRunning && timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
            if (timerSeconds == 0) timerRunning = false
        }
    }

    // Auto-dismiss PR banner after 5 seconds
    LaunchedEffect(lastPr) {
        if (lastPr != null) {
            delay(5000L)
            viewModel.clearPr()
        }
    }

    val count      = sessions.size
    val totalMin   = remember(sessions) { sessions.sumOf { it.durationMinutes } }
    val totalKcal  = remember(sessions) { sessions.sumOf { it.caloriesBurned } }
    val streakCount = streak?.currentStreak ?: 0

    val muscleFrequency = remember(sessions) {
        val freq = mutableMapOf<MuscleGroup, Int>()
        sessions.take(7).forEach { s ->
            s.muscleGroups.split(",").mapNotNull { it.trim().takeIf { it.isNotBlank() } }
                .mapNotNull { runCatching { MuscleGroup.valueOf(it) }.getOrNull() }
                .forEach { mg -> freq[mg] = (freq[mg] ?: 0) + 1 }
        }
        freq
    }

    Box(Modifier.fillMaxSize().background(NothingBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item(key = "header") {
                WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ENTRAÎNEMENT · SÉANCES") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            NLabel("ENTRAÎNEMENT")
                            Spacer(Modifier.height(8.dp))
                            Text("Séances", fontFamily = LocalNumericFont.current,
                                fontWeight = FontWeight.SemiBold, fontSize = 30.sp,
                                letterSpacing = 1.sp, color = NothingWhite)
                        }
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(
                                onClick = onNavigateToProgress,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.TrendingUp, "Progression",
                                    tint = NothingGrey1, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val prompt = if (sessions.isNotEmpty()) {
                                        val s = sessions.first()
                                        "Analyse ma dernière séance : ${s.name}, ${fmtDuration(s.durationMinutes)}, ${s.caloriesBurned} kcal. Donne-moi des conseils pour progresser."
                                    } else {
                                        "Donne-moi un programme d'entraînement pour débuter."
                                    }
                                    onNavigateToAi(prompt)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, "Coach IA",
                                    tint = NothingYellow, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (streakCount > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                NLabel("SÉRIE")
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).background(NothingRed, CircleShape))
                                    Spacer(Modifier.width(7.dp))
                                    NumText("$streakCount", fontSize = 22.sp, color = NothingRed)
                                }
                            }
                        }
                    }
                }
            }

            // PR banner (F2)
            item(key = "pr_banner") {
                AnimatedVisibility(
                    visible = lastPr != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    lastPr?.let { prText ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(NothingYellow.copy(alpha = 0.12f))
                                .border(BorderStroke(1.dp, NothingYellow.copy(alpha = 0.3f)))
                                .clickable { viewModel.clearPr() }
                                .padding(horizontal = PAD, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, null,
                                tint = NothingYellow, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(prText, color = NothingYellow, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Close, null,
                                tint = NothingYellow.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            item(key = "pr_spacer") { Spacer(Modifier.height(12.dp)) }

            item(key = "summary") {
                WidgetForm(modifier = Modifier.fillMaxWidth(), title = "RÉSUMÉ · SÉANCES") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryCol("SÉANCES", "$count", Modifier.weight(1f))
                        VDivider()
                        SummaryCol("DURÉE", fmtDuration(totalMin), Modifier.weight(1f))
                        VDivider()
                        SummaryCol("KCAL", "$totalKcal", Modifier.weight(1f))
                    }
                }
            }

            item(key = "week_spacer") { Spacer(Modifier.height(12.dp)) }
            item(key = "week") {
                WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ACTIVITÉ RÉCENTE · 7 SÉANCES") {
                    GlyphSegmentBar(
                        progress = (count / 7f).coerceIn(0f, 1f),
                        color = NothingWhite, segmentCount = 7, segmentHeight = 10f
                    )
                }
            }

            if (muscleFrequency.isNotEmpty()) {
                item(key = "radar_spacer") { Spacer(Modifier.height(12.dp)) }
                item(key = "radar") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "COUVERTURE MUSCULAIRE") {
                        MuscleRadarChart(
                            frequency = muscleFrequency,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }
            }

            item(key = "hist_spacer") { Spacer(Modifier.height(12.dp)) }
            item(key = "hist") {
                WidgetForm(modifier = Modifier.fillMaxWidth(), title = "HISTORIQUE DES SÉANCES") {
                    NLabel("LISTE DES SÉANCES RÉCENTES")
                }
            }

            item(key = "hist_items_spacer") { Spacer(Modifier.height(10.dp)) }

            if (sessions.isEmpty()) {
                item(key = "empty") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "AUCUNE SÉANCE") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                            NLabel("AUCUNE SÉANCE")
                        }
                    }
                }
            } else {
                itemsIndexed(sessions, key = { _, s -> s.id }) { i, s ->
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = s.name) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SessionRow(
                                session = s,
                                sets = setsBySession[s.id] ?: emptyList(),
                                onDelete = { viewModel.deleteWorkout(s) }
                            )
                        }
                    }
                    if (i > 0 && i < sessions.size - 1) Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Rest timer FAB (F1)
        SmallFloatingActionButton(
            onClick = { showTimerDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 160.dp)
                .border(1.dp, if (timerRunning) NothingYellow.copy(alpha = 0.6f) else NothingBorderMid, RoundedCornerShape(10.dp)),
            containerColor = NothingDeep,
            contentColor = if (timerRunning) NothingYellow else NothingWhite,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = "Minuteur", modifier = Modifier.size(18.dp))
        }

        // Add session FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 108.dp)
                .border(1.dp, NothingBorderMid, RoundedCornerShape(14.dp)),
            containerColor = NothingDeep,
            contentColor = NothingWhite,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Nouvelle séance")
        }
    }

    if (showAddDialog) {
        AddWorkoutDialog(
            exerciseSuggestions = exerciseNames,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, dur, cal, cat, muscles, notes, exercises ->
                viewModel.addWorkout(name, dur, cal, cat, muscles, notes, exercises)
                showAddDialog = false
            }
        )
    }

    if (showTimerDialog) {
        RestTimerDialog(
            seconds = timerSeconds,
            running = timerRunning,
            onSetTimer = { secs ->
                timerSeconds = secs
                timerRunning = true
            },
            onStop = {
                timerRunning = false
                timerSeconds = 0
            },
            onDismiss = { showTimerDialog = false }
        )
    }
}

// ── Rest Timer Dialog (F1) ────────────────────────────────────────────────────

@Composable
private fun RestTimerDialog(
    seconds: Int,
    running: Boolean,
    onSetTimer: (Int) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    val min = seconds / 60
    val sec = seconds % 60
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = {
            Text("Minuteur de récupération",
                style = MaterialTheme.typography.titleMedium, color = NothingWhite)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumText(
                    text = "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}",
                    fontSize = 56.sp, fontWeight = FontWeight.Light,
                    color = if (running && seconds <= 10) NothingRed else NothingWhite
                )
                Spacer(Modifier.height(8.dp))
                NLabel("CHOISIR LA DURÉE")
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(30 to "30s", 60 to "1 min", 90 to "1:30", 120 to "2 min", 180 to "3 min")
                        .forEach { (secs, label) ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = running && timerMatchesPreset(seconds, secs),
                                onClick = { onSetTimer(secs) },
                                label = {
                                    Text(label, style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1)
                                }
                            )
                        }
                }
            }
        },
        confirmButton = {
            if (running) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Arrêter", color = NothingWhite) }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Fermer", color = NothingGrey2)
                }
            }
        },
        dismissButton = if (running) {
            { TextButton(onClick = onDismiss) { Text("Réduire", color = NothingGrey2) } }
        } else null
    )
}

private fun timerMatchesPreset(current: Int, preset: Int): Boolean = current in (preset - 3)..preset

// ── Muscle Radar Chart ────────────────────────────────────────────────────────

@Composable
private fun MuscleRadarChart(
    frequency: Map<MuscleGroup, Int>,
    modifier: Modifier = Modifier
) {
    val groups = remember { MuscleGroup.values().toList() }
    val maxFreq = frequency.values.maxOrNull()?.takeIf { it > 0 } ?: 1

    Column(modifier) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = minOf(size.width, size.height) / 2f - 16f
            val n = groups.size
            if (n == 0) return@Canvas
            val angleStep = (2 * Math.PI / n)

            listOf(0.33f, 0.66f, 1f).forEach { ring ->
                val pts = groups.indices.map { i ->
                    val a = -Math.PI / 2 + i * angleStep
                    Offset(cx + (maxR * ring * cos(a)).toFloat(), cy + (maxR * ring * sin(a)).toFloat())
                }
                for (i in pts.indices) {
                    drawLine(NothingBorder, pts[i], pts[(i + 1) % pts.size], strokeWidth = 1f)
                }
            }
            groups.indices.forEach { i ->
                val a = -Math.PI / 2 + i * angleStep
                drawLine(NothingBorder, Offset(cx, cy),
                    Offset(cx + (maxR * cos(a)).toFloat(), cy + (maxR * sin(a)).toFloat()), 1f)
            }
            val fillPts = groups.mapIndexed { i, mg ->
                val ratio = (frequency[mg] ?: 0).toFloat() / maxFreq
                val a = -Math.PI / 2 + i * angleStep
                Offset(cx + (maxR * ratio * cos(a)).toFloat(), cy + (maxR * ratio * sin(a)).toFloat())
            }
            val path = Path().apply {
                moveTo(fillPts[0].x, fillPts[0].y)
                fillPts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            // Valeur = trait blanc, remplissage bleu officiel (couleur sur la donnée)
            drawPath(path, NothingBlue.copy(alpha = 0.35f))
            for (i in fillPts.indices) {
                drawLine(NothingWhite, fillPts[i], fillPts[(i + 1) % fillPts.size], 1.5f)
            }
            fillPts.forEach { drawCircle(Color.White, 3f, it) }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            groups.forEach { mg ->
                val cnt = frequency[mg] ?: 0
                NLabel(
                    text = mg.label.take(4).uppercase(),
                    size = 7.sp,
                    color = if (cnt > 0) NothingWhite else NothingGrey3
                )
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SummaryCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        NumText(value, fontSize = 40.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        NLabel(label)
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(46.dp).background(NothingDivider))
}

@Composable
private fun SessionRow(
    session: WorkoutSession,
    sets: List<ExerciseSet>,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val icon = remember(session.category) { categoryIcon(session.category) }
    val muscles = remember(session.muscleGroups) {
        session.muscleGroups.split(",")
            .mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }
            .mapNotNull { runCatching { MuscleGroup.valueOf(it) }.getOrNull() }
    }
    val exercises = remember(sets) {
        sets.groupBy { it.exerciseName }
            .toList()
            .sortedBy { (_, s) -> s.minOfOrNull { it.setNumber } ?: 0 }
    }

    Column(
        Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PAD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = NothingGrey1, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(session.name, color = NothingWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                NLabel("${session.durationMinutes} MIN · ${session.date}" +
                    if (exercises.isNotEmpty()) " · ${exercises.size} EX." else "", size = 9.sp)
                if (muscles.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        muscles.take(3).forEach { mg ->
                            Box(
                                Modifier.border(1.dp, NothingBorder, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                NLabel(mg.label.take(4).uppercase(), size = 7.sp, color = NothingGrey1)
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = NothingRed, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            NumText("${session.caloriesBurned}", fontSize = 16.sp)
            Spacer(Modifier.width(3.dp))
            NLabel("KCAL", size = 8.sp)
        }

        // Détail des exercices (dépliage)
        AnimatedVisibility(visible = expanded && exercises.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(start = PAD + 36.dp, end = PAD, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEach { (exName, exSets) ->
                    val best1rm = exSets.maxOfOrNull { it.estimatedOneRepMax() } ?: 0f
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(exName, color = NothingWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            if (best1rm > 0f)
                                NLabel("1RM ~${best1rm.toInt()} KG", size = 8.sp, color = NothingGrey1)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            exSets.sortedBy { it.setNumber }
                                .joinToString("   ") { "${it.reps}×${numStr(it.weightKg)}kg" },
                            color = NothingGrey1, fontSize = 11.sp,
                            fontFamily = LocalNumericFont.current
                        )
                    }
                }
            }
        }
    }
}

private fun numStr(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", v)

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

private fun fmtDuration(min: Int): String {
    val m = min.coerceAtLeast(0)
    return "${m / 60}h${(m % 60).toString().padStart(2, '0')}"
}

// ── Add workout dialog ────────────────────────────────────────────────────────

// Modèles éditables (état Compose) pour le constructeur d'exercices
private class EditableSet(reps: String = "", kg: String = "") {
    var reps by mutableStateOf(reps)
    var kg by mutableStateOf(kg)
}
private class EditableExercise(name: String = "") {
    var name by mutableStateOf(name)
    val sets = mutableStateListOf(EditableSet())
}

@Composable
fun AddWorkoutDialog(
    exerciseSuggestions: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, WorkoutCategory, String, String, List<ExerciseDraft>) -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var duration  by remember { mutableStateOf("") }
    var calories  by remember { mutableStateOf("") }
    var notes     by remember { mutableStateOf("") }
    var category  by remember { mutableStateOf(WorkoutCategory.STRENGTH) }
    val selectedMuscles = remember { mutableStateListOf<MuscleGroup>() }
    val exercises = remember { mutableStateListOf<EditableExercise>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = { Text("Nouvelle séance", style = MaterialTheme.typography.titleMedium, color = NothingWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nom de la séance") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = nothingTextFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = duration, onValueChange = { duration = it },
                        label = { Text("Durée (min)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                    OutlinedTextField(value = calories, onValueChange = { calories = it },
                        label = { Text("Calories") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                }

                NLabel("CATÉGORIE")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WorkoutCategory.values().toList(), key = { it.name }) { cat ->
                        val catLabel = when (cat) {
                            WorkoutCategory.STRENGTH -> "Force"
                            WorkoutCategory.CARDIO   -> "Cardio"
                            WorkoutCategory.HIIT     -> "HIIT"
                            WorkoutCategory.YOGA     -> "Yoga"
                            WorkoutCategory.CYCLING  -> "Vélo"
                            WorkoutCategory.RUNNING  -> "Course"
                            WorkoutCategory.SWIMMING -> "Natation"
                            WorkoutCategory.OTHER    -> "Autre"
                        }
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                NLabel("GROUPES MUSCULAIRES")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MuscleGroup.values().toList(), key = { it.name }) { mg ->
                        val sel = mg in selectedMuscles
                        FilterChip(
                            selected = sel,
                            onClick  = { if (sel) selectedMuscles.remove(mg) else selectedMuscles.add(mg) },
                            label    = { Text(mg.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // ── EXERCICES (séries × reps × charge) ──────────────────────────
                Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
                NLabel("EXERCICES", color = NothingGrey2)
                exercises.forEachIndexed { exIdx, ex ->
                    ExerciseEditor(
                        ex = ex,
                        suggestions = exerciseSuggestions,
                        onRemove = { exercises.removeAt(exIdx) }
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .border(1.dp, NothingBorderMid, RoundedCornerShape(8.dp))
                        .clickable { exercises.add(EditableExercise()) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = NothingGrey1, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    NLabel("AJOUTER UN EXERCICE", color = NothingGrey1, size = 9.sp)
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = nothingTextFieldColors())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dur = duration.toIntOrNull()?.coerceIn(1, 1440) ?: return@Button
                    val cal = calories.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    if (name.isNotBlank()) {
                        val musclesStr = selectedMuscles.joinToString(",") { it.name }
                        val drafts = exercises.mapNotNull { ex ->
                            val exName = ex.name.trim()
                            if (exName.isBlank()) return@mapNotNull null
                            val sets = ex.sets.mapNotNull { s ->
                                val reps = s.reps.trim().toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                                val kg = s.kg.trim().replace(',', '.').toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                                SetDraft(reps, kg)
                            }
                            if (sets.isEmpty()) null else ExerciseDraft(exName, sets)
                        }
                        onConfirm(name, dur, cal, category, musclesStr, notes, drafts)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NothingBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Enregistrer", color = NothingWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = NothingGrey2) }
        }
    )
}

@Composable
private fun ExerciseEditor(
    ex: EditableExercise,
    suggestions: List<String>,
    onRemove: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ex.name, onValueChange = { ex.name = it },
                label = { Text("Exercice") }, singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                colors = nothingTextFieldColors()
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Close, "Supprimer exercice", tint = NothingGrey2, modifier = Modifier.size(16.dp))
            }
        }

        // Suggestions de noms (exercices déjà utilisés)
        val matches = remember(ex.name, suggestions) {
            if (ex.name.isBlank()) suggestions.take(6)
            else suggestions.filter { it.contains(ex.name, ignoreCase = true) && !it.equals(ex.name, ignoreCase = true) }.take(6)
        }
        if (matches.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(matches, key = { it }) { s ->
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .border(1.dp, NothingBorderMid, RoundedCornerShape(6.dp))
                            .clickable { ex.name = s }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) { Text(s.take(18), color = NothingGrey1, fontSize = 11.sp, maxLines = 1) }
                }
            }
        }

        // Séries
        ex.sets.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NLabel("S${i + 1}", size = 9.sp, color = NothingGrey2, modifier = Modifier.width(22.dp))
                OutlinedTextField(
                    value = s.reps, onValueChange = { s.reps = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Reps", fontSize = 10.sp) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = nothingTextFieldColors()
                )
                OutlinedTextField(
                    value = s.kg, onValueChange = { s.kg = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                    label = { Text("kg", fontSize = 10.sp) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = nothingTextFieldColors()
                )
                IconButton(
                    onClick = { if (ex.sets.size > 1) ex.sets.removeAt(i) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Rounded.RemoveCircleOutline, "Supprimer série",
                        tint = if (ex.sets.size > 1) NothingGrey2 else NothingGrey3,
                        modifier = Modifier.size(16.dp))
                }
            }
        }

        // 1RM estimé de la meilleure série
        val best1rm = ex.sets.mapNotNull { s ->
            val r = s.reps.toIntOrNull() ?: return@mapNotNull null
            val w = s.kg.replace(',', '.').toFloatOrNull() ?: return@mapNotNull null
            if (r <= 0 || w <= 0f) null else if (r <= 1) w else w * (1f + r / 30f)
        }.maxOrNull()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val last = ex.sets.lastOrNull()
                        ex.sets.add(EditableSet(last?.reps ?: "", last?.kg ?: ""))
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, null, tint = NothingGrey1, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                NLabel("SÉRIE", color = NothingGrey1, size = 8.sp)
            }
            if (best1rm != null && best1rm > 0f)
                NLabel("1RM ~${best1rm.toInt()} KG", color = NothingGrey1, size = 8.sp)
        }
    }
}
