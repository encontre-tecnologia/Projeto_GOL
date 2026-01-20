package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingScreen(progress: Float) {
    // Cores Tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBackgroundStart = Color(0xFF0F172A)
    val darkBackgroundEnd = Color(0xFF0B1224)
    val iconColor = Color.White.copy(alpha = 0.12f) // Ícones bem sutis no fundo

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(darkBackgroundStart, darkBackgroundEnd)
                    )
                )
        ) {
            // --- Camada de Fundo: ?cones est?ticos espalhados ---
            Box(modifier = Modifier.fillMaxSize()) {
                BackgroundIcon(Icons.Rounded.Settings, 90.dp, iconColor, Alignment.TopStart, offsetX = (-18).dp, offsetY = 36.dp)
                BackgroundIcon(Icons.Rounded.Build, 70.dp, iconColor, Alignment.TopEnd, offsetX = 24.dp, offsetY = 120.dp)
                BackgroundIcon(Icons.Rounded.DirectionsCar, 80.dp, iconColor, Alignment.CenterStart, offsetX = (-28).dp, offsetY = (-8).dp)
                BackgroundIcon(Icons.Rounded.SettingsSuggest, 72.dp, iconColor, Alignment.BottomEnd, offsetX = 24.dp, offsetY = (-90).dp)
                BackgroundIcon(Icons.Rounded.Settings, 96.dp, iconColor, Alignment.BottomStart, offsetX = (-30).dp, offsetY = 36.dp)
                BackgroundIcon(Icons.Rounded.Build, 64.dp, iconColor, Alignment.TopCenter, offsetX = 0.dp, offsetY = (-10).dp)
                BackgroundIcon(Icons.Rounded.DirectionsCar, 72.dp, iconColor, Alignment.CenterEnd, offsetX = 22.dp, offsetY = 6.dp)
                BackgroundIcon(Icons.Rounded.SettingsSuggest, 60.dp, iconColor, Alignment.Center, offsetX = 0.dp, offsetY = 64.dp)
                BackgroundIcon(Icons.Rounded.Settings, 68.dp, iconColor, Alignment.CenterEnd, offsetX = 8.dp, offsetY = 120.dp)
                BackgroundIcon(Icons.Rounded.Build, 58.dp, iconColor, Alignment.BottomCenter, offsetX = (-10).dp, offsetY = 10.dp)
            }

            // --- Camada Central: Conteúdo Principal ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Nome do App em destaque
                Text(
                    text = "ZELLU",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )

                // Subtítulo temático
                Text(
                    text = "Manutenção inteligente",
                    color = primaryBlue.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(40.dp))

                // Barra de progresso moderna
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(200.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = primaryBlue,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Carregando...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// Componente auxiliar para posicionar os ícones de fundo
@Composable
fun BoxScope.BackgroundIcon(
    icon: ImageVector,
    size: Dp,
    tint: Color,
    alignment: Alignment,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .rotate(if(size > 100.dp) -15f else 30f) // Uma leve rotação inicial para não ficarem retos
    )
}
