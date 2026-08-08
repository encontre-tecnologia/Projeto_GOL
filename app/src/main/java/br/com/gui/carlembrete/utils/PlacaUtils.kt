package br.com.gui.carlembrete

/**
 * Formato antigo (ABC1234) e Mercosul (ABC1D23) na mesma expressao: os tres primeiros
 * sao letras, o quarto e digito, o quinto e letra no Mercosul e digito no antigo, e os
 * dois ultimos sao digitos nos dois.
 */
private val PLACA_FORMATO_CONHECIDO = Regex("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")

private const val PLACA_TAMANHO = 7

/** Guarda sem mascara e em maiuscula, para a comparacao nao depender de digitacao. */
fun normalizarPlaca(bruta: String?): String =
    bruta.orEmpty().uppercase().filter(Char::isLetterOrDigit).take(PLACA_TAMANHO)

fun placaTemFormatoConhecido(placa: String?): Boolean =
    PLACA_FORMATO_CONHECIDO.matches(normalizarPlaca(placa))

/**
 * O campo segue opcional: em branco passa. Preenchida, tem que bater com o padrao —
 * placa meio digitada salva no banco nao desempata veiculo nenhum e ainda contamina
 * a busca da garagem.
 */
fun placaAceitavel(placa: String?): Boolean {
    val limpa = normalizarPlaca(placa)
    return limpa.isBlank() || PLACA_FORMATO_CONHECIDO.matches(limpa)
}

/**
 * Se ja tem os sete caracteres, da para acusar erro de formato na hora. Antes disso a
 * digitacao so esta em curso — acusar no terceiro caractere e ruido.
 */
fun placaCompleta(placa: String?): Boolean =
    normalizarPlaca(placa).length >= PLACA_TAMANHO

/** "ABC1D23" -> "ABC-1D23", como se le no documento. */
fun formatarPlacaExibicao(placa: String?): String {
    val limpa = normalizarPlaca(placa)
    return if (limpa.length == PLACA_TAMANHO) "${limpa.take(3)}-${limpa.drop(3)}" else limpa
}

/**
 * Nome do veiculo para exibicao, desempatado pela placa quando preciso.
 *
 * O campo nome e alimentado pelas sugestoes de modelo da FIPE, entao quem aceita a
 * sugestao fica com "GOL 1.0 MI" — e o segundo Gol da casa fica identico ao primeiro no
 * seletor, na garagem e nos relatorios. A placa e o unico campo que os separa: cor,
 * modelo e ano repetem.
 *
 * So acrescenta a placa quando ha ambiguidade de fato, para nao poluir quem tem um
 * veiculo unico.
 */
fun nomeExibicaoVeiculo(carro: CarroInfo, todos: List<CarroInfo>): String {
    val nomeBase = carro.nome.trim().ifBlank { carro.modelo.trim() }
    val placa = normalizarPlaca(carro.placa)
    if (placa.isBlank()) return nomeBase
    val homonimos = todos.count { outro ->
        outro.nome.trim().ifBlank { outro.modelo.trim() }.equals(nomeBase, ignoreCase = true)
    }
    return if (homonimos > 1) "$nomeBase · ${formatarPlacaExibicao(placa)}" else nomeBase
}
