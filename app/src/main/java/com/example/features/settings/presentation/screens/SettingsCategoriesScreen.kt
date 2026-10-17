package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

@Composable
fun SettingsCategoriesSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("EXPENSE") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    val expenseCategories = remember {
        mutableStateOf(
            listOf(
                "Alimentación & Supermercado" to "#EF4444",
                "Transporte & Combustible" to "#F97316",
                "Vivienda & Servicios" to "#0284C7",
                "Salud & Bienestar" to "#10B981",
                "Educación & Cursos" to "#6366F1",
                "Entretenimiento & Ocio" to "#EC4899",
                "Compras & Ropa" to "#8B5CF6",
                "Obligaciones Financieras" to "#E11D48"
            )
        )
    }

    val incomeCategories = remember {
        mutableStateOf(
            listOf(
                "Nómina & Sueldos" to "#10B981",
                "Honorarios & Servicios" to "#0284C7",
                "Rendimientos e Inversiones" to "#6366F1",
                "Ventas y Negocio" to "#F59E0B",
                "Reembolsos y Otros" to "#14B8A6"
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedTab = "EXPENSE" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "EXPENSE") Color(0xFFEF4444) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Gastos (${expenseCategories.value.size})",
                    color = if (selectedTab == "EXPENSE") Color.White else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { selectedTab = "INCOME" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "INCOME") Color(0xFF10B981) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Ingresos (${incomeCategories.value.size})",
                    color = if (selectedTab == "INCOME") Color.White else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val currentList = if (selectedTab == "EXPENSE") expenseCategories.value else incomeCategories.value

                currentList.forEach { (catName, colorHex) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == "EXPENSE") Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(android.graphics.Color.parseColor(colorHex)),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = catName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                            Text("Activa ✓", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { showAddCategoryDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir nueva categoría", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Nueva Categoría de ${if (selectedTab == "EXPENSE") "Gasto" else "Ingreso"}", fontWeight = FontWeight.Bold, color = KashNavy) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Nombre de la categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        cursorColor = KashBlue
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            if (selectedTab == "EXPENSE") {
                                expenseCategories.value = expenseCategories.value + (newCatName to "#0284C7")
                            } else {
                                incomeCategories.value = incomeCategories.value + (newCatName to "#10B981")
                            }
                            onShowMessage("Categoría \"$newCatName\" creada")
                            newCatName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }
}
