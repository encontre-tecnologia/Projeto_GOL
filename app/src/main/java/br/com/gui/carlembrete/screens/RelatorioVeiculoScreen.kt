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
import br.com.gui.carlembrete.VehicleIcon
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelatorioVeiculoScreen(carroAtual: CarroInfo, lembretes: List<Lembrete>, isPremium: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF0F2A4A) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val resumo = remember(carroAtual, lembretes, isPremium) { gerarResumoRelatorio(carroAtual, lembretes, isPremium) }
    val resumoChip = remember(resumo) {
        resumo.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.equals("Relat├│rio do ve├¡culo", ignoreCase = true) }
            ?: "Sem dados"
    }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val lembretesPorTipo = TipoManutencao.values().associateWith { tipo -> lembretes.count { it.tipo == tipo } }
    val proximos = lembretes.mapNotNull { lembrete ->
        val data = try { LocalDate.parse(lembrete.dataLimite, formatter) } catch (e: Exception) { null }
        data?.let { lembrete to it }
    }.sortedBy { it.second }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = pageBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBackground)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Detalhes do veículo",
                            color = textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        val pdfAccent = if (isDark) Color.White else Color.Black
                        OutlinedButton(
                            onClick = {
                                val uri = gerarPdfRelatorio(context, carroAtual, lembretes, isPremium)
                                if (uri != null) {
                                    compartilharPdf(context, uri)
                                } else {
                                    Toast.makeText(context, "N├úo foi poss├¡vel gerar o PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, pdfAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = pdfAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF", color = pdfAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                Spacer(Modifier.height(24.dp))
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        val infoModelo = listOf(carroAtual.marca, carroAtual.modelo).filter { it.isNotBlank() }.joinToString(" - ")
                        val proximaData = proximos.firstOrNull()?.second?.format(formatter) ?: "Sem agenda"
                        val kmAtualText = if (carroAtual.kmAtual > 0) "${carroAtual.kmAtual} km" else "Não informado"
                        val proprietarioText = carroAtual.proprietario.ifBlank { "Não informado" }
                        val textoPrimario = Color(0xFF0F172A)
                        val textoSecundario = Color(0xFF475569)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF7F6F9))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    val isBikeIcon = carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA
                                    VehicleIcon(
                                        tipoVeiculo = carroAtual.tipoVeiculo,
                                        tint = if (isBikeIcon) Color.White else null,
                                        size = if (isBikeIcon) 88.dp else 210.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(carroAtual.nome, color = textoPrimario, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                        if (infoModelo.isNotBlank()) {
                                            Text(infoModelo, color = textoSecundario, fontSize = 13.sp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .wrapContentWidth()
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = resumoChip, color = textoPrimario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Divider(color = Color(0xFFE2E8F0))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VehicleStat(label = "Od├┤metro", value = kmAtualText, color = textoPrimario)
                                    VehicleStat(label = "Avisos ativos", value = lembretes.size.toString(), color = textoPrimario)
                                    VehicleStat(label = "Pr├│ximo servi├ºo", value = proximaData, color = textoPrimario)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VehicleStat(label = "Proprietário", value = proprietarioText, color = textoPrimario)
                                }
                            }
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1729))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Status geral", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Avisos ativos: ${lembretes.size}", color = Color(0xFF93C5FD))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TipoManutencao.values().forEach { tipo ->
                                    val quantidade = lembretesPorTipo.getOrDefault(tipo, 0)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF16233B))
                                                    .border(2.dp, calcularCorStatus(lembretes, tipo).copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                TipoIcon(
                                                    tipo = tipo,
                                                    tint = calcularCorStatus(lembretes, tipo),
                                                    size = 26.dp,
                                                    textSize = 12.sp
                                                )
                                            }
                                            if (quantidade > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-6).dp, y = 6.dp)
                                                        .size(22.dp)
                                                        .background(Color(0xFFEF4444), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(quantidade.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(tipo.label, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1729))
                    ) {
                        val (tituloReputacao, descricaoReputacao) = calcularReputacao(lembretes)
                        val corReputacao = when (tituloReputacao) {
                            "Excelente" -> Color(0xFF10B981)
                            "Cr├¡tica" -> Color(0xFFEF4444)
                            "Em aten├º├úo" -> Color(0xFFEAB308)
                            else -> Color.White
                        }
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Reputa├º├úo do ve├¡culo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = corReputacao)
                                Text(tituloReputacao, color = corReputacao, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(descricaoReputacao, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                    }
                    if (proximos.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Pr├│ximas manuten├º├Áes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            proximos.forEach { (lembrete, data) ->
                                ElevatedCard(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1729)),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(lembrete.titulo, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("Data: ${lembrete.dataLimite.ifBlank { data.format(formatter) }}", color = Color(0xFF93C5FD), fontSize = 12.sp)
                                        if (lembrete.kmLimite.isNotBlank()) Text("KM limite: ${lembrete.kmLimite}", color = Color(0xFFE0E7FF), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Sem manuten├º├Áes pendentes", color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }

