package br.com.gui.carlembrete

import android.app.Activity
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val PHScreenBg = Color(0xFFF7FAFF)
private val PHCardBg = Color(0xFFFFFFFF)
private val PHCardBorder = Color(0xFFE2E8F0)
private val PHGold = Color(0xFFFFD700)
private val PHTitle = Color(0xFF0F172A)
private val PHSub = Color(0xFF475569)
private val PHDim = Color(0xFF64748B)
private const val OPERATIONAL_PREFS = "premium_operational_records"
private const val OPERATIONAL_RECORDS_KEY = "records"
private const val OPERATIONAL_DRIVERS_KEY = "drivers"

private enum class OperationalFeature {
    TIRE_ROI,
    PARTS_DURABILITY,
    ROUTE_PROFITABILITY
}

private data class OperationalRecord(
    val id: String = UUID.randomUUID().toString(),
    val feature: String = OperationalFeature.TIRE_ROI.name,
    val name: String = "",
    val brandOrClient: String = "",
    val vehicleId: String = "",
    val vehicle: String = "",
    val positionOrRoute: String = "",
    val kmStart: Int = 0,
    val kmEnd: Int? = null,
    val cost: Double = 0.0,
    val quantity: Int = 1,
    val recordDate: String = "",
    val revenue: Double? = null,
    val taxPercent: Double? = null,
    val driverId: String = "",
    val driverName: String = "",
    val driverCost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

private data class OperationalDriver(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val code: String = "",
    val phone: String = "",
    val salary: Double = 0.0,
    val taxCost: Double = 0.0,
    val defaultCost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

private data class OperationalTravelTrip(
    val id: String,
    val name: String,
    val location: String,
    val vehicleNames: List<String>,
    val cost: Double,
    val createdAt: Long
)

private data class OperationalTitleParts(
    val title: String,
    val financialLines: List<String>
)

private data class OperationalInfoGroup(
    val title: String,
    val items: List<Pair<String, String>>,
    val emphasize: Boolean = false
)

@Composable
fun PremiumHubScreen(
    planTier: PlanTier,
    onDismiss: () -> Unit,
    onOpenGuardian: () -> Unit,
    onOpenVehicleAiChat: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenFleetOverview: () -> Unit,
    onOpenFleetStock: () -> Unit,
    onOpenSubscribe: (SubscriptionPlan) -> Unit,
    isAiBlocked: Boolean = false,
    isWebBlocked: Boolean = false
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color.Black else PHScreenBg
    val cardBg = if (isDark) Color(0xFF0B1220) else PHCardBg
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.12f) else PHCardBorder
    val titleColor = if (isDark) Color(0xFFE2E8F0) else PHTitle
    val subColor = if (isDark) Color(0xFF94A3B8) else PHSub
    val dimColor = if (isDark) Color(0xFF64748B) else PHDim
    val supportBg = if (isDark) Color(0xFF062E24) else Color(0xFFE9FDF3)
    val supportBorder = if (isDark) Color(0xFF34D399).copy(alpha = 0.45f) else Color(0xFF10B981).copy(alpha = 0.45f)
    val supportText = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
    val isEnglish = isEnglishUi()
    val supportPhone = "5516992136295"
    val dashboardUrl = "https://dasbord-frota-six.vercel.app/"
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
        ?: FirebaseAuth.getInstance().currentUser?.email
        ?: "cliente"
    val hasFleetOperationalModules = planTier == PlanTier.FROTA || planTier == PlanTier.ENTERPRISE
    var featureChannelVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        AdminUsersSync.syncFeatureChannels(context) { featureChannelVersion++ }
    }
    val userChannel = AdminUsersSync.getChannelStatus(context)
    fun featureAllowed(key: String): Boolean {
        @Suppress("UNUSED_EXPRESSION")
        featureChannelVersion
        val ch = AdminUsersSync.getFeatureChannel(context, key)
        return ch != "beta" || userChannel == "beta"
    }
    var selectedOperationalFeature by remember { mutableStateOf<OperationalFeature?>(null) }
    var showAiBlockedDialog by remember { mutableStateOf(false) }
    var showWebBlockedDialog by remember { mutableStateOf(false) }

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

    if (showAiBlockedDialog) {
        val emailSubject = tr("Revisão de bloqueio - Zellu AI", "AI block review - Zellu")
        val emailBody = tr(
            "Olá, meu nome é $userName e gostaria de solicitar a revisão da suspensão do meu acesso à Zellu AI.",
            "Hello, my name is $userName and I would like to request a review of my Zellu AI access suspension."
        )
        AlertDialog(
            onDismissRequest = { showAiBlockedDialog = false },
            icon = {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    tr("Acesso à IA suspenso", "AI access suspended"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    tr(
                        "Seu acesso à Zellu AI foi suspenso por uso indevido ou violação dos termos de uso.\n\nCaso acredite que isso foi um engano, entre em contato pelo e-mail de suporte.",
                        "Your access to Zellu AI has been suspended due to misuse or violation of terms of use.\n\nIf you believe this was a mistake, please contact us via support email."
                    ),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiBlockedDialog = false 
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:guilhermedevsistemas@gmail.com")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject)
                            putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, null))
                    }
                ) {
                    Text(tr("Enviar e-mail", "Send email"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiBlockedDialog = false }) {
                    Text(tr("Fechar", "Close"))
                }
            }
        )
    }

    if (showWebBlockedDialog) {
        val emailSubject = tr("Revisão de bloqueio - Dashboard Web", "Web Dashboard block review - Zellu")
        val emailBody = tr(
            "Olá, meu nome é $userName e gostaria de solicitar a revisão da suspensão do meu acesso ao Dashboard Web.",
            "Hello, my name is $userName and I would like to request a review of my Web Dashboard access suspension."
        )
        AlertDialog(
            onDismissRequest = { showWebBlockedDialog = false },
            icon = {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    tr("Acesso ao Dashboard suspenso", "Dashboard access suspended"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    tr(
                        "Seu acesso ao Dashboard Web foi suspenso por uso indevido ou violação dos termos de uso.\n\nCaso acredite que isso foi um engano, entre em contato pelo e-mail de suporte.",
                        "Your Web Dashboard access has been suspended due to misuse or violation of terms of use.\n\nIf you believe this was a mistake, please contact us via support email."
                    ),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWebBlockedDialog = false
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:guilhermedevsistemas@gmail.com")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject)
                            putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, null))
                    }
                ) {
                    Text(tr("Enviar e-mail", "Send email"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebBlockedDialog = false }) {
                    Text(tr("Fechar", "Close"))
                }
            }
        )
    }

    selectedOperationalFeature?.takeIf { hasFleetOperationalModules }?.let { feature ->
        BackHandler { selectedOperationalFeature = null }
        OperationalFeatureScreen(
            feature = feature,
            onDismiss = { selectedOperationalFeature = null },
            screenBg = screenBg,
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            dimColor = dimColor
        )
        return
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
            }

            val (badgeLabel, badgeColor, badgeBg) = when (planTier) {
                PlanTier.LITE -> Triple(tr("Plano Lite ativo", "Lite plan active"), Color(0xFF2563EB), Color(0xFFEAF2FF))
                PlanTier.FROTA -> Triple(tr("Plano Frota ativo", "Fleet plan active"), Color(0xFF7A5900), Color(0xFFFFF3CC))
                PlanTier.ENTERPRISE -> Triple(tr("Plano Enterprise ativo", "Enterprise plan active"), Color(0xFF0E7490), Color(0xFFE6FAFE))
                PlanTier.FREE -> Triple(tr("Sem plano ativo", "No active plan"), subColor, if (isDark) Color(0xFF111827) else Color(0xFFF1F5F9))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(PHGold.copy(alpha = 0.28f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = PHGold,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    tr("Zellu Premium", "Zellu Premium"),
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )

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
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (planTier) {
                    PlanTier.LITE -> UpgradePlanCard(
                        title = tr("Melhorar para Frota", "Upgrade to Fleet"),
                        subtitle = tr("Libere mais veiculos, mais registros e recursos de frota.", "Unlock more vehicles, more records and fleet tools."),
                        buttonText = tr("Ver upgrade", "View upgrade"),
                        cardBg = if (isDark) Color(0xFF172554) else Color(0xFFEAF2FF),
                        cardBorder = Color(0xFF60A5FA).copy(alpha = if (isDark) 0.55f else 0.7f),
                        titleColor = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1D4ED8),
                        subtitleColor = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                        buttonBg = Color(0xFF2563EB),
                        onClick = { onOpenSubscribe(SubscriptionPlan.FROTA) }
                    )

                    PlanTier.FROTA -> UpgradePlanCard(
                        title = tr("Melhorar para Enterprise", "Upgrade to Enterprise"),
                        subtitle = tr("Expanda limites e leve a gestao para um nivel maior.", "Expand limits and take management to the next level."),
                        buttonText = tr("Ver upgrade", "View upgrade"),
                        cardBg = if (isDark) Color(0xFF083344) else Color(0xFFE6FAFE),
                        cardBorder = Color(0xFF06B6D4).copy(alpha = if (isDark) 0.55f else 0.75f),
                        titleColor = if (isDark) Color(0xFFA5F3FC) else Color(0xFF0E7490),
                        subtitleColor = if (isDark) Color(0xFF67E8F9) else Color(0xFF155E75),
                        buttonBg = Color(0xFF0891B2),
                        onClick = { onOpenSubscribe(SubscriptionPlan.ENTERPRISE) }
                    )

                    else -> Unit
                }

                Text(
                    tr("Benefícios do seu plano", "Your plan benefits"),
                    color = dimColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val hubFeatures = buildList {
                    if (planTier in setOf(PlanTier.LITE, PlanTier.FROTA, PlanTier.ENTERPRISE) && featureAllowed("ai")) {
                        add(HubFeatureCubeData(
                            icon = if (isAiBlocked) Icons.Default.Block else Icons.Default.AutoAwesome,
                            iconColor = if (isAiBlocked) Color(0xFF94A3B8) else Color(0xFF2563EB),
                            iconBg = if (isAiBlocked) {
                                if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            } else {
                                if (isDark) Color(0xFF172554) else Color(0xFFEAF2FF)
                            },
                            title = tr("Zellu AI", "Zellu AI"),
                            subtitle = if (isAiBlocked)
                                tr("Acesso suspenso", "Access suspended")
                            else
                                tr("Todos os veiculos em uma conversa", "All vehicles in one chat"),
                            onClick = if (isAiBlocked) ({ showAiBlockedDialog = true }) else onOpenVehicleAiChat,
                            blocked = isAiBlocked
                        ))
                    }
                    if (featureAllowed("viagens")) {
                        add(HubFeatureCubeData(
                            icon = Icons.Default.Route,
                            iconColor = Color(0xFFEA580C),
                            iconBg = if (isDark) Color(0xFF431407) else Color(0xFFFDEEDB),
                            title = tr("Viagens", "Trips"),
                            subtitle = tr("Rotas, despesas e historico", "Routes, expenses and history"),
                            onClick = onOpenAiAssistant
                        ))
                    }
                    if (hasFleetOperationalModules) {
                        if (featureAllowed("frota_pneus")) add(HubFeatureCubeData(
                            icon = Icons.Default.TireRepair,
                            iconColor = Color(0xFF16A34A),
                            iconBg = if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
                            title = tr("Controle de pneus", "Tire tracking"),
                            subtitle = tr("Marca, posicao, KM e durabilidade", "Brand, position, mileage and durability"),
                            onClick = { selectedOperationalFeature = OperationalFeature.TIRE_ROI }
                        ))
                        if (featureAllowed("frota_pecas")) add(HubFeatureCubeData(
                            icon = Icons.Default.Build,
                            iconColor = Color(0xFF7C3AED),
                            iconBg = if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF),
                            title = tr("Durabilidade de pecas", "Parts durability"),
                            subtitle = tr("Pecas por marca e quilometragem", "Parts by brand and mileage"),
                            onClick = { selectedOperationalFeature = OperationalFeature.PARTS_DURABILITY }
                        ))
                        if (featureAllowed("frota_rotas")) add(HubFeatureCubeData(
                            icon = Icons.Default.Route,
                            iconColor = Color(0xFF0891B2),
                            iconBg = if (isDark) Color(0xFF083344) else Color(0xFFE0F2FE),
                            title = tr("Rentabilidade de rotas fixas", "Fixed-route profitability"),
                            subtitle = tr("Lucro ou prejuizo por linha", "Profit or loss by line"),
                            onClick = { selectedOperationalFeature = OperationalFeature.ROUTE_PROFITABILITY }
                        ))
                        if (featureAllowed("frota")) add(HubFeatureCubeData(
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF0284C7),
                            iconBg = if (isDark) Color(0xFF082F49) else Color(0xFFE0F2FE),
                            title = tr("Visão geral da frota", "Fleet overview"),
                            subtitle = tr("Todos os veiculos da garagem", "All garage vehicles"),
                            onClick = onOpenFleetOverview
                        ))
                        if (featureAllowed("frota_web")) add(HubFeatureCubeData(
                            icon = if (isWebBlocked) Icons.Default.Block else Icons.Default.OpenInNew,
                            iconColor = if (isWebBlocked) Color(0xFF94A3B8) else Color(0xFF0F766E),
                            iconBg = if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1),
                            title = tr("Dashboard web", "Web dashboard"),
                            subtitle = if (isWebBlocked) tr("Acesso suspenso", "Access suspended") else tr("Acesse o painel web da sua frota", "Access your fleet web panel"),
                            onClick = if (isWebBlocked) ({ showWebBlockedDialog = true }) else ({
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(dashboardUrl))
                                )
                            }),
                            blocked = isWebBlocked
                        ))
                        if (featureAllowed("frota_estoque")) add(HubFeatureCubeData(
                            icon = Icons.Default.Inventory2,
                            iconColor = Color(0xFF7C3AED),
                            iconBg = if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF),
                            title = tr("Estoque da Frota", "Fleet Stock"),
                            subtitle = tr("Itens, codigo de barras e reposicao", "Items, barcode and replenishment"),
                            onClick = onOpenFleetStock
                        ))
                    }
                }

                HubBenefitsGrid(
                    features = hubFeatures,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subtitleColor = subColor,
                    dimColor = dimColor,
                    isDark = isDark
                )

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
                            .clickable { onOpenSubscribe(SubscriptionPlan.FROTA) }
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
private fun OperationalFeatureScreen(
    feature: OperationalFeature,
    onDismiss: () -> Unit,
    screenBg: Color,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    dimColor: Color
) {
    val context = LocalContext.current
    val icon = when (feature) {
        OperationalFeature.TIRE_ROI -> Icons.Default.TireRepair
        OperationalFeature.PARTS_DURABILITY -> Icons.Default.Build
        OperationalFeature.ROUTE_PROFITABILITY -> Icons.Default.Route
    }
    val title = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Controle de pneus", "Tire tracking")
        OperationalFeature.PARTS_DURABILITY -> tr("Durabilidade de pecas", "Parts durability")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Rentabilidade de rotas fixas", "Fixed-route profitability")
    }
    val subtitle = when (feature) {
        OperationalFeature.TIRE_ROI -> tr(
            "Controle marca, posicao e quilometragem para descobrir qual pneu entrega mais retorno.",
            "Track brand, position and mileage to learn which tire gives the best return."
        )
        OperationalFeature.PARTS_DURABILITY -> tr(
            "Compare fabricantes de pecas de desgaste usando o KM real de troca.",
            "Compare wear-part makers using real replacement mileage."
        )
        OperationalFeature.ROUTE_PROFITABILITY -> tr(
            "Cadastre custo, imposto e valor cobrado para ver lucro ou prejuizo na hora.",
            "Log cost, tax and charged price to see profit or loss instantly."
        )
    }
    val integrationText = when (feature) {
        OperationalFeature.TIRE_ROI -> tr(
            "Integrado com veiculos cadastrados e manutencoes de pneu.",
            "Integrated with registered vehicles and tire maintenance."
        )
        OperationalFeature.PARTS_DURABILITY -> tr(
            "Integrado com avisos de freio, mecanica e pecas de desgaste.",
            "Integrated with brake, mechanic and wear-part reminders."
        )
        OperationalFeature.ROUTE_PROFITABILITY -> tr(
            "Integrado com veiculos, abastecimentos, manutencoes e viagens salvas.",
            "Integrated with vehicles, fuel, maintenance and saved trips."
        )
    }
    val isRoute = feature == OperationalFeature.ROUTE_PROFITABILITY
    val vehicles = remember(feature) { BancoDeDados.carregarCarros(context).orEmpty() }
    val maintenanceRecords = remember(feature) { BancoDeDados.carregarLembretes(context) }
    val fuelRecords = remember(feature) { BancoDeDados.carregarAbastecimentos(context) }
    val travelTrips = remember(feature) { loadOperationalTravelTrips(context) }
    var records by remember(feature) {
        mutableStateOf(loadOperationalRecords(context).filter { it.feature == feature.name })
    }
    var drivers by remember(feature) { mutableStateOf(loadOperationalDrivers(context)) }
    var selectedVehicleId by remember(feature) { mutableStateOf(vehicles.firstOrNull()?.id.orEmpty()) }
    val importedRecords = remember(feature, selectedVehicleId, maintenanceRecords, travelTrips) {
        buildIntegratedOperationalRecords(
            feature = feature,
            vehicles = vehicles,
            maintenanceRecords = maintenanceRecords,
            travelTrips = travelTrips,
            selectedVehicleId = selectedVehicleId
        )
    }
    val visibleRecords = remember(records, importedRecords) {
        importedRecords.filterNot { imported -> records.any { it.id == imported.id } } + records
    }
    var showRegistrationScreen by remember(feature) { mutableStateOf(false) }
    var editingRecordId by remember(feature) { mutableStateOf<String?>(null) }
    var name by remember(feature) { mutableStateOf("") }
    var brandOrClient by remember(feature) { mutableStateOf("") }
    var vehicle by remember(feature) { mutableStateOf(vehicles.firstOrNull()?.displayName().orEmpty()) }
    var positionOrRoute by remember(feature) { mutableStateOf("") }
    var kmStart by remember(feature) {
        mutableStateOf(if (!isRoute) vehicles.firstOrNull()?.kmAtual?.takeIf { it > 0 }?.toString().orEmpty() else "")
    }
    val routeSuggestedCost = remember(selectedVehicleId, kmStart, fuelRecords, maintenanceRecords, records) {
        val distance = kmStart.toIntOrNull() ?: 0
        if (isRoute && selectedVehicleId.isNotBlank() && distance > 0) {
            estimateRouteCost(selectedVehicleId, distance, fuelRecords, maintenanceRecords, loadOperationalRecords(context))
        } else {
            0.0
        }
    }
    var kmEnd by remember(feature) { mutableStateOf("") }
    var cost by remember(feature) { mutableStateOf("") }
    var quantity by remember(feature) { mutableStateOf("1") }
    var recordDate by remember(feature) { mutableStateOf(currentOperationalDate()) }
    var revenue by remember(feature) { mutableStateOf("") }
    var taxPercent by remember(feature) { mutableStateOf("") }
    var selectedDriverId by remember(feature) { mutableStateOf("") }
    var driverName by remember(feature) { mutableStateOf("") }
    var driverCode by remember(feature) { mutableStateOf("") }
    var driverPhone by remember(feature) { mutableStateOf("") }
    var driverSalary by remember(feature) { mutableStateOf("") }
    var driverTaxCost by remember(feature) { mutableStateOf("") }
    var driverCost by remember(feature) { mutableStateOf("") }
    var showReportOptions by remember(feature) { mutableStateOf(false) }
    var showDriversManager by remember(feature) { mutableStateOf(false) }

    val nameLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Pneu/modelo", "Tire/model")
        OperationalFeature.PARTS_DURABILITY -> tr("Peca", "Part")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Nome da linha", "Line name")
    }
    val brandLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Marca do pneu", "Tire brand")
        OperationalFeature.PARTS_DURABILITY -> tr("Marca/fabricante", "Brand/manufacturer")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Cliente", "Client")
    }
    val positionLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Posicao da roda, ex: R1", "Wheel position, ex: R1")
        OperationalFeature.PARTS_DURABILITY -> tr("Local/observacao", "Location/note")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Origem -> destino", "Origin -> destination")
    }
    val kmStartLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("KM instalacao", "Install mileage")
        OperationalFeature.PARTS_DURABILITY -> tr("KM instalado", "Install mileage")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Distancia media da viagem (km)", "Average trip distance (km)")
    }
    val kmEndLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("KM retirada", "Removal mileage")
        OperationalFeature.PARTS_DURABILITY -> tr("KM nova troca", "New replacement mileage")
        OperationalFeature.ROUTE_PROFITABILITY -> ""
    }
    val costLabel = when (feature) {
        OperationalFeature.TIRE_ROI -> tr("Custo do pneu", "Tire cost")
        OperationalFeature.PARTS_DURABILITY -> tr("Custo da peca", "Part cost")
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Custo operacional da viagem", "Operational trip cost")
    }
    val registrationTitle = when (feature) {
        OperationalFeature.TIRE_ROI -> if (editingRecordId == null) tr("Cadastrar pneu", "Add tire") else tr("Editar pneu", "Edit tire")
        OperationalFeature.PARTS_DURABILITY -> if (editingRecordId == null) tr("Cadastrar peca", "Add part") else tr("Editar peca", "Edit part")
        OperationalFeature.ROUTE_PROFITABILITY -> if (editingRecordId == null) tr("Cadastrar linha", "Add route") else tr("Editar linha", "Edit route")
    }
    val saveButtonText = if (editingRecordId == null) tr("Salvar e calcular", "Save and calculate") else tr("Salvar edicao", "Save changes")
    val requiredToast = tr("Preencha nome, veiculo, KM e valores.", "Fill name, vehicle, mileage and values.")
    val invalidKmToast = tr("KM final precisa ser maior que o KM inicial.", "Final mileage must be greater than initial mileage.")
    val savedToast = tr("Registro salvo. Agora sim, tela com musculo.", "Record saved.")
    val updatedToast = tr("Registro atualizado.", "Record updated.")
    val deletedToast = tr("Registro removido.", "Record removed.")
    val integratedRecordToast = tr("Esse veio dos dados ja existentes.", "This came from existing data.")
    val kmEndSavedToast = tr("KM final salvo e calculado.", "Final mileage saved.")

    fun resetOperationalForm() {
        name = ""
        brandOrClient = ""
        positionOrRoute = ""
        kmStart = if (!isRoute) {
            vehicles.firstOrNull { it.id == selectedVehicleId }?.kmAtual?.takeIf { it > 0 }?.toString().orEmpty()
        } else {
            ""
        }
        kmEnd = ""
        cost = ""
        quantity = "1"
        recordDate = currentOperationalDate()
        revenue = ""
        taxPercent = ""
        selectedDriverId = ""
        driverName = ""
        driverCode = ""
        driverPhone = ""
        driverSalary = ""
        driverTaxCost = ""
        driverCost = ""
    }

    fun closeRegistrationScreen() {
        showRegistrationScreen = false
        editingRecordId = null
        resetOperationalForm()
    }

    fun openCreateRegistration() {
        editingRecordId = null
        resetOperationalForm()
        showRegistrationScreen = true
    }

    fun openDriverDialog() {
        showDriversManager = true
    }

    fun refreshSelectedDriver(driver: OperationalDriver?) {
        if (driver == null) {
            selectedDriverId = ""
            driverName = ""
            driverCode = ""
            driverPhone = ""
            driverSalary = ""
            driverTaxCost = ""
            driverCost = ""
            return
        }
        selectedDriverId = driver.id
        driverName = driver.name
        driverCode = driver.code
        driverPhone = driver.phone
        driverSalary = driver.salary.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        driverTaxCost = driver.taxCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        if (driverCost.isBlank()) {
            driverCost = driver.defaultCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        }
    }

    fun saveDriverFromManager(
        editingDriverId: String?,
        editedName: String,
        editedCode: String,
        editedPhone: String,
        editedSalary: String,
        editedTaxCost: String,
        editedDefaultCost: String
    ): Boolean {
        val savedDriver = upsertOperationalDriver(
            context = context,
            drivers = drivers,
            selectedDriverId = editingDriverId.orEmpty(),
            name = editedName,
            code = editedCode,
            phone = editedPhone,
            salary = parseMoneyInput(editedSalary) ?: 0.0,
            taxCost = parseMoneyInput(editedTaxCost) ?: 0.0,
            defaultCost = parseMoneyInput(editedDefaultCost) ?: 0.0
        ) ?: run {
            Toast.makeText(context, trNow("Informe o nome do motorista.", "Enter the driver name."), Toast.LENGTH_SHORT).show()
            return false
        }
        val currentAll = loadOperationalRecords(context)
        val updatedAll = currentAll.map { record ->
            if (record.driverId == savedDriver.id) {
                record.copy(driverName = savedDriver.name)
            } else {
                record
            }
        }
        saveOperationalRecords(context, updatedAll)
        drivers = loadOperationalDrivers(context)
        records = updatedAll.filter { it.feature == feature.name }
        if (selectedDriverId == savedDriver.id || editingDriverId == null) {
            refreshSelectedDriver(savedDriver)
        }
        Toast.makeText(context, trNow("Motorista salvo.", "Driver saved."), Toast.LENGTH_SHORT).show()
        return true
    }

    fun deleteDriverFromManager(driver: OperationalDriver) {
        val currentAll = loadOperationalRecords(context)
        val updatedAll = currentAll.map { record ->
            if (record.driverId == driver.id) {
                record.copy(driverId = "", driverName = "", driverCost = 0.0)
            } else {
                record
            }
        }
        saveOperationalRecords(context, updatedAll)
        saveOperationalDrivers(context, loadOperationalDrivers(context).filterNot { it.id == driver.id })
        drivers = loadOperationalDrivers(context)
        records = updatedAll.filter { it.feature == feature.name }
        if (selectedDriverId == driver.id) {
            refreshSelectedDriver(null)
        }
        Toast.makeText(context, trNow("Motorista removido.", "Driver removed."), Toast.LENGTH_SHORT).show()
    }

    fun openEditRegistration(record: OperationalRecord) {
        editingRecordId = record.id
        selectedVehicleId = record.vehicleId
        vehicle = record.vehicle
        name = splitOperationalTitle(record.name).title
        brandOrClient = record.brandOrClient
        positionOrRoute = record.positionOrRoute
        kmStart = record.kmStart.takeIf { it > 0 }?.toString().orEmpty()
        kmEnd = record.kmEnd?.toString().orEmpty()
        cost = formatPlainDecimal(record.cost)
        quantity = record.quantity.coerceAtLeast(1).toString()
        recordDate = record.recordDate.ifBlank { currentOperationalDate() }
        revenue = record.revenue?.let { formatPlainDecimal(it) }.orEmpty()
        taxPercent = record.taxPercent?.let { formatPlainDecimal(it) }.orEmpty()
        selectedDriverId = record.driverId
        val selectedDriver = drivers.firstOrNull { it.id == record.driverId }
        driverName = record.driverName.ifBlank { selectedDriver?.name.orEmpty() }
        driverCode = selectedDriver?.code.orEmpty()
        driverPhone = selectedDriver?.phone.orEmpty()
        driverSalary = selectedDriver?.salary?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        driverTaxCost = selectedDriver?.taxCost?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        driverCost = (record.driverCost.takeIf { it > 0.0 } ?: selectedDriver?.defaultCost)
            ?.let { formatPlainDecimal(it) }
            .orEmpty()
        showRegistrationScreen = true
    }

    BackHandler(enabled = showRegistrationScreen) {
        closeRegistrationScreen()
    }

    BackHandler(enabled = showDriversManager) {
        showDriversManager = false
    }

    LaunchedEffect(selectedVehicleId, feature) {
        val picked = vehicles.firstOrNull { it.id == selectedVehicleId }
        if (picked != null) {
            vehicle = picked.displayName()
            if (!isRoute && kmStart.isBlank() && picked.kmAtual > 0) {
                kmStart = picked.kmAtual.toString()
            }
        }
    }

    fun saveOperationalRecord() {
        val kmStartValue = kmStart.toIntOrNull()
        val kmEndValue = kmEnd.toIntOrNull()
        val costValue = parseMoneyInput(cost)
        val quantityValue = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val revenueValue = parseMoneyInput(revenue)
        val taxValue = parseMoneyInput(taxPercent)
        val driverCostValue = parseMoneyInput(driverCost) ?: 0.0
        val missingRequired = name.isBlank() || vehicle.isBlank() || kmStartValue == null || costValue == null ||
            (isRoute && revenueValue == null)
        val invalidKmRange = !isRoute && kmEndValue != null && kmEndValue <= (kmStartValue ?: 0)

        when {
            missingRequired -> Toast.makeText(context, requiredToast, Toast.LENGTH_SHORT).show()
            invalidKmRange -> Toast.makeText(context, invalidKmToast, Toast.LENGTH_SHORT).show()
            else -> {
                val editingId = editingRecordId
                val currentAll = loadOperationalRecords(context)
                val currentRecord = editingId?.let { id -> currentAll.firstOrNull { it.id == id } }
                val driverForRecord = if (isRoute && selectedDriverId.isNotBlank()) {
                    drivers.firstOrNull { it.id == selectedDriverId } ?: loadOperationalDrivers(context).firstOrNull { it.id == selectedDriverId }
                } else {
                    null
                }
                val newRecord = OperationalRecord(
                    id = editingId ?: UUID.randomUUID().toString(),
                    feature = feature.name,
                    name = name.trim(),
                    brandOrClient = brandOrClient.trim(),
                    vehicleId = selectedVehicleId,
                    vehicle = vehicle.trim(),
                    positionOrRoute = positionOrRoute.trim(),
                    kmStart = kmStartValue ?: 0,
                    kmEnd = if (isRoute) null else kmEndValue,
                    cost = costValue ?: 0.0,
                    quantity = quantityValue,
                    recordDate = recordDate.ifBlank { currentOperationalDate() },
                    revenue = if (isRoute) revenueValue else null,
                    taxPercent = if (isRoute) taxValue ?: 0.0 else null,
                    driverId = driverForRecord?.id.orEmpty(),
                    driverName = driverForRecord?.name.orEmpty(),
                    driverCost = if (driverForRecord != null) driverCostValue else 0.0,
                    createdAt = currentRecord?.createdAt ?: System.currentTimeMillis()
                )
                if (editingId != null) {
                    val updatedAll = if (currentAll.any { it.id == editingId }) {
                        currentAll.map { if (it.id == editingId) newRecord else it }
                    } else {
                        currentAll + newRecord
                    }
                    saveOperationalRecords(context, updatedAll)
                    upsertOperationalReminder(context, newRecord)
                    records = updatedAll.filter { it.feature == feature.name }
                    closeRegistrationScreen()
                    Toast.makeText(context, updatedToast, Toast.LENGTH_SHORT).show()
                    return
                }
                val previousOpen = if (!isRoute) {
                    currentAll
                        .filter { it.feature == feature.name }
                        .filter { it.vehicleId == newRecord.vehicleId }
                        .filter { it.kmEnd == null }
                        .filter { it.kmStart < newRecord.kmStart }
                        .filter { operationalReplacementKey(it) == operationalReplacementKey(newRecord) }
                        .maxByOrNull { it.kmStart }
                } else {
                    null
                }
                val closedPrevious = previousOpen?.copy(kmEnd = newRecord.kmStart, createdAt = System.currentTimeMillis())
                val updatedAll = currentAll
                    .map { if (closedPrevious != null && it.id == closedPrevious.id) closedPrevious else it } +
                    newRecord
                saveOperationalRecords(context, updatedAll)
                if (closedPrevious != null) upsertOperationalReminder(context, closedPrevious)
                upsertOperationalReminder(context, newRecord)
                records = updatedAll.filter { it.feature == feature.name }
                closeRegistrationScreen()
                Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleOperationalReport() {
        val realCostPerKm = selectedVehicleId.takeIf { it.isNotBlank() }?.let {
            routeRealCostPerKm(it, fuelRecords, maintenanceRecords, loadOperationalRecords(context))
        } ?: 0.0
        val pdf = generateOperationalReportPdf(
            context = context,
            feature = feature,
            records = visibleRecords,
            vehiclesCount = vehicles.size,
            importedRecordsCount = importedRecords.size,
            realCostPerKm = realCostPerKm,
            routeSuggestedCost = routeSuggestedCost
        )
        if (pdf == null) {
            Toast.makeText(
                context,
                trNow("Nao foi possivel gerar o PDF.", "Could not generate the PDF."),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        shareOperationalPdf(context, pdf)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        if (showReportOptions) {
            OperationalReportOptionsDialog(
                bg = screenBg,
                textPrimary = titleColor,
                cardBorder = cardBorder,
                accentBlue = Color(0xFF2563EB),
                onExportPdf = {
                    showReportOptions = false
                    handleOperationalReport()
                },
                onDismiss = { showReportOptions = false }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
                }
                IconButton(
                    onClick = { showReportOptions = true },
                    modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = tr("Relatorio", "Report"),
                        tint = titleColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isRoute) {
                    IconButton(
                        onClick = { showDriversManager = true },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 44.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = tr("Gerenciar motoristas", "Manage drivers"),
                            tint = titleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(27.dp))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(title, color = titleColor, fontWeight = FontWeight.Black, fontSize = 19.sp, textAlign = TextAlign.Center)
                    Text(subtitle, color = subColor, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF16A34A).copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                        Text(integrationText, color = Color(0xFF047857), fontSize = 11.sp, lineHeight = 14.sp)
                    }
                    Text(
                        text = tr("Disponivel no Plano Frota", "Available on Fleet plan"),
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = { openCreateRegistration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF357AE8),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(tr("Cadastrar", "Add"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OperationalSummaryCard(
                feature = feature,
                records = visibleRecords,
                vehiclesCount = vehicles.size,
                driversCount = drivers.size,
                importedRecordsCount = importedRecords.size,
                realCostPerKm = selectedVehicleId.takeIf { it.isNotBlank() }?.let {
                    routeRealCostPerKm(it, fuelRecords, maintenanceRecords, loadOperationalRecords(context))
                } ?: 0.0,
                routeSuggestedCost = routeSuggestedCost,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor
            )

            Text(
                text = tr("Historico", "History"),
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            )

            if (visibleRecords.isEmpty()) {
                EmptyOperationalState(
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subColor = subColor
                )
            } else {
                visibleRecords.sortedByDescending { it.createdAt }.forEach { record ->
                    val canManageRecord = records.any { it.id == record.id } || !record.id.startsWith("auto-")
                    OperationalRecordCard(
                        feature = feature,
                        record = record,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        titleColor = titleColor,
                        subColor = subColor,
                        canDelete = canManageRecord,
                        onEdit = if (canManageRecord) {
                            { openEditRegistration(record) }
                        } else {
                            null
                        },
                        onUpdateKmEnd = if (feature != OperationalFeature.ROUTE_PROFITABILITY) {
                            { finalKm ->
                                val updatedRecord = record.copy(kmEnd = finalKm, createdAt = System.currentTimeMillis())
                                val current = loadOperationalRecords(context)
                                val updatedAll = if (current.any { it.id == record.id }) {
                                    current.map { if (it.id == record.id) updatedRecord else it }
                                } else {
                                    current + updatedRecord
                                }
                                saveOperationalRecords(context, updatedAll)
                                upsertOperationalReminder(context, updatedRecord)
                                records = updatedAll.filter { it.feature == feature.name }
                                Toast.makeText(context, kmEndSavedToast, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            null
                        },
                        onDelete = {
                            if (record.id.startsWith("auto-") && records.none { it.id == record.id }) {
                                Toast.makeText(context, integratedRecordToast, Toast.LENGTH_SHORT).show()
                                return@OperationalRecordCard
                            }
                            val updatedAll = loadOperationalRecords(context).filterNot { it.id == record.id }
                            saveOperationalRecords(context, updatedAll)
                            deleteOperationalReminder(context, record.id)
                            records = updatedAll.filter { it.feature == feature.name }
                            Toast.makeText(context, deletedToast, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        if (showRegistrationScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { closeRegistrationScreen() },
                        modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(30.dp))
                    }
                    Text(
                        registrationTitle,
                        color = titleColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OperationalFormSection(
                        title = if (isRoute) tr("Linha e cliente", "Line and client") else tr("Produto", "Product"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        if (feature == OperationalFeature.PARTS_DURABILITY) {
                            PartTypePickerField(
                                value = name,
                                onValueChange = { name = it },
                                label = nameLabel
                            )
                        } else {
                            OperationalTextField(value = name, onValueChange = { name = it }, label = nameLabel)
                        }
                        OperationalTextField(value = brandOrClient, onValueChange = { brandOrClient = it }, label = brandLabel)
                    }

                    OperationalFormSection(
                        title = tr("Veiculo", "Vehicle"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        VehiclePickerField(
                            vehicles = vehicles,
                            selectedVehicleId = selectedVehicleId,
                            onSelect = { picked ->
                                selectedVehicleId = picked.id
                                vehicle = picked.displayName()
                                if (!isRoute && picked.kmAtual > 0) kmStart = picked.kmAtual.toString()
                            },
                            fallbackValue = vehicle,
                            onFallbackChange = { vehicle = it },
                            label = tr("Veiculo", "Vehicle")
                        )
                    }

                    if (isRoute) {
                        OperationalFormSection(
                            title = tr("Motorista", "Driver"),
                            cardBorder = cardBorder,
                            subColor = subColor
                        ) {
                            DriverPickerField(
                                drivers = drivers,
                                selectedDriverId = selectedDriverId,
                                onSelect = { picked ->
                                    selectedDriverId = picked.id
                                    driverName = picked.name
                                    driverCode = picked.code
                                    driverPhone = picked.phone
                                    driverSalary = picked.salary.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
                                    driverTaxCost = picked.taxCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
                                    driverCost = picked.defaultCost.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
                                },
                                onClear = {
                                    selectedDriverId = ""
                                    driverName = ""
                                    driverCode = ""
                                    driverPhone = ""
                                    driverSalary = ""
                                    driverTaxCost = ""
                                    driverCost = ""
                                },
                                onRequestAdd = { openDriverDialog() },
                                label = tr("Motorista da linha", "Route driver")
                            )
                            val hasSelectedDriver = selectedDriverId.isNotBlank()
                            OperationalTextField(
                                value = driverCode,
                                onValueChange = {},
                                label = tr("Codigo do motorista", "Driver code"),
                                enabled = false
                            )
                            OperationalTextField(
                                value = driverName,
                                onValueChange = {},
                                label = tr("Nome do motorista", "Driver name"),
                                enabled = false
                            )
                            OperationalTextField(
                                value = driverPhone,
                                onValueChange = {},
                                label = tr("Telefone/observacao", "Phone/note"),
                                enabled = false
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OperationalTextField(
                                    value = driverSalary,
                                    onValueChange = {},
                                    label = tr("Salario", "Salary"),
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                )
                                OperationalTextField(
                                    value = driverTaxCost,
                                    onValueChange = {},
                                    label = tr("Impostos/custos", "Taxes/costs"),
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OperationalTextField(
                                value = driverCost,
                                onValueChange = { driverCost = keepDecimalInput(it) },
                                label = tr("Custo do motorista por linha", "Driver cost per route"),
                                keyboardType = KeyboardType.Decimal,
                                enabled = hasSelectedDriver
                            )
                        }
                    }

                    OperationalFormSection(
                        title = if (isRoute) tr("Rota", "Route") else tr("Instalacao", "Install"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        OperationalTextField(value = positionOrRoute, onValueChange = { positionOrRoute = it }, label = positionLabel)
                        if (feature == OperationalFeature.TIRE_ROI) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OperationalTextField(
                                    value = quantity,
                                    onValueChange = { quantity = keepNumericInput(it).take(3).ifBlank { "1" } },
                                    label = tr("Quantidade", "Quantity"),
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                                OperationalTextField(
                                    value = recordDate,
                                    onValueChange = { recordDate = it.take(10) },
                                    label = tr("Data", "Date"),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        OperationalTextField(
                            value = kmStart,
                            onValueChange = { kmStart = keepNumericInput(it) },
                            label = kmStartLabel,
                            keyboardType = KeyboardType.Number
                        )
                    }

                    if (!isRoute) {
                        OperationalFormSection(
                            title = tr("Retirada", "Removal"),
                            cardBorder = cardBorder,
                            subColor = subColor
                        ) {
                            OperationalTextField(
                                value = kmEnd,
                                onValueChange = { kmEnd = keepNumericInput(it) },
                                label = kmEndLabel,
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    OperationalFormSection(
                        title = tr("Valores", "Values"),
                        cardBorder = cardBorder,
                        subColor = subColor
                    ) {
                        OperationalTextField(
                            value = cost,
                            onValueChange = { cost = keepDecimalInput(it) },
                            label = costLabel,
                            keyboardType = KeyboardType.Decimal
                        )
                        if (isRoute) {
                            if (routeSuggestedCost > 0.0) {
                                OutlinedButton(
                                    onClick = { cost = formatPlainDecimal(routeSuggestedCost) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        tr("Usar custo real estimado: ", "Use estimated real cost: ") + formatMoney(routeSuggestedCost),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            OperationalTextField(
                                value = revenue,
                                onValueChange = { revenue = keepDecimalInput(it) },
                                label = tr("Valor cobrado do cliente", "Amount charged to client"),
                                keyboardType = KeyboardType.Decimal
                            )
                            OperationalTextField(
                                value = taxPercent,
                                onValueChange = { taxPercent = keepDecimalInput(it) },
                                label = tr("Imposto (%)", "Tax (%)"),
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                    }

                    Button(
                        onClick = { saveOperationalRecord() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(saveButtonText, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        if (showDriversManager) {
            DriverManagerScreen(
                drivers = drivers,
                routeRecords = loadOperationalRecords(context)
                    .filter { it.feature == OperationalFeature.ROUTE_PROFITABILITY.name },
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subColor = subColor,
                screenBg = screenBg,
                onDismiss = { showDriversManager = false },
                onSaveDriver = { editingDriverId, editedName, editedCode, editedPhone, editedSalary, editedTaxCost, editedDefaultCost ->
                    saveDriverFromManager(
                        editingDriverId = editingDriverId,
                        editedName = editedName,
                        editedCode = editedCode,
                        editedPhone = editedPhone,
                        editedSalary = editedSalary,
                        editedTaxCost = editedTaxCost,
                        editedDefaultCost = editedDefaultCost
                    )
                },
                onDeleteDriver = { driver -> deleteDriverFromManager(driver) }
            )
        }
    }
}

@Composable
private fun OperationalSummaryCard(
    feature: OperationalFeature,
    records: List<OperationalRecord>,
    vehiclesCount: Int,
    driversCount: Int,
    importedRecordsCount: Int,
    realCostPerKm: Double,
    routeSuggestedCost: Double,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color
) {
    val finished = records.filter { it.kmEnd != null && it.kmEnd > it.kmStart }
    val routeRecords = records.filter { it.revenue != null }
    val currentMonthTag = SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date())
    val currentMonthRouteBalance = routeRecords
        .filter { SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date(it.createdAt)) == currentMonthTag }
        .sumOf { routeProfit(it) }
    val bestRouteProfitMetric = routeRecords.maxByOrNull { routeProfit(it) }?.let {
        val margin = routeMargin(it)
        "${formatMoney(routeProfit(it))} • ${formatPlainDecimal(margin)}%"
    } ?: tr("Sem rotas calculadas", "No calculated routes")
    val totalRouteBalanceMetric = routeRecords.sumOf { routeProfit(it) }.let {
        formatMoney(it)
    }
    val bestDurabilityMetric = finished.maxByOrNull { (it.kmEnd ?: 0) - it.kmStart }?.let {
        "${(it.kmEnd ?: 0) - it.kmStart} km"
    } ?: tr("Aguardando KM final", "Waiting for final mileage")
    val lowestCostMetric = finished.minByOrNull { costPerKm(it) }?.let {
        "${formatMoney(costPerKm(it))}/km"
    } ?: tr("Aguardando KM", "Waiting mileage")
    val primaryMetric = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> totalRouteBalanceMetric
        else -> lowestCostMetric
    }
    val primaryLabel = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Saldo total", "Total balance")
        else -> tr("Menor custo/km", "Lowest cost/km")
    }
    val wideMetricLabel = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Melhor lucro", "Best profit")
        else -> tr("Melhor durabilidade", "Best durability")
    }
    val wideMetricValue = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> bestRouteProfitMetric
        else -> bestDurabilityMetric
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(tr("Resumo", "Summary"), color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryPill(
                label = tr("Registros", "Records"),
                value = records.size.toString(),
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
            SummaryPill(
                label = tr("Veiculos", "Vehicles"),
                value = vehiclesCount.toString(),
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryPill(
                label = tr("Importados", "Imported"),
                value = importedRecordsCount.toString(),
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
            SummaryPill(
                label = primaryLabel,
                value = primaryMetric,
                modifier = Modifier.weight(1f),
                titleColor = titleColor,
                subColor = subColor
            )
        }
        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill(
                    label = tr("Motoristas", "Drivers"),
                    value = driversCount.toString(),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
                SummaryPill(
                    label = tr("Linhas com motorista", "Routes with driver"),
                    value = routeRecords.count { it.driverName.isNotBlank() }.toString(),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
            }
            SummaryPill(
                label = tr("Saldo do mes", "Monthly balance"),
                value = formatMoney(currentMonthRouteBalance),
                modifier = Modifier.fillMaxWidth(),
                titleColor = titleColor,
                subColor = subColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill(
                    label = tr("Custo/km real", "Real cost/km"),
                    value = formatMoney(realCostPerKm),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
                SummaryPill(
                    label = tr("Sugestao rota", "Route suggestion"),
                    value = formatMoney(routeSuggestedCost),
                    modifier = Modifier.weight(1f),
                    titleColor = titleColor,
                    subColor = subColor
                )
            }
        }
        SummaryPill(
            label = wideMetricLabel,
            value = wideMetricValue,
            modifier = Modifier.fillMaxWidth(),
            titleColor = titleColor,
            subColor = subColor
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    modifier: Modifier,
    titleColor: Color,
    subColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF3B82F6).copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = subColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
    }
}

@Composable
private fun OperationalFormSection(
    title: String,
    cardBorder: Color,
    subColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF3B82F6).copy(alpha = 0.04f))
            .border(BorderStroke(1.dp, cardBorder.copy(alpha = 0.7f)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = subColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VehiclePickerField(
    vehicles: List<CarroInfo>,
    selectedVehicleId: String,
    onSelect: (CarroInfo) -> Unit,
    fallbackValue: String,
    onFallbackChange: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = vehicles.firstOrNull { it.id == selectedVehicleId }
    if (vehicles.isEmpty()) {
        OperationalTextField(value = fallbackValue, onValueChange = onFallbackChange, label = label)
        return
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.displayName() ?: fallbackValue.ifBlank { vehicles.firstOrNull()?.displayName().orEmpty() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(vehicle.displayName(), fontWeight = FontWeight.SemiBold)
                            Text(
                                if (vehicle.kmAtual > 0) "${vehicle.kmAtual} km" else tr("KM nao informado", "Mileage not informed"),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    onClick = {
                        onSelect(vehicle)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DriverPickerField(
    drivers: List<OperationalDriver>,
    selectedDriverId: String,
    onSelect: (OperationalDriver) -> Unit,
    onClear: () -> Unit,
    onRequestAdd: () -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = drivers.firstOrNull { it.id == selectedDriverId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (drivers.isEmpty()) {
                onRequestAdd()
            } else {
                expanded = !expanded
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.name ?: if (drivers.isEmpty()) {
                tr("Nenhum motorista cadastrado", "No drivers registered")
            } else {
                tr("Selecione um motorista", "Select a driver")
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        tr("Sem motorista nesta linha", "No driver for this route"),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {
                    onClear()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        tr("Cadastrar novo motorista", "Add new driver"),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                },
                onClick = {
                    expanded = false
                    onRequestAdd()
                }
            )
            drivers.forEach { driver ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(driver.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(
                                    driver.code.takeIf { it.isNotBlank() }?.let { "Cod: $it" },
                                    driver.phone,
                                    driver.salary.takeIf { it > 0.0 }?.let { tr("Salario", "Salary") + ": ${formatMoney(it)}" },
                                    driver.taxCost.takeIf { it > 0.0 }?.let { tr("Custos", "Costs") + ": ${formatMoney(it)}" },
                                    driver.defaultCost.takeIf { it > 0.0 }?.let { formatMoney(it) }
                                ).filterNotNull().filter { it.isNotBlank() }.joinToString(" • ")
                                    .ifBlank { tr("Sem custo padrao", "No default cost") },
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    onClick = {
                        onSelect(driver)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PartTypePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        tr("Pastilha de freio", "Brake pad"),
        tr("Terminal de suspensao", "Suspension terminal"),
        tr("Pivo de suspensao", "Suspension ball joint"),
        tr("Bucha de suspensao", "Suspension bushing"),
        tr("Amortecedor", "Shock absorber"),
        tr("Outra peca", "Other part")
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(tr("Selecione a peca", "Select the part")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OperationalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun EmptyOperationalState(
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tr("Nenhum registro ainda", "No records yet"), color = titleColor, fontWeight = FontWeight.Bold)
        Text(
            tr("Cadastre o primeiro item acima e o calculo aparece aqui.", "Add the first item above and the calculation appears here."),
            color = subColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OperationalRecordCard(
    feature: OperationalFeature,
    record: OperationalRecord,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    canDelete: Boolean,
    onEdit: (() -> Unit)?,
    onUpdateKmEnd: ((Int) -> Unit)?,
    onDelete: () -> Unit
) {
    var editingKmEnd by remember(record.id, record.kmEnd) { mutableStateOf(false) }
    var kmEndDraft by remember(record.id, record.kmEnd) { mutableStateOf(record.kmEnd?.toString().orEmpty()) }
    var kmEndError by remember(record.id, record.kmEnd) { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember(record.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val recordDriver = remember(record.driverId) {
        loadOperationalDrivers(context).firstOrNull { it.id == record.driverId }
    }
    val isWearRecord = feature != OperationalFeature.ROUTE_PROFITABILITY
    val durability = record.kmEnd?.let { it - record.kmStart }
    val isPendingWear = isWearRecord && (durability == null || durability <= 0)
    val metric = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> {
            if (record.revenue == null) {
                tr("Custo importado: ", "Imported cost: ") + formatMoney(record.cost)
            } else {
                val profit = routeProfit(record)
                val margin = routeMargin(record)
                val status = if (profit >= 0) tr("lucro", "profit") else tr("prejuizo", "loss")
                "${formatMoney(profit)} $status • ${"%.1f".format(Locale("pt", "BR"), margin)}%"
            }
        }
        else -> {
            if (durability != null && durability > 0) {
                "$durability km • ${formatMoney(costPerKm(record))}/km"
            } else {
                tr("Em uso • toque para informar KM final", "In use • add final mileage")
            }
        }
    }
    val titleParts = remember(record.name) { splitOperationalTitle(record.name) }
    val accentColor = when {
        feature == OperationalFeature.ROUTE_PROFITABILITY && routeProfit(record) < 0 -> Color(0xFFDC2626)
        isPendingWear -> Color(0xFFF59E0B)
        else -> Color(0xFF059669)
    }
    val metricLabel = when (feature) {
        OperationalFeature.ROUTE_PROFITABILITY -> tr("Resultado", "Result")
        else -> tr("Durabilidade", "Durability")
    }
    val financialItems = titleParts.financialLines.map { line ->
        val parts = line.split(":", limit = 2)
        if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            tr("Valor", "Value") to line
        }
    }
    val vehicleItems = buildList {
        if (record.vehicle.isNotBlank()) {
            add(tr("Veiculo", "Vehicle") to record.vehicle)
        }
        if (feature == OperationalFeature.ROUTE_PROFITABILITY && record.driverName.isNotBlank()) {
            add(tr("Motorista", "Driver") to record.driverName)
            recordDriver?.code?.takeIf { it.isNotBlank() }?.let { add(tr("Codigo", "Code") to it) }
        }
        if (record.brandOrClient.isNotBlank()) {
            val label = if (feature == OperationalFeature.ROUTE_PROFITABILITY) tr("Cliente", "Client") else tr("Marca/origem", "Brand/source")
            add(label to record.brandOrClient)
        }
    }
    val detailItems = buildList {
        if (record.positionOrRoute.isNotBlank()) {
            val label = when (feature) {
                OperationalFeature.TIRE_ROI -> tr("Posicao", "Position")
                OperationalFeature.PARTS_DURABILITY -> tr("Local", "Location")
                OperationalFeature.ROUTE_PROFITABILITY -> tr("Rota", "Route")
            }
            add(label to record.positionOrRoute)
        }
        if (feature == OperationalFeature.TIRE_ROI) {
            add(tr("Quantidade", "Quantity") to record.quantity.coerceAtLeast(1).toString())
            if (record.recordDate.isNotBlank()) add(tr("Data", "Date") to record.recordDate)
        }
    }
    val routeDetailItems = buildList {
        if (record.vehicle.isNotBlank()) add(tr("Veiculo", "Vehicle") to record.vehicle)
        if (record.brandOrClient.isNotBlank()) add(tr("Cliente", "Client") to record.brandOrClient)
        if (record.positionOrRoute.isNotBlank()) add(tr("Rota", "Route") to record.positionOrRoute)
        if (record.kmStart > 0) add(tr("Distancia", "Distance") to "${record.kmStart} km")
    }
    val routeDriverItems = buildList {
        if (record.driverName.isNotBlank()) add(tr("Nome", "Name") to record.driverName)
        recordDriver?.code?.takeIf { it.isNotBlank() }?.let { add(tr("Codigo", "Code") to it) }
        recordDriver?.salary?.takeIf { it > 0.0 }?.let { add(tr("Salario", "Salary") to formatMoney(it)) }
        recordDriver?.taxCost?.takeIf { it > 0.0 }?.let { add(tr("Impostos/custos", "Taxes/costs") to formatMoney(it)) }
    }
    val kmItems = buildList {
        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            if (record.kmStart > 0) add(tr("Distancia", "Distance") to "${record.kmStart} km")
        } else {
            add(tr("Inicial", "Start") to "${record.kmStart} km")
            add(tr("Final", "Final") to (record.kmEnd?.let { "$it km" } ?: tr("Aguardando KM", "Waiting mileage")))
        }
    }
    val routeMoneyItems = buildList {
        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            record.revenue?.let { add(tr("Receita", "Revenue") to formatMoney(it)) }
            record.taxPercent?.let { add(tr("Imposto", "Tax") to "${formatPlainDecimal(it)}%") }
            add(tr("Custo operacional", "Operational cost") to formatMoney(record.cost))
            if (record.driverCost > 0.0) {
                add(tr("Custo motorista", "Driver cost") to formatMoney(record.driverCost))
            }
            add(tr("Custo total", "Total cost") to formatMoney(routeTotalCost(record)))
        }
    }
    val infoGroups = buildList {
        if (feature != OperationalFeature.ROUTE_PROFITABILITY && financialItems.isNotEmpty()) {
            add(OperationalInfoGroup(tr("Financeiro", "Financial"), financialItems))
        }
        if (feature != OperationalFeature.ROUTE_PROFITABILITY && vehicleItems.isNotEmpty()) {
            add(OperationalInfoGroup(tr("Veiculo", "Vehicle"), vehicleItems))
        }
        if (detailItems.isNotEmpty()) {
            add(OperationalInfoGroup(tr("Detalhes", "Details"), detailItems))
        }
        if (feature != OperationalFeature.ROUTE_PROFITABILITY && kmItems.isNotEmpty()) {
            add(OperationalInfoGroup(if (feature == OperationalFeature.ROUTE_PROFITABILITY) tr("Distancia", "Distance") else "KM", kmItems))
        }
        if (feature != OperationalFeature.ROUTE_PROFITABILITY) {
            add(OperationalInfoGroup(metricLabel, listOf(metricLabel to metric), emphasize = true))
        }
    }
    val emptyKmEndError = tr("Informe o KM final.", "Enter final mileage.")
    val invalidKmEndError = tr("KM final deve ser maior que o inicial.", "Final mileage must be greater than initial.")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accentColor)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(titleParts.title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
            }
            if (canDelete || onEdit != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = tr("Editar", "Edit"),
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = tr("Excluir", "Delete"),
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
            OperationalRouteRecordInfo(
                financialItems = routeMoneyItems,
                driverItems = routeDriverItems,
                detailItems = routeDetailItems,
                result = metric,
                subColor = subColor,
                titleColor = titleColor,
                accentColor = accentColor
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                infoGroups.forEach { group ->
                    OperationalInfoGroupCard(
                        title = group.title,
                        items = group.items,
                        valueColor = subColor,
                        accentColor = accentColor,
                        emphasize = group.emphasize
                    )
                }
            }
        }

        if (isWearRecord && onUpdateKmEnd != null) {
            if (editingKmEnd || isPendingWear) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kmEndDraft,
                        onValueChange = {
                            kmEndDraft = keepNumericInput(it)
                            kmEndError = null
                        },
                        label = { Text(tr("KM final", "Final mileage")) },
                        singleLine = true,
                        isError = kmEndError != null,
                        supportingText = kmEndError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = {
                            val finalKm = kmEndDraft.toIntOrNull()
                            when {
                                finalKm == null -> kmEndError = emptyKmEndError
                                finalKm <= record.kmStart -> kmEndError = invalidKmEndError
                                else -> {
                                    onUpdateKmEnd(finalKm)
                                    editingKmEnd = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(tr("Salvar KM final", "Save final mileage"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { editingKmEnd = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (record.kmEnd == null) tr("Atualizar KM final", "Update final mileage")
                        else tr("Editar KM final", "Edit final mileage"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Text(
                        text = tr("Apagar este registro?", "Delete this record?"),
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Essa ação remove o registro permanentemente. Deseja continuar?",
                        "This action permanently deletes the record. Do you want to continue?"
                    ),
                    color = subColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                ) {
                    Text(tr("Sim, apagar", "Yes, delete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirm = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text(tr("Cancelar", "Cancel"))
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
private fun OperationalInfoGroupCard(
    title: String,
    items: List<Pair<String, String>>,
    valueColor: Color,
    accentColor: Color,
    emphasize: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = if (emphasize) 0.12f else 0.07f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = if (emphasize) 0.20f else 0.12f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    color = valueColor.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .widthIn(max = 88.dp)
                        .alignByBaseline()
                )
                Text(
                    text = value,
                    color = if (emphasize) accentColor else valueColor,
                    fontSize = 12.sp,
                    fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline()
                )
            }
        }
    }
}

@Composable
private fun OperationalRouteRecordInfo(
    financialItems: List<Pair<String, String>>,
    driverItems: List<Pair<String, String>>,
    detailItems: List<Pair<String, String>>,
    result: String,
    subColor: Color,
    titleColor: Color,
    accentColor: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OperationalRouteSection(
            title = tr("Rota", "Route"),
            items = detailItems,
            subColor = subColor,
            titleColor = titleColor,
            accentColor = accentColor
        )

        if (financialItems.isNotEmpty()) {
            OperationalRouteMoneyGrid(
                title = tr("Custos da rota", "Route costs"),
                items = financialItems,
                subColor = subColor,
                titleColor = titleColor,
                accentColor = accentColor
            )
        }

        if (driverItems.isNotEmpty()) {
            OperationalRouteSection(
                title = tr("Motorista", "Driver"),
                items = driverItems,
                subColor = subColor,
                titleColor = titleColor,
                accentColor = accentColor
            )
        }

        OperationalRouteResultPill(
            result = result,
            subColor = subColor,
            accentColor = accentColor
        )
    }
}

@Composable
private fun OperationalRouteSection(
    title: String,
    items: List<Pair<String, String>>,
    subColor: Color,
    titleColor: Color,
    accentColor: Color
) {
    if (items.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.11f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    color = subColor.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(76.dp)
                )
                Text(
                    text = value,
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OperationalRouteMoneyGrid(
    title: String,
    items: List<Pair<String, String>>,
    subColor: Color,
    titleColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.11f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    OperationalRouteMoneyCell(
                        label = label,
                        value = value,
                        subColor = subColor,
                        titleColor = titleColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OperationalRouteMoneyCell(
    label: String,
    value: String,
    subColor: Color,
    titleColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.48f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = subColor.copy(alpha = 0.78f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = titleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OperationalRouteResultPill(
    result: String,
    subColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.13f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tr("Resultado", "Result"),
            color = subColor.copy(alpha = 0.78f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = result,
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun loadOperationalRecords(context: Context): List<OperationalRecord> {
    val json = context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .getString(OPERATIONAL_RECORDS_KEY, "[]")
        ?: "[]"
    val type = object : TypeToken<List<OperationalRecord>>() {}.type
    return runCatching {
        Gson().fromJson<List<OperationalRecord>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}

private fun saveOperationalRecords(context: Context, records: List<OperationalRecord>) {
    context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(OPERATIONAL_RECORDS_KEY, Gson().toJson(records))
        .apply()
}

private fun loadOperationalDrivers(context: Context): List<OperationalDriver> {
    val json = context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .getString(OPERATIONAL_DRIVERS_KEY, "[]")
        ?: "[]"
    val type = object : TypeToken<List<OperationalDriver>>() {}.type
    return runCatching {
        Gson().fromJson<List<OperationalDriver>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}

private fun saveOperationalDrivers(context: Context, drivers: List<OperationalDriver>) {
    context.getSharedPreferences(OPERATIONAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(OPERATIONAL_DRIVERS_KEY, Gson().toJson(drivers))
        .apply()
}

private fun upsertOperationalDriver(
    context: Context,
    drivers: List<OperationalDriver>,
    selectedDriverId: String,
    name: String,
    code: String,
    phone: String,
    salary: Double,
    taxCost: Double,
    defaultCost: Double
): OperationalDriver? {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return null
    val current = loadOperationalDrivers(context).ifEmpty { drivers }
    val existing = current.firstOrNull { it.id == selectedDriverId }
        ?: current.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
    val driver = (existing ?: OperationalDriver()).copy(
        name = cleanName,
        code = code.trim(),
        phone = phone.trim(),
        salary = salary.coerceAtLeast(0.0),
        taxCost = taxCost.coerceAtLeast(0.0),
        defaultCost = defaultCost.coerceAtLeast(0.0),
        createdAt = existing?.createdAt ?: System.currentTimeMillis()
    )
    val updated = if (current.any { it.id == driver.id }) {
        current.map { if (it.id == driver.id) driver else it }
    } else {
        current + driver
    }
    saveOperationalDrivers(context, updated)
    return driver
}

private fun upsertOperationalReminder(context: Context, record: OperationalRecord) {
    val feature = runCatching { OperationalFeature.valueOf(record.feature) }.getOrNull() ?: return
    if (feature == OperationalFeature.ROUTE_PROFITABILITY) return
    val tipo = when (feature) {
        OperationalFeature.TIRE_ROI -> TipoManutencao.PNEU
        OperationalFeature.PARTS_DURABILITY -> TipoManutencao.MECANICA
        OperationalFeature.ROUTE_PROFITABILITY -> TipoManutencao.OUTROS
    }
    val titlePrefix = when (feature) {
        OperationalFeature.TIRE_ROI -> "Controle pneu"
        OperationalFeature.PARTS_DURABILITY -> "Durabilidade peca"
        OperationalFeature.ROUTE_PROFITABILITY -> "Rota"
    }
    val current = BancoDeDados.carregarLembretes(context)
    val existing = current.firstOrNull { it.operationalRecordId == record.id }
    val reminder = (existing ?: Lembrete(
        id = "operational-${record.id}",
        carroId = record.vehicleId,
        titulo = "$titlePrefix: ${record.name}",
        peca = record.name,
        dataLimite = "",
        kmLimite = "",
        tipo = tipo
    )).copy(
        carroId = record.vehicleId,
        titulo = "$titlePrefix: ${record.name}",
        peca = record.name,
        kmLimite = (record.kmEnd ?: record.kmStart).takeIf { it > 0 }?.toString().orEmpty(),
        tipo = tipo,
        valor = record.cost,
        operationalRecordId = record.id,
        operationalFeature = feature.name,
        operationalBrand = record.brandOrClient,
        operationalPosition = record.positionOrRoute,
        operationalKmStart = record.kmStart.takeIf { it > 0 },
        operationalKmEnd = record.kmEnd
    )
    val updated = if (current.any { it.operationalRecordId == record.id }) {
        current.map { if (it.operationalRecordId == record.id) reminder else it }
    } else {
        current + reminder
    }
    BancoDeDados.salvarLembretes(context, updated)
    AdminUsersSync.syncRemindersSnapshot(updated)
}

private fun deleteOperationalReminder(context: Context, recordId: String) {
    val current = BancoDeDados.carregarLembretes(context)
    val updated = current.filterNot { it.operationalRecordId == recordId }
    if (updated.size != current.size) {
        BancoDeDados.salvarLembretes(context, updated)
        AdminUsersSync.syncRemindersSnapshot(updated)
    }
}

private fun buildIntegratedOperationalRecords(
    feature: OperationalFeature,
    vehicles: List<CarroInfo>,
    maintenanceRecords: List<Lembrete>,
    travelTrips: List<OperationalTravelTrip>,
    selectedVehicleId: String
): List<OperationalRecord> {
    val vehicleById = vehicles.associateBy { it.id }
    return when (feature) {
        OperationalFeature.TIRE_ROI -> {
            val source = maintenanceRecords
                .filter { it.tipo == TipoManutencao.PNEU }
                .filter(::isLembreteRealizado)
                .filter { selectedVehicleId.isBlank() || it.carroId == selectedVehicleId }
            source.filter { it.hasOperationalMetadataFor(feature) }
                .map { it.toOperationalMetadataRecord(feature, vehicleById[it.carroId], "Pneu") } +
                source.filterNot { it.hasOperationalMetadataFor(feature) }
                    .toOperationalWearRecords(feature, vehicleById, "Pneu")
        }

        OperationalFeature.PARTS_DURABILITY -> {
            val source = maintenanceRecords
                .filter(::isQuickWearMaintenance)
                .filter(::isLembreteRealizado)
                .filter { selectedVehicleId.isBlank() || it.carroId == selectedVehicleId }
            source.filter { it.hasOperationalMetadataFor(feature) }
                .map { it.toOperationalMetadataRecord(feature, vehicleById[it.carroId], "Peca") } +
                source.filterNot { it.hasOperationalMetadataFor(feature) }
                    .toOperationalWearRecords(feature, vehicleById, "Peca")
        }

        OperationalFeature.ROUTE_PROFITABILITY -> travelTrips
            .filter { trip ->
                selectedVehicleId.isBlank() ||
                    vehicleById[selectedVehicleId]?.displayName()?.let { selectedName ->
                        trip.vehicleNames.any { it.equals(selectedName, ignoreCase = true) }
                    } == true
            }
            .map { trip ->
                OperationalRecord(
                    id = "auto-trip-${trip.id}",
                    feature = feature.name,
                    name = trip.name,
                    brandOrClient = trip.location,
                    vehicleId = selectedVehicleId,
                    vehicle = trip.vehicleNames.joinToString(", ").ifBlank { "Viagem" },
                    positionOrRoute = trip.location.ifBlank { trip.name },
                    kmStart = 0,
                    kmEnd = null,
                    cost = trip.cost,
                    revenue = null,
                    taxPercent = null,
                    createdAt = trip.createdAt
                )
            }
    }
}

private fun Lembrete.hasOperationalMetadataFor(feature: OperationalFeature): Boolean {
    return operationalFeature == feature.name && operationalRecordId.isNotBlank() && isLembreteRealizado(this)
}

private fun Lembrete.toOperationalMetadataRecord(
    feature: OperationalFeature,
    vehicle: CarroInfo?,
    fallbackName: String
): OperationalRecord {
    return OperationalRecord(
        id = operationalRecordId,
        feature = feature.name,
        name = peca.ifBlank { titulo.ifBlank { fallbackName } },
        brandOrClient = operationalBrand.ifBlank { "Registro do app" },
        vehicleId = carroId,
        vehicle = vehicle?.displayName().orEmpty().ifBlank { "Veiculo" },
        positionOrRoute = operationalPosition.ifBlank { tipo.label },
        kmStart = operationalKmStart ?: kmFromText(kmLimite) ?: vehicle?.kmAtual ?: 0,
        kmEnd = operationalKmEnd,
        cost = valor,
        revenue = null,
        taxPercent = null
    )
}

private fun List<Lembrete>.toOperationalWearRecords(
    feature: OperationalFeature,
    vehicleById: Map<String, CarroInfo>,
    fallbackName: String
): List<OperationalRecord> {
    return groupBy { "${it.carroId}|${wearKey(it)}" }
        .flatMap { (_, group) ->
            val ordered = group.sortedBy { kmFromText(it.kmLimite) ?: Int.MAX_VALUE }
            if (ordered.size >= 2) {
                ordered.zipWithNext().mapNotNull { (start, end) ->
                    val startKm = kmFromText(start.kmLimite) ?: return@mapNotNull null
                    val endKm = kmFromText(end.kmLimite) ?: return@mapNotNull null
                    if (endKm <= startKm) return@mapNotNull null
                    end.toOperationalWearRecord(
                        feature = feature,
                        vehicle = vehicleById[end.carroId],
                        fallbackName = fallbackName,
                        kmStart = startKm,
                        kmEnd = endKm,
                        idSuffix = "${start.id}-${end.id}"
                    )
                }
            } else {
                ordered.map { item ->
                    item.toOperationalWearRecord(
                        feature = feature,
                        vehicle = vehicleById[item.carroId],
                        fallbackName = fallbackName,
                        kmStart = kmFromText(item.kmLimite) ?: vehicleById[item.carroId]?.kmAtual ?: 0,
                        kmEnd = null,
                        idSuffix = item.id
                    )
                }
            }
        }
}

private fun Lembrete.toOperationalWearRecord(
    feature: OperationalFeature,
    vehicle: CarroInfo?,
    fallbackName: String,
    kmStart: Int,
    kmEnd: Int?,
    idSuffix: String
): OperationalRecord {
    return OperationalRecord(
        id = "auto-maint-${feature.name}-$idSuffix",
        feature = feature.name,
        name = peca.ifBlank { titulo.ifBlank { fallbackName } },
        brandOrClient = "Historico de manutencao",
        vehicleId = carroId,
        vehicle = vehicle?.displayName().orEmpty().ifBlank { "Veiculo" },
        positionOrRoute = tipo.label,
        kmStart = kmStart,
        kmEnd = kmEnd,
        cost = valor,
        revenue = null,
        taxPercent = null
    )
}

private fun isQuickWearMaintenance(item: Lembrete): Boolean {
    val text = "${item.titulo} ${item.peca} ${item.tipo.label}".lowercase(Locale.ROOT)
    val keywordMatch = listOf("pastilha", "freio", "pivo", "pivô", "terminal", "suspens", "bucha", "amortec").any { text.contains(it) }
    return item.tipo == TipoManutencao.FREIO || item.tipo == TipoManutencao.MECANICA || keywordMatch
}

private fun wearKey(item: Lembrete): String {
    return "${item.peca.ifBlank { item.titulo }.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }}|${item.tipo.name}"
}

private fun kmFromText(value: String): Int? = value.filter { it.isDigit() }.toIntOrNull()

private fun loadOperationalTravelTrips(context: Context): List<OperationalTravelTrip> {
    val raw = context.getSharedPreferences("travel_expenses_prefs", Context.MODE_PRIVATE)
        .getString("travel_trips_json", null)
        ?: return emptyList()
    return runCatching {
        val tripsArray = JSONArray(raw)
        buildList {
            for (i in 0 until tripsArray.length()) {
                val tripObj = tripsArray.getJSONObject(i)
                val expensesArray = tripObj.optJSONArray("expenses") ?: JSONArray()
                var cost = 0.0
                val vehicles = linkedSetOf<String>()
                for (j in 0 until expensesArray.length()) {
                    val expense = expensesArray.getJSONObject(j)
                    val original = expense.optDouble("originalAmount", expense.optDouble("amount", 0.0))
                    val discount = expense.optDouble("discountAmount", 0.0)
                    val amount = expense.optDouble("amount", 0.0)
                    cost += if (discount > 0.0) (original - discount).coerceAtLeast(0.0) else amount
                    expense.optString("vehicleName").takeIf { it.isNotBlank() }?.let(vehicles::add)
                }
                add(
                    OperationalTravelTrip(
                        id = tripObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = tripObj.optString("name").ifBlank { "Minha viagem" },
                        location = tripObj.optString("location"),
                        vehicleNames = vehicles.toList(),
                        cost = cost,
                        createdAt = tripObj.optLong("createdAtMillis").takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun fuelCostPerKm(vehicleId: String, fuelRecords: List<Abastecimento>): Double {
    val records = fuelRecords.filter { it.carroId == vehicleId && it.km != null }.sortedBy { it.km }
    val firstKm = records.firstOrNull()?.km ?: return 0.0
    val lastKm = records.lastOrNull()?.km ?: return 0.0
    val distance = lastKm - firstKm
    if (distance <= 0) return 0.0
    return records.drop(1).sumOf { it.valorPago } / distance
}

private fun maintenanceCostPerKm(vehicleId: String, maintenanceRecords: List<Lembrete>): Double {
    val records = maintenanceRecords.filter { it.carroId == vehicleId && it.valor > 0.0 }
    val maxKm = records.mapNotNull { kmFromText(it.kmLimite) }.maxOrNull() ?: return 0.0
    if (maxKm <= 0) return 0.0
    return records.sumOf { it.valor } / maxKm
}

private fun operationalWearCostPerKm(vehicleId: String, operationalRecords: List<OperationalRecord>): Double {
    return operationalRecords
        .filter { it.vehicleId == vehicleId }
        .filter {
            it.feature == OperationalFeature.TIRE_ROI.name ||
                it.feature == OperationalFeature.PARTS_DURABILITY.name
        }
        .filter { it.kmEnd != null && it.kmEnd > it.kmStart && it.cost > 0.0 }
        .groupBy { operationalReplacementKey(it) }
        .values
        .sumOf { group ->
            group.minOfOrNull { costPerKm(it) } ?: 0.0
        }
}

private fun routeRealCostPerKm(
    vehicleId: String,
    fuelRecords: List<Abastecimento>,
    maintenanceRecords: List<Lembrete>,
    operationalRecords: List<OperationalRecord>
): Double {
    return fuelCostPerKm(vehicleId, fuelRecords) +
        maintenanceCostPerKm(vehicleId, maintenanceRecords) +
        operationalWearCostPerKm(vehicleId, operationalRecords)
}

private fun estimateRouteCost(
    vehicleId: String,
    distanceKm: Int,
    fuelRecords: List<Abastecimento>,
    maintenanceRecords: List<Lembrete>,
    operationalRecords: List<OperationalRecord>
): Double {
    val costPerKm = routeRealCostPerKm(vehicleId, fuelRecords, maintenanceRecords, operationalRecords)
    return costPerKm * distanceKm
}

private fun CarroInfo.displayName(): String {
    return listOf(nome, marca, modelo)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "Veiculo" }
}

private fun formatPlainDecimal(value: Double): String {
    return "%.2f".format(Locale.US, value).replace(".", ",")
}

private fun currentOperationalDate(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
}

private fun splitOperationalTitle(raw: String): OperationalTitleParts {
    val lines = raw
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lines.isEmpty()) return OperationalTitleParts(title = "Registro", financialLines = emptyList())

    val financialPrefixes = listOf(
        "valor total",
        "desconto",
        "valor final",
        "total",
        "valor"
    )
    val financialLines = lines.filter { line ->
        val normalized = line.lowercase(Locale.ROOT)
        financialPrefixes.any { normalized.startsWith(it) }
    }
    val title = lines.firstOrNull { line ->
        val normalized = line.lowercase(Locale.ROOT)
        financialPrefixes.none { normalized.startsWith(it) }
    } ?: lines.first()

    return OperationalTitleParts(title = title, financialLines = financialLines)
}

private fun keepNumericInput(value: String): String = value.filter { it.isDigit() }

private fun operationalReplacementKey(record: OperationalRecord): String {
    val feature = runCatching { OperationalFeature.valueOf(record.feature) }.getOrNull()
    val position = record.positionOrRoute.normalizedOperationalKey()
    val name = record.name.normalizedOperationalKey()
    return when (feature) {
        OperationalFeature.TIRE_ROI -> position.ifBlank { name }
        OperationalFeature.PARTS_DURABILITY -> listOf(name, position)
            .filter { it.isNotBlank() }
            .joinToString("|")
            .ifBlank { name.ifBlank { position } }
        else -> position.ifBlank { name }
    }
}

private fun String.normalizedOperationalKey(): String {
    return lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
}

private fun keepDecimalInput(value: String): String {
    var hasSeparator = false
    return value.filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !hasSeparator -> {
                hasSeparator = true
                true
            }
            else -> false
        }
    }
}

private fun formatDriverPhoneInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(13)
    val hasCountryCode = digits.startsWith("55") && digits.length > 11
    val localDigits = if (hasCountryCode) digits.drop(2).take(11) else digits.take(11)
    val formattedLocal = when {
        localDigits.length <= 2 -> localDigits
        localDigits.length <= 6 -> "(${localDigits.take(2)}) ${localDigits.drop(2)}"
        localDigits.length <= 10 -> {
            val area = localDigits.take(2)
            val prefix = localDigits.drop(2).take(4)
            val suffix = localDigits.drop(6)
            "($area) $prefix-$suffix"
        }
        else -> {
            val area = localDigits.take(2)
            val prefix = localDigits.drop(2).take(5)
            val suffix = localDigits.drop(7)
            "($area) $prefix-$suffix"
        }
    }
    return if (hasCountryCode && formattedLocal.isNotBlank()) "+55 $formattedLocal" else formattedLocal
}

private fun parseMoneyInput(value: String): Double? {
    val normalized = value.replace(".", "").replace(",", ".")
    return normalized.toDoubleOrNull()
}

private fun costPerKm(record: OperationalRecord): Double {
    val distance = (record.kmEnd ?: record.kmStart) - record.kmStart
    return if (distance > 0) record.cost / distance else 0.0
}

private fun routeTotalCost(record: OperationalRecord): Double {
    return record.cost + record.driverCost.coerceAtLeast(0.0)
}

private fun routeProfit(record: OperationalRecord): Double {
    val revenue = record.revenue ?: 0.0
    val tax = revenue * ((record.taxPercent ?: 0.0) / 100.0)
    return revenue - routeTotalCost(record) - tax
}

private fun routeMargin(record: OperationalRecord): Double {
    val revenue = record.revenue ?: 0.0
    return if (revenue > 0) (routeProfit(record) / revenue) * 100.0 else 0.0
}

private fun formatMoney(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

private fun operationalReportTitle(feature: OperationalFeature): String = when (feature) {
    OperationalFeature.TIRE_ROI -> trNow("Relatorio Controle de Pneus", "Tire Tracking Report")
    OperationalFeature.PARTS_DURABILITY -> trNow("Relatorio Durabilidade de Pecas", "Parts Durability Report")
    OperationalFeature.ROUTE_PROFITABILITY -> trNow("Relatorio Rentabilidade de Rotas", "Route Profitability Report")
}

private fun operationalReportFileSlug(feature: OperationalFeature): String = when (feature) {
    OperationalFeature.TIRE_ROI -> "controle_pneus"
    OperationalFeature.PARTS_DURABILITY -> "durabilidade_pecas"
    OperationalFeature.ROUTE_PROFITABILITY -> "rentabilidade_rotas"
}

@Composable
private fun OperationalReportOptionsDialog(
    bg: Color,
    textPrimary: Color,
    cardBorder: Color,
    accentBlue: Color,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkDialog = bg.luminance() < 0.5f
    val dialogContainer = if (isDarkDialog) Color(0xFF070F1D) else Color(0xFFFFFFFF)
    val iconContainer = accentBlue.copy(alpha = if (isDarkDialog) 0.24f else 0.14f)
    val closeContainer = if (isDarkDialog) Color.Transparent else Color(0xFFF8FAFC)
    val closeBorder = if (isDarkDialog) cardBorder.copy(alpha = 0.55f) else cardBorder.copy(alpha = 0.28f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = dialogContainer),
            border = BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(iconContainer, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    tr("Exportar em PDF", "Export as PDF"),
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Exportar PDF", "Export PDF"), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, closeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(closeContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(tr("Fechar", "Close"))
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverManagerScreen(
    drivers: List<OperationalDriver>,
    routeRecords: List<OperationalRecord>,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    screenBg: Color,
    onDismiss: () -> Unit,
    onSaveDriver: (
        editingDriverId: String?,
        name: String,
        code: String,
        phone: String,
        salary: String,
        taxCost: String,
        defaultCost: String
    ) -> Boolean,
    onDeleteDriver: (OperationalDriver) -> Unit
) {
    var editingDriver by remember { mutableStateOf<OperationalDriver?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<OperationalDriver?>(null) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var taxCost by remember { mutableStateOf("") }
    var defaultCost by remember { mutableStateOf("") }

    fun openEditor(driver: OperationalDriver?) {
        editingDriver = driver
        name = driver?.name.orEmpty()
        code = driver?.code.orEmpty()
        phone = driver?.phone.orEmpty()
        salary = driver?.salary?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        taxCost = driver?.taxCost?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        defaultCost = driver?.defaultCost?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        showEditor = true
    }

    val sortedDrivers = remember(drivers) {
        drivers.sortedWith(compareBy<OperationalDriver> { it.name.lowercase(Locale.getDefault()) }.thenBy { it.code })
    }

    if (showEditor) {
        DriverEditorScreen(
            title = if (editingDriver == null) tr("Cadastrar motorista", "Add driver") else tr("Editar motorista", "Edit driver"),
            subtitle = tr(
                "Os vinculos com linhas serao mantidos pelo identificador interno.",
                "Route links are kept by the internal identifier."
            ),
            name = name,
            onNameChange = { name = it },
            code = code,
            onCodeChange = { code = it },
            phone = phone,
            onPhoneChange = { phone = formatDriverPhoneInput(it) },
            salary = salary,
            onSalaryChange = { salary = keepDecimalInput(it) },
            taxCost = taxCost,
            onTaxCostChange = { taxCost = keepDecimalInput(it) },
            cost = defaultCost,
            onCostChange = { defaultCost = keepDecimalInput(it) },
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            screenBg = screenBg,
            onSave = {
                val saved = onSaveDriver(editingDriver?.id, name, code, phone, salary, taxCost, defaultCost)
                if (saved) showEditor = false
            },
            onDismiss = { showEditor = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
            }
            IconButton(
                onClick = { openEditor(null) },
                modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = tr("Cadastrar motorista", "Add driver"), tint = titleColor)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(29.dp))
            }
            Text(
                text = tr("Gerenciar Motoristas", "Manage Drivers"),
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = tr(
                    "Edite os motoristas usados nas linhas sem criar cadastro duplicado.",
                    "Edit the drivers used on routes without creating duplicate records."
                ),
                color = subColor,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = { openEditor(null) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF357AE8), contentColor = Color.White)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Novo motorista", "New driver"), fontWeight = FontWeight.Bold)
        }

        if (sortedDrivers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Nenhum motorista cadastrado", "No drivers registered"),
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = tr("Cadastre aqui e use no seletor das linhas.", "Add one here and use it in route selectors."),
                    color = subColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            sortedDrivers.forEach { driver ->
                DriverManagerCard(
                    driver = driver,
                    linkedRoutes = routeRecords.filter { it.driverId == driver.id },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subColor = subColor,
                    onEdit = { openEditor(driver) },
                    onDelete = { deleteCandidate = driver }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }

    deleteCandidate?.let { driver ->
        val linkedRoutes = routeRecords.filter { it.driverId == driver.id }
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = {
                Text(
                    if (linkedRoutes.isEmpty()) tr("Excluir motorista?", "Delete driver?")
                    else tr("Motorista vinculado", "Driver linked")
                )
            },
            text = {
                Text(
                    if (linkedRoutes.isEmpty()) {
                        tr("Esse motorista sera removido do cadastro.", "This driver will be removed.")
                    } else {
                        tr(
                            "Esse motorista esta vinculado a ${linkedRoutes.size} linha(s). Ao excluir, essas linhas ficarao sem motorista para evitar referencia quebrada.",
                            "This driver is linked to ${linkedRoutes.size} route(s). Deleting will leave those routes without a driver to avoid broken references."
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDriver(driver)
                        deleteCandidate = null
                    }
                ) {
                    Text(tr("Excluir", "Delete"), color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(tr("Cancelar", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun DriverManagerCard(
    driver: OperationalDriver,
    linkedRoutes: List<OperationalRecord>,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = driver.name,
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        driver.code.takeIf { it.isNotBlank() }?.let { "Cod: $it" },
                        driver.phone.takeIf { it.isNotBlank() }
                    ).filterNotNull().joinToString(" • ").ifBlank { tr("Sem codigo ou telefone", "No code or phone") },
                    color = subColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Edit, contentDescription = tr("Editar", "Edit"), tint = Color(0xFF60A5FA))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Delete, contentDescription = tr("Excluir", "Delete"), tint = Color(0xFFEF4444))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DriverMetricChip(
                label = tr("Salario", "Salary"),
                value = formatMoney(driver.salary),
                titleColor = titleColor,
                subColor = subColor,
                modifier = Modifier.weight(1f)
            )
            DriverMetricChip(
                label = tr("Custos/impostos", "Taxes/costs"),
                value = formatMoney(driver.taxCost),
                titleColor = titleColor,
                subColor = subColor,
                modifier = Modifier.weight(1f)
            )
        }
        DriverMetricChip(
            label = tr("Custo padrao por linha", "Default cost per route"),
            value = formatMoney(driver.defaultCost),
            titleColor = titleColor,
            subColor = subColor,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2563EB).copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.18f)), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tr("Linhas vinculadas", "Linked routes"),
                color = subColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (linkedRoutes.isEmpty()) {
                    tr("Nenhuma linha vinculada", "No linked routes")
                } else {
                    linkedRoutes
                        .take(4)
                        .joinToString(" • ") { splitOperationalTitle(it.name).title.ifBlank { it.positionOrRoute } }
                        .let { names ->
                            if (linkedRoutes.size > 4) "$names +${linkedRoutes.size - 4}" else names
                        }
                },
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun DriverMetricChip(
    label: String,
    value: String,
    titleColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DriverEditorScreen(
    title: String,
    subtitle: String,
    name: String,
    onNameChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    salary: String,
    onSalaryChange: (String) -> Unit,
    taxCost: String,
    onTaxCostChange: (String) -> Unit,
    cost: String,
    onCostChange: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    screenBg: Color,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subtitle,
                color = subColor,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                OperationalTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = tr("Nome do motorista", "Driver name")
                )
                OperationalTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = tr("Codigo/identificador", "Code/identifier")
                )
                OperationalTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = tr("Telefone", "Phone"),
                    keyboardType = KeyboardType.Phone
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OperationalTextField(
                        value = salary,
                        onValueChange = onSalaryChange,
                        label = tr("Salario", "Salary"),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    OperationalTextField(
                        value = taxCost,
                        onValueChange = onTaxCostChange,
                        label = tr("Custos/impostos", "Taxes/costs"),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
                OperationalTextField(
                    value = cost,
                    onValueChange = onCostChange,
                    label = tr("Custo padrao por linha", "Default cost per route"),
                    keyboardType = KeyboardType.Decimal
                )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Cancelar", "Cancel"))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(tr("Salvar", "Save"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun generateOperationalReportPdf(
    context: Context,
    feature: OperationalFeature,
    records: List<OperationalRecord>,
    vehiclesCount: Int,
    importedRecordsCount: Int,
    realCostPerKm: Double,
    routeSuggestedCost: Double
): File? = runCatching {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageIndex = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
    var canvas = page.canvas
    val marginX = 36f
    val contentWidth = pageWidth - marginX * 2
    var y = 108f
    val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    val accentColor = android.graphics.Color.parseColor("#2563EB")
    val cardBg = android.graphics.Color.parseColor("#F8FAFC")
    val cardBorder = android.graphics.Color.parseColor("#E2E8F0")
    val textColor = android.graphics.Color.parseColor("#0F172A")
    val mutedColor = android.graphics.Color.parseColor("#475569")
    val successColor = android.graphics.Color.parseColor("#059669")
    val dangerColor = android.graphics.Color.parseColor("#DC2626")

    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val headerInfoPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#BFDBFE")
        textSize = 11f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val sectionPaint = Paint().apply {
        color = accentColor
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val labelPaint = Paint().apply {
        color = mutedColor
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val valuePaint = Paint().apply {
        color = textColor
        textSize = 12f
        isAntiAlias = true
    }
    val valueBoldPaint = Paint(valuePaint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val smallPaint = Paint().apply {
        color = mutedColor
        textSize = 10f
        isAntiAlias = true
    }
    val tableHeaderPaint = Paint().apply { color = accentColor }
    val tableHeaderTextPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val cardPaint = Paint().apply { color = cardBg; isAntiAlias = true }
    val cardBorderPaint = Paint().apply {
        color = cardBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        isAntiAlias = true
    }
    val accentPaint = Paint().apply { color = accentColor; isAntiAlias = true }
    val pageNumberPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 9f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun fit(text: String, maxChars: Int): String {
        val clean = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= maxChars) clean else clean.take(maxChars - 3) + "..."
    }

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 82f, Paint().apply { color = accentColor })
        canvas.drawText("RELATORIO OPERACIONAL", pageWidth / 2f, 42f, titlePaint)
        canvas.drawText("${operationalReportTitle(feature)} • $generatedAt", pageWidth / 2f, 64f, headerInfoPaint)
    }

    fun finishPage() {
        canvas.drawText("- $pageIndex -", pageWidth / 2f, pageHeight - 18f, pageNumberPaint)
        document.finishPage(page)
    }

    fun newPage() {
        finishPage()
        pageIndex++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        canvas = page.canvas
        y = 108f
        drawHeader()
    }

    fun ensureSpace(height: Float) {
        if (y + height > pageHeight - 46f) newPage()
    }

    fun drawSection(title: String) {
        ensureSpace(28f)
        canvas.drawRect(marginX, y - 13f, marginX + 4f, y + 3f, accentPaint)
        canvas.drawText(title, marginX + 10f, y, sectionPaint)
        y += 18f
    }

    fun drawMetric(label: String, value: String, left: Float, top: Float, width: Float, valueColor: Int = textColor) {
        val rect = android.graphics.RectF(left, top, left + width, top + 62f)
        canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
        canvas.drawRoundRect(rect, 12f, 12f, cardBorderPaint)
        canvas.drawText(label.uppercase(Locale.getDefault()), left + 12f, top + 22f, labelPaint)
        canvas.drawText(fit(value, 32), left + 12f, top + 44f, Paint(valueBoldPaint).apply { color = valueColor })
    }

    fun drawKeyValue(label: String, value: String, left: Float, baseline: Float, valueX: Float, maxChars: Int = 48) {
        canvas.drawText(label, left, baseline, labelPaint)
        canvas.drawText(fit(value, maxChars), valueX, baseline, valuePaint)
    }

    val finished = records.filter { it.kmEnd != null && it.kmEnd > it.kmStart }
    val routeRecords = records.filter { it.revenue != null }
    val drivers = if (feature == OperationalFeature.ROUTE_PROFITABILITY) loadOperationalDrivers(context) else emptyList()
    val bestDurability = finished.maxByOrNull { (it.kmEnd ?: 0) - it.kmStart }?.let { "${(it.kmEnd ?: 0) - it.kmStart} km" }
        ?: trNow("Aguardando KM final", "Waiting for final mileage")
    val lowestCost = finished.minByOrNull { costPerKm(it) }?.let { "${formatMoney(costPerKm(it))}/km" }
        ?: trNow("Aguardando KM", "Waiting mileage")
    val totalBalance = routeRecords.sumOf { routeProfit(it) }
    val currentMonthTag = SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date())
    val currentMonthRouteBalance = routeRecords
        .filter { SimpleDateFormat("MM/yyyy", Locale("pt", "BR")).format(Date(it.createdAt)) == currentMonthTag }
        .sumOf { routeProfit(it) }
    val bestRoute = routeRecords.maxByOrNull { routeProfit(it) }
    val bestRouteValue = bestRoute?.let { "${formatMoney(routeProfit(it))} • ${formatPlainDecimal(routeMargin(it))}%" }
        ?: trNow("Sem rotas calculadas", "No calculated routes")

    drawHeader()
    drawSection("RESUMO")
    val metricGap = 10f
    val metricWidth = (contentWidth - metricGap) / 2f
    drawMetric("Registros", records.size.toString(), marginX, y, metricWidth)
    drawMetric("Veiculos", vehiclesCount.toString(), marginX + metricWidth + metricGap, y, metricWidth)
    y += 72f
    drawMetric("Importados", importedRecordsCount.toString(), marginX, y, metricWidth)
    if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
        val totalDriverCost = drivers.sumOf { it.salary + it.taxCost + it.defaultCost }
        drawMetric("Saldo total", formatMoney(totalBalance), marginX + metricWidth + metricGap, y, metricWidth, if (totalBalance >= 0) successColor else dangerColor)
        y += 72f
        drawMetric("Custo/km real", formatMoney(realCostPerKm), marginX, y, metricWidth)
        drawMetric("Sugestao rota", formatMoney(routeSuggestedCost), marginX + metricWidth + metricGap, y, metricWidth)
        y += 72f
        drawMetric("Motoristas", drivers.size.toString(), marginX, y, metricWidth)
        drawMetric("Custos motoristas", formatMoney(totalDriverCost), marginX + metricWidth + metricGap, y, metricWidth)
        y += 72f
        drawMetric("Saldo mes $currentMonthTag", formatMoney(currentMonthRouteBalance), marginX, y, metricWidth, if (currentMonthRouteBalance >= 0) successColor else dangerColor)
        drawMetric("Melhor lucro", bestRouteValue, marginX + metricWidth + metricGap, y, metricWidth, successColor)
    } else {
        drawMetric("Menor custo/km", lowestCost, marginX + metricWidth + metricGap, y, metricWidth)
        y += 72f
        drawMetric("Melhor durabilidade", bestDurability, marginX, y, contentWidth, successColor)
    }
    y += 88f

    if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
        drawSection("MOTORISTAS")
        if (drivers.isEmpty()) {
            canvas.drawText("Nenhum motorista cadastrado.", marginX, y + 6f, valuePaint)
            y += 24f
        } else {
            drivers.sortedBy { it.name.lowercase(Locale.ROOT) }.forEach { driver ->
                val linkedRoutes = records
                    .filter { it.driverId == driver.id }
                    .map { splitOperationalTitle(it.name).title }
                    .distinct()
                val height = 108f
                val bottomGap = 32f
                ensureSpace(height + bottomGap)
                val top = y
                canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardPaint)
                canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardBorderPaint)
                canvas.drawText(fit(driver.name, 38), marginX + 12f, top + 22f, valueBoldPaint)
                drawKeyValue("Codigo", driver.code.ifBlank { "-" }, marginX + 12f, top + 42f, marginX + 72f, 24)
                drawKeyValue("Salario", formatMoney(driver.salary), marginX + 190f, top + 42f, marginX + 250f, 24)
                drawKeyValue("Custos/imp.", formatMoney(driver.taxCost), marginX + 360f, top + 42f, marginX + 430f, 22)
                val linhas = linkedRoutes.joinToString(", ").ifBlank { "-" }
                drawKeyValue("Linhas", fit(linhas, 62), marginX + 12f, top + 66f, marginX + 72f, 62)
                y += height + bottomGap
            }
        }
        y += 30f
    }

    ensureSpace(46f)
    drawSection("HISTORICO")
    if (records.isEmpty()) {
        canvas.drawText("Nenhum registro encontrado para este relatorio.", marginX, y + 6f, valuePaint)
        y += 24f
    } else {
        records.sortedByDescending { it.createdAt }.forEachIndexed { index, record ->
            val height = if (feature == OperationalFeature.ROUTE_PROFITABILITY) 232f else 154f
            ensureSpace(height + 16f)
            val top = y
            canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardPaint)
            canvas.drawRoundRect(android.graphics.RectF(marginX, top, marginX + contentWidth, top + height), 12f, 12f, cardBorderPaint)
            canvas.drawRect(marginX, top, marginX + 4f, top + height, accentPaint)
            val parts = splitOperationalTitle(record.name)
            canvas.drawText("${index + 1}. ${fit(parts.title, 58)}", marginX + 14f, top + 22f, valueBoldPaint)
            var rowY = top + 48f
            if (feature == OperationalFeature.ROUTE_PROFITABILITY) {
                val recordDriver = drivers.firstOrNull { it.id == record.driverId }
                val routeRows = listOf(
                    "Receita" to (record.revenue?.let(::formatMoney) ?: "-"),
                    "Custo operacional" to formatMoney(record.cost),
                    "Custo motorista" to formatMoney(record.driverCost),
                    "Custo total" to formatMoney(routeTotalCost(record)),
                    "Imposto" to "${formatPlainDecimal(record.taxPercent ?: 0.0)}%",
                    "Resultado" to "${formatMoney(routeProfit(record))} • ${formatPlainDecimal(routeMargin(record))}%",
                    "Veiculo" to record.vehicle.ifBlank { "-" },
                    "Motorista" to record.driverName.ifBlank { "-" },
                    "Codigo" to (recordDriver?.code?.ifBlank { "-" } ?: "-"),
                    "Rota" to record.positionOrRoute.ifBlank { "-" },
                    "Distancia" to "${record.kmStart} km"
                )
                routeRows.forEach { (label, value) ->
                    drawKeyValue(label, value, marginX + 14f, rowY, marginX + 128f, 70)
                    rowY += 16f
                }
            } else {
                drawKeyValue("Marca/origem", record.brandOrClient.ifBlank { "-" }, marginX + 14f, rowY, marginX + 108f, 62)
                if (feature == OperationalFeature.TIRE_ROI) {
                    drawKeyValue("Quantidade", record.quantity.coerceAtLeast(1).toString(), marginX + 330f, rowY, marginX + 405f)
                }
                rowY += 20f
                drawKeyValue("Veiculo", record.vehicle.ifBlank { "-" }, marginX + 14f, rowY, marginX + 108f, 68)
                if (feature == OperationalFeature.TIRE_ROI) {
                    drawKeyValue("Data", record.recordDate.ifBlank { "-" }, marginX + 330f, rowY, marginX + 405f)
                }
                rowY += 20f
                drawKeyValue("Posicao/local", record.positionOrRoute.ifBlank { "-" }, marginX + 14f, rowY, marginX + 100f)
                drawKeyValue("Custo", formatMoney(record.cost), marginX + 330f, rowY, marginX + 390f)
                rowY += 20f
                drawKeyValue("KM inicial", "${record.kmStart} km", marginX + 14f, rowY, marginX + 100f)
                drawKeyValue("KM final", record.kmEnd?.let { "$it km" } ?: "Aguardando", marginX + 220f, rowY, marginX + 290f)
                val durability = record.kmEnd?.let { it - record.kmStart }?.takeIf { it > 0 }
                rowY += 20f
                drawKeyValue("Durabilidade", durability?.let { "$it km • ${formatMoney(costPerKm(record))}/km" } ?: "Aguardando KM final", marginX + 14f, rowY, marginX + 100f, 62)
            }
            y += height + 14f
        }
    }

    finishPage()
    val file = File(context.cacheDir, "relatorio_${operationalReportFileSlug(feature)}_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    file
}.getOrElse {
    null
}

private fun shareOperationalPdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, trNow("Compartilhar PDF", "Share PDF")))
}

@Composable
private fun UpgradePlanCard(
    title: String,
    subtitle: String,
    buttonText: String,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subtitleColor: Color,
    buttonBg: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Diamond, contentDescription = null, tint = titleColor, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = subtitleColor, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
        ) {
            Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HubBenefitsGrid(
    features: List<HubFeatureCubeData>,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subtitleColor: Color,
    dimColor: Color,
    isDark: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        features.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { feature ->
                    HubFeatureCube(
                        feature = feature,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        dimColor = dimColor,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class HubFeatureCubeData(
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color,
    val title: String,
    val subtitle: String,
    val onClick: (() -> Unit)?,
    val blocked: Boolean = false
)

@Composable
private fun HubFeatureCube(
    feature: HubFeatureCubeData,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subtitleColor: Color,
    dimColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .then(if (feature.blocked) Modifier.alpha(0.55f) else Modifier)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .then(if (feature.onClick != null) Modifier.clickable(onClick = feature.onClick) else Modifier)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(feature.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, contentDescription = null, tint = feature.iconColor, modifier = Modifier.size(21.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                feature.title,
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                feature.subtitle,
                color = subtitleColor,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        if (feature.onClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(tr("Abrir", "Open"), color = feature.iconColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = feature.iconColor, modifier = Modifier.size(10.dp))
            }
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
    onClick: (() -> Unit)?,
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
        if (onClick != null) {
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = chevronColor, modifier = Modifier.size(14.dp))
        }
    }
}
// separa tudo isso antes de implementar coisa nova sem vergonha