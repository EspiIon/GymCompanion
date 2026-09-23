package com.gymcompanion.app.ui.screens.body

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.BodyViewModel
import java.time.LocalDate

private val PAD = 24.dp

@Composable
fun BodyDetailScreen(
    dataType: String, // "muscle" or "fat"
    viewModel: BodyViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val visits by viewModel.allGymVisits.collectAsStateWithLifecycle()
    val isMuscle = dataType == "muscle"

    val accentColor = if (isMuscle) NothingBlue else NothingRed
    val title = if (isMuscle) "Analyse Musculaire" else "Masse Graisseuse"

    Box(Modifier.fillMaxSize().background(NothingBlack)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
            item(key = "header") {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour", tint = NothingWhite)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            NLabel(if (isMuscle) "COMPOSITION · MUSCLE" else "COMPOSITION · GRAISSE")
                            Spacer(Modifier.height(4.dp))
                            Text(title, fontFamily = LocalNumericFont.current,
                                fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                                letterSpacing = 0.5.sp, color = NothingWhite)
                        }
                    }
                }
            }

            val validRecords = allRecords.sortedBy { it.date }
            val dataPoints = validRecords.mapNotNull { r ->
                val v = if (isMuscle) r.muscleMassKg else r.bodyFatPercent
                v?.let { LocalDate.parse(r.date).toEpochDay() to it }
            }

            if (validRecords.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        NLabel("AUCUNE DONNÉE DISPONIBLE")
                    }
                }
            } else {
                val latest = validRecords.last()
                val latestVal = if (isMuscle) latest.muscleMassKg else latest.bodyFatPercent
                val latestWeight = latest.weightKg

                item(key = "spacer_header") { Spacer(Modifier.height(18.dp)) }

                item(key = "hero_detail") {
                    WidgetForm(
                        modifier = Modifier.fillMaxWidth(),
                        title = "DERNIÈRE MESURE · ${latest.date}"
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    SegmentedArc(
                                        progress = (latestVal ?: 0f) / (if (isMuscle) 80f else 40f),
                                        color = accentColor,
                                        dotCount = 36,
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(Modifier.width(20.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            NumText(
                                                text = latestVal?.let { numStr(it) } ?: "—",
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            NLabel(if (isMuscle) "KG" else "%", size = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        NLabel(if (isMuscle) "MASSE MUSCULAIRE" else "MASSE GRASSE", size = 8.sp, color = NothingGrey2)
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                if (isMuscle && latestWeight != null && latestVal != null) {
                                    NLabel("SOIT ${numStr(latestVal / latestWeight * 100f)}% DU POIDS TOTAL", color = NothingGrey1, size = 9.sp)
                                } else if (!isMuscle && latestWeight != null && latestVal != null) {
                                    val fatKg = latestWeight * latestVal / 100f
                                    NLabel("SOIT ENVIRON ${numStr(fatKg)} KG DE GRAISSE", color = NothingGrey1, size = 9.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                item(key = "spacer_hero_chart") { Spacer(Modifier.height(12.dp)) }

                if (dataPoints.size >= 2) {
                    item(key = "chart_section") {
                        WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ÉVOLUTION HISTORIQUE") {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                NLabel("ÉVOLUTION HISTORIQUE")
                                val firstVal = dataPoints.first().second
                                val lastVal = dataPoints.last().second
                                val delta = lastVal - firstVal
                                NLabel("${if (delta >= 0) "+" else ""}${numStr(delta)} ${if (isMuscle) "KG" else "%"}", color = if (isMuscle && delta >= 0 || !isMuscle && delta <= 0) NothingWhite else NothingRed)
                            }
                            Spacer(Modifier.height(16.dp))
                            Sparkline(
                                values = dataPoints.map { it.second },
                                color = accentColor,
                                dotColor = NothingWhite,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                item(key = "spacer_chart_stats") { Spacer(Modifier.height(12.dp)) }

                item(key = "stats_grid") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "STATISTIQUES & RAPPORTS") {
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val vals = dataPoints.map { it.second }
                            val min = vals.minOrNull() ?: 0f
                            val max = vals.maxOrNull() ?: 0f
                            val avg = if (vals.isNotEmpty()) vals.average().toFloat() else 0f

                            StatCard("MINIMUM", "${numStr(min)} ${if (isMuscle) "kg" else "%"}", modifier = Modifier.weight(1f))
                            StatCard("MAXIMUM", "${numStr(max)} ${if (isMuscle) "kg" else "%"}", modifier = Modifier.weight(1f))
                            StatCard("MOYENNE", "${numStr(avg)} ${if (isMuscle) "kg" else "%"}", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (visits.isNotEmpty()) {
                    item(key = "spacer_stats_gym") { Spacer(Modifier.height(12.dp)) }
                    item(key = "gym_stats") {
                        WidgetForm(modifier = Modifier.fillMaxWidth(), title = "FRÉQUENTATION SALLE") {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.FitnessCenter, null, tint = accentColor, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("${visits.size} visites enregistrées", color = NothingWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Spacer(Modifier.height(3.dp))
                                    NLabel("CLUB : ${visits.firstOrNull()?.club ?: "Basic-Fit"}", color = NothingGrey2, size = 8.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            NLabel("DERNIÈRE VISITE : ${visits.firstOrNull()?.date} à ${visits.firstOrNull()?.time}", color = NothingGrey1, size = 9.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    NothingCard(modifier = modifier) {
        NLabel(label, size = 7.sp)
        Spacer(Modifier.height(8.dp))
        NumText(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun numStr(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
