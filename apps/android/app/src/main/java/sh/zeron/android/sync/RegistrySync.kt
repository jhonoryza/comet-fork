package sh.zeron.android.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONArray
import org.json.JSONObject
import sh.zeron.android.data.RegistryAdapter
import sh.zeron.android.data.RegistryDoc
import sh.zeron.android.data.RegistryRow
import sh.zeron.android.protocol.RegistryCodec
import sh.zeron.android.protocol.RegistryFrame

class RegistrySync(
    private val ws: WebSocketTransport,
    private val http: HttpTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private val doc = RegistryDoc()
    private val adapter = RegistryAdapter(doc)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected
    private val _chats = MutableStateFlow(adapter.chats())
    val chats: StateFlow<List<sh.zeron.android.data.ChatRow>> = _chats

    private var collectJob: Job? = null
    private var stateSeq: Long = -1

    suspend fun start(cursor: Long?, deviceId: String, url: String) {
        stop()
        ws.send(WsMessage.Text(RegistryCodec.encode(RegistryFrame.Hello(cursor, deviceId))))
        collectJob = ws.connect(url).onEach { msg ->
            when (msg) {
                is WsMessage.Connected -> {}
                is WsMessage.Text -> handleText(msg.text)
                is WsMessage.Binary -> {}
                is WsMessage.Closed -> _connected.value = false
            }
        }.catch { _connected.value = false }.launchIn(scope)
    }

    private fun handleText(text: String) {
        if (text == "pong") return
        val frame = RegistryCodec.decode(text) ?: return
        when (frame) {
            is RegistryFrame.State -> {
                doc.applyState(frame.full, parseRows(frame.rows), frame.seq)
                _connected.value = true
                publish()
            }
            is RegistryFrame.Rows -> {
                doc.applyState(full = false, parseRows(frame.rows), frame.seq)
                publish()
            }
            is RegistryFrame.Ack -> {}
            is RegistryFrame.Error -> {}
            else -> {}
        }
    }

    private fun parseRows(rowsJson: String): List<RegistryRow> = try {
        val arr = JSONArray(rowsJson)
        (0 until arr.length()).map { RegistryRow.parse(arr.getJSONObject(it)) }
    } catch (_: Exception) { emptyList() }

    private fun publish() { _chats.value = adapter.chats() }

    fun kick() {}

    suspend fun stop() {
        collectJob?.cancel()
        collectJob = null
        ws.close()
        _connected.value = false
        try { scope.cancel() } catch (_: Exception) {}
    }
}