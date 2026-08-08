package br.com.gui.carlembrete

import com.google.firebase.firestore.ListenerRegistration

/**
 * Cotas de cada plano. Preço NÃO mora aqui: valor exibido vem sempre do Google Play,
 * via [PlayPlanPrices], que é quem efetivamente cobra o usuário.
 */
data class PremiumPlanPrices(
    // Limite mensal de requisições de IA por plano. 0 = ilimitado.
    // Só conta pergunta que realmente vai ao LLM — resposta local é gratuita.
    val freeAiLimit: Int = 15,
    val liteAiLimit: Int = 100,
    val frotaAiLimit: Int = 400,
    val enterpriseAiLimit: Int = 1_500
) {
    fun aiLimitForTier(tier: PlanTier): Int = when (tier) {
        PlanTier.ENTERPRISE -> enterpriseAiLimit
        PlanTier.FROTA -> frotaAiLimit
        PlanTier.LITE -> liteAiLimit
        PlanTier.FREE -> freeAiLimit
    }
}

object RemotePlanPricing {
    val defaultPrices = PremiumPlanPrices()

    fun listen(onChanged: (PremiumPlanPrices) -> Unit): ListenerRegistration {
        onChanged(defaultPrices)
        return object : ListenerRegistration {
            override fun remove() = Unit
        }
    }
}
