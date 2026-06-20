package br.com.gui.carlembrete

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reagenda notifica?Ãµes ap?s eventos do sistema (boot/update).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i("BootReceiver", "onReceive action=$action")
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val appContext = context?.applicationContext ?: return
        InstallDiagnostics.logDetailedSnapshot(appContext, "BootReceiver.$action")
        scheduleDriveBackupWork(appContext)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lembretes = BancoDeDados.carregarLembretes(appContext)
                if (lembretes.isNotEmpty()) {
                    NotificacaoHelper.reagendarExistentes(appContext, lembretes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

