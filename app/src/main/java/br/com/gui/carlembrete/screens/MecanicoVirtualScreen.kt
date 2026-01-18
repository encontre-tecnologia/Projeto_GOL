package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MecanicoVirtualScreen(
    carro: CarroInfo,
    lembretes: List<Lembrete>,
    onDismiss: () -> Unit
) {
    // Paleta de Cores Modernizada
    val primaryDark = Color(0xFF0F172A)     // Fundo principal
    val surfaceDark = Color(0xFF1E293B)     // Cards
    val textLight = Color(0xFFF1F5F9)       // Texto principal
    val textDim = Color(0xFF94A3B8)         // Texto secundario
    val accent = Color(0xFF3B82F6)          // Azul destaque
    val success = Color(0xFF10B981)         // Verde sucesso
    val warning = Color(0xFFF59E0B)         // Amarelo alerta
    val danger = Color(0xFFEF4444)          // Vermelho perigo

    val kmMesInput = remember { mutableStateOf("1200") }
    val kmMes = kmMesInput.value.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1200.0

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

    Scaffold(
        containerColor = primaryDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Mecanico Virtual",
                            color = textLight,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Inteligencia Preditiva",
                            color = accent,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textLight)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = primaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 1. CARD DE INPUT DE KM (Visual mais limpo)
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = accent)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ritmo de Uso", color = textDim, style = MaterialTheme.typography.labelMedium)
                        Text("KM Media / Mes", color = textLight, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }

                    // Input Discreto
                    BasicTextFieldCustom(
                        value = kmMesInput.value,
                        onValueChange = { if (it.length <= 6) kmMesInput.value = it.filter { ch -> ch.isDigit() } },
                        color = textLight,
                        accent = accent
                    )
                }
            }

            // 2. DASHBOARD FINANCEIRO (Hero Section)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF059669), Color(0xFF064E3B))
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)), color = accent)
                Spacer(Modifier.width(12.dp))
                Text("Proximas Manutencoes", color = textLight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (proximosAvisos.isEmpty()) {
                EmptyStateCard(surfaceDark, textDim)
            } else {
                proximosAvisos.forEach { lembrete ->
                    AvisoCardModerno(lembrete, surfaceDark, textLight, textDim, accent, success, warning, danger)
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
                    Icon(lembrete.tipo.getIcon(), contentDescription = null, tint = statusColor)
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
                        color = backgroundColor.copy(alpha = 0.5f), // Darker shade
                        border = androidx.compose.foundation.BorderStroke(1.dp, dimColor.copy(alpha = 0.2f)),
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
    TipoManutencao.OLEO -> "Desgaste severo do motor"
    TipoManutencao.BATERIA -> "Falha na partida (pane)"
    TipoManutencao.MECANICA -> "Quebras maiores e guincho"
    TipoManutencao.FREIO -> "Perda de freio (acidente)"
    TipoManutencao.TEMPERATURA -> "Motor fundido por calor"
    TipoManutencao.LICENCIAMENTO -> "Apreensao do veiculo"
    TipoManutencao.IPVA -> "Bloqueio de documento"
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

private fun formatarMoedaMV(valor: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
}

