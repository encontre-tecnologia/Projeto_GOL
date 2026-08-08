package br.com.gui.carlembrete

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.view.WindowInsetsControllerCompat
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


@Composable
fun VehicleAiChatScreen(
    carros: List<CarroInfo>,
    currentCarroId: String,
    lembretesAtivos: List<Lembrete>,
    abastecimentos: List<Abastecimento>,
    onCreateReminders: (List<Lembrete>) -> Unit,
    onCreateFuelRecords: (List<Abastecimento>) -> Unit,
    onShareVehicleReport: (CarroInfo) -> Unit,
    onShareFleetReports: () -> Unit,
    onDismiss: () -> Unit,
    planTier: PlanTier = PlanTier.FREE
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (isDark) Color.Black else Color(0xFFF7FAFF)
    val barBg = bg
    val cardBg = if (isDark) Color(0xFF0B1220) else Color.White
    val border = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val userBubble = Color(0xFF2563EB)
    val aiBubble = if (isDark) Color(0xFF111827) else Color(0xFFEAF2FF)
    val aiText = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E3A8A)
    val inputShape = RoundedCornerShape(26.dp)
    val inputBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val inputFocusedBorderColor = Color(0xFF2563EB)
    val micButtonBg = Color(0xFF2563EB)
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }

    DisposableEffect(context, barBg, isDark) {
        val window = (context as? Activity)?.window
        val oldStatusColor = window?.statusBarColor
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldLightStatusBars = controller?.isAppearanceLightStatusBars
        if (window != null && controller != null) {
            window.statusBarColor = barBg.toArgb()
            controller.isAppearanceLightStatusBars = !isDark
        }
        onDispose {
            if (window != null && oldStatusColor != null) {
                window.statusBarColor = oldStatusColor
            }
            if (controller != null && oldLightStatusBars != null) {
                controller.isAppearanceLightStatusBars = oldLightStatusBars
            }
        }
    }

    BackHandler {
        if (showHelp) {
            showHelp = false
        } else {
            onDismiss()
        }
    }

    if (showHelp) {
        VehicleAiHelpScreen(
            onBack = { showHelp = false },
            isDark = isDark,
            bg = bg,
            barBg = barBg,
            cardBg = cardBg,
            border = border,
            titleColor = titleColor,
            subColor = subColor
        )
        return
    }

    val messages = remember {
        mutableStateListOf<VehicleChatMessage>()
    }
    var pendingReminderDrafts by remember { mutableStateOf<List<AiReminderDraft>>(emptyList()) }
    var duplicateReminderDrafts by remember { mutableStateOf<List<AiReminderDraft>>(emptyList()) }
    var pendingReminderDraftsAreVehicleSuggestions by remember { mutableStateOf(false) }
    var pendingFuelDrafts by remember { mutableStateOf<List<AiFuelDraft>>(emptyList()) }
    var pendingServiceRecordDrafts by remember { mutableStateOf<List<AiServiceRecordDraft>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val welcomeCompact = isInputFocused && isKeyboardVisible
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
                .trim()
            if (spokenText.isNotBlank()) {
                input = spokenText
            }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Limite mensal de IA por plano, vindo da dashboard em tempo real.
    var planPrices by remember { mutableStateOf(RemotePlanPricing.defaultPrices) }
    DisposableEffect(Unit) {
        val pricingReg = RemotePlanPricing.listen { planPrices = it }
        onDispose { pricingReg.remove() }
    }
    val currentVehicleName = carros.firstOrNull { it.id == currentCarroId }?.nome ?: "meu veiculo"
    val quickQuestionSets = listOf(
        listOf(
            "Plano de manutencao do $currentVehicleName",
            "Qual veiculo esta melhor para viajar 100 km?",
            "Qual veiculo devo revisar primeiro?",
            "Como sei se o pneu esta ruim?",
            "Compartilhar relatorios da frota",
            "O que e arrefecimento?"
        ),
        listOf(
            "Quanto o $currentVehicleName consumiu este mes?",
            "Qual veiculo esta gastando mais?",
            "Qual foi meu custo por km?",
            "Como melhorar o consumo?",
            "Registrar abastecimento de 30 reais",
            "Ver media de km/l"
        ),
        listOf(
            "Criar aviso de oleo em 85000 km",
            "Criar aviso de pneus daqui 30 dias",
            "Tenho algum aviso vencido?",
            "O que revisar antes de viajar?",
            "Quando trocar a correia?",
            "Freio fazendo barulho e grave?"
        ),
        listOf(
            "Gerar relatorio do $currentVehicleName",
            "Compartilhar relatorios da frota",
            "Resumo da minha garagem",
            "Qual veiculo tem mais risco?",
            "Como verificar o oleo?",
            "Bateria da sinal antes de acabar?"
        )
    )
    var quickSetIndex by remember { mutableStateOf(0) }
    val activeQuickQuestions = quickQuestionSets[quickSetIndex % quickQuestionSets.size]

    LaunchedEffect(messages.isEmpty(), welcomeCompact) {
        while (messages.isEmpty() && !welcomeCompact) {
            delay(6500)
            quickSetIndex = (quickSetIndex + 1) % quickQuestionSets.size
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua pergunta para o Zellu")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Reconhecimento de voz indisponivel neste aparelho.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendQuestion(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.isBlank()) return
        messages.add(VehicleChatMessage(question, fromUser = true))
        input = ""

        // Plano de manutenção: regras montam os avisos (intervalos confiáveis),
        // a IA só escreve a introdução. O usuário confirma os cards.
        if (MaintenancePlanner.isMaintenancePlanRequest(question)) {
            val carroPlano = carros.firstOrNull { it.id == currentCarroId } ?: carros.firstOrNull()
            if (carroPlano == null) {
                messages.add(
                    VehicleChatMessage(
                        "**Plano de manutenção**\n---\nPreciso de um veículo cadastrado para montar o plano. Cadastre um veículo e tente de novo.",
                        fromUser = false
                    )
                )
                return
            }
            val plano = MaintenancePlanner.buildPlan(carroPlano)
            if (plano.isEmpty()) {
                messages.add(
                    VehicleChatMessage(
                        "**Plano de manutenção**\n---\nNão consegui montar um plano para este veículo agora.",
                        fromUser = false
                    )
                )
                return
            }
            pendingReminderDrafts = plano
            pendingReminderDraftsAreVehicleSuggestions = true
            duplicateReminderDrafts = plano.filter { d -> lembretesAtivos.any { it.isSimilarTo(d) } }

            // Os avisos do plano vem das regras do MaintenancePlanner. A introducao e
            // apenas texto de apresentacao, entao fica local e nao consome cota de IA.
            messages.add(
                VehicleChatMessage(
                    MaintenancePlanner.fallbackIntro(carroPlano, plano.size),
                    fromUser = false
                )
            )
            return
        }

        when (val reportRequest = detectarPedidoRelatorio(question, carros, currentCarroId)) {
            AiReportRequest.Fleet -> {
                messages.add(
                    VehicleChatMessage(
                        "**Relatorios da frota**\n---\nVou abrir o compartilhamento dos relatorios que o Zellu ja gera na **Visao geral da frota**.\n\n- Eu nao vou inventar dados aqui.\n- Vou usar os PDFs oficiais dos veiculos cadastrados.",
                        fromUser = false
                    )
                )
                onShareFleetReports()
                return
            }
            is AiReportRequest.Vehicle -> {
                messages.add(
                    VehicleChatMessage(
                        "**Relatorio do ${reportRequest.carro.nome}**\n---\nVou abrir o compartilhamento do relatorio oficial desse veiculo, igual ao botao **Compartilhar** da tela do carro.\n\n- O PDF usa os dados cadastrados no app.\n- Confira antes de enviar para alguem.",
                        fromUser = false
                    )
                )
                onShareVehicleReport(reportRequest.carro)
                return
            }
            AiReportRequest.MissingVehicle -> {
                messages.add(
                    VehicleChatMessage(
                        "**Qual veiculo?**\n---\nEu consigo gerar o relatorio, mas preciso saber de qual veiculo.\n\n**Como pedir**\n- Gerar relatorio do Gol\n- Compartilhar relatorio do Crossfox\n- Me de os relatorios da frota",
                        fromUser = false
                    )
                )
                return
            }
            null -> Unit
        }
        val serviceRecordDraftResult = criarRascunhosDeServicoRealizado(question, carros, currentCarroId)
        if (serviceRecordDraftResult != null) {
            val drafts = serviceRecordDraftResult
            if (drafts.isEmpty()) {
                messages.add(
                    VehicleChatMessage(
                        "**Faltou um detalhe do registro**\n---\nPara registrar um servico ja realizado, mande **o que foi feito**, **veiculo**, **data de execucao** e **valor**, se tiver.\n\n**Exemplos**\n- Registrei troca de oleo no Gol hoje por 250 reais\n- Ja fiz o IPVA do Crossfox dia 10/02 valor 1200 reais\n- Paguei o seguro do Gol GTi ontem por 1800 reais",
                        fromUser = false
                    )
                )
            } else {
                pendingServiceRecordDrafts = drafts
                messages.add(
                    VehicleChatMessage(
                        "**Conferi o registro**\n---\nVou salvar isso como **servico realizado** no historico do veiculo, sem criar aviso futuro.",
                        fromUser = false
                    )
                )
            }
            return
        }
        val fuelDraftResult = criarRascunhosDeAbastecimento(question, carros, currentCarroId, abastecimentos)
        if (fuelDraftResult != null) {
            val drafts = fuelDraftResult
            if (drafts.isEmpty()) {
                messages.add(
                    VehicleChatMessage(
                        "**Faltou um detalhe do registro**\n---\nPara registrar abastecimento por aqui, mande **titulo do registro**, **veiculo**, **valor ou litros** e **km atual**.\n\n**Exemplos**\n- Abastecimento de 30 reais no Gol com 85400 km\n- Registrar abastecimento de 120 reais no Crossfox com 85400 km\n- Coloquei 20 litros no Gol a 5,60 o litro com 85400 km",
                        fromUser = false
                    )
                )
            } else {
                pendingFuelDrafts = drafts
                messages.add(
                    VehicleChatMessage(
                        "**Conferi o abastecimento**\n---\nPosso salvar esse registro com o km informado. Assim o consumo e o custo por km ficam bem mais confiaveis.",
                        fromUser = false
                    )
                )
            }
            return
        }
        val reminderDraftResult = criarRascunhosDeAviso(question, carros, currentCarroId)
        if (reminderDraftResult != null) {
            val drafts = reminderDraftResult
            if (drafts.isEmpty()) {
                messages.add(
                    VehicleChatMessage(
                        "**Quase la**\n---\nPara criar aviso por aqui eu preciso entender:\n- titulo do aviso\n- veiculo\n- km limite\n- data, se tiver\n\nA hora fica como campo separado no card de confirmacao. Se nao tiver hora, eu uso **09:00** como padrao.\n\n**Como mandar**\n- Criar aviso para trocar oleo do Gol em 85000 km dia 10/08\n- Me lembra de revisar pneus do Crossfox em 90000 km daqui 30 dias\n- Criar aviso de freio para o Coupe em 85000 km amanha\n\nPode citar mais de um veiculo na mesma frase que eu preparo tudo para confirmar.",
                        fromUser = false
                    )
                )
            } else {
                pendingReminderDrafts = drafts
                pendingReminderDraftsAreVehicleSuggestions = false
                duplicateReminderDrafts = drafts.filter { draft ->
                    lembretesAtivos.any { it.isSimilarTo(draft) }
                }
                val duplicateText = if (duplicateReminderDrafts.isNotEmpty()) {
                    "\n\n**Atencao**\n---\nJa existe aviso parecido para ${duplicateReminderDrafts.joinToString { it.carro.nome }}. Confirma se quer criar outro mesmo assim."
                } else {
                    ""
                }
                messages.add(
                    VehicleChatMessage(
                        "**Conferi o pedido**\n---\nEncontrei ${drafts.size} aviso${if (drafts.size == 1) "" else "s"} para criar. Confirma os dados abaixo antes de salvar.$duplicateText",
                        fromUser = false
                    )
                )
            }
            return
        }
        // Roteia antes de qualquer rede: a regra local resolve a maior parte das
        // perguntas de graca, e so o que sobra e candidato a consumir cota.
        val roteamento = resolverRespostaGaragem(question, carros, currentCarroId, lembretesAtivos, abastecimentos)
        val consomeCota = roteamento is RespostaGaragem.Escalar && zelluAiOnlineDisponivel()

        // Limite mensal de requisições de IA por plano (0 = ilimitado). Nunca bloqueia
        // resposta local: sem LLM no caminho nao existe custo para limitar.
        val aiLimit = planPrices.aiLimitForTier(planTier)
        if (consomeCota && !AiUsageLimiter.isWithinLimit(context, aiLimit)) {
            val fallbackLocal = (roteamento as RespostaGaragem.Escalar).fallbackLocal
            messages.add(
                VehicleChatMessage(
                    "$fallbackLocal\n\n---\n**Limite de consultas online atingido**\nVoce ja usou suas $aiLimit consultas online neste mes no plano ${planNameLabel(planTier)}. Respondi com a analise local. O contador zera no proximo mes.",
                    fromUser = false
                )
            )
            return
        }

        val answerIndex = messages.size
        messages.add(VehicleChatMessage("", fromUser = false, isTyping = true))
        scope.launch {
            val startedAt = System.currentTimeMillis()
            val resultado = when (roteamento) {
                is RespostaGaragem.Local -> RespostaZelluAi(roteamento.texto, usouLlm = false)
                is RespostaGaragem.Escalar -> responderOnlineZelluAi(
                    pergunta = question,
                    carros = carros,
                    currentCarroId = currentCarroId,
                    lembretesAtivos = lembretesAtivos,
                    abastecimentos = abastecimentos,
                    fallbackLocal = roteamento.fallbackLocal
                )
            }
            // Cota debitada apenas quando o LLM de fato respondeu.
            if (resultado.usouLlm) {
                AiUsageLimiter.register(context)
            }
            val elapsed = System.currentTimeMillis() - startedAt
            val minimumTypingMillis = 850L
            if (elapsed < minimumTypingMillis) {
                delay(minimumTypingMillis - elapsed)
            }
            if (answerIndex in messages.indices) {
                messages[answerIndex] = VehicleChatMessage(resultado.texto, fromUser = false)
            }
        }
    }

    val scrollSignature = messages.lastOrNull()?.let { last ->
        "${messages.size}:${last.text.length}:${last.isTyping}:${pendingFuelDrafts.size}:${pendingReminderDrafts.size}:${pendingServiceRecordDrafts.size}"
    }.orEmpty()
    LaunchedEffect(scrollSignature) {
        if (messages.isNotEmpty()) {
            delay(90)
            val extraCards = (if (pendingFuelDrafts.isNotEmpty()) 1 else 0) +
                (if (pendingReminderDrafts.isNotEmpty()) 1 else 0) +
                (if (pendingServiceRecordDrafts.isNotEmpty()) 1 else 0)
            val targetIndex = messages.size + extraCards + 1
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barBg)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = tr("Voltar", "Back"),
                    tint = titleColor,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = garageAnalysisName(),
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp
                )
            }
            IconButton(onClick = { showHelp = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = tr("Ajuda da análise da garagem", "Garage analysis help"),
                    tint = titleColor,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    VehicleAiWelcome(
                        titleColor = titleColor,
                        subColor = subColor,
                        cardBg = cardBg,
                        border = border,
                        compact = welcomeCompact,
                        questions = activeQuickQuestions,
                        onQuestionClick = ::sendQuestion
                    )
                }
            } else {
                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        VehicleChatBubble(
                            message = message,
                            userBubble = userBubble,
                            aiBubble = aiBubble,
                            border = border,
                            aiText = aiText
                        )
                    }
                }
                if (pendingFuelDrafts.isNotEmpty()) {
                    item {
                        FuelDraftConfirmationCard(
                            drafts = pendingFuelDrafts,
                            titleColor = titleColor,
                            subColor = subColor,
                            cardBg = cardBg,
                            border = border,
                            onCancel = {
                                pendingFuelDrafts = emptyList()
                                messages.add(VehicleChatMessage("Fechou, nao salvei o abastecimento.", fromUser = false))
                            },
                            onConfirm = {
                                val novos = pendingFuelDrafts.map { it.toAbastecimento() }
                                onCreateFuelRecords(novos)
                                pendingFuelDrafts = emptyList()
                                messages.add(
                                    VehicleChatMessage(
                                        "**Abastecimento salvo**\n---\nRegistrei ${novos.size} abastecimento${if (novos.size == 1) "" else "s"} no historico.",
                                        fromUser = false
                                    )
                                )
                            }
                        )
                    }
                }
                if (pendingServiceRecordDrafts.isNotEmpty()) {
                    item {
                        ServiceRecordDraftConfirmationCard(
                            drafts = pendingServiceRecordDrafts,
                            titleColor = titleColor,
                            subColor = subColor,
                            cardBg = cardBg,
                            border = border,
                            onCancel = {
                                pendingServiceRecordDrafts = emptyList()
                                messages.add(VehicleChatMessage("Fechou, nao salvei esse registro no historico.", fromUser = false))
                            },
                            onConfirm = {
                                val novos = pendingServiceRecordDrafts.map { it.toLembreteRealizado() }
                                onCreateReminders(novos)
                                pendingServiceRecordDrafts = emptyList()
                                messages.add(
                                    VehicleChatMessage(
                                        "**Registro salvo**\n---\nSalvei ${novos.size} registro${if (novos.size == 1) "" else "s"} realizado${if (novos.size == 1) "" else "s"} no historico.",
                                        fromUser = false
                                    )
                                )
                            }
                        )
                    }
                }
                if (pendingReminderDrafts.isNotEmpty()) {
                    item {
                        ReminderDraftConfirmationCard(
                            drafts = pendingReminderDrafts,
                            duplicateDrafts = duplicateReminderDrafts,
                            titleColor = titleColor,
                            subColor = subColor,
                            cardBg = cardBg,
                            border = border,
                            areVehicleSuggestions = pendingReminderDraftsAreVehicleSuggestions,
                            onCancel = {
                                pendingReminderDrafts = emptyList()
                                duplicateReminderDrafts = emptyList()
                                pendingReminderDraftsAreVehicleSuggestions = false
                                messages.add(VehicleChatMessage("Beleza, nao criei nenhum aviso.", fromUser = false))
                            },
                            onConfirm = {
                                val novos = pendingReminderDrafts.map { it.toLembrete() }
                                onCreateReminders(novos)
                                pendingReminderDrafts = emptyList()
                                duplicateReminderDrafts = emptyList()
                                pendingReminderDraftsAreVehicleSuggestions = false
                                messages.add(
                                    VehicleChatMessage(
                                        "**Aviso criado**\n---\nSalvei ${novos.size} aviso${if (novos.size == 1) "" else "s"} na garagem.",
                                        fromUser = false
                                    )
                                )
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
                item { Spacer(Modifier.height(1.dp)) }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 54.dp)
                    .onFocusChanged { isInputFocused = it.isFocused },
                placeholder = { Text(tr("Pergunte sobre sua garagem...", "Ask about your garage...")) },
                trailingIcon = {
                    if (input.isBlank()) {
                        IconButton(
                            onClick = ::startVoiceInput,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(micButtonBg)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = tr("Falar", "Speak"),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { sendQuestion(input) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(micButtonBg)
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = tr("Enviar", "Send"),
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendQuestion(input) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = inputFocusedBorderColor,
                    unfocusedBorderColor = inputBorderColor,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                shape = inputShape
            )
            Text(
                text = "A análise pode errar. Confira antes de agir ou salvar.",
                color = subColor,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        }
    }
}

