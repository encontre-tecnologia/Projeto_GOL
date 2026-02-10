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
fun NovoContatoDialog(
    onDismiss: () -> Unit,
    onSalvar: (ContatoProfissional) -> Unit,
    contatosExistentes: List<ContatoProfissional> = emptyList(),
    onSelecionarExistente: (ContatoProfissional) -> Unit = {}
) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    val hasNome = nome.trim().isNotEmpty()

    AlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFBF2),
        title = {
            Text(
                "Novo Profissional",
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = Color(0xFF475569),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo (Ex: Mecanico)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = Color(0xFF475569),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { if (it.all(Char::isDigit)) telefone = it },
                    label = { Text("WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = Color(0xFF475569),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                if (contatosExistentes.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text(
                        "Ou escolha um profissional ja cadastrado",
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(contatosExistentes) { contato ->
                            OutlinedButton(
                                onClick = { onSelecionarExistente(contato) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A))
                            ) {
                                Text(
                                    "${contato.nome} - ${contato.tipoServico.ifBlank { "Profissional" }}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasNome) {
                        onSalvar(
                            ContatoProfissional(
                                nome = nome.trim(),
                                telefone = telefone.trim(),
                                tipoServico = tipo.trim()
                            )
                        )
                    }
                },
                enabled = hasNome,
                modifier = Modifier
                    .height(52.dp)
                    .width(150.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Salvar", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(52.dp)
                    .width(150.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancelar", fontSize = 16.sp, color = Color(0xFF334155), fontWeight = FontWeight.SemiBold) }
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

fun formatarKm(valor: Int): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(valor)

fun formatarKmTexto(texto: String): String {
    val digits = texto.filter(Char::isDigit)
    val value = digits.toLongOrNull() ?: 0L
    return NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)
}

fun filtrarTextosDetectados(linhas: List<String>): List<String> {
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


