package br.com.gui.carlembrete

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovoCarroScreen(
    onDismiss: () -> Unit,
    onSalvar: (CarroInfo) -> Unit
) {
    CarroFormScreen(
        titulo = "Adicionar veiculo",
        carroAtual = CarroInfo(nome = "", modelo = ""),
        onDismiss = onDismiss,
        onSalvar = onSalvar
    )
}
