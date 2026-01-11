package br.com.gui.carlembrete

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import java.net.URLEncoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.gui.carlembrete.R
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/* ----------------- TELA PRINCIPAL (UI Melhorada) ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManutencaoScreen(modifier: Modifier = Modifier, context: Context = LocalContext.current) {
    var listaCarros by remember { mutableStateOf<List<CarroInfo>>(emptyList()) }
    var listaContatos by remember { mutableStateOf<List<ContatoProfissional>>(emptyList()) }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val carros = BancoDeDados.carregarCarrosComFallback(context)
            val contatos = BancoDeDados.carregarContatos(context)
            val lembretes = BancoDeDados.carregarLembretes(context)
            withContext(Dispatchers.Main) {
                listaCarros = carros
                listaContatos = contatos
                todosLembretes = lembretes
                isLoading = false
                NotificacaoHelper.reagendarExistentes(context.applicationContext, lembretes)
            }
        }
    }

    LaunchedEffect(listaCarros) { if(!isLoading && listaCarros.isNotEmpty()) withContext(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaCarros) } }
    LaunchedEffect(listaContatos) { if(!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarContatos(context, listaContatos) } }
    LaunchedEffect(todosLembretes) { if(!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarLembretes(context, todosLembretes) } }

    var indiceCarroAtual by remember { mutableIntStateOf(0) }
    val carroAtual = if (listaCarros.isNotEmpty()) {
        if (indiceCarroAtual >= listaCarros.size) indiceCarroAtual = 0
        listaCarros[indiceCarroAtual]
    } else { CarroInfo() }

    var showEditCarDialog by remember { mutableStateOf(false) }
    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddLembreteDialog by remember { mutableStateOf(false) }
    var iniciarCameraProduto by remember { mutableStateOf(false) }
    var showAddContatoDialog by remember { mutableStateOf(false) }
    var showRelatorioDialog by remember { mutableStateOf(false) }
    var showTesteNotificacaoDialog by remember { mutableStateOf(false) }
    var showConfiguracoes by remember { mutableStateOf(false) }
    var showPrivacidadeDialog by remember { mutableStateOf(false) }
    var showGaragemScreen by remember { mutableStateOf(false) }
    var lembreteSelecionado by remember { mutableStateOf<Lembrete?>(null) }
    var contatoDetalheSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    val lembretesDoCarroAtual = todosLembretes.filter { it.carroId == carroAtual.id }
    val formatterData = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.statusBarColor = android.graphics.Color.BLACK
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        }
    }

    if (showEditCarDialog) { EditarCarroDialog(carroAtual = carroAtual, titulo = "Editar Veículo", onDismiss = { showEditCarDialog = false }, onSalvar = { carroEditado -> listaCarros = listaCarros.map { if (it.id == carroAtual.id) carroEditado else it }; showEditCarDialog = false }) }
    if (showAddCarDialog) { EditarCarroDialog(carroAtual = CarroInfo(nome = "", modelo = ""), titulo = "Novo Carro", onDismiss = { showAddCarDialog = false }, onSalvar = { novoCarro -> listaCarros = listaCarros + novoCarro; indiceCarroAtual = listaCarros.lastIndex; showAddCarDialog = false }) }
    if (showAddContatoDialog) { NovoContatoDialog(onDismiss = { showAddContatoDialog = false }, onSalvar = { novo -> listaContatos = listaContatos + novo; showAddContatoDialog = false }) }
    if (showRelatorioDialog) {
        RelatorioVeiculoScreen(carroAtual = carroAtual, lembretes = lembretesDoCarroAtual, onDismiss = { showRelatorioDialog = false })
    }
    if (showTesteNotificacaoDialog) {
        NotificacaoRapidaDialog(
            onDismiss = { showTesteNotificacaoDialog = false },
            onDisparar = {
                NotificacaoHelper.dispararNotificacaoInstantanea(
                    context.applicationContext,
                    "Aviso imediato",
                    "Esse é um disparo rápido de teste."
                )
                Toast.makeText(context, "Notificação enviada!", Toast.LENGTH_SHORT).show()
            }
        )
    }
    if (showConfiguracoes) {
        ConfiguracoesScreen(
            onDismiss = { showConfiguracoes = false },
            onTestarNotificacao = {
                showTesteNotificacaoDialog = true
                showConfiguracoes = false
            },
            carros = listaCarros,
            lembretes = todosLembretes,
            contatos = listaContatos
        )
    }
    if (showPrivacidadeDialog) {
        PrivacidadeTermosDialog(onDismiss = { showPrivacidadeDialog = false })
    }
    BackHandler(enabled = showGaragemScreen) { showGaragemScreen = false }
    if (showGaragemScreen) {
        GaragemOverviewScreen(
            carros = listaCarros,
            onSelecionar = { selecionado ->
                val novoIndice = listaCarros.indexOfFirst { it.id == selecionado.id }
                if (novoIndice >= 0) {
                    indiceCarroAtual = novoIndice
                }
                showGaragemScreen = false
            },
            onDismiss = { showGaragemScreen = false }
        )
        return
    }
    lembreteSelecionado?.let { selecionado ->
        LembreteDetalhesDialog(
            lembrete = selecionado,
            contato = contatoDetalheSelecionado,
            carro = carroAtual,
            onDismiss = {
                lembreteSelecionado = null
                contatoDetalheSelecionado = null
            },
            onSalvar = { atualizado ->
                todosLembretes = todosLembretes.map { if (it.id == atualizado.id) atualizado else it }
                NotificacaoHelper.cancelarNotificacao(context.applicationContext, atualizado.id)
                NotificacaoHelper.agendarNotificacao(context.applicationContext, atualizado, atualizado.horaAviso)
                lembreteSelecionado = atualizado
                contatoDetalheSelecionado = listaContatos.find { it.id == atualizado.contatoId }
                Toast.makeText(context, "Aviso atualizado!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddLembreteDialog) {
        NovoAgendamentoDialog(
            carroAtual = carroAtual,
            contatosDisponiveis = listaContatos,
            onDismiss = { showAddLembreteDialog = false; iniciarCameraProduto = false },
            onConfirm = { novo ->
                todosLembretes = todosLembretes + novo.copy(carroId = carroAtual.id)
                showAddLembreteDialog = false
                iniciarCameraProduto = false
            },
            onMultiConfirm = { novosItens ->
                val novosLembretes = novosItens.map { it.copy(carroId = carroAtual.id) }
                todosLembretes = todosLembretes + novosLembretes
                showAddLembreteDialog = false
                iniciarCameraProduto = false
                Toast.makeText(context, "${novosLembretes.size} itens salvos!", Toast.LENGTH_SHORT).show()
            },
            onUpdateKmCarro = { novoKm -> listaCarros = listaCarros.map { if (it.id == carroAtual.id) it.copy(kmAtual = novoKm) else it } },
            autoAbrirCamera = iniciarCameraProduto,
            onAutoCameraConsumida = { iniciarCameraProduto = false },
            onAddContato = { novo ->
                listaContatos = listaContatos + novo
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = Color(0xFF0B0F1A),
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val nomeVeiculo = carroAtual.nome.ifBlank { "Seu veículo" }
                    val modeloTexto = carroAtual.modelo.ifBlank { "Modelo indefinido" }
                    val marcaTexto = carroAtual.marca.ifBlank { "Marca não informada" }
                    val logoRes = carroAtual.logoResOrNull()
                    val fotoGoogle = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(0.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(0.dp))
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                            if (!fotoGoogle.isNullOrBlank()) {
                                AsyncImage(
                                    model = fotoGoogle,
                                    contentDescription = "Foto do Google",
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (logoRes != null) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = marcaTexto,
                                    modifier = Modifier.size(40.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                    )
                                } else {
                                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(nomeVeiculo, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(marcaTexto, color = Color(0xFF93A6D1), fontSize = 13.sp)
                                    if (logoRes != null) {
                                        Image(
                                            painter = painterResource(id = logoRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            colorFilter = ColorFilter.tint(Color(0xFF93A6D1))
                                        )
                                    } else {
                                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color(0xFF93A6D1), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            IconButton(
                                onClick = { drawerScope.launch { drawerState.close() } },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1F2937), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar menu", tint = Color.White)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showConfiguracoes = true
                                drawerScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827), contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1F2937)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    "Configurações",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                showGaragemScreen = true
                                drawerScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827), contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1F2937)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color.White)
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    "Minha garagem",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                showAddCarDialog = true
                                drawerScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827), contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1F2937)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color.White)
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    "Adicionar veículo",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                showPrivacidadeDialog = true
                                drawerScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827), contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1F2937)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    "Privacidade e termos",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            GoogleSignIn.getClient(
                                context,
                                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            ).signOut()
                            drawerScope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Logout", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 4.dp)
                        .height(70.dp),
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(top = 18.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text("CarLembrete", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { drawerScope.launch { drawerState.open() } },
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFE2E8F0))
                                    )
                                }
                            }
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color(0xFFE2E8F0),
                        navigationIconContentColor = Color(0xFFE2E8F0),
                        actionIconContentColor = Color(0xFFE2E8F0)
                    )
                )
            },
            floatingActionButton = {}
        ) { innerPadding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }
            } else {
                Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(16.dp))
                // Card Principal do Carro
                val textoCardPrimario = Color(0xFF0F172A)
                val textoCardSecundario = Color(0xFF475569)
                val logoMarcaAtual = carroAtual.logoResOrNull()
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF7F6F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { if (indiceCarroAtual > 0) indiceCarroAtual-- else indiceCarroAtual = listaCarros.lastIndex },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .background(Color(0xFFE2E8F0), CircleShape)
                            ) { Icon(Icons.Default.ArrowBackIosNew, "Anterior", tint = textoCardPrimario.copy(alpha = 0.8f)) }
                            Column(
                                modifier = Modifier.align(Alignment.TopCenter),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    carroAtual.nome,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = textoCardPrimario,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                logoMarcaAtual?.let { logo ->
                                    Spacer(Modifier.height(6.dp))
                                    Image(
                                        painter = painterResource(id = logo),
                                        contentDescription = carroAtual.marca.ifBlank { "Marca" },
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Image(
                                    painter = painterResource(id = carroAtual.tipoIconRes()),
                                    contentDescription = carroAtual.tipoVeiculo.label,
                                    modifier = Modifier.size(180.dp)
                                )
                            }
                            IconButton(
                                onClick = { if (indiceCarroAtual < listaCarros.lastIndex) indiceCarroAtual++ else indiceCarroAtual = 0 },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .background(Color(0xFFE2E8F0), CircleShape)
                            ) { Icon(Icons.Default.ArrowForwardIos, "Próximo", tint = textoCardPrimario.copy(alpha = 0.8f)) }
                        }
                        Spacer(Modifier.height(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showEditCarDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(2.dp, textoCardPrimario.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textoCardPrimario)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar veículo", tint = textoCardPrimario, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Editar veículo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textoCardPrimario)
                            }
                            OutlinedButton(
                                onClick = { showRelatorioDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(2.dp, textoCardPrimario.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textoCardPrimario)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Relatório", tint = textoCardPrimario, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Relatório do veículo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textoCardPrimario)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        iniciarCameraProduto = false
                        showAddLembreteDialog = true
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(18.dp))
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D4ED8),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Novo aviso",
                        modifier = Modifier.size(26.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Criar novo aviso agora",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(24.dp))
                // Painel de Status (agora abaixo do botão)
                val contagemPorTipo = TipoManutencao.values().associateWith { tipo -> lembretesDoCarroAtual.count { it.tipo == tipo } }
                val statusBorderColor = Color.White.copy(alpha = 0.25f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF7F6F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MonitorIcon(TipoManutencao.OLEO, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.OLEO), contagemPorTipo[TipoManutencao.OLEO] ?: 0)
                        MonitorIcon(TipoManutencao.MECANICA, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.MECANICA), contagemPorTipo[TipoManutencao.MECANICA] ?: 0)
                        MonitorIcon(TipoManutencao.BATERIA, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.BATERIA), contagemPorTipo[TipoManutencao.BATERIA] ?: 0)
                        MonitorIcon(TipoManutencao.FREIO, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.FREIO), contagemPorTipo[TipoManutencao.FREIO] ?: 0)
                        MonitorIcon(TipoManutencao.TEMPERATURA, calcularCorStatus(lembretesDoCarroAtual, TipoManutencao.TEMPERATURA), contagemPorTipo[TipoManutencao.TEMPERATURA] ?: 0)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(24.dp))

                    val lembretesOrdenados = lembretesDoCarroAtual.sortedBy { dataParaOrdenacao(it) }

                    if (lembretesDoCarroAtual.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventNote, null, tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Nenhum registro", color = Color(0xFF64748B))
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val descricaoModeloCompleto = listOf(carroAtual.marca, carroAtual.modelo)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                                .ifBlank { carroAtual.modelo }
                            lembretesOrdenados.forEach { lembrete ->
                                val statusColor = calcularCorStatus(lembretesDoCarroAtual, lembrete.tipo)
                                val statusLabel = textoStatusPrazo(lembrete)
                                LembreteCard(
                                    lembrete = lembrete,
                                    contato = listaContatos.find { it.id == lembrete.contatoId },
                                    modeloCarro = descricaoModeloCompleto,
                                    onDelete = {
                                        NotificacaoHelper.cancelarNotificacao(context.applicationContext, lembrete.id)
                                        todosLembretes = todosLembretes.filter { it.id != lembrete.id }
                                    },
                                    onClick = {
                                        lembreteSelecionado = lembrete
                                        contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                                    },
                                    statusLabel = statusLabel,
                                    statusColor = statusColor
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp))
            }
        }
    }
}
}
