package br.com.gui.carlembrete

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onDismiss: () -> Unit,
    onTestarNotificacao: () -> Unit,
    carros: List<CarroInfo>,
    lembretes: List<Lembrete>,
    contatos: List<ContatoProfissional>
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = (LocalContext.current as? android.app.Activity)
    val subscriptionManager = remember { SubscriptionManager(context) }
    val isSubscribed by subscriptionManager.isSubscribed.collectAsState()

    DisposableEffect(Unit) {
        subscriptionManager.connect()
        onDispose { subscriptionManager.disconnect() }
    }

    fun criarBackup() {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            Toast.makeText(context, "Faça login para criar backup", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            BancoDeDados.salvarCarros(context, carros)
            BancoDeDados.salvarLembretes(context, lembretes)
            BancoDeDados.salvarContatos(context, contatos)
            val payload = BackupPayload(carros, lembretes, contatos).toMap()
            val ref = FirebaseFirestore.getInstance()
                .collection("backups")
                .document(usuario.uid)
            ref.set(payload).addOnSuccessListener {
                Toast.makeText(context, "Backup enviado com sucesso!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { err ->
                Log.e("Backup", "Falha ao enviar backup", err)
                Toast.makeText(context, "Falha ao enviar: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun recuperarBackup() {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            Toast.makeText(context, "Faça login para recuperar backup", Toast.LENGTH_SHORT).show()
            return
        }
        val ref = FirebaseFirestore.getInstance()
            .collection("backups")
            .document(usuario.uid)
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(context, "Nenhum backup encontrado na nuvem.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val data = snapshot.data ?: emptyMap()
            scope.launch(Dispatchers.IO) {
                try {
                    val payload = backupPayloadFromMap(data)
                    BancoDeDados.salvarCarros(context, payload.carros)
                    BancoDeDados.salvarLembretes(context, payload.lembretes)
                    BancoDeDados.salvarContatos(context, payload.contatos)
                    NotificacaoHelper.reagendarExistentes(context.applicationContext, payload.lembretes)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Dados restaurados com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erro ao processar backup.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener { err ->
            Log.e("Backup", "Falha ao obter backup", err)
            Toast.makeText(context, "Erro de conexão: ${err.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F2A4A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configura?Ãµes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F2A4A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {

            // --- SEÃ‡ÃƒO 1: BACKUP E DADOS ---
            SectionHeader(title = "BACKUP E SINCRONIZAÃ‡ÃƒO")

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {

                // Card de Venda (S? aparece se n?o for assinante)
                if (!isSubscribed) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Ouro
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "CarLembrete Pro",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            // Benef?cios compactos
                            BeneficioItem("Backup seguro na nuvem")
                            BeneficioItem("Nunca perca seus dados")

                            Button(
                                onClick = {
                                    if (activity != null) subscriptionManager.launchPurchaseFlow(activity)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                            ) {
                                Text("Assinar por R$ 9,90/mês", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Feedback visual se j? for assinante
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
                        Spacer(Modifier.width(8.dp))
                        Text("Sua assinatura está ativa.", color = Color(0xFF10B981), fontSize = 14.sp)
                    }
                }

                // BotÃµes de A??o (Backup e Restore)
                // O texto agora ? limpo, sem "bloqueado"
                Button(
                    onClick = {
                        if (!isSubscribed) {
                            Toast.makeText(context, "Funcionalidade exclusiva Pro!", Toast.LENGTH_SHORT).show()
                        } else {
                            criarBackup()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fazer Backup na Nuvem", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (!isSubscribed) {
                            Toast.makeText(context, "Funcionalidade exclusiva Pro!", Toast.LENGTH_SHORT).show()
                        } else {
                            recuperarBackup()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restaurar Backup", fontWeight = FontWeight.Bold)
                }
            }

            // --- SEÃ‡ÃƒO 2: NOTIFICAÃ‡Ã•ES ---
            Spacer(Modifier.height(16.dp))
            SectionHeader(title = "NOTIFICAÃ‡Ã•ES")

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    "Teste se seu aparelho está recebendo os alertas corretamente.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = onTestarNotificacao,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Testar Notificação", fontWeight = FontWeight.Bold)
                }
            }

            // --- RODAPÃ‰ ---
            Spacer(Modifier.height(24.dp))
            Text(
                "Versão 1.0.0",
                color = Color(0xFF475569),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
            )
        }
    }
}

// Componente Reutiliz?vel para o Cabe?alho da Se??o (Estilo Android Settings)
@Composable
fun SectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(0xFF64748B), // Cinza azulado tipo Android System
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        Divider(
            color = Color(0xFF334155), // Linha sutil
            thickness = 1.dp
        )
    }
}

@Composable
fun BeneficioItem(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, color = Color(0xFFCBD5E1), fontSize = 13.sp)
    }
}

