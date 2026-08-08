package br.com.gui.carlembrete

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Painel de abertura do relatorio: o estado do veiculo em imagem, antes das tabelas.
 *
 * O relatorio inteiro era uma pilha de linhas rotulo-valor. Informacao estava toda la, mas nada
 * respondia "e esse carro, ta bem ou nao?" sem ler tudo. Aqui isso vira um anel, tres contadores
 * e a curva de gasto do ano.
 */

private const val PESO_VENCIDO = 25
private const val PESO_PENDENTE = 3
private const val TETO_PENALIDADE_PENDENTE = 10
private const val MESES_DO_ANO = 12

private val CorCritica = Color(0xFFEF4444)
private val CorAtencao = Color(0xFFF59E0B)
private val CorSaudavel = Color(0xFF10B981)

/**
 * Nota de 0 a 100. Vencido e divida e domina o calculo; pendente e agenda normal.
 *
 * A penalidade dos pendentes tem teto de proposito: sem ele, quem cadastra muita manutencao futura
 * — justamente o uso correto do app — recebia nota pior que quem nao cadastra nada. Seis avisos
 * agendados nao sao seis problemas.
 */
fun notaDeSaude(vencidos: Int, pendentes: Int): Int {
    val penalidadePendentes = (pendentes * PESO_PENDENTE).coerceAtMost(TETO_PENALIDADE_PENDENTE)
    return (100 - vencidos * PESO_VENCIDO - penalidadePendentes).coerceIn(0, 100)
}

private fun corDaNota(nota: Int): Color = when {
    nota >= 80 -> CorSaudavel
    nota >= 50 -> CorAtencao
    else -> CorCritica
}

@Composable
fun RelatorioPainelCard(
    vencidos: Int,
    pendentes: Int,
    concluidos: Int,
    gastosPorMes: List<Double>,
    anoReferencia: Int,
    cardColor: Color,
    cardBorder: Color,
    textLight: Color,
    textDim: Color,
    modifier: Modifier = Modifier
) {
    val nota = remember(vencidos, pendentes) { notaDeSaude(vencidos, pendentes) }
    val corNota = corDaNota(nota)

    // Anima a partir do zero na entrada: o anel se desenha em vez de aparecer pronto.
    var iniciou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciou = true }
    val progresso by animateFloatAsState(
        targetValue = if (iniciou) nota / 100f else 0f,
        animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        label = "anel_saude"
    )
    val notaAnimada = (progresso * 100).roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(112.dp)) {
                    val traco = 11.dp.toPx()
                    val diametro = size.minDimension - traco
                    val canto = Offset(traco / 2f, traco / 2f)
                    val area = Size(diametro, diametro)
                    drawArc(
                        color = cardBorder.copy(alpha = 0.5f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = canto,
                        size = area,
                        style = Stroke(width = traco, cap = StrokeCap.Round)
                    )
                    if (progresso > 0f) {
                        drawArc(
                            color = corNota,
                            startAngle = -90f,
                            sweepAngle = 360f * progresso,
                            useCenter = false,
                            topLeft = canto,
                            size = area,
                            style = Stroke(width = traco, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = notaAnimada.toString(),
                        color = textLight,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = tr("de 100", "of 100"),
                        color = textDim,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = when {
                        nota >= 80 -> tr("Veiculo em dia", "Vehicle up to date")
                        nota >= 50 -> tr("Precisa de atencao", "Needs attention")
                        else -> tr("Manutencao atrasada", "Maintenance overdue")
                    },
                    color = corNota,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                LinhaDeContagem(tr("Vencidos", "Overdue"), vencidos, CorCritica, textDim)
                LinhaDeContagem(tr("A vencer", "Upcoming"), pendentes, CorAtencao, textDim)
                LinhaDeContagem(tr("Concluidos", "Completed"), concluidos, CorSaudavel, textDim)
            }
        }

        if (gastosPorMes.any { it > 0.0 }) {
            GraficoDoAno(
                gastosPorMes = gastosPorMes,
                anoReferencia = anoReferencia,
                textLight = textLight,
                textDim = textDim,
                cardBorder = cardBorder
            )
        }
    }
}

/** Ponto colorido, rotulo e numero: le como legenda do anel, nao como mais uma tabela. */
@Composable
private fun LinhaDeContagem(rotulo: String, valor: Int, cor: Color, textDim: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (valor > 0) cor else cor.copy(alpha = 0.28f))
        )
        Spacer(Modifier.width(8.dp))
        Text(text = rotulo, color = textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            text = valor.toString(),
            color = if (valor > 0) cor else textDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GraficoDoAno(
    gastosPorMes: List<Double>,
    anoReferencia: Int,
    textLight: Color,
    textDim: Color,
    cardBorder: Color
) {
    val maior = gastosPorMes.maxOrNull() ?: 0.0
    val total = gastosPorMes.sum()
    val mesAtual = remember { LocalDate.now().monthValue }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Gasto em $anoReferencia", "Spending in $anoReferencia"),
                    color = textLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = tr("Manutencao por mes", "Maintenance per month"),
                    color = textDim,
                    fontSize = 10.5.sp
                )
            }
            Text(
                text = formatarReaisCurto(total),
                color = textLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            gastosPorMes.forEachIndexed { indice, valor ->
                BarraDoAno(
                    valor = valor,
                    maior = maior,
                    ehMesAtual = indice + 1 == mesAtual,
                    indice = indice,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Uma letra por mes: com doze colunas, "JAN" nao cabe sem virar sopa de letras.
            "JFMAMJJASOND".forEachIndexed { indice, letra ->
                Text(
                    text = letra.toString(),
                    color = if (indice + 1 == mesAtual) textLight else textDim,
                    fontSize = 9.sp,
                    fontWeight = if (indice + 1 == mesAtual) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Uma letra solta nao se explica sozinha: sem esta linha, "J F M A M J J A S O N D"
        // pode ser lido como qualquer coisa. Diz tambem o que o destaque significa.
        Text(
            text = tr(
                "Cada letra e um mes, de janeiro a dezembro. Em destaque, ${nomeDoMes(mesAtual, false)}.",
                "Each letter is a month, January to December. Highlighted: ${nomeDoMes(mesAtual, true)}."
            ),
            color = textDim,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

private fun nomeDoMes(mes: Int, ingles: Boolean): String = if (ingles) {
    when (mes) {
        1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
        5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
        9 -> "September"; 10 -> "October"; 11 -> "November"; else -> "December"
    }
} else {
    when (mes) {
        1 -> "janeiro"; 2 -> "fevereiro"; 3 -> "marco"; 4 -> "abril"
        5 -> "maio"; 6 -> "junho"; 7 -> "julho"; 8 -> "agosto"
        9 -> "setembro"; 10 -> "outubro"; 11 -> "novembro"; else -> "dezembro"
    }
}

@Composable
private fun BarraDoAno(
    valor: Double,
    maior: Double,
    ehMesAtual: Boolean,
    indice: Int,
    cardBorder: Color,
    modifier: Modifier = Modifier
) {
    var iniciou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciou = true }
    // Piso de 4%: mes com gasto pequeno precisa continuar distinguivel de mes sem gasto nenhum.
    val alvo = if (valor > 0.0 && maior > 0.0) ((valor / maior).toFloat()).coerceAtLeast(0.04f) else 0f
    val altura by animateFloatAsState(
        targetValue = if (iniciou) alvo else 0f,
        // Atraso crescente por mes: as barras sobem da esquerda para a direita.
        animationSpec = tween(durationMillis = 620, delayMillis = indice * 45, easing = LinearOutSlowInEasing),
        label = "barra_ano_$indice"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(cardBorder.copy(alpha = 0.28f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (altura > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(altura)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (ehMesAtual) Color(0xFF38BDF8) else Color(0xFF6366F1))
            )
        }
    }
}

/**
 * Total do ano em formato curto. Por extenso, um ano cheio de manutencao empurra o valor para fora
 * da linha do titulo.
 */
private fun formatarReaisCurto(valor: Double): String {
    val formato = NumberFormat.getIntegerInstance(Locale("pt", "BR"))
    return when {
        valor >= 1000 -> "R$ ${formato.format((valor / 1000).roundToInt())} mil"
        else -> "R$ ${formato.format(valor.roundToInt())}"
    }
}

/** Gasto de manutencao mes a mes do ano indicado, em doze posicoes (janeiro a dezembro). */
fun gastosMensaisDoAno(lembretes: List<Lembrete>, ano: Int): List<Double> {
    val porMes = DoubleArray(MESES_DO_ANO)
    lembretes.forEach { lembrete ->
        if (lembrete.valor > 0.0) {
            val data = dataRealizacaoLembrete(lembrete) ?: dataParaOrdenacao(lembrete)
            if (data != LocalDate.MAX && YearMonth.from(data).year == ano) {
                porMes[data.monthValue - 1] += lembrete.valor
            }
        }
        historicoGastosRealizados(lembrete).forEach { (data, valor) ->
            if (data.year == ano) porMes[data.monthValue - 1] += valor
        }
    }
    return porMes.toList()
}
