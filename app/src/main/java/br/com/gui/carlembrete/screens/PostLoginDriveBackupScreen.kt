package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PostLoginBackupState {
    CHECKING,
    NEEDS_PERMISSION,
    FOUND,
    NOT_FOUND,
    RESTORING,
    ERROR
}

private tailrec fun Context.findBackupActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findBackupActivity()
    else -> null
}

@Composable
fun PostLoginDriveBackupScreen(
    planTier: PlanTier = PlanTier.FREE,
    onContinueWithoutRestore: () -> Unit,
    onRestoreComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveScope = remember { Scope(DriveScopes.DRIVE_APPDATA) }
    val driveBackupManager = remember { DriveBackupManager(context) }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(driveScope)
                .build()
        )
    }
    var state by remember { mutableStateOf(PostLoginBackupState.CHECKING) }
    var accountWithBackup by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val canUseDriveBackup = planTier != PlanTier.FREE

    fun checkBackup(account: GoogleSignInAccount) {
        if (!canUseDriveBackup) {
            onContinueWithoutRestore()
            return
        }
        state = PostLoginBackupState.CHECKING
        scope.launch {
            runCatching {
                driveBackupManager.hasBackup(account)
            }.onSuccess { hasBackup ->
                if (hasBackup) {
                    accountWithBackup = account
                    state = PostLoginBackupState.FOUND
                } else {
                    state = PostLoginBackupState.NOT_FOUND
                }
            }.onFailure { err ->
                errorMessage = err.message?.takeIf { it.isNotBlank() }
                    ?: "Nao foi possivel verificar o backup no Drive."
                state = PostLoginBackupState.ERROR
            }
        }
    }

    fun restoreBackup(account: GoogleSignInAccount) {
        if (!canUseDriveBackup) {
            onContinueWithoutRestore()
            return
        }
        state = PostLoginBackupState.RESTORING
        scope.launch(Dispatchers.IO) {
            try {
                val payload = driveBackupManager.downloadBackup(account)
                if (payload == null) {
                    withContext(Dispatchers.Main) { state = PostLoginBackupState.NOT_FOUND }
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
                    Toast.makeText(context, "Backup restaurado com sucesso.", Toast.LENGTH_SHORT).show()
                    onRestoreComplete()
                }
            } catch (err: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = err.message?.takeIf { it.isNotBlank() }
                        ?: "Falha ao restaurar o backup do Drive."
                    state = PostLoginBackupState.ERROR
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
            if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
                checkBackup(account)
            } else {
                state = PostLoginBackupState.NEEDS_PERMISSION
            }
        } catch (_: ApiException) {
            state = PostLoginBackupState.NEEDS_PERMISSION
        }
    }

    LaunchedEffect(Unit) {
        if (!canUseDriveBackup) {
            onContinueWithoutRestore()
            return@LaunchedEffect
        }
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
            checkBackup(account)
        } else {
            state = PostLoginBackupState.NEEDS_PERMISSION
        }
    }

    val pageBg = Color.Black
    val cardBg = Color(0xFF111827)
    val titleColor = Color(0xFFF8FAFC)
    val bodyColor = Color(0xFFCBD5E1)
    val accentBlue = Color(0xFF60A5FA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (state) {
                    PostLoginBackupState.CHECKING -> {
                        CircularProgressIndicator(color = accentBlue)
                        Text("Procurando backup no Drive", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "So um segundo: estamos vendo se sua conta ja tem dados salvos para restaurar.",
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                    PostLoginBackupState.NEEDS_PERMISSION -> {
                        Icon(Icons.Default.CloudDownload, null, tint = accentBlue, modifier = Modifier.size(54.dp))
                        Text("Verificar backup no Drive", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "Para saber se sua conta ja tem backup, o Zellu precisa acessar a pasta segura do app no Google Drive.",
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { driveLauncher.launch(googleSignInClient.signInIntent) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                        ) {
                            Text("Verificar meu backup", color = Color(0xFF06111F), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onContinueWithoutRestore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Pular e ir para boas-vindas", color = titleColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PostLoginBackupState.FOUND -> {
                        Icon(Icons.Default.CloudDownload, null, tint = accentBlue, modifier = Modifier.size(54.dp))
                        Text("Encontramos um backup", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "Quer restaurar os dados do Drive agora? Se preferir nao restaurar, voce segue para o fluxo de boas-vindas.",
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { accountWithBackup?.let(::restoreBackup) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                        ) {
                            Text("Restaurar backup", color = Color(0xFF06111F), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onContinueWithoutRestore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Nao, continuar sem restaurar", color = titleColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PostLoginBackupState.NOT_FOUND -> {
                        Icon(Icons.Default.CloudOff, null, tint = Color(0xFF64748B), modifier = Modifier.size(52.dp))
                        Text("Nenhum backup encontrado", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "Nao encontramos backup nessa conta do Google Drive. Vamos seguir para o fluxo de boas-vindas.",
                            color = bodyColor,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = onContinueWithoutRestore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                        ) {
                            Text("Continuar", color = Color(0xFF06111F), fontWeight = FontWeight.Bold)
                        }
                    }
                    PostLoginBackupState.RESTORING -> {
                        CircularProgressIndicator(color = accentBlue)
                        Text("Restaurando backup", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Estamos trazendo seus veiculos, avisos e historicos do Drive.", color = bodyColor, textAlign = TextAlign.Center)
                    }
                    PostLoginBackupState.ERROR -> {
                        Icon(Icons.Default.CloudOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(52.dp))
                        Text("Nao deu para verificar o backup", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(errorMessage.orEmpty(), color = bodyColor, textAlign = TextAlign.Center, fontSize = 14.sp)
                        Button(
                            onClick = onContinueWithoutRestore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                        ) {
                            Text("Continuar mesmo assim", color = Color(0xFF06111F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
