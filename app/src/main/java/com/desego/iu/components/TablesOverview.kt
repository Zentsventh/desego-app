package com.desego.iu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desego.iu.theme.DesegoUi

@Composable
fun TablesOverview(
    onClick: () -> Unit
) {
    val states = listOf(
        "Disponible" to 6,
        "Ocupada" to 12,
        "Reservada" to 4,
        "Mantenimiento" to 2
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DesegoUi.White
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DesegoUi.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DesegoUi.Blue.copy(alpha = .10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableRestaurant,
                        contentDescription = null,
                        tint = DesegoUi.Blue
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Salón principal",
                        color = DesegoUi.TextDark,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "24 mesas registradas",
                        color = DesegoUi.TextBlue,
                        fontSize = 10.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = DesegoUi.TextBlue
                )
            }

            states.forEach { (label, count) ->
                val color = when (label) {
                    "Disponible" -> DesegoUi.Green
                    "Ocupada" -> DesegoUi.Blue
                    "Reservada" -> DesegoUi.Purple
                    else -> DesegoUi.Red
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color)
                    )

                    Spacer(Modifier.width(9.dp))

                    Text(
                        text = label,
                        color = DesegoUi.TextBlue,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = count.toString(),
                        color = DesegoUi.TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            LinearProgressIndicator(
                progress = { .75f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = DesegoUi.Cyan,
                trackColor = Color(0xFFEAF0F8)
            )

            Text(
                text = "75% de ocupación actual",
                color = DesegoUi.TextBlue,
                fontSize = 9.sp
            )
        }
    }
}
