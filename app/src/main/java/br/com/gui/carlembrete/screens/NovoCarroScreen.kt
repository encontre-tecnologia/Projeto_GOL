package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.AddCircle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.Normalizer
import java.util.Locale
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoCarroScreen(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit
) {
    val context = LocalContext.current
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
    val bgLight = scheme.background
    val borderLight = scheme.outlineVariant
    val textPrimary = scheme.onBackground
    val textSecondary = scheme.onSurfaceVariant
    val accentBlue = scheme.primary
    val carroBase = CarroInfo(nome = "", modelo = "")

    var nome by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var proprietario by remember { mutableStateOf(nomeUsuarioLogado) }
    var quemUsaOpcao by remember { mutableStateOf("Eu mesmo") }
    var quemUsaExpanded by remember { mutableStateOf(false) }
    var kmAtualStr by remember { mutableStateOf("100.000") }
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
    val contentScrollState = rememberScrollState()
    val showTopBar by remember { derivedStateOf { contentScrollState.value <= 8 } }
    val sugestoesNomeExibidas = remember(modelosFipe) { modelosFipe }
    val opcoesCor = remember { coresVeiculoDisponiveis() }
    val nomeCorSelecionada = remember(corSelecionada, opcoesCor) {
        corSelecionada?.let { cor ->
            opcoesCor.firstOrNull { it.color.toArgb() == cor }?.name
        } ?: "Selecione"
    }
    val etapa1Valida = tipoSelecionado != null &&
        marca.isNotBlank() &&
        nome.isNotBlank() &&
        modelo.isNotBlank() &&
        corSelecionada != null
    val isBikeTypeGlobal =
        tipoSelecionado == TipoVeiculo.BICICLETA || tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    val etapaBikeValida = etapa1Valida &&
        proprietario.isNotBlank() &&
        kmAtualStr.filter(Char::isDigit).isNotEmpty()
    val erroTipo = etapaCadastro == 1 && tentouAvancarEtapa1 && tipoSelecionado == null
    val erroMarca = etapaCadastro == 1 && tentouAvancarEtapa1 && marca.isBlank()
    val erroNome = etapaCadastro == 1 && tentouAvancarEtapa1 && nome.isBlank()
    val erroModelo = etapaCadastro == 1 && tentouAvancarEtapa1 && modelo.isBlank()
    val erroCor = etapaCadastro == 1 && tentouAvancarEtapa1 && corSelecionada == null
    val erroKm =
        (etapaCadastro == 2 && !isBikeTypeGlobal && tentouSalvarEtapa2 && kmAtualStr.filter(Char::isDigit).isEmpty()) ||
        (etapaCadastro == 1 && isBikeTypeGlobal && tentouAvancarEtapa1 && kmAtualStr.filter(Char::isDigit).isEmpty())
    val etapa2Valida = proprietario.isNotBlank() &&
        vezesBatido != null &&
        tempoComVeiculo.isNotBlank() &&
        kmAtualStr.filter(Char::isDigit).isNotEmpty()
    val erroProprietario =
        (etapaCadastro == 2 && !isBikeTypeGlobal && tentouSalvarEtapa2 && proprietario.isBlank()) ||
        (etapaCadastro == 1 && isBikeTypeGlobal && tentouAvancarEtapa1 && proprietario.isBlank())
    val erroBatidas = etapaCadastro == 2 && tentouSalvarEtapa2 && vezesBatido == null
    val erroTempo = etapaCadastro == 2 && tentouSalvarEtapa2 && tempoComVeiculo.isBlank()

    val marcasDisponiveis = marcasPorTipo(tipoSelecionado)
    LaunchedEffect(tipoSelecionado) {
        if (tipoAnterior != null && tipoSelecionado != tipoAnterior) {
            marca = ""
            nome = ""
            modelo = ""
            proprietario = nomeUsuarioLogado
            quemUsaOpcao = "Eu mesmo"
            quemUsaExpanded = false
            kmAtualStr = "100.000"
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
        } else if (anoSelecionado !in anosFipe) {
            anoSelecionado = anosFipe.first()
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

    BackHandler {
        voltarTela()
    }

    Scaffold(
        containerColor = bgLight,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("Adicionar veiculo", color = textPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = ::voltarTela) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgLight)
                )
            }
        },
        bottomBar = {
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
                                        Toast.makeText(context, "Preencha os campos obrigatorios", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, "Preencha os campos obrigatorios", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, "Preencha os campos obrigatorios", Toast.LENGTH_SHORT).show()
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
                            Text("Cadastrar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NovoSectionCard(title = "", icon = null) {
                    var tipoExpanded by remember { mutableStateOf(false) }
                    var marcaExpanded by remember { mutableStateOf(false) }
                    var anoExpanded by remember { mutableStateOf(false) }
                    var nomeExpanded by remember { mutableStateOf(false) }
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
                            expanded = tipoExpanded,
                            onExpandedChange = { tipoExpanded = !tipoExpanded }
                        ) {
                            OutlinedTextField(
                                value = tipoSelecionado?.label ?: "",
                                onValueChange = {},
                                readOnly = true,
                                isError = erroTipo,
                                label = { Text("Tipo") },
                                placeholder = { Text("Selecione") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedPlaceholderColor = textSecondary,
                                    unfocusedPlaceholderColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                )
                            )
                            ExposedDropdownMenu(expanded = tipoExpanded, onDismissRequest = { tipoExpanded = false }) {
                                TipoVeiculo.values().forEach { tipo ->
                                    DropdownMenuItem(
                                        text = { Text(tipo.label, color = textPrimary) },
                                        onClick = {
                                            tipoSelecionado = tipo
                                            tipoExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                    ExposedDropdownMenuBox(
                        expanded = marcaExpanded,
                        onExpandedChange = {
                            if (tipoSelecionado != null) {
                                marcaExpanded = !marcaExpanded
                            } else {
                                Toast.makeText(context, "Selecione o tipo primeiro", Toast.LENGTH_SHORT).show()
                            }
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
                            enabled = tipoSelecionado != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedPlaceholderColor = textSecondary,
                                unfocusedPlaceholderColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            )
                        )
                        ExposedDropdownMenu(expanded = marcaExpanded, onDismissRequest = { marcaExpanded = false }) {
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
                                        marcaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = corExpanded,
                        onExpandedChange = { corExpanded = !corExpanded }
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedPlaceholderColor = textSecondary,
                                unfocusedPlaceholderColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            )
                        )
                        ExposedDropdownMenu(expanded = corExpanded, onDismissRequest = { corExpanded = false }) {
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

                    val isBikeType =
                        tipoSelecionado == TipoVeiculo.BICICLETA || tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
                    if (isBikeType) {
                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            isError = erroNome,
                            label = { Text("Nome da bike") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = nomeExpanded,
                            onExpandedChange = {
                                if (tipoSelecionado != null && marca.isNotBlank()) {
                                    nomeExpanded = !nomeExpanded
                                } else {
                                    Toast.makeText(context, "Selecione tipo e marca primeiro", Toast.LENGTH_SHORT).show()
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nomeExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                enabled = tipoSelecionado != null && marca.isNotBlank()
                            )
                            ExposedDropdownMenu(expanded = nomeExpanded, onDismissRequest = { nomeExpanded = false }) {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    if (carregandoModelos) {
                                        DropdownMenuItem(
                                            text = { Text("Buscando...", color = textSecondary) },
                                            onClick = {}
                                        )
                                    } else if (sugestoesNomeExibidas.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Nenhum veÃ­culo encontrado", color = textSecondary) },
                                            onClick = {}
                                        )
                                    } else {
                                        sugestoesNomeExibidas.forEach { modeloItem ->
                                            DropdownMenuItem(
                                                text = { Text(modeloItem.nome, color = textPrimary) },
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
                                                    nomeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!isBikeType && carregandoModelos) {
                        Text(
                            text = "Buscando...",
                            modifier = Modifier.fillMaxWidth(),
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
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

                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedLabelColor = textSecondary,
                            unfocusedLabelColor = textSecondary,
                            focusedBorderColor = accentBlue,
                            unfocusedBorderColor = borderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                )
                            )
                            ExposedDropdownMenu(expanded = quemUsaExpanded, onDismissRequest = { quemUsaExpanded = false }) {
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = kmAtualStr,
                            onValueChange = { kmAtualStr = formatarKmTextoLocal(it) },
                            isError = erroKm,
                            label = { Text("KM Atual (Painel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (anosFipe.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = anoExpanded,
                            onExpandedChange = { anoExpanded = !anoExpanded }
                        ) {
                            OutlinedTextField(
                                value = anoSelecionado,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ano") },
                                placeholder = { Text("Selecione") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = anoExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedPlaceholderColor = textSecondary,
                                    unfocusedPlaceholderColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                )
                            )
                            ExposedDropdownMenu(expanded = anoExpanded, onDismissRequest = { anoExpanded = false }) {
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
                    if (etapaCadastro == 2 && !isBikeTypeGlobal) {
                        OutlinedTextField(
                            value = kmAtualStr,
                            onValueChange = { kmAtualStr = formatarKmTextoLocal(it) },
                            isError = erroKm,
                            label = { Text("KM Atual (Painel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (etapaCadastro == 2 && !isBikeTypeGlobal) {
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                )
                            )
                            ExposedDropdownMenu(expanded = quemUsaExpanded, onDismissRequest = { quemUsaExpanded = false }) {
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedLabelColor = textSecondary,
                                    unfocusedLabelColor = textSecondary,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = borderLight
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = batidasExpanded,
                            onExpandedChange = { batidasExpanded = !batidasExpanded }
                        ) {
                        OutlinedTextField(
                            value = vezesBatido?.toString() ?: "Não informado",
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedPlaceholderColor = textSecondary,
                                unfocusedPlaceholderColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            )
                        )
                        ExposedDropdownMenu(expanded = batidasExpanded, onDismissRequest = { batidasExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Não informado", color = textPrimary) },
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

                        ExposedDropdownMenuBox(
                        expanded = tempoExpanded,
                        onExpandedChange = { tempoExpanded = !tempoExpanded }
                    ) {
                        OutlinedTextField(
                            value = tempoComVeiculo.ifBlank { "Não informado" },
                            onValueChange = {},
                            readOnly = true,
                            isError = erroTempo,
                            label = { Text("Tempo com veiculo") },
                            placeholder = { Text("Selecione") },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedPlaceholderColor = textSecondary,
                                unfocusedPlaceholderColor = textSecondary,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = borderLight
                            )
                        )
                        ExposedDropdownMenu(expanded = tempoExpanded, onDismissRequest = { tempoExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Não informado", color = textPrimary) },
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

            }
        }
    }

}

@Composable
private fun NovoHeroCard() {
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
private fun NovoSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant)
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
private fun ColorRowNovo(
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

private fun formatarKmTextoLocal(texto: String): String {
    val digits = texto.filter(Char::isDigit)
    val value = digits.toLongOrNull() ?: 0L
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}

private data class CorVeiculoOption(
    val name: String,
    val color: Color
)

private fun opcoesTempoComVeiculo(): List<String> = listOf(
    "Menos de 6 meses",
    "6 meses a 1 ano",
    "1 a 2 anos",
    "2 a 3 anos",
    "3 a 5 anos",
    "Mais de 5 anos"
)

private fun coresVeiculoDisponiveis(): List<CorVeiculoOption> = listOf(
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

private data class FipeMarcaDto(
    val codigo: String,
    val nome: String
)

private data class FipeModeloDto(
    val codigo: Int,
    val nome: String
)

private data class FipeAnoDto(
    val codigo: String,
    val nome: String
)

private data class FipeModelosResponseDto(
    val modelos: List<FipeModeloDto> = emptyList()
)

private interface FipeApi {
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

private val fipeApi: FipeApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://parallelum.com.br/fipe/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FipeApi::class.java)
}

private const val FIPE_CACHE_MODELOS_TTL_MS = 7L * 24L * 60L * 60L * 1000L
private const val FIPE_CACHE_ANOS_TTL_MS = 7L * 24L * 60L * 60L * 1000L

private suspend fun carregarModelosFipePorMarca(
    context: Context,
    marcaSelecionada: String,
    tipoVeiculo: TipoVeiculo?
): List<FipeModeloDto> {
    val tipoFipe = tipoFipePara(tipoVeiculo) ?: return emptyList()
    val cacheKey = "modelos_${gerarCacheKeyFipe(tipoFipe, marcaSelecionada)}"

    AppPreferences.getFipeCache(context, cacheKey, FIPE_CACHE_MODELOS_TTL_MS)?.let { cached ->
        decodeModelosCache(cached).takeIf { it.isNotEmpty() }?.let { return it }
    }

    val resultado = runCatching {
        withFipeRetry {
            val marcas = fipeApi.listarMarcas(tipoFipe)
            val codigoMarca = encontrarCodigoMarcaFipe(marcaSelecionada, marcas) ?: return@withFipeRetry emptyList()
            val modelos = fipeApi.listarModelos(tipoFipe, codigoMarca).modelos
                .map { it.copy(nome = it.nome.trim()) }
                .filter { it.nome.isNotEmpty() }
                .distinct()
            filtrarModelosPorTipo(tipoVeiculo, modelos)
        }
    }.getOrDefault(emptyList())

    if (resultado.isNotEmpty()) {
        AppPreferences.putFipeCache(context, cacheKey, encodeModelosCache(resultado))
    }
    return resultado
}

private suspend fun carregarAnosFipe(
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

    val resultado = runCatching {
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
    }.getOrDefault(emptyList())

    if (resultado.isNotEmpty()) {
        AppPreferences.putFipeCache(context, cacheKey, encodeAnosCache(resultado))
    }
    return resultado
}

private suspend fun <T> withFipeRetry(block: suspend () -> T): T {
    var lastError: Throwable? = null
    val delays = listOf(0L, 350L, 900L)
    for (waitMs in delays) {
        try {
            if (waitMs > 0) delay(waitMs)
            return block()
        } catch (e: Throwable) {
            lastError = e
        }
    }
    throw (lastError ?: IllegalStateException("Erro desconhecido em consulta FIPE"))
}

private fun gerarCacheKeyFipe(vararg partes: String): String {
    val raw = partes.joinToString("|") { normalizarTextoBusca(it) }
    return raw.hashCode().toUInt().toString()
}

private fun encodeModelosCache(modelos: List<FipeModeloDto>): String =
    modelos.joinToString("||") { "${it.codigo}::${it.nome.replace("||", " ").replace("::", " ")}" }

private fun decodeModelosCache(data: String): List<FipeModeloDto> =
    data.split("||")
        .mapNotNull { token ->
            val parts = token.split("::", limit = 2)
            val codigo = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val nome = parts.getOrNull(1)?.trim().orEmpty()
            if (nome.isBlank()) null else FipeModeloDto(codigo, nome)
        }

private fun encodeAnosCache(anos: List<String>): String = anos.joinToString("|")

private fun decodeAnosCache(data: String): List<String> =
    data.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinct()

private fun encontrarCodigoMarcaFipe(marcaSelecionada: String, marcasFipe: List<FipeMarcaDto>): String? {
    val alvo = normalizarTextoBusca(marcaSelecionada)
    if (alvo.isBlank()) return null
    marcasFipe.firstOrNull { normalizarTextoBusca(it.nome) == alvo }?.let { return it.codigo }
    marcasFipe.firstOrNull { normalizarTextoBusca(it.nome).contains(alvo) || alvo.contains(normalizarTextoBusca(it.nome)) }?.let { return it.codigo }
    return null
}

private fun normalizarTextoBusca(texto: String): String =
    Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^A-Za-z0-9 ]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .uppercase(Locale.ROOT)
        .trim()

private fun tipoFipePara(tipo: TipoVeiculo?): String? = when (tipo) {
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

private fun filtrarModelosPorTipo(
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

private fun List<FipeModeloDto>.filterNomeContem(vararg termos: String): List<FipeModeloDto> {
    if (termos.isEmpty()) return this
    return filter { modelo ->
        val nome = normalizarTextoBusca(modelo.nome)
        termos.any { termo -> nome.contains(normalizarTextoBusca(termo)) }
    }
}

private fun List<FipeModeloDto>.filterHatch(): List<FipeModeloDto> {
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

private fun combinarModeloAno(modelo: String, ano: String): String {
    val modeloLimpo = modelo.trim()
    val anoLimpo = ano.trim()
    if (anoLimpo.isBlank()) return modeloLimpo
    if (Regex("\\b${Regex.escape(anoLimpo)}\\b").containsMatchIn(modeloLimpo)) return modeloLimpo
    return listOf(modeloLimpo, anoLimpo).filter { it.isNotBlank() }.joinToString(" ").trim()
}

private fun separarNomeEMotorModelo(
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
