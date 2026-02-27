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
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import br.com.gui.carlembrete.VehicleIcon
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovoAgendamentoDialog(
    carroAtual: CarroInfo,
    contatosDisponiveis: List<ContatoProfissional>,
    onDismiss: () -> Unit,
    onConfirm: (Lembrete) -> Unit,
    onMultiConfirm: (List<Lembrete>) -> Unit,
    onUpdateKmCarro: (Int) -> Unit,
    autoAbrirCamera: Boolean = false,
    onAutoCameraConsumida: () -> Unit = {},
    onAddContato: (ContatoProfissional) -> Unit = {},
    initialTipo: TipoManutencao = TipoManutencao.OLEO,
    planTier: PlanTier,
    onRequestPremium: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val isPremium = planTier != PlanTier.FREE
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) scheme.background else Color.White
    val surfaceCardColor = if (isDark) Color(0xFF111827) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val iconColor = if (isDark) Color.White else Color.Black
    val cameraIconColor = Color.White
    var descricao by remember { mutableStateOf("") }
    var localServicoInput by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var kmBase by remember { mutableStateOf(if (carroAtual.kmAtual > 0) carroAtual.kmAtual.toString() else "") }
    var valorInput by remember { mutableStateOf("") }
    var tipoSelecionado by remember { mutableStateOf(initialTipo) }
    var contatosLista by remember { mutableStateOf(contatosDisponiveis) }
    var contatoSelecionado by remember { mutableStateOf<ContatoProfissional?>(null) }
    var listaItensDetectados by remember { mutableStateOf<List<ItemDetectado>>(emptyList()) }
    var isModoLista by remember { mutableStateOf(false) }
    var qrPossuiItensSeparaveis by remember { mutableStateOf(false) }
    var qrModoSeparado by remember { mutableStateOf(false) }
    var descricaoQrConsolidada by remember { mutableStateOf("") }
    var itemDataAvisoOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemHoraAvisoOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemValorOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showKmConfirmDialog by remember { mutableStateOf(false) }
    var kmDetectadoParaConfirmar by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var tipoMenuItemId by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var fotoCaminho by remember { mutableStateOf<String?>(null) }
    var horaNotificacao by remember { mutableStateOf("09:00") }
    var dataAviso by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var avisoPersonalizado by remember { mutableStateOf(false) }
    var etapaAtual by remember { mutableStateOf(1) }
    val descricaoFocusRequester = remember { FocusRequester() }
    var textosDetectados by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTextosDialog by remember { mutableStateOf(false) }
    var textoSelecionadoDialog by remember { mutableStateOf<String?>(null) }
    var showMarcaDialog by remember { mutableStateOf(false) }
    var produtoSelecionadoDialog by remember { mutableStateOf<String?>(null) }
    var marcaSelecionadaDialog by remember { mutableStateOf<String?>(null) }
    var isQrLoading by remember { mutableStateOf(false) }
    var qrNomeEstabelecimento by remember { mutableStateOf("") }
    var qrEnderecoEstabelecimento by remember { mutableStateOf("") }
    var descricaoAntesDialog by remember { mutableStateOf("") }
    var tipoAntesDialog by remember { mutableStateOf(TipoManutencao.OLEO) }
    var profissionaisDaCidade by remember { mutableStateOf<List<ProfissionalCidadeEncontrado>>(emptyList()) }
    var cidadeAtual by remember { mutableStateOf<String?>(null) }
    var ufAtual by remember { mutableStateOf<String?>(null) }
    var carregandoProfissionaisCidade by remember { mutableStateOf(false) }
    var erroProfissionaisCidade by remember { mutableStateOf<String?>(null) }
    var jaCarregouProfissionaisCidade by remember { mutableStateOf(false) }
    var profissionalParaCompletarTelefone by remember { mutableStateOf<ProfissionalCidadeEncontrado?>(null) }
    var telefoneCompletarInput by remember { mutableStateOf("") }
    val profissionaisListState = rememberLazyListState()
    val loadingTransition = rememberInfiniteTransition(label = "profissionais_loading")
    val loadingAlpha by loadingTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "profissionais_loading_alpha"
    )
    val isBikeVehicle = carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA || carroAtual.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
    val dataFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scope = rememberCoroutineScope()
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val textoReconhecido = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!textoReconhecido.isNullOrBlank()) {
                descricao = textoReconhecido
                tipoSelecionado = detectarTipoPeloTexto(textoReconhecido)
            }
        }
    }

    fun tentarAbrirCamera() {
        showCamera = true
    }

    LaunchedEffect(autoAbrirCamera) {
        if (autoAbrirCamera) {
            tentarAbrirCamera()
            onAutoCameraConsumida()
        }
    }

    LaunchedEffect(isModoLista, etapaAtual) {
        if (!isModoLista && etapaAtual == 1) {
            descricaoFocusRequester.requestFocus()
        }
    }

    fun adicionarKm(valor: Int) { val kmBaseInt = kmBase.toIntOrNull() ?: carroAtual.kmAtual; kmBase = (kmBaseInt + valor).toString() }
    LaunchedEffect(data, tipoSelecionado) {
        if (!avisoPersonalizado) {
            val dataBase = try { LocalDate.parse(data, dataFormatter) } catch (e: Exception) { LocalDate.now() }
            dataAviso = calcularProximaData(tipoSelecionado, dataBase)
        }
    }

    fun abrirDatePicker(dataAtual: String, aoSelecionar: (String) -> Unit) {
        val atual = try { LocalDate.parse(dataAtual, dataFormatter) } catch (e: Exception) { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                aoSelecionar("%02d/%02d/%04d".format(dayOfMonth, month + 1, year))
            },
            atual.year,
            atual.monthValue - 1,
            atual.dayOfMonth
        ).show()
    }

    fun abrirTimePicker(horaAtual: String, aoSelecionar: (String) -> Unit) {
        val partes = horaAtual.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 9
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hour, minute -> aoSelecionar("%02d:%02d".format(hour, minute)) },
            hora,
            minuto,
            true
        ).show()
    }

    fun iniciarCapturaVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Descreva o serviÃ§o realizado")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Recursos de voz indisponÃ­veis", Toast.LENGTH_SHORT).show()
        }
    }

    fun adicionarContatoDaCidade(profissional: ProfissionalCidadeEncontrado) {
        val telefoneLimpo = profissional.telefone.filter(Char::isDigit)
        if (telefoneLimpo.isBlank()) {
            Toast.makeText(context, "Complete o telefone antes de adicionar", Toast.LENGTH_SHORT).show()
            return
        }
        val existente = contatosLista.firstOrNull { atual ->
            val atualTelefone = atual.telefone.filter(Char::isDigit)
            (telefoneLimpo.isNotBlank() && atualTelefone == telefoneLimpo) ||
                atual.nome.trim().equals(profissional.nome.trim(), ignoreCase = true)
        }
        if (existente != null) {
            contatoSelecionado = existente
            Toast.makeText(context, "Profissional ja existe e foi vinculado", Toast.LENGTH_SHORT).show()
            return
        }
        val novoContato = ContatoProfissional(
            nome = profissional.nome,
            telefone = profissional.telefone,
            tipoServico = if (profissional.endereco.isBlank()) {
                "Profissional da cidade"
            } else {
                "Profissional da cidade | Endereco: ${profissional.endereco}"
            }
        )
        contatosLista = contatosLista + novoContato
        onAddContato(novoContato)
        contatoSelecionado = novoContato
        Toast.makeText(context, "Profissional da cidade adicionado", Toast.LENGTH_SHORT).show()
    }

    fun carregarProfissionaisDaCidade(forcar: Boolean = false) {
        if (carregandoProfissionaisCidade) {
            Log.d(PROF_CITY_TAG, "Busca ignorada: carregamento ja em andamento.")
            return
        }
        if (!forcar && jaCarregouProfissionaisCidade) {
            Log.d(PROF_CITY_TAG, "Busca ignorada: dados da cidade ja carregados.")
            return
        }
        Log.d(PROF_CITY_TAG, "Iniciando busca de profissionais da cidade. forcar=$forcar tipo=$tipoSelecionado")
        scope.launch {
            carregandoProfissionaisCidade = true
            erroProfissionaisCidade = null
            val resultado = withContext(Dispatchers.IO) {
                buscarProfissionaisDaCidadeAtual(context, tipoSelecionado, isBikeVehicle)
            }
            carregandoProfissionaisCidade = false
            resultado.onSuccess { busca ->
                cidadeAtual = busca.cidade
                ufAtual = busca.estado
                profissionaisDaCidade = busca.profissionais
                jaCarregouProfissionaisCidade = true
                Log.i(
                    PROF_CITY_TAG,
                    "Busca concluida com sucesso. cidade=${busca.cidade} uf=${busca.estado} total=${busca.profissionais.size}"
                )
                if (busca.profissionais.isEmpty()) {
                    erroProfissionaisCidade = "Nenhum profissional encontrado na sua cidade."
                }
            }.onFailure { erro ->
                profissionaisDaCidade = emptyList()
                erroProfissionaisCidade = erro.message ?: "Nao foi possivel buscar profissionais da cidade."
                jaCarregouProfissionaisCidade = false
                Log.e(PROF_CITY_TAG, "Falha na busca de profissionais da cidade.", erro)
            }
        }
    }

    LaunchedEffect(etapaAtual) {
        if (etapaAtual == 3) {
            carregarProfissionaisDaCidade()
        }
    }

    if (profissionalParaCompletarTelefone != null) {
        AlertDialog(
            onDismissRequest = { profissionalParaCompletarTelefone = null },
            title = { Text("Completar telefone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profissionalParaCompletarTelefone!!.nome, color = textPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = telefoneCompletarInput,
                        onValueChange = { telefoneCompletarInput = it },
                        label = { Text("Telefone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val telefoneLimpo = telefoneCompletarInput.filter(Char::isDigit)
                        if (telefoneLimpo.isBlank()) {
                            Toast.makeText(context, "Informe um telefone valido", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val base = profissionalParaCompletarTelefone ?: return@TextButton
                        adicionarContatoDaCidade(base.copy(telefone = telefoneLimpo))
                        profissionalParaCompletarTelefone = null
                        telefoneCompletarInput = ""
                    }
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    profissionalParaCompletarTelefone = null
                    telefoneCompletarInput = ""
                }) { Text("Cancelar") }
            }
        )
    }

    fun enviarMensagemWhatsapp(contato: ContatoProfissional) {
        val telefone = contato.telefone.filter(Char::isDigit)
        if (telefone.isBlank()) {
            Toast.makeText(context, "Telefone invÃ¡lido", Toast.LENGTH_SHORT).show()
            return
        }
        val kmInfo = kmBase.ifBlank { "nÃ£o informado" }
        val mensagem = "OlÃ¡ ${contato.nome}, a Ãºltima manutenÃ§Ã£o foi registrada em $data com $kmInfo km. Podemos agendar a prÃ³xima?"
        val uri = Uri.parse("https://wa.me/$telefone?text=${URLEncoder.encode(mensagem, "UTF-8")}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "NÃ£o foi possÃ­vel abrir o WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirBuscaGoogleProfissional(nome: String) {
        val local = listOfNotNull(cidadeAtual, ufAtual).joinToString(" ")
        val query = listOf(nome, local).filter { it.isNotBlank() }.joinToString(" ")
        val uri = Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(context, "NÃƒÂ£o foi possÃƒÂ­vel abrir o Google", Toast.LENGTH_SHORT).show()
        }
    }

    fun salvarAvisos() {
        val kmAtualBase = kmBase.toIntOrNull() ?: 0
        if (kmAtualBase > carroAtual.kmAtual) onUpdateKmCarro(kmAtualBase)
        val dataAvisoStr = dataAviso
        if (isModoLista) {
            val novosLembretes = listaItensDetectados.flatMap { item ->
                val rep = maxOf(1, item.quantidade)
                val kmFuturo = (kmAtualBase + getKmAdicionalPorTipo(item.tipo)).toString()
                val dataItem = itemDataAvisoOverrides[item.id] ?: dataAvisoStr
                val horaItem = itemHoraAvisoOverrides[item.id] ?: horaNotificacao
                val valorItem = itemValorOverrides[item.id]?.toDoubleOrNull() ?: item.valor
                (1..rep).map { indice ->
                    val tituloFormatado = if (rep > 1) "${item.nome} (${indice}/$rep)" else item.nome
                    Lembrete(
                        titulo = tituloFormatado,
                        peca = item.nome,
                        dataLimite = dataItem,
                        kmLimite = kmFuturo,
                        tipo = item.tipo,
                        valor = valorItem,
                        carroId = "",
                        contatoId = contatoSelecionado?.id,
                        fotoPath = fotoCaminho,
                        horaAviso = horaItem,
                        estabelecimentoNome = qrNomeEstabelecimento,
                        estabelecimentoEndereco = qrEnderecoEstabelecimento
                    )
                }
            }
            novosLembretes.forEach { NotificacaoHelper.agendarNotificacao(appContext, it, it.horaAviso) }
            onMultiConfirm(novosLembretes)
        } else if (descricao.isNotBlank()) {
            val tituloLembrete = localServicoInput.ifBlank { qrNomeEstabelecimento.ifBlank { descricao } }
            val novoLembrete = Lembrete(
                titulo = tituloLembrete,
                peca = descricao.trim(),
                dataLimite = dataAvisoStr,
                kmLimite = (kmAtualBase + getKmAdicionalPorTipo(tipoSelecionado)).toString(),
                tipo = tipoSelecionado,
                valor = valorInput.toDoubleOrNull() ?: 0.0,
                carroId = "",
                contatoId = contatoSelecionado?.id,
                fotoPath = fotoCaminho,
                horaAviso = horaNotificacao,
                estabelecimentoNome = qrNomeEstabelecimento,
                estabelecimentoEndereco = qrEnderecoEstabelecimento
            )
            NotificacaoHelper.agendarNotificacao(appContext, novoLembrete, horaNotificacao)
            onConfirm(novoLembrete)
        }
    }

    if (showCamera) {
        CameraCapturaDialog(onDismiss = { showCamera = false }, onFotoCapturada = { resultado ->
            if (!isPremium) {
                AppPreferences.incrementOcrCount(context)
            }
            fotoCaminho = resultado.arquivoFoto.absolutePath
            val qrUrl = resultado.qrCodeUrl?.trim()
            Log.i(QR_PARSER_TAG, "Camera retorno => qrUrl=$qrUrl foto=${resultado.arquivoFoto.name}")
            if (!qrUrl.isNullOrBlank()) {
                isModoLista = false
                textosDetectados = emptyList()
                showTextosDialog = false
                showMarcaDialog = false
                showCamera = false
                isQrLoading = true
                scope.launch {
                    val notaInfo = consultarNotaPorQrCode(qrUrl)
                    isQrLoading = false
                    if (notaInfo == null) {
                        qrNomeEstabelecimento = ""
                        qrEnderecoEstabelecimento = ""
                        localServicoInput = ""
                        qrPossuiItensSeparaveis = false
                        qrModoSeparado = false
                        descricaoQrConsolidada = ""
                        itemDataAvisoOverrides = emptyMap()
                        itemHoraAvisoOverrides = emptyMap()
                        itemValorOverrides = emptyMap()
                        Toast.makeText(context, "QR lido, mas nÃ£o foi possÃ­vel carregar a nota.", Toast.LENGTH_LONG).show()
                    } else {
                        val valorExtraido = notaInfo.valorTotal
                        val dataExtraida = notaInfo.dataCompra

                        if (valorExtraido != null) {
                            valorInput = String.format(Locale.US, "%.2f", valorExtraido)
                        }
                        if (!dataExtraida.isNullOrBlank()) {
                            data = dataExtraida
                            dataAviso = dataExtraida
                            avisoPersonalizado = true
                        }
                        qrNomeEstabelecimento = notaInfo.nomeEstabelecimento.orEmpty()
                        qrEnderecoEstabelecimento = notaInfo.enderecoEstabelecimento.orEmpty()
                        val descricaoExtraida = notaInfo.descricaoItens?.trim()
                        val itensQr = extrairItensDaDescricaoQr(descricaoExtraida)
                        qrPossuiItensSeparaveis = itensQr.size > 1
                        qrModoSeparado = false
                        if (itensQr.isNotEmpty()) {
                            listaItensDetectados = itensQr
                            itemDataAvisoOverrides = itensQr.associate { it.id to dataAviso }
                            itemHoraAvisoOverrides = itensQr.associate { it.id to horaNotificacao }
                            itemValorOverrides = itensQr.associate { it.id to String.format(Locale.US, "%.2f", it.valor) }
                        } else {
                            itemDataAvisoOverrides = emptyMap()
                            itemHoraAvisoOverrides = emptyMap()
                            itemValorOverrides = emptyMap()
                        }
                        localServicoInput = montarLocalNota(
                            estabelecimento = qrNomeEstabelecimento,
                            endereco = qrEnderecoEstabelecimento
                        )
                        descricaoQrConsolidada = montarDescricaoItensNota(
                            total = valorExtraido,
                            itens = descricaoExtraida
                        )
                        descricao = descricaoQrConsolidada
                        isModoLista = false

                        Log.i(
                            QR_PARSER_TAG,
                            "Bind UI QR => estabelecimento=$qrNomeEstabelecimento endereco=$qrEnderecoEstabelecimento descricao=$descricao valorInput=$valorInput data=$data dataAviso=$dataAviso"
                        )
                        Toast.makeText(context, "Dados da nota carregados pelo QR Code.", Toast.LENGTH_SHORT).show()
                    }
                }
                return@CameraCapturaDialog
            }
            Log.w(QR_PARSER_TAG, "QR nao detectado na captura. Seguindo fluxo de fallback da foto.")
            qrNomeEstabelecimento = ""
            qrEnderecoEstabelecimento = ""
            localServicoInput = ""
            qrPossuiItensSeparaveis = false
            qrModoSeparado = false
            descricaoQrConsolidada = ""
            itemDataAvisoOverrides = emptyMap()
            itemHoraAvisoOverrides = emptyMap()
            itemValorOverrides = emptyMap()
            textosDetectados = filtrarTextosDetectados(resultado.linhasReconhecidas)
            textoSelecionadoDialog = null
            showMarcaDialog = false
            produtoSelecionadoDialog = null
            marcaSelecionadaDialog = null
            if (resultado.itensEncontrados.isNotEmpty()) {
                listaItensDetectados = resultado.itensEncontrados
                isModoLista = true
            } else {
                isModoLista = false
                val principal = resultado.sugestoesProduto.firstOrNull()
                if (!principal.isNullOrBlank()) {
                    descricao = principal
                    tipoSelecionado = detectarTipoPeloTexto(principal)
                } else {
                    descricao = "Produto (Foto Anexada)"
                }
            }
            descricaoAntesDialog = descricao
            tipoAntesDialog = tipoSelecionado
            showTextosDialog = textosDetectados.isNotEmpty() && !isModoLista
            if (resultado.kmDetectado != null && resultado.kmDetectado > 0) { kmDetectadoParaConfirmar = resultado.kmDetectado; showKmConfirmDialog = true }
            showCamera = false
        })
    }

    if (isQrLoading) {
        val loadingDialogBg = if (isDark) Color(0xFF0F172A) else Color.White
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Lendo nota", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando dados no link do QR Code...", color = textSecondary)
                }
            },
            confirmButton = {},
            containerColor = loadingDialogBg
        )
    }

    if (showKmConfirmDialog) {
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showKmConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Atualizar KM?", color = textPrimary) },
            text = {
                Text(
                    "Detectamos ${kmDetectadoParaConfirmar} km na captura.\nAtualizar o odÃ´metro do carro?",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateKmCarro(kmDetectadoParaConfirmar)
                        kmBase = kmDetectadoParaConfirmar.toString()
                        showKmConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) { Text("Sim") }
            },
            dismissButton = {
                TextButton(onClick = { showKmConfirmDialog = false }) { Text("NÃ£o") }
            }
        )
    }

    if (showTextosDialog) {
        val itensDialogo = textosDetectados
            .distinct()
            .filter { it.isNotBlank() }
            .take(8)
        val jaSelecionou = textoSelecionadoDialog != null
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showTextosDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Qual e o Produto?", color = textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showTextosDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = iconColor)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itensDialogo.isEmpty()) {
                        Text("Nenhum texto identificado na captura.", color = Color(0xFF94A3B8))
                    } else {
                        itensDialogo.forEach { texto ->
                            val isSelected = textoSelecionadoDialog == texto
                            val disabled = jaSelecionou && !isSelected
                            val cardShape = RoundedCornerShape(12.dp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (disabled) 0.35f else 1f)
                                    .clip(cardShape)
                                    .clickable(
                                        enabled = !disabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        textoSelecionadoDialog = texto
                                        descricao = texto
                                        tipoSelecionado = detectarTipoPeloTexto(texto)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF111827)
                                ),
                                shape = cardShape,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF334155)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        texto,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTextosDialog = false
                        produtoSelecionadoDialog = textoSelecionadoDialog
                        marcaSelecionadaDialog = null
                        textoSelecionadoDialog = null
                        showMarcaDialog = true
                    },
                    enabled = textoSelecionadoDialog != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Proximo", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {}
        )
    }

    if (showMarcaDialog) {
        val produtoSelecionado = produtoSelecionadoDialog
        val itensDialogo = textosDetectados
            .distinct()
            .filter { it.isNotBlank() }
            .filter { it != produtoSelecionado }
            .take(8)
        val jaSelecionou = marcaSelecionadaDialog != null
        AlertDialog(
            modifier = Modifier.border(dialogBorderStroke, dialogCornerShape),
            shape = dialogCornerShape,
            onDismissRequest = { showMarcaDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Qual e a Marca do Produto?", color = textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { showMarcaDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = iconColor)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itensDialogo.isEmpty()) {
                        Text("Nenhuma marca diferente foi identificada.", color = Color(0xFF94A3B8))
                    } else {
                        itensDialogo.forEach { texto ->
                            val isSelected = marcaSelecionadaDialog == texto
                            val disabled = jaSelecionou && !isSelected
                            val cardShape = RoundedCornerShape(12.dp)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (disabled) 0.35f else 1f)
                                    .clip(cardShape)
                                    .clickable(
                                        enabled = !disabled,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        marcaSelecionadaDialog = texto
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF111827)
                                ),
                                shape = cardShape,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF334155)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        texto,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val produto = produtoSelecionadoDialog
                        val marca = marcaSelecionadaDialog
                        if (!produto.isNullOrBlank() && !marca.isNullOrBlank()) {
                            descricao = "$produto - $marca"
                        }
                        showMarcaDialog = false
                        produtoSelecionadoDialog = null
                        marcaSelecionadaDialog = null
                    },
                    enabled = marcaSelecionadaDialog != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { Text("Concluir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {}
        )
    }

    val podeAvancarEtapa1 = (isModoLista && listaItensDetectados.isNotEmpty()) || descricao.isNotBlank()
    val accentBlue = Color(0xFF3B82F6)
    val voltarUmaEtapaOuFechar = {
        if (etapaAtual > 1) etapaAtual -= 1 else onDismiss()
    }

    BackHandler(onBack = voltarUmaEtapaOuFechar)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = voltarUmaEtapaOuFechar) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = iconColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageBackground
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (etapaAtual) {
                    1 -> {
                        Button(
                            onClick = { etapaAtual = 2 },
                            enabled = podeAvancarEtapa1,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Avançar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    2 -> {
                        Button(
                            onClick = { etapaAtual = 3 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Próximo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    3 -> {
                        Button(
                            onClick = { etapaAtual = 4 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Próximo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    else -> {
                        Button(
                            onClick = {
                                salvarAvisos()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cadastrar aviso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (etapaAtual == 3) Modifier
                        else Modifier.verticalScroll(rememberScrollState())
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                when (etapaAtual) {
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = accentBlue, modifier = Modifier.size(30.dp))
                            Text("Dados do aviso", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        }
                        Button(
                            onClick = { tentarAbrirCamera() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (fotoCaminho != null) Color(0xFF10B981) else Color(0xFF3B82F6),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                if (fotoCaminho != null) Icons.Default.Check else Icons.Default.CameraAlt,
                                null,
                                tint = cameraIconColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (fotoCaminho != null) "Foto Anexada (Refazer)" else "Escanear Produto", color = Color.White)
                        }
                        OutlinedTextField(
                            value = descricao,
                            onValueChange = { descricao = it },
                            label = { Text("Produtos (total e itens)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .animateContentSize()
                                .focusRequester(descricaoFocusRequester),
                            maxLines = 12,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = !menuExpanded }) {
                            OutlinedTextField(
                                value = tipoSelecionado.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoria") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                leadingIcon = {
                                    TipoIcon(
                                        tipo = tipoSelecionado,
                                        tint = corCategoria(tipoSelecionado),
                                        size = 18.dp
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                TipoManutencao.values().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.label) },
                                        onClick = { tipoSelecionado = t; menuExpanded = false },
                                        leadingIcon = {
                                            TipoIcon(
                                                tipo = t,
                                                tint = corCategoria(t),
                                                size = 18.dp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = accentBlue, modifier = Modifier.size(30.dp))
                            Text("Quilometragem e data", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = kmBase,
                                onValueChange = { if (it.all(Char::isDigit)) kmBase = it },
                                label = { Text("KM Atual") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = data,
                                onValueChange = {},
                                label = { Text("Data do servico") },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        abrirDatePicker(data) {
                                            data = it
                                            avisoPersonalizado = false
                                        }
                                    }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        OutlinedTextField(
                            value = dataAviso,
                            onValueChange = {},
                            label = { Text("Data do aviso") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    abrirDatePicker(dataAviso) {
                                        dataAviso = it
                                        avisoPersonalizado = true
                                    }
                                }) {
                                    Icon(Icons.Default.Event, contentDescription = null)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = valorInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) valorInput = it },
                            label = { Text("Valor Total (R$)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = horaNotificacao,
                            onValueChange = {},
                            label = { Text("Hora do aviso") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    abrirTimePicker(horaNotificacao) { selecionada ->
                                        horaNotificacao = selecionada
                                    }
                                }) {
                                    Icon(Icons.Default.Schedule, contentDescription = null)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    3 -> {
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
                            onRecarregar = { carregarProfissionaisDaCidade(forcar = true) },
                            onVerNoGoogle = { abrirBuscaGoogleProfissional(it) },
                            onAdicionarDaCidade = { contatoCidade ->
                                if (contatoCidade.telefone.isBlank()) {
                                    profissionalParaCompletarTelefone = contatoCidade
                                    telefoneCompletarInput = ""
                                } else {
                                    adicionarContatoDaCidade(contatoCidade)
                                }
                            }
                        )
                    }
                    else -> {
                        EtapaRevisaoAvisoContent(
                            isDark = isDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentBlue = accentBlue,
                            descricao = descricao,
                            tipoSelecionado = tipoSelecionado,
                            isModoLista = isModoLista,
                            listaItensDetectados = listaItensDetectados,
                            kmBase = kmBase,
                            data = data,
                            dataAviso = dataAviso,
                            horaNotificacao = horaNotificacao,
                            valorInput = valorInput,
                            contatoSelecionado = contatoSelecionado,
                            cidadeAtual = cidadeAtual,
                            ufAtual = ufAtual
                        )
                    }
                }
            }
            }
        }
        }
    }
}

private const val PROF_CITY_TAG = "ProfCidade"

private data class BuscaProfissionaisCidadeResultado(
    val cidade: String?,
    val estado: String?,
    val profissionais: List<ProfissionalCidadeEncontrado>
)

data class ProfissionalCidadeEncontrado(
    val nome: String,
    val telefone: String,
    val endereco: String
)

private data class LocalizacaoCidade(
    val latitude: Double,
    val longitude: Double,
    val cidade: String?,
    val estado: String?
)

private fun buscarProfissionaisDaCidadeAtual(
    context: Context,
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): Result<BuscaProfissionaisCidadeResultado> = runCatching {
    Log.d(PROF_CITY_TAG, "buscaProfissionaisDaCidadeAtual: resolvendo localizacao.")
    val localizacao = obterCidadePelaLocalizacao(context)
        ?: throw IllegalStateException("Ative a localizacao para buscar profissionais da sua cidade.")
    Log.d(
        PROF_CITY_TAG,
        "Localizacao obtida: lat=${localizacao.latitude} lng=${localizacao.longitude} cidade=${localizacao.cidade} uf=${localizacao.estado}"
    )
    val profissionais = buscarProfissionaisOverpass(
        context = context,
        latitude = localizacao.latitude,
        longitude = localizacao.longitude,
        tipoSelecionado = tipoSelecionado,
        cidade = localizacao.cidade,
        estado = localizacao.estado,
        isBikeVehicle = isBikeVehicle
    )
    BuscaProfissionaisCidadeResultado(
        cidade = localizacao.cidade,
        estado = localizacao.estado,
        profissionais = profissionais
    )
}

private fun obterCidadePelaLocalizacao(context: Context): LocalizacaoCidade? {
    val permissaoFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!permissaoFine) {
        Log.w(PROF_CITY_TAG, "Permissao ACCESS_FINE_LOCATION nao concedida.")
        return null
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    var melhorLocal: Location? = null

    providers.forEach { provider ->
        val local = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: return@forEach
        if (melhorLocal == null || local.accuracy < melhorLocal!!.accuracy || local.time > melhorLocal!!.time) {
            melhorLocal = local
        }
    }

    val atual = melhorLocal ?: run {
        Log.w(PROF_CITY_TAG, "Nenhuma localizacao conhecida disponivel (GPS/NETWORK).")
        return null
    }
    var cidade: String? = null
    var estado: String? = null
    runCatching {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        @Suppress("DEPRECATION")
        val endereco = geocoder.getFromLocation(atual.latitude, atual.longitude, 1)?.firstOrNull()
        cidade = endereco?.subAdminArea ?: endereco?.locality
        estado = endereco?.adminArea
    }.onFailure { erro ->
        Log.w(PROF_CITY_TAG, "Falha ao resolver cidade/UF com Geocoder.", erro)
    }

    return LocalizacaoCidade(
        latitude = atual.latitude,
        longitude = atual.longitude,
        cidade = cidade,
        estado = estado
    )
}

private fun buscarProfissionaisOverpass(
    context: Context,
    latitude: Double,
    longitude: Double,
    tipoSelecionado: TipoManutencao,
    cidade: String?,
    estado: String?,
    isBikeVehicle: Boolean
): List<ProfissionalCidadeEncontrado> {
    Log.d(PROF_CITY_TAG, "Consultando Overpass. lat=$latitude lng=$longitude tipo=$tipoSelecionado cidade=$cidade uf=$estado isBikeVehicle=$isBikeVehicle")
    val payloadCidade = cidade
        ?.takeIf { it.isNotBlank() }
        ?.let { executarConsultaOverpass(montarOverpassCityAreaQuery(it, estado, tipoSelecionado, isBikeVehicle)) }
    val payload = if (!payloadCidade.isNullOrBlank() && contarElementosOverpass(payloadCidade) > 0) {
        payloadCidade
    } else {
        if (!payloadCidade.isNullOrBlank()) {
            Log.w(PROF_CITY_TAG, "Consulta por limite da cidade retornou zero elementos. Aplicando fallback por raio.")
        }
        executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 15000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
            ?: executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 50000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
            ?: executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 120000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
            ?: executarConsultaOverpass(montarOverpassQuery(latitude, longitude, raioMetros = 250000, tipoSelecionado = tipoSelecionado, isBikeVehicle = isBikeVehicle))
    }
        ?: throw IllegalStateException("Servidor de busca ocupado no momento. Toque em Atualizar em alguns segundos.")
    val root = JSONObject(payload)
    val elements = root.optJSONArray("elements") ?: return emptyList()
    Log.d(PROF_CITY_TAG, "Overpass retornou elements=${elements.length()}")

    val contatos = mutableListOf<ProfissionalCidadeEncontrado>()
    var semTelefone = 0
    var semEnderecoPreenchido = 0
    for (i in 0 until elements.length()) {
        val element = elements.optJSONObject(i) ?: continue
        val tags = element.optJSONObject("tags") ?: continue
        val nome = tags.optString("name").trim()
        if (nome.isBlank()) continue
        val telefone = listOf(
            "contact:phone",
            "phone",
            "mobile",
            "telephone",
            "contact:mobile",
            "contact:whatsapp",
            "whatsapp"
        ).mapNotNull { chave ->
            tags.optString(chave).trim().takeIf { it.isNotBlank() }
        }.firstOrNull().orEmpty()
        if (telefone.isBlank()) semTelefone += 1
        val endereco = montarEnderecoProfissional(context, element, tags)
        if (endereco.isBlank()) semEnderecoPreenchido += 1
        val candidato = ProfissionalCidadeEncontrado(
            nome = nome,
            telefone = telefone,
            endereco = endereco
        )
        contatos += candidato
    }

    val resultado = contatos.distinctBy { contato ->
        val telefone = contato.telefone.filter(Char::isDigit)
        if (telefone.isNotBlank()) telefone else "${contato.nome.lowercase(Locale.ROOT)}|${contato.endereco.lowercase(Locale.ROOT)}"
    }.sortedBy { it.nome.lowercase(Locale.ROOT) }
    Log.i(
        PROF_CITY_TAG,
        "Profissionais validos apos filtros: total=${resultado.size} semTelefone=$semTelefone semEndereco=$semEnderecoPreenchido"
    )
    return resultado
}

private fun contarElementosOverpass(payload: String): Int = runCatching {
    JSONObject(payload).optJSONArray("elements")?.length() ?: 0
}.getOrDefault(0)

private fun montarOverpassQuery(
    latitude: Double,
    longitude: Double,
    raioMetros: Int,
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): String {
    val tags = tagsBuscaPorContexto(tipoSelecionado, isBikeVehicle)
    val blocos = buildString {
        tags.forEach { (chave, valor) ->
            append("  node(around:$raioMetros,$latitude,$longitude)[\"$chave\"=\"$valor\"];\n")
            append("  way(around:$raioMetros,$latitude,$longitude)[\"$chave\"=\"$valor\"];\n")
            append("  relation(around:$raioMetros,$latitude,$longitude)[\"$chave\"=\"$valor\"];\n")
        }
    }
    return """
        [out:json][timeout:20];
        (
$blocos
        );
        out center tags 180;
    """.trimIndent()
}

private fun montarOverpassCityAreaQuery(
    cidade: String,
    estado: String?,
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): String {
    val cidadeEscapada = cidade.replace("\"", "\\\"")
    val estadoEscapado = estado?.replace("\"", "\\\"")
    val filtroEstado = estadoEscapado?.takeIf { it.isNotBlank() }?.let { """["is_in:state"="$it"]""" } ?: ""
    val tags = tagsBuscaPorContexto(tipoSelecionado, isBikeVehicle)
    val blocos = buildString {
        tags.forEach { (chave, valor) ->
            append("  node(area.searchArea)[\"$chave\"=\"$valor\"];\n")
            append("  way(area.searchArea)[\"$chave\"=\"$valor\"];\n")
            append("  relation(area.searchArea)[\"$chave\"=\"$valor\"];\n")
        }
    }
    return """
        [out:json][timeout:25];
        area["name"="$cidadeEscapada"]["boundary"="administrative"]["admin_level"="8"]$filtroEstado->.searchArea;
        (
$blocos
        );
        out center tags 400;
    """.trimIndent()
}

private fun tagsBuscaPorContexto(
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean
): List<Pair<String, String>> {
    if (isBikeVehicle) {
        return listOf(
            "shop" to "bicycle",
            "service:bicycle:repair" to "yes"
        )
    }
    return if (tipoSelecionado == TipoManutencao.PNEU) {
        listOf(
            "shop" to "tyres",
            "shop" to "car_repair",
            "amenity" to "car_repair"
        )
    } else {
        listOf(
            "shop" to "car_repair",
            "amenity" to "car_repair",
            "craft" to "mechanic",
            "service:vehicle:repair" to "yes"
        )
    }
}

private fun executarConsultaOverpass(query: String): String? {
    val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://z.overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )
    endpoints.forEach { endpointUrl ->
        Log.d(PROF_CITY_TAG, "Tentando endpoint Overpass: $endpointUrl")
        val resultado = runCatching {
            val endpoint = URL(endpointUrl)
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            conn.outputStream.use { out ->
                out.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            Log.d(PROF_CITY_TAG, "Endpoint $endpointUrl respondeu HTTP $code")
            if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        }
        val payload = resultado.getOrNull()
        if (!payload.isNullOrBlank()) {
            Log.d(PROF_CITY_TAG, "Endpoint $endpointUrl retornou payload valido.")
            return payload
        }
        val erro = resultado.exceptionOrNull()
        if (erro != null) {
            Log.w(PROF_CITY_TAG, "Falha ao consultar endpoint $endpointUrl", erro)
        }
    }
    Log.e(PROF_CITY_TAG, "Nenhum endpoint Overpass retornou dados validos.")
    return null
}

private fun montarEnderecoProfissional(
    context: Context,
    element: JSONObject,
    tags: JSONObject
): String {
    val enderecoCompleto = tags.optString("addr:full").trim()
    if (enderecoCompleto.isNotBlank()) return enderecoCompleto

    val rua = tags.optString("addr:street").trim()
    val numero = tags.optString("addr:housenumber").trim()
    val bairro = tags.optString("addr:suburb").trim()
    val cidade = tags.optString("addr:city").trim()
    val estado = tags.optString("addr:state").trim()

    val linhaRua = listOf(rua, numero).filter { it.isNotBlank() }.joinToString(", ")
    val linhaLocal = listOf(bairro, cidade, estado).filter { it.isNotBlank() }.joinToString(" - ")
    val enderecoTags = listOf(linhaRua, linhaLocal).filter { it.isNotBlank() }.joinToString(" | ")
    if (enderecoTags.isNotBlank()) return enderecoTags

    val coordenadas = extrairLatLngDoElemento(element) ?: return ""
    return enderecoPorGeocoder(context, coordenadas.first, coordenadas.second)
}

private fun extrairLatLngDoElemento(element: JSONObject): Pair<Double, Double>? {
    val latDireto = element.optDouble("lat", Double.NaN)
    val lonDireto = element.optDouble("lon", Double.NaN)
    if (!latDireto.isNaN() && !lonDireto.isNaN()) return latDireto to lonDireto

    val center = element.optJSONObject("center") ?: return null
    val latCenter = center.optDouble("lat", Double.NaN)
    val lonCenter = center.optDouble("lon", Double.NaN)
    if (latCenter.isNaN() || lonCenter.isNaN()) return null
    return latCenter to lonCenter
}

private fun enderecoPorGeocoder(context: Context, latitude: Double, longitude: Double): String {
    return runCatching {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        @Suppress("DEPRECATION")
        val endereco = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        listOfNotNull(
            endereco?.thoroughfare?.takeIf { it.isNotBlank() },
            endereco?.subThoroughfare?.takeIf { it.isNotBlank() }
        ).joinToString(", ").ifBlank {
            listOfNotNull(
                endereco?.subLocality?.takeIf { it.isNotBlank() },
                endereco?.locality?.takeIf { it.isNotBlank() },
                endereco?.adminArea?.takeIf { it.isNotBlank() }
            ).joinToString(" - ")
        }
    }.onFailure { erro ->
        Log.w(PROF_CITY_TAG, "Falha no geocoder reverso para lat=$latitude lon=$longitude", erro)
    }.getOrNull().orEmpty()
}






