package br.com.gui.carlembrete

import android.app.Activity
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
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth

private val PHScreenBg = Color(0xFFF7FAFF)
private val PHCardBg = Color(0xFFFFFFFF)
private val PHCardBorder = Color(0xFFE2E8F0)
private val PHGold = Color(0xFFFFD700)
private val PHTitle = Color(0xFF0F172A)
private val PHSub = Color(0xFF475569)
private val PHDim = Color(0xFF64748B)

@Composable
fun PremiumHubScreen(
    planTier: PlanTier,
    onDismiss: () -> Unit,
    onOpenGuardian: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenFleetOverview: () -> Unit,
    onOpenFleetStock: () -> Unit,
    onOpenSubscribe: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF020617) else PHScreenBg
    val cardBg = if (isDark) Color(0xFF0B1220) else PHCardBg
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.12f) else PHCardBorder
    val titleColor = if (isDark) Color(0xFFE2E8F0) else PHTitle
    val subColor = if (isDark) Color(0xFF94A3B8) else PHSub
    val dimColor = if (isDark) Color(0xFF64748B) else PHDim
    val supportBg = if (isDark) Color(0xFF062E24) else Color(0xFFE9FDF3)
    val supportBorder = if (isDark) Color(0xFF34D399).copy(alpha = 0.45f) else Color(0xFF10B981).copy(alpha = 0.45f)
    val supportText = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
    val isEnglish = isEnglishUi()
    val supportPhone = "5516994392545"
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
        ?: FirebaseAuth.getInstance().currentUser?.email
        ?: "cliente"

    DisposableEffect(view, isDark) {
        val activity = view.context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldStatusColor = window?.statusBarColor
        val oldLightStatus = insetsController?.isAppearanceLightStatusBars
        if (window != null && insetsController != null) {
            window.statusBarColor = screenBg.toArgb()
            insetsController.isAppearanceLightStatusBars = !isDark
        }
        onDispose {
            if (window != null && insetsController != null) {
                if (oldStatusColor != null) window.statusBarColor = oldStatusColor
                if (oldLightStatus != null) insetsController.isAppearanceLightStatusBars = oldLightStatus
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
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
                Text(
                    tr("Zellu Premium", "Zellu Premium"),
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(PHGold.copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = PHGold,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                tr("Zellu Premium", "Zellu Premium"),
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )

            Spacer(Modifier.height(6.dp))

            val (badgeLabel, badgeColor, badgeBg) = when (planTier) {
                PlanTier.LITE -> Triple(tr("Plano Lite ativo", "Lite plan active"), Color(0xFF2563EB), Color(0xFFEAF2FF))
                PlanTier.FROTA -> Triple(tr("Plano Frota ativo", "Fleet plan active"), Color(0xFF7A5900), Color(0xFFFFF3CC))
                PlanTier.ENTERPRISE -> Triple(tr("Plano Enterprise ativo", "Enterprise plan active"), Color(0xFF0E7490), Color(0xFFE6FAFE))
                PlanTier.FREE -> Triple(tr("Sem plano ativo", "No active plan"), subColor, if (isDark) Color(0xFF111827) else Color(0xFFF1F5F9))
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Diamond, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
                Text(badgeLabel, color = badgeColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    tr("Recursos disponíveis", "Available features"),
                    color = dimColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                HubFeatureCard(
                    icon = Icons.Default.Route,
                    iconColor = Color(0xFFEA580C),
                    iconBg = Color(0xFFFDEEDB),
                    title = tr("Viagens", "Trips"),
                    subtitle = tr("Registre rotas, despesas e histórico", "Log routes, expenses and history"),
                    onClick = onOpenAiAssistant,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subtitleColor = subColor,
                    chevronColor = dimColor
                )

                if (planTier == PlanTier.FROTA || planTier == PlanTier.ENTERPRISE) {
                    HubFeatureCard(
                        icon = Icons.Default.DirectionsCar,
                        iconColor = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        title = tr("Visão geral da frota", "Fleet overview"),
                        subtitle = tr("Veja todos os veículos da garagem", "See all vehicles in the garage"),
                        onClick = onOpenFleetOverview,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        titleColor = titleColor,
                        subtitleColor = subColor,
                        chevronColor = dimColor
                    )

                    HubFeatureCard(
                        icon = Icons.Default.Inventory2,
                        iconColor = Color(0xFF7C3AED),
                        iconBg = Color(0xFFF3E8FF),
                        title = tr("Estoque da Frota", "Fleet Stock"),
                        subtitle = tr("Gerencie itens, código de barras e reposição", "Manage items, barcode and replenishment"),
                        onClick = onOpenFleetStock,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        titleColor = titleColor,
                        subtitleColor = subColor,
                        chevronColor = dimColor
                    )
                }

                if (planTier == PlanTier.FREE) {
                    val freeCtaBg = if (isDark) Color(0xFF1F2937) else Color(0xFFFFF3CC)
                    val freeCtaBorder = if (isDark) Color(0xFFF2D57A).copy(alpha = 0.45f) else Color(0xFFF2D57A)
                    val freeCtaTitle = if (isDark) Color(0xFFFDE68A) else Color(0xFF7A5900)
                    val freeCtaSub = if (isDark) Color(0xFFFCD34D) else Color(0xFFA77A00)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(freeCtaBg)
                            .border(BorderStroke(1.dp, freeCtaBorder), RoundedCornerShape(18.dp))
                            .clickable { onOpenSubscribe() }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = PHGold, modifier = Modifier.size(28.dp))
                            Text(
                                tr("Assine para liberar todos os recursos", "Subscribe to unlock all features"),
                                color = freeCtaTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                tr("7 dias grátis • cancele quando quiser", "7 free days • cancel anytime"),
                                color = freeCtaSub,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(supportBg)
                    .border(BorderStroke(1.dp, supportBorder), RoundedCornerShape(16.dp))
                    .clickable {
                        abrirWhatsApp(
                            context = context,
                            telefone = supportPhone,
                            mensagem = if (isEnglish)
                                "Hi, I'm $userName and I need help with Zellu Premium."
                            else
                                "Olá, sou $userName e preciso de ajuda com o Zellu Premium."
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = supportText, modifier = Modifier.size(20.dp))
                    Text(tr("Suporte via WhatsApp", "WhatsApp support"), color = supportText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HubFeatureCard(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subtitleColor: Color,
    chevronColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = subtitleColor, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = chevronColor, modifier = Modifier.size(14.dp))
    }
}
