package com.gymcompanion.app.ui.screens.nutrition

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.*
import com.gymcompanion.app.data.remote.ScannedProduct
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.*
import java.time.LocalDate

private val PAD = 24.dp

@Composable
fun NutritionScreen(viewModel: NutritionViewModel = hiltViewModel()) {
    val entries         by viewModel.entries.collectAsStateWithLifecycle()
    val macroTotals     by viewModel.macroTotals.collectAsStateWithLifecycle()
    val selectedDate    by viewModel.selectedDate.collectAsStateWithLifecycle()
    val scanState       by viewModel.scanState.collectAsStateWithLifecycle()
    val nutritionGoals  by viewModel.nutritionGoals.collectAsStateWithLifecycle()
    val recentFoods     by viewModel.recentFoods.collectAsStateWithLifecycle()
    val favoriteFoods   by viewModel.favoriteFoods.collectAsStateWithLifecycle()
    val foodItems       by viewModel.foodItems.collectAsStateWithLifecycle()
    val searchState     by viewModel.searchState.collectAsStateWithLifecycle()

    val entriesByMeal = remember(entries) { entries.groupBy { it.mealType } }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf<MealType?>(null) }
    var prefillProduct by remember { mutableStateOf<ScannedProduct?>(null) }
    var prefillEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var showCreatineDialog by remember { mutableStateOf(false) }
    val creatineToday by viewModel.creatineToday.collectAsStateWithLifecycle()

    LaunchedEffect(scanState) {
        if (scanState is ScanState.Success) {
            prefillProduct = (scanState as ScanState.Success).product
            showAddDialog = true
        }
    }

    Box(Modifier.fillMaxSize().background(NothingBlack)) {

        if (scanState is ScanState.Scanning || scanState is ScanState.Loading) {
            if (scanState is ScanState.Scanning) {
                BarcodeScannerScreen(
                    onBarcodeDetected = viewModel::onBarcodeDetected,
                    onDismiss = viewModel::cancelScan
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DataOrange, strokeWidth = 2.dp)
                        Spacer(Modifier.height(14.dp))
                        NLabel("RECHERCHE…", color = NothingGrey1)
                    }
                }
            }
        } else {
            val consumed  = macroTotals.totalCalories
            val progress  = (consumed.toFloat() / nutritionGoals.calories).coerceIn(0f, 1f)
            val remaining = (nutritionGoals.calories - consumed).coerceAtLeast(0)

            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {

                // ── Header + date nav ──────────────────────────────────────────
                item(key = "header") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = PAD).padding(top = 26.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            NLabel("NUTRITION")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.changeDate(selectedDate.minusDays(1)) },
                                    modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.ChevronLeft, null, tint = NothingGrey2, modifier = Modifier.size(18.dp))
                                }
                                NLabel(if (selectedDate == LocalDate.now()) "AUJOURD'HUI" else selectedDate.toString(),
                                    color = NothingGrey1)
                                IconButton(onClick = { viewModel.changeDate(selectedDate.plusDays(1)) },
                                    enabled = selectedDate != LocalDate.now(), modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.ChevronRight, null,
                                        tint = if (selectedDate != LocalDate.now()) NothingGrey2 else NothingGrey3,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // ── Hero ring ──────────────────────────────────────────────────
                item(key = "ring") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "CALORIES RESTANTES") {
                        Spacer(Modifier.height(18.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                                SegmentedArc(progress = progress, color = DataOrange, dotCount = 60,
                                    modifier = Modifier.fillMaxSize())
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NumText("$remaining", fontSize = 60.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(8.dp))
                                    NLabel("$consumed / ${nutritionGoals.calories} KCAL")
                                    Spacer(Modifier.height(4.dp))
                                    NLabel("RESTANTES", color = NothingGrey2)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ── Macro bar ──────────────────────────────────────────────────
                item(key = "macros") {
                    Spacer(Modifier.height(8.dp))
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "MACROS · NUTRITION") {
                        // Protein
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                NLabel("PROTÉINES", size = 12.sp, color = NothingGrey2)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NumText("${macroTotals.totalProtein.toInt()}g", fontSize = 13.sp, color = NothingWhite)
                                    NLabel(" / ${nutritionGoals.protein.toInt()}g", size = 9.sp, color = NothingGrey2)
                                }
                            }
                            val proteinProg = (macroTotals.totalProtein / nutritionGoals.protein.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            Box(Modifier.width(60.dp).height(6.dp).background(NothingBorder, RoundedCornerShape(2.dp))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(proteinProg).background(ProteinColor, RoundedCornerShape(2.dp)))
                            }
                        }

                        // Carbs
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                NLabel("GLUCIDES", size = 12.sp, color = NothingGrey2)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NumText("${macroTotals.totalCarbs.toInt()}g", fontSize = 13.sp, color = NothingWhite)
                                    NLabel(" / ${nutritionGoals.carbs.toInt()}g", size = 9.sp, color = NothingGrey2)
                                }
                            }
                            val carbsProg = (macroTotals.totalCarbs / nutritionGoals.carbs.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            Box(Modifier.width(60.dp).height(6.dp).background(NothingBorder, RoundedCornerShape(2.dp))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(carbsProg).background(CarbsColor, RoundedCornerShape(2.dp)))
                            }
                        }

                        // Fat
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                NLabel("LIPIDES", size = 12.sp, color = NothingGrey2)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NumText("${macroTotals.totalFat.toInt()}g", fontSize = 13.sp, color = NothingWhite)
                                    NLabel(" / ${nutritionGoals.fat.toInt()}g", size = 9.sp, color = NothingGrey2)
                                }
                            }
                            val fatProg = (macroTotals.totalFat / nutritionGoals.fat.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            Box(Modifier.width(60.dp).height(6.dp).background(NothingBorder, RoundedCornerShape(2.dp))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(fatProg).background(FatColor, RoundedCornerShape(2.dp)))
                            }
                        }

                        // ── Créatine (en dessous des lipides) ──────────────────────
                        val creatineLog by viewModel.creatineToday.collectAsStateWithLifecycle()
                        val takenToday = creatineLog.any { it.taken }
                        val gramsToday = creatineLog.firstOrNull()?.grams ?: 0f
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(6.dp).background(
                                    if (takenToday) NothingBlue else NothingGrey3, CircleShape))
                                NLabel("CRÉATINE", size = 9.sp, color = NothingGrey2)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (takenToday) {
                                    NumText("${gramsToday.toInt()}g", fontSize = 13.sp, color = NothingBlue)
                                } else {
                                    NLabel("—", size = 9.sp, color = NothingGrey3)
                                }
                            }
                        }
                    }
                }
                item(key = "macros_spacer") { Spacer(Modifier.height(8.dp)) }

                // ── Quick-add (widget) ────────────────────────────────────────────
                if (favoriteFoods.isNotEmpty() || recentFoods.isNotEmpty()) {
                    item(key = "quickadd") {
                        Spacer(Modifier.height(8.dp))
                        WidgetForm(modifier = Modifier.fillMaxWidth(), title = "QUICK ADD · FAVORIS & RÉCENTS") {
                            QuickAddRow(
                                favorites = favoriteFoods,
                                recents = recentFoods.filterNot { fe ->
                                    favoriteFoods.any { it.name.equals(fe.name, ignoreCase = true) }
                                },
                                onQuickAdd = { entry ->
                                    prefillEntry = entry
                                    prefillProduct = null
                                    selectedMealType = null
                                    showAddDialog = true
                                },
                                onLongPress = {
                                    prefillEntry = it
                                    prefillProduct = null
                                    selectedMealType = null
                                    showAddDialog = true
                                }
                            )
                        }
                    }
                }
                item(key = "quick_spacer") { Spacer(Modifier.height(8.dp)) }

                // ── Meals (un seul widget) ─────────────────────────────────────────
                item(key = "meals") {
                    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "REPAS DE LA JOURNÉE") {
                        MealType.entries.forEach { mealType ->
                            val mealEntries = entriesByMeal[mealType] ?: emptyList()
                            MealSection(
                                mealType = mealType,
                                entries = mealEntries,
                                onAddClick = remember(mealType) {
                                    {
                                        selectedMealType = mealType
                                        prefillProduct = null
                                        showAddDialog = true
                                    }
                                },
                                onEntryClick = { editingEntry = it }
                            )
                        }
                    }
                }
            }

            // FABs
            Row(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallFAB(
                    onClick = viewModel::startScanning,
                    modifier = Modifier,
                    icon = Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scanner",
                    tinted = true
                )
                // Bouton + avec Cr en dessous
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StandardFAB(
                        onClick = { prefillProduct = null; showAddDialog = true },
                        modifier = Modifier,
                        icon = Icons.Rounded.Add,
                        contentDescription = "Ajouter"
                    )
                    // Logo Cr — même style que le bouton + mais plus petit, texte blanc
                    SmallFloatingActionButton(
                        onClick = { showCreatineDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                            .border(1.dp, NothingBorderMid, RoundedCornerShape(10.dp)),
                        containerColor = NothingDeep,
                        contentColor = NothingWhite,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cr",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { contentDescription = "Ajouter de la créatine" }
                        )
                    }
                }
            }

            if (scanState is ScanState.NotFound) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 20.dp, end = 20.dp),
                    action = {
                        TextButton(onClick = {
                            prefillProduct = null; showAddDialog = true; viewModel.clearScanResult()
                        }) { Text("Saisir", color = NothingGrey1) }
                    },
                    dismissAction = {
                        IconButton(onClick = viewModel::clearScanResult) {
                            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                        }
                    },
                    containerColor = NothingDark, contentColor = NothingWhite
                ) { Text("Produit introuvable (${(scanState as ScanState.NotFound).barcode})") }
            }
        }
    }

    if (showAddDialog) {
        AddFoodDialog(
            initialMealType = selectedMealType ?: MealType.LUNCH,
            prefill = prefillProduct,
            prefillEntry = prefillEntry,
            recentFoods = recentFoods,
            favoriteFoods = favoriteFoods,
            foodItems = foodItems,
            searchState = searchState,
            onSearch = viewModel::searchOnline,
            onClearSearch = viewModel::clearSearch,
            onToggleFavorite = viewModel::toggleFavorite,
            onSaveFoodItem = viewModel::addFoodItem,
            onAutoSaveCatalogue = { name, kcal, p, c, f, category ->
                viewModel.saveToCatalogueIfAbsent(
                    FoodItem(
                        name = name, kcalPer100g = kcal,
                        proteinPer100g = p, carbsPer100g = c, fatPer100g = f,
                        category = category
                    )
                )
            },
            onDismiss = {
                showAddDialog = false; selectedMealType = null; prefillProduct = null; prefillEntry = null
                viewModel.clearScanResult(); viewModel.clearSearch()
            },
            onConfirm = { name, cal, p, c, f, meal, grams, quantity, unitLabel, unit, barcode ->
                viewModel.addEntry(name, cal, p, c, f, meal, grams, quantity, unitLabel, unit = unit, barcode = barcode)
                showAddDialog = false; selectedMealType = null
                prefillProduct = null; prefillEntry = null
                viewModel.clearScanResult(); viewModel.clearSearch()
            }
        )
    }

    editingEntry?.let { entry ->
        EditFoodEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onDelete = {
                viewModel.deleteEntry(entry)
                editingEntry = null
            },
            onConfirm = { updated ->
                viewModel.updateEntry(updated)
                editingEntry = null
            }
        )
    }

    if (showCreatineDialog) {
        var grams by remember { mutableStateOf("5") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatineDialog = false },
            containerColor = NothingDark,
            title = { Text("Créatine", style = MaterialTheme.typography.titleMedium, color = NothingWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NLabel("SUIVI QUOTIDIEN · 5 G PAR DÉFAUT", size = 8.sp, color = NothingGrey2)
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it },
                        label = { Text("Grammage (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = nothingTextFieldColors()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optionnel)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = nothingTextFieldColors()
                    )
                    if (creatineToday.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            creatineToday.forEach { log ->
                                Box(
                                    Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(if (log.taken) NothingBlue.copy(alpha = 0.2f) else NothingDeep)
                                        .border(1.dp, NothingBorderMid, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    NLabel(if (log.taken) "✓ ${log.grams}g" else "✗ ${log.grams}g", size = 7.sp, color = NothingGrey1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = grams.replace(',', '.').toFloatOrNull() ?: 5f
                        viewModel.addCreatine(taken = true, grams = g, notes = notes)
                        showCreatineDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingDeep, contentColor = NothingWhite),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Enregistrer", color = NothingWhite) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatineDialog = false }) { Text("Annuler", color = NothingGrey2) }
            }
        )
    }
}

// ── 3-segment macro split (mono tones, discrete) ───────────────────────────────

@Composable
fun MealSection(
    mealType: MealType,
    entries: List<FoodEntry>,
    onAddClick: () -> Unit,
    onEntryClick: (FoodEntry) -> Unit
) {
    val (label, time) = when (mealType) {
        MealType.BREAKFAST -> "PETIT-DÉJEUNER" to "08:00"
        MealType.LUNCH     -> "DÉJEUNER" to "13:00"
        MealType.DINNER    -> "DÎNER" to "20:00"
        MealType.SNACK     -> "COLLATION" to "16:00"
    }
    val mealCalories = entries.sumOf { it.calories }

    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "$time · $label") {
        Column(Modifier.padding(top = 4.dp)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(1.dp)) // équilibre centré
                if (mealCalories > 0) NumText("$mealCalories", fontSize = 15.sp)
            }
            Spacer(Modifier.height(6.dp))

            entries.forEach { entry ->
                FoodRow(entry = entry, onClick = { onEntryClick(entry) })
            }

            // add row
            Row(Modifier.fillMaxWidth().clickable(onClick = onAddClick).padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = NothingGrey2, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(10.dp))
                NLabel("AJOUTER", color = NothingGrey2)
            }
        }
    }
}

@Composable
private fun FoodRow(entry: FoodEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .border(0.dp, Color.Transparent).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val portion = when {
            (entry.quantity ?: 0f) > 0f -> "${numStrUs(entry.quantity!!)} ${entry.unitLabel ?: "u"}"
            (entry.grams ?: 0f) > 0f    -> "${entry.grams!!.toInt()} ${entry.unit}"
            else -> null
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.name, color = NothingWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (portion != null) {
                    Spacer(Modifier.width(6.dp))
                    NLabel("· $portion", size = 8.sp, color = NothingGrey1)
                }
            }
            Spacer(Modifier.height(3.dp))
            NLabel("P ${entry.protein.toInt()}  G ${entry.carbs.toInt()}  L ${entry.fat.toInt()}", size = 8.sp)
        }
        NumText("${entry.calories}", fontSize = 15.sp)
        Spacer(Modifier.width(3.dp))
        NLabel("KCAL", size = 8.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
}

// ── Quick-add : favoris + récents, 1 tap = ajout au repas déduit de l'heure ────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAddRow(
    favorites: List<FoodEntry>,
    recents: List<FoodEntry>,
    onQuickAdd: (FoodEntry) -> Unit,
    onLongPress: (FoodEntry) -> Unit
) {
    WidgetForm(modifier = Modifier.fillMaxWidth(), title = "QUICK ADD · FAVORIS & RÉCENTS") {
        Column(Modifier.padding(top = 14.dp)) {
            if (favorites.isNotEmpty()) {
                NLabel("FAVORIS · TAP POUR AJOUTER", size = 12.sp, color = DataOrange, modifier = Modifier.padding(horizontal = PAD))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = PAD)) {
                    items(favorites, key = { "qa_fav_" + it.name }) { fe ->
                        QuickAddChip(fe.name, star = true, kcal = fe.calories,
                            onClick = { onQuickAdd(fe) }, onLongPress = { onLongPress(fe) })
                    }
                }
            }
            if (recents.isNotEmpty()) {
                if (favorites.isNotEmpty()) Spacer(Modifier.height(10.dp))
                NLabel("RÉCENTS", size = 8.sp, color = NothingGrey2, modifier = Modifier.padding(horizontal = PAD))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = PAD)) {
                    items(recents.take(10), key = { "qa_rec_" + it.name }) { fe ->
                        QuickAddChip(fe.name, star = false, kcal = fe.calories,
                            onClick = { onQuickAdd(fe) }, onLongPress = { onLongPress(fe) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAddChip(name: String, star: Boolean, kcal: Int, onClick: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .border(1.dp, NothingBorderMid, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (star) Icon(Icons.Rounded.Star, null, tint = NothingYellow, modifier = Modifier.size(11.dp))
        Text(name.take(22), color = NothingWhite, fontSize = 12.sp, maxLines = 1)
        NLabel("${kcal}", size = 8.sp, color = NothingGrey2)
    }
}

// Base nutritionnelle pour 100 g — permet de recalculer les macros selon la portion.
private data class Per100gBasis(
    val kcal: Int, val protein: Float, val carbs: Float, val fat: Float,
    val gramsPerUnit: Float? = null, val unitLabel: String? = null,
    val category: String? = null
)

// ── Add food dialog (catalogue + portion g / ×unités + scan prefill) ───────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodDialog(
    initialMealType: MealType,
    prefill: ScannedProduct? = null,
    prefillEntry: FoodEntry? = null,
    recentFoods: List<FoodEntry> = emptyList(),
    favoriteFoods: List<FoodEntry> = emptyList(),
    foodItems: List<FoodItem> = emptyList(),
    searchState: FoodSearchState = FoodSearchState.Idle,
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onToggleFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onSaveFoodItem: (FoodItem) -> Unit = {},
    onAutoSaveCatalogue: (name: String, kcal: Int, protein: Float, carbs: Float, fat: Float, category: String) -> Unit = { _, _, _, _, _, _ -> },
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Float, Float, Float, MealType, Float?, Float?, String?, String, String) -> Unit
) {
    var name     by remember { mutableStateOf(prefill?.name ?: prefillEntry?.name ?: "") }
    var calories by remember { mutableStateOf(prefillEntry?.calories?.toString() ?: "") }
    var protein  by remember { mutableStateOf(prefillEntry?.let { numStrUs(it.protein) } ?: "") }
    var carbs    by remember { mutableStateOf(prefillEntry?.let { numStrUs(it.carbs) } ?: "") }
    var fat      by remember { mutableStateOf(prefillEntry?.let { numStrUs(it.fat) } ?: "") }
    var mealType by remember { mutableStateOf(prefillEntry?.mealType ?: initialMealType) }
    var unit     by remember { mutableStateOf(prefillEntry?.unit ?: "g") }
    // Suppression du barcode au profit d'un suivi de portion proportionnel

    val matchedCatalogueItem = remember(prefillEntry, foodItems) {
        prefillEntry?.let { fe -> foodItems.find { it.name.equals(fe.name, ignoreCase = true) } }
    }

    // Base pour 100 g — vient du scan, d'une recherche en ligne, d'un aliment du catalogue ou match quick-add. Null = saisie libre.
    var basis by remember {
        mutableStateOf(
            prefill?.let { Per100gBasis(it.calories, it.protein, it.carbs, it.fat, category = it.category) }
                ?: matchedCatalogueItem?.let { Per100gBasis(it.kcalPer100g, it.proteinPer100g, it.carbsPer100g, it.fatPer100g, it.gramsPerUnit, it.unitLabel, it.category) }
        )
    }
    // Produit issu de l'API (scan ou recherche) → sauvegarde catalogue automatique à la validation
    var fromApi by remember { mutableStateOf(prefill != null) }
    // Portion : mode poids (g) ou unités (×N). Unités possible seulement si gramsPerUnit != null.
    var unitsMode by remember {
        mutableStateOf(
            (prefillEntry != null && (prefillEntry!!.quantity ?: 0f) > 0f) ||
            (matchedCatalogueItem?.gramsPerUnit != null && prefillEntry?.quantity != null && prefillEntry!!.quantity!! > 0f)
        )
    }
    var grams by remember {
        mutableStateOf(
            when {
                prefillEntry != null && (prefillEntry!!.quantity ?: 0f) > 0f && basis?.gramsPerUnit != null -> ""
                prefillEntry?.grams != null && prefillEntry!!.grams!! > 0f -> numStrUs(prefillEntry!!.grams!!)
                matchedCatalogueItem?.gramsPerUnit != null && prefillEntry?.grams != null -> numStrUs(prefillEntry!!.grams!!)
                else -> ((prefill?.servingSizeG ?: prefillEntry?.grams ?: 100f).takeIf { it > 0f } ?: 100f).toInt().toString()
            }
        )
    }
    var units by remember {
        mutableStateOf(
            if (prefillEntry?.quantity != null && (prefillEntry?.quantity ?: 0f) > 0f)
                numStrUs(prefillEntry!!.quantity!!)
            else "1"
        )
    }

    // true = auto-calcul des macros depuis les calories (30/45/25) — seulement en saisie libre
    var autoMacros by remember { mutableStateOf(prefill == null && prefillEntry == null && matchedCatalogueItem == null) }
    // Pour le quick-add (prefillEntry sans base 100g) : recalcul proportionnel au ratio portion / portion originale
    var manualMacros by remember { 
        mutableStateOf(
            (prefillEntry == null && matchedCatalogueItem == null) || 
            (prefillEntry != null && matchedCatalogueItem == null && (prefillEntry!!.grams ?: 0f) <= 0f && (prefillEntry!!.quantity ?: 0f) <= 0f)
        ) 
    }

    fun effectiveGrams(): Float =
        if (unitsMode) {
            val n = units.replace(',', '.').toFloatOrNull() ?: 0f
            n * (basis?.gramsPerUnit ?: 0f)
        } else grams.replace(',', '.').toFloatOrNull() ?: 0f

    // Recalcule les macros depuis la base 100 g à chaque changement de portion.
    LaunchedEffect(basis, unitsMode, grams, units) {
        val b = basis ?: return@LaunchedEffect
        val g = effectiveGrams()
        calories = ((b.kcal * g) / 100f).toInt().toString()
        protein  = scaleToServing(b.protein, g)
        carbs    = scaleToServing(b.carbs, g)
        fat      = scaleToServing(b.fat, g)
    }

    // Recalcule proportionnel pour quick-add (entrée pré-remplie sans base 100g)
    LaunchedEffect(basis, unitsMode, grams, units, prefillEntry) {
        if (basis == null && prefillEntry != null && !manualMacros) {
            val oldPortion = if (unitsMode) {
                (prefillEntry!!.quantity ?: 1f)
            } else {
                (prefillEntry!!.grams ?: 100f).takeIf { it > 0f } ?: 100f
            }
            val newPortion = if (unitsMode) (units.replace(',', '.').toFloatOrNull() ?: 1f) else (grams.replace(',', '.').toFloatOrNull() ?: 100f)
            if (oldPortion > 0f && newPortion > 0f) {
                val r = newPortion / oldPortion
                val cal = (prefillEntry!!.calories * r).toInt().coerceAtLeast(0)
                calories = cal.toString()
                protein  = numStrUs(prefillEntry!!.protein * r)
                carbs    = numStrUs(prefillEntry!!.carbs * r)
                fat      = numStrUs(prefillEntry!!.fat * r)
            }
        }
    }

    // Auto-macros depuis les calories (mode saisie libre uniquement)
    LaunchedEffect(calories, basis) {
        if (autoMacros && basis == null) {
            val cal = calories.replace(',', '.').toFloatOrNull() ?: return@LaunchedEffect
            if (cal > 0) {
                protein = (cal * 0.30f / 4f).toInt().toString()
                carbs   = (cal * 0.45f / 4f).toInt().toString()
                fat     = (cal * 0.25f / 9f).toInt().toString()
            } else {
                protein = ""; carbs = ""; fat = ""
            }
        }
    }

    // Recherche dans le catalogue
    var query by remember { mutableStateOf("") }
    val catalogueMatches = remember(foodItems, query) {
        (if (query.isBlank()) foodItems
         else foodItems.filter { it.name.contains(query, ignoreCase = true) }).take(12)
    }

    val selectItem: (FoodItem) -> Unit = { fi ->
        name = fi.name
        basis = Per100gBasis(fi.kcalPer100g, fi.proteinPer100g, fi.carbsPer100g, fi.fatPer100g,
            fi.gramsPerUnit, fi.unitLabel, fi.category)
        autoMacros = false
        if (fi.gramsPerUnit != null) { unitsMode = true; units = "1" }
        else { unitsMode = false; grams = "100" }
    }

    // Sélection d'un résultat de recherche en ligne → se comporte comme un scan (valeurs /100 g).
    val selectScanned: (ScannedProduct) -> Unit = { sp ->
        name = sp.name
        basis = Per100gBasis(sp.calories, sp.protein, sp.carbs, sp.fat, category = sp.category)
        fromApi = true
        autoMacros = false
        unitsMode = false
        grams = ((sp.servingSizeG ?: 100f).toInt()).toString()
        onClearSearch()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (prefill != null) {
                    Box(Modifier.size(7.dp).background(NothingYellow, RoundedCornerShape(50)))
                }
                Text(if (prefill != null) "Produit scanné" else "Ajouter un aliment",
                    style = MaterialTheme.typography.titleMedium, color = NothingWhite)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (prefill != null) {
                    NLabel("VALEURS REMPLIES PAR L'IA", color = NothingYellow, size = 8.sp)
                }

                // ── Recherche en ligne (OpenFoodFacts par nom) ──────────────────
                if (prefill == null) {
                    var onlineQuery by remember { mutableStateOf("") }
                    NLabel("RECHERCHER EN LIGNE", size = 8.sp, color = NothingGrey2)
                    OutlinedTextField(
                        value = onlineQuery,
                        onValueChange = { onlineQuery = it; onSearch(it) },
                        label = { Text("Nom de l'aliment (ex. banane)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = nothingTextFieldColors(),
                        trailingIcon = {
                            if (searchState is FoodSearchState.Loading) {
                                CircularProgressIndicator(
                                    color = DataMint, strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp))
                            } else if (onlineQuery.isNotBlank()) {
                                IconButton(onClick = { onlineQuery = ""; onClearSearch() }) {
                                    Icon(Icons.Rounded.Close, null, tint = NothingGrey2, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                    when (val s = searchState) {
                        is FoodSearchState.Results ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                s.products.take(8).forEach { sp ->
                                    OnlineResultRow(sp) { selectScanned(sp); onlineQuery = "" }
                                }
                            }
                        FoodSearchState.Empty ->
                            NLabel("AUCUN RÉSULTAT", size = 8.sp, color = NothingGrey3)
                        else -> {}
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
                }

                // ── Quick-add : favoris + récents (uniquement saisie manuelle) ──
                if (prefill == null && (favoriteFoods.isNotEmpty() || recentFoods.isNotEmpty())) {
                    val fill: (FoodEntry) -> Unit = { fe ->
                        name = fe.name
                        basis = null
                        calories = fe.calories.toString()
                        protein = numStrUs(fe.protein)
                        carbs   = numStrUs(fe.carbs)
                        fat     = numStrUs(fe.fat)
                        autoMacros = false   // valeurs réelles enregistrées
                    }
                    if (favoriteFoods.isNotEmpty()) {
                        NLabel("FAVORIS", size = 12.sp, color = DataOrange)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(favoriteFoods, key = { "fav_" + it.name }) { fe ->
                                QuickFoodChip(fe.name, star = true) { fill(fe) }
                            }
                        }
                    }
                    if (recentFoods.isNotEmpty()) {
                        NLabel("RÉCENTS", size = 8.sp, color = NothingGrey2)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(recentFoods, key = { "rec_" + it.name }) { fe ->
                                QuickFoodChip(fe.name, star = false) { fill(fe) }
                            }
                        }
                    }
                }

                // ── Catalogue d'aliments (poids/unités précis) ──────────────────
                if (prefill == null) {
                    NLabel("CATALOGUE", size = 8.sp, color = NothingGrey2)
                    OutlinedTextField(value = query, onValueChange = { query = it },
                        label = { Text("Rechercher un aliment") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = nothingTextFieldColors())
                    if (catalogueMatches.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(catalogueMatches, key = { "cat_" + it.id }) { fi ->
                                QuickFoodChip(
                                    "${fi.name} · ${fi.kcalPer100g}/100g",
                                    star = false
                                ) { selectItem(fi) }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
                }

                val isFav = favoriteFoods.any { it.name.equals(name, ignoreCase = true) }
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Aliment") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, colors = nothingTextFieldColors(),
                    trailingIcon = {
                        if (name.isNotBlank()) {
                            IconButton(onClick = { onToggleFavorite(name, !isFav) }) {
                                Icon(
                                    if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Favori",
                                    tint = if (isFav) NothingYellow else NothingGrey2,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    })

                // ── Unité (g / ml) ──────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        colors = nothingTextFieldColors())
                }

                // ── Portion (poids g / ×unités) — dès qu'une base 100 g est connue ──
                basis?.let { b ->
                    PortionSelector(
                        basis = b,
                        unitsMode = unitsMode, onModeChange = { unitsMode = it },
                        grams = grams, onGramsChange = { grams = it },
                        units = units, onUnitsChange = { units = it },
                        effectiveGrams = effectiveGrams()
                    )
                }

                // ── Portion sans base (entrée pré-remplie) — grammage / quantité modifiables ──
                if (basis == null && prefillEntry != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            NLabel("PORTION", size = 8.sp, color = NothingGrey2)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PortionModeChip("POIDS", selected = !unitsMode) { unitsMode = false }
                                PortionModeChip("QUANTITÉ", selected = unitsMode) { unitsMode = true }
                            }
                        }
                        if (unitsMode) {
                            OutlinedTextField(
                                value = units,
                                onValueChange = { units = it },
                                label = { Text("Quantité (${prefillEntry!!.unitLabel ?: "unités"})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                colors = nothingTextFieldColors()
                            )
                            val qty = units.replace(',', '.').toFloatOrNull() ?: 0f
                            val gVal = prefillEntry!!.grams ?: (qty * (prefillEntry!!.grams ?: 0f)).coerceAtLeast(0f)
                            NLabel("= ${gVal.toInt()} G", color = NothingGrey2, size = 8.sp)
                        } else {
                            OutlinedTextField(
                                value = grams,
                                onValueChange = { grams = it },
                                label = { Text("Poids (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                colors = nothingTextFieldColors()
                            )
                        }
                    }
                }

                OutlinedTextField(value = calories, onValueChange = { calories = it; manualMacros = true },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = nothingTextFieldColors())

                // Macro row header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    NLabel("MACROS (g)", size = 8.sp, color = NothingGrey2)
                    // Toggle auto/manuel — seulement en saisie libre (sinon portion pilote)
                    if (basis == null) {
                        Row(
                            Modifier.clickable { autoMacros = !autoMacros }
                                .background(
                                    if (autoMacros) NothingBlue.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(5.dp).background(
                                if (autoMacros) NothingBlue else NothingGrey3, CircleShape))
                            NLabel(if (autoMacros) "AUTO" else "MANUEL",
                                color = if (autoMacros) NothingBlue else NothingGrey2, size = 8.sp)
                        }
                    } else {
                        NLabel("CALCULÉ", color = NothingGrey2, size = 8.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it; autoMacros = false; basis = null; manualMacros = true },
                        label = { Text("Prot") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true,
                        colors = nothingTextFieldColors())
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it; autoMacros = false; basis = null; manualMacros = true },
                        label = { Text("Gluc") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true,
                        colors = nothingTextFieldColors())
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it; autoMacros = false; basis = null; manualMacros = true },
                        label = { Text("Lip") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true,
                        colors = nothingTextFieldColors())
                }

                // ── Enregistrer dans le catalogue (valeurs pour 100 g) ──────────
                if (name.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val item = buildCatalogueItem(name, basis, calories, protein, carbs, fat, unit)
                            if (item != null) onSaveFoodItem(item)
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.BookmarkBorder, null, tint = NothingGrey2, modifier = Modifier.size(14.dp))
                        NLabel(
                            if (basis != null) "ENREGISTRER DANS LE CATALOGUE"
                            else "ENREGISTRER (VALEURS = POUR 100 G)",
                            color = NothingGrey2, size = 8.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { type ->
                        val l = when (type) {
                            MealType.BREAKFAST -> "Matin"; MealType.LUNCH -> "Midi"
                            MealType.DINNER -> "Soir"; MealType.SNACK -> "Snack"
                        }
                        FilterChip(selected = mealType == type, onClick = { mealType = type },
                            label = { Text(l, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = calories.replace(',', '.').toIntOrNull() ?: 0
                    // replace(',', '.') pour gérer la locale française (virgule décimale)
                    val p   = protein.replace(',', '.').toFloatOrNull() ?: 0f
                    val c   = carbs.replace(',', '.').toFloatOrNull()   ?: 0f
                    val f   = fat.replace(',', '.').toFloatOrNull()     ?: 0f
                    // Produit issu de l'API → sauvegarde catalogue automatique (anti-doublon côté repo)
                    if (fromApi && basis != null && name.isNotBlank() && cal > 0) {
                        onAutoSaveCatalogue(
                            name.trim(), basis!!.kcal, basis!!.protein, basis!!.carbs, basis!!.fat,
                            basis!!.category ?: ""
                        )
                    }
                    // Portion à enregistrer pour l'affichage ("4 œufs" / "150 g")
                    val gOut: Float?; val qOut: Float?; val uOut: String?
                    when {
                        basis != null && unitsMode -> {
                            qOut = units.replace(',', '.').toFloatOrNull()
                            gOut = effectiveGrams().takeIf { it > 0f }
                            uOut = basis?.unitLabel
                        }
                        basis != null -> {
                            gOut = effectiveGrams().takeIf { it > 0f }
                            qOut = null; uOut = null
                        }
                        else -> {
                            // Sans base : utiliser directement grams / units du dialogue (entrée pré-remplie)
                            if (unitsMode) {
                                qOut = units.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }
                                gOut = grams.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }
                                uOut = prefillEntry?.unitLabel ?: unit
                            } else {
                                gOut = grams.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }
                                qOut = null; uOut = null
                            }
                        }
                    }
                    if (name.isNotBlank() && cal > 0)
                        onConfirm(name, cal, p, c, f, mealType, gOut, qOut, uOut, unit, "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = NothingDeep, contentColor = NothingWhite),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Ajouter", color = NothingWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = NothingGrey2) }
        }
    )
}

// Construit un FoodItem (valeurs pour 100 g) depuis l'état du dialogue.
private fun buildCatalogueItem(
    name: String, basis: Per100gBasis?,
    calories: String, protein: String, carbs: String, fat: String,
    unit: String, barcode: String = ""
): FoodItem? {
    if (basis != null) {
        return FoodItem(
            name = name,
            kcalPer100g = basis.kcal,
            proteinPer100g = basis.protein,
            carbsPer100g = basis.carbs,
            fatPer100g = basis.fat,
            gramsPerUnit = basis.gramsPerUnit,
            unitLabel = basis.unitLabel,
            category = basis.category ?: "",
            unit = unit,
            barcode = barcode
        )
    }
    val cal = calories.replace(',', '.').toIntOrNull() ?: return null
    return FoodItem(
        name = name,
        kcalPer100g = cal,
        proteinPer100g = protein.replace(',', '.').toFloatOrNull() ?: 0f,
        carbsPer100g = carbs.replace(',', '.').toFloatOrNull() ?: 0f,
        fatPer100g = fat.replace(',', '.').toFloatOrNull() ?: 0f,
        unit = unit,
        barcode = barcode
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortionSelector(
    basis: Per100gBasis,
    unitsMode: Boolean, onModeChange: (Boolean) -> Unit,
    grams: String, onGramsChange: (String) -> Unit,
    units: String, onUnitsChange: (String) -> Unit,
    effectiveGrams: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            NLabel("PORTION")
            // Toggle Poids / Unités (Unités visible seulement si l'aliment a une unité)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PortionModeChip("POIDS", selected = !unitsMode) { onModeChange(false) }
                if (basis.gramsPerUnit != null) {
                    PortionModeChip((basis.unitLabel ?: "UNITÉ").uppercase(), selected = unitsMode) { onModeChange(true) }
                }
            }
        }
        if (unitsMode && basis.gramsPerUnit != null) {
            OutlinedTextField(value = units, onValueChange = onUnitsChange,
                label = { Text("Nombre de ${basis.unitLabel ?: "unités"}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = nothingTextFieldColors())
            NLabel("= ${effectiveGrams.toInt()} G", color = NothingGrey2)
        } else {
            OutlinedTextField(value = grams, onValueChange = onGramsChange,
                label = { Text("Poids (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = nothingTextFieldColors())
        }
    }
}

@Composable
private fun PortionModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (selected) NothingBlue.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (selected) NothingBlue else NothingBorderMid, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NLabel(label, color = if (selected) NothingBlue else NothingGrey2, size = 8.sp)
    }
}

private fun scaleToServing(per100g: Float, servingG: Float?): String {
    val g = servingG ?: 100f
    // Locale.US garantit le point décimal (évite "22,5" sur appareils français)
    return String.format(java.util.Locale.US, "%.1f", (per100g * g) / 100f)
}

// Float → String avec point décimal (sans .0 inutile), pour pré-remplir les champs
private fun numStrUs(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", v)

@Composable
private fun OnlineResultRow(product: ScannedProduct, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = NothingGrey2, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(product.name, color = NothingWhite, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        NLabel("${product.calories}/100G", size = 8.sp, color = NothingGrey2)
    }
}

@Composable
private fun QuickFoodChip(label: String, star: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .border(1.dp, NothingBorderMid, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (star) Icon(Icons.Rounded.Star, null, tint = NothingYellow, modifier = Modifier.size(11.dp))
        Text(label.take(22), color = NothingWhite, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun nothingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    // Focus = bordure qui s'éclaircit (règle Nothing) — pas de couleur décorative
    focusedBorderColor      = NothingGrey2,
    unfocusedBorderColor    = NothingBorderMid,
    focusedLabelColor       = NothingGrey1,
    unfocusedLabelColor     = NothingGrey2,
    cursorColor             = NothingWhite,
    focusedTextColor        = NothingWhite,
    unfocusedTextColor      = NothingWhite,
    disabledTextColor       = NothingGrey3,
    focusedContainerColor   = NothingDark2,
    unfocusedContainerColor = NothingDark2,
    disabledContainerColor  = NothingDark
)

// ── Édition d'une entrée existante : portion (ratio) + macros + repas ──────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodEntryDialog(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (FoodEntry) -> Unit
) {
    val hasUnits = (entry.quantity ?: 0f) > 0f && !entry.unitLabel.isNullOrBlank()
    var unitsMode by remember { mutableStateOf(hasUnits) }

    // oldPortion fixe au mode d'origine de l'entrée (pour un ratio cohérent)
    val basePortion = if (hasUnits) (entry.quantity ?: entry.grams ?: 0f) else (entry.grams ?: entry.quantity ?: 0f)
    val oldPortion = basePortion

    var portion by remember {
        mutableStateOf(
            when {
                unitsMode && entry.quantity != null && entry.quantity!! > 0f -> numStrUs(entry.quantity!!)
                !unitsMode && entry.grams != null && entry.grams!! > 0f -> numStrUs(entry.grams!!)
                unitsMode && entry.quantity != null -> numStrUs(entry.quantity!!)
                else -> {
                    val v = if (unitsMode) (entry.quantity ?: entry.grams ?: 0f) else (entry.grams ?: entry.quantity ?: 0f)
                    if (v > 0f) numStrUs(v) else ""
                }
            }
        )
    }
    var calories by remember { mutableStateOf(entry.calories.toString()) }
    var protein  by remember { mutableStateOf(numStrUs(entry.protein)) }
    var carbs    by remember { mutableStateOf(numStrUs(entry.carbs)) }
    var fat      by remember { mutableStateOf(numStrUs(entry.fat)) }
    var mealType by remember { mutableStateOf(entry.mealType) }

    // false tant que l'utilisateur n'a pas édité une macro à la main → portion pilote le ratio
    var manualMacros by remember { mutableStateOf(oldPortion <= 0f) }

    // Quand le mode poids/unités change, convertir la portion si possible
    LaunchedEffect(unitsMode) {
        val currentVal = portion.replace(',', '.').toFloatOrNull() ?: return@LaunchedEffect
        if (currentVal <= 0f) return@LaunchedEffect
        // Si l'entrée a les deux valeurs, on peut estimer le poids par unité
        if (entry.grams != null && entry.grams!! > 0f && entry.quantity != null && entry.quantity!! > 0f) {
            val gramsPerUnit = entry.grams!! / entry.quantity!!
            val newVal = if (unitsMode) {
                // Passer du poids aux unités : np / gramsPerUnit
                (currentVal / gramsPerUnit).takeIf { it > 0 }
            } else {
                // Passer des unités au poids : np * gramsPerUnit
                (currentVal * gramsPerUnit).takeIf { it > 0 }
            }
            if (newVal != null && newVal > 0f) {
                portion = numStrUs(newVal)
                manualMacros = false
            }
        }
    }

    // Recalcul proportionnel : nouvelles macros = macros d'origine × (nouvelle portion / ancienne dans le même mode)
    LaunchedEffect(portion) {
        if (!manualMacros) {
            val np = portion.replace(',', '.').toFloatOrNull() ?: return@LaunchedEffect
            if (np > 0f) {
                val oldPortionInMode = if (unitsMode) (entry.quantity ?: entry.grams ?: 0f) else (entry.grams ?: entry.quantity ?: 0f)
                if (oldPortionInMode > 0f) {
                    val r = np / oldPortionInMode
                    calories = (entry.calories * r).toInt().coerceAtLeast(0).toString()
                    protein  = numStrUs(entry.protein * r)
                    carbs    = numStrUs(entry.carbs * r)
                    fat      = numStrUs(entry.fat * r)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = {
            Text("Modifier", style = MaterialTheme.typography.titleMedium, color = NothingWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(entry.name, color = NothingWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                if (oldPortion > 0f || hasUnits) {
                    // Toggle poids / unités si l'aliment a des unités
                    if (hasUnits) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            NLabel("PORTION", size = 8.sp, color = NothingGrey2)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PortionModeChip("POIDS", selected = !unitsMode) {
                                    unitsMode = false
                                    manualMacros = false
                                }
                                PortionModeChip((entry.unitLabel ?: "UNITÉ").uppercase(), selected = unitsMode) {
                                    unitsMode = true
                                    manualMacros = false
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = portion,
                        onValueChange = { portion = it },
                        label = {
                            Text(if (unitsMode) "Quantité (${entry.unitLabel ?: "unités"})" else "Poids mangé (g)")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = nothingTextFieldColors()
                    )
                    if (!manualMacros) {
                        NLabel("MACROS RECALCULÉES AU RATIO", size = 8.sp, color = NothingGrey3)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = calories, onValueChange = { calories = it; manualMacros = true },
                        label = { Text("Kcal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true, colors = nothingTextFieldColors())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it; manualMacros = true },
                        label = { Text("Prot") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true, colors = nothingTextFieldColors())
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it; manualMacros = true },
                        label = { Text("Gluc") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true, colors = nothingTextFieldColors())
                    OutlinedTextField(value = fat, onValueChange = { fat = it; manualMacros = true },
                        label = { Text("Lip") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true, colors = nothingTextFieldColors())
                }

                NLabel("REPAS", size = 8.sp, color = NothingGrey2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { type ->
                        val l = when (type) {
                            MealType.BREAKFAST -> "Matin"; MealType.LUNCH -> "Midi"
                            MealType.DINNER -> "Soir"; MealType.SNACK -> "Snack"
                        }
                        FilterChip(selected = mealType == type, onClick = { mealType = type },
                            label = { Text(l, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = calories.replace(',', '.').toIntOrNull() ?: return@Button
                    if (cal < 0) return@Button
                    val p = protein.replace(',', '.').toFloatOrNull() ?: 0f
                    val c = carbs.replace(',', '.').toFloatOrNull()   ?: 0f
                    val f = fat.replace(',', '.').toFloatOrNull()     ?: 0f

                    // Portion : selon le mode actuel (poids ou unités)
                    val np = portion.replace(',', '.').toFloatOrNull()
                    val newG = if (!unitsMode && np != null && np > 0f) np else entry.grams?.takeIf { it > 0f }
                    val newQ = if (unitsMode && np != null && np > 0f) np else entry.quantity?.takeIf { it > 0f }

                    onConfirm(
                        entry.copy(
                            calories = cal, protein = p, carbs = c, fat = f,
                            mealType = mealType,
                            grams = newG, quantity = newQ
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NothingDeep, contentColor = NothingWhite),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Enregistrer", color = NothingWhite) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = NothingRed, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Supprimer", color = NothingRed)
                }
                TextButton(onClick = onDismiss) { Text("Annuler", color = NothingGrey2) }
            }
        }
    )
}
