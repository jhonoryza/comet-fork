package sh.zeron.android.loro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sh.zeron.android.core.AppError

/** Real native-backed Loro doc via JNI. Handle owned by NativeDocBridge. */
class RealNativeLoroDoc : LoroDoc {
    private val handle: Long

    init {
        handle = NativeDocBridge.createDoc()
        if (handle == 0L) throw AppError.Loro("native doc creation failed")
    }

    override suspend fun importBytes(bytes: ByteArray) = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) throw AppError.Loro("empty bytes")
        val hex = bytes.joinToString("") { "%02x".format(it) }
        val code = NativeDocBridge.import(handle, hex)
        if (code != 0) throw AppError.Loro("import failed (code $code)")
    }

    override suspend fun exportSnapshot(): ByteArray = withContext(Dispatchers.IO) {
        // No native snapshot export via JNI yet; document limitation.
        ByteArray(0)
    }

    override suspend fun getDeepValueJson(): String = withContext(Dispatchers.IO) {
        NativeDocBridge.readJson(handle)
    }

    override suspend fun appendCommand(kind: String, payloadJson: String, issuedBy: String): Map<String, Any> =
        withContext(Dispatchers.IO) {
            val id = java.util.UUID.randomUUID().toString().lowercase()
            val entry = JSONObject()
                .put("id", id)
                .put("kind", kind)
                .put("payload", JSONObject(payloadJson))
                .put("issuedBy", issuedBy)
                .put("issuedAt", System.currentTimeMillis())
                .put("status", "pending")
                .put("resolution", JSONObject.NULL)
                .toString()
            val code = NativeDocBridge.appendCommand(handle, entry)
            if (code != 0) throw AppError.Loro("appendCommand failed (code $code)")
            mapOf("id" to id, "kind" to kind)
        }

    override suspend fun closeDoc() = withContext(Dispatchers.IO) {
        NativeDocBridge.free(handle)
    }

    override fun close() { NativeDocBridge.free(handle) }
}