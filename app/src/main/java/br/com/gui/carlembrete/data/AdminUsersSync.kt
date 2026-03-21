package br.com.gui.carlembrete

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val TAG_ADMIN_SYNC = "AdminUsersSync"

object AdminUsersSync {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun syncCurrentUser(plan: String? = null) {
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
                    "lastAccess" to FieldValue.serverTimestamp(),
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

