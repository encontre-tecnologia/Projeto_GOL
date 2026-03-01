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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AvisosPalette(
    val cardBackground: Color,
    val itemBackground: Color,
    val surfaceHighlight: Color,
    val categoryBadgeBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val isDark: Boolean
)

@Composable
private fun avisosPalette(): AvisosPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return AvisosPalette(
        cardBackground = if (isDark) Color(0xFF334155) else Color.White,
        itemBackground = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        surfaceHighlight = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
        categoryBadgeBorder = if (isDark) Color(0xFF2B3950) else Color.White,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        isDark = isDark
    )
}
private val WhatsAppGreen = Color(0xFF25D366)
private val MoneyGreen = Color(0xFF10B981)

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalFoundationApi::class) // NecessÃ¡rio para remover o Overscroll
@Composable
fun AvisosCategoriasCard(
    lembretesDoCarroAtual: List<Lembrete>,
    lembretesComBusca: List<Lembrete>,
    buscaTexto: String,
    onBuscar: (String) -> Unit,
    listaContatos: List<ContatoProfissional>,
    modeloCarro: String,
    filtroTipo: TipoManutencao?,
    onFiltroTipoChange: (TipoManutencao?) -> Unit,
    categoriasDisponiveis: List<TipoManutencao>,
    iconOverrides: Map<TipoManutencao, ImageVector> = emptyMap(),
    labelOverrides: Map<TipoManutencao, String> = emptyMap(),
    onDelete: (Lembrete) -> Unit,
    onAddPrestador: (Lembrete) -> Unit,
    onOpenDetalhes: (Lembrete) -> Unit,
    statusLabel: (Lembrete) -> String,
    statusColor: (TipoManutencao) -> Color,
    textDim: Color,
    modifier: Modifier = Modifier
) {
    val palette = avisosPalette()
    val cardBorderColor = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.35f)
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
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            border = BorderStroke(1.dp, cardBorderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(palette.cardBackground)
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
                        color = palette.textPrimary,
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
                        val contagem = categoriasDisponiveis.associateWith { tipo ->
                            lembretesDoCarroAtual.count { it.tipo == tipo }
                        }

                        MonitorAllIcon(
                            selected = filtroTipo == null,
                            onClick = { onFiltroTipoChange(null) }
                        )

                        // Ordena: tipos com mais itens aparecem primeiro
                        val categoriasOrdenadas = categoriasDisponiveis
                            .filter { it != TipoManutencao.OUTROS }
                            .sortedByDescending { contagem[it] ?: 0 }

                        categoriasOrdenadas.forEach { tipo ->
                            MonitorIcon(
                                tipo = tipo,
                                cor = statusColor(tipo),
                                quantidade = contagem[tipo] ?: 0,
                                selected = filtroTipo == tipo,
                                onClick = { onFiltroTipoChange(if (filtroTipo == tipo) null else tipo) },
                                iconOverride = iconOverrides[tipo],
                                labelOverride = labelOverrides[tipo]
                            )
                        }
                        if (categoriasDisponiveis.contains(TipoManutencao.OUTROS)) {
                            val tipo = TipoManutencao.OUTROS
                            MonitorIcon(
                                tipo = tipo,
                                cor = statusColor(tipo),
                                quantidade = contagem[tipo] ?: 0,
                                selected = filtroTipo == tipo,
                                onClick = { onFiltroTipoChange(if (filtroTipo == tipo) null else tipo) },
                                iconOverride = iconOverrides[tipo],
                                labelOverride = labelOverrides[tipo]
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
                        val tituloFiltro = labelOverrides[filtroTipo] ?: filtroTipo?.label
                        Text(
                            text = if (tituloFiltro != null) tituloFiltro.uppercase() else "PROXIMOS LEMBRETES:",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (lembretesDoCarroAtual.isNotEmpty()) {
                        val totalBadgeBorder = if (palette.isDark) palette.accent else Color.Black
                        val totalBadgeText = if (palette.isDark) Color.White else Color.Black
                        Surface(
                            color = if (palette.isDark) palette.accent.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, totalBadgeBorder)
                        ) {
                            Text(
                                text = "${lembretesDoCarroAtual.size}",
                                color = totalBadgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.height(8.dp))

                // --- LISTA DE LEMBRETES ---
                if (lembretesComBusca.isEmpty()) {
                    EmptyStateView(palette.textSecondary)
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
    val palette = avisosPalette()
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
        colors = CardDefaults.cardColors(containerColor = palette.itemBackground),
        border = BorderStroke(1.dp, if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.22f)),
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
                        color = palette.textPrimary,
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
                            tint = palette.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        val dataFormatada = try {
                            dataParaOrdenacao(lembrete).format(dateFormatter)
                        } catch (e: Exception) { "--/--" }

                        Text(
                            text = dataFormatada,
                            color = palette.textSecondary,
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
                // BotÃ£o WhatsApp
                if (contato != null && contato.telefone.isNotBlank()) {
                    Surface(
                        color = WhatsAppGreen.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val dataFormatada = try {
                                    dataParaOrdenacao(lembrete).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                } catch (e: Exception) { lembrete.dataLimite.ifBlank { "--/--/----" } }
                                val saudacao = when (LocalTime.now().hour) {
                                    in 5..11 -> "Bom dia"
                                    in 12..17 -> "Boa tarde"
                                    else -> "Boa noite"
                                }
                                val servico = lembrete.titulo.ifBlank { "serviço" }
                                val itemTrocado = lembrete.peca.ifBlank { lembrete.titulo }.ifBlank { "item do serviço" }
                                abrirWhatsApp(
                                    context,
                                    contato.telefone,
                                    "$saudacao, ${contato.nome}! Tudo bem?\n\nFiz a *$servico* com você, do item *$itemTrocado*, no dia *$dataFormatada*.\nGostaria de perguntar se o valor ainda é *$valorFormatado* e quando você teria uma data para realizar esse serviço novamente."
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
                } else if (contato != null) {
                    Surface(
                        color = palette.accent.copy(alpha = 0.15f),
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
                                contentDescription = "Adicionar telefone",
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Adicionar telefone",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = palette.accent.copy(alpha = 0.15f),
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
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Adicionar contato",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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
    onClick: () -> Unit,
    iconOverride: ImageVector? = null,
    labelOverride: String? = null
) {
    val palette = avisosPalette()
    val animatedColor by animateColorAsState(
        targetValue = if (selected) cor else palette.surfaceHighlight,
        animationSpec = tween(durationMillis = 300),
        label = "colorAnim"
    )

    val iconColor = if (selected) Color.White else palette.textSecondary
    val labelText = labelOverride ?: tipo.label

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
                        width = if (selected) 2.dp else 1.5.dp,
                        color = if (selected) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            palette.textSecondary.copy(alpha = 0.35f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconOverride != null) {
                    Icon(
                        imageVector = iconOverride,
                        contentDescription = labelText,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    TipoIcon(tipo = tipo, tint = iconColor, size = 26.dp)
                }
            }

            if (quantidade > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(24.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(2.dp, palette.categoryBadgeBorder, CircleShape),
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
            text = labelText,
            color = if (selected) palette.textPrimary else palette.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MonitorAllIcon(selected: Boolean, onClick: () -> Unit) {
    val palette = avisosPalette()
    val animatedColor by animateColorAsState(
        targetValue = if (selected) palette.accent else palette.surfaceHighlight,
        animationSpec = tween(durationMillis = 300),
        label = "allAnim"
    )
    val iconColor = if (selected) Color.White else palette.textSecondary

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
                    width = if (selected) 2.dp else 1.5.dp,
                    color = if (selected) {
                        Color.White.copy(alpha = 0.2f)
                    } else {
                        palette.textSecondary.copy(alpha = 0.35f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Notifications, "Todos", tint = iconColor, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Todos",
            color = if (selected) palette.textPrimary else palette.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateView(textColor: Color) {
    val palette = avisosPalette()
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
        Text("Tudo 100%!", color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Nenhum aviso nessa categoria.", color = textColor, fontSize = 13.sp)
    }
}


