package br.com.gui.carlembrete

import android.content.Context
import android.content.Context.MODE_PRIVATE
import java.time.YearMonth

object AppPreferences {
    private const val PREFS_NAME = "app_prefs_v3"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_FUEL_START_KM_PREFIX = "fuel_start_km_"
    private const val KEY_OCR_MONTH = "ocr_month"
    private const val KEY_OCR_COUNT = "ocr_count"
    private const val KEY_PARKED_LAT = "parked_lat"
    private const val KEY_PARKED_LNG = "parked_lng"
    private const val KEY_PARKED_TIME = "parked_time"

    fun needsOnboarding(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_FIRST_RUN, true)
    }

    fun markOnboardingComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_RUN, false)
            .apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_THEME, isDark)
            .putString(KEY_THEME_MODE, if (isDark) AppThemeMode.DARK.name else AppThemeMode.LIGHT.name)
            .apply()
    }

    fun getThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val stored = prefs.getString(KEY_THEME_MODE, null)
        if (stored != null) {
            return AppThemeMode.entries.firstOrNull { it.name == stored } ?: AppThemeMode.SYSTEM
        }
        val isDark = prefs.getBoolean(KEY_DARK_THEME, true)
        return if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .putBoolean(KEY_DARK_THEME, mode != AppThemeMode.LIGHT)
            .apply()
    }

    fun getFuelStartKm(context: Context, carroId: String): Int? {
        val value = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_FUEL_START_KM_PREFIX + carroId, -1)
        return if (value >= 0) value else null
    }

    fun setFuelStartKm(context: Context, carroId: String, km: Int) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_FUEL_START_KM_PREFIX + carroId, km)
            .apply()
    }

    private fun currentOcrMonth(): String = YearMonth.now().toString()

    fun getOcrCountThisMonth(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val storedMonth = prefs.getString(KEY_OCR_MONTH, null)
        val currentMonth = currentOcrMonth()
        if (storedMonth != currentMonth) {
            prefs.edit()
                .putString(KEY_OCR_MONTH, currentMonth)
                .putInt(KEY_OCR_COUNT, 0)
                .apply()
            return 0
        }
        return prefs.getInt(KEY_OCR_COUNT, 0)
    }

    fun incrementOcrCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val storedMonth = prefs.getString(KEY_OCR_MONTH, null)
        val currentMonth = currentOcrMonth()
        val currentCount = if (storedMonth != currentMonth) 0 else prefs.getInt(KEY_OCR_COUNT, 0)
        val newCount = currentCount + 1
        prefs.edit()
            .putString(KEY_OCR_MONTH, currentMonth)
            .putInt(KEY_OCR_COUNT, newCount)
            .apply()
        return newCount
    }

    fun canUseOcr(context: Context, limit: Int): Boolean {
        return getOcrCountThisMonth(context) < limit
    }

    fun setParkedLocation(context: Context, lat: Double, lng: Double, timeMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_PARKED_LAT, lat.toString())
            .putString(KEY_PARKED_LNG, lng.toString())
            .putLong(KEY_PARKED_TIME, timeMillis)
            .apply()
    }

    fun getParkedLocation(context: Context): ParkedLocation? {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val latStr = prefs.getString(KEY_PARKED_LAT, null) ?: return null
        val lngStr = prefs.getString(KEY_PARKED_LNG, null) ?: return null
        val lat = latStr.toDoubleOrNull() ?: return null
        val lng = lngStr.toDoubleOrNull() ?: return null
        val time = prefs.getLong(KEY_PARKED_TIME, 0L)
        return ParkedLocation(lat, lng, time)
    }
}

data class ParkedLocation(
    val lat: Double,
    val lng: Double,
    val timeMillis: Long
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
