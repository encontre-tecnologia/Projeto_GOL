package br.com.gui.carlembrete

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun OperationalReportOptionsDialog(
    bg: Color,
    textPrimary: Color,
    cardBorder: Color,
    accentBlue: Color,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkDialog = bg.luminance() < 0.5f
    val dialogContainer = if (isDarkDialog) Color(0xFF070F1D) else Color(0xFFFFFFFF)
    val iconContainer = accentBlue.copy(alpha = if (isDarkDialog) 0.24f else 0.14f)
    val closeContainer = if (isDarkDialog) Color.Transparent else Color(0xFFF8FAFC)
    val closeBorder = if (isDarkDialog) cardBorder.copy(alpha = 0.55f) else cardBorder.copy(alpha = 0.28f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = dialogContainer),
            border = BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(iconContainer, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    tr("Exportar em PDF", "Export as PDF"),
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Exportar PDF", "Export PDF"), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, closeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(closeContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(tr("Fechar", "Close"))
                    }
                }
            }
        }
    }
}

internal fun generateOperationalReportPdf(
    context: Context,
    feature: OperationalFeature,
    records: List<OperationalRecord>,
    vehiclesCount: Int,
    importedRecordsCount: Int,
    realCostPerKm: Double,
    routeSuggestedCost: Double
): File? = runCatching {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageIndex = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
    var canvas = page.canvas
    val marginX = 36f
    val contentWidth = pageWidth - marginX * 2
    var y = 108f
    val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    val accentColor = android.graphics.Color.parseColor("#2563EB")
    val cardBg = android.graphics.Color.parseColor("#F8FAFC")
    val cardBorder = android.graphics.Color.parseColor("#E2E8F0")
    val textColor = android.graphics.Color.parseColor("#0F172A")
    val mutedColor = android.graphics.Color.parseColor("#475569")
    val successColor = android.graphics.Color.parseColor("#059669")
    val dangerColor = android.graphics.Color.parseColor("#DC2626")

    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val headerInfoPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#BFDBFE")
        textSize = 11f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val sectionPaint = Paint().apply {
        color = accentColor
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val labelPaint = Paint().apply {
        color = mutedColor
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val valuePaint = Paint().apply {
        color = textColor
        textSize = 12f
        isAntiAlias = true
    }
    val valueBoldPaint = Paint(valuePaint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val smallPaint = Paint().apply {
        color = mutedColor
        textSize = 10f
        isAntiAlias = true
    }
    val tableHeaderPaint = Paint().apply { color = accentColor }
    val tableHeaderTextPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val cardPaint = Paint().apply { color = cardBg; isAntiAlias = true }
    val cardBorderPaint = Paint().apply {
        color = cardBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        isAntiAlias = true
    }
    val accentPaint = Paint().apply { color = accentColor; isAntiAlias = true }
    val pageNumberPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 9f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun fit(text: String, maxChars: Int): String {
        val clean = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= maxChars) clean else clean.take(maxChars - 3) + "..."
    }

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 82f, Paint().apply { color = accentColor })
        canvas.drawText("RELATORIO OPERACIONAL", pageWidth / 2f, 42f, titlePaint)
        canvas.drawText("${operationalReportTitle(feature)} • $generatedAt", pageWidth / 2f, 64f, headerInfoPaint)
    }

    fun finishPage() {
        canvas.drawText("- $pageIndex -", pageWidth / 2f, pageHeight - 18f, pageNumberPaint)
        document.finishPage(page)
    }

    fun newPage() {
        finishPage()
        pageIndex++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        canvas = page.canvas
        y = 108f
        drawHeader()
    }

    fun ensureSpace(height: Float) {
        if (y + height > pageHeight - 46f) newPage()
    }

    fun drawSection(title: String) {
        ensureSpace(28f)
        canvas.drawRect(marginX, y - 13f, marginX + 4f, y + 3f, accentPaint)
        canvas.drawText(title, marginX + 10f, y, sectionPaint)
        y += 18f
    }

    fun drawMetric(label: String, value: String, left: Float, top: Float, width: Float, valueColor: Int = textColor) {
        val rect = android.graphics.RectF(left, top, left + width, top + 62f)
        canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
        canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
        canvas.drawText(label.uppercase(Locale.getDefault()), left + 12f, top + 22f, labelPaint)
        canvas.drawText(fit(value, 32), left + 12f, top + 44f, Paint(valueBoldPaint).apply { color = valueColor })
    }

    fun drawKeyValue(label: String, value: String, left: Float, baseline: Float, valueX: Float, maxChars: Int = 48) {
        canvas.drawText(label, left, baseline, labelPaint)
        canvas.drawText(fit(value, maxChars), valueX, baseline, valuePaint)
    }

    val finished = records.filter { it.kmEnd != null && it.kmEnd > it.kmStart }
    val routeRecords = records.filter { it.revenue != null }
    val drivers = if (feature == OperationalFeature.ROUTE_PROFITABILITY) loadOperationalDrivers(context) else emptyList()
    val bestDurability = finished.maxByOrNull { (it.kmEnd ?: 0) - it.kmStart }?.let { "${(it.kmEnd ?: 0) - it.kmStart} km" }
        ?: trNow("Aguardando KM final", "Waiting for final mileage")
    val lowestCost = finished.minByOrNull { costPerKm(it) }?.let { "${formatMoney(costPerKm(it))}/km" }
        ?: trNow("Aguardando KM", "Waiting mileage")
    val totalBalance = routeRecords.sumOf { routeProfit(it) }
    val currentMonthTag = SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date())
    val currentMonthRouteBalance = routeRecords
        .filter { SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date(it.createdAt)) == currentMonthTag }
        .sumOf { routeProfit(it) }
    val bestRoute = routeRecords.maxByOrNull { routeProfit(it) }
    val bestRouteValue = bestRoute?.let { "${formatMoney(routeProfit(it))} • ${formatPlainDecimal(routeMargin(it))}%" }
        ?: trNow("Sem rotas calculadas", "No calculated routes")

    drawHeader()
    drawSection("RESUMO")
    val metricGap = 10f
    val metricWidth = (contentWidth - metricGap) / 2f
    drawMetric("Registros", records.size.toString(), marginX, y, metricWidth)
    drawMetric("Veiculos", vehiclesCount.toString(), marginX + metricWidth + metricGap, y, metricWidth)
    y += 72f
    drawMetric("Importados", importedRecordsCount.toString(), marginX, y, metricWidth)
    if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
        val totalDriverCost = drivers.sumOf { it.salary + it.taxCost + it.defaultCost }
        drawMetric("Saldo total", formatMoney(totalBalance), marginX + metricWidth + metricGap, y, metricWidth, if (totalBalance >= 0) successColor else dangerColor)
        y += 72f
        drawMetric("Custo/km real", formatMoney(realCostPerKm), marginX, y, metricWidth)
        drawMetric("Sugestao rota", formatMoney(routeSuggestedCost), marginX + metricWidth + metricGap, y, metricWidth)
        y += 72f
        drawMetric("Motoristas", drivers.size.toString(), marginX, y, metricWidth)
        drawMetric("Custos motoristas", formatMoney(totalDriverCost), marginX + metricWidth + metricGap, y, metricWidth)
        y += 72f
        drawMetric("Saldo mes $currentMonthTag", formatMoney(currentMonthRouteBalance), marginX, y, metricWidth, if (currentMonthRouteBalance >= 0) successColor else dangerColor)
        drawMetric("Melhor lucro", bestRouteValue, marginX + metricWidth + metricGap, y, metricWidth, successColor)
    } else {
        drawMetric("Menor custo/km", lowestCost, marginX + metricWidth + metricGap, y, metricWidth)
        y += 72f
        drawMetric("Melhor durabilidade", bestDurability, marginX, y, contentWidth, successColor)
    }
    y += 88f

    if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
        drawSection("MOTORISTAS")
        if (drivers.isEmpty()) {
            canvas.drawText("Nenhum motorista cadastrado.", marginX, y + 6f, valuePaint)
            y += 24f
        } else {
            drivers.sortedBy { it.name.lowercase(Locale.ROOT) }.forEach { driver ->
                val linkedRoutes = records
                    .filter { it.driverId == driver.id }
                    .map { splitOperationalTitle(it.name).title }
                    .distinct()
                val height = 108f
                val bottomGap = 32f
                ensureSpace(height + bottomGap)
                val top = y
                canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardPaint)
                canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardBorderPaint)
                canvas.drawText(fit(driver.name, 38), marginX + 12f, top + 22f, valueBoldPaint)
                drawKeyValue("Codigo", driver.code.ifBlank { "-" }, marginX + 12f, top + 42f, marginX + 72f, 24)
                drawKeyValue("Salario", formatMoney(driver.salary), marginX + 190f, top + 42f, marginX + 250f, 24)
                drawKeyValue("Custos/imp.", formatMoney(driver.taxCost), marginX + 360f, top + 42f, marginX + 430f, 22)
                val linhas = linkedRoutes.joinToString(", ").ifBlank { "-" }
                drawKeyValue("Linhas", fit(linhas, 62), marginX + 12f, top + 66f, marginX + 72f, 62)
                y += height + bottomGap
            }
        }
        y += 30f
    }

    ensureSpace(46f)
    drawSection("HISTORICO")
    if (records.isEmpty()) {
        canvas.drawText("Nenhum registro encontrado para este relatorio.", marginX, y + 6f, valuePaint)
        y += 24f
    } else {
        records.sortedByDescending { it.createdAt }.forEachIndexed { index, record ->
            val height = if (feature == OperationalFeature.ROUTE_PROFITABILITY) 232f else 154f
            ensureSpace(height + 16f)
            val top = y
            canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardPaint)
            canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardBorderPaint)
            canvas.drawRect(marginX, top, marginX + 4f, top + height, accentPaint)
            val parts = splitOperationalTitle(record.name)
            canvas.drawText("${index + 1}. ${fit(parts.title, 58)}", marginX + 14f, top + 22f, valueBoldPaint)
            var rowY = top + 48f
            if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
                val recordDriver = drivers.firstOrNull { it.id == record.driverId }
                val routeRows = listOf(
                    "Receita" to (record.revenue?.let(::formatMoney) ?: "-"),
                    "Custo operacional" to formatMoney(record.cost),
                    "Custo motorista" to formatMoney(record.driverCost),
                    "Custo total" to formatMoney(routeTotalCost(record)),
                    "Imposto" to "${formatPlainDecimal(record.taxPercent ?: 0.0)}%",
                    "Resultado" to "${formatMoney(routeProfit(record))} • ${formatPlainDecimal(routeMargin(record))}%",
                    "Veiculo" to record.vehicle.ifBlank { "-" },
                    "Motorista" to record.driverName.ifBlank { "-" },
                    "Codigo" to (recordDriver?.code?.ifBlank { "-" } ?: "-"),
                    "Rota" to record.positionOrRoute.ifBlank { "-" },
                    "Distancia" to "${record.kmStart} km"
                )
                routeRows.forEach { (label, value) ->
                    drawKeyValue(label, value, marginX + 14f, rowY, marginX + 128f, 70)
                    rowY += 16f
                }
            } else {
                drawKeyValue("Marca/origem", record.brandOrClient.ifBlank { "-" }, marginX + 14f, rowY, marginX + 108f, 62)
                if (feature == OperationalFeature.TIRE_ROI) {
                    drawKeyValue("Quantidade", record.quantity.coerceAtLeast(1).toString(), marginX + 330f, rowY, marginX + 405f)
                }
                rowY += 20f
                drawKeyValue("Veiculo", record.vehicle.ifBlank { "-" }, marginX + 14f, rowY, marginX + 108f, 68)
                if (feature == OperationalFeature.TIRE_ROI) {
                    drawKeyValue("Data", record.recordDate.ifBlank { "-" }, marginX + 330f, rowY, marginX + 405f)
                }
                rowY += 20f
                drawKeyValue("Posicao/local", record.positionOrRoute.ifBlank { "-" }, marginX + 14f, rowY, marginX + 100f)
                drawKeyValue("Custo", formatMoney(record.cost), marginX + 330f, rowY, marginX + 390f)
                rowY += 20f
                drawKeyValue("KM inicial", "${record.kmStart} km", marginX + 14f, rowY, marginX + 100f)
                drawKeyValue("KM final", record.kmEnd?.let { "$it km" } ?: "Aguardando", marginX + 220f, rowY, marginX + 290f)
                val durability = record.kmEnd?.let { it - record.kmStart }?.takeIf { it > 0 }
                rowY += 20f
                drawKeyValue("Durabilidade", durability?.let { "$it km • ${formatMoney(costPerKm(record))}/km" } ?: "Aguardando KM final", marginX + 14f, rowY, marginX + 100f, 62)
            }
            y += height + 14f
        }
    }

    finishPage()
    val file = File(context.cacheDir, "relatorio_${operationalReportFileSlug(feature)}_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    file
}.getOrElse {
    null
}

internal fun shareOperationalPdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, trNow("Compartilhar PDF", "Share PDF")))
}
