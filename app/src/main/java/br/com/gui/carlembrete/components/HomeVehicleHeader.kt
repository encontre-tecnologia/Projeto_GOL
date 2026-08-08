package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Altura do card do veiculo. */
private val ALTURA_TOPO = 236.dp

/** Cantos do card. Acompanha o raio das acoes rapidas logo abaixo. */
private val RAIO_CARD = 20.dp

/** Fundo quando nao ha foto: escuro neutro, com a cor do veiculo apenas como brilho. */
private val FundoNeutro = Color(0xFF0B1220)

/**
 * Card do veiculo no topo da home: a foto (ou a silhueta, quando nao ha foto) preenche
 * o card, e o nome do veiculo fica sobre ela.
 *
 * O ponto critico do desenho e a legibilidade: foto de usuario chega estourada de sol,
 * escura de garagem ou tremida. Por isso existe um escurecimento fixo no topo e na
 * base, independente da foto — com foto boa fica bonito, com foto ruim continua
 * legivel. Sem isso, metade dos usuarios veria texto branco sobre ceu branco.
 */
@Composable
fun HomeVehicleHeader(
    carro: CarroInfo,
    fotoArquivo: java.io.File?,
    avisosVencidos: Int,
    avisosChegando: Int,
    totalVeiculos: Int,
    indiceVeiculo: Int,
    onAbrirVeiculo: () -> Unit,
    onEscolherFoto: () -> Unit,
    onEditarVeiculo: () -> Unit,
    onVeiculoAnterior: () -> Unit,
    onProximoVeiculo: () -> Unit,
    mostrarAcoes: Boolean = true,
    modifier: Modifier = Modifier
) {
    // O topo e sempre escuro por cima da foto, entao nao depende do tema do app.
    val corVeiculo = carro.getCorUI()

    // A cor do veiculo entra como brilho, nao como fundo: pintar o card inteiro fazia
    // um carro vermelho virar um bloco vermelho no meio de uma tela preta.
    val brilhoDoVeiculo = when {
        corVeiculo == Color.Unspecified -> Color(0xFF3B82F6)
        corVeiculo.luminance() > 0.75f -> lerp(corVeiculo, Color(0xFF60A5FA), 0.55f)
        else -> corVeiculo
    }

    val marcaAno = listOf(
        carro.marca.uppercase().takeIf { it.isNotBlank() },
        extrairAnoDoModeloParaTopo(carro.modelo)
    ).filterNotNull().joinToString(" · ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(ALTURA_TOPO)
            .clip(RoundedCornerShape(RAIO_CARD))
            .background(FundoNeutro)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(RAIO_CARD))
            .pointerInput(totalVeiculos, indiceVeiculo) {
                if (totalVeiculos <= 1) return@pointerInput

                var deslocamentoHorizontal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { deslocamentoHorizontal = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        deslocamentoHorizontal += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            deslocamentoHorizontal <= -72f -> onProximoVeiculo()
                            deslocamentoHorizontal >= 72f -> onVeiculoAnterior()
                        }
                        deslocamentoHorizontal = 0f
                    },
                    onDragCancel = { deslocamentoHorizontal = 0f }
                )
            }
            .clickable { onAbrirVeiculo() }
    ) {
        if (fotoArquivo != null) {
            AsyncImage(
                model = fotoArquivo,
                contentDescription = tr("Foto do ${carro.nome}", "Photo of ${carro.nome}"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(250.dp)
                    .offset(x = 62.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(brilhoDoVeiculo.copy(alpha = 0.34f), Color.Transparent)
                        )
                    )
            )
            // Silhueta ancorada na base direita, sem sair pela lateral: cortada demais
            // ela lia como imagem quebrada em vez de composicao.
            VehicleIcon(
                tipoVeiculo = carro.tipoVeiculo,
                tint = Color.White.copy(alpha = 0.16f),
                size = 188.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 14.dp)
            )
        }

        // Escurecimento constante: e o que garante contraste com qualquer foto.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF060A11).copy(alpha = if (fotoArquivo != null) 0.70f else 0.28f),
                        0.40f to Color(0xFF060A11).copy(alpha = if (fotoArquivo != null) 0.08f else 0.0f),
                        1.0f to Color(0xFF060A11).copy(alpha = if (fotoArquivo != null) 0.92f else 0.78f)
                    )
                )
        )

        // Sempre no canto, nunca sobre o nome. Na primeira versao o convite caia em
        // cima do titulo e empurrava a marca para baixo dele, parecendo erro de layout.
        if (mostrarAcoes) Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 11.dp, end = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Atalho para editar o veiculo sem passar pela garagem: o caminho
            // card -> garagem -> lapis existe, mas sao dois toques a mais para a
            // edicao do proprio veiculo que esta na tela.
            BotaoDoTopo(
                icone = Icons.Rounded.Edit,
                contentDescription = tr("Editar veículo", "Edit vehicle"),
                onClick = onEditarVeiculo
            )
            BotaoDeFoto(
                temFoto = fotoArquivo != null,
                onClick = onEscolherFoto
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
        ) {
            if (marcaAno.isNotBlank()) {
                Text(
                    text = marcaAno,
                    color = Color(0xFF93C5FD),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = carro.nome,
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (carro.kmAtual > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%,d".format(carro.kmAtual).replace(',', '.'),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " km",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
                ResumoDeStatus(vencidos = avisosVencidos, chegando = avisosChegando)
            }

            if (totalVeiculos > 1) {
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(if (totalVeiculos > 8) 3.dp else 4.dp)) {
                    repeat(totalVeiculos) { indice ->
                        val ativo = indice == indiceVeiculo
                        Box(
                            modifier = Modifier
                                .width(
                                    when {
                                        totalVeiculos > 10 && ativo -> 14.dp
                                        totalVeiculos > 10 -> 6.dp
                                        totalVeiculos > 8 && ativo -> 16.dp
                                        totalVeiculos > 8 -> 8.dp
                                        ativo -> 18.dp
                                        else -> 12.dp
                                    }
                                )
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (ativo) Color.White else Color.White.copy(alpha = 0.30f)
                                )
                        )
                    }
                }
            }
        }

        // Canto oposto ao nome, para nao competir com ele. So aparece com placa
        // cadastrada — o campo e opcional.
        val placaCadastrada = normalizarPlaca(carro.placa)
        if (placaCadastrada.isNotBlank()) {
            PlacaMercosul(
                placa = placaCadastrada,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp)
            )
        }
    }
}

/** Azul da tarja da placa Mercosul. */
private val AzulMercosul = Color(0xFF13328C)

/**
 * Placa no desenho da Mercosul: tarja azul com "BRASIL" e a bandeira, e os caracteres
 * em fundo claro embaixo.
 *
 * Nao tenta reproduzir a placa fielmente — o brasao do Mercosul e o QR code ficariam
 * ilegiveis nesse tamanho e viveriam como sujeira. O que faz a peca ser reconhecida e a
 * tarja azul, a palavra BRASIL e o monoespacado escuro; o resto e ruido.
 */
@Composable
private fun PlacaMercosul(placa: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(118.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFF0F172A).copy(alpha = 0.55f), RoundedCornerShape(5.dp))
    ) {
        // Sem altura fixa: a tarja cresce com o texto. Travada em 12dp, a altura de
        // linha do "BRASIL" passava do limite e o clip do card cortava a palavra.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulMercosul)
                .padding(vertical = 2.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MERCOSUL",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 6.6.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Text(
                text = "BRASIL",
                color = Color.White,
                fontSize = 6.6.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 1
            )
            Spacer(Modifier.width(3.dp))
            // Bandeira reduzida ao essencial: verde com o circulo amarelo no meio.
            Box(
                modifier = Modifier
                    .size(width = 9.dp, height = 6.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFF16A34A)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFACC15))
                )
            }
        }
        // A fonte oficial da placa e a FE-Schrift, que nao existe no Android. Peso Black
        // do sans-serif e a aproximacao mais proxima daquele traco grosso e quadrado —
        // monospace ficava fino demais e nao lembrava placa.
        Text(
            text = normalizarPlaca(placa),
            color = Color(0xFF0B1220),
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.8.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, bottom = 3.dp)
        )
    }
}

@Composable
private fun ResumoDeStatus(vencidos: Int, chegando: Int) {
    val (texto, cor, icone) = when {
        vencidos > 0 -> Triple(
            tr("$vencidos vencido${if (vencidos == 1) "" else "s"}", "$vencidos overdue"),
            Color(0xFFF87171),
            Icons.Rounded.ErrorOutline
        )
        chegando > 0 -> Triple(
            tr("$chegando chegando", "$chegando due soon"),
            Color(0xFFFBBF24),
            Icons.Rounded.Schedule
        )
        else -> Triple(
            tr("tudo em dia", "all on track"),
            Color(0xFF34D399),
            Icons.Rounded.CheckCircle
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = cor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = texto, color = cor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Botao redondo de icone unico do topo do card. Mesmo vidro escuro do BotaoDeFoto. */
@Composable
private fun BotaoDoTopo(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF060A11).copy(alpha = 0.52f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icone,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * Botao de foto no canto do topo. Com foto, e so o icone de camera; sem foto, ganha o
 * rotulo "Adicionar foto" para ser descoberto — mas nunca ocupa a area do titulo.
 */
@Composable
private fun BotaoDeFoto(
    temFoto: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF060A11).copy(alpha = 0.52f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = if (temFoto) 7.dp else 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (temFoto) Icons.Outlined.PhotoCamera else Icons.Rounded.AddAPhoto,
            contentDescription = if (temFoto) {
                tr("Trocar foto", "Change photo")
            } else {
                tr("Adicionar foto", "Add photo")
            },
            tint = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.size(15.dp)
        )
        if (!temFoto) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = tr("Adicionar foto", "Add photo"),
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun extrairAnoDoModeloParaTopo(modelo: String): String? =
    Regex("(19|20)\\d{2}").find(modelo)?.value
