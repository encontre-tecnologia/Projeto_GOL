package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AvisoItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tipo: TipoManutencao? = null,
    val iconOverride: ImageVector? = null,
    val textIcon: String? = null,
    val wide: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun TipoAvisoScreen(
    itensAviso: List<AvisoItem>,
    title: String = tr("O que vamos lembrar?", "What should we remember?"),
    subtitle: String? = null,
    backgroundBrush: Brush,
    surfaceDark: Color,
    textLight: Color,
    textDim: Color,
    onDismiss: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Voltar fica fora da area rolavel e centralizada: centralizado junto, ele
                // desceria do canto e deixaria de ler como botao de voltar.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                contentDescription = tr("Voltar", "Back"),
                                tint = textDim
                            )
                        }
                    }

                    // O weight da altura exata do espaco que sobrou, e o verticalScroll so
                    // relaxa o maximo — o minimo continua sendo esse espaco. E isso que
                    // permite centralizar quando cabe e rolar quando nao cabe.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // Sem o medalhao de 50dp que ficava acima do titulo: numa tela cuja
                    // funcao e escolher entre treze itens, decoracao no topo empurra a
                    // lista para baixo e ninguem escolhe um icone.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            title,
                            color = textLight,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                color = textDim,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .widthIn(max = 620.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val escuro = surfaceDark.luminance() < 0.5f
                        val bordaItem =
                            if (escuro) Color(0xFF334155) else Color.Black.copy(alpha = 0.14f)
                        val fundoItem =
                            if (escuro) Color(0xFF0F172A).copy(alpha = 0.50f)
                            else surfaceDark.copy(alpha = 0.92f)

                        val wideItems = itensAviso.filter { it.wide }
                        val gridItems = itensAviso.filter { !it.wide }

                        wideItems.forEach { item ->
                            ItemDeAviso(
                                item = item,
                                textLight = textLight,
                                fundo = fundoItem,
                                borda = bordaItem,
                                escuro = escuro,
                                destaque = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Os itens largos acima resolvem lugar e hora; os de baixo sao
                        // categoria de servico. Eram taxonomias diferentes empilhadas sem
                        // separacao, e a lista lia como um monte arbitrario de treze cartoes.
                        if (wideItems.isNotEmpty() && gridItems.isNotEmpty()) {
                            Text(
                                text = tr("OU ESCOLHA UMA CATEGORIA", "OR PICK A CATEGORY"),
                                color = textDim,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        gridItems.chunked(2).forEach { linha ->
                            // Item impar sozinho ocupa a linha inteira. Meia largura com um
                            // vao do lado parecia grade quebrada — era o caso do "Outros".
                            if (linha.size == 1) {
                                ItemDeAviso(
                                    item = linha.first(),
                                    textLight = textLight,
                                    fundo = fundoItem,
                                    borda = bordaItem,
                                    escuro = escuro,
                                    destaque = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    linha.forEach { item ->
                                        ItemDeAviso(
                                            item = item,
                                            textLight = textLight,
                                            fundo = fundoItem,
                                            borda = bordaItem,
                                            escuro = escuro,
                                            destaque = false,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

/**
 * Um item da escolha. Antes esse corpo aparecia quatro vezes quase igual, uma para cada
 * combinacao de icone, e as diferencas entre as copias eram acidentais, nao intencionais.
 */
@Composable
private fun ItemDeAviso(
    item: AvisoItem,
    textLight: Color,
    fundo: Color,
    borda: Color,
    escuro: Boolean,
    destaque: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = item.onClick,
        border = BorderStroke(1.dp, borda),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = fundo,
            contentColor = textLight
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.height(if (destaque) 52.dp else 54.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgeDoItem(item = item, escuro = escuro)
            Text(
                item.label,
                color = textLight,
                fontWeight = if (destaque) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (destaque) 15.sp else 14.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Fundo do badge em cinza neutro, com a cor viva apenas no icone.
 *
 * Sao onze matizes diferentes na mesma tela, e elas nao codificam nada — a cor vem fixa
 * por categoria, nao do status do lembrete. Pintando tambem o circulo, a grade lia como
 * confete e atrasava a leitura; so no icone, a area colorida cai muito e a categoria
 * continua reconhecivel pela cor.
 */
@Composable
private fun BadgeDoItem(item: AvisoItem, escuro: Boolean) {
    val fundoBadge =
        if (escuro) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(fundoBadge, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            item.textIcon != null -> Text(
                item.textIcon,
                color = item.color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            item.iconOverride != null -> Icon(
                imageVector = item.iconOverride,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(18.dp)
            )
            item.tipo != null -> TipoIcon(
                tipo = item.tipo,
                tint = item.color,
                size = 18.dp,
                textSize = 12.sp
            )
            else -> Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

