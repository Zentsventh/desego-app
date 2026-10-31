package com.desego.iu.store

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.data.DiningApi
import com.desego.data.DiningSnapshot
import com.desego.data.DiningTablePreview
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val requestLabels = mapOf("waiter" to "Llamar al mesero", "bill" to "Pedir la cuenta", "help" to "Necesito ayuda")
private val requestIcons = mapOf("waiter" to Icons.Default.NotificationsActive, "bill" to Icons.Default.Receipt, "help" to Icons.Default.HelpOutline)

/**
 * Table experience: scan the QR, confirm the seat, order, call staff and pay.
 * Everything is decided by Laravel; this screen only reflects the latest server state.
 */
@Composable
fun DiningTable(user: String, message: (String) -> Unit, initialToken: String? = null) {
    val context = LocalContext.current
    val api = remember { DiningApi(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<DiningSnapshot?>(null) }
    var preview by remember { mutableStateOf<DiningTablePreview?>(null) }
    var qrToken by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun <T> run(action: suspend () -> T, done: (T) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { action() } }
                .onSuccess(done)
                .onFailure { message(it.message ?: "No se pudo completar la solicitud.") }
            busy = false
        }
    }

    fun openTable(raw: String) {
        val token = DiningApi.tokenFromScan(raw)
        if (token == null) { message("Este código no pertenece a una mesa de DESEGO."); return }
        qrToken = token
        run({ api.preview(token) }) { preview = it }
    }

    LaunchedEffect(initialToken) {
        if (api.hasSession) {
            runCatching { withContext(Dispatchers.IO) { api.state() } }.onSuccess { snapshot = it }
        } else if (!initialToken.isNullOrBlank()) {
            openTable(initialToken)
        }
    }
    LaunchedEffect(snapshot != null) {
        while (snapshot != null) {
            delay(8000)
            runCatching { withContext(Dispatchers.IO) { api.state() } }
                .onSuccess { snapshot = it }
                .onFailure { if (!api.hasSession) snapshot = null }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val current = snapshot
        if (current == null) {
            JoinTable(preview, busy, user, qrToken,
                scan = { scanQr(context, ::openTable) { message(it) } },
                manual = ::openTable,
                join = { name, code -> run({ api.join(qrToken, name, code) }) { snapshot = it; preview = null } })
        } else {
            TableSession(current, busy, api, message, scope,
                update = { snapshot = it },
                leave = { api.leave(); snapshot = null; preview = null })
        }
    }
}

private fun scanQr(context: android.content.Context, found: (String) -> Unit, failed: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(found) }
        .addOnFailureListener { failed("No pudimos abrir la cámara. Escribe el código de la mesa manualmente.") }
}

@Composable
private fun JoinTable(preview: DiningTablePreview?, busy: Boolean, user: String, qrToken: String,
    scan: () -> Unit, manual: (String) -> Unit, join: (String, String?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(user.substringBefore("@").replaceFirstChar { it.uppercase() }) }
    var code by rememberSaveable { mutableStateOf("") }
    var typed by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.QrCodeScanner, null, Modifier.size(56.dp), tint = Ocean)
            Text("Entra a tu mesa", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Navy)
            Text("Escanea el QR de tu mesa para pedir, llamar al mesero y pagar desde tu teléfono.", color = Muted, fontSize = 14.sp)
            Button(onClick = scan, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Escanear QR de la mesa", fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(typed, { typed = it.trim() }, Modifier.weight(1f), singleLine = true, label = { Text("Código de mesa") })
                OutlinedButton(onClick = { manual(typed) }, enabled = !busy && typed.isNotBlank()) { Text("Buscar") }
            }
        }
    }
    if (preview != null && qrToken.isNotBlank()) Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(preview.label, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy)
            when {
                preview.maintenance -> Text("Esta mesa está en mantenimiento. Pide ayuda al personal.", color = MaterialTheme.colorScheme.error)
                preview.occupied -> Text("La mesa ya tiene una visita abierta. Pide el código de seis dígitos a quien la abrió.", color = Muted)
                else -> Text("Mesa disponible. Al confirmar quedará marcada como ocupada.", color = Muted)
            }
            if (!preview.maintenance) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Tu nombre") })
                if (preview.occupied) OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Código de la mesa (6 dígitos)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(confirmed, { confirmed = it })
                    Text("Confirmo que estoy sentado en esta mesa", fontSize = 14.sp)
                }
                Button(onClick = { join(name, code.ifBlank { null }) },
                    enabled = !busy && confirmed && name.trim().length >= 2 && (!preview.occupied || code.length == 6),
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Entrar a la mesa", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TableSession(snapshot: DiningSnapshot, busy: Boolean, api: DiningApi, message: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope, update: (DiningSnapshot) -> Unit, leave: () -> Unit) {
    val context = LocalContext.current
    val cart = remember { mutableStateMapOf<Int, Int>() }
    var notes by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var payScope by remember { mutableStateOf<String?>(null) }

    fun act(block: suspend () -> DiningSnapshot, success: String? = null, done: () -> Unit = {}) {
        if (working) return
        working = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onSuccess { update(it); success?.let(message); done() }
                .onFailure { message(it.message ?: "No se pudo completar la solicitud.") }
            working = false
        }
    }

    val status = when (snapshot.visitStatus) {
        "open" -> "Mesa ocupada" to Mint; "checkout" -> "Cuenta solicitada" to Orange
        "paid" -> "Cuenta pagada" to Mint; else -> "Visita terminada" to Muted
    }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ocean)) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(snapshot.tableLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Surface(color = Color.White.copy(alpha = .16f), shape = RoundedCornerShape(50)) {
                Text(status.first, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            snapshot.joinCode?.let { Text("Código para tus acompañantes: $it", color = Color.White.copy(alpha = .9f), fontSize = 14.sp) }
        }
    }

    if (snapshot.isFinished) {
        EmptyState(Icons.Default.CheckCircle, if (snapshot.visitStatus == "paid") "¡Gracias por tu visita!" else "Visita finalizada",
            "El personal confirmará cuando la mesa vuelva a estar disponible.")
        OutlinedButton(onClick = leave, modifier = Modifier.fillMaxWidth()) { Text("Salir de la mesa") }
        return
    }

    SectionHeader("Atención")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        requestLabels.forEach { (kind, label) ->
            val active = snapshot.requests.firstOrNull { it.kind == kind && it.status in listOf("pending", "acknowledged") }
            OutlinedButton(onClick = { act({ api.attention(kind) }, "Avisamos al personal.") },
                enabled = !working && !busy && active == null, modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp),
                shape = RoundedCornerShape(14.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(requestIcons.getValue(kind), null, Modifier.size(22.dp))
                    Text(if (active?.status == "acknowledged") "En camino" else if (active != null) "Enviado" else label,
                        fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    SectionHeader("Pedir a la mesa")
    if (!snapshot.isOpen) Text("La cuenta está en cierre. Pide al mesero que la reabra para agregar platos.", color = Muted, fontSize = 13.sp)
    demoProducts.filter { it.available }.forEach { product ->
        val quantity = cart[product.id] ?: 0
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodImage(product, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(money(product.price), color = Ocean, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            IconButton(onClick = { if (quantity > 0) cart[product.id] = quantity - 1 }, enabled = quantity > 0) { Icon(Icons.Default.RemoveCircleOutline, "Quitar") }
            Text(quantity.toString(), fontWeight = FontWeight.Bold)
            IconButton(onClick = { cart[product.id] = (quantity + 1).coerceAtMost(20) }, enabled = snapshot.isOpen) { Icon(Icons.Default.AddCircleOutline, "Agregar") }
        }
    }
    val cartTotal = demoProducts.sumOf { it.price * (cart[it.id] ?: 0) }
    if (cartTotal > 0) {
        OutlinedTextField(notes, { notes = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Preferencias para cocina (opcional)") })
        Button(onClick = { act({ api.order(cart.toMap(), notes) }, "Pedido enviado a cocina.") { cart.clear(); notes = "" } },
            enabled = !working && !busy && snapshot.isOpen, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Enviar a cocina · ${money(cartTotal)}", fontWeight = FontWeight.Bold)
        }
    }

    SectionHeader("Pedidos de la mesa")
    if (snapshot.orders.isEmpty()) Text("Todavía no hay pedidos en esta mesa.", color = Muted, fontSize = 14.sp)
    snapshot.orders.forEach { order ->
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number.ifBlank { "Pedido ${order.id}" } + if (order.mine) " · tuyo" else "", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    AssistChip(onClick = {}, label = { Text(order.status, fontSize = 12.sp) })
                }
                order.items.forEach { Text("${it.quantity} × ${it.name}", fontSize = 14.sp, color = Muted) }
                Text(money(order.totalCents) + if (order.paymentStatus == "Pagado") " · Pagado" else "", fontWeight = FontWeight.SemiBold, color = Ocean)
            }
        }
    }

    SectionHeader("Cuenta y Cobro sin Comisión")
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Total pendiente de la mesa: ${money(snapshot.balanceCents)}", fontWeight = FontWeight.Bold)
            Text("Tu consumo pendiente: ${money(snapshot.myBalanceCents)}", color = Muted)
            if (snapshot.balanceCents > 0) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Cobro directo en mesa · 0% comisión", fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                Text("Yape / Plin: ${snapshot.yapeNumber} (${snapshot.paymentHolder})", color = Ocean, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { act({ api.attention("bill", "Pago con Yape/Plin en mesa") }, "Avisamos al mesero para verificar tu Yape/Plin.") },
                    enabled = !working && !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ya yapeé / Pagar con Yape o Plin") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { act({ api.attention("bill", "Solicito POS físico (Tarjeta en mesa)") }, "El mesero llevará el POS físico a tu mesa.") },
                        enabled = !working && !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("POS Físico", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { act({ api.attention("bill", "Pago en Efectivo en mesa") }, "El mesero se acercará a cobrar en efectivo.") },
                        enabled = !working && !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Efectivo", fontSize = 12.sp) }
                }
            }
            if (snapshot.onlinePayment && snapshot.balanceCents > 0) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Pasarela online (Niubiz Sandbox Demo)", fontWeight = FontWeight.SemiBold, color = Muted, fontSize = 12.sp)
                Button(onClick = { payScope = "mine" }, enabled = snapshot.myBalanceCents > 0 && !working, modifier = Modifier.fillMaxWidth()) { Text("Pagar mi consumo (Niubiz)") }
                OutlinedButton(onClick = { payScope = "all" }, enabled = snapshot.balanceCents > 0 && !working, modifier = Modifier.fillMaxWidth()) { Text("Pagar toda la mesa (Niubiz)") }
            }
        }
    }

    payScope?.let { selected ->
        PaymentDialog(dismiss = { payScope = null }) { email, phone, address, city, postal ->
            payScope = null
            if (working) return@PaymentDialog
            working = true
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { api.startPayment(selected, email, phone, address, city, postal) } }
                    .onSuccess { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    .onFailure { message(it.message ?: "No pudimos iniciar el pago.") }
                working = false
            }
        }
    }
}

@Composable
private fun PaymentDialog(dismiss: () -> Unit, confirm: (String, String, String, String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("Lima") }
    var postal by rememberSaveable { mutableStateOf("") }
    val valid = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim()) && Regex("^[0-9+ ]{7,15}$").matches(phone.trim()) &&
        address.isNotBlank() && city.isNotBlank() && Regex("^[0-9]{5,9}$").matches(postal.trim())
    AlertDialog(onDismissRequest = dismiss, title = { Text("Datos para el pago seguro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, singleLine = true, label = { Text("Correo") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, singleLine = true, label = { Text("Teléfono") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = address, onValueChange = { address = it.take(60) }, singleLine = true, label = { Text("Dirección") })
                OutlinedTextField(value = city, onValueChange = { city = it.take(50) }, singleLine = true, label = { Text("Ciudad") })
                OutlinedTextField(value = postal, onValueChange = { postal = it.filter(Char::isDigit).take(9) }, singleLine = true, label = { Text("Código postal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(onClick = { confirm(email, phone, address, city, postal) }, enabled = valid) { Text("Continuar al pago") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } })
}

@Composable
private fun SectionHeader(text: String) = Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Navy, modifier = Modifier.padding(top = 6.dp))
