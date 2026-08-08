import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.gui.carlembrete.Abastecimento
import br.com.gui.carlembrete.AppPreferences
import br.com.gui.carlembrete.BancoDeDados
import br.com.gui.carlembrete.formatarMoedaLocal
import br.com.gui.carlembrete.isEnglishUi
import br.com.gui.carlembrete.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import androidx.core.view.WindowInsetsControllerCompat

// --- PALETA ZELLU ---
private val PrimaryDark = Color.Black
private val GradientStart = Color(0xFF111827)
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF22C55E)
private val AlertRed = Color(0xFFEF4444)
private val SurfaceDark = Color(0xFF111827)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoAbastecimentoScreen(carroId: String, onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) PrimaryDark else colorScheme.background
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFCBD5E1)
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var kmAtualCarro by remember { mutableStateOf(0) }
    var itemEdicao by remember { mutableStateOf<Abastecimento?>(null) }
    var itemExcluir by remember { mutableStateOf<Abastecimento?>(null) }
    var filtroCombustivel by remember { mutableStateOf<String?>(null) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    BackHandler {
        if (itemEdicao != null) {
            itemEdicao = null
        } else if (itemExcluir != null) {
            itemExcluir = null
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val (abastecimentosCarro, kmAtual) = withContext(Dispatchers.IO) {
                val listaAbastecimento = BancoDeDados.carregarAbastecimentos(context).filter { it.carroId == carroId }
                val carro = (BancoDeDados.carregarCarros(context) ?: emptyList()).firstOrNull { it.id == carroId }
                listaAbastecimento to (carro?.kmAtual ?: 0)
            }
            abastecimentos = abastecimentosCarro
            kmAtualCarro = kmAtual
        }
    }

    val opcoesCombustivel = remember(abastecimentos) {
        abastecimentos
            .map { tipoCombustivelHistorico(it) }
            .distinct()
            .sorted()
    }
    LaunchedEffect(opcoesCombustivel, filtroCombustivel) {
        if (filtroCombustivel != null && filtroCombustivel !in opcoesCombustivel) {
            filtroCombustivel = null
        }
    }
    val abastecimentosFiltrados = remember(abastecimentos, filtroCombustivel) {
        val filtro = filtroCombustivel
        if (filtro == null) {
            abastecimentos
        } else {
            abastecimentos.filter { tipoCombustivelHistorico(it) == filtro }
        }
    }
    val ordenados = remember(abastecimentosFiltrados) {
        abastecimentosFiltrados.sortedByDescending { parseLocalDateFlexible(it.data, formatter) }
    }
    val resumoGastos = remember(ordenados) {
        calcularResumoGastosAbastecimento(
            abastecimentos = ordenados,
            formatter = formatter
        )
    }
    val resumoConsumo = remember(ordenados, kmAtualCarro, carroId) {
        calcularResumoConsumoAbastecimento(
            context = context,
            carroId = carroId,
            kmAtualCarro = kmAtualCarro,
            abastecimentos = ordenados
        )
    }
    DisposableEffect(view, isDark, screenBg) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldStatusColor = window?.statusBarColor
        val oldLightStatus = insetsController?.isAppearanceLightStatusBars
        if (window != null && insetsController != null) {
            window.statusBarColor = screenBg.toArgb()
            insetsController.isAppearanceLightStatusBars = !isDark
        }
        onDispose {
            if (window != null && insetsController != null) {
                if (oldStatusColor != null) window.statusBarColor = oldStatusColor
                if (oldLightStatus != null) insetsController.isAppearanceLightStatusBars = oldLightStatus
            }
        }
    }

    if (itemEdicao != null) {
        DialogEditar(
            item = itemEdicao!!,
            onDismiss = { itemEdicao = null },
            onConfirm = { itemAtualizado ->
                val novaLista = abastecimentos.map { if (it.id == itemAtualizado.id) itemAtualizado else it }
                scope.launch {
                    withContext(Dispatchers.IO) {
                        BancoDeDados.salvarAbastecimentos(context, novaLista)
                    }
                    abastecimentos = novaLista
                    itemEdicao = null
                }
            },
            formatter = formatter
        )
    }

    if (itemExcluir != null) {
        DialogExcluir(
            onDismiss = { itemExcluir = null },
            onConfirm = {
                val novaLista = abastecimentos.filter { it.id != itemExcluir!!.id }
                scope.launch {
                    withContext(Dispatchers.IO) {
                        BancoDeDados.salvarAbastecimentos(context, novaLista)
                    }
                    abastecimentos = novaLista
                    itemExcluir = null
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historico", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Voltar", tint = titleColor, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (abastecimentos.isNotEmpty()) {
                    ResumoRoscaCombustivel(
                        abastecimentos = abastecimentos,
                        filtroCombustivel = filtroCombustivel,
                        onFiltroChange = { filtroCombustivel = it },
                        gastoMes = resumoGastos.gastoMes,
                        litrosFiltrados = resumoConsumo.litrosTotais,
                        isDark = isDark,
                        cardBorderColor = cardBorderColor,
                        titleColor = titleColor,
                        bodyColor = bodyColor
                    )
                }
            }

            if (ordenados.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.LocalGasStation, null, tint = bodyColor.copy(alpha = 0.35f), modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Sem registros ainda", color = bodyColor, fontSize = 16.sp)
                    }
                }
                }
            } else {
                itemsIndexed(ordenados) { index, item ->
                    TimelineItem(
                        item = item,
                        isLast = index == ordenados.lastIndex,
                        onEdit = { itemEdicao = item },
                        onDelete = { itemExcluir = item },
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    item: Abastecimento,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val descricao = remember(item.id) { carregarDescricaoAbastecimento(context, item) }
    val cardTitle = if (isDark) TextWhite else Color(0xFF0F172A)
    val cardBody = if (isDark) TextGray else Color(0xFF64748B)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // COLUNA DA LINHA DO TEMPO
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentBlue)
                    .border(BorderStroke(2.dp, if (isDark) Color.White else Color(0xFFE2E8F0)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalGasStation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(AccentBlue)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Card plano: fundo solido, borda fina, sem sombra nem gradiente. A versao
        // anterior tinha quatro mini-superficies com borda propria DENTRO do card
        // (chip de combustivel, faixa de KM, pilula do total, pilula dos litros) —
        // valor e tipografia, nao moldura, e moldura repetida vira ruido.
        Column(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) GradientStart else Color.White)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.data, color = cardTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val tipoCombustivelCard = tipoCombustivelHistorico(item)
                    if (tipoCombustivelCard != "Não informado") {
                        Text(
                            text = tipoCombustivelCard,
                            color = cardBody,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Edit, null, tint = cardBody, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Delete, null, tint = AlertRed.copy(alpha = 0.75f), modifier = Modifier.size(17.dp))
                    }
                }
            }

            if (descricao.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = descricao,
                    color = cardBody,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0))
            Spacer(Modifier.height(10.dp))

            // Estatisticas como texto puro, rotulo em cima e valor embaixo. So o total
            // guarda cor propria (verde de dinheiro); o resto herda a hierarquia do texto.
            Row(modifier = Modifier.fillMaxWidth()) {
                EstatisticaDoRegistro(
                    rotulo = tr("Total pago", "Total paid"),
                    valor = formatarMoedaLocal(item.valorPago),
                    corValor = AccentGreen,
                    tamanhoValor = 16.sp,
                    corRotulo = cardBody,
                    modifier = Modifier.weight(1.2f)
                )
                EstatisticaDoRegistro(
                    rotulo = tr("Litros", "Liters"),
                    valor = String.format(Locale("pt", "BR"), "%.2f L", item.litros),
                    corValor = cardTitle,
                    corRotulo = cardBody,
                    modifier = Modifier.weight(1f)
                )
                item.km?.takeIf { it > 0 }?.let { kmRegistrado ->
                    EstatisticaDoRegistro(
                        rotulo = "KM",
                        valor = formatarKmHistorico(kmRegistrado),
                        corValor = cardTitle,
                        corRotulo = cardBody,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }
    }
}

/** Rotulo apagado em cima, valor forte embaixo. Sem borda nem fundo. */
@Composable
private fun EstatisticaDoRegistro(
    rotulo: String,
    valor: String,
    corValor: Color,
    corRotulo: Color,
    modifier: Modifier = Modifier,
    tamanhoValor: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Column(modifier = modifier) {
        Text(text = rotulo, color = corRotulo, fontSize = 10.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = valor,
            color = corValor,
            fontWeight = FontWeight.Bold,
            fontSize = tamanhoValor,
            maxLines = 1
        )
    }
}

private class FatiaCombustivel(val nome: String, val valor: Double, val cor: Color)

/**
 * Cor fixa por combustivel, casada nos arcos e na legenda. Sai do nome, nao da posicao:
 * com cor posicional, filtrar a lista mudaria a cor de cada combustivel entre visitas.
 */
private fun corDoCombustivel(nome: String): Color {
    val n = nome.lowercase(Locale.getDefault())
    return when {
        "gasol" in n -> Color(0xFFF59E0B)
        "etanol" in n || "alcool" in n || "ethanol" in n -> Color(0xFF22C55E)
        "diesel" in n -> Color(0xFFF97316)
        "gnv" in n || "cng" in n -> Color(0xFF14B8A6)
        "flex" in n -> Color(0xFF8B5CF6)
        else -> Color(0xFF64748B)
    }
}

/**
 * Rosca de gasto por combustivel, com a legenda fazendo o papel do antigo dropdown:
 * tocar num combustivel filtra a linha do tempo, tocar de novo limpa. Menu para
 * escolher entre tres opcoes era peso demais.
 *
 * Os arcos usam sempre todos os registros — o filtro apaga as fatias nao selecionadas
 * em vez de redesenhar a rosca, senao filtrar viraria sempre um circulo cheio e o
 * grafico nao diria nada.
 */
@Composable
private fun ResumoRoscaCombustivel(
    abastecimentos: List<Abastecimento>,
    filtroCombustivel: String?,
    onFiltroChange: (String?) -> Unit,
    gastoMes: Double,
    litrosFiltrados: Double,
    isDark: Boolean,
    cardBorderColor: Color,
    titleColor: Color,
    bodyColor: Color
) {
    val fatias = remember(abastecimentos) {
        abastecimentos
            .groupBy { tipoCombustivelHistorico(it) }
            .map { (nome, itens) ->
                FatiaCombustivel(nome, itens.sumOf { it.valorPago.coerceAtLeast(0.0) }, corDoCombustivel(nome))
            }
            .filter { it.valor > 0.0 }
            .sortedByDescending { it.valor }
    }
    val totalGeral = fatias.sumOf { it.valor }
    if (totalGeral <= 0.0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF0B1220) else Color.White)
            .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tr("Resumo de consumo", "Consumption summary"),
                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            if (fatias.size > 1) {
                Text(
                    text = tr("Toque num combustivel para filtrar", "Tap a fuel type to filter"),
                    color = bodyColor,
                    fontSize = 10.sp
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(128.dp)) {
                    val traco = 14.dp.toPx()
                    val diametro = size.minDimension - traco
                    val canto = Offset(traco / 2f, traco / 2f)
                    val area = Size(diametro, diametro)
                    // Vao so quando ha mais de uma fatia: rosca de fatia unica com vao
                    // vira um circulo com uma mordida inexplicavel.
                    val vao = if (fatias.size > 1) 2.5f else 0f
                    var angulo = -90f
                    fatias.forEach { fatia ->
                        val varredura = (fatia.valor / totalGeral * 360.0).toFloat()
                        val apagada = filtroCombustivel != null && fatia.nome != filtroCombustivel
                        drawArc(
                            color = if (apagada) fatia.cor.copy(alpha = 0.22f) else fatia.cor,
                            startAngle = angulo + vao / 2f,
                            sweepAngle = (varredura - vao).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = canto,
                            size = area,
                            style = Stroke(width = traco)
                        )
                        angulo += varredura
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatarMoedaLocal(gastoMes),
                        color = AccentGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Text(
                        text = tr("este mes", "this month"),
                        color = bodyColor,
                        fontSize = 9.5.sp
                    )
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.2f L", litrosFiltrados),
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                fatias.forEach { fatia ->
                    val selecionada = filtroCombustivel == fatia.nome
                    val apagada = filtroCombustivel != null && !selecionada
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selecionada) fatia.cor.copy(alpha = if (isDark) 0.14f else 0.10f)
                                else Color.Transparent
                            )
                            .clickable {
                                onFiltroChange(if (selecionada) null else fatia.nome)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (apagada) fatia.cor.copy(alpha = 0.35f) else fatia.cor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = fatia.nome,
                            color = if (apagada) bodyColor.copy(alpha = 0.6f) else titleColor,
                            fontSize = 12.sp,
                            fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatarMoedaLocal(fatia.valor),
                            color = if (apagada) bodyColor.copy(alpha = 0.6f) else bodyColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private data class ResumoGastosAbastecimento(
    val gastoMes: Double
)

private data class ResumoConsumoAbastecimento(
    val kmPorLitro: Double?,
    val litrosPorKm: Double?,
    val litrosTotais: Double
) {
    val label: String
        get() = if (kmPorLitro != null && litrosPorKm != null) {
            val locale = Locale("pt", "BR")
            "${String.format(locale, "%.2f", kmPorLitro)} km/L • ${String.format(locale, "%.3f", litrosPorKm)} L/km"
        } else if (litrosTotais > 0.0) {
            val locale = Locale("pt", "BR")
            "${String.format(locale, "%.2f", litrosTotais)} L registrados"
        } else {
            "--"
        }
}

private fun calcularResumoGastosAbastecimento(
    abastecimentos: List<Abastecimento>,
    formatter: DateTimeFormatter
): ResumoGastosAbastecimento {
    val hoje = LocalDate.now()
    val inicioMes = hoje.withDayOfMonth(1)

    val gastosMes = abastecimentos.sumOf { item ->
        val data = parseLocalDateFlexible(item.data, formatter)
        if (data != null && !data.isBefore(inicioMes) && !data.isAfter(hoje)) item.valorPago else 0.0
    }

    return ResumoGastosAbastecimento(
        gastoMes = gastosMes
    )
}

private fun parseLocalDateFlexible(raw: String, fallbackFormatter: DateTimeFormatter): LocalDate? {
    val valor = raw.trim()
    if (valor.isBlank()) return null

    runCatching { return LocalDate.parse(valor, fallbackFormatter) }
    runCatching { return LocalDate.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE) }
    runCatching { return Instant.parse(valor).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }

    val formatosDataHora = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    )
    formatosDataHora.forEach { formato ->
        try {
            return LocalDateTime.parse(valor, formato).toLocalDate()
        } catch (_: DateTimeParseException) {
        }
    }

    if (valor.length >= 10) {
        val prefixo = valor.substring(0, 10)
        runCatching { return LocalDate.parse(prefixo, DateTimeFormatter.ISO_LOCAL_DATE) }
    }
    return null
}

private fun calcularResumoConsumoAbastecimento(
    context: android.content.Context,
    carroId: String,
    kmAtualCarro: Int,
    abastecimentos: List<Abastecimento>
): ResumoConsumoAbastecimento {
    val litrosTotais = abastecimentos.sumOf { it.litros.coerceAtLeast(0.0) }
    val kmInicial = AppPreferences.getFuelStartKm(context, carroId)

    if (kmInicial == null || kmAtualCarro <= kmInicial || litrosTotais <= 0.0) {
        return ResumoConsumoAbastecimento(kmPorLitro = null, litrosPorKm = null, litrosTotais = litrosTotais)
    }

    val distancia = (kmAtualCarro - kmInicial).toDouble()
    if (distancia <= 0.0) {
        return ResumoConsumoAbastecimento(kmPorLitro = null, litrosPorKm = null, litrosTotais = litrosTotais)
    }

    val kmPorLitro = distancia / litrosTotais
    val litrosPorKm = litrosTotais / distancia

    return ResumoConsumoAbastecimento(
        kmPorLitro = kmPorLitro.takeIf { it.isFinite() && it > 0.0 },
        litrosPorKm = litrosPorKm.takeIf { it.isFinite() && it > 0.0 },
        litrosTotais = litrosTotais
    )
}

// ... Dialogs mantidos iguais ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEditar(
    item: Abastecimento,
    onDismiss: () -> Unit,
    onConfirm: (Abastecimento) -> Unit,
    formatter: DateTimeFormatter
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val dialogBg = if (isDark) SurfaceDark else Color.White
    val titleColor = colorScheme.onSurface
    val fieldTextColor = colorScheme.onSurface
    val fieldLabelColor = if (isDark) TextGray else colorScheme.onSurfaceVariant
    val fieldBorderColor = if (isDark) TextGray.copy(alpha = 0.5f) else Color(0xFFCBD5E1)
    val context = LocalContext.current
    var precoTexto by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", item.precoLitro)) }
    var totalTexto by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", item.valorPago)) }
    var dataSelecionada by remember { mutableStateOf(parseLocalDateFlexible(item.data, formatter) ?: LocalDate.now()) }
    var kmTexto by remember { mutableStateOf(item.km?.toString().orEmpty()) }
    var tipoCombustivel by remember { mutableStateOf(item.tipoCombustivel.orEmpty().ifBlank { "Gasolina" }) }
    var combustivelExpanded by remember { mutableStateOf(false) }
    val opcoesCombustivel = if (isEnglishUi()) {
        listOf("Gasoline", "Ethanol", "Diesel", "CNG", "Flex")
    } else {
        listOf("Gasolina", "Etanol", "Diesel", "GNV", "Flex")
    }
    val abrirDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dataSelecionada = LocalDate.of(year, month + 1, dayOfMonth)
            },
            dataSelecionada.year,
            dataSelecionada.monthValue - 1,
            dataSelecionada.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("Editar Abastecimento", color = titleColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = combustivelExpanded,
                    onExpandedChange = { combustivelExpanded = !combustivelExpanded }
                ) {
                    OutlinedTextField(
                        value = tipoCombustivel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Combustivel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = combustivelExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                            focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                            focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = combustivelExpanded,
                        onDismissRequest = { combustivelExpanded = false }
                    ) {
                        opcoesCombustivel.forEach { opcao ->
                            DropdownMenuItem(
                                text = { Text(opcao) },
                                onClick = {
                                    tipoCombustivel = opcao
                                    combustivelExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = precoTexto,
                    onValueChange = { precoTexto = formatarDecimalAbastecimentoInput(it) },
                    label = { Text("Preco por Litro") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalTexto,
                    onValueChange = { totalTexto = formatarDecimalAbastecimentoInput(it) },
                    label = { Text("Total Pago (R$)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kmTexto,
                    onValueChange = { kmTexto = it.filter(Char::isDigit).take(7) },
                    label = { Text("KM registrado") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dataSelecionada.format(formatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data") },
                    trailingIcon = {
                        IconButton(onClick = abrirDatePicker) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Selecionar data",
                                tint = fieldLabelColor
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val preco = precoTexto.replace(",", ".").toDoubleOrNull()
                val total = totalTexto.replace(",", ".").toDoubleOrNull()
                val litros = if (preco != null && total != null && preco > 0.0) total / preco else item.litros
                onConfirm(
                    item.copy(
                        data = dataSelecionada.format(formatter),
                        precoLitro = preco ?: item.precoLitro,
                        valorPago = total ?: item.valorPago,
                        litros = litros,
                        tipoCombustivel = tipoCombustivel,
                        km = kmTexto.filter(Char::isDigit).toIntOrNull()
                    )
                )
            }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Salvar", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = fieldLabelColor) } }
    )
}

private fun carregarDescricaoAbastecimento(context: Context, item: Abastecimento): String {
    val prefs = context.getSharedPreferences("fuel_desc_store", Context.MODE_PRIVATE)
    val descricaoSalva = prefs.getString(item.id, null)
    if (!descricaoSalva.isNullOrBlank()) return descricaoSalva
    return ""
}

private fun salvarDescricaoAbastecimento(context: Context, id: String, descricao: String) {
    context.getSharedPreferences("fuel_desc_store", Context.MODE_PRIVATE)
        .edit()
        .putString(id, descricao.trim())
        .apply()
}

private fun tipoCombustivelHistorico(item: Abastecimento): String {
    return item.tipoCombustivel.orEmpty().trim().ifBlank { "Não informado" }
}

private fun formatarKmHistorico(km: Int): String {
    return "${NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(km)} km"
}

private fun formatarKmInputAbastecimento(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(7)
    if (digits.isBlank()) return ""
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(digits.toInt())
}

private fun formatarDecimalAbastecimentoInput(raw: String): String {
    val normalizado = raw.replace('.', ',')
    val filtrado = normalizado.filter { it.isDigit() || it == ',' }
    val partes = filtrado.split(',', limit = 2)
    val inteiro = partes.getOrNull(0).orEmpty().filter(Char::isDigit).take(7)
    val decimal = partes.getOrNull(1)?.filter(Char::isDigit)?.take(2)
    return if (decimal != null) {
        "${inteiro.ifBlank { "0" }},$decimal"
    } else {
        inteiro
    }
}

@Composable
fun DialogExcluir(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val dialogBg = if (isDark) SurfaceDark else Color.White
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(AlertRed.copy(alpha = 0.15f), RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tr("Excluir registro?", "Delete record?"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Tem certeza que deseja apagar este abastecimento?", "Are you sure you want to delete this fuel entry?"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AlertRed)
                    ) {
                        Text(tr("Cancelar", "Cancel"), color = AlertRed)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                    ) {
                        Text(tr("Excluir", "Delete"), fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
            }
        }
    }
}


