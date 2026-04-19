package br.com.gui.carlembrete

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val AccentBlue = Color(0xFF3B82F6)

@Composable
fun PerfilScreen(
    onDismiss: () -> Unit,
    planTier: PlanTier,
    totalVeiculos: Int
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF020917) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val logoutTint = if (isDark) Color(0xFFFC8181) else Color(0xFFDC2626)

    val user = FirebaseAuth.getInstance().currentUser
    val nome = user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuário"
    val email = user?.email?.takeIf { it.isNotBlank() } ?: "Email não informado"
    val foto = user?.photoUrl?.toString()
    val ultimoLoginTexto = formatarData(user?.metadata?.lastSignInTimestamp ?: 0L)
    val isPremium = planTier != PlanTier.FREE

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                apagarContaLocalRemota(context)
                onDismiss()
            }
        )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Voltar",
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Meu Perfil",
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onDismiss()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sair",
                        tint = logoutTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- AVATAR + NOME ---
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentBlue, Color(0xFF6366F1))),
                        CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(cardBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!foto.isNullOrBlank()) {
                        AsyncImage(
                            model = foto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = nome,
                color = titleColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Plan badge
            val badgeColor = if (isPremium) Color(0xFFFBBF24) else AccentBlue
            val badgeBg = if (isPremium) {
                if (isDark) Color(0xFF451A03) else Color(0xFFFFF4D8)
            } else {
                if (isDark) Color(0xFF1E3A5F) else Color(0xFFEFF6FF)
            }
            val badgeLabel = if (isPremium) "Premium" else "Free"
            val badgeIcon = if (isPremium) Icons.Rounded.Star else Icons.Rounded.VerifiedUser

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                Text(badgeLabel, color = badgeColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))

            // --- STATS ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Veículos",
                    value = totalVeiculos.toString(),
                    color = AccentBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Plano",
                    value = badgeLabel,
                    color = badgeColor
                )
            }

            Spacer(Modifier.height(20.dp))

            // --- INFO CARD ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                InfoRowDark(
                    icon = Icons.Rounded.Email,
                    iconColor = AccentBlue,
                    iconBg = if (isDark) Color(0xFF1E3A5F) else Color(0xFFEFF6FF),
                    label = "Email",
                    value = email
                )
                HorizontalDivider(thickness = 1.dp, color = cardBorder.copy(alpha = 0.5f))
                InfoRowDark(
                    icon = Icons.Rounded.History,
                    iconColor = Color(0xFF34D399),
                    iconBg = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                    label = "Último acesso",
                    value = ultimoLoginTexto
                )
                HorizontalDivider(thickness = 1.dp, color = cardBorder.copy(alpha = 0.5f))
                InfoRowDark(
                    icon = Icons.Rounded.Person,
                    iconColor = Color(0xFFA78BFA),
                    iconBg = if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF),
                    label = "Conta",
                    value = "Google"
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- LOGOUT BUTTON ---
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE2E8F0))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Sair da conta",
                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // --- DELETE BUTTON ---
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFC8181))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Excluir conta e dados", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)
    val dimColor = if (isDark) Color(0xFF64748B) else Color(0xFF475569)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(label, color = dimColor, fontSize = 12.sp)
    }
}

@Composable
private fun InfoRowDark(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    label: String,
    value: String
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val dimColor = if (isDark) Color(0xFF64748B) else Color(0xFF475569)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = dimColor, fontSize = 11.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF0D1B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFD6E0EF)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7F1D1D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFC8181), modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                tr("Apagar permanentemente?", "Delete permanently?"),
                color = titleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                tr(
                    "Esta ação não pode ser desfeita. Todos os seus dados serão perdidos.",
                    "This action cannot be undone. All your data will be lost."
                ),
                color = subColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = subColor)
                ) {
                    Text(tr("Cancelar", "Cancel"), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                ) {
                    Text(tr("Apagar", "Delete"), color = Color(0xFFFC8181), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatarData(millis: Long): String {
    if (millis <= 0L) return "Não disponível"
    return try {
        val data = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(data)
    } catch (e: Exception) { "Formato inválido" }
}

private fun formatarTempoDesdeLogin(lastSignInMillis: Long): String {
    if (lastSignInMillis <= 0L) return "N/A"
    val agora = Instant.now()
    val ultimo = Instant.ofEpochMilli(lastSignInMillis)
    val dias = ChronoUnit.DAYS.between(ultimo, agora)
    if (dias > 0) return "Ativo há $dias ${if (dias == 1L) "dia" else "dias"}"
    val horas = ChronoUnit.HOURS.between(ultimo, agora)
    if (horas > 0) return "Ativo há $horas ${if (horas == 1L) "hora" else "horas"}"
    val minutos = ChronoUnit.MINUTES.between(ultimo, agora)
    return if (minutos > 0) "Ativo há $minutos min" else "Ativo agora"
}

private fun apagarContaLocalRemota(context: Context) {
    BancoDeDados.salvarCarros(context, emptyList())
    BancoDeDados.salvarLembretes(context, emptyList())
    BancoDeDados.salvarContatos(context, emptyList())
    BancoDeDados.salvarAbastecimentos(context, emptyList())
    BancoDeDados.salvarPedaladas(context, emptyList())

    context.getSharedPreferences("app_prefs_v3", Context.MODE_PRIVATE).edit().clear().apply()
    context.getSharedPreferences("home_tutorial_prefs", Context.MODE_PRIVATE).edit().clear().apply()

    val auth = FirebaseAuth.getInstance()
    auth.currentUser?.delete()?.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Toast.makeText(context, "Erro na exclusão remota.", Toast.LENGTH_LONG).show()
        }
    }
    auth.signOut()
    GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut()
    Toast.makeText(context, "Dados removidos com sucesso.", Toast.LENGTH_LONG).show()
}

