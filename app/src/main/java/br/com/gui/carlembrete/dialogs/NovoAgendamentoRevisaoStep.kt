package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EtapaRevisaoAvisoContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    tituloLugar: String,
    descricao: String,
    tipoSelecionado: TipoManutencao,
    isModoLista: Boolean,
    listaItensDetectados: List<ItemDetectado>,
    quantidadeTotalItens: Int,
    mostrarTotal: Boolean = true,
    mostrarQuantidade: Boolean = true,
    kmBase: String,
    data: String,
    dataAviso: String,
    horaNotificacao: String,
    valorInput: String,
    itemDataAvisoOverrides: Map<String, String>,
    itemValorOverrides: Map<String, String>,
    itemTipoOverrides: Map<String, TipoManutencao>,
    contatoSelecionado: ContatoProfissional?,
    cidadeAtual: String?,
    ufAtual: String?,
    isRegistroServico: Boolean,
    repetirAteDesativar: Boolean,
    descricaoRepeticao: String,
    mostrarResumoSimplificadoPosto: Boolean,
    tituloCategoria: String,
    onAcaoContato: (ContatoProfissional) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bgCard = if (isDark) Color(0xFF111827) else scheme.surface
    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val itemBg = if (isDark) Color(0xFF0B1220) else scheme.surface
    val headerBg = itemBg
    val bodyBg = if (isDark) itemBg else scheme.background
    val avisos = if (isModoLista && listaItensDetectados.isNotEmpty()) {
        listaItensDetectados.flatMap { item ->
            val repeticoes = maxOf(1, item.quantidade)
            (1..repeticoes).map { indice ->
                val tipo = itemTipoOverrides[item.id] ?: item.tipo
                AvisoResumoUi(
                    titulo = tituloLugar.ifBlank { tr("Aviso", "Reminder") },
                    descricaoItens = item.nome,
                    quantidadeResumo = if (repeticoes > 1) "$indice/$repeticoes" else "1",
                    mostrarQuantidade = true,
                    categoria = if (tipo == TipoManutencao.ABASTECIMENTO) tr("Posto", "Fuel") else tipo.label,
                    tipo = tipo,
                    km = kmBase.ifBlank { tr("Nao informado", "Not informed") },
                    hora = if (isRegistroServico) "" else horaNotificacao.ifBlank { tr("Nao informado", "Not informed") },
                    valor = "R$ ${itemValorOverrides[item.id] ?: item.valor.formatResumo()}",
                    mostrarValor = true,
                    dataAviso = if (isRegistroServico) "" else (itemDataAvisoOverrides[item.id] ?: dataAviso),
                    dataServico = data.ifBlank { tr("Nao informado", "Not informed") },
                    repeticao = if (isRegistroServico) "" else if (repetirAteDesativar) tr("Sim", "Yes") + " (${descricaoRepeticao})" else tr("Nao", "No"),
                    isRegistroServico = isRegistroServico
                )
            }
        }
    } else {
        listOf(
            AvisoResumoUi(
                titulo = tituloLugar.ifBlank { tr("Aviso", "Reminder") },
                descricaoItens = descricao.ifBlank { tr("Sem descricao", "No description") },
                quantidadeResumo = quantidadeTotalItens.coerceAtLeast(1).toString(),
                mostrarQuantidade = mostrarQuantidade,
                categoria = tituloCategoria,
                tipo = tipoSelecionado,
                km = kmBase.ifBlank { tr("Nao informado", "Not informed") },
                hora = if (isRegistroServico) "" else horaNotificacao.ifBlank { tr("Nao informado", "Not informed") },
                valor = if (valorInput.isBlank()) tr("Nao informado", "Not informed") else "R$ $valorInput",
                mostrarValor = mostrarTotal,
                dataAviso = if (isRegistroServico) "" else dataAviso.ifBlank { tr("Nao informado", "Not informed") },
                dataServico = data.ifBlank { tr("Nao informado", "Not informed") },
                repeticao = if (isRegistroServico) "" else if (repetirAteDesativar) tr("Sim", "Yes") + " (${descricaoRepeticao})" else tr("Nao", "No"),
                isRegistroServico = isRegistroServico
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-14).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Anel duplo ao redor do ícone
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(accentBlue.copy(alpha = 0.07f), CircleShape)
                .border(1.5.dp, accentBlue.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(accentBlue.copy(alpha = 0.16f), CircleShape)
                    .border(1.5.dp, accentBlue.copy(alpha = 0.34f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FactCheck,
                    contentDescription = null,
                    tint = accentBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (isRegistroServico) tr("Revisar serviço", "Review service") else tr("Revisar aviso", "Review reminder"),
            color = textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = tr("Confira os dados antes de salvar", "Check details before saving"),
            color = textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        avisos.forEach { aviso ->
            if (mostrarResumoSimplificadoPosto) {
                AvisoResumoCardPosto(
                    aviso = aviso,
                    bgCard = bgCard,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            } else {
                AvisoResumoCard(
                    aviso = aviso,
                    bgCard = bgCard,
                    headerBg = headerBg,
                    itemBg = itemBg,
                    bodyBg = bodyBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }
    }
}

@Composable
private fun AvisoResumoCardPosto(
    aviso: AvisoResumoUi,
    bgCard: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ícone + categoria + título unidos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(corCategoria(aviso.tipo).copy(alpha = 0.18f), CircleShape)
                        .border(1.5.dp, corCategoria(aviso.tipo).copy(alpha = 0.32f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    TipoIcon(
                        tipo = aviso.tipo,
                        tint = corCategoria(aviso.tipo),
                        size = 20.dp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        aviso.categoria,
                        color = corCategoria(aviso.tipo),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        aviso.titulo,
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            LinhaResumo(
                titulo = tr("Descricao dos itens", "Items description"),
                valor = aviso.descricaoItens,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                itemBg = if (bgCard.luminance() < 0.5f) Color(0xFF0B1220) else Color(0xFFF8FAFC),
                borderColor = borderColor
            )
            if (aviso.mostrarValor) {
                // Valor em destaque com fundo verde suave
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF22C55E).copy(alpha = if (bgCard.luminance() < 0.5f) 0.13f else 0.09f),
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tr("Total", "Total"),
                            color = Color(0xFF22C55E).copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            aviso.valor,
                            color = Color(0xFF22C55E),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }
        }
    }
}

private data class AvisoResumoUi(
    val titulo: String,
    val descricaoItens: String,
    val quantidadeResumo: String,
    val mostrarQuantidade: Boolean,
    val categoria: String,
    val tipo: TipoManutencao,
    val km: String,
    val hora: String,
    val valor: String,
    val mostrarValor: Boolean,
    val dataAviso: String,
    val dataServico: String,
    val repeticao: String,
    val isRegistroServico: Boolean
)

private fun Double.formatResumo(): String = if (this == 0.0) {
    "0.00"
} else {
    String.format("%.2f", this)
}

private fun formatarKmResumo(value: String): String {
    val numero = value.filter(Char::isDigit).toLongOrNull() ?: return value.ifBlank { "Not informed" }
    return "${NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(numero)} km"
}

@Composable
private fun AvisoResumoCard(
    aviso: AvisoResumoUi,
    bgCard: Color,
    headerBg: Color,
    itemBg: Color,
    bodyBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header com strip lateral colorida
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(corCategoria(aviso.tipo).copy(alpha = 0.18f), CircleShape)
                            .border(1.5.dp, corCategoria(aviso.tipo).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        TipoIcon(
                            tipo = aviso.tipo,
                            tint = corCategoria(aviso.tipo),
                            size = 20.dp
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            aviso.titulo,
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = corCategoria(aviso.tipo).copy(alpha = 0.15f)
                        ) {
                            Text(
                                aviso.categoria,
                                color = corCategoria(aviso.tipo),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // Strip lateral esquerda colorida por categoria
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(corCategoria(aviso.tipo))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bodyBg)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LinhaResumo(
                    titulo = tr("Descricao dos itens", "Items description"),
                    valor = aviso.descricaoItens,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    itemBg = itemBg,
                    borderColor = borderColor
                )
                if (aviso.mostrarQuantidade) {
                    LinhaResumo(
                        titulo = tr("Quantidade", "Quantity"),
                        valor = aviso.quantidadeResumo,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        itemBg = itemBg,
                        borderColor = borderColor
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumoTagGridItem(tr("Categoria", "Category"), aviso.categoria, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagKmGridItem(aviso.km, textPrimary, textSecondary, itemBg, borderColor)
                    if (!aviso.isRegistroServico) {
                        ResumoTagGridItem(tr("Hora", "Time"), aviso.hora, textPrimary, textSecondary, itemBg, borderColor)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (aviso.mostrarValor) {
                        ResumoTagGridItem(tr("Valor", "Amount"), aviso.valor, textPrimary, textSecondary, itemBg, borderColor)
                    }
                    ResumoTagGridItem(tr("Data do serviço", "Service date"), aviso.dataServico, textPrimary, textSecondary, itemBg, borderColor)
                    if (!aviso.isRegistroServico) {
                        ResumoTagGridItem(tr("Aviso", "Reminder"), aviso.dataAviso, textPrimary, textSecondary, itemBg, borderColor)
                    }
                }
                if (!aviso.isRegistroServico) {
                    ResumoLinhaCompacta(
                        titulo = tr("Repeticao", "Repeat"),
                        valor = aviso.repeticao,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        itemBg = itemBg,
                        borderColor = borderColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaResumo(
    titulo: String,
    valor: String,
    textPrimary: Color,
    textSecondary: Color,
    itemBg: Color,
    borderColor: Color
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Barra lateral de acento
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        accentColor.copy(alpha = 0.55f),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    titulo,
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
                Text(
                    valor,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RowScope.ResumoTagGridItem(
    titulo: String,
    valor: String,
    textPrimary: Color,
    textSecondary: Color,
    itemBg: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                titulo,
                color = textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            Text(
                valor,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RowScope.ResumoTagKmGridItem(
    valor: String,
    textPrimary: Color,
    textSecondary: Color,
    itemBg: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                "KM",
                color = textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            Text(
                formatarKmResumo(valor),
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResumoLinhaCompacta(
    titulo: String,
    valor: String,
    textPrimary: Color,
    textSecondary: Color,
    itemBg: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                titulo,
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                valor,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
