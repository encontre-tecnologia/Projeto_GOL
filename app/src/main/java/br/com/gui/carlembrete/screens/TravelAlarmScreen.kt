package br.com.gui.carlembrete

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private data class TravelDestination(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelAlarmScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBackground = if (isDark) Color.Black else MaterialTheme.colorScheme.background
    var persistedState by remember { mutableStateOf(TravelAlarmStore.load(context)) }
    var query by remember { mutableStateOf(persistedState.destinationName) }
    var selected by remember {
        mutableStateOf(
            persistedState.destinationName.takeIf { it.isNotBlank() }?.let {
                TravelDestination(it, "Destino salvo", persistedState.latitude, persistedState.longitude)
            }
        )
    }
    val radii = listOf(3_000, 5_000, 10_000, 20_000)
    var radiusMeters by remember {
        mutableStateOf(persistedState.radiusMeters.takeIf { it in radii } ?: 5_000)
    }
    var results by remember { mutableStateOf<List<TravelDestination>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchRequest by remember { mutableStateOf(0) }
    var showAlertSettings by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(TravelAlarmStore.isSoundEnabled(context)) }

    fun beginTracking() {
        val destination = selected ?: return
        TravelAlarmStore.saveDestination(context, destination.name, destination.latitude, destination.longitude, radiusMeters)
        TravelAlarmStore.setActive(context, active = true)
        TravelAlarmService.start(context)
        persistedState = TravelAlarmStore.load(context)
        Toast.makeText(context, "Despertador de viagem ativado!", Toast.LENGTH_SHORT).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            grants[Manifest.permission.POST_NOTIFICATIONS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (locationGranted && notificationGranted) beginTracking()
        else Toast.makeText(context, "Localização e notificações são necessárias para avisar na hora certa.", Toast.LENGTH_LONG).show()
    }

    fun requestStart() {
        if (selected == null) {
            Toast.makeText(context, "Escolha um destino primeiro.", Toast.LENGTH_SHORT).show()
            return
        }
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (hasLocation && hasNotifications) beginTracking()
        else permissionLauncher.launch(buildList {
            if (!hasLocation) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray())
    }

    LaunchedEffect(searchRequest) {
        if (searchRequest == 0 || query.trim().length < 3) return@LaunchedEffect
        searching = true
        results = withContext(Dispatchers.IO) { searchDestinations(context, query.trim()) }
        searching = false
        if (results.isEmpty()) Toast.makeText(context, "Não encontrei esse lugar. Tente incluir cidade e estado.", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(persistedState.active, persistedState.arrived) {
        while (persistedState.active || persistedState.arrived) {
            delay(2_000L)
            persistedState = TravelAlarmStore.load(context)
        }
    }

    BackHandler(onBack = onDismiss)
    if (showAlertSettings) {
        AlertDialog(
            onDismissRequest = { showAlertSettings = false },
            title = { Text("Como você quer ser avisado?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertModeRow(
                        icon = Icons.Rounded.VolumeUp,
                        title = "Som e vibração",
                        subtitle = "Toca o alarme e vibra até você confirmar.",
                        selected = soundEnabled,
                        onClick = { soundEnabled = true }
                    )
                    AlertModeRow(
                        icon = Icons.Rounded.Vibration,
                        title = "Somente vibração",
                        subtitle = "Não emite som ao chegar ao destino.",
                        selected = !soundEnabled,
                        onClick = { soundEnabled = false }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    TravelAlarmStore.setSoundEnabled(context, soundEnabled)
                    showAlertSettings = false
                    Toast.makeText(context, "Preferência de alerta salva.", Toast.LENGTH_SHORT).show()
                }) { Text("Salvar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    soundEnabled = TravelAlarmStore.isSoundEnabled(context)
                    showAlertSettings = false
                }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    Scaffold(
        containerColor = screenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp)) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Voltar", modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {
                            soundEnabled = TravelAlarmStore.isSoundEnabled(context)
                            showAlertSettings = true
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Rounded.Settings, "Configurar alerta", modifier = Modifier.size(25.dp))
                    }
                }
            }
            if (persistedState.active || persistedState.arrived) {
                item {
                    ActiveTravelCard(
                        state = persistedState,
                        onStop = {
                            TravelAlarmService.stop(context)
                            TravelAlarmStore.clear(context)
                            selected = null
                            query = ""
                            results = emptyList()
                            radiusMeters = 5_000
                            persistedState = TravelAlarmStore.load(context)
                        }
                    )
                }
            }

            if (!persistedState.active && !persistedState.arrived) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.DirectionsBus,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Qual é a próxima parada?",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 27.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Escolha uma cidade ou endereço. O Zellu te acorda na aproximação.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; selected = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cidade, bairro ou endereço") },
                        placeholder = { Text("Ex.: Av. Paulista, 1000") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            IconButton(onClick = { searchRequest++ }) { Icon(Icons.Rounded.Search, "Buscar") }
                        },
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.moveFocus(FocusDirection.Down)
                            searchRequest++
                        })
                    )
                    if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }

                items(results) { destination ->
                    DestinationRow(destination, selected == destination) {
                        selected = destination
                        query = destination.name
                        results = emptyList()
                    }
                }

                selected?.let { destination ->
                    item {
                        SelectedJourneyCard(destination) { selected = null; query = "" }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                                }
                                Spacer(Modifier.width(11.dp))
                                Column {
                                    Text("Distância do alerta", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                    Text("Quando o aviso deve tocar?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                radii.forEach { radius ->
                                    RadiusOption(
                                        label = if (radius < 1_000) "${radius} m" else "${radius / 1_000} km",
                                        selected = radiusMeters == radius,
                                        onClick = { radiusMeters = radius },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Text(
                                "Para viagens entre cidades. O cálculo usa a distância em linha reta até o destino.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = ::requestStart,
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.DirectionsBus, null)
                        Spacer(Modifier.width(9.dp))
                        Text("Começar monitoramento", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    Text(
                        "Você verá uma notificação enquanto a viagem estiver ativa.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AlertModeRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                RoundedCornerShape(17.dp)
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun DestinationRow(destination: TravelDestination, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(destination.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(destination.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectedJourneyCard(destination: TravelDestination, onClear: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SEU TRAJETO",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClear, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, "Trocar destino", modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                RouteRail()
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sua localização", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("Ponto de partida", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(33.dp))
                    Text("Destino", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(destination.name, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (destination.detail.isNotBlank() && destination.detail != destination.name) {
                        Text(destination.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteRail() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(13.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
        repeat(3) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(50)))
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        repeat(3) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(50)))
        }
        Spacer(Modifier.height(5.dp))
        Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RadiusOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(44.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                RoundedCornerShape(17.dp)
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActiveTravelCard(state: TravelAlarmState, onStop: () -> Unit) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(if (state.arrived) Color(0xFFEF4444) else Color(0xFF22C55E), RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.arrived) "VOCÊ ESTÁ CHEGANDO" else "MONITORAMENTO ATIVO",
                    color = if (state.arrived) Color(0xFFEF4444) else Color(0xFF22C55E),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(21.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Hotel,
                    contentDescription = "Descansando durante a viagem",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                if (state.arrived) "Hora de acordar!" else "Bons sonhos!",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Box(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = .55f), RoundedCornerShape(19.dp)).padding(16.dp)
            ) {
                Column(Modifier.padding(end = 48.dp)) {
                    Text(
                        if (state.arrived) "DESTINO DO ALERTA" else "PRÓXIMA PARADA",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(state.destinationName, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFEF4444), modifier = Modifier.align(Alignment.CenterEnd).size(34.dp))
            }
            Box(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = .55f), RoundedCornerShape(19.dp)).padding(16.dp)
            ) {
                Column {
                    Text(if (state.arrived) "DISTÂNCIA AO DISPARAR" else "DISTÂNCIA ATUAL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        state.lastDistanceMeters?.let(::formatTravelDistance) ?: "Localizando…",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    )
                    Text("Aviso ao entrar no raio de ${state.radiusMeters / 1_000} km", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Icon(Icons.Rounded.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterEnd).size(38.dp))
            }
            if (state.arrived) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArrivalStat(
                        label = "TEMPO MONITORADO",
                        value = formatMonitoringDuration(state),
                        modifier = Modifier.weight(1f)
                    )
                    ArrivalStat(
                        label = "RAIO ESCOLHIDO",
                        value = formatTravelRadius(state.radiusMeters),
                        modifier = Modifier.weight(1f)
                    )
                }
                state.initialDistanceMeters?.let { initialDistance ->
                    Text(
                        "Monitoramento iniciado a ${formatTravelDistance(initialDistance)} do destino.",
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(if (state.arrived) "Concluir viagem" else "Cancelar monitoramento")
            }
        }
    }
}

@Composable
private fun ArrivalStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = .55f), RoundedCornerShape(17.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

private fun formatMonitoringDuration(state: TravelAlarmState): String {
    if (state.startedAtMillis <= 0L) return "--"
    val end = state.arrivedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
    val minutes = ((end - state.startedAtMillis).coerceAtLeast(0L) / 60_000L).coerceAtLeast(1L)
    return if (minutes < 60L) "${minutes} min" else "${minutes / 60}h ${minutes % 60}min"
}

private fun formatTravelRadius(radiusMeters: Int): String = if (radiusMeters < 1_000) {
    "$radiusMeters m"
} else {
    "${radiusMeters / 1_000} km"
}

@Suppress("DEPRECATION")
private fun searchDestinations(context: Context, query: String): List<TravelDestination> = runCatching {
    if (!Geocoder.isPresent()) return emptyList()
    val geocoder = Geocoder(context, Locale.getDefault())
    geocoder.getFromLocationName(query, 6).orEmpty().mapNotNull { address -> address.toTravelDestination() }.distinctBy { "${it.latitude},${it.longitude}" }
}.getOrDefault(emptyList())

private fun Address.toTravelDestination(): TravelDestination? {
    if (!hasLatitude() || !hasLongitude()) return null
    val complete = (0..maxAddressLineIndex).mapNotNull(::getAddressLine).joinToString(", ")
    val title = featureName?.takeIf { it.isNotBlank() }
        ?: locality?.takeIf { it.isNotBlank() }
        ?: subAdminArea?.takeIf { it.isNotBlank() }
        ?: adminArea?.takeIf { it.isNotBlank() }
        ?: complete
    return TravelDestination(
        title,
        complete.ifBlank { listOfNotNull(locality, adminArea, countryName).joinToString(", ") },
        latitude,
        longitude
    )
}

private fun formatTravelDistance(meters: Float): String = if (meters < 1_000f) {
    "${meters.roundToInt()} m"
} else {
    String.format(Locale.getDefault(), "%.1f km", meters / 1_000f)
}
