import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.WaterDrop
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
import br.com.gui.carlembrete.AppPreferences
import br.com.gui.carlembrete.BancoDeDados
import br.com.gui.carlembrete.formatarMoedaLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.core.view.WindowInsetsControllerCompat

// --- PALETA ZELLU ---
private val PrimaryDark = Color(0xFF0F172A)
private val GradientStart = Color(0xFF334155)
private val GradientEnd = Color(0xFF1E293B)
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF22C55E)
private val AlertRed = Color(0xFFEF4444)
private val SurfaceDark = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoAbastecimentoScreen(carroId: String, onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) PrimaryDark else Color.White
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFCBD5E1)
    val summaryGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    }
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var kmAtualVeiculo by remember { mutableStateOf(0) }
    var itemEdicao by remember { mutableStateOf<Abastecimento?>(null) }
    var itemExcluir by remember { mutableStateOf<Abastecimento?>(null) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    LaunchedEffect(Unit) {
        scope.launch {
            abastecimentos = withContext(Dispatchers.IO) {
                BancoDeDados.carregarAbastecimentos(context).filter { it.carroId == carroId }
            }
            kmAtualVeiculo = withContext(Dispatchers.IO) {
                BancoDeDados.carregarCarros(context)
                    ?.firstOrNull { it.id == carroId }
                    ?.kmAtual ?: 0
            }
        }
    }

    val ordenados = remember(abastecimentos) {
        abastecimentos.sortedByDescending { runCatching { LocalDate.parse(it.data, formatter) }.getOrNull() }
    }
    val resumoConsumo = remember(ordenados, kmAtualVeiculo) {
        calcularResumoConsumoHistorico(
            abastecimentos = ordenados,
            formatter = formatter,
            kmAtual = kmAtualVeiculo,
            kmInicial = AppPreferences.getFuelStartKm(context, carroId)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .background(summaryGradient)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocalGasStation, contentDescription = null, tint = if (isDark) Color.White else AccentBlue, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Resumo de abastecimentos", color = titleColor, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResumoChip(
                            "Total esta semana",
                            resumoConsumo.custoSemana?.let { formatarMoedaLocal(it) } ?: "--",
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        ResumoChip(
                            "Total este mes",
                            resumoConsumo.custoMes?.let { formatarMoedaLocal(it) } ?: "--",
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (ordenados.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.LocalGasStation, null, tint = bodyColor.copy(alpha = 0.35f), modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Sem registros ainda", color = bodyColor, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
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
}

@Composable
private fun ResumoChip(label: String, value: String, isDark: Boolean, modifier: Modifier = Modifier) {
    val valueColor = if (isDark) TextWhite else Color(0xFF0F172A)
    val labelColor = if (isDark) TextGray else Color(0xFF475569)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.88f))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(label, color = labelColor, fontSize = 10.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
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

                    // --- RODAPÃ‰ COM INFORMAÃ‡Ã•ES INVERTIDAS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
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
                    }
                }
            }
        }
    }
}

private data class ResumoConsumoHistorico(
    val mediaLitrosPorAbastecimento: Double?,
    val custoSemana: Double?,
    val custoMes: Double?
)

private fun calcularResumoConsumoHistorico(
    abastecimentos: List<Abastecimento>,
    formatter: DateTimeFormatter,
    kmAtual: Int,
    kmInicial: Int?
): ResumoConsumoHistorico {
    val entries = abastecimentos.mapNotNull { item ->
        val data = runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
        if (data != null && item.litros > 0.0 && item.valorPago > 0.0) {
            Triple(data, item.litros, item.valorPago)
        } else {
            null
        }
    }.sortedBy { it.first }

    if (entries.isEmpty()) {
        return ResumoConsumoHistorico(
            mediaLitrosPorAbastecimento = null,
            custoSemana = null,
            custoMes = null
        )
    }

    val mediaDias = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.first, atual.first)
        if (dias <= 0) null else dias.toDouble()
    }.average().takeIf { !it.isNaN() } ?: 7.0

    val mediaCustoDia = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.first, atual.first)
        if (dias <= 0) null else atual.third / dias.toDouble()
    }.average().takeIf { !it.isNaN() } ?: (entries.last().third / mediaDias)

    val mediaLitrosPorAbastecimento = entries.map { it.second }.average().takeIf { !it.isNaN() }

    return ResumoConsumoHistorico(
        mediaLitrosPorAbastecimento = mediaLitrosPorAbastecimento,
        custoSemana = mediaCustoDia * 7.0,
        custoMes = mediaCustoDia * 30.0
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
    var dataSelecionada by remember { mutableStateOf(runCatching { LocalDate.parse(item.data, formatter) }.getOrNull() ?: LocalDate.now()) }

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
                    value = dataSelecionada.format(formatter),
                    onValueChange = {}, readOnly = true,
                    label = { Text("Data") },
                    trailingIcon = { Icon(Icons.Rounded.Edit, null, tint = AccentBlue) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor, unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = fieldBorderColor,
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = fieldLabelColor
                    ),
                    modifier = Modifier.fillMaxWidth().clickable {
                        DatePickerDialog(context, { _, y, m, d -> dataSelecionada = LocalDate.of(y, m + 1, d) }, dataSelecionada.year, dataSelecionada.monthValue - 1, dataSelecionada.dayOfMonth).show()
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val preco = precoTexto.replace(",", ".").toDoubleOrNull()
                val total = totalTexto.replace(",", ".").toDoubleOrNull()
                val litros = if (preco != null && total != null && preco > 0.0) total / preco else item.litros
                onConfirm(item.copy(data = dataSelecionada.format(formatter), precoLitro = preco ?: item.precoLitro, valorPago = total ?: item.valorPago, litros = litros))
            }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Salvar", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = fieldLabelColor) } }
    )
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
                        "Excluir registro?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tem certeza que deseja apagar este abastecimento?",
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
                        Text("Cancelar", color = AlertRed)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                    ) {
                        Text("Excluir", fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
            }
        }
    }
}


