package br.com.gui.carlembrete

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal const val HOME_TUTORIAL_PREFS = "home_tutorial_prefs"
internal const val KEY_HOME_TUTORIAL_VERSION = "home_tutorial_version"
internal const val CURRENT_HOME_TUTORIAL_VERSION = 1
internal const val FORCE_HOME_TUTORIAL_ALWAYS = false
internal const val ONBOARDING_PREFS = "onboarding_prefs"
internal const val KEY_REPORT_MINI_TUTORIAL_SEEN = "report_mini_tutorial_seen"

internal fun shouldAutoStartHomeTutorial(context: Context): Boolean {
    if (FORCE_HOME_TUTORIAL_ALWAYS) return true
    val seenVersion = context
        .getSharedPreferences(HOME_TUTORIAL_PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_HOME_TUTORIAL_VERSION, 0)
    return seenVersion < CURRENT_HOME_TUTORIAL_VERSION
}

internal fun markHomeTutorialSeen(context: Context) {
    context.getSharedPreferences(HOME_TUTORIAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_HOME_TUTORIAL_VERSION, CURRENT_HOME_TUTORIAL_VERSION)
        .apply()
}

internal fun shouldShowReportMiniTutorial(context: Context): Boolean {
    return !context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_REPORT_MINI_TUTORIAL_SEEN, false)
}

internal fun markReportMiniTutorialSeen(context: Context) {
    context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REPORT_MINI_TUTORIAL_SEEN, true)
        .apply()
}

@Composable
internal fun HomeTutorialSpotlightOverlay(
    targetRect: Rect?,
    message: String,
    step: Int,
    total: Int,
    targetCornerRadius: Dp,
    accentBlue: Color,
    stepIcon: ImageVector,
    stepTitle: String,
    onClose: () -> Unit,
    onNext: () -> Unit
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { targetCornerRadius.toPx() }
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val inset = strokeWidthPx / 2f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawRect(Color.Black.copy(alpha = 0.55f))
                if (targetRect != null) {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = androidx.compose.ui.geometry.Offset(targetRect.left, targetRect.top),
                        size = androidx.compose.ui.geometry.Size(targetRect.width, targetRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = Color(0xFF60A5FA),
                        topLeft = androidx.compose.ui.geometry.Offset(targetRect.left + inset, targetRect.top + inset),
                        size = androidx.compose.ui.geometry.Size(targetRect.width - (inset * 2f), targetRect.height - (inset * 2f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerRadiusPx - inset).coerceAtLeast(0f), (cornerRadiusPx - inset).coerceAtLeast(0f)),
                        style = Stroke(width = strokeWidthPx)
                    )
                }
                drawContent()
            }
            .clickable(enabled = true) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .heightIn(min = 220.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF0B1220)
                            )
                        )
                    )
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1D4ED8).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = stepTitle,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "Etapa $step de $total",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    color = Color(0xFF111827),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF64748B)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            text = "Fechar tutorial",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Próximo",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeQuickStartDialog(
    step: Int,
    pages: List<Triple<ImageVector, String, String>>,
    onNext: () -> Unit,
    onDemoCreateReminder: () -> Unit
) {
    if (pages.isEmpty()) return
    val safeStep = step.coerceIn(0, pages.lastIndex)
    val (icon, title, body) = pages[safeStep]
    val isLast = safeStep == pages.lastIndex
    val progress = (safeStep + 1f) / pages.size.toFloat()
    val scheme = MaterialTheme.colorScheme
    val cardBg = Color(0xFF0B1220)
    val panelBg = Color(0xFF101A2B)
    val borderColor = Color(0xFF334155)
    val titleColor = Color(0xFFE2E8F0)
    val bodyColor = Color(0xFFCBD5E1)
    val dimColor = Color(0xFF94A3B8)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBg
            ),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            scheme.primary.copy(alpha = 0.4f),
                                            scheme.secondary.copy(alpha = 0.28f)
                                        )
                                    )
                                )
                                .border(1.dp, scheme.primary.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Guia rápido",
                                color = titleColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Passo ${safeStep + 1} de ${pages.size}",
                                color = dimColor,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = scheme.primary,
                    trackColor = borderColor.copy(alpha = 0.45f)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = panelBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = title,
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = body,
                            color = bodyColor,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == safeStep) 20.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (index == safeStep) scheme.primary
                                    else scheme.outline.copy(alpha = 0.35f)
                                )
                        )
                    }
                }

                if (isLast) {
                    OutlinedButton(
                        onClick = onDemoCreateReminder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Fazer demonstração")
                    }
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (isLast) "Concluir guia" else "Próximo",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
