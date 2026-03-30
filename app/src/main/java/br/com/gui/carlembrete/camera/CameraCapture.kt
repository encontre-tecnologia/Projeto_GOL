package br.com.gui.carlembrete

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp // Importação corrigida
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.time.LocalDate
import java.util.Base64
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val LER_NOTAS_HABILITADO = false
private const val CAMERA_QR_TAG = "ZelluQrParser"

/* ----------------- CÂMERA INTELIGENTE ----------------- */

@Composable
fun CameraCapturaDialog(onDismiss: () -> Unit, onFotoCapturada: (ResultadoCaptura) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val uiExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    val liveScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isConsultandoQr by remember { mutableStateOf(false) }
    var progressoEscaneamento by remember { mutableStateOf(0f) }
    var mostrarFalhaLeitura by remember { mutableStateOf(false) }
    var mensagemFalhaLeitura by remember { mutableStateOf("Nota não encontrada") }
    var qrDetectadoAoVivo by remember { mutableStateOf(false) }
    var ambienteComPoucaLuz by remember { mutableStateOf(false) }
    var qrDeteccoesConsecutivas by remember { mutableStateOf(0) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraControlRef by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var previewAtivo by remember { mutableStateOf(false) }
    var rebindToken by remember { mutableIntStateOf(0) }
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
    DisposableEffect(liveScanner, analyzerExecutor) {
        onDispose {
            runCatching { liveScanner.close() }
            analyzerExecutor.shutdown()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    rebindToken += 1
                }
                Lifecycle.Event.ON_PAUSE -> {
                    previewAtivo = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (!hasCameraPermission) return

    fun bindCamera(previewView: PreviewView) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                val lumaAtual = imageProxy.averageLuma()
                uiExecutor.execute {
                    ambienteComPoucaLuz = lumaAtual < 70.0
                }
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                liveScanner.process(inputImage)
                    .addOnSuccessListener(uiExecutor) { barcodes ->
                        val qrEncontrado = barcodes.any { barcode ->
                            barcode.format == Barcode.FORMAT_QR_CODE && !barcode.toUsableQrText().isNullOrBlank()
                        }
                        if (qrEncontrado) {
                            qrDeteccoesConsecutivas += 1
                            qrDetectadoAoVivo = true
                        } else {
                            if (!isProcessing && !isConsultandoQr) {
                                qrDeteccoesConsecutivas = 0
                                qrDetectadoAoVivo = false
                            }
                        }
                    }
                    .addOnFailureListener(uiExecutor) {
                        if (!isProcessing && !isConsultandoQr) {
                            qrDetectadoAoVivo = false
                            qrDeteccoesConsecutivas = 0
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
                cameraControlRef = camera.cameraControl
                previewAtivo = true
            } catch (e: Exception) {
                cameraControlRef = null
                previewAtivo = false
                Log.e("Camera", "Erro", e)
            }
        }, uiExecutor)
    }

    fun finalizarCaptura(resultado: ResultadoCaptura) {
        val qrUrl = resultado.qrCodeUrl?.trim()
        if (qrUrl.isNullOrBlank()) {
            isProcessing = false
            isConsultandoQr = false
            progressoEscaneamento = 0f
            qrDetectadoAoVivo = false
            qrDeteccoesConsecutivas = 0
            mensagemFalhaLeitura = "Nota não encontrada"
            mostrarFalhaLeitura = true
            onFotoCapturada(resultado)
            return
        }

        if (qrEhTipoNaoSuportado(qrUrl)) {
            isProcessing = false
            isConsultandoQr = false
            progressoEscaneamento = 0f
            qrDetectadoAoVivo = false
            qrDeteccoesConsecutivas = 0
            mensagemFalhaLeitura = "Use uma NFC-e de compra com QR"
            mostrarFalhaLeitura = true
            return
        }

        isConsultandoQr = true
        mostrarFalhaLeitura = false
        mensagemFalhaLeitura = "Nota não encontrada"
        progressoEscaneamento = 0.94f
        scope.launch {
            val notaInfo = consultarNotaPorQrCode(qrUrl)
            isConsultandoQr = false
            isProcessing = false
            progressoEscaneamento = 1f
            qrDetectadoAoVivo = false
            qrDeteccoesConsecutivas = 0
            mensagemFalhaLeitura = "Nota não encontrada"
            mostrarFalhaLeitura = notaInfo == null
            onFotoCapturada(resultado.copy(notaQrInfo = notaInfo))
        }
    }

    fun iniciarCapturaComTentativas() {
        val capturaAtual = imageCapture ?: return
        if (isProcessing || isConsultandoQr) return

        val maxTentativas = 4
        isProcessing = true
        mostrarFalhaLeitura = false
        mensagemFalhaLeitura = "Nota não encontrada"
        progressoEscaneamento = 0.05f

        fun tentarCaptura(tentativa: Int) {
            val executarCaptura = {
                captureAndExtractItems(
                    context = context,
                    imageCapture = capturaAtual,
                    onProgress = { progressoEtapa ->
                        val progressoBase = (tentativa - 1).toFloat() / maxTentativas.toFloat()
                        progressoEscaneamento =
                            (progressoBase + (progressoEtapa / maxTentativas.toFloat())).coerceIn(0f, 1f)
                    }
                ) { resultado ->
                    if (resultadoTemLeituraUtil(resultado)) {
                        finalizarCaptura(resultado)
                        return@captureAndExtractItems
                    }

                    if (tentativa < maxTentativas) {
                        Log.w(CAMERA_QR_TAG, "Captura sem resultado util. Tentando novamente ($tentativa/$maxTentativas).")
                        tentarCaptura(tentativa + 1)
                    } else {
                        Log.w(CAMERA_QR_TAG, "Captura encerrada sem leitura util apos $maxTentativas tentativas.")
                        isProcessing = false
                        isConsultandoQr = false
                        progressoEscaneamento = 0f
                        qrDetectadoAoVivo = false
                        qrDeteccoesConsecutivas = 0
                        mensagemFalhaLeitura = "Nota não encontrada"
                        mostrarFalhaLeitura = true
                    }
                }
            }

            val preview = previewViewRef
            val cameraControl = cameraControlRef
            if (preview != null && cameraControl != null && previewAtivo) {
                runCatching {
                    val point = preview.meteringPointFactory.createPoint(
                        preview.width / 2f,
                        preview.height / 2f
                    )
                    val action = FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                        .build()
                    val focusFuture = cameraControl.startFocusAndMetering(action)
                    focusFuture.addListener(
                        { executarCaptura() },
                        uiExecutor
                    )
                }.onFailure {
                    executarCaptura()
                }
            } else {
                executarCaptura()
            }
        }

        tentarCaptura(1)
    }

    LaunchedEffect(previewViewRef, rebindToken) {
        previewViewRef?.let { previewView ->
            if (rebindToken > 0) {
                bindCamera(previewView)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // 1. Layer da Câmera (Preview)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                        bindCamera(this)
                    }
                },
                update = { previewView ->
                    previewViewRef = previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Layer da Máscara Escura e Borda Branca
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val larguraCorteDp = maxWidth * 0.85f
                val alturaCorteDp = 280.dp
                val cornerRadiusDp = 24.dp
                val overlayColor = Color.Black.copy(alpha = 0.75f)

                // Canvas desenha a máscara escura com o buraco no meio
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Convertendo DP para Pixels dentro do escopo de desenho (DrawScope)
                    val larguraPx = larguraCorteDp.toPx()
                    val alturaPx = alturaCorteDp.toPx()
                    val cornerPx = cornerRadiusDp.toPx()

                    // Calculando coordenadas para centralizar
                    val left = (size.width - larguraPx) / 2
                    val top = (size.height - alturaPx) / 2

                    // Criando o caminho do retângulo arredondado (o "buraco")
                    val rectPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    offset = Offset(left, top),
                                    size = Size(larguraPx, alturaPx)
                                ),
                                cornerRadius = CornerRadius(cornerPx)
                            )
                        )
                    }

                    // Cortando o buraco da camada escura
                    // ClipOp.Difference garante que pintamos TUDO, MENOS o retângulo
                    clipPath(rectPath, clipOp = ClipOp.Difference) {
                        drawRect(color = overlayColor)
                    }

                    if (!previewAtivo) {
                        drawRoundRect(
                            color = Color.Black,
                            topLeft = Offset(left, top),
                            size = Size(larguraPx, alturaPx),
                            cornerRadius = CornerRadius(cornerPx)
                        )
                    }
                }

                // O QUADRADO BRANCO CENTRAL (Apenas a borda visual e animação)
                Column(
                    modifier = Modifier
                        .width(larguraCorteDp)
                        .align(Alignment.Center)
                        .offset(y = (-252).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    if (!isProcessing && !isConsultandoQr) {
                        Text(
                            text = "Posicione o QR da NFC-e dentro da area e aproxime a camera",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isProcessing || isConsultandoQr) {
                    Column(
                        modifier = Modifier
                            .width(larguraCorteDp)
                            .align(Alignment.Center)
                            .offset(y = (-170).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${(progressoEscaneamento.coerceIn(0f, 1f) * 100).roundToInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { progressoEscaneamento.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = Color(0xFF22C55E),
                            trackColor = Color.White.copy(alpha = 0.18f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(larguraCorteDp)
                        .height(alturaCorteDp)
                        .align(Alignment.Center)
                        .border(
                            BorderStroke(
                                3.dp,
                                when {
                                    mostrarFalhaLeitura && !isProcessing && !isConsultandoQr -> Color(0xFFEF9A9A)
                                    isProcessing || isConsultandoQr -> Color(0xFF22C55E)
                                    else -> Color.White
                                }
                            ),
                            RoundedCornerShape(cornerRadiusDp)
                        )
                        .clip(RoundedCornerShape(cornerRadiusDp))
                ) {
                    if (isProcessing || isConsultandoQr || mostrarFalhaLeitura) {
                        Text(
                            text = if (mostrarFalhaLeitura && !isProcessing && !isConsultandoQr) {
                                mensagemFalhaLeitura
                            } else {
                                "Escaneando NFC-e..."
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    if (isProcessing || isConsultandoQr) {
                        // Animação do Scanner (Barra reta + cortina verde)
                        val lineHeight = 5.dp
                        val curtainMaxHeight = 124.dp
                        val scannerTransition = rememberInfiniteTransition(label = "scanner_line")
                        val scannerProgress by scannerTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scanner_line_progress"
                        )
                        val barraOffset = (alturaCorteDp - lineHeight) * scannerProgress
                        val curtainAnchorTop = (barraOffset - (curtainMaxHeight / 2))
                            .coerceAtLeast(0.dp)
                            .coerceAtMost((alturaCorteDp - curtainMaxHeight).coerceAtLeast(0.dp))
                        val curtainHeight = curtainMaxHeight.coerceAtMost(alturaCorteDp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(curtainHeight)
                                .align(Alignment.TopCenter)
                                .offset(y = curtainAnchorTop)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF22C55E).copy(alpha = 0.00f),
                                            Color(0xFF22C55E).copy(alpha = 0.18f),
                                            Color(0xFF22C55E).copy(alpha = 0.32f),
                                            Color(0xFF22C55E).copy(alpha = 0.18f),
                                            Color(0xFF22C55E).copy(alpha = 0.00f)
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(lineHeight)
                                .align(Alignment.TopCenter)
                                .offset(y = barraOffset)
                                .background(
                                    color = Color(0xFF22C55E)
                                )
                                .shadow(8.dp, spotColor = Color(0xFF22C55E))
                        )
                    }
                }
            }

            // CONTROLES E BOTÕES
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topo: Botões de Fechar e Flash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 24.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(Icons.Default.Close, "Fechar", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { iniciarCapturaComTentativas() },
                        enabled = !isProcessing && !isConsultandoQr,
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                if (isProcessing || isConsultandoQr) Color.White.copy(alpha = 0.45f) else Color.White,
                                CircleShape
                            )
                            .border(
                                4.dp,
                                if (isProcessing || isConsultandoQr) Color(0xFFE2E8F0).copy(alpha = 0.45f) else Color(0xFFE2E8F0),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Escanear agora",
                            tint = if (isProcessing || isConsultandoQr) Color.Black.copy(alpha = 0.45f) else Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun captureAndExtractItems(
    context: Context,
    imageCapture: ImageCapture,
    onProgress: (Float) -> Unit = {},
    onResult: (ResultadoCaptura) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    val mainExecutor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            mainExecutor.execute { onProgress(0.08f) }
            val bitmapBuffer = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees.toFloat()
            val matrix = Matrix().apply { postRotate(rotation) }
            val bitmapRotacionado = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
            val bitmapFocado = recortarAreaCentral(bitmapRotacionado)
            val bitmapQrAprimorado = prepararBitmapParaLeituraQr(bitmapRotacionado)
            val arquivo = File(context.filesDir, "servico_scan_${System.currentTimeMillis()}.jpg")
            // Salva a imagem completa para permitir OCR de documento (CRLV etc.).
            FileOutputStream(arquivo).use { out -> bitmapRotacionado.compress(Bitmap.CompressFormat.JPEG, 85, out) }

            // Para QR Code, analisamos a imagem inteira (não o recorte central),
            // pois o código pode estar fora da área focada.
            val inputImage = InputImage.fromBitmap(bitmapRotacionado, 0)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            val inputImageSemRotacao = InputImage.fromBitmap(bitmapBuffer, 0)
            val inputImageFocado = InputImage.fromBitmap(bitmapFocado, 0)
            val inputImageAprimorado = InputImage.fromBitmap(bitmapQrAprimorado, 0)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    mainExecutor.execute { onProgress(0.38f) }
                    val qrUrl = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.toUsableQrText()
                    Log.i(CAMERA_QR_TAG, "Passo1 - barcodes=${barcodes.size} qrUrl=$qrUrl")
                    if (!qrUrl.isNullOrBlank()) {
                        mainExecutor.execute {
                            onProgress(1f)
                            onResult(
                                ResultadoCaptura(
                                    arquivoFoto = arquivo,
                                    itensEncontrados = emptyList(),
                                    kmDetectado = null,
                                    qrCodeUrl = qrUrl,
                                    sugestoesProduto = emptyList(),
                                    linhasReconhecidas = emptyList()
                                )
                            )
                            image.close()
                        }
                        scanner.close()
                        return@addOnSuccessListener
                    }

                    scanner.process(inputImageSemRotacao)
                        .addOnSuccessListener { barcodesSemRotacao ->
                            mainExecutor.execute { onProgress(0.68f) }
                            val qrUrlSemRotacao = barcodesSemRotacao.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.toUsableQrText()
                            Log.i(
                                CAMERA_QR_TAG,
                                "Passo2 (sem rotacao) - barcodes=${barcodesSemRotacao.size} qrUrl=$qrUrlSemRotacao"
                            )
                            if (!qrUrlSemRotacao.isNullOrBlank()) {
                                mainExecutor.execute {
                                    onProgress(1f)
                                    onResult(
                                        ResultadoCaptura(
                                            arquivoFoto = arquivo,
                                            itensEncontrados = emptyList(),
                                            kmDetectado = null,
                                            qrCodeUrl = qrUrlSemRotacao,
                                            sugestoesProduto = emptyList(),
                                            linhasReconhecidas = emptyList()
                                        )
                                    )
                                    image.close()
                                }
                                scanner.close()
                                return@addOnSuccessListener
                            }

                            scanner.process(inputImageFocado)
                                .addOnSuccessListener { barcodesFoco ->
                                    mainExecutor.execute { onProgress(0.86f) }
                                    val qrUrlFoco = barcodesFoco.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.toUsableQrText()
                                    Log.i(CAMERA_QR_TAG, "Passo3 (foco) - barcodes=${barcodesFoco.size} qrUrl=$qrUrlFoco")
                                    if (!qrUrlFoco.isNullOrBlank()) {
                                        mainExecutor.execute {
                                            onProgress(1f)
                                            onResult(
                                                ResultadoCaptura(
                                                    arquivoFoto = arquivo,
                                                    itensEncontrados = emptyList(),
                                                    kmDetectado = null,
                                                    qrCodeUrl = qrUrlFoco,
                                                    sugestoesProduto = emptyList(),
                                                    linhasReconhecidas = emptyList()
                                                )
                                            )
                                            image.close()
                                        }
                                        scanner.close()
                                        return@addOnSuccessListener
                                    }

                                    scanner.process(inputImageAprimorado)
                                        .addOnSuccessListener { barcodesAprimorados ->
                                            mainExecutor.execute { onProgress(0.92f) }
                                            val qrUrlAprimorado = barcodesAprimorados
                                                .firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                                ?.toUsableQrText()
                                            Log.i(
                                                CAMERA_QR_TAG,
                                                "Passo4 (alto contraste) - barcodes=${barcodesAprimorados.size} qrUrl=$qrUrlAprimorado"
                                            )
                                            if (!qrUrlAprimorado.isNullOrBlank()) {
                                                mainExecutor.execute {
                                                    onProgress(1f)
                                                    onResult(
                                                        ResultadoCaptura(
                                                            arquivoFoto = arquivo,
                                                            itensEncontrados = emptyList(),
                                                            kmDetectado = null,
                                                            qrCodeUrl = qrUrlAprimorado,
                                                            sugestoesProduto = emptyList(),
                                                            linhasReconhecidas = emptyList()
                                                        )
                                                    )
                                                    image.close()
                                                }
                                                scanner.close()
                                                return@addOnSuccessListener
                                            }

                                            val inputImageAprimoradoRotPlus = InputImage.fromBitmap(
                                                rotacionarBitmapSeguro(bitmapQrAprimorado, 12f),
                                                0
                                            )
                                            scanner.process(inputImageAprimoradoRotPlus)
                                                .addOnSuccessListener { barcodesAprimoradosRotPlus ->
                                                    mainExecutor.execute { onProgress(0.96f) }
                                                    val qrUrlAprimoradoRotPlus = barcodesAprimoradosRotPlus
                                                        .firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                                        ?.toUsableQrText()
                                                    Log.i(
                                                        CAMERA_QR_TAG,
                                                        "Passo5 (alto contraste +12) - barcodes=${barcodesAprimoradosRotPlus.size} qrUrl=$qrUrlAprimoradoRotPlus"
                                                    )
                                                    if (!qrUrlAprimoradoRotPlus.isNullOrBlank()) {
                                                        mainExecutor.execute {
                                                            onProgress(1f)
                                                            onResult(
                                                                ResultadoCaptura(
                                                                    arquivoFoto = arquivo,
                                                                    itensEncontrados = emptyList(),
                                                                    kmDetectado = null,
                                                                    qrCodeUrl = qrUrlAprimoradoRotPlus,
                                                                    sugestoesProduto = emptyList(),
                                                                    linhasReconhecidas = emptyList()
                                                                )
                                                            )
                                                            image.close()
                                                        }
                                                        scanner.close()
                                                        return@addOnSuccessListener
                                                    }

                                                    val inputImageAprimoradoRotMinus = InputImage.fromBitmap(
                                                        rotacionarBitmapSeguro(bitmapQrAprimorado, -12f),
                                                        0
                                                    )
                                                    scanner.process(inputImageAprimoradoRotMinus)
                                                        .addOnSuccessListener { barcodesAprimoradosRotMinus ->
                                                            mainExecutor.execute { onProgress(1f) }
                                                            val qrUrlAprimoradoRotMinus = barcodesAprimoradosRotMinus
                                                                .firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                                                ?.toUsableQrText()
                                                            Log.i(
                                                                CAMERA_QR_TAG,
                                                                "Passo6 (alto contraste -12) - barcodes=${barcodesAprimoradosRotMinus.size} qrUrl=$qrUrlAprimoradoRotMinus"
                                                            )
                                                            mainExecutor.execute {
                                                                onResult(
                                                                    ResultadoCaptura(
                                                                        arquivoFoto = arquivo,
                                                                        itensEncontrados = emptyList(),
                                                                        kmDetectado = null,
                                                                        qrCodeUrl = qrUrlAprimoradoRotMinus,
                                                                        sugestoesProduto = emptyList(),
                                                                        linhasReconhecidas = emptyList()
                                                                    )
                                                                )
                                                                image.close()
                                                            }
                                                            scanner.close()
                                                        }
                                                        .addOnFailureListener { erroAprimoradoRotMinus ->
                                                            Log.e(CAMERA_QR_TAG, "Falha no passo6 QR", erroAprimoradoRotMinus)
                                                            mainExecutor.execute {
                                                                onProgress(1f)
                                                                onResult(
                                                                    ResultadoCaptura(
                                                                        arquivoFoto = arquivo,
                                                                        itensEncontrados = emptyList(),
                                                                        kmDetectado = null,
                                                                        qrCodeUrl = null,
                                                                        sugestoesProduto = emptyList(),
                                                                        linhasReconhecidas = emptyList()
                                                                    )
                                                                )
                                                                image.close()
                                                            }
                                                            scanner.close()
                                                        }
                                                }
                                                .addOnFailureListener { erroAprimoradoRotPlus ->
                                                    Log.e(CAMERA_QR_TAG, "Falha no passo5 QR", erroAprimoradoRotPlus)
                                                    mainExecutor.execute {
                                                        onProgress(1f)
                                                        onResult(
                                                            ResultadoCaptura(
                                                                arquivoFoto = arquivo,
                                                                itensEncontrados = emptyList(),
                                                                kmDetectado = null,
                                                                qrCodeUrl = null,
                                                                sugestoesProduto = emptyList(),
                                                                linhasReconhecidas = emptyList()
                                                            )
                                                        )
                                                        image.close()
                                                    }
                                                    scanner.close()
                                                }
                                        }
                                        .addOnFailureListener { erroAprimorado ->
                                            Log.e(CAMERA_QR_TAG, "Falha no passo4 QR", erroAprimorado)
                                            mainExecutor.execute {
                                                onProgress(1f)
                                                onResult(
                                                    ResultadoCaptura(
                                                        arquivoFoto = arquivo,
                                                        itensEncontrados = emptyList(),
                                                        kmDetectado = null,
                                                        qrCodeUrl = null,
                                                        sugestoesProduto = emptyList(),
                                                        linhasReconhecidas = emptyList()
                                                    )
                                                )
                                                image.close()
                                            }
                                            scanner.close()
                                        }
                                }
                                .addOnFailureListener { erroFoco ->
                                    Log.e(CAMERA_QR_TAG, "Falha no passo3 QR", erroFoco)
                                    mainExecutor.execute {
                                        onProgress(1f)
                                        onResult(
                                            ResultadoCaptura(
                                                arquivoFoto = arquivo,
                                                itensEncontrados = emptyList(),
                                                kmDetectado = null,
                                                qrCodeUrl = null,
                                                sugestoesProduto = emptyList(),
                                                linhasReconhecidas = emptyList()
                                            )
                                        )
                                        image.close()
                                    }
                                    scanner.close()
                                }
                        }
                        .addOnFailureListener { erroSemRotacao ->
                            Log.e(CAMERA_QR_TAG, "Falha no passo2 QR", erroSemRotacao)
                            mainExecutor.execute {
                                onProgress(1f)
                                onResult(
                                    ResultadoCaptura(
                                        arquivoFoto = arquivo,
                                        itensEncontrados = emptyList(),
                                        kmDetectado = null,
                                        qrCodeUrl = null,
                                        sugestoesProduto = emptyList(),
                                        linhasReconhecidas = emptyList()
                                    )
                                )
                                image.close()
                            }
                            scanner.close()
                        }
                }
                .addOnFailureListener { erro ->
                    Log.e(CAMERA_QR_TAG, "Falha no passo1 QR", erro)
                    mainExecutor.execute {
                        onProgress(1f)
                        onResult(
                            ResultadoCaptura(
                                arquivoFoto = arquivo,
                                itensEncontrados = emptyList(),
                                kmDetectado = null,
                                qrCodeUrl = null,
                                sugestoesProduto = emptyList(),
                                linhasReconhecidas = emptyList()
                            )
                        )
                        image.close()
                    }
                    scanner.close()
                }
                }
        override fun onError(exception: ImageCaptureException) {
            Log.e(CAMERA_QR_TAG, "Falha ao capturar imagem", exception)
            mainExecutor.execute {
                onProgress(1f)
                onResult(
                    ResultadoCaptura(
                        arquivoFoto = File(context.filesDir, "servico_scan_erro.jpg"),
                        itensEncontrados = emptyList(),
                        kmDetectado = null,
                        qrCodeUrl = null,
                        sugestoesProduto = emptyList(),
                        linhasReconhecidas = emptyList()
                    )
                )
            }
        }
    })
}

private fun resultadoTemLeituraUtil(resultado: ResultadoCaptura): Boolean {
    return !resultado.qrCodeUrl.isNullOrBlank() ||
        resultado.itensEncontrados.isNotEmpty() ||
        resultado.sugestoesProduto.isNotEmpty() ||
        resultado.linhasReconhecidas.isNotEmpty() ||
        (resultado.kmDetectado != null && resultado.kmDetectado > 0)
}

private fun qrEhTipoNaoSuportado(qrUrl: String): Boolean {
    val host = runCatching { android.net.Uri.parse(qrUrl).host.orEmpty().lowercase(Locale.ROOT) }
        .getOrDefault("")
    return host.contains("nfse.gov.br")
}

private fun ImageProxy.averageLuma(): Double {
    val buffer = planes.firstOrNull()?.buffer ?: return 255.0
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    if (data.isEmpty()) return 255.0
    val total = data.sumOf { it.toInt() and 0xFF }
    return total.toDouble() / data.size.toDouble()
}

private fun Barcode.toUsableQrText(): String? {
    if (format != Barcode.FORMAT_QR_CODE) return null

    val candidates = buildList {
        url?.url?.let(::sanitizeQrUrlText)?.takeIf { it.isNotBlank() }?.let { add(it) }
        rawValue?.let(::sanitizeQrUrlText)?.takeIf { it.isNotBlank() }?.let { add(it) }
        displayValue?.let(::sanitizeQrUrlText)?.takeIf { it.isNotBlank() }?.let { add(it) }
    }

    candidates.firstOrNull { it.looksLikeUsefulText() }?.let { return it }

    val bytes = rawBytes ?: return null
    val decodedCandidates = listOf(
        runCatching { String(bytes, Charsets.UTF_8) }.getOrNull(),
        runCatching { String(bytes, Charsets.ISO_8859_1) }.getOrNull(),
        runCatching { String(bytes, Charsets.UTF_16) }.getOrNull()
    ).mapNotNull { decoded ->
        decoded
            ?.let(::sanitizeQrUrlText)
            ?.takeIf { text -> text.isNotBlank() }
    }

    decodedCandidates.firstOrNull { it.looksLikeUsefulText() }?.let { return it }

    // Fallback para QR binário/assinado: preserva conteúdo sem gerar caracteres inválidos.
    return "B64:${Base64.getEncoder().encodeToString(bytes)}"
}

private fun String.looksLikeUsefulText(): Boolean {
    val sanitized = sanitizeQrUrlText(this)
    if (sanitized.isBlank()) return false
    if (sanitized.startsWith("http://", true) || sanitized.startsWith("https://", true)) return true
    val printable = sanitized.count { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }
    val ratio = printable.toFloat() / sanitized.length.coerceAtLeast(1)
    return ratio >= 0.85f
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
        listOf(
            "GASOLINA", "ETANOL", "DIESEL", "GNV", "COMBUSTIVEL", "ABASTECIMENTO",
            "ABAST", "POSTO", "LITRO", "LITROS"
        ).any { normalized.contains(it) } -> TipoManutencao.ABASTECIMENTO
        listOf(
            "OLEO", "LUBRAX", "MOBIL", "SHELL", "HELIX", "CASTROL", "PETRONAS",
            "LUBRIFICANTE", "LUB", "MOTOR OIL", "SAE", "0W", "5W", "10W", "15W",
            "20W", "25W", "ATF", "DEXRON", "HIDRAULICO", "FLUIDO MOTOR", "SEMISSINTETICO",
            "SINTETICO", "MINERAL"
        ).any { normalized.contains(it) } -> TipoManutencao.OLEO
        listOf(
            "BATERIA", "BATERIAS", "MOURA", "HELIAR", "ACDELCO", "ZETTA", "BOSCH BAT",
            "AMP", "AH", "12V", "24V", "VOLTS", "ARRANQUE", "START STOP", "ESTACIONARIA"
        ).any { normalized.contains(it) } -> TipoManutencao.BATERIA
        listOf(
            "FREIO", "PASTILHA", "PASTILHAS", "DISCO", "DISCOS", "LONA", "LONAS",
            "TAMBOR", "PINCA", "CILINDRO MESTRE", "SERVO FREIO", "FLUIDO DE FREIO",
            "ABS", "SAPATA"
        ).any { normalized.contains(it) } -> TipoManutencao.FREIO
        listOf(
            "PNEU", "PNEUS", "BORRACHA", "BORRACHARIA", "CAMARA DE AR", "CAMARA",
            "VALVULA", "RODA", "RODAS", "ALINHAMENTO", "BALANCEAMENTO", "BICO",
            "BICO DE PNEU", "REMENDO", "VULCANIZACAO"
        ).any { normalized.contains(it) } -> TipoManutencao.PNEU
        listOf(
            "CORRENTE", "KIT RELACAO", "RELACAO", "CATRACA", "CASSETE", "ENGRENAGEM",
            "PINHAO", "COG", "KMC", "COROA"
        ).any { normalized.contains(it) } -> TipoManutencao.CORRENTE
        listOf(
            "LUBRIFICACAO", "DESENGRIPANTE", "GRAXA", "GRAXA BRANCA", "SILICONE SPRAY",
            "LUB CHAIN", "CERA", "SELANTE", "LIMPA CONTATO", "LIMPA CORRENTE"
        ).any { normalized.contains(it) } -> TipoManutencao.LUBRIFICACAO
        listOf(
            "PEDIVELA", "MOVIMENTO CENTRAL", "BOTTOM BRACKET", "CRANK", "CRANKSET",
            "PEDAL", "PEDAIS", "EIXO CENTRAL"
        ).any { normalized.contains(it) } -> TipoManutencao.PEDIVELA
        listOf(
            "SELIM", "BANCO", "MANOPLA", "MANOPLAS", "GUIDAO", "MESA", "SUSPENSAO",
            "AMORTECEDOR", "AMORTECEDORES", "AMORTECEDOR DIANTEIRO", "COXIM", "MOLA",
            "ENCOSTO", "CAPA DE BANCO"
        ).any { normalized.contains(it) } -> TipoManutencao.CONFORTO
        listOf(
            "ACESSORIO", "ACESSORIOS", "SUPORTE", "BAGAGEIRO", "CESTO", "CESTINHA",
            "PARALAMA", "PARA-LAMA", "FAROL", "LANTERNA", "RETROVISOR", "CAPA",
            "ALARME", "SOM", "MULTIMIDIA", "CAMERA DE RE", "ENGATE", "CARREGADOR",
            "TRAVA", "CADEADO", "SUPORTE CELULAR"
        ).any { normalized.contains(it) } -> TipoManutencao.ACESSORIOS
        listOf(
            "TRANSMISSAO", "CAMBIO", "CAMBIO TRASEIRO", "CAMBIO DIANTEIRO", "DERAILLEUR",
            "TROCADOR", "ALAVANCA DE CAMBIO", "TRIZETA", "HOMOCINETICA", "EMBREAGEM",
            "KIT EMBREAGEM", "PLATO", "DISCO DE EMBREAGEM", "COLAR", "SEMI-EIXO",
            "DIFERENCIAL", "CARDAN"
        ).any { normalized.contains(it) } -> TipoManutencao.TRANSMISSAO
        listOf(
            "REVISAO", "REVISAO GERAL", "CHECKUP", "CHECK-UP", "INSPECAO", "DIAGNOSTICO",
            "SCAN", "SCANNER", "RASTREAMENTO", "VISTORIA", "TROCA PROGRAMADA"
        ).any { normalized.contains(it) } -> TipoManutencao.REVISAO
        listOf(
            "LAVAGEM", "LAVA", "LAVA JATO", "LAVACAO", "HIGIENIZACAO", "HIGIENIZAÇÃO",
            "LIMPEZA TECNICA", "LIMPEZA INTERNA", "ESTETICA AUTOMOTIVA", "DETALHAMENTO"
        ).any { normalized.contains(it) } -> TipoManutencao.LAVAGEM
        listOf(
            "VIDRO", "VIDROS", "PARABRISA", "PARA-BRISA", "PARA BRISA", "VIDRACARIA",
            "RETROVISOR", "PELÍCULA", "PELICULA", "INSULFILM", "LIMPADOR PARABRISA",
            "PALHETA", "PALHETAS"
        ).any { normalized.contains(it) } -> TipoManutencao.VIDROS
        listOf(
            "FUNILARIA", "PINTURA", "MASSA POLIR", "POLIMENTO", "LANTERNAGEM", "PARACHOQUE",
            "PARA-CHOQUE", "LATARIA", "PORTA", "CAPO", "PARALAMA", "PARA-LAMA", "RETROVISOR PINTURA"
        ).any { normalized.contains(it) } -> TipoManutencao.FUNILARIA
        listOf(
            "IPVA", "DPVAT", "TAXA VEICULAR", "LICENCA ANUAL"
        ).any { normalized.contains(it) } -> TipoManutencao.IPVA
        listOf(
            "LICENCIAMENTO", "LICENCIAMENTO ANUAL", "CRLV", "EMISSAO CRLV", "DOCUMENTACAO",
            "DOCUMENTO VEICULO", "TRANSFERENCIA", "EMPLACAMENTO", "PLACAS"
        ).any { normalized.contains(it) } -> TipoManutencao.LICENCIAMENTO
        listOf(
            "SEGURO", "APOLICE", "COBERTURA", "FRANQUIA", "RASTREADOR", "PROTECAO VEICULAR",
            "ASSISTENCIA 24H"
        ).any { normalized.contains(it) } -> TipoManutencao.SEGURO
        listOf(
            "FILTRO", "FILTRO DE OLEO", "FILTRO DE AR", "FILTRO DE CABINE", "FILTRO DE COMBUSTIVEL",
            "CORREIA", "CORREIA DENTADA", "TENSOR", "VELA", "VELAS", "CABO DE VELA",
            "BOBINA", "INJECAO", "INJETOR", "BICO INJETOR", "RADIADOR", "AR COND", "AR-COND",
            "CLIMA", "REFRIG", "REFRIGERACAO", "ADITIVO", "BOMBA DAGUA", "BOMBA D AGUA",
            "BOMBA DE AGUA", "JUNTA", "RETENTOR", "ROLAMENTO", "ROLAMENTOS", "HUB",
            "TERMINAL", "PIVO", "SUSPENSAO", "ESCAPAMENTO", "CATALISADOR", "SONDA LAMBDA",
            "COXIM", "SENSOR", "MOTOR", "MECANICA", "MECANICO", "TBI"
        ).any { normalized.contains(it) } -> TipoManutencao.MECANICA
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
    val larguraTarget = (largura * 0.72).toInt().coerceAtLeast(1)
    val alturaTarget = (altura * 0.40).toInt().coerceAtLeast(1)
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

private fun prepararBitmapParaLeituraQr(bitmap: Bitmap): Bitmap {
    val maxLado = 1700
    val maiorLadoOriginal = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val escala = if (maiorLadoOriginal > maxLado) {
        maxLado.toFloat() / maiorLadoOriginal.toFloat()
    } else {
        1f
    }
    val larguraProcessada = (bitmap.width * escala).roundToInt().coerceAtLeast(1)
    val alturaProcessada = (bitmap.height * escala).roundToInt().coerceAtLeast(1)
    val baseProcessamento = if (larguraProcessada != bitmap.width || alturaProcessada != bitmap.height) {
        Bitmap.createScaledBitmap(bitmap, larguraProcessada, alturaProcessada, true)
    } else {
        bitmap
    }

    val pixels = IntArray(baseProcessamento.width * baseProcessamento.height)
    baseProcessamento.getPixels(pixels, 0, baseProcessamento.width, 0, 0, baseProcessamento.width, baseProcessamento.height)

    var somaLuma = 0L
    for (pixel in pixels) {
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        somaLuma += ((r * 299) + (g * 587) + (b * 114)) / 1000
    }
    val mediaLuma = (somaLuma / pixels.size.coerceAtLeast(1)).toInt()
    val limiar = mediaLuma.coerceIn(95, 175)

    for (index in pixels.indices) {
        val pixel = pixels[index]
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        val luma = ((r * 299) + (g * 587) + (b * 114)) / 1000
        pixels[index] = if (luma >= limiar) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.BLACK
        }
    }

    return Bitmap.createBitmap(
        pixels,
        baseProcessamento.width,
        baseProcessamento.height,
        Bitmap.Config.RGB_565
    )
}

private fun rotacionarBitmapSeguro(bitmap: Bitmap, graus: Float): Bitmap {
    if (graus == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(graus) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
