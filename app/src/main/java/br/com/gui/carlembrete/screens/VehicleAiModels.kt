package br.com.gui.carlembrete

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

data class VehicleChatMessage(
    val text: String,
    val fromUser: Boolean,
    val isTyping: Boolean = false,
    val hasReminderDrafts: Boolean = false,
    val hasFuelDrafts: Boolean = false,
    val hasServiceRecordDrafts: Boolean = false,
    val sentAt: LocalTime = LocalTime.now()
)


enum class VehicleRiskLevel(
    val label: String,
    val color: Color
) {
    LOW("Baixo", Color(0xFF16A34A)),
    MEDIUM("Atencao", Color(0xFFF59E0B)),
    HIGH("Alto", Color(0xFFDC2626))
}

data class GarageVehicleStatus(
    val carro: CarroInfo,
    val risk: VehicleRiskLevel,
    val activeCount: Int,
    val overdueCount: Int,
    val next30Count: Int = 0,
    val topWarnings: String = "",
    val fuel: GarageFuelSummary = GarageFuelSummary()
)

data class GarageFuelSummary(
    val fuelCount: Int = 0,
    val totalCost: Double = 0.0,
    val totalLiters: Double = 0.0,
    val distanceKm: Int? = null,
    val kmPerLiter: Double? = null,
    val costPerKm: Double? = null,
    val lastFuelDate: String? = null
)

data class AiReminderDraft(
    val carro: CarroInfo,
    val titulo: String,
    val peca: String,
    val dataLimite: String,
    val kmLimite: String,
    val tipo: TipoManutencao,
    val horaAviso: String = "09:00",
    val horaInformada: Boolean = false
)

data class AiFuelDraft(
    val carro: CarroInfo,
    val valorPago: Double,
    val precoLitro: Double,
    val litros: Double,
    val data: String,
    val km: Int,
    val tipoCombustivel: String,
    val precoEstimado: Boolean
)

data class AiServiceRecordDraft(
    val carro: CarroInfo,
    val titulo: String,
    val descricao: String,
    val dataExecucao: String,
    val tipo: TipoManutencao,
    val valor: Double?
)

sealed class AiReportRequest {
    data object Fleet : AiReportRequest()
    data object MissingVehicle : AiReportRequest()
    data class Vehicle(val carro: CarroInfo) : AiReportRequest()
}
