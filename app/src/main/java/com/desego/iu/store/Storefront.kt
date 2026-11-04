package com.desego.iu.store

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.R
import com.desego.data.DemoUser
import com.desego.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

internal val Navy = NavyPrimary
internal val Ocean = OceanBlue
internal val Sand = BackgroundSand
internal val Orange = AmberRating
internal val Muted = TextMuted
internal val Mint = EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Storefront(
    user: DemoUser?,
    onLogout: () -> Unit,
    initialTableToken: String? = null,
    onRequireLogin: () -> Unit = {},
    initialAccount: Boolean = false
) {
    val context = LocalContext.current
    val state = remember(user?.email) { StoreState(context, user) }
    var tab by rememberSaveable(user?.email) { mutableIntStateOf(if (initialAccount) 4 else 0) }
    var page by rememberSaveable { mutableStateOf(if (!initialTableToken.isNullOrBlank()) "table" else "main") }
    var productId by rememberSaveable { mutableIntStateOf(1) }
    var orderId by rememberSaveable { mutableStateOf("") }
    var delivery by rememberSaveable { mutableStateOf(true) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var catalogCategory by rememberSaveable { mutableStateOf("Todo") }
    val screenState = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        while (true) {
            state.refresh()
            delay(30_000)
        }
    }

    DisposableEffect(state) {
        val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                scope.launch { state.refresh() }
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose { connectivity.unregisterNetworkCallback(callback); state.close() }
    }

    LaunchedEffect(page, tab) {
        if (page == "order" || (page == "main" && tab == 3)) {
            while (true) {
                delay(15_000)
                state.refresh()
            }
        }
    }

    val message: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) }; Unit }
    val back: () -> Unit = {
        page = when (page) {
            "checkout" -> "cart"
            "addressCheckout" -> "checkout"
            else -> "main"
        }
    }
    BackHandler(page != "main") { back() }

    val navTitles = listOf("Inicio", "Carta", "Favoritos", "Pedidos", "Cuenta")
    val navIcons = listOf(
        Icons.Default.Home,
        Icons.Default.RestaurantMenu,
        Icons.Default.FavoriteBorder,
        Icons.Default.ReceiptLong,
        Icons.Default.PersonOutline
    )

    DesegoAppTheme {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = BackgroundSand,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    title = {
                        Column(
                            Modifier.then(
                                if (page == "main") Modifier.clickable {
                                    if (user == null) onRequireLogin() else page = "addresses"
                                } else Modifier
                            )
                        ) {
                            if (page == "main") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(7.dp).clip(CircleShape).background(EmeraldSuccess)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "DESEGO · ${if (delivery) "DELIVERY" else "RECOJO"}",
                                        fontSize = 10.sp,
                                        color = OceanBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            Text(
                                if (page == "main") {
                                    if (delivery) {
                                        val addr = state.addresses.firstOrNull { it.isDefault } ?: state.addresses.firstOrNull()
                                        addr?.street ?: "Local Huaycán · Lima Este"
                                    } else "DeSego · Local Principal"
                                } else when (page) {
                                    "product" -> "Detalle del plato"
                                    "cart" -> "Tu carrito"
                                    "checkout" -> "Finalizar pedido"
                                    "order" -> "Seguimiento en vivo"
                                    "addresses", "addressCheckout" -> "Direcciones de entrega"
                                    "reservations" -> "Reserva tu mesa"
                                    "profile" -> "Mi perfil"
                                    "notifications" -> "Novedades"
                                    "table" -> "Mi mesa (QR)"
                                    else -> "Ayuda y soporte"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = if (page == "main") 15.sp else 18.sp,
                                color = NavyPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        if (page != "main") {
                            IconButton(onClick = back) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NavyPrimary)
                            }
                        }
                    },
                    actions = {
                        if (page == "main") {
                            IconButton(onClick = { page = "table" }) {
                                Icon(Icons.Default.QrCodeScanner, "Mi mesa", tint = NavyPrimary)
                            }
                            IconButton(onClick = { page = "notifications" }) {
                                Icon(Icons.Default.NotificationsNone, "Notificaciones", tint = NavyPrimary)
                            }
                        }
                        if (page !in listOf("cart", "checkout")) {
                            IconButton(onClick = { page = "cart" }) {
                                BadgedBox(badge = {
                                    if (state.count > 0) {
                                        Badge(
                                            containerColor = OceanBlue,
                                            contentColor = Color.White
                                        ) {
                                            Text(state.count.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ShoppingBag, "Carrito", tint = NavyPrimary)
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (page == "main") {
                    Column {
                        // Floating Modern Cart Bar estilo Rappi / Uber Eats
                        AnimatedVisibility(
                            visible = state.count > 0,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Surface(
                                    onClick = { page = "cart" },
                                    shape = RoundedCornerShape(20.dp),
                                    color = OceanBlue,
                                    shadowElevation = 6.dp
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                "${state.count}",
                                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "Ver carrito",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            money(calculateTotals(state.cart, false, state.coupon).let { it.subtotal - it.discount }),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // Bottom Navigation Bar Premium con fondo blanco que cubre y respeta la barra del sistema
                        Surface(
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                                NavigationBar(
                                    containerColor = Color.White,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                ) {
                                    navTitles.forEachIndexed { index, title ->
                                        val isSelected = tab == index
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { tab = index },
                                            icon = {
                                                Icon(
                                                    navIcons[index],
                                                    contentDescription = title,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = OceanBlue,
                                                selectedTextColor = OceanBlue,
                                                indicatorColor = OceanSoftBg,
                                                unselectedIconColor = TextMuted,
                                                unselectedTextColor = TextMuted
                                            ),
                                            label = {
                                                Text(
                                                    title,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val openProduct: (StoreProduct) -> Unit = { productId = it.id; page = "product" }
                val add: (StoreProduct) -> Unit = { state.add(it.id); message("${it.name} añadido al carrito") }
                val screenKey = when (page) {
                    "main" -> "main:$tab"
                    "product" -> "product:$productId"
                    "order" -> "order:$orderId"
                    else -> page
                }

                screenState.SaveableStateProvider(screenKey) {
                    when (page) {
                        "main" -> when (tab) {
                            0 -> StoreHome(
                                state = state,
                                delivery = delivery,
                                mode = { delivery = it },
                                query = catalogQuery,
                                setQuery = { catalogQuery = it },
                                category = { category -> catalogCategory = category; tab = 1 },
                                open = openProduct,
                                add = add,
                                reserve = { page = "reservations" }
                            )
                            1 -> StoreCatalog(
                                state = state,
                                favoritesOnly = false,
                                query = catalogQuery,
                                setQuery = { catalogQuery = it },
                                category = catalogCategory,
                                setCategory = { catalogCategory = it },
                                open = openProduct,
                                add = add
                            )
                            2 -> StoreCatalog(
                                state = state,
                                favoritesOnly = true,
                                query = "",
                                setQuery = {},
                                category = "Todo",
                                setCategory = {},
                                open = openProduct,
                                add = add
                            )
                            3 -> if (user == null || state.sessionExpired) GuestAccount(onRequireLogin)
                                 else StoreOrders(state, { orderId = it.id; page = "order" }, { state.reorder(it); page = "cart" })
                            else -> if (user == null || state.sessionExpired) GuestAccount(onRequireLogin)
                                 else StoreAccountResponsive(
                                     user = user,
                                     state = state,
                                     onOpenProduct = openProduct,
                                     onAddToCart = add,
                                     onViewOrder = { orderId = it.id; page = "order" },
                                     onLogout = onLogout,
                                     message = message
                                 )
                        }
                        "product" -> ProductDetail(
                            product = demoProducts.firstOrNull { it.id == productId }
                                ?: StoreProduct(productId, "Plato no disponible", "Carta", 0, R.drawable.food_ceviche, "Este plato ya no está disponible.", "", available = false),
                            state = state,
                            onBack = { page = "main" }
                        ) { quantity ->
                            state.add(productId, quantity)
                            page = "cart"
                        }
                        "cart" -> StoreCart(
                            state = state,
                            delivery = delivery,
                            mode = { delivery = it },
                            checkout = {
                                if (user == null || state.sessionExpired) onRequireLogin()
                                else page = "checkout"
                            },
                            menu = { page = "main"; tab = 1 }
                        )
                        "checkout" -> StoreCheckout(
                            state = state,
                            delivery = delivery,
                            mode = { delivery = it },
                            addresses = { page = "addressCheckout" }
                        ) { address, payment, note ->
                            scope.launch {
                                runCatching { state.order(delivery, address, payment, note) }
                                    .onSuccess { order ->
                                        orderId = order.id
                                        page = "order"
                                        tab = 3
                                        message("¡Tu pedido fue recibido con éxito!")
                                    }
                                    .onFailure { message(it.message ?: "No se pudo confirmar el pedido.") }
                            }
                        }
                        "order" -> state.orders.firstOrNull { it.id == orderId }?.let { order ->
                            OrderDetail(
                                order = order,
                                cancel = {
                                    scope.launch {
                                        runCatching { state.cancel(order.id) }
                                            .onFailure { message(it.message ?: "No se pudo cancelar el pedido.") }
                                    }
                                },
                                reorder = { state.reorder(order); page = "cart" }
                            )
                        }
                        "addresses", "addressCheckout" -> AddressBook(state, message)
                        "profile" -> EditProfile(state) { name, phone ->
                            scope.launch {
                                runCatching { state.saveProfile(name, phone) }
                                    .onSuccess {
                                        page = "main"
                                        message(if (state.pendingChanges > 0) "Perfil guardado en el dispositivo." else "Perfil actualizado.")
                                    }
                                    .onFailure { message(it.message ?: "No se pudo guardar el perfil.") }
                            }
                        }
                        "reservations" -> if (user == null || state.sessionExpired) GuestAccount(onRequireLogin)
                                         else Reservations(state, user.email, scope, message)
                        "notifications" -> Notifications(state) { orderId = it.id; page = "order" }
                        "table" -> DiningTable(user?.email.orEmpty(), message, initialTableToken)
                        "help" -> HelpScreen(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestAccount(login: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.logo), "Desego Cevichería", Modifier.size(130.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Tu mesa, siempre contigo.",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = NavyPrimary
        )
        Text(
            "Inicia sesión con tu cuenta de DeSego para consultar tus pedidos, acumular puntos y reservar tu mesa.",
            Modifier.padding(vertical = 14.dp),
            color = TextMuted,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 13.sp
        )
        Button(
            onClick = login,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
        ) {
            Text("Iniciar sesión o Registrarme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(
            "Puedes explorar la carta libremente sin iniciar sesión.",
            Modifier.padding(top = 14.dp),
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun ProductDetail(
    product: StoreProduct,
    state: StoreState,
    onBack: () -> Unit,
    add: (Int) -> Unit
) {
    var quantity by rememberSaveable(product.id) { mutableIntStateOf(1) }
    val isFav = product.id in state.favorites

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(290.dp)) {
                FoodImage(
                    product,
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                )
                // Back button overlay
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NavyPrimary)
                }
                // Favorite button overlay
                IconButton(
                    onClick = { state.favorite(product.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorito",
                        tint = if (isFav) CoralFavorite else TextMuted
                    )
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    color = OceanSoftBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        product.category.uppercase(),
                        color = OceanBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    product.name,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Text(
                    money(product.price),
                    fontSize = 22.sp,
                    color = OceanBlue,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    product.description,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = BorderSubtle)

                InfoCard(
                    "Frescura garantizada",
                    "Pesca artesanal del día y limón piurano fresco recién exprimido.",
                    Icons.Default.Restaurant
                )

                if (product.allergens.isNotBlank()) {
                    InfoCard(
                        "Información y alérgenos",
                        "${product.allergens}. Confirma con cocina cualquier condición de riesgo.",
                        Icons.Default.Info
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cantidad", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                    Quantity(
                        quantity,
                        minus = { if (quantity > 1) quantity-- },
                        plus = { if (quantity < 20) quantity++ }
                    )
                }
            }
        }

        // Bottom Add Button
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Button(
                onClick = { add(quantity) },
                enabled = product.available,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                Text(
                    "Añadir al carrito · ${money(product.price * quantity)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun StoreCart(
    state: StoreState,
    delivery: Boolean,
    mode: (Boolean) -> Unit,
    checkout: () -> Unit,
    menu: () -> Unit
) {
    if (state.cart.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyState(
                Icons.Default.ShoppingBag,
                "Tu carrito espera un antojo",
                "Descubre nuestra carta y déjate conquistar por la frescura del mar."
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = menu,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                Text("Explorar la carta", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    var couponInput by rememberSaveable(state.coupon) { mutableStateOf(state.coupon) }
    var couponError by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DeliveryChoice(delivery, mode)
            }

            items(demoProducts.filter { state.quantity(it.id) > 0 }, key = { it.id }) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FoodImage(product, Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                            Text(money(product.price * state.quantity(product.id)), color = OceanBlue, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Quantity(
                                state.quantity(product.id),
                                minus = { state.setQuantity(product.id, state.quantity(product.id) - 1) },
                                plus = { state.add(product.id) }
                            )
                        }
                        IconButton(onClick = { state.setQuantity(product.id, 0) }) {
                            Icon(Icons.Default.DeleteOutline, "Eliminar", tint = TextMuted)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cupón de descuento", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it; couponError = false },
                                placeholder = { Text("Ej. DESEGO10", fontSize = 13.sp) },
                                singleLine = true,
                                isError = couponError,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    couponError = false
                                    state.applyCoupon(couponInput)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                            ) {
                                Text("Aplicar")
                            }
                        }
                        if (state.coupon.isNotBlank()) {
                            Text("Cupón aplicado: ${state.coupon}", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen del pedido", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                        Totals(calculateTotals(state.cart, delivery, state.coupon))
                    }
                }
            }
        }

        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Button(
                onClick = checkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                Text(
                    "Continuar · ${money(calculateTotals(state.cart, delivery, state.coupon).total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun StoreCheckout(
    state: StoreState,
    delivery: Boolean,
    mode: (Boolean) -> Unit,
    addresses: () -> Unit,
    confirm: (DeliveryAddress?, String, String) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf(state.addresses.firstOrNull { it.isDefault }?.id ?: state.addresses.firstOrNull()?.id) }
    val selected = state.addresses.firstOrNull { it.id == selectedId } ?: state.addresses.firstOrNull()
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    var payment by rememberSaveable { mutableStateOf("Contraentrega") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DeliveryChoice(delivery, mode)

            Text(
                if (delivery) "¿A dónde lo llevamos?" else "Te esperamos en DeSego",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = NavyPrimary
            )

            if (delivery) {
                state.addresses.forEach { address ->
                    val isChosen = selected?.id == address.id
                    Surface(
                        onClick = { selectedId = address.id },
                        color = if (isChosen) OceanSoftBg else Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isChosen) OceanBlue else BorderLight)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isChosen,
                                onClick = { selectedId = address.id },
                                colors = RadioButtonDefaults.colors(selectedColor = OceanBlue)
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(address.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                                Text("${address.street}, ${address.district}", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = addresses,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nueva dirección")
                }
            } else {
                InfoCard(
                    "Recojo en el local principal",
                    "Te esperamos en DeSego Huaycán. Te avisaremos apenas esté listo.",
                    Icons.Default.Storefront
                )
            }

            OutlinedTextField(
                phone,
                { phone = it.filter(Char::isDigit).take(9) },
                label = { Text("Teléfono de contacto (9 dígitos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Forma de pago", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrimary)

            listOf("Contraentrega", "Tarjeta", "Yape / Plin").forEach { option ->
                val isSelected = payment == option
                Surface(
                    onClick = { payment = option },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) OceanSoftBg else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) OceanBlue else BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { payment = option },
                            colors = RadioButtonDefaults.colors(selectedColor = OceanBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(option, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                            Text(
                                when (option) {
                                    "Contraentrega" -> "Efectivo en mano al recibir el pedido"
                                    "Tarjeta" -> "POS inalámbrico al recibir"
                                    else -> "Yape o Plin al repartidor"
                                },
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                note,
                { note = it.take(300) },
                label = { Text("Indicaciones para cocina o reparto (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Totals(calculateTotals(state.cart, delivery, state.coupon))
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Button(
                onClick = {
                    error = when {
                        state.cart.isEmpty() -> "Añade productos antes de continuar."
                        delivery && selected == null -> "Selecciona o añade una dirección para delivery."
                        phone.length != 9 -> "Escribe un número de celular válido de 9 dígitos."
                        else -> null
                    }
                    if (error == null) {
                        state.profile(state.name, phone)
                        confirm(if (delivery) selected else null, payment, note)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                enabled = state.cart.isNotEmpty() && !state.busy
            ) {
                Text("Confirmar pedido", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun StoreOrders(state: StoreState, open: (DemoOrder) -> Unit, reorder: (DemoOrder) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Mis pedidos", fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = NavyPrimary)
            Text("Sigue el estado en tiempo real de tus pedidos en cocina.", fontSize = 12.sp, color = TextMuted)
        }

        if (state.orders.isEmpty()) {
            item {
                EmptyState(
                    Icons.Default.ReceiptLong,
                    "Tu primer pedido te espera",
                    "Explora la carta y arma tu pedido con la pesca más fresca."
                )
            }
        }

        items(state.orders, key = { it.id }) { order ->
            Card(
                onClick = { open(order) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${order.id}", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (order.status) {
                                "Entregado" -> Color(0xFFDCFCE7)
                                "Cancelado" -> Color(0xFFFEE2E2)
                                else -> Color(0xFFDBEAFE)
                            }
                        ) {
                            Text(
                                order.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (order.status) {
                                    "Entregado" -> Color(0xFF166534)
                                    "Cancelado" -> Color(0xFF991B1B)
                                    else -> Color(0xFF1E40AF)
                                }
                            )
                        }
                    }
                    Text(order.date, fontSize = 11.sp, color = TextMuted)
                    Text(
                        order.items.entries.joinToString { (id, qty) ->
                            "$qty × ${order.itemNames[id] ?: demoProducts.firstOrNull { it.id == id }?.name ?: "Plato"}"
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                    HorizontalDivider(color = BorderSubtle)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(money(order.total), Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = OceanBlue)
                        TextButton(onClick = { reorder(order) }) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Volver a pedir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetail(order: DemoOrder, cancel: () -> Unit, reorder: () -> Unit) {
    var confirmCancel by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (order.status == "Cancelado") Icons.Default.Cancel else Icons.Default.CheckCircle,
                null,
                tint = if (order.status == "Cancelado") Color(0xFFDC2626) else OceanBlue,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (order.status == "Cancelado") "Pedido cancelado" else "¡Pedido en proceso!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = NavyPrimary
                )
                Text("#${order.id} · ${order.date}", fontSize = 12.sp, color = TextMuted)
            }
        }

        // Stepper de Seguimiento en Vivo
        if (order.status != "Cancelado") {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Seguimiento del pedido", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                    val steps = if (order.mode == "Delivery") listOf("Pendiente", "Preparando", "Listo", "En reparto", "Entregado")
                    else listOf("Pendiente", "Preparando", "Listo", "Entregado")
                    val currentIdx = steps.indexOf(order.status).coerceAtLeast(0)

                    steps.forEachIndexed { index, title ->
                        val isDone = index <= currentIdx
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null,
                                tint = if (isDone) OceanBlue else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                title,
                                Modifier.padding(start = 12.dp),
                                fontWeight = if (index == currentIdx) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone) NavyPrimary else TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text("Se actualiza en vivo desde cocina cada 15 segundos.", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        InfoCard(order.mode, order.address, Icons.Default.LocationOn)

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Detalle de platos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                order.items.forEach { (id, qty) ->
                    val unitPrice = order.itemPrices[id] ?: demoProducts.firstOrNull { it.id == id }?.price ?: 0
                    SummaryLine(
                        "$qty × ${order.itemNames[id] ?: demoProducts.firstOrNull { it.id == id }?.name ?: "Plato"}",
                        money(unitPrice * qty)
                    )
                }
                HorizontalDivider(color = BorderSubtle)
                SummaryLine("Total del pedido", money(order.total), bold = true)
            }
        }

        if (order.note.isNotBlank()) {
            Text("Indicaciones: ${order.note}", fontSize = 12.sp, color = TextMuted)
        }

        OutlinedButton(
            onClick = reorder,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Volver a pedir estos platos", fontWeight = FontWeight.Bold)
        }

        if (order.status == "Pendiente") {
            TextButton(
                onClick = { confirmCancel = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar este pedido", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("¿Cancelar este pedido?") },
            text = { Text("Se cancelará el pedido pendiente en cocina.") },
            confirmButton = {
                TextButton(onClick = { cancel(); confirmCancel = false }) {
                    Text("Sí, cancelar", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text("Volver") }
            }
        )
    }
}

@Composable
private fun AddressBook(state: StoreState, message: (String) -> Unit) {
    val addressScope = rememberCoroutineScope()
    var adding by rememberSaveable { mutableStateOf(state.addresses.isEmpty()) }
    var label by rememberSaveable { mutableStateOf("Casa") }
    var street by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("Ate / Huaycán") }
    var reference by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(state.addresses, key = { it.id }) { address ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = OceanBlue)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(address.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${address.street}, ${address.district}", fontSize = 12.sp, color = NavyPrimary)
                        if (address.reference.isNotBlank()) Text(address.reference, color = TextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = {
                        addressScope.launch {
                            runCatching { state.removeAddress(address.id) }
                                .onFailure { message(it.message ?: "No se pudo eliminar.") }
                        }
                    }) {
                        Icon(Icons.Default.DeleteOutline, "Eliminar", tint = TextMuted)
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { adding = !adding },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (adding) Icons.Default.Close else Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(if (adding) "Cancelar" else "Nueva dirección")
            }
        }
        if (adding) item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Añadir dirección", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(label, { label = it.take(30) }, label = { Text("Nombre (ej. Casa, Trabajo)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(street, { street = it.take(150) }, label = { Text("Calle, número o lote") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(district, { district = it.take(60) }, label = { Text("Distrito") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(reference, { reference = it.take(150) }, label = { Text("Referencia (opcional)") }, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            if (label.isBlank() || street.trim().length < 5 || district.isBlank()) {
                                error = "Completa los campos principales."
                            } else {
                                addressScope.launch {
                                    runCatching { state.addAddress(label, street, district, reference) }
                                        .onSuccess {
                                            adding = false; street = ""; reference = ""; error = null
                                            message("Dirección guardada.")
                                        }
                                        .onFailure { error = it.message }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                    ) {
                        Text("Guardar dirección")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfile(state: StoreState, save: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(state.name) }
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.AccountCircle, null, tint = OceanBlue, modifier = Modifier.size(72.dp))
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(9) }, label = { Text("Celular (9 dígitos)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = { save(name, phone) },
            enabled = name.trim().length >= 2 && (phone.isBlank() || phone.length == 9),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
        ) {
            Text("Guardar cambios", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Reservations(
    state: StoreState,
    userEmail: String,
    scope: kotlinx.coroutines.CoroutineScope,
    message: (String) -> Unit
) {
    val dates = remember {
        (0..6).map { day -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, day + 1) }.time }
            .map { SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(it) }
    }
    var contactPhone by rememberSaveable { mutableStateOf(state.phone) }
    var date by rememberSaveable { mutableStateOf(dates.first()) }
    var time by rememberSaveable { mutableStateOf("13:00") }
    var people by rememberSaveable { mutableIntStateOf(2) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Reserva tu mesa", fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = NavyPrimary)
            Text("Disfruta tu experiencia en salón DeSego Huaycán.", fontSize = 12.sp, color = TextMuted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(contactPhone, { contactPhone = it }, label = { Text("Celular de contacto") }, modifier = Modifier.fillMaxWidth())
                    Text("Día de visita", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dates) { d ->
                            FilterChip(selected = date == d, onClick = { date = d }, label = { Text(d.substring(0, 5)) })
                        }
                    }
                    Text("Hora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00")) { t ->
                            FilterChip(selected = time == t, onClick = { time = t }, label = { Text(t) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Personas", fontWeight = FontWeight.Bold)
                        Quantity(people, { if (people > 1) people-- }, { if (people < 12) people++ })
                    }
                    Button(
                        onClick = {
                            state.profile(state.name, contactPhone)
                            scope.launch {
                                runCatching { state.reserve(date, time, people) }
                                    .onSuccess { message("Solicitud recibida. Te confirmaremos pronto.") }
                                    .onFailure { message(it.message ?: "No se pudo reservar.") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                    ) {
                        Text("Solicitar mi reserva", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Notifications(state: StoreState, open: (DemoOrder) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            InfoCard(
                "Bienvenido a DeSego",
                "Explora nuestra carta marina, haz tu pedido y acumula puntos en tu Club DeSego.",
                Icons.Default.LocalOffer
            )
        }
        items(state.orders, key = { it.id }) { order ->
            AccountAction("Pedido #${order.id}", "${order.status} · actualizado desde cocina", Icons.Default.ReceiptLong) {
                open(order)
            }
        }
    }
}

@Composable
private fun HelpScreen(message: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Estamos para ayudarte", fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = NavyPrimary)
        InfoCard("Tu tienda DeSego", "Explora la carta, haz tu pedido e ingresa a tu cuenta para acumular puntos.", Icons.Default.Info)
        InfoCard("Modo offline", "Tu carta y favoritos se conservan sin conexión. Para confirmar pedidos requieres internet.", Icons.Default.CloudOff)
        InfoCard("Pedidos y reservas", "El restaurante confirma la asignación de mesa y preparación al instante.", Icons.Default.Storefront)
    }
}

@Composable
internal fun FoodImage(product: StoreProduct, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, product.imageUrl) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.desego.data.OfflineImages.load(context, product.imageUrl)
        }
    }
    val image = bitmap
    if (image == null) {
        Image(painterResource(product.image), product.name, modifier, contentScale = ContentScale.Crop)
    } else {
        Image(image.asImageBitmap(), product.name, modifier, contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun DeliveryChoice(delivery: Boolean, change: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
            selected = delivery,
            onClick = { change(true) },
            label = { Text("Delivery") },
            leadingIcon = { Icon(Icons.Default.DeliveryDining, null, Modifier.size(18.dp)) },
            shape = RoundedCornerShape(16.dp)
        )
        FilterChip(
            selected = !delivery,
            onClick = { change(false) },
            label = { Text("Recojo en local") },
            leadingIcon = { Icon(Icons.Default.Storefront, null, Modifier.size(18.dp)) },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun Quantity(value: Int, minus: () -> Unit, plus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(onClick = minus, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Remove, "Reducir", modifier = Modifier.size(16.dp), tint = NavyPrimary)
        }
        Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp), color = NavyPrimary)
        IconButton(onClick = plus, modifier = Modifier.size(34.dp), enabled = value < 20) {
            Icon(Icons.Default.Add, "Aumentar", modifier = Modifier.size(16.dp), tint = NavyPrimary)
        }
    }
}

@Composable
private fun Totals(total: CartTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryLine("Subtotal", money(total.subtotal))
        if (total.discount > 0) SummaryLine("Descuento cupón", "− ${money(total.discount)}")
        SummaryLine("Delivery", if (total.delivery == 0) "Gratis" else money(total.delivery))
        HorizontalDivider(color = BorderSubtle)
        SummaryLine("Total a pagar", money(total.total), bold = true)
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) NavyPrimary else TextMuted, fontSize = if (bold) 15.sp else 13.sp)
        Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (bold) OceanBlue else NavyPrimary, fontSize = if (bold) 16.sp else 13.sp)
    }
}

@Composable
private fun InfoCard(title: String, body: String, icon: ImageVector) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(OceanSoftBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = OceanBlue, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                Spacer(Modifier.height(2.dp))
                Text(body, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
internal fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(OceanSoftBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = OceanBlue, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NavyPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, color = TextMuted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
    }
}

@Composable
private fun AccountAction(title: String, subtitle: String, icon: ImageVector, action: () -> Unit) {
    Surface(
        onClick = action,
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(OceanSoftBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = OceanBlue, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextMuted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
