package br.com.gui.carlembrete

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Uma manutencao como linha empilhada, no lugar de coluna de planilha.
 *
 * A tabela anterior tinha 700dp de largura fixa dentro de uma tela de ~390dp: KM, valor e os botoes
 * de acao ficavam fora do visor, alcancaveis so por rolagem horizontal, e o cabecalho aparecia
 * cortado ("K..."). Empilhado, tudo cabe na largura do telefone e nada fica escondido.
 */
@Composable
fun RegistroManutencaoRow(
    titulo: String,
    data: String,
    km: String,
    valor: String?,
    acento: Color,
    textLight: Color,
    textDim: Color,
    onEditar: (() -> Unit)? = null,
    onApagar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = textLight,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(5.dp))
            // Data e KM viram uma linha de apoio: sao contexto do servico, nao colunas irmas do titulo.
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaDoRegistro(Icons.Outlined.CalendarToday, data, textDim)
                if (km.isNotBlank() && km != "--") {
                    Spacer(Modifier.width(12.dp))
                    MetaDoRegistro(Icons.Outlined.Speed, km, textDim)
                }
            }
        }

        if (!valor.isNullOrBlank()) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = valor,
                color = textLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (onEditar != null || onApagar != null) {
            Spacer(Modifier.width(2.dp))
            onEditar?.let {
                IconButton(onClick = it, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = tr("Editar", "Edit"), tint = acento, modifier = Modifier.size(17.dp))
                }
            }
            onApagar?.let {
                IconButton(onClick = it, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = tr("Apagar", "Delete"), tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaDoRegistro(icone: ImageVector, texto: String, textDim: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, tint = textDim, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = texto, color = textDim, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Paginador que some quando ha uma pagina so.
 *
 * "Pagina 1/1" embaixo de um unico registro era um controle que nao controla nada — ocupava uma
 * faixa inteira do cartao para informar que nao ha mais nada para ver.
 */
@Composable
fun PaginadorDeTabela(
    paginaAtual: Int,
    totalPaginas: Int,
    fundo: Color,
    textLight: Color,
    textDim: Color,
    onAnterior: () -> Unit,
    onProxima: () -> Unit
) {
    if (totalPaginas <= 1) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fundo)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetaDePagina(
            icone = Icons.Default.KeyboardArrowLeft,
            habilitada = paginaAtual > 0,
            descricao = tr("Anterior", "Previous"),
            textLight = textLight,
            textDim = textDim,
            onClick = onAnterior
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${tr("Pagina", "Page")} ${paginaAtual + 1}/$totalPaginas",
            color = textDim,
            fontSize = 12.sp
        )
        Spacer(Modifier.width(6.dp))
        SetaDePagina(
            icone = Icons.Default.KeyboardArrowRight,
            habilitada = paginaAtual < totalPaginas - 1,
            descricao = tr("Proxima", "Next"),
            textLight = textLight,
            textDim = textDim,
            onClick = onProxima
        )
    }
}

@Composable
private fun SetaDePagina(
    icone: ImageVector,
    habilitada: Boolean,
    descricao: String,
    textLight: Color,
    textDim: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = habilitada, modifier = Modifier.size(32.dp)) {
        Icon(icone, contentDescription = descricao, tint = if (habilitada) textLight else textDim)
    }
}

/** Separador entre registros, mais leve que a grade de linhas da tabela antiga. */
@Composable
fun SeparadorDeRegistro(cor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(cor)
    )
}
