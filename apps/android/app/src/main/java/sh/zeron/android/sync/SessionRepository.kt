package sh.zeron.android.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import sh.zeron.android.data.SessionAdapter
import sh.zeron.android.data.Transcript
import sh.zeron.android.loro.LoroDoc

/**
 * One open session: the native Loro doc, its chat2 room, and the transcript
 * projection the UI reads. Writer discipline (docs/chat2-sync.md): the viewer
 * appends command-ledger entries only; the host writes every transcript entry.
 */
class SessionRepository(
    private val chatId: String,
    private val doc: LoroDoc,
    private val adapter: SessionAdapter,
    private val sync: ChatSync,
    private val scope: CoroutineScope,
) {
    private val _transcript = MutableStateFlow(Transcript(emptyList()))
    val transcript: StateFlow<Transcript> = _transcript

    val connected: StateFlow<Boolean> = sync.connected
    val lastError: StateFlow<String?> = sync.lastError
    val checkpointPending: StateFlow<Boolean> = sync.checkpointPending

    /** Re-project after every batch of imported rows. */
    fun observe() {
        scope.launch {
            sync.revision.collect { _transcript.value = adapter.transcript() }
        }
    }

    fun start(cursor: Long, deviceId: String, url: String) {
        observe()
        sync.start(cursor, deviceId, url)
    }

    suspend fun refresh() { _transcript.value = adapter.transcript() }

    /** Append a command, then push the doc update so the host can drain it. */
    private suspend fun queueAndPush(kind: String, payload: JSONObject) {
        adapter.queueCommand(kind, payload.toString())
        sync.enqueue(doc.exportSnapshot())
        _transcript.value = adapter.transcript()
    }

    suspend fun sendPrompt(text: String) = queueAndPush("run", JSONObject().put("text", text))
    suspend fun steer(text: String) = queueAndPush("steer", JSONObject().put("text", text))
    suspend fun interrupt() = queueAndPush("interrupt", JSONObject())
    suspend fun respondInput(requestId: String, answer: String) =
        queueAndPush("respondInput", JSONObject().put("requestId", requestId).put("answer", answer))

    suspend fun shutdown() {
        sync.stop()
        doc.closeDoc()
    }
}
