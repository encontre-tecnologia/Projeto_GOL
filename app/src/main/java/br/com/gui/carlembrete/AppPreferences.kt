package br.com.gui.carlembrete

import android.content.Context
import android.content.Context.MODE_PRIVATE

object AppPreferences {
    private const val PREFS_NAME = "app_prefs_v3"
    private const val KEY_FIRST_RUN = "first_run"

    fun needsOnboarding(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_FIRST_RUN, true)
    }

    fun markOnboardingComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_RUN, false)
            .apply()
    }
}
