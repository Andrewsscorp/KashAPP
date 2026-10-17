package com.example.presentation.trm.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RateHistoryPoint
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun HistoricalRatesChart(
    points: List<RateHistoryPoint>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF007BFF),
    chartHeight: Int = 180
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = lineColor,
                strokeWidth = 2.5.dp
            )
        }
        return
    }

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin datos históricos disponibles",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
        return
    }

    val minRate = remember(points) { points.minOf { it.rate } }
    val maxRate = remember(points) { points.maxOf { it.rate } }
    val range = remember(minRate, maxRate) {
        val diff = maxRate - minRate
        if (diff == 0.0) maxRate * 0.05 else diff
    }

    val latestPoint = points.last()
    val formatter = remember {
        val symbols = DecimalFormatSymbols(Locale.GERMAN).apply { groupingSeparator = '.' }
        DecimalFormat("#,##0.00", symbols)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // High point badge preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = lineColor,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = formatter.format(latestPoint.rate),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 24f
                val paddingTop = 12f
                val availableHeight = height - paddingBottom - paddingTop

                val stepX = if (points.size > 1) width / (points.size - 1) else width

                // 1. Draw horizontal subtle grid lines (3 levels)
                for (i in 0..3) {
                    val y = paddingTop + (availableHeight / 3f) * i
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // 2. Compute Points coordinates
                val coords = points.mapIndexed { index, item ->
                    val x = index * stepX
                    val normalized = ((item.rate - minRate) / range).toFloat().coerceIn(0f, 1f)
                    val y = paddingTop + availableHeight * (1f - normalized)
                    Offset(x, y)
                }

                if (coords.isNotEmpty()) {
                    // 3. Create Smooth Curve Path
                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            val prev = coords[i - 1]
                            val curr = coords[i]
                            val cpx = (prev.x + curr.x) / 2f
                            cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
                        }
                    }

                    // 4. Fill Area Under Curve
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(coords.last().x, height - paddingBottom)
                        lineTo(coords.first().x, height - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.28f),
                                lineColor.copy(alpha = 0.02f)
                            ),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                    )

                    // 5. Draw Stroke Curve
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 6. Draw Latest Point Marker Dot
                    val lastCoord = coords.last()
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = lastCoord
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.5.dp.toPx(),
                        center = lastCoord
                    )
                }
            }
        }

        // Date Axis Labels
        val displayIndices = when {
            points.size <= 7 -> points.indices.toList()
            else -> listOf(
                0,
                points.size / 4,
                points.size / 2,
                (points.size * 3) / 4,
                points.size - 1
            ).distinct()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displayIndices.forEach { idx ->
                val p = points.getOrNull(idx)
                if (p != null) {
                    Text(
                        text = p.displayDate,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
