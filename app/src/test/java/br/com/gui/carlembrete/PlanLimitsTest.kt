package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Limites de plano. Os valores aqui são os de [reminderLimitForPlan] e
 * [vehicleLimitForPlan] — os mesmos que o app exibe ao usuário no Perfil.
 *
 * Este arquivo antes testava `scannerLimitForPlan` e `fuelRecordLimitForPlan`, que não
 * existem no projeto, e esperava uma tabela de avisos diferente (5/50/300/ilimitado) da
 * que o código aplica. Isso impedia o test source set de compilar. Alinhei ao código;
 * se a tabela correta for a outra, mude [reminderLimitForPlan] e estes valores juntos.
 */
class PlanLimitsTest {
    @Test
    fun vehicleLimitsMatchPlanStrategy() {
        assertEquals(5, vehicleLimitForPlan(PlanTier.FREE))
        assertEquals(15, vehicleLimitForPlan(PlanTier.LITE))
        assertEquals(50, vehicleLimitForPlan(PlanTier.FROTA))
        assertEquals(200, vehicleLimitForPlan(PlanTier.ENTERPRISE))
    }

    @Test
    fun reminderLimitsMatchPlanStrategy() {
        assertEquals(20, reminderLimitForPlan(PlanTier.FREE))
        assertEquals(80, reminderLimitForPlan(PlanTier.LITE))
        assertEquals(250, reminderLimitForPlan(PlanTier.FROTA))
        assertEquals(1_000, reminderLimitForPlan(PlanTier.ENTERPRISE))
    }

    @Test
    fun limitesSobemMonotonicamenteAoLongoDaEscada() {
        val escada = listOf(PlanTier.FREE, PlanTier.LITE, PlanTier.FROTA, PlanTier.ENTERPRISE)
        escada.zipWithNext { menor, maior ->
            assertTrue(
                "Veiculos deveriam subir de $menor para $maior",
                vehicleLimitForPlan(menor) < vehicleLimitForPlan(maior)
            )
            assertTrue(
                "Avisos deveriam subir de $menor para $maior",
                reminderLimitForPlan(menor) < reminderLimitForPlan(maior)
            )
        }
    }

    @Test
    fun freeBloqueiaAoChegarNoLimiteDeAvisos() {
        assertTrue(canCreateReminders(PlanTier.FREE, atuais = 19, novos = 1))
        assertFalse(canCreateReminders(PlanTier.FREE, atuais = 20, novos = 1))
        // Lote que estoura o limite é recusado inteiro, não parcialmente.
        assertFalse(canCreateReminders(PlanTier.FREE, atuais = 18, novos = 5))
    }

    @Test
    fun frotaBloqueiaAoChegarEm250() {
        assertTrue(canCreateReminders(PlanTier.FROTA, atuais = 249, novos = 1))
        assertFalse(canCreateReminders(PlanTier.FROTA, atuais = 250, novos = 1))
        assertFalse(canCreateReminders(PlanTier.FROTA, atuais = 200, novos = 51))
    }

    @Test
    fun loteVazioNuncaEBloqueado() {
        // Registrar só serviços realizados não consome cota, então chega como novos = 0.
        assertTrue(canCreateReminders(PlanTier.FREE, atuais = 999, novos = 0))
        assertTrue(canCreateReminders(PlanTier.FREE, atuais = 999, novos = -1))
    }

    @Test
    fun avisosRealizadosEAbastecimentosNaoContamParaOLimite() {
        val ativo = lembrete(TipoManutencao.OLEO, dataLimite = "01/01/2027")
        val abastecimento = lembrete(TipoManutencao.ABASTECIMENTO, dataLimite = "01/01/2027")
        val realizado = marcarLembreteComoRealizado(
            lembrete(TipoManutencao.FREIO, dataLimite = "01/01/2027")
        )

        // O contador do Perfil usa exatamente esta regra; o bloqueio precisa concordar.
        assertEquals(1, countRemindersForPlanLimit(listOf(ativo, abastecimento, realizado)))
        assertEquals(0, countRemindersForPlanLimit(listOf(abastecimento, realizado)))
        assertEquals(0, countRemindersForPlanLimit(emptyList()))
        // Registrar serviço já feito não deve gastar cota de aviso.
        assertTrue(canCreateReminders(PlanTier.FREE, atuais = 20, novos = countRemindersForPlanLimit(listOf(realizado))))
    }

    private fun lembrete(tipo: TipoManutencao, dataLimite: String) = Lembrete(
        carroId = "carro-1",
        titulo = "Teste",
        dataLimite = dataLimite,
        kmLimite = "",
        tipo = tipo
    )
}
