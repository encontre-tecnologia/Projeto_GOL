package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MecanicoVirtualScreen(
    carros: List<CarroInfo>,
    abastecimentos: List<Abastecimento>,
    lembretes: List<Lembrete>,
    isPremium: Boolean,
    onPremiumRequired: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val themeMode = AppPreferences.getThemeMode(context)
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val primaryDark = if (isDark) Color(0xFF0B1224) else Color(0xFFF8FAFC)
    val surfaceDark = if (isDark) Color(0xFF0F172A) else Color.White
    val textLight = if (isDark) Color.White else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)
    val searchBorder = if (isDark) Color(0xFF334155) else Color.Black
    val accent = Color(0xFF3B82F6)          // Azul destaque
    val success = Color(0xFF10B981)         // Verde sucesso
    val warning = Color(0xFFF59E0B)         // Amarelo alerta
    val danger = Color(0xFFEF4444)          // Vermelho perigo

    val kmMesInput = remember { mutableStateOf("1200") }
    val kmMes = kmMesInput.value.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1200.0
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val totalGastoCombustivel = abastecimentos.sumOf { it.valorPago }
    val gastoTotalTexto = if (totalGastoCombustivel > 0.0) formatarMoedaMV(totalGastoCombustivel) else "--"
    val (tituloReputacao, _) = calcularReputacao(lembretes)

    // Calcula o valor total mensal necessario com base nos lembretes ativos
    val totalMensalParaGuardar = lembretes.fold(0.0) { total, lembrete ->
        val dataOrdenacao = dataParaOrdenacao(lembrete)
        val diasRestantes = if (dataOrdenacao == LocalDate.MAX) 0 else ChronoUnit.DAYS.between(LocalDate.now(), dataOrdenacao).toInt()
        val meses = if (diasRestantes <= 0) 1 else ceil(diasRestantes / 30.0).toInt()
        total + if (lembrete.valor > 0) lembrete.valor / meses else 0.0
    }

    val proximosAvisos: List<Lembrete> = remember(lembretes) {
        lembretes.sortedBy { dataParaOrdenacao(it) }
    }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val currentMonth = YearMonth.now()
    val gastoCombustivelMesFrota = abastecimentos.filter {
        runCatching { LocalDate.parse(it.data, formatter) }.getOrNull()?.let { d ->
            YearMonth.from(d) == currentMonth
        } ?: false
    }.sumOf { it.valorPago }
    val abastecimentosUltimos30Dias = abastecimentos.count {
        runCatching { LocalDate.parse(it.data, formatter) }.getOrNull()?.let { d ->
            !d.isBefore(LocalDate.now().minusDays(30))
        } ?: false
    }
    val gastoPorVeiculo = carros.associate { carro ->
        val gastoManutencao = lembretes.filter { it.carroId == carro.id }.sumOf { it.valor }
        val gastoCombustivel = abastecimentos.filter { it.carroId == carro.id }.sumOf { it.valorPago }
        carro to (gastoManutencao + gastoCombustivel)
    }.entries.sortedByDescending { it.value }

    Scaffold(
        containerColor = primaryDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textLight)
                }
                Text(
                    "Gestor de Frota",
                    color = textLight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        val uri = gerarPdfFinanceiro(context, carros, abastecimentos, lembretes)
                        if (uri != null) {
                            compartilharPdf(context, uri)
                        }
                    },
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textLight
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Relatorio financeiro em PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // 1. CARD DE INPUT DE KM (Visual mais limpo)
            // 2. DASHBOARD FINANCEIRO (Hero Section)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF059669), Color(0xFF047857))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFD1FAE5), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reserva Mensal Recomendada", color = Color(0xFFD1FAE5), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        formatarMoedaMV(totalMensalParaGuardar),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Guardando esse valor todo mes, voce cobre as manutencoes abaixo sem aperto.",
                        color = Color(0xFFA7F3D0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = textLight)
                        Spacer(Modifier.width(8.dp))
                        Text("Gestao de combustivel (Frota)", color = textLight, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UsageStat(
                            label = "Total geral",
                            value = gastoTotalTexto,
                            textColor = textLight,
                            dimColor = textDim,
                            valueColor = warning,
                            backgroundColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC),
                            borderColor = cardBorder,
                            modifier = Modifier.weight(1f)
                        )
                        UsageStat(
                            label = "No mes",
                            value = formatarMoedaMV(gastoCombustivelMesFrota),
                            textColor = textLight,
                            dimColor = textDim,
                            valueColor = accent,
                            backgroundColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC),
                            borderColor = cardBorder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "Registros nos ultimos 30 dias: $abastecimentosUltimos30Dias",
                        color = textDim,
                        fontSize = 12.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Gastos por veiculo", color = textLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    var buscaVeiculo by remember { mutableStateOf("") }
                    var termoBusca by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchTextField(
                            value = buscaVeiculo,
                            onValueChange = { buscaVeiculo = it },
                            placeholder = "Buscar veiculo",
                            textColor = textLight,
                            placeholderColor = textDim,
                            borderColor = searchBorder,
                            modifier = Modifier
                                .weight(0.75f)
                                .height(46.dp)
                        )
                        OutlinedButton(
                            onClick = { termoBusca = buscaVeiculo; keyboardController?.hide(); focusManager.clearFocus() },
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF3B82F6),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(22.dp))
                        }
                    }
                    if (gastoPorVeiculo.isEmpty()) {
                        Text("Nenhum gasto registrado.", color = textDim, fontSize = 12.sp)
                    } else {
                        val filtro = termoBusca.trim().lowercase(Locale.getDefault())
                        val gastoFiltrado = if (filtro.isBlank()) {
                            gastoPorVeiculo
                        } else {
                            gastoPorVeiculo.filter { (carro, _) ->
                                listOf(carro.nome, carro.marca, carro.modelo, carro.tipoVeiculo.label)
                                    .any { it.lowercase(Locale.getDefault()).contains(filtro) }
                            }
                        }
                        if (gastoFiltrado.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = textDim.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Nenhum veiculo encontrado", color = textDim, fontSize = 12.sp)
                            }
                        } else {
                            gastoFiltrado.forEach { (carro, total) ->
                            val abastecimentosCarro = abastecimentos.filter { it.carroId == carro.id }
                            val lembretesCarro = lembretes.filter { it.carroId == carro.id }
                            val combustivelMesCarro = abastecimentosCarro.filter {
                                val data = try { LocalDate.parse(it.data, formatter) } catch (_: Exception) { null }
                                data != null && YearMonth.from(data) == currentMonth
                            }.sumOf { it.valorPago }
                            val manutencoesMesCarro = lembretesCarro.mapNotNull { lembrete ->
                                val data = try { LocalDate.parse(lembrete.dataLimite, formatter) } catch (_: Exception) { null }
                                data?.let { lembrete to it }
                            }.filter { (_, data) ->
                                YearMonth.from(data) == currentMonth
                            }.sumOf { (lembrete, _) -> lembrete.valor }
                            val manutencoesFuturasCarro = lembretesCarro.mapNotNull { lembrete ->
                                val data = try { LocalDate.parse(lembrete.dataLimite, formatter) } catch (_: Exception) { null }
                                data?.let { lembrete to it }
                            }.filter { (_, data) ->
                                data.isAfter(LocalDate.now())
                            }.sumOf { (lembrete, _) -> lembrete.valor }
                            VehicleSpendCard(
                                carro = carro,
                                total = total,
                                combustivelMes = combustivelMesCarro,
                                manutencoesMes = manutencoesMesCarro,
                                manutencoesFuturas = manutencoesFuturasCarro,
                                isDark = isDark,
                                textLight = textLight,
                                textDim = textDim,
                                borderColor = cardBorder,
                                accent = accent,
                                warning = warning,
                                danger = danger,
                                success = success
                            )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun BasicTextFieldCustom(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    accent: Color
) {
    Surface(
        color = Color.Black.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = color,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Text("km", color = color.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun AvisoCardModerno(
    lembrete: Lembrete,
    backgroundColor: Color,
    textColor: Color,
    dimColor: Color,
    accentColor: Color,
    successColor: Color,
    warningColor: Color,
    dangerColor: Color
) {
    val titulo = lembrete.titulo.ifBlank { lembrete.peca.ifBlank { "Manutencao" } }
    val dataLabel = lembrete.dataLimite.ifBlank { "--/--" }

    // Calculos de tempo e custo
    val dataOrdenacao = dataParaOrdenacao(lembrete)
    val diasRestantes = if (dataOrdenacao == LocalDate.MAX) null else ChronoUnit.DAYS.between(LocalDate.now(), dataOrdenacao).toInt()

    val mesesRestantes = diasRestantes?.let { dias ->
        if (dias <= 0) 1 else ceil(dias / 30.0).toInt()
    } ?: 1

    val guardarPorMes = if (lembrete.valor > 0.0) lembrete.valor / mesesRestantes else 0.0
    val progresso = if (diasRestantes == null) 0.0f else (1f - (diasRestantes / 365f)).coerceIn(0.1f, 1f) // Exemplo visual

    // Define cor baseada na urgencia
    val statusColor = when {
        diasRestantes != null && diasRestantes < 30 -> dangerColor
        diasRestantes != null && diasRestantes < 90 -> warningColor
        else -> accentColor
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECALHO
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    TipoIcon(
                        tipo = lembrete.tipo,
                        tint = statusColor,
                        size = 22.dp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(titulo, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = dimColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(dataLabel, color = dimColor, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = dimColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "KM limite: ${lembrete.kmLimite.ifBlank { "-" }}",
                            color = dimColor,
                            fontSize = 12.sp
                        )
                    }
                }
                // Tag de Valor Total
                if (lembrete.valor > 0) {
                    Surface(
                        color = backgroundColor.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, dimColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            formatarMoedaMV(lembrete.valor),
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // CORPO: META FINANCEIRA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("RESERVAR MENSALMENTE", color = dimColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (guardarPorMes > 0) formatarMoedaMV(guardarPorMes) else "R$ 0,00",
                        color = successColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                VerticalDivider(color = dimColor.copy(alpha = 0.2f), modifier = Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text("PRAZO ESTIMADO", color = dimColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (mesesRestantes == 1) "1 mes" else "$mesesRestantes meses",
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // RODAPE: IMPACTO (Alerta)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(dangerColor.copy(alpha = 0.1f))
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = dangerColor.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Risco: ${impactoSeNaoTrocar(lembrete.tipo)}",
                    color = dangerColor.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun UsageStat(
    label: String,
    value: String,
    textColor: Color,
    dimColor: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = textColor,
    backgroundColor: Color = Color(0xFFF8FAFC),
    borderColor: Color = Color(0xFFE2E8F0)
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, color = dimColor, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    thickness: androidx.compose.ui.unit.Dp = 1.dp
) {
    Box(
        modifier
            .fillMaxHeight()
            .width(thickness)
            .background(color = color)
    )
}

// --- FUNCOES DE LOGICA DE NEGOCIO (Mantidas para integridade) ---
private fun impactoSeNaoTrocar(tipo: TipoManutencao): String = when (tipo) {
    TipoManutencao.CORRENTE -> "Desgaste e risco de quebra da corrente"
    TipoManutencao.LUBRIFICACAO -> "Atrito elevado e desgaste acelerado"
    TipoManutencao.PEDIVELA -> "Folgas e perda de eficiencia na pedalada"
    TipoManutencao.ACESSORIOS -> "Falhas ou quebras de itens adicionais"
    TipoManutencao.CONFORTO -> "Desgaste de itens que afetam o conforto"
    TipoManutencao.PNEU -> "Perda de aderencia e risco de furo"
    TipoManutencao.TRANSMISSAO -> "Trocas de marcha imprecisas"
    TipoManutencao.REVISAO -> "Falhas gerais e desgaste acumulado"
    TipoManutencao.OLEO -> "Desgaste severo do motor"
    TipoManutencao.BATERIA -> "Falha na partida (pane)"
    TipoManutencao.MECANICA -> "Quebras maiores e guincho"
    TipoManutencao.FUNILARIA -> "Risco de corrosao e perda de acabamento"
    TipoManutencao.FREIO -> "Perda de freio (acidente)"
    TipoManutencao.LICENCIAMENTO -> "Apreensao do veiculo"
    TipoManutencao.IPVA -> "Bloqueio de documento"
    TipoManutencao.SEGURO -> "Risco financeiro em caso de sinistro"
    TipoManutencao.OUTROS -> "Falhas inesperadas"
}

@Composable
fun EmptyStateCard(surfaceColor: Color, dimColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Build, contentDescription = null, tint = dimColor.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Tudo tranquilo por aqui", color = dimColor, fontWeight = FontWeight.SemiBold)
        Text("Adicione manutencoes para ver previsoes.", color = dimColor.copy(alpha = 0.5f), fontSize = 14.sp)
    }
}

fun formatarMoedaMV(valor: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
}

@Composable
private fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    placeholderColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            textStyle = TextStyle(color = textColor, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isEmpty()) {
            Text(placeholder, color = placeholderColor, fontSize = 13.sp)
        }
    }
}


