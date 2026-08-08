package br.com.gui.carlembrete

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import java.time.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG_PLAY_PRICES = "PlayPlanPrices"

/**
 * A oferta que o app escolheu para um plano: é ela que a tela exibe E é ela que o
 * fluxo de compra usa. Preço e teste grátis vêm do mesmo `offerToken`, então o que
 * o usuário lê é exatamente o que o Play vai cobrar.
 */
data class PlayPlanPrice(
    /** Token da oferta escolhida. Obrigatório no BillingFlowParams. */
    val offerToken: String,
    /** Preço de renovação já localizado pelo Play, ex.: "R$ 19,90". */
    val formattedPrice: String,
    /** Período de cobrança ISO-8601, ex.: P1M, P1Y. Vazio se o Play não informou. */
    val billingPeriod: String,
    /** Período do teste grátis ISO-8601, ex.: P7D. Vazio quando a oferta não tem trial. */
    val freeTrialPeriod: String
) {
    val hasFreeTrial: Boolean get() = freeTrialPeriod.isNotBlank()

    /** Dias de teste grátis, ou 0 quando não há trial. */
    fun freeTrialDays(): Int {
        if (freeTrialPeriod.isBlank()) return 0
        return runCatching { Period.parse(freeTrialPeriod) }
            .map { periodo ->
                // Play usa P7D, P1W, P1M. Normalizamos tudo para dias.
                periodo.days + periodo.months * 30 + periodo.years * 365
            }
            .getOrDefault(0)
    }
}

/**
 * Fonte única de preço de assinatura no app. O valor sempre vem do Google Play, que
 * é quem efetivamente cobra — assim um reajuste no Play Console aparece no app sem
 * release, e nunca existe divergência entre preço exibido e preço cobrado.
 *
 * Enquanto o Play não responde, [priceFor] devolve null e a tela decide o que mostrar.
 */
object PlayPlanPrices {
    private val _pricesByProductId = MutableStateFlow<Map<String, PlayPlanPrice>>(emptyMap())
    val pricesByProductId: StateFlow<Map<String, PlayPlanPrice>> = _pricesByProductId

    @Volatile
    private var loadingClient: BillingClient? = null

    val productIds: List<String> = listOf(
        SubscriptionManager.SUBSCRIPTION_LITE_PRODUCT_ID,
        SubscriptionManager.SUBSCRIPTION_FROTA_PRODUCT_ID,
        SubscriptionManager.SUBSCRIPTION_ENTERPRISE_PRODUCT_ID
    )

    fun priceFor(plan: SubscriptionPlan): PlayPlanPrice? = _pricesByProductId.value[plan.productId]

    /** Publica o que o Play devolveu. Chamado pelo [SubscriptionManager] e pelo load avulso. */
    internal fun publish(details: List<ProductDetails>) {
        if (details.isEmpty()) return
        val novos = details.mapNotNull { produto ->
            selecionarOferta(produto)?.let { produto.productId to it }
        }.toMap()
        if (novos.isEmpty()) return
        // Merge: uma consulta parcial não apaga preço já conhecido de outro plano.
        _pricesByProductId.value = _pricesByProductId.value + novos
    }

    /**
     * Escolhe qual oferta do plano o app vai exibir e vender.
     *
     * O Play só devolve ofertas para as quais este usuário é elegível, então uma oferta
     * de teste grátis presente aqui significa trial realmente disponível — quem já usou
     * não recebe essa oferta e vê o plano sem promessa de teste.
     *
     * Preferimos a oferta com o maior teste grátis; sem trial, cai no plano base.
     */
    private fun selecionarOferta(produto: ProductDetails): PlayPlanPrice? {
        val ofertas = produto.subscriptionOfferDetails.orEmpty()
        if (ofertas.isEmpty()) return null

        val comTrial = ofertas
            .mapNotNull { oferta -> montarPreco(oferta)?.takeIf { it.hasFreeTrial } }
            .maxByOrNull { it.freeTrialDays() }
        if (comTrial != null) return comTrial

        // Sem trial elegível: plano base puro (sem offerId) na frente de promoções.
        val base = ofertas.firstOrNull { it.offerId.isNullOrBlank() } ?: ofertas.first()
        return montarPreco(base)
    }

    /**
     * Extrai preço de renovação e teste grátis de uma oferta. O valor exibido é sempre
     * a fase recorrente — mostrar a primeira fase faria um plano com trial aparecer
     * como R$ 0,00.
     */
    private fun montarPreco(oferta: ProductDetails.SubscriptionOfferDetails): PlayPlanPrice? {
        val fases = oferta.pricingPhases.pricingPhaseList
        if (fases.isEmpty()) return null

        val recorrente = fases.firstOrNull {
            it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING &&
                it.priceAmountMicros > 0L
        } ?: fases.lastOrNull { it.priceAmountMicros > 0L }
        ?: return null

        val trial = fases.firstOrNull { it.priceAmountMicros == 0L }

        return PlayPlanPrice(
            offerToken = oferta.offerToken,
            formattedPrice = recorrente.formattedPrice,
            billingPeriod = recorrente.billingPeriod.orEmpty(),
            freeTrialPeriod = trial?.billingPeriod.orEmpty()
        )
    }

    /**
     * Carrega os preços sem depender de um [SubscriptionManager] ativo — usado por telas
     * que só exibem o catálogo, como o onboarding. Não faz nada se já houver preço
     * carregado ou uma consulta em andamento.
     *
     * Chamar da main thread (LaunchedEffect): a guarda de reentrância não usa lock.
     */
    fun ensureLoaded(context: Context) {
        if (_pricesByProductId.value.isNotEmpty() || loadingClient != null) return

        val client = BillingClient.newBuilder(context.applicationContext)
            .setListener { _, _ -> /* Esta instância só consulta preço, não compra. */ }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        loadingClient = client

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG_PLAY_PRICES, "Billing indisponivel para consultar preco: ${billingResult.responseCode}")
                    encerrar(client)
                    return
                }
                client.queryProductDetailsAsync(queryParams()) { queryResult, details ->
                    if (queryResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        publish(details.productDetailsList)
                    } else {
                        Log.w(TAG_PLAY_PRICES, "Falha ao consultar precos: ${queryResult.responseCode}")
                    }
                    encerrar(client)
                }
            }

            override fun onBillingServiceDisconnected() {
                encerrar(client)
            }
        })
    }

    private fun encerrar(client: BillingClient) {
        if (loadingClient === client) loadingClient = null
        runCatching { client.endConnection() }
    }

    internal fun queryParams(): QueryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()
}
