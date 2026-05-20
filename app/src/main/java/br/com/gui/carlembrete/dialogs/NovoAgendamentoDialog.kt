package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import android.widget.Toast
import java.net.URLEncoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.gui.carlembrete.R
import br.com.gui.carlembrete.VehicleIcon
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private enum class FluxoCadastroAviso {
    CRIAR_LEMBRETE,
    REGISTRAR_SERVICO
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoAgendamentoDialog(
    carroAtual: CarroInfo,
    contatosDisponiveis: List<ContatoProfissional>,
    onDismiss: () -> Unit,
    onBackToTipoAviso: () -> Unit = onDismiss,
    onConfirm: (Lembrete) -> Unit,
    onMultiConfirm: (List<Lembrete>) -> Unit,
    onUpdateKmCarro: (Int) -> Unit,
    autoAbrirCamera: Boolean = false,
    onAutoCameraConsumida: () -> Unit = {},
    onAddContato: (ContatoProfissional) -> Unit = {},
    initialTipo: TipoManutencao = TipoManutencao.OLEO,
    initialRegistroServico: Boolean? = null,
    planTier: PlanTier,
    adminReminderLimitOverride: Int? = null,
    activeReminderCount: Int = 0,
    activeRecordCount: Int = 0,
    activeFuelRecordCount: Int = 0,
    onFuelRecordsSaved: (List<Abastecimento>, Int) -> Unit = { _, _ -> },
    onRequestPremium: (String) -> Unit,
    onOpenVehicleGuide: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val appContext = context.applicationContext
    val englishUi = isEnglishUi()
    val reminderLimit = effectiveReminderLimitForPlan(planTier, adminReminderLimitOverride)
    val fuelRecordLimit = fuelRecordLimitForPlan(planTier)
    val scannerLimit = scannerLimitForPlan(planTier)
    val planLabel = planNameLabel(planTier)
    val canUseRecurringReminders = planTier != PlanTier.FREE
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color.Black else scheme.background
    val surfaceCardColor = if (isDark) Color(0xFF111827) else scheme.surface
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f)
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val iconColor = if (isDark) Color.White else Color.Black
    val modalContainer = if (isDark) Color(0xFF111827) else scheme.surface
    val modalTextSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val modalOptionContainer = if (isDark) Color(0xFF111827) else scheme.surface
    val modalOptionSelectedContainer = if (isDark) Color(0xFF1F2937) else Color(0xFFEFF6FF)
    val modalOptionBorder = if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE)
    val modalPrimaryAction = Color(0xFF3B82F6)
    val nextActionBlue = Color(0xFF3B82F6)
    val neutralButtonContainer = if (isDark) Color(0xFF111827) else scheme.surface
    val neutralButtonContent = textPrimary
    val neutralButtonBorder = if (isDark) Color(0xFF334155) else scheme.outlineVariant
    val categoriasDisponiveis = tiposAvisoCadastroPorVeiculo(carroAtual.tipoVeiculo)
    var tituloAviso by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var localServicoInput by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var kmBase by remember { mutableStateOf(if (carroAtual.kmAtual > 0) carroAtual.kmAtual.toString() else "") }
    var estadoUfSelecionado by remember {
        mutableStateOf(
            kmBase
                .trim()
                .uppercase(Locale("pt", "BR"))
                .takeIf { it.length == 2 && it.all(Char::isLetter) }
                ?: "SP"
        )
    }
    var valorInput by remember { mutableStateOf("") }
    var quantidadeManualInput by remember { mutableStateOf("1") }
    var avisoSemTotal by remember { mutableStateOf(false) }
    var avisoSemQuantidade by remember { mutableStateOf(false) }
    var tipoSelecionado by remember { mutableStateOf(initialTipo) }
    val anoAtual = remember { LocalDate.now().year }
    var contatosLista by remember { mutableStateOf(contatosDisponiveis) }
    var contatoSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    var listaItensDetectados by remember { mutableStateOf<List<ItemDetectado>>(emptyList()) }
    var isModoLista by remember { mutableStateOf(false) }
    var qrPossuiItensSeparaveis by remember { mutableStateOf(false) }
    var qrModoSeparado by remember { mutableStateOf(false) }
    var descricaoQrConsolidada by remember { mutableStateOf("") }
    var itemDataAvisoOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemHoraAvisoOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemValorOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemTituloOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemTipoOverrides by remember { mutableStateOf<Map<String, TipoManutencao>>(emptyMap()) }
    var itemCategoriaMenuAbertoId by remember { mutableStateOf<String?>(null) }
    var showKmConfirmDialog by remember { mutableStateOf(false) }
    var kmDetectadoParaConfirmar by remember { mutableStateOf(0) }
    var showKmSugeridoDialog by remember { mutableStateOf(false) }
    var kmSugeridoParaConfirmar by remember { mutableStateOf<Int?>(null) }
    var textoKmSugeridoDetalhe by remember { mutableStateOf("") }
    var tipoMenuItemId by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var fotoCaminho by remember { mutableStateOf<String?>(null) }
    var horaNotificacao by remember { mutableStateOf("09:00") }
    var dataAviso by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var frequenciaLembreteKey by remember { mutableStateOf("NONE") }
    var repetirAteDesativar by remember { mutableStateOf(false) }
    var intervaloDiasPersonalizado by remember { mutableStateOf("1") }
    var intervaloMesesPersonalizado by remember { mutableStateOf("3") }
    var intervaloAnosPersonalizado by remember { mutableStateOf("1") }
    var menuFrequenciaExpanded by remember { mutableStateOf(false) }
    val isInitialPosto = initialTipo == TipoManutencao.ABASTECIMENTO
    var avisoPersonalizado by remember { mutableStateOf(false) }
    var etapaAtual by remember { mutableStateOf(1) }
    var showGuiaManutencao by remember { mutableStateOf(false) }
    var fluxoCadastro by remember(initialRegistroServico) {
        mutableStateOf(
            when (initialRegistroServico) {
                true -> FluxoCadastroAviso.REGISTRAR_SERVICO
                false -> FluxoCadastroAviso.CRIAR_LEMBRETE
                null -> FluxoCadastroAviso.CRIAR_LEMBRETE
            }
        )
    }
    val isFluxoPosto = tipoSelecionado == TipoManutencao.ABASTECIMENTO
    val isRegistroServico = fluxoCadastro == FluxoCadastroAviso.REGISTRAR_SERVICO
    var textosDetectados by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTextosDialog by remember { mutableStateOf(false) }
    var textoSelecionadoDialog by remember { mutableStateOf<String?>(null) }
    var showMarcaDialog by remember { mutableStateOf(false) }
    var produtoSelecionadoDialog by remember { mutableStateOf<String?>(null) }
    var marcaSelecionadaDialog by remember { mutableStateOf<String?>(null) }
    var qrNomeEstabelecimento by remember { mutableStateOf("") }
    var qrEnderecoEstabelecimento by remember { mutableStateOf("") }
    var qrQuantidadeTotalItens by remember { mutableStateOf<Int?>(null) }
    var qrValorTotalBruto by remember { mutableStateOf<Double?>(null) }
    var qrValorDesconto by remember { mutableStateOf<Double?>(null) }
    var qrValorFinalComDesconto by remember { mutableStateOf<Double?>(null) }
    var qrFormaPagamento by remember { mutableStateOf<String?>(null) }
    var usarCadastroManualPosScan by remember { mutableStateOf(false) }
    var qrUrlValidacaoSpPendente by remember { mutableStateOf<String?>(null) }
    var resultadoQrPendente by remember { mutableStateOf<ResultadoCaptura?>(null) }
    var reprocessandoQrSp by remember { mutableStateOf(false) }
    var showValidacaoSpWebView by remember { mutableStateOf(false) }
    var webViewSpCarregando by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var tutorialVideoPath by remember { mutableStateOf<String?>(null) }
    var descricaoAntesDialog by remember { mutableStateOf("") }
    var tipoAntesDialog by remember { mutableStateOf(TipoManutencao.OLEO) }
    var showScannerGuide by remember { mutableStateOf(false) }
    var profissionaisDaCidade by remember { mutableStateOf<List<ProfissionalCidadeEncontrado>>(emptyList()) }
    var cidadeAtual by remember { mutableStateOf<String?>(null) }
    var ufAtual by remember { mutableStateOf<String?>(null) }
    var carregandoProfissionaisCidade by remember { mutableStateOf(false) }
    var erroProfissionaisCidade by remember { mutableStateOf<String?>(null) }
    var jaCarregouProfissionaisCidade by remember { mutableStateOf(false) }
    var profissionalParaCompletarTelefone by remember { mutableStateOf<ProfissionalCidadeEncontrado?>(null) }
    var telefoneCompletarInput by remember { mutableStateOf("") }
    var enviarWhatsappAposCompletarTelefone by remember { mutableStateOf(false) }
    val profissionaisListState = rememberLazyListState()
    val loadingTransition = rememberInfiniteTransition(label = "profissionais_loading")
    val loadingAlpha by loadingTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "profissionais_loading_alpha"
    )
    val isBikeVehicle = carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA || carroAtual.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
    val activity = remember(context) { context.findActivity() }
    val scannerGuidePrefs = remember(context) {
        context.getSharedPreferences("scanner_guide_prefs", Context.MODE_PRIVATE)
    }
    val scannerGuideImageResId = remember(context) {
        context.resources.getIdentifier("notaexemplo", "drawable", context.packageName)
    }

    DisposableEffect(activity, isDark, pageBackground) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val oldStatusColor = window.statusBarColor
            val oldNavColor = window.navigationBarColor
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            val oldLightStatus = insetsController.isAppearanceLightStatusBars
            val oldLightNav = insetsController.isAppearanceLightNavigationBars
            val targetBarColor = pageBackground.toArgb()
            window.statusBarColor = targetBarColor
            window.navigationBarColor = targetBarColor
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
            onDispose {
                window.statusBarColor = oldStatusColor
                window.navigationBarColor = oldNavColor
                insetsController.isAppearanceLightStatusBars = oldLightStatus
                insetsController.isAppearanceLightNavigationBars = oldLightNav
            }
        }
    }

    LaunchedEffect(carroAtual.tipoVeiculo) {
        if (tipoSelecionado !in categoriasDisponiveis) {
            tipoSelecionado = categoriasDisponiveis.firstOrNull() ?: TipoManutencao.OUTROS
        }
        val categoriaFallback = categoriasDisponiveis.firstOrNull() ?: TipoManutencao.OUTROS
        itemTipoOverrides = itemTipoOverrides.mapValues { (_, tipoAtual) ->
            if (tipoAtual in categoriasDisponiveis) tipoAtual else categoriaFallback
        }
    }

    LaunchedEffect(tipoSelecionado) {
        if (tipoSelecionado == TipoManutencao.ABASTECIMENTO) {
            avisoSemTotal = false
            avisoSemQuantidade = false
            if (quantidadeManualInput.isBlank()) quantidadeManualInput = "1"
        }
        if (tituloAviso.isBlank()) {
            tituloAviso = tipoSelecionado.label
        }
        val sugestaoDescricao = descricaoPadraoPorCategoria(tipoSelecionado, anoAtual) ?: return@LaunchedEffect
        if (descricao.isBlank() || isDescricaoPadraoCategoria(descricao)) {
            descricao = sugestaoDescricao
        }
    }
    val dataFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scope = rememberCoroutineScope()
    val etapasScrollState = rememberScrollState()
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val textoReconhecido = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!textoReconhecido.isNullOrBlank()) {
                descricao = textoReconhecido
                tipoSelecionado = detectarTipoPeloTexto(textoReconhecido)
            }
        }
    }
    fun limparDadosFinanceirosNotaScan() {
        qrQuantidadeTotalItens = null
        qrValorTotalBruto = null
        qrValorDesconto = null
        qrValorFinalComDesconto = null
        qrFormaPagamento = null
        avisoSemTotal = false
        avisoSemQuantidade = false
    }
    fun aplicarDadosFinanceirosNotaScan(notaInfo: NotaQrInfo) {
        qrQuantidadeTotalItens = notaInfo.quantidadeTotalItens
        qrValorTotalBruto = notaInfo.valorBruto
        qrValorDesconto = notaInfo.valorDesconto
        qrValorFinalComDesconto = notaInfo.valorFinalComDesconto
        qrFormaPagamento = notaInfo.formaPagamento
        avisoSemTotal = false
        avisoSemQuantidade = false
        val valorPreferencial = notaInfo.valorFinalComDesconto ?: notaInfo.valorTotal ?: notaInfo.valorBruto
        if (valorPreferencial != null) {
            valorInput = String.format(Locale.US, "%.2f", valorPreferencial)
        }
    }
    fun aplicarTituloPeloScan(nomeEstabelecimento: String) {
        val nomeLimpo = nomeEstabelecimento.trim()
        if (nomeLimpo.isBlank()) return
        tituloAviso = nomeLimpo
    }
    fun aplicarNotaReprocessadaNoFormulario(notaReconsultada: NotaQrInfo, resultadoPendente: ResultadoCaptura) {
        val valorExtraido = notaReconsultada.valorFinalComDesconto ?: notaReconsultada.valorTotal ?: notaReconsultada.valorBruto
        val dataExtraida = notaReconsultada.dataCompra

        aplicarDadosFinanceirosNotaScan(notaReconsultada)
        if (!dataExtraida.isNullOrBlank()) {
            data = dataExtraida
            dataAviso = dataExtraida
            avisoPersonalizado = true
        }
        qrNomeEstabelecimento = notaReconsultada.nomeEstabelecimento.orEmpty()
        qrEnderecoEstabelecimento = notaReconsultada.enderecoEstabelecimento.orEmpty()
        aplicarTituloPeloScan(qrNomeEstabelecimento)
        val descricaoExtraida = notaReconsultada.descricaoItens?.trim()
        val descricaoExtraidaSemTotal = descricaoExtraida?.let { limparTextoProdutosRemovendoTotal(it) }
        val itensQr = extrairItensDaDescricaoQr(descricaoExtraida)
        qrPossuiItensSeparaveis = itensQr.size > 1
        qrModoSeparado = false
        if (itensQr.isNotEmpty()) {
            listaItensDetectados = itensQr
            if (valorExtraido == null) {
                val totalItens = itensQr.sumOf { item -> item.valor }
                if (totalItens > 0.0) {
                    valorInput = String.format(Locale.US, "%.2f", totalItens)
                }
            }
            itemDataAvisoOverrides = emptyMap()
            itemHoraAvisoOverrides = emptyMap()
            itemValorOverrides = emptyMap()
            itemTituloOverrides = emptyMap()
            itemTipoOverrides = itensQr.associate { item -> item.id to item.tipo }
        } else {
            itemDataAvisoOverrides = emptyMap()
            itemHoraAvisoOverrides = emptyMap()
            itemValorOverrides = emptyMap()
            itemTituloOverrides = emptyMap()
            itemTipoOverrides = emptyMap()
        }
        itemCategoriaMenuAbertoId = null
        localServicoInput = montarLocalNota(
            estabelecimento = qrNomeEstabelecimento,
            endereco = qrEnderecoEstabelecimento
        )
        descricaoQrConsolidada = montarDescricaoItensNota(
            total = notaReconsultada.valorBruto ?: notaReconsultada.valorTotal,
            itens = descricaoExtraidaSemTotal,
            desconto = notaReconsultada.valorDesconto,
            valorFinal = notaReconsultada.valorFinalComDesconto ?: notaReconsultada.valorTotal,
            quantidadeTotalItens = notaReconsultada.quantidadeTotalItens
        )
        val sugestaoOcr = resultadoPendente.sugestoesProduto
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
        val sugestaoOcrLimpa =
            sugestaoOcr?.let { limparTextoProdutosRemovendoTotal(it) }?.takeIf { it.isNotBlank() }
        descricao = when {
            descricaoExtraidaSemTotal.isNullOrBlank() && !sugestaoOcrLimpa.isNullOrBlank() -> sugestaoOcrLimpa
            else -> descricaoQrConsolidada
        }
        isModoLista = false
    }

    fun reprocessarNotaSpAposValidacao(urlReconsulta: String, origem: String) {
        val resultadoPendente = resultadoQrPendente ?: return
        val cookieHeader = CookieManager.getInstance().getCookie(urlReconsulta)
        scope.launch {
            reprocessandoQrSp = true
            val notaReconsultada = consultarNotaPorQrCode(
                url = urlReconsulta,
                cookieHeader = cookieHeader
            )
            reprocessandoQrSp = false
            if (notaReconsultada != null && !notaQrIndicaBloqueioSp(notaReconsultada)) {
                aplicarNotaReprocessadaNoFormulario(notaReconsultada, resultadoPendente)
                showValidacaoSpWebView = false
                qrUrlValidacaoSpPendente = null
                resultadoQrPendente = null
                Toast.makeText(context, "Nota validada na SEFAZ-SP e reprocessada.", Toast.LENGTH_SHORT).show()
                Log.i(QR_PARSER_TAG, "Reprocessamento SP concluido com sucesso. origem=$origem")
            } else {
                Toast.makeText(
                    context,
                    "Validacao concluida, mas os dados da nota ainda estao indisponiveis.",
                    Toast.LENGTH_LONG
                ).show()
                Log.w(QR_PARSER_TAG, "Reprocessamento SP sem dados apos validacao. origem=$origem")
            }
        }
    }

    fun tentarAbrirCamera(exibirGuia: Boolean = true) {
        if (!AppPreferences.canUseOcr(context, scannerLimit)) {
            Toast.makeText(
                context,
                "Limite do plano $planLabel: $scannerLimit scans por mes.",
                Toast.LENGTH_LONG
            ).show()
            onRequestPremium("scanner_limit")
            return
        }
        val chaveGuiaNovoAviso = "mostrar_guia_scanner_produto_novo_aviso"
        val mostrarGuia = scannerGuidePrefs.getBoolean(chaveGuiaNovoAviso, true)
        if (exibirGuia && mostrarGuia) {
            scannerGuidePrefs.edit().putBoolean(chaveGuiaNovoAviso, false).apply()
            showScannerGuide = true
            return
        }
        showCamera = true
    }

    fun prepararVideoTutorialLocal(): String? {
        return runCatching {
            val destino = File(context.cacheDir, "tutorial.mp4")
            if (!destino.exists() || destino.length() <= 0L) {
                context.assets.open("tutorial.mp4").use { input ->
                    destino.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            destino.absolutePath
        }.onFailure {
            Log.e(QR_PARSER_TAG, "Falha ao preparar video tutorial em cache.", it)
        }.getOrNull()
    }

    LaunchedEffect(autoAbrirCamera) {
        if (autoAbrirCamera) {
            tentarAbrirCamera(exibirGuia = false)
            onAutoCameraConsumida()
        }
    }

    fun adicionarKm(valor: Int) { val kmBaseInt = kmBase.toIntOrNull() ?: carroAtual.kmAtual; kmBase = (kmBaseInt + valor).toString() }
    LaunchedEffect(data, tipoSelecionado) {
        if (!avisoPersonalizado) {
            val dataBase = try { LocalDate.parse(data, dataFormatter) } catch (e: Exception) { LocalDate.now() }
            dataAviso = calcularProximaData(tipoSelecionado, dataBase)
        }
    }

    LaunchedEffect(etapaAtual) {
        etapasScrollState.scrollTo(0)
    }

    fun abrirDatePicker(dataAtual: String, aoSelecionar: (String) -> Unit) {
        val atual = try { LocalDate.parse(dataAtual, dataFormatter) } catch (e: Exception) { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                aoSelecionar("%02d/%02d/%04d".format(dayOfMonth, month + 1, year))
            },
            atual.year,
            atual.monthValue - 1,
            atual.dayOfMonth
        ).show()
    }

    fun abrirTimePicker(horaAtual: String, aoSelecionar: (String) -> Unit) {
        val partes = horaAtual.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 9
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute -> aoSelecionar("%02d:%02d".format(hour, minute)) },
            hora,
            minuto,
            true
        ).show()
    }

    fun formatarKmCampo(valor: String): String {
        val numero = valor.filter(Char::isDigit).toLongOrNull() ?: return valor
        return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(numero)
    }

    fun kmOuEstadoPorTipo(tipo: TipoManutencao, kmAtualBase: Int): String {
        return when (tipo) {
            TipoManutencao.IPVA,
            TipoManutencao.LICENCIAMENTO -> estadoUfSelecionado
            else -> (kmAtualBase + getKmAdicionalPorTipo(tipo)).toString()
        }
    }

    fun tipoPermiteFrequencia(tipo: TipoManutencao): Boolean {
        return tipo != TipoManutencao.LICENCIAMENTO &&
            tipo != TipoManutencao.SEGURO &&
            tipo != TipoManutencao.IPVA &&
            tipo != TipoManutencao.ABASTECIMENTO
    }

    fun diaSemanaDaDataAviso(): String {
        val dia = runCatching {
            LocalDate.parse(dataAviso, dataFormatter).dayOfWeek
        }.getOrDefault(DayOfWeek.WEDNESDAY)
        return when (dia) {
            DayOfWeek.MONDAY -> "segunda-feira"
            DayOfWeek.TUESDAY -> "terça-feira"
            DayOfWeek.WEDNESDAY -> "quarta-feira"
            DayOfWeek.THURSDAY -> "quinta-feira"
            DayOfWeek.FRIDAY -> "sexta-feira"
            DayOfWeek.SATURDAY -> "sábado"
            DayOfWeek.SUNDAY -> "domingo"
        }
    }

    fun limitesIntervaloPorFrequencia(): IntRange = when (frequenciaLembreteKey) {
        "DAY" -> 1..31
        "MONTH" -> 1..12
        "YEAR" -> 1..10
        else -> 1..31
    }

    fun intervaloAtualTexto(): String = when (frequenciaLembreteKey) {
        "DAY" -> intervaloDiasPersonalizado
        "MONTH" -> intervaloMesesPersonalizado
        "YEAR" -> intervaloAnosPersonalizado
        else -> ""
    }

    fun atualizarIntervaloAtual(valor: String) {
        val digitsOnly = valor.filter(Char::isDigit).take(2)
        if (digitsOnly.isBlank()) {
            when (frequenciaLembreteKey) {
                "DAY" -> intervaloDiasPersonalizado = ""
                "MONTH" -> intervaloMesesPersonalizado = ""
                "YEAR" -> intervaloAnosPersonalizado = ""
            }
            return
        }
        val limites = limitesIntervaloPorFrequencia()
        val ajustado = digitsOnly.toIntOrNull()?.coerceIn(limites.first, limites.last)?.toString().orEmpty()
        when (frequenciaLembreteKey) {
            "DAY" -> intervaloDiasPersonalizado = ajustado
            "MONTH" -> intervaloMesesPersonalizado = ajustado
            "YEAR" -> intervaloAnosPersonalizado = ajustado
        }
    }

    fun descricaoFrequenciaSelecionada(): String = when (frequenciaLembreteKey) {
        "DAY" -> if (englishUi) "Days" else "Dias"
        "MONTH" -> if (englishUi) "Months" else "Meses"
        "YEAR" -> if (englishUi) "Years" else "Anos"
        else -> if (englishUi) "Do not repeat" else "Não repetir"
    }

    fun recorrenciaSelecionadaConfig(): Pair<String, Int>? {
        if (!canUseRecurringReminders) return null
        if (!repetirAteDesativar) return null
        return when (frequenciaLembreteKey) {
            "DAY" -> {
                val dias = intervaloDiasPersonalizado.filter(Char::isDigit).toIntOrNull() ?: 0
                if (dias > 0) NotificacaoHelper.REC_UNIT_DAY to dias.coerceIn(1, 31) else null
            }
            "MONTH" -> {
                val meses = intervaloMesesPersonalizado.filter(Char::isDigit).toIntOrNull() ?: 0
                if (meses > 0) NotificacaoHelper.REC_UNIT_MONTH to meses.coerceIn(1, 12) else null
            }
            "YEAR" -> {
                val anos = intervaloAnosPersonalizado.filter(Char::isDigit).toIntOrNull() ?: 0
                if (anos > 0) NotificacaoHelper.REC_UNIT_YEAR to anos.coerceIn(1, 10) else null
            }
            else -> null
        }
    }

    fun dataHojeFormatada(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    fun proximaDataRecorrenteFutura(
        dataInicial: String,
        hora: String,
        recorrenciaConfig: Pair<String, Int>?
    ): String {
        if (recorrenciaConfig == null) return dataInicial
        var dataCandidata = runCatching { LocalDate.parse(dataInicial, dataFormatter) }.getOrElse { LocalDate.now() }
        val partesHora = hora.split(":")
        val horario = runCatching {
            LocalTime.of(
                partesHora.getOrNull(0)?.toIntOrNull() ?: 9,
                partesHora.getOrNull(1)?.toIntOrNull() ?: 0
            )
        }.getOrElse { LocalTime.of(9, 0) }
        val agora = java.time.LocalDateTime.now()
        repeat(500) {
            if (dataCandidata.atTime(horario).isAfter(agora)) {
                return dataCandidata.format(dataFormatter)
            }
            dataCandidata = when (recorrenciaConfig.first) {
                NotificacaoHelper.REC_UNIT_DAY -> dataCandidata.plusDays(recorrenciaConfig.second.toLong())
                NotificacaoHelper.REC_UNIT_MONTH -> dataCandidata.plusMonths(recorrenciaConfig.second.toLong())
                NotificacaoHelper.REC_UNIT_YEAR -> dataCandidata.plusYears(recorrenciaConfig.second.toLong())
                else -> dataCandidata.plusDays(recorrenciaConfig.second.toLong())
            }
        }
        return dataCandidata.format(dataFormatter)
    }

    fun aplicarPrimeiroAvisoAgoraSeRecorrente() {
        if (!repetirAteDesativar) return
        if (frequenciaLembreteKey != "DAY" && frequenciaLembreteKey != "MONTH" && frequenciaLembreteKey != "YEAR") return
        // Mantem a data escolhida pelo usuario: a recorrencia deve nascer da data do aviso,
        // nao do dia em que a repeticao foi ativada.
    }

    fun iniciarCapturaVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            putExtra(RecognizerIntent.EXTRA_PROMPT, "Descreva o serviço realizado")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Recursos de voz indisponíveis", Toast.LENGTH_SHORT).show()
        }
    }

    fun adicionarContatoDaCidade(profissional: ProfissionalCidadeEncontrado): ContatoProfissional {
        val telefoneLimpo = profissional.telefone.filter(Char::isDigit)
        val existente = contatosLista.firstOrNull { atual ->
            val atualTelefone = atual.telefone.filter(Char::isDigit)
            (telefoneLimpo.isNotBlank() && atualTelefone == telefoneLimpo) ||
                atual.nome.trim().equals(profissional.nome.trim(), ignoreCase = true)
        }
        if (existente != null) {
            val atualizado = if (existente.telefone.isBlank() && telefoneLimpo.isNotBlank()) {
                existente.copy(telefone = profissional.telefone)
            } else {
                existente
            }
            if (atualizado != existente) {
                contatosLista = contatosLista.map { if (it.id == existente.id) atualizado else it }
            }
            contatoSelecionado = atualizado
            return atualizado
        }
        val novoContato = ContatoProfissional(
            nome = profissional.nome,
            telefone = profissional.telefone,
            tipoServico = if (profissional.endereco.isBlank()) {
                "Profissional da cidade"
            } else {
                "Profissional da cidade | Endereco: ${profissional.endereco}"
            }
        )
        contatosLista = contatosLista + novoContato
        onAddContato(novoContato)
        contatoSelecionado = novoContato
        Toast.makeText(context, "Profissional da cidade adicionado", Toast.LENGTH_SHORT).show()
        return novoContato
    }

    fun carregarProfissionaisDaCidade(forcar: Boolean = false) {
        if (carregandoProfissionaisCidade) {
            Log.d(PROF_CITY_TAG, "Busca ignorada: carregamento ja em andamento.")
            return
        }
        if (!forcar && jaCarregouProfissionaisCidade) {
            Log.d(PROF_CITY_TAG, "Busca ignorada: dados da cidade ja carregados.")
            return
        }
        Log.d(PROF_CITY_TAG, "Iniciando busca de profissionais da cidade. forcar=$forcar tipo=$tipoSelecionado")
        scope.launch {
            carregandoProfissionaisCidade = true
            erroProfissionaisCidade = null
            val resultado = withContext(Dispatchers.IO) {
                buscarProfissionaisDaCidadeAtual(context, tipoSelecionado, isBikeVehicle)
            }
            carregandoProfissionaisCidade = false
            resultado.onSuccess { busca ->
                cidadeAtual = busca.cidade
                ufAtual = busca.estado
                profissionaisDaCidade = busca.profissionais
                jaCarregouProfissionaisCidade = true
                Log.i(
                    PROF_CITY_TAG,
                    "Busca concluida com sucesso. cidade=${busca.cidade} uf=${busca.estado} total=${busca.profissionais.size}"
                )
                if (busca.profissionais.isEmpty()) {
                    erroProfissionaisCidade = "Nenhum profissional encontrado na sua cidade."
                }
            }.onFailure { erro ->
                profissionaisDaCidade = emptyList()
                erroProfissionaisCidade = erro.message ?: "Nao foi possivel buscar profissionais da cidade."
                jaCarregouProfissionaisCidade = false
                Log.e(PROF_CITY_TAG, "Falha na busca de profissionais da cidade.", erro)
            }
        }
    }

    fun enviarMensagemWhatsapp(contato: ContatoProfissional) {
        val telefone = contato.telefone.filter(Char::isDigit)
        if (telefone.isBlank()) {
            contatoSelecionado = contato
            profissionalParaCompletarTelefone = ProfissionalCidadeEncontrado(
                nome = contato.nome,
                telefone = "",
                endereco = ""
            )
            telefoneCompletarInput = ""
            enviarWhatsappAposCompletarTelefone = true
            Toast.makeText(context, "Informe o telefone para enviar no WhatsApp", Toast.LENGTH_SHORT).show()
            return
        }
        val mensagem = "Olá tudo bem?"
        val uri = Uri.parse("https://wa.me/$telefone?text=${URLEncoder.encode(mensagem, "UTF-8")}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    if (profissionalParaCompletarTelefone != null) {
        AlertDialog(
            onDismissRequest = { profissionalParaCompletarTelefone = null },
            title = { Text("Completar telefone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profissionalParaCompletarTelefone!!.nome, color = textPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = telefoneCompletarInput,
                        onValueChange = { telefoneCompletarInput = it },
                        label = { Text("Telefone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val telefoneLimpo = telefoneCompletarInput.filter(Char::isDigit)
                        if (telefoneLimpo.isBlank()) {
                            Toast.makeText(context, "Informe um telefone valido", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val base = profissionalParaCompletarTelefone ?: return@TextButton
                        val contatoAtualizado = adicionarContatoDaCidade(base.copy(telefone = telefoneLimpo))
                        if (enviarWhatsappAposCompletarTelefone) {
                            enviarWhatsappAposCompletarTelefone = false
                            enviarMensagemWhatsapp(contatoAtualizado)
                        }
                        profissionalParaCompletarTelefone = null
                        telefoneCompletarInput = ""
                    }
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    profissionalParaCompletarTelefone = null
                    telefoneCompletarInput = ""
                    enviarWhatsappAposCompletarTelefone = false
                }) { Text("Cancelar") }
            }
        )
    }

    fun abrirBuscaGoogleProfissional(nome: String) {
        val local = listOfNotNull(cidadeAtual, ufAtual).joinToString(" ")
        val query = listOf(nome, local).filter { it.isNotBlank() }.joinToString(" ")
        val uri = Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(context, "Não foi possível abrir o Google", Toast.LENGTH_SHORT).show()
        }
    }

    fun estimarKmSugeridoParaAbastecimento(): Int? {
        val kmAtualBase = kmBase.toIntOrNull() ?: carroAtual.kmAtual
        val valoresAbastecimento = if (isModoLista) {
            listaItensDetectados.mapNotNull { item ->
                val tipoItem = itemTipoOverrides[item.id] ?: tipoSelecionado
                if (tipoItem != TipoManutencao.ABASTECIMENTO) return@mapNotNull null
                itemValorOverrides[item.id]?.replace(",", ".")?.toDoubleOrNull() ?: item.valor
            }.filter { it > 0.0 }
        } else if (tipoSelecionado == TipoManutencao.ABASTECIMENTO) {
            listOf(valorInput.replace(",", ".").toDoubleOrNull() ?: 0.0).filter { it > 0.0 }
        } else {
            emptyList()
        }
        if (valoresAbastecimento.isEmpty()) return null

        val abastecimentosExistentes = BancoDeDados.carregarAbastecimentos(context)
        val precoReferencia = abastecimentosExistentes
            .asReversed()
            .firstOrNull { it.carroId == carroAtual.id && it.precoLitro > 0.0 }
            ?.precoLitro
            ?: 5.60
        val litrosEstimados = valoresAbastecimento.sumOf { total -> (total / precoReferencia).coerceAtLeast(0.01) }

        val kmInicial = AppPreferences.getFuelStartKm(context, carroAtual.id)
        val consumoHistorico = if (kmInicial != null && carroAtual.kmAtual > kmInicial) {
            val litrosHistoricos = abastecimentosExistentes
                .filter { it.carroId == carroAtual.id && it.litros > 0.0 }
                .sumOf { it.litros }
            if (litrosHistoricos > 0.0) {
                ((carroAtual.kmAtual - kmInicial) / litrosHistoricos).coerceIn(4.0, 30.0)
            } else {
                null
            }
        } else {
            null
        }
        val consumoPadrao = when (carroAtual.tipoVeiculo) {
            TipoVeiculo.MOTO -> 28.0
            TipoVeiculo.CAMINHAO, TipoVeiculo.ONIBUS -> 4.5
            else -> 11.0
        }
        val consumoUsado = consumoHistorico ?: consumoPadrao
        val kmEstimadoRodado = (litrosEstimados * consumoUsado).roundToInt().coerceAtLeast(1)
        textoKmSugeridoDetalhe =
            "Estimativa: ${String.format(Locale("pt", "BR"), "%.1f", litrosEstimados)} L x " +
                "${String.format(Locale("pt", "BR"), "%.1f", consumoUsado)} km/L"
        return kmAtualBase + kmEstimadoRodado
    }

    fun salvarAvisos(kmAtualBaseForcado: Int? = null): Boolean {
        fun validarLimiteAbastecimentos(novosAbastecimentos: Int): Boolean {
            if (novosAbastecimentos <= 0 || fuelRecordLimit == Int.MAX_VALUE) return true
            val totalAtual = runCatching {
                BancoDeDados.carregarAbastecimentos(context).size
            }.getOrDefault(activeFuelRecordCount)
                .coerceAtLeast(activeFuelRecordCount)
            if (totalAtual + novosAbastecimentos <= fuelRecordLimit) return true
            onRequestPremium("fuel_limit")
            return false
        }

        fun registrarAbastecimentosNoHistorico(
            valores: List<Double>,
            itensRegistrados: List<ItemAbastecimento> = emptyList()
        ): Boolean {
            val valoresValidos = valores.filter { it > 0.0 }
            if (valoresValidos.isEmpty()) return true
            if (!validarLimiteAbastecimentos(valoresValidos.size)) return false
            scope.launch(Dispatchers.IO) {
                val existentes = BancoDeDados.carregarAbastecimentos(context)
                val precoReferencia = existentes
                    .asReversed()
                    .firstOrNull { it.carroId == carroAtual.id && it.precoLitro > 0.0 }
                    ?.precoLitro
                    ?: 5.60
                val novosRegistros = valoresValidos.map { total ->
                    val litrosEstimados = (total / precoReferencia).coerceAtLeast(0.01)
                    Abastecimento(
                        carroId = carroAtual.id,
                        data = data,
                        precoLitro = precoReferencia,
                        valorPago = total,
                        litros = litrosEstimados,
                        itens = itensRegistrados
                    )
                }
                val atualizados = existentes + novosRegistros
                BancoDeDados.salvarAbastecimentos(context, atualizados)
                AdminUsersSync.syncFuelSnapshot(atualizados)
                withContext(Dispatchers.Main) {
                    onFuelRecordsSaved(atualizados, novosRegistros.size)
                }
            }
            return true
        }

        val kmAtualBase = kmAtualBaseForcado ?: (kmBase.toIntOrNull() ?: 0)
        if (kmAtualBase > carroAtual.kmAtual) onUpdateKmCarro(kmAtualBase)
        val recorrenciaConfig = recorrenciaSelecionadaConfig()
        val dataAvisoStr = when {
            isRegistroServico -> data
            !isRegistroServico && recorrenciaConfig != null -> proximaDataRecorrenteFutura(
                dataInicial = dataAviso,
                hora = horaNotificacao,
                recorrenciaConfig = recorrenciaConfig
            )
            else -> dataAviso
        }
        if (dataAviso != dataAvisoStr) {
            dataAviso = dataAvisoStr
        }
        fun validarLimiteAvisos(novosAvisosAtivos: Int): Boolean {
            if (novosAvisosAtivos <= 0 || reminderLimit == Int.MAX_VALUE) return true
            val totalCadastrosTecnicos = activeReminderCount + activeRecordCount
            if (isRegistroServico) {
                if (totalCadastrosTecnicos + novosAvisosAtivos <= reminderLimit) return true
                onRequestPremium("record_limit")
                return false
            }
            if (totalCadastrosTecnicos + novosAvisosAtivos <= reminderLimit) return true
            onRequestPremium("reminder_limit")
            return false
        }
        Log.i(
            "ReminderRepeat",
            "acao=salvar_aviso_listaModo=$isModoLista registroServico=$isRegistroServico recorrencia=$recorrenciaConfig frequenciaKey=$frequenciaLembreteKey repetir=$repetirAteDesativar"
        )
        if (isModoLista) {
            val novosLembretes = listaItensDetectados.flatMap { item ->
                val tipoItem = itemTipoOverrides[item.id] ?: tipoSelecionado
                val rep = if (qrModoSeparado) 1 else maxOf(1, item.quantidade)
                val kmFuturo = kmOuEstadoPorTipo(tipoItem, kmAtualBase)
                val horaItem = itemHoraAvisoOverrides[item.id] ?: horaNotificacao
                val dataItem = if (!isRegistroServico && recorrenciaConfig != null) {
                    val dataBaseItem = itemDataAvisoOverrides[item.id] ?: dataAvisoStr
                    proximaDataRecorrenteFutura(
                        dataInicial = dataBaseItem,
                        hora = horaItem,
                        recorrenciaConfig = recorrenciaConfig
                    )
                } else {
                    itemDataAvisoOverrides[item.id] ?: dataAvisoStr
                }
                val valorItem = itemValorOverrides[item.id]?.toDoubleOrNull() ?: item.valor
                (1..rep).map { indice ->
                    val tituloCustom = itemTituloOverrides[item.id]?.trim().orEmpty()
                    val tituloBase = when {
                        tituloCustom.isNotBlank() -> tituloCustom
                        tituloAviso.isBlank() -> item.nome
                        else -> "${tituloAviso.trim()} - ${item.nome}"
                    }
                    val tituloFormatado = if (rep > 1) "$tituloBase (${indice}/$rep)" else tituloBase
                    Lembrete(
                        titulo = tituloFormatado,
                        peca = item.nome,
                        dataLimite = dataItem,
                        kmLimite = kmFuturo,
                        tipo = tipoItem,
                        valor = valorItem,
                        carroId = "",
                        contatoId = contatoSelecionado?.id,
                        fotoPath = fotoCaminho,
                        horaAviso = horaItem,
                        estabelecimentoNome = qrNomeEstabelecimento,
                        estabelecimentoEndereco = qrEnderecoEstabelecimento
                    )
                }
            }
            val lembretesSemPosto = novosLembretes.filter { it.tipo != TipoManutencao.ABASTECIMENTO }
            if (!validarLimiteAvisos(lembretesSemPosto.size)) return false
            val valoresAbastecimento = novosLembretes
                .filter { it.tipo == TipoManutencao.ABASTECIMENTO }
                .map { it.valor }
            if (!isRegistroServico) {
                lembretesSemPosto.forEach { lembrete ->
                    if (tipoPermiteFrequencia(lembrete.tipo)) {
                        if (recorrenciaConfig != null) {
                            Log.i(
                                "ReminderRepeat",
                                "acao=salvar_lista_com_recorrencia id=${lembrete.id} unit=${recorrenciaConfig.first} interval=${recorrenciaConfig.second}"
                            )
                            NotificacaoHelper.salvarRecorrencia(
                                context = appContext,
                                lembreteId = lembrete.id,
                                unit = recorrenciaConfig.first,
                                interval = recorrenciaConfig.second
                            )
                        } else {
                            Log.i("ReminderRepeat", "acao=salvar_lista_sem_recorrencia id=${lembrete.id}")
                            NotificacaoHelper.removerRecorrencia(appContext, lembrete.id)
                        }
                    } else {
                        Log.i("ReminderRepeat", "acao=salvar_lista_tipo_sem_suporte id=${lembrete.id} tipo=${lembrete.tipo.name}")
                        NotificacaoHelper.removerRecorrencia(appContext, lembrete.id)
                    }
                }
            }
            if (!registrarAbastecimentosNoHistorico(valoresAbastecimento)) return false
            if (lembretesSemPosto.isNotEmpty()) {
                val resultado = if (isRegistroServico) {
                    lembretesSemPosto.map { marcarLembreteComoRealizado(it) }
                } else {
                    lembretesSemPosto
                }
                onMultiConfirm(resultado)
            } else if (valoresAbastecimento.any { it > 0.0 }) {
                                                    Toast.makeText(context, "Salvo no historico.", Toast.LENGTH_SHORT).show()
            }
        } else if (tituloAviso.isNotBlank() && descricao.isNotBlank()) {
            val tituloLembrete = localServicoInput.ifBlank { qrNomeEstabelecimento.ifBlank { descricao } }
            val quantidadeManual = if (avisoSemQuantidade) 1 else quantidadeManualInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val descricaoComQuantidade = if (!avisoSemQuantidade && qrQuantidadeTotalItens == null && quantidadeManual > 1) {
                "${descricao.trim()} (Qtd: $quantidadeManual)"
            } else {
                descricao.trim()
            }
            val valorDoAviso = if (avisoSemTotal) 0.0 else valorInput.replace(",", ".").toDoubleOrNull() ?: 0.0
            val novoLembrete = Lembrete(
                titulo = tituloAviso.trim().ifBlank { tituloLembrete },
                peca = descricaoComQuantidade,
                dataLimite = dataAvisoStr,
                kmLimite = kmOuEstadoPorTipo(tipoSelecionado, kmAtualBase),
                tipo = tipoSelecionado,
                valor = valorDoAviso,
                carroId = "",
                contatoId = contatoSelecionado?.id,
                fotoPath = fotoCaminho,
                horaAviso = horaNotificacao,
                estabelecimentoNome = qrNomeEstabelecimento,
                estabelecimentoEndereco = qrEnderecoEstabelecimento
            )
            if (novoLembrete.tipo == TipoManutencao.ABASTECIMENTO) {
                val itensAbastecimento = listaItensDetectados.mapNotNull { item ->
                    val valorItem = itemValorOverrides[item.id]
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()
                        ?: item.valor
                    if (valorItem <= 0.0) return@mapNotNull null
                    ItemAbastecimento(nome = item.nome, valor = valorItem)
                }
                if (!registrarAbastecimentosNoHistorico(
                    valores = listOf(novoLembrete.valor),
                    itensRegistrados = itensAbastecimento
                )) return false
                                            Toast.makeText(context, "Salvo no historico.", Toast.LENGTH_SHORT).show()
            } else if (isRegistroServico) {
                if (!validarLimiteAvisos(1)) return false
                onConfirm(marcarLembreteComoRealizado(novoLembrete))
            } else {
                if (!validarLimiteAvisos(1)) return false
                if (tipoPermiteFrequencia(novoLembrete.tipo)) {
                    if (recorrenciaConfig != null) {
                        Log.i(
                            "ReminderRepeat",
                            "acao=salvar_unico_com_recorrencia id=${novoLembrete.id} unit=${recorrenciaConfig.first} interval=${recorrenciaConfig.second}"
                        )
                        NotificacaoHelper.salvarRecorrencia(
                            context = appContext,
                            lembreteId = novoLembrete.id,
                            unit = recorrenciaConfig.first,
                            interval = recorrenciaConfig.second
                        )
                    } else {
                        Log.i("ReminderRepeat", "acao=salvar_unico_sem_recorrencia id=${novoLembrete.id}")
                        NotificacaoHelper.removerRecorrencia(appContext, novoLembrete.id)
                    }
                } else {
                    Log.i("ReminderRepeat", "acao=salvar_unico_tipo_sem_suporte id=${novoLembrete.id} tipo=${novoLembrete.tipo.name}")
                    NotificacaoHelper.removerRecorrencia(appContext, novoLembrete.id)
                }
                onConfirm(novoLembrete)
            }
        }
        return true
    }

    fun tentarSalvarAvisos() {
        if (salvarAvisos()) {
            onDismiss()
        }
    }

    if (showScannerGuide) {
        Scaffold(containerColor = pageBackground) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (scannerGuideImageResId != 0) {
                    Image(
                        painter = painterResource(id = scannerGuideImageResId),
                        contentDescription = "Exemplo de nota para escaneamento",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp, max = 620.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceCardColor),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Condições para escaneamento",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = scheme.primary)
                            Text(
                                text = "Ambiente claro para melhorar a leitura.",
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = scheme.primary)
                            Text(
                                text = "QR Code nítido, sem dobras e dentro do quadrado de leitura.",
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = scheme.primary)
                            Text(
                                text = "Aponte o quadrado apenas para o QR Code da nota.",
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        showScannerGuide = false
                        tentarAbrirCamera(exibirGuia = false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ler nota com IA pelo QR Code", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    if (showCamera) {
        CameraCapturaDialog(onDismiss = { showCamera = false }, onFotoCapturada = { resultado ->
            if (scannerLimit != Int.MAX_VALUE) {
                AppPreferences.incrementOcrCount(context)
            }
            fotoCaminho = resultado.arquivoFoto.absolutePath
            usarCadastroManualPosScan = false
            val qrUrl = resultado.qrCodeUrl?.trim()
            Log.i(QR_PARSER_TAG, "Camera retorno => qrUrl=$qrUrl foto=${resultado.arquivoFoto.name}")
            if (!qrUrl.isNullOrBlank()) {
                val notaInfo = resultado.notaQrInfo
                if (notaInfo != null && notaQrIndicaBloqueioSp(notaInfo)) {
                    val urlValidacao = montarUrlValidacaoHumanaSp(qrUrl)
                    if (!urlValidacao.isNullOrBlank()) {
                        qrNomeEstabelecimento = notaInfo.nomeEstabelecimento.orEmpty()
                        qrEnderecoEstabelecimento = notaInfo.enderecoEstabelecimento.orEmpty()
                        aplicarTituloPeloScan(qrNomeEstabelecimento)
                        localServicoInput = montarLocalNota(
                            estabelecimento = qrNomeEstabelecimento,
                            endereco = qrEnderecoEstabelecimento
                        )
                        qrUrlValidacaoSpPendente = urlValidacao
                        resultadoQrPendente = resultado
                        showCamera = false
                        showValidacaoSpWebView = true
                        webViewSpCarregando = true
                        Toast.makeText(
                            context,
                            "Abrindo validacao da SEFAZ-SP dentro do app.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.w(
                            QR_PARSER_TAG,
                            "Bloqueio SP detectado na camera. Direcionando direto para WebView."
                        )
                        return@CameraCapturaDialog
                    }
                }

                isModoLista = false
                textosDetectados = emptyList()
                showTextosDialog = false
                showMarcaDialog = false
                showCamera = false
                if (notaInfo == null) {
                    val sugestaoOcr = resultado.sugestoesProduto
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() }
                    val sugestaoOcrLimpa = sugestaoOcr?.let { limparTextoProdutosRemovendoTotal(it) }?.takeIf { it.isNotBlank() }
                    limparDadosFinanceirosNotaScan()
                    qrNomeEstabelecimento = ""
                    qrEnderecoEstabelecimento = ""
                    localServicoInput = ""
                    qrPossuiItensSeparaveis = false
                    qrModoSeparado = false
                    descricaoQrConsolidada = ""
                    itemDataAvisoOverrides = emptyMap()
                    itemHoraAvisoOverrides = emptyMap()
                    itemValorOverrides = emptyMap()
                    itemTituloOverrides = emptyMap()
                    itemTipoOverrides = emptyMap()
                    itemCategoriaMenuAbertoId = null
                    descricao = sugestaoOcrLimpa ?: "Nota fiscal lida (itens indisponiveis)"
                    if (!sugestaoOcrLimpa.isNullOrBlank()) {
                        tipoSelecionado = detectarTipoPeloTexto(sugestaoOcrLimpa)
                    }
                    Log.i(
                        QR_PARSER_TAG,
                        "Bind UI QR => notaInfo=null sugestaoOcr=${!sugestaoOcr.isNullOrBlank()} descricaoFinal=$descricao"
                    )
                } else {
                    val valorExtraido = notaInfo.valorFinalComDesconto ?: notaInfo.valorTotal ?: notaInfo.valorBruto
                    val dataExtraida = notaInfo.dataCompra

                    aplicarDadosFinanceirosNotaScan(notaInfo)
                    if (!dataExtraida.isNullOrBlank()) {
                        data = dataExtraida
                        dataAviso = dataExtraida
                        avisoPersonalizado = true
                    }
                    qrNomeEstabelecimento = notaInfo.nomeEstabelecimento.orEmpty()
                    qrEnderecoEstabelecimento = notaInfo.enderecoEstabelecimento.orEmpty()
                    aplicarTituloPeloScan(qrNomeEstabelecimento)
                    val descricaoExtraida = notaInfo.descricaoItens?.trim()
                    val descricaoExtraidaSemTotal = descricaoExtraida?.let { limparTextoProdutosRemovendoTotal(it) }
                    val itensQr = extrairItensDaDescricaoQr(descricaoExtraida)
                    qrPossuiItensSeparaveis = itensQr.size > 1
                    qrModoSeparado = false
                    if (itensQr.isNotEmpty()) {
                        listaItensDetectados = itensQr
                        if (valorExtraido == null) {
                            val totalItens = itensQr.sumOf { it.valor }
                            if (totalItens > 0.0) {
                                valorInput = String.format(Locale.US, "%.2f", totalItens)
                            }
                        }
                        itemDataAvisoOverrides = emptyMap()
                        itemHoraAvisoOverrides = emptyMap()
                        itemValorOverrides = emptyMap()
                        itemTituloOverrides = emptyMap()
                        itemTipoOverrides = itensQr.associate { item -> item.id to item.tipo }
                    } else {
                        itemDataAvisoOverrides = emptyMap()
                        itemHoraAvisoOverrides = emptyMap()
                        itemValorOverrides = emptyMap()
                        itemTituloOverrides = emptyMap()
                        itemTipoOverrides = emptyMap()
                    }
                    itemCategoriaMenuAbertoId = null
                    localServicoInput = montarLocalNota(
                        estabelecimento = qrNomeEstabelecimento,
                        endereco = qrEnderecoEstabelecimento
                    )
                    descricaoQrConsolidada = montarDescricaoItensNota(
                        total = notaInfo.valorBruto ?: notaInfo.valorTotal,
                        itens = descricaoExtraidaSemTotal,
                        desconto = notaInfo.valorDesconto,
                        valorFinal = notaInfo.valorFinalComDesconto ?: notaInfo.valorTotal,
                        quantidadeTotalItens = notaInfo.quantidadeTotalItens
                    )
                    val sugestaoOcr = resultado.sugestoesProduto
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() }
                    val sugestaoOcrLimpa = sugestaoOcr?.let { limparTextoProdutosRemovendoTotal(it) }?.takeIf { it.isNotBlank() }
                    descricao = when {
                        descricaoExtraidaSemTotal.isNullOrBlank() && !sugestaoOcrLimpa.isNullOrBlank() -> sugestaoOcrLimpa
                        else -> descricaoQrConsolidada
                    }
                    isModoLista = false

                    Log.i(
                        QR_PARSER_TAG,
                        "Bind UI QR => notaInfo=ok estabelecimento=$qrNomeEstabelecimento endereco=$qrEnderecoEstabelecimento descricao=$descricao valorInput=$valorInput data=$data dataAviso=$dataAviso itens=${itensQr.size} modoLista=$isModoLista"
                    )
                }
                return@CameraCapturaDialog
            }
            Log.w(QR_PARSER_TAG, "QR nao detectado na captura. Seguindo fluxo de fallback da foto.")
            limparDadosFinanceirosNotaScan()
            qrNomeEstabelecimento = ""
            qrEnderecoEstabelecimento = ""
            localServicoInput = ""
            qrPossuiItensSeparaveis = false
            qrModoSeparado = false
            descricaoQrConsolidada = ""
            itemDataAvisoOverrides = emptyMap()
            itemHoraAvisoOverrides = emptyMap()
            itemValorOverrides = emptyMap()
            itemTituloOverrides = emptyMap()
            itemTipoOverrides = emptyMap()
            itemCategoriaMenuAbertoId = null
            textosDetectados = filtrarTextosDetectados(resultado.linhasReconhecidas)
            textoSelecionadoDialog = null
            showMarcaDialog = false
            produtoSelecionadoDialog = null
            marcaSelecionadaDialog = null
            if (resultado.itensEncontrados.isNotEmpty()) {
                listaItensDetectados = resultado.itensEncontrados
                qrPossuiItensSeparaveis = resultado.itensEncontrados.size > 1
                itemTipoOverrides = resultado.itensEncontrados.associate { item -> item.id to item.tipo }
                val totalItens = resultado.itensEncontrados.sumOf { it.valor }
                if (totalItens > 0.0) {
                    valorInput = String.format(Locale.US, "%.2f", totalItens)
                }
                isModoLista = true
            } else {
                qrPossuiItensSeparaveis = false
                isModoLista = false
                val principal = resultado.sugestoesProduto.firstOrNull()
                val principalLimpo = principal?.let { limparTextoProdutosRemovendoTotal(it) }?.takeIf { it.isNotBlank() }
                if (!principalLimpo.isNullOrBlank()) {
                    descricao = principalLimpo
                    tipoSelecionado = detectarTipoPeloTexto(principalLimpo)
                } else {
                    descricao = "Produto (Foto Anexada)"
                }
            }
            descricaoAntesDialog = descricao
            tipoAntesDialog = tipoSelecionado
            showTextosDialog = textosDetectados.isNotEmpty() && !isModoLista
            if (resultado.kmDetectado != null && resultado.kmDetectado > 0) { kmDetectadoParaConfirmar = resultado.kmDetectado; showKmConfirmDialog = true }
            showCamera = false
        })
    }

    if (showValidacaoSpWebView) {
        DisposableEffect(activity, isDark, pageBackground) {
            val window = activity?.window
            if (window == null) {
                onDispose { }
            } else {
                val oldStatusColor = window.statusBarColor
                val oldNavColor = window.navigationBarColor
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                val oldLightStatus = insetsController.isAppearanceLightStatusBars
                val oldLightNav = insetsController.isAppearanceLightNavigationBars
                val targetBarColor = pageBackground.toArgb()
                window.statusBarColor = targetBarColor
                window.navigationBarColor = targetBarColor
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
                onDispose {
                    window.statusBarColor = oldStatusColor
                    window.navigationBarColor = oldNavColor
                    insetsController.isAppearanceLightStatusBars = oldLightStatus
                    insetsController.isAppearanceLightNavigationBars = oldLightNav
                }
            }
        }
    }

    if (showValidacaoSpWebView && !qrUrlValidacaoSpPendente.isNullOrBlank()) {
        Dialog(
            onDismissRequest = {
                showValidacaoSpWebView = false
                qrUrlValidacaoSpPendente = null
                resultadoQrPendente = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = pageBackground
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .height(40.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showValidacaoSpWebView = false
                                qrUrlValidacaoSpPendente = null
                                resultadoQrPendente = null
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = tr("Voltar", "Back"),
                                tint = textPrimary
                            )
                        }
                        Text(
                            text = "Valide a SEFAZ abaixo. A leitura da nota sera automatica.",
                            color = textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 44.dp)
                        )
                    }
                    if (webViewSpCarregando) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.textZoom = 100
                                settings.userAgentString =
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                setInitialScale(55)
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String?) {
                                        view.setInitialScale(55)
                                        webViewSpCarregando = false
                                        if (url.isNullOrBlank() || reprocessandoQrSp) return
                                        val script =
                                            "Boolean(document.querySelector('#tabResult, .txtTit, .txtProd, .totalNumb, #linhaTotal'))"
                                        view.evaluateJavascript(script) { hasData ->
                                            val possuiDados = hasData?.contains("true", ignoreCase = true) == true
                                            if (possuiDados && !reprocessandoQrSp) {
                                                reprocessarNotaSpAposValidacao(url, origem = "auto-webview")
                                            }
                                    }
                                }
                                }
                                loadUrl(qrUrlValidacaoSpPendente!!)
                            }
                        },
                        update = { view ->
                            val urlAlvo = qrUrlValidacaoSpPendente
                            if (!urlAlvo.isNullOrBlank() && view.url != urlAlvo) {
                                webViewSpCarregando = true
                                view.loadUrl(urlAlvo)
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Button(
                            onClick = {
                                val path = prepararVideoTutorialLocal()
                                if (path.isNullOrBlank()) {
                                    Toast.makeText(context, "Nao foi possivel abrir o tutorial.", Toast.LENGTH_LONG).show()
                                } else {
                                    tutorialVideoPath = path
                                    showTutorialDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, neutralButtonBorder),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = neutralButtonContainer,
                                contentColor = neutralButtonContent
                            )
                        ) {
                            Text("Tutorial")
                        }
                    }
                }
            }
        }
    }

    if (showTutorialDialog && !tutorialVideoPath.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showTutorialDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = surfaceCardColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tutorial",
                        style = MaterialTheme.typography.titleMedium,
                        color = textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                setVideoPath(tutorialVideoPath)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        update = { view ->
                            val path = tutorialVideoPath
                            if (!path.isNullOrBlank() && view.duration <= 0) {
                                view.setVideoPath(path)
                                view.start()
                            }
                        }
                    )
                    Button(
                        onClick = { showTutorialDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }

    if (reprocessandoQrSp) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Processando nota") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("Reconsultando a NFC-e apos validacao na SEFAZ-SP...")
                }
            },
            confirmButton = {}
        )
    }

    if (showKmConfirmDialog) {
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showKmConfirmDialog = false },
            containerColor = modalContainer,
            title = { Text("Atualizar KM?", color = textPrimary) },
            text = {
                Text(
                    "Detectamos ${kmDetectadoParaConfirmar} km na captura.\nAtualizar o odômetro do carro?",
                    color = modalTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateKmCarro(kmDetectadoParaConfirmar)
                        kmBase = kmDetectadoParaConfirmar.toString()
                        showKmConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) { Text("Sim") }
            },
            dismissButton = {
                TextButton(onClick = { showKmConfirmDialog = false }) { Text("Não") }
            }
        )
    }

    if (showTextosDialog) {
        val itensDialogo = textosDetectados
            .distinct()
            .filter { it.isNotBlank() }
            .take(8)
        val jaSelecionou = textoSelecionadoDialog != null
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showTextosDialog = false },
            containerColor = modalContainer,
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Qual e o Produto?", color = textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showTextosDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = iconColor)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itensDialogo.isEmpty()) {
                        Text("Nenhum texto identificado na captura.", color = modalTextSecondary)
                    } else {
                        itensDialogo.forEach { texto ->
                            val isSelected = textoSelecionadoDialog == texto
                            val disabled = jaSelecionou && !isSelected
                            val cardShape = RoundedCornerShape(12.dp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (disabled) 0.35f else 1f)
                                    .clip(cardShape)
                                    .clickable(
                                        enabled = !disabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        textoSelecionadoDialog = texto
                                        descricao = texto
                                        tipoSelecionado = detectarTipoPeloTexto(texto)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) modalOptionSelectedContainer else modalOptionContainer
                                ),
                                shape = cardShape,
                                border = if (isSelected) BorderStroke(1.dp, modalOptionBorder) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        texto,
                                        color = if (isSelected) textPrimary else modalTextSecondary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTextosDialog = false
                        produtoSelecionadoDialog = textoSelecionadoDialog
                        marcaSelecionadaDialog = null
                        textoSelecionadoDialog = null
                        showMarcaDialog = true
                    },
                    enabled = textoSelecionadoDialog != null,
                    border = BorderStroke(1.dp, neutralButtonBorder),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neutralButtonContainer,
                        contentColor = neutralButtonContent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text(tr("Proximo", "Next"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {}
        )
    }

    if (showMarcaDialog) {
        val produtoSelecionado = produtoSelecionadoDialog
        val itensDialogo = textosDetectados
            .distinct()
            .filter { it.isNotBlank() }
            .filter { it != produtoSelecionado }
            .take(8)
        val jaSelecionou = marcaSelecionadaDialog != null
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showMarcaDialog = false },
            containerColor = modalContainer,
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Qual e a Marca do Produto?", color = textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showMarcaDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = iconColor)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itensDialogo.isEmpty()) {
                        Text("Nenhuma marca diferente foi identificada.", color = modalTextSecondary)
                    } else {
                        itensDialogo.forEach { texto ->
                            val isSelected = marcaSelecionadaDialog == texto
                            val disabled = jaSelecionou && !isSelected
                            val cardShape = RoundedCornerShape(12.dp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (disabled) 0.35f else 1f)
                                    .clip(cardShape)
                                    .clickable(
                                        enabled = !disabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        marcaSelecionadaDialog = texto
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) modalOptionSelectedContainer else modalOptionContainer
                                ),
                                shape = cardShape,
                                border = if (isSelected) BorderStroke(1.dp, modalOptionBorder) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        texto,
                                        color = if (isSelected) textPrimary else modalTextSecondary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val produto = produtoSelecionadoDialog
                        val marca = marcaSelecionadaDialog
                        if (!produto.isNullOrBlank() && !marca.isNullOrBlank()) {
                            descricao = "$produto - $marca"
                        }
                        showMarcaDialog = false
                        produtoSelecionadoDialog = null
                        marcaSelecionadaDialog = null
                    },
                    enabled = marcaSelecionadaDialog != null,
                    border = BorderStroke(1.dp, neutralButtonBorder),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neutralButtonContainer,
                        contentColor = neutralButtonContent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Concluir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {}
        )
    }

    val valorTotalManual = valorInput.replace(",", ".").toDoubleOrNull()
    val valorTotalValido = valorTotalManual != null && valorTotalManual > 0.0
    val quantidadeManualValida = isFluxoPosto ||
        avisoSemQuantidade ||
        (quantidadeManualInput.toIntOrNull()?.let { it > 0 } == true)
    val podeAvancarEtapa1 = if (isModoLista && listaItensDetectados.isNotEmpty()) {
        true
    } else {
        tituloAviso.isNotBlank() &&
            descricao.isNotBlank() &&
            valorTotalValido &&
            quantidadeManualValida
    }

    if (showKmSugeridoDialog) {
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showKmSugeridoDialog = false },
            containerColor = modalContainer,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(modalPrimaryAction.copy(alpha = 0.15f))
                            .border(1.dp, modalPrimaryAction.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = modalPrimaryAction,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        "KM sugerido no abastecimento",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Novo KM sugerido",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        "${kmSugeridoParaConfirmar ?: "-"} km",
                        color = textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Atual KM: ${kmBase.ifBlank { "Nao informado" }} km",
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                            if (textoKmSugeridoDetalhe.isNotBlank()) {
                                Text(
                                    textoKmSugeridoDetalhe,
                                    color = textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val novoKm = kmSugeridoParaConfirmar
                        if (novoKm != null) {
                            kmBase = novoKm.toString()
                            if (salvarAvisos(kmAtualBaseForcado = novoKm)) {
                                showKmSugeridoDialog = false
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) { Text("Usar KM sugerido") }
            },
            dismissButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showKmSugeridoDialog = false
                            etapaAtual = 2
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Editar KM manualmente", color = textPrimary) }
                    OutlinedButton(
                        onClick = {
                            if (salvarAvisos()) {
                                showKmSugeridoDialog = false
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) { Text("Manter KM atual", color = textPrimary) }
                }
            }
        )
    }
    val accentBlue = Color(0xFF3B82F6)
    val quantidadeLembretesConfigurados = if (qrModoSeparado && listaItensDetectados.isNotEmpty()) {
        listaItensDetectados.size
    } else {
        1
    }
    val deveExibirEtapaModoCriacao = listaItensDetectados.size > 1
    fun dataItemValida(dataTexto: String): Boolean = runCatching {
        LocalDate.parse(dataTexto, dataFormatter)
    }.isSuccess
    fun parseDataOrNull(dataTexto: String): LocalDate? = runCatching {
        LocalDate.parse(dataTexto, dataFormatter)
    }.getOrNull()
    val totalItensModoSeparado = listaItensDetectados.size
    val itensCompletosModoSeparado = if (qrModoSeparado && totalItensModoSeparado > 0) {
        listaItensDetectados.count { item ->
            val tipoItem = itemTipoOverrides[item.id] ?: tipoSelecionado
            val dataItem = itemDataAvisoOverrides[item.id] ?: dataAviso
            val tituloItemPadrao = if (tituloAviso.isBlank()) item.nome else "${tituloAviso.trim()} - ${item.nome}"
            val tituloItemAtual = itemTituloOverrides[item.id] ?: tituloItemPadrao
            tituloItemAtual.trim().isNotBlank() &&
                dataItemValida(dataItem) &&
                (tipoItem in categoriasDisponiveis)
        }
    } else {
        0
    }
    val podeAvancarEtapa3 = !qrModoSeparado || totalItensModoSeparado == 0 || itensCompletosModoSeparado == totalItensModoSeparado
    val dataServicoSelecionada = parseDataOrNull(data)
    val dataAvisoSelecionada = parseDataOrNull(dataAviso)
    val dataAvisoMesmoDiaDoServico = !isRegistroServico &&
        dataServicoSelecionada != null &&
        dataAvisoSelecionada != null &&
        dataAvisoSelecionada.isEqual(dataServicoSelecionada)

    val podeAvancarEtapa2 = if (isRegistroServico) {
        dataItemValida(data)
    } else {
        dataItemValida(dataAviso) && !dataAvisoMesmoDiaDoServico
    }
    val textoBotaoEtapa1 = tr("Avançar", "Continue")
    val textoBotaoSalvar = if (isFluxoPosto) {
        tr("Cadastrar abastecimento", "Register fuel")
    } else if (isRegistroServico) {
        if (qrModoSeparado && listaItensDetectados.isNotEmpty()) {
            tr("Salvar $quantidadeLembretesConfigurados serviços", "Save $quantidadeLembretesConfigurados services")
        } else {
            tr("Salvar serviço", "Save service")
        }
    } else if (qrModoSeparado && listaItensDetectados.isNotEmpty()) {
        tr("Cadastrar $quantidadeLembretesConfigurados lembretes", "Create $quantidadeLembretesConfigurados reminders")
    } else {
        tr("Cadastrar lembrete", "Create reminder")
    }
    fun etapaAnteriorAtual(etapa: Int): Int = when {
        !deveExibirEtapaModoCriacao && etapa == 4 -> 2
        isFluxoPosto && etapa == 4 -> 1
        isRegistroServico && etapa == 4 -> 2
        etapa > 1 -> etapa - 1
        else -> 1
    }
    val voltarUmaEtapaOuFechar = {
        if (etapaAtual > 1) {
            etapaAtual = etapaAnteriorAtual(etapaAtual)
        } else {
            onBackToTipoAviso()
        }
    }

    BackHandler(onBack = voltarUmaEtapaOuFechar)

    Scaffold(
        containerColor = pageBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBackground)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (etapaAtual) {
                    1 -> Unit
                    2 -> {
                        Button(
                            onClick = {
                                etapaAtual = when {
                                    isRegistroServico -> 4
                                    deveExibirEtapaModoCriacao -> 3
                                    else -> 4
                                }
                            },
                            enabled = podeAvancarEtapa2,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = nextActionBlue,
                                contentColor = Color.White,
                                disabledContainerColor = nextActionBlue.copy(alpha = 0.42f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).offset(y = (-10).dp)
                        ) { Text(tr("Próximo", "Next"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    3 -> {
                        Button(
                            onClick = { etapaAtual = 4 },
                            enabled = podeAvancarEtapa3,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = nextActionBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).offset(y = (-10).dp)
                        ) { Text(tr("Próximo", "Next"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    4 -> {
                        Button(
                            onClick = {
                                tentarSalvarAvisos()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = nextActionBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).offset(y = (-10).dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(textoBotaoSalvar, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(etapasScrollState)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = voltarUmaEtapaOuFechar) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = iconColor)
                    }
                    // Botão de ajuda — visível apenas na etapa 1
                    if (etapaAtual == 1) {
                        IconButton(
                            onClick = { showGuiaManutencao = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(accentBlue.copy(alpha = 0.13f), CircleShape)
                                    .border(1.5.dp, accentBlue.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = tr("Guia de manutenção", "Maintenance guide"),
                                    tint = accentBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
                Column(
                    modifier = Modifier.padding(top = 0.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                when (etapaAtual) {
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentBlue.copy(alpha = 0.18f))
                                    .border(
                                        width = 1.dp,
                                        color = accentBlue.copy(alpha = 0.28f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = accentBlue, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                when {
                                    isRegistroServico -> tr("Dados do serviço", "Service data")
                                    fluxoCadastro == FluxoCadastroAviso.CRIAR_LEMBRETE -> tr("Dados do lembrete", "Reminder data")
                                    else -> tr("Dados do aviso", "Reminder data")
                                },
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        if (categoriaPermiteEscanearNota(tipoSelecionado, carroAtual.tipoVeiculo)) {
                            Button(
                                onClick = {
                                    usarCadastroManualPosScan = false
                                    tentarAbrirCamera()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    if (fotoCaminho != null) Icons.Default.Check else Icons.Default.CameraAlt,
                                    null,
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (fotoCaminho != null) {
                                        "Ler nota novamente com IA"
                                    } else {
                                        "Ler nota com IA pelo QR Code"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (fotoCaminho != null) {
                                OutlinedButton(
                                    onClick = {
                                        usarCadastroManualPosScan = true
                                        isModoLista = false
                                        qrModoSeparado = false
                                        fotoCaminho = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, accentBlue.copy(alpha = if (isDark) 0.85f else 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = accentBlue.copy(alpha = if (isDark) 0.18f else 0.10f),
                                        contentColor = accentBlue
                                    )
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tr("Preencher manualmente", "Fill manually"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = tituloAviso,
                            onValueChange = { tituloAviso = it },
                            label = {
                                Text(
                                    when {
                                        isRegistroServico -> tr("Título do serviço *", "Service title *")
                                        else -> tr("Título do aviso *", "Reminder title *")
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = TextFieldValue(
                                text = descricao,
                                selection = TextRange(descricao.length)
                            ),
                            onValueChange = { descricao = it.text },
                            label = {
                                Text(
                                    when {
                                        isRegistroServico -> tr("Descrição do serviço", "Service description")
                                        else -> tr("Descrição do aviso", "Reminder description")
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .animateContentSize(),
                            maxLines = 12,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (!qrModoSeparado) {
                            val valorBrutoExibicao = qrValorTotalBruto
                            val valorDescontoExibicao = qrValorDesconto
                            val valorFinalExibicao = qrValorFinalComDesconto
                                ?: valorInput.replace(",", ".").toDoubleOrNull()
                            val quantidadeTotalExtraida = qrQuantidadeTotalItens
                                ?: listaItensDetectados.takeIf { it.isNotEmpty() }?.sumOf { it.quantidade.coerceAtLeast(1) }
                            val formaPagamentoExibicao = qrFormaPagamento?.trim().orEmpty()
                            val veioDeEscaneamento = !usarCadastroManualPosScan && (
                                fotoCaminho != null ||
                                    qrNomeEstabelecimento.isNotBlank() ||
                                    listaItensDetectados.isNotEmpty() ||
                                    qrQuantidadeTotalItens != null ||
                                    qrValorTotalBruto != null ||
                                    qrValorDesconto != null ||
                                    qrValorFinalComDesconto != null ||
                                    formaPagamentoExibicao.isNotBlank()
                                )
                            val mostrarResumoNota = (
                                veioDeEscaneamento && (
                                    valorBrutoExibicao != null ||
                                    (valorDescontoExibicao ?: 0.0) > 0.0 ||
                                    valorFinalExibicao != null ||
                                    formaPagamentoExibicao.isNotBlank() ||
                                    quantidadeTotalExtraida != null
                                )
                            )
                            if (!veioDeEscaneamento) {
                            OutlinedTextField(
                                value = valorInput,
                                onValueChange = { novo ->
                                    valorInput = formatarValorMonetarioCampo(novo)
                                },
                                label = { Text("Total *") },
                                prefix = { Text("R$") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (!isFluxoPosto) {
                                val textoTipoCadastro = if (isRegistroServico) {
                                    tr("registro", "record")
                                } else {
                                    tr("aviso", "reminder")
                                }
                                OutlinedTextField(
                                    value = if (avisoSemQuantidade) "" else quantidadeManualInput,
                                    onValueChange = { quantidadeManualInput = it.filter(Char::isDigit).take(3) },
                                    enabled = !avisoSemQuantidade,
                                    label = { Text(if (avisoSemQuantidade) tr("Quantidade", "Quantity") else tr("Quantidade *", "Quantity *")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            avisoSemQuantidade = !avisoSemQuantidade
                                            if (avisoSemQuantidade) quantidadeManualInput = ""
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = avisoSemQuantidade,
                                        onCheckedChange = { marcado ->
                                            avisoSemQuantidade = marcado
                                            if (marcado) quantidadeManualInput = ""
                                        }
                                    )
                                    Text(
                                        text = tr(
                                            "Este $textoTipoCadastro não possui quantidade",
                                            "This $textoTipoCadastro has no quantity"
                                        ),
                                        color = textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            }
                            if (mostrarResumoNota) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (quantidadeTotalExtraida != null && quantidadeTotalExtraida > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(tr("Quantidade total de itens", "Total item quantity"), color = textSecondary, fontSize = 12.sp)
                                                Text(
                                                    text = quantidadeTotalExtraida.toString(),
                                                    color = textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(tr("Valor total", "Total amount"), color = textSecondary, fontSize = 12.sp)
                                            Text(
                                                text = valorBrutoExibicao?.let { currencyFormatter.format(it) } ?: "-",
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(tr("Desconto", "Discount"), color = textSecondary, fontSize = 12.sp)
                                            val descontoExibido = valorDescontoExibicao?.takeIf { it > 0.0 } ?: 0.0
                                            Text(
                                                text = currencyFormatter.format(descontoExibido),
                                                color = if (descontoExibido > 0.0) Color(0xFFDC2626) else textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(tr("Valor final", "Final amount"), color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = valorFinalExibicao?.let { currencyFormatter.format(it) } ?: "-",
                                                color = Color(0xFF16A34A),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (formaPagamentoExibicao.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(tr("Forma de pagamento", "Payment method"), color = textSecondary, fontSize = 12.sp)
                                                Text(
                                                    text = formaPagamentoExibicao,
                                                    color = textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                etapaAtual = when {
                                    isFluxoPosto -> 4
                                    isRegistroServico -> 2
                                    else -> 2
                                }
                            },
                            enabled = podeAvancarEtapa1,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = nextActionBlue,
                                contentColor = Color.White,
                                disabledContainerColor = nextActionBlue.copy(alpha = 0.42f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(textoBotaoEtapa1, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    2 -> {
                        val exigeEstadoUf = tipoSelecionado == TipoManutencao.IPVA || tipoSelecionado == TipoManutencao.LICENCIAMENTO
                        var menuUfExpanded by remember { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-14).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentBlue.copy(alpha = 0.18f))
                                    .border(
                                        width = 1.dp,
                                        color = accentBlue.copy(alpha = 0.28f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Speed, contentDescription = null, tint = accentBlue, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (exigeEstadoUf) {
                                    tr("Estado e data", "State and date")
                                } else if (isRegistroServico) {
                                    tr("KM e data do serviço", "Mileage and service date")
                                } else {
                                    tr("KM e data do lembrete", "Mileage and reminder date")
                                },
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (exigeEstadoUf) {
                                ExposedDropdownMenuBox(
                                    expanded = menuUfExpanded,
                                    onExpandedChange = { menuUfExpanded = !menuUfExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = estadoUfSelecionado,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(tr("Estado (UF)", "State (UF)")) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuUfExpanded)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = menuUfExpanded,
                                        onDismissRequest = { menuUfExpanded = false }
                                    ) {
                                        ESTADOS_UF.forEach { uf ->
                                            DropdownMenuItem(
                                                text = { Text(uf) },
                                                onClick = {
                                                    estadoUfSelecionado = uf
                                                    menuUfExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = formatarKmCampo(kmBase),
                                    onValueChange = { kmBase = it.filter(Char::isDigit) },
                                    label = { Text(tr("KM Atual", "Current mileage")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    trailingIcon = {
                                        Icon(
                                            Icons.Rounded.Speed,
                                            contentDescription = null
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            OutlinedTextField(
                                value = data,
                                onValueChange = {},
                                label = { Text(tr("Data do servico", "Service date")) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        abrirDatePicker(data) {
                                            data = it
                                            // Ao editar manualmente a data do servico, sincroniza o lembrete
                                            // para evitar que o calculo automatico empurre para meses a frente.
                                            dataAviso = it
                                            avisoPersonalizado = true
                                        }
                                    }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        if (!isRegistroServico) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = dataAviso,
                                    onValueChange = {},
                                    label = { Text(if (qrModoSeparado) tr("Data padrão", "Default date") else tr("Data do lembrete", "Reminder date")) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            abrirDatePicker(dataAviso) {
                                                dataAviso = it
                                                avisoPersonalizado = true
                                            }
                                        }) {
                                            Icon(Icons.Default.Event, contentDescription = null)
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = horaNotificacao,
                                    onValueChange = {},
                                    label = { Text(tr("Hora do lembrete", "Reminder time")) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            abrirTimePicker(horaNotificacao) { selecionada ->
                                                horaNotificacao = selecionada
                                            }
                                        }) {
                                            Icon(Icons.Default.Schedule, contentDescription = null)
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            if (!dataItemValida(dataAviso)) {
                                Text(
                                    text = tr("Selecione a data do lembrete para continuar.", "Select the reminder date to continue."),
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (dataAvisoMesmoDiaDoServico) {
                                Text(
                                    text = tr(
                                        "A data do lembrete não pode ser igual à data do serviço.",
                                        "Reminder date cannot be the same as service date."
                                    ),
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            val mostrarFrequencia = if (qrModoSeparado && listaItensDetectados.isNotEmpty()) {
                                listaItensDetectados.any { item ->
                                    val tipoItem = itemTipoOverrides[item.id] ?: tipoSelecionado
                                    tipoPermiteFrequencia(tipoItem)
                                }
                            } else {
                                tipoPermiteFrequencia(tipoSelecionado)
                            }
                            if (mostrarFrequencia) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (!canUseRecurringReminders) {
                                                onRequestPremium("recurrence_premium")
                                            } else {
                                                val novoValor = !repetirAteDesativar
                                                repetirAteDesativar = novoValor
                                                if (novoValor) {
                                                    if (frequenciaLembreteKey == "NONE") {
                                                        frequenciaLembreteKey = "DAY"
                                                    }
                                                    aplicarPrimeiroAvisoAgoraSeRecorrente()
                                                } else {
                                                    frequenciaLembreteKey = "NONE"
                                                    menuFrequenciaExpanded = false
                                                }
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = repetirAteDesativar,
                                        onCheckedChange = { marcado ->
                                            if (!canUseRecurringReminders) {
                                                onRequestPremium("recurrence_premium")
                                            } else {
                                                repetirAteDesativar = marcado
                                                if (marcado) {
                                                    if (frequenciaLembreteKey == "NONE") {
                                                        frequenciaLembreteKey = "DAY"
                                                    }
                                                    aplicarPrimeiroAvisoAgoraSeRecorrente()
                                                } else {
                                                    frequenciaLembreteKey = "NONE"
                                                    menuFrequenciaExpanded = false
                                                }
                                            }
                                        }
                                    )
                                    Text(
                                        text = tr("Repetir este aviso", "Repeat this reminder"),
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!canUseRecurringReminders) {
                                        Spacer(Modifier.width(8.dp))
                                        AssistChip(
                                            onClick = { onRequestPremium("recurrence_premium") },
                                            label = { Text("Lite+") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                                if (repetirAteDesativar) {
                                    Text(
                                        text = tr(
                                            "A repetição é calculada a partir da data deste aviso.",
                                            "Repeat is calculated from this reminder date."
                                        ),
                                        color = textSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    ExposedDropdownMenuBox(
                                        expanded = menuFrequenciaExpanded,
                                        onExpandedChange = { menuFrequenciaExpanded = !menuFrequenciaExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = descricaoFrequenciaSelecionada(),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(tr("Frequência do lembrete", "Reminder frequency")) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuFrequenciaExpanded)
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = menuFrequenciaExpanded,
                                            onDismissRequest = { menuFrequenciaExpanded = false }
                                        ) {
                                            val opcoes = listOf(
                                                "DAY" to tr("Dias", "Days"),
                                                "MONTH" to tr("Meses", "Months"),
                                                "YEAR" to tr("Anos", "Years")
                                            )
                                            opcoes.forEach { (key, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        frequenciaLembreteKey = key
                                                        aplicarPrimeiroAvisoAgoraSeRecorrente()
                                                        menuFrequenciaExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (
                                        frequenciaLembreteKey == "DAY" ||
                                        frequenciaLembreteKey == "MONTH" ||
                                        frequenciaLembreteKey == "YEAR"
                                    ) {
                                        OutlinedTextField(
                                            value = intervaloAtualTexto(),
                                            onValueChange = { atualizarIntervaloAtual(it) },
                                            label = { Text(tr("Repetir a cada", "Repeat every")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            suffix = {
                                                Text(
                                                    when (frequenciaLembreteKey) {
                                                        "DAY" -> tr("dia(s)", "day(s)")
                                                        "MONTH" -> tr("mês(es)", "month(s)")
                                                        else -> tr("ano(s)", "year(s)")
                                                    }
                                                )
                                            },
                                            supportingText = {
                                                Text(
                                                    when (frequenciaLembreteKey) {
                                                        "DAY" -> tr("Informe de 1 a 31 dias.", "Enter from 1 to 31 days.")
                                                        "MONTH" -> tr("Informe de 1 a 12 meses.", "Enter from 1 to 12 months.")
                                                        else -> tr("Informe de 1 a 10 anos (12 meses = 1 ano).", "Enter from 1 to 10 years (12 months = 1 year).")
                                                    }
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-14).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentBlue.copy(alpha = 0.18f))
                                    .border(
                                        width = 1.dp,
                                        color = accentBlue.copy(alpha = 0.28f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Build, contentDescription = null, tint = accentBlue, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tr("Como criar os lembretes", "How to create reminders"),
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp
                            )
                        }
                        if (deveExibirEtapaModoCriacao) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    tr("Como deseja criar os lembretes desta nota?", "How do you want to create reminders from this receipt?"),
                                    color = textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            qrModoSeparado = false
                                            isModoLista = false
                                            descricao = descricaoQrConsolidada
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (!qrModoSeparado) accentBlue else if (isDark) Color.White else Color.Black
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (!qrModoSeparado) accentBlue else Color.Transparent,
                                            contentColor = if (!qrModoSeparado) Color.White else textPrimary
                                        )
                                    ) {
                                        Text(tr("Criar 1 lembrete", "Create 1 reminder"), fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            qrModoSeparado = true
                                            isModoLista = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (qrModoSeparado) accentBlue else if (isDark) Color.White else Color.Black
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (qrModoSeparado) accentBlue else Color.Transparent,
                                            contentColor = if (qrModoSeparado) Color.White else textPrimary
                                        )
                                    ) {
                                        Text(tr("1 por item", "1 per item"), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Text(
                                    text = tr("Esta nota será criada como 1 lembrete.", "This receipt will be created as 1 reminder."),
                                    modifier = Modifier.padding(12.dp),
                                    color = textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (deveExibirEtapaModoCriacao && !qrModoSeparado) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.18f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(accentBlue.copy(alpha = 0.14f))
                                                .border(
                                                    width = 1.dp,
                                                    color = accentBlue.copy(alpha = 0.24f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = accentBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = tituloAviso.trim().ifBlank { descricao.trim().ifBlank { tr("Aviso", "Reminder") } },
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedTextField(
                                        value = dataAviso,
                                        onValueChange = {},
                                        label = { Text(tr("Data deste aviso", "Date for this reminder")) },
                                        readOnly = true,
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Icon(Icons.Default.Event, contentDescription = null, tint = textSecondary)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = tipoManutencaoLabel(tipoSelecionado),
                                        onValueChange = {},
                                        label = { Text(tr("Categoria", "Category")) },
                                        readOnly = true,
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = {
                                            TipoIcon(
                                                tipo = tipoSelecionado,
                                                tint = textSecondary,
                                                size = 18.dp
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    tr("Categoria", "Category"),
                                                    color = textSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    tipoManutencaoLabel(tipoSelecionado),
                                                    color = textSecondary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    tr("Valor", "Amount"),
                                                    color = textSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = currencyFormatter.format(valorInput.replace(",", ".").toDoubleOrNull() ?: 0.0),
                                                    color = textSecondary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                tr("Descrição dos itens", "Items description"),
                                                color = textSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = descricaoQrConsolidada.ifBlank { descricao },
                                                color = textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = tr(
                                    "Não é possível editar essas informações aqui. Para editar, volte para a tela Dados do lembrete.",
                                    "You cannot edit this information here. To edit it, go back to Reminder data."
                                ),
                                color = textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        if (qrModoSeparado && listaItensDetectados.isNotEmpty()) {
                            val progresso = if (totalItensModoSeparado > 0) {
                                itensCompletosModoSeparado.toFloat() / totalItensModoSeparado.toFloat()
                            } else 0f
                            val existemPendencias = itensCompletosModoSeparado < totalItensModoSeparado
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            "Edite título, data e categoria de cada aviso.",
                                            "Edit title, date and category for each reminder."
                                        ),
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (existemPendencias) {
                                            tr(
                                                "$itensCompletosModoSeparado de $totalItensModoSeparado itens prontos. Complete os pendentes para avançar.",
                                                "$itensCompletosModoSeparado of $totalItensModoSeparado items ready. Complete pending ones to continue."
                                            )
                                        } else {
                                            tr(
                                                "Tudo certo: $totalItensModoSeparado de $totalItensModoSeparado itens configurados.",
                                                "All good: $totalItensModoSeparado of $totalItensModoSeparado items configured."
                                            )
                                        },
                                        color = if (existemPendencias) Color(0xFFF59E0B) else Color(0xFF16A34A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    LinearProgressIndicator(
                                        progress = { progresso.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = if (existemPendencias) Color(0xFFF59E0B) else Color(0xFF16A34A),
                                        trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                itemDataAvisoOverrides = listaItensDetectados.associate { it.id to dataAviso }
                                            },
                                            modifier = Modifier.weight(1f).height(42.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, cardBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                                        ) {
                                            Text(tr("Aplicar data padrão", "Apply default date"), fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                itemTipoOverrides = listaItensDetectados.associate { it.id to tipoSelecionado }
                                                itemCategoriaMenuAbertoId = null
                                            },
                                            modifier = Modifier.weight(1f).height(42.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, cardBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                                        ) {
                                            Text(tr("Aplicar categoria padrão", "Apply default category"), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listaItensDetectados.forEachIndexed { index, item ->
                                    val tipoItem = itemTipoOverrides[item.id] ?: tipoSelecionado
                                    val dataItem = itemDataAvisoOverrides[item.id] ?: dataAviso
                                    val tituloItemPadrao = if (tituloAviso.isBlank()) item.nome else "${tituloAviso.trim()} - ${item.nome}"
                                    val tituloItemAtual = itemTituloOverrides[item.id] ?: tituloItemPadrao
                                    val tituloValido = tituloItemAtual.trim().isNotBlank()
                                    val dataValida = dataItemValida(dataItem)
                                    val categoriaValida = tipoItem in categoriasDisponiveis
                                    val itemCompleto = tituloValido && dataValida && categoriaValida
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (itemCompleto) {
                                                if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.18f)
                                            } else {
                                                Color(0xFFF59E0B).copy(alpha = if (isDark) 0.65f else 0.45f)
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = tr("Item ${index + 1}/$totalItensModoSeparado", "Item ${index + 1}/$totalItensModoSeparado"),
                                                    color = textSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = if (itemCompleto) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.16f),
                                                    border = BorderStroke(1.dp, if (itemCompleto) Color(0xFF16A34A).copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.40f))
                                                ) {
                                                    Text(
                                                        text = if (itemCompleto) tr("Pronto", "Ready") else tr("Pendente", "Pending"),
                                                        color = if (itemCompleto) Color(0xFF16A34A) else Color(0xFFF59E0B),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            OutlinedTextField(
                                                value = tituloItemAtual,
                                                onValueChange = { novoTitulo ->
                                                    itemTituloOverrides = if (novoTitulo == tituloItemPadrao) {
                                                        itemTituloOverrides - item.id
                                                    } else {
                                                        itemTituloOverrides + (item.id to novoTitulo)
                                                    }
                                                },
                                                label = { Text(tr("Título do aviso *", "Reminder title *")) },
                                                singleLine = true,
                                                isError = !tituloValido,
                                                supportingText = {
                                                    if (!tituloValido) {
                                                        Text(tr("Título obrigatório.", "Title is required."))
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            OutlinedTextField(
                                                value = dataItem,
                                                onValueChange = {},
                                                label = { Text(tr("Data deste aviso *", "Date for this reminder *")) },
                                                readOnly = true,
                                                enabled = true,
                                                isError = !dataValida,
                                                supportingText = {
                                                    if (!dataValida) {
                                                        Text(tr("Data inválida. Toque no calendário para corrigir.", "Invalid date. Tap calendar to fix it."))
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        abrirDatePicker(dataItem) { novaData ->
                                                            itemDataAvisoOverrides = itemDataAvisoOverrides + (item.id to novaData)
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Event, contentDescription = null, tint = textSecondary)
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenuBox(
                                                expanded = itemCategoriaMenuAbertoId == item.id,
                                                onExpandedChange = { expanded ->
                                                    itemCategoriaMenuAbertoId = if (expanded) item.id else null
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = tipoManutencaoLabel(tipoItem),
                                                    onValueChange = {},
                                                    label = { Text(tr("Categoria *", "Category *")) },
                                                    readOnly = true,
                                                    isError = !categoriaValida,
                                                    modifier = Modifier
                                                        .menuAnchor()
                                                        .fillMaxWidth(),
                                                    leadingIcon = {
                                                        TipoIcon(
                                                            tipo = tipoItem,
                                                            tint = textSecondary,
                                                            size = 18.dp
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemCategoriaMenuAbertoId == item.id)
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = itemCategoriaMenuAbertoId == item.id,
                                                    onDismissRequest = { itemCategoriaMenuAbertoId = null }
                                                ) {
                                                    categoriasDisponiveis.forEach { tipo ->
                                                        DropdownMenuItem(
                                                            text = { Text(tipoManutencaoLabel(tipo)) },
                                                            onClick = {
                                                                itemTipoOverrides = itemTipoOverrides + (item.id to tipo)
                                                                itemCategoriaMenuAbertoId = null
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            tr("Categoria", "Category"),
                                                            color = textSecondary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            tipoManutencaoLabel(tipoItem),
                                                            color = textSecondary,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            tr("Valor", "Amount"),
                                                            color = textSecondary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = currencyFormatter.format(item.valor),
                                                            color = textSecondary,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        tr("Descrição do produto", "Product description"),
                                                        color = textSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = item.nome,
                                                        color = textPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        tr("Quantidade", "Quantity"),
                                                        color = textSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = item.quantidade.coerceAtLeast(1).toString(),
                                                        color = textPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        EtapaRevisaoAvisoContent(
                            isDark = isDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentBlue = accentBlue,
                            tituloLugar = tituloAviso.trim().ifBlank { localServicoInput.trim() },
                            descricao = descricao,
                            tipoSelecionado = tipoSelecionado,
                            isModoLista = isModoLista,
                            listaItensDetectados = listaItensDetectados,
                            quantidadeTotalItens = qrQuantidadeTotalItens
                                ?: listaItensDetectados.takeIf { it.isNotEmpty() }?.sumOf { it.quantidade.coerceAtLeast(1) }
                                ?: (quantidadeManualInput.toIntOrNull()?.coerceAtLeast(1) ?: 1),
                            mostrarTotal = !avisoSemTotal,
                            mostrarQuantidade = !avisoSemQuantidade,
                            kmBase = kmBase,
                            data = data,
                            dataAviso = dataAviso,
                            horaNotificacao = horaNotificacao,
                            valorInput = valorInput,
                            itemDataAvisoOverrides = itemDataAvisoOverrides,
                            itemValorOverrides = itemValorOverrides,
                            itemTipoOverrides = itemTipoOverrides,
                            contatoSelecionado = contatoSelecionado,
                            cidadeAtual = cidadeAtual,
                            ufAtual = ufAtual,
                            isRegistroServico = isRegistroServico,
                            repetirAteDesativar = repetirAteDesativar,
                            descricaoRepeticao = descricaoFrequenciaSelecionada(),
                            mostrarResumoSimplificadoPosto = isFluxoPosto,
                            tituloCategoria = if (isFluxoPosto) tr("Posto", "Fuel") else tipoManutencaoLabel(tipoSelecionado),
                            onAcaoContato = { enviarMensagemWhatsapp(it) }
                        )
                    }
                }
            }
            }
        }
        // Overlay do guia de manutenção — cobre o dialog inteiro ao ser aberto
        if (showGuiaManutencao) {
            GuiaManutencaoOverlay(
                onDismiss = { showGuiaManutencao = false },
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentBlue = accentBlue,
                pageBackground = pageBackground
            )
        }
        }
    }
}

private fun limparTextoProdutosRemovendoTotal(texto: String): String {
    val marcadorTotal = Regex("(?i)\\b(valor\\s*total|total\\s*(da\\s*nota)?|vl\\.?\\s*total)\\b")
    return texto
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() && !marcadorTotal.containsMatchIn(it) }
        .joinToString("\n")
        .trim()
}

private fun descricaoPadraoPorCategoria(tipo: TipoManutencao, ano: Int): String? = when (tipo) {
    TipoManutencao.IPVA -> "IPVA Ano $ano"
    TipoManutencao.LICENCIAMENTO -> "Licenciamento Ano $ano"
    TipoManutencao.SEGURO -> "Renovacao de seguro $ano"
    TipoManutencao.LAVAGEM -> "Realizar lavagem"
    else -> null
}

private fun formatarValorMonetarioCampo(input: String): String {
    val filtrado = input.filter { it.isDigit() || it == '.' || it == ',' }
    if (filtrado.isBlank()) return ""

    val normalizado = filtrado.replace(",", ".")
    val partes = normalizado.split(".")
    val inteiro = partes.firstOrNull().orEmpty().ifBlank { "0" }
    val decimal = partes.drop(1).joinToString("").take(2)

    return if (decimal.isNotEmpty()) {
        "$inteiro,$decimal"
    } else {
        inteiro
    }
}

private fun isDescricaoPadraoCategoria(texto: String): Boolean {
    val textoLimpo = texto.trim()
    return Regex(
        "^(IPVA|Licenciamento|Seguro|Renovacao\\s+de\\s+seguro)\\s+(Ano\\s+)?\\d{4}$",
        RegexOption.IGNORE_CASE
    )
        .matches(textoLimpo) || textoLimpo.equals("Realizar lavagem", ignoreCase = true)
}

private fun tiposAvisoCadastroPorVeiculo(tipoVeiculo: TipoVeiculo): List<TipoManutencao> = when (tipoVeiculo) {
    TipoVeiculo.BICICLETA -> listOf(
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.REVISAO,
        TipoManutencao.FREIO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.BIKE_ELETRICA -> listOf(
        TipoManutencao.CORRENTE,
        TipoManutencao.LUBRIFICACAO,
        TipoManutencao.PEDIVELA,
        TipoManutencao.ACESSORIOS,
        TipoManutencao.CONFORTO,
        TipoManutencao.PNEU,
        TipoManutencao.TRANSMISSAO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.REVISAO,
        TipoManutencao.FREIO,
        TipoManutencao.BATERIA,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.MOTO -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.OLEO,
        TipoManutencao.LAVAGEM,
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
    TipoVeiculo.CAMINHAO,
    TipoVeiculo.VAN,
    TipoVeiculo.ONIBUS,
    TipoVeiculo.CAMINHONETE,
    TipoVeiculo.FURGAO,
    TipoVeiculo.HATCH,
    TipoVeiculo.MOTORHOME -> listOf(
        TipoManutencao.ABASTECIMENTO,
        TipoManutencao.OLEO,
        TipoManutencao.LAVAGEM,
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
        TipoManutencao.OLEO,
        TipoManutencao.LAVAGEM,
        TipoManutencao.FREIO,
        TipoManutencao.MECANICA,
        TipoManutencao.BATERIA,
        TipoManutencao.PNEU,
        TipoManutencao.REVISAO,
        TipoManutencao.OUTROS
    )
    TipoVeiculo.VEICULO_ELETRICO -> listOf(
        TipoManutencao.FREIO,
        TipoManutencao.LAVAGEM,
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
        TipoManutencao.OLEO,
        TipoManutencao.LAVAGEM,
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

private fun categoriaPermiteEscanearNota(tipo: TipoManutencao, tipoVeiculo: TipoVeiculo): Boolean {
    if (tipoVeiculo == TipoVeiculo.BICICLETA || tipoVeiculo == TipoVeiculo.BIKE_ELETRICA) {
        return true
    }
    return when (tipo) {
    TipoManutencao.ABASTECIMENTO,
    TipoManutencao.OLEO,
    TipoManutencao.FREIO,
    TipoManutencao.MECANICA,
    TipoManutencao.FUNILARIA,
    TipoManutencao.BATERIA,
    TipoManutencao.LAVAGEM,
    TipoManutencao.VIDROS,
    TipoManutencao.PNEU,
    TipoManutencao.REVISAO,
        TipoManutencao.IPVA -> true
        else -> false
    }
}

private fun detectarTipoInicialDoLembrete(
    nomeItem: String,
    tipoBase: TipoManutencao,
    categoriasDisponiveis: List<TipoManutencao>
): TipoManutencao {
    val detectadoPorTexto = detectarTipoPeloTexto(nomeItem)
    return when {
        detectadoPorTexto in categoriasDisponiveis -> detectadoPorTexto
        tipoBase in categoriasDisponiveis -> tipoBase
        else -> categoriasDisponiveis.firstOrNull() ?: TipoManutencao.OUTROS
    }
}

private const val PROF_CITY_TAG = "ProfCidade"

data class BuscaProfissionaisCidadeResultado(
    val cidade: String?,
    val estado: String?,
    val profissionais: List<ProfissionalCidadeEncontrado>
)

data class ProfissionalCidadeEncontrado(
    val nome: String,
    val telefone: String,
    val endereco: String
)

private data class LocalizacaoCidade(
    val latitude: Double,
    val longitude: Double,
    val cidade: String?,
    val estado: String?
)

fun buscarProfissionaisDaCidadeAtual(
    context: Context,
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): Result<BuscaProfissionaisCidadeResultado> = runCatching {
    Log.d(PROF_CITY_TAG, "buscaProfissionaisDaCidadeAtual: resolvendo localizacao.")
    val localizacao = obterCidadePelaLocalizacao(context)
        ?: throw IllegalStateException("Ative a localizacao para buscar profissionais da sua cidade.")
    Log.d(
        PROF_CITY_TAG,
        "Localizacao obtida: lat=${localizacao.latitude} lng=${localizacao.longitude} cidade=${localizacao.cidade} uf=${localizacao.estado}"
    )
    val profissionais = buscarProfissionaisOverpass(
        context = context,
        latitude = localizacao.latitude,
        longitude = localizacao.longitude,
        tipoSelecionado = tipoSelecionado,
        cidade = localizacao.cidade,
        estado = localizacao.estado,
        isBikeVehicle = isBikeVehicle
    )
    BuscaProfissionaisCidadeResultado(
        cidade = localizacao.cidade,
        estado = localizacao.estado,
        profissionais = profissionais
    )
}

private fun obterCidadePelaLocalizacao(context: Context): LocalizacaoCidade? {
    val permissaoFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val permissaoCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!permissaoFine && !permissaoCoarse) {
        Log.w(PROF_CITY_TAG, "Permissao de localizacao nao concedida (fine/coarse).")
        return null
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    var melhorLocal: Location? = null

    providers.forEach { provider ->
        val local = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: return@forEach
        if (melhorLocal == null || local.accuracy < melhorLocal!!.accuracy || local.time > melhorLocal!!.time) {
            melhorLocal = local
        }
    }

    val atual = melhorLocal ?: run {
        Log.w(PROF_CITY_TAG, "Nenhuma localizacao conhecida disponivel. Tentando localizacao atual.")
        obterLocalAtualCompat(locationManager) ?: run {
            Log.w(PROF_CITY_TAG, "Falha ao obter localizacao atual por getCurrentLocation.")
            return null
        }
    }
    var cidade: String? = null
    var estado: String? = null
    runCatching {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        @Suppress("DEPRECATION")
        val endereco = geocoder.getFromLocation(atual.latitude, atual.longitude, 1)?.firstOrNull()
        cidade = endereco?.subAdminArea ?: endereco?.locality
        estado = endereco?.adminArea
    }.onFailure { erro ->
        Log.w(PROF_CITY_TAG, "Falha ao resolver cidade/UF com Geocoder.", erro)
    }

    return LocalizacaoCidade(
        latitude = atual.latitude,
        longitude = atual.longitude,
        cidade = cidade,
        estado = estado
    )
}

private fun obterLocalAtualCompat(locationManager: LocationManager): Location? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

    val provider = when {
        runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
            LocationManager.NETWORK_PROVIDER
        runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
            LocationManager.GPS_PROVIDER
        else -> LocationManager.PASSIVE_PROVIDER
    }

    val latch = CountDownLatch(1)
    var location: Location? = null
    val executor = Executors.newSingleThreadExecutor()
    try {
        locationManager.getCurrentLocation(
            provider,
            null,
            executor
        ) { atual ->
            location = atual
            latch.countDown()
        }
        latch.await(4, TimeUnit.SECONDS)
    } catch (erro: SecurityException) {
        Log.w(PROF_CITY_TAG, "Sem permissao para getCurrentLocation.", erro)
    } catch (erro: Throwable) {
        Log.w(PROF_CITY_TAG, "Falha ao chamar getCurrentLocation.", erro)
    } finally {
        executor.shutdown()
    }
    return location
}

private fun buscarProfissionaisOverpass(
    context: Context,
    latitude: Double,
    longitude: Double,
    tipoSelecionado: TipoManutencao,
    cidade: String?,
    estado: String?,
    isBikeVehicle: Boolean
): List<ProfissionalCidadeEncontrado> {
    Log.d(PROF_CITY_TAG, "Consultando Overpass. lat=$latitude lng=$longitude tipo=$tipoSelecionado cidade=$cidade uf=$estado isBikeVehicle=$isBikeVehicle")
    val payloadCidade = cidade
        ?.takeIf { it.isNotBlank() }
        ?.let { executarConsultaOverpass(montarOverpassCityAreaQuery(it, estado, tipoSelecionado, isBikeVehicle)) }
    val payload = if (!payloadCidade.isNullOrBlank() && contarElementosOverpass(payloadCidade) > 0) {
        payloadCidade
    } else {
        if (!payloadCidade.isNullOrBlank()) {
            Log.w(PROF_CITY_TAG, "Consulta por limite da cidade retornou zero elementos. Aplicando fallback por raio.")
        }
        executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 15000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
            ?: executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 50000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
            ?: executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 120000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
            ?: executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 250000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
    }
        ?: throw IllegalStateException("Servidor de busca ocupado no momento. Toque em Atualizar em alguns segundos.")
    val root = JSONObject(payload)
    val elements = root.optJSONArray("elements") ?: return emptyList()
    Log.d(PROF_CITY_TAG, "Overpass retornou elements=${elements.length()}")

    val contatos = mutableListOf<ProfissionalCidadeEncontrado>()
    var semTelefone = 0
    var semEnderecoPreenchido = 0
    for (i in 0 until elements.length()) {
        val element = elements.optJSONObject(i) ?: continue
        val tags = element.optJSONObject("tags") ?: continue
        val nome = tags.optString("name").trim()
        if (nome.isBlank()) continue
        val telefone = listOf(
            "contact:phone",
            "phone",
            "mobile",
            "telephone",
            "contact:mobile",
            "contact:whatsapp",
            "whatsapp"
        ).mapNotNull { chave ->
            tags.optString(chave).trim().takeIf { it.isNotBlank() }
        }.firstOrNull().orEmpty()
        if (telefone.isBlank()) semTelefone += 1
        val endereco = montarEnderecoProfissional(context, element, tags)
        if (endereco.isBlank()) semEnderecoPreenchido += 1
        val candidato = ProfissionalCidadeEncontrado(
            nome = nome,
            telefone = telefone,
            endereco = endereco
        )
        contatos += candidato
    }

    val contatosFiltradosPorTipo = aplicarFiltroProfissionaisPorTipo(tipoSelecionado, contatos)
    val resultado = contatosFiltradosPorTipo.distinctBy { contato ->
        val telefone = contato.telefone.filter(Char::isDigit)
        if (telefone.isNotBlank()) telefone else "${contato.nome.lowercase(Locale.ROOT)}|${contato.endereco.lowercase(Locale.ROOT)}"
    }.sortedBy { it.nome.lowercase(Locale.ROOT) }
    Log.i(
        PROF_CITY_TAG,
        "Profissionais validos apos filtros: total=${resultado.size} semTelefone=$semTelefone semEndereco=$semEnderecoPreenchido tipo=$tipoSelecionado"
    )
    return resultado
}

private fun contarElementosOverpass(payload: String): Int = runCatching {
    JSONObject(payload).optJSONArray("elements")?.length() ?: 0
}.getOrDefault(0)

private fun montarOverpassQuery(
    latitude: Double,
    longitude: Double,
    raioMetros: Int,
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): String {
    val tags = tagsBuscaPorContexto(tipoSelecionado, isBikeVehicle)
    val blocos = buildString {
        tags.forEach { (chave, valor) ->
            append("  node(around:$raioMetros,$latitude,$longitude)[\"$chave\"=\"$valor\"];\n")
            append("  way(around:$raioMetros,$latitude,$longitude)[\"$chave\"=\"$valor\"];\n")
            append("  relation(around:$raioMetros,$latitude,$longitude)[\"$chave\"=\"$valor\"];\n")
        }
    }
    return """
        [out:json][timeout:20];
        (
$blocos
        );
        out center tags 180;
    """.trimIndent()
}

private fun montarOverpassCityAreaQuery(
    cidade: String,
    estado: String?,
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): String {
    val cidadeEscapada = cidade.replace("\"", "\\\"")
    val estadoEscapado = estado?.replace("\"", "\\\"")
    val filtroEstado = estadoEscapado?.takeIf { it.isNotBlank() }?.let { """["is_in:state"="$it"]""" } ?: ""
    val tags = tagsBuscaPorContexto(tipoSelecionado, isBikeVehicle)
    val blocos = buildString {
        tags.forEach { (chave, valor) ->
            append("  node(area.searchArea)[\"$chave\"=\"$valor\"];\n")
            append("  way(area.searchArea)[\"$chave\"=\"$valor\"];\n")
            append("  relation(area.searchArea)[\"$chave\"=\"$valor\"];\n")
        }
    }
    return """
        [out:json][timeout:25];
        area["name"="$cidadeEscapada"]["boundary"="administrative"]["admin_level"="8"]$filtroEstado->.searchArea;
        (
$blocos
        );
        out center tags 400;
    """.trimIndent()
}

private fun tagsBuscaPorContexto(
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): List<Pair<String, String>> {
    if (isBikeVehicle) {
        return listOf(
            "shop" to "bicycle",
            "service:bicycle:repair" to "yes"
        )
    }
    return if (tipoSelecionado == TipoManutencao.PNEU) {
        listOf(
            "shop" to "tyres",
            "shop" to "car_repair",
            "amenity" to "car_repair"
        )
    } else {
        listOf(
            "shop" to "car_repair",
            "amenity" to "car_repair",
            "craft" to "mechanic",
            "service:vehicle:repair" to "yes"
        )
    }
}

private fun aplicarFiltroProfissionaisPorTipo(
    tipoSelecionado: TipoManutencao,
    contatos: List<ProfissionalCidadeEncontrado>
): List<ProfissionalCidadeEncontrado> {
    if (tipoSelecionado != TipoManutencao.FUNILARIA) return contatos

    val palavras = listOf(
        "funilaria",
        "funileiro",
        "lanternagem",
        "martelinho",
        "martelinho de ouro",
        "reparo de para-choque",
        "pintura automotiva",
        "chapeacao",
        "chapeação"
    )
    val filtrados = contatos.filter { contato ->
        val texto = "${contato.nome} ${contato.endereco}".lowercase(Locale.ROOT)
        palavras.any { termo -> texto.contains(termo) }
    }
    return if (filtrados.isNotEmpty()) filtrados else contatos
}

private fun executarConsultaOverpass(query: String): String? {
    val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://z.overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )
    endpoints.forEach { endpointUrl ->
        Log.d(PROF_CITY_TAG, "Tentando endpoint Overpass: $endpointUrl")
        val resultado = runCatching {
            val endpoint = URL(endpointUrl)
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            conn.outputStream.use { out ->
                out.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            Log.d(PROF_CITY_TAG, "Endpoint $endpointUrl respondeu HTTP $code")
            if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        }
        val payload = resultado.getOrNull()
        if (!payload.isNullOrBlank()) {
            Log.d(PROF_CITY_TAG, "Endpoint $endpointUrl retornou payload valido.")
            return payload
        }
        val erro = resultado.exceptionOrNull()
        if (erro != null) {
            Log.w(PROF_CITY_TAG, "Falha ao consultar endpoint $endpointUrl", erro)
        }
    }
    Log.e(PROF_CITY_TAG, "Nenhum endpoint Overpass retornou dados validos.")
    return null
}

private fun montarEnderecoProfissional(
    context: Context,
    element: JSONObject,
    tags: JSONObject
): String {
    val enderecoCompleto = tags.optString("addr:full").trim()
    if (enderecoCompleto.isNotBlank()) return enderecoCompleto

    val rua = tags.optString("addr:street").trim()
    val numero = tags.optString("addr:housenumber").trim()
    val bairro = tags.optString("addr:suburb").trim()
    val cidade = tags.optString("addr:city").trim()
    val estado = tags.optString("addr:state").trim()

    val linhaRua = listOf(rua, numero).filter { it.isNotBlank() }.joinToString(", ")
    val linhaLocal = listOf(bairro, cidade, estado).filter { it.isNotBlank() }.joinToString(" - ")
    val enderecoTags = listOf(linhaRua, linhaLocal).filter { it.isNotBlank() }.joinToString(" | ")
    if (enderecoTags.isNotBlank()) return enderecoTags

    val coordenadas = extrairLatLngDoElemento(element) ?: return ""
    return enderecoPorGeocoder(context, coordenadas.first, coordenadas.second)
}

private fun extrairLatLngDoElemento(element: JSONObject): Pair<Double, Double>? {
    val latDireto = element.optDouble("lat", Double.NaN)
    val lonDireto = element.optDouble("lon", Double.NaN)
    if (!latDireto.isNaN() && !lonDireto.isNaN()) return latDireto to lonDireto

    val center = element.optJSONObject("center") ?: return null
    val latCenter = center.optDouble("lat", Double.NaN)
    val lonCenter = center.optDouble("lon", Double.NaN)
    if (latCenter.isNaN() || lonCenter.isNaN()) return null
    return latCenter to lonCenter
}

private fun enderecoPorGeocoder(context: Context, latitude: Double, longitude: Double): String {
    return runCatching {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        @Suppress("DEPRECATION")
        val endereco = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        listOfNotNull(
            endereco?.thoroughfare?.takeIf { it.isNotBlank() },
            endereco?.subThoroughfare?.takeIf { it.isNotBlank() }
        ).joinToString(", ").ifBlank {
            listOfNotNull(
                endereco?.subLocality?.takeIf { it.isNotBlank() },
                endereco?.locality?.takeIf { it.isNotBlank() },
                endereco?.adminArea?.takeIf { it.isNotBlank() }
            ).joinToString(" - ")
        }
    }.onFailure { erro ->
        Log.w(PROF_CITY_TAG, "Falha no geocoder reverso para lat=$latitude lon=$longitude", erro)
    }.getOrNull().orEmpty()
}

private val ESTADOS_UF = listOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
    "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
    "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
)










