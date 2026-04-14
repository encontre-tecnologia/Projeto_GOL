package br.com.gui.carlembrete

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.AcknowledgePurchaseParams
import androidx.compose.runtime.collectAsState
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val EBOOK_BUNDLE_PRODUCT_ID = "zellu_ebooks_bundle"

private data class EbookUi(
    val titulo: String,
    val subtitulo: String,
    val capaAsset: String,
    val pdfAsset: String
)

@Composable
fun EbookStoreScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bundleBilling = remember { EbookBundleBillingManager(context.applicationContext) }
    val isBundleUnlocked by bundleBilling.isBundleUnlocked.collectAsState(initial = false)
    DisposableEffect(Unit) {
        bundleBilling.connect()
        onDispose { bundleBilling.disconnect() }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val screenBg = if (isDark) Color.Black else Color(0xFFF3F6FB)
    val cardBg = if (isDark) Color(0xFF111827) else Color.White
    val border = if (isDark) Color(0xFF1F2937) else Color(0xFFD7E3F4)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val badgeBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFDBEAFE)
    val badgeText = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
    val buyBlue = Color(0xFF3F83F8)
    val bundlePrice = "R$ 19,90"

    val ebooks = remember {
        listOf(
            EbookUi(
                titulo = "Carro Sempre Novo",
                subtitulo = "Cuidados essenciais pra manter o carro redondo",
                capaAsset = "ebooks/covers/carro_sempre_novo.png",
                pdfAsset = "ebooks/pdfs/carro_sempre_novo.pdf"
            ),
            EbookUi(
                titulo = "Dona da Oficina",
                subtitulo = "Guia pratico de mecanica para mulheres",
                capaAsset = "ebooks/covers/dona_da_oficina.png",
                pdfAsset = "ebooks/pdfs/dona_da_oficina.pdf"
            ),
            EbookUi(
                titulo = "Independencia na Estrada",
                subtitulo = "Autonomia no dia a dia com linguagem simples",
                capaAsset = "ebooks/covers/independencia_na_estrada.png",
                pdfAsset = "ebooks/pdfs/independencia_na_estrada.pdf"
            ),
            EbookUi(
                titulo = "Manual Tatico Auto",
                subtitulo = "Checklist estrategico pra nao ficar na mao",
                capaAsset = "ebooks/covers/manual_tatico_sobrevivencia_automotiva.png",
                pdfAsset = "ebooks/pdfs/manual_tatico_sobrevivencia_automotiva.pdf"
            )
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Biblioteca Zellu",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Voltar",
                            tint = textPrimary
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(99.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Colecao Premium Zellu",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = badgeText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Colecao com os 4 eBooks",
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Acesso total aos 4 eBooks por $bundlePrice",
                                    color = textSecondary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isBundleUnlocked) "Compra confirmada: acesso liberado" else "Compra unica para liberar todos os PDFs",
                                    color = if (isBundleUnlocked) Color(0xFF22C55E) else textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val activity = context.findActivity()
                                if (activity == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Nao foi possivel iniciar a compra agora.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    bundleBilling.launchBundlePurchase(
                                        activity = activity,
                                        onUnavailable = {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Produto nao encontrado. Crie e ative no Play Console com ID: $EBOOK_BUNDLE_PRODUCT_ID",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buyBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (isBundleUnlocked) "Ja liberado" else "LIBERAR AGORA POR $bundlePrice")
                        }
                    }
                }
            }

            items(ebooks) { ebook ->
                val cardModifier = if (isBundleUnlocked) {
                    Modifier
                        .fillMaxWidth()
                        .clickable { openPdfFromAssets(context, ebook.pdfAsset, ebook.titulo) }
                } else {
                    Modifier.fillMaxWidth()
                }
                Card(
                    modifier = cardModifier,
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    AsyncImage(
                        model = "file:///android_asset/${ebook.capaAsset}",
                        contentDescription = ebook.titulo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.73f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ebook.titulo,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Card(
                                shape = RoundedCornerShape(999.dp),
                                colors = CardDefaults.cardColors(containerColor = badgeBg)
                            ) {
                                Text(
                                    text = "eBook",
                                    color = badgeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = ebook.subtitulo,
                            color = textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        OutlinedButton(
                            onClick = { openPdfFromAssets(context, ebook.pdfAsset, ebook.titulo) },
                            enabled = isBundleUnlocked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, border),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isBundleUnlocked) Icons.Default.PictureAsPdf else Icons.Default.Lock,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = if (isBundleUnlocked) "Abrir PDF" else "Compre para liberar PDF",
                                color = textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Dica: para venda real, crie no Play Console um produto INAPP com ID $EBOOK_BUNDLE_PRODUCT_ID.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }
    }
}

private class EbookBundleBillingManager(private val appContext: Context) : PurchasesUpdatedListener {
    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var bundleProductDetails: ProductDetails? = null
    private val _isBundleUnlocked = MutableStateFlow(false)
    val isBundleUnlocked: StateFlow<Boolean> = _isBundleUnlocked

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryBundleProduct()
                    refreshPurchaseState()
                }
            }

            override fun onBillingServiceDisconnected() {
                bundleProductDetails = null
            }
        })
    }

    fun disconnect() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    fun launchBundlePurchase(activity: Activity, onUnavailable: () -> Unit) {
        if (!billingClient.isReady) {
            onUnavailable()
            connect()
            return
        }

        val details = bundleProductDetails
        if (details == null) {
            onUnavailable()
            queryBundleProduct()
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            handlePurchases(purchases)
            android.widget.Toast.makeText(
                appContext,
                "Compra iniciada!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshPurchaseState() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var unlocked = false
        purchases.forEach { purchase ->
            if (
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(EBOOK_BUNDLE_PRODUCT_ID)
            ) {
                unlocked = true
                if (!purchase.isAcknowledged) {
                    val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgeParams) { }
                }
            }
        }
        _isBundleUnlocked.value = unlocked
    }

    private fun queryBundleProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(EBOOK_BUNDLE_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
            bundleProductDetails =
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    detailsList.firstOrNull()
                } else {
                    null
                }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openPdfFromAssets(context: Context, assetPath: String, ebookTitle: String) {
    runCatching {
        val safeName = assetPath.substringAfterLast("/")
        val targetFile = File(context.cacheDir, safeName)
        context.assets.open(assetPath).use { input ->
            FileOutputStream(targetFile).use { out -> input.copyTo(out) }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(openIntent)
    }.onFailure {
        runCatching {
            val safeName = assetPath.substringAfterLast("/")
            val targetFile = File(context.cacheDir, safeName)
            if (targetFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, ebookTitle)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar PDF"))
            } else {
                throw ActivityNotFoundException("Arquivo PDF nao encontrado")
            }
        }.onFailure {
            android.widget.Toast.makeText(
                context,
                "Nao foi possivel abrir o PDF agora.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
