package com.example.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy
import kotlin.math.roundToInt

@Composable
fun PinInputDialog(
    isSetupMode: Boolean,
    isLockedOut: Boolean,
    lockoutSecondsRemaining: Long,
    remainingAttempts: Int,
    errorMessage: String?,
    onPinSubmit: (String) -> Unit,
    onSetupPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val activePin = if (isConfirmStep) confirmPin else enteredPin
    val effectiveError = localError ?: errorMessage

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(effectiveError) {
        if (!effectiveError.isNullOrBlank()) {
            repeat(3) {
                shakeOffset.animateTo(10f, animationSpec = tween(45, easing = LinearEasing))
                shakeOffset.animateTo(-10f, animationSpec = tween(45, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(45, easing = LinearEasing))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                .testTag("pin_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon with Glowing Circle
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.5.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PIN",
                            tint = KashBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isSetupMode && !isConfirmStep -> "Crear PIN de Seguridad"
                        isSetupMode && isConfirmStep -> "Confirmar tu PIN"
                        else -> "Ingresa tu PIN"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isLockedOut -> "Dispositivo bloqueado por seguridad (${lockoutSecondsRemaining}s)"
                        isSetupMode && !isConfirmStep -> "Define un código numérico de 4 dígitos para proteger tu cuenta"
                        isSetupMode && isConfirmStep -> "Vuelve a escribir el PIN de 4 dígitos para verificarlo"
                        else -> "Introduce tu código de 4 dígitos para acceder"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isLockedOut) Color(0xFFDC2626) else Color.Gray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4 Animated PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < activePin.length
                        val dotSize by animateDpAsState(
                            targetValue = if (isFilled) 18.dp else 14.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dot_size"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                effectiveError != null -> Color(0xFFEF4444)
                                isFilled -> KashBlue
                                else -> Color(0xFFE2E8F0)
                            },
                            animationSpec = tween(200),
                            label = "dot_color"
                        )

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(dotColor)
                                .then(
                                    if (!isFilled) Modifier.border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                                    else Modifier
                                )
                                .testTag("pin_dot_$i")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error / Remaining attempts banner
                AnimatedVisibility(
                    visible = effectiveError != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = effectiveError ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("pin_error_text")
                    )
                }

                if (!isSetupMode && !isLockedOut && remainingAttempts < 3 && remainingAttempts > 0) {
                    Text(
                        text = "Intentos restantes: $remainingAttempts de 3",
                        color = Color(0xFFD97706),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3x4 Number Keypad with tactile spring animation
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                when (key) {
                                    "C" -> {
                                        KeypadButton(
                                            label = "C",
                                            enabled = !isLockedOut,
                                            testTag = "key_clear",
                                            isSpecial = true,
                                            onClick = {
                                                localError = null
                                                if (isConfirmStep) confirmPin = "" else enteredPin = ""
                                            }
                                        )
                                    }
                                    "DEL" -> {
                                        KeypadButton(
                                            icon = Icons.AutoMirrored.Filled.Backspace,
                                            enabled = !isLockedOut,
                                            testTag = "key_backspace",
                                            isSpecial = true,
                                            onClick = {
                                                localError = null
                                                if (isConfirmStep) {
                                                    if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                                } else {
                                                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                        )
                                    }
                                    else -> {
                                        KeypadButton(
                                            label = key,
                                            enabled = !isLockedOut,
                                            testTag = "key_$key",
                                            isSpecial = false,
                                            onClick = {
                                                localError = null
                                                if (isSetupMode) {
                                                    if (!isConfirmStep) {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += key
                                                            if (enteredPin.length == 4) {
                                                                isConfirmStep = true
                                                            }
                                                        }
                                                    } else {
                                                        if (confirmPin.length < 4) {
                                                            confirmPin += key
                                                            if (confirmPin.length == 4) {
                                                                if (enteredPin == confirmPin) {
                                                                    onSetupPin(enteredPin)
                                                                } else {
                                                                    localError = "Los códigos PIN no coinciden"
                                                                    confirmPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                        if (enteredPin.length == 4) {
                                                            onPinSubmit(enteredPin)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_pin_button")
                ) {
                    Text(
                        text = "Cancelar",
                        color = KashNavy,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean,
    testTag: String,
    isSpecial: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "key_scale"
    )

    Box(
        modifier = Modifier
            .size(62.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isSpecial) Color.Transparent
                else if (isPressed) Color(0xFFE2E8F0)
                else Color(0xFFF1F5F9)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isSpecial) FontWeight.SemiBold else FontWeight.Bold,
                    color = if (isSpecial) Color(0xFF64748B) else KashNavy,
                    fontSize = if (isSpecial) 18.sp else 22.sp
                )
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = "Borrar",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
