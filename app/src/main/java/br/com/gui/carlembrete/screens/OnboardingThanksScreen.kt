package br.com.gui.carlembrete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OnboardingThanksScreen(onGoToHome: () -> Unit) {
    var showHero     by remember { mutableStateOf(false) }
    var showTitle    by remember { mutableStateOf(false) }
    var showDesc     by remember { mutableStateOf(false) }
    var showCards    by remember { mutableStateOf(false) }
    var showButton   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHero   = true;  delay(140)
        showTitle  = true;  delay(110)
        showDesc   = true;  delay(160)
        showCards  = true;  delay(180)
        showButton = true
    }

    val inf = rememberInfiniteTransition(label = "thanks")
    val ring1 by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring1"
    )
    val ring2 by inf.animateFloat(
        1f, 0f,
        infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Reverse),
        label = "ring2"
    )
    val glow by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05101F), Color(0xFF030B15))
                )
            )
    ) {
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(380.dp)
                .offset(x = (-60 + ring1 * 40).dp, y = (-80 + ring2 * 60).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF10B981).copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (40 - ring2 * 30).dp, y = (60 + ring1 * 40).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF3B82F6).copy(alpha = 0.09f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(36.dp))

                // Hero: layered glow rings + check icon
                AnimatedVisibility(
                    visible = showHero,
                    enter = fadeIn(tween(600, easing = EaseOutCubic)) +
                            slideInVertically(tween(600, easing = EaseOutCubic)) { it / 3 }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Outer ring
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer { alpha = 0.18f + ring1 * 0.07f }
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF34D399), Color(0xFF059669), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )
                        // Mid ring
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .graphicsLayer { alpha = 0.28f + ring2 * 0.10f }
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF047857), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )
                        // Core circle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .graphicsLayer { scaleX = 0.97f + glow * 0.04f; scaleY = 0.97f + glow * 0.04f }
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF34D399).copy(alpha = 0.30f), Color(0xFF065F46).copy(alpha = 0.55f))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Title block
                AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(tween(480)) + slideInVertically(tween(480)) { it / 6 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tudo pronto!",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.8).sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Bem-vindo ao Zellu",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF34D399),
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Description
                AnimatedVisibility(
                    visible = showDesc,
                    enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 8 }
                ) {
                    Text(
                        text = "Seu parceiro de garagem está pronto.\nVeja o que você tem a partir de agora:",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Feature cards
                AnimatedVisibility(
                    visible = showCards,
                    enter = fadeIn(tween(480)) + slideInVertically(tween(480)) { it / 5 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThanksFeatureCard(
                            number = "01",
                            icon = Icons.Default.NotificationsActive,
                            iconTint = Color(0xFF60A5FA),
                            iconBg = Color(0xFF1E3A5F),
                            accentLine = Color(0xFF3B82F6),
                            title = "Alertas Inteligentes",
                            subtitle = "IPVA, CNH, revisões e muito mais no momento certo."
                        )
                        ThanksFeatureCard(
                            number = "02",
                            icon = Icons.Default.DirectionsCar,
                            iconTint = Color(0xFF34D399),
                            iconBg = Color(0xFF064E3B),
                            accentLine = Color(0xFF10B981),
                            title = "Controle de Gastos",
                            subtitle = "Combustível, peças e serviços tudo registrado."
                        )
                        ThanksFeatureCard(
                            number = "03",
                            icon = Icons.Default.Star,
                            iconTint = Color(0xFFFBBF24),
                            iconBg = Color(0xFF451A03),
                            accentLine = Color(0xFFF59E0B),
                            title = "Valorização na Revenda",
                            subtitle = "Histórico impecável para o seu veículo."
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Pinned CTA button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 8 }
            ) {
                Button(
                    onClick = onGoToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text(
                        text = "Começar a usar",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThanksFeatureCard(
    number: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    accentLine: Color,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1929))
    ) {
        // Left accent line
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentLine, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = number,
                color = accentLine.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(0.7f)
            )
        }
    }
}
