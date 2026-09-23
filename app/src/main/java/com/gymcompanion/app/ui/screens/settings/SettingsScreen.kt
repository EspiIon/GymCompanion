package com.gymcompanion.app.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.UserProfile
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.SettingsViewModel
import java.time.LocalDate
import java.time.Year

private val PAD = 24.dp

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val profile         by viewModel.userProfile.collectAsStateWithLifecycle()
    val aiModel         by viewModel.aiModel.collectAsStateWithLifecycle()
    val targetWeightKg  by viewModel.targetWeightKg.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour    by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMin     by viewModel.reminderMin.collectAsStateWithLifecycle()
    val calorieGoal     by viewModel.calorieGoal.collectAsStateWithLifecycle()
    val proteinGoal     by viewModel.proteinGoal.collectAsStateWithLifecycle()
    val carbsGoal       by viewModel.carbsGoal.collectAsStateWithLifecycle()
    val fatGoal         by viewModel.fatGoal.collectAsStateWithLifecycle()
    val autoMacroGoals  by viewModel.autoMacroGoals.collectAsStateWithLifecycle()
    val theme           by viewModel.theme.collectAsStateWithLifecycle()
    val autoBackup      by viewModel.autoBackup.collectAsStateWithLifecycle()
    val lastBackupAt    by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val driveEmail      by viewModel.driveEmail.collectAsStateWithLifecycle()
    val backupBusy      by viewModel.backupBusy.collectAsStateWithLifecycle()
    val backupMessage   by viewModel.backupMessage.collectAsStateWithLifecycle()

    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearBackupMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportTo(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFrom(it) } }
    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onDriveSignInResult(result.data) }
    var showImportConfirm by remember { mutableStateOf(false) }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            containerColor = NothingDark,
            title = { Text("Restaurer une sauvegarde", color = NothingWhite) },
            text = { Text("Les données actuelles seront remplacées par celles du fichier choisi.",
                color = NothingGrey1) },
            confirmButton = {
                Button(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }, colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("Restaurer", color = NothingWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("Annuler", color = NothingGrey2)
                }
            }
        )
    }

    var name           by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var birthYear      by remember(profile) { mutableStateOf(profile?.birthYear?.takeIf { it > 0 }?.toString() ?: "") }
    var heightCm       by remember(profile) { mutableStateOf(profile?.heightCm?.takeIf { it > 0 }?.toString() ?: "") }
    var genderCode     by remember(profile) { mutableStateOf(profile?.genderCode ?: "M") }
    var activityLevel  by remember(profile) { mutableStateOf(profile?.activityLevel ?: 2) }
    var objective      by remember(profile) { mutableStateOf(profile?.objectiveCode ?: "maintain") }
    var selectedAiModel by remember(aiModel) { mutableStateOf(aiModel) }
    var targetWeightStr by remember(targetWeightKg) { mutableStateOf(targetWeightKg?.toString() ?: "") }
    var localReminderEnabled by remember(reminderEnabled) { mutableStateOf(reminderEnabled) }
    var localHour      by remember(reminderHour) { mutableIntStateOf(reminderHour) }
    var localMin       by remember(reminderMin) { mutableIntStateOf(reminderMin) }
    var localCalorieGoal by remember(calorieGoal) { mutableStateOf(calorieGoal?.toString() ?: "") }
    var localProteinGoal by remember(proteinGoal) { mutableStateOf(proteinGoal?.toString() ?: "") }
    var localCarbsGoal by remember(carbsGoal) { mutableStateOf(carbsGoal?.toString() ?: "") }
    var localFatGoal by remember(fatGoal) { mutableStateOf(fatGoal?.toString() ?: "") }
    var localAutoMacro by remember(autoMacroGoals) { mutableStateOf(autoMacroGoals) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NothingBlack),
        contentPadding = PaddingValues(bottom = 88.dp, top = 0.dp)
    ) {
        item(key = "header") {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    NLabel("PARAMÈTRES", color = NothingRed.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(NothingRed, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Configuration", fontFamily = LocalNumericFont.current,
                            fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
                            letterSpacing = 0.5.sp, color = NothingWhite)
                    }
                }
            }
        }

        item(key = "theme") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "THÈME VISUEL") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "oled" to "OLED",
                        "warm" to "CHAUD",
                        "contrast" to "CONTRASTE"
                    ).forEach { (key, label) ->
                        OutlinedButton(
                            onClick = { viewModel.setTheme(key) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, if (theme == key) NothingWhite else NothingBorderMid),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (theme == key) NothingDark2 else Color.Transparent,
                                contentColor = if (theme == key) NothingWhite else NothingGrey2
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── PROFILE ────────────────────────────────────────────────────────────
        item(key = "profile_widget") {
            Spacer(Modifier.height(8.dp))
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "PROFIL UTILISATEUR") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField("Prénom", name, { name = it }, KeyboardType.Text)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InputField("Année de naissance", birthYear,
                            { birthYear = it.filter { c -> c.isDigit() }.take(4) },
                            KeyboardType.Number, Modifier.weight(1f))
                        InputField("Taille (cm)", heightCm,
                            { heightCm = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            KeyboardType.Decimal, Modifier.weight(1f))
                    }
                    Column {
                        NLabel("SEXE", size = 8.sp, color = NothingGrey2)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("M" to "Homme", "F" to "Femme").forEach { (code, label) ->
                                FilterChip(selected = genderCode == code, onClick = { genderCode = code },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    Column {
                        NLabel("NIVEAU D'ACTIVITÉ", size = 8.sp, color = NothingGrey2)
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = activityLevel.toFloat(),
                            onValueChange = { activityLevel = it.toInt().coerceIn(1, 5) },
                            valueRange = 1f..5f, steps = 3, modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = NothingRed, activeTrackColor = NothingRed.copy(alpha = 0.6f),
                                inactiveTrackColor = NothingBorder
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        NLabel(listOf("Sédentaire", "Peu actif", "Modérément actif", "Très actif",
                            "Extrêmement actif").getOrNull(activityLevel - 1) ?: "",
                            size = 9.sp, color = NothingGrey1)
                    }
                    Column {
                        NLabel("OBJECTIF", size = 8.sp, color = NothingGrey2)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("cut" to "Perte de poids", "maintain" to "Maintien", "bulk" to "Prise de masse")
                                .forEach { (code, label) ->
                                    FilterChip(selected = objective == code, onClick = { objective = code },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                                }
                        }
                    }
                    Column {
                        NLabel("OBJECTIF POIDS (KG)", size = 8.sp, color = NothingGrey2)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = targetWeightStr,
                            onValueChange = { targetWeightStr = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            placeholder = { Text("ex: 75.0", color = NothingGrey3, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = nothingTextFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true
                        )
                        Spacer(Modifier.height(4.dp))
                        NLabel("Affiché en trait pointillé sur le graphique de poids", size = 7.sp, color = NothingGrey3)
                    }
                }
            }
        }

        // ── NUTRITION GOALS ────────────────────────────────────────────────────
        item(key = "nutrition_widget") {
            Spacer(Modifier.height(12.dp))
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "OBJECTIFS NUTRITION") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Macros automatiques", color = NothingGrey1, fontSize = 13.sp)
                            NLabel("30% prot, 45% gluc, 25% lip", size = 7.sp, color = NothingGrey3)
                        }
                        Switch(
                            checked = localAutoMacro,
                            onCheckedChange = { localAutoMacro = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NothingBlack,
                                checkedTrackColor = DataMint,
                                uncheckedThumbColor = NothingGrey3,
                                uncheckedTrackColor = NothingBorder
                            )
                        )
                    }
                    Column {
                        NLabel("OBJECTIF CALORIES (KCAL)", size = 8.sp, color = NothingGrey2)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = localCalorieGoal,
                            onValueChange = { localCalorieGoal = it.filter { c -> c.isDigit() }.take(5) },
                            placeholder = { Text("ex: 2000", color = NothingGrey3, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = nothingTextFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true
                        )
                    }
                    if (!localAutoMacro) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                NLabel("PROTÉINES (G)", size = 8.sp, color = NothingGrey2)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = localProteinGoal,
                                    onValueChange = { localProteinGoal = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                    placeholder = { Text("ex: 150", color = NothingGrey3, fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = nothingTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    singleLine = true
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                NLabel("GLUCIDES (G)", size = 8.sp, color = NothingGrey2)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = localCarbsGoal,
                                    onValueChange = { localCarbsGoal = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                    placeholder = { Text("ex: 200", color = NothingGrey3, fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = nothingTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    singleLine = true
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                NLabel("LIPIDES (G)", size = 8.sp, color = NothingGrey2)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = localFatGoal,
                                    onValueChange = { localFatGoal = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                                    placeholder = { Text("ex: 70", color = NothingGrey3, fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = nothingTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }

        item { DottedDivider(Modifier.padding(horizontal = PAD)); Spacer(Modifier.height(20.dp)) }

        // ── IA SECTION ────────────────────────────────────────────────────────
        item(key = "ai_head") {
            NLabel("ASSISTANT IA", modifier = Modifier.padding(horizontal = PAD),
                color = NothingYellow.copy(alpha = 0.7f), size = 9.sp)
            Spacer(Modifier.height(14.dp))
        }

        item(key = "ai_model") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "MODÈLE IA") {
                NLabel("MODÈLE OPENROUTER PERSONNALISÉ", size = 8.sp, color = NothingGrey2)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedAiModel,
                    onValueChange = { selectedAiModel = it; viewModel.setAiModel(it) },
                    placeholder = { Text("ex: google/gemma-4-31b-it:free", color = NothingGrey3, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = nothingTextFieldColors(), shape = RoundedCornerShape(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp), singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                NLabel("Liste : openrouter.ai/models", size = 7.sp, color = NothingGrey3)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3",
                        "google/gemma-4-31b-it:free"             to "Gemma 4",
                        "mistralai/mistral-small-3.1-24b-instruct:free" to "Mistral Small",
                        "thinkingmachines/inkling-small:free"    to "Inkling Small"
                    ).forEach { (model, label) ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .clickable { selectedAiModel = model; viewModel.setAiModel(model) }
                                .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lightbulb, null,
                                    tint = NothingYellow, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(label, color = NothingGrey1, fontSize = 11.sp)
                                Spacer(Modifier.weight(1f))
                                NLabel(model.take(20) + if (model.length > 20) "…" else "",
                                    size = 7.sp, color = NothingGrey3)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        item { DottedDivider(Modifier.padding(horizontal = PAD)); Spacer(Modifier.height(20.dp)) }

        // ── NOTIFICATIONS ─────────────────────────────────────────────────────
        item(key = "reminder") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "RAPPELS") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rappel nutrition quotidien", color = NothingGrey1, fontSize = 13.sp)
                        NLabel("Notification à l'heure choisie", size = 7.sp, color = NothingGrey3)
                    }
                    Switch(
                        checked = localReminderEnabled,
                        onCheckedChange = {
                            localReminderEnabled = it
                            viewModel.setReminder(it, localHour, localMin)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingBlack,
                            checkedTrackColor = NothingWhite,
                            uncheckedThumbColor = NothingGrey3,
                            uncheckedTrackColor = NothingBorder
                        )
                    )
                }
                if (localReminderEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InputField(
                            "Heure (0-23)", localHour.toString(),
                            { v ->
                                val h = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 23)
                                if (h != null) {
                                    localHour = h
                                    viewModel.setReminder(true, h, localMin)
                                }
                            },
                            KeyboardType.Number, Modifier.weight(1f)
                        )
                        InputField(
                            "Minutes (0-59)", localMin.toString().padStart(2, '0'),
                            { v ->
                                val m = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59)
                                if (m != null) {
                                    localMin = m
                                    viewModel.setReminder(true, localHour, m)
                                }
                            },
                            KeyboardType.Number, Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(14.dp))
        }

        item { DottedDivider(Modifier.padding(horizontal = PAD)); Spacer(Modifier.height(20.dp)) }

        // ── SAUVEGARDE ─────────────────────────────────────────────────────────
        item(key = "backup") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "SAUVEGARDE") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sauvegarde automatique", color = NothingGrey1, fontSize = 13.sp)
                        NLabel(
                            lastBackupAt?.let { "Dernière : " + backupTimeLabel(it) }
                                ?: "Quotidienne (locale + Drive si connecté)",
                            size = 7.sp, color = NothingGrey3
                        )
                    }
                    Switch(
                        checked = autoBackup,
                        onCheckedChange = { viewModel.setAutoBackup(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingBlack,
                            checkedTrackColor = NothingWhite,
                            uncheckedThumbColor = NothingGrey3,
                            uncheckedTrackColor = NothingBorder
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NothingBorderMid)
                    ) {
                        Icon(Icons.Rounded.Upload, null, tint = NothingWhite, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Exporter", color = NothingWhite, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { showImportConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NothingBorderMid)
                    ) {
                        Icon(Icons.Rounded.Download, null, tint = NothingWhite, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Importer", color = NothingWhite, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                NLabel("GOOGLE DRIVE", size = 8.sp, color = NothingGrey2)
                Spacer(Modifier.height(8.dp))
                if (driveEmail == null) {
                    OutlinedButton(
                        onClick = { driveSignInLauncher.launch(viewModel.driveSignInIntent()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NothingBorderMid)
                    ) {
                        Icon(Icons.Rounded.CloudSync, null, tint = NothingWhite, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Connecter Google Drive", color = NothingWhite, fontSize = 12.sp)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(driveEmail ?: "", color = NothingGrey1, fontSize = 12.sp, maxLines = 1)
                            NLabel("CONNECTÉ", size = 7.sp, color = NothingGrey1)
                        }
                        TextButton(onClick = { viewModel.signOutDrive() }) {
                            Text("Déconnecter", color = NothingGrey2, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.syncNow() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NothingBorderMid)
                        ) {
                            Icon(Icons.Rounded.CloudUpload, null, tint = NothingGrey1, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Synchroniser", color = NothingWhite, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.restoreFromDrive() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NothingBorderMid)
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, tint = NothingGrey1, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Restaurer", color = NothingWhite, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                NLabel(
                    if (driveEmail != null)
                        "✓ Connecté · dossier privé de l'app (appDataFolder)"
                    else
                        "Non connecté · nécessite la configuration OAuth (Google Cloud Console)",
                    size = 7.sp,
                    color = if (driveEmail != null) DataMint else NothingGrey3
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        item { DottedDivider(Modifier.padding(horizontal = PAD)); Spacer(Modifier.height(20.dp)) }

        // ── SAVE ──────────────────────────────────────────────────────────────
        item(key = "save") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ENREGISTRER") {
                Button(
                    onClick = {
                        viewModel.saveProfile(
                            UserProfile(
                                name = name.trim(),
                                birthYear = birthYear.toIntOrNull() ?: 0,
                                heightCm = heightCm.toFloatOrNull() ?: 0f,
                                genderCode = genderCode,
                                activityLevel = activityLevel,
                                objectiveCode = objective
                            )
                        )
                        viewModel.setTargetWeight(targetWeightStr.toFloatOrNull()?.takeIf { it > 0f })
                        viewModel.setNutritionGoals(
                            calories = localCalorieGoal.toIntOrNull(),
                            protein = if (!localAutoMacro) localProteinGoal.toFloatOrNull() else null,
                            carbs = if (!localAutoMacro) localCarbsGoal.toFloatOrNull() else null,
                            fat = if (!localAutoMacro) localFatGoal.toFloatOrNull() else null,
                            auto = localAutoMacro
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ENREGISTRER", color = NothingWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

private fun backupTimeLabel(epochMillis: Long): String {
    val dt = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
    return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        NLabel(label, size = 8.sp, color = NothingGrey2)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            colors = nothingTextFieldColors(),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            singleLine = true
        )
    }
}
