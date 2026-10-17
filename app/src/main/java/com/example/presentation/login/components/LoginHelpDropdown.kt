package com.example.presentation.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Help dropdown popup anchored under the top-right '?' button on Login screen.
 * Exactly matches Screenshot 2:
 * - Header: "¿CÓMO TE AYUDAMOS?" with green active status dot
 * - Items: Ayuda, Seguridad, Contacto
 * - Footer: "Atención segura todos los días"
 */
@Composable
fun LoginHelpDropdown(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onHelpClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        Popup(
            alignment = Alignment.TopEnd,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Card(
                modifier = modifier
                    .width(230.dp)
                    .padding(top = 8.dp, end = 16.dp)
                    .shadow(16.dp, shape = RoundedCornerShape(20.dp))
                    .testTag("login_help_popup"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿CÓMO TE AYUDAMOS?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        // Green online indicator dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: Ayuda
                    HelpMenuItem(
                        title = "Ayuda",
                        subtitle = "Resuelve tus dudas",
                        icon = Icons.Default.HelpOutline,
                        iconColor = Color.White,
                        iconBg = Color(0xFF007BFF),
                        onClick = {
                            onDismissRequest()
                            onHelpClick()
                        },
                        testTag = "help_menu_faq"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 2: Seguridad
                    HelpMenuItem(
                        title = "Seguridad",
                        subtitle = "Protege tu cuenta",
                        icon = Icons.Default.Shield,
                        iconColor = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        onClick = {
                            onDismissRequest()
                            onSecurityClick()
                        },
                        testTag = "help_menu_security"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 3: Contacto
                    HelpMenuItem(
                        title = "Contacto",
                        subtitle = "Habla con soporte",
                        icon = Icons.Default.ChatBubbleOutline,
                        iconColor = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        onClick = {
                            onDismissRequest()
                            onContactClick()
                        },
                        testTag = "help_menu_contact"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Footer
                    Text(
                        text = "Atención segura todos los días",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )
    }
}
