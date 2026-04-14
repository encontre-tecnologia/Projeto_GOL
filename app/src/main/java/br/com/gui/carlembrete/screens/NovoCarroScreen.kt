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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.NumberFormat
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoCarroScreen(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit
) {
    NovoCarroScreenContent(
        onDismiss = onDismiss,
        onSalvar = onSalvar,
        allowBackNavigation = true,
        isOnboardingVariant = false
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun NovoCarroScreenContent(
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
    val bgLight = if (isOnboardingVariant) Color(0xFF0B1320) else if (isDark) Color.Black else scheme.background
    val borderLight = if (isOnboardingVariant) Color(0xFF334155) else scheme.outlineVariant
    val textPrimary = if (isOnboardingVariant) Color(0xFFF8FAFC) else scheme.onBackground
    val textSecondary = if (isOnboardingVariant) Color(0xFF94A3B8) else scheme.onSurfaceVariant
    val accentBlue = if (isOnboardingVariant) Color(0xFF60A5FA) else scheme.primary
    val selectorTextPrimary = if (isOnboardingVariant) Color(0xFFF8FAFC) else scheme.onSurface
    val selectorTextSecondary = if (isOnboardingVariant) Color(0xFF94A3B8) else scheme.onSurfaceVariant
    val selectorAccent = if (isOnboardingVariant) Color(0xFF60A5FA) else scheme.primary
    val selectorBorder = if (isOnboardingVariant) Color(0xFF334155) else scheme.outlineVariant
    val selectorDropdownBg = if (isOnboardingVariant) Color(0xFF1E293B) else if (isDark) Color(0xFF111827) else scheme.surface
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
        focusedBorderColor = selectorAccent,
        unfocusedBorderColor = selectorBorder,
        disabledBorderColor = selectorBorder,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )
    val cardBg = if (isOnboardingVariant) Color(0xFF111827) else if (isDark) Color(0xFF111827) else scheme.surface
    val carroBase = CarroInfo(nome = "", modelo = "")

    var nome by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var proprietario by remember { mutableStateOf(nomeUsuarioLogado) }
    var quemUsaOpcao by remember { mutableStateOf("Eu mesmo") }
    var quemUsaExpanded by remember { mutableStateOf(false) }
    var kmAtualStr by remember { mutableStateOf("0") }
    var tipoSelecionado by remember {
        mutableStateOf<TipoVeiculo?>(null)
    }
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
    var tipoEscolhidoManualmente by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()
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
        } ?: "Selecione"
    }
    val isBikeTypeGlobal =
        tipoSelecionado == TipoVeiculo.BICICLETA || tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    val tipoSemAno =
        tipoSelecionado == TipoVeiculo.BICICLETA ||
            tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    val anoObrigatorio = !tipoSemAno && anosFipe.isNotEmpty()
    val etapa1Valida = tipoSelecionado != null &&
        marca.isNotBlank() &&
        nome.isNotBlank() &&
        modelo.isNotBlank() &&
        (!anoObrigatorio || anoSelecionado.isNotBlank()) &&
        corSelecionada != null
    val hasTypeSelected = tipoSelecionado != null
    val etapaBikeValida = etapa1Valida &&
        proprietario.isNotBlank()
    val erroTipo = etapaCadastro == 1 && tentouAvancarEtapa1 && tipoSelecionado == null
    val erroMarca = etapaCadastro == 1 && tentouAvancarEtapa1 && marca.isBlank()
    val erroNome = etapaCadastro == 1 && tentouAvancarEtapa1 && nome.isBlank()
    val erroModelo = etapaCadastro == 1 && tentouAvancarEtapa1 && modelo.isBlank()
    val erroAno = etapaCadastro == 1 && !tipoSemAno && tentouAvancarEtapa1 && anoObrigatorio && anoSelecionado.isBlank()
    val erroCor = etapaCadastro == 1 && tentouAvancarEtapa1 && corSelecionada == null
    val erroKm = etapaCadastro == 2 && !isBikeTypeGlobal && tentouSalvarEtapa2 && kmAtualStr.filter(Char::isDigit).isEmpty()
    val etapa2Valida = proprietario.isNotBlank() &&
        (isBikeTypeGlobal || vezesBatido != null) &&
        tempoComVeiculo.isNotBlank() &&
        (isBikeTypeGlobal || kmAtualStr.filter(Char::isDigit).isNotEmpty())
    val erroProprietario =
        (etapaCadastro == 2 && !isBikeTypeGlobal && tentouSalvarEtapa2 && proprietario.isBlank()) ||
        (etapaCadastro == 1 && isBikeTypeGlobal && tentouAvancarEtapa1 && proprietario.isBlank())
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
            proprietario = nomeUsuarioLogado
            quemUsaOpcao = "Eu mesmo"
            quemUsaExpanded = false
            kmAtualStr = "0"
            corSelecionada = null
            vezesBatido = null
            tempoComVeiculo = ""
            modelosFipe = emptyList()
            carregandoModelos = false
            modeloSelecionadoCodigo = null
            anosFipe = emptyList()
            anoSelecionado = ""
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
            modeloSelecionadoCodigo = null
            anosFipe = emptyList()
            anoSelecionado = ""
        }
    }
    LaunchedEffect(marca, tipoSelecionado) {
        if (marca.isBlank() || tipoSelecionado == null) {
            modelosFipe = emptyList()
            carregandoModelos = false
            modeloSelecionadoCodigo = null
            anosFipe = emptyList()
            anoSelecionado = ""
            return@LaunchedEffect
        }
        carregandoModelos = true
        modelosFipe = withContext(Dispatchers.IO) { carregarModelosFipePorMarca(context, marca, tipoSelecionado) }
        carregandoModelos = false
        modeloSelecionadoCodigo = modelosFipe.firstOrNull { normalizarTextoBusca(it.nome) == normalizarTextoBusca(modelo) }?.codigo
    }
    LaunchedEffect(modeloSelecionadoCodigo, marca) {
        val codigoModelo = modeloSelecionadoCodigo
        if (marca.isBlank() || codigoModelo == null) {
            anosFipe = emptyList()
            anoSelecionado = ""
            return@LaunchedEffect
        }
        anosFipe = withContext(Dispatchers.IO) { carregarAnosFipe(context, marca, codigoModelo, tipoSelecionado) }
        if (anosFipe.isEmpty()) {
            anoSelecionado = ""
        }
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

    fun iniciarCapturaVozApelido() {
        alvoVoz = "nome"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o nome do veiculo")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voz indisponivel neste dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun iniciarCapturaVozMotor() {
        alvoVoz = "motor"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o motor do veiculo")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voz indisponivel neste dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun voltarTela() {
        if (etapaCadastro == 2) {
            etapaCadastro = 1
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = allowBackNavigation) {
        if (allowBackNavigation) {
            voltarTela()
        }
    }

    Scaffold(
        containerColor = bgLight,
        topBar = {
            if (!isOnboardingVariant) {
                CenterAlignedTopAppBar(
                    title = { Text(trNow("Adicione seus veículos", "Add your vehicles"), color = textPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = if (allowBackNavigation) {
                        {
                            IconButton(onClick = ::voltarTela) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = trNow("Voltar", "Back"), tint = textPrimary)
                            }
                        }
                    } else {
                        {}
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgLight)
                )
            }
        },
        bottomBar = {
            if (!isOnboardingVariant) {
                Surface(color = bgLight, tonalElevation = 0.dp, shadowElevation = 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 34.dp)
                    ) {
                        if (etapaCadastro == 1) {
                            Button(
                                onClick = {
                                    tentouAvancarEtapa1 = true
                                    if (isBikeTypeGlobal) {
                                        if (!etapaBikeValida || tipoSelecionado == null) {
                                            Toast.makeText(context, trNow("Preencha os campos obrigatorios", "Fill in required fields"), Toast.LENGTH_SHORT).show()
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
                                                vezesBatido = null,
                                                tempoComVeiculo = ""
                                            )
                                        )
                                        return@Button
                                    }
                                    if (!etapa1Valida) {
                                        Toast.makeText(context, trNow("Preencha os campos obrigatorios", "Fill in required fields"), Toast.LENGTH_SHORT).show()
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (isBikeTypeGlobal) "Cadastrar bike" else "Proximo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    tentouSalvarEtapa2 = true
                                    if (!etapa2Valida || tipoSelecionado == null) {
                                        Toast.makeText(context, trNow("Preencha os campos obrigatorios", "Fill in required fields"), Toast.LENGTH_SHORT).show()
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(trNow("Cadastrar", "Register"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgLight)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = if (isOnboardingVariant) 20.dp else 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isOnboardingVariant) {
                    Text(
                        text = "Cadastre um Veículo",
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    OnboardingGarageHeader(
                        selectedType = tipoSelecionado,
                        hasUserSelection = tipoEscolhidoManualmente,
                        onSelectType = {
                            tipoSelecionado = it
                            tipoEscolhidoManualmente = true
                        }
                    )
                } else {
                    OnboardingGarageHeader(
                        selectedType = tipoSelecionado,
                        hasUserSelection = tipoEscolhidoManualmente,
                        onSelectType = {
                            tipoSelecionado = it
                            tipoEscolhidoManualmente = true
                        }
                    )
                }
                NovoSectionCard(
                    title = "",
                    icon = null,
                    containerColor = cardBg,
                    borderColor = borderLight,
                    modifier = if (isOnboardingVariant) Modifier.offset(y = (-52).dp) else Modifier
                ) {
                    var marcaExpanded by remember { mutableStateOf(false) }
                    var anoExpanded by remember { mutableStateOf(false) }
                    var showNomeDialog by remember { mutableStateOf(false) }
                    var corExpanded by remember { mutableStateOf(false) }
                    var batidasExpanded by remember { mutableStateOf(false) }
                    var tempoExpanded by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBikeTypeGlobal) "Etapa 1 de 1" else "Etapa $etapaCadastro de 2",
                            color = textSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (etapaCadastro == 1) {
                    ExposedDropdownMenuBox(
                        expanded = marcaExpanded,
                        onExpandedChange = {
                            if (hasTypeSelected) marcaExpanded = !marcaExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = marca,
                            onValueChange = {},
                            readOnly = true,
                            isError = erroMarca,
                            label = { Text("Marca") },
                            placeholder = { Text("Selecione") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = marcaExpanded) },
                            enabled = hasTypeSelected,
                            colors = selectorFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = marcaExpanded,
                            onDismissRequest = { marcaExpanded = false },
                            modifier = Modifier.background(selectorDropdownBg)
                        ) {
                            marcasDisponiveis.forEach { marcaNome ->
                                DropdownMenuItem(
                                    text = { Text(marcaNome, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary) },
                                    onClick = {
                                        marca = marcaNome
                                        carregandoModelos = true
                                        modelosFipe = emptyList()
                                        nome = ""
                                        modeloSelecionadoCodigo = null
                                        anosFipe = emptyList()
                                        anoSelecionado = ""
                                        filtroNomeVeiculo = ""
                                        marcaExpanded = false
                                    }
                                    )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = corExpanded,
                        onExpandedChange = {
                            if (hasTypeSelected) corExpanded = !corExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = nomeCorSelecionada,
                            onValueChange = {},
                            readOnly = true,
                            isError = erroCor,
                            label = { Text("Cor do veiculo") },
                            placeholder = { Text("Selecione") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = corExpanded) },
                            colors = selectorFieldColors,
                            enabled = hasTypeSelected
                        )
                        ExposedDropdownMenu(
                            expanded = corExpanded,
                            onDismissRequest = { corExpanded = false },
                            modifier = Modifier.background(selectorDropdownBg)
                        ) {
                            opcoesCor.forEach { opcao ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(opcao.color)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            )
                                            Text(opcao.name, color = textPrimary)
                                        }
                                    },
                                    onClick = {
                                        corSelecionada = opcao.color.toArgb()
                                        corExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    val isFreeNameType =
                        tipoSelecionado == TipoVeiculo.BICICLETA ||
                            tipoSelecionado == TipoVeiculo.BIKE_ELETRICA ||
                            tipoSelecionado == TipoVeiculo.MOTORHOME
                    if (isFreeNameType) {
                        val freeNameLabel = when (tipoSelecionado) {
                            TipoVeiculo.BICICLETA, TipoVeiculo.BIKE_ELETRICA -> "Nome da bike"
                            TipoVeiculo.MOTORHOME -> "Nome do motorhome"
                            else -> "Nome do veiculo"
                        }
                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            isError = erroNome,
                            label = { Text(freeNameLabel) },
                            singleLine = true,
                            colors = selectorFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasTypeSelected
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!hasTypeSelected) {
                                        Toast.makeText(context, "Selecione o tipo de veículo primeiro", Toast.LENGTH_SHORT).show()
                                        } else if (marca.isNotBlank()) {
                                            filtroNomeVeiculo = ""
                                            showNomeDialog = true
                                        } else {
                                            Toast.makeText(context, "Selecione a marca primeiro", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            OutlinedTextField(
                                value = nome,
                                onValueChange = {},
                                readOnly = true,
                                isError = erroNome,
                                label = { Text("Nome do veiculo") },
                                placeholder = { Text("Selecione") },
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showNomeDialog) },
                                colors = selectorFieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            )
                        }

                        if (showNomeDialog) {
                            Dialog(
                                onDismissRequest = { showNomeDialog = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = selectorDropdownBg)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Selecione o nome do veiculo",
                                            color = textPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(
                                            color = selectorBorder.copy(alpha = 0.45f),
                                            thickness = 1.dp
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = filtroNomeVeiculo,
                                            onValueChange = { filtroNomeVeiculo = it },
                                            label = { Text("Buscar veículo") },
                                            placeholder = { Text("Ex.: Gol, Civic, Strada...") },
                                            singleLine = true,
                                            colors = selectorFieldColors,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        if (carregandoModelos) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 18.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator()
                                                Spacer(Modifier.height(10.dp))
                                                Text("Carregando...", color = textSecondary)
                                            }
                                        } else if (sugestoesNomeFiltradas.isEmpty()) {
                                            Text(
                                                text = "Nenhum veículo encontrado para a busca",
                                                color = textSecondary,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 360.dp)
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                sugestoesNomeFiltradas.forEach { modeloItem ->
                                                    TextButton(
                                                        onClick = {
                                                            val (nomeExtraido, modeloExtraido) = separarNomeEMotorModelo(
                                                                descricaoCompleta = modeloItem.nome,
                                                                marcaSelecionada = marca
                                                            )
                                                            nome = nomeExtraido
                                                            if (modeloExtraido.isNotBlank()) {
                                                                modelo = modeloExtraido
                                                            }
                                                            modeloSelecionadoCodigo = modeloItem.codigo
                                                            showNomeDialog = false
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = modeloItem.nome,
                                                            color = textPrimary,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Start
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = { showNomeDialog = false },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(8.dp),
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
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val motorLabel = if (tipoSelecionado == TipoVeiculo.BICICLETA) "Aro/Modelo" else "Motor/Modelo"
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

                        colors = selectorFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasTypeSelected
                    )

                    if (!tipoSemAno) {
                        ExposedDropdownMenuBox(
                            expanded = anoExpanded,
                            onExpandedChange = {
                                if (anosFipe.isNotEmpty()) {
                                    anoExpanded = !anoExpanded
                                }
                            }
                        ) {
                            OutlinedTextField(
                                value = anoSelecionado,
                                onValueChange = { anoSelecionado = it.filter(Char::isDigit).take(4) },
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = anoExpanded) },
                                colors = selectorFieldColors
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

                    if (isBikeTypeGlobal) {
                        ExposedDropdownMenuBox(
                            expanded = quemUsaExpanded,
                            onExpandedChange = { quemUsaExpanded = !quemUsaExpanded }
                        ) {
                            OutlinedTextField(
                                value = quemUsaOpcao,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Quem usa essa bike?") },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quemUsaExpanded) },
                                colors = selectorFieldColors
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
                                colors = selectorFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                    }

                    }
                    if (etapaCadastro == 2 && !isBikeTypeGlobal) {
                        OutlinedTextField(
                            value = kmAtualStr,
                            onValueChange = { kmAtualStr = formatarKmTextoLocal(it) },
                            isError = erroKm,
                            label = { Text("KM Atual (Painel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = selectorFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = quemUsaExpanded,
                            onExpandedChange = { quemUsaExpanded = !quemUsaExpanded }
                        ) {
                            OutlinedTextField(
                                value = quemUsaOpcao,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Quem usa esse veiculo?") },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quemUsaExpanded) },
                                colors = selectorFieldColors
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
                                colors = selectorFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!isBikeTypeGlobal) {
                            ExposedDropdownMenuBox(
                                expanded = batidasExpanded,
                                onExpandedChange = { batidasExpanded = !batidasExpanded }
                            ) {
                                OutlinedTextField(
                                    value = vezesBatido?.toString() ?: "N\u00E3o informado",
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
                                    colors = selectorFieldColors
                                )
                                ExposedDropdownMenu(
                                    expanded = batidasExpanded,
                                    onDismissRequest = { batidasExpanded = false },
                                    modifier = Modifier.background(selectorDropdownBg)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("N\u00E3o informado", color = textPrimary, maxLines = 1) },
                                        onClick = {
                                            vezesBatido = null
                                            batidasExpanded = false
                                        }
                                    )
                                    (0..10).forEach { quantidade ->
                                        DropdownMenuItem(
                                            text = { Text(quantidade.toString(), color = textPrimary) },
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
                            value = tempoComVeiculo.ifBlank { "N\u00E3o informado" },
                            onValueChange = {},
                            readOnly = true,
                            isError = erroTempo,
                            label = { Text(if (isBikeTypeGlobal) "Tempo com a bike" else "Tempo com veiculo") },
                            placeholder = { Text("Selecione") },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoExpanded) },
                            colors = selectorFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = tempoExpanded,
                            onDismissRequest = { tempoExpanded = false },
                            modifier = Modifier.background(selectorDropdownBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("N\u00E3o informado", color = textPrimary, maxLines = 1) },
                                onClick = {
                                    tempoComVeiculo = ""
                                    tempoExpanded = false
                                }
                            )
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

                if (isOnboardingVariant) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-48).dp)
                    ) {
                        if (etapaCadastro == 1) {
                            Button(
                                onClick = {
                                    tentouAvancarEtapa1 = true
                                    if (isBikeTypeGlobal) {
                                        if (!etapaBikeValida || tipoSelecionado == null) {
                                            Toast.makeText(context, trNow("Preencha os campos obrigatorios", "Fill in required fields"), Toast.LENGTH_SHORT).show()
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
                                                vezesBatido = null,
                                                tempoComVeiculo = ""
                                            )
                                        )
                                        return@Button
                                    }
                                    if (!etapa1Valida) {
                                        Toast.makeText(context, trNow("Preencha os campos obrigatorios", "Fill in required fields"), Toast.LENGTH_SHORT).show()
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (isBikeTypeGlobal) "Cadastrar bike" else "Proximo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    tentouSalvarEtapa2 = true
                                    if (!etapa2Valida || tipoSelecionado == null) {
                                        Toast.makeText(context, trNow("Preencha os campos obrigatorios", "Fill in required fields"), Toast.LENGTH_SHORT).show()
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(trNow("Cadastrar", "Register"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }

            }
        }
    }

}

@Composable
internal fun OnboardingGarageHeader(
    selectedType: TipoVeiculo?,
    hasUserSelection: Boolean,
    onSelectType: (TipoVeiculo) -> Unit
) {
    val tipos = remember { TipoVeiculo.values().toList() }
    val typeCount = tipos.size
    val virtualCount = remember(typeCount) { if (typeCount == 0) 0 else typeCount * 400 }
    val middleBlock = remember(typeCount, virtualCount) {
        if (typeCount == 0) 0 else (virtualCount / 2 / typeCount) * typeCount
    }
    val selectedTypeIndex = remember(selectedType) {
        val idx = tipos.indexOf(selectedType)
        if (idx >= 0) idx else tipos.indexOf(TipoVeiculo.CARRO).coerceAtLeast(0)
    }
    val initialVirtualIndex = remember(selectedTypeIndex, middleBlock) { middleBlock + selectedTypeIndex }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialVirtualIndex)
    var initialCentered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val layoutInfo = listState.layoutInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val centeredVirtualIndex = layoutInfo.visibleItemsInfo
        .minByOrNull { info -> abs((info.offset + info.size / 2f) - viewportCenter) }
        ?.index ?: initialVirtualIndex
    val centeredTypeIndex = if (typeCount == 0) 0 else centeredVirtualIndex.mod(typeCount)
    val selectedLabel = selectedType?.label ?: "Nenhum"

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (typeCount == 0 || virtualCount == 0) return@LaunchedEffect
        val minSafe = typeCount * 2
        val maxSafe = virtualCount - (typeCount * 2)
        val currentIndex = listState.firstVisibleItemIndex
        if (currentIndex < minSafe || currentIndex > maxSafe) {
            val normalized = currentIndex.mod(typeCount)
            val recenteredIndex = middleBlock + normalized
            listState.scrollToItem(recenteredIndex, listState.firstVisibleItemScrollOffset)
        }
    }

    LaunchedEffect(layoutInfo.visibleItemsInfo.size, initialVirtualIndex) {
        if (initialCentered || typeCount == 0 || virtualCount == 0) return@LaunchedEffect
        val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == initialVirtualIndex }
        if (targetItem != null) {
            val viewportCenterPx =
                (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2f
            val itemCenterPx = targetItem.offset + (targetItem.size / 2f)
            val delta = itemCenterPx - viewportCenterPx
            if (abs(delta) > 1f) {
                listState.scrollBy(delta)
            }
            initialCentered = true
        }
    }

    fun moveSelection(delta: Int) {
        if (typeCount == 0 || virtualCount == 0 || delta == 0) return
        val nextVirtualIndex = centeredVirtualIndex + delta
        val nextTypeIndex = nextVirtualIndex.mod(typeCount)
        onSelectType(tipos[nextTypeIndex])
        val targetVirtualIndex = (middleBlock + nextTypeIndex).coerceIn(0, virtualCount - 1)
        val keepOffset = listState.firstVisibleItemScrollOffset
        scope.launch {
            listState.animateScrollToItem(targetVirtualIndex, keepOffset)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(286.dp)
    ) {
        val itemSpacing = 14.dp
        val horizontalPadding = 22.dp
        val slotWidth =
            ((maxWidth - (horizontalPadding * 2) - (itemSpacing * 2)) / 3f).coerceAtLeast(106.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(top = 44.dp)
            ) {
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    items(count = virtualCount) { index ->
                        val typeIndex = if (typeCount == 0) 0 else index.mod(typeCount)
                        val tipo = tipos[typeIndex]
                        val linearDistance = abs(typeIndex - centeredTypeIndex)
                        val distance = minOf(linearDistance, typeCount - linearDistance)
                        val visualFocus = 1f - (distance * 0.2f).coerceAtMost(0.5f)
                        val isCenter = distance == 0
                        val scale by animateFloatAsState(
                            targetValue = if (isCenter) 1.08f else visualFocus,
                            animationSpec = tween(200),
                            label = "carousel_scale"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isCenter) 1f else (0.5f + visualFocus * 0.28f),
                            animationSpec = tween(200),
                            label = "carousel_alpha"
                        )
                        val isSelected = hasUserSelection && selectedType == tipo
                        val cardSize by animateDpAsState(
                            targetValue = if (isCenter) slotWidth * 0.88f else slotWidth * 0.74f,
                            animationSpec = tween(200),
                            label = "carousel_card_size"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier
                                .width(slotWidth)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                    translationY = 0f
                                }
                                .clickable { onSelectType(tipo) }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0x2686EFAC) else Color(0xFF0F172A),
                                border = BorderStroke(
                                    width = if (isSelected) 1.7.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF86EFAC) else Color(0xFF334155)
                                ),
                                modifier = Modifier.size(cardSize)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    VehicleIcon(
                                        tipoVeiculo = tipo,
                                        tint = Color(0xFFE2E8F0),
                                        size = if (isCenter) 58.dp else 48.dp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selecionado",
                                            tint = Color(0xFF86EFAC),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(18.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = tipo.label,
                                color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = if (isCenter || isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xCC0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.CenterStart)
                        .offset(y = (-18).dp)
                        .zIndex(2f)
                ) {
                    IconButton(
                        modifier = Modifier.fillMaxSize(),
                        onClick = { moveSelection(-1) }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Tipo anterior",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xCC0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.CenterEnd)
                        .offset(y = (-18).dp)
                        .zIndex(2f)
                ) {
                    IconButton(
                        modifier = Modifier.fillMaxSize(),
                        onClick = { moveSelection(1) }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Próximo tipo",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF86EFAC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Seleção única: $selectedLabel",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

    }
}

@Composable
internal fun NovoHeroCard() {
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
        Text("Novo veiculo", color = scheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}

@Composable
internal fun NovoSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
internal fun ColorRowNovo(
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

internal fun formatarKmTextoLocal(texto: String): String {
    val digits = texto.filter(Char::isDigit).take(10)
    if (digits.isEmpty()) return ""
    val value = (digits.toLongOrNull() ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong())
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}

internal data class CorVeiculoOption(
    val name: String,
    val color: Color
)

internal fun opcoesTempoComVeiculo(): List<String> = listOf(
    "Menos de 6 meses",
    "6 meses a 1 ano",
    "1 a 2 anos",
    "2 a 3 anos",
    "3 a 5 anos",
    "Mais de 5 anos"
)

internal fun coresVeiculoDisponiveis(): List<CorVeiculoOption> = listOf(
    CorVeiculoOption("Branco", Color(0xFFFFFFFF)),
    CorVeiculoOption("Preto", Color(0xFF0F172A)),
    CorVeiculoOption("Prata", Color(0xFFC0C0C0)),
    CorVeiculoOption("Cinza", Color(0xFF9CA3AF)),
    CorVeiculoOption("Vermelho", Color(0xFFDC2626)),
    CorVeiculoOption("Azul", Color(0xFF4F7DBE)),
    CorVeiculoOption("Marrom", Color(0xFF7C3F00)),
    CorVeiculoOption("Bege", Color(0xFFE7D7C1)),
    CorVeiculoOption("Verde", Color(0xFF16A34A)),
    CorVeiculoOption("Amarelo", Color(0xFFFACC15)),
    CorVeiculoOption("Laranja", Color(0xFFF97316)),
    CorVeiculoOption("Roxo", Color(0xFF6D5BD0)),
    CorVeiculoOption("Rosa", Color(0xFFEC4899)),
    CorVeiculoOption("Dourado", Color(0xFFC0841A)),
    CorVeiculoOption("Bordo", Color(0xFF7F1D1D)),
    CorVeiculoOption("Turquesa", Color(0xFF38BDF8)),
    CorVeiculoOption("Creme", Color(0xFFF5F5DC))
)

internal data class FipeMarcaDto(
    val codigo: String,
    val nome: String
)

internal data class FipeModeloDto(
    val codigo: Int,
    val nome: String
)

internal data class FipeAnoDto(
    val codigo: String,
    val nome: String
)

internal data class FipeModelosResponseDto(
    val modelos: List<FipeModeloDto> = emptyList()
)

internal interface FipeApi {
    @GET("api/v1/{tipo}/marcas")
    suspend fun listarMarcas(@Path("tipo") tipo: String): List<FipeMarcaDto>

    @GET("api/v1/{tipo}/marcas/{codigo}/modelos")
    suspend fun listarModelos(
        @Path("tipo") tipo: String,
        @Path("codigo") codigo: String
    ): FipeModelosResponseDto

    @GET("api/v1/{tipo}/marcas/{codigoMarca}/modelos/{codigoModelo}/anos")
    suspend fun listarAnos(
        @Path("tipo") tipo: String,
        @Path("codigoMarca") codigoMarca: String,
        @Path("codigoModelo") codigoModelo: Int
    ): List<FipeAnoDto>
}

internal data class FipeNamedDto(
    @SerializedName(value = "codigo", alternate = ["code"])
    val codigo: String? = null,
    @SerializedName(value = "nome", alternate = ["name"])
    val nome: String? = null
)

internal data class FipeModelosResponseV2Dto(
    @SerializedName("modelos")
    val modelosPt: List<FipeNamedDto>? = null,
    @SerializedName("models")
    val modelosEn: List<FipeNamedDto>? = null
)

internal interface FipeApiReservaV2 {
    @GET("{vehicleType}/brands")
    suspend fun listarMarcas(@Path("vehicleType") vehicleType: String): List<FipeNamedDto>

    @GET("{vehicleType}/brands/{brandId}/models")
    suspend fun listarModelos(
        @Path("vehicleType") vehicleType: String,
        @Path("brandId") brandId: String
    ): FipeModelosResponseV2Dto

    @GET("{vehicleType}/brands/{brandId}/models/{modelId}/years")
    suspend fun listarAnos(
        @Path("vehicleType") vehicleType: String,
        @Path("brandId") brandId: String,
        @Path("modelId") modelId: Int
    ): List<FipeNamedDto>
}

internal val fipeApi: FipeApi by lazy {
    Retrofit.Builder()
        .baseUrl(BuildConfig.FIPE_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FipeApi::class.java)
}

internal val fipeApiReservaV2: FipeApiReservaV2 by lazy {
    Retrofit.Builder()
        .baseUrl("https://parallelum.com.br/fipe/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FipeApiReservaV2::class.java)
}

internal const val FIPE_CACHE_MODELOS_TTL_MS = 7L * 24L * 60L * 60L * 1000L
internal const val FIPE_CACHE_ANOS_TTL_MS = 7L * 24L * 60L * 60L * 1000L
internal const val FIPE_RETRY_TIMEOUT_MS = 3500L
private const val TAG_FIPE_RETRY = "FipeRetry"

internal suspend fun carregarModelosFipePorMarca(
    context: Context,
    marcaSelecionada: String,
    tipoVeiculo: TipoVeiculo?
): List<FipeModeloDto> {
    val tipoFipe = tipoFipePara(tipoVeiculo) ?: return emptyList()
    val cacheKey = "modelos_${gerarCacheKeyFipe(tipoFipe, marcaSelecionada)}"

    AppPreferences.getFipeCache(context, cacheKey, FIPE_CACHE_MODELOS_TTL_MS)?.let { cached ->
        decodeModelosCache(cached).takeIf { it.isNotEmpty() }?.let { return it }
    }

    val tentativa = runCatching {
        withFipeRetry {
            val marcas = fipeApi.listarMarcas(tipoFipe)
            val codigoMarca = encontrarCodigoMarcaFipe(marcaSelecionada, marcas)
            Log.d(
                TAG_FIPE_RETRY,
                "modelos consulta primaria tipo='$tipoFipe' marca='$marcaSelecionada' marcas=${marcas.size} codigoMarca=${codigoMarca ?: "null"}"
            )
            if (codigoMarca == null) return@withFipeRetry emptyList()
            val modelos = fipeApi.listarModelos(tipoFipe, codigoMarca).modelos
                .map { it.copy(nome = it.nome.trim()) }
                .filter { it.nome.isNotEmpty() }
                .distinct()
            filtrarModelosPorTipo(tipoVeiculo, modelos)
        }
    }

    val resultado = tentativa.getOrDefault(emptyList())

    if (resultado.isEmpty()) {
        val reserva = runCatching {
            withFipeRetry {
                carregarModelosFipeReservaPorMarca(marcaSelecionada, tipoVeiculo)
            }
        }.onFailure { erro ->
            Log.w(
                TAG_FIPE_RETRY,
                "API reserva modelos falhou marca='$marcaSelecionada': ${erro::class.simpleName}"
            )
        }.getOrDefault(emptyList())
        if (reserva.isNotEmpty()) {
            Log.w(
                TAG_FIPE_RETRY,
                "usando API reserva de modelos para marca='$marcaSelecionada' (motivo=${if (tentativa.exceptionOrNull() != null) "erro_primaria" else "lista_vazia_primaria"})"
            )
            AppPreferences.putFipeCache(context, cacheKey, encodeModelosCache(reserva))
            return reserva
        }
        Log.w(
            TAG_FIPE_RETRY,
            "modelos vazios para marca='$marcaSelecionada' (primaria+reserva)"
        )
        AppPreferences.getFipeCacheAnyAge(context, cacheKey)?.let { stale ->
            decodeModelosCache(stale).takeIf { it.isNotEmpty() }?.let {
                Log.w(TAG_FIPE_RETRY, "usando cache antigo de modelos para marca='$marcaSelecionada'")
                return it
            }
        }
    }

    if (resultado.isNotEmpty()) {
        AppPreferences.putFipeCache(context, cacheKey, encodeModelosCache(resultado))
    }
    return resultado
}

internal suspend fun carregarMarcasFipePorTipo(
    context: Context,
    tipoVeiculo: TipoVeiculo?
): List<String> {
    val tipoFipe = tipoFipePara(tipoVeiculo) ?: return emptyList()
    val cacheKey = "marcas_${gerarCacheKeyFipe(tipoFipe)}"

    AppPreferences.getFipeCache(context, cacheKey, FIPE_CACHE_MODELOS_TTL_MS)?.let { cached ->
        decodeAnosCache(cached).takeIf { it.isNotEmpty() }?.let { return it }
    }

    val tentativa = runCatching {
        withFipeRetry {
            fipeApi.listarMarcas(tipoFipe)
                .map { it.nome.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }

    val resultado = tentativa.getOrDefault(emptyList())

    if (resultado.isEmpty() && tentativa.exceptionOrNull() != null) {
        val reserva = runCatching {
            withFipeRetry {
                carregarMarcasFipeReservaPorTipo(tipoVeiculo)
            }
        }.getOrDefault(emptyList())
        if (reserva.isNotEmpty()) {
            Log.w(TAG_FIPE_RETRY, "usando API reserva de marcas para tipo='$tipoFipe'")
            AppPreferences.putFipeCache(context, cacheKey, encodeAnosCache(reserva))
            return reserva
        }
        AppPreferences.getFipeCacheAnyAge(context, cacheKey)?.let { stale ->
            decodeAnosCache(stale).takeIf { it.isNotEmpty() }?.let {
                Log.w(TAG_FIPE_RETRY, "usando cache antigo de marcas para tipo='$tipoFipe'")
                return it
            }
        }
    }

    if (resultado.isNotEmpty()) {
        AppPreferences.putFipeCache(context, cacheKey, encodeAnosCache(resultado))
    }
    return resultado
}

internal suspend fun carregarAnosFipe(
    context: Context,
    marcaSelecionada: String,
    codigoModelo: Int,
    tipoVeiculo: TipoVeiculo?
): List<String> {
    val tipoFipe = tipoFipePara(tipoVeiculo) ?: return emptyList()
    val cacheKey = "anos_${gerarCacheKeyFipe(tipoFipe, marcaSelecionada, codigoModelo.toString())}"

    AppPreferences.getFipeCache(context, cacheKey, FIPE_CACHE_ANOS_TTL_MS)?.let { cached ->
        decodeAnosCache(cached).takeIf { it.isNotEmpty() }?.let { return it }
    }

    val tentativa = runCatching {
        withFipeRetry {
            val marcas = fipeApi.listarMarcas(tipoFipe)
            val codigoMarca = encontrarCodigoMarcaFipe(marcaSelecionada, marcas) ?: return@withFipeRetry emptyList()
            fipeApi.listarAnos(tipoFipe, codigoMarca, codigoModelo)
                .mapNotNull { item ->
                    Regex("\\b(19\\d{2}|20\\d{2})\\b").find(item.nome)?.value
                }
                .distinct()
                .sortedDescending()
        }
    }

    val resultado = tentativa.getOrDefault(emptyList())

    if (resultado.isEmpty() && tentativa.exceptionOrNull() != null) {
        val reserva = runCatching {
            withFipeRetry {
                carregarAnosFipeReserva(marcaSelecionada, codigoModelo, tipoVeiculo)
            }
        }.getOrDefault(emptyList())
        if (reserva.isNotEmpty()) {
            Log.w(
                TAG_FIPE_RETRY,
                "usando API reserva de anos marca='$marcaSelecionada' modelo='$codigoModelo'"
            )
            AppPreferences.putFipeCache(context, cacheKey, encodeAnosCache(reserva))
            return reserva
        }
        AppPreferences.getFipeCacheAnyAge(context, cacheKey)?.let { stale ->
            decodeAnosCache(stale).takeIf { it.isNotEmpty() }?.let {
                Log.w(
                    TAG_FIPE_RETRY,
                    "usando cache antigo de anos marca='$marcaSelecionada' modelo='$codigoModelo'"
                )
                return it
            }
        }
    }

    if (resultado.isNotEmpty()) {
        AppPreferences.putFipeCache(context, cacheKey, encodeAnosCache(resultado))
    }
    return resultado
}

internal suspend fun <T> withFipeRetry(block: suspend () -> T): T {
    var lastError: Throwable? = null
    val delays = listOf(0L, 250L)
    for ((attemptIndex, waitMs) in delays.withIndex()) {
        try {
            if (waitMs > 0) delay(waitMs)
            return withTimeout(FIPE_RETRY_TIMEOUT_MS) { block() }
        } catch (e: Throwable) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            lastError = e
            val erro = e::class.simpleName ?: "Erro"
            Log.w(TAG_FIPE_RETRY, "tentativa=${attemptIndex + 1}/${delays.size} falhou: $erro")
        }
    }
    throw (lastError ?: IllegalStateException("Erro desconhecido em consulta FIPE"))
}

internal suspend fun carregarModelosFipeReservaPorMarca(
    marcaSelecionada: String,
    tipoVeiculo: TipoVeiculo?
): List<FipeModeloDto> {
    val tipo = tipoFipeReservaPara(tipoVeiculo) ?: return emptyList()
    val marcas = fipeApiReservaV2.listarMarcas(tipo)
    val codigoMarca = encontrarCodigoMarcaFipeReserva(marcaSelecionada, marcas) ?: return emptyList()
    val modelosRaw = fipeApiReservaV2.listarModelos(tipo, codigoMarca)
    val modelos = (modelosRaw.modelosPt ?: modelosRaw.modelosEn ?: emptyList())
        .mapNotNull { item ->
            val codigo = item.codigo?.trim()?.toIntOrNull() ?: return@mapNotNull null
            val nome = item.nome?.trim().orEmpty()
            if (nome.isBlank()) null else FipeModeloDto(codigo = codigo, nome = nome)
        }
        .distinct()
    return filtrarModelosPorTipo(tipoVeiculo, modelos)
}

internal suspend fun carregarMarcasFipeReservaPorTipo(tipoVeiculo: TipoVeiculo?): List<String> {
    val tipo = tipoFipeReservaPara(tipoVeiculo) ?: return emptyList()
    return fipeApiReservaV2.listarMarcas(tipo)
        .mapNotNull { it.nome?.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

internal suspend fun carregarAnosFipeReserva(
    marcaSelecionada: String,
    codigoModelo: Int,
    tipoVeiculo: TipoVeiculo?
): List<String> {
    val tipo = tipoFipeReservaPara(tipoVeiculo) ?: return emptyList()
    val marcas = fipeApiReservaV2.listarMarcas(tipo)
    val codigoMarca = encontrarCodigoMarcaFipeReserva(marcaSelecionada, marcas) ?: return emptyList()
    return fipeApiReservaV2.listarAnos(tipo, codigoMarca, codigoModelo)
        .mapNotNull { item ->
            Regex("\\b(19\\d{2}|20\\d{2})\\b").find(item.nome.orEmpty())?.value
        }
        .distinct()
        .sortedDescending()
}

internal fun gerarCacheKeyFipe(vararg partes: String): String {
    val raw = partes.joinToString("|") { normalizarTextoBusca(it) }
    return raw.hashCode().toUInt().toString()
}

internal fun encodeModelosCache(modelos: List<FipeModeloDto>): String =
    modelos.joinToString("||") { "${it.codigo}::${it.nome.replace("||", " ").replace("::", " ")}" }

internal fun decodeModelosCache(data: String): List<FipeModeloDto> =
    data.split("||")
        .mapNotNull { token ->
            val parts = token.split("::", limit = 2)
            val codigo = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val nome = parts.getOrNull(1)?.trim().orEmpty()
            if (nome.isBlank()) null else FipeModeloDto(codigo, nome)
        }

internal fun encodeAnosCache(anos: List<String>): String = anos.joinToString("|")

internal fun decodeAnosCache(data: String): List<String> =
    data.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinct()

internal fun encontrarCodigoMarcaFipe(marcaSelecionada: String, marcasFipe: List<FipeMarcaDto>): String? {
    val alvos = variacoesMarcaBusca(marcaSelecionada)
    if (alvos.isEmpty()) return null
    marcasFipe.firstOrNull { item ->
        val nome = normalizarTextoBusca(item.nome)
        alvos.any { it == nome }
    }?.let { return it.codigo }
    marcasFipe.firstOrNull { item ->
        val nome = normalizarTextoBusca(item.nome)
        alvos.any { alvo -> nome.contains(alvo) || alvo.contains(nome) }
    }?.let { return it.codigo }
    return null
}

internal fun normalizarTextoBusca(texto: String): String =
    Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^A-Za-z0-9 ]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .uppercase(Locale.ROOT)
        .trim()

internal fun tipoFipePara(tipo: TipoVeiculo?): String? = when (tipo) {
    TipoVeiculo.MOTO -> "motos"
    TipoVeiculo.CAMINHAO, TipoVeiculo.ONIBUS -> "caminhoes"
    TipoVeiculo.CARRO,
    TipoVeiculo.HATCH,
    TipoVeiculo.SUV,
    TipoVeiculo.CAMINHONETE,
    TipoVeiculo.FURGAO,
    TipoVeiculo.VAN,
    TipoVeiculo.VEICULO_ELETRICO -> "carros"
    else -> null
}

internal fun tipoFipeReservaPara(tipo: TipoVeiculo?): String? = when (tipo) {
    TipoVeiculo.MOTO -> "motorcycles"
    TipoVeiculo.CAMINHAO, TipoVeiculo.ONIBUS -> "trucks"
    TipoVeiculo.CARRO,
    TipoVeiculo.HATCH,
    TipoVeiculo.SUV,
    TipoVeiculo.CAMINHONETE,
    TipoVeiculo.FURGAO,
    TipoVeiculo.VAN,
    TipoVeiculo.VEICULO_ELETRICO -> "cars"
    else -> null
}

internal fun encontrarCodigoMarcaFipeReserva(
    marcaSelecionada: String,
    marcas: List<FipeNamedDto>
): String? {
    val alvos = variacoesMarcaBusca(marcaSelecionada)
    if (alvos.isEmpty()) return null
    marcas.firstOrNull {
        val nome = normalizarTextoBusca(it.nome.orEmpty())
        alvos.any { alvo -> nome == alvo }
    }?.codigo?.let { return it }
    marcas.firstOrNull {
        val nome = normalizarTextoBusca(it.nome.orEmpty())
        alvos.any { alvo -> nome.contains(alvo) || alvo.contains(nome) }
    }?.codigo?.let { return it }
    return null
}

internal fun variacoesMarcaBusca(marcaSelecionada: String): Set<String> {
    val alvo = normalizarTextoBusca(marcaSelecionada)
    if (alvo.isBlank()) return emptySet()
    val aliases = when (alvo) {
        "VOLKSWAGEN" -> setOf("VW")
        "CHEVROLET" -> setOf("GM")
        "MERCEDES BENZ" -> setOf("MERCEDES", "MB")
        "LAND ROVER" -> setOf("LANDROVER")
        "CITROEN" -> setOf("CITROEN")
        "PEUGEOT" -> setOf("PEUGEOT")
        else -> emptySet()
    }
    return linkedSetOf(alvo).apply { addAll(aliases) }
}

internal fun filtrarModelosPorTipo(
    tipo: TipoVeiculo?,
    modelos: List<FipeModeloDto>
): List<FipeModeloDto> {
    if (tipo == null) return modelos
    val filtrados = when (tipo) {
        TipoVeiculo.CARRO -> modelos
        TipoVeiculo.HATCH -> modelos.filterHatch()
        TipoVeiculo.SUV -> modelos.filterNomeContem("SUV")
        TipoVeiculo.CAMINHONETE -> modelos.filterNomeContem("PICK", "PICKUP", "PICK-UP", "CABINE")
        TipoVeiculo.FURGAO -> modelos.filterNomeContem("FURGAO", "FURGON", "CARGO", "BAU")
        TipoVeiculo.VAN -> modelos.filterNomeContem("VAN", "MINIBUS", "PASSAGEIRO")
        TipoVeiculo.VEICULO_ELETRICO -> modelos.filterNomeContem("ELETR", "EV", "E-TECH")
        TipoVeiculo.ONIBUS -> modelos.filterNomeContem("ONIBUS", "BUS")
        TipoVeiculo.CAMINHAO -> modelos.filterNomeContem("CAMINHAO", "TRUCK", "CARGO", "WORKER")
        else -> modelos
    }
    return if (filtrados.isNotEmpty()) filtrados else modelos
}

internal fun List<FipeModeloDto>.filterNomeContem(vararg termos: String): List<FipeModeloDto> {
    if (termos.isEmpty()) return this
    return filter { modelo ->
        val nome = normalizarTextoBusca(modelo.nome)
        termos.any { termo -> nome.contains(normalizarTextoBusca(termo)) }
    }
}

internal fun List<FipeModeloDto>.filterHatch(): List<FipeModeloDto> {
    val inclusoesFortes = listOf(
        "GOL", "HB20", "FIESTA", "ONIX", "PALIO", "UNO", "POLO", "ARGO", "MOBI",
        "FOX", "KA", "208", "207", "C3", "SANDERO", "CLIO", "CELTA", "CORSA",
        "YARIS", "MARCH", "FIT", "UP", "ETIOS", "A1", "A3", "SERIE 1", "COOPER"
    )
    val exclusoes = listOf(
        "SUV", "PICK", "PICKUP", "PICK-UP", "CAMINHAO", "TRUCK", "VAN", "MINIBUS",
        "FURGAO", "FURGON", "CARGO", "SEDAN", "COUPE", "CONVERSIVEL", "CABINE"
    )

    val porNomeConhecido = filter { modelo ->
        val nome = normalizarTextoBusca(modelo.nome)
        inclusoesFortes.any { nome.contains(normalizarTextoBusca(it)) }
    }
    if (porNomeConhecido.isNotEmpty()) return porNomeConhecido

    val porExclusao = filter { modelo ->
        val nome = normalizarTextoBusca(modelo.nome)
        exclusoes.none { nome.contains(normalizarTextoBusca(it)) }
    }
    return if (porExclusao.isNotEmpty()) porExclusao else this
}

internal fun combinarModeloAno(modelo: String, ano: String): String {
    val modeloLimpo = modelo.trim()
    val anoLimpo = ano.trim()
    if (anoLimpo.isBlank()) return modeloLimpo
    if (Regex("\\b${Regex.escape(anoLimpo)}\\b").containsMatchIn(modeloLimpo)) return modeloLimpo
    return listOf(modeloLimpo, anoLimpo).filter { it.isNotBlank() }.joinToString(" ").trim()
}

internal fun separarNomeEMotorModelo(
    descricaoCompleta: String,
    marcaSelecionada: String
): Pair<String, String> {
    var texto = descricaoCompleta.trim().replace("\\s+".toRegex(), " ")
    if (texto.isBlank()) return "" to ""

    val marcaNorm = normalizarTextoBusca(marcaSelecionada)
    if (marcaNorm.isNotBlank()) {
        val tokens = texto.split(" ").toMutableList()
        while (tokens.isNotEmpty()) {
            val tokenNorm = normalizarTextoBusca(tokens.first())
            if (tokenNorm == marcaNorm || marcaNorm.contains(tokenNorm) || tokenNorm.contains(marcaNorm)) {
                tokens.removeAt(0)
            } else {
                break
            }
        }
        texto = tokens.joinToString(" ").trim().ifBlank { descricaoCompleta.trim() }
    }

    val tokens = texto.split(" ").filter { it.isNotBlank() }
    if (tokens.isEmpty()) return descricaoCompleta.trim() to ""

    val regexTecnico = Regex(
        pattern = "^(\\d|\\d[\\d,.]*L?|\\d{2,4}CC|\\d{1,2}V|\\dP|FLEX|GAS|GASOLINA|DIESEL|ALCOOL|HIBRID|ELETR|TURBO|AUT|AUTOMATICO|MEC|MANUAL|CVT|AT|MT|TIPTRONIC|TSI|MPI|TDI|CDI|VVT|16V|8V)$",
        option = RegexOption.IGNORE_CASE
    )
    val indiceTecnico = tokens.indexOfFirst { token ->
        val limpo = token.uppercase(Locale.ROOT).replace("[^A-Z0-9,.]".toRegex(), "")
        regexTecnico.matches(limpo) || limpo.matches(Regex("^\\d+[.,]?\\d*$"))
    }

    if (indiceTecnico <= 0) {
        return tokens.joinToString(" ") to ""
    }

    val nome = tokens.take(indiceTecnico).joinToString(" ").trim()
    val resto = tokens.drop(indiceTecnico).joinToString(" ").trim()
    return nome to resto
}








