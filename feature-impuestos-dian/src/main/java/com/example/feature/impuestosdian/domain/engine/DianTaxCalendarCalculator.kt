package com.example.feature.impuestosdian.domain.engine

import com.example.feature.impuestosdian.domain.model.TaxJurisdiction

object DianTaxCalendarCalculator {

    /**
     * Estima la ventana de vencimiento del Calendario Tributario para Personas Naturales
     * según los últimos dos dígitos del NIT/Cédula/Identificación Fiscal y la jurisdicción activa.
     */
    fun estimateDeadlineWindow(
        fiscalYear: Int,
        lastTwoDigitsInput: String,
        jurisdiction: TaxJurisdiction = TaxJurisdiction.CO_DIAN
    ): String {
        val declarationYear = fiscalYear + 1
        val digits = lastTwoDigitsInput.filter { it.isDigit() }.takeLast(2).padStart(2, '0')

        if (jurisdiction != TaxJurisdiction.CO_DIAN) {
            return when (jurisdiction) {
                TaxJurisdiction.MX_SAT -> "🇲🇽 SAT México: Declaración Anual Personas Físicas del 1 al 30 de Abril de $declarationYear (Año fiscal $fiscalYear)"
                TaxJurisdiction.US_IRS -> "🇺🇸 IRS EE. UU.: Vencimiento Federal Tax Day el 15 de Abril de $declarationYear (Extensión hasta 15 de Octubre)"
                TaxJurisdiction.ES_AEAT -> "🇪🇸 AEAT España: Campaña Renta IRPF entre Abril y el 30 de Junio de $declarationYear"
                TaxJurisdiction.PE_SUNAT -> "🇵🇪 SUNAT Perú: Cronograma Anual según último dígito RUC/DNI ('${digits.last()}') entre Mayo y Junio de $declarationYear"
                TaxJurisdiction.CL_SII -> "🇨🇱 SII Chile: Operación Renta (Formulario 22) del 1 al 30 de Abril de $declarationYear"
                TaxJurisdiction.AR_ARCA -> "🇦🇷 ARCA Argentina: Vencimiento Ganancias y Bienes Personales en Junio de $declarationYear (Terminación CUIT '$digits')"
                TaxJurisdiction.GLOBAL_CUSTOM -> "🌍 Internacional: Verifica el calendario anual de tu autoridad tributaria para el año fiscal $fiscalYear"
                else -> ""
            }
        }

        val numeric = digits.toIntOrNull()
            ?: return "Configura los últimos 2 dígitos de tu Cédula/NIT para ver tu fecha límite en $declarationYear"

        val normalized = if (numeric == 0) 100 else numeric
        val periodDescription = when (normalized) {
            in 1..26 -> "Segunda y tercera semana de Agosto de $declarationYear"
            in 27..66 -> "Primera a cuarta semana de Septiembre de $declarationYear"
            else -> "Primera a cuarta semana de Octubre de $declarationYear"
        }
        return "Dígitos '$digits': Vencimiento estimado en $periodDescription (Declaración del año gravable $fiscalYear)"
    }
}
