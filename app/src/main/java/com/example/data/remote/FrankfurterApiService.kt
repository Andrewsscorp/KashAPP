package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data model for Frankfurter API Latest Rates response
 */
data class LatestRatesResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
    val provider: String = "Frankfurter API (BCE)"
)

/**
 * Data model for Frankfurter API Historical Time Series response
 */
data class HistoricalRatesResponse(
    val amount: Double,
    val base: String,
    val startDate: String,
    val endDate: String,
    val rates: Map<String, Map<String, Double>> // Map of "YYYY-MM-DD" -> { "EUR": 0.89 }
)

/**
 * Enterprise real-time currency network client.
 * Primary: Frankfurter Open Exchange Rates API v1 (https://api.frankfurter.dev/v1) for European Central Bank reference rates.
 * Secondary / Global Complement: Open Exchange Rate API (https://open.er-api.com/v6/latest) for currencies not tracked by the ECB (e.g., COP, PEN, CLP, ARS).
 */
class FrankfurterApiService {

    companion object {
        private const val TAG = "FrankfurterApiService"
        const val BASE_URL = "https://api.frankfurter.dev/v1"
        const val OPEN_RATES_URL = "https://open.er-api.com/v6/latest"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000

        val ECB_CURRENCIES = setOf(
            "AUD", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD",
            "HUF", "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "MYR", "NOK",
            "NZD", "PHP", "PLN", "RON", "SEK", "SGD", "THB", "TRY", "USD", "ZAR"
        )
    }

    /**
     * Fetches current real-time exchange rates for a given base currency.
     * Uses Frankfurter v1 for ECB pairs, or Open Rates for global currencies like COP.
     */
    suspend fun fetchLatestRates(
        baseCurrency: String,
        targetCurrencies: List<String>? = null
    ): Result<LatestRatesResponse> = withContext(Dispatchers.IO) {
        val baseParam = baseCurrency.trim().uppercase()
        val targetsUpper = targetCurrencies?.map { it.trim().uppercase() }

        // If base and targets are all within ECB reference currencies, query Frankfurter directly
        val canUseFrankfurterDirectly = ECB_CURRENCIES.contains(baseParam) &&
                (targetsUpper == null || targetsUpper.all { ECB_CURRENCIES.contains(it) })

        if (canUseFrankfurterDirectly) {
            val frankfurterResult = queryFrankfurterLatest(baseParam, targetsUpper)
            if (frankfurterResult.isSuccess) {
                return@withContext frankfurterResult
            }
        }

        // If base is COP or target contains COP/global currencies, use Open Rates (free, live, 160+ currencies)
        val openRatesResult = queryOpenRates(baseParam, targetsUpper)
        if (openRatesResult.isSuccess) {
            return@withContext openRatesResult
        }

        // If Open Rates failed, fallback to Frankfurter if base is supported
        if (ECB_CURRENCIES.contains(baseParam)) {
            val fallbackFrankfurter = queryFrankfurterLatest(baseParam, targetsUpper?.filter { ECB_CURRENCIES.contains(it) })
            if (fallbackFrankfurter.isSuccess) {
                return@withContext fallbackFrankfurter
            }
        }

        Result.failure(IllegalStateException("No se pudo obtener tasas en vivo para $baseParam"))
    }

    private fun queryFrankfurterLatest(base: String, targets: List<String>?): Result<LatestRatesResponse> {
        return try {
            val urlString = buildString {
                append("$BASE_URL/latest?from=$base")
                if (!targets.isNullOrEmpty()) {
                    val symbols = targets.filter { it != base }.joinToString(",")
                    if (symbols.isNotEmpty()) {
                        append("&to=$symbols")
                    }
                }
            }

            val (statusCode, responseBody) = executeHttpGet(urlString)
            if (statusCode in 200..299) {
                val json = JSONObject(responseBody)
                val amount = json.optDouble("amount", 1.0)
                val respBase = json.optString("base", base)
                val date = json.optString("date", "")
                val ratesObj = json.getJSONObject("rates")

                val ratesMap = mutableMapOf<String, Double>()
                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val r = ratesObj.optDouble(key, Double.NaN)
                    if (r.isFinite() && r > 0.0 && r < 1_000_000_000.0) {
                        ratesMap[key] = r
                    }
                }

                Result.success(
                    LatestRatesResponse(
                        amount = amount,
                        base = respBase,
                        date = date,
                        rates = ratesMap,
                        provider = "Frankfurter API (BCE)"
                    )
                )
            } else {
                Result.failure(IllegalStateException("HTTP $statusCode Frankfurter: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryOpenRates(base: String, targets: List<String>?): Result<LatestRatesResponse> {
        return try {
            val urlString = "$OPEN_RATES_URL/$base"
            val (statusCode, responseBody) = executeHttpGet(urlString)

            if (statusCode in 200..299) {
                val json = JSONObject(responseBody)
                val resultStatus = json.optString("result", "")
                if (resultStatus.equals("success", ignoreCase = true)) {
                    val respBase = json.optString("base_code", base)
                    val date = json.optString("time_last_update_utc", "").take(16).ifBlank { "Hoy" }
                    val ratesObj = json.getJSONObject("rates")

                    val ratesMap = mutableMapOf<String, Double>()
                    if (!targets.isNullOrEmpty()) {
                        for (target in targets) {
                            if (ratesObj.has(target)) {
                                val r = ratesObj.optDouble(target, Double.NaN)
                                if (r.isFinite() && r > 0.0 && r < 1_000_000_000.0) {
                                    ratesMap[target] = r
                                }
                            }
                        }
                    } else {
                        val keys = ratesObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val r = ratesObj.optDouble(key, Double.NaN)
                            if (r.isFinite() && r > 0.0 && r < 1_000_000_000.0) {
                                ratesMap[key] = r
                            }
                        }
                    }

                    Result.success(
                        LatestRatesResponse(
                            amount = 1.0,
                            base = respBase,
                            date = date,
                            rates = ratesMap,
                            provider = "Mercado en Vivo (Frankfurter/Open)"
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("API retorno error: $responseBody"))
                }
            } else {
                Result.failure(IllegalStateException("HTTP $statusCode al consultar tasas abiertas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches historical exchange rate time series for a date range.
     * Endpoint: GET /v1/YYYY-MM-DD..YYYY-MM-DD?from=USD&to=EUR
     */
    suspend fun fetchHistoricalRates(
        baseCurrency: String,
        targetCurrency: String,
        startDate: String,
        endDate: String
    ): Result<HistoricalRatesResponse> = withContext(Dispatchers.IO) {
        try {
            val baseParam = baseCurrency.trim().uppercase()
            val targetParam = targetCurrency.trim().uppercase()

            // If both are ECB currencies, use Frankfurter v1 time series
            if (ECB_CURRENCIES.contains(baseParam) && ECB_CURRENCIES.contains(targetParam)) {
                val urlString = "$BASE_URL/$startDate..$endDate?from=$baseParam&to=$targetParam"
                val (statusCode, responseBody) = executeHttpGet(urlString)

                if (statusCode in 200..299) {
                    val json = JSONObject(responseBody)
                    val amount = json.optDouble("amount", 1.0)
                    val base = json.optString("base", baseParam)
                    val start = json.optString("start_date", startDate)
                    val end = json.optString("end_date", endDate)
                    val ratesObj = json.getJSONObject("rates")

                    val timeSeriesMap = mutableMapOf<String, Map<String, Double>>()
                    val dateKeys = ratesObj.keys()
                    while (dateKeys.hasNext()) {
                        val d = dateKeys.next()
                        val subObj = ratesObj.getJSONObject(d)
                        val subMap = mutableMapOf<String, Double>()
                        val cKeys = subObj.keys()
                        while (cKeys.hasNext()) {
                            val c = cKeys.next()
                            subMap[c] = subObj.getDouble(c)
                        }
                        timeSeriesMap[d] = subMap
                    }

                    return@withContext Result.success(
                        HistoricalRatesResponse(
                            amount = amount,
                            base = base,
                            startDate = start,
                            endDate = end,
                            rates = timeSeriesMap
                        )
                    )
                }
            }

            // Fallback for currencies like COP (generate smooth baseline series based on live rate)
            Result.failure(IllegalStateException("Historical series not directly available on ECB for $targetParam"))
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar historico: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches list of available currencies from Frankfurter + Global catalog.
     */
    suspend fun fetchAvailableCurrencies(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val urlString = "$BASE_URL/currencies"
            val (statusCode, responseBody) = executeHttpGet(urlString)

            val map = mutableMapOf<String, String>()
            if (statusCode in 200..299) {
                val json = JSONObject(responseBody)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getString(key)
                }
            }

            // Ensure COP, USD, EUR, etc. are always present
            map["COP"] = "Peso Colombiano"
            map["USD"] = "Dólar Estadounidense"
            map["EUR"] = "Euro"
            map["MXN"] = "Peso Mexicano"
            map["GBP"] = "Libra Esterlina"
            map["PEN"] = "Sol Peruano"
            map["CLP"] = "Peso Chileno"
            map["BRL"] = "Real Brasileño"

            Result.success(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error de red al consultar divisas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Measures network latency in milliseconds by pinging the Frankfurter API.
     */
    suspend fun pingLatency(): Result<Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val url = URL("$BASE_URL/currencies")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "KashApp-Android/2.4")
            }
            connection.connect()
            val code = connection.responseCode
            connection.disconnect()
            val latency = System.currentTimeMillis() - startTime
            if (code in 200..299) {
                Result.success(latency)
            } else {
                Result.failure(IllegalStateException("HTTP $code (${latency}ms)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeHttpGet(urlString: String): Pair<Int, String> {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "KashApp-Android/2.4")
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
        val reader = BufferedReader(InputStreamReader(stream))
        val response = reader.use { it.readText() }
        connection.disconnect()
        return Pair(statusCode, response)
    }
}
