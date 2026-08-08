package br.com.gui.carlembrete

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG_PATROCINADOS_SYNC = "PrestadoresPatrocinadosSync"
private const val COLECAO_PATROCINADOS = "prestadores_patrocinados"

object PrestadoresPatrocinadosSync {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun buscar(
        tipoServico: String,
        cidade: String?,
        estado: String?,
        onResult: (List<PrestadorPatrocinado>) -> Unit
    ) {
        firestore.collection(COLECAO_PATROCINADOS)
            .whereEqualTo("tipoServico", tipoServico)
            .whereEqualTo("ativo", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val agora = System.currentTimeMillis()
                val lista = snapshot.documents.mapNotNull { doc ->
                    val nome = doc.getString("nome") ?: return@mapNotNull null
                    val telefone = doc.getString("telefone") ?: return@mapNotNull null
                    val expiraEm = doc.getLong("expiraEm")
                    if (expiraEm != null && expiraEm < agora) return@mapNotNull null
                    val cidadeAnuncio = doc.getString("cidade")?.takeIf { it.isNotBlank() }
                    val estadoAnuncio = doc.getString("estado")?.takeIf { it.isNotBlank() }
                    if (estadoAnuncio != null && estado != null && !estadoAnuncio.equals(estado, ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    PrestadorPatrocinado(
                        id = doc.id,
                        nome = nome,
                        telefone = telefone,
                        tipoServico = tipoServico,
                        cidade = cidadeAnuncio,
                        estado = estadoAnuncio,
                        posicao = (
                            doc.getLong("posicao")
                                // Compatibilidade com os primeiros documentos do painel.
                                ?: doc.getLong("prioridade")
                                ?: 1L
                            ).toInt().coerceIn(1, 5)
                    )
                }.sortedWith(
                    compareByDescending<PrestadorPatrocinado> {
                        cidade != null && it.cidade?.equals(cidade, ignoreCase = true) == true
                    }.thenBy { it.posicao }
                )
                onResult(lista)
            }
            .addOnFailureListener { erro ->
                Log.w(TAG_PATROCINADOS_SYNC, "Falha ao buscar prestadores patrocinados", erro)
                onResult(emptyList())
            }
    }

    fun registrarClique(id: String) {
        firestore.collection(COLECAO_PATROCINADOS).document(id)
            .update("cliques", FieldValue.increment(1))
            .addOnFailureListener { erro ->
                Log.w(TAG_PATROCINADOS_SYNC, "Falha ao registrar clique do patrocinado $id", erro)
            }
    }
}
