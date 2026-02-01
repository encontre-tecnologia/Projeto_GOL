package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.material.icons.filled.CalendarMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbastecimentoScreen(carroId: String, onDismiss: () -> Unit) {
    val primaryDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF34D399)
    val cardStroke = Color(0xFF1F2A44)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var precoGasolina by remember { mutableStateOf("") }
    var valorAbastecido by remember { mutableStateOf("") }
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    var dataSelecionada by remember { mutableStateOf(LocalDate.now()) }
    val preco = precoGasolina.replace(",", ".").toDoubleOrNull()
    val total = valorAbastecido.replace(",", ".").toDoubleOrNull()
    val litros = if (preco != null && total != null && preco > 0.0) total / preco else null
    val litrosTexto = litros?.let { String.format(Locale("pt", "BR"), "%.2f L", it) } ?: "--"
    val gastoTexto = total?.let { formatarMoeda(it) } ?: "--"
    val canSave = preco != null && total != null && preco > 0.0 && total > 0.0 && !isSaving
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Color.White,
        focusedBorderColor = Color(0xFF334155),
        unfocusedBorderColor = Color(0xFF1F2A44),
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color(0xFF94A3B8),
        focusedLeadingIconColor = Color(0xFFCBD5F5),
        unfocusedLeadingIconColor = Color(0xFF94A3B8),
        focusedContainerColor = Color(0xFF0F172A),
        unfocusedContainerColor = Color(0xFF0F172A)
    )

    LaunchedEffect(Unit) {
        abastecimentos = BancoDeDados.carregarAbastecimentos(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = primaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Abastecimento",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, cardStroke)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0B1224), Color(0xFF0F172A), Color(0xFF111827))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, accentBlue, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Abastecimento",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "Registre o gasto e calcule os litros",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }// TSW
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, cardStroke)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Dados do posto",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    OutlinedTextField(
                        value = precoGasolina,
                        onValueChange = { precoGasolina = it },
                        label = { Text("Valor da gasolina (R$/L)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.LocalGasStation,
                                contentDescription = null,
                                tint = Color(0xFFCBD5F5)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = valorAbastecido,
                        onValueChange = { valorAbastecido = it },
                        label = { Text("Valor abastecido (R$)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFFCBD5F5)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    val abrirDatePicker = {
                        val dataAtual = dataSelecionada
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dataSelecionada = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            dataAtual.year,
                            dataAtual.monthValue - 1,
                            dataAtual.dayOfMonth
                        ).show()
                    }
                    OutlinedTextField(
                        value = dataSelecionada.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data do registro") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clickable(onClick = abrirDatePicker),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFFCBD5F5)
                            )
                        },
                        colors = fieldColors
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, cardStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Resumo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ResumoItem(
                            title = "Litros",
                            value = litrosTexto,
                            accent = accentBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ResumoItem(
                            title = "Gasto",
                            value = gastoTexto,
                            accent = accentGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val precoValue = preco ?: return@Button
                    val totalValue = total ?: return@Button
                    val litrosCalculados = totalValue / precoValue
                    val data = dataSelecionada.format(dateFormatter)
                    val novo = Abastecimento(
                        carroId = carroId,
                        data = data,
                        precoLitro = precoValue,
                        valorPago = totalValue,
                        litros = litrosCalculados
                    )
                    val carroAtual = BancoDeDados.carregarCarros(context)
                        ?.firstOrNull { it.id == carroId }
                    if (carroAtual != null && AppPreferences.getFuelStartKm(context, carroId) == null) {
                        AppPreferences.setFuelStartKm(context, carroId, carroAtual.kmAtual)
                    }
                    val atualizada = abastecimentos + novo
                    isSaving = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            BancoDeDados.salvarAbastecimentos(context, atualizada)
                        }
                        abastecimentos = atualizada
                        precoGasolina = ""
                        valorAbastecido = ""
                        isSaving = false
                        Toast.makeText(context, "Abastecimento salvo!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isSaving) "Salvando..." else "Salvar gasto",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, textLight: Color, textDim: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textDim, fontSize = 12.sp)
        Text(value, color = textLight, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun ResumoItem(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B1224))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}
