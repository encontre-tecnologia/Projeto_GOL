package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AbastecimentoCard(
    proximaData: LocalDate?,
    diasAte: Long?,
    custoDia: Double?,
    custoSemana: Double?,
    custoMes: Double?,
    dateFormatter: DateTimeFormatter,
    onHistorico: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textLight = Color(0xFFF1F5F9)
    val textDim = Color(0xFF94A3B8)
    val accentBlue = Color(0xFF3B82F6)
    val surfaceCard = Color(0xFF0B1224)
    val cardStroke = Color(0xFF23324D)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceCard),
        border = BorderStroke(1.dp, cardStroke)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B1224), Color(0xFF0F172A), Color(0xFF111827))
                    )
                )
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, accentBlue, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalGasStation,
                        contentDescription = null,
                        tint = textLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Abastecimento",
                        color = textLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Previsao automatica da proxima parada",
                        color = textDim,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF111827))))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (proximaData != null && diasAte != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B1224))
                            .border(1.dp, Color(0xFF1F2A44), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Proximo abastecimento",
                            color = textDim,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            proximaData.format(dateFormatter),
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Em $diasAte dia(s)",
                            color = textDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumoCustoItem(
                        label = "Diario",
                        value = custoDia?.let { formatarMoedaLocal(it) } ?: "--",
                        icon = Icons.Rounded.Timelapse,
                        iconTint = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    ResumoCustoItem(
                        label = "Semana",
                        value = custoSemana?.let { formatarMoedaLocal(it) } ?: "--",
                        icon = Icons.Rounded.DateRange,
                        iconTint = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    ResumoCustoItem(
                        label = "Mes",
                        value = custoMes?.let { formatarMoedaLocal(it) } ?: "--",
                        icon = Icons.Rounded.CalendarMonth,
                        iconTint = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onHistorico,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
            ) {
                Text(
                    text = "Ver historico de abastecimento",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ResumoCustoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val textLight = Color(0xFFF1F5F9)
    val textDim = Color(0xFF94A3B8)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B1224))
            .border(1.dp, Color(0xFF1F2A44), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, color = textDim, fontSize = 11.sp)
        }
        Text(
            value,
            color = textLight,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
