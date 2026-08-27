package sh.zeron.android.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RegistrySync(
    private val ws: WebSocketTransport,
    private val http: HttpTransport,
) {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    suspend fun start(cursor: Long?, deviceId: String, url: String) {
        // hello/cursor, state apply via LoroAdapter, bounded backoff, liveness deadlines
        // Protocol frames prove health, not pongs.
    }
    fun kick() { /* foreground hook */ }
    suspend fun stop() { ws.close(); _connected.value = false }
}
