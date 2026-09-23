package com.gymcompanion.app.ui.screens.goals

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.Goal
import com.gymcompanion.app.data.model.GoalCategory
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.GoalViewModel

private val PAD = 24.dp

@Composable
fun GoalsScreen(viewModel: GoalViewModel = hiltViewModel()) {
    val allGoals by viewModel.allGoals.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<GoalCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(allGoals, selectedCategory) {
        if (selectedCategory == null) allGoals
        else allGoals.filter { it.category == selectedCategory }
    }

    val done = filtered.count { it.isCompleted }
    val total = filtered.size

    Box(Modifier.fillMaxSize().background(NothingBlack)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {

            // ── Header ──────────────────────────────────────────────────────
            item(key = "header") {
                WidgetForm(modifier = Modifier.fillMaxWidth(), title = "MES OBJECTIFS") {
                    Text(
                        text = "Mes objectifs",
                        fontFamily = LocalNumericFont.current,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        letterSpacing = 0.5.sp,
                        color = NothingWhite
                    )
                }
            }

            item { Spacer(Modifier.height(10.dp)) }

            // ── Progress ring ────────────────────────────────────────────────
            if (total > 0) {
                item(key = "progress") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "PROGRESSION · OBJECTIFS") {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                SegmentedArc(
                                    progress = if (total > 0) done.toFloat() / total else 0f,
                                    color = NothingBlue,
                                    dotCount = 36,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NumText("$done", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    NLabel("/$total", size = 7.sp, color = NothingGrey2)
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text(
                                    "$done objectif${if (done > 1) "s" else ""} accompli${if (done > 1) "s" else ""}",
                                    color = NothingWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                NLabel("${total - done} RESTANT${if (total - done > 1) "S" else ""}", color = NothingGrey2)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(10.dp)) }

            // ── Category filters ─────────────────────────────────────────────
            item(key = "filters") {
                WidgetForm(modifier = Modifier.fillMaxWidth(), title = "FILTRES") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Tous", style = MaterialTheme.typography.labelSmall) }
                        )
                        GoalCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(10.dp)) }

            // ── Empty state ──────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "AUCUN OBJECTIF") {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircleOutline, null,
                                tint = NothingGrey3, modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            NLabel("AUCUN OBJECTIF", color = NothingGrey2)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Appuyez sur + pour ajouter votre premier objectif",
                                color = NothingGrey3, fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Goals list ───────────────────────────────────────────────────
            val active = filtered.filter { !it.isCompleted }
            val completed = filtered.filter { it.isCompleted }

            if (active.isNotEmpty()) {
                item(key = "active_head") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "EN COURS") {
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(active, key = { "a_${it.id}" }) { goal ->
                    WidgetForm(modifier = Modifier.fillMaxWidth()) {
                        GoalItem(
                            goal = goal,
                            onToggle = { viewModel.toggleGoal(goal) },
                            onDelete = { viewModel.deleteGoal(goal) }
                        )
                    }
                }
            }

            if (completed.isNotEmpty()) {
                item { Spacer(Modifier.height(10.dp)) }
                item(key = "done_head") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ACCOMPLIS") {
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(completed, key = { "c_${it.id}" }) { goal ->
                    WidgetForm(modifier = Modifier.fillMaxWidth()) {
                        GoalItem(
                            goal = goal,
                            onToggle = { viewModel.toggleGoal(goal) },
                            onDelete = { viewModel.deleteGoal(goal) }
                        )
                    }
                }
            }
        }

        // ── FAB ─────────────────────────────────────────────────────────────
        StandardFAB(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 110.dp),
            icon = Icons.Rounded.Add,
            contentDescription = "Ajouter objectif"
        )
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, cat ->
                viewModel.addGoal(title, cat)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GoalItem(goal: Goal, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox circle
        Box(
            Modifier
                .size(20.dp)
                .border(
                    1.dp,
                    if (goal.isCompleted) NothingBlue else NothingBorderMid,
                    CircleShape
                )
                .background(
                    if (goal.isCompleted) NothingBlue else Color.Transparent,
                    CircleShape
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (goal.isCompleted) {
                Icon(
                    Icons.Rounded.Check, null,
                    tint = NothingWhite, modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = goal.title,
                color = if (goal.isCompleted) NothingGrey3 else NothingWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp,
                textDecoration = if (goal.isCompleted)
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                else null
            )
            Spacer(Modifier.height(3.dp))
            CategoryChip(goal.category)
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Rounded.DeleteOutline, null,
                tint = NothingRed, modifier = Modifier.size(16.dp)
            )
        }
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(1.dp).background(NothingDivider))
}

@Composable
private fun CategoryChip(category: GoalCategory) {
    val color = when (category) {
        GoalCategory.WORKOUT   -> NothingRed.copy(alpha = 0.8f)
        GoalCategory.NUTRITION -> NothingYellow.copy(alpha = 0.8f)
        GoalCategory.BODY      -> NothingGrey1.copy(alpha = 0.8f)
        GoalCategory.HABIT     -> NothingGrey2.copy(alpha = 0.8f)
    }
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        NLabel(category.label.uppercase(), size = 7.5.sp, color = color)
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, GoalCategory) -> Unit
) {
    var title    by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GoalCategory.HABIT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = {
            Text("Nouvel objectif", style = MaterialTheme.typography.titleMedium, color = NothingWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Objectif") },
                    placeholder = { Text("Ex : Courir 5 km sans s'arrêter", color = NothingGrey3, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = nothingTextFieldColors(),
                    singleLine = true
                )

                Column {
                    NLabel("CATÉGORIE", size = 8.sp, color = NothingGrey2)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GoalCategory.entries.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { cat ->
                                    FilterChip(
                                        selected = category == cat,
                                        onClick = { category = cat },
                                        label = {
                                            Text(cat.label, style = MaterialTheme.typography.labelSmall)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim(), category) },
                colors = ButtonDefaults.buttonColors(containerColor = NothingDeep, contentColor = NothingWhite),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Ajouter", color = NothingWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = NothingGrey2) }
        }
    )
}
