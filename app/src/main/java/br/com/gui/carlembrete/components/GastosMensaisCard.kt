package br.com.gui.carlembrete

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Quantos meses o grafico mostra. Seis cabe na largura sem apertar os rotulos. */
const val MESES_NO_GRAFICO = 6

private val CorCombustivel = Color(0xFF3B82F6)
private val CorManutencao = Color(0xFFF59E0B)

/** Gasto de um mes, separado por origem. */
data class GastoDoMes(
    val mes: YearMonth,
    val combustivel: Double,
    val manutencao: Double
) {
    val total: Double get() = combustivel + manutencao
}

/**
 * Gasto por mes do veiculo, dos ultimos [meses] meses ate o mes atual.
 *
 * Conta apenas **servico realizado**, nao aviso pendente: aviso com valor previsto e
 * intencao, nao despesa. A data usada e a de realizacao, nao a data limite — gasto
 * pertence ao mes em que o dinheiro saiu.
 */
fun calcularGastosMensais(
    lembretes: List<Lembrete>,
    abastecimentos: List<Abastecimento>,
    meses: Int = MESES_NO_GRAFICO,
    hoje: LocalDate = LocalDate.now()
): List<GastoDoMes> {
    val mesAtual = YearMonth.from(hoje)
    val janela = (meses - 1 downTo 0).map { mesAtual.minusMonths(it.toLong()) }
    val combustivelPorMes = mutableMapOf<YearMonth, Double>()
    val manutencaoPorMes = mutableMapOf<YearMonth, Double>()

    lembretes.forEach { lembrete ->
        if (lembrete.valor > 0.0) {
            dataRealizacaoLembrete(lembrete)?.let { data ->
                val mes = YearMonth.from(data)
                manutencaoPorMes[mes] = (manutencaoPorMes[mes] ?: 0.0) + lembrete.valor
            }
        }
        historicoGastosRealizados(lembrete).forEach { (data, valor) ->
            val mes = YearMonth.from(data)
            manutencaoPorMes[mes] = (manutencaoPorMes[mes] ?: 0.0) + valor
        }
    }

    abastecimentos.forEach { abastecimento ->
        if (abastecimento.valorPago <= 0.0) return@forEach
        val data = parseFuelDate(abastecimento.data) ?: return@forEach
        val mes = YearMonth.from(data)
        combustivelPorMes[mes] = (combustivelPorMes[mes] ?: 0.0) + abastecimento.valorPago
    }

    return janela.map { mes ->
        GastoDoMes(
            mes = mes,
            combustivel = combustivelPorMes[mes] ?: 0.0,
            manutencao = manutencaoPorMes[mes] ?: 0.0
        )
    }
}

/**
 * Grafico de gastos por mes, com barras empilhadas: combustivel e manutencao.
 *
 * Separar as duas origens e o que torna o grafico util em vez de decorativo — um mes
 * caro por pneu novo conta uma historia diferente de um mes caro por rodar muito.
 */
@Composable
fun GastosMensaisCard(
    gastos: List<GastoDoMes>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val fundo = if (isDark) Color(0xFF0D1117) else Color(0xFFF1F5F9)
    val borda = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.07f)
    val textoFraco = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val maiorTotal = remember(gastos) { gastos.maxOfOrNull { it.total } ?: 0.0 }
    /*
     * O numero em destaque soma a janela inteira, e nao so o mes corrente.
     *
     * Mostrando o mes atual, o card abria com "R$ 0,00" em corpo grande ao lado de barras cheias
     * — nos primeiros dias de qualquer mes o valor e zero, e a leitura imediata era de grafico
     * quebrado. O mes corrente continua visivel, em corpo menor e sem competir com o total.
     */
    val totalDoPeriodo = remember(gastos) { gastos.sumOf { it.total } }
    val totalDoMes = gastos.lastOrNull()?.total ?: 0.0
    val variacaoMesAnterior = remember(gastos) { variacaoPercentualMesAnterior(gastos) }
    val corVariacao = variacaoMesAnterior?.let { variacao ->
        when {
            variacao > 0.0 -> Color(0xFFF59E0B)
            variacao < 0.0 -> Color(0xFF22C55E)
            else -> textoFraco
        }
    } ?: textoFraco

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fundo)
            .border(1.dp, borda, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = tr("Gastos", "Spending"),
                    color = scheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "Este mes ${formatarReais(totalDoMes)}",
                        "This month ${formatarReais(totalDoMes)}"
                    ),
                    color = textoFraco,
                    fontSize = 11.sp
                )
                variacaoMesAnterior?.let { variacao ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = textoVariacaoMesAnterior(variacao),
                        color = corVariacao,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatarReais(totalDoPeriodo),
                    color = scheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = tr("ultimos ${gastos.size} meses", "last ${gastos.size} months"),
                    color = textoFraco,
                    fontSize = 10.5.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (maiorTotal <= 0.0) {
            SemGastos(textoFraco = textoFraco)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                gastos.forEach { gasto ->
                    BarraDoMes(
                        gasto = gasto,
                        maiorTotal = maiorTotal,
                        textoFraco = textoFraco,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendaDeCor(CorCombustivel, tr("Combustivel", "Fuel"), textoFraco)
                LegendaDeCor(CorManutencao, tr("Manutencao", "Maintenance"), textoFraco)
            }
        }
    }
}

@Composable
private fun BarraDoMes(
    gasto: GastoDoMes,
    maiorTotal: Double,
    textoFraco: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // Altura relativa ao maior mes da janela. Minimo de 2% para o mes com gasto
    // pequeno nao desaparecer e parecer mes sem lancamento nenhum.
    val proporcao = if (maiorTotal <= 0.0) 0f else (gasto.total / maiorTotal).toFloat()
    val alturaAnimada by animateFloatAsState(
        targetValue = if (gasto.total > 0.0) proporcao.coerceAtLeast(0.02f) else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "barra_${gasto.mes}"
    )
    val trilha = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(trilha),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (alturaAnimada > 0f) {
                // Empilhada: combustivel embaixo, manutencao em cima.
                val fatiaCombustivel = if (gasto.total <= 0.0) 0f
                else (gasto.combustivel / gasto.total).toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(alturaAnimada)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    if (fatiaCombustivel < 1f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight((1f - fatiaCombustivel).coerceAtLeast(0.0001f))
                                .background(CorManutencao)
                        )
                    }
                    if (fatiaCombustivel > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(fatiaCombustivel)
                                .background(CorCombustivel)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = rotuloDoMes(gasto.mes),
            color = textoFraco,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LegendaDeCor(cor: Color, rotulo: String, textoFraco: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cor)
        )
        Spacer(Modifier.width(5.dp))
        Text(text = rotulo, color = textoFraco, fontSize = 10.5.sp)
    }
}

@Composable
private fun SemGastos(textoFraco: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tr("Nenhum gasto registrado ainda", "No spending recorded yet"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tr(
                "Registre abastecimentos e servicos realizados para ver o grafico.",
                "Record fuel and completed services to see the chart."
            ),
            color = textoFraco,
            fontSize = 10.5.sp
        )
    }
}

private val formatoMesCurto = DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))

private fun rotuloDoMes(mes: YearMonth): String =
    mes.atDay(1).format(formatoMesCurto).replace(".", "").uppercase()

private fun formatarReais(valor: Double): String =
    runCatching { NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor) }
        .getOrDefault("R$ --")

fun variacaoPercentualMesAnterior(gastos: List<GastoDoMes>): Double? {
    if (gastos.size < 2) return null
    val mesAtual = gastos.last().total
    val mesAnterior = gastos[gastos.lastIndex - 1].total
    if (mesAnterior <= 0.0) return null
    return ((mesAtual - mesAnterior) / mesAnterior) * 100.0
}

fun textoVariacaoMesAnterior(variacao: Double): String {
    val arredondada = kotlin.math.round(variacao).toInt()
    val sinal = if (arredondada > 0) "+" else ""
    return "$sinal$arredondada% vs mes anterior"
}
