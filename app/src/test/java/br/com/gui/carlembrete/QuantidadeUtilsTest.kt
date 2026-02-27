package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantidadeUtilsTest {

    @Test
    fun extrairQuantidade_quandoFormatoQtd_retornaValor() {
        assertEquals(3, extrairQuantidadeDaParte("QTD: 3"))
    }

    @Test
    fun extrairQuantidade_quandoFormatoUnd_retornaValor() {
        assertEquals(12, extrairQuantidadeDaParte("12 UN"))
    }

    @Test
    fun extrairQuantidade_quandoFormatoPrefixoX_retornaValor() {
        assertEquals(4, extrairQuantidadeDaParte("x4 filtro de oleo"))
    }

    @Test
    fun extrairQuantidade_quandoValorUm_retornaNulo() {
        assertNull(extrairQuantidadeDaParte("QTD: 1"))
    }

    @Test
    fun extrairQuantidade_quandoSemPadrao_retornaNulo() {
        assertNull(extrairQuantidadeDaParte("troca de oleo e filtro"))
    }
}

