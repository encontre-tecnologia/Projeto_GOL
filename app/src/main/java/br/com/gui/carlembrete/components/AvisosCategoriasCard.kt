import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.ContatoProfissional
import br.com.gui.carlembrete.Lembrete
import br.com.gui.carlembrete.TipoIcon
import br.com.gui.carlembrete.TipoManutencao
import br.com.gui.carlembrete.abrirWhatsApp
import br.com.gui.carlembrete.dataParaOrdenacao
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- CORES & FORMATADORES ---
private val CardBackgroundColor = Color(0xFF1E293B)
private val ItemBackgroundColor = Color(0xFF0F172A)
private val SurfaceHighlight = Color(0xFF334155)
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val WhatsAppGreen = Color(0xFF25D366)
private val MoneyGreen = Color(0xFF10B981)

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalFoundationApi::class) // NecessÃ¡rio para remover o Overscroll
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

    // Container Pai com Sombra
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(CardBackgroundColor) // Cor sÃ³lida, sem degradÃª
                    .padding(vertical = 12.dp),
            ) {

                // --- TÃTULO DO CARD ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(start = 8.dp)
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CATEGORIAS",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(Modifier.width(1.dp))
                }

                Spacer(Modifier.height(16.dp))

                // --- CARROSSEL DE FILTROS ---
                // O CompositionLocalProvider aqui remove o efeito de "brilho/elÃ¡stico" ao scrollar
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        val contagem = TipoManutencao.values().associateWith { tipo ->
                            lembretesDoCarroAtual.count { it.tipo == tipo }
                        }

                        MonitorAllIcon(
                            selected = filtroTipo == null,
                            onClick = { onFiltroTipoChange(null) }
                        )

                        // Ordena: tipos com mais itens aparecem primeiro
                        val categoriasOrdenadas = listOf(
                            TipoManutencao.OLEO, TipoManutencao.MECANICA, TipoManutencao.BATERIA,
                            TipoManutencao.FREIO, TipoManutencao.TEMPERATURA, TipoManutencao.LICENCIAMENTO,
                            TipoManutencao.IPVA, TipoManutencao.SEGURO
                        ).sortedByDescending { contagem[it] ?: 0 }

                        categoriasOrdenadas.forEach { tipo ->
                            MonitorIcon(
                                tipo = tipo,
                                cor = statusColor(tipo),
                                quantidade = contagem[tipo] ?: 0,
                                selected = filtroTipo == tipo,
                                onClick = { onFiltroTipoChange(if (filtroTipo == tipo) null else tipo) }
                            )
                        }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // --- CABEÃ‡ALHO DA LISTA ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = if (filtroTipo != null) filtroTipo.label.uppercase() else "PRÃ“XIMOS LEMBRETES",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (lembretesDoCarroAtual.isNotEmpty()) {
                        Surface(
                            color = AccentBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AccentBlue)
                        ) {
                            Text(
                                text = "${lembretesDoCarroAtual.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- LISTA DE LEMBRETES ---
                if (lembretesComBusca.isEmpty()) {
                    EmptyStateView(TextGray)
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        lembretesComBusca.sortedBy { dataParaOrdenacao(it) }.forEach { lembrete ->
                            LembreteCardLocal(
                                lembrete = lembrete,
                                contato = listaContatos.find { it.id == lembrete.contatoId },
                                statusColor = statusColor(lembrete.tipo),
                                onClick = { onOpenDetalhes(lembrete) },
                                onAddPrestador = onAddPrestador
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LembreteCardLocal(
    lembrete: Lembrete,
    contato: ContatoProfissional?,
    statusColor: Color,
    onClick: () -> Unit,
    onAddPrestador: (Lembrete) -> Unit
) {
    val context = LocalContext.current

    val valorFormatado = remember(lembrete.valor) {
        try {
            NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(lembrete.valor)
        } catch (e: Exception) {
            "R$ --"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ItemBackgroundColor),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
            // Ãcone + Texto Principal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TipoIcon(tipo = lembrete.tipo, tint = statusColor, size = 26.dp)
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lembrete.titulo,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        val dataFormatada = try {
                            dataParaOrdenacao(lembrete).format(dateFormatter)
                        } catch (e: Exception) { "--/--" }

                        Text(
                            text = dataFormatada,
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // DivisÃ³ria sutil
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
            Spacer(Modifier.height(16.dp))

            }

            // RodapÃ©: Valor e BotÃ£o
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // Valor (Design Clean)
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "R$",
                            color = MoneyGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Text(
                            text = valorFormatado.replace("R$", "").trim(),
                            color = MoneyGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // BotÃ£o WhatsApp
                if (contato != null) {
                    Surface(
                        color = WhatsAppGreen.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val dataFormatada = try {
                                    dataParaOrdenacao(lembrete).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                } catch (e: Exception) { lembrete.dataLimite.ifBlank { "--/--/----" } }
                                abrirWhatsApp(
                                    context,
                                    contato.telefone,
                                    "Olá ${contato.nome}, tudo bem? Estou entrando em contato sobre o serviço *${lembrete.titulo}* do veículo ${lembrete.carroId}. A data prevista é *$dataFormatada* e o valor estimado é *${valorFormatado}*. Podemos agendar?"
                                )
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Message,
                                contentDescription = "WhatsApp",
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Chamar no WhatsApp",
                                color = WhatsAppGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = AccentBlue.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onAddPrestador(lembrete) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar contato",
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Adicionar contato",
                                color = AccentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ÃCONES DE CATEGORIA ANIMADOS ---

@Composable
fun MonitorIcon(
    tipo: TipoManutencao,
    cor: Color,
    quantidade: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) cor else SurfaceHighlight,
        animationSpec = tween(durationMillis = 300),
        label = "colorAnim"
    )

    val iconColor = if (selected) Color.White else TextGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .border(
                        width = if(selected) 2.dp else 0.dp,
                        color = if(selected) Color.White.copy(alpha=0.2f) else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                TipoIcon(tipo = tipo, tint = iconColor, size = 26.dp)
            }

            if (quantidade > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(24.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(2.dp, CardBackgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$quantidade",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = tipo.label,
            color = if (selected) TextWhite else TextGray,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MonitorAllIcon(selected: Boolean, onClick: () -> Unit) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) AccentBlue else SurfaceHighlight,
        animationSpec = tween(durationMillis = 300),
        label = "allAnim"
    )
    val iconColor = if (selected) Color.White else TextGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(animatedColor)
                .border(
                    width = if(selected) 2.dp else 0.dp,
                    color = if(selected) Color.White.copy(alpha=0.2f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Notifications, "Todos", tint = iconColor, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Todos",
            color = if (selected) TextWhite else TextGray,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateView(textColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(textColor.copy(alpha = 0.05f), CircleShape)
                .border(1.dp, textColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EventNote,
                null,
                tint = textColor.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Tudo 100%!", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Nenhum aviso nessa categoria.", color = textColor, fontSize = 13.sp)
    }
}


