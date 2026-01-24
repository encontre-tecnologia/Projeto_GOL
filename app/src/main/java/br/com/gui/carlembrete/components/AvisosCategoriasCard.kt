package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AvisosCategoriasCard(
    lembretesDoCarroAtual: List<Lembrete>,
    lembretesComBusca: List<Lembrete>,
    listaContatos: List<ContatoProfissional>,
    modeloCarro: String,
    filtroTipo: TipoManutencao?,
    onFiltroTipoChange: (TipoManutencao?) -> Unit,
    onDelete: (Lembrete) -> Unit,
    onAddPrestador: (Lembrete) -> Unit,
    onOpenDetalhes: (Lembrete) -> Unit,
    statusLabel: (Lembrete) -> String,
    statusColor: (TipoManutencao) -> Color,
    textDim: Color,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0xFF23324D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val contagem = TipoManutencao.values().associateWith { tipo ->
                        lembretesDoCarroAtual.count { it.tipo == tipo }
                    }
                    listOf(
                        TipoManutencao.OLEO,
                        TipoManutencao.MECANICA,
                        TipoManutencao.BATERIA,
                        TipoManutencao.FREIO,
                        TipoManutencao.TEMPERATURA,
                        TipoManutencao.LICENCIAMENTO,
                        TipoManutencao.IPVA,
                        TipoManutencao.SEGURO
                    ).forEach { tipo ->
                        MonitorIcon(
                            tipo = tipo,
                            cor = statusColor(tipo),
                            quantidade = contagem[tipo] ?: 0,
                            selected = filtroTipo == tipo,
                            onClick = {
                                onFiltroTipoChange(if (filtroTipo == tipo) null else tipo)
                            },
                            containerSize = 52.dp,
                            boxSize = 44.dp,
                            cornerRadius = 12.dp,
                            iconSize = 16.dp,
                            labelSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (lembretesComBusca.isEmpty()) {
                Text(
                    text = "Nenhum lembrete encontrado",
                    color = textDim,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val lembretesOrdenados = lembretesComBusca.sortedBy { dataParaOrdenacao(it) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lembretesOrdenados.forEach { lembrete ->
                        LembreteCardLocal(
                            lembrete = lembrete,
                            contato = listaContatos.find { it.id == lembrete.contatoId },
                            modeloCarro = modeloCarro,
                            onDelete = { onDelete(lembrete) },
                            onAddPrestador = { onAddPrestador(lembrete) },
                            onClick = { onOpenDetalhes(lembrete) },
                            statusLabel = statusLabel(lembrete),
                            statusColor = statusColor(lembrete.tipo)
                        )
                    }
                }
            }
        }
    }
}
