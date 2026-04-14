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
    val enderecoEstabelecimento: String? = null,
    val valorBruto: Double? = null,
    val valorDesconto: Double? = null,
    val valorFinalComDesconto: Double? = null,
    val quantidadeTotalItens: Int? = null,
    val formaPagamento: String? = null
)

private data class ResumoNotaFinanceiro(
    val valorTotal: Double?,
    val valorDesconto: Double?,
    val valorFinal: Double?,
    val quantidadeTotalItens: Int?,
    val formaPagamento: String?
)

const val QR_PARSER_TAG = "ZelluQrParser"
private const val ENABLE_QR_MOCK_DIAGNOSTIC = false
private const val MENSAGEM_BLOQUEIO_SP =
    "Consulta automatica indisponivel na SEFAZ-SP (sessao/captcha). Tente novamente ou use OCR da foto."

internal fun notaQrIndicaBloqueioSp(notaInfo: NotaQrInfo?): Boolean {
    val descricao = notaInfo?.descricaoItens?.trim().orEmpty()
    return descricao.equals(MENSAGEM_BLOQUEIO_SP, ignoreCase = true)
}

internal fun montarUrlValidacaoHumanaSp(qrUrl: String): String? {
    val normalizada = normalizarUrlQr(qrUrl)
    val uri = runCatching { Uri.parse(normalizada) }.getOrNull() ?: return null
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    if (!host.contains("nfce.fazenda.sp.gov.br")) return null
    return normalizarRotaSpNfce(normalizarParametroPDaUrl(normalizada))
}

suspend fun consultarNotaPorQrCode(url: String, cookieHeader: String? = null): NotaQrInfo? = withContext(Dispatchers.IO) {
    runCatching {
        if (ENABLE_QR_MOCK_DIAGNOSTIC) {
            testarParserSpMock()
        }
        val urlNormalizada = normalizarUrlQr(url)
        fun conexao(urlConsulta: String): org.jsoup.Connection {
            val req = Jsoup.connect(urlConsulta)
                .userAgent("Mozilla/5.0")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .header("Referer", "https://www.nfce.fazenda.sp.gov.br/")
                .followRedirects(true)
                .timeout(15000)
            if (!cookieHeader.isNullOrBlank()) {
                req.header("Cookie", cookieHeader)
            }
            return req
        }
        Log.i(QR_PARSER_TAG, "Iniciando consulta QR URL=$urlNormalizada")
        var doc = conexao(urlNormalizada).get()
        Log.d(
            QR_PARSER_TAG,
            "Resposta QR: location=${doc.location()} title=${doc.title().take(120)}"
        )

        if (doc.location().contains("%257C")) {
            val retryUrl = normalizarParametroPDaUrl(doc.location())
            Log.d(QR_PARSER_TAG, "Retry SP: corrigindo dupla codificacao de p e consultando novamente.")
            doc = conexao(retryUrl).get()
            Log.d(
                QR_PARSER_TAG,
                "Resposta QR retry: location=${doc.location()} title=${doc.title().take(120)}"
            )
        }

        val hostConsulta = runCatching { java.net.URI(doc.location()).host?.lowercase(Locale.ROOT) }
            .getOrNull()
            .orEmpty()
        val pareceRespostaVaziaSp = hostConsulta.contains("nfce.fazenda.sp.gov.br") &&
            doc.select("#tabResult, .txtTit, .txtProd, .totalNumb, #linhaTotal").isEmpty()
        if (pareceRespostaVaziaSp) {
            Log.d(QR_PARSER_TAG, "SP bootstrap: tentando consulta com sessao/cookies.")
            val baseUrlSp = "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx"
            val bootstrap = conexao(baseUrlSp).execute()
            val cookies = bootstrap.cookies()
            val retrySessionUrl = normalizarParametroPDaUrl(normalizarRotaSpNfce(urlNormalizada))
            doc = conexao(retrySessionUrl)
                .header("Referer", baseUrlSp)
                .cookies(cookies)
                .get()
            Log.d(
                QR_PARSER_TAG,
                "Resposta QR sessao: location=${doc.location()} title=${doc.title().take(120)} hasTabResult=${doc.select("#tabResult").isNotEmpty()}"
            )
        }

        // PadrÃµes mais comuns por estado (SP / MG).
        val porLayout = extrairNotaPorLayout(doc)
        if (porLayout != null && (porLayout.valorTotal != null || porLayout.dataCompra != null)) {
            Log.d(
                QR_PARSER_TAG,
                "Resultado por layout: valor=${porLayout.valorTotal} data=${porLayout.dataCompra} itens=${porLayout.descricaoItens} estabelecimento=${porLayout.nomeEstabelecimento} endereco=${porLayout.enderecoEstabelecimento}"
            )
            return@runCatching porLayout
        }
        Log.d(QR_PARSER_TAG, "Layout nÃ£o resolveu. Aplicando fallback por regex.")

        val valorTotalPelaUrl = extrairValorVnfDaUrl(urlNormalizada)
        val resumo = extrairResumoFinanceiroNota(doc)
        val texto = doc.text()

        // Tentativa 1: seletores comuns da pÃ¡gina da NFC-e/SEFAZ.
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
        val valorTotal = valorMatch?.let { parseValorMonetarioFlexivel(it) }
        val valorTotalFinal = when {
            resumo.valorFinal != null -> resumo.valorFinal
            valorTotalPelaUrl != null && valorTotal != null -> maxOf(valorTotalPelaUrl, valorTotal)
            valorTotalPelaUrl != null -> valorTotalPelaUrl
            resumo.valorTotal != null -> resumo.valorTotal
            else -> valorTotal
        }
        val dataCompra = candidatosData.firstNotNullOfOrNull { bloco ->
            dataRegex.find(bloco)?.value
        } ?: dataRegex.find(texto)?.value

        val descricaoItens = extrairItensGenerico(doc)
        val estabelecimento = extrairDadosEstabelecimento(doc)
        Log.d(
            QR_PARSER_TAG,
            "Resultado fallback: valor=$valorTotalFinal bruto=${resumo.valorTotal} desconto=${resumo.valorDesconto} final=${resumo.valorFinal} qtdTotal=${resumo.quantidadeTotalItens} valorUrl=$valorTotalPelaUrl data=$dataCompra itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
        )
        if (valorTotalFinal == null && dataCompra == null && descricaoItens.isNullOrBlank()) {
            val bloqueioSp = detectarBloqueioConsultaSp(doc)
            if (bloqueioSp != null) {
                Log.w(QR_PARSER_TAG, "Consulta SP sem dados estruturados: motivo=$bloqueioSp")
                NotaQrInfo(
                    valorTotal = null,
                    dataCompra = null,
                    descricaoItens = MENSAGEM_BLOQUEIO_SP,
                    nomeEstabelecimento = estabelecimento.first ?: "SEFAZ-SP",
                    enderecoEstabelecimento = estabelecimento.second
                )
            } else {
                null
            }
        } else {
            NotaQrInfo(
                valorTotal = valorTotalFinal,
                dataCompra = dataCompra,
                descricaoItens = descricaoItens,
                nomeEstabelecimento = estabelecimento.first,
                enderecoEstabelecimento = estabelecimento.second,
                valorBruto = resumo.valorTotal ?: valorTotal,
                valorDesconto = resumo.valorDesconto,
                valorFinalComDesconto = resumo.valorFinal ?: valorTotalFinal,
                quantidadeTotalItens = resumo.quantidadeTotalItens,
                formaPagamento = resumo.formaPagamento
            )
        }
    }.onFailure {
        Log.e(QR_PARSER_TAG, "Erro ao consultar QR: ${it.message}", it)
    }.getOrNull()
}

internal fun sanitizeQrUrlText(value: String): String {
    return value
        .replace("\uFEFF", "")
        .replace("\u200B", "")
        .replace("\u200C", "")
        .replace("\u200D", "")
        .replace("\u2060", "")
        .replace('\u00A0', ' ')
        .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
        .trim()
}

private fun normalizarUrlQr(url: String): String {
    val urlLimpa = sanitizeQrUrlText(url)
        .replace(Regex("^[^A-Za-z0-9]+(?=https?://)", RegexOption.IGNORE_CASE), "")
        .let { cleaned ->
            if (cleaned.startsWith("www.", ignoreCase = true)) "https://$cleaned" else cleaned
        }
    val uri = runCatching { Uri.parse(urlLimpa) }.getOrNull() ?: return urlLimpa
    val host = uri.host.orEmpty()
    if (host.isBlank()) return urlLimpa

    val httpsUrl = if (uri.scheme.equals("http", ignoreCase = true)) {
        uri.buildUpon().scheme("https").build().toString()
    } else {
        urlLimpa
    }

    val urlComRotaNormalizada = normalizarRotaSpNfce(httpsUrl)
    val urlComQueryNormalizada = normalizarParametroPDaUrl(urlComRotaNormalizada)
    val urlFinal = urlComQueryNormalizada
    if (urlComRotaNormalizada != urlComQueryNormalizada) {
        Log.d(QR_PARSER_TAG, "URL QR ajustada (query p codificada): host=$host")
    }
    if (httpsUrl != urlComRotaNormalizada) {
        Log.d(QR_PARSER_TAG, "URL QR ajustada para endpoint canÃ´nico SP.")
    } else if (uri.scheme.equals("http", ignoreCase = true)) {
        Log.d(QR_PARSER_TAG, "URL QR normalizada para HTTPS: host=$host")
    }
    return urlFinal
}

private fun normalizarParametroPDaUrl(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    val isSpHost = host.contains("nfce.fazenda.sp.gov.br")
    val query = uri.encodedQuery ?: return url
    if (!query.contains("p=", ignoreCase = true)) return url

    val queryAjustada = query.split("&").joinToString("&") { parte ->
        val idx = parte.indexOf('=')
        if (idx <= 0) return@joinToString parte
        val chave = parte.substring(0, idx)
        val valor = parte.substring(idx + 1)
        if (chave.equals("p", ignoreCase = true)) {
            val valorAjustado = if (isSpHost) {
                valor.replace("%257C", "|", ignoreCase = true)
                    .replace("%7C", "|", ignoreCase = true)
            } else {
                valor.replace("|", "%7C")
            }
            "$chave=$valorAjustado"
        } else {
            parte
        }
    }

    if (queryAjustada == query) return url
    return uri.buildUpon().encodedQuery(queryAjustada).build().toString()
}

private fun normalizarRotaSpNfce(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    if (!host.contains("nfce.fazenda.sp.gov.br")) return url

    val query = uri.encodedQuery ?: return url
    if (!query.contains("p=", ignoreCase = true)) return url

    val pathAtual = uri.encodedPath.orEmpty()
    if (pathAtual.contains("/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx", ignoreCase = true)) {
        return url
    }

    val urlCanonica = uri.buildUpon()
        .encodedPath("/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx")
        .encodedQuery(query)
        .build()
        .toString()
    Log.d(QR_PARSER_TAG, "URL QR SP ajustada para rota canÃ´nica de consulta.")
    return urlCanonica
}

internal fun parseNotaHtmlForTest(html: String, url: String = "https://www.fazenda.sp.gov.br"): NotaQrInfo? {
    val doc = Jsoup.parse(html, url)
    val porLayout = extrairNotaPorLayout(doc)
    if (porLayout != null && (porLayout.valorTotal != null || porLayout.dataCompra != null)) {
        return porLayout
    }

    val valorTotalPelaUrl = extrairValorVnfDaUrl(url)
    val resumo = extrairResumoFinanceiroNota(doc)
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
    val valorTotal = valorMatch?.let { parseValorMonetarioFlexivel(it) }
    val valorTotalFinal = when {
        resumo.valorFinal != null -> resumo.valorFinal
        valorTotalPelaUrl != null && valorTotal != null -> maxOf(valorTotalPelaUrl, valorTotal)
        valorTotalPelaUrl != null -> valorTotalPelaUrl
        resumo.valorTotal != null -> resumo.valorTotal
        else -> valorTotal
    }
    val dataCompra = candidatosData.firstNotNullOfOrNull { bloco ->
        dataRegex.find(bloco)?.value
    } ?: dataRegex.find(texto)?.value

    val descricaoItens = extrairItensGenerico(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    if (valorTotalFinal == null && dataCompra == null && descricaoItens.isNullOrBlank()) {
        val bloqueioSp = detectarBloqueioConsultaSp(doc)
        if (bloqueioSp != null) {
            return NotaQrInfo(
                valorTotal = null,
                dataCompra = null,
                descricaoItens = MENSAGEM_BLOQUEIO_SP,
                nomeEstabelecimento = estabelecimento.first ?: "SEFAZ-SP",
                enderecoEstabelecimento = estabelecimento.second
            )
        }
        return null
    }
    return NotaQrInfo(
        valorTotal = valorTotalFinal,
        dataCompra = dataCompra,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second,
        valorBruto = resumo.valorTotal ?: valorTotal,
        valorDesconto = resumo.valorDesconto,
        valorFinalComDesconto = resumo.valorFinal ?: valorTotalFinal,
        quantidadeTotalItens = resumo.quantidadeTotalItens,
        formaPagamento = resumo.formaPagamento
    )
}

private fun detectarBloqueioConsultaSp(doc: Document): String? {
    val host = runCatching { java.net.URI(doc.location()).host?.lowercase(Locale.ROOT) }
        .getOrNull()
        .orEmpty()
    if (!host.contains("nfce.fazenda.sp.gov.br")) return null

    val temDadosNota = doc.select(
        "#tabResult, .txtTit, .txtProd, .totalNumb, #linhaTotal, #totalNota, #dataEmissao"
    ).isNotEmpty()
    if (temDadosNota) return null

    val title = doc.title().lowercase(Locale.ROOT)
    val texto = doc.text().lowercase(Locale.ROOT)
    val html = doc.html().lowercase(Locale.ROOT)

    val indiciosCaptcha = listOf("captcha", "hcaptcha", "recaptcha")
    if (indiciosCaptcha.any { html.contains(it) || texto.contains(it) }) return "captcha"

    val indiciosBloqueio = listOf(
        "acesso negado",
        "requisicao bloqueada",
        "sessao expirada",
        "temporariamente indisponivel",
        "validacao"
    )
    if (indiciosBloqueio.any { texto.contains(it) || html.contains(it) }) return "bloqueio"

    val paginaBaseConsulta =
        title.contains("consulta nfc-e qr code") ||
            texto.contains("secretaria da fazenda - governo do estado de sao paulo") ||
            texto.contains("secretaria da fazenda - governo do estado de sÃ£o paulo")
    return if (paginaBaseConsulta) "pagina-base" else null
}

private fun extrairNotaPorLayout(doc: Document): NotaQrInfo? {
    val host = runCatching { java.net.URI(doc.location()).host?.lowercase(Locale.ROOT) }.getOrNull().orEmpty()
    return when {
        host.contains("nfse.gov.br") -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: NFS-e nacional (host=$host)")
            extrairNotaLayoutNfse(doc)
        }

        host.contains("dfe.ms.gov.br") || host.contains("sefaz.ms.gov.br") -> {
            Log.d(QR_PARSER_TAG, "Parser selecionado: MS (host=$host) - tentando layout nacional")
            extrairNotaLayoutNacional(doc)
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
            Log.d(QR_PARSER_TAG, "Layout nacional/fallback (host=$host)")
            extrairNotaLayoutNacional(doc)
        }
    }
}

private fun extrairNotaLayoutSp(doc: Document): NotaQrInfo? {
    val valorLegacy = sequenceOf(
        doc.select("#totalNota").text(),
        doc.select(".totalNumb").text(),
        doc.select("#linhaTotal").text(),
        doc.select(".txtMax").text(),
        doc.select("td:matchesOwn((?i)valor total|total da nota)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|total\\s*da\\s*nota"))
    val resumo = extrairResumoFinanceiroNota(doc)
    val valorFinalPreferencial = resumo.valorFinal ?: valorLegacy ?: resumo.valorTotal

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
        "SP parser => valor=$valorFinalPreferencial bruto=${resumo.valorTotal} desconto=${resumo.valorDesconto} final=${resumo.valorFinal} qtdTotal=${resumo.quantidadeTotalItens} data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valorFinalPreferencial == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valorFinalPreferencial,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second,
        valorBruto = resumo.valorTotal ?: valorLegacy,
        valorDesconto = resumo.valorDesconto,
        valorFinalComDesconto = resumo.valorFinal ?: valorFinalPreferencial,
        quantidadeTotalItens = resumo.quantidadeTotalItens,
        formaPagamento = resumo.formaPagamento
    )
}

private fun extrairNotaLayoutMg(doc: Document): NotaQrInfo? {
    val valorLegacy = sequenceOf(
        doc.select("#valorTotal").text(),
        doc.select(".valorTotal").text(),
        doc.select(".dadosNf").text(),
        doc.select(".nfce").text(),
        doc.select("td:matchesOwn((?i)valor total|valor a pagar|total)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|valor\\s*a\\s*pagar|total"))
    val resumo = extrairResumoFinanceiroNota(doc)
    val valorFinalPreferencial = resumo.valorFinal ?: valorLegacy ?: resumo.valorTotal

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
        "MG parser => valor=$valorFinalPreferencial bruto=${resumo.valorTotal} desconto=${resumo.valorDesconto} final=${resumo.valorFinal} qtdTotal=${resumo.quantidadeTotalItens} data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valorFinalPreferencial == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valorFinalPreferencial,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second,
        valorBruto = resumo.valorTotal ?: valorLegacy,
        valorDesconto = resumo.valorDesconto,
        valorFinalComDesconto = resumo.valorFinal ?: valorFinalPreferencial,
        quantidadeTotalItens = resumo.quantidadeTotalItens,
        formaPagamento = resumo.formaPagamento
    )
}

private fun extrairNotaLayoutNacional(doc: Document): NotaQrInfo? {
    val valorLegacy = sequenceOf(
        doc.select("#totalNota").text(),
        doc.select("#valorTotal").text(),
        doc.select(".totalNumb").text(),
        doc.select(".valorTotal").text(),
        doc.select(".vNF").text(),
        doc.select(".txtMax").text(),
        doc.select("td:matchesOwn((?i)valor\\s*a\\s*pagar|valor\\s*total|total\\s*da\\s*nota|v\\.?\\s*nf)").text()
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }
        ?: encontrarValorProximoAoRotulo(
            doc,
            Regex("(?i)valor\\s*a\\s*pagar|valor\\s*total|total\\s*da\\s*nota|v\\.?\\s*nf|total")
        )
    val resumo = extrairResumoFinanceiroNota(doc)
    val valorFinalPreferencial = resumo.valorFinal ?: valorLegacy ?: resumo.valorTotal

    val data = sequenceOf(
        doc.select("#dataEmissao").text(),
        doc.select(".dataEmissao").text(),
        doc.select(".dhEmi").text(),
        doc.select(".txtCenter").text(),
        doc.select(".dadosNf").text(),
        doc.select("td:matchesOwn((?i)data\\s*de\\s*emissao|emissao|data)").text()
    ).map { extrairPrimeiraData(it) }.firstOrNull { it != null }
        ?: encontrarDataProximaAoRotulo(doc, Regex("(?i)data\\s*de\\s*emissao|emissao|data"))

    val descricaoItens = extrairItensGenerico(doc)
    val estabelecimento = extrairDadosEstabelecimento(doc)
    Log.d(
        QR_PARSER_TAG,
        "Nacional parser => valor=$valorFinalPreferencial bruto=${resumo.valorTotal} desconto=${resumo.valorDesconto} final=${resumo.valorFinal} qtdTotal=${resumo.quantidadeTotalItens} data=$data itens=$descricaoItens estabelecimento=${estabelecimento.first} endereco=${estabelecimento.second}"
    )
    return if (valorFinalPreferencial == null && data == null && descricaoItens.isNullOrBlank()) null else NotaQrInfo(
        valorTotal = valorFinalPreferencial,
        dataCompra = data,
        descricaoItens = descricaoItens,
        nomeEstabelecimento = estabelecimento.first,
        enderecoEstabelecimento = estabelecimento.second,
        valorBruto = resumo.valorTotal ?: valorLegacy,
        valorDesconto = resumo.valorDesconto,
        valorFinalComDesconto = resumo.valorFinal ?: valorFinalPreferencial,
        quantidadeTotalItens = resumo.quantidadeTotalItens,
        formaPagamento = resumo.formaPagamento
    )
}

private fun extrairNotaLayoutNfse(doc: Document): NotaQrInfo? {
    val texto = Parser.unescapeEntities(doc.text(), false).replace(Regex("\\s+"), " ").trim()
    val valor = sequenceOf(
        doc.select("#ValorServico").text(),
        doc.select("#ValorLiquidoNfse").text(),
        doc.select("#ValorNfse").text(),
        doc.select("td:matchesOwn((?i)valor\\s*do\\s*servi[cÃ§]o|valor\\s*total|valor\\s*lÃ­quido)").text(),
        texto
    ).map { extrairPrimeiroValorMonetario(it) }.firstOrNull { it != null }

    val data = sequenceOf(
        doc.select("#DataEmissao").text(),
        doc.select("td:matchesOwn((?i)data\\s*de\\s*emiss[aÃ£]o|compet[Ãªe]ncia|emiss[aÃ£]o)").text(),
        texto
    ).map { extrairPrimeiraData(it) }.firstOrNull { it != null }

    val prestador = sequenceOf(
        doc.select("#PrestadorServico").text(),
        doc.select("#NomePrestador").text(),
        doc.select("td:matchesOwn((?i)prestador|emitente)").text()
    ).map { it.trim() }.firstOrNull { it.isNotBlank() }

    val itens = extrairItensGenerico(doc)
    val exigeValidacaoHumana = doc.select("script[src*='hcaptcha'], .h-captcha, iframe[src*='hcaptcha']").isNotEmpty() ||
        doc.select("#ChaveAcesso, input[name='ChaveAcesso']").isNotEmpty()

    if (valor == null && data == null && itens.isNullOrBlank()) {
        if (exigeValidacaoHumana) {
            val descricao = "NFS-e detectada. A consulta publica exige validacao manual (captcha)."
            Log.d(QR_PARSER_TAG, "NFS-e sem dados estruturados: exige validacao manual.")
            return NotaQrInfo(
                valorTotal = null,
                dataCompra = null,
                descricaoItens = descricao,
                nomeEstabelecimento = prestador ?: "NFS-e",
                enderecoEstabelecimento = null
            )
        }
        return null
    }

    return NotaQrInfo(
        valorTotal = valor,
        dataCompra = data,
        descricaoItens = itens ?: "NFS-e detectada",
        nomeEstabelecimento = prestador,
        enderecoEstabelecimento = null
    )
}

private fun extrairItensSp(doc: Document): String? {
    val porTabelaResumo = extrairItensSpPorTitulosEValores(doc)
    if (!porTabelaResumo.isNullOrBlank()) return porTabelaResumo
    return extrairItensPorLinhas(
        doc = doc,
        seletoresLinha = listOf("#tabResult tr", ".txtTit", ".txtProd", "table tr")
    )
}

private fun extrairItensSpPorTitulosEValores(doc: Document): String? {
    val nomeElements = doc.select("#tabResult .txtTit, .txtTit")
    val nomes = nomeElements
        .mapNotNull { limparNomeProduto(it.text()) }
    if (nomes.isEmpty()) return null

    val valores = doc.select("#tabResult .valor, #tabResult .valorItem, .valor")
        .mapNotNull { extrairPrimeiroValorMonetario(it.text()) }
        .toList()
    if (valores.isEmpty()) return nomes.take(4).joinToString(" + ")

    val quantidades = nomeElements.map { el ->
        extrairQuantidadeItemNoTexto(el.parent()?.text().orEmpty())
            ?: extrairQuantidadeItemNoTexto(el.nextElementSibling()?.text().orEmpty())
            ?: extrairQuantidadeItemNoTexto(el.parent()?.nextElementSibling()?.text().orEmpty())
            ?: 1
    }

    val limite = minOf(nomes.size, valores.size, quantidades.size, 8)
    if (limite == 0) return null
    val itens = (0 until limite).map { i ->
        val qtd = quantidades.getOrNull(i)?.coerceAtLeast(1) ?: 1
        val sufixoQtd = if (qtd > 1) " x$qtd" else ""
        "${nomes[i].take(60)}$sufixoQtd (${String.format(Locale.US, "%.2f", valores[i])})"
    }
    return normalizarItensDescricao(itens)
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
        lower.contains("vl. total") ||
        lower.contains("vl total") ||
        lower.startsWith(". total") ||
        lower.startsWith("total ") ||
        lower.startsWith("valor pago") ||
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
        Regex("(?i)\\b(c[oÃ³]digo|cod\\.?|qtde|qtd|un|vl\\.?\\s*total|valor\\s*total)\\b"),
        limit = 2
    ).firstOrNull().orEmpty()
    val nomeBase = texto
        .substringBefore("(")
        .let { if (cortePorRotulo.isNotBlank()) cortePorRotulo else it }
        .replace(Regex("(?i)\\b(c[oÃ³]digo|cod\\.?|qtde|qtd|un|vl\\.?|r\\$)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (nomeBase.isBlank()) return null
    return "${nomeBase.take(60)} (${String.format(Locale.US, "%.2f", valor)})"
}

private fun extrairItensPorTitulosEValores(doc: Document): String? {
    val nomes = doc.select(
        ".txtTit, .txtProd, .descricao, .xProd, .descProd, .produto, [id*=xProd], [class*=xProd]"
    )
        .mapNotNull { limparNomeProduto(it.text()) }
        .distinct()
    if (nomes.isEmpty()) return null

    val valores = doc.select(
        ".valor, .valorItem, .vProd, .preco, .valorProduto, .RxValor, [id*=vProd], [class*=vProd], [class*=valor]"
    )
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
        lower.startsWith("cÃ³digo")
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
    val nomeAntesCnpj = doc.select("*")
        .firstOrNull { it.text().contains("CNPJ", ignoreCase = true) }
        ?.previousElementSibling()
        ?.text()
        ?.let { limparNomeEstabelecimentoExtraido(it) }

    val nomeAzul = doc.select("[style*=color], .txtTit, .txtCenter")
        .mapNotNull { limparNomeEstabelecimentoExtraido(it.text()) }
        .firstOrNull { texto ->
            texto.length > 6 &&
                !texto.contains("CNPJ", ignoreCase = true) &&
                !texto.contains("DOCUMENTO AUXILIAR", ignoreCase = true) &&
                !texto.contains("DANFE", ignoreCase = true)
        }

    val nome = sequenceOf(
        nomeAntesCnpj,
        nomeAzul,
        doc.select(".txtTopo").firstOrNull()?.text(),
        doc.select(".txtTit").firstOrNull()?.text(),
        doc.select("h1").firstOrNull()?.text(),
        doc.select("h2").firstOrNull()?.text()
    ).mapNotNull { it?.let { t -> limparNomeEstabelecimentoExtraido(t) } }
        .firstOrNull { texto ->
            texto.length > 3 &&
                !texto.contains("nfc-e", ignoreCase = true) &&
                !texto.contains("danfe", ignoreCase = true) &&
                !texto.contains("consulta", ignoreCase = true) &&
                !texto.contains("cnpj", ignoreCase = true)
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

private fun limparNomeEstabelecimentoExtraido(textoOriginal: String?): String? {
    val texto = textoOriginal
        ?.let { Parser.unescapeEntities(it, false) }
        ?.replace(Regex("\\s+"), " ")
        ?.replace(Regex("(?i)\\bcnpj\\b.*$"), "")
        ?.trim()
        .orEmpty()
    if (texto.isBlank()) return null
    if (texto.length < 4) return null
    return texto
}

private fun extrairResumoFinanceiroNota(doc: Document): ResumoNotaFinanceiro {
    val textoCompleto = Parser.unescapeEntities(doc.text(), false)
        .replace(Regex("\\s+"), " ")
        .trim()

    val valorTotalPorRegex = Regex("(?i)valor\\s*total\\s*R?\\$?\\s*:?\\s*([\\d\\.,]+)")
        .find(textoCompleto)
        ?.groupValues
        ?.getOrNull(1)
        ?.let { parseValorMonetarioFlexivel(it) }

    val valorDescontoPorRegex = Regex("(?i)descontos?\\s*R?\\$?\\s*:?\\s*([\\d\\.,]+)")
        .find(textoCompleto)
        ?.groupValues
        ?.getOrNull(1)
        ?.let { parseValorMonetarioFlexivel(it) }

    val valorFinalPorRegex = Regex("(?i)(valor\\s*a\\s*pagar|valor\\s*pago)\\s*R?\\$?\\s*:?\\s*([\\d\\.,]+)")
        .find(textoCompleto)
        ?.groupValues
        ?.getOrNull(2)
        ?.let { parseValorMonetarioFlexivel(it) }

    val valorTotal = sequenceOf(
        valorTotalPorRegex,
        encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*total|total\\s*da\\s*nota|v\\.?\\s*nf")),
        encontrarValorProximoAoRotulo(doc, Regex("(?i)vl\\.?\\s*total"))
    ).firstOrNull { it != null }

    val valorDesconto = sequenceOf(
        valorDescontoPorRegex,
        encontrarValorProximoAoRotulo(doc, Regex("(?i)desconto|descontos")),
        encontrarValorProximoAoRotulo(doc, Regex("(?i)desc\\.?"))
    ).firstOrNull { it != null }

    val valorFinal = sequenceOf(
        valorFinalPorRegex,
        encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*a\\s*pagar|valor\\s*liquido|valor\\s*l[iÃ­]quido")),
        encontrarValorProximoAoRotulo(doc, Regex("(?i)valor\\s*final|total\\s*a\\s*pagar"))
    ).firstOrNull { it != null }

    val quantidadeTotalItens = extrairQuantidadeTotalItens(doc)
    val formaPagamento = extrairFormaPagamentoNota(doc)

    return ResumoNotaFinanceiro(
        valorTotal = valorTotal,
        valorDesconto = valorDesconto,
        valorFinal = valorFinal,
        quantidadeTotalItens = quantidadeTotalItens,
        formaPagamento = formaPagamento
    )
}

private fun extrairFormaPagamentoNota(doc: Document): String? {
    val linhasTabela = doc.select("tr")
    linhasTabela.forEach { tr ->
        val cols = tr.select("td, th")
        if (cols.size < 2) return@forEach
        val primeira = Parser.unescapeEntities(cols[0].text(), false).replace(Regex("\\s+"), " ").trim()
        val segunda = Parser.unescapeEntities(cols[1].text(), false).replace(Regex("\\s+"), " ").trim()
        val segundaTemValor = extrairPrimeiroValorMonetario(segunda) != null
        val metodoTabela = normalizarFormaPagamento(primeira)
        if (segundaTemValor && ehFormaPagamentoValida(metodoTabela)) return metodoTabela
    }

    val textoNormalizado = Parser.unescapeEntities(doc.text(), false).replace(Regex("\\s+"), " ").trim()
    val regexLinha = Regex("(?i)forma\\s*de\\s*pagamento\\s*:?\\s*([A-Za-z0-9\\u00C0-\\u017F\\s/.-]{3,60})")
    val porLinha = normalizarFormaPagamento(regexLinha.find(textoNormalizado)?.groupValues?.getOrNull(1)?.trim())
    if (ehFormaPagamentoValida(porLinha)) return porLinha

    val celulas = doc.select("td, th, span, div").take(1200)
    celulas.forEachIndexed { idx, el ->
        val atual = Parser.unescapeEntities(el.text(), false).replace(Regex("\\s+"), " ").trim()
        if (!atual.contains("forma de pagamento", ignoreCase = true)) return@forEachIndexed
        val prox = listOfNotNull(
            celulas.getOrNull(idx + 1)?.text(),
            el.nextElementSibling()?.text(),
            el.parent()?.children()?.getOrNull(1)?.text()
        ).map { Parser.unescapeEntities(it, false).replace(Regex("\\s+"), " ").trim() }
            .mapNotNull { candidato -> normalizarFormaPagamento(candidato) }
            .firstOrNull { candidato -> ehFormaPagamentoValida(candidato) }
        if (!prox.isNullOrBlank()) return prox
    }
    return null
}

private fun normalizarFormaPagamento(texto: String?): String? {
    val bruto = texto?.trim().orEmpty()
    if (bruto.isBlank()) return null
    val semRotulos = bruto
        .replace(Regex("(?i)^forma\\s*de\\s*pagamento\\s*:?\\s*"), "")
        .replace(Regex("(?i)^valor\\s*pago\\s*r\\$\\s*"), "")
        .replace(Regex("\\d+[\\.,]\\d{2}"), "")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '-', ':', ';')
    return semRotulos.takeIf { it.isNotBlank() }
}

private fun ehFormaPagamentoValida(texto: String?): Boolean {
    val valor = texto?.trim().orEmpty()
    if (valor.isBlank()) return false
    val lower = valor.lowercase(Locale.ROOT)
    if (lower.contains("forma de pagamento")) return false
    if (lower.contains("valor pago")) return false
    if (lower == "r$" || lower == "valor pago r$") return false
    if (valor.length > 40) return false
    return Regex("(?i)\\b(cart[aï¿½]o|cr[eï¿½]dito|d[eï¿½]bito|pix|dinheiro|boleto|transfer[eï¿½]ncia|cheque|vale|tef|cr[eï¿½]dito\\s*loja)\\b")
        .containsMatchIn(valor)
}
private fun extrairQuantidadeTotalItens(doc: Document): Int? {
    val regexQtd = Regex("(?i)qtd\\.?\\s*total\\s*de\\s*itens\\D{0,20}(\\d+)")
    val candidatos = doc.select("*").take(800).map { it.text() } + doc.text()
    candidatos.forEach { bloco ->
        val valor = regexQtd.find(bloco)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (valor != null && valor > 0) return valor
    }
    return null
}

private fun extrairQuantidadeItemNoTexto(texto: String): Int? {
    if (texto.isBlank()) return null
    val matchQtd = Regex("(?i)(?:qtd|qtde)\\.?\\s*:?\\s*(\\d+)").find(texto)
    val porQtd = matchQtd?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (porQtd != null && porQtd > 0) return porQtd

    val matchMultiplicacao = Regex("(?i)\\b(\\d+)\\s*(?:x|un|und)\\b").find(texto)
    val porMultiplicacao = matchMultiplicacao?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (porMultiplicacao != null && porMultiplicacao > 0) return porMultiplicacao

    return null
}

private fun extrairPrimeiroValorMonetario(texto: String): Double? {
    val valor = Regex("\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+[\\.,]\\d{2}")
        .find(texto)
        ?.value
        ?: return null
    return parseValorMonetarioFlexivel(valor)
}

fun montarLocalNota(estabelecimento: String, endereco: String): String {
    val partes = mutableListOf<String>()
    if (estabelecimento.isNotBlank()) partes += estabelecimento
    if (endereco.isNotBlank()) partes += endereco
    return partes.joinToString(" - ")
}

fun montarDescricaoItensNota(
    total: Double?,
    itens: String?,
    desconto: Double? = null,
    valorFinal: Double? = null,
    quantidadeTotalItens: Int? = null
): String {
    val linhas = mutableListOf<String>()
    if (!itens.isNullOrBlank()) {
        val itensFormatados = itens
            .removePrefix("Itens:")
            .split("+")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        linhas += itensFormatados
    }
    if (total != null) {
        linhas += "Total: R$ ${String.format(Locale.US, "%.2f", total)}"
    }
    if (desconto != null && desconto > 0.0) {
        linhas += "Desconto: R$ ${String.format(Locale.US, "%.2f", desconto)}"
    }
    if (valorFinal != null && valorFinal > 0.0) {
        linhas += "Valor final: R$ ${String.format(Locale.US, "%.2f", valorFinal)}"
    }
    if (quantidadeTotalItens != null && quantidadeTotalItens > 0) {
        linhas += "Quantidade total de itens: $quantidadeTotalItens"
    }
    if (linhas.isNotEmpty()) {
        return linhas.joinToString("\n")
    }
    return if (total != null) {
        "Total: R$ ${String.format(Locale.US, "%.2f", total)}"
    } else {
        "Servico da nota"
    }
}

fun extrairItensDaDescricaoQr(descricao: String?): List<ItemDetectado> {
    if (descricao.isNullOrBlank()) return emptyList()
    val blocoItens = descricao.substringAfter("Itens:", descricao).trim()
    if (blocoItens.isBlank()) return emptyList()
    val partes = blocoItens.split("+").map { it.trim() }.filter { it.isNotBlank() }
    val regexItem = Regex("(.+?)\\s*(?:x\\s*(\\d+))?\\s*\\((\\d+[\\.,]\\d{2})\\)")
    return partes.mapNotNull { parte ->
        val match = regexItem.find(parte) ?: return@mapNotNull null
        val nome = match.groupValues[1].trim()
        val quantidade = match.groupValues.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val valor = match.groupValues[3].replace(",", ".").toDoubleOrNull() ?: 0.0
        if (nome.isBlank()) return@mapNotNull null
        ItemDetectado(
            id = UUID.randomUUID().toString(),
            nome = nome,
            tipo = detectarTipoPeloTexto(nome),
            valor = valor,
            quantidade = quantidade
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
    return parseValorMonetarioFlexivel(ultimo)
}

private fun parseValorMonetarioFlexivel(raw: String): Double? {
    val valor = raw
        .replace("R$", "", ignoreCase = true)
        .replace(Regex("\\s+"), "")
        .trim()
    if (valor.isBlank()) return null

    val temVirgula = valor.contains(',')
    val temPonto = valor.contains('.')

    val normalizado = when {
        temVirgula && temPonto -> {
            val ultimoPonto = valor.lastIndexOf('.')
            val ultimaVirgula = valor.lastIndexOf(',')
            if (ultimaVirgula > ultimoPonto) {
                // Ex.: 1.234,56 -> 1234.56
                valor.replace(".", "").replace(",", ".")
            } else {
                // Ex.: 1,234.56 -> 1234.56
                valor.replace(",", "")
            }
        }
        temVirgula -> {
            if (Regex(",\\d{2}$").containsMatchIn(valor)) {
                valor.replace(",", ".")
            } else {
                valor.replace(",", "")
            }
        }
        temPonto -> {
            if (Regex("\\.\\d{2}$").containsMatchIn(valor)) {
                valor
            } else {
                valor.replace(".", "")
            }
        }
        else -> valor
    }

    return normalizado.toDoubleOrNull()
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
                <div id="dataEmissao">EmissÃ£o: 02/02/2026 13:30:00</div>
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




