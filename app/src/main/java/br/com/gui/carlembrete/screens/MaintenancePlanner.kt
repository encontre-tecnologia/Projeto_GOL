package br.com.gui.carlembrete

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Monta um plano de manutenção recomendado para um veículo, baseado em REGRAS
 * confiáveis (intervalos padrão por tipo, ajustados por idade e km). A IA depois
 * só personaliza a explicação — os números/intervalos vêm sempre daqui, pra não
 * correr risco de a IA inventar valor errado.
 */
object MaintenancePlanner {
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun buildPlan(carro: CarroInfo): List<AiReminderDraft> = when (carro.tipoVeiculo) {
        TipoVeiculo.BICICLETA, TipoVeiculo.BIKE_ELETRICA -> bikePlan(carro)
        TipoVeiculo.MOTO -> motoPlan(carro)
        TipoVeiculo.VEICULO_ELETRICO -> eletricoPlan(carro)
        else -> carroPlan(carro)
    }

    fun isMaintenancePlanRequest(text: String): Boolean {
        val t = normalize(text)
        val gatilhos = listOf(
            "plano de manutencao",
            "plano de revisao",
            "plano de manutencoes",
            "manutencoes recomendadas",
            "manutencao recomendada",
            "sugestao de manutencao",
            "sugestoes de manutencao",
            "gerar plano",
            "gerar um plano",
            "criar plano",
            "criar um plano",
            "montar plano",
            "montar um plano",
            "plano de lembretes",
            "lembretes de manutencao",
            "lembretes de revisao",
            "avisos automaticos",
            "criar avisos sozinho",
            "criar os avisos sozinho"
        )
        return gatilhos.any { t.contains(it) }
    }

    fun fallbackIntro(carro: CarroInfo, count: Int): String {
        val ano = anoVeiculo(carro)
        val idadePlano = idadeAnos(carro)
        val detalhesPlano = buildList {
            if (carro.marca.isNotBlank()) add(carro.marca)
            if (carro.modelo.isNotBlank()) add(carro.modelo)
            if (ano != null) add("ano $ano")
            if (carro.kmAtual > 0) add("%,d km".format(carro.kmAtual).replace(',', '.'))
        }
        val sufixoPlano = if (detalhesPlano.isEmpty()) "" else " (${detalhesPlano.joinToString(", ")})"
        val contextoPlano = "Montei **$count sugestoes de manutencao** para o ${carro.nome}$sufixoPlano."
        val basePlano = if (ano != null) {
            "Usei o veiculo cadastrado, o ano encontrado, o KM atual e pontos comuns de manutencao desse tipo de veiculo."
        } else {
            "Usei o veiculo cadastrado, o KM atual e pontos comuns de manutencao desse tipo de veiculo. Nao encontrei um ano claro no nome/modelo cadastrado."
        }
        val dicaPlano = when {
            idadePlano != null && idadePlano >= 12 -> "\n\nComo e um veiculo mais antigo, dei mais peso para itens que costumam incomodar com o tempo: correia dentada, mangueiras/borrachas, freio, suspensao, bateria, oleo e arrefecimento."
            idadePlano != null && idadePlano >= 6 -> "\n\nPriorizei itens comuns que costumam aparecer pelo uso e pelo KM: oleo, filtros, freios, pneus, arrefecimento e correia."
            else -> "\n\nOs cards abaixo sao sugestoes preventivas calculadas a partir do KM atual e de intervalos comuns de manutencao."
        }
        return "**Plano de manutencao do ${carro.nome}**\n---\n$contextoPlano\n$basePlano$dicaPlano\n\nConfira os cards abaixo antes de criar. Eles sao sugestoes, entao vale ajustar conforme manual do fabricante e historico real do veiculo."

        val idade = idadeAnos(carro)
        val detalhes = buildList {
            if (carro.kmAtual > 0) add("%,d km".format(carro.kmAtual).replace(',', '.'))
            if (idade != null) add("~$idade anos")
        }
        val sufixo = if (detalhes.isEmpty()) "" else " (${detalhes.joinToString(", ")})"
        val contexto = "Montei **$count manutenções recomendadas** para o ${carro.nome}$sufixo."
        val dica = when {
            idade != null && idade >= 12 -> "\n\nComo é um veículo mais antigo, priorizei itens que envelhecem com o tempo: correia, mangueiras/borrachas, freio e suspensão."
            idade != null && idade >= 6 -> "\n\nDei prioridade aos itens próximos do seu km atual."
            else -> "\n\nOs itens estão calculados a partir do km atual do veículo."
        }
        return "**Plano de manutenção do ${carro.nome}**\n---\n$contexto$dica\n\nConfira os cards abaixo e crie os que quiser (pode criar todos de uma vez)."
    }

    fun introPrompt(carro: CarroInfo, count: Int): String {
        val ano = anoVeiculo(carro)?.toString() ?: "ano nao informado"
        val idadePrompt = idadeAnos(carro)?.let { "$it anos" } ?: "idade nao informada"
        val marca = carro.marca.ifBlank { "marca nao informada" }
        val modelo = carro.modelo.ifBlank { "modelo nao informado" }
        return "Escreva uma introducao curta e amigavel (2 a 3 frases) em portugues do Brasil para um plano de manutencao. " +
            "Explique que os $count cards abaixo sao sugestoes baseadas no veiculo cadastrado, marca/modelo, ano quando existir, KM atual e pontos comuns de manutencao. " +
            "Veiculo: nome=${carro.nome}, marca=$marca, modelo=$modelo, ano=$ano, idade=$idadePrompt, km=${carro.kmAtual}, tipo=${carro.tipoVeiculo.label}. " +
            "Se reconhecer pontos comuns desse perfil, cite exemplos de atencao em linguagem de sugestao, como correia dentada, oleo, filtros, freios, pneus, arrefecimento, bateria ou suspensao. " +
            "NAO diga que consultou internet. NAO invente defeito especifico com certeza. NAO invente intervalos nem numeros e NAO liste os itens, porque eles ja aparecem em cards. " +
            "Diga para conferir com o manual/fabricante e ajustar conforme historico real do veiculo."

        val idade = idadeAnos(carro)?.let { "$it anos" } ?: "idade nao informada"
        return "Escreva uma introducao curta e amigavel (2 a 3 frases) em portugues do Brasil recomendando a manutencao do ${carro.nome} " +
            "(${carro.modelo}, $idade, ${carro.kmAtual} km). Diga que preparei $count lembretes recomendados abaixo e o que vale priorizar pela idade e km. " +
            "NAO invente intervalos nem numeros e NAO liste os itens (eles ja aparecem em cards). Apenas explique de forma simples a importancia."
    }

    // ---------- Planos por tipo ----------

    private fun carroPlan(carro: CarroInfo): List<AiReminderDraft> {
        val km = carro.kmAtual
        val idade = idadeAnos(carro) ?: 0
        val list = mutableListOf<AiReminderDraft>()
        list += draft(carro, "Troca de óleo e filtro", "Óleo do motor", TipoManutencao.OLEO, km = nextKm(km, 10000))
        list += draft(carro, "Trocar filtro de ar", "Filtro de ar", TipoManutencao.REVISAO, km = nextKm(km, 20000))
        list += draft(carro, "Trocar filtro de combustível", "Filtro de combustível", TipoManutencao.REVISAO, km = nextKm(km, 20000))
        list += draft(carro, "Trocar velas de ignição", "Velas de ignição", TipoManutencao.MECANICA, km = nextKm(km, if (idade >= 10) 30000 else 40000))
        list += draft(carro, "Verificar pastilhas de freio", "Pastilhas de freio", TipoManutencao.FREIO, km = nextKm(km, 30000))
        list += draft(carro, "Trocar fluido de freio", "Fluido de freio", TipoManutencao.FREIO, data = emMeses(24))
        list += draft(carro, "Rodízio e calibragem dos pneus", "Pneus", TipoManutencao.PNEU, km = nextKm(km, 10000))
        list += draft(carro, "Alinhamento e balanceamento", "Suspensão e pneus", TipoManutencao.PNEU, km = nextKm(km, 10000))
        list += draft(carro, "Trocar fluido de arrefecimento", "Arrefecimento", TipoManutencao.MECANICA, data = emMeses(24))
        list += draft(carro, "Verificar correia dentada", "Correia dentada", TipoManutencao.MECANICA, km = nextKm(km, 50000))
        list += draft(carro, "Revisão geral", "Revisão", TipoManutencao.REVISAO, data = emMeses(12))
        if (idade >= 10) {
            list += draft(carro, "Verificar mangueiras e borrachas", "Mangueiras e borrachas", TipoManutencao.MECANICA, data = emMeses(6))
            list += draft(carro, "Verificar suspensão e amortecedores", "Suspensão", TipoManutencao.MECANICA, km = nextKm(km, 20000))
            list += draft(carro, "Testar a bateria", "Bateria", TipoManutencao.BATERIA, data = emMeses(12))
        }
        return list
    }

    private fun motoPlan(carro: CarroInfo): List<AiReminderDraft> {
        val km = carro.kmAtual
        return listOf(
            draft(carro, "Troca de óleo do motor", "Óleo do motor", TipoManutencao.OLEO, km = nextKm(km, 3000)),
            draft(carro, "Limpeza e lubrificação da corrente", "Corrente", TipoManutencao.LUBRIFICACAO, km = nextKm(km, 1000)),
            draft(carro, "Verificar/ajustar relação (corrente e coroa)", "Transmissão", TipoManutencao.TRANSMISSAO, km = nextKm(km, 15000)),
            draft(carro, "Verificar pastilhas/lonas de freio", "Freio", TipoManutencao.FREIO, km = nextKm(km, 10000)),
            draft(carro, "Calibrar e checar pneus", "Pneus", TipoManutencao.PNEU, km = nextKm(km, 5000)),
            draft(carro, "Trocar fluido de freio", "Fluido de freio", TipoManutencao.FREIO, data = emMeses(24)),
            draft(carro, "Revisão geral", "Revisão", TipoManutencao.REVISAO, data = emMeses(12))
        )
    }

    private fun bikePlan(carro: CarroInfo): List<AiReminderDraft> = listOf(
        draft(carro, "Lubrificar a corrente", "Corrente", TipoManutencao.LUBRIFICACAO, data = emMeses(1)),
        draft(carro, "Verificar freios e pastilhas", "Freio", TipoManutencao.FREIO, data = emMeses(3)),
        draft(carro, "Calibrar e checar pneus", "Pneus", TipoManutencao.PNEU, data = emMeses(1)),
        draft(carro, "Revisão geral (marchas e cabos)", "Revisão", TipoManutencao.REVISAO, data = emMeses(6))
    )

    private fun eletricoPlan(carro: CarroInfo): List<AiReminderDraft> {
        val km = carro.kmAtual
        return listOf(
            draft(carro, "Rodízio e calibragem dos pneus", "Pneus", TipoManutencao.PNEU, km = nextKm(km, 10000)),
            draft(carro, "Verificar pastilhas de freio", "Freio", TipoManutencao.FREIO, km = nextKm(km, 30000)),
            draft(carro, "Trocar fluido de freio", "Fluido de freio", TipoManutencao.FREIO, data = emMeses(24)),
            draft(carro, "Alinhamento e balanceamento", "Suspensão e pneus", TipoManutencao.PNEU, km = nextKm(km, 10000)),
            draft(carro, "Checar bateria e sistema elétrico", "Bateria", TipoManutencao.BATERIA, data = emMeses(12)),
            draft(carro, "Revisão geral", "Revisão", TipoManutencao.REVISAO, data = emMeses(12))
        )
    }

    // ---------- Helpers ----------

    private fun draft(
        carro: CarroInfo,
        titulo: String,
        peca: String,
        tipo: TipoManutencao,
        km: String = "",
        data: String = ""
    ): AiReminderDraft {
        val dataFinal = data.ifBlank { calcularProximaData(tipo, LocalDate.now()) }
        val kmFinal = km.ifBlank { kmPadraoCadastroManual(carro, tipo) }
        return AiReminderDraft(
            carro = carro,
            titulo = titulo,
            peca = peca,
            dataLimite = dataFinal,
            kmLimite = kmFinal,
            tipo = tipo
        )
    }

    private fun nextKm(current: Int, interval: Int): String {
        if (interval <= 0) return ""
        val base = if (current < 0) 0 else current
        val next = ((base / interval) + 1) * interval
        return next.toString()
    }

    private fun emMeses(meses: Long): String = LocalDate.now().plusMonths(meses).format(dateFmt)

    private fun kmPadraoCadastroManual(carro: CarroInfo, tipo: TipoManutencao): String {
        val adicional = getKmAdicionalPorTipo(tipo)
        return (carro.kmAtual + adicional).coerceAtLeast(0).toString()
    }

    private fun anoVeiculo(carro: CarroInfo): Int? {
        val texto = listOf(carro.modelo, carro.nome).joinToString(" ")
        val ano = Regex("(19|20)\\d{2}").find(texto)?.value?.toIntOrNull()
        val anoAtual = LocalDate.now().year
        return ano?.takeIf { it in 1950..(anoAtual + 1) }
    }

    private fun idadeAnos(carro: CarroInfo): Int? {
        val ano = anoVeiculo(carro) ?: return null
        val idade = LocalDate.now().year - ano
        return if (idade in 0..60) idade else null
    }

    private fun normalize(text: String): String {
        val lower = text.lowercase()
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
    }
}
