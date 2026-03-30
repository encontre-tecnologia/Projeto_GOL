package br.com.gui.carlembrete

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingScreen() {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    val bg = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF071022), Color(0xFF0B1A36), Color(0xFF0A1429))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF2F7FF), Color(0xFFEAF2FF))
        )
    }

    val titleColor = if (isDark) Color(0xFFEAF1FF) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF9BB0D3) else Color(0xFF5B6B86)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Logo Zellu",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(CornerSize(18.dp)))
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "SEJA BEM-VINDO",
                color = subtitleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zellu",
                color = titleColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Aguarde",
                color = subtitleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
