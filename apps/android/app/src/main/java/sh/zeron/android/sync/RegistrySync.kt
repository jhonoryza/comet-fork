package sh.zeron.android.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import sh.zeron.android.protocol.RegistryCodec
import sh.zeron.android.protocol.RegistryFrame

class RegistrySync(
    private val ws: WebSocketTransport,
    private val http: HttpTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private var collectJob: Job? = null
    private var flow: Flow<WsMessage>? = null

    suspend fun start(cursor: Long?, deviceId: String, url: String) {
        stop()
        flow = ws.connect(url)
        ws.send(WsMessage.Text(RegistryCodec.encode(RegistryFrame.Hello(cursor, deviceId))))
        collectJob = flow?.onEach { msg ->
            when (msg) {
                is WsMessage.Text -> handle(frame(msg.text))
                is WsMessage.Connected -> _connected.value = true
                is WsMessage.Closed -> _connected.value = false
                is WsMessage.Binary -> {}
            }
        }?.catch { _connected.value = false }?.launchIn(scope)
    }

    private fun frame(text: String): RegistryFrame? = text.takeIf { it != "pong" }?.let { RegistryCodec.decode(it) }

    private fun handle(f: RegistryFrame?) {
        when (f) {
            // state = hello answer: connection established + rows snapshot.
            // Rows are applied through the Loro adapter in WorkspaceRepository;
            // here we only track liveness (protocol frames prove health).
            is RegistryFrame.State -> { _connected.value = true }
            is RegistryFrame.Rows -> {}
            is RegistryFrame.Ack -> {}
            is RegistryFrame.Error -> {}
            else -> {}
        }
    }

    fun kick() { /* foreground: redial handled by caller re-calling start */ }

    suspend fun stop() {
        collectJob?.cancel()
        collectJob = null
        ws.close()
        _connected.value = false
    }
}