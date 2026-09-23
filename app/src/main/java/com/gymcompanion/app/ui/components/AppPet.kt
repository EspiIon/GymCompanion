package com.gymcompanion.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcompanion.app.data.model.PetMood
import com.gymcompanion.app.ui.theme.DataLavender
import com.gymcompanion.app.ui.theme.DataMint
import com.gymcompanion.app.ui.theme.DataOrange
import com.gymcompanion.app.ui.theme.NothingBorderMid
import com.gymcompanion.app.ui.theme.NothingBlack
import com.gymcompanion.app.ui.theme.NothingCardSurface
import com.gymcompanion.app.ui.theme.NothingDark
import com.gymcompanion.app.ui.theme.NothingDark2
import com.gymcompanion.app.ui.theme.NothingGrey1
import com.gymcompanion.app.ui.theme.NothingGrey2
import com.gymcompanion.app.ui.theme.NothingRed
import com.gymcompanion.app.ui.theme.NothingWhite
import com.gymcompanion.app.ui.theme.PetCream
import com.gymcompanion.app.ui.theme.PetCreamShade
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.viewmodel.PetUiState

/**
 * Mascotte compacte de l'application. Elle est volontairement dessinée en Canvas
 * afin de rester nette à toutes les tailles et de ne dépendre d'une image bitmap.
 */
@Composable
fun AppPet(
    mood: PetMood,
    name: String,
    modifier: Modifier = Modifier,
    variant: Int = 0,
    colorIndex: Int = 0,
    interactive: Boolean = true,
    onTap: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition("app-pet")
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_600, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val petColor = when (colorIndex) {
        1 -> DataMint
        2 -> DataLavender
        3 -> DataOrange
        else -> PetCream
    }
    val petShade = when (colorIndex) {
        1 -> Color(0xFF2F8F78)
        2 -> Color(0xFF7665C7)
        3 -> Color(0xFFC76C2D)
        else -> PetCreamShade
    }
    val bodyWidthFactor = when (variant) {
        1 -> 0.9f
        2 -> 1.08f
        3 -> 0.82f
        else -> 1f
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = "$name, compagnon. Toucher pour interagir." }
            .then(if (interactive) Modifier.pointerInput(name) {
                detectTapGestures(onTap = { onTap() })
            } else Modifier)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 16f
            val centerX = size.width / 2f
            val centerY = size.height / 2f + bob * unit * 0.16f
            val bodyWidth = unit * 10.2f * bodyWidthFactor
            val bodyHeight = unit * (if (variant == 3) 9.2f else 8.4f)
            val topLeft = Offset(centerX - bodyWidth / 2f, centerY - bodyHeight * 0.40f)

            // Petites oreilles triangulaires.
            val leftEar = Path().apply {
                moveTo(topLeft.x + unit * 1.2f, topLeft.y + unit * 0.7f)
                lineTo(topLeft.x + unit * 2.2f, topLeft.y - unit * 1.1f)
                lineTo(topLeft.x + unit * 3.2f, topLeft.y + unit * 0.2f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(topLeft.x + bodyWidth - unit * 1.2f, topLeft.y + unit * 0.7f)
                lineTo(topLeft.x + bodyWidth - unit * 2.2f, topLeft.y - unit * 1.1f)
                lineTo(topLeft.x + bodyWidth - unit * 3.2f, topLeft.y + unit * 0.2f)
                close()
            }
            drawPath(leftEar, petShade)
            drawPath(rightEar, petShade)

            // Corps, tête et pattes : une silhouette compacte lisible à 48 dp.
            drawRoundRect(
                color = petColor,
                topLeft = topLeft,
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(unit * 3.2f)
            )
            drawRoundRect(
                color = petColor,
                topLeft = Offset(centerX - unit * 4.2f, centerY + unit * 2.0f),
                size = Size(unit * 3.0f, unit * 3.1f),
                cornerRadius = CornerRadius(unit * 1.4f)
            )
            drawRoundRect(
                color = petColor,
                topLeft = Offset(centerX + unit * 1.2f, centerY + unit * 2.0f),
                size = Size(unit * 3.0f, unit * 3.1f),
                cornerRadius = CornerRadius(unit * 1.4f)
            )

            drawFace(mood, centerX, centerY - unit * 0.35f, unit)
        }
    }
}

private fun DrawScope.drawFace(mood: PetMood, centerX: Float, faceY: Float, unit: Float) {
    val sleepy = mood == PetMood.SAD || mood == PetMood.MISERABLE
    val eyeY = faceY + if (sleepy) unit * 0.15f else 0f

    drawEye(Offset(centerX - unit * 2.05f, eyeY), unit, sleepy)
    drawEye(Offset(centerX + unit * 2.05f, eyeY), unit, sleepy)

    val mouthWidth = when (mood) {
        PetMood.ECSTATIC, PetMood.HAPPY -> unit * 1.55f
        PetMood.NEUTRAL -> unit * 0.9f
        PetMood.SAD, PetMood.MISERABLE -> unit * 0.75f
    }
    val smile = when (mood) {
        PetMood.ECSTATIC -> unit * 0.9f
        PetMood.HAPPY -> unit * 0.65f
        PetMood.NEUTRAL -> unit * 0.18f
        else -> -unit * 0.45f
    }
    val mouth = Path().apply {
        moveTo(centerX - mouthWidth, faceY + unit * 1.45f)
        quadraticTo(centerX, faceY + unit * 1.45f + smile, centerX + mouthWidth, faceY + unit * 1.45f)
    }
    drawPath(
        path = mouth,
        color = NothingBlack,
        style = Stroke(width = unit * 0.42f, cap = StrokeCap.Round)
    )

    if (mood == PetMood.ECSTATIC) {
        drawCircle(
            color = NothingRed,
            radius = unit * 0.48f,
            center = Offset(centerX, faceY + unit * 1.85f)
        )
    }
}

private fun DrawScope.drawEye(center: Offset, unit: Float, sleepy: Boolean) {
    if (sleepy) {
        drawLine(
            color = NothingBlack,
            start = Offset(center.x - unit * 0.58f, center.y),
            end = Offset(center.x + unit * 0.58f, center.y),
            strokeWidth = unit * 0.42f,
            cap = StrokeCap.Round
        )
    } else {
        drawRoundRect(
            color = NothingBlack,
            topLeft = Offset(center.x - unit * 0.52f, center.y - unit * 0.72f),
            size = Size(unit * 1.04f, unit * 1.44f),
            cornerRadius = CornerRadius(unit * 0.42f)
        )
    }
}

/** Bandeau compact qui accompagne l'utilisateur dans tous les écrans principaux. */
@Composable
fun AppPetBar(
    state: PetUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(NothingCardSurface)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${state.name}, ${state.goalsMet} objectifs sur ${state.goalsTotal}. Toucher pour ouvrir la fiche du compagnon."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppPet(
            mood = state.mood,
            name = state.name,
            variant = state.variant,
            colorIndex = state.colorIndex,
            onTap = {},
            interactive = false,
            modifier = Modifier.size(58.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.name,
                    color = NothingWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.goalsMet}/${state.goalsTotal}",
                    color = NothingGrey1,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.message,
                color = NothingGrey2,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        NLabel(
            text = "VOIR",
            color = NothingGrey2,
            size = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPetSheet(
    state: PetUiState,
    onDismiss: () -> Unit,
    onPet: () -> Unit,
    onRename: (String) -> Unit,
    onVariant: (Int) -> Unit,
    onColor: (Int) -> Unit
) {
    var name by remember(state.name) { mutableStateOf(state.name) }
    val progress = if (state.goalsTotal > 0) state.goalsMet.toFloat() / state.goalsTotal else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NothingCardSurface,
        contentColor = NothingWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppPet(
                    mood = state.mood,
                    name = state.name,
                    variant = state.variant,
                    colorIndex = state.colorIndex,
                    onTap = onPet,
                    modifier = Modifier.size(118.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    NLabel("COMPAGNON", color = NothingGrey2, size = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(state.name, color = NothingWhite, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(state.message, color = NothingGrey1, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NLabel("OBJECTIFS DU JOUR", color = NothingGrey2, size = 12.sp)
                NLabel("${state.goalsMet} / ${state.goalsTotal}", color = NothingGrey1, size = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            GlyphSegmentBar(
                progress = progress,
                color = DataMint,
                segmentCount = state.goalsTotal.coerceAtLeast(1),
                segmentHeight = 8f
            )
            state.goals.forEach { goal ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(if (goal.met) DataMint else NothingDark2, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (goal.met) Text("✓", color = NothingBlack, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(goal.label, color = NothingGrey1, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            NLabel("APPARENCE", color = NothingGrey2, size = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Pixel", "Mochi", "Suki", "Nova").forEachIndexed { index, label ->
                    NothingActionButton(
                        onClick = { onVariant(index) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, fontSize = 12.sp, color = if (state.variant == index) DataOrange else NothingWhite)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(PetCream, DataMint, DataLavender, DataOrange).forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .border(
                                width = if (state.colorIndex == index) 3.dp else 1.dp,
                                color = if (state.colorIndex == index) NothingWhite else NothingBorderMid,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable { onColor(index) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                label = { Text("Nom du compagnon") },
                singleLine = true,
                colors = nothingTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onRename(name); onDismiss() },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NothingDark, contentColor = NothingWhite),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enregistrer")
                }
                Button(
                    onClick = onPet,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingDark2,
                        contentColor = NothingWhite
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Jouer")
                }
            }
        }
    }
}
