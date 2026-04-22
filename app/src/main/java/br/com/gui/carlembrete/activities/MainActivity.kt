package br.com.gui.carlembrete


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

const val EXTRA_OPEN_AONDE_PAREI = "extra_open_aonde_parei"
const val EXTRA_OPEN_LEMBRETE_ID = "extra_open_lembrete_id"
const val EXTRA_OPEN_LEMBRETE_CARRO_ID = "extra_open_lembrete_carro_id"
private const val TAG_MAIN_STARTUP = "MainStartup"

class MainActivity : ComponentActivity() {
    private var contentInitialized = false
    private var openAondePareiFromIntent by mutableStateOf(false)
    private var openLembreteIdFromIntent by mutableStateOf<String?>(null)
    private var openLembreteCarroIdFromIntent by mutableStateOf<String?>(null)
    @Volatile
    private var keepNativeSplashVisible: Boolean = false
    private var inAppUpdateChecked = false
    private var reviewCheckedThisSession = false
    private var appUpdateManager: AppUpdateManager? = null
    private var installStateListener: InstallStateUpdatedListener? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleManager.applySavedLanguage(this)
        val shouldHoldSplashForHomeData = FirebaseAuth.getInstance().currentUser != null &&
            !AppPreferences.needsOnboarding(this)
        keepNativeSplashVisible = shouldHoldSplashForHomeData
        installSplashScreen().setKeepOnScreenCondition { keepNativeSplashVisible }
        super.onCreate(savedInstanceState)
        InstallDiagnostics.logDetailedSnapshot(applicationContext, "MainActivity.onCreate")
        val startupAt = System.currentTimeMillis()
        Log.d(TAG_MAIN_STARTUP, "onCreate start")
        handleNavigationIntent(intent)
        initializeContentIfNeeded()
        Log.d(TAG_MAIN_STARTUP, "setContent initialized in ${System.currentTimeMillis() - startupAt}ms")
        NotificacaoHelper.criarCanal(applicationContext)
        Log.d(TAG_MAIN_STARTUP, "notification channel initialized")

        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    private fun requestStartupPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_AONDE_PAREI, false) == true) {
            openAondePareiFromIntent = true
            intent.removeExtra(EXTRA_OPEN_AONDE_PAREI)
        }
        intent?.getStringExtra(EXTRA_OPEN_LEMBRETE_ID)?.let { lembreteId ->
            if (lembreteId.isNotBlank()) {
                openLembreteIdFromIntent = lembreteId
                openLembreteCarroIdFromIntent = intent.getStringExtra(EXTRA_OPEN_LEMBRETE_CARRO_ID)
            }
            intent.removeExtra(EXTRA_OPEN_LEMBRETE_ID)
            intent.removeExtra(EXTRA_OPEN_LEMBRETE_CARRO_ID)
        }
    }

    private fun initializeContentIfNeeded() {
        if (contentInitialized) return
        contentInitialized = true
        setContent {
            var themeMode by remember { mutableStateOf(AppPreferences.getThemeMode(this@MainActivity)) }
            val firstFrameAt = remember { System.currentTimeMillis() }
            SideEffect {
                Log.d(
                    TAG_MAIN_STARTUP,
                    "compose frame ready in ${System.currentTimeMillis() - firstFrameAt}ms | userLogged=${FirebaseAuth.getInstance().currentUser != null}"
                )
            }

            CarLembreteTheme(themeMode = themeMode) {
                val isDarkTheme = when (themeMode) {
                    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                    AppThemeMode.ROSE -> false
                }
                val colorScheme = MaterialTheme.colorScheme
                val auth = remember { FirebaseAuth.getInstance() }
                var usuario by remember { mutableStateOf(auth.currentUser) }
                var announcementTitle by remember { mutableStateOf<String?>(null) }
                var announcementBody by remember { mutableStateOf<String?>(null) }
                var showOnboarding by remember {
                    mutableStateOf(
                        AppPreferences.needsOnboarding(this@MainActivity)
                    )
                }
                LaunchedEffect(isDarkTheme, usuario) {
                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    if (usuario == null) {
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        insetsController.isAppearanceLightStatusBars = false
                        insetsController.isAppearanceLightNavigationBars = false
                    } else {
                        val systemBarColor = colorScheme.background.toArgb()
                        val useDarkIcons = colorScheme.background.luminance() > 0.5f
                        window.statusBarColor = systemBarColor
                        window.navigationBarColor = systemBarColor
                        insetsController.isAppearanceLightStatusBars = useDarkIcons
                        insetsController.isAppearanceLightNavigationBars = useDarkIcons
                    }
                }
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        usuario = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }
                LaunchedEffect(usuario) {
                    if (usuario != null) {
                        delay(450)
                        AdminUsersSync.syncLocalOverview(this@MainActivity)
                        AdminUsageMetrics.markAppOpen(this@MainActivity)
                        AdminUsersSync.checkAnnouncement(this@MainActivity) { title, description ->
                            announcementTitle = title
                            announcementBody = description
                        }
                        registrarUsoEEventosLeves()
                        if (!showOnboarding) {
                            iniciarInAppReviewSeElegivel()
                            iniciarInAppUpdateSeDisponivel()
                        }
                    }
                }
                val baseBackground = MaterialTheme.colorScheme.background
                Surface(modifier = Modifier.fillMaxSize(), color = baseBackground) {
                    if (usuario == null) {
                        keepNativeSplashVisible = false
                        AuthScreen(
                            onSignedIn = {
                                usuario = auth.currentUser
                                showOnboarding = AppPreferences.needsOnboarding(this@MainActivity)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (showOnboarding) {
                                keepNativeSplashVisible = false
                                OnboardingScreen(
                                    onFinish = {
                                        AppPreferences.markOnboardingComplete(this@MainActivity)
                                        showOnboarding = false
                                    },
                                    onThemeModeChanged = { mode ->
                                        themeMode = mode
                                    }
                                )
                            } else {
                                ManutencaoScreen(
                                    openAondePareiOnStart = openAondePareiFromIntent,
                                    onAondePareiStartConsumed = { openAondePareiFromIntent = false },
                                    onLoaded = { keepNativeSplashVisible = false },
                                    onThemeModeChanged = { themeMode = it }
                                )
                                if (announcementTitle != null && announcementBody != null) {
                                    val dialogBg = if (isDarkTheme) Color(0xFF0B1220) else Color.White
                                    val dialogTitle = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                                    val dialogBody = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                    val dialogAccent = Color(0xFF2563EB)
                                    val dialogButtonBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFD1D5DB)
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { announcementTitle = null; announcementBody = null },
                                        modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
                                        shape = dialogCornerShape,
                                        containerColor = dialogBg,
                                        icon = {
                                            androidx.compose.material3.Surface(
                                                color = dialogAccent.copy(alpha = 0.14f),
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, dialogAccent.copy(alpha = 0.35f))
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = dialogAccent,
                                                    modifier = Modifier.padding(11.dp)
                                                )
                                            }
                                        },
                                        title = {
                                            androidx.compose.material3.Text(
                                                text = announcementTitle ?: "",
                                                color = dialogTitle,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        },
                                        text = {
                                            androidx.compose.material3.Text(
                                                text = announcementBody ?: "",
                                                color = dialogBody
                                            )
                                        },
                                        confirmButton = {
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = { announcementTitle = null; announcementBody = null },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp),
                                                shape = dialogActionButtonShape,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, dialogButtonBorder),
                                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = dialogBody
                                                )
                                            ) {
                                                androidx.compose.material3.Text(
                                                    text = "Entendi",
                                                    color = dialogBody,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val manager = appUpdateManager ?: return
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    runCatching {
                        manager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.FLEXIBLE,
                            this,
                            REQ_CODE_IN_APP_UPDATE
                        )
                    }
                }
            }
    }

    override fun onDestroy() {
        installStateListener?.let { listener ->
            appUpdateManager?.unregisterListener(listener)
        }
        installStateListener = null
        super.onDestroy()
    }

    private fun iniciarInAppUpdateSeDisponivel() {
        if (inAppUpdateChecked) return
        inAppUpdateChecked = true
        val manager = AppUpdateManagerFactory.create(this)
        appUpdateManager = manager
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Toast.makeText(
                    this,
                    trNow("Atualização baixada. Finalizando instalação...", "Update downloaded. Finishing installation..."),
                    Toast.LENGTH_LONG
                ).show()
                runCatching { manager.completeUpdate() }
            }
        }
        installStateListener = listener
        manager.registerListener(listener)
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                if (updateAvailable && flexibleAllowed) {
                    runCatching {
                        manager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.FLEXIBLE,
                            this,
                            REQ_CODE_IN_APP_UPDATE
                        )
                    }
                }
            }
    }

    private fun iniciarInAppReviewSeElegivel() {
        if (reviewCheckedThisSession) return
        reviewCheckedThisSession = true
        val prefs = getSharedPreferences(ENGAGEMENT_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val firstOpen = prefs.getLong(KEY_FIRST_OPEN_MS, 0L).takeIf { it > 0L } ?: now
        val openCount = prefs.getInt(KEY_OPEN_COUNT, 0)
        val lastPrompt = prefs.getLong(KEY_LAST_REVIEW_PROMPT_MS, 0L)
        val daysSinceFirst = ((now - firstOpen) / DAY_MS).toInt()
        val canPrompt = openCount >= 8 &&
            daysSinceFirst >= 5 &&
            (lastPrompt == 0L || (now - lastPrompt) >= 45L * DAY_MS)
        if (!canPrompt) return

        val reviewManager = ReviewManagerFactory.create(this)
        reviewManager.requestReviewFlow()
            .addOnCompleteListener { task ->
                prefs.edit().putLong(KEY_LAST_REVIEW_PROMPT_MS, now).apply()
                if (!task.isSuccessful) return@addOnCompleteListener
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(this, reviewInfo)
            }
    }

    private fun registrarUsoEEventosLeves() {
        val prefs = getSharedPreferences(ENGAGEMENT_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val firstOpen = prefs.getLong(KEY_FIRST_OPEN_MS, 0L)
        val openCount = prefs.getInt(KEY_OPEN_COUNT, 0) + 1
        val editor = prefs.edit()
            .putInt(KEY_OPEN_COUNT, openCount)
            .putLong(KEY_LAST_OPEN_MS, now)
        if (firstOpen == 0L) editor.putLong(KEY_FIRST_OPEN_MS, now)
        editor.apply()

        if (!ENABLE_STARTUP_ENGAGEMENT_NOTIFICATIONS) return
        if (!notificacoesPermitidas()) return

        val lastSimpleReminder = prefs.getLong(KEY_LAST_SIMPLE_REMINDER_MS, 0L)
        if (openCount >= 3 && (now - lastSimpleReminder) >= 7L * DAY_MS) {
            NotificacaoHelper.dispararNotificacaoInstantanea(
                context = this,
                titulo = trNow("Hora de revisar seu veículo", "Time for a vehicle check"),
                descricao = trNow(
                    "Dá uma olhada nos avisos e mantenha a revisão em dia.",
                    "Quick check your reminders and keep maintenance up to date."
                )
            )
            prefs.edit().putLong(KEY_LAST_SIMPLE_REMINDER_MS, now).apply()
        }

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dueSoon = BancoDeDados.carregarLembretes(this)
            .filterNot(::isLembreteRealizado)
            .mapNotNull { lembrete ->
                runCatching { LocalDate.parse(lembrete.dataLimite, formatter) }.getOrNull()?.let { data -> lembrete to data }
            }
            .any { (_, data) ->
                val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), data)
                days in 0..3
            }
        if (dueSoon) {
            val lastDueSoon = prefs.getLong(KEY_LAST_DUE_SOON_NOTIF_MS, 0L)
            if ((now - lastDueSoon) >= DAY_MS) {
                NotificacaoHelper.dispararNotificacaoInstantanea(
                    context = this,
                    titulo = trNow("Tem aviso chegando", "Upcoming reminder"),
                    descricao = trNow(
                        "Você tem revisão/aviso para os próximos dias. Bora conferir?",
                        "You have maintenance reminders for the next days. Want to check now?"
                    )
                )
                prefs.edit().putLong(KEY_LAST_DUE_SOON_NOTIF_MS, now).apply()
            }
        }
    }

    private fun notificacoesPermitidas(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    companion object {
        private const val REQ_CODE_IN_APP_UPDATE = 2417
        private const val ENGAGEMENT_PREFS = "engagement_prefs_v1"
        private const val KEY_FIRST_OPEN_MS = "first_open_ms"
        private const val KEY_LAST_OPEN_MS = "last_open_ms"
        private const val KEY_OPEN_COUNT = "open_count"
        private const val KEY_LAST_REVIEW_PROMPT_MS = "last_review_prompt_ms"
        private const val KEY_LAST_SIMPLE_REMINDER_MS = "last_simple_reminder_ms"
        private const val KEY_LAST_DUE_SOON_NOTIF_MS = "last_due_soon_notif_ms"
        private const val ENABLE_STARTUP_ENGAGEMENT_NOTIFICATIONS = false
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}




/* ----------------- UTILIT�RIOS & L�GICA DE C�LCULO ----------------- */

fun String.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
}

fun calcularProximaData(tipo: TipoManutencao, dataServico: LocalDate): String {
    val mesesParaAdicionar = when (tipo) {
        TipoManutencao.CORRENTE -> 2L
        TipoManutencao.LUBRIFICACAO -> 1L
        TipoManutencao.PEDIVELA -> 12L
        TipoManutencao.ACESSORIOS -> 12L
        TipoManutencao.CONFORTO -> 18L
        TipoManutencao.PNEU -> 12L
        TipoManutencao.TRANSMISSAO -> 12L
        TipoManutencao.REVISAO -> 12L
        TipoManutencao.OLEO -> 6L
        TipoManutencao.LAVAGEM -> 1L
        TipoManutencao.ABASTECIMENTO -> 1L
        TipoManutencao.BATERIA -> 24L
        TipoManutencao.VIDROS -> 12L
        TipoManutencao.FREIO -> 12L
        TipoManutencao.MECANICA -> 12L
        TipoManutencao.FUNILARIA -> 6L
        TipoManutencao.LICENCIAMENTO -> 12L
        TipoManutencao.IPVA -> 12L
        TipoManutencao.SEGURO -> 12L
        TipoManutencao.OUTROS -> 6L
    }
    return dataServico.plusMonths(mesesParaAdicionar).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

fun getKmAdicionalPorTipo(tipo: TipoManutencao): Int {
    return when (tipo) {
        TipoManutencao.CORRENTE -> 1000
        TipoManutencao.LUBRIFICACAO -> 300
        TipoManutencao.PEDIVELA -> 5000
        TipoManutencao.ACESSORIOS -> 0
        TipoManutencao.CONFORTO -> 0
        TipoManutencao.PNEU -> 10000
        TipoManutencao.TRANSMISSAO -> 10000
        TipoManutencao.REVISAO -> 10000
        TipoManutencao.OLEO -> 10000
        TipoManutencao.LAVAGEM -> 500
        TipoManutencao.ABASTECIMENTO -> 500
        TipoManutencao.BATERIA -> 40000
        TipoManutencao.VIDROS -> 10000
        TipoManutencao.FREIO -> 15000
        TipoManutencao.MECANICA -> 15000
        TipoManutencao.FUNILARIA -> 10000
        TipoManutencao.LICENCIAMENTO -> 0
        TipoManutencao.IPVA -> 0
        TipoManutencao.SEGURO -> 0
        TipoManutencao.OUTROS -> 5000
    }
}

fun TipoManutencao.ehManutencaoTecnica(): Boolean {
    return when (this) {
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.REVISAO,
        TipoManutencao.OLEO,
        TipoManutencao.BATERIA,
        TipoManutencao.VIDROS,
        TipoManutencao.MECANICA,
        TipoManutencao.FUNILARIA,
        TipoManutencao.FREIO -> true
        TipoManutencao.LAVAGEM,
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA,
        TipoManutencao.SEGURO,
        TipoManutencao.OUTROS -> false
    }
}

fun formatarMoeda(valor: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

fun gerarResumoRelatorio(carro: CarroInfo, lembretes: List<Lembrete>, isPremium: Boolean): String {
    val builder = StringBuilder()
    val isBike = carro.tipoVeiculo == TipoVeiculo.BICICLETA || carro.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
    val exibirKm = !isBike || !carro.semControleKm
    builder.appendLine("Relatório do veículo")
    builder.appendLine("Nome: ${carro.nome}")
    builder.appendLine("Proprietário: ${carro.proprietario.ifBlank { "Não informado" }}")
    builder.appendLine("Marca: ${carro.marca.ifBlank { "Não informada" }}")
    builder.appendLine("Modelo: ${carro.modelo}")
    builder.appendLine(
        "Odômetro: ${
            if (!exibirKm) "Não aplicável" else if (carro.kmAtual > 0) "${carro.kmAtual} km" else "Não informado"
        }"
    )
    builder.appendLine()
    builder.appendLine("Avisos ativos: ${lembretes.size}")
    TipoManutencao.values().forEach { tipo ->
        val count = lembretes.count { it.tipo == tipo }
        if (count > 0) builder.appendLine("- ${tipo.label}: $count")
    }
    if (lembretes.isNotEmpty()) {
        builder.appendLine()
        builder.appendLine("Detalhes dos próximos serviços:")
        lembretes.sortedBy { it.dataLimite }.forEach { lembrete ->
            builder.appendLine("* ${lembrete.titulo} - Data: ${lembrete.dataLimite.ifBlank { "Sem data" }} - KM: ${lembrete.kmLimite.ifBlank { "-" }}")
        }
    }
    if (!isPremium) {
        builder.appendLine()
        builder.appendLine("Gerado pelo Zellu")
    }
    return builder.toString()
}

fun compartilharRelatorio(context: Context, texto: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
}

fun gerarPdfRelatorio(
    context: Context,
    carro: CarroInfo,
    lembretes: List<Lembrete>,
    isPremium: Boolean,
    valorTabela: String? = null,
    valorParaVender: String? = null
): Uri? {
    return try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas
        val headerPaint = Paint().apply {
            textSize = 24f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerSubPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val valueBoldPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val debugPaint = Paint().apply {
            textSize = 26f
            color = android.graphics.Color.RED
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            strokeWidth = 2f
            color = android.graphics.Color.parseColor("#94A3B8")
            isAntiAlias = true
        }
        val watermarkPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.parseColor("#94A3B8")
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val colorSuccess = android.graphics.Color.parseColor("#16A34A")
        val colorDanger = android.graphics.Color.parseColor("#DC2626")
        val accentColor = android.graphics.Color.parseColor("#2563EB")
        val logoBitmap = try {
            context.assets.open("logorelatorio.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val headerCardPaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
        val headerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val marginX = 36f
        canvas.drawColor(android.graphics.Color.WHITE)
        val topBarPaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
        canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 6f, topBarPaint)
        val contentWidth = pageInfo.pageWidth - marginX * 2
        var y = 138f

        fun fit(text: String, maxChars: Int): String =
            if (text.length <= maxChars) text else text.take(maxChars - 3) + "..."

        fun ensureSpace(extra: Float) {
            if (y + extra > pageInfo.pageHeight - 40) {
                if (!isPremium) {
                    canvas.drawText("Gerado pelo Zellu", pageInfo.pageWidth / 2f, pageInfo.pageHeight - 24f, watermarkPaint)
                }
                document.finishPage(currentPage)
                val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                currentPage = document.startPage(nextPageInfo)
                canvas = currentPage.canvas
                y = 60f
            }
        }

        fun drawHeader() {
            val titleCenterPaint = Paint(headerPaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("RELATORIO TÊCNICO", pageInfo.pageWidth / 2f, 54f, titleCenterPaint)
            canvas.drawLine(marginX, 78f, pageInfo.pageWidth - marginX, 78f, dividerPaint)
            val headerInfoPaint = Paint(headerSubPaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(
                "Gerado em ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                pageInfo.pageWidth / 2f,
                108f,
                headerInfoPaint
            )
        }

        fun drawSectionTitle(title: String) {
            ensureSpace(30f)
            val titleY = y
            canvas.drawText(title, marginX, titleY, sectionTitlePaint)
            y += 10f
            canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
            y += 12f
        }

        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        fun drawCard(height: Float, content: (Float) -> Unit) {
            ensureSpace(height + 6f)
            val rect = android.graphics.RectF(marginX, y, marginX + contentWidth, y + height)
            canvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
            content(y)
            y += height + 28f
        }

        fun drawWrappedText(text: String, x: Float, maxWidth: Float, paint: Paint): Float {
            val words = text.split(" ")
            var line = ""
            var currentY = y
            words.forEach { word ->
                val test = if (line.isBlank()) word else "$line $word"
                if (paint.measureText(test) <= maxWidth) {
                    line = test
                } else {
                    canvas.drawText(line, x, currentY, paint)
                    currentY += 14f
                    line = word
                }
            }
            if (line.isNotBlank()) {
                canvas.drawText(line, x, currentY, paint)
                currentY += 14f
            }
            return currentY
        }

        fun drawKeyValue(label: String, value: String, x: Float, lineY: Float, valueOffset: Float = 14f) {
            canvas.drawText(label.uppercase(Locale.getDefault()), x, lineY, valueBoldPaint)
            canvas.drawText(value, x, lineY + valueOffset, valuePaint)
        }

        drawHeader()

        val lembretesSemAbastecimento = lembretes.filter { it.tipo != TipoManutencao.ABASTECIMENTO }
        val lembretesTecnicos = lembretes.filter { it.tipo.ehManutencaoTecnica() }
        val lembretesTecnicosAtivos = lembretesTecnicos.filterNot(::isLembreteRealizado)
        val lembretesAtivos = lembretesSemAbastecimento.filterNot(::isLembreteRealizado)
        val totalGastos = lembretes.sumOf { it.valor }
        val proximos = lembretesTecnicosAtivos
            .mapNotNull { lembrete ->
                val data = try { LocalDate.parse(lembrete.dataLimite, DateTimeFormatter.ofPattern("dd/MM/yyyy")) } catch (_: Exception) { null }
                data?.let { lembrete to it }
            }
            .sortedBy { it.second }
        val manutencoesRealizadas = (
            lembretesTecnicosAtivos
            .mapNotNull { lembrete ->
                val data = try { LocalDate.parse(lembrete.dataLimite, DateTimeFormatter.ofPattern("dd/MM/yyyy")) } catch (_: Exception) { null }
                data?.let { it to lembrete }
            }
            .filter { (data, _) -> data.isBefore(LocalDate.now()) } +
            lembretesTecnicos.filter(::isLembreteRealizado).mapNotNull { lembrete ->
                val data = dataRealizacaoLembrete(lembrete) ?: runCatching {
                    LocalDate.parse(lembrete.dataLimite, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }.getOrNull()
                data?.let { it to lembrete }
            }
        )
            .distinctBy { (_, lembrete) -> lembrete.id }
            .sortedByDescending { it.first }
            .take(10)
        val proximoServico = proximos.firstOrNull()?.second?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "--"
        val valorTabelaTexto = valorTabela?.takeIf { it.isNotBlank() } ?: "Nao disponivel"
        val valorParaVenderTexto = valorParaVender?.takeIf { it.isNotBlank() } ?: "Nao disponivel"
        val vezesBatidoTexto = carro.vezesBatido?.toString() ?: "Nao informado"
        val tempoComVeiculoTexto = carro.tempoComVeiculo.ifBlank { "Nao informado" }
        val isBike = carro.tipoVeiculo == TipoVeiculo.BICICLETA || carro.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
        val exibirKmBike = isBike && !carro.semControleKm
        val abastecimentosCarro = BancoDeDados.carregarAbastecimentos(context).filter { it.carroId == carro.id }
        val hoje = LocalDate.now()
        val litrosTotais = abastecimentosCarro.sumOf { it.litros.coerceAtLeast(0.0) }
        val totalMesCombustivel = abastecimentosCarro.sumOf { item ->
            val data = runCatching { LocalDate.parse(item.data, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()
            if (data != null && data.year == hoje.year && data.monthValue == hoje.monthValue) item.valorPago else 0.0
        }
        val referenciaMes = hoje.format(DateTimeFormatter.ofPattern("MM/yyyy"))
        if (!isBike) {
            drawSectionTitle("INFORMACOES")
            val infoBoxHeight = 126f
            ensureSpace(infoBoxHeight)
            canvas.drawRect(marginX, y, marginX + contentWidth, y + infoBoxHeight, cardBgPaint)
            canvas.drawRoundRect(android.graphics.RectF(marginX, y, marginX + contentWidth, y + infoBoxHeight), 12f, 12f, cardBorderPaint)
            val infoLeftX = marginX + 12f
            val infoRightX = marginX + contentWidth / 2 + 10f
            val infoRowY = y + 24f
            drawKeyValue("Valor de tabela", valorTabelaTexto, infoLeftX, infoRowY)
            drawKeyValue("Valor para vender", valorParaVenderTexto, infoRightX, infoRowY)
            drawKeyValue("Vezes batido", vezesBatidoTexto, infoLeftX, infoRowY + 42f)
            drawKeyValue("Tempo com veiculo", tempoComVeiculoTexto, infoRightX, infoRowY + 42f)
            y += infoBoxHeight + 24f
        }

        drawSectionTitle("IDENTIFICACAO")
        val boxHeight = if (isBike) {
            if (exibirKmBike) 186f else 150f
        } else {
            180f
        }
        ensureSpace(boxHeight)
        canvas.drawRect(marginX, y, marginX + contentWidth, y + boxHeight, cardBgPaint)
        canvas.drawRoundRect(android.graphics.RectF(marginX, y, marginX + contentWidth, y + boxHeight), 12f, 12f, cardBorderPaint)
        val leftX = marginX + 12f
        val rightX = marginX + contentWidth / 2 + 10f
        val rowY = y + 24f
        drawKeyValue("Nome", fit(carro.nome, 30), leftX, rowY)
        val proprietarioTexto = carro.proprietario.ifBlank { "-" }
        if (isBike) {
            val aroTexto = carro.modelo.ifBlank { "-" }
            drawKeyValue("Aro", fit(aroTexto, 26), rightX, rowY)
            drawKeyValue("Marca", carro.marca.ifBlank { "-" }, leftX, rowY + 42f)
            drawKeyValue("Tipo", carro.tipoVeiculo.label, rightX, rowY + 42f)
            drawKeyValue("Proprietario", fit(proprietarioTexto, 30), leftX, rowY + 78f)
            val corTexto = corNomePorArgb(carro.corArgb)
            if (exibirKmBike) {
                val odometroTexto = if (carro.kmAtual > 0) "${carro.kmAtual} km" else "Nao informado"
                drawKeyValue("Odometro", odometroTexto, leftX, rowY + 114f)
                drawKeyValue("Cor", corTexto, rightX, rowY + 114f)
            } else {
                drawKeyValue("Cor", corTexto, rightX, rowY + 78f)
            }
        } else {
            drawKeyValue("Motor", fit(carro.modelo.ifBlank { "-" }, 26), rightX, rowY)
            drawKeyValue("Marca", carro.marca.ifBlank { "-" }, leftX, rowY + 42f)
            drawKeyValue("Tipo", carro.tipoVeiculo.label, rightX, rowY + 42f)
            val odometroTexto = if (carro.kmAtual > 0) "${carro.kmAtual} km" else "Nao informado"
            drawKeyValue("Odometro", odometroTexto, leftX, rowY + 78f)
            drawKeyValue("Mantenedor/Proprietario", fit(proprietarioTexto, 26), rightX, rowY + 78f)
            val corTexto = corNomePorArgb(carro.corArgb)
            drawKeyValue("Cor", corTexto, leftX, rowY + 114f)
        }
        y += boxHeight + 24f

        drawSectionTitle("STATUS E SAUDE")
        val (tituloRep, descRep) = calcularReputacao(lembretes)
        val saudeCritica = tituloRep.trim().lowercase(Locale("pt", "BR")).contains("crit")
        val saudeEmDia = !saudeCritica
        val statusBoxHeight = 160f
        ensureSpace(statusBoxHeight)
        canvas.drawRect(marginX, y, marginX + contentWidth, y + statusBoxHeight, cardBgPaint)
        canvas.drawRoundRect(android.graphics.RectF(marginX, y, marginX + contentWidth, y + statusBoxHeight), 12f, 12f, cardBorderPaint)
        val saudeColor = if (saudeEmDia) colorSuccess else colorDanger
        val pillPaint = Paint().apply { color = saudeColor }
        val pillTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pillText = if (saudeEmDia) "EM DIA" else "CRITICA"
        val pillWidth = pillTextPaint.measureText(pillText) + 16f
        canvas.drawText("SAUDE", leftX, y + 24f, valueBoldPaint)
        val pillRect = android.graphics.RectF(leftX, y + 30f, leftX + pillWidth, y + 46f)
        canvas.drawRoundRect(pillRect, 10f, 10f, pillPaint)
        canvas.drawText(pillText, leftX + 8f, y + 42f, pillTextPaint)
        drawKeyValue("Alertas ativos", lembretes.size.toString(), rightX, y + 24f)
        drawKeyValue("Proximo servico", proximoServico, leftX, y + 74f)
        drawKeyValue("Total gasto", formatarMoeda(totalGastos), rightX, y + 74f)
        val resultadoGeral = "RESULTADO GERAL"
        val saudeLabel = if (saudeEmDia) "Todas as manutencoes em dia" else "Revisar manutencoes pendentes"
        val saudePaint = Paint(bodyPaint).apply {
            color = if (saudeEmDia) colorSuccess else colorDanger
        }
        val saudeMetrics = saudePaint.fontMetrics
        val saudeBaseline = y + statusBoxHeight - 14f - saudeMetrics.descent
        val resultadoBaseline = saudeBaseline - 14f
        canvas.drawText(resultadoGeral, leftX, resultadoBaseline, valueBoldPaint)
        canvas.drawText(saudeLabel, leftX, saudeBaseline, saudePaint)
        y += statusBoxHeight + 34f

        if (!isBike) {
            drawSectionTitle("CONSUMO")
            val consumoCardHeight = 84f
            drawCard(consumoCardHeight) { topY ->
                val infoLeftX = marginX + 12f
                val infoRightX = marginX + contentWidth / 2 + 10f
                val infoRowY = topY + 24f
                drawKeyValue("Total mes ($referenciaMes)", formatarMoeda(totalMesCombustivel), infoLeftX, infoRowY)
                drawKeyValue(
                    "Litros totais",
                    "${String.format(Locale("pt", "BR"), "%.2f", litrosTotais)} L",
                    infoRightX,
                    infoRowY
                )
            }
            y += 8f
        }

        if (!isBike) {
            drawSectionTitle("DOCUMENTACAO")
            val documentos = listOf(
                TipoManutencao.IPVA to "IPVA",
                TipoManutencao.LICENCIAMENTO to "Licenciamento"
            ).map { (tipo, label) ->
                val ultimaData = lembretesAtivos
                    .filter { it.tipo == tipo }
                    .map { dataParaOrdenacao(it) }
                    .filter { it != LocalDate.MAX }
                    .maxOrNull()
                val status = when {
                    ultimaData == null -> "Nao informado"
                    !ultimaData.isBefore(LocalDate.now()) -> "Em dia"
                    else -> "Vencido"
                }
                Triple(label, status, ultimaData?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "--")
            }
            val docsVazios = documentos.all { it.second == "Nao informado" }
            val docsCardHeight = if (docsVazios) 70f else 70f + (documentos.size * 18f)
            drawCard(docsCardHeight) { topY ->
                var rowY = topY + 28f
                if (docsVazios) {
                    val avisoPaint = Paint(bodyPaint).apply {
                        color = colorDanger
                        textSize = 13f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Veiculo sem documentacao", marginX + (contentWidth / 2), rowY + 6f, avisoPaint)
                } else {
                    documentos.forEach { (label, status, data) ->
                        canvas.drawText(label, marginX + 16f, rowY, valuePaint)
                        val dateCenterPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
                        canvas.drawText("Venc: $data", marginX + (contentWidth / 2), rowY, dateCenterPaint)
                        val statusPaint = Paint(valuePaint).apply {
                            color = if (status == "Em dia") colorSuccess else colorDanger
                            textAlign = Paint.Align.RIGHT
                        }
                        canvas.drawText(status, marginX + contentWidth - 16f, rowY, statusPaint)
                        rowY += 22f
                    }
                }
            }
            y += 8f
        }

        drawSectionTitle("REGISTROS CADASTRADOS")
        if (manutencoesRealizadas.isEmpty()) {
            canvas.drawText("Nenhum registro cadastrado.", marginX, y, bodyPaint)
            y += 16f
        } else {
            val headerHeight = 22f
            val headerBg = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
            canvas.drawRect(marginX, y, marginX + contentWidth, y + headerHeight, headerBg)
            val headerTextPaint = Paint(labelPaint).apply {
                color = android.graphics.Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerY = y + 15f
            canvas.drawText("Item", marginX + 6f, headerY, headerTextPaint)
            canvas.drawText("Data", marginX + 250f, headerY, headerTextPaint)
            canvas.drawText("Valor", marginX + 360f, headerY, headerTextPaint)
            y += headerHeight + 8f

            manutencoesRealizadas.forEach { (data, lembrete) ->
                ensureSpace(26f)
                val rowTextY = y + 4f
                canvas.drawText(fit(lembrete.titulo, 30), marginX + 6f, rowTextY, bodyPaint)
                canvas.drawText(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), marginX + 250f, rowTextY, bodyPaint)
                canvas.drawText(formatarMoeda(lembrete.valor), marginX + 360f, rowTextY, bodyPaint)
                y += 26f
                canvas.drawLine(marginX, y - 14f, marginX + contentWidth, y - 14f, dividerPaint)
            }
            y += 16f
        }

        y += 24f
        drawSectionTitle("AVISOS CADASTRADOS")
        if (proximos.isEmpty()) {
            canvas.drawText("Nenhum aviso cadastrado.", marginX, y, bodyPaint)
            y += 16f
        } else {
            val headerHeight = 22f
            val headerBg = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
            canvas.drawRect(marginX, y, marginX + contentWidth, y + headerHeight, headerBg)
            val headerTextPaint = Paint(labelPaint).apply {
                color = android.graphics.Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerY = y + 15f
            canvas.drawText("Item", marginX + 6f, headerY, headerTextPaint)
            canvas.drawText("Data", marginX + 240f, headerY, headerTextPaint)
            canvas.drawText("KM", marginX + 330f, headerY, headerTextPaint)
            canvas.drawText("Cat.", marginX + 420f, headerY, headerTextPaint)
            y += headerHeight + 8f
            proximos.take(10).forEach { (lembrete, data) ->
                ensureSpace(26f)
                val rowTextY = y + 4f
                canvas.drawText(fit(lembrete.titulo, 28), marginX + 6f, rowTextY, bodyPaint)
                canvas.drawText(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), marginX + 240f, rowTextY, bodyPaint)
                canvas.drawText(lembrete.kmLimite.ifBlank { "-" }, marginX + 330f, rowTextY, bodyPaint)
                canvas.drawText(fit(lembrete.tipo.label, 8), marginX + 420f, rowTextY, bodyPaint)
                y += 26f
                canvas.drawLine(marginX, y - 14f, marginX + contentWidth, y - 14f, dividerPaint)
            }
        }
        y += 18f
        canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
        y += 24f
        if (logoBitmap != null) {
            val targetWidth = 160f
            val scale = targetWidth / logoBitmap.width.toFloat()
            val targetHeight = logoBitmap.height * scale
            val scaled = Bitmap.createScaledBitmap(logoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
            val left = (pageInfo.pageWidth - targetWidth) / 2f
            canvas.drawBitmap(scaled, left, y, null)
            y += targetHeight
        }

        if (!isPremium) {
            canvas.drawText("Gerado pelo Zellu", pageInfo.pageWidth / 2f, pageInfo.pageHeight - 24f, watermarkPaint)
        }
        document.finishPage(currentPage)
        val anoNoModelo = Regex("(19|20)\\d{2}").find(carro.modelo)?.value ?: LocalDate.now().year.toString()
        val dataArquivo = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        val nomeCarroNormalizado = Normalizer.normalize(carro.nome.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "veiculo" }
        val nomeArquivo = "relatorio_${nomeCarroNormalizado}_${anoNoModelo}_${dataArquivo}.pdf"
        val pdfFile = File(context.cacheDir, nomeArquivo)
        FileOutputStream(pdfFile).use { output -> document.writeTo(output) }
        document.close()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    } catch (e: Exception) {
        Log.e("PDF", "Erro ao gerar PDF", e)
        null
    }
}

fun gerarPdfFinanceiro(
    context: Context,
    carros: List<CarroInfo>,
    abastecimentos: List<Abastecimento>,
    lembretes: List<Lembrete>
): Uri? {
    return try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas
        val titlePaint = Paint().apply {
            textSize = 22f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            textSize = 10.5f
            color = android.graphics.Color.parseColor("#475569")
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            textSize = 12.5f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val valueBoldPaint = Paint().apply {
            textSize = 12.5f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            strokeWidth = 1.2f
            color = android.graphics.Color.parseColor("#CBD5E1")
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val accentPaint = Paint().apply { color = android.graphics.Color.parseColor("#2563EB") }
        val marginX = 36f
        val contentWidth = pageInfo.pageWidth - marginX * 2
        var y = 72f

        fun fit(text: String, maxChars: Int): String =
            if (text.length <= maxChars) text else text.take(maxChars - 3) + "..."

        fun ensureSpace(extra: Float) {
            if (y + extra > pageInfo.pageHeight - 40) {
                document.finishPage(currentPage)
                val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                currentPage = document.startPage(nextPageInfo)
                canvas = currentPage.canvas
                y = 72f
            }
        }

        fun drawHeader() {
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 6f, accentPaint)
            val titleCenterPaint = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("RELATORIO FINANCEIRO", pageInfo.pageWidth / 2f, 48f, titleCenterPaint)
            val subtitleCenterPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(
                "Gerado em ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                pageInfo.pageWidth / 2f,
                70f,
                subtitleCenterPaint
            )
            canvas.drawLine(marginX, 84f, pageInfo.pageWidth - marginX, 84f, dividerPaint)
        }

        fun drawSectionTitle(title: String) {
            ensureSpace(24f)
            canvas.drawText(title, marginX, y, sectionPaint)
            y += 8f
            canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
            y += 14f
        }

        fun drawCard(height: Float, content: (Float) -> Unit) {
            ensureSpace(height + 10f)
            val rect = android.graphics.RectF(marginX, y, marginX + contentWidth, y + height)
            canvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
            content(y)
            y += height + 20f
        }

        drawHeader()
        y = 108f

        val totalManutencao = lembretes.sumOf { it.valor }
        val totalCombustivel = abastecimentos.sumOf { it.valorPago }
        val totalGeral = totalManutencao + totalCombustivel
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        drawSectionTitle("RESUMO GERAL")
        drawCard(88f) { topY ->
            val colLeft = marginX + 14f
            val colRight = marginX + contentWidth / 2f + 8f
            fun drawRow(label: String, value: String, x: Float, rowY: Float, bold: Boolean = false) {
                canvas.drawText(label.uppercase(Locale.getDefault()), x, rowY, labelPaint)
                canvas.drawText(value, x, rowY + 18f, if (bold) valueBoldPaint else valuePaint)
            }
            drawRow("Total geral", formatarMoeda(totalGeral), colLeft, topY + 22f, true)
            drawRow("Total manutencao", formatarMoeda(totalManutencao), colRight, topY + 22f)
            drawRow("Total combustivel", formatarMoeda(totalCombustivel), colLeft, topY + 56f)
        }

        drawCard(58f) { topY ->
            val colLeft = marginX + 14f
            val colRight = marginX + contentWidth / 2f + 8f
            canvas.drawText("VEICULOS CADASTRADOS", colLeft, topY + 22f, labelPaint)
            canvas.drawText(carros.size.toString(), colLeft, topY + 36f, valueBoldPaint)
            canvas.drawText("LEMBRETES ATIVOS", colRight, topY + 22f, labelPaint)
            canvas.drawText(lembretes.size.toString(), colRight, topY + 36f, valueBoldPaint)
        }

        y += 10f
        drawSectionTitle("GASTOS POR VEICULO")

        val gastoPorVeiculo = carros.associate { carro ->
            val gastoManutencao = lembretes.filter { it.carroId == carro.id }.sumOf { it.valor }
            val gastoComb = abastecimentos.filter { it.carroId == carro.id }.sumOf { it.valorPago }
            carro to (gastoManutencao + gastoComb)
        }.entries.sortedByDescending { it.value }

        if (gastoPorVeiculo.isEmpty()) {
            canvas.drawText("Nenhum gasto registrado.", marginX, y, labelPaint)
            y += 16f
        } else {
            val totalPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
            val tipoPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
            gastoPorVeiculo.forEach { (carro, total) ->
                ensureSpace(40f)
                val nome = fit(carro.nome.ifBlank { "Veiculo" }, 24)
                val gastoComb = abastecimentos.filter { it.carroId == carro.id }.sumOf { it.valorPago }
                val detalheExtra = if (carro.tipoVeiculo == TipoVeiculo.BICICLETA) {
                    carro.modelo.ifBlank { "" }.let { if (it.isBlank()) "" else "Aro: $it" }
                } else {
                    carro.modelo.ifBlank { "" }.let { if (it.isBlank()) "" else "Motor: $it" }
                }
                val isBikeRow = carro.tipoVeiculo == TipoVeiculo.BICICLETA || carro.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
                val exibirKmRow = !isBikeRow || !carro.semControleKm
                val kmTexto = if (exibirKmRow && carro.kmAtual > 0) "KM: ${carro.kmAtual}" else ""
                val combustivelTexto = if (gastoComb > 0.0) "Comb: ${formatarMoeda(gastoComb)}" else ""
                val detalhes = listOf(carro.marca, detalheExtra, kmTexto, combustivelTexto)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                val detalheTexto = if (detalhes.isBlank()) "Sem detalhes" else fit(detalhes, 28)
                canvas.drawText(nome, marginX, y + 6f, valueBoldPaint)
                canvas.drawText(detalheTexto, marginX, y + 20f, labelPaint)
                canvas.drawText(formatarMoeda(total), pageInfo.pageWidth - marginX, y + 12f, totalPaint)
                canvas.drawText(carro.tipoVeiculo.label, pageInfo.pageWidth - marginX, y + 24f, tipoPaint)
                y += 40f
                canvas.drawLine(marginX, y - 8f, pageInfo.pageWidth - marginX, y - 8f, dividerPaint)
            }
        }

        // Rodape com banner
        val bannerBitmap = try {
            context.assets.open("ZelluBanner.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
        if (bannerBitmap != null) {
            val maxWidth = contentWidth * 0.55f
            val scale = maxWidth / bannerBitmap.width.toFloat()
            val targetWidth = bannerBitmap.width * scale
            val targetHeight = bannerBitmap.height * scale
            ensureSpace(targetHeight + 20f)
            val left = (pageInfo.pageWidth - targetWidth) / 2f
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(
                    bannerBitmap,
                    targetWidth.toInt(),
                    targetHeight.toInt(),
                    true
                ),
                left,
                y,
                null
            )
            y += targetHeight + 10f
        }

        document.finishPage(currentPage)
        val pdfFile = File(context.cacheDir, "relatorio_financeiro_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { output -> document.writeTo(output) }
        document.close()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    } catch (e: Exception) {
        Log.e("PDF", "Erro ao gerar PDF financeiro", e)
        null
    }
}

fun compartilharPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
}

private fun corNomePorArgb(argb: Int): String {
    val cores = listOf(
        "Branco" to 0xFFFFFFFF.toInt(),
        "Preto" to 0xFF0F172A.toInt(),
        "Prata" to 0xFFC0C0C0.toInt(),
        "Cinza" to 0xFF9CA3AF.toInt(),
        "Vermelho" to 0xFFDC2626.toInt(),
        "Azul" to 0xFF4F7DBE.toInt(),
        "Marrom" to 0xFF7C3F00.toInt(),
        "Bege" to 0xFFE7D7C1.toInt(),
        "Verde" to 0xFF16A34A.toInt(),
        "Amarelo" to 0xFFFACC15.toInt(),
        "Laranja" to 0xFFF97316.toInt(),
        "Roxo" to 0xFF6D5BD0.toInt(),
        "Rosa" to 0xFFEC4899.toInt(),
        "Dourado" to 0xFFC0841A.toInt(),
        "Bordô" to 0xFF7F1D1D.toInt(),
        "Turquesa" to 0xFF38BDF8.toInt(),
        "Creme" to 0xFFF5F5DC.toInt()
    )
    return cores.firstOrNull { it.second == argb }?.first ?: "Personalizada"
}







