package com.gymcompanion.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.gymcompanion.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ── Nothing OS Card ───────────────────────────────────────────────────────────
// Design strict Nothing : surface plate (gris très sombre #171717), sans bordure,
// angles très marqués (24 dp), zéro ombre, totalement plat. En-tête blanc cassé.
// Ratio d'or (φ ≈ 1.618) — utilisé dans la mise en page des widgets Nothing
val GoldenRatio = 1.618f
val GoldenRatioInverse = 0.618f

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    pet: com.gymcompanion.app.viewmodel.PetUiState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColor = NothingCardSurface
    val shape = RoundedCornerShape(24.dp)
    val petTransition = rememberInfiniteTransition("card-pet-border")
    val petTravel by petTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card-pet-travel"
    )
    val base = modifier
        .clip(shape)
        .background(cardColor)

    Box(
        modifier = base
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
            .padding(18.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = NothingWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp
                    )
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.Rounded.NorthEast,
                            contentDescription = "Ouvrir",
                            tint = NothingGrey2,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            content()
        }
        pet?.let { state ->
            AppPet(
                mood = state.mood,
                name = state.name,
                variant = state.variant,
                colorIndex = state.colorIndex,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .offset(x = (-18 * petTravel).dp),
                onTap = {}
            )
        }
    }
}

// Alias — all old code using GlassCard keeps working
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = NothingCard(modifier = modifier, onClick = onClick, content = content)

@Composable
fun WidgetForm(
    modifier: Modifier = Modifier,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    pet: com.gymcompanion.app.viewmodel.PetUiState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    NothingCard(
        modifier = modifier.fillMaxWidth(),
        title = title,
        onClick = onClick,
        pet = pet
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

// ── Standard Floating Action Buttons ───────────────────────────────────────────
@Composable
fun NothingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NothingDeep,
            contentColor = NothingWhite
        ),
        border = BorderStroke(1.dp, NothingBorderMid),
        content = content
    )
}

@Composable
fun StandardFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Add,
    contentDescription: String? = null
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = NothingDeep,
        contentColor = NothingWhite,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, NothingBorderMid, RoundedCornerShape(14.dp))
    ) { Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp)) }
}

@Composable
fun SmallFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Add,
    contentDescription: String? = null,
    tinted: Boolean = false
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = NothingDeep,
        contentColor = if (tinted) NothingYellow else NothingWhite,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, if (tinted) NothingYellow.copy(alpha = 0.6f) else NothingBorderMid, RoundedCornerShape(10.dp))
    ) { Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp)) }
}

// ── Glyph Segment Bar ─────────────────────────────────────────────────────────
// Nothing's signature visual: discrete rectangular segments, like Glyph LEDs.
// Filled segments = progress. Unfilled = track. Gaps between = Nothing aesthetic.
@Composable
fun GlyphSegmentBar(
    progress: Float,          // 0f..1f
    color: Color,
    modifier: Modifier = Modifier,
    segmentCount: Int = 20,
    segmentGap: Float = 3f,
    segmentHeight: Float = 6f
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "glyph"
    )
    val filledCount = (animated * segmentCount).toInt()

    Canvas(modifier = modifier.fillMaxWidth().height(segmentHeight.dp)) {
        val totalGap = segmentGap * (segmentCount - 1)
        val segW = (size.width - totalGap) / segmentCount

        for (i in 0 until segmentCount) {
            val x = i * (segW + segmentGap)
            val isFilled = i < filledCount
            drawRoundRect(
                color = if (isFilled) color else color.copy(alpha = 0.10f),
                topLeft = Offset(x, 0f),
                size = Size(segW.coerceAtLeast(1f), size.height),
                cornerRadius = CornerRadius(2f)
            )
        }
    }
}

// ── Macro segment bar (3-colour glyph version) ────────────────────────────────
@Composable
fun MacroGlyphBar(
    protein: Float,
    carbs: Float,
    fat: Float,
    modifier: Modifier = Modifier,
    segmentCount: Int = 24,
    segmentGap: Float = 2f,
    segmentHeight: Float = 5f
) {
    val total = (protein + carbs + fat).coerceAtLeast(0.001f)
    val proteinSeg = ((protein / total) * segmentCount).toInt()
    val carbsSeg = ((carbs / total) * segmentCount).toInt()
    // fat gets the remainder

    Canvas(modifier = modifier.fillMaxWidth().height(segmentHeight.dp)) {
        val totalGap = segmentGap * (segmentCount - 1)
        val segW = (size.width - totalGap) / segmentCount

        for (i in 0 until segmentCount) {
            val x = i * (segW + segmentGap)
            val col = when {
                i < proteinSeg -> ProteinColor
                i < proteinSeg + carbsSeg -> CarbsColor
                else -> FatColor
            }
            drawRoundRect(
                color = col,
                topLeft = Offset(x, 0f),
                size = Size(segW.coerceAtLeast(1f), size.height),
                cornerRadius = CornerRadius(2f)
            )
        }
    }
}

// ── Arc progress (thin, mechanical — no rounded caps) ─────────────────────────
@Composable
fun ArcProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 6f,
    trackAlpha: Float = 0.08f
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "arc"
    )
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f - strokeWidth / 2f
        val topLeft = Offset(size.width / 2f - r, size.height / 2f - r)
        val arcSize = Size(r * 2f, r * 2f)

        // Track
        drawArc(
            color = color.copy(alpha = trackAlpha),
            startAngle = 135f, sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft, size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Progress — Butt cap = sharp ends, more mechanical
        if (animated > 0f) {
            drawArc(
                color = color,
                startAngle = 135f, sweepAngle = 270f * animated,
                useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
            )
        }
    }
}

// ── Linear progress bar (thin, flat) ─────────────────────────────────────────
@Composable
fun LinearProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(color.copy(alpha = 0.10f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color)
        )
    }
}

// ── Circular progress card ────────────────────────────────────────────────────
@Composable
fun CircularProgressCard(
    title: String,
    current: Int,
    goal: Int,
    unit: String,
    color: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )

    NothingCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                // Arc mécanique (caps carrés) — cohérent avec le reste du système
                ArcProgress(
                    progress = animatedProgress,
                    color = color,
                    strokeWidth = 4f,
                    modifier = Modifier.size(56.dp)
                )
                icon()
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingGrey2
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = current.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFamily,
                        color = color
                    )
                    Text(
                        text = " / $goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = NothingGrey2,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                GlyphSegmentBar(progress = animatedProgress, color = color, segmentCount = 12)
            }
        }
    }
}

// ── Streak badge ──────────────────────────────────────────────────────────────
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "streak")
    val alpha by transition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NothingDeep)
            .border(1.dp, StreakColor.copy(alpha = alpha * 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Signal dot — monoline, pas d'emoji (règle Nothing)
        Box(
            Modifier
                .size(6.dp)
                .background(StreakColor.copy(alpha = alpha), CircleShape)
        )
        Text(
            text = "$streak",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFamily,
            color = StreakColor
        )
    }
}

// ── Section header (Nothing-style: ALL CAPS + wide tracking) ──────────────────
@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = NothingGrey2
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = action.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = NothingGrey1
                )
            }
        }
    }
}

// ── Stat chip ─────────────────────────────────────────────────────────────────
@Composable
fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF171717))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFamily,
            color = color
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NothingGrey2
        )
    }
}

// ── Divider ───────────────────────────────────────────────────────────────────
@Composable
fun NothingDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NothingDivider)
    )
}

// ── Glyph decorative line (horizontal) ───────────────────────────────────────
// Mimics Nothing Phone's Glyph interface — a row of small lit segments.
@Composable
fun GlyphDecoration(
    modifier: Modifier = Modifier,
    filled: Int = 5,
    total: Int = 16,
    color: Color = NothingBorderMid
) {
    Canvas(modifier = modifier.height(4.dp).fillMaxWidth()) {
        val gap = 4f
        val segW = (size.width - gap * (total - 1)) / total
        for (i in 0 until total) {
            val x = i * (segW + gap)
            drawRoundRect(
                color = if (i < filled) color else color.copy(alpha = 0.15f),
                topLeft = Offset(x, 0f),
                size = Size(segW.coerceAtLeast(1f), size.height),
                cornerRadius = CornerRadius(1f)
            )
        }
    }
}



// ══════════════════════════════════════════════════════════════════════════════
//  v3 · Nothing OS primitives (dot-matrix system)
// ══════════════════════════════════════════════════════════════════════════════

// ── Numeric text — respects the global dot-matrix toggle (Doto / Space Mono) ───
// Règle officielle : les fontes mécaniques ne mélangent pas les tailles.
// Sous DOT_MIN_SP le dot-matrix est illisible → repli automatique Space Mono.
@Composable
fun NumText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = NothingWhite,
    fontWeight: FontWeight = FontWeight.SemiBold,
    letterSpacing: TextUnit = 0.5.sp
) {
    val dotFamily = LocalNumericFont.current
    val family = if (fontSize >= DOT_MIN_SP.sp || dotFamily == MonoFamily) dotFamily else MonoFamily
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = family,
        fontWeight = fontWeight,
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        maxLines = 1
    )
}

// ── Dotted divider — Nothing signature hairline ───────────────────────────────
@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = NothingBorder,
    dotRadius: Dp = 0.7.dp,
    gap: Dp = 4.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val r = dotRadius.toPx()
        val g = gap.toPx()
        val y = size.height / 2f
        var x = r
        while (x <= size.width) {
            drawCircle(color = color, radius = r, center = Offset(x, y))
            x += g
        }
    }
}

// ── Mono label — ALL CAPS, wide tracking (used across every v3 screen) ────────
@Composable
fun NLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NothingGrey2,
    size: TextUnit = 12.sp,
    letterSpacing: TextUnit = 0.8.sp
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size,
        letterSpacing = letterSpacing,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Sparkline — minimal trend line with an end dot ────────────────────────────
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = NothingBlue,
    dotColor: Color = NothingWhite,
    strokeWidth: Dp = 2.5.dp,
    targetValue: Float? = null
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val allVals = if (targetValue != null) values + targetValue else values
        val chartMin = allVals.min()
        val chartMax = allVals.max()
        val range = (chartMax - chartMin).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val pad = strokeWidth.toPx() * 2
        val h = size.height - pad * 2
        val pts = values.mapIndexed { i, v ->
            Offset(i * stepX, pad + (h - ((v - chartMin) / range) * h))
        }
        for (i in 0 until pts.size - 1) {
            drawLine(color, pts[i], pts[i + 1], strokeWidth.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(dotColor, strokeWidth.toPx() * 1.6f, pts.last())
        // Target weight dashed line — accent officiel (N-Yellow)
        targetValue?.let { target ->
            val targetY = pad + (h - ((target - chartMin) / range) * h)
            val dash = 8f; val gap = 5f; var x = 0f
            while (x < size.width) {
                drawLine(
                    color = NothingYellow.copy(alpha = 0.65f),
                    start = Offset(x, targetY),
                    end = Offset(minOf(x + dash, size.width), targetY),
                    strokeWidth = 1.5f, cap = StrokeCap.Round
                )
                x += dash + gap
            }
        }
    }
}

// ── Segmented arc — discrete dots around a 270° arc (Glyph LED look) ───────────
@Composable
fun SegmentedArc(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NothingWhite,
    trackColor: Color = NothingBorder,
    dotCount: Int = 56,
    dotRadius: Dp = 1.6.dp,
    startAngle: Float = 135f,
    sweepAngle: Float = 270f
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "segarc"
    )
    val filled = (animated * dotCount).toInt()
    Canvas(modifier = modifier) {
        val rad = dotRadius.toPx()
        val radius = size.minDimension / 2f - rad
        val cx = size.width / 2f
        val cy = size.height / 2f
        for (i in 0 until dotCount) {
            val t = if (dotCount > 1) i / (dotCount - 1f) else 0f
            val a = Math.toRadians((startAngle + sweepAngle * t).toDouble())
            val x = cx + radius * cos(a).toFloat()
            val y = cy + radius * sin(a).toFloat()
            drawCircle(
                color = if (i < filled) color else trackColor,
                radius = rad,
                center = Offset(x, y)
            )
        }
    }
}

// ── Dual trend chart — muscle (solid white) vs fat (dashed red) ───────────────
// Différenciation par pattern (règle Nothing) : plein = muscle, pointillé = graisse.
// Chaque série est normalisée indépendamment (les unités kg/% n'ont pas le même domaine).
data class TrendPoint(val epochDay: Long, val value: Float)

@Composable
fun DualTrendChart(
    muscle: List<TrendPoint>,
    fat: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if ((muscle.size < 2 && fat.size < 2)) return@Canvas

        val allDays = (muscle.map { it.epochDay } + fat.map { it.epochDay })
        val dMin = allDays.min().toFloat()
        val dMax = allDays.max().toFloat()
        val daySpan = (dMax - dMin).takeIf { it > 0f } ?: 1f

        val padTop = 8f; val padBottom = 8f
        val chartH = size.height - padTop - padBottom

        fun xOf(day: Long): Float =
            ((day - dMin) / daySpan) * size.width

        fun yOf(v: Float, min: Float, max: Float): Float {
            val span = (max - min).takeIf { it > 0.001f } ?: 1f
            return padTop + chartH - ((v - min) / span) * chartH
        }

        // ── Grille horizontale discrète (25/50/75 %) ─────────────────────────
        listOf(0.25f, 0.5f, 0.75f).forEach { t ->
            val y = padTop + chartH * t
            drawLine(NothingBorder, Offset(0f, y), Offset(size.width, y), 1f)
        }

        // ── Graisse — pointillé N-Red + points aux mesures ───────────────────
        if (fat.size >= 2) {
            val fMin = fat.minOf { it.value }; val fMax = fat.maxOf { it.value }
            val fatPath = Path()
            fat.sortedBy { it.epochDay }.forEachIndexed { i, p ->
                val pt = Offset(xOf(p.epochDay), yOf(p.value, fMin, fMax))
                if (i == 0) fatPath.moveTo(pt.x, pt.y) else fatPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                fatPath,
                color = NothingRed,
                style = Stroke(width = 2f, cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f)))
            )
            fat.forEach { p ->
                drawCircle(NothingRed, 4f, Offset(xOf(p.epochDay), yOf(p.value, fMin, fMax)))
            }
        }

        // ── Muscle — trait plein blanc + dot final ───────────────────────────
        if (muscle.size >= 2) {
            val mMin = muscle.minOf { it.value }; val mMax = muscle.maxOf { it.value }
            val mPath = Path()
            muscle.sortedBy { it.epochDay }.forEachIndexed { i, p ->
                val pt = Offset(xOf(p.epochDay), yOf(p.value, mMin, mMax))
                if (i == 0) mPath.moveTo(pt.x, pt.y) else mPath.lineTo(pt.x, pt.y)
            }
            drawPath(mPath, NothingWhite, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            val lastPt = muscle.maxBy { it.epochDay }
            drawCircle(NothingWhite, 6f, Offset(xOf(lastPt.epochDay), yOf(lastPt.value, mMin, mMax)))
            drawCircle(
                NothingBlack, 3f,
                Offset(xOf(lastPt.epochDay), yOf(lastPt.value, mMin, mMax))
            )
        }
    }
}
