package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG_ONBOARDING_NOVO_CARRO = "OnboardingNovoCarro"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingNovoCarroScreen(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit,
    onboardingVehicleNumber: Int = 1
) {
    OnboardingNovoCarroScreenContent(
        onDismiss = onDismiss,
        onSalvar = onSalvar,
        allowBackNavigation = false,
        isOnboardingVariant = true,
        onboardingVehicleNumber = onboardingVehicleNumber
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoCarroScreenPrimeiroFluxoComVoltar(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit
) {
    OnboardingNovoCarroScreenContent(
        onDismiss = onDismiss,
        onSalvar = onSalvar,
        allowBackNavigation = true,
        isOnboardingVariant = false
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OnboardingNovoCarroScreenContent(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit,
    allowBackNavigation: Boolean = true,
    isOnboardingVariant: Boolean = false,
    onboardingVehicleNumber: Int = 1
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nomeUsuarioLogado = remember {
        val displayName = FirebaseAuth.getInstance().currentUser?.displayName
            ?.trim()
            ?.split("\\s+".toRegex())
            ?.filter { it.isNotBlank() }
        when {
            displayName.isNullOrEmpty() -> "Eu mesmo"
            displayName.size == 1 -> displayName.first()
            else -> "${displayName.first()} ${displayName.last()}"
        }
    }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bgLight = if (isOnboardingVariant) Color.Black else if (isDark) Color.Black else scheme.background
    val borderLight = if (isOnboardingVariant) Color(0xFF262626) else scheme.outlineVariant
    val textPrimary = if (isOnboardingVariant) Color(0xFFF8FAFC) else scheme.onBackground
    val textSecondary = if (isOnboardingVariant) Color(0xFFA3A3A3) else scheme.onSurfaceVariant
    val accentBlue = if (isOnboardingVariant) Color(0xFF60A5FA) else scheme.primary
    val selectorTextPrimary = if (isOnboardingVariant) Color(0xFFF8FAFC) else scheme.onSurface
    val selectorTextSecondary = if (isOnboardingVariant) Color(0xFF94A3B8) else scheme.onSurfaceVariant
    val selectorAccent = if (isOnboardingVariant) Color(0xFF60A5FA) else scheme.primary
    val selectorBorder = if (isOnboardingVariant) Color(0xFF2A2A2A) else scheme.outlineVariant
    val selectorDropdownBg = if (isOnboardingVariant) Color(0xFF0A0A0A) else if (isDark) Color(0xFF111827) else scheme.surface
    val selectorFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = selectorTextPrimary,
        unfocusedTextColor = selectorTextPrimary,
        disabledTextColor = selectorTextPrimary,
        focusedLabelColor = selectorTextSecondary,
        unfocusedLabelColor = selectorTextSecondary,
        disabledLabelColor = selectorTextSecondary,
        focusedPlaceholderColor = selectorTextSecondary,
        unfocusedPlaceholderColor = selectorTextSecondary,
        disabledPlaceholderColor = selectorTextSecondary,
        focusedTrailingIconColor = selectorTextSecondary,
        unfocusedTrailingIconColor = selectorTextSecondary,
        disabledTrailingIconColor = selectorTextSecondary,
        focusedBorderColor = if (isOnboardingVariant) selectorBorder else selectorAccent,
        unfocusedBorderColor = selectorBorder,
        disabledBorderColor = selectorBorder,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )
    val errorBorder = scheme.error
    val successBorder = if (isOnboardingVariant) Color(0xFF4ADE80) else Color(0xFF16A34A)
    @Composable
    fun selectorFieldColorsWithState(
        filled: Boolean,
        isError: Boolean
    ) = OutlinedTextFieldDefaults.colors(
        focusedTextColor = selectorTextPrimary,
        unfocusedTextColor = selectorTextPrimary,
        disabledTextColor = selectorTextPrimary,
        focusedLabelColor = selectorTextSecondary,
        unfocusedLabelColor = selectorTextSecondary,
        disabledLabelColor = selectorTextSecondary,
        focusedPlaceholderColor = selectorTextSecondary,
        unfocusedPlaceholderColor = selectorTextSecondary,
        disabledPlaceholderColor = selectorTextSecondary,
        focusedTrailingIconColor = selectorTextSecondary,
        unfocusedTrailingIconColor = selectorTextSecondary,
        disabledTrailingIconColor = selectorTextSecondary,
        focusedBorderColor = when {
            isError -> errorBorder
            filled -> successBorder
            isOnboardingVariant -> selectorBorder
            else -> selectorAccent
        },
        unfocusedBorderColor = when {
            isError -> errorBorder
            filled -> successBorder
            else -> selectorBorder
        },
        disabledBorderColor = when {
            isError -> errorBorder
            filled -> successBorder
            else -> selectorBorder
        },
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )
    val cardBg = if (isOnboardingVariant) Color(0xFF050505) else if (isDark) Color(0xFF111827) else scheme.surface
    val carroBase = CarroInfo(nome = "", modelo = "")

    var nome by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var proprietario by remember { mutableStateOf("") }
    var quemUsaOpcao by remember { mutableStateOf("Selecione") }
    var quemUsaExpanded by remember { mutableStateOf(false) }
    var kmAtualStr by remember { mutableStateOf("0") }
    var tipoSelecionado by remember { mutableStateOf<TipoVeiculo?>(null) }
    var corSelecionada by remember { mutableStateOf<Int?>(null) }
    var vezesBatido by remember { mutableStateOf<Int?>(null) }
    var tempoComVeiculo by remember { mutableStateOf("") }
    var alvoVoz by remember { mutableStateOf("nome") }
    var modelosFipe by remember { mutableStateOf<List<FipeModeloDto>>(emptyList()) }
    var carregandoModelos by remember { mutableStateOf(false) }
    var modeloSelecionadoCodigo by remember { mutableStateOf<Int?>(null) }
    var anosFipe by remember { mutableStateOf<List<String>>(emptyList()) }
    var anoSelecionado by remember { mutableStateOf("") }
    var etapaCadastro by remember { mutableStateOf(1) }
    var tentouAvancarEtapa1 by remember { mutableStateOf(false) }
    var tentouSalvarEtapa2 by remember { mutableStateOf(false) }
    var tipoAnterior by remember { mutableStateOf<TipoVeiculo?>(null) }
    var marcasFipeOnibus by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNomePickerScreen by remember { mutableStateOf(false) }
    var anoExpanded by remember { mutableStateOf(false) }
    var pendingOpenYearSelector by remember { mutableStateOf(false) }
    var nomeManualNoCadastro by remember { mutableStateOf(false) }
    var marcaManualNoCadastro by remember { mutableStateOf(false) }
    var nomeManualPorFalhaApi by remember { mutableStateOf(false) }
    var consultaModelosConcluida by remember { mutableStateOf(false) }

    val contentScrollState = rememberScrollState()
    val showTopBar by remember { derivedStateOf { contentScrollState.value <= 8 } }
    val sugestoesNomeExibidas = remember(modelosFipe) { modelosFipe }
    var filtroNomeVeiculo by remember { mutableStateOf("") }
    val sugestoesNomeFiltradas = remember(sugestoesNomeExibidas, filtroNomeVeiculo) {
        val busca = normalizarTextoBusca(filtroNomeVeiculo)
        if (busca.isBlank()) {
            sugestoesNomeExibidas
        } else {
            sugestoesNomeExibidas.filter { modeloItem ->
                normalizarTextoBusca(modeloItem.nome).contains(busca)
            }
        }
    }
    val opcoesCor = remember { coresVeiculoDisponiveis() }
    val nomeCorSelecionada = remember(corSelecionada, opcoesCor) {
        corSelecionada?.let { cor ->
            opcoesCor.firstOrNull { it.color.toArgb() == cor }?.name
        }
    }
    val anoAtualPermitido = remember { Calendar.getInstance().get(Calendar.YEAR) + 1 }
    val anoSelecionadoNumerico = remember(anoSelecionado) {
        Regex("\\d{4}").find(anoSelecionado)?.value?.toIntOrNull()
    }
    val anoValido = remember(anoSelecionadoNumerico, anoAtualPermitido) {
        val ano = anoSelecionadoNumerico
        ano != null && ano in 1900..anoAtualPermitido
    }

    val isBikeTypeGlobal =
        tipoSelecionado == TipoVeiculo.BICICLETA || tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    val tipoSemAno =
        tipoSelecionado == TipoVeiculo.BICICLETA ||
            tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    val etapa1Valida = marca.isNotBlank() &&
            nome.isNotBlank() &&
            modelo.isNotBlank() &&
            (tipoSemAno || (anoSelecionado.isNotBlank() && anoValido)) &&
            corSelecionada != null
    val hasTypeSelected = tipoSelecionado != null
    val hasBrandSelected = marca.isNotBlank()
    val aguardarBuscaModelos = hasBrandSelected && carregandoModelos

    val erroTipo = false
    val erroMarca = etapaCadastro == 1 && tentouAvancarEtapa1 && marca.isBlank()
    val erroNome = etapaCadastro == 1 && tentouAvancarEtapa1 && nome.isBlank()
    val erroModelo = etapaCadastro == 1 && tentouAvancarEtapa1 && modelo.isBlank()
    val erroAno = etapaCadastro == 1 && !tipoSemAno && tentouAvancarEtapa1 && (anoSelecionado.isBlank() || !anoValido)
    val erroCor = etapaCadastro == 1 && tentouAvancarEtapa1 && corSelecionada == null
    val erroKm = etapaCadastro == 2 && !isBikeTypeGlobal && tentouSalvarEtapa2 && kmAtualStr.filter(Char::isDigit).isEmpty()
    val etapa2Valida = proprietario.isNotBlank() &&
            quemUsaOpcao != "Selecione" &&
            (isBikeTypeGlobal || vezesBatido != null) &&
            tempoComVeiculo.isNotBlank() &&
            (isBikeTypeGlobal || kmAtualStr.filter(Char::isDigit).isNotEmpty())
    val erroQuemUsa = etapaCadastro == 2 && tentouSalvarEtapa2 && quemUsaOpcao == "Selecione"
    val erroProprietario = etapaCadastro == 2 && tentouSalvarEtapa2 && proprietario.isBlank()
    val erroBatidas = etapaCadastro == 2 && !isBikeTypeGlobal && tentouSalvarEtapa2 && vezesBatido == null
    val erroTempo = etapaCadastro == 2 && tentouSalvarEtapa2 && tempoComVeiculo.isBlank()

    val marcasDisponiveis = when {
        tipoSelecionado == null -> marcasSuportadas
        tipoSelecionado == TipoVeiculo.ONIBUS && marcasFipeOnibus.isNotEmpty() -> marcasFipeOnibus
        else -> marcasPorTipo(tipoSelecionado)
    }

    LaunchedEffect(tipoSelecionado) {
        if (tipoAnterior != null && tipoSelecionado != tipoAnterior) {
            marca = ""
            nome = ""
            modelo = ""
            proprietario = ""
            quemUsaOpcao = "Selecione"
            quemUsaExpanded = false
            kmAtualStr = "0"
            corSelecionada = null
            vezesBatido = null
            tempoComVeiculo = ""
            marcaManualNoCadastro = false
            modelosFipe = emptyList()
            carregandoModelos = false
            consultaModelosConcluida = false
            nomeManualPorFalhaApi = false
            modeloSelecionadoCodigo = null
            anosFipe = emptyList()
            anoSelecionado = ""
            nomeManualNoCadastro = false
            anoExpanded = false
            pendingOpenYearSelector = false
            etapaCadastro = 1
            tentouAvancarEtapa1 = false
            tentouSalvarEtapa2 = false
        }
        tipoAnterior = tipoSelecionado

        if (tipoSelecionado == TipoVeiculo.ONIBUS) {
            marcasFipeOnibus = withContext(Dispatchers.IO) { carregarMarcasFipePorTipo(context, tipoSelecionado) }
        } else {
            marcasFipeOnibus = emptyList()
        }

        if (marca.isNotBlank() && !marcasDisponiveis.contains(marca)) {
            marca = ""
            modelosFipe = emptyList()
            carregandoModelos = false
            consultaModelosConcluida = false
            nomeManualPorFalhaApi = false
            modeloSelecionadoCodigo = null
            anosFipe = emptyList()
            anoSelecionado = ""
            nomeManualNoCadastro = false
            anoExpanded = false
            pendingOpenYearSelector = false
            marcaManualNoCadastro = false
        }
    }

    LaunchedEffect(marca, tipoSelecionado) {
        val tipoAtual = tipoSelecionado
        Log.d(
            TAG_ONBOARDING_NOVO_CARRO,
            "LaunchedEffect(marca,tipo) marca='$marca' tipo='${tipoAtual?.name}'"
        )
        if (marca.isBlank() || tipoAtual == null) {
            Log.d(
                TAG_ONBOARDING_NOVO_CARRO,
                "limpando modelos/anos: marca vazia ou tipo nulo"
            )
            modelosFipe = emptyList()
            carregandoModelos = false
            consultaModelosConcluida = false
            nomeManualPorFalhaApi = false
            modeloSelecionadoCodigo = null
            anosFipe = emptyList()
            anoSelecionado = ""
            anoExpanded = false
            pendingOpenYearSelector = false
            return@LaunchedEffect
        }
        carregandoModelos = true
        consultaModelosConcluida = false
        try {
            Log.d(
                TAG_ONBOARDING_NOVO_CARRO,
                "carregando modelos FIPE marca='$marca' tipo='${tipoAtual.name}'"
            )
            val modelosCarregados = withContext(Dispatchers.IO) {
                carregarModelosFipePorMarca(context, marca, tipoAtual)
            }
            modelosFipe = modelosCarregados
            Log.d(
                TAG_ONBOARDING_NOVO_CARRO,
                "modelos carregados qtd=${modelosCarregados.size} marca='$marca'"
            )
            if (modelosCarregados.isEmpty()) {
                nomeManualPorFalhaApi = true
                filtroNomeVeiculo = ""
                Log.w(
                    TAG_ONBOARDING_NOVO_CARRO,
                    "sem modelos para marca='$marca', habilitando preenchimento manual automático"
                )
            } else {
                nomeManualPorFalhaApi = false
            }
            modeloSelecionadoCodigo = modelosCarregados
                .firstOrNull { normalizarTextoBusca(it.nome) == normalizarTextoBusca(modelo) }
                ?.codigo
            Log.d(
                TAG_ONBOARDING_NOVO_CARRO,
                "modeloSelecionadoCodigo=${modeloSelecionadoCodigo ?: -1} modeloAtual='$modelo'"
            )
        } catch (t: Throwable) {
            Log.e(
                TAG_ONBOARDING_NOVO_CARRO,
                "erro ao carregar modelos FIPE marca='$marca' tipo='${tipoAtual.name}'",
                t
            )
            throw t
        } finally {
            carregandoModelos = false
            consultaModelosConcluida = true
            Log.d(TAG_ONBOARDING_NOVO_CARRO, "carregandoModelos=false (finalizado)")
        }
    }

    LaunchedEffect(modeloSelecionadoCodigo, marca) {
        val codigoModelo = modeloSelecionadoCodigo
        if (marca.isBlank() || codigoModelo == null) {
            anosFipe = emptyList()
            anoSelecionado = ""
            anoExpanded = false
            return@LaunchedEffect
        }
        anosFipe = withContext(Dispatchers.IO) { carregarAnosFipe(context, marca, codigoModelo, tipoSelecionado) }
        if (anosFipe.isEmpty()) {
            anoSelecionado = ""
            anoExpanded = false
        } else if (pendingOpenYearSelector) {
            anoSelecionado = ""
        }
        pendingOpenYearSelector = false
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val textoReconhecido = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!textoReconhecido.isNullOrBlank()) {
                if (alvoVoz == "motor") {
                    modelo = textoReconhecido
                } else {
                    nome = textoReconhecido
                }
            }
        }
    }

    fun voltarTela() {
        if (etapaCadastro == 2) {
            etapaCadastro = 1
        } else {
            onDismiss()
        }
    }

    if (showNomePickerScreen) {
        OnboardingNomeVeiculoPickerScreen(
            bgLight = bgLight,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            selectorBorder = selectorBorder,
            selectorAccent = selectorAccent,
            tipoSelecionado = tipoSelecionado,
            marcaSelecionada = marca,
            carregandoModelos = carregandoModelos,
            filtroNomeVeiculo = filtroNomeVeiculo,
            onFiltroNomeVeiculoChange = { filtroNomeVeiculo = it },
            sugestoesNomeFiltradas = sugestoesNomeFiltradas,
            onDismiss = { showNomePickerScreen = false },
            onSelectModelo = { modeloItem ->
                val (nomeExtraido, modeloExtraido) = separarNomeEMotorModelo(
                    descricaoCompleta = modeloItem.nome,
                    marcaSelecionada = marca
                )
                nome = nomeExtraido
                if (modeloExtraido.isNotBlank()) {
                    modelo = modeloExtraido
                }
                modeloSelecionadoCodigo = modeloItem.codigo
                nomeManualNoCadastro = false
                pendingOpenYearSelector = true
                showNomePickerScreen = false
            },
            onSelectNomeManual = { nomeManual ->
                nome = nomeManual
                modeloSelecionadoCodigo = null
                nomeManualNoCadastro = true
                pendingOpenYearSelector = false
                showNomePickerScreen = false
                Toast.makeText(
                    context,
                    "Você pode ajustar depois nas configurações do veículo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        return
    }

    BackHandler(enabled = true) {
        if (showNomePickerScreen) {
            showNomePickerScreen = false
        } else {
            voltarTela()
        }
    }

    Scaffold(
        containerColor = bgLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgLight)
                .padding(innerPadding)
                .then(if (isOnboardingVariant) Modifier.statusBarsPadding() else Modifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isOnboardingVariant) 0.dp else 2.dp)
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .padding(bottom = if (isOnboardingVariant) 20.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (isOnboardingVariant) 16.dp else 10.dp)
            ) {
                if (!isOnboardingVariant && allowBackNavigation) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(onClick = ::voltarTela) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                        }
                    }
                }

                if (!isOnboardingVariant) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-8).dp)
                            .padding(top = 0.dp, bottom = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = textSecondary.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddCircle,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "Novo veículo",
                            color = textPrimary,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isOnboardingVariant) {
                    if (allowBackNavigation) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 0.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(onClick = ::voltarTela) {
                                Text(
                                    text = "<",
                                    color = textPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                            tint = Color(0xFF6EA7E8),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Text(
                        text = "Vamos cadastrar seu primeiro item da garagem",
                        color = textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                NovoSectionCardOnboarding(
                    title = "",
                    icon = null,
                    containerColor = cardBg,
                    borderColor = borderLight,
                    modifier = if (isOnboardingVariant) Modifier.padding(top = 4.dp) else Modifier
                ) {
                    var showTipoDialog by remember { mutableStateOf(false) }
                    var showMarcaDialog by remember { mutableStateOf(false) }
                    var showCorDialog by remember { mutableStateOf(false) }
                    var batidasExpanded by remember { mutableStateOf(false) }
                    var tempoExpanded by remember { mutableStateOf(false) }
                    val tipoDialogListState = rememberLazyListState()
                    val marcaDialogListState = rememberLazyListState()
                    val corDialogListState = rememberLazyListState()
                    val noRippleInteraction = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Etapa $etapaCadastro de 2",
                            color = textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (etapaCadastro == 1) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tipoSelecionado?.label.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                isError = erroTipo,
                                label = { Text("Categoria") },
                                placeholder = { Text("Selecione") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTipoDialog) },
                                colors = selectorFieldColorsWithState(
                                    filled = tipoSelecionado != null,
                                    isError = erroTipo
                                ),
                                enabled = false,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = noRippleInteraction,
                                        indication = null
                                    ) { showTipoDialog = true }
                            )
                        }

                        if (showTipoDialog) {
                            Dialog(
                                onDismissRequest = { showTipoDialog = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = selectorDropdownBg)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                    ) {
                                        Text(
                                            text = "Selecione a categoria",
                                            color = textPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(
                                            color = selectorBorder.copy(alpha = 0.45f),
                                            thickness = 1.dp
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        val orderedTipos = listOf(
                                            TipoVeiculo.CARRO,
                                            TipoVeiculo.HATCH,
                                            TipoVeiculo.SUV,
                                            TipoVeiculo.CAMINHONETE,
                                            TipoVeiculo.MOTO,
                                            TipoVeiculo.VEICULO_ELETRICO,
                                            TipoVeiculo.BICICLETA,
                                            TipoVeiculo.BIKE_ELETRICA,
                                            TipoVeiculo.VAN,
                                            TipoVeiculo.FURGAO,
                                            TipoVeiculo.CAMINHAO,
                                            TipoVeiculo.ONIBUS,
                                            TipoVeiculo.MOTORHOME
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 360.dp)
                                        ) {
                                            LazyColumn(
                                                state = tipoDialogListState,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(orderedTipos.size) { index ->
                                                    val tipo = orderedTipos[index]
                                                    val isSelectedType = tipoSelecionado == tipo
                                                    Card(
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSelectedType) Color(0x1A60A5FA) else Color.Transparent
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelectedType) Color(0xFF60A5FA) else selectorBorder.copy(alpha = 0.7f)
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                interactionSource = noRippleInteraction,
                                                                indication = null
                                                            ) {
                                                                tipoSelecionado = tipo
                                                                showTipoDialog = false
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            VehicleIcon(
                                                                tipoVeiculo = tipo,
                                                                tint = textPrimary,
                                                                size = 50.dp
                                                            )
                                                            Text(
                                                                text = tipo.label,
                                                                color = textPrimary,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            SimpleLazyScrollbar(
                                                state = tipoDialogListState,
                                                itemCount = orderedTipos.size,
                                                trackColor = selectorBorder.copy(alpha = 0.35f),
                                                thumbColor = selectorAccent.copy(alpha = 0.95f),
                                                touchAreaWidth = 34.dp,
                                                barWidth = 4.dp,
                                                minThumbHeight = 24.dp,
                                                modifier = Modifier.align(Alignment.CenterEnd)
                                            )
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = { showTipoDialog = false },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isOnboardingVariant) Color.White else selectorAccent
                                            ),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = if (isOnboardingVariant) Color.White else selectorAccent
                                            )
                                        ) {
                                            Text(
                                                text = "Fechar",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (marcaManualNoCadastro) {
                            OutlinedTextField(
                                value = marca,
                                onValueChange = { marca = it },
                                isError = erroMarca,
                                label = { Text("Marca") },
                                placeholder = { Text("Digite a marca") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = selectorFieldColorsWithState(
                                    filled = marca.isNotBlank(),
                                    isError = erroMarca
                                ),
                                enabled = hasTypeSelected,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            TextButton(
                                onClick = {
                                    marcaManualNoCadastro = false
                                    marca = ""
                                    showMarcaDialog = true
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Ver lista de marcas", color = selectorAccent, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = marca,
                                    onValueChange = {},
                                    readOnly = true,
                                    isError = erroMarca,
                                    label = { Text("Marca") },
                                    placeholder = { Text("Selecione") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMarcaDialog) },
                                    enabled = false,
                                    colors = selectorFieldColorsWithState(
                                        filled = marca.isNotBlank(),
                                        isError = erroMarca
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = noRippleInteraction,
                                            indication = null
                                        ) {
                                            if (hasTypeSelected) {
                                                showMarcaDialog = true
                                            } else {
                                                Toast.makeText(context, "Escolha uma categoria antes de escolher marca ou cor", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                )
                            }
                        }

                        if (showMarcaDialog) {
                            Dialog(
                                onDismissRequest = { showMarcaDialog = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = selectorDropdownBg)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                    ) {
                                        Text(
                                            text = "Selecione a marca",
                                            color = textPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(
                                            color = selectorBorder.copy(alpha = 0.45f),
                                            thickness = 1.dp
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        val marcasComOutro = remember(marcasDisponiveis) {
                                            (marcasDisponiveis + "Outro").distinctBy { it.trim().lowercase() }
                                        }

                                        if (marcasComOutro.isEmpty()) {
                                            Text(
                                                text = "Nenhuma marca disponível",
                                                color = textSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 360.dp)
                                            ) {
                                                LazyColumn(
                                                    state = marcaDialogListState,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(end = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(marcasComOutro.size) { index ->
                                                        val marcaNome = marcasComOutro[index]
                                                        Card(
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (marca == marcaNome) Color(0x1A60A5FA) else Color.Transparent
                                                            ),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                if (marca == marcaNome) Color(0xFF60A5FA) else selectorBorder.copy(alpha = 0.7f)
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable(
                                                                    interactionSource = noRippleInteraction,
                                                                    indication = null
                                                                ) {
                                                                    Log.d(
                                                                        TAG_ONBOARDING_NOVO_CARRO,
                                                                        "marca selecionada no dialog='$marcaNome' tipo='${tipoSelecionado?.name}'"
                                                                    )
                                                                    marcaManualNoCadastro = marcaNome.equals("Outro", ignoreCase = true)
                                                                    marca = if (marcaManualNoCadastro) "" else marcaNome
                                                                    modelosFipe = emptyList()
                                                                    nome = ""
                                                                    modeloSelecionadoCodigo = null
                                                                    anosFipe = emptyList()
                                                                    anoSelecionado = ""
                                                                    filtroNomeVeiculo = ""
                                                                    nomeManualNoCadastro = false
                                                                    showMarcaDialog = false
                                                                    Log.d(
                                                                        TAG_ONBOARDING_NOVO_CARRO,
                                                                        "apos selecionar marca: marca='${if (marcaManualNoCadastro) "(manual)" else marca}', modelosFipeLimpo=${modelosFipe.isEmpty()}"
                                                                    )
                                                                }
                                                        ) {
                                                            Text(
                                                                text = marcaNome,
                                                                color = textPrimary,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                SimpleLazyScrollbar(
                                                    state = marcaDialogListState,
                                                    itemCount = marcasComOutro.size,
                                                    trackColor = selectorBorder.copy(alpha = 0.35f),
                                                    thumbColor = selectorAccent.copy(alpha = 0.95f),
                                                    touchAreaWidth = 34.dp,
                                                    barWidth = 4.dp,
                                                    minThumbHeight = 24.dp,
                                                    modifier = Modifier.align(Alignment.CenterEnd)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = { showMarcaDialog = false },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isOnboardingVariant) Color.White else selectorAccent
                                            ),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = if (isOnboardingVariant) Color.White else selectorAccent
                                            )
                                        ) {
                                            Text(
                                                text = "Fechar",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = nomeCorSelecionada.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                isError = erroCor,
                                label = { Text("Cor") },
                                placeholder = { Text("Selecione") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCorDialog) },
                                colors = selectorFieldColorsWithState(
                                    filled = corSelecionada != null,
                                    isError = erroCor
                                ),
                                enabled = false,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                    .clickable(
                                        interactionSource = noRippleInteraction,
                                        indication = null
                                    ) {
                                        if (hasBrandSelected) {
                                            showCorDialog = true
                                        } else {
                                            Toast.makeText(context, "Escolha a marca antes de preencher os outros campos", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            )
                        }

                        if (showCorDialog) {
                            Dialog(
                                onDismissRequest = { showCorDialog = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = selectorDropdownBg)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                    ) {
                                        Text(
                                            text = "Selecione a cor",
                                            color = textPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(
                                            color = selectorBorder.copy(alpha = 0.45f),
                                            thickness = 1.dp
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 360.dp)
                                        ) {
                                            LazyColumn(
                                                state = corDialogListState,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(opcoesCor.size) { index ->
                                                    val opcao = opcoesCor[index]
                                                    val isSelectedColor = corSelecionada == opcao.color.toArgb()
                                                    Card(
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSelectedColor) Color(0x1A60A5FA) else Color.Transparent
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelectedColor) Color(0xFF60A5FA) else selectorBorder.copy(alpha = 0.7f)
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                interactionSource = noRippleInteraction,
                                                                indication = null
                                                            ) {
                                                                corSelecionada = opcao.color.toArgb()
                                                                showCorDialog = false
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clip(CircleShape)
                                                                    .background(opcao.color)
                                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                            )
                                                            Text(opcao.name, color = textPrimary)
                                                        }
                                                    }
                                                }
                                            }
                                            SimpleLazyScrollbar(
                                                state = corDialogListState,
                                                itemCount = opcoesCor.size,
                                                trackColor = selectorBorder.copy(alpha = 0.35f),
                                                thumbColor = selectorAccent.copy(alpha = 0.95f),
                                                touchAreaWidth = 34.dp,
                                                barWidth = 4.dp,
                                                minThumbHeight = 24.dp,
                                                modifier = Modifier.align(Alignment.CenterEnd)
                                            )
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = { showCorDialog = false },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isOnboardingVariant) Color.White else selectorAccent
                                            ),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = if (isOnboardingVariant) Color.White else selectorAccent
                                            )
                                        ) {
                                            Text(
                                                text = "Fechar",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val isFreeNameType =
                            tipoSelecionado == TipoVeiculo.BICICLETA ||
                                    tipoSelecionado == TipoVeiculo.BIKE_ELETRICA ||
                                    tipoSelecionado == TipoVeiculo.MOTORHOME
                        val isManualNameMode =
                            isFreeNameType || nomeManualNoCadastro || (hasBrandSelected && consultaModelosConcluida && nomeManualPorFalhaApi)

                        if (isManualNameMode) {
                            val freeNameLabel = when (tipoSelecionado) {
                                TipoVeiculo.BICICLETA, TipoVeiculo.BIKE_ELETRICA -> "Nome da bike"
                                TipoVeiculo.MOTORHOME -> "Nome do motorhome"
                                else -> "Nome do veículo"
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = nome,
                                    onValueChange = { nome = it },
                                    isError = erroNome,
                                    label = { Text(freeNameLabel) },
                                    singleLine = true,
                                    colors = selectorFieldColorsWithState(
                                        filled = nome.isNotBlank(),
                                        isError = erroNome
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = hasBrandSelected && !aguardarBuscaModelos,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                if (!hasBrandSelected || aguardarBuscaModelos) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable(
                                                interactionSource = noRippleInteraction,
                                                indication = null
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    if (aguardarBuscaModelos) "Aguarde carregar os modelos" else "Escolha a marca antes de preencher os outros campos",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    )
                                }
                            }
                        } else {
                            val nomeVeiculoLabel = if (aguardarBuscaModelos) "Carregando nomes...." else "Nome do veículo"
                            val nomeVeiculoPlaceholder = if (aguardarBuscaModelos) "Carregando nomes...." else "Selecione"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = nome,
                                    onValueChange = {},
                                    readOnly = true,
                                    isError = erroNome,
                                label = { Text(nomeVeiculoLabel) },
                                placeholder = { Text(nomeVeiculoPlaceholder) },
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showNomePickerScreen) },
                                colors = selectorFieldColorsWithState(
                                    filled = nome.isNotBlank(),
                                    isError = erroNome
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = noRippleInteraction,
                                            indication = null
                                        ) {
                                            if (aguardarBuscaModelos) {
                                                Toast.makeText(context, "Aguarde carregar os modelos", Toast.LENGTH_SHORT).show()
                                            } else if (hasBrandSelected) {
                                                filtroNomeVeiculo = ""
                                                showNomePickerScreen = true
                                            } else {
                                                Toast.makeText(context, "Escolha a marca antes de preencher os outros campos", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                )
                            }
                        }

                        val motorLabel = if (tipoSelecionado == TipoVeiculo.BICICLETA) "Aro/Modelo" else "Modelo"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = modelo,
                                onValueChange = {
                                    modelo = it
                                    modeloSelecionadoCodigo = modelosFipe.firstOrNull { item ->
                                        normalizarTextoBusca(item.nome) == normalizarTextoBusca(it)
                                    }?.codigo
                                },
                                isError = erroModelo,
                                label = { Text(motorLabel) },
                                singleLine = true,
                                colors = selectorFieldColorsWithState(
                                    filled = modelo.isNotBlank(),
                                    isError = erroModelo
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = hasBrandSelected && !aguardarBuscaModelos,
                                shape = RoundedCornerShape(14.dp)
                            )
                            if (!hasBrandSelected || aguardarBuscaModelos) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = noRippleInteraction,
                                            indication = null
                                        ) {
                                            Toast.makeText(
                                                context,
                                                if (aguardarBuscaModelos) "Aguarde carregar os modelos" else "Escolha a marca antes de preencher os outros campos",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                )
                            }
                        }

                        if (!tipoSemAno) {
                            ExposedDropdownMenuBox(
                                expanded = anoExpanded,
                                onExpandedChange = {
                                    if (!hasBrandSelected) {
                                        Toast.makeText(
                                            context,
                                            "Escolha a marca antes de preencher os outros campos",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (aguardarBuscaModelos) {
                                        // aguarda lista de modelos
                                    } else if (anosFipe.isNotEmpty()) {
                                        anoExpanded = !anoExpanded
                                    }
                                }
                            ) {
                                OutlinedTextField(
                                    value = anoSelecionado,
                                    onValueChange = {
                                        anoSelecionado = it.filter(Char::isDigit).take(4)
                                    },
                                    readOnly = false,
                                    isError = erroAno,
                                    label = { Text("Ano") },
                                    placeholder = { Text("Selecione ou digite") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = selectorFieldColorsWithState(
                                        filled = anoSelecionado.isNotBlank(),
                                        isError = erroAno
                                    ),
                                    enabled = !aguardarBuscaModelos,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = anoExpanded,
                                    onDismissRequest = { anoExpanded = false },
                                    modifier = Modifier.background(selectorDropdownBg)
                                ) {
                                    anosFipe.forEach { anoItem ->
                                        DropdownMenuItem(
                                            text = { Text(anoItem, color = textPrimary) },
                                            onClick = {
                                                anoSelecionado = anoItem
                                                anoExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                    }

                    if (etapaCadastro == 2) {
                        if (!isBikeTypeGlobal) {
                            OutlinedTextField(
                                value = kmAtualStr,
                                onValueChange = { kmAtualStr = formatarKmTextoOnboarding(it) },
                                isError = erroKm,
                                label = { Text("KM Atual (Painel)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = selectorFieldColorsWithState(
                                    filled = kmAtualStr.filter(Char::isDigit).isNotEmpty(),
                                    isError = erroKm
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = quemUsaExpanded,
                            onExpandedChange = { quemUsaExpanded = !quemUsaExpanded }
                        ) {
                            OutlinedTextField(
                                value = if (quemUsaOpcao == "Selecione") "" else quemUsaOpcao,
                                onValueChange = {},
                                readOnly = true,
                                isError = erroQuemUsa,
                                label = { Text(if (isBikeTypeGlobal) "Quem usa essa bike?" else "Quem usa esse veículo?") },
                                placeholder = { Text("Selecione") },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quemUsaExpanded) },
                                colors = selectorFieldColorsWithState(
                                    filled = quemUsaOpcao != "Selecione",
                                    isError = erroQuemUsa
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = quemUsaExpanded,
                                onDismissRequest = { quemUsaExpanded = false },
                                modifier = Modifier.background(selectorDropdownBg)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Eu mesmo", color = textPrimary) },
                                    onClick = {
                                        quemUsaOpcao = "Eu mesmo"
                                        proprietario = nomeUsuarioLogado
                                        quemUsaExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Outra pessoa", color = textPrimary) },
                                    onClick = {
                                        quemUsaOpcao = "Outra pessoa"
                                        if (proprietario == nomeUsuarioLogado || proprietario.equals("Eu mesmo", ignoreCase = true)) {
                                            proprietario = ""
                                        }
                                        quemUsaExpanded = false
                                    }
                                )
                            }
                        }

                        if (quemUsaOpcao == "Outra pessoa") {
                            OutlinedTextField(
                                value = proprietario,
                                onValueChange = { proprietario = it },
                                isError = erroProprietario,
                                label = { Text("Nome da pessoa") },
                                singleLine = true,
                                colors = selectorFieldColorsWithState(
                                    filled = proprietario.isNotBlank(),
                                    isError = erroProprietario
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        if (!isBikeTypeGlobal) {
                            ExposedDropdownMenuBox(
                                expanded = batidasExpanded,
                                onExpandedChange = { batidasExpanded = !batidasExpanded }
                            ) {
                                OutlinedTextField(
                                    value = when (vezesBatido) {
                                        null -> ""
                                        0 -> "Nunca foi batido"
                                        else -> vezesBatido.toString()
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    isError = erroBatidas,
                                    label = { Text("Vezes batido") },
                                    placeholder = { Text("Selecione") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batidasExpanded) },
                                    colors = selectorFieldColorsWithState(
                                        filled = vezesBatido != null,
                                        isError = erroBatidas
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = batidasExpanded,
                                    onDismissRequest = { batidasExpanded = false },
                                    modifier = Modifier.background(selectorDropdownBg)
                                ) {
                                    (0..10).forEach { quantidade ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (quantidade == 0) "Nunca foi batido" else quantidade.toString(),
                                                    color = textPrimary
                                                )
                                            },
                                            onClick = {
                                                vezesBatido = quantidade
                                                batidasExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = tempoExpanded,
                            onExpandedChange = { tempoExpanded = !tempoExpanded }
                        ) {
                            OutlinedTextField(
                                value = tempoComVeiculo,
                                onValueChange = {},
                                readOnly = true,
                                isError = erroTempo,
                                label = { Text(if (isBikeTypeGlobal) "Tempo com a bike" else "Tempo com veículo") },
                                placeholder = { Text("Selecione") },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoExpanded) },
                                colors = selectorFieldColorsWithState(
                                    filled = tempoComVeiculo.isNotBlank(),
                                    isError = erroTempo
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = tempoExpanded,
                                onDismissRequest = { tempoExpanded = false },
                                modifier = Modifier.background(selectorDropdownBg)
                            ) {
                                opcoesTempoComVeiculo().forEach { tempo ->
                                    DropdownMenuItem(
                                        text = { Text(tempo, color = textPrimary) },
                                        onClick = {
                                            tempoComVeiculo = tempo
                                            tempoExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isOnboardingVariant) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        if (etapaCadastro == 1) {
                            Button(
                                onClick = {
                                    tentouAvancarEtapa1 = true
                            if (!tipoSemAno && anoSelecionado.isNotBlank() && !anoValido) {
                                        Toast.makeText(
                                            context,
                                            "Ano inválido. Digite um ano entre 1900 e $anoAtualPermitido.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    if (!etapa1Valida) {
                                        Toast.makeText(context, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    etapaCadastro = 2
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (etapa1Valida) accentBlue else borderLight,
                                    contentColor = if (etapa1Valida) Color.White else textSecondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "Próximo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    tentouSalvarEtapa2 = true
                                    if (!etapa2Valida || tipoSelecionado == null) {
                                        Toast.makeText(context, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    onSalvar(
                                        carroBase.copy(
                                            nome = nome,
                                            marca = marca,
                                            modelo = combinarModeloAno(modelo, anoSelecionado),
                                            proprietario = proprietario,
                                            corArgb = corSelecionada ?: carroBase.corArgb,
                                            kmAtual = kmAtualStr.filter(Char::isDigit).toIntOrNull() ?: 0,
                                            tipoVeiculo = tipoSelecionado!!,
                                            vezesBatido = vezesBatido,
                                            tempoComVeiculo = tempoComVeiculo
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (etapa2Valida) accentBlue else borderLight,
                                    contentColor = if (etapa2Valida) Color.White else textSecondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cadastrar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }

                if (isOnboardingVariant) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp)
                    ) {
                        if (etapaCadastro == 1) {
                            Button(
                                onClick = {
                                    tentouAvancarEtapa1 = true
                            if (!tipoSemAno && anoSelecionado.isNotBlank() && !anoValido) {
                                        Toast.makeText(
                                            context,
                                            "Ano inválido. Digite um ano entre 1900 e $anoAtualPermitido.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    if (!etapa1Valida) {
                                        Toast.makeText(context, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    etapaCadastro = 2
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (etapa1Valida) accentBlue else borderLight,
                                    contentColor = if (etapa1Valida) Color.White else textSecondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "Próximo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    tentouSalvarEtapa2 = true
                        if (!tipoSemAno && anoSelecionado.isNotBlank() && !anoValido) {
                                        Toast.makeText(
                                            context,
                                            "Ano inválido. Digite um ano entre 1900 e $anoAtualPermitido.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    if (!etapa2Valida || tipoSelecionado == null) {
                                        Toast.makeText(context, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    onSalvar(
                                        carroBase.copy(
                                            nome = nome,
                                            marca = marca,
                                            modelo = combinarModeloAno(modelo, anoSelecionado),
                                            proprietario = proprietario,
                                            corArgb = corSelecionada ?: carroBase.corArgb,
                                            kmAtual = kmAtualStr.filter(Char::isDigit).toIntOrNull() ?: 0,
                                            tipoVeiculo = tipoSelecionado!!,
                                            vezesBatido = vezesBatido,
                                            tempoComVeiculo = tempoComVeiculo
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (etapa2Valida) accentBlue else borderLight,
                                    contentColor = if (etapa2Valida) Color.White else textSecondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cadastrar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SimpleLazyScrollbar(
    state: LazyListState,
    itemCount: Int,
    trackColor: Color,
    thumbColor: Color,
    thumbLabel: String = "",
    thumbLabelColor: Color = Color.White,
    touchAreaWidth: Dp = 40.dp,
    barWidth: Dp = 4.dp,
    minThumbHeight: Dp = 24.dp,
    dynamicThumbSize: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (itemCount <= 0) return
    val visibleCount = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    if (itemCount <= visibleCount) return
    val scope = rememberCoroutineScope()
    val maxFirstVisible = (itemCount - visibleCount).coerceAtLeast(0)
    BoxWithConstraints(
        modifier = modifier
            .width(touchAreaWidth)
            .fillMaxHeight()
            .pointerInput(itemCount, visibleCount) {
                var scrollJob: Job? = null
                fun scrollToFraction(fraction: Float): Int {
                    val clamped = fraction.coerceIn(0f, 1f)
                    val targetIndex = if (clamped >= 0.999f) {
                        maxFirstVisible
                    } else {
                        (clamped * maxFirstVisible.toFloat()).toInt().coerceIn(0, maxFirstVisible)
                    }
                    scrollJob?.cancel()
                    scrollJob = scope.launch { state.scrollToItem(targetIndex) }
                    return targetIndex
                }
                awaitEachGesture {
                    val height = size.height.toFloat().coerceAtLeast(1f)
                    var activePointerId: androidx.compose.ui.input.pointer.PointerId? = null
                    var lastTargetIndex = -1
                    while (activePointerId == null) {
                        val downEvent = awaitPointerEvent()
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                        if (downChange != null) {
                            activePointerId = downChange.id
                            val targetIndex = ((downChange.position.y / height).coerceIn(0f, 1f) *
                                    (itemCount - visibleCount).coerceAtLeast(0).toFloat()).toInt()
                            if (targetIndex != lastTargetIndex) {
                                lastTargetIndex = scrollToFraction(downChange.position.y / height)
                            }
                            downChange.consume()
                        }
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == activePointerId } ?: break
                        val targetIndex = ((change.position.y / height).coerceIn(0f, 1f) *
                                (itemCount - visibleCount).coerceAtLeast(0).toFloat()).toInt()
                        if (targetIndex != lastTargetIndex) {
                            lastTargetIndex = scrollToFraction(change.position.y / height)
                        }
                        change.consume()
                        if (!change.pressed) break
                    }
                }
            }
    ) {
        val density = LocalDensity.current
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val adaptiveMinThumbPx = with(density) {
            minThumbHeight.toPx().coerceAtMost(trackHeightPx * 0.70f)
        }
        val thumbPx = if (dynamicThumbSize) {
            (trackHeightPx * (visibleCount.toFloat() / itemCount.toFloat()))
                .coerceIn(adaptiveMinThumbPx, trackHeightPx)
        } else {
            adaptiveMinThumbPx
        }
        val travelPx = (trackHeightPx - thumbPx).coerceAtLeast(0f)

        val progress = when {
            maxFirstVisible <= 0 -> 0f
            !state.canScrollForward -> 1f
            !state.canScrollBackward -> 0f
            else -> {
                val firstVisible = state.firstVisibleItemIndex.toFloat()
                val firstOffsetPx = state.firstVisibleItemScrollOffset.toFloat()
                val avgItemSizePx = state.layoutInfo.visibleItemsInfo
                    .map { it.size }
                    .average()
                    .toFloat()
                    .coerceAtLeast(1f)
                val fractionalIndex = firstVisible + (firstOffsetPx / avgItemSizePx)
                (fractionalIndex / maxFirstVisible.toFloat()).coerceIn(0f, 1f)
            }
        }
        val thumbOffsetPx by animateFloatAsState(
            targetValue = travelPx * progress,
            animationSpec = tween(durationMillis = 90),
            label = "scrollbar_thumb_offset"
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(barWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(barWidth)
                .height(with(density) { thumbPx.toDp() })
                .offset(y = with(density) { thumbOffsetPx.toDp() })
                .clip(RoundedCornerShape(999.dp))
                .background(thumbColor)
        )
        if (thumbLabel.isNotBlank()) {
            val labelSizeDp = 28.dp
            val labelSizePx = with(density) { labelSizeDp.toPx() }
            val labelOffsetPx = (thumbOffsetPx + (thumbPx / 2f) - (labelSizePx / 2f))
                .coerceIn(0f, (trackHeightPx - labelSizePx).coerceAtLeast(0f))
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-22).dp, y = with(density) { labelOffsetPx.toDp() })
                    .size(labelSizeDp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thumbLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NovoHeroCardOnboarding() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Novo veículo", color = scheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}

@Composable
private fun NovoSectionCardOnboarding(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title.isNotBlank() && icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun ColorRowNovoOnboarding(
    selecionada: Int,
    onSelect: (Int) -> Unit,
    textSecondary: Color
) {
    val cores = coresVeiculoDisponiveis()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cores.forEach { opcao ->
            val selecionadaCor = selecionada == opcao.color.toArgb()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(opcao.color)
                        .border(
                            width = if (selecionadaCor) 3.dp else 1.dp,
                            color = if (selecionadaCor) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { onSelect(opcao.color.toArgb()) }
                )
                Text(
                    text = opcao.name,
                    color = textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatarKmTextoOnboarding(texto: String): String {
    val digits = texto.filter(Char::isDigit).take(10)
    if (digits.isEmpty()) return ""
    val value = (digits.toLongOrNull() ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong())
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}

