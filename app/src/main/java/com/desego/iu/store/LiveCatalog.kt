package com.desego.iu.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun StoreHome(state: StoreState, delivery: Boolean, mode: (Boolean)->Unit, query: String,
    setQuery: (String)->Unit, category: (String)->Unit, open: (StoreProduct)->Unit,
    add: (StoreProduct)->Unit, reserve: ()->Unit) {
    val motion = rememberInfiniteTransition(label = "Oleaje")
    val drift by motion.animateFloat(initialValue = 0f, targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Plato")
    LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(360.dp).clip(RoundedCornerShape(24.dp))) {
                Image(painterResource(R.drawable.pacific_coast),"Costa del Pacífico",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xCF022F42),Color.Transparent))))
                Image(painterResource(R.drawable.logo), "Desego Cevichería", Modifier.align(Alignment.TopEnd).padding(20.dp).width(105.dp).height(40.dp))
                Column(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 36.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text("EL ORIGEN DEL ANTOJO",color=Color.White,fontSize=8.sp,letterSpacing=1.sp)
                    Text("PURO\nMAR.",fontSize=62.sp,lineHeight=57.sp,color=Color.White,fontWeight=FontWeight.ExtraBold,letterSpacing=(-3).sp)
                }
                Image(painterResource(R.drawable.ceviche_coastal), "Ceviche peruano servido en un plato tendido", Modifier.align(Alignment.BottomEnd).offset(x = 24.dp, y = (-35 + drift).dp).width(290.dp).height(210.dp), contentScale = ContentScale.Fit)
                Button(onClick={category("Todo")},modifier=Modifier.align(Alignment.BottomStart).padding(20.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Ocean),shape=RoundedCornerShape(24.dp)) { Text("Ver carta");Spacer(Modifier.width(8.dp));Icon(Icons.Default.ArrowOutward,null,Modifier.size(16.dp)) }
            }
        }
        item { Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
            DeliveryChoice(delivery,mode)
            CatalogConnection(state)
            OutlinedTextField(query,setQuery,placeholder={Text("¿Qué se te antoja hoy?")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(9.dp))
            SectionTitle("Tu próximo antojo","Ver carta") { category("Todo") }
        } }
        item { LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("Todo")+demoProducts.map { it.category }.distinct()) { name -> AssistChip(onClick={category(name)},label={Text(name)}) }
        } }
        items(demoProducts.filter { query.isBlank()||it.name.contains(query,true)||it.description.contains(query,true) },key={it.id}) { product ->
            ProductRow(product,state,{open(product)},{add(product)})
        }
        if (demoProducts.isEmpty()&&!state.loading) item { EmptyState(Icons.Default.RestaurantMenu,"Nuestra carta se está actualizando",state.connectionError?:"Vuelve a consultar en unos momentos.") }
        item { Surface(onClick=reserve,color=Ocean,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp)) {
            Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Tu mesa. Tu momento.",color=Color.White,fontSize=24.sp,fontFamily=FontFamily.Serif);Text("Solicita tu reserva en DESEGO",color=Color(0xFFD8E0CC),fontSize=11.sp,modifier=Modifier.padding(top=6.dp)) };Icon(Icons.Default.ArrowOutward,null,tint=Color(0xFFE3AD64)) }
        } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoreCatalog(state: StoreState, favoritesOnly: Boolean, query: String, setQuery: (String)->Unit,
    category: String, setCategory: (String)->Unit, open: (StoreProduct)->Unit, add: (StoreProduct)->Unit) {
    val products=demoProducts.filter { (!favoritesOnly||it.id in state.favorites)&&(category=="Todo"||it.category==category)&&(query.isBlank()||it.name.contains(query,true)||it.description.contains(query,true)) }
    LazyColumn(contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
        item { Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
            Text(if(favoritesOnly)"Tus buenos antojos." else "La carta de DESEGO",fontSize=30.sp,fontFamily=FontFamily.Serif,color=Navy)
            CatalogConnection(state)
            if(!favoritesOnly)OutlinedTextField(query,setQuery,placeholder={Text("Buscar platos e ingredientes")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
        } }
        if(!favoritesOnly)stickyHeader { LazyRow(modifier=Modifier.fillMaxWidth().background(Sand),contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) { items(listOf("Todo")+demoProducts.map { it.category }.distinct()) { name->FilterChip(selected=category==name,onClick={setCategory(name)},label={Text(name)}) } } }
        items(products,key={it.id}) { product->ProductRow(product,state,{open(product)},{add(product)}) }
        if(products.isEmpty()&&!state.loading)item { EmptyState(if(favoritesOnly)Icons.Default.FavoriteBorder else Icons.Default.Search,"Busquemos otro antojo.",if(favoritesOnly)"Marca tus platos favoritos con un corazón." else "No encontramos platos con esos filtros.") }
    }
}

@Composable
private fun CatalogConnection(state: StoreState) {
    if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
    state.connectionError?.let { message->Surface(color=Color(0xFFFFF0DA),shape=RoundedCornerShape(6.dp)) { Text("Sin actualizar: $message\nPuedes consultar la última carta guardada. Para confirmar pedidos necesitas conexión.",Modifier.padding(12.dp),fontSize=10.sp,color=Color(0xFF856B3E)) } }
    if (state.pendingChanges > 0) Text("${state.pendingChanges} cambios pendientes · sincronización automática", color=Ocean, fontSize=12.sp)
    state.syncIssue?.let { Text(it, color=MaterialTheme.colorScheme.error, fontSize=12.sp) }
}

@Composable
private fun ProductRow(product: StoreProduct,state: StoreState,open: ()->Unit,add: ()->Unit) {
    Card(onClick=open,modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            FoodImage(product,Modifier.size(112.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(product.category.uppercase(),fontSize=8.sp,letterSpacing=1.sp,color=Muted)
                Text(product.name,fontFamily=FontFamily.Serif,fontSize=22.sp,lineHeight=24.sp,color=Navy)
                Text(product.description,maxLines=2,overflow=TextOverflow.Ellipsis,fontSize=10.sp,color=Muted,lineHeight=15.sp)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(money(product.price),fontSize=14.sp,fontWeight=FontWeight.Bold,color=Ocean,modifier=Modifier.weight(1f))
                    IconButton(onClick={state.favorite(product.id)},modifier=Modifier.size(30.dp)) { Icon(if(product.id in state.favorites)Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Favorito",tint=Ocean,modifier=Modifier.size(18.dp)) }
                    FilledIconButton(onClick=add,enabled=product.available,modifier=Modifier.size(31.dp)) { Icon(Icons.Default.Add,"Agregar",Modifier.size(18.dp)) }
                }
                if(!product.available)Text("No disponible por el momento",fontSize=9.sp,color=Muted)
            }
        }
    }
}
