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
    var novoContatoNome by remember { mutableStateOf("") }
    var novoContatoTelefone by remember { mutableStateOf("") }
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Descreva o serviço realizado")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Recursos de voz indisponíveis", Toast.LENGTH_SHORT).show()
        }
    }

fun adicionarContatoManual() {
        val nome = novoContatoNome.trim()
        val telefone = novoContatoTelefone.filter(Char::isDigit)
        if (nome.isBlank() || telefone.isBlank()) {
            Toast.makeText(context, "Informe nome e telefone", Toast.LENGTH_SHORT).show()
            return
        }
        val novoContato = ContatoProfissional(
            nome = nome,
            telefone = telefone,
            tipoServico = "Contato manual"
        )
        contatosLista = contatosLista + novoContato
        onAddContato(novoContato)
        contatoSelecionado = novoContato
        novoContatoNome = ""
        novoContatoTelefone = ""
        Toast.makeText(context, "Profissional adicionado", Toast.LENGTH_SHORT).show()
    }

    fun enviarMensagemWhatsapp(contato: ContatoProfissional) {
        val telefone = contato.telefone.filter(Char::isDigit)
        if (telefone.isBlank()) {
            Toast.makeText(context, "Telefone inválido", Toast.LENGTH_SHORT).show()
            return
        }
        val kmInfo = kmBase.ifBlank { "não informado" }
        val mensagem = "Olá ${contato.nome}, a última manutenção foi registrada em $data com $kmInfo km. Podemos agendar a próxima?"
        val uri = Uri.parse("https://wa.me/$telefone?text=${URLEncoder.encode(mensagem, "UTF-8")}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "QR lido, mas não foi possível carregar a nota.", Toast.LENGTH_LONG).show()
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
                    "Detectamos ${kmDetectadoParaConfirmar} km na captura.\nAtualizar o odômetro do carro?",
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
                TextButton(onClick = { showKmConfirmDialog = false }) { Text("Não") }
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
                title = {
                    Text("Criar novo Aviso", color = textPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = voltarUmaEtapaOuFechar) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = iconColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageBackground
                )
            )
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = surfaceCardColor),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                when (etapaAtual) {
                    1 -> {
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

                        if (qrPossuiItensSeparaveis) {
                            val splitCardBg = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
                            val splitCardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f)
                            val splitIdleButton = if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0)
                            val splitAllInOneSelected = Color(0xFF2563EB)
                            val splitSeparateSelected = Color(0xFF059669)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = splitCardBg),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, splitCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Como deseja criar os avisos?", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                qrModoSeparado = false
                                                isModoLista = false
                                                if (descricaoQrConsolidada.isNotBlank()) descricao = descricaoQrConsolidada
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!qrModoSeparado) splitAllInOneSelected else splitIdleButton,
                                                contentColor = if (!qrModoSeparado) Color.White else textPrimary
                                            ),
                                            border = BorderStroke(1.dp, splitCardBorder),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Tudo em 1") }
                                        Button(
                                            onClick = {
                                                qrModoSeparado = true
                                                isModoLista = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (qrModoSeparado) splitSeparateSelected else splitIdleButton,
                                                contentColor = if (qrModoSeparado) Color.White else textPrimary
                                            ),
                                            border = BorderStroke(1.dp, splitCardBorder),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Separar Avisos") }
                                    }
                                }
                            }
                        }

                        if (isModoLista) {
                            val splitPreviewItemBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            val splitPreviewChipBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            val splitPreviewChipText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            val splitPreviewItemBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f)
                            Text("Previa dos avisos separados", color = textPrimary, fontWeight = FontWeight.SemiBold)
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(listaItensDetectados) { item ->
                                    val kmAtualBase = kmBase.toIntOrNull() ?: 0
                                    val kmFuturoCalculado = if (kmAtualBase > 0) (kmAtualBase + getKmAdicionalPorTipo(item.tipo)).toString() else ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(splitPreviewItemBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, splitPreviewItemBorder, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TipoIcon(
                                            tipo = item.tipo,
                                            tint = corCategoria(item.tipo),
                                            size = 20.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.nome, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ExposedDropdownMenuBox(
                                                    expanded = tipoMenuItemId == item.id,
                                                    onExpandedChange = { expanded ->
                                                        tipoMenuItemId = if (expanded) item.id else null
                                                    }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .menuAnchor()
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(splitPreviewChipBg)
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            TipoIcon(
                                                                tipo = item.tipo,
                                                                tint = corCategoria(item.tipo),
                                                                size = 12.dp
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(item.tipo.label, color = splitPreviewChipText, fontSize = 11.sp)
                                                        }
                                                    }
                                                    ExposedDropdownMenu(
                                                        expanded = tipoMenuItemId == item.id,
                                                        onDismissRequest = { tipoMenuItemId = null }
                                                    ) {
                                                        TipoManutencao.values().forEach { tipo ->
                                                            DropdownMenuItem(
                                                                text = { Text(tipo.label) },
                                                                onClick = {
                                                                    listaItensDetectados = listaItensDetectados.map {
                                                                        if (it.id == item.id) it.copy(tipo = tipo) else it
                                                                    }
                                                                    tipoMenuItemId = null
                                                                },
                                                                leadingIcon = {
                                                                    TipoIcon(
                                                                        tipo = tipo,
                                                                        tint = corCategoria(tipo),
                                                                        size = 16.dp
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                if (kmFuturoCalculado.isNotEmpty()) {
                                                    Text(
                                                        "  - Vence +${getKmAdicionalPorTipo(item.tipo)}km",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(onClick = { listaItensDetectados = listaItensDetectados - item; if (listaItensDetectados.isEmpty()) isModoLista = false }) {
                                            Icon(Icons.Default.Delete, "Remover", tint = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = ::iniciarCapturaVoz) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Falar descricao",
                                                tint = Color(0xFF3B82F6)
                                            )
                                        }
                                    }
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
                        }

                    }
                    2 -> {
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
                            if (!isModoLista) {
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
                        }

                        if (!isModoLista) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        abrirTimePicker(horaNotificacao) { selecionada ->
                                            horaNotificacao = selecionada
                                        }
                                    },
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
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                listaItensDetectados.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.12f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(item.nome, color = textPrimary, fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(
                                                value = itemValorOverrides[item.id] ?: String.format(Locale.US, "%.2f", item.valor),
                                                onValueChange = { novo ->
                                                    if (novo.all { c -> c.isDigit() || c == '.' }) {
                                                        itemValorOverrides = itemValorOverrides + (item.id to novo)
                                                    }
                                                },
                                                label = { Text("Valor (R$)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = itemDataAvisoOverrides[item.id] ?: dataAviso,
                                                    onValueChange = {},
                                                    label = { Text("Data aviso") },
                                                    readOnly = true,
                                                    modifier = Modifier.weight(1f),
                                                    trailingIcon = {
                                                        IconButton(onClick = {
                                                            abrirDatePicker(itemDataAvisoOverrides[item.id] ?: dataAviso) { selecionada ->
                                                                itemDataAvisoOverrides = itemDataAvisoOverrides + (item.id to selecionada)
                                                            }
                                                        }) {
                                                            Icon(Icons.Default.Event, contentDescription = null)
                                                        }
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = itemHoraAvisoOverrides[item.id] ?: horaNotificacao,
                                                    onValueChange = {},
                                                    label = { Text("Hora") },
                                                    readOnly = true,
                                                    modifier = Modifier.weight(1f),
                                                    trailingIcon = {
                                                        IconButton(onClick = {
                                                            abrirTimePicker(itemHoraAvisoOverrides[item.id] ?: horaNotificacao) { selecionada ->
                                                                itemHoraAvisoOverrides = itemHoraAvisoOverrides + (item.id to selecionada)
                                                            }
                                                        }) {
                                                            Icon(Icons.Default.Schedule, contentDescription = null)
                                                        }
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Adicionar novo profissional", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = localServicoInput,
                                    onValueChange = { localServicoInput = it },
                                    label = { Text("Local") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = novoContatoNome,
                                    onValueChange = { novoContatoNome = it },
                                    label = { Text("Nome") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = novoContatoTelefone,
                                    onValueChange = { novoContatoTelefone = it },
                                    label = { Text("Telefone") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = ::adicionarContatoManual,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cadastrar profissional")
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                    Text("Vincular profissional", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                if (contatosLista.isEmpty()) {
                                    Text("Nenhum profissional cadastrado. Adicione um acima.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        contatosLista.forEach { contato ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        1.dp,
                                                        if (contatoSelecionado == contato) Color(0xFF3B82F6)
                                                        else if (isDark) Color.White.copy(alpha = 0.08f)
                                                        else Color.Black.copy(alpha = 0.12f),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable { contatoSelecionado = contato },
                                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111827) else Color.White),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = iconColor)
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(contato.nome, color = textPrimary, fontWeight = FontWeight.SemiBold)
                                                        Text(contato.telefone, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                                    }
                                                    if (contatoSelecionado == contato) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                                    } else {
                                                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color(0xFF64748B))
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(10.dp),
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
                            Text("Salvar Aviso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        }
    }
}



