package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

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
    val custoMes = (combustivelMes + manutencoesMes).coerceAtLeast(0.0)
    val riscoLabel = when {
        manutencoesFuturas > 3000.0 -> "Risco crítico"
        manutencoesFuturas > 0.0 -> "Risco moderado"
        else -> "Operação estável"
    }
    val riscoColor = when {
        manutencoesFuturas > 3000.0 -> danger
        manutencoesFuturas > 0.0 -> warning
        else -> success
    }

    val cardBackground = if (isDark) Color(0xFF0F1B2E) else Color(0xFFFDFEFF)
    val cardStroke = if (isDark) borderColor.copy(alpha = 0.8f) else Color(0xFFD9E2EC)
    val subtitleColor = if (isDark) Color(0xFFBFCCE0) else Color(0xFF5B6B7B)
    val typeBadgeBg = if (isDark) Color(0xFF172842) else Color(0xFFEAF2FF)
    val typeBadgeBorder = if (isDark) Color(0xFF2E4568) else Color(0xFFD4E3FF)
    val typeBadgeText = if (isDark) Color(0xFFD8E7FF) else Color(0xFF264266)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardStroke, RoundedCornerShape(18.dp))
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = listOfNotNull(carro.marca.takeIf { it.isNotBlank() }, carro.modelo.takeIf { it.isNotBlank() })
                            .joinToString(" • ")
                            .ifBlank { "Sem detalhes" },
                        color = subtitleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Custo do mês",
                color = textDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatarMoedaMV(custoMes),
                color = accent,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF13233A) else Color(0xFFF3F8FF))
                    .border(1.dp, riscoColor.copy(alpha = if (isDark) 0.45f else 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = riscoLabel,
                    color = textLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (manutencoesFuturas > 0.0) formatarMoedaMV(manutencoesFuturas) else "R$ 0,00",
                    color = riscoColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Acumulado: ${if (total > 0.0) formatarMoedaMV(total) else "R$ 0,00"}",
                color = textDim,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatarMoedaMV(valor: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
}
