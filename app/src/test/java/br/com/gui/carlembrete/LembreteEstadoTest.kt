package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LembreteEstadoTest {

    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    @Test
    fun marcarLembreteComoRealizado_marcaPrefixoEHoraPadrao() {
        val base = Lembrete(
            carroId = "car-1",
            titulo = "Troca de oleo",
            dataLimite = LocalDate.now().plusDays(10).format(formatter),
            kmLimite = "10000",
            tipo = TipoManutencao.OLEO
        )

        val dataRealizacao = LocalDate.of(2026, 2, 20)
        val realizado = marcarLembreteComoRealizado(base, dataRealizacao)

        assertTrue(isLembreteRealizado(realizado))
        assertEquals("00:00", realizado.horaAviso)
        assertEquals("20/02/2026", realizado.dataLimite)
        assertEquals(LocalDate.of(2026, 2, 20), dataRealizacaoLembrete(realizado))
    }

    @Test
    fun dataParaOrdenacao_quandoRealizado_usaDataRealizacao() {
        val base = Lembrete(
            carroId = "car-2",
            titulo = "Bateria",
            dataLimite = "31/12/2030",
            kmLimite = "",
            tipo = TipoManutencao.BATERIA
        )

        val realizado = marcarLembreteComoRealizado(base, LocalDate.of(2026, 1, 10))
        assertEquals(LocalDate.of(2026, 1, 10), dataParaOrdenacao(realizado))
    }
}

