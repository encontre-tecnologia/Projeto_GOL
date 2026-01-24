import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Build
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.CarroInfo
import br.com.gui.carlembrete.logoResOrNull
import br.com.gui.carlembrete.tipoIconRes

// --- PALETA ZELLU ---
private val GradientStart = Color(0xFF334155) // Slate 700 (Fundo do Card Maior)
private val GradientEnd = Color(0xFF1E293B)   // Slate 800
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val SurfaceDark = Color(0xFF0F172A)

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                val baseColor = carroAtual.getCorUI()
                val heroStart = lerp(Color(0xFF0F172A), baseColor, 0.55f)
                val heroEnd = lerp(Color(0xFF0B1224), baseColor, 0.35f)

                // 1. HERO CARD DO CARRO (usa a cor do carro)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(heroStart, heroEnd),
                                start = Offset(0f, 0f),
                                end = Offset(0f, Float.POSITIVE_INFINITY)
                            )
                        )
                        .clickable { onOpenCarInfo() }
                ) {
                    // --- ESTAMPA DE FUNDO (Marcas d'água sutis) ---
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.03f),
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-20).dp, y = (-20).dp)
                            .rotate(15f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.03f),
                        modifier = Modifier
                            .size(140.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 30.dp, y = 30.dp)
                            .rotate(-25f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.02f),
                        modifier = Modifier
                            .size(90.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 10.dp, y = 40.dp)
                            .rotate(-10f)
                    )

                    // --- CONTEÚDO DO CARRO ---
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
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
                                    tint = TextWhite.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = carroAtual.marca.uppercase(),
                                    fontSize = 12.sp,
                                    color = TextWhite.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = carroAtual.nome,
                                    fontSize = 22.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            IconButton(onClick = onNextCar) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = TextWhite.copy(alpha = 0.7f),
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
                                modifier = Modifier.size(80.dp),
                                colorFilter = ColorFilter.tint(TextWhite)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = carroAtual.tipoIconRes()),
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(80.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = carroAtual.modelo.ifBlank { "Modelo padrão" },
                                fontSize = 16.sp,
                                color = TextWhite.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(4.dp))

                            // Badge Proprietário - Fundo escuro transparente para legibilidade
                            Spacer(Modifier.height(6.dp))
                            Card(
                                shape = RoundedCornerShape(50),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (carroAtual.proprietario.isBlank()) "Carro de: não informado" else "Carro de: ${carroAtual.proprietario}",
                                    fontSize = 12.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2. AÇÕES RÁPIDAS (Estilo Glass)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                // 3. BOTÃO PRIMÁRIO (Mantém o destaque azul vibrante só aqui)
                Button(
                    onClick = onNovoLembrete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Rounded.Event, null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Novo Lembrete",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Helper para botão transparente (Glass Effect)
@Composable
fun ActionGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceDark,
            contentColor = TextGray
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextGray
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
