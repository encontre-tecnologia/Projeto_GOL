import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BikeDistanceCard(
    kmHoje: Double,
    kmSemana: Double,
    kmMes: Double,
    kmTotal: Double,
    onRegistrar: () -> Unit,
    onHistorico: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bikeCardBackground = if (isDark) Color(0xFF2B3950) else Color.White
    val bikeAccent = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
    val bikeTextPrimary = scheme.onSurface
    val bikeTextSecondary = scheme.onSurfaceVariant
    val bikeInnerBackground = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.75f)
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.4f))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = bikeCardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .background(bikeCardBackground)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsBike,
                                contentDescription = null,
                                tint = bikeAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distancia Pedalada",
                            color = bikeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Registre suas voltas e km",
                            color = bikeTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DistanceCompactItem(
                        label = "Hoje",
                        value = kmHoje,
                        icon = Icons.Rounded.Today,
                        color = Color(0xFF60A5FA),
                        textPrimary = bikeTextPrimary,
                        textSecondary = bikeTextSecondary,
                        innerBackground = bikeInnerBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    DistanceCompactItem(
                        label = "Semana",
                        value = kmSemana,
                        icon = Icons.Rounded.Event,
                        color = Color(0xFF34D399),
                        textPrimary = bikeTextPrimary,
                        textSecondary = bikeTextSecondary,
                        innerBackground = bikeInnerBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    DistanceCompactItem(
                        label = "Mes",
                        value = kmMes,
                        icon = Icons.Rounded.Route,
                        color = Color(0xFFF59E0B),
                        textPrimary = bikeTextPrimary,
                        textSecondary = bikeTextSecondary,
                        innerBackground = bikeInnerBackground,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bikeInnerBackground)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", color = bikeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatKm(kmTotal),
                            color = bikeTextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "km",
                        color = bikeTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRegistrar,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bikeAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Registrar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onHistorico,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = bikeTextPrimary)
                    ) {
                        Text("Historico", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceCompactItem(
    label: String,
    value: Double,
    icon: ImageVector,
    color: Color,
    textPrimary: Color,
    textSecondary: Color,
    innerBackground: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(innerBackground)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatKm(value),
            color = textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

private fun formatKm(value: Double): String {
    return if (value == 0.0) "0.0" else String.format("%.1f", value)
}
