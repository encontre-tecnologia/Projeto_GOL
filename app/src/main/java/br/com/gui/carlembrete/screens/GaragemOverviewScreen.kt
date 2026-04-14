package br.com.gui.carlembrete

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaragemOverviewScreen(
    carros: List<CarroInfo>,
    onSelecionar: (CarroInfo) -> Unit,
    onDismiss: () -> Unit,
    title: String = tr("Meus veículos", "My vehicles"),
    showVehicleHealthSection: Boolean = true,
    onOpenReminderDetails: (Lembrete) -> Unit = {}
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (isDark) Color.Black else scheme.background
    val accentBlue = Color(0xFF3B82F6)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isDark) Color(0xFF111827) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val isEnglish = isEnglishUi()
    val allFilterLabel = if (isEnglish) "ALL" else "TODOS"
    val healthyLabel = if (isEnglish) "Healthy" else "Em dia"
    val attentionLabel = if (isEnglish) "Attention" else "Atenção"
    val criticalLabel = if (isEnglish) "Critical" else "Crítica"
    val noHistoryLabel = if (isEnglish) "No history" else "Sem histórico"
    val notInformedLabel = if (isEnglish) "Not informed" else "Não informado"
    val searchFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8),
        unfocusedBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1),
        focusedContainerColor = cardBg,
        unfocusedContainerColor = cardBg,
        cursorColor = textPrimary,
        focusedLeadingIconColor = textDim,
        unfocusedLeadingIconColor = textDim,
        focusedPlaceholderColor = textDim,
        unfocusedPlaceholderColor = textDim
    )

    var busca by remember { mutableStateOf("") }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var filtroSaude by remember { mutableStateOf(allFilterLabel) }
    var lembretesVencidosDialogCarro by remember { mutableStateOf<CarroInfo?>(null) }
    val garageHeaderImage = remember(context) {
        runCatching {
            context.assets.open("GarageZellu.png").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val buscaNormalizada = busca.trim().lowercase(Locale.getDefault())
    val usuarioNome = FirebaseAuth.getInstance().currentUser?.displayName
    val nomeMantedor = usuarioNome?.trim()?.ifBlank { null }
        ?: FirebaseAuth.getInstance().currentUser?.email
        ?: notInformedLabel

    val carrosComBusca = if (buscaNormalizada.isBlank()) {
        carros
    } else {
        carros.filter { carro ->
            val alvo = listOf(carro.nome, carro.marca, carro.modelo)
                .joinToString(" ")
                .lowercase(Locale.getDefault())
            alvo.contains(buscaNormalizada)
        }
    }

    LaunchedEffect(carros, context) {
        todosLembretes = withContext(Dispatchers.IO) {
            BancoDeDados.carregarLembretes(context.applicationContext)
        }
    }

    val saudePorCarro = remember(carros, todosLembretes, textDim, isEnglish) {
        carros.associate { carro ->
            val lembretesCarro = todosLembretes.filter {
                it.carroId == carro.id && it.tipo != TipoManutencao.ABASTECIMENTO
            }
            val (tituloReputacao, descricaoReputacao) = calcularReputacao(lembretesCarro)
            val (tituloSaude, corSaude) = when {
                tituloReputacao.contains("cr", ignoreCase = true) -> criticalLabel to Color(0xFFEF4444)
                tituloReputacao.contains("aten", ignoreCase = true) -> attentionLabel to Color(0xFFEAB308)
                tituloReputacao.contains("excel", ignoreCase = true) -> healthyLabel to Color(0xFF10B981)
                else -> noHistoryLabel to textDim
            }
            carro.id to Triple(tituloSaude, corSaude, descricaoReputacao)
        }
    }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val hoje = remember { LocalDate.now() }
    val vencidosPorCarro = remember(todosLembretes, formatter, hoje) {
        todosLembretes
            .filterNot(::isLembreteRealizado)
            .filter { it.tipo != TipoManutencao.ABASTECIMENTO }
            .mapNotNull { lembrete ->
                val data = runCatching { LocalDate.parse(lembrete.dataLimite, formatter) }.getOrNull()
                    ?: return@mapNotNull null
                if (data.isBefore(hoje)) lembrete to data else null
            }
            .groupBy(
                keySelector = { it.first.carroId },
                valueTransform = { it.first }
            )
    }
    val carrosFiltrados = remember(carrosComBusca, saudePorCarro, filtroSaude, showVehicleHealthSection) {
        if (!showVehicleHealthSection || filtroSaude == allFilterLabel) {
            carrosComBusca
        } else {
            carrosComBusca.filter { carro ->
                saudePorCarro[carro.id]?.first == filtroSaude
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(bg)) {
            if (carros.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 8.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = tr("Voltar", "Back"),
                                tint = textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(tr("Nenhum veículo cadastrado", "No registered vehicles"), color = textDim)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(56.dp)
                                    .align(Alignment.CenterStart)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = tr("Voltar", "Back"),
                                    tint = textPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            garageHeaderImage?.let { image ->
                                Image(
                                    bitmap = image,
                                    contentDescription = tr("Garage Zellu", "Garage Zellu"),
                                    modifier = Modifier.height(104.dp).clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Text(
                                title,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = busca,
                            onValueChange = { busca = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = textDim
                                )
                            },
                            placeholder = { Text(tr("Buscar veículo", "Search vehicle"), color = textDim) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = searchFieldColors,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    if (showVehicleHealthSection) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val filtros = listOf(
                                        allFilterLabel,
                                        healthyLabel,
                                        attentionLabel,
                                        criticalLabel,
                                        noHistoryLabel
                                    )
                                    filtros.forEach { itemFiltro ->
                                        val selecionado = filtroSaude == itemFiltro
                                        val corFiltro = when (itemFiltro) {
                                            healthyLabel -> Color(0xFF10B981)
                                            attentionLabel -> Color(0xFFEAB308)
                                            criticalLabel -> Color(0xFFEF4444)
                                            noHistoryLabel -> textDim
                                            else -> accentBlue
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selecionado) corFiltro.copy(alpha = if (isDark) 0.24f else 0.14f) else Color.Transparent,
                                            border = BorderStroke(1.dp, corFiltro.copy(alpha = if (selecionado) 0.9f else 0.45f)),
                                            modifier = Modifier.clickable { filtroSaude = itemFiltro }
                                        ) {
                                            Text(
                                                text = itemFiltro,
                                                color = if (selecionado) corFiltro else textPrimary,
                                                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (carrosFiltrados.isEmpty()) {
                        item {
                            val mensagemSemResultados = if (showVehicleHealthSection && filtroSaude != allFilterLabel) {
                                if (isEnglish) {
                                    "There is no vehicle for category \"$filtroSaude\"."
                                } else {
                                    "Não há nenhum veículo para a categoria \"$filtroSaude\"."
                                }
                            } else {
                                tr("Nenhum veículo encontrado", "No vehicle found")
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = textDim,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(mensagemSemResultados, color = textDim)
                            }
                        }
                    } else {
                        items(carrosFiltrados) { carro ->
                            val corBaseVeiculo = carro.getCorUI()
                            val corCirculoVeiculo = when {
                                // Evita "sumir" no tema escuro quando a cor do veículo é preta/muito escura.
                                isDark && corBaseVeiculo.luminance() < 0.12f -> Color(0xFF334155)
                                // Em tema claro, evita bolha extremamente escura com pouco contraste.
                                !isDark && corBaseVeiculo.luminance() < 0.08f -> Color(0xFFE2E8F0)
                                else -> corBaseVeiculo
                            }
                            val tintIconeVeiculo = if (corCirculoVeiculo.luminance() < 0.45f) {
                                Color.White
                            } else {
                                Color(0xFF0F172A)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelecionar(carro) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(corCirculoVeiculo.copy(alpha = if (isDark) 0.60f else 0.24f))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            VehicleIcon(
                                                tipoVeiculo = carro.tipoVeiculo,
                                                tint = tintIconeVeiculo,
                                                size = 28.dp
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                carro.nome.ifBlank { tr("Veículo sem nome", "Unnamed vehicle") },
                                                color = textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            val isBikeType =
                                                carro.tipoVeiculo == TipoVeiculo.BICICLETA ||
                                                    carro.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
                                            val detalheModelo = if (isBikeType) {
                                                extrairAroDoModeloNoCard(carro.modelo)
                                                    .takeIf { it.isNotBlank() && it != "--" }
                                                    ?.let { "${tr("Aro", "Rim")}: $it" }
                                            } else {
                                                extrairAnoDoModeloNoCard(carro.modelo)
                                                    .takeIf { it.isNotBlank() && it != "--" }
                                            }
                                            val marcaAno = listOf(
                                                detalheModelo,
                                                carro.marca.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ").ifBlank { tr("Marca não informada", "Brand not informed") }
                                            Text(
                                                text = marcaAno,
                                                color = textDim,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${tr("Proprietário", "Owner")}: ${carro.proprietario.ifBlank { tr("Não informado", "Not informed") }}",
                                                color = if (isDark) Color(0xFFCBD5E1) else textDim,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${tr("Mantenedor", "Maintainer")}: $nomeMantedor",
                                                color = textDim,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (showVehicleHealthSection) {
                                        val saudeInfo = saudePorCarro[carro.id]
                                        val saudeTitulo = saudeInfo?.first ?: tr("Sem histórico", "No history")
                                        val saudeCor = saudeInfo?.second ?: textDim
                                        val saudeDescricao = saudeInfo?.third ?: tr("Cadastre serviços para avaliar.", "Register services to evaluate.")
                                        val podeAbrirVencidosDialog = saudeTitulo == attentionLabel || saudeTitulo == criticalLabel

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (podeAbrirVencidosDialog) {
                                                        Modifier.clickable { lembretesVencidosDialogCarro = carro }
                                                    } else {
                                                        Modifier
                                                    }
                                                ),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, saudeCor.copy(alpha = 0.35f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    tr("Saúde do veículo", "Vehicle health"),
                                                    color = textDim,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    saudeTitulo,
                                                    color = saudeCor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    saudeDescricao,
                                                    color = textDim,
                                                    fontSize = 12.sp,
                                                    lineHeight = 15.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Clip
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
        }
    }

    lembretesVencidosDialogCarro?.let { carroSelecionado ->
        val vencidosDoCarro = vencidosPorCarro[carroSelecionado.id].orEmpty()
            .sortedBy { lembrete -> runCatching { LocalDate.parse(lembrete.dataLimite, formatter) }.getOrNull() }
        AlertDialog(
            onDismissRequest = { lembretesVencidosDialogCarro = null },
            title = {
                Text(
                    text = tr("Avisos vencidos", "Overdue reminders"),
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = carroSelecionado.nome.ifBlank { tr("Veículo sem nome", "Unnamed vehicle") },
                        color = textDim,
                        fontSize = 12.sp
                    )
                    if (vencidosDoCarro.isEmpty()) {
                        Text(
                            text = tr("Nenhum aviso vencido para este veículo.", "No overdue reminders for this vehicle."),
                            color = textDim
                        )
                    } else {
                        vencidosDoCarro.forEach { lembrete ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        lembretesVencidosDialogCarro = null
                                        onOpenReminderDetails(lembrete)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = lembrete.titulo.ifBlank { tr("Aviso sem título", "Untitled reminder") },
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${tr("Data", "Date")}: ${lembrete.dataLimite}",
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { lembretesVencidosDialogCarro = null }) {
                    Text(tr("Fechar", "Close"))
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
fun VisaoGeralFrotaScreen(
    carros: List<CarroInfo>,
    onSelecionar: (CarroInfo) -> Unit,
    onDismiss: () -> Unit,
    onOpenReminderDetails: (Lembrete) -> Unit = {}
) {
    GaragemOverviewScreen(
        carros = carros,
        onSelecionar = onSelecionar,
        onDismiss = onDismiss,
        title = tr("Visão geral frota", "Fleet overview"),
        showVehicleHealthSection = true,
        onOpenReminderDetails = onOpenReminderDetails
    )
}

private fun extrairAnoDoModeloNoCard(modelo: String): String {
    val match = Regex("\\b(19|20)\\d{2}\\b").find(modelo)
    return match?.value ?: "--"
}

private fun extrairAroDoModeloNoCard(modelo: String): String {
    val texto = modelo.trim()
    if (texto.isBlank()) return "--"

    val comPrefixo = Regex("(?i)\\baro\\s*[:\\-]?\\s*(\\d{1,2})\\b").find(texto)?.groupValues?.getOrNull(1)
    if (!comPrefixo.isNullOrBlank()) return comPrefixo

    val numeroSolto = Regex("\\b(\\d{1,2})\\b").find(texto)?.groupValues?.getOrNull(1)
    return numeroSolto ?: texto
}


