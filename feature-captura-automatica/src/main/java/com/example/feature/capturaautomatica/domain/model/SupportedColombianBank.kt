package com.example.feature.capturaautomatica.domain.model

/**
 * Catálogo exhaustivo de entidades bancarias, billeteras digitales y emisores de tarjetas en Colombia.
 * Permite filtrar notificaciones por nombre de paquete oficial y mensajes SMS transaccionales legítimos.
 */
enum class SupportedColombianBank(
    val displayName: String,
    val bankNameInKashApp: String,
    val packageNames: Set<String>,
    val brandColorHex: String = "#0284C7"
) {
    BANCOLOMBIA(
        displayName = "Bancolombia",
        bankNameInKashApp = "Bancolombia",
        packageNames = setOf("com.todo1.mobile", "com.bancolombia.alamanov2", "com.bancolombia.personas", "co.com.bancolombia"),
        brandColorHex = "#FDDA24"
    ),
    NEQUI(
        displayName = "Nequi",
        bankNameInKashApp = "Nequi",
        packageNames = setOf("com.nequi.MobileApp", "co.com.nequi.nequipersonas"),
        brandColorHex = "#E00070"
    ),
    DAVIPLATA(
        displayName = "DaviPlata",
        bankNameInKashApp = "DaviPlata",
        packageNames = setOf("com.davivienda.daviplataapp", "co.com.daviplata"),
        brandColorHex = "#ED1C24"
    ),
    NU_COLOMBIA(
        displayName = "Nu Colombia",
        bankNameInKashApp = "Nu",
        packageNames = setOf("com.nu.production", "com.nu.colombia"),
        brandColorHex = "#820AD1"
    ),
    DAVIVIENDA(
        displayName = "Davivienda",
        bankNameInKashApp = "Davivienda",
        packageNames = setOf("com.davivienda.daviviendaapp", "co.com.davivienda.movil"),
        brandColorHex = "#ED1C24"
    ),
    BBVA(
        displayName = "BBVA Colombia",
        bankNameInKashApp = "BBVA",
        packageNames = setOf("com.bbva.bbvacolombia", "co.bbva.netcash"),
        brandColorHex = "#004481"
    ),
    LULO_BANK(
        displayName = "Lulo Bank",
        bankNameInKashApp = "Lulo Bank",
        packageNames = setOf("com.lulobank.app"),
        brandColorHex = "#00E5FF"
    ),
    BANCO_DE_BOGOTA(
        displayName = "Banco de Bogotá",
        bankNameInKashApp = "Banco de Bogotá",
        packageNames = setOf("com.bancodebogota.bancamovil", "com.bancodebogota.bancamovil.release", "co.com.bancodebogota"),
        brandColorHex = "#004884"
    ),
    SCOTIABANK_COLPATRIA(
        displayName = "Scotiabank Colpatria",
        bankNameInKashApp = "Scotiabank Colpatria",
        packageNames = setOf("com.colpatria.banca.movil", "com.scotiabank.colpatria", "co.com.scotiabankcolpatria"),
        brandColorHex = "#E31837"
    ),
    BANCO_DE_OCCIDENTE(
        displayName = "Banco de Occidente",
        bankNameInKashApp = "Banco de Occidente",
        packageNames = setOf("com.bancodeoccidente.bancamovil", "co.com.bancodeoccidente"),
        brandColorHex = "#002F6C"
    ),
    BANCO_POPULAR(
        displayName = "Banco Popular",
        bankNameInKashApp = "Banco Popular",
        packageNames = setOf("com.bancopopular.bancamovil", "co.com.bancopopular"),
        brandColorHex = "#00833E"
    ),
    BANCO_AV_VILLAS(
        displayName = "Banco AV Villas",
        bankNameInKashApp = "Banco AV Villas",
        packageNames = setOf("com.avvillas.bancamovil", "co.com.avvillas"),
        brandColorHex = "#C8102E"
    ),
    BANCO_FALABELLA(
        displayName = "Banco Falabella",
        bankNameInKashApp = "Banco Falabella",
        packageNames = setOf("cl.falabella.bancafalabella.co", "com.falabella.bancafalabella", "co.com.bancofalabella"),
        brandColorHex = "#00843D"
    ),
    BANCO_ITAU(
        displayName = "Banco Itaú",
        bankNameInKashApp = "Banco Itaú",
        packageNames = setOf("com.itau.colombia", "co.com.itau"),
        brandColorHex = "#EC7000"
    ),
    BANCO_CAJA_SOCIAL(
        displayName = "Banco Caja Social",
        bankNameInKashApp = "Banco Caja Social",
        packageNames = setOf("com.bcs.bancamovil", "com.bancocajasocial.bancamovil"),
        brandColorHex = "#005596"
    ),
    DALE(
        displayName = "Dale!",
        bankNameInKashApp = "Dale",
        packageNames = setOf("co.com.dale.app", "com.dale.app", "com.aval.dale"),
        brandColorHex = "#FFCC00"
    ),
    MOVII(
        displayName = "Movii",
        bankNameInKashApp = "Movii",
        packageNames = setOf("co.movii.app", "com.movii.app"),
        brandColorHex = "#28D17C"
    ),
    RAPPIPAY(
        displayName = "RappiPay",
        bankNameInKashApp = "RappiPay",
        packageNames = setOf("co.com.rappipay.app", "com.grability.rappi"),
        brandColorHex = "#FF441F"
    ),
    UALA(
        displayName = "Ualá Colombia",
        bankNameInKashApp = "Ualá",
        packageNames = setOf("com.uala.colombia"),
        brandColorHex = "#F33B47"
    ),
    TUYA(
        displayName = "Tarjeta Tuya",
        bankNameInKashApp = "Tuya",
        packageNames = setOf("co.com.tuya.app", "com.tuya.app"),
        brandColorHex = "#FF5A00"
    ),
    COOPERATIVAS(
        displayName = "Banca Cooperativa",
        bankNameInKashApp = "Cooperativa",
        packageNames = setOf("com.coopcentral.bancamovil", "com.comultrasan.movil", "co.com.confiar"),
        brandColorHex = "#0284C7"
    ),
    ENTIDAD_FINANCIERA(
        displayName = "Entidad Financiera",
        bankNameInKashApp = "Bancario",
        packageNames = emptySet(),
        brandColorHex = "#10B981"
    );

    companion object {
        private val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.xiaomi.channel",
            "com.huawei.message"
        )

        val ALLOWED_PACKAGES: Set<String> by lazy {
            entries.flatMap { it.packageNames }.toSet() + SMS_PACKAGES
        }

        fun findByPackage(packageName: String, title: String? = null, text: String? = null): SupportedColombianBank? {
            // 1. Coincidencia directa por paquete de app bancaria
            val directMatch = entries.firstOrNull { it.packageNames.contains(packageName) }
            if (directMatch != null) return directMatch

            // 2. Si proviene de una app de SMS o similar, identificar banco por contenido
            if (SMS_PACKAGES.contains(packageName) || packageName.contains("messaging", ignoreCase = true) || packageName.contains("mms", ignoreCase = true)) {
                val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()
                return when {
                    combined.contains("bancolombia") || combined.contains("a la mano") -> BANCOLOMBIA
                    combined.contains("nequi") -> NEQUI
                    combined.contains("daviplata") -> DAVIPLATA
                    combined.contains("davivienda") -> DAVIVIENDA
                    combined.contains("nu ") || combined.contains("nubank") || combined.contains("nu colombia") -> NU_COLOMBIA
                    combined.contains("bbva") -> BBVA
                    combined.contains("lulo") -> LULO_BANK
                    combined.contains("banco de bogota") || combined.contains("banco de bogotá") -> BANCO_DE_BOGOTA
                    combined.contains("colpatria") || combined.contains("scotiabank") -> SCOTIABANK_COLPATRIA
                    combined.contains("banco de occidente") || combined.contains("occidente") -> BANCO_DE_OCCIDENTE
                    combined.contains("banco popular") -> BANCO_POPULAR
                    combined.contains("av villas") || combined.contains("villas") -> BANCO_AV_VILLAS
                    combined.contains("falabella") || combined.contains("cmr") -> BANCO_FALABELLA
                    combined.contains("itau") || combined.contains("itaú") -> BANCO_ITAU
                    combined.contains("caja social") -> BANCO_CAJA_SOCIAL
                    combined.contains("dale!") || combined.contains("dale") -> DALE
                    combined.contains("movii") -> MOVII
                    combined.contains("rappipay") || combined.contains("rappicard") -> RAPPIPAY
                    combined.contains("ualá") || combined.contains("uala") -> UALA
                    combined.contains("tuya") || combined.contains("tarjeta exito") || combined.contains("tarjeta éxito") -> TUYA
                    combined.contains("bre-b") || combined.contains("transfiya") || combined.contains("pse") -> ENTIDAD_FINANCIERA
                    else -> null
                }
            }

            return null
        }
    }
}
