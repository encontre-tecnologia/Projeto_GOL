import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.CarroInfo
import br.com.gui.carlembrete.logoResOrNull
import br.com.gui.carlembrete.tipoIcon

// --- CORES ---
// Cor sÃ³lida para o fundo do Card Principal (Uniforme)
private val CardBackgroundColor = Color(0xFF1E293B) // Slate 800
private val TextWhite = Color(0xFFF1F5F9)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)

@Composable
fun CarroInfoCard(
    carroAtual: CarroInfo,
    onPrevCar: () -> Unit,
    onNextCar: () -> Unit,
    onOpenCarInfo: () -> Unit,
    onEditCar: () -> Unit,
    onOpenRelatorio: () -> Unit,
    onNovoLembrete: () -> Unit,
    nomeMantedor: String,
    textLight: Color,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    // Cores DinÃ¢micas baseadas no carro (Apenas para o card interno do carro)
    val baseColor = carroAtual.getCorUI()
    val carDisplayGradientStart = lerp(Color(0xFF1E293B), baseColor, 0.3f)
    val carDisplayGradientEnd = lerp(Color(0xFF020617), baseColor, 0.1f)

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
            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Garante que o fundo seja uniforme
                    .background(CardBackgroundColor)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. HERO CARD DO CARRO (Visual do Carro)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(22.dp))
                        // O degradÃª fica SÃ“ aqui dentro, na imagem do carro
                        .background(
                            Brush.linearGradient(
                                colors = listOf(carDisplayGradientStart, carDisplayGradientEnd),
                                start = Offset(0f, 0f),
                                end = Offset(0f, Float.POSITIVE_INFINITY)
                            )
                        )
                        .clickable { onOpenCarInfo() }
                ) {
                    // --- WATERMARKS (Background decorativo) ---
                    DecoracoesDeFundo(baseColor)

                    // --- CONTEÃšDO PRINCIPAL ---
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // CABEÃ‡ALHO (Nav + Nome)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onPrevCar) {
                                Icon(Icons.Default.ChevronLeft, null, tint = TextWhite.copy(0.6f), modifier = Modifier.size(32.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = carroAtual.marca.uppercase(),
                                    fontSize = 12.sp,
                                    color = TextWhite.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = carroAtual.nome,
                                    fontSize = 24.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitulo = if (carroAtual.tipoVeiculo == br.com.gui.carlembrete.TipoVeiculo.BICICLETA) {
                                    "Aro: ${carroAtual.modelo.ifBlank { "Nao informado" }}"
                                } else {
                                    carroAtual.modelo.ifBlank { "Padrao" }
                                }
                                Text(
                                    text = subtitulo,
                                    fontSize = 14.sp,
                                    color = TextWhite.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(onClick = onNextCar) {
                                Icon(Icons.Default.ChevronRight, null, tint = TextWhite.copy(0.6f), modifier = Modifier.size(32.dp))
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

                            val logoRes = carroAtual.logoResOrNull()
                            if (logoRes != null) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    colorFilter = ColorFilter.tint(TextWhite)
                                )
                            } else {
                                Icon(
                                    imageVector = carroAtual.tipoIcon(),
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }

                        // RODAPÃ‰ DO DISPLAY (Barra de Status Glassmorphic)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Info Mantedor
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Person, null, tint = baseColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (nomeMantedor.isNotBlank()) nomeMantedor else carroAtual.proprietario.split(" ").first(),
                                    color = TextWhite.copy(0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (carroAtual.tipoVeiculo != br.com.gui.carlembrete.TipoVeiculo.BICICLETA) {
                                // Separador vertical
                                Box(modifier = Modifier.width(1.dp).height(12.dp).background(TextWhite.copy(0.2f)))
                            }

                            if (carroAtual.tipoVeiculo != br.com.gui.carlembrete.TipoVeiculo.BICICLETA) {
                                // Info KM
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Speed, null, tint = baseColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (carroAtual.kmAtual > 0) "${carroAtual.kmAtual} KM" else "--",
                                        color = TextWhite.copy(0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = AccentBlue.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
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

// Sub-componente de decoraÃ§Ãµes
@Composable
fun DecoracoesDeFundo(color: Color) {
    Box(Modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Rounded.WaterDrop,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.03f),
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = (-30).dp)
        )
        Icon(
            imageVector = Icons.Rounded.Build,
            contentDescription = null,
            tint = color.copy(alpha = 0.05f),
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .rotate(-20f)
        )
    }
}

@Composable
fun ActionGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            // Um tom ligeiramente diferente do fundo sÃ³lido para destacar o botÃ£o
            containerColor = Color(0xFF334155).copy(alpha = 0.5f),
            contentColor = TextGray
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
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
                modifier = Modifier.size(18.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

