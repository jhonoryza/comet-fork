package sh.zeron.android.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatSync(
    private val chatId: String,
    private val ws: WebSocketTransport,
    private val http: HttpTransport,
) {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    suspend fun start(cursor: Long, deviceId: String, url: String) {
        // hello/state, cursor amnesty, checkpoint vs rows-only plan,
        // checkpoint GET Range-resume, buffered rows replay, contiguity guard
    }
    suspend fun enqueueUpdate(bytes: ByteArray) { /* queue batch, send after state, dedupe */ }
    suspend fun handleAck(batchId: String, seq: Long) { /* contiguity check, gap backfill */ }
    fun kick() {}
    suspend fun stop() { ws.close(); _connected.value = false }
}
