package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val container = when {
        highlighted && isDark -> Color(0xFF3B2A0A)
        highlighted && !isDark -> Color(0xFFFEF3C7)
        isDark -> Color(0xFF111827)
        else -> Color(0xFFF1F5F9)
    }
    val borderColor = when {
        highlighted -> Color(0xFFFBBF24)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> colorScheme.outlineVariant.copy(alpha = 0.9f)
    }
    val iconTint = when {
        highlighted -> Color(0xFFF59E0B)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }
    val textColor = when {
        highlighted && isDark -> Color(0xFFFEF3C7)
        highlighted && !isDark -> Color(0xFF92400E)
        isDark -> Color(0xFFF1F5F9)
        else -> Color(0xFF0F172A)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.Black.copy(alpha = 0.18f) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)), // Surface Dark
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6), // Blue 500
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Color(0xFFE2E8F0), // Slate 200
                fontSize = 14.sp
            )
        }
    }
}


@Composable
internal fun LembreteCardLocal(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    modeloCarro: String,
    onDelete: () -> Unit,
    onAddPrestador: () -> Unit,
    onClick: () -> Unit,
    statusLabel: String,
    statusColor: Color
) {
    val context = LocalContext.current
    val bg = Color(0xFF111827)
    val bg2 = Color(0xFF0B1224)
    val stroke = Color(0xFF23324D)
    val text = Color(0xFFF1F5F9)
    val dim = Color(0xFF94A3B8)

    // LÃ³gica para formatar o KM
    val kmFormatado = remember(lembrete.kmLimite) {
        val apenasDigitos = lembrete.kmLimite.filter { it.isDigit() }
        apenasDigitos.toLongOrNull()?.let {
            java.text.NumberFormat.getInstance(java.util.Locale("pt", "BR")).format(it)
        } ?: lembrete.kmLimite.ifBlank { "-" }
    }

    val iconBg = Brush.linearGradient(
        colors = listOf(
            statusColor.copy(alpha = 0.28f),
            statusColor.copy(alpha = 0.10f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, stroke),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(bg, bg2)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-2).dp)
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconBg)
                            .border(1.dp, statusColor.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        TipoIcon(
                            tipo = lembrete.tipo,
                            tint = statusColor,
                            size = 22.dp
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lembrete.titulo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (lembrete.peca.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Peça: ${lembrete.peca}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (lembrete.valor > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = formatarMoedaLocal(lembrete.valor),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- INFO CHIPS ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dataOuKm = when {
                        lembrete.dataLimite.isNotBlank() -> lembrete.dataLimite
                        lembrete.kmLimite.isNotBlank() -> kmFormatado + " km"
                        else -> "Sem meta"
                    }

                    // 1. Data
                    InfoMini(
                        icon = Icons.Rounded.CalendarMonth,
                        text = dataOuKm,
                        tint = dim,
                        iconTint = statusColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // 2. Status/Prazo
                    val statusIcon = when (statusLabel) {
                        "No Prazo", "Hoje" -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.ErrorOutline
                    }
                    val statusIconColor = when (statusLabel) {
                        "No Prazo", "Hoje" -> Color(0xFF10B981)
                        "Urgente" -> Color(0xFFF59E0B)
                        "Vencido" -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }
                    InfoMini(
                        icon = statusIcon,
                        text = statusLabel,
                        tint = dim,
                        iconTint = statusColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // 3. KM
                    InfoMini(
                        icon = Icons.Rounded.Speed,
                        text = "KM: $kmFormatado",
                        tint = dim,
                        iconTint = statusColor,
                        ellipsize = false
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                if (contato != null && contato.telefone.isNotBlank()) {
                    Button(
                        onClick = {
                            abrirWhatsApp(
                                context,
                                contato.telefone,
                                "Olá tudo bem?"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Chamar no Whatszap",
                            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (contato != null) {
                    Button(
                        onClick = onAddPrestador,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr("Adicionar telefone", "Add phone"),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Button(
                        onClick = onAddPrestador,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14532D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFFD1FAE5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr("Adicionar prestador do servico", "Add service provider"),
                            color = Color(0xFFD1FAE5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BadgeStatus(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}
