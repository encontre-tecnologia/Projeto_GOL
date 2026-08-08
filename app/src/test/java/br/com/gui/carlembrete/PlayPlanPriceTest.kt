package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O número de dias de teste grátis vai direto para a copy da tela de planos ("7 DIAS
 * GRATIS"). Se este parse errar, o app anuncia um trial diferente do que o Play concede.
 */
class PlayPlanPriceTest {
    private fun preco(freeTrialPeriod: String) = PlayPlanPrice(
        offerToken = "token",
        formattedPrice = "R$ 19,90",
        billingPeriod = "P1M",
        freeTrialPeriod = freeTrialPeriod
    )

    @Test
    fun trialEmDiasEUsadoComoVeio() {
        assertEquals(7, preco("P7D").freeTrialDays())
        assertEquals(3, preco("P3D").freeTrialDays())
        assertEquals(14, preco("P14D").freeTrialDays())
    }

    @Test
    fun trialEmSemanasEConvertidoParaDias() {
        // O Play Console permite configurar o trial em semanas.
        assertEquals(7, preco("P1W").freeTrialDays())
        assertEquals(14, preco("P2W").freeTrialDays())
    }

    @Test
    fun trialEmMesesEConvertidoParaDias() {
        assertEquals(30, preco("P1M").freeTrialDays())
    }

    @Test
    fun semTrialNaoPrometeNada() {
        assertEquals(0, preco("").freeTrialDays())
        assertFalse(preco("").hasFreeTrial)
    }

    @Test
    fun periodoInvalidoNaoQuebraATelaEDesligaAPromessa() {
        // Melhor esconder a promessa do que crashar ou anunciar um numero errado.
        assertEquals(0, preco("qualquer-coisa").freeTrialDays())
        assertEquals(0, preco("P").freeTrialDays())
    }

    @Test
    fun hasFreeTrialSegueOPeriodoInformadoPeloPlay() {
        assertTrue(preco("P7D").hasFreeTrial)
        assertFalse(preco("   ").hasFreeTrial)
    }
}
