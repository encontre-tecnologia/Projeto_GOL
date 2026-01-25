import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- CORES (Sólidas para consistência) ---
private val CardBackgroundColor = Color(0xFF1E293B) // Slate 800 (Mesma do card do carro)

private val AccentBlue = Color(0xFF3B82F6)
private val TextWhite = Color(0xFFF1F5F9)
private val TextGray = Color(0xFF94A3B8)
private val SuccessGreen = Color(0xFF10B981)

// Fundo dos "cards internos" um pouco mais escuro para criar profundidade
private val InnerCardBackground = Color(0xFF0F172A).copy(alpha = 0.5f)

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp, // Sombra um pouco mais suave
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            // Cor sólida aplicada aqui
            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .background(CardBackgroundColor)
                    .padding(12.dp)
            ) {
                // --- Cabeçalho ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f), // Vidro sutil
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.LocalGasStation,
                                contentDescription = null,
                                tint = AccentBlue, // Destaque em azul para combinar com o tema
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gestão de Combustível",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Previsão e custos médios",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- Bloco de Previsão ---
                if (proximaData != null && diasAte != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(InnerCardBackground) // Fundo interno mais escuro
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Próximo Tanque",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = proximaData.format(dateFormatter),
                                color = TextWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }

                        // Badge de dias
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Em $diasAte dias",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- Grid de Custos ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CustoCompactoItem(
                        label = "Diário",
                        value = custoDia,
                        icon = Icons.Rounded.Timelapse,
                        color = Color(0xFF60A5FA), // Blue 400
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    CustoCompactoItem(
                        label = "Semana",
                        value = custoSemana,
                        icon = Icons.Rounded.DateRange,
                        color = Color(0xFF34D399), // Emerald 400
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    CustoCompactoItem(
                        label = "Mês",
                        value = custoMes,
                        icon = Icons.Rounded.CalendarMonth,
                        color = Color(0xFFF59E0B), // Amber 500
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // --- Botão de Ação ---
                Button(
                    onClick = onHistorico,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp), // Botão ligeiramente mais alto
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        text = "Ver Histórico Completo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustoCompactoItem(
    label: String,
    value: Double?,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)) // Cantos mais arredondados
            .background(InnerCardBackground)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value?.let { "R$ ${String.format("%.2f", it)}" } ?: "R$ 0,00",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}




