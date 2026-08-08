package br.com.gui.carlembrete

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.net.URLDecoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var reservationHistoryOpen by remember { mutableStateOf(false) }
    val isReservationsModule = module == CorporateFleetModule.RESERVATIONS

    BackHandler(enabled = isReservationsModule && (reservationFormOpen || reservationHistoryOpen)) {
        if (reservationFormOpen) reservationFormOpen = false else reservationHistoryOpen = false
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
                // Sem imePadding a area de scroll continua com a altura da tela cheia quando o
                // teclado abre, e o campo focado (Destino, no fim do formulario) fica embaixo dele.
                // O verticalScroll rola sozinho ate o campo em foco, mas so consegue mirar direito
                // se a viewport souber que encolheu. Mesmo padrao de EditarCarroScreen.
                .imePadding()
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
                        when {
                            isReservationsModule && reservationFormOpen -> reservationFormOpen = false
                            isReservationsModule && reservationHistoryOpen -> reservationHistoryOpen = false
                            else -> onDismiss()
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
                val headerIcon = when {
                    !isReservationsModule -> icon
                    reservationFormOpen -> Icons.Default.AddCircle
                    reservationHistoryOpen -> Icons.Default.History
                    else -> Icons.Default.CalendarToday
                }
                val headerAccent = when {
                    isReservationsModule && reservationHistoryOpen -> Color(0xFF15803D)
                    else -> Color(0xFF0284C7)
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(headerAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(headerIcon, contentDescription = null, tint = headerAccent, modifier = Modifier.size(28.dp))
                }
                val displayTitle = when {
                    !isReservationsModule -> title
                    reservationFormOpen -> tr("Nova reserva", "New reservation")
                    reservationHistoryOpen -> tr("Histórico de viagens", "Trip history")
                    else -> tr("Agenda da frota", "Fleet schedule")
                }
                val displaySubtitle = when {
                    !isReservationsModule -> subtitle
                    reservationFormOpen -> tr("Escolha veiculo, horario e destino.", "Choose vehicle, time and destination.")
                    reservationHistoryOpen -> tr(
                        "Viagens finalizadas, com assinaturas de retirada e devolucao.",
                        "Finished trips, with pickup and return signatures."
                    )
                    else -> tr(
                        "Viagens em andamento e as proximas reservas da sua frota.",
                        "Active trips and your fleet's upcoming reservations."
                    )
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
                    onShowNewReservationFormChange = { reservationFormOpen = it },
                    showTripHistory = reservationHistoryOpen,
                    onShowTripHistoryChange = { reservationHistoryOpen = it }
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
    onShowNewReservationFormChange: (Boolean) -> Unit,
    showTripHistory: Boolean,
    onShowTripHistoryChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val authUser = FirebaseAuth.getInstance().currentUser
    val fallbackCompanyId = remember(authUser?.uid) { authUser?.uid?.let { "personal_$it" } }
    var companyId by remember(authUser?.uid) { mutableStateOf<String?>(null) }
    val vehicles = remember { mutableStateListOf<CorporateFleetVehicle>() }
    var vehiclesLoaded by remember { mutableStateOf(false) }
    val reservations = remember { mutableStateListOf<CorporateReservation>().apply { addAll(loadLocalCorporateReservations(context)) } }
    var selectedVehicleId by remember { mutableStateOf("") }
    var vehicleMenuOpen by remember { mutableStateOf(false) }
    var driverName by remember { mutableStateOf(authUser?.displayName ?: authUser?.email.orEmpty()) }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var editingReservationId by remember { mutableStateOf<String?>(null) }
    /*
     * A reserva que este formulario acabou de gravar. Ela entra na lista local antes de o servidor
     * confirmar, e sem guardar o id a propria reserva recem-criada aparecia como conflito: o
     * formulario acusava "0 de 1 livre" e "ocupado com <o proprio motorista>" no instante entre
     * salvar e fechar. Nao da para reaproveitar editingReservationId — ao criar, ele e nulo.
     */
    var savedReservationId by remember { mutableStateOf<String?>(null) }
    var reservationPendingDeletion by remember { mutableStateOf<CorporateReservation?>(null) }
    var startMillis by remember { mutableStateOf(nextRoundedHourMillis()) }
    var endMillis by remember { mutableStateOf(nextRoundedHourMillis() + 60 * 60 * 1000L) }
    var saving by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }
    var locatingOrigin by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val bookableVehicles = vehicles.filter { it.status == "disponivel" || it.status == "reservado" }
    val selectedVehicle = bookableVehicles.firstOrNull { it.id == selectedVehicleId } ?: bookableVehicles.firstOrNull()
    var qrReservation by remember { mutableStateOf<CorporateReservation?>(null) }
    var activeTripSummaryReservation by remember { mutableStateOf<CorporateReservation?>(null) }
    var showSignatureManager by remember { mutableStateOf(false) }
    var historyPeriod by remember { mutableStateOf(TripHistoryPeriod.ALL) }
    var occupancyDialogOpen by remember { mutableStateOf(false) }
    val expiredMarkedIds = remember { mutableSetOf<String>() }
    // Relogio da agenda: mantem o cronometro das viagens ativas e as contagens "em X min" atualizados.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

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
                            origin = doc.getString("origin").orEmpty(),
                            trackingStatus = doc.getString("trackingStatus").orEmpty(),
                            trackingBatteryPercent = doc.getLong("trackingBatteryPercent")?.toInt(),
                            trackingLastLocationAtMillis = doc.getTimestamp("trackingLastLocationAt")?.toDate()?.time,
                            trackingNeedsReview = doc.getBoolean("trackingNeedsReview") == true,
                            // A dashboard web grava pickup/returnOdometerKm; o app tambem aceita os nomes usados nas viagens.
                            pickupOdometerKm = doc.getLong("pickupOdometerKm") ?: doc.getLong("odometerStartKm"),
                            returnOdometerKm = doc.getLong("returnOdometerKm") ?: doc.getLong("odometerEndKm")
                        )
                    }
                    // Evita "piscar" a lista: um snapshot vazio vindo do cache local (antes do servidor confirmar)
                    // nao deve apagar reservas ja exibidas; so um snapshot vazio do servidor e confirmado.
                    if (incoming.isEmpty() && snapshot.metadata.isFromCache && reservations.isNotEmpty()) {
                        return@addSnapshotListener
                    }
                    reservations.clear()
                    reservations.addAll(incoming)
                    expireStaleReservations(
                        db = db,
                        companyId = activeCompanyId,
                        reservations = incoming,
                        currentUser = authUser,
                        alreadyMarked = expiredMarkedIds
                    )
                }
            onDispose {
                vehicleRegistration.remove()
                registration.remove()
            }
        }
    }

    fun createReservation() {
        attemptedSave = true
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
            origin.isBlank() -> {
                message = "Informe o local de partida."
                return
            }
            destination.isBlank() -> {
                message = "Informe o destino."
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
        savedReservationId = reservationId
        val reservationsBeforeSave = reservations.toList()
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
                savedReservationId = null
                origin = ""
                destination = ""
                onShowNewReservationFormChange(false)
            },
            onError = { error ->
                saving = false
                // Conflito: alguem confirmou a mesma vaga primeiro. A reserva local precisa voltar atras.
                if (error.message == RESERVATION_CONFLICT) {
                    reservations.clear()
                    reservations.addAll(reservationsBeforeSave)
                    saveLocalCorporateReservations(context, reservationsBeforeSave)
                    // A reserva local foi desfeita, entao nao ha mais o que ignorar na ocupacao —
                    // e o conflito que resta agora e real, de outra pessoa, e precisa aparecer.
                    savedReservationId = null
                    message = "Este veiculo acabou de ser reservado por outra pessoa nesse horario. Escolha outro horario ou veiculo."
                    return@saveCorporateReservation
                }
                // Nos demais erros a reserva ficou gravada localmente e o formulario continua aberto
                // mostrando o aviso de sincronizacao, entao savedReservationId permanece: sem isso o
                // formulario voltaria a acusar conflito contra a reserva que o proprio usuario criou.
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

    fun confirmQrForReservation(reservation: CorporateReservation, qrText: String, signature: String, odometerKm: Long? = null) {
        val activeCompanyId = companyId
        val user = authUser
        if (user == null || activeCompanyId.isNullOrBlank()) {
            message = "Entre na sua conta para validar retirada e devolucao."
            return
        }
        val vehicleMatchedByQr = vehicles.firstOrNull { corporateVehicleQrMatches(it, qrText, activeCompanyId) }
        val qrMatchesReservation = reservationQrMatches(reservation, qrText, activeCompanyId) ||
            (vehicleMatchedByQr != null && vehicleMatchedByQr.id == reservation.vehicleId)
        if (!qrMatchesReservation) {
            message = "QR Code nao confere com este veiculo ou com esta reserva."
            return
        }
        val now = System.currentTimeMillis()
        val startingTrip = reservation.status == "reservada"
        val newStatus = if (startingTrip) "em_uso" else "finalizada"
        // O KM do odometro e obrigatorio nas duas pontas: e a unica fonte de distancia da
        // viagem — o monitoramento por GPS foi removido de proposito, o fluxo oficial e
        // escanear o QR, assinar e informar o KM do painel.
        if (odometerKm == null) {
            message = if (startingTrip) {
                "Informe o KM do odometro na retirada."
            } else {
                "Informe o KM do odometro na devolucao."
            }
            return
        }
        val previousOdometerKm = reservation.pickupOdometerKm
            ?: vehicles.firstOrNull { it.id == reservation.vehicleId }?.odometerKm?.toLong()
        if (!startingTrip && previousOdometerKm != null && odometerKm < previousOdometerKm) {
            message = "O KM da devolucao nao pode ser menor que o da retirada ($previousOdometerKm km)."
            return
        }
        val updated = reservation.copy(
            status = newStatus,
            tripStartedAtMillis = if (startingTrip) now else reservation.tripStartedAtMillis,
            tripEndedAtMillis = if (startingTrip) reservation.tripEndedAtMillis else now,
            pickupSignature = if (startingTrip) signature else reservation.pickupSignature.orEmpty(),
            returnSignature = if (startingTrip) reservation.returnSignature.orEmpty() else signature,
            pickupOdometerKm = if (startingTrip) odometerKm else reservation.pickupOdometerKm,
            returnOdometerKm = if (startingTrip) reservation.returnOdometerKm else odometerKm
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
            signature = signature,
            odometerKm = odometerKm,
            user = user,
            onError = { error ->
                message = "Atualizado no app, mas ainda nao sincronizou: ${error.localizedMessage}"
            }
        )
    }

    val myIdentity = authUser?.displayName?.ifBlank { null } ?: authUser?.email.orEmpty()
    val myReservations = remember(reservations.toList(), myIdentity) {
        reservations.filter { item ->
            item.status != "cancelada" &&
                normalizedReservationText(item.driverName) == normalizedReservationText(myIdentity)
        }
    }
    // A agenda mostra so o que ainda vai acontecer: em andamento primeiro, depois as reservas futuras.
    val activeTrips = remember(myReservations) {
        myReservations.filter { it.status == "em_uso" }.sortedBy { it.tripStartedAtMillis ?: it.startsAtMillis }
    }
    val upcomingTrips = remember(myReservations) {
        myReservations
            .filter { it.status == "reservada" || it.status == "suspensa_manutencao" }
            .sortedBy { it.startsAtMillis }
    }
    // Viagens finalizadas (e reservas que venceram sem retirada) saem da agenda e ficam no historico.
    val finishedTrips = remember(myReservations) {
        myReservations.filter { it.status == "finalizada" || it.status == "expirada" }
            .sortedByDescending { it.tripEndedAtMillis ?: it.endsAtMillis }
    }

    fun openNewReservationForm() {
        if (bookableVehicles.isEmpty()) {
            message = "Nenhum veiculo disponivel para reserva no momento."
            return
        }
        editingReservationId = null
        savedReservationId = null
        origin = ""
        destination = ""
        message = ""
        attemptedSave = false
        startMillis = nextRoundedHourMillis()
        endMillis = startMillis + 60 * 60 * 1000L
        onShowNewReservationFormChange(true)
    }

    // Livre ou ocupado depende do periodo escolhido, entao recalcula a cada mudanca de horario.
    // A reserva do proprio formulario nunca conta como conflito, esteja ela em edicao ou recem-gravada.
    val ownReservationId = editingReservationId ?: savedReservationId
    val formAvailabilities = remember(vehicles.toList(), reservations.toList(), startMillis, endMillis, ownReservationId) {
        vehicles.map { vehicle ->
            vehicleAvailabilityFor(
                vehicle = vehicle,
                reservations = reservations,
                startMillis = startMillis,
                endMillis = endMillis,
                ignoreReservationId = ownReservationId
            )
        }.sortedWith(compareByDescending<VehicleAvailability> { it.isFree }.thenBy { it.vehicle.name })
    }

    fun startEditingReservation(reservation: CorporateReservation) {
        selectedVehicleId = reservation.vehicleId
        driverName = reservation.driverName
        origin = reservation.origin
        destination = reservation.destination
        startMillis = reservation.startsAtMillis
        endMillis = reservation.endsAtMillis
        editingReservationId = reservation.id
        savedReservationId = null
        message = ""
        attemptedSave = false
        onShowNewReservationFormChange(true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showNewReservationForm) {
            NewCorporateReservationCard(
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor,
                dimColor = dimColor,
                availabilities = formAvailabilities,
                selectedVehicle = selectedVehicle,
                vehicleMenuOpen = vehicleMenuOpen,
                onVehicleMenuChange = { vehicleMenuOpen = it },
                onVehicleSelected = { vehicle ->
                    selectedVehicleId = vehicle.id
                    vehicleMenuOpen = false
                },
                onOpenPeriodPicker = { occupancyDialogOpen = true },
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
                saving = saving,
                message = message,
                isEditing = editingReservationId != null,
                showValidationErrors = attemptedSave,
                onSave = { createReservation() }
            )
        } else if (showTripHistory) {
            TripHistoryContent(
                trips = finishedTrips,
                period = historyPeriod,
                onPeriodChange = { historyPeriod = it },
                titleColor = titleColor,
                subColor = subColor,
                dimColor = dimColor,
                cardBg = cardBg,
                cardBorder = cardBorder
            )
            Spacer(Modifier.height(32.dp))
        } else {
            AgendaOverviewCard(
                activeCount = activeTrips.size,
                upcomingCount = upcomingTrips.size,
                freeVehicleCount = bookableVehicles.size,
                finishedCount = finishedTrips.size,
                fleetUnavailable = vehiclesLoaded && bookableVehicles.isEmpty(),
                titleColor = titleColor,
                subColor = subColor,
                dimColor = dimColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                onNewReservation = { openNewReservationForm() },
                onOpenHistory = { onShowTripHistoryChange(true) },
                onOpenSignature = { showSignatureManager = true }
            )

            if (message.isNotBlank()) {
                AgendaMessageBanner(
                    message = message,
                    isError = message.contains("nao", ignoreCase = true) || message.contains("Falha", ignoreCase = true),
                    subColor = subColor,
                    cardBg = cardBg
                )
            }

            if (activeTrips.isNotEmpty()) {
                AgendaSectionHeader(
                    title = "Em andamento",
                    detail = "${activeTrips.size} viagem(ns) acontecendo agora",
                    accent = Color(0xFFEA580C),
                    showLiveBadge = true,
                    titleColor = titleColor,
                    subColor = subColor,
                    cardBg = cardBg
                )
                activeTrips.forEach { trip ->
                    ActiveTripLiveCard(
                        reservation = trip,
                        nowMillis = nowMillis,
                        titleColor = titleColor,
                        subColor = subColor,
                        dimColor = dimColor,
                        cardBg = cardBg,
                        onOpenSummary = { activeTripSummaryReservation = trip },
                        onQrAction = { qrReservation = trip }
                    )
                }
            }

            AgendaSectionHeader(
                title = "Próximas viagens",
                detail = if (upcomingTrips.isEmpty()) "Nada agendado" else "${upcomingTrips.size} reserva(s) a caminho",
                accent = Color(0xFF2563EB),
                showLiveBadge = false,
                titleColor = titleColor,
                subColor = subColor,
                cardBg = cardBg
            )
            if (upcomingTrips.isEmpty()) {
                AgendaEmptyUpcomingCard(
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    fleetUnavailable = vehiclesLoaded && bookableVehicles.isEmpty(),
                    onNewReservation = { openNewReservationForm() }
                )
            } else {
                upcomingTrips.forEach { reservation ->
                    val isToday = startOfDayMillis(reservation.startsAtMillis) == startOfDayMillis(nowMillis)
                    UpcomingReservationCard(
                        reservation = reservation,
                        nowMillis = nowMillis,
                        titleColor = titleColor,
                        subColor = subColor,
                        dimColor = dimColor,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        showCheckIn = isToday || reservation.startsAtMillis <= nowMillis,
                        onQrAction = { qrReservation = reservation },
                        onEdit = if (reservation.status == "reservada") {
                            { startEditingReservation(reservation) }
                        } else null,
                        onDelete = if (reservation.status == "reservada") {
                            { reservationPendingDeletion = reservation }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    qrReservation?.let { reservation ->
        ReservationQrValidationDialog(
            reservation = reservation,
            userId = authUser?.uid.orEmpty(),
            lastKnownOdometerKm = vehicles.firstOrNull { it.id == reservation.vehicleId }?.odometerKm?.toLong(),
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            onDismiss = { qrReservation = null },
            onConfirm = { qrText, signature, odometerKm -> confirmQrForReservation(reservation, qrText, signature, odometerKm) }
        )
    }

    activeTripSummaryReservation?.let { reservation ->
        ActiveTripSummaryDialog(
            reservation = reservation,
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

    if (occupancyDialogOpen) {
        FleetPeriodDialog(
            startMillis = startMillis,
            endMillis = endMillis,
            vehicles = vehicles,
            reservations = reservations,
            ignoreReservationId = editingReservationId,
            titleColor = titleColor,
            subColor = subColor,
            dimColor = dimColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onDismiss = { occupancyDialogOpen = false },
            onConfirm = { newStart, newEnd ->
                startMillis = newStart
                endMillis = newEnd
                occupancyDialogOpen = false
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
                    // A viagem vinculada precisa sair junto, senao ela fica orfa no historico e no PDF da web.
                    val database = FirebaseFirestore.getInstance()
                    val companyRef = database.collection("companies").document(activeCompanyId)
                    database.batch()
                        .apply {
                            delete(companyRef.collection("reservations").document(reservation.id))
                            delete(companyRef.collection("trips").document(reservation.id))
                            // Libera a vaga no indice de ocupacao do veiculo.
                            set(
                                companyRef.collection("vehicleBookings").document(reservation.vehicleId),
                                mapOf("slots" to mapOf(reservation.id to FieldValue.delete())),
                                SetOptions.merge()
                            )
                        }
                        .commit()
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

private enum class TripHistoryPeriod(val label: String) {
    ALL("Todas"),
    THIS_MONTH("Este mês"),
    LAST_30("Últimos 30 dias")
}

/** Cabecalho de secao da agenda: faixa colorida, titulo, contagem e selo "ao vivo". */
@Composable
private fun AgendaSectionHeader(
    title: String,
    detail: String,
    accent: Color,
    showLiveBadge: Boolean,
    titleColor: Color,
    subColor: Color,
    cardBg: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(accent)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(detail, color = subColor, fontSize = 12.sp)
        }
        if (showLiveBadge) {
            val transition = rememberInfiniteTransition(label = "livePulse")
            val pulse by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "livePulseAlpha"
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.24f else 0.13f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(pulse)
                        .clip(RoundedCornerShape(99.dp))
                        .background(accent)
                )
                Text("AO VIVO", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.6.sp)
            }
        }
    }
}

/** Resumo do topo da agenda com contagens rapidas e atalhos. */
@Composable
private fun AgendaOverviewCard(
    activeCount: Int,
    upcomingCount: Int,
    freeVehicleCount: Int,
    finishedCount: Int,
    fleetUnavailable: Boolean,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onNewReservation: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSignature: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AgendaStatTile(
                icon = Icons.Default.Route,
                label = "Em andamento",
                value = activeCount.toString(),
                accent = Color(0xFFEA580C),
                cardBg = cardBg,
                labelColor = subColor,
                modifier = Modifier.weight(1f)
            )
            AgendaStatTile(
                icon = Icons.Default.CalendarToday,
                label = "Próximas",
                value = upcomingCount.toString(),
                accent = Color(0xFF2563EB),
                cardBg = cardBg,
                labelColor = subColor,
                modifier = Modifier.weight(1f)
            )
            AgendaStatTile(
                icon = Icons.Default.DirectionsCar,
                label = "Veículos livres",
                value = freeVehicleCount.toString(),
                accent = if (fleetUnavailable) Color(0xFFDC2626) else Color(0xFF0F766E),
                cardBg = cardBg,
                labelColor = subColor,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onNewReservation,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !fleetUnavailable,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nova reserva", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AgendaQuickAction(
                icon = Icons.Default.History,
                label = "Histórico",
                detail = "$finishedCount finalizada(s)",
                accent = Color(0xFF15803D),
                titleColor = titleColor,
                dimColor = dimColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                modifier = Modifier.weight(1f),
                onClick = onOpenHistory
            )
            AgendaQuickAction(
                icon = Icons.Default.Edit,
                label = "Assinatura",
                detail = "Usada na retirada",
                accent = Color(0xFF7C3AED),
                titleColor = titleColor,
                dimColor = dimColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                modifier = Modifier.weight(1f),
                onClick = onOpenSignature
            )
        }

        if (fleetUnavailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkReservationSurface(cardBg)) Color(0xFF451A1A) else Color(0xFFFEF2F2))
                    .padding(11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                Text(
                    "Nenhum veiculo disponivel para reserva agora.",
                    color = Color(0xFFB91C1C),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun AgendaStatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    cardBg: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(accent.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.18f else 0.1f))
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 24.sp)
        Text(label, color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp, maxLines = 2)
    }
}

@Composable
private fun AgendaQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    accent: Color,
    titleColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(reservationSoftSurface(cardBg))
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.24f else 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = dimColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AgendaMessageBanner(
    message: String,
    isError: Boolean,
    subColor: Color,
    cardBg: Color
) {
    val accent = if (isError) Color(0xFFDC2626) else Color(0xFF2563EB)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(accent.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.2f else 0.09f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (isError) Icons.Default.Close else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )
        Text(message, color = if (isError) accent else subColor, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

/** Estado vazio da lista de proximas viagens. */
@Composable
private fun AgendaEmptyUpcomingCard(
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    fleetUnavailable: Boolean,
    onNewReservation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(reservationElevatedSurface(cardBg))
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2563EB).copy(alpha = if (isDarkReservationSurface(cardBg)) 0.22f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
        }
        Text("Nenhuma viagem agendada", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
        Text(
            "Reserve um veiculo e ele aparece aqui com horario, destino e o QR Code da retirada.",
            color = subColor,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
        if (fleetUnavailable) {
            Text("A frota esta sem veiculos liberados no momento.", color = dimColor, fontSize = 11.sp, textAlign = TextAlign.Center)
        } else {
            OutlinedButton(onClick = onNewReservation, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Criar reserva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

private fun isDarkReservationSurface(color: Color): Boolean = color.luminance() < 0.45f

private fun reservationSoftSurface(cardBg: Color): Color =
    if (isDarkReservationSurface(cardBg)) Color(0xFF111827) else Color(0xFFF8FAFC)

private fun reservationElevatedSurface(cardBg: Color): Color =
    if (isDarkReservationSurface(cardBg)) Color(0xFF172033) else Color.White

private fun reservationMutedSurface(cardBg: Color): Color =
    if (isDarkReservationSurface(cardBg)) Color(0xFF1F2937) else Color(0xFFF1F5F9)

/** Card destacado da viagem em andamento, com cronometro vivo e atalho para devolucao. */
@Composable
private fun ActiveTripLiveCard(
    reservation: CorporateReservation,
    nowMillis: Long,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    onOpenSummary: () -> Unit,
    onQrAction: () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(0xFFEA580C)
    val darkSurface = isDarkReservationSurface(cardBg)
    val startedAt = reservation.tripStartedAtMillis ?: reservation.startsAtMillis
    val elapsed = (nowMillis - startedAt).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = if (darkSurface) 0.16f else 0.08f))
            .border(BorderStroke(1.5.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenSummary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Route, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    reservation.vehicleName,
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(reservation.driverName, color = subColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                    Text(formatTripElapsed(elapsed), color = accent, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Text("em viagem", color = dimColor, fontSize = 10.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(reservationElevatedSurface(cardBg))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AgendaInfoLine(
                icon = Icons.Default.AccessTime,
                text = "Retirada ${formatReservationMillis(startedAt)} • devolucao prevista ${formatReservationTime(reservation.endsAtMillis)}",
                color = subColor
            )
            AgendaInfoLine(
                icon = Icons.Default.Place,
                text = reservation.destination.ifBlank { "Destino nao informado" },
                color = subColor
            )
        }

        // Um botao so. O detalhe da viagem continua a um toque: o card inteiro ja abre o resumo.
        Button(
            onClick = onQrAction,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Devolver com QR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/**
 * Card de uma reserva que ainda nao virou viagem: quando, para onde, e o que dá para fazer com ela.
 *
 * Irmao do [ActiveTripLiveCard] e de proposito mais discreto — a viagem em andamento e que merece o
 * destaque laranja com cronometro. Aqui o acento e azul e o QR de retirada so aparece quando a
 * retirada e de hoje ou ja passou da hora; oferecer "retirar com QR" numa reserva de semana que vem
 * seria um botao que a validacao recusa.
 */
@Composable
private fun UpcomingReservationCard(
    reservation: CorporateReservation,
    nowMillis: Long,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    showCheckIn: Boolean,
    onQrAction: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val accent = Color(0xFF2563EB)
    val atrasada = reservation.status == "reservada" && reservation.startsAtMillis < nowMillis

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    reservation.vehicleName,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    reservation.driverName.ifBlank { "Sem motorista" },
                    color = subColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // "Nao retirada" e o unico estado que precisa gritar; o resto e informativo.
            val statusAccent = if (atrasada) Color(0xFFDC2626) else accent
            Text(
                if (atrasada) "Retirada atrasada" else reservationStatusLabel(reservation.status),
                color = statusAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(statusAccent.copy(alpha = 0.13f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(reservationElevatedSurface(cardBg))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AgendaInfoLine(
                icon = Icons.Default.AccessTime,
                text = "Retirada ${formatReservationMillis(reservation.startsAtMillis)} • devolucao ${formatReservationTime(reservation.endsAtMillis)}",
                color = subColor,
                bold = true
            )
            AgendaInfoLine(
                icon = Icons.Default.Place,
                text = reservation.destination.ifBlank { "Destino nao informado" },
                color = subColor
            )
        }

        if (showCheckIn) {
            Button(
                onClick = onQrAction,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Retirar com QR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            AgendaInfoLine(
                icon = Icons.Default.QrCodeScanner,
                text = "O QR de retirada libera no dia da reserva.",
                color = dimColor
            )
        }

        if (onEdit != null || onDelete != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                onEdit?.let {
                    AgendaPillAction(
                        icon = Icons.Default.Edit,
                        label = "Editar",
                        accent = accent,
                        background = accent.copy(alpha = 0.12f),
                        onClick = it
                    )
                }
                onDelete?.let {
                    AgendaPillAction(
                        icon = Icons.Default.Delete,
                        label = "Cancelar",
                        accent = Color(0xFFDC2626),
                        background = Color(0xFFDC2626).copy(alpha = 0.12f),
                        onClick = it
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaInfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    bold: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AgendaPillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    background: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Tela separada com o historico das viagens finalizadas. */
@Composable
private fun TripHistoryContent(
    trips: List<CorporateReservation>,
    period: TripHistoryPeriod,
    onPeriodChange: (TripHistoryPeriod) -> Unit,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color
) {
    val now = System.currentTimeMillis()
    val monthStart = remember(now) {
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    fun referenceMillis(reservation: CorporateReservation): Long =
        reservation.tripEndedAtMillis ?: reservation.endsAtMillis
    val monthCount = trips.count { referenceMillis(it) >= monthStart }
    val filtered = trips.filter { reservation ->
        when (period) {
            TripHistoryPeriod.ALL -> true
            TripHistoryPeriod.THIS_MONTH -> referenceMillis(reservation) >= monthStart
            TripHistoryPeriod.LAST_30 -> referenceMillis(reservation) >= now - 30L * 24 * 60 * 60 * 1000L
        }
    }
    val completedCount = trips.count { it.status == "finalizada" }
    val notPickedUpCount = trips.count { it.status == "expirada" }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                AgendaStatTile(
                    icon = Icons.Default.CheckCircle,
                    label = "Finalizadas",
                    value = completedCount.toString(),
                    accent = Color(0xFF15803D),
                    cardBg = cardBg,
                    labelColor = subColor,
                    modifier = Modifier.weight(1f)
                )
                AgendaStatTile(
                    icon = Icons.Default.CalendarToday,
                    label = "Neste mês",
                    value = monthCount.toString(),
                    accent = Color(0xFF2563EB),
                    cardBg = cardBg,
                    labelColor = subColor,
                    modifier = Modifier.weight(1f)
                )
                AgendaStatTile(
                    icon = Icons.Default.AccessTime,
                    label = "Não retiradas",
                    value = notPickedUpCount.toString(),
                    accent = Color(0xFF64748B),
                    cardBg = cardBg,
                    labelColor = subColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TripHistoryPeriod.entries.forEach { option ->
                val count = trips.count { reservation ->
                    when (option) {
                        TripHistoryPeriod.ALL -> true
                        TripHistoryPeriod.THIS_MONTH -> referenceMillis(reservation) >= monthStart
                        TripHistoryPeriod.LAST_30 -> referenceMillis(reservation) >= now - 30L * 24 * 60 * 60 * 1000L
                    }
                }
                AgendaFilterChip(
                    label = option.label,
                    count = count,
                    selected = option == period,
                    titleColor = titleColor,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    onClick = { onPeriodChange(option) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(reservationElevatedSurface(cardBg))
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF15803D).copy(alpha = if (isDarkReservationSurface(cardBg)) 0.22f else 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(22.dp))
                }
                Text(
                    if (trips.isEmpty()) "Nenhuma viagem finalizada ainda" else "Nenhuma viagem neste periodo",
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Ao devolver um veiculo pelo QR Code, a viagem sai da agenda e fica registrada aqui com KM e assinaturas.",
                    color = subColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            filtered
                .groupBy {
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(Date(referenceMillis(it)))
                        .replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
                }
                .forEach { (monthLabel, monthTrips) ->
                    AgendaSectionHeader(
                        title = monthLabel,
                        detail = "${monthTrips.size} viagem(ns) finalizada(s)",
                        accent = Color(0xFF15803D),
                        showLiveBadge = false,
                        titleColor = titleColor,
                        subColor = subColor,
                        cardBg = cardBg
                    )
                    monthTrips.forEach { reservation ->
                        CorporateReservationDayCard(
                            reservation = reservation,
                            titleColor = titleColor,
                            subColor = subColor,
                            dimColor = dimColor,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            dateLabel = formatReservationDate(referenceMillis(reservation)),
                            showCheckIn = false,
                            onQrAction = {}
                        )
                    }
                }
        }
    }
}

@Composable
private fun AgendaFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    titleColor: Color,
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

/**
 * Ocupacao nao e propriedade do veiculo, e do par veiculo + janela de horario. Por isso a
 * disponibilidade e calculada sempre com o periodo escolhido no formulario, e nunca por dia.
 */
private data class VehicleAvailability(
    val vehicle: CorporateFleetVehicle,
    val isFree: Boolean,
    val blockedReason: String,
    val freeFromMillis: Long?,
    val busyDriverName: String
) {
    fun statusLabel(): String = when {
        blockedReason.isNotBlank() -> blockedReason
        isFree -> "Livre"
        freeFromMillis != null -> "Ocupado ate ${formatReservationTime(freeFromMillis)}"
        else -> "Ocupado"
    }
}

private fun vehicleStatusBlockedReason(status: String): String = when (status) {
    "em_manutencao" -> "Em manutencao"
    "bloqueado" -> "Bloqueado"
    "inativo" -> "Inativo"
    else -> ""
}

private fun vehicleAvailabilityFor(
    vehicle: CorporateFleetVehicle,
    reservations: List<CorporateReservation>,
    startMillis: Long,
    endMillis: Long,
    ignoreReservationId: String?
): VehicleAvailability {
    val blockedReason = vehicleStatusBlockedReason(vehicle.status)
    val conflicts = reservations.filter { reservation ->
        reservation.vehicleId == vehicle.id &&
            reservation.id != ignoreReservationId &&
            reservation.status in setOf("reservada", "em_uso") &&
            rangesOverlap(startMillis, endMillis, reservation.startsAtMillis, reservation.endsAtMillis)
    }
    val capacity = vehicle.maxConcurrentReservations.coerceAtLeast(1)
    return VehicleAvailability(
        vehicle = vehicle,
        isFree = blockedReason.isBlank() && conflicts.size < capacity,
        blockedReason = blockedReason,
        freeFromMillis = conflicts.maxOfOrNull { it.endsAtMillis },
        busyDriverName = conflicts.firstOrNull()?.driverName.orEmpty()
    )
}



private fun withTimeOfDay(dayMillis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfDayMillis(dayMillis)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * Escolha do periodo inteiro num lugar so: calendario com contagem de veiculos livres por dia
 * (calculada com a MESMA janela de horario escolhida, nao com o dia inteiro - esse era o erro do
 * grid antigo, que marcava um dia como lotado mesmo com uma reserva de 1h sobrando o resto do dia).
 * Tocar num dia define a retirada; tocar num dia posterior estende para uma reserva de varios dias.
 * A hora de cada ponta abre o relogio nativo do Android, e os dois cartoes ficam sob o calendario.
 */
@Composable
private fun FleetPeriodDialog(
    startMillis: Long,
    endMillis: Long,
    vehicles: List<CorporateFleetVehicle>,
    reservations: List<CorporateReservation>,
    ignoreReservationId: String?,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    var draftStart by remember { mutableStateOf(startMillis) }
    var draftEnd by remember { mutableStateOf(endMillis) }
    var monthAnchor by remember { mutableStateOf(startOfDayMillis(startMillis)) }
    val today = startOfDayMillis(System.currentTimeMillis())
    val startDay = startOfDayMillis(draftStart)
    val endDay = startOfDayMillis(draftEnd)
    val isMultiDay = startDay != endDay
    val days = remember(monthAnchor) { occupancyCalendarDays(monthAnchor) }
    val anchorMonth = Calendar.getInstance().apply { timeInMillis = monthAnchor }.get(Calendar.MONTH)
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(monthAnchor))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val windowDuration = (draftEnd - draftStart).coerceAtLeast(30 * 60 * 1000L)
    val isValid = draftEnd > draftStart

    // Tocar num dia unico vira retirada; tocar num dia mais a frente amplia para varios dias.
    // Qualquer outro toque (dia anterior, ou quando ja havia um intervalo) comeca uma selecao nova.
    fun onDayTapped(tappedDay: Long) {
        val startCalendar = Calendar.getInstance().apply { timeInMillis = draftStart }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = draftEnd }
        if (!isMultiDay && tappedDay > startDay) {
            draftEnd = withTimeOfDay(tappedDay, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE))
        } else {
            val newStart = withTimeOfDay(tappedDay, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE))
            var newEnd = withTimeOfDay(tappedDay, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE))
            if (newEnd <= newStart) newEnd = newStart + 60 * 60 * 1000L
            draftStart = newStart
            draftEnd = newEnd
        }
    }

    fun pickNativeTime(current: Long, onPicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = current }
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(withTimeOfDay(current, hour, minute)) },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun pickStartTime() {
        pickNativeTime(draftStart) { picked ->
            draftStart = picked
            if (draftEnd <= draftStart) draftEnd = draftStart + 60 * 60 * 1000L
        }
    }

    fun pickEndTime() {
        pickNativeTime(draftEnd) { picked -> draftEnd = picked }
    }

    // O dialog nao pode passar da tela: cabecalho e acao ficam fixos e o calendario rola.
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Dia e horario", color = titleColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        "Toque num dia mais a frente para reservar por varios dias",
                        color = subColor,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(reservationMutedSurface(cardBg))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = titleColor, modifier = Modifier.size(16.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(reservationMutedSurface(cardBg)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    monthAnchor = Calendar.getInstance().apply {
                        timeInMillis = monthAnchor
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, -1)
                    }.timeInMillis
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = titleColor)
                }
                Text(monthLabel, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = {
                    monthAnchor = Calendar.getInstance().apply {
                        timeInMillis = monthAnchor
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, 1)
                    }.timeInMillis
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Proximo mes", tint = titleColor)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { label ->
                    Text(
                        label,
                        color = dimColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Semana inteiramente fora do mes nao entra: economiza uma linha no dialog.
            val weeks = days.chunked(7).filter { week ->
                week.any { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MONTH) == anchorMonth }
            }
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { dayMillis ->
                        val dayCalendar = Calendar.getInstance().apply { timeInMillis = dayMillis }
                        val isOutsideMonth = dayCalendar.get(Calendar.MONTH) != anchorMonth
                        val isPast = dayMillis < today
                        val startCalendar = Calendar.getInstance().apply { timeInMillis = draftStart }
                        val dayWindowStart = withTimeOfDay(dayMillis, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE))
                        val freeCount = if (isPast) 0 else vehicles.count { vehicle ->
                            vehicleAvailabilityFor(
                                vehicle = vehicle,
                                reservations = reservations,
                                startMillis = dayWindowStart,
                                endMillis = dayWindowStart + windowDuration,
                                ignoreReservationId = ignoreReservationId
                            ).isFree
                        }
                        val isStart = dayMillis == startDay
                        val isEnd = dayMillis == endDay
                        val isBetween = dayMillis > startDay && dayMillis < endDay
                        val occupancyColor = when {
                            isPast -> dimColor
                            freeCount == 0 -> Color(0xFFDC2626)
                            else -> Color(0xFF15803D)
                        }
                        val background = when {
                            isStart -> Color(0xFF2563EB)
                            isEnd -> Color(0xFF0F766E)
                            isBetween -> Color(0xFF2563EB).copy(alpha = 0.16f)
                            isPast -> reservationMutedSurface(cardBg).copy(alpha = 0.5f)
                            else -> occupancyColor.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.18f else 0.09f)
                        }
                        val isEdge = isStart || isEnd
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(background)
                                .then(
                                    // Dia que ja passou fica riscado e tracejado: nao da pra confundir com "disponivel".
                                    if (isPast) Modifier.border(BorderStroke(1.dp, cardBorder.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .alpha(if (isOutsideMonth) 0.45f else if (isPast) 0.55f else 1f)
                                .clickable(enabled = !isPast) { onDayTapped(dayMillis) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                SimpleDateFormat("d", Locale.getDefault()).format(Date(dayMillis)),
                                color = if (isEdge) Color.White else if (isPast) dimColor else titleColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                textDecoration = if (isPast) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                if (isPast) "passou" else if (freeCount == 0) "lotado" else "$freeCount livre${if (freeCount > 1) "s" else ""}",
                                color = if (isEdge) Color.White else occupancyColor,
                                fontSize = 8.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // As duas pontas ficam juntas, sob o calendario: data preenchida pelo toque no dia,
            // hora aberta pelo relogio nativo do Android ao tocar no horario.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PeriodEndCard(
                    label = "Retirada",
                    millis = draftStart,
                    accent = Color(0xFF2563EB),
                    titleColor = titleColor,
                    dimColor = dimColor,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f),
                    onTimeClick = { pickStartTime() }
                )
                PeriodEndCard(
                    label = "Devolucao",
                    millis = draftEnd,
                    accent = Color(0xFF0F766E),
                    titleColor = titleColor,
                    dimColor = dimColor,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f),
                    onTimeClick = { pickEndTime() }
                )
            }

            if (!isValid) {
                Text("A devolucao precisa ser depois da retirada.", color = Color(0xFFDC2626), fontSize = 12.sp)
            }
            }

            Button(
                onClick = { onConfirm(draftStart, draftEnd) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PeriodEndCard(
    label: String,
    millis: Long,
    accent: Color,
    titleColor: Color,
    dimColor: Color,
    cardBg: Color,
    cardBorder: Color,
    modifier: Modifier = Modifier,
    onTimeClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(reservationSoftSurface(cardBg))
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(13.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label.uppercase(Locale.getDefault()), color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.5.sp)
            Text(formatReservationDate(millis), color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.24f else 0.12f))
                .clickable(onClick = onTimeClick)
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Text(formatReservationTime(millis), color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ActiveTripSummaryDialog(
    reservation: CorporateReservation,
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
    val darkSurface = isDarkReservationSurface(cardBg)
    val activeDialogAccent = if (darkSurface) Color.White else Color(0xFF2563EB)

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
                        Text("VIAGEM EM ANDAMENTO", color = activeDialogAccent, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp)
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

                TripSummaryMetric(
                    icon = Icons.Default.AccessTime,
                    label = "Tempo",
                    value = formatTripElapsed(elapsedMillis),
                    color = activeDialogAccent,
                    labelColor = if (darkSurface) Color.White.copy(alpha = 0.72f) else Color(0xFF64748B),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(reservationSoftSurface(cardBg))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryLine("Retirada", formatReservationMillis(startedAt), titleColor, subColor)
                    if (reservation.destination.isNotBlank()) SummaryLine("Destino", reservation.destination, titleColor, subColor)
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
    labelColor: Color,
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
        Text(label.uppercase(Locale.getDefault()), color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    val context = LocalContext.current
    val statusColor = when (reservation.status) {
        "em_uso" -> Color(0xFFEA580C)
        "finalizada" -> Color(0xFF15803D)
        "expirada" -> Color(0xFF64748B)
        "suspensa_manutencao" -> Color(0xFFDC2626)
        else -> Color(0xFF0369A1)
    }
    val statusBackground = when (reservation.status) {
        "em_uso" -> Color(0xFFFFEDD5)
        "finalizada" -> Color(0xFFDCFCE7)
        "expirada" -> Color(0xFFE2E8F0)
        "suspensa_manutencao" -> Color(0xFFFEE2E2)
        else -> Color(0xFFE0F2FE)
    }
    val statusText = reservationStatusLabel(reservation.status)
    val tripDetail = reservation.destination.ifBlank { "Destino nao informado" }
    val editActionBg = if (isDarkReservationSurface(cardBg)) Color(0xFF1D4ED8).copy(alpha = 0.22f) else Color(0xFFEFF6FF)
    val editActionColor = if (isDarkReservationSurface(cardBg)) Color(0xFF93C5FD) else Color(0xFF2563EB)
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
                            .background(editActionBg)
                            .clickable(onClick = it)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = editActionColor, modifier = Modifier.size(14.dp))
                        Text("Editar", color = editActionColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
        } else if (reservation.status == "expirada") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(reservationMutedSurface(cardBg))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Text(
                    "Reserva nao retirada: o periodo passou sem leitura do QR Code e a vaga foi liberada para a frota.",
                    color = subColor,
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
            val pickupKm = reservation.pickupOdometerKm
            val returnKm = reservation.returnOdometerKm
            if (pickupKm != null || returnKm != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(reservationSoftSurface(cardBg))
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("ODOMETRO", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    Text(
                        "Retirada ${pickupKm?.let { "$it km" } ?: "nao informado"}  •  Devolucao ${returnKm?.let { "$it km" } ?: "nao informado"}",
                        color = subColor,
                        fontSize = 12.sp
                    )
                    if (pickupKm != null && returnKm != null) {
                        Text(
                            "${(returnKm - pickupKm).coerceAtLeast(0L)} km rodados",
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
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
        FleetSignaturePreview(
            signature = signature,
            cardBg = reservationElevatedSurface(cardBg),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        )
    }
}

@Composable
private fun NewCorporateReservationCard(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    availabilities: List<VehicleAvailability>,
    selectedVehicle: CorporateFleetVehicle?,
    vehicleMenuOpen: Boolean,
    onVehicleMenuChange: (Boolean) -> Unit,
    onVehicleSelected: (CorporateFleetVehicle) -> Unit,
    onOpenPeriodPicker: () -> Unit,
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
    saving: Boolean,
    message: String,
    isEditing: Boolean,
    showValidationErrors: Boolean,
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
        val freeCount = availabilities.count { it.isFree }
        val selectedAvailability = availabilities.firstOrNull { it.vehicle.id == selectedVehicle?.id }
        // Um unico ponto para periodo: data, hora e ocupacao vivem juntos no calendario.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(reservationSoftSurface(cardBg))
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenPeriodPicker)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                    Text("Periodo e ocupacao", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                    "$freeCount de ${availabilities.size} livre${if (freeCount > 1) "s" else ""}",
                    color = if (freeCount > 0) Color(0xFF15803D) else Color(0xFFDC2626),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PeriodEndSummary(
                    label = "Retirada",
                    millis = startMillis,
                    accent = Color(0xFF2563EB),
                    titleColor = titleColor,
                    dimColor = dimColor,
                    cardBg = cardBg,
                    modifier = Modifier.weight(1f)
                )
                PeriodEndSummary(
                    label = "Devolucao",
                    millis = endMillis,
                    accent = Color(0xFF0F766E),
                    titleColor = titleColor,
                    dimColor = dimColor,
                    cardBg = cardBg,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Toque para escolher dia e hora no calendario",
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

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
                availabilities.forEach { availability ->
                    DropdownMenuItem(
                        enabled = availability.isFree,
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    availability.vehicle.displayName(),
                                    color = if (availability.isFree) titleColor else dimColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    buildString {
                                        append(availability.statusLabel())
                                        if (!availability.isFree && availability.busyDriverName.isNotBlank()) {
                                            append(" · ")
                                            append(availability.busyDriverName)
                                        }
                                    },
                                    color = if (availability.isFree) Color(0xFF15803D) else Color(0xFFD97706),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (availability.isFree) Color(0xFF16A34A) else Color(0xFFCBD5E1))
                            )
                        },
                        onClick = { onVehicleSelected(availability.vehicle) }
                    )
                }
                if (availabilities.isEmpty()) {
                    DropdownMenuItem(enabled = false, text = { Text("Nenhum veiculo cadastrado") }, onClick = {})
                }
            }
        }

        if (selectedAvailability != null && !selectedAvailability.isFree) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkReservationSurface(cardBg)) Color(0xFF422006) else Color(0xFFFEF3C7))
                    .padding(11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                Text(
                    buildString {
                        append(selectedAvailability.vehicle.displayName())
                        append(": ")
                        append(selectedAvailability.statusLabel().lowercase(Locale.getDefault()))
                        if (selectedAvailability.busyDriverName.isNotBlank()) {
                            append(" com ")
                            append(selectedAvailability.busyDriverName)
                        }
                        append(". Mude o horario ou escolha outro veiculo.")
                    },
                    color = Color(0xFF92400E),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
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
                label = { Text("Partida *") },
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
                isError = showValidationErrors && origin.isBlank(),
                supportingText = { if (showValidationErrors && origin.isBlank()) Text("Obrigatorio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text("Destino *") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                isError = showValidationErrors && destination.isBlank(),
                supportingText = { if (showValidationErrors && destination.isBlank()) Text("Obrigatorio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        Button(
            onClick = onSave,
            enabled = !saving && selectedAvailability?.isFree == true,
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
private fun PeriodEndSummary(
    label: String,
    millis: Long,
    accent: Color,
    titleColor: Color,
    dimColor: Color,
    cardBg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = if (isDarkReservationSurface(cardBg)) 0.2f else 0.1f))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label.uppercase(Locale.getDefault()), color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.5.sp)
        Text(formatReservationTime(millis), color = titleColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(formatReservationDate(millis), color = dimColor, fontSize = 10.sp)
    }
}

@Composable
private fun ReservationQrValidationDialog(
    reservation: CorporateReservation,
    userId: String,
    lastKnownOdometerKm: Long?,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?) -> Unit
) {
    val context = LocalContext.current
    val isReturning = reservation.status == "em_uso"
    val minimumOdometerKm = if (isReturning) {
        reservation.pickupOdometerKm ?: lastKnownOdometerKm
    } else {
        lastKnownOdometerKm
    }
    var showCameraScanner by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("") }
    var pendingQrText by remember { mutableStateOf<String?>(null) }
    var manualOdometerText by remember { mutableStateOf("") }
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
            /*
             * O dialogo tem duas etapas: ler o QR e informar o KM. Antes ele mostrava as duas ao
             * mesmo tempo — com o QR ja lido, o botao "Ler QR do veiculo" continuava em destaque e o
             * texto do topo ainda mandava escanear, enquanto o campo de KM aparecia embaixo. Agora o
             * cabecalho inteiro fala da etapa atual.
             */
            val qrLido = pendingQrText != null
            val acaoFinal = if (isReturning) "devolucao" else "retirada"

            Icon(
                if (qrLido) Icons.Default.Speed else Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color(0xFF0F766E),
                modifier = Modifier.size(34.dp)
            )
            Text(
                when {
                    qrLido -> if (isReturning) "KM da devolucao" else "KM da retirada"
                    isReturning -> "Validar devolucao"
                    else -> "Validar retirada"
                },
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (qrLido) {
                    "Leia o odometro no painel do veiculo e informe o KM atual para concluir a $acaoFinal."
                } else {
                    "Escaneie o QR gerado na dashboard para este veiculo. A retirada ou devolucao sera marcada automaticamente."
                },
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
                    if (qrLido) {
                        "Cadastre sua assinatura para confirmar a $acaoFinal."
                    } else {
                        "Cadastre sua assinatura para liberar a leitura do QR Code."
                    },
                    color = subColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!qrLido) {
                Button(onClick = { showCameraScanner = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ler QR do veiculo", fontWeight = FontWeight.Bold)
                }
            }
            if (pendingQrText != null) {
                // Etapa vencida vira selo curto: a instrucao dela ja saiu do caminho.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                    Text("QR do veiculo validado", color = Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = manualOdometerText,
                    // Formata no proprio onValueChange, como o campo de KM da EditarCarroScreen. O
                    // estado guarda o texto ja com separador, entao a leitura precisa tirar os pontos.
                    onValueChange = { value ->
                        manualOdometerText = formatarKmDigitado(value)
                        scanMessage = ""
                    },
                    label = { Text(if (isReturning) "KM na devolucao" else "KM na retirada") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                minimumOdometerKm?.takeIf { it > 0L }?.let { minimum ->
                    Text(
                        if (isReturning) {
                            "KM registrado na retirada: ${formatarKmNumero(minimum)}"
                        } else {
                            "Ultimo KM registrado do veiculo: ${formatarKmNumero(minimum)}"
                        },
                        color = subColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        // Sem tirar o separador, "455.666".toLongOrNull() e nulo e o KM valido seria recusado.
                        val km = manualOdometerText.filter(Char::isDigit).toLongOrNull()
                        val minimum = minimumOdometerKm ?: 0L
                        when {
                            km == null || km <= 0L -> scanMessage = "Informe um KM valido."
                            km < minimum -> scanMessage = "O KM nao pode ser menor que ${formatarKmNumero(minimum)}."
                            else -> pendingQrText?.let { qr -> onConfirm(qr, savedSignature, km) }
                        }
                    },
                    enabled = manualOdometerText.isNotBlank() && savedSignature.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isReturning) "Concluir devolucao" else "Confirmar retirada", fontWeight = FontWeight.Bold)
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
                } else {
                    // O KM sempre e pedido depois da leitura, entao o QR fica pendente ate a confirmacao.
                    pendingQrText = qrText
                    scanMessage = ""
                    if (savedSignature.isBlank()) showSignatureEditor = true
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
                // O QR lido continua pendente: falta o KM para confirmar.
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Assinatura da retirada", color = titleColor, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        "Esta assinatura sera usada nas proximas retiradas e devolucoes por QR Code.",
                        color = subColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(reservationMutedSurface(cardBg))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = titleColor, modifier = Modifier.size(16.dp))
                }
            }
            FleetSignaturePad(
                value = signature,
                cardBg = cardBg,
                cardBorder = cardBorder,
                onValueChange = { signature = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { signature = "" },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Limpar") }
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

/** Grade de 6 semanas (domingo a sabado) que cobre o mes do millis informado. */
private fun occupancyCalendarDays(anchorMillis: Long): List<Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = anchorMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    calendar.add(Calendar.DAY_OF_MONTH, -(calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
    return List(42) {
        val value = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        value
    }
}

/** Sinaliza que outra pessoa ocupou a vaga antes: usado para desfazer a reserva otimista local. */
private const val RESERVATION_CONFLICT = "RESERVATION_CONFLICT"

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
    val memberRef = companyRef.collection("members").document(user.uid)
    val bookingsRef = companyRef.collection("vehicleBookings").document(vehicle.id)
    val userName = user.displayName ?: user.email.orEmpty()
    val reservationPayload = mapOf(
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
    val bookingSlotPayload = mapOf(
        "vehicleId" to vehicle.id,
        "slots" to mapOf(
            reservationId to mapOf(
                "startsAt" to startMillis,
                "endsAt" to endMillis,
                "driverUid" to user.uid
            )
        ),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    // Reserva, indice de ocupacao e cadastro do membro numa transacao: o indice e um documento
    // unico por veiculo, entao dois motoristas reservando ao mesmo tempo sao serializados pelo
    // Firestore em vez de furarem a capacidade com duas checagens locais simultaneas.
    db.runTransaction { transaction ->
        val bookings = transaction.get(bookingsRef)
        val member = transaction.get(memberRef)
        @Suppress("UNCHECKED_CAST")
        val slots = (bookings.get("slots") as? Map<String, Map<String, Any?>>).orEmpty()
        val capacity = vehicle.maxConcurrentReservations.coerceAtLeast(1)
        val conflicts = slots.count { (slotId, slot) ->
            if (slotId == reservationId) return@count false
            val slotStart = (slot["startsAt"] as? Number)?.toLong()
            val slotEnd = (slot["endsAt"] as? Number)?.toLong()
            slotStart != null && slotEnd != null && rangesOverlap(startMillis, endMillis, slotStart, slotEnd)
        }
        if (conflicts >= capacity) throw IllegalStateException(RESERVATION_CONFLICT)

        val memberPayload = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "name" to userName,
            "email" to user.email.orEmpty().lowercase(Locale.getDefault()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        // Papel so e definido na criacao: senao um gestor viraria "motorista" ao criar uma reserva pelo app.
        if (!member.exists()) {
            memberPayload["role"] = "motorista"
            memberPayload["active"] = true
        }
        transaction.set(memberRef, memberPayload, SetOptions.merge())
        transaction.set(reservationRef, reservationPayload, SetOptions.merge())
        transaction.set(bookingsRef, bookingSlotPayload, SetOptions.merge())
    }
        .addOnSuccessListener {
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
    }.addOnFailureListener { error ->
        // Transacao exige rede. Offline, a escrita volta para a fila do Firestore (como antes),
        // valendo apenas a checagem local de conflito - que e o melhor possivel sem conexao.
        val offline = (error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.UNAVAILABLE
        if (offline) {
            db.batch()
                .apply {
                    set(reservationRef, reservationPayload, SetOptions.merge())
                    set(bookingsRef, bookingSlotPayload, SetOptions.merge())
                }
                .commit()
        }
        onError(error)
    }
}

private fun updateCorporateReservationTripStatus(
    companyId: String,
    reservation: CorporateReservation,
    qrText: String,
    signature: String,
    odometerKm: Long,
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
    val bookingsRef = db.collection("companies")
        .document(companyId)
        .collection("vehicleBookings")
        .document(reservation.vehicleId)
    val startingTrip = reservation.status == "em_uso"

    // Uma transacao unica mantem reserva, viagem e veiculo coerentes: e o veiculo que a dashboard
    // usa para contar "em uso" / "disponiveis", por isso o status dele precisa acompanhar o QR Code.
    db.runTransaction { transaction ->
        val vehicle = transaction.get(vehicleRef)
        val trip = transaction.get(tripRef)
        val vehicleStatus = vehicle.getString("status").orEmpty().ifBlank { "disponivel" }
        val pickupKm = reservation.pickupOdometerKm
            ?: trip.getLong("odometerStartKm")
            ?: vehicle.getLong("odometerKm")
            ?: vehicle.getLong("kmAtual")
            ?: odometerKm

        val payload = mutableMapOf<String, Any>(
            "status" to reservation.status,
            "lastQrText" to qrText.trim(),
            "lastQrValidatedBy" to user.uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val tripPayload = mutableMapOf<String, Any>(
            "id" to reservation.id,
            "companyId" to companyId,
            "reservationId" to reservation.id,
            "vehicleId" to reservation.vehicleId,
            "vehicleName" to reservation.vehicleName,
            "driverName" to reservation.driverName,
            "driverUid" to user.uid,
            "origin" to reservation.origin,
            "destination" to reservation.destination,
            "status" to if (startingTrip) "em_andamento" else "concluida",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val vehiclePayload = mutableMapOf<String, Any>(
            "odometerKm" to odometerKm,
            "kmAtual" to odometerKm,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (startingTrip) {
            payload["tripStartedAt"] = Date(reservation.tripStartedAtMillis ?: System.currentTimeMillis())
            payload["tripStartedBy"] = user.uid
            payload["pickupSignature"] = signature
            // Grava os dois nomes de campo: pickupOdometerKm e o que a dashboard web ja le.
            payload["pickupOdometerKm"] = odometerKm
            payload["odometerStartKm"] = odometerKm
            tripPayload["odometerStartKm"] = odometerKm
            // Nao mexe em veiculo bloqueado ou em manutencao: quem libera isso e a gestao da frota.
            if (vehicleStatus == "disponivel" || vehicleStatus == "reservado") {
                vehiclePayload["status"] = "em_uso"
            }
        } else {
            val increment = (odometerKm - pickupKm).coerceAtLeast(0L)
            payload["tripEndedAt"] = Date(reservation.tripEndedAtMillis ?: System.currentTimeMillis())
            payload["tripEndedBy"] = user.uid
            payload["returnSignature"] = signature
            payload["returnOdometerKm"] = odometerKm
            payload["odometerEndKm"] = odometerKm
            payload["odometerIncrementKm"] = increment
            tripPayload["odometerStartKm"] = pickupKm
            tripPayload["odometerEndKm"] = odometerKm
            tripPayload["odometerIncrementKm"] = increment
            if (vehicleStatus == "em_uso" || vehicleStatus == "atrasado") {
                vehiclePayload["status"] = "disponivel"
            }
        }

        reservation.tripStartedAtMillis?.let { tripPayload["startedAt"] = Date(it) }
        reservation.tripEndedAtMillis?.let { tripPayload["endedAt"] = Date(it) }
        reservation.pickupSignature.orEmpty().takeIf { it.isNotBlank() }?.let { tripPayload["pickupSignature"] = it }
        reservation.returnSignature.orEmpty().takeIf { it.isNotBlank() }?.let { tripPayload["returnSignature"] = it }

        transaction.set(reservationRef, payload, SetOptions.merge())
        transaction.set(tripRef, tripPayload, SetOptions.merge())
        transaction.set(vehicleRef, vehiclePayload, SetOptions.merge())
        if (!startingTrip) {
            // Viagem encerrada: a vaga sai do indice de ocupacao do veiculo.
            transaction.set(
                bookingsRef,
                mapOf("slots" to mapOf(reservation.id to FieldValue.delete())),
                SetOptions.merge()
            )
        }
    }.addOnFailureListener { onError(it) }
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
            if (isCorporateCompanyIdForUser(activeCompanyId, user.uid)) {
                db.collection("companies")
                    .document(activeCompanyId.orEmpty())
                    .collection("members")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { memberDoc ->
                        if (memberDoc.exists() && memberDoc.getBoolean("active") != false) {
                            onResolved(activeCompanyId.orEmpty())
                        } else {
                            resolveCorporateCompanyIdFromInvite(db, user, fallbackCompanyId, normalizedEmail, null, onResolved)
                        }
                    }
                    .addOnFailureListener {
                        resolveCorporateCompanyIdFromInvite(db, user, fallbackCompanyId, normalizedEmail, null, onResolved)
                    }
                return@addOnSuccessListener
            }
            resolveCorporateCompanyIdFromInvite(db, user, fallbackCompanyId, normalizedEmail, activeCompanyId, onResolved)
        }
        .addOnFailureListener { onResolved(fallbackCompanyId) }
}

private fun resolveCorporateCompanyIdFromInvite(
    db: FirebaseFirestore,
    user: com.google.firebase.auth.FirebaseUser,
    fallbackCompanyId: String,
    normalizedEmail: String,
    staleCompanyId: String?,
    onResolved: (String) -> Unit
) {
    if (normalizedEmail.isBlank()) {
        onResolved(fallbackCompanyId)
        return
    }
    val userRef = db.collection("users").document(user.uid)
    db.collection("userInvites")
        .document(corporateEmailKey(normalizedEmail))
        .collection("companies")
        .whereEqualTo("email", normalizedEmail)
        .limit(1)
        .get()
        .addOnSuccessListener { invites ->
            val invite = invites.documents.firstOrNull()
            val invitedCompanyId = invite?.getString("companyId")
            if (invitedCompanyId.isNullOrBlank()) {
                onResolved(fallbackCompanyId)
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
                        // As rules exigem saber qual convite autoriza esta adesao.
                        "inviteKey" to corporateEmailKey(normalizedEmail),
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
        .addOnFailureListener { onResolved(staleCompanyId?.takeIf { it == fallbackCompanyId } ?: fallbackCompanyId) }
}

private fun corporateEmailKey(email: String): String {
    return email.trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9._-]"), "_")
}

private fun isCorporateCompanyIdForUser(companyId: String?, userUid: String): Boolean {
    val id = companyId.orEmpty()
    return id.isNotBlank() && id != "personal_$userUid"
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

/** Contagem regressiva amigavel para a proxima retirada. */
private fun reservationCountdownLabel(startMillis: Long, nowMillis: Long): String {
    val diff = startMillis - nowMillis
    if (diff <= 0L) return "Retirada liberada"
    val totalMinutes = diff / 60_000L
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0L && hours > 0L -> "Em ${days}d ${hours}h"
        days > 0L -> "Em ${days}d"
        hours > 0L && minutes > 0L -> "Em ${hours}h ${minutes}min"
        hours > 0L -> "Em ${hours}h"
        minutes > 0L -> "Em ${minutes}min"
        else -> "Comeca agora"
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


/** Conteudo do QR gerado pela dashboard: {"app":"zellu","type":"fleet_vehicle","companyId":...,"vehicleId":...}. */
private data class VehicleQrPayload(
    val companyId: String,
    val vehicleId: String,
    val vehicleName: String,
    val plate: String
)

private fun parseVehicleQrPayload(qrText: String): VehicleQrPayload? {
    val raw = qrText.trim()
    if (raw.isBlank()) return null
    val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    val json = runCatching { Gson().fromJson(decoded, JsonObject::class.java) }.getOrNull() ?: return null
    fun field(name: String): String = runCatching { json.get(name)?.asString.orEmpty() }.getOrDefault("")
    if (field("app").lowercase(Locale.getDefault()) != "zellu") return null
    if (field("type").lowercase(Locale.getDefault()) != "fleet_vehicle") return null
    return VehicleQrPayload(
        companyId = field("companyId"),
        vehicleId = field("vehicleId"),
        vehicleName = field("vehicleName"),
        plate = field("plate")
    )
}

/**
 * Compara identificadores de forma exata (apenas normalizando acentos e separadores).
 * Antes isso usava `contains`, entao qualquer texto que citasse o nome do veiculo era aceito.
 */
private fun sameQrIdentity(candidate: String, expected: String): Boolean {
    val normalizedExpected = normalizedReservationText(expected)
    if (normalizedExpected.isBlank()) return false
    return normalizedReservationText(candidate) == normalizedExpected
}

private fun reservationQrMatches(reservation: CorporateReservation, qrText: String, companyId: String): Boolean {
    val payload = parseVehicleQrPayload(qrText)
    if (payload != null) {
        // QR oficial: empresa e veiculo precisam bater exatamente.
        if (payload.companyId.isNotBlank() && companyId.isNotBlank() && payload.companyId != companyId) return false
        if (payload.vehicleId.isNotBlank()) return payload.vehicleId == reservation.vehicleId
        return sameQrIdentity(payload.vehicleName, reservation.vehicleName)
    }
    // QR legado (texto simples impresso antes do formato JSON): exige igualdade, nao substring.
    val candidate = qrComparableText(qrText)
    return sameQrIdentity(candidate, reservation.vehicleId) ||
        sameQrIdentity(candidate, reservation.id) ||
        sameQrIdentity(candidate, reservation.vehicleName)
}

private fun corporateVehicleQrMatches(vehicle: CorporateFleetVehicle, qrText: String, companyId: String): Boolean {
    val payload = parseVehicleQrPayload(qrText)
    if (payload != null) {
        if (payload.companyId.isNotBlank() && companyId.isNotBlank() && payload.companyId != companyId) return false
        if (payload.vehicleId.isNotBlank()) return payload.vehicleId == vehicle.id
        return sameQrIdentity(payload.vehicleName, vehicle.name) || sameQrIdentity(payload.plate, vehicle.plate)
    }
    val candidate = qrComparableText(qrText)
    return sameQrIdentity(candidate, vehicle.id) ||
        sameQrIdentity(candidate, vehicle.name) ||
        sameQrIdentity(candidate, vehicle.plate)
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
    "expirada" -> "Nao retirada"
    "suspensa_manutencao" -> "Suspensa por manutencao"
    else -> "Reservada"
}

/**
 * Reserva cujo dia terminou sem retirada vira "expirada", em vez de ficar para sempre na agenda.
 * So o proprio motorista marca a dele, para nao ter varios dispositivos escrevendo o mesmo documento.
 */
private fun expireStaleReservations(
    db: FirebaseFirestore,
    companyId: String,
    reservations: List<CorporateReservation>,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    alreadyMarked: MutableSet<String>
) {
    if (currentUser == null) return
    val myIdentity = currentUser.displayName?.ifBlank { null } ?: currentUser.email.orEmpty()
    if (myIdentity.isBlank()) return
    val today = startOfDayMillis(System.currentTimeMillis())
    val stale = reservations.filter { reservation ->
        reservation.status == "reservada" &&
            startOfDayMillis(reservation.endsAtMillis) < today &&
            reservation.id !in alreadyMarked &&
            normalizedReservationText(reservation.driverName) == normalizedReservationText(myIdentity)
    }
    if (stale.isEmpty()) return
    val companyRef = db.collection("companies").document(companyId)
    val batch = db.batch()
    stale.forEach { reservation ->
        alreadyMarked += reservation.id
        batch.set(
            companyRef.collection("reservations").document(reservation.id),
            mapOf(
                "status" to "expirada",
                "expiredAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        // A vaga volta para a frota: sem isso o veiculo seguiria "reservado" na dashboard.
        batch.set(
            companyRef.collection("vehicleBookings").document(reservation.vehicleId),
            mapOf("slots" to mapOf(reservation.id to FieldValue.delete())),
            SetOptions.merge()
        )
    }
    batch.commit().addOnFailureListener { alreadyMarked.removeAll(stale.map { it.id }.toSet()) }
}

private fun nextRoundedHourMillis(): Long {
    return Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatarKmNumero(valor: Long): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(valor)

/**
 * Formata enquanto se digita, mantendo so os digitos e reagrupando os milhares.
 *
 * Formatar aqui, no onValueChange, e nao derivando o `value` a partir do estado: derivado, o texto
 * formatado briga com o que foi digitado e apagar caracteres para de funcionar.
 */
private fun formatarKmDigitado(texto: String): String {
    val digitos = texto.filter(Char::isDigit).take(9)
    if (digitos.isEmpty()) return ""
    return formatarKmNumero(digitos.toLongOrNull() ?: 0L)
}

private fun formatReservationDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

private fun formatReservationTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatReservationMillis(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
