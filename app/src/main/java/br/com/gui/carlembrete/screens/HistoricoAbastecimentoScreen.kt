import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import br.com.gui.carlembrete.AdminUsersSync
import br.com.gui.carlembrete.BancoDeDados
import br.com.gui.carlembrete.formatarMoedaLocal
import br.com.gui.carlembrete.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
private val GradientEnd = Color(0xFF1F2937)
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
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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

    val ordenados = remember(abastecimentos) {
        abastecimentos.sortedByDescending { parseLocalDateFlexible(it.data, formatter) }
    }
    val resumoGastos = remember(ordenados) {
        calcularResumoGastosAbastecimento(
            abastecimentos = ordenados,
            formatter = formatter
        )
    }
    val resumoConsumo = remember(abastecimentos) {
        calcularResumoConsumoAbastecimento(
            abastecimentos = abastecimentos
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
                    AdminUsersSync.syncFuelSnapshot(novaLista)
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
                    AdminUsersSync.syncFuelSnapshot(novaLista)
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
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(Color(0xFF0B1220), Color(0xFF111827))
                                    } else {
                                        listOf(Color(0xFFF8FAFC), Color(0xFFFFFFFF))
                                    }
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr("Resumo de consumo", "Consumption summary"),
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniResumoDestaque(
                                label = tr("Media por dia", "Daily average"),
                                value = resumoConsumo.litrosPorDia?.let {
                                    String.format(Locale("pt", "BR"), "%.2f L/dia", it)
                                } ?: "--",
                                isDark = isDark,
                                valueColor = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
                                modifier = Modifier.weight(1f)
                            )
                            MiniResumoDestaque(
                                label = tr("Litros", "Liters"),
                                value = String.format(Locale("pt", "BR"), "%.2f L", resumoConsumo.litrosTotais),
                                isDark = isDark,
                                valueColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = resumoConsumo.descricaoBase,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        HorizontalDivider(color = cardBorderColor.copy(alpha = 0.55f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Total do mes", "Month total"),
                                color = if (isDark) TextGray else Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatarMoedaLocal(resumoGastos.gastoMes),
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (ordenados.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.TopCenter
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
    val cardGradient = if (isDark) {
        Brush.verticalGradient(colors = listOf(GradientStart, GradientEnd))
    } else {
        Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFF8FAFC)))
    }
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

        // CARD DE CONTEÃšDO
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(cardGradient)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else AccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        Text(item.data, color = cardTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Edit, null, tint = cardBody, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Delete, null, tint = AlertRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFCBD5E1))
                    Spacer(Modifier.height(16.dp))

                    if (descricao.isNotBlank()) {
                        Text(
                            text = descricao,
                            color = cardBody,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(12.dp))
                    }


                    // --- RODAPÃ‰ COM INFORMAÃ‡Ã•ES INVERTIDAS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Total Pago",
                                color = cardBody,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = AccentGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    formatarMoedaLocal(item.valorPago),
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Litros",
                                color = cardBody,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = AccentBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "${String.format(Locale("pt", "BR"), "%.2f", item.litros)} L",
                                    color = if (isDark) Color(0xFF93C5FD) else AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun MiniResumoDestaque(
    label: String,
    value: String,
    isDark: Boolean,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val labelColor = if (isDark) TextGray else Color(0xFF64748B)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.62f) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = labelColor, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = valueColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            maxLines = 1
        )
    }
}

private data class ResumoGastosAbastecimento(
    val gastoMes: Double
)

private data class ResumoConsumoAbastecimento(
    val litrosPorDia: Double?,
    val litrosTotais: Double,
    val litrosConsiderados: Double,
    val diasConsiderados: Long?,
    val intervalosValidos: Int
) {
    val label: String
        get() = if (litrosPorDia != null) {
            val locale = Locale("pt", "BR")
            "${String.format(locale, "%.2f", litrosPorDia)} L/dia"
        } else if (litrosTotais > 0.0) {
            val locale = Locale("pt", "BR")
            "${String.format(locale, "%.2f", litrosTotais)} L registrados"
        } else {
            "--"
        }

    val descricaoBase: String
        get() {
            val locale = Locale("pt", "BR")
            return when {
                litrosPorDia != null && diasConsiderados != null && intervalosValidos > 0 -> {
                    "Baseado em $diasConsiderados dia(s) entre abastecimentos e " +
                        "${String.format(locale, "%.2f", litrosConsiderados)} L abastecidos em $intervalosValidos intervalo(s)."
                }
                litrosPorDia != null && diasConsiderados != null -> {
                    "Baseado em $diasConsiderados dia(s) com abastecimento registrado e " +
                        "${String.format(locale, "%.2f", litrosConsiderados)} L no total."
                }
                litrosTotais > 0.0 -> {
                    "Cadastre abastecimentos em dias diferentes para calcular a media de consumo por dia."
                }
                else -> {
                    "Cadastre abastecimentos para o app calcular o consumo medio por intervalo."
                }
            }
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
    abastecimentos: List<Abastecimento>
): ResumoConsumoAbastecimento {
    val litrosTotais = abastecimentos.sumOf { it.litros.coerceAtLeast(0.0) }
    val registrosComData = abastecimentos
        .mapIndexedNotNull { index, item ->
            val data = parseLocalDateFlexible(item.data, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                ?: return@mapIndexedNotNull null
            if (item.litros <= 0.0) return@mapIndexedNotNull null
            FuelConsumptionPoint(index = index, data = data, litros = item.litros)
        }
        .sortedWith(
            compareBy<FuelConsumptionPoint> { it.data }
                .thenBy { it.index }
        )

    val intervalos = registrosComData.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = java.time.temporal.ChronoUnit.DAYS.between(anterior.data, atual.data)
        val litros = atual.litros.coerceAtLeast(0.0)
        if (dias > 0L && litros > 0.0) dias to litros else null
    }

    if (intervalos.isEmpty() || litrosTotais <= 0.0) {
        val diasComRegistro = registrosComData
            .map { it.data }
            .distinct()
            .size
            .takeIf { it > 0 }
        val mediaPorDiaRegistrado = diasComRegistro?.let { litrosTotais / it.toDouble() }
        return ResumoConsumoAbastecimento(
            litrosPorDia = mediaPorDiaRegistrado?.takeIf { it.isFinite() && it > 0.0 },
            litrosTotais = litrosTotais,
            litrosConsiderados = litrosTotais.takeIf { it.isFinite() && it > 0.0 } ?: 0.0,
            diasConsiderados = diasComRegistro?.toLong(),
            intervalosValidos = 0
        )
    }

    val dias = intervalos.sumOf { it.first }
    val litrosConsiderados = intervalos.sumOf { it.second }
    val litrosPorDia = litrosConsiderados / dias.toDouble()

    return ResumoConsumoAbastecimento(
        litrosPorDia = litrosPorDia.takeIf { it.isFinite() && it > 0.0 },
        litrosTotais = litrosTotais,
        litrosConsiderados = litrosConsiderados.takeIf { it.isFinite() && it > 0.0 } ?: 0.0,
        diasConsiderados = dias.takeIf { it > 0L },
        intervalosValidos = intervalos.size
    )
}

private data class FuelConsumptionPoint(
    val index: Int,
    val data: LocalDate,
    val litros: Double
)

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
    var dataTexto by remember { mutableStateOf(item.data) }
    var descricaoTexto by remember { mutableStateOf(carregarDescricaoAbastecimento(context, item)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("Editar Abastecimento", color = titleColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = precoTexto,
                    onValueChange = { precoTexto = it },
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
                    onValueChange = { totalTexto = it },
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
                    value = descricaoTexto,
                    onValueChange = { descricaoTexto = it },
                    label = { Text("Descricao") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dataTexto,
                    onValueChange = { dataTexto = it },
                    label = { Text("Data") },
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
                salvarDescricaoAbastecimento(context, item.id, descricaoTexto)
                onConfirm(
                    item.copy(
                        data = dataTexto.ifBlank { item.data },
                        precoLitro = preco ?: item.precoLitro,
                        valorPago = total ?: item.valorPago,
                        litros = litros
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


