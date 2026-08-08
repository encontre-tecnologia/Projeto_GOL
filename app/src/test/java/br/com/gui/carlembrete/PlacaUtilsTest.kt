package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A placa e a unica chave que separa dois veiculos do mesmo modelo. Se a normalizacao
 * divergir entre digitacao e comparacao, o desempate para de funcionar sem avisar.
 */
class PlacaUtilsTest {

    private fun carro(nome: String, placa: String? = null, modelo: String = "Modelo") =
        CarroInfo(nome = nome, modelo = modelo, placa = placa)

    @Test
    fun normalizacaoTiraMascaraEspacoEMinuscula() {
        assertEquals("ABC1D23", normalizarPlaca(" abc-1d23 "))
        assertEquals("ABC1234", normalizarPlaca("abc.1234"))
    }

    @Test
    fun normalizacaoLimitaSeteCaracteres() {
        // Digitacao continua depois do 7o caractere nao pode virar placa invalida.
        assertEquals("ABC1D23", normalizarPlaca("ABC1D2345"))
    }

    @Test
    fun normalizacaoDeNuloOuVazioEVazia() {
        assertEquals("", normalizarPlaca(null))
        assertEquals("", normalizarPlaca("   "))
    }

    @Test
    fun aceitaFormatoAntigoEMercosul() {
        assertTrue(placaTemFormatoConhecido("ABC1234"))
        assertTrue(placaTemFormatoConhecido("ABC1D23"))
        assertTrue(placaTemFormatoConhecido("abc-1d23"))
    }

    @Test
    fun rejeitaFormatoQueNaoBate() {
        assertFalse(placaTemFormatoConhecido("AB1234"))
        assertFalse(placaTemFormatoConhecido("1234ABC"))
        assertFalse(placaTemFormatoConhecido("ABCD123"))
        assertFalse(placaTemFormatoConhecido(""))
    }

    @Test
    fun placaEmBrancoEAceitavelPorqueOCampoEOpcional() {
        assertTrue(placaAceitavel(null))
        assertTrue(placaAceitavel(""))
        assertTrue(placaAceitavel("   "))
    }

    @Test
    fun placaPreenchidaForaDoPadraoNaoEAceitavel() {
        // Meia placa salva no banco nao desempata veiculo e suja a busca da garagem.
        assertFalse(placaAceitavel("ABC1"))
        assertFalse(placaAceitavel("ABCD123"))
        assertTrue(placaAceitavel("ABC1D23"))
        assertTrue(placaAceitavel("abc-1234"))
    }

    @Test
    fun completaSoDepoisDosSeteCaracteres() {
        assertFalse(placaCompleta("ABC1D"))
        assertTrue(placaCompleta("ABC1D23"))
        assertTrue(placaCompleta("abc-1d23"))
        assertFalse(placaCompleta(null))
    }

    @Test
    fun exibicaoInsereHifenSoQuandoCompleta() {
        assertEquals("ABC-1D23", formatarPlacaExibicao("abc1d23"))
        // Parcial durante a digitacao fica sem hifen, para o cursor nao pular.
        assertEquals("ABC1", formatarPlacaExibicao("abc1"))
        assertEquals("", formatarPlacaExibicao(null))
    }

    @Test
    fun nomeSemHomonimoNaoRecebePlaca() {
        val gol = carro("GOL 1.0 MI", "ABC1D23")
        val moto = carro("Biz", "XYZ4A56")
        assertEquals("GOL 1.0 MI", nomeExibicaoVeiculo(gol, listOf(gol, moto)))
    }

    @Test
    fun nomeRepetidoRecebePlacaParaDesempatar() {
        // O caso real: nome vem da sugestao da FIPE, entao dois Gols ficam identicos.
        val gol1 = carro("GOL 1.0 MI", "ABC1D23")
        val gol2 = carro("GOL 1.0 MI", "XYZ4A56")
        val garagem = listOf(gol1, gol2)
        assertEquals("GOL 1.0 MI · ABC-1D23", nomeExibicaoVeiculo(gol1, garagem))
        assertEquals("GOL 1.0 MI · XYZ-4A56", nomeExibicaoVeiculo(gol2, garagem))
    }

    @Test
    fun homonimoSemPlacaNaoInventaDesempate() {
        val gol1 = carro("GOL 1.0 MI")
        val gol2 = carro("GOL 1.0 MI", "XYZ4A56")
        assertEquals("GOL 1.0 MI", nomeExibicaoVeiculo(gol1, listOf(gol1, gol2)))
        assertEquals("GOL 1.0 MI · XYZ-4A56", nomeExibicaoVeiculo(gol2, listOf(gol1, gol2)))
    }

    @Test
    fun nomeEmBrancoCaiNoModelo() {
        // Nome deixou de ser obrigatorio no cadastro: em branco, o modelo assume.
        val semNome = carro(nome = "", modelo = "Strada 1.4")
        assertEquals("Strada 1.4", nomeExibicaoVeiculo(semNome, listOf(semNome)))
    }
}
