package br.com.gui.carlembrete

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class StepState { Waiting, Loading, Found, NotFound }

private data class SearchState(
    val started: Boolean = false,
    val localStep: StepState = StepState.Waiting,
    val localCount: Int = 0,
    val driveStep: StepState = StepState.Waiting,
    val driveModifiedAt: Long? = null,
    val driveNeedsAuth: Boolean = false,
    val restoring: Boolean = false
) {
    val isSearching: Boolean
        get() = localStep == StepState.Loading || driveStep == StepState.Loading
    val isDone: Boolean
        get() = started && !isSearching && !restoring
    val anyFound: Boolean
        get() = localStep == StepState.Found || driveStep == StepState.Found
    val bothFound: Boolean
        get() = localStep == StepState.Found && driveStep == StepState.Found
}

private enum class DriveSignInIntent { Check, Restore }

@Composable
fun BackupCheckScreen(
    onContinue: () -> Unit,
    onNoBackup: () -> Unit,
    cardBg: Color = Color(0xFF1E293B),
    accentColor: Color = Color(0xFF22C55E),
    title: String? = null,
    subtitle: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveScope = remember { Scope(DriveScopes.DRIVE_APPDATA) }
    val driveBackupManager = remember { DriveBackupManager(context) }

    var state by remember { mutableStateOf(SearchState()) }
    var driveSignInIntent by remember { mutableStateOf(DriveSignInIntent.Restore) }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(driveScope)
                .build()
        )
    }

    fun restoreDriveAccount(account: GoogleSignInAccount) {
        scope.launch {
            state = state.copy(restoring = true)
            val payload = withContext(Dispatchers.IO) { driveBackupManager.downloadBackup(account) }
            if (payload != null) {
                withContext(Dispatchers.IO) { applyBackupPayload(context, payload) }
                onContinue()
            } else {
                state = state.copy(restoring = false)
                Toast.makeText(context, "Nenhum backup encontrado no Drive", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val driveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
                    when (driveSignInIntent) {
                        DriveSignInIntent.Check -> {
                            state = state.copy(driveStep = StepState.Loading, driveNeedsAuth = false)
                            val modifiedAt = withContext(Dispatchers.IO) {
                                driveBackupManager.backupModifiedAtInDrive(account)
                            }
                            state = state.copy(
                                driveStep = if (modifiedAt != null) StepState.Found else StepState.NotFound,
                                driveModifiedAt = modifiedAt
                            )
                        }
                        DriveSignInIntent.Restore -> restoreDriveAccount(account)
                    }
                } else {
                    Toast.makeText(context, "Permissão negada para o Google Drive", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Falha ao acessar o Google Drive", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startSearch() {
        scope.launch {
            state = SearchState(started = true, localStep = StepState.Loading)

            // 1. Busca local
            val localCarros = withContext(Dispatchers.IO) { BancoDeDados.carregarCarros(context) }
            val localCount = localCarros?.size ?: 0
            delay(500)
            state = state.copy(
                localStep = if (localCount > 0) StepState.Found else StepState.NotFound,
                localCount = localCount,
                driveStep = StepState.Loading
            )

            // 2. Busca no Drive
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val hasAuth = account != null && GoogleSignIn.hasPermissions(account, driveScope)
            val driveModifiedAt = if (hasAuth) {
                withContext(Dispatchers.IO) { driveBackupManager.backupModifiedAtInDrive(account!!) }
            } else null
            state = state.copy(
                driveStep = if (driveModifiedAt != null) StepState.Found else StepState.NotFound,
                driveModifiedAt = driveModifiedAt,
                driveNeedsAuth = !hasAuth
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = screenHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (title != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF1E3A5F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                        HorizontalDivider(
                            color = Color(0xFF334155),
                            thickness = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    when {
                        state.restoring -> RestoringSection(accentColor)
                        !state.started -> IdleSection(accentColor, onSearch = ::startSearch)
                        else -> SearchSection(
                            state = state,
                            accentColor = accentColor,
                            onRestoreLocal = onContinue,
                            onRestoreDrive = {
                                val account = GoogleSignIn.getLastSignedInAccount(context)
                                if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
                                    restoreDriveAccount(account)
                                } else {
                                    driveSignInIntent = DriveSignInIntent.Restore
                                    driveLauncher.launch(googleSignInClient.signInIntent)
                                }
                            },
                            onCheckDrive = {
                                driveSignInIntent = DriveSignInIntent.Check
                                driveLauncher.launch(googleSignInClient.signInIntent)
                            },
                            onNoBackup = onNoBackup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleSection(accentColor: Color, onSearch: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Color(0xFF1E3A5F).copy(alpha = pulse * 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFF1E3A5F), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(30.dp)
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Verificar backup",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Vamos verificar se há dados salvos no seu dispositivo ou no Google Drive.",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }

    Button(
        onClick = onSearch,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Buscar Backup",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun RestoringSection(accentColor: Color) {
    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(48.dp))
    Text(
        "Restaurando seus dados…",
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SearchSection(
    state: SearchState,
    accentColor: Color,
    onRestoreLocal: () -> Unit,
    onRestoreDrive: () -> Unit,
    onCheckDrive: () -> Unit,
    onNoBackup: () -> Unit
) {
    Text(
        "Verificando seus dados",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )

    val driveDetail = when {
        state.driveNeedsAuth && state.driveStep == StepState.NotFound -> "Login necessário"
        state.driveModifiedAt != null -> {
            val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")) }
            "Salvo em ${sdf.format(java.util.Date(state.driveModifiedAt))}"
        }
        else -> ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchStepRow(
            icon = Icons.Default.FolderOpen,
            label = "Dispositivo",
            stepState = state.localStep,
            detail = if (state.localCount > 0)
                "${state.localCount} veículo${if (state.localCount != 1) "s" else ""} encontrado${if (state.localCount != 1) "s" else ""}"
            else "",
            accentColor = accentColor
        )
        SearchStepRow(
            icon = Icons.Default.Cloud,
            label = "Google Drive",
            stepState = state.driveStep,
            detail = driveDetail,
            accentColor = Color(0xFF60A5FA)
        )
    }

    AnimatedVisibility(
        visible = state.isDone,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 3 })
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.anyFound) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum backup encontrado",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (state.bothFound) {
                Text(
                    "Selecione qual deseja restaurar:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (state.localStep == StepState.Found) {
                Button(
                    onClick = onRestoreLocal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Restaurar do Dispositivo",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (state.driveStep == StepState.Found) {
                OutlinedButton(
                    onClick = onRestoreDrive,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD))
                ) {
                    Icon(Icons.Default.CloudDownload, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Restaurar do Google Drive",
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (state.driveNeedsAuth && state.driveStep == StepState.NotFound) {
                OutlinedButton(
                    onClick = onCheckDrive,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD))
                ) {
                    Icon(Icons.Default.Cloud, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Verificar no Google Drive",
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (!state.anyFound) {
                Button(
                    onClick = onNoBackup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Cadastrar meu veículo",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                TextButton(onClick = onNoBackup, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Ignorar e cadastrar novo",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchStepRow(
    icon: ImageVector,
    label: String,
    stepState: StepState,
    detail: String = "",
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when (stepState) {
                        StepState.Found -> Color(0xFF14532D)
                        StepState.Loading -> Color(0xFF1E3A5F)
                        else -> Color(0xFF1C1C2E)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (stepState) {
                StepState.Loading -> CircularProgressIndicator(
                    color = accentColor,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                StepState.Found -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                StepState.NotFound -> Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
                StepState.Waiting -> Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            val statusText = when (stepState) {
                StepState.Waiting -> "Aguardando…"
                StepState.Loading -> "Buscando…"
                StepState.Found -> detail.ifBlank { "Encontrado" }
                StepState.NotFound -> detail.ifBlank { "Não encontrado" }
            }
            Text(
                statusText,
                color = when (stepState) {
                    StepState.Found -> accentColor
                    StepState.NotFound -> Color(0xFF475569)
                    else -> Color.White.copy(alpha = 0.35f)
                },
                fontSize = 12.sp
            )
        }
    }
}

private fun applyBackupPayload(context: android.content.Context, payload: BackupPayload) {
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
    payload.fuelStartKms.forEach { (carroId, km) ->
        AppPreferences.setFuelStartKm(context, carroId, km)
    }
    NotificacaoHelper.reagendarExistentes(context.applicationContext, payload.lembretes)
}
