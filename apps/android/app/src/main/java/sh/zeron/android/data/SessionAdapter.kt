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

/** Message author (schema.rs `MessageRole`). */
enum class MessageRole {
    User, Assistant, System;

    companion object {
        fun parse(raw: String?): MessageRole = when (raw?.lowercase()) {
            "user" -> User
            "system" -> System
            else -> Assistant
        }
    }
}

/**
 * One authored turn. Parts stay grouped under their message so the transcript
 * can attribute them — rendering a flat part list made every row look the same
 * regardless of who produced it.
 */
data class TranscriptMessage(
    val id: String,
    val role: MessageRole,
    val parts: List<Part>,
)

data class Transcript(val messages: List<TranscriptMessage>) {
    /** Flattened view, for counts and assertions that don't care about grouping. */
    val parts: List<Part> get() = messages.flatMap { it.parts }

    val isEmpty: Boolean get() = messages.isEmpty()
}

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
        val out = mutableListOf<TranscriptMessage>()
        try {
            val root = JSONObject(json)
            val messages = root.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until messages.length()) {
                val msg = messages.getJSONObject(i)
                val msgId = msg.optString("id", "$i")
                val role = MessageRole.parse(msg.optString("role").takeIf { it.isNotEmpty() })
                val msgParts = msg.optJSONArray("parts") ?: JSONArray()
                val parts = mutableListOf<Part>()
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
                            parts += Part.Tool(
                                partId,
                                p.optString("subagent_tail", call),
                                p.optBoolean("isError", false),
                                p.optString("output").ifBlank { null },
                            )
                        }
                        "input" -> parts += Part.Input(partId, p.optString("message", "Question"))
                        "error" -> parts += Part.Error(partId, p.optString("message", "Error"))
                        else -> text?.let { parts += Part.Text(partId, it) }
                    }
                }
                if (parts.isNotEmpty()) out += TranscriptMessage(msgId, role, parts)
            }
        } catch (e: Exception) {
            // Malformed doc: surface what we can, never crash the viewer.
            if (out.isEmpty()) return Transcript(emptyList())
        }
        return Transcript(out)
    }

    /// Durable command-ledger append (viewer-only write allowed by writer discipline).
    suspend fun queueCommand(kind: String, payload: String): String {
        val cmd = doc.appendCommand(kind, payload, "android")
        return cmd.getValue("id") as? String ?: UUID.randomUUID().toString().lowercase()
    }
}
