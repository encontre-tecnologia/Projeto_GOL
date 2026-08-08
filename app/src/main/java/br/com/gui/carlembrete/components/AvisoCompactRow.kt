package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.text.NumberFormat
import java.util.Locale

private val CorPrazoNeutra = Color(0xFF64748B)

private val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM")

/**
 * Linha de aviso compacta, com cerca de 56dp de altura.
 *
 * O item antigo (LembreteCardLocal) tinha ~140dp: Card com borda, faixa em gradiente,
 * icone em circulo com borda, divisor, chip de acao e chip de valor. Com 100 avisos
 * isso e o gargalo de composicao da tela.
 *
 * Aqui o item carrega **estado**, nao acao: o icone a direita diz se ja existe
 * prestador vinculado. A acao mora no detalhe, que abre no toque e ja tem espaco.
 */
@Composable
fun AvisoCompactRow(
    lembrete: Lembrete,
    corStatus: Color,
    temPrestador: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarDivisor: Boolean = true,
    /** Superficie propria com cantos: usada no resumo da home, onde o item precisa
     *  ler como cartao clicavel. Na lista completa fica falso, com divisores. */
    comSuperficie: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val prazo = prazoDoAviso(lembrete)
    val fundoItem = if (isDark) Color(0xFF0D1117) else Color(0xFFF1F5F9)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (comSuperficie) {
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(fundoItem)
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.06f)
                                else Color.Black.copy(alpha = 0.07f),
                                RoundedCornerShape(14.dp)
                            )
                    } else {
                        Modifier
                    }
                )
                .clickable { onClick() }
                .padding(
                    horizontal = if (comSuperficie) 12.dp else 0.dp,
                    vertical = if (comSuperficie) 11.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(corStatus.copy(alpha = if (isDark) 0.16f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                TipoIcon(tipo = lembrete.tipo, tint = corStatus, size = 19.dp)
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lembrete.titulo,
                    color = scheme.onSurface,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = metaDoAviso(lembrete)
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            if (prazo.urgente) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(prazo.cor.copy(alpha = if (isDark) 0.16f else 0.13f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = prazo.texto,
                        color = prazo.cor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else {
                Text(
                    text = prazo.texto,
                    color = prazo.cor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            if (comSuperficie) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        if (mostrarDivisor) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.08f)
                    )
            )
        }
    }
}

/** Valor e km numa linha de meta, em vez de dois chips com borda como antes. */
private fun metaDoAviso(lembrete: Lembrete): String {
    val partes = mutableListOf<String>()
    if (lembrete.valor > 0.0) {
        partes += runCatching {
            NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(lembrete.valor)
        }.getOrDefault("R$ --")
    }
    val km = lembrete.kmLimite.trim()
    if (km.isNotBlank() && km.any(Char::isDigit)) {
        partes += "$km km"
    }
    val data = dataParaOrdenacao(lembrete)
    if (data != LocalDate.MAX) {
        partes += data.format(formatoDiaMes)
    }
    return partes.joinToString(" · ")
}

data class PrazoAviso(val texto: String, val cor: Color, val urgente: Boolean = false)

/**
 * Prazo curto o suficiente para caber na coluna fixa da direita. Usa a mesma regra de
 * dias que o resto do app, para o texto nunca discordar da cor do status.
 */
fun prazoDoAviso(lembrete: Lembrete): PrazoAviso {
    val data = dataParaOrdenacao(lembrete)
    // Aviso sem data acompanha KM: nao tem prazo em dias para mostrar.
    if (data == LocalDate.MAX) return PrazoAviso("por km", CorPrazoNeutra)
    val dias = ChronoUnit.DAYS.between(LocalDate.now(), data)
    return when {
        dias < 0 -> PrazoAviso("${-dias}d atras", Color(0xFFF87171), urgente = true)
        dias == 0L -> PrazoAviso("hoje", Color(0xFFF87171), urgente = true)
        dias <= 30 -> PrazoAviso("${dias}d", Color(0xFFFBBF24), urgente = true)
        else -> PrazoAviso("${dias}d", CorPrazoNeutra)
    }
}
