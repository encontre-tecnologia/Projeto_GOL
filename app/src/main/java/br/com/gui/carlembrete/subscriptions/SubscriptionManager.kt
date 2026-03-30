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

    private var productDetails: ProductDetails? = null
    private var selectedOfferToken: String? = null

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

    fun launchPurchaseFlow(activity: android.app.Activity) {
        val details = productDetails ?: return
        val offerToken = selectedOfferToken ?: run {
            Log.w("Billing", "Oferta de assinatura indisponivel para $SUBSCRIPTION_PRODUCT_ID")
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
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                productDetails = null
                selectedOfferToken = null
                Log.w("Billing", "Falha ao consultar produto: code=${billingResult.responseCode}")
                return@queryProductDetailsAsync
            }

            productDetails = detailsList.firstOrNull()
            selectedOfferToken = productDetails
                ?.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken

            if (productDetails == null) {
                Log.w("Billing", "Produto nao encontrado: $SUBSCRIPTION_PRODUCT_ID")
            } else if (selectedOfferToken == null) {
                Log.w("Billing", "Produto encontrado sem oferta ativa: $SUBSCRIPTION_PRODUCT_ID")
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
        var hasPremium = false
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(SUBSCRIPTION_PRODUCT_ID)
            ) {
                hasPremium = true
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(params) { }
                }
            }
        }
        val tier = when {
            hasPremium -> PlanTier.PREMIUM
            else -> PlanTier.FREE
        }
        _planTier.value = tier
        _isSubscribed.value = tier != PlanTier.FREE
    }

    companion object {
        // Troque pelo ID real da assinatura no Google Play
        const val SUBSCRIPTION_PRODUCT_ID = "carlembrete_premium_monthly"
    }
}

enum class PlanTier(val productId: String) {
    FREE(""),
    PREMIUM(SubscriptionManager.SUBSCRIPTION_PRODUCT_ID)
}
