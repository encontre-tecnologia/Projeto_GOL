package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Quantos avisos a home mostra. O acervo mora na tela completa. */
const val AVISOS_NO_RESUMO = 3

/**
 * Resumo dos avisos na home: os mais urgentes e um caminho para a lista completa.
 *
 * Substitui o AvisosCategoriasCard, que compunha **todos** os avisos de uma vez num
 * Column dentro de verticalScroll. Com 100 avisos isso significava 100 cartoes de
 * ~140dp compostos na abertura, sem reciclagem. Aqui a altura e fixa e previsivel:
 * no maximo [AVISOS_NO_RESUMO] linhas.
 */
@Composable
fun AvisosResumoCard(
    avisosOrdenados: List<Lembrete>,
    totalAvisos: Int,
    corDoStatus: (Lembrete) -> Color,
    temPrestador: (Lembrete) -> Boolean,
    onAbrirAviso: (Lembrete) -> Unit,
    onVerTodos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val visiveis = avisosOrdenados.take(AVISOS_NO_RESUMO)
    val acento = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)

    Column(modifier = modifier.fillMaxWidth()) {
        // Titulo a esquerda e acao a direita, na mesma linha. Antes o "Ver todos"
        // era um botao centralizado embaixo da lista, que ficava orfao e sem moldura.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Proximos avisos", "Upcoming"),
                color = scheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (totalAvisos > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onVerTodos() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalAvisos > visiveis.size) {
                            tr("Ver todos ($totalAvisos)", "See all ($totalAvisos)")
                        } else {
                            tr("Ver todos", "See all")
                        },
                        color = acento,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = acento,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (visiveis.isEmpty()) {
            SemAvisosPendentes(isDark = isDark)
        } else {
            // Cada aviso numa superficie propria, com seta de navegacao. Sem isso o
            // item era texto solto com o prazo colado na borda e um vao no meio.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visiveis.forEach { lembrete ->
                    AvisoCompactRow(
                        lembrete = lembrete,
                        corStatus = corDoStatus(lembrete),
                        temPrestador = temPrestador(lembrete),
                        onClick = { onAbrirAviso(lembrete) },
                        mostrarDivisor = false,
                        comSuperficie = true
                    )
                }
            }
        }
    }
}

/**
 * Vazio tambem em superficie propria: solto no fundo da tela, o aviso de lista vazia
 * ficava sem moldura entre dois cartoes e lia como texto perdido, nao como estado.
 */
@Composable
private fun SemAvisosPendentes(isDark: Boolean) {
    val fundo = if (isDark) Color(0xFF0D1117) else Color(0xFFF1F5F9)
    val borda = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.07f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fundo)
            .border(1.dp, borda, RoundedCornerShape(18.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = tr("Nenhum aviso pendente", "No pending reminders"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tr(
                "Toque em Novo lembrete para criar o primeiro",
                "Tap New reminder to create the first one"
            ),
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Altura comum da faixa de acoes, para o primario e os secundarios alinharem. */
private val ALTURA_ACAO = 48.dp

/**
 * Acoes da home: criar aviso em destaque, consultar em segundo plano.
 *
 * Antes os tres botoes tinham peso identico, e o resultado era que nenhum liderava — a
 * tela virava uma pilha de caixas escuras iguais e o estado vazio abaixo precisava
 * escrever "Toque em Aviso" para apontar o caminho. Criar aviso e o loop central do app;
 * historico e relatorio sao consulta, um deles raro. Peso visual agora reflete isso.
 */
@Composable
fun HomeQuickActions(
    onNovoAviso: () -> Unit,
    onAbastecer: () -> Unit,
    onRelatorio: () -> Unit,
    mostrarAbastecer: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    // No claro a tela e branco puro (LightBackground = #FFFFFF), entao #F8FAFC ficava
    // indistinguivel do fundo. #F1F5F9 com borda da a definicao que faltava.
    val fundo = if (isDark) Color(0xFF0D1117) else Color(0xFFF1F5F9)
    // Azul cheio nos dois temas: e o que separa acao de conteudo nesta tela.
    val fundoPrimario = if (isDark) Color(0xFF2563EB) else Color(0xFF1D4ED8)
    // Icone de consulta em cinza, nao no azul de destaque: no azul competia com o
    // primario e desfazia a hierarquia que o preenchimento acabou de criar.
    val iconeSecundario = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        AcaoPrimaria(
            icone = Icons.Rounded.CalendarMonth,
            rotulo = tr("Novo lembrete", "New reminder"),
            fundo = fundoPrimario,
            onClick = onNovoAviso,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            AcaoSecundaria(
                icone = Icons.Default.LocalGasStation,
                rotulo = tr("Historico", "History"),
                fundo = fundo,
                acento = iconeSecundario,
                onClick = onAbastecer,
                modifier = Modifier.weight(1f)
            )
            AcaoSecundaria(
                icone = Icons.Outlined.Description,
                rotulo = tr("Relatorio", "Report"),
                fundo = fundo,
                acento = iconeSecundario,
                onClick = onRelatorio,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Preenchido e com rotulo na horizontal: le como botao, nao como cartao. */
@Composable
private fun AcaoPrimaria(
    icone: ImageVector,
    rotulo: String,
    fundo: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(ALTURA_ACAO)
            .clip(RoundedCornerShape(14.dp))
            .background(fundo)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = rotulo,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Estreito e sem preenchimento: ocupa o espaco de uma consulta, nao de uma acao. */
@Composable
private fun AcaoSecundaria(
    icone: ImageVector,
    rotulo: String,
    fundo: Color,
    acento: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ALTURA_ACAO)
            .clip(RoundedCornerShape(14.dp))
            .background(fundo)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.09f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            // Respiro so no topo: desce o conjunto icone+rotulo, que centralizado ficava
            // colado na borda de cima e nao acompanhava o rotulo do botao primario.
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = acento,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
