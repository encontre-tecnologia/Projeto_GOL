package br.com.gui.carlembrete

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            "Calendario da frota para reservas e retiradas.",
            "Fleet calendar for reservations and pickups."
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

    var reservationFormOpen by remember { mutableStateOf(false) }

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
                IconButton(
                    onClick = {
                        if (module == CorporateFleetModule.RESERVATIONS && reservationFormOpen) {
                            reservationFormOpen = false
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = tr("Voltar", "Back"),
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                val displayTitle = if (module == CorporateFleetModule.RESERVATIONS && reservationFormOpen) {
                    tr("Nova reserva", "New reservation")
                } else {
                    title
                }
                val displaySubtitle = if (module == CorporateFleetModule.RESERVATIONS && reservationFormOpen) {
                    tr("Escolha veiculo, horario e destino.", "Choose vehicle, time and destination.")
                } else {
                    subtitle
                }
                Text(displayTitle, color = titleColor, fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(
                    displaySubtitle,
                    color = subColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (module == CorporateFleetModule.RESERVATIONS) {
                CorporateReservationsContent(
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
                    showNewReservationForm = reservationFormOpen,
                    onShowNewReservationFormChange = { reservationFormOpen = it }
                )
            } else if (module == CorporateFleetModule.MAINTENANCE) {
                CorporateFleetAlertsContent(
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
private fun CorporateFleetAlertsContent(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color
) {
    val authUser = FirebaseAuth.getInstance().currentUser
    val fallbackCompanyId = remember(authUser?.uid) { authUser?.uid?.let { "personal_$it" } }
    var companyId by remember(authUser?.uid) { mutableStateOf<String?>(null) }
    val alerts = remember { mutableStateListOf<CorporateFleetAlert>() }
    val context = LocalContext.current
    var loadMessage by remember { mutableStateOf("") }

    DisposableEffect(authUser?.uid, authUser?.email) {
        val user = authUser
        if (user != null && fallbackCompanyId != null) {
            resolveCorporateCompanyId(user, fallbackCompanyId) { companyId = it }
        }
        onDispose { }
    }

    DisposableEffect(companyId) {
        val activeCompanyId = companyId
        if (activeCompanyId.isNullOrBlank()) {
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection("companies")
                .document(activeCompanyId)
                .collection("alerts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        loadMessage = "Nao foi possivel carregar os avisos da empresa."
                        return@addSnapshotListener
                    }
                    val incoming = snapshot?.documents.orEmpty().map { document ->
                        CorporateFleetAlert(
                            id = document.id,
                            title = document.getString("title").orEmpty().ifBlank { "Aviso da empresa" },
                            description = document.getString("description").orEmpty(),
                            vehicleName = document.getString("vehicleName").orEmpty(),
                            maintenanceType = document.getString("maintenanceType").orEmpty().ifBlank { "Outros" },
                            priority = document.getString("priority").orEmpty().ifBlank { "media" },
                            status = document.getString("status").orEmpty().ifBlank { "aberto" },
                            dueDateMillis = document.getTimestamp("dueDate")?.toDate()?.time,
                            dueTime = document.getString("dueTime").orEmpty().ifBlank { "09:00" },
                            dueOdometerKm = document.getLong("dueOdometerKm")?.toInt() ?: 0,
                            createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        )
                    }.sortedByDescending { it.createdAtMillis }
                    alerts.clear()
                    alerts.addAll(incoming)
                    CorporateFleetAlertNotifications.sync(context, activeCompanyId, incoming)
                    loadMessage = ""
                }
            onDispose { registration.remove() }
        }
    }

    val openAlerts = alerts.filter { it.status != "resolvido" }
    val resolvedAlerts = alerts.filter { it.status == "resolvido" }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE6F6F3))
                .border(BorderStroke(1.dp, Color(0xFFB9E3DA)), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Avisos da empresa", color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Separados dos seus avisos pessoais e sincronizados pela organizacao.", color = subColor, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Text("${openAlerts.size} aberto(s)", color = Color(0xFF0F766E), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }

        if (loadMessage.isNotBlank()) Text(loadMessage, color = Color(0xFFDC2626), fontSize = 13.sp)
        if (openAlerts.isEmpty() && loadMessage.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("Nenhum aviso corporativo em aberto", color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Quando a empresa publicar um aviso pela dashboard, ele aparecera aqui automaticamente.", color = dimColor, fontSize = 13.sp, lineHeight = 18.sp)
            }
        } else {
            openAlerts.forEach { alert -> CorporateFleetAlertCard(alert, cardBg, cardBorder, titleColor, subColor) }
        }
        if (resolvedAlerts.isNotEmpty()) {
            Text("Resolvidos recentemente", color = dimColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            resolvedAlerts.take(5).forEach { alert -> CorporateFleetAlertCard(alert, cardBg, cardBorder, titleColor, subColor) }
        }
    }
}

@Composable
private fun CorporateFleetAlertCard(
    alert: CorporateFleetAlert,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color
) {
    val priorityColor = when (alert.priority) {
        "critica" -> Color(0xFFDC2626)
        "alta" -> Color(0xFFEA580C)
        "baixa" -> Color(0xFF2563EB)
        else -> Color(0xFFD97706)
    }
    val priorityLabel = when (alert.priority) {
        "critica" -> "Critica"
        "alta" -> "Alta"
        "baixa" -> "Baixa"
        else -> "Media"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(alert.maintenanceType, color = priorityColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text(alert.title, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Text(if (alert.status == "resolvido") "Resolvido" else priorityLabel, color = priorityColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        if (alert.vehicleName.isNotBlank()) Text(alert.vehicleName, color = Color(0xFF0F766E), fontSize = 12.sp)
        val limits = listOfNotNull(
            alert.dueDateMillis?.let { "Notificar: ${formatReservationDate(it)} ${alert.dueTime}" },
            alert.dueOdometerKm.takeIf { it > 0 }?.let { "KM limite: ${it.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1.")}" }
        ).joinToString("  •  ")
        if (limits.isNotBlank()) Text(limits, color = Color(0xFF64748B), fontSize = 12.sp)
        if (alert.description.isNotBlank()) Text(alert.description, color = subColor, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun CorporateReservationsContent(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    showNewReservationForm: Boolean,
    onShowNewReservationFormChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val authUser = FirebaseAuth.getInstance().currentUser
    val fallbackCompanyId = remember(authUser?.uid) { authUser?.uid?.let { "personal_$it" } }
    var companyId by remember(authUser?.uid) { mutableStateOf<String?>(null) }
    val vehicles = remember { mutableStateListOf<CorporateFleetVehicle>() }
    val speedEvents = remember { mutableStateListOf<CorporateSpeedEvent>() }
    var vehiclesLoaded by remember { mutableStateOf(false) }
    val reservations = remember { mutableStateListOf<CorporateReservation>().apply { addAll(loadLocalCorporateReservations(context)) } }
    var selectedVehicleId by remember { mutableStateOf("") }
    var vehicleMenuOpen by remember { mutableStateOf(false) }
    var driverName by remember { mutableStateOf(authUser?.displayName ?: authUser?.email.orEmpty()) }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var editingReservationId by remember { mutableStateOf<String?>(null) }
    var reservationPendingDeletion by remember { mutableStateOf<CorporateReservation?>(null) }
    var startMillis by remember { mutableStateOf(nextRoundedHourMillis()) }
    var endMillis by remember { mutableStateOf(nextRoundedHourMillis() + 60 * 60 * 1000L) }
    var saving by remember { mutableStateOf(false) }
    var locatingOrigin by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val bookableVehicles = vehicles.filter { it.status == "disponivel" || it.status == "reservado" }
    val selectedVehicle = bookableVehicles.firstOrNull { it.id == selectedVehicleId } ?: bookableVehicles.firstOrNull()
    var selectedDayMillis by remember { mutableStateOf(startOfDayMillis(System.currentTimeMillis())) }
    var dayDialogMillis by remember { mutableStateOf<Long?>(null) }
    var qrReservation by remember { mutableStateOf<CorporateReservation?>(null) }
    var activeTripSummaryReservation by remember { mutableStateOf<CorporateReservation?>(null) }
    var showSignatureManager by remember { mutableStateOf(false) }
    var myTripsFilter by remember { mutableStateOf(MyTripsFilter.ALL) }
    var speedLimitKmh by remember { mutableStateOf(100) }
    var speedToleranceKmh by remember { mutableStateOf(10) }
    val currentMonthDays = remember(selectedDayMillis, reservations.size) { reservationCalendarDays(selectedDayMillis) }
    val dayDialogReservations = dayDialogMillis?.let { day ->
        reservations.filter { reservationCoversDay(it, day) }.sortedBy { it.startsAtMillis }
    }.orEmpty()

    fun fetchCurrentOrigin() {
        locatingOrigin = true
        coroutineScope.launch {
            val address = withContext(Dispatchers.IO) { resolveCurrentAddress(context) }
            locatingOrigin = false
            if (address != null) {
                origin = address
            } else {
                message = "Nao foi possivel obter a localizacao atual. Ative o GPS ou digite manualmente."
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCurrentOrigin()
        } else {
            message = "Permissao de localizacao negada. Digite a partida manualmente."
        }
    }

    fun requestCurrentOrigin() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            fetchCurrentOrigin()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
            val companySettingsRegistration = db
                .collection("companies")
                .document(activeCompanyId)
                .addSnapshotListener { snapshot, _ ->
                    speedLimitKmh = (snapshot?.getLong("speedLimitKmh") ?: 100L).toInt().coerceIn(40, 160)
                    speedToleranceKmh = (snapshot?.getLong("speedToleranceKmh") ?: 10L).toInt().coerceIn(0, 40)
                }
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
                            odometerKm = doc.getLong("odometerKm")?.toInt() ?: doc.getLong("kmAtual")?.toInt() ?: 0,
                            maxConcurrentReservations = doc.getLong("maxConcurrentReservations")?.toInt()?.coerceAtLeast(1) ?: 1
                        )
                    }
                    val bookable = vehicles.filter { it.status == "disponivel" || it.status == "reservado" }
                    if (selectedVehicleId.isBlank() || bookable.none { it.id == selectedVehicleId }) {
                        selectedVehicleId = bookable.firstOrNull()?.id.orEmpty()
                    }
                    vehiclesLoaded = true
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
                    if (snapshot == null) return@addSnapshotListener
                    val incoming = snapshot.documents.mapNotNull { doc ->
                        val startsAt = doc.getTimestamp("startsAt")?.toDate()?.time ?: return@mapNotNull null
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
                            tripEndedAtMillis = doc.getTimestamp("tripEndedAt")?.toDate()?.time,
                            pickupSignature = doc.getString("pickupSignature").orEmpty(),
                            returnSignature = doc.getString("returnSignature").orEmpty(),
                            origin = doc.getString("origin").orEmpty()
                        )
                    }
                    // Evita "piscar" a lista: um snapshot vazio vindo do cache local (antes do servidor confirmar)
                    // nao deve apagar reservas ja exibidas; so um snapshot vazio do servidor e confirmado.
                    if (incoming.isEmpty() && snapshot.metadata.isFromCache && reservations.isNotEmpty()) {
                        return@addSnapshotListener
                    }
                    reservations.clear()
                    reservations.addAll(incoming)
                }
            val speedEventRegistration = db
                .collection("companies")
                .document(activeCompanyId)
                .collection("speedEvents")
                .orderBy("occurredAt", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    speedEvents.clear()
                    snapshot.documents.mapNotNullTo(speedEvents) { doc ->
                        CorporateSpeedEvent(
                            id = doc.id,
                            tripId = doc.getString("tripId").orEmpty(),
                            reservationId = doc.getString("reservationId").orEmpty(),
                            vehicleId = doc.getString("vehicleId").orEmpty(),
                            speedKmh = doc.getLong("speedKmh")?.toInt() ?: 0,
                            speedLimitKmh = doc.getLong("speedLimitKmh")?.toInt() ?: speedLimitKmh,
                            toleranceKmh = doc.getLong("toleranceKmh")?.toInt() ?: speedToleranceKmh,
                            durationSeconds = doc.getLong("durationSeconds")?.toInt() ?: 0,
                            occurredAtMillis = doc.getTimestamp("occurredAt")?.toDate()?.time
                        )
                    }
                }
            onDispose {
                companySettingsRegistration.remove()
                vehicleRegistration.remove()
                registration.remove()
                speedEventRegistration.remove()
            }
        }
    }

    fun shiftCalendarMonth(delta: Int) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDayMillis
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        selectedDayMillis = startOfDayMillis(calendar.timeInMillis)
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
            startMillis <= System.currentTimeMillis() -> {
                message = "Escolha uma retirada em data e horario futuros."
                return
            }
            reservations.count { it.id != editingReservationId && it.vehicleId == vehicle.id && it.status in setOf("reservada", "em_uso") && rangesOverlap(startMillis, endMillis, it.startsAtMillis, it.endsAtMillis) } >= vehicle.maxConcurrentReservations.coerceAtLeast(1) -> {
                message = "Este veiculo ja atingiu o limite de reservas simultaneas nesse horario."
                return
            }
        }

        saving = true
        message = ""
        val reservationId = editingReservationId ?: UUID.randomUUID().toString()
        val localReservation = CorporateReservation(
            id = reservationId,
            vehicleId = vehicle.id,
            vehicleName = vehicle.displayName(),
            driverName = driverName.ifBlank { authUser.email.orEmpty() },
            destination = destination,
            startsAtMillis = startMillis,
            endsAtMillis = endMillis,
            status = "reservada",
            origin = origin
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
            origin = origin,
            destination = destination,
            startMillis = startMillis,
            endMillis = endMillis,
            onSuccess = {
                saving = false
                message = if (editingReservationId == null) "Reserva criada. Ela ja aparece no dashboard web." else "Reserva atualizada."
                editingReservationId = null
                origin = ""
                destination = ""
                selectedDayMillis = startOfDayMillis(startMillis)
                onShowNewReservationFormChange(false)
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

    fun confirmQrForReservation(reservation: CorporateReservation, qrText: String, signature: String) {
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
            tripEndedAtMillis = if (startingTrip) reservation.tripEndedAtMillis else now,
            pickupSignature = if (startingTrip) signature else reservation.pickupSignature.orEmpty(),
            returnSignature = if (startingTrip) reservation.returnSignature.orEmpty() else signature
        )
        val localUpdated = reservations.map { if (it.id == reservation.id) updated else it }.sortedBy { it.startsAtMillis }
        reservations.clear()
        reservations.addAll(localUpdated)
        saveLocalCorporateReservations(context, localUpdated)
        qrReservation = null
        message = if (startingTrip) "Viagem iniciada por QR Code." else "Viagem finalizada por QR Code."
        if (startingTrip) {
            CorporateTripTrackingService.start(
                context = context,
                companyId = activeCompanyId,
                reservationId = reservation.id,
                vehicleId = reservation.vehicleId,
                vehicleName = reservation.vehicleName,
                driverName = reservation.driverName
            )
        } else {
            CorporateTripTrackingService.finish(
                context = context,
                companyId = activeCompanyId,
                reservationId = reservation.id,
                vehicleId = reservation.vehicleId
            )
        }
        updateCorporateReservationTripStatus(
            companyId = activeCompanyId,
            reservation = updated,
            qrText = qrText,
            signature = signature,
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
                vehicles = bookableVehicles,
                selectedVehicle = selectedVehicle,
                vehicleMenuOpen = vehicleMenuOpen,
                onVehicleMenuChange = { vehicleMenuOpen = it },
                onVehicleSelected = { vehicle ->
                    selectedVehicleId = vehicle.id
                    vehicleMenuOpen = false
                },
                driverName = driverName,
                onDriverNameChange = { driverName = it },
                origin = origin,
                onOriginChange = { origin = it },
                onUseCurrentLocation = { requestCurrentOrigin() },
                locatingOrigin = locatingOrigin,
                destination = destination,
                onDestinationChange = { destination = it },
                startMillis = startMillis,
                endMillis = endMillis,
                onStartDateClick = { openDatePicker(startMillis) { updated -> startMillis = updated; if (endMillis <= startMillis) endMillis = startMillis + 60 * 60 * 1000L } },
                onStartTimeClick = { openTimePicker(startMillis) { updated -> startMillis = updated; if (endMillis <= startMillis) endMillis = startMillis + 60 * 60 * 1000L } },
                onEndDateClick = { openDatePicker(endMillis) { updated -> endMillis = updated } },
                onEndTimeClick = { openTimePicker(endMillis) { updated -> endMillis = updated } },
                saving = saving,
                message = message,
                isEditing = editingReservationId != null,
                onSave = { createReservation() }
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
                vehicles = bookableVehicles,
                isFleetUnavailable = vehiclesLoaded && bookableVehicles.isEmpty(),
                selectedDayMillis = selectedDayMillis,
                onDaySelected = { day ->
                    selectedDayMillis = day
                    dayDialogMillis = day
                },
                onPrevMonth = { shiftCalendarMonth(-1) },
                onNextMonth = { shiftCalendarMonth(1) }
            )
            if (message.isNotBlank()) {
                Text(
                    message,
                    color = if (message.contains("nao", ignoreCase = true) || message.contains("Falha", ignoreCase = true)) Color(0xFFDC2626) else dimColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            val myIdentity = authUser?.displayName?.ifBlank { null } ?: authUser?.email.orEmpty()
            val allMyReservations = remember(reservations.toList(), myIdentity) {
                reservations.filter { item ->
                    item.status != "cancelada" &&
                        normalizedReservationText(item.driverName) == normalizedReservationText(myIdentity)
                }.sortedWith(
                    compareBy<CorporateReservation> { reservation ->
                        when (reservation.status) {
                            "em_uso" -> 0
                            "reservada" -> 1
                            else -> 2
                        }
                    }.thenByDescending { reservation ->
                        if (reservation.status == "finalizada") {
                            reservation.tripEndedAtMillis ?: reservation.startsAtMillis
                        } else {
                            reservation.startsAtMillis
                        }
                    }
                )
            }
            val myReservations = remember(allMyReservations, myTripsFilter) {
                allMyReservations.filter { reservation ->
                    when (myTripsFilter) {
                        MyTripsFilter.ALL -> true
                        MyTripsFilter.IN_USE -> reservation.status == "em_uso"
                        MyTripsFilter.RESERVED -> reservation.status == "reservada"
                        MyTripsFilter.FINISHED -> reservation.status == "finalizada"
                    }
                }
            }
            if (allMyReservations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Minhas viagens", color = titleColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        TextButton(onClick = { showSignatureManager = true }) { Text("Minha assinatura", fontSize = 12.sp) }
                    }
                    MyTripsFilterRow(
                        selected = myTripsFilter,
                        counts = MyTripsFilterCounts(
                            all = allMyReservations.size,
                            inUse = allMyReservations.count { it.status == "em_uso" },
                            reserved = allMyReservations.count { it.status == "reservada" },
                            finished = allMyReservations.count { it.status == "finalizada" }
                        ),
                        titleColor = titleColor,
                        subColor = subColor,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onSelected = { myTripsFilter = it }
                    )
                    if (myReservations.isEmpty()) {
                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Nenhuma viagem nesse filtro.",
                                color = subColor,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                    myReservations.forEach { myReservation ->
                        val isToday = startOfDayMillis(myReservation.startsAtMillis) == startOfDayMillis(System.currentTimeMillis())
                        CorporateReservationDayCard(
                            reservation = myReservation,
                            titleColor = titleColor,
                            subColor = subColor,
                            dimColor = dimColor,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            dateLabel = reservationDayLabel(myReservation.startsAtMillis),
                            showCheckIn = isToday,
                            onQrAction = { qrReservation = myReservation },
                            onOpenTripSummary = if (myReservation.status == "em_uso") {
                                { activeTripSummaryReservation = myReservation }
                            } else null,
                            onEdit = if (myReservation.status == "reservada") {
                                {
                                    selectedVehicleId = myReservation.vehicleId
                                    driverName = myReservation.driverName
                                    origin = myReservation.origin
                                    destination = myReservation.destination
                                    startMillis = myReservation.startsAtMillis
                                    endMillis = myReservation.endsAtMillis
                                    editingReservationId = myReservation.id
                                    message = ""
                                    onShowNewReservationFormChange(true)
                                }
                            } else null,
                            onDelete = if (myReservation.status == "reservada") {
                                { reservationPendingDeletion = myReservation }
                            } else null
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    dayDialogMillis?.let { day ->
        ReservationDayDialog(
            dayMillis = day,
            reservations = dayDialogReservations,
            isFleetFull = isDayFleetFull(day, bookableVehicles, reservations),
            isFleetUnavailable = vehiclesLoaded && bookableVehicles.isEmpty(),
            isPastDay = day < startOfDayMillis(System.currentTimeMillis()),
            titleColor = titleColor,
            subColor = subColor,
            dimColor = dimColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onDismiss = { dayDialogMillis = null },
            onNewReservation = {
                dayDialogMillis = null
                if (bookableVehicles.isEmpty()) {
                    message = "Nenhum veiculo disponivel para reserva no momento."
                } else {
                    startMillis = selectedDayMillis + 9 * 60 * 60 * 1000L
                    endMillis = startMillis + 60 * 60 * 1000L
                    onShowNewReservationFormChange(true)
                    message = ""
                }
            },
            onQrAction = { reservation ->
                dayDialogMillis = null
                qrReservation = reservation
            }
        )
    }

    qrReservation?.let { reservation ->
        ReservationQrValidationDialog(
            reservation = reservation,
            userId = authUser?.uid.orEmpty(),
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            onDismiss = { qrReservation = null },
            onConfirm = { qrText, signature -> confirmQrForReservation(reservation, qrText, signature) }
        )
    }

    activeTripSummaryReservation?.let { reservation ->
        val events = speedEventsForReservation(reservation, speedEvents, System.currentTimeMillis())
        ActiveTripSummaryDialog(
            reservation = reservation,
            speedEvents = events,
            speedLimitKmh = speedLimitKmh,
            speedToleranceKmh = speedToleranceKmh,
            titleColor = titleColor,
            subColor = subColor,
            dimColor = dimColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onDismiss = { activeTripSummaryReservation = null }
        )
    }

    if (showSignatureManager && authUser != null) {
        FleetSignatureDialog(
            initialSignature = loadFleetSignature(context, authUser.uid),
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            onDismiss = { showSignatureManager = false },
            onSave = { signature ->
                saveFleetSignature(context, authUser.uid, signature)
                showSignatureManager = false
                message = "Assinatura atualizada para as proximas viagens."
            }
        )
    }

    reservationPendingDeletion?.let { reservation ->
        AlertDialog(
            onDismissRequest = { reservationPendingDeletion = null },
            title = { Text("Apagar reserva?") },
            text = { Text("Esta reserva sera removida do calendario e da dashboard.") },
            confirmButton = {
                TextButton(onClick = {
                    reservationPendingDeletion = null
                    val activeCompanyId = companyId
                    if (activeCompanyId.isNullOrBlank()) {
                        message = "Empresa nao encontrada para apagar a reserva."
                        return@TextButton
                    }
                    val previous = reservations.toList()
                    reservations.removeAll { it.id == reservation.id }
                    saveLocalCorporateReservations(context, reservations)
                    FirebaseFirestore.getInstance().collection("companies").document(activeCompanyId)
                        .collection("reservations").document(reservation.id).delete()
                        .addOnSuccessListener { message = "Reserva apagada." }
                        .addOnFailureListener {
                            reservations.clear()
                            reservations.addAll(previous)
                            saveLocalCorporateReservations(context, previous)
                            message = "Nao foi possivel apagar a reserva: ${it.localizedMessage}"
                        }
                }) { Text("Apagar", color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { reservationPendingDeletion = null }) { Text("Cancelar") } }
        )
    }
}

private enum class MyTripsFilter(val label: String) {
    ALL("Todas"),
    IN_USE("Em uso"),
    RESERVED("Reservadas"),
    FINISHED("Finalizadas")
}

private data class MyTripsFilterCounts(
    val all: Int,
    val inUse: Int,
    val reserved: Int,
    val finished: Int
)

private data class CorporateSpeedEvent(
    val id: String,
    val tripId: String = "",
    val reservationId: String = "",
    val vehicleId: String = "",
    val speedKmh: Int = 0,
    val speedLimitKmh: Int = 100,
    val toleranceKmh: Int = 10,
    val durationSeconds: Int = 0,
    val occurredAtMillis: Long? = null
)

private fun isDarkReservationSurface(color: Color): Boolean = color.luminance() < 0.45f

private fun reservationSoftSurface(cardBg: Color): Color =
    if (isDarkReservationSurface(cardBg)) Color(0xFF111827) else Color(0xFFF8FAFC)

private fun reservationElevatedSurface(cardBg: Color): Color =
    if (isDarkReservationSurface(cardBg)) Color(0xFF172033) else Color.White

private fun reservationMutedSurface(cardBg: Color): Color =
    if (isDarkReservationSurface(cardBg)) Color(0xFF1F2937) else Color(0xFFF1F5F9)

@Composable
private fun MyTripsFilterRow(
    selected: MyTripsFilter,
    counts: MyTripsFilterCounts,
    titleColor: Color,
    subColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onSelected: (MyTripsFilter) -> Unit
) {
    val filters = listOf(MyTripsFilter.ALL, MyTripsFilter.IN_USE, MyTripsFilter.RESERVED, MyTripsFilter.FINISHED)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val count = when (filter) {
                MyTripsFilter.ALL -> counts.all
                MyTripsFilter.IN_USE -> counts.inUse
                MyTripsFilter.RESERVED -> counts.reserved
                MyTripsFilter.FINISHED -> counts.finished
            }
            MyTripsFilterChip(
                label = filter.label,
                count = count,
                selected = selected == filter,
                titleColor = titleColor,
                subColor = subColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                onClick = { onSelected(filter) }
            )
        }
    }
}

@Composable
private fun MyTripsFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    titleColor: Color,
    subColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF2563EB) else reservationElevatedSurface(cardBg)
    val border = if (selected) Color(0xFF2563EB) else cardBorder
    val textColor = if (selected) Color.White else titleColor
    val countBg = if (selected) Color.White.copy(alpha = 0.18f) else reservationSoftSurface(cardBg)
    val countColor = if (selected) Color.White else Color(0xFF2563EB)
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .height(38.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .defaultMinSize(minWidth = 24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(countBg)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(count.toString(), color = countColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun dayAccentColor(dayReservations: List<CorporateReservation>, isUnavailable: Boolean): Color = when {
    isUnavailable -> Color(0xFFDC2626)
    dayReservations.any { it.status == "em_uso" } -> Color(0xFFEA580C)
    dayReservations.any { it.status == "reservada" } -> Color(0xFF2563EB)
    dayReservations.isNotEmpty() -> Color(0xFF16A34A)
    else -> Color.Transparent
}

private fun isDayFleetFull(dayMillis: Long, vehicles: List<CorporateFleetVehicle>, reservations: List<CorporateReservation>): Boolean {
    if (vehicles.isEmpty()) return false
    val dayStart = dayMillis
    val dayEnd = dayMillis + 24 * 60 * 60 * 1000L
    return vehicles.all { vehicle ->
        val overlapping = reservations.count {
            it.vehicleId == vehicle.id && it.status in setOf("reservada", "em_uso") && rangesOverlap(dayStart, dayEnd, it.startsAtMillis, it.endsAtMillis)
        }
        overlapping >= vehicle.maxConcurrentReservations.coerceAtLeast(1)
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
    vehicles: List<CorporateFleetVehicle>,
    isFleetUnavailable: Boolean,
    selectedDayMillis: Long,
    onDaySelected: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(reservationMutedSurface(cardBg)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = tr("Mes anterior", "Previous month"), tint = titleColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Agenda da frota", color = titleColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(monthLabel.replaceFirstChar { it.titlecase(Locale.getDefault()) }, color = subColor, fontSize = 13.sp)
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = tr("Proximo mes", "Next month"), tint = titleColor)
                }
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
                    val dayReservations = reservations.filter { reservationCoversDay(it, dayMillis) }
                    val hasReservations = dayReservations.isNotEmpty()
                    val isSelected = dayMillis == selectedDayMillis
                    val isToday = dayMillis == startOfDayMillis(System.currentTimeMillis())
                    val isPastDay = dayMillis < startOfDayMillis(System.currentTimeMillis())
                    val isUnavailable = isFleetUnavailable || isDayFleetFull(dayMillis, vehicles, reservations)
                    val hasEvents = hasReservations || isUnavailable
                    val accent = dayAccentColor(dayReservations, isUnavailable)
                    val driverInitials = dayReservations.firstOrNull()?.driverName
                        .orEmpty()
                        .trim()
                        .split(Regex("\\s+"))
                        .filter { it.isNotBlank() }
                        .let { parts ->
                            when (parts.size) {
                                0 -> "?"
                                1 -> parts.first().take(1)
                                else -> "${parts.first().take(1)}${parts.last().take(1)}"
                            }.uppercase(Locale.getDefault())
                        }
                    val bg = when {
                        isPastDay -> reservationMutedSurface(cardBg).copy(alpha = if (isDarkReservationSurface(cardBg)) 0.55f else 1f)
                        isSelected -> Color(0xFF0F766E)
                        hasEvents -> accent.copy(alpha = 0.16f)
                        isToday -> if (isDarkReservationSurface(cardBg)) Color(0xFF0F2F2D) else Color(0xFFF0FDFA)
                        else -> reservationSoftSurface(cardBg)
                    }
                    val borderColor = when {
                        isPastDay -> cardBorder.copy(alpha = 0.55f)
                        isSelected -> Color(0xFF0F766E)
                        hasEvents -> accent.copy(alpha = 0.55f)
                        isToday -> Color(0xFF99E0D1)
                        else -> cardBorder
                    }
                    val textColor = when {
                        isPastDay -> dimColor.copy(alpha = 0.62f)
                        isSelected -> Color.White
                        else -> titleColor
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(BorderStroke(if (hasEvents || isSelected) 1.6.dp else 1.dp, borderColor), RoundedCornerShape(12.dp))
                            .clickable { onDaySelected(dayMillis) }
                            .padding(7.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(dayMillis)), color = textColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            if (!hasReservations && hasEvents) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(if (isSelected) Color.White else accent)
                                )
                            }
                        }
                        if (hasReservations) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .size(19.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (isPastDay) Color(0xFFCBD5E1) else if (isSelected) Color.White else accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    driverInitials,
                                    color = if (isPastDay) Color(0xFF475569) else if (isSelected) accent else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationDayDialog(
    dayMillis: Long,
    reservations: List<CorporateReservation>,
    isFleetFull: Boolean,
    isFleetUnavailable: Boolean,
    isPastDay: Boolean,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onDismiss: () -> Unit,
    onNewReservation: () -> Unit,
    onQrAction: (CorporateReservation) -> Unit
) {
    val dayLabel = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date(dayMillis))
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = titleColor, modifier = Modifier.size(18.dp))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(reservationSoftSurface(cardBg)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Reservas do dia", color = subColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text(
                        dayLabel.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                        color = titleColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isPastDay) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(reservationMutedSurface(cardBg))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = dimColor, modifier = Modifier.size(18.dp))
                    Text(
                        "Este dia ja passou. As reservas exibidas abaixo ficam apenas para consulta.",
                        color = subColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            } else if (isFleetFull || isFleetUnavailable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Text(
                        if (isFleetUnavailable) {
                            "Nenhum veiculo esta disponivel para reserva no momento. A frota esta bloqueada ou em manutencao."
                        } else {
                            "Todos os veiculos disponiveis ja tem reservas neste dia. Nao e possivel criar uma nova reserva."
                        },
                        color = Color(0xFF991B1B),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            if (reservations.isEmpty()) {
                Text("Nenhum veiculo reservado neste dia.", color = subColor, fontSize = 14.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reservations.forEach { reservation ->
                        CorporateReservationDayCard(
                            reservation = reservation,
                            titleColor = titleColor,
                            subColor = subColor,
                            dimColor = dimColor,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            onQrAction = { onQrAction(reservation) }
                        )
                    }
                }
            }

            if (!isPastDay && !isFleetFull && !isFleetUnavailable) {
                OutlinedButton(onClick = onNewReservation, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nova reserva neste dia", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActiveTripSummaryDialog(
    reservation: CorporateReservation,
    speedEvents: List<CorporateSpeedEvent>,
    speedLimitKmh: Int,
    speedToleranceKmh: Int,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onDismiss: () -> Unit
) {
    val now = System.currentTimeMillis()
    val startedAt = reservation.tripStartedAtMillis ?: reservation.startsAtMillis
    val elapsedMillis = (now - startedAt).coerceAtLeast(0L)
    val threshold = speedLimitKmh + speedToleranceKmh
    val infractions = speedEvents.filter { event ->
        val eventThreshold = (event.speedLimitKmh + event.toleranceKmh).takeIf { it > 0 } ?: threshold
        event.speedKmh >= eventThreshold
    }
    val maxSpeed = infractions.maxOfOrNull { it.speedKmh } ?: speedEvents.maxOfOrNull { it.speedKmh } ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = cardBg,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, cardBorder),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text("VIAGEM EM ANDAMENTO", color = Color(0xFF2563EB), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        Text(reservation.vehicleName, color = titleColor, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(reservation.driverName, color = subColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(reservationMutedSurface(cardBg))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = titleColor, modifier = Modifier.size(18.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TripSummaryMetric(
                        icon = Icons.Default.AccessTime,
                        label = "Tempo",
                        value = formatTripElapsed(elapsedMillis),
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    TripSummaryMetric(
                        icon = Icons.Default.Speed,
                        label = "Infrações",
                        value = infractions.size.toString(),
                        color = if (infractions.isEmpty()) Color(0xFF15803D) else Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(reservationSoftSurface(cardBg))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryLine("Limite da empresa", "$speedLimitKmh km/h", titleColor, subColor)
                    SummaryLine("Tolerancia", "+$speedToleranceKmh km/h", titleColor, subColor)
                    SummaryLine("Acima de", "$threshold km/h", titleColor, subColor)
                    SummaryLine("Maior velocidade", if (maxSpeed > 0) "$maxSpeed km/h" else "Sem registro", titleColor, subColor)
                    SummaryLine("Retirada", formatReservationMillis(startedAt), titleColor, subColor)
                    if (reservation.destination.isNotBlank()) SummaryLine("Destino", reservation.destination, titleColor, subColor)
                }

                if (infractions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                .background(if (isDarkReservationSurface(cardBg)) Color(0xFF451A1A) else Color(0xFFFEF2F2))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Excessos detectados", color = Color(0xFFB91C1C), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        infractions.take(3).forEach { event ->
                            Text(
                                "${event.speedKmh} km/h${event.occurredAtMillis?.let { " - ${formatReservationMillis(it)}" }.orEmpty()}",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp
                            )
                        }
                        if (infractions.size > 3) {
                            Text("+ ${infractions.size - 3} outro(s) evento(s)", color = Color(0xFF991B1B), fontSize = 12.sp)
                        }
                    }
                } else {
                    Text("Nenhum excesso registrado ate agora.", color = dimColor, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun TripSummaryMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(label.uppercase(Locale.getDefault()), color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SummaryLine(label: String, value: String, titleColor: Color, subColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = subColor, fontSize = 12.sp)
        Text(value, color = titleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun CorporateReservationDayCard(
    reservation: CorporateReservation,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onQrAction: () -> Unit,
    dateLabel: String? = null,
    showCheckIn: Boolean = true,
    onOpenTripSummary: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val statusColor = when (reservation.status) {
        "em_uso" -> Color(0xFFEA580C)
        "finalizada" -> Color(0xFF15803D)
        "suspensa_manutencao" -> Color(0xFFDC2626)
        else -> Color(0xFF0369A1)
    }
    val statusBackground = when (reservation.status) {
        "em_uso" -> Color(0xFFFFEDD5)
        "finalizada" -> Color(0xFFDCFCE7)
        "suspensa_manutencao" -> Color(0xFFFEE2E2)
        else -> Color(0xFFE0F2FE)
    }
    val statusText = reservationStatusLabel(reservation.status)
    val tripDetail = reservation.destination.ifBlank { "Destino nao informado" }
    val cardModifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clip(RoundedCornerShape(18.dp))
        .background(reservationElevatedSurface(cardBg))
        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
    Row(
        modifier = if (onOpenTripSummary != null) cardModifier.clickable(onClick = onOpenTripSummary) else cardModifier
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(statusColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (reservation.status == "finalizada") Icons.Default.CheckCircle else Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (dateLabel != null) {
                        Text(
                            dateLabel.uppercase(Locale.getDefault()),
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp
                        )
                    } else {
                        Text("RESERVA CORPORATIVA", color = dimColor, fontSize = 10.sp, letterSpacing = 0.4.sp)
                    }
                    Text(
                        "${formatReservationTime(reservation.startsAtMillis)} - ${formatReservationTime(reservation.endsAtMillis)}",
                        color = subColor,
                        fontSize = 12.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(statusBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Text(
            reservation.vehicleName,
            color = titleColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(reservationSoftSurface(cardBg))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                Text(reservation.driverName, color = subColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                Text(tripDetail, color = subColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (onEdit != null || onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onEdit?.let {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(reservationMutedSurface(cardBg))
                            .clickable(onClick = it)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(14.dp))
                        Text("Editar", color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                onDelete?.let {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0xFFFEE2E2))
                            .clickable(onClick = it)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                        Text("Apagar", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (reservation.status == "suspensa_manutencao") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF2F2))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                Text(
                    "Reserva suspensa: este veiculo aguarda manutencao e sera liberado pela gestao da frota.",
                    color = Color(0xFFB91C1C),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        } else if (reservation.status != "finalizada") {
            if (showCheckIn) {
                OutlinedButton(onClick = onQrAction, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (reservation.status == "em_uso") "Escanear QR para devolver" else "Escanear QR para retirar")
                }
            } else {
                Text(
                    "O check-in fica disponivel no dia da retirada.",
                    color = dimColor,
                    fontSize = 12.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDarkReservationSurface(cardBg)) Color(0xFF143323) else Color(0xFFF0FDF4))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                Text(
                    "Viagem concluida${reservation.tripEndedAtMillis?.let { " as ${formatReservationTime(it)}" }.orEmpty()}",
                    color = Color(0xFF15803D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val pickupSignature = reservation.pickupSignature.orEmpty()
            val returnSignature = reservation.returnSignature.orEmpty()
            if (pickupSignature.isNotBlank() || returnSignature.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(reservationSoftSurface(cardBg))
                        .padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("ASSINATURAS DA VIAGEM", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (pickupSignature.isNotBlank()) {
                            FleetSignatureHistoryStamp(
                                label = "Retirada",
                                timeMillis = reservation.tripStartedAtMillis,
                                signature = pickupSignature,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                titleColor = titleColor,
                                subColor = subColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (returnSignature.isNotBlank()) {
                            FleetSignatureHistoryStamp(
                                label = "Devolucao",
                                timeMillis = reservation.tripEndedAtMillis,
                                signature = returnSignature,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                titleColor = titleColor,
                                subColor = subColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun FleetSignatureHistoryStamp(
    label: String,
    timeMillis: Long?,
    signature: String,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(reservationElevatedSurface(cardBg))
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Text(
            timeMillis?.let { "${formatReservationDate(it)} - ${formatReservationTime(it)}" } ?: "Horario nao registrado",
            color = subColor,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FleetSignaturePreview(signature = signature, modifier = Modifier.fillMaxWidth().height(42.dp))
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
    origin: String,
    onOriginChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    locatingOrigin: Boolean,
    destination: String,
    onDestinationChange: (String) -> Unit,
    startMillis: Long,
    endMillis: Long,
    onStartDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    saving: Boolean,
    message: String,
    isEditing: Boolean,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box {
            OutlinedButton(
                onClick = { onVehicleMenuChange(true) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    selectedVehicle?.displayName().orEmpty().ifBlank { "Selecionar veiculo" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = dimColor, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = vehicleMenuOpen, onDismissRequest = { onVehicleMenuChange(false) }) {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(text = { Text(vehicle.displayName()) }, onClick = { onVehicleSelected(vehicle) })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = driverName,
                onValueChange = onDriverNameChange,
                label = { Text("Responsavel / motorista") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = origin,
                onValueChange = onOriginChange,
                label = { Text("Partida") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onUseCurrentLocation, enabled = !locatingOrigin) {
                        if (locatingOrigin) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Usar localizacao atual", tint = Color(0xFF0284C7))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text("Destino") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        ReservationFormSection(label = "Periodo") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReservationDateTimeRow(
                    label = "Retirada",
                    millis = startMillis,
                    cardBg = cardBg,
                    titleColor = titleColor,
                    onDateClick = onStartDateClick,
                    onTimeClick = onStartTimeClick
                )
                ReservationDateTimeRow(
                    label = "Devolucao",
                    millis = endMillis,
                    cardBg = cardBg,
                    titleColor = titleColor,
                    onDateClick = onEndDateClick,
                    onTimeClick = onEndTimeClick
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = !saving && vehicles.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (saving) "Salvando..." else if (isEditing) "Salvar alteracoes" else "Agendar veiculo",
                fontWeight = FontWeight.Bold
            )
        }
        if (message.isNotBlank()) {
            Text(message, color = if (message.startsWith("Falha") || message.startsWith("Este")) Color(0xFFDC2626) else dimColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ReservationFormSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label.uppercase(Locale.getDefault()),
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        )
        content()
    }
}

@Composable
private fun ReservationQrValidationDialog(
    reservation: CorporateReservation,
    userId: String,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val context = LocalContext.current
    var showCameraScanner by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("") }
    var pendingQrText by remember { mutableStateOf<String?>(null) }
    var showSignatureEditor by remember { mutableStateOf(false) }
    var savedSignature by remember(userId) { mutableStateOf(loadFleetSignature(context, userId)) }
    DisposableEffect(userId) {
        if (userId.isBlank()) return@DisposableEffect onDispose { }
        val registration = FirebaseFirestore.getInstance().collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                val remoteSignature = snapshot?.getString("fleetSignature").orEmpty()
                if (remoteSignature.isNotBlank() && remoteSignature != savedSignature) {
                    savedSignature = remoteSignature
                    context.getSharedPreferences("fleet_signature_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("signature_$userId", remoteSignature)
                        .apply()
                }
            }
        onDispose { registration.remove() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(34.dp))
            Text(
                if (reservation.status == "em_uso") "Validar devolucao" else "Validar retirada",
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Escaneie o QR gerado na dashboard para este veiculo. A retirada ou devolucao sera marcada automaticamente.",
                color = subColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (savedSignature.isBlank()) {
                Button(
                    onClick = { showSignatureEditor = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cadastrar assinatura", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Cadastre sua assinatura para liberar a leitura do QR Code.",
                    color = subColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Button(onClick = { showCameraScanner = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ler QR do veiculo", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { showSignatureEditor = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Alterar assinatura")
                }
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
                } else if (savedSignature.isBlank()) {
                    pendingQrText = qrText
                    showSignatureEditor = true
                } else {
                    onConfirm(qrText, savedSignature)
                }
            }
        )
    }
    if (showSignatureEditor) {
        FleetSignatureDialog(
            initialSignature = savedSignature,
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            onDismiss = { showSignatureEditor = false },
            onSave = { signature ->
                saveFleetSignature(context, userId, signature)
                savedSignature = signature
                showSignatureEditor = false
                pendingQrText?.let { qr ->
                    pendingQrText = null
                    onConfirm(qr, signature)
                }
            }
        )
    }
}

@Composable
private fun FleetSignatureDialog(
    initialSignature: String,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var signature by remember(initialSignature) { mutableStateOf(initialSignature) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Assinatura da retirada", color = titleColor, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(
                "Esta assinatura sera usada nas proximas retiradas e devolucoes por QR Code.",
                color = subColor,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            FleetSignaturePad(
                value = signature,
                cardBg = cardBg,
                cardBorder = cardBorder,
                onValueChange = { signature = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { signature = "" }) { Text("Limpar") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar") }
                Button(
                    onClick = { onSave(signature) },
                    enabled = signature.isNotBlank(),
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Salvar") }
            }
        }
    }
}

@Composable
private fun ReservationDateTimeRow(
    label: String,
    millis: Long,
    cardBg: Color,
    titleColor: Color,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(reservationSoftSurface(cardBg))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDateClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatReservationDate(millis), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            }
            OutlinedButton(onClick = onTimeClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatReservationTime(millis), maxLines = 1, fontSize = 13.sp)
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
    origin: String,
    destination: String,
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
                "origin" to origin,
                "destination" to destination,
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
    signature: String,
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
    val vehicleRef = db.collection("companies")
        .document(companyId)
        .collection("vehicles")
        .document(reservation.vehicleId)

    fun persistQrEvent(odometerStartKm: Long? = null) {
        val payload = mutableMapOf<String, Any>(
            "status" to reservation.status,
            "lastQrText" to qrText.trim(),
            "lastQrValidatedBy" to user.uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (reservation.status == "em_uso") {
            payload["tripStartedAt"] = Date(reservation.tripStartedAtMillis ?: System.currentTimeMillis())
            payload["tripStartedBy"] = user.uid
            payload["pickupSignature"] = signature
        }
        if (reservation.status == "finalizada") {
            payload["tripEndedAt"] = Date(reservation.tripEndedAtMillis ?: System.currentTimeMillis())
            payload["tripEndedBy"] = user.uid
            payload["returnSignature"] = signature
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
        reservation.pickupSignature.orEmpty().takeIf { it.isNotBlank() }?.let { tripPayload["pickupSignature"] = it }
        reservation.returnSignature.orEmpty().takeIf { it.isNotBlank() }?.let { tripPayload["returnSignature"] = it }
        odometerStartKm?.let { tripPayload["odometerStartKm"] = it }

        db.batch()
            .apply {
                set(reservationRef, payload, SetOptions.merge())
                set(tripRef, tripPayload, SetOptions.merge())
            }
            .commit()
            .addOnFailureListener { onError(it) }
    }

    if (reservation.status == "em_uso") {
        vehicleRef.get()
            .addOnSuccessListener { vehicle ->
                val odometer = vehicle.getLong("odometerKm") ?: vehicle.getLong("kmAtual") ?: 0L
                persistQrEvent(odometer)
            }
            .addOnFailureListener { onError(it) }
    } else {
        persistQrEvent()
    }
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

private fun reservationCoversDay(reservation: CorporateReservation, dayMillis: Long): Boolean {
    val startDay = startOfDayMillis(reservation.startsAtMillis)
    val endDay = startOfDayMillis(reservation.endsAtMillis)
    return dayMillis in startDay..endDay
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

private fun reservationDayLabel(millis: Long): String {
    val day = startOfDayMillis(millis)
    val today = startOfDayMillis(System.currentTimeMillis())
    val diffDays = (day - today) / (24 * 60 * 60 * 1000L)
    return when (diffDays) {
        0L -> "Hoje"
        1L -> "Amanha"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}

private fun resolveCurrentAddress(context: Context): String? {
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return null

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    var bestLocation: Location? = null
    providers.forEach { provider ->
        val location = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: return@forEach
        if (bestLocation == null || location.time > bestLocation!!.time) {
            bestLocation = location
        }
    }
    val location = bestLocation ?: return null

    return runCatching {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        @Suppress("DEPRECATION")
        val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        listOfNotNull(
            listOfNotNull(address?.thoroughfare, address?.subThoroughfare).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
            address?.subLocality?.ifBlank { null } ?: address?.subAdminArea?.ifBlank { null },
            address?.locality?.ifBlank { null }
        ).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
    }.getOrNull()
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

private fun speedEventsForReservation(
    reservation: CorporateReservation,
    events: List<CorporateSpeedEvent>,
    nowMillis: Long
): List<CorporateSpeedEvent> {
    val start = reservation.tripStartedAtMillis ?: reservation.startsAtMillis
    val end = reservation.tripEndedAtMillis ?: nowMillis
    return events
        .filter { event ->
            event.reservationId == reservation.id ||
                event.tripId == reservation.id ||
                (
                    event.vehicleId == reservation.vehicleId &&
                        event.occurredAtMillis != null &&
                        event.occurredAtMillis in start..end
                )
        }
        .distinctBy { it.id }
        .sortedByDescending { it.occurredAtMillis ?: 0L }
}

private fun formatTripElapsed(elapsedMillis: Long): String {
    val totalMinutes = (elapsedMillis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}min"
        hours > 0L -> "${hours}h"
        else -> "${minutes}min"
    }
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
    "suspensa_manutencao" -> "Suspensa por manutencao"
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
