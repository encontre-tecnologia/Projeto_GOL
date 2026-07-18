package br.com.gui.carlembrete

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    var selectedCorporateModule by remember { mutableStateOf<CorporateFleetModule?>(null) }
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

    selectedCorporateModule?.takeIf { hasFleetOperationalModules }?.let { module ->
        BackHandler { selectedCorporateModule = null }
        CorporateFleetModuleScreen(
            module = module,
            onDismiss = { selectedCorporateModule = null },
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
                        if (featureAllowed("frota")) add(HubFeatureCubeData(
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF0284C7),
                            iconBg = if (isDark) Color(0xFF082F49) else Color(0xFFE0F2FE),
                            title = tr("Visão geral da frota", "Fleet overview"),
                            subtitle = tr("Todos os veiculos da garagem", "All garage vehicles"),
                            onClick = onOpenFleetOverview
                        ))
                        add(HubFeatureCubeData(
                            icon = Icons.Default.Route,
                            iconColor = Color(0xFF0891B2),
                            iconBg = if (isDark) Color(0xFF083344) else Color(0xFFE0F2FE),
                            title = tr("Reservas", "Reservations"),
                            subtitle = tr("Calendario, retirada e devolucao", "Calendar, pickup and return"),
                            onClick = { selectedCorporateModule = CorporateFleetModule.RESERVATIONS }
                        ))
                        if (false && featureAllowed("frota_qrcode")) add(HubFeatureCubeData(
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF16A34A),
                            iconBg = if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7),
                            title = tr("QR Code e retirada", "QR code pickup"),
                            subtitle = tr("Validacao do veiculo e do motorista", "Vehicle and driver validation"),
                            onClick = { selectedCorporateModule = CorporateFleetModule.QR_PICKUP }
                        ))
                        if (false && featureAllowed("frota_viagens")) add(HubFeatureCubeData(
                            icon = Icons.Default.Route,
                            iconColor = Color(0xFFEA580C),
                            iconBg = if (isDark) Color(0xFF431407) else Color(0xFFFDEEDB),
                            title = tr("Viagens corporativas", "Corporate trips"),
                            subtitle = tr("Distancia, odometro e ocorrencias", "Distance, odometer and incidents"),
                            onClick = { selectedCorporateModule = CorporateFleetModule.TRIPS }
                        ))
                        if (false && featureAllowed("frota_manutencoes")) add(HubFeatureCubeData(
                            icon = Icons.Default.Build,
                            iconColor = Color(0xFF7C3AED),
                            iconBg = if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF),
                            title = tr("Manutencoes", "Maintenance"),
                            subtitle = tr("Alertas, bloqueios e responsaveis", "Alerts, blocks and owners"),
                            onClick = { selectedCorporateModule = CorporateFleetModule.MAINTENANCE }
                        ))
                        if (false && featureAllowed("frota_documentos")) add(HubFeatureCubeData(
                            icon = Icons.Default.Inventory2,
                            iconColor = Color(0xFF0F766E),
                            iconBg = if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1),
                            title = tr("Documentos", "Documents"),
                            subtitle = tr("CRLV, seguro, notas e comprovantes", "Registration, insurance and invoices"),
                            onClick = { selectedCorporateModule = CorporateFleetModule.DOCUMENTS }
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
