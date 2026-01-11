package br.com.gui.carlembrete

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun criarBackup() {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            Toast.makeText(context, "Faca login para criar backup", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Backup enviado com sucesso", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { err ->
                Log.e("Backup", "Falha ao enviar backup", err)
                Toast.makeText(context, "Falha ao enviar backup: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun recuperarBackup() {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            Toast.makeText(context, "Faca login para recuperar backup", Toast.LENGTH_SHORT).show()
            return
        }
        val ref = FirebaseFirestore.getInstance()
            .collection("backups")
            .document(usuario.uid)
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(context, "Nenhum backup encontrado", Toast.LENGTH_SHORT).show()
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
                    ref.delete()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup recuperado com sucesso", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Falha ao ler backup", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener { err ->
            Log.e("Backup", "Falha ao obter backup", err)
            Toast.makeText(context, "Falha ao obter backup: ${err.message}", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Configuracoes", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Personalize o jeito de cuidar da frota", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.15f))

                Text("Backup", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Envie os dados locais para o Firebase e recupere em outro dispositivo.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Button(
                    onClick = ::criarBackup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar backup", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = ::recuperarBackup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Obter backup", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTestarNotificacao,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar notificacao", fontWeight = FontWeight.Bold)
                }

                Text(
                    "Versao do app 1.0.0 | dados salvos localmente",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
