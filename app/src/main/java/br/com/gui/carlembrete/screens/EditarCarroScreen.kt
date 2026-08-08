package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Build
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
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
    val nomeUsuarioLogado = remember {
        val displayName = FirebaseAuth.getInstance().currentUser?.displayName
            ?.trim()?.split("\\s+".toRegex())?.filter { it.isNotBlank() }
        when {
            displayName.isNullOrEmpty() -> "Eu mesmo"
            displayName.size == 1 -> displayName.first()
            else -> "${displayName.first()} ${displayName.last()}"
        }
    }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val titleColor = scheme.onBackground
    val textSecondary = scheme.onSurfaceVariant
    val accentBlue = scheme.primary
    val bgScreen = if (isDark) Color.Black else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)
    val fieldBorder = if (isDark) Color(0xFF334155) else scheme.outlineVariant

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = titleColor,
        unfocusedTextColor = titleColor,
        focusedLabelColor = textSecondary,
        unfocusedLabelColor = textSecondary,
        focusedBorderColor = accentBlue,
        unfocusedBorderColor = fieldBorder,
        cursorColor = titleColor,
        focusedLeadingIconColor = accentBlue,
        unfocusedLeadingIconColor = textSecondary,
        focusedTrailingIconColor = textSecondary,
        unfocusedTrailingIconColor = textSecondary,
        disabledTextColor = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF64748B),
        disabledLabelColor = textSecondary.copy(alpha = 0.6f),
        disabledBorderColor = fieldBorder.copy(alpha = 0.5f),
        disabledLeadingIconColor = textSecondary.copy(alpha = 0.5f)
    )

    var nome by remember { mutableStateOf(carroAtual.nome) }
    // Sem este campo aqui, veiculo cadastrado antes da placa existir nunca ganharia uma:
    // as telas de criacao eram os unicos lugares com o campo.
    var placa by remember { mutableStateOf(normalizarPlaca(carroAtual.placa)) }
    var marca by remember { mutableStateOf(carroAtual.marca) }
    var modelo by remember { mutableStateOf(carroAtual.modelo) }
    val tipoSelecionado = carroAtual.tipoVeiculo
    val isBikeType = tipoSelecionado == TipoVeiculo.BICICLETA || tipoSelecionado == TipoVeiculo.BIKE_ELETRICA
    val proprietarioInicial = remember(carroAtual.proprietario, nomeUsuarioLogado) {
        if (carroAtual.proprietario.equals("Eu mesmo", ignoreCase = true)) nomeUsuarioLogado
        else carroAtual.proprietario
    }
    var proprietario by remember { mutableStateOf(proprietarioInicial) }
    var quemUsaOpcao by remember {
        mutableStateOf(
            if (carroAtual.proprietario.equals("Eu mesmo", ignoreCase = true) ||
                carroAtual.proprietario.equals(nomeUsuarioLogado, ignoreCase = true)) "Eu mesmo"
            else "Outra pessoa"
        )
    }
    var quemUsaExpanded by remember { mutableStateOf(false) }
    var kmAtualStr by remember { mutableStateOf(if (carroAtual.kmAtual > 0) formatarKmLocal(carroAtual.kmAtual) else "") }
    var corSelecionada by remember { mutableStateOf(carroAtual.corArgb) }
    var vezesBatido by remember { mutableStateOf(carroAtual.vezesBatido) }
    var tempoComVeiculo by remember { mutableStateOf(carroAtual.tempoComVeiculo) }
    var batidasExpanded by remember { mutableStateOf(false) }
    var tempoExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()

    val kmAtualNormalizado by remember(kmAtualStr) {
        derivedStateOf { kmAtualStr.filter(Char::isDigit).toIntOrNull() ?: 0 }
    }
    // Placa fora do padrao nao salva, mas em branco salva: o campo e opcional e apagar
    // a placa tem que ser possivel.
    val placaAceita = placaAceitavel(placa)
    val hasChanges by remember(
        nome, placa, marca, modelo, proprietario, corSelecionada,
        kmAtualNormalizado, vezesBatido, tempoComVeiculo, placaAceita
    ) {
        derivedStateOf {
            placaAceita && (
                nome != carroAtual.nome ||
                    placa != normalizarPlaca(carroAtual.placa) ||
                    marca != carroAtual.marca ||
                    modelo != carroAtual.modelo ||
                    proprietario != proprietarioInicial ||
                    corSelecionada != carroAtual.corArgb ||
                    kmAtualNormalizado != carroAtual.kmAtual ||
                    vezesBatido != carroAtual.vezesBatido ||
                    tempoComVeiculo != carroAtual.tempoComVeiculo
                )
        }
    }

    if (showDeleteDialog) {
        DeleteVehicleDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { showDeleteDialog = false; onExcluir() }
        )
    }

    Scaffold(
        containerColor = bgScreen,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Botão sempre visível, sobe com o teclado automaticamente
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgScreen)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                val disabledBg = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = if (hasChanges) 20.dp else 0.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF2563EB).copy(alpha = 0.55f)
                        )
                        .background(
                            brush = if (hasChanges)
                                Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
                            else
                                Brush.horizontalGradient(listOf(disabledBg, disabledBg)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            enabled = hasChanges,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onSalvar(
                                carroAtual.copy(
                                    nome = nome,
                                    placa = normalizarPlaca(placa).takeIf { it.isNotBlank() },
                                    marca = marca,
                                    modelo = modelo,
                                    proprietario = proprietario,
                                    corArgb = corSelecionada,
                                    kmAtual = kmAtualNormalizado,
                                    tipoVeiculo = tipoSelecionado,
                                    vezesBatido = vezesBatido,
                                    tempoComVeiculo = tempoComVeiculo
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trNow("Salvar Alteracoes", "Save changes"),
                        color = if (hasChanges) Color.White
                                else if (isDark) Color.White.copy(alpha = 0.30f) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(contentScrollState)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top bar ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(52.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBackIosNew, null, tint = titleColor)
                }
                Text(
                    text = if (isBikeType) trNow("Editar bike", "Edit bike")
                           else trNow("Editar veiculo", "Edit vehicle"),
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── Hero card ──────────────────────────────────────────────
            EditHeroCard(
                nome = nome,
                modelo = modelo,
                tipoVeiculo = tipoSelecionado,
                cor = corSelecionada,
                accent = accentBlue,
                isDark = isDark
            )

            // ── Cor ────────────────────────────────────────────────────
            FormSectionLabel(trNow("COR DO VEICULO", "VEHICLE COLOR"), isDark)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    ColorRow(selecionada = corSelecionada, onSelect = { corSelecionada = it })
                }
            }

            // ── Dados do veículo ───────────────────────────────────────
            FormSectionLabel(trNow("DADOS DO VEICULO", "VEHICLE DATA"), isDark)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tipoSelecionado.label,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Tipo") },
                        leadingIcon = { Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = marca,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Marca") },
                        leadingIcon = { Icon(Icons.Rounded.Build, null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text(trNow("Nome do veiculo", "Vehicle name")) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    val placaForaDoPadrao = placa.isNotBlank() && !placaAceita
                    OutlinedTextField(
                        value = placa,
                        onValueChange = { placa = normalizarPlaca(it) },
                        isError = placaForaDoPadrao,
                        label = { Text(trNow("Placa (opcional)", "Plate (optional)")) },
                        placeholder = { Text("ABC1D23") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Badge, null, modifier = Modifier.size(20.dp))
                        },
                        supportingText = if (placaForaDoPadrao) {
                            { Text(trNow("Use o formato ABC1D23 ou ABC1234.", "Use ABC1D23 or ABC1234.")) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    val motorLabel = if (tipoSelecionado == TipoVeiculo.BICICLETA) "Aro" else "Motor"
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(motorLabel) },
                        leadingIcon = { Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Proprietário e uso ─────────────────────────────────────
            FormSectionLabel(trNow("PROPRIETARIO E USO", "OWNER & USAGE"), isDark)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val labelQuemUsa = if (isBikeType) trNow("Quem usa essa bike?", "Who rides this bike?")
                                       else trNow("Quem usa esse veiculo?", "Who uses this vehicle?")
                    ExposedDropdownMenuBox(
                        expanded = quemUsaExpanded,
                        onExpandedChange = { quemUsaExpanded = !quemUsaExpanded }
                    ) {
                        OutlinedTextField(
                            value = quemUsaOpcao,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(labelQuemUsa) },
                            leadingIcon = { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quemUsaExpanded) },
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = quemUsaExpanded,
                            onDismissRequest = { quemUsaExpanded = false }
                        ) {
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
                                    if (proprietario == nomeUsuarioLogado ||
                                        proprietario.equals("Eu mesmo", ignoreCase = true)) {
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
                            label = { Text(trNow("Nome da pessoa", "Person's name")) },
                            leadingIcon = { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = kmAtualStr,
                        onValueChange = { kmAtualStr = formatarKmTextoEditar(it) },
                        label = { Text("KM atual") },
                        leadingIcon = { Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = batidasExpanded,
                        onExpandedChange = { batidasExpanded = !batidasExpanded }
                    ) {
                        OutlinedTextField(
                            value = vezesBatido?.toString() ?: trNow("Nao informado", "Not informed"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(trNow("Vezes batido", "Times crashed")) },
                            leadingIcon = { Icon(Icons.Rounded.Build, null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batidasExpanded) },
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = batidasExpanded,
                            onDismissRequest = { batidasExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(trNow("Nao informado", "Not informed"), color = titleColor) },
                                onClick = { vezesBatido = null; batidasExpanded = false }
                            )
                            (0..10).forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q.toString(), color = titleColor) },
                                    onClick = { vezesBatido = q; batidasExpanded = false }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = tempoExpanded,
                        onExpandedChange = { tempoExpanded = !tempoExpanded }
                    ) {
                        OutlinedTextField(
                            value = tempoComVeiculo.ifBlank { trNow("Nao informado", "Not informed") },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(trNow("Tempo com veiculo", "Time with vehicle")) },
                            leadingIcon = { Icon(Icons.Rounded.AccessTime, null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoExpanded) },
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = tempoExpanded,
                            onDismissRequest = { tempoExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(trNow("Nao informado", "Not informed"), color = titleColor) },
                                onClick = { tempoComVeiculo = ""; tempoExpanded = false }
                            )
                            opcoesTempoComVeiculoEdicao().forEach { tempo ->
                                DropdownMenuItem(
                                    text = { Text(tempo, color = titleColor) },
                                    onClick = { tempoComVeiculo = tempo; tempoExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // ── Excluir veículo ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showDeleteDialog = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = scheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isBikeType) trNow("Excluir esta bike", "Delete this bike")
                               else trNow("Excluir este veiculo", "Delete this vehicle"),
                        color = scheme.error.copy(alpha = 0.75f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FormSectionLabel(text: String, isDark: Boolean) {
    Text(
        text = text,
        color = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF64748B),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun DeleteVehicleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else scheme.surface),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else scheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(scheme.error.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = scheme.error, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = trNow("Excluir este veiculo?", "Delete this vehicle?"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = trNow(
                        "Esta acao nao pode ser desfeita e todos os dados serao removidos.",
                        "This action cannot be undone and all data will be removed."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, scheme.outline)
                    ) { Text(trNow("Cancelar", "Cancel")) }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(trNow("Excluir", "Delete"), fontWeight = FontWeight.Bold) }
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
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSec = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF475569)
    val glowAlpha = if (isDark) 0.30f else 0.18f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBase),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(baseColor.copy(alpha = glowAlpha), Color.Transparent)
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(baseColor.copy(alpha = if (isDark) 0.20f else 0.12f), CircleShape)
                        .border(1.dp, baseColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    VehicleIcon(tipoVeiculo = tipoVeiculo, tint = textPrimary, size = 36.dp)
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        nome.ifBlank { trNow("Veiculo", "Vehicle") },
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, null, tint = accent, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        val fallback = if (tipoVeiculo == TipoVeiculo.BICICLETA) "Aro" else "Motor"
                        Text(modelo.ifBlank { fallback }, color = textSec, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    icon: ImageVector?,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else scheme.surface),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.09f) else scheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (title.isNotBlank() && icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun ColorRow(selecionada: Int, onSelect: (Int) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val labelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val selectedAccent = Color(0xFF2563EB)
    val cores = listOf(
        "Branco" to Color(0xFFFFFFFF), "Preto" to Color(0xFF0F172A),
        "Prata" to Color(0xFFC0C0C0), "Cinza" to Color(0xFF9CA3AF),
        "Vermelho" to Color(0xFFDC2626), "Azul" to Color(0xFF4F7DBE),
        "Marrom" to Color(0xFF7C3F00), "Bege" to Color(0xFFE7D7C1),
        "Verde" to Color(0xFF16A34A), "Amarelo" to Color(0xFFFACC15),
        "Laranja" to Color(0xFFF97316), "Roxo" to Color(0xFF6D5BD0),
        "Rosa" to Color(0xFFEC4899), "Dourado" to Color(0xFFC0841A),
        "Bordo" to Color(0xFF7F1D1D), "Turquesa" to Color(0xFF38BDF8),
        "Creme" to Color(0xFFF5F5DC)
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cores.forEach { (label, cor) ->
            val sel = selecionada == cor.toArgb()
            val contraste = if (cor.luminance() > 0.62f) Color.Black else Color.White
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (sel) 3.dp else 1.5.dp,
                            color = if (sel) selectedAccent else contraste.copy(alpha = 0.80f),
                            shape = CircleShape
                        )
                        .padding(if (sel) 2.dp else 1.dp)
                        .clip(CircleShape)
                        .background(cor)
                        .clickable { onSelect(cor.toArgb()) }
                )
                Text(label, color = labelColor, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

private fun opcoesTempoComVeiculoEdicao(): List<String> = listOf(
    "Menos de 6 meses", "6 meses a 1 ano", "1 a 2 anos",
    "2 a 3 anos", "3 a 5 anos", "Mais de 5 anos"
)

private fun formatarKmLocal(valor: Int): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(valor)

private fun formatarKmTextoEditar(texto: String): String {
    val digits = texto.filter(Char::isDigit).take(10)
    if (digits.isEmpty()) return ""
    val value = (digits.toLongOrNull() ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong())
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}
