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
        val locales = if (language == AppLanguage.SYSTEM || language.tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun wrap(base: Context): ContextWrapper {
        val locale = Locale.forLanguageTag(AppLanguage.PORTUGUESE.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        val localized = base.createConfigurationContext(config)
        return ContextWrapper(localized)
    }
}
