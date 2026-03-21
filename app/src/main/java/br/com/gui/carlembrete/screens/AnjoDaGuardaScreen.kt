package br.com.gui.carlembrete







import android.app.KeyguardManager



import android.content.Context



import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager



import android.content.pm.PackageManager



import android.net.ConnectivityManager



import android.net.NetworkCapabilities



import android.os.Build



import android.widget.Toast



import androidx.activity.compose.rememberLauncherForActivityResult



import androidx.activity.result.contract.ActivityResultContracts



import androidx.compose.animation.animateColorAsState



import androidx.compose.animation.core.*



import androidx.compose.foundation.BorderStroke



import androidx.compose.foundation.background
import androidx.compose.foundation.border



import androidx.compose.foundation.layout.*



import androidx.compose.foundation.rememberScrollState



import androidx.compose.foundation.shape.CircleShape



import androidx.compose.foundation.shape.RoundedCornerShape



import androidx.compose.foundation.verticalScroll



import androidx.compose.material.icons.Icons



import androidx.compose.material.icons.filled.*



import androidx.compose.material3.*



import androidx.compose.runtime.*



import androidx.compose.ui.Alignment



import androidx.compose.ui.Modifier



import androidx.compose.ui.draw.alpha



import androidx.compose.ui.draw.scale



import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance



import androidx.compose.ui.graphics.vector.ImageVector



import androidx.compose.ui.platform.LocalContext



import androidx.compose.ui.text.font.FontWeight



import androidx.compose.ui.text.style.TextAlign



import androidx.compose.ui.unit.dp



import androidx.compose.ui.unit.sp



import androidx.core.content.ContextCompat



import com.google.firebase.auth.FirebaseAuth



import com.google.firebase.firestore.FirebaseFirestore



import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions



import kotlinx.coroutines.delay



import java.time.Instant



import java.time.ZoneId



import java.time.format.DateTimeFormatter







private const val GUARDIAN_INFO_PREFS = "guardian_info_prefs"
private const val KEY_GUARDIAN_INFO_DISMISSED = "guardian_info_dismissed"
private const val KEY_GUARDIAN_DEVICE_IS_CAR = "guardian_device_is_car"

private fun shouldAutoShowGuardianInfo(context: Context): Boolean {
    val prefs = context.getSharedPreferences(GUARDIAN_INFO_PREFS, Context.MODE_PRIVATE)
    return !prefs.getBoolean(KEY_GUARDIAN_INFO_DISMISSED, false)
}

private fun setGuardianInfoDismissed(context: Context, dismissed: Boolean) {
    context.getSharedPreferences(GUARDIAN_INFO_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_GUARDIAN_INFO_DISMISSED, dismissed)
        .apply()
}

private fun loadGuardianDeviceIsCar(context: Context): Boolean {
    val prefs = context.getSharedPreferences(GUARDIAN_INFO_PREFS, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_GUARDIAN_DEVICE_IS_CAR, false)
}

private fun saveGuardianDeviceIsCar(context: Context, isCar: Boolean) {
    context.getSharedPreferences(GUARDIAN_INFO_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_GUARDIAN_DEVICE_IS_CAR, isCar)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)



@Composable



fun AnjoDaGuardaScreen(onDismiss: () -> Unit) {



    // --- Cores do Tema ---
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val bgDark = colorScheme.background
    val cardBg = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val accentBlue = Color(0xFF3B82F6)
    val accentRed = Color(0xFFEF4444)
    val successGreen = Color(0xFF10B981)
    val textPrimary = colorScheme.onSurface
    val textSecondary = colorScheme.onSurfaceVariant
    val cardSurface = if (isDark) Color(0xFF111827) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)







    val context = LocalContext.current

    // --- Estados Reativos ---



    var isArmed by remember { mutableStateOf(false) }



    var isTriggered by remember { mutableStateOf(false) } // Estado do Alerta Visual







    var notifyRemote by remember { mutableStateOf(true) }



    var alarmLocal by remember { mutableStateOf(true) }



    var alarmRemote by remember { mutableStateOf(true) }



    var isCarMode by remember { mutableStateOf(loadGuardianDeviceIsCar(context)) }







    val isLogged = FirebaseAuth.getInstance().currentUser != null



    var alertItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var batteryStatus by remember { mutableStateOf<String?>(null) }



    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm") }



    var isAuthenticated by remember { mutableStateOf(false) }
    var isVehicleSirenUnlocked by remember { mutableStateOf(false) }



    var authLaunched by remember { mutableStateOf(false) }



    var networkLabel by remember { mutableStateOf("Verificando...") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var carReadyAtMs by remember { mutableLongStateOf(0L) }
    var ownerReadyAtMs by remember { mutableLongStateOf(0L) }
    val shouldAutoShowInfo = remember(context) { shouldAutoShowGuardianInfo(context) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var infoOpenedFromHelp by remember { mutableStateOf(false) }
    val readyWindowMs = 60_000L
    val isCarReady = (nowMs - carReadyAtMs) <= readyWindowMs
    val isOwnerReady = (nowMs - ownerReadyAtMs) <= readyWindowMs
    val canArmGuardian = isCarReady && isOwnerReady







    // --- Logica Visual Dinamica ---



    val statusColor = when {



        isTriggered -> accentRed       // 1. Vermelho (Alerta temporario)



        isArmed -> successGreen        // 2. Verde (Monitorando)



        else -> textSecondary          // 3. Cinza (Desligado)



    }







    val statusIcon = when {



        isTriggered -> Icons.Filled.Warning



        isArmed -> Icons.Filled.Lock



        else -> Icons.Filled.LockOpen



    }







    val statusText = when {



        isTriggered -> "ALERTA!"



        isArmed -> "ARMADO"



        else -> "DESARMADO"



    }







    // --- TIMER DE RESET RAPIDO ---



    LaunchedEffect(isTriggered) {



        if (isTriggered) {



            // AGORA SAO APENAS 5 SEGUNDOS (5000ms)



            delay(5_000L)



            isTriggered = false



        }



    }







    // --- Launchers ---



    val permissionLauncher = rememberLauncherForActivityResult(



        ActivityResultContracts.RequestMultiplePermissions()



    ) { granted ->



        if (granted.values.all { it }) {



            startGuardianService(context, isCarMode, notifyRemote, alarmLocal, alarmRemote)



        } else {



            Toast.makeText(context, "Permissao negada! O sistema nao funcionara corretamente.", Toast.LENGTH_LONG).show()



            isArmed = false



        }



    }







    val authLauncher = rememberLauncherForActivityResult(



        ActivityResultContracts.StartActivityForResult()



    ) { result ->



        if (result.resultCode == android.app.Activity.RESULT_OK) {



            isAuthenticated = true



        } else {



            onDismiss()



        }



    }







    // --- Efeitos e Logica ---



    DisposableEffect(Unit) {



        if (!authLaunched) {



            authLaunched = true



            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager



            if (keyguard.isKeyguardSecure) {



                val intent = keyguard.createConfirmDeviceCredentialIntent("Acesso Seguro", "Confirme para acessar")



                if (intent != null) authLauncher.launch(intent) else isAuthenticated = true



            } else {



                Toast.makeText(context, "Recomendado: Use senha no celular.", Toast.LENGTH_LONG).show()



                isAuthenticated = true



            }



        }



        onDispose { }



    }







    DisposableEffect(isLogged) {
        var registration: ListenerRegistration? = null
        var fallbackRegistration: ListenerRegistration? = null
        var rootPrimaryRegistration: ListenerRegistration? = null
        var rootFallbackRegistration: ListenerRegistration? = null
        var lastRootVersion by mutableLongStateOf(0L)
        var primaryEventVersion by mutableLongStateOf(0L)
        var fallbackEventVersion by mutableLongStateOf(0L)
        var primaryEventItems by mutableStateOf<List<String>>(emptyList())
        var fallbackEventItems by mutableStateOf<List<String>>(emptyList())
        var primaryLatestType by mutableStateOf("")
        var fallbackLatestType by mutableStateOf("")
        var primaryLatestTs by mutableLongStateOf(0L)
        var fallbackLatestTs by mutableLongStateOf(0L)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            fun eventVersion(snapshot: com.google.firebase.firestore.QuerySnapshot?): Long {
                val latest = snapshot?.documents
                    ?.maxByOrNull { doc ->
                        doc.getLong("clientMillis")
                            ?: doc.getTimestamp("timestamp")?.toDate()?.time
                            ?: 0L
                    } ?: return 0L
                return latest.getLong("clientMillis")
                    ?: latest.getTimestamp("timestamp")?.toDate()?.time
                    ?: 0L
            }

            fun recomputeEventUi() {
                val usePrimary = primaryEventVersion >= fallbackEventVersion
                val items = if (usePrimary) primaryEventItems else fallbackEventItems
                val latestType = if (usePrimary) primaryLatestType else fallbackLatestType
                val latestTs = if (usePrimary) primaryLatestTs else fallbackLatestTs
                alertItems = items

                val now = System.currentTimeMillis()
                val isRecent = latestTs > 0L && (now - latestTs) < 60_000L
                if (isArmed && isRecent && (latestType.contains("Movimento", ignoreCase = true) || latestType.contains("Roubo", ignoreCase = true))) {
                    isTriggered = true
                }
            }

            fun applyEventSnapshot(
                snapshot: com.google.firebase.firestore.QuerySnapshot?,
                source: String
            ) {
                val version = eventVersion(snapshot)
                val docs = snapshot?.documents
                    ?.sortedByDescending { doc ->
                        doc.getLong("clientMillis")
                            ?: doc.getTimestamp("timestamp")?.toDate()?.time
                            ?: 0L
                    }
                    ?.take(10)
                    ?: emptyList()
                val latestDoc = docs.firstOrNull()
                val latestType = latestDoc?.getString("type") ?: ""
                val latestTs = latestDoc?.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                val items = docs.map { doc ->
                    val ts = doc.getTimestamp("timestamp")?.toDate()?.time
                    val type = doc.getString("type") ?: "Alerta"
                    val timeLabel = if (ts != null) Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(formatter) else "--:--"
                    "$timeLabel - $type"
                }

                if (source == "primary") {
                    primaryEventVersion = version
                    primaryEventItems = items
                    primaryLatestType = latestType
                    primaryLatestTs = latestTs
                } else {
                    fallbackEventVersion = version
                    fallbackEventItems = items
                    fallbackLatestType = latestType
                    fallbackLatestTs = latestTs
                }
                recomputeEventUi()
            }

            registration = FirebaseFirestore.getInstance()
                .collection("guardian_alerts")
                .document(uid)
                .collection("events")
                .addSnapshotListener { snapshot, _ ->
                    applyEventSnapshot(snapshot, "primary")
                }

            fallbackRegistration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("guardian_alerts")
                .document("main")
                .collection("events")
                .addSnapshotListener { snapshot, _ ->
                    applyEventSnapshot(snapshot, "fallback")
                }

            fun rootVersion(snapshot: com.google.firebase.firestore.DocumentSnapshot?): Long {
                val clientVersion = snapshot?.getLong("updatedAtClient") ?: 0L
                if (clientVersion > 0L) return clientVersion
                return snapshot?.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            }

            fun applyRootSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot?) {
                if (snapshot == null || !snapshot.exists()) return
                val version = rootVersion(snapshot)
                if (version > 0L) {
                    if (version < lastRootVersion) return
                    lastRootVersion = version
                }
                val remoteArmed = snapshot.getBoolean("armed")
                val remoteBattery = snapshot.getLong("batteryPercent")?.toInt()
                val remoteCharging = snapshot.getBoolean("batteryCharging")
                val remoteCarReady = snapshot.getBoolean("carReady") ?: false
                val remoteOwnerReady = snapshot.getBoolean("ownerReady") ?: false
                val remoteCarReadyAt = snapshot.getLong("carReadyAtClient") ?: 0L
                val remoteOwnerReadyAt = snapshot.getLong("ownerReadyAtClient") ?: 0L
                carReadyAtMs = if (remoteCarReady) remoteCarReadyAt else 0L
                ownerReadyAtMs = if (remoteOwnerReady) remoteOwnerReadyAt else 0L
                val remoteCanArm = remoteCarReady &&
                    remoteOwnerReady &&
                    (System.currentTimeMillis() - remoteCarReadyAt) <= readyWindowMs &&
                    (System.currentTimeMillis() - remoteOwnerReadyAt) <= readyWindowMs
                batteryStatus = if (remoteBattery != null && remoteCharging != null) {
                    if (remoteCharging) "$remoteBattery% (carregando)" else "$remoteBattery% (uso)"
                } else {
                    null
                }
                if (remoteArmed != null) {
                    if (remoteArmed && !remoteCanArm) {
                        if (isArmed) {
                            isTriggered = false
                            pauseGuardianService(context, isCarMode, notifyRemote, alarmLocal, alarmRemote)
                        }
                        isArmed = false
                    } else {
                        isArmed = remoteArmed
                    }
                    if (!remoteArmed) {
                        alertItems = emptyList()
                        primaryEventItems = emptyList()
                        fallbackEventItems = emptyList()
                        primaryLatestType = ""
                        fallbackLatestType = ""
                        primaryLatestTs = 0L
                        fallbackLatestTs = 0L
                        primaryEventVersion = 0L
                        fallbackEventVersion = 0L
                    }
                }
            }

            rootPrimaryRegistration = FirebaseFirestore.getInstance()
                .collection("guardian_alerts")
                .document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    applyRootSnapshot(snapshot)
                }

            rootFallbackRegistration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("guardian_alerts")
                .document("main")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    applyRootSnapshot(snapshot)
                }
        }

        onDispose {
            registration?.remove()
            fallbackRegistration?.remove()
            rootPrimaryRegistration?.remove()
            rootFallbackRegistration?.remove()
        }
    }

    DisposableEffect(isAuthenticated, isLogged, isCarMode) {
        if (isAuthenticated && isLogged) {
            publishGuardianReadyState(isCarMode = isCarMode, ready = true)
        }
        onDispose {
            if (isLogged) {
                publishGuardianReadyState(isCarMode = isCarMode, ready = false)
            }
        }
    }

    val sirenAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isVehicleSirenUnlocked = true
        } else {
            Toast.makeText(context, "Desbloqueio da sirene cancelado.", Toast.LENGTH_SHORT).show()
        }
    }







    LaunchedEffect(Unit) {



        while (true) {



            nowMs = System.currentTimeMillis()
            networkLabel = getNetworkLabel(context)



            delay(2000)



        }



    }







    LaunchedEffect(isAuthenticated, shouldAutoShowInfo) {
        if (isAuthenticated && shouldAutoShowInfo) {
            dontShowAgain = false
            infoOpenedFromHelp = false
            showInfoDialog = true
        }
    }

    // --- UI Principal ---



    Scaffold(
        containerColor = bgDark
    ) { innerPadding ->



        if (!isAuthenticated) {



            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {



                CircularProgressIndicator(color = accentBlue)



            }



            return@Scaffold



        }


        Box(modifier = Modifier.fillMaxSize()) {
            Column(



            modifier = Modifier



                .fillMaxSize()



                .padding(innerPadding)



                .verticalScroll(rememberScrollState()),



            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = accentBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "ZELLU GUARDIÃO",
                            color = textPrimary,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            color = accentBlue.copy(alpha = 0.18f),
                            contentColor = accentBlue,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                "BETA",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        dontShowAgain = false
                        infoOpenedFromHelp = true
                        showInfoDialog = true
                    }
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Como funciona", tint = accentBlue)
                }
            }

            Column(



                modifier = Modifier.padding(horizontal = 20.dp),



                horizontalAlignment = Alignment.CenterHorizontally,



                verticalArrangement = Arrangement.spacedBy(24.dp)



            ) {







                // 2. Botão Principal (Radar)



                Spacer(Modifier.height(32.dp))

                Box(contentAlignment = Alignment.Center) {



                    RadarAnimation(isArmed || isTriggered, statusColor)







                    Button(



                        onClick = {



                            val newState = !isArmed
                            if (newState && !canArmGuardian) {
                                Toast.makeText(context, "Abra a tela Guardiao nos dois celulares para ativar.", Toast.LENGTH_LONG).show()
                                return@Button
                            }



                            isArmed = newState







                            if (newState) {



                                val permissions = getRequiredPermissions()



                                val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }







                                if (missing.isNotEmpty()) {



                                    permissionLauncher.launch(missing.toTypedArray())



                                } else {



                                    startGuardianService(context, isCarMode, notifyRemote, alarmLocal, alarmRemote)



                                }



                            } else {



                                isTriggered = false



                                pauseGuardianService(context, isCarMode, notifyRemote, alarmLocal, alarmRemote)



                            }



                        },



                        modifier = Modifier.size(160.dp),
                        enabled = isArmed || canArmGuardian,



                        shape = CircleShape,



                        colors = ButtonDefaults.buttonColors(



                            containerColor = statusColor.copy(alpha = 0.2f),



                        ),



                        border = BorderStroke(2.dp, if (isArmed || isTriggered) statusColor else textSecondary.copy(alpha = 0.3f)),



                        contentPadding = PaddingValues(0.dp)



                    ) {



                        Column(horizontalAlignment = Alignment.CenterHorizontally) {



                            Icon(



                                imageVector = statusIcon,



                                contentDescription = null,



                                modifier = Modifier.size(48.dp),



                                tint = statusColor



                            )



                            Spacer(Modifier.height(8.dp))



                            Text(



                                text = statusText,



                                color = statusColor,



                                fontWeight = FontWeight.Bold,



                                fontSize = 14.sp



                            )



                        }



                    }



                }







                Spacer(Modifier.height(24.dp))







                // 3. Status da Rede



                Row(verticalAlignment = Alignment.CenterVertically) {



                    Icon(Icons.Filled.Wifi, null, tint = if(networkLabel.contains("Sem")) accentRed else successGreen, modifier = Modifier.size(14.dp))



                    Spacer(Modifier.width(6.dp))



                    Text(networkLabel.uppercase(), color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)



                }







                Text(
                    text = if (canArmGuardian) "CARRO E DONO PRONTOS PARA ARMAR" else "AGUARDANDO OS DOIS CELULARES NA TELA",
                    color = if (canArmGuardian) successGreen else accentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = cardBg, thickness = 1.dp)







                // 4. Seletor de Modo

                Column(modifier = Modifier.fillMaxWidth()) {

                    Text("Este aparelho será usado como:", color = textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(12.dp))

                    if (isArmed) {

                        ModeSelectionCard(

                            title = if (isCarMode) "CELULAR DO CARRO" else "CELULAR DO DONO",

                            desc = if (isCarMode) "Detecta movimento e envia alerta" else "Recebe alertas do carro",

                            icon = if (isCarMode) Icons.Filled.DirectionsCar else Icons.Filled.Smartphone,

                            isSelected = true,

                            color = accentBlue,

                            onClick = { },

                            modifier = Modifier.fillMaxWidth()

                        )

                    } else {

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                            ModeSelectionCard(

                                title = "CELULAR DO CARRO",

                                desc = "Detecta movimento e envia alerta",

                                icon = Icons.Filled.DirectionsCar,

                                isSelected = isCarMode,

                                color = accentBlue,

                                onClick = {

                                    isCarMode = true
                                    saveGuardianDeviceIsCar(context, true)

                                    if(isArmed) updateService(context, true, notifyRemote, alarmLocal, alarmRemote)

                                },

                                modifier = Modifier.weight(1f)

                            )

                            ModeSelectionCard(

                                title = "CELULAR DO DONO",

                                desc = "Recebe alertas do carro",

                                icon = Icons.Filled.Smartphone,

                                isSelected = !isCarMode,

                                color = accentBlue,

                                onClick = {

                                    isCarMode = false
                                    saveGuardianDeviceIsCar(context, false)

                                    if(isArmed) updateService(context, false, notifyRemote, alarmLocal, alarmRemote)

                                },

                                modifier = Modifier.weight(1f)

                            )

                        }

                    }

                }



                // 5. Configurações



                if (isCarMode) {



                    SettingToggleRow(
                        title = "Sirene Local",
                        subtitle = if (isVehicleSirenUnlocked) "Toca alarme alto no veículo" else "Bloqueada ate validar senha do dispositivo",
                        state = alarmLocal,
                        activeColor = accentRed,
                        enabled = isVehicleSirenUnlocked,
                        modifier = Modifier
                    ) {



                        alarmLocal = it



                        updateService(context, true, notifyRemote, it, alarmRemote)



                    }

                    if (!isVehicleSirenUnlocked) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                if (keyguard.isKeyguardSecure) {
                                    val intent = keyguard.createConfirmDeviceCredentialIntent(
                                        "Desbloquear Sirene",
                                        "Confirme para alterar a sirene local"
                                    )
                                    if (intent != null) {
                                        sirenAuthLauncher.launch(intent)
                                    } else {
                                        isVehicleSirenUnlocked = true
                                    }
                                } else {
                                    Toast.makeText(context, "Defina senha/padrao no dispositivo para proteger a sirene.", Toast.LENGTH_LONG).show()
                                    isVehicleSirenUnlocked = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Desbloquear sirene")
                        }
                    }



                } else {



                    SettingToggleRow(
                        title = "Alerta Sonoro",
                        subtitle = "Tocar som no seu celular",
                        state = alarmRemote,
                        activeColor = accentRed,
                        modifier = Modifier
                    ) {



                        alarmRemote = it



                        updateService(context, false, notifyRemote, alarmLocal, it)



                    }



                }







                // 6. Logs de Sistema



                Card(



                    colors = CardDefaults.cardColors(containerColor = cardSurface),



                    shape = RoundedCornerShape(12.dp),



                    border = BorderStroke(1.dp, cardBorder),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Column(Modifier.padding(16.dp)) {



                        Row(verticalAlignment = Alignment.CenterVertically) {



                            Icon(Icons.Filled.Terminal, null, tint = textSecondary, modifier = Modifier.size(16.dp))



                            Spacer(Modifier.width(8.dp))



                            Text("ATIVIDADE", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)



                        }



                        Spacer(Modifier.height(12.dp))

                        batteryStatus?.let { status ->
                            Text(
                                "> Status bateria: $status",
                                color = textSecondary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (alertItems.isEmpty()) {



                            Text("> Sem atividade", color = textSecondary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)



                        } else {



                            alertItems.forEach { item ->



                                val itemColor = if (item.contains("Roubo") || item.contains("Movimento")) accentRed else successGreen



                                Text("> $item", color = itemColor, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)



                                Spacer(Modifier.height(4.dp))



                            }



                        }



                    }



                }







                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val diagnostics = buildString {
                        append("Guardiao Beta\n")
                        append("Modo: ").append(if (isCarMode) "carro" else "dono").append('\n')
                        append("Estado: ").append(if (isArmed) "armado" else "desarmado").append('\n')
                        append("Rede: ").append(networkLabel).append('\n')
                        append("Bateria: ").append(batteryStatus ?: "sem dados").append('\n')
                        append("Atividade: ").append(alertItems.firstOrNull() ?: "sem atividade")
                    }
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BugReport, null, tint = textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("DIAGNOSTICO", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Use este resumo para suporte tecnico do beta.",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("guardian_diagnostics", diagnostics))
                                Toast.makeText(context, "Diagnostico copiado.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar diagnostico")
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))



            }


            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    containerColor = cardSurface,
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(38.dp)
                        )
                    },
                    title = {
                        Text(
                            "Como funciona o Zellu Guardião",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = textPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("1. Selecione o perfil antes de armar.", fontWeight = FontWeight.Bold, color = textPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text("Perfil no veículo: detecta movimento e envia alertas.", modifier = Modifier.weight(1f), color = textPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Filled.Smartphone,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text("Perfil no celular: recebe os alertas no seu celular.", modifier = Modifier.weight(1f), color = textPrimary)
                            }
                            Text("2. Toque no botão central para ARMAR ou DESARMAR.", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("3. Ative Sirene Local (carro) ou Alerta Sonoro (dono), se desejar.", color = textPrimary)
                            Text("4. A área ATIVIDADE mostra os últimos eventos.", fontWeight = FontWeight.Bold, color = textPrimary)
                            if (!infoOpenedFromHelp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = dontShowAgain,
                                        onCheckedChange = { checked -> dontShowAgain = checked }
                                    )
                                    Text("Não mostrar novamente", color = textPrimary)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (!infoOpenedFromHelp && dontShowAgain) {
                                setGuardianInfoDismissed(context, true)
                            }
                            showInfoDialog = false
                        }) {
                            Text("Entendi")
                        }
                    }
                )
            }
        }



    }



}

}







// --- Componentes Auxiliares ---







@Composable
fun RadarAnimation(isActive: Boolean, color: Color) {



    if (!isActive) return







    val infiniteTransition = rememberInfiniteTransition()



    val scale by infiniteTransition.animateFloat(



        initialValue = 1f, targetValue = 1.8f,



        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)



    )



    val alpha by infiniteTransition.animateFloat(



        initialValue = 0.5f, targetValue = 0f,



        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)



    )







    Box(



        modifier = Modifier



            .size(160.dp)



            .scale(scale)



            .alpha(alpha)



            .background(color, CircleShape)



    )



}







@Composable
fun ModeSelectionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val borderColor by animateColorAsState(
        if (isSelected) color else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
        label = "mode_selection_border"
    )
    val cardContainer = if (isSelected) {
        color.copy(alpha = if (isDark) 0.25f else 0.12f)
    } else {
        if (isDark) Color(0xFF111827) else color.copy(alpha = 0.04f)
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) color else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(title, color = colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                desc,
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    state: Boolean,
    activeColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onUpdate: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) Color(0xFF111827) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).alpha(if (enabled) 1f else 0.6f)) {
            Text(title, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = state,
            onCheckedChange = if (enabled) onUpdate else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = activeColor.copy(alpha = 0.3f),
                uncheckedThumbColor = colorScheme.outline,
                uncheckedTrackColor = borderColor
            )
        )
    }
}

// --- Funções Lógicas ---







private fun getNetworkLabel(context: Context): String {



    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager



    val network = cm.activeNetwork ?: return "Sem Conexão"



    val caps = cm.getNetworkCapabilities(network) ?: return "Sem Conexão"



    return when {



        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi Conectado"



        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados Móveis"



        else -> "Online"



    }



}







private fun getRequiredPermissions(): Array<String> {



    val permissions = mutableListOf<String>()



    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {



        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)



    }



    return permissions.toTypedArray()



}

private fun publishGuardianReadyState(isCarMode: Boolean, ready: Boolean) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    val now = System.currentTimeMillis()
    val payload = hashMapOf<String, Any>()
    if (isCarMode) {
        payload["carReady"] = ready
        payload["carReadyAtClient"] = if (ready) now else 0L
    } else {
        payload["ownerReady"] = ready
        payload["ownerReadyAtClient"] = if (ready) now else 0L
    }
    payload["updatedAt"] = FieldValue.serverTimestamp()
    payload["updatedAtClient"] = now
    db.collection("guardian_alerts").document(uid).set(payload, SetOptions.merge())
    db.collection("users").document(uid).collection("guardian_alerts").document("main")
        .set(payload, SetOptions.merge())
}







private fun updateService(ctx: Context, isCar: Boolean, notify: Boolean, local: Boolean, remote: Boolean) {

    val intent = Intent(ctx, AnjoDaGuardaService::class.java).apply {

        action = AnjoDaGuardaService.ACTION_UPDATE

        putExtra(AnjoDaGuardaService.EXTRA_IS_CAR, isCar)

        putExtra(AnjoDaGuardaService.EXTRA_NOTIFY_REMOTE, notify)

        putExtra(AnjoDaGuardaService.EXTRA_ALARM_LOCAL, local)

        putExtra(AnjoDaGuardaService.EXTRA_ALARM_REMOTE, remote)

    }

    ctx.startService(intent)

}






private fun startGuardianService(context: Context, isCar: Boolean, notifyRemote: Boolean, alarmLocal: Boolean, alarmRemote: Boolean) {



    val intent = Intent(context, AnjoDaGuardaService::class.java).apply {



        action = AnjoDaGuardaService.ACTION_START



        putExtra(AnjoDaGuardaService.EXTRA_IS_CAR, isCar)



        putExtra(AnjoDaGuardaService.EXTRA_NOTIFY_REMOTE, notifyRemote)



        putExtra(AnjoDaGuardaService.EXTRA_ALARM_LOCAL, alarmLocal)



        putExtra(AnjoDaGuardaService.EXTRA_ALARM_REMOTE, alarmRemote)



        putExtra(AnjoDaGuardaService.EXTRA_ARMED, true)



    }



    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {



        ContextCompat.startForegroundService(context, intent)



    } else {



        context.startService(intent)



    }



}







private fun pauseGuardianService(context: Context, isCar: Boolean, notifyRemote: Boolean, alarmLocal: Boolean, alarmRemote: Boolean) {



    val intent = Intent(context, AnjoDaGuardaService::class.java).apply {



        action = AnjoDaGuardaService.ACTION_PAUSE



        putExtra(AnjoDaGuardaService.EXTRA_IS_CAR, isCar)



        putExtra(AnjoDaGuardaService.EXTRA_ARMED, false)



    }



    context.startService(intent)



}



















