package br.com.gui.carlembrete

import android.widget.Toast
import HistoricoAbastecimentoScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarroInfoScreen(
    carro: CarroInfo,
    lembretes: List<Lembrete>,
    isPremium: Boolean,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageLight = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textLight = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentColor = Color(0xFF38BDF8)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.18f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
    val pdfAccent = if (isDark) Color.White else Color.Black
    val pdfContainer = Color.Transparent

    val lembretesSemAbastecimento = lembretes.filter { it.tipo != TipoManutencao.ABASTECIMENTO }
    val lembretesAtivos = lembretesSemAbastecimento.filterNot(::isLembreteRealizado)
    val lembretesRealizados = lembretesSemAbastecimento.filter(::isLembreteRealizado)
    val totalGastos = lembretes.sumOf { it.valor }
    val context = LocalContext.current
    val view = LocalView.current
    var showHistoricoConsumo by remember { mutableStateOf(false) }
    val isBikeType = carro.tipoVeiculo == TipoVeiculo.BICICLETA || carro.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA

    DisposableEffect(view, isDark) {
        val activity = view.context as? android.app.Activity
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldStatusColor = window?.statusBarColor
        val oldNavColor = window?.navigationBarColor
        val oldLightStatus = insetsController?.isAppearanceLightStatusBars
        val oldLightNav = insetsController?.isAppearanceLightNavigationBars

        if (window != null && insetsController != null) {
            window.statusBarColor = pageLight.toArgb()
            window.navigationBarColor = pageLight.toArgb()
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }

        onDispose {
            if (window != null && insetsController != null) {
                if (oldStatusColor != null) window.statusBarColor = oldStatusColor
                if (oldNavColor != null) window.navigationBarColor = oldNavColor
                if (oldLightStatus != null) insetsController.isAppearanceLightStatusBars = oldLightStatus
                if (oldLightNav != null) insetsController.isAppearanceLightNavigationBars = oldLightNav
            }
        }
    }

    // Lógica de dados
    val proximo = lembretesAtivos.minByOrNull { dataParaOrdenacao(it) }?.let {
        val data = dataParaOrdenacao(it)
        if (data == LocalDate.MAX) null else data.format(DateTimeFormatter.ofPattern("dd/MM"))
    } ?: "--"
    val anoVeiculo = extrairAnoVeiculo(carro.modelo) ?: "--"
    val modeloSemAno = removerAnoDoModelo(carro.modelo).ifBlank { carro.modelo.ifBlank { "--" } }
    var precoTabelaFipe by remember(carro.id, carro.marca, carro.modelo, carro.tipoVeiculo) { mutableStateOf<String?>(null) }
    var carregandoPrecoFipe by remember(carro.id, carro.marca, carro.modelo, carro.tipoVeiculo) { mutableStateOf(false) }

    val corNome = corNomePorArgb(carro.corArgb)
    val (tituloSaudeOriginal, descricaoSaude) = calcularReputacao(lembretes)
    val saudeCritica = tituloSaudeOriginal.trim().lowercase(Locale("pt", "BR")).contains("crit")
    val tituloSaude = if (saudeCritica) "Crítica" else "Em dia"
    val kmAtualResumo = if (carro.kmAtual > 0) {
        "${NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(carro.kmAtual)} km"
    } else {
        "--"
    }

    val corSaude = if (saudeCritica) Color(0xFFEF4444) else Color(0xFF10B981)

    val historicoManutencoes = (
        lembretesAtivos
            .mapNotNull { lembrete ->
                val data = dataParaOrdenacao(lembrete)
                if (data == LocalDate.MAX) null else data to lembrete
            }
            .filter { (data, _) -> data.isBefore(LocalDate.now()) } +
        lembretesRealizados.mapNotNull { lembrete ->
            val data = dataRealizacaoLembrete(lembrete) ?: dataParaOrdenacao(lembrete)
            if (data == LocalDate.MAX) null else data to lembrete
        }
    )
        .distinctBy { (_, lembrete) -> lembrete.id }
        .sortedByDescending { it.first }
        .take(5)
    val manutencoesFuturas = lembretesAtivos
        .mapNotNull { lembrete ->
            val data = dataParaOrdenacao(lembrete)
            if (data == LocalDate.MAX) null else data to lembrete
        }
        .filter { (data, lembrete) ->
            !data.isBefore(LocalDate.now()) &&
                lembrete.tipo != TipoManutencao.IPVA &&
                lembrete.tipo != TipoManutencao.LICENCIAMENTO
        }
        .sortedBy { it.first }
        .take(10)

    val documentos = listOf(
        TipoManutencao.IPVA to "IPVA",
        TipoManutencao.LICENCIAMENTO to "Licenciamento",
        TipoManutencao.SEGURO to "Seguro"
    ).map { (tipo, label) ->
        val ultimaData = lembretesAtivos
            .filter { it.tipo == tipo }
            .map { dataParaOrdenacao(it) }
            .filter { it != LocalDate.MAX }
            .maxOrNull()
        val status = when {
            ultimaData == null -> "N/A"
            !ultimaData.isBefore(LocalDate.now()) -> "Em dia"
            else -> "Vencido"
        }
        val corStatus = if(status == "Vencido") Color(0xFFEF4444) else if(status == "Em dia") Color(0xFF10B981) else textDim
        Triple(label, status, corStatus)
    }

    val valorFipeNumerico = remember(precoTabelaFipe) {
        precoTabelaFipe?.let(::parseMoedaBrParaDouble)
    }
    val fatorVendaSugerido = remember(tituloSaudeOriginal, lembretes.size, carro.vezesBatido, carro.tempoComVeiculo) {
        val fatorSaude = when (tituloSaudeOriginal) {
            "Excelente" -> 0.98
            "Em atenção" -> 0.93
            "Crítica" -> 0.86
            else -> 0.94
        }
        val descontoAvisos = (lembretes.size * 0.012).coerceAtMost(0.10)
        val fatorBase = max(0.75, fatorSaude - descontoAvisos)
        val fatorBatidas = fatorPorBatidas(carro.vezesBatido)
        val fatorTempo = fatorPorTempoComVeiculo(carro.tempoComVeiculo)
        (fatorBase * fatorBatidas * fatorTempo).coerceIn(0.60, 1.08)
    }
    val valorVendaSugerido = valorFipeNumerico?.let { it * fatorVendaSugerido }

    LaunchedEffect(carro.id, carro.marca, carro.modelo, carro.tipoVeiculo) {
        carregandoPrecoFipe = true
        precoTabelaFipe = withContext(Dispatchers.IO) { buscarPrecoFipeVeiculo(context, carro) }
        carregandoPrecoFipe = false
    }

    if (showHistoricoConsumo) {
        HistoricoAbastecimentoScreen(
            carroId = carro.id,
            onDismiss = { showHistoricoConsumo = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = pageLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // --- TOP BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Relatório Técnico",
                    color = textLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Voltar",
                        tint = textLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val uri = gerarPdfRelatorio(
                            context = context,
                            carro = carro,
                            lembretes = lembretes,
                            isPremium = isPremium,
                            valorTabela = precoTabelaFipe,
                            valorParaVender = valorVendaSugerido?.let(::formatarMoedaLocal)
                        )
                        if (uri != null) {
                            compartilharPdf(context, uri)
                        } else {
                            Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = pdfContainer,
                        contentColor = pdfAccent
                    ),
                    border = BorderStroke(1.dp, cardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // --- HERO SECTION (Carro) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = carro.nome,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textLight,
                    modifier = Modifier.offset(y = (4).dp)
                )

                VehicleIcon(
                    tipoVeiculo = carro.tipoVeiculo,
                    tint = if (isDark) Color.White else Color.Black,
                    size = 210.dp,
                    modifier = Modifier.offset(y = (-22).dp)
                )

                Surface(
                    modifier = Modifier
                        .offset(y = (-64).dp)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    border = BorderStroke(0.8.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Marca • Modelo • Ano",
                            color = textDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = listOf(carro.marca, modeloSemAno, anoVeiculo.takeIf { it != "--" })
                                .filterNotNull()
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                                .ifBlank { "N/A" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (carregandoPrecoFipe) {
                    Text(
                        text = "Buscando FIPE...",
                        style = MaterialTheme.typography.bodySmall,
                        color = textDim,
                        modifier = Modifier.offset(y = (-54).dp)
                    )
                } else if (!precoTabelaFipe.isNullOrBlank()) {
                    ElevatedCard(
                        modifier = Modifier
                            .offset(y = (-54).dp)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.8.dp, cardBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRowModern("Tabela FIPE", precoTabelaFipe.orEmpty(), textDim, Color(0xFF22C55E))
                            if (valorVendaSugerido != null) {
                                Divider(color = dividerColor)
                                InfoRowModern("Por quanto vender", formatarMoedaLocal(valorVendaSugerido), textDim, Color(0xFF22C55E))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(0.dp))
            }

            if (!isBikeType) {
                FilledTonalButton(
                    onClick = { showHistoricoConsumo = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .offset(y = (-46).dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver consumo", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(0.dp))

            // --- RESUMO RÁPIDO ---
            ContentSection(
                modifier = Modifier
                    .offset(y = (-34).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = "Resumo",
                icon = Icons.Outlined.Info,
                cardColor = cardColor,
                titleColor = textLight,
                borderColor = cardBorder
            ) {
                InfoRowModern("Saúde", tituloSaude, textDim, corSaude)
                Divider(color = dividerColor)
                InfoRowModern("Total gasto", formatarMoedaLocal(totalGastos), textDim, textLight)
                if (!isBikeType) {
                    Divider(color = dividerColor)
                    InfoRowModern("KM atual", kmAtualResumo, textDim, accentColor)
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- CONTEÚDO DETALHADO ---
            Column(
                modifier = Modifier
                    .offset(y = (-34).dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Seção Técnica
                ContentSection(title = "Ficha Técnica", icon = Icons.Outlined.Build, cardColor = cardColor, titleColor = textLight, borderColor = cardBorder) {
                    InfoRowModern("Cor", corNome, textDim, textLight)
                    Divider(color = dividerColor)
                    InfoRowModern("Modelo", modeloSemAno, textDim, textLight)
                    if (!isBikeType) {
                        Divider(color = dividerColor)
                        InfoRowModern("Ano", anoVeiculo, textDim, textLight)
                    }
                    Divider(color = dividerColor)
                    InfoRowModern("Código ID", codigoCurto(carro.id), textDim, textLight)
                    Divider(color = dividerColor)
                    InfoRowModern("Próx. Serviço", proximo, textDim, if(proximo == "--") textDim else accentColor)
                    Divider(color = dividerColor)
                    InfoRowModern("Mantenedor", carro.proprietario.ifBlank { "--" }, textDim, textLight)
                }

                // Seção Documentos (apenas para veículos com documentação)
                if (carro.tipoVeiculo != TipoVeiculo.BICICLETA) {
                    ContentSection(
                        title = "Situação Legal",
                        icon = Icons.Outlined.Description,
                        cardColor = cardColor,
                        titleColor = textLight,
                        borderColor = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            documentos.forEachIndexed { index, (label, status, color) ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(1.dp)
                                            .background(dividerColor.copy(alpha = 0.9f))
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = textDim)
                                    Text(status, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }
                }

                // Histórico Recente
                ContentSection(
                    title = "Manutencoes Realizadas",
                    icon = Icons.Outlined.History,
                    cardColor = cardColor,
                    titleColor = textLight,
                    borderColor = cardBorder
                ) {
                    if (historicoManutencoes.isEmpty()) {
                        Text("Nenhum registro encontrado.", color = textDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, dividerColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Item", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("Data", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(92.dp))
                                    Text("Valor", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(82.dp), textAlign = TextAlign.End)
                                }
                                historicoManutencoes.forEachIndexed { index, (data, lembrete) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            lembrete.titulo,
                                            color = textLight,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                            color = textDim,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(92.dp)
                                        )
                                        Text(
                                            formatarMoedaLocal(lembrete.valor),
                                            color = textLight.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.width(82.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    if (index < historicoManutencoes.lastIndex) Divider(color = dividerColor)
                                }
                            }
                        }
                    }
                }

                ContentSection(
                    title = "Manutencoes Futuras",
                    icon = Icons.Default.Event,
                    cardColor = cardColor,
                    titleColor = textLight,
                    borderColor = cardBorder
                ) {
                    if (manutencoesFuturas.isEmpty()) {
                        Text("Nenhum lembrete futuro.", color = textDim, fontSize = 12.sp)
                    } else {
                        val colData = 110.dp
                        val colKm = 90.dp
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, dividerColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Item", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("Data", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(colData), textAlign = TextAlign.Center)
                                    Text("KM", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(colKm), textAlign = TextAlign.End)
                                }
                                manutencoesFuturas.forEachIndexed { index, (data, lembrete) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            lembrete.titulo,
                                            color = textLight,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                            color = textDim,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(colData),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            lembrete.kmLimite.ifBlank { "--" },
                                            color = textLight,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(colKm),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    if (index < manutencoesFuturas.lastIndex) Divider(color = dividerColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String? = null,
    valueColor: Color,
    icon: ImageVector,
    cardColor: Color,
    dimColor: Color,
    borderColor: Color
) {
    ElevatedCard(
        modifier = modifier
            .height(100.dp)
            .border(0.8.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = dimColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
                if (!subValue.isNullOrBlank()) {
                    Text(subValue, style = MaterialTheme.typography.labelSmall, color = dimColor, maxLines = 1)
                }
                Text(title, style = MaterialTheme.typography.labelSmall, color = dimColor)
            }
        }
    }
}

@Composable
fun ContentSection(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    cardColor: Color,
    titleColor: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(0.8.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = titleColor.copy(alpha = 0.82f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = titleColor.copy(alpha = 0.82f))
            }
            content()
        }
    }
}

@Composable
fun InfoRowModern(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = labelColor,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.58f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PriceInfoPill(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// --- FUNÇÕES UTILITÁRIAS ---

private fun fatorPorBatidas(vezesBatido: Int?): Double {
    val totalBatidas = (vezesBatido ?: 0).coerceAtLeast(0)
    return when {
        totalBatidas <= 0 -> 1.00
        totalBatidas == 1 -> 0.97
        totalBatidas == 2 -> 0.94
        totalBatidas == 3 -> 0.90
        else -> 0.85
    }
}

private fun fatorPorTempoComVeiculo(tempoComVeiculo: String): Double {
    val tempoNormalizado = tempoComVeiculo.trim().lowercase(Locale("pt", "BR"))
    return when {
        tempoNormalizado.startsWith("menos de 6 meses") -> 0.97
        tempoNormalizado.startsWith("6 meses a 1 ano") -> 0.98
        tempoNormalizado.startsWith("1 a 2 anos") -> 1.00
        tempoNormalizado.startsWith("2 a 3 anos") -> 1.02
        tempoNormalizado.startsWith("3 a 5 anos") -> 1.04
        tempoNormalizado.startsWith("mais de 5 anos") -> 1.05
        else -> 1.00
    }
}

private fun corNomePorArgb(argb: Int): String {
    val cores = listOf(
        "Branco" to Color(0xFFFFFFFF).toArgb(),
        "Preto" to Color(0xFF0F172A).toArgb(),
        "Prata" to Color(0xFFC0C0C0).toArgb(),
        "Cinza" to Color(0xFF9CA3AF).toArgb(),
        "Vermelho" to Color(0xFFDC2626).toArgb(),
        "Azul" to Color(0xFF4F7DBE).toArgb(),
        "Marrom" to Color(0xFF7C3F00).toArgb(),
        "Bege" to Color(0xFFE7D7C1).toArgb(),
        "Verde" to Color(0xFF16A34A).toArgb(),
        "Amarelo" to Color(0xFFFACC15).toArgb(),
        "Laranja" to Color(0xFFF97316).toArgb(),
        "Roxo" to Color(0xFF6D5BD0).toArgb(),
        "Rosa" to Color(0xFFEC4899).toArgb(),
        "Dourado" to Color(0xFFC0841A).toArgb(),
        "Bordô" to Color(0xFF7F1D1D).toArgb(),
        "Turquesa" to Color(0xFF38BDF8).toArgb(),
        "Creme" to Color(0xFFF5F5DC).toArgb()
    )
    return cores.firstOrNull { it.second == argb }?.first ?: "Personalizada"
}

private fun codigoCurto(id: String): String {
    val numero = abs(id.hashCode()) % 10000
    return numero.toString().padStart(4, '0')
}

private fun extrairAnoVeiculo(modelo: String): String? {
    val match = Regex("(19|20)\\d{2}(?:/(19|20)\\d{2})?").find(modelo)
    return match?.value
}

private fun removerAnoDoModelo(modelo: String): String {
    return modelo
        .replace(Regex("(19|20)\\d{2}(?:/(19|20)\\d{2})?"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

private fun parseMoedaBrParaDouble(valor: String): Double? {
    return valor
        .replace("R$", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull()
}

private data class FipeMarcaRelatorioDto(
    val codigo: String,
    val nome: String
)

private data class FipeModeloRelatorioDto(
    val codigo: Int,
    val nome: String
)

private data class FipeAnoRelatorioDto(
    val codigo: String,
    val nome: String
)

private data class FipeModelosRelatorioResponseDto(
    val modelos: List<FipeModeloRelatorioDto> = emptyList()
)

private data class FipeValorRelatorioDto(
    @SerializedName("Valor") val valor: String? = null
)

private interface FipeRelatorioApi {
    @GET("api/v1/{tipo}/marcas")
    suspend fun listarMarcas(@Path("tipo") tipo: String): List<FipeMarcaRelatorioDto>

    @GET("api/v1/{tipo}/marcas/{codigoMarca}/modelos")
    suspend fun listarModelos(
        @Path("tipo") tipo: String,
        @Path("codigoMarca") codigoMarca: String
    ): FipeModelosRelatorioResponseDto

    @GET("api/v1/{tipo}/marcas/{codigoMarca}/modelos/{codigoModelo}/anos")
    suspend fun listarAnos(
        @Path("tipo") tipo: String,
        @Path("codigoMarca") codigoMarca: String,
        @Path("codigoModelo") codigoModelo: Int
    ): List<FipeAnoRelatorioDto>

    @GET("api/v1/{tipo}/marcas/{codigoMarca}/modelos/{codigoModelo}/anos/{codigoAno}")
    suspend fun consultarValor(
        @Path("tipo") tipo: String,
        @Path("codigoMarca") codigoMarca: String,
        @Path("codigoModelo") codigoModelo: Int,
        @Path("codigoAno") codigoAno: String
    ): FipeValorRelatorioDto
}

private val fipeRelatorioApi: FipeRelatorioApi by lazy {
    Retrofit.Builder()
        .baseUrl(BuildConfig.FIPE_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FipeRelatorioApi::class.java)
}

private const val FIPE_CACHE_PRECO_TTL_MS = 24L * 60L * 60L * 1000L

private suspend fun buscarPrecoFipeVeiculo(context: android.content.Context, carro: CarroInfo): String? {
    val tipoFipe = tipoFipeParaRelatorio(carro.tipoVeiculo) ?: return null
    val marca = carro.marca.trim()
    val modelo = removerAnoDoModelo(carro.modelo).ifBlank { carro.modelo }.trim()
    if (marca.isBlank() || modelo.isBlank()) return null
    val anoDesejado = Regex("(19|20)\\d{2}").find(carro.modelo)?.value.orEmpty()
    val cacheKey = "preco_${gerarCacheKeyRelatorio(tipoFipe, marca, modelo, anoDesejado)}"

    AppPreferences.getFipeCache(context, cacheKey, FIPE_CACHE_PRECO_TTL_MS)?.let { cached ->
        if (cached.isNotBlank()) return cached
    }

    val valor = runCatching {
        withFipeRetryRelatorio {
            val marcas = fipeRelatorioApi.listarMarcas(tipoFipe)
            val codigoMarca = encontrarCodigoMarcaRelatorio(marca, marcas) ?: return@withFipeRetryRelatorio null

            val modelos = fipeRelatorioApi.listarModelos(tipoFipe, codigoMarca).modelos
            val modeloNorm = normalizarTextoRelatorio(modelo)
            val modeloMatch = modelos.firstOrNull {
                val atual = normalizarTextoRelatorio(it.nome)
                atual == modeloNorm || atual.contains(modeloNorm) || modeloNorm.contains(atual)
            } ?: return@withFipeRetryRelatorio null

            val anos = fipeRelatorioApi.listarAnos(tipoFipe, codigoMarca, modeloMatch.codigo)
            if (anos.isEmpty()) return@withFipeRetryRelatorio null

            val anoMatch = if (anoDesejado.isNotBlank()) {
                anos.firstOrNull { it.nome.contains(anoDesejado) || it.codigo.startsWith(anoDesejado) }
            } else null
            val anoEscolhido = anoMatch ?: anos.first()

            fipeRelatorioApi.consultarValor(tipoFipe, codigoMarca, modeloMatch.codigo, anoEscolhido.codigo).valor
        }
    }.getOrNull()

    if (!valor.isNullOrBlank()) {
        AppPreferences.putFipeCache(context, cacheKey, valor)
    }
    return valor
}

private fun tipoFipeParaRelatorio(tipo: TipoVeiculo): String? = when (tipo) {
    TipoVeiculo.MOTO -> "motos"
    TipoVeiculo.CAMINHAO, TipoVeiculo.ONIBUS -> "caminhoes"
    TipoVeiculo.CARRO,
    TipoVeiculo.HATCH,
    TipoVeiculo.SUV,
    TipoVeiculo.CAMINHONETE,
    TipoVeiculo.FURGAO,
    TipoVeiculo.VAN,
    TipoVeiculo.VEICULO_ELETRICO -> "carros"
    else -> null
}

private fun encontrarCodigoMarcaRelatorio(
    marcaSelecionada: String,
    marcasFipe: List<FipeMarcaRelatorioDto>
): String? {
    val alvo = normalizarTextoRelatorio(marcaSelecionada)
    if (alvo.isBlank()) return null
    marcasFipe.firstOrNull { normalizarTextoRelatorio(it.nome) == alvo }?.let { return it.codigo }
    marcasFipe.firstOrNull {
        val atual = normalizarTextoRelatorio(it.nome)
        atual.contains(alvo) || alvo.contains(atual)
    }?.let { return it.codigo }
    return null
}

private fun normalizarTextoRelatorio(texto: String): String =
    java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^A-Za-z0-9 ]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .uppercase(Locale.ROOT)
        .trim()

private suspend fun <T> withFipeRetryRelatorio(block: suspend () -> T): T {
    var lastError: Throwable? = null
    val delays = listOf(0L, 350L, 900L)
    for (waitMs in delays) {
        try {
            if (waitMs > 0) delay(waitMs)
            return block()
        } catch (e: Throwable) {
            lastError = e
        }
    }
    throw (lastError ?: IllegalStateException("Erro desconhecido em consulta FIPE (relatorio)"))
}

private fun gerarCacheKeyRelatorio(vararg partes: String): String {
    val raw = partes.joinToString("|") { normalizarTextoRelatorio(it) }
    return raw.hashCode().toUInt().toString()
}
