package br.com.gui.carlembrete

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
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
    var proprietario by remember { mutableStateOf("") }
    var kmAtualStr by remember { mutableStateOf("100.000") }
    var tipoSelecionado by remember { mutableStateOf<TipoVeiculo?>(null) }
    var corSelecionada by remember { mutableStateOf(carroBase.corArgb) }
    var alvoVoz by remember { mutableStateOf("nome") }
    var modelosFipe by remember { mutableStateOf<List<FipeModeloDto>>(emptyList()) }
    var carregandoModelos by remember { mutableStateOf(false) }
    var modeloSelecionadoCodigo by remember { mutableStateOf<Int?>(null) }
    var anosFipe by remember { mutableStateOf<List<String>>(emptyList()) }
    var anoSelecionado by remember { mutableStateOf("") }
    val contentScrollState = rememberScrollState()
    val showTopBar by remember { derivedStateOf { contentScrollState.value <= 8 } }
    val sugestoesNomeExibidas = remember(modelosFipe) { modelosFipe }
    val opcoesCor = remember { coresVeiculoDisponiveis() }
    val nomeCorSelecionada = remember(corSelecionada, opcoesCor) {
        opcoesCor.firstOrNull { it.color.toArgb() == corSelecionada }?.name ?: "Selecione"
    }

    val marcasDisponiveis = marcasPorTipo(tipoSelecionado)
    LaunchedEffect(tipoSelecionado) {
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
        modelosFipe = withContext(Dispatchers.IO) { carregarModelosFipePorMarca(marca, tipoSelecionado) }
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
        anosFipe = withContext(Dispatchers.IO) { carregarAnosFipe(marca, codigoModelo, tipoSelecionado) }
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

    Scaffold(
        containerColor = bgLight,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("Adicionar veiculo", color = textPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgLight)
                )
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
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NovoSectionCard(title = "", icon = null) {
                    var tipoExpanded by remember { mutableStateOf(false) }
                    var marcaExpanded by remember { mutableStateOf(false) }
                    var anoExpanded by remember { mutableStateOf(false) }
                    var nomeExpanded by remember { mutableStateOf(false) }
                    var corExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = tipoExpanded,
                        onExpandedChange = { tipoExpanded = !tipoExpanded }
                    ) {
                        OutlinedTextField(
                            value = tipoSelecionado?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            placeholder = { Text("Selecione") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
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
                                        marcaExpanded = false
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
                                if (!carregandoModelos && modelosFipe.isNotEmpty()) {
                                    nomeExpanded = !nomeExpanded
                                } else {
                                    val mensagem = if (carregandoModelos) {
                                        "Aguarde, buscando opções..."
                                    } else {
                                        "Selecione tipo e marca para carregar opções"
                                    }
                                    Toast.makeText(context, mensagem, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            OutlinedTextField(
                                value = nome,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Nome do veiculo") },
                                placeholder = { Text("Selecione") },
                                singleLine = true,
                                trailingIcon = if (modelosFipe.isNotEmpty()) {
                                    { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nomeExpanded) }
                                } else null,
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
                                enabled = !carregandoModelos && modelosFipe.isNotEmpty()
                            )
                            ExposedDropdownMenu(expanded = nomeExpanded, onDismissRequest = { nomeExpanded = false }) {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
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
                        label = { Text(motorLabel) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = ::iniciarCapturaVozMotor) {
                                Icon(Icons.Default.Mic, contentDescription = "Falar motor")
                            }
                        },
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

                    ExposedDropdownMenuBox(
                        expanded = corExpanded,
                        onExpandedChange = { corExpanded = !corExpanded }
                    ) {
                        OutlinedTextField(
                            value = nomeCorSelecionada,
                            onValueChange = {},
                            readOnly = true,
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
                    val labelQuemUsa = if (
                        tipoSelecionado == TipoVeiculo.BICICLETA ||
                        tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
                    ) {
                        "Quem usa essa bike?"
                    } else {
                        "Quem usa esse veiculo?"
                    }
                    OutlinedTextField(
                        value = proprietario,
                        onValueChange = { proprietario = it },
                        label = { Text(labelQuemUsa) },
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

                    OutlinedTextField(
                        value = kmAtualStr,
                        onValueChange = { kmAtualStr = formatarKmTextoLocal(it) },
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

                Button(
                    onClick = {
                        if (tipoSelecionado == null) {
                            Toast.makeText(context, "Selecione o tipo de veiculo", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSalvar(
                            carroBase.copy(
                                nome = nome,
                                marca = marca,
                                modelo = combinarModeloAno(modelo, anoSelecionado),
                                proprietario = proprietario,
                                corArgb = corSelecionada,
                                kmAtual = kmAtualStr.filter(Char::isDigit).toIntOrNull() ?: 0,
                                tipoVeiculo = tipoSelecionado!!
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val textoBotao = if (
                        tipoSelecionado == TipoVeiculo.BICICLETA ||
                        tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
                    ) {
                        "Adicionar bike"
                    } else {
                        "Cadastrar Veículo"
                    }
                    Text(textoBotao, color = Color.White, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

private suspend fun carregarModelosFipePorMarca(
    marcaSelecionada: String,
    tipoVeiculo: TipoVeiculo?
): List<FipeModeloDto> {
    val tipoFipe = tipoFipePara(tipoVeiculo) ?: return emptyList()
    return runCatching {
        val marcas = fipeApi.listarMarcas(tipoFipe)
        val codigoMarca = encontrarCodigoMarcaFipe(marcaSelecionada, marcas) ?: return emptyList()
        val modelos = fipeApi.listarModelos(tipoFipe, codigoMarca).modelos
            .map { it.copy(nome = it.nome.trim()) }
            .filter { it.nome.isNotEmpty() }
            .distinct()
        filtrarModelosPorTipo(tipoVeiculo, modelos)
    }.getOrDefault(emptyList())
}

private suspend fun carregarAnosFipe(
    marcaSelecionada: String,
    codigoModelo: Int,
    tipoVeiculo: TipoVeiculo?
): List<String> {
    val tipoFipe = tipoFipePara(tipoVeiculo) ?: return emptyList()
    return runCatching {
        val marcas = fipeApi.listarMarcas(tipoFipe)
        val codigoMarca = encontrarCodigoMarcaFipe(marcaSelecionada, marcas) ?: return emptyList()
        fipeApi.listarAnos(tipoFipe, codigoMarca, codigoModelo)
            .mapNotNull { item ->
                Regex("\\b(19\\d{2}|20\\d{2})\\b").find(item.nome)?.value
            }
            .distinct()
            .sortedDescending()
    }.getOrDefault(emptyList())
}

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
