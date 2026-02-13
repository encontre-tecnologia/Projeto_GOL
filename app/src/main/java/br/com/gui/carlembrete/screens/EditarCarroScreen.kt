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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.graphics.Brush
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryDark = Color(0xFF121B30)
    val accentBlue = Color(0xFF3B82F6)
    val screenBackground = if (isDark) Brush.verticalGradient(listOf(Color(0xFF16233A), primaryDark, Color(0xFF0F172A))) else Brush.verticalGradient(listOf(Color(0xFFF4F7FB), Color(0xFFEFF3FA)))
    val sectionBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val actionOutline = if (isDark) Color.White else Color.Black
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
    var proprietario by remember { mutableStateOf(carroAtual.proprietario) }
    var kmAtualStr by remember { mutableStateOf(if (carroAtual.kmAtual > 0) formatarKmLocal(carroAtual.kmAtual) else "") }
    val tipoSelecionado = carroAtual.tipoVeiculo
    var corSelecionada by remember { mutableStateOf(carroAtual.corArgb) }
    var alvoVoz by remember { mutableStateOf("nome") }
    val contentScrollState = rememberScrollState()
    val showTopBar by remember { derivedStateOf { contentScrollState.value <= 8 } }

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
        containerColor = Color.Transparent,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Editar veiculo", color = titleColor, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = titleColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EditHeroCard(
                    nome = if (nome.isNotBlank()) nome else carroAtual.nome,
                    modelo = if (modelo.isNotBlank()) modelo else carroAtual.modelo,
                    tipoVeiculo = tipoSelecionado,
                    cor = corSelecionada,
                    accent = accentBlue,
                    isDark = isDark
                )

                EditSectionCard(title = "Dados do veiculo", icon = Icons.Rounded.DirectionsCar) {
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
                        onValueChange = { modelo = it },
                        label = { Text(motorLabel) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = ::iniciarCapturaVozMotor) {
                                Icon(Icons.Default.Mic, contentDescription = "Falar motor")
                            }
                        },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                }

                EditSectionCard(title = "Proprietario e uso", icon = Icons.Rounded.Person) {
                    OutlinedTextField(
                        value = proprietario,
                        onValueChange = { proprietario = it },
                        label = { Text("Proprietario") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = kmAtualStr,
                        onValueChange = { kmAtualStr = formatarKmTextoLocal(it) },
                        label = { Text("KM Atual (Painel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EditSectionCard(title = "Cor do veiculo", icon = Icons.Rounded.Edit) {
                    ColorRow(
                        selecionada = corSelecionada,
                        onSelect = { corSelecionada = it }
                    )
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
                                tipoVeiculo = tipoSelecionado
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Salvar alteracoes", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExcluir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    border = BorderStroke(1.dp, actionOutline),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = actionOutline)
                ) {
                    Text(
                        text = "Excluir veiculo",
                        color = actionOutline,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardSurface = if (isDark) Color(0xFF0F172A) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)
    val titleColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val iconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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

private fun formatarKmLocal(valor: Int): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(valor)

private fun formatarKmTextoLocal(texto: String): String {
    val digits = texto.filter(Char::isDigit)
    val value = digits.toLongOrNull() ?: 0L
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}
