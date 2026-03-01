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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
    contatoSelecionado: ContatoProfissional?,
    cidadeAtual: String?,
    ufAtual: String?,
    onAcaoContato: (ContatoProfissional) -> Unit
) {
    val bgCard = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val headerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF)
    val itemBg = if (isDark) Color(0xFF0B1220) else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
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
        Text("Revisar aviso", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }

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
                Text("Resumo do cadastro", color = accentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isModoLista) "${listaItensDetectados.size} avisos selecionados" else descricao.ifBlank { "Aviso sem nome" },
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val local = listOfNotNull(cidadeAtual, ufAtual).joinToString(" - ").ifBlank { "Não informado" }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Confira os dados antes de concluir o cadastro.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumoTagGridItem("Categoria", tipoSelecionado.label, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("KM", kmBase.ifBlank { "Nao informado" }, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("Hora", horaNotificacao.ifBlank { "Nao informado" }, textPrimary, textSecondary, itemBg, borderColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumoTagGridItem("Valor", if (valorInput.isBlank()) "Nao informado" else "R$ $valorInput", textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("Aviso", dataAviso.ifBlank { "Nao informado" }, textPrimary, textSecondary, itemBg, borderColor)
                    ResumoTagGridItem("Servico", data.ifBlank { "Nao informado" }, textPrimary, textSecondary, itemBg, borderColor)
                }

                if (isModoLista && listaItensDetectados.isNotEmpty()) {
                    LinhaResumo(
                        "Avisos",
                        "${listaItensDetectados.size} itens selecionados",
                        textPrimary,
                        textSecondary,
                        itemBg,
                        borderColor
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listaItensDetectados.take(5).forEachIndexed { index, item ->
                            ResumoLinhaCompacta(
                                titulo = "${index + 1}. ${item.nome}",
                                valor = item.tipo.label,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                itemBg = itemBg,
                                borderColor = borderColor
                            )
                        }
                        if (listaItensDetectados.size > 5) {
                            Text(
                                "+${listaItensDetectados.size - 5} avisos adicionais",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                } else {
                    LinhaResumo(
                        "Descricao",
                        descricao.ifBlank { "Nao informado" },
                        textPrimary,
                        textSecondary,
                        itemBg,
                        borderColor
                    )
                }

                LinhaResumo("Profissional", contatoSelecionado?.nome ?: "Nenhum vinculado", textPrimary, textSecondary, itemBg, borderColor)
                LinhaResumo("Cidade", local, textPrimary, textSecondary, itemBg, borderColor)
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
        shape = RoundedCornerShape(14.dp),
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
    Surface(
        modifier = Modifier.weight(1f),
        color = itemBg,
        shape = RoundedCornerShape(14.dp),
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
private fun ResumoLinhaCompacta(
    titulo: String,
    valor: String,
    textPrimary: Color,
    textSecondary: Color,
    itemBg: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = itemBg,
        shape = RoundedCornerShape(12.dp),
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
