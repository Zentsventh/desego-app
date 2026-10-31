package com.desego.iu.orders

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
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.iu.components.BlueScreen
import com.desego.iu.dashboard.OrderUi
import com.desego.iu.theme.DesegoUi

@Composable
fun OrdersSection(
    padding: PaddingValues,
    onAction: (String) -> Unit
) {
    val filters = listOf(
        "Todos",
        "Pendientes",
        "Preparando",
        "Listos",
        "Delivery"
    )

    var selected by remember {
        mutableIntStateOf(0)
    }

    val orders = listOf(
        OrderUi(
            "#DES-1048",
            "Mesa 08",
            "Lomo saltado + Inca Kola",
            "S/ 48.00",
            "Preparando",
            DesegoUi.Blue
        ),
        OrderUi(
            "#DES-1047",
            "Delivery",
            "2 hamburguesas + papas",
            "S/ 64.50",
            "En reparto",
            DesegoUi.Green
        ),
        OrderUi(
            "#DES-1046",
            "Mesa 15",
            "Ceviche clásico + chicha",
            "S/ 42.00",
            "Listo",
            DesegoUi.Cyan
        ),
        OrderUi(
            "#DES-1045",
            "Para llevar",
            "Pollo a la brasa",
            "S/ 38.00",
            "Pendiente",
            DesegoUi.Red
        )
    )

    BlueScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Text(
                    text = "Centro de pedidos",
                    color = DesegoUi.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Estado y seguimiento de tus pedidos",
                    color = DesegoUi.White.copy(alpha = .76f),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    filters.forEachIndexed { index, item ->

                        val active = selected == index

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selected = index
                                },
                            color = if (active)
                                DesegoUi.Cyan
                            else
                                DesegoUi.White.copy(alpha = .13f),
                            border = if (active)
                                null
                            else
                                BorderStroke(
                                    1.dp,
                                    DesegoUi.White.copy(alpha = .18f)
                                )
                        ) {
                            Text(
                                text = item,
                                color = if (active)
                                    DesegoUi.Blue
                                else
                                    DesegoUi.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
                            )
                        }
                    }
                }
            }

            items(orders) { order ->
                OrderCard(order) {
                    onAction("Abrir ${order.code}")
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: OrderUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DesegoUi.White
        ),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, DesegoUi.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {

        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(order.accent.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = null,
                    tint = order.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = order.code,
                    color = DesegoUi.TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = order.customer,
                    color = DesegoUi.TextBlue,
                    fontSize = 10.sp
                )

                Text(
                    text = order.description,
                    color = DesegoUi.TextBlue.copy(alpha = .85f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = order.total,
                    color = DesegoUi.TextDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )

                Surface(
                    color = order.accent.copy(alpha = .10f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status,
                        color = order.accent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        )
                    )
                }
            }
        }
    }
}
