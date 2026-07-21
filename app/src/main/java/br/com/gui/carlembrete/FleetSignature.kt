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
    val initial = remember(value) { decodeFleetSignature(value) }
    var strokes by remember(value) { mutableStateOf(initial) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun emit(updated: List<List<FleetSignaturePoint>>) {
        strokes = updated
        onValueChange(Gson().toJson(FleetSignaturePayload(updated)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .background(
                if (cardBg.luminance() < 0.45f) Color(0xFF111827) else Color(0xFFF8FAFC),
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
                        color = Color(0xFF0F172A),
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
internal fun FleetSignaturePreview(signature: String, modifier: Modifier = Modifier) {
    val strokes = remember(signature) { decodeFleetSignature(signature) }
    Canvas(modifier = modifier) {
        strokes.forEach { stroke ->
            stroke.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = Color(0xFF0F172A),
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
