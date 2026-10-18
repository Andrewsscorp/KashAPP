package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Screen for managing Commercial Merchants and Retail Stores.
 * Connected to the central Third-Party Architecture.
 */
@Composable
fun SettingsMerchantsSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToTerceros: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val merchants = remember {
        mutableStateOf(
            listOf(
                Triple("Supermercados Éxito S.A.", "NIT 890900608-9", "Mercado & Hogar (Categorización automática)"),
                Triple("Carulla FreshMarket", "NIT 890900608-1", "Alimentación Premium & Supermercado"),
                Triple("Organización Terpel S.A.", "NIT 860005224-6", "Combustibles & Movilidad Terrestre"),
                Triple("Tiendas D1 & Ara", "NIT 900603423-1", "Compras de Conveniencia Diaria"),
                Triple("Uber & Didi Colombia", "NIT 901234567-8", "Transporte Urbano & Aplicaciones"),
                Triple("EPM Servicios Públicos", "NIT 890904996-1", "Servicios Públicos Domiciliarios")
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back row
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = KashNavy)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("Tiendas y Comercios", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KashNavy)
                Text("Establecimientos comerciales y categorización inteligente", fontSize = 11.5.sp, color = Color(0xFF64748B))
            }
        }

        // Explanatory Enterprise Card: ¿Para qué sirve?
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("¿Para qué sirve este submódulo?", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0369A1))
                    Text(
                        text = "Una tienda o comercio representa los establecimientos donde efectúas tus compras recurrentes. Al asociar un comercio a tus gastos o tickets OCR, KashApp clasifica automáticamente la categoría de presupuesto (Alimentación, Combustible, etc.). Todas las tiendas forman parte del Módulo Central de Terceros.",
                        fontSize = 12.sp,
                        color = Color(0xFF0C4A6E),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Merchants List
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Comercios Principales Registrados", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KashNavy)

                merchants.value.forEach { (name, nit, cat) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = KashNavy)
                            Text("$nit • $cat", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Icon(Icons.Default.Star, contentDescription = "Frecuente", tint = Color(0xFFEAB308), modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Navigate to Full Third Parties Module
                Button(
                    onClick = onNavigateToTerceros,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Módulo de Terceros y Contactos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
