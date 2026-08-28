package sh.zeron.android.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import sh.zeron.android.loro.LoroDoc
import sh.zeron.android.protocol.Chat2Codec
import java.util.UUID

/**
 * chat2 room client (docs/chat2-sync.md). One socket per chat:
 * `hello{cursor,device}` → `state` header, then a `rowsReq` backfill whose rows
 * are opaque Loro updates imported into the session doc. Local writes are
 * pushed as `push{batchId}` frames and retired on `ack`.
 *
 * Checkpoint fetch is not implemented yet: a room whose history was compacted
 * reports `checkpointSize > 0`, and this client can only replay the rows still
 * in the log. `checkpointPending` says so instead of showing an empty transcript.
 */
class ChatSync(
    private val chatId: String,
    private val ws: WebSocketTransport,
    private val http: HttpTransport,
    private val doc: LoroDoc,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected
    /** Bumped whenever imported rows changed the doc, so the UI re-reads it. */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    /** True when the room has a checkpoint this client cannot fetch yet. */
    private val _checkpointPending = MutableStateFlow(false)
    val checkpointPending: StateFlow<Boolean> = _checkpointPending

    private var collectJob: Job? = null
    private var cursor: Long = 0
    private var deviceId: String = ""
    private var stateReceived = false
    private val pending = LinkedHashMap<String, ByteArray>()

    fun start(cursor: Long, deviceId: String, url: String) {
        stop()
        this.cursor = cursor
        this.deviceId = deviceId
        stateReceived = false
        _lastError.value = null
        collectJob = ws.connect(url)
            .onEach { msg ->
                when (msg) {
                    is WsMessage.Connected -> ws.send(WsMessage.Binary(Chat2Codec.hello(cursor, deviceId)))
                    is WsMessage.Binary -> handleFrame(msg.bytes)
                    is WsMessage.Text -> {} // "pong" — transport only
                    is WsMessage.Closed -> {
                        _connected.value = false
                        stateReceived = false
                    }
                }
            }
            .catch { e ->
                _connected.value = false
                _lastError.value = e.message ?: "connection failed"
            }
            .launchIn(scope)
    }

    private fun handleFrame(bytes: ByteArray) {
        val frame = Chat2Codec.decode(bytes) ?: run {
            _lastError.value = "unparseable chat frame"
            return
        }
        when (frame.kind) {
            Chat2Codec.STATE -> {
                val checkpointSize = frame.header.optLong("checkpointSize", 0)
                val checkpointSeq = frame.header.optLong("checkpointSeq", 0)
                stateReceived = true
                _connected.value = true
                // A compacted room needs GET /checkpoint before its rows make
                // sense; without that fetch the honest move is to say so.
                _checkpointPending.value = checkpointSize > 0 && cursor < checkpointSeq
                scope.launch {
                    ws.send(WsMessage.Binary(Chat2Codec.rowsReq(cursor, excludeOwn = false)))
                    flushPending()
                }
            }
            Chat2Codec.ROW -> {
                val seq = frame.header.optLong("seq", 0)
                scope.launch {
                    try {
                        doc.importBytes(frame.payload)
                        // Contiguity: the cursor may walk, never jump a gap.
                        if (seq <= cursor + 1) cursor = seq
                        _revision.value += 1
                    } catch (e: Throwable) {
                        _lastError.value = "row import failed: ${e.message}"
                    }
                }
            }
            Chat2Codec.ROWS_DONE -> _revision.value += 1
            Chat2Codec.ACK -> {
                val batchId = frame.header.optString("batchId")
                val seq = frame.header.optLong("seq", 0)
                pending.remove(batchId)
                if (seq <= cursor + 1) cursor = seq
            }
            Chat2Codec.ERROR -> {
                val code = frame.header.optString("code", "unknown")
                val batchId = frame.header.optString("batchId")
                _lastError.value = "$code: ${frame.header.optString("message")}"
                // Permanent verdicts retire the batch; otherwise it would replay
                // on every reconnect forever.
                if (code in setOf("too_large", "empty", "bad_push") && batchId.isNotEmpty()) {
                    pending.remove(batchId)
                }
            }
            else -> {} // presence / probe-ok / future frames
        }
    }

    /** Queue a local update for push; survives reconnects until acked. */
    fun enqueue(update: ByteArray) {
        if (update.isEmpty()) return
        pending[UUID.randomUUID().toString().lowercase()] = update
        if (stateReceived) scope.launch { flushPending() }
    }

    private suspend fun flushPending() {
        if (!stateReceived) return
        for ((batchId, bytes) in pending.entries.toList()) {
            ws.send(WsMessage.Binary(Chat2Codec.push(batchId, bytes)))
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
        scope.launch { ws.close() }
        _connected.value = false
        stateReceived = false
    }
}
