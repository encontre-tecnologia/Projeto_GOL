package br.com.gui.carlembrete

import androidx.compose.ui.graphics.Color
import java.util.UUID

internal val PHScreenBg = Color(0xFFF7FAFF)
internal val PHCardBg = Color(0xFFFFFFFF)
internal val PHCardBorder = Color(0xFFE2E8F0)
internal val PHGold = Color(0xFFFFD700)
internal val PHTitle = Color(0xFF0F172A)
internal val PHSub = Color(0xFF475569)
internal val PHDim = Color(0xFF64748B)
internal const val OPERATIONAL_PREFS = "premium_operational_records"
internal const val OPERATIONAL_RECORDS_KEY = "records"
internal const val OPERATIONAL_DRIVERS_KEY = "drivers"
internal const val CORPORATE_RESERVATIONS_PREFS = "corporate_fleet_reservations"
internal const val CORPORATE_RESERVATIONS_KEY = "reservations"

internal enum class OperationalFeature {
    TIRE_ROI,
    PARTS_DURABILITY,
    ROUTE_PROFITABILITY
}

internal enum class CorporateFleetModule {
    OVERVIEW,
    RESERVATIONS,
    QR_PICKUP,
    TRIPS,
    MAINTENANCE,
    DOCUMENTS,
    USERS
}

internal data class OperationalRecord(
    val id: String = UUID.randomUUID().toString(),
    val feature: String = OperationalFeature.TIRE_ROI.name,
    val name: String = "",
    val brandOrClient: String = "",
    val vehicleId: String = "",
    val vehicle: String = "",
    val positionOrRoute: String = "",
    val kmStart: Int = 0,
    val kmEnd: Int? = null,
    val cost: Double = 0.0,
    val quantity: Int = 1,
    val recordDate: String = "",
    val revenue: Double? = null,
    val taxPercent: Double? = null,
    val driverId: String = "",
    val driverName: String = "",
    val driverCost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

internal data class OperationalDriver(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val code: String = "",
    val phone: String = "",
    val salary: Double = 0.0,
    val taxCost: Double = 0.0,
    val defaultCost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

internal data class OperationalTravelTrip(
    val id: String,
    val name: String,
    val location: String,
    val vehicleNames: List<String>,
    val cost: Double,
    val createdAt: Long
)

internal data class OperationalTitleParts(
    val title: String,
    val financialLines: List<String>
)

internal data class OperationalInfoGroup(
    val title: String,
    val items: List<Pair<String, String>>,
    val emphasize: Boolean = false
)

internal data class CorporateReservation(
    val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val driverName: String,
    val destination: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val status: String,
    val tripStartedAtMillis: Long? = null,
    val tripEndedAtMillis: Long? = null
)

internal data class CorporateFleetVehicle(
    val id: String,
    val name: String,
    val plate: String = "",
    val model: String = "",
    val status: String = "disponivel",
    val odometerKm: Int = 0
)
