package br.com.gui.carlembrete

import BikeDistanceCard
import AvisosCategoriasCard
import CarroInfoCard
import HistoricoAbastecimentoScreen
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.math.ceil

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
    onLoaded: () -> Unit = {},
    onThemeModeChanged: (AppThemeMode) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    // ----------------- ESTADOS E VARIÁVEIS -----------------
    var listaCarros by remember { mutableStateOf<List<CarroInfo>>(emptyList()) }
    var listaContatos by remember { mutableStateOf<List<ContatoProfissional>>(emptyList()) }
    var todosLembretes by remember { mutableStateOf<List<Lembrete>>(emptyList()) }
    var abastecimentos by remember { mutableStateOf<List<Abastecimento>>(emptyList()) }
    var pedaladas by remember { mutableStateOf<List<Pedalada>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var notifiedLoaded by remember { mutableStateOf(false) }

    // CORES DO TEMA (Azul Premium)
    val primaryDark = colorScheme.background
    val surfaceDark = colorScheme.surface
    val fuelCardStart = if (isDark) colorScheme.surface else Color.White
    val fuelCardEnd = if (isDark) colorScheme.background else Color.White
    val topBarDark = colorScheme.background
    val accentBlue = colorScheme.primary
    val textLight = colorScheme.onSurface
    val textDim = colorScheme.onSurfaceVariant

    // ----------------- CARREGAMENTO DE DADOS -----------------
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val carros = BancoDeDados.carregarCarrosComFallback(context)
            val contatos = BancoDeDados.carregarContatos(context)
            val lembretes = BancoDeDados.carregarLembretes(context)
            val abastecimentosDb = BancoDeDados.carregarAbastecimentos(context)
            val pedaladasDb = BancoDeDados.carregarPedaladas(context)
            withContext(Dispatchers.Main) {
                listaCarros = carros
                listaContatos = contatos
                todosLembretes = lembretes
                abastecimentos = abastecimentosDb
                pedaladas = pedaladasDb
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
    var showEditCarScreen by remember { mutableStateOf(false) }
    var showAddCarScreen by remember { mutableStateOf(false) }
    var showAddLembreteDialog by remember { mutableStateOf(false) }
    var showTipoAvisoDialog by remember { mutableStateOf(false) }
    var tipoAvisoSelecionado by remember { mutableStateOf(TipoManutencao.OLEO) }
    var iniciarCameraProduto by remember { mutableStateOf(false) }
    var showAddContatoDialog by remember { mutableStateOf(false) }
    var lembreteParaVincularContato by remember { mutableStateOf<String?>(null) }
    var showTesteNotificacaoDialog by remember { mutableStateOf(false) }
    var showConfiguracoes by remember { mutableStateOf(false) }
    var showPrivacidadeDialog by remember { mutableStateOf(false) }
    var showMecanicoVirtualScreen by remember { mutableStateOf(false) }
    var showAbastecimentoScreen by remember { mutableStateOf(false) }
    var showHistoricoAbastecimentoScreen by remember { mutableStateOf(false) }
    var showBikeDistanceRegister by remember { mutableStateOf(false) }
    var showBikeDistanceHistory by remember { mutableStateOf(false) }
    var showPremiumHubScreen by remember { mutableStateOf(false) }
    var showShareVehicleScreen by remember { mutableStateOf(false) }
    var showAondePareiScreen by remember { mutableStateOf(false) }
    var showAiAssistantScreen by remember { mutableStateOf(false) }

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
    val contentScrollState = rememberScrollState()
    val showTopBar by remember { derivedStateOf { contentScrollState.value <= 12 } }
    val activity = remember(context) { context.findActivity() }
    val subscriptionManager = remember { SubscriptionManager(context) }
    val planTier by subscriptionManager.planTier.collectAsState()
    val isSubscribed by subscriptionManager.isSubscribed.collectAsState()
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showPremiumInfo by remember { mutableStateOf(false) }
    val vehicleLimit = if (planTier == PlanTier.FREE) 3 else Int.MAX_VALUE
    val lembreteLimit = if (planTier == PlanTier.FREE) 15 else Int.MAX_VALUE

    DisposableEffect(Unit) {
        subscriptionManager.connect()
        onDispose { subscriptionManager.disconnect() }
    }

    // ----------------- TELAS DE VEÍCULO -----------------
    BackHandler(enabled = showEditCarScreen) { showEditCarScreen = false }
    if (showEditCarScreen) {
        EditarCarroScreen(
            carroAtual = carroAtual,
            onDismiss = { showEditCarScreen = false },
            onSalvar = { carroEditado ->
                listaCarros = listaCarros.map { if (it.id == carroAtual.id) carroEditado else it }
                showEditCarScreen = false
            }
        )
        return
    }
    BackHandler(enabled = showAddCarScreen) { showAddCarScreen = false }
    if (showAddCarScreen) {
        NovoCarroScreen(
            onDismiss = { showAddCarScreen = false },
            onSalvar = { novoCarro ->
                if (listaCarros.size >= vehicleLimit) {
                    Toast.makeText(context, "Limite de veículos do plano grátis atingido.", Toast.LENGTH_SHORT).show()
                    showPremiumDialog = true
                    showAddCarScreen = false
                } else {
                    listaCarros = listaCarros + novoCarro
                    indiceCarroAtual = listaCarros.lastIndex
                    showAddCarScreen = false
                }
            }
        )
        return
    }
    if (showAddContatoDialog) {
        NovoContatoDialog(
            onDismiss = { showAddContatoDialog = false },
            onSalvar = { novo ->
                listaContatos = listaContatos + novo
                lembreteParaVincularContato?.let { lembreteId ->
                    todosLembretes = todosLembretes.map { lembrete ->
                        if (lembrete.id == lembreteId) lembrete.copy(contatoId = novo.id) else lembrete
                    }
                    lembreteSelecionado = todosLembretes.find { it.id == lembreteId }
                    contatoDetalheSelecionado = novo
                    lembreteParaVincularContato = null
                }
                showAddContatoDialog = false
            }
        )
    }
    BackHandler(enabled = showTipoAvisoDialog) { showTipoAvisoDialog = false }
    if (showTipoAvisoDialog) {
        val isBike = carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA
        val tiposAviso = if (isBike) {
            listOf(
                TipoManutencao.CORRENTE,
                TipoManutencao.LUBRIFICACAO,
                TipoManutencao.PEDIVELA,
                TipoManutencao.ACESSORIOS,
                TipoManutencao.CONFORTO,
                TipoManutencao.FREIO,
                TipoManutencao.PNEU,
                TipoManutencao.TRANSMISSAO,
                TipoManutencao.REVISAO,
                TipoManutencao.OUTROS
            )
        } else {
            listOf(
                TipoManutencao.OLEO,
                TipoManutencao.MECANICA,
                TipoManutencao.FREIO,
                TipoManutencao.BATERIA,
                TipoManutencao.PNEU,
                TipoManutencao.LICENCIAMENTO,
                TipoManutencao.IPVA,
                TipoManutencao.SEGURO,
                TipoManutencao.OUTROS
            )
        }
        val itensAviso = listOf(
            AvisoItem(
                label = "Lembrar aonde estacionei",
                icon = Icons.Default.LocalParking,
                color = accentBlue,
                wide = true
            ) {
                showTipoAvisoDialog = false
                showAondePareiScreen = true
            }
        ) + (if (isBike) emptyList() else listOf(
            AvisoItem("Gasolina", Icons.Rounded.LocalGasStation, accentBlue) {
                showTipoAvisoDialog = false
                showAbastecimentoScreen = true
            }
        )) + tiposAviso.map { tipo ->
            val label = if (isBike && tipo == TipoManutencao.REVISAO) "Peças" else tipo.label
            AvisoItem(
                label,
                tipo.getIcon(),
                calcularCorStatusLocal(lembretesDoCarroAtual, tipo),
                tipo = tipo,
                iconOverride = if (isBike && tipo == TipoManutencao.FREIO) Icons.Rounded.DirectionsBike else null
            ) {
                showTipoAvisoDialog = false
                iniciarCameraProduto = false
                tipoAvisoSelecionado = tipo
                showAddLembreteDialog = true
            }
        }
        val avisoBackground = if (isDark) {
            Brush.verticalGradient(
                listOf(Color(0xFF16233A), primaryDark, Color(0xFF0F172A))
            )
        } else {
            SolidColor(Color.White)
        }
        val avisoTextPrimary = if (isDark) textLight else Color.Black
        val avisoTextDim = if (isDark) textDim else Color(0xFF475569)
        TipoAvisoScreen(
            itensAviso = itensAviso,
            backgroundBrush = avisoBackground,
            surfaceDark = if (isDark) surfaceDark else Color.White,
            textLight = avisoTextPrimary,
            textDim = avisoTextDim,
            onDismiss = { showTipoAvisoDialog = false }
        )
        return
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
            contatos = listaContatos,
            onThemeModeChanged = onThemeModeChanged
        )
        return
    }
    BackHandler(enabled = showAnjoDaGuardaScreen) { showAnjoDaGuardaScreen = false }
    if (showAnjoDaGuardaScreen) {
        AnjoDaGuardaScreen(onDismiss = { showAnjoDaGuardaScreen = false })
        return
    }
    BackHandler(enabled = showShareVehicleScreen) { showShareVehicleScreen = false }
    if (showShareVehicleScreen) {
        ShareVehicleScreen(
            carroAtual = carroAtual,
            onDismiss = { showShareVehicleScreen = false }
        )
        return
    }

    BackHandler(enabled = showMecanicoVirtualScreen) { showMecanicoVirtualScreen = false }
    if (showMecanicoVirtualScreen) {
        MecanicoVirtualScreen(
            carros = listaCarros,
            abastecimentos = abastecimentos,
            lembretes = todosLembretes,
            isPremium = isSubscribed,
            onPremiumRequired = { activity?.let { subscriptionManager.launchPurchaseFlow(it) } },
            onDismiss = { showMecanicoVirtualScreen = false }
        )
        return
    }
    BackHandler(enabled = showPremiumHubScreen) { showPremiumHubScreen = false }
    if (showPremiumHubScreen) {
        PremiumHubScreen(
            isPremium = planTier != PlanTier.FREE,
            onDismiss = { showPremiumHubScreen = false },
            onOpenGuardian = {
                showPremiumHubScreen = false
                showAnjoDaGuardaScreen = true
            },
            onOpenFinance = {
                showPremiumHubScreen = false
                showMecanicoVirtualScreen = true
            },
            onOpenAiAssistant = {
                showPremiumHubScreen = false
                showAiAssistantScreen = true
            },
            onOpenSubscribe = {
                showPremiumHubScreen = false
                activity?.let { subscriptionManager.launchPurchaseFlow(it) }
            }
        )
        return
    }

    if (showPrivacidadeDialog) {
        PrivacidadeTermosDialog(onDismiss = { showPrivacidadeDialog = false })
    }
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("Recurso Premium", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold) },
            text = { Text("Assine o Premium para usar OCR, PDF e backup automático.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        activity?.let { subscriptionManager.launchPurchaseFlow(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Assinar Premium", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPremiumDialog = false },
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Text("Agora não", color = Color(0xFFF59E0B))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
    if (showPremiumInfo) {
        AlertDialog(
            onDismissRequest = { showPremiumInfo = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_diamond_alt),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Zellu Premium", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Desbloqueie recursos avançados:", color = Color(0xFFCBD5E1))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text("Zellu Guardião", color = Color(0xFFCBD5E1))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text("Backup automático no Google Drive", color = Color(0xFFCBD5E1))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text("OCR ilimitado", color = Color(0xFFCBD5E1))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text("PDF completo", color = Color(0xFFCBD5E1))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(Modifier.width(8.dp))
                        Text("Veículos ilimitados", color = Color(0xFFCBD5E1))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumInfo = false
                        activity?.let { subscriptionManager.launchPurchaseFlow(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Assinar Premium", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPremiumInfo = false },
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Text("Agora não", color = Color(0xFFF59E0B))
                }
            },
            containerColor = Color(0xFF162235)
        )
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
            isPremium = planTier != PlanTier.FREE,
            onDismiss = { showCarInfoScreen = false }
        )
        return
    }

    BackHandler(enabled = showAbastecimentoScreen) { showAbastecimentoScreen = false }
    if (showAbastecimentoScreen) {
        AbastecimentoScreen(carroId = carroAtual.id, onDismiss = { showAbastecimentoScreen = false })
        return
    }
    BackHandler(enabled = showHistoricoAbastecimentoScreen) { showHistoricoAbastecimentoScreen = false }
    if (showHistoricoAbastecimentoScreen) {
        HistoricoAbastecimentoScreen(carroId = carroAtual.id, onDismiss = { showHistoricoAbastecimentoScreen = false })
        return
    }
    BackHandler(enabled = showBikeDistanceRegister) { showBikeDistanceRegister = false }
    if (showBikeDistanceRegister) {
        BikeDistanceScreen(carroId = carroAtual.id, onDismiss = { showBikeDistanceRegister = false })
        return
    }
    BackHandler(enabled = showBikeDistanceHistory) { showBikeDistanceHistory = false }
    if (showBikeDistanceHistory) {
        BikeDistanceHistoryScreen(carroId = carroAtual.id, onDismiss = { showBikeDistanceHistory = false })
        return
    }
    BackHandler(enabled = showAondePareiScreen) { showAondePareiScreen = false }
    if (showAondePareiScreen) {
        AondePareiScreen(onDismiss = { showAondePareiScreen = false })
        return
    }
    BackHandler(enabled = showAiAssistantScreen) { showAiAssistantScreen = false }
    if (showAiAssistantScreen) {
        AssistentePremiumScreen(onDismiss = { showAiAssistantScreen = false })
        return
    }
    LaunchedEffect(showAbastecimentoScreen) {
        if (!showAbastecimentoScreen) {
            abastecimentos = withContext(Dispatchers.IO) { BancoDeDados.carregarAbastecimentos(context) }
        }
    }
    LaunchedEffect(showBikeDistanceRegister, showBikeDistanceHistory) {
        if (!showBikeDistanceRegister && !showBikeDistanceHistory) {
            pedaladas = withContext(Dispatchers.IO) { BancoDeDados.carregarPedaladas(context) }
        }
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
                if (todosLembretes.size + 1 > lembreteLimit) {
                    Toast.makeText(context, "Limite de lembretes do plano grátis atingido.", Toast.LENGTH_SHORT).show()
                    showPremiumDialog = true
                } else {
                    todosLembretes = todosLembretes + novo.copy(carroId = carroAtual.id)
                    showAddLembreteDialog = false
                    iniciarCameraProduto = false
                }
            },
            onMultiConfirm = { novosItens ->
                val novosLembretes = novosItens.map { it.copy(carroId = carroAtual.id) }
                if (todosLembretes.size + novosLembretes.size > lembreteLimit) {
                    Toast.makeText(context, "Limite de lembretes do plano grátis atingido.", Toast.LENGTH_SHORT).show()
                    showPremiumDialog = true
                } else {
                    todosLembretes = todosLembretes + novosLembretes
                    showAddLembreteDialog = false
                    iniciarCameraProduto = false
                    Toast.makeText(context, "${novosLembretes.size} itens salvos!", Toast.LENGTH_SHORT).show()
                }
            },
            onUpdateKmCarro = { novoKm -> listaCarros = listaCarros.map { if (it.id == carroAtual.id) it.copy(kmAtual = novoKm) else it } },
            autoAbrirCamera = iniciarCameraProduto,
            onAutoCameraConsumida = { iniciarCameraProduto = false },
            onAddContato = { novo ->
                listaContatos = listaContatos + novo
            },
            initialTipo = tipoAvisoSelecionado,
            planTier = planTier,
            onRequestPremium = { showPremiumDialog = true }
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
                drawerContainerColor = fuelCardEnd,
                drawerContentColor = textLight
            ) {
                // Cabeçalho do Drawer com Gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(fuelCardStart, fuelCardEnd)))
                        .padding(24.dp)
                ) {
                    val fotoGoogle = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
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
                                Icon(Icons.Rounded.DirectionsCar, null, tint = textLight, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = nomeExibido,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textLight
                            )
                            Text(
                                text = "Seja bem vindo!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textDim
                            )
                        }
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
                    Text(
                        text = "VEÍCULO",
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(Icons.Rounded.DirectionsCar, "Meus Veículos") {
                        showGaragemScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.AddCircle, "Adicionar Veículo") {
                        if (listaCarros.size >= vehicleLimit) {
                            Toast.makeText(context, "Limite de veículos do plano grátis atingido.", Toast.LENGTH_SHORT).show()
                            showPremiumDialog = true
                        } else {
                            showAddCarScreen = true
                        }
                        drawerScope.launch { drawerState.close() }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "SERVIÇOS",
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.WorkspacePremium,
                        label = "Zellu Premium",
                        highlighted = true
                    ) {
                        showPremiumHubScreen = true
                        drawerScope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "SEGURANÇA",
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(Icons.Default.Lock, "Privacidade e Termos") {
                        showPrivacidadeDialog = true
                        drawerScope.launch { drawerState.close() }
                    }
                    DrawerMenuItem(Icons.Default.Shield, "Zello Guardião") {
                        showAnjoDaGuardaScreen = true
                        drawerScope.launch { drawerState.close() }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "AJUSTES",
                        color = textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DrawerMenuItem(Icons.Default.Settings, "Configurações") {
                        showConfiguracoes = true
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
        containerColor = Color.Transparent,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                        modifier = Modifier.statusBarsPadding(),
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
                        actions = {
                            IconButton(onClick = { showPremiumHubScreen = true }) {
                                Icon(
                                    painterResource(id = R.drawable.ic_diamond_alt),
                                    contentDescription = "Premium",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = topBarDark)
                    )
                }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
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
                    Spacer(Modifier.height(8.dp))

                    CarroInfoCard(
                        carroAtual = carroAtual,
                        onPrevCar = {
                            if (indiceCarroAtual > 0) indiceCarroAtual-- else indiceCarroAtual = listaCarros.lastIndex
                        },
                        onNextCar = {
                            if (indiceCarroAtual < listaCarros.lastIndex) indiceCarroAtual++ else indiceCarroAtual = 0
                        },
                        onOpenCarInfo = { showCarInfoScreen = true },
                        onEditCar = { showEditCarScreen = true },
                        onOpenRelatorio = { showCarInfoScreen = true },
                        onNovoLembrete = {
                            iniciarCameraProduto = false
                            showTipoAvisoDialog = true
                        },
                        nomeMantedor = nomeExibido,
                        textLight = textLight,
                        accentBlue = accentBlue
                    )

                    Spacer(Modifier.height(8.dp))

                    if (carroAtual.tipoVeiculo != TipoVeiculo.BICICLETA) {
                        OutlinedButton(
                            onClick = { showHistoricoAbastecimentoScreen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black)
                        ) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Historico de abastecimento", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) {
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val pedaladasDoCarro = pedaladas.filter { it.carroId == carroAtual.id }
                        val hoje = LocalDate.now()
                        val inicioSemana = hoje.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        val inicioMes = hoje.withDayOfMonth(1)
                        val kmHoje = pedaladasDoCarro.sumOf { item ->
                            runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
                                ?.takeIf { it == hoje }
                                ?.let { item.km } ?: 0.0
                        }
                        val kmSemana = pedaladasDoCarro.sumOf { item ->
                            runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
                                ?.takeIf { !it.isBefore(inicioSemana) && !it.isAfter(hoje) }
                                ?.let { item.km } ?: 0.0
                        }
                        val kmMes = pedaladasDoCarro.sumOf { item ->
                            runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
                                ?.takeIf { !it.isBefore(inicioMes) && !it.isAfter(hoje) }
                                ?.let { item.km } ?: 0.0
                        }
                        val kmTotal = pedaladasDoCarro.sumOf { it.km }
                        BikeDistanceCard(
                            kmHoje = kmHoje,
                            kmSemana = kmSemana,
                            kmMes = kmMes,
                            kmTotal = kmTotal,
                            onRegistrar = { showBikeDistanceRegister = true },
                            onHistorico = { showBikeDistanceHistory = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    val categoriasDisponiveis = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) {
                        listOf(
                            TipoManutencao.CORRENTE,
                            TipoManutencao.LUBRIFICACAO,
                            TipoManutencao.PEDIVELA,
                            TipoManutencao.ACESSORIOS,
                            TipoManutencao.CONFORTO,
                            TipoManutencao.FREIO,
                            TipoManutencao.PNEU,
                            TipoManutencao.TRANSMISSAO,
                            TipoManutencao.REVISAO,
                            TipoManutencao.OUTROS
                        )
                    } else {
                        listOf(
                            TipoManutencao.OLEO,
                            TipoManutencao.MECANICA,
                            TipoManutencao.BATERIA,
                            TipoManutencao.FREIO,
                            TipoManutencao.PNEU,
                            TipoManutencao.LICENCIAMENTO,
                            TipoManutencao.IPVA,
                            TipoManutencao.SEGURO,
                            TipoManutencao.OUTROS
                        )
                    }
                    val iconOverrides = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) {
                        mapOf(TipoManutencao.FREIO to Icons.Rounded.DirectionsBike)
                    } else {
                        emptyMap()
                    }
                    val labelOverrides = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) {
                        mapOf(TipoManutencao.REVISAO to "Peças")
                    } else {
                        emptyMap()
                    }
                    Spacer(Modifier.height(14.dp))
                    AvisosCategoriasCard(
                        lembretesDoCarroAtual = lembretesDoCarroAtual,
                        lembretesComBusca = lembretesComBusca,
                        buscaTexto = buscaTexto,
                        onBuscar = { buscaTexto = it },
                        listaContatos = listaContatos,
                        modeloCarro = carroAtual.nome,
                        filtroTipo = filtroTipo,
                        onFiltroTipoChange = { filtroTipo = it },
                        categoriasDisponiveis = categoriasDisponiveis,
                        iconOverrides = iconOverrides,
                        labelOverrides = labelOverrides,
                        onDelete = { lembrete ->
                            NotificacaoHelper.cancelarNotificacao(context.applicationContext, lembrete.id)
                            todosLembretes = todosLembretes.filter { it.id != lembrete.id }
                        },
                        onAddPrestador = { lembrete ->
                            lembreteParaVincularContato = lembrete.id
                            showAddContatoDialog = true
                        },
                        onOpenDetalhes = { lembrete ->
                            lembreteSelecionado = lembrete
                            contatoDetalheSelecionado = listaContatos.find { it.id == lembrete.contatoId }
                        },
                        statusLabel = { textoStatusPrazoLocal(it) },
                        statusColor = { tipo -> calcularCorStatusLocal(lembretesDoCarroAtual, tipo) },
                        textDim = textDim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    Spacer(Modifier.height(80.dp))

                }
                }
            }
        }
    }
}

// ----------------- COMPONENTES AUXILIARES LOCAIS -----------------

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val container = when {
        highlighted && isDark -> Color(0xFF3B2A0A)
        highlighted && !isDark -> Color(0xFFFEF3C7)
        isDark -> Color(0xFF111827)
        else -> Color(0xFFF1F5F9)
    }
    val borderColor = when {
        highlighted -> Color(0xFFFBBF24)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color(0xFFCBD5E1)
    }
    val iconTint = when {
        highlighted -> Color(0xFFF59E0B)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }
    val textColor = when {
        highlighted && isDark -> Color(0xFFFEF3C7)
        highlighted && !isDark -> Color(0xFF92400E)
        isDark -> Color(0xFFF1F5F9)
        else -> Color(0xFF0F172A)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.Black.copy(alpha = 0.18f) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
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
                        TipoIcon(
                            tipo = lembrete.tipo,
                            tint = statusColor,
                            size = 22.dp
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

fun calcularCorStatusLocal(lembretes: List<Lembrete>, tipo: TipoManutencao): Color {
    return when (tipo) {
        TipoManutencao.CORRENTE -> Color(0xFF22C55E) // verde
        TipoManutencao.LUBRIFICACAO -> Color(0xFF14B8A6) // verde-azulado
        TipoManutencao.PEDIVELA -> Color(0xFF0EA5E9) // azul
        TipoManutencao.ACESSORIOS -> Color(0xFFF97316) // laranja
        TipoManutencao.CONFORTO -> Color(0xFFEAB308) // amarelo
        TipoManutencao.PNEU -> Color(0xFFF59E0B) // laranja
        TipoManutencao.TRANSMISSAO -> Color(0xFF60A5FA) // azul claro
        TipoManutencao.REVISAO -> Color(0xFF8B5CF6) // roxo
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

private data class AbastecimentoResumo(
    val ultimo: AbastecimentoEntry?,
    val proximaData: LocalDate?,
    val diasAte: Long?,
    val mediaCustoDia: Double?,
    val custoSemana: Double?,
    val custoMes: Double?
)

private data class AbastecimentoEntry(
    val data: LocalDate,
    val litros: Double,
    val valorPago: Double
)

private fun calcularResumoAbastecimento(
    abastecimentos: List<Abastecimento>,
    formatter: DateTimeFormatter
): AbastecimentoResumo {
    val entries = abastecimentos.mapNotNull { item ->
        val data = runCatching { LocalDate.parse(item.data, formatter) }.getOrNull()
        if (data != null && item.litros > 0.0 && item.valorPago > 0.0) {
            AbastecimentoEntry(data, item.litros, item.valorPago)
        } else {
            null
        }
    }.sortedBy { it.data }

    if (entries.isEmpty()) {
        return AbastecimentoResumo(
            ultimo = null,
            proximaData = null,
            diasAte = null,
            mediaCustoDia = null,
            custoSemana = null,
            custoMes = null
        )
    }

    val diasEntreAbastecimentos = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.data, atual.data)
        if (dias <= 0) null else dias.toDouble()
    }

    val custoDiario = entries.windowed(2).mapNotNull { (anterior, atual) ->
        val dias = ChronoUnit.DAYS.between(anterior.data, atual.data)
        if (dias <= 0) null else atual.valorPago / dias.toDouble()
    }

    val mediaDiasBase = diasEntreAbastecimentos.takeIf { it.isNotEmpty() }?.average()
    val mediaCustoBase = custoDiario.takeIf { it.isNotEmpty() }?.average()
    val ultimo = entries.last()
    val fallbackDias = 7.0
    val mediaDias = mediaDiasBase ?: fallbackDias
    val mediaCusto = mediaCustoBase ?: (ultimo.valorPago / mediaDias)
    val diasAte = mediaDias.takeIf { it > 0.0 }?.let { ceil(it).toLong() }
    val proximaData = diasAte?.let { ultimo.data.plusDays(it) }

    return AbastecimentoResumo(
        ultimo = ultimo,
        proximaData = proximaData,
        diasAte = diasAte,
        mediaCustoDia = mediaCusto,
        custoSemana = mediaCusto?.times(7),
        custoMes = mediaCusto?.times(30)
    )
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
                            TipoIcon(
                                tipo = tipo,
                                tint = Color.White.copy(alpha = 0.7f),
                                size = iconSize,
                                textSize = 10.sp
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
                    TipoIcon(
                        tipo = tipo,
                        tint = Color.White.copy(alpha = 0.7f),
                        size = 12.dp,
                        textSize = 8.sp
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
    TipoManutencao.CORRENTE -> Color(0xFF22C55E)
    TipoManutencao.LUBRIFICACAO -> Color(0xFF14B8A6)
    TipoManutencao.PEDIVELA -> Color(0xFF0EA5E9)
    TipoManutencao.ACESSORIOS -> Color(0xFFF97316)
    TipoManutencao.CONFORTO -> Color(0xFFEAB308)
    TipoManutencao.PNEU -> Color(0xFFF59E0B)
    TipoManutencao.TRANSMISSAO -> Color(0xFF60A5FA)
    TipoManutencao.REVISAO -> Color(0xFF8B5CF6)
    TipoManutencao.OLEO -> Color(0xFF3B82F6) // azul
    TipoManutencao.BATERIA -> Color(0xFF16A34A) // verde
    TipoManutencao.MECANICA -> Color(0xFF60A5FA) // azul claro
    TipoManutencao.FREIO -> Color(0xFFDC2626) // vermelho
    TipoManutencao.LICENCIAMENTO -> Color(0xFF22C55E) // verde claro
    TipoManutencao.IPVA -> Color(0xFF5B8DEF) // azul leve
    TipoManutencao.SEGURO -> Color(0xFF10B981) // verde
    TipoManutencao.OUTROS -> Color(0xFF94A3B8)
}


