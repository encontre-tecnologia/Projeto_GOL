import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
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
        cardBackground = if (isDark) Color(0xFF0D1117) else Color(0xFFF8FAFC),
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        actionBackground = if (isDark) Color(0xFF161D2B) else Color(0xFFEFF2F7),
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
    onEditButtonPositioned: (Rect) -> Unit = {},
    onReportButtonPositioned: (Rect) -> Unit = {},
    onNewReminderButtonPositioned: (Rect) -> Unit = {},
    nomeMantedor: String,
    textLight: Color,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    val palette = carCardPalette()
    val cardBorderColor = if (palette.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.08f)
    val nomeProprietarioExibido = run {
        val partesNome = carroAtual.proprietario
            .trim()
            .ifBlank { nomeMantedor.trim() }
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
        when (partesNome.size) {
            0 -> ""
            1 -> partesNome.first()
            else -> "${partesNome.first()} ${partesNome.last()}"
        }
    }
    val anoVeiculo = extrairAnoVeiculo(carroAtual.modelo)
    val modeloSemAno = removerAnoDoModelo(carroAtual.modelo).ifBlank { carroAtual.modelo.ifBlank { "N/A" } }
    val vehicleIconOffsetY = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) (-26).dp else 0.dp
    val modeloPrincipalExibicao = if (carroAtual.tipoVeiculo == TipoVeiculo.BICICLETA) {
        if (modeloSemAno.startsWith("Aro", ignoreCase = true)) modeloSemAno else "Aro: $modeloSemAno"
    } else {
        modeloSemAno
    }
    val modeloAno = listOf(
        modeloPrincipalExibicao.takeIf { it.isNotBlank() },
        anoVeiculo
    ).filterNotNull().joinToString(" · ")
    val marcaTexto = carroAtual.marca.uppercase().ifBlank { "" }
    val baseColor = carroAtual.getCorUI()

    val zelluCardBlue = Color(0xFF2F80ED)

    // Use a vivid fallback if the vehicle color is too light/white.
    val glowColor = if (baseColor == Color.Unspecified) {
        zelluCardBlue
    } else if (baseColor.luminance() > 0.75f) {
        Color(0xFF60A5FA)
    } else {
        baseColor
    }
    val heroAccentColor = if (!palette.isDark && glowColor.luminance() < 0.35f) {
        lerp(glowColor, Color(0xFF60A5FA), 0.38f)
    } else {
        glowColor
    }

    val heroTextColor = if (palette.isDark) Color.White else Color(0xFF0F172A)

    // Dynamic gradient based on vehicle accent color
    val heroGradient = if (palette.isDark) {
        Brush.verticalGradient(
            0.0f to heroAccentColor.copy(alpha = 0.62f),
            0.40f to heroAccentColor.copy(alpha = 0.22f),
            1.0f to Color(0xFF020510)
        )
    } else {
        Brush.verticalGradient(
            0.0f to heroAccentColor.copy(alpha = 0.88f),
            0.48f to heroAccentColor.copy(alpha = 0.50f),
            1.0f to heroAccentColor.copy(alpha = 0.25f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = heroAccentColor.copy(alpha = 0.40f),
                ambientColor = heroAccentColor.copy(alpha = 0.18f)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            border = BorderStroke(1.dp, cardBorderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.cardBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Hero ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush = heroGradient)
                        .clickable { onOpenCarInfo() }
                ) {
                    // Decorative glows
                    DecoracoesDeFundo(heroAccentColor)

                    // Content
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Name + model
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 22.dp, start = 12.dp, end = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = carroAtual.nome,
                                fontSize = 26.sp,
                                color = heroTextColor,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = (-0.5).sp
                            )
                            if (modeloAno.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = modeloAno,
                                    fontSize = 12.sp,
                                    color = heroTextColor.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        // Vehicle icon + brand
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Radial glow behind icon
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                heroTextColor.copy(alpha = if (palette.isDark) 0.10f else 0.14f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            VehicleIcon(
                                tipoVeiculo = carroAtual.tipoVeiculo,
                                tint = heroTextColor,
                                size = 210.dp,
                                modifier = Modifier.offset(y = vehicleIconOffsetY)
                            )

                            if (marcaTexto.isNotBlank()) {
                                Text(
                                    text = marcaTexto,
                                    color = heroTextColor.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 14.dp)
                                )
                            }
                        }
                    }

                    // KM chip — bottom-left
                    if (carroAtual.kmAtual > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 14.dp, bottom = 14.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.30f),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    heroTextColor.copy(alpha = 0.20f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "%,d km".format(carroAtual.kmAtual).replace(',', '.'),
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Ações ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionGlassButton(
                        icon = Icons.Rounded.Edit,
                        label = tr("Editar", "Edit"),
                        accentColor = heroAccentColor,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { onEditButtonPositioned(it.boundsInRoot()) },
                        onClick = onEditCar
                    )
                    ActionGlassButton(
                        icon = Icons.Rounded.Description,
                        label = tr("Relatorio", "Report"),
                        accentColor = heroAccentColor,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { onReportButtonPositioned(it.boundsInRoot()) },
                        onClick = onOpenRelatorio
                    )
                }

                // ── Novo Lembrete ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            16.dp,
                            RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF3B82F6).copy(alpha = 0.55f),
                            ambientColor = Color(0xFF3B82F6).copy(alpha = 0.20f)
                        )
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNovoLembrete() }
                        .onGloballyPositioned { onNewReminderButtonPositioned(it.boundsInRoot()) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Event,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tr("Novo Lembrete", "New Reminder"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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

@Composable
fun DecoracoesDeFundo(color: Color) {
    val safeColor = if (color == Color.Unspecified || color.luminance() > 0.75f) Color(0xFF3B82F6) else color
    Box(Modifier.fillMaxSize()) {
        // Top-left blob
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        listOf(safeColor.copy(alpha = 0.40f), Color.Transparent)
                    )
                )
        )
        // Bottom-right blob
        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .background(
                    Brush.radialGradient(
                        listOf(safeColor.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
        )
        // Center subtle ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun ActionGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color = Color(0xFF3B82F6),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = carCardPalette()
    val borderColor = if (palette.isDark)
        accentColor.copy(alpha = 0.22f)
    else
        Color.Black.copy(alpha = 0.10f)

    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.actionBackground,
            contentColor = palette.textSecondary
        ),
        border = BorderStroke(1.dp, borderColor),
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
                tint = palette.textPrimary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary.copy(alpha = 0.85f),
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
