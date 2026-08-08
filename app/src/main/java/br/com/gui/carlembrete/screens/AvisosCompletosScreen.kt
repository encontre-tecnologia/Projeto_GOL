package br.com.gui.carlembrete

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class GrupoDeAviso(val rotuloPt: String, val rotuloEn: String) {
    VENCIDOS("VENCIDOS", "OVERDUE"),
    ESTE_MES("ESTE MES", "THIS MONTH"),
    DEPOIS("MAIS ADIANTE", "LATER")
}

private enum class ModoAcervo {
    AVISOS,
    REGISTROS
}

private val registroDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun grupoDe(lembrete: Lembrete): GrupoDeAviso {
    val data = dataParaOrdenacao(lembrete)
    if (data == LocalDate.MAX) return GrupoDeAviso.DEPOIS
    val dias = ChronoUnit.DAYS.between(LocalDate.now(), data)
    return when {
        dias < 0 -> GrupoDeAviso.VENCIDOS
        dias <= 30 -> GrupoDeAviso.ESTE_MES
        else -> GrupoDeAviso.DEPOIS
    }
}

/**
 * Lista completa do acervo do veiculo.
 *
 * O LazyColumn e o unico container rolavel da tela. Cabecalho, seletor, busca, filtros,
 * grupos e cards ficam no mesmo scroll para o gesto funcionar em qualquer ponto.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AvisosCompletosScreen(
    nomeVeiculo: String,
    avisos: List<Lembrete>,
    registros: List<Lembrete> = emptyList(),
    categoriasDisponiveis: List<TipoManutencao>,
    corDoStatus: (Lembrete) -> Color,
    temPrestador: (Lembrete) -> Boolean,
    onAbrirAviso: (Lembrete) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val fundoTela = if (isDark) Color.Black else scheme.background
    val fundoCampo = if (isDark) Color(0xFF111827) else Color(0xFFEFF2F7)

    var busca by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf<TipoManutencao?>(null) }
    var modo by remember { mutableStateOf(ModoAcervo.AVISOS) }
    val chipsScroll = rememberScrollState()

    val acervoDoModo = if (modo == ModoAcervo.AVISOS) avisos else registros
    val placeholderBusca = if (modo == ModoAcervo.AVISOS) {
        tr("Buscar aviso", "Search reminder")
    } else {
        tr("Buscar registro", "Search record")
    }

    val filtrados = remember(acervoDoModo, busca, filtro, modo) {
        acervoDoModo
            .asSequence()
            .filter { filtro == null || it.tipo == filtro }
            .filter { busca.isBlank() || it.titulo.contains(busca.trim(), ignoreCase = true) || it.peca.contains(busca.trim(), ignoreCase = true) }
            .let { sequencia ->
                if (modo == ModoAcervo.AVISOS) {
                    sequencia.sortedBy { dataParaOrdenacao(it) }
                } else {
                    sequencia.sortedByDescending { dataRealizacaoLembrete(it) ?: dataParaOrdenacao(it) }
                }
            }
            .toList()
    }

    val porGrupo = remember(filtrados) {
        GrupoDeAviso.values().associateWith { grupo ->
            filtrados.filter { grupoDe(it) == grupo }
        }
    }
    val contagemPorTipo = remember(acervoDoModo) {
        acervoDoModo.groupingBy { it.tipo }.eachCount()
    }
    val mostrarFiltros = acervoDoModo.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fundoTela)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "cabecalho") {
                CabecalhoDoAcervo(
                    nomeVeiculo = nomeVeiculo,
                    modo = modo,
                    avisos = avisos.size,
                    registros = registros.size,
                    isDark = isDark
                )
            }

            item(key = "modo") {
                SeletorModoAcervo(
                    modo = modo,
                    avisos = avisos.size,
                    registros = registros.size,
                    isDark = isDark,
                    onModoChange = {
                        modo = it
                        filtro = null
                        busca = ""
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }

            if (mostrarFiltros) {
                item(key = "busca") {
                    CampoBuscaAcervo(
                        busca = busca,
                        placeholderBusca = placeholderBusca,
                        fundoCampo = fundoCampo,
                        onBuscaChange = { busca = it },
                        onLimparBusca = { busca = "" }
                    )
                }

                item(key = "filtros") {
                    FiltrosAcervo(
                        categoriasDisponiveis = categoriasDisponiveis,
                        contagemPorTipo = contagemPorTipo,
                        total = acervoDoModo.size,
                        filtro = filtro,
                        isDark = isDark,
                        chipsScroll = chipsScroll,
                        onFiltroChange = { filtro = it }
                    )
                }
            }

            if (filtrados.isEmpty()) {
                item(key = "vazio") {
                    NenhumResultado(
                        isDark = isDark,
                        comBusca = busca.isNotBlank() || filtro != null,
                        modo = modo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                }
            } else if (modo == ModoAcervo.AVISOS) {
                val gruposComItens = GrupoDeAviso.values().count { porGrupo[it].orEmpty().isNotEmpty() }
                GrupoDeAviso.values().forEach { grupo ->
                    val itens = porGrupo[grupo].orEmpty()
                    if (itens.isEmpty()) return@forEach

                    if (gruposComItens > 1) {
                        stickyHeader(key = "cabecalho_${grupo.name}") {
                            CabecalhoDeGrupo(
                                grupo = grupo,
                                quantidade = itens.size,
                                isDark = isDark
                            )
                        }
                    }

                    items(items = itens, key = { it.id }) { lembrete ->
                        AvisoCompactRow(
                            lembrete = lembrete,
                            corStatus = corDoStatus(lembrete),
                            temPrestador = temPrestador(lembrete),
                            onClick = { onAbrirAviso(lembrete) },
                            mostrarDivisor = false,
                            comSuperficie = true,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            } else {
                items(items = filtrados, key = { it.id }) { registro ->
                    RegistroRealizadoCard(
                        registro = registro,
                        isDark = isDark,
                        onClick = { onAbrirAviso(registro) },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 4.dp)
                .size(42.dp)
        ) {
            Icon(
                Icons.Default.ArrowBackIosNew,
                contentDescription = tr("Voltar", "Back"),
                tint = scheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CabecalhoDoAcervo(
    nomeVeiculo: String,
    modo: ModoAcervo,
    avisos: Int,
    registros: Int,
    isDark: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF11213D) else Color(0xFFE8F0FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (modo == ModoAcervo.AVISOS) Icons.Default.Notifications else Icons.Default.History,
                contentDescription = null,
                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (modo == ModoAcervo.AVISOS) tr("Avisos do $nomeVeiculo", "Reminders for $nomeVeiculo") else tr("Registros do $nomeVeiculo", "Records for $nomeVeiculo"),
            color = scheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tr("$avisos avisos ativos - $registros registros", "$avisos active - $registros records"),
            color = scheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CampoBuscaAcervo(
    busca: String,
    placeholderBusca: String,
    fundoCampo: Color,
    onBuscaChange: (String) -> Unit,
    onLimparBusca: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fundoCampo)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = busca,
                onValueChange = onBuscaChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 13.sp),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
            if (busca.isEmpty()) {
                Text(
                    text = placeholderBusca,
                    color = scheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        if (busca.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = tr("Limpar busca", "Clear search"),
                tint = scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onLimparBusca() }
            )
        }
    }
}

@Composable
private fun FiltrosAcervo(
    categoriasDisponiveis: List<TipoManutencao>,
    contagemPorTipo: Map<TipoManutencao, Int>,
    total: Int,
    filtro: TipoManutencao?,
    isDark: Boolean,
    chipsScroll: androidx.compose.foundation.ScrollState,
    onFiltroChange: (TipoManutencao?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(chipsScroll)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChipDeFiltro(
            rotulo = tr("Todos", "All"),
            quantidade = total,
            ativo = filtro == null,
            isDark = isDark,
            onClick = { onFiltroChange(null) }
        )
        categoriasDisponiveis
            .filter { (contagemPorTipo[it] ?: 0) > 0 }
            .sortedByDescending { contagemPorTipo[it] ?: 0 }
            .forEach { tipo ->
                ChipDeFiltro(
                    rotulo = tipoManutencaoLabel(tipo),
                    quantidade = contagemPorTipo[tipo] ?: 0,
                    ativo = filtro == tipo,
                    isDark = isDark,
                    onClick = { onFiltroChange(if (filtro == tipo) null else tipo) }
                )
            }
    }
}

@Composable
private fun SeletorModoAcervo(
    modo: ModoAcervo,
    avisos: Int,
    registros: Int,
    isDark: Boolean,
    onModoChange: (ModoAcervo) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF0F1724) else Color(0xFFEFF2F7))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BotaoModoAcervo(
            label = tr("Avisos", "Reminders"),
            count = avisos,
            selected = modo == ModoAcervo.AVISOS,
            isDark = isDark,
            onClick = { onModoChange(ModoAcervo.AVISOS) },
            modifier = Modifier.weight(1f)
        )
        BotaoModoAcervo(
            label = tr("Registros", "Records"),
            count = registros,
            selected = modo == ModoAcervo.REGISTROS,
            isDark = isDark,
            onClick = { onModoChange(ModoAcervo.REGISTROS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BotaoModoAcervo(
    label: String,
    count: Int,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fundo = if (selected) {
        if (isDark) Color(0xFF1D4ED8) else Color.White
    } else {
        Color.Transparent
    }
    val cor = when {
        selected && isDark -> Color.White
        selected -> Color(0xFF1D4ED8)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF64748B)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fundo)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = cor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text(count.toString(), color = cor.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RegistroRealizadoCard(
    registro: Lembrete,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val acento = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
    val textLight = if (isDark) Color(0xFFE5E7EB) else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF8EA2BE) else Color(0xFF64748B)
    val fundo = if (isDark) Color(0xFF10151C) else Color.White
    val data = dataRealizacaoLembrete(registro)?.format(registroDateFormatter)
        ?: registro.dataLimite.ifBlank { "--" }
    val km = registro.kmLimite.ifBlank { "--" }
    val valor = registro.valor.takeIf { it > 0.0 }?.let { formatarMoeda(it) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(fundo)
            .clickable { onClick() }
    ) {
        RegistroManutencaoRow(
            titulo = registro.titulo.ifBlank { registro.peca.ifBlank { tipoManutencaoLabel(registro.tipo) } },
            data = data,
            km = km,
            valor = valor,
            acento = acento,
            textLight = textLight,
            textDim = textDim
        )
    }
}

@Composable
private fun CabecalhoDeGrupo(
    grupo: GrupoDeAviso,
    quantidade: Int,
    isDark: Boolean
) {
    val (fundo, texto) = when (grupo) {
        GrupoDeAviso.VENCIDOS ->
            (if (isDark) Color(0xFF1A1013) else Color(0xFFFCEBEB)) to Color(0xFFDC2626)
        GrupoDeAviso.ESTE_MES ->
            (if (isDark) Color(0xFF191509) else Color(0xFFFAEEDA)) to Color(0xFFB45309)
        GrupoDeAviso.DEPOIS ->
            (if (isDark) Color(0xFF0F1620) else Color(0xFFF1F5F9)) to
                (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fundo)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = tr(grupo.rotuloPt, grupo.rotuloEn),
            color = texto,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
        Text(
            text = quantidade.toString(),
            color = texto,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChipDeFiltro(
    rotulo: String,
    quantidade: Int,
    ativo: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val fundo = when {
        ativo -> if (isDark) Color(0xFF1D4ED8) else Color(0xFF2563EB)
        isDark -> Color(0xFF131A27)
        else -> Color(0xFFEFF2F7)
    }
    val cor = when {
        ativo -> Color.White
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fundo)
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = rotulo, color = cor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(5.dp))
        Text(
            text = quantidade.toString(),
            color = cor.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NenhumResultado(
    isDark: Boolean,
    comBusca: Boolean,
    modo: ModoAcervo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (comBusca) {
                if (modo == ModoAcervo.AVISOS) tr("Nenhum aviso com esse filtro", "No reminder matches this filter") else tr("Nenhum registro com esse filtro", "No record matches this filter")
            } else {
                if (modo == ModoAcervo.AVISOS) tr("Nenhum aviso cadastrado", "No reminders yet") else tr("Nenhum registro cadastrado", "No records yet")
            },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
