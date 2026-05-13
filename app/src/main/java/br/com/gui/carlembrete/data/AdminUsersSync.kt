package br.com.gui.carlembrete

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
            "vehicleNames" to names,
            "carros" to vehicles.map(::vehicleSnapshotPayload)
        )
    }

    private fun vehicleSnapshotPayload(vehicle: CarroInfo): Map<String, Any?> = mapOf(
        "id" to vehicle.id,
        "nome" to vehicle.nome,
        "modelo" to vehicle.modelo,
        "marca" to vehicle.marca,
        "proprietario" to vehicle.proprietario,
        "corArgb" to vehicle.corArgb,
        "kmAtual" to vehicle.kmAtual,
        "semControleKm" to vehicle.semControleKm,
        "tipoVeiculo" to vehicle.tipoVeiculo.name,
        "vezesBatido" to vehicle.vezesBatido,
        "tempoComVeiculo" to vehicle.tempoComVeiculo
    )

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
            "completedRemindersTotal" to completed,
            "lembretes" to reminders.map(::reminderSnapshotPayload)
        )
    }

    private fun reminderSnapshotPayload(reminder: Lembrete): Map<String, Any?> = mapOf(
        "id" to reminder.id,
        "carroId" to reminder.carroId,
        "titulo" to reminder.titulo,
        "peca" to reminder.peca,
        "dataLimite" to reminder.dataLimite,
        "kmLimite" to reminder.kmLimite,
        "tipo" to reminder.tipo.name,
        "valor" to reminder.valor
    )

    private fun fuelPayload(items: List<Abastecimento>): Map<String, Any> = mapOf(
        "abastecimentos" to items.map(::fuelSnapshotPayload)
    )

    private fun fuelSnapshotPayload(item: Abastecimento): Map<String, Any?> = mapOf(
        "id" to item.id,
        "carroId" to item.carroId,
        "data" to item.data,
        "precoLitro" to item.precoLitro,
        "valorPago" to item.valorPago,
        "litros" to item.litros
    )

    fun syncLocalOverview(context: android.content.Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val vehicles = BancoDeDados.carregarCarros(context).orEmpty()
        val reminders = BancoDeDados.carregarLembretes(context)
        val fuelRecords = BancoDeDados.carregarAbastecimentos(context)
        val payload = mutableMapOf<String, Any>()
        payload.putAll(vehiclePayload(vehicles))
        payload.putAll(remindersPayload(reminders))
        payload.putAll(fuelPayload(fuelRecords))
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

    fun syncFuelSnapshot(items: List<Abastecimento>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val payload = fuelPayload(items).toMutableMap()
        payload["updatedAt"] = FieldValue.serverTimestamp()
        firestore.collection("admin_users").document(uid)
            .set(payload, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao sincronizar abastecimentos", it) }
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

    fun listenReminderLimitOverride(onChanged: (Int?) -> Unit): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val userDoc = firestore.collection("admin_users").document(uid)
        val controlsDoc = userDoc.collection("controls").document("admin")

        fun readLegacyLimitOnce() {
            userDoc.get()
                .addOnSuccessListener { snapshot ->
                    val legacyLimit = snapshot
                        ?.getLong("adminReminderLimitOverride")
                        ?.toInt()
                        ?.takeIf { it > 0 }
                    onChanged(legacyLimit)
                }
                .addOnFailureListener { Log.w(TAG_ADMIN_SYNC, "Falha ao ler limite admin legado de avisos", it) }
        }

        return controlsDoc
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG_ADMIN_SYNC, "Falha ao ouvir limite admin de avisos", error)
                    readLegacyLimitOnce()
                    return@addSnapshotListener
                }
                if (snapshot?.exists() != true) {
                    readLegacyLimitOnce()
                    return@addSnapshotListener
                }
                val remoteLimit = snapshot
                    .getLong("adminReminderLimitOverride")
                    ?.toInt()
                    ?.takeIf { it > 0 }
                onChanged(remoteLimit)
            }
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
