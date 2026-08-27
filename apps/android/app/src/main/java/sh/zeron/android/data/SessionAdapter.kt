package sh.zeron.android.data

import sh.zeron.android.loro.LoroDoc

sealed class Part { data class Text(val id: String, val text: String) : Part(); data class Tool(val id: String, val call: String) : Part() }
data class Transcript(val parts: List<Part>)

class SessionAdapter(private val doc: LoroDoc) {
    suspend fun transcript(): Transcript {
        val json = doc.getDeepValueJson()
        // Real impl joins parts+continuationOf, groups tools, maps command ledger (client ids)
        // Viewer never mutates host-owned transcript fields.
        return Transcript(emptyList())
    }
    suspend fun queueCommand(kind: String, payload: String): String {
        val id = java.util.UUID.randomUUID().toString().lowercase()
        // Append to commands LoroList with client-minted id
        return id
    }
}
