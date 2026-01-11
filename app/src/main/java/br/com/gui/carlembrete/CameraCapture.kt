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

private const val LER_NOTAS_HABILITADO = false

/* ----------------- CÂMERA INTELIGENTE ----------------- */

@Composable
fun CameraCapturaDialog(onDismiss: () -> Unit, onFotoCapturada: (ResultadoCaptura) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lanternaLigada by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            Toast.makeText(context, "Permita o uso da câmera para escanear o produto", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }
    if (!hasCameraPermission) return

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) { Log.e("Camera", "Erro", e) }
                }, executor)
                previewView
            }, modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 120.dp)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onDismiss, modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)) { Icon(Icons.Default.Close, "Fechar", tint = Color.White) }
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                lanternaLigada = !lanternaLigada
                                try {
                                    cameraControl?.enableTorch(lanternaLigada)
                                } catch (_: SecurityException) {
                                    lanternaLigada = !lanternaLigada
                                    Toast.makeText(context, "Não foi possível acessar o flash", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Permita o uso da câmera para ativar o flash", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                    ) { Icon(if (lanternaLigada) Icons.Default.FlashOn else Icons.Default.FlashOff, "Flash", tint = if (lanternaLigada) Color(0xFFF59E0B) else Color.White) }
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                    if (isProcessing) { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White); Spacer(Modifier.width(16.dp)); Text("Processando captura...", color = Color.White) } }
                    else { Text("Centralize o nome do produto dentro do retângulo para obter o melhor resultado.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center) }
                }
                Button(
                    onClick = {
                        if (!isProcessing) {
                            isProcessing = true
                            captureAndExtractItems(context, imageCapture!!) { resultado ->
                                isProcessing = false
                                onFotoCapturada(resultado)
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Processando..." else "Escanear Produto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun captureAndExtractItems(context: Context, imageCapture: ImageCapture, onResult: (ResultadoCaptura) -> Unit) {
    val executor = Executors.newSingleThreadExecutor()
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val bitmapBuffer = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees.toFloat()
            val matrix = Matrix().apply { postRotate(rotation) }
            val bitmapRotacionado = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
            val bitmapFocado = recortarAreaCentral(bitmapRotacionado)
            val arquivo = File(context.filesDir, "servico_scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(arquivo).use { out -> bitmapFocado.compress(Bitmap.CompressFormat.JPEG, 80, out) }

            val inputImage = InputImage.fromBitmap(bitmapFocado, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage).addOnSuccessListener { visionText ->
                val linhas = visionText.textBlocks.flatMap { it.lines }
                val linhasInfo = linhas.map {
                    val box = it.boundingBox
                    val area = if (box != null) box.width().coerceAtLeast(1) * box.height().coerceAtLeast(1) else 0
                    LinhaOCR(corrigirCaracteresVisuais(it.text), area, box?.height() ?: 0)
                }
                val sugestoesProduto = sugerirProdutosParaAviso(linhasInfo, visionText.text)
                val itensEncontrados = mutableListOf<ItemDetectado>()
                var kmDetectado: Int? = null

                if (LER_NOTAS_HABILITADO) {
                    var lendoObservacoes = false
                    val regexData = Regex("\\b(\\d{2})/(\\d{2})/(\\d{2,4})\\b")
                    var dataServico = LocalDate.now()
                    val matchData = regexData.find(visionText.text)
                    if (matchData != null) {
                        try {
                            var ano = matchData.groupValues[3]
                            if (ano.length == 2) ano = "20$ano"
                            dataServico = LocalDate.of(ano.toInt(), matchData.groupValues[2].toInt(), matchData.groupValues[1].toInt())
                        } catch (e: Exception) {}
                    }

                    val regexKm = Regex("(?i)(?:KM|ODOMETRO|HODOMETRO)[\\s:.]*(\\d{1,3}(?:[.,]\\d{3})*)")
                    val matchKm = regexKm.find(visionText.text)
                    if (matchKm != null) kmDetectado = matchKm.groupValues[1].replace(".", "").replace(",", "").toIntOrNull()

                    val keywordsServico = listOf("OLEO", "FILTRO", "ALINHAMENTO", "BALANCEAMENTO", "PASTILHA", "DISCO", "FREIO", "BATERIA", "SUSPENSAO", "PNEU", "RODIZIO", "LUBRAX", "HIGIENIZACAO", "REVISAO", "CORREIA", "LAMPADA", "FLUIDO", "ADITIVO", "AR-CONDICIONADO", "ELETRICO", "INJECAO", "VELA")
                    val regexViscosidade = Regex("\\b\\d{1,2}W-?\\d{2}\\b", RegexOption.IGNORE_CASE)
                    val regexPrecoParaRemocao = Regex("(R\\$|\\$)?\\s*\\d{1,4}(?:[.,]\\d{3})*[.,]\\d{2}")
                    val keywordsIgnorar = listOf("PLACA", "VEICULO", "CARRO", "MODELO", "KM", "ODOMETRO", "DATA", "CLIENTE", "CPF", "CNPJ", "TOTAL", "VALOR", "PAGAMENTO", "TELEFONE", "ENDERECO", "ENTRADA", "SAIDA", "NOME", "IE:", "CEP", "NOTA", "TESTE", "TESTADO", "DIAGNOSTICO")

                    for (linhaObj in linhas) {
                        val linhaRaw = linhaObj.text
                        val linhaNormalizada = linhaRaw.uppercase().unaccent()

                        if (linhaNormalizada.contains("OBSERVACOES") || linhaNormalizada.contains("OBS:") || linhaNormalizada.contains("CHECK") || linhaNormalizada.contains("RESUMO") || linhaNormalizada.contains("ITENS REVISADOS")) { lendoObservacoes = true; continue }
                        if (lendoObservacoes) continue

                        val linhaSemPreco = linhaRaw.replace(regexPrecoParaRemocao, "").replace(Regex("\\.{2,}"), " ").trim()
                        val partesDaLinha = linhaSemPreco.split(Regex("[,+/]"))

                        for (parte in partesDaLinha) {
                            val parteUpper = parte.uppercase().trim()
                            val parteNormalizada = parteUpper.unaccent()
                            val deveIgnorar = keywordsIgnorar.any { parteNormalizada.contains(it) }

                            if (!deveIgnorar && parteNormalizada.length > 2) {
                                val contemServico = keywordsServico.any { parteNormalizada.contains(it) }
                                val contemViscosidade = regexViscosidade.containsMatchIn(parteUpper)

                                if (contemServico || contemViscosidade) {
                                    var nomeLimpo = parte.replace(Regex("^[\\d-]{1,3}\\s"), "").replace(Regex("[\\[\\(][xX*][\\]\\)]"), "").trim()
                                    val tipo = when {
                                        parteNormalizada.contains("ALINHAMENTO") || parteNormalizada.contains("BALANCEAMENTO") || parteNormalizada.contains("SUSPENSAO") || parteNormalizada.contains("PNEU") -> TipoManutencao.MECANICA
                                        parteNormalizada.contains("OLEO") || parteNormalizada.contains("FILTRO") || parteNormalizada.contains("LUBRAX") || contemViscosidade -> TipoManutencao.OLEO
                                        parteNormalizada.contains("FREIO") || parteNormalizada.contains("PASTILHA") || parteNormalizada.contains("DISCO") -> TipoManutencao.FREIO
                                        parteNormalizada.contains("BATERIA") || parteNormalizada.contains("ELETRICO") || parteNormalizada.contains("LAMPADA") -> TipoManutencao.BATERIA
                                        parteNormalizada.contains("AR-CONDICIONADO") || parteNormalizada.contains("HIGIENIZACAO") -> TipoManutencao.OUTROS
                                        parteNormalizada.contains("CORREIA") || parteNormalizada.contains("VELA") || parteNormalizada.contains("INJECAO") -> TipoManutencao.MECANICA
                                        else -> TipoManutencao.OUTROS
                                    }
                                    val dataFuturaItem = calcularProximaData(tipo, dataServico)
                                    if (nomeLimpo.isNotBlank() && itensEncontrados.none { it.nome == nomeLimpo }) {
                                        val quantidadeItem = extrairQuantidadeDaParte(parteUpper) ?: 1
                                        itensEncontrados.add(ItemDetectado(nome = nomeLimpo, tipo = tipo, quantidade = quantidadeItem, dataFutura = dataFuturaItem))
                                    }
                                }
                            }
                        }
                    }
                    if (itensEncontrados.isEmpty()) {
                        itensEncontrados.add(ItemDetectado(nome = "Serviço Detectado (Editar)", tipo = TipoManutencao.OUTROS, valor = 0.0, dataFutura = calcularProximaData(TipoManutencao.OUTROS, dataServico)))
                    }
                }

                val itensParaRetorno = if (LER_NOTAS_HABILITADO) itensEncontrados else emptyList()
                val kmParaRetorno = if (LER_NOTAS_HABILITADO) kmDetectado else null
                val linhasReconhecidas = linhasInfo.map { it.texto }
                ContextCompat.getMainExecutor(context).execute { onResult(ResultadoCaptura(arquivo, itensParaRetorno, kmParaRetorno, sugestoesProduto, linhasReconhecidas)); image.close() }
            }.addOnFailureListener {
                val itensFallback = if (LER_NOTAS_HABILITADO) listOf(ItemDetectado(nome = "Novo Serviço", tipo = TipoManutencao.OUTROS, dataFutura = calcularProximaData(TipoManutencao.OUTROS, LocalDate.now()))) else emptyList()
                ContextCompat.getMainExecutor(context).execute { onResult(ResultadoCaptura(arquivo, itensFallback, null, emptyList(), emptyList())); image.close() }
            }
        }
        override fun onError(exception: ImageCaptureException) {}
    })
}

private data class CandidatoProduto(
    val texto: String,
    val score: Int,
    val uppercaseRatio: Float,
    val letras: Int
)

private data class LinhaOCR(val texto: String, val area: Int, val altura: Int) {
    fun dividirEmTokens(): List<LinhaOCR> {
        val tokens = texto.split(Regex("[\\s/\\\\|-]+"))
            .map { it.trim().replace(Regex("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$"), "") }
            .filter { it.length >= 3 }
        return tokens.map { token -> LinhaOCR(token, area, altura) }
    }
}

private val termosPromocionaisPadrao = listOf(
    "MAIOR", "VIDA", "UTIL", "PROTEGE", "PROTECAO", "QUALIDADE", "CONFIANCA",
    "LIMPO", "MANTEM", "MANTE", "DESEMPENHO", "SEGURANCA", "GARANTIA", "EFICIENCIA",
    "POTENCIA", "RESISTENTE", "OTIMO", "ULTRA", "NOVA", "NOVO", "MOTOR", "ESSENCIAL", "ESSENCIAL", "ST", "SL"
)

private val dicionarioProdutosPrincipais = listOf(
    "LUBRAX", "PETROBRAS", "PIONEIRO", "MBR", "CASTROL", "SHELL", "MOTUL",
    "PIRELLI", "BOSCH", "DELCO", "ACDELCO", "MOBIL", "TOTAL"
)

val padraoUrlOuContato = Regex("(?i)(WWW\\.|HTTP|HTTPS|\\.COM|\\.NET|\\.ORG|\\.BR|@)")

private fun sugerirProdutosParaAviso(linhas: List<LinhaOCR>, textoCompleto: String): List<String> {
    if (linhas.isEmpty() && textoCompleto.isBlank()) return emptyList()
    val candidatos = mutableListOf<CandidatoProduto>()
    val maiorArea = linhas.maxOfOrNull { it.area }?.coerceAtLeast(1) ?: 1
    val termosIgnorados = listOf(
        "PLACA", "VEICULO", "CARRO", "KM", "ODOMETRO", "DATA", "TOTAL",
        "VALOR", "SERVICO", "CLIENTE", "NOTA", "NF", "ENDERECO", "CNPJ",
        "CPF", "TELEFONE", "GARANTIA", "QUANTIDADE", "CODIGO", "REFERENCIA",
        "MODELO", "MARCA", "ASSINATURA", "HORA", "PRODUTO"
    )

    fun avaliarCandidato(textoOriginal: String, bonus: Int = 0, area: Int = 0) {
        val normalizado = normalizarTextoProduto(textoOriginal)
        if (normalizado.length < 3) return
        val upper = normalizado.uppercase(Locale.ROOT)
        if (termosIgnorados.any { upper.contains(it) }) return
        if (padraoUrlOuContato.containsMatchIn(upper)) return
        val textoCanonico = corrigirTokenPorDicionario(upper) ?: upper
        if (isTextoPromocional(textoCanonico)) return
        val letras = textoCanonico.count { it.isLetter() }
        if (letras < 3) return
        val digitos = textoCanonico.count { it.isDigit() }
        val palavras = textoCanonico.split(" " ).filter { it.length > 2 }
        val promocionais = palavras.count { tokenEhPromocional(it) }
        if (palavras.isNotEmpty() && promocionais.toFloat() / palavras.size > 0.5f) return
        val maiusculas = textoCanonico.count { it.isUpperCase() }
        val uppercaseRatio = if (letras > 0) maiusculas.toFloat() / letras else 0f
        var score = letras * 2 + palavras.size * 3 - digitos * 2 + bonus
        if (textoCanonico.length > 30) score -= 4
        if (palavras.size >= 2) score += 5
        score += (uppercaseRatio * 12).roundToInt()
        if (uppercaseRatio > 0.9f && digitos == 0 && textoCanonico.length in 4..16) score += 18
        else if (uppercaseRatio > 0.7f && letras >= 4) score += 8
        if (area > 0) {
            val areaRatio = area.toFloat() / maiorArea
            score += (areaRatio * 20).roundToInt()
        }
        if (score > 0) candidatos.add(CandidatoProduto(textoCanonico.trim(), score, uppercaseRatio, letras))
    }

    val entradas = buildList {
        addAll(linhas)
        linhas.forEach { addAll(it.dividirEmTokens()) }
    }

    entradas.forEach { linha ->
        val texto = linha.texto.trim()
        if (contemSequenciaPromocional(texto)) return@forEach
        if (texto.isNotBlank()) {
            val letrasNaLinha = texto.count { it.isLetter() }
            val bonus = if (letrasNaLinha > 0 && texto.count { it.isUpperCase() } >= (letrasNaLinha * 0.6)) 2 else 0
            avaliarCandidato(texto, bonus, linha.area)
        }
    }

    normalizarTextoProduto(textoCompleto)
        .split(" ")
        .map(String::trim)
        .filter { it.length >= 4 }
        .forEach { token ->
            if (token.isNotBlank()) {
                val tokenFormatado = token.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
                avaliarCandidato(tokenFormatado, -1)
            }
        }

    if (candidatos.isEmpty()) return emptyList()

    val sugestoesOrdenadas = candidatos
        .sortedWith(
            compareByDescending<CandidatoProduto> { it.score }
                .thenByDescending { it.uppercaseRatio }
                .thenByDescending { it.letras }
        )
        .map { it.texto.trim().take(60) }
        .distinctBy { normalizarTextoProduto(it).uppercase(Locale.ROOT) }

    val melhorDireto = sugestoesOrdenadas.firstOrNull()
    if (melhorDireto != null) return listOf(melhorDireto)

    val primeiroCodigoLinha = extrairCodigoComEspacos(linhas)
    if (primeiroCodigoLinha != null) return listOf(primeiroCodigoLinha)
    if (melhorDireto != null) return listOf(melhorDireto)

    val fallbackCodigos = extrairCodigosLegiveis(textoCompleto)
    if (fallbackCodigos.isNotEmpty()) return listOf(fallbackCodigos.first())

    val fallbackLinha = linhas
        .map { it.texto.trim() }
        .filter { it.length in 4..80 }
        .maxByOrNull { calcularLegibilidadeLinha(it) }

    return fallbackLinha?.let { listOf(it) } ?: emptyList()
}

private fun normalizarTextoProduto(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace("[^\\p{L}\\p{Nd} ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

private fun extrairCodigosLegiveis(texto: String): List<String> {
    val normalizado = normalizarTextoProduto(texto).uppercase(Locale.ROOT)
    val regexCodigo = Regex("\\b[A-Z0-9]{4,}\\b")
    return regexCodigo.findAll(normalizado)
        .map { it.value }
        .distinct()
        .toList()
}

private fun extrairCodigoComEspacos(linhas: List<LinhaOCR>): String? {
    val regex = Regex("([A-Z0-9]{2,}(?:\\s+[A-Z0-9]{1,}){1,3})")
    return linhas.map { it.texto.trim() }.firstNotNullOfOrNull { linha ->
        val match = regex.find(linha.uppercase(Locale.ROOT))
        match?.value?.takeIf {
            val semEspaco = it.replace(" ", "")
            semEspaco.length >= 4
        }?.trim()
    }
}

private fun calcularLegibilidadeLinha(texto: String): Int {
    val normalizado = normalizarTextoProduto(texto)
    val letras = normalizado.count { it.isLetter() }
    val maiusculas = normalizado.count { it.isUpperCase() }
    val uppercaseRatio = if (letras > 0) maiusculas.toFloat() / letras else 0f
    val digitos = normalizado.count { it.isDigit() }
    var score = letras * 2 - digitos
    score += (uppercaseRatio * 10).roundToInt()
    if (uppercaseRatio > 0.8f) score += 5
    if (normalizado.length in 4..20) score += 4
    return score
}

internal fun detectarTipoPeloTexto(texto: String): TipoManutencao {
    val normalized = texto.uppercase(Locale.ROOT).unaccent()
    return when {
        listOf("OLEO", "LUBRAX", "LUBRIFICANTE", "20W", "15W", "5W").any { normalized.contains(it) } -> TipoManutencao.OLEO
        listOf("BATERIA", "MBR", "AMP", "12V", "VOLTS").any { normalized.contains(it) } -> TipoManutencao.BATERIA
        listOf("FREIO", "PASTILHA", "ABS").any { normalized.contains(it) } -> TipoManutencao.FREIO
        listOf("AR COND", "AR-COND", "CLIMA", "REFRIG").any { normalized.contains(it) } -> TipoManutencao.TEMPERATURA
        listOf("FILTRO", "CORREIA", "VELA", "INJECAO", "PNEU").any { normalized.contains(it) } -> TipoManutencao.MECANICA
        else -> TipoManutencao.OUTROS
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer; val bytes = ByteArray(buffer.remaining()); buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}



private fun contemSequenciaPromocional(texto: String): Boolean {
    val clean = texto.uppercase(Locale.ROOT).unaccent()
    val termosBloco = listOf("PROTEGE", "MANTEM", "MANTE", "LIMPO", "MOTOR")
    val tokens = clean.split(" " ).filter { it.isNotBlank() }
    if (tokens.size < 2) return false
    for (i in 0 until tokens.size - 1) {
        val primeira = tokens[i]
        val segunda = tokens[i + 1]
        if ((primeira.startsWith("PROTEGE") && segunda.startsWith("MANT")) ||
            (primeira.startsWith("MANT") && segunda.contains("LIMP")) ||
            (primeira.contains("LIMP") && segunda.contains("MOTOR")) ||
            primeira.contains("MOTOR")
        ) {
            return true
        }
    }
    return false
}

private fun tokenEhPromocional(tokenRaw: String): Boolean {
    if (tokenRaw.isBlank()) return false
    val normalizado = tokenRaw.uppercase(Locale.ROOT).unaccent()
    if (termosPromocionaisPadrao.contains(normalizado)) return true
    if (normalizado.length > 1 && (normalizado[0] == 'O' || normalizado[0] == 'A' || normalizado[0] == 'E')) {
        val semPrefixo = normalizado.substring(1)
        if (termosPromocionaisPadrao.contains(semPrefixo)) return true
    }
    return false
}

private fun corrigirTokenPorDicionario(token: String): String? {
    val clean = token.uppercase(Locale.ROOT).unaccent()
    return dicionarioProdutosPrincipais.firstOrNull { distanciaLevenshtein(clean, it) <= 1 }
}

private fun distanciaLevenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[a.length][b.length]
}

fun isTextoPromocional(texto: String): Boolean {
    val clean = texto.uppercase(Locale.ROOT).unaccent()
    val tokens = clean.split(" " ).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return false
    val count = tokens.count { tokenEhPromocional(it) }
    val total = tokens.size
    if (total == 0) return false
    val percent = count.toFloat() / total
    val temBarra = tokens.any { it.contains('/') }
    return percent > 0.5f || (temBarra && percent > 0.3f)
}

private fun corrigirCaracteresVisuais(texto: String): String =
    buildString {
        texto.forEach { char ->
            append(
                when (char) {
                    '/', '\\' -> 'L'
                    else -> char
                }
            )
        }
    }

private fun recortarAreaCentral(bitmap: Bitmap): Bitmap {
    val largura = bitmap.width
    val altura = bitmap.height
    val larguraTarget = (largura * 0.55).toInt().coerceAtLeast(1)
    val alturaTarget = (altura * 0.25).toInt().coerceAtLeast(1)
    val inicioX = ((largura - larguraTarget) / 2).coerceAtLeast(0)
    val inicioY = ((altura - alturaTarget) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(
        bitmap,
        inicioX,
        inicioY,
        larguraTarget.coerceAtMost(largura),
        alturaTarget.coerceAtMost(altura)
    )
}
