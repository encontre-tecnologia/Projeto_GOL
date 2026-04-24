package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PBScreenBg = Color(0xFFF7FAFF)
private val PBCardBg = Color(0xFFFFFFFF)
private val PBGoldStart = Color(0xFFFFD700)
private val PBGoldEnd = Color(0xFFD4A017)
private val PBGoldGradient = Brush.horizontalGradient(listOf(PBGoldStart, PBGoldEnd))
private val PBTitleColor = Color(0xFF0F172A)
private val PBSubColor = Color(0xFF475569)
private val PBDimColor = Color(0xFF64748B)

@Composable
fun PremiumBeneficiosScreen(
    onDismiss: () -> Unit,
    onSubscribeNow: (SubscriptionPlan) -> Unit,
    showBackButton: Boolean = true,
    showSubscribeButton: Boolean = true,
    onPlanSelected: ((SubscriptionPlan) -> Unit)? = null
) {
    var selectedPlan by rememberSaveable { mutableStateOf(SubscriptionPlan.FROTA) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PBScreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = PBTitleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = tr("Zellu Premium", "Zellu Premium"),
                    color = PBTitleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // --- HERO ---
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFD4A017).copy(alpha = 0.3f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = PBGoldStart,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = tr("Escolha seu plano", "Choose your plan"),
                color = PBGoldStart,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tr("7 dias gratis • cancele quando quiser", "7 free days • cancel anytime"),
                color = Color(0xFF34D399),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // --- PLAN CARDS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlanCard(
                    name = tr("Plano Lite", "Lite Plan"),
                    price = "10,50",
                    tagLabel = null,
                    nameColor = Color(0xFF93C5FD),
                    borderColorSelected = Color(0xFF60A5FA),
                    borderColorIdle = Color(0xFFD7E6FF),
                    tagBg = Color.Transparent,
                    tagTextColor = Color.Transparent,
                    isSelected = selectedPlan == SubscriptionPlan.LITE,
                    description = tr("Modulo de viagens com custos por rota.", "Trip module with route cost tracking."),
                    features = listOf(
                        tr("Registro de viagens", "Trip logging"),
                        tr("Controle de custos por rota", "Route cost tracking"),
                        tr("Historico completo de viagens", "Full trip history")
                    ),
                    featureColor = Color(0xFF93C5FD),
                    onClick = {
                        selectedPlan = SubscriptionPlan.LITE
                        onPlanSelected?.invoke(SubscriptionPlan.LITE)
                    }
                )

                PlanCard(
                    name = tr("Plano Frota", "Fleet Plan"),
                    price = "29,90",
                    tagLabel = tr("COMPLETO", "COMPLETE"),
                    nameColor = PBGoldStart,
                    borderColorSelected = PBGoldStart,
                    borderColorIdle = Color(0xFFFFE7A3),
                    tagBg = Color(0xFFFFF3CC),
                    tagTextColor = Color(0xFF7A5900),
                    isSelected = selectedPlan == SubscriptionPlan.FROTA,
                    description = tr("Viagens, gestao de frota e estoque.", "Trips, fleet management and stock."),
                    features = listOf(
                        tr("Tudo do plano Lite", "Everything from Lite"),
                        tr("Visao geral da frota", "Fleet overview"),
                        tr("Sistema de estoque completo", "Full stock system"),
                        tr("Gestao avancada e relatorios", "Advanced management")
                    ),
                    featureColor = PBGoldStart,
                    onClick = {
                        selectedPlan = SubscriptionPlan.FROTA
                        onPlanSelected?.invoke(SubscriptionPlan.FROTA)
                    }
                )

                PlanCard(
                    name = tr("Plano Enterprise", "Enterprise Plan"),
                    price = "59,90",
                    tagLabel = tr("MAXIMO", "MAX"),
                    nameColor = Color(0xFF22D3EE),
                    borderColorSelected = Color(0xFF22D3EE),
                    borderColorIdle = Color(0xFFBFEFF7),
                    tagBg = Color(0xFFE6FAFE),
                    tagTextColor = Color(0xFF0E7490),
                    isSelected = selectedPlan == SubscriptionPlan.ENTERPRISE,
                    description = tr("Tudo do Frota com mais capacidade de veiculos.", "Everything from Fleet with more vehicle capacity."),
                    features = listOf(
                        tr("Tudo do plano Frota", "Everything from Fleet"),
                        tr("Viagens e custos avancados", "Advanced trips and costs"),
                        tr("Estoque para operacao em escala", "Stock for large operations"),
                        tr("Maior capacidade de veiculos", "Highest vehicle capacity")
                    ),
                    featureColor = Color(0xFF22D3EE),
                    onClick = {
                        selectedPlan = SubscriptionPlan.ENTERPRISE
                        onPlanSelected?.invoke(SubscriptionPlan.ENTERPRISE)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- CTA ---
            if (showSubscribeButton) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PBGoldGradient)
                        .clickable { onSubscribeNow(selectedPlan) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedPlan) {
                            SubscriptionPlan.LITE -> tr("ASSINAR LITE • 7 DIAS GRATIS", "SUBSCRIBE LITE • 7 FREE DAYS")
                            SubscriptionPlan.FROTA -> tr("ASSINAR FROTA • 7 DIAS GRATIS", "SUBSCRIBE FLEET • 7 FREE DAYS")
                            SubscriptionPlan.ENTERPRISE -> tr("ASSINAR ENTERPRISE • 7 DIAS GRATIS", "SUBSCRIBE ENTERPRISE • 7 FREE DAYS")
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF1A0800),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = tr("Renovacao automatica. Cancele a qualquer momento.", "Auto-renewal. Cancel anytime."),
                    color = PBDimColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanCard(
    name: String,
    price: String,
    tagLabel: String?,
    nameColor: Color,
    borderColorSelected: Color,
    borderColorIdle: Color,
    tagBg: Color,
    tagTextColor: Color,
    isSelected: Boolean,
    description: String,
    features: List<String>,
    featureColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) borderColorSelected else borderColorIdle
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PBCardBg)
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(name, color = nameColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (tagLabel != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tagBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(tagLabel, color = tagTextColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("R$", color = PBSubColor, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = PBTitleColor)) { append(price) }
                },
                color = PBTitleColor
            )
            Text(tr("/mes", "/mo"), color = PBDimColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        }

        Text(description, color = PBSubColor, fontSize = 13.sp, lineHeight = 18.sp)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = featureColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(feature, color = PBTitleColor, fontSize = 13.sp)
                }
            }
        }
    }
}
