package br.com.gui.carlembrete

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

private const val EBOOK_BUNDLE_PRODUCT_ID = "zellu_ebooks_bundle"

private data class EbookUi(
    val titulo: String,
    val subtitulo: String,
    val capaAsset: String,
    val pdfAsset: String
)

@Composable
fun EbookStoreScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bundleBilling = remember { EbookBundleBillingManager(context.applicationContext) }
    val isBundlePurchased by bundleBilling.isBundleUnlocked.collectAsState(initial = false)
    var ebookOverrideVersion by remember { mutableStateOf(0) }
    var ebookPrice by remember { mutableStateOf(RemotePlanPricing.defaultPrices.ebook) }
    val isBundleUnlocked = remember(isBundlePurchased, ebookOverrideVersion) {
        isBundlePurchased || SubscriptionManager.isAdminEbookOverrideEnabled(context)
    }

    DisposableEffect(Unit) {
        bundleBilling.connect()
        // Aplica override do cache (salvo por syncUserConfig no login) — zero reads extras.
        val cachedEbook = AdminUsersSync.getCachedAdminEbookOverride(context)
        if (cachedEbook != SubscriptionManager.isAdminEbookOverrideEnabled(context)) {
            SubscriptionManager.setAdminEbookOverride(context, cachedEbook)
            ebookOverrideVersion++
        }
        val pricingListener = RemotePlanPricing.listen { prices ->
            ebookPrice = prices.ebook
        }
        onDispose {
            bundleBilling.disconnect()
            pricingListener.remove()
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    // Cores Refinadas
    val screenBg = if (isDark) Color.Black else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF2B3545) else Color.White
    val accentBlue = Color(0xFF3B82F6)
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val ebooks = remember {
        listOf(
            EbookUi("Carro Sempre Novo", "Manutenção preventiva essencial", "ebooks/covers/carro_sempre_novo.png", "ebooks/pdfs/carro_sempre_novo.pdf"),
            EbookUi("Dona da Oficina", "Mecânica para o público feminino", "ebooks/covers/dona_da_oficina.png", "ebooks/pdfs/dona_da_oficina.pdf"),
            EbookUi("Independência na Estrada", "Autonomia total em viagens", "ebooks/covers/independencia_na_estrada.png", "ebooks/pdfs/independencia_na_estrada.pdf"),
            EbookUi("Manual Tático Auto", "Checklist de sobrevivência", "ebooks/covers/manual_tatico_sobrevivencia_automotiva.png", "ebooks/pdfs/manual_tatico_sobrevivencia_automotiva.pdf")
        )
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + navBarBottom),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Zellu Biblioteca",
                    color = textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(2.dp))
            }
        }

        // --- BUNDLE CARD (Destaque) ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2563EB) else Color(0xFF2563EB))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Combo Completo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Desbloqueie todos os 4 guias agora e tenha acesso vitalício ao conhecimento automotivo.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (isBundleUnlocked) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Conteúdo já liberado nesta conta.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            val activity = context.findActivity()
                            if (activity != null) {
                                bundleBilling.launchBundlePurchase(activity) {
                                    android.widget.Toast.makeText(context, "Produto não configurado", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Bolt, modifier = Modifier.size(18.dp), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isBundleUnlocked) "CONTEÚDO LIBERADO" else "LIBERAR TUDO POR R$ $ebookPrice", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // --- LISTA DE EBOOKS ---
        items(ebooks) { ebook ->
            val isLocked = !isBundleUnlocked

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(enabled = !isLocked) { openPdfFromAssets(context, ebook.pdfAsset, ebook.titulo) }
            ) {
                Box {
                    AsyncImage(
                        model = "file:///android_asset/${ebook.capaAsset}",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f)
                    )

                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = ebook.titulo,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ebook.subtitulo,
                        color = textSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        lineHeight = 14.sp,
                        modifier = Modifier.height(30.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (!isLocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = accentBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ler agora", color = accentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- BILLING MANAGER (Simplificado para o exemplo) ---
private class EbookBundleBillingManager(private val appContext: Context) : PurchasesUpdatedListener {
    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private var bundleProductDetails: ProductDetails? = null
    private val _isBundleUnlocked = MutableStateFlow(false)
    val isBundleUnlocked: StateFlow<Boolean> = _isBundleUnlocked

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(res: BillingResult) {
                if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryBundleProduct()
                    refreshPurchaseState()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun disconnect() = if (billingClient.isReady) billingClient.endConnection() else Unit

    fun launchBundlePurchase(activity: Activity, onUnavailable: () -> Unit) {
        val details = bundleProductDetails
        if (details == null) { onUnavailable(); return }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(res: BillingResult, purchases: MutableList<Purchase>?) {
        if (res.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) handlePurchases(purchases)
    }

    private fun refreshPurchaseState() {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { _, purchases ->
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val unlocked = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.contains(EBOOK_BUNDLE_PRODUCT_ID) }
        _isBundleUnlocked.value = unlocked
        purchases.forEach { if (it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged) {
            billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {}
        }}
    }

    private fun queryBundleProduct() {
        val query = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(EBOOK_BUNDLE_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        )).build()
        billingClient.queryProductDetailsAsync(query) { _, result ->
            bundleProductDetails = result.firstOrNull()
        }
    }
}

// --- HELPERS ---
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openPdfFromAssets(context: Context, assetPath: String, ebookTitle: String) {
    runCatching {
        val safeName = assetPath.substringAfterLast("/")
        val targetFile = File(context.cacheDir, safeName)
        context.assets.open(assetPath).use { input -> FileOutputStream(targetFile).use { out -> input.copyTo(out) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        android.widget.Toast.makeText(context, "Erro ao abrir PDF", android.widget.Toast.LENGTH_SHORT).show()
    }
}

