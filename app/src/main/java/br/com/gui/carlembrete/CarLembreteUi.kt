package br.com.gui.carlembrete

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

private val dialogBorderStroke = BorderStroke(0.2.dp, Color.White.copy(alpha = 0.2f))
private val dialogCornerShape = RoundedCornerShape(20.dp)
private val dialogActionButtonShape = RoundedCornerShape(8.dp)
private const val LER_NOTAS_HABILITADO = false

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
    val dataLimite: String,
    val kmLimite: String,
    val tipo: TipoManutencao,
    val valor: Double = 0.0,
    val fotoPath: String? = null,
    val horaAviso: String = "09:00"
) : Serializable

data class CarroInfo(
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "Novo Carro",
    val modelo: String = "Modelo 1.0",
    val marca: String = "",
    val corArgb: Int = 0xFF3B82F6.toInt(),
    val kmAtual: Int = 0
) : Serializable {
    fun getCorUI(): Color = Color(corArgb)
}

data class ContatoProfissional(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val telefone: String,
    val tipoServico: String
) : Serializable

enum class TipoManutencao(val label: String) {
    OLEO("Óleo"),
    BATERIA("Bateria"),
    MECANICA("Mecânica"),
    FREIO("Freio/ABS"),
    TEMPERATURA("Temp."), // Mudado para ficar menor
    OUTROS("Outros");

    fun getIcon(): ImageVector = when(this) {
        OLEO -> Icons.Rounded.WaterDrop
        BATERIA -> Icons.Rounded.BatteryAlert
        MECANICA -> Icons.Rounded.Build
        FREIO -> Icons.Rounded.ErrorOutline
        TEMPERATURA -> Icons.Rounded.Thermostat
        OUTROS -> Icons.Rounded.Edit
    }
}

data class SugestaoRapida(val texto: String, val tipo: TipoManutencao)


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

private val padraoUrlOuContato = Regex("(?i)(WWW\\.|HTTP|HTTPS|\\.COM|\\.NET|\\.ORG|\\.BR|@)")

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

private fun detectarTipoPeloTexto(texto: String): TipoManutencao {
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

/* ----------------- NAVEGAÇÃO ----------------- */

/* ----------------- ONBOARDING ----------------- */

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var carroNome by remember { mutableStateOf("") }
    var carroMarca by remember { mutableStateOf("") }
    var carroModeloUnico by remember { mutableStateOf("") }
    var carroKm by remember { mutableStateOf("") }
    var carroCor by remember { mutableIntStateOf(0xFF3B82F6.toInt()) }
    var frotaTemporaria by remember { mutableStateOf(listOf<CarroInfo>()) }
    var contatosAdicionados by remember { mutableStateOf(listOf<ContatoProfissional>()) }
    var showContatoDialog by remember { mutableStateOf(false) }

    if (showContatoDialog) NovoContatoDialog(onDismiss = { showContatoDialog = false }, onSalvar = { novo -> contatosAdicionados = contatosAdicionados + novo; scope.launch(Dispatchers.IO) { BancoDeDados.salvarContatos(context, contatosAdicionados) }; showContatoDialog = false })
    val coresDisponiveis = listOf(0xFF3B82F6.toInt(), 0xFFEF4444.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFF94A3B8.toInt(), 0xFFFFFFFF.toInt())

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
                        if (frotaTemporaria.isNotEmpty()) { Text("Carros Adicionados:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8)); Spacer(Modifier.height(8.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) { items(frotaTemporaria) { c -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color(c.corArgb), modifier = Modifier.size(40.dp)); Spacer(Modifier.height(4.dp)); Text(text = c.nome, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) } } } }
                        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.height(170.dp).width(160.dp)) { Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(160.dp).align(Alignment.BottomCenter)); Box(modifier = Modifier.size(width = 38.dp, height = 58.dp).background(Color(0xFF4B5563), shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).align(Alignment.BottomCenter).padding(bottom = 2.dp)); Icon(imageVector = Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color(carroCor), modifier = Modifier.size(100.dp).align(Alignment.BottomCenter).offset(y = (-4).dp)) }
                        Spacer(Modifier.height(16.dp)); Text("Sua Garagem", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp)); OutlinedTextField(value = carroNome, onValueChange = { carroNome = it }, label = { Text("Apelido (ex: Fox do Gui)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp)); OutlinedTextField(value = carroMarca, onValueChange = { carroMarca = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp)); OutlinedTextField(value = carroModeloUnico, onValueChange = { carroModeloUnico = it }, label = { Text("Modelo e Motor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp)); OutlinedTextField(value = carroKm, onValueChange = { if (it.all(Char::isDigit)) carroKm = it }, label = { Text("KM Atual") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        Spacer(Modifier.height(16.dp)); Text("Selecione a cor do veículo:", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { coresDisponiveis.forEach { cor -> Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(cor)).border(width = if (carroCor == cor) 3.dp else 0.dp, color = if (carroCor == cor) Color.White else Color.Transparent, shape = CircleShape).clickable { carroCor = cor }) } }
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(onClick = { if (carroNome.isNotBlank() && carroModeloUnico.isNotBlank()) { val novo = CarroInfo(nome = carroNome, modelo = carroModeloUnico, marca = carroMarca, kmAtual = carroKm.toIntOrNull() ?: 0, corArgb = carroCor); frotaTemporaria = frotaTemporaria + novo; carroNome = ""; carroMarca = ""; carroModeloUnico = ""; carroKm = "" } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6))) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar Outro Carro") }
                        Spacer(Modifier.height(8.dp)); Button(onClick = { var listaFinal = frotaTemporaria; if (carroNome.isNotBlank() || carroModeloUnico.isNotBlank() || carroMarca.isNotBlank()) { val ultimo = CarroInfo(nome = if(carroNome.isBlank()) "Carro" else carroNome, modelo = carroModeloUnico, marca = carroMarca, kmAtual = carroKm.toIntOrNull() ?: 0, corArgb = carroCor); listaFinal = listaFinal + ultimo }; val listaSalvar = if (listaFinal.isNotEmpty()) listaFinal else listOf(CarroInfo(nome = "Meu Carro", modelo = "Padrão", marca = "Marca", kmAtual = 0)); scope.launch(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaSalvar) }; step = 3 }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) { Text("Salvar e Continuar", fontSize = 18.sp) }
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

/* ----------------- BANCO DE DADOS LOCAL ----------------- */

object BancoDeDados {
    private const val FILE_CARROS = "carros_v3.dat"
    private const val FILE_LEMBRETES = "lembretes_v3.dat"
    private const val FILE_CONTATOS = "contatos_v3.dat"

    fun salvarCarros(context: Context, lista: List<CarroInfo>) = salvar(context, FILE_CARROS, lista)
    fun carregarCarros(context: Context): List<CarroInfo>? = carregar<List<CarroInfo>>(context, FILE_CARROS)
    fun carregarCarrosComFallback(context: Context): List<CarroInfo> { val lista = carregarCarros(context); return if (lista.isNullOrEmpty()) listOf(CarroInfo(nome = "Carro Padrão", modelo = "Modelo 1.0", marca = "Marca Padrão", kmAtual = 0)) else lista }

    fun salvarLembretes(context: Context, lista: List<Lembrete>) = salvar(context, FILE_LEMBRETES, lista)
    fun carregarLembretes(context: Context): List<Lembrete> = carregar<List<Lembrete>>(context, FILE_LEMBRETES) ?: emptyList()

    fun salvarContatos(context: Context, lista: List<ContatoProfissional>) = salvar(context, FILE_CONTATOS, lista)
    fun carregarContatos(context: Context): List<ContatoProfissional> = carregar<List<ContatoProfissional>>(context, FILE_CONTATOS) ?: emptyList()

    private fun <T> salvar(context: Context, fileName: String, data: T) { try { context.openFileOutput(fileName, Context.MODE_PRIVATE).use { fos -> ObjectOutputStream(fos).use { it.writeObject(data) } } } catch (e: Exception) { e.printStackTrace() } }
    private fun <T> carregar(context: Context, fileName: String): T? { val file = File(context.filesDir, fileName); if (!file.exists()) return null; return try { context.openFileInput(fileName).use { fis -> ObjectInputStream(fis).use { it.readObject() as T } } } catch (e: Exception) { e.printStackTrace(); null } }
}

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

private fun dataParaOrdenacao(lembrete: Lembrete): LocalDate =
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

/* ----------------- TELA PRINCIPAL (UI Melhorada) ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManutencaoScreen(modifier: Modifier = Modifier, context: Context = LocalContext.current) {
    var listaCarros by remember { mutableStateOf<List<CarroInfo>>(emptyList()) }
    var listaContatos by remember { mutableStateOf<List<ContatoProfissional>>(emptyList()) }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val carros = BancoDeDados.carregarCarrosComFallback(context)
            val contatos = BancoDeDados.carregarContatos(context)
            val lembretes = BancoDeDados.carregarLembretes(context)
            withContext(Dispatchers.Main) {
                listaCarros = carros
                listaContatos = contatos
                todosLembretes = lembretes
                isLoading = false
                NotificacaoHelper.reagendarExistentes(context.applicationContext, lembretes)
            }
        }
    }

    LaunchedEffect(listaCarros) { if(!isLoading && listaCarros.isNotEmpty()) withContext(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaCarros) } }
    LaunchedEffect(listaContatos) { if(!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarContatos(context, listaContatos) } }
    LaunchedEffect(todosLembretes) { if(!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarLembretes(context, todosLembretes) } }

    var indiceCarroAtual by remember { mutableIntStateOf(0) }
    val carroAtual = if (listaCarros.isNotEmpty()) {
        if (indiceCarroAtual >= listaCarros.size) indiceCarroAtual = 0
        listaCarros[indiceCarroAtual]
    } else { CarroInfo() }

    var showEditCarDialog by remember { mutableStateOf(false) }
    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddLembreteDialog by remember { mutableStateOf(false) }
    var iniciarCameraProduto by remember { mutableStateOf(false) }
    var showAddContatoDialog by remember { mutableStateOf(false) }
    var showRelatorioDialog by remember { mutableStateOf(false) }
    var showTesteNotificacaoDialog by remember { mutableStateOf(false) }
    var showConfiguracoes by remember { mutableStateOf(false) }
    var lembreteSelecionado by remember { mutableStateOf<Lembrete?>(null) }
    var contatoDetalheSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    val lembretesDoCarroAtual = todosLembretes.filter { it.carroId == carroAtual.id }
    var isCalendarMode by remember { mutableStateOf(false) }
    var calendarioMes by remember { mutableStateOf(YearMonth.now()) }
    var diaSelecionado by remember { mutableStateOf<LocalDate?>(null) }
    val formatterData = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val lembretesPorData = lembretesDoCarroAtual.mapNotNull { lembrete ->
        runCatching { LocalDate.parse(lembrete.dataLimite, formatterData) }.getOrNull()?.let { it to lembrete }
    }.groupBy({ it.first }, { it.second })

    LaunchedEffect(isCalendarMode, lembretesPorData, calendarioMes) {
        if (isCalendarMode) {
            val dataAtual = diaSelecionado
            if (dataAtual == null || !lembretesPorData.containsKey(dataAtual)) {
                diaSelecionado = lembretesPorData.keys.firstOrNull { YearMonth.from(it) == calendarioMes }
                    ?: lembretesPorData.keys.minOrNull()
            }
        }
    }

    if (showEditCarDialog) { EditarCarroDialog(carroAtual = carroAtual, titulo = "Editar Veículo", onDismiss = { showEditCarDialog = false }, onSalvar = { carroEditado -> listaCarros = listaCarros.map { if (it.id == carroAtual.id) carroEditado else it }; showEditCarDialog = false }) }
    if (showAddCarDialog) { EditarCarroDialog(carroAtual = CarroInfo(nome = "", modelo = ""), titulo = "Novo Carro", onDismiss = { showAddCarDialog = false }, onSalvar = { novoCarro -> listaCarros = listaCarros + novoCarro; indiceCarroAtual = listaCarros.lastIndex; showAddCarDialog = false }) }
    if (showAddContatoDialog) { NovoContatoDialog(onDismiss = { showAddContatoDialog = false }, onSalvar = { novo -> listaContatos = listaContatos + novo; showAddContatoDialog = false }) }
    if (showRelatorioDialog) {
        RelatorioVeiculoScreen(carroAtual = carroAtual, lembretes = lembretesDoCarroAtual, onDismiss = { showRelatorioDialog = false })
    }
    if (showTesteNotificacaoDialog) {
        NotificacaoRapidaDialog(
            onDismiss = { showTesteNotificacaoDialog = false },
            onDisparar = {
                NotificacaoHelper.dispararNotificacaoInstantanea(
                    context.applicationContext,
                    "Aviso imediato",
                    "Esse é um disparo rápido de teste."
                )
                Toast.makeText(context, "Notificação enviada!", Toast.LENGTH_SHORT).show()
            }
        )
    }
    if (showConfiguracoes) {
        ConfiguracoesScreen(
            onDismiss = { showConfiguracoes = false },
            onTestarNotificacao = {
                showTesteNotificacaoDialog = true
                showConfiguracoes = false
            }
        )
    }
    lembreteSelecionado?.let { selecionado ->
        LembreteDetalhesDialog(
            lembrete = selecionado,
            contato = contatoDetalheSelecionado,
            carro = carroAtual,
            onDismiss = {
                lembreteSelecionado = null
                contatoDetalheSelecionado = null
            },
            onSalvar = { atualizado ->
                todosLembretes = todosLembretes.map { if (it.id == atualizado.id) atualizado else it }
                NotificacaoHelper.cancelarNotificacao(context.applicationContext, atualizado.id)
                NotificacaoHelper.agendarNotificacao(context.applicationContext, atualizado, atualizado.horaAviso)
                lembreteSelecionado = atualizado
                contatoDetalheSelecionado = listaContatos.find { it.id == atualizado.contatoId }
                Toast.makeText(context, "Aviso atualizado!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddLembreteDialog) {
        NovoAgendamentoDialog(
            carroAtual = carroAtual,
            contatosDisponiveis = listaContatos,
            onDismiss = { showAddLembreteDialog = false; iniciarCameraProduto = false },
            onConfirm = { novo ->
                todosLembretes = todosLembretes + novo.copy(carroId = carroAtual.id)
                showAddLembreteDialog = false
                iniciarCameraProduto = false
            },
            onMultiConfirm = { novosItens ->
                val novosLembretes = novosItens.map { it.copy(carroId = carroAtual.id) }
                todosLembretes = todosLembretes + novosLembretes
                showAddLembreteDialog = false
                iniciarCameraProduto = false
                Toast.makeText(context, "${novosLembretes.size} itens salvos!", Toast.LENGTH_SHORT).show()
            },
            onUpdateKmCarro = { novoKm -> listaCarros = listaCarros.map { if (it.id == carroAtual.id) it.copy(kmAtual = novoKm) else it } },
            autoAbrirCamera = iniciarCameraProduto,
            onAutoCameraConsumida = { iniciarCameraProduto = false }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    iniciarCameraProduto = false
                    showAddLembreteDialog = true
                },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) { Icon(Icons.Rounded.Add, "Novo", modifier = Modifier.size(32.dp)) }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
        } else {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
                // Header Topo
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("CarLembrete", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Gestão de Frota", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAddCarDialog = true }, modifier = Modifier.background(Color(0xFF1E293B), CircleShape)) {
                            Icon(Icons.Rounded.DirectionsCar, "Add Carro", tint = Color(0xFF10B981))
                        }
                        IconButton(onClick = { showConfiguracoes = true }, modifier = Modifier.background(Color(0xFF1E293B), CircleShape)) {
                            Icon(Icons.Default.Settings, "Configurações", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                // Botão de identificação por câmera
                Button(
                    onClick = {
                        iniciarCameraProduto = true
                        showAddLembreteDialog = true
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Escanear produto")
                    Spacer(Modifier.width(8.dp))
                    Text("Identificar Produto", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                // Card Principal do Carro (Com Gradiente)
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED)))) // Gradiente Azul -> Roxo
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (indiceCarroAtual > 0) indiceCarroAtual-- else indiceCarroAtual = listaCarros.lastIndex }) { Icon(Icons.Default.ArrowBackIosNew, "Anterior", tint = Color.White.copy(0.8f)) }
                            Box(modifier = Modifier.size(80.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                            IconButton(onClick = { if (indiceCarroAtual < listaCarros.lastIndex) indiceCarroAtual++ else indiceCarroAtual = 0 }) { Icon(Icons.Default.ArrowForwardIos, "Próximo", tint = Color.White.copy(0.8f)) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(carroAtual.nome, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        val descricaoModelo = listOf(carroAtual.marca, carroAtual.modelo).filter { it.isNotBlank() }.joinToString(" - ")
                        if (descricaoModelo.isNotBlank()) {
                            Text(descricaoModelo, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.Black.copy(0.2f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${carroAtual.kmAtual} km", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(20.dp))
                        // Botão Editar dentro do card
                        OutlinedButton(
                            onClick = { showEditCarDialog = true },
                            modifier = Modifier.height(36.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Editar Detalhes", fontSize = 12.sp) }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Painel de Status
                val contagemPorTipo = TipoManutencao.values().associateWith { tipo -> lembretesDoCarroAtual.count { it.tipo == tipo } }
                Text("Status Geral", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))

                val statusBorderColor = Color.White.copy(alpha = 0.25f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, statusBorderColor, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text("Status Geral", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MonitorIcon(TipoManutencao.OLEO, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.OLEO), contagemPorTipo[TipoManutencao.OLEO] ?: 0)
                        MonitorIcon(TipoManutencao.MECANICA, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.MECANICA), contagemPorTipo[TipoManutencao.MECANICA] ?: 0)
                        MonitorIcon(TipoManutencao.BATERIA, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.BATERIA), contagemPorTipo[TipoManutencao.BATERIA] ?: 0)
                        MonitorIcon(TipoManutencao.FREIO, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.FREIO), contagemPorTipo[TipoManutencao.FREIO] ?: 0)
                        MonitorIcon(TipoManutencao.TEMPERATURA, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.TEMPERATURA), contagemPorTipo[TipoManutencao.TEMPERATURA] ?: 0)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Agenda", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isCalendarMode = false },
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (!isCalendarMode) Color(0xFF3B82F6) else Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(Icons.Default.ViewList, contentDescription = "Exibir lista", tint = Color.White)
                        }
                        IconButton(
                            onClick = { isCalendarMode = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isCalendarMode) Color(0xFF3B82F6) else Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Exibir calendário", tint = Color.White)
                        }
                        IconButton(
                            onClick = { showTesteNotificacaoDialog = true },
                            modifier = Modifier.size(40.dp).background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Teste de notificação", tint = Color(0xFFF97316))
                        }
                        Button(
                            onClick = { showRelatorioDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Relatório", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Relatório", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                    val lembretesOrdenados = lembretesDoCarroAtual.sortedBy { dataParaOrdenacao(it) }

                    if (lembretesDoCarroAtual.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventNote, null, tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Nenhum registro", color = Color(0xFF64748B))
                            }
                        }
                    } else {
                        if (!isCalendarMode) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val descricaoModeloCompleto = listOf(carroAtual.marca, carroAtual.modelo)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                    .ifBlank { carroAtual.modelo }
                                lembretesOrdenados.forEach { lembrete ->
                                    val statusColor = calcularCorStatus(lembretesDoCarroAtual, lembrete.tipo)
                                    val statusLabel = textoStatusPrazo(lembrete)
                                    LembreteCard(
                                        lembrete = lembrete,
                                        contato = listaContatos.find { it.id == lembrete.contatoId },
                                        modeloCarro = descricaoModeloCompleto,
                                        onDelete = {
                                            NotificacaoHelper.cancelarNotificacao(context.applicationContext, lembrete.id)
                                            todosLembretes = todosLembretes.filter { it.id != lembrete.id }
                                        },
                                        onClick = {
                                            lembreteSelecionado = lembrete
                                            contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                                        },
                                        statusLabel = statusLabel,
                                        statusColor = statusColor
                                    )
                                }
                            }
                        } else {
                            CalendarLembretesView(
                                mesAtual = calendarioMes,
                                onMesAnterior = { calendarioMes = calendarioMes.minusMonths(1) },
                                onProximoMes = { calendarioMes = calendarioMes.plusMonths(1) },
                                lembretesPorData = lembretesPorData,
                                diaSelecionado = diaSelecionado,
                                onSelecionarDia = { diaSelecionado = it },
                                onAbrirLembrete = { lembrete ->
                                    lembreteSelecionado = lembrete
                                    contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                                },
                                modeloCarro = listOf(carroAtual.marca, carroAtual.modelo).filter { it.isNotBlank() }.joinToString(" ").ifBlank { carroAtual.modelo },
                                contatoProvider = { lembrete -> listaContatos.find { it.id == lembrete.contatoId } },
                                onExcluir = { alvo ->
                                    NotificacaoHelper.cancelarNotificacao(context.applicationContext, alvo.id)
                                    todosLembretes = todosLembretes.filter { it.id != alvo.id }
                                },
                                lembretesBase = lembretesDoCarroAtual
                            )
                        }
                    }
                    Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ConfiguracoesScreen(onDismiss: () -> Unit, onTestarNotificacao: () -> Unit) {
    var notificacoesAtivas by rememberSaveable { mutableStateOf(true) }
    var alertasPorKm by rememberSaveable { mutableStateOf(true) }
    var resumoSemanal by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Configurações", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Personalize o jeito de cuidar da frota", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.15f))

                Text("Notificações", color = Color.White, fontWeight = FontWeight.SemiBold)
                ConfigToggleItem(
                    title = "Alertas automáticos",
                    description = "Receba lembretes nos horários definidos",
                    checked = notificacoesAtivas,
                    onCheckedChange = { notificacoesAtivas = it }
                )
                ConfigToggleItem(
                    title = "Alertas por KM",
                    description = "Utilize o odômetro como gatilho adicional",
                    checked = alertasPorKm,
                    onCheckedChange = { alertasPorKm = it }
                )
                ConfigToggleItem(
                    title = "Resumo semanal",
                    description = "Receba um boletim dos principais avisos",
                    checked = resumoSemanal,
                    onCheckedChange = { resumoSemanal = it }
                )

                Button(
                    onClick = onTestarNotificacao,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Testar alerta", fontWeight = FontWeight.Bold)
                }

                Text(
                    "Versão do app 1.0.0 | dados salvos localmente",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ConfigToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111C2E))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF3B82F6),
                checkedTrackColor = Color(0xFF3B82F6)
            )
        )
    }
}

@Composable
fun MonitorIcon(tipo: TipoManutencao, cor: Color, quantidade: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Box Pai invisível: Não corta o conteúdo
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {

            // 1. Ícone com fundo recortado em Círculo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(2.dp, cor.copy(alpha = 0.5f), CircleShape)
            ) {
                // Se for FREIO, mostra texto "ABS". Senão, mostra o ícone.
                if (tipo == TipoManutencao.FREIO) {
                    Text(
                        text = "ABS",
                        color = cor,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = tipo.getIcon(),
                        contentDescription = tipo.label,
                        tint = cor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. Bolinha Vermelha Flutuando (Badge)
            if (quantidade > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(22.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(2.dp, Color(0xFF0F172A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quantidade.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(text = tipo.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}

@Composable
fun LembreteCard(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    modeloCarro: String,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    statusLabel: String,
    statusColor: Color
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            title = { Text("Excluir?", color = Color.White) },
            text = { Text("Apagar '${lembrete.titulo}' permanentemente?", color = Color(0xFF94A3B8)) },
            containerColor = Color(0xFF1E293B),
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Excluir", color = Color(0xFFEF4444)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(statusColor)
                )

                Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lembrete.titulo, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CalendarMonth, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(lembrete.dataLimite, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Spacer(Modifier.width(12.dp))
                                if (lembrete.kmLimite.isNotBlank()) {
                                    Icon(Icons.Rounded.Speed, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${lembrete.kmLimite} Km", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        }
                        if (lembrete.valor > 0) {
                            Surface(color = Color(0xFF064E3B), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    formatarMoeda(lembrete.valor),
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (statusLabel.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(statusLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (contato != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { abrirWhatsApp(context, contato.telefone, "Olá ${contato.nome}, preciso de *${lembrete.titulo}* para o *$modeloCarro*.") },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Agendar c/ ${contato.nome.split(" ")[0]}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
            }
        }
    }
}

/* ----------------- DIALOGS & OUTROS ----------------- */

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

@Composable
fun CalendarLembretesView(
    mesAtual: YearMonth,
    onMesAnterior: () -> Unit,
    onProximoMes: () -> Unit,
    lembretesPorData: Map<LocalDate, List<Lembrete>>,
    lembretesBase: List<Lembrete>,
    diaSelecionado: LocalDate?,
    onSelecionarDia: (LocalDate) -> Unit,
    onAbrirLembrete: (Lembrete) -> Unit,
    modeloCarro: String,
    contatoProvider: (Lembrete) -> ContatoProfissional?,
    onExcluir: (Lembrete) -> Unit
) {
    val localeBR = remember { Locale("pt", "BR") }
    val formatterMes = remember { DateTimeFormatter.ofPattern("MMMM yyyy", localeBR) }
    val semanaLabels = listOf("D", "S", "T", "Q", "Q", "S", "S")
    val primeiroDiaMes = mesAtual.atDay(1)
    val inicioGrade = primeiroDiaMes.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val diasGrade = (0 until 42).map { inicioGrade.plusDays(it.toLong()) }

    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111C2E)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMesAnterior) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Mês anterior", tint = Color.White) }
            Text(
                formatterMes.format(mesAtual.atDay(1)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeBR) else it.toString() },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onProximoMes) { Icon(Icons.Default.ArrowForwardIos, contentDescription = "Próximo mês", tint = Color.White) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            semanaLabels.forEach { label ->
                Text(label, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
        diasGrade.chunked(7).forEach { semana ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                semana.forEach { dia ->
                    val pertenceAoMes = YearMonth.from(dia) == mesAtual
                    val temAviso = lembretesPorData.containsKey(dia)
                    val selecionado = diaSelecionado == dia
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    selecionado -> Color(0xFF3B82F6)
                                    pertenceAoMes -> Color(0xFF1E293B)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (temAviso && !selecionado) 1.dp else 0.dp,
                                color = if (temAviso) Color(0xFF3B82F6) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = pertenceAoMes) { onSelecionarDia(dia) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(
                                text = dia.dayOfMonth.toString(),
                                color = when {
                                    selecionado -> Color.White
                                    pertenceAoMes -> Color.White
                                    else -> Color(0xFF475569)
                                },
                                fontWeight = FontWeight.Bold
                            )
                            if (temAviso) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (selecionado) Color.White else Color(0xFF3B82F6))
                                )
                            }
                        }
                    }
                }
            }
        }
        val selecionados = diaSelecionado?.let { lembretesPorData[it] } ?: emptyList()
        if (selecionados.isEmpty()) {
            Text("Selecione um dia com avisos para ver os detalhes.", color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 8.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                selecionados.forEach { lembrete ->
                    LembreteCard(
                        lembrete = lembrete,
                        contato = contatoProvider(lembrete),
                        modeloCarro = modeloCarro,
                        onDelete = { onExcluir(lembrete) },
                        onClick = { onAbrirLembrete(lembrete) },
                        statusLabel = textoStatusPrazo(lembrete),
                        statusColor = calcularCorStatus(lembretesBase, lembrete.tipo)
                    )
                }
            }
        }
        }
    }
}

@Composable
fun LembreteDetalhesDialog(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    carro: CarroInfo,
    onDismiss: () -> Unit,
    onSalvar: (Lembrete) -> Unit
) {
    val context = LocalContext.current
    var isEditando by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf(lembrete.titulo) }
    var dataAviso by remember { mutableStateOf(lembrete.dataLimite) }
    var horaAviso by remember { mutableStateOf(lembrete.horaAviso) }
    var kmLimite by remember { mutableStateOf(lembrete.kmLimite) }
    var valorTexto by remember { mutableStateOf(if (lembrete.valor > 0) lembrete.valor.toString() else "") }

    LaunchedEffect(lembrete) {
        titulo = lembrete.titulo
        dataAviso = lembrete.dataLimite
        horaAviso = lembrete.horaAviso
        kmLimite = lembrete.kmLimite
        valorTexto = if (lembrete.valor > 0) lembrete.valor.toString() else ""
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
                            Icon(lembrete.tipo.getIcon(), contentDescription = null, tint = Color(0xFF3B82F6))
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
                    } else {
                        val infoItems = buildList {
                            add("Veículo" to carro.nome)
                            add("Data do aviso" to lembrete.dataLimite.ifBlank { "Sem data" })
                            add("Hora do aviso" to lembrete.horaAviso)
                            add("KM limite" to lembrete.kmLimite.ifBlank { "Não definido" })
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
                                    val atualizado = lembrete.copy(
                                        titulo = titulo.ifBlank { lembrete.titulo },
                                        dataLimite = dataAviso.ifBlank { lembrete.dataLimite },
                                        horaAviso = horaAviso.ifBlank { lembrete.horaAviso },
                                        kmLimite = kmLimite,
                                        valor = novoValor
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
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = dialogActionButtonShape
                        ) { Text("Fechar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
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
    onAutoCameraConsumida: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var descricao by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var kmBase by remember { mutableStateOf(if (carroAtual.kmAtual > 0) carroAtual.kmAtual.toString() else "") }
    var valorInput by remember { mutableStateOf("") }
    var tipoSelecionado by remember { mutableStateOf(TipoManutencao.OLEO) }
    var contatoSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    var listaItensDetectados by remember { mutableStateOf<List<ItemDetectado>>(emptyList()) }
    var isModoLista by remember { mutableStateOf(false) }
    var showKmConfirmDialog by remember { mutableStateOf(false) }
    var kmDetectadoParaConfirmar by remember { mutableStateOf(0) }
    var menuContatosExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var fotoCaminho by remember { mutableStateOf<String?>(null) }
    var horaNotificacao by remember { mutableStateOf("09:00") }
    var dataAviso by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var avisoPersonalizado by remember { mutableStateOf(false) }
    val descricaoFocusRequester = remember { FocusRequester() }
    var textosDetectados by remember { mutableStateOf<List<String>>(emptyList()) }
    val dataFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    LaunchedEffect(autoAbrirCamera) {
        if (autoAbrirCamera) {
            showCamera = true
            onAutoCameraConsumida()
        }
    }

    LaunchedEffect(isModoLista) {
        if (!isModoLista) {
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

    if (showCamera) {
        CameraCapturaDialog(onDismiss = { showCamera = false }, onFotoCapturada = { resultado ->
            fotoCaminho = resultado.arquivoFoto.absolutePath
            textosDetectados = resultado.linhasReconhecidas.filter { it.isNotBlank() && !isTextoPromocional(it) && !padraoUrlOuContato.containsMatchIn(it.uppercase(Locale.ROOT)) }
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
            if (resultado.kmDetectado != null && resultado.kmDetectado > 0) { kmDetectadoParaConfirmar = resultado.kmDetectado; showKmConfirmDialog = true }
            showCamera = false
        })
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp)
            .border(dialogBorderStroke, dialogCornerShape),
        shape = dialogCornerShape,
        title = { Text(if(isModoLista) "Itens Detectados" else "Novo Aviso", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Botão Camera
                Button(
                    onClick = { showCamera = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(fotoCaminho != null) Color(0xFF10B981) else Color(0xFF3B82F6))
                ) {
                    Icon(if(fotoCaminho != null) Icons.Default.Check else Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp))
                    Text(if(fotoCaminho != null) "Foto Anexada (Refazer)" else "Escanear Produto")
                }

                if (isModoLista) {
                    // Lista de Itens Detectados
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(listaItensDetectados) { item ->
                            val kmAtualBase = kmBase.toIntOrNull() ?: 0; val kmFuturoCalculado = if(kmAtualBase > 0) (kmAtualBase + getKmAdicionalPorTipo(item.tipo)).toString() else ""
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFF0F172A), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(item.tipo.getIcon(), null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row { Text(item.tipo.label, color = Color(0xFF94A3B8), fontSize = 12.sp); if (kmFuturoCalculado.isNotEmpty()) Text("  - Vence +${getKmAdicionalPorTipo(item.tipo)}km", color = Color(0xFF10B981), fontSize = 12.sp) }
                                }
                                IconButton(onClick = { listaItensDetectados = listaItensDetectados - item; if(listaItensDetectados.isEmpty()) isModoLista = false }) { Icon(Icons.Default.Delete, "Remover", tint = Color(0xFFEF4444)) }
                            }
                        }
                    }
                } else {
                    // Campos Manuais
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = descricao,
                            onValueChange = { descricao = it },
                            label = { Text("O que foi feito?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(descricaoFocusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(SugestaoRapida("Troca de óleo", TipoManutencao.OLEO), SugestaoRapida("Revisão", TipoManutencao.MECANICA), SugestaoRapida("Pneu", TipoManutencao.MECANICA)).forEach { s -> AssistChip(onClick = { descricao = s.texto; tipoSelecionado = s.tipo }, label = { Text(s.texto) }) } }
                        ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = !menuExpanded }) { OutlinedTextField(value = tipoSelecionado.label, onValueChange = {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) }, shape = RoundedCornerShape(12.dp)); ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { TipoManutencao.values().forEach { t -> DropdownMenuItem(text = { Text(t.label) }, onClick = { tipoSelecionado = t; menuExpanded = false }) } } }
                    }
                }

                if (textosDetectados.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("Textos capturados", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        textosDetectados.take(6).forEach { texto ->
                            Text("• $texto", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }

                Divider(color = Color(0xFF334155))

                // Configurações Gerais (KM, Data, Profissional)
                Text("Detalhes do Registro", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(expanded = menuContatosExpanded, onExpandedChange = { menuContatosExpanded = !menuContatosExpanded }) {
                    OutlinedTextField(value = contatoSelecionado?.nome ?: "Sem vínculo", onValueChange = {}, readOnly = true, label = { Text("Profissional Responsável") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuContatosExpanded) }, shape = RoundedCornerShape(12.dp)); ExposedDropdownMenu(expanded = menuContatosExpanded, onDismissRequest = { menuContatosExpanded = false }) { DropdownMenuItem(text = { Text("Nenhum") }, onClick = { contatoSelecionado = null; menuContatosExpanded = false }); contatosDisponiveis.forEach { c -> DropdownMenuItem(text = { Text("${c.nome} (${c.tipoServico})") }, onClick = { contatoSelecionado = c; menuContatosExpanded = false }) } }
                }

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
                    OutlinedTextField(
                        value = data,
                        onValueChange = {},
                        label = { Text("Data do serviço") },
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

                if (!isModoLista) {
                    OutlinedTextField(value = valorInput, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) valorInput = it }, label = { Text("Valor Total (R$)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                }

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kmAtualBase = kmBase.toIntOrNull() ?: 0
                    if (kmAtualBase > carroAtual.kmAtual) onUpdateKmCarro(kmAtualBase)
                    val dataBaseParsed = try { LocalDate.parse(data, DateTimeFormatter.ofPattern("dd/MM/yyyy")) } catch (e: Exception) { LocalDate.now() }

                    val dataAvisoStr = dataAviso
                    if (isModoLista) {
                        val novosLembretes = listaItensDetectados.flatMap { item ->
                            val rep = maxOf(1, item.quantidade)
                            val kmFuturo = (kmAtualBase + getKmAdicionalPorTipo(item.tipo)).toString()
                            (1..rep).map { indice ->
                                val tituloFormatado = if (rep > 1) "${item.nome} (${indice}/$rep)" else item.nome
                                Lembrete(
                                    titulo = tituloFormatado,
                                    dataLimite = dataAvisoStr,
                                    kmLimite = kmFuturo,
                                    tipo = item.tipo,
                                    valor = item.valor,
                                    carroId = "",
                                    contatoId = contatoSelecionado?.id,
                                    fotoPath = fotoCaminho,
                                    horaAviso = horaNotificacao
                                )
                            }
                        }
                        novosLembretes.forEach { NotificacaoHelper.agendarNotificacao(appContext, it, horaNotificacao) }
                        onMultiConfirm(novosLembretes)
                    } else {
                        if (descricao.isNotBlank()) {
                            val novoLembrete = Lembrete(
                                titulo = descricao,
                                dataLimite = dataAvisoStr,
                                kmLimite = (kmAtualBase + getKmAdicionalPorTipo(tipoSelecionado)).toString(),
                                tipo = tipoSelecionado,
                                valor = valorInput.toDoubleOrNull() ?: 0.0,
                                carroId = "",
                                contatoId = contatoSelecionado?.id,
                                fotoPath = fotoCaminho,
                                horaAviso = horaNotificacao
                            )
                            NotificacaoHelper.agendarNotificacao(appContext, novoLembrete, horaNotificacao)
                            onConfirm(novoLembrete)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Salvar Registro", fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar", color = Color(0xFF94A3B8)) } }
    )
}

@Composable
fun EditarCarroDialog(carroAtual: CarroInfo, titulo: String, onDismiss: () -> Unit, onSalvar: (CarroInfo) -> Unit) {
    var nome by remember { mutableStateOf(carroAtual.nome) }
    var marca by remember { mutableStateOf(carroAtual.marca) }
    var modelo by remember { mutableStateOf(carroAtual.modelo) }
    var kmAtualStr by remember { mutableStateOf(if (carroAtual.kmAtual > 0) carroAtual.kmAtual.toString() else "") }
    var corSelecionada by remember { mutableIntStateOf(carroAtual.corArgb) }
    val coresDisponiveis = listOf(0xFF3B82F6.toInt(), 0xFFEF4444.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFF94A3B8.toInt(), 0xFFFFFFFF.toInt())
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
                    label = { Text("Apelido") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo e Motor") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = kmAtualStr,
                    onValueChange = { if (it.all(Char::isDigit)) kmAtualStr = it },
                    label = { Text("KM Atual (Painel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text("Cor:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    coresDisponiveis.forEach { corInt ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(corInt))
                                .border(
                                    width = if (corSelecionada == corInt) 2.dp else 0.dp,
                                    color = if (corSelecionada == corInt) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { corSelecionada = corInt }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSalvar(carroAtual.copy(nome = nome, marca = marca, modelo = modelo, kmAtual = kmAtualStr.toIntOrNull() ?: 0, corArgb = corSelecionada)) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelatorioVeiculoScreen(carroAtual: CarroInfo, lembretes: List<Lembrete>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resumo = remember(carroAtual, lembretes) { gerarResumoRelatorio(carroAtual, lembretes) }
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
            color = Color(0xFF030B18)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F202F), Color(0xFF031222)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                ).padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0x441E293B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Relatório do veículo",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val uri = gerarPdfRelatorio(context, carroAtual, lembretes)
                            if (uri != null) {
                                compartilharPdf(context, uri)
                            } else {
                                Toast.makeText(context, "Não foi possível gerar o PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF312E81))))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(carroAtual.nome, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                            val infoModelo = listOf(carroAtual.marca, carroAtual.modelo).filter { it.isNotBlank() }.joinToString(" - ")
                            if (infoModelo.isNotBlank()) {
                                Text(infoModelo, color = Color(0xFFBFDBFE), fontSize = 14.sp)
                            }
                            Text(
                                text = if (carroAtual.kmAtual > 0) "Odômetro atual: ${carroAtual.kmAtual} km" else "Odômetro não informado",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
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
                                                Icon(tipo.getIcon(), contentDescription = tipo.label, tint = calcularCorStatus(lembretes, tipo), modifier = Modifier.size(26.dp))
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
                            "Crítica" -> Color(0xFFEF4444)
                            "Em atenção" -> Color(0xFFEAB308)
                            else -> Color.White
                        }
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Reputação do veículo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = corReputacao)
                                Text(tituloReputacao, color = corReputacao, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(descricaoReputacao, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                    }
                    if (proximos.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Próximas manutenções", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            Text("Sem manutenções pendentes", color = Color(0xFF94A3B8))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("Fechar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewManutencao() { CarLembreteTheme { ManutencaoScreen() } }

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

private fun isTextoPromocional(texto: String): Boolean {
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
