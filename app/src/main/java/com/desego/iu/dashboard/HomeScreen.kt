package com.desego.iu.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.data.DatabaseApi
import com.desego.data.RemoteStats
import com.desego.iu.components.BlueScreen
import com.desego.iu.components.DatabaseConnectionStatus
import com.desego.iu.components.TablesOverview
import com.desego.iu.theme.DesegoUi

@Composable
fun HomeSection(
    padding: PaddingValues,
    onAction: (String) -> Unit
) {
    var remoteStats by remember { mutableStateOf<RemoteStats?>(null) }
    LaunchedEffect(Unit) {
        remoteStats = DatabaseApi.fetchStats()
    }

    val kpis = listOf(
        KpiUi(
            "Ventas Hoy",
            remoteStats?.let { "S/ %.2f".format(it.salesToday) } ?: "S/ 1,248.50",
            if (remoteStats != null) "Sincronizado con servidor" else "+12.5% vs ayer",
            Icons.Default.TrendingUp,
            DesegoUi.Green
        ),
        KpiUi(
            "Pedidos",
            remoteStats?.ordersToday?.toString() ?: "42",
            remoteStats?.let { "${it.pendingOrders} pendientes" } ?: "5 pendientes",
            Icons.Default.AssignmentTurnedIn,
            DesegoUi.Blue
        ),
        KpiUi("Ticket Promedio", "S/ 32.80", "-2.1% vs ayer", Icons.Default.ShowChart, DesegoUi.Red, false)
    )

    val quickActions = listOf(
        QuickActionUi("Nueva Venta", "Caja rápida", Icons.Default.PointOfSale, DesegoUi.Cyan),
        QuickActionUi("Reserva rápida", "Mesa directa", Icons.Default.AssignmentTurnedIn, DesegoUi.Purple)
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
                    text = "Panel General",
                    color = DesegoUi.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Bienvenido de nuevo, Juan",
                    color = DesegoUi.White.copy(alpha = .76f),
                    fontSize = 11.sp
                )
                DatabaseConnectionStatus()
                Spacer(Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickActions.forEach { action ->
                        QuickActionCard(
                            action = action,
                            modifier = Modifier.weight(1f),
                            onClick = { onAction(action.title) }
                        )
                    }
                }
            }

            items(kpis) { kpi ->
                KpiCard(kpi)
            }

            item {
                TablesOverview(
                    onClick = { onAction("Ver Mesas") }
                )
            }
        }
    }
}

@Composable
fun KpiCard(kpi: KpiUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DesegoUi.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(kpi.accent.copy(alpha = .1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(kpi.icon, null, tint = kpi.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(kpi.title, color = DesegoUi.TextBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(kpi.value, color = DesegoUi.TextDark, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Text(
                kpi.detail,
                color = if (kpi.positive) DesegoUi.Green else DesegoUi.Red,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickActionCard(
    action: QuickActionUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = action.accent.copy(alpha = .12f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(action.icon, null, tint = action.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(action.title, color = DesegoUi.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(action.subtitle, color = DesegoUi.White.copy(alpha = .7f), fontSize = 8.sp)
        }
    }
}
