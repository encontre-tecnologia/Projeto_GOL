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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    descricao: String,
    tipoSelecionado: TipoManutencao,
    isModoLista: Boolean,
    listaItensDetectados: List<ItemDetectado>,
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
    mostrarResumoSimplificadoPosto: Boolean,
    tituloCategoria: String,
    onAcaoContato: (ContatoProfissional) -> Unit
) {
    val bgCard = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val headerBg = if (isDark) Color(0xFF0F172A) else Color.White
    val itemBg = if (isDark) Color(0xFF0B1220) else Color.White
    val bodyBg = if (isDark) itemBg else Color.White
    val avisos = if (isModoLista && listaItensDetectados.isNotEmpty()) {
        listaItensDetectados.flatMap { item ->
            val repeticoes = maxOf(1, item.quantidade)
            (1..repeticoes).map { indice ->
                val tituloFormatado = if (repeticoes > 1) "${item.nome} (${indice}/${repeticoes})" else item.nome
                val tipo = itemTipoOverrides[item.id] ?: item.tipo
                AvisoResumoUi(
                    titulo = tituloFormatado,
                    categoria = if (tipo == TipoManutencao.ABASTECIMENTO) "Posto" else tipo.label,
                    tipo = tipo,
                    km = kmBase.ifBlank { "Nao informado" },
                    hora = horaNotificacao.ifBlank { "Nao informado" },
                    valor = "R$ ${itemValorOverrides[item.id] ?: item.valor.formatResumo()}",
                    dataAviso = itemDataAvisoOverrides[item.id] ?: dataAviso,
                    dataServico = data.ifBlank { "Nao informado" }
                )
            }
        }
    } else {
        listOf(
            AvisoResumoUi(
                titulo = descricao.ifBlank { "Aviso sem nome" },
                categoria = tituloCategoria,
                tipo = tipoSelecionado,
                km = kmBase.ifBlank { "Nao informado" },
                hora = horaNotificacao.ifBlank { "Nao informado" },
                valor = if (valorInput.isBlank()) "Nao informado" else "R$ $valorInput",
                dataAviso = dataAviso.ifBlank { "Nao informado" },
                dataServico = data.ifBlank { "Nao informado" }
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
        Text("Revisar aviso", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 25.sp)
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
            Text(
                aviso.titulo,
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Valor: ${aviso.valor}",
                color = textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class AvisoResumoUi(
    val titulo: String,
    val categoria: String,
    val tipo: TipoManutencao,
    val km: String,
    val hora: String,
    val valor: String,
    val dataAviso: String,
    val dataServico: String
)

private fun Double.formatResumo(): String = if (this == 0.0) {
    "0.00"
} else {
    String.format("%.2f", this)
}

private fun formatarKmResumo(value: String): String {
    val numero = value.filter(Char::isDigit).toLongOrNull() ?: return value.ifBlank { "Nao informado" }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumoTagGridItem("Categoria", aviso.categoria, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagKmGridItem(aviso.km, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("Hora", aviso.hora, textPrimary, textSecondary, itemBg, borderColor)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumoTagGridItem("Valor", aviso.valor, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("Aviso", aviso.dataAviso, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("Servico", aviso.dataServico, textPrimary, textSecondary, itemBg, borderColor)
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
