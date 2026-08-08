package br.com.gui.carlembrete

import android.content.Context
import android.util.Log
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import java.io.File

private const val TAG = "BackupPhotoSync"

/** Prefixo dos arquivos de foto no appDataFolder, para distinguir do JSON do backup. */
private const val PREFIXO_DRIVE = "foto_"

/**
 * Fotos vao para o Drive como arquivos irmaos do JSON, nao dentro dele.
 *
 * Embutir em base64 seria mais simples de escrever, mas o JSON e substituido inteiro a cada
 * backup e lido inteiro na memoria na restauracao (`readText()` antes do Gson). Com dezenas
 * de fotos isso viraria dezenas de MB subindo a cada sincronizacao e um pico de memoria na
 * volta. Como arquivo separado, sobe so o que falta e a volta baixa uma por vez.
 *
 * O `appDataFolder` aceita varios arquivos, e o Drive ja esta autenticado para o JSON —
 * nao entra dependencia nem regra de seguranca nova.
 */
object BackupPhotoSync {

    /**
     * Sobe as fotos que ainda nao estao no Drive.
     *
     * So envia o que falta: foto nao muda depois de criada — o nome carrega timestamp —
     * entao reenviar as mesmas a cada backup seria banda jogada fora.
     */
    fun enviar(context: Context, drive: Drive, nomes: Collection<String>) {
        if (nomes.isEmpty()) return
        val jaNoDrive = listar(drive).keys
        nomes.distinct().forEach { nome ->
            if (nome.isBlank() || jaNoDrive.contains(nomeNoDrive(nome))) return@forEach
            val arquivo = File(context.filesDir, nome)
            if (!arquivo.exists() || arquivo.length() == 0L) return@forEach
            runCatching {
                val metadata = DriveFile().apply {
                    name = nomeNoDrive(nome)
                    parents = listOf("appDataFolder")
                }
                drive.files()
                    .create(metadata, ByteArrayContent("image/jpeg", arquivo.readBytes()))
                    .setFields("id")
                    .execute()
            }.onFailure {
                // Falha de uma foto nao aborta o backup: perder uma imagem e ruim, perder
                // os lembretes e pior.
                Log.w(TAG, "Falha ao enviar foto $nome", it)
            }
        }
    }

    /**
     * Baixa para o filesDir as fotos que faltam localmente. Devolve os nomes recuperados.
     *
     * Nao sobrescreve arquivo que ja existe: restaurar backup num aparelho que ja tem a
     * foto nao precisa gastar rede.
     */
    fun receber(context: Context, drive: Drive, nomes: Collection<String>): Set<String> {
        if (nomes.isEmpty()) return emptySet()
        val noDrive = listar(drive)
        val recuperados = mutableSetOf<String>()
        nomes.distinct().forEach { nome ->
            if (nome.isBlank()) return@forEach
            val destino = File(context.filesDir, nome)
            if (destino.exists() && destino.length() > 0L) {
                recuperados.add(nome)
                return@forEach
            }
            val id = noDrive[nomeNoDrive(nome)] ?: return@forEach
            runCatching {
                drive.files().get(id).executeMediaAsInputStream().use { entrada ->
                    destino.outputStream().use { saida -> entrada.copyTo(saida) }
                }
                recuperados.add(nome)
            }.onFailure {
                Log.w(TAG, "Falha ao baixar foto $nome", it)
                runCatching { destino.delete() }
            }
        }
        return recuperados
    }

    /** Remove do Drive as fotos que nenhum registro referencia mais. */
    fun limparOrfas(drive: Drive, nomesEmUso: Collection<String>) {
        val emUso = nomesEmUso.filter { it.isNotBlank() }.map { nomeNoDrive(it) }.toSet()
        listar(drive).forEach { (nomeRemoto, id) ->
            if (emUso.contains(nomeRemoto)) return@forEach
            runCatching { drive.files().delete(id).execute() }
                .onFailure { Log.w(TAG, "Falha ao apagar foto orfa $nomeRemoto", it) }
        }
    }

    /** Nome remoto -> id do arquivo no Drive. */
    private fun listar(drive: Drive): Map<String, String> = runCatching {
        val busca = "'appDataFolder' in parents and trashed = false and name contains '$PREFIXO_DRIVE'"
        drive.files()
            .list()
            .setSpaces("appDataFolder")
            .setQ(busca)
            .setFields("files(id, name)")
            .setPageSize(1000)
            .execute()
            .files
            .orEmpty()
            .mapNotNull { arquivo ->
                val nome = arquivo.name ?: return@mapNotNull null
                val id = arquivo.id ?: return@mapNotNull null
                nome to id
            }
            .toMap()
    }.onFailure { Log.w(TAG, "Falha ao listar fotos no Drive", it) }.getOrDefault(emptyMap())

    private fun nomeNoDrive(nomeLocal: String) = "$PREFIXO_DRIVE$nomeLocal"
}
