package br.com.gui.carlembrete

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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

private data class FleetTripSnapshot(
    val name: String,
    val location: String,
    val responsible: String,
    val isFinished: Boolean,
    val vehiclesUsed: List<String>
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
    var healthFilter by remember { mutableStateOf<FleetHealth?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
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

    val ownerFilter: String? = null
    val ownerScopedStatuses = fleetStatuses

    val total = ownerScopedStatuses.size
    val good = ownerScopedStatuses.count { it.health == FleetHealth.GOOD }
    val attention = ownerScopedStatuses.count { it.health == FleetHealth.ATTENTION }
    val critical = ownerScopedStatuses.count { it.health == FleetHealth.CRITICAL }
    val pendingTotal = ownerScopedStatuses.sumOf { it.pendingCost }
    val criticalPrev = remember(carros, lembretes, today, selectedPeriod) {
        val previousRef = today.minusDays(selectedPeriod.days.toLong())
        carros
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
    val tripSnapshots = loadFleetTripSnapshots(context)
    val activeTripSnapshots = tripSnapshots.filterNot { it.isFinished }
    val companyName = "Sua Empresa"
    val activeVehicles = ownerScopedStatuses.count { it.recentFuelCost > 0.0 || it.recentFuelLiters > 0.0 }
    val periodFuelCost = ownerScopedStatuses.sumOf { it.recentFuelCost }
    val periodFuelLiters = ownerScopedStatuses.sumOf { it.recentFuelLiters }
    val averageCostPerActiveVehicle = if (activeVehicles > 0) periodFuelCost / activeVehicles else 0.0

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) colorScheme.background else colorScheme.background
    val surface = colorScheme.surface
    val textPrimary = colorScheme.onSurface
    val textDim = colorScheme.onSurfaceVariant
    val exportFleetPdf: () -> Unit = {
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
    val exportFleetSpreadsheet: () -> Unit = {
        scope.launch {
            val spreadsheet = withContext(Dispatchers.IO) {
                gerarPlanilhaStatusFrota(
                    context = context,
                    statuses = filteredStatuses,
                    activeTrips = activeTripSnapshots,
                    selectedPeriod = selectedPeriod,
                    ownerFilter = ownerFilter,
                    healthFilter = healthFilter,
                    companyName = companyName,
                    total = total,
                    good = good,
                    attention = attention,
                    critical = critical,
                    pendingTotal = pendingTotal,
                    periodFuelCost = periodFuelCost
                )
            }
            if (spreadsheet != null) {
                compartilharPlanilhaFrota(context, spreadsheet)
            } else {
                Toast.makeText(context, "Nao foi possivel gerar a planilha da frota.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textDim)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = textDim.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BuildCircle,
                        contentDescription = null,
                        tint = textDim,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    "Status da Frota",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp
                )
            }

            OutlinedButton(
                onClick = { showExportDialog = true },
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.18f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.24f) else Color.White,
                    contentColor = textPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Exportar",
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showExportDialog) {
                Dialog(onDismissRequest = { showExportDialog = false }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Exportar",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Exporte para compartilhar a leitura atual da frota.",
                                color = textDim,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    exportFleetPdf()
                                },
                                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Exportar PDF", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    exportFleetSpreadsheet()
                                },
                                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Exportar planilha", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { showExportDialog = false },
                                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.26f),
                                    contentColor = textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Fechar", fontWeight = FontWeight.SemiBold)
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
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status das viagens", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (activeTripSnapshots.isEmpty()) {
                        Text("Nenhuma viagem em andamento.", color = textDim, fontSize = 12.sp)
                    } else {
                        activeTripSnapshots.take(4).forEach { trip ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(trip.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "Em andamento",
                                            color = Color(0xFF2563EB),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text("Responsável: ${trip.responsible.ifBlank { "Nao informado" }}", color = textDim, fontSize = 11.sp)
                                    Text("Local: ${trip.location.ifBlank { "Nao informado" }}", color = textDim, fontSize = 11.sp)
                                    Text(
                                        "Veículo(s): ${trip.vehiclesUsed.joinToString(", ").ifBlank { "Sem veículo" }}",
                                        color = textDim,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        if (activeTripSnapshots.size > 4) {
                            Text(
                                "+${activeTripSnapshots.size - 4} viagem(ns) em andamento.",
                                color = textDim,
                                fontSize = 11.sp
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
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resumo da frota", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryPill("Veículos", total.toString(), Color(0xFF2563EB), Modifier.weight(1f))
                        SummaryPill("Em dia", good.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryPill("Atenção", attention.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                        SummaryPill("Crítico", critical.toString(), Color(0xFFEF4444), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryPill("Usados", activeVehicles.toString(), Color(0xFF0891B2), Modifier.weight(1f))
                        SummaryPill("Sem uso", withoutRecentFuel.toString(), Color(0xFF64748B), Modifier.weight(1f))
                    }
                    Text(
                        "Custo pendente de manutenções: ${formatCurrency(pendingTotal)}",
                        color = textDim,
                        fontSize = 12.sp
                    )
                    Text(
                        "Gasto no período: ${formatCurrency(periodFuelCost)} • ${String.format(java.util.Locale.US, "%.1f L", periodFuelLiters)}",
                        color = textDim,
                        fontSize = 12.sp
                    )
                    Text(
                        "Média por veículo ativo: ${formatCurrency(averageCostPerActiveVehicle)}",
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
                            color = Color(0xFFEF4444)
                        )
                    }
                    if (attention > 0) {
                        AlertActionCard(
                            text = "$attention veículo(s) com manutenção próxima.",
                            color = Color(0xFFF59E0B)
                        )
                    }
                    if (withoutRecentFuel > 0) {
                        AlertActionCard(
                            text = "$withoutRecentFuel veículo(s) sem abastecimento nos últimos ${selectedPeriod.days} dias.",
                            color = Color(0xFF2563EB)
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
                    Text("Uso e custo da frota", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                Text(
                    "Detalhes por veículo",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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

            status.lastFuelValue?.let {
                Text(
                    "Último valor de abastecimento: ${formatCurrency(it)}",
                    color = textDim,
                    fontSize = 12.sp
                )
            }
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
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)
        )
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
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
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

private fun loadFleetTripSnapshots(context: Context): List<FleetTripSnapshot> {
    val rawTrips = context
        .getSharedPreferences("travel_expenses_prefs", Context.MODE_PRIVATE)
        .getString("travel_trips_json", null)
        ?: return emptyList()

    return runCatching {
        val tripsArray = JSONArray(rawTrips)
        buildList {
            for (i in 0 until tripsArray.length()) {
                val tripObj = tripsArray.optJSONObject(i) ?: continue
                val expensesArray = tripObj.optJSONArray("expenses") ?: JSONArray()
                val vehicles = buildSet {
                    for (j in 0 until expensesArray.length()) {
                        val expenseObj = expensesArray.optJSONObject(j) ?: continue
                        val vehicleName = expenseObj.optString("vehicleName").trim()
                        if (vehicleName.isNotBlank()) add(vehicleName)
                    }
                }.toList()

                add(
                    FleetTripSnapshot(
                        name = tripObj.optString("name").ifBlank { "Minha viagem" },
                        location = tripObj.optString("location"),
                        responsible = tripObj.optString("responsible"),
                        isFinished = tripObj.optBoolean("isFinished", false),
                        vehiclesUsed = vehicles
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
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
        val coverAccentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, coverAccentPaint)
        val coverTitlePaint = Paint(titlePaint).apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            textSize = 26f
        }
        canvas.drawText("RELATORIO EXECUTIVO", pageWidth / 2f, 60f, coverTitlePaint)
        val coverSubPaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.parseColor("#BFDBFE")
            textSize = 14f
            isAntiAlias = true
        }
        canvas.drawText("STATUS DA FROTA", pageWidth / 2f, 88f, coverSubPaint)
        val coverDivider = Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            strokeWidth = 1.2f
        }
        canvas.drawLine(margin, 210f, pageWidth - margin, 210f, coverDivider)
        val coverInfoPaint = Paint(bodyPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 14f
            color = android.graphics.Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(companyName, pageWidth / 2f, 252f, coverInfoPaint)
        val coverDetailPaint = Paint(bodyPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 11f
            color = android.graphics.Color.parseColor("#64748B")
        }
        canvas.drawText("Periodo: ${selectedPeriod.label}", pageWidth / 2f, 282f, coverDetailPaint)
        canvas.drawText("Responsavel: ${ownerFilter ?: "Todos"}", pageWidth / 2f, 304f, coverDetailPaint)
        canvas.drawText("Emitido em ${LocalDate.now().format(dateFormatter)}", pageWidth / 2f, 326f, coverDetailPaint)
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
        val pageAccentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 72f, pageAccentPaint)
        val pageTitlePaint = Paint(titlePaint).apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            textSize = 18f
        }
        canvas.drawText("RELATORIO STATUS DA FROTA", pageWidth / 2f, 40f, pageTitlePaint)
        val pageDatePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.parseColor("#BFDBFE")
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText(
            "Gerado em ${LocalDate.now().format(dateFormatter)}  |  Periodo: ${selectedPeriod.label}  |  Resp.: ${ownerFilter ?: "Todos"}",
            pageWidth / 2f, 58f, pageDatePaint
        )
        y = 90f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f

        val sectionAccentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        val accentSectionPaint = Paint(sectionPaint).apply { color = android.graphics.Color.parseColor("#2563EB") }

        canvas.drawRect(margin, y - 13f, margin + 4f, y + 3f, sectionAccentPaint)
        canvas.drawText("RESUMO EXECUTIVO", margin + 10f, y, accentSectionPaint)
        y += 18f

        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val metricLabelPaint = Paint().apply {
            textSize = 9f
            color = android.graphics.Color.parseColor("#64748B")
            isAntiAlias = true
        }
        val metricValuePaint = Paint().apply {
            textSize = 20f
            color = android.graphics.Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val halfWidth = (pageWidth - margin * 2 - 8f) / 2f
        val bigCardH = 52f

        val card1Rect = android.graphics.RectF(margin, y, margin + halfWidth, y + bigCardH)
        canvas.drawRoundRect(card1Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card1Rect, 8f, 8f, cardBorderPaint)
        canvas.drawText("TOTAL VEICULOS", margin + 8f, y + 18f, metricLabelPaint)
        canvas.drawText(total.toString(), margin + 8f, y + 44f, metricValuePaint)

        val card2X = margin + halfWidth + 8f
        val card2Rect = android.graphics.RectF(card2X, y, card2X + halfWidth, y + bigCardH)
        canvas.drawRoundRect(card2Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card2Rect, 8f, 8f, cardBorderPaint)
        canvas.drawText("CUSTO PENDENTE", card2X + 8f, y + 18f, metricLabelPaint)
        val costValuePaint = Paint(metricValuePaint).apply { textSize = 15f }
        canvas.drawText(formatCurrency(pendingTotal), card2X + 8f, y + 44f, costValuePaint)
        y += bigCardH + 8f

        val thirdWidth = (pageWidth - margin * 2 - 16f) / 3f
        val miniCardH = 50f
        val colorGood = android.graphics.Color.parseColor("#16A34A")
        val colorWarn = android.graphics.Color.parseColor("#D97706")
        val colorCrit = android.graphics.Color.parseColor("#DC2626")
        listOf(
            Triple("EM DIA", good, colorGood),
            Triple("ATENCAO", attention, colorWarn),
            Triple("CRITICOS", critical, colorCrit)
        ).forEachIndexed { i, (label, count, color) ->
            val cardX = margin + i * (thirdWidth + 8f)
            val miniRect = android.graphics.RectF(cardX, y, cardX + thirdWidth, y + miniCardH)
            canvas.drawRoundRect(miniRect, 8f, 8f, cardBgPaint)
            val leftStripPaint = Paint().apply { this.color = color }
            canvas.drawRoundRect(android.graphics.RectF(cardX, y, cardX + 4f, y + miniCardH), 4f, 4f, leftStripPaint)
            canvas.drawRoundRect(miniRect, 8f, 8f, cardBorderPaint)
            val coloredLabel = Paint(metricLabelPaint).apply { this.color = color }
            canvas.drawText(label, cardX + 10f, y + 18f, coloredLabel)
            val countPaint = Paint(metricValuePaint).apply { textSize = 18f; this.color = color }
            canvas.drawText(count.toString(), cardX + 10f, y + 42f, countPaint)
        }
        y += miniCardH + 16f

        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 18f
        canvas.drawRect(margin, y - 13f, margin + 4f, y + 3f, sectionAccentPaint)
        canvas.drawText("DETALHAMENTO POR VEICULO", margin + 10f, y, accentSectionPaint)
        y += 18f

        if (statuses.isEmpty()) {
            drawLine("Nenhum veiculo no filtro atual.")
        } else {
            val vehicleCardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
            val vehicleCardBorderPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                isAntiAlias = true
            }
            val pillTextPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val vehicleNamePaint = Paint(sectionPaint).apply {
                textSize = 12f
                color = android.graphics.Color.parseColor("#0F172A")
            }

            statuses.forEachIndexed { index, item ->
                val cardHeight = 120f
                ensureSpace(cardHeight + 10f)
                val healthColor = when (item.health) {
                    FleetHealth.GOOD -> android.graphics.Color.parseColor("#16A34A")
                    FleetHealth.ATTENTION -> android.graphics.Color.parseColor("#D97706")
                    FleetHealth.CRITICAL -> android.graphics.Color.parseColor("#DC2626")
                }
                val healthText = when (item.health) {
                    FleetHealth.GOOD -> "EM DIA"
                    FleetHealth.ATTENTION -> "ATENCAO"
                    FleetHealth.CRITICAL -> "CRITICO"
                }
                val cardTop = y
                val cardRect = android.graphics.RectF(margin, cardTop, pageWidth - margin, cardTop + cardHeight)
                canvas.drawRoundRect(cardRect, 8f, 8f, vehicleCardBgPaint)
                val leftStripPaint = Paint().apply { color = healthColor }
                canvas.drawRoundRect(android.graphics.RectF(margin, cardTop, margin + 4f, cardTop + cardHeight), 4f, 4f, leftStripPaint)
                canvas.drawRoundRect(cardRect, 8f, 8f, vehicleCardBorderPaint)

                val textX = margin + 12f
                canvas.drawText("${index + 1}. ${item.carro.nome}", textX, cardTop + 20f, vehicleNamePaint)

                val pillText = healthText
                val pillW = pillTextPaint.measureText(pillText) + 14f
                val pillLeft = pageWidth - margin - pillW - 6f
                val pillPaint = Paint().apply { color = healthColor }
                canvas.drawRoundRect(android.graphics.RectF(pillLeft, cardTop + 8f, pageWidth - margin - 6f, cardTop + 26f), 8f, 8f, pillPaint)
                canvas.drawText(pillText, pillLeft + 7f, cardTop + 21f, pillTextPaint)

                var textY = cardTop + 38f
                val lineH = 16f
                canvas.drawText("Tipo: ${item.carro.tipoVeiculo.label}  |  Responsavel: ${item.owner}", textX, textY, bodyPaint)
                textY += lineH
                canvas.drawText("Vencidas: ${item.overdueCount}  |  Proximas 30d: ${item.upcoming30Count}  |  Prox. manut.: ${item.nextMaintenanceDate?.format(dateFormatter) ?: "--"}", textX, textY, bodyPaint)
                textY += lineH
                canvas.drawText("Ult. abastecimento: ${item.lastFuelDate?.format(dateFormatter) ?: "--"}  |  Qtd. periodo: ${item.recentFuelCount}  |  Litros: ${String.format(java.util.Locale.US, "%.1f", item.recentFuelLiters)}", textX, textY, bodyPaint)
                textY += lineH
                canvas.drawText("Custo combustivel: ${formatCurrency(item.recentFuelCost)}  |  Pendente manut.: ${formatCurrency(item.pendingCost)}", textX, textY, bodyPaint)

                y = cardTop + cardHeight + 10f
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

private fun gerarPlanilhaStatusFrota(
    context: Context,
    statuses: List<VehicleFleetStatus>,
    activeTrips: List<FleetTripSnapshot>,
    selectedPeriod: FleetPeriod,
    ownerFilter: String?,
    healthFilter: FleetHealth?,
    companyName: String,
    total: Int,
    good: Int,
    attention: Int,
    critical: Int,
    pendingTotal: Double,
    periodFuelCost: Double
): File? = runCatching {
    val file = File(context.cacheDir, "status_frota_${System.currentTimeMillis()}.xls")
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val healthLabel = when (healthFilter) {
        FleetHealth.GOOD -> "Saude boa"
        FleetHealth.ATTENTION -> "Atencao"
        FleetHealth.CRITICAL -> "Critico"
        null -> "Todos"
    }
    val content = buildString {
        appendLine("""<?xml version="1.0"?>""")
        appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
        appendLine("""<Styles>""")
        appendLine("""<Style ss:ID="Title"><Font ss:Bold="1" ss:Size="14"/><Alignment ss:Horizontal="Center"/></Style>""")
        appendLine("""<Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#1E3A8A" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="Label"><Font ss:Bold="1"/></Style>""")
        appendLine("""<Style ss:ID="Money"><NumberFormat ss:Format="Currency"/></Style>""")
        appendLine("""<Style ss:ID="Wrap"><Alignment ss:WrapText="1" ss:Vertical="Top"/></Style>""")
        appendLine("""</Styles>""")

        appendLine("""<Worksheet ss:Name="Resumo">""")
        appendLine("""<Table>""")
        appendLine("""<Column ss:Width="190"/><Column ss:Width="220"/>""")
        appendLine("""<Row><Cell ss:MergeAcross="1" ss:StyleID="Title"><Data ss:Type="String">STATUS DA FROTA</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Empresa</Data></Cell><Cell><Data ss:Type="String">${companyName.escapeSpreadsheetXml()}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Gerado em</Data></Cell><Cell><Data ss:Type="String">$today</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Periodo</Data></Cell><Cell><Data ss:Type="String">${selectedPeriod.label}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Responsavel</Data></Cell><Cell><Data ss:Type="String">${(ownerFilter ?: "Todos").escapeSpreadsheetXml()}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Filtro</Data></Cell><Cell><Data ss:Type="String">${healthLabel.escapeSpreadsheetXml()}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Total de veiculos</Data></Cell><Cell><Data ss:Type="Number">$total</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Saude boa</Data></Cell><Cell><Data ss:Type="Number">$good</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Em atencao</Data></Cell><Cell><Data ss:Type="Number">$attention</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Criticos</Data></Cell><Cell><Data ss:Type="Number">$critical</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Custo combustivel no periodo</Data></Cell><Cell ss:StyleID="Money"><Data ss:Type="Number">${periodFuelCost.toSpreadsheetNumber()}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="Label"><Data ss:Type="String">Custo pendente manutencao</Data></Cell><Cell ss:StyleID="Money"><Data ss:Type="Number">${pendingTotal.toSpreadsheetNumber()}</Data></Cell></Row>""")
        appendLine("""</Table>""")
        appendLine("""</Worksheet>""")

        appendLine("""<Worksheet ss:Name="Viagens Ativas">""")
        appendLine("""<Table>""")
        appendLine("""<Column ss:Width="170"/><Column ss:Width="170"/><Column ss:Width="150"/><Column ss:Width="220"/>""")
        appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Viagem</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Responsavel</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Local</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Veiculos usados</Data></Cell></Row>""")
        if (activeTrips.isEmpty()) {
            appendLine("""<Row><Cell ss:MergeAcross="3"><Data ss:Type="String">Nenhuma viagem em andamento.</Data></Cell></Row>""")
        } else {
            activeTrips.forEach { trip ->
                appendLine(
                    """<Row>
<Cell><Data ss:Type="String">${trip.name.escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="String">${trip.responsible.ifBlank { "Nao informado" }.escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="String">${trip.location.ifBlank { "Nao informado" }.escapeSpreadsheetXml()}</Data></Cell>
<Cell ss:StyleID="Wrap"><Data ss:Type="String">${trip.vehiclesUsed.joinToString(", ").ifBlank { "Sem veiculo" }.escapeSpreadsheetXml()}</Data></Cell>
</Row>"""
                )
            }
        }
        appendLine("""</Table>""")
        appendLine("""</Worksheet>""")

        appendLine("""<Worksheet ss:Name="Veiculos">""")
        appendLine("""<Table>""")
        appendLine("""<Column ss:Width="160"/><Column ss:Width="120"/><Column ss:Width="140"/><Column ss:Width="80"/><Column ss:Width="80"/><Column ss:Width="115"/><Column ss:Width="110"/><Column ss:Width="110"/><Column ss:Width="120"/>""")
        appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Veiculo</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Tipo</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Responsavel</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Vencidas</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Prox. manutencao</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Ult. abastecimento</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Custo periodo</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Pendente manut.</Data></Cell></Row>""")
        if (statuses.isEmpty()) {
            appendLine("""<Row><Cell ss:MergeAcross="8"><Data ss:Type="String">Nenhum veiculo no filtro atual.</Data></Cell></Row>""")
        } else {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            statuses.forEach { item ->
                val health = when (item.health) {
                    FleetHealth.GOOD -> "Saude boa"
                    FleetHealth.ATTENTION -> "Atencao"
                    FleetHealth.CRITICAL -> "Critico"
                }
                appendLine(
                    """<Row>
<Cell><Data ss:Type="String">${item.carro.nome.escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="String">${item.carro.tipoVeiculo.label.escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="String">${item.owner.escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="String">${health.escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="Number">${item.overdueCount}</Data></Cell>
<Cell><Data ss:Type="String">${(item.nextMaintenanceDate?.format(formatter) ?: "Sem previsao").escapeSpreadsheetXml()}</Data></Cell>
<Cell><Data ss:Type="String">${(item.lastFuelDate?.format(formatter) ?: "Sem registro").escapeSpreadsheetXml()}</Data></Cell>
<Cell ss:StyleID="Money"><Data ss:Type="Number">${item.recentFuelCost.toSpreadsheetNumber()}</Data></Cell>
<Cell ss:StyleID="Money"><Data ss:Type="Number">${item.pendingCost.toSpreadsheetNumber()}</Data></Cell>
</Row>"""
                )
            }
        }
        appendLine("""</Table>""")
        appendLine("""</Worksheet>""")
        appendLine("""</Workbook>""")
    }
    file.writeText(content, Charsets.UTF_8)
    file
}.getOrNull()

private fun compartilharPlanilhaFrota(context: Context, spreadsheetFile: File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        spreadsheetFile
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/vnd.ms-excel"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Exportar planilha da frota"))
}

private fun String.escapeSpreadsheetXml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun Double.toSpreadsheetNumber(): String =
    String.format(java.util.Locale.US, "%.2f", this)


