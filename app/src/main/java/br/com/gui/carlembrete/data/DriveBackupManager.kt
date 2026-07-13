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
            backupPayloadFromMap(map)
        }
    }

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
