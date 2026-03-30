package br.com.gui.carlembrete

import android.content.Context

private const val TRAVEL_EXPENSES_PREFS_BACKUP = "travel_expenses_prefs"
private const val KEY_TRAVEL_TRIPS_BACKUP = "travel_trips_json"

fun loadTravelTripsBackupJson(context: Context): String {
    return context
        .getSharedPreferences(TRAVEL_EXPENSES_PREFS_BACKUP, Context.MODE_PRIVATE)
        .getString(KEY_TRAVEL_TRIPS_BACKUP, "")
        .orEmpty()
}

fun saveTravelTripsBackupJson(context: Context, rawJson: String) {
    context.getSharedPreferences(TRAVEL_EXPENSES_PREFS_BACKUP, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_TRAVEL_TRIPS_BACKUP, rawJson)
        .apply()
}
