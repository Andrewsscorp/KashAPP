package com.example.presentation.fixedassets.export

import android.content.Context
import androidx.core.content.FileProvider
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.util.FinancialReportExporter.ExportResult
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FixedAssetExcelExporter {

    fun exportToExcel(
        context: Context,
        assets: List<FixedAssetEntity>,
        filterDesc: String = "Inventario Completo"
    ): ExportResult {
        return try {
            val sb = StringBuilder()

            // UTF-8 BOM for Microsoft Excel compatibility
            sb.append("\uFEFF")

            sb.append("REPORTE EJECUTIVO DE ACTIVOS FIJOS Y DEPRECIACIÓN CONTABLE - KASHAPP\n")
            sb.append("Fecha de Generación:;${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}\n")
            sb.append("Criterio / Filtro:;\"$filterDesc\"\n")
            sb.append("Total de Activos:;${assets.size}\n\n")

            // Headers
            sb.append("ID;Nombre del Activo;Tipo;Categoría;Fecha Adquisición;Moneda;Valor Adquisición;Método Depreciación;Vida Útil (Meses);Valor Residual (Salvamento);Depreciación Acumulada;Valor Neto en Libros;Valor Comercial Actual;Última Depreciación;Marca;Modelo;Serial / Placa;Ubicación;Estado\n")

            var sumAcq = 0.0
            var sumAccumDep = 0.0
            var sumBookVal = 0.0
            var sumMarketVal = 0.0

            for (asset in assets) {
                val bookVal = AssetFinancialCalculations.calculateCurrentBookValue(
                    asset.acquisitionValue, asset.accumulatedDepreciation, asset.residualValue
                )

                sumAcq += asset.acquisitionValue
                sumAccumDep += asset.accumulatedDepreciation
                sumBookVal += bookVal
                sumMarketVal += asset.currentMarketValue

                sb.append("\"${asset.id}\";")
                sb.append("\"${asset.name.replace("\"", "\"\"")}\";")
                sb.append("\"${asset.type}\";")
                sb.append("\"${asset.category}\";")
                sb.append("\"${asset.acquisitionDate}\";")
                sb.append("\"${asset.currency}\";")
                sb.append("${"%.2f".format(asset.acquisitionValue).replace(".", ",")};")
                sb.append("\"${asset.depreciationMethod}\";")
                sb.append("${asset.usefulLifeMonths};")
                sb.append("${"%.2f".format(asset.residualValue).replace(".", ",")};")
                sb.append("${"%.2f".format(asset.accumulatedDepreciation).replace(".", ",")};")
                sb.append("${"%.2f".format(bookVal).replace(".", ",")};")
                sb.append("${"%.2f".format(asset.currentMarketValue).replace(".", ",")};")
                sb.append("\"${asset.lastDepreciationDate ?: "Sin correr"}\";")
                sb.append("\"${asset.brand ?: ""}\";")
                sb.append("\"${asset.model ?: ""}\";")
                sb.append("\"${asset.serialNumber ?: asset.licensePlate ?: ""}\";")
                sb.append("\"${asset.location ?: ""}\";")
                sb.append("\"${asset.status}\"\n")
            }

            // Totals Row
            sb.append("\nTOTALES CONSOLIDADOS;;;;;COP;")
            sb.append("${"%.2f".format(sumAcq).replace(".", ",")};;;;")
            sb.append("${"%.2f".format(sumAccumDep).replace(".", ",")};")
            sb.append("${"%.2f".format(sumBookVal).replace(".", ",")};")
            sb.append("${"%.2f".format(sumMarketVal).replace(".", ",")};;;;;;\n")

            val fileName = "KashApp_ActivosFijos_Excel_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = sb.toString(),
                message = "Planilla Excel de Activos Fijos generada exitosamente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "Error al exportar Excel de Activos Fijos: ${e.message}"
            )
        }
    }
}
