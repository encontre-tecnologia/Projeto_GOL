package br.com.gui.carlembrete

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Motorcycle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.gui.carlembrete.VehicleIcon
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoCarroScreen(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit
) {
    val context = LocalContext.current
    val bgLight = Color(0xFFF8FAFC)
    val cardLight = Color.White
    val borderLight = Color(0xFFE2E8F0)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF64748B)
    val accentBlue = Color(0xFF3B82F6)
    val carroBase = CarroInfo(nome = "", modelo = "")

    var nome by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var proprietario by remember { mutableStateOf("") }
    var kmAtualStr by remember { mutableStateOf("") }
    var tipoSelecionado by remember { mutableStateOf<TipoVeiculo?>(null) }
    var tipoSelecionadoConfirmado by remember { mutableStateOf(false) }
    var corSelecionada by remember { mutableStateOf(carroBase.corArgb) }
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
        containerColor = bgLight,
        topBar = {
            if (showTopBar) {
                TopAppBar(
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
                NovoHeroCard()

                NovoSectionCard(title = "Identificacao", icon = Icons.Rounded.DirectionsCar) {
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tipo de veiculo", color = textSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    TipoVeiculoSelector(
                        selecionado = tipoSelecionado,
                        onSelect = {
                            tipoSelecionado = it
                            tipoSelecionadoConfirmado = true
                        },
                        lightStyle = true
                    )

                    val marcasBicicletaLocal = listOf(
                        "Caloi",
                        "Monark",
                        "Houston",
                        "Oggi",
                        "Sense",
                        "Audax",
                        "Soul Cycles",
                        "Cannondale",
                        "Specialized",
                        "Trek"
                    )
                    val marcasCaminhaoLocal = listOf(
                        "Mercedes-Benz",
                        "Volkswagen",
                        "Scania",
                        "Volvo",
                        "IVECO",
                        "DAF",
                        "Ford",
                        "MAN"
                    )
                    val marcasMotoLocal = listOf(
                        "Honda",
                        "Yamaha",
                        "Suzuki",
                        "Kawasaki",
                        "BMW",
                        "Harley-Davidson",
                        "Ducati",
                        "Royal Enfield",
                        "Triumph",
                        "Shineray"
                    )
                    val marcasCaminhoneteLocal = listOf(
                        "Toyota",
                        "Chevrolet",
                        "Ford",
                        "Volkswagen",
                        "Fiat",
                        "Nissan",
                        "Mitsubishi",
                        "Ram",
                        "Renault",
                        "Jeep",
                        "Honda",
                        "Hyundai"
                    )
                    val marcasTratorLocal = listOf(
                        "John Deere",
                        "Massey Ferguson",
                        "Valtra",
                        "New Holland",
                        "Case IH",
                        "Ford",
                        "Kubota",
                        "Fendt",
                        "Mahindra",
                        "Agrale"
                    )
                    val marcasDisponiveis = when (tipoSelecionado) {
                        TipoVeiculo.BICICLETA -> marcasBicicletaLocal
                        TipoVeiculo.CAMINHONETE -> marcasCaminhoneteLocal
                        TipoVeiculo.CAMINHAO -> marcasCaminhaoLocal
                        TipoVeiculo.MOTO -> marcasMotoLocal
                        TipoVeiculo.TRATOR -> marcasTratorLocal
                        null -> emptyList()
                        else -> marcasSuportadas
                    }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    LaunchedEffect(tipoSelecionado) {
                        if (marca.isNotBlank() && !marcasDisponiveis.contains(marca)) {
                            marca = ""
                        }
                    }

                    var marcaExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = marcaExpanded,
                        onExpandedChange = {
                            if (tipoSelecionadoConfirmado) {
                                marcaExpanded = !marcaExpanded
                            } else {
                                Toast.makeText(context, "Selecione o tipo de veiculo primeiro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = marca,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Marca") },
                            placeholder = { Text(if (marcaExpanded) "Selecione a marca" else "Marca") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = marcaExpanded) },
                            enabled = tipoSelecionadoConfirmado,
                            leadingIcon = if (tipoSelecionado != null) {
                                {
                                    VehicleIcon(
                                        tipoVeiculo = tipoSelecionado!!,
                                        tint = textPrimary,
                                        size = 22.dp
                                    )
                                }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedPlaceholderColor = textSecondary,
                                unfocusedPlaceholderColor = textSecondary
                            )
                        )
                        ExposedDropdownMenu(expanded = marcaExpanded, onDismissRequest = { marcaExpanded = false }) {
                            marcasDisponiveis.forEach { marcaNome ->
                                val res = logoResForMarca(marcaNome, tipoSelecionado)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (res != null || tipoSelecionado != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFF1F5F9))
                                                        .border(1.dp, borderLight, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (res != null) {
                                                        Image(
                                                            painter = painterResource(res),
                                                            contentDescription = marcaNome,
                                                            modifier = Modifier.size(16.dp),
                                                            colorFilter = ColorFilter.tint(textPrimary)
                                                        )
                                                    } else {
                                                        val tipoLocal = tipoSelecionado
                                                        if (tipoLocal != null) {
                                                            VehicleIcon(
                                                                tipoVeiculo = tipoLocal,
                                                                tint = textPrimary,
                                                                size = 16.dp
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Rounded.DirectionsCar,
                                                                contentDescription = null,
                                                                tint = textPrimary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.width(10.dp))
                                            }
                                            Text(
                                                marcaNome,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        marca = marcaNome
                                        marcaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                NovoSectionCard(title = "Detalhes", icon = Icons.Rounded.Speed) {
                    OutlinedTextField(
                        value = proprietario,
                        onValueChange = { proprietario = it },
                        label = { Text("Proprietario") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
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
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                NovoSectionCard(title = "Cor do veiculo", icon = Icons.Rounded.Edit) {
                    ColorRowNovo(
                        selecionada = corSelecionada,
                        onSelect = { corSelecionada = it }
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
                                modelo = modelo,
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
                    Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar veiculo", color = Color.White, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = textSecondary)
                }
            }
        }
    }
}

@Composable
private fun NovoHeroCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Novo veiculo", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Crie um perfil para acompanhar seus lembretes", color = Color(0xFF475569), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NovoSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color(0xFF334155), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            content()
        }
    }
}

@Composable
private fun ColorRowNovo(
    selecionada: Int,
    onSelect: (Int) -> Unit
) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cor)
                        .border(
                            width = if (selecionadaCor) 3.dp else 1.dp,
                            color = if (selecionadaCor) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                        .clickable { onSelect(cor.toArgb()) }
                )
                Text(
                    text = label,
                    color = Color(0xFF64748B),
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
