package br.com.gui.carlembrete

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val TAG_ADMIN_SYNC = "AdminUsersSync"

object AdminUsersSync {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private fun vehiclePayload(vehicles: List<CarroInfo>): Map<String, Any> {
        val names = vehicles.map { "${it.nome} ${it.modelo}".trim() }
        val total = vehicles.size
        return mapOf(
            "vehiclesTotal" to total,
            "veiculosTotal" to total,
            "veiculosCadastrados" to total,
            "vehicleNames" to names
        )
    }

    private fun remindersPayload(reminders: List<Lembrete>): Map<String, Any> {
        val total = reminders.size
        val completed = reminders.count(::isLembreteRealizado)
        val active = (total - completed).coerceAtLeast(0)
        return mapOf(
            "remindersTotal" to total,
            "avisosTotal" to active,
            "avisosCriados" to active,
            "activeRemindersTotal" to active,
            "recordsTotal" to completed,
            "registrosTotal" to completed,
            "registrosCriados" to completed,
            "completedRemindersTotal" to completed
        )
    }

    fun syncLocalOverview(context: android.content.Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val vehicles = BancoDeDados.carregarCarros(context).orEmpty()
        val reminders = BancoDeDados.carregarLembretes(context)
        val payload = mutableMapOf<String, Any>()
        payload.putAll(vehiclePayload(vehicles))
        payload.putAll(remindersPayload(reminders))
        payload["updatedAt"] = FieldValue.serverTimestamp()

        firestore.collection("admin_users").document(uid)
            .set(
                payload,
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar visão geral local", it) }
    }

    fun syncVehicles(vehicles: List<CarroInfo>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val payload = vehiclePayload(vehicles).toMutableMap()
        payload["updatedAt"] = FieldValue.serverTimestamp()
        firestore.collection("admin_users").document(uid)
            .set(
                payload,
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar veículos", it) }
    }

    fun incrementRemindersTotal(amount: Int = 1) {
        if (amount <= 0) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("admin_users").document(uid)
            .set(mapOf("remindersTotal" to FieldValue.increment(amount.toLong())), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao incrementar remindersTotal", it) }
    }

    fun incrementAiRequests(amount: Int = 1) {
        if (amount <= 0) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("admin_users").document(uid)
            .set(mapOf("aiRequestsTotal" to FieldValue.increment(amount.toLong())), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao incrementar aiRequestsTotal", it) }
    }

    fun syncRemindersTotal(total: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val sanitizedTotal = total.coerceAtLeast(0)
        firestore.collection("admin_users").document(uid)
            .set(
                mapOf(
                    "remindersTotal" to sanitizedTotal,
                    "avisosTotal" to sanitizedTotal,
                    "avisosCriados" to sanitizedTotal,
                    "activeRemindersTotal" to sanitizedTotal,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar remindersTotal", it) }
    }

    fun syncRemindersSnapshot(reminders: List<Lembrete>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val payload = remindersPayload(reminders).toMutableMap()
        payload["updatedAt"] = FieldValue.serverTimestamp()
        firestore.collection("admin_users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar snapshot de lembretes", it) }
    }

    // Lê admin_users/{uid} UMA VEZ e salva channel + overrides em cache local.
    // Substitui syncChannelStatus + applyRemoteAdminOverride + applyRemoteEbookOverride.
    fun syncUserConfig(context: android.content.Context, onComplete: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("admin_users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                // channel (mantém prefs separada para compatibilidade)
                val channel = doc.getString("channel") ?: "oficial"
                context.getSharedPreferences("admin_channel", android.content.Context.MODE_PRIVATE)
                    .edit().putString("channel", channel).apply()
                // overrides em cache unificado
                context.getSharedPreferences("admin_user_config", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("adminPremiumOverride", doc.getBoolean("adminPremiumOverride") ?: false)
                    .putString("adminPremiumPlan", doc.getString("adminPremiumPlan") ?: "")
                    .putBoolean("adminEbookOverride", doc.getBoolean("adminEbookOverride") ?: false)
                    .putBoolean("aiBlocked", doc.getBoolean("aiBlocked") ?: false)
                    .putBoolean("webBlocked", doc.getBoolean("webBlocked") ?: false)
                    .apply()
                onComplete()
            }
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar config do usuário", it); onComplete() }
    }

    fun getChannelStatus(context: android.content.Context): String {
        return context.getSharedPreferences("admin_channel", android.content.Context.MODE_PRIVATE)
            .getString("channel", "") ?: ""
    }

    fun getCachedAdminPremiumOverride(context: android.content.Context): Boolean =
        context.getSharedPreferences("admin_user_config", android.content.Context.MODE_PRIVATE)
            .getBoolean("adminPremiumOverride", false)

    fun getCachedAdminPremiumPlan(context: android.content.Context): String? =
        context.getSharedPreferences("admin_user_config", android.content.Context.MODE_PRIVATE)
            .getString("adminPremiumPlan", null)?.takeIf { it.isNotEmpty() }

    fun getCachedAdminEbookOverride(context: android.content.Context): Boolean =
        context.getSharedPreferences("admin_user_config", android.content.Context.MODE_PRIVATE)
            .getBoolean("adminEbookOverride", false)

    fun getCachedAiBlocked(context: android.content.Context): Boolean =
        context.getSharedPreferences("admin_user_config", android.content.Context.MODE_PRIVATE)
            .getBoolean("aiBlocked", false)

    fun getCachedWebBlocked(context: android.content.Context): Boolean =
        context.getSharedPreferences("admin_user_config", android.content.Context.MODE_PRIVATE)
            .getBoolean("webBlocked", false)

    fun syncFeatureChannels(context: android.content.Context, onComplete: () -> Unit = {}) {
        firestore.collection("admin_app_config").document("feature_channels")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onComplete(); return@addOnSuccessListener }
                val prefs = context.getSharedPreferences("feature_channels", android.content.Context.MODE_PRIVATE)
                val editor = prefs.edit()
                doc.data?.forEach { (key, value) ->
                    if (value is String) editor.putString(key, value)
                }
                editor.apply()
                onComplete()
            }
            .addOnFailureListener {
                Log.w(TAG_ADMIN_SYNC, "Falha ao ler feature_channels", it)
                onComplete()
            }
    }

    fun getFeatureChannel(context: android.content.Context, featureKey: String): String {
        return context.getSharedPreferences("feature_channels", android.content.Context.MODE_PRIVATE)
            .getString(featureKey, "oficial") ?: "oficial"
    }

    fun recordLastAccess(context: android.content.Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = context.getSharedPreferences("admin_last_access", android.content.Context.MODE_PRIVATE)
        val lastRecorded = prefs.getLong("last_recorded_at", 0L)
        val thirtyMinutes = 30 * 60 * 1000L
        if (System.currentTimeMillis() - lastRecorded < thirtyMinutes) return
        firestore.collection("admin_users").document(uid)
            .set(mapOf("lastAccess" to FieldValue.serverTimestamp()), SetOptions.merge())
            .addOnSuccessListener {
                prefs.edit().putLong("last_recorded_at", System.currentTimeMillis()).apply()
            }
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao registrar lastAccess", it) }
    }

    fun checkAnnouncement(
        context: android.content.Context,
        onShow: (title: String, description: String, iconType: String, imageUrl: String) -> Unit
    ) {
        firestore.collection("admin_announcements").document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                if (doc.getBoolean("active") != true) return@addOnSuccessListener
                val id = doc.getString("id") ?: return@addOnSuccessListener
                val title = doc.getString("title") ?: return@addOnSuccessListener
                val description = doc.getString("description") ?: return@addOnSuccessListener
                val iconType = doc.getString("iconType") ?: "bell"
                val imageUrl = doc.getString("imageUrl") ?: ""
                // Filtro de audiência: "beta" → só beta testers; "todos" → todos
                val audience = doc.getString("audience") ?: "todos"
                val userChannel = getChannelStatus(context)
                if (audience == "beta" && userChannel != "beta") return@addOnSuccessListener
                val expiresAt = doc.getLong("expiresAt")
                val now = System.currentTimeMillis()
                if (expiresAt != null) {
                    if (now > expiresAt) return@addOnSuccessListener
                    onShow(title, description, iconType, imageUrl)
                } else {
                    val prefs = context.getSharedPreferences("admin_announcements", android.content.Context.MODE_PRIVATE)
                    if (prefs.getString("last_seen_id", null) == id) return@addOnSuccessListener
                    prefs.edit().putString("last_seen_id", id).apply()
                    onShow(title, description, iconType, imageUrl)
                }
            }
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao verificar anúncio", it) }
    }

    fun applyRemoteAdminOverride(
        getCurrentOverride: () -> Boolean,
        setOverride: (Boolean) -> Unit,
        setPlan: (String?) -> Unit = {},
        onChanged: () -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("admin_users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val remote = doc.getBoolean("adminPremiumOverride") ?: false
                val plan = doc.getString("adminPremiumPlan")
                if (remote != getCurrentOverride()) {
                    setOverride(remote)
                    setPlan(if (remote) plan else null)
                    onChanged()
                } else if (remote) {
                    setPlan(plan)
                }
            }
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao ler adminPremiumOverride", it) }
    }

    fun applyRemoteEbookOverride(
        getCurrentOverride: () -> Boolean,
        setOverride: (Boolean) -> Unit,
        onChanged: () -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("admin_users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val remote = doc.getBoolean("adminEbookOverride") ?: false
                if (remote != getCurrentOverride()) {
                    setOverride(remote)
                    onChanged()
                }
            }
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao ler adminEbookOverride", it) }
    }

    fun syncCurrentUser(plan: String? = null, tierName: String? = null) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = firestore.collection("admin_users").document(user.uid)

        userDoc.get()
            .addOnSuccessListener { snapshot ->
                val payload = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "name" to (user.displayName ?: ""),
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "providerIds" to user.providerData.mapNotNull { it.providerId }.distinct(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (!snapshot.exists()) {
                    payload["createdAt"] = FieldValue.serverTimestamp()
                }

                if (!plan.isNullOrBlank()) {
                    val normalized = if (plan.equals("premium", ignoreCase = true)) "premium" else "free"
                    payload["plan"] = normalized
                    payload["tier"] = normalized
                    payload["isPremium"] = normalized == "premium"
                }
                if (!tierName.isNullOrBlank()) {
                    payload["planTierName"] = tierName
                }

                userDoc.set(payload, SetOptions.merge())
                    .addOnFailureListener { error ->
                        Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar admin_users", error)
                    }
            }
            .addOnFailureListener { error ->
                Log.w(TAG_ADMIN_SYNC, "Falha ao ler admin_users para sync", error)
            }
    }

}
