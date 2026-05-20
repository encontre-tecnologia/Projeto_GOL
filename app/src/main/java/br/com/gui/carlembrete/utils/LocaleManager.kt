package br.com.gui.carlembrete

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {
    fun applySavedLanguage(context: Context) {
        applyLanguage(AppLanguage.PORTUGUESE)
    }

    fun applyLanguage(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(AppLanguage.PORTUGUESE.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun wrap(base: Context): ContextWrapper {
        val locale = Locale.forLanguageTag(AppLanguage.PORTUGUESE.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.fontScale = 1f
        val localized = base.createConfigurationContext(config)
        return ContextWrapper(localized)
    }
}
