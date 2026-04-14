package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBeneficiosScreen(
    onDismiss: () -> Unit,
    onSubscribeNow: (SubscriptionPlan) -> Unit,
    showBackButton: Boolean = true,
    showSubscribeButton: Boolean = true,
    onPlanSelected: ((SubscriptionPlan) -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Cores Premium
    val goldStart = Color(0xFFFFD700)
    val goldEnd = Color(0xFFD4A017)
    val goldGradient = Brush.horizontalGradient(listOf(goldStart, goldEnd))
    val bgSurface = if (isDark) Color.Black else MaterialTheme.colorScheme.surface

    // Scroll state para a coluna principal
    val scrollState = rememberScrollState()
    var selectedPlan by rememberSaveable { mutableStateOf(SubscriptionPlan.FROTA) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgSurface)
                .padding(innerPadding)
                .verticalScroll(scrollState) // Permite rolar se a tela for pequena
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Text(
                    text = tr("Zellu Premium", "Zellu Premium"),
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tr("Escolha seu plano", "Choose your plan"),
                    color = goldStart,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    tr("2 opções para o seu momento", "2 options for your moment"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedPlan = SubscriptionPlan.LITE
                        onPlanSelected?.invoke(SubscriptionPlan.LITE)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    if (selectedPlan == SubscriptionPlan.LITE) 1.8.dp else 1.dp,
                    if (selectedPlan == SubscriptionPlan.LITE) Color(0xFF60A5FA) else Color(0xFF3B82F6).copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        tr("Plano Lite", "Lite Plan"),
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)) { append("R$ ") }
                            withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)) { append("10,50") }
                            withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)) { append(tr(" /mês", " /month")) }
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        tr("7 dias grátis para testar", "7 free days to try"),
                        color = Color(0xFF86EFAC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        tr("Inclui apenas o módulo Viagens.", "Includes only the Trips module."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            tr("• Registro de viagens", "• Trip logging"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            tr("• Controle de custos por rota", "• Route cost tracking"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            tr("• Histórico completo de viagens", "• Full trip history"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedPlan = SubscriptionPlan.FROTA
                        onPlanSelected?.invoke(SubscriptionPlan.FROTA)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF111827) else MaterialTheme.colorScheme.surface
                ),
                border = if (selectedPlan == SubscriptionPlan.FROTA) {
                    BorderStroke(1.8.dp, goldGradient)
                } else {
                    BorderStroke(1.5.dp, Color(0xFF64748B))
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(goldGradient, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            tr("COMPLETO", "COMPLETE"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        tr("Plano Frota", "Fleet Plan"),
                        color = goldStart,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)) { append("R$ ") }
                            withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)) { append("29,90") }
                            withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)) { append(tr(" /mês", " /month")) }
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        tr("7 dias grátis para testar", "7 free days to try"),
                        color = Color(0xFF86EFAC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        tr("Inclui tudo: Viagens + gestão de frota + sistema de estoque.", "Includes everything: Trips + fleet management + stock system."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            tr("• Tudo do plano Lite", "• Everything from Lite"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            tr("• Visão geral da frota", "• Fleet overview"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            tr("• Sistema de estoque completo", "• Full stock system"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            tr("• Gestão avançada e relatórios", "• Advanced management and reports"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (showSubscribeButton) {
                Button(
                    onClick = { onSubscribeNow(selectedPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(goldGradient, shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (selectedPlan) {
                                SubscriptionPlan.LITE -> tr("ASSINAR LITE • 7 DIAS GRÁTIS", "SUBSCRIBE LITE • 7 FREE DAYS")
                                SubscriptionPlan.FROTA -> tr("ASSINAR FROTA • 7 DIAS GRÁTIS", "SUBSCRIBE FLEET • 7 FREE DAYS")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


