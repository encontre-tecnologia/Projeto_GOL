package br.com.gui.carlembrete

import java.io.Serializable

data class BackupPayload(
    val carros: List<CarroInfo>,
    val lembretes: List<Lembrete>,
    val contatos: List<ContatoProfissional>,
    val abastecimentos: List<Abastecimento> = emptyList(),
    val pedaladas: List<Pedalada> = emptyList(),
    val travelTripsJson: String = "",
    val geradoEm: Long = System.currentTimeMillis()
) : Serializable

fun BackupPayload.toMap(): Map<String, Any> = mapOf(
    "carros" to carros.map { carro ->
        mapOf(
            "id" to carro.id,
            "nome" to carro.nome,
            "modelo" to carro.modelo,
            "marca" to carro.marca,
            "proprietario" to carro.proprietario,
            "corArgb" to carro.corArgb,
            "kmAtual" to carro.kmAtual,
            "tipoVeiculo" to carro.tipoVeiculo.name,
            "vezesBatido" to (carro.vezesBatido ?: -1),
            "tempoComVeiculo" to carro.tempoComVeiculo
        )
    },
    "lembretes" to lembretes.map { lembrete ->
        mapOf(
            "id" to lembrete.id,
            "carroId" to lembrete.carroId,
            "contatoId" to (lembrete.contatoId ?: ""),
            "titulo" to lembrete.titulo,
            "peca" to lembrete.peca,
            "dataLimite" to lembrete.dataLimite,
            "kmLimite" to lembrete.kmLimite,
            "tipo" to lembrete.tipo.name,
            "valor" to lembrete.valor,
            "fotoPath" to (lembrete.fotoPath ?: ""),
            "horaAviso" to lembrete.horaAviso
        )
    },
    "contatos" to contatos.map { contato ->
        mapOf(
            "id" to contato.id,
            "nome" to contato.nome,
            "telefone" to contato.telefone,
            "tipoServico" to contato.tipoServico
        )
    },
    "abastecimentos" to abastecimentos.map { item ->
        mapOf(
            "id" to item.id,
            "carroId" to item.carroId,
            "data" to item.data,
            "precoLitro" to item.precoLitro,
            "valorPago" to item.valorPago,
            "litros" to item.litros
        )
    },
    "pedaladas" to pedaladas.map { item ->
        mapOf(
            "id" to item.id,
            "carroId" to item.carroId,
            "data" to item.data,
            "km" to item.km
        )
    },
    "travelTripsJson" to travelTripsJson,
    "geradoEm" to geradoEm
)

fun backupPayloadFromMap(data: Map<String, Any>): BackupPayload {
    val carros = (data["carros"] as? List<*>)?.mapNotNull { item ->
        val mapa = item as? Map<*, *> ?: return@mapNotNull null
        val tipoRaw = mapa["tipoVeiculo"] as? String ?: TipoVeiculo.CARRO.name
        CarroInfo(
            id = mapa["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            nome = mapa["nome"] as? String ?: "Novo Carro",
            modelo = mapa["modelo"] as? String ?: "",
            marca = mapa["marca"] as? String ?: "",
            proprietario = mapa["proprietario"] as? String ?: "",
            corArgb = (mapa["corArgb"] as? Number)?.toInt() ?: 0xFF3B82F6.toInt(),
            kmAtual = (mapa["kmAtual"] as? Number)?.toInt() ?: 0,
            tipoVeiculo = runCatching { TipoVeiculo.valueOf(tipoRaw) }.getOrDefault(TipoVeiculo.CARRO),
            vezesBatido = (mapa["vezesBatido"] as? Number)?.toInt()?.takeIf { it >= 0 },
            tempoComVeiculo = mapa["tempoComVeiculo"] as? String ?: ""
        )
    } ?: emptyList()

    val lembretes = (data["lembretes"] as? List<*>)?.mapNotNull { item ->
        val mapa = item as? Map<*, *> ?: return@mapNotNull null
        val tipoRaw = mapa["tipo"] as? String ?: TipoManutencao.OUTROS.name
        Lembrete(
            id = mapa["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            carroId = mapa["carroId"] as? String ?: "",
            contatoId = (mapa["contatoId"] as? String)?.ifBlank { null },
            titulo = mapa["titulo"] as? String ?: "",
            peca = mapa["peca"] as? String ?: "",
            dataLimite = mapa["dataLimite"] as? String ?: "",
            kmLimite = mapa["kmLimite"] as? String ?: "",
            tipo = runCatching { TipoManutencao.valueOf(tipoRaw) }.getOrDefault(TipoManutencao.OUTROS),
            valor = (mapa["valor"] as? Number)?.toDouble() ?: 0.0,
            fotoPath = (mapa["fotoPath"] as? String)?.ifBlank { null },
            horaAviso = mapa["horaAviso"] as? String ?: "09:00"
        )
    } ?: emptyList()

    val contatos = (data["contatos"] as? List<*>)?.mapNotNull { item ->
        val mapa = item as? Map<*, *> ?: return@mapNotNull null
        ContatoProfissional(
            id = mapa["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            nome = mapa["nome"] as? String ?: "",
            telefone = mapa["telefone"] as? String ?: "",
            tipoServico = mapa["tipoServico"] as? String ?: ""
        )
    } ?: emptyList()

    val abastecimentos = (data["abastecimentos"] as? List<*>)?.mapNotNull { item ->
        val mapa = item as? Map<*, *> ?: return@mapNotNull null
        Abastecimento(
            id = mapa["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            carroId = mapa["carroId"] as? String ?: "",
            data = mapa["data"] as? String ?: "",
            precoLitro = (mapa["precoLitro"] as? Number)?.toDouble() ?: 0.0,
            valorPago = (mapa["valorPago"] as? Number)?.toDouble() ?: 0.0,
            litros = (mapa["litros"] as? Number)?.toDouble() ?: 0.0,
            itens = (mapa["itens"] as? List<*>)?.mapNotNull { itemDetalhe ->
                val itemMap = itemDetalhe as? Map<*, *> ?: return@mapNotNull null
                val nome = itemMap["nome"] as? String ?: return@mapNotNull null
                val valor = (itemMap["valor"] as? Number)?.toDouble() ?: 0.0
                br.com.gui.carlembrete.ItemAbastecimento(nome = nome, valor = valor)
            } ?: emptyList()
        )
    } ?: emptyList()

    val pedaladas = (data["pedaladas"] as? List<*>)?.mapNotNull { item ->
        val mapa = item as? Map<*, *> ?: return@mapNotNull null
        Pedalada(
            id = mapa["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            carroId = mapa["carroId"] as? String ?: "",
            data = mapa["data"] as? String ?: "",
            km = (mapa["km"] as? Number)?.toDouble() ?: 0.0
        )
    } ?: emptyList()

    val travelTripsJson = data["travelTripsJson"] as? String ?: ""
    val geradoEm = (data["geradoEm"] as? Number)?.toLong() ?: System.currentTimeMillis()
    return BackupPayload(
        carros = carros,
        lembretes = lembretes,
        contatos = contatos,
        abastecimentos = abastecimentos,
        pedaladas = pedaladas,
        travelTripsJson = travelTripsJson,
        geradoEm = geradoEm
    )
}
