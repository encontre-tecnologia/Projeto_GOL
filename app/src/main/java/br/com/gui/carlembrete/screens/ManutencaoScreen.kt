package br.com.gui.carlembrete

import HistoricoAbastecimentoScreen
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Paint
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

// FunÃ§Ã£o utilitÃ¡ria para encontrar a Activity
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val lembreteUiDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private const val TAG_LOGIN_BACKUP_FLOW = "LoginBackupFlow"
private const val TAG_CORPORATE_AGENDA_ACCESS = "CorpAgendaAccess"

private fun corporateAgendaEmailKey(email: String): String =
    email.trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9._-]"), "_")

private fun isCorporateAgendaCompanyId(companyId: String?, userUid: String): Boolean {
    val id = companyId.orEmpty()
    return id.isNotBlank() && id != "personal_$userUid"
}

private fun compartilharPdfsDaFrota(context: Context, uris: List<Uri>): Boolean {
    if (uris.isEmpty()) return false

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    return runCatching {
        context.startActivity(Intent.createChooser(intent, "Compartilhar relatorios da frota"))
    }.isSuccess
}

/* ----------------- TELA PRINCIPAL (Visual Dashboard Premium) ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManutencaoScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    openAondePareiOnStart: Boolean = false,
    onAondePareiStartConsumed: () -> Unit = {},
    openVehicleImportUriOnStart: Uri? = null,
    onVehicleImportStartConsumed: () -> Unit = {},
    openReminderIdOnStart: String? = null,
    openReminderCarIdOnStart: String? = null,
    onReminderStartConsumed: () -> Unit = {},
    onLoaded: () -> Unit = {},
    onEmptyVehicleData: () -> Unit = {},
    onThemeModeChanged: (AppThemeMode) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    // ----------------- ESTADOS E VARIÃVEIS -----------------
    var listaCarros by remember { mutableStateOf<List<CarroInfo>>(emptyList()) }
    var listaContatos by remember { mutableStateOf<List<ContatoProfissional>>(emptyList()) }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var pedaladas by remember { mutableStateOf<List<Pedalada>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var notifiedLoaded by remember { mutableStateOf(false) }

    // CORES DO TEMA (Azul Premium)
    val primaryDark = if (isDark) Color.Black else colorScheme.background
    val surfaceDark = if (isDark) Color(0xFF111827) else colorScheme.surface
    val homeScreenBg = if (isDark) Color.Black else colorScheme.background
    val fuelCardStart = if (isDark) Color(0xFF111827) else colorScheme.surface
    val fuelCardEnd = if (isDark) Color(0xFF111827) else colorScheme.background
    val topBarDark = if (isDark) Color.Black else colorScheme.background
    val accentBlue = colorScheme.primary
    val textLight = colorScheme.onSurface
    val textDim = colorScheme.onSurfaceVariant
    val drawerItemBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        colorScheme.outlineVariant.copy(alpha = 0.9f)
    }

    // ----------------- CARREGAMENTO DE DADOS -----------------
    LaunchedEffect(Unit) {
        Log.d(TAG_LOGIN_BACKUP_FLOW, "ManutencaoScreen load start")
        withContext(Dispatchers.IO) {
            val nomeUsuarioLogado = FirebaseAuth.getInstance().currentUser?.displayName
                ?.trim()
                ?.split("\\s+".toRegex())
                ?.filter { it.isNotBlank() }
                ?.let { partes ->
                    when {
                        partes.isEmpty() -> null
                        partes.size == 1 -> partes.first()
                        else -> "${partes.first()} ${partes.last()}"
                    }
                }

            val carrosOriginais = BancoDeDados.carregarCarros(context).orEmpty()
            Log.d(
                TAG_LOGIN_BACKUP_FLOW,
                "ManutencaoScreen loaded raw carros=${carrosOriginais.size} uid=${FirebaseAuth.getInstance().currentUser?.uid ?: "null"}"
            )
            val carros = if (!nomeUsuarioLogado.isNullOrBlank()) {
                carrosOriginais.map { carro ->
                    if (carro.proprietario.equals("Eu mesmo", ignoreCase = true)) {
                        carro.copy(proprietario = nomeUsuarioLogado)
                    } else {
                        carro
                    }
                }
            } else {
                carrosOriginais
            }
            val contatos = BancoDeDados.carregarContatos(context)
            val lembretes = BancoDeDados.carregarLembretes(context)
            val abastecimentosDb = BancoDeDados.carregarAbastecimentos(context)
            val pedaladasDb = BancoDeDados.carregarPedaladas(context)
            Log.d(
                TAG_LOGIN_BACKUP_FLOW,
                "ManutencaoScreen loaded mapped carros=${carros.size} contatos=${contatos.size} " +
                    "lembretes=${lembretes.size} abastecimentos=${abastecimentosDb.size} pedaladas=${pedaladasDb.size}"
            )
            val lembretesPendentes = lembretes.filterNot(::isLembreteRealizado)
            withContext(Dispatchers.Main) {
                listaCarros = carros
                listaContatos = contatos
                todosLembretes = lembretes
                abastecimentos = abastecimentosDb
                pedaladas = pedaladasDb
                isLoading = false
                // Reagendar notificacoes fora da thread principal para nao travar a animacao de loading.
                launch(Dispatchers.IO) {
                    NotificacaoHelper.reagendarExistentes(
                        context.applicationContext,
                        lembretesPendentes
                    )
                }
            }
        }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading && !notifiedLoaded) {
            notifiedLoaded = true
            onLoaded()
        }
    }
    LaunchedEffect(isLoading, listaCarros, openVehicleImportUriOnStart) {
        if (!isLoading && listaCarros.isEmpty() && openVehicleImportUriOnStart == null) {
            Log.w(TAG_LOGIN_BACKUP_FLOW, "ManutencaoScreen empty vehicles after load -> onEmptyVehicleData")
            onEmptyVehicleData()
        }
    }

    // PersistÃªncia automÃ¡tica ao alterar dados
    LaunchedEffect(listaCarros) {
        if (!isLoading) {
            Log.d(TAG_LOGIN_BACKUP_FLOW, "ManutencaoScreen autosave carros=${listaCarros.size}")
            withContext(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaCarros) }
            AdminUsersSync.syncVehicles(listaCarros)
        }
    }
    LaunchedEffect(listaContatos) { if (!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarContatos(context, listaContatos) } }
    LaunchedEffect(todosLembretes) {
        if (!isLoading) {
            withContext(Dispatchers.IO) { BancoDeDados.salvarLembretes(context, todosLembretes) }
            AdminUsersSync.syncRemindersSnapshot(todosLembretes)
        }
    }
    LaunchedEffect(abastecimentos) {
        if (!isLoading) {
            withContext(Dispatchers.IO) { BancoDeDados.salvarAbastecimentos(context, abastecimentos) }
        }
    }

    var indiceCarroAtual by remember { mutableIntStateOf(0) }
    var restoredLastCar by remember { mutableStateOf(false) }
    val carroAtual = if (listaCarros.isNotEmpty()) {
        if (indiceCarroAtual >= listaCarros.size) indiceCarroAtual = 0
        listaCarros[indiceCarroAtual]
    } else {
        CarroInfo()
    }

    LaunchedEffect(isLoading, listaCarros, restoredLastCar) {
        if (!isLoading && listaCarros.isNotEmpty() && !restoredLastCar) {
            val lastCarId = AppPreferences.getLastSelectedCarId(context)
            val savedIndex = lastCarId?.let { id -> listaCarros.indexOfFirst { it.id == id } } ?: -1
            if (savedIndex >= 0) {
                indiceCarroAtual = savedIndex
            }
            restoredLastCar = true
        }
    }

    LaunchedEffect(isLoading, carroAtual.id) {
        if (!isLoading && listaCarros.isNotEmpty()) {
            AppPreferences.setLastSelectedCarId(context, carroAtual.id)
        }
    }

    // Estados de Controle de Interface
    var showEditCarScreen by remember { mutableStateOf(false) }
    var showAddCarScreen by remember { mutableStateOf(false) }
    var showAddLembreteDialog by remember { mutableStateOf(false) }
    var showFluxoCadastroDialog by remember { mutableStateOf(false) }
    var fluxoInicialRegistroServico by remember { mutableStateOf<Boolean?>(null) }
    var showTipoAvisoDialog by remember { mutableStateOf(false) }
    var tipoAvisoSelecionado by remember { mutableStateOf(TipoManutencao.OLEO) }
    var iniciarCameraProduto by remember { mutableStateOf(false) }
    var showSelecionarPrestadorScreen by remember { mutableStateOf(false) }
    var lembreteParaVincularContato by remember { mutableStateOf<String?>(null) }
    var showTesteNotificacaoDialog by remember { mutableStateOf(false) }
    var showConfiguracoes by remember { mutableStateOf(false) }
    var showTermsScreen by remember { mutableStateOf(false) }
    var showPrivacyScreen by remember { mutableStateOf(false) }
    var showMecanicoVirtualScreen by remember { mutableStateOf(false) }
    var showHistoricoAbastecimentoScreen by remember { mutableStateOf(false) }
    var showBikeDistanceRegister by remember { mutableStateOf(false) }
    var showBikeDistanceHistory by remember { mutableStateOf(false) }
    var showPremiumHubScreen by remember { mutableStateOf(false) }
    var showCorporateAgendaScreen by remember { mutableStateOf(false) }
    var corporateAgendaCompanyId by remember { mutableStateOf<String?>(null) }
    var corporateAgendaAccessLoading by remember { mutableStateOf(true) }
    var showPerfilScreen by remember { mutableStateOf(false) }
    var showAondePareiScreen by remember { mutableStateOf(openAondePareiOnStart) }
    var showVehicleAiChatScreen by remember { mutableStateOf(false) }
    var returnToPremiumBenefitsAfterAi by remember { mutableStateOf(false) }
    var showAiAssistantScreen by remember { mutableStateOf(false) }
    var showFleetOverviewScreen by remember { mutableStateOf(false) }
    var showFleetStockScreen by remember { mutableStateOf(false) }
    var showVehicleGuideScreen by remember { mutableStateOf(false) }
    var showEbookStoreScreen by remember { mutableStateOf(false) }
    var showReleaseNotesScreen by remember { mutableStateOf(false) }
    var showReportMiniTutorial by rememberSaveable { mutableStateOf(false) }
    
    val density = LocalDensity.current
    var showHomeTutorial by remember { mutableStateOf(false) }
    var homeTutorialStep by remember { mutableIntStateOf(0) }
    var homeTutorialAutoStarted by rememberSaveable { mutableStateOf(false) }
    var menuButtonRect by remember { mutableStateOf<Rect?>(null) }
    var helpButtonRect by remember { mutableStateOf<Rect?>(null) }
    var notificationsButtonRect by remember { mutableStateOf<Rect?>(null) }
    var premiumButtonRect by remember { mutableStateOf<Rect?>(null) }
    var fuelHistoryButtonRect by remember { mutableStateOf<Rect?>(null) }
    var carInfoRect by remember { mutableStateOf<Rect?>(null) }
    var editCarButtonRect by remember { mutableStateOf<Rect?>(null) }
    var reportButtonRect by remember { mutableStateOf<Rect?>(null) }
    var newReminderButtonRect by remember { mutableStateOf<Rect?>(null) }
    var remindersRect by remember { mutableStateOf<Rect?>(null) }
    var tutorialViewportHeightPx by remember { mutableFloatStateOf(0f) }

    var authUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            authUser = firebaseAuth.currentUser
            Log.d(
                TAG_CORPORATE_AGENDA_ACCESS,
                "auth changed uid=${firebaseAuth.currentUser?.uid ?: "null"} email=${firebaseAuth.currentUser?.email ?: "null"}"
            )
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    DisposableEffect(authUser?.uid, authUser?.email) {
        val user = authUser
        if (user == null) {
            Log.d(TAG_CORPORATE_AGENDA_ACCESS, "no auth user; hiding agenda")
            corporateAgendaCompanyId = null
            corporateAgendaAccessLoading = false
            return@DisposableEffect onDispose { }
        }

        val db = FirebaseFirestore.getInstance()
        val registrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val normalizedEmail = user.email.orEmpty().trim().lowercase(Locale.getDefault())
        val emailKey = corporateAgendaEmailKey(normalizedEmail)
        Log.d(
            TAG_CORPORATE_AGENDA_ACCESS,
            "start access listeners uid=${user.uid} email=$normalizedEmail emailKey=$emailKey"
        )
        var userCompanyId: String? = null
        var inviteCompanyId: String? = null
        var activeMemberRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        fun publishAccess() {
            corporateAgendaCompanyId = inviteCompanyId ?: userCompanyId
            corporateAgendaAccessLoading = false
            Log.d(
                TAG_CORPORATE_AGENDA_ACCESS,
                "publish userCompanyId=$userCompanyId userInvites=$inviteCompanyId final=${corporateAgendaCompanyId ?: "null"}"
            )
        }

        registrations += db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG_CORPORATE_AGENDA_ACCESS, "users/${user.uid} listener error", error)
                }
                val activeCompanyId = snapshot
                    ?.getString("activeCompanyId")
                    ?.takeIf { isCorporateAgendaCompanyId(it, user.uid) }
                activeMemberRegistration?.remove()
                activeMemberRegistration = null
                userCompanyId = null
                Log.d(
                    TAG_CORPORATE_AGENDA_ACCESS,
                    "users activeCompanyId raw=${snapshot?.getString("activeCompanyId") ?: "null"} candidate=${activeCompanyId ?: "null"} exists=${snapshot?.exists()}"
                )
                if (activeCompanyId != null) {
                    activeMemberRegistration = db.collection("companies")
                        .document(activeCompanyId)
                        .collection("members")
                        .document(user.uid)
                        .addSnapshotListener { memberSnapshot, memberError ->
                            if (memberError != null) {
                                Log.w(TAG_CORPORATE_AGENDA_ACCESS, "companies/$activeCompanyId/members/${user.uid} listener error", memberError)
                            }
                            val memberActive = memberSnapshot?.exists() == true && memberSnapshot.getBoolean("active") != false
                            userCompanyId = activeCompanyId.takeIf { memberActive }
                            Log.d(
                                TAG_CORPORATE_AGENDA_ACCESS,
                                "active member company=$activeCompanyId exists=${memberSnapshot?.exists()} active=$memberActive accepted=${userCompanyId ?: "null"}"
                            )
                            publishAccess()
                        }
                }
                publishAccess()
            }

        if (emailKey.isNotBlank()) {
            registrations += db.collection("userInvites")
                .document(emailKey)
                .collection("companies")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG_CORPORATE_AGENDA_ACCESS, "userInvites/$emailKey/companies listener error", error)
                    }
                    inviteCompanyId = snapshot
                        ?.documents
                        ?.firstOrNull()
                        ?.getString("companyId")
                        ?.takeIf { isCorporateAgendaCompanyId(it, user.uid) }
                    Log.d(
                        TAG_CORPORATE_AGENDA_ACCESS,
                        "userInvites docs=${snapshot?.size() ?: -1} accepted=${inviteCompanyId ?: "null"} firstPath=${snapshot?.documents?.firstOrNull()?.reference?.path ?: "null"}"
                    )
                    publishAccess()
                }
        } else {
            Log.d(TAG_CORPORATE_AGENDA_ACCESS, "blank emailKey; cannot check email invite")
            publishAccess()
        }

        onDispose {
            registrations.forEach { it.remove() }
            activeMemberRegistration?.remove()
        }
    }

    LaunchedEffect(corporateAgendaCompanyId) {
        if (corporateAgendaCompanyId.isNullOrBlank()) {
            showCorporateAgendaScreen = false
        }
    }

    LaunchedEffect(openAondePareiOnStart) {
        if (openAondePareiOnStart) {
            showAondePareiScreen = true
            onAondePareiStartConsumed()
        }
    }
    LaunchedEffect(openVehicleImportUriOnStart, isLoading) {
        val uri = openVehicleImportUriOnStart ?: return@LaunchedEffect
        if (isLoading) return@LaunchedEffect

        val imported = importVehicleTransferFromUri(context, uri)
        onVehicleImportStartConsumed()
        if (imported == null) {
            Toast.makeText(context, "Nao consegui importar esse veiculo.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        val carrosAtualizados = listaCarros + imported.vehicle
        val lembretesAtualizados = todosLembretes + imported.reminders
        val abastecimentosAtualizados = abastecimentos + imported.fuelRecords
        listaCarros = carrosAtualizados
        todosLembretes = lembretesAtualizados
        abastecimentos = abastecimentosAtualizados
        indiceCarroAtual = carrosAtualizados.lastIndex

        withContext(Dispatchers.IO) {
            BancoDeDados.salvarCarros(context, carrosAtualizados)
            BancoDeDados.salvarLembretes(context, lembretesAtualizados)
            BancoDeDados.salvarAbastecimentos(context, abastecimentosAtualizados)
            NotificacaoHelper.reagendarExistentes(
                context.applicationContext,
                lembretesAtualizados.filterNot(::isLembreteRealizado)
            )
        }
        Toast.makeText(context, "Veiculo importado: ${imported.vehicle.nome}", Toast.LENGTH_LONG).show()
    }
    val shouldAutoStartTutorial = remember(context) { shouldAutoStartHomeTutorial(context) }
    val homeTutorialSteps = remember(carroAtual.tipoVeiculo, premiumButtonRect) {
        buildList {
            add("Toque aqui para abrir o menu principal do app e acessar todas as seções rapidamente." to "menu")
            if (premiumButtonRect != null) {
                add("Aqui fica a área Premium, onde você ativa e usa os recursos avançados da sua conta." to "premium")
            }
            add("No sino você acompanha as notificações enviadas e o histórico dos avisos recentes." to "notifications")
            add("Este card mostra seu veículo atual. Use os botões para editar informações e criar lembretes." to "car")
            add("Botão Editar: toque aqui para atualizar os dados do veículo, como nome, modelo e outras informações." to "edit_car")
            add("Botão Relatório: toque aqui para abrir os detalhes e relatórios do veículo selecionado." to "report")
            add("Botão Novo Lembrete: toque aqui para criar um novo aviso de manutenção para este veículo." to "new_reminder")
            add("Aqui estão as categorias dos lembretes. Toque em uma categoria para ver os avisos desse tipo." to "reminders")
            add("Precisa rever o passo a passo depois? Toque aqui para abrir o tutorial novamente." to "help")
        }
    }
    fun normalizedTutorialRect(key: String, rect: Rect?): Rect? {
        if (rect == null) return null
        return when (key) {
            "car" -> {
                val horizontalInset = with(density) { 16.dp.toPx() }
                val verticalInset = with(density) { 8.dp.toPx() }
                Rect(
                    left = rect.left + horizontalInset,
                    top = rect.top + verticalInset,
                    right = rect.right - horizontalInset,
                    bottom = rect.bottom - verticalInset
                )
            }
            else -> rect
        }
    }

    var showAnjoDaGuardaScreen by remember { mutableStateOf(false) }
    var showTravelAlarmScreen by remember { mutableStateOf(false) }
    var showGaragemScreen by remember { mutableStateOf(false) }
    var showCarInfoScreen by remember { mutableStateOf(false) }
    var lembreteSelecionado by remember { mutableStateOf<Lembrete?>(null) }
    var showLembreteDetalhesScreen by remember { mutableStateOf(false) }
    var contatoDetalheSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    LaunchedEffect(openReminderIdOnStart, isLoading, listaCarros, todosLembretes) {
        val alvo = openReminderIdOnStart.orEmpty()
        if (alvo.isBlank() || isLoading) return@LaunchedEffect

        if (alvo.startsWith("PARKING_")) {
            openReminderCarIdOnStart?.let { carroId ->
                val idx = listaCarros.indexOfFirst { it.id == carroId }
                if (idx >= 0) indiceCarroAtual = idx
            }
            showAondePareiScreen = true
            onReminderStartConsumed()
            return@LaunchedEffect
        }

        val lembrete = todosLembretes.firstOrNull { it.id == alvo }
            ?: todosLembretes.firstOrNull { alvo.startsWith("${it.id}_") }

        if (lembrete != null) {
            val idx = listaCarros.indexOfFirst { it.id == lembrete.carroId }
            if (idx >= 0) indiceCarroAtual = idx
            lembreteSelecionado = lembrete
            contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
            showLembreteDetalhesScreen = true
        }
        onReminderStartConsumed()
    }
    val canStartHomeTutorial by remember(
        isLoading,
        tutorialViewportHeightPx,
        menuButtonRect,
        helpButtonRect,
        notificationsButtonRect,
        carInfoRect,
        editCarButtonRect,
        reportButtonRect,
        newReminderButtonRect,
        remindersRect
    ) {
        derivedStateOf {
            !isLoading &&
                tutorialViewportHeightPx > 0f &&
                menuButtonRect != null &&
                helpButtonRect != null &&
                notificationsButtonRect != null &&
                carInfoRect != null &&
                editCarButtonRect != null &&
                reportButtonRect != null &&
                newReminderButtonRect != null &&
                remindersRect != null
        }
    }

    LaunchedEffect(shouldAutoStartTutorial, canStartHomeTutorial, showHomeTutorial, homeTutorialSteps, homeTutorialAutoStarted) {
        if (
            shouldAutoStartTutorial &&
            canStartHomeTutorial &&
            !showHomeTutorial &&
            !homeTutorialAutoStarted &&
            homeTutorialSteps.isNotEmpty()
        ) {
            homeTutorialStep = 0
            showHomeTutorial = true
            homeTutorialAutoStarted = true
        }
    }
    var filtroTipo by remember { mutableStateOf<TipoManutencao?>(null) }
    var buscaTexto by remember { mutableStateOf("") }

    val lembretesDoCarroAtual = todosLembretes.filter { it.carroId == carroAtual.id }
    val lembretesAtivosDoCarroAtual = lembretesDoCarroAtual
        .filterNot(::isLembreteRealizado)
        .filter { it.tipo != TipoManutencao.ABASTECIMENTO }
    val lembretesFiltrados = if (filtroTipo == null) {
        lembretesAtivosDoCarroAtual
    } else {
        lembretesAtivosDoCarroAtual.filter { it.tipo == filtroTipo }
    }
    val lembretesComBusca = if (buscaTexto.isBlank()) {
        lembretesFiltrados
    } else {
        lembretesFiltrados.filter { lembrete ->
            lembrete.titulo.contains(buscaTexto, ignoreCase = true) ||
                    lembrete.peca.contains(buscaTexto, ignoreCase = true)
        }
    }
    val totalGastos = lembretesAtivosDoCarroAtual.sumOf { it.valor }

    // Gasto por mes do veiculo em foco. Usa servico realizado (nao aviso pendente) e
    // abastecimento, que sao as duas despesas que o app efetivamente registra.
    val gastosMensaisDoCarro = remember(lembretesDoCarroAtual, abastecimentos, carroAtual.id) {
        calcularGastosMensais(
            lembretes = lembretesDoCarroAtual,
            abastecimentos = abastecimentos.filter { it.carroId == carroAtual.id }
        )
    }

    // Avisos do veiculo em foco, do mais urgente para o menos. Alimenta o resumo da
    // home e a tela completa, para as duas nunca discordarem da ordem.
    val avisosOrdenadosDoCarro = remember(lembretesAtivosDoCarroAtual) {
        lembretesAtivosDoCarroAtual.sortedBy { dataParaOrdenacao(it) }
    }
    val registrosOrdenadosDoCarro = remember(lembretesDoCarroAtual) {
        lembretesDoCarroAtual
            .filter(::isLembreteRealizado)
            .filter { it.tipo != TipoManutencao.ABASTECIMENTO }
            .sortedByDescending { dataRealizacaoLembrete(it) ?: dataParaOrdenacao(it) }
    }
    /** Vencidos e chegando (ate 30 dias) do veiculo em foco, para o topo da home. */
    val resumoDoCarroAtual = remember(lembretesAtivosDoCarroAtual) {
        val hoje = java.time.LocalDate.now()
        var vencidos = 0
        var chegando = 0
        lembretesAtivosDoCarroAtual.forEach { aviso ->
            val data = dataParaOrdenacao(aviso)
            if (data == java.time.LocalDate.MAX) return@forEach
            val dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, data)
            if (dias < 0) vencidos++ else if (dias <= 30) chegando++
        }
        vencidos to chegando
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val contentScrollState = rememberScrollState()
    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }
    LaunchedEffect(showReportMiniTutorial) {
        if (showReportMiniTutorial && contentScrollState.value > 0) {
            contentScrollState.animateScrollTo(0)
        }
    }
    LaunchedEffect(showHomeTutorial, homeTutorialStep, remindersRect, tutorialViewportHeightPx) {
        if (!showHomeTutorial || homeTutorialSteps.isEmpty()) return@LaunchedEffect
        val safeStep = homeTutorialStep.coerceIn(0, homeTutorialSteps.lastIndex)
        val targetKey = homeTutorialSteps[safeStep].second
        if (targetKey == "menu" || targetKey == "help" || targetKey == "premium" || targetKey == "notifications" || targetKey == "car" || targetKey == "edit_car" || targetKey == "report" || targetKey == "new_reminder") {
            if (contentScrollState.value > 0) {
                contentScrollState.animateScrollTo(0)
            }
            return@LaunchedEffect
        }
        if (targetKey != "reminders") return@LaunchedEffect
        val rect = remindersRect ?: return@LaunchedEffect
        if (tutorialViewportHeightPx <= 0f) return@LaunchedEffect

        val reservedBottomSpace = with(density) { 240.dp.toPx() }
        val desiredBottom = (tutorialViewportHeightPx - reservedBottomSpace).coerceAtLeast(with(density) { 220.dp.toPx() })
        val extraPadding = with(density) { 20.dp.toPx() }
        val delta = (rect.bottom - desiredBottom + extraPadding).coerceAtLeast(0f)
        if (delta > 1f) {
            val targetValue = (contentScrollState.value + delta).toInt().coerceAtMost(contentScrollState.maxValue)
            contentScrollState.animateScrollTo(targetValue)
        }
    }
    val activity = remember(context) { context.findActivity() }
    val subscriptionManager = remember { SubscriptionManager(context) }
    val planTier by subscriptionManager.planTier.collectAsState()
    val isSubscribed by subscriptionManager.isSubscribed.collectAsState()
    val subscriptionBillingInfo by subscriptionManager.billingInfo.collectAsState()
    // Preco sempre do Google Play: nenhum valor escrito nesta tela.
    val playPlanPrices by PlayPlanPrices.pricesByProductId.collectAsState()
    val aiFeatureChannel = remember { AdminUsersSync.getFeatureChannel(context, "ai") }
    val userChannelForAi = remember { AdminUsersSync.getChannelStatus(context) }
    val isAiBlocked = remember { AdminUsersSync.getCachedAiBlocked(context) }
    var isWebBlocked by remember { mutableStateOf(AdminUsersSync.getCachedWebBlocked(context)) }
    val hasVehicleAiAccess = planTier in setOf(PlanTier.LITE, PlanTier.FROTA, PlanTier.ENTERPRISE) &&
        (aiFeatureChannel != "beta" || userChannelForAi == "beta") &&
        !isAiBlocked
    var featureChannelVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        AdminUsersSync.syncFeatureChannels(context) { featureChannelVersion++ }
        AdminUsersSync.syncUserConfig(context) {
            isWebBlocked = AdminUsersSync.getCachedWebBlocked(context)
        }
    }
    fun featureAllowed(key: String): Boolean {
        @Suppress("UNUSED_EXPRESSION")
        featureChannelVersion
        val ch = AdminUsersSync.getFeatureChannel(context, key)
        return ch != "beta" || userChannelForAi == "beta"
    }
    val maxVehiclesCurrentPlan by remember(planTier) { mutableIntStateOf(vehicleLimitForPlan(planTier)) }
    val maxRemindersCurrentPlan by remember(planTier) { mutableIntStateOf(reminderLimitForPlan(planTier)) }
    val planNameCurrent by remember(planTier) { mutableStateOf(planNameLabel(planTier)) }

    // Arquivo da foto do veiculo em foco, resolvido uma vez por troca de carro/foto.
    val fotoDoCarroAtual = remember(indiceCarroAtual, listaCarros) {
        VehiclePhotoStore.arquivoDe(context, listaCarros.getOrNull(indiceCarroAtual)?.fotoNome)
    }

    // O limite de avisos do plano era só exibido no Perfil e nunca aplicado. Serviços
    // já realizados e abastecimentos não contam, igual ao contador daquela tela.
    fun podeCriarAvisos(novos: List<Lembrete>): Boolean = canCreateReminders(
        planTier = planTier,
        atuais = countRemindersForPlanLimit(todosLembretes),
        novos = countRemindersForPlanLimit(novos)
    )

    fun avisarLimiteDeAvisos() {
        Toast.makeText(
            context,
            "Limite do plano $planNameCurrent: $maxRemindersCurrentPlan avisos ativos.",
            Toast.LENGTH_LONG
        ).show()
    }
    var showAvisosCompletosScreen by remember { mutableStateOf(false) }

    // Foto do veiculo: escolhida da galeria, copiada para filesDir e referenciada por
    // nome no CarroInfo. A troca apaga a anterior para nao acumular arquivo orfao.
    val escolherFotoDoVeiculo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val alvo = listaCarros.getOrNull(indiceCarroAtual) ?: return@rememberLauncherForActivityResult
        val novoNome = VehiclePhotoStore.salvar(context, uri, alvo.id)
        if (novoNome == null) {
            Toast.makeText(context, trNow("Nao foi possivel usar essa imagem.", "Could not use that image."), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        VehiclePhotoStore.apagar(context, alvo.fotoNome)
        listaCarros = listaCarros.map { if (it.id == alvo.id) it.copy(fotoNome = novoNome) else it }
    }

    var showPremiumDialog by remember { mutableStateOf(false) }
    var showPremiumInfo by remember { mutableStateOf(false) }
    var showPremiumBeneficiosScreen by remember { mutableStateOf(false) }
    var showAvisosNotificacoesDialog by remember { mutableStateOf(false) }
    var notificacoesDisparadas by remember { mutableStateOf<List<NotificacaoDisparada>>(emptyList()) }
    fun closeVehicleAiChat() {
        showVehicleAiChatScreen = false
        if (returnToPremiumBenefitsAfterAi) {
            returnToPremiumBenefitsAfterAi = false
            showPremiumHubScreen = true
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val refreshNotificacoes = remember(context) {
        {
            notificacoesDisparadas = NotificacaoHelper.carregarNotificacoesDisparadas(context)
        }
    }

    DisposableEffect(Unit) {
        subscriptionManager.connect()
        // Aplica override do cache (salvo por syncUserConfig no login) — zero reads extras.
        val cachedOverride = AdminUsersSync.getCachedAdminPremiumOverride(context)
        val cachedPlan = AdminUsersSync.getCachedAdminPremiumPlan(context)
        if (cachedOverride != SubscriptionManager.isAdminPremiumOverrideEnabled(context)) {
            SubscriptionManager.setAdminPremiumOverride(context, cachedOverride)
            SubscriptionManager.setAdminPremiumOverridePlan(context, cachedPlan)
            subscriptionManager.refreshLocalEntitlements()
        } else if (cachedOverride) {
            SubscriptionManager.setAdminPremiumOverridePlan(context, cachedPlan)
        }
        onDispose { subscriptionManager.disconnect() }
    }
    LaunchedEffect(todosLembretes) {
        notificacoesDisparadas = withContext(Dispatchers.IO) { NotificacaoHelper.carregarNotificacoesDisparadas(context) }
    }
    DisposableEffect(context) {
        val listener = NotificacaoHelper.registrarListenerHistorico(context) {
            refreshNotificacoes()
        }
        onDispose { NotificacaoHelper.removerListenerHistorico(context, listener) }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshNotificacoes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ----------------- TELAS DE VEÃCULO -----------------
    BackHandler(enabled = showEditCarScreen) { showEditCarScreen = false }
    if (showEditCarScreen) {
        EditarCarroScreen(
            carroAtual = carroAtual,
            onDismiss = { showEditCarScreen = false },
            onSalvar = { carroEditado ->
                listaCarros = listaCarros.map { if (it.id == carroAtual.id) carroEditado else it }
                showEditCarScreen = false
            },
            onExcluir = {
                if (listaCarros.size <= 1) {
                    Toast.makeText(context, "Mantenha ao menos um veiculo cadastrado.", Toast.LENGTH_SHORT).show()
                } else {
                    val indiceAtual = indiceCarroAtual
                    listaCarros = listaCarros.filterNot { it.id == carroAtual.id }
                    indiceCarroAtual = (indiceAtual - 1).coerceAtLeast(0).coerceAtMost((listaCarros.size - 1).coerceAtLeast(0))
                    showEditCarScreen = false
                }
            }
        )
        return
    }
    BackHandler(enabled = showAddCarScreen) { showAddCarScreen = false }
    if (showAddCarScreen) {
        NovoCarroScreenPrimeiroFluxoComVoltar(
            onDismiss = { showAddCarScreen = false },
            onSalvar = { novoCarro ->
                if (listaCarros.size >= maxVehiclesCurrentPlan) {
                    Toast.makeText(
                        context,
                        "Limite do plano $planNameCurrent: $maxVehiclesCurrentPlan veiculos.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@NovoCarroScreenPrimeiroFluxoComVoltar
                }
                listaCarros = listaCarros + novoCarro
                indiceCarroAtual = listaCarros.lastIndex
                showAddCarScreen = false
            }
        )
        return
    }
    BackHandler(enabled = showSelecionarPrestadorScreen) {
        showSelecionarPrestadorScreen = false
        lembreteParaVincularContato = null
    }
    if (showSelecionarPrestadorScreen) {
        val lembreteAlvo = lembreteParaVincularContato?.let { alvoId ->
            todosLembretes.find { it.id == alvoId }
        }
        if (lembreteAlvo == null) {
            showSelecionarPrestadorScreen = false
            lembreteParaVincularContato = null
        } else {
            SelecionarPrestadorScreen(
                tipoSelecionado = lembreteAlvo.tipo,
                isBikeVehicle = isBikeCategory(carroAtual.tipoVeiculo),
                prestadoresCadastrados = listaContatos,
                onDismiss = {
                    showSelecionarPrestadorScreen = false
                    lembreteParaVincularContato = null
                },
                onConfirmar = { novoContato ->
                    val indiceContatoExistente = listaContatos.indexOfFirst { contatoExistente ->
                        contatoExistente.id == novoContato.id ||
                            (contatoExistente.nome.equals(novoContato.nome, ignoreCase = true) &&
                                contatoExistente.tipoServico == novoContato.tipoServico)
                    }
                    val contatoVinculado = if (indiceContatoExistente >= 0) {
                        val contatoExistente = listaContatos[indiceContatoExistente]
                        val contatoAtualizado = if (contatoExistente.telefone.isBlank() && novoContato.telefone.isNotBlank()) {
                            contatoExistente.copy(telefone = novoContato.telefone)
                        } else {
                            contatoExistente
                        }
                        listaContatos = listaContatos.toMutableList().also { lista ->
                            lista[indiceContatoExistente] = contatoAtualizado
                        }
                        contatoAtualizado
                    } else {
                        listaContatos = listaContatos + novoContato
                        novoContato
                    }

                    todosLembretes = todosLembretes.map { lembrete ->
                        if (lembrete.id == lembreteAlvo.id) lembrete.copy(contatoId = contatoVinculado.id) else lembrete
                    }
                    lembreteSelecionado = todosLembretes.find { it.id == lembreteAlvo.id }
                    contatoDetalheSelecionado = contatoVinculado
                    lembreteParaVincularContato = null
                    showSelecionarPrestadorScreen = false
                }
            )
        }
        return
    }
    BackHandler(enabled = showTipoAvisoDialog) { showTipoAvisoDialog = false }
    BackHandler(enabled = showHomeTutorial) {}
    if (showFluxoCadastroDialog) {
        Dialog(
            onDismissRequest = { showFluxoCadastroDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val popupBg = if (isDark) Color.Black else Color.White
            val popupBorder = if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0)
            val popupTextPrimary = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
            val popupTextSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

            @Composable
            fun FluxoOptionCard(
                icon: ImageVector,
                title: String,
                subtitle: String,
                accent: Color,
                onClick: () -> Unit
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0B1220) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, popupBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.15f))
                                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = popupTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                color = popupTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = popupTextSecondary
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = popupBg),
                border = BorderStroke(1.dp, popupBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.14f))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Novo aviso",
                            color = popupTextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Como voce quer cadastrar?",
                            color = popupTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    FluxoOptionCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Ja aconteceu",
                        subtitle = "Registrar servico concluido no historico (sem lembrete futuro).",
                        accent = Color(0xFF10B981),
                        onClick = {
                            fluxoInicialRegistroServico = true
                            showFluxoCadastroDialog = false
                            showTipoAvisoDialog = true
                        }
                    )

                    FluxoOptionCard(
                        icon = Icons.Default.Schedule,
                        title = "Vai acontecer",
                        subtitle = "Criar um lembrete com data para o app te avisar no momento certo.",
                        accent = Color(0xFF3B82F6),
                        onClick = {
                            fluxoInicialRegistroServico = false
                            showFluxoCadastroDialog = false
                            showTipoAvisoDialog = true
                        }
                    )

                    OutlinedButton(
                        onClick = { showFluxoCadastroDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, popupBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = popupTextSecondary
                        )
                    ) {
                        Text("Fechar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    if (showTipoAvisoDialog) {
        val isBike = isBikeCategory(carroAtual.tipoVeiculo)
        val tiposAviso = tiposAvisoPorVeiculo(carroAtual.tipoVeiculo)
            .filterNot { tipo ->
                fluxoInicialRegistroServico == false && tipo == TipoManutencao.ABASTECIMENTO
            }
        val itensEspeciais = buildList {
            add(AvisoItem(
                label = tr("Lembrar aonde parei", "Remember where I parked"),
                icon = Icons.Default.LocalParking,
                color = accentBlue,
                textIcon = "E",
                wide = true
            ) {
                showTipoAvisoDialog = false
                showAondePareiScreen = true
            })
            if (fluxoInicialRegistroServico == false) {
                add(AvisoItem(
                    label = tr("Me acorde ao chegar", "Wake me when I arrive"),
                    icon = Icons.Rounded.Alarm,
                    color = Color(0xFF0D9488),
                    wide = true
                ) {
                    showTipoAvisoDialog = false
                    showTravelAlarmScreen = true
                })
            }
        }
        val itensAviso = itensEspeciais + tiposAviso.map { tipo ->
            val label = if (isBike && tipo == TipoManutencao.REVISAO) tr("Peças", "Parts") else tipoManutencaoLabel(tipo)
            AvisoItem(
                label,
                tipo.getIcon(),
                calcularCorStatusLocal(lembretesDoCarroAtual, tipo),
                tipo = tipo,
                iconOverride = if (isBike && tipo == TipoManutencao.FREIO) Icons.Rounded.TireRepair else null
            ) {
                showTipoAvisoDialog = false
                iniciarCameraProduto = false
                tipoAvisoSelecionado = tipo
                showAddLembreteDialog = true
            }
        }
        val avisoBackground = SolidColor(homeScreenBg)
        val avisoTextPrimary = if (isDark) textLight else Color.Black
        val avisoTextDim = if (isDark) textDim else Color(0xFF475569)
        TipoAvisoScreen(
            itensAviso = itensAviso,
            backgroundBrush = avisoBackground,
            surfaceDark = if (isDark) surfaceDark else colorScheme.surface,
            textLight = avisoTextPrimary,
            textDim = avisoTextDim,
            onDismiss = { showTipoAvisoDialog = false }
        )
        return
    }
    if (showTesteNotificacaoDialog) {
        NotificacaoRapidaDialog(
            onDismiss = { showTesteNotificacaoDialog = false },
            onDisparar = {
                NotificacaoHelper.dispararNotificacaoInstantanea(
                    context.applicationContext,
                    "Aviso Zellu",
                    "Teste de notificação enviado com sucesso."
                )
                Toast.makeText(context, "Notificação enviada!", Toast.LENGTH_SHORT).show()
            }
        )
    }
    BackHandler(enabled = showConfiguracoes) { showConfiguracoes = false }
    if (showConfiguracoes) {
        ConfiguracoesScreen(
            onDismiss = { showConfiguracoes = false },
            onTestarNotificacao = {
                showTesteNotificacaoDialog = true
                showConfiguracoes = false
            },
            carros = listaCarros,
            lembretes = todosLembretes,
            contatos = listaContatos,
            planTier = planTier,
            subscriptionBillingInfo = subscriptionBillingInfo,
            onRefreshPlan = { subscriptionManager.refreshBillingStatus() },
            onThemeModeChanged = onThemeModeChanged
        )
        return
    }
    BackHandler(enabled = showAnjoDaGuardaScreen) { showAnjoDaGuardaScreen = false }
    if (showAnjoDaGuardaScreen) {
        AnjoDaGuardaScreen(onDismiss = { showAnjoDaGuardaScreen = false })
        return
    }
    BackHandler(enabled = showMecanicoVirtualScreen) { showMecanicoVirtualScreen = false }
    if (showMecanicoVirtualScreen) {
        Log.d("PremiumNav", "render MecanicoVirtualScreen=true")
        MecanicoVirtualScreen(
            carros = listaCarros,
            abastecimentos = abastecimentos,
            lembretes = todosLembretes,
            isPremium = isSubscribed,
            onPremiumRequired = { activity?.let { subscriptionManager.launchPurchaseFlow(it) } },
            onDismiss = { showMecanicoVirtualScreen = false }
        )
        return
    }
    BackHandler(enabled = showCorporateAgendaScreen) { showCorporateAgendaScreen = false }
    if (showCorporateAgendaScreen && !corporateAgendaCompanyId.isNullOrBlank()) {
        CorporateFleetModuleScreen(
            module = CorporateFleetModule.RESERVATIONS,
            onDismiss = { showCorporateAgendaScreen = false },
            screenBg = homeScreenBg,
            cardBg = surfaceDark,
            cardBorder = drawerItemBorderColor,
            titleColor = textLight,
            subColor = textDim,
            dimColor = textDim.copy(alpha = 0.78f)
        )
        return
    }
    BackHandler(enabled = showPremiumHubScreen) { showPremiumHubScreen = false }
    if (showPremiumHubScreen && planTier != PlanTier.FREE) {
        PremiumHubScreen(
            planTier = planTier,
            onDismiss = { showPremiumHubScreen = false },
            onOpenGuardian = {
                showPremiumHubScreen = false
                showAnjoDaGuardaScreen = true
            },
            onOpenVehicleAiChat = {
                showPremiumHubScreen = false
                returnToPremiumBenefitsAfterAi = true
                showVehicleAiChatScreen = true
            },
            onOpenAiAssistant = {
                showPremiumHubScreen = false
                showAiAssistantScreen = true
            },
            onOpenFleetOverview = {
                showPremiumHubScreen = false
                showFleetOverviewScreen = true
            },
            onOpenFleetStock = {
                showPremiumHubScreen = false
                showFleetStockScreen = true
            },
            onOpenSubscribe = {
                showPremiumHubScreen = false
                activity?.let { subscriptionManager.launchPurchaseFlow(it) }
            },
            isAiBlocked = isAiBlocked,
            isWebBlocked = isWebBlocked,
            hasCorporateInviteAccess = !corporateAgendaCompanyId.isNullOrBlank()
        )
        return
    }
    BackHandler(enabled = showFleetOverviewScreen) { showFleetOverviewScreen = false }
    if (showFleetOverviewScreen) {
        VisaoGeralFrotaScreen(
            carros = listaCarros,
            onSelecionar = { selecionado ->
                val novoIndice = listaCarros.indexOfFirst { it.id == selecionado.id }
                if (novoIndice >= 0) {
                    indiceCarroAtual = novoIndice
                }
                showFleetOverviewScreen = false
            },
            onDismiss = { showFleetOverviewScreen = false },
            onOpenReminderDetails = { lembrete ->
                showFleetOverviewScreen = false
                lembreteSelecionado = lembrete
                contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                showLembreteDetalhesScreen = true
            }
        )
        return
    }
    BackHandler(enabled = showFleetStockScreen) { showFleetStockScreen = false }
    if (showFleetStockScreen) {
        PremiumFleetStockScreen(
            onDismiss = { showFleetStockScreen = false }
        )
        return
    }
    if (showPremiumHubScreen && planTier == PlanTier.FREE) {
        showPremiumHubScreen = false
        showPremiumBeneficiosScreen = true
    }
    BackHandler(enabled = showEbookStoreScreen) { showEbookStoreScreen = false }
    if (showEbookStoreScreen) {
        EbookStoreScreen(
            onDismiss = { showEbookStoreScreen = false }
        )
        return
    }
    BackHandler(enabled = showReleaseNotesScreen) { showReleaseNotesScreen = false }
    if (showReleaseNotesScreen) {
        ReleaseNotesScreen(onDismiss = { showReleaseNotesScreen = false })
        return
    }
    BackHandler(enabled = showLembreteDetalhesScreen) { showLembreteDetalhesScreen = false }
    BackHandler(enabled = showAvisosCompletosScreen) { showAvisosCompletosScreen = false }
    if (showAvisosCompletosScreen) {
        AvisosCompletosScreen(
            nomeVeiculo = carroAtual.nome,
            avisos = avisosOrdenadosDoCarro,
            registros = registrosOrdenadosDoCarro,
            categoriasDisponiveis = tiposAvisoPorVeiculo(carroAtual.tipoVeiculo)
                .filterNot { it == TipoManutencao.ABASTECIMENTO },
            corDoStatus = { calcularCorStatusLocal(lembretesAtivosDoCarroAtual, it.tipo) },
            temPrestador = { aviso -> listaContatos.any { it.id == aviso.contatoId } },
            onAbrirAviso = { lembrete ->
                lembreteSelecionado = lembrete
                contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                showAvisosCompletosScreen = false
                showLembreteDetalhesScreen = true
            },
            onDismiss = { showAvisosCompletosScreen = false }
        )
        return
    }
    if (showLembreteDetalhesScreen && lembreteSelecionado != null) {
        LembreteDetalhesScreen(
            lembrete = lembreteSelecionado!!,
            contato = contatoDetalheSelecionado,
            carro = carroAtual,
            onDismiss = { showLembreteDetalhesScreen = false },
            onDelete = { selecionado ->
                NotificacaoHelper.cancelarNotificacao(context.applicationContext, selecionado.id)
                todosLembretes = todosLembretes.filter { it.id != selecionado.id }
                lembreteSelecionado = null
                contatoDetalheSelecionado = null
                showLembreteDetalhesScreen = false
            },
            onMarkAsDone = { selecionado ->
                val appContext = context.applicationContext
                val recorrenciaAtual = NotificacaoHelper.obterRecorrencia(appContext, selecionado.id)
                Log.i(
                    "ReminderRepeat",
                    "acao=mark_done id=${selecionado.id} recorrenciaAtiva=${recorrenciaAtual != null} dataAtual=${selecionado.dataLimite}"
                )
                if (recorrenciaAtual != null) {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val intervalo = recorrenciaAtual.interval.coerceAtLeast(1)
                    val hoje = LocalDate.now()
                    var proximaData = runCatching {
                        LocalDate.parse(selecionado.dataLimite, formatter)
                    }.getOrElse { hoje }
                    while (!proximaData.isAfter(hoje)) {
                        proximaData = when (recorrenciaAtual.unit) {
                            NotificacaoHelper.REC_UNIT_DAY -> proximaData.plusDays(intervalo.toLong())
                            NotificacaoHelper.REC_UNIT_WEEK -> proximaData.plusWeeks(intervalo.toLong())
                            NotificacaoHelper.REC_UNIT_MONTH -> proximaData.plusMonths(intervalo.toLong())
                            NotificacaoHelper.REC_UNIT_YEAR -> proximaData.plusYears(intervalo.toLong())
                            else -> proximaData.plusDays(intervalo.toLong())
                        }
                    }
                    val atualizado = registrarCicloRealizado(selecionado, hoje).copy(
                        dataLimite = proximaData.format(formatter),
                        horaAviso = selecionado.horaAviso.ifBlank { "09:00" },
                        estabelecimentoEndereco = if (isLembreteRealizado(selecionado)) "" else selecionado.estabelecimentoEndereco
                    )
                    Log.i(
                        "ReminderRepeat",
                        "acao=mark_done_reagendar id=${selecionado.id} unit=${recorrenciaAtual.unit} interval=${intervalo} proximaData=${atualizado.dataLimite} hora=${atualizado.horaAviso}"
                    )
                    todosLembretes = todosLembretes.map {
                        if (it.id == selecionado.id) atualizado else it
                    }
                    NotificacaoHelper.agendarNotificacao(appContext, atualizado, atualizado.horaAviso)
                    Toast.makeText(
                        context,
                        trNow("Ciclo concluído. Próximo aviso reagendado.", "Cycle completed. Next reminder rescheduled."),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.i("ReminderRepeat", "acao=mark_done_sem_recorrencia id=${selecionado.id} -> encerrando")
                    NotificacaoHelper.cancelarNotificacao(appContext, selecionado.id)
                    todosLembretes = todosLembretes.map {
                        if (it.id == selecionado.id) marcarLembreteComoRealizado(it) else it
                    }
                    Toast.makeText(context, trNow("Aviso marcado como feito.", "Reminder marked as done."), Toast.LENGTH_SHORT).show()
                }
                lembreteSelecionado = null
                contatoDetalheSelecionado = null
                showLembreteDetalhesScreen = false
            },
            onFinalizeAndClose = { selecionado ->
                val appContext = context.applicationContext
                Log.i("ReminderRepeat", "acao=finalizar_encerrar id=${selecionado.id}")
                NotificacaoHelper.cancelarNotificacao(appContext, selecionado.id)
                NotificacaoHelper.removerRecorrencia(appContext, selecionado.id)
                todosLembretes = todosLembretes.map {
                    if (it.id == selecionado.id) marcarLembreteComoRealizado(it) else it
                }
                lembreteSelecionado = null
                contatoDetalheSelecionado = null
                showLembreteDetalhesScreen = false
                Toast.makeText(
                    context,
                    trNow("Aviso finalizado e encerrado.", "Reminder finalized and closed."),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSalvar = { atualizado ->
                todosLembretes = todosLembretes.map { if (it.id == atualizado.id) atualizado else it }
                NotificacaoHelper.cancelarNotificacao(context.applicationContext, atualizado.id)
                NotificacaoHelper.agendarNotificacao(context.applicationContext, atualizado, atualizado.horaAviso)
                lembreteSelecionado = atualizado
                contatoDetalheSelecionado = listaContatos.find { it.id == atualizado.contatoId }
                Toast.makeText(context, trNow("Aviso atualizado!", "Reminder updated!"), Toast.LENGTH_SHORT).show()
            },
            onAddPrestador = { lembrete ->
                lembreteParaVincularContato = lembrete.id
                showSelecionarPrestadorScreen = true
            }
        )
        return
    }

    val termosCorporativos = if (isEnglishUi()) """
        1. Acceptance: by using Zellu, you agree to these Terms and the Privacy Policy.
        2. Scope: Zellu organizes vehicles, reminders, maintenance, costs, documents and history. Corporate accounts include reservations, QR Code trips, odometers, signatures, alerts and a web dashboard.
        3. Organizations: companies can invite users, assign roles and control access to vehicles, reservations, trips, alerts, documents and reports.
        4. QR Code and speed: pickup and return record date, time, user, signature and mileage. GPS distance and speed are operational estimates, not official fines or certified measurements.
        5. Proper use: use the app lawfully, provide truthful data and do not use another reservation, QR Code or organization.
        6. AI and responsibility: AI may be wrong and does not replace technical diagnosis, inspection, insurance or professional advice.
        7. Plans, availability and contact: paid plans follow the payment platform rules; features may change or be suspended for security, evolution or legal reasons. Contact: guilhermedevsistemas@gmail.com
    """.trimIndent() else """
        1. Aceite: ao usar o Zellu, voce concorda com estes Termos e com a Politica de Privacidade.
        2. Objeto: o Zellu organiza veiculos, lembretes, manutencao, custos, documentos e historico. O modulo corporativo oferece reservas, viagens por QR Code, odometro, assinaturas, alertas e dashboard web.
        3. Organizacoes: empresas podem convidar usuarios, definir papeis e controlar o acesso a veiculos, reservas, viagens, avisos, documentos e relatorios.
        4. QR Code e velocidade: retirada e devolucao registram data, hora, usuario, assinatura e quilometragem. Distancia e velocidade do GPS sao estimativas operacionais, nao multas oficiais ou medicoes certificadas.
        5. Uso adequado: use o app de forma licita, informe dados verdadeiros e nao use reserva, QR Code ou organizacao de outra pessoa.
        6. IA e responsabilidade: a IA pode errar e nao substitui diagnostico tecnico, vistoria, seguro, mecanico ou orientacao profissional.
        7. Planos, disponibilidade e contato: planos pagos seguem as regras da plataforma de pagamento; funcionalidades podem mudar ou ser suspensas por seguranca, evolucao ou obrigacao legal. Contato: guilhermedevsistemas@gmail.com
    """.trimIndent()

    val privacidadeCorporativa = if (isEnglishUi()) """
        1. Data: account, vehicles, reservations, trips, organization, drivers, schedules, destinations, maintenance, alerts, documents, costs, mileage, signatures, photos, PDFs, receipts, active-trip location, estimated speed, files, notifications, technical data and AI interactions.
        2. Purposes: authentication, reservations, QR Code pickup and return, history, maintenance, security, notifications, AI, support and abuse/fraud prevention.
        3. Location: when authorized, location is used only during an active trip to estimate distance and speed. The app should not track the user outside the trip.
        4. Sharing: we do not sell personal data. Corporate administrators and authorized managers may access organization data according to their roles; technical providers may process what is necessary for the service.
        5. Storage and retention: data may be stored on the device and in the cloud for service, organization history, audit and legal purposes. Corporate data may depend on the company administrator for deletion.
        6. Rights and incidents: you may request access, correction, information about sharing, deletion and consent revocation. Relevant security incidents are assessed, contained and communicated when legally required.
        7. Full pages and contact:
        https://zellu-privacidade.vercel.app/privacy-policy.html
        https://zellu-privacidade.vercel.app/terms-of-use.html
        guilhermedevsistemas@gmail.com
    """.trimIndent() else """
        1. Dados: conta, veiculos, reservas, viagens, empresa, motoristas, horarios, destinos, manutencoes, avisos, documentos, custos, quilometragens, assinaturas, fotos, PDFs, comprovantes, localizacao durante viagem ativa, velocidade estimada, arquivos, notificacoes, dados tecnicos e IA.
        2. Finalidades: autenticacao, reservas, QR Code, registros de retirada e devolucao, historico, manutencao, seguranca, notificacoes, IA, suporte e prevencao de abuso/fraude.
        3. Localizacao: quando autorizada, e usada somente durante uma viagem ativa para estimar distancia e velocidade. O app nao deve rastrear o usuario fora da viagem.
        4. Compartilhamento: nao vendemos dados pessoais. Administradores e gestores autorizados podem acessar dados corporativos conforme o papel; provedores tecnicos tratam apenas o necessario ao servico.
        5. Armazenamento e retencao: dados podem ficar no dispositivo e na nuvem pelo tempo necessario ao servico, historico, auditoria e obrigacoes legais. Dados corporativos podem depender do administrador da empresa para exclusao.
        6. Direitos e incidentes: voce pode solicitar acesso, correcao, informacoes sobre compartilhamento, exclusao e revogacao de consentimento. Incidentes relevantes serao avaliados, contidos e comunicados quando exigido pela legislacao.
        7. Paginas completas e contato:
        https://zellu-privacidade.vercel.app/privacy-policy.html
        https://zellu-privacidade.vercel.app/terms-of-use.html
        guilhermedevsistemas@gmail.com
    """.trimIndent()

    BackHandler(enabled = showTermsScreen) { showTermsScreen = false }
    if (showTermsScreen) {
        LegalInfoScreen(
            title = tr("Termos de uso", "Terms of use"),
            icon = Icons.Default.Description,
            content = termosCorporativos,
            onDismiss = { showTermsScreen = false }
        )
        return
    }
    BackHandler(enabled = showPrivacyScreen) { showPrivacyScreen = false }
    if (showPrivacyScreen) {
        LegalInfoScreen(
            title = tr("Política de privacidade", "Privacy policy"),
            icon = Icons.Default.Lock,
            content = privacidadeCorporativa,
            onDismiss = { showPrivacyScreen = false }
        )
        return
    }
    BackHandler(enabled = showPremiumBeneficiosScreen) { showPremiumBeneficiosScreen = false }
    if (showPremiumBeneficiosScreen) {
        PremiumBeneficiosScreen(
            onDismiss = { showPremiumBeneficiosScreen = false },
            onSubscribeNow = { plano ->
                activity?.let { subscriptionManager.launchPurchaseFlow(it, plano) }
            }
        )
        return
    }
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("Recurso Premium", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold) },
            text = { Text("Escolha o Lite, Frota ou Enterprise conforme o nivel de gestao que voce precisa. O valor de cada plano aparece na tela de planos, direto do Google Play.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        activity?.let { subscriptionManager.launchPurchaseFlow(it) }
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
    if (showPremiumInfo) {
        val premiumDialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFBF2)
        val premiumTitle = if (isDark) Color(0xFFF8FAFC) else Color(0xFF7A5600)
        val premiumText = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
        val premiumSubtitle = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        val premiumBorder = if (isDark) Color(0xFFB88915).copy(alpha = 0.7f) else Color(0xFFB88915)
        AlertDialog(
            onDismissRequest = { showPremiumInfo = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = Color(0xFFD4A017),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Zellu Premium", color = premiumTitle, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Escolha Lite para uso individual, Frota para gestao avancada ou Enterprise para criar sua empresa e gerenciar acessos.",
                        color = premiumText,
                        fontWeight = FontWeight.Medium
                    )
                    // O valor de cada linha vem do Play; se ele ainda nao respondeu,
                    // mostramos so o nome do plano em vez de um preco possivelmente errado.
                    listOf(
                        Triple(
                            SubscriptionPlan.LITE,
                            "Lite",
                            "Avisos, viagens, custos e historico para o uso individual."
                        ),
                        Triple(
                            SubscriptionPlan.FROTA,
                            "Frota",
                            "Gestao avancada dos veiculos e acesso corporativo por convite."
                        ),
                        Triple(
                            SubscriptionPlan.ENTERPRISE,
                            "Enterprise",
                            "Crie sua empresa, convide usuarios e opere tudo pelo painel."
                        )
                    ).forEach { (plano, nome, descricao) ->
                        val preco = playPriceInlineLabel(playPlanPrices[plano.productId])
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (plano == SubscriptionPlan.LITE) Icons.Default.DirectionsCar else Icons.Default.Diamond,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    if (preco.isBlank()) nome else "$nome - $preco",
                                    color = premiumTitle,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(descricao, color = premiumSubtitle, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumInfo = false
                        showPremiumBeneficiosScreen = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Assine já", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPremiumInfo = false },
                    border = BorderStroke(1.dp, premiumBorder)
                ) {
                    Text("Agora não", color = premiumTitle)
                }
            },
            containerColor = premiumDialogBg
        )
    }

    BackHandler(enabled = showGaragemScreen) { showGaragemScreen = false }
    if (showGaragemScreen) {
        GaragemOverviewScreen(
            carros = listaCarros,
            onSelecionar = { selecionado ->
                val novoIndice = listaCarros.indexOfFirst { it.id == selecionado.id }
                if (novoIndice >= 0) {
                    indiceCarroAtual = novoIndice
                }
                showGaragemScreen = false
            },
            onDismiss = { showGaragemScreen = false },
            // EditarCarroScreen edita o veiculo ativo, entao editar um da lista passa por
            // torna-lo ativo primeiro.
            onEditar = { selecionado ->
                val novoIndice = listaCarros.indexOfFirst { it.id == selecionado.id }
                if (novoIndice >= 0) {
                    indiceCarroAtual = novoIndice
                    showGaragemScreen = false
                    showEditCarScreen = true
                }
            },
            showVehicleHealthSection = false,
            onOpenReminderDetails = { lembrete ->
                showGaragemScreen = false
                lembreteSelecionado = lembrete
                contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                showLembreteDetalhesScreen = true
            }
        )
        return
    }
    BackHandler(enabled = showCarInfoScreen) { showCarInfoScreen = false }
    if (showCarInfoScreen) {
        CarroInfoScreen(
            carro = carroAtual,
            lembretes = lembretesDoCarroAtual,
            isPremium = planTier != PlanTier.FREE,
            onExportVehicle = {
                val opened = exportVehicleToOtherDevice(
                    context = context,
                    vehicle = carroAtual,
                    reminders = todosLembretes.filter { it.carroId == carroAtual.id },
                    fuelRecords = abastecimentos.filter { it.carroId == carroAtual.id }
                )
                if (!opened) {
                    Toast.makeText(context, "Nao consegui abrir o compartilhamento.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showCarInfoScreen = false }
        )
        return
    }
    BackHandler(enabled = showPerfilScreen) { showPerfilScreen = false }
    if (showPerfilScreen) {
        PerfilScreen(
            onDismiss = { showPerfilScreen = false },
            planTier = planTier,
            subscriptionBillingInfo = subscriptionBillingInfo,
            totalVeiculos = listaCarros.size
        )
        return
    }

    BackHandler(enabled = showHistoricoAbastecimentoScreen) { showHistoricoAbastecimentoScreen = false }
    if (showHistoricoAbastecimentoScreen) {
        HistoricoAbastecimentoScreen(carroId = carroAtual.id, onDismiss = { showHistoricoAbastecimentoScreen = false })
        return
    }
    BackHandler(enabled = showBikeDistanceRegister) { showBikeDistanceRegister = false }
    if (showBikeDistanceRegister) {
        BikeDistanceScreen(carroId = carroAtual.id, onDismiss = { showBikeDistanceRegister = false })
        return
    }
    BackHandler(enabled = showBikeDistanceHistory) { showBikeDistanceHistory = false }
    if (showBikeDistanceHistory) {
        BikeDistanceHistoryScreen(carroId = carroAtual.id, onDismiss = { showBikeDistanceHistory = false })
        return
    }
    BackHandler(enabled = showAondePareiScreen) { showAondePareiScreen = false }
    if (showAondePareiScreen) {
        AondePareiScreen(onDismiss = { showAondePareiScreen = false })
        return
    }
    BackHandler(enabled = showTravelAlarmScreen) { showTravelAlarmScreen = false }
    if (showTravelAlarmScreen) {
        TravelAlarmScreen(onDismiss = { showTravelAlarmScreen = false })
        return
    }
    BackHandler(enabled = showVehicleAiChatScreen) { closeVehicleAiChat() }
    if (showVehicleAiChatScreen && hasVehicleAiAccess) {
        VehicleAiChatScreen(
            carros = listaCarros,
            currentCarroId = carroAtual.id,
            lembretesAtivos = todosLembretes.filterNot(::isLembreteRealizado),
            abastecimentos = abastecimentos,
            onCreateReminders = { novosAvisos ->
                if (!podeCriarAvisos(novosAvisos)) {
                    avisarLimiteDeAvisos()
                    return@VehicleAiChatScreen
                }
                todosLembretes = todosLembretes + novosAvisos
                AdminUsageMetrics.markReminderCreated(novosAvisos.size)
                // Mesmo padrão do cadastro manual: agenda a notificação de cada aviso
                // (avisos por data disparam alarme; por km ficam só no monitoramento).
                val ctxAgendar = context.applicationContext
                novosAvisos.filterNot(::isLembreteRealizado).forEach { aviso ->
                    NotificacaoHelper.agendarNotificacao(ctxAgendar, aviso, aviso.horaAviso)
                }
            },
            onCreateFuelRecords = { novosAbastecimentos ->
                abastecimentos = abastecimentos + novosAbastecimentos
                val maiorKmPorCarro = novosAbastecimentos
                    .mapNotNull { abastecimento ->
                        abastecimento.km?.takeIf { it > 0 }?.let { km -> abastecimento.carroId to km }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, kms) -> kms.maxOrNull() ?: 0 }
                if (maiorKmPorCarro.isNotEmpty()) {
                    listaCarros = listaCarros.map { carro ->
                        val novoKm = maiorKmPorCarro[carro.id]
                        if (novoKm != null && novoKm > carro.kmAtual) {
                            carro.copy(kmAtual = novoKm)
                        } else {
                            carro
                        }
                    }
                }
            },
            onShareVehicleReport = { carro ->
                drawerScope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        gerarPdfRelatorio(
                            context = context,
                            carro = carro,
                            lembretes = todosLembretes.filter { it.carroId == carro.id },
                            isPremium = planTier != PlanTier.FREE
                        )
                    }
                    if (uri != null) {
                        compartilharPdf(context, uri)
                    } else {
                        Toast.makeText(context, "Nao consegui gerar o PDF desse veiculo agora.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onShareFleetReports = {
                drawerScope.launch {
                    val uris = withContext(Dispatchers.IO) {
                        listaCarros.mapNotNull { carro ->
                            gerarPdfRelatorio(
                                context = context,
                                carro = carro,
                                lembretes = todosLembretes.filter { it.carroId == carro.id },
                                isPremium = true
                            )
                        }
                    }
                    if (!compartilharPdfsDaFrota(context, uris)) {
                        Toast.makeText(context, "Nao consegui abrir o compartilhamento da frota agora.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { closeVehicleAiChat() },
            planTier = planTier
        )
        return
    }
    if (showVehicleAiChatScreen && !hasVehicleAiAccess) {
        showVehicleAiChatScreen = false
        returnToPremiumBenefitsAfterAi = false
        showPremiumBeneficiosScreen = true
    }
    BackHandler(enabled = showAiAssistantScreen) { showAiAssistantScreen = false }
    if (showAiAssistantScreen) {
        AssistentePremiumScreen(onDismiss = { showAiAssistantScreen = false })
        return
    }
    BackHandler(enabled = showVehicleGuideScreen) { showVehicleGuideScreen = false }
    if (showVehicleGuideScreen) {
        VehicleBasicsGuideScreen(onDismiss = { showVehicleGuideScreen = false })
        return
    }
    LaunchedEffect(showBikeDistanceRegister, showBikeDistanceHistory) {
        if (!showBikeDistanceRegister && !showBikeDistanceHistory) {
            pedaladas = withContext(Dispatchers.IO) { BancoDeDados.carregarPedaladas(context) }
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(homeScreenBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = accentBlue)
                Text(
                    text = "Carregando seus dados...",
                    color = textDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    if (listaCarros.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(homeScreenBg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = accentBlue)
                Text(
                    text = "Verificando seus veículos...",
                    color = textDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    BackHandler(enabled = showAddLembreteDialog) {
        showAddLembreteDialog = false
        iniciarCameraProduto = false
        showTipoAvisoDialog = true
    }
    if (showAddLembreteDialog) {
        NovoAgendamentoDialog(
            carroAtual = carroAtual,
            contatosDisponiveis = listaContatos,
            onDismiss = { showAddLembreteDialog = false; iniciarCameraProduto = false },
            onBackToTipoAviso = {
                showAddLembreteDialog = false
                iniciarCameraProduto = false
                showTipoAvisoDialog = true
            },
            onConfirm = { novo ->
                val novoComCarro = novo.copy(carroId = carroAtual.id)
                if (!podeCriarAvisos(listOf(novoComCarro))) {
                    avisarLimiteDeAvisos()
                    return@NovoAgendamentoDialog
                }
                val hadNoReminderBefore = todosLembretes.none { it.tipo != TipoManutencao.ABASTECIMENTO }
                todosLembretes = todosLembretes + novoComCarro
                AdminUsageMetrics.markReminderCreated()
                showAddLembreteDialog = false
                iniciarCameraProduto = false
                if (
                    hadNoReminderBefore &&
                    novo.tipo != TipoManutencao.ABASTECIMENTO &&
                    isLembreteRealizado(novo) &&
                    shouldShowReportMiniTutorial(context)
                ) {
                    showReportMiniTutorial = true
                }
                val mensagem = if (isLembreteRealizado(novo)) {
                    trNow("Serviço registrado no histórico.", "Service recorded in history.")
                } else {
                    trNow("Aviso cadastrado com sucesso!", "Reminder saved successfully!")
                }
                Toast.makeText(context, mensagem, Toast.LENGTH_SHORT).show()
            },
            onMultiConfirm = { novosItens ->
                val novosLembretes = novosItens.map { it.copy(carroId = carroAtual.id) }
                if (!podeCriarAvisos(novosLembretes)) {
                    avisarLimiteDeAvisos()
                    return@NovoAgendamentoDialog
                }
                val hadNoReminderBefore = todosLembretes.none { it.tipo != TipoManutencao.ABASTECIMENTO }
                todosLembretes = todosLembretes + novosLembretes
                AdminUsageMetrics.markReminderCreated(novosLembretes.size)
                showAddLembreteDialog = false
                iniciarCameraProduto = false
                if (
                    hadNoReminderBefore &&
                    novosLembretes.any { it.tipo != TipoManutencao.ABASTECIMENTO } &&
                    novosLembretes.any { isLembreteRealizado(it) } &&
                    shouldShowReportMiniTutorial(context)
                ) {
                    showReportMiniTutorial = true
                }
                Toast.makeText(context, "${novosLembretes.size} itens salvos!", Toast.LENGTH_SHORT).show()
            },
            onUpdateKmCarro = { novoKm -> listaCarros = listaCarros.map { if (it.id == carroAtual.id) it.copy(kmAtual = novoKm) else it } },
            autoAbrirCamera = iniciarCameraProduto,
            onAutoCameraConsumida = { iniciarCameraProduto = false },
            onAddContato = { novo ->
                listaContatos = listaContatos + novo
            },
            initialTipo = tipoAvisoSelecionado,
            initialRegistroServico = fluxoInicialRegistroServico,
            planTier = planTier,
            onRequestPremium = { showPremiumDialog = true },
            onOpenVehicleGuide = { showVehicleGuideScreen = true }
        )
        return
    }

    // detalhes agora abrem em tela dedicada (não em diÃ¡logo)

    // ----------------- DRAWER (MENU LATERAL) -----------------
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = if (isDark) Color.Black else fuelCardEnd,
                drawerContentColor = textLight
            ) {
                Spacer(Modifier.height(16.dp))

                // Itens do Menu
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.drawer_section_vehicle),
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(Icons.Rounded.DirectionsCar, stringResource(R.string.drawer_item_my_vehicles)) {
                        showGaragemScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.AddCircle, stringResource(R.string.drawer_item_add_vehicle)) {
                        if (listaCarros.size >= maxVehiclesCurrentPlan) {
                            Toast.makeText(
                                context,
                                "Limite do plano $planNameCurrent: $maxVehiclesCurrentPlan veiculos.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@DrawerMenuItem
                        }
                        showAddCarScreen = true
                        drawerScope.launch { drawerState.close() }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.drawer_section_services),
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!corporateAgendaAccessLoading && !corporateAgendaCompanyId.isNullOrBlank()) {
                        DrawerMenuItem(
                            icon = Icons.Default.CalendarToday,
                            label = "Agenda corporativa",
                            highlighted = true
                        ) {
                            showCorporateAgendaScreen = true
                            drawerScope.launch { drawerState.close() }
                        }
                    }
                    if (planTier != PlanTier.FREE) {
                        DrawerMenuItem(
                            icon = Icons.Default.WorkspacePremium,
                            label = "Benefícios do seu plano",
                            highlighted = true
                        ) {
                            showPremiumHubScreen = true
                            drawerScope.launch { drawerState.close() }
                        }
                    }
                    DrawerMenuItem(
                        icon = Icons.Default.AutoStories,
                        label = stringResource(R.string.drawer_item_ebooks)
                    ) {
                        showEbookStoreScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(
                        icon = Icons.Default.Campaign,
                        label = "Novidades do App"
                    ) {
                        showReleaseNotesScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.drawer_section_security),
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(Icons.Default.Description, stringResource(R.string.drawer_item_terms)) {
                        showTermsScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.Lock, stringResource(R.string.drawer_item_privacy)) {
                        showPrivacyScreen = true
                        drawerScope.launch { drawerState.close() }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.drawer_section_settings),
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(Icons.Default.Person, "Conta") {
                        showPerfilScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.Settings, stringResource(R.string.drawer_item_settings)) {
                        showConfiguracoes = true
                        drawerScope.launch { drawerState.close() }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    ) {
        // ----------------- CONTEÃšDO PRINCIPAL (SCAFFOLD) -----------------
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { tutorialViewportHeightPx = it.size.height.toFloat() }
                .background(homeScreenBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
            ) {
                    val topBarHideProgress by remember {
                        derivedStateOf {
                            (contentScrollState.value / 120f).coerceIn(0f, 1f)
                        }
                    }
                    val topBarAnimatedProgress by animateFloatAsState(
                        targetValue = topBarHideProgress,
                        animationSpec = tween(durationMillis = 150),
                        label = "home_top_bar_progress"
                    )
                    val topBarMaxOffsetPx = with(LocalDensity.current) { 18.dp.toPx() }
                    val topBarAlpha = 1f - topBarAnimatedProgress
                    val topBarTranslationY = -topBarMaxOffsetPx * topBarAnimatedProgress
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = topBarAlpha
                            translationY = topBarTranslationY
                        }
                    ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Zellu",
                                color = textLight,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { drawerScope.launch { drawerState.open() } },
                                modifier = Modifier.onGloballyPositioned { menuButtonRect = it.boundsInRoot() }
                            ) {
                                Icon(Icons.Default.Menu, "Menu", tint = textLight)
                            }
                        },
                        modifier = Modifier.statusBarsPadding(),
                        actions = {
                            IconButton(
                                onClick = {
                                    if (homeTutorialSteps.isNotEmpty()) {
                                        drawerScope.launch {
                                            contentScrollState.animateScrollTo(0)
                                            homeTutorialStep = 0
                                            showHomeTutorial = true
                                        }
                                    }
                                },
                                modifier = Modifier.onGloballyPositioned { helpButtonRect = it.boundsInRoot() }
                            ) {
                                Icon(Icons.Default.HelpOutline, "Duvidas frequentes", tint = textLight)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (planTier == PlanTier.FREE) {
                                    IconButton(
                                        onClick = { showPremiumBeneficiosScreen = true },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .onGloballyPositioned { premiumButtonRect = it.boundsInRoot() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Diamond,
                                            contentDescription = "Premium",
                                            tint = Color(0xFFD4A017),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (notificacoesDisparadas.isNotEmpty()) {
                                                Badge(
                                                    modifier = Modifier
                                                        .offset(x = (-3).dp, y = 7.dp)
                                                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                                                    containerColor = Color(0xFFEF4444),
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        text = if (notificacoesDisparadas.size > 99) "99+" else notificacoesDisparadas.size.toString(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        IconButton(
                                            onClick = {
                                                notificacoesDisparadas = NotificacaoHelper.carregarNotificacoesDisparadas(context)
                                                showAvisosNotificacoesDialog = true
                                            },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .onGloballyPositioned { notificationsButtonRect = it.boundsInRoot() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsNone,
                                                contentDescription = tr("Notificações dos avisos", "Reminder notifications"),
                                                tint = textLight
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = topBarDark)
                    )
                    }

                    // Card do veiculo precisa de respiro: quando sangrava de ponta a
                    // ponta, colar na barra era o efeito desejado.
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier.onGloballyPositioned { carInfoRect = it.boundsInRoot() }
                    ) {
                        HomeVehicleHeader(
                            carro = carroAtual,
                            fotoArquivo = fotoDoCarroAtual,
                            avisosVencidos = resumoDoCarroAtual.first,
                            avisosChegando = resumoDoCarroAtual.second,
                            totalVeiculos = listaCarros.size,
                            indiceVeiculo = indiceCarroAtual,
                            onAbrirVeiculo = { showGaragemScreen = true },
                            onEscolherFoto = { escolherFotoDoVeiculo.launch("image/*") },
                            onEditarVeiculo = { showEditCarScreen = true },
                            onVeiculoAnterior = {
                                if (listaCarros.size > 1) {
                                    indiceCarroAtual =
                                        (indiceCarroAtual - 1 + listaCarros.size) % listaCarros.size
                                }
                            },
                            onProximoVeiculo = {
                                if (listaCarros.size > 1) {
                                    indiceCarroAtual = (indiceCarroAtual + 1) % listaCarros.size
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    HomeQuickActions(
                        onNovoAviso = {
                            iniciarCameraProduto = false
                            fluxoInicialRegistroServico = null
                            showFluxoCadastroDialog = true
                        },
                        onAbastecer = { showHistoricoAbastecimentoScreen = true },
                        onRelatorio = {
                            showCarInfoScreen = true
                        },
                        mostrarAbastecer = !isBikeCategory(carroAtual.tipoVeiculo),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .onGloballyPositioned { newReminderButtonRect = it.boundsInRoot() }
                    )

                    Spacer(Modifier.height(4.dp))

                    val categoriasDisponiveis = tiposAvisoPorVeiculo(carroAtual.tipoVeiculo)
                        .filterNot { it == TipoManutencao.ABASTECIMENTO }
                    val iconOverrides = if (isBikeCategory(carroAtual.tipoVeiculo)) {
                        mapOf(TipoManutencao.FREIO to Icons.Rounded.TireRepair)
                    } else {
                        emptyMap()
                    }
                    val labelOverrides = if (isBikeCategory(carroAtual.tipoVeiculo)) {
                        mapOf(TipoManutencao.REVISAO to tr("Peças", "Parts"))
                    } else {
                        emptyMap()
                    }
                    Spacer(Modifier.height(14.dp))
                    AvisosResumoCard(
                        avisosOrdenados = avisosOrdenadosDoCarro,
                        totalAvisos = avisosOrdenadosDoCarro.size,
                        corDoStatus = { calcularCorStatusLocal(lembretesAtivosDoCarroAtual, it.tipo) },
                        temPrestador = { aviso -> listaContatos.any { it.id == aviso.contatoId } },
                        onAbrirAviso = { lembrete ->
                            lembreteSelecionado = lembrete
                            contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                            showLembreteDetalhesScreen = true
                        },
                        onVerTodos = { showAvisosCompletosScreen = true },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .onGloballyPositioned { remindersRect = it.boundsInRoot() }
                    )

                    Spacer(Modifier.height(18.dp))

                    GastosMensaisCard(
                        gastos = gastosMensaisDoCarro,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Folga final para a lista nao encostar na barra de navegacao.
                    Spacer(Modifier.height(96.dp))

                }
            }
            if (showHomeTutorial && homeTutorialSteps.isNotEmpty()) {
                val safeStep = homeTutorialStep.coerceIn(0, homeTutorialSteps.lastIndex)
                val (message, targetKey) = homeTutorialSteps[safeStep]
                val targetRect = normalizedTutorialRect(
                    targetKey,
                    when (targetKey) {
                        "menu" -> menuButtonRect
                        "help" -> helpButtonRect
                        "notifications" -> notificationsButtonRect
                        "premium" -> premiumButtonRect
                        "fuel_history" -> fuelHistoryButtonRect ?: carInfoRect
                        "car" -> carInfoRect
                        "edit_car" -> editCarButtonRect
                        "report" -> reportButtonRect
                        "new_reminder" -> newReminderButtonRect
                        "reminders" -> remindersRect
                        else -> null
                    }
                )
                val targetCorner = when (targetKey) {
                    "menu", "help", "premium", "notifications" -> 14.dp
                    "edit_car", "report", "new_reminder" -> 14.dp
                    "car", "fuel_history", "reminders" -> 18.dp
                    else -> 14.dp
                }
                val tutorialHeaderIcon = when (targetKey) {
                    "menu" -> Icons.Default.Menu
                    "notifications" -> Icons.Default.NotificationsNone
                    "help" -> Icons.Default.HelpOutline
                    "premium" -> Icons.Default.Diamond
                    "car" -> Icons.Default.DirectionsCar
                    "edit_car" -> Icons.Default.Edit
                    "report" -> Icons.Default.Description
                    "new_reminder" -> Icons.Default.Event
                    "reminders" -> Icons.Default.Category
                    "fuel_history" -> Icons.Default.LocalGasStation
                    else -> Icons.Default.Explore
                }
                val tutorialHeaderTitle = when (targetKey) {
                    "menu" -> "Menu principal"
                    "notifications" -> "Notificações"
                    "help" -> "Ajuda guiada"
                    "premium" -> "Premium"
                    "car" -> "Card do veículo"
                    "edit_car" -> "Editar veículo"
                    "report" -> "Relatório"
                    "new_reminder" -> "Novo lembrete"
                    "reminders" -> "Categorias de avisos"
                    "fuel_history" -> "Histórico de abastecimento"
                    else -> "Guia rápido"
                }
                HomeTutorialSpotlightOverlay(
                    targetRect = targetRect,
                    message = message,
                    step = safeStep + 1,
                    total = homeTutorialSteps.size,
                    targetCornerRadius = targetCorner,
                    accentBlue = accentBlue,
                    stepIcon = tutorialHeaderIcon,
                    stepTitle = tutorialHeaderTitle,
                    onClose = {
                        showHomeTutorial = false
                        markHomeTutorialSeen(context)
                    },
                    onNext = {
                        if (safeStep < homeTutorialSteps.lastIndex) {
                            homeTutorialStep = safeStep + 1
                        } else {
                            showHomeTutorial = false
                            markHomeTutorialSeen(context)
                        }
                    }
                )
            }
            if (showReportMiniTutorial && !showHomeTutorial) {
                HomeTutorialSpotlightOverlay(
                    targetRect = reportButtonRect,
                    message = "Toque em Relatório para abrir o resumo completo do veículo.",
                    step = 1,
                    total = 1,
                    targetCornerRadius = 14.dp,
                    accentBlue = accentBlue,
                    stepIcon = Icons.Default.Description,
                    stepTitle = "Abrir relatório",
                    onClose = {
                        showReportMiniTutorial = false
                        markReportMiniTutorialSeen(context)
                    },
                    onNext = {
                        showReportMiniTutorial = false
                        markReportMiniTutorialSeen(context)
                        showCarInfoScreen = true
                    }
                )
            }
        }
    }
    if (showAvisosNotificacoesDialog) {
        BackHandler { showAvisosNotificacoesDialog = false }
        var showConfirmarLimpezaNotificacoes by remember { mutableStateOf(false) }
        val nomeCarroPorId = remember(listaCarros) {
            listaCarros.associate { carro ->
                // Desempatado pela placa: aviso de dois veiculos homonimos ficava
                // indistinguivel nesta lista.
                val nome = nomeExibicaoVeiculo(carro, listaCarros).ifBlank {
                    listOf(carro.marca, carro.modelo).joinToString(" ").trim().ifBlank { "Veículo sem nome" }
                }
                carro.id to nome
            }
        }
        val msgAvisoNaoEncontrado = tr(
            "Não foi possível abrir este aviso. Ele pode ter sido removido.",
            "Could not open this reminder. It may have been removed."
        )
        val msgAvisoInformativo = tr(
            "Esta notificação é apenas informativa e não abre tela.",
            "This notification is informational only and cannot be opened."
        )
        AvisosNotificacoesScreen(
            notificacoes = notificacoesDisparadas,
            onClear = {
                showConfirmarLimpezaNotificacoes = true
            },
            onRemove = { aviso ->
                if (aviso.id.startsWith("PARKING_")) {
                    val blockedMsg = if (Locale.getDefault().language.startsWith("en")) {
                        "This ongoing parking reminder cannot be removed."
                    } else {
                        "O aviso de parada em andamento não pode ser removido."
                    }
                    Toast.makeText(context, blockedMsg, Toast.LENGTH_SHORT).show()
                    return@AvisosNotificacoesScreen
                }
                NotificacaoHelper.removerNotificacaoDisparada(context, aviso.id, aviso.timestamp)
                notificacoesDisparadas = notificacoesDisparadas.filterNot {
                    it.id == aviso.id && it.timestamp == aviso.timestamp
                }
                val removeMsg = if (Locale.getDefault().language.startsWith("en")) {
                    "Notification deleted successfully."
                } else {
                    "Notificação apagada com sucesso."
                }
                Toast.makeText(
                    context,
                    removeMsg,
                    Toast.LENGTH_SHORT
                ).show()
            },
            resolveVehicleName = { aviso ->
                aviso.carroId?.let { nomeCarroPorId[it] }
                    ?: if (aviso.id.startsWith("PARKING_")) {
                        nomeCarroPorId[carroAtual.id]
                    } else {
                        val lembreteRelacionado = todosLembretes.firstOrNull { it.id == aviso.id }
                            ?: todosLembretes.firstOrNull { aviso.id.startsWith("${it.id}_") }
                        lembreteRelacionado?.carroId?.let { nomeCarroPorId[it] }
                    }
            },
            canOpenNotification = { aviso ->
                if (aviso.id.startsWith("PARKING_")) {
                    true
                } else {
                    todosLembretes.any { it.id == aviso.id || aviso.id.startsWith("${it.id}_") }
                }
            },
            onOpen = { aviso ->
                if (aviso.id.startsWith("PARKING_")) {
                    aviso.carroId?.let { carroId ->
                        val idx = listaCarros.indexOfFirst { it.id == carroId }
                        if (idx >= 0) indiceCarroAtual = idx
                    }
                    showAvisosNotificacoesDialog = false
                    showAondePareiScreen = true
                    return@AvisosNotificacoesScreen
                }

                val lembrete = todosLembretes.firstOrNull { it.id == aviso.id }
                    ?: todosLembretes.firstOrNull { aviso.id.startsWith("${it.id}_") }

                if (lembrete == null) {
                    if (aviso.id.startsWith("INSTANT_")) {
                        Toast.makeText(context, msgAvisoInformativo, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, msgAvisoNaoEncontrado, Toast.LENGTH_SHORT).show()
                    }
                    return@AvisosNotificacoesScreen
                }

                val idx = listaCarros.indexOfFirst { it.id == lembrete.carroId }
                if (idx >= 0) indiceCarroAtual = idx
                lembreteSelecionado = lembrete
                contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                showAvisosNotificacoesDialog = false
                showLembreteDetalhesScreen = true
            },
            onDismiss = { showAvisosNotificacoesDialog = false }
        )

        if (showConfirmarLimpezaNotificacoes) {
            AlertDialog(
                onDismissRequest = { showConfirmarLimpezaNotificacoes = false },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = if (isDark) 0.24f else 0.14f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(27.dp)
                            )
                        }
                        Text(
                            text = tr("Limpar notificações?", "Clear notifications?"),
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Text(
                        text = tr(
                            "Quer mesmo apagar todas as notificações removíveis agora? O aviso de parada em andamento será mantido.",
                            "Do you really want to clear all removable notifications now? The ongoing parking reminder will be kept."
                        ),
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmarLimpezaNotificacoes = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.widthIn(min = 120.dp)
                        ) {
                            Text(tr("Cancelar", "Cancel"), color = Color(0xFFF8FAFC))
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                showConfirmarLimpezaNotificacoes = false
                                val avisosFixos = notificacoesDisparadas.filter { it.id.startsWith("PARKING_") }
                                val avisosRemoviveis = notificacoesDisparadas.filterNot { it.id.startsWith("PARKING_") }

                                avisosRemoviveis.forEach { aviso ->
                                    NotificacaoHelper.removerNotificacaoDisparada(context, aviso.id, aviso.timestamp)
                                }

                                notificacoesDisparadas = avisosFixos

                                val clearMsg = if (Locale.getDefault().language.startsWith("en")) {
                                    if (avisosRemoviveis.isEmpty()) {
                                        "No notifications to clear."
                                    } else {
                                        "Notifications cleared. Ongoing parking reminder was kept."
                                    }
                                } else {
                                    if (avisosRemoviveis.isEmpty()) {
                                        "Não há notificações para limpar."
                                    } else {
                                        "Todas as notificações foram limpadas."
                                    }
                                }
                                Toast.makeText(context, clearMsg, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.widthIn(min = 140.dp)
                        ) {
                            Text(tr("Sim, limpar", "Yes, clear"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = {},
                containerColor = Color(0xFF0F172A)
            )
        }
    }
}
