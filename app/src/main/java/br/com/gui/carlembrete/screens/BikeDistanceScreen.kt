package br.com.gui.carlembrete

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDistanceScreen(
    carroId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")) }

    // Estados do formulário
    var dataText by remember { mutableStateOf(LocalDate.now().format(formatter)) }
    var kmText by remember { mutableStateOf("") }

    // Cores personalizadas para o tema Dark
    val backgroundColor = Color(0xFF0B1120) // Fundo bem escuro
    val surfaceColor = Color(0xFF1E293B)    // Cor do Card (Slate 800)
    val primaryColor = Color(0xFF22C55E)    // Verde Neon
    val textColor = Color(0xFFF1F5F9)       // Branco gelo

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nova Pedalada",
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Voltar",
                            tint = textColor
                        )
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
                .padding(24.dp), // Mais respiro nas bordas
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Ícone de Destaque no topo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card do Formulário
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Registrar Treino",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                    // Campo Data
                    OutlinedTextField(
                        value = dataText,
                        onValueChange = { dataText = it },
                        label = { Text("Data") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = primaryColor)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = primaryColor
                        )
                    )

                    // Campo Distância
                    OutlinedTextField(
                        value = kmText,
                        onValueChange = { kmText = it.replace(",", ".") },
                        label = { Text("Distância (km)") },
                        leadingIcon = {
                            Icon(Icons.Default.Straighten, contentDescription = null, tint = primaryColor)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = primaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão de Ação
                    Button(
                        onClick = {
                            val km = kmText.toDoubleOrNull()
                            if (km == null || km <= 0.0) {
                                Toast.makeText(context, "Informe uma distância válida.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val data = runCatching { LocalDate.parse(dataText, formatter) }.getOrNull()
                            if (data == null) {
                                Toast.makeText(context, "Informe uma data válida.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch(Dispatchers.IO) {
                                val novo = Pedalada(
                                    carroId = carroId,
                                    data = data.format(formatter),
                                    km = km
                                )
                                val atual = BancoDeDados.carregarPedaladas(context).toMutableList()
                                atual.add(novo)
                                BancoDeDados.salvarPedaladas(context, atual)
                                withContext(Dispatchers.Main) {
                                    kmText = ""
                                    Toast.makeText(context, "Distância registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // Botão um pouco mais alto
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("SALVAR REGISTRO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}