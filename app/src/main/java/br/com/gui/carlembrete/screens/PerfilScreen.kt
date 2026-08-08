package br.com.gui.carlembrete

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ExecutionException
import com.google.android.gms.tasks.Tasks as GmsTasks
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val AccentBlue = Color(0xFF3B82F6)

@Composable
fun PerfilScreen(
    onDismiss: () -> Unit,
    planTier: PlanTier,
    subscriptionBillingInfo: SubscriptionBillingInfo,
    totalVeiculos: Int
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color.Black else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val auth = remember { FirebaseAuth.getInstance() }
    val user = auth.currentUser
    val nome = user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuário"
    val email = user?.email?.takeIf { it.isNotBlank() } ?: "Email não informado"
    val foto = user?.photoUrl?.toString()
    val ultimoLoginTexto = formatarData(user?.metadata?.lastSignInTimestamp ?: 0L)
    val planLabel = planNameLabel(planTier)
    val avisoLimit = reminderLimitForPlan(planTier)
    var planPrices by remember { mutableStateOf(RemotePlanPricing.defaultPrices) }
    DisposableEffect(Unit) {
        val pricingRegistration = RemotePlanPricing.listen { planPrices = it }
        onDispose { pricingRegistration.remove() }
    }
    // A análise da garagem responde por regras locais, sem custo e sem cota. A cota só
    // existe para o caminho de consulta online, que hoje está desligado — então este
    // bloco inteiro fica oculto até existir caminho online de verdade.
    val temConsultaOnline = zelluAiOnlineDisponivel()
    val aiUsageCount = AiUsageLimiter.currentCount(context)
    val aiLimit = planPrices.aiLimitForTier(planTier)
    val aiUsageValue = when {
        aiLimit <= 0 -> "$aiUsageCount / Ilimitado"
        else -> "$aiUsageCount/$aiLimit"
    }
    val aiRenewalText = when {
        planTier == PlanTier.FREE -> "Cota do plano Grátis — o contador zera todo mês"
        subscriptionBillingInfo.nextBillingTimeMillis > 0L ->
            "Próxima renovação: ${formatarProximaRenovacao(subscriptionBillingInfo.nextBillingTimeMillis)}"
        else -> "Consulte a próxima renovação no Google Play"
    }
    val totalAvisosAtivos = remember(context) {
        BancoDeDados.carregarLembretes(context).count { lembrete ->
            lembrete.tipo != TipoManutencao.ABASTECIMENTO && !isLembreteRealizado(lembrete)
        }
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeletingDialog by remember { mutableStateOf(false) }
    var deleteProgressStep by remember { mutableStateOf<String?>(null) }
    var deleteSuccess by remember { mutableStateOf(false) }
    var deleteNeedsReauth by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val driveBackupManager = remember { DriveBackupManager(context) }
    val driveScope = remember { Scope(DriveScopes.DRIVE_APPDATA) }
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }

    suspend fun excluirAuthConta(): Boolean = suspendCancellableCoroutine { cont ->
        val user = auth.currentUser
        if (user == null) { cont.resume(true); return@suspendCancellableCoroutine }
        user.delete().addOnCompleteListener { task ->
            when {
                task.isSuccessful -> cont.resume(true)
                task.exception is FirebaseAuthRecentLoginRequiredException -> cont.resume(false)
                else -> cont.resumeWithException(task.exception ?: Exception("Falha ao excluir conta."))
            }
        }
    }

    fun iniciarExclusao() {
        showDeleteConfirmDialog = false
        showDeletingDialog = true
        deleteSuccess = false
        deleteNeedsReauth = false
        deleteProgressStep = null

        scope.launch {
            try {
                // Passo 1: dados locais
                deleteProgressStep = "local"
                withContext(Dispatchers.IO) { apagarDadosLocais(context) }
                delay(500)

                // Passo 2: Drive
                deleteProgressStep = "drive"
                try {
                    val driveAccount = GoogleSignIn.getLastSignedInAccount(context)
                    if (driveAccount != null) {
                        driveBackupManager.deleteBackup(driveAccount)
                    }
                } catch (_: Exception) {}
                delay(400)

                // Passo 3: Firestore
                deleteProgressStep = "firestore"
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    withContext(Dispatchers.IO) {
                        val db = FirebaseFirestore.getInstance()
                        listOf(
                            db.collection("admin_users").document(uid).delete(),
                            db.collection("guardian_alerts").document(uid).delete(),
                            db.collection("users").document(uid).delete()
                        ).forEach { t -> try { GmsTasks.await(t) } catch (_: Exception) {} }
                    }
                }
                delay(400)

                // Passo 4: Auth
                deleteProgressStep = "auth"
                val deleted = excluirAuthConta()
                if (!deleted) {
                    deleteNeedsReauth = true
                    return@launch
                }
                googleSignInClient.signOut()
                deleteProgressStep = null
                deleteSuccess = true
            } catch (err: Exception) {
                deleteProgressStep = null
                showDeletingDialog = false
                Toast.makeText(context, "Erro ao excluir: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val googleReauthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Token do Google não gerado. Tente novamente.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(token, null)
            auth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            googleSignInClient.signOut()
                            deleteNeedsReauth = false
                            deleteSuccess = true
                        } else {
                            showDeletingDialog = false
                            Toast.makeText(context, "Não foi possível excluir a conta. Tente novamente.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Não deu para confirmar sua conta Google.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Confirmação com Google cancelada.", Toast.LENGTH_LONG).show()
        }
    }

    if (showDeleteConfirmDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            isLoading = false,
            onConfirm = { iniciarExclusao() }
        )
    }

    if (showDeletingDialog) {
        DeletingAccountDialog(
            progressStep = deleteProgressStep,
            isSuccess = deleteSuccess,
            needsReauth = deleteNeedsReauth,
            isDark = isDark,
            onReauth = {
                googleSignInClient.signOut().addOnCompleteListener {
                    googleReauthLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            onGoToLogin = {
                showDeletingDialog = false
                onDismiss()
            }
        )
    }

    BackHandler {
        when {
            showDeleteConfirmDialog -> showDeleteConfirmDialog = false
            showDeletingDialog && (deleteSuccess || deleteNeedsReauth) -> { /* non-dismissable during progress */ }
            !showDeletingDialog -> onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Voltar",
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.height(24.dp))

            // --- AVATAR + NOME ---
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentBlue, Color(0xFF6366F1))),
                        CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(cardBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!foto.isNullOrBlank()) {
                        AsyncImage(
                            model = foto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = nome,
                color = titleColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Plan badge
            val badgeColor = when (planTier) {
                PlanTier.FREE -> AccentBlue
                PlanTier.LITE -> Color(0xFF60A5FA)
                PlanTier.FROTA -> Color(0xFFFBBF24)
                PlanTier.ENTERPRISE -> Color(0xFF22D3EE)
            }
            val badgeBg = when (planTier) {
                PlanTier.FREE -> if (isDark) Color(0xFF1E3A5F) else Color(0xFFEFF6FF)
                PlanTier.LITE -> if (isDark) Color(0xFF172554) else Color(0xFFDBEAFE)
                PlanTier.FROTA -> if (isDark) Color(0xFF451A03) else Color(0xFFFFF4D8)
                PlanTier.ENTERPRISE -> if (isDark) Color(0xFF164E63) else Color(0xFFCFFAFE)
            }
            val badgeIcon = if (planTier == PlanTier.FREE) Icons.Rounded.VerifiedUser else Icons.Rounded.Star

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                Text(planLabel, color = badgeColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))

            // --- STATS ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Veículos",
                    value = totalVeiculos.toString(),
                    color = AccentBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Plano",
                    value = planLabel,
                    color = badgeColor
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Avisos ativos",
                    value = "$totalAvisosAtivos/$avisoLimit",
                    color = Color(0xFF34D399)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Veículos",
                    value = "$totalVeiculos/${vehicleLimitForPlan(planTier)}",
                    color = Color(0xFF60A5FA)
                )
                // A cota de consultas online só aparece quando existe caminho online de
                // verdade. Sem isso, seria um contador de algo que nunca é consumido.
                if (temConsultaOnline) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Consultas online no mes",
                        value = aiUsageValue,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            if (temConsultaOnline) {
                Text(
                    text = aiRenewalText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontSize = 12.sp,
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(10.dp))

            // --- INFO CARD ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                InfoRowDark(
                    icon = Icons.Rounded.Email,
                    iconColor = AccentBlue,
                    iconBg = if (isDark) Color(0xFF1E3A5F) else Color(0xFFEFF6FF),
                    label = "Email",
                    value = email
                )
                HorizontalDivider(thickness = 1.dp, color = cardBorder.copy(alpha = 0.5f))
                InfoRowDark(
                    icon = Icons.Rounded.History,
                    iconColor = Color(0xFF34D399),
                    iconBg = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                    label = "Último acesso",
                    value = ultimoLoginTexto
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- DELETE BUTTON ---
            OutlinedButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFC8181))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Excluir conta e dados", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // --- LOGOUT BUTTON ---
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE2E8F0))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Sair da conta",
                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)
    val dimColor = if (isDark) Color(0xFF64748B) else Color(0xFF475569)

    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(label, color = dimColor, fontSize = 12.sp)
    }
}

@Composable
private fun InfoRowDark(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    label: String,
    value: String
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val dimColor = if (isDark) Color(0xFF64748B) else Color(0xFF475569)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = dimColor, fontSize = 11.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7F1D1D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFC8181), modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                tr("Apagar permanentemente?", "Delete permanently?"),
                color = titleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                tr(
                    "Esta ação não pode ser desfeita. Todos os seus dados serão perdidos.",
                    "This action cannot be undone. All your data will be lost."
                ),
                color = subColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = subColor)
                ) {
                    Text(tr("Cancelar", "Cancel"), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFC8181)
                        )
                    } else {
                        Text(tr("Apagar", "Delete"), color = Color(0xFFFC8181), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletingAccountDialog(
    progressStep: String?,
    isSuccess: Boolean,
    needsReauth: Boolean,
    isDark: Boolean,
    onReauth: () -> Unit,
    onGoToLogin: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)

    val stepOrder = listOf("local", "drive", "firestore", "auth")
    val stepLabels = mapOf(
        "local" to "Removendo dados locais...",
        "drive" to "Apagando backup no Google Drive...",
        "firestore" to "Removendo dados do servidor...",
        "auth" to "Removendo conta..."
    )
    val currentIndex = stepOrder.indexOf(progressStep)

    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isSuccess -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF064E3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Conta excluída com sucesso",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Todos os seus dados foram removidos permanentemente.",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onGoToLogin,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Ir para tela de login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                needsReauth -> {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF78350F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Confirmação necessária",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Para finalizar, precisamos confirmar sua identidade no Google.",
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onReauth,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Confirmar com Google", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                else -> {
                    Text(
                        "Excluindo conta...",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        stepOrder.forEachIndexed { i, step ->
                            val isDone = i < currentIndex
                            val isActive = i == currentIndex
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                    when {
                                        isDone -> Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        isActive -> CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color(0xFFFC8181),
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
                                        isActive -> if (isDark) Color.White else Color(0xFF0F172A)
                                        else -> Color(0xFF475569)
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatarData(millis: Long): String {
    if (millis <= 0L) return "Não disponível"
    return try {
        val data = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(data)
    } catch (e: Exception) { "Formato inválido" }
}

private fun formatarProximaRenovacao(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR")))

private fun formatarTempoDesdeLogin(lastSignInMillis: Long): String {
    if (lastSignInMillis <= 0L) return "N/A"
    val agora = Instant.now()
    val ultimo = Instant.ofEpochMilli(lastSignInMillis)
    val dias = ChronoUnit.DAYS.between(ultimo, agora)
    if (dias > 0) return "Ativo há $dias ${if (dias == 1L) "dia" else "dias"}"
    val horas = ChronoUnit.HOURS.between(ultimo, agora)
    if (horas > 0) return "Ativo há $horas ${if (horas == 1L) "hora" else "horas"}"
    val minutos = ChronoUnit.MINUTES.between(ultimo, agora)
    return if (minutos > 0) "Ativo há $minutos min" else "Ativo agora"
}


private fun apagarDadosLocais(context: Context) {
    BancoDeDados.salvarCarros(context, emptyList())
    BancoDeDados.salvarLembretes(context, emptyList())
    BancoDeDados.salvarContatos(context, emptyList())
    BancoDeDados.salvarAbastecimentos(context, emptyList())
    BancoDeDados.salvarPedaladas(context, emptyList())

    context.getSharedPreferences("app_prefs_v3", Context.MODE_PRIVATE).edit().clear().apply()
    context.getSharedPreferences("home_tutorial_prefs", Context.MODE_PRIVATE).edit().clear().apply()
}

