package br.com.gui.carlembrete

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
internal fun OperationalFeatureScreen(
    feature: OperationalFeature,
    onDismiss: () -> Unit,
    screenBg: Color,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color
) {
    val context = LocalContext.current
    val icon = when (feature) {
        OperationalFeature.TIRE_ROI -> Icons.Default.TireRepair
        OperationalFeature.PARTS_DURABILITY -> Icons.Default.Build
        OperationalFeature.ROUTE_PROFITABILITY -> Icons.Default.Route
    }
    val title = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Controle de pneus", "Tire tracking")
        OperationalFeature.PARTS_DURABILITY -> tr("Durabilidade de pecas", "Parts durability")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Rentabilidade de rotas fixas", "Fixed-route profitability")
    }
    val subtitle = when (feature) {
        OperationalFeature.TIRE_ROI -> tr(
            "Controle marca, posicao e quilometragem para descobrir qual pneu entrega mais retorno.",
            "Track brand, position and mileage to learn which tire gives the best return."
        )
        OperationalFeature.PARTS_DURABILITY -> tr(
            "Compare fabricantes de pecas de desgaste usando o KM real de troca.",
            "Compare wear-part makers using real replacement mileage."
        )
        OperationalFeature.ROUTE_PROFITABILITY -> tr(
            "Cadastre custo, imposto e valor cobrado para ver lucro ou prejuizo na hora.",
            "Log cost, tax and charged price to see profit or loss instantly."
        )
    }
    val integrationText = when (feature) {
        OperationalFeature.TIRE_ROI -> tr(
            "Integrado com veiculos cadastrados e manutencoes de pneu.",
            "Integrated with registered vehicles and tire maintenance."
        )
        OperationalFeature.PARTS_DURABILITY -> tr(
            "Integrado com avisos de freio, mecanica e pecas de desgaste.",
            "Integrated with brake, mechanic and wear-part reminders."
        )
        OperationalFeature.ROUTE_PROFITABILITY -> tr(
            "Integrado com veiculos, abastecimentos, manutencoes e viagens salvas.",
            "Integrated with vehicles, fuel, maintenance and saved trips."
        )
    }
    val isRoute = feature == OperationalFeature.ROUTE_PROFITABILITY
    val vehicles = remember(feature) { BancoDeDados.carregarCarros(context).orEmpty() }
    val maintenanceRecords = remember(feature) { BancoDeDados.carregarLembretes(context) }
    val fuelRecords = remember(feature) { BancoDeDados.carregarAbastecimentos(context) }
    val travelTrips = remember(feature) { loadOperationalTravelTrips(context) }
    var records by remember(feature) {
        mutableStateOf(loadOperationalRecords(context).filter { it.feature == feature.name })
    }
    var drivers by remember(feature) { mutableStateOf(loadOperationalDrivers(context)) }
    var selectedVehicleId by remember(feature) { mutableStateOf(vehicles.firstOrNull()?.id.orEmpty()) }
    val importedRecords = remember(feature, selectedVehicleId, maintenanceRecords, travelTrips) {
        buildIntegratedOperationalRecords(
            feature = feature,
            vehicles = vehicles,
            maintenanceRecords = maintenanceRecords,
            travelTrips = travelTrips,
            selectedVehicleId = selectedVehicleId
        )
    }
    val visibleRecords = remember(records, importedRecords) {
        importedRecords.filterNot { imported -> records.any { it.id == imported.id } } + records
    }
    var showRegistrationScreen by remember(feature) { mutableStateOf(false) }
    var editingRecordId by remember(feature) { mutableStateOf<String?>(null) }
    var name by remember(feature) { mutableStateOf("") }
    var brandOrClient by remember(feature) { mutableStateOf("") }
    var vehicle by remember(feature) { mutableStateOf(vehicles.firstOrNull()?.displayName().orEmpty()) }
    var positionOrRoute by remember(feature) { mutableStateOf("") }
    var kmStart by remember(feature) {
        mutableStateOf(if (!isRoute) vehicles.firstOrNull()?.kmAtual?.takeIf { it > 0 }?.toString().orEmpty() else "")
    }
    val routeSuggestedCost = remember(selectedVehicleId, kmStart, fuelRecords, maintenanceRecords, records) {
        val distance = kmStart.toIntOrNull() ?: 0
        if (isRoute && selectedVehicleId.isNotBlank() && distance > 0) {
            estimateRouteCost(selectedVehicleId, distance, fuelRecords, maintenanceRecords, loadOperationalRecords(context))
        } else {
            0.0
        }
    }
    var kmEnd by remember(feature) { mutableStateOf("") }
    var cost by remember(feature) { mutableStateOf("") }
    var quantity by remember(feature) { mutableStateOf("1") }
    var recordDate by remember(feature) { mutableStateOf(currentOperationalDate()) }
    var revenue by remember(feature) { mutableStateOf("") }
    var taxPercent by remember(feature) { mutableStateOf("") }
    var selectedDriverId by remember(feature) { mutableStateOf("") }
    var driverName by remember(feature) { mutableStateOf("") }
    var driverCode by remember(feature) { mutableStateOf("") }
    var driverPhone by remember(feature) { mutableStateOf("") }
    var driverSalary by remember(feature) { mutableStateOf("") }
    var driverTaxCost by remember(feature) { mutableStateOf("") }
    var driverCost by remember(feature) { mutableStateOf("") }
    var showReportOptions by remember(feature) { mutableStateOf(false) }
    var showDriversManager by remember(feature) { mutableStateOf(false) }

    val nameLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Pneu/modelo", "Tire/model")
        OperationalFeature.PARTS_DURABILITY -> tr("Peca", "Part")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Nome da linha", "Line name")
    }
    val brandLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Marca do pneu", "Tire brand")
        OperationalFeature.PARTS_DURABILITY -> tr("Marca/fabricante", "Brand/manufacturer")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Cliente", "Client")
    }
    val positionLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Posicao da roda, ex: R1", "Wheel position, ex: R1")
        OperationalFeature.PARTS_DURABILITY -> tr("Local/observacao", "Location/note")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Origem -> destino", "Origin -> destination")
    }
    val kmStartLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("KM instalacao", "Install mileage")
        OperationalFeature.PARTS_DURABILITY -> tr("KM instalado", "Install mileage")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Distancia media da viagem (km)", "Average trip distance (km)")
    }
    val kmEndLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("KM retirada", "Removal mileage")
        OperationalFeature.PARTS_DURABILITY -> tr("KM nova troca", "New replacement mileage")
        OperationalFeature.ROUTE_PROFITABILITY -> ""
    }
    val costLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Custo do pneu", "Tire cost")
        OperationalFeature.PARTS_DURABILITY -> tr("Custo da peca", "Part cost")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Custo operacional da viagem", "Operational trip cost")
    }
    val registrationTitle = when (feature) {
        OperationalFeature.TIRE_ROI -> if (editingRecordId == null) tr("Cadastrar pneu", "Add tire") else tr("Editar pneu", "Edit tire")
        OperationalFeature.PARTS_DURABILITY -> if (editingRecordId == null) tr("Cadastrar peca", "Add part") else tr("Editar peca", "Edit part")
        OperationalFeature.ROUTE_PROFITABILITY -> if (editingRecordId == null) tr("Cadastrar linha", "Add route") else tr("Editar linha", "Edit route")
    }
    val saveButtonText = if (editingRecordId == null) tr("Salvar e calcular", "Save and calculate") else tr("Salvar edicao", "Save changes")
    val requiredToast = tr("Preencha nome, veiculo, KM e valores.", "Fill name, vehicle, mileage and values.")
    val invalidKmToast = tr("KM final precisa ser maior que o KM inicial.", "Final mileage must be greater than initial mileage.")
    val savedToast = tr("Registro salvo. Agora sim, tela com musculo.", "Record saved.")
    val updatedToast = tr("Registro atualizado.", "Record updated.")
    val deletedToast = tr("Registro removido.", "Record removed.")
    val integratedRecordToast = tr("Esse veio dos dados ja existentes.", "This came from existing data.")
    val kmEndSavedToast = tr("KM final salvo e calculado.", "Final mileage saved.")

    fun resetOperationalForm() {
        name = ""
        brandOrClient = ""
        positionOrRoute = ""
        kmStart = if (!isRoute) {
            vehicles.firstOrNull { it.id == selectedVehicleId }?.kmAtual?.takeIf { it > 0 }?.toString().orEmpty()
        } else {
            ""
        }
        kmEnd = ""
        cost = ""
        quantity = "1"
        recordDate = currentOperationalDate()
        revenue = ""
        taxPercent = ""
        selectedDriverId = ""
        driverName = ""
        driverCode = ""
        driverPhone = ""
        driverSalary = ""
        driverTaxCost = ""
        driverCost = ""
    }

    fun closeRegistrationScreen() {
        showRegistrationScreen = false
        editingRecordId = null
        resetOperationalForm()
    }

    fun openCreateRegistration() {
        editingRecordId = null
        resetOperationalForm()
        showRegistrationScreen = true
    }

    fun openDriverDialog() {
        showDriversManager = true
    }

    fun refreshSelectedDriver(driver: OperationalDriver?) {
        if (driver == null) {
            selectedDriverId = ""
            driverName = ""
            driverCode = ""
            driverPhone = ""
            driverSalary = ""
            driverTaxCost = ""
            driverCost = ""
            return
        }
        selectedDriverId = driver.id
        driverName = driver.name
        driverCode = driver.code
        driverPhone = driver.phone
        driverSalary = driver.salary.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        driverTaxCost = driver.taxCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        if (driverCost.isBlank()) {
            driverCost = driver.defaultCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        }
    }

    fun saveDriverFromManager(
        editingDriverId: String?,
        editedName: String,
        editedCode: String,
        editedPhone: String,
        editedSalary: String,
        editedTaxCost: String,
        editedDefaultCost: String
    ): Boolean {
        val savedDriver = upsertOperationalDriver(
            context = context,
            drivers = drivers,
            selectedDriverId = editingDriverId.orEmpty(),
            name = editedName,
            code = editedCode,
            phone = editedPhone,
            salary = parseMoneyInput(editedSalary) ?: 0.0,
            taxCost = parseMoneyInput(editedTaxCost) ?: 0.0,
            defaultCost = parseMoneyInput(editedDefaultCost) ?: 0.0
        ) ?: run {
            Toast.makeText(context, trNow("Informe o nome do motorista.", "Enter the driver name."), Toast.LENGTH_SHORT).show()
            return false
        }
        val currentAll = loadOperationalRecords(context)
        val updatedAll = currentAll.map { record ->
            if (record.driverId == savedDriver.id) {
                record.copy(driverName = savedDriver.name)
            } else {
                record
            }
        }
        saveOperationalRecords(context, updatedAll)
        drivers = loadOperationalDrivers(context)
        records = updatedAll.filter { it.feature == feature.name }
        if (selectedDriverId == savedDriver.id || editingDriverId == null) {
            refreshSelectedDriver(savedDriver)
        }
        Toast.makeText(context, trNow("Motorista salvo.", "Driver saved."), Toast.LENGTH_SHORT).show()
        return true
    }

    fun deleteDriverFromManager(driver: OperationalDriver) {
        val currentAll = loadOperationalRecords(context)
        val updatedAll = currentAll.map { record ->
            if (record.driverId == driver.id) {
                record.copy(driverId = "", driverName = "", driverCost = 0.0)
            } else {
                record
            }
        }
        saveOperationalRecords(context, updatedAll)
        saveOperationalDrivers(context, loadOperationalDrivers(context).filterNot { it.id == driver.id })
        drivers = loadOperationalDrivers(context)
        records = updatedAll.filter { it.feature == feature.name }
        if (selectedDriverId == driver.id) {
            refreshSelectedDriver(null)
        }
        Toast.makeText(context, trNow("Motorista removido.", "Driver removed."), Toast.LENGTH_SHORT).show()
    }

    fun openEditRegistration(record: OperationalRecord) {
        editingRecordId = record.id
        selectedVehicleId = record.vehicleId
        vehicle = record.vehicle
        name = splitOperationalTitle(record.name).title
        brandOrClient = record.brandOrClient
        positionOrRoute = record.positionOrRoute
        kmStart = record.kmStart.takeIf { it > 0 }?.toString().orEmpty()
        kmEnd = record.kmEnd?.toString().orEmpty()
        cost = formatPlainDecimal(record.cost)
        quantity = record.quantity.coerceAtLeast(1).toString()
        recordDate = record.recordDate.ifBlank { currentOperationalDate() }
        revenue = record.revenue?.let { formatPlainDecimal(it) }.orEmpty()
        taxPercent = record.taxPercent?.let { formatPlainDecimal(it) }.orEmpty()
        selectedDriverId = record.driverId
        val selectedDriver = drivers.firstOrNull { it.id == record.driverId }
        driverName = record.driverName.ifBlank { selectedDriver?.name.orEmpty() }
        driverCode = selectedDriver?.code.orEmpty()
        driverPhone = selectedDriver?.phone.orEmpty()
        driverSalary = selectedDriver?.salary?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        driverTaxCost = selectedDriver?.taxCost?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        driverCost = (record.driverCost.takeIf { it > 0.0 } ?: selectedDriver?.defaultCost)
            ?.let { formatPlainDecimal(it) }
            .orEmpty()
        showRegistrationScreen = true
    }

    BackHandler(enabled = showRegistrationScreen) {
        closeRegistrationScreen()
    }

    BackHandler(enabled = showDriversManager) {
        showDriversManager = false
    }

    LaunchedEffect(selectedVehicleId, feature) {
        val picked = vehicles.firstOrNull { it.id == selectedVehicleId }
        if (picked != null) {
            vehicle = picked.displayName()
            if (!isRoute && kmStart.isBlank() && picked.kmAtual > 0) {
                kmStart = picked.kmAtual.toString()
            }
        }
    }

    fun saveOperationalRecord() {
        val kmStartValue = kmStart.toIntOrNull()
        val kmEndValue = kmEnd.toIntOrNull()
        val costValue = parseMoneyInput(cost)
        val quantityValue = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val revenueValue = parseMoneyInput(revenue)
        val taxValue = parseMoneyInput(taxPercent)
        val driverCostValue = parseMoneyInput(driverCost) ?: 0.0
        val missingRequired = name.isBlank() || vehicle.isBlank() || kmStartValue == null || costValue == null ||
            (isRoute && revenueValue == null)
        val invalidKmRange = !isRoute && kmEndValue != null && kmEndValue <= (kmStartValue ?: 0)

        when {
            missingRequired -> Toast.makeText(context, requiredToast, Toast.LENGTH_SHORT).show()
            invalidKmRange -> Toast.makeText(context, invalidKmToast, Toast.LENGTH_SHORT).show()
            else -> {
                val editingId = editingRecordId
                val currentAll = loadOperationalRecords(context)
                val currentRecord = editingId?.let { id -> currentAll.firstOrNull { it.id == id } }
                val driverForRecord = if (isRoute && selectedDriverId.isNotBlank()) {
                    drivers.firstOrNull { it.id == selectedDriverId } ?: loadOperationalDrivers(context).firstOrNull { it.id == selectedDriverId }
                } else {
                    null
                }
                val newRecord = OperationalRecord(
                    id = editingId ?: UUID.randomUUID().toString(),
                    feature = feature.name,
                    name = name.trim(),
                    brandOrClient = brandOrClient.trim(),
                    vehicleId = selectedVehicleId,
                    vehicle = vehicle.trim(),
                    positionOrRoute = positionOrRoute.trim(),
                    kmStart = kmStartValue ?: 0,
                    kmEnd = if (isRoute) null else kmEndValue,
                    cost = costValue ?: 0.0,
                    quantity = quantityValue,
                    recordDate = recordDate.ifBlank { currentOperationalDate() },
                    revenue = if (isRoute) revenueValue else null,
                    taxPercent = if (isRoute) taxValue ?: 0.0 else null,
                    driverId = driverForRecord?.id.orEmpty(),
                    driverName = driverForRecord?.name.orEmpty(),
                    driverCost = if (driverForRecord != null) driverCostValue else 0.0,
                    createdAt = currentRecord?.createdAt ?: System.currentTimeMillis()
                )
                if (editingId != null) {
                    val updatedAll = if (currentAll.any { it.id == editingId }) {
                        currentAll.map { if (it.id == editingId) newRecord else it }
                    } else {
                        currentAll + newRecord
                    }
                    saveOperationalRecords(context, updatedAll)
                    upsertOperationalReminder(context, newRecord)
                    records = updatedAll.filter { it.feature == feature.name }
                    closeRegistrationScreen()
                    Toast.makeText(context, updatedToast, Toast.LENGTH_SHORT).show()
                    return
                }
                val previousOpen = if (!isRoute) {
                    currentAll
                        .filter { it.feature == feature.name }
                        .filter { it.vehicleId == newRecord.vehicleId }
                        .filter { it.kmEnd == null }
                        .filter { it.kmStart < newRecord.kmStart }
                        .filter { operationalReplacementKey(it) == operationalReplacementKey(newRecord) }
                        .maxByOrNull { it.kmStart }
                } else {
                    null
                }
                val closedPrevious = previousOpen?.copy(kmEnd = newRecord.kmStart, createdAt = System.currentTimeMillis())
                val updatedAll = currentAll
                    .map { if (closedPrevious != null && it.id == closedPrevious.id) closedPrevious else it } +
                    newRecord
                saveOperationalRecords(context, updatedAll)
                if (closedPrevious != null) upsertOperationalReminder(context, closedPrevious)
                upsertOperationalReminder(context, newRecord)
                records = updatedAll.filter { it.feature == feature.name }
                closeRegistrationScreen()
                Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleOperationalReport() {
        val realCostPerKm = selectedVehicleId.takeIf { it.isNotBlank() }?.let {
            routeRealCostPerKm(it, fuelRecords, maintenanceRecords, loadOperationalRecords(context))
        } ?: 0.0
        val pdf = generateOperationalReportPdf(
            context = context,
            feature = feature,
            records = visibleRecords,
            vehiclesCount = vehicles.size,
            importedRecordsCount = importedRecords.size,
            realCostPerKm = realCostPerKm,
            routeSuggestedCost = routeSuggestedCost
        )
        if (pdf == null) {
            Toast.makeText(
                context,
                trNow("Nao foi possivel gerar o PDF.", "Could not generate the PDF."),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        shareOperationalPdf(context, pdf)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        if (showReportOptions) {
            OperationalReportOptionsDialog(
                bg = screenBg,
                textPrimary = titleColor,
                cardBorder = cardBorder,
                accentBlue = Color(0xFF2563EB),
                onExportPdf = {
                    showReportOptions = false
                    handleOperationalReport()
                },
                onDismiss = { showReportOptions = false }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
                }
                IconButton(
                    onClick = { showReportOptions = true },
                    modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = tr("Relatorio", "Report"),
                        tint = titleColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isRoute) {
                    IconButton(
                        onClick = { showDriversManager = true },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 44.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = tr("Gerenciar motoristas", "Manage drivers"),
                            tint = titleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(27.dp))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(title, color = titleColor, fontWeight = FontWeight.Black, fontSize = 19.sp, textAlign = TextAlign.Center)
                    Text(subtitle, color = subColor, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF16A34A).copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                        Text(integrationText, color = Color(0xFF047857), fontSize = 11.sp, lineHeight = 14.sp)
                    }
                    Text(
                        text = tr("Disponivel no Plano Frota", "Available on Fleet plan"),
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = { openCreateRegistration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF357AE8),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(tr("Cadastrar", "Add"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OperationalSummaryCard(
                feature = feature,
                records = visibleRecords,
                vehiclesCount = vehicles.size,
                driversCount = drivers.size,
                importedRecordsCount = importedRecords.size,
                realCostPerKm = selectedVehicleId.takeIf { it.isNotBlank() }?.let {
                    routeRealCostPerKm(it, fuelRecords, maintenanceRecords, loadOperationalRecords(context))
                } ?: 0.0,
                routeSuggestedCost = routeSuggestedCost,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor
            )

            Text(
                text = tr("Historico", "History"),
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            )

            if (visibleRecords.isEmpty()) {
                EmptyOperationalState(
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subColor = subColor
                )
            } else {
                visibleRecords.sortedByDescending { it.createdAt }.forEach { record ->
                    val canManageRecord = records.any { it.id == record.id } || !record.id.startsWith("auto-")
                    OperationalRecordCard(
                        feature = feature,
                        record = record,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        titleColor = titleColor,
                        subColor = subColor,
                        canDelete = canManageRecord,
                        onEdit = if (canManageRecord) {
                            { openEditRegistration(record) }
                        } else {
                            null
                        },
                        onUpdateKmEnd = if (feature != OperationalFeature.ROUTE_PROFITABILITY) {
                            { finalKm ->
                                val updatedRecord = record.copy(kmEnd = finalKm, createdAt = System.currentTimeMillis())
                                val current = loadOperationalRecords(context)
                                val updatedAll = if (current.any { it.id == record.id }) {
                                    current.map { if (it.id == record.id) updatedRecord else it }
                                } else {
                                    current + updatedRecord
                                }
                                saveOperationalRecords(context, updatedAll)
                                upsertOperationalReminder(context, updatedRecord)
                                records = updatedAll.filter { it.feature == feature.name }
                                Toast.makeText(context, kmEndSavedToast, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            null
                        },
                        onDelete = {
                            if (record.id.startsWith("auto-") && records.none { it.id == record.id }) {
                                Toast.makeText(context, integratedRecordToast, Toast.LENGTH_SHORT).show()
                                return@OperationalRecordCard
                            }
                            val updatedAll = loadOperationalRecords(context).filterNot { it.id == record.id }
                            saveOperationalRecords(context, updatedAll)
                            deleteOperationalReminder(context, record.id)
                            records = updatedAll.filter { it.feature == feature.name }
                            Toast.makeText(context, deletedToast, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        if (showRegistrationScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { closeRegistrationScreen() },
                        modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(30.dp))
                    }
                    Text(
                        registrationTitle,
                        color = titleColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OperationalFormSection(
                        title = if (isRoute) tr("Linha e cliente", "Line and client") else tr("Produto", "Product"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        if (feature == OperationalFeature.PARTS_DURABILITY) {
                            PartTypePickerField(
                                value = name,
                                onValueChange = { name = it },
                                label = nameLabel
                            )
                        } else {
                            OperationalTextField(value = name, onValueChange = { name = it }, label = nameLabel)
                        }
                        OperationalTextField(value = brandOrClient, onValueChange = { brandOrClient = it }, label = brandLabel)
                    }

                    OperationalFormSection(
                        title = tr("Veiculo", "Vehicle"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        VehiclePickerField(
                            vehicles = vehicles,
                            selectedVehicleId = selectedVehicleId,
                            onSelect = { picked ->
                                selectedVehicleId = picked.id
                                vehicle = picked.displayName()
                                if (!isRoute && picked.kmAtual > 0) kmStart = picked.kmAtual.toString()
                            },
                            fallbackValue = vehicle,
                            onFallbackChange = { vehicle = it },
                            label = tr("Veiculo", "Vehicle")
                        )
                    }

                    if (isRoute) {
                        OperationalFormSection(
                            title = tr("Motorista", "Driver"),
                            cardBorder = cardBorder,
                            subColor = subColor
                        ) {
                            DriverPickerField(
                                drivers = drivers,
                                selectedDriverId = selectedDriverId,
                                onSelect = { picked ->
                                    selectedDriverId = picked.id
                                    driverName = picked.name
                                    driverCode = picked.code
                                    driverPhone = picked.phone
                                    driverSalary = picked.salary.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
                                    driverTaxCost = picked.taxCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
                                    driverCost = picked.defaultCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
                                },
                                onClear = {
                                    selectedDriverId = ""
                                    driverName = ""
                                    driverCode = ""
                                    driverPhone = ""
                                    driverSalary = ""
                                    driverTaxCost = ""
                                    driverCost = ""
                                },
                                onRequestAdd = { openDriverDialog() },
                                label = tr("Motorista da linha", "Route driver")
                            )
                            val hasSelectedDriver = selectedDriverId.isNotBlank()
                            OperationalTextField(
                                value = driverCode,
                                onValueChange = {},
                                label = tr("Codigo do motorista", "Driver code"),
                                enabled = false
                            )
                            OperationalTextField(
                                value = driverName,
                                onValueChange = {},
                                label = tr("Nome do motorista", "Driver name"),
                                enabled = false
                            )
                            OperationalTextField(
                                value = driverPhone,
                                onValueChange = {},
                                label = tr("Telefone/observacao", "Phone/note"),
                                enabled = false
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OperationalTextField(
                                    value = driverSalary,
                                    onValueChange = {},
                                    label = tr("Salario", "Salary"),
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                )
                                OperationalTextField(
                                    value = driverTaxCost,
                                    onValueChange = {},
                                    label = tr("Impostos/custos", "Taxes/costs"),
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OperationalTextField(
                                value = driverCost,
                                onValueChange = { driverCost = keepDecimalInput(it) },
                                label = tr("Custo do motorista por linha", "Driver cost per route"),
                                keyboardType = KeyboardType.Decimal,
                                enabled = hasSelectedDriver
                            )
                        }
                    }

                    OperationalFormSection(
                        title = if (isRoute) tr("Rota", "Route") else tr("Instalacao", "Install"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        OperationalTextField(value = positionOrRoute, onValueChange = { positionOrRoute = it }, label = positionLabel)
                        if (feature == OperationalFeature.TIRE_ROI) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OperationalTextField(
                                    value = quantity,
                                    onValueChange = { quantity = keepNumericInput(it).take(3).ifBlank { "1" } },
                                    label = tr("Quantidade", "Quantity"),
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                                OperationalTextField(
                                    value = recordDate,
                                    onValueChange = { recordDate = it.take(10) },
                                    label = tr("Data", "Date"),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        OperationalTextField(
                            value = kmStart,
                            onValueChange = { kmStart = keepNumericInput(it) },
                            label = kmStartLabel,
                            keyboardType = KeyboardType.Number
                        )
                    }

                    if (!isRoute) {
                        OperationalFormSection(
                            title = tr("Retirada", "Removal"),
                            cardBorder = cardBorder,
                            subColor = subColor
                        ) {
                            OperationalTextField(
                                value = kmEnd,
                                onValueChange = { kmEnd = keepNumericInput(it) },
                                label = kmEndLabel,
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    OperationalFormSection(
                        title = tr("Valores", "Values"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        OperationalTextField(
                            value = cost,
                            onValueChange = { cost = keepDecimalInput(it) },
                            label = costLabel,
                            keyboardType = KeyboardType.Decimal
                        )
                        if (isRoute) {
                            if (routeSuggestedCost > 0.0) {
                                OutlinedButton(
                                    onClick = { cost = formatPlainDecimal(routeSuggestedCost) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        tr("Usar custo real estimado: ", "Use estimated real cost: ") + formatMoney(routeSuggestedCost),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            OperationalTextField(
                                value = revenue,
                                onValueChange = { revenue = keepDecimalInput(it) },
                                label = tr("Valor cobrado do cliente", "Amount charged to client"),
                                keyboardType = KeyboardType.Decimal
                            )
                            OperationalTextField(
                                value = taxPercent,
                                onValueChange = { taxPercent = keepDecimalInput(it) },
                                label = tr("Imposto (%)", "Tax (%)"),
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                    }

                    Button(
                        onClick = { saveOperationalRecord() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(saveButtonText, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        if (showDriversManager) {
            DriverManagerScreen(
                drivers = drivers,
                routeRecords = loadOperationalRecords(context)
                    .filter { it.feature == OperationalFeature.ROUTE_PROFITABILITY.name },
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor,
                screenBg = screenBg,
                onDismiss = { showDriversManager = false },
                onSaveDriver = { editingDriverId, editedName, editedCode, editedPhone, editedSalary, editedTaxCost, editedDefaultCost ->
                    saveDriverFromManager(
                        editingDriverId = editingDriverId,
                        editedName = editedName,
                        editedCode = editedCode,
                        editedPhone = editedPhone,
                        editedSalary = editedSalary,
                        editedTaxCost = editedTaxCost,
                        editedDefaultCost = editedDefaultCost
                    )
                },
                onDeleteDriver = { driver -> deleteDriverFromManager(driver) }
            )
        }
    }
}

@Composable
private fun OperationalSummaryCard(
    feature: OperationalFeature,
    records: List<OperationalRecord>,
    vehiclesCount: Int,
    driversCount: Int,
    importedRecordsCount: Int,
    realCostPerKm: Double,
    routeSuggestedCost: Double,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color
) {
    val finished = records.filter { it.kmEnd != null && it.kmEnd > it.kmStart }
    val routeRecords = records.filter { it.revenue != null }
    val currentMonthTag = SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date())
    val currentMonthRouteBalance = routeRecords
        .filter { SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date(it.createdAt)) == currentMonthTag }
        .sumOf { routeProfit(it) }
    val bestRouteProfitMetric = routeRecords.maxByOrNull { routeProfit(it) }?.let {
        val margin = routeMargin(it)
        "${formatMoney(routeProfit(it))} • ${formatPlainDecimal(margin)}%"
    } ?: tr("Sem rotas calculadas", "No calculated routes")
    val totalRouteBalanceMetric = routeRecords.sumOf { routeProfit(it) }.let {
        formatMoney(it)
    }
    val bestDurabilityMetric = finished.maxByOrNull { (it.kmEnd ?: 0) - it.kmStart }?.let {
        "${(it.kmEnd ?: 0) - it.kmStart} km"
    } ?: tr("Aguardando KM final", "Waiting for final mileage")
    val lowestCostMetric = finished.minByOrNull { costPerKm(it) }?.let {
        "${formatMoney(costPerKm(it))}/km"
    } ?: tr("Aguardando KM", "Waiting mileage")
    val primaryMetric = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> totalRouteBalanceMetric
        else -> lowestCostMetric
    }
    val primaryLabel = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Saldo total", "Total balance")
        else -> tr("Menor custo/km", "Lowest cost/km")
    }
    val wideMetricLabel = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Melhor lucro", "Best profit")
        else -> tr("Melhor durabilidade", "Best durability")
    }
    val wideMetricValue = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> bestRouteProfitMetric
        else -> bestDurabilityMetric
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(tr("Resumo", "Summary"), color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryPill(
                label = tr("Registros", "Records"),
                value = records.size.toString(),
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
            SummaryPill(
                label = tr("Veiculos", "Vehicles"),
                value = vehiclesCount.toString(),
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryPill(
                label = tr("Importados", "Imported"),
                value = importedRecordsCount.toString(),
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
            SummaryPill(
                label = primaryLabel,
                value = primaryMetric,
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
        }
        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill(
                    label = tr("Motoristas", "Drivers"),
                    value = driversCount.toString(),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
                SummaryPill(
                    label = tr("Linhas com motorista", "Routes with driver"),
                    value = routeRecords.count { it.driverName.isNotBlank() }.toString(),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
            }
            SummaryPill(
                label = tr("Saldo do mes", "Monthly balance"),
                value = formatMoney(currentMonthRouteBalance),
                modifier = Modifier.fillMaxWidth(),
                titleColor = titleColor,
                subColor = subColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill(
                    label = tr("Custo/km real", "Real cost/km"),
                    value = formatMoney(realCostPerKm),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
                SummaryPill(
                    label = tr("Sugestao rota", "Route suggestion"),
                    value = formatMoney(routeSuggestedCost),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
            }
        }
        SummaryPill(
            label = wideMetricLabel,
            value = wideMetricValue,
            modifier = Modifier.fillMaxWidth(),
            titleColor = titleColor,
            subColor = subColor
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    modifier: Modifier,
    titleColor: Color,
    subColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF3B82F6).copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = subColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
    }
}

@Composable
private fun OperationalFormSection(
    title: String,
    cardBorder: Color,
    subColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF3B82F6).copy(alpha = 0.04f))
            .border(BorderStroke(1.dp, cardBorder.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = subColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VehiclePickerField(
    vehicles: List<CarroInfo>,
    selectedVehicleId: String,
    onSelect: (CarroInfo) -> Unit,
    fallbackValue: String,
    onFallbackChange: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = vehicles.firstOrNull { it.id == selectedVehicleId }
    if (vehicles.isEmpty()) {
        OperationalTextField(value = fallbackValue, onValueChange = onFallbackChange, label = label)
        return
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.displayName() ?: fallbackValue.ifBlank { vehicles.firstOrNull()?.displayName().orEmpty() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(vehicle.displayName(), fontWeight = FontWeight.SemiBold)
                            Text(
                                if (vehicle.kmAtual > 0) "${vehicle.kmAtual} km" else tr("KM nao informado", "Mileage not informed"),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    onClick = {
                        onSelect(vehicle)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DriverPickerField(
    drivers: List<OperationalDriver>,
    selectedDriverId: String,
    onSelect: (OperationalDriver) -> Unit,
    onClear: () -> Unit,
    onRequestAdd: () -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = drivers.firstOrNull { it.id == selectedDriverId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (drivers.isEmpty()) {
                onRequestAdd()
            } else {
                expanded = !expanded
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.name ?: if (drivers.isEmpty()) {
                tr("Nenhum motorista cadastrado", "No drivers registered")
            } else {
                tr("Selecione um motorista", "Select a driver")
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        tr("Sem motorista nesta linha", "No driver for this route"),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {
                    onClear()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        tr("Cadastrar novo motorista", "Add new driver"),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                },
                onClick = {
                    expanded = false
                    onRequestAdd()
                }
            )
            drivers.forEach { driver ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(driver.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(
                                    driver.code.takeIf { it.isNotBlank() }?.let { "Cod: $it" },
                                    driver.phone,
                                    driver.salary.takeIf { it > 0.0 }?.let { tr("Salario", "Salary") + ": ${formatMoney(it)}" },
                                    driver.taxCost.takeIf { it > 0.0 }?.let { tr("Custos", "Costs") + ": ${formatMoney(it)}" },
                                    driver.defaultCost.takeIf { it > 0.0 }?.let { formatMoney(it) }
                                ).filterNotNull().filter { it.isNotBlank() }.joinToString(" • ")
                                    .ifBlank { tr("Sem custo padrao", "No default cost") },
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    onClick = {
                        onSelect(driver)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PartTypePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        tr("Pastilha de freio", "Brake pad"),
        tr("Terminal de suspensao", "Suspension terminal"),
        tr("Pivo de suspensao", "Suspension ball joint"),
        tr("Bucha de suspensao", "Suspension bushing"),
        tr("Amortecedor", "Shock absorber"),
        tr("Outra peca", "Other part")
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(tr("Selecione a peca", "Select the part")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun OperationalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun EmptyOperationalState(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tr("Nenhum registro ainda", "No records yet"), color = titleColor, fontWeight = FontWeight.Bold)
        Text(
            tr("Cadastre o primeiro item acima e o calculo aparece aqui.", "Add the first item above and the calculation appears here."),
            color = subColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OperationalRecordCard(
    feature: OperationalFeature,
    record: OperationalRecord,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    canDelete: Boolean,
    onEdit: (() -> Unit)?,
    onUpdateKmEnd: ((Int) -> Unit)?,
    onDelete: () -> Unit
) {
    var editingKmEnd by remember(record.id, record.kmEnd) { mutableStateOf(false) }
    var kmEndDraft by remember(record.id, record.kmEnd) { mutableStateOf(record.kmEnd?.toString().orEmpty()) }
    var kmEndError by remember(record.id, record.kmEnd) { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember(record.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val recordDriver = remember(record.driverId) {
        loadOperationalDrivers(context).firstOrNull { it.id == record.driverId }
    }
    val isWearRecord = feature != OperationalFeature.ROUTE_PROFITABILITY
    val durability = record.kmEnd?.let { it - record.kmStart }
    val isPendingWear = isWearRecord && (durability == null || durability <= 0)
    val metric = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> {
            if (record.revenue == null) {
                tr("Custo importado: ", "Imported cost: ") + formatMoney(record.cost)
            } else {
                val profit = routeProfit(record)
                val margin = routeMargin(record)
                val status = if (profit >= 0) tr("lucro", "profit") else tr("prejuizo", "loss")
                "${formatMoney(profit)} $status • ${"%.1f".format(Locale("pt", "BR"), margin)}%"
            }
        }
        else -> {
            if (durability != null && durability > 0) {
                "$durability km • ${formatMoney(costPerKm(record))}/km"
            } else {
                tr("Em uso • toque para informar KM final", "In use • add final mileage")
            }
        }
    }
    val titleParts = remember(record.name) { splitOperationalTitle(record.name) }
    val accentColor = when {
        feature == OperationalFeature.ROUTE_PROFITABILITY && routeProfit(record) < 0 -> Color(0xFFDC2626)
        isPendingWear -> Color(0xFFF59E0B)
        else -> Color(0xFF059669)
    }
    val metricLabel = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Resultado", "Result")
        else -> tr("Durabilidade", "Durability")
    }
    val financialItems = titleParts.financialLines.map { line ->
        val parts = line.split(":", limit = 2)
        if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            tr("Valor", "Value") to line
        }
    }
    val vehicleItems = buildList {
        if (record.vehicle.isNotBlank()) {
            add(tr("Veiculo", "Vehicle") to record.vehicle)
        }
        if (feature == OperationalFeature.ROUTE_PROFITABILITY && record.driverName.isNotBlank()) {
            add(tr("Motorista", "Driver") to record.driverName)
            recordDriver?.code?.takeIf { it.isNotBlank() }?.let { add(tr("Codigo", "Code") to it) }
        }
        if (record.brandOrClient.isNotBlank()) {
            val label = if (feature == OperationalFeature.ROUTE_PROFITABILITY) tr("Cliente", "Client") else tr("Marca/origem", "Brand/source")
            add(label to record.brandOrClient)
        }
    }
    val detailItems = buildList {
        if (record.positionOrRoute.isNotBlank()) {
            val label = when (feature) {
                OperationalFeature.TIRE_ROI -> tr("Posicao", "Position")
                OperationalFeature.PARTS_DURABILITY -> tr("Local", "Location")
                OperationalFeature.ROUTE_PROFITABILITY -> tr("Rota", "Route")
            }
            add(label to record.positionOrRoute)
        }
        if (feature == OperationalFeature.TIRE_ROI) {
            add(tr("Quantidade", "Quantity") to record.quantity.coerceAtLeast(1).toString())
            if (record.recordDate.isNotBlank()) add(tr("Data", "Date") to record.recordDate)
        }
    }
    val routeDetailItems = buildList {
        if (record.vehicle.isNotBlank()) add(tr("Veiculo", "Vehicle") to record.vehicle)
        if (record.brandOrClient.isNotBlank()) add(tr("Cliente", "Client") to record.brandOrClient)
        if (record.positionOrRoute.isNotBlank()) add(tr("Rota", "Route") to record.positionOrRoute)
        if (record.kmStart > 0) add(tr("Distancia", "Distance") to "${record.kmStart} km")
    }
    val routeDriverItems = buildList {
        if (record.driverName.isNotBlank()) add(tr("Nome", "Name") to record.driverName)
        recordDriver?.code?.takeIf { it.isNotBlank() }?.let { add(tr("Codigo", "Code") to it) }
        recordDriver?.salary?.takeIf { it > 0.0 }?.let { add(tr("Salario", "Salary") to formatMoney(it)) }
        recordDriver?.taxCost?.takeIf { it > 0.0 }?.let { add(tr("Impostos/custos", "Taxes/costs") to formatMoney(it)) }
    }
    val kmItems = buildList {
        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            if (record.kmStart > 0) add(tr("Distancia", "Distance") to "${record.kmStart} km")
        } else {
            add(tr("Inicial", "Start") to "${record.kmStart} km")
            add(tr("Final", "Final") to (record.kmEnd?.let { "$it km" } ?: tr("Aguardando KM", "Waiting mileage")))
        }
    }
    val routeMoneyItems = buildList {
        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            record.revenue?.let { add(tr("Receita", "Revenue") to formatMoney(it)) }
            record.taxPercent?.let { add(tr("Imposto", "Tax") to "${formatPlainDecimal(it)}%") }
            add(tr("Custo operacional", "Operational cost") to formatMoney(record.cost))
            if (record.driverCost > 0.0) {
                add(tr("Custo motorista", "Driver cost") to formatMoney(record.driverCost))
            }
            add(tr("Custo total", "Total cost") to formatMoney(routeTotalCost(record)))
        }
    }
    val infoGroups = buildList {
        if (feature != OperationalFeature.ROUTE_PROFITABILITY && financialItems.isNotEmpty()) {
            add(OperationalInfoGroup(tr("Financeiro", "Financial"), financialItems))
        }
        if (feature != OperationalFeature.ROUTE_PROFITABILITY && vehicleItems.isNotEmpty()) {
            add(OperationalInfoGroup(tr("Veiculo", "Vehicle"), vehicleItems))
        }
        if (detailItems.isNotEmpty()) {
            add(OperationalInfoGroup(tr("Detalhes", "Details"), detailItems))
        }
        if (feature != OperationalFeature.ROUTE_PROFITABILITY && kmItems.isNotEmpty()) {
            add(OperationalInfoGroup(if (feature == OperationalFeature.ROUTE_PROFITABILITY) tr("Distancia", "Distance") else "KM", kmItems))
        }
        if (feature != OperationalFeature.ROUTE_PROFITABILITY) {
            add(OperationalInfoGroup(metricLabel, listOf(metricLabel to metric), emphasize = true))
        }
    }
    val emptyKmEndError = tr("Informe o KM final.", "Enter final mileage.")
    val invalidKmEndError = tr("KM final deve ser maior que o inicial.", "Final mileage must be greater than initial.")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accentColor)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(titleParts.title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
            }
            if (canDelete || onEdit != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = tr("Editar", "Edit"),
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = tr("Excluir", "Delete"),
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            OperationalRouteRecordInfo(
                financialItems = routeMoneyItems,
                driverItems = routeDriverItems,
                detailItems = routeDetailItems,
                result = metric,
                subColor = subColor,
                titleColor = titleColor,
                accentColor = accentColor
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                infoGroups.forEach { group ->
                    OperationalInfoGroupCard(
                        title = group.title,
                        items = group.items,
                        valueColor = subColor,
                        accentColor = accentColor,
                        emphasize = group.emphasize
                    )
                }
            }
        }

        if (isWearRecord && onUpdateKmEnd != null) {
            if (editingKmEnd || isPendingWear) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kmEndDraft,
                        onValueChange = {
                            kmEndDraft = keepNumericInput(it)
                            kmEndError = null
                        },
                        label = { Text(tr("KM final", "Final mileage")) },
                        singleLine = true,
                        isError = kmEndError != null,
                        supportingText = kmEndError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = {
                            val finalKm = kmEndDraft.toIntOrNull()
                            when {
                                finalKm == null -> kmEndError = emptyKmEndError
                                finalKm <= record.kmStart -> kmEndError = invalidKmEndError
                                else -> {
                                    onUpdateKmEnd(finalKm)
                                    editingKmEnd = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(tr("Salvar KM final", "Save final mileage"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { editingKmEnd = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (record.kmEnd == null) tr("Atualizar KM final", "Update final mileage")
                        else tr("Editar KM final", "Edit final mileage"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Text(
                        text = tr("Apagar este registro?", "Delete this record?"),
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Essa ação remove o registro permanentemente. Deseja continuar?",
                        "This action permanently deletes the record. Do you want to continue?"
                    ),
                    color = subColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                ) {
                    Text(tr("Sim, apagar", "Yes, delete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirm = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text(tr("Cancelar", "Cancel"))
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
private fun OperationalInfoGroupCard(
    title: String,
    items: List<Pair<String, String>>,
    valueColor: Color,
    accentColor: Color,
    emphasize: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = if (emphasize) 0.12f else 0.07f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = if (emphasize) 0.20f else 0.12f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    color = valueColor.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .widthIn(max = 88.dp)
                        .alignByBaseline()
                )
                Text(
                    text = value,
                    color = if (emphasize) accentColor else valueColor,
                    fontSize = 12.sp,
                    fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline()
                )
            }
        }
    }
}

@Composable
private fun OperationalRouteRecordInfo(
    financialItems: List<Pair<String, String>>,
    driverItems: List<Pair<String, String>>,
    detailItems: List<Pair<String, String>>,
    result: String,
    subColor: Color,
    titleColor: Color,
    accentColor: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OperationalRouteSection(
            title = tr("Rota", "Route"),
            items = detailItems,
            subColor = subColor,
            titleColor = titleColor,
            accentColor = accentColor
        )

        if (financialItems.isNotEmpty()) {
            OperationalRouteMoneyGrid(
                title = tr("Custos da rota", "Route costs"),
                items = financialItems,
                subColor = subColor,
                titleColor = titleColor,
                accentColor = accentColor
            )
        }

        if (driverItems.isNotEmpty()) {
            OperationalRouteSection(
                title = tr("Motorista", "Driver"),
                items = driverItems,
                subColor = subColor,
                titleColor = titleColor,
                accentColor = accentColor
            )
        }

        OperationalRouteResultPill(
            result = result,
            subColor = subColor,
            accentColor = accentColor
        )
    }
}

@Composable
private fun OperationalRouteSection(
    title: String,
    items: List<Pair<String, String>>,
    subColor: Color,
    titleColor: Color,
    accentColor: Color
) {
    if (items.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.11f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    color = subColor.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(76.dp)
                )
                Text(
                    text = value,
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OperationalRouteMoneyGrid(
    title: String,
    items: List<Pair<String, String>>,
    subColor: Color,
    titleColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.11f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    OperationalRouteMoneyCell(
                        label = label,
                        value = value,
                        subColor = subColor,
                        titleColor = titleColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OperationalRouteMoneyCell(
    label: String,
    value: String,
    subColor: Color,
    titleColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.48f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = subColor.copy(alpha = 0.78f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = titleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OperationalRouteResultPill(
    result: String,
    subColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.13f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tr("Resultado", "Result"),
            color = subColor.copy(alpha = 0.78f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = result,
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
