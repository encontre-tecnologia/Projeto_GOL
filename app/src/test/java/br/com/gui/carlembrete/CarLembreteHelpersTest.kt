package br.com.gui.carlembrete

import androidx.compose.ui.graphics.Color
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.Step // Importante para os passos
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Epic("Manutenção do Veículo") // Categoria macro (ex: Módulo)
@Feature("Lógica de Lembretes") // Funcionalidade específica
@DisplayName("Testes dos Auxiliares de Lembrete") // Nome bonito no relatório
class CarLembreteHelpersTest {

    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val carroId = "car"

    @Test
    @Story("Cálculo de Status de Prazo") // História do usuário
    @Severity(SeverityLevel.CRITICAL) // Prioridade: Se falhar, para tudo!
    @DisplayName("Deve identificar status 'Vencido' para datas passadas")
    @Description("Este teste verifica se o sistema retorna a string 'Vencido' quando a data limite é anterior ao dia de hoje.")
    fun textoStatusPrazoIdentificaVencidos() {
        val ontem = LocalDate.now().minusDays(1).format(formatter)

        // Usando steps para separar as fases do teste no relatório
        val lembrete = criarLembreteTeste(ontem, TipoManutencao.OLEO)
        verificarStatusPrazo(lembrete, "Vencido")
    }

    @Test
    @Story("Cálculo de Status de Prazo")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Deve identificar status 'Vence hoje' para data atual")
    fun textoStatusPrazoIdentificaHoje() {
        val hoje = LocalDate.now().format(formatter)

        val lembrete = criarLembreteTeste(hoje, TipoManutencao.MECANICA)
        verificarStatusPrazo(lembrete, "Vence hoje")
    }

    @Test
    @Story("Cálculo de Status de Prazo")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Deve calcular dias restantes para datas futuras")
    fun textoStatusPrazoIdentificaFuturo() {
        val amanha = LocalDate.now().plusDays(1).format(formatter)

        val lembrete = criarLembreteTeste(amanha, TipoManutencao.BATERIA)
        verificarStatusPrazo(lembrete, "Vence em 1 dia")
    }

    @Test
    @Story("Identificação Visual (Cores)")
    @Severity(SeverityLevel.CRITICAL) // Cor é visual, mas se tiver errado confunde o usuário
    @DisplayName("Deve retornar VERMELHO se houver item vencido")
    fun calcularCorStatusRetornaVermelhoQuandoVencido() {
        val passado = LocalDate.now().minusDays(5).format(formatter)
        val lembretes = listOf(
            Lembrete(carroId = carroId, titulo = "Bateria", dataLimite = passado, kmLimite = "", tipo = TipoManutencao.BATERIA)
        )
        verificarCor(lembretes, TipoManutencao.BATERIA, Color(0xFFEF4444))
    }

    @Test
    @Story("Identificação Visual (Cores)")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Deve retornar VERDE se todos os itens estiverem no futuro")
    fun calcularCorStatusRetornaVerdeQuandoTodosFuturos() {
        val futuro = LocalDate.now().plusDays(60).format(formatter)
        val lembretes = listOf(
            Lembrete(carroId = carroId, titulo = "Troca de óleo", dataLimite = futuro, kmLimite = "", tipo = TipoManutencao.OLEO),
            Lembrete(carroId = carroId, titulo = "Pastilha", dataLimite = futuro, kmLimite = "", tipo = TipoManutencao.FREIO)
        )
        verificarCor(lembretes, TipoManutencao.OLEO, Color(0xFF10B981))
    }

    @Test
    @Story("Detecção Inteligente de Texto")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("IA: Deve detectar tipo 'OLEO' pelo texto")
    fun detectarTipoPeloTextoIdentificaOleo() {
        verificarDeteccaoTexto("Troca de óleo 5W30 sintético", TipoManutencao.OLEO)
    }

    // --- MÉTODOS AUXILIARES COM @Step ---
    // O @Step faz aparecer uma linha expansível no relatório para cada ação

    @Step("Criar lembrete de teste para data: {data}")
    private fun criarLembreteTeste(data: String, tipo: TipoManutencao): Lembrete {
        return Lembrete(
            carroId = carroId,
            titulo = "Teste",
            dataLimite = data,
            kmLimite = "",
            tipo = tipo
        )
    }

    @Step("Verificar se o texto retornado é '{esperado}'")
    private fun verificarStatusPrazo(lembrete: Lembrete, esperado: String) {
        assertEquals(esperado, textoStatusPrazo(lembrete))
    }

    @Step("Verificar se a cor calculada é correta")
    private fun verificarCor(lista: List<Lembrete>, tipo: TipoManutencao, corEsperada: Color) {
        val corCalculada = calcularCorStatus(lista, tipo)
        assertEquals(corEsperada, corCalculada)
    }

    @Step("Verificar detecção de IA para texto: '{texto}'")
    private fun verificarDeteccaoTexto(texto: String, tipoEsperado: TipoManutencao) {
        assertEquals(tipoEsperado, detectarTipoPeloTexto(texto))
    }
}