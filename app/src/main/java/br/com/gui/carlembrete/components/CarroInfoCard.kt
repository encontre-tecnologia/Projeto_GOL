import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gui.carlembrete.CarroInfo
import br.com.gui.carlembrete.TipoVeiculo
import br.com.gui.carlembrete.VehicleIcon
import br.com.gui.carlembrete.iconRes
import br.com.gui.carlembrete.isEnglishUi
import br.com.gui.carlembrete.tr
import coil.compose.AsyncImage
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import androidx.compose.ui.res.painterResource

private data class CarCardPalette(
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val actionBackground: Color,
    val isDark: Boolean
)

private data class VehicleImageResult(
    val url: String,
    val attribution: String = "Fonte: Wikimedia Commons"
)

private const val VEHICLE_IMAGE_TAG = "VehicleImageCard"

private val vehicleImageUrlCache = mutableMapOf<String, VehicleImageResult?>()

@Composable
private fun carCardPalette(): CarCardPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return CarCardPalette(
        cardBackground = if (isDark) Color(0xFF2B3950) else Color.White,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        actionBackground = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.65f),
        isDark = isDark
    )
}

@Composable
fun CarroInfoCard(
    carroAtual: CarroInfo,
    onPrevCar: () -> Unit,
    onNextCar: () -> Unit,
    onOpenCarInfo: () -> Unit,
    onEditCar: () -> Unit,
    onOpenRelatorio: () -> Unit,
    onOpenFuelHistory: () -> Unit,
    showFuelHistoryAction: Boolean = false,
    onNovoLembrete: () -> Unit,
    nomeMantedor: String,
    textLight: Color,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    val palette = carCardPalette()
    val cardBorderColor = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.35f)
    val nomeProprietarioExibido = carroAtual.proprietario
        .trim()
        .ifBlank { nomeMantedor.trim() }
        .split(" ")
        .firstOrNull()
        .orEmpty()
    val anoVeiculo = extrairAnoVeiculo(carroAtual.modelo)
    val modeloSemAno = removerAnoDoModelo(carroAtual.modelo).ifBlank { carroAtual.modelo.ifBlank { "N/A" } }
    val vehicleIconOffsetY = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) (-26).dp else 0.dp
    val modeloAno = listOf(
        modeloSemAno.takeIf { it.isNotBlank() },
        anoVeiculo
    ).filterNotNull().joinToString(" - ")
    val marcaTexto = carroAtual.marca.uppercase().ifBlank { "N/A" }
    // Cores DinÃƒÂ¢micas baseadas no carro (Apenas para o card interno do carro)
    val baseColor = carroAtual.getCorUI()
    val isVeiculoBranco = isCorVeiculoBranca(baseColor)
    val heroTextColor = if (isVeiculoBranco) Color(0xFF0F172A) else Color.White
    val carDisplayGradientStart = if (isVeiculoBranco) Color(0xFFFFFFFF) else lerp(Color(0xFF1E293B), baseColor, 0.55f)
    val carDisplayGradientEnd = if (isVeiculoBranco) Color(0xFFF3F4F6) else lerp(Color(0xFF0B1224), baseColor, 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.5f))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            // AQUI ESTÃƒÂ A MUDANÃƒâ€¡A: Usamos uma cor sÃƒÂ³lida para o container
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            border = BorderStroke(1.dp, cardBorderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Garante que o fundo seja uniforme
                    .background(palette.cardBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. HERO CARD DO CARRO (Visual do Carro)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .clip(RoundedCornerShape(22.dp))
                        // O degradÃƒÂª fica SÃƒâ€œ aqui dentro, na imagem do carro
                        .background(
                            Brush.linearGradient(
                                colors = listOf(carDisplayGradientStart, carDisplayGradientEnd),
                                start = Offset(0f, 0f),
                                end = Offset(0f, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(
                            1.dp,
                            if (isVeiculoBranco) Color(0xFF0F172A).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(22.dp)
                        )
                        .clickable { onOpenCarInfo() }
                ) {
                    // --- WATERMARKS (Background decorativo) ---
                    DecoracoesDeFundo(baseColor)

                    // --- CONTEÃƒÅ¡DO PRINCIPAL ---
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // CABEÃ‡ALHO (Nome)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = carroAtual.nome,
                                    fontSize = 24.sp,
                                    color = heroTextColor,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = modeloAno,
                                    fontSize = 12.sp,
                                    color = heroTextColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = marcaTexto,
                                    fontSize = 11.sp,
                                    color = heroTextColor.copy(alpha = 0.82f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.offset(y = (-2).dp)
                                )
                            }
                        }

                        // CENTRO (Logo com Glow)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glow effect atrÃƒÂ¡s do logo
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(baseColor.copy(alpha = 0.3f), Color.Transparent)
                                        )
                                    )
                            )

                            VehicleIcon(
                                tipoVeiculo = carroAtual.tipoVeiculo,
                                tint = heroTextColor,
                                size = 210.dp,
                                modifier = Modifier.offset(y = vehicleIconOffsetY)
                            )

                            Text(
                                text = if (isEnglishUi()) "Vehicle owner: $nomeProprietarioExibido" else "Veiculo de: $nomeProprietarioExibido",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 20.dp),
                                color = heroTextColor.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                            )

                        }
                    }
                }

                // 2. PAINEL DE AÃƒâ€¡Ãƒâ€¢ES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionGlassButton(
                        icon = Icons.Rounded.Edit,
                        label = tr("Editar", "Edit"),
                        modifier = Modifier.weight(1f),
                        onClick = onEditCar
                    )
                    ActionGlassButton(
                        icon = Icons.Rounded.Description,
                        label = tr("Relatorio", "Report"),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRelatorio
                    )
                }

                // 3. BOTÃƒÆ’O PRINCIPAL (Wide)
                Button(
                    onClick = onNovoLembrete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = palette.accent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Event, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr("Novo Lembrete", "New Reminder"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun extrairAnoVeiculo(modelo: String): String? {
    val match = Regex("(19|20)\\d{2}(?:/(19|20)\\d{2})?").find(modelo)
    return match?.value
}

private fun removerAnoDoModelo(modelo: String): String {
    return modelo
        .replace(Regex("(19|20)\\d{2}(?:/(19|20)\\d{2})?"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

private fun isCorVeiculoBranca(cor: Color): Boolean {
    return cor.red >= 0.95f && cor.green >= 0.95f && cor.blue >= 0.95f
}

// Sub-componente de decoraÃƒÂ§ÃƒÂµes
@Composable
fun DecoracoesDeFundo(color: Color) {
    Box(Modifier.fillMaxSize())
}

@Composable
fun ActionGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = carCardPalette()
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            // Um tom ligeiramente diferente do fundo sÃƒÂ³lido para destacar o botÃƒÂ£o
            containerColor = palette.actionBackground,
            contentColor = palette.textSecondary
        ),
        border = BorderStroke(1.5.dp, palette.textPrimary.copy(alpha = 0.2f)),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = palette.textPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


private fun resolveVehicleImage(
    nome: String,
    marca: String,
    modelo: String,
    ano: String?,
    tipo: TipoVeiculo
): VehicleImageResult? {
    val normalizedKey = listOf(nome, marca, modelo, ano.orEmpty(), tipo.name)
        .joinToString("|")
        .trim()
        .lowercase()
    vehicleImageUrlCache[normalizedKey]?.let {
        Log.d(VEHICLE_IMAGE_TAG, "cache hit key='$normalizedKey' hasUrl=${it?.url != null}")
        return it
    }

    val tipoHint = when (tipo) {
        TipoVeiculo.VAN -> "van"
        TipoVeiculo.ONIBUS -> "bus"
        TipoVeiculo.MOTORHOME -> "motorhome"
        TipoVeiculo.CAMINHAO -> "truck"
        TipoVeiculo.MOTO -> "motorcycle"
        TipoVeiculo.BICICLETA, TipoVeiculo.BIKE_ELETRICA -> "bicycle"
        else -> "car"
    }
    val queries = listOf(
        listOf(marca, nome, ano.orEmpty(), tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(marca, nome, tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(marca, nome).filter { it.isNotBlank() }.joinToString(" "),
        listOf(nome, marca).filter { it.isNotBlank() }.joinToString(" "),
        listOf(nome, tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(nome, marca, modelo, ano.orEmpty(), tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(nome, marca, modelo, tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(nome, modelo, tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(marca, modelo, ano.orEmpty(), tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(marca, modelo, tipoHint).filter { it.isNotBlank() }.joinToString(" "),
        listOf(modelo, tipoHint).filter { it.isNotBlank() }.joinToString(" ")
    ).distinct().filter { it.isNotBlank() }

    var result: VehicleImageResult? = null
    for (query in queries) {
        Log.d(VEHICLE_IMAGE_TAG, "trying query='$query'")
        result = fetchWikimediaVehicleImage(query = query)
        if (result != null) break
    }

    vehicleImageUrlCache[normalizedKey] = result
    Log.d(VEHICLE_IMAGE_TAG, "resolved key='$normalizedKey' result='${result?.url ?: "null"}'")
    return result
}

private fun fetchWikimediaVehicleImage(query: String): VehicleImageResult? {
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
    val url =
        "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=12&prop=imageinfo&iiprop=url&iiurlwidth=900"
    return runCatching {
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "CarLembrete/1.0 (vehicle image lookup)")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.getInputStream().bufferedReader().use { reader ->
            val root = JSONObject(reader.readText())
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return@runCatching null
            val keys = pages.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val page = pages.optJSONObject(key) ?: continue
                val title = page.optString("title", "")
                val titleLower = title.lowercase()
                if (titleLower.contains("logo") || titleLower.contains("badge") || titleLower.contains("emblem")) continue
                if (!isRelevantWikimediaTitle(title = title, query = query)) {
                    Log.d(VEHICLE_IMAGE_TAG, "wikimedia skip irrelevant title='$title' query='$query'")
                    continue
                }
                val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val thumb = imageInfo.optString("thumburl")
                if (thumb.isNotBlank()) {
                    Log.d(VEHICLE_IMAGE_TAG, "wikimedia match query='$query' title='$title' thumb='$thumb'")
                    return@runCatching VehicleImageResult(url = thumb)
                }
                val full = imageInfo.optString("url")
                if (full.isNotBlank()) {
                    Log.d(VEHICLE_IMAGE_TAG, "wikimedia match query='$query' title='$title' full='$full'")
                    return@runCatching VehicleImageResult(url = full)
                }
            }
            null
        }
    }.onFailure { Log.e(VEHICLE_IMAGE_TAG, "wikimedia request failed query='$query': ${it.message}", it) }.getOrNull()
}

private fun isRelevantWikimediaTitle(title: String, query: String): Boolean {
    val normalizedTitle = title.lowercase()
        .replace("file:", "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    val blockedWords = setOf(
        "police", "policia", "street", "avenue", "traffic", "protest", "riot",
        "city", "town", "zone", "vista", "north", "south", "east", "west"
    )
    if (blockedWords.any { normalizedTitle.contains(it) }) return false

    val ignoredTerms = setOf(
        "car", "truck", "bus", "van", "motorhome", "motorcycle", "bicycle",
        "bike", "suv", "hatch", "pickup", "sedan", "vehicle", "veiculo", "de", "do", "da"
    )
    val queryTerms = query.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .split(" ")
        .map { it.trim() }
        .filter { it.length >= 3 && it !in ignoredTerms && !it.all(Char::isDigit) }
        .distinct()

    if (queryTerms.isEmpty()) return true
    return queryTerms.any { term ->
        normalizedTitle.contains(term)
    }
}

