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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.gui.carlembrete.VehicleIcon
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditarCarroScreen(
    carroAtual: CarroInfo,
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit,
    onExcluir: () -> Unit
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
    val titleColor = scheme.onBackground
    val textSecondary = scheme.onSurfaceVariant
    val accentBlue = scheme.primary
    val sectionBorder = scheme.outlineVariant
    val bgLight = scheme.background
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = titleColor,
        unfocusedTextColor = titleColor,
        focusedLabelColor = textSecondary,
        unfocusedLabelColor = textSecondary,
        focusedBorderColor = accentBlue,
        unfocusedBorderColor = sectionBorder,
        cursorColor = titleColor,
        focusedLeadingIconColor = textSecondary,
        unfocusedLeadingIconColor = textSecondary,
        focusedTrailingIconColor = textSecondary,
        unfocusedTrailingIconColor = textSecondary
    )

    var nome by remember { mutableStateOf(carroAtual.nome) }
    var marca by remember { mutableStateOf(carroAtual.marca) }
    var modelo by remember { mutableStateOf(carroAtual.modelo) }
    var proprietario by remember {
        mutableStateOf(
            if (carroAtual.proprietario.equals("Eu mesmo", ignoreCase = true)) {
                nomeUsuarioLogado
            } else {
                carroAtual.proprietario
            }
        )
    }
    var quemUsaOpcao by remember {
        mutableStateOf(
            if (
                carroAtual.proprietario.equals("Eu mesmo", ignoreCase = true) ||
                carroAtual.proprietario.equals(nomeUsuarioLogado, ignoreCase = true)
            ) {
                "Eu mesmo"
            } else {
                "Outra pessoa"
            }
        )
    }
    var quemUsaExpanded by remember { mutableStateOf(false) }
    var kmAtualStr by remember { mutableStateOf(if (carroAtual.kmAtual > 0) formatarKmLocal(carroAtual.kmAtual) else "") }
    val tipoSelecionado = carroAtual.tipoVeiculo
    var corSelecionada by remember { mutableStateOf(carroAtual.corArgb) }
    var corExpanded by remember { mutableStateOf(false) }
    val opcoesCor = remember {
        listOf(
            "Branco" to Color(0xFFFFFFFF),
            "Preto" to Color(0xFF0F172A),
            "Prata" to Color(0xFFC0C0C0),
            "Cinza" to Color(0xFF9CA3AF),
            "Vermelho" to Color(0xFFDC2626),
            "Azul" to Color(0xFF4F7DBE),
            "Marrom" to Color(0xFF7C3F00),
            "Bege" to Color(0xFFE7D7C1),
            "Verde" to Color(0xFF16A34A),
            "Amarelo" to Color(0xFFFACC15),
            "Laranja" to Color(0xFFF97316),
            "Roxo" to Color(0xFF6D5BD0),
            "Rosa" to Color(0xFFEC4899),
            "Dourado" to Color(0xFFC0841A),
            "Bordo" to Color(0xFF7F1D1D),
            "Turquesa" to Color(0xFF38BDF8),
            "Creme" to Color(0xFFF5F5DC)
        )
    }
    val nomeCorSelecionada by remember(corSelecionada, opcoesCor) {
        derivedStateOf {
            opcoesCor.firstOrNull { (_, cor) -> cor.toArgb() == corSelecionada }?.first ?: "Selecionar"
        }
    }
    var vezesBatido by remember { mutableStateOf(carroAtual.vezesBatido) }
    var tempoComVeiculo by remember { mutableStateOf(carroAtual.tempoComVeiculo) }
    var alvoVoz by remember { mutableStateOf("nome") }
    val contentScrollState = rememberScrollState()
    val isBikeType = tipoSelecionado == TipoVeiculo.BICICLETA || tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteVehicleDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onExcluir()
            }
        )
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isBikeType) "Editar bike" else "Editar veiculo",
                        color = titleColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = titleColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Excluir veiculo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgLight)
            )
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EditSectionCard(title = "", icon = null) {
                    var batidasExpanded by remember { mutableStateOf(false) }
                    var tempoExpanded by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = tipoSelecionado.label,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Tipo") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = marca,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Marca") },
                        singleLine = true,
                        colors = textFieldColors,
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
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = corExpanded) },
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = corExpanded,
                            onDismissRequest = { corExpanded = false }
                        ) {
                            opcoesCor.forEach { (nomeCor, corItem) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(corItem)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(nomeCor, color = titleColor)
                                        }
                                    },
                                    onClick = {
                                        corSelecionada = corItem.toArgb()
                                        corExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome do veiculo") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = ::iniciarCapturaVozApelido) {
                                Icon(Icons.Default.Mic, contentDescription = "Falar nome do veiculo")
                            }
                        },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val motorLabel = if (tipoSelecionado == TipoVeiculo.BICICLETA) "Aro" else "Motor"
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(motorLabel) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val labelQuemUsa = if (
                        tipoSelecionado == TipoVeiculo.BICICLETA ||
                        tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
                    ) {
                        "Quem usa essa bike?"
                    } else {
                        "Quem usa esse veiculo?"
                    }
                    ExposedDropdownMenuBox(
                        expanded = quemUsaExpanded,
                        onExpandedChange = { quemUsaExpanded = !quemUsaExpanded }
                    ) {
                        OutlinedTextField(
                            value = quemUsaOpcao,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(labelQuemUsa) },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quemUsaExpanded) },
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(expanded = quemUsaExpanded, onDismissRequest = { quemUsaExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Eu mesmo", color = titleColor) },
                                onClick = {
                                    quemUsaOpcao = "Eu mesmo"
                                    proprietario = nomeUsuarioLogado
                                    quemUsaExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Outra pessoa", color = titleColor) },
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
                            label = { Text("Nome da pessoa") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = kmAtualStr,
                        onValueChange = { kmAtualStr = formatarKmTextoLocal(it) },
                        label = { Text("KM atual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = batidasExpanded,
                        onExpandedChange = { batidasExpanded = !batidasExpanded }
                    ) {
                        OutlinedTextField(
                            value = vezesBatido?.toString() ?: "Não informado",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vezes batido") },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batidasExpanded) },
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(expanded = batidasExpanded, onDismissRequest = { batidasExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Não informado", color = titleColor) },
                                onClick = {
                                    vezesBatido = null
                                    batidasExpanded = false
                                }
                            )
                            (0..10).forEach { quantidade ->
                                DropdownMenuItem(
                                    text = { Text(quantidade.toString(), color = titleColor) },
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
                            label = { Text("Tempo com veiculo") },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoExpanded) },
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(expanded = tempoExpanded, onDismissRequest = { tempoExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Não informado", color = titleColor) },
                                onClick = {
                                    tempoComVeiculo = ""
                                    tempoExpanded = false
                                }
                            )
                            opcoesTempoComVeiculoEdicao().forEach { tempo ->
                                DropdownMenuItem(
                                    text = { Text(tempo, color = titleColor) },
                                    onClick = {
                                        tempoComVeiculo = tempo
                                        tempoExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onSalvar(
                            carroAtual.copy(
                                nome = nome,
                                marca = marca,
                                modelo = modelo,
                                proprietario = proprietario,
                                corArgb = corSelecionada,
                                kmAtual = kmAtualStr.filter(Char::isDigit).toIntOrNull() ?: 0,
                                tipoVeiculo = tipoSelecionado,
                                vezesBatido = vezesBatido,
                                tempoComVeiculo = tempoComVeiculo
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Salvar Alterações", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeleteVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = scheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Excluir este veículo?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Esta ação não pode ser desfeita e todos os dados do veículo serão removidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditHeroCard(
    nome: String,
    modelo: String,
    tipoVeiculo: TipoVeiculo,
    cor: Int,
    accent: Color,
    isDark: Boolean
) {
    val baseColor = Color(cor)
    val cardBase = if (isDark) Color(0xFF0B1224) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)
    val iconCircleBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val iconCircleBorder = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.14f)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)
    val cardFill = if (isDark) baseColor.copy(alpha = 0.34f) else baseColor.copy(alpha = 0.20f)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBase),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardFill)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg)
                        .border(1.dp, iconCircleBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    VehicleIcon(tipoVeiculo = tipoVeiculo, tint = textPrimary, size = 40.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(nome.ifBlank { "Veiculo" }, color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        val modeloFallback = if (tipoVeiculo == TipoVeiculo.BICICLETA) "Aro" else "Motor"
                        Text(modelo.ifBlank { modeloFallback }, color = textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSectionCard(
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
private fun ColorRow(
    selecionada: Int,
    onSelect: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val labelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val selectedAccent = Color(0xFF2563EB)
    val cores = listOf(
        "Branco" to Color(0xFFFFFFFF),
        "Preto" to Color(0xFF0F172A),
        "Prata" to Color(0xFFC0C0C0),
        "Cinza" to Color(0xFF9CA3AF),
        "Vermelho" to Color(0xFFDC2626),
        "Azul" to Color(0xFF4F7DBE),
        "Marrom" to Color(0xFF7C3F00),
        "Bege" to Color(0xFFE7D7C1),
        "Verde" to Color(0xFF16A34A),
        "Amarelo" to Color(0xFFFACC15),
        "Laranja" to Color(0xFFF97316),
        "Roxo" to Color(0xFF6D5BD0),
        "Rosa" to Color(0xFFEC4899),
        "Dourado" to Color(0xFFC0841A),
        "Bordo" to Color(0xFF7F1D1D),
        "Turquesa" to Color(0xFF38BDF8),
        "Creme" to Color(0xFFF5F5DC)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cores.forEach { (label, cor) ->
            val selecionadaCor = selecionada == cor.toArgb()
            val corContraste = if (cor.luminance() > 0.62f) Color.Black else Color.White
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (selecionadaCor) 3.dp else 1.5.dp,
                            color = if (selecionadaCor) selectedAccent else corContraste.copy(alpha = 0.95f),
                            shape = CircleShape
                        )
                        .padding(if (selecionadaCor) 2.dp else 1.dp)
                        .clip(CircleShape)
                        .background(cor)
                        .border(
                            width = 1.dp,
                            color = corContraste.copy(alpha = if (selecionadaCor) 0.9f else 0.55f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(cor.toArgb()) }
                )
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun opcoesTempoComVeiculoEdicao(): List<String> = listOf(
    "Menos de 6 meses",
    "6 meses a 1 ano",
    "1 a 2 anos",
    "2 a 3 anos",
    "3 a 5 anos",
    "Mais de 5 anos"
)

private fun formatarKmLocal(valor: Int): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(valor)

private fun formatarKmTextoLocal(texto: String): String {
    val digits = texto.filter(Char::isDigit)
    val value = digits.toLongOrNull() ?: 0L
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}
