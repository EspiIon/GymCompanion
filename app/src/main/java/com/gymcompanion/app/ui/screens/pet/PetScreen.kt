package com.gymcompanion.app.ui.screens.pet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.data.model.FoodEntry
import com.gymcompanion.app.data.model.PetMood
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.PetUiState
import com.gymcompanion.app.viewmodel.PetViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

private val PAD = 24.dp

/** Couleur des yeux style Cozmo — écrans OLED cyan-bleu. */
private val CozmoBlue = NothingYellow

// ── Mood label ────────────────────────────────────────────────────────────────

fun moodLabel(m: PetMood) = when (m) {
    PetMood.ECSTATIC  -> "RAYONNANT"
    PetMood.HAPPY     -> "CONTENT"
    PetMood.NEUTRAL   -> "NEUTRE"
    PetMood.SAD       -> "TRISTE"
    PetMood.MISERABLE -> "MALHEUREUX"
}

// ── Particule cœur ────────────────────────────────────────────────────────────

private data class HeartParticle(val id: Long, val offsetX: Float)

@Composable
private fun HeartParticleView(particle: HeartParticle, onEnd: () -> Unit) {
    val y   = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(particle.id) {
        launch { y.animateTo(-90f, tween(900, easing = EaseOutCubic)) }
        launch {
            kotlinx.coroutines.delay(300)
            alpha.animateTo(0f, tween(600))
        }
        kotlinx.coroutines.delay(900)
        onEnd()
    }
    Box(
        Modifier
            .offset(x = particle.offsetX.dp, y = y.value.dp)
            .alpha(alpha.value)
    ) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            tint = NothingRed,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Pet Glyph Face ────────────────────────────────────────────────────────────

/**
 * Visage dot-matrix animé style Nothing/glyph.
 *
 * Yeux 2×3 style écrans Cozmo, trame de fond paramétrable,
 * regard qui suit le doigt ([lookAt]), état somnolent si [sleepy].
 */
@Composable
fun PetGlyphFace(
    mood: PetMood,
    color: Color,
    modifier: Modifier = Modifier,
    lookAt: Offset? = null,
    sleepy: Boolean = false,
    trameStyle: Int = 0
) {
    // Sourire cible (−1 = moue profonde … +1 = grand sourire)
    val targetSmile = when (mood) {
        PetMood.ECSTATIC  -> 1f
        PetMood.HAPPY     -> 0.80f   // sourire bien visible
        PetMood.NEUTRAL   -> 0f
        PetMood.SAD       -> -0.45f
        PetMood.MISERABLE -> -0.95f
    }
    val smile by animateFloatAsState(targetSmile, tween(700, easing = EaseInOutCubic), label = "smile")

    // Vivacité des animations selon l'humeur (somnolent → très lent)
    val live = when {
        sleepy            -> 0.15f
        mood == PetMood.ECSTATIC  -> 1f
        mood == PetMood.HAPPY     -> 0.80f
        mood == PetMood.NEUTRAL   -> 0.50f
        mood == PetMood.SAD       -> 0.25f
        else -> 0.10f
    }

    val inf = rememberInfiniteTransition("petFace")

    // Flottement vertical
    val bob by inf.animateFloat(
        -1f, 1f,
        infiniteRepeatable(
            tween((3000 - (live * 1400).toInt()), easing = EaseInOutSine),
            RepeatMode.Reverse
        ), "bob"
    )
    // Regard horizontal
    val gazeX by inf.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(3800, easing = EaseInOutSine), RepeatMode.Reverse),
        "gazeX"
    )
    // Regard vertical (léger)
    val gazeY by inf.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2700, easing = EaseInOutSine), RepeatMode.Reverse),
        "gazeY"
    )
    // Clignement — fenêtre de fermeture = 5,5 % du cycle de 4,5 s
    val blinkPhase by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        "blink"
    )
    var eyeOpen = if (blinkPhase < 0.055f)
        (abs(blinkPhase / 0.055f - 0.5f) * 2f).coerceIn(0f, 1f)
    else 1f
    // Somnolent : paupières mi-closes permanentes
    if (sleepy) eyeOpen *= 0.45f

    Canvas(modifier) {
        val W = size.width; val H = size.height
        val unit   = minOf(W, H) / 8f
        val dotR   = unit * 0.17f

        val bobY = bob  * unit * 0.18f * live

        // Regard : errance libre, ou dirigé vers le doigt ([lookAt])
        var gX = gazeX * unit * 0.20f * live
        var gY = gazeY * unit * 0.09f * live
        if (lookAt != null) {
            val dx = lookAt.x - W / 2f
            val dy = lookAt.y - H * 0.40f
            val len = kotlin.math.sqrt(dx * dx + dy * dy).takeIf { it > 1f } ?: 1f
            gX = (dx / len) * unit * 0.38f
            gY = (dy / len) * unit * 0.24f
        }

        // ── Trame de fond ────────────────────────────────────────────────────
        when (trameStyle) {
            0 -> for (col in 0 until 9) for (row in 0 until 9) {
                drawCircle(
                    color.copy(alpha = 0.065f), dotR * 0.78f,
                    Offset(W * (col + 0.5f) / 9f, H * (row + 0.5f) / 9f)
                )
            }
            1 -> { // rangées glyph horizontales
                for (row in 0 until 6) {
                    val y = H * (row + 0.5f) / 6f
                    val segs = 7
                    for (s in 0 until segs) {
                        val w = W / segs
                        drawRoundRect(
                            color.copy(alpha = 0.07f),
                            topLeft = Offset(s * w + w * 0.22f, y - dotR * 0.5f),
                            size = Size(w * 0.56f, dotR),
                            cornerRadius = CornerRadius(dotR * 0.5f)
                        )
                    }
                }
            }
            else -> Unit
        }

        // ── Dimensions des "écrans" Cozmo ────────────────────────────────────
        val eyeW   = unit * 1.42f          // largeur d'un œil
        val eyeH   = unit * 0.78f          // hauteur de base (avant blink)
        val eyeCorner = eyeW * 0.28f       // arrondi des coins

        // ── Yeux style Cozmo — écrans rectangulaires lumineux ────────────────
        // Inclinaison des yeux selon l'humeur : triste/malheureux = coin intérieur
        // qui descend (comme des sourcils froncés intégrés à l'œil)
        val sadTilt = when (mood) {
            PetMood.MISERABLE -> 14f
            PetMood.SAD       -> 8f
            else              -> 0f
        }

        val eyeBaseY  = H * 0.40f + bobY + gY
        val leftEyeX  = W * 0.30f + gX
        val rightEyeX = W * 0.70f + gX

        /**
         * Dessine un œil style Cozmo (écran OLED arrondi + halo lumineux).
         * [tiltDeg] > 0 = sens horaire (coin droit descend = triste pour l'œil gauche).
         */
        fun drawCozmoEye(ex: Float, ey: Float, tiltDeg: Float) {
            val hw  = eyeW / 2f
            val hh  = (eyeH / 2f) * eyeOpen.coerceAtLeast(0.04f)
            val eyeColor = CozmoBlue

            withTransform({ rotate(tiltDeg, Offset(ex, ey)) }) {
                // ── Halo lumineux (3 couches de glow) ─────────────────────
                drawRoundRect(
                    eyeColor.copy(alpha = 0.06f),
                    topLeft = Offset(ex - hw * 2.1f, ey - hh * 2.1f),
                    size    = Size(hw * 4.2f, hh * 4.2f),
                    cornerRadius = CornerRadius(eyeCorner * 2.1f)
                )
                drawRoundRect(
                    eyeColor.copy(alpha = 0.15f),
                    topLeft = Offset(ex - hw * 1.45f, ey - hh * 1.45f),
                    size    = Size(hw * 2.9f, hh * 2.9f),
                    cornerRadius = CornerRadius(eyeCorner * 1.45f)
                )
                drawRoundRect(
                    eyeColor.copy(alpha = 0.32f),
                    topLeft = Offset(ex - hw * 1.15f, ey - hh * 1.15f),
                    size    = Size(hw * 2.3f, hh * 2.3f),
                    cornerRadius = CornerRadius(eyeCorner * 1.15f)
                )
                // ── Écran principal ─────────────────────────────────────
                drawRoundRect(
                    eyeColor,
                    topLeft = Offset(ex - hw, ey - hh),
                    size    = Size(hw * 2f, hh * 2f),
                    cornerRadius = CornerRadius(eyeCorner)
                )
                // ── Reflet intérieur (bande lumineuse dans la partie haute) ──
                if (eyeOpen > 0.25f) {
                    drawRoundRect(
                        Color.White.copy(alpha = 0.20f * eyeOpen),
                        topLeft = Offset(ex - hw * 0.65f, ey - hh * 0.90f),
                        size    = Size(hw * 1.30f, hh * 0.70f),
                        cornerRadius = CornerRadius(eyeCorner * 0.35f)
                    )
                }
                // ── Point brillant (shine) ───────────────────────────────
                if (eyeOpen > 0.15f) {
                    drawCircle(
                        Color.White.copy(alpha = 0.80f * eyeOpen),
                        minOf(hw, hh) * 0.24f,
                        Offset(ex - hw * 0.38f, ey - hh * 0.38f)
                    )
                }
            }
        }

        // Œil gauche : tilt positif (CW) → coin droit/intérieur descend = triste
        // Œil droit  : tilt négatif (CCW) → coin gauche/intérieur descend = triste
        drawCozmoEye(leftEyeX,  eyeBaseY, tiltDeg =  sadTilt)
        drawCozmoEye(rightEyeX, eyeBaseY, tiltDeg = -sadTilt)

        // ── Bouche (adaptée à l'humeur) ───────────────────────────────────
        val mBaseY = H * 0.68f + bobY
        drawMouth(smile, W, mBaseY, color, dotR)
    }
}

/**
 * Bouche parabolique dont la forme et le nombre de points varient avec l'humeur.
 *
 * smile > 0  → sourire ∪   (centre monte)
 * smile < 0  → moue ∩      (centre descend)
 * smile = 0  → ligne plate (5 points)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMouth(
    smile: Float,
    W: Float,
    mBaseY: Float,
    color: Color,
    dotR: Float
) {
    val absSmile = kotlin.math.abs(smile)

    // Nombre de points et largeur selon l'intensité du sourire/moue
    val count  = when {
        absSmile < 0.15f -> 5
        absSmile < 0.55f -> 7
        else             -> 9
    }
    val halfW = W * when {
        absSmile < 0.15f -> 0.17f
        absSmile < 0.55f -> 0.22f
        else             -> 0.28f
    }
    val amp = (W / 8f) * when {
        absSmile < 0.15f -> 0f
        absSmile < 0.55f -> 0.55f
        else             -> 0.90f
    }

    // Courbe parabolique
    // En coords écran (Y croît vers le bas) :
    //   smile > 0 → centre descend (+) → ∪ = sourire ✓
    //   smile < 0 → centre monte (−) → ∩ = moue ✓
    for (i in 0 until count) {
        val t  = if (count > 1) (i.toFloat() / (count - 1)) * 2f - 1f else 0f
        val mx = W * 0.5f + t * halfW
        val my = mBaseY + smile * amp * (1f - t * t)
        drawCircle(color, dotR * 1.05f, Offset(mx, my))
    }

    // Rangée de "dents" pour ECSTATIC (smile > 0.80)
    if (smile > 0.80f) {
        val alpha = ((smile - 0.80f) / 0.20f).coerceIn(0f, 1f)
        val toothY = mBaseY + smile * amp - dotR * 1.8f
        val toothHalfW = halfW * 0.55f
        for (i in 0 until 5) {
            val t  = (i.toFloat() / 4f) * 2f - 1f
            val tx = W * 0.5f + t * toothHalfW
            drawCircle(color.copy(alpha = alpha * 0.55f), dotR * 0.70f, Offset(tx, toothY))
        }
        // Langue
        val tongueAlpha = alpha * 0.55f
        drawCircle(
            NothingRed.copy(alpha = tongueAlpha), dotR * 0.90f,
            Offset(W * 0.5f, mBaseY + (W / 8f) * 0.50f)
        )
    }
}

// ── Pet Screen ────────────────────────────────────────────────────────────────

@Composable
fun PetScreen(
    onBack: () -> Unit = {},
    viewModel: PetViewModel = hiltViewModel()
) {
    val pet by viewModel.state.collectAsStateWithLifecycle()
    val recentFoods by viewModel.recentFoods.collectAsStateWithLifecycle()
    val petStyle by viewModel.petStyle.collectAsStateWithLifecycle()
    val targetColor = if (pet.mood == PetMood.SAD || pet.mood == PetMood.MISERABLE) NothingRed else NothingWhite
    val accent by animateColorAsState(targetColor, tween(700), label = "petAccent")

    var showFeedDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
    ) {
        PetHeader(name = pet.name, mood = pet.mood, accent = accent, onBack = onBack)
        Spacer(Modifier.height(16.dp))

        // ── Grande face cliquable ──────────────────────────────────────────
        PetFaceWithPetting(
            pet = pet,
            accent = accent,
            trameStyle = petStyle,
            onPet = { viewModel.pet() }
        )

        Spacer(Modifier.height(24.dp))
        PetHappinessSection(pet = pet, accent = accent)
        Spacer(Modifier.height(8.dp))
        DottedDivider(Modifier.padding(horizontal = PAD))
        Spacer(Modifier.height(20.dp))
        PetGoalsSection(pet = pet, accent = accent)
        Spacer(Modifier.height(8.dp))
        DottedDivider(Modifier.padding(horizontal = PAD))
        Spacer(Modifier.height(20.dp))
        PetInteractSection(
            currentName = pet.name,
            onRename = { viewModel.rename(it) },
            onFeed = { showFeedDialog = true },
            onStyle = { showStyleDialog = true },
            styleLabel = when (petStyle) {
                1 -> "GLYPH"; 2 -> "NU"; else -> "POINTS"
            }
        )
        Spacer(Modifier.height(110.dp))
    }

    if (showFeedDialog) {
        FeedDialog(
            recentFoods = recentFoods,
            onDismiss = { showFeedDialog = false },
            onFeed = {
                viewModel.feed(it)
                showFeedDialog = false
            }
        )
    }

    if (showStyleDialog) {
        StyleDialog(
            current = petStyle,
            onDismiss = { showStyleDialog = false },
            onSelect = { viewModel.setStyle(it); showStyleDialog = false }
        )
    }
}

// ── Face cliquable dans son housing « appareil » + cœurs ──────────────────────

@Composable
private fun PetFaceWithPetting(
    pet: PetUiState,
    accent: Color,
    trameStyle: Int,
    onPet: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val hearts = remember { mutableStateListOf<HeartParticle>() }
    var lookTarget by remember { mutableStateOf<Offset?>(null) }

    // Le regard revient à l'errance après 900 ms
    LaunchedEffect(lookTarget) {
        if (lookTarget != null) {
            kotlinx.coroutines.delay(900)
            lookTarget = null
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(224.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NothingDeep)
                .border(1.dp, NothingBorderMid, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onPet()
                        // Les yeux regardent le doigt (coords locales → face 180dp centrée)
                        lookTarget = Offset(offset.x - (size.width - 180.dp.toPx()) / 2f,
                                            offset.y - (size.height - 180.dp.toPx()) / 2f)
                        scope.launch {
                            scale.animateTo(
                                1.06f,
                                tween(140, easing = EaseOutCubic)
                            )
                            scale.animateTo(1f, tween(220, easing = EaseInOutCubic))
                        }
                        repeat((4..5).random()) {
                            hearts += HeartParticle(
                                id = System.nanoTime() + it,
                                offsetX = Random.nextFloat() * 120f - 60f
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Vis de coin — détail industriel Nothing
            listOf(
                Alignment.TopStart to Offset(1f, 1f),
                Alignment.TopEnd to Offset(-1f, 1f),
                Alignment.BottomStart to Offset(1f, -1f),
                Alignment.BottomEnd to Offset(-1f, -1f)
            ).forEach { (align, _) ->
                Box(
                    Modifier
                        .align(align)
                        .padding(10.dp)
                        .size(5.dp)
                        .background(NothingBorderStrong, CircleShape)
                )
            }

            PetGlyphFace(
                mood  = pet.mood,
                color = accent,
                lookAt = lookTarget,
                sleepy = pet.happiness < 20,
                trameStyle = trameStyle,
                modifier = Modifier.size(184.dp).scale(scale.value)
            )

            // Particules cœur flottantes
            hearts.forEach { h ->
                HeartParticleView(particle = h, onEnd = { hearts -= h })
            }
        }
    }

    // ── LED statut : un segment par objectif du jour ────────────────────────
    Spacer(Modifier.height(14.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        pet.goals.forEach { goal ->
            Box(
                Modifier
                    .width(34.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (goal.met) accent else NothingBorder)
            )
        }
    }

    // Indice sous la face
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        NLabel("APPUYER POUR CARESSER", color = NothingGrey3, size = 7.sp)
    }
}

// ── Sections ──────────────────────────────────────────────────────────────────

@Composable
private fun PetHeader(name: String, mood: PetMood, accent: Color, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = PAD, top = 26.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ChevronLeft, "Retour", tint = NothingGrey1, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            NLabel("COMPAGNON", color = NothingRed.copy(alpha = 0.7f), size = 9.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(accent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    name.uppercase(),
                    fontFamily = LocalNumericFont.current,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingWhite
                )
            }
        }
        NLabel(moodLabel(mood), color = accent, size = 9.sp)
    }
}

@Composable
private fun PetHappinessSection(pet: PetUiState, accent: Color) {
    Column(Modifier.padding(horizontal = PAD)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            NLabel("BONHEUR", color = NothingGrey2, size = 9.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${pet.happiness}",
                    fontFamily = LocalNumericFont.current,
                    fontWeight = FontWeight.Medium,
                    fontSize = 36.sp,
                    color = accent
                )
                Spacer(Modifier.width(3.dp))
                NLabel("%", color = accent, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        GlyphSegmentBar(
            progress = pet.happiness / 100f,
            color = accent,
            segmentCount = 20,
            segmentHeight = 7f
        )
        Spacer(Modifier.height(10.dp))
        Text(pet.message, color = NothingGrey1, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun PetGoalsSection(pet: PetUiState, accent: Color) {
    Column(Modifier.padding(horizontal = PAD)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NLabel("OBJECTIFS DU JOUR", color = NothingGrey2, size = 9.sp)
            NLabel("${pet.goalsMet} / ${pet.goalsTotal}", color = accent, size = 9.sp)
        }
        Spacer(Modifier.height(14.dp))
        pet.goals.forEachIndexed { i, goal ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .border(1.dp, if (goal.met) accent else NothingBorderMid, CircleShape)
                        .background(if (goal.met) accent else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (goal.met) Icon(Icons.Rounded.Check, null, tint = NothingBlack, modifier = Modifier.size(11.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    goal.label,
                    color = if (goal.met) NothingGrey1 else NothingWhite,
                    fontSize = 13.sp,
                    fontWeight = if (goal.met) FontWeight.Normal else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PetInteractSection(
    currentName: String,
    onRename: (String) -> Unit,
    onFeed: () -> Unit,
    onStyle: () -> Unit,
    styleLabel: String
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var nameValue by remember(currentName) { mutableStateOf(currentName) }

    Column(Modifier.padding(horizontal = PAD)) {
        NLabel("INTERAGIR", color = NothingGrey2, size = 9.sp)
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Nourrir — ouvre le quick-add depuis les aliments récents
            OutlinedButton(
                onClick = onFeed,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NothingBorderMid)
            ) {
                Icon(Icons.Rounded.LocalDining, null, tint = NothingYellow, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nourrir", color = NothingWhite, fontSize = 12.sp)
            }
            // Style — variantes de trame du visage
            OutlinedButton(
                onClick = onStyle,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NothingBorderMid)
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = NothingYellow, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Style · $styleLabel", color = NothingWhite, fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.height(20.dp))
        NLabel("PRÉNOM", color = NothingGrey2, size = 9.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nameValue,
                onValueChange = { nameValue = it.take(16) },
                modifier = Modifier.weight(1f),
                colors = nothingTextFieldColors(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                placeholder = { Text("Pixel", color = NothingGrey3, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onRename(nameValue)
                    keyboard?.hide()
                })
            )
            Button(
                onClick = { onRename(nameValue); keyboard?.hide() },
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("OK", color = NothingWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        NLabel("Le nom apparaît sur l'accueil · max 16 caractères", size = 7.sp, color = NothingGrey3)
    }
}

// ── Dialog Nourrir : aliments récents → collation + bonheur ───────────────────
@Composable
private fun FeedDialog(
    recentFoods: List<FoodEntry>,
    onDismiss: () -> Unit,
    onFeed: (FoodEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = { Text("Nourrir", style = MaterialTheme.typography.titleMedium, color = NothingWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (recentFoods.isEmpty()) {
                    NLabel("LOGGE D'ABORD DES ALIMENTS DANS NUTRITION",
                        size = 8.sp, color = NothingGrey3,
                        modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    NLabel("COLLATION DU JOUR · +3 BONHEUR", size = 8.sp, color = NothingYellow)
                    Spacer(Modifier.height(8.dp))
                    recentFoods.forEach { fe ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFeed(fe) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fe.name, color = NothingWhite, fontSize = 13.sp,
                                maxLines = 1, modifier = Modifier.weight(1f))
                            NLabel("${fe.calories} KCAL", size = 8.sp, color = NothingGrey2)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(NothingDivider))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = NothingGrey2) }
        }
    )
}

// ── Dialog Style : variante de trame du visage ────────────────────────────────
@Composable
private fun StyleDialog(
    current: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        0 to "POINTS",
        1 to "GLYPH",
        2 to "NU"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NothingDark,
        title = { Text("Style de trame", style = MaterialTheme.typography.titleMedium, color = NothingWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (value, label) ->
                    val selected = value == current
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) NothingDark2 else Color.Transparent)
                            .border(1.dp, if (selected) NothingBorderStrong else NothingBorderMid, RoundedCornerShape(8.dp))
                            .clickable { onSelect(value) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NLabel(label, size = 10.sp, color = if (selected) NothingWhite else NothingGrey2)
                        if (selected) Icon(Icons.Rounded.Check, null,
                            tint = NothingYellow, modifier = Modifier.size(15.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = NothingGrey2) }
        }
    )
}
