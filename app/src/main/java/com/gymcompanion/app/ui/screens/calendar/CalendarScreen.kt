package com.gymcompanion.app.ui.screens.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val PAD = 24.dp
private val NutritionDotColor = NothingYellow

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val selectedMonth  by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val workoutDates   by viewModel.workoutDates.collectAsStateWithLifecycle()
    val nutritionDates by viewModel.nutritionDates.collectAsStateWithLifecycle()
    val stepDates      by viewModel.stepDates.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NothingBlack),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item(key = "header") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "ACTIVITÉ · CALENDRIER") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column {
                        NLabel("ACTIVITÉ")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Calendrier", fontFamily = LocalNumericFont.current,
                            fontWeight = FontWeight.SemiBold, fontSize = 30.sp,
                            letterSpacing = 1.sp, color = NothingWhite
                        )
                    }
                }
            }
        }

        item(key = "nav") {
            Spacer(Modifier.height(8.dp))
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "NAVIGATION MENSUELLE") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(onClick = viewModel::prevMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, null, tint = NothingWhite, modifier = Modifier.size(22.dp))
                }
                val monthName = selectedMonth.month
                    .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    .replaceFirstChar { it.uppercase() }
                Text(
                    "$monthName ${selectedMonth.year}",
                    color = NothingWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    fontFamily = LocalNumericFont.current, letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = viewModel::nextMonth,
                    enabled = selectedMonth < YearMonth.now(), // strict: no future months
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.ChevronRight, null,
                        tint = if (selectedMonth < YearMonth.now()) NothingWhite else NothingGrey3,
                        modifier = Modifier.size(22.dp)
                    )
                }
                }
            }
        }

        item(key = "weekdays") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "JOURS DE LA SEMAINE") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("L", "M", "M", "J", "V", "S", "D").forEach { d ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            NLabel(d, size = 8.sp, color = NothingGrey3)
                        }
                    }
                }
            }
        }

        item(key = "grid") {
            Spacer(Modifier.height(8.dp))
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "GRILLE DU MOIS") {
                CalendarGrid(
                    month = selectedMonth,
                    workoutDates = workoutDates,
                    nutritionDates = nutritionDates,
                    stepDates = stepDates,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "stats") {
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "STATISTIQUES DU MOIS") {
                MonthStats(
                    month = selectedMonth,
                    workoutDates = workoutDates,
                    nutritionDates = nutritionDates,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "legend") {
            Spacer(Modifier.height(8.dp))
            WidgetForm(modifier = Modifier.fillMaxWidth(), title = "LÉGENDE") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                ) {
                    LegendItem(NothingRed, "Séance")
                    LegendItem(NutritionDotColor, "Nutrition")
                    LegendItem(NothingGrey1, "Pas")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    workoutDates: Set<String>,
    nutritionDates: Set<String>,
    stepDates: Set<String>,
    modifier: Modifier = Modifier
) {
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value - 1  // Mon=0 … Sun=6
    val rows = (startOffset + daysInMonth + 6) / 7

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - startOffset + 1
                    Box(Modifier.weight(1f)) {
                        if (dayNum in 1..daysInMonth) {
                            val dateStr = month.atDay(dayNum).toString()
                            DayCell(
                                day          = dayNum,
                                isToday      = dateStr == LocalDate.now().toString(),
                                hasWorkout   = dateStr in workoutDates,
                                hasNutrition = dateStr in nutritionDates,
                                hasSteps     = dateStr in stepDates
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    hasWorkout: Boolean,
    hasNutrition: Boolean,
    hasSteps: Boolean
) {
    val hasActivity = hasWorkout || hasNutrition || hasSteps
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isToday) NothingDark2 else Color.Transparent)
            .border(
                if (isToday) 1.dp else 0.dp,
                if (isToday) NothingBorderMid else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(vertical = 5.dp, horizontal = 2.dp)
    ) {
        Text(
            text = "$day",
            color = when {
                isToday      -> NothingWhite
                hasActivity  -> NothingGrey1
                else         -> NothingGrey3
            },
            fontSize = 11.sp,
            fontFamily = LocalNumericFont.current,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasWorkout)   Box(Modifier.size(3.5.dp).background(NothingRed,          CircleShape))
            if (hasNutrition) Box(Modifier.size(3.5.dp).background(NutritionDotColor,   CircleShape))
            if (hasSteps)     Box(Modifier.size(3.5.dp).background(NothingGrey1,        CircleShape))
            // Keep height stable when no dots
            if (!hasActivity) Box(Modifier.size(3.5.dp).background(Color.Transparent))
        }
    }
}

@Composable
private fun MonthStats(
    month: YearMonth,
    workoutDates: Set<String>,
    nutritionDates: Set<String>,
    modifier: Modifier = Modifier
) {
    val monthPrefix = month.toString()  // "yyyy-MM"
    val workoutsThisMonth  = workoutDates.count { it.startsWith(monthPrefix) }
    val nutritionThisMonth = nutritionDates.count { it.startsWith(monthPrefix) }
    val daysInMonth = month.lengthOfMonth()

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        StatCol("SÉANCES", "$workoutsThisMonth", NothingRed)
        StatCol("JOURS NUTRITION", "$nutritionThisMonth", NutritionDotColor)
        StatCol("JOURS / MOIS", "$daysInMonth", NothingGrey2)
    }
}

@Composable
private fun StatCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NumText(value, fontSize = 32.sp, fontWeight = FontWeight.Medium, color = color)
        Spacer(Modifier.height(4.dp))
        NLabel(label, size = 7.sp, color = NothingGrey3)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        NLabel(label, size = 8.sp, color = NothingGrey1)
    }
}
