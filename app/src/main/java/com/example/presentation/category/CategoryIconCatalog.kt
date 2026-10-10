package com.example.presentation.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Enhanced Category Icon model supporting keyword search and grouping
 */
data class CategoryIconItem(
    val id: String,
    val name: String,
    val group: String,
    val keywords: String,
    val icon: ImageVector
)

val FullCategoryIconCatalog = listOf(
    // COMIDA Y BEBIDAS
    CategoryIconItem("restaurant", "Restaurante / Tenedores", "Comida", "comida restaurante tenedores platos almuerzo cena gourmet buffet", Icons.Default.Restaurant),
    CategoryIconItem("local_cafe", "Café / Barista", "Comida", "cafe cafeteria cafeina espresso capuchino bebidas desayuno merienda", Icons.Default.LocalCafe),
    CategoryIconItem("fastfood", "Comida Rápida", "Comida", "hamburguesa papas combo snacks fastfood perro caliente comida chatarra", Icons.Default.Fastfood),
    CategoryIconItem("local_pizza", "Pizza / Pizzería", "Comida", "pizza queso rebanada comida italiana delivery domicilios", Icons.Default.LocalPizza),
    CategoryIconItem("bakery_dining", "Panadería / Pastelería", "Comida", "pan pastel torta dulces repostería croissants panaderia", Icons.Default.BakeryDining),
    CategoryIconItem("local_bar", "Bar / Tragos / Cócteles", "Comida", "bar copas tragos cerveza cocteles discoteca rumba fiesta", Icons.Default.LocalBar),
    CategoryIconItem("lunch_dining", "Almuerzo / Sandwiches", "Comida", "almuerzo sandwich emparedado lonchera comida rapida", Icons.Default.LunchDining),
    CategoryIconItem("dinner_dining", "Cena / Platos Especiales", "Comida", "cena sopa plato fuerte comida casera restaurante", Icons.Default.DinnerDining),
    CategoryIconItem("icecream", "Helados / Postres", "Comida", "helado postre dulce paleta crepes antojos", Icons.Default.Icecream),
    CategoryIconItem("liquor", "Licores / Vinos", "Comida", "licor vino cerveza botella alcohol brindis", Icons.Default.Liquor),
    CategoryIconItem("ramen_dining", "Comida Asiática / Sopas", "Comida", "sushi ramen wok comida china japonesa sopa fideos", Icons.Default.RamenDining),
    CategoryIconItem("local_grocery_store", "Supermercado / Mercado", "Comida", "mercado viveres despensa supermercado compras provisiones", Icons.Default.LocalGroceryStore),

    // TRANSPORTE Y VEHÍCULOS
    CategoryIconItem("directions_car", "Automóvil / Carro", "Transporte", "carro auto vehiculo coche conduccion desplazamiento", Icons.Default.DirectionsCar),
    CategoryIconItem("local_gas_station", "Gasolina / Combustible", "Transporte", "gasolina combustible tanquear acpm gas estacion petroleo", Icons.Default.LocalGasStation),
    CategoryIconItem("local_taxi", "Taxi / Uber / Plataformas", "Transporte", "taxi uber cabify didi indriver transporte viajes", Icons.Default.LocalTaxi),
    CategoryIconItem("directions_bus", "Autobús / Transporte Público", "Transporte", "bus autobus transmilenio metro buseta colectivo transporte publico", Icons.Default.DirectionsBus),
    CategoryIconItem("two_wheeler", "Moto / Motocicleta", "Transporte", "moto motocicleta scooter dos ruedas delivery soat", Icons.Default.TwoWheeler),
    CategoryIconItem("pedal_bike", "Bicicleta / Ciclorruta", "Transporte", "bicicleta bici cicla ciclorruta pedales deporte transporte ecologico", Icons.Default.PedalBike),
    CategoryIconItem("flight", "Vuelos / Avión", "Transporte", "vuelo avion aeropuerto aerolinea tiquetes pasajes viajar", Icons.Default.Flight),
    CategoryIconItem("train", "Tren / Metro", "Transporte", "tren metro tranvia estacion riel ferrocarril", Icons.Default.Train),
    CategoryIconItem("local_parking", "Parqueadero / Estacionamiento", "Transporte", "parqueadero parqueo estacionamiento garaje celaduria", Icons.Default.LocalParking),
    CategoryIconItem("car_repair", "Taller / Mecánico / Repuestos", "Transporte", "mecanico taller repuestos llantas revision tecnico bateria", Icons.Default.CarRepair),
    CategoryIconItem("toll", "Peajes", "Transporte", "peaje carretera autopista viaje via peajes", Icons.Default.Toll),
    CategoryIconItem("directions_boat", "Barco / Ferry / Lancha", "Transporte", "barco lancha ferry transporte maritimo fluvial rio", Icons.Default.DirectionsBoat),

    // HOGAR Y SERVICIOS
    CategoryIconItem("home", "Vivienda / Casa", "Hogar", "casa hogar vivienda finca techo inmueble", Icons.Default.Home),
    CategoryIconItem("apartment", "Apartamento / Arriendo", "Hogar", "arriendo apartamento edificio alquiler administracion canon", Icons.Default.Apartment),
    CategoryIconItem("receipt_long", "Servicios Públicos / Facturas", "Hogar", "servicios facturas recibos recibo cobros mensualidad", Icons.Default.ReceiptLong),
    CategoryIconItem("bolt", "Luz / Electricidad", "Hogar", "luz energia electricidad enel epm codensa corriente", Icons.Default.Bolt),
    CategoryIconItem("water_drop", "Agua / Acueducto", "Hogar", "agua acueducto alcantarillado hidraulica gotas consumo", Icons.Default.WaterDrop),
    CategoryIconItem("local_fire_department", "Gas Natural", "Hogar", "gas gas domiciliario pipeta cocina calentador", Icons.Default.LocalFireDepartment),
    CategoryIconItem("wifi", "Internet / Wi-Fi", "Hogar", "internet wifi fibra claro movistar tigo conexion red", Icons.Default.Wifi),
    CategoryIconItem("tv", "Televisión / Streaming", "Hogar", "tv television netflix hbo disney streaming directv cable", Icons.Default.Tv),
    CategoryIconItem("phone_iphone", "Celular / Plan Móvil", "Hogar", "celular telefono recargas plan datos minutos tecnologia", Icons.Default.PhoneIphone),
    CategoryIconItem("cleaning_services", "Limpieza / Aseo", "Hogar", "limpieza aseo empleada domestica detergente lavanderia", Icons.Default.CleaningServices),
    CategoryIconItem("construction", "Mantenimiento / Reparaciones", "Hogar", "reparaciones obras remodelacion pintura tuberia plomeria", Icons.Default.Construction),
    CategoryIconItem("chair", "Muebles / Decoración", "Hogar", "muebles cama sofa decoracion hogar sala comedor", Icons.Default.Chair),

    // COMPRAS Y MODA
    CategoryIconItem("shopping_bag", "Compras / Tienda", "Compras", "compras ropa zapatos moda accesorios almacen compras", Icons.Default.ShoppingBag),
    CategoryIconItem("shopping_cart", "Carrito / Mercado", "Compras", "carrito compras mercado compras online tienda", Icons.Default.ShoppingCart),
    CategoryIconItem("store", "Comercio / Tienda Local", "Compras", "tienda negocio almacen local comercio ventas", Icons.Default.Store),
    CategoryIconItem("checkroom", "Ropa / Vestuario", "Compras", "ropa vestuario pantalones camisas chaquetas moda", Icons.Default.Checkroom),
    CategoryIconItem("diamond", "Joyería / Lujos", "Compras", "joyas anillo collar reloj diamantes lujo oro", Icons.Default.Diamond),
    CategoryIconItem("watch", "Relojes / Accesorios", "Compras", "reloj smartwatch pulsera accesorios gafas", Icons.Default.Watch),
    CategoryIconItem("loyalty", "Promociones / Descuentos", "Compras", "descuentos promociones cupones ofertas puntos", Icons.Default.Loyalty),
    CategoryIconItem("card_giftcard", "Regalos / Obsequios", "Compras", "regalo detalle cumpleaños tarjeta presente sorpresa", Icons.Default.CardGiftcard),
    CategoryIconItem("shopping_basket", "Canasta Familiar", "Compras", "canasta compras viveres alimentos mercado", Icons.Default.ShoppingBasket),
    CategoryIconItem("local_mall", "Centro Comercial", "Compras", "mall centro comercial almacenes shopping paseo", Icons.Default.LocalMall),

    // SALUD Y CUIDADO
    CategoryIconItem("favorite", "Salud / Bienestar", "Salud", "salud bienestar medicina vida cuerpo corazon", Icons.Default.Favorite),
    CategoryIconItem("local_hospital", "Hospital / Urgencias", "Salud", "hospital clinica urgencias eps cita medica salud", Icons.Default.LocalHospital),
    CategoryIconItem("medical_services", "Médico / Especialistas", "Salud", "medico doctor consulta especialista odontologo chequeo", Icons.Default.MedicalServices),
    CategoryIconItem("medication", "Farmacia / Medicamentos", "Salud", "farmacia drogueria pastillas jarabe remedios medicamentos formula", Icons.Default.Medication),
    CategoryIconItem("spa", "Spa / Belleza / Masajes", "Salud", "spa masajes estetica relajacion belleza cuidado", Icons.Default.Spa),
    CategoryIconItem("content_cut", "Peluquería / Barbería", "Salud", "peluqueria barberia corte de pelo uñas manicura estetica", Icons.Default.ContentCut),
    CategoryIconItem("fitness_center", "Gimnasio / Fitness", "Salud", "gimnasio gym pesas entrenamiento crossfit ejercicio", Icons.Default.FitnessCenter),
    CategoryIconItem("self_improvement", "Yoga / Meditación", "Salud", "yoga meditacion mente zen relajacion salud mental", Icons.Default.SelfImprovement),
    CategoryIconItem("sanitizer", "Cuidado Facial / Cosméticos", "Salud", "cremas cosmeticos skincare cuidado personal jabon", Icons.Default.Sanitizer),
    CategoryIconItem("bloodtype", "Laboratorio / Exámenes", "Salud", "laboratorio examenes sangre analisis clinico", Icons.Default.Bloodtype),

    // ENTRETENIMIENTO Y VIAJES
    CategoryIconItem("movie", "Cine / Películas", "Entretenimiento", "cine pelicula cartelera entradas palomitas teatro", Icons.Default.Movie),
    CategoryIconItem("sports_esports", "Videojuegos / Gaming", "Entretenimiento", "videojuegos juegos playstation xbox nintendo steam gamer", Icons.Default.SportsEsports),
    CategoryIconItem("music_note", "Música / Conciertos", "Entretenimiento", "musica spotify conciertos boletas recital instrumentos", Icons.Default.MusicNote),
    CategoryIconItem("theater_comedy", "Teatro / Eventos", "Entretenimiento", "teatro comedia stand up eventos shows espectaculo", Icons.Default.TheaterComedy),
    CategoryIconItem("sports_soccer", "Fútbol / Deportes", "Entretenimiento", "futbol cancha partido estadio deporte pelota", Icons.Default.SportsSoccer),
    CategoryIconItem("hotel", "Hotel / Hospedaje", "Entretenimiento", "hotel hospedaje airbnb cabaña habitacion vacaciones", Icons.Default.Hotel),
    CategoryIconItem("beach_access", "Vacaciones / Playa", "Entretenimiento", "playa sol vacaciones piscina viaje descanso paseo", Icons.Default.BeachAccess),
    CategoryIconItem("attractions", "Parques de Diversión", "Entretenimiento", "parque atracciones juegos feria entretenimiento diversion", Icons.Default.Attractions),
    CategoryIconItem("casino", "Juegos de Azar / Apuestas", "Entretenimiento", "apuestas casino loteria cartas juegos ruleta", Icons.Default.Casino),
    CategoryIconItem("photo_camera", "Fotografía / Cámaras", "Entretenimiento", "fotos camara sesion recuerdos fotografia lentes", Icons.Default.PhotoCamera),
    CategoryIconItem("palette", "Arte / Creatividad", "Entretenimiento", "arte pintura museo manualidades diseno hobbies", Icons.Default.Palette),
    CategoryIconItem("celebration", "Fiestas / Eventos", "Entretenimiento", "fiesta celebracion rumba cumpleaños evento agasajo", Icons.Default.Celebration),

    // TRABAJO Y EDUCACIÓN
    CategoryIconItem("school", "Educación / Universidad", "Educación", "educacion universidad colegio matricula pension semestre", Icons.Default.School),
    CategoryIconItem("menu_book", "Libros / Biblioteca", "Educación", "libros lectura editorial libreria textos guias", Icons.AutoMirrored.Filled.MenuBook),
    CategoryIconItem("work", "Trabajo / Nómina", "Trabajo", "trabajo nomina sueldo salario empleo empresa oficina", Icons.Default.Work),
    CategoryIconItem("business_center", "Oficina / Negocios", "Trabajo", "negocios maletin corporativo oficina consultoria", Icons.Default.BusinessCenter),
    CategoryIconItem("laptop", "Tecnología / Computadores", "Trabajo", "computador laptop pc software licencias herramientas tech", Icons.Default.Laptop),
    CategoryIconItem("auto_stories", "Cursos / Capacitaciones", "Educación", "cursos talleres diplomado certificacion udemy platzi", Icons.Default.AutoStories),
    CategoryIconItem("science", "Ciencia / Investigación", "Educación", "investigacion proyectos laboratorio tecnologia ciencia", Icons.Default.Science),
    CategoryIconItem("workspace_premium", "Premios / Certificaciones", "Educación", "certificaciones diplomas logros distinciones", Icons.Default.WorkspacePremium),
    CategoryIconItem("domain", "Empresa / Corporativo", "Trabajo", "empresa organizacion corporativo sede oficinas", Icons.Default.Domain),
    CategoryIconItem("badge", "Carnet / Afiliaciones", "Trabajo", "carnet identificacion membresia suscripcion club", Icons.Default.Badge),
    CategoryIconItem("handyman", "Servicios Técnicos", "Trabajo", "servicios soporte tecnico mantenimiento arreglos", Icons.Default.Handyman),
    CategoryIconItem("design_services", "Diseño / Freelance", "Trabajo", "freelance diseño arquitectura freelance proyectos", Icons.Default.DesignServices),

    // FINANZAS E INGRESOS
    CategoryIconItem("payments", "Efectivo / Dinero", "Finanzas", "dinero billetes efectivo cash liquidez pagos", Icons.Default.Payments),
    CategoryIconItem("account_balance", "Banco / Entidad", "Finanzas", "banco sucursal entidad financiera intereses comisiones", Icons.Default.AccountBalance),
    CategoryIconItem("account_balance_wallet", "Billetera / Salario", "Finanzas", "billetera saldo transferencias ingresos sueldo", Icons.Default.AccountBalanceWallet),
    CategoryIconItem("credit_card", "Tarjetas / Crédito", "Finanzas", "tarjeta tarjeta de credito cuotas datáfono franquicia", Icons.Default.CreditCard),
    CategoryIconItem("trending_up", "Inversiones / Rendimientos", "Finanzas", "inversiones cdt acciones bolsa dividendos ganancias crypto", Icons.Default.TrendingUp),
    CategoryIconItem("savings", "Ahorros / Hucha", "Finanzas", "ahorro alcancia reservas fondo de emergencia hucha", Icons.Default.Savings),
    CategoryIconItem("monetization_on", "Monedas / Ingresos Extra", "Finanzas", "monedas bonos comisiones ingresos adicionales", Icons.Default.MonetizationOn),
    CategoryIconItem("currency_exchange", "Divisas / Cambio", "Finanzas", "dolar euro divisa trm cambio moneda", Icons.Default.CurrencyExchange),
    CategoryIconItem("sell", "Ventas / Facturación", "Finanzas", "ventas facturacion cobros ingresos de ventas cliente", Icons.Default.Sell),
    CategoryIconItem("account_tree", "Rentas / Dividendos", "Finanzas", "dividendos rentas regalias capital pasivo", Icons.Default.AccountTree),
    CategoryIconItem("request_quote", "Honorarios / Cotizaciones", "Finanzas", "honorarios cuenta de cobro cotizacion servicios", Icons.Default.RequestQuote),
    CategoryIconItem("pie_chart", "Finanzas / Portafolio", "Finanzas", "estadisticas portafolio balance gestion patrimonial", Icons.Default.PieChart),

    // FAMILIA Y MASCOTAS
    CategoryIconItem("pets", "Mascotas / Veterinario", "Familia", "mascotas perro gato veterinario purina concentrado animales", Icons.Default.Pets),
    CategoryIconItem("child_care", "Bebés / Guardería", "Familia", "bebes niños guarderia pañales juguetes pediatra", Icons.Default.ChildCare),
    CategoryIconItem("family_restroom", "Familia / Hogar", "Familia", "familia hijos padres salidas apoyo familiar", Icons.Default.FamilyRestroom),
    CategoryIconItem("toys", "Juguetes / Niños", "Familia", "juguetes regalos infantiles diversion juegos niños", Icons.Default.Toys),
    CategoryIconItem("elderly", "Adultos Mayores / Abuelos", "Familia", "abuelos adultos mayores geriatrico cuidados pension", Icons.Default.Elderly),
    CategoryIconItem("volunteer_activism", "Donaciones / Caridad", "Familia", "donacion caridad ayuda fundacion voluntariado diezmo", Icons.Default.VolunteerActivism),
    CategoryIconItem("cake", "Cumpleaños / Fiestas", "Familia", "cumpleaños pastel aniversario celebracion fiesta", Icons.Default.Cake),
    CategoryIconItem("card_travel", "Viajes Familiares", "Familia", "maletas viaje turismo paseo familiar", Icons.Default.CardTravel),

    // OTROS Y GENERAL
    CategoryIconItem("category", "General / Categoría", "General", "general varios otros conceptos categoria miscelanea", Icons.Default.Category),
    CategoryIconItem("star", "Favoritos / Especial", "General", "favorito estrella destacado especial prioridad", Icons.Default.Star),
    CategoryIconItem("lock", "Seguros / Pólizas", "General", "seguros poliza proteccion todo riesgo vida seguros", Icons.Default.Lock),
    CategoryIconItem("shield", "Seguridad / Protección", "General", "seguridad proteccion blindaje alarma vigilancia", Icons.Default.Shield),
    CategoryIconItem("gavel", "Legal / Impuestos / Trámites", "General", "notaria abogado dian impuestos trámites legal demandas", Icons.Default.Gavel),
    CategoryIconItem("mail", "Envíos / Mensajería", "General", "envios servientrega encomiendas mensajeria correspondencia", Icons.Default.Mail),
    CategoryIconItem("help", "Imprevistos / Emergencias", "General", "imprevistos dudas emergencias gastos imprevistos", Icons.Default.Help),
    CategoryIconItem("flight_takeoff", "Traslados / Mudanzas", "General", "mudanza salida despegue trasteo viaje", Icons.Default.FlightTakeoff)
)

val CategoryColorPalette = listOf(
    "#EF4444", // Rojo Coral
    "#F97316", // Naranja
    "#F59E0B", // Ámbar
    "#10B981", // Esmeralda
    "#14B8A6", // Teal
    "#06B6D4", // Cian
    "#0284C7", // Azul Cielo
    "#3B82F6", // Azul Eléctrico
    "#6366F1", // Índigo
    "#8B5CF6", // Púrpura
    "#EC4899", // Rosa
    "#64748B"  // Pizarra
)

val CategoryFilterGroups = listOf(
    "Todos",
    "Comida",
    "Transporte",
    "Hogar",
    "Compras",
    "Salud",
    "Entretenimiento",
    "Educación",
    "Trabajo",
    "Finanzas",
    "Familia",
    "General"
)

/**
 * Universal mapper from string icon key or category name to ImageVector
 */
fun resolveCategoryIcon(iconKey: String?, categoryName: String?): ImageVector {
    val cleanKey = iconKey?.trim().orEmpty()
    val cleanName = categoryName?.trim().orEmpty().lowercase()

    // 1. Direct match with catalog ID
    if (cleanKey.isNotBlank()) {
        val direct = FullCategoryIconCatalog.find { it.id.equals(cleanKey, ignoreCase = true) }
        if (direct != null) return direct.icon
    }

    // 2. Keyword match with catalog
    if (cleanName.isNotBlank()) {
        val keywordMatch = FullCategoryIconCatalog.find { item ->
            item.keywords.split(" ").any { kw -> kw.length > 3 && cleanName.contains(kw) }
        }
        if (keywordMatch != null) return keywordMatch.icon
    }

    // 3. Fallback heuristics
    return when {
        cleanName.contains("restauran") || cleanName.contains("alimen") || cleanName.contains("comida") || cleanName.contains("tenedor") -> Icons.Default.Restaurant
        cleanName.contains("cafe") || cleanName.contains("café") -> Icons.Default.LocalCafe
        cleanName.contains("transp") || cleanName.contains("carro") || cleanName.contains("gasolina") -> Icons.Default.DirectionsCar
        cleanName.contains("arriendo") || cleanName.contains("hogar") || cleanName.contains("vivienda") || cleanName.contains("casa") -> Icons.Default.Home
        cleanName.contains("luz") || cleanName.contains("electric") -> Icons.Default.Bolt
        cleanName.contains("agua") -> Icons.Default.WaterDrop
        cleanName.contains("serv") || cleanName.contains("factura") -> Icons.Default.ReceiptLong
        cleanName.contains("salud") || cleanName.contains("med") || cleanName.contains("farmacia") -> Icons.Default.Favorite
        cleanName.contains("gym") || cleanName.contains("gimnasio") || cleanName.contains("deporte") -> Icons.Default.FitnessCenter
        cleanName.contains("ropa") || cleanName.contains("compra") || cleanName.contains("mercado") -> Icons.Default.ShoppingBag
        cleanName.contains("sueldo") || cleanName.contains("salario") || cleanName.contains("nomina") || cleanName.contains("nómina") -> Icons.Default.Work
        cleanName.contains("inversi") || cleanName.contains("rendimien") || cleanName.contains("dividend") -> Icons.Default.TrendingUp
        cleanName.contains("viaje") || cleanName.contains("vuelo") || cleanName.contains("hotel") -> Icons.Default.Flight
        cleanName.contains("cine") || cleanName.contains("entreten") || cleanName.contains("juego") -> Icons.Default.Movie
        cleanName.contains("mascota") || cleanName.contains("perro") || cleanName.contains("gato") -> Icons.Default.Pets
        cleanName.contains("educa") || cleanName.contains("curso") || cleanName.contains("colegio") -> Icons.Default.School
        cleanName.contains("regalo") || cleanName.contains("detalle") -> Icons.Default.CardGiftcard
        else -> Icons.Default.Category
    }
}
