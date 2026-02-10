package br.com.gui.carlembrete

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ⚠️ IMPORTANTE: Coloque sua API Key aqui ou no local.properties
private const val GEMINI_API_KEY = "AIzaSyAszHLd9qv9_p0GfIkvwwWrOAYjUaTvEbg"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerPecasScreen(
    onDismiss: () -> Unit
) {
    // Cores do seu tema
    val bg = Color(0xFFF8FAFC)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)
    val accent = Color(0xFF0EA5E9)
    val errorColor = Color(0xFFEF4444)

    // Estados da Tela
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var analysisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Launcher para pegar foto da galeria
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri; analysisResult = ""; errorMessage = null }
    )

    // Instância do Gemini
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-pro",
            apiKey = GEMINI_API_KEY
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scanner Zellu IA", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de Preview da Imagem
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (imageUri != null) accent else Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Foto da peça",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Toque para adicionar foto", color = textSecondary)
                            Text("(Pneu, motor, freio...)", color = textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Botão de Ação
            Button(
                onClick = {
                    if (imageUri != null) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val bitmap = withContext(Dispatchers.IO) {
                                    val inputStream = context.contentResolver.openInputStream(imageUri!!)
                                    BitmapFactory.decodeStream(inputStream)
                                }

                                val prompt = """
                                    Você é um mecânico especialista do app Zellu.
                                    Analise esta imagem visualmente.
                                    Se for um PNEU: Verifique o TWI e sinais de ressecamento ou desgaste irregular.
                                    Se for PEÇA (Freio, Correia, Motor): Procure por trincas, vazamentos, ferrugem ou desgaste excessivo.
                                    Se a imagem não for clara ou não for de carro, avise o usuário.
                                    Seja direto, curto e útil. Use tópicos.
                                """.trimIndent()

                                val response = generativeModel.generateContent(
                                    content {
                                        image(bitmap)
                                        text(prompt)
                                    }
                                )
                                analysisResult = response.text ?: "Não consegui analisar."
                            } catch (e: Exception) {
                                errorMessage = "Erro na análise: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = imageUri != null && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analisar com IA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Exibição de Erro
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, errorColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = errorColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage!!, color = errorColor, fontSize = 14.sp)
                    }
                }
            }

            // Resultado da IA
            if (analysisResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Diagnóstico Zellu",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))
                        Text(
                            text = analysisResult,
                            color = textPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}