package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isDark: Boolean,
    textLight: Color,
    textDim: Color,
    borderColor: Color,
    accent: Color,
    warning: Color,
    danger: Color,
    success: Color
) {
    val cardBackground = MaterialTheme.colorScheme.surface
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF475569)
    val typeBadgeBg = if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)
    val typeBadgeBorder = if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1)
    val typeBadgeText = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    val totalValueColor = if (isDark) Color.White else Color(0xFF0F172A)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        color = subtitleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeBadgeBg)
                        .border(0.5.dp, typeBadgeBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = carro.tipoVeiculo.label.uppercase(),
                        color = typeBadgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancePill(
                    modifier = Modifier.weight(1f),
                    label = "Abastecido",
                    value = if (combustivelMes > 0.0) formatarMoedaMV(combustivelMes) else "R$ 0,00",
                    indicatorColor = accent,
                    isDark = isDark,
                    textColor = textLight,
                    dimColor = textDim
                )
                FinancePill(
                    modifier = Modifier.weight(1f),
                    label = "Manut. mês",
                    value = if (manutencoesMes > 0.0) formatarMoedaMV(manutencoesMes) else "R$ 0,00",
                    indicatorColor = warning,
                    isDark = isDark,
                    textColor = textLight,
                    dimColor = textDim
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancePill(
                    modifier = Modifier.weight(1f),
                    label = "Próx. manut.",
                    value = if (manutencoesFuturas > 0.0) formatarMoedaMV(manutencoesFuturas) else "R$ 0,00",
                    indicatorColor = danger,
                    isDark = isDark,
                    textColor = textLight,
                    dimColor = textDim
                )

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
                        color = totalValueColor,
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
    indicatorColor: Color,
    isDark: Boolean,
    textColor: Color,
    dimColor: Color
) {
    val pillBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
    val pillBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(pillBg)
            .border(0.5.dp, pillBorder, RoundedCornerShape(14.dp))
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
                color = dimColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
