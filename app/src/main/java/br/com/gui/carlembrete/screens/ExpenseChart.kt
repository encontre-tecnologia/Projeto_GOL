package br.com.gui.carlembrete

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CategoryExpenseChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    emptyColor: Color = Color(0xFF334155),
    centerColor: Color = Color(0xFF0B1224)
) {
    val safeData =
        if (data.isEmpty()) listOf(CategorySpend(label = "Sem dados", valor = 0.0, color = emptyColor)) else data
    val totalValor = safeData.sumOf { it.valor }.coerceAtLeast(0.0)
    val hasData = totalValor > 0.0
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val barCount = safeData.size.coerceAtLeast(1)
        val spacingDp = 10.dp
        val spacingPx = with(density) { spacingDp.toPx() }
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalSpacingPx = spacingPx * (barCount - 1)
        val barWidthPx = ((totalWidthPx - totalSpacingPx) / barCount)
            .coerceAtLeast(with(density) { 6.dp.toPx() })
        val barWidthDp = with(density) { barWidthPx.toDp() }
        val iconSize = 14.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val maxValor = safeData.maxOfOrNull { it.valor }?.coerceAtLeast(0.0) ?: 0.0
                val maxHeight = size.height * 0.85f
                val baseY = size.height
            val lowColor = Color(0xFF22C55E)
            val midColor = Color(0xFFF59E0B)
            val highColor = Color(0xFFEF4444)
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val gridSteps = 4
            repeat(gridSteps + 1) { step ->
                val y = baseY - (maxHeight / gridSteps) * step
                val t = step.toFloat() / gridSteps.toFloat()
                val baseColor = if (t <= 0.5f) {
                    lerp(lowColor, midColor, t / 0.5f)
                } else {
                    lerp(midColor, highColor, (t - 0.5f) / 0.5f)
                }
                drawLine(
                    color = baseColor.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
                if (!hasData) {
                    val barHeight = maxHeight * 0.4f
                    val left = (size.width - barWidthPx) / 2f
                    drawRoundRect(
                        color = emptyColor,
                        topLeft = Offset(left, baseY - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                } else {
                    safeData.forEachIndexed { index, item ->
                        val ratio = if (maxValor > 0.0) (item.valor / maxValor).toFloat() else 0f
                        val barHeight = (maxHeight * ratio * progress.value).coerceAtLeast(4.dp.toPx())
                        val left = index * (barWidthPx + spacingPx)
                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(left, baseY - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                        val percent = if (totalValor > 0.0) ((item.valor / totalValor) * 100).toInt() else 0
                        if (percent > 0) {
                            val x = left + (barWidthPx / 2f)
                            val y = (baseY - barHeight - 6.dp.toPx()).coerceAtLeast(textPaint.textSize)
                            drawContext.canvas.nativeCanvas.drawText("$percent%", x, y, textPaint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacingDp)
            ) {
                safeData.forEach { item ->
                    val tipo = TipoManutencao.values().firstOrNull { it.label == item.label }
                    Box(
                        modifier = Modifier.width(barWidthDp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tipo != null) {
                            TipoIcon(
                                tipo = tipo,
                                tint = Color.White.copy(alpha = 0.7f),
                                size = iconSize,
                                textSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryExpenseLegend(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    labelColor: Color = Color(0xFF94A3B8),
    emptyColor: Color = Color(0xFF334155),
    minItems: Int = 0
) {
    val safeData =
        if (data.isEmpty()) listOf(CategorySpend(label = "Sem dados", valor = 0.0, color = emptyColor)) else data
    val totalValor = safeData.sumOf { it.valor }.coerceAtLeast(0.0)
    val centralizarLegenda = safeData.size <= 5
    Column(
        modifier = modifier,
        verticalArrangement = if (centralizarLegenda) Arrangement.Center else Arrangement.spacedBy(6.dp)
    ) {
        safeData.forEach { item ->
            val dotColor = if (item.valor <= 0.0) emptyColor else item.color
            val tipo = TipoManutencao.values().firstOrNull { it.label == item.label }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(8.dp))
                if (tipo != null) {
                    TipoIcon(
                        tipo = tipo,
                        tint = Color.White.copy(alpha = 0.7f),
                        size = 12.dp,
                        textSize = 8.sp
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.label,
                    color = labelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatarMoedaLocal(item.valor),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        if (safeData.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(Modifier.height(0.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total:", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatarMoedaLocal(totalValor),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val fillers = (minItems - safeData.size).coerceAtLeast(0)
        repeat(fillers) { Spacer(Modifier.height(16.dp)) }
    }
}

internal fun corCategoria(tipo: TipoManutencao): Color = when (tipo) {
    TipoManutencao.CORRENTE -> Color(0xFF22C55E)
    TipoManutencao.LUBRIFICACAO -> Color(0xFF14B8A6)
    TipoManutencao.PEDIVELA -> Color(0xFF0EA5E9)
    TipoManutencao.ACESSORIOS -> Color(0xFFF97316)
    TipoManutencao.CONFORTO -> Color(0xFFEAB308)
    TipoManutencao.PNEU -> Color(0xFFF59E0B)
    TipoManutencao.TRANSMISSAO -> Color(0xFF60A5FA)
    TipoManutencao.REVISAO -> Color(0xFF8B5CF6)
    TipoManutencao.OLEO -> Color(0xFF3B82F6) // azul
    TipoManutencao.ABASTECIMENTO -> Color(0xFF0EA5E9) // azul ciano
    TipoManutencao.LAVAGEM -> Color(0xFF06B6D4) // azul agua
    TipoManutencao.BATERIA -> Color(0xFF16A34A) // verde
    TipoManutencao.VIDROS -> Color(0xFF38BDF8) // azul vidro
    TipoManutencao.MECANICA -> Color(0xFF60A5FA) // azul claro
    TipoManutencao.FUNILARIA -> Color(0xFFF97316) // laranja
    TipoManutencao.FREIO -> Color(0xFFDC2626) // vermelho
    TipoManutencao.LICENCIAMENTO -> Color(0xFF22C55E) // verde claro
    TipoManutencao.IPVA -> Color(0xFF5B8DEF) // azul leve
    TipoManutencao.SEGURO -> Color(0xFF10B981) // verde
    TipoManutencao.OUTROS -> Color(0xFF94A3B8)
}
