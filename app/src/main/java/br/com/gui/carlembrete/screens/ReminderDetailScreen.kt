package br.com.gui.carlembrete

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun InfoMini(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LembreteDetalhesScreen(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    carro: CarroInfo,
    onDismiss: () -> Unit,
    onDelete: (Lembrete) -> Unit,
    onMarkAsDone: (Lembrete) -> Unit,
    onFinalizeAndClose: (Lembrete) -> Unit,
    onSalvar: (Lembrete) -> Unit,
    onAddPrestador: (Lembrete) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val appBarBg = if (isDark) Color.Black else colorScheme.surface
    val screenBg = if (isDark) Color.Black else colorScheme.background
    val cardBg = if (isDark) Color(0xFF111827) else colorScheme.surface
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val textPrimary = colorScheme.onSurface
    val textSecondary = colorScheme.onSurfaceVariant
    val englishUi = isEnglishUi()
    fun extrairValorNumericoLinhaResumo(texto: String, matcher: (String) -> Boolean): String? {
        val linhaResumo = texto
            .lines()
            .map { it.trim() }
            .firstOrNull {
                matcher(it.lowercase(Locale.getDefault()))
            } ?: return null
        val bruto = linhaResumo.substringAfter(':', "").trim().ifBlank { linhaResumo }
        val numero = Regex("""\d{1,3}(?:\.\d{3})*(?:,\d{1,2})|\d+(?:[.,]\d{1,2})?""")
            .find(bruto)
            ?.value
            ?: return null
        return numero.replace(".", "").replace(",", ".")
    }
    fun atualizarDescricaoComResumoFinanceiro(
        descricaoAtual: String,
        totalBruto: Double?,
        valorFinal: Double?
    ): String {
        val linhasBase = descricaoAtual
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot {
                val n = it.lowercase(Locale.getDefault())
                n.startsWith("total") ||
                    n.startsWith("valor total") ||
                    n.contains("desconto") ||
                    n.contains("valor final") ||
                    n.contains("valor a pagar")
            }
            .toMutableList()
        val totalNormalizado = totalBruto?.takeIf { it >= 0.0 }
        val finalNormalizado = valorFinal?.takeIf { it >= 0.0 } ?: totalNormalizado
        if (totalNormalizado != null) {
            linhasBase += "Valor total: R$ ${String.format(Locale.US, "%.2f", totalNormalizado)}"
        }
        if (totalNormalizado != null || finalNormalizado != null) {
            val descontoCalculado = ((totalNormalizado ?: 0.0) - (finalNormalizado ?: 0.0)).coerceAtLeast(0.0)
            linhasBase += "Desconto: R$ ${String.format(Locale.US, "%.2f", descontoCalculado)}"
        }
        if (finalNormalizado != null) {
            linhasBase += "Valor final: R$ ${String.format(Locale.US, "%.2f", finalNormalizado)}"
        }
        return linhasBase.joinToString("\n")
    }

    var editando by remember(lembrete.id) { mutableStateOf(false) }
    var titulo by remember(lembrete.id) { mutableStateOf(lembrete.titulo) }
    var tipoSelecionadoEdicao by remember(lembrete.id) { mutableStateOf(lembrete.tipo) }
    var descricaoEdicao by remember(lembrete.id) { mutableStateOf(lembrete.peca) }
    var dataAviso by remember(lembrete.id) { mutableStateOf(lembrete.dataLimite) }
    var horaAviso by remember(lembrete.id) { mutableStateOf(lembrete.horaAviso) }
    var kmLimite by remember(lembrete.id) { mutableStateOf(lembrete.kmLimite) }
    var repetirAviso by remember(lembrete.id) { mutableStateOf(false) }
    var recorrenciaUnit by remember(lembrete.id) { mutableStateOf(NotificacaoHelper.REC_UNIT_DAY) }
    var recorrenciaIntervaloTexto by remember(lembrete.id) { mutableStateOf("1") }
    var menuRecorrenciaExpanded by remember(lembrete.id) { mutableStateOf(false) }
    var menuTipoExpanded by remember(lembrete.id) { mutableStateOf(false) }
    var showConfirmarFeitoDialog by remember(lembrete.id) { mutableStateOf(false) }
    var showFinalizarEncerrarDialog by remember(lembrete.id) { mutableStateOf(false) }
    var showConfirmarExclusaoDialog by remember(lembrete.id) { mutableStateOf(false) }
    fun abrirSeletorDataAviso() {
        val formatadorData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dataAtual = runCatching { LocalDate.parse(dataAviso, formatadorData) }.getOrElse { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, ano, mes, dia ->
                dataAviso = String.format(Locale.US, "%02d/%02d/%04d", dia, mes + 1, ano)
            },
            dataAtual.year,
            dataAtual.monthValue - 1,
            dataAtual.dayOfMonth
        ).show()
    }
    fun abrirSeletorHoraAviso() {
        val formatadorHora = DateTimeFormatter.ofPattern("HH:mm")
        val horaAtual = runCatching { LocalTime.parse(horaAviso, formatadorHora) }.getOrElse { LocalTime.of(9, 0) }
        TimePickerDialog(
            context,
            { _, hora, minuto ->
                horaAviso = String.format(Locale.US, "%02d:%02d", hora, minuto)
            },
            horaAtual.hour,
            horaAtual.minute,
            true
        ).show()
    }
    val tipoPermiteRepeticaoEdicao = tipoSelecionadoEdicao != TipoManutencao.LICENCIAMENTO &&
        tipoSelecionadoEdicao != TipoManutencao.SEGURO &&
        tipoSelecionadoEdicao != TipoManutencao.IPVA &&
        tipoSelecionadoEdicao != TipoManutencao.ABASTECIMENTO
    val tipoPermiteRepeticao = lembrete.tipo != TipoManutencao.LICENCIAMENTO &&
        lembrete.tipo != TipoManutencao.SEGURO &&
        lembrete.tipo != TipoManutencao.IPVA &&
        lembrete.tipo != TipoManutencao.ABASTECIMENTO
    fun textoRecorrencia(unit: String, interval: Int): String {
        val intervaloValido = interval.coerceAtLeast(1)
        return when (unit) {
            NotificacaoHelper.REC_UNIT_DAY -> if (intervaloValido == 1) {
                if (englishUi) "Every 1 day" else "A cada 1 dia"
            } else {
                if (englishUi) "Every $intervaloValido days" else "A cada $intervaloValido dias"
            }
            NotificacaoHelper.REC_UNIT_MONTH -> if (intervaloValido == 1) {
                if (englishUi) "Every 1 month" else "A cada 1 mes"
            } else {
                if (englishUi) "Every $intervaloValido months" else "A cada $intervaloValido meses"
            }
            NotificacaoHelper.REC_UNIT_YEAR -> if (intervaloValido == 1) {
                if (englishUi) "Every 1 year" else "A cada 1 ano"
            } else {
                if (englishUi) "Every $intervaloValido years" else "A cada $intervaloValido anos"
            }
            else -> if (englishUi) "Do not repeat" else "Nao repetir"
        }
    }
    val descricaoRecorrenciaAtual = if (!tipoPermiteRepeticao || !repetirAviso) {
        if (englishUi) "No" else "Nao"
    } else {
        (if (englishUi) "Yes" else "Sim") + " (${textoRecorrencia(recorrenciaUnit, recorrenciaIntervaloTexto.toIntOrNull() ?: 1)})"
    }
    val detalhesDateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val dataBaseLembrete = remember(lembrete.dataLimite) {
        runCatching { LocalDate.parse(lembrete.dataLimite, detalhesDateFormatter) }.getOrNull()
    }
    val statusDetalhe = remember(lembrete.id, lembrete.dataLimite, lembrete.estabelecimentoEndereco) {
        when {
            isLembreteRealizado(lembrete) -> if (englishUi) "Completed" else "Concluído"
            dataBaseLembrete == null -> if (englishUi) "Active" else "Ativo"
            dataBaseLembrete.isBefore(LocalDate.now()) -> if (englishUi) "Overdue" else "Vencido"
            dataBaseLembrete.isEqual(LocalDate.now()) -> if (englishUi) "Due today" else "Vence hoje"
            else -> if (englishUi) "Active" else "Ativo"
        }
    }
    val statusVencido = remember(lembrete.id, lembrete.dataLimite, lembrete.estabelecimentoEndereco) {
        !isLembreteRealizado(lembrete) && dataBaseLembrete?.isBefore(LocalDate.now()) == true
    }
    val proximoLembreteTexto = run {
        if (isLembreteRealizado(lembrete)) {
            tr("Concluído", "Completed")
        } else {
            val base = dataBaseLembrete
            if (base == null) {
                tr("Não definido", "Not set")
            } else if (tipoPermiteRepeticao && repetirAviso) {
                val intervalo = (recorrenciaIntervaloTexto.toIntOrNull() ?: 1).coerceAtLeast(1)
                val proximaData = when (recorrenciaUnit) {
                    NotificacaoHelper.REC_UNIT_DAY -> base.plusDays(intervalo.toLong())
                    NotificacaoHelper.REC_UNIT_MONTH -> base.plusMonths(intervalo.toLong())
                    NotificacaoHelper.REC_UNIT_YEAR -> base.plusYears(intervalo.toLong())
                    else -> base
                }
                proximaData.format(detalhesDateFormatter)
            } else {
                base.format(detalhesDateFormatter)
            }
        }
    }
    val proximaDataDoFluxoAtual = remember(
        lembrete.id,
        lembrete.dataLimite,
        repetirAviso,
        recorrenciaUnit,
        recorrenciaIntervaloTexto
    ) {
        if (!repetirAviso || isLembreteRealizado(lembrete)) return@remember null
        val intervalo = (recorrenciaIntervaloTexto.toIntOrNull() ?: 1).coerceAtLeast(1)
        val base = runCatching { LocalDate.parse(lembrete.dataLimite, detalhesDateFormatter) }.getOrNull()
            ?: return@remember null
        val hoje = LocalDate.now()
        var proxima = base
        while (!proxima.isAfter(hoje)) {
            proxima = when (recorrenciaUnit) {
                NotificacaoHelper.REC_UNIT_DAY -> proxima.plusDays(intervalo.toLong())
                NotificacaoHelper.REC_UNIT_WEEK -> proxima.plusWeeks(intervalo.toLong())
                NotificacaoHelper.REC_UNIT_MONTH -> proxima.plusMonths(intervalo.toLong())
                NotificacaoHelper.REC_UNIT_YEAR -> proxima.plusYears(intervalo.toLong())
                else -> proxima.plusDays(intervalo.toLong())
            }
        }
        proxima.format(detalhesDateFormatter)
    }

    LaunchedEffect(lembrete.id) {
        val recorrenciaAtual = NotificacaoHelper.obterRecorrencia(context.applicationContext, lembrete.id)
        repetirAviso = recorrenciaAtual != null && tipoPermiteRepeticao
        recorrenciaUnit = recorrenciaAtual?.unit ?: NotificacaoHelper.REC_UNIT_DAY
        recorrenciaIntervaloTexto = (recorrenciaAtual?.interval ?: 1).coerceAtLeast(1).toString()
    }

    val resetarEdicao = {
        titulo = lembrete.titulo
        tipoSelecionadoEdicao = lembrete.tipo
        descricaoEdicao = lembrete.peca
        dataAviso = lembrete.dataLimite
        horaAviso = lembrete.horaAviso
        kmLimite = lembrete.kmLimite
        val recorrenciaAtual = NotificacaoHelper.obterRecorrencia(context.applicationContext, lembrete.id)
        repetirAviso = recorrenciaAtual != null && tipoPermiteRepeticao
        recorrenciaUnit = recorrenciaAtual?.unit ?: NotificacaoHelper.REC_UNIT_DAY
        recorrenciaIntervaloTexto = (recorrenciaAtual?.interval ?: 1).coerceAtLeast(1).toString()
        menuRecorrenciaExpanded = false
        menuTipoExpanded = false
    }
    val categoriaColor = remember(lembrete.tipo, statusVencido) {
        if (statusVencido) {
            Color(0xFFDC2626)
        } else {
            when (lembrete.tipo) {
                TipoManutencao.OLEO -> Color(0xFF2563EB)
                TipoManutencao.ABASTECIMENTO -> Color(0xFF0EA5E9)
                TipoManutencao.LAVAGEM -> Color(0xFF06B6D4)
                TipoManutencao.FREIO -> Color(0xFFDC2626)
                TipoManutencao.PNEU -> Color(0xFFF59E0B)
                TipoManutencao.BATERIA -> Color(0xFF0EA5E9)
                TipoManutencao.VIDROS -> Color(0xFF38BDF8)
                TipoManutencao.FUNILARIA -> Color(0xFFF97316)
                TipoManutencao.SEGURO, TipoManutencao.LICENCIAMENTO, TipoManutencao.IPVA -> Color(0xFF16A34A)
                else -> Color(0xFF6366F1)
            }
        }
    }

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = tr("Voltar", "Back"),
                            tint = textPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (editando) {
                                    resetarEdicao()
                                    editando = false
                                } else {
                                    editando = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (editando) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (editando) tr("Cancelar edição", "Cancel editing") else tr("Editar", "Edit"),
                                tint = if (editando) {
                                    Color(0xFFEF4444)
                                } else if (isDark) {
                                    Color.White
                                } else {
                                    Color(0xFF2563EB)
                                }
                            )
                        }
                        if (!editando) {
                            IconButton(onClick = { onAddPrestador(lembrete) }) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = tr("Adicionar prestador", "Add provider"),
                                    tint = if (isDark) Color.White else Color(0xFF2563EB)
                                )
                            }
                            IconButton(onClick = { showConfirmarExclusaoDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = tr("Apagar aviso", "Delete reminder"),
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(categoriaColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                                        .border(1.dp, categoriaColor.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TipoIcon(tipo = lembrete.tipo, tint = categoriaColor, size = 22.dp)
                                    if (statusVencido) {
                                        Text(
                                            text = "!",
                                            color = Color(0xFFDC2626),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-3).dp)
                                        )
                                    }
                                }
                                if (statusVencido) {
                                    Text(
                                        text = tr("Vencido", "Overdue"),
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text(
                                    text = abreviarTituloAvisoDetalhes(titulo.ifBlank { lembrete.titulo }),
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = if (statusVencido) 0.dp else 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = cardBorder)

                        if (editando) {
                            val tiposDisponiveisEdicao = remember(carro.tipoVeiculo, lembrete.tipo) {
                                (tiposAvisoPorVeiculo(carro.tipoVeiculo) + lembrete.tipo)
                                    .distinct()
                                    .filter { it != TipoManutencao.ABASTECIMENTO || showFuelReminder(carro.tipoVeiculo) }
                            }

                            EditReminderSection(
                                title = tr("Identificação", "Identification"),
                                icon = Icons.Default.Edit,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary
                            ) {
                                OutlinedTextField(
                                    value = titulo,
                                    onValueChange = { titulo = it },
                                    label = { Text(tr("Título", "Title")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ExposedDropdownMenuBox(
                                    expanded = menuTipoExpanded,
                                    onExpandedChange = { menuTipoExpanded = !menuTipoExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = tipoSelecionadoEdicao.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(tr("Categoria", "Category")) },
                                        leadingIcon = {
                                            TipoIcon(
                                                tipo = tipoSelecionadoEdicao,
                                                tint = corCategoria(tipoSelecionadoEdicao),
                                                size = 18.dp
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuTipoExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = menuTipoExpanded,
                                        onDismissRequest = { menuTipoExpanded = false }
                                    ) {
                                        tiposDisponiveisEdicao.forEach { tipo ->
                                            DropdownMenuItem(
                                                text = { Text(tipo.label) },
                                                leadingIcon = {
                                                    TipoIcon(
                                                        tipo = tipo,
                                                        tint = corCategoria(tipo),
                                                        size = 18.dp
                                                    )
                                                },
                                                onClick = {
                                                    tipoSelecionadoEdicao = tipo
                                                    menuTipoExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = descricaoEdicao,
                                    onValueChange = { descricaoEdicao = it },
                                    label = { Text(tr("Descrição / peça / observações", "Description / item / notes")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    maxLines = 10
                                )
                            }

                            EditReminderSection(
                                title = tr("Quando avisar", "When to notify"),
                                icon = Icons.Default.Event,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dataAviso,
                                        onValueChange = {},
                                        label = { Text(tr("Data", "Date")) },
                                        trailingIcon = {
                                            IconButton(onClick = { abrirSeletorDataAviso() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Event,
                                                    contentDescription = tr("Selecionar data", "Select date"),
                                                    tint = textSecondary
                                                )
                                            }
                                        },
                                        readOnly = true,
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { abrirSeletorDataAviso() }
                                    )
                                    OutlinedTextField(
                                        value = horaAviso,
                                        onValueChange = {},
                                        label = { Text(tr("Hora", "Time")) },
                                        trailingIcon = {
                                            IconButton(onClick = { abrirSeletorHoraAviso() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = tr("Selecionar hora", "Select time"),
                                                    tint = textSecondary
                                                )
                                            }
                                        },
                                        readOnly = true,
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { abrirSeletorHoraAviso() }
                                    )
                                }
                                OutlinedTextField(
                                    value = kmLimite,
                                    onValueChange = { kmLimite = it.filter(Char::isDigit) },
                                    label = { Text(tr("KM limite", "Mileage limit")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (tipoPermiteRepeticaoEdicao) {
                                EditReminderSection(
                                    title = tr("Repetição", "Repeat"),
                                    icon = Icons.Default.Repeat,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { repetirAviso = !repetirAviso }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = repetirAviso,
                                            onCheckedChange = { repetirAviso = it }
                                        )
                                        Text(
                                            text = tr("Repetir esse aviso", "Repeat this reminder"),
                                            color = textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (repetirAviso) {
                                        ExposedDropdownMenuBox(
                                            expanded = menuRecorrenciaExpanded,
                                            onExpandedChange = { menuRecorrenciaExpanded = !menuRecorrenciaExpanded },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = textoRecorrencia(recorrenciaUnit, recorrenciaIntervaloTexto.toIntOrNull() ?: 1),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text(tr("Frequência da repetição", "Repeat frequency")) },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth(),
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuRecorrenciaExpanded)
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = menuRecorrenciaExpanded,
                                                onDismissRequest = { menuRecorrenciaExpanded = false }
                                            ) {
                                                listOf(
                                                    NotificacaoHelper.REC_UNIT_DAY to tr("Dias", "Days"),
                                                    NotificacaoHelper.REC_UNIT_MONTH to tr("Meses", "Months"),
                                                    NotificacaoHelper.REC_UNIT_YEAR to tr("Anos", "Years")
                                                ).forEach { (unitKey, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            recorrenciaUnit = unitKey
                                                            menuRecorrenciaExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = recorrenciaIntervaloTexto,
                                            onValueChange = { recorrenciaIntervaloTexto = it.filter(Char::isDigit).take(2) },
                                            label = {
                                                Text(
                                                    when (recorrenciaUnit) {
                                                        NotificacaoHelper.REC_UNIT_DAY -> tr("Repetir a cada quantos dias?", "Repeat every how many days?")
                                                        NotificacaoHelper.REC_UNIT_MONTH -> tr("Repetir a cada quantos meses?", "Repeat every how many months?")
                                                        else -> tr("Repetir a cada quantos anos?", "Repeat every how many years?")
                                                    }
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val descricaoAviso = lembrete.peca.ifBlank { tr("Sem descrição informada.", "No description provided.") }
                            val linhasDescricao = descricaoAviso
                                .lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            fun isLinhaResumoFinanceiro(linha: String): Boolean {
                                val normalized = linha.lowercase(Locale.getDefault())
                                return normalized.contains("valor total") ||
                                    normalized.startsWith("total") ||
                                    normalized.contains("desconto") ||
                                    normalized.contains("valor final") ||
                                    normalized.contains("valor a pagar") ||
                                    normalized.contains("troco")
                            }
                            fun valorAposDoisPontos(linha: String?): String? {
                                if (linha.isNullOrBlank()) return null
                                val extraido = linha.substringAfter(':', "").trim()
                                return extraido.takeIf { it.isNotBlank() }
                            }
                            fun parseMoeda(valor: String?): Double? {
                                if (valor.isNullOrBlank()) return null
                                val semSimbolo = valor
                                    .replace("R$", "", ignoreCase = true)
                                    .replace(" ", "")
                                val normalizado = if (semSimbolo.contains(',')) {
                                    semSimbolo.replace(".", "").replace(',', '.')
                                } else {
                                    semSimbolo
                                }
                                return normalizado.toDoubleOrNull()
                            }
                            fun extrairMoedaDaLinha(linha: String?): Double? {
                                if (linha.isNullOrBlank()) return null
                                val regexMoeda = Regex("""-?\s*R?\$?\s*\d{1,3}(?:\.\d{3})*(?:,\d{1,2})|-?\s*R?\$?\s*\d+(?:[.,]\d{1,2})?""")
                                val match = regexMoeda.find(linha) ?: return null
                                return parseMoeda(match.value)
                            }
                            val itensDoAviso = linhasDescricao.filterNot(::isLinhaResumoFinanceiro)
                            val linhaTotal = linhasDescricao.firstOrNull {
                                val normalized = it.lowercase(Locale.getDefault())
                                normalized.contains("valor total") || normalized.startsWith("total")
                            }
                            val linhaDesconto = linhasDescricao.firstOrNull {
                                it.lowercase(Locale.getDefault()).contains("desconto")
                            }
                            val linhaValorFinal = linhasDescricao.firstOrNull {
                                val normalized = it.lowercase(Locale.getDefault())
                                normalized.contains("valor final") || normalized.contains("valor a pagar")
                            }
                            val totalExtraido = valorAposDoisPontos(linhaTotal)
                            val descontoExtraido = valorAposDoisPontos(linhaDesconto)
                            val valorFinalExtraido = valorAposDoisPontos(linhaValorFinal)
                            val totalValor = parseMoeda(totalExtraido)
                                ?: extrairMoedaDaLinha(linhaTotal)
                                ?: lembrete.valor.takeIf { it > 0.0 }
                            val descontoValor = parseMoeda(descontoExtraido)
                                ?: extrairMoedaDaLinha(linhaDesconto)
                            val finalValor = parseMoeda(valorFinalExtraido)
                                ?: extrairMoedaDaLinha(linhaValorFinal)
                                ?: if (totalValor != null && descontoValor != null) {
                                    (totalValor - descontoValor).coerceAtLeast(0.0)
                                } else {
                                    totalValor
                                }
                            val mostrarResumoFinanceiro = totalValor != null || finalValor != null || descontoValor != null
                            val tabelaDados = buildList<Pair<String, String>> {
                                add(tr("Próximo lembrete", "Next reminder") to proximoLembreteTexto)
                                add(tr("Status", "Status") to statusDetalhe)
                                add(tr("Veículo", "Vehicle") to carro.nome)
                                add(tr("Hora", "Time") to lembrete.horaAviso.ifBlank { tr("Não definida", "Not set") })
                                add(tr("Repetição", "Repeat") to descricaoRecorrenciaAtual)
                                add(
                                    tr("Prestador", "Provider") to (
                                        contato?.let { "${it.nome} (${it.tipoServico})" }
                                            ?: tr("Não definido", "Not set")
                                    )
                                )
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                                            .background(
                                                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                            )
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("Descrição do aviso", "Reminder description"),
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (itensDoAviso.isEmpty()) {
                                            Text(
                                                text = descricaoAviso,
                                                color = textPrimary,
                                                fontSize = 14.sp,
                                                lineHeight = 19.sp
                                            )
                                        } else {
                                            itensDoAviso.forEach { item ->
                                                Text(
                                                    text = item,
                                                    color = textPrimary,
                                                    fontSize = 14.sp,
                                                    lineHeight = 19.sp
                                                )
                                            }
                                        }
                                        if (mostrarResumoFinanceiro) {
                                            Spacer(Modifier.height(6.dp))
                                            HorizontalDivider(color = cardBorder.copy(alpha = 0.65f))
                                            Spacer(Modifier.height(6.dp))
                                            totalValor?.let {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(tr("Valor total", "Total amount"), color = textSecondary, fontSize = 12.sp)
                                                    Text(formatarMoedaLocal(it), color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            val descontoExibicao = descontoValor ?: 0.0
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(tr("Desconto", "Discount"), color = textSecondary, fontSize = 12.sp)
                                                val descontoTexto = if (descontoExibicao > 0.0) {
                                                    "- ${formatarMoedaLocal(descontoExibicao)}"
                                                } else {
                                                    formatarMoedaLocal(0.0)
                                                }
                                                val descontoColor = if (descontoExibicao > 0.0) Color(0xFFDC2626) else textPrimary
                                                Text(
                                                    descontoTexto,
                                                    color = descontoColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            finalValor?.let {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(tr("Valor final", "Final amount"), color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(formatarMoedaLocal(it), color = Color(0xFF16A34A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                                            .background(
                                                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                            )
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("Resumo do aviso", "Reminder summary"),
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                                    tabelaDados.forEachIndexed { index, (label, value) ->
                                        InfoTableRow(
                                            label = label,
                                            value = value,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary
                                        )
                                        if (index != tabelaDados.lastIndex) {
                                            HorizontalDivider(color = cardBorder.copy(alpha = 0.65f))
                                        }
                                    }
                                }
                            }
                        }
                }

                if (editando) {
                    Button(
                        onClick = {
                            val atualizado = lembrete.copy(
                                titulo = titulo.ifBlank { lembrete.titulo },
                                tipo = tipoSelecionadoEdicao,
                                peca = descricaoEdicao.ifBlank { lembrete.peca },
                                dataLimite = dataAviso.ifBlank { lembrete.dataLimite },
                                horaAviso = horaAviso.ifBlank { lembrete.horaAviso },
                                kmLimite = kmLimite
                            )
                            val intervaloRecorrencia = (recorrenciaIntervaloTexto.toIntOrNull() ?: 1).coerceAtLeast(1)
                            val atualizadoPermiteRepeticao = atualizado.tipo != TipoManutencao.LICENCIAMENTO &&
                                atualizado.tipo != TipoManutencao.SEGURO &&
                                atualizado.tipo != TipoManutencao.IPVA &&
                                atualizado.tipo != TipoManutencao.ABASTECIMENTO
                            if (atualizadoPermiteRepeticao && repetirAviso) {
                                NotificacaoHelper.salvarRecorrencia(
                                    context = context.applicationContext,
                                    lembreteId = atualizado.id,
                                    unit = recorrenciaUnit,
                                    interval = intervaloRecorrencia
                                )
                            } else {
                                NotificacaoHelper.removerRecorrencia(context.applicationContext, atualizado.id)
                            }
                            onSalvar(atualizado)
                            editando = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tr("Salvar edição", "Save edit"), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showConfirmarFeitoDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Marcar como realizado", "Mark as completed"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showFinalizarEncerrarDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tr("Finalizar e encerrar aviso", "Finalize and close reminder"),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
    if (showConfirmarFeitoDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmarFeitoDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.24f else 0.14f))
                            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(27.dp)
                        )
                    }
                    Text(
                        text = tr("Marcar aviso como concluído?", "Mark reminder as completed?"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                if (proximaDataDoFluxoAtual != null) {
                    Text(
                        text = buildAnnotatedString {
                            append(
                                tr(
                                    "Isso conclui apenas este ciclo. A próxima data de aviso desse lembrete vai ser: ",
                                    "This only completes the current cycle. The next reminder date for this item will be: "
                                )
                            )
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary)) {
                                append(proximaDataDoFluxoAtual)
                            }
                        },
                        color = textSecondary
                    )
                } else {
                    Text(
                        text = tr("Você confirma que este aviso já foi resolvido?", "Do you confirm this reminder has been completed?"),
                        color = textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmarFeitoDialog = false
                        onMarkAsDone(lembrete)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
                ) {
                    Text(tr("Sim, concluir", "Yes, complete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmarFeitoDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text(tr("Voltar", "Back"))
                }
            },
            containerColor = cardBg
        )
    }
    if (showFinalizarEncerrarDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizarEncerrarDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.24f else 0.14f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = tr("Finalizar e encerrar aviso?", "Finalize and close this reminder?"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Se você continuar, este aviso será encerrado de vez, mesmo que tenha repetição ativa. Você poderá criar outro depois, se quiser.",
                        "If you continue, this reminder will be permanently closed even if recurrence is active. You can create another one later if needed."
                    ),
                    color = textSecondary
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFinalizarEncerrarDialog = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Text(tr("Cancelar", "Cancel"))
                    }
                    Button(
                        onClick = {
                            showFinalizarEncerrarDialog = false
                            onFinalizeAndClose(lembrete)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                    ) {
                        Text(
                            text = tr("Sim, Finalizar", "Yes, Finalize"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            dismissButton = {},
            containerColor = cardBg
        )
    }
    if (showConfirmarExclusaoDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmarExclusaoDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.24f else 0.14f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Text(
                        text = tr("Apagar este aviso?", "Delete this reminder?"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = tr("Essa ação remove o aviso permanentemente. Deseja continuar?", "This action permanently deletes the reminder. Do you want to continue?"),
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmarExclusaoDialog = false
                        onDelete(lembrete)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                ) {
                    Text(tr("Sim, apagar", "Yes, delete"), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmarExclusaoDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Text(tr("Cancelar", "Cancel"))
                }
            },
            containerColor = cardBg
        )
    }
}

private fun abreviarTituloAvisoDetalhes(texto: String, maxChars: Int = 34): String {
    val valor = texto.trim().ifBlank { "Aviso" }
    return if (valor.length <= maxChars) valor else valor.take(maxChars - 3) + "..."
}

@Composable
private fun EditReminderSection(
    title: String,
    icon: ImageVector,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            content()
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

@Composable
private fun InfoTableRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.44f)
        )
        Text(
            text = value,
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.56f)
        )
    }
}
