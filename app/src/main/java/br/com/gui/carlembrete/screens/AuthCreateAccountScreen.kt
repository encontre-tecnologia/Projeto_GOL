package br.com.gui.carlembrete

import android.widget.Toast
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun AuthCreateAccountScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var tentouCriar by remember { mutableStateOf(false) }
    val emailValido = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val emailFormatoInvalido = email.isNotBlank() && !emailValido
    val emailErro = (tentouCriar && email.isBlank()) || emailFormatoInvalido
    val senhaValida = senha.length >= 6
    val senhaFormatoInvalido = senha.isNotBlank() && !senhaValida
    val senhaErro = (tentouCriar && senha.isBlank()) || senhaFormatoInvalido
    val podeCriar = emailValido && senhaValida
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Token do Google não gerado. Verifique SHA-1.", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(token, null)
            auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    onSignedIn()
                } else {
                    Toast.makeText(context, "Falha no cadastro com Google", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Falha no cadastro com Google", Toast.LENGTH_SHORT).show()
        }
    }

    fun criarContaEmailSenha() {
        if (email.isBlank() || senha.isBlank()) {
            Toast.makeText(context, "Informe email e senha", Toast.LENGTH_SHORT).show()
            return
        }
        auth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                    if (emailTask.isSuccessful) {
                        Toast.makeText(context, "Conta criada! Enviamos um e-mail de confirmação.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Conta criada, mas não foi possível enviar o e-mail.", Toast.LENGTH_LONG).show()
                    }
                }
                onSignedIn()
            } else {
                Toast.makeText(context, "Falha ao criar conta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF081428),
                        Color(0xFF0B2342),
                        Color(0xFF143A6C)
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
            Box(
                modifier = Modifier
                    .height(96.dp)
                    .width(96.dp)
                    .border(1.dp, Color(0xFF334155), CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Logo do app",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = "Criar conta",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFF8FAFC)
            )
            Text(
                text = "Preencha email e senha para criar sua conta",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                isError = emailErro,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    focusedLabelColor = Color(0xFF94A3B8),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedBorderColor = if (emailErro) Color(0xFFEF4444) else Color(0xFF334155),
                    unfocusedBorderColor = if (emailErro) Color(0xFFEF4444) else Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
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
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                isError = senhaErro,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            imageVector = if (senhaVisivel) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    focusedLabelColor = Color(0xFF94A3B8),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedBorderColor = if (senhaErro) Color(0xFFEF4444) else Color(0xFF334155),
                    unfocusedBorderColor = if (senhaErro) Color(0xFFEF4444) else Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    cursorColor = Color(0xFFF8FAFC)
                )
            )
            if (senhaFormatoInvalido) {
                Text(
                    text = "Senha inválida: mínimo de 6 caracteres",
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            } else {
                Text(
                    text = "A senha deve ter no mínimo 6 caracteres",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Button(
                onClick = {
                    tentouCriar = true
                    criarContaEmailSenha()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (podeCriar) Color(0xFF3B82F6) else Color(0xFF334155),
                    contentColor = if (podeCriar) Color.White else Color(0xFF94A3B8)
                )
            ) {
                Text(
                    "Criar conta",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 1.dp)
                )
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
                Text("Voltar")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(color = Color(0xFF334155), modifier = Modifier.weight(1f))
                Text("ou", color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 12.dp))
                Divider(color = Color(0xFF334155), modifier = Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD).copy(alpha = 0.55f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_g),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Cadastre-se com o Google")
            }
        }
    }
}
