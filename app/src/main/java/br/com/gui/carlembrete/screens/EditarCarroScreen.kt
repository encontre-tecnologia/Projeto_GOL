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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Motorcycle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditarCarroScreen(
    carroAtual: CarroInfo,
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit
) {
    val context = LocalContext.current
    val primaryDark = Color(0xFF121B30)
    val accentBlue = Color(0xFF3B82F6)

    var nome by remember { mutableStateOf(carroAtual.nome) }
    var marca by remember { mutableStateOf(carroAtual.marca) }
    var modelo by remember { mutableStateOf(carroAtual.modelo) }
    var proprietario by remember { mutableStateOf(carroAtual.proprietario) }
    var kmAtualStr by remember { mutableStateOf(if (carroAtual.kmAtual > 0) formatarKmLocal(carroAtual.kmAtual) else "") }
    var tipoSelecionado by remember { mutableStateOf(carroAtual.tipoVeiculo) }
    var tipoSelecionadoConfirmado by remember { mutableStateOf(true) }
    var corSelecionada by remember { mutableStateOf(carroAtual.corArgb) }
    var alvoVoz by remember { mutableStateOf("nome") }

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
            TopAppBar(
                title = { Text("Editar veiculo", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF16233A), primaryDark, Color(0xFF0F172A))
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EditHeroCard(
                    nome = if (nome.isNotBlank()) nome else carroAtual.nome,
                    modelo = if (modelo.isNotBlank()) modelo else carroAtual.modelo,
                    marca = marca.ifBlank { carroAtual.marca },
                    tipoVeiculo = tipoSelecionado,
                    cor = corSelecionada,
                    accent = accentBlue
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tipo de veiculo", color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    TipoVeiculoSelector(
                        selecionado = tipoSelecionado,
                        onSelect = {
                            tipoSelecionado = it
                            tipoSelecionadoConfirmado = true
                        }
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
                        else -> marcasSuportadas
                    }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    LaunchedEffect(tipoSelecionado) {
                        if (marca.isNotBlank() && !marcasDisponiveis.contains(marca)) {
                            marca = ""
                        }
                    }

                    var marcaExpanded by remember { mutableStateOf(false) }
                    val marcaLogo = logoResForMarca(marca, tipoSelecionado)
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
                            placeholder = { Text("Selecione a marca") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = marcaExpanded) },
                            enabled = tipoSelecionadoConfirmado,
                            leadingIcon = if (marcaLogo != null) {
                                {
                                    Image(
                                        painter = painterResource(marcaLogo),
                                        contentDescription = marca,
                                        modifier = Modifier.size(24.dp),
                                        colorFilter = ColorFilter.tint(Color.White)
                                    )
                                }
                            } else {
                                null
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
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
                                                        .background(Color.White.copy(alpha = 0.08f))
                                                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (res != null) {
                                                        Image(
                                                            painter = painterResource(res),
                                                            contentDescription = marcaNome,
                                                            modifier = Modifier.size(16.dp),
                                                            colorFilter = ColorFilter.tint(Color.White)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = tipoSelecionado?.icon ?: Icons.Rounded.DirectionsCar,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.width(10.dp))
                                            }
                                            Text(
                                                marcaNome,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
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

                EditSectionCard(title = "Proprietario e uso", icon = Icons.Rounded.Person) {
                    OutlinedTextField(
                        value = proprietario,
                        onValueChange = { proprietario = it },
                        label = { Text("Proprietario") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
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
                    Icon(Icons.Rounded.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar alteracoes", color = Color.White, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun EditHeroCard(
    nome: String,
    modelo: String,
    marca: String,
    tipoVeiculo: TipoVeiculo,
    cor: Int,
    accent: Color
) {
    val baseColor = Color(cor)
    val gradient = Brush.linearGradient(
        colors = listOf(baseColor.copy(alpha = 0.6f), Color(0xFF0F172A))
    )
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1224)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val logoRes = logoResForMarca(marca, tipoVeiculo)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(logoRes),
                            contentDescription = marca,
                            modifier = Modifier.size(32.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    } else {
                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(marca.ifBlank { "Marca" }, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(nome.ifBlank { "Veiculo" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        val modeloFallback = if (tipoVeiculo == TipoVeiculo.BICICLETA) "Aro" else "Motor"
                        Text(modelo.ifBlank { modeloFallback }, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ColorRow(
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
                            color = if (selecionadaCor) Color.White else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(cor.toArgb()) }
                )
                Text(
                    text = label,
                    color = Color(0xFF94A3B8),
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
