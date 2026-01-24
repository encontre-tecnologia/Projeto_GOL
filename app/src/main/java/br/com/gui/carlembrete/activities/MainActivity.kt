package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var contentInitialized = false

    private val onboardingResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            initializeContentIfNeeded()
        } else if (!contentInitialized) {
            finish()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Ative as notificações para receber os avisos programados", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        NotificacaoHelper.criarCanal(applicationContext)

        if (AppPreferences.needsOnboarding(this)) {
            onboardingResultLauncher.launch(Intent(this, OnboardingActivity::class.java))
        } else {
            initializeContentIfNeeded()
        }

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    }

    private fun initializeContentIfNeeded() {
        if (contentInitialized) return
        contentInitialized = true
        setContent {
            CarLembreteTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                var usuario by remember { mutableStateOf(auth.currentUser) }
                var showLoading by remember { mutableStateOf(false) }
                var loadingDoneSignal by remember { mutableIntStateOf(0) }
                val loadingProgress = remember { Animatable(0f) }
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        usuario = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }
                LaunchedEffect(usuario) {
                    if (usuario != null) {
                        showLoading = true
                        loadingProgress.snapTo(0f)
                        loadingProgress.animateTo(0.9f, animationSpec = tween(durationMillis = 1200))
                    } else {
                        showLoading = false
                    }
                }
                LaunchedEffect(loadingDoneSignal) {
                    if (loadingDoneSignal > 0) {
                        loadingProgress.animateTo(1f, animationSpec = tween(durationMillis = 400))
                        delay(200)
                        showLoading = false
                    }
                }
                val baseBackground = if (usuario == null) Color.Black else Color(0xFF0F2A4A)
                Surface(modifier = Modifier.fillMaxSize(), color = baseBackground) {
                    if (usuario == null) {
                        AuthScreen(onSignedIn = { })
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                                ManutencaoScreen(
                                    onLoaded = { loadingDoneSignal++ }
                                )
                            if (showLoading) {
                                LoadingScreen(progress = loadingProgress.value)
                            }
                        }
                    }
                }
            }
        }
    }
}




/* ----------------- UTILITÁRIOS & LÓGICA DE CÁLCULO ----------------- */

fun String.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
}

fun calcularProximaData(tipo: TipoManutencao, dataServico: LocalDate): String {
    val mesesParaAdicionar = when (tipo) {
        TipoManutencao.OLEO -> 6L
        TipoManutencao.BATERIA -> 24L
        TipoManutencao.FREIO -> 12L
        TipoManutencao.MECANICA -> 6L
        TipoManutencao.TEMPERATURA -> 12L
        TipoManutencao.LICENCIAMENTO -> 12L
        TipoManutencao.IPVA -> 12L
        TipoManutencao.SEGURO -> 12L
        TipoManutencao.OUTROS -> 3L
    }
    return dataServico.plusMonths(mesesParaAdicionar).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

fun getKmAdicionalPorTipo(tipo: TipoManutencao): Int {
    return when (tipo) {
        TipoManutencao.OLEO -> 10000
        TipoManutencao.BATERIA -> 40000
        TipoManutencao.FREIO -> 20000
        TipoManutencao.MECANICA -> 10000
        TipoManutencao.TEMPERATURA -> 30000
        TipoManutencao.LICENCIAMENTO -> 0
        TipoManutencao.IPVA -> 0
        TipoManutencao.SEGURO -> 0
        TipoManutencao.OUTROS -> 5000
    }
}

fun formatarMoeda(valor: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

fun gerarResumoRelatorio(carro: CarroInfo, lembretes: List<Lembrete>): String {
    val builder = StringBuilder()
    builder.appendLine("Relatório do veículo")
    builder.appendLine("Nome: ${carro.nome}")
    builder.appendLine("Proprietário: ${carro.proprietario.ifBlank { "Não informado" }}")
    builder.appendLine("Marca: ${carro.marca.ifBlank { "Não informada" }}")
    builder.appendLine("Modelo: ${carro.modelo}")
    builder.appendLine("Odômetro: ${if (carro.kmAtual > 0) "${carro.kmAtual} km" else "Não informado"}")
    builder.appendLine()
    builder.appendLine("Avisos ativos: ${lembretes.size}")
    TipoManutencao.values().forEach { tipo ->
        val count = lembretes.count { it.tipo == tipo }
        if (count > 0) builder.appendLine("- ${tipo.label}: $count")
    }
    if (lembretes.isNotEmpty()) {
        builder.appendLine()
        builder.appendLine("Detalhes dos próximos serviços:")
        lembretes.sortedBy { it.dataLimite }.forEach { lembrete ->
            builder.appendLine("* ${lembrete.titulo} - Data: ${lembrete.dataLimite.ifBlank { "Sem data" }} - KM: ${lembrete.kmLimite.ifBlank { "-" }}")
        }
    }
    builder.appendLine()
    builder.appendLine("Gerado via CarLembrete")
    return builder.toString()
}

fun compartilharRelatorio(context: Context, texto: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
}

fun gerarPdfRelatorio(context: Context, carro: CarroInfo, lembretes: List<Lembrete>): Uri? {
    return try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas
        val headerPaint = Paint().apply {
            textSize = 24f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerSubPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            textSize = 13f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val valueBoldPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
        }
        val debugPaint = Paint().apply {
            textSize = 26f
            color = android.graphics.Color.RED
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            strokeWidth = 2f
            color = android.graphics.Color.parseColor("#94A3B8")
            isAntiAlias = true
        }
        val colorSuccess = android.graphics.Color.parseColor("#16A34A")
        val colorDanger = android.graphics.Color.parseColor("#DC2626")
        val accentColor = android.graphics.Color.parseColor("#2563EB")
        val logoBitmap = try {
            context.assets.open("logorelatorio.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F8FAFC") }
        val headerCardPaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
        val headerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val marginX = 36f
        canvas.drawColor(android.graphics.Color.WHITE)
        val topBarPaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
        canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 6f, topBarPaint)
        val contentWidth = pageInfo.pageWidth - marginX * 2
        var y = 170f

        fun fit(text: String, maxChars: Int): String =
            if (text.length <= maxChars) text else text.take(maxChars - 3) + "..."

        fun ensureSpace(extra: Float) {
            if (y + extra > pageInfo.pageHeight - 40) {
                document.finishPage(currentPage)
                val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                currentPage = document.startPage(nextPageInfo)
                canvas = currentPage.canvas
                y = 60f
            }
        }

        fun drawHeader() {
            val titleCenterPaint = Paint(headerPaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("RELATORIO TÉCNICO", pageInfo.pageWidth / 2f, 54f, titleCenterPaint)
            val tituloCarro = buildString {
                append(carro.nome)
                val detalhes = listOf(carro.marca, carro.modelo).filter { it.isNotBlank() }.joinToString(" - ")
                if (detalhes.isNotBlank()) append(" - $detalhes")
            }
            canvas.drawLine(marginX, 78f, pageInfo.pageWidth - marginX, 78f, dividerPaint)
            canvas.drawText(fit(tituloCarro, 60), marginX + 12f, 100f, headerSubPaint)
            canvas.drawText(
                "Gerado em ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                marginX + 12f,
                126f,
                headerSubPaint
            )
            canvas.drawLine(marginX, 140f, pageInfo.pageWidth - marginX, 140f, dividerPaint)
        }

        fun drawSectionTitle(title: String) {
            ensureSpace(30f)
            val titleY = y
            canvas.drawText(title, marginX, titleY, sectionTitlePaint)
            y += 10f
            canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
            y += 12f
        }

        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        fun drawCard(height: Float, content: (Float) -> Unit) {
            ensureSpace(height + 6f)
            val rect = android.graphics.RectF(marginX, y, marginX + contentWidth, y + height)
            canvas.drawRoundRect(rect, 12f, 12f, cardBgPaint)
            canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
            content(y)
            y += height + 28f
        }

        fun drawWrappedText(text: String, x: Float, maxWidth: Float, paint: Paint): Float {
            val words = text.split(" ")
            var line = ""
            var currentY = y
            words.forEach { word ->
                val test = if (line.isBlank()) word else "$line $word"
                if (paint.measureText(test) <= maxWidth) {
                    line = test
                } else {
                    canvas.drawText(line, x, currentY, paint)
                    currentY += 14f
                    line = word
                }
            }
            if (line.isNotBlank()) {
                canvas.drawText(line, x, currentY, paint)
                currentY += 14f
            }
            return currentY
        }

        fun drawKeyValue(label: String, value: String, x: Float, lineY: Float, valueOffset: Float = 14f) {
            canvas.drawText(label.uppercase(Locale.getDefault()), x, lineY, valueBoldPaint)
            canvas.drawText(value, x, lineY + valueOffset, valuePaint)
        }

        drawHeader()

        val totalGastos = lembretes.sumOf { it.valor }
        val proximos = lembretes
            .mapNotNull { lembrete ->
                val data = try { LocalDate.parse(lembrete.dataLimite, DateTimeFormatter.ofPattern("dd/MM/yyyy")) } catch (_: Exception) { null }
                data?.let { lembrete to it }
            }
            .sortedBy { it.second }
        val proximoServico = proximos.firstOrNull()?.second?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "--"

        drawSectionTitle("IDENTIFICACAO DO VEICULO")
        val boxHeight = 180f
        ensureSpace(boxHeight)
        canvas.drawRect(marginX, y, marginX + contentWidth, y + boxHeight, cardBgPaint)
        canvas.drawRoundRect(android.graphics.RectF(marginX, y, marginX + contentWidth, y + boxHeight), 12f, 12f, cardBorderPaint)
        val leftX = marginX + 12f
        val rightX = marginX + contentWidth / 2 + 10f
        val rowY = y + 24f
        drawKeyValue("Nome", fit(carro.nome, 30), leftX, rowY)
        drawKeyValue("Motor", fit(carro.modelo.ifBlank { "-" }, 26), rightX, rowY)
        drawKeyValue("Marca", carro.marca.ifBlank { "-" }, leftX, rowY + 42f)
        drawKeyValue("Tipo", carro.tipoVeiculo.label, rightX, rowY + 42f)
        val proprietarioTexto = carro.proprietario.ifBlank { "-" }
        drawKeyValue("Proprietario", fit(proprietarioTexto, 30), leftX, rowY + 78f)
        val odometroTexto = if (carro.kmAtual > 0) "${carro.kmAtual} km" else "Nao informado"
        drawKeyValue("Odometro", odometroTexto, rightX, rowY + 78f)
        val corHex = String.format(Locale.US, "#%08X", carro.corArgb)
        drawKeyValue("Cor", corHex, leftX, rowY + 114f)
        y += boxHeight + 24f

        drawSectionTitle("STATUS E SAUDE")
        val (tituloRep, descRep) = calcularReputacao(lembretes)
        val statusBoxHeight = 160f
        ensureSpace(statusBoxHeight)
        canvas.drawRect(marginX, y, marginX + contentWidth, y + statusBoxHeight, cardBgPaint)
        canvas.drawRoundRect(android.graphics.RectF(marginX, y, marginX + contentWidth, y + statusBoxHeight), 12f, 12f, cardBorderPaint)
        val saudeColor = if (tituloRep == "Excelente") colorSuccess else colorDanger
        val pillPaint = Paint().apply { color = saudeColor }
        val pillTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pillText = tituloRep.uppercase(Locale.getDefault())
        val pillWidth = pillTextPaint.measureText(pillText) + 16f
        canvas.drawText("SAUDE", leftX, y + 24f, valueBoldPaint)
        val pillRect = android.graphics.RectF(leftX, y + 30f, leftX + pillWidth, y + 46f)
        canvas.drawRoundRect(pillRect, 10f, 10f, pillPaint)
        canvas.drawText(pillText, leftX + 8f, y + 42f, pillTextPaint)
        drawKeyValue("Alertas ativos", lembretes.size.toString(), rightX, y + 24f)
        drawKeyValue("Proximo servico", proximoServico, leftX, y + 74f)
        drawKeyValue("Total gasto", formatarMoeda(totalGastos), rightX, y + 74f)
        val resultadoGeral = "RESULTADO GERAL"
        val saudeLabel = if (tituloRep == "Excelente") "Todas as manutencoes em dia" else "Revisar manutencoes pendentes"
        val saudePaint = Paint(bodyPaint).apply {
            color = if (tituloRep == "Excelente") colorSuccess else colorDanger
        }
        val saudeMetrics = saudePaint.fontMetrics
        val saudeBaseline = y + statusBoxHeight - 14f - saudeMetrics.descent
        val resultadoBaseline = saudeBaseline - 14f
        canvas.drawText(resultadoGeral, leftX, resultadoBaseline, valueBoldPaint)
        canvas.drawText(saudeLabel, leftX, saudeBaseline, saudePaint)
        y += statusBoxHeight + 34f

        drawSectionTitle("DOCUMENTACAO")
        val documentos = listOf(
            TipoManutencao.IPVA to "IPVA",
            TipoManutencao.LICENCIAMENTO to "Licenciamento"
        ).map { (tipo, label) ->
            val ultimaData = lembretes
                .filter { it.tipo == tipo }
                .map { dataParaOrdenacao(it) }
                .filter { it != LocalDate.MAX }
                .maxOrNull()
            val status = when {
                ultimaData == null -> "Nao informado"
                !ultimaData.isBefore(LocalDate.now()) -> "Em dia"
                else -> "Vencido"
            }
            Triple(label, status, ultimaData?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "--")
        }
        val docsVazios = documentos.all { it.second == "Nao informado" }
        val docsCardHeight = if (docsVazios) 70f else 70f + (documentos.size * 18f)
        drawCard(docsCardHeight) { topY ->
            var rowY = topY + 28f
            if (docsVazios) {
                val avisoPaint = Paint(bodyPaint).apply {
                    color = colorDanger
                    textSize = 13f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Veiculo sem documentacao", marginX + (contentWidth / 2), rowY + 6f, avisoPaint)
            } else {
                documentos.forEach { (label, status, data) ->
                    canvas.drawText(label, marginX + 16f, rowY, valuePaint)
                    val dateCenterPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText("Venc: $data", marginX + (contentWidth / 2), rowY, dateCenterPaint)
                    val statusPaint = Paint(valuePaint).apply {
                        color = if (status == "Em dia") colorSuccess else colorDanger
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(status, marginX + contentWidth - 16f, rowY, statusPaint)
                    rowY += 22f
                }
            }
        }
        y += 8f

        document.finishPage(currentPage)
        val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
        currentPage = document.startPage(nextPageInfo)
        canvas = currentPage.canvas
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawRect(0f, 0f, nextPageInfo.pageWidth.toFloat(), 6f, topBarPaint)
        y = 60f

        drawSectionTitle("TROCAS POR PECA")
        val pecaLabels = linkedMapOf<String, String>()
        lembretes.forEach { lembrete ->
            val raw = lembrete.peca.ifBlank { lembrete.titulo }.trim()
            if (raw.isNotBlank()) {
                val key = raw.lowercase(Locale.getDefault())
                pecaLabels.putIfAbsent(key, raw)
            }
        }
        val trocasPorPeca = lembretes
            .map { lembrete -> lembrete.peca.ifBlank { lembrete.titulo }.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it.lowercase(Locale.getDefault()) }
            .eachCount()
            .map { (key, count) -> (pecaLabels[key] ?: key) to count }
            .sortedByDescending { it.second }
            .take(8)
        val trocasCardHeight = if (trocasPorPeca.isEmpty()) 60f else 70f + (trocasPorPeca.size * 16f)
        drawCard(trocasCardHeight) { topY ->
            var rowY = topY + 28f
            if (trocasPorPeca.isEmpty()) {
                canvas.drawText("Nenhuma peca registrada.", marginX + 16f, rowY, bodyPaint)
            } else {
                trocasPorPeca.forEach { (peca, count) ->
                    canvas.drawText("- ${fit(peca, 36)}", marginX + 16f, rowY, bodyPaint)
                    val countPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText("${count}x", marginX + contentWidth - 16f, rowY, countPaint)
                    rowY += 20f
                }
            }
        }
        y += 8f

        drawSectionTitle("MANUTENCOES FUTURAS")
        if (proximos.isEmpty()) {
            canvas.drawText("Nenhum lembrete cadastrado.", marginX, y, bodyPaint)
            y += 16f
        } else {
            val headerHeight = 22f
            val headerBg = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
            canvas.drawRect(marginX, y, marginX + contentWidth, y + headerHeight, headerBg)
            val headerTextPaint = Paint(labelPaint).apply {
                color = android.graphics.Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerY = y + 15f
            canvas.drawText("Item", marginX + 6f, headerY, headerTextPaint)
            canvas.drawText("Data", marginX + 240f, headerY, headerTextPaint)
            canvas.drawText("KM", marginX + 330f, headerY, headerTextPaint)
            canvas.drawText("Cat.", marginX + 420f, headerY, headerTextPaint)
            y += headerHeight + 8f
            proximos.take(10).forEach { (lembrete, data) ->
                ensureSpace(26f)
                val rowTextY = y + 4f
                canvas.drawText(fit(lembrete.titulo, 28), marginX + 6f, rowTextY, bodyPaint)
                canvas.drawText(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), marginX + 240f, rowTextY, bodyPaint)
                canvas.drawText(lembrete.kmLimite.ifBlank { "-" }, marginX + 330f, rowTextY, bodyPaint)
                canvas.drawText(fit(lembrete.tipo.label, 8), marginX + 420f, rowTextY, bodyPaint)
                y += 26f
                canvas.drawLine(marginX, y - 14f, marginX + contentWidth, y - 14f, dividerPaint)
            }
        }
        y += 18f
        canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
        y += 24f
        if (logoBitmap != null) {
            val targetWidth = 160f
            val scale = targetWidth / logoBitmap.width.toFloat()
            val targetHeight = logoBitmap.height * scale
            val scaled = Bitmap.createScaledBitmap(logoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
            val left = (pageInfo.pageWidth - targetWidth) / 2f
            canvas.drawBitmap(scaled, left, y, null)
            y += targetHeight
        }

        document.finishPage(currentPage)
        val pdfFile = File(context.cacheDir, "relatorio_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { output -> document.writeTo(output) }
        document.close()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    } catch (e: Exception) {
        Log.e("PDF", "Erro ao gerar PDF", e)
        null
    }
}

fun compartilharPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
}





