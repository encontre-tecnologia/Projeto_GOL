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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import kotlin.math.min
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.gui.carlembrete.R
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

@Composable
fun ConfigToggleItem(
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
fun MonitorIcon(
    tipo: TipoManutencao,
    cor: Color,
    quantidade: Int,
    selected: Boolean,
    onClick: () -> Unit,
    containerSize: Dp = 60.dp,
    boxSize: Dp = 52.dp,
    cornerRadius: Dp = 14.dp,
    iconSize: Dp = 22.dp,
    labelSize: TextUnit = 12.sp
) {
    val bg = if (selected) cor.copy(alpha = 0.18f) else Color(0xFF0B1220)
    val border = if (selected) cor.copy(alpha = 0.9f) else Color(0xFF334155)
    val labelColor = if (selected) Color.White else Color(0xFFCBD5E1)
    val badgeBg = if (quantidade > 0) Color(0xFFEF4444) else Color(0xFF1F2937)
    val badgeText = if (quantidade > 0) Color.White else Color(0xFF94A3B8)

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(containerSize), contentAlignment = Alignment.Center) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(bg)
                    .border(2.dp, border, RoundedCornerShape(cornerRadius))
            ) {
                TipoIcon(
                    tipo = tipo,
                    tint = cor,
                    size = iconSize,
                    textSize = (labelSize.value + 2).sp
                )
            }

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .border(1.dp, border, RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = quantidade.toString(),
                    color = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = tipo.label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = labelSize,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TipoIcon(
    tipo: TipoManutencao,
    tint: Color,
    size: Dp,
    textSize: TextUnit = 12.sp
) {
    Icon(
        imageVector = tipo.getIcon(),
        contentDescription = tipo.label,
        tint = tint,
        modifier = Modifier.size(size)
    )
}



@Composable
fun RowScope.VehicleStat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier = modifier.weight(1f)) {
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .border(1.dp, Color(0xFF1F2A44), RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B33)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(statusColor.copy(alpha = 0.18f), CircleShape)
                            .border(1.dp, statusColor.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        TipoIcon(
                            tipo = lembrete.tipo,
                            tint = statusColor,
                            size = 24.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            lembrete.titulo,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(lembrete.tipo.label, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(color = Color(0xFF122542), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(lembrete.dataLimite, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    if (lembrete.kmLimite.isNotBlank()) {
                        Surface(color = Color(0xFF122542), shape = RoundedCornerShape(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Speed, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${lembrete.kmLimite} km", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    if (lembrete.valor > 0) {
                        Surface(color = Color(0xFF0F2A1F), shape = RoundedCornerShape(10.dp)) {
                            Text(
                                formatarMoeda(lembrete.valor),
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (statusLabel.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(color = statusColor.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            statusLabel,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (contato != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF1F2A44))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { abrirWhatsApp(context, contato.telefone, "Olá ${contato.nome}, preciso de *${lembrete.titulo}* para o *$modeloCarro*.") },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Enviar mensagem para ${contato.nome.split(" ")[0]}",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16243D))
                    .border(1.dp, Color(0xFF2B3A5C), CircleShape)
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
        }
    }
}

