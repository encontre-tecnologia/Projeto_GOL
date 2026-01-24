import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import br.com.gui.carlembrete.ContatoProfissional
import br.com.gui.carlembrete.Lembrete
import br.com.gui.carlembrete.TipoIcon
import br.com.gui.carlembrete.TipoManutencao
import br.com.gui.carlembrete.dataParaOrdenacao
import java.time.format.DateTimeFormatter

// --- CORES ---
private val GradientStart = Color(0xFF334155)
private val GradientEnd = Color(0xFF1E293B)
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val SurfaceDark = Color(0xFF0F172A)
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
            .padding(horizontal = 8.dp) // Espaçamento externo na tela
            .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
                    .padding(vertical = 20.dp) // Apenas padding vertical no container principal
                    .heightIn(min = 480.dp),
            ) {

                // TÍTULO CATEGORIAS
                Text(
                    text = "CATEGORIAS",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp) // Alinhado com o conteúdo abaixo
                )

                Spacer(Modifier.height(12.dp))

                // LISTA DE FILTROS (Scroll Horizontal)
                // Correção: Adicionei padding externo para a lista não bater nas bordas arredondadas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp) // [FIX] Evita cortar nos cantos arredondados
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 4.dp), // Padding interno extra para o primeiro/ultimo item
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Spacer(Modifier.height(24.dp))

                // TÍTULO LISTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filtroTipo != null) filtroTipo.name else "PRÓXIMOS LEMBRETES",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Contador
                    if (lembretesComBusca.isNotEmpty()) {
                        val badgeColor = filtroTipo?.let { statusColor(it).copy(alpha = 0.7f) } ?: SurfaceDark
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${lembretesComBusca.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // LISTA DE CARDS OU EMPTY STATE
                if (lembretesComBusca.isEmpty()) {
                    EmptyStateView(TextGray)
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        lembretesComBusca.sortedBy { dataParaOrdenacao(it) }.forEach { lembrete ->
                            LembreteCardLocal(
                                lembrete = lembrete,
                                contato = listaContatos.find { it.id == lembrete.contatoId },
                                statusLabel = statusLabel(lembrete),
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

@Composable
fun MonitorIcon(
    tipo: TipoManutencao,
    cor: Color,
    quantidade: Int,
    selected: Boolean,
    onClick: () -> Unit,
    containerSize: Dp = 60.dp,
    boxSize: Dp = 50.dp,
    cornerRadius: Dp = 14.dp,
    iconSize: Dp = 20.dp,
    labelSize: TextUnit = 10.sp
) {
    val backgroundColor = if (selected) cor else Color(0xFF0F172A).copy(alpha = 0.6f)
    val contentColor = if (selected) Color.White else cor.copy(alpha = 0.9f)
    val borderColor = if (selected) cor else Color.White.copy(alpha = 0.05f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        // Box pai para permitir que o badge (offset) não seja cortado
        Box(
            modifier = Modifier.size(boxSize),
            contentAlignment = Alignment.BottomStart // Alinhamento base
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(cornerRadius),
                color = backgroundColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TipoIcon(
                        tipo = tipo,
                        tint = contentColor,
                        size = iconSize,
                        textSize = (labelSize.value + 2).sp
                    )
                }
            }

            // Badge (Contador)
            if (quantidade > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Alinha no topo à direita
                        .offset(x = 4.dp, y = (-4).dp) // Move levemente para fora ("flutuando")
                        .size(22.dp)
                        .background(cor, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$quantidade",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Label visível apenas se selecionado para limpar o visual
        Text(
            text = tipo.label,
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonitorAllIcon(
    selected: Boolean,
    onClick: () -> Unit,
    containerSize: Dp = 60.dp,
    boxSize: Dp = 50.dp,
    cornerRadius: Dp = 14.dp,
    iconSize: Dp = 20.dp,
    labelSize: TextUnit = 10.sp
) {
    val backgroundColor = if (selected) AccentBlue else Color(0xFF0F172A).copy(alpha = 0.6f)
    val contentColor = if (selected) Color.White else TextWhite
    val borderColor = if (selected) AccentBlue else Color.White.copy(alpha = 0.05f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier.size(boxSize),
            contentAlignment = Alignment.BottomStart
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(cornerRadius),
                color = backgroundColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Todos",
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Todos",
            color = TextWhite,
            fontSize = labelSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LembreteCardLocal(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    statusLabel: String,
    statusColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(0.dp) // Shadow removida, usando contraste
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Faixa lateral
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(statusColor)
            )

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícone circular
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TipoIcon(
                            tipo = lembrete.tipo,
                            tint = statusColor,
                            size = 20.dp
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Info Principal
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lembrete.titulo,
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        // DATA CORRIGIDA
                        val dataTexto = try {
                            // Usando a função auxiliar que você já tem
                            dataParaOrdenacao(lembrete).format(DateTimeFormatter.ofPattern("dd/MM"))
                        } catch (e: Exception) {
                            "--/--"
                        }

                        Text(
                            text = dataTexto,
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (contato != null) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = contato.nome,
                                color = TextGray,
                                fontSize = 12.sp,
                                maxLines = 1
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
                    tint = TextGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(textDim: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EventNote,
            contentDescription = null,
            tint = textDim.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Cadastre seu primeiro aviso!",
            color = textDim,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

fun getIconForTipo(tipo: TipoManutencao): ImageVector = tipo.getIcon()
