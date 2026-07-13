package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertEquals("Gasolina (150.00)\nTotal: R$ 150.00", resultado)
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
    fun parseNotaHtmlForTest_sp_montaItensComNomeEValor() {
        val html = """
            <html><body>
                <table id="tabResult">
                    <tr>
                        <td class="txtTit">GASOLINA COMUM</td>
                        <td class="valor">20,00</td>
                    </tr>
                    <tr>
                        <td class="txtTit">LUB LUBRAX SJ SL 20W50 LIT</td>
                        <td class="valor">32,99</td>
                    </tr>
                </table>
                <div id="linhaTotal"><span class="totalNumb">52,99</span></div>
            </body></html>
        """.trimIndent()

        val nota = parseNotaHtmlForTest(html, "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx?p=x")
        assertNotNull(nota)
        val itens = nota!!.descricaoItens.orEmpty()
        assertTrue(itens.contains("GASOLINA COMUM (20.00)"))
        assertTrue(itens.contains("LUB LUBRAX SJ SL 20W50 LIT (32.99)"))
    }

    @Test
    fun parseNotaHtmlForTest_sp_naoCortaNotaGrandeEmSeisItens() {
        val linhas = (1..10).joinToString("\n") { index ->
            """
                <tr>
                    <td>
                        <span class="txtTit">PRODUTO $index</span>
                        <span><strong>Qtde.:</strong>$index</span>
                    </td>
                    <td class="txtTit noWrap">Vl. Total<br><span class="valor">${index},00</span></td>
                </tr>
            """.trimIndent()
        }
        val html = """
            <html><body>
                <table id="tabResult">
                    $linhas
                </table>
                <div id="linhaTotal"><span class="totalNumb">55,00</span></div>
            </body></html>
        """.trimIndent()

        val nota = parseNotaHtmlForTest(
            html,
            "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx?p=x"
        )

        assertNotNull(nota)
        val itens = extrairItensDaDescricaoQr(nota!!.descricaoItens)
        assertEquals(10, itens.size)
        assertEquals("PRODUTO 10", itens.last().nome)
        assertEquals(10, itens.last().quantidade)
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

    @Test
    fun parseNotaHtmlForTest_layoutNacionalFallback() {
        val html = """
            <html><body>
                <div class="emitente">MERCADO MODELO LTDA</div>
                <table>
                    <tr><td class="xProd">ARROZ TIPO 1</td><td class="vProd">18,90</td></tr>
                    <tr><td class="xProd">FEIJAO CARIOCA</td><td class="vProd">9,40</td></tr>
                </table>
                <div>Valor a pagar R$ 28,30</div>
                <div>Data de emissão: 06/03/2026</div>
            </body></html>
        """.trimIndent()

        val nota = parseNotaHtmlForTest(html, "https://nfce.sefaz.exemplo.gov.br/consulta")
        assertNotNull(nota)
        assertEquals(28.3, nota!!.valorTotal!!, 0.001)
        assertEquals("06/03/2026", nota.dataCompra)
    }

    @Test
    fun parseNotaHtmlForTest_sp_paginaBaseRetornaMensagemDeBloqueio() {
        val html = """
            <html>
                <head>
                    <title>Consulta NFC-e QR Code - Secretaria da Fazenda - Governo do Estado de Sao Paulo</title>
                </head>
                <body>
                    <h1>Secretaria da Fazenda - Governo do Estado de Sao Paulo</h1>
                </body>
            </html>
        """.trimIndent()

        val nota = parseNotaHtmlForTest(
            html,
            "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx?p=x"
        )
        assertNotNull(nota)
        assertEquals(null, nota!!.valorTotal)
        assertTrue(nota.descricaoItens.orEmpty().contains("SEFAZ-SP"))
    }
}
