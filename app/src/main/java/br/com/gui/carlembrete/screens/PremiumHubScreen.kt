package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumHubScreen(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onOpenGuardian: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenSubscribe: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1200), Color(0xFF0F172A), Color(0xFF111827))
    )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zellu Premium", color = Color(0xFFFFE7A8), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF111827))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B05)),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFBBF24))
                        Text("Central Premium", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            if (isPremium) "Seu plano está ativo. Acesse os recursos abaixo."
                            else "Desbloqueie recursos avançados do Zellu.",
                            color = Color(0xFFFDE68A),
                            fontSize = 13.sp
                        )
                    }
                }

                PremiumFeatureButton(
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White) },
                    title = "Zellu Guardião",
                    subtitle = "Proteção e alertas inteligentes",
                    onClick = onOpenGuardian
                )

                PremiumFeatureButton(
                    icon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White) },
                    title = "Gestor Financeiro",
                    subtitle = "Visão de gastos e relatórios",
                    onClick = onOpenFinance
                )

                if (!isPremium) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onOpenSubscribe,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Assinar Premium", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureButton(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { icon() }
        }
    }
}

