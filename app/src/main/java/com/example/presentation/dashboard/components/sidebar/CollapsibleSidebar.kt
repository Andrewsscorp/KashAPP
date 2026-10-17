package com.example.presentation.dashboard.components.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy
import com.example.ui.theme.KashTextSecondary

@Composable
fun CollapsibleSidebar(
    items: List<SidebarNavItem>,
    currentRoute: String,
    onToggleGroup: (String) -> Unit,
    onSelectRoute: (String) -> Unit,
    onOpenKashAI: () -> Unit,
    onOpenSecurity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .testTag("collapsible_sidebar"),
        color = Color(0xFFFBFDFF),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kashapp_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = KashNavy, fontWeight = FontWeight.Bold)) {
                                append("Kash")
                            }
                            withStyle(SpanStyle(color = KashCyan, fontWeight = FontWeight.Bold)) {
                                append("App")
                            }
                        },
                        fontSize = 22.sp
                    )
                }

                // Nav Items / Accordion
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEach { navItem ->
                        when (navItem) {
                            is SidebarNavItem.Single -> {
                                val isSelected = currentRoute == navItem.route
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFFE0F2FE) else Color.Transparent)
                                        .clickable { onSelectRoute(navItem.route) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("nav_single_${navItem.title}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = navItem.iconVector,
                                        contentDescription = navItem.title,
                                        tint = if (isSelected) KashBlue else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = navItem.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) KashNavy else Color(0xFF334155)
                                    )
                                }
                            }

                            is SidebarNavItem.Group -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(animationSpec = tween(200))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onToggleGroup(navItem.title) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .testTag("nav_group_${navItem.title}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = navItem.iconVector,
                                                contentDescription = navItem.title,
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = navItem.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }

                                        Icon(
                                            imageVector = if (navItem.isExpanded)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (navItem.isExpanded) "Colapsar" else "Expandir",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Children Sub-leaves
                                    if (navItem.isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 36.dp, top = 2.dp, bottom = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            navItem.children.forEach { child ->
                                                val isChildSelected = currentRoute == child.route
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isChildSelected) Color(0xFFE0F2FE) else Color.Transparent)
                                                        .clickable { onSelectRoute(child.route) }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                                        .testTag("nav_leaf_${child.title}"),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = child.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isChildSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isChildSelected) KashBlue else Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SQLCipher Security Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0FDF4))
                    .clickable { onOpenSecurity() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("sidebar_sqlcipher_security_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "SQLCipher Activo",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SQLCipher AES-256",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14532D)
                    )
                    Text(
                        text = "Cifrado en reposo activo",
                        fontSize = 9.sp,
                        color = Color(0xFF166534)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom KashAI Prompt Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sidebar_kashai_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(KashCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = KashBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KashAI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "¿En qué puedo ayudarte hoy?",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onOpenKashAI,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(KashNavy)
                                .testTag("open_kashai_sidebar_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Abrir KashAI",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
