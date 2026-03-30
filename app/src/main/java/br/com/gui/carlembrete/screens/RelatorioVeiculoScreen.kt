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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelatorioVeiculoScreen(carroAtual: CarroInfo, lembretes: List<Lembrete>, isPremium: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val pageBackground = colorScheme.background
    val textPrimary = colorScheme.onSurface
    val textDim = colorScheme.onSurfaceVariant
    val englishUi = isEnglishUi()
    val reportTitle = if (englishUi) "Vehicle report" else "Relatório do veículo"
    val noDataLabel = if (englishUi) "No data" else "Sem dados"
    val resumo = remember(carroAtual, lembretes, isPremium) { gerarResumoRelatorio(carroAtual, lembretes, isPremium) }
    val resumoChip = remember(resumo, reportTitle, noDataLabel) {
        resumo.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.equals(reportTitle, ignoreCase = true) }
            ?: noDataLabel
    }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val lembretesTecnicos = lembretes.filter { it.tipo.ehManutencaoTecnica() }
    val tiposTecnicos = TipoManutencao.values().filter { it.ehManutencaoTecnica() }
    val lembretesPorTipo = tiposTecnicos.associateWith { tipo -> lembretesTecnicos.count { it.tipo == tipo } }
    val proximos = lembretesTecnicos.mapNotNull { lembrete ->
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
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = textPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tr("Detalhes do veículo", "Vehicle details"),
                            color = textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        val pdfAccent = Color.Black
                        OutlinedButton(
                            onClick = {
                                val uri = gerarPdfRelatorio(context, carroAtual, lembretes, isPremium)
                                if (uri != null) {
                                    compartilharPdf(context, uri)
                                } else {
                                    Toast.makeText(context, trNow("Não foi possível gerar o PDF", "Could not generate PDF"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, pdfAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = tr("Exportar PDF", "Export PDF"), tint = pdfAccent, modifier = Modifier.size(18.dp))
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
                        val proximaData = proximos.firstOrNull()?.second?.format(formatter) ?: tr("Sem agenda", "No schedule")
                        val kmAtualText = if (carroAtual.kmAtual > 0) "${carroAtual.kmAtual} km" else tr("Não informado", "Not informed")
                        val proprietarioText = carroAtual.proprietario.ifBlank { tr("Não informado", "Not informed") }
                        val textoPrimario = Color(0xFF0F172A)
                        val textoSecundario = Color(0xFF475569)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(colorScheme.surface)
                                .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = carroAtual.nome,
                                        color = textoPrimario,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (infoModelo.isNotBlank()) {
                                        Text(infoModelo, color = textoSecundario, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .align(Alignment.End)
                                            .wrapContentWidth()
                                            .background(colorScheme.primary.copy(alpha = if (isDark) 0.24f else 0.14f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = resumoChip,
                                            color = textoPrimario,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Divider(color = colorScheme.outlineVariant.copy(alpha = 0.7f))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VehicleStat(label = tr("Odômetro", "Odometer"), value = kmAtualText, color = textoPrimario)
                                    VehicleStat(label = tr("Avisos ativos", "Active reminders"), value = lembretes.size.toString(), color = textoPrimario)
                                    VehicleStat(label = tr("Próximo serviço", "Next service"), value = proximaData, color = textoPrimario)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VehicleStat(label = tr("Proprietário", "Owner"), value = proprietarioText, color = textoPrimario)
                                }
                            }
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(tr("Status geral", "General status"), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${tr("Avisos ativos", "Active reminders")}: ${lembretesTecnicos.size}", color = colorScheme.primary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                tiposTecnicos.forEach { tipo ->
                                    val quantidade = lembretesPorTipo.getOrDefault(tipo, 0)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(colorScheme.background)
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
                                        Text(tipo.label, color = textDim, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                    ) {
                        val (tituloReputacao, descricaoReputacao) = calcularReputacao(lembretes)
                        val corReputacao = when (tituloReputacao) {
                            tr("Excelente", "Excellent") -> Color(0xFF10B981)
                            tr("Crítica", "Critical") -> Color(0xFFEF4444)
                            tr("Em atenção", "Attention") -> Color(0xFFEAB308)
                            else -> textPrimary
                        }
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(tr("Reputação do veículo", "Vehicle reputation"), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = corReputacao)
                                Text(tituloReputacao, color = corReputacao, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(descricaoReputacao, color = textDim, fontSize = 12.sp)
                        }
                    }
                    if (proximos.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(tr("Próximas manutenções", "Upcoming maintenance"), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            proximos.forEach { (lembrete, data) ->
                                ElevatedCard(
                                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(lembrete.titulo, color = textPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("${tr("Data", "Date")}: ${lembrete.dataLimite.ifBlank { data.format(formatter) }}", color = colorScheme.primary, fontSize = 12.sp)
                                        if (lembrete.kmLimite.isNotBlank()) Text("${tr("KM limite", "Mileage limit")}: ${lembrete.kmLimite}", color = textDim, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(tr("Sem manutenções pendentes", "No pending maintenance"), color = textDim)
                        }
                    }
                }
            }
        }
    }

