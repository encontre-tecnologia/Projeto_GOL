package br.com.gui.carlembrete

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class FipeNamedCode(val name: String, val code: String)
private data class FipeModelsResponse(val models: List<FipeNamedCode> = emptyList())
private data class FipeResult(
    val price: String?,
    val brand: String?,
    val model: String?,
    val modelYear: Int?,
    val fuel: String?,
    val referenceMonth: String?,
    val codeFipe: String?
)

private const val FIPE_BASE_URL = "https://parallelum.com.br/fipe/api/v2"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpvaMultasScreen(
    carroAtual: CarroInfo,
    onDismiss: () -> Unit
) {
    val background = Color(0xFF0F172A)
    val cardColor = Color(0xFF1E293B)
    val textDim = Color(0xFF94A3B8)
    val accentGreen = Color(0xFF22C55E)

    val vehicleType = remember(carroAtual.tipoVeiculo) { mapFipeVehicleType(carroAtual.tipoVeiculo) }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    var brands by remember { mutableStateOf<List<FipeNamedCode>>(emptyList()) }
    var models by remember { mutableStateOf<List<FipeNamedCode>>(emptyList()) }
    var years by remember { mutableStateOf<List<FipeNamedCode>>(emptyList()) }
    var selectedBrand by remember { mutableStateOf<FipeNamedCode?>(null) }
    var selectedModel by remember { mutableStateOf<FipeNamedCode?>(null) }
    var selectedYear by remember { mutableStateOf<FipeNamedCode?>(null) }
    var result by remember { mutableStateOf<FipeResult?>(null) }

    var loadingBrands by remember { mutableStateOf(false) }
    var loadingModels by remember { mutableStateOf(false) }
    var loadingYears by remember { mutableStateOf(false) }
    var loadingResult by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun fetchJson(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.inputStream.bufferedReader().use { it.readText() }
    }

    fun findByName(list: List<FipeNamedCode>, query: String): FipeNamedCode? {
        val q = query.trim().lowercase()
        if (q.isBlank()) return null
        return list.firstOrNull { it.name.lowercase().contains(q) }
    }

    LaunchedEffect(vehicleType) {
        if (vehicleType == null) return@LaunchedEffect
        loadingBrands = true
        errorMessage = null
        result = null
        selectedBrand = null
        selectedModel = null
        selectedYear = null
        models = emptyList()
        years = emptyList()
        try {
            val json = fetchJson("$FIPE_BASE_URL/$vehicleType/brands")
            val listType = object : TypeToken<List<FipeNamedCode>>() {}.type
            brands = gson.fromJson(json, listType)
            val preselect = findByName(brands, carroAtual.marca)
            if (preselect != null) {
                selectedBrand = preselect
            }
        } catch (e: Exception) {
            errorMessage = "Falha ao carregar marcas."
        } finally {
            loadingBrands = false
        }
    }

    LaunchedEffect(selectedBrand) {
        val brand = selectedBrand ?: return@LaunchedEffect
        if (vehicleType == null) return@LaunchedEffect
        loadingModels = true
        errorMessage = null
        result = null
        selectedModel = null
        selectedYear = null
        models = emptyList()
        years = emptyList()
        try {
            val json = fetchJson("$FIPE_BASE_URL/$vehicleType/brands/${brand.code}/models")
            val response = gson.fromJson(json, FipeModelsResponse::class.java)
            models = response.models
            val preselect = findByName(models, carroAtual.modelo)
            if (preselect != null) {
                selectedModel = preselect
            }
        } catch (e: Exception) {
            errorMessage = "Falha ao carregar modelos."
        } finally {
            loadingModels = false
        }
    }

    LaunchedEffect(selectedModel) {
        val brand = selectedBrand ?: return@LaunchedEffect
        val model = selectedModel ?: return@LaunchedEffect
        if (vehicleType == null) return@LaunchedEffect
        loadingYears = true
        errorMessage = null
        result = null
        selectedYear = null
        years = emptyList()
        try {
            val json = fetchJson("$FIPE_BASE_URL/$vehicleType/brands/${brand.code}/models/${model.code}/years")
            val listType = object : TypeToken<List<FipeNamedCode>>() {}.type
            years = gson.fromJson(json, listType)
        } catch (e: Exception) {
            errorMessage = "Falha ao carregar anos."
        } finally {
            loadingYears = false
        }
    }

    suspend fun fetchResult() {
        val brand = selectedBrand ?: return
        val model = selectedModel ?: return
        val year = selectedYear ?: return
        if (vehicleType == null) return
        loadingResult = true
        errorMessage = null
        result = null
        try {
            val json = fetchJson("$FIPE_BASE_URL/$vehicleType/brands/${brand.code}/models/${model.code}/years/${year.code}")
            result = gson.fromJson(json, FipeResult::class.java)
        } catch (e: Exception) {
            errorMessage = "Falha ao consultar FIPE."
        } finally {
            loadingResult = false
        }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tabela FIPE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = accentGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tabela FIPE (Valor de Mercado)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (vehicleType == null) {
                        Text(
                            "Tabela FIPE não disponível para este tipo de veículo.",
                            color = textDim,
                            fontSize = 13.sp
                        )
                    } else {
                        FipeDropdown(
                            label = "Marca",
                            value = selectedBrand?.name ?: "",
                            enabled = !loadingBrands,
                            loading = loadingBrands,
                            items = brands,
                            onSelect = { selectedBrand = it }
                        )

                        FipeDropdown(
                            label = "Modelo",
                            value = selectedModel?.name ?: "",
                            enabled = selectedBrand != null && !loadingModels,
                            loading = loadingModels,
                            items = models,
                            onSelect = { selectedModel = it }
                        )

                        FipeDropdown(
                            label = "Ano / Combustível",
                            value = selectedYear?.name ?: "",
                            enabled = selectedModel != null && !loadingYears,
                            loading = loadingYears,
                            items = years,
                            onSelect = { selectedYear = it }
                        )

                        Button(
                            onClick = {
                                if (!loadingResult && selectedYear != null) {
                                    scope.launch { fetchResult() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedYear != null && !loadingResult,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            if (loadingResult) {
                                CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Consultar Valor", color = Color.White)
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F0F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB91C1C))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage ?: "", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                    }
                }
            }

            result?.let { r ->
                ResultCardFipe(
                    valor = r.price ?: "--",
                    modelo = listOfNotNull(r.brand, r.model).joinToString(" "),
                    mesReferencia = r.referenceMonth ?: "--",
                    codigoFipe = r.codeFipe ?: "--"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FipeDropdown(
    label: String,
    value: String,
    enabled: Boolean,
    loading: Boolean,
    items: List<FipeNamedCode>,
    onSelect: (FipeNamedCode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color(0xFF64748B),
                focusedBorderColor = Color(0xFF334155),
                unfocusedBorderColor = Color(0xFF334155)
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
    if (loading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp, color = Color(0xFF94A3B8))
            Spacer(Modifier.width(8.dp))
            Text("Carregando...", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResultCardFipe(valor: String, modelo: String, mesReferencia: String, codigoFipe: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF122033)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preço Médio Atual",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF22C55E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = modelo.ifBlank { "--" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Referência: $mesReferencia",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "Código FIPE: $codigoFipe",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

private fun mapFipeVehicleType(tipo: TipoVeiculo): String? {
    return when (tipo) {
        TipoVeiculo.CARRO,
        TipoVeiculo.CAMINHONETE,
        TipoVeiculo.CARRETINHA -> "cars"
        TipoVeiculo.MOTO -> "motorcycles"
        TipoVeiculo.CAMINHAO,
        TipoVeiculo.TRATOR -> "trucks"
        TipoVeiculo.BICICLETA -> null
    }
}
