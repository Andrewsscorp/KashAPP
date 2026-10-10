package com.example.presentation.dashboard.components.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy

@Composable
fun DashboardBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_bottom_nav_bar")
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Inicio
                BottomNavItem(
                    label = "Inicio",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == "kashapp://dashboard",
                    onClick = { onNavigate("kashapp://dashboard") },
                    modifier = Modifier.weight(1f)
                )

                // 2. Finanzas
                BottomNavItem(
                    label = "Finanzas",
                    icon = Icons.Default.AccountBalanceWallet,
                    isSelected = currentRoute.startsWith("kashapp://finances"),
                    onClick = { onNavigate("kashapp://finances") },
                    modifier = Modifier.weight(1f)
                )

                // Center placeholder for FAB
                Spacer(modifier = Modifier.weight(1f))

                // 3. Objetivos
                BottomNavItem(
                    label = "Objetivos",
                    icon = Icons.Default.TrackChanges,
                    isSelected = currentRoute.startsWith("kashapp://planning/objetivos"),
                    onClick = { onNavigate("kashapp://planning/objetivos") },
                    modifier = Modifier.weight(1f)
                )

                // 4. Ajustes
                BottomNavItem(
                    label = "Ajustes",
                    icon = Icons.Default.Settings,
                    isSelected = currentRoute.startsWith("kashapp://settings") || currentRoute.contains("ajustes"),
                    onClick = { onNavigate("kashapp://settings") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center Elevated Circular Floating Action Button (+) with Gradient
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(54.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(KashBlue, KashCyan)
                    )
                )
                .clickable { onFabClick() }
                .testTag("center_quick_add_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar nuevo",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) KashBlue else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) KashBlue else Color(0xFF64748B)
        )
    }
}
