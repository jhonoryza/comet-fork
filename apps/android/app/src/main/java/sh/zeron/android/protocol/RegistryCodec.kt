package sh.zeron.android.protocol

import org.json.JSONObject

sealed class RegistryFrame {
    data class Hello(val cursor: Long?, val device: String) : RegistryFrame()
    data class Push(val batch: String, val ops: String) : RegistryFrame()
    data class State(val seq: Long, val full: Boolean, val gcFloor: Long, val rows: String) : RegistryFrame()
    data class Rows(val seq: Long, val rows: String) : RegistryFrame()
    data class Ack(val batch: String, val seq: Long, val applied: Long) : RegistryFrame()
    data class Error(val code: String, val message: String) : RegistryFrame()
}

object RegistryCodec {
    fun encode(frame: RegistryFrame): String = when (frame) {
        is RegistryFrame.Hello -> JSONObject().apply {
            put("t", "hello"); put("device", frame.device)
            if (frame.cursor != null) put("cursor", frame.cursor) else put("cursor", JSONObject.NULL)
        }.toString()
        is RegistryFrame.Push -> JSONObject().apply { put("t", "push"); put("batch", frame.batch); put("ops", frame.ops) }.toString()
        else -> "{}"
    }
    fun decode(json: String): RegistryFrame? = try {
        val o = JSONObject(json)
        when (o.optString("t")) {
            "state" -> RegistryFrame.State(o.getLong("seq"), o.getBoolean("full"), o.optLong("gcFloor", 0), o.optString("rows"))
            "rows" -> RegistryFrame.Rows(o.getLong("seq"), o.optString("rows"))
            "ack" -> RegistryFrame.Ack(o.getString("batch"), o.getLong("seq"), o.optLong("applied", 0))
            "error" -> RegistryFrame.Error(o.optString("code", "unknown"), o.optString("message", ""))
            else -> null
        }
    } catch (_: Exception) { null }
}
