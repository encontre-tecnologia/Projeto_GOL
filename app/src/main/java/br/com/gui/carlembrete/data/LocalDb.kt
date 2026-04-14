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

/* ----------------- BANCO DE DADOS LOCAL ----------------- */

object BancoDeDados {
    private const val FILE_CARROS = "carros_v3.dat"
    private const val FILE_LEMBRETES = "lembretes_v3.dat"
    private const val FILE_CONTATOS = "contatos_v3.dat"
    private const val FILE_ABASTECIMENTOS = "abastecimentos_v3.dat"
    private const val FILE_PEDALADAS = "pedaladas_v1.dat"

    fun salvarCarros(context: Context, lista: List<CarroInfo>) = salvar(context, FILE_CARROS, lista)
    fun carregarCarros(context: Context): List<CarroInfo>? = carregar<List<CarroInfo>>(context, FILE_CARROS)
    fun carregarCarrosComFallback(context: Context): List<CarroInfo> = carregarCarros(context).orEmpty()

    fun salvarLembretes(context: Context, lista: List<Lembrete>) = salvar(context, FILE_LEMBRETES, lista)
    fun carregarLembretes(context: Context): List<Lembrete> = carregar<List<Lembrete>>(context, FILE_LEMBRETES) ?: emptyList()

    fun salvarContatos(context: Context, lista: List<ContatoProfissional>) = salvar(context, FILE_CONTATOS, lista)
    fun carregarContatos(context: Context): List<ContatoProfissional> = carregar<List<ContatoProfissional>>(context, FILE_CONTATOS) ?: emptyList()

    fun salvarAbastecimentos(context: Context, lista: List<Abastecimento>) = salvar(context, FILE_ABASTECIMENTOS, lista)
    fun carregarAbastecimentos(context: Context): List<Abastecimento> = carregar<List<Abastecimento>>(context, FILE_ABASTECIMENTOS) ?: emptyList()

    fun salvarPedaladas(context: Context, lista: List<Pedalada>) = salvar(context, FILE_PEDALADAS, lista)
    fun carregarPedaladas(context: Context): List<Pedalada> = carregar<List<Pedalada>>(context, FILE_PEDALADAS) ?: emptyList()

    private fun <T> salvar(context: Context, fileName: String, data: T) { try { context.openFileOutput(fileName, Context.MODE_PRIVATE).use { fos -> ObjectOutputStream(fos).use { it.writeObject(data) } } } catch (e: Exception) { e.printStackTrace() } }
    private fun <T> carregar(context: Context, fileName: String): T? { val file = File(context.filesDir, fileName); if (!file.exists()) return null; return try { context.openFileInput(fileName).use { fis -> ObjectInputStream(fis).use { it.readObject() as T } } } catch (e: Exception) { e.printStackTrace(); null } }
}
