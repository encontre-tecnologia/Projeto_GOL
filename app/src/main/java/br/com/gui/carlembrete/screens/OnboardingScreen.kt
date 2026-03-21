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
import androidx.compose.ui.graphics.Brush
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
    val scope = rememberCoroutineScope()
    var carroNome by remember { mutableStateOf("") }
    var carroMarca by remember { mutableStateOf("") }
    var carroModeloUnico by remember { mutableStateOf("") }
    var carroKm by remember { mutableStateOf("") }
    var carroTipo by remember { mutableStateOf(TipoVeiculo.CARRO) }
    var frotaTemporaria by remember { mutableStateOf(listOf<CarroInfo>()) }
    var showOutroVeiculoDialog by remember { mutableStateOf(false) }
    var onboardingVehicleFormSession by remember { mutableIntStateOf(0) }
    var selectedThemeMode by remember { mutableStateOf(AppThemeMode.DARK) }
    var aceitouTermos by remember { mutableStateOf(false) }
    var aceitouPrivacidade by remember { mutableStateOf(false) }
    val previousStep = when (step) {
        8 -> 4
        7 -> 5
        6 -> 8
        5 -> 1
        9 -> 5
        4 -> 7
        2 -> 1
        else -> null
    }
    BackHandler(enabled = previousStep != null) {
        step = previousStep ?: step
    }
    val maxVehicles = 3
    val termosUsoTexto = remember {
        """
        1. Objeto: o Zellu oferece recursos de cadastro e gerenciamento de veículos, lembretes e informações relacionadas.

        2. Uso adequado: você se compromete a utilizar o app de forma lícita e a fornecer dados verdadeiros, atualizados e de sua responsabilidade.

        3. Responsabilidade do usuário: decisões de manutenção, compra, venda, deslocamento e segurança do veículo são de responsabilidade exclusiva do usuário.

        4. Limitação de responsabilidade: o Zellu é ferramenta de apoio e não substitui diagnóstico técnico, vistoria, seguro, assistência mecânica ou orientação profissional.

        5. Disponibilidade: funcionalidades podem ser alteradas, corrigidas, suspensas ou descontinuadas sem aviso prévio, quando necessário.

        6. Foro: para dirimir eventuais conflitos relacionados ao uso do app, fica eleito o foro da comarca de Sao Carlos/SP, sem endereco comercial divulgado neste momento.
        """.trimIndent()
    }
    val politicaPrivacidadeTexto = remember {
        """
        1. Dados tratados: o app pode tratar dados de cadastro de veículos, lembretes, contatos, localização, câmera e notificações, conforme recursos utilizados por você.

        2. Finalidade: os dados são usados para executar funcionalidades do app, personalizar a experiência e permitir recursos solicitados pelo usuário.

        3. LGPD (Lei 13.709/2018): o tratamento de dados observa os princípios da necessidade, finalidade, adequação e transparência, com base legal aplicável para execução do serviço e consentimento quando exigido.

        4. Permissões: câmera, localização e notificações somente são usadas após consentimento e podem ser revogadas a qualquer momento nas configurações do dispositivo.

        5. Compartilhamento: o Zellu não comercializa dados pessoais e utiliza informações apenas para operação do serviço e integrações técnicas necessárias.

        6. Direitos do titular: você pode solicitar confirmação de tratamento, acesso, correção, anonimização, exclusão e revogação do consentimento, nos termos da LGPD.

        7. Exclusão de conta e dados: ao solicitar a exclusão da conta, os dados pessoais e registros vinculados serão removidos, observadas apenas retenções legais obrigatórias.

        8. Contato de privacidade, remoção de dados, dúvidas e sugestões:
        - guilhermedevsistemas@gmail.com
        - hiasminlorrane8@gmail.com
        Os mesmos e-mails acima também são canais oficiais para dúvidas, suporte e sugestões de melhoria.
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
        val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
        Dialog(onDismissRequest = { showOutroVeiculoDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        Text("Veículo 1 cadastrado!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Deseja cadastrar outro veículo agora ou seguir para a próxima etapa?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showOutroVeiculoDialog = false
                                onboardingVehicleFormSession += 1
                                step = 4
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cadastrar", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showOutroVeiculoDialog = false
                                step = 8
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Próximo", color = Color.White)
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
                        Toast.makeText(context, "Limite de veículos do plano grátis atingido.", Toast.LENGTH_SHORT).show()
                    } else {
                        val atualizada = (frotaTemporaria + novoCarro).take(maxVehicles)
                        frotaTemporaria = atualizada
                        scope.launch(Dispatchers.IO) { BancoDeDados.salvarCarros(context, atualizada) }
                        if (atualizada.size == 1) {
                            showOutroVeiculoDialog = true
                        } else {
                            step = 8
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
            .background(Color(0xFF0F2A4A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(targetState = step, transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }, label = "onboarding") { currentStep ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (currentStep == 8 || currentStep == 6) 0.dp else 24.dp)
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

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
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

                            Spacer(Modifier.height(48.dp))

                            AnimatedVisibility(
                                visible = showButton,
                                enter = fadeIn(animationSpec = tween(380)) +
                                    slideInVertically(
                                    animationSpec = tween(380),
                                    initialOffsetY = { it / 12 }
                                )
                            ) {
                                Button(
                                    onClick = { step = 5 },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) { Text("Vamos lá!", fontSize = 19.sp, color = Color.White) }
                            }
                        }
                    }
                    8 -> {
                        OnboardingPremiumWelcomeScreen(
                            onNext = { step = 6 },
                            onSkip = { step = 6 }
                        )
                    }
                    5 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF93C5FD),
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                            item {
                                Text(
                                    "Permissões do App",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item { Spacer(Modifier.height(10.dp)) }
                            item {
                                Text(
                                    "Para o Zellu funcionar bem no dia a dia, permita os acessos necessários. Isso garante lembretes, recursos automáticos e uma experiência completa. Você pode ajustar depois nas configurações do celular.",
                                    color = Color(0xFFBFDBFE),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                            items(permissionItems) { item ->
                                val granted = permissionStatus[item.permission] == true
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(
                                        1.dp,
                                        if (granted) Color(0xFF22C55E) else Color(0xFFEF4444)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = permissionIconFor(item.permission),
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    item.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                if (granted) "Permitido" else "Pendente",
                                                color = if (granted) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            item.reason,
                                            color = Color(0xFFBFDBFE),
                                            fontSize = 12.sp
                                        )
                                        if (!granted) {
                                            Button(
                                                onClick = {
                                                    Log.d(
                                                        TAG_ONBOARDING_PERMISSIONS,
                                                        "click Permitir -> permission='${item.permission}' required=${isRuntimePermissionRequired(item.permission)} currentGranted=${permissionStatus[item.permission] == true}"
                                                    )
                                                    if (item.permission == Manifest.permission.POST_NOTIFICATIONS) {
                                                        step = 9
                                                    } else if (isRuntimePermissionRequired(item.permission)) {
                                                        val activity = context.findActivity()
                                                        val wasRequested = requestedPermissionOnce[item.permission] == true
                                                        val shouldShowRationale = activity?.let {
                                                            ActivityCompat.shouldShowRequestPermissionRationale(it, item.permission)
                                                        } ?: false
                                                        if (wasRequested && !shouldShowRationale) {
                                                            Log.d(TAG_ONBOARDING_PERMISSIONS, "open app settings for '${item.permission}'")
                                                            openAppPermissionSettings(context)
                                                        } else {
                                                            requestedPermissionOnce[item.permission] = true
                                                            Log.d(TAG_ONBOARDING_PERMISSIONS, "request runtime '${item.permission}'")
                                                            permissionLauncher.launch(arrayOf(item.permission))
                                                        }
                                                    } else {
                                                        Log.d(TAG_ONBOARDING_PERMISSIONS, "permission '${item.permission}' does not require runtime request")
                                                        refreshPermissionStatus()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF3B82F6),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(if (item.permission == Manifest.permission.POST_NOTIFICATIONS) "Configurar notificações" else "Permitir")
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                            item {
                                Button(
                                    onClick = { step = 7 },
                                    enabled = allRequiredPermissionsGranted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (allRequiredPermissionsGranted) Color(0xFF60A5FA) else Color(0xFF475569),
                                        contentColor = Color.White
                                    )
                                ) { Text("Próximo", fontSize = 19.sp) }
                            }
                        }
                    }
                    9 -> {
                        val notifGranted = permissionStatus[Manifest.permission.POST_NOTIFICATIONS] == true
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Text(
                                "Notificações",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Ative as notificações para receber lembretes, avisos de manutenção e alertas importantes do Zellu.",
                                color = Color(0xFFBFDBFE),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, if (notifGranted) Color(0xFF22C55E) else Color(0xFFEF4444)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (notifGranted) "Permitido" else "Pendente",
                                        color = if (notifGranted) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B82F6),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Permitir notificações") }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { step = 5 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (notifGranted) Color(0xFF60A5FA) else Color(0xFF475569),
                                    contentColor = Color.White
                                )
                            ) { Text(if (notifGranted) "Voltar para permissões" else "Voltar", fontSize = 18.sp) }
                        }
                    }
                    7 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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
                                item {
                                Button(
                                onClick = { step = 4 },
                                enabled = aceitouTermos && aceitouPrivacidade,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (aceitouTermos && aceitouPrivacidade) Color(0xFF60A5FA) else Color(0xFF475569),
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
                            onValueChange = { if (it.all(Char::isDigit)) carroKm = it },
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
                                    Toast.makeText(context, "Limite de veÃ­culos do plano grÃ¡tis atingido.", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                if (carroNome.isNotBlank() && carroModeloUnico.isNotBlank()) {
                                    val novo = CarroInfo(
                                        nome = carroNome,
                                        modelo = carroModeloUnico,
                                        marca = carroMarca,
                                        kmAtual = carroKm.toIntOrNull() ?: 0,
                                        tipoVeiculo = carroTipo
                                    )
                                    frotaTemporaria = frotaTemporaria + novo
                                    carroNome = ""
                                    carroMarca = ""
                                    carroModeloUnico = ""
                                    carroKm = ""
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
                                        kmAtual = carroKm.toIntOrNull() ?: 0,
                                        tipoVeiculo = carroTipo
                                    )
                                    listaFinal = listaFinal + ultimo
                                }
                                if (listaFinal.size > maxVehicles) {
                                    Toast.makeText(context, "Limite de veÃ­culos do plano grÃ¡tis atingido.", Toast.LENGTH_SHORT).show()
                                    listaFinal = listaFinal.take(maxVehicles)
                                }
                                if (listaFinal.isEmpty()) {
                                    Toast.makeText(context, "Adicione pelo menos um veículo para continuar.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val listaSalvar = listaFinal
                                scope.launch(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaSalvar) }
                                step = 8
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


