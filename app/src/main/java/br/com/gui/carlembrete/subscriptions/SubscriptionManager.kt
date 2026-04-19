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
    private val appContext = context.applicationContext
    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val productDetailsById = mutableMapOf<String, ProductDetails>()
    private val offerTokenByProductId = mutableMapOf<String, String>()
    private var billingTier: PlanTier = PlanTier.FREE

    private val _planTier = MutableStateFlow(PlanTier.FREE)
    val planTier: StateFlow<PlanTier> = _planTier
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed

    fun refreshLocalEntitlements() {
        applyEffectiveEntitlements()
    }

    private fun applyEffectiveEntitlements() {
        val override = isAdminPremiumOverrideEnabled(appContext)
        val effective = if (override && billingTier == PlanTier.FREE)
            getAdminPremiumOverrideTier(appContext)
        else
            billingTier
        _planTier.value = effective
        _isSubscribed.value = effective != PlanTier.FREE
        val planLabel = if (effective != PlanTier.FREE) "premium" else "free"
        val tierLabel = effective.name // "FREE", "LITE", "FROTA", "ENTERPRISE"
        AdminUsersSync.syncCurrentUser(plan = planLabel, tierName = tierLabel)
    }

    fun connect() {
        applyEffectiveEntitlements()
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
        val details = productDetailsById[targetProductId] ?: run {
            Log.w("Billing", "Produto de assinatura indisponivel no Billing: $targetProductId")
            return
        }
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
            SubscriptionPlan.FROTA.productId,
            SubscriptionPlan.ENTERPRISE.productId
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
                purchase.products.any {
                    it == SubscriptionPlan.LITE.productId ||
                        it == SubscriptionPlan.FROTA.productId ||
                        it == SubscriptionPlan.ENTERPRISE.productId
                }
            ) {
                if (purchase.products.contains(SubscriptionPlan.ENTERPRISE.productId)) {
                    tier = PlanTier.ENTERPRISE
                } else if (purchase.products.contains(SubscriptionPlan.FROTA.productId)) {
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
        billingTier = tier
        applyEffectiveEntitlements()
    }

    companion object {
        const val SUBSCRIPTION_LITE_PRODUCT_ID = "zellu_lite"
        const val SUBSCRIPTION_FROTA_PRODUCT_ID = "zellu_frota"
        const val SUBSCRIPTION_ENTERPRISE_PRODUCT_ID = "zellu_enterprise"
        private const val ADMIN_PREFS = "admin_mode_prefs"
        private const val KEY_ADMIN_OVERRIDE = "admin_premium_override"
        private const val KEY_ADMIN_OVERRIDE_PLAN = "admin_premium_override_plan"
        private const val KEY_ADMIN_EBOOK_OVERRIDE = "admin_ebook_override"

        fun setAdminPremiumOverride(context: Context, enabled: Boolean) {
            context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ADMIN_OVERRIDE, enabled).apply()
        }

        fun isAdminPremiumOverrideEnabled(context: Context): Boolean {
            return context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ADMIN_OVERRIDE, false)
        }

        fun setAdminPremiumOverridePlan(context: Context, plan: String?) {
            context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .edit().apply {
                    if (plan != null) putString(KEY_ADMIN_OVERRIDE_PLAN, plan)
                    else remove(KEY_ADMIN_OVERRIDE_PLAN)
                }.apply()
        }

        fun getAdminPremiumOverrideTier(context: Context): PlanTier {
            val plan = context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ADMIN_OVERRIDE_PLAN, null)
            return when (plan?.uppercase()) {
                "LITE" -> PlanTier.LITE
                "ENTERPRISE" -> PlanTier.ENTERPRISE
                else -> PlanTier.FROTA
            }
        }

        fun setAdminEbookOverride(context: Context, enabled: Boolean) {
            context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ADMIN_EBOOK_OVERRIDE, enabled).apply()
        }

        fun isAdminEbookOverrideEnabled(context: Context): Boolean {
            return context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ADMIN_EBOOK_OVERRIDE, false)
        }
    }
}

enum class SubscriptionPlan(val productId: String) {
    LITE(SubscriptionManager.SUBSCRIPTION_LITE_PRODUCT_ID),
    FROTA(SubscriptionManager.SUBSCRIPTION_FROTA_PRODUCT_ID),
    ENTERPRISE(SubscriptionManager.SUBSCRIPTION_ENTERPRISE_PRODUCT_ID)
}

enum class PlanTier(val productId: String) {
    FREE(""),
    LITE(SubscriptionManager.SUBSCRIPTION_LITE_PRODUCT_ID),
    FROTA(SubscriptionManager.SUBSCRIPTION_FROTA_PRODUCT_ID),
    ENTERPRISE(SubscriptionManager.SUBSCRIPTION_ENTERPRISE_PRODUCT_ID)
}

fun vehicleLimitForPlan(planTier: PlanTier): Int = when (planTier) {
    PlanTier.FREE -> 5
    PlanTier.LITE -> 15
    PlanTier.FROTA -> 50
    PlanTier.ENTERPRISE -> 200
}

fun planNameLabel(planTier: PlanTier): String = when (planTier) {
    PlanTier.FREE -> "Grátis"
    PlanTier.LITE -> "Lite"
    PlanTier.FROTA -> "Frota"
    PlanTier.ENTERPRISE -> "Enterprise"
}
