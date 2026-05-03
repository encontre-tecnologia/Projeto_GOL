package br.com.gui.carlembrete

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.gui.carlembrete.ui.theme.CarLembreteTheme

class OnboardingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.DARK) }
            FixedFontScale {
            CarLembreteTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OnboardingScreen(
                        onThemeModeChanged = { mode -> themeMode = mode },
                        onFinish = {
                            AppPreferences.markOnboardingComplete(this@OnboardingActivity)
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    )
                }
            }
            }
        }
    }
}
