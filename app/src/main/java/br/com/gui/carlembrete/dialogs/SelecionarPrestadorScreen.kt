package br.com.gui.carlembrete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SelecionarPrestadorScreen(
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean,
    prestadoresCadastrados: List<ContatoProfissional> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmar: (ContatoProfissional) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val accentBlue = Color(0xFF2563EB)
    val whatsAppGreen = Color(0xFF25D366)
    val sponsorAmber = Color(0xFFF59E0B)
    val screenBg = if (isDark) Color.Black else colorScheme.background
    val cardBg = if (isDark) Color(0xFF111827) else colorScheme.surface
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textFieldBg = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)

    var nomeInput by rememberSaveable { mutableStateOf("") }
    var telefoneInput by rememberSaveable { mutableStateOf("") }
    var prestadoresPatrocinados by remember { mutableStateOf<List<PrestadorPatrocinado>>(emptyList()) }

    LaunchedEffect(tipoSelecionado) {
        val (cidade, estado) = runCatching {
            withContext(Dispatchers.IO) { resolverCidadeEstadoAtual(context) }
        }.getOrNull() ?: (null to null)
        PrestadoresPatrocinadosSync.buscar(tipoSelecionado.label, cidade, estado) { lista ->
            prestadoresPatrocinados = lista
        }
    }

    val telefoneDigitos = telefoneInput.filter(Char::isDigit).take(11)
    val nomeValido = nomeInput.trim().length >= 2
    val telefoneValido = telefoneDigitos.length >= 10
    val podeSalvar = nomeValido && telefoneValido
    val prestadoresOrdenados = prestadoresCadastrados
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<ContatoProfissional> {
                it.tipoServico.equals(tipoSelecionado.label, ignoreCase = true)
            }.thenBy { it.nome.lowercase() }
        )
    val prestadoresNaLista = remember(prestadoresOrdenados, prestadoresPatrocinados) {
        val itens = prestadoresOrdenados.map { it as Any }.toMutableList()
        prestadoresPatrocinados
            .sortedBy { it.posicao }
            .forEach { patrocinado ->
                val indice = (patrocinado.posicao - 1).coerceIn(0, itens.size)
                itens.add(indice, patrocinado)
            }
        itens
    }

    BackHandler(onBack = onDismiss)

    val sectionHeaderBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Scaffold(containerColor = screenBg) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = tr("Voltar", "Back"),
                        tint = textPrimary
                    )
                }
                Text(
                    text = tr("Prestador do serviço", "Service provider"),
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            if (prestadoresNaLista.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                                .background(sectionHeaderBg)
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr("Já cadastrados", "Already registered"),
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            prestadoresNaLista.forEach { item ->
                                val patrocinado = item as? PrestadorPatrocinado
                                val cadastrado = item as? ContatoProfissional
                                val nome = patrocinado?.nome ?: cadastrado?.nome.orEmpty()
                                val telefone = patrocinado?.telefone ?: cadastrado?.telefone.orEmpty()
                                val tipoServico = patrocinado?.tipoServico ?: cadastrado?.tipoServico.orEmpty()
                                val corDestaque = if (patrocinado != null) sponsorAmber else accentBlue
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (patrocinado != null) {
                                                sponsorAmber.copy(alpha = if (isDark) 0.08f else 0.06f)
                                            } else if (isDark) {
                                                Color.White.copy(alpha = 0.04f)
                                            } else {
                                                Color.Black.copy(alpha = 0.03f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (patrocinado != null) sponsorAmber.copy(alpha = 0.35f)
                                            else cardBorder.copy(alpha = 0.6f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (patrocinado != null) {
                                                PrestadoresPatrocinadosSync.registrarClique(patrocinado.id)
                                                onConfirmar(
                                                    ContatoProfissional(
                                                        nome = patrocinado.nome,
                                                        telefone = patrocinado.telefone,
                                                        tipoServico = patrocinado.tipoServico
                                                    )
                                                )
                                            } else if (cadastrado != null) {
                                                onConfirmar(cadastrado)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(corDestaque.copy(alpha = if (isDark) 0.24f else 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (patrocinado != null) Icons.Default.Star else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = corDestaque,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = nome,
                                            color = textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (patrocinado != null) {
                                                listOfNotNull(
                                                    tipoServico.ifBlank { null },
                                                    patrocinado.cidade,
                                                    tr("Patrocinado", "Sponsored")
                                                ).joinToString(" • ")
                                            } else {
                                                tipoServico.ifBlank { tr("Prestador", "Provider") }
                                            },
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (telefone.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(whatsAppGreen, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_whatsapp),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                            .background(sectionHeaderBg)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr("Cadastrar novo prestador", "Register a new provider"),
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(color = cardBorder.copy(alpha = 0.55f))
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = nomeInput,
                            onValueChange = { nomeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Nome *", "Name *")) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = textFieldBg,
                                unfocusedContainerColor = textFieldBg,
                                disabledContainerColor = textFieldBg
                            )
                        )
                        OutlinedTextField(
                            value = telefoneInput,
                            onValueChange = { novo ->
                                telefoneInput = novo.filter { it.isDigit() || it == ' ' || it == '(' || it == ')' || it == '-' || it == '+' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Telefone *", "Phone *")) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = textFieldBg,
                                unfocusedContainerColor = textFieldBg,
                                disabledContainerColor = textFieldBg
                            )
                        )
                        Text(
                            text = tr("Campos obrigatórios marcados com *", "Required fields are marked with *"),
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                val contato = ContatoProfissional(
                                    nome = nomeInput.trim(),
                                    telefone = formatarTelefoneBr(telefoneDigitos),
                                    tipoServico = tipoSelecionado.label
                                )
                                onConfirmar(contato)
                            },
                            enabled = podeSalvar,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(tr("Salvar prestador", "Save provider"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatarTelefoneBr(digitos: String): String {
    val limpo = digitos.filter(Char::isDigit)
    return when (limpo.length) {
        11 -> "(${limpo.substring(0, 2)}) ${limpo.substring(2, 7)}-${limpo.substring(7, 11)}"
        10 -> "(${limpo.substring(0, 2)}) ${limpo.substring(2, 6)}-${limpo.substring(6, 10)}"
        else -> limpo
    }
}
