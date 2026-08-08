package br.com.gui.carlembrete

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val FLEET_SIGNATURE_PREFS = "fleet_signature_prefs"

private data class FleetSignaturePayload(val strokes: List<List<FleetSignaturePoint>> = emptyList())
private data class FleetSignaturePoint(val x: Float, val y: Float)

internal fun loadFleetSignature(context: Context, userId: String): String =
    context.getSharedPreferences(FLEET_SIGNATURE_PREFS, Context.MODE_PRIVATE)
        .getString("signature_$userId", "")
        .orEmpty()

internal fun saveFleetSignature(context: Context, userId: String, signature: String) {
    context.getSharedPreferences(FLEET_SIGNATURE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("signature_$userId", signature)
        .apply()
    FirebaseFirestore.getInstance().collection("users").document(userId).set(
        mapOf("fleetSignature" to signature, "fleetSignatureUpdatedAt" to FieldValue.serverTimestamp()),
        SetOptions.merge()
    )
}

@androidx.compose.runtime.Composable
internal fun FleetSignaturePad(
    value: String,
    cardBg: Color = Color.White,
    cardBorder: Color = Color(0xFFBFDBFE),
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    /*
     * O estado do traco nao pode ser recriado a cada `value`.
     *
     * Com `remember(value)`, todo ponto desenhado disparava onValueChange, o pai devolvia um novo
     * `value` e nascia um MutableState novo. Mas o `pointerInput(canvasSize)` nao reinicia junto —
     * o bloco de gesto continuava lendo e escrevendo o objeto de estado ANTIGO.
     *
     * Enquanto so se desenha isso passa despercebido, porque o JSON vai e volta igual. Ao apertar
     * "Limpar" o estado visivel virava vazio, mas o do gesto ainda guardava os tracos apagados:
     * o proximo toque emitia "antigos + novo ponto" e a assinatura apagada reaparecia.
     *
     * Agora o objeto de estado e unico enquanto o pad existe, e mudanca vinda de fora (o Limpar)
     * entra pelo efeito abaixo.
     */
    var strokes by remember { mutableStateOf(decodeFleetSignature(value)) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(value) {
        val deFora = decodeFleetSignature(value)
        // So reage quando o valor externo diverge do que o pad desenhou; durante o traco os dois
        // sao iguais e este efeito nao faz nada.
        if (deFora != strokes) strokes = deFora
    }
    // O traço precisa contrastar com o fundo do proprio quadro, senao em tema escuro
    // a tinta escura fica quase invisivel sobre um fundo tambem escuro.
    val isDarkPad = cardBg.luminance() < 0.45f
    val inkColor = if (isDarkPad) Color(0xFFE2E8F0) else Color(0xFF0F172A)

    fun emit(updated: List<List<FleetSignaturePoint>>) {
        strokes = updated
        onValueChange(Gson().toJson(FleetSignaturePayload(updated)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .background(
                if (isDarkPad) Color(0xFF111827) else Color(0xFFF8FAFC),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(canvasSize) {
                    detectDragGestures(
                        onDragStart = { start ->
                            if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures
                            emit(strokes + listOf(listOf(start.toSignaturePoint(canvasSize))))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (strokes.isEmpty() || canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures
                            val last = strokes.last() + change.position.toSignaturePoint(canvasSize)
                            emit(strokes.dropLast(1) + listOf(last))
                        }
                    )
                }
        ) {
            strokes.forEach { stroke ->
                stroke.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        color = inkColor,
                        start = Offset(start.x * size.width, start.y * size.height),
                        end = Offset(end.x * size.width, end.y * size.height),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
internal fun FleetSignaturePreview(signature: String, cardBg: Color = Color.White, modifier: Modifier = Modifier) {
    val strokes = remember(signature) { decodeFleetSignature(signature) }
    // Mesmo cuidado do quadro de assinar: o traço precisa contrastar com o fundo real por tras dele.
    val inkColor = if (cardBg.luminance() < 0.45f) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    Canvas(modifier = modifier) {
        strokes.forEach { stroke ->
            stroke.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = inkColor,
                    start = Offset(start.x * size.width, start.y * size.height),
                    end = Offset(end.x * size.width, end.y * size.height),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun Offset.toSignaturePoint(size: IntSize) = FleetSignaturePoint(
    x = (x / size.width.toFloat()).coerceIn(0f, 1f),
    y = (y / size.height.toFloat()).coerceIn(0f, 1f)
)

private fun decodeFleetSignature(raw: String): List<List<FleetSignaturePoint>> = runCatching {
    Gson().fromJson<FleetSignaturePayload>(raw, object : TypeToken<FleetSignaturePayload>() {}.type).strokes
}.getOrDefault(emptyList())
