package com.desego.iu.store

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.data.DatabaseApi
import com.desego.data.DemoUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.desego.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal val Navy = Color(0xFF163A3C)
internal val Ocean = Color(0xFF1D4C46)
internal val Sand = Color(0xFFFAF8F3)
internal val Orange = Color(0xFF1D4C46)
internal val Muted = Color(0xFF7B8478)
internal val Mint = Color(0xFF16764B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Storefront(user: DemoUser, onLogout: () -> Unit, initialTableToken: String? = null) {
    val context = LocalContext.current
    val state = remember(user.email) { StoreState(context, user) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableStateOf(if (!initialTableToken.isNullOrBlank()) "table" else "main") }
    var productId by rememberSaveable { mutableIntStateOf(1) }
    var orderId by rememberSaveable { mutableStateOf("") }
    var delivery by rememberSaveable { mutableStateOf(true) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var catalogCategory by rememberSaveable { mutableStateOf("Todo") }
    val screenState = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) { state.refresh() }
    LaunchedEffect(page,tab) {
        if (page=="order" || (page=="main" && tab==3)) {
            while(true) { delay(15000); state.refresh() }
        }
    }
    val message: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) }; Unit }
    val back: () -> Unit = { page = when (page) { "checkout" -> "cart"; "addressCheckout" -> "checkout"; else -> "main" } }
    BackHandler(page != "main") { back() }
    val titles = listOf("Inicio", "Carta", "Favoritos", "Pedidos", "Cuenta")
    val icons = listOf(Icons.Default.Home, Icons.Default.RestaurantMenu, Icons.Default.FavoriteBorder, Icons.Default.ReceiptLong, Icons.Default.PersonOutline)
    MaterialTheme(colorScheme = lightColorScheme(primary = Ocean, onPrimary = Color.White,
        secondary = Mint, secondaryContainer = Color(0xFFE9EEDF), onSecondaryContainer = Ocean,
        primaryContainer = Color(0xFFE9EEDF), onPrimaryContainer = Ocean,
        surfaceVariant = Sand, onSurfaceVariant = Muted, outline = Color(0xFFCFD1D5),
        outlineVariant = Color(0xFFEDEEF0), background = Sand, surface = Color.White,
        surfaceContainer = Color.White, surfaceContainerHigh = Sand, surfaceContainerHighest = Sand,
        onSurface = Navy, onBackground = Navy)) {
        Scaffold(modifier = Modifier.imePadding(), containerColor = Sand,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    title = {
                        Column(Modifier.then(if (page == "main") Modifier.clickable { page = "addresses" } else Modifier)) {
                            if (page == "main") Text("DESEGO · ${if (delivery) "ENTREGAR EN" else "RECOGER EN"}", fontSize = 10.sp, color = Ocean, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Text(if (page == "main") {
                                if (delivery) state.addresses.firstOrNull()?.street ?: "Añade tu dirección"
                                else "Desego · Local principal"
                            } else when (page) {
                                "product" -> "Sabor del mar"; "cart" -> "Tu carrito"; "checkout" -> "Finaliza tu pedido"
                                "order" -> "Detalle del pedido"; "addresses", "addressCheckout" -> "Mis direcciones"
                                "reservations" -> "Reserva tu mesa"; "profile" -> "Mi perfil"; "notifications" -> "Novedades"; "table" -> "Mi mesa"
                                else -> "Ayuda y contacto"
                            }, fontWeight = FontWeight.Bold, fontSize = if (page == "main") 16.sp else 19.sp, color = Navy,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }, navigationIcon = {
                        if (page != "main") IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    }, actions = {
                        if (page == "main") IconButton(onClick = { page = "table" }) { Icon(Icons.Default.QrCodeScanner, "Mi mesa") }
                        if (page == "main") IconButton(onClick = { page = "notifications" }) { Icon(Icons.Default.NotificationsNone, "Notificaciones") }
                        if (page !in listOf("cart", "checkout")) IconButton(onClick = { page = "cart" }) {
                            BadgedBox(badge = { if (state.count > 0) Badge { Text(state.count.toString()) } }) {
                                Icon(Icons.Default.ShoppingBag, "Carrito")
                            }
                        }
                    })
            }, bottomBar = {
                if (page == "main") Column {
                    if (state.count > 0) Surface(color = Color.White) {
                        Button(onClick = { page = "cart" }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint), shape = RoundedCornerShape(16.dp)) {
                            Surface(color = Color.White.copy(alpha = .18f), shape = RoundedCornerShape(8.dp)) {
                                Text(state.count.toString(), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("Ver carrito", Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Bold)
                            Text(money(calculateTotals(state.cart, false, state.coupon).let { it.subtotal - it.discount }), fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    titles.forEachIndexed { index, title -> NavigationBarItem(selected = tab == index,
                        onClick = { tab = index }, icon = { Icon(icons[index], title, modifier = Modifier.size(23.dp)) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Ocean, selectedTextColor = Ocean,
                            indicatorColor = Color.White, unselectedIconColor = Muted, unselectedTextColor = Muted),
                        label = { Text(title, fontSize = 10.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium) }) }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val openProduct: (StoreProduct) -> Unit = { productId = it.id; page = "product" }
                val add: (StoreProduct) -> Unit = { state.add(it.id); message("${it.name} añadido al carrito") }
                val screenKey = when (page) { "main" -> "main:$tab"; "product" -> "product:$productId"; "order" -> "order:$orderId"; else -> page }
                screenState.SaveableStateProvider(screenKey) {
                when (page) {
                    "main" -> when (tab) {
                        0 -> StoreHome(state, delivery, { delivery = it }, catalogQuery, { catalogQuery = it },
                            { category -> catalogCategory = category; tab = 1 }, openProduct, add, { page = "reservations" })
                        1 -> StoreCatalog(state, false, catalogQuery, { catalogQuery = it }, catalogCategory, { catalogCategory = it }, openProduct, add)
                        2 -> StoreCatalog(state, true, "", {}, "Todo", {}, openProduct, add)
                        3 -> StoreOrders(state, { orderId = it.id; page = "order" }, { state.reorder(it); page = "cart" })
                        else -> StoreAccount(user, state, { page = it }, onLogout)
                    }
                    "product" -> ProductDetail(demoProducts.firstOrNull { it.id == productId } ?: StoreProduct(productId,"Producto no disponible","Carta",0,R.drawable.food_ceviche,"Este producto ya no esta disponible.","",available=false), state) { quantity ->
                        state.add(productId, quantity); page = "cart"
                    }
                    "cart" -> StoreCart(state, delivery, { delivery = it }, { page = "checkout" }, { page = "main"; tab = 1 })
                    "checkout" -> StoreCheckout(state, delivery, { delivery = it }, { page = "addressCheckout" }) { address, payment, note ->
                        scope.launch {
                            runCatching { state.order(delivery,address,payment,note) }
                                .onSuccess { order -> orderId=order.id;page="order";tab=3;message("Tu pedido fue recibido por DESEGO.") }
                                .onFailure { message(it.message ?: "No se pudo confirmar el pedido.") }
                        }
                    }
                    "order" -> state.orders.firstOrNull { it.id == orderId }?.let { order ->
                        OrderDetail(order, { scope.launch { runCatching { state.cancel(order.id) }.onFailure { message(it.message ?: "No se pudo cancelar.") } }; Unit }, { state.reorder(order); page = "cart" })
                    }
                    "addresses", "addressCheckout" -> AddressBook(state, message)
                    "profile" -> EditProfile(state) { name, phone ->
                        scope.launch { runCatching { state.saveProfile(name,phone) }.onSuccess { page="main";message("Perfil actualizado.") }.onFailure { message(it.message ?: "No se pudo guardar el perfil.") } }
                    }
                    "reservations" -> Reservations(state, user.email, scope, message)
                    "notifications" -> Notifications(state) { orderId = it.id; page = "order" }
                    "table" -> DiningTable(user.email, message, initialTableToken)
                    "help" -> HelpScreen(message)
                }
                }
            }
        }
    }
}

@Composable
private fun ProductDetail(product: StoreProduct, state: StoreState, add: (Int) -> Unit) {
    var quantity by rememberSaveable(product.id) { mutableIntStateOf(1) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FoodImage(product, Modifier.fillMaxWidth().height(320.dp))
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.tag.ifEmpty { product.category }, Modifier.weight(1f), color = Ocean, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { state.favorite(product.id) }) {
                        Icon(if (product.id in state.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = Orange)
                    }
                }
                Text(product.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(money(product.price), fontSize = 23.sp, color = Ocean, fontWeight = FontWeight.Bold)
                Text(product.description, color = Muted, lineHeight = 24.sp)
                InfoCard("Preparado al momento", "Disponibilidad validada por el restaurante", Icons.Default.Restaurant)
                InfoCard("Ingredientes y alérgenos", "${product.allergens}. Confirma con el restaurante cualquier alergia o riesgo de trazas.", Icons.Default.Info)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cantidad", fontWeight = FontWeight.Bold)
                    Quantity(quantity, { if (quantity > 1) quantity-- }, { if (quantity < 20) quantity++ })
                }
            }
        }
        Button(onClick = { add(quantity) }, enabled = product.available, modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Añadir al carrito · ${money(product.price * quantity)}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StoreCart(state: StoreState, delivery: Boolean, mode: (Boolean) -> Unit, checkout: () -> Unit, menu: () -> Unit) {
    if (state.cart.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EmptyState(Icons.Default.ShoppingBag, "Tu carrito espera un antojo", "Descubre nuestra carta y encuentra tu favorito.")
            Button(onClick = menu) { Text("Explorar la carta") }
        }; return
    }
    var couponInput by rememberSaveable(state.coupon) { mutableStateOf(state.coupon) }
    var couponError by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { DeliveryChoice(delivery, mode) }
            items(demoProducts.filter { state.quantity(it.id) > 0 }, key = { it.id }) { product ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FoodImage(product, Modifier.size(85.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            Text(money(product.price * state.quantity(product.id)), color = Ocean, fontWeight = FontWeight.Bold)
                            Quantity(state.quantity(product.id), { state.setQuantity(product.id, state.quantity(product.id) - 1) }, { state.add(product.id) })
                        }
                        IconButton(onClick = { state.setQuantity(product.id, 0) }) { Icon(Icons.Default.DeleteOutline, "Eliminar ${product.name}", tint = Muted) }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(couponInput, { couponInput = it; couponError = false }, label = { Text("Cupón") },
                        singleLine = true, isError = couponError, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    TextButton(onClick = {
                        couponError = false
                        if (!couponError) state.applyCoupon(couponInput)
                    }) { Text("Aplicar") }
                }
                if (couponError) Text("Cupon no disponible", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (state.coupon.isNotBlank()) Text("El cupon se validara al confirmar con el restaurante.", color = Ocean, fontSize = 12.sp)
            }
            item { Totals(calculateTotals(state.cart, delivery, state.coupon)) }
            item { Text("El precio final, el cupon y la disponibilidad se validan al confirmar con el restaurante.", color = Muted, fontSize = 12.sp) }
        }
        Button(onClick = checkout, modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Continuar · ${money(calculateTotals(state.cart, delivery, state.coupon).total)}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StoreCheckout(state: StoreState, delivery: Boolean, mode: (Boolean) -> Unit, addresses: () -> Unit,
    confirm: (DeliveryAddress?, String, String) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf(state.addresses.firstOrNull()?.id) }
    val selected = state.addresses.firstOrNull { it.id == selectedId } ?: state.addresses.firstOrNull()
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    var payment by rememberSaveable { mutableStateOf("Contraentrega") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DeliveryChoice(delivery, mode)
            Text(if (delivery) "¿A dónde lo llevamos?" else "Te esperamos en Desego", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (delivery) {
                state.addresses.forEach { address ->
                    Surface(onClick = { selectedId = address.id }, color = if (selected?.id == address.id) Color(0xFFE9EEDF) else Color.White, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected?.id == address.id, onClick = { selectedId = address.id })
                            Column { Text(address.label, fontWeight = FontWeight.Bold); Text("${address.street}, ${address.district}", fontSize = 13.sp, color = Muted) }
                        }
                    }
                }
                OutlinedButton(onClick = addresses, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Añadir dirección") }
                Text("Delivery: S/ 5.00. El restaurante coordinara la entrega.", fontSize = 12.sp, color = Muted)
            } else InfoCard("Recojo en el local", "Recojo en DESEGO. Espera a que tu pedido figure como Listo.", Icons.Default.Storefront)
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(9) }, label = { Text("Celular de contacto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Forma de pago", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            listOf("Contraentrega", "Tarjeta", "Yape / Plin").forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { payment = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(payment == option, { payment = option })
                    Column { Text(option); if (option != "Contraentrega") Text("Pago coordinado al recibir", fontSize = 11.sp, color = Muted) }
                }
            }
            OutlinedTextField(note, { note = it.take(300) }, label = { Text("Indicaciones para tu pedido (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Totals(calculateTotals(state.cart, delivery, state.coupon))
            InfoCard("Tu pedido en DESEGO", "El restaurante recibe tu pedido. El pago se coordina al recibir o recoger.", Icons.Default.Info)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Button(onClick = {
            error = when {
                state.cart.isEmpty() -> "Añade productos antes de continuar."
                delivery && selected == null -> "Añade una dirección para delivery."
                phone.length != 9 -> "Escribe un celular de 9 dígitos."
                else -> null
            }
            if (error == null) { state.profile(state.name, phone); confirm(if (delivery) selected else null, payment, note) }
        }, modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp), shape = RoundedCornerShape(16.dp), enabled = state.cart.isNotEmpty() && !state.busy) {
            Text("Confirmar pedido", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StoreOrders(state: StoreState, open: (DemoOrder) -> Unit, reorder: (DemoOrder) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Mis pedidos", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("Tus antojos, en un solo lugar.", color = Muted) }
        if (state.orders.isEmpty()) item { EmptyState(Icons.Default.ReceiptLong, "Tu primer pedido está por llegar", "Tus pedidos confirmados con el restaurante apareceran aqui.") }
        items(state.orders, key = { it.id }) { order ->
            Card(onClick = { open(order) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row { Text("#${order.id}", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(order.status, color = if (order.status == "Cancelado") Orange else Ocean, fontWeight = FontWeight.Bold) }
                    Text(order.date, fontSize = 12.sp, color = Muted)
                    Text(order.items.entries.joinToString { (id, qty) -> "$qty × ${order.itemNames[id] ?: demoProducts.firstOrNull { it.id == id }?.name ?: "Producto"}" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(money(order.total), Modifier.weight(1f), fontWeight = FontWeight.ExtraBold)
                        TextButton(onClick = { reorder(order) }) { Text("Volver a pedir") }
                    }
                    Text("${order.mode} · Registrado en DESEGO", fontSize = 11.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun OrderDetail(order: DemoOrder, cancel: () -> Unit, reorder: () -> Unit) {
    var confirmCancel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Icon(if (order.status == "Cancelado") Icons.Default.Cancel else Icons.Default.CheckCircle, null, tint = Ocean, modifier = Modifier.size(54.dp))
        Text(if (order.status == "Cancelado") "Pedido cancelado" else "¡Tu pedido ya esta en DESEGO!", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("#${order.id} · ${order.date}", color = Muted)
        InfoCard("Sin cobro realizado", "${order.payment} · pago pendiente. Pedido registrado en el restaurante.", Icons.Default.Payments)
        if (order.status != "Cancelado") {
            Text("Seguimiento", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            (if(order.mode=="Delivery") listOf("Pendiente","Preparando","Listo","En reparto","Entregado") else listOf("Pendiente","Preparando","Listo","Entregado")).forEachIndexed { index, title ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (index <= (if(order.mode=="Delivery") listOf("Pendiente","Preparando","Listo","En reparto","Entregado") else listOf("Pendiente","Preparando","Listo","Entregado")).indexOf(order.status)) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (index <= (if(order.mode=="Delivery") listOf("Pendiente","Preparando","Listo","En reparto","Entregado") else listOf("Pendiente","Preparando","Listo","Entregado")).indexOf(order.status)) Ocean else Color.LightGray)
                    Text(title, Modifier.padding(start = 14.dp), color = if (index <= (if(order.mode=="Delivery") listOf("Pendiente","Preparando","Listo","En reparto","Entregado") else listOf("Pendiente","Preparando","Listo","Entregado")).indexOf(order.status)) Navy else Muted)
                }
            }
            Text("El estado se actualiza desde el restaurante cada 15 segundos.", fontSize = 12.sp, color = Muted)
        }
        InfoCard(order.mode, order.address, Icons.Default.LocationOn)
        order.items.forEach { (id, qty) -> SummaryLine("$qty x ${order.itemNames[id] ?: demoProducts.firstOrNull { it.id==id }?.name ?: "Producto"}",money((order.itemPrices[id] ?: demoProducts.firstOrNull { it.id==id }?.price ?: 0)*qty)) }
        HorizontalDivider(); SummaryLine("Total del pedido", money(order.total), true)
        if (order.note.isNotBlank()) Text("Indicaciones: ${order.note}", color = Muted)
        OutlinedButton(onClick = reorder, modifier = Modifier.fillMaxWidth()) { Text("Volver a pedir") }
        if (order.status == "Pendiente") TextButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar pedido", color = Orange) }
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("¿Cancelar este pedido?") },
        text = { Text("Se cancelara el pedido pendiente en el restaurante.") },
        confirmButton = { TextButton(onClick = { cancel(); confirmCancel = false }) { Text("Cancelar pedido") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Conservar") } })
}

@Composable
private fun StoreAccount(user: DemoUser, state: StoreState, open: (String) -> Unit, logout: () -> Unit) {
    var askLogout by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(55.dp))
                    Spacer(Modifier.height(12.dp)); Text(state.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(user.email, color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
                    Text("Cuenta de cliente · prototipo local", color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                }
            }
        }
        item { AccountAction("Mi perfil", "Nombre y celular", Icons.Default.PersonOutline) { open("profile") } }
        item { AccountAction("Mis direcciones", "Gestiona tus destinos de delivery", Icons.Default.LocationOn) { open("addresses") } }
        item { AccountAction("Reservar mesa", "Planea tu próxima visita", Icons.Default.TableRestaurant) { open("reservations") } }
        item { AccountAction("Ayuda y contacto", "Preguntas y sugerencias", Icons.Default.SupportAgent) { open("help") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsNone, null, tint = Ocean)
                Column(Modifier.weight(1f).padding(start = 16.dp)) { Text("Notificaciones", fontWeight = FontWeight.Bold); Text("Preferencia de este dispositivo", color = Muted, fontSize = 12.sp) }
                Switch(state.notifications, { state.notificationSetting(it) })
            }
        }
        item { OutlinedButton(onClick = { askLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") } }
        item { Text("DESEGO - Conectado con el restaurante.\nTu carrito se conserva en este dispositivo.", color = Muted, fontSize = 11.sp) }
    }
    if (askLogout) AlertDialog(onDismissRequest = { askLogout = false }, title = { Text("¿Cerrar sesión?") }, text = { Text("Tu carrito y favoritos se conservarán para cuando vuelvas.") },
        confirmButton = { TextButton(onClick = logout) { Text("Cerrar sesión") } }, dismissButton = { TextButton(onClick = { askLogout = false }) { Text("Volver") } })
}

@Composable
private fun AddressBook(state: StoreState, message: (String) -> Unit) {
    val addressScope = rememberCoroutineScope()
    var adding by rememberSaveable { mutableStateOf(state.addresses.isEmpty()) }
    var label by rememberSaveable { mutableStateOf("Casa") }
    var street by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(state.addresses, key = { it.id }) { address ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Ocean)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(address.label, fontWeight = FontWeight.Bold); Text("${address.street}, ${address.district}", fontSize = 13.sp)
                        if (address.reference.isNotBlank()) Text(address.reference, color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { addressScope.launch { runCatching { state.removeAddress(address.id) }.onFailure { message(it.message ?: "No se pudo eliminar la direccion.") } } }) { Icon(Icons.Default.DeleteOutline, "Eliminar dirección") }
                }
            }
        }
        item { OutlinedButton(onClick = { adding = !adding }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Nueva dirección") } }
        if (adding) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(label, { label = it.take(30) }, label = { Text("Nombre: casa, trabajo…") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(street, { street = it.take(150) }, label = { Text("Calle, número y departamento") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(district, { district = it.take(60) }, label = { Text("Distrito") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(reference, { reference = it.take(150) }, label = { Text("Referencia (opcional)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    if (label.isBlank() || street.trim().length < 5 || district.isBlank()) error = "Completa el nombre, una dirección válida y el distrito."
                    else { addressScope.launch { runCatching { state.addAddress(label,street,district,reference) }.onSuccess { adding=false;street="";district="";reference="";error=null;message("Direccion guardada en DESEGO.") }.onFailure { error=it.message } } }
                }, modifier = Modifier.fillMaxWidth()) { Text("Guardar dirección") }
                Text("El restaurante confirmara la entrega. La ubicacion en mapa requiere configurar un proveedor.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EditProfile(state: StoreState, save: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(state.name) }
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Icon(Icons.Default.AccountCircle, null, tint = Ocean, modifier = Modifier.size(80.dp))
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(9) }, label = { Text("Celular") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { save(name, phone) }, enabled = name.trim().length >= 2 && (phone.isBlank() || phone.length == 9), modifier = Modifier.fillMaxWidth()) { Text("Guardar cambios") }
    }
}

@Composable
private fun Reservations(state: StoreState, userEmail: String, scope: kotlinx.coroutines.CoroutineScope, message: (String) -> Unit) {
    val dates = remember {
        (0..6).map { day -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, day + 1) }.time }
            .map { SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(it) }
    }
    var contactPhone by rememberSaveable { mutableStateOf(state.phone) }
    var date by rememberSaveable { mutableStateOf(dates.first()) }
    var time by rememberSaveable { mutableStateOf("13:00") }
    var people by rememberSaveable { mutableIntStateOf(2) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Un lugar para compartir", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Solicitud de reserva · pendiente de confirmación del local", color = Muted, fontSize = 12.sp) }
        item { OutlinedTextField(contactPhone,{contactPhone=it},label={Text("Celular de contacto")},modifier=Modifier.fillMaxWidth()) }
        item { Text("Elige un día", fontWeight = FontWeight.Bold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(dates) { d -> FilterChip(date == d, { date = d }, label = { Text(d.substring(0, 5)) }) } } }
        item { Text("Hora", fontWeight = FontWeight.Bold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf("12:00", "13:00", "14:00", "15:00", "16:00")) { t -> FilterChip(time == t, { time = t }, label = { Text(t) }) } } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Personas", fontWeight = FontWeight.Bold); Quantity(people, { if (people > 1) people-- }, { if (people < 12) people++ }) } }
        item { Button(onClick = {
            state.profile(state.name,contactPhone)
            scope.launch { runCatching { state.reserve(date,time,people) }.onSuccess { message("Solicitud recibida. El restaurante confirmara tu reserva.") }.onFailure { message(it.message ?: "No se pudo reservar.") } }
        }, modifier = Modifier.fillMaxWidth()) { Text("Solicitar mi reserva") } }
        item { Text("Mis reservas", fontWeight = FontWeight.Bold, fontSize = 21.sp) }
        items(state.reservations, key = { it.id }) { reservation ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${reservation.date} · ${reservation.time}", fontWeight = FontWeight.Bold)
                    Text("${reservation.people} personas · ${reservation.status}", color = Muted)
                    if (reservation.status != "Cancelada") TextButton(onClick = { scope.launch { runCatching { state.cancelReservation(reservation.id) }.onFailure { message(it.message ?: "No se pudo cancelar.") } } }) { Text("Cancelar solicitud") }
                }
            }
        }
    }
}

@Composable
private fun Notifications(state: StoreState, open: (DemoOrder) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { InfoCard("Bienvenido a Desego", "Explora nuestra carta. Ingresa a tu cuenta para consultar pedidos y reservar tu mesa.", Icons.Default.LocalOffer) }
        items(state.orders, key = { it.id }) { order -> AccountAction("Pedido #${order.id}", "${order.status} · estado del restaurante", Icons.Default.ReceiptLong) { open(order) } }
        item { Text("Esta bandeja es local. Las notificaciones push se conectarán más adelante.", fontSize = 12.sp, color = Muted) }
    }
}

@Composable
private fun HelpScreen(message: (String) -> Unit) {
    var suggestion by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Estamos para ayudarte", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        InfoCard("¿Cómo funciona esta versión?", "Elige tus platos, añade una dirección y recorre el checkout. Los pedidos son de prueba y no generan cobros.", Icons.Default.Info)
        InfoCard("¿Puedo recoger mi pedido?", "Sí. Puedes elegir Recojo y ver el flujo de atención en tienda.", Icons.Default.Storefront)
        InfoCard("Ingredientes y alergias", "La carta contiene información de ejemplo. La información final debe ser confirmada por el restaurante.", Icons.Default.Restaurant)
        OutlinedTextField(suggestion, { suggestion = it.take(500) }, label = { Text("Escribe una sugerencia de prueba") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(onClick = { suggestion = ""; message("Formulario probado. El envío a soporte se conectará después.") }, enabled = suggestion.trim().length >= 5, modifier = Modifier.fillMaxWidth()) { Text("Probar formulario") }
    }
}

@Composable
internal fun FoodImage(product: StoreProduct, modifier: Modifier) = Image(painterResource(product.image), product.name, modifier, contentScale = ContentScale.Crop)

@Composable
internal fun DeliveryChoice(delivery: Boolean, change: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(delivery, { change(true) }, label = { Text("Delivery") }, leadingIcon = { Icon(Icons.Default.DeliveryDining, null, Modifier.size(19.dp)) })
        FilterChip(!delivery, { change(false) }, label = { Text("Recojo") }, leadingIcon = { Icon(Icons.Default.Storefront, null, Modifier.size(19.dp)) })
    }
}
@Composable
private fun Quantity(value: Int, minus: () -> Unit, plus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = minus, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Remove, "Reducir cantidad") }
        Text(value.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
        IconButton(onClick = plus, modifier = Modifier.size(38.dp), enabled = value < 20) { Icon(Icons.Default.Add, "Aumentar cantidad") }
    }
}
@Composable
private fun Totals(total: CartTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryLine("Subtotal", money(total.subtotal))
        if (total.discount > 0) SummaryLine("Descuento", "− ${money(total.discount)}")
        SummaryLine("Delivery", if (total.delivery == 0) "Sin costo" else money(total.delivery))
        HorizontalDivider(); SummaryLine("Total", money(total.total), true)
    }
}
@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) Navy else Muted)
        Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium, color = Navy)
    }
}
@Composable
internal fun SectionTitle(title: String, action: String, click: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        TextButton(onClick = click) { Text(action) }
    }
}
@Composable
private fun InfoCard(title: String, body: String, icon: ImageVector) {
    Surface(color = Color(0xFFF4F4F5), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, null, tint = Ocean, modifier = Modifier.size(24.dp))
            Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(5.dp)); Text(body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp) }
        }
    }
}
@Composable
internal fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 45.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Ocean, modifier = Modifier.size(60.dp)); Spacer(Modifier.height(20.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp)); Text(body, color = Muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
@Composable
private fun AccountAction(title: String, subtitle: String, icon: ImageVector, action: () -> Unit) {
    Surface(onClick = action, color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Ocean)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 12.sp, color = Muted) }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
