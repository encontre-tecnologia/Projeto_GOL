package br.com.gui.carlembrete

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

private const val TAG_USAGE_METRICS = "AdminUsageMetrics"
private const val PREFS_USAGE = "admin_usage_metrics"

object AdminUsageMetrics {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val lastSentAt = mutableMapOf<String, Long>()

    private fun todayKey(): String = LocalDate.now().toString()

    private fun incrementMetric(metricField: String, amount: Long = 1L, throttleMs: Long = 0L) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val key = "$uid:$metricField"
        val nowMs = System.currentTimeMillis()
        val last = lastSentAt[key] ?: 0L
        if (throttleMs > 0 && nowMs - last < throttleMs) return
        lastSentAt[key] = nowMs

        val payload = mapOf(
            "dateKey" to todayKey(),
            metricField to FieldValue.increment(amount.toDouble()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("admin_daily_metrics")
            .document(todayKey())
            .set(payload, SetOptions.merge())
            .addOnFailureListener { error ->
                Log.w(TAG_USAGE_METRICS, "Falha ao incrementar métrica $metricField", error)
            }
    }

    fun markAppOpen(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = todayKey()
        val prefs = context.getSharedPreferences(PREFS_USAGE, Context.MODE_PRIVATE)
        val prefKey = "last_app_open_day_$uid"
        val alreadyMarked = prefs.getString(prefKey, null) == today
        if (alreadyMarked) return

        incrementMetric(metricField = "appOpenCount", amount = 1L)
        prefs.edit().putString(prefKey, today).apply()

        firestore.collection("admin_users")
            .document(uid)
            .set(
                mapOf(
                    "lastAccess" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { error ->
                Log.w(TAG_USAGE_METRICS, "Falha ao atualizar lastAccess no app open", error)
            }
    }

    fun markReminderCreated(amount: Int = 1) {
        if (amount <= 0) return
        incrementMetric(metricField = "remindersCreated", amount = amount.toLong())
    }

    fun markTravelExpenseSaved() {
        incrementMetric(metricField = "travelExpensesSaved", amount = 1L)
    }

    fun markQrScanSuccess() {
        incrementMetric(metricField = "qrScansSuccessful", amount = 1L, throttleMs = 5_000L)
    }
}

