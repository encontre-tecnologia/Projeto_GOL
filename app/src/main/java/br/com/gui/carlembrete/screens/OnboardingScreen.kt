package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.gui.carlembrete.R
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/* ----------------- ONBOARDING ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var carroNome by remember { mutableStateOf("") }
    var carroMarca by remember { mutableStateOf("") }
    var carroModeloUnico by remember { mutableStateOf("") }
    var carroKm by remember { mutableStateOf("") }
    var carroTipo by remember { mutableStateOf(TipoVeiculo.CARRO) }
    var frotaTemporaria by remember { mutableStateOf(listOf<CarroInfo>()) }
    var contatosAdicionados by remember { mutableStateOf(listOf<ContatoProfissional>()) }
    var showContatoDialog by remember { mutableStateOf(false) }

    if (showContatoDialog) NovoContatoDialog(onDismiss = { showContatoDialog = false }, onSalvar = { novo -> contatosAdicionados = contatosAdicionados + novo; scope.launch(Dispatchers.IO) { BancoDeDados.salvarContatos(context, contatosAdicionados) }; showContatoDialog = false })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2A4A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(targetState = step, transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }, label = "onboarding") { currentStep ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when (currentStep) {
                    1 -> {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(120.dp))
                        Spacer(Modifier.height(32.dp)); Text("Bem-vindo ao\nCarLembrete", style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp)); Text("Gerencie sua frota, tire fotos das notas e tenha seus mecânicos sempre à mão.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(48.dp)); Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) { Text("Começar", fontSize = 18.sp) }
                    }
                    2 -> {
                        if (frotaTemporaria.isNotEmpty()) {
                            Text("Carros Adicionados:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(frotaTemporaria) { c ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            painter = painterResource(id = c.tipoIconRes()),
                                            contentDescription = c.tipoVeiculo.label,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = c.nome,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.height(170.dp).width(160.dp)) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(160.dp).align(Alignment.BottomCenter))
                            Box(modifier = Modifier.size(width = 38.dp, height = 58.dp).background(Color(0xFF4B5563), shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).align(Alignment.BottomCenter).padding(bottom = 2.dp))
                            Image(
                                painter = painterResource(id = carroTipo.iconRes),
                                contentDescription = carroTipo.label,
                                modifier = Modifier.size(100.dp).align(Alignment.BottomCenter).offset(y = (-4).dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp)); Text("Sua Garagem", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = carroNome, onValueChange = { carroNome = it }, label = { Text("Apelido (ex: Fox do Gui)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        var marcaExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = marcaExpanded, onExpandedChange = { marcaExpanded = !marcaExpanded }) {
                            OutlinedTextField(
                                value = carroMarca,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Marca") },
                                placeholder = { Text("Selecione a marca") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = marcaExpanded) },
                                leadingIcon = {
                                    val logoMarca = logoResForMarca(carroMarca)
                                    if (logoMarca != null) {
                                        Image(
                                            painter = painterResource(id = logoMarca),
                                            contentDescription = carroMarca,
                                            modifier = Modifier.size(24.dp),
                                            colorFilter = ColorFilter.tint(Color.White)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color(0xFF3B82F6))
                                    }
                                }
                            )
                            ExposedDropdownMenu(expanded = marcaExpanded, onDismissRequest = { marcaExpanded = false }) {
                                marcasSuportadas.forEach { marcaNome ->
                                    DropdownMenuItem(
                                        text = { Text(marcaNome) },
                                        onClick = {
                                            carroMarca = marcaNome
                                            marcaExpanded = false
                                        },
                                        leadingIcon = {
                                            val res = logoResForMarca(marcaNome)
                                            if (res != null) {
                                                Image(
                                                    painter = painterResource(id = res),
                                                    contentDescription = marcaNome,
                                                    modifier = Modifier.size(20.dp),
                                                    colorFilter = ColorFilter.tint(Color.White)
                                                )
                                            } else {
                                                Icon(Icons.Rounded.DirectionsCar, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = carroModeloUnico, onValueChange = { carroModeloUnico = it }, label = { Text("Modelo e Motor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = carroKm,
                            onValueChange = { if (it.all(Char::isDigit)) carroKm = it },
                            label = { Text("KM Atual") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        TipoVeiculoSelector(
                            selecionado = carroTipo,
                            onSelect = { carroTipo = it }
                        )
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                if (carroNome.isNotBlank() && carroModeloUnico.isNotBlank()) {
                                    val novo = CarroInfo(
                                        nome = carroNome,
                                        modelo = carroModeloUnico,
                                        marca = carroMarca,
                                        kmAtual = carroKm.toIntOrNull() ?: 0,
                                        tipoVeiculo = carroTipo
                                    )
                                    frotaTemporaria = frotaTemporaria + novo
                                    carroNome = ""
                                    carroMarca = ""
                                    carroModeloUnico = ""
                                    carroKm = ""
                                    carroTipo = TipoVeiculo.CARRO
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6))
                        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar Outro Carro") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                var listaFinal = frotaTemporaria
                                if (carroNome.isNotBlank() || carroModeloUnico.isNotBlank() || carroMarca.isNotBlank()) {
                                    val ultimo = CarroInfo(
                                        nome = if(carroNome.isBlank()) "Carro" else carroNome,
                                        modelo = carroModeloUnico,
                                        marca = carroMarca,
                                        kmAtual = carroKm.toIntOrNull() ?: 0,
                                        tipoVeiculo = carroTipo
                                    )
                                    listaFinal = listaFinal + ultimo
                                }
                                val listaSalvar = if (listaFinal.isNotEmpty()) listaFinal else listOf(CarroInfo(nome = "Meu Carro", modelo = "Padrão", marca = "Marca", kmAtual = 0))
                                scope.launch(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaSalvar) }
                                step = 3
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) { Text("Salvar e Continuar", fontSize = 18.sp) }
                    }
                    3 -> {
                        Text("Profissionais", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp)); Text("Quem cuida dos seus carros?", color = Color(0xFF94A3B8))
                        Spacer(Modifier.height(24.dp))
                        if (contatosAdicionados.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("Nenhum contato adicionado", color = Color(0xFF64748B)) } }
                        else { LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) { items(contatosAdicionados) { c -> Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.padding(bottom = 8.dp)) { Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = Color(0xFFF59E0B)); Spacer(Modifier.width(12.dp)); Column { Text(c.nome, color = Color.White, fontWeight = FontWeight.Bold); Text(c.tipoServico, color = Color(0xFF94A3B8), fontSize = 12.sp) } } } } } }
                        Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = { showContatoDialog = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Adicionar Profissional") }
                        Spacer(Modifier.height(48.dp)); Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) { Text("Finalizar e Entrar", fontSize = 18.sp) }
                    }
                }
            }
        }
    }
}
