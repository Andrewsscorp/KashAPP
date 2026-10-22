package com.example.feature.impuestosdian.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.feature.impuestosdian.data.export.DianPreRentaPdfExporter
import com.example.feature.impuestosdian.data.repository.DianTaxRepositoryImpl
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class DianTaxUiState(
    val isLoading: Boolean = true,
    val report: DianTaxRadarReport? = null,
    val isConfigDialogOpen: Boolean = false,
    val exportedPdfFile: File? = null,
    val statusMessage: String? = null
)

class DianTaxViewModel(
    private val repository: DianTaxRepositoryImpl,
    private val pdfExporter: DianPreRentaPdfExporter,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DianTaxUiState())
    val uiState: StateFlow<DianTaxUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeTaxRadarReport(userId).collectLatest { radarReport ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = radarReport
                    )
                }
            }
        }
    }

    fun toggleAccountTaxExempt(accountId: String, isExempt: Boolean) {
        viewModelScope.launch {
            repository.toggleAccountTaxExempt(userId, accountId, isExempt)
            _uiState.update {
                it.copy(
                    statusMessage = if (isExempt) {
                        "Cuenta marcada como exenta de impuesto bancario (sincronizada en Cuentas)."
                    } else {
                        "Exención desactivada para esta cuenta."
                    }
                )
            }
        }
    }

    fun selectJurisdictionQuick(jurisdiction: TaxJurisdiction) {
        val currentProfile = _uiState.value.report?.configProfile ?: TaxConfigurationProfile(userId = userId)
        val updated = currentProfile.copy(
            jurisdiction = jurisdiction,
            currency = jurisdiction.defaultCurrency,
            taxUnitName = jurisdiction.taxUnitName,
            taxUnitValue = jurisdiction.defaultTaxUnitValue,
            vatOrSalesTaxRate = jurisdiction.standardVatOrSalesTaxRate,
            financialTransactionTaxRate = jurisdiction.financialTransactionTaxRate,
            electronicInvoiceDeductionRate = jurisdiction.electronicInvoiceDeductionRate,
            effectiveIncomeTaxRate = jurisdiction.effectiveIncomeTaxEstimateRate,
            enforceSingleExemptAccountInColombia = jurisdiction == TaxJurisdiction.CO_DIAN
        )
        repository.updateTaxConfiguration(updated)
        _uiState.update {
            it.copy(statusMessage = "Régimen actualizado a ${jurisdiction.flagEmoji} ${jurisdiction.countryName} (${jurisdiction.authorityAcronym})")
        }
    }

    fun saveTaxConfiguration(newProfile: TaxConfigurationProfile) {
        repository.updateTaxConfiguration(newProfile)
        _uiState.update {
            it.copy(
                isConfigDialogOpen = false,
                statusMessage = "Configuración tributaria guardada y vinculada con Cuentas, Movimientos e Informes."
            )
        }
    }

    fun openConfigDialog() {
        _uiState.update { it.copy(isConfigDialogOpen = true) }
    }

    fun closeConfigDialog() {
        _uiState.update { it.copy(isConfigDialogOpen = false) }
    }

    fun exportPreRentaPdf(onExported: (File) -> Unit = {}) {
        val currentReport = _uiState.value.report ?: return
        viewModelScope.launch {
            try {
                val file = pdfExporter.exportPreRentaPdf(currentReport)
                _uiState.update {
                    it.copy(
                        exportedPdfFile = file,
                        statusMessage = "Certificado Pre-Renta generado con sello SHA-256: ${file.name}"
                    )
                }
                onExported(file)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "Error exportando PDF Pre-Renta: ${e.message}")
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(
        private val repository: DianTaxRepositoryImpl,
        private val pdfExporter: DianPreRentaPdfExporter,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DianTaxViewModel(repository, pdfExporter, userId) as T
        }
    }
}
