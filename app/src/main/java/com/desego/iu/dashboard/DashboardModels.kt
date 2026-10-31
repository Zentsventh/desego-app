package com.desego.iu.dashboard

import androidx.compose.ui.graphics.vector.ImageVector


enum class DashboardTab(val label: String) {
    HOME("Inicio"),
    ORDERS("Pedidos"),
    MENU("Menú"),
    RESERVATIONS("Reservas"),
    PROFILE("Perfil"),
}

data class KpiUi(
    val title: String,
    val value: String,
    val detail: String,
    val icon: ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val positive: Boolean = true,
)

data class QuickActionUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
)

data class OrderUi(
    val code: String,
    val customer: String,
    val description: String,
    val total: String,
    val status: String,
    val accent: androidx.compose.ui.graphics.Color,
)
