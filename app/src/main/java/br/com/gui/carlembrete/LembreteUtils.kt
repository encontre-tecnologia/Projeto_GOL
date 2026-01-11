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

/* ----------------- LÓGICA GERAL ----------------- */

private val lembreteDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseDataLimite(lembrete: Lembrete): LocalDate? =
    runCatching { LocalDate.parse(lembrete.dataLimite, lembreteDateFormatter) }.getOrNull()

private fun diasParaVencer(lembrete: Lembrete): Int? =
    parseDataLimite(lembrete)?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

fun textoStatusPrazo(lembrete: Lembrete): String {
    val dias = diasParaVencer(lembrete)
    return when {
        dias == null -> ""
        dias < 0 -> "Vencido"
        dias == 0 -> "Vence hoje"
        dias == 1 -> "Vence em 1 dia"
        else -> "Vence em $dias dias"
    }
}

fun dataParaOrdenacao(lembrete: Lembrete): LocalDate =
    parseDataLimite(lembrete) ?: LocalDate.MAX

fun calcularCorStatus(lembretes: List<Lembrete>, tipoAlvo: TipoManutencao): Color {
    val lembretesDoTipo = lembretes.filter { it.tipo == tipoAlvo }
    if (lembretesDoTipo.isEmpty()) return Color(0xFF334155) // Cinza escuro
    val hoje = LocalDate.now(); val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    var temVencido = false; var temUrgente = false
    for (item in lembretesDoTipo) { if (item.dataLimite.length == 10) { try { val dataItem = LocalDate.parse(item.dataLimite, formatter); val diasParaVencer = ChronoUnit.DAYS.between(hoje, dataItem); if (diasParaVencer < 0) temVencido = true else if (diasParaVencer <= 30) temUrgente = true } catch (e: Exception) { } } }
    return when { temVencido -> Color(0xFFEF4444); temUrgente -> Color(0xFFEAB308); else -> Color(0xFF10B981) }
}

fun calcularReputacao(lembretes: List<Lembrete>): Pair<String, String> {
    if (lembretes.isEmpty()) return "Sem histórico" to "Cadastre serviços para gerar uma reputação."
    val cores = TipoManutencao.values().map { calcularCorStatus(lembretes, it) }
    return when {
        cores.all { it == Color(0xFF10B981) || it == Color(0xFF334155) } ->
            "Excelente" to "Todas as manutenções estão em dia."
        cores.any { it == Color(0xFFEF4444) } ->
            "Crítica" to "Existem manutenções vencidas. Agende o quanto antes."
        else ->
            "Em atenção" to "Alguns lembretes estão próximos do vencimento."
    }
}
fun abrirWhatsApp(context: Context, telefone: String, mensagem: String) { try { val numeroLimpo = telefone.filter { it.isDigit() }; val numeroFinal = if (!numeroLimpo.startsWith("55") && numeroLimpo.length >= 10) "55$numeroLimpo" else numeroLimpo; val uri = Uri.parse("https://api.whatsapp.com/send?phone=$numeroFinal&text=${Uri.encode(mensagem)}"); context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: Exception) { Toast.makeText(context, "Erro ao abrir WhatsApp.", Toast.LENGTH_SHORT).show() } }
