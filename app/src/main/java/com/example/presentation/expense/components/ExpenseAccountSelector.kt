package com.example.presentation.expense.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.presentation.expense.model.formatMoneyDisplay

/**
 * Card 2 & 2.5: Bank Account Selector and Real-Time Account Impact Card.
 * Also provides the Bank Account Selection ModalBottomSheet.
 */
@Composable
fun ExpenseAccountSelector(
    selectedAccount: AccountEntity?,
    allAccounts: List<AccountEntity>,
    amountDouble: Double,
    selectedCurrencyCode: String,
    onSelectAccount: (AccountEntity) -> Unit,
    onCreateAccountClick: (typeCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAccountSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ------------------------------------------
        // CARD 2: PAGAR DESDE (CUENTAS DE BANCO REALES)
        // ------------------------------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAccountSheet = true }
                .testTag("pagar_desde_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pagas desde",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "* Obligatorio",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    if (selectedAccount != null) {
                        val acc = selectedAccount
                        val mask = acc.accountNumberMasked ?: "•••• ${acc.id.takeLast(4)}"
                        Text(
                            text = "${acc.name} ($mask)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Disponible: ${formatMoneyDisplay(acc.balance, acc.currency)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    } else {
                        Text(
                            text = "+ Seleccionar o crear cuenta",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                        Text(
                            text = "Toca para elegir desde qué cuenta pagar",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ------------------------------------------
        // CARD 2.5: CÁLCULOS EN TIEMPO REAL (IMPACTO EN CUENTA & % DEL SALDO)
        // ------------------------------------------
        val activeAccount = selectedAccount ?: allAccounts.firstOrNull()
        if (activeAccount != null) {
            val currentBal = activeAccount.balance
            val expAmt = amountDouble
            val projBal = currentBal - expAmt
            val pctOfTotal = if (currentBal > 0.0) {
                (expAmt / currentBal * 100.0).coerceAtLeast(0.0)
            } else if (expAmt > 0.0) {
                100.0
            } else 0.0

            val progRatio = if (currentBal > 0.0) {
                (expAmt / currentBal).toFloat().coerceIn(0.0f, 1.0f)
            } else if (expAmt > 0.0) 1.0f else 0.0f

            val allowsNeg = activeAccount.hasOverdraft
            val isExceeded = projBal < 0.0

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    1.dp,
                    if (isExceeded && !allowsNeg) Color(0xFFFCA5A5)
                    else if (isExceeded) Color(0xFFFCD34D)
                    else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_realtime_impact_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isExceeded && !allowsNeg) Color(0xFFEF4444) else Color(0xFF0284C7))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Impacto en cuenta en tiempo real",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isExceeded && !allowsNeg -> Color(0xFFFEE2E2)
                                isExceeded -> Color(0xFFFEF3C7)
                                pctOfTotal > 50.0 -> Color(0xFFFFF7ED)
                                else -> Color(0xFFF0FDF4)
                            }
                        ) {
                            Text(
                                text = if (expAmt > 0) "${"%.1f".format(pctOfTotal)}% del saldo" else "0% del saldo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isExceeded && !allowsNeg -> Color(0xFFDC2626)
                                    isExceeded -> Color(0xFFD97706)
                                    pctOfTotal > 50.0 -> Color(0xFFEA580C)
                                    else -> Color(0xFF16A34A)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                isExceeded && !allowsNeg -> Color(0xFFEF4444)
                                isExceeded -> Color(0xFFF59E0B)
                                pctOfTotal > 70.0 -> Color(0xFFF97316)
                                else -> Color(0xFF0284C7)
                            },
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }

                    // Balance Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Saldo Actual", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = formatMoneyDisplay(currentBal, activeAccount.currency),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gasto a Debitar", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = "- ${formatMoneyDisplay(expAmt, selectedCurrencyCode)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo Resultante", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = formatMoneyDisplay(projBal, activeAccount.currency),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                        }
                    }

                    // Warning / Overdraft Badge
                    if (isExceeded) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!allowsNeg) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (!allowsNeg) Color(0xFFFECACA) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (!allowsNeg)
                                        "⚠️ Fondos insuficientes: Esta cuenta no permite sobregiro. No podrás guardar el gasto a menos que reduzcas el monto."
                                    else
                                        "ℹ️ Sobregiro habilitado: El saldo quedará en negativo (${formatMoneyDisplay(projBal, activeAccount.currency)}).",
                                    fontSize = 11.sp,
                                    color = if (!allowsNeg) Color(0xFF991B1B) else Color(0xFF92400E),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Cuentas de banco
    if (showAccountSheet) {
        ExpenseAccountBottomSheet(
            visible = showAccountSheet,
            roomAccounts = allAccounts,
            selectedCurrencyCode = selectedCurrencyCode,
            selectedAccountId = selectedAccount?.id,
            onSelectAccount = { acc ->
                onSelectAccount(acc)
                showAccountSheet = false
            },
            onCreateAccountClick = { type ->
                showAccountSheet = false
                onCreateAccountClick(type)
            },
            onDismiss = { showAccountSheet = false }
        )
    }
}

/**
 * Modal Bottom Sheet to choose bank accounts or quickly trigger new account creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAccountBottomSheet(
    visible: Boolean,
    roomAccounts: List<AccountEntity>,
    selectedCurrencyCode: String,
    selectedAccountId: String?,
    onSelectAccount: (AccountEntity) -> Unit,
    onCreateAccountClick: (typeCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val totalBankBalance = remember(roomAccounts) {
        roomAccounts.sumOf { it.balance }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            // Header with Bank icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cuentas bancarias",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${roomAccounts.size} cuenta(s) vinculadas en BD",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "Total: ${formatMoneyDisplay(totalBankBalance, selectedCurrencyCode)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Action Tiles to create different account types
            Text(
                text = "AGREGAR NUEVA CUENTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // + Ahorros
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCreateAccountClick("SAVINGS") }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+ Ahorros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }

                // + Corriente
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCreateAccountClick("CHECKING") }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+ Corriente", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                }

                // + Efectivo
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCreateAccountClick("CASH") }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+ Efectivo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                }

                // + Billetera
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCreateAccountClick("WALLET") }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+ Billetera", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TUS CUENTAS DISPONIBLES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roomAccounts.forEach { acc ->
                    val isSelected = selectedAccountId == acc.id
                    val mask = acc.accountNumberMasked ?: "•••• ${acc.id.takeLast(4)}"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectAccount(acc) },
                        color = if (isSelected) Color(0xFFF0F9FF) else Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${acc.name} ($mask)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Saldo: ${formatMoneyDisplay(acc.balance, acc.currency)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF059669)
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
