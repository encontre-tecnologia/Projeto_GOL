package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private const val TAG_LOGIN_BACKUP_FLOW = "LoginBackupFlow"

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
        Log.d(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.checkBackup start canUse=$canUseDriveBackup email=${account.email}")
        if (!canUseDriveBackup) {
            onContinueWithoutRestore()
            return
        }
        state = PostLoginBackupState.CHECKING
        scope.launch {
            runCatching {
                driveBackupManager.hasBackup(account)
            }.onSuccess { hasBackup ->
                Log.d(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.checkBackup hasBackup=$hasBackup")
                if (hasBackup) {
                    accountWithBackup = account
                    state = PostLoginBackupState.FOUND
                } else {
                    state = PostLoginBackupState.NOT_FOUND
                }
            }.onFailure { err ->
                Log.e(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.checkBackup failed", err)
                errorMessage = err.message?.takeIf { it.isNotBlank() }
                    ?: "Nao foi possivel verificar o backup no Drive."
                state = PostLoginBackupState.ERROR
            }
        }
    }

    fun restoreBackup(account: GoogleSignInAccount) {
        Log.d(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.restoreBackup start canUse=$canUseDriveBackup email=${account.email}")
        if (!canUseDriveBackup) {
            onContinueWithoutRestore()
            return
        }
        state = PostLoginBackupState.RESTORING
        scope.launch(Dispatchers.IO) {
            try {
                val payload = driveBackupManager.downloadBackup(account)
                Log.d(
                    TAG_LOGIN_BACKUP_FLOW,
                    "PostLoginDrive.restoreBackup payload carros=${payload?.carros?.size ?: -1} " +
                        "lembretes=${payload?.lembretes?.size ?: -1} contatos=${payload?.contatos?.size ?: -1}"
                )
                if (payload == null || payload.carros.isEmpty()) {
                    Log.w(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.restoreBackup rejected empty/null payload")
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
                if (payload.carros.isNotEmpty()) {
                    AppPreferences.markOnboardingComplete(context)
                }
                val savedCount = BancoDeDados.carregarCarros(context)?.size ?: -1
                Log.d(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.restoreBackup saved carros=$savedCount")
                NotificacaoHelper.reagendarExistentes(context.applicationContext, payload.lembretes)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup restaurado com sucesso.", Toast.LENGTH_SHORT).show()
                    onRestoreComplete()
                }
            } catch (err: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG_LOGIN_BACKUP_FLOW, "PostLoginDrive.restoreBackup failed", err)
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
    val corErro = Color(0xFFEF4444)
    val corNeutra = Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Isto e um passo da entrada no app, nao um alerta do sistema. Sem nada acima, o cartao
            // solto no preto lia como dialogo de erro do Android.
            Text(
                text = tr("ZELLU", "ZELLU"),
                color = accentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = tr("Preparando sua garagem", "Setting up your garage"),
                color = bodyColor.copy(alpha = 0.75f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                /*
                 * Crossfade entre os estados, e altura minima fixa no conteudo.
                 *
                 * Antes cada estado montava seu proprio bloco, com numero diferente de botoes: o
                 * cartao pulava de tamanho a cada transicao e o corte era seco. Com altura minima e
                 * transicao suave, a tela deixa de piscar enquanto procura e restaura.
                 */
                Crossfade(targetState = state, label = "estado_do_backup") { estadoAtual ->
                    when (estadoAtual) {
                        PostLoginBackupState.CHECKING -> ConteudoDeBackup(
                            icone = Icons.Default.CloudDownload,
                            corIcone = accentBlue,
                            carregando = true,
                            titulo = tr("Procurando backup no Drive", "Looking for a Drive backup"),
                            mensagem = tr(
                                "Só um segundo: estamos vendo se a sua conta já tem dados salvos para restaurar.",
                                "One moment: we're checking whether your account already has saved data to restore."
                            ),
                            titleColor = titleColor,
                            bodyColor = bodyColor,
                            accent = accentBlue
                        )

                        PostLoginBackupState.NEEDS_PERMISSION -> ConteudoDeBackup(
                            icone = Icons.Default.CloudDownload,
                            corIcone = accentBlue,
                            titulo = tr("Verificar backup no Drive", "Check your Drive backup"),
                            mensagem = tr(
                                "Para saber se a sua conta já tem backup, o Zellu precisa acessar a pasta segura do app no Google Drive.",
                                "To check whether your account already has a backup, Zellu needs access to the app's private folder on Google Drive."
                            ),
                            rotuloPrimario = tr("Verificar meu backup", "Check my backup"),
                            onPrimario = { driveLauncher.launch(googleSignInClient.signInIntent) },
                            rotuloSecundario = tr("Pular por agora", "Skip for now"),
                            onSecundario = onContinueWithoutRestore,
                            titleColor = titleColor,
                            bodyColor = bodyColor,
                            accent = accentBlue
                        )

                        PostLoginBackupState.FOUND -> ConteudoDeBackup(
                            icone = Icons.Default.CheckCircle,
                            corIcone = Color(0xFF34D399),
                            titulo = tr("Encontramos um backup", "We found a backup"),
                            mensagem = tr(
                                "Quer restaurar os dados do Drive agora? Se preferir não restaurar, você segue para as boas-vindas.",
                                "Restore your Drive data now? If you'd rather not, you'll go straight to the welcome flow."
                            ),
                            rotuloPrimario = tr("Restaurar backup", "Restore backup"),
                            onPrimario = { accountWithBackup?.let(::restoreBackup) },
                            rotuloSecundario = tr("Não, continuar sem restaurar", "No, continue without restoring"),
                            onSecundario = onContinueWithoutRestore,
                            titleColor = titleColor,
                            bodyColor = bodyColor,
                            accent = accentBlue
                        )

                        PostLoginBackupState.NOT_FOUND -> ConteudoDeBackup(
                            icone = Icons.Default.CloudOff,
                            corIcone = corNeutra,
                            titulo = tr("Nenhum backup encontrado", "No backup found"),
                            mensagem = tr(
                                "Não encontramos backup nessa conta do Google Drive. Vamos seguir para as boas-vindas.",
                                "We didn't find a backup in this Google Drive account. Let's continue to the welcome flow."
                            ),
                            rotuloPrimario = tr("Continuar", "Continue"),
                            onPrimario = onContinueWithoutRestore,
                            titleColor = titleColor,
                            bodyColor = bodyColor,
                            accent = accentBlue
                        )

                        PostLoginBackupState.RESTORING -> ConteudoDeBackup(
                            icone = Icons.Default.CloudDownload,
                            corIcone = accentBlue,
                            carregando = true,
                            titulo = tr("Restaurando backup", "Restoring backup"),
                            mensagem = tr(
                                "Estamos trazendo seus veículos, avisos e históricos do Drive.",
                                "We're bringing your vehicles, reminders and history back from Drive."
                            ),
                            // Sem botao de propósito: sair no meio da escrita deixaria a garagem
                            // pela metade.
                            titleColor = titleColor,
                            bodyColor = bodyColor,
                            accent = accentBlue
                        )

                        PostLoginBackupState.ERROR -> ConteudoDeBackup(
                            icone = Icons.Default.CloudOff,
                            corIcone = corErro,
                            titulo = tr("Não deu para verificar o backup", "We couldn't check the backup"),
                            mensagem = tr(
                                "Seus dados continuam salvos no Drive. Você pode tentar de novo depois, em Configurações.",
                                "Your data is still safe on Drive. You can try again later from Settings."
                            ),
                            detalheTecnico = errorMessage,
                            rotuloPrimario = tr("Continuar mesmo assim", "Continue anyway"),
                            onPrimario = onContinueWithoutRestore,
                            titleColor = titleColor,
                            bodyColor = bodyColor,
                            accent = accentBlue
                        )
                    }
                }
            }
        }
    }
}

/**
 * Corpo unico para os seis estados da tela.
 *
 * Antes cada estado repetia icone, titulo, texto e botoes com os mesmos modificadores — seis copias
 * que ja tinham divergido entre si em tamanho de fonte e rotulo. Aqui o estado vira dado e o
 * desenho e um so, entao ajustar o espacamento vale para todos de uma vez.
 */
@Composable
private fun ConteudoDeBackup(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    corIcone: Color,
    titulo: String,
    mensagem: String,
    titleColor: Color,
    bodyColor: Color,
    accent: Color,
    carregando: Boolean = false,
    detalheTecnico: String? = null,
    rotuloPrimario: String? = null,
    onPrimario: (() -> Unit)? = null,
    rotuloSecundario: String? = null,
    onSecundario: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Piso de altura: sem ele o cartao encolhia nos estados sem botao e crescia nos com
            // dois, e a transicao virava um solavanco.
            .heightIn(min = 292.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MedalhaoDeEstado(icone = icone, cor = corIcone, carregando = carregando)

        Spacer(Modifier.height(18.dp))
        Text(
            text = titulo,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = mensagem,
            color = bodyColor,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        if (!detalheTecnico.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            // Mensagem crua do Drive separada do texto humano: ajuda no suporte sem virar o recado
            // principal da tela.
            Text(
                text = detalheTecnico,
                color = bodyColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        if (rotuloPrimario != null && onPrimario != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPrimario,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(rotuloPrimario, color = Color(0xFF06111F), fontWeight = FontWeight.Bold)
            }
        }
        if (rotuloSecundario != null && onSecundario != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSecundario,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(rotuloSecundario, color = titleColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Icone dentro de um halo da propria cor do estado; enquanto carrega, um anel gira em volta.
 *
 * O spinner solto nao dizia o que estava acontecendo, e nos estados parados o icone flutuava sem
 * peso no cartao. Juntando os dois, a espera e o resultado passam a ter a mesma silhueta e a
 * transicao entre eles nao troca a composicao inteira.
 */
@Composable
private fun MedalhaoDeEstado(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    cor: Color,
    carregando: Boolean
) {
    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(cor.copy(alpha = 0.12f))
        )
        if (carregando) {
            CircularProgressIndicator(
                color = cor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(96.dp)
            )
        }
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(38.dp))
    }
}
