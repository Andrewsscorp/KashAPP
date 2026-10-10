package com.example.presentation.dashboard.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CategoryExpenseShare
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

@Composable
fun ExpenseCategoryDonut(
    categories: List<CategoryExpenseShare>,
    totalExpense: Double = 2850000.0,
    onViewDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_donut_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Gastos por categoría",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = KashNavy
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart Canvas with 24dp thickness
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .testTag("donut_chart_box")
                ) {
                    Canvas(modifier = Modifier.size(126.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        var currentAngle = -90f

                        categories.forEach { cat ->
                            val sweep = (cat.percentage / 100f) * 360f
                            drawArc(
                                color = Color(cat.colorHex),
                                startAngle = currentAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            currentAngle += sweep
                        }
                    }

                    // Center of ring: Total Amount
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$ 2.850.000",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Total",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Side Legend List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                ) {
                                    Canvas(modifier = Modifier.size(8.dp)) {
                                        drawCircle(color = Color(cat.colorHex))
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }

                            Text(
                                text = "${cat.percentage}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ver detalles →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KashBlue,
                modifier = Modifier
                    .clickable { onViewDetailsClick() }
                    .testTag("expense_categories_details_link")
            )
        }
    }
}
