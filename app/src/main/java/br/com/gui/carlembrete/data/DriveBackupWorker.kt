package br.com.gui.carlembrete

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class DriveBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val scope = Scope(DriveScopes.DRIVE_APPDATA)
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null || !GoogleSignIn.hasPermissions(account, scope)) {
            return Result.success()
        }
        return try {
            val carros = BancoDeDados.carregarCarrosComFallback(applicationContext)
            val lembretes = BancoDeDados.carregarLembretes(applicationContext)
            val contatos = BancoDeDados.carregarContatos(applicationContext)
            val payload = BackupPayload(carros, lembretes, contatos)
            DriveBackupManager(applicationContext).uploadBackup(payload, account)
            setLastBackupTime(applicationContext, System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
