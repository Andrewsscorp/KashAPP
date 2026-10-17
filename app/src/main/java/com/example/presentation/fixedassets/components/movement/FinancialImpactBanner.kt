package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetMovementType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Dynamic financial impact banner that recomputes in real time:
 * - Current Book Value vs Prospective New Depreciable Base
 * - Revaluation Surplus or Deficit
 * - Expense vs Capitalization classification under IFRS / NIIF
 */
@Composable
fun FinancialImpactBanner(
    asset: FixedAssetEntity,
    type: AssetMovementType,
    amount: Double?,
    usefulLifeIncrementMonths: Int = 0,
    newResidualValue: Double? = null,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        val sym = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0", sym)
    }

    val currentBookValue = remember(asset) {
        kotlin.math.max(0.0, asset.acquisitionValue - asset.accumulatedDepreciation)
    }

    val validAmount = amount ?: 0.0

    when (type) {
        AssetMovementType.IMPROVEMENT -> {
            val newBase = currentBookValue + validAmount
            val newLife = asset.usefulLifeMonths + usefulLifeIncrementMonths

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF03223F),
                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "IMPACTO NIIF: INVERSIÓN CAPITALIZABLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = "El monto no va al gasto. Se suma al saldo del activo: " +
                                "Valor en libros ($ ${formatter.format(currentBookValue)}) + Mejora ($ ${formatter.format(validAmount)}) " +
                                "= Nueva Base ($ ${formatter.format(newBase)}).",
                        fontSize = 11.5.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    if (usefulLifeIncrementMonths > 0) {
                        Text(
                            text = "Vida útil extendida: ${asset.usefulLifeMonths} meses -> $newLife meses (+${usefulLifeIncrementMonths / 12} años y ${usefulLifeIncrementMonths % 12} meses). Cronograma recalculado prospectivamente.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }
        }

        AssetMovementType.REVALUATION -> {
            val diff = validAmount - currentBookValue
            val isSurplus = diff >= 0

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1035),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MODELO DE REVALUACIÓN (NIC 16)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA)
                        )
                    }
                    Text(
                        text = "El nuevo avalúo técnico ($ ${formatter.format(validAmount)}) reemplaza la base contable. " +
                                if (isSurplus) {
                                    "Superávit por Revaluación en Patrimonio: +$ ${formatter.format(diff)}."
                                } else {
                                    "Déficit reconocido contra resultados: -$ ${formatter.format(kotlin.math.abs(diff))}."
                                },
                        fontSize = 11.5.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "Se reexpresa el valor en libros y la cuota de amortización futura se recalcula sobre la nueva tasación.",
                        fontSize = 10.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        AssetMovementType.MAINTENANCE -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2E1B00),
                border = BorderStroke(1.dp, Color(0xFFD97706)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "GASTO OPERACIONAL DEL PERIODO (100%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Text(
                        text = "Mantenimiento preventivo/correctivo. Este monto se va 100% al Estado de Resultados. " +
                                "NO incrementa el valor en libros del activo ni altera el cronograma de depreciación.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        AssetMovementType.IMPAIRMENT -> {
            val remainingAfterImpairment = kotlin.math.max(0.0, currentBookValue - validAmount)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2D0607),
                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "RECONOCIMIENTO DE DETERIORO (NIC 36)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                    }
                    Text(
                        text = "Pérdida por deterioro: $ ${formatter.format(validAmount)}. " +
                                "Reduce el valor en libros de $ ${formatter.format(currentBookValue)} a $ ${formatter.format(remainingAfterImpairment)}. " +
                                "Las cuotas mensuales futuras se reducen prospectivamente.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        AssetMovementType.DISPOSAL, AssetMovementType.SALE -> {
            val isSale = type == AssetMovementType.SALE
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSale) "ENAJENACIÓN / VENTA DEL ACTIVO" else "BAJA Y DESINCORPORACIÓN DEFINITIVA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Text(
                        text = "El activo pasará a estado inactivo (${if (isSale) "Vendido" else "Dado de baja"}). " +
                                "Se detienen permanentemente los ciclos futuros de cálculo y depreciación.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        else -> {}
    }
}
