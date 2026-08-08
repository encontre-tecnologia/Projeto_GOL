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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

// O preco exibido aqui vem do Google Play (PlayPlanPrices), que e quem cobra.
// Nao existe preco escrito nesta tela: reajuste no Play Console aparece sem release.

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
    val context = LocalContext.current
    // Telas que so exibem o catalogo (onboarding) nao tem SubscriptionManager ativo,
    // entao garantimos a consulta de preco aqui tambem.
    LaunchedEffect(Unit) { PlayPlanPrices.ensureLoaded(context) }
    val playPrices by PlayPlanPrices.pricesByProductId.collectAsState()
    // O Play só devolve oferta que este usuário pode usar, então trial ausente aqui
    // significa trial indisponível (já usou antes) — e a tela para de prometer.
    val trialDays = playPrices[selectedPlan.productId]?.freeTrialDays() ?: 0
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color.Black else PBScreenBg
    val cardBg = if (isDark) Color(0xFF0F172A) else PBCardBg
    val titleColor = if (isDark) Color(0xFFF8FAFC) else PBTitleColor
    val subColor = if (isDark) Color(0xFFCBD5E1) else PBSubColor
    val dimColor = if (isDark) Color(0xFF94A3B8) else PBDimColor
    val goldTitle = if (isDark) Color(0xFFFFD85A) else PBGoldStart

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
                text = tr("Avisos, viagens, custos e relatorios em um so lugar.", "Reminders, trips, costs and reports in one place."),
                color = subColor,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                // Só promete teste gratis quando o Play confirma trial para este usuario.
                text = if (trialDays > 0) {
                    tr("$trialDays dias gratis - cancele quando quiser", "$trialDays free days - cancel anytime")
                } else {
                    tr("Cancele quando quiser", "Cancel anytime")
                },
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
                    price = playPrices[SubscriptionPlan.LITE.productId],
                    tagLabel = tr("USO PESSOAL", "PERSONAL USE"),
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
                    description = tr("Para cuidar melhor do veiculo e decidir com mais seguranca.", "For people who want to care for their vehicle and decide with confidence."),
                    features = listOf(
                        tr("Pergunte sobre avisos, consumo, pneus, oleo e viagem", "Ask about reminders, fuel, tires, oil and trips"),
                        tr("Crie avisos e registros escrevendo em linguagem natural", "Create reminders and records by typing in plain language"),
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
                    price = playPrices[SubscriptionPlan.FROTA.productId],
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
                    description = tr("Para quem administra varios veiculos e tambem participa de frotas por convite.", "For people managing multiple vehicles and joining invited fleets."),
                    features = listOf(
                        tr("Tudo do plano Lite", "Everything from Lite"),
                        tr("Analise avancada de custos, viagens e manutencao", "Advanced cost, trip and maintenance analysis"),
                        tr("Acesso a agenda corporativa quando for convidado", "Corporate schedule access when invited"),
                        tr("Reservas, QR Code e historico das viagens liberadas", "Reservations, QR codes and history for granted trips"),
                        tr("Avisos, documentos e registros completos por veiculo", "Complete alerts, documents and records per vehicle"),
                        tr("Comparacao automatica entre os veiculos da garagem", "Automatic comparison across your garage")
                    ),
                    featureColor = PBGoldEnd,
                    onClick = {
                        selectedPlan = SubscriptionPlan.FROTA
                        onPlanSelected?.invoke(SubscriptionPlan.FROTA)
                    }
                )

                PlanCard(
                    name = tr("Plano Enterprise", "Enterprise Plan"),
                    price = playPrices[SubscriptionPlan.ENTERPRISE.productId],
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
                    description = tr("Para empresas que precisam criar a propria frota e operar tudo em um painel.", "For companies that need to create their own fleet and operate from one dashboard."),
                    features = listOf(
                        tr("Tudo do plano Frota", "Everything from Fleet"),
                        tr("Crie sua frota e convide usuarios pelo painel corporativo", "Create your fleet and invite users from the corporate dashboard"),
                        tr("Agenda, QR Code, assinaturas e rastreamento em viagens", "Schedule, QR codes, signatures and trip tracking"),
                        tr("Relatorios, historico e custos para auditoria", "Reports, trip history and costs for auditing"),
                        tr("Alertas de velocidade e monitoramento das viagens", "Speed alerts and trip monitoring")
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
                    val planLabel = when (selectedPlan) {
                        SubscriptionPlan.LITE -> tr("LITE", "LITE")
                        SubscriptionPlan.FROTA -> tr("FROTA", "FLEET")
                        SubscriptionPlan.ENTERPRISE -> tr("ENTERPRISE", "ENTERPRISE")
                    }
                    Text(
                        text = if (trialDays > 0) {
                            tr("COMECAR $planLabel - $trialDays DIAS GRATIS", "START $planLabel - $trialDays FREE DAYS")
                        } else {
                            tr("ASSINAR $planLabel", "SUBSCRIBE TO $planLabel")
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
                    text = if (trialDays > 0) {
                        tr("Teste primeiro. Se nao fizer sentido para sua garagem, cancele quando quiser.", "Try it first. If it does not fit your garage, cancel anytime.")
                    } else {
                        tr("Se nao fizer sentido para sua garagem, cancele quando quiser.", "If it does not fit your garage, cancel anytime.")
                    },
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
                text = garageAnalysisName(),
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

/**
 * Preco pronto para texto corrido, ex.: "R$ 19,90/mês". Vazio quando o Play ainda
 * nao respondeu — quem chama decide se omite o trecho ou mostra outra coisa.
 */
@Composable
internal fun playPriceInlineLabel(price: PlayPlanPrice?): String {
    if (price == null) return ""
    return price.formattedPrice + periodSuffixLabel(price.billingPeriod)
}

/** Sufixo de periodo a partir do billingPeriod ISO-8601 que o Play devolve. */
@Composable
internal fun periodSuffixLabel(billingPeriod: String): String = when (billingPeriod.uppercase()) {
    "P1M" -> tr("/mês", "/mo")
    "P1Y" -> tr("/ano", "/yr")
    "P6M" -> tr("/semestre", "/6 mo")
    "P3M" -> tr("/trimestre", "/quarter")
    "P1W" -> tr("/semana", "/wk")
    else -> ""
}

@Composable
private fun PlanCard(
    name: String,
    price: PlayPlanPrice?,
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
            if (price == null) {
                // O Play ainda nao respondeu. Preferimos nao mostrar valor nenhum a
                // mostrar um numero que pode divergir do que o Play vai cobrar.
                Text(
                    text = tr("Ver preço no Google Play", "See price on Google Play"),
                    color = subColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, color = titleColor)) {
                            append(price.formattedPrice)
                        }
                    },
                    color = titleColor
                )
                val periodo = periodSuffixLabel(price.billingPeriod)
                if (periodo.isNotBlank()) {
                    Text(periodo, color = dimColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }

        // Cada plano pode ter um trial diferente no Play Console, então o selo é por card.
        val diasTrial = price?.freeTrialDays() ?: 0
        if (diasTrial > 0) {
            Text(
                text = tr("$diasTrial dias gratis para testar", "$diasTrial free days to try"),
                color = Color(0xFF059669),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
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
