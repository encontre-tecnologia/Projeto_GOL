package br.com.gui.carlembrete

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val BACKUP_PREFS = "backup_prefs"
private const val KEY_LAST_BACKUP = "last_backup_time"
private const val KEY_BACKUP_INTERVAL = "backup_interval"

enum class BackupInterval {
    OFF,
    WEEKLY,
    MONTHLY
}

fun getLastBackupTime(context: Context): Long =
    context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
        .getLong(KEY_LAST_BACKUP, 0L)

fun setLastBackupTime(context: Context, timeMillis: Long) {
    context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_LAST_BACKUP, timeMillis)
        .apply()
}

fun getBackupInterval(context: Context): BackupInterval {
    val raw = context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_BACKUP_INTERVAL, BackupInterval.OFF.name)
    return runCatching { BackupInterval.valueOf(raw ?: BackupInterval.OFF.name) }
        .getOrDefault(BackupInterval.OFF)
}

fun setBackupInterval(context: Context, interval: BackupInterval) {
    context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_BACKUP_INTERVAL, interval.name)
        .apply()
}

fun scheduleDriveBackupWork(context: Context, interval: BackupInterval = getBackupInterval(context)) {
    val workManager = WorkManager.getInstance(context.applicationContext)
    if (interval == BackupInterval.OFF) {
        workManager.cancelUniqueWork(DRIVE_BACKUP_WORK_NAME)
        return
    }

    val repeatDays = if (interval == BackupInterval.WEEKLY) 7L else 30L
    val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(repeatDays, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    workManager.enqueueUniquePeriodicWork(
        DRIVE_BACKUP_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private const val DRIVE_BACKUP_WORK_NAME = "drive_backup"
