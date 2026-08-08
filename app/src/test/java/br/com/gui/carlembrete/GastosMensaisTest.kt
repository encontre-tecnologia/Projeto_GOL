package br.com.gui.carlembrete

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * O grafico da home mostra dinheiro, e numero errado ali vira decisao errada de
 * manutencao. Estes testes travam as regras que decidem o que entra na conta.
 */
class GastosMensaisTest {

    private val hoje = LocalDate.of(2026, 7, 15)
    private val carro = "carro-1"

    private fun servicoRealizado(valor: Double, dataExecucao: LocalDate): Lembrete =
        marcarLembreteComoRealizado(
            Lembrete(
                carroId = carro,
                titulo = "Servico",
                dataLimite = "01/01/2027",
                kmLimite = "",
                tipo = TipoManutencao.OLEO,
                valor = valor
            ),
            dataRealizacao = dataExecucao
        )

    private fun avisoPendente(valor: Double) = Lembrete(
        carroId = carro,
        titulo = "Aviso futuro",
        dataLimite = "10/08/2026",
        kmLimite = "",
        tipo = TipoManutencao.FREIO,
        valor = valor
    )

    private fun abastecimento(valorPago: Double, data: String) = Abastecimento(
        carroId = carro,
        data = data,
        precoLitro = 6.0,
        valorPago = valorPago,
        litros = valorPago / 6.0
    )

    @Test
    fun janelaTemUmMesPorPosicaoTerminandoNoMesAtual() {
        val gastos = calcularGastosMensais(emptyList(), emptyList(), meses = 6, hoje = hoje)
        assertEquals(6, gastos.size)
        assertEquals(YearMonth.of(2026, 2), gastos.first().mes)
        assertEquals(YearMonth.of(2026, 7), gastos.last().mes)
    }

    @Test
    fun avisoPendenteNaoContaComoGasto() {
        // Valor previsto e intencao, nao despesa: o dinheiro ainda nao saiu.
        val gastos = calcularGastosMensais(
            lembretes = listOf(avisoPendente(500.0)),
            abastecimentos = emptyList(),
            hoje = hoje
        )
        assertEquals(0.0, gastos.sumOf { it.total }, 0.001)
    }

    @Test
    fun servicoRealizadoEntraNoMesDaRealizacao() {
        val gastos = calcularGastosMensais(
            lembretes = listOf(servicoRealizado(320.0, LocalDate.of(2026, 6, 3))),
            abastecimentos = emptyList(),
            hoje = hoje
        )
        val junho = gastos.first { it.mes == YearMonth.of(2026, 6) }
        assertEquals(320.0, junho.manutencao, 0.001)
        assertEquals(0.0, junho.combustivel, 0.001)
        assertEquals(320.0, gastos.sumOf { it.total }, 0.001)
    }

    @Test
    fun abastecimentoSomaNoMesECaiEmCombustivel() {
        val gastos = calcularGastosMensais(
            lembretes = emptyList(),
            abastecimentos = listOf(
                abastecimento(180.0, "02/07/2026"),
                abastecimento(120.0, "20/07/2026")
            ),
            hoje = hoje
        )
        val julho = gastos.last()
        assertEquals(300.0, julho.combustivel, 0.001)
        assertEquals(0.0, julho.manutencao, 0.001)
    }

    @Test
    fun aceitaDataIsoAlemDeDiaMesAno() {
        val gastos = calcularGastosMensais(
            lembretes = emptyList(),
            abastecimentos = listOf(abastecimento(90.0, "2026-07-05")),
            hoje = hoje
        )
        assertEquals(90.0, gastos.last().combustivel, 0.001)
    }

    @Test
    fun gastoForaDaJanelaNaoAparece() {
        val gastos = calcularGastosMensais(
            lembretes = listOf(servicoRealizado(999.0, LocalDate.of(2025, 12, 1))),
            abastecimentos = listOf(abastecimento(777.0, "01/12/2025")),
            meses = 6,
            hoje = hoje
        )
        assertEquals(0.0, gastos.sumOf { it.total }, 0.001)
    }

    @Test
    fun valorZeradoOuNegativoEIgnorado() {
        val gastos = calcularGastosMensais(
            lembretes = listOf(servicoRealizado(0.0, LocalDate.of(2026, 7, 1))),
            abastecimentos = listOf(abastecimento(0.0, "05/07/2026")),
            hoje = hoje
        )
        assertEquals(0.0, gastos.sumOf { it.total }, 0.001)
    }

    @Test
    fun dataInvalidaNaoDerrubaOCalculo() {
        val gastos = calcularGastosMensais(
            lembretes = emptyList(),
            abastecimentos = listOf(
                abastecimento(50.0, "sem data"),
                abastecimento(70.0, "10/07/2026")
            ),
            hoje = hoje
        )
        // O lancamento sem data e descartado; o valido continua contando.
        assertEquals(70.0, gastos.sumOf { it.total }, 0.001)
    }

    @Test
    fun cicloRecorrenteEncerradoEntraNoMesDoEncerramento() {
        // onMarkAsDone de um lembrete recorrente reagenda dataLimite e nunca chama
        // marcarLembreteComoRealizado; registrarCicloRealizado e o unico jeito desse
        // gasto sobreviver para o grafico.
        var lembrete = Lembrete(
            carroId = carro,
            titulo = "Troca de oleo recorrente",
            dataLimite = "01/01/2027",
            kmLimite = "",
            tipo = TipoManutencao.OLEO,
            valor = 150.0
        )
        lembrete = registrarCicloRealizado(lembrete, dataRealizacao = LocalDate.of(2026, 6, 20))
        lembrete = lembrete.copy(dataLimite = "20/12/2026")

        val gastos = calcularGastosMensais(
            lembretes = listOf(lembrete),
            abastecimentos = emptyList(),
            hoje = hoje
        )
        val junho = gastos.first { it.mes == YearMonth.of(2026, 6) }
        assertEquals(150.0, junho.manutencao, 0.001)
        assertEquals(150.0, gastos.sumOf { it.total }, 0.001)
    }

    @Test
    fun totalSomaAsDuasOrigens() {
        val gastos = calcularGastosMensais(
            lembretes = listOf(servicoRealizado(200.0, LocalDate.of(2026, 7, 8))),
            abastecimentos = listOf(abastecimento(150.0, "09/07/2026")),
            hoje = hoje
        )
        val julho = gastos.last()
        assertEquals(200.0, julho.manutencao, 0.001)
        assertEquals(150.0, julho.combustivel, 0.001)
        assertEquals(350.0, julho.total, 0.001)
    }

    @Test
    fun variacaoComparaMesAtualComMesAnterior() {
        val gastos = listOf(
            GastoDoMes(YearMonth.of(2026, 6), combustivel = 100.0, manutencao = 0.0),
            GastoDoMes(YearMonth.of(2026, 7), combustivel = 150.0, manutencao = 50.0)
        )

        assertEquals(100.0, variacaoPercentualMesAnterior(gastos) ?: -1.0, 0.001)
        assertEquals("+100% vs mes anterior", textoVariacaoMesAnterior(100.0))
    }

    @Test
    fun variacaoNaoApareceSemBaseNoMesAnterior() {
        val gastos = listOf(
            GastoDoMes(YearMonth.of(2026, 6), combustivel = 0.0, manutencao = 0.0),
            GastoDoMes(YearMonth.of(2026, 7), combustivel = 200.0, manutencao = 0.0)
        )

        assertEquals(null, variacaoPercentualMesAnterior(gastos))
    }

    @Test
    fun variacaoPodeSerNegativaQuandoGastoCai() {
        val gastos = listOf(
            GastoDoMes(YearMonth.of(2026, 6), combustivel = 200.0, manutencao = 0.0),
            GastoDoMes(YearMonth.of(2026, 7), combustivel = 50.0, manutencao = 0.0)
        )

        assertEquals(-75.0, variacaoPercentualMesAnterior(gastos) ?: 0.0, 0.001)
        assertEquals("-75% vs mes anterior", textoVariacaoMesAnterior(-75.0))
    }
}
