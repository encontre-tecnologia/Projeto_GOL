package br.com.gui.carlembrete

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Motorcycle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.Serializable
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

/* ----------------- ESTRUTURAS DE DADOS ----------------- */

data class ItemDetectado(
    val id: String = UUID.randomUUID().toString(),
    var nome: String,
    var tipo: TipoManutencao,
    var valor: Double = 0.0,
    var dataFutura: String = "",
    var quantidade: Int = 1
)

data class ResultadoCaptura(
    val arquivoFoto: File,
    val itensEncontrados: List<ItemDetectado>,
    val kmDetectado: Int?,
    val qrCodeUrl: String? = null,
    val sugestoesProduto: List<String> = emptyList(),
    val linhasReconhecidas: List<String> = emptyList()
)

data class Lembrete(
    val id: String = UUID.randomUUID().toString(),
    val carroId: String,
    val contatoId: String? = null,
    val titulo: String,
    val peca: String = "",
    val dataLimite: String,
    val kmLimite: String,
    val tipo: TipoManutencao,
    val valor: Double = 0.0,
    val fotoPath: String? = null,
    val horaAviso: String = "09:00",
    val estabelecimentoNome: String = "",
    val estabelecimentoEndereco: String = ""
) : Serializable

// Logos removidos por segurança legal. Use ícones genéricos por tipo de veículo.

enum class TipoVeiculo(val label: String, val icon: ImageVector) {
    BICICLETA("Bicicleta", Icons.Rounded.DirectionsBike),
    CARRETINHA("Carretinha", Icons.Rounded.Inventory2),
    CARRO("Carro", Icons.Rounded.DirectionsCar),
    MOTO("Moto", Icons.Rounded.Motorcycle),
    CAMINHONETE("Caminhonete", Icons.Rounded.LocalShipping),
    CAMINHAO("Caminhao", Icons.Rounded.LocalShipping),
    TRATOR("Trator", Icons.Rounded.Agriculture)
}

val marcasSuportadas = listOf(
    "Audi",
    "BMW",
    "Citroën",
    "Citroen",
    "Citroem",
    "Chevrolet",
    "Fiat",
    "Ford",
    "Honda",
    "Hyundai",
    "Jeep",
    "Kia",
    "Lamborghini",
    "Mercedes-Benz",
    "Mitsubishi",
    "Nissan",
    "Nissam",
    "Peugeot",
    "RAM",
    "Renault",
    "Toyota",
    "Volkswagen"
).map { it.trim() }.filter { it.isNotEmpty() }

val marcasBicicleta = listOf(
    "Caloi",
    "Monark",
    "Houston",
    "Oggi",
    "Sense",
    "Audax",
    "Soul Cycles",
    "Cannondale",
    "Specialized",
    "Trek"
)

val marcasCaminhao = listOf(
    "Mercedes-Benz",
    "Volkswagen",
    "Scania",
    "Volvo",
    "IVECO",
    "DAF",
    "Ford",
    "MAN"
)

data class CarroInfo(
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "Novo Veículo",
    val modelo: String = "Modelo 1.0",
    val marca: String = "",
    val proprietario: String = "",
    val corArgb: Int = 0xFF3B82F6.toInt(),
    val kmAtual: Int = 0,
    val tipoVeiculo: TipoVeiculo = TipoVeiculo.CARRO
) : Serializable {
    fun getCorUI(): Color = Color(corArgb)
}

fun CarroInfo.tipoIcon(): ImageVector = tipoVeiculo.icon

@DrawableRes
fun TipoVeiculo.iconRes(): Int? = when (this) {
    TipoVeiculo.CARRO -> R.drawable.ic_carro
    TipoVeiculo.CAMINHAO -> R.drawable.ic_caminhao
    TipoVeiculo.CAMINHONETE -> R.drawable.ic_camionete
    TipoVeiculo.MOTO -> R.drawable.ic_moto
    TipoVeiculo.CARRETINHA -> R.drawable.ic_carreta
    TipoVeiculo.TRATOR -> R.drawable.ic_trator
    else -> null
}

@Composable
fun VehicleIcon(
    tipoVeiculo: TipoVeiculo,
    tint: Color? = null,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val res = tipoVeiculo.iconRes()
    if (res != null) {
        Image(
            painter = painterResource(id = res),
            contentDescription = contentDescription ?: tipoVeiculo.label,
            modifier = modifier.size(size),
            colorFilter = if (tint != null) ColorFilter.tint(tint) else null
        )
    } else {
        Icon(
            imageVector = tipoVeiculo.icon,
            contentDescription = contentDescription ?: tipoVeiculo.label,
            tint = tint ?: Color.Unspecified,
            modifier = modifier.size(size)
        )
    }
}

private fun normalizarMarca(marca: String): String {
    val semAcentos = Normalizer.normalize(marca.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.getDefault())
    return semAcentos.replace(Regex("[^a-z0-9]"), "")
}

@DrawableRes
fun logoResForMarca(marca: String): Int? = null

@DrawableRes
fun logoResForMarca(marca: String, tipoVeiculo: TipoVeiculo?): Int? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipoVeiculoSelector(
    selecionado: TipoVeiculo?,
    onSelect: (TipoVeiculo) -> Unit,
    lightStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF3B82F6)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TipoVeiculo.values().forEach { tipo ->
            val selected = tipo == selecionado
            val bgColor = when {
                selected -> accent
                lightStyle -> Color.White
                else -> Color(0xFF0B1224)
            }
            val borderColor = when {
                selected -> accent
                lightStyle -> Color.Black
                else -> Color.White.copy(alpha = 0.12f)
            }
            val textColor = when {
                selected -> Color.White
                lightStyle -> Color(0xFF0F172A)
                else -> Color(0xFF94A3B8)
            }
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = borderColor
                ),
                modifier = Modifier
                    .clickable { onSelect(tipo) }
            ) {
                Text(
                    text = tipo.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

data class ContatoProfissional(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val telefone: String,
    val tipoServico: String
) : Serializable

data class Abastecimento(
    val id: String = UUID.randomUUID().toString(),
    val carroId: String,
    val data: String,
    val precoLitro: Double,
    val valorPago: Double,
    val litros: Double
) : Serializable

data class Pedalada(
    val id: String = UUID.randomUUID().toString(),
    val carroId: String,
    val data: String,
    val km: Double
) : Serializable

enum class TipoManutencao(val label: String) {
    CORRENTE("Corrente"),
    LUBRIFICACAO("Lubrificação"),
    PEDIVELA("Pedivela"),
    ACESSORIOS("Acessórios"),
    CONFORTO("Conforto"),
    PNEU("Pneu"),
    TRANSMISSAO("Transmissão"),
    REVISAO("Revisão"),
    OLEO("Óleo"),
    BATERIA("Bateria"),
    MECANICA("Mecânica"),
    FREIO("Freio"),
    LICENCIAMENTO("Licença"),
    IPVA("IPVA"),
    SEGURO("Seguro"),
    OUTROS("Outros");

    fun getIcon(): ImageVector = when(this) {
        CORRENTE -> Icons.Rounded.Link
        LUBRIFICACAO -> Icons.Rounded.WaterDrop
        PEDIVELA -> Icons.Rounded.DirectionsBike
        ACESSORIOS -> Icons.Rounded.Inventory2
        CONFORTO -> Icons.Rounded.Chair
        PNEU -> Icons.Rounded.TireRepair
        TRANSMISSAO -> Icons.Rounded.Settings
        REVISAO -> Icons.Rounded.Description
        OLEO -> Icons.Rounded.WaterDrop
        BATERIA -> Icons.Rounded.BatteryChargingFull
        MECANICA -> Icons.Rounded.Build
        FREIO -> Icons.Rounded.DiscFull
        LICENCIAMENTO -> Icons.Rounded.Description
        IPVA -> Icons.Rounded.Payments
        SEGURO -> Icons.Rounded.Shield
        OUTROS -> Icons.Rounded.Edit
    }
}

