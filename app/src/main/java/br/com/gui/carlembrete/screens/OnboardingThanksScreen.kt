package br.com.gui.carlembrete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
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
    var showIcon by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showDesc by remember { mutableStateOf(false) }
    var showFeature1 by remember { mutableStateOf(false) }
    var showFeature2 by remember { mutableStateOf(false) }
    var showFeature3 by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showIcon = true
        delay(100)
        showTitle = true
        delay(100)
        showDesc = true
        delay(120)
        showFeature1 = true
        delay(80)
        showFeature2 = true
        delay(80)
        showFeature3 = true
        delay(120)
        showButton = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "thanks")
    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    val blob1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val blob2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020917))
    ) {
        // Animated background blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (100 + blob1 * 50).dp, y = (-50 + blob1 * 60).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF10B981).copy(alpha = 0.18f), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-60 + blob2 * 40).dp, y = (400 + blob2 * 80).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.15f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(0.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Icon
                AnimatedVisibility(
                    visible = showIcon,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { scaleX = iconPulse; scaleY = iconPulse }
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF10B981).copy(alpha = 0.25f), Color(0xFF065F46).copy(alpha = 0.4f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Title
                AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(tween(440)) + slideInVertically(tween(440)) { it / 6 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tudo pronto!",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Obrigado por escolher o Zellu",
                            fontSize = 15.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                // Description
                AnimatedVisibility(
                    visible = showDesc,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 }
                ) {
                    Text(
                        text = "Seu parceiro de garagem está pronto.\nVeja o que você ganha a partir de agora:",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Feature rows
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedVisibility(
                        visible = showFeature1,
                        enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 6 }
                    ) {
                        ThanksFeatureRow(
                            icon = Icons.Default.NotificationsActive,
                            iconTint = Color(0xFF60A5FA),
                            iconBg = Color(0xFF1E3A5F),
                            title = "Alertas Inteligentes",
                            subtitle = "IPVA, CNH, revisões e muito mais."
                        )
                    }
                    AnimatedVisibility(
                        visible = showFeature2,
                        enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 6 }
                    ) {
                        ThanksFeatureRow(
                            icon = Icons.Default.DirectionsCar,
                            iconTint = Color(0xFF34D399),
                            iconBg = Color(0xFF064E3B),
                            title = "Controle de Gastos",
                            subtitle = "Combustível, peças e serviços registrados."
                        )
                    }
                    AnimatedVisibility(
                        visible = showFeature3,
                        enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 6 }
                    ) {
                        ThanksFeatureRow(
                            icon = Icons.Default.Star,
                            iconTint = Color(0xFFFBBF24),
                            iconBg = Color(0xFF451A03),
                            title = "Valorização na Revenda",
                            subtitle = "Histórico impecável para o seu carro."
                        )
                    }
                }
            }

            // CTA button
            Column {
                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn(tween(340)) + slideInVertically(tween(340)) { it / 8 }
                ) {
                    Button(
                        onClick = onGoToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Começar a usar",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ThanksFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1B2E))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = subtitle, color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}
