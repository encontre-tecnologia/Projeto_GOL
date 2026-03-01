package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EtapaProfissionaisContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    iconColor: Color,
    accentBlue: Color,
    cidadeAtual: String?,
    ufAtual: String?,
    carregandoProfissionaisCidade: Boolean,
    erroProfissionaisCidade: String?,
    profissionaisDaCidade: List<ProfissionalCidadeEncontrado>,
    profissionaisListState: LazyListState,
    loadingAlpha: Float,
    contatoSelecionado: ContatoProfissional?,
    onRecarregar: () -> Unit,
    onVerNoGoogle: (String) -> Unit,
    onAdicionarDaCidade: (ProfissionalCidadeEncontrado) -> Unit
) {
    val bgCard = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val bgLocation = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD6E0EA)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Build, contentDescription = null, tint = accentBlue, modifier = Modifier.size(28.dp))
            }
            Text("Selecione um prestador", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        val localLabel = listOfNotNull(cidadeAtual, ufAtual).joinToString(" - ")
        if (localLabel.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgLocation)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(localLabel, color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.weight(1f))

                if (!carregandoProfissionaisCidade) {
                    IconButton(
                        onClick = onRecarregar,
                        modifier = Modifier.size(24.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = accentBlue)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (contatoSelecionado != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF052E16) else Color(0xFFF0FDF4))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Prestador selecionado: ${contatoSelecionado.nome}",
                    color = if (isDark) Color(0xFFDCFCE7) else Color(0xFF166534),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!carregandoProfissionaisCidade && !erroProfissionaisCidade.isNullOrBlank()) {
            Text(erroProfissionaisCidade, color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        } else if (!carregandoProfissionaisCidade && profissionaisDaCidade.isEmpty() && localLabel.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.SearchOff, contentDescription = null, tint = textSecondary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    "Nenhum profissional encontrado.\nTente atualizar ou cadastre manualmente.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        val mostrarAreaResultados = carregandoProfissionaisCidade || profissionaisDaCidade.isNotEmpty()
        if (mostrarAreaResultados) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(dividerColor))
                Text(
                    text = "Lugares próximos a você",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(dividerColor))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 860.dp)
            ) {
                if (carregandoProfissionaisCidade) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(end = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bgCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp).alpha(loadingAlpha),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)))
                                        Spacer(Modifier.width(10.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)))
                                            Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)))
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = profissionaisListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 40.dp)
                    ) {
                        items(profissionaisDaCidade) { contatoCidade ->
                            val telefoneSelecionado = contatoSelecionado?.telefone?.filter(Char::isDigit).orEmpty()
                            val telefoneAtual = contatoCidade.telefone.filter(Char::isDigit)
                            val isSelecionado =
                                contatoSelecionado != null &&
                                    (
                                        contatoSelecionado.nome.equals(contatoCidade.nome, ignoreCase = true) ||
                                            (telefoneSelecionado.isNotBlank() && telefoneSelecionado == telefoneAtual)
                                        )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bgCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelecionado) Color(0xFF22C55E) else borderColor
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSelecionado) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selecionado",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 10.dp, end = 10.dp)
                                                .size(18.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(bgLocation),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Build, contentDescription = null, tint = accentBlue, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = contatoCidade.nome,
                                                    color = textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (contatoCidade.telefone.isBlank()) {
                                                    Text("Telefone não informado", color = Color(0xFFF59E0B), fontSize = 12.sp)
                                                } else {
                                                    Text(contatoCidade.telefone, color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }

                                        if (contatoCidade.endereco.isNotBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = "Endereço",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = contatoCidade.endereco,
                                                    color = textSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onVerNoGoogle(contatoCidade.nome) },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.18f) else Color(0xFFD6E0EA))
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Search,
                                                        contentDescription = "Ver no Google",
                                                        tint = textPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Ver no Google", color = textPrimary, fontSize = 12.sp)
                                                }
                                            }
                                            Button(
                                                onClick = { onAdicionarDaCidade(contatoCidade) },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = accentBlue,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        "Selecionar",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
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
    }
}
