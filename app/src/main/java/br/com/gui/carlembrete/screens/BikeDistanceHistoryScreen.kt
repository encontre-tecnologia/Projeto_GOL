package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDistanceHistoryScreen(
    carroId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pedaladas by remember { mutableStateOf<List<Pedalada>>(emptyList()) }
    var deleteItem by remember { mutableStateOf<Pedalada?>(null) }

    // Cores do Tema
    val backgroundColor = Color(0xFF0B1120)
    val cardColor = Color(0xFF1E293B)
    val accentColor = Color(0xFF22C55E)
    val dangerColor = Color(0xFFEF4444)
    val textColor = Color(0xFFF1F5F9)
    val subTextColor = Color(0xFF94A3B8)

    LaunchedEffect(Unit) {
        pedaladas = withContext(Dispatchers.IO) {
            BancoDeDados.carregarPedaladas(context).filter { it.carroId == carroId }
        }
    }

    // Modal de ConfirmaÃ§Ã£o
    if (deleteItem != null) {
        AlertDialog(
            onDismissRequest = { deleteItem = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = dangerColor) },
            title = { Text(tr("Excluir Registro?", "Delete record?"), color = textColor, fontWeight = FontWeight.Bold) },
            text = { Text(tr("Essa ação removerá permanentemente este registro do histórico.", "This action will permanently remove this history record."), color = subTextColor, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        val alvo = deleteItem
                        deleteItem = null
                        if (alvo != null) {
                            scope.launch(Dispatchers.IO) {
                                val atual = BancoDeDados.carregarPedaladas(context).toMutableList()
                                atual.removeAll { it.id == alvo.id }
                                BancoDeDados.salvarPedaladas(context, atual)
                                pedaladas = atual.filter { it.carroId == carroId }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = dangerColor)
                 ) { Text(tr("Excluir", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { deleteItem = null }) {
                    Text(tr("Cancelar", "Cancel"), color = subTextColor)
                }
            },
            containerColor = cardColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("HistÃ³rico", color = textColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Card de Resumo Total
            if (pedaladas.isNotEmpty()) {
                val totalKm = pedaladas.sumOf { it.km }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Total Acumulado", color = subTextColor, fontSize = 12.sp)
                            Text(
                                text = "${String.format("%.1f", totalKm)} km",
                                color = accentColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }

            // Lista ou Estado Vazio
            if (pedaladas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nenhum registro ainda.",
                            color = subTextColor,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pedaladas.sortedByDescending { it.data }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bolinha com a data (estilo calendÃ¡rio)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF334155), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val parts = item.data.split("/")
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (parts.size >= 2) {
                                            Text(parts[0], color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(parts[1], color = subTextColor, fontSize = 10.sp)
                                        } else {
                                            Text("ðŸ“…", fontSize = 14.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // InformaÃ§Ã£o de KM
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${String.format("%.2f", item.km)} km",
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "DistÃ¢ncia percorrida",
                                        color = subTextColor,
                                        fontSize = 12.sp
                                    )
                                }

                                // BotÃ£o Deletar
                                IconButton(onClick = { deleteItem = item }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Excluir",
                                        tint = dangerColor.copy(alpha = 0.8f)
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
