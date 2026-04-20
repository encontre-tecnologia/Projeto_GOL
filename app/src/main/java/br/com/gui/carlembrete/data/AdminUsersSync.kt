package br.com.gui.carlembrete

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val TAG_ADMIN_SYNC = "AdminUsersSync"

object AdminUsersSync {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun syncVehicles(vehicles: List<CarroInfo>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val names = vehicles.map { "${it.nome} ${it.modelo}".trim() }
        firestore.collection("admin_users").document(uid)
            .set(
                mapOf(
                    "vehiclesTotal" to vehicles.size,
                    "vehicleNames" to names,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
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

    fun syncRemindersTotal(total: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("admin_users").document(uid)
            .set(
                mapOf(
                    "remindersTotal" to total.coerceAtLeast(0),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar remindersTotal", it) }
    }

    fun checkAnnouncement(context: android.content.Context, onShow: (title: String, description: String) -> Unit) {
        firestore.collection("admin_announcements").document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                if (doc.getBoolean("active") != true) return@addOnSuccessListener
                val id = doc.getString("id") ?: return@addOnSuccessListener
                val title = doc.getString("title") ?: return@addOnSuccessListener
                val description = doc.getString("description") ?: return@addOnSuccessListener
                val expiresAt = doc.getLong("expiresAt")
                val now = System.currentTimeMillis()
                if (expiresAt != null) {
                    if (now > expiresAt) return@addOnSuccessListener
                    onShow(title, description)
                } else {
                    val prefs = context.getSharedPreferences("admin_announcements", android.content.Context.MODE_PRIVATE)
                    if (prefs.getString("last_seen_id", null) == id) return@addOnSuccessListener
                    prefs.edit().putString("last_seen_id", id).apply()
                    onShow(title, description)
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
