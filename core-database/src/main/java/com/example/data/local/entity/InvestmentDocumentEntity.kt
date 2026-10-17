package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a document attached to an Investment (Comprobante, Extracto, Contrato, etc.).
 */
@Entity(
    tableName = "investment_documents",
    foreignKeys = [
        ForeignKey(
            entity = InvestmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["investmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["investmentId"]),
        Index(value = ["docCategory"])
    ]
)
data class InvestmentDocumentEntity(
    @PrimaryKey
    val id: String,
    val investmentId: String,
    val docCategory: String = "COMPROBANTE", // "COMPROBANTE", "EXTRACTO", "DOCUMENTO", "OTROS"
    val fileUrl: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val mimeType: String = "application/pdf",
    val createdAt: Long = System.currentTimeMillis()
)
