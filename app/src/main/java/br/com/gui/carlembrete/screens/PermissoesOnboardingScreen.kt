package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val TAG_PERMISSOES = "PermissoesScreen"

private data class PermissaoItem(
    val permission: String,
    val title: String,
    val reason: String
)

private fun iconParaPermissao(permission: String): ImageVector = when (permission) {
    Manifest.permission.CAMERA -> Icons.Default.CameraAlt
    Manifest.permission.ACCESS_FINE_LOCATION -> Icons.Default.LocationOn
    Manifest.permission.POST_NOTIFICATIONS -> Icons.Default.Notifications
    else -> Icons.Default.Security
}

private fun permissaoRequerRuntime(permission: String): Boolean = when (permission) {
    Manifest.permission.POST_NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    else -> true
}

private tailrec fun Context.findActivityPerm(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityPerm()
    else -> null
}

private fun abrirConfigPermissao(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    )
}

private fun abrirConfigNotificacao(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    )
}

private fun permissaoConcedida(context: Context, permission: String): Boolean = when (permission) {
    Manifest.permission.POST_NOTIFICATIONS -> {
        val notifOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED && notifOk
        } else notifOk
    }
    else -> {
        if (!permissaoRequerRuntime(permission)) true
        else ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissoesOnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current

    val permissoes = remember {
        listOf(
            PermissaoItem(Manifest.permission.CAMERA, "Câmera",
                "Escanear QR Codes e anexar fotos nos registros."),
            PermissaoItem(Manifest.permission.ACCESS_FINE_LOCATION, "Localização",
                "Salvar posição do veículo na função Aonde Parei."),
            PermissaoItem(Manifest.permission.POST_NOTIFICATIONS, "Notificações",
                "Enviar alertas de manutenção e lembretes importantes.")
        )
    }

    var statusPermissoes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val solicitadaUmaVez = remember { mutableStateMapOf<String, Boolean>() }

    fun atualizar() {
        statusPermissoes = permissoes.associate { it.permission to permissaoConcedida(context, it.permission) }
        Log.d(TAG_PERMISSOES, statusPermissoes.entries.joinToString(" | ") { "${it.key}=${it.value}" })
    }

    val todasConcedidas by remember(statusPermissoes, permissoes) {
        derivedStateOf { permissoes.isNotEmpty() && permissoes.all { statusPermissoes[it.permission] == true } }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        atualizar()
    }

    LaunchedEffect(Unit) { atualizar() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) atualizar()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Header — sem barra azul, fundo herdado da tela
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A5F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Permissões necessárias",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Conceda os acessos para usar todos os recursos do Zellu.",
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Permission cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                permissoes.forEach { item ->
                    val granted = statusPermissoes[item.permission] == true
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
                                Icon(iconParaPermissao(item.permission), null, tint = iconTint, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (granted) Color(0xFF166534) else Color(0xFF7F1D1D))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            if (granted) "Permitido" else "Pendente",
                                            color = if (granted) Color(0xFF4ADE80) else Color(0xFFFCA5A5),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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
                                    Log.d(TAG_PERMISSOES, "Permitir click -> ${item.permission}")
                                    if (item.permission == Manifest.permission.POST_NOTIFICATIONS) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            solicitadaUmaVez[Manifest.permission.POST_NOTIFICATIONS] = true
                                            launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                        } else {
                                            abrirConfigNotificacao(context)
                                        }
                                    } else if (permissaoRequerRuntime(item.permission)) {
                                        val activity = context.findActivityPerm()
                                        val jaSolicitou = solicitadaUmaVez[item.permission] == true
                                        val mostraRationale = activity?.let {
                                            ActivityCompat.shouldShowRequestPermissionRationale(it, item.permission)
                                        } ?: false
                                        if (jaSolicitou && !mostraRationale) {
                                            abrirConfigPermissao(context)
                                        } else {
                                            solicitadaUmaVez[item.permission] = true
                                            launcher.launch(arrayOf(item.permission))
                                        }
                                    } else {
                                        atualizar()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Permitir acesso", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // Botão sempre visível fora da área de scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onContinue,
                enabled = todasConcedidas,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF475569)
                )
            ) {
                Text(
                    if (todasConcedidas) "Continuar" else "Conceda os acessos acima",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
