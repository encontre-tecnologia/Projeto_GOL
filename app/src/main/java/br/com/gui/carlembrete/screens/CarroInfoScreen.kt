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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
    autoScrollToCompletedMaintenance: Boolean = false,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val pageLight = if (isDark) Color.Black else scheme.background
    val cardColor = if (isDark) Color(0xFF111827) else Color.White
    val textLight = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentColor = Color(0xFF38BDF8)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)

    val lembretesSemAbastecimento = lembretes.filter { it.tipo != TipoManutencao.ABASTECIMENTO }
    val lembretesAtivos = lembretesSemAbastecimento.filterNot(::isLembreteRealizado)
    val lembretesRealizados = lembretesSemAbastecimento.filter(::isLembreteRealizado)
    val totalGastos = lembretes.sumOf { it.valor }
    val hoje = LocalDate.now()
    val anoReferencia = hoje.year
    val mesReferencia = hoje.format(DateTimeFormatter.ofPattern("MM/yyyy"))
    val totalGastosAno = lembretesSemAbastecimento.sumOf { lembrete ->
        val data = dataRealizacaoLembrete(lembrete) ?: dataParaOrdenacao(lembrete)
        if (data != LocalDate.MAX && data.year == anoReferencia) lembrete.valor else 0.0
    }
    val totalGastosMes = lembretesSemAbastecimento.sumOf { lembrete ->
        val data = dataRealizacaoLembrete(lembrete) ?: dataParaOrdenacao(lembrete)
        if (data != LocalDate.MAX && data.year == anoReferencia && data.monthValue == hoje.monthValue) lembrete.valor else 0.0
    }
    val context = LocalContext.current
    val view = LocalView.current
    var showHistoricoConsumo by remember { mutableStateOf(false) }
    val isBikeType = carro.tipoVeiculo == TipoVeiculo.BICICLETA || carro.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
    val exibirKmNoPainel = !isBikeType || !carro.semControleKm
    val suportaFipe = remember(carro.tipoVeiculo) { tipoFipeParaRelatorio(carro.tipoVeiculo) != null }

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
    val (tituloSaudeOriginal, descricaoSaudeOriginal) = calcularReputacao(lembretesSemAbastecimento)
    val avisosVencidos = lembretesAtivos.count { lembrete ->
        val data = dataParaOrdenacao(lembrete)
        data != LocalDate.MAX && data.isBefore(hoje)
    }
    val avisosPendentes = lembretesAtivos.count { lembrete ->
        val data = dataParaOrdenacao(lembrete)
        data != LocalDate.MAX && !data.isBefore(hoje)
    }
    val saudeCritica = avisosVencidos > 0
    val tituloSaude = if (saudeCritica) tr("Crítica", "Critical") else tr("Em dia", "Healthy")
    val descricaoSaude = when {
        saudeCritica -> if (isEnglishUi()) "There are $avisosVencidos overdue reminder(s)." else "Existem $avisosVencidos aviso(s) vencido(s)."
        avisosPendentes > 0 -> if (isEnglishUi()) "There are $avisosPendentes active reminder(s) in follow-up." else "Há $avisosPendentes aviso(s) ativo(s) em acompanhamento."
        else -> descricaoSaudeOriginal
    }
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

    val historicoPageSize = 5
    var historicoPage by remember(historicoManutencoes.size) { mutableStateOf(0) }
    val historicoTotalPages = if (historicoManutencoes.isEmpty()) 1 else (historicoManutencoes.size + historicoPageSize - 1) / historicoPageSize
    val historicoPageAtual = historicoPage.coerceIn(0, historicoTotalPages - 1)
    val historicoPaginado = historicoManutencoes
        .drop(historicoPageAtual * historicoPageSize)
        .take(historicoPageSize)

    val futurasPageSize = 5
    var futurasPage by remember(manutencoesFuturas.size) { mutableStateOf(0) }
    val futurasTotalPages = if (manutencoesFuturas.isEmpty()) 1 else (manutencoesFuturas.size + futurasPageSize - 1) / futurasPageSize
    val futurasPageAtual = futurasPage.coerceIn(0, futurasTotalPages - 1)
    val futurasPaginado = manutencoesFuturas
        .drop(futurasPageAtual * futurasPageSize)
        .take(futurasPageSize)

    val documentos = listOf(
        TipoManutencao.IPVA to tr("IPVA", "IPVA"),
        TipoManutencao.LICENCIAMENTO to tr("Licenciamento", "Licensing"),
        TipoManutencao.SEGURO to tr("Seguro", "Insurance")
    ).map { (tipo, label) ->
        val ultimaData = lembretesAtivos
            .filter { it.tipo == tipo }
            .map { dataParaOrdenacao(it) }
            .filter { it != LocalDate.MAX }
            .maxOrNull()
        val status = when {
            ultimaData == null -> "N/A"
            !ultimaData.isBefore(LocalDate.now()) -> tr("Em dia", "Healthy")
            else -> tr("Vencido", "Overdue")
        }
        val corStatus = if(status == tr("Vencido", "Overdue")) Color(0xFFEF4444) else if(status == tr("Em dia", "Healthy")) Color(0xFF10B981) else textDim
        Triple(label, status, corStatus)
    }

    val valorFipeNumerico = remember(precoTabelaFipe) {
        precoTabelaFipe?.let(::parseMoedaBrParaDouble)
    }
    val fatorVendaSugerido = remember(tituloSaudeOriginal, lembretesSemAbastecimento.size, carro.vezesBatido, carro.tempoComVeiculo) {
        val fatorSaude = when (tituloSaudeOriginal) {
            "Excelente" -> 0.98
            "Em atenção" -> 0.93
            "Crítica" -> 0.86
            else -> 0.94
        }
        val descontoAvisos = (lembretesSemAbastecimento.size * 0.012).coerceAtMost(0.10)
        val fatorBase = max(0.75, fatorSaude - descontoAvisos)
        val fatorBatidas = fatorPorBatidas(carro.vezesBatido)
        val fatorTempo = fatorPorTempoComVeiculo(carro.tempoComVeiculo)
        (fatorBase * fatorBatidas * fatorTempo).coerceIn(0.60, 1.08)
    }
    val valorVendaSugerido = valorFipeNumerico?.let { it * fatorVendaSugerido }
    val contentScrollState = rememberScrollState()
    var completedMaintenanceSectionTop by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(carro.id, carro.marca, carro.modelo, carro.tipoVeiculo) {
        if (!suportaFipe) {
            precoTabelaFipe = null
            carregandoPrecoFipe = false
            return@LaunchedEffect
        }
        carregandoPrecoFipe = true
        precoTabelaFipe = withContext(Dispatchers.IO) { buscarPrecoFipeVeiculo(context, carro) }
        carregandoPrecoFipe = false
    }

    LaunchedEffect(autoScrollToCompletedMaintenance, completedMaintenanceSectionTop) {
        if (!autoScrollToCompletedMaintenance) return@LaunchedEffect
        if (completedMaintenanceSectionTop <= 0f) return@LaunchedEffect
        delay(180)
        val target = (completedMaintenanceSectionTop - 120f).coerceAtLeast(0f).toInt()
        contentScrollState.animateScrollTo(target)
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
                .verticalScroll(contentScrollState)
                .padding(bottom = 24.dp)
        ) {
            // --- TOP BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = tr("Voltar", "Back"),
                        tint = textLight,
                        modifier = Modifier.size(18.dp)
                    )
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
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VehicleIcon(
                        tipoVeiculo = carro.tipoVeiculo,
                        tint = if (isDark) Color.White else Color.Black,
                        size = 180.dp
                    )
                }
            }

            if (!isBikeType) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showHistoricoConsumo = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Ver consumo", "View consumption"), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val uri = gerarPdfRelatorio(
                                context = context,
                                carro = carro,
                                lembretes = lembretes,
                                isPremium = isPremium,
                                valorTabela = if (suportaFipe) precoTabelaFipe else null,
                                valorParaVender = if (suportaFipe) valorVendaSugerido?.let(::formatarMoedaLocal) else null
                            )
                            if (uri != null) {
                                compartilharPdf(context, uri)
                            } else {
                                Toast.makeText(context, trNow("Erro ao gerar PDF", "Failed to generate PDF"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFD1FAE5)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Compartilhar", "Share"), fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        val uri = gerarPdfRelatorio(
                            context = context,
                            carro = carro,
                            lembretes = lembretes,
                            isPremium = isPremium,
                            valorTabela = if (suportaFipe) precoTabelaFipe else null,
                            valorParaVender = if (suportaFipe) valorVendaSugerido?.let(::formatarMoedaLocal) else null
                        )
                        if (uri != null) {
                            compartilharPdf(context, uri)
                        } else {
                            Toast.makeText(context, trNow("Erro ao gerar PDF", "Failed to generate PDF"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFD1FAE5)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Compartilhar", "Share"), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(0.dp))
            Spacer(Modifier.height(12.dp))

            // --- RESUMO RÁPIDO ---
            ContentSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = tr("Resumo", "Summary"),
                icon = Icons.Outlined.Info,
                cardColor = cardColor,
                titleColor = textLight,
                borderColor = cardBorder
            ) {
                InfoRowModern(
                    tr("Marca/Modelo", "Brand/Model"),
                    listOf(carro.marca, modeloSemAno)
                        .filterNotNull()
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { "N/A" },
                    textDim,
                    textLight
                )
                Divider(color = dividerColor)
                InfoRowModern(tr("Saúde", "Health"), tituloSaude, textDim, corSaude)
                if (!isBikeType) {
                    Divider(color = dividerColor)
                    InfoRowModern(tr("Ano do veículo", "Vehicle year"), anoVeiculo, textDim, textLight)
                }
                Divider(color = dividerColor)
                InfoRowModern(tr("Total ano $anoReferencia", "Year total $anoReferencia"), formatarMoedaLocal(totalGastosAno), textDim, textLight)
                Divider(color = dividerColor)
                InfoRowModern(tr("Total mês $mesReferencia", "Month total $mesReferencia"), formatarMoedaLocal(totalGastosMes), textDim, textLight)
                if (suportaFipe) {
                    Divider(color = dividerColor)
                    when {
                        carregandoPrecoFipe -> {
                            InfoRowModern(tr("Tabela FIPE", "FIPE Table"), tr("Buscando...", "Loading..."), textDim, textDim)
                        }
                        !precoTabelaFipe.isNullOrBlank() -> {
                            InfoRowModern(tr("Tabela FIPE", "FIPE Table"), precoTabelaFipe.orEmpty(), textDim, Color(0xFF22C55E))
                        }
                        else -> {
                            InfoRowModern(tr("Tabela FIPE", "FIPE Table"), "--", textDim, textDim)
                        }
                    }
                    Divider(color = dividerColor)
                    InfoRowModern(
                        tr("Por quanto vender", "Suggested sale price"),
                        valorVendaSugerido?.let(::formatarMoedaLocal) ?: "--",
                        textDim,
                        if (valorVendaSugerido != null) Color(0xFF22C55E) else textDim
                    )
                }
                if (exibirKmNoPainel) {
                    Divider(color = dividerColor)
                    InfoRowModern(tr("KM atual", "Current mileage"), kmAtualResumo, textDim, accentColor)
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- CONTEÚDO DETALHADO ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Seção Técnica
                ContentSection(title = tr("Ficha Técnica", "Technical Sheet"), icon = Icons.Outlined.Build, cardColor = cardColor, titleColor = textLight, borderColor = cardBorder) {
                    InfoRowModern(tr("Cor", "Color"), corNome, textDim, textLight)
                    Divider(color = dividerColor)
                    InfoRowModern(tr("Modelo", "Model"), modeloSemAno, textDim, textLight)
                    if (!isBikeType) {
                        Divider(color = dividerColor)
                        InfoRowModern(tr("Ano", "Year"), anoVeiculo, textDim, textLight)
                    }
                    Divider(color = dividerColor)
                    InfoRowModern(tr("Código ID", "ID Code"), codigoCurto(carro.id), textDim, textLight)
                    Divider(color = dividerColor)
                    InfoRowModern(tr("Próx. Serviço", "Next service"), proximo, textDim, if(proximo == "--") textDim else accentColor)
                    Divider(color = dividerColor)
                    InfoRowModern(tr("Mantenedor", "Maintainer"), carro.proprietario.ifBlank { "--" }, textDim, textLight)
                }

                // Seção Documentos (apenas para veículos com documentação)
                if (!isBikeType) {
                    ContentSection(
                        title = tr("Situação Legal", "Legal Status"),
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
                    modifier = Modifier.onGloballyPositioned { coords ->
                        completedMaintenanceSectionTop = coords.positionInParent().y
                    },
                    title = tr("Registros cadastrados", "Saved records"),
                    icon = Icons.Outlined.History,
                    cardColor = cardColor,
                    titleColor = textLight,
                    borderColor = cardBorder,
                    contentPadding = PaddingValues(top = 0.dp, start = 0.dp, end = 0.dp, bottom = 0.dp)
                ) {
                    if (historicoManutencoes.isEmpty()) {
                        EmptyTableStateCard(
                            icon = Icons.Outlined.History,
                            title = tr("Sem registros cadastrados", "No saved records yet"),
                            message = tr(
                                "Quando você concluir um serviço, ele vai aparecer aqui com data e valor.",
                                "When you complete a service, it will appear here with date and amount."
                            ),
                            isDark = isDark,
                            textLight = textLight,
                            textDim = textDim,
                            borderColor = cardBorder
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Divider(color = dividerColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("Item", "Item"), color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(tr("Data", "Date"), color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(92.dp))
                                Text(tr("Valor", "Amount"), color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(82.dp), textAlign = TextAlign.End)
                            }
                            historicoPaginado.forEachIndexed { index, (data, lembrete) ->
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
                                if (index < historicoPaginado.lastIndex) Divider(color = dividerColor)
                            }
                            Divider(color = dividerColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { historicoPage = (historicoPageAtual - 1).coerceAtLeast(0) },
                                    enabled = historicoPageAtual > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowLeft,
                                        contentDescription = tr("Anterior", "Previous"),
                                        tint = if (historicoPageAtual > 0) textLight else textDim
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${tr("Página", "Page")} ${historicoPageAtual + 1}/$historicoTotalPages",
                                    color = textDim,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = { historicoPage = (historicoPageAtual + 1).coerceAtMost(historicoTotalPages - 1) },
                                    enabled = historicoPageAtual < historicoTotalPages - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = tr("Próxima", "Next"),
                                        tint = if (historicoPageAtual < historicoTotalPages - 1) textLight else textDim
                                    )
                                }
                            }
                        }
                    }
                }

                ContentSection(
                    title = tr("Avisos cadastrados", "Saved reminders"),
                    icon = Icons.Default.Event,
                    cardColor = cardColor,
                    titleColor = textLight,
                    borderColor = cardBorder,
                    contentPadding = PaddingValues(top = 0.dp, start = 0.dp, end = 0.dp, bottom = 0.dp)
                ) {
                    if (manutencoesFuturas.isEmpty()) {
                        EmptyTableStateCard(
                            icon = Icons.Default.Event,
                            title = tr("Sem lembretes futuros", "No upcoming reminders"),
                            message = tr(
                                "Adicione um novo aviso para não perder a próxima manutenção.",
                                "Add a new reminder so you don't miss the next maintenance."
                            ),
                            isDark = isDark,
                            textLight = textLight,
                            textDim = textDim,
                            borderColor = cardBorder
                        )
                    } else {
                        val colData = 110.dp
                        val colKm = 90.dp
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Divider(color = dividerColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("Item", "Item"), color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(tr("Data", "Date"), color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(colData), textAlign = TextAlign.Center)
                                Text(tr("KM", "Mileage"), color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(colKm), textAlign = TextAlign.End)
                            }
                            futurasPaginado.forEachIndexed { index, (data, lembrete) ->
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
                                if (index < futurasPaginado.lastIndex) Divider(color = dividerColor)
                            }
                            Divider(color = dividerColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { futurasPage = (futurasPageAtual - 1).coerceAtLeast(0) },
                                    enabled = futurasPageAtual > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowLeft,
                                        contentDescription = tr("Anterior", "Previous"),
                                        tint = if (futurasPageAtual > 0) textLight else textDim
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${tr("Página", "Page")} ${futurasPageAtual + 1}/$futurasTotalPages",
                                    color = textDim,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = { futurasPage = (futurasPageAtual + 1).coerceAtMost(futurasTotalPages - 1) },
                                    enabled = futurasPageAtual < futurasTotalPages - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = tr("Próxima", "Next"),
                                        tint = if (futurasPageAtual < futurasTotalPages - 1) textLight else textDim
                                    )
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
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val headerBg = if (cardColor.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE2E8F0)
    }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(0.8.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = titleColor.copy(alpha = 0.82f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor.copy(alpha = 0.82f)
                )
            }
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun EmptyTableStateCard(
    icon: ImageVector,
    title: String,
    message: String,
    isDark: Boolean,
    textLight: Color,
    textDim: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.65f else 1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textDim.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = textLight,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                color = textDim,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
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
        trNow("Branco", "White") to Color(0xFFFFFFFF).toArgb(),
        trNow("Preto", "Black") to Color(0xFF0F172A).toArgb(),
        trNow("Prata", "Silver") to Color(0xFFC0C0C0).toArgb(),
        trNow("Cinza", "Gray") to Color(0xFF9CA3AF).toArgb(),
        trNow("Vermelho", "Red") to Color(0xFFDC2626).toArgb(),
        trNow("Azul", "Blue") to Color(0xFF4F7DBE).toArgb(),
        trNow("Marrom", "Brown") to Color(0xFF7C3F00).toArgb(),
        trNow("Bege", "Beige") to Color(0xFFE7D7C1).toArgb(),
        trNow("Verde", "Green") to Color(0xFF16A34A).toArgb(),
        trNow("Amarelo", "Yellow") to Color(0xFFFACC15).toArgb(),
        trNow("Laranja", "Orange") to Color(0xFFF97316).toArgb(),
        trNow("Roxo", "Purple") to Color(0xFF6D5BD0).toArgb(),
        trNow("Rosa", "Pink") to Color(0xFFEC4899).toArgb(),
        trNow("Dourado", "Gold") to Color(0xFFC0841A).toArgb(),
        trNow("Bordô", "Burgundy") to Color(0xFF7F1D1D).toArgb(),
        trNow("Turquesa", "Turquoise") to Color(0xFF38BDF8).toArgb(),
        trNow("Creme", "Cream") to Color(0xFFF5F5DC).toArgb()
    )
    return cores.firstOrNull { it.second == argb }?.first ?: trNow("Personalizada", "Custom")
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

