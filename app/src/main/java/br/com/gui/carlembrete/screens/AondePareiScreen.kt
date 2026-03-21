package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AondePareiScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    // --- TEMAS E CORES ---
    val primaryColor = Color(0xFF3B82F6) // Azul Zellu suavizado
    val secondaryColor = Color(0xFF64748B)
    val successColor = Color(0xFF10B981)
    val themedIconTint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black

    // --- ESTADOS ---
    var savedLocation by remember { mutableStateOf(AppPreferences.getParkedLocation(context)) }
    var parkingFinalized by remember { mutableStateOf(AppPreferences.isParkingFinalized(context)) }

    val isParkedState = savedLocation != null && !parkingFinalized

    val photoUris = remember {
        mutableStateListOf<Uri>().apply {
            addAll(AppPreferences.getParkingPhotoUris(context).map { Uri.parse(it) })
        }
    }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var showParkingFinishedDialog by remember { mutableStateOf(false) }
    var showNavigationDialog by remember { mutableStateOf(false) }

    var finishedParkingDurationText by remember { mutableStateOf<String?>(null) }
    var finalizedLocation by remember { mutableStateOf<ParkedLocation?>(null) }
    var finalizedStartedAtMillis by remember { mutableStateOf<Long?>(null) }
    var finalizedEndedAtMillis by remember { mutableStateOf<Long?>(null) }
    var finalizedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // --- VEICULOS ---
    val otherVehicleLabel = "Outro"
    val registeredVehicles = remember {
        BancoDeDados.carregarCarros(context).orEmpty()
    }
    val vehicleDisplayName: (CarroInfo) -> String = { carro ->
        carro.nome.ifBlank { "${carro.marca} ${carro.modelo}".trim().ifBlank { "Veículo sem nome" } }
    }
    val registeredVehicleNames = remember(registeredVehicles) {
        registeredVehicles.map(vehicleDisplayName).filter { it.isNotBlank() }.distinct()
    }
    var showVehicleSelectorDialog by remember { mutableStateOf(false) }
    var selectedVehicleName by remember { mutableStateOf(registeredVehicleNames.firstOrNull().orEmpty()) }
    var customVehicleName by remember { mutableStateOf("") }
    var showVehicleImageDialog by remember { mutableStateOf(false) }
    var previewVehicleType by remember { mutableStateOf(TipoVeiculo.HATCH) }

    // --- LAUNCHERS ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) salvarLocalizacao(context) {
            savedLocation = it
            parkingFinalized = false
            AppPreferences.setParkingFinalized(context, false)
            photoUris.clear()
            AppPreferences.clearParkingPhotoUris(context)
            showParkingOngoingNotification(context, it)
        }
        else Toast.makeText(context, "Permissão necessária para salvar o local.", Toast.LENGTH_SHORT).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingPhotoUri?.let {
                photoUris.add(it)
                AppPreferences.addParkingPhotoUri(context, it.toString())
            }
        }
    }

    LaunchedEffect(savedLocation, parkingFinalized) {
        if (savedLocation != null && !parkingFinalized) {
            showParkingOngoingNotification(context, savedLocation!!)
        } else {
            cancelParkingOngoingNotification(context)
        }
    }

    // --- FUNCOES INTERNAS ---
    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            salvarLocalizacao(context) {
                savedLocation = it
                parkingFinalized = false
                AppPreferences.setParkingFinalized(context, false)
                photoUris.clear()
                AppPreferences.clearParkingPhotoUris(context)
                showParkingOngoingNotification(context, it)
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun openMapsNavigation() {
        val loc = savedLocation ?: return
        val uri = Uri.parse("google.navigation:q=${loc.lat},${loc.lng}&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
        try { context.startActivity(intent) } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${loc.lat},${loc.lng}")))
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, "Voltar")
                }
                Text(
                    text = "Onde parei",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // 1. HEADER STATUS
            StatusHeader(
                isParked = isParkedState,
                location = savedLocation,
                primaryColor = primaryColor
            )

            // 2. AÇÕES PRINCIPAIS
            Column(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
            ) {
                AnimatedContent(
                    targetState = isParkedState,
                    label = "MainActionsTransition",
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { parked ->
                    if (!parked) {
                        BigActionButton(
                            text = "Marcar Local Atual",
                            icon = Icons.Rounded.LocationOn,
                            color = primaryColor,
                            onClick = { requestLocation() }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BigActionButton(
                                text = "Encontrei meu Carro",
                                icon = Icons.Default.CheckCircle,
                                color = successColor,
                                onClick = {
                                    val finishedAt = System.currentTimeMillis()
                                    val currentLocation = savedLocation
                                    val startedAt = currentLocation?.timeMillis

                                    finishedParkingDurationText = if (startedAt != null && finishedAt > startedAt) {
                                        formatElapsedTime(finishedAt - startedAt)
                                    } else null

                                    finalizedLocation = currentLocation
                                    finalizedStartedAtMillis = startedAt
                                    finalizedEndedAtMillis = finishedAt
                                    finalizedPhotoUris = photoUris.toList()
                                    parkingFinalized = true

                                    AppPreferences.setParkingFinalized(context, true)
                                    AppPreferences.clearParkedLocation(context)
                                    cancelParkingOngoingNotification(context)
                                    AppPreferences.clearParkingPhotoUris(context)

                                    photoUris.clear()
                                    pendingPhotoUri = null
                                    selectedPhotoUri = null
                                    savedLocation = null
                                    showParkingFinishedDialog = true
                                }
                            )

                            OutlinedButton(
                                onClick = {
                                    // VERIFICA SE O USUARIO MARCOU "NÃO MOSTRAR NOVAMENTE"
                                    val dontShowAgain = prefs.getBoolean("skip_nav_warning", false)
                                    if (dontShowAgain) {
                                        openMapsNavigation()
                                    } else {
                                        showNavigationDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, primaryColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                            ) {
                                Icon(Icons.Rounded.Navigation, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Abrir rota no Maps",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. DETALHES (Veículo e Fotos)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Qual veículo?", style = MaterialTheme.typography.labelLarge, color = secondaryColor)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVehicleSelectorDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedVehicleMatch = registeredVehicles.firstOrNull { vehicleDisplayName(it) == selectedVehicleName }
                            if (selectedVehicleMatch != null) {
                                VehicleIcon(
                                    tipoVeiculo = selectedVehicleMatch.tipoVeiculo,
                                    tint = themedIconTint,
                                    size = 30.dp,
                                    modifier = Modifier.clickable {
                                        previewVehicleType = selectedVehicleMatch.tipoVeiculo
                                        showVehicleImageDialog = true
                                    }
                                )
                            } else {
                                VehicleIcon(
                                    tipoVeiculo = TipoVeiculo.HATCH,
                                    tint = themedIconTint,
                                    size = 30.dp,
                                    modifier = Modifier.clickable {
                                        previewVehicleType = TipoVeiculo.HATCH
                                        showVehicleImageDialog = true
                                    }
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = selectedVehicleName.ifBlank { "Selecionar veículo" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = themedIconTint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = themedIconTint)
                        }
                    }

                    if (selectedVehicleName == otherVehicleLabel) {
                        OutlinedTextField(
                            value = customVehicleName,
                            onValueChange = { customVehicleName = it },
                            label = { Text("Nome personalizado") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fotos de referência", style = MaterialTheme.typography.labelLarge, color = secondaryColor)
                        OutlinedButton(
                            onClick = {
                                val uri = createTempImageUri(context)
                                pendingPhotoUri = uri
                                cameraLauncher.launch(uri)
                            },
                            enabled = isParkedState,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar")
                        }
                    }

                    if (photoUris.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(4.dp))
                                Text("Nenhuma foto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photoUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedPhotoUri = uri }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    // 1. AVISO ANTES DO MAPS (COM CHECKBOX E ALINHAMENTO ESQUERDA)
    if (showNavigationDialog) {
        var dontShowAgain by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showNavigationDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start, // ALINHAMENTO A ESQUERDA
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = primaryColor)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Lembrete Rápido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Quando voltar ao carro, toque em \"Encontrei meu carro\" para registrar o tempo corretamente.",
                        textAlign = TextAlign.Start, // TEXTO ALINHADO A ESQUERDA
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // CHECKBOX "NÃO EXIBIR NOVAMENTE"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Não exibir esse aviso novamente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (dontShowAgain) {
                                prefs.edit().putBoolean("skip_nav_warning", true).apply()
                            }
                            showNavigationDialog = false
                            openMapsNavigation()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Entendi, abrir Maps", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showNavigationDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = secondaryColor)
                    }
                }
            }
        }
    }

    // 2. Zoom Foto
    if (selectedPhotoUri != null) {
        Dialog(onDismissRequest = { selectedPhotoUri = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    OutlinedButton(
                        onClick = { selectedPhotoUri = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }

    // 3. Seletor de Veículo
    if (showVehicleSelectorDialog) {
        Dialog(onDismissRequest = { showVehicleSelectorDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Selecionar veículo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val list = registeredVehicleNames + otherVehicleLabel
                        items(list) { name ->
                            val isSelected = selectedVehicleName == name
                            val vehicleMatch = registeredVehicles.firstOrNull { vehicleDisplayName(it) == name }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
                                border = if (isSelected) {
                                    BorderStroke(1.dp, primaryColor)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVehicleName = name
                                        if (name != otherVehicleLabel) customVehicleName = ""
                                        showVehicleSelectorDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (vehicleMatch != null) {
                                        VehicleIcon(
                                            tipoVeiculo = vehicleMatch.tipoVeiculo,
                                            tint = themedIconTint,
                                            size = 30.dp,
                                            modifier = Modifier.clickable {
                                                previewVehicleType = vehicleMatch.tipoVeiculo
                                                showVehicleImageDialog = true
                                            }
                                        )
                                    } else {
                                        VehicleIcon(
                                            tipoVeiculo = TipoVeiculo.HATCH,
                                            tint = themedIconTint,
                                            size = 30.dp,
                                            modifier = Modifier.clickable {
                                                previewVehicleType = TipoVeiculo.HATCH
                                                showVehicleImageDialog = true
                                            }
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        name,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) primaryColor else themedIconTint
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = themedIconTint, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showVehicleSelectorDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            1.dp,
                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                Color.White.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Cancelar",
                            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                Color.White.copy(alpha = 0.92f)
                            } else {
                                secondaryColor
                            }
                        )
                    }
                }
            }
        }
    }

    if (showVehicleImageDialog) {
        Dialog(onDismissRequest = { showVehicleImageDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VehicleIcon(
                        tipoVeiculo = previewVehicleType,
                        tint = themedIconTint,
                        size = 96.dp
                    )
                    Text(
                        text = previewVehicleType.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { showVehicleImageDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }

    // 4. Estacionamento Finalizado
    if (showParkingFinishedDialog) {
        Dialog(onDismissRequest = { showParkingFinishedDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(successColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = successColor, modifier = Modifier.size(40.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tudo certo!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Estacionamento finalizado.", style = MaterialTheme.typography.bodyMedium, color = secondaryColor, textAlign = TextAlign.Center)
                    }

                    if (finishedParkingDurationText != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tempo Total: ", style = MaterialTheme.typography.bodyMedium)
                                Text(finishedParkingDurationText.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val location = finalizedLocation
                                val startedAt = finalizedStartedAtMillis
                                val endedAt = finalizedEndedAtMillis
                                if (location != null && startedAt != null && endedAt != null) {
                                    val pdf = generateParkingReceiptPdf(
                                        context = context,
                                        location = location,
                                        startedAtMillis = startedAt,
                                        endedAtMillis = endedAt,
                                        durationText = finishedParkingDurationText.orEmpty(),
                                        vehicleName = if(selectedVehicleName == otherVehicleLabel) customVehicleName else selectedVehicleName,
                                        photoUris = finalizedPhotoUris
                                    )
                                    if (pdf != null) shareParkingReceiptPdf(context, pdf)
                                    else Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("PDF", color = primaryColor)
                        }

                        Button(
                            onClick = { showParkingFinishedDialog = false },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- HELPERS E PDF (Mantenha o StatusHeader e funções de PDF iguais) ---
@Composable
fun StatusHeader(isParked: Boolean, location: ParkedLocation?, primaryColor: Color) {
    val bgBrush = if (isParked) {
        Brush.horizontalGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.7f)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF64748B), Color(0xFF94A3B8)))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isParked) Icons.Rounded.LocationOn else Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (isParked) "Estacionado" else "Livre",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    if (isParked && location != null) {
                        val sdf = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
                        Text(
                            "Desde as ${sdf.format(Date(location.timeMillis))}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            "Toque para marcar o local",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BigActionButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color.White)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun salvarLocalizacao(context: Context, onSaved: (ParkedLocation) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    var bestLoc: Location? = null

    for (provider in providers) {
        try {
            val l = locationManager.getLastKnownLocation(provider)
            if (l != null && (bestLoc == null || l.time > bestLoc.time)) {
                bestLoc = l
            }
        } catch (_: SecurityException) {}
    }

    if (bestLoc != null) {
        val loc = ParkedLocation(bestLoc.latitude, bestLoc.longitude, System.currentTimeMillis())
        AppPreferences.setParkedLocation(context, loc.lat, loc.lng, loc.timeMillis)
        onSaved(loc)
        Toast.makeText(context, "Local salvo!", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Ative o GPS e tente novamente.", Toast.LENGTH_LONG).show()
    }
}

private fun createTempImageUri(context: Context): Uri {
    val dir = File(context.filesDir, "parking_photos").apply { mkdirs() }
    val file = File(dir, "parked_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun formatElapsedTime(elapsedMillis: Long): String {
    val totalMinutes = (elapsedMillis / 60000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return String.format("%02dh %02dm", hours, minutes)
}

private const val PARKING_NOTIFICATION_CHANNEL_ID = "parking_ongoing_channel"
private const val PARKING_NOTIFICATION_ID = 90421
private const val PARKING_NOTIFICATION_HISTORY_ID = "PARKING_90421"

private fun showParkingOngoingNotification(context: Context, location: ParkedLocation) {
    if (!hasNotificationPermission(context)) return

    val manager = NotificationManagerCompat.from(context)
    createParkingNotificationChannel(context)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_OPEN_AONDE_PAREI, true)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        90422,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val sinceText = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(location.timeMillis))
    val notification = NotificationCompat.Builder(context, PARKING_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.logonotificacao)
        .setContentTitle("Estacionamento em andamento")
        .setContentText("Toque quando encontrar o carro. Desde: $sinceText")
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "Local marcado com sucesso. Esta notificacao fica ativa ate voce tocar em \"Encontrei meu carro\" no app."
            )
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setContentIntent(pendingIntent)
        .build()

    manager.notify(PARKING_NOTIFICATION_ID, notification)
    NotificacaoHelper.registrarNotificacaoDisparadaUnica(
        context = context,
        id = PARKING_NOTIFICATION_HISTORY_ID,
        titulo = "Estacionamento em andamento",
        descricao = "Local marcado com sucesso. A notificacao permanece ate voce finalizar no app.",
        carroId = null
    )
}

private fun cancelParkingOngoingNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(PARKING_NOTIFICATION_ID)
    NotificacaoHelper.removerNotificacoesPorId(context, PARKING_NOTIFICATION_HISTORY_ID)
}

private fun createParkingNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        PARKING_NOTIFICATION_CHANNEL_ID,
        "Estacionamento",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Lembrete de estacionamento em andamento"
        setShowBadge(false)
    }
    manager.createNotificationChannel(channel)
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

// --- GERAÇÃO DE PDF ---
private fun generateParkingReceiptPdf(
    context: Context,
    location: ParkedLocation,
    startedAtMillis: Long,
    endedAtMillis: Long,
    durationText: String,
    vehicleName: String,
    photoUris: List<Uri>
): File? {
    return runCatching {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)

        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = page.canvas
        val accentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        val dividerPaint = Paint().apply { strokeWidth = 1.2f; color = android.graphics.Color.parseColor("#CBD5E1"); isAntiAlias = true }
        val titlePaint = Paint().apply { textSize = 22f; color = android.graphics.Color.BLACK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true; textAlign = Paint.Align.CENTER }
        val subtitlePaint = Paint().apply { textSize = 12f; color = android.graphics.Color.DKGRAY; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        val labelPaint = Paint().apply { textSize = 10.5f; color = android.graphics.Color.parseColor("#475569"); isAntiAlias = true }
        val valuePaint = Paint().apply { textSize = 12f; color = android.graphics.Color.BLACK; isAntiAlias = true }
        val valueBoldPaint = Paint(valuePaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val cardStrokePaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }

        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentPaint)
        canvas.drawText("COMPROVANTE DE ESTACIONAMENTO", pageWidth / 2f, 48f, titlePaint)
        canvas.drawText("Gerado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())}", pageWidth / 2f, 68f, subtitlePaint)
        canvas.drawLine(margin, 84f, pageWidth - margin, 84f, dividerPaint)

        var y = 106f
        val infoRect = RectF(margin, y, margin + contentWidth, y + 188f)
        canvas.drawRoundRect(infoRect, 12f, 12f, cardBgPaint)
        canvas.drawRoundRect(infoRect, 12f, 12f, cardStrokePaint)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val startedText = sdf.format(Date(startedAtMillis))
        val endedText = sdf.format(Date(endedAtMillis))
        canvas.drawText("RESUMO DO ESTACIONAMENTO", margin + 14f, y + 22f, labelPaint)
        canvas.drawText("Veículo: $vehicleName", margin + 14f, y + 40f, valuePaint)
        canvas.drawText("Início: $startedText", margin + 14f, y + 58f, valuePaint)
        canvas.drawText("Fim: $endedText", margin + 14f, y + 74f, valuePaint)
        canvas.drawText("Tempo total: ${if (durationText.isBlank()) "-" else durationText}", margin + 14f, y + 92f, valueBoldPaint)
        canvas.drawLine(margin + 14f, y + 106f, margin + contentWidth - 14f, y + 106f, dividerPaint)
        val latStr = "%.6f".format(Locale.US, location.lat)
        val lngStr = "%.6f".format(Locale.US, location.lng)
        canvas.drawText("DADOS TÉCNICOS", margin + 14f, y + 126f, labelPaint)
        canvas.drawText("Lat: $latStr", margin + 14f, y + 144f, valuePaint)
        canvas.drawText("Lng: $lngStr", margin + 14f, y + 160f, valuePaint)
        canvas.drawText("Mapa: http://maps.google.com/?q=${location.lat},${location.lng}", margin + 14f, y + 178f, valuePaint)
        y += 226f
        canvas.drawText("FOTOS DE REFERÊNCIA", margin, y, Paint().apply { textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD })
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 12f
        val largePhotos = photoUris.take(2)
        if (largePhotos.isEmpty()) {
            canvas.drawText("Nenhuma foto registrada.", margin, y + 12f, valuePaint)
        } else {
            val colGap = 14f
            val slotWidth = (contentWidth - colGap) / 2f
            val slotHeight = 350f
            largePhotos.forEachIndexed { index, uri ->
                val left = margin + index * (slotWidth + colGap)
                val top = y
                val rect = RectF(left, top, left + slotWidth, top + slotHeight)
                canvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
                canvas.drawRoundRect(rect, 12f, 12f, cardStrokePaint)
                canvas.drawText("Foto ${index + 1}", left + 10f, top + 22f, valueBoldPaint)
                val bitmap = runCatching {
                    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.let(::toPortraitBitmapForPdf)
                }.getOrNull()
                if (bitmap != null) {
                    drawBitmapFitForPdf(canvas, bitmap, left + 8f, top + 30f, slotWidth - 16f, slotHeight - 38f)
                } else {
                    canvas.drawText("Imagem indisponível", left + 10f, top + 50f, valuePaint)
                }
            }
        }
        document.finishPage(page)
        val pdfFile = File(context.cacheDir, "comprovante_estacionamento_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use(document::writeTo)
        document.close()
        pdfFile
    }.getOrNull()
}

private fun shareParkingReceiptPdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar comprovante"))
}

private fun drawBitmapFitForPdf(canvas: Canvas, bitmap: Bitmap, left: Float, top: Float, maxWidth: Float, maxHeight: Float) {
    val scale = min(maxWidth / bitmap.width.toFloat(), maxHeight / bitmap.height.toFloat())
    val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
    val resized = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    canvas.drawBitmap(resized, left + (maxWidth - targetW) / 2f, top + (maxHeight - targetH) / 2f, null)
}

private fun toPortraitBitmapForPdf(bitmap: Bitmap): Bitmap {
    if (bitmap.height >= bitmap.width) return bitmap
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
