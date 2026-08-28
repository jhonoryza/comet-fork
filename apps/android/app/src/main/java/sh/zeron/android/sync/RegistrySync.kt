package sh.zeron.android.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import sh.zeron.android.data.ChatRow
import sh.zeron.android.data.RegistryAdapter
import sh.zeron.android.data.RegistryDoc
import sh.zeron.android.data.RegistryRow
import sh.zeron.android.protocol.RegistryCodec
import sh.zeron.android.protocol.RegistryFrame

/**
 * Registry room client (docs/registry-sync.md). `hello` must be the first frame
 * and can only be sent once the socket is OPEN — the reply is the `state` frame
 * that carries the row snapshot.
 *
 * Presence beats every 15s announce this device to peers (the registry's
 * ephemeral presence map). Like iOS, a phone is a viewport: it publishes
 * presence but owns no `devices` row.
 */
class RegistrySync(
    private val ws: WebSocketTransport,
    private val http: HttpTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private companion object {
        const val PRESENCE_INTERVAL_MS = 15_000L
    }

    private val doc = RegistryDoc()
    private val adapter = RegistryAdapter(doc)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected
    private val _chats = MutableStateFlow(adapter.chats())
    val chats: StateFlow<List<ChatRow>> = _chats
    /** Last transport/protocol error, for the UI to show instead of hanging. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var collectJob: Job? = null
    private var presenceJob: Job? = null
    private var cursor: Long? = null
    private var deviceId: String = ""

    fun start(cursor: Long?, deviceId: String, url: String) {
        stop()
        this.cursor = cursor
        this.deviceId = deviceId
        _lastError.value = null
        collectJob = ws.connect(url)
            .onEach { msg ->
                when (msg) {
                    // hello goes out only after the socket is open — sending it
                    // before connect() left the frame on a null socket and the
                    // room never answered (the endless "Connecting…" state).
                    is WsMessage.Connected -> {
                        ws.send(WsMessage.Text(RegistryCodec.encode(RegistryFrame.Hello(cursor, deviceId))))
                        startPresence()
                    }
                    is WsMessage.Text -> handleText(msg.text)
                    is WsMessage.Binary -> {}
                    is WsMessage.Closed -> {
                        _connected.value = false
                        presenceJob?.cancel()
                    }
                }
            }
            .catch { e ->
                _connected.value = false
                _lastError.value = e.message ?: "connection failed"
            }
            .launchIn(scope)
    }

    private fun startPresence() {
        presenceJob?.cancel()
        presenceJob = scope.launch {
            while (isActive) {
                ws.send(WsMessage.Text(RegistryCodec.encode(RegistryFrame.Presence(System.currentTimeMillis()))))
                delay(PRESENCE_INTERVAL_MS)
            }
        }
    }

    private fun handleText(text: String) {
        if (text == "pong") return
        val frame = RegistryCodec.decode(text)
        if (frame == null) {
            _lastError.value = "unparseable frame from registry"
            return
        }
        when (frame) {
            is RegistryFrame.State -> {
                doc.applyState(frame.full, parseRows(frame.rows), frame.seq)
                cursor = frame.seq
                _connected.value = true
                publish()
            }
            is RegistryFrame.Rows -> {
                doc.applyState(full = false, parseRows(frame.rows), frame.seq)
                cursor = frame.seq
                publish()
            }
            is RegistryFrame.Error -> _lastError.value = "${frame.code}: ${frame.message}"
            else -> {}
        }
    }

    private fun parseRows(rowsJson: String): List<RegistryRow> = try {
        val arr = JSONArray(rowsJson)
        (0 until arr.length()).mapNotNull {
            runCatching { RegistryRow.parse(arr.getJSONObject(it)) }.getOrNull()
        }
    } catch (_: Exception) { emptyList() }

    private fun publish() { _chats.value = adapter.chats() }

    fun kick() {}

    fun stop() {
        presenceJob?.cancel()
        presenceJob = null
        collectJob?.cancel()
        collectJob = null
        // The scope outlives a single session — cancelling it here killed every
        // later start() (the collector never ran again).
        scope.launch { ws.close() }
        _connected.value = false
    }
}
