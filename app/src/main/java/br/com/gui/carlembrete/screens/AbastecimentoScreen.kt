package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbastecimentoScreen(onDismiss: () -> Unit) {
    val primaryDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
    var precoGasolina by remember { mutableStateOf("") }
    var valorAbastecido by remember { mutableStateOf("") }
    val preco = precoGasolina.replace(",", ".").toDoubleOrNull()
    val total = valorAbastecido.replace(",", ".").toDoubleOrNull()
    val litros = if (preco != null && total != null && preco > 0) total / preco else null
    val litrosTexto = litros?.let { String.format(Locale("pt", "BR"), "%.2f L", it) } ?: "--"
    val gastoTexto = total?.let { formatarMoeda(it) } ?: "--"

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0B1224))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalGasStation,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Dados do posto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }

            Spacer(Modifier.height(4.dp))

            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = precoGasolina,
                        onValueChange = { precoGasolina = it },
                        label = { Text("Valor da gasolina (R$/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = valorAbastecido,
                        onValueChange = { valorAbastecido = it },
                        label = { Text("Valor abastecido (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            ElevatedCard(
                colors = CardDefaults.cardColors(containerColor = surfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumo", color = Color.White, fontWeight = FontWeight.Bold)
                    InfoRow("Litros calculados", litrosTexto, Color.White, Color(0xFF94A3B8))
                    InfoRow("Gasto no posto", gastoTexto, Color.White, Color(0xFF94A3B8))
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Concluir", color = Color.White, fontWeight = FontWeight.Bold)
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
