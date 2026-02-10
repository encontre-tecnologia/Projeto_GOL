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
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
            expenses = emptyList()
        )
    }
    if (trips.isEmpty()) {
        trips.add(defaultTrip)
        saveTravelTrips(context, trips)
    }
    var activeTripId by remember(context) { mutableStateOf(trips.firstOrNull()?.id ?: defaultTrip.id) }
    var tripName by remember(context) { mutableStateOf(trips.firstOrNull()?.name ?: defaultTrip.name) }
    var showExpensesScreen by remember { mutableStateOf(false) }
    var showTripsScreen by remember { mutableStateOf(true) }

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
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var isQrLoading by remember { mutableStateOf(false) }
    var isGeneratingGeneralReport by remember { mutableStateOf(false) }
    var generalReportMode by remember { mutableStateOf<String?>(null) } // "pdf" | "print"
    val scope = rememberCoroutineScope()

    fun persistCurrentTrip() {
        val tripToSave = TravelTrip(
            id = activeTripId,
            name = tripName.ifBlank { "Minha viagem" },
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
        showCamera = true
    }

    fun startRegisterNotePhotoFlow() {
        val uri = createTempImageUri(context, "trip_note_register")
        pendingPhotoUri = uri
        registerNotePhotoLauncher.launch(uri)
    }

    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    if (showExpensesScreen) {
        TravelExpensesScreen(
            tripName = tripName,
            onTripNameChange = {
                tripName = it
                persistCurrentTrip()
            },
            expenses = expenses,
            onPersistExpenses = { persistCurrentTrip() },
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
            onExportPdf = {
                val pdf = generateTripReportPdf(context, tripName, expenses)
                if (pdf == null) {
                    Toast.makeText(context, "Nao foi possivel gerar o PDF.", Toast.LENGTH_SHORT).show()
                } else {
                    sharePdf(context, pdf)
                }
            },
            onPrintPdf = {
                val pdf = generateTripReportPdf(context, tripName, expenses)
                if (pdf == null) {
                    Toast.makeText(context, "Nao foi possivel gerar o PDF.", Toast.LENGTH_SHORT).show()
                } else {
                    printPdf(context, pdf, "Relatorio Viagem")
                }
            }
        )
        return
    }

    if (showTripsScreen) {
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
            onOpenTrip = { trip ->
                switchToTrip(trip)
                showExpensesScreen = true
            },
            onCreateTrip = {
                showAddExpenseDialog = true
            },
            onRenameTrip = { tripId, newName ->
                val idx = trips.indexOfFirst { it.id == tripId }
                if (idx >= 0) {
                    val updated = trips[idx].copy(name = newName.ifBlank { "Minha viagem" })
                    trips[idx] = updated
                    if (tripId == activeTripId) {
                        tripName = updated.name
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
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Viagem", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        "Acesse suas viagens cadastradas",
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
                        Text("Ver viagens e gastos", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
                        Text("Lance despesas rapidamente com nota fiscal", color = textDim, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = cardBorder)
                    Button(
                        onClick = { showAddExpenseDialog = true },
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

    if (showAddExpenseDialog) {
        var showCategoryDialog by remember { mutableStateOf(false) }
        var showTripSelectorDialog by remember { mutableStateOf(false) }
        var showVehicleSelectorDialog by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { showAddExpenseDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Adicionar gasto", color = textPrimary, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            if (skipPhotoFlowWarning) {
                                startRegisterNotePhotoFlow()
                            } else {
                                showPhotoFlowWarning = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar Foto da Nota", fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showCategoryDialog = true }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else Color.White),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(category, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textDim)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showTripSelectorDialog = true }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else Color.White),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wallet, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tripName, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textDim)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showVehicleSelectorDialog = true }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else Color.White),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wallet, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    selectedVehicleName.ifBlank { "Selecionar veiculo" },
                                    color = textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textDim)
                            }
                        }
                    }

                    if (selectedVehicleName == otherVehicleLabel) {
                        OutlinedTextField(
                            value = customVehicleName,
                            onValueChange = { customVehicleName = it },
                            label = { Text("Nome do veiculo externo") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
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
                        )
                    }

                    OutlinedTextField(
                        value = expenseLabel,
                        onValueChange = { expenseLabel = it },
                        label = { Text("Produtos") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
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
                    )
                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Valor Total") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
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
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAddExpenseDialog = false },
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.width(152.dp).height(46.dp)
                        ) { Text("Cancelar") }
                        Button(
                            onClick = {
                                if (expenseLabel.trim().isBlank()) {
                                    Toast.makeText(context, "Preencha o campo Produtos.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val amount = parseValorMonetario(expenseAmount)
                                if (amount == null || amount <= 0.0) {
                                    Toast.makeText(context, "Informe um valor valido.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val vehicleNameToSave = when {
                                    selectedVehicleName == otherVehicleLabel -> customVehicleName.trim()
                                    selectedVehicleName.isNotBlank() -> selectedVehicleName
                                    else -> ""
                                }
                                if (selectedVehicleName.isBlank()) {
                                    Toast.makeText(context, "Selecione um veiculo.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedVehicleName == otherVehicleLabel && vehicleNameToSave.isBlank()) {
                                    Toast.makeText(context, "Informe o nome do veiculo externo.", Toast.LENGTH_SHORT).show()
                                    return@Button
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
                                persistCurrentTrip()
                                expenseLabel = ""
                                expenseAmount = ""
                                customVehicleName = ""
                                showAddExpenseDialog = false
                                showExpenseAddedDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                            modifier = Modifier.width(152.dp).height(46.dp)
                        ) {
                            Text("Salvar")
                        }
                    }

                    if (photoUris.isNotEmpty()) {
                        Text("Fotos adicionadas: ${photoUris.size}", color = textDim, fontSize = 12.sp)
                    }
                }
            }
        }

        if (showCategoryDialog) {
            Dialog(onDismissRequest = { showCategoryDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Categoria", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        categories.forEach { item ->
                            val icon = when (item) {
                                "Combustivel" -> Icons.Default.LocalGasStation
                                "Pedagio" -> Icons.Default.Toll
                                else -> Icons.Default.ReceiptLong
                            }
                            OutlinedButton(
                                onClick = {
                                    category = item
                                    showCategoryDialog = false
                                },
                                border = BorderStroke(1.dp, cardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(icon, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                    Text(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showTripSelectorDialog) {
            Dialog(onDismissRequest = { showTripSelectorDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Selecionar viagem", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        trips.forEach { trip ->
                            OutlinedButton(
                                onClick = {
                                    switchToTrip(trip)
                                    showTripSelectorDialog = false
                                },
                                border = BorderStroke(1.dp, cardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Wallet, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(trip.name, modifier = Modifier.weight(1f))
                                    Text("${trip.expenses.size}", color = textDim, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showVehicleSelectorDialog) {
            Dialog(onDismissRequest = { showVehicleSelectorDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.dp, cardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Selecionar veiculo", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        vehicleOptions.forEach { option ->
                            OutlinedButton(
                                onClick = {
                                    selectedVehicleName = option
                                    if (option != otherVehicleLabel) {
                                        customVehicleName = ""
                                    }
                                    showVehicleSelectorDialog = false
                                },
                                border = BorderStroke(1.dp, cardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Wallet, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(option, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCamera) {
        CameraCapturaDialog(
            onDismiss = { showCamera = false },
            onFotoCapturada = { resultado ->
                showCamera = false
                val qrUrl = resultado.qrCodeUrl?.trim()
                if (qrUrl.isNullOrBlank()) {
                    Toast.makeText(context, "QR Code da nota nao detectado.", Toast.LENGTH_LONG).show()
                    return@CameraCapturaDialog
                }

                scope.launch {
                    isQrLoading = true
                    val notaInfo = consultarNotaPorQrCode(qrUrl)
                    isQrLoading = false
                    if (notaInfo == null) {
                        Toast.makeText(context, "QR lido, mas nao foi possivel carregar a nota.", Toast.LENGTH_LONG).show()
                    } else {
                        notaInfo.valorTotal?.let { total ->
                            expenseAmount = "R$ ${formatarValorBr(total)}"
                        }
                        val descricaoItens = notaInfo.descricaoItens?.trim().orEmpty()
                        if (descricaoItens.isNotBlank()) {
                            expenseLabel = formatarProdutosParaDescricao(descricaoItens)
                        }
                        Toast.makeText(context, "Nota lida. Campos preenchidos.", Toast.LENGTH_SHORT).show()
                    }
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
                    Text("Adicionar foto da nota", color = textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Atencao: ")
                            }
                            append("para cada nota, voce fara ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("2 fotos")
                            }
                            append(".\n")
                            append("1) Foto para registro da nota.\n")
                            append("2) Foto no scanner para ler produtos e valores.\n\n")
                            append("O processo e de ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("uma nota por vez")
                            }
                            append(".")
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
                        buildAnnotatedString {
                            append("O gasto foi salvo na viagem selecionada. ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Clique em Abrir viagens")
                            }
                            append(" para ver o gasto.")
                        },
                        color = textDim,
                        fontSize = 13.sp
                    )
                    Text(
                        "Nota cadastrada na viagem de nome: $tripName",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        OutlinedButton(
                            onClick = { showExpenseAddedDialog = false },
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.width(152.dp).height(46.dp)
                        ) {
                            Text("Fechar")
                        }
                        Button(
                            onClick = {
                                showExpenseAddedDialog = false
                                persistCurrentTrip()
                                showTripsScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                            modifier = Modifier.width(152.dp).height(46.dp)
                        ) {
                            Text("Abrir viagens")
                        }
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
    onOpenTrip: (TravelTrip) -> Unit,
    onCreateTrip: () -> Unit,
    onRenameTrip: (tripId: String, newName: String) -> Unit,
    onDeleteTrip: (tripId: String) -> Unit
) {
    val context = LocalContext.current
    var editingTripId by remember { mutableStateOf<String?>(null) }
    var editingTripName by remember { mutableStateOf("") }
    val shouldAutoStartTutorial = remember(context) { shouldAutoStartTripsTutorial(context) }
    var showTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableStateOf(0) }
    var pdfButtonRect by remember { mutableStateOf<Rect?>(null) }
    var printButtonRect by remember { mutableStateOf<Rect?>(null) }
    var addButtonRect by remember { mutableStateOf<Rect?>(null) }
    var firstTripRect by remember { mutableStateOf<Rect?>(null) }
    val tutorialSteps = remember {
        listOf(
            "PDF geral: toque aqui para gerar um arquivo com todas as viagens, gastos e notas." to "pdf",
            "Imprimir geral: toque aqui para abrir a impressão do relatório completo de viagens." to "print",
            "Novo gasto: toque no botão + para abrir o formulário e cadastrar uma despesa na viagem escolhida." to "add",
            "Detalhes da viagem: toque em uma viagem para ver os gastos, editar informações e excluir o que precisar." to "trip"
        )
    }
    LaunchedEffect(shouldAutoStartTutorial) {
        if (shouldAutoStartTutorial) {
            tutorialStep = 0
            showTutorial = true
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Viagens", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { tutorialStep = 0; showTutorial = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Tutorial", tint = accentBlue)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .onGloballyPositioned { pdfButtonRect = it.boundsInRoot() }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onExportAllTripsPdf() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF geral", tint = accentBlue)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .onGloballyPositioned { printButtonRect = it.boundsInRoot() }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPrintAllTripsPdf() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir geral", tint = accentBlue)
                    }
                    IconButton(
                        onClick = onCreateTrip,
                        modifier = Modifier.onGloballyPositioned { addButtonRect = it.boundsInRoot() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nova viagem", tint = accentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (idx == 0) Modifier.onGloballyPositioned { firstTripRect = it.boundsInRoot() } else Modifier)
                            .clickable { onOpenTrip(trip) },
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(trip.name, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(currency.format(total), color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Text("${trip.expenses.size} gastos", color = textDim, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        editingTripId = trip.id
                                        editingTripName = trip.name
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar viagem", tint = accentBlue)
                                }
                                IconButton(onClick = { onDeleteTrip(trip.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir viagem", tint = Color(0xFFDC2626))
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { editingTripId = null }) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val tripId = editingTripId
                                if (tripId != null) {
                                    onRenameTrip(tripId, editingTripName.trim())
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
                Text("Guia rápido", color = textPrimary, fontWeight = FontWeight.Bold)
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
                        Text(if (step < total) "Avançar" else "Finalizar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelExpensesScreen(
    tripName: String,
    onTripNameChange: (String) -> Unit,
    expenses: MutableList<TravelExpense>,
    onPersistExpenses: () -> Unit,
    isDark: Boolean,
    bg: Color,
    textPrimary: Color,
    textDim: Color,
    cardBorder: Color,
    accentBlue: Color,
    currency: NumberFormat,
    categories: List<String>,
    onBack: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val context = LocalContext.current
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var editExpenseLabel by remember { mutableStateOf("") }
    var editExpenseAmount by remember { mutableStateOf("") }
    var editExpenseCategory by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Gastos da viagem", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) { Text("Gerar PDF") }
                OutlinedButton(
                    onClick = onPrintPdf,
                    border = BorderStroke(1.dp, cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    modifier = Modifier.weight(1f)
                ) { Text("Imprimir") }
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
                            Text(productsPreview.preview, color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            if (productsPreview.hiddenCount > 0) {
                                Text("+${productsPreview.hiddenCount} produtos", color = textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (expense.notePhotoUri == null) {
                                            Toast.makeText(context, "Este gasto nao possui foto da nota.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedPhotoUri = expense.notePhotoUri
                                        }
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
                    OutlinedTextField(value = editExpenseLabel, onValueChange = { editExpenseLabel = it }, label = { Text("Produtos") }, modifier = Modifier.fillMaxWidth())
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

private fun generateTripReportPdf(context: Context, tripName: String, expenses: List<TravelExpense>): File? {
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

        val resumoRect = RectF(margin, y, margin + contentWidth, y + 92f)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardBgPaint)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardStrokePaint)
        canvas.drawText("VIAGEM", margin + 14f, y + 22f, labelPaint)
        canvas.drawText(tripName.take(36), margin + 14f, y + 40f, valueBoldPaint)
        canvas.drawText("TOTAL GASTO", margin + 14f, y + 62f, labelPaint)
        canvas.drawText(currency.format(totalGastos), margin + 14f, y + 80f, valuePaint)
        canvas.drawText("LANCAMENTOS", margin + contentWidth / 2f + 10f, y + 22f, labelPaint)
        canvas.drawText(expenses.size.toString(), margin + contentWidth / 2f + 10f, y + 40f, valueBoldPaint)
        canvas.drawText("COM NOTA", margin + contentWidth / 2f + 10f, y + 62f, labelPaint)
        canvas.drawText(expenses.count { it.notePhotoUri != null }.toString(), margin + contentWidth / 2f + 10f, y + 80f, valuePaint)
        y += 120f

        canvas.drawText("GASTOS DETALHADOS", margin, y, sectionPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 14f

        expenses.forEachIndexed { idx, expense ->
            val rowHeight = 132f
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
            buildProductsPreview(expense.label, 5).preview.lineSequence().forEach { line ->
                if (textY > rowTop + rowHeight - 22f) return@forEach
                canvas.drawText(line.take(84), margin + 12f, textY, smallPaint)
                textY += 12f
            }
            y = rowTop + rowHeight + 12f
        }

        val noteEntries = expenses.mapIndexedNotNull { index, expense ->
            expense.notePhotoUri?.let { uri -> (index + 1) to uri }
        }

        if (noteEntries.isNotEmpty()) {
            document.finishPage(page)
            var notesPageIndex = 1
            var notesPage = document.startPage(PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create())
            var notesCanvas = notesPage.canvas

            fun drawNotesHeader(pageNumber: Int) {
                notesCanvas.drawColor(android.graphics.Color.WHITE)
                notesCanvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentPaint)
                val titleCenter = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER }
                val subCenter = Paint(subtitlePaint).apply { textAlign = Paint.Align.CENTER }
                notesCanvas.drawText("FOTOS DAS NOTAS", pageWidth / 2f, 48f, titleCenter)
                notesCanvas.drawText("Pagina $pageNumber", pageWidth / 2f, 68f, subCenter)
                notesCanvas.drawLine(margin, 84f, pageWidth - margin, 84f, dividerPaint)
            }

            drawNotesHeader(notesPageIndex)
            noteEntries.chunked(4).forEachIndexed { chunkIndex, chunk ->
                if (chunkIndex > 0) {
                    document.finishPage(notesPage)
                    notesPageIndex += 1
                    notesPage = document.startPage(PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create())
                    notesCanvas = notesPage.canvas
                    drawNotesHeader(notesPageIndex)
                }

                val colGap = 14f
                val rowGap = 16f
                val startY = 98f
                val slotWidth = (contentWidth - colGap) / 2f
                val slotHeight = 352f
                val imagePadding = 10f
                val imageTopOffset = 30f

                chunk.forEachIndexed { i, (itemIndex, uri) ->
                    val row = i / 2
                    val col = i % 2
                    val left = margin + col * (slotWidth + colGap)
                    val top = startY + row * (slotHeight + rowGap)
                    val rect = RectF(left, top, left + slotWidth, top + slotHeight)
                    notesCanvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
                    notesCanvas.drawRoundRect(rect, 12f, 12f, cardStrokePaint)
                    notesCanvas.drawText("Foto da nota do item $itemIndex", left + 10f, top + 20f, valueBoldPaint)

                    val bitmap = runCatching {
                        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.let(::toPortraitBitmap)
                    }.getOrNull()
                    if (bitmap != null) {
                        drawBitmapFit(
                            canvas = notesCanvas,
                            bitmap = bitmap,
                            left = left + imagePadding,
                            top = top + imageTopOffset,
                            maxWidth = slotWidth - imagePadding * 2,
                            maxHeight = slotHeight - imageTopOffset - imagePadding
                        )
                    } else {
                        notesCanvas.drawText("Imagem indisponivel", left + 10f, top + 52f, smallPaint)
                    }
                }
            }
            page = notesPage
            canvas = notesCanvas
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
        val totalNotes = reportTrips.sumOf { trip -> trip.expenses.count { it.notePhotoUri != null } }

        val resumoRect = RectF(margin, y, margin + contentWidth, y + 98f)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardBgPaint)
        canvas.drawRoundRect(resumoRect, 12f, 12f, cardStrokePaint)
        canvas.drawText("VIAGENS", margin + 14f, y + 22f, labelPaint)
        canvas.drawText(reportTrips.size.toString(), margin + 14f, y + 42f, valueBoldPaint)
        canvas.drawText("LANCAMENTOS", margin + 14f, y + 64f, labelPaint)
        canvas.drawText(totalLaunches.toString(), margin + 14f, y + 84f, valuePaint)
        canvas.drawText("TOTAL GERAL", margin + contentWidth / 2f + 10f, y + 22f, labelPaint)
        canvas.drawText(currency.format(totalExpenses), margin + contentWidth / 2f + 10f, y + 42f, valueBoldPaint)
        canvas.drawText("NOTAS COM FOTO", margin + contentWidth / 2f + 10f, y + 64f, labelPaint)
        canvas.drawText(totalNotes.toString(), margin + contentWidth / 2f + 10f, y + 84f, valuePaint)
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
            val sectionHeight = (70f + (trip.expenses.size * 32f)).coerceAtMost(360f)
            ensureSpace(sectionHeight + 18f)

            val tripRect = RectF(margin, y, margin + contentWidth, y + sectionHeight)
            canvas.drawRoundRect(tripRect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(tripRect, 12f, 12f, cardStrokePaint)

            var textY = y + 22f
            canvas.drawText("${index + 1}. ${trip.name}", margin + 12f, textY, valueBoldPaint)
            val rightPaint = Paint(valueBoldPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(currency.format(tripTotal), margin + contentWidth - 12f, textY, rightPaint)
            textY += 16f
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

        val tripsWithNotes = reportTrips.mapNotNull { trip ->
            val notes = trip.expenses.mapIndexedNotNull { expenseIndex, expense ->
                expense.notePhotoUri?.let { uri -> (expenseIndex + 1) to uri }
            }
            if (notes.isEmpty()) null else trip to notes
        }

        if (tripsWithNotes.isNotEmpty()) {
            nextPage("FOTOS DAS NOTAS", "Agrupadas por viagem")

            tripsWithNotes.forEachIndexed { tripIdx, (trip, notes) ->
                if (tripIdx > 0) {
                    nextPage("FOTOS DAS NOTAS", trip.name)
                } else {
                    canvas.drawText("Viagem: ${trip.name}", margin, y, sectionPaint)
                    y += 10f
                    canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                    y += 12f
                }

                notes.chunked(4).forEachIndexed { chunkIndex, chunk ->
                    if (chunkIndex > 0 || (tripIdx > 0 && chunkIndex == 0)) {
                        if (chunkIndex > 0) {
                            nextPage("FOTOS DAS NOTAS", trip.name)
                        }
                        canvas.drawText("Viagem: ${trip.name}", margin, y, sectionPaint)
                        y += 10f
                        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                        y += 12f
                    }

                    val colGap = 14f
                    val rowGap = 16f
                    val slotWidth = (contentWidth - colGap) / 2f
                    val slotHeight = 340f
                    val imagePadding = 10f
                    val imageTopOffset = 30f
                    val startY = y

                    chunk.forEachIndexed { noteIndex, (itemIndex, uri) ->
                        val row = noteIndex / 2
                        val col = noteIndex % 2
                        val left = margin + col * (slotWidth + colGap)
                        val top = startY + row * (slotHeight + rowGap)
                        val rect = RectF(left, top, left + slotWidth, top + slotHeight)
                        canvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
                        canvas.drawRoundRect(rect, 12f, 12f, cardStrokePaint)
                        canvas.drawText("Nota do item $itemIndex", left + 10f, top + 20f, valueBoldPaint)

                        val bitmap = runCatching {
                            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.let(::toPortraitBitmap)
                        }.getOrNull()
                        if (bitmap != null) {
                            drawBitmapFit(
                                canvas = canvas,
                                bitmap = bitmap,
                                left = left + imagePadding,
                                top = top + imageTopOffset,
                                maxWidth = slotWidth - imagePadding * 2,
                                maxHeight = slotHeight - imageTopOffset - imagePadding
                            )
                        } else {
                            canvas.drawText("Imagem indisponivel", left + 10f, top + 52f, smallPaint)
                        }
                    }
                }
            }
        }

        document.finishPage(page)
        val pdfFile = File(context.cacheDir, "trip_general_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use(document::writeTo)
        document.close()
        pdfFile
    }.getOrNull()
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

private fun sharePdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
}

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
