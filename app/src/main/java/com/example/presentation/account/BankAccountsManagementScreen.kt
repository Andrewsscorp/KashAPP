package com.example.presentation.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.example.core.ui.components.CurrencyText
import com.example.core.ui.components.KashSearchBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.KashAppApplication
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.EffectiveRate
import com.example.domain.currency.CurrencyConversionEngine
import com.example.presentation.account.components.AccountMachineLearningAnalyticsTab
import com.example.presentation.account.components.AccountTransactionsAuditedTab
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.account.components.LiveCurrencyConversionPreview
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.presentation.account.components.WORLD_CURRENCIES_CATALOG
import com.example.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Enterprise "Mis Cuentas Bancarias" screen accessible from the left sandwich drawer.
 * Allows viewing all bank accounts, editing/modifying them, viewing deep statistics and cashflow,
 * and performing full account management with Room DB persistence.
 */
@Composable
fun BankAccountsManagementScreen(
    onNavigateBack: () -> Unit,
    onOpenCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    userId: String = "user_default",
    customFinancialRepository: com.example.domain.repository.FinancialRepository? = null,
    customSettingsRepository: com.example.features.settings.domain.repository.SettingsRepository? = null
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val financialRepository = remember(context, customFinancialRepository) {
        customFinancialRepository ?: try {
            (context.applicationContext as? KashAppApplication)?.appContainer?.financialRepository
        } catch (_: Throwable) {
            null
        } ?: error("FinancialRepository not provided and KashAppApplication not found")
    }
    val settingsRepository = remember(context, customSettingsRepository) {
        customSettingsRepository ?: try {
            (context.applicationContext as? KashAppApplication)?.appContainer?.settingsRepository
        } catch (_: Throwable) {
            null
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val accounts by financialRepository.getAccounts(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val allTransactions by financialRepository.getTransactions(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val userPreferences by (settingsRepository?.observePreferences(userId) ?: kotlinx.coroutines.flow.flowOf(UserPreferencesDomain())).collectAsStateWithLifecycle(
        initialValue = UserPreferencesDomain()
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountForStats by remember { mutableStateOf<AccountEntity?>(null) }
    var accountForTransactions by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    // Computed totals
    val totalBalance = remember(accounts) {
        accounts.sumOf { it.balance }
    }

    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts
        else accounts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.bankName.contains(searchQuery, ignoreCase = true) ||
                    it.type.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .testTag("bank_accounts_management_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ==========================================
            // TOP BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("accounts_manager_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Mis Cuentas Bancarias",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${accounts.size} cuentas registradas",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Add Account Action Button
                Surface(
                    onClick = onOpenCreateAccount,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF007BFF),
                    modifier = Modifier.testTag("accounts_manager_add_account_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Nueva",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // ==========================================
            // HERO SUMMARY CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALDO TOTAL CONSOLIDADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Room Local DB",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CurrencyText(
                        amountDouble = totalBalance,
                        currencyCode = "COP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Breakdown chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val savingsCount = accounts.count { it.type.contains("SAVING", ignoreCase = true) || it.type.contains("AHORRO", ignoreCase = true) }
                        val checkingCount = accounts.count { it.type.contains("CHECKING", ignoreCase = true) || it.type.contains("CORRIENTE", ignoreCase = true) }
                        val otherCount = accounts.size - savingsCount - checkingCount

                        SummaryPill("Ahorros: $savingsCount", Color(0xFF10B981))
                        SummaryPill("Corrientes: $checkingCount", Color(0xFF38BDF8))
                        if (otherCount > 0) {
                            SummaryPill("Otras: $otherCount", Color(0xFFA78BFA))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // TABS ROW: Cuentas | Estadísticas | Movimientos
            // ==========================================
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF007BFF),
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Cuentas (${accounts.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == 0) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Estadísticas & ML",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == 1) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "Movimientos & Auditoría",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == 2) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
            }

            // ==========================================
            // TAB CONTENT
            // ==========================================
            when (selectedTabIndex) {
                0 -> {
                    // TAB 1: ACCOUNTS LIST WITH ACTION BUTTONS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Global Dashboard Impact Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (userPreferences.excludeAllAccountsFromDashboard) Color(0xFF78350F).copy(alpha = 0.35f)
                                else Color(0xFF1E293B)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (userPreferences.excludeAllAccountsFromDashboard) Color(0xFFF59E0B).copy(alpha = 0.6f)
                                else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (userPreferences.excludeAllAccountsFromDashboard) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (userPreferences.excludeAllAccountsFromDashboard) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Afectación en Dashboard",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (userPreferences.excludeAllAccountsFromDashboard)
                                            "Todas las cuentas están excluidas: ningún saldo afecta el disponible ni patrimonio en Dashboard."
                                        else
                                            "Las cuentas activas suman al saldo disponible y patrimonio neto del Dashboard.",
                                        fontSize = 11.sp,
                                        color = if (userPreferences.excludeAllAccountsFromDashboard) Color(0xFFFDE68A) else Color(0xFF94A3B8),
                                        lineHeight = 15.sp
                                    )
                                }

                                Switch(
                                    checked = !userPreferences.excludeAllAccountsFromDashboard,
                                    onCheckedChange = { active ->
                                        coroutineScope.launch {
                                            settingsRepository?.setExcludeAllAccountsFromDashboard(userId, !active)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF007BFF),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFF475569)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search bar
                        KashSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Buscar por nombre o banco...",
                            onClear = { searchQuery = "" },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredAccounts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No se encontraron cuentas" else "No tienes cuentas registradas",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onOpenCreateAccount,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                                    ) {
                                        Text("Crear primera cuenta", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredAccounts, key = { it.id }) { account ->
                                    val isExcluded = userPreferences.excludedAccountIds.contains(account.id)
                                    BankAccountManagementCard(
                                        account = account,
                                        isGloballyExcluded = userPreferences.excludeAllAccountsFromDashboard,
                                        isIndividuallyExcluded = isExcluded,
                                        onEditClick = { accountToEdit = account },
                                        onStatsClick = { accountForStats = account },
                                        onTransactionsClick = { accountForTransactions = account },
                                        onDeleteClick = { accountToDelete = account }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(20.dp)) }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: OVERALL ACCOUNT STATISTICS & MACHINE LEARNING PROJECTIONS
                    AccountMachineLearningAnalyticsTab(
                        accounts = accounts,
                        allTransactions = allTransactions,
                        totalBalance = totalBalance
                    )
                }

                2 -> {
                    // TAB 3: ACCOUNT TRANSACTIONS & CRYPTOGRAPHIC AUDIT LEDGER
                    AccountTransactionsAuditedTab(
                        accounts = accounts,
                        allTransactions = allTransactions
                    )
                }
            }
        }
    }

    // ==========================================
    // MODAL: MODIFICAR / EDITAR CUENTA
    // ==========================================
    accountToEdit?.let { targetAcc ->
        val isExcluded = userPreferences.excludedAccountIds.contains(targetAcc.id)
        EditAccountModal(
            account = targetAcc,
            isInitiallyExcluded = isExcluded,
            onDismiss = { accountToEdit = null },
            onSave = { updatedAccount, newExcludedState ->
                coroutineScope.launch {
                    financialRepository.updateAccount(updatedAccount)
                    settingsRepository?.setAccountExcludedFromDashboard(userId, updatedAccount.id, newExcludedState)
                    accountToEdit = null
                }
            }
        )
    }

    // ==========================================
    // MODAL: ESTADÍSTICAS INDIVIDUALES DE CUENTA
    // ==========================================
    accountForStats?.let { targetAcc ->
        SingleAccountStatsModal(
            account = targetAcc,
            transactions = allTransactions.filter { it.accountId == targetAcc.id },
            totalBalance = totalBalance,
            onDismiss = { accountForStats = null }
        )
    }

    // ==========================================
    // MODAL: MOVIMIENTOS DE CUENTA
    // ==========================================
    accountForTransactions?.let { targetAcc ->
        SingleAccountTransactionsModal(
            account = targetAcc,
            transactions = allTransactions.filter { it.accountId == targetAcc.id },
            onDismiss = { accountForTransactions = null }
        )
    }

    // ==========================================
    // DIALOG: CONFIRMAR ELIMINACIÓN DE CUENTA
    // ==========================================
    accountToDelete?.let { targetAcc ->
        val associatedTransactionsCount = allTransactions.count { it.accountId == targetAcc.id }
        if (associatedTransactionsCount > 0) {
            // POPUP DE ALERTA: BLOQUEAR ELIMINACIÓN DE CUENTA CON MOVIMIENTOS
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Bloqueado",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Eliminar y Reversar Cuenta",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "La cuenta '${targetAcc.name}' (${targetAcc.bankName}) posee $associatedTransactionsCount movimientos contables registrados.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val isCredit = targetAcc.type.equals("CREDIT", ignoreCase = true) ||
                                targetAcc.type.equals("CREDIT_CARD", ignoreCase = true) ||
                                targetAcc.name.contains("Tarjeta", ignoreCase = true)

                        if (isCredit) {
                            Text(
                                text = "Al eliminar esta tarjeta, se cancela la deuda asociada en el pasivo, lo que disminuye tus obligaciones y aumenta tu patrimonio neto en el Dashboard.",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        } else {
                            Text(
                                text = "Al eliminar esta cuenta, su saldo se desincorpora del dinero disponible y la información se reversa en el Dashboard.",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        Text(
                            text = "Para transparencia y cumplimiento fiscal, puedes conservar el historial en la pista de auditoría contable o purgar todo el registro.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$associatedTransactionsCount transacciones respaldadas en auditoría",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    financialRepository.deleteAccountWithAudit(targetAcc.id, preserveAuditTrail = true)
                                    android.widget.Toast.makeText(context, "Cuenta eliminada y reversada. Auditoría conservada.", android.widget.Toast.LENGTH_SHORT).show()
                                    accountToDelete = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Conservar en Auditoría y Reversar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    financialRepository.deleteAccountWithAudit(targetAcc.id, preserveAuditTrail = false)
                                    android.widget.Toast.makeText(context, "Cuenta y transacciones purgadas definitivamente.", android.widget.Toast.LENGTH_SHORT).show()
                                    accountToDelete = null
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Purgar todo definitivamente", fontSize = 12.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(20.dp)
            )
        } else {
            // Cuenta limpia sin movimientos -> Sí se permite eliminar
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = {
                    Text(text = "Eliminar cuenta bancaria", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas eliminar '${targetAcc.name}' de ${targetAcc.bankName}? Esta cuenta no registra transacciones activas y será removida de Room Local DB.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                financialRepository.deleteAccount(targetAcc.id)
                                accountToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Eliminar definitivamente", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

// ==========================================
// COMPONENT: BANK ACCOUNT CARD WITH ACTIONS
// ==========================================
@Composable
private fun BankAccountManagementCard(
    account: AccountEntity,
    isGloballyExcluded: Boolean = false,
    isIndividuallyExcluded: Boolean = false,
    onEditClick: () -> Unit,
    onStatsClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardBg = Color(0xFF1E293B)
    val customColor = try {
        Color(android.graphics.Color.parseColor(account.customColorHex.ifBlank { "#0284C7" }))
    } catch (_: Exception) {
        Color(0xFF0284C7)
    }

    val icon = when (account.type.uppercase()) {
        "SAVINGS", "AHORRO" -> Icons.Default.Savings
        "CHECKING", "CORRIENTE" -> Icons.Default.CreditCard
        "INVESTMENT", "INVERSION", "CDT" -> Icons.Default.TrendingUp
        "CASH", "EFECTIVO" -> Icons.Default.Payments
        else -> Icons.Default.AccountBalance
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon + Name + Bank + Currency Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(customColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = customColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = account.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${account.bankName} • ${account.type}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = account.currency,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Balance
            Text(
                text = "Saldo Actual",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            CurrencyText(
                amountDouble = account.balance,
                currencyCode = account.currency,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = if (account.balance < 0) Color(0xFFEF4444) else Color.White
            )

            // Live Cross Currency Conversion
            val context = LocalContext.current
            val exchangeRateRepository = remember(context) {
                try {
                    (context.applicationContext as? KashAppApplication)?.appContainer?.exchangeRateRepository
                } catch (_: Throwable) {
                    null
                }
            }
            val targetConversionCurr = if (account.currency == "COP") "USD" else "COP"
            var cardEffectiveRate by remember { mutableStateOf<EffectiveRate?>(null) }

            LaunchedEffect(account.currency, targetConversionCurr, exchangeRateRepository) {
                exchangeRateRepository?.let { repo ->
                    try {
                        cardEffectiveRate = repo.getInstantEffectiveRate(account.currency, targetConversionCurr)
                        repo.observeEffectiveRate(account.currency, targetConversionCurr).collect {
                            cardEffectiveRate = it
                        }
                    } catch (_: Throwable) {
                        // Ignore rate resolution failure in offline/test mode
                    }
                }
            }

            if (account.balance != 0.0 && cardEffectiveRate != null && cardEffectiveRate!!.rate > 0) {
                val convertedEquivalent = account.balance * cardEffectiveRate!!.rate
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "≈ ${MoneyFormatter.format(BigDecimal.valueOf(convertedEquivalent), false, targetConversionCurr)}",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "(${if (targetConversionCurr == "COP") "en COP" else "en USD"} en vivo)",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Dashboard Impact Pill & Overdraft Badge
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isGloballyExcluded) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                            Text("Excluida globalmente", fontSize = 11.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (isIndividuallyExcluded) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                            Text("Excluida del Dashboard", fontSize = 11.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(11.dp))
                            Text("Impacta Dashboard", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (account.hasOverdraft) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Sobregiro",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: [Modificar] [Estadísticas] [Movimientos] [Eliminar]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Modificar / Editar
                Surface(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Modificar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Estadísticas
                Surface(
                    onClick = onStatsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Métricas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Movimientos
                Surface(
                    onClick = onTransactionsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Movimientos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Eliminar Icon
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// MODAL: EDIT / MODIFY ACCOUNT
// ==========================================
@Composable
private fun EditAccountModal(
    account: AccountEntity,
    isInitiallyExcluded: Boolean,
    onDismiss: () -> Unit,
    onSave: (AccountEntity, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var bankName by remember { mutableStateOf(account.bankName) }
    var balanceText by remember { mutableStateOf(String.format(Locale.ROOT, "%.2f", account.balance)) }
    var selectedType by remember { mutableStateOf(account.type) }
    var selectedCurrency by remember { mutableStateOf(account.currency) }
    var selectedColor by remember { mutableStateOf(account.customColorHex.ifBlank { "#0284C7" }) }
    var hasOverdraft by remember { mutableStateOf(account.hasOverdraft) }
    var isExcludedFromDashboard by remember { mutableStateOf(isInitiallyExcluded) }
    var isCurrencyPickerOpen by remember { mutableStateOf(false) }

    val accountTypes = listOf("SAVINGS" to "Ahorros", "CHECKING" to "Corriente", "INVESTMENT" to "Inversión", "CASH" to "Efectivo")
    val colorPalette = listOf("#0284C7", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899", "#06B6D4", "#0F172A")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .imePadding(),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modificar Cuenta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                Text("Nombre de la Cuenta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF007BFF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bank Name Input
                Text("Banco o Entidad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF007BFF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Balance Input
                Text("Saldo Actual", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF007BFF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                // Currency selector
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Divisa:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    Surface(
                        onClick = { isCurrencyPickerOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF007BFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = selectedCurrency, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Live conversion preview
                val balDouble = CurrencyConversionEngine.parseUserInputAmount(balanceText)
                Spacer(modifier = Modifier.height(8.dp))
                LiveCurrencyConversionPreview(
                    amount = balDouble,
                    inputCurrency = selectedCurrency,
                    isDarkTheme = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Account Type Chips
                Text("Tipo de Cuenta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accountTypes.forEach { (code, label) ->
                        val isSelected = selectedType.equals(code, ignoreCase = true)
                        Surface(
                            onClick = { selectedType = code },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF007BFF) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF007BFF) else Color(0xFF334155)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Picker
                Text("Color Corporativo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPalette.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Overdraft toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permitir Sobregiro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Permite balance negativo en transacciones", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = hasOverdraft,
                        onCheckedChange = { hasOverdraft = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF007BFF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dashboard Impact toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Afectar Dashboard y Patrimonio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (!isExcludedFromDashboard) "Esta cuenta suma al disponible y patrimonio del Dashboard"
                            else "Cuenta excluida del cálculo en Dashboard",
                            fontSize = 11.sp,
                            color = if (!isExcludedFromDashboard) Color(0xFF34D399) else Color(0xFFFBBF24)
                        )
                    }
                    Switch(
                        checked = !isExcludedFromDashboard,
                        onCheckedChange = { isExcludedFromDashboard = !it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF007BFF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        val updated = account.copy(
                            name = name.trim().ifBlank { account.name },
                            bankName = bankName.trim().ifBlank { account.bankName },
                            balance = CurrencyConversionEngine.parseUserInputAmount(balanceText),
                            currency = selectedCurrency,
                            type = selectedType,
                            customColorHex = selectedColor,
                            hasOverdraft = hasOverdraft
                        )
                        onSave(updated, isExcludedFromDashboard)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (isCurrencyPickerOpen) {
        CurrencyPickerModal(
            selectedCurrencyCode = selectedCurrency,
            onCurrencySelected = {
                selectedCurrency = it
                isCurrencyPickerOpen = false
            },
            onDismiss = { isCurrencyPickerOpen = false }
        )
    }
}

// ==========================================
// MODAL: SINGLE ACCOUNT STATS
// ==========================================
@Composable
private fun SingleAccountStatsModal(
    account: AccountEntity,
    transactions: List<TransactionEntity>,
    totalBalance: Double,
    onDismiss: () -> Unit
) {
    val totalInflow = transactions.filter { it.type == "INCOME" || it.type == "TRANSFER_IN" }.sumOf { it.amount }
    val totalOutflow = transactions.filter { it.type == "EXPENSE" || it.type == "TRANSFER_OUT" }.sumOf { it.amount }
    val netCashflow = totalInflow - totalOutflow
    val shareOfTotal = if (totalBalance > 0) ((account.balance / totalBalance) * 100).coerceIn(0.0, 100.0) else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estadísticas de Cuenta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(account.name, fontSize = 12.sp, color = Color(0xFFA78BFA), fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                StatItemCard("Saldo Disponible", MoneyFormatter.format(BigDecimal.valueOf(account.balance), false, account.currency), Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                StatItemCard("Participación en Patrimonio", String.format(Locale.ROOT, "%.1f%% del total", shareOfTotal), Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(8.dp))
                StatItemCard("Total Ingresos Entrantes", "+ ${MoneyFormatter.format(BigDecimal.valueOf(totalInflow), false, account.currency)}", Color(0xFF10B981))
                Spacer(modifier = Modifier.height(8.dp))
                StatItemCard("Total Gastos Salientes", "- ${MoneyFormatter.format(BigDecimal.valueOf(totalOutflow), false, account.currency)}", Color(0xFFEF4444))
                Spacer(modifier = Modifier.height(8.dp))
                StatItemCard("Flujo Neto de Caja", MoneyFormatter.format(BigDecimal.valueOf(netCashflow), false, account.currency), if (netCashflow >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                Spacer(modifier = Modifier.height(8.dp))
                StatItemCard("Movimientos Registrados", "${transactions.size} transacciones", Color(0xFFCBD5E1))

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// MODAL: SINGLE ACCOUNT TRANSACTIONS LIST
// ==========================================
@Composable
private fun SingleAccountTransactionsModal(
    account: AccountEntity,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Movimientos de Cuenta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${account.name} • ${transactions.size} movimientos", fontSize = 12.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay transacciones registradas en esta cuenta", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(min = 160.dp, max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions, key = { it.id }) { tx ->
                            TransactionRowItem(tx)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Modular tabs AccountMachineLearningAnalyticsTab and AccountTransactionsAuditedTab used above.

// ==========================================
// HELPER COMPONENTS
// ==========================================
@Composable
private fun SummaryPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatItemCard(title: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun TransactionRowItem(tx: TransactionEntity) {
    val isIncome = tx.type == "INCOME" || tx.type == "TRANSFER_IN"
    val isTransfer = tx.type == "TRANSFER" || tx.type == "TRANSFER_OUT" || tx.type == "TRANSFER_IN"

    val iconColor = if (isIncome) Color(0xFF10B981) else if (isTransfer) Color(0xFF818CF8) else Color(0xFFEF4444)
    val icon = if (isIncome) Icons.Default.TrendingUp else if (isTransfer) Icons.Default.Payments else Icons.Default.TrendingDown

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.5.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = tx.description ?: tx.categoryName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tx.date} • ${tx.accountName}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            CurrencyText(
                amountDouble = tx.amount,
                currencyCode = tx.currency,
                prefix = if (isIncome) "+ " else "- ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}
