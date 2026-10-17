package com.example.presentation.dashboard.components.sidebar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

data class NavLeaf(
    val title: String,
    val route: String
)

sealed class SidebarNavItem(
    val header: String,
    val icon: ImageVector,
    val defaultRoute: String
) {
    data class Single(
        val title: String,
        val iconVector: ImageVector,
        val route: String
    ) : SidebarNavItem(title, iconVector, route)

    data class Group(
        val title: String,
        val iconVector: ImageVector,
        val children: List<NavLeaf>,
        val isExpanded: Boolean = false,
        val groupRoute: String
    ) : SidebarNavItem(title, iconVector, groupRoute)
}

object SidebarNavDefaults {
    fun getInitialNavigationTree(): List<SidebarNavItem> {
        return listOf(
            SidebarNavItem.Single(
                title = "Inicio",
                iconVector = Icons.Default.Home,
                route = "kashapp://dashboard"
            ),
            SidebarNavItem.Group(
                title = "Finanzas",
                iconVector = Icons.Default.AccountBalanceWallet,
                isExpanded = true, // expanded by default as in screenshot
                groupRoute = "kashapp://finances",
                children = listOf(
                    NavLeaf("Cuentas", "kashapp://finances/cuentas"),
                    NavLeaf("Gastos", "kashapp://finances/gastos"),
                    NavLeaf("Ingresos", "kashapp://finances/ingresos"),
                    NavLeaf("Transferencias", "kashapp://finances/transferencias")
                )
            ),
            SidebarNavItem.Group(
                title = "Planificación",
                iconVector = Icons.Default.TrackChanges,
                isExpanded = false,
                groupRoute = "kashapp://planning",
                children = listOf(
                    NavLeaf("Presupuestos", "kashapp://planning/presupuestos"),
                    NavLeaf("Objetivos", "kashapp://planning/objetivos"),
                    NavLeaf("Proyecciones", "kashapp://planning/proyecciones")
                )
            ),
            SidebarNavItem.Group(
                title = "Crédito",
                iconVector = Icons.Default.CreditCard,
                isExpanded = false,
                groupRoute = "kashapp://credit",
                children = listOf(
                    NavLeaf("Tarjetas", "kashapp://credit/tarjetas"),
                    NavLeaf("Préstamos", "kashapp://credit/prestamos"),
                    NavLeaf("Deudas", "kashapp://credit/deudas")
                )
            ),
            SidebarNavItem.Group(
                title = "Patrimonio",
                iconVector = Icons.Default.TrendingUp,
                isExpanded = false,
                groupRoute = "kashapp://wealth",
                children = listOf(
                    NavLeaf("Inversiones", "kashapp://wealth/inversiones"),
                    NavLeaf("Activos", "kashapp://wealth/activos"),
                    NavLeaf("Patrimonio", "kashapp://wealth/patrimonio")
                )
            ),
            SidebarNavItem.Group(
                title = "Herramientas",
                iconVector = Icons.Default.Layers,
                isExpanded = false,
                groupRoute = "kashapp://tools",
                children = listOf(
                    NavLeaf("Calculadoras", "kashapp://tools/calculadoras"),
                    NavLeaf("Documentos", "kashapp://tools/documentos"),
                    NavLeaf("Suscripciones", "kashapp://tools/suscripciones"),
                    NavLeaf("KashAI", "kashapp://tools/kashai")
                )
            ),
            SidebarNavItem.Group(
                title = "Más",
                iconVector = Icons.Default.Settings,
                isExpanded = false,
                groupRoute = "kashapp://settings",
                children = listOf(
                    NavLeaf("Configuración", "kashapp://settings/configuracion"),
                    NavLeaf("Seguridad", "kashapp://settings/seguridad")
                )
            )
        )
    }
}
