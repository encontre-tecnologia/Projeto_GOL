package br.com.gui.carlembrete

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class PremiumPlanPrices(
    val lite: String = "10,50",
    val frota: String = "29,90",
    val enterprise: String = "59,90",
    val ebook: String = "19,90",
    // Limite mensal de requisições de IA por plano. 0 = ilimitado.
    val liteAiLimit: Int = 150,
    val frotaAiLimit: Int = 600,
    val enterpriseAiLimit: Int = 0
) {
    fun priceFor(plan: SubscriptionPlan): String = when (plan) {
        SubscriptionPlan.LITE -> lite
        SubscriptionPlan.FROTA -> frota
        SubscriptionPlan.ENTERPRISE -> enterprise
    }

    fun aiLimitForTier(tier: PlanTier): Int = when (tier) {
        PlanTier.ENTERPRISE -> enterpriseAiLimit
        PlanTier.FROTA -> frotaAiLimit
        else -> liteAiLimit // FREE e LITE
    }
}

object RemotePlanPricing {
    private const val TAG = "RemotePlanPricing"
    private const val COLLECTION = "admin_app_config"
    private const val DOCUMENT = "premium_plans"

    val defaultPrices = PremiumPlanPrices()

    fun listen(onChanged: (PremiumPlanPrices) -> Unit): ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Falha ao ouvir precos dos planos", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onChanged(defaultPrices)
                    return@addSnapshotListener
                }

                onChanged(
                    PremiumPlanPrices(
                        lite = snapshot.getPlanPrice("litePrice", defaultPrices.lite),
                        frota = snapshot.getPlanPrice("frotaPrice", defaultPrices.frota),
                        enterprise = snapshot.getPlanPrice("enterprisePrice", defaultPrices.enterprise),
                        ebook = snapshot.getPlanPrice("ebookPrice", defaultPrices.ebook),
                        liteAiLimit = snapshot.getPlanInt("liteAiLimit", defaultPrices.liteAiLimit),
                        frotaAiLimit = snapshot.getPlanInt("frotaAiLimit", defaultPrices.frotaAiLimit),
                        enterpriseAiLimit = snapshot.getPlanInt("enterpriseAiLimit", defaultPrices.enterpriseAiLimit)
                    )
                )
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getPlanPrice(
        field: String,
        fallback: String
    ): String {
        val raw = getString(field)?.trim().orEmpty()
        return raw.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getPlanInt(
        field: String,
        fallback: Int
    ): Int {
        val value = get(field)
        return when (value) {
            is Number -> value.toInt().coerceAtLeast(0)
            is String -> value.trim().toIntOrNull()?.coerceAtLeast(0) ?: fallback
            else -> fallback
        }
    }
}
