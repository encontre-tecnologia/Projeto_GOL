package br.com.gui.carlembrete

import AvisosCategoriasCard
import CarroInfoCard
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
private const val HOME_TUTORIAL_PREFS = "home_tutorial_prefs"
private const val KEY_HOME_TUTORIAL_VERSION = "home_tutorial_version"
private const val CURRENT_HOME_TUTORIAL_VERSION = 1
private const val FORCE_HOME_TUTORIAL_ALWAYS = false
private const val ONBOARDING_PREFS = "onboarding_prefs"
private const val KEY_REPORT_MINI_TUTORIAL_SEEN = "report_mini_tutorial_seen"
private const val TAG_LOGIN_BACKUP_FLOW = "LoginBackupFlow"

private fun shouldAutoStartHomeTutorial(context: Context): Boolean {
    if (FORCE_HOME_TUTORIAL_ALWAYS) return true
    val seenVersion = context
        .getSharedPreferences(HOME_TUTORIAL_PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_HOME_TUTORIAL_VERSION, 0)
    return seenVersion < CURRENT_HOME_TUTORIAL_VERSION
}

private fun markHomeTutorialSeen(context: Context) {
    context.getSharedPreferences(HOME_TUTORIAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_HOME_TUTORIAL_VERSION, CURRENT_HOME_TUTORIAL_VERSION)
        .apply()
}

private fun shouldShowReportMiniTutorial(context: Context): Boolean {
    return !context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_REPORT_MINI_TUTORIAL_SEEN, false)
}

private fun markReportMiniTutorialSeen(context: Context) {
    context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REPORT_MINI_TUTORIAL_SEEN, true)
        .apply()
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
    LaunchedEffect(isLoading, listaCarros) {
        if (!isLoading && listaCarros.isEmpty()) {
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
    var showPerfilScreen by remember { mutableStateOf(false) }
    var showShareVehicleScreen by remember { mutableStateOf(false) }
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
    var autoScrollReportToMaintenance by rememberSaveable { mutableStateOf(false) }
    
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

    LaunchedEffect(openAondePareiOnStart) {
        if (openAondePareiOnStart) {
            showAondePareiScreen = true
            onAondePareiStartConsumed()
        }
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

    val usuarioNome = FirebaseAuth.getInstance().currentUser?.displayName
    val nomeExibido = usuarioNome?.trim()?.split("\\s+".toRegex())?.let { partes ->
        if (partes.isEmpty()) null else if (partes.size == 1) partes[0] else "${partes.first()} ${partes.last()}"
    } ?: (FirebaseAuth.getInstance().currentUser?.email ?: "Usuario")

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
    val planNameCurrent by remember(planTier) { mutableStateOf(planNameLabel(planTier)) }
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
                onDismiss = {
                    showSelecionarPrestadorScreen = false
                    lembreteParaVincularContato = null
                },
                onConfirmar = { novoContato ->
                    val indiceContatoExistente = listaContatos.indexOfFirst { contatoExistente ->
                        contatoExistente.nome.equals(novoContato.nome, ignoreCase = true) &&
                            contatoExistente.tipoServico == novoContato.tipoServico
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
    BackHandler(enabled = showShareVehicleScreen) { showShareVehicleScreen = false }
    if (showShareVehicleScreen) {
        ShareVehicleScreen(
            carroAtual = carroAtual,
            onDismiss = { showShareVehicleScreen = false }
        )
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
            isWebBlocked = isWebBlocked
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
                    val atualizado = selecionado.copy(
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

    BackHandler(enabled = showTermsScreen) { showTermsScreen = false }
    if (showTermsScreen) {
        LegalInfoScreen(
            title = tr("Termos de uso", "Terms of use"),
            icon = Icons.Default.Description,
            content = if (isEnglishUi()) """
                1. Acceptance: by using Zellu, you agree to these Terms and the Privacy Policy.

                2. Service scope: the app includes vehicle management, reminders, maintenance, trips, fleet, stock, and artificial intelligence features.

                3. AI features: Zellu AI is a support tool to interpret registered data, answer questions, and prepare requested actions. It may make mistakes and does not replace technical diagnosis, inspection, mechanics, insurance, or user decisions.

                4. Proper use: you must use the app lawfully, without fraud, abuse, or rights violations.

                5. Account security: you are responsible for account data and access credentials.

                6. Plans and billing: paid plans (such as Lite/Fleet) follow store/payment platform rules for renewal, cancellation, and refunds.

                7. Limitation: Zellu is a support tool and does not replace technical diagnosis, inspection, insurance, mechanical assistance, or professional advice.

                8. Availability: features may be updated, fixed, suspended, or discontinued due to product evolution, security, or legal obligations.

                9. Intellectual property: brand, software, layout, and content are legally protected.

                10. Governing law and venue: Brazilian law applies, with venue in Sao Carlos/SP, except where mandatory law states otherwise.

                11. Legal/support contact: guilhermedevsistemas@gmail.com
            """.trimIndent() else """
                1. Aceite: ao usar o Zellu, você concorda com estes Termos e com a Política de Privacidade.

                2. Objeto: o app oferece gestão de veículos, lembretes, manutenções, viagens, frota, estoque e recursos de inteligência artificial.

                3. Recursos de IA: a Zellu AI é ferramenta de apoio para interpretar dados cadastrados, responder perguntas e preparar ações solicitadas. Ela pode cometer erros e não substitui diagnóstico técnico, vistoria, mecânico, seguro ou decisão do usuário.

                4. Uso adequado: você se compromete a usar o app de forma lícita, sem fraude, abuso técnico ou violação de direitos de terceiros.

                5. Conta e segurança: você é responsável pelos dados da conta e pela guarda do acesso.

                6. Planos e cobrança: planos pagos (como Lite/Frota) seguem regras da loja/plataforma de pagamento para renovação, cancelamento e reembolso.

                7. Limitação: o Zellu é ferramenta de apoio e não substitui diagnóstico técnico, vistoria, seguro, assistência mecânica ou orientação profissional.

                8. Disponibilidade: funcionalidades podem ser alteradas, corrigidas, suspensas ou descontinuadas por evolução do produto, segurança ou obrigação legal.

                9. Propriedade intelectual: marca, software, layout e conteúdo do app são protegidos por lei.

                10. Legislação e foro: aplica-se a legislação brasileira, com foro da comarca de São Carlos/SP, salvo competência legal específica.

                11. Contato legal e suporte: guilhermedevsistemas@gmail.com
            """.trimIndent(),
            onDismiss = { showTermsScreen = false }
        )
        return
    }
    BackHandler(enabled = showPrivacyScreen) { showPrivacyScreen = false }
    if (showPrivacyScreen) {
        LegalInfoScreen(
            title = tr("Política de privacidade", "Privacy policy"),
            icon = Icons.Default.Lock,
            content = if (isEnglishUi()) """
                1. Data processed: account data (name, e-mail, identifiers), vehicle records, reminders, contacts, trips, stock items, location, camera, notifications, essential technical data, and interactions with AI features.

                2. Purposes: authentication, feature execution, intelligent/AI features, security, abuse/fraud prevention, support, and continuous improvement.

                3. Legal bases (LGPD): contract execution, consent where required, legitimate interest for security/stability, and legal/regulatory obligations.

                4. Permissions: camera, location, and notifications are used only with your authorization and can be revoked in device settings.

                5. AI and technical providers: Zellu AI may use registered data and chat messages to respond and prepare actions. When online features are enabled, required content may be processed by infrastructure and/or AI technical providers.

                6. Sharing: we do not sell personal data. Data may be shared with technical operators/providers required for app operation and with authorities when legally required.

                7. Storage and retention: data may be stored on device and cloud, for as long as necessary for service purposes and legal obligations.

                8. Data subject rights: under LGPD, you can request confirmation, access, correction, anonymization, deletion, and consent revocation.

                9. Account/data deletion: when requested, personal and linked records are removed, except mandatory legal retention.

                10. International transfer: some providers may process data outside Brazil under appropriate safeguards.

                11. Official privacy/support contact:
                guilhermedevsistemas@gmail.com
                Official pages:
                https://zellu-privacidade.vercel.app/privacy-policy.html
                https://zellu-privacidade.vercel.app/terms-of-use.html
            """.trimIndent() else """
                1. Dados tratados: o app pode tratar dados de conta (nome, e-mail e identificadores), cadastro de veículos, lembretes, contatos, viagens, itens de estoque, localização, câmera, notificações, dados técnicos essenciais e interações com recursos de IA.

                2. Finalidades: autenticação, execução das funcionalidades, recursos inteligentes/IA, segurança, prevenção de abuso/fraude, suporte e melhoria contínua.

                3. Bases legais (LGPD): execução de contrato, consentimento quando exigido, legítimo interesse para segurança/estabilidade e cumprimento de obrigação legal.

                4. Permissões: câmera, localização e notificações são usadas somente com autorização e podem ser revogadas a qualquer momento no dispositivo.

                5. IA e provedores técnicos: a Zellu AI pode usar dados cadastrados e mensagens do chat para responder e preparar ações. Quando recursos online estiverem habilitados, o conteúdo necessário pode ser processado por provedores técnicos de infraestrutura e/ou IA.

                6. Compartilhamento: não vendemos dados pessoais. Podemos compartilhar com operadores/provedores técnicos necessários ao funcionamento do app e com autoridades quando houver obrigação legal.

                7. Retenção e armazenamento: parte dos dados pode ficar no dispositivo e parte em nuvem, pelo tempo necessário às finalidades e obrigações legais.

                8. Direitos do titular: você pode solicitar confirmação de tratamento, acesso, correção, anonimização, exclusão e revogação do consentimento, nos termos da LGPD.

                9. Exclusão de conta e dados: ao solicitar exclusão, removemos dados pessoais e registros vinculados, ressalvadas retenções legais obrigatórias.

                10. Transferência internacional: alguns provedores podem processar dados fora do Brasil, com salvaguardas adequadas.

                11. Contato oficial de privacidade, remoção de dados, dúvidas e suporte:
                guilhermedevsistemas@gmail.com
                Páginas oficiais:
                https://zellu-privacidade.vercel.app/privacy-policy.html
                https://zellu-privacidade.vercel.app/terms-of-use.html
            """.trimIndent(),
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
            text = { Text("Agora temos 2 planos: Lite (R$ 10,50) para Viagens e Frota (R$ 29,90) com tudo, incluindo o sistema de estoque.", color = Color(0xFFCBD5E1)) },
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
                        "Escolha entre Lite para Viagens ou Frota para liberar todos os recursos do app.",
                        color = premiumText,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Lite - R$ 10,50/mês", color = premiumTitle, fontWeight = FontWeight.Bold)
                            Text(
                                "Inclui somente Viagens: registro de gastos e relatórios por trajeto.",
                                color = premiumSubtitle,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Frota - R$ 29,90/mês", color = premiumTitle, fontWeight = FontWeight.Bold)
                            Text(
                                "Inclui tudo: Viagens + gestão completa de frota + sistema de estoque.",
                                color = premiumSubtitle,
                                fontSize = 13.sp
                            )
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
            autoScrollToCompletedMaintenance = autoScrollReportToMaintenance,
            onDismiss = { showCarInfoScreen = false }
        )
        autoScrollReportToMaintenance = false
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
        HistoricoAbastecimentoScreen(
            carroId = carroAtual.id,
            isPremium = planTier != PlanTier.FREE,
            onDismiss = { showHistoricoAbastecimentoScreen = false }
        )
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
                val hadNoReminderBefore = todosLembretes.none { it.tipo != TipoManutencao.ABASTECIMENTO }
                todosLembretes = todosLembretes + novo.copy(carroId = carroAtual.id)
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
                val hadNoReminderBefore = todosLembretes.none { it.tipo != TipoManutencao.ABASTECIMENTO }
                val novosLembretes = novosItens.map { it.copy(carroId = carroAtual.id) }
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
                        modifier = Modifier
                            .statusBarsPadding()
                            .offset(y = (-3).dp),
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

                    Spacer(Modifier.height(0.dp))

                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .onGloballyPositioned { carInfoRect = it.boundsInRoot() }
                    ) {
                        CarroInfoCard(
                            carroAtual = carroAtual,
                            onPrevCar = {
                                if (indiceCarroAtual > 0) indiceCarroAtual-- else indiceCarroAtual = listaCarros.lastIndex
                            },
                            onNextCar = {
                                if (indiceCarroAtual < listaCarros.lastIndex) indiceCarroAtual++ else indiceCarroAtual = 0
                            },
                            onOpenCarInfo = { showGaragemScreen = true },
                            onEditCar = { showEditCarScreen = true },
                            onOpenRelatorio = {
                                autoScrollReportToMaintenance = false
                                showCarInfoScreen = true
                            },
                            onOpenFuelHistory = { showHistoricoAbastecimentoScreen = true },
                            showFuelHistoryAction = !isBikeCategory(carroAtual.tipoVeiculo),
                            onNovoLembrete = {
                                iniciarCameraProduto = false
                                fluxoInicialRegistroServico = null
                                showFluxoCadastroDialog = true
                            },
                            onEditButtonPositioned = { editCarButtonRect = it },
                            onReportButtonPositioned = { reportButtonRect = it },
                            onNewReminderButtonPositioned = { newReminderButtonRect = it },
                            nomeMantedor = nomeExibido,
                            textLight = textLight,
                            accentBlue = accentBlue
                        )
                    }

                    Spacer(Modifier.height((-4).dp))

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
                    AvisosCategoriasCard(
                        lembretesDoCarroAtual = lembretesAtivosDoCarroAtual,
                        lembretesComBusca = lembretesComBusca,
                        buscaTexto = buscaTexto,
                        onBuscar = { buscaTexto = it },
                        listaContatos = listaContatos,
                        modeloCarro = carroAtual.nome,
                        filtroTipo = filtroTipo,
                        onFiltroTipoChange = { filtroTipo = it },
                        categoriasDisponiveis = categoriasDisponiveis,
                        iconOverrides = iconOverrides,
                        labelOverrides = labelOverrides,
                        onDelete = { lembrete ->
                            NotificacaoHelper.cancelarNotificacao(context.applicationContext, lembrete.id)
                            todosLembretes = todosLembretes.filter { it.id != lembrete.id }
                        },
                        onAddPrestador = { lembrete ->
                            lembreteParaVincularContato = lembrete.id
                            showSelecionarPrestadorScreen = true
                        },
                        onOpenDetalhes = { lembrete ->
                            lembreteSelecionado = lembrete
                            contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                            showLembreteDetalhesScreen = true
                        },
                        statusLabel = { textoStatusPrazoLocal(it) },
                        statusColor = { tipo -> calcularCorStatusLocal(lembretesAtivosDoCarroAtual, tipo) },
                        textDim = textDim,
                        accentColor = carroAtual.getCorUI(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .onGloballyPositioned { remindersRect = it.boundsInRoot() }
                    )

                    Spacer(Modifier.height(24.dp))

                    FreePlanAdBanner(
                        isPremium = planTier != PlanTier.FREE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(80.dp))

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
                    message = "Toque em Relatório para abrir a tela e ir direto em Registros cadastrados, onde fica o que você salvou.",
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
                        autoScrollReportToMaintenance = true
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
                val nome = carro.nome.ifBlank {
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

// ----------------- COMPONENTES AUXILIARES LOCAIS -----------------

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val container = when {
        highlighted && isDark -> Color(0xFF3B2A0A)
        highlighted && !isDark -> Color(0xFFFEF3C7)
        isDark -> Color(0xFF111827)
        else -> Color(0xFFF1F5F9)
    }
    val borderColor = when {
        highlighted -> Color(0xFFFBBF24)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> colorScheme.outlineVariant.copy(alpha = 0.9f)
    }
    val iconTint = when {
        highlighted -> Color(0xFFF59E0B)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }
    val textColor = when {
        highlighted && isDark -> Color(0xFFFEF3C7)
        highlighted && !isDark -> Color(0xFF92400E)
        isDark -> Color(0xFFF1F5F9)
        else -> Color(0xFF0F172A)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.Black.copy(alpha = 0.18f) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)), // Surface Dark
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6), // Blue 500
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Color(0xFFE2E8F0), // Slate 200
                fontSize = 14.sp
            )
        }
    }
}


@Composable
private fun HomeTutorialSpotlightOverlay(
    targetRect: Rect?,
    message: String,
    step: Int,
    total: Int,
    targetCornerRadius: Dp,
    accentBlue: Color,
    stepIcon: ImageVector,
    stepTitle: String,
    onClose: () -> Unit,
    onNext: () -> Unit
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { targetCornerRadius.toPx() }
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val inset = strokeWidthPx / 2f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawRect(Color.Black.copy(alpha = 0.55f))
                if (targetRect != null) {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = androidx.compose.ui.geometry.Offset(targetRect.left, targetRect.top),
                        size = androidx.compose.ui.geometry.Size(targetRect.width, targetRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = Color(0xFF60A5FA),
                        topLeft = androidx.compose.ui.geometry.Offset(targetRect.left + inset, targetRect.top + inset),
                        size = androidx.compose.ui.geometry.Size(targetRect.width - (inset * 2f), targetRect.height - (inset * 2f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerRadiusPx - inset).coerceAtLeast(0f), (cornerRadiusPx - inset).coerceAtLeast(0f)),
                        style = Stroke(width = strokeWidthPx)
                    )
                }
                drawContent()
            }
            .clickable(enabled = true) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .heightIn(min = 220.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF0B1220)
                            )
                        )
                    )
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1D4ED8).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = stepTitle,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "Etapa $step de $total",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    color = Color(0xFF111827),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF64748B)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            text = "Fechar tutorial",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Próximo",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickStartDialog(
    step: Int,
    pages: List<Triple<ImageVector, String, String>>,
    onNext: () -> Unit,
    onDemoCreateReminder: () -> Unit
) {
    if (pages.isEmpty()) return
    val safeStep = step.coerceIn(0, pages.lastIndex)
    val (icon, title, body) = pages[safeStep]
    val isLast = safeStep == pages.lastIndex
    val progress = (safeStep + 1f) / pages.size.toFloat()
    val scheme = MaterialTheme.colorScheme
    val cardBg = Color(0xFF0B1220)
    val panelBg = Color(0xFF101A2B)
    val borderColor = Color(0xFF334155)
    val titleColor = Color(0xFFE2E8F0)
    val bodyColor = Color(0xFFCBD5E1)
    val dimColor = Color(0xFF94A3B8)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBg
            ),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            scheme.primary.copy(alpha = 0.4f),
                                            scheme.secondary.copy(alpha = 0.28f)
                                        )
                                    )
                                )
                                .border(1.dp, scheme.primary.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Guia rápido",
                                color = titleColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Passo ${safeStep + 1} de ${pages.size}",
                                color = dimColor,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = scheme.primary,
                    trackColor = borderColor.copy(alpha = 0.45f)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = panelBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = title,
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = body,
                            color = bodyColor,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == safeStep) 20.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (index == safeStep) scheme.primary
                                    else scheme.outline.copy(alpha = 0.35f)
                                )
                        )
                    }
                }

                if (isLast) {
                    OutlinedButton(
                        onClick = onDemoCreateReminder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Fazer demonstração")
                    }
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (isLast) "Concluir guia" else "Próximo",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
// ----------------- NOVO COMPONENTE LEMBRETE CARD (PREMIUM) -----------------
@Composable
fun LembreteCardLocal(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    modeloCarro: String,
    onDelete: () -> Unit,
    onAddPrestador: () -> Unit,
    onClick: () -> Unit,
    statusLabel: String,
    statusColor: Color
) {
    val context = LocalContext.current
    val bg = Color(0xFF111827)
    val bg2 = Color(0xFF0B1224)
    val stroke = Color(0xFF23324D)
    val text = Color(0xFFF1F5F9)
    val dim = Color(0xFF94A3B8)

    // LÃ³gica para formatar o KM
    val kmFormatado = remember(lembrete.kmLimite) {
        val apenasDigitos = lembrete.kmLimite.filter { it.isDigit() }
        apenasDigitos.toLongOrNull()?.let {
            java.text.NumberFormat.getInstance(java.util.Locale("pt", "BR")).format(it)
        } ?: lembrete.kmLimite.ifBlank { "-" }
    }

    val iconBg = Brush.linearGradient(
        colors = listOf(
            statusColor.copy(alpha = 0.28f),
            statusColor.copy(alpha = 0.10f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, stroke),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(bg, bg2)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-2).dp)
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconBg)
                            .border(1.dp, statusColor.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        TipoIcon(
                            tipo = lembrete.tipo,
                            tint = statusColor,
                            size = 22.dp
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lembrete.titulo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (lembrete.peca.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Peça: ${lembrete.peca}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (lembrete.valor > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = formatarMoedaLocal(lembrete.valor),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- INFO CHIPS ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dataOuKm = when {
                        lembrete.dataLimite.isNotBlank() -> lembrete.dataLimite
                        lembrete.kmLimite.isNotBlank() -> kmFormatado + " km"
                        else -> "Sem meta"
                    }

                    // 1. Data
                    InfoMini(
                        icon = Icons.Rounded.CalendarMonth,
                        text = dataOuKm,
                        tint = dim,
                        iconTint = statusColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // 2. Status/Prazo
                    val statusIcon = when (statusLabel) {
                        "No Prazo", "Hoje" -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.ErrorOutline
                    }
                    val statusIconColor = when (statusLabel) {
                        "No Prazo", "Hoje" -> Color(0xFF10B981)
                        "Urgente" -> Color(0xFFF59E0B)
                        "Vencido" -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }
                    InfoMini(
                        icon = statusIcon,
                        text = statusLabel,
                        tint = dim,
                        iconTint = statusColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // 3. KM
                    InfoMini(
                        icon = Icons.Rounded.Speed,
                        text = "KM: $kmFormatado",
                        tint = dim,
                        iconTint = statusColor,
                        ellipsize = false
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                if (contato != null && contato.telefone.isNotBlank()) {
                    Button(
                        onClick = {
                            abrirWhatsApp(
                                context,
                                contato.telefone,
                                "Olá tudo bem?"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Chamar no Whatszap",
                            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (contato != null) {
                    Button(
                        onClick = onAddPrestador,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr("Adicionar telefone", "Add phone"),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Button(
                        onClick = onAddPrestador,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14532D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFFD1FAE5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr("Adicionar prestador do servico", "Add service provider"),
                            color = Color(0xFFD1FAE5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMini(
    icon: ImageVector,
    text: String,
    tint: Color,
    iconTint: Color = tint,
    ellipsize: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B1224))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
            .widthIn(min = 72.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign =  TextAlign.Center,
            maxLines = 1,
            overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}

@Composable
private fun ValorPill(valor: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF052E2B)) // verde escuro elegante
            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = valor,
            color = Color(0xFF34D399),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun BadgeStatus(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembreteDetalhesScreen(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    carro: CarroInfo,
    onDismiss: () -> Unit,
    onDelete: (Lembrete) -> Unit,
    onMarkAsDone: (Lembrete) -> Unit,
    onFinalizeAndClose: (Lembrete) -> Unit,
    onSalvar: (Lembrete) -> Unit,
    onAddPrestador: (Lembrete) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val appBarBg = if (isDark) Color.Black else colorScheme.surface
    val screenBg = if (isDark) Color.Black else colorScheme.background
    val cardBg = if (isDark) Color(0xFF111827) else colorScheme.surface
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val textPrimary = colorScheme.onSurface
    val textSecondary = colorScheme.onSurfaceVariant
    val englishUi = isEnglishUi()
    fun extrairValorNumericoLinhaResumo(texto: String, matcher: (String) -> Boolean): String? {
        val linhaResumo = texto
            .lines()
            .map { it.trim() }
            .firstOrNull {
                matcher(it.lowercase(Locale.getDefault()))
            } ?: return null
        val bruto = linhaResumo.substringAfter(':', "").trim().ifBlank { linhaResumo }
        val numero = Regex("""\d{1,3}(?:\.\d{3})*(?:,\d{1,2})|\d+(?:[.,]\d{1,2})?""")
            .find(bruto)
            ?.value
            ?: return null
        return numero.replace(".", "").replace(",", ".")
    }
    fun atualizarDescricaoComResumoFinanceiro(
        descricaoAtual: String,
        totalBruto: Double?,
        valorFinal: Double?
    ): String {
        val linhasBase = descricaoAtual
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot {
                val n = it.lowercase(Locale.getDefault())
                n.startsWith("total") ||
                    n.startsWith("valor total") ||
                    n.contains("desconto") ||
                    n.contains("valor final") ||
                    n.contains("valor a pagar")
            }
            .toMutableList()
        val totalNormalizado = totalBruto?.takeIf { it >= 0.0 }
        val finalNormalizado = valorFinal?.takeIf { it >= 0.0 } ?: totalNormalizado
        if (totalNormalizado != null) {
            linhasBase += "Valor total: R$ ${String.format(Locale.US, "%.2f", totalNormalizado)}"
        }
        if (totalNormalizado != null || finalNormalizado != null) {
            val descontoCalculado = ((totalNormalizado ?: 0.0) - (finalNormalizado ?: 0.0)).coerceAtLeast(0.0)
            linhasBase += "Desconto: R$ ${String.format(Locale.US, "%.2f", descontoCalculado)}"
        }
        if (finalNormalizado != null) {
            linhasBase += "Valor final: R$ ${String.format(Locale.US, "%.2f", finalNormalizado)}"
        }
        return linhasBase.joinToString("\n")
    }

    var editando by remember(lembrete.id) { mutableStateOf(false) }
    var titulo by remember(lembrete.id) { mutableStateOf(lembrete.titulo) }
    var tipoSelecionadoEdicao by remember(lembrete.id) { mutableStateOf(lembrete.tipo) }
    var descricaoEdicao by remember(lembrete.id) { mutableStateOf(lembrete.peca) }
    var dataAviso by remember(lembrete.id) { mutableStateOf(lembrete.dataLimite) }
    var horaAviso by remember(lembrete.id) { mutableStateOf(lembrete.horaAviso) }
    var kmLimite by remember(lembrete.id) { mutableStateOf(lembrete.kmLimite) }
    var repetirAviso by remember(lembrete.id) { mutableStateOf(false) }
    var recorrenciaUnit by remember(lembrete.id) { mutableStateOf(NotificacaoHelper.REC_UNIT_DAY) }
    var recorrenciaIntervaloTexto by remember(lembrete.id) { mutableStateOf("1") }
    var menuRecorrenciaExpanded by remember(lembrete.id) { mutableStateOf(false) }
    var menuTipoExpanded by remember(lembrete.id) { mutableStateOf(false) }
    var showConfirmarFeitoDialog by remember(lembrete.id) { mutableStateOf(false) }
    var showFinalizarEncerrarDialog by remember(lembrete.id) { mutableStateOf(false) }
    var showConfirmarExclusaoDialog by remember(lembrete.id) { mutableStateOf(false) }
    fun abrirSeletorDataAviso() {
        val formatadorData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dataAtual = runCatching { LocalDate.parse(dataAviso, formatadorData) }.getOrElse { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, ano, mes, dia ->
                dataAviso = String.format(Locale.US, "%02d/%02d/%04d", dia, mes + 1, ano)
            },
            dataAtual.year,
            dataAtual.monthValue - 1,
            dataAtual.dayOfMonth
        ).show()
    }
    fun abrirSeletorHoraAviso() {
        val formatadorHora = DateTimeFormatter.ofPattern("HH:mm")
        val horaAtual = runCatching { LocalTime.parse(horaAviso, formatadorHora) }.getOrElse { LocalTime.of(9, 0) }
        TimePickerDialog(
            context,
            { _, hora, minuto ->
                horaAviso = String.format(Locale.US, "%02d:%02d", hora, minuto)
            },
            horaAtual.hour,
            horaAtual.minute,
            true
        ).show()
    }
    val tipoPermiteRepeticaoEdicao = tipoSelecionadoEdicao != TipoManutencao.LICENCIAMENTO &&
        tipoSelecionadoEdicao != TipoManutencao.SEGURO &&
        tipoSelecionadoEdicao != TipoManutencao.IPVA &&
        tipoSelecionadoEdicao != TipoManutencao.ABASTECIMENTO
    val tipoPermiteRepeticao = lembrete.tipo != TipoManutencao.LICENCIAMENTO &&
        lembrete.tipo != TipoManutencao.SEGURO &&
        lembrete.tipo != TipoManutencao.IPVA &&
        lembrete.tipo != TipoManutencao.ABASTECIMENTO
    fun textoRecorrencia(unit: String, interval: Int): String {
        val intervaloValido = interval.coerceAtLeast(1)
        return when (unit) {
            NotificacaoHelper.REC_UNIT_DAY -> if (intervaloValido == 1) {
                if (englishUi) "Every 1 day" else "A cada 1 dia"
            } else {
                if (englishUi) "Every $intervaloValido days" else "A cada $intervaloValido dias"
            }
            NotificacaoHelper.REC_UNIT_MONTH -> if (intervaloValido == 1) {
                if (englishUi) "Every 1 month" else "A cada 1 mes"
            } else {
                if (englishUi) "Every $intervaloValido months" else "A cada $intervaloValido meses"
            }
            NotificacaoHelper.REC_UNIT_YEAR -> if (intervaloValido == 1) {
                if (englishUi) "Every 1 year" else "A cada 1 ano"
            } else {
                if (englishUi) "Every $intervaloValido years" else "A cada $intervaloValido anos"
            }
            else -> if (englishUi) "Do not repeat" else "Nao repetir"
        }
    }
    val descricaoRecorrenciaAtual = if (!tipoPermiteRepeticao || !repetirAviso) {
        if (englishUi) "No" else "Nao"
    } else {
        (if (englishUi) "Yes" else "Sim") + " (${textoRecorrencia(recorrenciaUnit, recorrenciaIntervaloTexto.toIntOrNull() ?: 1)})"
    }
    val detalhesDateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val dataBaseLembrete = remember(lembrete.dataLimite) {
        runCatching { LocalDate.parse(lembrete.dataLimite, detalhesDateFormatter) }.getOrNull()
    }
    val statusDetalhe = remember(lembrete.id, lembrete.dataLimite, lembrete.estabelecimentoEndereco) {
        when {
            isLembreteRealizado(lembrete) -> if (englishUi) "Completed" else "Concluído"
            dataBaseLembrete == null -> if (englishUi) "Active" else "Ativo"
            dataBaseLembrete.isBefore(LocalDate.now()) -> if (englishUi) "Overdue" else "Vencido"
            dataBaseLembrete.isEqual(LocalDate.now()) -> if (englishUi) "Due today" else "Vence hoje"
            else -> if (englishUi) "Active" else "Ativo"
        }
    }
    val statusVencido = remember(lembrete.id, lembrete.dataLimite, lembrete.estabelecimentoEndereco) {
        !isLembreteRealizado(lembrete) && dataBaseLembrete?.isBefore(LocalDate.now()) == true
    }
    val proximoLembreteTexto = run {
        if (isLembreteRealizado(lembrete)) {
            tr("Concluído", "Completed")
        } else {
            val base = dataBaseLembrete
            if (base == null) {
                tr("Não definido", "Not set")
            } else if (tipoPermiteRepeticao && repetirAviso) {
                val intervalo = (recorrenciaIntervaloTexto.toIntOrNull() ?: 1).coerceAtLeast(1)
                val proximaData = when (recorrenciaUnit) {
                    NotificacaoHelper.REC_UNIT_DAY -> base.plusDays(intervalo.toLong())
                    NotificacaoHelper.REC_UNIT_MONTH -> base.plusMonths(intervalo.toLong())
                    NotificacaoHelper.REC_UNIT_YEAR -> base.plusYears(intervalo.toLong())
                    else -> base
                }
                proximaData.format(detalhesDateFormatter)
            } else {
                base.format(detalhesDateFormatter)
            }
        }
    }
    val proximaDataDoFluxoAtual = remember(
        lembrete.id,
        lembrete.dataLimite,
        repetirAviso,
        recorrenciaUnit,
        recorrenciaIntervaloTexto
    ) {
        if (!repetirAviso || isLembreteRealizado(lembrete)) return@remember null
        val intervalo = (recorrenciaIntervaloTexto.toIntOrNull() ?: 1).coerceAtLeast(1)
        val base = runCatching { LocalDate.parse(lembrete.dataLimite, detalhesDateFormatter) }.getOrNull()
            ?: return@remember null
        val hoje = LocalDate.now()
        var proxima = base
        while (!proxima.isAfter(hoje)) {
            proxima = when (recorrenciaUnit) {
                NotificacaoHelper.REC_UNIT_DAY -> proxima.plusDays(intervalo.toLong())
                NotificacaoHelper.REC_UNIT_WEEK -> proxima.plusWeeks(intervalo.toLong())
                NotificacaoHelper.REC_UNIT_MONTH -> proxima.plusMonths(intervalo.toLong())
                NotificacaoHelper.REC_UNIT_YEAR -> proxima.plusYears(intervalo.toLong())
                else -> proxima.plusDays(intervalo.toLong())
            }
        }
        proxima.format(detalhesDateFormatter)
    }

    LaunchedEffect(lembrete.id) {
        val recorrenciaAtual = NotificacaoHelper.obterRecorrencia(context.applicationContext, lembrete.id)
        repetirAviso = recorrenciaAtual != null && tipoPermiteRepeticao
        recorrenciaUnit = recorrenciaAtual?.unit ?: NotificacaoHelper.REC_UNIT_DAY
        recorrenciaIntervaloTexto = (recorrenciaAtual?.interval ?: 1).coerceAtLeast(1).toString()
    }

    val resetarEdicao = {
        titulo = lembrete.titulo
        tipoSelecionadoEdicao = lembrete.tipo
        descricaoEdicao = lembrete.peca
        dataAviso = lembrete.dataLimite
        horaAviso = lembrete.horaAviso
        kmLimite = lembrete.kmLimite
        val recorrenciaAtual = NotificacaoHelper.obterRecorrencia(context.applicationContext, lembrete.id)
        repetirAviso = recorrenciaAtual != null && tipoPermiteRepeticao
        recorrenciaUnit = recorrenciaAtual?.unit ?: NotificacaoHelper.REC_UNIT_DAY
        recorrenciaIntervaloTexto = (recorrenciaAtual?.interval ?: 1).coerceAtLeast(1).toString()
        menuRecorrenciaExpanded = false
        menuTipoExpanded = false
    }
    val categoriaColor = remember(lembrete.tipo, statusVencido) {
        if (statusVencido) {
            Color(0xFFDC2626)
        } else {
            when (lembrete.tipo) {
                TipoManutencao.OLEO -> Color(0xFF2563EB)
                TipoManutencao.ABASTECIMENTO -> Color(0xFF0EA5E9)
                TipoManutencao.LAVAGEM -> Color(0xFF06B6D4)
                TipoManutencao.FREIO -> Color(0xFFDC2626)
                TipoManutencao.PNEU -> Color(0xFFF59E0B)
                TipoManutencao.BATERIA -> Color(0xFF0EA5E9)
                TipoManutencao.VIDROS -> Color(0xFF38BDF8)
                TipoManutencao.FUNILARIA -> Color(0xFFF97316)
                TipoManutencao.SEGURO, TipoManutencao.LICENCIAMENTO, TipoManutencao.IPVA -> Color(0xFF16A34A)
                else -> Color(0xFF6366F1)
            }
        }
    }

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = textPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (editando) {
                                    resetarEdicao()
                                    editando = false
                                } else {
                                    editando = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (editando) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (editando) tr("Cancelar edição", "Cancel editing") else tr("Editar", "Edit"),
                                tint = if (editando) {
                                    Color(0xFFEF4444)
                                } else if (isDark) {
                                    Color.White
                                } else {
                                    Color(0xFF2563EB)
                                }
                            )
                        }
                        if (!editando) {
                            IconButton(onClick = { onAddPrestador(lembrete) }) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = tr("Adicionar prestador", "Add provider"),
                                    tint = if (isDark) Color.White else Color(0xFF2563EB)
                                )
                            }
                            IconButton(onClick = { showConfirmarExclusaoDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = tr("Apagar aviso", "Delete reminder"),
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(categoriaColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                                        .border(1.dp, categoriaColor.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TipoIcon(tipo = lembrete.tipo, tint = categoriaColor, size = 22.dp)
                                    if (statusVencido) {
                                        Text(
                                            text = "!",
                                            color = Color(0xFFDC2626),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-3).dp)
                                        )
                                    }
                                }
                                if (statusVencido) {
                                    Text(
                                        text = tr("Vencido", "Overdue"),
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text(
                                    text = abreviarTituloAvisoDetalhes(titulo.ifBlank { lembrete.titulo }),
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = if (statusVencido) 0.dp else 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = cardBorder)

                        if (editando) {
                            val tiposDisponiveisEdicao = remember(carro.tipoVeiculo, lembrete.tipo) {
                                (tiposAvisoPorVeiculo(carro.tipoVeiculo) + lembrete.tipo)
                                    .distinct()
                                    .filter { it != TipoManutencao.ABASTECIMENTO || showFuelReminder(carro.tipoVeiculo) }
                            }

                            EditReminderSection(
                                title = tr("Identificação", "Identification"),
                                icon = Icons.Default.Edit,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary
                            ) {
                                OutlinedTextField(
                                    value = titulo,
                                    onValueChange = { titulo = it },
                                    label = { Text(tr("Título", "Title")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ExposedDropdownMenuBox(
                                    expanded = menuTipoExpanded,
                                    onExpandedChange = { menuTipoExpanded = !menuTipoExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = tipoSelecionadoEdicao.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(tr("Categoria", "Category")) },
                                        leadingIcon = {
                                            TipoIcon(
                                                tipo = tipoSelecionadoEdicao,
                                                tint = corCategoria(tipoSelecionadoEdicao),
                                                size = 18.dp
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuTipoExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = menuTipoExpanded,
                                        onDismissRequest = { menuTipoExpanded = false }
                                    ) {
                                        tiposDisponiveisEdicao.forEach { tipo ->
                                            DropdownMenuItem(
                                                text = { Text(tipo.label) },
                                                leadingIcon = {
                                                    TipoIcon(
                                                        tipo = tipo,
                                                        tint = corCategoria(tipo),
                                                        size = 18.dp
                                                    )
                                                },
                                                onClick = {
                                                    tipoSelecionadoEdicao = tipo
                                                    menuTipoExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = descricaoEdicao,
                                    onValueChange = { descricaoEdicao = it },
                                    label = { Text(tr("Descrição / peça / observações", "Description / item / notes")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    maxLines = 10
                                )
                            }

                            EditReminderSection(
                                title = tr("Quando avisar", "When to notify"),
                                icon = Icons.Default.Event,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dataAviso,
                                        onValueChange = {},
                                        label = { Text(tr("Data", "Date")) },
                                        trailingIcon = {
                                            IconButton(onClick = { abrirSeletorDataAviso() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Event,
                                                    contentDescription = tr("Selecionar data", "Select date"),
                                                    tint = textSecondary
                                                )
                                            }
                                        },
                                        readOnly = true,
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { abrirSeletorDataAviso() }
                                    )
                                    OutlinedTextField(
                                        value = horaAviso,
                                        onValueChange = {},
                                        label = { Text(tr("Hora", "Time")) },
                                        trailingIcon = {
                                            IconButton(onClick = { abrirSeletorHoraAviso() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = tr("Selecionar hora", "Select time"),
                                                    tint = textSecondary
                                                )
                                            }
                                        },
                                        readOnly = true,
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { abrirSeletorHoraAviso() }
                                    )
                                }
                                OutlinedTextField(
                                    value = kmLimite,
                                    onValueChange = { kmLimite = it.filter(Char::isDigit) },
                                    label = { Text(tr("KM limite", "Mileage limit")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (tipoPermiteRepeticaoEdicao) {
                                EditReminderSection(
                                    title = tr("Repetição", "Repeat"),
                                    icon = Icons.Default.Repeat,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { repetirAviso = !repetirAviso }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = repetirAviso,
                                            onCheckedChange = { repetirAviso = it }
                                        )
                                        Text(
                                            text = tr("Repetir esse aviso", "Repeat this reminder"),
                                            color = textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (repetirAviso) {
                                        ExposedDropdownMenuBox(
                                            expanded = menuRecorrenciaExpanded,
                                            onExpandedChange = { menuRecorrenciaExpanded = !menuRecorrenciaExpanded },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = textoRecorrencia(recorrenciaUnit, recorrenciaIntervaloTexto.toIntOrNull() ?: 1),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text(tr("Frequência da repetição", "Repeat frequency")) },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth(),
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuRecorrenciaExpanded)
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = menuRecorrenciaExpanded,
                                                onDismissRequest = { menuRecorrenciaExpanded = false }
                                            ) {
                                                listOf(
                                                    NotificacaoHelper.REC_UNIT_DAY to tr("Dias", "Days"),
                                                    NotificacaoHelper.REC_UNIT_MONTH to tr("Meses", "Months"),
                                                    NotificacaoHelper.REC_UNIT_YEAR to tr("Anos", "Years")
                                                ).forEach { (unitKey, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            recorrenciaUnit = unitKey
                                                            menuRecorrenciaExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = recorrenciaIntervaloTexto,
                                            onValueChange = { recorrenciaIntervaloTexto = it.filter(Char::isDigit).take(2) },
                                            label = {
                                                Text(
                                                    when (recorrenciaUnit) {
                                                        NotificacaoHelper.REC_UNIT_DAY -> tr("Repetir a cada quantos dias?", "Repeat every how many days?")
                                                        NotificacaoHelper.REC_UNIT_MONTH -> tr("Repetir a cada quantos meses?", "Repeat every how many months?")
                                                        else -> tr("Repetir a cada quantos anos?", "Repeat every how many years?")
                                                    }
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val descricaoAviso = lembrete.peca.ifBlank { tr("Sem descrição informada.", "No description provided.") }
                            val linhasDescricao = descricaoAviso
                                .lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            fun isLinhaResumoFinanceiro(linha: String): Boolean {
                                val normalized = linha.lowercase(Locale.getDefault())
                                return normalized.contains("valor total") ||
                                    normalized.startsWith("total") ||
                                    normalized.contains("desconto") ||
                                    normalized.contains("valor final") ||
                                    normalized.contains("valor a pagar") ||
                                    normalized.contains("troco")
                            }
                            fun valorAposDoisPontos(linha: String?): String? {
                                if (linha.isNullOrBlank()) return null
                                val extraido = linha.substringAfter(':', "").trim()
                                return extraido.takeIf { it.isNotBlank() }
                            }
                            fun parseMoeda(valor: String?): Double? {
                                if (valor.isNullOrBlank()) return null
                                val semSimbolo = valor
                                    .replace("R$", "", ignoreCase = true)
                                    .replace(" ", "")
                                val normalizado = if (semSimbolo.contains(',')) {
                                    semSimbolo.replace(".", "").replace(',', '.')
                                } else {
                                    semSimbolo
                                }
                                return normalizado.toDoubleOrNull()
                            }
                            fun extrairMoedaDaLinha(linha: String?): Double? {
                                if (linha.isNullOrBlank()) return null
                                val regexMoeda = Regex("""-?\s*R?\$?\s*\d{1,3}(?:\.\d{3})*(?:,\d{1,2})|-?\s*R?\$?\s*\d+(?:[.,]\d{1,2})?""")
                                val match = regexMoeda.find(linha) ?: return null
                                return parseMoeda(match.value)
                            }
                            val itensDoAviso = linhasDescricao.filterNot(::isLinhaResumoFinanceiro)
                            val linhaTotal = linhasDescricao.firstOrNull {
                                val normalized = it.lowercase(Locale.getDefault())
                                normalized.contains("valor total") || normalized.startsWith("total")
                            }
                            val linhaDesconto = linhasDescricao.firstOrNull {
                                it.lowercase(Locale.getDefault()).contains("desconto")
                            }
                            val linhaValorFinal = linhasDescricao.firstOrNull {
                                val normalized = it.lowercase(Locale.getDefault())
                                normalized.contains("valor final") || normalized.contains("valor a pagar")
                            }
                            val totalExtraido = valorAposDoisPontos(linhaTotal)
                            val descontoExtraido = valorAposDoisPontos(linhaDesconto)
                            val valorFinalExtraido = valorAposDoisPontos(linhaValorFinal)
                            val totalValor = parseMoeda(totalExtraido)
                                ?: extrairMoedaDaLinha(linhaTotal)
                                ?: lembrete.valor.takeIf { it > 0.0 }
                            val descontoValor = parseMoeda(descontoExtraido)
                                ?: extrairMoedaDaLinha(linhaDesconto)
                            val finalValor = parseMoeda(valorFinalExtraido)
                                ?: extrairMoedaDaLinha(linhaValorFinal)
                                ?: if (totalValor != null && descontoValor != null) {
                                    (totalValor - descontoValor).coerceAtLeast(0.0)
                                } else {
                                    totalValor
                                }
                            val mostrarResumoFinanceiro = totalValor != null || finalValor != null || descontoValor != null
                            val tabelaDados = buildList<Pair<String, String>> {
                                add(tr("Próximo lembrete", "Next reminder") to proximoLembreteTexto)
                                add(tr("Status", "Status") to statusDetalhe)
                                add(tr("Veículo", "Vehicle") to carro.nome)
                                add(tr("Hora", "Time") to lembrete.horaAviso.ifBlank { tr("Não definida", "Not set") })
                                add(tr("Repetição", "Repeat") to descricaoRecorrenciaAtual)
                                add(
                                    tr("Prestador", "Provider") to (
                                        contato?.let { "${it.nome} (${it.tipoServico})" }
                                            ?: tr("Não definido", "Not set")
                                    )
                                )
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                                            .background(
                                                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                            )
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("Descrição do aviso", "Reminder description"),
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (itensDoAviso.isEmpty()) {
                                            Text(
                                                text = descricaoAviso,
                                                color = textPrimary,
                                                fontSize = 14.sp,
                                                lineHeight = 19.sp
                                            )
                                        } else {
                                            itensDoAviso.forEach { item ->
                                                Text(
                                                    text = item,
                                                    color = textPrimary,
                                                    fontSize = 14.sp,
                                                    lineHeight = 19.sp
                                                )
                                            }
                                        }
                                        if (mostrarResumoFinanceiro) {
                                            Spacer(Modifier.height(6.dp))
                                            HorizontalDivider(color = cardBorder.copy(alpha = 0.65f))
                                            Spacer(Modifier.height(6.dp))
                                            totalValor?.let {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(tr("Valor total", "Total amount"), color = textSecondary, fontSize = 12.sp)
                                                    Text(formatarMoedaLocal(it), color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            val descontoExibicao = descontoValor ?: 0.0
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(tr("Desconto", "Discount"), color = textSecondary, fontSize = 12.sp)
                                                val descontoTexto = if (descontoExibicao > 0.0) {
                                                    "- ${formatarMoedaLocal(descontoExibicao)}"
                                                } else {
                                                    formatarMoedaLocal(0.0)
                                                }
                                                val descontoColor = if (descontoExibicao > 0.0) Color(0xFFDC2626) else textPrimary
                                                Text(
                                                    descontoTexto,
                                                    color = descontoColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            finalValor?.let {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(tr("Valor final", "Final amount"), color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(formatarMoedaLocal(it), color = Color(0xFF16A34A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                                            .background(
                                                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                            )
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("Resumo do aviso", "Reminder summary"),
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                                    tabelaDados.forEachIndexed { index, (label, value) ->
                                        InfoTableRow(
                                            label = label,
                                            value = value,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary
                                        )
                                        if (index != tabelaDados.lastIndex) {
                                            HorizontalDivider(color = cardBorder.copy(alpha = 0.65f))
                                        }
                                    }
                                }
                            }
                        }
                }

                if (editando) {
                    Button(
                        onClick = {
                            val atualizado = lembrete.copy(
                                titulo = titulo.ifBlank { lembrete.titulo },
                                tipo = tipoSelecionadoEdicao,
                                peca = descricaoEdicao.ifBlank { lembrete.peca },
                                dataLimite = dataAviso.ifBlank { lembrete.dataLimite },
                                horaAviso = horaAviso.ifBlank { lembrete.horaAviso },
                                kmLimite = kmLimite
                            )
                            val intervaloRecorrencia = (recorrenciaIntervaloTexto.toIntOrNull() ?: 1).coerceAtLeast(1)
                            val atualizadoPermiteRepeticao = atualizado.tipo != TipoManutencao.LICENCIAMENTO &&
                                atualizado.tipo != TipoManutencao.SEGURO &&
                                atualizado.tipo != TipoManutencao.IPVA &&
                                atualizado.tipo != TipoManutencao.ABASTECIMENTO
                            if (atualizadoPermiteRepeticao && repetirAviso) {
                                NotificacaoHelper.salvarRecorrencia(
                                    context = context.applicationContext,
                                    lembreteId = atualizado.id,
                                    unit = recorrenciaUnit,
                                    interval = intervaloRecorrencia
                                )
                            } else {
                                NotificacaoHelper.removerRecorrencia(context.applicationContext, atualizado.id)
                            }
                            onSalvar(atualizado)
                            editando = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tr("Salvar edição", "Save edit"), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showConfirmarFeitoDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Marcar como realizado", "Mark as completed"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showFinalizarEncerrarDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tr("Finalizar e encerrar aviso", "Finalize and close reminder"),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
    if (showConfirmarFeitoDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmarFeitoDialog = false },
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
                            .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.24f else 0.14f))
                            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(27.dp)
                        )
                    }
                    Text(
                        text = tr("Marcar aviso como concluído?", "Mark reminder as completed?"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                if (proximaDataDoFluxoAtual != null) {
                    Text(
                        text = buildAnnotatedString {
                            append(
                                tr(
                                    "Isso conclui apenas este ciclo. A próxima data de aviso desse lembrete vai ser: ",
                                    "This only completes the current cycle. The next reminder date for this item will be: "
                                )
                            )
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary)) {
                                append(proximaDataDoFluxoAtual)
                            }
                        },
                        color = textSecondary
                    )
                } else {
                    Text(
                        text = tr("Você confirma que este aviso já foi resolvido?", "Do you confirm this reminder has been completed?"),
                        color = textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmarFeitoDialog = false
                        onMarkAsDone(lembrete)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
                ) {
                    Text(tr("Sim, concluir", "Yes, complete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmarFeitoDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text(tr("Voltar", "Back"))
                }
            },
            containerColor = cardBg
        )
    }
    if (showFinalizarEncerrarDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizarEncerrarDialog = false },
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
                            .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.24f else 0.14f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = tr("Finalizar e encerrar aviso?", "Finalize and close this reminder?"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Se você continuar, este aviso será encerrado de vez, mesmo que tenha repetição ativa. Você poderá criar outro depois, se quiser.",
                        "If you continue, this reminder will be permanently closed even if recurrence is active. You can create another one later if needed."
                    ),
                    color = textSecondary
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFinalizarEncerrarDialog = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Text(tr("Cancelar", "Cancel"))
                    }
                    Button(
                        onClick = {
                            showFinalizarEncerrarDialog = false
                            onFinalizeAndClose(lembrete)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                    ) {
                        Text(
                            text = tr("Sim, Finalizar", "Yes, Finalize"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            dismissButton = {},
            containerColor = cardBg
        )
    }
    if (showConfirmarExclusaoDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmarExclusaoDialog = false },
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
                            .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.24f else 0.14f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Text(
                        text = tr("Apagar este aviso?", "Delete this reminder?"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = tr("Essa ação remove o aviso permanentemente. Deseja continuar?", "This action permanently deletes the reminder. Do you want to continue?"),
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmarExclusaoDialog = false
                        onDelete(lembrete)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                ) {
                    Text(tr("Sim, apagar", "Yes, delete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmarExclusaoDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text(tr("Cancelar", "Cancel"))
                }
            },
            containerColor = cardBg
        )
    }
}

// ----------------- FUNÃ‡Ã•ES AUXILIARES DE ESTILO E LÃ“GICA -----------------

private fun isBikeCategory(tipoVeiculo: TipoVeiculo): Boolean =
    tipoVeiculo == TipoVeiculo.BICICLETA || tipoVeiculo == TipoVeiculo.BIKE_ELETRICA

private fun showFuelReminder(tipoVeiculo: TipoVeiculo): Boolean =
    tipoVeiculo != TipoVeiculo.BICICLETA &&
        tipoVeiculo != TipoVeiculo.BIKE_ELETRICA &&
        tipoVeiculo != TipoVeiculo.VEICULO_ELETRICO

private fun tiposAvisoPorVeiculo(tipoVeiculo: TipoVeiculo): List<TipoManutencao> = when (tipoVeiculo) {
    TipoVeiculo.BICICLETA -> listOf(
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.FREIO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.MECANICA,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.BIKE_ELETRICA -> listOf(
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.FREIO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.BATERIA,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.VEICULO_ELETRICO -> listOf(
        TipoManutencao.BATERIA,
        TipoManutencao.FREIO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.VIDROS,
        TipoManutencao.PNEU,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.REVISAO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.ONIBUS,
    TipoVeiculo.CAMINHAO,
    TipoVeiculo.VAN,
    TipoVeiculo.CAMINHONETE,
    TipoVeiculo.FURGAO,
    TipoVeiculo.HATCH,
    TipoVeiculo.MOTORHOME -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.FREIO,
        TipoManutencao.VIDROS,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.TRATOR -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.FREIO,
        TipoManutencao.MECANICA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.MOTO -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.FREIO,
        TipoManutencao.MECANICA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.CARRETINHA -> listOf(
        TipoManutencao.LAVAGEM,
        TipoManutencao.PNEU,
        TipoManutencao.MECANICA,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
    else -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.OLEO,
        TipoManutencao.VIDROS,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS
    )
}

fun calcularCorStatusLocal(lembretes: List<Lembrete>, tipo: TipoManutencao): Color {
    return when (tipo) {
        TipoManutencao.CORRENTE -> Color(0xFF22C55E) // verde
        TipoManutencao.LUBRIFICACAO -> Color(0xFF14B8A6) // verde-azulado
        TipoManutencao.PEDIVELA -> Color(0xFF0EA5E9) // azul
        TipoManutencao.ACESSORIOS -> Color(0xFFF97316) // laranja
        TipoManutencao.CONFORTO -> Color(0xFFEAB308) // amarelo
        TipoManutencao.PNEU -> Color(0xFFF59E0B) // laranja
        TipoManutencao.TRANSMISSAO -> Color(0xFF60A5FA) // azul claro
        TipoManutencao.REVISAO -> Color(0xFF8B5CF6) // roxo
        TipoManutencao.OLEO -> Color(0xFF3B82F6) // Azul
        TipoManutencao.ABASTECIMENTO -> Color(0xFF0EA5E9) // azul ciano
        TipoManutencao.LAVAGEM -> Color(0xFF06B6D4) // azul agua
        TipoManutencao.FREIO -> Color(0xFFEF4444) // Vermelho
        TipoManutencao.VIDROS -> Color(0xFF38BDF8) // azul vidro
        TipoManutencao.MECANICA -> Color(0xFFF59E0B) // Laranja
        TipoManutencao.FUNILARIA -> Color(0xFFF97316) // Laranja escuro
        TipoManutencao.LICENCIAMENTO -> Color(0xFF10B981) // Verde
        TipoManutencao.SEGURO -> Color(0xFF22C55E) // Verde claro
        else -> Color(0xFF6366F1) // Roxo padrÃ£o
    }
}

fun textoStatusPrazoLocal(lembrete: Lembrete): String {
    val hoje = LocalDate.now()
    val data = dataParaOrdenacao(lembrete)
    if (data == LocalDate.MAX) return "Acompanhar KM"
    val dias = ChronoUnit.DAYS.between(hoje, data)
    return when {
        dias < 0 -> "Vencido"
        dias == 0L -> "Hoje"
        dias <= 7 -> "Urgente"
        else -> "No Prazo"
    }
}

fun formatarMoedaLocal(valor: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
}

private fun abreviarTituloAvisoDetalhes(texto: String, maxChars: Int = 34): String {
    val valor = texto.trim().ifBlank { "Aviso" }
    return if (valor.length <= maxChars) valor else valor.take(maxChars - 3) + "..."
}

// ----------------- OUTROS COMPONENTES DA TELA DE DETALHES -----------------


@Composable
private fun EditReminderSection(
    title: String,
    icon: ImageVector,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            content()
        }
    }
}


@Composable
private fun InfoRow(label: String, value: String, textLight: Color, textDim: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textDim, fontSize = 12.sp)
        Text(value, color = textLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoTableRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.44f)
        )
        Text(
            text = value,
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.56f)
        )
    }
}

@Composable
private fun AvisosNotificacoesScreen(
    notificacoes: List<NotificacaoDisparada>,
    onClear: () -> Unit,
    onRemove: (NotificacaoDisparada) -> Unit,
    resolveVehicleName: (NotificacaoDisparada) -> String?,
    canOpenNotification: (NotificacaoDisparada) -> Boolean,
    onOpen: (NotificacaoDisparada) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) Color(0xFF020917) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBgSoft = if (isDark) Color(0xFF0A1628) else Color(0xFFF1F5F9)
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFCBD5E1)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF64748B) else Color(0xFF64748B)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)
    val clearBg = if (isDark) Color(0xFF3B1A1A) else Color(0xFFFEE2E2)
    val clearIconTint = if (isDark) Color(0xFFFC8181) else Color(0xFFDC2626)
    val accentBlue = Color(0xFF60A5FA)
    val secondaryChipBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val chevronTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item("header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = tr("Notificações", "Notifications"),
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (notificacoes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(clearBg)
                                .clickable { onClear() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = tr("Limpar notificações", "Clear notifications"),
                                tint = clearIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                }
            }

            if (notificacoes.isEmpty()) {
                item("empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E3A5F) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = accentBlue,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            tr("Tudo em dia por aqui", "Everything is up to date"),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tr(
                                "Quando um aviso disparar, ele aparece aqui para você acompanhar com calma.",
                                "When a reminder is triggered, it will appear here."
                            ),
                            color = textDim,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                item("hint_card") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBgSoft)
                            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Swipe,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tr("Deslize para apagar um aviso", "Swipe to delete a notification"),
                            color = textSub,
                            fontSize = 12.sp
                        )
                    }
                }

                items(notificacoes, key = { "${it.id}_${it.timestamp}" }) { aviso ->
                    val canOpen = canOpenNotification(aviso)
                    val vehicleName = resolveVehicleName(aviso)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (aviso.id.startsWith("PARKING_")) {
                                return@rememberSwipeToDismissBoxState false
                            }
                            if (value != SwipeToDismissBoxValue.Settled) {
                                onRemove(aviso)
                            }
                            true
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                                listOf(Color(0xFFDC2626), Color(0xFFEF4444))
                                            else
                                                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                        )
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                    Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    Text(tr("Apagar", "Delete"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    ) {
                        val isParking = aviso.id.startsWith("PARKING_")
                        val chipColor = when {
                            isParking -> Color(0xFF22C55E)
                            canOpen -> Color(0xFF60A5FA)
                            else -> Color(0xFFA78BFA)
                        }
                        val chipLabel = when {
                            isParking -> tr("Estacionamento", "Parking")
                            canOpen -> tr("Aviso", "Reminder")
                            else -> tr("Informativo", "Info")
                        }
                        val notifIcon = when {
                            isParking -> Icons.Default.DirectionsCar
                            else -> Icons.Default.NotificationsActive
                        }
                        val instante = runCatching {
                            java.time.Instant.ofEpochMilli(aviso.timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"))
                        }.getOrDefault("--")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(cardBg)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                                .clickable(enabled = canOpen) { onOpen(aviso) }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chipColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = notifIcon,
                                        contentDescription = null,
                                        tint = chipColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        aviso.titulo.ifBlank { tr("Notificação", "Notification") },
                                        fontWeight = FontWeight.Bold,
                                        color = titleColor,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        instante,
                                        color = textDim,
                                        fontSize = 11.sp
                                    )
                                }
                                if (canOpen) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = chevronTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (aviso.descricao.isNotBlank()) {
                                Text(
                                    text = aviso.descricao,
                                    color = textSub,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(chipLabel, color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (!vehicleName.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(secondaryChipBg)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(vehicleName, color = textSub, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item("bottom_spacer") { Spacer(Modifier.height(8.dp)) }
        }
    }
}
private data class AbastecimentoResumo(
    val ultimo: AbastecimentoEntry?,
    val proximaData: LocalDate?,
    val diasAte: Long?,
    val mediaCustoDia: Double?,
    val custoSemana: Double?,
    val custoMes: Double?
)

private data class AbastecimentoEntry(
    val data: LocalDate,
    val litros: Double,
    val valorPago: Double
)

private fun calcularResumoAbastecimento(
    abastecimentos: List<Abastecimento>,
    formatter: DateTimeFormatter
): AbastecimentoResumo {
    val entries = abastecimentos.mapNotNull { item ->
        val data = runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
        if (data != null && item.litros > 0.0 && item.valorPago > 0.0) {
            AbastecimentoEntry(data, item.litros, item.valorPago)
        } else {
            null
        }
    }.sortedBy { it.data }

    if (entries.isEmpty()) {
        return AbastecimentoResumo(
            ultimo = null,
            proximaData = null,
            diasAte = null,
            mediaCustoDia = null,
            custoSemana = null,
            custoMes = null
        )
    }

    val diasEntreAbastecimentos = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.data, atual.data)
        if (dias <= 0) null else dias.toDouble()
    }

    val custoDiario = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.data, atual.data)
        if (dias <= 0) null else atual.valorPago / dias.toDouble()
    }

    val mediaDiasBase = diasEntreAbastecimentos.takeIf { it.isNotEmpty() }?.average()
    val mediaCustoBase = custoDiario.takeIf { it.isNotEmpty() }?.average()
    val ultimo = entries.last()
    val fallbackDias = 7.0
    val mediaDias = mediaDiasBase ?: fallbackDias
    val mediaCusto = mediaCustoBase ?: (ultimo.valorPago / mediaDias)
    val diasAte = mediaDias.takeIf { it > 0.0 }?.let { ceil(it).toLong() }
    val proximaData = diasAte?.let { ultimo.data.plusDays(it) }

    return AbastecimentoResumo(
        ultimo = ultimo,
        proximaData = proximaData,
        diasAte = diasAte,
        mediaCustoDia = mediaCusto,
        custoSemana = mediaCusto?.times(7),
        custoMes = mediaCusto?.times(30)
    )
}

data class CategorySpend(
    val label: String,
    val valor: Double,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleBasicsGuideScreen(onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val pageBg = if (isDark) scheme.background else scheme.background
    val cardBg = if (isDark) Color(0xFF111827) else scheme.surface
    val border = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accent = scheme.primary

    val context = LocalContext.current
    val dicas = remember {
        listOf(
            GuideVideoItem(
                icon = Icons.Rounded.TireRepair,
                title = "Como trocar pneu",
                description = "Aprenda o passo a passo para fazer a troca com segurança.",
                videoUrl = "https://autoesporte.globo.com/video/como-trocar-o-pneu-do-carro-9501074.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.Speed,
                title = "Como calibrar pneu",
                description = "Veja como calibrar corretamente e por que o pneu deve estar frio.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/07/video-como-a-escolha-de-pneus-influencia-na-seguranca-e-no-desempenho-do-seu-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.Build,
                title = "Troca de óleo",
                description = "Entenda quando trocar o óleo e o filtro do motor.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/04/video-como-ver-o-nivel-de-oleo-do-motor-do-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.WaterDrop,
                title = "Conferir água/arrefecimento",
                description = "Como verificar o nível do reservatório com o motor frio.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.WarningAmber,
                title = "Luzes do painel",
                description = "Entenda quais luzes exigem parada imediata e quais permitem seguir com cautela.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.BatteryAlert,
                title = "Bateria fraca (chupeta)",
                description = "Passo a passo para partida auxiliar sem danificar o sistema elétrico.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.DeviceThermostat,
                title = "Superaquecimento",
                description = "O que fazer quando o carro esquenta e o que nunca fazer com motor quente.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2020/02/enchentes-veja-quando-vale-atravessar-e-o-que-fazer-se-teve-prejuizo-com-o-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.ElectricalServices,
                title = "Fusíveis do carro",
                description = "Como identificar fusível queimado e fazer a troca correta.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.CarRepair,
                title = "Itens de emergência",
                description = "Onde ficam triângulo, macaco e chave de roda no veículo.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.OilBarrel,
                title = "Medir nível do óleo",
                description = "Como usar a vareta corretamente para conferir o nível do óleo do motor.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/04/video-como-ver-o-nivel-de-oleo-do-motor-do-carro.ghtml"
            ),
            GuideVideoItem(
                icon = null,
                badgeText = "ABS",
                title = "Sinais de problema no freio",
                description = "Ruído, vibração e pedal baixo: quando procurar oficina imediatamente.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/07/video-como-a-escolha-de-pneus-influencia-na-seguranca-e-no-desempenho-do-seu-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.Description,
                title = "Documentos e emergência",
                description = "Checklist essencial de documentos e contatos para manter no carro.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            )
        )
    }

    Scaffold(
        containerColor = pageBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (isDark) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Guia rápido do veículo",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }            
            dicas.forEach { dica ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dica.badgeText.isNullOrBlank()) {
                                Icon(
                                    dica.icon ?: Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = dica.badgeText,
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = dica.title,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(dica.description, color = textSecondary, fontSize = 14.sp, lineHeight = 19.sp)
                    Button(
                        onClick = { openExternalUrl(context, dica.videoUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Assistir vídeo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private data class GuideVideoItem(
    val icon: ImageVector?,
    val badgeText: String? = null,
    val title: String,
    val description: String,
    val videoUrl: String
)

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Não foi possível abrir o vídeo", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun HomeFaqScreen(
    onDismiss: () -> Unit,
    onOpenVehicleGuide: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val background = if (isDark) Color.Black else colorScheme.background
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant
    val englishUi = isEnglishUi()
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }
    val faqItems = remember {
        if (englishUi) {
            listOf(
                "How do I register a new vehicle?" to "Tap New vehicle, choose the type, then select brand and model. If names are loading, wait a moment and the list will appear.",
                "Why can't I pick the vehicle name right away?" to "The app fetches names after the brand is selected. While loading, the field shows a loading message. Wait a few seconds and try again.",
                "How do I create a reminder faster?" to "Tap New reminder, choose the category, review date, mileage and details, then save. You can also start from the camera flow when available.",
                "Where can I see reminder notifications?" to "Use the bell in Home to open notifications history. You can remove single items or clear everything.",
                "How do I add a service provider to a reminder?" to "Open the reminder details and tap Add provider. Fill name and phone, then save to link the contact.",
                "Where is the vehicle guide now?" to "Open Frequently Asked Questions and use the Vehicle guide selector. It opens quick tips with practical videos.",
                "How do I back up and restore my data?" to "Open Settings > Backup. Use restore on this device when needed and reopen the app after completion.",
                "What changes in Premium?" to "Premium unlocks advanced modules like Fleet features, extra management tools, and expanded operational flows."
            )
        } else {
            listOf(
                "Como cadastrar um novo veículo?" to "Toque em Novo veículo, escolha o tipo e depois selecione marca e modelo. Se os nomes estiverem carregando, aguarde alguns segundos.",
                "Por que o nome do veículo não abre na hora?" to "O app busca os nomes após a escolha da marca. Enquanto carrega, o campo mostra mensagem de carregamento. Depois disso, a lista libera.",
                "Como criar um aviso mais rápido?" to "Toque em Novo aviso, escolha a categoria, revise data, km e detalhes e finalize em salvar. Quando disponível, você também pode iniciar pela câmera.",
                "Onde vejo as notificações dos avisos?" to "Use o sino na Home para abrir o histórico de notificações. Dá para remover individualmente ou limpar tudo.",
                "Como adicionar um prestador no aviso?" to "Abra os detalhes do aviso e toque em Adicionar prestador. Preencha nome e telefone e salve para vincular o contato.",
                "Onde ficou o guia do veículo?" to "Agora ele está em Dúvidas frequentes, no seletor Guia sobre o veículo. Lá você abre dicas rápidas com vídeos.",
                "Como fazer backup e restaurar meus dados?" to "Vá em Configurações > Backup. Use Restaurar backup neste aparelho quando precisar e reabra o app após concluir.",
                "O que muda no Premium?" to "O Premium libera módulos avançados como recursos de frota, ferramentas extras de gestão e fluxos operacionais expandidos."
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = tr("Voltar", "Back"),
                        tint = titleColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr("Dúvidas frequentes", "Frequently asked questions"),
                    color = titleColor,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tr("Respostas rápidas para as dúvidas mais comuns", "Quick answers for the most common questions"),
                    color = bodyColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVehicleGuide() },
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF111827) else colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.75f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Guia sobre o veículo", "Vehicle guide"),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = tr("Abra dicas rápidas com vídeos para cuidar melhor do seu veículo.", "Open quick video tips to take better care of your vehicle."),
                            color = bodyColor,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = bodyColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            faqItems.forEachIndexed { index, (pergunta, resposta) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF111827) else colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.75f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pergunta,
                                color = titleColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expandedFaqIndex == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = titleColor
                            )
                        }
                        if (expandedFaqIndex == index) {
                            Spacer(Modifier.height(8.dp))
                            Divider(
                                color = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.55f),
                                thickness = 1.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = resposta,
                                color = bodyColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LegalInfoScreen(
    title: String,
    icon: ImageVector,
    content: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val background = if (isDark) Color.Black else colorScheme.background
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = titleColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF111827) else colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.75f))
            ) {
                Text(
                    text = content,
                    color = bodyColor,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryExpenseChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    emptyColor: Color = Color(0xFF334155),
    centerColor: Color = Color(0xFF0B1224)
) {
    val safeData =
        if (data.isEmpty()) listOf(CategorySpend(label = "Sem dados", valor = 0.0, color = emptyColor)) else data
    val totalValor = safeData.sumOf { it.valor }.coerceAtLeast(0.0)
    val hasData = totalValor > 0.0
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val barCount = safeData.size.coerceAtLeast(1)
        val spacingDp = 10.dp
        val spacingPx = with(density) { spacingDp.toPx() }
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalSpacingPx = spacingPx * (barCount - 1)
        val barWidthPx = ((totalWidthPx - totalSpacingPx) / barCount)
            .coerceAtLeast(with(density) { 6.dp.toPx() })
        val barWidthDp = with(density) { barWidthPx.toDp() }
        val iconSize = 14.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val maxValor = safeData.maxOfOrNull { it.valor }?.coerceAtLeast(0.0) ?: 0.0
                val maxHeight = size.height * 0.85f
                val baseY = size.height
            val lowColor = Color(0xFF22C55E)
            val midColor = Color(0xFFF59E0B)
            val highColor = Color(0xFFEF4444)
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val gridSteps = 4
            repeat(gridSteps + 1) { step ->
                val y = baseY - (maxHeight / gridSteps) * step
                val t = step.toFloat() / gridSteps.toFloat()
                val baseColor = if (t <= 0.5f) {
                    lerp(lowColor, midColor, t / 0.5f)
                } else {
                    lerp(midColor, highColor, (t - 0.5f) / 0.5f)
                }
                drawLine(
                    color = baseColor.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
                if (!hasData) {
                    val barHeight = maxHeight * 0.4f
                    val left = (size.width - barWidthPx) / 2f
                    drawRoundRect(
                        color = emptyColor,
                        topLeft = Offset(left, baseY - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                } else {
                    safeData.forEachIndexed { index, item ->
                        val ratio = if (maxValor > 0.0) (item.valor / maxValor).toFloat() else 0f
                        val barHeight = (maxHeight * ratio * progress.value).coerceAtLeast(4.dp.toPx())
                        val left = index * (barWidthPx + spacingPx)
                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(left, baseY - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                        val percent = if (totalValor > 0.0) ((item.valor / totalValor) * 100).toInt() else 0
                        if (percent > 0) {
                            val x = left + (barWidthPx / 2f)
                            val y = (baseY - barHeight - 6.dp.toPx()).coerceAtLeast(textPaint.textSize)
                            drawContext.canvas.nativeCanvas.drawText("$percent%", x, y, textPaint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacingDp)
            ) {
                safeData.forEach { item ->
                    val tipo = TipoManutencao.values().firstOrNull { it.label == item.label }
                    Box(
                        modifier = Modifier.width(barWidthDp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tipo != null) {
                            TipoIcon(
                                tipo = tipo,
                                tint = Color.White.copy(alpha = 0.7f),
                                size = iconSize,
                                textSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryExpenseLegend(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    labelColor: Color = Color(0xFF94A3B8),
    emptyColor: Color = Color(0xFF334155),
    minItems: Int = 0
) {
    val safeData =
        if (data.isEmpty()) listOf(CategorySpend(label = "Sem dados", valor = 0.0, color = emptyColor)) else data
    val totalValor = safeData.sumOf { it.valor }.coerceAtLeast(0.0)
    val centralizarLegenda = safeData.size <= 5
    Column(
        modifier = modifier,
        verticalArrangement = if (centralizarLegenda) Arrangement.Center else Arrangement.spacedBy(6.dp)
    ) {
        safeData.forEach { item ->
            val dotColor = if (item.valor <= 0.0) emptyColor else item.color
            val tipo = TipoManutencao.values().firstOrNull { it.label == item.label }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(8.dp))
                if (tipo != null) {
                    TipoIcon(
                        tipo = tipo,
                        tint = Color.White.copy(alpha = 0.7f),
                        size = 12.dp,
                        textSize = 8.sp
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.label,
                    color = labelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatarMoedaLocal(item.valor),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        if (safeData.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(Modifier.height(0.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total:", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatarMoedaLocal(totalValor),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val fillers = (minItems - safeData.size).coerceAtLeast(0)
        repeat(fillers) { Spacer(Modifier.height(16.dp)) }
    }
}

fun corCategoria(tipo: TipoManutencao): Color = when (tipo) {
    TipoManutencao.CORRENTE -> Color(0xFF22C55E)
    TipoManutencao.LUBRIFICACAO -> Color(0xFF14B8A6)
    TipoManutencao.PEDIVELA -> Color(0xFF0EA5E9)
    TipoManutencao.ACESSORIOS -> Color(0xFFF97316)
    TipoManutencao.CONFORTO -> Color(0xFFEAB308)
    TipoManutencao.PNEU -> Color(0xFFF59E0B)
    TipoManutencao.TRANSMISSAO -> Color(0xFF60A5FA)
    TipoManutencao.REVISAO -> Color(0xFF8B5CF6)
    TipoManutencao.OLEO -> Color(0xFF3B82F6) // azul
    TipoManutencao.ABASTECIMENTO -> Color(0xFF0EA5E9) // azul ciano
    TipoManutencao.LAVAGEM -> Color(0xFF06B6D4) // azul agua
    TipoManutencao.BATERIA -> Color(0xFF16A34A) // verde
    TipoManutencao.VIDROS -> Color(0xFF38BDF8) // azul vidro
    TipoManutencao.MECANICA -> Color(0xFF60A5FA) // azul claro
    TipoManutencao.FUNILARIA -> Color(0xFFF97316) // laranja
    TipoManutencao.FREIO -> Color(0xFFDC2626) // vermelho
    TipoManutencao.LICENCIAMENTO -> Color(0xFF22C55E) // verde claro
    TipoManutencao.IPVA -> Color(0xFF5B8DEF) // azul leve
    TipoManutencao.SEGURO -> Color(0xFF10B981) // verde
    TipoManutencao.OUTROS -> Color(0xFF94A3B8)
}








