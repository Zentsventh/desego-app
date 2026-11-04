package com.desego.iu.store

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.R
import com.desego.ui.theme.*

@Composable
fun StoreHome(
    state: StoreState,
    delivery: Boolean,
    mode: (Boolean) -> Unit,
    query: String,
    setQuery: (String) -> Unit,
    category: (String) -> Unit,
    open: (StoreProduct) -> Unit,
    add: (StoreProduct) -> Unit,
    reserve: () -> Unit
) {
    val motion = rememberInfiniteTransition(label = "Oleaje")
    val drift by motion.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Plato"
    )

    val categories = remember(demoProducts.size) {
        listOf("Todo") + demoProducts.map { it.category }.distinct()
    }

    val chefSpecials = remember(demoProducts.size) {
        demoProducts.filter { it.available }.take(5)
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Hero Banner Cinematográfico Gastronómico
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(290.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Imagen de costa marina de fondo
                    Image(
                        painterResource(R.drawable.pacific_coast),
                        "Costa del Pacífico",
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Degradado multicapa oscuro de alta legibilidad
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xF5061224),
                                        Color(0xEE092542),
                                        Color(0x55000000)
                                    )
                                )
                            )
                    )

                    // Emblema circular blanco para el Isotipo Logo oficial
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(44.dp)
                    ) {
                        Image(
                            painterResource(R.drawable.logo),
                            "Desego Cevichería",
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Textos del Hero
                    Column(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 20.dp, top = 20.dp, end = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "🌊 PESCA DEL DÍA · HUAYCÁN",
                                color = Color(0xFFFEF08A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            "Pasión por el mar en cada plato.",
                            fontSize = 23.sp,
                            lineHeight = 27.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Ceviches tradicionales, tiraditos frescos y mariscos preparados al instante.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFFF1F5F9),
                            maxLines = 2
                        )
                    }

                    // Plato tendido de ceviche flotando con microanimación
                    Image(
                        painterResource(R.drawable.ceviche_coastal),
                        "Ceviche peruano servido",
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 15.dp, y = (-8 + drift).dp)
                            .width(215.dp)
                            .height(160.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Botón de acción principal
                    Button(
                        onClick = { category("Todo") },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = 20.dp)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = OceanBlue
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("Ver carta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowOutward, null, Modifier.size(15.dp))
                    }
                }
            }
        }

        // 2. Selector de Modalidad Segmentado (Delivery vs Recojo) & Buscador Cápsula
        item {
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Segmented Pill Control
                DeliverySegmentedControl(delivery = delivery, onSelect = mode)

                // Mensaje sutil de entrega o recojo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        if (delivery) Icons.Default.LocationOn else Icons.Default.Storefront,
                        null,
                        tint = OceanBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (delivery) {
                            val addr = state.addresses.firstOrNull { it.isDefault } ?: state.addresses.firstOrNull()
                            if (addr != null) "Entregar en: ${addr.label} (${addr.street})" else "Delivery a domicilio en Huaycán y Ate"
                        } else "Recojo en salón: DeSego Huaycán (Local Principal)",
                        fontSize = 12.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Buscador estilo cápsula premium
                CapsuleSearchBar(query = query, onQueryChange = setQuery)

                // Estado de sincronización
                CatalogConnection(state)
            }
        }

        // 3. Recomendados del Chef (Horizontal Carousel) - solo visible si no hay búsqueda
        if (query.isBlank() && chefSpecials.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Especialidades del Chef",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            "Favoritos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OceanBlue
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chefSpecials, key = { "special-${it.id}" }) { product ->
                            ChefSpecialCard(
                                product = product,
                                state = state,
                                onOpen = { open(product) },
                                onAdd = { add(product) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Barra de Categorías Píldora
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Explora nuestra carta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { catName ->
                        CategoryPill(
                            name = catName,
                            isSelected = false,
                            onClick = { category(catName) }
                        )
                    }
                }
            }
        }

        // 5. Lista de Platos en Tarjetas Elegantes
        val filtered = demoProducts.filter {
            query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true)
        }

        items(filtered, key = { it.id }) { product ->
            PremiumProductCard(
                product = product,
                state = state,
                onOpen = { open(product) },
                onAdd = { add(product) }
            )
        }

        if (demoProducts.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    Icons.Default.RestaurantMenu,
                    "Nuestra carta se está actualizando",
                    state.connectionError ?: "Vuelve a consultar en unos instantes."
                )
            }
        }

        // 6. Banner Premium de Reservas de Mesa
        item {
            Card(
                onClick = reserve,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF004797))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x33F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TableRestaurant,
                                null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                "Tu mesa en DeSego",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                "Reserva tu momento gastronómico en salón Huaycán",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Icon(Icons.Default.ArrowOutward, null, tint = Color(0xFFF59E0B))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoreCatalog(
    state: StoreState,
    favoritesOnly: Boolean,
    query: String,
    setQuery: (String) -> Unit,
    category: String,
    setCategory: (String) -> Unit,
    open: (StoreProduct) -> Unit,
    add: (StoreProduct) -> Unit
) {
    val categories = remember(demoProducts.size) {
        listOf("Todo") + demoProducts.map { it.category }.distinct()
    }

    val products = demoProducts.filter {
        (!favoritesOnly || it.id in state.favorites) &&
        (category == "Todo" || it.category == category) &&
        (query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true))
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (favoritesOnly) "Tus Platos Favoritos" else "La Carta DeSego",
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Text(
                    if (favoritesOnly) "Los platos marinos que te han conquistado, listos para volver a pedir."
                    else "Pescados, mariscos, arroces y ceviches preparados con la pesca más fresca.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                CatalogConnection(state)

                CapsuleSearchBar(
                    query = query,
                    onQueryChange = setQuery,
                    placeholder = if (favoritesOnly) "Buscar en tus favoritos..." else "Buscar platos e ingredientes..."
                )
            }
        }

        if (!favoritesOnly) {
            stickyHeader {
                Surface(
                    color = BackgroundSand,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { catName ->
                            CategoryPill(
                                name = catName,
                                isSelected = category == catName,
                                onClick = { setCategory(catName) }
                            )
                        }
                    }
                }
            }
        }

        items(products, key = { it.id }) { product ->
            PremiumProductCard(
                product = product,
                state = state,
                onOpen = { open(product) },
                onAdd = { add(product) }
            )
        }

        if (products.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    if (favoritesOnly) Icons.Default.FavoriteBorder else Icons.Default.Search,
                    if (favoritesOnly) "Aún no tienes favoritos" else "No encontramos platos",
                    if (favoritesOnly) "Toca el corazón en cualquier plato de la carta para guardarlo aquí."
                    else "Prueba buscando con otro ingrediente o categoría."
                )
            }
        }
    }
}

// ---------------- UI COMPONENTS ----------------

@Composable
private fun DeliverySegmentedControl(
    delivery: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            // Delivery
            Surface(
                onClick = { onSelect(true) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = if (delivery) OceanBlue else Color.Transparent,
                shadowElevation = if (delivery) 2.dp else 0.dp
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.DeliveryDining,
                        null,
                        tint = if (delivery) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Delivery",
                        fontSize = 13.sp,
                        fontWeight = if (delivery) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (delivery) Color.White else Color(0xFF334155)
                    )
                }
            }

            // Recojo en local
            Surface(
                onClick = { onSelect(false) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = if (!delivery) OceanBlue else Color.Transparent,
                shadowElevation = if (!delivery) 2.dp else 0.dp
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        null,
                        tint = if (!delivery) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Recojo en local",
                        fontSize = 13.sp,
                        fontWeight = if (!delivery) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (!delivery) Color.White else Color(0xFF334155)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "¿Qué antojo marino buscas hoy?"
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = OceanBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(placeholder, color = Color(0xFF64748B), fontSize = 13.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, color = TextDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, "Limpiar", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val emoji = when (name.lowercase()) {
        "todo" -> "🌊"
        "ceviches", "ceviche" -> "🐟"
        "tiraditos" -> "🥢"
        "arroces", "arroz" -> "🍚"
        "chicharrones", "frituras" -> "🍤"
        "bebidas", "chicha" -> "🍹"
        "postres" -> "🍰"
        else -> "🍽️"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) OceanBlue else Color.White,
        border = BorderStroke(1.dp, if (isSelected) OceanBlue else Color(0xFFE2E8F0)),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun ChefSpecialCard(
    product: StoreProduct,
    state: StoreState,
    onOpen: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier
            .width(175.dp)
            .height(215.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(108.dp)) {
                FoodImage(product, Modifier.fillMaxSize())
                Surface(
                    color = Color(0xF00F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
                ) {
                    Text(
                        "★ Recomendado",
                        color = Color(0xFFFEF08A),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        product.name,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = NavyPrimary
                    )
                    Text(
                        product.description,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(money(product.price), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = OceanBlue)
                    FilledIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = OceanBlue)
                    ) {
                        Icon(Icons.Default.Add, "Agregar", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumProductCard(
    product: StoreProduct,
    state: StoreState,
    onOpen: () -> Unit,
    onAdd: () -> Unit
) {
    val isFav = product.id in state.favorites

    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Image with smooth rounded corners
            Box(Modifier.size(100.dp)) {
                FoodImage(
                    product,
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                )
                if (!product.available) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Agotado",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info column
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    product.category.uppercase(),
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = SeafoamAccent,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    product.name,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Text(
                    product.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )

                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        money(product.price),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OceanBlue,
                        modifier = Modifier.weight(1f)
                    )

                    // Favorite button
                    IconButton(
                        onClick = { state.favorite(product.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorito",
                            tint = if (isFav) CoralFavorite else TextSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Add button
                    FilledIconButton(
                        onClick = onAdd,
                        enabled = product.available,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = OceanBlue)
                    ) {
                        Icon(Icons.Default.Add, "Agregar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogConnection(state: StoreState) {
    if (state.loading) {
        LinearProgressIndicator(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
            color = OceanBlue
        )
    }
    state.connectionError?.let { message ->
        Surface(
            color = Color(0xFFFEF3C7),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WifiOff, null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Modo offline: Consultando carta guardada.",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E)
                )
            }
        }
    }
    if (state.pendingChanges > 0) {
        Text(
            "${state.pendingChanges} cambios pendientes · sincronización automática",
            color = OceanBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
