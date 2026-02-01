package br.com.gui.carlembrete

import android.content.Context

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
