package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AvisosNotificacoesScreen(
    notificacoes: List<NotificacaoDisparada>,
    onClear: () -> Unit,
    onRemove: (NotificacaoDisparada) -> Unit,
    resolveVehicleName: (NotificacaoDisparada) -> String?,
    canOpenNotification: (NotificacaoDisparada) -> Boolean,
    onOpen: (NotificacaoDisparada) -> Unit,
    onDismiss: () -> Unit
) {
    // Notifications stay in a stable light surface so alert text remains readable
    // even when the rest of the application follows the system dark theme.
    val screenBg = Color(0xFFF8FAFC)
    val cardBg = Color.White
    val cardBgSoft = Color(0xFFF1F5F9)
    val cardBorder = Color(0xFFCBD5E1)
    val titleColor = Color(0xFF111827)
    val textDim = Color(0xFF64748B)
    val textSub = Color(0xFF334155)
    val clearBg = Color(0xFFFEE2E2)
    val clearIconTint = Color(0xFFDC2626)
    val accentBlue = Color(0xFF2563EB)
    val secondaryChipBg = Color(0xFFE2E8F0)
    val chevronTint = Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item("header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = tr("Notificações", "Notifications"),
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (notificacoes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(clearBg)
                                .clickable { onClear() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = tr("Limpar notificações", "Clear notifications"),
                                tint = clearIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                }
            }

            if (notificacoes.isEmpty()) {
                item("empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = accentBlue,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            tr("Tudo em dia por aqui", "Everything is up to date"),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tr(
                                "Quando um aviso disparar, ele aparece aqui para você acompanhar com calma.",
                                "When a reminder is triggered, it will appear here."
                            ),
                            color = textDim,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                item("hint_card") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBgSoft)
                            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Swipe,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tr("Deslize para apagar um aviso", "Swipe to delete a notification"),
                            color = textSub,
                            fontSize = 12.sp
                        )
                    }
                }

                items(notificacoes, key = { "${it.id}_${it.timestamp}" }) { aviso ->
                    val canOpen = canOpenNotification(aviso)
                    val vehicleName = resolveVehicleName(aviso)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (aviso.id.startsWith("PARKING_")) {
                                return@rememberSwipeToDismissBoxState false
                            }
                            if (value != SwipeToDismissBoxValue.Settled) {
                                onRemove(aviso)
                            }
                            true
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                                listOf(Color(0xFFDC2626), Color(0xFFEF4444))
                                            else
                                                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                        )
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                    Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    Text(tr("Apagar", "Delete"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    ) {
                        val isParking = aviso.id.startsWith("PARKING_")
                        val chipColor = when {
                            isParking -> Color(0xFF22C55E)
                            canOpen -> Color(0xFF60A5FA)
                            else -> Color(0xFFA78BFA)
                        }
                        val chipLabel = when {
                            isParking -> tr("Estacionamento", "Parking")
                            canOpen -> tr("Aviso", "Reminder")
                            else -> tr("Informativo", "Info")
                        }
                        val notifIcon = when {
                            isParking -> Icons.Default.DirectionsCar
                            else -> Icons.Default.NotificationsActive
                        }
                        val instante = runCatching {
                            java.time.Instant.ofEpochMilli(aviso.timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"))
                        }.getOrDefault("--")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(cardBg)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                                .clickable(enabled = canOpen) { onOpen(aviso) }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chipColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = notifIcon,
                                        contentDescription = null,
                                        tint = chipColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        aviso.titulo.ifBlank { tr("Notificação", "Notification") },
                                        fontWeight = FontWeight.Bold,
                                        color = titleColor,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        instante,
                                        color = textDim,
                                        fontSize = 11.sp
                                    )
                                }
                                if (canOpen) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = chevronTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (aviso.descricao.isNotBlank()) {
                                Text(
                                    text = aviso.descricao,
                                    color = textSub,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(chipLabel, color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (!vehicleName.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(secondaryChipBg)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(vehicleName, color = textSub, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item("bottom_spacer") { Spacer(Modifier.height(8.dp)) }
        }
    }
}
