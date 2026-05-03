package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.gui.carlembrete.VehicleIcon
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.gui.carlembrete.R
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin

/* ----------------- ONBOARDING ----------------- */

private data class PermissionUiItem(
    val permission: String,
    val title: String,
    val reason: String
)

private const val TAG_ONBOARDING_PERMISSIONS = "OnboardingPerms"

private fun permissionIconFor(permission: String): ImageVector = when (permission) {
    Manifest.permission.CAMERA -> Icons.Default.CameraAlt
    Manifest.permission.ACCESS_FINE_LOCATION -> Icons.Default.LocationOn
    Manifest.permission.POST_NOTIFICATIONS -> Icons.Default.Notifications
    else -> Icons.Default.Security
}

private fun isRuntimePermissionRequired(permission: String): Boolean = when (permission) {
    Manifest.permission.POST_NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    else -> true
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openAppPermissionSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun isPermissionGrantedNow(context: Context, permission: String): Boolean {
    return when (permission) {
        Manifest.permission.POST_NOTIFICATIONS -> {
            val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val runtimeGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                runtimeGranted && notificationsEnabled
            } else {
                notificationsEnabled
            }
        }
        else -> {
            if (!isRuntimePermissionRequired(permission)) true
            else ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val onboardingBg = if (isDark) Color.Black else Color(0xFF0F2A4A)
    val onboardingCardBg = if (isDark) Color(0xFF111827) else Color(0xFF1E293B)
    val scope = rememberCoroutineScope()
    var carroNome by remember { mutableStateOf("") }
    var carroMarca by remember { mutableStateOf("") }
    var carroModeloUnico by remember { mutableStateOf("") }
    var carroKm by remember { mutableStateOf("20.000") }
    var carroTipo by remember { mutableStateOf(TipoVeiculo.CARRO) }
    var frotaTemporaria by remember { mutableStateOf(listOf<CarroInfo>()) }
    var showOutroVeiculoDialog by remember { mutableStateOf(false) }
    var onboardingVehicleFormSession by remember { mutableIntStateOf(0) }
    var selectedThemeMode by remember { mutableStateOf(AppThemeMode.DARK) }
    var aceitouTermos by remember { mutableStateOf(false) }
    var aceitouPrivacidade by remember { mutableStateOf(false) }
    val previousStep = when (step) {
        7 -> 5
        6 -> 4
        5 -> 1
        9 -> 5
        4 -> 7
        2 -> 1
        else -> null
    }
    BackHandler(enabled = previousStep != null) {
        step = previousStep ?: step
    }
    val maxVehicles = vehicleLimitForPlan(PlanTier.FREE)
    val termosUsoTexto = remember {
        """
        1. Aceite: ao usar o Zellu, você concorda com estes Termos e com a Política de Privacidade.

        2. Objeto: o app oferece gestão de veículos, lembretes, manutenções, viagens, frota e estoque.

        3. Uso adequado: você se compromete a usar o app de forma lícita, sem fraude, abuso técnico ou violação de direitos de terceiros.

        4. Conta e segurança: você é responsável pelos dados da conta e pela guarda do acesso.

        5. Planos e cobrança: planos pagos (como Lite/Frota) seguem regras da loja/plataforma de pagamento para renovação, cancelamento e reembolso.

        6. Limitação: o Zellu é ferramenta de apoio e não substitui diagnóstico técnico, vistoria, seguro, assistência mecânica ou orientação profissional.

        7. Disponibilidade: funcionalidades podem ser alteradas, corrigidas, suspensas ou descontinuadas por evolução do produto, segurança ou obrigação legal.

        8. Propriedade intelectual: marca, software, layout e conteúdo do app são protegidos por lei.

        9. Legislação e foro: aplica-se a legislação brasileira, com foro da comarca de Sao Carlos/SP, salvo competência legal específica.

        10. Contato legal e suporte: guilhermedevsistemas@gmail.com
        """.trimIndent()
    }
    val politicaPrivacidadeTexto = remember {
        """
        1. Dados tratados: o app pode tratar dados de conta (nome, e-mail e identificadores), cadastro de veículos, lembretes, contatos, viagens, itens de estoque, localização, câmera, notificações e dados técnicos essenciais.

        2. Finalidades: autenticação, execução das funcionalidades, segurança, prevenção de abuso/fraude, suporte e melhoria contínua.

        3. Bases legais (LGPD): execução de contrato, consentimento quando exigido, legítimo interesse para segurança/estabilidade e cumprimento de obrigação legal.

        4. Permissões: câmera, localização e notificações são usadas somente com autorização e podem ser revogadas a qualquer momento no dispositivo.

        5. Compartilhamento: não vendemos dados pessoais. Podemos compartilhar com operadores/provedores técnicos necessários ao funcionamento do app e com autoridades quando houver obrigação legal.

        6. Retenção e armazenamento: parte dos dados pode ficar no dispositivo e parte em nuvem, pelo tempo necessário às finalidades e obrigações legais.

        7. Direitos do titular: você pode solicitar confirmação de tratamento, acesso, correção, anonimização, exclusão e revogação do consentimento, nos termos da LGPD.

        8. Exclusão de conta e dados: ao solicitar exclusão, removemos dados pessoais e registros vinculados, ressalvadas retenções legais obrigatórias.

        9. Transferência internacional: alguns provedores podem processar dados fora do Brasil, com salvaguardas adequadas.

        10. Contato oficial de privacidade, remoção de dados, dúvidas e suporte:
        - guilhermedevsistemas@gmail.com
        Páginas oficiais:
        - https://account-deletion-site-eight.vercel.app/privacy-policy.html
        - https://account-deletion-site-eight.vercel.app/terms-of-use.html
        """.trimIndent()
    }
    val permissionItems = remember {
        buildList {
            add(
                PermissionUiItem(
                    permission = Manifest.permission.CAMERA,
                    title = "Câmera",
                    reason = "Escanear QR Codes e anexar fotos nos registros."
                )
            )
            add(
                PermissionUiItem(
                    permission = Manifest.permission.ACCESS_FINE_LOCATION,
                    title = "Localização",
                    reason = "Salvar posição do veículo na função Aonde Parei."
                )
            )
            add(
                PermissionUiItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Notificações",
                    reason = "Enviar alertas de manutenção e lembretes importantes."
                )
            )
        }
    }
    var permissionStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val requestedPermissionOnce = remember { mutableStateMapOf<String, Boolean>() }
    fun refreshPermissionStatus() {
        permissionStatus = permissionItems.associate { item ->
            val granted = isPermissionGrantedNow(context, item.permission)
            item.permission to granted
        }
        val snapshot = permissionStatus.entries.joinToString(" | ") { (permission, granted) ->
            "$permission=${if (granted) "granted" else "pending"}"
        }
        Log.d(TAG_ONBOARDING_PERMISSIONS, "refreshPermissionStatus -> $snapshot")
    }
    val allRequiredPermissionsGranted by remember(permissionStatus, permissionItems) {
        derivedStateOf {
            permissionItems.isNotEmpty() && permissionItems.all { item ->
                permissionStatus[item.permission] == true
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        Log.d(
            TAG_ONBOARDING_PERMISSIONS,
            "permissionLauncher result -> ${it.entries.joinToString(" | ") { e -> "${e.key}=${e.value}" }}"
        )
        refreshPermissionStatus()
    }
    LaunchedEffect(Unit) {
        refreshPermissionStatus()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showOutroVeiculoDialog) {
        val primaryColor = Color(0xFF3B82F6)
        val successColor = Color(0xFF10B981)
        val dialogBg = Color.Black
        val dialogBorder = Color.White.copy(alpha = 0.14f)
        val titleColor = Color(0xFFE5E7EB)
        val secondaryColor = Color(0xFF94A3B8)
        val outlineBtnBorder = Color(0xFFCBD5E1)
        val outlineBtnText = Color(0xFFE2E8F0)
        val primaryBtnColor = Color(0xFF2563EB)
        Dialog(onDismissRequest = { showOutroVeiculoDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = dialogBg),
                    border = BorderStroke(1.dp, dialogBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(successColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = successColor, modifier = Modifier.size(40.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Veículo 1 cadastrado!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Deseja cadastrar outro veículo agora ou seguir para a próxima etapa?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showOutroVeiculoDialog = false
                                    onboardingVehicleFormSession += 1
                                    step = 4
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, outlineBtnBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = outlineBtnText
                                )
                            ) {
                                Text(
                                    "Cadastrar outro",
                                    color = outlineBtnText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Button(
                                onClick = {
                                    showOutroVeiculoDialog = false
                                    step = 6
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryBtnColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    "Próximo",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (step == 4) {
        key(onboardingVehicleFormSession) {
            OnboardingNovoCarroScreen(
                onDismiss = { step = 7 },
                onboardingVehicleNumber = (frotaTemporaria.size + 1).coerceAtMost(maxVehicles),
                onSalvar = { novoCarro ->
                    if (frotaTemporaria.size >= maxVehicles) {
                                    Toast.makeText(context, "Limite do plano Gratis: $maxVehicles veiculos.", Toast.LENGTH_SHORT).show()
                    } else {
                        val atualizada = (frotaTemporaria + novoCarro).take(maxVehicles)
                        frotaTemporaria = atualizada
                        scope.launch(Dispatchers.IO) { BancoDeDados.salvarCarros(context, atualizada) }
                        AdminUsersSync.syncVehicles(atualizada)
                        if (atualizada.size == 1) {
                            showOutroVeiculoDialog = true
                        } else {
                            step = 6
                        }
                    }
                }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBg)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(targetState = step, transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }, label = "onboarding") { currentStep ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (currentStep in listOf(5, 6, 9)) 0.dp else 24.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        var showOrbit by remember { mutableStateOf(false) }
                        var showTitle by remember { mutableStateOf(false) }
                        var showSubtitle by remember { mutableStateOf(false) }
                        var showButton by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            showOrbit = true
                            delay(120)
                            showTitle = true
                            delay(100)
                            showSubtitle = true
                            delay(100)
                            showButton = true
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 20.dp, bottom = 108.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    visible = showOrbit,
                                    enter = fadeIn(animationSpec = tween(480)) +
                                        scaleIn(
                                            animationSpec = tween(480),
                                            initialScale = 0.92f
                                        ) +
                                        slideInVertically(
                                        animationSpec = tween(480),
                                        initialOffsetY = { it / 6 }
                                    )
                                ) { OnboardingWelcomeOrbit() }

                                Spacer(Modifier.height(32.dp))

                                AnimatedVisibility(
                                    visible = showTitle,
                                    enter = fadeIn(animationSpec = tween(420)) +
                                        slideInVertically(
                                        animationSpec = tween(420),
                                        initialOffsetY = { it / 8 }
                                    )
                                ) {
                                    Text(
                                        "Bem-vindo ao Zellu",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                AnimatedVisibility(
                                    visible = showSubtitle,
                                    enter = fadeIn(animationSpec = tween(400)) +
                                        slideInVertically(
                                        animationSpec = tween(400),
                                        initialOffsetY = { it / 10 }
                                    )
                                ) {
                                    Text(
                                        "Organize sua garagem, cuide dos seus veículos e receba avisos no momento certo.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFBFDBFE),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showButton,
                                    enter = fadeIn(animationSpec = tween(380)) +
                                        slideInVertically(
                                        animationSpec = tween(380),
                                        initialOffsetY = { it / 12 }
                                    )
                                ) {
                                    Button(
                                        onClick = { step = 5 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                    ) { Text("Vamos lá!", fontSize = 19.sp, color = Color.White) }
                                }
                            }
                        }
                    }
                    5 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 92.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Permissões necessárias",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Conceda os acessos para usar todos os recursos do Zellu.",
                                            color = Color.White.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    }
                                }
                                items(permissionItems) { item ->
                                        val granted = permissionStatus[item.permission] == true
                                        val iconTint = when (item.permission) {
                                            Manifest.permission.CAMERA -> Color(0xFF60A5FA)
                                            Manifest.permission.ACCESS_FINE_LOCATION -> Color(0xFF34D399)
                                            Manifest.permission.POST_NOTIFICATIONS -> Color(0xFFFBBF24)
                                            else -> Color(0xFF94A3B8)
                                        }
                                        val iconBg = when (item.permission) {
                                            Manifest.permission.CAMERA -> Color(0xFF1E3A5F)
                                            Manifest.permission.ACCESS_FINE_LOCATION -> Color(0xFF064E3B)
                                            Manifest.permission.POST_NOTIFICATIONS -> Color(0xFF78350F)
                                            else -> Color(0xFF1E293B)
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF1E293B))
                                                .border(
                                                    1.dp,
                                                    if (granted) Color(0xFF22C55E) else Color(0xFF334155),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(iconBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = permissionIconFor(item.permission),
                                                        contentDescription = null,
                                                        tint = iconTint,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        if (granted) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(20.dp))
                                                                    .background(Color(0xFF166534))
                                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            ) {
                                                                Text("Permitido", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(20.dp))
                                                                    .background(Color(0xFF7F1D1D))
                                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            ) {
                                                                Text("Pendente", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(item.reason, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                                }
                                            }
                                            if (!granted) {
                                                Spacer(Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        Log.d(TAG_ONBOARDING_PERMISSIONS, "click Permitir -> permission='${item.permission}'")
                                                        if (item.permission == Manifest.permission.POST_NOTIFICATIONS) {
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                                            ) {
                                                                requestedPermissionOnce[Manifest.permission.POST_NOTIFICATIONS] = true
                                                                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                                            } else {
                                                                openAppNotificationSettings(context)
                                                            }
                                                        } else if (isRuntimePermissionRequired(item.permission)) {
                                                            val activity = context.findActivity()
                                                            val wasRequested = requestedPermissionOnce[item.permission] == true
                                                            val shouldShowRationale = activity?.let {
                                                                ActivityCompat.shouldShowRequestPermissionRationale(it, item.permission)
                                                            } ?: false
                                                            if (wasRequested && !shouldShowRationale) {
                                                                openAppPermissionSettings(context)
                                                            } else {
                                                                requestedPermissionOnce[item.permission] = true
                                                                permissionLauncher.launch(arrayOf(item.permission))
                                                            }
                                                        } else {
                                                            refreshPermissionStatus()
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF2563EB),
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Permitir acesso", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Button(
                                    onClick = { step = 7 },
                                    enabled = allRequiredPermissionsGranted,
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF1E293B),
                                        disabledContentColor = Color(0xFF475569)
                                    )
                                ) {
                                    Text(
                                        if (allRequiredPermissionsGranted) "Continuar" else "Conceda os acessos acima",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    9 -> {
                        val notifGranted = permissionStatus[Manifest.permission.POST_NOTIFICATIONS] == true
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Notificações",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ative para receber lembretes, avisos de manutenção e alertas importantes do Zellu.",
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(28.dp))
                                // Status card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(
                                            1.dp,
                                            if (notifGranted) Color(0xFF22C55E) else Color(0xFF334155),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Notificações do app", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    if (notifGranted) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF166534))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Permitido", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF7F1D1D))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Pendente", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (!notifGranted) {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            Log.d(TAG_ONBOARDING_PERMISSIONS, "click Permitir na tela dedicada de notificações")
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                requestedPermissionOnce[Manifest.permission.POST_NOTIFICATIONS] = true
                                                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                            } else {
                                                openAppNotificationSettings(context)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2563EB),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Permitir notificações", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            // Bottom
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                Button(
                                    onClick = { step = 5 },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (notifGranted) Color(0xFF2563EB) else Color(0xFF1E293B),
                                        contentColor = if (notifGranted) Color.White else Color(0xFF475569)
                                    )
                                ) {
                                    Text(
                                        if (notifGranted) "Voltar para permissões" else "Voltar",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    7 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .navigationBarsPadding()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(bottom = 92.dp)
                            ) {
                                item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF93C5FD),
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                                }
                                item {
                                Text(
                                    "Termos e Privacidade",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                }
                                item {
                                Text(
                                    "Para continuar, aceite os Termos de Uso e a Política de Privacidade do Zellu.",
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                }
                                item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = onboardingCardBg),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "Termos de Uso",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            termosUsoTexto,
                                            color = Color(0xFFBFDBFE),
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp
                                        )
                                    }
                                }
                                }
                                item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = onboardingCardBg),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "Política de Privacidade",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            politicaPrivacidadeTexto,
                                            color = Color(0xFFBFDBFE),
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp
                                        )
                                    }
                                }
                                }
                                item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = onboardingCardBg),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (aceitouTermos && aceitouPrivacidade) Color(0xFF22C55E) else Color(0xFF334155)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Confirmações obrigatórias",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = aceitouTermos && aceitouPrivacidade,
                                                onCheckedChange = {
                                                    aceitouTermos = it
                                                    aceitouPrivacidade = it
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF22C55E),
                                                    uncheckedColor = Color(0xFF94A3B8),
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Text(
                                                "Concordo com tudo.",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = aceitouTermos,
                                                onCheckedChange = { aceitouTermos = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF22C55E),
                                                    uncheckedColor = Color(0xFF94A3B8),
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Text(
                                                "Li e aceito os Termos de Uso.",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = aceitouPrivacidade,
                                                onCheckedChange = { aceitouPrivacidade = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF22C55E),
                                                    uncheckedColor = Color(0xFF94A3B8),
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Text(
                                                "Li e aceito a Política de Privacidade.",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .padding(vertical = 12.dp)
                            ) {
                                Button(
                                    onClick = { step = 4 },
                                    enabled = aceitouTermos && aceitouPrivacidade,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (aceitouTermos && aceitouPrivacidade) Color(0xFF2563EB) else Color(0xFF475569),
                                        contentColor = Color.White
                                    )
                                ) { Text("Próximo", fontSize = 19.sp) }
                            }
                        }
                    }
                    4 -> Unit
                    2 -> {
                        if (frotaTemporaria.isNotEmpty()) {
                            Text("VeÃ­culos Adicionados:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(frotaTemporaria) { c ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        VehicleIcon(
                                            tipoVeiculo = c.tipoVeiculo,
                                            tint = Color.White,
                                            size = 40.dp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = c.nome,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.height(170.dp).width(160.dp)) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(160.dp).align(Alignment.BottomCenter))
                            Box(modifier = Modifier.size(width = 38.dp, height = 58.dp).background(Color(0xFF4B5563), shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).align(Alignment.BottomCenter).padding(bottom = 2.dp))
                            VehicleIcon(
                                tipoVeiculo = carroTipo,
                                tint = Color(0xFFCBD5E1),
                                size = 100.dp,
                                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp)); Text("Sua Garagem", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = carroNome, onValueChange = { carroNome = it }, label = { Text("Apelido (ex: Fox do Gui)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        var marcaExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = marcaExpanded, onExpandedChange = { marcaExpanded = !marcaExpanded }) {
                            OutlinedTextField(
                                value = carroMarca,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Marca") },
                                placeholder = { Text("Selecione a marca") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = marcaExpanded) }
                            )
                            ExposedDropdownMenu(expanded = marcaExpanded, onDismissRequest = { marcaExpanded = false }) {
                                marcasPorTipo(carroTipo).forEach { marcaNome ->
                                    DropdownMenuItem(
                                        text = { Text(marcaNome) },
                                        onClick = {
                                            carroMarca = marcaNome
                                            marcaExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val modeloLabel = if (carroTipo == TipoVeiculo.BICICLETA) "Aro" else "Modelo e Motor"
                        OutlinedTextField(
                            value = carroModeloUnico,
                            onValueChange = { carroModeloUnico = it },
                            label = { Text(modeloLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = carroKm,
                            onValueChange = {
                                carroKm = it.filter(Char::isDigit).take(10)
                            },
                            label = { Text("KM Atual") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        TipoVeiculoSelector(
                            selecionado = carroTipo,
                            onSelect = { carroTipo = it }
                        )
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                if (frotaTemporaria.size >= maxVehicles) {
                                    Toast.makeText(context, "Limite do plano Gratis: $maxVehicles veiculos.", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                if (carroNome.isNotBlank() && carroModeloUnico.isNotBlank()) {
                                    val novo = CarroInfo(
                                        nome = carroNome,
                                        modelo = carroModeloUnico,
                                        marca = carroMarca,
                                        kmAtual = carroKm.filter(Char::isDigit).toIntOrNull() ?: 0,
                                        tipoVeiculo = carroTipo
                                    )
                                    frotaTemporaria = frotaTemporaria + novo
                                    carroNome = ""
                                    carroMarca = ""
                                    carroModeloUnico = ""
                                    carroKm = "20.000"
                                    carroTipo = TipoVeiculo.CARRO
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6))
                        ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar Outro VeÃ­culo") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                var listaFinal = frotaTemporaria
                                if (carroNome.isNotBlank() || carroModeloUnico.isNotBlank() || carroMarca.isNotBlank()) {
                                    val ultimo = CarroInfo(
                                        nome = if(carroNome.isBlank()) carroTipo.label else carroNome,
                                        modelo = carroModeloUnico,
                                        marca = carroMarca,
                                        kmAtual = carroKm.filter(Char::isDigit).toIntOrNull() ?: 0,
                                        tipoVeiculo = carroTipo
                                    )
                                    listaFinal = listaFinal + ultimo
                                }
                                if (listaFinal.size > maxVehicles) {
                                    Toast.makeText(context, "Limite do plano Gratis: $maxVehicles veiculos.", Toast.LENGTH_SHORT).show()
                                    listaFinal = listaFinal.take(maxVehicles)
                                }
                                if (listaFinal.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um veículo para continuar.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val listaSalvar = listaFinal
                                scope.launch(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaSalvar) }
                                AdminUsersSync.syncVehicles(listaSalvar)
                                step = 6
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) { Text("Salvar e Continuar", fontSize = 18.sp) }
                    }
                    6 -> {
                        OnboardingThanksScreen(
                            onGoToHome = onFinish
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeOrbit() {
    val transition = rememberInfiniteTransition(label = "welcome_orbit")
    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )
    val iconCounterRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "icon_counter_rotation"
    )
    val orbitIcons = listOf(
        Icons.Rounded.WaterDrop,
        Icons.Rounded.TireRepair,
        Icons.Rounded.Settings,
        Icons.Rounded.Description,
        Icons.Rounded.BatteryChargingFull,
        Icons.Rounded.Build,
        Icons.Rounded.FormatPaint,
        Icons.Rounded.Payments,
        Icons.Rounded.Shield,
        Icons.Rounded.Edit,
        Icons.Rounded.LocalGasStation
    )

    val density = LocalDensity.current
    val orbitRadiusPx = with(density) { 136.dp.toPx() }
    val centerPulse by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_pulse"
    )

    Box(
        modifier = Modifier.size(350.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(156.dp)
                .shadow(14.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color(0xFF0A1424))
                .border(2.dp, Color(0xFF2C4E73), CircleShape)
                .graphicsLayer {
                    scaleX = centerPulse
                    scaleY = centerPulse
                },
            contentAlignment = Alignment.Center
        ) {
            val logoMatrix = ColorMatrix().apply {
                setToScale(1.18f, 1.18f, 1.18f, 1f)
            }
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Logo do app",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .graphicsLayer {
                        scaleX = 1.55f
                        scaleY = 1.55f
                    },
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(logoMatrix)
            )
        }

        orbitIcons.forEachIndexed { index, icon ->
            val startAngle = index * (360f / orbitIcons.size)
            val angle = startAngle + orbitRotation
            val radians = Math.toRadians(angle.toDouble())
            val x = (cos(radians) * orbitRadiusPx).toFloat()
            val y = (sin(radians) * orbitRadiusPx).toFloat()

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        translationX = x
                        translationY = y
                        rotationZ = iconCounterRotation
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A5F))
                    .border(1.dp, Color(0xFF365E89), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFBFDBFE),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


