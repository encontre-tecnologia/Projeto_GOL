package br.com.gui.carlembrete

import HistoricoAbastecimentoScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.app.DatePickerDialog
import androidx.compose.material.icons.filled.CalendarMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbastecimentoScreen(carroId: String, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val primaryDark = if (isDark) Color(0xFF0F172A) else scheme.background
    val surfaceDark = if (isDark) Color(0xFF1E293B) else scheme.surface
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF34D399)
    val cardStroke = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var precoGasolina by remember { mutableStateOf("5,60") }
    var valorAbastecido by remember { mutableStateOf("20,00") }
    val opcoesCombustivel = if (isEnglishUi()) {
        listOf("Gasoline", "Ethanol", "Diesel", "CNG", "Flex")
    } else {
        listOf("Gasolina", "Etanol", "Diesel", "GNV", "Flex")
    }
    var tipoCombustivel by remember { mutableStateOf("") }
    var combustivelExpanded by remember { mutableStateOf(false) }
    var precoEditado by remember { mutableStateOf(false) }
    var valorEditado by remember { mutableStateOf(false) }
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var kmAtualVeiculo by remember { mutableStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }
    var showSalvarSucessoDialog by remember { mutableStateOf(false) }
    var showHistoricoScreen by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    var dataSelecionada by remember { mutableStateOf(LocalDate.now()) }
    val preco = precoGasolina.replace(",", ".").toDoubleOrNull()
    val total = valorAbastecido.replace(",", ".").toDoubleOrNull()
    val litros = if (preco != null && total != null && preco > 0.0) total / preco else null
    val litrosTexto = litros?.let { String.format(Locale("pt", "BR"), "%.2f L", it) } ?: "--"
    val gastoTexto = total?.let { formatarMoeda(it) } ?: "--"
    val resumoConsumo = remember(abastecimentos, kmAtualVeiculo) {
        calcularResumoConsumo(
            abastecimentos = abastecimentos.filter { it.carroId == carroId },
            formatter = dateFormatter,
            kmAtual = kmAtualVeiculo,
            kmInicial = AppPreferences.getFuelStartKm(context, carroId)
        )
    }
    val canSave = preco != null &&
        total != null &&
        preco > 0.0 &&
        total > 0.0 &&
        tipoCombustivel.isNotBlank() &&
        precoEditado &&
        valorEditado &&
        !isSaving
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        cursorColor = textPrimary,
        focusedBorderColor = if (isDark) Color(0xFF334155) else Color.Black,
        unfocusedBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
        focusedLabelColor = textPrimary,
        unfocusedLabelColor = textDim,
        focusedLeadingIconColor = if (isDark) Color(0xFFCBD5F5) else Color(0xFF334155),
        unfocusedLeadingIconColor = textDim,
        focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
        unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White
    )

    LaunchedEffect(Unit) {
        abastecimentos = BancoDeDados.carregarAbastecimentos(context)
        kmAtualVeiculo = BancoDeDados.carregarCarros(context)
            ?.firstOrNull { it.id == carroId }
            ?.kmAtual ?: 0
    }

    if (showHistoricoScreen) {
        HistoricoAbastecimentoScreen(
            carroId = carroId,
            onDismiss = { showHistoricoScreen = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = primaryDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        tr("Abastecimento", "Fuel"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, cardStroke)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        tr("Dados do posto", "Gas station data"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    ExposedDropdownMenuBox(
                        expanded = combustivelExpanded,
                        onExpandedChange = { combustivelExpanded = !combustivelExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (tipoCombustivel.isBlank()) tr("Selecione o tipo", "Select type") else tipoCombustivel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr("Combustivel", "Fuel type")) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(58.dp),
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = combustivelExpanded) },
                            colors = fieldColors
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
                        value = precoGasolina,
                        onValueChange = {
                            precoGasolina = it
                            precoEditado = true
                        },
                        label = { Text(tr("Valor do combustivel (R$/L)", "Fuel price (R$/L)")) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.LocalGasStation,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFCBD5F5) else Color(0xFF334155)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = valorAbastecido,
                        onValueChange = {
                            valorAbastecido = it
                            valorEditado = true
                        },
                        label = { Text(tr("Valor abastecido (R$)", "Refuel amount (R$)")) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFCBD5F5) else Color(0xFF334155)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    val abrirDatePicker = {
                        val dataAtual = dataSelecionada
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dataSelecionada = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            dataAtual.year,
                            dataAtual.monthValue - 1,
                            dataAtual.dayOfMonth
                        ).show()
                    }
                    TextField(
                        value = dataSelecionada.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Data do registro", "Record date")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            IconButton(onClick = abrirDatePicker) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = tr("Selecionar data", "Select date"),
                                    tint = if (isDark) Color(0xFFCBD5F5) else Color(0xFF334155)
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = if (isDark) Color(0xFF0B1224) else Color(0xFFF8FAFC),
                            unfocusedContainerColor = if (isDark) Color(0xFF0B1224) else Color(0xFFF8FAFC),
                            focusedIndicatorColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                            unfocusedIndicatorColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                            focusedLabelColor = textDim,
                            unfocusedLabelColor = textDim,
                            cursorColor = textPrimary
                        )
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, cardStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(tr("Resumo", "Summary"), color = textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ResumoItem(
                            title = tr("Litros", "Liters"),
                            value = litrosTexto,
                            accent = accentBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ResumoItem(
                            title = tr("Gasto", "Amount"),
                            value = gastoTexto,
                            accent = accentGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = cardStroke.copy(alpha = 0.65f))
                    InfoRow(
                        label = tr("Consumo medio (km/L)", "Average consumption (km/L)"),
                        value = resumoConsumo.consumoKmPorLitro?.let { String.format(Locale("pt", "BR"), "%.1f", it) } ?: "--",
                        textLight = textPrimary,
                        textDim = textDim
                    )
                    InfoRow(
                        label = tr("Abastecimento semanal", "Weekly fuel"),
                        value = resumoConsumo.custoSemana?.let { formatarMoeda(it) } ?: "--",
                        textLight = textPrimary,
                        textDim = textDim
                    )
                    InfoRow(
                        label = tr("Abastecimento mensal", "Monthly fuel"),
                        value = resumoConsumo.custoMes?.let { formatarMoeda(it) } ?: "--",
                        textLight = textPrimary,
                        textDim = textDim
                    )
                }
            }

            Button(
                onClick = {
                    val precoValue = preco ?: return@Button
                    val totalValue = total ?: return@Button
                    val litrosCalculados = totalValue / precoValue
                    val data = dataSelecionada.format(dateFormatter)
                    val novo = Abastecimento(
                        carroId = carroId,
                        data = data,
                        precoLitro = precoValue,
                        valorPago = totalValue,
                        litros = litrosCalculados
                    )
                    val carroAtual = BancoDeDados.carregarCarros(context)
                        ?.firstOrNull { it.id == carroId }
                    if (carroAtual != null && AppPreferences.getFuelStartKm(context, carroId) == null) {
                        AppPreferences.setFuelStartKm(context, carroId, carroAtual.kmAtual)
                    }
                    val atualizada = abastecimentos + novo
                    isSaving = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            BancoDeDados.salvarAbastecimentos(context, atualizada)
                        }
                        abastecimentos = atualizada
                        precoGasolina = "5,60"
                        valorAbastecido = "20,00"
                        tipoCombustivel = ""
                        precoEditado = false
                        valorEditado = false
                        isSaving = false
                        showSalvarSucessoDialog = true
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isSaving) tr("Salvando...", "Saving...") else tr("Salvar gasto", "Save expense"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }

    if (showSalvarSucessoDialog) {
        Dialog(onDismissRequest = { showSalvarSucessoDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, cardStroke)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(accentBlue.copy(alpha = 0.15f), RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalGasStation,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            tr("Abastecimento registrado", "Fuel record saved"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tr("O gasto registrado esta na tela Historico de abastecimento.", "Your expense is now in Fuel history."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textDim,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSalvarSucessoDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, accentBlue)
                        ) {
                            Text(tr("Fechar", "Close"), color = accentBlue)
                        }

                        Button(
                            onClick = {
                                showSalvarSucessoDialog = false
                                showHistoricoScreen = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                        ) {
                            Text(tr("Historico", "History"), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, textLight: Color, textDim: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textDim, fontSize = 12.sp)
        Text(value, color = textLight, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun ResumoItem(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (isDark) Color(0xFF0B1224) else scheme.background
    val border = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.12f)
    val titleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = titleColor, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

private data class ResumoConsumo(
    val consumoKmPorLitro: Double?,
    val custoSemana: Double?,
    val custoMes: Double?
)

private fun calcularResumoConsumo(
    abastecimentos: List<Abastecimento>,
    formatter: DateTimeFormatter,
    kmAtual: Int,
    kmInicial: Int?
): ResumoConsumo {
    val entries = abastecimentos.mapNotNull { item ->
        val data = runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
        if (data != null && item.litros > 0.0 && item.valorPago > 0.0) {
            Triple(data, item.litros, item.valorPago)
        } else {
            null
        }
    }.sortedBy { it.first }

    if (entries.isEmpty()) {
        return ResumoConsumo(
            consumoKmPorLitro = null,
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

    val totalLitros = entries.sumOf { it.second }
    val consumoKmPorLitro = if (kmInicial != null && kmAtual > kmInicial && totalLitros > 0.0) {
        (kmAtual - kmInicial) / totalLitros
    } else {
        null
    }

    return ResumoConsumo(
        consumoKmPorLitro = consumoKmPorLitro,
        custoSemana = mediaCustoDia * 7.0,
        custoMes = mediaCustoDia * 30.0
    )
}

