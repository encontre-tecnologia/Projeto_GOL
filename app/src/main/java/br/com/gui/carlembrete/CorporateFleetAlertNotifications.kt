package br.com.gui.carlembrete

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

internal object CorporateFleetAlertNotifications {
    private const val PREFS = "corporate_fleet_alert_notifications"
    private const val KEY_ALERTS = "scheduled_alerts"
    private var liveRegistration: ListenerRegistration? = null

    fun startListening(context: Context) {
        liveRegistration?.remove()
        liveRegistration = null
        val user = FirebaseAuth.getInstance().currentUser ?: return
        resolveCompanyId(user) { companyId ->
            if (companyId.isBlank() || companyId.startsWith("personal_")) return@resolveCompanyId
            liveRegistration = FirebaseFirestore.getInstance()
                    .collection("companies")
                    .document(companyId)
                    .collection("alerts")
                    .addSnapshotListener { snapshot, _ ->
                        val alerts = snapshot?.documents.orEmpty().map { item ->
                            CorporateFleetAlert(
                                id = item.id,
                                title = item.getString("title").orEmpty().ifBlank { "Aviso da frota" },
                                description = item.getString("description").orEmpty(),
                                vehicleName = item.getString("vehicleName").orEmpty(),
                                maintenanceType = item.getString("maintenanceType").orEmpty().ifBlank { "Outros" },
                                priority = item.getString("priority").orEmpty().ifBlank { "media" },
                                status = item.getString("status").orEmpty().ifBlank { "aberto" },
                                dueDateMillis = item.getTimestamp("dueDate")?.toDate()?.time,
                                dueTime = item.getString("dueTime").orEmpty().ifBlank { "09:00" },
                                dueOdometerKm = item.getLong("dueOdometerKm")?.toInt() ?: 0,
                                createdAtMillis = item.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            )
                        }
                        sync(context.applicationContext, companyId, alerts)
                    }
        }
    }

    private fun resolveCompanyId(user: FirebaseUser, onResolved: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)
        val email = user.email.orEmpty().trim().lowercase(Locale.getDefault())
        userRef.get().addOnSuccessListener { userDoc ->
            val activeCompanyId = userDoc.getString("activeCompanyId").orEmpty()
            if (activeCompanyId.isNotBlank() && !activeCompanyId.startsWith("personal_")) {
                onResolved(activeCompanyId)
                return@addOnSuccessListener
            }
            if (email.isBlank()) {
                onResolved(activeCompanyId)
                return@addOnSuccessListener
            }
            db.collection("userInvites")
                .document(emailKey(email))
                .collection("companies")
                .limit(1)
                .get()
                .addOnSuccessListener { invites ->
                    val invite = invites.documents.firstOrNull()
                    val companyId = invite?.getString("companyId").orEmpty()
                    if (companyId.isBlank()) {
                        onResolved(activeCompanyId)
                        return@addOnSuccessListener
                    }
                    db.collection("companies").document(companyId).collection("members").document(user.uid).set(
                        mapOf(
                            "uid" to user.uid,
                            "email" to email,
                            "name" to (user.displayName ?: email),
                            "role" to (invite?.getString("role") ?: "motorista"),
                            "active" to true,
                            "acceptedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    userRef.set(
                        mapOf(
                            "email" to email,
                            "displayName" to (user.displayName ?: ""),
                            "activeCompanyId" to companyId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    onResolved(companyId)
                }
                .addOnFailureListener { onResolved(activeCompanyId) }
        }.addOnFailureListener { onResolved("") }
    }

    private fun emailKey(email: String): String =
        email.trim().lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9._-]"), "_")

    fun sync(context: Context, companyId: String, alerts: List<CorporateFleetAlert>) {
        if (companyId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = readScheduled(prefs.getString(KEY_ALERTS, null))
        val current = alerts
            .filter { it.status != "resolvido" && it.dueDateMillis != null }
            .map { alert -> alert.toScheduled(companyId) }

        val currentIds = current.map { it.id }.toSet()
        previous.filter { it.companyId == companyId && it.id !in currentIds }.forEach {
            NotificacaoHelper.cancelarNotificacao(context, it.notificationId)
        }

        current.forEach { scheduled ->
            NotificacaoHelper.agendarNotificacaoPorData(
                context = context,
                id = scheduled.notificationId,
                titulo = "Frota: ${scheduled.title}",
                descricao = scheduled.description,
                data = scheduled.date,
                hora = scheduled.time
            )
        }
        val preserved = previous.filterNot { it.companyId == companyId }
        prefs.edit().putString(KEY_ALERTS, writeScheduled(preserved + current)).apply()
    }

    fun reschedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        readScheduled(prefs.getString(KEY_ALERTS, null)).forEach { scheduled ->
            NotificacaoHelper.agendarNotificacaoPorData(
                context = context,
                id = scheduled.notificationId,
                titulo = "Frota: ${scheduled.title}",
                descricao = scheduled.description,
                data = scheduled.date,
                hora = scheduled.time
            )
        }
    }

    private fun CorporateFleetAlert.toScheduled(companyId: String): ScheduledAlert {
        val date = Instant.ofEpochMilli(requireNotNull(dueDateMillis))
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val vehicleText = vehicleName.takeIf { it.isNotBlank() }?.let { " para $it" }.orEmpty()
        val details = description.ifBlank { "$maintenanceType$vehicleText. Abra o app para conferir." }
        return ScheduledAlert(
            id = id,
            companyId = companyId,
            title = title,
            description = details,
            date = date,
            time = dueTime.ifBlank { "09:00" }
        )
    }

    private data class ScheduledAlert(
        val id: String,
        val companyId: String,
        val title: String,
        val description: String,
        val date: java.time.LocalDate,
        val time: String
    ) {
        val notificationId get() = "CORP_ALERT_${companyId}_$id"
    }

    private fun readScheduled(raw: String?): List<ScheduledAlert> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            val companyId = item.optString("companyId")
            val dateText = item.optString("date")
            if (id.isBlank() || companyId.isBlank() || dateText.isBlank()) return@mapNotNull null
            ScheduledAlert(
                id = id,
                companyId = companyId,
                title = item.optString("title", "Aviso da frota"),
                description = item.optString("description"),
                date = java.time.LocalDate.parse(dateText),
                time = item.optString("time", "09:00")
            )
        }
    }.getOrDefault(emptyList())

    private fun writeScheduled(items: List<ScheduledAlert>): String = JSONArray().apply {
        items.forEach { item -> put(JSONObject().apply {
            put("id", item.id)
            put("companyId", item.companyId)
            put("title", item.title)
            put("description", item.description)
            put("date", item.date.toString())
            put("time", item.time)
        }) }
    }.toString()
}
