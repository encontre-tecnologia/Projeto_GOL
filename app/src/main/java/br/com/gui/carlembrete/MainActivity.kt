package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
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
    }

    private fun initializeContentIfNeeded() {
        if (contentInitialized) return
        contentInitialized = true
        setContent {
            CarLembreteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
                    ManutencaoScreen()
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
        TipoManutencao.OUTROS -> 5000
    }
}

fun formatarMoeda(valor: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

fun gerarResumoRelatorio(carro: CarroInfo, lembretes: List<Lembrete>): String {
    val builder = StringBuilder()
    builder.appendLine("Relatório do veículo")
    builder.appendLine("Nome: ${carro.nome}")
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
            builder.appendLine("* ${lembrete.titulo} • Data: ${lembrete.dataLimite.ifBlank { "Sem data" }} • KM: ${lembrete.kmLimite.ifBlank { "-" }}")
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
        val titlePaint = Paint().apply {
            textSize = 24f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.DKGRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.DKGRAY
        }
        val dividerPaint = Paint().apply {
            strokeWidth = 2f
            color = android.graphics.Color.LTGRAY
        }
        val marginX = 40f
        var y = 60f

        fun ensureSpace(extra: Float) {
            if (y + extra > pageInfo.pageHeight - 40) {
                document.finishPage(currentPage)
                val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                currentPage = document.startPage(nextPageInfo)
                canvas = currentPage.canvas
                y = 60f
            }
        }

        canvas.drawText("Relatório do Veículo", marginX, y, titlePaint)
        y += 30f
        canvas.drawText("Gerado em ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", marginX, y, bodyPaint)
        y += 24f
        canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
        y += 24f

        val tituloCarro = buildString {
            append(carro.nome)
            val detalhes = listOf(carro.marca, carro.modelo).filter { it.isNotBlank() }.joinToString(" · ")
            if (detalhes.isNotBlank()) {
                append(" - ")
                append(detalhes)
            }
        }
        canvas.drawText(tituloCarro, marginX, y, subtitlePaint)
        y += 18f
        val odometroTexto = if (carro.kmAtual > 0) "Odômetro: ${carro.kmAtual} km" else "Odômetro: não informado"
        canvas.drawText(odometroTexto, marginX, y, bodyPaint)
        y += 28f

        canvas.drawText("Status Geral", marginX, y, subtitlePaint)
        y += 20f
        TipoManutencao.values().forEach { tipo ->
            ensureSpace(20f)
            val quantidade = lembretes.count { it.tipo == tipo }
            val status = when (calcularCorStatus(lembretes, tipo)) {
                Color(0xFFEF4444) -> "CRÍTICO"
                Color(0xFFEAB308) -> "ATENÇÃO"
                Color(0xFF10B981) -> "OK"
                else -> "SEM DADOS"
            }
            canvas.drawText("- ${tipo.label}: $quantidade avisos ($status)", marginX + 10f, y, bodyPaint)
            y += 18f
        }
        y += 16f
        val (tituloRep, descRep) = calcularReputacao(lembretes)
        canvas.drawText("Reputação: $tituloRep", marginX, y, subtitlePaint)
        y += 18f
        canvas.drawText(descRep, marginX, y, bodyPaint)
        y += 28f

        canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
        y += 24f
        canvas.drawText("Próximas manutenções", marginX, y, subtitlePaint)
        y += 20f
        if (lembretes.isEmpty()) {
            canvas.drawText("Nenhum lembrete cadastrado.", marginX + 10f, y, bodyPaint)
            y += 18f
        } else {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val proximos = lembretes
                .mapNotNull { lembrete ->
                    val data = try { LocalDate.parse(lembrete.dataLimite, formatter) } catch (e: Exception) { null }
                    data?.let { lembrete to it }
                }
                .sortedBy { it.second }
            proximos.forEach { (lembrete, data) ->
                ensureSpace(40f)
                canvas.drawText("• ${lembrete.titulo}", marginX + 10f, y, bodyPaint)
                y += 16f
                canvas.drawText("  Data: ${lembrete.dataLimite.ifBlank { data.format(formatter) }} | KM: ${lembrete.kmLimite.ifBlank { "–" }}", marginX + 10f, y, bodyPaint)
                y += 20f
            }
        }
        y += 16f
        canvas.drawLine(marginX, y, pageInfo.pageWidth - marginX, y, dividerPaint)
        y += 20f
        canvas.drawText("Gerado via CarLembrete", marginX, y, bodyPaint)

        document.finishPage(currentPage)
        val pdfFile = File(context.cacheDir, "relatorio_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { output -> document.writeTo(output) }
        document.close()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    } catch (e: Exception) {
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

private val padroesQuantidade = listOf(
    Regex("QTD\\s*[:=]?\\s*(\\d+)", RegexOption.IGNORE_CASE),
    Regex("(\\d+)\\s?(?:UN|UND|UNID|P[ÇC]S?|PCS|ITENS?)\\b", RegexOption.IGNORE_CASE),
    Regex("(\\d+)\\s?[X×]", RegexOption.IGNORE_CASE),
    Regex("[X×]\\s?(\\d+)", RegexOption.IGNORE_CASE)
)

fun extrairQuantidadeDaParte(parte: String): Int? {
    val texto = parte.trim()
    for (regex in padroesQuantidade) {
        val match = regex.find(texto)
        if (match != null) {
            val quantidade = match.groupValues[1].toIntOrNull()
            if (quantidade != null && quantidade > 1) return quantidade
        }
    }
    return null
}

