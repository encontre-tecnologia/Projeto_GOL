package br.com.gui.carlembrete

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal fun loadOperationalRecords(context: Context): List<OperationalRecord> {
    val json = context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .getString(OPERATIONAL_RECORDS_KEY, "[]")
        ?: "[]"
    val type = object : TypeToken<List<OperationalRecord>>() {}.type
    return runCatching {
        Gson().fromJson<List<OperationalRecord>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}

internal fun saveOperationalRecords(context: Context, records: List<OperationalRecord>) {
    context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(OPERATIONAL_RECORDS_KEY, Gson().toJson(records))
        .apply()
}

internal fun loadOperationalDrivers(context: Context): List<OperationalDriver> {
    val json = context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .getString(OPERATIONAL_DRIVERS_KEY, "[]")
        ?: "[]"
    val type = object : TypeToken<List<OperationalDriver>>() {}.type
    return runCatching {
        Gson().fromJson<List<OperationalDriver>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}

internal fun saveOperationalDrivers(context: Context, drivers: List<OperationalDriver>) {
    context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(OPERATIONAL_DRIVERS_KEY, Gson().toJson(drivers))
        .apply()
}

internal fun upsertOperationalDriver(
    context: Context,
    drivers: List<OperationalDriver>,
    selectedDriverId: String,
    name: String,
    code: String,
    phone: String,
    salary: Double,
    taxCost: Double,
    defaultCost: Double
): OperationalDriver? {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return null
    val current = loadOperationalDrivers(context).ifEmpty { drivers }
    val existing = current.firstOrNull { it.id == selectedDriverId }
        ?: current.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
    val driver = (existing ?: OperationalDriver()).copy(
        name = cleanName,
        code = code.trim(),
        phone = phone.trim(),
        salary = salary.coerceAtLeast(0.0),
        taxCost = taxCost.coerceAtLeast(0.0),
        defaultCost = defaultCost.coerceAtLeast(0.0),
        createdAt = existing?.createdAt ?: System.currentTimeMillis()
    )
    val updated = if (current.any { it.id == driver.id }) {
        current.map { if (it.id == driver.id) driver else it }
    } else {
        current + driver
    }
    saveOperationalDrivers(context, updated)
    return driver
}

internal fun upsertOperationalReminder(context: Context, record: OperationalRecord) {
    val feature = runCatching { OperationalFeature.valueOf(record.feature) }.getOrNull() ?: return
    if (feature == OperationalFeature.ROUTE_PROFITABILITY) return
    val tipo = when (feature) {
        OperationalFeature.TIRE_ROI -> TipoManutencao.PNEU
        OperationalFeature.PARTS_DURABILITY -> TipoManutencao.MECANICA
        OperationalFeature.ROUTE_PROFITABILITY -> TipoManutencao.OUTROS
    }
    val titlePrefix = when (feature) {
        OperationalFeature.TIRE_ROI -> "Controle pneu"
        OperationalFeature.PARTS_DURABILITY -> "Durabilidade peca"
        OperationalFeature.ROUTE_PROFITABILITY -> "Rota"
    }
    val current = BancoDeDados.carregarLembretes(context)
    val existing = current.firstOrNull { it.operationalRecordId == record.id }
    val reminder = (existing ?: Lembrete(
        id = "operational-${record.id}",
        carroId = record.vehicleId,
        titulo = "$titlePrefix: ${record.name}",
        peca = record.name,
        dataLimite = "",
        kmLimite = "",
        tipo = tipo
    )).copy(
        carroId = record.vehicleId,
        titulo = "$titlePrefix: ${record.name}",
        peca = record.name,
        kmLimite = (record.kmEnd ?: record.kmStart).takeIf { it > 0 }?.toString().orEmpty(),
        tipo = tipo,
        valor = record.cost,
        operationalRecordId = record.id,
        operationalFeature = feature.name,
        operationalBrand = record.brandOrClient,
        operationalPosition = record.positionOrRoute,
        operationalKmStart = record.kmStart.takeIf { it > 0 },
        operationalKmEnd = record.kmEnd
    )
    val updated = if (current.any { it.operationalRecordId == record.id }) {
        current.map { if (it.operationalRecordId == record.id) reminder else it }
    } else {
        current + reminder
    }
    BancoDeDados.salvarLembretes(context, updated)
    AdminUsersSync.syncRemindersSnapshot(updated)
}

internal fun deleteOperationalReminder(context: Context, recordId: String) {
    val current = BancoDeDados.carregarLembretes(context)
    val updated = current.filterNot { it.operationalRecordId == recordId }
    if (updated.size != current.size) {
        BancoDeDados.salvarLembretes(context, updated)
        AdminUsersSync.syncRemindersSnapshot(updated)
    }
}

internal fun buildIntegratedOperationalRecords(
    feature: OperationalFeature,
    vehicles: List<CarroInfo>,
    maintenanceRecords: List<Lembrete>,
    travelTrips: List<OperationalTravelTrip>,
    selectedVehicleId: String
): List<OperationalRecord> {
    val vehicleById = vehicles.associateBy { it.id }
    return when (feature) {
        OperationalFeature.TIRE_ROI -> {
            val source = maintenanceRecords
                .filter { it.tipo == TipoManutencao.PNEU }
                .filter(::isLembreteRealizado)
                .filter { selectedVehicleId.isBlank() || it.carroId == selectedVehicleId }
            source.filter { it.hasOperationalMetadataFor(feature) }
                .map { it.toOperationalMetadataRecord(feature, vehicleById[it.carroId], "Pneu") } +
                source.filterNot { it.hasOperationalMetadataFor(feature) }
                    .toOperationalWearRecords(feature, vehicleById, "Pneu")
        }

        OperationalFeature.PARTS_DURABILITY -> {
            val source = maintenanceRecords
                .filter(::isQuickWearMaintenance)
                .filter(::isLembreteRealizado)
                .filter { selectedVehicleId.isBlank() || it.carroId == selectedVehicleId }
            source.filter { it.hasOperationalMetadataFor(feature) }
                .map { it.toOperationalMetadataRecord(feature, vehicleById[it.carroId], "Peca") } +
                source.filterNot { it.hasOperationalMetadataFor(feature) }
                    .toOperationalWearRecords(feature, vehicleById, "Peca")
        }

        OperationalFeature.ROUTE_PROFITABILITY -> travelTrips
            .filter { trip ->
                selectedVehicleId.isBlank() ||
                    vehicleById[selectedVehicleId]?.displayName()?.let { selectedName ->
                        trip.vehicleNames.any { it.equals(selectedName, ignoreCase = true) }
                    } == true
            }
            .map { trip ->
                OperationalRecord(
                    id = "auto-trip-${trip.id}",
                    feature = feature.name,
                    name = trip.name,
                    brandOrClient = trip.location,
                    vehicleId = selectedVehicleId,
                    vehicle = trip.vehicleNames.joinToString(", ").ifBlank { "Viagem" },
                    positionOrRoute = trip.location.ifBlank { trip.name },
                    kmStart = 0,
                    kmEnd = null,
                    cost = trip.cost,
                    revenue = null,
                    taxPercent = null,
                    createdAt = trip.createdAt
                )
            }
    }
}

internal fun Lembrete.hasOperationalMetadataFor(feature: OperationalFeature): Boolean {
    return operationalFeature == feature.name && operationalRecordId.isNotBlank() && isLembreteRealizado(this)
}

internal fun Lembrete.toOperationalMetadataRecord(
    feature: OperationalFeature,
    vehicle: CarroInfo?,
    fallbackName: String
): OperationalRecord {
    return OperationalRecord(
        id = operationalRecordId,
        feature = feature.name,
        name = peca.ifBlank { titulo.ifBlank { fallbackName } },
        brandOrClient = operationalBrand.ifBlank { "Registro do app" },
        vehicleId = carroId,
        vehicle = vehicle?.displayName().orEmpty().ifBlank { "Veiculo" },
        positionOrRoute = operationalPosition.ifBlank { tipo.label },
        kmStart = operationalKmStart ?: kmFromText(kmLimite) ?: vehicle?.kmAtual ?: 0,
        kmEnd = operationalKmEnd,
        cost = valor,
        revenue = null,
        taxPercent = null
    )
}

internal fun List<Lembrete>.toOperationalWearRecords(
    feature: OperationalFeature,
    vehicleById: Map<String, CarroInfo>,
    fallbackName: String
): List<OperationalRecord> {
    return groupBy { "${it.carroId}|${wearKey(it)}" }
        .flatMap { (_, group) ->
            val ordered = group.sortedBy { kmFromText(it.kmLimite) ?: Int.MAX_VALUE }
            if (ordered.size >= 2) {
                ordered.zipWithNext().mapNotNull { (start, end) ->
                    val startKm = kmFromText(start.kmLimite) ?: return@mapNotNull null
                    val endKm = kmFromText(end.kmLimite) ?: return@mapNotNull null
                    if (endKm <= startKm) return@mapNotNull null
                    end.toOperationalWearRecord(
                        feature = feature,
                        vehicle = vehicleById[end.carroId],
                        fallbackName = fallbackName,
                        kmStart = startKm,
                        kmEnd = endKm,
                        idSuffix = "${start.id}-${end.id}"
                    )
                }
            } else {
                ordered.map { item ->
                    item.toOperationalWearRecord(
                        feature = feature,
                        vehicle = vehicleById[item.carroId],
                        fallbackName = fallbackName,
                        kmStart = kmFromText(item.kmLimite) ?: vehicleById[item.carroId]?.kmAtual ?: 0,
                        kmEnd = null,
                        idSuffix = item.id
                    )
                }
            }
        }
}

internal fun Lembrete.toOperationalWearRecord(
    feature: OperationalFeature,
    vehicle: CarroInfo?,
    fallbackName: String,
    kmStart: Int,
    kmEnd: Int?,
    idSuffix: String
): OperationalRecord {
    return OperationalRecord(
        id = "auto-maint-${feature.name}-$idSuffix",
        feature = feature.name,
        name = peca.ifBlank { titulo.ifBlank { fallbackName } },
        brandOrClient = "Historico de manutencao",
        vehicleId = carroId,
        vehicle = vehicle?.displayName().orEmpty().ifBlank { "Veiculo" },
        positionOrRoute = tipo.label,
        kmStart = kmStart,
        kmEnd = kmEnd,
        cost = valor,
        revenue = null,
        taxPercent = null
    )
}

internal fun isQuickWearMaintenance(item: Lembrete): Boolean {
    val text = "${item.titulo} ${item.peca} ${item.tipo.label}".lowercase(Locale.ROOT)
    val keywordMatch = listOf("pastilha", "freio", "pivo", "pivô", "terminal", "suspens", "bucha", "amortec").any { text.contains(it) }
    return item.tipo == TipoManutencao.FREIO || item.tipo == TipoManutencao.MECANICA || keywordMatch
}

internal fun wearKey(item: Lembrete): String {
    return "${item.peca.ifBlank { item.titulo }.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }}|${item.tipo.name}"
}

internal fun kmFromText(value: String): Int? = value.filter { it.isDigit() }.toIntOrNull()

internal fun loadOperationalTravelTrips(context: Context): List<OperationalTravelTrip> {
    val raw = context.getSharedPreferences("travel_expenses_prefs", Context.MODE_PRIVATE)
        .getString("travel_trips_json", null)
        ?: return emptyList()
    return runCatching {
        val tripsArray = JSONArray(raw)
        buildList {
            for (i in 0 until tripsArray.length()) {
                val tripObj = tripsArray.getJSONObject(i)
                val expensesArray = tripObj.optJSONArray("expenses") ?: JSONArray()
                var cost = 0.0
                val vehicles = linkedSetOf<String>()
                for (j in 0 until expensesArray.length()) {
                    val expense = expensesArray.getJSONObject(j)
                    val original = expense.optDouble("originalAmount", expense.optDouble("amount", 0.0))
                    val discount = expense.optDouble("discountAmount", 0.0)
                    val amount = expense.optDouble("amount", 0.0)
                    cost += if (discount > 0.0) (original - discount).coerceAtLeast(0.0) else amount
                    expense.optString("vehicleName").takeIf { it.isNotBlank() }?.let(vehicles::add)
                }
                add(
                    OperationalTravelTrip(
                        id = tripObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = tripObj.optString("name").ifBlank { "Minha viagem" },
                        location = tripObj.optString("location"),
                        vehicleNames = vehicles.toList(),
                        cost = cost,
                        createdAt = tripObj.optLong("createdAtMillis").takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

internal fun fuelCostPerKm(vehicleId: String, fuelRecords: List<Abastecimento>): Double {
    val records = fuelRecords.filter { it.carroId == vehicleId && it.km != null }.sortedBy { it.km }
    val firstKm = records.firstOrNull()?.km ?: return 0.0
    val lastKm = records.lastOrNull()?.km ?: return 0.0
    val distance = lastKm - firstKm
    if (distance <= 0) return 0.0
    return records.drop(1).sumOf { it.valorPago } / distance
}

internal fun maintenanceCostPerKm(vehicleId: String, maintenanceRecords: List<Lembrete>): Double {
    val records = maintenanceRecords.filter { it.carroId == vehicleId && it.valor > 0.0 }
    val maxKm = records.mapNotNull { kmFromText(it.kmLimite) }.maxOrNull() ?: return 0.0
    if (maxKm <= 0) return 0.0
    return records.sumOf { it.valor } / maxKm
}

internal fun operationalWearCostPerKm(vehicleId: String, operationalRecords: List<OperationalRecord>): Double {
    return operationalRecords
        .filter { it.vehicleId == vehicleId }
        .filter {
            it.feature == OperationalFeature.TIRE_ROI.name ||
                it.feature == OperationalFeature.PARTS_DURABILITY.name
        }
        .filter { it.kmEnd != null && it.kmEnd > it.kmStart && it.cost > 0.0 }
        .groupBy { operationalReplacementKey(it) }
        .values
        .sumOf { group ->
            group.minOfOrNull { costPerKm(it) } ?: 0.0
        }
}

internal fun routeRealCostPerKm(
    vehicleId: String,
    fuelRecords: List<Abastecimento>,
    maintenanceRecords: List<Lembrete>,
    operationalRecords: List<OperationalRecord>
): Double {
    return fuelCostPerKm(vehicleId, fuelRecords) +
        maintenanceCostPerKm(vehicleId, maintenanceRecords) +
        operationalWearCostPerKm(vehicleId, operationalRecords)
}

internal fun estimateRouteCost(
    vehicleId: String,
    distanceKm: Int,
    fuelRecords: List<Abastecimento>,
    maintenanceRecords: List<Lembrete>,
    operationalRecords: List<OperationalRecord>
): Double {
    val costPerKm = routeRealCostPerKm(vehicleId, fuelRecords, maintenanceRecords, operationalRecords)
    return costPerKm * distanceKm
}

internal fun CarroInfo.displayName(): String {
    return listOf(nome, marca, modelo)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "Veiculo" }
}

internal fun CorporateFleetVehicle.displayName(): String {
    return listOf(name, plate, model)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" - ")
        .ifBlank { "Veiculo" }
}

internal fun formatPlainDecimal(value: Double): String {
    return "%.2f".format(Locale.US, value).replace(".", ",")
}

internal fun currentOperationalDate(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
}

internal fun splitOperationalTitle(raw: String): OperationalTitleParts {
    val lines = raw
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lines.isEmpty()) return OperationalTitleParts(title = "Registro", financialLines = emptyList())

    val financialPrefixes = listOf(
        "valor total",
        "desconto",
        "valor final",
        "total",
        "valor"
    )
    val financialLines = lines.filter { line ->
        val normalized = line.lowercase(Locale.ROOT)
        financialPrefixes.any { normalized.startsWith(it) }
    }
    val title = lines.firstOrNull { line ->
        val normalized = line.lowercase(Locale.ROOT)
        financialPrefixes.none { normalized.startsWith(it) }
    } ?: lines.first()

    return OperationalTitleParts(title = title, financialLines = financialLines)
}

internal fun keepNumericInput(value: String): String = value.filter { it.isDigit() }

internal fun operationalReplacementKey(record: OperationalRecord): String {
    val feature = runCatching { OperationalFeature.valueOf(record.feature) }.getOrNull()
    val position = record.positionOrRoute.normalizedOperationalKey()
    val name = record.name.normalizedOperationalKey()
    return when (feature) {
        OperationalFeature.TIRE_ROI -> position.ifBlank { name }
        OperationalFeature.PARTS_DURABILITY -> listOf(name, position)
            .filter { it.isNotBlank() }
            .joinToString("|")
            .ifBlank { name.ifBlank { position } }
        else -> position.ifBlank { name }
    }
}

internal fun String.normalizedOperationalKey(): String {
    return lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
}

internal fun keepDecimalInput(value: String): String {
    var hasSeparator = false
    return value.filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !hasSeparator -> {
                hasSeparator = true
                true
            }
            else -> false
        }
    }
}

internal fun formatDriverPhoneInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(13)
    val hasCountryCode = digits.startsWith("55") && digits.length > 11
    val localDigits = if (hasCountryCode) digits.drop(2).take(11) else digits.take(11)
    val formattedLocal = when {
        localDigits.length <= 2 -> localDigits
        localDigits.length <= 6 -> "(${localDigits.take(2)}) ${localDigits.drop(2)}"
        localDigits.length <= 10 -> {
            val area = localDigits.take(2)
            val prefix = localDigits.drop(2).take(4)
            val suffix = localDigits.drop(6)
            "($area) $prefix-$suffix"
        }
        else -> {
            val area = localDigits.take(2)
            val prefix = localDigits.drop(2).take(5)
            val suffix = localDigits.drop(7)
            "($area) $prefix-$suffix"
        }
    }
    return if (hasCountryCode && formattedLocal.isNotBlank()) "+55 $formattedLocal" else formattedLocal
}

internal fun parseMoneyInput(value: String): Double? {
    val normalized = value.replace(".", "").replace(",", ".")
    return normalized.toDoubleOrNull()
}

internal fun costPerKm(record: OperationalRecord): Double {
    val distance = (record.kmEnd ?: record.kmStart) - record.kmStart
    return if (distance > 0) record.cost / distance else 0.0
}

internal fun routeTotalCost(record: OperationalRecord): Double {
    return record.cost + record.driverCost.coerceAtLeast(0.0)
}

internal fun routeProfit(record: OperationalRecord): Double {
    val revenue = record.revenue ?: 0.0
    val tax = revenue * ((record.taxPercent ?: 0.0) / 100.0)
    return revenue - routeTotalCost(record) - tax
}

internal fun routeMargin(record: OperationalRecord): Double {
    val revenue = record.revenue ?: 0.0
    return if (revenue > 0) (routeProfit(record) / revenue) * 100.0 else 0.0
}

internal fun formatMoney(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

internal fun operationalReportTitle(feature: OperationalFeature): String = when (feature) {
    OperationalFeature.TIRE_ROI -> trNow("Relatorio Controle de Pneus", "Tire Tracking Report")
    OperationalFeature.PARTS_DURABILITY -> trNow("Relatorio Durabilidade de Pecas", "Parts Durability Report")
    OperationalFeature.ROUTE_PROFITABILITY -> trNow("Relatorio Rentabilidade de Rotas", "Route Profitability Report")
}

internal fun operationalReportFileSlug(feature: OperationalFeature): String = when (feature) {
    OperationalFeature.TIRE_ROI -> "controle_pneus"
    OperationalFeature.PARTS_DURABILITY -> "durabilidade_pecas"
    OperationalFeature.ROUTE_PROFITABILITY -> "rentabilidade_rotas"
}
