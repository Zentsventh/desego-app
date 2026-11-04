package com.desego.iu.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.data.DemoUser
import com.desego.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AccountNavSection(val key: String, val title: String, val icon: ImageVector)

val ACCOUNT_NAV_SECTIONS = listOf(
    AccountNavSection("orders", "Mis pedidos", Icons.Default.ReceiptLong),
    AccountNavSection("loyalty", "Club DeSego", Icons.Default.Stars),
    AccountNavSection("reservations", "Mis reservas", Icons.Default.TableRestaurant),
    AccountNavSection("addresses", "Mis direcciones", Icons.Default.LocationOn),
    AccountNavSection("favorites", "Favoritos", Icons.Default.Favorite),
    AccountNavSection("reviews", "Mis reseñas", Icons.Default.Star),
    AccountNavSection("support", "Ayuda y soporte", Icons.Default.SupportAgent),
    AccountNavSection("profile", "Mi perfil", Icons.Default.Person)
)

@Composable
fun StoreAccountResponsive(
    user: DemoUser,
    state: StoreState,
    onOpenProduct: (StoreProduct) -> Unit,
    onAddToCart: (StoreProduct) -> Unit,
    onViewOrder: (DemoOrder) -> Unit,
    onLogout: () -> Unit,
    message: (String) -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf("orders") }
    val scope = rememberCoroutineScope()
    var askLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Hero Card & Stats
        AccountHeroCard(user, state, onSelectSection = { activeTab = it })

        // Horizontal Navigation Bar (8 sections)
        HorizontalNavPills(
            sections = ACCOUNT_NAV_SECTIONS,
            activeKey = activeTab,
            onSelect = { activeTab = it }
        )

        // Section Content
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                "orders" -> OrdersSection(state, onViewOrder = onViewOrder, onReorder = { state.reorder(it); message("Platos añadidos al carrito") })
                "loyalty" -> LoyaltySection(state, scope, message)
                "reservations" -> ReservationsSection(state, user.email, scope, message)
                "addresses" -> AddressesSection(state, scope, message)
                "favorites" -> FavoritesSection(state, onOpenProduct, onAddToCart)
                "reviews" -> ReviewsSection(state, scope, message)
                "support" -> SupportSection(message)
                "profile" -> ProfileSection(state, scope, message, onAskLogout = { askLogout = true })
            }
        }
    }

    if (askLogout) {
        AlertDialog(
            onDismissRequest = { askLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se cerrará la sesión en este dispositivo. Tus pedidos y reservas seguirán sincronizados en tu cuenta.") },
            confirmButton = { TextButton(onClick = { askLogout = false; onLogout() }) { Text("Cerrar sesión", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { askLogout = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AccountHeroCard(
    user: DemoUser,
    state: StoreState,
    onSelectSection: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Navy)
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF004797)))
            ).padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(state.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(user.email, color = Color(0xFFE2E8F0), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Tier badge
                    Surface(
                        color = when (state.loyaltyTier) {
                            "Oro" -> Color(0xFFF59E0B)
                            "Plata" -> Color(0xFF475569)
                            else -> Color(0xFFD97706)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, null, tint = if (state.loyaltyTier == "Oro") NavyPrimary else Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                " ${state.loyaltyTier}",
                                color = if (state.loyaltyTier == "Oro") NavyPrimary else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // Quick stats grid (4 clickable cards)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Pedidos", "${state.orders.size}", Icons.Default.ReceiptLong, Modifier.weight(1f)) { onSelectSection("orders") }
                    StatCard("Club Puntos", "${state.loyaltyPoints}", Icons.Default.Stars, Modifier.weight(1f)) { onSelectSection("loyalty") }
                    StatCard("Reservas", "${state.reservations.size}", Icons.Default.TableRestaurant, Modifier.weight(1f)) { onSelectSection("reservations") }
                    StatCard("Favoritos", "${state.favorites.size}", Icons.Default.Favorite, Modifier.weight(1f)) { onSelectSection("favorites") }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x33000000),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFFFEF08A), modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(3.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun HorizontalNavPills(
    sections: List<AccountNavSection>,
    activeKey: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sections, key = { it.key }) { sec ->
            val isActive = activeKey == sec.key
            Surface(
                onClick = { onSelect(sec.key) },
                shape = RoundedCornerShape(20.dp),
                color = if (isActive) OceanBlue else Color.White,
                border = BorderStroke(1.dp, if (isActive) OceanBlue else Color(0xFFCBD5E1)),
                shadowElevation = if (isActive) 2.dp else 0.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        sec.icon,
                        null,
                        tint = if (isActive) Color.White else Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sec.title,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isActive) Color.White else Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

// ---------------- 1. MIS PEDIDOS ----------------
@Composable
private fun OrdersSection(
    state: StoreState,
    onViewOrder: (DemoOrder) -> Unit,
    onReorder: (DemoOrder) -> Unit
) {
    if (state.orders.isEmpty()) {
        EmptyState(Icons.Default.ReceiptLong, "Aún no tienes pedidos", "Tus pedidos confirmados aparecerán aquí con seguimiento en vivo.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(state.orders, key = { it.id }) { order ->
            Card(
                onClick = { onViewOrder(order) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f), color = Navy)
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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

                    Text(order.date, fontSize = 12.sp, color = Muted)

                    // Order Tracking Stepper
                    if (order.status != "Cancelado") {
                        val steps = if (order.mode == "Delivery") listOf("Pendiente", "Preparando", "Listo", "En reparto", "Entregado")
                        else listOf("Pendiente", "Preparando", "Listo", "Entregado")
                        val currentIdx = steps.indexOf(order.status).coerceAtLeast(0)

                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                steps.forEachIndexed { idx, s ->
                                    val done = idx <= currentIdx
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Box(
                                            Modifier.size(16.dp).clip(CircleShape).background(if (done) Ocean else Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                        Text(s, fontSize = 9.sp, maxLines = 1, color = if (done) Navy else Muted, fontWeight = if (idx == currentIdx) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        order.items.entries.joinToString { (id, qty) -> "$qty × ${order.itemNames[id] ?: demoProducts.firstOrNull { it.id == id }?.name ?: "Plato"}" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = Navy
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(money(order.total), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Ocean, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onReorder(order) }) {
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

// ---------------- 2. CLUB DESEGO (LOYALTY) ----------------
@Composable
private fun LoyaltySection(state: StoreState, scope: kotlinx.coroutines.CoroutineScope, message: (String) -> Unit) {
    var selectedReward by remember { mutableStateOf<LoyaltyReward?>(null) }
    var redeeming by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Club DeSego Gastronomía", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                            Text("Gana 1 punto por cada S/ 10 en consumo", fontSize = 11.sp, color = Muted)
                        }
                    }
                    Text("${state.loyaltyPoints} Puntos", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ocean)
                    if (state.nextTier != null) {
                        Text("Te faltan ${state.pointsToNext} pts para el nivel ${state.nextTier}", fontSize = 12.sp, color = Muted)
                    }
                }
            }
        }

        item {
            Text("Recompensas para canjear", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
        }

        if (state.rewards.isEmpty()) {
            item {
                Text("Pronto nuevas recompensas estarán disponibles.", fontSize = 13.sp, color = Muted)
            }
        } else {
            items(state.rewards, key = { it.id }) { reward ->
                val canAfford = state.loyaltyPoints >= reward.pointsCost
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(if (canAfford) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CardGiftcard, null, tint = if (canAfford) Color(0xFF166534) else Muted, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(reward.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy)
                            if (reward.description.isNotBlank()) Text(reward.description, fontSize = 11.sp, color = Muted, maxLines = 2)
                            Text("${reward.pointsCost} puntos", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ocean)
                        }
                        Button(
                            onClick = { selectedReward = reward },
                            enabled = canAfford && !redeeming,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Canjear", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (state.redemptions.isNotEmpty()) {
            item { Text("Mis cupones canjeados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) }
            items(state.redemptions, key = { it.id }) { red ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(red.rewardName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Código: ${red.code}", fontWeight = FontWeight.ExtraBold, color = Ocean, fontSize = 13.sp)
                            Text("Emitido: ${red.date}", fontSize = 11.sp, color = Muted)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF1F5F9)) {
                            Text(red.status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = Navy)
                        }
                    }
                }
            }
        }

        if (state.transactions.isNotEmpty()) {
            item { Text("Historial de puntos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) }
            items(state.transactions, key = { it.id }) { tx ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tx.description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy)
                        Text(tx.date, fontSize = 11.sp, color = Muted)
                    }
                    Text(
                        "${if (tx.pointsDelta > 0) "+" else ""}${tx.pointsDelta} pts",
                        fontWeight = FontWeight.Bold,
                        color = if (tx.pointsDelta > 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                        fontSize = 14.sp
                    )
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }

    selectedReward?.let { reward ->
        AlertDialog(
            onDismissRequest = { selectedReward = null },
            title = { Text("¿Canjear ${reward.name}?") },
            text = { Text("Se descontarán ${reward.pointsCost} puntos de tu saldo para generar tu cupón de descuento.") },
            confirmButton = {
                Button(
                    onClick = {
                        redeeming = true
                        scope.launch {
                            runCatching { state.redeemReward(reward.id) }
                                .onSuccess { message("¡Recompensa canjeada con éxito!"); selectedReward = null }
                                .onFailure { message(it.message ?: "No se pudo canjear la recompensa.") }
                            redeeming = false
                        }
                    },
                    enabled = !redeeming
                ) {
                    Text("Confirmar canje")
                }
            },
            dismissButton = { TextButton(onClick = { selectedReward = null }) { Text("Volver") } }
        )
    }
}

// ---------------- 3. MIS RESERVAS ----------------
@Composable
private fun ReservationsSection(
    state: StoreState,
    userEmail: String,
    scope: kotlinx.coroutines.CoroutineScope,
    message: (String) -> Unit
) {
    val dates = remember {
        (0..6).map { day -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, day + 1) }.time }
            .map { SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(it) }
    }
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    var date by rememberSaveable { mutableStateOf(dates.first()) }
    var time by rememberSaveable { mutableStateOf("13:00") }
    var people by rememberSaveable { mutableIntStateOf(2) }
    var submitting by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Solicitar reserva de mesa", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Sede Huaycán · Confirmación sujeta a disponibilidad", fontSize = 12.sp, color = Muted)

                    OutlinedTextField(
                        phone, { phone = it },
                        label = { Text("Celular de contacto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Fecha de visita", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dates) { d ->
                            FilterChip(
                                selected = date == d,
                                onClick = { date = d },
                                label = { Text(d.substring(0, 5)) }
                            )
                        }
                    }

                    Text("Hora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00")) { t ->
                            FilterChip(
                                selected = time == t,
                                onClick = { time = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Número de personas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (people > 1) people-- }) { Icon(Icons.Default.Remove, null) }
                            Text("$people", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if (people < 12) people++ }) { Icon(Icons.Default.Add, null) }
                        }
                    }

                    Button(
                        onClick = {
                            submitting = true
                            scope.launch {
                                runCatching { state.reserve(date, time, people) }
                                    .onSuccess { message("Reserva solicitada. El restaurante confirmará en breve.") }
                                    .onFailure { message(it.message ?: "Error al solicitar reserva.") }
                                submitting = false
                            }
                        },
                        enabled = !submitting && phone.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enviar solicitud de reserva", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Text("Mis reservas registradas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) }

        if (state.reservations.isEmpty()) {
            item {
                Text("No tienes reservas registradas.", fontSize = 13.sp, color = Muted)
            }
        } else {
            items(state.reservations, key = { it.id }) { res ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${res.date} · ${res.time}", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (res.status == "Cancelada") Color(0xFFFEE2E2) else Color(0xFFDBEAFE)
                            ) {
                                Text(res.status, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (res.status == "Cancelada") Color(0xFF991B1B) else Color(0xFF1E40AF))
                            }
                        }
                        Text("${res.people} personas · Mesa en salón Huaycán", fontSize = 12.sp, color = Muted)
                        if (res.status != "Cancelada") {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { state.cancelReservation(res.id) }
                                        .onSuccess { message("Reserva cancelada.") }
                                        .onFailure { message(it.message ?: "No se pudo cancelar.") }
                                }
                            }) {
                                Text("Cancelar reserva", color = Color(0xFFDC2626), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 4. MIS DIRECCIONES ----------------
@Composable
private fun AddressesSection(state: StoreState, scope: kotlinx.coroutines.CoroutineScope, message: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(state.addresses.isEmpty()) }
    var label by rememberSaveable { mutableStateOf("Casa") }
    var street by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("Ate / Huaycán") }
    var reference by rememberSaveable { mutableStateOf("") }
    var isDefault by rememberSaveable { mutableStateOf(state.addresses.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(state.addresses, key = { it.id }) { address ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Ocean, modifier = Modifier.size(20.dp))
                        Text(address.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        if (address.isDefault) {
                            Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                                Text("Principal", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            }
                        }
                    }
                    Text("${address.street}, ${address.district}", fontSize = 13.sp, color = Navy)
                    if (address.reference.isNotBlank()) Text("Ref: ${address.reference}", fontSize = 11.sp, color = Muted)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!address.isDefault) {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { state.setDefaultAddress(address.id) }
                                        .onSuccess { message("Dirección predeterminada actualizada.") }
                                        .onFailure { message(it.message ?: "Error al actualizar.") }
                                }
                            }) { Text("Marcar como principal", fontSize = 11.sp) }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            scope.launch {
                                runCatching { state.removeAddress(address.id) }
                                    .onSuccess { message("Dirección eliminada.") }
                                    .onFailure { message(it.message ?: "No se pudo eliminar.") }
                            }
                        }) {
                            Icon(Icons.Default.DeleteOutline, null, tint = Muted, modifier = Modifier.size(20.dp))
                        }
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
                Text(if (adding) "Cancelar" else "Agregar nueva dirección")
            }
        }

        if (adding) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nueva dirección de delivery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        OutlinedTextField(label, { label = it.take(30) }, label = { Text("Etiqueta (ej. Casa, Trabajo)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(street, { street = it.take(150) }, label = { Text("Calle, número o lote") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(district, { district = it.take(60) }, label = { Text("Distrito") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(reference, { reference = it.take(150) }, label = { Text("Referencia (opcional)") }, modifier = Modifier.fillMaxWidth())

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                            Text("Establecer como dirección principal", fontSize = 13.sp)
                        }

                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                        Button(
                            onClick = {
                                if (label.isBlank() || street.trim().length < 5 || district.isBlank()) {
                                    error = "Completa los campos de la dirección."
                                } else {
                                    busy = true
                                    scope.launch {
                                        runCatching { state.addAddress(label, street, district, reference, isDefault) }
                                            .onSuccess {
                                                adding = false; street = ""; reference = ""; error = null
                                                message("Dirección guardada correctamente.")
                                            }
                                            .onFailure { error = it.message }
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Guardar dirección")
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 5. FAVORITOS ----------------
@Composable
private fun FavoritesSection(
    state: StoreState,
    onOpenProduct: (StoreProduct) -> Unit,
    onAddToCart: (StoreProduct) -> Unit
) {
    val favProducts = demoProducts.filter { it.id in state.favorites }
    if (favProducts.isEmpty()) {
        EmptyState(Icons.Default.FavoriteBorder, "Sin favoritos aún", "Marca tus platos favoritos con el corazón para tenerlos siempre a mano.")
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(favProducts, key = { it.id }) { product ->
            Card(
                onClick = { onOpenProduct(product) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FoodImage(product, Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Navy)
                        Text(money(product.price), fontWeight = FontWeight.ExtraBold, color = Ocean, fontSize = 14.sp)
                    }
                    IconButton(onClick = { state.favorite(product.id) }) {
                        Icon(Icons.Default.Favorite, "Quitar", tint = Color(0xFFEF4444))
                    }
                    FilledIconButton(onClick = { onAddToCart(product) }, enabled = product.available, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, "Añadir")
                    }
                }
            }
        }
    }
}

// ---------------- 6. MIS RESEÑAS ----------------
@Composable
private fun ReviewsSection(
    state: StoreState,
    scope: kotlinx.coroutines.CoroutineScope,
    message: (String) -> Unit
) {
    var orderId by rememberSaveable { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<StoreProduct?>(null) }
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var comment by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Calificar tu experiencia gastronómica", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                    Text("Tus opiniones nos ayudan a mantener la frescura y sazón que mereces.", fontSize = 12.sp, color = Muted)

                    Text("Selecciona una calificación:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        comment, { comment = it.take(500) },
                        label = { Text("¿Qué te pareció el sabor y la atención?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    val deliveredOrders = state.orders.filter { it.status in listOf("Listo", "Entregado") }
                    if (deliveredOrders.isNotEmpty()) {
                        Button(
                            onClick = {
                                val firstDelivered = deliveredOrders.first()
                                val firstItem = firstDelivered.items.keys.firstOrNull() ?: 1
                                busy = true
                                scope.launch {
                                    runCatching {
                                        state.submitReview(firstDelivered.serverId, firstItem, rating, comment)
                                    }.onSuccess {
                                        message("¡Gracias por tu reseña!")
                                        comment = ""
                                    }.onFailure {
                                        message(it.message ?: "No se pudo registrar la reseña.")
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && comment.trim().length >= 4,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Publicar reseña", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Podrás calificar una vez que recibas tu primer pedido.", fontSize = 12.sp, color = Muted)
                    }
                }
            }
        }

        item { Text("Mis opiniones publicadas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) }

        if (state.reviews.isEmpty()) {
            item {
                Text("Aún no has enviado reseñas.", fontSize = 13.sp, color = Muted)
            }
        } else {
            items(state.reviews, key = { it.id }) { rev ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rev.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Row {
                                (1..5).forEach { s ->
                                    Icon(
                                        Icons.Default.Star, null,
                                        tint = if (s <= rev.rating) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Text(rev.comment, fontSize = 12.sp, color = Navy)
                        Text(rev.date, fontSize = 10.sp, color = Muted)
                    }
                }
            }
        }
    }
}

// ---------------- 7. AYUDA Y SOPORTE ----------------
@Composable
private fun SupportSection(message: (String) -> Unit) {
    val context = LocalContext.current
    val openWhatsApp: () -> Unit = {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/51987654321?text=Hola%20DeSego%2C%20quisiera%20consultar%20sobre%20un%20pedido."))
            context.startActivity(intent)
        }.onFailure { message("No se pudo abrir WhatsApp.") }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Atención directa al cliente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("DeSego Huaycán · Lima Este", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    Text("¿Dudas con tu pedido, cobertura o reserva? Escríbenos directamente por WhatsApp y nuestro equipo te atenderá de inmediato.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Button(
                        onClick = openWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Contactar por WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item { Text("Preguntas frecuentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) }

        item {
            FaqItem("¿Cuál es la cobertura de delivery?", "Llegamos a todo Huaycán, zonas aledañas de Ate y carretera central. El costo estimado se confirma al seleccionar tu dirección.")
        }
        item {
            FaqItem("¿Qué métodos de pago aceptan?", "Aceptamos Efectivo contraentrega, Yape, Plin y pagos en línea o tarjeta al recibir.")
        }
        item {
            FaqItem("¿Cómo funcionan las reservas?", "Las reservas deben solicitarse con al menos 1 hora de anticipación. Te notificaremos la confirmación de la mesa en tu cuenta de la app.")
        }
        item {
            FaqItem("¿Cuál es el horario de atención?", "Atendemos de Lunes a Domingo de 11:30 AM a 5:30 PM para deleitarte con lo mejor del mar.")
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(question, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), color = Navy)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Muted)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(answer, fontSize = 12.sp, color = Muted, lineHeight = 18.sp)
            }
        }
    }
}

// ---------------- 8. MI PERFIL ----------------
@Composable
private fun ProfileSection(
    state: StoreState,
    scope: kotlinx.coroutines.CoroutineScope,
    message: (String) -> Unit,
    onAskLogout: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(state.name) }
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    var docType by rememberSaveable { mutableStateOf(state.documentType) }
    var docNumber by rememberSaveable { mutableStateOf(state.documentNumber) }
    var savingProfile by remember { mutableStateOf(false) }

    // Password change fields
    var curPass by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var confPass by rememberSaveable { mutableStateOf("") }
    var savingPass by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Datos personales", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                    OutlinedTextField(name, { name = it.take(80) }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(9) }, label = { Text("Teléfono celular (9 dígitos)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(docType, { docType = it }, label = { Text("Tipo Doc.") }, modifier = Modifier.weight(0.4f), singleLine = true, shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(docNumber, { docNumber = it.take(20) }, label = { Text("N° Documento (DNI)") }, modifier = Modifier.weight(0.6f), singleLine = true, shape = RoundedCornerShape(12.dp))
                    }

                    Button(
                        onClick = {
                            savingProfile = true
                            scope.launch {
                                runCatching { state.saveFullProfile(name, phone, docType, docNumber) }
                                    .onSuccess { message("Perfil actualizado correctamente.") }
                                    .onFailure { message(it.message ?: "No se pudo actualizar el perfil.") }
                                savingProfile = false
                            }
                        },
                        enabled = !savingProfile && name.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar datos personales", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Seguridad y contraseña", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                    OutlinedTextField(curPass, { curPass = it }, label = { Text("Contraseña actual") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(newPass, { newPass = it }, label = { Text("Nueva contraseña (mínimo 8 caracteres)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(confPass, { confPass = it }, label = { Text("Confirmar nueva contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

                    Button(
                        onClick = {
                            savingPass = true
                            scope.launch {
                                runCatching { state.changePassword(curPass, newPass, confPass) }
                                    .onSuccess {
                                        message("¡Contraseña actualizada con éxito!")
                                        curPass = ""; newPass = ""; confPass = ""
                                    }
                                    .onFailure { message(it.message ?: "Error al cambiar contraseña.") }
                                savingPass = false
                            }
                        },
                        enabled = !savingPass && curPass.isNotBlank() && newPass.length >= 8 && newPass == confPass,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Actualizar contraseña", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Ocean, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Notificaciones en este teléfono", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Recibe avisos sobre el estado de tus pedidos", fontSize = 11.sp, color = Muted)
                    }
                    Switch(checked = state.notifications, onCheckedChange = { state.notificationSetting(it) })
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onAskLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}
