package br.com.gui.carlembrete

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onDismiss: () -> Unit,
    planTier: PlanTier,
    totalVeiculos: Int
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Cores baseadas no tema
    val isDark = colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) Color(0xFF0F172A) else colorScheme.background
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val border = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)

    val user = FirebaseAuth.getInstance().currentUser
    val nome = user?.displayName?.takeIf { it.isNotBlank() } ?: "Usuário"
    val email = user?.email?.takeIf { it.isNotBlank() } ?: "Email não informado"
    val foto = user?.photoUrl?.toString()

    val ultimoLoginMillis = user?.metadata?.lastSignInTimestamp ?: 0L
    val ultimoLoginTexto = formatarData(ultimoLoginMillis)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            cardBg = cardBg,
            border = border,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                apagarContaLocalRemota(context)
                onDismiss()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar")
                }
                Text("Meu Perfil", fontWeight = FontWeight.Black)
                IconButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onDismiss()
                }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = colorScheme.error)
                }
            }

            // --- HEADER: FOTO E NOME ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(4.dp, border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!foto.isNullOrBlank()) {
                        AsyncImage(
                            model = foto,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.Person, contentDescription = null,
                            modifier = Modifier.size(60.dp), tint = colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = nome,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Surface(
                    color = colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(14.dp), tint = colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Conta Ativa", style = MaterialTheme.typography.labelMedium, color = colorScheme.primary)
                    }
                }
            }

            // --- SEÇÃO: INFORMAÇÕES ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, border)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoRow(icon = Icons.Rounded.Email, label = "Email", value = email)
                    HorizontalDivider(thickness = 0.5.dp, color = border)
                    InfoRow(icon = Icons.Rounded.History, label = "Último acesso", value = ultimoLoginTexto)
                    HorizontalDivider(thickness = 0.5.dp, color = border)
                    InfoRow(
                        icon = Icons.Rounded.VerifiedUser,
                        label = "Plano",
                        value = if (planTier == PlanTier.FREE) "Free" else "Premium"
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = border)
                    InfoRow(
                        icon = Icons.Rounded.Person,
                        label = "Veículos cadastrados",
                        value = totalVeiculos.toString()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- BOTÃO DE PERIGO ---
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error)
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Excluir conta e dados", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    cardBg: Color,
    border: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(tr("Apagar permanentemente?", "Delete permanently?"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    tr("Esta ação não pode ser desfeita. Todos os seus dados serão perdidos.", "This action cannot be undone. All your data will be lost."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr("Cancelar", "Cancel"))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tr("Apagar", "Delete"), fontWeight = FontWeight.Bold)
                    }
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
    // Presumindo que os métodos salvarX existam no seu singleton BancoDeDados
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
