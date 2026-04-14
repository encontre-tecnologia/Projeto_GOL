package br.com.gui.carlembrete

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

@Composable
@ReadOnlyComposable
fun isEnglishUi(): Boolean {
    return false
}

@Composable
@ReadOnlyComposable
fun tr(pt: String, en: String): String = if (isEnglishUi()) en else pt

fun isEnglishNow(): Boolean = false

fun trNow(pt: String, en: String): String = if (isEnglishNow()) en else pt

@Composable
@ReadOnlyComposable
fun tipoManutencaoLabel(tipo: TipoManutencao): String = when (tipo) {
    TipoManutencao.CORRENTE -> tr("Corrente", "Chain")
    TipoManutencao.LUBRIFICACAO -> tr("Lubrificação", "Lubrication")
    TipoManutencao.PEDIVELA -> tr("Pedivela", "Crankset")
    TipoManutencao.ACESSORIOS -> tr("Acessórios", "Accessories")
    TipoManutencao.CONFORTO -> tr("Conforto", "Comfort")
    TipoManutencao.PNEU -> tr("Pneu", "Tire")
    TipoManutencao.TRANSMISSAO -> tr("Transmissão", "Transmission")
    TipoManutencao.REVISAO -> tr("Revisão", "Inspection")
    TipoManutencao.OLEO -> tr("Óleo", "Oil")
    TipoManutencao.LAVAGEM -> tr("Lavagem", "Wash")
    TipoManutencao.ABASTECIMENTO -> tr("Posto", "Fuel")
    TipoManutencao.BATERIA -> tr("Elétrica", "Electric")
    TipoManutencao.VIDROS -> tr("Vidros", "Glass")
    TipoManutencao.MECANICA -> tr("Mecânica", "Mechanics")
    TipoManutencao.FUNILARIA -> tr("Funilaria", "Bodywork")
    TipoManutencao.FREIO -> tr("Freio", "Brake")
    TipoManutencao.LICENCIAMENTO -> tr("Licença", "License")
    TipoManutencao.IPVA -> "IPVA"
    TipoManutencao.SEGURO -> tr("Seguro", "Insurance")
    TipoManutencao.OUTROS -> tr("Outros", "Others")
}
