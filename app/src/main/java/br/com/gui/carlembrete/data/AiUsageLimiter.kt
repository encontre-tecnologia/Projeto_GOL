package br.com.gui.carlembrete

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

/**
 * Limite mensal de requisições à Zellu AI, controlado localmente por usuário.
 * O contador zera automaticamente quando vira o mês. limit <= 0 = ilimitado.
 */
object AiUsageLimiter {
    private const val PREFS = "ai_usage_limiter"

    private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

    private fun monthKey(): String {
        val now = LocalDate.now()
        return "%04d-%02d".format(now.year, now.monthValue)
    }

    private fun monthField() = "${uid()}:month"
    private fun countField() = "${uid()}:count"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Quantas requisições já foram feitas neste mês (zera ao virar o mês). */
    fun currentCount(context: Context): Int {
        val p = prefs(context)
        val now = monthKey()
        if (p.getString(monthField(), null) != now) {
            p.edit().putString(monthField(), now).putInt(countField(), 0).apply()
            return 0
        }
        return p.getInt(countField(), 0)
    }

    fun isWithinLimit(context: Context, limit: Int): Boolean {
        if (limit <= 0) return true
        return currentCount(context) < limit
    }

    fun remaining(context: Context, limit: Int): Int {
        if (limit <= 0) return Int.MAX_VALUE
        return (limit - currentCount(context)).coerceAtLeast(0)
    }

    /** Registra uma requisição usada neste mês. */
    fun register(context: Context) {
        val p = prefs(context)
        val current = currentCount(context)
        p.edit().putInt(countField(), current + 1).apply()
    }
}
