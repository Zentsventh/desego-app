package com.desego.iu.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.iu.components.BlueScreen
import com.desego.iu.theme.DesegoUi

@Composable
fun MenuSection(
    padding: PaddingValues,
    onAction: (String) -> Unit
) {
    val categories = listOf(
        "Todos",
        "Entradas",
        "Platos",
        "Bebidas",
        "Postres"
    )

    val products = listOf(
        "Ceviche Desego" to "S/ 24.90",
        "Ceviche Clásico" to "S/ 26.00",
        "Pescado Frito" to "S/ 29.00",
        "Arroz con Mariscos" to "S/ 31.00",
        "Chicha Morada" to "S/ 8.00",
        "Torta de Chocolate" to "S/ 12.50"
    )

    var selected by remember {
        mutableIntStateOf(0)
    }

    BlueScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Menú DESEGO",
                            color = DesegoUi.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "Productos y disponibilidad",
                            color = DesegoUi.White.copy(alpha = .76f),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.clip(
                            RoundedCornerShape(13.dp)
                        ),
                        color = DesegoUi.White.copy(alpha = .13f)
                    ) {
                        IconButton(
                            onClick = {
                                onAction("Buscar producto")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = DesegoUi.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEachIndexed { index, item ->
                        val active = index == selected
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selected = index
                                },
                            color = if (active)
                                DesegoUi.Cyan
                            else
                                DesegoUi.White.copy(alpha = .13f)
                        ) {
                            Text(
                                text = item,
                                color = if (active)
                                    DesegoUi.Blue
                                else
                                    DesegoUi.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
                            )
                        }
                    }
                }
            }

            items(products) { (name, price) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAction(name)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = DesegoUi.White
                    ),
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, DesegoUi.Border),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(DesegoUi.CyanSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🐟",
                                fontSize = 27.sp
                            )
                        }

                        Spacer(Modifier.width(11.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = name,
                                color = DesegoUi.TextDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "Disponible • 4.8 ★",
                                color = DesegoUi.TextBlue,
                                fontSize = 9.sp
                            )

                            Text(
                                text = "Alérgenos visibles",
                                color = DesegoUi.TextBlue.copy(alpha = .72f),
                                fontSize = 8.sp
                            )
                        }

                        Text(
                            text = price,
                            color = DesegoUi.Blue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
