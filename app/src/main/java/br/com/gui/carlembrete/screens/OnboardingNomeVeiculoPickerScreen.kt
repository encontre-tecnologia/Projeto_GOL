package br.com.gui.carlembrete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OnboardingNomeVeiculoPickerScreen(
    bgLight: Color,
    textPrimary: Color,
    textSecondary: Color,
    selectorBorder: Color,
    selectorAccent: Color,
    marcaSelecionada: String,
    carregandoModelos: Boolean,
    filtroNomeVeiculo: String,
    onFiltroNomeVeiculoChange: (String) -> Unit,
    sugestoesNomeFiltradas: List<FipeModeloDto>,
    onDismiss: () -> Unit,
    onSelectModelo: (FipeModeloDto) -> Unit,
    onSelectNomeManual: (String) -> Unit
) {
    val nomePickerListState = rememberLazyListState()
    val noRippleInteraction = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var buscaInput by remember(filtroNomeVeiculo) { mutableStateOf(filtroNomeVeiculo) }

    val searchFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = selectorAccent,
        unfocusedBorderColor = selectorBorder.copy(alpha = 0.5f),
        focusedContainerColor = bgLight.copy(alpha = 0.5f),
        unfocusedContainerColor = bgLight.copy(alpha = 0.5f),
        cursorColor = selectorAccent,
        focusedLeadingIconColor = textSecondary,
        unfocusedLeadingIconColor = textSecondary,
        focusedPlaceholderColor = textSecondary,
        unfocusedPlaceholderColor = textSecondary,
        focusedTrailingIconColor = selectorAccent,
        unfocusedTrailingIconColor = textSecondary
    )

    val sugestoesOrdenadas = remember(sugestoesNomeFiltradas) {
        sugestoesNomeFiltradas.sortedBy { normalizarTextoBusca(it.nome) }
    }

    // Agrupando para facilitar o uso do StickyHeader
    val itensAgrupados = remember(sugestoesOrdenadas) {
        sugestoesOrdenadas.groupBy {
            normalizarTextoBusca(it.nome).firstOrNull()?.uppercaseChar() ?: '#'
        }
    }

    // Mantendo a lógica da letra atual para o seu Scrollbar customizado
    val letraAtual by remember {
        derivedStateOf {
            val visibleIndex = nomePickerListState.firstVisibleItemIndex
            var currentIndex = 0
            for ((letra, modelos) in itensAgrupados) {
                // +1 por causa do header
                currentIndex += modelos.size + 1
                if (visibleIndex < currentIndex) return@derivedStateOf letra.toString()
            }
            ""
        }
    }

    val canSearch by remember(buscaInput) { derivedStateOf { buscaInput.trim().isNotEmpty() } }

    fun aplicarBusca() {
        onFiltroNomeVeiculoChange(buscaInput.trim())
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    BackHandler(enabled = true, onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            // --- TOP BAR E BUSCA FIXOS NO TOPO ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Voltar",
                            tint = textPrimary
                        )
                    }
                    Text(
                        text = "Escolha seu veículo",
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Barra de busca moderna
                OutlinedTextField(
                    value = buscaInput,
                    onValueChange = {
                        buscaInput = it
                        if (it.isEmpty()) aplicarBusca() // Busca em tempo real ao limpar
                    },
                    placeholder = { Text("Buscar modelo...", color = textSecondary) },
                    singleLine = true,
                    colors = searchFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { aplicarBusca() }),
                    trailingIcon = {
                        IconButton(
                            onClick = { if (canSearch) aplicarBusca() },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canSearch) selectorAccent else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = if (canSearch) Color.White else textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // --- CONTEÚDO SCROLLÁVEL ---
            if (carregandoModelos) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = selectorAccent)
                    Spacer(Modifier.height(12.dp))
                    Text("Carregando modelos...", color = textSecondary)
                }
            } else if (sugestoesNomeFiltradas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = selectorBorder.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, selectorBorder.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Nada encontrado",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tente buscar apenas o modelo base, sem o ano ou versão específica.",
                                color = textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onSelectNomeManual(buscaInput.trim()) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, selectorAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = selectorAccent)
                            ) {
                                Text("Não encontrei meu veículo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = nomePickerListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // Espaço extra no final
                    ) {
                        itensAgrupados.forEach { (letra, modelos) ->
                            stickyHeader {
                                val primeiroNome = primeiroNomeVeiculo(modelos.first().nome)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgLight.copy(alpha = 0.95f)) // Fundo sólido pro sticky header
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(selectorAccent)
                                            .border(1.dp, selectorAccent.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letra.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = primeiroNome,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            items(modelos) { modelo ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = noRippleInteraction,
                                            indication = null
                                        ) { onSelectModelo(modelo) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, selectorBorder.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = modelo.nome,
                                        color = textPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        textAlign = TextAlign.Start,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { onSelectNomeManual(buscaInput.trim()) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, selectorAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = selectorAccent)
                            ) {
                                Text("Não encontrei meu veículo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Seu Scrollbar customizado (assumindo que ele exista no projeto)
                    SimpleLazyScrollbar(
                        state = nomePickerListState,
                        itemCount = sugestoesOrdenadas.size + itensAgrupados.size, // total itens + headers
                        trackColor = selectorBorder.copy(alpha = 0.35f),
                        thumbColor = selectorAccent.copy(alpha = 0.95f),
                        thumbLabel = letraAtual,
                        thumbLabelColor = textPrimary,
                        touchAreaWidth = 56.dp,
                        barWidth = 6.dp,
                        minThumbHeight = 56.dp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

private fun primeiroNomeVeiculo(nomeCompleto: String): String {
    val limpo = nomeCompleto.trim()
    if (limpo.isBlank()) return ""
    return limpo.split(Regex("\\s+")).firstOrNull().orEmpty()
}