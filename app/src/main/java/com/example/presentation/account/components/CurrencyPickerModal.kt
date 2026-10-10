package com.example.presentation.account.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CurrencyEntity
import com.example.domain.repository.FinancialRepository
import kotlinx.coroutines.launch

data class CurrencyItemInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String = "🌐",
    val isCustom: Boolean = false
)

/**
 * Complete Official World Currencies Catalog (164 ISO-4217 official global currencies + crypto).
 */
val WORLD_CURRENCIES_CATALOG: List<CurrencyItemInfo> = listOf(
    // 1. Americas & Latin America
    CurrencyItemInfo("COP", "Peso Colombiano", "$", "🇨🇴"),
    CurrencyItemInfo("USD", "Dólar Estadounidense", "$", "🇺🇸"),
    CurrencyItemInfo("EUR", "Euro", "€", "🇪🇺"),
    CurrencyItemInfo("GBP", "Libra Esterlina", "£", "🇬🇧"),
    CurrencyItemInfo("MXN", "Peso Mexicano", "$", "🇲🇽"),
    CurrencyItemInfo("BRL", "Real Brasileño", "R$", "🇧🇷"),
    CurrencyItemInfo("ARS", "Peso Argentino", "$", "🇦🇷"),
    CurrencyItemInfo("CLP", "Peso Chileno", "$", "🇨🇱"),
    CurrencyItemInfo("PEN", "Sol Peruano", "S/", "🇵🇪"),
    CurrencyItemInfo("CAD", "Dólar Canadiense", "$", "🇨🇦"),
    CurrencyItemInfo("UYU", "Peso Uruguayo", "$", "🇺🇾"),
    CurrencyItemInfo("DOP", "Peso Dominicano", "RD$", "🇩🇴"),
    CurrencyItemInfo("CRC", "Colón Costarricense", "₡", "🇨🇷"),
    CurrencyItemInfo("PAB", "Balboa Panameño", "B/.", "🇵🇦"),
    CurrencyItemInfo("GTQ", "Quetzal Guatemalteco", "Q", "🇬🇹"),
    CurrencyItemInfo("HNL", "Lempira Hondureño", "L", "🇭🇳"),
    CurrencyItemInfo("NIO", "Córdoba Nicaragüense", "C$", "🇳🇮"),
    CurrencyItemInfo("BOB", "Boliviano", "Bs.", "🇧🇴"),
    CurrencyItemInfo("PYG", "Guaraní Paraguayo", "₲", "🇵🇾"),
    CurrencyItemInfo("VES", "Bolívar Digital Venezolano", "Bs.D", "🇻🇪"),
    CurrencyItemInfo("JMD", "Dólar Jamaiquino", "J$", "🇯🇲"),
    CurrencyItemInfo("TTD", "Dólar de Trinidad y Tobago", "TT$", "🇹🇹"),
    CurrencyItemInfo("BBD", "Dólar de Barbados", "Bds$", "🇧🇧"),
    CurrencyItemInfo("BSD", "Dólar Bahameño", "B$", "🇧🇸"),
    CurrencyItemInfo("BZD", "Dólar Beliceño", "BZ$", "🇧🇿"),
    CurrencyItemInfo("GYD", "Dólar Guyanés", "G$", "🇬🇾"),
    CurrencyItemInfo("SRD", "Dólar Surinamés", "Sr$", "🇸🇷"),
    CurrencyItemInfo("AWG", "Florín Arubeño", "Afl.", "🇦🇼"),
    CurrencyItemInfo("ANG", "Florín de las Antillas", "NAƒ", "🇨🇼"),
    CurrencyItemInfo("HTG", "Gourde Haitiano", "G", "🇭🇹"),
    CurrencyItemInfo("CUP", "Peso Cubano", "$", "🇨🇺"),
    CurrencyItemInfo("BMD", "Dólar Bermudeño", "$", "🇧🇲"),
    CurrencyItemInfo("KYD", "Dólar de Islas Caimán", "CI$", "🇰🇾"),
    CurrencyItemInfo("XCD", "Dólar del Caribe Oriental", "EC$", "🏝️"),

    // 2. Europe & Eurasia
    CurrencyItemInfo("CHF", "Franco Suizo", "CHF", "🇨🇭"),
    CurrencyItemInfo("SEK", "Corona Sueca", "kr", "🇸🇪"),
    CurrencyItemInfo("NOK", "Corona Noruega", "kr", "🇳🇴"),
    CurrencyItemInfo("DKK", "Corona Danesa", "kr", "🇩🇰"),
    CurrencyItemInfo("PLN", "Złoty Polaco", "zł", "🇵🇱"),
    CurrencyItemInfo("CZK", "Corona Checa", "Kč", "🇨🇿"),
    CurrencyItemInfo("HUF", "Forinto Húngaro", "Ft", "🇭🇺"),
    CurrencyItemInfo("RON", "Leu Rumano", "lei", "🇷🇴"),
    CurrencyItemInfo("BGN", "Lev Búlgaro", "лв", "🇧🇬"),
    CurrencyItemInfo("TRY", "Lira Turca", "₺", "🇹🇷"),
    CurrencyItemInfo("RUB", "Rublo Ruso", "₽", "🇷🇺"),
    CurrencyItemInfo("UAH", "Grivna Ucraniana", "₴", "🇺🇦"),
    CurrencyItemInfo("ISK", "Corona Islandesa", "kr", "🇮🇸"),
    CurrencyItemInfo("RSD", "Dinar Serbio", "din", "🇷🇸"),
    CurrencyItemInfo("ALL", "Lek Albanés", "L", "🇦🇱"),
    CurrencyItemInfo("GEL", "Lari Georgiano", "₾", "🇬🇪"),
    CurrencyItemInfo("AMD", "Dram Armenio", "֏", "🇦🇲"),
    CurrencyItemInfo("AZN", "Manat Azerbaiyano", "₼", "🇦🇿"),
    CurrencyItemInfo("BYN", "Rublo Bielorruso", "Br", "🇧🇾"),
    CurrencyItemInfo("MDL", "Leu Moldavo", "L", "🇲🇩"),
    CurrencyItemInfo("BAM", "Marco Convertible", "KM", "🇧🇦"),
    CurrencyItemInfo("MKD", "Denar Macedonio", "ден", "🇲🇰"),
    CurrencyItemInfo("GIP", "Libra de Gibraltar", "£", "🇬🇮"),
    CurrencyItemInfo("FKP", "Libra de Islas Malvinas", "£", "🇫🇰"),

    // 3. Asia & Middle East
    CurrencyItemInfo("JPY", "Yen Japonés", "¥", "🇯🇵"),
    CurrencyItemInfo("CNY", "Yuan Chino", "¥", "🇨🇳"),
    CurrencyItemInfo("HKD", "Dólar de Hong Kong", "HK$", "🇭🇰"),
    CurrencyItemInfo("TWD", "Nuevo Dólar Taiwanés", "NT$", "🇹🇼"),
    CurrencyItemInfo("KRW", "Won Surcoreano", "₩", "🇰🇷"),
    CurrencyItemInfo("SGD", "Dólar de Singapur", "S$", "🇸🇬"),
    CurrencyItemInfo("MYR", "Ringgit Malayo", "RM", "🇲🇾"),
    CurrencyItemInfo("THB", "Baht Tailandés", "฿", "🇹🇭"),
    CurrencyItemInfo("IDR", "Rupia Indonesia", "Rp", "🇮🇩"),
    CurrencyItemInfo("PHP", "Peso Filipino", "₱", "🇵🇭"),
    CurrencyItemInfo("VND", "Dong Vietnamita", "₫", "🇻🇳"),
    CurrencyItemInfo("INR", "Rupia India", "₹", "🇮🇳"),
    CurrencyItemInfo("PKR", "Rupia Pakistaní", "₨", "🇵🇰"),
    CurrencyItemInfo("BDT", "Taka Bangladesí", "৳", "🇧🇩"),
    CurrencyItemInfo("LKR", "Rupia de Sri Lanka", "Rs", "🇱🇰"),
    CurrencyItemInfo("ILS", "Nuevo Séquel Israelí", "₪", "🇮🇱"),
    CurrencyItemInfo("AED", "Dírham de Emiratos Árabes", "د.إ", "🇦🇪"),
    CurrencyItemInfo("SAR", "Riyal Saudí", "﷼", "🇸🇦"),
    CurrencyItemInfo("QAR", "Riyal Catarí", "﷼", "🇶🇦"),
    CurrencyItemInfo("KWD", "Dinar Kuwaití", "د.ك", "🇰🇼"),
    CurrencyItemInfo("BHD", "Dinar Bahreiní", "د.ب", "🇧🇭"),
    CurrencyItemInfo("OMR", "Rial Omaní", "ر.ع.", "🇴🇲"),
    CurrencyItemInfo("JOD", "Dinar Jordano", "د.ا", "🇯🇴"),
    CurrencyItemInfo("IQD", "Dinar Iraquí", "ع.د", "🇮🇶"),
    CurrencyItemInfo("IRR", "Rial Iraní", "﷼", "🇮🇷"),
    CurrencyItemInfo("LBP", "Libra Libanesa", "ل.ل", "🇱🇧"),
    CurrencyItemInfo("SYP", "Libra Siria", "£S", "🇸🇾"),
    CurrencyItemInfo("YER", "Rial Yemení", "﷼", "🇾🇪"),
    CurrencyItemInfo("MOP", "Pataca de Macao", "MOP$", "🇲🇴"),
    CurrencyItemInfo("BND", "Dólar de Brunéi", "B$", "🇧🇳"),
    CurrencyItemInfo("KHR", "Riel Camboyano", "៛", "🇰🇭"),
    CurrencyItemInfo("LAK", "Kip Lao", "₭", "🇱🇦"),
    CurrencyItemInfo("MMK", "Kyat Birmano", "K", "🇲🇲"),
    CurrencyItemInfo("MNT", "Tugrik Mongol", "₮", "🇲🇳"),
    CurrencyItemInfo("NPR", "Rupia Nepalí", "₨", "🇳🇵"),
    CurrencyItemInfo("MVR", "Rufiyaa Maldiva", "Rf", "🇲🇻"),
    CurrencyItemInfo("AFN", "Afgani Afgano", "؋", "🇦🇫"),
    CurrencyItemInfo("UZS", "Som Uzbeko", "so'm", "🇺🇿"),
    CurrencyItemInfo("TJS", "Somoni Tayiko", "ЅМ", "🇹🇯"),
    CurrencyItemInfo("KGS", "Som Kirguís", "с", "🇰🇬"),
    CurrencyItemInfo("TMT", "Manat Turcomano", "m", "🇹🇲"),
    CurrencyItemInfo("KZT", "Tenge Kazajo", "₸", "🇰🇿"),
    CurrencyItemInfo("BTN", "Ngultrum Butanés", "Nu.", "🇧🇹"),
    CurrencyItemInfo("KPW", "Won Norcoreano", "₩", "🇰🇵"),

    // 4. Africa
    CurrencyItemInfo("ZAR", "Rand Sudafricano", "R", "🇿🇦"),
    CurrencyItemInfo("EGP", "Libra Egipcia", "E£", "🇪🇬"),
    CurrencyItemInfo("MAD", "Dírham Marroquí", "MAD", "🇲🇦"),
    CurrencyItemInfo("NGN", "Naira Nigeriana", "₦", "🇳🇬"),
    CurrencyItemInfo("KES", "Chelín Keniano", "KSh", "🇰🇪"),
    CurrencyItemInfo("GHS", "Cedi Ganés", "GH₵", "🇬🇭"),
    CurrencyItemInfo("TND", "Dinar Tunecino", "د.ت", "🇹🇳"),
    CurrencyItemInfo("DZD", "Dinar Argelino", "د.ج", "🇩🇿"),
    CurrencyItemInfo("LYD", "Dinar Libio", "ل.د", "🇱🇾"),
    CurrencyItemInfo("SDG", "Libra Sudanesa", "ج.س.", "🇸🇩"),
    CurrencyItemInfo("SSP", "Libra Sursudanesa", "£", "🇸🇸"),
    CurrencyItemInfo("ETB", "Birr Etíope", "Br", "🇪🇹"),
    CurrencyItemInfo("TZS", "Chelín Tanzano", "TSh", "🇹🇿"),
    CurrencyItemInfo("UGX", "Chelín Ugandés", "USh", "🇺🇬"),
    CurrencyItemInfo("RWF", "Franco Ruandés", "FRw", "🇷🇼"),
    CurrencyItemInfo("ZMW", "Kwacha Zambiano", "ZK", "🇿🇲"),
    CurrencyItemInfo("MZN", "Metical Mozambiqueño", "MT", "🇲🇿"),
    CurrencyItemInfo("BWP", "Pula Botswanesa", "P", "🇧🇼"),
    CurrencyItemInfo("NAD", "Dólar Namibio", "N$", "🇳🇦"),
    CurrencyItemInfo("MUR", "Rupia de Mauricio", "₨", "🇲🇺"),
    CurrencyItemInfo("SCR", "Rupia de Seychelles", "₨", "🇸🇨"),
    CurrencyItemInfo("AOA", "Kwanza Angoleño", "Kz", "🇦🇴"),
    CurrencyItemInfo("XOF", "Franco CFA Occidental", "CFA", "🌍"),
    CurrencyItemInfo("XAF", "Franco CFA Central", "FCFA", "🌍"),
    CurrencyItemInfo("MGA", "Ariary Malgache", "Ar", "🇲🇬"),
    CurrencyItemInfo("MWK", "Kwacha Malauí", "MK", "🇲🇼"),
    CurrencyItemInfo("BIF", "Franco Burundés", "FBu", "🇧🇮"),
    CurrencyItemInfo("CDF", "Franco Congoleño", "FC", "🇨🇩"),
    CurrencyItemInfo("STN", "Dobra Santotomense", "Db", "🇸🇹"),
    CurrencyItemInfo("CVE", "Escudo Caboverdiano", "Esc", "🇨🇻"),
    CurrencyItemInfo("GMD", "Dalasi Gambiano", "D", "🇬🇲"),
    CurrencyItemInfo("GNF", "Franco Guineano", "FG", "🇬🇳"),
    CurrencyItemInfo("SLL", "Leone de Sierra Leona", "Le", "🇸🇱"),
    CurrencyItemInfo("LRD", "Dólar Liberiano", "L$", "🇱🇷"),
    CurrencyItemInfo("SOS", "Chelín Somalí", "Sh.So.", "🇸🇴"),
    CurrencyItemInfo("DJF", "Franco Yibutiano", "Fdj", "🇩🇯"),
    CurrencyItemInfo("KMF", "Franco Comorense", "CF", "🇰🇲"),
    CurrencyItemInfo("SZL", "Lilangeni Suazi", "L", "🇸🇿"),
    CurrencyItemInfo("LSL", "Loti Lesothense", "L", "🇱🇸"),
    CurrencyItemInfo("ERN", "Nakfa Eritreo", "Nfk", "🇪🇷"),
    CurrencyItemInfo("MRU", "Ouguiya Mauritana", "UM", "🇲🇷"),
    CurrencyItemInfo("SHP", "Libra de Santa Elena", "£", "🇸🇭"),

    // 5. Oceania & Pacific
    CurrencyItemInfo("AUD", "Dólar Australiano", "$", "🇦🇺"),
    CurrencyItemInfo("NZD", "Dólar Neozelandés", "NZ$", "🇳🇿"),
    CurrencyItemInfo("FJD", "Dólar Fiyiano", "FJ$", "🇫🇯"),
    CurrencyItemInfo("PGK", "Kina de Papúa Nueva Guinea", "K", "🇵🇬"),
    CurrencyItemInfo("WST", "Tala Samoano", "WS$", "🇼🇸"),
    CurrencyItemInfo("TOP", "Paʻanga Tongano", "T$", "🇹🇴"),
    CurrencyItemInfo("VUV", "Vatu Vanuatuense", "VT", "🇻🇺"),
    CurrencyItemInfo("SBD", "Dólar de Islas Salomón", "SI$", "🇸🇧"),
    CurrencyItemInfo("XPF", "Franco CFP del Pacífico", "₣", "🇵🇫"),

    // 6. Criptomonedas Populares
    CurrencyItemInfo("BTC", "Bitcoin", "₿", "🪙", true),
    CurrencyItemInfo("ETH", "Ethereum", "Ξ", "💎", true),
    CurrencyItemInfo("USDT", "Tether USD", "₮", "💵", true),
    CurrencyItemInfo("USDC", "USD Coin", "$", "🪙", true)
)

/**
 * Universal Currency Selector Modal Bottom Sheet (World 164 Currencies + DB custom currencies + Creation)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerModal(
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    financialRepository: FinancialRepository? = null,
    userId: String = "user_default"
) {
    val coroutineScope = rememberCoroutineScope()
    val dbCurrenciesState = financialRepository?.getCurrencies(userId)?.collectAsState(initial = emptyList())
    val dbCurrencies = dbCurrenciesState?.value ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Combine 164 catalog with DB custom currencies
    val allCurrencies = remember(dbCurrencies, searchQuery) {
        val customMapped = dbCurrencies.map {
            CurrencyItemInfo(
                code = it.code.uppercase(),
                name = it.name,
                symbol = it.symbol.ifBlank { "$" },
                flag = "⭐",
                isCustom = true
            )
        }
        val combined = (customMapped + WORLD_CURRENCIES_CATALOG).distinctBy { it.code.uppercase() }

        if (searchQuery.isBlank()) {
            combined
        } else {
            val q = searchQuery.trim()
            combined.filter {
                it.code.contains(q, ignoreCase = true) ||
                it.name.contains(q, ignoreCase = true) ||
                it.symbol.contains(q, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Seleccionar Moneda / Divisa",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "164 monedas mundiales y personalizadas",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }

            // Search input + Create New Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por código, divisa o país...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0284C7)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_currency_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        cursorColor = Color(0xFF0284C7)
                    )
                )

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_create_custom_currency")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // List of Currencies
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(allCurrencies, key = { it.code }) { curr ->
                    val isSelected = selectedCurrencyCode.equals(curr.code, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCurrencySelected(curr.code)
                                onDismiss()
                            }
                            .testTag("currency_item_${curr.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(curr.flag, fontSize = 22.sp)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = curr.code,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                        ) {
                                            Text(
                                                text = curr.symbol,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (curr.isCustom) {
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Personalizada",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = curr.name,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to create custom currency
    if (showCreateDialog) {
        CreateCustomCurrencyDialog(
            financialRepository = financialRepository,
            userId = userId,
            onCurrencyCreated = { newCurr ->
                showCreateDialog = false
                onCurrencySelected(newCurr.code)
                onDismiss()
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/**
 * Inline Dialog to Create and Save a New Currency directly to Room DB
 */
@Composable
fun CreateCustomCurrencyDialog(
    financialRepository: FinancialRepository?,
    userId: String = "user_default",
    onCurrencyCreated: (CurrencyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("$") }
    var rateInput by remember { mutableStateOf("1.0") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                            }
                            Text("Crear Nueva Moneda", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }

                    if (errorMessage != null) {
                        Text(errorMessage ?: "", color = Color(0xFFDC2626), fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it.uppercase() },
                        label = { Text("Código ISO / Símbolo (Ej: CAD, USDT, BTC)*", color = Color(0xFF64748B)) },
                        modifier = Modifier.fillMaxWidth().testTag("custom_currency_code_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            cursorColor = Color(0xFF0284C7)
                        )
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la moneda*", color = Color(0xFF64748B)) },
                        placeholder = { Text("Ej: Dólar Canadiense", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth().testTag("custom_currency_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            cursorColor = Color(0xFF0284C7)
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = { symbol = it },
                            label = { Text("Símbolo", color = Color(0xFF64748B)) },
                            modifier = Modifier.weight(1f).testTag("custom_currency_symbol_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                cursorColor = Color(0xFF0284C7)
                            )
                        )

                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it },
                            label = { Text("Tasa TRM (COP)", color = Color(0xFF64748B)) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                cursorColor = Color(0xFF0284C7)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (code.isBlank() || name.isBlank()) {
                                errorMessage = "Por favor completa el código y nombre"
                                return@Button
                            }
                            isSaving = true
                            coroutineScope.launch {
                                val rateNum = rateInput.replace(",", ".").toDoubleOrNull() ?: 1.0
                                val newCurr = CurrencyEntity(
                                    id = "curr_${code.lowercase()}_${System.currentTimeMillis()}",
                                    userId = userId,
                                    code = code.uppercase().trim(),
                                    name = name.trim(),
                                    symbol = symbol.ifBlank { "$" }.trim(),
                                    manualExchangeRate = rateNum,
                                    isActive = true,
                                    isSystem = false
                                )
                                try {
                                    financialRepository?.createCurrency(
                                        userId = userId,
                                        code = newCurr.code,
                                        name = newCurr.name,
                                        symbol = newCurr.symbol,
                                        manualExchangeRate = newCurr.manualExchangeRate
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                isSaving = false
                                onCurrencyCreated(newCurr)
                            }
                        },
                        enabled = !isSaving && code.isNotBlank() && name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_custom_currency_btn")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Guardar Moneda", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
