package br.com.gui.carlembrete

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveBackupManager(private val context: Context) {
    private val gson = Gson()
    private val backupFileName = "carlembrete_backup.json"

    suspend fun uploadBackup(payload: BackupPayload, account: GoogleSignInAccount) {
        withContext(Dispatchers.IO) {
            val drive = buildDriveService(account)
            val json = gson.toJson(payload.toMap())
            val content = ByteArrayContent("application/json", json.toByteArray(Charsets.UTF_8))
            val existingId = findBackupFileId(drive)
            if (existingId != null) {
                drive.files()
                    .update(existingId, null, content)
                    .setFields("id")
                    .execute()
            } else {
                val metadata = File().apply {
                    name = backupFileName
                    parents = listOf("appDataFolder")
                }
                drive.files()
                    .create(metadata, content)
                    .setFields("id")
                    .execute()
            }
            // Depois do JSON, nao antes: se o envio das fotos falhar, o backup dos dados ja
            // esta salvo. O inverso deixaria foto no Drive sem registro que a referencie.
            val nomes = nomesDeFotos(payload)
            BackupPhotoSync.enviar(context, drive, nomes)
            BackupPhotoSync.limparOrfas(drive, nomes)
        }
    }

    suspend fun downloadBackup(account: GoogleSignInAccount): BackupPayload? {
        return withContext(Dispatchers.IO) {
            val drive = buildDriveService(account)
            val fileId = findBackupFileId(drive) ?: return@withContext null
            val inputStream = drive.files().get(fileId).executeMediaAsInputStream()
            val json = inputStream.bufferedReader().use { it.readText() }
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map = gson.fromJson<Map<String, Any>>(json, mapType)
            val payload = backupPayloadFromMap(map)
            val recuperados = BackupPhotoSync.receber(context, drive, nomesDeFotos(payload))
            comFotosLocais(payload, recuperados)
        }
    }

    /**
     * Nomes de arquivo das fotos referenciadas pelo backup.
     *
     * `CarroInfo.fotoNome` ja e um nome de arquivo, mas `Lembrete.fotoPath` guarda caminho
     * absoluto — e caminho absoluto de outro aparelho nao significa nada aqui. Por isso o
     * Drive e indexado pelo nome, nunca pelo caminho.
     */
    private fun nomesDeFotos(payload: BackupPayload): List<String> = buildList {
        payload.carros.forEach { carro ->
            carro.fotoNome?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        payload.lembretes.forEach { lembrete ->
            lembrete.fotoPath?.takeIf { it.isNotBlank() }?.let { add(java.io.File(it).name) }
            // Ja e nome de arquivo, entao entra direto — foi por isso que o campo novo
            // seguiu a convencao do veiculo em vez da do fotoPath.
            lembrete.fotoAvisoNome?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    /**
     * Reaponta `fotoPath` para o arquivo local recem-baixado.
     *
     * Referencia que nao foi recuperada fica como esta, de proposito: apagar aqui
     * transformaria falha momentanea de rede em perda definitiva, e no backup seguinte a
     * limpeza de orfas removeria a foto do Drive tambem. Quem le a foto ja checa se o
     * arquivo existe.
     */
    private fun comFotosLocais(payload: BackupPayload, recuperados: Set<String>): BackupPayload =
        payload.copy(
            lembretes = payload.lembretes.map { lembrete ->
                val nome = lembrete.fotoPath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { java.io.File(it).name }
                if (nome != null && recuperados.contains(nome)) {
                    lembrete.copy(fotoPath = java.io.File(context.filesDir, nome).absolutePath)
                } else {
                    lembrete
                }
            }
        )

    suspend fun hasBackup(account: GoogleSignInAccount): Boolean {
        return withContext(Dispatchers.IO) {
            val drive = buildDriveService(account)
            findBackupFileId(drive) != null
        }
    }

    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val androidAccount = requireNotNull(account.account) { "Conta Google invalida." }
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccount(androidAccount)
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("CarLembrete")
            .build()
    }

    suspend fun deleteBackup(account: GoogleSignInAccount) {
        withContext(Dispatchers.IO) {
            try {
                val drive = buildDriveService(account)
                val fileId = findBackupFileId(drive) ?: return@withContext
                drive.files().delete(fileId).execute()
            } catch (_: Exception) {}
        }
    }

    suspend fun hasBackupInDrive(account: GoogleSignInAccount): Boolean =
        backupModifiedAtInDrive(account) != null

    /** Returns the Drive file's last-modified timestamp in ms, or null if no backup exists. */
    suspend fun backupModifiedAtInDrive(account: GoogleSignInAccount): Long? {
        return withContext(Dispatchers.IO) {
            try {
                findBackupFileMeta(buildDriveService(account))?.second
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun findBackupFileId(drive: Drive): String? =
        findBackupFileMeta(drive)?.first

    private fun findBackupFileMeta(drive: Drive): Pair<String, Long>? {
        val query = "name = '$backupFileName' and 'appDataFolder' in parents and trashed = false"
        val file = drive.files()
            .list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .setFields("files(id, modifiedTime)")
            .execute()
            .files
            ?.firstOrNull() ?: return null
        val ms = file.modifiedTime?.value ?: System.currentTimeMillis()
        return file.id to ms
    }
}
