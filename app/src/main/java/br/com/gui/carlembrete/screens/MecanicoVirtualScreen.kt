package br.com.gui.carlembrete

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class FleetHealth { GOOD, ATTENTION, CRITICAL }
private enum class FleetPeriod(val label: String, val days: Int) {
    D7("7 dias", 7),
    D30("30 dias", 30),
    D90("90 dias", 90)
}

private data class VehicleFleetStatus(
    val carro: CarroInfo,
    val owner: String,
    val health: FleetHealth,
    val overdueCount: Int,
    val upcoming30Count: Int,
    val nextMaintenanceDate: LocalDate?,
    val pendingCost: Double,
    val lastFuelDate: LocalDate?,
    val lastFuelValue: Double?,
    val recentFuelCount: Int,
    val recentFuelCost: Double,
    val recentFuelLiters: Double
)

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
    // Mantidos por compatibilidade da assinatura
    if (!isPremium && carros.isEmpty() && lembretes.isEmpty()) {
        onPremiumRequired()
    }

    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val today = remember { LocalDate.now() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf(FleetPeriod.D30) }
    var ownerFilter by remember { mutableStateOf<String?>(null) }
    var healthFilter by remember { mutableStateOf<FleetHealth?>(null) }
    val periodStart = remember(today, selectedPeriod) { today.minusDays((selectedPeriod.days - 1).toLong()) }

    val fleetStatuses = remember(carros, abastecimentos, lembretes, today, periodStart) {
        carros.map { carro ->
            val reminders = lembretes.filter { it.carroId == carro.id }
                .filterNot { isLembreteRealizado(it) }

            val reminderDates = reminders.mapNotNull { lembrete ->
                val date = dataParaOrdenacao(lembrete)
                if (date == LocalDate.MAX) null else date
            }

            val overdueCount = reminderDates.count { it.isBefore(today) }
            val upcoming30Count = reminderDates.count { !it.isBefore(today) && ChronoUnit.DAYS.between(today, it) <= 30 }
            val nextMaintenanceDate = reminderDates.filter { !it.isBefore(today) }.minOrNull()
            val pendingCost = reminders.sumOf { it.valor }

            val fuels = abastecimentos.filter { it.carroId == carro.id }
            val lastFuel = fuels.mapNotNull { a ->
                runCatching { LocalDate.parse(a.data, formatter) }.getOrNull()?.let { d -> d to a }
            }.maxByOrNull { it.first }
            val fuelsInPeriod = fuels.mapNotNull { a ->
                runCatching { LocalDate.parse(a.data, formatter) }.getOrNull()?.let { d -> d to a }
            }.filter { (d, _) -> !d.isBefore(periodStart) }

            val health = when {
                overdueCount > 0 -> FleetHealth.CRITICAL
                upcoming30Count > 0 -> FleetHealth.ATTENTION
                else -> FleetHealth.GOOD
            }

            VehicleFleetStatus(
                carro = carro,
                owner = carro.proprietario.ifBlank { "Sem responsável" },
                health = health,
                overdueCount = overdueCount,
                upcoming30Count = upcoming30Count,
                nextMaintenanceDate = nextMaintenanceDate,
                pendingCost = pendingCost,
                lastFuelDate = lastFuel?.first,
                lastFuelValue = lastFuel?.second?.valorPago,
                recentFuelCount = fuelsInPeriod.size,
                recentFuelCost = fuelsInPeriod.sumOf { it.second.valorPago },
                recentFuelLiters = fuelsInPeriod.sumOf { it.second.litros }
            )
        }.sortedWith(compareBy<VehicleFleetStatus> {
            when (it.health) {
                FleetHealth.CRITICAL -> 0
                FleetHealth.ATTENTION -> 1
                FleetHealth.GOOD -> 2
            }
        }.thenBy { it.carro.nome })
    }

    val tripResponsibles = remember(context) { carregarResponsaveisViagem(context) }
    val ownerOptions = remember(fleetStatuses, tripResponsibles) {
        (fleetStatuses.map { it.owner } + tripResponsibles).map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val ownerScopedStatuses = fleetStatuses.filter { ownerFilter == null || it.owner == ownerFilter }

    val total = ownerScopedStatuses.size
    val good = ownerScopedStatuses.count { it.health == FleetHealth.GOOD }
    val attention = ownerScopedStatuses.count { it.health == FleetHealth.ATTENTION }
    val critical = ownerScopedStatuses.count { it.health == FleetHealth.CRITICAL }
    val pendingTotal = ownerScopedStatuses.sumOf { it.pendingCost }
    val criticalPrev = remember(carros, lembretes, today, selectedPeriod, ownerFilter) {
        val previousRef = today.minusDays(selectedPeriod.days.toLong())
        carros
            .filter { ownerFilter == null || it.proprietario.ifBlank { "Sem responsável" } == ownerFilter }
            .count { carro ->
            lembretes
                .asSequence()
                .filter { it.carroId == carro.id }
                .filterNot { isLembreteRealizado(it) }
                .map { dataParaOrdenacao(it) }
                .any { it.isBefore(previousRef) }
            }
    }
    val criticalTrendDelta = critical - criticalPrev
    val withoutRecentFuel = ownerScopedStatuses.count { it.lastFuelDate == null || it.lastFuelDate.isBefore(periodStart) }
    val filteredStatuses = ownerScopedStatuses.filter { status -> healthFilter == null || status.health == healthFilter }
    val topCostVehicles = ownerScopedStatuses.sortedByDescending { it.recentFuelCost }.take(5)
    val topUsageVehicles = ownerScopedStatuses.sortedByDescending { it.recentFuelLiters }.take(5)
    val topOwners = ownerScopedStatuses
        .groupBy { it.owner }
        .map { (owner, list) ->
            Triple(owner, list.sumOf { it.recentFuelCost }, list.sumOf { it.recentFuelLiters })
        }
        .sortedByDescending { it.second }
        .take(5)
    val companyName = ownerFilter ?: ownerOptions.firstOrNull() ?: "Sua Empresa"
    val currentMonthLabel = remember(today) { today.format(DateTimeFormatter.ofPattern("MM/yyyy")) }
    val criticalTarget = 1
    val kpiAtingido = critical <= criticalTarget

    val colorScheme = MaterialTheme.colorScheme
    val bg = colorScheme.background
    val surface = colorScheme.surface
    val textPrimary = colorScheme.onSurface
    val textDim = colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Status da Frota", fontWeight = FontWeight.Bold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val exportStatuses = filteredStatuses
                                val uri = withContext(Dispatchers.IO) {
                                    gerarPdfStatusFrota(
                                        context = context,
                                        statuses = exportStatuses,
                                        selectedPeriod = selectedPeriod,
                                        ownerFilter = ownerFilter,
                                        healthFilter = healthFilter,
                                        companyName = companyName,
                                        total = total,
                                        good = good,
                                        attention = attention,
                                        critical = critical,
                                        pendingTotal = pendingTotal
                                    )
                                }
                                if (uri != null) {
                                    compartilharPdf(context, uri)
                                } else {
                                    Toast.makeText(context, "Nao foi possivel gerar o PDF da frota.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = textPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FleetPeriod.values().forEach { period ->
                        val selected = selectedPeriod == period
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) colorScheme.primary.copy(alpha = 0.2f) else colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) colorScheme.primary else colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { selectedPeriod = period }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(period.label, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OwnerChip(
                        label = "Todos",
                        selected = ownerFilter == null,
                        onClick = { ownerFilter = null; healthFilter = null }
                    )
                    ownerOptions.forEach { owner ->
                        OwnerChip(
                            label = owner,
                            selected = ownerFilter == owner,
                            onClick = { ownerFilter = owner; healthFilter = null }
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KPI mensal", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Meta de críticos ($currentMonthLabel): até $criticalTarget veículo(s).", color = textDim, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Atual: $critical crítico(s)",
                            color = if (kpiAtingido) Color(0xFF16A34A) else Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            if (kpiAtingido) "Meta atingida" else "Fora da meta",
                            color = if (kpiAtingido) Color(0xFF16A34A) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resumo geral", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Responsável: ${ownerFilter ?: "Todos"}", color = textDim, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryPill("Veículos", total.toString(), Color(0xFF2563EB), Modifier.weight(1f))
                        SummaryPill("Saúde boa", good.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryPill("Atenção", attention.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                        SummaryPill("Crítico", critical.toString(), Color(0xFFEF4444), Modifier.weight(1f))
                    }
                    Text(
                        "Custo pendente de manutenções: ${formatCurrency(pendingTotal)}",
                        color = textDim,
                        fontSize = 12.sp
                    )
                    Text(
                        "Críticos no período anterior: $criticalPrev • Variação: ${if (criticalTrendDelta > 0) "+" else ""}$criticalTrendDelta",
                        color = if (criticalTrendDelta > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alertas acionáveis", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (critical > 0) {
                        AlertActionCard(
                            text = "$critical veículo(s) com risco crítico de manutenção.",
                            color = Color(0xFFEF4444),
                            actionLabel = "Ver críticos",
                            onClick = { healthFilter = FleetHealth.CRITICAL }
                        )
                    }
                    if (attention > 0) {
                        AlertActionCard(
                            text = "$attention veículo(s) com manutenção próxima.",
                            color = Color(0xFFF59E0B),
                            actionLabel = "Ver atenção",
                            onClick = { healthFilter = FleetHealth.ATTENTION }
                        )
                    }
                    if (withoutRecentFuel > 0) {
                        AlertActionCard(
                            text = "$withoutRecentFuel veículo(s) sem abastecimento nos últimos ${selectedPeriod.days} dias.",
                            color = Color(0xFF2563EB),
                            actionLabel = "Ver todos",
                            onClick = { healthFilter = null }
                        )
                    }
                    if (critical == 0 && attention == 0 && withoutRecentFuel == 0) {
                        Text(
                            "Nenhum alerta crítico no período selecionado.",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Comparativo da frota", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Top custo por veículo (${selectedPeriod.label})", color = textDim, fontSize = 12.sp)
                    if (topCostVehicles.isEmpty()) {
                        Text("Sem dados de abastecimento no período.", color = textDim, fontSize = 12.sp)
                    } else {
                        topCostVehicles.forEachIndexed { index, item ->
                            RankingRow(
                                position = index + 1,
                                title = item.carro.nome,
                                subtitle = item.owner,
                                value = formatCurrency(item.recentFuelCost)
                            )
                        }
                    }
                    Text("Top uso por veículo (litros)", color = textDim, fontSize = 12.sp)
                    if (topUsageVehicles.isEmpty()) {
                        Text("Sem dados de uso no período.", color = textDim, fontSize = 12.sp)
                    } else {
                        topUsageVehicles.forEachIndexed { index, item ->
                            RankingRow(
                                position = index + 1,
                                title = item.carro.nome,
                                subtitle = item.owner,
                                value = String.format(java.util.Locale.US, "%.1f L", item.recentFuelLiters)
                            )
                        }
                    }
                    Text("Top responsáveis (custo total)", color = textDim, fontSize = 12.sp)
                    if (topOwners.isEmpty()) {
                        Text("Sem responsáveis com dados no período.", color = textDim, fontSize = 12.sp)
                    } else {
                        topOwners.forEachIndexed { index, item ->
                            RankingRow(
                                position = index + 1,
                                title = item.first,
                                subtitle = String.format(java.util.Locale.US, "%.1f L", item.third),
                                value = formatCurrency(item.second)
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryPill("Todos", fleetStatuses.size.toString(), Color(0xFF64748B), Modifier.weight(1f))
                    SummaryPill("Boa", good.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                    SummaryPill("Atenção", attention.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                    SummaryPill("Crítico", critical.toString(), Color(0xFFEF4444), Modifier.weight(1f))
                }
            }

            if (fleetStatuses.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = surface),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Nenhum veículo cadastrado para análise.",
                        color = textDim,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                filteredStatuses.forEach { status ->
                    VehicleStatusCard(status = status, today = today)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VehicleStatusCard(
    status: VehicleFleetStatus,
    today: LocalDate
) {
    val scheme = MaterialTheme.colorScheme
    val textPrimary = scheme.onSurface
    val textDim = scheme.onSurfaceVariant

    val (healthText, healthColor, healthBg) = when (status.health) {
        FleetHealth.GOOD -> Triple("Saúde boa", Color(0xFF16A34A), Color(0xFFDCFCE7))
        FleetHealth.ATTENTION -> Triple("Atenção", Color(0xFFF59E0B), Color(0xFFFEF3C7))
        FleetHealth.CRITICAL -> Triple("Crítico", Color(0xFFEF4444), Color(0xFFFEE2E2))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.carro.nome, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        listOfNotNull(
                            status.carro.tipoVeiculo.label,
                            status.carro.marca.takeIf { it.isNotBlank() },
                            status.carro.modelo.takeIf { it.isNotBlank() }
                        ).joinToString(" • "),
                        color = textDim,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(healthBg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(healthText, color = healthColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBox(
                    icon = Icons.Default.BuildCircle,
                    label = "Manutenções",
                    value = "Vencidas: ${status.overdueCount} • Próx. 30d: ${status.upcoming30Count}",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val nextDateText = status.nextMaintenanceDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sem previsão"
                val fuelDateText = status.lastFuelDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sem registro"
                val fuelExtra = if (status.lastFuelDate != null) {
                    val days = ChronoUnit.DAYS.between(status.lastFuelDate, today)
                    " há ${days}d"
                } else ""

                MetricBox(
                    icon = Icons.Default.CalendarMonth,
                    label = "Próxima manutenção",
                    value = nextDateText,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Default.LocalGasStation,
                    label = "Último abastecimento",
                    value = "$fuelDateText$fuelExtra",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "Valor pendente: ${formatCurrency(status.pendingCost)}${status.lastFuelValue?.let { " • Último valor: ${formatCurrency(it)}" } ?: ""}",
                color = textDim,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = scheme.onSurface, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AlertActionCard(
    text: String,
    color: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(actionLabel, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RankingRow(
    position: Int,
    title: String,
    subtitle: String,
    value: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(position.toString(), color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = scheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = scheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Text(value, color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun formatCurrency(value: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(value)
}

@Composable
private fun OwnerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) scheme.primary.copy(alpha = 0.18f) else scheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Text(
            text = label,
            color = scheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

private fun gerarPdfStatusFrota(
    context: Context,
    statuses: List<VehicleFleetStatus>,
    selectedPeriod: FleetPeriod,
    ownerFilter: String?,
    healthFilter: FleetHealth?,
    companyName: String,
    total: Int,
    good: Int,
    attention: Int,
    critical: Int,
    pendingTotal: Double
): Uri? {
    return try {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val lineHeight = 17f
        val titlePaint = Paint().apply {
            textSize = 20f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        var pageIndex = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        var canvas = page.canvas
        var y = 48f

        canvas.drawColor(android.graphics.Color.WHITE)
        val centerPaint = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("RELATORIO EXECUTIVO", pageWidth / 2f, 210f, centerPaint)
        canvas.drawText("STATUS DA FROTA", pageWidth / 2f, 242f, centerPaint)
        val coverBody = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 12f }
        canvas.drawText(companyName, pageWidth / 2f, 290f, coverBody)
        canvas.drawText("Periodo: ${selectedPeriod.label}", pageWidth / 2f, 314f, coverBody)
        canvas.drawText("Responsavel: ${ownerFilter ?: "Todos"}", pageWidth / 2f, 338f, coverBody)
        canvas.drawText("Emitido em ${LocalDate.now().format(dateFormatter)}", pageWidth / 2f, 362f, coverBody)
        document.finishPage(page)
        pageIndex += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        canvas = page.canvas
        y = 48f

        fun newPage() {
            document.finishPage(page)
            pageIndex += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
            canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            y = 48f
        }

        fun ensureSpace(required: Float) {
            if (y + required > pageHeight - 48f) newPage()
        }

        fun drawLine(text: String, paint: Paint = bodyPaint) {
            ensureSpace(lineHeight + 2f)
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawText("RELATORIO STATUS DA FROTA", margin, y, titlePaint)
        y += 24f
        drawLine("Gerado em: ${LocalDate.now().format(dateFormatter)}")
        drawLine("Periodo: ${selectedPeriod.label}")
        drawLine("Responsavel: ${ownerFilter ?: "Todos"}")
        drawLine(
            "Filtro: ${
                when (healthFilter) {
                    FleetHealth.GOOD -> "Saude boa"
                    FleetHealth.ATTENTION -> "Atencao"
                    FleetHealth.CRITICAL -> "Critico"
                    null -> "Todos"
                }
            }"
        )
        y += 4f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f

        canvas.drawText("RESUMO EXECUTIVO", margin, y, sectionPaint)
        y += 18f
        drawLine("Total de veiculos: $total")
        drawLine("Saude boa: $good")
        drawLine("Em atencao: $attention")
        drawLine("Criticos: $critical")
        drawLine("Custo pendente de manutencao: ${formatCurrency(pendingTotal)}")

        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f
        canvas.drawText("DETALHAMENTO POR VEICULO", margin, y, sectionPaint)
        y += 18f

        if (statuses.isEmpty()) {
            drawLine("Nenhum veiculo no filtro atual.")
        } else {
            statuses.forEachIndexed { index, item ->
                ensureSpace(112f)
                val healthText = when (item.health) {
                    FleetHealth.GOOD -> "Saude boa"
                    FleetHealth.ATTENTION -> "Atencao"
                    FleetHealth.CRITICAL -> "Critico"
                }
                canvas.drawText("${index + 1}. ${item.carro.nome}", margin, y, sectionPaint)
                y += 16f
                drawLine("Tipo: ${item.carro.tipoVeiculo.label} | Responsavel: ${item.owner}")
                drawLine("Status: $healthText | Vencidas: ${item.overdueCount} | Prox.30d: ${item.upcoming30Count}")
                drawLine("Proxima manutencao: ${item.nextMaintenanceDate?.format(dateFormatter) ?: "Sem previsao"}")
                drawLine("Ultimo abastecimento: ${item.lastFuelDate?.format(dateFormatter) ?: "Sem registro"}")
                drawLine("Abastecimentos no periodo: ${item.recentFuelCount} | Litros: ${String.format(java.util.Locale.US, "%.1f", item.recentFuelLiters)}")
                drawLine("Custo combustivel periodo: ${formatCurrency(item.recentFuelCost)} | Pendente manutencao: ${formatCurrency(item.pendingCost)}")
                y += 4f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 14f
            }
        }

        document.finishPage(page)
        val file = File(context.cacheDir, "status_frota_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}

private fun carregarResponsaveisViagem(context: Context): List<String> {
    return runCatching {
        val raw = context.getSharedPreferences("travel_expenses_prefs", Context.MODE_PRIVATE)
            .getString("travel_trips_json", null)
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val responsible = obj.optString("responsible").trim()
                if (responsible.isNotBlank()) add(responsible)
            }
        }.distinct().sorted()
    }.getOrDefault(emptyList())
}

