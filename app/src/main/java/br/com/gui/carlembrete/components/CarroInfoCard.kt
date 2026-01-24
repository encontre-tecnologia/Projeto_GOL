package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CarroInfoCard(
    carroAtual: CarroInfo,
    onPrevCar: () -> Unit,
    onNextCar: () -> Unit,
    onOpenCarInfo: () -> Unit,
    onEditCar: () -> Unit,
    onOpenRelatorio: () -> Unit,
    onNovoLembrete: () -> Unit,
    textLight: Color,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0xFF23324D))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. HERO CARD DO CARRO (Com Estampa de Fundo)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF172554)),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
                    .clickable { onOpenCarInfo() }
            ) {
                // --- CAMADA DE ESTAMPA (BACKGROUND DECORATIVO) ---
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-20).dp, y = (-20).dp)
                        .rotate(15f)
                )
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 30.dp)
                        .rotate(-25f)
                )
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = 10.dp)
                        .rotate(45f)
                )
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = 10.dp, y = 40.dp)
                        .rotate(-10f)
                )

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevCar) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                null,
                                tint = textLight.copy(0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = carroAtual.marca.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = carroAtual.nome,
                                style = MaterialTheme.typography.headlineSmall,
                                color = textLight,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        IconButton(onClick = onNextCar) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = textLight.copy(0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    val logoRes = carroAtual.logoResOrNull()
                    if (logoRes != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            modifier = Modifier.size(70.dp),
                            colorFilter = ColorFilter.tint(textLight)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = carroAtual.tipoIconRes()),
                            contentDescription = null,
                            tint = textLight,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = carroAtual.modelo.ifBlank { "Modelo não informado" },
                            style = MaterialTheme.typography.titleMedium,
                            color = textLight.copy(alpha = 0.85f)
                        )
                        Text(
                            text = if (carroAtual.kmAtual > 0) "${carroAtual.kmAtual} km" else "KM nao informado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // 2. AÇÕES RÁPIDAS (Botoes Lado a Lado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Rounded.Edit,
                    label = "Editar veiculo",
                    modifier = Modifier.weight(1f),
                    onClick = onEditCar
                )
                ActionButton(
                    icon = Icons.Default.Description,
                    label = "Abrir Relatório",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenRelatorio
                )
            }

            // 3. BOTÃO "NOVO LEMBRETE"
            Button(
                onClick = onNovoLembrete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
            ) {
                Icon(Icons.Default.Event, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Novo Lembrete", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
