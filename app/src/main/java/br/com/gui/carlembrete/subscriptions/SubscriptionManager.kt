package br.com.gui.carlembrete

import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SubscriptionManager(context: Context) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val productDetailsById = mutableMapOf<String, ProductDetails>()
    private val offerTokenByProductId = mutableMapOf<String, String>()
    private var billingTier: PlanTier = PlanTier.FREE
    private var dashboardTier: PlanTier = PlanTier.FREE
    private var dashboardListener: ListenerRegistration? = null

    private val _planTier = MutableStateFlow(PlanTier.FREE)
    val planTier: StateFlow<PlanTier> = _planTier
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed
    private val _billingInfo = MutableStateFlow(SubscriptionBillingInfo())
    val billingInfo: StateFlow<SubscriptionBillingInfo> = _billingInfo

    fun refreshLocalEntitlements() {
        applyEffectiveEntitlements()
    }

    fun refreshBillingStatus() {
        applyEffectiveEntitlements()
        if (billingClient.isReady) {
            refreshPurchases()
        }
    }

    private fun applyEffectiveEntitlements() {
        val override = isAdminPremiumOverrideEnabled(appContext)
        // billingTier is authoritative only when Firestore also confirms premium for
        // this Firebase UID — this prevents a Play Store subscription from another
        // Google account on the device from leaking to a different Firebase account.
        // When both agree the user is premium, billingTier wins to fix stale Firestore
        // data (e.g. planTierName="frota" left from a cancelled higher-tier subscription).
        val billingConfirmed = billingTier != PlanTier.FREE && dashboardTier != PlanTier.FREE
        val effective = when {
            override -> getAdminPremiumOverrideTier(appContext)
            billingConfirmed -> billingTier
            else -> dashboardTier
        }
        _planTier.value = effective
        _isSubscribed.value = effective != PlanTier.FREE
    }

    fun connect() {
        applyEffectiveEntitlements()
        listenDashboardEntitlements()
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
        dashboardListener?.remove()
        dashboardListener = null
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun listenDashboardEntitlements() {
        dashboardListener?.remove()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            dashboardTier = PlanTier.FREE
            applyEffectiveEntitlements()
            return
        }

        dashboardListener = FirebaseFirestore.getInstance()
            .collection("admin_users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Billing", "Falha ao ouvir plano do dashboard", error)
                    return@addSnapshotListener
                }
                dashboardTier = snapshot?.let { planTierFromDashboard(it.data.orEmpty()) } ?: PlanTier.FREE
                applyEffectiveEntitlements()
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
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                productDetailsById.clear()
                offerTokenByProductId.clear()
                Log.w("Billing", "Falha ao consultar produto: code=${billingResult.responseCode}")
                return@queryProductDetailsAsync
            }

            val detailsList = productDetailsResult.productDetailsList
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
            handlePurchases(purchases, publishPurchaseToDashboard = true)
        }
    }

    private fun handlePurchases(
        purchases: List<Purchase>,
        publishPurchaseToDashboard: Boolean = false
    ) {
        var tier = PlanTier.FREE
        var bestPurchase: Purchase? = null
        var bestPurchaseTier = PlanTier.FREE
        for (purchase in purchases) {
            val purchaseTier = planTierFromProductIds(purchase.products)
            if (purchaseTier.rank() > bestPurchaseTier.rank()) {
                bestPurchase = purchase
                bestPurchaseTier = purchaseTier
            }
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
        _billingInfo.value = buildBillingInfo(bestPurchase, bestPurchaseTier)
        if (publishPurchaseToDashboard && tier != PlanTier.FREE) {
            dashboardTier = tier
            AdminUsersSync.syncCurrentUser(plan = "premium", tierName = tier.name)
        }
        applyEffectiveEntitlements()
    }

    private fun buildBillingInfo(purchase: Purchase?, tier: PlanTier): SubscriptionBillingInfo {
        if (purchase == null || tier == PlanTier.FREE) return SubscriptionBillingInfo()
        val status = when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (purchase.isAcknowledged) SubscriptionPaymentStatus.CONFIRMED else SubscriptionPaymentStatus.WAITING_CONFIRMATION
            }
            Purchase.PurchaseState.PENDING -> SubscriptionPaymentStatus.PENDING
            else -> SubscriptionPaymentStatus.NOT_FOUND
        }
        return SubscriptionBillingInfo(
            planTier = tier,
            productId = tier.productId,
            purchaseTimeMillis = purchase.purchaseTime,
            nextBillingTimeMillis = estimateNextBillingMillis(purchase.purchaseTime),
            status = status,
            autoRenewing = purchase.isAutoRenewing
        )
    }

    private fun estimateNextBillingMillis(purchaseTimeMillis: Long): Long {
        if (purchaseTimeMillis <= 0L) return 0L
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        var next = java.time.Instant.ofEpochMilli(purchaseTimeMillis).atZone(zone).plusMonths(1)
        while (next.isBefore(now)) {
            next = next.plusMonths(1)
        }
        return next.toInstant().toEpochMilli()
    }

    private fun planTierFromProductIds(products: List<String>): PlanTier = when {
        products.contains(SubscriptionPlan.ENTERPRISE.productId) -> PlanTier.ENTERPRISE
        products.contains(SubscriptionPlan.FROTA.productId) -> PlanTier.FROTA
        products.contains(SubscriptionPlan.LITE.productId) -> PlanTier.LITE
        else -> PlanTier.FREE
    }

    private fun PlanTier.rank(): Int = when (this) {
        PlanTier.FREE -> 0
        PlanTier.LITE -> 1
        PlanTier.FROTA -> 2
        PlanTier.ENTERPRISE -> 3
    }

    companion object {
        const val SUBSCRIPTION_LITE_PRODUCT_ID = "zellu_lite"
        const val SUBSCRIPTION_FROTA_PRODUCT_ID = "zellu_frota"
        const val SUBSCRIPTION_ENTERPRISE_PRODUCT_ID = "zellu_enterprise"
        private const val ADMIN_PREFS = "admin_mode_prefs"
        private const val KEY_ADMIN_OVERRIDE = "admin_premium_override"
        private const val KEY_ADMIN_OVERRIDE_PLAN = "admin_premium_override_plan"
        private const val KEY_ADMIN_EBOOK_OVERRIDE = "admin_ebook_override"

        private fun planTierFromDashboard(data: Map<String, Any>): PlanTier {
            val tierName = (data["planTierName"] as? String)
                ?: (data["tierName"] as? String)
                ?: (data["planTier"] as? String)
                ?: (data["adminPremiumPlan"] as? String)
            when (tierName?.uppercase()) {
                "LITE" -> return PlanTier.LITE
                "FROTA", "FLEET", "PREMIUM" -> return PlanTier.FROTA
                "ENTERPRISE" -> return PlanTier.ENTERPRISE
                "FREE", "GRATIS", "GRÁTIS" -> return PlanTier.FREE
            }

            val plan = ((data["plan"] as? String) ?: (data["tier"] as? String))?.lowercase()
            return when {
                plan == "lite" -> PlanTier.LITE
                plan == "frota" || plan == "fleet" -> PlanTier.FROTA
                plan == "enterprise" -> PlanTier.ENTERPRISE
                data["isPremium"] == true -> PlanTier.LITE
                plan == "premium" -> PlanTier.LITE
                else -> PlanTier.FREE
            }
        }

        private fun scopedKey(baseKey: String): String? {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?.replace(Regex("[^A-Za-z0-9_-]"), "_")
                ?.takeIf { it.isNotBlank() }
            return uid?.let { "${it}_$baseKey" }
        }

        fun setAdminPremiumOverride(context: Context, enabled: Boolean) {
            val key = scopedKey(KEY_ADMIN_OVERRIDE) ?: return
            context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(key, enabled).apply()
        }

        fun isAdminPremiumOverrideEnabled(context: Context): Boolean {
            val key = scopedKey(KEY_ADMIN_OVERRIDE) ?: return false
            return context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .getBoolean(key, false)
        }

        fun setAdminPremiumOverridePlan(context: Context, plan: String?) {
            val key = scopedKey(KEY_ADMIN_OVERRIDE_PLAN) ?: return
            context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .edit().apply {
                    if (plan != null) putString(key, plan)
                    else remove(key)
                }.apply()
        }

        fun getAdminPremiumOverrideTier(context: Context): PlanTier {
            val key = scopedKey(KEY_ADMIN_OVERRIDE_PLAN) ?: return PlanTier.FROTA
            val plan = context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .getString(key, null)
            return when (plan?.uppercase()) {
                "LITE" -> PlanTier.LITE
                "ENTERPRISE" -> PlanTier.ENTERPRISE
                else -> PlanTier.FROTA
            }
        }

        fun setAdminEbookOverride(context: Context, enabled: Boolean) {
            val key = scopedKey(KEY_ADMIN_EBOOK_OVERRIDE) ?: return
            context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(key, enabled).apply()
        }

        fun isAdminEbookOverrideEnabled(context: Context): Boolean {
            val key = scopedKey(KEY_ADMIN_EBOOK_OVERRIDE) ?: return false
            return context.applicationContext
                .getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                .getBoolean(key, false)
        }
    }
}

data class SubscriptionBillingInfo(
    val planTier: PlanTier = PlanTier.FREE,
    val productId: String = "",
    val purchaseTimeMillis: Long = 0L,
    val nextBillingTimeMillis: Long = 0L,
    val status: SubscriptionPaymentStatus = SubscriptionPaymentStatus.NOT_FOUND,
    val autoRenewing: Boolean = false
)

enum class SubscriptionPaymentStatus {
    CONFIRMED,
    WAITING_CONFIRMATION,
    PENDING,
    NOT_FOUND
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

fun reminderLimitForPlan(planTier: PlanTier): Int = when (planTier) {
    PlanTier.FREE -> 20
    PlanTier.LITE -> 80
    PlanTier.FROTA -> 250
    PlanTier.ENTERPRISE -> 1000
}

fun planNameLabel(planTier: PlanTier): String = when (planTier) {
    PlanTier.FREE -> "Grátis"
    PlanTier.LITE -> "Lite"
    PlanTier.FROTA -> "Frota"
    PlanTier.ENTERPRISE -> "Enterprise"
}
