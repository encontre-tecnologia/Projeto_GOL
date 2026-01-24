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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Payments
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

/* ----------------- ESTRUTURAS DE DADOS ----------------- */

data class ItemDetectado(
    val id: String = UUID.randomUUID().toString(),
    var nome: String,
    var tipo: TipoManutencao,
    var valor: Double = 0.0,
    var dataFutura: String = "",
    var quantidade: Int = 1
)

data class ResultadoCaptura(
    val arquivoFoto: File,
    val itensEncontrados: List<ItemDetectado>,
    val kmDetectado: Int?,
    val sugestoesProduto: List<String> = emptyList(),
    val linhasReconhecidas: List<String> = emptyList()
)

data class Lembrete(
    val id: String = UUID.randomUUID().toString(),
    val carroId: String,
    val contatoId: String? = null,
    val titulo: String,
    val peca: String = "",
    val dataLimite: String,
    val kmLimite: String,
    val tipo: TipoManutencao,
    val valor: Double = 0.0,
    val fotoPath: String? = null,
    val horaAviso: String = "09:00"
) : Serializable

private val marcaLogoMap = mapOf(
    "audi" to R.drawable.logo_audi,
    "bmw" to R.drawable.logo_bmw,
    "citroen" to R.drawable.logo_citroen,
    "citroem" to R.drawable.logo_citroen,
    "chevrolet" to R.drawable.logo_chevrolet,
    "chevy" to R.drawable.logo_chevrolet,
    "chevolet" to R.drawable.logo_chevrolet,
    "fiat" to R.drawable.logo_fiat,
    "ford" to R.drawable.logo_ford,
    "honda" to R.drawable.logo_honda,
    "hyundai" to R.drawable.logo_hyundai,
    "jeep" to R.drawable.logo_jeep,
    "kia" to R.drawable.logo_kia,
    "lamborghini" to R.drawable.logo_lamborghini,
    "mercedes" to R.drawable.logo_mercedes,
    "mercedesbenz" to R.drawable.logo_mercedes,
    "mitsubishi" to R.drawable.logo_mitsubishi,
    "nissan" to R.drawable.logo_nissan,
    "nissam" to R.drawable.logo_nissan,
    "peugeot" to R.drawable.logo_peugeot,
    "renault" to R.drawable.logo_renault,
    "toyota" to R.drawable.logo_toyota,
    "volkswagen" to R.drawable.logo_volkswagen,
    "vw" to R.drawable.logo_volkswagen
)

enum class TipoVeiculo(val label: String, @DrawableRes val iconRes: Int) {
    BICICLETA("Bicicleta", R.drawable.bike),
    CARRETINHA("Carretinha", R.drawable.carretinha),
    CARRO("Carro", R.drawable.carro),
    MOTO("Moto", R.drawable.moto),
    CAMINHONETE("Caminhonete", R.drawable.picap),
    TRATOR("Trator", R.drawable.trator)
}

val marcasSuportadas = listOf(
    "Audi",
    "BMW",
    "Citroën",
    "Citroen",
    "Citroem",
    "Chevrolet",
    "Fiat",
    "Ford",
    "Honda",
    "Hyundai",
    "Jeep",
    "Kia",
    "Lamborghini",
    "Mercedes-Benz",
    "Mitsubishi",
    "Nissan",
    "Nissam",
    "Peugeot",
    "Renault",
    "Toyota",
    "Volkswagen"
)

data class CarroInfo(
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "Novo Carro",
    val modelo: String = "Modelo 1.0",
    val marca: String = "",
    val corArgb: Int = 0xFF3B82F6.toInt(),
    val kmAtual: Int = 0,
    val tipoVeiculo: TipoVeiculo = TipoVeiculo.CARRO
) : Serializable {
    fun getCorUI(): Color = Color(corArgb)
}

@DrawableRes
fun CarroInfo.logoResOrNull(): Int? = logoResForMarca(marca)

@DrawableRes
fun CarroInfo.tipoIconRes(): Int = tipoVeiculo.iconRes

private fun normalizarMarca(marca: String): String =
    marca.trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]"), "")

@DrawableRes
fun logoResForMarca(marca: String): Int? = marcaLogoMap[normalizarMarca(marca)]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipoVeiculoSelector(
    selecionado: TipoVeiculo,
    onSelect: (TipoVeiculo) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selecionado.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de veículo") },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                Image(
                    painter = painterResource(id = selecionado.iconRes),
                    contentDescription = selecionado.label,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TipoVeiculo.values().forEach { tipo ->
                DropdownMenuItem(
                    text = { Text(tipo.label) },
                    onClick = {
                        onSelect(tipo)
                        expanded = false
                    },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = tipo.iconRes),
                            contentDescription = tipo.label,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

data class ContatoProfissional(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val telefone: String,
    val tipoServico: String
) : Serializable

data class Abastecimento(
    val id: String = UUID.randomUUID().toString(),
    val carroId: String,
    val data: String,
    val precoLitro: Double,
    val valorPago: Double,
    val litros: Double
) : Serializable

enum class TipoManutencao(val label: String) {
    OLEO("Óleo"),
    BATERIA("Bateria"),
    MECANICA("Mecânica"),
    FREIO("Freio/ABS"),
    TEMPERATURA("Temp."), // Mudado para ficar menor
    LICENCIAMENTO("Licença"),
    IPVA("IPVA"),

    SEGURO("Seguro"),
    OUTROS("Outros");

    fun getIcon(): ImageVector = when(this) {
        OLEO -> Icons.Rounded.WaterDrop
        BATERIA -> Icons.Rounded.BatteryAlert
        MECANICA -> Icons.Rounded.Build
        FREIO -> Icons.Rounded.ErrorOutline
        TEMPERATURA -> Icons.Rounded.Thermostat
        LICENCIAMENTO -> Icons.Rounded.Description
        IPVA -> Icons.Rounded.Payments
        SEGURO -> Icons.Rounded.Shield
        OUTROS -> Icons.Rounded.Edit
    }
}

