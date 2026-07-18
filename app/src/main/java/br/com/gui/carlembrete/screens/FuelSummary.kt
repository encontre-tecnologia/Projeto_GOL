package br.com.gui.carlembrete

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

internal data class AbastecimentoResumo(
    val ultimo: AbastecimentoEntry?,
    val proximaData: LocalDate?,
    val diasAte: Long?,
    val mediaCustoDia: Double?,
    val custoSemana: Double?,
    val custoMes: Double?
)

internal data class AbastecimentoEntry(
    val data: LocalDate,
    val litros: Double,
    val valorPago: Double
)

internal fun calcularResumoAbastecimento(
    abastecimentos: List<Abastecimento>,
    formatter: DateTimeFormatter
): AbastecimentoResumo {
    val entries = abastecimentos.mapNotNull { item ->
        val data = runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
        if (data != null && item.litros > 0.0 && item.valorPago > 0.0) {
            AbastecimentoEntry(data, item.litros, item.valorPago)
        } else {
            null
        }
    }.sortedBy { it.data }

    if (entries.isEmpty()) {
        return AbastecimentoResumo(
            ultimo = null,
            proximaData = null,
            diasAte = null,
            mediaCustoDia = null,
            custoSemana = null,
            custoMes = null
        )
    }

    val diasEntreAbastecimentos = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.data, atual.data)
        if (dias <= 0) null else dias.toDouble()
    }

    val custoDiario = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.data, atual.data)
        if (dias <= 0) null else atual.valorPago / dias.toDouble()
    }

    val mediaDiasBase = diasEntreAbastecimentos.takeIf { it.isNotEmpty() }?.average()
    val mediaCustoBase = custoDiario.takeIf { it.isNotEmpty() }?.average()
    val ultimo = entries.last()
    val fallbackDias = 7.0
    val mediaDias = mediaDiasBase ?: fallbackDias
    val mediaCusto = mediaCustoBase ?: (ultimo.valorPago / mediaDias)
    val diasAte = mediaDias.takeIf { it > 0.0 }?.let { ceil(it).toLong() }
    val proximaData = diasAte?.let { ultimo.data.plusDays(it) }

    return AbastecimentoResumo(
        ultimo = ultimo,
        proximaData = proximaData,
        diasAte = diasAte,
        mediaCustoDia = mediaCusto,
        custoSemana = mediaCusto?.times(7),
        custoMes = mediaCusto?.times(30)
    )
}

internal data class CategorySpend(
    val label: String,
    val valor: Double,
    val color: Color
)
