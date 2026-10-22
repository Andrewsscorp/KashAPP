package com.example.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Host de confirmaciones flotantes estilo iPhone / Dynamic Island (iOS Toast Pill).
 * Reemplaza la barra rectangular gris de Material 3 por una cápsula flotante moderna,
 * con ícono contextual, sombra suave y diseño responsivo.
 */
@Composable
fun IosStyleSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) { snackbarData: SnackbarData ->
        IosStyleToastPill(
            message = snackbarData.visuals.message,
            actionLabel = snackbarData.visuals.actionLabel,
            onActionClick = { snackbarData.performAction() },
            onDismiss = { snackbarData.dismiss() }
        )
    }
}

@Composable
fun IosStyleToastPill(
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lower = message.lowercase()
    val isSecurityOrBlocked = lower.contains("bloquead") ||
        lower.contains("desactivad") ||
        lower.contains("excluid") ||
        lower.contains("protección") ||
        lower.contains("proteccion")
    val isErrorOrDanger = lower.contains("error") ||
        lower.contains("inválid") ||
        lower.contains("invalid") ||
        lower.contains("falló") ||
        lower.contains("eliminad")

    val badgeBgColor = when {
        isErrorOrDanger -> Color(0xFFFEF2F2)
        isSecurityOrBlocked -> Color(0xFFEFF6FF)
        else -> Color(0xFFECFDF5)
    }
    val badgeIconColor = when {
        isErrorOrDanger -> Color(0xFFEF4444)
        isSecurityOrBlocked -> Color(0xFF0284C7)
        else -> Color(0xFF10B981)
    }
    val badgeIcon: ImageVector = when {
        isErrorOrDanger -> Icons.Default.WarningAmber
        isSecurityOrBlocked -> Icons.Default.Shield
        else -> Icons.Default.CheckCircle
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 440.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = when {
                            isErrorOrDanger -> Color(0xFFF87171)
                            isSecurityOrBlocked -> Color(0xFF38BDF8)
                            else -> Color(0xFF34D399)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Confirmación",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0284C7),
                        modifier = Modifier.clickable { onActionClick() }
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pop-up modal de confirmación estilo Cupertino / iPhone (UIAlertController).
 * Esquinas redondeadas de 24.dp, encabezado limpio centrado, ícono opcional en cápsula circular
 * y barra inferior dividida estilo iOS con acciones Cancelar / Confirmar.
 */
@Composable
fun IosStyleConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Default.Info,
    iconTint: Color = Color(0xFF0284C7),
    iconBackground: Color = Color(0xFFE0F2FE),
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null
) {
    val resolvedConfirmColor = if (isDestructive) Color(0xFFEF4444) else Color(0xFF007AFF)

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(iconBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        if (extraContent != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            extraContent()
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isLoading) { onDismiss() }
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dismissText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }

                        VerticalDivider(
                            color = Color(0xFFE2E8F0),
                            thickness = 0.8.dp,
                            modifier = Modifier.fillMaxHeight()
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isLoading) { onConfirm() }
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = resolvedConfirmColor,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = confirmText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = resolvedConfirmColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
