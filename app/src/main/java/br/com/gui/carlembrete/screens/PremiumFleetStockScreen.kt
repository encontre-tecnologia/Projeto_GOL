package br.com.gui.carlembrete

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

private const val FLEET_STOCK_PREFS = "fleet_stock_prefs"
private const val KEY_FLEET_STOCK_ITEMS = "fleet_stock_items_json"
private const val KEY_FLEET_STOCK_MOVEMENTS = "fleet_stock_movements_json"
private const val FLEET_STOCK_TAG = "FleetStock"

private data class FleetStockItem(
    val id: String,
    val name: String,
    val barcode: String,
    val category: String,
    val quantity: Int,
    val minimum: Int,
    val avgPrice: Double
)

private data class FleetStockMovement(
    val id: String,
    val itemId: String,
    val itemName: String,
    val type: String,
    val quantity: Int,
    val source: String,
    val createdAtMillis: Long,
    val details: String
)

private enum class FleetStockFilter { ALL, LOW, ZERO }
private enum class HistoryPeriodMode { MONTH, YEAR }

private data class FleetCategorySummary(
    val key: String,
    val label: String,
    val totalUnits: Int,
    val totalItems: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PremiumFleetStockScreen(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (isDark) Color.Black else scheme.background
    val cardBg = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val border = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
    val accentBlue = Color(0xFF2563EB)
    val accentOrange = Color(0xFFEA580C)
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    val stockItems = remember { mutableStateListOf<FleetStockItem>() }
    var showItemDialog by remember { mutableStateOf(false) }
    var editItemId by remember { mutableStateOf<String?>(null) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var prefillName by remember { mutableStateOf("") }
    var prefillBarcode by remember { mutableStateOf("") }
    var prefillCategory by remember { mutableStateOf("") }
    var prefillQuantity by remember { mutableStateOf("1") }
    var prefillAvgPrice by remember { mutableStateOf("") }
    val movements = remember { mutableStateListOf<FleetStockMovement>() }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf(FleetStockFilter.ALL) }
    var showProductsScreen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        stockItems.clear()
        stockItems.addAll(loadFleetStockItems(context))
        movements.clear()
        movements.addAll(loadFleetStockMovements(context))
        Log.d(FLEET_STOCK_TAG, "Tela aberta. itensCarregados=${stockItems.size}")
    }

    fun persist() {
        saveFleetStockItems(context, stockItems)
        Log.d(FLEET_STOCK_TAG, "Persistido estoque. totalItens=${stockItems.size}")
    }

    fun persistMovements() {
        saveFleetStockMovements(context, movements)
        Log.d(FLEET_STOCK_TAG, "Persistido historico. totalMovimentos=${movements.size}")
    }

    fun registerMovement(
        item: FleetStockItem,
        type: String,
        quantity: Int,
        source: String,
        details: String = ""
    ) {
        if (quantity <= 0) return
        movements.add(
            0,
            FleetStockMovement(
                id = UUID.randomUUID().toString(),
                itemId = item.id,
                itemName = item.name,
                type = type,
                quantity = quantity,
                source = source,
                createdAtMillis = System.currentTimeMillis(),
                details = details
            )
        )
        if (movements.size > 500) {
            movements.removeRange(500, movements.size)
        }
        persistMovements()
        Log.i(
            FLEET_STOCK_TAG,
            "Movimento registrado. itemId=${item.id} tipo=$type qtd=$quantity origem=$source"
        )
    }

    fun openCreateDialog(
        nome: String = "",
        barcode: String = "",
        categoria: String = "",
        quantidade: Int = 1,
        valorMedio: Double? = null
    ) {
        editItemId = null
        prefillName = nome
        prefillBarcode = barcode
        prefillCategory = categoria
        prefillQuantity = quantidade.coerceAtLeast(1).toString()
        prefillAvgPrice = valorMedio?.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
        Log.d(
            FLEET_STOCK_TAG,
            "Abrindo cadastro item. prefillNome=$nome prefillBarcode=$barcode prefillCategoria=$categoria prefillQtd=$prefillQuantity prefillValor=$prefillAvgPrice"
        )
        showItemDialog = true
    }

    fun openEditDialog(itemId: String) {
        editItemId = itemId
        Log.d(FLEET_STOCK_TAG, "Abrindo edicao itemId=$itemId")
        showItemDialog = true
    }

    fun onBarcodeDetected(rawCode: String) {
        val normalized = rawCode.filter(Char::isDigit)
        Log.d(FLEET_STOCK_TAG, "Codigo detectado. raw=$rawCode normalized=$normalized")
        if (normalized.isBlank()) return
        val existingIndex = stockItems.indexOfFirst { it.barcode == normalized && it.barcode.isNotBlank() }
        if (existingIndex >= 0) {
            val atual = stockItems[existingIndex]
            stockItems[existingIndex] = atual.copy(quantity = atual.quantity + 1)
            registerMovement(
                item = stockItems[existingIndex],
                type = "ENTRADA",
                quantity = 1,
                source = "SCAN_CODIGO",
                details = "Incremento automatico por codigo de barras"
            )
            persist()
            Log.i(
                FLEET_STOCK_TAG,
                "Codigo existente. itemId=${atual.id} nome=${atual.name} quantidadeNova=${stockItems[existingIndex].quantity}"
            )
            Toast.makeText(context, "Item encontrado. Estoque +1.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.i(FLEET_STOCK_TAG, "Codigo novo. abrindo cadastro para normalized=$normalized")
        openCreateDialog(
            nome = "Item escaneado",
            barcode = normalized,
            quantidade = 1
        )
    }

    val lowStockCount = stockItems.count { it.quantity <= it.minimum }
    val totalUnits = stockItems.sumOf { it.quantity }
    val normalizedSearch = searchQuery.trim().lowercase(Locale.ROOT)
    val normalizedSelectedCategory = selectedCategory?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val displayedItems = stockItems
        .filter { item ->
            val matchesSearch = normalizedSearch.isBlank() ||
                item.name.lowercase(Locale.ROOT).contains(normalizedSearch) ||
                item.barcode.contains(normalizedSearch)
            val matchesFilter = when (stockFilter) {
                FleetStockFilter.ALL -> true
                FleetStockFilter.LOW -> item.quantity <= item.minimum
                FleetStockFilter.ZERO -> item.quantity == 0
            }
            val itemCategoryKey = normalizeStockCategoryKey(item.category)
            val matchesCategory = normalizedSelectedCategory.isBlank() || itemCategoryKey == normalizedSelectedCategory
            matchesSearch && matchesFilter && matchesCategory
        }
        .sortedBy { it.name.lowercase(Locale.ROOT) }

    val defaultCategories = defaultFleetStockCategoryKeys()
    val categorySummaries = defaultCategories.map { categoryKey ->
        val items = stockItems.filter { normalizeStockCategoryKey(it.category) == categoryKey }
        FleetCategorySummary(
            key = categoryKey,
            label = stockCategoryLabel(categoryKey),
            totalUnits = items.sumOf { it.quantity },
            totalItems = items.size,
            icon = iconForCategory(categoryKey)
        )
    }
    val visibleCategories = categorySummaries
    val filteredMovements = movements.filter { movement ->
        isMovementInCurrentPeriod(movement.createdAtMillis, HistoryPeriodMode.MONTH)
    }
    val historyPeriodLabel = currentHistoryPeriodLabel(HistoryPeriodMode.MONTH)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val pagerScope = rememberCoroutineScope()

    Scaffold(containerColor = bg) { innerPadding ->
        if (showProductsScreen) {
            FleetStockProductsContent(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                selectedCategory = selectedCategory,
                displayedItems = displayedItems,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                stockFilter = stockFilter,
                onStockFilterChange = { stockFilter = it },
                textPrimary = textPrimary,
                textDim = textDim,
                accentBlue = accentBlue,
                border = border,
                cardBg = cardBg,
                currency = currency,
                lowStockColor = accentOrange,
                onBack = { showProductsScreen = false },
                onIncrease = { item ->
                    val idx = stockItems.indexOfFirst { it.id == item.id }
                    if (idx >= 0) {
                        stockItems[idx] = stockItems[idx].copy(quantity = stockItems[idx].quantity + 1)
                        registerMovement(
                            item = stockItems[idx],
                            type = "ENTRADA",
                            quantity = 1,
                            source = "MANUAL",
                            details = "Botao Repor"
                        )
                        persist()
                    }
                },
                onDecrease = { item ->
                    val idx = stockItems.indexOfFirst { it.id == item.id }
                    if (idx >= 0) {
                        val novoValor = (stockItems[idx].quantity - 1).coerceAtLeast(0)
                        val delta = stockItems[idx].quantity - novoValor
                        stockItems[idx] = stockItems[idx].copy(quantity = novoValor)
                        if (delta > 0) {
                            registerMovement(
                                item = stockItems[idx],
                                type = "SAIDA",
                                quantity = delta,
                                source = "MANUAL",
                                details = "Botao Baixar"
                            )
                        }
                        persist()
                    }
                },
                onEdit = { item -> openEditDialog(item.id) },
                onDelete = { item ->
                    stockItems.removeAll { it.id == item.id }
                    persist()
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = tr("Voltar", "Back"),
                        tint = textPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = accentBlue)
                }
                Text(
                    tr("Estoque da Frota", "Fleet Stock"),
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Controle itens, codigo e reposicao", "Manage items, barcode and replenishment"),
                    color = textDim,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = { showAddOptionsDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text(tr("Adicionar produto ao estoque", "Add product to stock"))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = tr("Itens cadastrados", "Registered items"),
                    value = stockItems.size.toString(),
                    textPrimary = textPrimary,
                    textDim = textDim,
                    cardBg = cardBg,
                    border = border,
                    accent = accentBlue,
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = tr("Baixo estoque", "Low stock"),
                    value = lowStockCount.toString(),
                    textPrimary = textPrimary,
                    textDim = textDim,
                    cardBg = cardBg,
                    border = border,
                    accent = Color(0xFFEA580C),
                    icon = Icons.Default.Remove,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(0) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pagerState.currentPage == 0) accentBlue else accentBlue.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(tr("Categorias", "Categories"))
                }
                Button(
                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(1) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pagerState.currentPage == 1) accentBlue else accentBlue.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(tr("Historico", "History"))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = accentBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            tr("Categorias do estoque", "Stock categories"),
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }

                                    if (visibleCategories.isEmpty()) {
                                        Text(
                                            tr("Sem itens para exibir categorias.", "No items to show categories."),
                                            color = textDim,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        visibleCategories.chunked(2).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowItems.forEach { category ->
                                                    CategorySquareCard(
                                                        summary = category,
                                                        textPrimary = textPrimary,
                                                        textDim = textDim,
                                                        cardBg = cardBg,
                                                        border = border,
                                                        accentBlue = accentBlue,
                                                        modifier = Modifier.weight(1f),
                                                        onClick = {
                                                            selectedCategory = category.key
                                                            searchQuery = ""
                                                            stockFilter = FleetStockFilter.ALL
                                                            showProductsScreen = true
                                                        }
                                                    )
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = accentBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            tr("Historico de movimentacoes", "Movement history"),
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Text(
                                        tr("Filtro ativo: $historyPeriodLabel", "Active filter: $historyPeriodLabel"),
                                        color = textDim,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = accentBlue.copy(alpha = 0.1f)),
                                            border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.22f)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                tr("Visualizacao mensal (fixa)", "Monthly view (fixed)"),
                                                color = textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = accentBlue.copy(alpha = 0.08f)),
                                        border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.24f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                tr(
                                                    "Registros no periodo",
                                                    "Records in period"
                                                ),
                                                color = textDim,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                filteredMovements.size.toString(),
                                                color = textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    if (filteredMovements.isEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = cardBg),
                                            border = BorderStroke(1.dp, border),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                tr(
                                                    "Nenhuma movimentacao registrada neste periodo.",
                                                    "No movements recorded in this period."
                                                ),
                                                color = textDim,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    } else {
                                        filteredMovements.forEach { movement ->
                                            val isEntrada = movement.type.equals("ENTRADA", ignoreCase = true)
                                            val typeColor = when (movement.type.uppercase(Locale.ROOT)) {
                                                "ENTRADA" -> Color(0xFF16A34A)
                                                "SAIDA" -> Color(0xFFEA580C)
                                                else -> accentBlue
                                            }
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f)),
                                                border = BorderStroke(1.dp, typeColor.copy(alpha = 0.24f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(11.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.weight(1f),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .background(typeColor.copy(alpha = 0.16f), RoundedCornerShape(6.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isEntrada) Icons.Default.Add else Icons.Default.Remove,
                                                                    contentDescription = null,
                                                                    tint = typeColor,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                            Text(
                                                                movement.itemName,
                                                                color = textPrimary,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Card(
                                                            colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.18f)),
                                                            shape = RoundedCornerShape(999.dp)
                                                        ) {
                                                            Text(
                                                                formatTimestamp(movement.createdAtMillis),
                                                                color = textPrimary,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = typeColor.copy(alpha = 0.2f))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            formatMovementType(movement.type),
                                                            color = typeColor,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            "${if (isEntrada) "+" else "-"}${movement.quantity}",
                                                            color = typeColor,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        tr(
                                                            "Origem: ${movement.source}",
                                                            "Source: ${movement.source}"
                                                        ),
                                                        color = textPrimary.copy(alpha = 0.72f),
                                                        fontSize = 11.sp
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

            Spacer(modifier = Modifier.height(12.dp))
        }
        }
    }

    if (showItemDialog) {
        val editing = stockItems.firstOrNull { it.id == editItemId }
        FleetStockItemDialog(
            initialItem = editing,
            prefillName = if (editing == null) prefillName else "",
            prefillBarcode = if (editing == null) prefillBarcode else "",
            prefillCategory = if (editing == null) prefillCategory else "",
            prefillQuantity = if (editing == null) prefillQuantity else "",
            prefillAvgPrice = if (editing == null) prefillAvgPrice else "",
            onDismiss = { showItemDialog = false },
            onSave = { savedItem ->
                val normalizedBarcode = savedItem.barcode.filter(Char::isDigit)
                if (editing == null) {
                    val duplicateIndex = stockItems.indexOfFirst {
                        normalizedBarcode.isNotBlank() && it.barcode == normalizedBarcode
                    }
                    if (duplicateIndex >= 0) {
                        val atual = stockItems[duplicateIndex]
                        val incremento = savedItem.quantity.coerceAtLeast(0)
                        val novoItem = atual.copy(quantity = atual.quantity + incremento)
                        stockItems[duplicateIndex] = novoItem
                        registerMovement(
                            item = novoItem,
                            type = "ENTRADA",
                            quantity = incremento,
                            source = "MANUAL",
                            details = "Cadastro unido por codigo de barras duplicado"
                        )
                        Toast.makeText(context, "Codigo ja cadastrado. Quantidade somada no item existente.", Toast.LENGTH_SHORT).show()
                    } else {
                        val novoItem = savedItem.copy(
                            id = UUID.randomUUID().toString(),
                            barcode = normalizedBarcode
                        )
                        stockItems.add(novoItem)
                        registerMovement(
                            item = novoItem,
                            type = "ENTRADA",
                            quantity = novoItem.quantity.coerceAtLeast(0),
                            source = "MANUAL",
                            details = "Cadastro de novo item"
                        )
                    }
                } else {
                    val duplicateIndex = stockItems.indexOfFirst {
                        it.id != editing.id && normalizedBarcode.isNotBlank() && it.barcode == normalizedBarcode
                    }
                    if (duplicateIndex >= 0) {
                        Toast.makeText(context, "Codigo de barras ja usado por outro item.", Toast.LENGTH_SHORT).show()
                        return@FleetStockItemDialog
                    }
                    val idx = stockItems.indexOfFirst { it.id == editing.id }
                    if (idx >= 0) {
                        val previous = stockItems[idx]
                        val updated = savedItem.copy(id = editing.id, barcode = normalizedBarcode)
                        stockItems[idx] = updated
                        val delta = updated.quantity - previous.quantity
                        when {
                            delta > 0 -> registerMovement(
                                item = updated,
                                type = "ENTRADA",
                                quantity = delta,
                                source = "MANUAL",
                                details = "Edicao de quantidade"
                            )
                            delta < 0 -> registerMovement(
                                item = updated,
                                type = "SAIDA",
                                quantity = -delta,
                                source = "MANUAL",
                                details = "Edicao de quantidade"
                            )
                        }
                    }
                }
                persist()
                showItemDialog = false
            }
        )
    }

    if (showAddOptionsDialog) {
        val addDialogAccent = Color(0xFF2563EB)
        val addDialogCardBg = addDialogAccent.copy(alpha = 0.08f)
        AlertDialog(
            onDismissRequest = { showAddOptionsDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(addDialogAccent.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = addDialogAccent, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        tr("Como deseja cadastrar?", "How do you want to add?"),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = addDialogCardBg),
                        border = BorderStroke(1.dp, addDialogAccent.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            tr(
                                "Escolha a forma mais rapida para adicionar um item ao estoque.",
                                "Choose the fastest way to add an item to stock."
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    AddItemOptionButton(
                        onClick = {
                            showAddOptionsDialog = false
                            openCreateDialog()
                        },
                        title = tr("Cadastro manual", "Manual entry"),
                        subtitle = tr("Preencha nome, codigo, categoria e estoque", "Fill in name, code, category and stock"),
                        icon = Icons.Default.Edit,
                        containerColor = addDialogAccent,
                        contentColor = Color.White
                    )
                    AddItemOptionButton(
                        onClick = {
                            showAddOptionsDialog = false
                            showQrScannerDialog = true
                        },
                        title = tr("Escanear QR da nota", "Scan invoice QR"),
                        subtitle = tr("Importa itens direto da nota fiscal", "Import items directly from invoice"),
                        icon = Icons.Default.QrCodeScanner,
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color.White
                    )
                    AddItemOptionButton(
                        onClick = {
                            showAddOptionsDialog = false
                            showBarcodeScannerDialog = true
                        },
                        title = tr("Escanear codigo de barras", "Scan barcode"),
                        subtitle = tr("Le o codigo e agiliza a reposicao", "Read barcode and speed up replenishment"),
                        icon = Icons.Default.LinearScale,
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { showAddOptionsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, addDialogAccent.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = addDialogAccent
                        )
                    ) {
                        Text(tr("Fechar", "Close"))
                    }
                }
            }
        )
    }

    if (showBarcodeScannerDialog) {
        Log.d(FLEET_STOCK_TAG, "Dialog scanner codigo aberto")
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScannerDialog = false },
            onBarcodeDetected = { code ->
                showBarcodeScannerDialog = false
                Log.d(FLEET_STOCK_TAG, "Dialog scanner codigo retornou code=$code")
                onBarcodeDetected(code)
            }
        )
    }

    if (showQrScannerDialog) {
        Log.d(FLEET_STOCK_TAG, "Dialog scanner QR aberto")
        CameraCapturaDialog(
            onDismiss = { showQrScannerDialog = false },
            onFotoCapturada = { resultado ->
                showQrScannerDialog = false
                Log.d(FLEET_STOCK_TAG, "Resultado QR recebido. temNota=${resultado.notaQrInfo != null} qrUrl=${resultado.qrCodeUrl}")
                val nota = resultado.notaQrInfo
                if (nota == null) {
                    Log.w(FLEET_STOCK_TAG, "Nota nula no retorno do scanner QR")
                    Toast.makeText(context, "Nota nao encontrada.", Toast.LENGTH_SHORT).show()
                    return@CameraCapturaDialog
                }
                val itens = extrairItensDaDescricaoQr(nota.descricaoItens)
                Log.d(FLEET_STOCK_TAG, "Itens extraidos da nota: total=${itens.size}")
                if (itens.isEmpty()) {
                    Log.w(FLEET_STOCK_TAG, "Nenhum item detectado na nota. descricao=${nota.descricaoItens}")
                    Toast.makeText(context, "Nenhum item detectado na nota.", Toast.LENGTH_SHORT).show()
                    return@CameraCapturaDialog
                }
                val primeiroItem = itens.first()
                openCreateDialog(
                    nome = primeiroItem.nome,
                    categoria = "Nota fiscal",
                    quantidade = primeiroItem.quantidade.coerceAtLeast(1),
                    valorMedio = primeiroItem.valor
                )
                if (itens.size > 1) {
                    Toast.makeText(
                        context,
                        "Foram encontrados ${itens.size} itens. Revisando o primeiro no cadastro.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.i(FLEET_STOCK_TAG, "QR concluido. Abrindo cadastro manual pre-preenchido.")
            }
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    textPrimary: Color,
    textDim: Color,
    cardBg: Color,
    border: Color,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = textPrimary
) {
    Card(
        modifier = modifier.height(86.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    label,
                    color = textDim,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Text(
                value,
                color = valueColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddItemOptionButton(
    onClick: () -> Unit,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun CategoryShelfDivider(
    modifier: Modifier = Modifier,
    border: Color,
    isDark: Boolean
) {
    val shelfTop = if (isDark) Color(0xFF64748B) else Color(0xFFE2E8F0)
    val shelfBody = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(shelfTop, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(shelfBody, RoundedCornerShape(4.dp))
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = border.copy(alpha = if (isDark) 0.5f else 0.35f)
        )
    }
}

@Composable
private fun FleetStockProductsContent(
    modifier: Modifier,
    selectedCategory: String?,
    displayedItems: List<FleetStockItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    stockFilter: FleetStockFilter,
    onStockFilterChange: (FleetStockFilter) -> Unit,
    textPrimary: Color,
    textDim: Color,
    accentBlue: Color,
    border: Color,
    cardBg: Color,
    currency: NumberFormat,
    lowStockColor: Color,
    onBack: () -> Unit,
    onIncrease: (FleetStockItem) -> Unit,
    onDecrease: (FleetStockItem) -> Unit,
    onEdit: (FleetStockItem) -> Unit,
    onDelete: (FleetStockItem) -> Unit
) {
    val selectedCategoryLabel = if (selectedCategory.isNullOrBlank()) {
        tr("Todas categorias", "All categories")
    } else {
        stockCategoryLabel(selectedCategory)
    }
    val selectedCategoryIcon = if (selectedCategory.isNullOrBlank()) {
        Icons.Default.Inventory2
    } else {
        iconForCategory(selectedCategory)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = tr("Voltar", "Back"),
                    tint = textPrimary
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = accentBlue.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.22f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentBlue.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(selectedCategoryIcon, contentDescription = null, tint = accentBlue)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tr("Produtos do estoque", "Stock products"),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        selectedCategoryLabel,
                        color = textDim,
                        fontSize = 12.sp
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        tr("${displayedItems.size} itens", "${displayedItems.size} items"),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Buscar por nome ou codigo", "Search by name or code")) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = textDim)
            },
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onStockFilterChange(FleetStockFilter.ALL) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stockFilter == FleetStockFilter.ALL) accentBlue else accentBlue.copy(alpha = 0.16f),
                    contentColor = if (stockFilter == FleetStockFilter.ALL) Color.White else textPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(tr("Todos", "All"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { onStockFilterChange(FleetStockFilter.LOW) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stockFilter == FleetStockFilter.LOW) Color(0xFFEA580C) else Color(0xFFEA580C).copy(alpha = 0.16f),
                    contentColor = if (stockFilter == FleetStockFilter.LOW) Color.White else textPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(tr("Baixo estoque", "Low stock"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { onStockFilterChange(FleetStockFilter.ZERO) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stockFilter == FleetStockFilter.ZERO) Color(0xFFDC2626) else Color(0xFFDC2626).copy(alpha = 0.16f),
                    contentColor = if (stockFilter == FleetStockFilter.ZERO) Color.White else textPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(tr("Sem estoque", "Out of stock"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (displayedItems.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = textDim,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tr(
                                "Nenhum item encontrado com os filtros atuais.",
                                "No items found with current filters."
                            ),
                            color = textDim,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                displayedItems.forEach { item ->
                    FleetStockItemCard(
                        item = item,
                        currency = currency,
                        textPrimary = textPrimary,
                        textDim = textDim,
                        cardBg = cardBg,
                        border = border,
                        lowStockColor = lowStockColor,
                        accentBlue = accentBlue,
                        onIncrease = { onIncrease(item) },
                        onDecrease = { onDecrease(item) },
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item) }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CategorySquareCard(
    summary: FleetCategorySummary,
    textPrimary: Color,
    textDim: Color,
    cardBg: Color,
    border: Color,
    accentBlue: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    summary.icon,
                    contentDescription = null,
                    tint = accentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    summary.label,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                tr("${summary.totalUnits} un.", "${summary.totalUnits} units"),
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                tr("${summary.totalItems} item(ns)", "${summary.totalItems} item(s)"),
                color = textDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun FleetStockItemCard(
    item: FleetStockItem,
    currency: NumberFormat,
    textPrimary: Color,
    textDim: Color,
    cardBg: Color,
    border: Color,
    lowStockColor: Color,
    accentBlue: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLow = item.quantity <= item.minimum
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.name, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        tr("Código do produto: ${item.barcode.ifBlank { "-" }}", "Product code: ${item.barcode.ifBlank { "-" }}"),
                        color = textDim,
                        fontSize = 12.sp
                    )
                    Text(
                        tr("Categoria: ${item.category.ifBlank { "Geral" }}", "Category: ${item.category.ifBlank { "General" }}"),
                        color = textDim,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = tr("Editar", "Edit"), tint = textDim)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = tr("Excluir", "Delete"), tint = Color(0xFFDC2626))
                    }
                }
            }
            HorizontalDivider(color = border.copy(alpha = 0.6f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(tr("Estoque atual", "Current stock"), color = textDim, fontSize = 11.sp)
                    Text(
                        item.quantity.toString(),
                        color = if (isLow) lowStockColor else textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(tr("Minimo", "Minimum"), color = textDim, fontSize = 11.sp)
                    Text(item.minimum.toString(), color = textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        tr("Valor medio: ${currency.format(item.avgPrice)}", "Avg. value: ${currency.format(item.avgPrice)}"),
                        color = textDim,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDecrease,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = textDim.copy(alpha = 0.2f), contentColor = textPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(tr("Baixar", "Decrease"))
                }
                Button(
                    onClick = onIncrease,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(tr("Repor", "Increase"))
                }
            }
        }
    }
}

private fun iconForCategory(categoryKey: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (categoryKey.lowercase(Locale.ROOT)) {
        "combustivel" -> Icons.Default.LocalGasStation
        "oleo_lubrificantes", "arrefecimento" -> Icons.Default.Opacity
        "filtros", "freios", "pneus_rodas", "motor_transmissao", "suspensao_direcao" -> Icons.Default.Build
        "eletrica_baterias" -> Icons.Default.Settings
        else -> Icons.Default.Inventory2
    }
}

private fun defaultFleetStockCategoryKeys(): List<String> = listOf(
    "combustivel",
    "oleo_lubrificantes",
    "consumiveis",
    "limpeza",
    "epi_seguranca",
    "outros"
)

private fun stockCategoryLabel(key: String): String = when (key.lowercase(Locale.ROOT)) {
    "combustivel" -> "Combustivel"
    "oleo_lubrificantes" -> "Oleo e lubrificantes"
    "consumiveis" -> "Consumiveis"
    "limpeza" -> "Limpeza"
    "epi_seguranca" -> "EPI e seguranca"
    "outros" -> "Outros"
    else -> "Outros"
}

private fun normalizeStockCategoryKey(raw: String): String {
    val category = raw.trim().lowercase(Locale.ROOT)
    return when {
        category.contains("combust") || category.contains("gasolina") || category.contains("etanol") || category.contains("diesel") || category.contains("gnv") || category.contains("arla") -> "combustivel"
        category.contains("oleo") || category.contains("lubr") || category.contains("graxa") -> "oleo_lubrificantes"
        category.contains("limpeza") || category.contains("lavagem") -> "limpeza"
        category.contains("ferramenta") || category.contains("consumivel") || category.contains("parafuso") || category.contains("porca") || category.contains("fita") || category.contains("cola") -> "consumiveis"
        category.contains("filtro") || category.contains("freio") || category.contains("pastilha") || category.contains("disco") || category.contains("lona") || category.contains("pneu") || category.contains("borrachar") || category.contains("roda") || category.contains("alinhamento") || category.contains("balanceamento") || category.contains("eletric") || category.contains("bateria") || category.contains("lampada") || category.contains("fusivel") || category.contains("motor") || category.contains("transmiss") || category.contains("cambio") || category.contains("embreagem") || category.contains("correia") || category.contains("suspens") || category.contains("amortecedor") || category.contains("mola") || category.contains("bandeja") || category.contains("direcao") || category.contains("terminal") || category.contains("arrefecimento") || category.contains("radiador") || category.contains("aditivo") || category.contains("liquido de arrefecimento") || category.contains("bomba d") -> "outros"
        category.contains("epi") || category.contains("seguranca") || category.contains("cone") || category.contains("colete") || category.contains("luva") || category.contains("oculos") || category.contains("extintor") -> "epi_seguranca"
        else -> "outros"
    }
}
@Composable
private fun FleetStockItemDialog(
    initialItem: FleetStockItem?,
    prefillName: String,
    prefillBarcode: String,
    prefillCategory: String,
    prefillQuantity: String,
    prefillAvgPrice: String,
    onDismiss: () -> Unit,
    onSave: (FleetStockItem) -> Unit
) {
    val dialogAccent = Color(0xFF2563EB)
    val dialogTextDim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    var name by remember(initialItem?.id, prefillName) {
        mutableStateOf(initialItem?.name ?: prefillName)
    }
    var barcode by remember(initialItem?.id, prefillBarcode) {
        mutableStateOf(initialItem?.barcode ?: prefillBarcode)
    }
    var category by remember(initialItem?.id, prefillCategory) {
        mutableStateOf(initialItem?.category ?: prefillCategory)
    }
    var quantityText by remember(initialItem?.id, prefillQuantity) {
        mutableStateOf(initialItem?.quantity?.toString() ?: prefillQuantity.ifBlank { "1" })
    }
    var minimumText by remember(initialItem?.id) { mutableStateOf((initialItem?.minimum ?: 1).toString()) }
    var avgPriceText by remember(initialItem?.id, prefillAvgPrice) {
        mutableStateOf(
            if (initialItem?.avgPrice != null) {
                String.format(Locale.US, "%.2f", initialItem.avgPrice)
            } else {
                prefillAvgPrice
            }
        )
    }

    val canSave = name.isNotBlank() && quantityText.toIntOrNull() != null && minimumText.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(dialogAccent.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = dialogAccent, modifier = Modifier.size(18.dp))
                }
                Text(
                    if (initialItem == null) tr("Novo item de estoque", "New stock item")
                    else tr("Editar item de estoque", "Edit stock item"),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Preencha os dados abaixo", "Fill in the fields below"),
                    color = dialogTextDim,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Nome do item *", "Item name *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it.filter(Char::isDigit).take(30) },
                    label = { Text(tr("Codigo de barras", "Barcode")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(40) },
                    label = { Text(tr("Categoria", "Category")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter(Char::isDigit).take(6) },
                        label = { Text(tr("Quantidade", "Quantity")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minimumText,
                        onValueChange = { minimumText = it.filter(Char::isDigit).take(6) },
                        label = { Text(tr("Minimo", "Minimum")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = avgPriceText,
                    onValueChange = { avgPriceText = it.replace(",", ".").filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                    label = { Text(tr("Valor medio (R$)", "Average value (R$)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        FleetStockItem(
                            id = initialItem?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            barcode = barcode.trim(),
                            category = category.trim(),
                            quantity = quantityText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                            minimum = minimumText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                            avgPrice = avgPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        )
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dialogAccent,
                    contentColor = Color.White
                )
            ) {
                Text(tr("Salvar", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"))
            }
        }
    )
}

@Composable
private fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        Log.d(FLEET_STOCK_TAG, "Permissao camera scanner codigo. granted=$granted")
        if (!granted) {
            Toast.makeText(
                context,
                "Permita camera para escanear o codigo.",
                Toast.LENGTH_SHORT
            ).show()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    if (!hasPermission) return

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var processing by remember { mutableStateOf(false) }
    var lastLoggedCount by remember { mutableIntStateOf(-1) }
    var lastZeroLogAt by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { scanner.close() }
            analysisExecutor.shutdown()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        post {
                            val provider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                if (processing) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage == null) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener(mainExecutor) { barcodes ->
                                        val now = System.currentTimeMillis()
                                        if (barcodes.isNotEmpty() || barcodes.size != lastLoggedCount) {
                                            Log.v(FLEET_STOCK_TAG, "Scanner codigo sucesso. lidos=${barcodes.size}")
                                            lastLoggedCount = barcodes.size
                                        } else if (now - lastZeroLogAt >= 2000L) {
                                            Log.v(FLEET_STOCK_TAG, "Scanner codigo sem leitura (continua ativo)")
                                            lastZeroLogAt = now
                                        }
                                        val raw = barcodes.firstNotNullOfOrNull { barcode ->
                                            barcode.rawValue?.trim()?.takeIf { code -> code.isNotBlank() }
                                                ?: barcode.displayValue?.trim()?.takeIf { code -> code.isNotBlank() }
                                        }
                                        if (!raw.isNullOrBlank() && !processing) {
                                            processing = true
                                            Log.i(FLEET_STOCK_TAG, "Scanner codigo capturou valor=$raw")
                                            onBarcodeDetected(raw)
                                        }
                                    }
                                    .addOnFailureListener(mainExecutor) { err ->
                                        Log.e(FLEET_STOCK_TAG, "Falha scanner codigo", err)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("Escaneie o codigo de barras", "Scan barcode"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = Color.White)
                    }
                }
                Text(
                    tr("Aponte a camera para o codigo EAN/GTIN.", "Point camera to EAN/GTIN code."),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun loadFleetStockItems(context: Context): List<FleetStockItem> {
    val raw = context
        .getSharedPreferences(FLEET_STOCK_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_FLEET_STOCK_ITEMS, null)
        ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    FleetStockItem(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = obj.optString("name"),
                        barcode = obj.optString("barcode"),
                        category = obj.optString("category"),
                        quantity = obj.optInt("quantity", 0).coerceAtLeast(0),
                        minimum = obj.optInt("minimum", 0).coerceAtLeast(0),
                        avgPrice = obj.optDouble("avgPrice", 0.0).takeIf { it.isFinite() } ?: 0.0
                    )
                )
            }
        }
    }.onFailure { err ->
        Log.e(FLEET_STOCK_TAG, "Falha ao carregar estoque salvo", err)
    }.getOrDefault(emptyList())
}

private fun formatMovementType(type: String): String = when (type.uppercase(Locale.ROOT)) {
    "ENTRADA" -> "Entrada"
    "SAIDA" -> "Saida"
    "AJUSTE" -> "Ajuste"
    else -> type
}

private fun isMovementInCurrentPeriod(
    timestampMillis: Long,
    mode: HistoryPeriodMode
): Boolean {
    if (timestampMillis <= 0L) return false
    val now = Calendar.getInstance()
    val movement = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return when (mode) {
        HistoryPeriodMode.MONTH ->
            movement.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                movement.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        HistoryPeriodMode.YEAR ->
            movement.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }
}

private fun currentHistoryPeriodLabel(mode: HistoryPeriodMode): String {
    val now = Calendar.getInstance().time
    return when (mode) {
        HistoryPeriodMode.MONTH -> {
            val monthLabel = SimpleDateFormat("MMMM/yyyy", Locale("pt", "BR")).format(now)
            monthLabel.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale("pt", "BR")) else ch.toString()
            }
        }
        HistoryPeriodMode.YEAR -> SimpleDateFormat("yyyy", Locale("pt", "BR")).format(now)
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "-"
    val formatter = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(timestampMillis))
}

private fun saveFleetStockItems(context: Context, items: List<FleetStockItem>) {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("barcode", item.barcode)
                put("category", item.category)
                put("quantity", item.quantity)
                put("minimum", item.minimum)
                put("avgPrice", item.avgPrice)
            }
        )
    }
    context
        .getSharedPreferences(FLEET_STOCK_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_FLEET_STOCK_ITEMS, arr.toString())
        .apply()
    Log.v(FLEET_STOCK_TAG, "saveFleetStockItems aplicado. total=${items.size}")
}

private fun loadFleetStockMovements(context: Context): List<FleetStockMovement> {
    val raw = context
        .getSharedPreferences(FLEET_STOCK_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_FLEET_STOCK_MOVEMENTS, null)
        ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    FleetStockMovement(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        itemId = obj.optString("itemId"),
                        itemName = obj.optString("itemName"),
                        type = obj.optString("type"),
                        quantity = obj.optInt("quantity", 0).coerceAtLeast(0),
                        source = obj.optString("source"),
                        createdAtMillis = obj.optLong("createdAtMillis", 0L),
                        details = obj.optString("details")
                    )
                )
            }
        }
    }.onFailure { err ->
        Log.e(FLEET_STOCK_TAG, "Falha ao carregar historico de estoque", err)
    }.getOrDefault(emptyList())
}

private fun saveFleetStockMovements(context: Context, movements: List<FleetStockMovement>) {
    val arr = JSONArray()
    movements.forEach { movement ->
        arr.put(
            JSONObject().apply {
                put("id", movement.id)
                put("itemId", movement.itemId)
                put("itemName", movement.itemName)
                put("type", movement.type)
                put("quantity", movement.quantity)
                put("source", movement.source)
                put("createdAtMillis", movement.createdAtMillis)
                put("details", movement.details)
            }
        )
    }
    context
        .getSharedPreferences(FLEET_STOCK_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_FLEET_STOCK_MOVEMENTS, arr.toString())
        .apply()
    Log.v(FLEET_STOCK_TAG, "saveFleetStockMovements aplicado. total=${movements.size}")
}

