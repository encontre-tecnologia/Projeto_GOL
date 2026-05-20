package br.com.gui.carlembrete

import android.content.Context
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    planTier: PlanTier = PlanTier.FREE,
    onRequestPremium: (String) -> Unit = {},
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
    var backupInProgressAction by remember { mutableStateOf<BackupAction?>(null) }
    val canUseDriveBackup = planTier != PlanTier.FREE

    LaunchedEffect(canUseDriveBackup) {
        lastBackupTime = getLastBackupTime(context)
        backupInterval = getBackupInterval(context)
        if (canUseDriveBackup) {
            scheduleBackupWork(context, backupInterval)
        } else {
            backupInterval = BackupInterval.OFF
            scheduleBackupWork(context, BackupInterval.OFF)
        }
    }

    fun criarBackup(account: GoogleSignInAccount) {
        scope.launch(Dispatchers.IO) {
            try {
                BancoDeDados.salvarCarros(context, carros)
                BancoDeDados.salvarLembretes(context, lembretes)
                BancoDeDados.salvarContatos(context, contatos)
                val abastecimentos = BancoDeDados.carregarAbastecimentos(context)
                val pedaladas = BancoDeDados.carregarPedaladas(context)
                val travelTripsJson = loadTravelTripsBackupJson(context)
                val fleetStockItemsJson = loadFleetStockItemsBackupJson(context)
                val fleetStockMovementsJson = loadFleetStockMovementsBackupJson(context)
                val payload = BackupPayload(
                    carros = carros,
                    lembretes = lembretes,
                    contatos = contatos,
                    abastecimentos = abastecimentos,
                    pedaladas = pedaladas,
                    travelTripsJson = travelTripsJson,
                    fleetStockItemsJson = fleetStockItemsJson,
                    fleetStockMovementsJson = fleetStockMovementsJson
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
        scope.launch(Dispatchers.IO) {
            try {
                val payload = driveBackupManager.downloadBackup(account)
                if (payload == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.cfg_backup_not_found), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                BancoDeDados.salvarCarros(context, payload.carros)
                BancoDeDados.salvarLembretes(context, payload.lembretes)
                BancoDeDados.salvarContatos(context, payload.contatos)
                BancoDeDados.salvarAbastecimentos(context, payload.abastecimentos)
                BancoDeDados.salvarPedaladas(context, payload.pedaladas)
                saveTravelTripsBackupJson(context, payload.travelTripsJson)
                saveFleetStockBackupJson(
                    context,
                    itemsJson = payload.fleetStockItemsJson,
                    movementsJson = payload.fleetStockMovementsJson
                )
                NotificacaoHelper.reagendarExistentes(context.applicationContext, payload.lembretes)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.cfg_backup_restore_success), Toast.LENGTH_SHORT).show()
                    showRestoreRestartDialog = true
                }
            } catch (err: Exception) {
                Log.e("Backup", "Falha ao obter backup", err)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.toBackupErrorMessage(BackupAction.RESTORE, err),
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

    val driveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
                Toast.makeText(context, context.getString(R.string.cfg_drive_permission_denied), Toast.LENGTH_SHORT).show()
                pendingBackupAction = null
                backupInProgressAction = null
                return@rememberLauncherForActivityResult
            }
            when (pendingBackupAction) {
                BackupAction.BACKUP -> criarBackup(account)
                BackupAction.RESTORE -> recuperarBackup(account)
                null -> {}
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
        }
    }


    fun executarBackup(action: BackupAction) {
        if (!canUseDriveBackup) {
            onRequestPremium("drive_backup")
            return
        }
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (isDark) Color.Black else colorScheme.background,
        bottomBar = {
            Divider(color = colorScheme.outlineVariant, thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.cfg_version_label, BuildConfig.VERSION_NAME),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.common_back), tint = colorScheme.onSurface)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .padding(top = 2.dp)
                        .background(
                            color = colorScheme.primary.copy(alpha = if (colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    stringResource(R.string.cfg_title),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            SectionHeader(title = stringResource(R.string.cfg_section_appearance))
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeModeOptionButton(
                        label = stringResource(R.string.cfg_theme_system),
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

            SectionHeader(title = stringResource(R.string.cfg_section_backup))

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                val backupBusy = backupInProgressAction != null
                if (!canUseDriveBackup) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF111827) else Color(0xFFFFFBF2)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = if (isDark) 0.55f else 0.85f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Backup no Drive é Lite+",
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                "No plano grátis, seus dados ficam salvos neste aparelho. Assine o Lite para proteger veículos, avisos e históricos no Google Drive.",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (backupBusy) {
                    Text(
                        text = when (backupInProgressAction) {
                            BackupAction.BACKUP -> stringResource(R.string.cfg_backup_sending)
                            BackupAction.RESTORE -> stringResource(R.string.cfg_backup_restoring)
                            null -> ""
                        },
                        color = colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        executarBackup(BackupAction.BACKUP)
                    },
                    enabled = !backupBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canUseDriveBackup) Color(0xFF2563EB) else Color(0xFFF59E0B)
                    )
                ) {
                    Icon(
                        if (canUseDriveBackup) Icons.Default.CloudUpload else Icons.Default.Lock,
                        null,
                        tint = if (canUseDriveBackup) Color.White else Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (canUseDriveBackup) stringResource(R.string.cfg_backup_save) else "Liberar backup no Lite",
                        fontWeight = FontWeight.Bold,
                        color = if (canUseDriveBackup) Color.White else Color.Black
                    )
                }

                if (canUseDriveBackup) {
                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            executarBackup(BackupAction.RESTORE)
                        },
                        enabled = !backupBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (colorScheme.background.luminance() < 0.5f) Color.White else Color(0xFF0F172A)
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.cfg_backup_restore), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))
                val lastBackupLabel = if (lastBackupTime > 0L) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(lastBackupTime)
                } else {
                    stringResource(R.string.cfg_backup_none)
                }
                Text(
                    stringResource(R.string.cfg_backup_last, lastBackupLabel),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private enum class BackupAction {
    BACKUP,
    RESTORE
}

// Componente Reutilizável para o Cabeçalho da Seção (Estilo Android Settings)
@Composable
fun SectionHeader(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        Divider(
            color = if (isDark) Color.White.copy(alpha = 0.10f) else colorScheme.outlineVariant,
            thickness = 1.dp
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

private fun scheduleBackupWork(context: android.content.Context, interval: BackupInterval) {
    val workManager = WorkManager.getInstance(context)
    if (interval == BackupInterval.OFF) {
        workManager.cancelUniqueWork("drive_backup")
        return
    }
    val days = if (interval == BackupInterval.WEEKLY) 7L else 30L
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(days, TimeUnit.DAYS)
        .setConstraints(constraints)
        .build()
    workManager.enqueueUniquePeriodicWork(
        "drive_backup",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
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
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2563EB) else Color(0xFF1F2937),
            contentColor = Color.White
        )
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ThemeModeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colorScheme.primary else colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                colorScheme.primary.copy(alpha = 0.16f)
            } else {
                if (isDark) Color(0xFF111827) else colorScheme.surface
            },
            contentColor = colorScheme.onSurface
        )
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

