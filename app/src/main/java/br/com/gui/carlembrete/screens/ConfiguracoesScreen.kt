package br.com.gui.carlembrete

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var adminTapCount by remember { mutableStateOf(0) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showRestoreRestartDialog by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(AppPreferences.getThemeMode(context)) }
    var adminPassword by remember { mutableStateOf("") }
    var lastBackupTime by remember { mutableStateOf(0L) }
    var backupInterval by remember { mutableStateOf(BackupInterval.OFF) }
    val driveScope = remember { Scope(DriveScopes.DRIVE_APPDATA) }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(driveScope)
                .build()
        )
    }
    val driveBackupManager = remember { DriveBackupManager(context) }
    var pendingBackupAction by remember { mutableStateOf<BackupAction?>(null) }
    var backupInProgressAction by remember { mutableStateOf<BackupAction?>(null) }

    LaunchedEffect(Unit) {
        lastBackupTime = getLastBackupTime(context)
        backupInterval = getBackupInterval(context)
        scheduleBackupWork(context, backupInterval)
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
                    Toast.makeText(context, context.getString(R.string.cfg_backup_sent_success), Toast.LENGTH_SHORT).show()
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
    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminDialog = false
                adminPassword = ""
            },
            title = { Text(stringResource(R.string.cfg_admin_title), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.cfg_admin_password_placeholder), color = Color(0xFF94A3B8)) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827),
                        disabledContainerColor = Color(0xFF111827),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdminDialog = false
                        adminPassword = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(stringResource(R.string.common_ok), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAdminDialog = false
                        adminPassword = ""
                    },
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(stringResource(R.string.common_cancel), color = Color.White)
                }
            },
            containerColor = if (isDark) Color(0xFF111827) else Color(0xFF0F172A)
        )
    }
    if (showRestoreRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreRestartDialog = false },
            title = { Text(stringResource(R.string.cfg_restore_done_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.cfg_restore_done_message))
            },
            confirmButton = {
                Button(
                    onClick = { showRestoreRestartDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(stringResource(R.string.common_ok), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (isDark) Color(0xFF111827) else colorScheme.surface
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (isDark) Color.Black else colorScheme.background,
        bottomBar = {
            Divider(color = colorScheme.outlineVariant, thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        adminTapCount += 1
                        if (adminTapCount >= 5) {
                            adminTapCount = 0
                            showAdminDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.cfg_version_label),
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
                Text(
                    stringResource(R.string.cfg_theme_label),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cfg_backup_save), fontWeight = FontWeight.Bold, color = Color.White)
                }

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

                Spacer(Modifier.height(12.dp))
                val lastBackupLabel = if (lastBackupTime > 0L) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(lastBackupTime)
                } else {
                    stringResource(R.string.cfg_backup_none)
                }
                Text(
                    stringResource(R.string.cfg_backup_last, lastBackupLabel),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
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

