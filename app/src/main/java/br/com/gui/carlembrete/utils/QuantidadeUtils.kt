package br.com.gui.carlembrete

import java.text.Normalizer
import java.util.Locale

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

private val unidadesPorExtenso = mapOf(
    "um" to 1,
    "uma" to 1,
    "hum" to 1,
    "dois" to 2,
    "duas" to 2,
    "tres" to 3,
    "quatro" to 4,
    "cinco" to 5,
    "seis" to 6,
    "sete" to 7,
    "oito" to 8,
    "nove" to 9
)

private val especiaisPorExtenso = mapOf(
    "dez" to 10,
    "onze" to 11,
    "doze" to 12,
    "treze" to 13,
    "quatorze" to 14,
    "catorze" to 14,
    "quinze" to 15,
    "dezesseis" to 16,
    "dezessete" to 17,
    "dezoito" to 18,
    "dezenove" to 19
)

private val dezenasPorExtenso = mapOf(
    "vinte" to 20,
    "trinta" to 30,
    "quarenta" to 40,
    "cinquenta" to 50,
    "sessenta" to 60,
    "setenta" to 70,
    "oitenta" to 80,
    "noventa" to 90
)

fun extrairQuantidadeDaVoz(textoReconhecido: String): Int? {
    val digitos = Regex("""\d{1,3}""")
        .find(textoReconhecido)
        ?.value
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
    if (digitos != null) return digitos

    val tokens = normalizarQuantidadeVoz(textoReconhecido)
        .split(" ")
        .filter { it.isNotBlank() && it !in palavrasIgnoradasQuantidade }

    if (tokens.isEmpty()) return null

    tokens.forEach { token ->
        unidadesPorExtenso[token]?.let { return it }
        especiaisPorExtenso[token]?.let { return it }
    }

    tokens.forEachIndexed { index, token ->
        val dezena = dezenasPorExtenso[token] ?: return@forEachIndexed
        val unidade = tokens.drop(index + 1).firstNotNullOfOrNull { unidadesPorExtenso[it] } ?: 0
        return (dezena + unidade).takeIf { it > 0 }
    }

    return null
}

private val palavrasIgnoradasQuantidade = setOf(
    "a",
    "as",
    "de",
    "do",
    "da",
    "e",
    "item",
    "itens",
    "peca",
    "pecas",
    "produto",
    "produtos",
    "quantidade",
    "unidade",
    "unidades"
)

private fun normalizarQuantidadeVoz(texto: String): String {
    val semAcento = Normalizer.normalize(texto.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return semAcento
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
