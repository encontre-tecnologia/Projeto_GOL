package br.com.gui.carlembrete

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@Composable
fun SuggestionIdeaEntryPoint(
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val iconBg = Color(0xFFFFC107).copy(alpha = 0.16f)
    val iconBorder = Color(0xFFFFD54F).copy(alpha = 0.65f)
    val iconTint = Color(0xFFFFD54F)

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier
            .size(38.dp)
            .background(iconBg, CircleShape)
            .border(1.dp, iconBorder, CircleShape)
    ) {
        Icon(
            imageVector = Icons.Rounded.TipsAndUpdates,
            contentDescription = tr("Tive uma ideia", "I have an idea"),
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDialog) {
        SuggestionIdeaDialog(
            onDismiss = { showDialog = false },
            onSuccess = { showDialog = false }
        )
    }
}

@Composable
private fun SuggestionIdeaDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var sugestaoTexto by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val sugestaoLimpa = sugestaoTexto.text.trim()
    val canSend = sugestaoLimpa.length >= 10
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textLight = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val dialogContainer = if (isDark) Color(0xFF0B1220) else Color(0xFFF8FAFC)
    val dialogField = if (isDark) Color(0xFF111827) else Color.White
    val dialogBorder = if (isDark) Color(0xFF334155) else Color(0xFFD1D5DB)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = dialogContainer,
            border = BorderStroke(1.dp, dialogBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFFFFC107).copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TipsAndUpdates,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = tr("Tive uma ideia", "I have an idea"),
                        fontWeight = FontWeight.Bold,
                        color = textLight,
                        fontSize = 21.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tr(
                            "Tem algo que pode ficar melhor no Zellu? Escreve aqui em poucos segundos. Sua ideia vai direto pro nosso time.",
                            "Got an idea to improve Zellu? Share it in a few seconds. Your suggestion goes straight to our team."
                        ),
                        color = textDim,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = sugestaoTexto,
                    onValueChange = { novo ->
                        if (novo.text.length <= 500) sugestaoTexto = novo
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 7,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = {
                        Text(
                            tr(
                                "Ex.: queria filtro por data nos lembretes e opção de fixar favoritos...",
                                "Ex: I'd like a reminder date filter and pinned favorites..."
                            )
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${sugestaoLimpa.length}/500",
                            color = if (sugestaoLimpa.length > 450) Color(0xFFF59E0B) else textDim
                        )
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = dialogField,
                        unfocusedContainerColor = dialogField
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .widthIn(min = 144.dp)
                            .height(46.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White else Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tr("Cancelar", "Cancel"))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            if (!canSend) return@Button
                            val sent = enviarSugestaoPorEmailGlobal(context, sugestaoLimpa)
                            if (sent) onSuccess()
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .widthIn(min = 144.dp)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF94A3B8)
                        )
                    ) {
                        Text(tr("Enviar ideia", "Send idea"))
                    }
                }
            }
        }
    }
}

private fun enviarSugestaoPorEmailGlobal(context: Context, sugestao: String): Boolean {
    val prefs = context.getSharedPreferences("suggestions_local_guard", Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val lastSentAt = prefs.getLong("last_sent_at", 0L)
    val cooldownMs = 2 * 60 * 1000L
    val remainingMs = (lastSentAt + cooldownMs) - now
    if (remainingMs > 0) {
        val remainingSeconds = (remainingMs / 1000L).coerceAtLeast(1L)
        Toast.makeText(
            context,
            "Aguarde ${remainingSeconds}s para enviar outra sugestão.",
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    val emailDestino = "guilhermedevsistemas@gmail.com"
    val subject = "Sugestão no app Zellu"
    val body = buildString {
        appendLine("Sugestão enviada pelo app:")
        appendLine()
        appendLine(sugestao)
        appendLine()
        appendLine("Idioma: ${Locale.getDefault()}")
        appendLine("Versão: ${BuildConfig.VERSION_NAME}")
    }

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(emailDestino))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return runCatching {
        context.startActivity(intent)
        prefs.edit().putLong("last_sent_at", now).apply()
        Toast.makeText(
            context,
            trNow("Valeu! Sua ideia foi preparada no e-mail.", "Thanks! Your idea is ready in email."),
            Toast.LENGTH_SHORT
        ).show()
        true
    }.getOrElse {
        Toast.makeText(
            context,
            trNow("Nenhum app de e-mail encontrado.", "No email app found."),
            Toast.LENGTH_SHORT
        ).show()
        false
    }
}
