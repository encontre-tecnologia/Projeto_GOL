package br.com.gui.carlembrete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OnboardingPremiumWelcomeScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit = onNext
) {
    var showIcon by remember { mutableStateOf(false) }
    var showTitles by remember { mutableStateOf(false) }
    var showDesc by remember { mutableStateOf(false) }
    var showBenefits by remember { mutableStateOf(false) }
    var showPrice by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "premium_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premium_pulse_scale"
    )

    LaunchedEffect(Unit) {
        showIcon = true
        delay(100)
        showTitles = true
        delay(90)
        showDesc = true
        delay(90)
        showBenefits = true
        delay(90)
        showPrice = true
        delay(100)
        showButtons = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B2240),
                        Color(0xFF0F2A4A),
                        Color(0xFF102D52)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(2.dp))

                AnimatedVisibility(
                    visible = showIcon,
                    enter = fadeIn(animationSpec = tween(480)) +
                        scaleIn(animationSpec = tween(480), initialScale = 0.92f) +
                        slideInVertically(animationSpec = tween(480), initialOffsetY = { it / 6 })
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x33FACC15),
                                        Color(0x22F59E0B),
                                        Color(0x00000000)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.premium),
                            contentDescription = "Zellu Premium",
                            modifier = Modifier.size(52.dp),
                            colorFilter = ColorFilter.tint(Color(0xFFFACC15))
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showTitles,
                    enter = fadeIn(animationSpec = tween(420)) +
                        slideInVertically(animationSpec = tween(420), initialOffsetY = { it / 8 })
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ZELLU PREMIUM",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Teste grátis por 7 dias",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                AnimatedVisibility(
                    visible = showDesc,
                    enter = fadeIn(animationSpec = tween(400)) +
                        slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 10 })
                ) {
                    Text(
                        text = "Ative seu teste e aproveite todos os recursos Premium para cuidar melhor dos seus veículos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showBenefits,
                    enter = fadeIn(animationSpec = tween(380)) +
                        slideInVertically(animationSpec = tween(380), initialOffsetY = { it / 12 })
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A304F)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x3360A5FA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            PremiumBenefitLine(
                                icon = Icons.Default.Route,
                                title = "Controle de Viagens",
                                desc = "Viagens ilimitadas com gastos e relatorios."
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = showPrice,
                    enter = fadeIn(animationSpec = tween(360)) +
                        slideInVertically(animationSpec = tween(360), initialOffsetY = { it / 12 })
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A304F)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x3360A5FA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Depois: R$ 9,90/mês",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "A cobrança inicia ao fim do teste.",
                                color = Color(0xFFA9BBD1),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Cancele quando quiser na Play Store.",
                                color = Color(0xFFA9BBD1),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(animationSpec = tween(340)) +
                    slideInVertically(animationSpec = tween(340), initialOffsetY = { it / 10 })
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text(
                            text = "Iniciar teste grátis",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF335A86)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB9D4F5))
                    ) {
                        Text("Agora não", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBenefitLine(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFACC15),
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = desc,
                color = Color(0xFFA9BBD1),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
