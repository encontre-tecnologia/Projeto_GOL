package br.com.gui.carlembrete

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

data class AvisoItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tipo: TipoManutencao? = null,
    val iconOverride: ImageVector? = null,
    val textIcon: String? = null,
    val wide: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun TipoAvisoScreen(
    itensAviso: List<AvisoItem>,
    title: String = tr("O que vamos lembrar?", "What should we remember?"),
    subtitle: String? = null,
    backgroundBrush: Brush,
    surfaceDark: Color,
    textLight: Color,
    textDim: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showSugestaoDialog by rememberSaveable { mutableStateOf(false) }
    var sugestaoTexto by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    val scheme = MaterialTheme.colorScheme
    val titleIconTint = scheme.primary
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.ArrowBackIosNew,
                                contentDescription = tr("Voltar", "Back"),
                                tint = textDim
                            )
                        }
                        IconButton(
                            onClick = { showSugestaoDialog = true },
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .size(42.dp)
                                .background(Color(0xFFFFC107).copy(alpha = 0.16f), CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFFD54F).copy(alpha = 0.65f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TipsAndUpdates,
                                contentDescription = tr("Tive uma ideia", "I have an idea"),
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(titleIconTint.copy(alpha = 0.14f), CircleShape)
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EventNote,
                                contentDescription = null,
                                tint = titleIconTint,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                        Text(
                            title,
                            color = textLight,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                color = textDim,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .widthIn(max = 620.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val wideItems = itensAviso.filter { it.wide }
                        val gridItems = itensAviso.filter { !it.wide }
                        wideItems.forEach { item ->
                            OutlinedButton(
                                onClick = item.onClick,
                                border = BorderStroke(1.dp, if (surfaceDark.luminance() < 0.5f) Color(0xFF334155) else Color.Black.copy(alpha = 0.18f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (surfaceDark.luminance() < 0.5f) Color(0xFF0F172A).copy(alpha = 0.55f) else surfaceDark.copy(alpha = 0.92f),
                                    contentColor = textLight
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (item.textIcon != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(item.color.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                item.textIcon,
                                                color = item.color,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(item.color.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.iconOverride ?: item.icon,
                                                contentDescription = null,
                                                tint = item.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        item.label,
                                        color = textLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        gridItems.chunked(2).forEach { linha ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                linha.forEach { item ->
                                    OutlinedButton(
                                        onClick = item.onClick,
                                        border = BorderStroke(1.dp, if (surfaceDark.luminance() < 0.5f) Color(0xFF334155) else Color.Black.copy(alpha = 0.12f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (surfaceDark.luminance() < 0.5f) Color(0xFF0F172A).copy(alpha = 0.45f) else surfaceDark.copy(alpha = 0.90f),
                                            contentColor = textLight
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (item.iconOverride != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(item.color.copy(alpha = 0.14f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.iconOverride,
                                                        contentDescription = null,
                                                        tint = item.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else if (item.tipo != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(item.color.copy(alpha = 0.14f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    TipoIcon(
                                                        tipo = item.tipo,
                                                        tint = item.color,
                                                        size = 18.dp,
                                                        textSize = 12.sp
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(item.color.copy(alpha = 0.14f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = null,
                                                        tint = item.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                item.label,
                                                color = textLight,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                lineHeight = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (linha.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    if (showSugestaoDialog) {
        val sugestaoLimpa = sugestaoTexto.text.trim()
        val canSend = sugestaoLimpa.length >= 10
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val dialogContainer = if (isDark) Color(0xFF0B1220) else Color(0xFFF8FAFC)
        val dialogField = if (isDark) Color(0xFF111827) else Color.White
        val dialogBorder = if (isDark) Color(0xFF334155) else Color(0xFFD1D5DB)

        Dialog(
            onDismissRequest = { showSugestaoDialog = false },
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
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFFD54F).copy(alpha = 0.7f),
                                    shape = CircleShape
                                ),
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
                            onClick = { showSugestaoDialog = false },
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
                                val sent = enviarSugestaoPorEmail(context, sugestaoLimpa)
                                if (sent) {
                                    showSugestaoDialog = false
                                    sugestaoTexto = TextFieldValue("")
                                }
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
}

private fun enviarSugestaoPorEmail(context: Context, sugestao: String): Boolean {
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
        Toast.makeText(context, "Sucesso! Abrindo e-mail para envio.", Toast.LENGTH_SHORT).show()
        true
    }.getOrElse {
        Toast.makeText(context, "Não foi possível abrir o e-mail.", Toast.LENGTH_SHORT).show()
        false
    }
}

