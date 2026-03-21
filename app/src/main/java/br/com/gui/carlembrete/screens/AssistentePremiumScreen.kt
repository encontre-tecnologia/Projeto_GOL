package br.com.gui.carlembrete

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.CalendarContract
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

private data class TravelExpense(
    val id: String,
    val label: String,
    val amount: Double,
    val category: String,
    val vehicleName: String = "",
    val notePhotoUri: Uri? = null
)

private data class TravelTrip(
    val id: String,
    val name: String,
    val location: String = "",
    val responsible: String = "",
    val participantEmails: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val expenses: List<TravelExpense>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistentePremiumScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (isDark) scheme.background else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.12f)
    val accentBlue = Color(0xFF3B82F6)
    val categories = listOf("Combustivel", "Pedagio", "Alimentacao", "Hospedagem", "Compras", "Outros")
    val trips = remember(context) {
        mutableStateListOf<TravelTrip>().apply { addAll(loadTravelTrips(context)) }
    }
    val defaultTrip = remember {
        TravelTrip(
            id = UUID.randomUUID().toString(),
            name = "Minha viagem",
            location = "",
            responsible = "",
            expenses = emptyList()
        )
    }
    if (trips.isEmpty()) {
        trips.add(defaultTrip)
        saveTravelTrips(context, trips)
    }
    var activeTripId by remember(context) { mutableStateOf(trips.firstOrNull()?.id ?: defaultTrip.id) }
    var tripName by remember(context) { mutableStateOf(trips.firstOrNull()?.name ?: defaultTrip.name) }
    var tripLocation by remember(context) { mutableStateOf(trips.firstOrNull()?.location.orEmpty()) }
    var showExpensesScreen by remember { mutableStateOf(false) }
    var showTripsScreen by remember { mutableStateOf(true) }
    var showCreateTripScreen by remember { mutableStateOf(false) }
    var showShareTripDialog by remember { mutableStateOf(false) }
    var pendingShareTrip by remember { mutableStateOf<TravelTrip?>(null) }

    val expenses = remember(context, activeTripId) {
        mutableStateListOf<TravelExpense>().apply {
            addAll(trips.firstOrNull { it.id == activeTripId }?.expenses.orEmpty())
        }
    }
    var expenseLabel by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Combustivel") }
    val registeredVehicles = remember(context) {
        BancoDeDados.carregarCarros(context).orEmpty().map { it.nome }.filter { it.isNotBlank() }
    }
    val otherVehicleLabel = "Outro (fora do app)"
    val vehicleOptions = remember(registeredVehicles) { registeredVehicles + otherVehicleLabel }
    var selectedVehicleName by remember(registeredVehicles) { mutableStateOf(registeredVehicles.firstOrNull().orEmpty()) }
    var customVehicleName by remember { mutableStateOf("") }
    val photoUris = remember { mutableStateListOf<Uri>() }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoFlowWarning by remember { mutableStateOf(false) }
    var skipPhotoFlowWarning by remember(context) { mutableStateOf(loadSkipPhotoWarning(context)) }

    var showExpenseAddedDialog by remember { mutableStateOf(false) }
    var showAddExpenseScreen by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showScannerGuide by remember { mutableStateOf(false) }
    var isQrLoading by remember { mutableStateOf(false) }
    var isGeneratingGeneralReport by remember { mutableStateOf(false) }
    var generalReportMode by remember { mutableStateOf<String?>(null) } // "pdf" | "print"
    val scope = rememberCoroutineScope()
    val scannerGuidePrefs = remember(context) {
        context.getSharedPreferences("scanner_guide_prefs", Context.MODE_PRIVATE)
    }
    val scannerGuideImageResId = remember(context) {
        context.resources.getIdentifier("notaexemplo", "drawable", context.packageName)
    }

    fun abrirScannerComGuia() {
        val chaveGuiaViagem = "mostrar_guia_scanner_produto_viagem"
        val mostrarGuia = scannerGuidePrefs.getBoolean(chaveGuiaViagem, true)
        if (mostrarGuia) {
            scannerGuidePrefs.edit().putBoolean(chaveGuiaViagem, false).apply()
            showScannerGuide = true
        } else {
            showCamera = true
        }
    }

    fun persistCurrentTrip() {
        val existingTrip = trips.firstOrNull { it.id == activeTripId }
        val tripToSave = TravelTrip(
            id = activeTripId,
            name = tripName.ifBlank { "Minha viagem" },
            location = tripLocation.trim(),
            responsible = existingTrip?.responsible.orEmpty(),
            participantEmails = existingTrip?.participantEmails.orEmpty(),
            isFinished = existingTrip?.isFinished ?: false,
            expenses = expenses.toList()
        )
        val idx = trips.indexOfFirst { it.id == activeTripId }
        if (idx >= 0) {
            trips[idx] = tripToSave
        } else {
            trips.add(tripToSave)
            activeTripId = tripToSave.id
        }
        saveTravelTrips(context, trips)
    }

    fun switchToTrip(trip: TravelTrip) {
        persistCurrentTrip()
        activeTripId = trip.id
        tripName = trip.name
        tripLocation = trip.location
        expenses.clear()
        expenses.addAll(trip.expenses)
    }

    val registerNotePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = pendingPhotoUri
        pendingPhotoUri = null
        if (!success || capturedUri == null) {
            Toast.makeText(context, "Foto da nota nao registrada.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        photoUris.add(capturedUri)
        abrirScannerComGuia()
    }

    fun startRegisterNotePhotoFlow() {
        val uri = createTempImageUri(context, "trip_note_register")
        pendingPhotoUri = uri
        registerNotePhotoLauncher.launch(uri)
    }

    fun openAddExpenseForm() {
        val latestExpense = trips.firstOrNull { it.id == activeTripId }?.expenses?.lastOrNull()
        latestExpense?.category
            ?.takeIf { it in categories }
            ?.let { category = it }
        val latestVehicle = latestExpense?.vehicleName?.trim().orEmpty()
        if (latestVehicle.isNotBlank()) {
            if (latestVehicle in vehicleOptions) {
                selectedVehicleName = latestVehicle
                customVehicleName = ""
            } else {
                selectedVehicleName = otherVehicleLabel
                customVehicleName = latestVehicle
            }
        } else if (selectedVehicleName.isBlank()) {
            selectedVehicleName = registeredVehicles.firstOrNull().orEmpty()
        }
        expenseLabel = ""
        expenseAmount = ""
        photoUris.clear()
        pendingPhotoUri = null
        showAddExpenseScreen = true
    }

    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    if (showAddExpenseScreen) {
        BackHandler {
            showAddExpenseScreen = false
        }
    }

    if (showCreateTripScreen) {
        CreateTripScreen(
            isDark = isDark,
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            accentBlue = accentBlue,
            onBack = { showCreateTripScreen = false },
            onCreate = { newName, location, responsible, participantEmails ->
                persistCurrentTrip()
                val newTrip = TravelTrip(
                    id = UUID.randomUUID().toString(),
                    name = newName.ifBlank { "Nova viagem" },
                    location = location,
                    responsible = responsible,
                    participantEmails = participantEmails,
                    expenses = emptyList()
                )
                trips.add(0, newTrip)
                saveTravelTrips(context, trips)
                switchToTrip(newTrip)
                pendingShareTrip = newTrip
                showShareTripDialog = true
                showCreateTripScreen = false
                showTripsScreen = false
                showExpensesScreen = true
            }
        )
    } else if (showAddExpenseScreen) {
        AddTravelExpenseScreen(
            tripName = tripName,
            categories = categories,
            vehicleOptions = vehicleOptions,
            otherVehicleLabel = otherVehicleLabel,
            isDark = isDark,
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            accentBlue = accentBlue,
            category = category,
            onCategoryChange = { category = it },
            selectedVehicleName = selectedVehicleName,
            onVehicleChange = {
                selectedVehicleName = it
                if (it != otherVehicleLabel) {
                    customVehicleName = ""
                }
            },
            customVehicleName = customVehicleName,
            onCustomVehicleNameChange = { customVehicleName = it },
            expenseLabel = expenseLabel,
            onExpenseLabelChange = { expenseLabel = it },
            expenseAmount = expenseAmount,
            onExpenseAmountChange = { expenseAmount = it },
            photoCount = photoUris.size,
            onBack = { showAddExpenseScreen = false },
            onAddPhoto = {
                if (skipPhotoFlowWarning) {
                    startRegisterNotePhotoFlow()
                } else {
                    showPhotoFlowWarning = true
                }
            },
            onSave = saveExpense@{
                if (expenseLabel.trim().isBlank()) {
                    Toast.makeText(context, "Preencha o campo Produtos.", Toast.LENGTH_SHORT).show()
                    return@saveExpense
                }
                val amount = parseValorMonetario(expenseAmount)
                if (amount == null || amount <= 0.0) {
                    Toast.makeText(context, "Informe um valor valido.", Toast.LENGTH_SHORT).show()
                    return@saveExpense
                }
                val vehicleNameToSave = when {
                    selectedVehicleName == otherVehicleLabel -> customVehicleName.trim()
                    selectedVehicleName.isNotBlank() -> selectedVehicleName
                    else -> ""
                }
                if (selectedVehicleName.isBlank()) {
                    Toast.makeText(context, "Selecione um veiculo.", Toast.LENGTH_SHORT).show()
                    return@saveExpense
                }
                if (selectedVehicleName == otherVehicleLabel && vehicleNameToSave.isBlank()) {
                    Toast.makeText(context, "Informe o nome do veiculo externo.", Toast.LENGTH_SHORT).show()
                    return@saveExpense
                }
                expenses.add(
                    TravelExpense(
                        id = UUID.randomUUID().toString(),
                        label = expenseLabel.ifBlank { category },
                        amount = amount,
                        category = category,
                        vehicleName = vehicleNameToSave,
                        notePhotoUri = photoUris.lastOrNull()
                    )
                )
                AdminUsageMetrics.markTravelExpenseSaved()
                persistCurrentTrip()
                expenseLabel = ""
                expenseAmount = ""
                customVehicleName = ""
                photoUris.clear()
                pendingPhotoUri = null
                showAddExpenseScreen = false
                showExpenseAddedDialog = false
                showTripsScreen = false
                showExpensesScreen = true
                Toast.makeText(context, "Gasto adicionado na viagem atual.", Toast.LENGTH_SHORT).show()
            }
        )
    } else if (showExpensesScreen) {
        val activeTrip = trips.firstOrNull { it.id == activeTripId }
        TravelExpensesScreen(
            tripName = tripName,
            onTripNameChange = {
                tripName = it
                persistCurrentTrip()
            },
            tripLocation = tripLocation,
            onTripLocationChange = {
                tripLocation = it
                persistCurrentTrip()
            },
            expenses = expenses,
            onPersistExpenses = { persistCurrentTrip() },
            isTripFinished = activeTrip?.isFinished == true,
            isDark = isDark,
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            accentBlue = accentBlue,
            currency = currency,
            categories = categories,
            onBack = {
                persistCurrentTrip()
                showExpensesScreen = false
                showTripsScreen = true
            },
            onAddExpense = {
                if (activeTrip?.isFinished == true) {
                    Toast.makeText(context, "Reabra a viagem para adicionar novos gastos.", Toast.LENGTH_SHORT).show()
                } else {
                    openAddExpenseForm()
                }
            },
            onFinishTrip = {
                val idx = trips.indexOfFirst { it.id == activeTripId }
                if (idx >= 0) {
                    trips[idx] = trips[idx].copy(isFinished = true, expenses = expenses.toList())
                    saveTravelTrips(context, trips)
                }
            },
            onReopenTrip = {
                val idx = trips.indexOfFirst { it.id == activeTripId }
                if (idx >= 0) {
                    trips[idx] = trips[idx].copy(isFinished = false, expenses = expenses.toList())
                    saveTravelTrips(context, trips)
                }
            },
            onExportPdf = {
                val pdf = generateTripReportPdf(
                    context = context,
                    tripName = tripName,
                    location = activeTrip?.location.orEmpty(),
                    responsible = activeTrip?.responsible.orEmpty(),
                    expenses = expenses
                )
                if (pdf == null) {
                    Toast.makeText(context, "Nao foi possivel gerar o PDF.", Toast.LENGTH_SHORT).show()
                } else {
                    sharePdf(context, pdf)
                }
            },
            onPrintPdf = {
                val activeTrip = trips.firstOrNull { it.id == activeTripId }
                val pdf = generateTripReportPdf(
                    context = context,
                    tripName = tripName,
                    location = activeTrip?.location.orEmpty(),
                    responsible = activeTrip?.responsible.orEmpty(),
                    expenses = expenses
                )
                if (pdf == null) {
                    Toast.makeText(context, "Nao foi possivel gerar o PDF.", Toast.LENGTH_SHORT).show()
                } else {
                    printPdf(context, pdf, "Relatorio Viagem")
                }
            },
            onExportSpreadsheetWithPhotos = {
                val spreadsheet = generateTripReportSpreadsheet(context, tripName, activeTrip?.location.orEmpty(), expenses)
                val zipPackage = generateTripReportPackage(context, tripName, expenses)
                if (spreadsheet == null || zipPackage == null) {
                    Toast.makeText(context, "Nao foi possivel gerar os arquivos de exportacao.", Toast.LENGTH_SHORT).show()
                } else {
                    shareSpreadsheetAndPhotos(context, spreadsheet, zipPackage)
                }
            }
        )
    } else if (showTripsScreen) {
        TripsByTravelScreen(
            trips = trips,
            isDark = isDark,
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            accentBlue = accentBlue,
            currency = currency,
            onBack = onDismiss,
            onExportAllTripsPdf = {
                scope.launch {
                    persistCurrentTrip()
                    generalReportMode = "pdf"
                    isGeneratingGeneralReport = true
                    val snapshot = trips.toList()
                    val pdf = withContext(Dispatchers.Default) {
                        generateAllTripsReportPdf(context, snapshot)
                    }
                    isGeneratingGeneralReport = false
                    generalReportMode = null
                    if (pdf == null) {
                        Toast.makeText(context, "Nao foi possivel gerar o PDF geral.", Toast.LENGTH_SHORT).show()
                    } else {
                        sharePdf(context, pdf)
                    }
                }
            },
            onPrintAllTripsPdf = {
                scope.launch {
                    persistCurrentTrip()
                    generalReportMode = "print"
                    isGeneratingGeneralReport = true
                    val snapshot = trips.toList()
                    val pdf = withContext(Dispatchers.Default) {
                        generateAllTripsReportPdf(context, snapshot)
                    }
                    isGeneratingGeneralReport = false
                    generalReportMode = null
                    if (pdf == null) {
                        Toast.makeText(context, "Nao foi possivel gerar o relatorio geral.", Toast.LENGTH_SHORT).show()
                    } else {
                        printPdf(context, pdf, "Relatorio Geral Viagens")
                    }
                }
            },
            onExportAllTripsSpreadsheet = {
                persistCurrentTrip()
                val snapshot = trips.toList()
                val spreadsheet = generateAllTripsSpreadsheet(context, snapshot)
                val zipPackage = generateAllTripsNotesPackage(context, snapshot)
                if (spreadsheet == null || zipPackage == null) {
                    Toast.makeText(context, "Nao foi possivel gerar os arquivos da exportacao geral.", Toast.LENGTH_SHORT).show()
                } else {
                    shareSpreadsheetAndPhotos(context, spreadsheet, zipPackage)
                }
            },
            onOpenTrip = { trip ->
                switchToTrip(trip)
                showExpensesScreen = true
            },
            onCreateTrip = {
                showCreateTripScreen = true
            },
            onRenameTrip = { tripId, newName, newLocation, newResponsible, newParticipants ->
                val idx = trips.indexOfFirst { it.id == tripId }
                if (idx >= 0) {
                    val updated = trips[idx].copy(
                        name = newName.ifBlank { "Minha viagem" },
                        location = newLocation.trim(),
                        responsible = newResponsible.trim(),
                        participantEmails = newParticipants
                    )
                    trips[idx] = updated
                    if (tripId == activeTripId) {
                        tripName = updated.name
                        tripLocation = updated.location
                    }
                    saveTravelTrips(context, trips)
                }
            },
            onDeleteTrip = { tripId ->
                if (trips.size <= 1) {
                    Toast.makeText(context, "Mantenha ao menos uma viagem.", Toast.LENGTH_SHORT).show()
                } else {
                    trips.removeAll { it.id == tripId }
                    if (tripId == activeTripId) {
                        val fallback = trips.first()
                        activeTripId = fallback.id
                        tripName = fallback.name
                        expenses.clear()
                        expenses.addAll(fallback.expenses)
                    }
                    saveTravelTrips(context, trips)
                }
            }
        )
    } else Scaffold(
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                }
                Text("Viagem", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Centralize aqui suas viagens e gastos",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            persistCurrentTrip()
                            showTripsScreen = true
                        },
                        border = BorderStroke(1.dp, if (isDark) Color.White else Color.Black),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir controle de viagens", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Wallet,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(30.dp)
                        )
                        Text("Adicionar gasto", color = textPrimary, fontWeight = FontWeight.Bold)
                        Text("Lance despesas com NFC-e de compra com QR", color = textDim, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = cardBorder)
                    Button(
                        onClick = { openAddExpenseForm() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar Gasto", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                }
            }

        }
    }

    if (showShareTripDialog && pendingShareTrip != null) {
        Dialog(onDismissRequest = { showShareTripDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        "Viagem criada com sucesso!",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    pendingShareTrip?.let { trip ->
                        Text(
                            "A viagem ${trip.name} foi criada. Deseja adicionar um evento na agenda do celular ou Google Agenda?",
                            color = textDim,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Text(
                        "Se houver acompanhantes, os e-mails informados vao junto no evento.",
                        color = textDim,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showShareTripDialog = false
                                pendingShareTrip = null
                            },
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Agora nao")
                        }
                        Button(
                            onClick = {
                                pendingShareTrip?.let { abrirEventoAgendaDaViagem(context, it) }
                                showShareTripDialog = false
                                pendingShareTrip = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Adicionar evento")
                        }
                    }
                }
            }
        }
    }

    if (showScannerGuide) {
        Scaffold(containerColor = bg) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (scannerGuideImageResId != 0) {
                        Image(
                            painter = painterResource(id = scannerGuideImageResId),
                            contentDescription = "Exemplo de nota para escaneamento",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else Color.White),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Condições para escaneamento",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = accentBlue)
                                Text("Ambiente claro para melhorar a leitura.", color = textDim, fontSize = 14.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = accentBlue)
                                Text("QR Code nítido, sem dobras e dentro do quadrado de leitura.", color = textDim, fontSize = 14.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = accentBlue)
                                Text("Aponte o quadrado apenas para o QR Code da nota.", color = textDim, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        showScannerGuide = false
                        showCamera = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Escanear QR code da nota")
                }
            }
        }
        return
    }

    if (showCamera) {
        CameraCapturaDialog(
            onDismiss = { showCamera = false },
            onFotoCapturada = { resultado ->
                showCamera = false
                val qrUrl = resultado.qrCodeUrl?.trim()
                if (qrUrl.isNullOrBlank()) {
                    return@CameraCapturaDialog
                }
                AdminUsageMetrics.markQrScanSuccess()
                val notaInfo = resultado.notaQrInfo
                isQrLoading = false
                if (notaInfo != null) {
                    notaInfo.valorTotal?.let { total ->
                        expenseAmount = "R$ ${formatarValorBr(total)}"
                    }
                    val descricaoItens = notaInfo.descricaoItens?.trim().orEmpty()
                    val sugestaoOcr = resultado.sugestoesProduto
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() }
                    val estabelecimento = notaInfo.nomeEstabelecimento?.trim().orEmpty()
                    val estabelecimentoUtil = estabelecimento.isNotBlank() &&
                        !estabelecimento.contains("secretaria da fazenda", ignoreCase = true) &&
                        !estabelecimento.contains("governo do estado", ignoreCase = true)

                    expenseLabel = when {
                        descricaoItens.isNotBlank() -> formatarProdutosParaDescricao(descricaoItens)
                        !sugestaoOcr.isNullOrBlank() -> sugestaoOcr
                        estabelecimentoUtil -> estabelecimento
                        notaInfo.valorTotal != null -> "Nota fiscal (itens indisponiveis)"
                        else -> "Nota fiscal lida (itens indisponiveis)"
                    }
                    Log.i(
                        QR_PARSER_TAG,
                        "Bind UI gastos => notaInfo=ok valor=${notaInfo.valorTotal} descricaoItens=${descricaoItens.isNotBlank()} sugestaoOcr=${!sugestaoOcr.isNullOrBlank()} labelFinal=$expenseLabel"
                    )
                } else {
                    val sugestaoOcr = resultado.sugestoesProduto
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() }
                    expenseLabel = sugestaoOcr ?: "Nota fiscal lida (itens indisponiveis)"
                    Log.i(
                        QR_PARSER_TAG,
                        "Bind UI gastos => notaInfo=null sugestaoOcr=${!sugestaoOcr.isNullOrBlank()} labelFinal=$expenseLabel"
                    )
                }
            }
        )
    }

    if (showPhotoFlowWarning) {
        Dialog(onDismissRequest = { showPhotoFlowWarning = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(34.dp)
                    )
                    Text("Registrar nota", color = textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        buildAnnotatedString {
                            append("Para cada nota, o processo acontece em ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("2 etapas")
                            }
                            append(":\n")
                            append("1) Uma foto da nota para registro.\n")
                            append("2) Uma leitura do ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("QR code")
                            }
                            append(" para preencher os dados automaticamente.\n\n")
                            append("A segunda etapa nao e outra foto completa da nota, e apenas a leitura do QR.")
                        },
                        color = textDim,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = skipPhotoFlowWarning,
                            onCheckedChange = {
                                skipPhotoFlowWarning = it
                                saveSkipPhotoWarning(context, it)
                            }
                        )
                        Text("Nao mostrar mais esse aviso", color = textDim, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showPhotoFlowWarning = false },
                            modifier = Modifier.width(150.dp).height(46.dp),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                showPhotoFlowWarning = false
                                startRegisterNotePhotoFlow()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                            modifier = Modifier.width(150.dp).height(46.dp)
                        ) {
                            Text("Continuar")
                        }
                    }
                }
            }
        }
    }

    if (isQrLoading) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Lendo nota", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentBlue
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando dados da nota...", color = textDim)
                }
            },
            confirmButton = {}
        )
    }

    if (isGeneratingGeneralReport) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (generalReportMode == "print") Icons.Default.Print else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(34.dp)
                    )
                    Text("Gerando relatorio", color = textPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentBlue
                    )
                    Text("Gerando relatorio...", color = textDim, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            confirmButton = {}
        )
    }

    if (showExpenseAddedDialog) {
        Dialog(onDismissRequest = { showExpenseAddedDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(36.dp)
                    )
                    Text("Gasto cadastrado com sucesso!", color = textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        "O gasto foi salvo. Deseja continuar lancando despesas ou ver os gastos da viagem?",
                        color = textDim,
                        fontSize = 13.sp
                    )
                    Text(
                        "Nota cadastrada na viagem de nome: $tripName",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    OutlinedButton(
                        onClick = {
                            showExpenseAddedDialog = false
                            openAddExpenseForm()
                        },
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Adicionar outro")
                    }
                    Button(
                        onClick = {
                            showExpenseAddedDialog = false
                            persistCurrentTrip()
                            showAddExpenseScreen = false
                            showTripsScreen = false
                            showExpensesScreen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Ver gastos")
                    }
                }
            }
        }
    }

}

private fun formatarProdutosParaDescricao(descricaoItens: String): String {
    val itemComValorRegex = Regex("^(.*?)\\s*\\((\\d+[\\.,]\\d{2})\\)\\s*$")
    return descricaoItens
        .split("+")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { item ->
            val match = itemComValorRegex.find(item)
            if (match != null) {
                val nome = match.groupValues[1].trim()
                val valor = match.groupValues[2].trim().replace(".", ",")
                if (nome.isNotBlank()) "$nome - R$ $valor" else item
            } else {
                item
            }
        }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

private fun formatarValorBr(valor: Double): String =
    String.format(Locale("pt", "BR"), "%.2f", valor)

private fun parseValorMonetario(valorInput: String): Double? {
    val limpo = valorInput
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
    return limpo.toDoubleOrNull()
}

private data class ProductsPreview(
    val preview: String,
    val hiddenCount: Int
)

private fun buildProductsPreview(rawProducts: String, maxLines: Int): ProductsPreview {
    val lines = rawProducts
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lines.isEmpty()) return ProductsPreview(rawProducts, 0)
    val previewLines = lines.take(maxLines)
    val hidden = (lines.size - previewLines.size).coerceAtLeast(0)
    return ProductsPreview(
        preview = previewLines.joinToString("\n"),
        hiddenCount = hidden
    )
}

private const val TRAVEL_EXPENSES_PREFS = "travel_expenses_prefs"
private const val KEY_TRAVEL_EXPENSES = "travel_expenses_json"
private const val KEY_TRAVEL_TRIPS = "travel_trips_json"
private const val KEY_SKIP_PHOTO_WARNING = "skip_photo_warning"
private const val KEY_TRIPS_TUTORIAL_VERSION = "trips_tutorial_version"
private const val CURRENT_TRIPS_TUTORIAL_VERSION = 1

private fun saveTravelExpenses(context: Context, expenses: List<TravelExpense>) {
    val jsonArray = JSONArray()
    expenses.forEach { expense ->
        val obj = JSONObject()
            .put("id", expense.id)
            .put("label", expense.label)
            .put("amount", expense.amount)
            .put("category", expense.category)
            .put("vehicleName", expense.vehicleName)
            .put("notePhotoUri", expense.notePhotoUri?.toString())
        jsonArray.put(obj)
    }

    context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_TRAVEL_EXPENSES, jsonArray.toString())
        .apply()
}

private fun loadTravelExpenses(context: Context): List<TravelExpense> {
    val raw = context
        .getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_TRAVEL_EXPENSES, null)
        ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    TravelExpense(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        label = obj.optString("label"),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category"),
                        vehicleName = obj.optString("vehicleName"),
                        notePhotoUri = obj.optString("notePhotoUri").takeIf { it.isNotBlank() }?.let(Uri::parse)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private const val KEY_TRIP_NAME = "trip_name"

private fun saveTripName(context: Context, tripName: String) {
    context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_TRIP_NAME, tripName)
        .apply()
}

private fun loadTripName(context: Context): String {
    return context
        .getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_TRIP_NAME, "Minha viagem")
        .orEmpty()
        .ifBlank { "Minha viagem" }
}

private fun saveSkipPhotoWarning(context: Context, skip: Boolean) {
    context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_SKIP_PHOTO_WARNING, skip)
        .apply()
}

private fun loadSkipPhotoWarning(context: Context): Boolean {
    return context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_SKIP_PHOTO_WARNING, false)
}

private fun markTripsTutorialSeen(context: Context) {
    context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_TRIPS_TUTORIAL_VERSION, CURRENT_TRIPS_TUTORIAL_VERSION)
        .apply()
}

private fun shouldAutoStartTripsTutorial(context: Context): Boolean {
    val savedVersion = context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_TRIPS_TUTORIAL_VERSION, 0)
    return savedVersion < CURRENT_TRIPS_TUTORIAL_VERSION
}

private fun saveTravelTrips(context: Context, trips: List<TravelTrip>) {
    val tripsArray = JSONArray()
    trips.forEach { trip ->
        val expensesArray = JSONArray()
        trip.expenses.forEach { expense ->
            expensesArray.put(
                JSONObject()
                    .put("id", expense.id)
                    .put("label", expense.label)
                    .put("amount", expense.amount)
                    .put("category", expense.category)
                    .put("vehicleName", expense.vehicleName)
                    .put("notePhotoUri", expense.notePhotoUri?.toString())
            )
        }
        tripsArray.put(
            JSONObject()
                .put("id", trip.id)
                .put("name", trip.name)
                .put("location", trip.location)
                .put("responsible", trip.responsible)
                .put("participantEmails", JSONArray().apply { trip.participantEmails.forEach { put(it) } })
                .put("isFinished", trip.isFinished)
                .put("expenses", expensesArray)
        )
    }
    context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_TRAVEL_TRIPS, tripsArray.toString())
        .apply()
}

private fun loadTravelTrips(context: Context): List<TravelTrip> {
    val prefs = context.getSharedPreferences(TRAVEL_EXPENSES_PREFS, Context.MODE_PRIVATE)
    val rawTrips = prefs.getString(KEY_TRAVEL_TRIPS, null)
    if (!rawTrips.isNullOrBlank()) {
        return runCatching {
            val tripsArray = JSONArray(rawTrips)
            buildList {
                for (i in 0 until tripsArray.length()) {
                    val tripObj = tripsArray.getJSONObject(i)
                    val expensesArray = tripObj.optJSONArray("expenses") ?: JSONArray()
                    val expenses = buildList {
                        for (j in 0 until expensesArray.length()) {
                            val obj = expensesArray.getJSONObject(j)
                            add(
                                TravelExpense(
                                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                    label = obj.optString("label"),
                                    amount = obj.optDouble("amount", 0.0),
                                    category = obj.optString("category"),
                                    vehicleName = obj.optString("vehicleName"),
                                    notePhotoUri = obj.optString("notePhotoUri").takeIf { it.isNotBlank() }?.let(Uri::parse)
                                )
                            )
                        }
                    }
                    add(
                        TravelTrip(
                            id = tripObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = tripObj.optString("name").ifBlank { "Minha viagem" },
                            location = tripObj.optString("location"),
                            responsible = tripObj.optString("responsible"),
                            participantEmails = buildList {
                                val participants = tripObj.optJSONArray("participantEmails") ?: JSONArray()
                                for (k in 0 until participants.length()) {
                                    val email = participants.optString(k).trim()
                                    if (email.isNotBlank()) add(email)
                                }
                            },
                            isFinished = tripObj.optBoolean("isFinished", false),
                            expenses = expenses
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    val legacyExpenses = loadTravelExpenses(context)
    val legacyName = loadTripName(context)
    if (legacyExpenses.isNotEmpty()) {
        val migrated = listOf(
            TravelTrip(
                id = UUID.randomUUID().toString(),
                name = legacyName.ifBlank { "Minha viagem" },
                location = "",
                responsible = "",
                participantEmails = emptyList(),
                isFinished = false,
                expenses = legacyExpenses
            )
        )
        saveTravelTrips(context, migrated)
        return migrated
    }
    return emptyList()
}

private fun createTempImageUri(context: Context, prefix: String): Uri {
    val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripsByTravelScreen(
    trips: List<TravelTrip>,
    isDark: Boolean,
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    accentBlue: Color,
    currency: NumberFormat,
    onBack: () -> Unit,
    onExportAllTripsPdf: () -> Unit,
    onPrintAllTripsPdf: () -> Unit,
    onExportAllTripsSpreadsheet: () -> Unit,
    onOpenTrip: (TravelTrip) -> Unit,
    onCreateTrip: () -> Unit,
    onRenameTrip: (tripId: String, newName: String, newLocation: String, newResponsible: String, newParticipants: List<String>) -> Unit,
    onDeleteTrip: (tripId: String) -> Unit
) {
    val context = LocalContext.current
    val backIconTint = if (isDark) Color(0xFFE2E8F0) else Color.Black
    var editingTripId by remember { mutableStateOf<String?>(null) }
    var editingTripName by remember { mutableStateOf("") }
    var editingTripLocation by remember { mutableStateOf("") }
    var editingTripResponsible by remember { mutableStateOf("") }
    var editingTripParticipants by remember { mutableStateOf("") }
    var pendingDeleteTrip by remember { mutableStateOf<TravelTrip?>(null) }
    val shouldAutoStartTutorial = remember(context) { shouldAutoStartTripsTutorial(context) }
    var showTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableStateOf(0) }
    var pdfButtonRect by remember { mutableStateOf<Rect?>(null) }
    var printButtonRect by remember { mutableStateOf<Rect?>(null) }
    var addButtonRect by remember { mutableStateOf<Rect?>(null) }
    var firstTripRect by remember { mutableStateOf<Rect?>(null) }
    var showExportScreen by remember { mutableStateOf(false) }
    val tutorialSteps = remember {
        listOf(
            "Exportar: toque aqui para abrir as opcoes de PDF, impressao e planilha geral das viagens." to "pdf",
            "Imprimir geral: use no menu Exportar para abrir a impressÃ£o do relatÃ³rio completo de viagens." to "print",
            "Novo gasto: toque no botÃ£o + para abrir o formulÃ¡rio e cadastrar uma despesa na viagem escolhida." to "add",
            "Detalhes da viagem: toque em uma viagem para ver os gastos, editar informaÃ§Ãµes e excluir o que precisar." to "trip"
        )
    }
    LaunchedEffect(shouldAutoStartTutorial) {
        if (shouldAutoStartTutorial) {
            tutorialStep = 0
            showTutorial = true
        }
    }

    if (showExportScreen) {
        ExportOptionsScreen(
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            accentBlue = accentBlue,
            subtitle = "Use os arquivos abaixo para compartilhar o controle centralizado.",
            actions = listOf(
                ExportOptionAction(
                    label = "Imprimir",
                    icon = Icons.Default.Print,
                    onClick = {
                        showExportScreen = false
                        onPrintAllTripsPdf()
                    }
                ),
                ExportOptionAction(
                    label = "Exportar PDF",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        showExportScreen = false
                        onExportAllTripsPdf()
                    }
                ),
                ExportOptionAction(
                    label = "Exportar planilha + fotos",
                    icon = Icons.Default.ReceiptLong,
                    onClick = {
                        showExportScreen = false
                        onExportAllTripsSpreadsheet()
                    }
                )
            ),
            isDark = isDark,
            onBack = { showExportScreen = false }
        )
        return
    }

    Scaffold(containerColor = bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 0.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = backIconTint)
                }
                ExportActionButton(
                    isDark = isDark,
                    onClick = { showExportScreen = true },
                    modifier = Modifier.onGloballyPositioned {
                        val bounds = it.boundsInRoot()
                        pdfButtonRect = bounds
                        printButtonRect = bounds
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-8).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = accentBlue.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(y = (-4).dp)
                        )
                    }
                }
                Text(
                    "Viagem",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.offset(y = 3.dp)
                )
            }

            Button(
                onClick = {
                    onCreateTrip()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .onGloballyPositioned { addButtonRect = it.boundsInRoot() }
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Criar viagem", fontWeight = FontWeight.SemiBold)
            }

            if (trips.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = textDim, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhuma viagem criada.", color = textDim, fontSize = 12.sp)
                    }
                }
            } else {
                trips.forEachIndexed { idx, trip ->
                    val total = trip.expenses.sumOf { it.amount }
                    val tripCardBg = if (isDark) Color(0xFF0E1628) else Color(0xFFFCFDFF)
                    val chipBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)
                    val infoCardBorderColor = cardBorder.copy(alpha = if (isDark) 0.34f else 0.28f)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (idx == 0) Modifier.onGloballyPositioned { firstTripRect = it.boundsInRoot() } else Modifier)
                            .clickable { onOpenTrip(trip) },
                        colors = CardDefaults.cardColors(containerColor = tripCardBg),
                        border = BorderStroke(1.dp, cardBorder.copy(alpha = if (isDark) 0.78f else 0.5f)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.05f) else accentBlue.copy(alpha = 0.06f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, cardBorder.copy(alpha = if (isDark) 0.34f else 0.28f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(accentBlue.copy(alpha = if (isDark) 0.24f else 0.14f), RoundedCornerShape(999.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = accentBlue, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        trip.name,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    currency.format(total),
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .background(chipBg, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (trip.isFinished) {
                                    Text(
                                        "Finalizada",
                                        color = Color(0xFF16A34A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .background(
                                                Color(0xFF16A34A).copy(alpha = if (isDark) 0.22f else 0.12f),
                                                RoundedCornerShape(999.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                } else {
                                    Text(
                                        "Em andamento",
                                        color = accentBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .background(accentBlue.copy(alpha = if (isDark) 0.22f else 0.12f), RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    "${trip.expenses.size} gastos",
                                    color = textDim,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .background(chipBg, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (trip.location.isNotBlank() || trip.responsible.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(chipBg, RoundedCornerShape(10.dp))
                                        .border(1.dp, infoCardBorderColor, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (trip.location.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.LocationOn,
                                                contentDescription = null,
                                                tint = textDim,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                "Local:",
                                                color = textDim,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                trip.location,
                                                color = textDim,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (trip.responsible.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = textDim,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                "Responsavel:",
                                                color = textDim,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                trip.responsible,
                                                color = textDim,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = infoCardBorderColor)

                            OutlinedButton(
                                onClick = { onOpenTrip(trip) },
                                border = BorderStroke(1.dp, cardBorder.copy(alpha = if (isDark) 0.34f else 0.28f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = textPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ver gastos", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editingTripId = trip.id
                                        editingTripName = trip.name
                                        editingTripLocation = trip.location
                                        editingTripResponsible = trip.responsible
                                        editingTripParticipants = trip.participantEmails.joinToString(", ")
                                    },
                                    border = BorderStroke(1.dp, cardBorder.copy(alpha = if (isDark) 0.34f else 0.28f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Editar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { pendingDeleteTrip = trip },
                                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Excluir", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingTripId != null) {
        Dialog(onDismissRequest = { editingTripId = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Editar viagem", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = editingTripName,
                        onValueChange = { editingTripName = it },
                        label = { Text("Nome da viagem") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingTripLocation,
                        onValueChange = { editingTripLocation = it },
                        label = { Text("Local") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingTripResponsible,
                        onValueChange = { editingTripResponsible = it },
                        label = { Text("Responsavel") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingTripParticipants,
                        onValueChange = { editingTripParticipants = it },
                        label = { Text("E-mails dos participantes") },
                        placeholder = { Text("email1@exemplo.com, email2@exemplo.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { editingTripId = null }) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val tripId = editingTripId
                                if (tripId != null) {
                                    onRenameTrip(
                                        tripId,
                                        editingTripName.trim(),
                                        editingTripLocation,
                                        editingTripResponsible,
                                        parseParticipantEmails(editingTripParticipants)
                                    )
                                }
                                editingTripId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteTrip != null) {
        val tripToDelete = pendingDeleteTrip
        Dialog(onDismissRequest = { pendingDeleteTrip = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Excluir viagem",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Tem certeza que deseja apagar a viagem ${tripToDelete?.name?.let { "\"$it\"" } ?: ""}?",
                        color = textDim,
                        fontSize = 14.sp
                    )
                    Text(
                        "Essa acao remove os gastos vinculados a ela.",
                        color = textDim,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { pendingDeleteTrip = null },
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                        ) {
                            Text("Cancelar")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val tripId = tripToDelete?.id
                                pendingDeleteTrip = null
                                if (tripId != null) {
                                    onDeleteTrip(tripId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Apagar")
                        }
                    }
                }
            }
        }
    }

    if (showTutorial) {
        val (message, targetKey) = tutorialSteps[tutorialStep]
        val targetRect = when (targetKey) {
            "pdf" -> pdfButtonRect
            "print" -> printButtonRect
            "add" -> addButtonRect
            else -> firstTripRect
        }
        TutorialVoiceNarration(
            enabled = true,
            text = message
        )
        TutorialSpotlightOverlay(
            targetRect = targetRect,
            message = message,
            step = tutorialStep + 1,
            total = tutorialSteps.size,
            accentBlue = accentBlue,
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            onSkip = {
                showTutorial = false
                markTripsTutorialSeen(context)
            },
            onNext = {
                if (tutorialStep < tutorialSteps.lastIndex) {
                    tutorialStep += 1
                } else {
                    showTutorial = false
                    markTripsTutorialSeen(context)
                }
            }
        )
    }
}

@Composable
private fun TutorialVoiceNarration(
    enabled: Boolean,
    text: String
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = engine?.setLanguage(Locale("pt", "BR")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                engine?.let { configureTutorialVoice(it) }
            }
        }
        tts = engine
        onDispose {
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    LaunchedEffect(enabled, text, isReady) {
        if (enabled && isReady) {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "trips_tutorial_step")
        }
    }
}

private fun configureTutorialVoice(tts: TextToSpeech) {
    tts.setSpeechRate(1.0f)
    tts.setPitch(0.92f)
    val maleHints = listOf("male", "masc", "homem", "m")
    val preferred = runCatching {
        tts.voices
            ?.filter { voice -> voice.locale?.language == "pt" }
            ?.sortedBy { voice ->
                val name = voice.name.lowercase(Locale.ROOT)
                when {
                    name.contains("pt-br") -> 0
                    name.contains("brazil") -> 1
                    else -> 2
                }
            }
            ?.firstOrNull { voice ->
                val name = voice.name.lowercase(Locale.ROOT)
                val features = voice.features?.joinToString(" ")?.lowercase(Locale.ROOT).orEmpty()
                maleHints.any { hint -> name.contains(hint) || features.contains(hint) }
            }
            ?: tts.voices?.firstOrNull { it.locale?.language == "pt" }
    }.getOrNull()
    preferred?.let { tts.voice = it }
}

@Composable
private fun TutorialSpotlightOverlay(
    targetRect: Rect?,
    message: String,
    step: Int,
    total: Int,
    accentBlue: Color,
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawRect(Color.Black.copy(alpha = 0.55f))
                if (targetRect != null) {
                    val strokeWidth = 2.5f
                    val inset = strokeWidth / 2f
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = androidx.compose.ui.geometry.Offset(targetRect.left, targetRect.top),
                        size = androidx.compose.ui.geometry.Size(targetRect.width, targetRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = Color(0xFF60A5FA),
                        topLeft = androidx.compose.ui.geometry.Offset(targetRect.left + inset, targetRect.top + inset),
                        size = androidx.compose.ui.geometry.Size(targetRect.width - (inset * 2f), targetRect.height - (inset * 2f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(11f, 11f),
                        style = Stroke(width = strokeWidth)
                    )
                }
                drawContent()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Guia rÃ¡pido", color = textPrimary, fontWeight = FontWeight.Bold)
                Text("Etapa $step de $total", color = textDim, fontSize = 12.sp)
                Text(message, color = textPrimary, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onSkip, border = BorderStroke(1.dp, cardBorder)) {
                        Text("Pular guia")
                    }
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                    ) {
                        Text(if (step < total) "AvanÃ§ar" else "Finalizar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripScreen(
    isDark: Boolean,
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    accentBlue: Color,
    onBack: () -> Unit,
    onCreate: (tripName: String, location: String, responsible: String, participantEmails: List<String>) -> Unit
) {
    var tripName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var responsible by remember { mutableStateOf("") }
    var participantEmailsRaw by remember { mutableStateOf("") }
    var travelingAlone by remember { mutableStateOf(false) }
    val backIconTint = if (isDark) Color(0xFFE2E8F0) else Color.Black
    val keyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 0.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = backIconTint)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = accentBlue.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    "Nova viagem",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            OutlinedTextField(
                value = tripName,
                onValueChange = { tripName = it },
                label = { Text("Nome da viagem") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            OutlinedTextField(
                value = responsible,
                onValueChange = { responsible = it },
                label = { Text("Responsável") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Local") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            OutlinedTextField(
                value = participantEmailsRaw,
                onValueChange = { participantEmailsRaw = it },
                label = { Text("E-mails dos acompanhantes") },
                placeholder = { Text("email1@exemplo.com, email2@exemplo.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !travelingAlone,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = travelingAlone,
                    onCheckedChange = {
                        travelingAlone = it
                        if (it) {
                            participantEmailsRaw = ""
                            keyboardController?.hide()
                        }
                    }
                )
                Text("Estou viajando sozinho", color = textDim, fontSize = 13.sp)
            }
            Button(
                onClick = {
                    onCreate(
                        tripName.trim().ifBlank { "Nova viagem" },
                        location.trim(),
                        responsible.trim(),
                        if (travelingAlone) emptyList() else parseParticipantEmails(participantEmailsRaw)
                    )
                },
                enabled = tripName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Criar viagem", fontWeight = FontWeight.SemiBold)
            }
        }
    }

}

private fun parseParticipantEmails(raw: String): List<String> {
    return raw
        .split(",", ";", "\n")
        .map { it.trim() }
        .filter { it.contains("@") && it.isNotBlank() }
        .distinct()
}

private fun abrirEventoAgendaDaViagem(context: Context, trip: TravelTrip) {
    val now = System.currentTimeMillis()
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "Viagem: ${trip.name}")
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            "Viagem criada no Zellu. Responsavel: ${trip.responsible.ifBlank { "Nao informado" }}."
        )
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, now)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, now + (60 * 60 * 1000))
        if (trip.participantEmails.isNotEmpty()) {
            putExtra(Intent.EXTRA_EMAIL, trip.participantEmails.toTypedArray())
        }
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "Nao foi possivel abrir a agenda.", Toast.LENGTH_SHORT).show()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTravelExpenseScreen(
    tripName: String,
    categories: List<String>,
    vehicleOptions: List<String>,
    otherVehicleLabel: String,
    isDark: Boolean,
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    accentBlue: Color,
    category: String,
    onCategoryChange: (String) -> Unit,
    selectedVehicleName: String,
    onVehicleChange: (String) -> Unit,
    customVehicleName: String,
    onCustomVehicleNameChange: (String) -> Unit,
    expenseLabel: String,
    onExpenseLabelChange: (String) -> Unit,
    expenseAmount: String,
    onExpenseAmountChange: (String) -> Unit,
    photoCount: Int,
    onBack: () -> Unit,
    onAddPhoto: () -> Unit,
    onSave: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showVehicleSelectorDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val canSave = expenseLabel.trim().isNotBlank() &&
        expenseAmount.trim().isNotBlank() &&
        selectedVehicleName.isNotBlank() &&
        (selectedVehicleName != otherVehicleLabel || customVehicleName.trim().isNotBlank())

    Scaffold(
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = accentBlue.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    "Adicionar gasto",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onAddPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escanear QR code da nota", fontWeight = FontWeight.SemiBold)
            }

            OutlinedTextField(
                value = expenseLabel,
                onValueChange = onExpenseLabelChange,
                label = { Text("Itens") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedFieldColors(bg, textPrimary, textDim, isDark),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )

            OutlinedTextField(
                value = expenseAmount,
                onValueChange = onExpenseAmountChange,
                label = { Text("Valor total") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedFieldColors(bg, textPrimary, textDim, isDark)
            )

            SelectorCard(
                label = "Categoria",
                value = category,
                icon = null,
                textPrimary = textPrimary,
                textDim = textDim,
                accentBlue = accentBlue,
                borderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                containerColor = if (isDark) Color(0xFF111827) else Color.White,
                onClick = { showCategoryDialog = true }
            )

            SelectorCard(
                label = "Veiculo",
                value = selectedVehicleName.ifBlank { "Selecionar veiculo" },
                icon = null,
                textPrimary = textPrimary,
                textDim = textDim,
                accentBlue = accentBlue,
                borderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                containerColor = if (isDark) Color(0xFF111827) else Color.White,
                onClick = { showVehicleSelectorDialog = true }
            )

            if (selectedVehicleName == otherVehicleLabel) {
                OutlinedTextField(
                    value = customVehicleName,
                    onValueChange = onCustomVehicleNameChange,
                    label = { Text("Nome do veiculo externo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(bg, textPrimary, textDim, isDark),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )
            }

            if (photoCount > 0) {
                Text("Fotos adicionadas: $photoCount", color = textDim, fontSize = 12.sp)
            }

            Button(
                onClick = onSave,
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Salvar", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showCategoryDialog) {
        SelectionDialog(
            title = "Categoria",
            bg = bg,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            titleCentered = true,
            titleFontSize = 20.sp,
            onDismiss = { showCategoryDialog = false }
        ) {
            categories.forEach { item ->
                OutlinedButton(
                    onClick = {
                        onCategoryChange(item)
                        showCategoryDialog = false
                    },
                    border = BorderStroke(1.dp, cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(item)
                }
            }
        }
    }

    if (showVehicleSelectorDialog) {
        SelectionDialog(
            title = "Selecionar veiculo",
            bg = bg,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            onDismiss = { showVehicleSelectorDialog = false }
        ) {
            vehicleOptions.forEach { option ->
                OutlinedButton(
                    onClick = {
                        onVehicleChange(option)
                        showVehicleSelectorDialog = false
                    },
                    border = BorderStroke(1.dp, cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option)
                }
            }
        }
    }
}

@Composable
private fun ExportActionButton(
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    val border = if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE)
    val content = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1D4ED8)

    Row(
        modifier = modifier
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .background(container, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Upload, contentDescription = "Exportar", tint = content, modifier = Modifier.size(18.dp))
    }
}

private data class ExportOptionAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun ExportOptionsScreen(
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    accentBlue: Color,
    subtitle: String,
    actions: List<ExportOptionAction>,
    isDark: Boolean,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(containerColor = bg) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = accentBlue.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    "Exportar",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    subtitle,
                    color = textDim,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                actions.forEach { action ->
                    OutlinedButton(
                        onClick = action.onClick,
                        border = BorderStroke(1.dp, cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(action.icon, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(action.label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    textPrimary: Color,
    textDim: Color,
    accentBlue: Color,
    borderColor: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(label, color = textDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(value, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textDim)
                }
            }
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    bg: Color,
    cardBorder: Color,
    textPrimary: Color,
    titleCentered: Boolean = false,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = {
                    Text(
                        title,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = titleFontSize,
                        modifier = if (titleCentered) Modifier.fillMaxWidth() else Modifier,
                        textAlign = if (titleCentered) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                    )
                    Spacer(Modifier.height(6.dp))
                    content()
                }
            )
        }
    }
}

@Composable
private fun outlinedFieldColors(
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    isDark: Boolean
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textPrimary,
    unfocusedTextColor = textPrimary,
    cursorColor = textPrimary,
    focusedBorderColor = if (isDark) Color(0xFF334155) else Color.Black,
    unfocusedBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
    focusedContainerColor = bg,
    unfocusedContainerColor = bg,
    focusedLabelColor = textDim,
    unfocusedLabelColor = textDim
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelExpensesScreen(
    tripName: String,
    onTripNameChange: (String) -> Unit,
    tripLocation: String,
    onTripLocationChange: (String) -> Unit,
    expenses: MutableList<TravelExpense>,
    onPersistExpenses: () -> Unit,
    isTripFinished: Boolean,
    isDark: Boolean,
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    accentBlue: Color,
    currency: NumberFormat,
    categories: List<String>,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onFinishTrip: () -> Unit,
    onReopenTrip: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onExportSpreadsheetWithPhotos: () -> Unit
) {
    val context = LocalContext.current
    val backIconTint = if (isDark) Color(0xFFE2E8F0) else Color.Black
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var editExpenseLabel by remember { mutableStateOf("") }
    var editExpenseAmount by remember { mutableStateOf("") }
    var editExpenseCategory by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var showExportScreen by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf(false) }

    if (showExportScreen) {
        ExportOptionsScreen(
            bg = bg,
            textPrimary = textPrimary,
            textDim = textDim,
            cardBorder = cardBorder,
            accentBlue = accentBlue,
            subtitle = "Exporte para compartilhar os registros desta viagem.",
            actions = listOf(
                ExportOptionAction(
                    label = "Exportar PDF",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        showExportScreen = false
                        onExportPdf()
                    }
                ),
                ExportOptionAction(
                    label = "Exportar planilha + fotos",
                    icon = Icons.Default.ReceiptLong,
                    onClick = {
                        showExportScreen = false
                        onExportSpreadsheetWithPhotos()
                    }
                )
            ),
            isDark = isDark,
            onBack = { showExportScreen = false }
        )
        return
    }

    Scaffold(containerColor = bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 0.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                }
                ExportActionButton(
                    isDark = isDark,
                    onClick = { showExportScreen = true }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = accentBlue.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    "Adicionar gasto",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            if (isTripFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Viagem finalizada", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Esta viagem foi encerrada. Os gastos ficam bloqueados ate voce reabrir.",
                            color = textDim,
                            fontSize = 12.sp
                        )
                        OutlinedButton(
                            onClick = { showReopenDialog = true },
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Reabrir viagem", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onAddExpense,
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar gasto", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { showFinishDialog = true },
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Finalizar viagem", fontWeight = FontWeight.SemiBold)
                }
            }

            if (expenses.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = textDim, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhum gasto adicionado.", color = textDim, fontSize = 12.sp)
                    }
                }
            } else {
                expenses.forEach { expense ->
                    val expenseCardBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    val productsPreview = buildProductsPreview(expense.label, 5)
                    var showAllItems by remember(expense.id) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = expenseCardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    expense.category,
                                    color = accentBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .background(accentBlue.copy(alpha = if (isDark) 0.22f else 0.14f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                                Text(currency.format(expense.amount), color = textPrimary, fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(color = cardBorder)
                            if (expense.vehicleName.isNotBlank()) {
                                Text("Veiculo: ${expense.vehicleName}", color = textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                if (showAllItems) expense.label else productsPreview.preview,
                                color = textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            if (productsPreview.hiddenCount > 0) {
                                Text(
                                    if (showAllItems) "Ocultar itens" else "+${productsPreview.hiddenCount} itens",
                                    color = accentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        showAllItems = !showAllItems
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (expense.notePhotoUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedPhotoUri = expense.notePhotoUri
                                        },
                                        border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.7f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = "Ver foto da nota", tint = textDim, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ver foto da nota", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (!isTripFinished) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingExpenseId = expense.id
                                                editExpenseLabel = expense.label
                                                editExpenseAmount = "R$ ${formatarValorBr(expense.amount)}"
                                                editExpenseCategory = expense.category
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar gasto", tint = accentBlue)
                                        }
                                        IconButton(
                                            onClick = {
                                                expenses.removeAll { it.id == expense.id }
                                                onPersistExpenses()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Apagar gasto", tint = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        Dialog(onDismissRequest = { showFinishDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Finalizar viagem", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Deseja encerrar esta viagem e bloquear novos gastos?", color = textDim, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { showFinishDialog = false },
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                        ) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showFinishDialog = false
                                onFinishTrip()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                        ) { Text("Finalizar") }
                    }
                }
            }
        }
    }

    if (showReopenDialog) {
        Dialog(onDismissRequest = { showReopenDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reabrir viagem", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Deseja reabrir esta viagem para voltar a adicionar gastos?", color = textDim, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { showReopenDialog = false },
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                        ) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showReopenDialog = false
                                onReopenTrip()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                        ) { Text("Reabrir") }
                    }
                }
            }
        }
    }

    if (selectedPhotoUri != null) {
        Dialog(onDismissRequest = { selectedPhotoUri = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                val bitmap = remember(selectedPhotoUri) {
                    selectedPhotoUri?.let { uri ->
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.let(::toPortraitBitmap)
                        }.getOrNull()
                    }
                }
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Foto da nota", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto da nota",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(430.dp)
                        )
                    } else {
                        Text("Nao foi possivel carregar a foto.", color = textDim, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { selectedPhotoUri = null }) { Text("Fechar") }
                    }
                }
            }
        }
    }

    if (editingExpenseId != null) {
        Dialog(onDismissRequest = { editingExpenseId = null }) {
            Card(colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(1.dp, cardBorder), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Editar gasto", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = editExpenseLabel, onValueChange = { editExpenseLabel = it }, label = { Text("Itens") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = editExpenseAmount,
                        onValueChange = { editExpenseAmount = it },
                        label = { Text("Valor Total") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    var showEditCategoryDialog by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showEditCategoryDialog = true }) {
                        OutlinedTextField(value = editExpenseCategory, onValueChange = {}, readOnly = true, enabled = false, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (showEditCategoryDialog) {
                        Dialog(onDismissRequest = { showEditCategoryDialog = false }) {
                            Card(colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(1.dp, cardBorder), shape = RoundedCornerShape(14.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Categoria", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                    categories.forEach { item ->
                                        OutlinedButton(onClick = { editExpenseCategory = item; showEditCategoryDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                            Text(item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { editingExpenseId = null }) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = parseValorMonetario(editExpenseAmount)
                                if (amount == null || amount <= 0.0) {
                                    Toast.makeText(context, "Informe um valor valido.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val idx = expenses.indexOfFirst { it.id == editingExpenseId }
                                if (idx >= 0) {
                                    expenses[idx] = expenses[idx].copy(
                                        label = editExpenseLabel.ifBlank { editExpenseCategory },
                                        amount = amount,
                                        category = editExpenseCategory
                                    )
                                    onPersistExpenses()
                                }
                                editingExpenseId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                        ) { Text("Salvar") }
                    }
                }
            }
        }
    }
}

private fun generateTripReportPdf(
    context: Context,
    tripName: String,
    location: String,
    responsible: String,
    expenses: List<TravelExpense>
): File? {
    return runCatching {
        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 22f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            textSize = 10.5f
            color = android.graphics.Color.parseColor("#475569")
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            textSize = 12.5f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val valueBoldPaint = Paint().apply {
            textSize = 12.5f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val categoryPaint = Paint(valueBoldPaint).apply {
            textSize = 12.8f
            letterSpacing = 0.02f
        }
        val smallPaint = Paint().apply {
            textSize = 9.5f
            color = android.graphics.Color.parseColor("#334155")
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            strokeWidth = 1.2f
            color = android.graphics.Color.parseColor("#CBD5E1")
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val cardStrokePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val accentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val logoBitmap = runCatching {
            context.assets.open("logorelatorio.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        val margin = 36f
        val contentWidth = 595f - margin * 2
        val pageWidth = 595
        var y = 72f
        val totalGastos = expenses.sumOf { it.amount }
        val locationText = location.ifBlank { "-" }
        val responsibleText = responsible.ifBlank { "-" }

        fun ensureSpace(extra: Float) {
            if (y + extra > 802f) {
                document.finishPage(page)
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create())
                canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentPaint)
                y = 72f
            }
        }

        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentPaint)
        val titleCenterPaint = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER }
        val subtitleCenterPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("RELATORIO DE VIAGEM", pageWidth / 2f, 48f, titleCenterPaint)
        canvas.drawText(
            "Gerado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())}",
            pageWidth / 2f,
            68f,
            subtitleCenterPaint
        )
        canvas.drawLine(margin, 84f, pageWidth - margin, 84f, dividerPaint)
        y = 108f

        val resumoRect = RectF(margin, y, margin + contentWidth, y + 154f)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardBgPaint)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardStrokePaint)
        canvas.drawText("VIAGEM", margin + 14f, y + 22f, labelPaint)
        canvas.drawText(tripName.take(36), margin + 14f, y + 40f, valueBoldPaint)
        canvas.drawText("TOTAL GASTO", margin + 14f, y + 62f, labelPaint)
        canvas.drawText(currency.format(totalGastos), margin + 14f, y + 80f, valuePaint)
        canvas.drawText("LOCAL", margin + 14f, y + 102f, valueBoldPaint)
        canvas.drawText(locationText.take(44), margin + 14f, y + 120f, valuePaint)
        canvas.drawText("LANCAMENTOS", margin + contentWidth / 2f + 10f, y + 22f, labelPaint)
        canvas.drawText(expenses.size.toString(), margin + contentWidth / 2f + 10f, y + 40f, valueBoldPaint)
        canvas.drawText("RESPONSAVEL", margin + contentWidth / 2f + 10f, y + 62f, labelPaint)
        canvas.drawText(responsibleText.take(24), margin + contentWidth / 2f + 10f, y + 80f, valueBoldPaint)
        y += 182f

        canvas.drawText("GASTOS DETALHADOS", margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 14f

        expenses.forEachIndexed { idx, expense ->
            val detailLines = buildPdfDetailLines(expense.label, 84)
            val rowHeight = maxOf(132f, 48f + (detailLines.size * 12f) + 18f)
            ensureSpace(rowHeight + 14f)
            val rowTop = y
            val rowRect = RectF(
                margin,
                rowTop,
                margin + contentWidth,
                rowTop + rowHeight
            )
            canvas.drawRoundRect(rowRect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(rowRect, 12f, 12f, cardStrokePaint)

            var textY = rowTop + 20f
            canvas.drawText(
                "${idx + 1}. ${expense.category}",
                margin + 12f,
                textY,
                categoryPaint
            )
            val rightPaint = Paint(valueBoldPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(currency.format(expense.amount), margin + contentWidth - 12f, textY, rightPaint)
            textY += 16f
            detailLines.forEach { line ->
                if (textY > rowTop + rowHeight - 22f) return@forEach
                canvas.drawText(line, margin + 12f, textY, smallPaint)
                textY += 12f
            }
            y = rowTop + rowHeight + 12f
        }

        val expensesWithPhoto = expenses.filter { it.notePhotoUri != null }
        expensesWithPhoto.forEachIndexed { index, expense ->
            val photoUri = expense.notePhotoUri ?: return@forEachIndexed
            val bitmap = runCatching {
                context.contentResolver.openInputStream(photoUri)?.use(BitmapFactory::decodeStream)
            }.getOrNull() ?: return@forEachIndexed
            val portraitBitmap = toPortraitBitmap(bitmap)

            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create())
            canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentPaint)

            val subtitleInfoPaint = Paint(subtitlePaint).apply {
                color = android.graphics.Color.parseColor("#334155")
                textAlign = Paint.Align.LEFT
            }
            val imageTop = 150f
            val imageHeight = 620f
            canvas.drawText("COMPROVANTE ${index + 1} - ${tripName.take(28)}", margin, 46f, titlePaint)
            canvas.drawText("Viagem: ${tripName.take(42)}", margin, 68f, subtitleInfoPaint)
            canvas.drawText("Categoria: ${expense.category}", margin, 86f, subtitleInfoPaint)
            canvas.drawText("Valor: ${currency.format(expense.amount)}", margin, 104f, subtitleInfoPaint)
            canvas.drawText(
                "Descricao: ${expense.label.lineSequence().firstOrNull().orEmpty().take(70)}",
                margin,
                122f,
                subtitleInfoPaint
            )
            canvas.drawLine(margin, 132f, pageWidth - margin, 132f, dividerPaint)
            drawBitmapFit(canvas, portraitBitmap, margin, imageTop, contentWidth, imageHeight)
        }

        if (logoBitmap != null) {
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create())
            canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)

            val targetWidth = 170f
            val scale = targetWidth / logoBitmap.width.toFloat()
            val targetHeight = logoBitmap.height * scale
            val scaled = Bitmap.createScaledBitmap(logoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
            val left = (pageWidth - targetWidth) / 2f
            val lineY = 380f
            val logoTop = lineY + 14f

            canvas.drawLine(margin, lineY, pageWidth - margin, lineY, dividerPaint)
            canvas.drawBitmap(scaled, left, logoTop, null)
        }

        document.finishPage(page)
        val pdfFile = File(context.cacheDir, "trip_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use(document::writeTo)
        document.close()
        pdfFile
    }.getOrNull()
}

private fun generateAllTripsReportPdf(context: Context, trips: List<TravelTrip>): File? {
    return runCatching {
        val reportTrips = trips.filter { it.name.isNotBlank() || it.expenses.isNotEmpty() }
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)

        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 22f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            textSize = 10.5f
            color = android.graphics.Color.parseColor("#475569")
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            textSize = 11.8f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val valueBoldPaint = Paint().apply {
            textSize = 12.3f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val categoryPaint = Paint(valuePaint).apply {
            textSize = 12.1f
            letterSpacing = 0.02f
        }
        val smallPaint = Paint().apply {
            textSize = 9.5f
            color = android.graphics.Color.parseColor("#334155")
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            strokeWidth = 1.2f
            color = android.graphics.Color.parseColor("#CBD5E1")
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val cardStrokePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val accentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        var y = 72f

        fun drawPageHeader(title: String, subtitle: String) {
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentPaint)
            val titleCenterPaint = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER }
            val subtitleCenterPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(title, pageWidth / 2f, 48f, titleCenterPaint)
            canvas.drawText(subtitle, pageWidth / 2f, 68f, subtitleCenterPaint)
            canvas.drawLine(margin, 84f, pageWidth - margin, 84f, dividerPaint)
            y = 108f
        }

        fun nextPage(title: String, subtitle: String) {
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create())
            canvas = page.canvas
            drawPageHeader(title, subtitle)
        }

        fun ensureSpace(extra: Float) {
            if (y + extra > pageHeight - 40f) {
                nextPage("RELATORIO GERAL DE VIAGENS", "Continuacao")
            }
        }

        drawPageHeader(
            "RELATORIO GERAL DE VIAGENS",
            "Gerado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())}"
        )

        val totalExpenses = reportTrips.sumOf { trip -> trip.expenses.sumOf { it.amount } }
        val totalLaunches = reportTrips.sumOf { it.expenses.size }

        val resumoRect = RectF(margin, y, margin + contentWidth, y + 98f)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardBgPaint)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardStrokePaint)
        canvas.drawText("VIAGENS", margin + 14f, y + 22f, labelPaint)
        canvas.drawText(reportTrips.size.toString(), margin + 14f, y + 42f, valueBoldPaint)
        canvas.drawText("LANCAMENTOS", margin + 14f, y + 64f, labelPaint)
        canvas.drawText(totalLaunches.toString(), margin + 14f, y + 84f, valuePaint)
        canvas.drawText("TOTAL GERAL", margin + contentWidth / 2f + 10f, y + 22f, labelPaint)
        canvas.drawText(currency.format(totalExpenses), margin + contentWidth / 2f + 10f, y + 42f, valueBoldPaint)
        y += 118f

        canvas.drawText("VIAGENS E GASTOS", margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 14f

        if (reportTrips.isEmpty()) {
            canvas.drawText("Nenhuma viagem cadastrada.", margin, y + 4f, valuePaint)
            y += 22f
        }

        reportTrips.forEachIndexed { index, trip ->
            val tripTotal = trip.expenses.sumOf { it.amount }
                val sectionHeight = (108f + (trip.expenses.size * 32f)).coerceAtMost(400f)
            ensureSpace(sectionHeight + 18f)

            val tripRect = RectF(margin, y, margin + contentWidth, y + sectionHeight)
            canvas.drawRoundRect(tripRect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(tripRect, 12f, 12f, cardStrokePaint)

            var textY = y + 22f
            canvas.drawText("${index + 1}. ${trip.name}", margin + 12f, textY, valueBoldPaint)
            val rightPaint = Paint(valueBoldPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(currency.format(tripTotal), margin + contentWidth - 12f, textY, rightPaint)
            textY += 16f
            val boldLabelPaint = Paint(smallPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            canvas.drawText("Responsavel:", margin + 12f, textY, boldLabelPaint)
            canvas.drawText(trip.responsible.ifBlank { "-" }.take(56), margin + 84f, textY, smallPaint)
            textY += 12f
            canvas.drawText("Local:", margin + 12f, textY, boldLabelPaint)
            canvas.drawText(trip.location.ifBlank { "-" }.take(64), margin + 50f, textY, smallPaint)
            textY += 12f
            canvas.drawText("${trip.expenses.size} gastos", margin + 12f, textY, smallPaint)
            textY += 12f
            canvas.drawLine(margin + 12f, textY, margin + contentWidth - 12f, textY, dividerPaint)
            textY += 16f

            if (trip.expenses.isEmpty()) {
                canvas.drawText("Sem gastos cadastrados.", margin + 12f, textY, smallPaint)
            } else {
                trip.expenses.forEachIndexed { expenseIndex, expense ->
                    if (textY > y + sectionHeight - 16f) return@forEachIndexed
                    val firstLine = expense.label.lineSequence().firstOrNull().orEmpty()
                    canvas.drawText("${expenseIndex + 1}. ${expense.category}", margin + 12f, textY, categoryPaint)
                    val amountPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText(currency.format(expense.amount), margin + contentWidth - 12f, textY, amountPaint)
                    textY += 12f
                    canvas.drawText(firstLine.take(76), margin + 12f, textY, smallPaint)
                    textY += 16f
                }
            }
            y += sectionHeight + 12f
        }

        document.finishPage(page)
        val pdfFile = File(context.cacheDir, "trip_general_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use(document::writeTo)
        document.close()
        pdfFile
    }.getOrNull()
}

private fun generateAllTripsSpreadsheet(context: Context, trips: List<TravelTrip>): File? = runCatching {
    val reportTrips = trips.filter { it.name.isNotBlank() || it.expenses.isNotEmpty() }
    val spreadsheetFile = File(context.cacheDir, "relatorio_geral_viagens.xls")
    val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    val spreadsheetContent = buildString {
        appendLine("""<?xml version="1.0"?>""")
        appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"""")
        appendLine(""" xmlns:o="urn:schemas-microsoft-com:office:office"""")
        appendLine(""" xmlns:x="urn:schemas-microsoft-com:office:excel"""")
        appendLine(""" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"""")
        appendLine(""" xmlns:html="http://www.w3.org/TR/REC-html40">""")
        appendLine("""<Styles>""")
        appendLine("""<Style ss:ID="Default"><Alignment ss:Vertical="Center"/><Font ss:FontName="Calibri" ss:Size="11"/><Interior/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/><Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/></Borders></Style>""")
        appendLine("""<Style ss:ID="Title"><Font ss:FontName="Calibri" ss:Size="14" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#1D4ED8" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="SummaryLabel"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F172A"/><Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="SummaryValue"><Font ss:FontName="Calibri" ss:Color="#0F172A"/><Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="Header"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#0F172A" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="TextCell"><Alignment ss:Vertical="Top" ss:WrapText="1"/></Style>""")
        appendLine("""<Style ss:ID="CenterCell"><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="MoneyCell"><NumberFormat ss:Format="[${'$'}R${'$'}-416] #,##0.00"/><Alignment ss:Horizontal="Right" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="TotalLabel"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F172A"/><Interior ss:Color="#DBEAFE" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="TotalValue"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F172A"/><Interior ss:Color="#DBEAFE" ss:Pattern="Solid"/><NumberFormat ss:Format="[${'$'}R${'$'}-416] #,##0.00"/><Alignment ss:Horizontal="Right" ss:Vertical="Center"/></Style>""")
        appendLine("""</Styles>""")

        appendLine("""<Worksheet ss:Name="Resumo">""")
        appendLine("""<Table>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Column ss:Width="160"/>""")
        appendLine("""<Column ss:Width="220"/>""")
        appendLine("""<Column ss:Width="80"/>""")
        appendLine("""<Column ss:Width="110"/>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Row ss:Height="26"><Cell ss:MergeAcross="6" ss:StyleID="Title"><Data ss:Type="String">RELATORIO GERAL DE VIAGENS</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="SummaryLabel"><Data ss:Type="String">Gerado em</Data></Cell><Cell ss:MergeAcross="6" ss:StyleID="SummaryValue"><Data ss:Type="String">${generatedAt.spreadsheetXmlSafe()}</Data></Cell></Row>""")
        appendLine("""<Row ss:Height="22"><Cell ss:StyleID="Header"><Data ss:Type="String">Viagem</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Local</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Responsavel</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Participantes</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Gastos</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Valor total</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Veiculo usado</Data></Cell></Row>""")

        if (reportTrips.isEmpty()) {
            appendLine("""<Row><Cell ss:MergeAcross="6" ss:StyleID="TextCell"><Data ss:Type="String">Nenhuma viagem cadastrada.</Data></Cell></Row>""")
        } else {
            reportTrips.forEach { trip ->
                val orderedExpenses = buildOrderedTripExpenses(trip.expenses)
                val participants = trip.participantEmails.joinToString(", ").ifBlank { "-" }
                val totalAmount = orderedExpenses.sumOf { it.amount }
                val usedVehicles = orderedExpenses
                    .map { it.vehicleName.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    ?: "-"
                appendLine(
                    """
                    <Row ss:AutoFitHeight="1">
                    <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.name.ifBlank { "Sem nome" }.spreadsheetXmlSafe()}</Data></Cell>
                    <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.location.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                    <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.responsible.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                    <Cell ss:StyleID="TextCell"><Data ss:Type="String">${participants.spreadsheetXmlSafe()}</Data></Cell>
                    <Cell ss:StyleID="CenterCell"><Data ss:Type="Number">${orderedExpenses.size}</Data></Cell>
                    <Cell ss:StyleID="MoneyCell"><Data ss:Type="Number">${totalAmount.toSpreadsheetNumber()}</Data></Cell>
                    <Cell ss:StyleID="TextCell"><Data ss:Type="String">${usedVehicles.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                    </Row>
                    """.trimIndent()
                )
            }
        }

        val grandTotal = reportTrips.sumOf { trip -> trip.expenses.sumOf { it.amount } }
        appendLine("""<Row ss:Height="10"/>""")
        appendLine("""<Row><Cell ss:MergeAcross="4" ss:StyleID="TotalLabel"><Data ss:Type="String">TOTAL GERAL</Data></Cell><Cell ss:StyleID="TotalValue"><Data ss:Type="Number">${grandTotal.toSpreadsheetNumber()}</Data></Cell><Cell ss:StyleID="TotalLabel"><Data ss:Type="String">${reportTrips.size} viagens</Data></Cell></Row>""")
        appendLine("""</Table>""")
        appendLine("""</Worksheet>""")

        appendLine("""<Worksheet ss:Name="Gastos">""")
        appendLine("""<Table>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Column ss:Width="160"/>""")
        appendLine("""<Column ss:Width="220"/>""")
        appendLine("""<Column ss:Width="120"/>""")
        appendLine("""<Column ss:Width="320"/>""")
        appendLine("""<Column ss:Width="100"/>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Row ss:Height="26"><Cell ss:MergeAcross="7" ss:StyleID="Title"><Data ss:Type="String">GASTOS POR VIAGEM</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="SummaryLabel"><Data ss:Type="String">Gerado em</Data></Cell><Cell ss:MergeAcross="7" ss:StyleID="SummaryValue"><Data ss:Type="String">${generatedAt.spreadsheetXmlSafe()}</Data></Cell></Row>""")
        appendLine("""<Row ss:Height="22"><Cell ss:StyleID="Header"><Data ss:Type="String">Viagem</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Local</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Responsavel</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Participantes</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Categoria</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Item da nota</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Valor da nota</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Veiculo</Data></Cell></Row>""")

        if (reportTrips.isEmpty()) {
            appendLine("""<Row><Cell ss:MergeAcross="7" ss:StyleID="TextCell"><Data ss:Type="String">Nenhuma viagem cadastrada.</Data></Cell></Row>""")
        } else {
            reportTrips.forEach { trip ->
                val participants = trip.participantEmails.joinToString(", ").ifBlank { "-" }
                val orderedExpenses = buildOrderedTripExpenses(trip.expenses)
                if (orderedExpenses.isEmpty()) {
                    appendLine(
                        """
                        <Row ss:AutoFitHeight="1">
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.name.ifBlank { "Sem nome" }.spreadsheetXmlSafe()}</Data></Cell>
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.location.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.responsible.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">${participants.spreadsheetXmlSafe()}</Data></Cell>
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">-</Data></Cell>
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">Sem gastos cadastrados</Data></Cell>
                        <Cell ss:StyleID="MoneyCell"><Data ss:Type="Number">0</Data></Cell>
                        <Cell ss:StyleID="TextCell"><Data ss:Type="String">-</Data></Cell>
                        </Row>
                        """.trimIndent()
                    )
                } else {
                    orderedExpenses.forEach { expense ->
                        buildExpenseSpreadsheetRows(expense).forEach { item ->
                            appendLine(
                                """
                                <Row ss:AutoFitHeight="1">
                                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.name.ifBlank { "Sem nome" }.spreadsheetXmlSafe()}</Data></Cell>
                                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.location.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${trip.responsible.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${participants.spreadsheetXmlSafe()}</Data></Cell>
                                <Cell ss:StyleID="CenterCell"><Data ss:Type="String">${expense.category.spreadsheetXmlSafe()}</Data></Cell>
                                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${item.first.spreadsheetXmlSafe()}</Data></Cell>
                                <Cell ss:StyleID="MoneyCell"><Data ss:Type="Number">${item.second.toSpreadsheetNumber()}</Data></Cell>
                                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${expense.vehicleName.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                                </Row>
                                """.trimIndent()
                            )
                        }
                    }
                }
            }
        }

        appendLine("""</Table>""")
        appendLine("""</Worksheet>""")
        appendLine("""</Workbook>""")
    }
    spreadsheetFile.writeText(spreadsheetContent, Charsets.UTF_8)
    spreadsheetFile
}.getOrNull()

private fun generateAllTripsNotesPackage(context: Context, trips: List<TravelTrip>): File? = runCatching {
    val reportTrips = trips.filter { it.name.isNotBlank() || it.expenses.isNotEmpty() }
    val zipFile = File(context.cacheDir, "fotos_notas_viagens.zip")

    ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
        reportTrips.forEach { trip ->
            val tripFolder = trip.name.tripFileSlug()
            buildOrderedTripExpenses(trip.expenses).forEachIndexed { index, expense ->
                val photoUri = expense.notePhotoUri ?: return@forEachIndexed
                val input = runCatching { context.contentResolver.openInputStream(photoUri) }.getOrNull() ?: return@forEachIndexed
                input.use { stream ->
                val extension = photoUri.lastPathSegment
                    ?.substringAfterLast('.', "")
                    ?.takeIf { it.isNotBlank() }
                    ?.lowercase(Locale.ROOT)
                    ?: "jpg"
                val photoFileName = "$tripFolder/${buildExpensePhotoFileBaseName(index, expense)}.$extension"
                zip.putNextEntry(ZipEntry(photoFileName))
                stream.copyTo(zip)
                zip.closeEntry()
            }
        }
        }
    }

    zipFile
}.getOrNull()

private fun buildPdfDetailLines(text: String, maxChars: Int): List<String> {
    val normalizedLines = text
        .replace("\r", "\n")
        .replace(" + ", "\n")
        .replace(" | ", "\n")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()

    val wrappedLines = normalizedLines.flatMap { line ->
        if (line.length <= maxChars) {
            listOf(line)
        } else {
            line.chunked(maxChars)
        }
    }

    return if (wrappedLines.isEmpty()) listOf("-") else wrappedLines
}

private fun buildExpenseSpreadsheetRows(expense: TravelExpense): List<Pair<String, Double>> {
    val directRegex = Regex("""(.*?)\s*\((\d+[.,]\d{2})\)""")
    val normalizedLabel = expense.label
        .replace("\r", "\n")
        .replace(" + ", "\n")
        .replace(" | ", "\n")

    val parsedItems = directRegex
        .findAll(normalizedLabel)
        .mapNotNull { match ->
            val description = match.groupValues[1].trim().trim('-', ':', '|')
            if (description.isBlank() || description.startsWith("Total", ignoreCase = true)) {
                return@mapNotNull null
            }
            val amount = match.groupValues[2].replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
            description to amount
        }
        .toList()

    if (parsedItems.isNotEmpty()) {
        return parsedItems
    }

    val fallbackLabel = normalizedLabel
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() && !it.startsWith("Total:", ignoreCase = true) }
        ?.ifBlank { expense.category }
        ?: expense.category
    return listOf(fallbackLabel to expense.amount)
}

private fun drawBitmapFit(
    canvas: Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    maxWidth: Float,
    maxHeight: Float
) {
    val scale = min(maxWidth / bitmap.width.toFloat(), maxHeight / bitmap.height.toFloat())
    val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
    val resized = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    val drawX = left + (maxWidth - targetW) / 2f
    val drawY = top + (maxHeight - targetH) / 2f
    canvas.drawBitmap(resized, drawX, drawY, null)
}

private fun toPortraitBitmap(bitmap: Bitmap): Bitmap {
    if (bitmap.height >= bitmap.width) return bitmap
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun printPdf(context: Context, pdfFile: File, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter = PdfFilePrintAdapter(context, pdfFile, jobName)
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

private fun generateTripReportSpreadsheet(context: Context, tripName: String, tripLocation: String, expenses: List<TravelExpense>): File? = runCatching {
    val sanitizedTripName = tripName.tripFileSlug()
    val spreadsheetFile = File(context.cacheDir, "relatorio_${sanitizedTripName}.xls")
    val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    val totalAmount = expenses.sumOf { it.amount }
    val orderedExpenses = buildOrderedTripExpenses(expenses)
    val spreadsheetContent = buildString {
        appendLine("""<?xml version="1.0"?>""")
        appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"""")
        appendLine(""" xmlns:o="urn:schemas-microsoft-com:office:office"""")
        appendLine(""" xmlns:x="urn:schemas-microsoft-com:office:excel"""")
        appendLine(""" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"""")
        appendLine(""" xmlns:html="http://www.w3.org/TR/REC-html40">""")
        appendLine("""<Styles>""")
        appendLine("""<Style ss:ID="Default"><Alignment ss:Vertical="Center"/><Font ss:FontName="Calibri" ss:Size="11"/><Interior/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/><Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D7DEE8"/></Borders></Style>""")
        appendLine("""<Style ss:ID="Title"><Font ss:FontName="Calibri" ss:Size="14" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#1D4ED8" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="SummaryLabel"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F172A"/><Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="SummaryValue"><Font ss:FontName="Calibri" ss:Color="#0F172A"/><Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="Header"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#0F172A" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="TextCell"><Alignment ss:Vertical="Top" ss:WrapText="1"/></Style>""")
        appendLine("""<Style ss:ID="CenterCell"><Alignment ss:Horizontal="Center" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="MoneyCell"><NumberFormat ss:Format="[${'$'}R${'$'}-416] #,##0.00"/><Alignment ss:Horizontal="Right" ss:Vertical="Center"/></Style>""")
        appendLine("""<Style ss:ID="TotalLabel"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F172A"/><Interior ss:Color="#DBEAFE" ss:Pattern="Solid"/></Style>""")
        appendLine("""<Style ss:ID="TotalValue"><Font ss:FontName="Calibri" ss:Bold="1" ss:Color="#0F172A"/><Interior ss:Color="#DBEAFE" ss:Pattern="Solid"/><NumberFormat ss:Format="[${'$'}R${'$'}-416] #,##0.00"/><Alignment ss:Horizontal="Right" ss:Vertical="Center"/></Style>""")
        appendLine("""</Styles>""")
        appendLine("""<Worksheet ss:Name="Gastos">""")
        appendLine("""<Table>""")
        appendLine("""<Column ss:Width="55"/>""")
        appendLine("""<Column ss:Width="135"/>""")
        appendLine("""<Column ss:Width="340"/>""")
        appendLine("""<Column ss:Width="105"/>""")
        appendLine("""<Column ss:Width="170"/>""")
        appendLine("""<Row ss:Height="26"><Cell ss:MergeAcross="4" ss:StyleID="Title"><Data ss:Type="String">RELATORIO DE GASTOS DA VIAGEM</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="SummaryLabel"><Data ss:Type="String">Viagem</Data></Cell><Cell ss:MergeAcross="3" ss:StyleID="SummaryValue"><Data ss:Type="String">${tripName.spreadsheetXmlSafe()}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="SummaryLabel"><Data ss:Type="String">Local</Data></Cell><Cell ss:MergeAcross="4" ss:StyleID="SummaryValue"><Data ss:Type="String">${tripLocation.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell></Row>""")
        appendLine("""<Row><Cell ss:StyleID="SummaryLabel"><Data ss:Type="String">Gerado em</Data></Cell><Cell ss:MergeAcross="4" ss:StyleID="SummaryValue"><Data ss:Type="String">${generatedAt.spreadsheetXmlSafe()}</Data></Cell></Row>""")
        appendLine("""<Row ss:Height="10"/>""")
        appendLine("""<Row ss:Height="22"><Cell ss:StyleID="Header"><Data ss:Type="String">#</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Categoria</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Descricao</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Valor</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Veiculo</Data></Cell></Row>""")
        orderedExpenses.forEachIndexed { index, expense ->
            appendLine(
                """
                <Row ss:AutoFitHeight="1">
                <Cell ss:StyleID="CenterCell"><Data ss:Type="Number">${index + 1}</Data></Cell>
                <Cell ss:StyleID="CenterCell"><Data ss:Type="String">${expense.category.spreadsheetXmlSafe()}</Data></Cell>
                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${expense.label.spreadsheetXmlSafe()}</Data></Cell>
                <Cell ss:StyleID="MoneyCell"><Data ss:Type="Number">${expense.amount.toSpreadsheetNumber()}</Data></Cell>
                <Cell ss:StyleID="TextCell"><Data ss:Type="String">${expense.vehicleName.ifBlank { "-" }.spreadsheetXmlSafe()}</Data></Cell>
                </Row>
                """.trimIndent()
            )
        }
        appendLine("""<Row ss:Height="10"/>""")
        appendLine("""<Row><Cell ss:MergeAcross="2" ss:StyleID="TotalLabel"><Data ss:Type="String">TOTAL GERAL</Data></Cell><Cell ss:StyleID="TotalValue"><Data ss:Type="Number">${totalAmount.toSpreadsheetNumber()}</Data></Cell><Cell ss:StyleID="TotalLabel"><Data ss:Type="String">${expenses.size} lancamentos</Data></Cell></Row>""")
        appendLine("""</Table>""")
        appendLine("""</Worksheet>""")
        appendLine("""</Workbook>""")
    }
    spreadsheetFile.writeText(spreadsheetContent, Charsets.UTF_8)
    spreadsheetFile
}.getOrNull()

private fun generateTripReportPackage(context: Context, tripName: String, expenses: List<TravelExpense>): File? = runCatching {
    val sanitizedTripName = tripName.tripFileSlug()
    val orderedExpenses = buildOrderedTripExpenses(expenses)
    val zipFile = File(context.cacheDir, "pacote_${sanitizedTripName}.zip")

    ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
        orderedExpenses.forEachIndexed { index, expense ->
            val photoUri = expense.notePhotoUri ?: return@forEachIndexed
            val input = runCatching { context.contentResolver.openInputStream(photoUri) }.getOrNull() ?: return@forEachIndexed
            input.use { stream ->
                val extension = photoUri.lastPathSegment
                    ?.substringAfterLast('.', "")
                    ?.takeIf { it.isNotBlank() }
                    ?.lowercase(Locale.ROOT)
                    ?: "jpg"
                val photoFileName = "fotos/${buildExpensePhotoFileBaseName(index, expense)}.$extension"
                zip.putNextEntry(ZipEntry(photoFileName))
                stream.copyTo(zip)
                zip.closeEntry()
            }
        }
    }

    zipFile
}.getOrNull()

private fun sharePdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
}

private fun shareSpreadsheet(context: Context, spreadsheetFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", spreadsheetFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.ms-excel"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar planilha"))
}

private fun shareSpreadsheetAndPhotos(context: Context, spreadsheetFile: File, zipFile: File) {
    val spreadsheetUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", spreadsheetFile)
    val zipUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(spreadsheetUri, zipUri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar arquivos"))
}

private fun buildOrderedTripExpenses(expenses: List<TravelExpense>): List<TravelExpense> =
    expenses.sortedWith(
        compareBy<TravelExpense> { it.category.lowercase(Locale.ROOT) }
            .thenBy { it.label.lowercase(Locale.ROOT) }
    )

private fun buildExpensePhotoFileBaseName(index: Int, expense: TravelExpense): String {
    val itemSlug = expense.label
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .tripFileSlug()
        .take(24)
        .ifBlank { "item" }
    val amountSlug = String.format(Locale.US, "%.2f", expense.amount).replace('.', '_')
    return "${String.format(Locale.US, "%02d", index + 1)}_${expense.category.tripFileSlug()}_${itemSlug}_r${amountSlug}"
}

private fun String.tripFileSlug(): String =
    trim()
        .ifBlank { "minha_viagem" }
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "minha_viagem" }

private fun String.spreadsheetXmlSafe(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
        .replace("\n", " | ")
        .replace("\r", " ")
        .trim()

private fun Double.toSpreadsheetNumber(): String =
    String.format(Locale.US, "%.2f", this)

private class PdfFilePrintAdapter(
    private val context: Context,
    private val pdfFile: File,
    private val jobName: String
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            pdfFile.inputStream().use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        }
    }
}

