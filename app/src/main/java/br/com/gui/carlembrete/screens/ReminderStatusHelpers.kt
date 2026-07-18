package br.com.gui.carlembrete

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

internal fun isBikeCategory(tipoVeiculo: TipoVeiculo): Boolean =
    tipoVeiculo == TipoVeiculo.BICICLETA || tipoVeiculo == TipoVeiculo.BIKE_ELETRICA

internal fun showFuelReminder(tipoVeiculo: TipoVeiculo): Boolean =
    tipoVeiculo != TipoVeiculo.BICICLETA &&
        tipoVeiculo != TipoVeiculo.BIKE_ELETRICA &&
        tipoVeiculo != TipoVeiculo.VEICULO_ELETRICO

internal fun tiposAvisoPorVeiculo(tipoVeiculo: TipoVeiculo): List<TipoManutencao> = when (tipoVeiculo) {
    TipoVeiculo.BICICLETA -> listOf(
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.FREIO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.MECANICA,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.BIKE_ELETRICA -> listOf(
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.FREIO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.BATERIA,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.VEICULO_ELETRICO -> listOf(
        TipoManutencao.BATERIA,
        TipoManutencao.FREIO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.VIDROS,
        TipoManutencao.PNEU,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.REVISAO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.ONIBUS,
    TipoVeiculo.CAMINHAO,
    TipoVeiculo.VAN,
    TipoVeiculo.CAMINHONETE,
    TipoVeiculo.FURGAO,
    TipoVeiculo.HATCH,
    TipoVeiculo.MOTORHOME -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.FREIO,
        TipoManutencao.VIDROS,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.TRATOR -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.FREIO,
        TipoManutencao.MECANICA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.MOTO -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.FREIO,
        TipoManutencao.MECANICA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.CARRETINHA -> listOf(
        TipoManutencao.LAVAGEM,
        TipoManutencao.PNEU,
        TipoManutencao.MECANICA,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    else -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.VIDROS,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
}

internal fun calcularCorStatusLocal(lembretes: List<Lembrete>, tipo: TipoManutencao): Color {
    return when (tipo) {
        TipoManutencao.CORRENTE -> Color(0xFF22C55E) // verde
        TipoManutencao.LUBRIFICACAO -> Color(0xFF14B8A6) // verde-azulado
        TipoManutencao.PEDIVELA -> Color(0xFF0EA5E9) // azul
        TipoManutencao.ACESSORIOS -> Color(0xFFF97316) // laranja
        TipoManutencao.CONFORTO -> Color(0xFFEAB308) // amarelo
        TipoManutencao.PNEU -> Color(0xFFF59E0B) // laranja
        TipoManutencao.TRANSMISSAO -> Color(0xFF60A5FA) // azul claro
        TipoManutencao.REVISAO -> Color(0xFF8B5CF6) // roxo
        TipoManutencao.OLEO -> Color(0xFF3B82F6) // Azul
        TipoManutencao.ABASTECIMENTO -> Color(0xFF0EA5E9) // azul ciano
        TipoManutencao.LAVAGEM -> Color(0xFF06B6D4) // azul agua
        TipoManutencao.FREIO -> Color(0xFFEF4444) // Vermelho
        TipoManutencao.VIDROS -> Color(0xFF38BDF8) // azul vidro
        TipoManutencao.MECANICA -> Color(0xFFF59E0B) // Laranja
        TipoManutencao.FUNILARIA -> Color(0xFFF97316) // Laranja escuro
        TipoManutencao.LICENCIAMENTO -> Color(0xFF10B981) // Verde
        TipoManutencao.SEGURO -> Color(0xFF22C55E) // Verde claro
        else -> Color(0xFF6366F1) // Roxo padrÃ£o
    }
}

internal fun textoStatusPrazoLocal(lembrete: Lembrete): String {
    val hoje = LocalDate.now()
    val data = dataParaOrdenacao(lembrete)
    if (data == LocalDate.MAX) return "Acompanhar KM"
    val dias = ChronoUnit.DAYS.between(hoje, data)
    return when {
        dias < 0 -> "Vencido"
        dias == 0L -> "Hoje"
        dias <= 7 -> "Urgente"
        else -> "No Prazo"
    }
}

internal fun formatarMoedaLocal(valor: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
}
