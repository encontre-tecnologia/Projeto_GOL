package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VehicleSpendCard(
    carro: CarroInfo,
    total: Double,
    combustivelMes: Double,
    manutencoesMes: Double,
    manutencoesFuturas: Double,
    textLight: Color,
    textDim: Color,
    accent: Color,
    warning: Color,
    danger: Color,
    success: Color
) {
    val cardBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBackground, RoundedCornerShape(20.dp))
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = carro.nome,
                        color = textLight,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = listOfNotNull(carro.marca.takeIf { it.isNotBlank() }, carro.modelo.takeIf { it.isNotBlank() })
                            .joinToString(" • ")
                            .ifBlank { "Sem detalhes" },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Badge do Tipo: Agora em um tom sóbrio e equilibrado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2D3748)) // Azul acinzentado fechado
                        .border(0.5.dp, Color(0xFF4A5568), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = carro.tipoVeiculo.label.uppercase(),
                        color = Color(0xFFE2E8F0), // Branco gelo suave
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Grid 2x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancePill(
                    modifier = Modifier.weight(1f),
                    label = "Abastecido",
                    value = if (combustivelMes > 0.0) formatarMoedaMV(combustivelMes) else "R$ 0,00",
                    indicatorColor = accent
                )
                FinancePill(
                    modifier = Modifier.weight(1f),
                    label = "Manut. Mês",
                    value = if (manutencoesMes > 0.0) formatarMoedaMV(manutencoesMes) else "R$ 0,00",
                    indicatorColor = warning
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancePill(
                    modifier = Modifier.weight(1f),
                    label = "Próx. Manut.",
                    value = if (manutencoesFuturas > 0.0) formatarMoedaMV(manutencoesFuturas) else "R$ 0,00",
                    indicatorColor = danger
                )

                // Total Geral
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(success.copy(alpha = 0.15f))
                        .border(0.5.dp, success.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "TOTAL GERAL",
                        color = success,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (total > 0.0) formatarMoedaMV(total) else "R$ 0,00",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancePill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    indicatorColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}