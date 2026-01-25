package br.com.gui.carlembrete

import android.content.Context
import android.content.Context.MODE_PRIVATE

object AppPreferences {
    private const val PREFS_NAME = "app_prefs_v3"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_FUEL_START_KM_PREFIX = "fuel_start_km_"

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

}
