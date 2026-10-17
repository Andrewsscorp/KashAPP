package com.example.domain.usecase

import com.example.domain.models.TransactionItemDto

/**
 * Returns recent transactions for the operational table with Drive receipt integration.
 */
class GetRecentTransactionsUseCase {

    operator fun invoke(): List<TransactionItemDto> {
        return listOf(
            TransactionItemDto(
                id = "tx_1",
                dateFormatted = "25 Sep 2025",
                description = "Éxito",
                category = "Alimentación",
                account = "Cuenta Ahorros",
                amount = 185000.0,
                isIncome = false,
                driveFileId = "drive_receipt_exito_01"
            ),
            TransactionItemDto(
                id = "tx_2",
                dateFormatted = "24 Sep 2025",
                description = "Salario",
                category = "Ingresos",
                account = "Nómina",
                amount = 5000000.0,
                isIncome = true,
                driveFileId = "drive_paystub_nomina_01"
            ),
            TransactionItemDto(
                id = "tx_3",
                dateFormatted = "23 Sep 2025",
                description = "Uber",
                category = "Transporte",
                account = "Cuenta Corriente",
                amount = 32500.0,
                isIncome = false,
                driveFileId = "drive_receipt_uber_01"
            ),
            TransactionItemDto(
                id = "tx_4",
                dateFormatted = "22 Sep 2025",
                description = "Netflix",
                category = "Entretenimiento",
                account = "Tarjeta de Crédito",
                amount = 26900.0,
                isIncome = false,
                driveFileId = "drive_receipt_netflix_01"
            ),
            TransactionItemDto(
                id = "tx_5",
                dateFormatted = "21 Sep 2025",
                description = "Internet",
                category = "Servicios",
                account = "Cuenta Ahorros",
                amount = 89900.0,
                isIncome = false,
                driveFileId = "drive_bill_internet_01"
            )
        )
    }
}
