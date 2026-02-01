package br.com.gui.carlembrete

import android.content.Context
import android.content.Context.MODE_PRIVATE
import java.time.YearMonth

object AppPreferences {
    private const val PREFS_NAME = "app_prefs_v3"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_FUEL_START_KM_PREFIX = "fuel_start_km_"
    private const val KEY_OCR_MONTH = "ocr_month"
    private const val KEY_OCR_COUNT = "ocr_count"

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
}
