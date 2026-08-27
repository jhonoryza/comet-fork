package sh.zeron.android.data

import sh.zeron.android.loro.LoroDoc

data class ChatRow(val id: String, val title: String?, val archived: Boolean, val spaceId: String?)
data class SpaceRow(val id: String, val path: String)

class RegistryAdapter(private val doc: LoroDoc) {
    suspend fun chats(): List<ChatRow> {
        val json = doc.getDeepValueJson()
        // Real impl parses doc.getDeepValue() via getDeepValueJson -> typed overlayRows
        // Deterministic mapping, missing optional fields use defaults, malformed rows skipped.
        return emptyList()
    }
    suspend fun spaces(): List<SpaceRow> = emptyList()
}
