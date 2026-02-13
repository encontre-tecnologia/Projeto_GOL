package br.com.gui.carlembrete

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumHubScreen(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onOpenGuardian: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenSubscribe: () -> Unit
) {
    val view = LocalView.current
    val textPrimary = Color(0xFF3A2500)
    val textDim = Color(0xFF7A5A1F)
    val accentGold = Color(0xFFD4A017)
    val borderColor = Color(0xFFE9C46A)
    val cardSurface = Color.White
    val screenBg = cardSurface
    val featureCardSurface = Color(0xFFFFFEF8)

    DisposableEffect(view) {
        val activity = view.context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldStatusColor = window?.statusBarColor
        val oldLightStatus = insetsController?.isAppearanceLightStatusBars

        if (window != null && insetsController != null) {
            window.statusBarColor = screenBg.toArgb()
            insetsController.isAppearanceLightStatusBars = true
        }

        onDispose {
            if (window != null && insetsController != null) {
                if (oldStatusColor != null) window.statusBarColor = oldStatusColor
                if (oldLightStatus != null) insetsController.isAppearanceLightStatusBars = oldLightStatus
            }
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zellu Premium", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cardSurface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, borderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = accentGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("Central Premium", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Text(
                            if (isPremium) {
                                "Plano Premium ativo. Todos os recursos desta tela estao liberados."
                            } else {
                                "Esta e a tela de recursos Premium do Zellu."
                            },
                            color = textDim,
                            fontSize = 13.sp
                        )
                    }
                }

                PremiumFeatureCard(
                    title = "Zellu Guardiao",
                    subtitle = "Protecao e alertas inteligentes",
                    iconColor = accentGold,
                    textPrimary = textPrimary,
                    textDim = textDim,
                    borderColor = borderColor,
                    cardSurface = featureCardSurface,
                    onClick = onOpenGuardian
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = accentGold)
                }

                PremiumFeatureCard(
                    title = "Gestor de Frota",
                    subtitle = "Visao de gastos e relatorios",
                    iconColor = Color(0xFF7C3AED),
                    textPrimary = textPrimary,
                    textDim = textDim,
                    borderColor = borderColor,
                    cardSurface = featureCardSurface,
                    onClick = onOpenFinance
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF7C3AED))
                }

                PremiumFeatureCard(
                    title = "Viagem",
                    subtitle = "Gastos, notas e relatorio",
                    iconColor = Color(0xFFEA580C),
                    textPrimary = textPrimary,
                    textDim = textDim,
                    borderColor = borderColor,
                    cardSurface = featureCardSurface,
                    onClick = onOpenAiAssistant
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFEA580C))
                }

            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    title: String,
    subtitle: String,
    iconColor: Color,
    textPrimary: Color,
    textDim: Color,
    borderColor: Color,
    cardSurface: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = textDim, fontSize = 12.sp)
            }
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = textDim, modifier = Modifier.size(16.dp))
        }
    }
}

