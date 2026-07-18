package br.com.gui.carlembrete

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
internal fun CorporateFleetModuleScreen(
    module: CorporateFleetModule,
    onDismiss: () -> Unit,
    screenBg: Color,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color
) {
    val icon = when (module) {
        CorporateFleetModule.OVERVIEW -> Icons.Default.DirectionsCar
        CorporateFleetModule.RESERVATIONS -> Icons.Default.Route
        CorporateFleetModule.QR_PICKUP -> Icons.Default.CheckCircle
        CorporateFleetModule.TRIPS -> Icons.Default.Route
        CorporateFleetModule.MAINTENANCE -> Icons.Default.Build
        CorporateFleetModule.DOCUMENTS -> Icons.Default.Inventory2
        CorporateFleetModule.USERS -> Icons.Default.PersonAdd
    }
    val title = when (module) {
        CorporateFleetModule.OVERVIEW -> tr("VisÃ£o geral da frota", "Fleet overview")
        CorporateFleetModule.RESERVATIONS -> tr("Reservas", "Reservations")
        CorporateFleetModule.QR_PICKUP -> tr("QR Code e retirada", "QR code pickup")
        CorporateFleetModule.TRIPS -> tr("Viagens corporativas", "Corporate trips")
        CorporateFleetModule.MAINTENANCE -> tr("Manutencoes", "Maintenance")
        CorporateFleetModule.DOCUMENTS -> tr("Documentos", "Documents")
        CorporateFleetModule.USERS -> tr("Usuarios e permissoes", "Users and permissions")
    }
    val subtitle = when (module) {
        CorporateFleetModule.OVERVIEW -> tr(
            "Resumo operacional com veiculos, reservas, viagens e pendencias.",
            "Operational summary with vehicles, reservations, trips and pending work."
        )
        CorporateFleetModule.RESERVATIONS -> tr(
            "Calendario corporativo para evitar sobreposicao e controlar retirada e devolucao.",
            "Corporate calendar to avoid overlaps and control pickup and return."
        )
        CorporateFleetModule.QR_PICKUP -> tr(
            "Validacao por QR Code antes de liberar o veiculo para uma viagem.",
            "QR code validation before releasing the vehicle for a trip."
        )
        CorporateFleetModule.TRIPS -> tr(
            "Registro de distancia por GPS, odometro, ocorrencias e abastecimentos.",
            "GPS distance, odometer, incidents and fuel records."
        )
        CorporateFleetModule.MAINTENANCE -> tr(
            "Alertas automaticos, bloqueios preventivos e historico de servicos.",
            "Automatic alerts, preventive blocks and service history."
        )
        CorporateFleetModule.DOCUMENTS -> tr(
            "CRLV, seguro, notas fiscais, fotos e comprovantes em um so lugar.",
            "Registration, insurance, invoices, photos and receipts in one place."
        )
        CorporateFleetModule.USERS -> tr(
            "Controle de administradores, gestores, motoristas, manutencao e leitores.",
            "Control admins, managers, drivers, maintenance and viewers."
        )
    }
    val checklist = when (module) {
        CorporateFleetModule.OVERVIEW -> listOf(
            tr("Indicadores de disponibilidade", "Availability indicators"),
            tr("Viagens em andamento", "Active trips"),
            tr("Manutencoes e documentos vencendo", "Maintenance and documents expiring")
        )
        CorporateFleetModule.RESERVATIONS -> listOf(
            tr("Criar reserva com veiculo, horario e destino", "Create reservation with vehicle, time and destination"),
            tr("Bloquear reservas sobrepostas", "Block overlapping reservations"),
            tr("Impedir uso de veiculo bloqueado ou em manutencao", "Prevent use of blocked or maintenance vehicles")
        )
        CorporateFleetModule.QR_PICKUP -> listOf(
            tr("Ler QR Code do veiculo ou chave", "Read vehicle or key QR code"),
            tr("Validar usuario, empresa, reserva e horario", "Validate user, company, reservation and time"),
            tr("Iniciar viagem somente apos aprovacao", "Start trip only after approval")
        )
        CorporateFleetModule.TRIPS -> listOf(
            tr("Calcular distancia somando pontos de GPS", "Calculate distance by summing GPS points"),
            tr("Funcionar offline durante o percurso", "Work offline during the route"),
            tr("Confirmar odometro final com foto", "Confirm final odometer with photo")
        )
        CorporateFleetModule.MAINTENANCE -> listOf(
            tr("Verificar vencimento por data e KM", "Check due date and mileage"),
            tr("Criar alerta para responsavel", "Create alert for owner"),
            tr("Bloquear veiculo em caso critico", "Block vehicle in critical cases")
        )
        CorporateFleetModule.DOCUMENTS -> listOf(
            tr("Guardar metadados no banco", "Store metadata in the database"),
            tr("Enviar arquivos para bucket privado", "Upload files to private bucket"),
            tr("Controlar vencimento de seguro e licenciamento", "Track insurance and registration expiration")
        )
        CorporateFleetModule.USERS -> listOf(
            tr("Separar dados por empresa", "Separate data by company"),
            tr("Definir papeis e permissoes", "Define roles and permissions"),
            tr("Registrar auditoria das excecoes", "Audit exception approvals")
        )
    }

    Scaffold(
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(42.dp)) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = tr("Voltar", "Back"),
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
                }
                Text(title, color = titleColor, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text(subtitle, color = subColor, fontSize = 14.sp, lineHeight = 20.sp)
            }

            if (module == CorporateFleetModule.RESERVATIONS) {
                CorporateReservationsContent(
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        tr("Primeira versao do modulo", "First module version"),
                        color = titleColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    checklist.forEach { item ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Text(item, color = subColor, fontSize = 14.sp, lineHeight = 19.sp)
                        }
                    }
                }

                Text(
                    tr(
                        "Este modulo agora abre separado da visao geral. As telas completas serao conectadas ao Firestore e ao dashboard web conforme os fluxos forem implementados.",
                        "This module now opens separately from the overview. Full screens will connect to Firestore and the web dashboard as flows are implemented."
                    ),
                    color = dimColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CorporateReservationsContent(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color
) {
    val context = LocalContext.current
    val authUser = FirebaseAuth.getInstance().currentUser
    val fallbackCompanyId = remember(authUser?.uid) { authUser?.uid?.let { "personal_$it" } }
    var companyId by remember(authUser?.uid) { mutableStateOf(fallbackCompanyId) }
    val vehicles = remember { mutableStateListOf<CorporateFleetVehicle>() }
    val reservations = remember { mutableStateListOf<CorporateReservation>().apply { addAll(loadLocalCorporateReservations(context)) } }
    var selectedVehicleId by remember { mutableStateOf("") }
    var vehicleMenuOpen by remember { mutableStateOf(false) }
    var driverName by remember { mutableStateOf(authUser?.displayName ?: authUser?.email.orEmpty()) }
    var destination by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(nextRoundedHourMillis()) }
    var endMillis by remember { mutableStateOf(nextRoundedHourMillis() + 60 * 60 * 1000L) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
    var showNewReservationForm by remember { mutableStateOf(false) }
    var selectedDayMillis by remember { mutableStateOf(startOfDayMillis(System.currentTimeMillis())) }
    var qrReservation by remember { mutableStateOf<CorporateReservation?>(null) }
    val currentMonthDays = remember(selectedDayMillis, reservations.size) { reservationCalendarDays(selectedDayMillis) }
    val selectedDayReservations = reservations
        .filter { startOfDayMillis(it.startsAtMillis) == selectedDayMillis }
        .sortedBy { it.startsAtMillis }

    DisposableEffect(authUser?.uid, authUser?.email) {
        val user = authUser
        if (user != null && fallbackCompanyId != null) {
            resolveCorporateCompanyId(user, fallbackCompanyId) { resolvedCompanyId ->
                companyId = resolvedCompanyId
            }
        }
        onDispose { }
    }

    DisposableEffect(companyId) {
        val activeCompanyId = companyId
        if (activeCompanyId.isNullOrBlank()) {
            onDispose { }
        } else {
            val db = FirebaseFirestore.getInstance()
            val vehicleRegistration = db
                .collection("companies")
                .document(activeCompanyId)
                .collection("vehicles")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    vehicles.clear()
                    snapshot?.documents?.mapNotNullTo(vehicles) { doc ->
                        val status = doc.getString("status").orEmpty().ifBlank { "disponivel" }
                        if (status == "inativo") return@mapNotNullTo null
                        CorporateFleetVehicle(
                            id = doc.id,
                            name = doc.getString("name").orEmpty().ifBlank { doc.getString("nome").orEmpty().ifBlank { "Veiculo" } },
                            plate = doc.getString("plate").orEmpty().ifBlank { doc.getString("placa").orEmpty() },
                            model = doc.getString("model").orEmpty().ifBlank { doc.getString("modelo").orEmpty() },
                            status = status,
                            odometerKm = doc.getLong("odometerKm")?.toInt() ?: doc.getLong("kmAtual")?.toInt() ?: 0
                        )
                    }
                    if (selectedVehicleId.isBlank() || vehicles.none { it.id == selectedVehicleId }) {
                        selectedVehicleId = vehicles.firstOrNull()?.id.orEmpty()
                    }
                }
            val registration = db
                .collection("companies")
                .document(activeCompanyId)
                .collection("reservations")
                .orderBy("startsAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (reservations.isEmpty()) reservations.addAll(loadLocalCorporateReservations(context))
                        return@addSnapshotListener
                    }
                    reservations.clear()
                    snapshot?.documents?.mapNotNullTo(reservations) { doc ->
                        val startsAt = doc.getTimestamp("startsAt")?.toDate()?.time ?: return@mapNotNullTo null
                        val endsAt = doc.getTimestamp("endsAt")?.toDate()?.time ?: startsAt
                        CorporateReservation(
                            id = doc.id,
                            vehicleId = doc.getString("vehicleId").orEmpty(),
                            vehicleName = doc.getString("vehicleName").orEmpty().ifBlank { "Veiculo" },
                            driverName = doc.getString("driverName").orEmpty().ifBlank { "Motorista" },
                            destination = doc.getString("destination").orEmpty(),
                            startsAtMillis = startsAt,
                            endsAtMillis = endsAt,
                            status = doc.getString("status").orEmpty().ifBlank { "reservada" },
                            tripStartedAtMillis = doc.getTimestamp("tripStartedAt")?.toDate()?.time,
                            tripEndedAtMillis = doc.getTimestamp("tripEndedAt")?.toDate()?.time
                        )
                    }
                }
            onDispose {
                vehicleRegistration.remove()
                registration.remove()
            }
        }
    }

    fun openDatePicker(currentMillis: Long, onChanged: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = currentMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                onChanged(updated.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker(currentMillis: Long, onChanged: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = currentMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onChanged(updated.timeInMillis)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun createReservation() {
        val vehicle = selectedVehicle
        val activeCompanyId = companyId
        when {
            authUser == null -> {
                message = "Entre na sua conta para criar reservas."
                return
            }
            activeCompanyId.isNullOrBlank() -> {
                message = "Empresa nao encontrada para este usuario."
                return
            }
            vehicle == null -> {
                message = "Cadastre ou selecione um veiculo antes de reservar."
                return
            }
            vehicle.status != "disponivel" && vehicle.status != "reservado" -> {
                message = "Este veiculo corporativo nao esta disponivel para reserva."
                return
            }
            endMillis <= startMillis -> {
                message = "A devolucao precisa ser depois da retirada."
                return
            }
            reservations.any { it.vehicleId == vehicle.id && it.status != "cancelada" && rangesOverlap(startMillis, endMillis, it.startsAtMillis, it.endsAtMillis) } -> {
                message = "Este veiculo ja esta reservado nesse horario."
                return
            }
        }

        saving = true
        message = ""
        val reservationId = UUID.randomUUID().toString()
        val localReservation = CorporateReservation(
            id = reservationId,
            vehicleId = vehicle.id,
            vehicleName = vehicle.displayName(),
            driverName = driverName.ifBlank { authUser.email.orEmpty() },
            destination = destination,
            startsAtMillis = startMillis,
            endsAtMillis = endMillis,
            status = "reservada"
        )
        val localUpdated = (reservations.filterNot { it.id == reservationId } + localReservation)
            .sortedBy { it.startsAtMillis }
        reservations.clear()
        reservations.addAll(localUpdated)
        saveLocalCorporateReservations(context, localUpdated)
        saveCorporateReservation(
            reservationId = reservationId,
            user = authUser,
            companyId = activeCompanyId,
            vehicle = vehicle,
            driverName = driverName.ifBlank { authUser.email.orEmpty() },
            destination = destination,
            purpose = purpose,
            startMillis = startMillis,
            endMillis = endMillis,
            onSuccess = {
                saving = false
                message = "Reserva criada. Ela ja aparece no dashboard web."
                destination = ""
                purpose = ""
                selectedDayMillis = startOfDayMillis(startMillis)
                showNewReservationForm = false
            },
            onError = { error ->
                saving = false
                message = if (error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                    error.localizedMessage?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                    error.localizedMessage?.contains("permission", ignoreCase = true) == true
                ) {
                    "Reserva salva no app. Para aparecer no dashboard, publique as regras do Firestore."
                } else {
                    "Reserva salva no app, mas ainda nao sincronizou: ${error.localizedMessage}"
                }
            }
        )
    }

    fun confirmQrForReservation(reservation: CorporateReservation, qrText: String) {
        val activeCompanyId = companyId
        val user = authUser
        if (user == null || activeCompanyId.isNullOrBlank()) {
            message = "Entre na sua conta para validar retirada e devolucao."
            return
        }
        val vehicleMatchedByQr = vehicles.firstOrNull { corporateVehicleQrMatches(it, qrText) }
        val qrMatchesReservation = reservationQrMatches(reservation, qrText) ||
            (vehicleMatchedByQr != null && normalizedReservationText(vehicleMatchedByQr.name) == normalizedReservationText(reservation.vehicleName))
        if (!qrMatchesReservation) {
            message = "QR Code nao confere com este veiculo ou reserva."
            return
        }
        val now = System.currentTimeMillis()
        val startingTrip = reservation.status == "reservada"
        val newStatus = if (startingTrip) "em_uso" else "finalizada"
        val updated = reservation.copy(
            status = newStatus,
            tripStartedAtMillis = if (startingTrip) now else reservation.tripStartedAtMillis,
            tripEndedAtMillis = if (startingTrip) reservation.tripEndedAtMillis else now
        )
        val localUpdated = reservations.map { if (it.id == reservation.id) updated else it }.sortedBy { it.startsAtMillis }
        reservations.clear()
        reservations.addAll(localUpdated)
        saveLocalCorporateReservations(context, localUpdated)
        qrReservation = null
        message = if (startingTrip) "Viagem iniciada por QR Code." else "Viagem finalizada por QR Code."
        updateCorporateReservationTripStatus(
            companyId = activeCompanyId,
            reservation = updated,
            qrText = qrText,
            user = user,
            onError = { error ->
                message = "Atualizado no app, mas ainda nao sincronizou: ${error.localizedMessage}"
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showNewReservationForm) {
            NewCorporateReservationCard(
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor,
                dimColor = dimColor,
                vehicles = vehicles,
                selectedVehicle = selectedVehicle,
                vehicleMenuOpen = vehicleMenuOpen,
                onVehicleMenuChange = { vehicleMenuOpen = it },
                onVehicleSelected = { vehicle ->
                    selectedVehicleId = vehicle.id
                    vehicleMenuOpen = false
                },
                driverName = driverName,
                onDriverNameChange = { driverName = it },
                destination = destination,
                onDestinationChange = { destination = it },
                purpose = purpose,
                onPurposeChange = { purpose = it },
                startMillis = startMillis,
                endMillis = endMillis,
                onStartDateClick = { openDatePicker(startMillis) { updated -> startMillis = updated; if (endMillis <= startMillis) endMillis = startMillis + 60 * 60 * 1000L } },
                onStartTimeClick = { openTimePicker(startMillis) { updated -> startMillis = updated; if (endMillis <= startMillis) endMillis = startMillis + 60 * 60 * 1000L } },
                onEndDateClick = { openDatePicker(endMillis) { updated -> endMillis = updated } },
                onEndTimeClick = { openTimePicker(endMillis) { updated -> endMillis = updated } },
                saving = saving,
                message = message,
                onSave = { createReservation() },
                onBack = { showNewReservationForm = false }
            )
        } else {
            CorporateReservationCalendarCard(
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor,
                dimColor = dimColor,
                days = currentMonthDays,
                reservations = reservations,
                selectedDayMillis = selectedDayMillis,
                selectedDayReservations = selectedDayReservations,
                onDaySelected = { selectedDayMillis = it },
                onNewReservation = {
                    startMillis = selectedDayMillis + 9 * 60 * 60 * 1000L
                    endMillis = startMillis + 60 * 60 * 1000L
                    showNewReservationForm = true
                    message = ""
                },
                onQrAction = { qrReservation = it }
            )
            if (message.isNotBlank()) {
                Text(
                    message,
                    color = if (message.contains("nao", ignoreCase = true) || message.contains("Falha", ignoreCase = true)) Color(0xFFDC2626) else dimColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    qrReservation?.let { reservation ->
        ReservationQrValidationDialog(
            reservation = reservation,
            onDismiss = { qrReservation = null },
            onConfirm = { qrText -> confirmQrForReservation(reservation, qrText) }
        )
    }
}

@Composable
private fun CorporateReservationCalendarCard(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    days: List<Long>,
    reservations: List<CorporateReservation>,
    selectedDayMillis: Long,
    selectedDayReservations: List<CorporateReservation>,
    onDaySelected: (Long) -> Unit,
    onNewReservation: () -> Unit,
    onQrAction: (CorporateReservation) -> Unit
) {
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(selectedDayMillis))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Agenda da frota", color = titleColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(monthLabel.replaceFirstChar { it.titlecase(Locale.getDefault()) }, color = subColor, fontSize = 13.sp)
            }
            Button(onClick = onNewReservation, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nova reserva", fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { label ->
                Text(
                    label,
                    color = dimColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { dayMillis ->
                    val dayReservations = reservations.filter { startOfDayMillis(it.startsAtMillis) == dayMillis }
                    val isSelected = dayMillis == selectedDayMillis
                    val isToday = dayMillis == startOfDayMillis(System.currentTimeMillis())
                    val bg = when {
                        isSelected -> Color(0xFF0F766E)
                        dayReservations.isNotEmpty() -> Color(0xFFE0F2FE)
                        isToday -> Color(0xFFF0FDFA)
                        else -> Color(0xFFF8FAFC)
                    }
                    val textColor = if (isSelected) Color.White else titleColor
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(BorderStroke(1.dp, if (isSelected) Color(0xFF0F766E) else cardBorder), RoundedCornerShape(12.dp))
                            .clickable { onDaySelected(dayMillis) }
                            .padding(7.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(dayMillis)), color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        if (dayReservations.isNotEmpty()) {
                            Text(
                                "${dayReservations.size} reserva",
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF0369A1),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Divider(color = cardBorder)
        Text("Reservas do dia", color = titleColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
        if (selectedDayReservations.isEmpty()) {
            Text("Nenhum veiculo reservado neste dia.", color = subColor, fontSize = 14.sp)
        } else {
            selectedDayReservations.forEach { reservation ->
                CorporateReservationDayCard(
                    reservation = reservation,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
                    onQrAction = { onQrAction(reservation) }
                )
            }
        }
    }
}

@Composable
private fun CorporateReservationDayCard(
    reservation: CorporateReservation,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    onQrAction: () -> Unit
) {
    val statusColor = when (reservation.status) {
        "em_uso" -> Color(0xFFEA580C)
        "finalizada" -> Color(0xFF15803D)
        else -> Color(0xFF0369A1)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(reservation.vehicleName, color = titleColor, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${formatReservationTime(reservation.startsAtMillis)} - ${formatReservationTime(reservation.endsAtMillis)}", color = subColor, fontSize = 13.sp)
                Text("${reservation.driverName}${reservation.destination.ifBlank { "" }.let { if (it.isBlank()) "" else " - $it" }}", color = dimColor, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(reservationStatusLabel(reservation.status), color = statusColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        if (reservation.status != "finalizada") {
            OutlinedButton(onClick = onQrAction, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (reservation.status == "em_uso") "Escanear QR para devolver" else "Escanear QR para retirar")
            }
        } else {
            Text(
                "Viagem concluida ${reservation.tripEndedAtMillis?.let { "as ${formatReservationTime(it)}" }.orEmpty()}",
                color = Color(0xFF15803D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NewCorporateReservationCard(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    vehicles: List<CorporateFleetVehicle>,
    selectedVehicle: CorporateFleetVehicle?,
    vehicleMenuOpen: Boolean,
    onVehicleMenuChange: (Boolean) -> Unit,
    onVehicleSelected: (CorporateFleetVehicle) -> Unit,
    driverName: String,
    onDriverNameChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    purpose: String,
    onPurposeChange: (String) -> Unit,
    startMillis: Long,
    endMillis: Long,
    onStartDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    saving: Boolean,
    message: String,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = titleColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Nova reserva", color = titleColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Escolha veiculo, horario e destino.", color = subColor, fontSize = 13.sp)
            }
        }

        Box {
            OutlinedButton(
                onClick = { onVehicleMenuChange(true) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(selectedVehicle?.displayName().orEmpty().ifBlank { "Selecionar veiculo" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = vehicleMenuOpen, onDismissRequest = { onVehicleMenuChange(false) }) {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(text = { Text(vehicle.displayName()) }, onClick = { onVehicleSelected(vehicle) })
                }
            }
        }

        OutlinedTextField(
            value = driverName,
            onValueChange = onDriverNameChange,
            label = { Text("Responsavel / motorista") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = destination,
            onValueChange = onDestinationChange,
            label = { Text("Destino") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = purpose,
            onValueChange = onPurposeChange,
            label = { Text("Motivo da viagem") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        ReservationDateTimeRow("Retirada", startMillis, onStartDateClick, onStartTimeClick)
        ReservationDateTimeRow("Devolucao", endMillis, onEndDateClick, onEndTimeClick)

        Button(
            onClick = onSave,
            enabled = !saving && vehicles.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (saving) "Salvando..." else "Agendar veiculo", fontWeight = FontWeight.Bold)
        }
        if (message.isNotBlank()) {
            Text(message, color = if (message.startsWith("Falha") || message.startsWith("Este")) Color(0xFFDC2626) else dimColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ReservationQrValidationDialog(
    reservation: CorporateReservation,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var showCameraScanner by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(34.dp))
            Text(
                if (reservation.status == "em_uso") "Validar devolucao" else "Validar retirada",
                color = PHTitle,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                "Escaneie o QR gerado na dashboard para este veiculo. A retirada ou devolucao sera marcada automaticamente.",
                color = PHSub,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Button(onClick = { showCameraScanner = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ler QR do veiculo", fontWeight = FontWeight.Bold)
            }
            if (scanMessage.isNotBlank()) {
                Text(scanMessage, color = Color(0xFFDC2626), fontSize = 12.sp)
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar")
            }
        }
    }
    if (showCameraScanner) {
        CameraCapturaDialog(
            onDismiss = { showCameraScanner = false },
            onFotoCapturada = { result ->
                val qrText = result.qrCodeUrl.orEmpty().trim()
                showCameraScanner = false
                if (qrText.isBlank()) {
                    scanMessage = "Nenhum QR Code valido foi lido."
                } else {
                    onConfirm(qrText)
                }
            }
        )
    }
}

@Composable
private fun ReservationDateTimeRow(
    label: String,
    millis: Long,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDateClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text(formatReservationDate(millis), maxLines = 1)
            }
            OutlinedButton(onClick = onTimeClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text(formatReservationTime(millis), maxLines = 1)
            }
        }
    }
}

private fun saveCorporateReservation(
    reservationId: String,
    user: com.google.firebase.auth.FirebaseUser,
    companyId: String,
    vehicle: CorporateFleetVehicle,
    driverName: String,
    destination: String,
    purpose: String,
    startMillis: Long,
    endMillis: Long,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val companyRef = db.collection("companies").document(companyId)
    val reservationRef = companyRef.collection("reservations").document(reservationId)
    val userName = user.displayName ?: user.email.orEmpty()

    companyRef.collection("members").document(user.uid).set(
        mapOf(
            "uid" to user.uid,
            "name" to userName,
            "email" to user.email.orEmpty().lowercase(Locale.getDefault()),
            "role" to "motorista",
            "active" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        SetOptions.merge()
    ).addOnSuccessListener {
        reservationRef.set(
            mapOf(
                "id" to reservationRef.id,
                "companyId" to companyId,
                "vehicleId" to vehicle.id,
                "vehicleName" to vehicle.displayName(),
                "driverName" to driverName,
                "driverUid" to user.uid,
                "destination" to destination,
                "purpose" to purpose,
                "startsAt" to Date(startMillis),
                "endsAt" to Date(endMillis),
                "status" to "reservada",
                "source" to "android",
                "createdBy" to user.uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener {
        db.collection("users").document(user.uid).set(
            mapOf(
                "email" to user.email.orEmpty(),
                "displayName" to userName,
                "activeCompanyId" to companyId,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        if (companyId == "personal_${user.uid}") {
            companyRef.set(
                mapOf(
                    "name" to (if (userName.contains("@")) "Minha frota" else "Frota de $userName"),
                    "ownerUid" to user.uid,
                    "plan" to "frota",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }
        onSuccess()
        }.addOnFailureListener { onError(it) }
    }.addOnFailureListener { onError(it) }
}

private fun updateCorporateReservationTripStatus(
    companyId: String,
    reservation: CorporateReservation,
    qrText: String,
    user: com.google.firebase.auth.FirebaseUser,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val reservationRef = db.collection("companies")
        .document(companyId)
        .collection("reservations")
        .document(reservation.id)
    val tripRef = db.collection("companies")
        .document(companyId)
        .collection("trips")
        .document(reservation.id)
    val payload = mutableMapOf<String, Any>(
        "status" to reservation.status,
        "lastQrText" to qrText.trim(),
        "lastQrValidatedBy" to user.uid,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (reservation.status == "em_uso") {
        payload["tripStartedAt"] = Date(reservation.tripStartedAtMillis ?: System.currentTimeMillis())
        payload["tripStartedBy"] = user.uid
    }
    if (reservation.status == "finalizada") {
        payload["tripEndedAt"] = Date(reservation.tripEndedAtMillis ?: System.currentTimeMillis())
        payload["tripEndedBy"] = user.uid
    }

    val tripPayload = mutableMapOf<String, Any>(
        "id" to reservation.id,
        "companyId" to companyId,
        "reservationId" to reservation.id,
        "vehicleId" to reservation.vehicleId,
        "vehicleName" to reservation.vehicleName,
        "driverName" to reservation.driverName,
        "driverUid" to user.uid,
        "destination" to reservation.destination,
        "status" to if (reservation.status == "finalizada") "concluida" else "em_andamento",
        "updatedAt" to FieldValue.serverTimestamp()
    )
    reservation.tripStartedAtMillis?.let { tripPayload["startedAt"] = Date(it) }
    reservation.tripEndedAtMillis?.let { tripPayload["endedAt"] = Date(it) }

    db.batch()
        .apply {
            set(reservationRef, payload, SetOptions.merge())
            set(tripRef, tripPayload, SetOptions.merge())
        }
        .commit()
        .addOnFailureListener { onError(it) }
}

private fun resolveCorporateCompanyId(
    user: com.google.firebase.auth.FirebaseUser,
    fallbackCompanyId: String,
    onResolved: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(user.uid)
    val normalizedEmail = user.email.orEmpty().trim().lowercase(Locale.getDefault())
    userRef.get()
        .addOnSuccessListener { userDoc ->
            val activeCompanyId = userDoc.getString("activeCompanyId")
            if (!activeCompanyId.isNullOrBlank() && !activeCompanyId.startsWith("personal_")) {
                onResolved(activeCompanyId)
                return@addOnSuccessListener
            }
            if (normalizedEmail.isBlank()) {
                onResolved(fallbackCompanyId)
                return@addOnSuccessListener
            }
            db.collection("userInvites")
                .document(corporateEmailKey(normalizedEmail))
                .collection("companies")
                .limit(1)
                .get()
                .addOnSuccessListener { invites ->
                    val invite = invites.documents.firstOrNull()
                    val invitedCompanyId = invite?.getString("companyId")
                    if (invitedCompanyId.isNullOrBlank()) {
                        onResolved(activeCompanyId ?: fallbackCompanyId)
                        return@addOnSuccessListener
                    }
                    db.collection("companies").document(invitedCompanyId)
                        .collection("members").document(user.uid)
                        .set(
                            mapOf(
                                "uid" to user.uid,
                                "email" to normalizedEmail,
                                "name" to (user.displayName ?: normalizedEmail),
                                "role" to (invite.getString("role") ?: "motorista"),
                                "active" to true,
                                "acceptedAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                    userRef.set(
                        mapOf(
                            "email" to normalizedEmail,
                            "displayName" to (user.displayName ?: ""),
                            "activeCompanyId" to invitedCompanyId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    onResolved(invitedCompanyId)
                }
                .addOnFailureListener { onResolved(activeCompanyId ?: fallbackCompanyId) }
        }
        .addOnFailureListener { onResolved(fallbackCompanyId) }
}

private fun corporateEmailKey(email: String): String {
    return email.trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9._-]"), "_")
}

private fun loadLocalCorporateReservations(context: Context): List<CorporateReservation> {
    val raw = context.getSharedPreferences(CORPORATE_RESERVATIONS_PREFS, Context.MODE_PRIVATE)
        .getString(CORPORATE_RESERVATIONS_KEY, "")
        .orEmpty()
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val type = object : TypeToken<List<CorporateReservation>>() {}.type
        Gson().fromJson<List<CorporateReservation>>(raw, type).orEmpty()
    }.getOrDefault(emptyList())
}

private fun saveLocalCorporateReservations(context: Context, reservations: List<CorporateReservation>) {
    context.getSharedPreferences(CORPORATE_RESERVATIONS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(CORPORATE_RESERVATIONS_KEY, Gson().toJson(reservations))
        .apply()
}

private fun rangesOverlap(startA: Long, endA: Long, startB: Long, endB: Long): Boolean {
    return startA < endB && startB < endA
}

private fun startOfDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun reservationCalendarDays(anchorMillis: Long): List<Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = anchorMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val firstDayOffset = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    calendar.add(Calendar.DAY_OF_MONTH, -firstDayOffset)
    return List(42) {
        val value = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        value
    }
}

private fun reservationQrMatches(reservation: CorporateReservation, qrText: String): Boolean {
    val normalized = normalizedReservationText(qrComparableText(qrText))
    if (normalized.isBlank()) return false
    val vehicleId = normalizedReservationText(reservation.vehicleId)
    val reservationId = normalizedReservationText(reservation.id)
    val vehicleName = normalizedReservationText(reservation.vehicleName)
    return normalized == vehicleId ||
        normalized == reservationId ||
        normalized == vehicleName ||
        normalized.contains(vehicleId) ||
        normalized.contains(reservationId) ||
        (vehicleName.isNotBlank() && normalized.contains(vehicleName))
}

private fun corporateVehicleQrMatches(vehicle: CorporateFleetVehicle, qrText: String): Boolean {
    val normalized = normalizedReservationText(qrComparableText(qrText))
    if (normalized.isBlank()) return false
    val vehicleId = normalizedReservationText(vehicle.id)
    val vehicleName = normalizedReservationText(vehicle.name)
    val plate = normalizedReservationText(vehicle.plate)
    return normalized == vehicleId ||
        normalized.contains(vehicleId) ||
        (vehicleName.isNotBlank() && normalized.contains(vehicleName)) ||
        (plate.isNotBlank() && normalized.contains(plate))
}

private fun normalizedReservationText(value: String): String {
    return value.trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "")
}

private fun qrComparableText(value: String): String {
    val raw = value.trim()
    val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    return decoded
        .replace("\\\"", "\"")
        .replace("_", "")
        .replace("-", "")
}

private fun reservationStatusLabel(status: String): String = when (status) {
    "em_uso" -> "Em uso"
    "finalizada" -> "Finalizada"
    "cancelada" -> "Cancelada"
    else -> "Reservada"
}

private fun nextRoundedHourMillis(): Long {
    return Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatReservationDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

private fun formatReservationTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatReservationMillis(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
