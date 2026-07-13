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
            BancoDeDados.validarDadosParaBackup(applicationContext)
            val carros = BancoDeDados.carregarCarros(applicationContext).orEmpty()
            val lembretes = BancoDeDados.carregarLembretes(applicationContext)
            val contatos = BancoDeDados.carregarContatos(applicationContext)
            val abastecimentos = BancoDeDados.carregarAbastecimentos(applicationContext)
            val pedaladas = BancoDeDados.carregarPedaladas(applicationContext)
            val travelTripsJson = loadTravelTripsBackupJson(applicationContext)
            val fleetStockItemsJson = loadFleetStockItemsBackupJson(applicationContext)
            val fleetStockMovementsJson = loadFleetStockMovementsBackupJson(applicationContext)
            val fuelStartKms = carros.mapNotNull { carro ->
                val km = AppPreferences.getFuelStartKm(applicationContext, carro.id)
                if (km != null) carro.id to km else null
            }.toMap()
            val payload = BackupPayload(
                carros = carros,
                lembretes = lembretes,
                contatos = contatos,
                abastecimentos = abastecimentos,
                pedaladas = pedaladas,
                travelTripsJson = travelTripsJson,
                fleetStockItemsJson = fleetStockItemsJson,
                fleetStockMovementsJson = fleetStockMovementsJson,
                fuelStartKms = fuelStartKms
            )
            DriveBackupManager(applicationContext).uploadBackup(payload, account)
            setLastBackupTime(applicationContext, System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
