package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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
    context: Context = LocalContext.current
) {
    // ----------------- ESTADOS E VARIÁVEIS -----------------
    var listaCarros by remember { mutableStateOf<List<CarroInfo>>(emptyList()) }
    var listaContatos by remember { mutableStateOf<List<ContatoProfissional>>(emptyList()) }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // CORES DO TEMA (Azul Premium)
    val primaryDark = Color(0xFF0F172A)
    val surfaceDark = Color(0xFF1E293B)
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

    // Persistência automática ao alterar dados
    LaunchedEffect(listaCarros) { if(!isLoading && listaCarros.isNotEmpty()) withContext(Dispatchers.IO) { BancoDeDados.salvarCarros(context, listaCarros) } }
    LaunchedEffect(listaContatos) { if(!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarContatos(context, listaContatos) } }
    LaunchedEffect(todosLembretes) { if(!isLoading) withContext(Dispatchers.IO) { BancoDeDados.salvarLembretes(context, todosLembretes) } }

    var indiceCarroAtual by remember { mutableIntStateOf(0) }
    val carroAtual = if (listaCarros.isNotEmpty()) {
        if (indiceCarroAtual >= listaCarros.size) indiceCarroAtual = 0
        listaCarros[indiceCarroAtual]
    } else { CarroInfo() }

    // Estados de Controle de Interface
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
    var filtroTipo by remember { mutableStateOf<TipoManutencao?>(null) }

    val lembretesDoCarroAtual = todosLembretes.filter { it.carroId == carroAtual.id }
    val lembretesFiltrados = if (filtroTipo == null) {
        lembretesDoCarroAtual
    } else {
        lembretesDoCarroAtual.filter { it.tipo == filtroTipo }
    }
    val totalGastos = lembretesDoCarroAtual.sumOf { it.valor }
    val totalGastosCount = lembretesDoCarroAtual.count { it.valor > 0 }
    val categorySpendData = TipoManutencao.values().map { tipo ->
        val totalCategoria = lembretesDoCarroAtual.filter { it.tipo == tipo }.sumOf { it.valor }
        CategorySpend(
            label = tipo.label,
            valor = totalCategoria,
            color = corCategoria(tipo)
        )
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
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
    if (showRelatorioDialog) {
        RelatorioVeiculoScreen(
            carroAtual = carroAtual,
            lembretes = lembretesDoCarroAtual,
            onDismiss = { showRelatorioDialog = false }
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
                            text = "Olá, Motorista",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textLight
                        )
                        Text(
                            text = "Gerencie sua frota com o Zellu",
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
                    DrawerMenuItem(Icons.Default.Lock, "Privacidade e Termos") {
                        showPrivacidadeDialog = true
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = primaryDark)
                )
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(10.dp))

                    // 1. HERO CARD DO CARRO (Com Estampa de Fundo)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(260.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF172554)),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 1000f)
                                )
                            )
                            .clickable { showRelatorioDialog = true }
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
                                        color = accentBlue,
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

                            // Modelo
                            Text(
                                text = carroAtual.modelo.ifBlank { "Modelo não informado" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDim
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. AÇÕES RÁPIDAS (Botoes Lado a Lado)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Rounded.Edit,
                            label = "Editar",
                            modifier = Modifier.weight(1f),
                            onClick = { showEditCarDialog = true }
                        )
                        ActionButton(
                            icon = Icons.Default.Description,
                            label = "Relatório",
                            modifier = Modifier.weight(1f),
                            onClick = { showRelatorioDialog = true }
                        )
                    }

                    Spacer(Modifier.height(24.dp))


                    // 4. BOTÃO "NOVO LEMBRETE"
                    Button(
                        onClick = {
                            iniciarCameraProduto = false
                            showAddLembreteDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Novo Lembrete", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceDark)
                            .border(1.dp, Color(0xFF24324D), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF122542)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Payments, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Controle de gastos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Total gasto no carro", color = textDim, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = formatarMoeda(totalGastos),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (totalGastosCount > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "$totalGastosCount registros com valor",
                                color = textDim,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceDark)
                            .border(1.dp, Color(0xFF24324D), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF122542)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ShowChart, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gasto por categoria", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Controle de gastos por tipo", color = textDim, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Total: ${formatarMoeda(totalGastos)}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        CategoryExpenseLineChart(
                            data = categorySpendData,
                            lineColor = Color(0xFF60A5FA),
                            gridColor = Color(0xFF13203A),
                            labelColor = textDim,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        val scrollState = rememberScrollState()
                        LaunchedEffect(categorySpendData.size) {
                            while (true) {
                                if (scrollState.maxValue > 0) {
                                    scrollState.animateScrollBy(1.6f, animationSpec = tween(45))
                                    if (scrollState.value >= scrollState.maxValue) {
                                        scrollState.scrollTo(0)
                                    }
                                }
                                delay(16)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            categorySpendData.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${item.label}: ${formatarMoeda(item.valor)}",
                                        color = textDim,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 3. PAINEL DE STATUS (Monitoramento)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(surfaceDark)
                            .border(1.dp, Color(0xFF24324D), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Categorias",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                TipoManutencao.IPVA
                            ).forEach { tipo ->
                                // FIX: Usando 'quantidade' conforme solicitado no erro
                                MonitorIcon(
                                    tipo = tipo,
                                    cor = calcularCorStatus(lembretesDoCarroAtual, tipo),
                                    quantidade = contagem[tipo] ?: 0,
                                    selected = filtroTipo == tipo,
                                    onClick = {
                                        filtroTipo = if (filtroTipo == tipo) null else tipo
                                    }
                                )
                                Spacer(Modifier.width(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 5. LISTA DE LEMBRETES
                    Text(
                        text = "Próximas Manutenções",
                        style = MaterialTheme.typography.titleMedium,
                        color = textLight,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )

                    if (lembretesFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, tint = textDim, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Tudo em dia! ✅", color = textDim)
                            }
                        }
                    } else {
                        val lembretesOrdenados = lembretesFiltrados.sortedBy { dataParaOrdenacao(it) }
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            lembretesOrdenados.forEach { lembrete ->
                                LembreteCard(
                                    lembrete = lembrete,
                                    contato = listaContatos.find { it.id == lembrete.contatoId },
                                    modeloCarro = carroAtual.modelo,
                                    onDelete = {
                                        NotificacaoHelper.cancelarNotificacao(context.applicationContext, lembrete.id)
                                        todosLembretes = todosLembretes.filter { it.id != lembrete.id }
                                    },
                                    onClick = {
                                        lembreteSelecionado = lembrete
                                        contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                                    },
                                    statusLabel = textoStatusPrazo(lembrete),
                                    statusColor = calcularCorStatus(lembretesDoCarroAtual, lembrete.tipo)
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
        shape = RoundedCornerShape(12.dp),
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

data class CategorySpend(
    val label: String,
    val valor: Double,
    val color: Color
)

@Composable
fun CategoryExpenseLineChart(
    data: List<CategorySpend>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF3B82F6),
    gridColor: Color = Color(0xFF1E293B),
    labelColor: Color = Color(0xFF94A3B8)
) {
    val safeData = if (data.isEmpty()) listOf(CategorySpend(label = "--", valor = 0.0, color = lineColor)) else data
    val maxValor = safeData.maxOf { it.valor }.coerceAtLeast(1.0)
    val maxIndex = if (maxValor > 0) safeData.indexOfFirst { it.valor == maxValor } else -1
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val count = safeData.size
            val spacing = if (count <= 1) 0f else size.width / (count - 1)
            val guideColor = labelColor.copy(alpha = 0.12f)
            val guideSteps = 3
            for (i in 1..guideSteps) {
                val y = size.height * (i / (guideSteps + 1f))
                drawLine(
                    color = guideColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
            val points = safeData.mapIndexed { index, item ->
                val ratio = if (item.valor <= 0.0) 0f else (item.valor / maxValor).toFloat().coerceIn(0f, 1f)
                val x = if (count <= 1) size.width / 2 else spacing * index
                val y = size.height - (size.height * ratio)
                Offset(x, y)
            }
            if (points.size >= 2) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
            points.forEachIndexed { index, point ->
                val dotColor = safeData[index].color
                val isMax = index == maxIndex
                drawCircle(
                    color = dotColor,
                    radius = if (isMax) 6f else 4.5f,
                    center = point
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            safeData.forEach { item ->
                Text(
                    text = item.label,
                    color = labelColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun corCategoria(tipo: TipoManutencao): Color = when (tipo) {
    TipoManutencao.OLEO -> Color(0xFF38BDF8)
    TipoManutencao.BATERIA -> Color(0xFFF97316)
    TipoManutencao.MECANICA -> Color(0xFF22C55E)
    TipoManutencao.FREIO -> Color(0xFFEF4444)
    TipoManutencao.TEMPERATURA -> Color(0xFFEAB308)
    TipoManutencao.LICENCIAMENTO -> Color(0xFFA855F7)
    TipoManutencao.IPVA -> Color(0xFF06B6D4)
    TipoManutencao.OUTROS -> Color(0xFF94A3B8)
}
