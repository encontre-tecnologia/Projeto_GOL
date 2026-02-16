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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

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
    val cardBorder = if (isDark) Color(0xFF475569) else Color.Black.copy(alpha = 0.85f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
    val pdfAccent = if (isDark) Color.White else Color.Black
    val pdfContainer = Color.Transparent

    val totalGastos = lembretes.sumOf { it.valor }
    val context = LocalContext.current
    val view = LocalView.current
    var showHistoricoConsumo by remember { mutableStateOf(false) }

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
    val proximo = lembretes.minByOrNull { dataParaOrdenacao(it) }?.let {
        val data = dataParaOrdenacao(it)
        if (data == LocalDate.MAX) null else data.format(DateTimeFormatter.ofPattern("dd/MM"))
    } ?: "--"
    val anoVeiculo = extrairAnoVeiculo(carro.modelo) ?: "--"
    val modeloSemAno = removerAnoDoModelo(carro.modelo).ifBlank { carro.modelo.ifBlank { "--" } }

    val corNome = corNomePorArgb(carro.corArgb)
    val (tituloSaude, descricaoSaude) = calcularReputacao(lembretes)

    val corSaude = when (tituloSaude) {
        "Excelente" -> Color(0xFF10B981)
        "Crítica" -> Color(0xFFEF4444)
        "Em atenção" -> Color(0xFFEAB308)
        else -> textLight
    }

    val historicoManutencoes = lembretes
        .mapNotNull { lembrete ->
            val data = dataParaOrdenacao(lembrete)
            if (data == LocalDate.MAX) null else data to lembrete
        }
        .filter { (data, _) -> data.isBefore(LocalDate.now()) }
        .sortedByDescending { it.first }
        .take(5)

    val documentos = listOf(
        TipoManutencao.IPVA to "IPVA",
        TipoManutencao.LICENCIAMENTO to "Licenc."
    ).map { (tipo, label) ->
        val ultimaData = lembretes
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

    // Processamento de peças
    val pecaLabels = linkedMapOf<String, String>()
    val tipoPorPeca = mutableMapOf<String, TipoManutencao>()
    lembretes.forEach { lembrete ->
        val raw = lembrete.peca.ifBlank { lembrete.titulo }.trim()
        if (raw.isNotBlank()) {
            val key = raw.lowercase(Locale.getDefault())
            pecaLabels.putIfAbsent(key, raw)
            tipoPorPeca.putIfAbsent(key, lembrete.tipo)
        }
    }
    val trocasPorPeca = lembretes
        .map { lembrete -> lembrete.peca.ifBlank { lembrete.titulo }.trim() }
        .filter { it.isNotBlank() }
        .groupingBy { it.lowercase(Locale.getDefault()) }
        .eachCount()
        .map { (key, count) ->
            val label = pecaLabels[key] ?: key
            val tipo = tipoPorPeca[key] ?: TipoManutencao.OUTROS
            PecaResumo(label, count, tipo)
        }
        .sortedByDescending { it.count }
        .take(5)

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Voltar",
                            tint = textLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Relatório Técnico",
                        color = textLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val uri = gerarPdfRelatorio(context, carro, lembretes, isPremium)
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PDF", fontWeight = FontWeight.Bold)
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
                    color = textLight
                )

                Text(
                    text = listOf(carro.marca, modeloSemAno, anoVeiculo.takeIf { it != "--" })
                        .filterNotNull()
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { "N/A" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textDim
                )

                Spacer(Modifier.height(24.dp))
            }

            FilledTonalButton(
                onClick = { showHistoricoConsumo = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = pdfContainer,
                    contentColor = pdfAccent
                ),
                border = BorderStroke(1.dp, cardBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver consumo", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // --- DASHBOARD STATS (Grid Rápido) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Saúde
                DashboardCard(
                    modifier = Modifier.weight(1f),
                    title = "Saúde",
                    value = tituloSaude,
                    valueColor = corSaude,
                    icon = Icons.Outlined.Info,
                    cardColor = cardColor,
                    dimColor = textDim,
                    borderColor = cardBorder
                )
                // Card 2: Gasto
                DashboardCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Gasto",
                    value = formatarMoedaLocal(totalGastos),
                    valueColor = textLight,
                    icon = Icons.Outlined.Description,
                    cardColor = cardColor,
                    dimColor = textDim,
                    borderColor = cardBorder
                )
                // Card 3: KM
                DashboardCard(
                    modifier = Modifier.weight(1f),
                    title = "Km Atual",
                    value = if(carro.kmAtual > 0) "${carro.kmAtual / 1000}k" else "--",
                    subValue = "Ano: $anoVeiculo",
                    valueColor = accentColor,
                    icon = Icons.Default.Speed,
                    cardColor = cardColor,
                    dimColor = textDim,
                    borderColor = cardBorder
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- CONTEÚDO DETALHADO ---
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Seção Técnica
                ContentSection(title = "Ficha Técnica", icon = Icons.Outlined.Build, cardColor = cardColor, titleColor = textLight, borderColor = cardBorder) {
                    InfoRowModern("Cor", corNome, textDim, textLight)
                    Divider(color = dividerColor)
                    InfoRowModern("Modelo", modeloSemAno, textDim, textLight)
                    Divider(color = dividerColor)
                    InfoRowModern("Ano", anoVeiculo, textDim, textLight)
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            documentos.forEach { (label, status, color) ->
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
                    title = "Últimas Manutenções",
                    icon = Icons.Outlined.History,
                    cardColor = cardColor,
                    titleColor = textLight,
                    borderColor = cardBorder
                ) {
                    if (historicoManutencoes.isEmpty()) {
                        Text("Nenhum registro encontrado.", color = textDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        historicoManutencoes.forEachIndexed { index, (data, lembrete) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lembrete.titulo, color = textLight, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
                                    Text(data.format(DateTimeFormatter.ofPattern("dd 'de' MMM, yyyy")), color = textDim, fontSize = 11.sp)
                                }
                                Text(
                                    formatarMoedaLocal(lembrete.valor),
                                    color = textLight.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (index < historicoManutencoes.lastIndex) {
                                Divider(color = dividerColor)
                            }
                        }
                    }
                }

                // Peças Trocadas
                ContentSection(title = "Peças Recorrentes", icon = Icons.Default.Settings, cardColor = cardColor, titleColor = textLight, borderColor = cardBorder) {
                    if (trocasPorPeca.isEmpty()) {
                        Text("Sem dados de peças.", color = textDim, fontSize = 12.sp)
                    } else {
                        OptIn(ExperimentalLayoutApi::class)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            trocasPorPeca.forEach { (label, count, _) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, color = textLight, fontSize = 12.sp)
                                    Text("x$count", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
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
    title: String,
    icon: ImageVector,
    cardColor: Color,
    titleColor: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = labelColor, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// --- FUNÇÕES UTILITÁRIAS ---

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

private data class PecaResumo(
    val label: String,
    val count: Int,
    val tipo: TipoManutencao
)

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
