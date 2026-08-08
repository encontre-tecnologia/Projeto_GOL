package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Protege a regra economica do chat: cota de IA so pode ser consumida por pergunta
 * que o motor local nao resolve. Se um destes testes quebrar depois de mexer no
 * roteamento de gerarRespostaIaGaragem, o custo por usuario mudou junto.
 */
class VehicleAiRouterTest {
    private val garagem = listOf(
        CarroInfo(id = "1", nome = "Gol", modelo = "Gol 1.0", marca = "VW", kmAtual = 85_000),
        CarroInfo(id = "2", nome = "Crossfox", modelo = "Crossfox 1.6", marca = "VW", kmAtual = 120_000)
    )

    @Test
    fun saudacaoAutomaticaDeAberturaNuncaConsomeCota() {
        // "resumo rapido" e disparado sozinho ao abrir o chat: cobrar aqui zeraria
        // a cota do usuario sem ele ter perguntado nada.
        assertFalse(perguntaExigeLlm("resumo rapido", garagem))
    }

    @Test
    fun conversaSocialNaoConsomeCota() {
        assertFalse(perguntaExigeLlm("oi", garagem))
        assertFalse(perguntaExigeLlm("obrigado", garagem))
    }

    @Test
    fun perguntasComCalculoDeterministicoFicamLocais() {
        assertFalse(perguntaExigeLlm("qual foi meu custo por km?", garagem))
        assertFalse(perguntaExigeLlm("quanto o Gol consumiu este mes?", garagem))
        assertFalse(perguntaExigeLlm("como esta minha frota?", garagem))
        assertFalse(perguntaExigeLlm("qual veiculo devo revisar primeiro?", garagem))
        assertFalse(perguntaExigeLlm("qual veiculo esta melhor para viajar 100 km?", garagem))
    }

    @Test
    fun mecanicaBasicaJaCobertaPelaListaFixaFicaLocal() {
        assertFalse(perguntaExigeLlm("como sei se o pneu esta ruim?", garagem))
        assertFalse(perguntaExigeLlm("o que e arrefecimento?", garagem))
        assertFalse(perguntaExigeLlm("freio fazendo barulho e grave?", garagem))
    }

    @Test
    fun pedidoDeRegistroFicaLocalPorqueOsDraftsSaoRegex() {
        assertFalse(perguntaExigeLlm("quero fazer um registro", garagem))
    }

    @Test
    fun assuntoDeMecanicaForaDaListaFixaEscalaParaOLlm() {
        // "luzes" passa por isBeginnerMechanicQuestion mas nao tem resposta pronta,
        // entao hoje o usuario recebe um texto generico. E o caso em que o LLM agrega.
        assertTrue(perguntaExigeLlm("como verificar as luzes do carro?", garagem))
    }

    @Test
    fun perguntaAbertaSobreOVeiculoEscalaParaOLlm() {
        assertTrue(perguntaExigeLlm("o que voce acha do meu carro hoje", garagem))
    }

    @Test
    fun perguntaForaDoEscopoDeVeiculosNaoGastaLlm() {
        assertFalse(perguntaExigeLlm("qual a capital da Franca?", garagem))
        assertFalse(perguntaExigeLlm("me conta uma piada", garagem))
    }

    @Test
    fun garagemVaziaNaoGastaLlmEmAnaliseDeDados() {
        assertFalse(perguntaExigeLlm("o que voce acha do meu carro hoje", emptyList()))
    }

    @Test
    fun cotaDeIaEDiferentePorPlanoInclusiveNoGratis() {
        val precos = PremiumPlanPrices()
        // O Gratis tinha a mesma cota do Lite, o que tirava qualquer motivo de upgrade.
        assertEquals(15, precos.aiLimitForTier(PlanTier.FREE))
        assertEquals(100, precos.aiLimitForTier(PlanTier.LITE))
        assertEquals(400, precos.aiLimitForTier(PlanTier.FROTA))
        assertEquals(1_500, precos.aiLimitForTier(PlanTier.ENTERPRISE))
        assertTrue(precos.aiLimitForTier(PlanTier.FREE) < precos.aiLimitForTier(PlanTier.LITE))
    }
}
