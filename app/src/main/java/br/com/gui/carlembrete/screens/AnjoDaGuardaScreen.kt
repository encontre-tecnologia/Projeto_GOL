package br.com.gui.carlembrete







import android.app.KeyguardManager



import android.content.Context



import android.content.Intent



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



import com.google.firebase.firestore.Query



import kotlinx.coroutines.delay



import java.time.Instant



import java.time.ZoneId



import java.time.format.DateTimeFormatter







private const val GUARDIAN_INFO_PREFS = "guardian_info_prefs"
private const val KEY_GUARDIAN_INFO_DISMISSED = "guardian_info_dismissed"

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

@OptIn(ExperimentalMaterial3Api::class)



@Composable



fun AnjoDaGuardaScreen(onDismiss: () -> Unit) {



    // --- Cores do Tema ---



    val bgDark = Color(0xFFF8FAFC)



    val cardBg = Color(0xFFE2E8F0)



    val accentBlue = Color(0xFF3B82F6)



    val accentRed = Color(0xFFEF4444)



    val successGreen = Color(0xFF10B981)



    val textPrimary = Color(0xFF0F172A)



    val textSecondary = Color(0xFF475569)







    val context = LocalContext.current

    // --- Estados Reativos ---



    var isArmed by remember { mutableStateOf(false) }



    var isTriggered by remember { mutableStateOf(false) } // Estado do Alerta Visual







    var notifyRemote by remember { mutableStateOf(true) }



    var alarmLocal by remember { mutableStateOf(true) }



    var alarmRemote by remember { mutableStateOf(true) }



    var isCarMode by remember { mutableStateOf(true) }







    val isLogged = FirebaseAuth.getInstance().currentUser != null



    var alertItems by remember { mutableStateOf<List<String>>(emptyList()) }



    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm") }



    var isAuthenticated by remember { mutableStateOf(false) }



    var authLaunched by remember { mutableStateOf(false) }



    var networkLabel by remember { mutableStateOf("Verificando...") }
    val shouldAutoShowInfo = remember(context) { shouldAutoShowGuardianInfo(context) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var infoOpenedFromHelp by remember { mutableStateOf(false) }







    // --- Lógica Visual Dinâmica ---



    val statusColor = when {



        isTriggered -> accentRed       // 1. Vermelho (Alerta temporário)



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







    // --- TIMER DE RESET RÁPIDO ---



    LaunchedEffect(isTriggered) {



        if (isTriggered) {



            // AGORA SÃO APENAS 5 SEGUNDOS (5000ms)



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



            Toast.makeText(context, "Permissão negada! O sistema não funcionará corretamente.", Toast.LENGTH_LONG).show()



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







    // --- Efeitos e Lógica ---



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



        val uid = FirebaseAuth.getInstance().currentUser?.uid



        if (uid != null) {



            registration = FirebaseFirestore.getInstance()



                .collection("guardian_alerts")



                .document(uid)



                .collection("events")



                .orderBy("timestamp", Query.Direction.DESCENDING)



                .limit(10)



                .addSnapshotListener { snapshot, _ ->



                    val latestDoc = snapshot?.documents?.firstOrNull()



                    val latestType = latestDoc?.getString("type") ?: ""



                    val latestTs = latestDoc?.getTimestamp("timestamp")?.toDate()?.time ?: 0L







                    val now = System.currentTimeMillis()



                    // Verifica se o evento é "novo" (menos de 1 minuto pra garantir)



                    val isRecent = (now - latestTs) < 60_000L







                    if (isArmed && isRecent && (latestType.contains("Movimento", ignoreCase = true) || latestType.contains("Roubo", ignoreCase = true))) {



                        isTriggered = true



                    }







                    val items = snapshot?.documents?.map { doc ->



                        val ts = doc.getTimestamp("timestamp")?.toDate()?.time



                        val type = doc.getString("type") ?: "Alerta"



                        val timeLabel = if (ts != null) Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(formatter) else "--:--"



                        "$timeLabel • $type"



                    } ?: emptyList()



                    alertItems = items



                }



        }



        onDispose { registration?.remove() }



    }







    LaunchedEffect(Unit) {



        while (true) {



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



        containerColor = bgDark,



        topBar = {



            CenterAlignedTopAppBar(



                title = {



                    Row(verticalAlignment = Alignment.CenterVertically) {



                        Icon(Icons.Filled.Shield, contentDescription = null, tint = accentBlue, modifier = Modifier.size(24.dp))



                        Spacer(Modifier.width(8.dp))



                        Text("ZELLO GUARDIÃO", color = textPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)



                    }



                },



                navigationIcon = {



                    IconButton(onClick = onDismiss) {



                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textSecondary)



                    }



                },



                actions = {
                    IconButton(
                        onClick = {
                            dontShowAgain = false
                            infoOpenedFromHelp = true
                            showInfoDialog = true
                        }
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Como funciona", tint = accentBlue)
                    }
                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)



            )



        }



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







                HorizontalDivider(color = cardBg, thickness = 1.dp)







                // 4. Seletor de Modo

                Column(modifier = Modifier.fillMaxWidth()) {

                    Text("Aonde este celular irá ficar?", color = textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(12.dp))

                    if (isArmed) {

                        ModeSelectionCard(

                            title = if (isCarMode) "NO VEÍCULO" else "NO CELULAR",

                            desc = if (isCarMode) "Monitora no carro" else "Recebe os alertas",

                            icon = if (isCarMode) Icons.Filled.DirectionsCar else Icons.Filled.Smartphone,

                            isSelected = true,

                            color = accentBlue,

                            onClick = { },

                            modifier = Modifier.fillMaxWidth()

                        )

                    } else {

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                            ModeSelectionCard(

                                title = "NO VEÍCULO",

                                desc = "Monitora no carro",

                                icon = Icons.Filled.DirectionsCar,

                                isSelected = isCarMode,

                                color = accentBlue,

                                onClick = {

                                    isCarMode = true

                                    if(isArmed) updateService(context, true, notifyRemote, alarmLocal, alarmRemote)

                                },

                                modifier = Modifier.weight(1f)

                            )

                            ModeSelectionCard(

                                title = "NO CELULAR",

                                desc = "Recebe os alertas",

                                icon = Icons.Filled.Smartphone,

                                isSelected = !isCarMode,

                                color = accentBlue,

                                onClick = {

                                    isCarMode = false

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
                        subtitle = "Toca alarme alto no veículo",
                        state = alarmLocal,
                        activeColor = accentRed,
                        modifier = Modifier
                    ) {



                        alarmLocal = it



                        updateService(context, true, notifyRemote, it, alarmRemote)



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



                    colors = CardDefaults.cardColors(containerColor = Color.White),



                    shape = RoundedCornerShape(12.dp),



                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),



                    modifier = Modifier.fillMaxWidth()



                ) {



                    Column(Modifier.padding(16.dp)) {



                        Row(verticalAlignment = Alignment.CenterVertically) {



                            Icon(Icons.Filled.Terminal, null, tint = textSecondary, modifier = Modifier.size(16.dp))



                            Spacer(Modifier.width(8.dp))



                            Text("ATIVIDADE", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)



                        }



                        Spacer(Modifier.height(12.dp))



                        if (alertItems.isEmpty()) {



                            Text("> Sem atividade", color = Color(0xFF64748B), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)



                        } else {



                            alertItems.forEach { item ->



                                val itemColor = if (item.contains("Roubo") || item.contains("Movimento")) accentRed else successGreen



                                Text("> $item", color = itemColor, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)



                                Spacer(Modifier.height(4.dp))



                            }



                        }



                    }



                }







                Spacer(Modifier.height(48.dp))



            }


            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
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
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("1. Selecione o perfil antes de armar.", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text("Perfil No Veículo: fica no veículo, detecta movimento e envia alertas.")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Filled.Smartphone,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text("Perfil No Celular: recebe os alertas no seu celular.")
                            }
                            Text("2. Toque no botão central para ARMAR ou DESARMAR.", fontWeight = FontWeight.Bold)
                            Text("3. Ative Sirene Local (carro) ou Alerta Sonoro (dono), se desejar.")
                            Text("4. A área ATIVIDADE mostra os últimos eventos.", fontWeight = FontWeight.Bold)
                            if (!infoOpenedFromHelp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = dontShowAgain,
                                        onCheckedChange = { checked -> dontShowAgain = checked }
                                    )
                                    Text("Não mostrar novamente")
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



    title: String, desc: String, icon: ImageVector, isSelected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier



) {



    val borderColor by animateColorAsState(if (isSelected) color else Color.Transparent)



    val bgAlpha = if (isSelected) 0.15f else 0.05f







    Card(



        onClick = onClick,



        modifier = modifier,



        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = bgAlpha)),



        border = BorderStroke(1.dp, if (isSelected) color else Color(0xFFE2E8F0)),



        shape = RoundedCornerShape(16.dp)



    ) {



        Column(



            Modifier.padding(16.dp).fillMaxWidth(),



            horizontalAlignment = Alignment.CenterHorizontally



        ) {



            Icon(icon, null, tint = if(isSelected) color else Color(0xFF64748B), modifier = Modifier.size(32.dp))



            Spacer(Modifier.height(8.dp))



            Text(title, color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)



            Text(desc, color = Color(0xFF0F172A), fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)



        }



    }



}







@Composable



fun SettingToggleRow(
    title: String,
    subtitle: String,
    state: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onUpdate: (Boolean) -> Unit
) {



    Row(



        modifier = modifier



            .fillMaxWidth()



            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))



            .background(Color.White, RoundedCornerShape(12.dp))



            .padding(16.dp),



        verticalAlignment = Alignment.CenterVertically,



        horizontalArrangement = Arrangement.SpaceBetween



    ) {



        Column(Modifier.weight(1f)) {



            Text(title, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)



            Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp)



        }



        Switch(



            checked = state,



            onCheckedChange = onUpdate,



            colors = SwitchDefaults.colors(checkedThumbColor = activeColor, checkedTrackColor = activeColor.copy(alpha = 0.3f))



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















