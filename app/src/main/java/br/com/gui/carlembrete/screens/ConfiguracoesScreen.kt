package br.com.gui.carlembrete

import android.content.Context
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.restartApp() {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    if (launchIntent != null) {
        startActivity(launchIntent)
        findActivity()?.finishAffinity()
    } else {
        findActivity()?.finishAffinity()
    }
}

@Composable
private fun BackupRestartDialog(
    title: String,
    message: String,
    isDark: Boolean,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF2563EB).copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(
                        text = "Ok, entendi a ideia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun ByteArray.toHexColon(): String = joinToString(":") { "%02x".format(it) }

private fun Context.currentSigningSha1(): String? {
    return runCatching {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }
        val signatureBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pkgInfo.signingInfo ?: return@runCatching null
            val signatures = if (info.hasMultipleSigners()) {
                info.apkContentsSigners
            } else {
                info.signingCertificateHistory
            }
            signatures.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures?.firstOrNull()?.toByteArray()
        } ?: return@runCatching null

        MessageDigest.getInstance("SHA1").digest(signatureBytes).toHexColon()
    }.getOrNull()
}

private fun Context.toBackupErrorMessage(action: BackupAction, err: Throwable): String {
    val raw = err.message?.trim().orEmpty()
    val normalized = raw.lowercase(Locale.ROOT)
    val hasCode10 = Regex("""\b(code|status)\s*[:=]?\s*10\b""").containsMatchIn(normalized)
    val seemsGoogleKeyIssue =
        normalized.contains("key error") ||
            normalized.contains("developer_error") ||
            normalized.contains("12500") ||
            hasCode10

    if (seemsGoogleKeyIssue) {
        val sha = currentSigningSha1()
        return if (sha.isNullOrBlank() && raw.isBlank()) {
            "Falha de chave Google (SHA). Atualize o google-services.json com a assinatura da Play Store."
        } else if (sha.isNullOrBlank()) {
            "Falha de chave Google (SHA). Detalhe: $raw"
        } else {
            val detalhe = if (raw.isBlank()) "" else " Detalhe: $raw"
            "Falha de chave Google (SHA). SHA atual desta build: $sha.$detalhe"
        }
    }

    val fallback = if (raw.isBlank()) "-" else raw
    return when (action) {
        BackupAction.BACKUP -> getString(R.string.cfg_backup_send_failed, fallback)
        BackupAction.RESTORE -> getString(R.string.cfg_backup_restore_failed, fallback)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onDismiss: () -> Unit,
    onTestarNotificacao: () -> Unit,
    carros: List<CarroInfo>,
    lembretes: List<Lembrete>,
    contatos: List<ContatoProfissional>,
    planTier: PlanTier,
    subscriptionBillingInfo: SubscriptionBillingInfo,
    onRefreshPlan: () -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBackupDoneDialog by remember { mutableStateOf(false) }
    var showRestoreRestartDialog by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(AppPreferences.getThemeMode(context)) }
    var lastBackupTime by remember { mutableStateOf(0L) }
    var backupInterval by remember { mutableStateOf(BackupInterval.OFF) }
    val driveScope = remember { Scope(DriveScopes.DRIVE_APPDATA) }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(driveScope)
                .build()
        )
    }
    val driveBackupManager = remember { DriveBackupManager(context) }
    var pendingBackupAction by remember { mutableStateOf<BackupAction?>(null) }
    var pendingBackupInterval by remember { mutableStateOf<BackupInterval?>(null) }
    var backupInProgressAction by remember { mutableStateOf<BackupAction?>(null) }
    var restoreProgressStep by remember { mutableStateOf<String?>(null) }

    BackHandler {
        when {
            showBackupDoneDialog -> showBackupDoneDialog = false
            showRestoreRestartDialog -> showRestoreRestartDialog = false
            pendingBackupAction != null -> pendingBackupAction = null
            pendingBackupInterval != null -> pendingBackupInterval = null
            else -> onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        lastBackupTime = getLastBackupTime(context)
        backupInterval = getBackupInterval(context)
        scheduleDriveBackupWork(context, backupInterval)
    }

    fun criarBackup(account: GoogleSignInAccount) {
        scope.launch(Dispatchers.IO) {
            try {
                BancoDeDados.validarDadosParaBackup(context)
                BancoDeDados.salvarCarros(context, carros)
                BancoDeDados.salvarLembretes(context, lembretes)
                BancoDeDados.salvarContatos(context, contatos)
                val abastecimentos = BancoDeDados.carregarAbastecimentos(context)
                val pedaladas = BancoDeDados.carregarPedaladas(context)
                val travelTripsJson = loadTravelTripsBackupJson(context)
                val fleetStockItemsJson = loadFleetStockItemsBackupJson(context)
                val fleetStockMovementsJson = loadFleetStockMovementsBackupJson(context)
                val fuelStartKms = carros.mapNotNull { carro ->
                    val km = AppPreferences.getFuelStartKm(context, carro.id)
                    if (km != null) carro.id to km else null
                }.toMap()
                val payload = BackupPayload(
                    carros = carros,
                    lembretes = lembretes,
                    contatos = contatos,
                    abastecimentos = abastecimentos,
                    pedaladas = pedaladas,
                    travelTripsJson = travelTripsJson,
                    fleetStockItemsJson = fleetStockItemsJson,
                    fleetStockMovementsJson = fleetStockMovementsJson,
                    fuelStartKms = fuelStartKms
                )
                driveBackupManager.uploadBackup(payload, account)
                withContext(Dispatchers.Main) {
                    val now = System.currentTimeMillis()
                    setLastBackupTime(context, now)
                    lastBackupTime = now
                    showBackupDoneDialog = true
                }
            } catch (err: Exception) {
                Log.e("Backup", "Falha ao enviar backup", err)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.toBackupErrorMessage(BackupAction.BACKUP, err),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    backupInProgressAction = null
                }
            }
        }
    }

    fun recuperarBackup(account: GoogleSignInAccount) {
        scope.launch {
            try {
                // Passo 1: Buscando backup local
                restoreProgressStep = "local"
                val localAbastecimentos = withContext(Dispatchers.IO) { BancoDeDados.carregarAbastecimentos(context) }
                val localPedaladas = withContext(Dispatchers.IO) { BancoDeDados.carregarPedaladas(context) }
                val localCount = carros.size + lembretes.size + contatos.size + localAbastecimentos.size + localPedaladas.size
                delay(600)

                // Passo 2: Buscando backup no Drive
                restoreProgressStep = "drive"
                val drivePayload = withContext(Dispatchers.IO) { driveBackupManager.downloadBackup(account) }

                if (drivePayload == null) {
                    restoreProgressStep = null
                    backupInProgressAction = null
                    Toast.makeText(context, context.getString(R.string.cfg_backup_not_found), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                delay(300)

                // Passo 3: Comparando
                restoreProgressStep = "comparing"
                val driveCount = drivePayload.carros.size + drivePayload.lembretes.size + drivePayload.contatos.size + drivePayload.abastecimentos.size + drivePayload.pedaladas.size
                delay(700)

                // Passo 4: Restaurando
                restoreProgressStep = "restoring"
                if (localCount > driveCount) {
                    delay(400)
                    restoreProgressStep = null
                    backupInProgressAction = null
                    Toast.makeText(
                        context,
                        "Seus dados locais estão mais completos (${localCount} vs ${driveCount} registros). Nenhuma alteração foi feita.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    BancoDeDados.salvarCarros(context, drivePayload.carros)
                    BancoDeDados.salvarLembretes(context, drivePayload.lembretes)
                    BancoDeDados.salvarContatos(context, drivePayload.contatos)
                    BancoDeDados.salvarAbastecimentos(context, drivePayload.abastecimentos)
                    BancoDeDados.salvarPedaladas(context, drivePayload.pedaladas)
                    saveTravelTripsBackupJson(context, drivePayload.travelTripsJson)
                    saveFleetStockBackupJson(
                        context,
                        itemsJson = drivePayload.fleetStockItemsJson,
                        movementsJson = drivePayload.fleetStockMovementsJson
                    )
                    drivePayload.fuelStartKms.forEach { (carroId, km) ->
                        AppPreferences.setFuelStartKm(context, carroId, km)
                    }
                    NotificacaoHelper.reagendarExistentes(context.applicationContext, drivePayload.lembretes)
                }
                restoreProgressStep = null
                backupInProgressAction = null
                showRestoreRestartDialog = true
            } catch (err: Exception) {
                Log.e("Backup", "Falha ao obter backup", err)
                restoreProgressStep = null
                Toast.makeText(
                    context,
                    context.toBackupErrorMessage(BackupAction.RESTORE, err),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                restoreProgressStep = null
                backupInProgressAction = null
            }
        }
    }

    val driveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
                Toast.makeText(context, context.getString(R.string.cfg_drive_permission_denied), Toast.LENGTH_SHORT).show()
                pendingBackupAction = null
                pendingBackupInterval = null
                backupInProgressAction = null
                return@rememberLauncherForActivityResult
            }
            when (pendingBackupAction) {
                BackupAction.BACKUP -> criarBackup(account)
                BackupAction.RESTORE -> recuperarBackup(account)
                null -> {}
            }
            pendingBackupInterval?.let { interval ->
                setBackupInterval(context, interval)
                scheduleDriveBackupWork(context, interval)
                backupInterval = interval
                Toast.makeText(context, context.getString(R.string.cfg_backup_auto_enabled), Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            val msg = if (e.statusCode == 10) {
                val sha = context.currentSigningSha1()
                if (sha.isNullOrBlank()) {
                    "Falha ao autenticar com Google (código 10). Verifique SHA no Firebase."
                } else {
                    "Falha ao autenticar com Google (código 10). SHA desta build: $sha"
                }
            } else {
                context.getString(R.string.cfg_google_auth_failed)
            }
            Log.e("Backup", "Google auth failed. status=${e.statusCode}", e)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            backupInProgressAction = null
        } finally {
            pendingBackupAction = null
            pendingBackupInterval = null
        }
    }

    fun executarBackup(action: BackupAction) {
        if (backupInProgressAction != null) return
        backupInProgressAction = action
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
            when (action) {
                BackupAction.BACKUP -> criarBackup(account)
                BackupAction.RESTORE -> recuperarBackup(account)
            }
        } else {
            pendingBackupAction = action
            driveLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    fun alterarIntervaloBackup(interval: BackupInterval) {
        if (interval == BackupInterval.OFF) {
            pendingBackupInterval = null
            setBackupInterval(context, interval)
            scheduleDriveBackupWork(context, interval)
            backupInterval = interval
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
            setBackupInterval(context, interval)
            scheduleDriveBackupWork(context, interval)
            backupInterval = interval
            Toast.makeText(context, context.getString(R.string.cfg_backup_auto_enabled), Toast.LENGTH_SHORT).show()
        } else {
            pendingBackupInterval = interval
            driveLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    if (showBackupDoneDialog) {
        BackupRestartDialog(
            title = "Backup concluido",
            message = "Vamos fechar e abrir o app automaticamente para aplicar o backup com seguranca.",
            isDark = isDark
        ) {
            showBackupDoneDialog = false
            context.restartApp()
        }
    }
    if (showRestoreRestartDialog) {
        BackupRestartDialog(
            title = stringResource(R.string.cfg_restore_done_title),
            message = "Vamos fechar e abrir o app automaticamente para aplicar os dados restaurados.",
            isDark = isDark
        ) {
            showRestoreRestartDialog = false
            context.restartApp()
        }
    }

    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.09f) else Color(0xFF000000).copy(alpha = 0.06f)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (isDark) Color.Black else colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 0.dp)
                    .height(52.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.common_back),
                        tint = colorScheme.onSurface
                    )
                }
                Text(
                    stringResource(R.string.cfg_title),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Aparência ────────────────────────────────────────────────
            SectionHeader(title = "Aparência")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeOptionButton(
                        label = stringResource(R.string.cfg_theme_system),
                        icon = Icons.Default.Contrast,
                        selected = themeMode == AppThemeMode.SYSTEM,
                        onClick = {
                            themeMode = AppThemeMode.SYSTEM
                            AppPreferences.setThemeMode(context, themeMode)
                            onThemeModeChanged(themeMode)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeOptionButton(
                        label = stringResource(R.string.cfg_theme_light),
                        icon = Icons.Default.LightMode,
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick = {
                            themeMode = AppThemeMode.LIGHT
                            AppPreferences.setThemeMode(context, themeMode)
                            onThemeModeChanged(themeMode)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeOptionButton(
                        label = stringResource(R.string.cfg_theme_dark),
                        icon = Icons.Default.DarkMode,
                        selected = themeMode == AppThemeMode.DARK,
                        onClick = {
                            themeMode = AppThemeMode.DARK
                            AppPreferences.setThemeMode(context, themeMode)
                            onThemeModeChanged(themeMode)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Assinatura ───────────────────────────────────────────────
            SectionHeader(title = "Assinatura")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                PlanSubscriptionCard(
                    planTier = planTier,
                    billingInfo = subscriptionBillingInfo,
                    isDark = isDark,
                    onRefresh = {
                        onRefreshPlan()
                        Toast.makeText(context, "Plano atualizado.", Toast.LENGTH_SHORT).show()
                    },
                    onManage = {
                        abrirGerenciamentoAssinaturaGooglePlay(
                            context = context,
                            productId = subscriptionBillingInfo.productId
                        )
                    }
                )
            }

            // ── Backup ───────────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.cfg_section_backup))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    val backupBusy = backupInProgressAction != null
                    Text(
                        text = stringResource(R.string.cfg_backup_auto_title),
                        color = colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.cfg_backup_auto_description),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackupIntervalButton(
                            label = stringResource(R.string.cfg_backup_auto_off),
                            selected = backupInterval == BackupInterval.OFF,
                            onClick = { alterarIntervaloBackup(BackupInterval.OFF) },
                            modifier = Modifier.weight(1f)
                        )
                        BackupIntervalButton(
                            label = stringResource(R.string.cfg_backup_auto_monthly),
                            selected = backupInterval == BackupInterval.MONTHLY,
                            onClick = { alterarIntervaloBackup(BackupInterval.MONTHLY) },
                            modifier = Modifier.weight(1f)
                        )
                        BackupIntervalButton(
                            label = stringResource(R.string.cfg_backup_auto_weekly),
                            selected = backupInterval == BackupInterval.WEEKLY,
                            onClick = { alterarIntervaloBackup(BackupInterval.WEEKLY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    if (backupInProgressAction == BackupAction.BACKUP) {
                        Text(
                            text = stringResource(R.string.cfg_backup_sending),
                            color = colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF2563EB),
                            trackColor = Color(0xFF2563EB).copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(14.dp))
                    } else if (backupInProgressAction == BackupAction.RESTORE) {
                        val stepOrder = listOf("local", "drive", "comparing", "restoring")
                        val stepLabels = mapOf(
                            "local" to "Buscando backup local...",
                            "drive" to "Buscando backup no Google Drive...",
                            "comparing" to "Comparando qual possui mais dados...",
                            "restoring" to "Restaurando..."
                        )
                        val currentIndex = stepOrder.indexOf(restoreProgressStep)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            stepOrder.forEachIndexed { i, step ->
                                val isDone = i < currentIndex
                                val isActive = i == currentIndex
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(22.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isDone -> Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF22C55E),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            isActive -> CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color(0xFF2563EB),
                                                strokeWidth = 2.dp
                                            )
                                            else -> Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .background(Color(0xFF334155), CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = stepLabels[step] ?: step,
                                        color = when {
                                            isDone -> Color(0xFF22C55E)
                                            isActive -> Color.White
                                            else -> Color(0xFF475569)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    Button(
                        onClick = { executarBackup(BackupAction.BACKUP) },
                        enabled = !backupBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.cfg_backup_save),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { executarBackup(BackupAction.RESTORE) },
                        enabled = !backupBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.cfg_backup_restore),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    val lastBackupLabel = if (lastBackupTime > 0L) {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(lastBackupTime)
                    } else {
                        stringResource(R.string.cfg_backup_none)
                    }
                    Text(
                        stringResource(R.string.cfg_backup_last, lastBackupLabel),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            AppVersionFooter(isDark = isDark)
            Spacer(Modifier.height(32.dp))
        }
    }
}

private enum class BackupAction { BACKUP, RESTORE }

@Composable
fun SectionHeader(title: String, isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f) {
    Text(
        text = title.uppercase(),
        color = if (isDark) Color.White else Color(0xFF0F172A),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp, end = 16.dp)
    )
}

@Composable
private fun PlanSubscriptionCard(
    planTier: PlanTier,
    billingInfo: SubscriptionBillingInfo,
    isDark: Boolean,
    onRefresh: () -> Unit,
    onManage: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val planName = planNameLabel(planTier)
    val isAdminPlan = planTier != PlanTier.FREE && billingInfo.planTier != planTier
    val hasGooglePlaySubscription = billingInfo.productId.isNotBlank()
    val billingBelongsToCurrentPlan = hasGooglePlaySubscription && billingInfo.planTier == planTier
    val statusText = statusAssinaturaTexto(planTier, billingInfo, billingBelongsToCurrentPlan)

    val statusColor = when (billingInfo.status) {
        SubscriptionPaymentStatus.CONFIRMED -> if (billingBelongsToCurrentPlan) Color(0xFF10B981) else if (planTier == PlanTier.FREE) subColor else Color(0xFF10B981)
        SubscriptionPaymentStatus.WAITING_CONFIRMATION -> if (billingBelongsToCurrentPlan) Color(0xFFF59E0B) else if (planTier == PlanTier.FREE) subColor else Color(0xFF10B981)
        SubscriptionPaymentStatus.PENDING -> if (billingBelongsToCurrentPlan) Color(0xFFF59E0B) else if (planTier == PlanTier.FREE) subColor else Color(0xFF10B981)
        SubscriptionPaymentStatus.NOT_FOUND -> if (planTier == PlanTier.FREE) subColor else Color(0xFF10B981)
    }

    val planAccentColor = when (planTier) {
        PlanTier.FREE -> subColor
        PlanTier.LITE -> Color(0xFF60A5FA)
        PlanTier.FROTA -> Color(0xFF34D399)
        PlanTier.ENTERPRISE -> Color(0xFFF59E0B)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRefresh() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(planAccentColor.copy(alpha = 0.16f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = planAccentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Plano atual",
                    color = subColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    planName,
                    color = planAccentColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF000000).copy(alpha = 0.06f))

        PlanInfoLine(
            label = "Cobrança",
            value = dataCobrancaTexto(planTier, billingInfo, billingBelongsToCurrentPlan),
            textColor = textColor,
            subColor = subColor
        )
        PlanInfoLine(
            label = "Renovação",
            value = when {
                billingBelongsToCurrentPlan && billingInfo.autoRenewing -> "Automática"
                billingBelongsToCurrentPlan -> "Consulte no Google Play"
                planTier != PlanTier.FREE -> "Plano administrativo"
                else -> "Sem assinatura ativa"
            },
            textColor = textColor,
            subColor = subColor
        )
        PlanInfoLine(
            label = "Origem",
            value = when {
                billingBelongsToCurrentPlan -> "Google Play"
                planTier != PlanTier.FREE -> "Admin Zellu"
                else -> "App Zellu"
            },
            textColor = textColor,
            subColor = subColor
        )
        if (!isAdminPlan && !billingBelongsToCurrentPlan && hasGooglePlaySubscription) {
            PlanInfoLine(
                label = "Google Play",
                value = "${planNameLabel(billingInfo.planTier)} neste aparelho",
                textColor = textColor,
                subColor = subColor
            )
        }

        Button(
            onClick = onManage,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Gerenciar assinatura", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlanInfoLine(
    label: String,
    value: String,
    textColor: Color,
    subColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = subColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun statusAssinaturaTexto(
    planTier: PlanTier,
    billingInfo: SubscriptionBillingInfo,
    billingBelongsToCurrentPlan: Boolean
): String {
    if (!billingBelongsToCurrentPlan) {
        return if (planTier == PlanTier.FREE) "Sem assinatura" else "Ativo"
    }
    return when (billingInfo.status) {
        SubscriptionPaymentStatus.CONFIRMED -> "Pagamento ok"
        SubscriptionPaymentStatus.WAITING_CONFIRMATION -> "Confirmando"
        SubscriptionPaymentStatus.PENDING -> "Pendente"
        SubscriptionPaymentStatus.NOT_FOUND -> if (planTier == PlanTier.FREE) "Sem assinatura" else "Ativo"
    }
}

private fun dataCobrancaTexto(
    planTier: PlanTier,
    billingInfo: SubscriptionBillingInfo,
    billingBelongsToCurrentPlan: Boolean
): String {
    if (!billingBelongsToCurrentPlan) {
        return if (planTier == PlanTier.FREE) "Confira no Google Play" else "Via admin"
    }
    return when {
        billingInfo.nextBillingTimeMillis > 0L -> "Próxima: ${formatarDataCobranca(billingInfo.nextBillingTimeMillis)}"
        billingInfo.purchaseTimeMillis > 0L -> "Última: ${formatarDataCobranca(billingInfo.purchaseTimeMillis)}"
        else -> "Confira no Google Play"
    }
}

private fun formatarDataCobranca(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(millis)

private fun abrirGerenciamentoAssinaturaGooglePlay(context: Context, productId: String) {
    val baseUrl = "https://play.google.com/store/account/subscriptions"
    val url = if (productId.isNotBlank()) {
        "$baseUrl?sku=$productId&package=${context.packageName}"
    } else {
        baseUrl
    }
    val uri = Uri.parse(url)
    val playIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending")
    runCatching {
        context.startActivity(playIntent)
    }.recoverCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }.onFailure {
        Toast.makeText(context, "Não deu para abrir o Google Play.", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun AppVersionFooter(isDark: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val remoteChannel = AdminUsersSync.getChannelStatus(context)
    val isBeta = when (remoteChannel) {
        "beta"   -> true
        "oficial" -> false
        else     -> BuildConfig.DEBUG || BuildConfig.VERSION_NAME.contains("beta", ignoreCase = true)
    }
    val versionText = "Versão ${BuildConfig.VERSION_NAME}"
    val subColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val betaBg = if (isDark) Color(0xFF1E3A5F) else Color(0xFFEFF6FF)
    val betaColor = Color(0xFF60A5FA)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(versionText, color = subColor, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isBeta) "BETA" else "OFICIAL",
            color = if (isBeta) betaColor else Color(0xFF10B981),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    if (isBeta) betaBg else Color(0xFF10B981).copy(alpha = 0.14f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun BeneficioItem(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, color = Color(0xFFCBD5E1), fontSize = 13.sp)
    }
}

@Composable
private fun BackupIntervalButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2563EB) else Color(0xFF1F2937),
            contentColor = Color.White
        )
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ThemeModeOptionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (selected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                colorScheme.primary.copy(alpha = 0.14f)
            } else {
                if (isDark) Color(0xFF0F172A) else colorScheme.surface
            },
            contentColor = if (selected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.65f)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
