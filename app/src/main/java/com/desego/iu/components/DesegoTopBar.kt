package com.desego.iu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.iu.theme.DesegoUi
import com.desego.ui.theme.DesegoColors

@Composable
fun DesegoTopBar(
    isOffline: Boolean,
    onToggleConnection: () -> Unit,
    onNotifications: () -> Unit
) {
    Surface(
        color = DesegoColors.RappiRed,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DesegoColors.NeutralWhite),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "D",
                        color = DesegoColors.RappiRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 23.sp,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "DESEGO",
                        color = DesegoColors.RappiRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 6.sp
                    )
                }
            }

            Spacer(Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "DESEGO CEVICHERÍA",
                    color = DesegoUi.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = .2.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOffline) DesegoUi.Red
                                else DesegoUi.Cyan
                            )
                    )

                    Text(
                        text = if (isOffline) "Sin conexión" else "Administración • Online",
                        color = DesegoUi.White.copy(alpha = .82f),
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleConnection() },
                color = DesegoUi.White.copy(alpha = .13f)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = "Estado de conexión",
                    tint = DesegoUi.White,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(19.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            BadgedBox(
                badge = {
                    Badge(
                        containerColor = DesegoUi.Cyan,
                        contentColor = DesegoUi.Blue
                    ) {
                        Text("4", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                IconButton(onClick = onNotifications) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notificaciones",
                        tint = DesegoUi.White
                    )
                }
            }
        }
    }
}
