package sh.zeron.android.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sh.zeron.android.data.SessionAdapter
import sh.zeron.android.data.Transcript
import sh.zeron.android.loro.LoroDoc

class SessionRepository(
    private val chatId: String,
    private val doc: LoroDoc,
    private val adapter: SessionAdapter,
    private val sync: ChatSync,
) {
    private val _transcript = MutableStateFlow(Transcript(emptyList()))
    val transcript: StateFlow<Transcript> = _transcript

    suspend fun open() { _transcript.value = adapter.transcript() }
    suspend fun sendPrompt(text: String) { adapter.queueCommand("run", text) }
    suspend fun steer(text: String) { adapter.queueCommand("steer", text) }
    suspend fun interrupt() { adapter.queueCommand("interrupt", "") }
    suspend fun respondInput(requestId: String, answer: String) { adapter.queueCommand("respondInput", answer) }
    suspend fun shutdown() { sync.stop(); doc.closeDoc() }
}
