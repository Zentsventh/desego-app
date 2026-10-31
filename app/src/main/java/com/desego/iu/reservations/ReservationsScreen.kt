package com.desego.iu.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.iu.components.BlueScreen
import com.desego.iu.components.TablesOverview
import com.desego.iu.theme.DesegoUi

@Composable
fun ReservationsSection(
    padding: PaddingValues,
    onAction: (String) -> Unit
) {
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
                    text = "Reservas y mesas",
                    color = DesegoUi.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Disponibilidad y plano del restaurante",
                    color = DesegoUi.White.copy(alpha = .76f),
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(7.dp))
            }

            item {
                Button(
                    onClick = {
                        onAction("Nueva reserva")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesegoUi.Cyan,
                        contentColor = DesegoUi.Blue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Crear reserva",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                TablesOverview(
                    onClick = {
                        onAction("Plano visual")
                    }
                )
            }

            items(
                listOf(
                    "Hoy 13:00 • Mesa 05 • 4 personas",
                    "Hoy 14:30 • Mesa 12 • 2 personas",
                    "Mañana 20:00 • Mesa 08 • 6 personas"
                )
            ) { item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAction(item)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = DesegoUi.White
                    ),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, DesegoUi.Border),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = DesegoUi.Blue
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = item,
                            modifier = Modifier.weight(1f),
                            color = DesegoUi.TextDark,
                            fontSize = 11.sp
                        )

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DesegoUi.TextBlue
                        )
                    }
                }
            }
        }
    }
}
