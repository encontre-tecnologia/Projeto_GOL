package br.com.gui.carlembrete

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onDismiss: () -> Unit,
    onTestarNotificacao: () -> Unit,
    carros: List<CarroInfo>,
    lembretes: List<Lembrete>,
    contatos: List<ContatoProfissional>
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = (LocalContext.current as? android.app.Activity)
    val subscriptionManager = remember { SubscriptionManager(context) }
    val planTier by subscriptionManager.planTier.collectAsState()
    var adminTapCount by remember { mutableStateOf(0) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var adminPassword by remember { mutableStateOf("") }
    var adminUnlocked by remember { mutableStateOf(false) }
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
                val payload = BackupPayload(carros, lembretes, contatos)
                driveBackupManager.uploadBackup(payload, account)
                withContext(Dispatchers.Main) {
                    val now = System.currentTimeMillis()
                    setLastBackupTime(context, now)
                    lastBackupTime = now
                    Toast.makeText(context, "Backup enviado com sucesso!", Toast.LENGTH_SHORT).show()
                }
            } catch (err: Exception) {
                Log.e("Backup", "Falha ao enviar backup", err)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Falha ao enviar: ${err.message}", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, "Nenhum backup encontrado no Drive.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                BancoDeDados.salvarCarros(context, payload.carros)
                BancoDeDados.salvarLembretes(context, payload.lembretes)
                BancoDeDados.salvarContatos(context, payload.contatos)
                NotificacaoHelper.reagendarExistentes(context.applicationContext, payload.lembretes)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Dados restaurados com sucesso!", Toast.LENGTH_SHORT).show()
                }
            } catch (err: Exception) {
                Log.e("Backup", "Falha ao obter backup", err)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro ao restaurar: ${err.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(context, "Permissao do Drive nao concedida.", Toast.LENGTH_SHORT).show()
                pendingBackupAction = null
                return@rememberLauncherForActivityResult
            }
            when (pendingBackupAction) {
                BackupAction.BACKUP -> criarBackup(account)
                BackupAction.RESTORE -> recuperarBackup(account)
                null -> {}
            }
        } catch (_: ApiException) {
            Toast.makeText(context, "Falha ao autenticar com Google.", Toast.LENGTH_SHORT).show()
    } finally {
        pendingBackupAction = null
    }
}


    fun executarBackup(action: BackupAction) {
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
    DisposableEffect(Unit) {
        subscriptionManager.connect()
        onDispose { subscriptionManager.disconnect() }
    }

    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminDialog = false
                adminPassword = ""
            },
            title = { Text("Admin", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    singleLine = true,
                    placeholder = { Text("Senha", color = Color(0xFF94A3B8)) },
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
                        if (adminPassword == "GUIMIN14") {
                            adminUnlocked = true
                        }
                        showAdminDialog = false
                        adminPassword = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
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
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("Recurso Premium", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold) },
            text = { Text("Assine o Premium para ativar backup automático.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        if (activity != null) subscriptionManager.launchPurchaseFlow(activity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Assinar Premium", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPremiumDialog = false },
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Text("Agora não", color = Color(0xFFF59E0B))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF070C18),
        bottomBar = {
            Divider(color = Color(0xFF334155), thickness = 1.dp)
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
                    "Versão 1.0.0",
                    color = Color(0xFF475569),
                    fontSize = 12.sp
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configurações",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF070C18))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {

            // --- SEÇÃO 1: BACKUP E DADOS ---
            SectionHeader(title = "PLANO PREMIUM")

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {

                // Card de Venda (Só aparece se não for assinante)
                if (planTier == PlanTier.FREE) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Ouro
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Zellu Premium",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            // Benefícios compactos
                            BeneficioItem("Zellu Guardião sempre ativo")
                            BeneficioItem("Backup automático no Google Drive")
                            BeneficioItem("OCR ilimitado com sugestões")
                            BeneficioItem("PDF completo em 1 toque")

                            Button(
                                onClick = {
                                    if (activity != null) subscriptionManager.launchPurchaseFlow(activity)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                            ) {
                                Text("Assinar por R$ 19,90/mês", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "7 dias grátis para testar",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Feedback visual se já for assinante
                    val planoLabel = "Premium"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
                        Spacer(Modifier.width(8.dp))
                        Text("Sua assinatura $planoLabel está ativa.", color = Color(0xFF10B981), fontSize = 14.sp)
                    }
                }

                // Botões de Ação (Backup e Restore)
                SectionHeader(title = "BACKUP GRATUITO")

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        executarBackup(BackupAction.BACKUP)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fazer Backup na Nuvem", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        executarBackup(BackupAction.RESTORE)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restaurar Backup", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))
                val lastBackupLabel = if (lastBackupTime > 0L) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(lastBackupTime)
                } else {
                    "Nenhum backup ainda"
                }
                Text(
                    "Último backup: $lastBackupLabel",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Agendar backup automático",
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BackupIntervalButton(
                        label = "Desligado",
                        selected = backupInterval == BackupInterval.OFF,
                        onClick = {
                            backupInterval = BackupInterval.OFF
                            setBackupInterval(context, backupInterval)
                            scheduleBackupWork(context, backupInterval)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BackupIntervalButton(
                        label = "Semanal",
                        selected = backupInterval == BackupInterval.WEEKLY,
                        onClick = {
                            if (planTier == PlanTier.FREE) {
                                showPremiumDialog = true
                            } else {
                                backupInterval = BackupInterval.WEEKLY
                                setBackupInterval(context, backupInterval)
                                scheduleBackupWork(context, backupInterval)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BackupIntervalButton(
                        label = "Mensal",
                        selected = backupInterval == BackupInterval.MONTHLY,
                        onClick = {
                            if (planTier == PlanTier.FREE) {
                                showPremiumDialog = true
                            } else {
                                backupInterval = BackupInterval.MONTHLY
                                setBackupInterval(context, backupInterval)
                                scheduleBackupWork(context, backupInterval)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- SEÇÃO 2: NOTIFICAÇÕES ---
            Spacer(Modifier.height(16.dp))
            SectionHeader(title = "NOTIFICAÇÕES")

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    "Teste se seu aparelho está recebendo os alertas corretamente.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (adminUnlocked) {
                    Button(
                        onClick = onTestarNotificacao,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Testar Notificação", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- RODAPÉ ---
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(0xFF64748B), // Cinza azulado tipo Android System
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        Divider(
            color = Color(0xFF334155), // Linha sutil
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



