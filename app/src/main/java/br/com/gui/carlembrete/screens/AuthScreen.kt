package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authStatusMessage by remember { mutableStateOf<String?>(null) }
    val uiScope = rememberCoroutineScope()

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
                isAuthLoading = false
                val msg = "Token do Google não gerado. Verifique SHA-1/SHA-256 no Firebase."
                authStatusMessage = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
        } catch (e: ApiException) {
            isAuthLoading = false
            val msg = when {
                !isInternetAvailable(context) -> "Sem internet. Conecte-se e tente novamente."
                e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED || result.resultCode == Activity.RESULT_CANCELED ->
                    "Login com Google cancelado"
                e.statusCode == GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                    "Falha no login com Google. Tente novamente."
                e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
                    "Login já em andamento. Aguarde e tente de novo."
                e.statusCode == 10 ->
                    "Configuração Google/Firebase inválida (SHA-1/SHA-256 ou OAuth)."
                else -> "Falha no login com Google (código ${e.statusCode})"
            }
            authStatusMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            isAuthLoading = false
            val msg = if (!isInternetAvailable(context)) {
                "Sem internet. Conecte-se e tente novamente."
            } else {
                "Falha inesperada no login com Google"
            }
            authStatusMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Animated gradient blobs
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val blob1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )
    val blob2Offset by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )
    val blob3Offset by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020917))
    ) {
        // Animated blob 1 — blue, top-right
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(
                    x = (120 + blob1Offset * 60).dp,
                    y = (-60 + blob1Offset * 80).dp
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Animated blob 2 — indigo, bottom-left
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(
                    x = (-80 + blob2Offset * 60).dp,
                    y = (420 + blob2Offset * 80).dp
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Animated blob 3 — cyan, center
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(
                    x = (60 + blob3Offset * 40).dp,
                    y = (200 + blob3Offset * 120).dp
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0EA5E9).copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Branding section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.6f),
                                    Color(0xFF6366F1).copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo Zellu",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Zellu",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Cuide do seu veículo\nsem complicação.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Bottom card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF0D1B2E),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 32.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Entrar ou criar conta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Text(
                    text = "Já tem conta? Você entra. Novo por aqui? Sua conta é criada automaticamente.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (isAuthLoading) return@Button
                        if (!isInternetAvailable(context)) {
                            val msg = "Sem internet. Conecte-se para entrar com Google."
                            authStatusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        authStatusMessage = null
                        isAuthLoading = true
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A5F),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E3A5F).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    enabled = !isAuthLoading
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Entrando…", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_g),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Continuar com Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                authStatusMessage?.let { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = status,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp
                        )
                    }
                }
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
