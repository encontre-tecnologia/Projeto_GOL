package br.com.gui.carlembrete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SelecionarPrestadorScreen(
    tipoSelecionado: TipoManutencao,
    isBikeVehicle: Boolean,
    prestadoresCadastrados: List<ContatoProfissional> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmar: (ContatoProfissional) -> Unit
) {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentBlue = Color(0xFF2563EB)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val containerColor = if (isDark) Color(0xFF111827) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f)
    val textFieldBg = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
    val cancelarBorderColor = if (isDark) Color.White else borderColor
    val cancelarTextColor = if (isDark) Color.White else textPrimary

    var nomeInput by rememberSaveable { mutableStateOf("") }
    var telefoneInput by rememberSaveable { mutableStateOf("") }
    var prestadoresExpanded by rememberSaveable { mutableStateOf(false) }

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

    BackHandler(onBack = onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = containerColor,
        tonalElevation = 0.dp,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentBlue.copy(alpha = if (isDark) 0.28f else 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = accentBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tr("Adicionar prestador", "Add provider"),
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (prestadoresOrdenados.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { prestadoresExpanded = true },
                            readOnly = true,
                            enabled = false,
                            label = { Text(tr("Selecione prestador", "Select provider")) },
                            placeholder = { Text(tr("Clique para escolher", "Tap to choose")) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = textSecondary
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                disabledTextColor = textPrimary,
                                disabledLabelColor = textSecondary,
                                disabledPlaceholderColor = textSecondary,
                                disabledTrailingIconColor = textSecondary,
                                disabledContainerColor = textFieldBg,
                                disabledIndicatorColor = borderColor
                            )
                        )
                        DropdownMenu(
                            expanded = prestadoresExpanded,
                            onDismissRequest = { prestadoresExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.78f)
                        ) {
                            prestadoresOrdenados.forEach { prestador ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = prestador.nome,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = prestador.tipoServico.ifBlank { tr("Prestador", "Provider") },
                                                color = textSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        prestadoresExpanded = false
                                        onConfirmar(prestador)
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = borderColor)
                    Text(
                        text = tr("Ou cadastre um novo", "Or add a new one"),
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
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
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, cancelarBorderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = cancelarTextColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    tr("Cancelar", "Cancel"),
                    color = cancelarTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
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
    )
}

private fun formatarTelefoneBr(digitos: String): String {
    val limpo = digitos.filter(Char::isDigit)
    return when (limpo.length) {
        11 -> "(${limpo.substring(0, 2)}) ${limpo.substring(2, 7)}-${limpo.substring(7, 11)}"
        10 -> "(${limpo.substring(0, 2)}) ${limpo.substring(2, 6)}-${limpo.substring(6, 10)}"
        else -> limpo
    }
}
