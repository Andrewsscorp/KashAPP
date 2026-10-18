package com.example.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import com.example.presentation.login.components.LoginFaqBottomSheet
import com.example.presentation.login.components.LoginHelpDropdown
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Pixel-Perfect Enterprise Login Screen matching the exact design specification:
 * - Deep navy gradient header with cursive tagline "Tu dinero, en movimiento."
 * - Top-right circular '?' button with anchored Help dropdown
 * - Support & FAQ BottomSheet with searchable answers
 * - Full-width "Continuar con Google" primary button
 * - Clean "Huella digital" and "PIN seguro" quick access cards
 * - Security & biometric encryption badge
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onGoogleSignInClick: () -> Unit,
    onBiometricClick: () -> Unit,
    onPinClick: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onSetupPin: (String) -> Unit,
    onDismissPinDialog: () -> Unit,
    onSwitchAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val adaptive = rememberKashAdaptiveInfo()
    val scrollState = rememberScrollState()

    var showHelpDropdown by remember { mutableStateOf(false) }
    var showFaqBottomSheet by remember { mutableStateOf(false) }

    // Entrance Animation
    var isScreenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        isScreenVisible = true
    }

    // Shake error animation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            repeat(3) {
                shakeOffset.animateTo(10f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-10f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
        ) {
            // ==========================================
            // TOP HERO SECTION (Deep Navy Gradient)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF031628),
                                Color(0xFF062847),
                                Color(0xFF0B3B64)
                            )
                        )
                    )
            ) {
                // Subtle decorative concentric wave lines
                val heroCanvasHeight = if (adaptive.isCompactHeight) 140.dp else 260.dp
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroCanvasHeight)
                ) {
                    val w = size.width
                    val h = size.height

                    drawArc(
                        color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.4f, h * 0.45f),
                        size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    drawArc(
                        color = Color(0xFF38BDF8).copy(alpha = 0.05f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.25f, h * 0.3f),
                        size = androidx.compose.ui.geometry.Size(w * 1.1f, h * 1.1f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Top App Bar: Logo + Title + Help Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo & Brand Name
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "K",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF007BFF)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "KashAPP",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        // Circular Help Button '?'
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .clickable { showHelpDropdown = !showHelpDropdown }
                                    .testTag("login_help_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = "Ayuda y Soporte",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Anchored Dropdown
                            LoginHelpDropdown(
                                isExpanded = showHelpDropdown,
                                onDismissRequest = { showHelpDropdown = false },
                                onHelpClick = { showFaqBottomSheet = true },
                                onSecurityClick = { showFaqBottomSheet = true },
                                onContactClick = { showFaqBottomSheet = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (adaptive.isCompactHeight) 14.dp else 28.dp))

                    // Tagline in Cursive Script (Screenshot 1: "Tu dinero, en movimiento.")
                    Text(
                        text = "Tu dinero, en\nmovimiento.",
                        fontSize = if (adaptive.isUltraCompact) 26.sp else 32.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        lineHeight = if (adaptive.isUltraCompact) 32.sp else 38.sp
                    )

                    Spacer(modifier = Modifier.height(if (adaptive.isCompactHeight) 6.dp else 10.dp))

                    // Subtitle
                    Text(
                        text = "Accede de forma rápida y segura a todo lo que construyes.",
                        fontSize = 14.sp,
                        color = Color(0xFFE2E8F0).copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(if (adaptive.isCompactHeight) 12.dp else 24.dp))
                }
            }

            // ==========================================
            // MAIN CARD SECTION (White Rounded Base)
            // ==========================================
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = adaptive.maxContentWidth)
                        .offset(y = (-16).dp)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    if (uiState.rememberedUser != null) {
                        // Quick Unlock Personalized View
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF007BFF).copy(alpha = 0.12f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (uiState.rememberedUser.displayName?.firstOrNull()
                                            ?: uiState.rememberedUser.email?.firstOrNull() ?: 'U').uppercase(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF007BFF)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hola, ${uiState.rememberedUser.displayName?.ifBlank { null } ?: "Usuario"}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                val email = uiState.rememberedUser.email
                                if (!email.isNullOrBlank()) {
                                    Text(
                                        text = email,
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Desbloquea tu cuenta de inmediato para ver tus finanzas.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Error message banner
                        if (!uiState.errorMessage.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // 1. Primary Quick Unlock Button: Huella
                        Button(
                            onClick = onBiometricClick,
                            enabled = !uiState.isLoading && !uiState.isLockedOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007BFF),
                                disabledContainerColor = Color(0xFF93C5FD)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF007BFF).copy(alpha = 0.4f))
                                .testTag("biometric_unlock_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Desbloquear con Huella",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Desbloquear con Huella",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Secondary Quick Unlock Button: PIN
                        OutlinedButton(
                            onClick = onPinClick,
                            enabled = !uiState.isLoading && !uiState.isLockedOut,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pin_unlock_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = "Ingresar PIN",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ingresar con PIN",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Switch Account
                        TextButton(
                            onClick = onSwitchAccount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_account_button")
                        ) {
                            Text(
                                text = "¿No eres tú? Iniciar con otra cuenta",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                    } else {
                        // Header
                        Text(
                            text = "Bienvenido de nuevo",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Elige la forma más cómoda de entrar.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Error message banner
                        if (!uiState.errorMessage.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // 1. Continuar con Google Button (Screenshot 1)
                        Button(
                            onClick = onGoogleSignInClick,
                            enabled = !uiState.isLoading && !uiState.isLockedOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007BFF),
                                disabledContainerColor = Color(0xFF93C5FD)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF007BFF).copy(alpha = 0.4f))
                                .testTag("google_sign_in_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Google 'G' White Circle Badge
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "G",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF007BFF)
                                        )
                                    }
                                }

                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Continuar con Google",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Black Arrow Right Circle Badge (Screenshot 1)
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0A192F),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Separator: ── ACCESO RÁPIDO ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "ACCESO RÁPIDO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2. Huella Digital Quick Access Card (Screenshot 1)
                        QuickAuthOptionCard(
                            title = "Huella digital",
                            subtitle = "Un toque y listo",
                            icon = Icons.Default.Fingerprint,
                            onClick = onBiometricClick,
                            enabled = !uiState.isLoading && !uiState.isLockedOut,
                            testTag = "biometric_quick_auth_card"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. PIN Seguro Quick Access Card (Screenshot 1)
                        QuickAuthOptionCard(
                            title = "PIN seguro",
                            subtitle = "Tu clave de 6 dígitos",
                            icon = Icons.Default.Apps,
                            onClick = onPinClick,
                            enabled = !uiState.isLoading && !uiState.isLockedOut,
                            testTag = "pin_quick_auth_card"
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Security Footer Note: 🛡 Protección biométrica · Cifrado de extremo a extremo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Protección biométrica · Cifrado de extremo a extremo",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Footer Link: ¿Necesitas ayuda? Habla con nosotros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Necesitas ayuda? ",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Habla con nosotros",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007BFF),
                            modifier = Modifier
                                .clickable { showFaqBottomSheet = true }
                                .testTag("talk_to_us_footer_link")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            }
        }
    }

    // Support & FAQ BottomSheet Modal
    if (showFaqBottomSheet) {
        LoginFaqBottomSheet(
            onDismissRequest = { showFaqBottomSheet = false },
            onContactSupportClick = {
                showFaqBottomSheet = false
            }
        )
    }

    // Enter PIN Modal
    if (uiState.showPinDialog) {
        PinInputDialog(
            isSetupMode = false,
            isLockedOut = uiState.isLockedOut,
            lockoutSecondsRemaining = uiState.lockoutSecondsRemaining,
            remainingAttempts = uiState.remainingAttempts,
            errorMessage = uiState.errorMessage,
            onPinSubmit = onPinSubmit,
            onSetupPin = onSetupPin,
            onDismiss = onDismissPinDialog
        )
    }

    // Setup PIN Modal
    if (uiState.showPinSetupDialog) {
        PinInputDialog(
            isSetupMode = true,
            isLockedOut = uiState.isLockedOut,
            lockoutSecondsRemaining = uiState.lockoutSecondsRemaining,
            remainingAttempts = uiState.remainingAttempts,
            errorMessage = uiState.errorMessage,
            onPinSubmit = onPinSubmit,
            onSetupPin = onSetupPin,
            onDismiss = onDismissPinDialog
        )
    }
}

@Composable
private fun QuickAuthOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Light Blue Rounded Square Badge with Blue Icon (Screenshot 1)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF007BFF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Light Blue Circle Badge with Chevron Right (Screenshot 1)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE).copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF007BFF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
