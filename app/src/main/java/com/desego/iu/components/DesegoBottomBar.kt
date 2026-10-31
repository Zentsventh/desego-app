package com.desego.iu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.iu.dashboard.DashboardTab
import com.desego.iu.theme.DesegoUi

@Composable
fun DesegoBottomBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Surface(
        color = DesegoUi.White,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        NavigationBar(
            containerColor = Color.Transparent
        ) {
            val tabs = listOf(
                DashboardTab.HOME to Icons.Default.Home,
                DashboardTab.ORDERS to Icons.Default.ReceiptLong,
                DashboardTab.MENU to Icons.Default.MenuBook,
                DashboardTab.RESERVATIONS to Icons.Default.EventAvailable,
                DashboardTab.PROFILE to Icons.Default.Person
            )

            tabs.forEach { (tab, icon) ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (selectedTab == tab)
                                        DesegoUi.CyanSoft
                                    else
                                        Color.Transparent
                                )
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                tint = if (selectedTab == tab)
                                    DesegoUi.Blue
                                else
                                    DesegoUi.TextBlue,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontSize = 9.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}
