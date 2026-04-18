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
                    categoria = if (tipo == TipoManutencao.ABASTECIMENTO) tr("Posto", "Fuel") else tipo.label,
                    tipo = tipo,
                    km = kmBase.ifBlank { tr("Nao informado", "Not informed") },
                    hora = if (isRegistroServico) "" else horaNotificacao.ifBlank { tr("Nao informado", "Not informed") },
                    valor = "R$ ${itemValorOverrides[item.id] ?: item.valor.formatResumo()}",
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
                categoria = tituloCategoria,
                tipo = tipoSelecionado,
                km = kmBase.ifBlank { tr("Nao informado", "Not informed") },
                hora = if (isRegistroServico) "" else horaNotificacao.ifBlank { tr("Nao informado", "Not informed") },
                valor = if (valorInput.isBlank()) tr("Nao informado", "Not informed") else "R$ $valorInput",
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(accentBlue.copy(alpha = 0.18f), CircleShape)
                .border(1.dp, accentBlue.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = accentBlue, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isRegistroServico) tr("Revisar serviço", "Review service") else tr("Revisar aviso", "Review reminder"),
            color = textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(corCategoria(aviso.tipo).copy(alpha = 0.18f), CircleShape)
                        .border(1.dp, corCategoria(aviso.tipo).copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    TipoIcon(
                        tipo = aviso.tipo,
                        tint = corCategoria(aviso.tipo),
                        size = 15.dp
                    )
                }
                Text(
                    aviso.categoria,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                aviso.titulo,
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            LinhaResumo(
                titulo = tr("Descricao dos itens", "Items description"),
                valor = aviso.descricaoItens,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                itemBg = if (bgCard.luminance() < 0.5f) Color(0xFF0B1220) else Color(0xFFF8FAFC),
                borderColor = borderColor
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (bgCard.luminance() < 0.5f) Color(0xFF0B1220) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("Valor", "Amount"),
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        aviso.valor,
                        color = Color(0xFF22C55E),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class AvisoResumoUi(
    val titulo: String,
    val descricaoItens: String,
    val quantidadeResumo: String,
    val categoria: String,
    val tipo: TipoManutencao,
    val km: String,
    val hora: String,
    val valor: String,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(corCategoria(aviso.tipo).copy(alpha = 0.18f), CircleShape)
                            .border(1.dp, corCategoria(aviso.tipo).copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        TipoIcon(
                            tipo = aviso.tipo,
                            tint = corCategoria(aviso.tipo),
                            size = 15.dp
                        )
                    }
                    Text(
                        aviso.titulo,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                LinhaResumo(
                    titulo = tr("Quantidade", "Quantity"),
                    valor = aviso.quantidadeResumo,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    itemBg = itemBg,
                    borderColor = borderColor
                )
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
                    ResumoTagGridItem(tr("Valor", "Amount"), aviso.valor, textPrimary, textSecondary, itemBg, borderColor)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                .height(62.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(titulo, color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(
                valor,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("KM", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    formatarKmResumo(valor),
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(titulo, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(valor, color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
