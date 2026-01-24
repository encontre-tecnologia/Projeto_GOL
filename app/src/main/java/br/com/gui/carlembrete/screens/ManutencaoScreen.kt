package br.com.gui.carlembrete

import android.app.Activity
import android.graphics.Paint
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// Função utilitária para encontrar a Activity
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/* ----------------- TELA PRINCIPAL (Visual Dashboard Premium) ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManutencaoScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    onLoaded: () -> Unit = {}
) {
    // ----------------- ESTADOS E VARIÁVEIS -----------------
    var listaCarros by remember { mutableStateOf<List<CarroInfo>>(emptyList()) }
    var listaContatos by remember { mutableStateOf<List<ContatoProfissional>>(emptyList()) }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var notifiedLoaded by remember { mutableStateOf(false) }

    // CORES DO TEMA (Azul Premium)
    val primaryDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
    val topBarDark = Color(0xFF15223A)
    val accentBlue = Color(0xFF3B82F6)
    val textLight = Color(0xFFF1F5F9)
    val textDim = Color(0xFF94A3B8)

    // ----------------- CARREGAMENTO DE DADOS -----------------
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
    LaunchedEffect(isLoading) {
        if (!isLoading && !notifiedLoaded) {
            notifiedLoaded = true
            onLoaded()
        }
    }

    // Persistência automática ao alterar dados
    LaunchedEffect(listaCarros) { if (!isLoading && listaCarros.isNotEmpty()) withContext(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaCarros) } }
    LaunchedEffect(listaContatos) { if (!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarContatos(context, listaContatos) } }
    LaunchedEffect(todosLembretes) { if (!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarLembretes(context, todosLembretes) } }

    var indiceCarroAtual by remember { mutableIntStateOf(0) }
    val carroAtual = if (listaCarros.isNotEmpty()) {
        if (indiceCarroAtual >= listaCarros.size) indiceCarroAtual = 0
        listaCarros[indiceCarroAtual]
    } else {
        CarroInfo()
    }

    // Estados de Controle de Interface
    var showEditCarDialog by remember { mutableStateOf(false) }
    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddLembreteDialog by remember { mutableStateOf(false) }
    var iniciarCameraProduto by remember { mutableStateOf(false) }
    var showAddContatoDialog by remember { mutableStateOf(false) }
    var showTesteNotificacaoDialog by remember { mutableStateOf(false) }
    var showConfiguracoes by remember { mutableStateOf(false) }
    var showPrivacidadeDialog by remember { mutableStateOf(false) }
    var showMecanicoVirtualScreen by remember { mutableStateOf(false) }
    var showAbastecimentoScreen by remember { mutableStateOf(false) }

    var showAnjoDaGuardaScreen by remember { mutableStateOf(false) }
    var showGaragemScreen by remember { mutableStateOf(false) }
    var showCarInfoScreen by remember { mutableStateOf(false) }
    var lembreteSelecionado by remember { mutableStateOf<Lembrete?>(null) }
    var contatoDetalheSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    var filtroTipo by remember { mutableStateOf<TipoManutencao?>(null) }
    var buscaTexto by remember { mutableStateOf("") }

    val lembretesDoCarroAtual = todosLembretes.filter { it.carroId == carroAtual.id }
    val lembretesFiltrados = if (filtroTipo == null) {
        lembretesDoCarroAtual
    } else {
        lembretesDoCarroAtual.filter { it.tipo == filtroTipo }
    }
    val lembretesComBusca = if (buscaTexto.isBlank()) {
        lembretesFiltrados
    } else {
        lembretesFiltrados.filter { lembrete ->
            lembrete.titulo.contains(buscaTexto, ignoreCase = true) ||
                    lembrete.peca.contains(buscaTexto, ignoreCase = true)
        }
    }
    val totalGastos = lembretesDoCarroAtual.sumOf { it.valor }
    val usuarioNome = FirebaseAuth.getInstance().currentUser?.displayName
    val nomeExibido = usuarioNome?.trim()?.split("\\s+".toRegex())?.let { partes ->
        if (partes.isEmpty()) null else if (partes.size == 1) partes[0] else "${partes.first()} ${partes.last()}"
    } ?: (FirebaseAuth.getInstance().currentUser?.email ?: "Usuario")

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val categoryScrollScope = rememberCoroutineScope()
    val categoryScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()
    val showTopBar by remember { derivedStateOf { contentScrollState.value <= 12 } }
    val activity = remember(context) { context.findActivity() }

    // Configuração da Barra de Status (Cor escura)
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.statusBarColor = android.graphics.Color.parseColor("#0F172A")
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        }
    }

    // ----------------- DIÁLOGOS -----------------
    if (showEditCarDialog) {
        EditarCarroDialog(
            carroAtual = carroAtual,
            titulo = "Editar Veículo",
            onDismiss = { showEditCarDialog = false },
            onSalvar = { carroEditado ->
                listaCarros = listaCarros.map { if (it.id == carroAtual.id) carroEditado else it }
                showEditCarDialog = false
            }
        )
    }
    if (showAddCarDialog) {
        EditarCarroDialog(
            carroAtual = CarroInfo(nome = "", modelo = ""),
            titulo = "Novo Carro",
            onDismiss = { showAddCarDialog = false },
            onSalvar = { novoCarro ->
                listaCarros = listaCarros + novoCarro
                indiceCarroAtual = listaCarros.lastIndex
                showAddCarDialog = false
            }
        )
    }
    if (showAddContatoDialog) {
        NovoContatoDialog(
            onDismiss = { showAddContatoDialog = false },
            onSalvar = { novo ->
                listaContatos = listaContatos + novo
                showAddContatoDialog = false
            }
        )
    }
    if (showTesteNotificacaoDialog) {
        NotificacaoRapidaDialog(
            onDismiss = { showTesteNotificacaoDialog = false },
            onDisparar = {
                NotificacaoHelper.dispararNotificacaoInstantanea(
                    context.applicationContext,
                    "Aviso Zellu",
                    "Teste de notificação enviado com sucesso."
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
        return
    }
    BackHandler(enabled = showAnjoDaGuardaScreen) { showAnjoDaGuardaScreen = false }
    if (showAnjoDaGuardaScreen) {
        AnjoDaGuardaScreen(onDismiss = { showAnjoDaGuardaScreen = false })
        return
    }

    BackHandler(enabled = showMecanicoVirtualScreen) { showMecanicoVirtualScreen = false }
    if (showMecanicoVirtualScreen) {
        MecanicoVirtualScreen(
            carro = carroAtual,
            lembretes = lembretesDoCarroAtual,
            onDismiss = { showMecanicoVirtualScreen = false }
        )
        return
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
    BackHandler(enabled = showCarInfoScreen) { showCarInfoScreen = false }
    if (showCarInfoScreen) {
        CarroInfoScreen(
            carro = carroAtual,
            lembretes = lembretesDoCarroAtual,
            onDismiss = { showCarInfoScreen = false }
        )
        return
    }

    BackHandler(enabled = showAbastecimentoScreen) { showAbastecimentoScreen = false }
    if (showAbastecimentoScreen) {
        AbastecimentoScreen(carroId = carroAtual.id, onDismiss = { showAbastecimentoScreen = false })
        return
    }

    BackHandler(enabled = showAddLembreteDialog) {
        showAddLembreteDialog = false
        iniciarCameraProduto = false
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
            onDelete = {
                NotificacaoHelper.cancelarNotificacao(context.applicationContext, selecionado.id)
                todosLembretes = todosLembretes.filter { it.id != selecionado.id }
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

    // ----------------- DRAWER (MENU LATERAL) -----------------
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = primaryDark,
                drawerContentColor = textLight
            ) {
                // Cabeçalho do Drawer com Gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E3A8A), primaryDark)))
                        .padding(24.dp)
                ) {
                    Column {
                        val fotoGoogle = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(2.dp, accentBlue, CircleShape)
                                .background(Color.White.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!fotoGoogle.isNullOrBlank()) {
                                AsyncImage(
                                    model = fotoGoogle,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Rounded.DirectionsCar, null, tint = textLight, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = nomeExibido,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textLight
                        )
                        Text(
                            text = "Seja bem vindo!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textDim
                        )
                    }
                }

                HorizontalDivider(color = surfaceDark, thickness = 1.dp)

                // Itens do Menu
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DrawerMenuItem(Icons.Rounded.DirectionsCar, "Minha Garagem") {
                        showGaragemScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.AddCircle, "Adicionar Veículo") {
                        showAddCarDialog = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.Settings, "Configurações") {
                        showConfiguracoes = true
                        drawerScope.launch { drawerState.close() }
                    }

                    DrawerMenuItem(Icons.Default.Build, "Mecanico Virtual") {
                        showMecanicoVirtualScreen = true
                        drawerScope.launch { drawerState.close() }
                    }

                    DrawerMenuItem(Icons.Default.Lock, "Privacidade e Termos") {
                        showPrivacidadeDialog = true
                        drawerScope.launch { drawerState.close() }
                    }

                    DrawerMenuItem(Icons.Default.Shield, "Zello Guardião") {
                        showAnjoDaGuardaScreen = true
                        drawerScope.launch { drawerState.close() }
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
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(0.5f))
                    ) {
                        Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sair da conta", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) {
        // ----------------- CONTEÚDO PRINCIPAL (SCAFFOLD) -----------------
        Scaffold(
            modifier = modifier,
            containerColor = primaryDark,
            topBar = {
                if (showTopBar) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Zellu",
                                color = textLight,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menu", tint = textLight)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = topBarDark)
                    )
                }
            }
        ) { innerPadding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(contentScrollState)
                ) {
                    Spacer(Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .shadow(10.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF23324D))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. HERO CARD DO CARRO (Com Estampa de Fundo)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF172554)),
                                            start = Offset(0f, 0f),
                                            end = Offset(1000f, 1000f)
                                        )
                                    )
                                    .clickable { showCarInfoScreen = true }
                            ) {
                                // --- CAMADA DE ESTAMPA (BACKGROUND DECORATIVO) ---
                                // Ícone 1: Óleo (Canto superior esquerdo)
                                Icon(
                                    imageVector = Icons.Rounded.WaterDrop,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.TopStart)
                                        .offset(x = (-20).dp, y = (-20).dp)
                                        .rotate(15f)
                                )
                                // Ícone 2: Ferramenta (Canto inferior direito)
                                Icon(
                                    imageVector = Icons.Rounded.Build,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .size(140.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 30.dp, y = 30.dp)
                                        .rotate(-25f)
                                )
                                // Ícone 3: Engrenagem (Canto superior direito)
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.04f),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 20.dp, y = 10.dp)
                                        .rotate(45f)
                                )
                                // Ícone 4: Velocímetro (Canto inferior esquerdo)
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.04f),
                                    modifier = Modifier
                                        .size(90.dp)
                                        .align(Alignment.BottomStart)
                                        .offset(x = 10.dp, y = 40.dp)
                                        .rotate(-10f)
                                )
                                // --- FIM DA ESTAMPA ---

                                // Conteúdo Principal do Card (Texto e Logo)
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Topo: Navegação e Nome
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (indiceCarroAtual > 0) indiceCarroAtual-- else indiceCarroAtual = listaCarros.lastIndex
                                            }
                                        ) {
                                            Icon(Icons.Default.ChevronLeft, null, tint = textLight.copy(0.7f), modifier = Modifier.size(32.dp))
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = carroAtual.marca.uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = carroAtual.nome,
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = textLight,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (indiceCarroAtual < listaCarros.lastIndex) indiceCarroAtual++ else indiceCarroAtual = 0
                                            }
                                        ) {
                                            Icon(Icons.Default.ChevronRight, null, tint = textLight.copy(0.7f), modifier = Modifier.size(32.dp))
                                        }
                                    }

                                    Spacer(Modifier.weight(1f))

                                    // Logo Central (Se houver logo da marca, exibe. Se não, exibe ícone padrão menor)
                                    val logoRes = carroAtual.logoResOrNull()
                                    if (logoRes != null) {
                                        Image(
                                            painter = painterResource(id = logoRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(70.dp),
                                            colorFilter = ColorFilter.tint(textLight)
                                        )
                                    } else {
                                        // Ícone padrão central (se não tiver logo)
                                        Icon(
                                            painter = painterResource(id = carroAtual.tipoIconRes()),
                                            contentDescription = null,
                                            tint = textLight,
                                            modifier = Modifier.size(80.dp)
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    // Modelo + KM atual
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = carroAtual.modelo.ifBlank { "Modelo não informado" },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = textLight.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = if (carroAtual.kmAtual > 0) "${carroAtual.kmAtual} km" else "KM nao informado",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textLight.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            // 2. AÇÕES RÁPIDAS (Botoes Lado a Lado)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ActionButton(
                                    icon = Icons.Rounded.Edit,
                                    label = "Editar veiculo",
                                    modifier = Modifier.weight(1f),
                                    onClick = { showEditCarDialog = true }
                                )
                                ActionButton(
                                    icon = Icons.Default.Description,
                                    label = "Abrir Relatório",
                                    modifier = Modifier.weight(1f),
                                    onClick = { showCarInfoScreen = true }
                                )
                            }

                            // 4. BOTÃO "NOVO LEMBRETE"
                            Button(
                                onClick = {
                                    iniciarCameraProduto = false
                                    showAddLembreteDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                            ) {
                                Icon(Icons.Default.Event, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Novo Lembrete", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1224)),
                        border = BorderStroke(1.dp, Color(0xFF23324D))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF111827))
                                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                        .offset(y = (-4).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalGasStation,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Abastecimento",
                                        color = textLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                Text(
                                    text = "Adicionar parada ao posto",
                                    color = textDim,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF111827))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(14.dp)
                                .defaultMinSize(minHeight = 64.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Consumo estimado", color = textLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Por semana: -- km • -- L", color = textDim, fontSize = 12.sp)
                            Text("No mês: -- km • -- L", color = textDim, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { showAbastecimentoScreen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                            ) {
                                Text(
                                    text = "Adicionar",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF23324D))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(categoryScrollState),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val contagem = TipoManutencao.values().associateWith { tipo ->
                                        lembretesDoCarroAtual.count { it.tipo == tipo }
                                    }
                                    listOf(
                                        TipoManutencao.OLEO,
                                        TipoManutencao.MECANICA,
                                        TipoManutencao.BATERIA,
                                        TipoManutencao.FREIO,
                                        TipoManutencao.TEMPERATURA,
                                        TipoManutencao.LICENCIAMENTO,
                                        TipoManutencao.IPVA,
                                        TipoManutencao.SEGURO
                                    ).forEach { tipo ->
                                        MonitorIcon(
                                            tipo = tipo,
                                            cor = calcularCorStatusLocal(lembretesDoCarroAtual, tipo),
                                            quantidade = contagem[tipo] ?: 0,
                                            selected = filtroTipo == tipo,
                                            onClick = {
                                                filtroTipo = if (filtroTipo == tipo) null else tipo
                                            },
                                            containerSize = 52.dp,
                                            boxSize = 44.dp,
                                            cornerRadius = 12.dp,
                                            iconSize = 16.dp,
                                            labelSize = 11.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            if (lembretesComBusca.isEmpty()) {
                                Text(
                                    text = "Nenhum lembrete encontrado",
                                    color = textDim,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                val lembretesOrdenados = lembretesComBusca.sortedBy { dataParaOrdenacao(it) }
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    lembretesOrdenados.forEach { lembrete ->
                                        LembreteCardLocal(
                                            lembrete = lembrete,
                                            contato = listaContatos.find { it.id == lembrete.contatoId },
                                            modeloCarro = carroAtual.nome,
                                            onDelete = {
                                                NotificacaoHelper.cancelarNotificacao(context.applicationContext, lembrete.id)
                                                todosLembretes = todosLembretes.filter { it.id != lembrete.id }
                                            },
                                            onAddPrestador = {
                                                lembreteSelecionado = lembrete
                                                contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                                            },
                                            onClick = {
                                                lembreteSelecionado = lembrete
                                                contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                                            },
                                            statusLabel = textoStatusPrazoLocal(lembrete),
                                            statusColor = calcularCorStatusLocal(lembretesDoCarroAtual, lembrete.tipo)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Spacer(Modifier.height(80.dp))

                }
            }
        }
    }
}

// ----------------- COMPONENTES AUXILIARES LOCAIS -----------------

@Composable
fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF94A3B8), // Slate 400
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color(0xFFF1F5F9), // Slate 100x
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)), // Surface Dark
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6), // Blue 500
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Color(0xFFE2E8F0), // Slate 200
                fontSize = 14.sp
            )
        }
    }
}

// ----------------- NOVO COMPONENTE LEMBRETE CARD (PREMIUM) -----------------
@Composable
fun LembreteCardLocal(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    modeloCarro: String,
    onDelete: () -> Unit,
    onAddPrestador: () -> Unit,
    onClick: () -> Unit,
    statusLabel: String,
    statusColor: Color
) {
    val bg = Color(0xFF111827)
    val bg2 = Color(0xFF0B1224)
    val stroke = Color(0xFF23324D)
    val text = Color(0xFFF1F5F9)
    val dim = Color(0xFF94A3B8)

    // Lógica para formatar o KM
    val kmFormatado = remember(lembrete.kmLimite) {
        val apenasDigitos = lembrete.kmLimite.filter { it.isDigit() }
        apenasDigitos.toLongOrNull()?.let {
            java.text.NumberFormat.getInstance(java.util.Locale("pt", "BR")).format(it)
        } ?: lembrete.kmLimite.ifBlank { "-" }
    }

    val iconBg = Brush.linearGradient(
        colors = listOf(
            statusColor.copy(alpha = 0.28f),
            statusColor.copy(alpha = 0.10f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, stroke),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(bg, bg2)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-2).dp)
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconBg)
                            .border(1.dp, statusColor.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForType(lembrete.tipo),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lembrete.titulo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (lembrete.peca.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Peça: ${lembrete.peca}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (lembrete.valor > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = formatarMoedaLocal(lembrete.valor),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- INFO CHIPS ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dataOuKm = when {
                        lembrete.dataLimite.isNotBlank() -> lembrete.dataLimite
                        lembrete.kmLimite.isNotBlank() -> kmFormatado + " km"
                        else -> "Sem meta"
                    }

                    // 1. Data
                    InfoMini(
                        icon = Icons.Rounded.CalendarMonth,
                        text = dataOuKm,
                        tint = dim,
                        iconTint = statusColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // 2. Status/Prazo
                    val statusIcon = when (statusLabel) {
                        "No Prazo", "Hoje" -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.ErrorOutline
                    }
                    val statusIconColor = when (statusLabel) {
                        "No Prazo", "Hoje" -> Color(0xFF10B981)
                        "Urgente" -> Color(0xFFF59E0B)
                        "Vencido" -> Color(0xFFEF4444)
                        else -> Color(0xFF94A3B8)
                    }
                    InfoMini(
                        icon = statusIcon,
                        text = statusLabel,
                        tint = dim,
                        iconTint = statusColor
                    )

                    Spacer(Modifier.width(12.dp))

                    // 3. KM
                    InfoMini(
                        icon = Icons.Rounded.Speed,
                        text = "KM: $kmFormatado",
                        tint = dim,
                        iconTint = statusColor,
                        ellipsize = false
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                if (contato != null) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Agendar o serviço com ${contato.nome}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Button(
                        onClick = onAddPrestador,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14532D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFFD1FAE5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Adicionar prestador do servico",
                            color = Color(0xFFD1FAE5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMini(
    icon: ImageVector,
    text: String,
    tint: Color,
    iconTint: Color = tint,
    ellipsize: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B1224))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
            .widthIn(min = 72.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign =  TextAlign.Center,
            maxLines = 1,
            overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}

@Composable
private fun ValorPill(valor: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF052E2B)) // verde escuro elegante
            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = valor,
            color = Color(0xFF34D399),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun BadgeStatus(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

// ----------------- FUNÇÕES AUXILIARES DE ESTILO E LÓGICA -----------------

fun getIconForType(tipo: TipoManutencao): ImageVector {
    return when (tipo) {
        TipoManutencao.OLEO -> Icons.Rounded.WaterDrop
        TipoManutencao.MECANICA -> Icons.Rounded.Build
        TipoManutencao.BATERIA -> Icons.Rounded.BatteryChargingFull
        TipoManutencao.FREIO -> Icons.Rounded.DiscFull
        TipoManutencao.TEMPERATURA -> Icons.Rounded.Thermostat
        TipoManutencao.LICENCIAMENTO, TipoManutencao.IPVA -> Icons.Rounded.Description
        TipoManutencao.SEGURO -> Icons.Rounded.Shield
        else -> Icons.Rounded.Notifications
    }
}

fun calcularCorStatusLocal(lembretes: List<Lembrete>, tipo: TipoManutencao): Color {
    return when (tipo) {
        TipoManutencao.OLEO -> Color(0xFF3B82F6) // Azul
        TipoManutencao.FREIO -> Color(0xFFEF4444) // Vermelho
        TipoManutencao.MECANICA -> Color(0xFFF59E0B) // Laranja
        TipoManutencao.LICENCIAMENTO -> Color(0xFF10B981) // Verde
        TipoManutencao.SEGURO -> Color(0xFF22C55E) // Verde claro
        else -> Color(0xFF6366F1) // Roxo padrão
    }
}

fun textoStatusPrazoLocal(lembrete: Lembrete): String {
    val hoje = LocalDate.now()
    val data = dataParaOrdenacao(lembrete)
    if (data == LocalDate.MAX) return "Acompanhar KM"
    val dias = ChronoUnit.DAYS.between(hoje, data)
    return when {
        dias < 0 -> "Vencido"
        dias == 0L -> "Hoje"
        dias <= 7 -> "Urgente"
        else -> "No Prazo"
    }
}

fun formatarMoedaLocal(valor: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
}

// ----------------- OUTROS COMPONENTES DA TELA DE DETALHES -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarroInfoScreen(
    carro: CarroInfo,
    lembretes: List<Lembrete>,
    onDismiss: () -> Unit
) {
    val primaryDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
    val topBarDark = Color(0xFF15223A)
    val textLight = Color(0xFFF1F5F9)
    val textDim = Color(0xFF94A3B8)
    val totalGastos = lembretes.sumOf { it.valor }
    val context = LocalContext.current
    val proximo = lembretes.minByOrNull { dataParaOrdenacao(it) }?.let {
        val data = dataParaOrdenacao(it)
        if (data == LocalDate.MAX) null else data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } ?: "Sem agenda"
    val corHex = String.format(Locale.US, "#%08X", carro.corArgb)
    val (tituloSaude, descricaoSaude) = calcularReputacao(lembretes)
    val corSaude = when (tituloSaude) {
        "Excelente" -> Color(0xFF10B981)
        "Crítica" -> Color(0xFFEF4444)
        "Em atenção" -> Color(0xFFEAB308)
        else -> textLight
    }
    val historicoManutencoes = lembretes
        .mapNotNull { lembrete ->
            val data = dataParaOrdenacao(lembrete)
            if (data == LocalDate.MAX) null else data to lembrete
        }
        .filter { (data, _) -> data.isBefore(LocalDate.now()) }
        .sortedByDescending { it.first }
        .take(6)
    val documentos = listOf(
        TipoManutencao.IPVA to "IPVA",
        TipoManutencao.LICENCIAMENTO to "Licenciamento"
    ).map { (tipo, label) ->
        val ultimaData = lembretes
            .filter { it.tipo == tipo }
            .map { dataParaOrdenacao(it) }
            .filter { it != LocalDate.MAX }
            .maxOrNull()
        val status = when {
            ultimaData == null -> "Não informado"
            !ultimaData.isBefore(LocalDate.now()) -> "Em dia"
            else -> "Vencido"
        }
        Triple(label, status, ultimaData?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "--")
    }
    val pecaLabels = linkedMapOf<String, String>()
    lembretes.forEach { lembrete ->
        val raw = lembrete.peca.ifBlank { lembrete.titulo }.trim()
        if (raw.isNotBlank()) {
            val key = raw.lowercase(Locale.getDefault())
            pecaLabels.putIfAbsent(key, raw)
        }
    }
    val trocasPorPeca = lembretes
        .map { lembrete -> lembrete.peca.ifBlank { lembrete.titulo }.trim() }
        .filter { it.isNotBlank() }
        .groupingBy { it.lowercase(Locale.getDefault()) }
        .eachCount()
        .map { (key, count) -> (pecaLabels[key] ?: key) to count }
        .sortedByDescending { it.second }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = primaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalhes do veiculo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            val uri = gerarPdfRelatorio(context, carro, lembretes)
                            if (uri != null) {
                                compartilharPdf(context, uri)
                            } else {
                                Toast.makeText(context, "Nao foi possivel gerar o PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PDF", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Informacoes gerais", color = textLight, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val logoRes = carro.logoResOrNull()
                            if (logoRes != null) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    colorFilter = ColorFilter.tint(textLight)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = carro.tipoIconRes()),
                                    contentDescription = null,
                                    tint = textLight,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(carro.nome, color = textLight, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(carro.marca.ifBlank { "Marca nao informada" }, color = Color.White, fontSize = 12.sp)
                                Text(
                                    if (carro.kmAtual > 0) "${carro.kmAtual} km" else "KM nao informado",
                                    color = textDim,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        InfoRow("Modelo", carro.modelo.ifBlank { "Nao informado" }, textLight, textDim)
                        InfoRow("Tipo", carro.tipoVeiculo.label, textLight, textDim)
                        InfoRow("KM atual", if (carro.kmAtual > 0) "${carro.kmAtual} km" else "Nao informado", textLight, textDim)
                    }
                }

                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Saude do veiculo", color = textLight, fontWeight = FontWeight.Bold)
                        Text(tituloSaude, color = corSaude, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(descricaoSaude, color = textDim, fontSize = 12.sp)
                    }
                }

                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Informacoes tecnicas", color = textLight, fontWeight = FontWeight.Bold)
                        InfoRow("Cor", corHex, textLight, textDim)
                        InfoRow("ID", carro.id, textLight, textDim)
                        InfoRow("Avisos ativos", lembretes.size.toString(), textLight, textDim)
                        InfoRow("Proximo servico", proximo, textLight, textDim)
                        InfoRow("Total gasto", formatarMoedaLocal(totalGastos), textLight, textDim)
                    }
                }

                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Historico de manutencoes", color = textLight, fontWeight = FontWeight.Bold)
                        if (historicoManutencoes.isEmpty()) {
                            Text("Nenhuma manutencao registrada ainda.", color = textDim, fontSize = 12.sp)
                        } else {
                            historicoManutencoes.forEach { (data, lembrete) ->
                                InfoRow(
                                    lembrete.titulo,
                                    data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    textLight,
                                    textDim
                                )
                            }
                        }
                    }
                }

                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Documentacao", color = textLight, fontWeight = FontWeight.Bold)
                        documentos.forEach { (label, status, data) ->
                            InfoRow(label, "$status • $data", textLight, textDim)
                        }
                    }
                }

                ElevatedCard(
                    colors = CardDefaults.cardColors(containerColor = surfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Trocas de peças", color = textLight, fontWeight = FontWeight.Bold)
                        if (trocasPorPeca.isEmpty()) {
                            Text("Nenhuma peça registrada ainda.", color = textDim, fontSize = 12.sp)
                        } else {
                            trocasPorPeca.forEach { (label, count) ->
                                InfoRow(label, "$count vez(es)", textLight, textDim)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Voltar", color = textLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, textLight: Color, textDim: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textDim, fontSize = 12.sp)
        Text(value, color = textLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

data class CategorySpend(
    val label: String,
    val valor: Double,
    val color: Color
)

@Composable
fun CategoryExpenseChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    emptyColor: Color = Color(0xFF334155),
    centerColor: Color = Color(0xFF0B1224)
) {
    val safeData =
        if (data.isEmpty()) listOf(CategorySpend(label = "Sem dados", valor = 0.0, color = emptyColor)) else data
    val totalValor = safeData.sumOf { it.valor }.coerceAtLeast(0.0)
    val hasData = totalValor > 0.0
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val barCount = safeData.size.coerceAtLeast(1)
        val spacingDp = 10.dp
        val spacingPx = with(density) { spacingDp.toPx() }
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalSpacingPx = spacingPx * (barCount - 1)
        val barWidthPx = ((totalWidthPx - totalSpacingPx) / barCount)
            .coerceAtLeast(with(density) { 6.dp.toPx() })
        val barWidthDp = with(density) { barWidthPx.toDp() }
        val iconSize = 14.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val maxValor = safeData.maxOfOrNull { it.valor }?.coerceAtLeast(0.0) ?: 0.0
                val maxHeight = size.height * 0.85f
                val baseY = size.height
            val lowColor = Color(0xFF22C55E)
            val midColor = Color(0xFFF59E0B)
            val highColor = Color(0xFFEF4444)
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val gridSteps = 4
            repeat(gridSteps + 1) { step ->
                val y = baseY - (maxHeight / gridSteps) * step
                val t = step.toFloat() / gridSteps.toFloat()
                val baseColor = if (t <= 0.5f) {
                    lerp(lowColor, midColor, t / 0.5f)
                } else {
                    lerp(midColor, highColor, (t - 0.5f) / 0.5f)
                }
                drawLine(
                    color = baseColor.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
                if (!hasData) {
                    val barHeight = maxHeight * 0.4f
                    val left = (size.width - barWidthPx) / 2f
                    drawRoundRect(
                        color = emptyColor,
                        topLeft = Offset(left, baseY - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                } else {
                    safeData.forEachIndexed { index, item ->
                        val ratio = if (maxValor > 0.0) (item.valor / maxValor).toFloat() else 0f
                        val barHeight = (maxHeight * ratio * progress.value).coerceAtLeast(4.dp.toPx())
                        val left = index * (barWidthPx + spacingPx)
                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(left, baseY - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )
                        val percent = if (totalValor > 0.0) ((item.valor / totalValor) * 100).toInt() else 0
                        if (percent > 0) {
                            val x = left + (barWidthPx / 2f)
                            val y = (baseY - barHeight - 6.dp.toPx()).coerceAtLeast(textPaint.textSize)
                            drawContext.canvas.nativeCanvas.drawText("$percent%", x, y, textPaint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacingDp)
            ) {
                safeData.forEach { item ->
                    val tipo = TipoManutencao.values().firstOrNull { it.label == item.label }
                    Box(
                        modifier = Modifier.width(barWidthDp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tipo != null) {
                            Icon(
                                imageVector = tipo.getIcon(),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryExpenseLegend(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    labelColor: Color = Color(0xFF94A3B8),
    emptyColor: Color = Color(0xFF334155),
    minItems: Int = 0
) {
    val safeData =
        if (data.isEmpty()) listOf(CategorySpend(label = "Sem dados", valor = 0.0, color = emptyColor)) else data
    val totalValor = safeData.sumOf { it.valor }.coerceAtLeast(0.0)
    val centralizarLegenda = safeData.size <= 5
    Column(
        modifier = modifier,
        verticalArrangement = if (centralizarLegenda) Arrangement.Center else Arrangement.spacedBy(6.dp)
    ) {
        safeData.forEach { item ->
            val dotColor = if (item.valor <= 0.0) emptyColor else item.color
            val tipo = TipoManutencao.values().firstOrNull { it.label == item.label }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(8.dp))
                if (tipo != null) {
                    Icon(
                        imageVector = tipo.getIcon(),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.label,
                    color = labelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatarMoedaLocal(item.valor),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        if (safeData.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(Modifier.height(0.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total:", color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatarMoedaLocal(totalValor),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val fillers = (minItems - safeData.size).coerceAtLeast(0)
        repeat(fillers) { Spacer(Modifier.height(16.dp)) }
    }
}

fun corCategoria(tipo: TipoManutencao): Color = when (tipo) {
    TipoManutencao.OLEO -> Color(0xFF3B82F6) // azul
    TipoManutencao.BATERIA -> Color(0xFF16A34A) // verde
    TipoManutencao.MECANICA -> Color(0xFF60A5FA) // azul claro
    TipoManutencao.FREIO -> Color(0xFFDC2626) // vermelho
    TipoManutencao.TEMPERATURA -> Color(0xFFEF4444) // vermelho claro
    TipoManutencao.LICENCIAMENTO -> Color(0xFF22C55E) // verde claro
    TipoManutencao.IPVA -> Color(0xFF5B8DEF) // azul leve
    TipoManutencao.SEGURO -> Color(0xFF10B981) // verde
    TipoManutencao.OUTROS -> Color(0xFF94A3B8)
}
