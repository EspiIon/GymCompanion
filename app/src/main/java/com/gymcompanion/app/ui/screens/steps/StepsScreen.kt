package com.gymcompanion.app.ui.screens.steps

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.StepRecord
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.StepsViewModel

private val PAD = 24.dp

@Composable
fun StepsScreen(viewModel: StepsViewModel = hiltViewModel()) {
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val weekHistory by viewModel.weekHistory.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    val steps    = todayRecord?.steps ?: 0
    val goal     = todayRecord?.goal ?: 10000
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val pct      = (progress * 100).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NothingBlack),
        contentPadding = PaddingValues(bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(horizontal = PAD).padding(top = 26.dp)) {
                NLabel("ACTIVITÉ")
                Spacer(Modifier.height(8.dp))
                Text("Pas", fontFamily = LocalNumericFont.current, fontWeight = FontWeight.SemiBold,
                    fontSize = 30.sp, letterSpacing = 1.sp, color = NothingWhite)
            }
        }

        item(key = "ring") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "PAS QUOTIDIENS") {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    SegmentedArc(progress = progress, color = DataBlue, dotCount = 64,
                        modifier = Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NumText("$steps", fontSize = 64.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        NLabel("/ $goal PAS · $pct%")
                    }
                }
                Spacer(Modifier.height(18.dp))
                GlyphSegmentBar(progress = progress, color = DataBlue, segmentCount = 20,
                    segmentHeight = 7f, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
            }
        }

        item(key = "stats_spacer") { Spacer(Modifier.height(12.dp)) }
        item(key = "stats") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "STATISTIQUES") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    StatCol("KCAL", "${todayRecord?.caloriesBurned ?: 0}", Modifier.weight(1f))
                    VDivider()
                    StatCol("KM", String.format(java.util.Locale.US, "%.1f", todayRecord?.distanceKm ?: 0f), Modifier.weight(1f))
                    VDivider()
                    StatCol("OBJECTIF", "$pct%", Modifier.weight(1f))
                }
            }
        }

        item(key = "edit") {
            Spacer(Modifier.height(8.dp))
            WidgetForm(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NothingBorderMid),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite)
                ) {
                    NLabel("SAISIR MANUELLEMENT", color = NothingWhite, size = 10.sp)
                }
            }
        }

        item(key = "chart") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ACTIVITÉ SUR 7 JOURS") {
                InteractiveTrendChart(
                    values = weekHistory.take(7).reversed().map { it.steps.toFloat() },
                    labels = weekHistory.take(7).reversed().map { it.date },
                    valueFormatter = { value -> "${value.toInt()} pas" },
                    modifier = Modifier.fillMaxWidth().height(190.dp)
                )
            }
        }
    }

    if (showEditDialog) {
        StepsEditDialog(
            currentSteps = steps, currentGoal = goal,
            onDismiss = { showEditDialog = false },
            onConfirm = { s, g ->
                viewModel.updateSteps(s, g)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun StatCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        NumText(value, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        NLabel(label)
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.padding(horizontal = 16.dp).width(1.dp).height(40.dp).background(NothingDivider))
}

@Composable
private fun WeekChart(records: List<StepRecord>, modifier: Modifier = Modifier) {
    if (records.isEmpty()) return
    val maxSteps = records.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1
    val todayDate = java.time.LocalDate.now().toString()

    Row(
        modifier = modifier.fillMaxWidth().height(110.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        records.forEach { rec ->
            val ratio = rec.steps.toFloat() / maxSteps
            val goalMet = rec.steps >= rec.goal
            val isToday = rec.date == todayDate
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                NumText(if (rec.steps >= 1000) "${rec.steps / 1000}k" else "${rec.steps}",
                    fontSize = 9.sp, color = if (isToday) NothingRed else NothingGrey2)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((ratio * 70).dp.coerceAtLeast(3.dp))
                        .background(
                            when {
                                isToday  -> NothingRed
                                goalMet  -> NothingWhite
                                else     -> NothingGrey3
                            },
                            RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
                Spacer(Modifier.height(6.dp))
                NLabel(rec.date.takeLast(5), size = 7.sp,
                    color = if (isToday) NothingRed else NothingGrey2)
            }
        }
    }
}

@Composable
fun StepsEditDialog(
    currentSteps: Int, currentGoal: Int,
    onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit
) {
    var steps by remember { mutableStateOf(currentSteps.toString()) }
    var goal  by remember { mutableStateOf(currentGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = { Text("Mettre à jour les pas", style = MaterialTheme.typography.titleMedium, color = NothingWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = steps, onValueChange = { steps = it },
                    label = { Text("Nombre de pas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = nothingTextFieldColors())
                OutlinedTextField(value = goal, onValueChange = { goal = it },
                    label = { Text("Objectif journalier") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = nothingTextFieldColors())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = steps.toIntOrNull()?.coerceAtLeast(0) ?: return@Button
                    val g = goal.toIntOrNull()?.coerceAtLeast(1) ?: return@Button
                    onConfirm(s, g)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NothingDeep, contentColor = NothingWhite),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Enregistrer", color = NothingWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = NothingGrey2) }
        }
    )
}
