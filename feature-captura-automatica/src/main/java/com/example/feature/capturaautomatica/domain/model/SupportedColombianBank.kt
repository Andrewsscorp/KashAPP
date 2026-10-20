package com.example.feature.capturaautomatica.domain.model

enum class SupportedColombianBank(
    val displayName: String,
    val bankNameInKashApp: String,
    val packageNames: Set<String>
) {
    BANCOLOMBIA(
        displayName = "Bancolombia",
        bankNameInKashApp = "Bancolombia",
        packageNames = setOf("com.todo1.mobile", "com.bancolombia.alamanov2")
    ),
    NEQUI(
        displayName = "Nequi",
        bankNameInKashApp = "Nequi",
        packageNames = setOf("com.nequi.MobileApp")
    ),
    DAVIPLATA(
        displayName = "DaviPlata",
        bankNameInKashApp = "DaviPlata",
        packageNames = setOf("com.davivienda.daviplataapp")
    ),
    NU_COLOMBIA(
        displayName = "Nu Colombia",
        bankNameInKashApp = "Nu",
        packageNames = setOf("com.nu.production")
    ),
    DAVIVIENDA(
        displayName = "Davivienda",
        bankNameInKashApp = "Davivienda",
        packageNames = setOf("com.davivienda.daviviendaapp")
    ),
    BBVA(
        displayName = "BBVA Colombia",
        bankNameInKashApp = "BBVA",
        packageNames = setOf("com.bbva.bbvacolombia")
    ),
    LULO_BANK(
        displayName = "Lulo Bank",
        bankNameInKashApp = "Lulo Bank",
        packageNames = setOf("com.lulobank.app")
    );

    companion object {
        val ALLOWED_PACKAGES: Set<String> by lazy {
            entries.flatMap { it.packageNames }.toSet()
        }

        fun findByPackage(packageName: String): SupportedColombianBank? {
            return entries.firstOrNull { it.packageNames.contains(packageName) }
        }
    }
}
