package com.gymcompanion.app.ui.screens.body

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.BodyRecord
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.BodyViewModel
import com.gymcompanion.app.viewmodel.ImportState
import com.gymcompanion.app.viewmodel.SettingsViewModel
import com.gymcompanion.app.viewmodel.WeightProjection

private val PAD = 24.dp

@Composable
fun BodyScreen(
    viewModel: BodyViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToProgress: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val latest         by viewModel.latestRecord.collectAsStateWithLifecycle()
    val allRecords     by viewModel.allRecords.collectAsStateWithLifecycle()
    val last30Days     by viewModel.last30Days.collectAsStateWithLifecycle()
    val importState    by viewModel.importState.collectAsStateWithLifecycle()
    val targetWeightKg by settingsViewModel.targetWeightKg.collectAsStateWithLifecycle()
    val projection     by viewModel.weightProjection.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.importFromBasicFit(context, it) }
    }

    var showHistory by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(NothingBlack)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {

            item(key = "header") {
                Row(
                    Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        NLabel("CORPS")
                        Spacer(Modifier.height(8.dp))
                        Text("Composition", fontFamily = LocalNumericFont.current,
                            fontWeight = FontWeight.SemiBold, fontSize = 30.sp,
                            letterSpacing = 1.sp, color = NothingWhite)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingDeep)
                                .border(1.dp, NothingBorderMid, RoundedCornerShape(10.dp))
                                .clickable { filePicker.launch("application/json") }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Upload,
                                    contentDescription = null,
                                    tint = NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                NLabel("IMPORT BASIC FIT", color = NothingWhite, size = 9.sp)
                            }
                        }
                    }
                }
            }

            item(key = "header_spacer") { Spacer(Modifier.height(16.dp)) }

            if (importState !is ImportState.Idle) {
                item(key = "import") {
                    Spacer(Modifier.height(14.dp))
                    ImportBanner(importState, viewModel::clearImportState, Modifier.padding(horizontal = PAD))
                }
            }

            if (latest != null) {
                item(key = "hero") {
                    WidgetForm(
                        modifier = Modifier.fillMaxWidth(),
                        title = "DERNIER RELEVÉ · ${latest!!.date}",
                        onClick = null
                    ) {
                        BodyHero(record = latest!!, modifier = Modifier.padding(horizontal = 0.dp), onNavigateToDetail = onNavigateToDetail)
                    }
                }
            }

            val weights = last30Days.filter { it.weightKg != null }
            if (weights.size >= 2) {
                item(key = "chart_spacer") { Spacer(Modifier.height(8.dp)) }
                item(key = "chart") {
                    WidgetForm(
                        modifier = Modifier.fillMaxWidth(),
                        title = "ÉVOLUTION DU POIDS · 30 J"
                    ) {
                        val sorted = weights.sortedBy { it.date }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            NLabel("ÉVOLUTION · 30 J")
                            NLabel("${numStr(sorted.first().weightKg!!)} → ${numStr(sorted.last().weightKg!!)} KG",
                                color = NothingGrey1)
                        }
                        Spacer(Modifier.height(16.dp))
                        Sparkline(
                            values = sorted.mapNotNull { it.weightKg },
                            color = NothingBlue, dotColor = NothingWhite,
                            modifier = Modifier.fillMaxWidth().height(70.dp),
                            targetValue = targetWeightKg
                        )
                    }
                }
            }

            if (projection !is WeightProjection.Insufficient) {
                item(key = "proj_spacer") { Spacer(Modifier.height(8.dp)) }
                item(key = "projection") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "PROJECTION") {
                        ProjectionCard(projection, Modifier.fillMaxWidth())
                    }
                }
            }

            // ── COMPOSITION · 30 J — muscle (plein) vs graisse (pointillé) ──────
            val fatPoints = last30Days.filter { it.bodyFatPercent != null }.sortedBy { it.date }
            val musclePoints = last30Days.filter { it.muscleMassKg != null }.sortedBy { it.date }
            if (fatPoints.size >= 2 || musclePoints.size >= 2) {
                item(key = "chart_comp_spacer") { Spacer(Modifier.height(8.dp)) }
                item(key = "chart_comp") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "COMPOSITION · 30 J") {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            NLabel("COMPOSITION · 30 J")
                            // Légende : pattern = signification
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LegendEntry(color = NothingWhite, dashed = false, label = "MUSCLE")
                                LegendEntry(color = NothingRed,   dashed = true,  label = "GRAISSE")
                            }
                        }
                        // Deltas
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (musclePoints.size >= 2) {
                                val d = musclePoints.last().muscleMassKg!! - musclePoints.first().muscleMassKg!!
                                DeltaChip("M", d, "KG", if (d >= 0f) NothingWhite else NothingRed)
                            }
                            if (fatPoints.size >= 2) {
                                val d = fatPoints.last().bodyFatPercent!! - fatPoints.first().bodyFatPercent!!
                                DeltaChip("G", d, "%", if (d <= 0f) NothingWhite else NothingRed)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        DualTrendChart(
                            muscle = musclePoints.mapNotNull { r ->
                                r.muscleMassKg?.let {
                                    TrendPoint(java.time.LocalDate.parse(r.date).toEpochDay(), it)
                                }
                            },
                            fat = fatPoints.mapNotNull { r ->
                                r.bodyFatPercent?.let {
                                    TrendPoint(java.time.LocalDate.parse(r.date).toEpochDay(), it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                }
            }

            item(key = "hist_spacer") { Spacer(Modifier.height(8.dp)) }
            item(key = "hist_head") {
                WidgetForm(
                    modifier = Modifier.fillMaxWidth(),
                    title = if (showHistory) "HISTORIQUE · MESURES" else "HISTORIQUE · MASQUÉ",
                    onClick = { showHistory = !showHistory }
                ) {
                    if (showHistory) {
                        if (allRecords.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                                NLabel("AUCUNE MESURE — IMPORTEZ VOS DONNÉES BASIC FIT")
                            }
                        } else {
                            allRecords.forEachIndexed { i, r ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NLabel(r.date, color = NothingGrey1, modifier = Modifier.weight(1f))
                                        r.weightKg?.let    { NumText("${numStr(it)}kg", fontSize = 13.sp); Spacer(Modifier.width(8.dp)) }
                                        r.bodyFatPercent?.let { NumText("${numStr(it)}%F", fontSize = 13.sp, color = NothingGrey1); Spacer(Modifier.width(8.dp)) }
                                        r.muscleMassKg?.let   { NumText("${numStr(it)}kgM", fontSize = 13.sp, color = NothingGrey1) }
                                    }
                                    if (i < allRecords.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = NothingGrey2, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                NLabel("CLIQUEZ POUR VOIR L'HISTORIQUE DES MESURES", color = NothingGrey2)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 80.dp)
                .border(1.dp, NothingBorderMid, RoundedCornerShape(14.dp)),
            containerColor = NothingDeep, contentColor = NothingWhite,
            shape = RoundedCornerShape(14.dp)
        ) { Icon(Icons.Rounded.Add, contentDescription = "Ajouter mesure") }
    }

    if (showAddDialog) {
        AddBodyRecordDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { w, bf, mm, bone, water, wc, cc, ac ->
                viewModel.addRecord(w, bf, mm, bone, water, wc, cc, ac)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProjectionCard(projection: WeightProjection, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        NLabel("PROJECTION", color = NothingGrey2)
        Spacer(Modifier.height(14.dp))
        when (projection) {
            is WeightProjection.OnTrack -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    NumText(numStr(projection.targetKg), fontSize = 40.sp, fontWeight = FontWeight.Medium,
                        color = NothingBlue)
                    Spacer(Modifier.width(6.dp))
                    NLabel("KG", size = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                }
                Spacer(Modifier.height(10.dp))
                val months = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin",
                    "juil.", "août", "sept.", "oct.", "nov.", "déc.")
                val d = projection.etaDate
                Text(
                    "${if (projection.losing) "Perte" else "Prise"} en cours · objectif vers le " +
                        "${d.dayOfMonth} ${months[d.monthValue - 1]} ${d.year}",
                    color = NothingGrey1, fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                NLabel("ENVIRON ${projection.weeks} SEMAINE${if (projection.weeks > 1) "S" else ""} À CE RYTHME",
                    color = NothingGrey2, size = 8.sp)
            }
            is WeightProjection.Reached -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = NothingGrey1, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Objectif de ${numStr(projection.targetKg)} kg atteint !",
                        color = NothingWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            is WeightProjection.WrongTrend -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TrendingFlat, null, tint = NothingRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Tendance opposée à l'objectif",
                            color = NothingWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(3.dp))
                        NLabel("CIBLE ${numStr(projection.targetKg)} KG — AJUSTE TON DÉFICIT",
                            color = NothingGrey2, size = 8.sp)
                    }
                }
            }
            WeightProjection.Insufficient -> Unit
        }
    }
}

@Composable
private fun BodyHero(
    record: BodyRecord,
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit = {}
) {
    Column(modifier.fillMaxWidth()) {
        NLabel("DERNIER RELEVÉ · ${record.date}")
        Spacer(Modifier.height(12.dp))
        record.weightKg?.let {
            Row(verticalAlignment = Alignment.Bottom) {
                NumText(numStr(it), fontSize = 64.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                NLabel("KG", size = 12.sp, modifier = Modifier.padding(bottom = 9.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // dual arcs — fat + muscle — symétrique, centré, nombre d'or
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly) {
            record.bodyFatPercent?.let {
                val fatKg = record.weightKg?.let { w -> w * it / 100f }
                Box(Modifier.weight(1f)
                    .clickable { onNavigateToDetail("fat") },
                    contentAlignment = Alignment.Center) {
                    ArcStat("GRAISSE", "${numStr(it)}%",
                        (it / 40f).coerceIn(0f, 1f), NothingRed,
                        fatKg?.let { k -> "${numStr(k)} KG" } ?: "")
                }
            }
            record.muscleMassKg?.let {
                Box(Modifier.weight(1f)
                    .clickable { onNavigateToDetail("muscle") },
                    contentAlignment = Alignment.Center) {
                    ArcStat("MUSCLE", "${numStr(it)} KG", (it / 80f).coerceIn(0f, 1f), NothingBlue,
                        record.weightKg?.let { w -> "${numStr(it / w * 100f)}%" } ?: "")
                }
            }
        }

        // Bone + Water row — centré, symétrique, sans lignes
        val hasBoneOrWater = record.bonePercent != null || record.waterPercent != null
        if (hasBoneOrWater) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                record.bonePercent?.let { bp ->
                    val boneKg = record.weightKg?.let { w -> w * bp / 100f }
                    CompactStat(
                        label = "OS",
                        value = "${numStr(bp)}%",
                        sub   = boneKg?.let { "${numStr(it)} KG" } ?: "",
                        modifier = Modifier.weight(1f)
                    )
                }
                record.waterPercent?.let { wp ->
                    val waterKg = record.weightKg?.let { w -> w * wp / 100f }
                    CompactStat(
                        label = "EAU",
                        value = "${numStr(wp)}%",
                        sub   = waterKg?.let { "${numStr(it)} KG" } ?: "",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Optional measurements — sans lignes dividers
        if (record.waistCm != null || record.chestCm != null || record.armCm != null) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                record.waistCm?.let { CompactStat("TAILLE", "${numStr(it)} CM", "", Modifier.weight(1f)) }
                record.chestCm?.let { CompactStat("POITRINE", "${numStr(it)} CM", "", Modifier.weight(1f)) }
                record.armCm?.let   { CompactStat("BRAS",    "${numStr(it)} CM", "", Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ArcStat(label: String, value: String, progress: Float, color: Color, sub: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(92.dp).align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            SegmentedArc(progress = progress, color = color, dotCount = 40,
                modifier = Modifier.fillMaxSize().align(Alignment.Center))
            NumText(value, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(8.dp))
        NLabel(label, size = 8.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (sub.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            NLabel(sub, color = NothingGrey1, size = 8.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NumText(value, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
        NLabel(label, size = 8.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (sub.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            NLabel(sub, color = NothingGrey1, size = 8.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun RecordRow(record: BodyRecord, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { showDelete = !showDelete }
            .padding(horizontal = PAD, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NLabel(record.date, color = NothingGrey1, modifier = Modifier.weight(1f))
        record.weightKg?.let    { NumText("${numStr(it)}kg", fontSize = 13.sp); Spacer(Modifier.width(10.dp)) }
        record.bodyFatPercent?.let { NumText("${numStr(it)}%F", fontSize = 13.sp, color = NothingGrey1); Spacer(Modifier.width(10.dp)) }
        record.muscleMassKg?.let   { NumText("${numStr(it)}kgM", fontSize = 13.sp, color = NothingGrey1) }
        if (showDelete) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = NothingRed, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
fun ImportBanner(state: ImportState, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val (accent, text) = when (state) {
        is ImportState.Loading -> NothingGrey1 to "IMPORT EN COURS…"
        is ImportState.Success -> NothingGrey1 to "${state.count} MESURES IMPORTÉES"
        is ImportState.Error   -> NothingRed   to (state.message.uppercase())
        else -> return
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        NLabel(text, color = accent, modifier = Modifier.weight(1f))
        if (state !is ImportState.Loading) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, null, tint = NothingGrey2, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun AddBodyRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float?, Float?, Float?, Float?, Float?, Float?, Float?, Float?) -> Unit
) {
    var weight  by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var muscle  by remember { mutableStateOf("") }
    var bone    by remember { mutableStateOf("") }
    var water   by remember { mutableStateOf("") }
    var waist   by remember { mutableStateOf("") }
    var chest   by remember { mutableStateOf("") }
    var arm     by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = { Text("Nouvelle mesure", color = NothingWhite, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NLabel("COMPOSITION")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = weight, onValueChange = { weight = it },
                        label = { Text("Poids kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                    OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it },
                        label = { Text("Graisse %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = muscle, onValueChange = { muscle = it },
                        label = { Text("Muscle kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                    OutlinedTextField(value = bone, onValueChange = { bone = it },
                        label = { Text("Os %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                    OutlinedTextField(value = water, onValueChange = { water = it },
                        label = { Text("Eau %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                }
                NLabel("MENSURATIONS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = waist, onValueChange = { waist = it },
                        label = { Text("Taille") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                    OutlinedTextField(value = chest, onValueChange = { chest = it },
                        label = { Text("Poitrine") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                    OutlinedTextField(value = arm, onValueChange = { arm = it },
                        label = { Text("Bras") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), colors = nothingTextFieldColors())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        weight.toFloatOrNull()?.takeIf { it > 0f },
                        bodyFat.toFloatOrNull()?.takeIf { it in 0f..100f },
                        muscle.toFloatOrNull()?.takeIf { it > 0f },
                        bone.toFloatOrNull()?.takeIf { it in 0f..100f },
                        water.toFloatOrNull()?.takeIf { it in 0f..100f },
                        waist.toFloatOrNull()?.takeIf { it > 0f },
                        chest.toFloatOrNull()?.takeIf { it > 0f },
                        arm.toFloatOrNull()?.takeIf { it > 0f }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NothingBlue),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Enregistrer", color = NothingWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = NothingGrey2) }
        }
    )
}

private fun numStr(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)

// ── Légende du graphique combiné ───────────────────────────────────────────────
@Composable
private fun LegendEntry(color: Color, dashed: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (dashed) {
                Box(Modifier.width(5.dp).height(2.dp).background(color, RoundedCornerShape(1.dp)))
                Box(Modifier.width(5.dp).height(2.dp).background(color, RoundedCornerShape(1.dp)))
                Box(Modifier.width(2.dp).height(2.dp).background(color, RoundedCornerShape(1.dp)))
            } else {
                Box(Modifier.width(16.dp).height(2.dp).background(color, RoundedCornerShape(1.dp)))
            }
        }
        NLabel(label, size = 7.sp, color = NothingGrey2)
    }
}

@Composable
private fun DeltaChip(prefix: String, delta: Float, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NLabel(prefix, size = 8.sp, color = NothingGrey3)
        NumText(
            "${if (delta >= 0f) "+" else ""}${numStr(delta)}",
            fontSize = 13.sp, color = color
        )
        NLabel(unit, size = 8.sp, color = NothingGrey3)
    }
}
