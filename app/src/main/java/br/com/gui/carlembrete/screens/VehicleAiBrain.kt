package br.com.gui.carlembrete

import br.com.gui.carlembrete.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

suspend fun gerarRespostaIaGaragemComGroqOuLocal(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>
): String {
    if (!BuildConfig.AI_ONLINE_ENABLED) {
        return gerarRespostaIaGaragem(pergunta, carros, currentCarroId, lembretesAtivos, abastecimentos)
    }

    val proxyUrl = BuildConfig.AI_PROXY_URL.trim()
    if (proxyUrl.isNotBlank()) {
        return runCatching {
            chamarZelluAiProxy(
                proxyUrl = proxyUrl,
                pergunta = pergunta,
                carros = carros,
                currentCarroId = currentCarroId,
                lembretesAtivos = lembretesAtivos,
                abastecimentos = abastecimentos
            )
        }.getOrElse {
            gerarRespostaIaGaragem(pergunta, carros, currentCarroId, lembretesAtivos, abastecimentos) +
                "\n\nObs: tentei usar a IA online, mas ela nao respondeu agora. Usei a analise local para nao te deixar na mao."
        }
    }

    val apiKey = BuildConfig.GROQ_API_KEY.trim()
    if (apiKey.isBlank()) {
        return gerarRespostaIaGaragem(pergunta, carros, currentCarroId, lembretesAtivos, abastecimentos)
    }

    return runCatching {
        chamarGroqGarageAi(
            apiKey = apiKey,
            model = BuildConfig.GROQ_MODEL.ifBlank { "llama-3.1-8b-instant" },
            pergunta = pergunta,
            carros = carros,
            currentCarroId = currentCarroId,
            lembretesAtivos = lembretesAtivos,
            abastecimentos = abastecimentos
        )
    }.getOrElse {
        gerarRespostaIaGaragem(pergunta, carros, currentCarroId, lembretesAtivos, abastecimentos) +
            "\n\nObs: tentei usar a IA online, mas ela nao respondeu agora. Usei a analise local para nao te deixar na mao."
    }
}

suspend fun chamarZelluAiProxy(
    proxyUrl: String,
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>
): String = withContext(Dispatchers.IO) {
    val connection = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 30_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        BuildConfig.AI_PROXY_TOKEN.trim().takeIf { it.isNotBlank() }?.let {
            setRequestProperty("X-Zellu-App-Token", it)
        }
    }

    val body = JSONObject()
        .put("message", pergunta)
        .put("garageContext", montarResumoGaragem(carros, currentCarroId, lembretesAtivos, abastecimentos))
        .toString()

    connection.outputStream.use { output ->
        output.write(body.toByteArray(Charsets.UTF_8))
    }

    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    connection.disconnect()

    if (code !in 200..299) {
        error("Zellu AI proxy HTTP $code: $responseText")
    }

    val answer = JSONObject(responseText)
        .getString("answer")
        .trim()
        .ifBlank { error("Resposta vazia do proxy") }
    AdminUsageMetrics.markAiRequest()
    answer
}

suspend fun chamarGroqGarageAi(
    apiKey: String,
    model: String,
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>
): String = withContext(Dispatchers.IO) {
    val url = URL("https://api.groq.com/openai/v1/chat/completions")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 30_000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $apiKey")
        setRequestProperty("Content-Type", "application/json")
    }

    val body = JSONObject()
        .put("model", model)
        .put("temperature", 0.35)
        .put("max_completion_tokens", 420)
        .put(
            "messages",
            JSONArray()
                .put(JSONObject().put("role", "system").put("content", groqSystemPrompt()))
                .put(JSONObject().put("role", "user").put("content", montarContextoGroq(pergunta, carros, currentCarroId, lembretesAtivos, abastecimentos)))
        )
        .toString()

    connection.outputStream.use { output ->
        output.write(body.toByteArray(Charsets.UTF_8))
    }

    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    connection.disconnect()

    if (code !in 200..299) {
        error("Groq HTTP $code: $responseText")
    }

    val content = JSONObject(responseText)
        .getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
        .trim()
        .ifBlank { error("Resposta vazia da Groq") }
    AdminUsageMetrics.markAiRequest()
    content
}

fun groqSystemPrompt(): String {
    return """
Voce e a IA da Garagem do app Zellu.
Converse de forma natural, curta e amigavel em portugues do Brasil.
Voce pode falar sobre todos os veiculos cadastrados no contexto.
Se o usuario disser apenas oi, cumprimente e pergunte como pode ajudar com a garagem.
Use os avisos do app como contexto, mas nao invente defeitos nem dados.
Use os dados de abastecimento e consumo calculados pelo app quando a pergunta for sobre gasto, autonomia, km/l ou economia.
Ajude usuarios iniciantes explicando mecanica em linguagem simples quando perguntarem o que e, como saber, sinais, cuidados ou se algo parece grave.
Para criar aviso, peca titulo, veiculo e km limite se faltar. Para aviso, a hora fica como campo separado; se nao houver hora, use 09:00 como padrao.
Para registrar servico ja realizado, use titulo, veiculo, data de execucao e valor opcional. Nao exija km nem hora para esse tipo de registro.
Para abastecimento, peca veiculo, valor ou litros e km atual se faltar.
Use formatacao leve de AI: titulos em **negrito**, separadores com --- e listas curtas com -.
Nunca diga que um veiculo esta 100% seguro. Diga "pelos avisos cadastrados".
Nao substitua mecanico. Se houver risco em freios, pneus, oleo, motor, bateria ou revisao vencida, recomende revisar antes de viajar.
Quando comparar veiculos, priorize risco local, avisos vencidos, avisos criticos e proximos vencimentos.
Quando fizer sentido, inclua uma secao **Minha opiniao** com uma leitura direta do que voce faria primeiro, sempre baseada nos dados cadastrados.
Quando fizer sentido, responda com: veiculo indicado ou prioritario, risco, recomendacao pratica e proximos passos.
""".trimIndent()
}

fun montarContextoGroq(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>
): String {
    val garagem = montarResumoGaragem(carros, currentCarroId, lembretesAtivos, abastecimentos)

    return """
Pergunta do usuario:
$pergunta

Resumo compacto da garagem:
$garagem
""".trimIndent()
}

fun montarResumoGaragem(
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>
): String {
    val statuses = calcularStatusGaragem(carros, lembretesAtivos, abastecimentos)
    if (statuses.isEmpty()) return "- Nenhum veiculo cadastrado."
    return statuses.joinToString("\n") { status ->
        val carro = status.carro
        val selecionado = if (carro.id == currentCarroId) "sim" else "nao"
        """
- ${carro.nome}
  selecionado agora: $selecionado
  marca/modelo: ${carro.marca.ifBlank { "nao informada" }} / ${carro.modelo.ifBlank { "nao informado" }}
  tipo: ${carro.tipoVeiculo.label}
  km atual: ${if (carro.semControleKm) "sem controle de km" else carro.kmAtual.toString()}
  risco local: ${status.risk.label}
  avisos ativos: ${status.activeCount}
  avisos vencidos: ${status.overdueCount}
  proximos 30 dias: ${status.next30Count}
  principais avisos: ${status.topWarnings.ifBlank { "nenhum aviso tecnico ativo" }}
  abastecimentos registrados: ${status.fuel.fuelCount}
  total abastecido: ${formatarMoedaAi(status.fuel.totalCost)} em ${formatarNumero(status.fuel.totalLiters)} litros
  consumo medio calculado: ${status.fuel.kmPerLiter?.let { "${formatarNumero(it)} km/l" } ?: "sem km suficiente"}
  custo por km calculado: ${status.fuel.costPerKm?.let { "${formatarMoedaAi(it)}/km" } ?: "sem km suficiente"}
  distancia usada no calculo: ${status.fuel.distanceKm?.let { "$it km" } ?: "sem km suficiente"}
  ultimo abastecimento: ${status.fuel.lastFuelDate ?: "nao informado"}
""".trimIndent()
    }
}

fun calcularStatusGaragem(
    carros: List<CarroInfo>,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento> = emptyList()
): List<GarageVehicleStatus> {
    val hoje = LocalDate.now()
    return carros.map { carro ->
        val avisos = lembretesAtivos
            .filter { it.carroId == carro.id }
            .filter { it.tipo != TipoManutencao.ABASTECIMENTO }
        val vencidos = avisos.count { dataParaOrdenacao(it).isBefore(hoje) }
        val proximos = avisos.count {
            val data = dataParaOrdenacao(it)
            !data.isBefore(hoje) && ChronoUnit.DAYS.between(hoje, data) <= 30
        }
        val topWarnings = avisos
            .sortedWith(compareBy<Lembrete> { !dataParaOrdenacao(it).isBefore(hoje) }.thenBy { dataParaOrdenacao(it) })
            .take(3)
            .joinToString("; ") {
                "${it.tipo.label}: ${it.titulo.ifBlank { it.peca.ifBlank { "aviso ativo" } }}"
            }
        GarageVehicleStatus(
            carro = carro,
            risk = calcularRiscoVeiculo(avisos),
            activeCount = avisos.size,
            overdueCount = vencidos,
            next30Count = proximos,
            topWarnings = topWarnings,
            fuel = calcularResumoConsumo(carro, abastecimentos)
        )
    }.sortedWith(
        compareByDescending<GarageVehicleStatus> { it.risk.ordinal }
            .thenByDescending { it.overdueCount }
            .thenByDescending { it.activeCount }
            .thenBy { it.carro.nome }
    )
}

fun gerarRespostaIaGaragem(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>
): String {
    val perguntaNormalizada = pergunta.lowercase(Locale.getDefault())
    val perguntaLimpa = normalizarMensagemChat(perguntaNormalizada)
    val isResumoInicial = perguntaLimpa == "resumo rapido"
    val statuses = calcularStatusGaragem(carros, lembretesAtivos, abastecimentos)
    val atual = carros.firstOrNull { it.id == currentCarroId } ?: carros.firstOrNull()
    val melhorParaUso = statuses.minWithOrNull(compareBy<GarageVehicleStatus> { it.risk.ordinal }.thenBy { it.overdueCount })
    val prioridade = statuses.maxWithOrNull(compareBy<GarageVehicleStatus> { it.risk.ordinal }.thenBy { it.overdueCount })

    if (!isResumoInicial && isGreetingOnly(perguntaLimpa)) {
        return "**Oi! Bora cuidar da garagem?**\n---\nPosso olhar tudo ou focar no ${atual?.nome ?: "veiculo atual"}.\n- Melhor para viajar\n- Revisao mais urgente\n- Consumo, km/l e gasto por km\n- Explicar mecanica basica sem complicar"
    }

    if (!isResumoInicial && isThanksOnly(perguntaLimpa)) {
        return "**Tamo junto!**\n---\nQuando quiser, manda outra pergunta sobre qualquer veiculo da garagem."
    }

    if (!isResumoInicial && isGenericRegistrationRequest(perguntaLimpa)) {
        return gerarRespostaRegistroGuiado(atual)
    }

    if (!isResumoInicial && isBeginnerMechanicQuestion(perguntaLimpa)) {
        return gerarRespostaMecanicaBasica(perguntaLimpa)
    }

    if (!isResumoInicial && !isVehicleQuestion(perguntaLimpa)) {
        return "**Ainda nao peguei bem esse pedido**\n---\nTenta mandar de um jeito mais ligado a sua garagem, que eu consigo ajudar melhor.\n\n**Posso ajudar com**\n- Avisos e revisoes\n- Consumo, abastecimento e custo por km\n- Melhor veiculo para viagem\n- Relatorios da frota ou de um veiculo\n- Duvidas simples de mecanica\n\n**Exemplos**\n- Qual veiculo devo revisar primeiro?\n- Quanto o Gol consumiu este mes?\n- Criar aviso de oleo para o Crossfox em 90000 km\n- Como sei se o pneu esta ruim?\n\nSe for sobre outro assunto, talvez eu nao seja a melhor pessoa. Eu sou mais forte cuidando dos seus veiculos."
    }

    val distanciaKm = Regex("(\\d{2,5})\\s*km").find(perguntaNormalizada)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    if (statuses.isEmpty()) {
        return "**Garagem vazia por enquanto**\n---\nAinda nao encontrei veiculos cadastrados para analisar. Assim que tiver uma garagem, eu comparo avisos, consumo e prioridades."
    }

    val resumoFormatado = statuses.take(5).joinToString("\n") {
        "- ${it.carro.nome}: risco ${it.risk.label}, ${it.activeCount} avisos, ${it.overdueCount} vencidos"
    }

    return when {
        isIntervalQuestion(perguntaLimpa) -> {
            gerarRespostaIntervaloManutencao(perguntaLimpa)
        }
        isConsumptionQuestion(perguntaLimpa) -> {
            gerarRespostaConsumoLocal(
                pergunta = pergunta,
                statuses = statuses,
                carros = carros,
                currentCarroId = currentCarroId,
                abastecimentos = abastecimentos
            )
        }
        isFleetStatusQuestion(perguntaLimpa) -> {
            gerarRespostaStatusFrotaLocal(statuses, melhorParaUso, prioridade)
        }
        perguntaNormalizada.contains("viagem") || distanciaKm != null || perguntaNormalizada.contains("viajar") -> {
            val indicado = melhorParaUso ?: return "Nao consegui comparar os veiculos agora."
            gerarRespostaViagemLocal(indicado, distanciaKm)
        }
        perguntaNormalizada.contains("primeiro") || perguntaNormalizada.contains("urgente") || perguntaNormalizada.contains("pior") || perguntaNormalizada.contains("revisar") -> {
            val alvo = prioridade ?: return "Nao consegui definir prioridade agora."
            "**Prioridade de revisao**\n---\nEu priorizaria o **${alvo.carro.nome}**.\n- Risco: ${alvo.risk.label}\n- Avisos ativos: ${alvo.activeCount}\n- Vencidos: ${alvo.overdueCount}\n\n**Minha opiniao**\n- Se fosse minha garagem, eu resolveria esse veiculo primeiro e depois revisaria a fila restante.\n- Motivo: ele concentra o maior ponto de atencao pelos avisos cadastrados.\n\n**Fila da garagem**\n$resumoFormatado"
        }
        else -> {
            "**Resumo da garagem**\n---\n$resumoFormatado\n\n**Minha opiniao**\n- Eu usaria o **${melhorParaUso?.carro?.nome ?: "veiculo com menor risco"}** se precisasse escolher agora.\n- Eu olharia primeiro o **${prioridade?.carro?.nome ?: "veiculo com mais avisos"}**, porque ele parece pedir mais atencao.\n\n**Leitura rapida**\n- Melhor opcao hoje: ${melhorParaUso?.carro?.nome ?: "nao definido"}\n- Maior prioridade: ${prioridade?.carro?.nome ?: "nao definido"}"
        }
    }
}

fun gerarRespostaViagemLocal(
    indicado: GarageVehicleStatus,
    distanciaKm: Int?
): String {
    val complemento = when (indicado.risk) {
        VehicleRiskLevel.HIGH -> "Mesmo sendo a melhor opcao encontrada, ainda tem risco alto. Eu revisaria antes."
        VehicleRiskLevel.MEDIUM -> "Da para considerar, mas eu faria um checklist antes de sair."
        VehicleRiskLevel.LOW -> "Pelos avisos cadastrados, parece uma boa escolha para rodar."
    }
    val fuel = indicado.fuel
    val consumo = fuel.kmPerLiter?.let { "${formatarNumero(it)} km/l" } ?: "sem km suficiente para calcular"
    val custoKm = fuel.costPerKm?.let { "${formatarMoedaAi(it)}/km" } ?: "sem custo por km calculado"
    val custoEstimado = if (distanciaKm != null && fuel.costPerKm != null) {
        "\n- Custo estimado para $distanciaKm km: ${formatarMoedaAi(fuel.costPerKm * distanciaKm)}"
    } else {
        ""
    }
    val avisos = when {
        indicado.topWarnings.isNotBlank() -> indicado.topWarnings
        indicado.activeCount == 0 -> "nenhum aviso tecnico ativo"
        else -> "${indicado.activeCount} aviso(s) ativo(s)"
    }

    return "**Melhor opcao para viagem**\n---\nEu escolheria o **${indicado.carro.nome}**${distanciaKm?.let { " para $it km" }.orEmpty()}.\n\n**Minha opiniao**\n- Pelos dados cadastrados, esse e o veiculo que eu colocaria na frente hoje.\n- Ainda assim, eu faria um check rapido de pneus, oleo, freios, luzes e temperatura antes de sair.\n\n**Por que ele**\n- Risco: ${indicado.risk.label}\n- Avisos vencidos: ${indicado.overdueCount}\n- Pontos de atencao: $avisos\n- Leitura: $complemento\n\n**Consumo desse veiculo**\n- Media calculada: $consumo\n- Custo por km: $custoKm$custoEstimado\n- Total registrado em abastecimentos: ${formatarMoedaAi(fuel.totalCost)} em ${formatarNumero(fuel.totalLiters)} L"
}

fun gerarRespostaStatusFrotaLocal(
    statuses: List<GarageVehicleStatus>,
    melhorParaUso: GarageVehicleStatus?,
    prioridade: GarageVehicleStatus?
): String {
    val totalVeiculos = statuses.size
    val totalAvisos = statuses.sumOf { it.activeCount }
    val totalVencidos = statuses.sumOf { it.overdueCount }
    val altoRisco = statuses.count { it.risk == VehicleRiskLevel.HIGH }
    val atencao = statuses.count { it.risk == VehicleRiskLevel.MEDIUM }
    val baixo = statuses.count { it.risk == VehicleRiskLevel.LOW }
    val listaCurta = statuses.take(5).joinToString("\n") {
        "- ${it.carro.nome}: risco ${it.risk.label}, ${it.activeCount} avisos, ${it.overdueCount} vencidos"
    }
    val leitura = when {
        totalVeiculos == 0 -> "Ainda nao tenho veiculos para analisar."
        totalVencidos > 0 -> "Sua frota precisa de atencao porque existem avisos vencidos."
        altoRisco > 0 -> "Sua frota tem pontos importantes para revisar antes de rodar tranquilo."
        totalAvisos > 0 -> "Sua frota esta acompanhada, mas ainda tem avisos para monitorar."
        else -> "Sua frota parece tranquila pelos dados cadastrados agora."
    }

    return "**Status da frota**\n---\n$leitura\n\n**Visao geral**\n- Veiculos cadastrados: $totalVeiculos\n- Avisos ativos: $totalAvisos\n- Avisos vencidos: $totalVencidos\n- Alto risco: $altoRisco\n- Atencao: $atencao\n- Baixo risco: $baixo\n\n**Minha opiniao**\n- Melhor opcao hoje: **${melhorParaUso?.carro?.nome ?: "nao definido"}**\n- Primeiro para olhar: **${prioridade?.carro?.nome ?: "nao definido"}**\n\n**Resumo por veiculo**\n$listaCurta\n\nSe quiser, posso detalhar um veiculo especifico ou gerar os relatorios da frota."
}

fun gerarRespostaRegistroGuiado(carroAtual: CarroInfo?): String {
    val nomeVeiculo = carroAtual?.nome ?: "Gol"
    return "**Qual registro voce quer fazer?**\n---\nConsigo te guiar em tres caminhos diferentes.\n\n**Servico ja realizado**\n- Exemplo: registrei troca de oleo no $nomeVeiculo hoje por 250 reais\n- Vai para o historico e nao cria aviso futuro\n\n**Abastecimento**\n- Exemplo: abastecimento de 30 reais no $nomeVeiculo com 85400 km\n- Usa km atual para calcular consumo depois\n\n**Aviso de manutencao**\n- Exemplo: criar aviso para trocar oleo do $nomeVeiculo em 90000 km dia 28/11/2026\n- A hora fica separada no card; sem hora, uso 09:00\n\nManda desse jeitinho que eu preparo o card para confirmar antes de salvar."
}

fun gerarRespostaMecanicaBasica(text: String): String {
    val assunto = detectarAssuntoMecanicaBasica(text)
    val texto = normalizarParaBusca(text)
    return when (assunto) {
        TipoManutencao.OLEO -> {
            if (listOf("verificar", "checar", "conferir", "nivel").any { texto.contains(it) }) {
                "**Como verificar o oleo**\n---\nDe um jeito simples: o oleo precisa estar no nivel certo e com aspecto normal para proteger o motor.\n\n**Passo a passo basico**\n- Deixe o carro em local plano e desligado por alguns minutos\n- Veja no manual se o seu carro usa vareta ou leitura no painel\n- Se tiver vareta, limpe, coloque de novo e confira se esta entre minimo e maximo\n- Nao complete com qualquer oleo: use a especificacao correta do manual\n\n**Minha opiniao**\n- Se a luz de oleo acender ou o nivel estiver muito baixo, eu nao arriscaria rodar. Melhor parar com seguranca e revisar."
            } else {
                "**Explicando simples: oleo**\n---\nO oleo ajuda o motor a trabalhar com menos atrito e menos desgaste. Pensa nele como uma protecao interna do motor.\n\n**Sinais para observar**\n- luz de oleo no painel\n- motor mais barulhento\n- vazamento embaixo do carro\n- troca muito atrasada por km ou tempo\n\n**Minha opiniao**\n- Se voce nao lembra quando trocou, eu criaria um aviso preventivo com km. Oleo atrasado e pequeno no custo, mas grande no prejuizo."
            }
        }
        TipoManutencao.PNEU -> "**Explicando simples: pneus**\n---\nPneu e o contato do carro com o chao. Ele influencia freio, estabilidade, consumo e seguranca na chuva.\n\n**Sinais para observar**\n- pneu careca ou no TWI\n- bolha, rachadura ou corte\n- carro puxando para um lado\n- desgaste mais forte em um lado do pneu\n\n**Minha opiniao**\n- Eu nao deixaria pneu ruim para depois. Se tiver duvida visual, vale revisar antes de viajar."
        TipoManutencao.FREIO -> "**Explicando simples: freios**\n---\nFreio envolve pastilhas, discos, fluido e outros itens que ajudam o carro a parar com seguranca.\n\n**Sinais para observar**\n- chiado forte ou raspando\n- pedal baixo ou estranho\n- volante vibrando ao frear\n- carro demorando mais para parar\n\n**Minha opiniao**\n- Freio e prioridade. Se apareceu barulho novo ou o pedal mudou, eu revisaria antes de continuar usando pesado."
        TipoManutencao.BATERIA -> "**Explicando simples: bateria**\n---\nA bateria ajuda a dar partida e alimenta sistemas eletricos quando o carro precisa.\n\n**Sinais para observar**\n- partida pesada ou lenta\n- painel apagando ao ligar\n- luzes fracas\n- bateria com mais de 2 ou 3 anos\n\n**Minha opiniao**\n- Se a partida ja esta ficando pesada, eu testaria a bateria antes dela te deixar na mao."
        TipoManutencao.MECANICA -> "**Explicando simples: motor/mecanica**\n---\nMotor, correias, arrefecimento e suspensao sao partes que merecem atencao quando aparece barulho, cheiro, luz no painel ou mudanca no comportamento.\n\n**Sinais para observar**\n- barulho novo\n- cheiro de queimado\n- luz acesa no painel\n- temperatura subindo\n- perda de forca\n\n**Minha opiniao**\n- Quando muda o comportamento do carro, eu nao ignoraria. Melhor registrar o sintoma e revisar cedo."
        TipoManutencao.REVISAO -> "**Explicando simples: revisao**\n---\nRevisao e um check-up do veiculo. Serve para encontrar problemas antes de virarem dor de cabeca.\n\n**O que costuma entrar**\n- oleo e filtros\n- freios\n- pneus\n- luzes\n- bateria\n- arrefecimento\n\n**Minha opiniao**\n- Se voce nao sabe por onde comecar, eu faria uma revisao geral e criaria avisos por km para os proximos itens."
        else -> "**Explicando simples**\n---\nPosso traduzir mecanica para uma linguagem mais facil. Me pergunte, por exemplo:\n- O que e arrefecimento?\n- Como sei se o pneu esta ruim?\n- Freio fazendo barulho e grave?\n- Quando trocar oleo?\n- Bateria da sinal antes de acabar?\n\n**Minha opiniao**\n- Para quem nao entende muito de mecanica, o melhor caminho e observar sinais simples e criar avisos por km para nao depender da memoria."
    }
}

fun gerarRespostaIntervaloManutencao(text: String): String {
    val tipo = detectarTipoAvisoIa(text)
    val texto = normalizarParaBusca(text)
    val resposta = when {
        texto.contains("correia") -> "**Troca de correia**\n---\nCorreia depende muito do modelo e do manual, mas muita gente usa como referencia algo entre **40.000 e 80.000 km** ou alguns anos de uso.\n- Se for correia dentada, nao vale brincar: quando quebra, o prejuizo pode ser grande\n- Veja o manual do veiculo para o prazo correto\n- Se voce nao sabe quando foi trocada, trate como prioridade de revisao\n\n**Minha opiniao**\n- Eu criaria um aviso por km e confirmaria no manual ou com mecanico. Correia e barata perto do estrago que pode causar."
        tipo == TipoManutencao.PNEU -> "**Troca de pneu**\n---\nEm media, pneu costuma durar entre **40.000 e 60.000 km**, mas depende muito de alinhamento, calibragem, peso e tipo de uso.\n- Confira desgaste a cada 10.000 km\n- Faca rodizio quando fizer sentido\n- Troque antes se tiver bolha, rachadura, desgaste irregular ou TWI no limite"
        tipo == TipoManutencao.OLEO -> "**Troca de oleo**\n---\nNa maioria dos carros, a troca fica entre **6 meses ou 10.000 km**, mas o manual manda mais que qualquer media.\n- Uso severo pede intervalo menor\n- Sempre confira filtro de oleo junto"
        tipo == TipoManutencao.FREIO -> "**Freios**\n---\nPastilhas variam bastante, mas uma checagem a cada **10.000 km** e bem prudente.\n- Chiado, vibracao ou pedal baixo pedem revisao antes\n- Fluido de freio geralmente entra em revisao periodica"
        tipo == TipoManutencao.BATERIA -> "**Bateria**\n---\nBateria costuma durar em media **2 a 3 anos**.\n- Partida pesada, luz fraca ou falhas eletricas sao sinais de atencao\n- Calor e pouco uso podem reduzir a vida util"
        tipo == TipoManutencao.REVISAO -> "**Revisao geral**\n---\nUma boa media e revisar a cada **6 a 12 meses** ou a cada **10.000 km**.\n- Antes de viagem, vale checar pneus, oleo, freios, luzes e arrefecimento"
        else -> "**Media de manutencao**\n---\nDepende da peca, uso e manual do veiculo. Como regra boa:\n- Itens criticos: revisar antes de viagem\n- Itens de desgaste: acompanhar por km e sintomas\n- Quando tiver duvida, criar um aviso preventivo e melhor que esquecer"
    }
    val aviso = if (texto.contains("correia")) "correia" else tipo.label.lowercase(Locale.getDefault())
    return "$resposta\n\n**Quer que eu crie um aviso?**\nManda tipo: criar aviso para revisar $aviso do Gol em 90000 km daqui 30 dias."
}

fun isIntervalQuestion(text: String): Boolean {
    val asksInterval = listOf(
        "quanto tempo",
        "tempo medio",
        "tempo em media",
        "media de troca",
        "quando trocar",
        "quando revisar",
        "durabilidade",
        "dura quanto",
        "quantos km"
    ).any { text.contains(it) }
    return asksInterval && listOf(
        "pneu",
        "pneus",
        "oleo",
        "freio",
        "bateria",
        "revisao",
        "manutencao",
        "correia"
    ).any { text.contains(it) }
}

fun gerarRespostaConsumoLocal(
    pergunta: String,
    statuses: List<GarageVehicleStatus>,
    carros: List<CarroInfo>,
    currentCarroId: String,
    abastecimentos: List<Abastecimento>
): String {
    val period = detectarPeriodoConsumo(pergunta)
    if (period != null) {
        return gerarRespostaConsumoPorPeriodo(pergunta, period, carros, currentCarroId, abastecimentos)
    }

    val comConsumo = statuses.filter { it.fuel.fuelCount > 0 }
    if (comConsumo.isEmpty()) {
        return "**Consumo da garagem**\n---\nAinda nao encontrei abastecimentos registrados.\n- Quando voce cadastrar valor, litros e km, eu consigo falar de km/l e custo por km."
    }

    val melhorMedia = comConsumo
        .filter { it.fuel.kmPerLiter != null }
        .maxByOrNull { it.fuel.kmPerLiter ?: 0.0 }
    val maiorCusto = comConsumo
        .filter { it.fuel.costPerKm != null }
        .maxByOrNull { it.fuel.costPerKm ?: 0.0 }
    val totalGasto = comConsumo.sumOf { it.fuel.totalCost }
    val totalLitros = comConsumo.sumOf { it.fuel.totalLiters }
    val linhas = comConsumo.take(5).joinToString("\n") { status ->
        val fuel = status.fuel
        "- ${status.carro.nome}: ${fuel.kmPerLiter?.let { "${formatarNumero(it)} km/l" } ?: "sem km/l"}, ${fuel.costPerKm?.let { "${formatarMoedaAi(it)}/km" } ?: "sem custo/km"}, ${formatarMoedaAi(fuel.totalCost)} no total"
    }

    val leitura = when {
        melhorMedia == null -> "Tem abastecimento salvo, mas falta km em pelo menos dois registros do mesmo veiculo para calcular km/l."
        maiorCusto != null && maiorCusto.carro.id != melhorMedia.carro.id -> "O **${melhorMedia.carro.nome}** aparece mais economico em km/l. O **${maiorCusto.carro.nome}** merece atencao no custo por km."
        else -> "O **${melhorMedia.carro.nome}** aparece como melhor leitura de consumo pelos dados cadastrados."
    }

    return "**Consumo da garagem**\n---\n$linhas\n\n**Minha opiniao**\n- $leitura\n- Se quiser comparar melhor, eu manteria todos os proximos abastecimentos com km informado.\n- Total abastecido registrado: ${formatarMoedaAi(totalGasto)} em ${formatarNumero(totalLitros)} litros\n- Esses calculos usam os dados que o app ja tem, sem a IA inventar numero."
}

data class ConsumptionPeriod(
    val label: String,
    val start: LocalDate,
    val end: LocalDate
)

fun detectarPeriodoConsumo(pergunta: String): ConsumptionPeriod? {
    val text = normalizarParaBusca(pergunta)
    val today = LocalDate.now()
    return when {
        text.contains("hoje") -> ConsumptionPeriod(
            label = "hoje",
            start = today,
            end = today
        )
        text.contains("semana") -> ConsumptionPeriod(
            label = "esta semana",
            start = today.minusDays((today.dayOfWeek.value - 1).toLong()),
            end = today
        )
        text.contains("mes passado") -> {
            val lastMonth = today.minusMonths(1)
            ConsumptionPeriod(
                label = "mes passado",
                start = lastMonth.withDayOfMonth(1),
                end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            )
        }
        text.contains("mes") || text.contains("mensal") -> ConsumptionPeriod(
            label = "este mes",
            start = today.withDayOfMonth(1),
            end = today
        )
        text.contains("ano") -> ConsumptionPeriod(
            label = "este ano",
            start = today.withDayOfYear(1),
            end = today
        )
        else -> null
    }
}

fun gerarRespostaConsumoPorPeriodo(
    pergunta: String,
    period: ConsumptionPeriod,
    carros: List<CarroInfo>,
    currentCarroId: String,
    abastecimentos: List<Abastecimento>
): String {
    val veiculos = detectarVeiculosNoTexto(pergunta, carros, currentCarroId)
        .ifEmpty { carros.firstOrNull { it.id == currentCarroId }?.let(::listOf).orEmpty() }
    if (veiculos.isEmpty()) {
        return "**Consumo por periodo**\n---\nNao encontrei veiculo para calcular. Me fala o nome do veiculo, tipo: quanto o Gol consumiu no mes?"
    }

    val linhas = veiculos.map { carro ->
        val registros = abastecimentos
            .filter { it.carroId == carro.id }
            .filter { item ->
                val data = parseFuelDate(item.data)
                data != null && !data.isBefore(period.start) && !data.isAfter(period.end)
            }
        val totalGasto = registros.sumOf { it.valorPago }
        val totalLitros = registros.sumOf { it.litros }
        val kmInformados = registros.mapNotNull { it.km }.sorted()
        val distancia = if (kmInformados.size >= 2) kmInformados.last() - kmInformados.first() else null
        val kmPorLitro = if (distancia != null && distancia > 0 && totalLitros > 0.0) distancia / totalLitros else null
        val custoPorKm = if (distancia != null && distancia > 0 && totalGasto > 0.0) totalGasto / distancia else null

        if (registros.isEmpty()) {
            "- **${carro.nome}**: nenhum abastecimento registrado em ${period.label}."
        } else {
            "- **${carro.nome}**: ${formatarMoedaAi(totalGasto)} em ${formatarNumero(totalLitros)} L, ${registros.size} registro${if (registros.size == 1) "" else "s"}" +
                "\n  Media no periodo: ${kmPorLitro?.let { "${formatarNumero(it)} km/l" } ?: "sem km suficiente"}" +
                "\n  Custo por km: ${custoPorKm?.let { "${formatarMoedaAi(it)}/km" } ?: "sem km suficiente"}"
        }
    }.joinToString("\n")

    return "**Consumo de ${period.label}**\n---\n$linhas\n\n**Minha opiniao**\n- Eu usaria esse recorte para acompanhar tendencia de gasto, mas nao tiraria conclusao de km/l sem pelo menos dois abastecimentos com km.\n- Se o gasto subir muito no proximo periodo, vale comparar com pneu, alinhamento, rota e tipo de combustivel.\n\n**Periodo usado**\n- De ${period.start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} ate ${period.end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\n- Para calcular km/l no periodo, preciso de pelo menos dois abastecimentos com km informado."
}

fun calcularResumoConsumo(
    carro: CarroInfo,
    abastecimentos: List<Abastecimento>
): GarageFuelSummary {
    val registros = abastecimentos
        .filter { it.carroId == carro.id }
        .filter { it.litros > 0.0 || it.valorPago > 0.0 }
    if (registros.isEmpty()) return GarageFuelSummary()

    val totalCost = registros.sumOf { it.valorPago }
    val totalLiters = registros.sumOf { it.litros }
    val ultimo = registros.maxWithOrNull(
        compareBy<Abastecimento> { parseFuelDate(it.data) ?: LocalDate.MIN }
            .thenBy { it.km ?: Int.MIN_VALUE }
    )

    val comKm = registros
        .filter { (it.km ?: 0) > 0 && it.litros > 0.0 }
        .sortedWith(
            compareBy<Abastecimento> { it.km ?: 0 }
                .thenBy { parseFuelDate(it.data) ?: LocalDate.MIN }
        )
    val firstKm = comKm.firstOrNull()?.km
    val lastKm = comKm.lastOrNull()?.km
    val distanceKm = if (firstKm != null && lastKm != null && lastKm > firstKm) lastKm - firstKm else null
    val consumoBase = if (distanceKm != null) comKm.drop(1) else emptyList()
    val litrosBase = consumoBase.sumOf { it.litros }
    val custoBase = consumoBase.sumOf { it.valorPago }
    val kmPerLiter = if (distanceKm != null && litrosBase > 0.0) distanceKm.toDouble() / litrosBase else null
    val costPerKm = if (distanceKm != null && distanceKm > 0 && custoBase > 0.0) custoBase / distanceKm.toDouble() else null

    return GarageFuelSummary(
        fuelCount = registros.size,
        totalCost = totalCost,
        totalLiters = totalLiters,
        distanceKm = distanceKm,
        kmPerLiter = kmPerLiter,
        costPerKm = costPerKm,
        lastFuelDate = ultimo?.data?.takeIf { it.isNotBlank() }
    )
}

fun parseFuelDate(raw: String): LocalDate? {
    val value = raw.trim()
    if (value.isBlank()) return null
    val formats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ISO_LOCAL_DATE
    )
    return formats.firstNotNullOfOrNull { formatter ->
        runCatching { LocalDate.parse(value, formatter) }.getOrNull()
    }
}

fun formatarMoedaAi(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

fun formatarNumero(value: Double): String {
    return String.format(Locale("pt", "BR"), "%.1f", value)
}

fun criarRascunhosDeAviso(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String
): List<AiReminderDraft>? {
    val texto = pergunta.lowercase(Locale.getDefault())
    val querCriar = listOf(
        "cria",
        "criar",
        "cadastre",
        "cadastrar",
        "adicione",
        "adicionar",
        "me lembra",
        "lembra",
        "aviso",
        "avisos",
        "lembrete",
        "lembretes"
    ).any { texto.contains(it) }
    if (!querCriar) return null

    val veiculos = detectarVeiculosNoTexto(pergunta, carros, currentCarroId)
    val tipo = detectarTipoAvisoIa(pergunta)
    val titulo = detectarTituloAvisoIa(pergunta, tipo)
    val dataLimite = detectarDataLimiteIa(pergunta)
    val kmLimite = detectarKmLimiteIa(pergunta)
    val horaAviso = detectarHoraAvisoIa(pergunta)

    if (veiculos.isEmpty() || kmLimite.isBlank()) {
        return emptyList()
    }

    return veiculos.map { carro ->
        AiReminderDraft(
            carro = carro,
            titulo = titulo,
            peca = titulo,
            dataLimite = dataLimite,
            kmLimite = kmLimite,
            tipo = tipo,
            horaAviso = horaAviso ?: "09:00",
            horaInformada = horaAviso != null
        )
    }
}

fun Lembrete.isSimilarTo(draft: AiReminderDraft): Boolean {
    if (carroId != draft.carro.id) return false
    if (tipo != draft.tipo) return false
    val tituloAtual = normalizarParaBusca(titulo)
    val tituloDraft = normalizarParaBusca(draft.titulo)
    val pecaAtual = normalizarParaBusca(peca)
    val pecaDraft = normalizarParaBusca(draft.peca)
    val titleMatch = listOf(
        tituloAtual to tituloDraft,
        pecaAtual to pecaDraft
    ).any { (current, incoming) ->
        current.length >= 3 && incoming.length >= 3 && (current.contains(incoming) || incoming.contains(current))
    }
    val sameDate = dataLimite.isNotBlank() && dataLimite == draft.dataLimite
    val sameKm = kmLimite.isNotBlank() && kmLimite == draft.kmLimite
    return titleMatch || sameDate || sameKm
}

fun AiReminderDraft.matchesIdentity(other: AiReminderDraft): Boolean {
    return carro.id == other.carro.id &&
        tipo == other.tipo &&
        normalizarParaBusca(titulo) == normalizarParaBusca(other.titulo) &&
        dataLimite == other.dataLimite &&
        kmLimite == other.kmLimite &&
        horaAviso == other.horaAviso
}

fun criarRascunhosDeServicoRealizado(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String
): List<AiServiceRecordDraft>? {
    val texto = normalizarParaBusca(pergunta)
    if (isFuelRecordText(texto)) return null
    val querRegistrarFeito = listOf(
        "registrei",
        "registrar servico",
        "registrar manutencao",
        "cadastre que fiz",
        "cadastrar que fiz",
        "ja fiz",
        "fiz ",
        "feito",
        "realizei",
        "troquei",
        "paguei"
    ).any { texto.contains(it) }
    if (!querRegistrarFeito) return null

    val veiculos = detectarVeiculosNoTexto(pergunta, carros, currentCarroId)
    if (veiculos.isEmpty()) return emptyList()

    val tipo = detectarTipoAvisoIa(pergunta)
    val titulo = detectarTituloServicoRealizadoIa(pergunta, tipo)
    val dataExecucao = detectarDataLimiteIa(pergunta).ifBlank {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
    val valor = detectarValorMonetario(pergunta)
    val descricao = buildList {
        add("Servico realizado")
        if (valor != null) add("Valor: ${formatarMoedaAi(valor)}")
    }.joinToString(" - ")

    return veiculos.map { carro ->
        AiServiceRecordDraft(
            carro = carro,
            titulo = titulo,
            descricao = descricao,
            dataExecucao = dataExecucao,
            tipo = tipo,
            valor = valor
        )
    }
}

fun criarRascunhosDeAbastecimento(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String,
    abastecimentos: List<Abastecimento>
): List<AiFuelDraft>? {
    val texto = normalizarParaBusca(pergunta)
    val querRegistrar = isFuelRecordText(texto)
    if (!querRegistrar) return null

    val veiculos = detectarVeiculosNoTexto(pergunta, carros, currentCarroId)
    val valorPago = detectarValorAbastecimento(pergunta)
    val litrosInformados = detectarLitros(pergunta)
    val precoInformado = detectarPrecoLitro(pergunta)
    val km = detectarKmInteiro(pergunta)
    val tipoCombustivel = detectarTipoCombustivel(pergunta) ?: "Gasolina"

    if (veiculos.isEmpty()) return emptyList()
    if (valorPago == null && litrosInformados == null) return emptyList()
    if (km == null) return emptyList()

    val hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    return veiculos.map { carro ->
        val ultimoPreco = abastecimentos
            .filter { it.carroId == carro.id && it.precoLitro > 0.0 }
            .maxWithOrNull(compareBy<Abastecimento> { parseFuelDate(it.data) ?: LocalDate.MIN })
            ?.precoLitro
        val precoLitro = precoInformado ?: ultimoPreco ?: 5.60
        val valor = valorPago ?: ((litrosInformados ?: 0.0) * precoLitro)
        val litros = litrosInformados ?: (valor / precoLitro)
        AiFuelDraft(
            carro = carro,
            valorPago = valor,
            precoLitro = precoLitro,
            litros = litros,
            data = hoje,
            km = km,
            tipoCombustivel = tipoCombustivel,
            precoEstimado = precoInformado == null
        )
    }
}

fun AiFuelDraft.toAbastecimento(): Abastecimento {
    return Abastecimento(
        carroId = carro.id,
        data = data,
        precoLitro = precoLitro,
        valorPago = valorPago,
        litros = litros,
        tipoCombustivel = tipoCombustivel,
        km = km
    )
}

private fun detectarTipoCombustivel(textoOriginal: String): String? {
    val texto = normalizarParaBusca(textoOriginal)
    return when {
        "etanol" in texto || "alcool" in texto || "álcool" in texto -> "Etanol"
        "diesel" in texto -> "Diesel"
        "gnv" in texto || "gas natural" in texto -> "GNV"
        "flex" in texto -> "Flex"
        "gasolina" in texto -> "Gasolina"
        else -> null
    }
}

fun AiReminderDraft.toLembrete(): Lembrete {
    return Lembrete(
        carroId = carro.id,
        titulo = titulo,
        peca = peca,
        dataLimite = dataLimite,
        kmLimite = kmLimite,
        tipo = tipo,
        horaAviso = horaAviso
    )
}

fun AiServiceRecordDraft.toLembreteRealizado(): Lembrete {
    val data = runCatching {
        LocalDate.parse(dataExecucao, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrElse { LocalDate.now() }
    return marcarLembreteComoRealizado(
        Lembrete(
            carroId = carro.id,
            titulo = titulo,
            peca = descricao,
            dataLimite = dataExecucao,
            kmLimite = "",
            tipo = tipo,
            valor = valor ?: 0.0,
            horaAviso = "00:00"
        ),
        data
    )
}

fun detectarValorMonetario(pergunta: String): Double? {
    val texto = pergunta.lowercase(Locale.getDefault())
    val patterns = listOf(
        Regex("(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)\\s*(?:reais|real|rs)"),
        Regex("(?:valor|total|abasteci|coloquei)\\s*(?:de|com)?\\s*(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)")
    )
    return patterns.firstNotNullOfOrNull { regex ->
        regex.find(texto)?.groupValues?.getOrNull(1)?.parseNumeroPtBr()
    }
}

fun detectarValorAbastecimento(pergunta: String): Double? {
    val texto = pergunta.lowercase(Locale.getDefault())
    Regex("(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)\\s*(?:reais|real|rs)").find(texto)
        ?.groupValues
        ?.getOrNull(1)
        ?.parseNumeroPtBr()
        ?.let { return it }

    Regex("(?:valor|total)\\s*(?:de|com)?\\s*(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)").find(texto)
        ?.groupValues
        ?.getOrNull(1)
        ?.parseNumeroPtBr()
        ?.let { return it }

    val match = Regex("(?:abastecimento|abasteci|coloquei)\\s*(?:de|com)?\\s*(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)").find(texto)
        ?: return null
    val depoisDoNumero = texto.substring(match.range.last + 1).trimStart()
    if (Regex("^(?:litros?|l\\b)").containsMatchIn(depoisDoNumero)) return null
    return match.groupValues.getOrNull(1)?.parseNumeroPtBr()
}

fun detectarLitros(pergunta: String): Double? {
    val texto = pergunta.lowercase(Locale.getDefault())
    return Regex("(\\d+(?:[,.]\\d{1,2})?)\\s*(?:litros|litro|l\\b)")
        .find(texto)
        ?.groupValues
        ?.getOrNull(1)
        ?.parseNumeroPtBr()
}

fun detectarPrecoLitro(pergunta: String): Double? {
    val texto = pergunta.lowercase(Locale.getDefault())
    val patterns = listOf(
        Regex("(?:a|por)\\s*(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)\\s*(?:o litro|/l|por litro)"),
        Regex("(?:preco|preÃ§o)\\s*(?:do litro)?\\s*(?:r\\$\\s*)?(\\d+(?:[,.]\\d{1,2})?)")
    )
    return patterns.firstNotNullOfOrNull { regex ->
        regex.find(texto)?.groupValues?.getOrNull(1)?.parseNumeroPtBr()
    }
}

fun detectarKmInteiro(pergunta: String): Int? {
    val texto = normalizarParaBusca(pergunta)
    return Regex("\\b(\\d{2,7})\\s*(?:km|quilometros?)\\b")
        .find(texto)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}

fun String.parseNumeroPtBr(): Double? {
    val normalized = if (contains(",")) {
        replace(".", "").replace(",", ".")
    } else {
        this
    }
    return normalized
        .toDoubleOrNull()
}

private fun contemTermoCompleto(textoNormalizado: String, termoNormalizado: String): Boolean {
    if (termoNormalizado.isBlank()) return false
    return Regex("(^|\\s)${Regex.escape(termoNormalizado)}(\\s|$)").containsMatchIn(textoNormalizado)
}

private fun detectarVeiculosMencionados(
    pergunta: String,
    carros: List<CarroInfo>
): List<CarroInfo> {
    val texto = normalizarParaBusca(pergunta)
    return carros
        .mapNotNull { carro ->
            val termos = listOf(
                carro.nome,
                carro.modelo,
                "${carro.marca} ${carro.modelo}".trim()
            )
                .map(::normalizarParaBusca)
                .filter { it.length >= 3 }
                .distinct()

            val melhorTermo = termos
                .filter { contemTermoCompleto(texto, it) }
                .maxByOrNull { it.length }

            melhorTermo?.let { carro to it.length }
        }
        .sortedByDescending { it.second }
        .map { it.first }
        .distinctBy { it.id }
}

fun detectarVeiculosNoTexto(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String
): List<CarroInfo> {
    val mencionados = detectarVeiculosMencionados(pergunta, carros)
    if (mencionados.isNotEmpty()) return mencionados
    return carros.firstOrNull { it.id == currentCarroId }?.let { listOf(it) }.orEmpty()
}

fun detectarPedidoRelatorio(
    pergunta: String,
    carros: List<CarroInfo>,
    currentCarroId: String
): AiReportRequest? {
    val texto = normalizarParaBusca(pergunta)
    val pediuRelatorio = listOf(
        "relatorio",
        "relatorios",
        "pdf",
        "exportar",
        "compartilhar"
    ).any { texto.contains(it) }
    if (!pediuRelatorio) return null

    val pediuFrota = listOf(
        "frota",
        "garagem",
        "todos os veiculos",
        "todos veiculos",
        "todos os carros",
        "todos carros",
        "geral"
    ).any { texto.contains(it) }
    if (pediuFrota) return AiReportRequest.Fleet

    val mencionados = detectarVeiculosMencionados(pergunta, carros)

    if (mencionados.size == 1) return AiReportRequest.Vehicle(mencionados.first())
    if (mencionados.size > 1) return AiReportRequest.Fleet

    val pediuVeiculoAtual = listOf(
        "meu veiculo",
        "veiculo atual",
        "meu carro",
        "carro atual",
        "desse veiculo",
        "deste veiculo",
        "dele"
    ).any { texto.contains(it) }
    if (pediuVeiculoAtual) {
        val atual = carros.firstOrNull { it.id == currentCarroId } ?: carros.firstOrNull()
        return atual?.let { AiReportRequest.Vehicle(it) } ?: AiReportRequest.MissingVehicle
    }

    return AiReportRequest.MissingVehicle
}

fun detectarTipoAvisoIa(pergunta: String): TipoManutencao {
    val texto = normalizarParaBusca(pergunta)
    return when {
        listOf("oleo", "filtro de oleo", "troca de oleo").any { texto.contains(it) } -> TipoManutencao.OLEO
        listOf("pneu", "pneus", "calibrar", "rodizio").any { texto.contains(it) } -> TipoManutencao.PNEU
        listOf("freio", "freios", "pastilha").any { texto.contains(it) } -> TipoManutencao.FREIO
        listOf("bateria", "eletrica", "alternador").any { texto.contains(it) } -> TipoManutencao.BATERIA
        listOf("revisao", "revisar", "checkup").any { texto.contains(it) } -> TipoManutencao.REVISAO
        listOf("lavar", "lavagem").any { texto.contains(it) } -> TipoManutencao.LAVAGEM
        listOf("licenciamento", "licenca").any { texto.contains(it) } -> TipoManutencao.LICENCIAMENTO
        texto.contains("ipva") -> TipoManutencao.IPVA
        texto.contains("seguro") -> TipoManutencao.SEGURO
        listOf("vidro", "vidros", "parabrisa").any { texto.contains(it) } -> TipoManutencao.VIDROS
        listOf("funilaria", "pintura", "lataria").any { texto.contains(it) } -> TipoManutencao.FUNILARIA
        listOf("mecanica", "motor", "suspensao", "correia").any { texto.contains(it) } -> TipoManutencao.MECANICA
        else -> TipoManutencao.OUTROS
    }
}

fun detectarTituloAvisoIa(pergunta: String, tipo: TipoManutencao): String {
    val texto = normalizarParaBusca(pergunta)
    return when {
        texto.contains("trocar") && tipo == TipoManutencao.OLEO -> "Trocar oleo"
        texto.contains("calibrar") && tipo == TipoManutencao.PNEU -> "Calibrar pneus"
        texto.contains("rodizio") && tipo == TipoManutencao.PNEU -> "Rodizio dos pneus"
        texto.contains("revisar") || texto.contains("revisao") -> "Revisar ${tipo.label.lowercase(Locale.getDefault())}"
        tipo != TipoManutencao.OUTROS -> tipo.label
        else -> "Aviso criado pela IA"
    }
}

fun detectarTituloServicoRealizadoIa(pergunta: String, tipo: TipoManutencao): String {
    val texto = normalizarParaBusca(pergunta)
    return when {
        listOf("troca de oleo", "troquei oleo", "trocar oleo").any { texto.contains(it) } -> "Troca de oleo realizada"
        texto.contains("ipva") -> "IPVA pago"
        texto.contains("seguro") -> "Seguro registrado"
        texto.contains("licenciamento") -> "Licenciamento pago"
        texto.contains("lavagem") || texto.contains("lavei") -> "Lavagem realizada"
        texto.contains("freio") || texto.contains("pastilha") -> "Servico de freio realizado"
        texto.contains("pneu") || texto.contains("rodizio") -> "Servico de pneus realizado"
        texto.contains("bateria") -> "Servico de bateria realizado"
        texto.contains("revisao") || texto.contains("revisar") -> "Revisao realizada"
        tipo != TipoManutencao.OUTROS -> "${tipo.label} realizado"
        else -> "Servico registrado"
    }
}

fun detectarDataLimiteIa(pergunta: String): String {
    val texto = pergunta.lowercase(Locale.getDefault())
    val hoje = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    Regex("daqui\\s+(\\d{1,3})\\s+dias?").find(texto)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let {
        return hoje.plusDays(it).format(formatter)
    }
    if (texto.contains("amanha") || texto.contains("amanh")) {
        return hoje.plusDays(1).format(formatter)
    }
    if (texto.contains("ontem")) {
        return hoje.minusDays(1).format(formatter)
    }
    if (texto.contains("hoje")) {
        return hoje.format(formatter)
    }
    Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b").find(texto)?.let { match ->
        val dia = match.groupValues[1].toIntOrNull() ?: return ""
        val mes = match.groupValues[2].toIntOrNull() ?: return ""
        val anoRaw = match.groupValues.getOrNull(3).orEmpty()
        val ano = when {
            anoRaw.length == 2 -> 2000 + (anoRaw.toIntOrNull() ?: hoje.year)
            anoRaw.length == 4 -> anoRaw.toIntOrNull() ?: hoje.year
            else -> hoje.year
        }
        return runCatching { LocalDate.of(ano, mes, dia).format(formatter) }.getOrDefault("")
    }
    return ""
}

fun detectarKmLimiteIa(pergunta: String): String {
    val texto = normalizarParaBusca(pergunta)
    val match = Regex("\\b(?:aos|em|com)?\\s*(\\d{2,6})\\s*(?:km|quilometros?)\\b").find(texto)
    return match?.groupValues?.getOrNull(1).orEmpty()
}

fun detectarHoraAvisoIa(pergunta: String): String? {
    val textoOriginal = pergunta.lowercase(Locale.getDefault())
    val texto = normalizarParaBusca(pergunta)
    val patterns = listOf(
        Regex("""(?:Ã s|as)\s+(\d{1,2})(?::|h)?(\d{2})?"""),
        Regex("""(?:hora|horario|horÃ¡rio)\s*(?:de|do|para|as|Ã s)?\s*(\d{1,2})(?::|h)?(\d{2})?"""),
        Regex("""\b([01]?\d|2[0-3])[:h]([0-5]\d)\b""")
    )
    patterns.forEach { pattern ->
        pattern.find(textoOriginal)?.let { match ->
            val hour = match.groupValues.getOrNull(1)?.toIntOrNull()
            val minute = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            if (hour != null && hour in 0..23 && minute in 0..59) {
                return "%02d:%02d".format(hour, minute)
            }
        }
    }
    return when {
        texto.contains("manha") -> "09:00"
        texto.contains("tarde") -> "14:00"
        texto.contains("noite") -> "19:00"
        else -> null
    }
}

fun isFuelRecordText(textoNormalizado: String): Boolean {
    return listOf(
        "abasteci",
        "abastecimento",
        "registrar abastecimento",
        "registra abastecimento",
        "coloquei",
        "colocar",
        "posto",
        "gasolina",
        "etanol",
        "diesel"
    ).any { textoNormalizado.contains(it) }
}

fun normalizarParaBusca(text: String): String {
    return java.text.Normalizer.normalize(text.lowercase(Locale.getDefault()), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9/\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun normalizarMensagemChat(text: String): String {
    return text
        .replace(Regex("[!?.,;:]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun isGreetingOnly(text: String): Boolean {
    if (text.isBlank()) return false
    val greetings = setOf(
        "oi",
        "ola",
        "olÃ¡",
        "opa",
        "e ai",
        "eae",
        "bom dia",
        "boa tarde",
        "boa noite",
        "tudo bem",
        "tudo bom"
    )
    return text in greetings || greetings.any { greeting ->
        text == "$greeting tudo bem" || text == "$greeting tudo bom"
    }
}

fun isThanksOnly(text: String): Boolean {
    val thanks = setOf("obrigado", "obrigada", "valeu", "vlw", "show", "beleza", "blz", "ok", "ta bom")
    return text in thanks
}

fun isBeginnerMechanicQuestion(text: String): Boolean {
    val normalized = normalizarParaBusca(text)
    val hasQuestionIntent = listOf(
        "o que e",
        "oque e",
        "pra que serve",
        "para que serve",
        "como sei",
        "como saber",
        "como verificar",
        "verificar",
        "checar",
        "conferir",
        "sinais",
        "sintoma",
        "sintomas",
        "e grave",
        "ta ruim",
        "esta ruim",
        "explica",
        "me explica",
        "nao entendo",
        "nao conheco"
    ).any { normalized.contains(it) }
    val hasMechanicTopic = listOf(
        "oleo",
        "pneu",
        "pneus",
        "freio",
        "freios",
        "bateria",
        "motor",
        "mecanica",
        "arrefecimento",
        "temperatura",
        "radiador",
        "revisao",
        "correia",
        "pastilha",
        "painel",
        "luz do painel",
        "luzes"
    ).any { normalized.contains(it) }
    return hasQuestionIntent && hasMechanicTopic
}

fun detectarAssuntoMecanicaBasica(text: String): TipoManutencao {
    val normalized = normalizarParaBusca(text)
    return when {
        listOf("oleo", "filtro de oleo").any { normalized.contains(it) } -> TipoManutencao.OLEO
        listOf("pneu", "pneus", "calibragem").any { normalized.contains(it) } -> TipoManutencao.PNEU
        listOf("freio", "freios", "pastilha", "disco").any { normalized.contains(it) } -> TipoManutencao.FREIO
        listOf("bateria", "alternador").any { normalized.contains(it) } -> TipoManutencao.BATERIA
        listOf("revisao", "checkup", "check up").any { normalized.contains(it) } -> TipoManutencao.REVISAO
        listOf("motor", "mecanica", "arrefecimento", "temperatura", "radiador", "correia", "painel", "luz do painel").any { normalized.contains(it) } -> TipoManutencao.MECANICA
        else -> TipoManutencao.OUTROS
    }
}

fun isGenericRegistrationRequest(text: String): Boolean {
    val registrationWords = listOf(
        "registro",
        "registrar",
        "registra",
        "cadastrar",
        "cadastro",
        "lancar",
        "lanÃ§ar",
        "adicionar"
    )
    val hasRegistrationWord = registrationWords.any { text.contains(it) }
    val hasSpecificTarget = listOf(
        "abastec",
        "combust",
        "gasolina",
        "etanol",
        "diesel",
        "aviso",
        "lembrete",
        "oleo",
        "pneu",
        "freio",
        "bateria",
        "revisao",
        "manutencao"
    ).any { text.contains(it) }
    return hasRegistrationWord && !hasSpecificTarget
}

fun isFleetStatusQuestion(text: String): Boolean {
    val normalized = normalizarParaBusca(text)
    val hasFleetTarget = listOf("frota", "garagem", "veiculos", "carros").any { normalized.contains(it) }
    val hasStatusIntent = listOf(
        "como esta",
        "como ta",
        "status",
        "situacao",
        "resumo",
        "visao geral",
        "estado",
        "saude"
    ).any { normalized.contains(it) }
    return hasFleetTarget && hasStatusIntent
}

fun isVehicleQuestion(text: String): Boolean {
    if (Regex("\\d{2,5}\\s*km").containsMatchIn(text)) return true
    if (isConsumptionQuestion(text)) return true
    if (isFleetStatusQuestion(text)) return true
    val normalized = normalizarParaBusca(text)
    if (listOf("frota", "garagem", "veiculos").any { normalized.contains(it) }) return true
    val keywords = listOf(
        "carro",
        "veiculo",
        "veÃ­culo",
        "coupe",
        "viagem",
        "viajar",
        "estrada",
        "rodar",
        "aviso",
        "avisos",
        "registro",
        "registrar",
        "relatorio",
        "relatÃƒÂ³rio",
        "relatorios",
        "relatÃƒÂ³rios",
        "pdf",
        "exportar",
        "compartilhar",
        "cadastro",
        "cadastrar",
        "lancar",
        "lanÃ§ar",
        "manutencao",
        "manutenÃ§Ã£o",
        "revisao",
        "revisÃ£o",
        "oleo",
        "Ã³leo",
        "freio",
        "freios",
        "pneu",
        "pneus",
        "bateria",
        "mecanica",
        "mecÃ¢nica",
        "motor",
        "correia",
        "radiador",
        "temperatura",
        "arrefecimento",
        "licenciamento",
        "ipva",
        "seguro",
        "urgente",
        "vencido",
        "vencidos",
        "posso",
        "bom para",
        "esta bom",
        "estÃ¡ bom",
        "revisar"
    )
    return keywords.any { text.contains(it) }
}

fun isConsumptionQuestion(text: String): Boolean {
    val keywords = listOf(
        "consumo",
        "combustivel",
        "combust",
        "gasolina",
        "etanol",
        "diesel",
        "km/l",
        "kml",
        "litros",
        "abastecimento",
        "abastecimentos",
        "gasto",
        "gastando",
        "economia",
        "media",
        "custo por km",
        "preco por km",
        "preco do km"
    )
    return keywords.any { text.contains(it) }
}

fun calcularRiscoVeiculo(lembretesAtivos: List<Lembrete>): VehicleRiskLevel {
    val hoje = LocalDate.now()
    val vencidos = lembretesAtivos.count { dataParaOrdenacao(it).isBefore(hoje) }
    val temCritico = lembretesAtivos.any { it.tipo in tiposCriticosParaViagem }
    return when {
        temCritico && vencidos > 0 -> VehicleRiskLevel.HIGH
        vencidos >= 2 -> VehicleRiskLevel.HIGH
        temCritico -> VehicleRiskLevel.MEDIUM
        vencidos == 1 -> VehicleRiskLevel.MEDIUM
        lembretesAtivos.isNotEmpty() -> VehicleRiskLevel.LOW
        else -> VehicleRiskLevel.LOW
    }
}

val tiposCriticosParaViagem = setOf(
    TipoManutencao.FREIO,
    TipoManutencao.PNEU,
    TipoManutencao.OLEO,
    TipoManutencao.BATERIA,
    TipoManutencao.MECANICA,
    TipoManutencao.REVISAO
)
