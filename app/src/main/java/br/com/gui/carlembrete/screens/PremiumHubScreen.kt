package br.com.gui.carlembrete

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumHubScreen(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onOpenGuardian: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenSubscribe: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF3A2500)
    val textDim = if (isDark) Color(0xFFCBD5E1) else Color(0xFF7A5A1F)
    val accentGold = Color(0xFFD4A017)
    val borderColor = if (isDark) Color(0xFF8B6B1F) else Color(0xFFE9C46A)
    val cardSurface = if (isDark) Color(0xFF111827) else colorScheme.surface
    val screenBg = if (isDark) colorScheme.background else colorScheme.background
    val featureCardSurface = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFEF8)
    val isEnglish = isEnglishUi()
    val supportPhone = "5516994392545"
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
        ?: FirebaseAuth.getInstance().currentUser?.email
        ?: "cliente"

    DisposableEffect(view) {
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

    Scaffold(
        containerColor = screenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cardSurface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = accentGold.copy(alpha = if (isDark) 0.18f else 0.14f),
                                    shape = RoundedCornerShape(999.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = accentGold,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(tr("Zellu Premium", "Zellu Premium"), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                        Text(tr("Veja seu novo recurso", "See your new feature"), color = textDim, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.size(14.dp))

                    PremiumFeatureCard(
                        title = tr("Viagens", "Trips"),
                        subtitle = tr("Acessar modulo de viagens e despesas", "Open trips and expenses module"),
                        iconColor = Color(0xFFEA580C),
                        textPrimary = textPrimary,
                        textDim = textDim,
                        borderColor = borderColor,
                        cardSurface = featureCardSurface,
                        onClick = onOpenAiAssistant
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFEA580C))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        abrirWhatsApp(
                            context = context,
                            telefone = supportPhone,
                            mensagem = if (isEnglish) {
                                "Hi, I'm $userName and I need help with Zellu Premium."
                            } else {
                                "Olá, sou $userName e preciso de ajuda com o Zellu Premium."
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF16A34A) else Color(0xFF22C55E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(tr("Suporte WhatsApp", "WhatsApp support"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    title: String,
    subtitle: String,
    iconColor: Color,
    textPrimary: Color,
    textDim: Color,
    borderColor: Color,
    cardSurface: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = textDim, fontSize = 13.sp)
            }
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = textDim, modifier = Modifier.size(16.dp))
        }
    }
}

