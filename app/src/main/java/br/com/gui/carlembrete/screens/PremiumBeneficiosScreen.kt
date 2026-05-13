package br.com.gui.carlembrete

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

private val PBGoldStart = Color(0xFFE7B84A)
private val PBGoldEnd = Color(0xFFC78A18)
private val PBGoldGradient = Brush.horizontalGradient(listOf(PBGoldStart, PBGoldEnd))

@Composable
fun PremiumBeneficiosScreen(
    onDismiss: () -> Unit,
    onSubscribeNow: (SubscriptionPlan) -> Unit,
    showBackButton: Boolean = true,
    showSubscribeButton: Boolean = true,
    onPlanSelected: ((SubscriptionPlan) -> Unit)? = null
) {
    var selectedPlan by rememberSaveable { mutableStateOf(SubscriptionPlan.FROTA) }
    val view = LocalView.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF020617) else Color(0xFFF7FAFF)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val dimColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val idleLiteBorder = if (isDark) Color(0xFF1E3A8A) else Color(0xFFD7E6FF)
    val idleFleetBorder = if (isDark) Color(0xFF854D0E) else Color(0xFFFFE7A3)
    val idleEnterpriseBorder = if (isDark) Color(0xFF155E75) else Color(0xFFBFEFF7)

    DisposableEffect(view, isDark, screenBg) {
        val activity = view.context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldStatusColor = window?.statusBarColor
        val oldNavigationColor = window?.navigationBarColor
        val oldLightStatus = insetsController?.isAppearanceLightStatusBars
        val oldLightNavigation = insetsController?.isAppearanceLightNavigationBars
        if (window != null && insetsController != null) {
            window.statusBarColor = screenBg.toArgb()
            window.navigationBarColor = screenBg.toArgb()
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
        onDispose {
            if (window != null && insetsController != null) {
                if (oldStatusColor != null) window.statusBarColor = oldStatusColor
                if (oldNavigationColor != null) window.navigationBarColor = oldNavigationColor
                if (oldLightStatus != null) insetsController.isAppearanceLightStatusBars = oldLightStatus
                if (oldLightNavigation != null) insetsController.isAppearanceLightNavigationBars = oldLightNavigation
            }
        }
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
                            tint = titleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = tr("Zellu Premium", "Zellu Premium"),
                    color = titleColor,
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
                    .background(Brush.radialGradient(listOf(PBGoldEnd.copy(alpha = 0.18f), Color.Transparent))),
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
                    borderColorIdle = idleLiteBorder,
                    tagBg = Color.Transparent,
                    tagTextColor = Color.Transparent,
                    isSelected = selectedPlan == SubscriptionPlan.LITE,
                    description = tr("Modulo de viagens com custos por rota.", "Trip module with route cost tracking."),
                    features = listOf(
                        tr("15 veiculos, 50 avisos/registros e 150 abastecimentos", "15 vehicles, 50 reminders/records and 150 fuel records"),
                        tr("30 scans de QR por mes", "30 QR scans per month"),
                        tr("Registro de viagens", "Trip logging"),
                        tr("Controle de custos por rota", "Route cost tracking"),
                        tr("Historico completo de viagens", "Full trip history")
                    ),
                    cardBg = cardBg,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
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
                    borderColorIdle = idleFleetBorder,
                    tagBg = if (isDark) Color(0xFF451A03) else Color(0xFFFFF3CC),
                    tagTextColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF7A5900),
                    isSelected = selectedPlan == SubscriptionPlan.FROTA,
                    description = tr("Viagens, gestao de frota e estoque.", "Trips, fleet management and stock."),
                    features = listOf(
                        tr("Tudo do plano Lite", "Everything from Lite"),
                        tr("50 veiculos, 300 avisos/registros e 1000 abastecimentos", "50 vehicles, 300 reminders/records and 1000 fuel records"),
                        tr("200 scans de QR por mes", "200 QR scans per month"),
                        tr("Visao geral da frota", "Fleet overview"),
                        tr("Sistema de estoque completo", "Full stock system"),
                        tr("Gestao avancada e relatorios", "Advanced management")
                    ),
                    cardBg = cardBg,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
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
                    borderColorIdle = idleEnterpriseBorder,
                    tagBg = if (isDark) Color(0xFF083344) else Color(0xFFE6FAFE),
                    tagTextColor = if (isDark) Color(0xFFA5F3FC) else Color(0xFF0E7490),
                    isSelected = selectedPlan == SubscriptionPlan.ENTERPRISE,
                    description = tr("Tudo do Frota com mais capacidade de veiculos.", "Everything from Fleet with more vehicle capacity."),
                    features = listOf(
                        tr("Tudo do plano Frota", "Everything from Fleet"),
                        tr("200 veiculos, avisos/registros e abastecimentos ilimitados", "200 vehicles, unlimited reminders/records and fuel records"),
                        tr("Scans de QR ilimitados", "Unlimited QR scans"),
                        tr("Viagens e custos avancados", "Advanced trips and costs"),
                        tr("Estoque para operacao em escala", "Stock for large operations"),
                        tr("Maior capacidade de veiculos", "Highest vehicle capacity")
                    ),
                    cardBg = cardBg,
                    titleColor = titleColor,
                    subColor = subColor,
                    dimColor = dimColor,
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
                    color = dimColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(
                                "mailto:guilhermedevsistemas@gmail.com" +
                                    "?subject=${Uri.encode("Cotacao de plano empresarial")}" +
                                    "&body=${Uri.encode("Gostaria de cotar um plano para minha empresa")}"
                            )
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, "Enviar email"))
                        }.onFailure {
                            Toast.makeText(
                                context,
                                "Nenhum app de email encontrado.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                        contentColor = titleColor
                    )
                ) {
                    Text(
                        text = tr("Planos nao atendem? Quero sob demanda", "Need more capacity? Request custom plan"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
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
    cardBg: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color,
    featureColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) borderColorSelected else borderColorIdle
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
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
            Text("R$", color = subColor, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = titleColor)) { append(price) }
                },
                color = titleColor
            )
            Text(tr("/mes", "/mo"), color = dimColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        }

        Text(description, color = subColor, fontSize = 13.sp, lineHeight = 18.sp)

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
                    Text(feature, color = titleColor, fontSize = 13.sp)
                }
            }
        }
    }
}
