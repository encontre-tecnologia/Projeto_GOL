package br.com.gui.carlembrete

import android.content.Context

private const val FLEET_STOCK_PREFS_BACKUP = "fleet_stock_prefs"
private const val KEY_FLEET_STOCK_ITEMS_BACKUP = "fleet_stock_items_json"
private const val KEY_FLEET_STOCK_MOVEMENTS_BACKUP = "fleet_stock_movements_json"

fun loadFleetStockItemsBackupJson(context: Context): String {
    return context
        .getSharedPreferences(FLEET_STOCK_PREFS_BACKUP, Context.MODE_PRIVATE)
        .getString(KEY_FLEET_STOCK_ITEMS_BACKUP, "")
        .orEmpty()
}

fun loadFleetStockMovementsBackupJson(context: Context): String {
    return context
        .getSharedPreferences(FLEET_STOCK_PREFS_BACKUP, Context.MODE_PRIVATE)
        .getString(KEY_FLEET_STOCK_MOVEMENTS_BACKUP, "")
        .orEmpty()
}

fun saveFleetStockBackupJson(
    context: Context,
    itemsJson: String,
    movementsJson: String
) {
    context.getSharedPreferences(FLEET_STOCK_PREFS_BACKUP, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_FLEET_STOCK_ITEMS_BACKUP, itemsJson)
        .putString(KEY_FLEET_STOCK_MOVEMENTS_BACKUP, movementsJson)
        .apply()
}
