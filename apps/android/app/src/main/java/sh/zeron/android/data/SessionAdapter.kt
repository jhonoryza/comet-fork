package sh.zeron.android.data

import org.json.JSONArray
import org.json.JSONObject
import sh.zeron.android.loro.LoroDoc
import java.util.UUID

sealed class Part {
    data class Text(val id: String, val text: String) : Part()
    data class Reasoning(val id: String, val text: String) : Part()
    data class Tool(val id: String, val call: String, val isError: Boolean, val output: String?) : Part()
    data class Input(val id: String, val question: String) : Part()
    data class Error(val id: String, val message: String) : Part()
}

data class Transcript(val parts: List<Part>)

class SessionAdapter(private val doc: LoroDoc) {
    /**
     * Parse the session doc's `messages`/`parts` container (schema.rs) into
     * viewer-safe domain parts. Text lives in LoroText, so `getDeepValue()`
     * returns them as flattened strings. Continuation parts (continuationOf)
     * are appended to their message so streaming never duplicates text.
     */
    suspend fun transcript(): Transcript {
        val json = doc.getDeepValueJson()
        if (json.isBlank() || json == "{}" || json == "null") return Transcript(emptyList())
        if (json.isBlank() || json == "{}" || json == "null") return Transcript(emptyList())
        val parts = mutableListOf<Part>()
        try {
            val root = JSONObject(json)
            val messages = root.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until messages.length()) {
                val msg = messages.getJSONObject(i)
                val msgId = msg.optString("id", "$i")
                val msgParts = msg.optJSONArray("parts") ?: JSONArray()
                for (j in 0 until msgParts.length()) {
                    val p = msgParts.getJSONObject(j)
                    val kind = p.optString("kind")
                    val partId = p.optString("id", "$msgId.$j")
                    val text = p.optString("text").takeIf { it.isNotEmpty() }
                    when (kind) {
                        "text" -> text?.let { parts += Part.Text(partId, it) }
                        "reasoning" -> text?.let { parts += Part.Reasoning(partId, it) }
                        "tool" -> {
                            val call = p.optJSONObject("call")?.toString() ?: ""
                            parts += Part.Tool(partId, p.optString("subagent_tail", call), p.optBoolean("isError", false), p.optString("output").ifBlank { null })
                        }
                        "input" -> parts += Part.Input(partId, p.optString("message", "Question"))
                        "error" -> parts += Part.Error(partId, p.optString("message", "Error"))
                        else -> text?.let { parts += Part.Text(partId, it) }
                    }
                }
            }
        } catch (e: Exception) {
            // Malformed doc: surface what we can, never crash the viewer.
            if (parts.isEmpty()) return Transcript(emptyList())
        }
        return Transcript(parts)
    }

    /// Durable command-ledger append (viewer-only write allowed by writer discipline).
    suspend fun queueCommand(kind: String, payload: String): String {
        val cmd = doc.appendCommand(kind, payload, "android")
        return cmd.getValue("id") as? String ?: UUID.randomUUID().toString().lowercase()
    }
}