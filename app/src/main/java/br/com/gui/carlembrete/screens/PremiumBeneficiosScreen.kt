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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    var planPrices by remember { mutableStateOf(RemotePlanPricing.defaultPrices) }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color.Black else PBScreenBg
    val cardBg = if (isDark) Color(0xFF0F172A) else PBCardBg
    val titleColor = if (isDark) Color(0xFFF8FAFC) else PBTitleColor
    val subColor = if (isDark) Color(0xFFCBD5E1) else PBSubColor
    val dimColor = if (isDark) Color(0xFF94A3B8) else PBDimColor
    val goldTitle = if (isDark) Color(0xFFFFD85A) else PBGoldStart

    DisposableEffect(Unit) {
        val listener = RemotePlanPricing.listen { prices ->
            planPrices = prices
        }
        onDispose { listener.remove() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = tr("Zellu Premium", "Zellu Premium"),
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(PBGoldEnd.copy(alpha = if (isDark) 0.38f else 0.30f), Color.Transparent))),
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
                text = tr("Deixe sua garagem mais inteligente", "Make your garage smarter"),
                color = goldTitle,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                lineHeight = 31.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = tr("IA, avisos, viagens e relatorios em um so lugar.", "AI, reminders, trips and reports in one place."),
                color = subColor,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr("7 dias gratis - cancele quando quiser", "7 free days - cancel anytime"),
                color = Color(0xFF059669),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            PremiumAiSpotlightCard(
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlanCard(
                    name = tr("Plano Lite", "Lite Plan"),
                    price = planPrices.priceFor(SubscriptionPlan.LITE),
                    tagLabel = tr("IA INCLUSA", "AI INCLUDED"),
                    nameColor = Color(0xFF2563EB),
                    borderColorSelected = Color(0xFF2563EB),
                    borderColorIdle = if (isDark) Color(0xFF1E3A8A) else Color(0xFFD7E6FF),
                    tagBg = if (isDark) Color(0xFF172554) else Color(0xFFEAF2FF),
                    tagTextColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                    isSelected = selectedPlan == SubscriptionPlan.LITE,
                    cardBg = cardBg,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
                    description = tr("Para quem quer a IA ajudando no dia a dia do carro.", "For users who want AI helping with daily vehicle care."),
                    features = listOf(
                        tr("Pergunte sobre avisos, consumo, pneus, oleo e viagem", "Ask about reminders, fuel, tires, oil and trips"),
                        tr("Crie avisos e registros conversando com a Zellu AI", "Create reminders and records by chatting with Zellu AI"),
                        tr("Veja se o veiculo parece bom para viajar", "Check if the vehicle looks ready for a trip"),
                        tr("Controle custos e historico de viagens", "Track trip costs and history")
                    ),
                    featureColor = Color(0xFF2563EB),
                    onClick = {
                        selectedPlan = SubscriptionPlan.LITE
                        onPlanSelected?.invoke(SubscriptionPlan.LITE)
                    }
                )

                PlanCard(
                    name = tr("Plano Frota", "Fleet Plan"),
                    price = planPrices.priceFor(SubscriptionPlan.FROTA),
                    tagLabel = tr("MAIS ESCOLHIDO", "MOST PICKED"),
                    nameColor = PBGoldEnd,
                    borderColorSelected = PBGoldEnd,
                    borderColorIdle = if (isDark) Color(0xFF854D0E) else Color(0xFFFFE7A3),
                    tagBg = if (isDark) Color(0xFF422006) else Color(0xFFFFF3CC),
                    tagTextColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF7A5900),
                    isSelected = selectedPlan == SubscriptionPlan.FROTA,
                    cardBg = cardBg,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
                    description = tr("Para controlar varios veiculos sem se perder.", "For managing several vehicles without getting lost."),
                    features = listOf(
                        tr("Tudo do plano Lite", "Everything from Lite"),
                        tr("Zellu AI comparando todos os veiculos da garagem", "Zellu AI comparing all vehicles in the garage"),
                        tr("Resumo da frota e prioridade de revisao", "Fleet summary and revision priority"),
                        tr("Relatorios para compartilhar em poucos toques", "Shareable reports in a few taps"),
                        tr("Estoque completo para pecas, produtos e operacao", "Full stock for parts, products and operations")
                    ),
                    featureColor = PBGoldEnd,
                    onClick = {
                        selectedPlan = SubscriptionPlan.FROTA
                        onPlanSelected?.invoke(SubscriptionPlan.FROTA)
                    }
                )

                PlanCard(
                    name = tr("Plano Enterprise", "Enterprise Plan"),
                    price = planPrices.priceFor(SubscriptionPlan.ENTERPRISE),
                    tagLabel = tr("MAXIMO", "MAX"),
                    nameColor = Color(0xFF0891B2),
                    borderColorSelected = Color(0xFF0891B2),
                    borderColorIdle = if (isDark) Color(0xFF155E75) else Color(0xFFBFEFF7),
                    tagBg = if (isDark) Color(0xFF083344) else Color(0xFFE6FAFE),
                    tagTextColor = if (isDark) Color(0xFF67E8F9) else Color(0xFF0E7490),
                    isSelected = selectedPlan == SubscriptionPlan.ENTERPRISE,
                    cardBg = cardBg,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
                    description = tr("Para operacao maior, com mais veiculos e mais controle.", "For larger operations with more vehicles and more control."),
                    features = listOf(
                        tr("Tudo do plano Frota", "Everything from Fleet"),
                        tr("Zellu AI para uma garagem maior", "Zellu AI for a larger garage"),
                        tr("Mais capacidade de veiculos cadastrados", "More registered vehicle capacity"),
                        tr("Gestao, estoque e relatorios em escala", "Management, stock and reports at scale"),
                        tr("Mais organizacao para crescer sem bagunca", "More organization to grow without mess")
                    ),
                    featureColor = Color(0xFF0891B2),
                    onClick = {
                        selectedPlan = SubscriptionPlan.ENTERPRISE
                        onPlanSelected?.invoke(SubscriptionPlan.ENTERPRISE)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

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
                            SubscriptionPlan.LITE -> tr("COMECAR LITE - 7 DIAS GRATIS", "START LITE - 7 FREE DAYS")
                            SubscriptionPlan.FROTA -> tr("COMECAR FROTA - 7 DIAS GRATIS", "START FLEET - 7 FREE DAYS")
                            SubscriptionPlan.ENTERPRISE -> tr("COMECAR ENTERPRISE - 7 DIAS GRATIS", "START ENTERPRISE - 7 FREE DAYS")
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF1A0800),
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = tr("Teste primeiro. Se nao fizer sentido para sua garagem, cancele quando quiser.", "Try it first. If it does not fit your garage, cancel anytime."),
                    color = dimColor,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PremiumAiSpotlightCard(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    if (isDark) {
                        listOf(Color(0xFF111827), Color(0xFF020617))
                    } else {
                        listOf(Color(0xFF0F172A), Color(0xFF111827))
                    }
                )
            )
            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PBGoldStart,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = tr("Agora com Zellu AI", "Now with Zellu AI"),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Text(
                text = tr(
                    "Pergunte como esta sua garagem, crie avisos, registre servicos e receba respostas simples sobre mecanica.",
                    "Ask about your garage, create reminders, register services and get simple maintenance answers."
                ),
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
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
    cardBg: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) borderColorSelected else borderColorIdle
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val selectedBg = if (isSelected) featureColor.copy(alpha = 0.10f) else cardBg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(selectedBg)
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = name,
                color = nameColor,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (tagLabel != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tagBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(tagLabel, color = tagTextColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("R$", color = subColor, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = titleColor)) {
                        append(price)
                    }
                },
                color = titleColor
            )
            Text(tr("/mes", "/mo"), color = dimColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        }

        Text(description, color = subColor, fontSize = 13.sp, lineHeight = 18.sp)

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = featureColor,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = feature,
                        color = titleColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (isSelected) {
            Text(
                text = tr("Selecionado", "Selected"),
                color = featureColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}
