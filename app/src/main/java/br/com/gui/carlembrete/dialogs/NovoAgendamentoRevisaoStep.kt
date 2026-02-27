package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EtapaRevisaoAvisoContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    descricao: String,
    tipoSelecionado: TipoManutencao,
    isModoLista: Boolean,
    listaItensDetectados: List<ItemDetectado>,
    kmBase: String,
    data: String,
    dataAviso: String,
    horaNotificacao: String,
    valorInput: String,
    contatoSelecionado: ContatoProfissional?,
    cidadeAtual: String?,
    ufAtual: String?
) {
    val bgCard = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = accentBlue, modifier = Modifier.size(32.dp))
        Text("Revisar aviso", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinhaResumo("Categoria", tipoSelecionado.label, textPrimary, textSecondary)
            LinhaResumo("Descrição", if (isModoLista) "${listaItensDetectados.size} itens da lista" else descricao.ifBlank { "Não informado" }, textPrimary, textSecondary)
            LinhaResumo("KM", kmBase.ifBlank { "Não informado" }, textPrimary, textSecondary)
            LinhaResumo("Data serviço", data.ifBlank { "Não informado" }, textPrimary, textSecondary)
            LinhaResumo("Data aviso", dataAviso.ifBlank { "Não informado" }, textPrimary, textSecondary)
            LinhaResumo("Hora", horaNotificacao.ifBlank { "Não informado" }, textPrimary, textSecondary)
            LinhaResumo("Valor", if (valorInput.isBlank()) "Não informado" else "R$ $valorInput", textPrimary, textSecondary)
            LinhaResumo("Profissional", contatoSelecionado?.nome ?: "Nenhum vinculado", textPrimary, textSecondary)
            val local = listOfNotNull(cidadeAtual, ufAtual).joinToString(" - ").ifBlank { "Não informado" }
            LinhaResumo("Cidade", local, textPrimary, textSecondary)
        }
    }

    Text(
        text = "Confirme os dados e toque em Cadastrar aviso.",
        color = textSecondary,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LinhaResumo(
    titulo: String,
    valor: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(titulo, color = textSecondary, fontSize = 12.sp, modifier = Modifier.width(96.dp))
        Spacer(Modifier.width(8.dp))
        Text(valor, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
    }
}
