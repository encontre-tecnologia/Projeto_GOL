package br.com.gui.carlembrete

import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SubscriptionManager(context: Context) : PurchasesUpdatedListener {
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val productDetailsById = mutableMapOf<String, ProductDetails>()
    private val offerTokenByProductId = mutableMapOf<String, String>()

    private val _planTier = MutableStateFlow(PlanTier.FREE)
    val planTier: StateFlow<PlanTier> = _planTier
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    refreshPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Will retry on next user action
            }
        })
    }

    fun disconnect() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun launchPurchaseFlow(
        activity: android.app.Activity,
        plan: SubscriptionPlan = SubscriptionPlan.FROTA
    ) {
        val targetProductId = plan.productId
        val details = productDetailsById[targetProductId] ?: productDetailsById.values.firstOrNull() ?: return
        val offerToken = offerTokenByProductId[targetProductId]
            ?: offerTokenByProductId[details.productId]
            ?: run {
            Log.w("Billing", "Oferta de assinatura indisponivel para $targetProductId")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    private fun queryProduct() {
        val productIds = listOf(
            SubscriptionPlan.LITE.productId,
            SubscriptionPlan.FROTA.productId
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                productDetailsById.clear()
                offerTokenByProductId.clear()
                Log.w("Billing", "Falha ao consultar produto: code=${billingResult.responseCode}")
                return@queryProductDetailsAsync
            }

            productDetailsById.clear()
            offerTokenByProductId.clear()

            detailsList.forEach { details ->
                val offerToken = details.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.offerToken
                if (offerToken != null) {
                    productDetailsById[details.productId] = details
                    offerTokenByProductId[details.productId] = offerToken
                } else {
                    Log.w("Billing", "Produto encontrado sem oferta ativa: ${details.productId}")
                }
            }

            productIds.forEach { productId ->
                if (!productDetailsById.containsKey(productId)) {
                    Log.w("Billing", "Produto nao encontrado: $productId")
                }
            }
        }
    }

    private fun refreshPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            handlePurchases(purchases)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var tier = PlanTier.FREE
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it == SubscriptionPlan.LITE.productId || it == SubscriptionPlan.FROTA.productId }
            ) {
                if (purchase.products.contains(SubscriptionPlan.FROTA.productId)) {
                    tier = PlanTier.FROTA
                } else if (tier == PlanTier.FREE && purchase.products.contains(SubscriptionPlan.LITE.productId)) {
                    tier = PlanTier.LITE
                }
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(params) { }
                }
            }
        }
        _planTier.value = tier
        _isSubscribed.value = tier != PlanTier.FREE
    }

    companion object {
        const val SUBSCRIPTION_LITE_PRODUCT_ID = "zellu_lite"
        const val SUBSCRIPTION_FROTA_PRODUCT_ID = "zellu_frota"
    }
}

enum class SubscriptionPlan(val productId: String) {
    LITE(SubscriptionManager.SUBSCRIPTION_LITE_PRODUCT_ID),
    FROTA(SubscriptionManager.SUBSCRIPTION_FROTA_PRODUCT_ID)
}

enum class PlanTier(val productId: String) {
    FREE(""),
    LITE(SubscriptionManager.SUBSCRIPTION_LITE_PRODUCT_ID),
    FROTA(SubscriptionManager.SUBSCRIPTION_FROTA_PRODUCT_ID)
}
