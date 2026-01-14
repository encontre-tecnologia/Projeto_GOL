package br.com.gui.carlembrete

private val padroesQuantidade = listOf(
    Regex("QTD\\s*[:=]?\\s*(\\d+)", RegexOption.IGNORE_CASE),
    Regex("(\\d+)\\s?(?:UN|UND|UNID|PCS|ITENS?)\\b", RegexOption.IGNORE_CASE),
    Regex("(\\d+)\\s?[Xx-]", RegexOption.IGNORE_CASE),
    Regex("[Xx-]\\s?(\\d+)", RegexOption.IGNORE_CASE)
)

fun extrairQuantidadeDaParte(parte: String): Int? {
    val texto = parte.trim()
    for (regex in padroesQuantidade) {
        val match = regex.find(texto)
        if (match != null) {
            val quantidade = match.groupValues[1].toIntOrNull()
            if (quantidade != null && quantidade > 1) return quantidade
        }
    }
    return null
}
