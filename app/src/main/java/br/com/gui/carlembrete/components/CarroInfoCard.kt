import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.CarroInfo
import br.com.gui.carlembrete.TipoVeiculo
import br.com.gui.carlembrete.VehicleIcon

private data class CarCardPalette(
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val actionBackground: Color,
    val isDark: Boolean
)

@Composable
private fun carCardPalette(): CarCardPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return CarCardPalette(
        cardBackground = if (isDark) Color(0xFF2B3950) else Color.White,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        actionBackground = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.65f),
        isDark = isDark
    )
}

@Composable
fun CarroInfoCard(
    carroAtual: CarroInfo,
    onPrevCar: () -> Unit,
    onNextCar: () -> Unit,
    onOpenCarInfo: () -> Unit,
    onEditCar: () -> Unit,
    onOpenRelatorio: () -> Unit,
    onOpenFuelHistory: () -> Unit,
    showFuelHistoryAction: Boolean = false,
    onNovoLembrete: () -> Unit,
    nomeMantedor: String,
    textLight: Color,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    val palette = carCardPalette()
    val heroTextColor = Color.White
    val cardBorderColor = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.35f)
    val nomeProprietarioExibido = carroAtual.proprietario
        .trim()
        .ifBlank { nomeMantedor.trim() }
        .split(" ")
        .firstOrNull()
        .orEmpty()
    val anoVeiculo = extrairAnoVeiculo(carroAtual.modelo)
    val modeloSemAno = removerAnoDoModelo(carroAtual.modelo).ifBlank { carroAtual.modelo.ifBlank { "N/A" } }
    val vehicleIconOffsetY = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) (-12).dp else 0.dp
    val marcaModeloAno = listOf(
        carroAtual.marca.uppercase().takeIf { it.isNotBlank() },
        modeloSemAno.takeIf { it.isNotBlank() },
        anoVeiculo
    ).filterNotNull().joinToString(" • ")
    // Cores DinÃ¢micas baseadas no carro (Apenas para o card interno do carro)
    val baseColor = carroAtual.getCorUI()
    val carDisplayGradientStart = lerp(Color(0xFF1E293B), baseColor, 0.55f)
    val carDisplayGradientEnd = lerp(Color(0xFF0B1224), baseColor, 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.5f))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            // AQUI ESTÃ A MUDANÃ‡A: Usamos uma cor sÃ³lida para o container
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            border = BorderStroke(1.dp, cardBorderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Garante que o fundo seja uniforme
                    .background(palette.cardBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. HERO CARD DO CARRO (Visual do Carro)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .clip(RoundedCornerShape(22.dp))
                        // O degradÃª fica SÃ“ aqui dentro, na imagem do carro
                        .background(
                            Brush.linearGradient(
                                colors = listOf(carDisplayGradientStart, carDisplayGradientEnd),
                                start = Offset(0f, 0f),
                                end = Offset(0f, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                        .clickable { onOpenCarInfo() }
                ) {
                    // --- WATERMARKS (Background decorativo) ---
                    DecoracoesDeFundo(baseColor)

                    // --- CONTEÃšDO PRINCIPAL ---
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // CABEÇALHO (Nome)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = marcaModeloAno,
                                    fontSize = 12.sp,
                                    color = heroTextColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = carroAtual.nome,
                                    fontSize = 24.sp,
                                    color = heroTextColor,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // CENTRO (Logo com Glow)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glow effect atrÃ¡s do logo
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(baseColor.copy(alpha = 0.3f), Color.Transparent)
                                        )
                                    )
                            )

                            VehicleIcon(
                                tipoVeiculo = carroAtual.tipoVeiculo,
                                tint = null,
                                size = 210.dp,
                                modifier = Modifier.offset(y = vehicleIconOffsetY)
                            )

                            Text(
                                text = "Veículo de: $nomeProprietarioExibido",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp),
                                color = heroTextColor.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. PAINEL DE AÃ‡Ã•ES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionGlassButton(
                        icon = Icons.Rounded.Edit,
                        label = "Editar",
                        modifier = Modifier.weight(1f),
                        onClick = onEditCar
                    )
                    ActionGlassButton(
                        icon = Icons.Rounded.Description,
                        label = "Relatório",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRelatorio
                    )
                }

                // 3. BOTÃƒO PRINCIPAL (Wide)
                Button(
                    onClick = onNovoLembrete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = palette.accent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Event, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Novo Lembrete",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun extrairAnoVeiculo(modelo: String): String? {
    val match = Regex("(19|20)\\d{2}(?:/(19|20)\\d{2})?").find(modelo)
    return match?.value
}

private fun removerAnoDoModelo(modelo: String): String {
    return modelo
        .replace(Regex("(19|20)\\d{2}(?:/(19|20)\\d{2})?"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

// Sub-componente de decoraÃ§Ãµes
@Composable
fun DecoracoesDeFundo(color: Color) {
    Box(Modifier.fillMaxSize())
}

@Composable
fun ActionGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = carCardPalette()
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            // Um tom ligeiramente diferente do fundo sÃ³lido para destacar o botÃ£o
            containerColor = palette.actionBackground,
            contentColor = palette.textSecondary
        ),
        border = BorderStroke(1.5.dp, palette.textPrimary.copy(alpha = 0.2f)),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = palette.textPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

