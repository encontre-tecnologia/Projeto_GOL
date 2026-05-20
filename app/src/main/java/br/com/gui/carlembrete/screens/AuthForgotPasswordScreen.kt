package br.com.gui.carlembrete

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthForgotPasswordScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendLink: () -> Unit,
    onBack: () -> Unit
) {
    val emailValido = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val emailFormatoInvalido = email.isNotBlank() && !emailValido
    val canSendLink = emailValido

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020617),
                        Color(0xFF000000)
                    )
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF93C5FD),
                modifier = Modifier.height(38.dp)
            )
            Text(
                text = "Recuperar senha",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFF8FAFC)
            )
            Text(
                text = "Coloque seu email para redefinir sua senha",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                isError = emailFormatoInvalido,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    disabledTextColor = Color(0xFFF8FAFC),
                    focusedLabelColor = Color(0xFF94A3B8),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    disabledLabelColor = Color(0xFF94A3B8),
                    focusedBorderColor = if (emailFormatoInvalido) Color(0xFFEF4444) else Color(0xFF334155),
                    unfocusedBorderColor = if (emailFormatoInvalido) Color(0xFFEF4444) else Color(0xFF334155),
                    disabledBorderColor = if (emailFormatoInvalido) Color(0xFFEF4444) else Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    disabledContainerColor = Color(0xFF0F172A),
                    cursorColor = Color(0xFFF8FAFC)
                )
            )
            if (emailFormatoInvalido) {
                Text(
                    text = "Email inválido",
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Button(
                onClick = { if (canSendLink) onSendLink() },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSendLink) Color(0xFF3B82F6) else Color(0xFF334155),
                    contentColor = if (canSendLink) Color.White else Color(0xFF94A3B8)
                )
            ) {
                Text("Enviar link", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD).copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Voltar ao login")
            }
        }
    }
}
