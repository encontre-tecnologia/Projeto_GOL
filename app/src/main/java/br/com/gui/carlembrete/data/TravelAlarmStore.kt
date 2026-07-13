package br.com.gui.carlembrete

import android.content.Context

data class TravelAlarmState(
    val destinationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Int = 5_000,
    val active: Boolean = false,
    val arrived: Boolean = false,
    val lastDistanceMeters: Float? = null,
    val initialDistanceMeters: Float? = null,
    val lastUpdateMillis: Long = 0L,
    val startedAtMillis: Long = 0L,
    val arrivedAtMillis: Long = 0L
)

object TravelAlarmStore {
    private const val PREFS = "travel_alarm"
    private const val SETTINGS_PREFS = "travel_alarm_settings"

    fun load(context: Context): TravelAlarmState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return TravelAlarmState(
            destinationName = prefs.getString("name", "").orEmpty(),
            latitude = java.lang.Double.longBitsToDouble(prefs.getLong("lat", 0L)),
            longitude = java.lang.Double.longBitsToDouble(prefs.getLong("lng", 0L)),
            radiusMeters = prefs.getInt("radius", 5_000),
            active = prefs.getBoolean("active", false),
            arrived = prefs.getBoolean("arrived", false),
            lastDistanceMeters = if (prefs.contains("distance")) prefs.getFloat("distance", 0f) else null,
            initialDistanceMeters = if (prefs.contains("initial_distance")) prefs.getFloat("initial_distance", 0f) else null,
            lastUpdateMillis = prefs.getLong("updated", 0L),
            startedAtMillis = prefs.getLong("started_at", 0L),
            arrivedAtMillis = prefs.getLong("arrived_at", 0L)
        )
    }

    fun saveDestination(context: Context, name: String, lat: Double, lng: Double, radiusMeters: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", name)
            .putLong("lat", java.lang.Double.doubleToRawLongBits(lat))
            .putLong("lng", java.lang.Double.doubleToRawLongBits(lng))
            .putInt("radius", radiusMeters)
            .putBoolean("arrived", false)
            .remove("distance")
            .remove("initial_distance")
            .remove("started_at")
            .remove("arrived_at")
            .apply()
    }

    fun setActive(context: Context, active: Boolean, arrived: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putBoolean("active", active)
            .putBoolean("arrived", arrived)
        if (active && !prefs.getBoolean("active", false)) {
            editor.putLong("started_at", System.currentTimeMillis()).remove("arrived_at")
        }
        if (arrived && !prefs.getBoolean("arrived", false)) {
            editor.putLong("arrived_at", System.currentTimeMillis())
        }
        editor.apply()
    }

    fun updateDistance(context: Context, distanceMeters: Float) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putFloat("distance", distanceMeters)
            .putLong("updated", System.currentTimeMillis())
        if (!prefs.contains("initial_distance")) editor.putFloat("initial_distance", distanceMeters)
        editor.apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun isSoundEnabled(context: Context): Boolean =
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getBoolean("sound_enabled", true)

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("sound_enabled", enabled)
            .apply()
    }
}
