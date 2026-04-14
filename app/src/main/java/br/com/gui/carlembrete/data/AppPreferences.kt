package br.com.gui.carlembrete

import android.content.Context
import android.content.Context.MODE_PRIVATE
import java.time.YearMonth

object AppPreferences {
    private const val PREFS_NAME = "app_prefs_v3"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_FUEL_START_KM_PREFIX = "fuel_start_km_"
    private const val KEY_OCR_MONTH = "ocr_month"
    private const val KEY_OCR_COUNT = "ocr_count"
    private const val KEY_PARKED_LAT = "parked_lat"
    private const val KEY_PARKED_LNG = "parked_lng"
    private const val KEY_PARKED_TIME = "parked_time"
    private const val KEY_PARKING_FINALIZED = "parking_finalized"
    private const val KEY_PARKING_PRICING_MODE = "parking_pricing_mode"
    private const val KEY_PARKING_FIXED_VALUE = "parking_fixed_value"
    private const val KEY_PARKING_HOURLY_VALUE = "parking_hourly_value"
    private const val KEY_PARKING_SELECTED_HOURS = "parking_selected_hours"
    private const val KEY_LAST_SELECTED_CAR_ID = "last_selected_car_id"
    private const val KEY_PARKING_PHOTO_URIS = "parking_photo_uris"
    private const val KEY_FIPE_CACHE_VALUE_PREFIX = "fipe_cache_value_"
    private const val KEY_FIPE_CACHE_TIME_PREFIX = "fipe_cache_time_"

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
            .putBoolean(KEY_DARK_THEME, mode == AppThemeMode.DARK)
            .apply()
    }

    fun getAppLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val stored = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.PORTUGUESE.name)
        val resolved = AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.PORTUGUESE
        // O app opera somente em portugues; normaliza legado "SYSTEM" para evitar seguir idioma do aparelho.
        if (resolved != AppLanguage.PORTUGUESE) {
            prefs.edit().putString(KEY_APP_LANGUAGE, AppLanguage.PORTUGUESE.name).apply()
        }
        return AppLanguage.PORTUGUESE
    }

    fun setAppLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, AppLanguage.PORTUGUESE.name)
            .apply()
    }

    fun getFuelStartKm(context: Context, carroId: String): Int? {
        val value = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_FUEL_START_KM_PREFIX + carroId, -1)
        return if (value >= 0) value else null
    }

    fun getLastSelectedCarId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LAST_SELECTED_CAR_ID, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun setLastSelectedCarId(context: Context, carroId: String?) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SELECTED_CAR_ID, carroId)
            .apply()
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
            .putBoolean(KEY_PARKING_FINALIZED, false)
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

    fun clearParkedLocation(context: Context) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_PARKED_LAT)
            .remove(KEY_PARKED_LNG)
            .remove(KEY_PARKED_TIME)
            .apply()
    }

    fun isParkingFinalized(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_PARKING_FINALIZED, false)
    }

    fun setParkingFinalized(context: Context, finalized: Boolean) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PARKING_FINALIZED, finalized)
            .apply()
    }

    fun getParkingPhotoUris(context: Context): List<String> {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getStringSet(KEY_PARKING_PHOTO_URIS, emptySet())
            ?.toList()
            .orEmpty()
    }

    fun setParkingPhotoUris(context: Context, uris: List<String>) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PARKING_PHOTO_URIS, uris.toSet())
            .apply()
    }

    fun addParkingPhotoUri(context: Context, uri: String) {
        val current = getParkingPhotoUris(context).toMutableSet()
        current.add(uri)
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PARKING_PHOTO_URIS, current)
            .apply()
    }

    fun clearParkingPhotoUris(context: Context) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_PARKING_PHOTO_URIS)
            .apply()
    }

    fun getFipeCache(context: Context, key: String, ttlMillis: Long): String? {
        if (key.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val storedAt = prefs.getLong(KEY_FIPE_CACHE_TIME_PREFIX + key, 0L)
        if (storedAt <= 0L) return null
        val age = System.currentTimeMillis() - storedAt
        if (age < 0L || age > ttlMillis) return null
        return prefs.getString(KEY_FIPE_CACHE_VALUE_PREFIX + key, null)
    }

    fun putFipeCache(context: Context, key: String, value: String) {
        if (key.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_FIPE_CACHE_VALUE_PREFIX + key, value)
            .putLong(KEY_FIPE_CACHE_TIME_PREFIX + key, System.currentTimeMillis())
            .apply()
    }

    fun getFipeCacheAnyAge(context: Context, key: String): String? {
        if (key.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_FIPE_CACHE_VALUE_PREFIX + key, null)
    }

    fun setParkingCostConfig(
        context: Context,
        pricingMode: ParkingPricingMode,
        fixedValue: Double?,
        hourlyValue: Double?,
        selectedHours: Int
    ) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_PARKING_PRICING_MODE, pricingMode.name)
            .putString(KEY_PARKING_FIXED_VALUE, fixedValue?.toString())
            .putString(KEY_PARKING_HOURLY_VALUE, hourlyValue?.toString())
            .putInt(KEY_PARKING_SELECTED_HOURS, selectedHours)
            .apply()
    }

    fun getParkingCostConfig(context: Context): ParkingCostConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val mode = prefs.getString(KEY_PARKING_PRICING_MODE, ParkingPricingMode.FIXO.name)
            ?.let { runCatching { ParkingPricingMode.valueOf(it) }.getOrNull() }
            ?: ParkingPricingMode.FIXO
        val fixedValue = prefs.getString(KEY_PARKING_FIXED_VALUE, null)?.toDoubleOrNull()
        val hourlyValue = prefs.getString(KEY_PARKING_HOURLY_VALUE, null)?.toDoubleOrNull()
        val selectedHours = prefs.getInt(KEY_PARKING_SELECTED_HOURS, 1).coerceAtLeast(1)
        return ParkingCostConfig(
            pricingMode = mode,
            fixedValue = fixedValue,
            hourlyValue = hourlyValue,
            selectedHours = selectedHours
        )
    }
}

data class ParkedLocation(
    val lat: Double,
    val lng: Double,
    val timeMillis: Long
)

data class ParkingCostConfig(
    val pricingMode: ParkingPricingMode = ParkingPricingMode.FIXO,
    val fixedValue: Double? = null,
    val hourlyValue: Double? = null,
    val selectedHours: Int = 1
)

enum class ParkingPricingMode {
    FIXO,
    POR_HORA
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ROSE
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    PORTUGUESE("pt-BR"),
    ENGLISH("en-US")
}
