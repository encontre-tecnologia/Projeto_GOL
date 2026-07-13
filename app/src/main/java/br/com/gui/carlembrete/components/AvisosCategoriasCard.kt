import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.ContatoProfissional
import br.com.gui.carlembrete.Lembrete
import br.com.gui.carlembrete.TipoIcon
import br.com.gui.carlembrete.TipoManutencao
import br.com.gui.carlembrete.abrirWhatsApp
import br.com.gui.carlembrete.dataParaOrdenacao
import br.com.gui.carlembrete.isEnglishUi
import br.com.gui.carlembrete.tr
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
        cardBackground = if (isDark) Color(0xFF0D1117) else Color(0xFFF8FAFC),
        itemBackground = if (isDark) Color(0xFF131A27) else Color(0xFFFFFFFF),
        surfaceHighlight = if (isDark) Color(0xFF1A2236) else Color(0xFFE2E8F0),
        categoryBadgeBorder = if (isDark) Color(0xFF0D1117) else Color.White,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        isDark = isDark
    )
}

private val WhatsAppGreen = Color(0xFF25D366)
private val MoneyGreen = Color(0xFF10B981)
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalFoundationApi::class)
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
    accentColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val palette = avisosPalette()
    val cardBorderColor = if (palette.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
    val scrollState = rememberScrollState()

    val glowColor = when {
        accentColor == Color.Unspecified -> Color(0xFF3B82F6)
        accentColor.luminance() > 0.75f -> Color(0xFFCBD5E1)
        else -> accentColor
    }

    val cardGradient = if (palette.isDark) {
        Brush.verticalGradient(
            0.0f to glowColor.copy(alpha = 0.18f),
            0.25f to glowColor.copy(alpha = 0.06f),
            1.0f to palette.cardBackground
        )
    } else {
        Brush.verticalGradient(
            0.0f to glowColor.copy(alpha = 0.08f),
            0.20f to palette.cardBackground,
            1.0f to palette.cardBackground
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = glowColor.copy(alpha = 0.30f),
                ambientColor = glowColor.copy(alpha = 0.12f)
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
                    .padding(vertical = 12.dp)
            ) {

                // ── Header: Categorias ───────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("CATEGORIAS", "CATEGORIES"),
                        color = if (!palette.isDark) Color(0xFF0F172A) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Carrossel de filtros ─────────────────────────────────
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
                                glowColor = glowColor,
                                onClick = { onFiltroTipoChange(null) }
                            )

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

                Spacer(Modifier.height(28.dp))

                // ── Header: Próximos lembretes ───────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tituloFiltro = filtroTipo?.let { tipo ->
                        labelOverrides[tipo] ?: tipo.localizedLabel()
                    }
                    Text(
                        text = if (tituloFiltro != null)
                            tituloFiltro.uppercase()
                        else
                            tr("PROXIMOS LEMBRETES:", "UPCOMING:"),
                        color = if (!palette.isDark) Color(0xFF0F172A) else palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    if (lembretesDoCarroAtual.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    glowColor.copy(alpha = if (palette.isDark) 0.22f else 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${lembretesDoCarroAtual.size}",
                                color = if (palette.isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Lista de lembretes ───────────────────────────────────
                if (lembretesComBusca.isEmpty()) {
                    EmptyStateView(palette.textSecondary)
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
                                onClick = { onOpenDetalhes(lembrete) },
                                onAddPrestador = onAddPrestador
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
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
    val isEnglish = isEnglishUi()

    val valorFormatado = remember(lembrete.valor) {
        try {
            NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(lembrete.valor)
        } catch (e: Exception) {
            "R$ --"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.itemBackground),
        border = BorderStroke(1.dp, if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // Faixa lateral colorida com status
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(statusColor, statusColor.copy(alpha = 0.35f))
                        ),
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(statusColor.copy(alpha = 0.14f), CircleShape)
                                .border(1.dp, statusColor.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            TipoIcon(tipo = lembrete.tipo, tint = statusColor, size = 24.dp)
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lembrete.titulo,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = palette.textSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
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

                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                if (palette.isDark) Color.White.copy(alpha = 0.08f)
                                else Color.Black.copy(alpha = 0.06f)
                            )
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Rodapé: ação + valor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val ufAviso = lembrete.kmLimite.trim().uppercase(Locale("pt", "BR"))
                    if (lembrete.tipo == TipoManutencao.IPVA || lembrete.tipo == TipoManutencao.LICENCIAMENTO) {
                        val textoAcao = if (lembrete.tipo == TipoManutencao.IPVA) tr("Renovar IPVA", "Renew IPVA") else tr("Renovar Licença", "Renew License")
                        ActionChip(
                            icon = Icons.Default.OpenInNew,
                            label = textoAcao,
                            color = palette.accent
                        ) { abrirPortalEstado(context, lembrete.tipo, ufAviso) }
                    } else if (lembrete.tipo == TipoManutencao.SEGURO) {
                        ActionChip(
                            icon = Icons.Default.OpenInNew,
                            label = tr("Cotar seguro", "Get quote"),
                            color = palette.accent
                        ) { abrirCotacaoSeguro(context, lembrete) }
                    } else if (contato != null && contato.telefone.isNotBlank()) {
                        ActionChip(
                            icon = Icons.Rounded.Message,
                            label = tr("WhatsApp", "WhatsApp"),
                            color = WhatsAppGreen
                        ) { abrirWhatsApp(context, contato.telefone, "Olá tudo bem?") }
                    } else {
                        ActionChip(
                            icon = Icons.Default.Add,
                            label = tr("Adicionar contato", "Add contact"),
                            color = palette.accent
                        ) { onAddPrestador(lembrete) }
                    }

                    // Valor
                    Box(
                        modifier = Modifier
                            .background(
                                MoneyGreen.copy(alpha = if (palette.isDark) 0.14f else 0.10f),
                                RoundedCornerShape(9.dp)
                            )
                            .border(1.dp, MoneyGreen.copy(alpha = 0.30f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Ícones de categoria ──────────────────────────────────────────────────────

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
    val labelText = labelOverride ?: tipo.localizedLabel()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(listOf(cor.copy(alpha = 0.30f), Color.Transparent))
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) cor.copy(alpha = 0.50f)
                               else palette.textSecondary.copy(alpha = 0.25f),
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
                        .size(22.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                        .border(2.dp, palette.categoryBadgeBorder, CircleShape),
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

        Spacer(Modifier.height(7.dp))
        Text(
            text = labelText,
            color = if (!palette.isDark) Color(0xFF0F172A) else Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TipoManutencao.localizedLabel(): String = when (this) {
    TipoManutencao.CORRENTE -> tr("Corrente", "Chain")
    TipoManutencao.LUBRIFICACAO -> tr("Lubrificação", "Lubrication")
    TipoManutencao.PEDIVELA -> tr("Pedivela", "Crankset")
    TipoManutencao.ACESSORIOS -> tr("Acessórios", "Accessories")
    TipoManutencao.CONFORTO -> tr("Conforto", "Comfort")
    TipoManutencao.PNEU -> tr("Pneu", "Tire")
    TipoManutencao.TRANSMISSAO -> tr("Transmissão", "Transmission")
    TipoManutencao.REVISAO -> tr("Revisão", "Checkup")
    TipoManutencao.OLEO -> tr("Óleo", "Oil")
    TipoManutencao.LAVAGEM -> tr("Lavagem", "Wash")
    TipoManutencao.ABASTECIMENTO -> tr("Posto", "Fuel")
    TipoManutencao.BATERIA -> tr("Elétrica", "Electric")
    TipoManutencao.VIDROS -> tr("Vidros", "Glass")
    TipoManutencao.MECANICA -> tr("Mecânica", "Mechanical")
    TipoManutencao.FUNILARIA -> tr("Funilaria", "Bodywork")
    TipoManutencao.FREIO -> tr("Freio", "Brake")
    TipoManutencao.LICENCIAMENTO -> tr("Licença", "License")
    TipoManutencao.IPVA -> tr("IPVA", "IPVA")
    TipoManutencao.SEGURO -> tr("Seguro", "Insurance")
    TipoManutencao.OUTROS -> tr("Outros", "Others")
}

@Composable
private fun MonitorAllIcon(
    selected: Boolean,
    glowColor: Color,
    onClick: () -> Unit
) {
    val palette = avisosPalette()
    val animatedColor by animateColorAsState(
        targetValue = if (selected) glowColor else palette.surfaceHighlight,
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
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(listOf(glowColor.copy(alpha = 0.30f), Color.Transparent))
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) glowColor.copy(alpha = 0.50f)
                               else palette.textSecondary.copy(alpha = 0.25f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, tr("Todos", "All"), tint = iconColor, modifier = Modifier.size(26.dp))
            }
        }

        Spacer(Modifier.height(7.dp))
        Text(
            text = tr("Todos", "All"),
            color = if (!palette.isDark) Color(0xFF0F172A) else Color.White,
            fontSize = 14.sp,
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
                .background(textColor.copy(alpha = 0.06f), CircleShape)
                .border(1.dp, textColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EventNote,
                null,
                tint = textColor.copy(alpha = 0.35f),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = tr("Nenhum aviso por aqui", "No reminders here"),
            color = palette.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = tr(
                "Quando precisar acompanhar uma revisão, troca ou manutenção, toque em Novo Lembrete.",
                "When you need to track a service, replacement, or maintenance, tap New Reminder."
            ),
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
    }
}

private fun abrirPortalEstado(context: android.content.Context, tipo: TipoManutencao, uf: String) {
    val termo = when (tipo) {
        TipoManutencao.IPVA -> "IPVA"
        TipoManutencao.LICENCIAMENTO -> "Licenciamento"
        else -> "IPVA"
    }
    val ufFinal = uf.takeIf { it.length == 2 && it.all(Char::isLetter) } ?: "SP"
    val query = "$termo $ufFinal site oficial"
    val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private fun abrirCotacaoSeguro(context: android.content.Context, lembrete: Lembrete) {
    val query = "cotacao seguro auto ${lembrete.titulo}"
    val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}
