import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Divider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.ContatoProfissional
import br.com.gui.carlembrete.Lembrete
import br.com.gui.carlembrete.TipoIcon
import br.com.gui.carlembrete.TipoManutencao
import br.com.gui.carlembrete.dataParaOrdenacao
import java.time.format.DateTimeFormatter

// --- CORES PADRONIZADAS ---
private val CardBackgroundColor = Color(0xFF1E293B) // Slate 800 (Base Sólida)
private val ItemBackgroundColor = Color(0xFF0F172A) // Slate 900 (Fundo dos itens)

private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            // COR SÓLIDA APLICADA AQUI
            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(CardBackgroundColor) // Garante fundo sólido
                    .padding(vertical = 24.dp)
                    .heightIn(min = 480.dp),
            ) {

                // TÍTULO CATEGORIAS
                Text(
                    text = "FILTRAR POR CATEGORIA",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Divider(
                    color = Color.Transparent,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(8.dp))

                // LISTA DE FILTROS (Scroll Horizontal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 12.dp), // Padding inicial correto
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val contagem = TipoManutencao.values().associateWith { tipo ->
                        lembretesDoCarroAtual.count { it.tipo == tipo }
                    }

                    MonitorAllIcon(
                        selected = filtroTipo == null,
                        onClick = { onFiltroTipoChange(null) }
                    )

                    listOf(
                        TipoManutencao.OLEO, TipoManutencao.MECANICA, TipoManutencao.BATERIA,
                        TipoManutencao.FREIO, TipoManutencao.TEMPERATURA, TipoManutencao.LICENCIAMENTO,
                        TipoManutencao.IPVA, TipoManutencao.SEGURO
                    ).forEach { tipo ->
                        MonitorIcon(
                            tipo = tipo,
                            cor = statusColor(tipo),
                            quantidade = contagem[tipo] ?: 0,
                            selected = filtroTipo == tipo,
                            onClick = { onFiltroTipoChange(if (filtroTipo == tipo) null else tipo) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Divider(
                    color = Color.Transparent,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(28.dp))

                // HEADER DA LISTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filtroTipo != null) filtroTipo.name else "PRÓXIMOS LEMBRETES",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Contador (Badge)
                    if (lembretesComBusca.isNotEmpty()) {
                        Surface(
                            color = if (filtroTipo != null) statusColor(filtroTipo) else AccentBlue,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${lembretesComBusca.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // CONTEÚDO DA LISTA
                if (lembretesComBusca.isEmpty()) {
                    EmptyStateView(TextGray)
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        lembretesComBusca.sortedBy { dataParaOrdenacao(it) }.forEach { lembrete ->
                            LembreteCardLocal(
                                lembrete = lembrete,
                                contato = listaContatos.find { it.id == lembrete.contatoId },
                                statusColor = statusColor(lembrete.tipo),
                                onClick = { onOpenDetalhes(lembrete) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun MonitorIcon(
    tipo: TipoManutencao,
    cor: Color,
    quantidade: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) cor else ItemBackgroundColor
    val iconColor = if (selected) Color.White else cor
    val borderColor = if (selected) cor else Color.White.copy(alpha = 0.1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TipoIcon(
                        tipo = tipo,
                        tint = iconColor,
                        size = 24.dp
                    )
                }
            }

            // Badge flutuante para contagem
            if (quantidade > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(24.dp)
                        .background(Color.Red, CircleShape)
                        .border(2.dp, CardBackgroundColor, CircleShape), // Borda da cor do fundo para "recortar" visualmente
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$quantidade",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = tipo.label,
            color = if(selected) TextWhite else TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonitorAllIcon(
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) AccentBlue else ItemBackgroundColor
    val iconColor = if (selected) Color.White else TextGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, if(selected) AccentBlue else Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Todos",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Todos",
            color = if(selected) TextWhite else TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LembreteCardLocal(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    statusColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItemBackgroundColor), // Fundo mais escuro para contraste
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone com fundo suave
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TipoIcon(
                        tipo = lembrete.tipo,
                        tint = statusColor,
                        size = 22.dp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lembrete.titulo,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Data formatada
                    val dataFormatada = try {
                        dataParaOrdenacao(lembrete).format(DateTimeFormatter.ofPattern("dd/MM"))
                    } catch (e: Exception) { "--" }

                    Text(
                        text = dataFormatada,
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (contato != null) {
                        Icon(
                            Icons.Rounded.Person, null,
                            tint = TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = contato.nome,
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Toque para ver detalhes",
                            color = TextGray.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateView(textColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(textColor.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tudo em dia!",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Nenhum lembrete para esta categoria.",
            color = textColor,
            fontSize = 14.sp
        )
    }
}
