package br.com.gui.carlembrete

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelecionarPrestadorScreen(
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (ContatoProfissional) -> Unit
) {
    val context = LocalContext.current
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val iconColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val accentBlue = Color(0xFF2563EB)
    val profissionaisListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val loadingTransition = rememberInfiniteTransition(label = "prestadorLoading")
    val loadingAlpha by loadingTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prestadorLoadingAlpha"
    )

    var carregandoProfissionaisCidade by remember { mutableStateOf(false) }
    var erroProfissionaisCidade by remember { mutableStateOf<String?>(null) }
    var profissionaisDaCidade by remember { mutableStateOf<List<ProfissionalCidadeEncontrado>>(emptyList()) }
    var contatoSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    var cidadeAtual by remember { mutableStateOf<String?>(null) }
    var ufAtual by remember { mutableStateOf<String?>(null) }

    fun carregarProfissionais(forcar: Boolean = false) {
        if (carregandoProfissionaisCidade) return
        scope.launch {
            carregandoProfissionaisCidade = true
            erroProfissionaisCidade = null
            val resultado = withContext(Dispatchers.IO) {
                buscarProfissionaisDaCidadeAtual(
                    context = context,
                    tipoSelecionado = tipoSelecionado,
                    isBikeVehicle = isBikeVehicle
                )
            }
            carregandoProfissionaisCidade = false
            resultado.onSuccess { busca ->
                cidadeAtual = busca.cidade
                ufAtual = busca.estado
                profissionaisDaCidade = busca.profissionais
                if (forcar && busca.profissionais.isEmpty()) {
                    erroProfissionaisCidade = "Nenhum profissional encontrado na sua cidade."
                }
            }.onFailure { erro ->
                profissionaisDaCidade = emptyList()
                erroProfissionaisCidade = erro.message ?: "Nao foi possivel buscar profissionais da cidade."
            }
        }
    }

    LaunchedEffect(tipoSelecionado, isBikeVehicle) {
        carregarProfissionais()
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = iconColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBackground)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { contatoSelecionado?.let(onConfirmar) },
                    enabled = contatoSelecionado != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vincular prestador", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            EtapaProfissionaisContent(
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconColor = iconColor,
                accentBlue = accentBlue,
                cidadeAtual = cidadeAtual,
                ufAtual = ufAtual,
                carregandoProfissionaisCidade = carregandoProfissionaisCidade,
                erroProfissionaisCidade = erroProfissionaisCidade,
                profissionaisDaCidade = profissionaisDaCidade,
                profissionaisListState = profissionaisListState,
                loadingAlpha = loadingAlpha,
                contatoSelecionado = contatoSelecionado,
                onRecarregar = { carregarProfissionais(forcar = true) },
                onVerNoGoogle = { nome -> abrirBuscaGoogleProfissional(context, nome) },
                onAdicionarDaCidade = { profissional ->
                    contatoSelecionado = profissional.toContato(tipoSelecionado)
                }
            )
        }
    }
}

private fun abrirBuscaGoogleProfissional(context: android.content.Context, nome: String) {
    val query = Uri.encode("$nome telefone")
    val uri = Uri.parse("https://www.google.com/search?q=$query")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun ProfissionalCidadeEncontrado.toContato(tipoSelecionado: TipoManutencao): ContatoProfissional {
    return ContatoProfissional(
        nome = nome,
        telefone = telefone,
        tipoServico = tipoSelecionado.label
    )
}
