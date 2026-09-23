package com.gymcompanion.app.ui.screens.workout

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.ExerciseProgressViewModel
import com.gymcompanion.app.viewmodel.ExerciseSummary

private val PAD = 24.dp

@Composable
fun ExerciseProgressScreen(viewModel: ExerciseProgressViewModel = hiltViewModel()) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(NothingBlack)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp)) {
                    NLabel("ENTRAÎNEMENT")
                    Spacer(Modifier.height(8.dp))
                    Text("Progression", fontFamily = LocalNumericFont.current,
                        fontWeight = FontWeight.SemiBold, fontSize = 30.sp,
                        letterSpacing = 1.sp, color = NothingWhite)
                    Spacer(Modifier.height(6.dp))
                    NLabel("1RM ESTIMÉ PAR EXERCICE (FORMULE EPLEY)", size = 8.sp, color = NothingGrey2)
                }
            }

            if (exercises.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                        NLabel("AUCUN EXERCICE ENREGISTRÉ — AJOUTE DES SÉRIES À TES SÉANCES")
                    }
                }
            } else {
                item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
                items(exercises, key = { it.name }) { ex ->
                    ExerciseCard(
                        summary = ex,
                        expanded = selected == ex.name,
                        onClick = { selected = if (selected == ex.name) null else ex.name },
                        viewModel = viewModel
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    summary: ExerciseSummary,
    expanded: Boolean,
    onClick: () -> Unit,
    viewModel: ExerciseProgressViewModel
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = PAD)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (expanded) NothingBorderStrong else NothingBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(summary.name, color = NothingWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                NLabel("${summary.sessionCount} SÉANCE${if (summary.sessionCount > 1) "S" else ""}",
                    size = 8.sp, color = NothingGrey2)
            }
            Column(horizontalAlignment = Alignment.End) {
                NumText("${summary.best1rm.toInt()}", fontSize = 26.sp, fontWeight = FontWeight.Medium,
                    color = NothingBlue)
                NLabel("1RM KG", size = 7.sp, color = NothingGrey2)
            }
        }

        if (summary.oneRmTrend.size >= 2) {
            Spacer(Modifier.height(12.dp))
            Sparkline(
                values = summary.oneRmTrend,
                color = NothingBlue, dotColor = NothingWhite,
                modifier = Modifier.fillMaxWidth().height(if (expanded) 70.dp else 40.dp)
            )
        }

        if (expanded) {
            val detailFlow = remember(summary.name) { viewModel.detailFor(summary.name) }
            val detail by detailFlow.collectAsStateWithLifecycle(null)
            detail?.let { d ->
                Spacer(Modifier.height(16.dp))
                DottedDivider()
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBlock("MEILLEURE SÉRIE", d.bestSetLabel)
                    StatBlock("VOLUME TOTAL", "${d.totalVolumeKg.toInt()} kg")
                    StatBlock("PROGRESSION",
                        if (d.oneRmByDate.size >= 2) {
                            val delta = d.oneRmByDate.last() - d.oneRmByDate.first()
                            (if (delta >= 0) "+" else "") + delta.toInt() + " kg"
                        } else "—")
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = NothingWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            fontFamily = LocalNumericFont.current)
        Spacer(Modifier.height(4.dp))
        NLabel(label, size = 7.sp, color = NothingGrey2)
    }
}
