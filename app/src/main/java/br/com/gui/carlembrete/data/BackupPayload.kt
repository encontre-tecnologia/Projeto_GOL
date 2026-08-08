package br.com.gui.carlembrete

import java.io.Serializable

data class BackupPayload(
    val carros: List<CarroInfo>,
    val lembretes: List<Lembrete>,
    val contatos: List<ContatoProfissional>,
    val abastecimentos: List<Abastecimento> = emptyList(),
    val pedaladas: List<Pedalada> = emptyList(),
    val travelTripsJson: String = "",
    val fleetStockItemsJson: String = "",
    val fleetStockMovementsJson: String = "",
    // KM inicial por veículo para cálculo de eficiência de combustível (carroId → km)
    val fuelStartKms: Map<String, Int> = emptyMap(),
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
            "semControleKm" to carro.semControleKm,
            "tipoVeiculo" to carro.tipoVeiculo.name,
            "vezesBatido" to (carro.vezesBatido ?: -1),
            "tempoComVeiculo" to carro.tempoComVeiculo,
            "fotoNome" to (carro.fotoNome ?: ""),
            "placa" to (carro.placa ?: "")
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
            "horaAviso" to lembrete.horaAviso,
            "quantidade" to lembrete.quantidade,
            "estabelecimentoNome" to lembrete.estabelecimentoNome,
            "estabelecimentoEndereco" to lembrete.estabelecimentoEndereco,
            "operationalRecordId" to lembrete.operationalRecordId,
            "operationalFeature" to lembrete.operationalFeature,
            "operationalBrand" to lembrete.operationalBrand,
            "operationalPosition" to lembrete.operationalPosition,
            "operationalKmStart" to (lembrete.operationalKmStart ?: -1),
            "operationalKmEnd" to (lembrete.operationalKmEnd ?: -1),
            "historicoGastos" to (lembrete.historicoGastos ?: ""),
            "fotoAvisoNome" to (lembrete.fotoAvisoNome ?: "")
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
            "litros" to item.litros,
            "tipoCombustivel" to item.tipoCombustivel,
            "km" to (item.km ?: -1),
            "itens" to item.itens.map { detalhe ->
                mapOf(
                    "nome" to detalhe.nome,
                    "valor" to detalhe.valor
                )
            }
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
    "fleetStockItemsJson" to fleetStockItemsJson,
    "fleetStockMovementsJson" to fleetStockMovementsJson,
    "fuelStartKms" to fuelStartKms,
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
            semControleKm = mapa["semControleKm"] as? Boolean ?: false,
            tipoVeiculo = runCatching { TipoVeiculo.valueOf(tipoRaw) }.getOrDefault(TipoVeiculo.CARRO),
            vezesBatido = (mapa["vezesBatido"] as? Number)?.toInt()?.takeIf { it >= 0 },
            tempoComVeiculo = mapa["tempoComVeiculo"] as? String ?: "",
            fotoNome = (mapa["fotoNome"] as? String)?.takeIf { it.isNotBlank() },
            placa = (mapa["placa"] as? String)?.takeIf { it.isNotBlank() }
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
            horaAviso = mapa["horaAviso"] as? String ?: "09:00",
            quantidade = (mapa["quantidade"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1,
            estabelecimentoNome = mapa["estabelecimentoNome"] as? String ?: "",
            estabelecimentoEndereco = mapa["estabelecimentoEndereco"] as? String ?: "",
            operationalRecordId = mapa["operationalRecordId"] as? String ?: "",
            operationalFeature = mapa["operationalFeature"] as? String ?: "",
            operationalBrand = mapa["operationalBrand"] as? String ?: "",
            operationalPosition = mapa["operationalPosition"] as? String ?: "",
            operationalKmStart = (mapa["operationalKmStart"] as? Number)?.toInt()?.takeIf { it >= 0 },
            operationalKmEnd = (mapa["operationalKmEnd"] as? Number)?.toInt()?.takeIf { it >= 0 },
            historicoGastos = (mapa["historicoGastos"] as? String)?.ifBlank { null },
            fotoAvisoNome = (mapa["fotoAvisoNome"] as? String)?.ifBlank { null }
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
            tipoCombustivel = mapa["tipoCombustivel"] as? String ?: "",
            itens = (mapa["itens"] as? List<*>)?.mapNotNull { itemDetalhe ->
                val itemMap = itemDetalhe as? Map<*, *> ?: return@mapNotNull null
                val nome = itemMap["nome"] as? String ?: return@mapNotNull null
                val valor = (itemMap["valor"] as? Number)?.toDouble() ?: 0.0
                br.com.gui.carlembrete.ItemAbastecimento(nome = nome, valor = valor)
            } ?: emptyList(),
            km = (mapa["km"] as? Number)?.toInt()?.takeIf { it >= 0 }
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
    val fleetStockItemsJson = data["fleetStockItemsJson"] as? String ?: ""
    val fleetStockMovementsJson = data["fleetStockMovementsJson"] as? String ?: ""

    @Suppress("UNCHECKED_CAST")
    val fuelStartKms = (data["fuelStartKms"] as? Map<*, *>)
        ?.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val value = (v as? Number)?.toInt() ?: return@mapNotNull null
            key to value
        }
        ?.toMap()
        ?: emptyMap()

    val geradoEm = (data["geradoEm"] as? Number)?.toLong() ?: System.currentTimeMillis()

    return BackupPayload(
        carros = carros,
        lembretes = lembretes,
        contatos = contatos,
        abastecimentos = abastecimentos,
        pedaladas = pedaladas,
        travelTripsJson = travelTripsJson,
        fleetStockItemsJson = fleetStockItemsJson,
        fleetStockMovementsJson = fleetStockMovementsJson,
        fuelStartKms = fuelStartKms,
        geradoEm = geradoEm
    )
}
