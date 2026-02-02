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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
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
import br.com.gui.carlembrete.VehicleIcon
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
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

private const val pecaOutraLabel = "Outra (digitar)"
private const val OCR_FREE_LIMIT = 3
private val pecasSugestao = listOf(
    "Óleo do motor",
    "Filtro de óleo",
    "Filtro de ar",
    "Filtro de combustível",
    "Pastilha de freio",
    "Disco de freio",
    "Fluido de freio",
    "Bateria",
    "Pneus",
    "Amortecedor",
    "Correia dentada",
    "Embreagem",
    "Velas",
    "Radiador",
    "Fluido de arrefecimento",
    "Limpador de para-brisa",
    "Lâmpadas",
    "Suspensão",
    "Ar condicionado",
    "Outra (digitar)"
)

@Composable
fun PrivacidadeTermosDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
        shape = dialogCornerShape,
        title = { Text("Privacidade e Termos", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Os dados ficam armazenados apenas neste dispositivo para gerar lembretes personalizados.", color = Color(0xFF94A3B8))
                Text("Ao continuar, você concorda com o uso das informações para criar recomendações e notificações.", color = Color(0xFF94A3B8))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun NovoContatoDialog(onDismiss: () -> Unit, onSalvar: (ContatoProfissional) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
        shape = dialogCornerShape,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = { Text("Novo Profissional", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo (Ex: Mecânico)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { if (it.all(Char::isDigit)) telefone = it },
                    label = { Text("WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nome.isNotBlank()) onSalvar(ContatoProfissional(nome = nome, telefone = telefone, tipoServico = tipo)) },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = dialogActionButtonShape
            ) { Text("Salvar", fontSize = 18.sp) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(56.dp)
            ) { Text("Cancelar", fontSize = 18.sp) }
        }
    )
}

@Composable
fun NotificacaoRapidaDialog(onDismiss: () -> Unit, onDisparar: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .border(dialogBorderStroke, dialogCornerShape),
                shape = dialogCornerShape,
                color = Color(0xFF0F172A)
            ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Disparo de notificação",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Use este botão para enviar uma notificação imediata e validar as permissões do aparelho.",
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        onDisparar()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Disparar agora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembreteDetalhesDialog(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    carro: CarroInfo,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSalvar: (Lembrete) -> Unit
) {
    val context = LocalContext.current
    var isEditando by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf(lembrete.titulo) }
    var tipoSelecionado by remember { mutableStateOf(lembrete.tipo) }
    var dataAviso by remember { mutableStateOf(lembrete.dataLimite) }
    var horaAviso by remember { mutableStateOf(lembrete.horaAviso) }
    var kmLimite by remember { mutableStateOf(lembrete.kmLimite) }
    var valorTexto by remember { mutableStateOf(if (lembrete.valor > 0) lembrete.valor.toString() else "") }
    var menuExpanded by remember { mutableStateOf(false) }
    val pecasDisponiveis = pecasSugestao
    var pecaSelecionada by remember {
        mutableStateOf(
            if (lembrete.peca.isNotBlank() && pecasDisponiveis.contains(lembrete.peca)) {
                lembrete.peca
            } else if (lembrete.peca.isNotBlank()) {
                pecaOutraLabel
            } else {
                ""
            }
        )
    }

    LaunchedEffect(lembrete) {
        titulo = lembrete.titulo
        tipoSelecionado = lembrete.tipo
        dataAviso = lembrete.dataLimite
        horaAviso = lembrete.horaAviso
        kmLimite = lembrete.kmLimite
        valorTexto = if (lembrete.valor > 0) lembrete.valor.toString() else ""
        pecaSelecionada =
            if (lembrete.peca.isNotBlank() && pecasDisponiveis.contains(lembrete.peca)) lembrete.peca
            else if (lembrete.peca.isNotBlank()) pecaOutraLabel
            else ""
        isEditando = false
    }

    fun abrirDatePickerEdit() {
        val atual = try { LocalDate.parse(dataAviso, DateTimeFormatter.ofPattern("dd/MM/yyyy")) } catch (e: Exception) { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dataAviso = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
            },
            atual.year,
            atual.monthValue - 1,
            atual.dayOfMonth
        ).show()
    }

    fun abrirTimePickerEdit() {
        val partes = horaAviso.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 9
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute -> horaAviso = "%02d:%02d".format(hour, minute) },
            hora,
            minuto,
            true
        ).show()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .border(dialogBorderStroke, dialogCornerShape),
                shape = dialogCornerShape,
                color = Color(0xFF0B1729)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TipoIcon(
                                tipo = lembrete.tipo,
                                tint = corCategoria(lembrete.tipo),
                                size = 20.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(lembrete.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text(lembrete.tipo.label, color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                        if (!isEditando) {
                            IconButton(
                                onClick = { isEditando = true },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar aviso", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1F2A44))
                    if (isEditando) {
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("Título do aviso") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenuBox(
                            expanded = menuExpanded,
                            onExpandedChange = { menuExpanded = !menuExpanded }
                        ) {
                            OutlinedTextField(
                                value = tipoSelecionado.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoria") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                leadingIcon = {
                                    TipoIcon(
                                        tipo = tipoSelecionado,
                                        tint = corCategoria(tipoSelecionado),
                                        size = 18.dp
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) }
                            )
                            ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                TipoManutencao.values().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.label) },
                                        onClick = {
                                            tipoSelecionado = t
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            TipoIcon(
                                                tipo = t,
                                                tint = corCategoria(t),
                                                size = 18.dp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dataAviso,
                                onValueChange = {},
                                modifier = Modifier.weight(1f).clickable { abrirDatePickerEdit() },
                                readOnly = true,
                                label = { Text("Data") },
                                trailingIcon = {
                                    IconButton(onClick = { abrirDatePickerEdit() }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = horaAviso,
                                onValueChange = {},
                                modifier = Modifier.weight(1f).clickable { abrirTimePickerEdit() },
                                readOnly = true,
                                label = { Text("Hora") },
                                trailingIcon = {
                                    IconButton(onClick = { abrirTimePickerEdit() }) {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                    }
                                }
                            )
                        }
                        OutlinedTextField(
                            value = kmLimite,
                            onValueChange = { if (it.all(Char::isDigit)) kmLimite = it },
                            label = { Text("KM limite") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = valorTexto,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == ',' }) valorTexto = it.replace(',', '.') },
                            label = { Text("Valor (R$)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        SeletorPeca(
                            pecaSelecionada = pecaSelecionada,
                            onSelecionar = { pecaSelecionada = it }
                        )
                    } else {
                        val infoItems = buildList {
                            add("Veículo" to carro.nome)
                            add("Data do aviso" to lembrete.dataLimite.ifBlank { "Sem data" })
                            add("Hora do aviso" to lembrete.horaAviso)
                            add("KM limite" to lembrete.kmLimite.ifBlank { "Não definido" })
                            if (lembrete.peca.isNotBlank()) add("Peça" to lembrete.peca)
                            if (lembrete.valor > 0) add("Valor" to formatarMoeda(lembrete.valor))
                            contato?.let { add("Profissional" to "${it.nome} (${it.tipoServico})") }
                            lembrete.fotoPath?.let { add("Anexo" to "Foto disponível") }
                        }
                        infoItems.chunked(2).forEach { linhaInfos ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                linhaInfos.forEach { (label, valor) ->
                                    InfoLinha(label = label, valor = valor, modifier = Modifier.weight(1f))
                                }
                                if (linhaInfos.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isEditando) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    titulo = lembrete.titulo
                                    dataAviso = lembrete.dataLimite
                                    horaAviso = lembrete.horaAviso
                                    kmLimite = lembrete.kmLimite
                                    valorTexto = if (lembrete.valor > 0) lembrete.valor.toString() else ""
                                    pecaSelecionada =
                                        if (lembrete.peca.isNotBlank() && pecasDisponiveis.contains(lembrete.peca)) lembrete.peca
                                        else if (lembrete.peca.isNotBlank()) pecaOutraLabel
                                        else ""
                                    isEditando = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                border = BorderStroke(0.2.dp, Color.White.copy(alpha = 0.7f)),
                                shape = dialogActionButtonShape
                            ) { Text("Cancelar", fontSize = 18.sp) }
                                Button(
                                    onClick = {
                                        val novoValor = valorTexto.toDoubleOrNull() ?: 0.0
                                        val pecaFinal = when {
                                            pecaSelecionada == pecaOutraLabel -> titulo.trim()
                                            pecaSelecionada.isBlank() -> ""
                                            else -> pecaSelecionada
                                        }
                                        val atualizado = lembrete.copy(
                                            titulo = titulo.ifBlank { lembrete.titulo },
                                            tipo = tipoSelecionado,
                                            dataLimite = dataAviso.ifBlank { lembrete.dataLimite },
                                            horaAviso = horaAviso.ifBlank { lembrete.horaAviso },
                                            kmLimite = kmLimite,
                                            valor = novoValor,
                                            peca = pecaFinal
                                        )
                                    onSalvar(atualizado)
                                    isEditando = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = dialogActionButtonShape
                            ) { Text("Salvar alterações", fontSize = 18.sp) }
                        }
                    } else {
                        Button(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = dialogActionButtonShape
                        ) { Text("Apagar aviso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.6f)),
                            shape = dialogActionButtonShape
                        ) { Text("Fechar", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLinha(label: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(valor, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatarKm(valor: Int): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(valor)

private fun formatarKmTexto(texto: String): String {
    val digits = texto.filter(Char::isDigit)
    val value = digits.toLongOrNull() ?: 0L
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}

private fun filtrarTextosDetectados(linhas: List<String>): List<String> {
    val regexSomenteDigitos = Regex("^\\d{1,6}$")
    val regexViscosidade = Regex("\\b\\d{1,2}W-?\\d{2}\\b", RegexOption.IGNORE_CASE)
    val regexCodigoAlfanumerico = Regex("\\b[A-Z]{2,}\\s?\\d{2,}\\b")
    val termosGenericos = setOf(
        "OLEO", "LUBRIFICANTE", "PARA", "MOTOR", "FLEX",
        "SEMISSINTETICO", "SINTETICO", "MINERAL", "COMBUSTIVEL"
    )
    val termosEspecificacao = setOf(
        "API", "SAE", "ILSAC", "SL", "SM", "SN", "SP", "CJ", "CF", "CI", "SJ"
    )
    return linhas
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotBlank() }
        .filter { !padraoUrlOuContato.containsMatchIn(it.uppercase(Locale.ROOT)) }
        .filter { !isTextoPromocional(it) }
        .filter { texto ->
            if (texto.length < 4) return@filter false
            if (regexSomenteDigitos.matches(texto)) return@filter false
            val upper = texto.uppercase(Locale.ROOT)
            val normalizado = upper.unaccent()
            val tokens = normalizado.split(" ").filter { it.isNotBlank() }
            if (!normalizado.contains("MOTOROIL")) {
                val apenasGenerico = tokens.isNotEmpty() && tokens.all { it in termosGenericos }
                val apenasEspecificacao = tokens.isNotEmpty() && tokens.all { it in termosEspecificacao }
                if (apenasGenerico || apenasEspecificacao) return@filter false
                if (normalizado.contains("OLEO PARA") || normalizado.contains("PARA MOTOR")) return@filter false
            }
            val letras = normalizado.count { it.isLetter() }
            val digitos = normalizado.count { it.isDigit() }
            val uppercaseRatio = if (letras > 0) normalizado.count { it.isUpperCase() }.toFloat() / letras else 0f
            val pareceCodigo = regexCodigoAlfanumerico.containsMatchIn(normalizado) || regexViscosidade.containsMatchIn(normalizado)
            if (!pareceCodigo && letras < 3) return@filter false
            if (!pareceCodigo && digitos > letras * 2) return@filter false
            if (!pareceCodigo && uppercaseRatio < 0.5f && letras < 4) return@filter false
            val naoAlfanumericos = texto.count { !it.isLetterOrDigit() && !it.isWhitespace() }
            if (naoAlfanumericos > 3) return@filter false
            val charsSemEspaco = texto.filterNot { it.isWhitespace() }
            val diversidade = charsSemEspaco.toSet().size
            if (charsSemEspaco.length <= 6 && diversidade <= 2) return@filter false
            true
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorPeca(
    pecaSelecionada: String,
    onSelecionar: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = pecaSelecionada,
            onValueChange = {},
            readOnly = true,
            label = { Text("Peça") },
            placeholder = { Text("Selecione a peça") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            pecasSugestao.forEach { peca ->
                DropdownMenuItem(
                    text = { Text(peca) },
                    onClick = {
                        onSelecionar(peca)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoAgendamentoDialog(
    carroAtual: CarroInfo,
    contatosDisponiveis: List<ContatoProfissional>,
    onDismiss: () -> Unit,
    onConfirm: (Lembrete) -> Unit,
    onMultiConfirm: (List<Lembrete>) -> Unit,
    onUpdateKmCarro: (Int) -> Unit,
    autoAbrirCamera: Boolean = false,
    onAutoCameraConsumida: () -> Unit = {},
    onAddContato: (ContatoProfissional) -> Unit = {},
    initialTipo: TipoManutencao = TipoManutencao.OLEO,
    planTier: PlanTier,
    onRequestPremium: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val isPremium = planTier != PlanTier.FREE
    var descricao by remember { mutableStateOf("") }
    var localServicoInput by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var kmBase by remember { mutableStateOf(if (carroAtual.kmAtual > 0) carroAtual.kmAtual.toString() else "") }
    var valorInput by remember { mutableStateOf("") }
    var tipoSelecionado by remember { mutableStateOf(initialTipo) }
    var contatosLista by remember { mutableStateOf(contatosDisponiveis) }
    var contatoSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    var listaItensDetectados by remember { mutableStateOf<List<ItemDetectado>>(emptyList()) }
    var isModoLista by remember { mutableStateOf(false) }
    var qrPossuiItensSeparaveis by remember { mutableStateOf(false) }
    var qrModoSeparado by remember { mutableStateOf(false) }
    var descricaoQrConsolidada by remember { mutableStateOf("") }
    var itemDataAvisoOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemHoraAvisoOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemValorOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showKmConfirmDialog by remember { mutableStateOf(false) }
    var kmDetectadoParaConfirmar by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var tipoMenuItemId by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var fotoCaminho by remember { mutableStateOf<String?>(null) }
    var horaNotificacao by remember { mutableStateOf("09:00") }
    var dataAviso by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var avisoPersonalizado by remember { mutableStateOf(false) }
    var etapaAtual by remember { mutableStateOf(1) }
    val descricaoFocusRequester = remember { FocusRequester() }
    var textosDetectados by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTextosDialog by remember { mutableStateOf(false) }
    var textoSelecionadoDialog by remember { mutableStateOf<String?>(null) }
    var showMarcaDialog by remember { mutableStateOf(false) }
    var produtoSelecionadoDialog by remember { mutableStateOf<String?>(null) }
    var marcaSelecionadaDialog by remember { mutableStateOf<String?>(null) }
    var isQrLoading by remember { mutableStateOf(false) }
    var qrNomeEstabelecimento by remember { mutableStateOf("") }
    var qrEnderecoEstabelecimento by remember { mutableStateOf("") }
    var descricaoAntesDialog by remember { mutableStateOf("") }
    var tipoAntesDialog by remember { mutableStateOf(TipoManutencao.OLEO) }
    var novoContatoNome by remember { mutableStateOf("") }
    var novoContatoTelefone by remember { mutableStateOf("") }
    val dataFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scope = rememberCoroutineScope()
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val textoReconhecido = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!textoReconhecido.isNullOrBlank()) {
                descricao = textoReconhecido
                tipoSelecionado = detectarTipoPeloTexto(textoReconhecido)
            }
        }
    }

    fun tentarAbrirCamera() {
        showCamera = true
    }

    LaunchedEffect(autoAbrirCamera) {
        if (autoAbrirCamera) {
            tentarAbrirCamera()
            onAutoCameraConsumida()
        }
    }

    LaunchedEffect(isModoLista, etapaAtual) {
        if (!isModoLista && etapaAtual == 1) {
            descricaoFocusRequester.requestFocus()
        }
    }

    fun adicionarKm(valor: Int) { val kmBaseInt = kmBase.toIntOrNull() ?: carroAtual.kmAtual; kmBase = (kmBaseInt + valor).toString() }
    LaunchedEffect(data, tipoSelecionado) {
        if (!avisoPersonalizado) {
            val dataBase = try { LocalDate.parse(data, dataFormatter) } catch (e: Exception) { LocalDate.now() }
            dataAviso = calcularProximaData(tipoSelecionado, dataBase)
        }
    }

    fun abrirDatePicker(dataAtual: String, aoSelecionar: (String) -> Unit) {
        val atual = try { LocalDate.parse(dataAtual, dataFormatter) } catch (e: Exception) { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                aoSelecionar("%02d/%02d/%04d".format(dayOfMonth, month + 1, year))
            },
            atual.year,
            atual.monthValue - 1,
            atual.dayOfMonth
        ).show()
    }

    fun abrirTimePicker(horaAtual: String, aoSelecionar: (String) -> Unit) {
        val partes = horaAtual.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 9
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute -> aoSelecionar("%02d:%02d".format(hour, minute)) },
            hora,
            minuto,
            true
        ).show()
    }

    fun iniciarCapturaVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Descreva o serviço realizado")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Recursos de voz indisponíveis", Toast.LENGTH_SHORT).show()
        }
    }

fun adicionarContatoManual() {
        val nome = novoContatoNome.trim()
        val telefone = novoContatoTelefone.filter(Char::isDigit)
        if (nome.isBlank() || telefone.isBlank()) {
            Toast.makeText(context, "Informe nome e telefone", Toast.LENGTH_SHORT).show()
            return
        }
        val novoContato = ContatoProfissional(
            nome = nome,
            telefone = telefone,
            tipoServico = "Contato manual"
        )
        contatosLista = contatosLista + novoContato
        onAddContato(novoContato)
        contatoSelecionado = novoContato
        novoContatoNome = ""
        novoContatoTelefone = ""
        Toast.makeText(context, "Profissional adicionado", Toast.LENGTH_SHORT).show()
    }

    fun enviarMensagemWhatsapp(contato: ContatoProfissional) {
        val telefone = contato.telefone.filter(Char::isDigit)
        if (telefone.isBlank()) {
            Toast.makeText(context, "Telefone inválido", Toast.LENGTH_SHORT).show()
            return
        }
        val kmInfo = kmBase.ifBlank { "não informado" }
        val mensagem = "Olá ${contato.nome}, a última manutenção foi registrada em $data com $kmInfo km. Podemos agendar a próxima?"
        val uri = Uri.parse("https://wa.me/$telefone?text=${URLEncoder.encode(mensagem, "UTF-8")}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    fun salvarAvisos() {
        val kmAtualBase = kmBase.toIntOrNull() ?: 0
        if (kmAtualBase > carroAtual.kmAtual) onUpdateKmCarro(kmAtualBase)
        val dataAvisoStr = dataAviso
        if (isModoLista) {
            val novosLembretes = listaItensDetectados.flatMap { item ->
                val rep = maxOf(1, item.quantidade)
                val kmFuturo = (kmAtualBase + getKmAdicionalPorTipo(item.tipo)).toString()
                val dataItem = itemDataAvisoOverrides[item.id] ?: dataAvisoStr
                val horaItem = itemHoraAvisoOverrides[item.id] ?: horaNotificacao
                val valorItem = itemValorOverrides[item.id]?.toDoubleOrNull() ?: item.valor
                (1..rep).map { indice ->
                    val tituloFormatado = if (rep > 1) "${item.nome} (${indice}/$rep)" else item.nome
                    Lembrete(
                        titulo = tituloFormatado,
                        peca = item.nome,
                        dataLimite = dataItem,
                        kmLimite = kmFuturo,
                        tipo = item.tipo,
                        valor = valorItem,
                        carroId = "",
                        contatoId = contatoSelecionado?.id,
                        fotoPath = fotoCaminho,
                        horaAviso = horaItem,
                        estabelecimentoNome = qrNomeEstabelecimento,
                        estabelecimentoEndereco = qrEnderecoEstabelecimento
                    )
                }
            }
            novosLembretes.forEach { NotificacaoHelper.agendarNotificacao(appContext, it, it.horaAviso) }
            onMultiConfirm(novosLembretes)
        } else if (descricao.isNotBlank()) {
            val tituloLembrete = localServicoInput.ifBlank { qrNomeEstabelecimento.ifBlank { descricao } }
            val novoLembrete = Lembrete(
                titulo = tituloLembrete,
                peca = descricao.trim(),
                dataLimite = dataAvisoStr,
                kmLimite = (kmAtualBase + getKmAdicionalPorTipo(tipoSelecionado)).toString(),
                tipo = tipoSelecionado,
                valor = valorInput.toDoubleOrNull() ?: 0.0,
                carroId = "",
                contatoId = contatoSelecionado?.id,
                fotoPath = fotoCaminho,
                horaAviso = horaNotificacao,
                estabelecimentoNome = qrNomeEstabelecimento,
                estabelecimentoEndereco = qrEnderecoEstabelecimento
            )
            NotificacaoHelper.agendarNotificacao(appContext, novoLembrete, horaNotificacao)
            onConfirm(novoLembrete)
        }
    }

    if (showCamera) {
        CameraCapturaDialog(onDismiss = { showCamera = false }, onFotoCapturada = { resultado ->
            if (!isPremium) {
                AppPreferences.incrementOcrCount(context)
            }
            fotoCaminho = resultado.arquivoFoto.absolutePath
            val qrUrl = resultado.qrCodeUrl?.trim()
            Log.i(QR_PARSER_TAG, "Camera retorno => qrUrl=$qrUrl foto=${resultado.arquivoFoto.name}")
            if (!qrUrl.isNullOrBlank()) {
                isModoLista = false
                textosDetectados = emptyList()
                showTextosDialog = false
                showMarcaDialog = false
                showCamera = false
                isQrLoading = true
                scope.launch {
                    val notaInfo = consultarNotaPorQrCode(qrUrl)
                    isQrLoading = false
                    if (notaInfo == null) {
                        qrNomeEstabelecimento = ""
                        qrEnderecoEstabelecimento = ""
                        localServicoInput = ""
                        qrPossuiItensSeparaveis = false
                        qrModoSeparado = false
                        descricaoQrConsolidada = ""
                        itemDataAvisoOverrides = emptyMap()
                        itemHoraAvisoOverrides = emptyMap()
                        itemValorOverrides = emptyMap()
                        Toast.makeText(context, "QR lido, mas não foi possível carregar a nota.", Toast.LENGTH_LONG).show()
                    } else {
                        val valorExtraido = notaInfo.valorTotal
                        val dataExtraida = notaInfo.dataCompra

                        if (valorExtraido != null) {
                            valorInput = String.format(Locale.US, "%.2f", valorExtraido)
                        }
                        if (!dataExtraida.isNullOrBlank()) {
                            data = dataExtraida
                            dataAviso = dataExtraida
                            avisoPersonalizado = true
                        }
                        qrNomeEstabelecimento = notaInfo.nomeEstabelecimento.orEmpty()
                        qrEnderecoEstabelecimento = notaInfo.enderecoEstabelecimento.orEmpty()
                        val descricaoExtraida = notaInfo.descricaoItens?.trim()
                        val itensQr = extrairItensDaDescricaoQr(descricaoExtraida)
                        qrPossuiItensSeparaveis = itensQr.size > 1
                        qrModoSeparado = false
                        if (itensQr.isNotEmpty()) {
                            listaItensDetectados = itensQr
                            itemDataAvisoOverrides = itensQr.associate { it.id to dataAviso }
                            itemHoraAvisoOverrides = itensQr.associate { it.id to horaNotificacao }
                            itemValorOverrides = itensQr.associate { it.id to String.format(Locale.US, "%.2f", it.valor) }
                        } else {
                            itemDataAvisoOverrides = emptyMap()
                            itemHoraAvisoOverrides = emptyMap()
                            itemValorOverrides = emptyMap()
                        }
                        localServicoInput = montarLocalNota(
                            estabelecimento = qrNomeEstabelecimento,
                            endereco = qrEnderecoEstabelecimento
                        )
                        descricaoQrConsolidada = montarDescricaoItensNota(
                            total = valorExtraido,
                            itens = descricaoExtraida
                        )
                        descricao = descricaoQrConsolidada
                        isModoLista = false

                        Log.i(
                            QR_PARSER_TAG,
                            "Bind UI QR => estabelecimento=$qrNomeEstabelecimento endereco=$qrEnderecoEstabelecimento descricao=$descricao valorInput=$valorInput data=$data dataAviso=$dataAviso"
                        )
                        Toast.makeText(context, "Dados da nota carregados pelo QR Code.", Toast.LENGTH_SHORT).show()
                    }
                }
                return@CameraCapturaDialog
            }
            Log.w(QR_PARSER_TAG, "QR nao detectado na captura. Seguindo fluxo de fallback da foto.")
            qrNomeEstabelecimento = ""
            qrEnderecoEstabelecimento = ""
            localServicoInput = ""
            qrPossuiItensSeparaveis = false
            qrModoSeparado = false
            descricaoQrConsolidada = ""
            itemDataAvisoOverrides = emptyMap()
            itemHoraAvisoOverrides = emptyMap()
            itemValorOverrides = emptyMap()
            textosDetectados = filtrarTextosDetectados(resultado.linhasReconhecidas)
            textoSelecionadoDialog = null
            showMarcaDialog = false
            produtoSelecionadoDialog = null
            marcaSelecionadaDialog = null
            if (resultado.itensEncontrados.isNotEmpty()) {
                listaItensDetectados = resultado.itensEncontrados
                isModoLista = true
            } else {
                isModoLista = false
                val principal = resultado.sugestoesProduto.firstOrNull()
                if (!principal.isNullOrBlank()) {
                    descricao = principal
                    tipoSelecionado = detectarTipoPeloTexto(principal)
                } else {
                    descricao = "Produto (Foto Anexada)"
                }
            }
            descricaoAntesDialog = descricao
            tipoAntesDialog = tipoSelecionado
            showTextosDialog = textosDetectados.isNotEmpty() && !isModoLista
            if (resultado.kmDetectado != null && resultado.kmDetectado > 0) { kmDetectadoParaConfirmar = resultado.kmDetectado; showKmConfirmDialog = true }
            showCamera = false
        })
    }

    if (isQrLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Lendo nota", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando dados no link do QR Code...", color = Color(0xFFCBD5E1))
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF0F172A)
        )
    }

    if (showKmConfirmDialog) {
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showKmConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Atualizar KM?", color = Color.White) },
            text = {
                Text(
                    "Detectamos ${kmDetectadoParaConfirmar} km na captura.\nAtualizar o odômetro do carro?",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateKmCarro(kmDetectadoParaConfirmar)
                        kmBase = kmDetectadoParaConfirmar.toString()
                        showKmConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) { Text("Sim") }
            },
            dismissButton = {
                TextButton(onClick = { showKmConfirmDialog = false }) { Text("Não") }
            }
        )
    }

    if (showTextosDialog) {
        val itensDialogo = textosDetectados
            .distinct()
            .filter { it.isNotBlank() }
            .take(8)
        val jaSelecionou = textoSelecionadoDialog != null
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showTextosDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Qual e o Produto?", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showTextosDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itensDialogo.isEmpty()) {
                        Text("Nenhum texto identificado na captura.", color = Color(0xFF94A3B8))
                    } else {
                        itensDialogo.forEach { texto ->
                            val isSelected = textoSelecionadoDialog == texto
                            val disabled = jaSelecionou && !isSelected
                            val cardShape = RoundedCornerShape(12.dp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (disabled) 0.35f else 1f)
                                    .clip(cardShape)
                                    .clickable(
                                        enabled = !disabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        textoSelecionadoDialog = texto
                                        descricao = texto
                                        tipoSelecionado = detectarTipoPeloTexto(texto)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF111827)
                                ),
                                shape = cardShape,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF334155)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        texto,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTextosDialog = false
                        produtoSelecionadoDialog = textoSelecionadoDialog
                        marcaSelecionadaDialog = null
                        textoSelecionadoDialog = null
                        showMarcaDialog = true
                    },
                    enabled = textoSelecionadoDialog != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Proximo", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {}
        )
    }

    if (showMarcaDialog) {
        val produtoSelecionado = produtoSelecionadoDialog
        val itensDialogo = textosDetectados
            .distinct()
            .filter { it.isNotBlank() }
            .filter { it != produtoSelecionado }
            .take(8)
        val jaSelecionou = marcaSelecionadaDialog != null
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showMarcaDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Qual e a Marca do Produto?", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showMarcaDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itensDialogo.isEmpty()) {
                        Text("Nenhuma marca diferente foi identificada.", color = Color(0xFF94A3B8))
                    } else {
                        itensDialogo.forEach { texto ->
                            val isSelected = marcaSelecionadaDialog == texto
                            val disabled = jaSelecionou && !isSelected
                            val cardShape = RoundedCornerShape(12.dp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (disabled) 0.35f else 1f)
                                    .clip(cardShape)
                                    .clickable(
                                        enabled = !disabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        marcaSelecionadaDialog = texto
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF111827)
                                ),
                                shape = cardShape,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF334155)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        texto,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val produto = produtoSelecionadoDialog
                        val marca = marcaSelecionadaDialog
                        if (!produto.isNullOrBlank() && !marca.isNullOrBlank()) {
                            descricao = "$produto - $marca"
                        }
                        showMarcaDialog = false
                        produtoSelecionadoDialog = null
                        marcaSelecionadaDialog = null
                    },
                    enabled = marcaSelecionadaDialog != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Concluir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {}
        )
    }

    val podeAvancarEtapa1 = (isModoLista && listaItensDetectados.isNotEmpty()) || descricao.isNotBlank()
    val tituloEtapa = when (etapaAtual) {
        1 -> if (isModoLista) "Itens Detectados" else "Novo Aviso"
        2 -> "Detalhes do Registro"
        else -> "Profissional Responsavel"
    }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = { Text(tituloEtapa, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val totalEtapas = 3
            val etapaAtualNumero = etapaAtual.coerceIn(1, totalEtapas)
            val etapas = listOf(
                1 to "Aviso",
                2 to "Detalhes",
                3 to "Profissional"
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    val circleSize = 26.dp
                    val centerY = circleSize / 2
                    val stepCount = etapas.size.coerceAtLeast(2)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        val startX = size.width / (stepCount * 2f)
                        val y = centerY.toPx()
                        val gap = size.width / stepCount
                        for (i in 0 until stepCount - 1) {
                            val active = etapaAtualNumero > (i + 1)
                            val color = if (active) Color(0xFF3B82F6) else Color(0xFF1E293B)
                            val x1 = startX + (gap * i)
                            val x2 = startX + (gap * (i + 1))
                            val inset = 6.dp.toPx()
                            drawLine(
                                color = color,
                                start = Offset(x1 + inset, y),
                                end = Offset(x2 - inset, y),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        etapas.forEach { (numero, label) ->
                            val ativo = etapaAtualNumero >= numero
                            val circleColor = if (ativo) Color(0xFF3B82F6) else Color(0xFF1E293B)
                            val textColor = if (ativo) Color.White else Color(0xFF94A3B8)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(circleSize)
                                        .clip(CircleShape)
                                        .background(circleColor)
                                        .border(1.dp, Color(0xFF23324D), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = numero.toString(),
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val tituloCadastro = when (etapaAtual) {
                    1 -> "Novo aviso"
                    2 -> "Detalhes do aviso"
                    else -> "Vincular profissional"
                }
                Text(
                    text = tituloCadastro,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                when (etapaAtual) {
                    1 -> {
                        Button(
                            onClick = { tentarAbrirCamera() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (fotoCaminho != null) Color(0xFF10B981) else Color(0xFF3B82F6),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                if (fotoCaminho != null) Icons.Default.Check else Icons.Default.CameraAlt,
                                null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (fotoCaminho != null) "Foto Anexada (Refazer)" else "Escanear Produto", color = Color.White)
                        }

                        if (qrPossuiItensSeparaveis) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Como deseja criar os avisos?", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                qrModoSeparado = false
                                                isModoLista = false
                                                if (descricaoQrConsolidada.isNotBlank()) descricao = descricaoQrConsolidada
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!qrModoSeparado) Color(0xFF3B82F6) else Color(0xFF1F2937)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Tudo em 1") }
                                        Button(
                                            onClick = {
                                                qrModoSeparado = true
                                                isModoLista = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (qrModoSeparado) Color(0xFF10B981) else Color(0xFF1F2937)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Separar por item") }
                                    }
                                }
                            }
                        }

                        if (isModoLista) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(listaItensDetectados) { item ->
                                    val kmAtualBase = kmBase.toIntOrNull() ?: 0
                                    val kmFuturoCalculado = if (kmAtualBase > 0) (kmAtualBase + getKmAdicionalPorTipo(item.tipo)).toString() else ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TipoIcon(
                                            tipo = item.tipo,
                                            tint = corCategoria(item.tipo),
                                            size = 20.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ExposedDropdownMenuBox(
                                                    expanded = tipoMenuItemId == item.id,
                                                    onExpandedChange = { expanded ->
                                                        tipoMenuItemId = if (expanded) item.id else null
                                                    }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .menuAnchor()
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(Color(0xFF1E293B))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            TipoIcon(
                                                                tipo = item.tipo,
                                                                tint = corCategoria(item.tipo),
                                                                size = 12.dp
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(item.tipo.label, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                        }
                                                    }
                                                    ExposedDropdownMenu(
                                                        expanded = tipoMenuItemId == item.id,
                                                        onDismissRequest = { tipoMenuItemId = null }
                                                    ) {
                                                        TipoManutencao.values().forEach { tipo ->
                                                            DropdownMenuItem(
                                                                text = { Text(tipo.label) },
                                                                onClick = {
                                                                    listaItensDetectados = listaItensDetectados.map {
                                                                        if (it.id == item.id) it.copy(tipo = tipo) else it
                                                                    }
                                                                    tipoMenuItemId = null
                                                                },
                                                                leadingIcon = {
                                                                    TipoIcon(
                                                                        tipo = tipo,
                                                                        tint = corCategoria(tipo),
                                                                        size = 16.dp
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                if (kmFuturoCalculado.isNotEmpty()) {
                                                    Text(
                                                        "  - Vence +${getKmAdicionalPorTipo(item.tipo)}km",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(onClick = { listaItensDetectados = listaItensDetectados - item; if (listaItensDetectados.isEmpty()) isModoLista = false }) {
                                            Icon(Icons.Default.Delete, "Remover", tint = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = localServicoInput,
                                    onValueChange = { localServicoInput = it },
                                    label = { Text("Local") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = descricao,
                                    onValueChange = { descricao = it },
                                    label = { Text("Produtos (total e itens)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(descricaoFocusRequester),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = ::iniciarCapturaVoz) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Falar descricao",
                                                tint = Color(0xFF3B82F6)
                                            )
                                        }
                                    }
                                )
                                ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = !menuExpanded }) {
                                    OutlinedTextField(
                                        value = tipoSelecionado.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Categoria") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        leadingIcon = {
                                            TipoIcon(
                                                tipo = tipoSelecionado,
                                                tint = corCategoria(tipoSelecionado),
                                                size = 18.dp
                                            )
                                        },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        TipoManutencao.values().forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t.label) },
                                                onClick = { tipoSelecionado = t; menuExpanded = false },
                                                leadingIcon = {
                                                    TipoIcon(
                                                        tipo = t,
                                                        tint = corCategoria(t),
                                                        size = 18.dp
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                    2 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = kmBase,
                                onValueChange = { if (it.all(Char::isDigit)) kmBase = it },
                                label = { Text("KM Atual") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (!isModoLista) {
                                OutlinedTextField(
                                    value = data,
                                    onValueChange = {},
                                    label = { Text("Data do servico") },
                                    readOnly = true,
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            abrirDatePicker(data) {
                                                data = it
                                                avisoPersonalizado = false
                                            }
                                        }) {
                                            Icon(Icons.Default.DateRange, contentDescription = null)
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        if (!isModoLista) {
                            OutlinedTextField(
                                value = dataAviso,
                                onValueChange = {},
                                label = { Text("Data do aviso") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        abrirDatePicker(dataAviso) {
                                            dataAviso = it
                                            avisoPersonalizado = true
                                        }
                                    }) {
                                        Icon(Icons.Default.Event, contentDescription = null)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = valorInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) valorInput = it },
                                label = { Text("Valor Total (R$)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = horaNotificacao,
                                onValueChange = {},
                                label = { Text("Hora do aviso") },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        abrirTimePicker(horaNotificacao) { selecionada ->
                                            horaNotificacao = selecionada
                                        }
                                    },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        abrirTimePicker(horaNotificacao) { selecionada ->
                                            horaNotificacao = selecionada
                                        }
                                    }) {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Text(
                                "Ajustes por item (automatico, mas você pode editar):",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                            TextButton(
                                onClick = {
                                    itemDataAvisoOverrides = listaItensDetectados.associate { it.id to dataAviso }
                                    itemHoraAvisoOverrides = listaItensDetectados.associate { it.id to horaNotificacao }
                                }
                            ) { Text("Aplicar data/hora padrao para todos") }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                listaItensDetectados.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(item.nome, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(
                                                value = itemValorOverrides[item.id] ?: String.format(Locale.US, "%.2f", item.valor),
                                                onValueChange = { novo ->
                                                    if (novo.all { c -> c.isDigit() || c == '.' }) {
                                                        itemValorOverrides = itemValorOverrides + (item.id to novo)
                                                    }
                                                },
                                                label = { Text("Valor (R$)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = itemDataAvisoOverrides[item.id] ?: dataAviso,
                                                    onValueChange = {},
                                                    label = { Text("Data aviso") },
                                                    readOnly = true,
                                                    modifier = Modifier.weight(1f),
                                                    trailingIcon = {
                                                        IconButton(onClick = {
                                                            abrirDatePicker(itemDataAvisoOverrides[item.id] ?: dataAviso) { selecionada ->
                                                                itemDataAvisoOverrides = itemDataAvisoOverrides + (item.id to selecionada)
                                                            }
                                                        }) {
                                                            Icon(Icons.Default.Event, contentDescription = null)
                                                        }
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = itemHoraAvisoOverrides[item.id] ?: horaNotificacao,
                                                    onValueChange = {},
                                                    label = { Text("Hora") },
                                                    readOnly = true,
                                                    modifier = Modifier.weight(1f),
                                                    trailingIcon = {
                                                        IconButton(onClick = {
                                                            abrirTimePicker(itemHoraAvisoOverrides[item.id] ?: horaNotificacao) { selecionada ->
                                                                itemHoraAvisoOverrides = itemHoraAvisoOverrides + (item.id to selecionada)
                                                            }
                                                        }) {
                                                            Icon(Icons.Default.Schedule, contentDescription = null)
                                                        }
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Adicionar novo profissional", color = Color.White, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = novoContatoNome,
                                    onValueChange = { novoContatoNome = it },
                                    label = { Text("Nome") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = novoContatoTelefone,
                                    onValueChange = { novoContatoTelefone = it },
                                    label = { Text("Telefone") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = ::adicionarContatoManual,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cadastrar profissional")
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Vincular profissional", color = Color.White, fontWeight = FontWeight.SemiBold)
                                if (contatosLista.isEmpty()) {
                                    Text("Nenhum profissional cadastrado. Adicione um acima.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        contatosLista.forEach { contato ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, if (contatoSelecionado == contato) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                                    .clickable { contatoSelecionado = contato },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF1E293B)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(contato.nome, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                        Text(contato.telefone, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                                    }
                                                    if (contatoSelecionado == contato) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                                    } else {
                                                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color(0xFF64748B))
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (etapaAtual) {
                    1 -> {
                        Button(
                            onClick = { etapaAtual = 2 },
                            enabled = podeAvancarEtapa1,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Avancar", fontSize = 16.sp) }
                        
                    }
                    2 -> {
                        Button(
                            onClick = { etapaAtual = 3 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Avancar", fontSize = 16.sp) }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { etapaAtual = 1 }) { Text("Voltar", color = Color.White) }
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                salvarAvisos()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Salvar Registro", fontSize = 16.sp) }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { etapaAtual = 2 }) { Text("Voltar", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

private data class NotaQrInfo(
    val valorTotal: Double?,
    val dataCompra: String?,
    val descricaoItens: String? = null,
    val nomeEstabelecimento: String? = null,
    val enderecoEstabelecimento: String? = null
)

private const val QR_PARSER_TAG = "ZelluQrParser"
private const val ENABLE_QR_MOCK_DIAGNOSTIC = false

private suspend fun consultarNotaPorQrCode(url: String): NotaQrInfo? = withContext(Dispatchers.IO) {
    runCatching {
        if (ENABLE_QR_MOCK_DIAGNOSTIC) {
            testarParserSpMock()
        }
        Log.i(QR_PARSER_TAG, "Iniciando consulta QR URL=$url")
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(15000)
            .get()

        // Padrões mais comuns por estado (SP / MG).
        val porLayout = extrairNotaPorLayout(doc)
        if (porLayout != null && (porLayout.valorTotal != null || porLayout.dataCompra != null)) {
            Log.d(
                QR_PARSER_TAG,
                "Resultado por layout: valor=${porLayout.valorTotal} data=${porLayout.dataCompra} itens=${porLayout.descricaoItens} estabelecimento=${porLayout.nomeEstabelecimento} endereco=${porLayout.enderecoEstabelecimento}"
            )
            return@runCatching porLayout
        }
        Log.d(QR_PARSER_TAG, "Layout não resolveu. Aplicando fallback por regex.")

        val valorTotalPelaUrl = extrairValorVnfDaUrl(url)
        val texto = doc.text()

        // Tentativa 1: seletores comuns da página da NFC-e/SEFAZ.
        val candidatosValor = listOf(
            "#totalNota", ".totalNumb", ".txtMax", ".txtCenter"
        ).flatMap { selector -> doc.select(selector).map { it.text() } }
        val candidatosData = listOf(
            ".txtCenter", ".chave", ".txtObs", ".ui-li-static"
        ).flatMap { selector -> doc.select(selector).map { it.text() } }

        val valorRegex = Regex(
            "(?i)(valor\\s*total|v\\.?\\s*total|total\\s*(?:da\\s*nota|nota|nfce)?|vl\\s*total)\\D{0,30}(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2})"
        )
        val dataRegex = Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b")

        val valorMatch = (
            candidatosValor.firstNotNullOfOrNull { bloco ->
                valorRegex.find(bloco)?.groupValues?.getOrNull(2)
            } ?: valorRegex.find(texto)?.groupValues?.getOrNull(2)
            )
        val valorTotal = valorMatch
            ?.replace(".", "")
            ?.replace(",", ".")
            ?.toDoubleOrNull()
        val valorTotalFinal = when {
            valorTotalPelaUrl != null && valorTotal != null -> maxOf(valorTotalPelaUrl, valorTotal)
            valorTotalPelaUrl != null -> valorTotalPelaUrl
            else -> valorTotal
        }
        val dataCompra = candidatosData.firstNotNullOfOrNull { bloco ->
            dataRegex.find(bloco)?.value
        } ?: dataRegex.find(texto)?.value

        val descricaoItens = extrairItensGenerico(doc)
        val estabelecimento = extrairDadosEstabelecimento(doc)
        Log.d(
            QR_PARSER_TAG,
            "Resultado fallback: valor=$valorTotalFinal valorUrl=$valorTotalPelaUrl data=$dataCompra itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
        )
        if (valorTotalFinal == null && dataCompra == null && descricaoItens.isNullOrBlank()) {
            null
        } else {
            NotaQrInfo(
                valorTotal = valorTotalFinal,
                dataCompra = dataCompra,
                descricaoItens = descricaoItens,
                nomeEstabelecimento = estabelecimento.first,
                enderecoEstabelecimento = estabelecimento.second
            )
        }
    }.onFailure {
        Log.e(QR_PARSER_TAG, "Erro ao consultar QR: ${it.message}", it)
    }.getOrNull()
}

private fun extrairNotaPorLayout(doc: Document): NotaQrInfo? {
    val host = runCatching { java.net.URI(doc.location()).host?.lowercase(Locale.ROOT) }.getOrNull().orEmpty()
    return when {
        host.contains("dfe.ms.gov.br") || host.contains("sefaz.ms.gov.br") -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: MS (host=$host) - usando fallback robusto")
            null
        }

        host.contains("fazenda.sp.gov.br") ||
            doc.select("#totalNota, .totalNumb, #linhaTotal, .txtMax").isNotEmpty() -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: SP (host=$host)")
            extrairNotaLayoutSp(doc)
        }

        host.contains("fazenda.mg.gov.br") ||
            doc.select("#valorTotal, .valorTotal, .dadosNf, .nfce, .nfce-body").isNotEmpty() -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: MG (host=$host)")
            extrairNotaLayoutMg(doc)
        }

        else -> {
            Log.d(QR_PARSER_TAG, "Nenhum layout SP/MG identificado (host=$host)")
            null
        }
    }
}

private fun extrairNotaLayoutSp(doc: Document): NotaQrInfo? {
    val valor = sequenceOf(
        doc.select("#totalNota").text(),
        doc.select(".totalNumb").text(),
        doc.select("#linhaTotal").text(),
        doc.select(".txtMax").text(),
        doc.select("td:matchesOwn((?i)valor total|total da nota)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|total\\s*da\\s*nota"))

    val data = sequenceOf(
        doc.select("#dataEmissao").text(),
        doc.select(".txtCenter").text(),
        doc.select(".txtObs").text(),
        doc.select(".ui-li-static").text(),
        doc.select("td:matchesOwn((?i)data de emissao|emissao|data)").text()
    ).map { extrairPrimeiraData(it) }.firstOrNull { it != null }
        ?: encontrarDataProximaAoRotulo(doc, Regex("(?i)data\\s*de\\s*emissao|emissao|data"))

    val descricaoItens = extrairItensSp(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    Log.d(
        QR_PARSER_TAG,
        "SP parser => valor=$valor data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valor == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valor,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second
    )
}

private fun extrairNotaLayoutMg(doc: Document): NotaQrInfo? {
    val valor = sequenceOf(
        doc.select("#valorTotal").text(),
        doc.select(".valorTotal").text(),
        doc.select(".dadosNf").text(),
        doc.select(".nfce").text(),
        doc.select("td:matchesOwn((?i)valor total|valor a pagar|total)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|valor\\s*a\\s*pagar|total"))

    val data = sequenceOf(
        doc.select(".dadosNf").text(),
        doc.select(".nfce").text(),
        doc.select(".nfce-body").text(),
        doc.select("td:matchesOwn((?i)data|emissao)").text()
    ).map { extrairPrimeiraData(it) }.firstOrNull { it != null }
        ?: encontrarDataProximaAoRotulo(doc, Regex("(?i)data|emissao"))

    val descricaoItens = extrairItensMg(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    Log.d(
        QR_PARSER_TAG,
        "MG parser => valor=$valor data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valor == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valor,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second
    )
}

private fun extrairItensSp(doc: Document): String? {
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf("#tabResult tr", ".txtTit", ".txtProd", "table tr")
    )
}

private fun extrairItensMg(doc: Document): String? {
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf(".dadosNf tr", ".nfce tr", ".produto", ".descricao", "table tr")
    )
}

private fun extrairItensGenerico(doc: Document): String? {
    val porTitulos = extrairItensPorTitulosEValores(doc)
    if (!porTitulos.isNullOrBlank()) return porTitulos
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf("tr", "li", ".ui-li-static", ".txtTit", ".txtProd", ".descricao")
    )
}

private fun extrairItensPorLinhas(doc: Document, seletoresLinha: List<String>): String? {
    val itens = mutableListOf<String>()
    seletoresLinha.forEach { seletor ->
        doc.select(seletor).forEach { linha ->
            val item = montarItemDescricao(linha.text().trim())
            if (!item.isNullOrBlank()) itens.add(item)
        }
    }
    return normalizarItensDescricao(itens)
}

private fun montarItemDescricao(textoLinha: String): String? {
    if (textoLinha.isBlank()) return null
    val texto = Parser.unescapeEntities(textoLinha, false).replace(Regex("\\s+"), " ").trim()
    val lower = texto.lowercase(Locale.ROOT)
    if (
        lower.contains("valor total") ||
        lower.contains("total da nota") ||
        lower.contains("chave de acesso") ||
        lower.contains("protocolo") ||
        lower.contains("tribut") ||
        lower.contains("emissao") ||
        lower.contains("qtd") ||
        lower.contains("qtde") ||
        lower.contains("un ") ||
        lower.startsWith("codigo")
    ) return null

    val valor = extrairUltimoValorMonetario(texto) ?: return null
    val cortePorRotulo = texto.split(
        Regex("(?i)\\b(c[oó]digo|cod\\.?|qtde|qtd|un|vl\\.?\\s*total|valor\\s*total)\\b"),
        limit = 2
    ).firstOrNull().orEmpty()
    val nomeBase = texto
        .substringBefore("(")
        .let { if (cortePorRotulo.isNotBlank()) cortePorRotulo else it }
        .replace(Regex("(?i)\\b(c[oó]digo|cod\\.?|qtde|qtd|un|vl\\.?|r\\$)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (nomeBase.isBlank()) return null
    return "${nomeBase.take(60)} (${String.format(Locale.US, "%.2f", valor)})"
}

private fun extrairItensPorTitulosEValores(doc: Document): String? {
    val nomes = doc.select(".txtTit, .txtProd, .descricao, .xProd")
        .mapNotNull { limparNomeProduto(it.text()) }
        .distinct()
    if (nomes.isEmpty()) return null

    val valores = doc.select(".valor, .valorItem, .vProd, .preco, .valorProduto, .RxValor")
        .mapNotNull { extrairPrimeiroValorMonetario(it.text()) }
        .toList()
    if (valores.isEmpty()) return nomes.take(4).joinToString(" + ")

    val limite = minOf(nomes.size, valores.size, 6)
    if (limite == 0) return null
    val itens = (0 until limite).map { i ->
        "${nomes[i].take(60)} (${String.format(Locale.US, "%.2f", valores[i])})"
    }
    return normalizarItensDescricao(itens)
}

private fun limparNomeProduto(textoOriginal: String): String? {
    val texto = Parser.unescapeEntities(textoOriginal, false)
        .replace(Regex("\\s+"), " ")
        .trim()
    if (texto.isBlank()) return null
    val lower = texto.lowercase(Locale.ROOT)
    if (
        lower.contains("valor total") ||
        lower.contains("total da nota") ||
        lower.contains("vl. total") ||
        lower.startsWith("total") ||
        lower.startsWith("codigo") ||
        lower.startsWith("código")
    ) return null
    return texto.substringBefore("(").trim().ifBlank { null }
}

private fun normalizarItensDescricao(itens: List<String>): String? {
    val normalizados = itens
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { it.contains(" (0.00)") }
        .distinct()
        .take(6)
    if (normalizados.isEmpty()) return null
    return normalizados.joinToString(" + ")
}

private fun extrairDadosEstabelecimento(doc: Document): Pair<String?, String?> {
    val nome = sequenceOf(
        doc.select(".txtTopo").firstOrNull()?.text(),
        doc.select(".txtTit").firstOrNull()?.text(),
        doc.select("h1").firstOrNull()?.text(),
        doc.select("h2").firstOrNull()?.text()
    ).mapNotNull { it?.let { t -> Parser.unescapeEntities(t, false) }?.replace(Regex("\\s+"), " ")?.trim() }
        .firstOrNull { texto ->
            texto.length > 3 &&
                !texto.contains("nfc-e", ignoreCase = true) &&
                !texto.contains("danfe", ignoreCase = true) &&
                !texto.contains("consulta", ignoreCase = true)
        }

    val endereco = sequenceOf(
        doc.select(".text").firstOrNull { it.text().contains(",") }?.text(),
        doc.select(".txtEndereco").firstOrNull()?.text(),
        doc.select(".enderEmit").firstOrNull()?.text(),
        doc.select(".txtEnder").firstOrNull()?.text(),
        doc.select(".dadosEmit").firstOrNull()?.text(),
        doc.select(".txtCorpo").firstOrNull()?.text(),
        doc.select(".txtTopo").firstOrNull { possuiPadraoEndereco(it.text()) }?.text()
    ).mapNotNull { it?.let { t -> Parser.unescapeEntities(t, false) }?.replace(Regex("\\s+"), " ")?.trim() }
        .firstOrNull { possuiPadraoEndereco(it) }

    val enderecoFinal = endereco ?: extrairEnderecoPorRegex(doc)
    return Pair(nome, enderecoFinal)
}

private fun possuiPadraoEndereco(texto: String): Boolean {
    val t = texto.lowercase(Locale.ROOT)
    val temVia = t.contains("rua") || t.contains("av") || t.contains("avenida") || t.contains("rod")
    return temVia && texto.contains(",")
}

private fun extrairEnderecoPorRegex(doc: Document): String? {
    val texto = Parser.unescapeEntities(doc.text(), false).replace(Regex("\\s+"), " ").trim()
    if (texto.isBlank()) return null
    val regexEndereco = Regex(
        "(?i)\\b(rua|r\\.|avenida|av\\.|travessa|trv\\.|alameda|rodovia|rod\\.)\\b[^|]{8,140}"
    )
    val encontrado = regexEndereco.find(texto)?.value?.trim() ?: return null
    return encontrado.take(120)
}

private fun extrairPrimeiroValorMonetario(texto: String): Double? {
    val valor = Regex("\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2}")
        .find(texto)
        ?.value
        ?: return null
    return valor.replace(".", "").replace(",", ".").toDoubleOrNull()
}

private fun montarLocalNota(estabelecimento: String, endereco: String): String {
    val partes = mutableListOf<String>()
    if (estabelecimento.isNotBlank()) partes += estabelecimento
    if (endereco.isNotBlank()) partes += endereco
    return partes.joinToString(" - ")
}

private fun montarDescricaoItensNota(total: Double?, itens: String?): String {
    val partes = mutableListOf<String>()
    if (total != null) partes += "Total: R$ ${String.format(Locale.US, "%.2f", total)}"
    if (!itens.isNullOrBlank()) partes += "Itens: $itens"
    if (partes.isEmpty()) {
        return if (total != null) {
            "Servico da nota (R$ ${String.format(Locale.US, "%.2f", total)})"
        } else {
            "Servico da nota"
        }
    }
    return partes.joinToString(" | ")
}

private fun extrairItensDaDescricaoQr(descricao: String?): List<ItemDetectado> {
    if (descricao.isNullOrBlank()) return emptyList()
    val blocoItens = descricao.substringAfter("Itens:", descricao).trim()
    if (blocoItens.isBlank()) return emptyList()
    val partes = blocoItens.split("+").map { it.trim() }.filter { it.isNotBlank() }
    val regexItem = Regex("(.+?)\\s*\\((\\d+[\\.,]\\d{2})\\)")
    return partes.mapNotNull { parte ->
        val match = regexItem.find(parte) ?: return@mapNotNull null
        val nome = match.groupValues[1].trim()
        val valor = match.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 0.0
        if (nome.isBlank()) return@mapNotNull null
        ItemDetectado(
            id = UUID.randomUUID().toString(),
            nome = nome,
            tipo = detectarTipoPeloTexto(nome),
            valor = valor,
            quantidade = 1
        )
    }
}

private fun extrairValorVnfDaUrl(url: String): Double? {
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    val query = uri.rawQuery ?: return null
    val valorBruto = query
        .split("&")
        .firstOrNull { it.startsWith("vNF=", ignoreCase = true) }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return valorBruto.replace(",", ".").toDoubleOrNull()
}

private fun extrairUltimoValorMonetario(texto: String): Double? {
    val valores = Regex("\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2}")
        .findAll(texto)
        .map { it.value }
        .toList()
    val ultimo = valores.lastOrNull() ?: return null
    return ultimo.replace(".", "").replace(",", ".").toDoubleOrNull()
}

private fun extrairPrimeiraData(texto: String): String? =
    Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b").find(texto)?.value

private fun encontrarValorProximoAoRotulo(doc: Document, rotulo: Regex): Double? {
    val elementos = doc.select("*").take(400)
    elementos.forEach { el ->
        if (!rotulo.containsMatchIn(el.ownText())) return@forEach
        val candidatos = listOf(
            el.ownText(),
            el.text(),
            el.nextElementSibling()?.text().orEmpty(),
            el.parent()?.text().orEmpty()
        )
        candidatos.forEach { bloco ->
            val valor = extrairPrimeiroValorMonetario(bloco)
            if (valor != null) return valor
        }
    }
    return null
}

private fun encontrarDataProximaAoRotulo(doc: Document, rotulo: Regex): String? {
    val elementos = doc.select("*").take(400)
    elementos.forEach { el ->
        if (!rotulo.containsMatchIn(el.ownText())) return@forEach
        val candidatos = listOf(
            el.ownText(),
            el.text(),
            el.nextElementSibling()?.text().orEmpty(),
            el.parent()?.text().orEmpty()
        )
        candidatos.forEach { bloco ->
            val data = extrairPrimeiraData(bloco)
            if (data != null) return data
        }
    }
    return null
}

private fun testarParserSpMock() {
    val htmlMockSp = """
        <html>
            <body>
                <span class="txtTit">Gasolina Comum</span>
                <span class="valor">150,00</span>
                <div id="linhaTotal">
                    <label>Valor Total R$</label>
                    <span class="totalNumb">150,00</span>
                </div>
                <div id="dataEmissao">Emissão: 02/02/2026 13:30:00</div>
            </body>
        </html>
    """.trimIndent()

    val doc = Jsoup.parse(htmlMockSp)
    val resultado = extrairNotaLayoutSp(doc)
    Log.i(
        QR_PARSER_TAG,
        "Teste mock SP => valor=${resultado?.valorTotal} data=${resultado?.dataCompra} itens=${resultado?.descricaoItens} estabelecimento=${resultado?.nomeEstabelecimento} endereco=${resultado?.enderecoEstabelecimento}"
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditarCarroDialog(carroAtual: CarroInfo, titulo: String, onDismiss: () -> Unit, onSalvar: (CarroInfo) -> Unit) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf(carroAtual.nome) }
    var marca by remember { mutableStateOf(carroAtual.marca) }
    var modelo by remember { mutableStateOf(carroAtual.modelo) }
    var proprietario by remember { mutableStateOf(carroAtual.proprietario) }
    var kmAtualStr by remember { mutableStateOf(if (carroAtual.kmAtual > 0) formatarKm(carroAtual.kmAtual) else "") }
    var tipoSelecionado by remember { mutableStateOf(carroAtual.tipoVeiculo) }
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o nome do carro")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voz indisponível neste dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun iniciarCapturaVozMotor() {
        alvoVoz = "motor"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o motor do carro")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voz indisponível neste dispositivo", Toast.LENGTH_SHORT).show()
        }
    }
    AlertDialog(
        modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
        shape = dialogCornerShape,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = { Text(titulo, color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do carro") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = ::iniciarCapturaVozApelido) {
                            Icon(Icons.Default.Mic, contentDescription = "Falar nome do carro")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Motor") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = ::iniciarCapturaVozMotor) {
                            Icon(Icons.Default.Mic, contentDescription = "Falar motor")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = proprietario,
                    onValueChange = { proprietario = it },
                    label = { Text("Proprietario") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                var marcaExpanded by remember { mutableStateOf(false) }
                val marcaLogo = logoResForMarca(marca)
                ExposedDropdownMenuBox(expanded = marcaExpanded, onExpandedChange = { marcaExpanded = !marcaExpanded }) {
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
                        leadingIcon = {
                            val tipoLocal = tipoSelecionado
                            if (marcaLogo != null) {
                                Image(
                                    painter = painterResource(marcaLogo),
                                    contentDescription = marca,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            } else if (tipoLocal != null) {
                                VehicleIcon(
                                    tipoVeiculo = tipoLocal,
                                    tint = Color(0xFF3B82F6),
                                    size = 20.dp
                                )
                            } else {
                                Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color(0xFF3B82F6))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(expanded = marcaExpanded, onDismissRequest = { marcaExpanded = false }) {
                        marcasSuportadas.forEach { marcaNome ->
                            DropdownMenuItem(
                                text = { Text(marcaNome) },
                                onClick = {
                                    marca = marcaNome
                                    marcaExpanded = false
                                },
                                leadingIcon = {
                                    val res = logoResForMarca(marcaNome)
                                    if (res != null) {
                                        Image(
                                            painter = painterResource(res),
                                            contentDescription = marcaNome,
                                            modifier = Modifier.size(20.dp),
                                            colorFilter = ColorFilter.tint(Color.White)
                                        )
                                    } else {
                                        val tipoLocal = tipoSelecionado
                                        if (tipoLocal != null) {
                                            VehicleIcon(
                                                tipoVeiculo = tipoLocal,
                                                tint = Color.White,
                                                size = 18.dp
                                            )
                                        } else {
                                            Icon(Icons.Rounded.DirectionsCar, contentDescription = null)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TipoVeiculoSelector(
                    selecionado = tipoSelecionado,
                    onSelect = { tipoSelecionado = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = kmAtualStr,
                    onValueChange = { kmAtualStr = formatarKmTexto(it) },
                    label = { Text("KM Atual (Painel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text("Cor do carro", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
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
                    "Bordô" to Color(0xFF7F1D1D),
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
                        val selecionada = corSelecionada == cor.toArgb()
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
                                        width = if (selecionada) 3.dp else 1.dp,
                                        color = if (selecionada) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable { corSelecionada = cor.toArgb() }
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
                Spacer(Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
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
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AbastecimentoDialog(onDismiss: () -> Unit) {
    var precoGasolina by remember { mutableStateOf("") }
    var valorAbastecido by remember { mutableStateOf("") }
    val preco = precoGasolina.replace(",", ".").toDoubleOrNull()
    val total = valorAbastecido.replace(",", ".").toDoubleOrNull()
    val litros = if (preco != null && total != null && preco > 0) total / preco else null
    val litrosTexto = litros?.let { String.format(Locale("pt", "BR"), "%.2f L", it) } ?: "--"
    val gastoTexto = total?.let { formatarMoeda(it) } ?: "--"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B1224))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Adicionar abastecimento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Informe os valores para calcular os litros", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = precoGasolina,
                    onValueChange = { precoGasolina = it },
                    label = { Text("Valor da gasolina (R$/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF111827))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Litros calculados", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(litrosTexto, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gasto no posto", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(gastoTexto, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
}
