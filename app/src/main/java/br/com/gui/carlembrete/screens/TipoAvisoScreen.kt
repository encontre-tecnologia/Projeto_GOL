package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AvisoItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tipo: TipoManutencao? = null,
    val iconOverride: ImageVector? = null,
    val wide: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun TipoAvisoScreen(
    itensAviso: List<AvisoItem>,
    backgroundBrush: Brush,
    surfaceDark: Color,
    textLight: Color,
    textDim: Color,
    onOpenVehicleGuide: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val titleIconTint = scheme.primary
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.ArrowBackIosNew,
                                contentDescription = tr("Voltar", "Back"),
                                tint = textDim
                            )
                        }
                        val playButtonColor = Color(0xFF2563EB)
                        IconButton(
                            onClick = onOpenVehicleGuide,
                            modifier = Modifier
                                .size(40.dp)
                                .background(playButtonColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = playButtonColor.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = tr("Abrir dicas de mecânica", "Open mechanic tips"),
                                tint = playButtonColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(titleIconTint.copy(alpha = 0.14f), CircleShape)
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EventNote,
                                contentDescription = null,
                                tint = titleIconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            tr("O que vamos lembrar?", "What should we remember?"),
                            color = textLight,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .widthIn(max = 620.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val wideItems = itensAviso.filter { it.wide }
                        val gridItems = itensAviso.filter { !it.wide }
                        wideItems.forEach { item ->
                            OutlinedButton(
                                onClick = item.onClick,
                                border = BorderStroke(1.dp, if (surfaceDark.luminance() < 0.5f) Color(0xFF334155) else Color.Black.copy(alpha = 0.18f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (surfaceDark.luminance() < 0.5f) Color(0xFF0F172A).copy(alpha = 0.55f) else surfaceDark.copy(alpha = 0.92f),
                                    contentColor = textLight
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (item.label.contains("estacionei", ignoreCase = true)) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(item.color.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "E",
                                                color = item.color,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(item.color.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.iconOverride ?: item.icon,
                                                contentDescription = null,
                                                tint = item.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        item.label,
                                        color = textLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        gridItems.chunked(2).forEach { linha ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                linha.forEach { item ->
                                    OutlinedButton(
                                        onClick = item.onClick,
                                        border = BorderStroke(1.dp, if (surfaceDark.luminance() < 0.5f) Color(0xFF334155) else Color.Black.copy(alpha = 0.12f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (surfaceDark.luminance() < 0.5f) Color(0xFF0F172A).copy(alpha = 0.45f) else surfaceDark.copy(alpha = 0.90f),
                                            contentColor = textLight
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (item.iconOverride != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(item.color.copy(alpha = 0.14f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.iconOverride,
                                                        contentDescription = null,
                                                        tint = item.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else if (item.tipo != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(item.color.copy(alpha = 0.14f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    TipoIcon(
                                                        tipo = item.tipo,
                                                        tint = item.color,
                                                        size = 18.dp,
                                                        textSize = 12.sp
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(item.color.copy(alpha = 0.14f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = null,
                                                        tint = item.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                item.label,
                                                color = textLight,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                                if (linha.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

