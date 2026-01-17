package br.com.gui.carlembrete

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.app.KeyguardManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnjoDaGuardaScreen(onDismiss: () -> Unit) {
    // Paleta Dark Mode Premium
    val primaryDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
    val textLight = Color(0xFFF1F5F9)
    val textDim = Color(0xFF94A3B8)
    val accent = Color(0xFF3B82F6) // Azul tecnologico
    val accentSoft = Color(0xFF1E3A8A)
    val cardStroke = Color(0xFF273449)
    val danger = Color(0xFFEF4444) // Vermelho alerta

    val context = LocalContext.current

    // Estados
    val isArmed = remember { mutableStateOf(false) }
    val notifyRemote = remember { mutableStateOf(true) }
    val alarmLocal = remember { mutableStateOf(true) }
    val alarmRemote = remember { mutableStateOf(true) }
    val isCarMode = remember { mutableStateOf(true) } // True = Celular no Carro, False = Dono
    val isLogged = FirebaseAuth.getInstance().currentUser != null
    val alertItems = remember { mutableStateOf<List<String>>(emptyList()) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm") }
    val pendingEnableBluetooth = remember { mutableStateOf(false) }
    val isAuthenticated = remember { mutableStateOf(false) }
    val authLaunched = remember { mutableStateOf(false) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && pendingEnableBluetooth.value) {
            pendingEnableBluetooth.value = false
            startGuardianService(
                context = context,
                isCar = isCarMode.value,
                notifyRemote = notifyRemote.value,
                alarmLocal = alarmLocal.value,
                alarmRemote = alarmRemote.value
            )
        }
    }

    // Launcher de Permissoes (Agora focado em GPS e Notificacao)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.values.all { it }
        if (allGranted) {
            ensureBluetoothAndStart(
                context = context,
                pendingEnableBluetooth = pendingEnableBluetooth,
                enableBluetoothLauncher = enableBluetoothLauncher,
                isCar = isCarMode.value,
                notifyRemote = notifyRemote.value,
                alarmLocal = alarmLocal.value,
                alarmRemote = alarmRemote.value
            )
        } else {
            Toast.makeText(context, "Sem permissao de GPS, o rastreamento falhara.", Toast.LENGTH_LONG).show()
            isArmed.value = false
        }
    }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isAuthenticated.value = true
        } else {
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        if (!authLaunched.value) {
            authLaunched.value = true
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguard.isKeyguardSecure) {
                val intent = keyguard.createConfirmDeviceCredentialIntent(
                    "Anjo da Guarda",
                    "Confirme sua senha para acessar"
                )
                if (intent != null) {
                    authLauncher.launch(intent)
                } else {
                    isAuthenticated.value = true
                }
            } else {
                Toast.makeText(context, "Configure uma senha no celular para proteger esta tela", Toast.LENGTH_LONG).show()
                isAuthenticated.value = true
            }
        }
        onDispose { }
    }

    // Listener do Firestore para mostrar o Log de Eventos
    DisposableEffect(isLogged) {
        var registration: ListenerRegistration? = null
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            registration = FirebaseFirestore.getInstance()
                .collection("guardian_alerts")
                .document(uid)
                .collection("events")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { snapshot, _ ->
                    val items = snapshot?.documents?.map { doc ->
                        val ts = doc.getTimestamp("timestamp")?.toDate()?.time
                        val type = doc.getString("type") ?: "Alerta"
                        val timeLabel = if (ts != null) {
                            Instant.ofEpochMilli(ts)
                                .atZone(ZoneId.systemDefault())
                                .format(formatter)
                        } else "--:--"
                        "$timeLabel - $type"
                    } ?: emptyList()
                    alertItems.value = items
                }
        }
        onDispose { registration?.remove() }
    }


    Scaffold(
        containerColor = primaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Anjo da Guarda",
                            color = textLight,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Sistema Anti-Furto via Nuvem",
                            color = textDim,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Shield, contentDescription = "Voltar", tint = textLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryDark)
            )
        }
    ) { innerPadding ->
        if (!isAuthenticated.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Autenticando...", color = textDim)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD PRINCIPAL: Configuracao do Modo
            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = accent)
                        Text("Modo Sentinela 24h", color = textLight, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        StatusPill(
                            text = if (isArmed.value) "ATIVO" else "PAUSADO",
                            background = if (isArmed.value) Color(0xFF0EA5E9).copy(alpha = 0.18f) else Color(0xFF334155),
                            foreground = if (isArmed.value) Color(0xFF38BDF8) else textDim
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Seletor de Perfil (Carro vs Dono)
                    Text(
                        text = "Escolha um perfil:",
                        color = textLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isCarMode.value = true
                                if (isArmed.value) {
                                    ensureBluetoothAndStart(
                                        context = context,
                                        pendingEnableBluetooth = pendingEnableBluetooth,
                                        enableBluetoothLauncher = enableBluetoothLauncher,
                                        isCar = true,
                                        notifyRemote = notifyRemote.value,
                                        alarmLocal = alarmLocal.value,
                                        alarmRemote = alarmRemote.value
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            border = BorderStroke(1.dp, if (isCarMode.value) accent else cardStroke),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCarMode.value) accentSoft else Color.Transparent,
                                contentColor = if (isCarMode.value) textLight else textDim
                            )
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(20.dp), tint = if(isCarMode.value) textLight else textDim)
                            Spacer(Modifier.padding(6.dp))
                            Text(
                                text = "Carro",
                                color = if (isCarMode.value) textLight else textDim,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                isCarMode.value = false
                                if (isArmed.value) {
                                    ensureBluetoothAndStart(
                                        context = context,
                                        pendingEnableBluetooth = pendingEnableBluetooth,
                                        enableBluetoothLauncher = enableBluetoothLauncher,
                                        isCar = false,
                                        notifyRemote = notifyRemote.value,
                                        alarmLocal = alarmLocal.value,
                                        alarmRemote = alarmRemote.value
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            border = BorderStroke(1.dp, if (!isCarMode.value) accent else cardStroke),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isCarMode.value) accentSoft else Color.Transparent,
                                contentColor = if (!isCarMode.value) textLight else textDim
                            )
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp), tint = if(!isCarMode.value) textLight else textDim)
                            Spacer(Modifier.padding(6.dp))
                            Text(
                                text = "Dono",
                                color = if (!isCarMode.value) textLight else textDim,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = cardStroke)

                    // Texto explicativo dinamico
                    Text(
                        text = if (isCarMode.value)
                            "Esconda este celular no carro. Ele envia localizacao e alertas via Internet se detectar movimento."
                        else
                            "Este celular recebe os alertas de roubo onde quer que voce esteja.",
                        color = textDim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Switch Principal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArmed.value) "MONITORANDO" else "Desativado", color = if(isArmed.value) accent else textDim, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isArmed.value,
                            onCheckedChange = { checked ->
                                isArmed.value = checked
                                if (checked) {
                                    val permissions = getRequiredPermissions()
                                    // Verifica se tem permissao faltando
                                    val missing = permissions.filter {
                                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                                    }

                                    if (missing.isNotEmpty()) {
                                        permissionLauncher.launch(missing.toTypedArray())
                                    } else {
                                        startGuardianService(
                                            context = context,
                                            isCar = isCarMode.value,
                                            notifyRemote = notifyRemote.value,
                                            alarmLocal = alarmLocal.value,
                                            alarmRemote = alarmRemote.value
                                        )
                                    }
                                } else {
                                    stopGuardianService(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accent,
                                checkedTrackColor = accent.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }


            // Instrucoes Rapidas
            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Como funciona?", color = textLight, fontWeight = FontWeight.Bold)
                    StepText("1. Esconda o celular velho no carro ligado a um powerbank ou USB.")
                    StepText("2. Ative o 'Modo Sentinela' no celular do carro.")
                    StepText("3. Se o carro mover, voce recebe o alerta onde estiver.")
                }
            }

            // Log de Eventos
            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Historico de Atividade", color = textLight, fontWeight = FontWeight.Bold)
                    if (alertItems.value.isEmpty()) {
                        Text("Nenhum evento registrado hoje.", color = textDim, fontSize = 12.sp)
                    } else {
                        alertItems.value.forEach { item ->
                            Text(item, color = textDim, fontSize = 12.sp)
                        }
                    }
                }
            }


            // Opcoes Avancadas
            if (isCarMode.value) {
                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Configuracao de Disparo", color = textLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Opcao de Sirene
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = danger)
                                Column {
                                    Text("Sirene Local (Max. Vol)", color = textLight)
                                    Text("Toca alarme no carro ao detectar roubo", color = textDim, fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = alarmLocal.value,
                                onCheckedChange = { alarmLocal.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = danger,
                                    checkedTrackColor = danger.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            } else {
                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Alertas do Dono", color = textLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = danger)
                                Column {
                                    Text("Sirene no dono", color = textLight)
                                    Text("Silencie a sirene e deixe so a notificacao", color = textDim, fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = alarmRemote.value,
                                onCheckedChange = {
                                    alarmRemote.value = it
                                    if (isArmed.value && !isCarMode.value) {
                                        startGuardianService(
                                            context = context,
                                            isCar = false,
                                            notifyRemote = notifyRemote.value,
                                            alarmLocal = alarmLocal.value,
                                            alarmRemote = alarmRemote.value
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = danger,
                                    checkedTrackColor = danger.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Voltar", color = textLight, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}


@Composable
fun StepText(text: String) {
    Text(text, color = Color(0xFF94A3B8), fontSize = 12.sp)
}

@Composable
fun StatusPill(text: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Permissoes Focadas em RASTREAMENTO (GPS) e nao mais Bluetooth
private fun getRequiredPermissions(): Array<String> {
    val permissions = mutableListOf<String>()

    // Bluetooth
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        permissions.add(android.Manifest.permission.BLUETOOTH)
        permissions.add(android.Manifest.permission.BLUETOOTH_ADMIN)
    }

    // GPS (alguns dispositivos exigem para scan BLE)
    permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
    permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    // Notificacoes (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    return permissions.toTypedArray()
}


private fun ensureBluetoothAndStart(
    context: Context,
    pendingEnableBluetooth: MutableState<Boolean>,
    enableBluetoothLauncher: ActivityResultLauncher<Intent>,
    isCar: Boolean,
    notifyRemote: Boolean,
    alarmLocal: Boolean,
    alarmRemote: Boolean
) {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    if (adapter == null) {
        Toast.makeText(context, "Bluetooth nao suportado neste dispositivo", Toast.LENGTH_SHORT).show()
        return
    }
    if (!adapter.isEnabled) {
        pendingEnableBluetooth.value = true
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        return
    }
    startGuardianService(context, isCar, notifyRemote, alarmLocal, alarmRemote)
}


private fun startGuardianService(
    context: Context,
    isCar: Boolean,
    notifyRemote: Boolean,
    alarmLocal: Boolean,
    alarmRemote: Boolean
) {
    // Inicia o Servico que agora vai usar Firestore listener ou GPS Updates
    val intent = Intent(context, AnjoDaGuardaService::class.java).apply {
        action = AnjoDaGuardaService.ACTION_START
        putExtra(AnjoDaGuardaService.EXTRA_IS_CAR, isCar)
        putExtra(AnjoDaGuardaService.EXTRA_NOTIFY_REMOTE, notifyRemote)
        putExtra(AnjoDaGuardaService.EXTRA_ALARM_LOCAL, alarmLocal)
        putExtra(AnjoDaGuardaService.EXTRA_ALARM_REMOTE, alarmRemote)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }
}

private fun stopGuardianService(context: Context) {
    Toast.makeText(context, "Alertas do guardiao serao apagados", Toast.LENGTH_SHORT).show()
    val intent = Intent(context, AnjoDaGuardaService::class.java).apply {
        action = AnjoDaGuardaService.ACTION_STOP
    }
    context.startService(intent)
}
