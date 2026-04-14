package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var modoRecuperacao by remember { mutableStateOf(false) }
    var modoCriarConta by remember { mutableStateOf(false) }
    var tentouEntrar by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authStatusMessage by remember { mutableStateOf<String?>(null) }
    val uiScope = rememberCoroutineScope()

    if (modoRecuperacao) {
        AuthForgotPasswordScreen(
            email = email,
            onEmailChange = { email = it },
            onSendLink = {
                if (email.isBlank()) {
                    Toast.makeText(context, "Informe o email para redefinir", Toast.LENGTH_SHORT).show()
                } else {
                    auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Email de redefinição enviado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Falha ao enviar email", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onBack = { modoRecuperacao = false }
        )
        return
    }
    if (modoCriarConta) {
        AuthCreateAccountScreen(
            onSignedIn = onSignedIn,
            onBack = { modoCriarConta = false }
        )
        return
    }

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
        isAuthLoading = false
        if (result.resultCode != Activity.RESULT_OK) {
            val semInternetAgora = !isInternetAvailable(context)
            val msg = if (semInternetAgora) {
                "Sem internet. Conecte-se e tente novamente."
            } else {
                "Login com Google cancelado"
            }
            authStatusMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
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
                isAuthLoading = false
                if (signInTask.isSuccessful) {
                    authStatusMessage = null
                    onSignedIn()
                    uiScope.launch {
                        delay(500)
                        AdminUsersSync.syncCurrentUser()
                    }
                } else {
                    val msg = if (!isInternetAvailable(context)) {
                        "Sem internet. Conecte-se e tente novamente."
                    } else {
                        "Falha no login com Google"
                    }
                    authStatusMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            isAuthLoading = false
            val msg = if (!isInternetAvailable(context)) {
                "Sem internet. Conecte-se e tente novamente."
            } else {
                "Falha no login com Google"
            }
            authStatusMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun entrarEmailSenha() {
        if (isAuthLoading) return
        if (email.isBlank() || senha.isBlank()) {
            Toast.makeText(context, "Informe email e senha", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isInternetAvailable(context)) {
            val msg = "Sem internet. Conecte-se para entrar."
            authStatusMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }
        authStatusMessage = null
        isAuthLoading = true
        auth.signInWithEmailAndPassword(email, senha).addOnCompleteListener { task ->
            isAuthLoading = false
            if (task.isSuccessful) {
                authStatusMessage = null
                onSignedIn()
                uiScope.launch {
                    delay(500)
                    AdminUsersSync.syncCurrentUser()
                }
            } else {
                val msg = if (!isInternetAvailable(context)) {
                    "Sem internet. Conecte-se e tente novamente."
                } else {
                    "Falha ao entrar"
                }
                authStatusMessage = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                text = "Entrar",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFF8FAFC)
            )
            Text(
                text = "Use email e senha ou continue com Google",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodySmall
            )
            val emailValido = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
            val emailFormatoInvalido = email.isNotBlank() && !emailValido
            val emailErro = (tentouEntrar && email.isBlank()) || emailFormatoInvalido
            val senhaErro = tentouEntrar && senha.isBlank()
            val podeEntrar = emailValido && senha.isNotBlank() && !isAuthLoading
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
            TextButton(
                onClick = { modoRecuperacao = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Esqueci a senha", color = Color(0xFF94A3B8))
            }
            Button(
                onClick = {
                    tentouEntrar = true
                    entrarEmailSenha()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (podeEntrar) Color(0xFF3B82F6) else Color(0xFF334155),
                    contentColor = if (podeEntrar) Color.White else Color(0xFF94A3B8)
                ),
                enabled = podeEntrar
            ) {
                if (isAuthLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Text(
                            "Entrando...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Text(
                        "Entrar",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            OutlinedButton(
                onClick = { modoCriarConta = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(
                    "Cadastre-se",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(color = Color(0xFFE2E8F0), modifier = Modifier.weight(1f))
                Text("ou", color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 12.dp))
                Divider(color = Color(0xFFE2E8F0), modifier = Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = {
                    if (isAuthLoading) return@OutlinedButton
                    if (!isInternetAvailable(context)) {
                        val msg = "Sem internet. Conecte-se para entrar com Google."
                        authStatusMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    authStatusMessage = null
                    isAuthLoading = true
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF93C5FD).copy(alpha = 0.55f)),
                enabled = !isAuthLoading
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_g),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Entrar com Google")
            }
            authStatusMessage?.let { status ->
                Text(
                    text = status,
                    color = Color(0xFFFCA5A5),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

private fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

