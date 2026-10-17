package com.example.presentation.dashboard.components.dialogs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.ScheduledPayment
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import com.example.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledPaymentDetailsBottomSheet(
    payment: ScheduledPayment,
    onDismiss: () -> Unit,
    onOpenUpcomingPayments: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = LocalDate.now()
    val daysUntilDue = ChronoUnit.DAYS.between(today, payment.dueDate)

    val (badgeText, badgeBg, badgeColor) = when {
        payment.isPaid -> Triple("PAGADO", Color(0xFFD1FAE5), Color(0xFF059669))
        daysUntilDue < 0 -> Triple("VENCIDO HACE ${kotlin.math.abs(daysUntilDue)} DÍAS", Color(0xFFFEE2E2), Color(0xFFDC2626))
        daysUntilDue == 0L -> Triple("VENCE HOY", Color(0xFFFEE2E2), Color(0xFFDC2626))
        daysUntilDue in 1..3 -> Triple("VENCE EN $daysUntilDue DÍAS", Color(0xFFFEF3C7), Color(0xFFD97706))
        else -> Triple("PROGRAMADO", Color(0xFFE0F2FE), Color(0xFF0284C7))
    }

    val (icon, iconBg, iconTint) = when {
        payment.isIncome -> Triple(Icons.Default.Payments, Color(0xFFFEF3C7), Color(0xFFD97706))
        payment.entityName.contains("netflix", ignoreCase = true) || payment.category.contains("SUSCRIPCION", ignoreCase = true) ->
            Triple(Icons.Default.PlayArrow, Color(0xFFFEE2E2), Color(0xFFEF4444))
        payment.entityName.contains("tarjeta", ignoreCase = true) || payment.category.contains("TARJETA", ignoreCase = true) ->
            Triple(Icons.Default.CreditCard, Color(0xFFF3E8FF), Color(0xFF8B5CF6))
        payment.entityName.contains("arriendo", ignoreCase = true) || payment.category.contains("ARRIENDO", ignoreCase = true) ->
            Triple(Icons.Default.Home, Color(0xFFE0F2FE), Color(0xFF007BFF))
        else -> Triple(Icons.Default.CalendarMonth, Color(0xFFE0F2FE), Color(0xFF007BFF))
    }

    val formattedDueDate = try {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "CO"))
        payment.dueDate.format(formatter).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        payment.dueDate.toString()
    }

    val formattedCreatedDate = try {
        val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, hh:mm a", Locale("es", "CO"))
        sdf.format(Date(payment.createdAt))
    } catch (_: Exception) {
        "Registrado previamente"
    }

    val readableRecurrence = when (payment.recurrence.uppercase()) {
        "ONCE" -> "Pago único (Sin recurrencia)"
        "BIWEEKLY" -> "Quincenal (Cada 15 días)"
        "ANNUAL" -> "Anual (Una vez al año)"
        else -> "Mensual (Cada mes)"
    }

    val readableCategory = when (payment.category.uppercase()) {
        "TARJETA_CREDITO" -> "Tarjeta de Crédito / Deuda"
        "SERVICIOS" -> "Servicios Públicos / Privados"
        "SUSCRIPCION" -> "Suscripciones / Streaming"
        "DEUDA_PRESTAMO" -> "Préstamo / Deuda Financiera"
        "ARRIENDO" -> "Vivienda / Arriendo"
        else -> payment.category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("scheduled_payment_details_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle spacer
            Spacer(modifier = Modifier.height(4.dp))

            // Main Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entity Name / Title
            Text(
                text = payment.entityName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Urgency Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeBg,
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Display Card
            Surface(
                color = if (payment.isIncome) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (payment.isIncome) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (payment.isIncome) "MONTO A RECIBIR" else "MONTO A PAGAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (payment.isIncome) "+ " else ""}${MoneyFormatter.format(payment.amount, false)} ${payment.currency}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (payment.isIncome) Color(0xFF10B981) else Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Information Rows Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DETALLES DE LA PROGRAMACIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashBlue,
                        letterSpacing = 0.5.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    InfoRow(label = "Fecha límite de pago", value = formattedDueDate)
                    InfoRow(label = "Categoría", value = readableCategory)
                    InfoRow(label = "Frecuencia / Recurrencia", value = readableRecurrence)
                    InfoRow(
                        label = "Cuenta de débito",
                        value = payment.sourceAccountName?.ifBlank { null } ?: "Sin cuenta asignada"
                    )
                    InfoRow(
                        label = "Notificaciones automáticas",
                        value = if (payment.notificationEnabled) "Activadas (${payment.reminderDaysBefore} días antes)" else "Desactivadas"
                    )

                    if (!payment.notes.isNullOrBlank()) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Notas / Referencia",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = payment.notes,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    InfoRow(label = "Fecha de registro", value = formattedCreatedDate)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onOpenUpcomingPayments != null) {
                    OutlinedButton(
                        onClick = onOpenUpcomingPayments,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF007BFF)),
                        border = BorderStroke(1.dp, Color(0xFF007BFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("manage_scheduled_payment_button")
                    ) {
                        Icon(imageVector = Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Gestionar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007BFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("close_scheduled_payment_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Aceptar", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Entendido", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}
