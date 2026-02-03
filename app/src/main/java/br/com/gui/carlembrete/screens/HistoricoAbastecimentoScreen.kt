import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.Abastecimento
import br.com.gui.carlembrete.BancoDeDados
import br.com.gui.carlembrete.formatarMoedaLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- PALETA ZELLU ---
private val PrimaryDark = Color(0xFF0F172A)
private val GradientStart = Color(0xFF334155)
private val GradientEnd = Color(0xFF1E293B)
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF22C55E)
private val AlertRed = Color(0xFFEF4444)
private val SurfaceDark = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoAbastecimentoScreen(carroId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var itemEdicao by remember { mutableStateOf<Abastecimento?>(null) }
    var itemExcluir by remember { mutableStateOf<Abastecimento?>(null) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    LaunchedEffect(Unit) {
        scope.launch {
            abastecimentos = withContext(Dispatchers.IO) {
                BancoDeDados.carregarAbastecimentos(context).filter { it.carroId == carroId }
            }
        }
    }

    val ordenados = remember(abastecimentos) {
        abastecimentos.sortedByDescending { runCatching { LocalDate.parse(it.data, formatter) }.getOrNull() }
    }
    val totalGasto = remember(ordenados) { ordenados.sumOf { it.valorPago } }
    val mediaPorRegistro = remember(ordenados) {
        if (ordenados.isEmpty()) 0.0 else totalGasto / ordenados.size.toDouble()
    }

    if (itemEdicao != null) {
        DialogEditar(
            item = itemEdicao!!,
            onDismiss = { itemEdicao = null },
            onConfirm = { itemAtualizado ->
                val novaLista = abastecimentos.map { if (it.id == itemAtualizado.id) itemAtualizado else it }
                scope.launch {
                    withContext(Dispatchers.IO) {
                        BancoDeDados.salvarAbastecimentos(context, novaLista)
                    }
                    abastecimentos = novaLista
                    itemEdicao = null
                }
            },
            formatter = formatter
        )
    }

    if (itemExcluir != null) {
        DialogExcluir(
            onDismiss = { itemExcluir = null },
            onConfirm = {
                val novaLista = abastecimentos.filter { it.id != itemExcluir!!.id }
                scope.launch {
                    withContext(Dispatchers.IO) {
                        BancoDeDados.salvarAbastecimentos(context, novaLista)
                    }
                    abastecimentos = novaLista
                    itemExcluir = null
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("Histórico", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Voltar", tint = TextWhite, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocalGasStation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Resumo de abastecimentos", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResumoChip("Registros", ordenados.size.toString(), modifier = Modifier.weight(1f))
                        ResumoChip("Total", formatarMoedaLocal(totalGasto), modifier = Modifier.weight(1f))
                        ResumoChip("Media", formatarMoedaLocal(mediaPorRegistro), modifier = Modifier.weight(1f))
                    }
                }
            }

            if (ordenados.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.LocalGasStation, null, tint = TextGray.copy(alpha = 0.3f), modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Sem registros ainda", color = TextGray, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(ordenados) { index, item ->
                        TimelineItem(
                            item = item,
                            isLast = index == ordenados.lastIndex,
                            onEdit = { itemEdicao = item },
                            onDelete = { itemExcluir = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(label, color = TextGray, fontSize = 10.sp)
        Text(value, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun TimelineItem(
    item: Abastecimento,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // COLUNA DA LINHA DO TEMPO
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentBlue)
                    .border(BorderStroke(2.dp, Color.White), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalGasStation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(AccentBlue)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // CARD DE CONTEÚDO
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.verticalGradient(colors = listOf(GradientStart, GradientEnd)))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(item.data, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Edit, null, tint = TextGray, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Delete, null, tint = AlertRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(Modifier.height(16.dp))

                    // --- RODAPÉ COM INFORMAÇÕES INVERTIDAS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. ESQUERDA: TOTAL PAGO (Agora aqui e em Negrito)
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Total Pago",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold // <-- Texto em Negrito
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = AccentGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    formatarMoedaLocal(item.valorPago),
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // 2. DIREITA: PREÇO/LITRO E LITROS (Moveram para cá)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Preço/Litro", color = TextGray, fontSize = 11.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(formatarMoedaLocal(item.precoLitro), color = TextWhite.copy(alpha = 0.9f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WaterDrop, null, tint = TextGray, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(String.format(Locale("pt", "BR"), "%.2f L", item.litros), color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ... Dialogs mantidos iguais ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEditar(
    item: Abastecimento,
    onDismiss: () -> Unit,
    onConfirm: (Abastecimento) -> Unit,
    formatter: DateTimeFormatter
) {
    val context = LocalContext.current
    var precoTexto by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", item.precoLitro)) }
    var totalTexto by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", item.valorPago)) }
    var dataSelecionada by remember { mutableStateOf(runCatching { LocalDate.parse(item.data, formatter) }.getOrNull() ?: LocalDate.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Editar Abastecimento", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = precoTexto,
                    onValueChange = { precoTexto = it },
                    label = { Text("Preço por Litro") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = TextGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalTexto,
                    onValueChange = { totalTexto = it },
                    label = { Text("Total Pago (R$)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = TextGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dataSelecionada.format(formatter),
                    onValueChange = {}, readOnly = true,
                    label = { Text("Data") },
                    trailingIcon = { Icon(Icons.Rounded.Edit, null, tint = AccentBlue) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                        focusedLabelColor = AccentBlue, unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier.fillMaxWidth().clickable {
                        DatePickerDialog(context, { _, y, m, d -> dataSelecionada = LocalDate.of(y, m + 1, d) }, dataSelecionada.year, dataSelecionada.monthValue - 1, dataSelecionada.dayOfMonth).show()
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val preco = precoTexto.replace(",", ".").toDoubleOrNull()
                val total = totalTexto.replace(",", ".").toDoubleOrNull()
                val litros = if (preco != null && total != null && preco > 0.0) total / preco else item.litros
                onConfirm(item.copy(data = dataSelecionada.format(formatter), precoLitro = preco ?: item.precoLitro, valorPago = total ?: item.valorPago, litros = litros))
            }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Salvar", color = TextWhite) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextGray) } }
    )
}

@Composable
fun DialogExcluir(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceDark,
        title = { Text("Excluir Registro", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = { Text("Tem certeza? O valor será removido dos cálculos mensais.", color = TextGray) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) { Text("Excluir", color = TextWhite) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextGray) } }
    )
}
