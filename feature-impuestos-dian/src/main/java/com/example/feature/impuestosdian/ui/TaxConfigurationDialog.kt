package com.example.feature.impuestosdian.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction

@Composable
fun TaxConfigurationDialog(
    initialProfile: TaxConfigurationProfile,
    onDismiss: () -> Unit,
    onSave: (TaxConfigurationProfile) -> Unit
) {
    var selectedJurisdiction by remember { mutableStateOf(initialProfile.jurisdiction) }
    var fiscalYearText by remember { mutableStateOf(initialProfile.fiscalYear.toString()) }
    var currencyText by remember { mutableStateOf(initialProfile.currency) }
    var unitNameText by remember { mutableStateOf(initialProfile.taxUnitName) }
    var unitValueText by remember { mutableStateOf(initialProfile.taxUnitValue.toInt().toString()) }
    var lastDigitsText by remember { mutableStateOf(initialProfile.taxIdLastDigits) }
    var vatPercentText by remember { mutableStateOf("%.2f".format(initialProfile.vatOrSalesTaxRate * 100.0)) }
    var bankTaxPercentText by remember { mutableStateOf("%.3f".format(initialProfile.financialTransactionTaxRate * 100.0)) }
    var feDeductionPercentText by remember { mutableStateOf("%.1f".format(initialProfile.electronicInvoiceDeductionRate * 100.0)) }
    var singleExemptInColombia by remember { mutableStateOf(initialProfile.enforceSingleExemptAccountInColombia) }
    var autoTagOcr by remember { mutableStateOf(initialProfile.autoTagOcrReceiptsAsElectronicInvoice) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configuración Tributaria Multi-País",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Vinculada en tiempo real con Creación de Cuentas (4x1000/ITF), Gastos/OCR (IVA y Factura Electrónica), Activos Fijos e Informes.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Text(
                    text = "1. Selecciona País / Jurisdicción Fiscal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaxJurisdiction.entries.forEach { jur ->
                        val isSelected = jur == selectedJurisdiction
                        Surface(
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable {
                                selectedJurisdiction = jur
                                currencyText = jur.defaultCurrency
                                unitNameText = jur.taxUnitName
                                unitValueText = jur.defaultTaxUnitValue.toInt().toString()
                                vatPercentText = "%.2f".format(jur.standardVatOrSalesTaxRate * 100.0)
                                bankTaxPercentText = "%.3f".format(jur.financialTransactionTaxRate * 100.0)
                                feDeductionPercentText = "%.1f".format(jur.electronicInvoiceDeductionRate * 100.0)
                                singleExemptInColombia = jur == TaxJurisdiction.CO_DIAN
                            }
                        ) {
                            Text(
                                text = "${jur.flagEmoji} ${jur.countryName} (${jur.authorityAcronym})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fiscalYearText,
                        onValueChange = { fiscalYearText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Año Gravable") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastDigitsText,
                        onValueChange = { lastDigitsText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Últ. 2 Dígitos NIT/ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitNameText,
                        onValueChange = { unitNameText = it },
                        label = { Text("Unidad (UVT/UMA/UIT)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitValueText,
                        onValueChange = { unitValueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Valor 1 $unitNameText ($currencyText)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = vatPercentText,
                        onValueChange = { vatPercentText = it.replace(',', '.') },
                        label = { Text("IVA / Sales Tax (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bankTaxPercentText,
                        onValueChange = { bankTaxPercentText = it.replace(',', '.') },
                        label = { Text("Imp. Bancario/GMF (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = feDeductionPercentText,
                    onValueChange = { feDeductionPercentText = it.replace(',', '.') },
                    label = { Text("% Deducción por Factura Electrónica (Ley 2277 = 1%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Text(
                    text = "2. Vinculación Automática con Otros Módulos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Regla Única Cuenta Exenta (350 UVT)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Al marcar una cuenta como exenta en Creación de Cuentas, desmarca automáticamente la anterior.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = singleExemptInColombia,
                            onCheckedChange = { singleExemptInColombia = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vincular Recibos OCR y Gastos con Factura Electrónica",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Suma automáticamente al beneficio del 1% DIAN / deducción fiscal los gastos con soporte digital.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoTagOcr,
                            onCheckedChange = { autoTagOcr = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val parsedYear = fiscalYearText.toIntOrNull() ?: 2026
                            val parsedUnitVal = unitValueText.toDoubleOrNull() ?: selectedJurisdiction.defaultTaxUnitValue
                            val parsedVat = (vatPercentText.toDoubleOrNull() ?: (selectedJurisdiction.standardVatOrSalesTaxRate * 100.0)) / 100.0
                            val parsedBankTax = (bankTaxPercentText.toDoubleOrNull() ?: (selectedJurisdiction.financialTransactionTaxRate * 100.0)) / 100.0
                            val parsedFeRate = (feDeductionPercentText.toDoubleOrNull() ?: (selectedJurisdiction.electronicInvoiceDeductionRate * 100.0)) / 100.0

                            onSave(
                                initialProfile.copy(
                                    jurisdiction = selectedJurisdiction,
                                    fiscalYear = parsedYear,
                                    currency = currencyText.ifBlank { selectedJurisdiction.defaultCurrency },
                                    taxUnitName = unitNameText.ifBlank { selectedJurisdiction.taxUnitName },
                                    taxUnitValue = parsedUnitVal,
                                    taxIdLastDigits = lastDigitsText.ifBlank { "01" },
                                    vatOrSalesTaxRate = parsedVat,
                                    financialTransactionTaxRate = parsedBankTax,
                                    electronicInvoiceDeductionRate = parsedFeRate,
                                    enforceSingleExemptAccountInColombia = singleExemptInColombia,
                                    autoTagOcrReceiptsAsElectronicInvoice = autoTagOcr
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar y Vincular", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
