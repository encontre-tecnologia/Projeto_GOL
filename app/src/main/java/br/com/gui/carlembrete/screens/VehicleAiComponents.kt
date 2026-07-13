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
fun VehicleAiHelpScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    bg: Color,
    barBg: Color,
    cardBg: Color,
    border: Color,
    titleColor: Color,
    subColor: Color
) {
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
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = tr("Voltar", "Back"),
                        tint = titleColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    text = "Ajuda da Zellu IA",
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(36.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    VehicleAiHelpHero(
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.AutoAwesome,
                        title = "O que ela consegue fazer",
                        lines = listOf(
                            "Analisar os veiculos cadastrados e dizer qual parece melhor para viajar.",
                            "Apontar qual veiculo merece revisao primeiro.",
                            "Responder consumo, gasto no mes, km/l e custo por km usando seus abastecimentos.",
                            "Criar avisos com titulo, km, data e hora antes de salvar.",
                            "Registrar servicos ja feitos, como oleo, IPVA, seguro e revisoes.",
                            "Registrar abastecimentos com valor, litros e km.",
                            "Abrir o compartilhamento de relatorios do veiculo ou da frota.",
                            "Explicar mecanica basica em linguagem simples."
                        ),
                        examples = emptyList(),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.Event,
                        title = "Criar avisos",
                        lines = listOf(
                            "Sempre informe titulo, veiculo e km limite.",
                            "A data deixa o aviso mais completo. A hora aparece separada no card; sem hora, uso 09:00.",
                            "Se ja existir aviso parecido, ela pergunta antes de criar outro."
                        ),
                        examples = listOf(
                            "Criar aviso para trocar oleo do Gol em 85000 km dia 28/11/2026",
                            "Me lembra de revisar pneus do Crossfox em 90000 km daqui 30 dias",
                            "Criar aviso de freio para Gol GTi em 78000 km amanha"
                        ),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.Speed,
                        title = "Consumo e abastecimento",
                        lines = listOf(
                            "Para registrar abastecimento, mande titulo do registro, valor ou litros, veiculo e km atual.",
                            "Para perguntar consumo, ela usa os registros ja cadastrados.",
                            "Quanto mais abastecimentos com km correto, melhor fica a media."
                        ),
                        examples = listOf(
                            "Abastecimento de 30 reais no Gol com 85400 km",
                            "Coloquei 20 litros no Crossfox a 5,60 com 90200 km",
                            "Quanto o Gol consumiu esse mes?",
                            "Qual veiculo esta gastando mais?"
                        ),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.CheckCircle,
                        title = "Registro de servico feito",
                        lines = listOf(
                            "Use quando o servico ja aconteceu e voce so quer guardar no historico.",
                            "Informe o que foi feito, veiculo, data de execucao e valor, se tiver.",
                            "Esse registro nao cria aviso futuro."
                        ),
                        examples = listOf(
                            "Registrei troca de oleo no Gol hoje por 250 reais",
                            "Ja fiz o IPVA do Crossfox dia 10/02 valor 1200 reais",
                            "Paguei o seguro do Gol GTi ontem por 1800 reais"
                        ),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.DirectionsCar,
                        title = "Viagem e prioridade",
                        lines = listOf(
                            "Ela compara avisos ativos, vencidos, risco e consumo quando houver dados.",
                            "A resposta e uma orientacao, nao substitui uma revisao mecanica.",
                            "Perguntas com distancia ajudam a estimar custo quando existe custo por km."
                        ),
                        examples = listOf(
                            "Qual veiculo esta melhor para viajar 100 km?",
                            "Qual devo revisar primeiro?",
                            "Meu Gol esta bom para pegar estrada?"
                        ),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.Build,
                        title = "Mecanica sem complicar",
                        lines = listOf(
                            "Ela explica sinais basicos de oleo, pneus, freios, bateria, arrefecimento, motor e correia.",
                            "Tambem pode falar intervalos medios de troca e o que observar no dia a dia.",
                            "Quando envolver risco real, a recomendacao certa e procurar um mecanico."
                        ),
                        examples = listOf(
                            "Como verificar o oleo?",
                            "Quando trocar a correia dentada?",
                            "Como sei se o pneu esta ruim?",
                            "O que e arrefecimento?"
                        ),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
                item {
                    VehicleAiHelpSection(
                        icon = Icons.Default.WarningAmber,
                        title = "Relatorios",
                        lines = listOf(
                            "Ela nao inventa relatorio no chat.",
                            "Quando voce pede, ela abre o compartilhamento do PDF oficial que o app ja gera.",
                            "Da para pedir relatorio de um veiculo ou todos os relatorios da frota."
                        ),
                        examples = listOf(
                            "Gerar relatorio do Gol",
                            "Compartilhar relatorio do Crossfox",
                            "Me de os relatorios da frota"
                        ),
                        cardBg = cardBg,
                        border = border,
                        titleColor = titleColor,
                        subColor = subColor
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleAiHelpHero(
    cardBg: Color,
    border: Color,
    titleColor: Color,
    subColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Pergunte como se estivesse falando com alguem da garagem.",
            color = titleColor,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            lineHeight = 23.sp
        )
        Text(
            text = "A Zellu IA entende pedidos sobre avisos, consumo, viagens, relatorios e duvidas basicas de mecanica. Ela usa seus dados cadastrados e sempre pede confirmacao antes de salvar algo.",
            color = subColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun VehicleAiHelpSection(
    icon: ImageVector,
    title: String,
    lines: List<String>,
    examples: List<String>,
    cardBg: Color,
    border: Color,
    titleColor: Color,
    subColor: Color
) {
    val isDarkCard = cardBg.luminance() < 0.5f
    val accent = if (isDarkCard) Color(0xFF93C5FD) else Color(0xFF2563EB)
    val exampleColor = if (isDarkCard) Color(0xFFC7D2FE) else Color(0xFF2563EB)
    val accentBg = if (isDarkCard) Color.White.copy(alpha = 0.07f) else accent.copy(alpha = 0.14f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
        lines.forEach { line ->
            Text(
                text = "- $line",
                color = subColor,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (examples.isNotEmpty()) {
            Text(
                text = "Exemplos",
                color = titleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            examples.forEach { example ->
                Text(
                    text = "\"$example\"",
                    color = exampleColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun VehicleChatBubble(
    message: VehicleChatMessage,
    userBubble: Color,
    aiBubble: Color,
    border: Color,
    aiText: Color
) {
    val bubbleColor = if (message.fromUser) userBubble else aiBubble
    val bubbleBorder = if (message.fromUser) userBubble else border
    val textColor = if (message.fromUser) Color.White else aiText
    val timeColor = if (message.fromUser) Color(0xFF93C5FD) else aiText.copy(alpha = 0.58f)
    val timeText = message.sentAt.format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(
        modifier = Modifier.widthIn(max = 318.dp),
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .border(BorderStroke(1.dp, bubbleBorder), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (message.isTyping) {
                TypingDots(color = aiText)
            } else if (message.fromUser) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            } else {
                AiFormattedMessage(
                    text = message.text,
                    color = aiText
                )
            }
        }
        if (!message.isTyping) {
            Text(
                text = timeText,
                color = timeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = if (message.fromUser) TextAlign.End else TextAlign.Start,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun VehicleAiWelcome(
    titleColor: Color,
    subColor: Color,
    cardBg: Color,
    border: Color,
    compact: Boolean,
    questions: List<String>,
    onQuestionClick: (String) -> Unit
) {
    val chipBg = if (cardBg.luminance() < 0.5f) Color.White.copy(alpha = 0.06f) else Color.White
    val chipText = if (cardBg.luminance() < 0.5f) Color(0xFFE2E8F0) else Color(0xFF1D4ED8)
    val iconBg = if (cardBg.luminance() < 0.5f) Color.White.copy(alpha = 0.08f) else Color(0xFFEAF2FF)
    val iconTint = if (cardBg.luminance() < 0.5f) Color(0xFFE2E8F0) else Color(0xFF2563EB)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 78.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = iconTint)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Como o Zellu pode ajudar?",
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pergunte sobre avisos, revisoes, viagem ou consumo.",
                color = subColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn(animationSpec = tween(260, delayMillis = 80)) +
                expandVertically(animationSpec = tween(280), expandFrom = Alignment.Top) +
                slideInVertically(animationSpec = tween(280)) { -it / 5 },
            exit = fadeOut(animationSpec = tween(130)) +
                shrinkVertically(animationSpec = tween(190), shrinkTowards = Alignment.Top) +
                slideOutVertically(animationSpec = tween(190)) { -it / 6 }
        ) {
            AnimatedContent(
                targetState = questions,
                transitionSpec = {
                    ((fadeIn(animationSpec = tween(280)) +
                        slideInVertically(animationSpec = tween(320)) { it / 8 }) togetherWith
                        (fadeOut(animationSpec = tween(180)) +
                            slideOutVertically(animationSpec = tween(220)) { -it / 10 }))
                        .using(SizeTransform(clip = false))
                },
                label = "rotatingQuickQuestions"
            ) { animatedQuestions ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    animatedQuestions.chunked(2).forEachIndexed { rowIndex, rowQuestions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowQuestions.forEachIndexed { itemIndex, question ->
                                val itemDelay = (rowIndex * 2 + itemIndex) * 35
                                val itemVisible = remember(question) { mutableStateOf(false) }
                                LaunchedEffect(question) {
                                    itemVisible.value = false
                                    delay(itemDelay.toLong())
                                    itemVisible.value = true
                                }
                                val itemScale by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (itemVisible.value) 1f else 0.96f,
                                    animationSpec = tween(240),
                                    label = "quickChipScale"
                                )
                                OutlinedButton(
                                    onClick = { onQuestionClick(question) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .scale(itemScale),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, border.copy(alpha = 0.9f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = chipBg,
                                        contentColor = chipText
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = question,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (rowQuestions.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderDraftConfirmationCard(
    drafts: List<AiReminderDraft>,
    duplicateDrafts: List<AiReminderDraft>,
    titleColor: Color,
    subColor: Color,
    cardBg: Color,
    border: Color,
    areVehicleSuggestions: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val hasDuplicates = duplicateDrafts.isNotEmpty()
    val accent = if (hasDuplicates) Color(0xFFF59E0B) else Color(0xFF2563EB)
    val confirmLabel = when {
        hasDuplicates -> "Criar mesmo assim"
        drafts.size == 1 -> "Criar aviso"
        else -> "Criar avisos"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasDuplicates) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = when {
                        areVehicleSuggestions -> "Avisos sugeridos"
                        hasDuplicates -> "Aviso parecido encontrado"
                        else -> "Criar aviso"
                    },
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = when {
                        areVehicleSuggestions -> "Baseados no ano/modelo e KM do veiculo. Revise antes de criar."
                        hasDuplicates -> "Confira antes de salvar outro igual."
                        else -> "Revise os dados antes de confirmar."
                    },
                    color = subColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${drafts.size} ${if (drafts.size == 1) "aviso" else "avisos"}",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (hasDuplicates) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.11f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.28f)), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Ja existe um aviso bem parecido. Se for uma revisao nova ou outro servico, pode criar mesmo assim.",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp
                )
            }
        }

        drafts.forEachIndexed { index, draft ->
            ReminderDraftSummaryItem(
                index = index,
                draft = draft,
                isDuplicate = duplicateDrafts.any { it.matchesIdentity(draft) },
                titleColor = titleColor,
                subColor = subColor,
                border = border
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = subColor)
            ) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1.15f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = confirmLabel,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun ReminderDraftSummaryItem(
    index: Int,
    draft: AiReminderDraft,
    isDuplicate: Boolean,
    titleColor: Color,
    subColor: Color,
    border: Color
) {
    val itemAccent = if (isDuplicate) Color(0xFFF59E0B) else Color(0xFF2563EB)
    val limitText = listOfNotNull(
        draft.dataLimite.ifBlank { null },
        draft.kmLimite.takeIf { it.isNotBlank() }?.let { "$it km" }
    ).joinToString(" - ").ifBlank { "sem limite definido" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(itemAccent.copy(alpha = if (isDuplicate) 0.10f else 0.06f))
            .border(
                BorderStroke(1.dp, if (isDuplicate) itemAccent.copy(alpha = 0.40f) else border),
                RoundedCornerShape(16.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}. Aviso de manutencao",
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            if (isDuplicate) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(itemAccent.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "parecido",
                        color = itemAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        ReminderInfoRow(Icons.Default.CheckCircle, "Titulo", draft.titulo, subColor)
        ReminderInfoRow(Icons.Default.DirectionsCar, "Veiculo", draft.carro.nome, subColor)
        ReminderInfoRow(Icons.Default.Build, "Tipo", draft.tipo.label, subColor)
        ReminderInfoRow(
            icon = if (draft.kmLimite.isNotBlank() && draft.dataLimite.isBlank()) Icons.Default.Speed else Icons.Default.Event,
            label = "Limite",
            value = limitText,
            color = subColor
        )
        ReminderInfoRow(
            icon = Icons.Default.Event,
            label = "Hora",
            value = draft.horaAviso + if (draft.horaInformada) "" else " (padrao)",
            color = subColor
        )
    }
}

@Composable
fun ReminderInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.74f),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "$label: ",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FuelDraftConfirmationCard(
    drafts: List<AiFuelDraft>,
    titleColor: Color,
    subColor: Color,
    cardBg: Color,
    border: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val accent = Color(0xFF2563EB)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Confirmar registro",
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "Revise o abastecimento antes de salvar.",
                    color = subColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${drafts.size} ${if (drafts.size == 1) "registro" else "registros"}",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        drafts.forEachIndexed { index, draft ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "${index + 1}. Registro de abastecimento",
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                ReminderInfoRow(Icons.Default.CheckCircle, "Titulo", "Abastecimento - ${draft.carro.nome}", subColor)
                ReminderInfoRow(Icons.Default.DirectionsCar, "Veiculo", draft.carro.nome, subColor)
                ReminderInfoRow(Icons.Default.Event, "Data", draft.data, subColor)
                ReminderInfoRow(Icons.Default.Speed, "Km", "${draft.km} km", subColor)
                ReminderInfoRow(Icons.Default.Build, "Valor", "${formatarMoedaAi(draft.valorPago)} | ${formatarNumero(draft.litros)} L", subColor)
                if (draft.precoEstimado) {
                    Text(
                        text = "Preco por litro estimado: ${formatarMoedaAi(draft.precoLitro)}/L.",
                        color = subColor,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = subColor)
            ) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                Text("Salvar registro", fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun ServiceRecordDraftConfirmationCard(
    drafts: List<AiServiceRecordDraft>,
    titleColor: Color,
    subColor: Color,
    cardBg: Color,
    border: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val accent = Color(0xFF10B981)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Servico ja realizado",
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "Isso vai para o historico, sem aviso futuro.",
                    color = subColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${drafts.size} ${if (drafts.size == 1) "registro" else "registros"}",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        drafts.forEachIndexed { index, draft ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "${index + 1}. Registro no historico",
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                ReminderInfoRow(Icons.Default.CheckCircle, "Titulo", draft.titulo, subColor)
                ReminderInfoRow(Icons.Default.DirectionsCar, "Veiculo", draft.carro.nome, subColor)
                ReminderInfoRow(Icons.Default.Build, "Tipo", draft.tipo.label, subColor)
                ReminderInfoRow(Icons.Default.Event, "Execucao", draft.dataExecucao, subColor)
                ReminderInfoRow(
                    Icons.Default.CheckCircle,
                    "Valor",
                    draft.valor?.let { formatarMoedaAi(it) } ?: "nao informado",
                    subColor
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = subColor)
            ) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                Text("Salvar registro", fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun TypingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "vehicle_ai_typing")
    Row(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val lift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, delayMillis = index * 120),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "vehicle_ai_typing_dot_$index"
            )
            Box(
                modifier = Modifier
                    .padding(bottom = lift.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
fun AiFormattedMessage(
    text: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trim() == "---") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color.copy(alpha = 0.25f))
                )
            } else if (line.isNotBlank()) {
                Text(
                    text = buildBoldAnnotatedString(line),
                    color = color,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

fun buildBoldAnnotatedString(text: String) = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(part)
            }
        } else {
            append(part)
        }
    }
}

