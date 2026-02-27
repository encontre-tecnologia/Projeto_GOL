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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
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
import br.com.gui.carlembrete.VehicleIcon
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
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

data class NotaQrInfo(
    val valorTotal: Double?,
    val dataCompra: String?,
    val descricaoItens: String? = null,
    val nomeEstabelecimento: String? = null,
    val enderecoEstabelecimento: String? = null
)

const val QR_PARSER_TAG = "ZelluQrParser"
private const val ENABLE_QR_MOCK_DIAGNOSTIC = false

suspend fun consultarNotaPorQrCode(url: String): NotaQrInfo? = withContext(Dispatchers.IO) {
    runCatching {
        if (ENABLE_QR_MOCK_DIAGNOSTIC) {
            testarParserSpMock()
        }
        Log.i(QR_PARSER_TAG, "Iniciando consulta QR URL=$url")
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(15000)
            .get()

        // Padrões mais comuns por estado (SP / MG).
        val porLayout = extrairNotaPorLayout(doc)
        if (porLayout != null && (porLayout.valorTotal != null || porLayout.dataCompra != null)) {
            Log.d(
                QR_PARSER_TAG,
                "Resultado por layout: valor=${porLayout.valorTotal} data=${porLayout.dataCompra} itens=${porLayout.descricaoItens} estabelecimento=${porLayout.nomeEstabelecimento} endereco=${porLayout.enderecoEstabelecimento}"
            )
            return@runCatching porLayout
        }
        Log.d(QR_PARSER_TAG, "Layout não resolveu. Aplicando fallback por regex.")

        val valorTotalPelaUrl = extrairValorVnfDaUrl(url)
        val texto = doc.text()

        // Tentativa 1: seletores comuns da página da NFC-e/SEFAZ.
        val candidatosValor = listOf(
            "#totalNota", ".totalNumb", ".txtMax", ".txtCenter"
        ).flatMap { selector -> doc.select(selector).map { it.text() } }
        val candidatosData = listOf(
            ".txtCenter", ".chave", ".txtObs", ".ui-li-static"
        ).flatMap { selector -> doc.select(selector).map { it.text() } }

        val valorRegex = Regex(
            "(?i)(valor\\s*total|v\\.?\\s*total|total\\s*(?:da\\s*nota|nota|nfce)?|vl\\s*total)\\D{0,30}(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2})"
        )
        val dataRegex = Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b")

        val valorMatch = (
            candidatosValor.firstNotNullOfOrNull { bloco ->
                valorRegex.find(bloco)?.groupValues?.getOrNull(2)
            } ?: valorRegex.find(texto)?.groupValues?.getOrNull(2)
            )
        val valorTotal = valorMatch
            ?.replace(".", "")
            ?.replace(",", ".")
            ?.toDoubleOrNull()
        val valorTotalFinal = when {
            valorTotalPelaUrl != null && valorTotal != null -> maxOf(valorTotalPelaUrl, valorTotal)
            valorTotalPelaUrl != null -> valorTotalPelaUrl
            else -> valorTotal
        }
        val dataCompra = candidatosData.firstNotNullOfOrNull { bloco ->
            dataRegex.find(bloco)?.value
        } ?: dataRegex.find(texto)?.value

        val descricaoItens = extrairItensGenerico(doc)
        val estabelecimento = extrairDadosEstabelecimento(doc)
        Log.d(
            QR_PARSER_TAG,
            "Resultado fallback: valor=$valorTotalFinal valorUrl=$valorTotalPelaUrl data=$dataCompra itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
        )
        if (valorTotalFinal == null && dataCompra == null && descricaoItens.isNullOrBlank()) {
            null
        } else {
            NotaQrInfo(
                valorTotal = valorTotalFinal,
                dataCompra = dataCompra,
                descricaoItens = descricaoItens,
                nomeEstabelecimento = estabelecimento.first,
                enderecoEstabelecimento = estabelecimento.second
            )
        }
    }.onFailure {
        Log.e(QR_PARSER_TAG, "Erro ao consultar QR: ${it.message}", it)
    }.getOrNull()
}

internal fun parseNotaHtmlForTest(html: String, url: String = "https://www.fazenda.sp.gov.br"): NotaQrInfo? {
    val doc = Jsoup.parse(html, url)
    val porLayout = extrairNotaPorLayout(doc)
    if (porLayout != null && (porLayout.valorTotal != null || porLayout.dataCompra != null)) {
        return porLayout
    }

    val valorTotalPelaUrl = extrairValorVnfDaUrl(url)
    val texto = doc.text()
    val candidatosValor = listOf(
        "#totalNota", ".totalNumb", ".txtMax", ".txtCenter"
    ).flatMap { selector -> doc.select(selector).map { it.text() } }
    val candidatosData = listOf(
        ".txtCenter", ".chave", ".txtObs", ".ui-li-static"
    ).flatMap { selector -> doc.select(selector).map { it.text() } }

    val valorRegex = Regex(
        "(?i)(valor\\s*total|v\\.?\\s*total|total\\s*(?:da\\s*nota|nota|nfce)?|vl\\s*total)\\D{0,30}(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2})"
    )
    val dataRegex = Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b")

    val valorMatch = (
        candidatosValor.firstNotNullOfOrNull { bloco ->
            valorRegex.find(bloco)?.groupValues?.getOrNull(2)
        } ?: valorRegex.find(texto)?.groupValues?.getOrNull(2)
        )
    val valorTotal = valorMatch
        ?.replace(".", "")
        ?.replace(",", ".")
        ?.toDoubleOrNull()
    val valorTotalFinal = when {
        valorTotalPelaUrl != null && valorTotal != null -> maxOf(valorTotalPelaUrl, valorTotal)
        valorTotalPelaUrl != null -> valorTotalPelaUrl
        else -> valorTotal
    }
    val dataCompra = candidatosData.firstNotNullOfOrNull { bloco ->
        dataRegex.find(bloco)?.value
    } ?: dataRegex.find(texto)?.value

    val descricaoItens = extrairItensGenerico(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    if (valorTotalFinal == null && dataCompra == null && descricaoItens.isNullOrBlank()) {
        return null
    }
    return NotaQrInfo(
        valorTotal = valorTotalFinal,
        dataCompra = dataCompra,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second
    )
}

private fun extrairNotaPorLayout(doc: Document): NotaQrInfo? {
    val host = runCatching { java.net.URI(doc.location()).host?.lowercase(Locale.ROOT) }.getOrNull().orEmpty()
    return when {
        host.contains("dfe.ms.gov.br") || host.contains("sefaz.ms.gov.br") -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: MS (host=$host) - usando fallback robusto")
            null
        }

        host.contains("fazenda.sp.gov.br") ||
            doc.select("#totalNota, .totalNumb, #linhaTotal, .txtMax").isNotEmpty() -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: SP (host=$host)")
            extrairNotaLayoutSp(doc)
        }

        host.contains("fazenda.mg.gov.br") ||
            doc.select("#valorTotal, .valorTotal, .dadosNf, .nfce, .nfce-body").isNotEmpty() -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: MG (host=$host)")
            extrairNotaLayoutMg(doc)
        }

        else -> {
            Log.d(QR_PARSER_TAG, "Nenhum layout SP/MG identificado (host=$host)")
            null
        }
    }
}

private fun extrairNotaLayoutSp(doc: Document): NotaQrInfo? {
    val valor = sequenceOf(
        doc.select("#totalNota").text(),
        doc.select(".totalNumb").text(),
        doc.select("#linhaTotal").text(),
        doc.select(".txtMax").text(),
        doc.select("td:matchesOwn((?i)valor total|total da nota)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|total\\s*da\\s*nota"))

    val data = sequenceOf(
        doc.select("#dataEmissao").text(),
        doc.select(".txtCenter").text(),
        doc.select(".txtObs").text(),
        doc.select(".ui-li-static").text(),
        doc.select("td:matchesOwn((?i)data de emissao|emissao|data)").text()
    ).map { extrairPrimeiraData(it) }.firstOrNull { it != null }
        ?: encontrarDataProximaAoRotulo(doc, Regex("(?i)data\\s*de\\s*emissao|emissao|data"))

    val descricaoItens = extrairItensSp(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    Log.d(
        QR_PARSER_TAG,
        "SP parser => valor=$valor data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valor == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valor,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second
    )
}

private fun extrairNotaLayoutMg(doc: Document): NotaQrInfo? {
    val valor = sequenceOf(
        doc.select("#valorTotal").text(),
        doc.select(".valorTotal").text(),
        doc.select(".dadosNf").text(),
        doc.select(".nfce").text(),
        doc.select("td:matchesOwn((?i)valor total|valor a pagar|total)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|valor\\s*a\\s*pagar|total"))

    val data = sequenceOf(
        doc.select(".dadosNf").text(),
        doc.select(".nfce").text(),
        doc.select(".nfce-body").text(),
        doc.select("td:matchesOwn((?i)data|emissao)").text()
    ).map { extrairPrimeiraData(it) }.firstOrNull { it != null }
        ?: encontrarDataProximaAoRotulo(doc, Regex("(?i)data|emissao"))

    val descricaoItens = extrairItensMg(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    Log.d(
        QR_PARSER_TAG,
        "MG parser => valor=$valor data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valor == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valor,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second
    )
}

private fun extrairItensSp(doc: Document): String? {
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf("#tabResult tr", ".txtTit", ".txtProd", "table tr")
    )
}

private fun extrairItensMg(doc: Document): String? {
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf(".dadosNf tr", ".nfce tr", ".produto", ".descricao", "table tr")
    )
}

private fun extrairItensGenerico(doc: Document): String? {
    val porTitulos = extrairItensPorTitulosEValores(doc)
    if (!porTitulos.isNullOrBlank()) return porTitulos
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf("tr", "li", ".ui-li-static", ".txtTit", ".txtProd", ".descricao")
    )
}

private fun extrairItensPorLinhas(doc: Document, seletoresLinha: List<String>): String? {
    val itens = mutableListOf<String>()
    seletoresLinha.forEach { seletor ->
        doc.select(seletor).forEach { linha ->
            val item = montarItemDescricao(linha.text().trim())
            if (!item.isNullOrBlank()) itens.add(item)
        }
    }
    return normalizarItensDescricao(itens)
}

private fun montarItemDescricao(textoLinha: String): String? {
    if (textoLinha.isBlank()) return null
    val texto = Parser.unescapeEntities(textoLinha, false).replace(Regex("\\s+"), " ").trim()
    val lower = texto.lowercase(Locale.ROOT)
    if (
        lower.contains("valor total") ||
        lower.contains("total da nota") ||
        lower.contains("chave de acesso") ||
        lower.contains("protocolo") ||
        lower.contains("tribut") ||
        lower.contains("emissao") ||
        lower.contains("qtd") ||
        lower.contains("qtde") ||
        lower.contains("un ") ||
        lower.startsWith("codigo")
    ) return null

    val valor = extrairUltimoValorMonetario(texto) ?: return null
    val cortePorRotulo = texto.split(
        Regex("(?i)\\b(c[oó]digo|cod\\.?|qtde|qtd|un|vl\\.?\\s*total|valor\\s*total)\\b"),
        limit = 2
    ).firstOrNull().orEmpty()
    val nomeBase = texto
        .substringBefore("(")
        .let { if (cortePorRotulo.isNotBlank()) cortePorRotulo else it }
        .replace(Regex("(?i)\\b(c[oó]digo|cod\\.?|qtde|qtd|un|vl\\.?|r\\$)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (nomeBase.isBlank()) return null
    return "${nomeBase.take(60)} (${String.format(Locale.US, "%.2f", valor)})"
}

private fun extrairItensPorTitulosEValores(doc: Document): String? {
    val nomes = doc.select(".txtTit, .txtProd, .descricao, .xProd")
        .mapNotNull { limparNomeProduto(it.text()) }
        .distinct()
    if (nomes.isEmpty()) return null

    val valores = doc.select(".valor, .valorItem, .vProd, .preco, .valorProduto, .RxValor")
        .mapNotNull { extrairPrimeiroValorMonetario(it.text()) }
        .toList()
    if (valores.isEmpty()) return nomes.take(4).joinToString(" + ")

    val limite = minOf(nomes.size, valores.size, 6)
    if (limite == 0) return null
    val itens = (0 until limite).map { i ->
        "${nomes[i].take(60)} (${String.format(Locale.US, "%.2f", valores[i])})"
    }
    return normalizarItensDescricao(itens)
}

private fun limparNomeProduto(textoOriginal: String): String? {
    val texto = Parser.unescapeEntities(textoOriginal, false)
        .replace(Regex("\\s+"), " ")
        .trim()
    if (texto.isBlank()) return null
    val lower = texto.lowercase(Locale.ROOT)
    if (
        lower.contains("valor total") ||
        lower.contains("total da nota") ||
        lower.contains("vl. total") ||
        lower.startsWith("total") ||
        lower.startsWith("codigo") ||
        lower.startsWith("código")
    ) return null
    return texto.substringBefore("(").trim().ifBlank { null }
}

private fun normalizarItensDescricao(itens: List<String>): String? {
    val normalizados = itens
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { it.contains(" (0.00)") }
        .distinct()
        .take(6)
    if (normalizados.isEmpty()) return null
    return normalizados.joinToString(" + ")
}

private fun extrairDadosEstabelecimento(doc: Document): Pair<String?, String?> {
    val nome = sequenceOf(
        doc.select(".txtTopo").firstOrNull()?.text(),
        doc.select(".txtTit").firstOrNull()?.text(),
        doc.select("h1").firstOrNull()?.text(),
        doc.select("h2").firstOrNull()?.text()
    ).mapNotNull { it?.let { t -> Parser.unescapeEntities(t, false) }?.replace(Regex("\\s+"), " ")?.trim() }
        .firstOrNull { texto ->
            texto.length > 3 &&
                !texto.contains("nfc-e", ignoreCase = true) &&
                !texto.contains("danfe", ignoreCase = true) &&
                !texto.contains("consulta", ignoreCase = true)
        }

    val endereco = sequenceOf(
        doc.select(".text").firstOrNull { it.text().contains(",") }?.text(),
        doc.select(".txtEndereco").firstOrNull()?.text(),
        doc.select(".enderEmit").firstOrNull()?.text(),
        doc.select(".txtEnder").firstOrNull()?.text(),
        doc.select(".dadosEmit").firstOrNull()?.text(),
        doc.select(".txtCorpo").firstOrNull()?.text(),
        doc.select(".txtTopo").firstOrNull { possuiPadraoEndereco(it.text()) }?.text()
    ).mapNotNull { it?.let { t -> Parser.unescapeEntities(t, false) }?.replace(Regex("\\s+"), " ")?.trim() }
        .firstOrNull { possuiPadraoEndereco(it) }

    val enderecoFinal = endereco ?: extrairEnderecoPorRegex(doc)
    return Pair(nome, enderecoFinal)
}

private fun possuiPadraoEndereco(texto: String): Boolean {
    val t = texto.lowercase(Locale.ROOT)
    val temVia = t.contains("rua") || t.contains("av") || t.contains("avenida") || t.contains("rod")
    return temVia && texto.contains(",")
}

private fun extrairEnderecoPorRegex(doc: Document): String? {
    val texto = Parser.unescapeEntities(doc.text(), false).replace(Regex("\\s+"), " ").trim()
    if (texto.isBlank()) return null
    val regexEndereco = Regex(
        "(?i)\\b(rua|r\\.|avenida|av\\.|travessa|trv\\.|alameda|rodovia|rod\\.)\\b[^|]{8,140}"
    )
    val encontrado = regexEndereco.find(texto)?.value?.trim() ?: return null
    return encontrado.take(120)
}

private fun extrairPrimeiroValorMonetario(texto: String): Double? {
    val valor = Regex("\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2}")
        .find(texto)
        ?.value
        ?: return null
    return valor.replace(".", "").replace(",", ".").toDoubleOrNull()
}

fun montarLocalNota(estabelecimento: String, endereco: String): String {
    val partes = mutableListOf<String>()
    if (estabelecimento.isNotBlank()) partes += estabelecimento
    if (endereco.isNotBlank()) partes += endereco
    return partes.joinToString(" - ")
}

fun montarDescricaoItensNota(total: Double?, itens: String?): String {
    val partes = mutableListOf<String>()
    if (total != null) partes += "Total: R$ ${String.format(Locale.US, "%.2f", total)}"
    if (!itens.isNullOrBlank()) partes += "Itens: $itens"
    if (partes.isEmpty()) {
        return if (total != null) {
            "Servico da nota (R$ ${String.format(Locale.US, "%.2f", total)})"
        } else {
            "Servico da nota"
        }
    }
    return partes.joinToString(" | ")
}

fun extrairItensDaDescricaoQr(descricao: String?): List<ItemDetectado> {
    if (descricao.isNullOrBlank()) return emptyList()
    val blocoItens = descricao.substringAfter("Itens:", descricao).trim()
    if (blocoItens.isBlank()) return emptyList()
    val partes = blocoItens.split("+").map { it.trim() }.filter { it.isNotBlank() }
    val regexItem = Regex("(.+?)\\s*\\((\\d+[\\.,]\\d{2})\\)")
    return partes.mapNotNull { parte ->
        val match = regexItem.find(parte) ?: return@mapNotNull null
        val nome = match.groupValues[1].trim()
        val valor = match.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 0.0
        if (nome.isBlank()) return@mapNotNull null
        ItemDetectado(
            id = UUID.randomUUID().toString(),
            nome = nome,
            tipo = detectarTipoPeloTexto(nome),
            valor = valor,
            quantidade = 1
        )
    }
}

private fun extrairValorVnfDaUrl(url: String): Double? {
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    val query = uri.rawQuery ?: return null
    val valorBruto = query
        .split("&")
        .firstOrNull { it.startsWith("vNF=", ignoreCase = true) }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return valorBruto.replace(",", ".").toDoubleOrNull()
}

private fun extrairUltimoValorMonetario(texto: String): Double? {
    val valores = Regex("\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2}")
        .findAll(texto)
        .map { it.value }
        .toList()
    val ultimo = valores.lastOrNull() ?: return null
    return ultimo.replace(".", "").replace(",", ".").toDoubleOrNull()
}

private fun extrairPrimeiraData(texto: String): String? =
    Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b").find(texto)?.value

private fun encontrarValorProximoAoRotulo(doc: Document, rotulo: Regex): Double? {
    val elementos = doc.select("*").take(400)
    elementos.forEach { el ->
        if (!rotulo.containsMatchIn(el.ownText())) return@forEach
        val candidatos = listOf(
            el.ownText(),
            el.text(),
            el.nextElementSibling()?.text().orEmpty(),
            el.parent()?.text().orEmpty()
        )
        candidatos.forEach { bloco ->
            val valor = extrairPrimeiroValorMonetario(bloco)
            if (valor != null) return valor
        }
    }
    return null
}

private fun encontrarDataProximaAoRotulo(doc: Document, rotulo: Regex): String? {
    val elementos = doc.select("*").take(400)
    elementos.forEach { el ->
        if (!rotulo.containsMatchIn(el.ownText())) return@forEach
        val candidatos = listOf(
            el.ownText(),
            el.text(),
            el.nextElementSibling()?.text().orEmpty(),
            el.parent()?.text().orEmpty()
        )
        candidatos.forEach { bloco ->
            val data = extrairPrimeiraData(bloco)
            if (data != null) return data
        }
    }
    return null
}

private fun testarParserSpMock() {
    val htmlMockSp = """
        <html>
            <body>
                <span class="txtTit">Gasolina Comum</span>
                <span class="valor">150,00</span>
                <div id="linhaTotal">
                    <label>Valor Total R$</label>
                    <span class="totalNumb">150,00</span>
                </div>
                <div id="dataEmissao">Emissão: 02/02/2026 13:30:00</div>
            </body>
        </html>
    """.trimIndent()

    val doc = Jsoup.parse(htmlMockSp)
    val resultado = extrairNotaLayoutSp(doc)
    Log.i(
        QR_PARSER_TAG,
        "Teste mock SP => valor=${resultado?.valorTotal} data=${resultado?.dataCompra} itens=${resultado?.descricaoItens} estabelecimento=${resultado?.nomeEstabelecimento} endereco=${resultado?.enderecoEstabelecimento}"
    )
}

