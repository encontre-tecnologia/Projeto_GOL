package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class QrNotaParserTest {

    @Test
    fun montarLocalNota_juntaEstabelecimentoEEndereco() {
        val resultado = montarLocalNota("Posto XPTO", "Rua A, 123")
        assertEquals("Posto XPTO - Rua A, 123", resultado)
    }

    @Test
    fun montarDescricaoItensNota_formataTotalEItens() {
        val resultado = montarDescricaoItensNota(150.0, "Gasolina (150.00)")
        assertEquals("Total: R$ 150.00 | Itens: Gasolina (150.00)", resultado)
    }

    @Test
    fun extrairItensDaDescricaoQr_retornaItensComValor() {
        val itens = extrairItensDaDescricaoQr("Itens: Oleo 5W30 (120,00) + Filtro de oleo (30,00)")
        assertEquals(2, itens.size)
        assertEquals("Oleo 5W30", itens[0].nome)
        assertEquals(120.0, itens[0].valor, 0.001)
        assertEquals("Filtro de oleo", itens[1].nome)
        assertEquals(30.0, itens[1].valor, 0.001)
    }

    @Test
    fun parseNotaHtmlForTest_sp_layoutDetectado() {
        val html = """
            <html><body>
                <div id="linhaTotal"><span class="totalNumb">150,00</span></div>
                <div id="dataEmissao">Emissao: 02/02/2026</div>
                <span class="txtTit">Troca de Oleo</span>
                <span class="valor">150,00</span>
            </body></html>
        """.trimIndent()

        val nota = parseNotaHtmlForTest(html, "https://www.fazenda.sp.gov.br/nfce")
        assertNotNull(nota)
        assertEquals(150.0, nota!!.valorTotal!!, 0.001)
        assertEquals("02/02/2026", nota.dataCompra)
    }

    @Test
    fun parseNotaHtmlForTest_mg_layoutDetectado() {
        val html = """
            <html><body>
                <div class="valorTotal">Valor Total 89,90</div>
                <div class="dadosNf">Data 03/03/2026</div>
                <div class="descricao">Pastilha freio (89,90)</div>
            </body></html>
        """.trimIndent()

        val nota = parseNotaHtmlForTest(html, "https://www.fazenda.mg.gov.br/nfce")
        assertNotNull(nota)
        assertEquals(89.9, nota!!.valorTotal!!, 0.001)
        assertEquals("03/03/2026", nota.dataCompra)
    }
}
