package com.gymcompanion.app.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymcompanion.app.ui.theme.DataBlue
import com.gymcompanion.app.ui.theme.DataMint
import com.gymcompanion.app.ui.theme.NothingBlack
import com.gymcompanion.app.ui.theme.NothingBorder
import com.gymcompanion.app.ui.theme.NothingGrey2
import com.gymcompanion.app.ui.theme.NothingWhite
import kotlin.math.roundToInt

/**
 * Graphique tactile plus lisible que la sparkline historique.
 * Le geste est limité à l'horizontale pour ne pas confisquer le défilement vertical.
 */
@Composable
fun InteractiveTrendChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    targetValue: Float? = null,
    valueFormatter: (Float) -> String = { value ->
        if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
    }
) {
    if (values.isEmpty()) return
    var selected by remember(values) { mutableIntStateOf(values.lastIndex.coerceAtLeast(0)) }

    val selectedIndex = selected.coerceIn(values.indices)
    val selectedLabel = labels.getOrNull(selectedIndex).orEmpty()
    val displayFormatter = valueFormatter
    val accessibility = remember(values, targetValue, selectedIndex) {
        if (values.size == 1) {
            "Mesure : ${displayFormatter(values.first())}"
        } else {
            "Graphique interactif. Sélection actuelle : ${displayFormatter(values[selectedIndex])}${
                if (selectedLabel.isNotBlank()) ", $selectedLabel" else ""
            }. Glissez pour consulter les valeurs."
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibility }
            .pointerInput(values.size) {
                detectTapGestures { offset ->
                    selected = indexForOffset(offset.x, size.width.toFloat(), values.size)
                }
            }
            .pointerInput(values.size) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val step = if (values.size > 1) size.width.toFloat() / (values.size - 1) else size.width.toFloat()
                    selected = (selected - (dragAmount / step).roundToInt()).coerceIn(0, values.lastIndex)
                }
            }
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            if (values.size < 2) {
                drawCircle(DataBlue, radius = size.minDimension * 0.025f, center = Offset(size.width / 2f, size.height / 2f))
                return@Canvas
            }

            val allValues = if (targetValue != null) values + targetValue else values
            val rawMin = allValues.min()
            val rawMax = allValues.max()
            val margin = ((rawMax - rawMin) * 0.16f).coerceAtLeast(0.5f)
            val min = rawMin - margin
            val max = rawMax + margin
            val range = (max - min).coerceAtLeast(0.001f)
            val top = 18f
            val bottom = size.height - 30f
            val chartHeight = bottom - top
            val stepX = size.width / (values.size - 1)

            fun point(index: Int): Offset = Offset(
                x = index * stepX,
                y = top + (1f - (values[index] - min) / range) * chartHeight
            )

            // Grille et repères de valeur : le graphique reste compréhensible sans couleur seule.
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = top + chartHeight * fraction
                drawLine(NothingBorder, Offset(0f, y), Offset(size.width, y), 1f)
            }

            targetValue?.let { target ->
                val y = top + (1f - (target - min) / range) * chartHeight
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = DataMint.copy(alpha = 0.8f),
                        start = Offset(x, y),
                        end = Offset(x + 8f, y),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                    x += 14f
                }
            }

            val line = Path()
            values.indices.forEach { index ->
                val p = point(index)
                if (index == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(size.width, bottom)
                lineTo(0f, bottom)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(listOf(DataBlue.copy(alpha = 0.22f), DataBlue.copy(alpha = 0f)))
            )
            drawPath(path = line, color = DataBlue, style = Stroke(width = 4f, cap = StrokeCap.Round))
            values.indices.forEach { index ->
                val p = point(index)
                drawCircle(NothingBlack, radius = 4.5f, center = p)
                drawCircle(NothingWhite, radius = 2.4f, center = p)
            }

            val selectedPoint = point(selectedIndex)
            drawLine(
                color = NothingGrey2.copy(alpha = 0.7f),
                start = Offset(selectedPoint.x, top),
                end = Offset(selectedPoint.x, bottom),
                strokeWidth = 1.5f
            )
            drawCircle(NothingWhite, radius = 9f, center = selectedPoint)
            drawCircle(DataBlue, radius = 5f, center = selectedPoint)
        }

        androidx.compose.material3.Text(
            text = buildString {
                append(displayFormatter(values[selectedIndex]))
                if (selectedLabel.isNotBlank()) append(" · $selectedLabel")
            },
            color = NothingWhite,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopStart)
                .padding(2.dp)
        )
    }
}

private fun indexForOffset(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val ratio = (x / width).coerceIn(0f, 1f)
    return (ratio * (count - 1)).roundToInt()
}
