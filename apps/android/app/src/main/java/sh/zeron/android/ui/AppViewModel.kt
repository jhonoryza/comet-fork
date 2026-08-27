package sh.zeron.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.auth.AuthStateMachine
import sh.zeron.android.config.AppConfig
import sh.zeron.android.config.DemoConfig
import sh.zeron.android.data.SessionAdapter
import sh.zeron.android.data.Transcript
import sh.zeron.android.loro.FakeLoroDoc
import sh.zeron.android.sync.AppState
import sh.zeron.android.sync.RegistrySync
import sh.zeron.android.sync.SessionRepository

class AppViewModel(
    private val auth: AuthStateMachine,
    private val registry: RegistrySync,
    private val config: AppConfig,
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.SignedOut)
    val state: StateFlow<AppState> = _state
    val chats = registry.chats

    private val _selectedChat = MutableStateFlow<String?>(null)
    val selectedChat: StateFlow<String?> = _selectedChat

    /** Live session repo for the open chat; null while in workspace. */
    private val _transcript = MutableStateFlow(Transcript(emptyList()))
    val transcript: StateFlow<Transcript> = _transcript

    fun onForeground() { registry.kick() }

    fun openChat(id: String) {
        _selectedChat.value = id
        viewModelScope.launch {
            // Provisional: native Loro import wires the real doc. For now the
            // adapter reads an (empty) doc so the shell + composer are reachable.
            val repo = SessionRepository(
                chatId = id,
                doc = FakeLoroDoc("{}"),
                adapter = SessionAdapter(FakeLoroDoc("{}")),
                sync = sh.zeron.android.sync.ChatSync("", sh.zeron.android.sync.FakeWebSocketTransport(), sh.zeron.android.sync.FakeHttpTransport()),
            )
            _transcript.value = repo.transcript.value
        }
    }

    fun closeChat() {
        _selectedChat.value = null
        _transcript.value = Transcript(emptyList())
    }

    fun sendPrompt(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val cmd = SessionAdapter(FakeLoroDoc("{}")).queueCommand("run", text)
            // optimistic echo; durable via command ledger once native doc wired
        }
    }

    fun signIn() {
        viewModelScope.launch {
            _state.value = AppState.SigningIn
            try {
                if (config.isDev) {
                    val user = DemoConfig.devUserId
                    val org = DemoConfig.devOrgId
                    auth.signInDev(user, org)
                    _state.value = AppState.SelectingOrg(listOf(AuthOrg(org, org, org)))
                } else {
                    _state.value = AppState.Fatal("WorkOS flow not wired for dev APK")
                }
            } catch (e: Throwable) {
                _state.value = AppState.Fatal(e.message ?: "sign-in failed")
            }
        }
    }

    fun selectOrg(org: AuthOrg) {
        viewModelScope.launch {
            _state.value = AppState.Connecting
            try {
                if (!config.isDev) {
                    auth.selectOrgAndRefresh(org.organizationId)
                }
                // Dev mode: bearer is already org-scoped; join the registry room.
                registry.start(
                    cursor = null,
                    deviceId = config.deviceId,
                    url = DemoConfig.registryWSUrl(org.organizationId),
                )
                _state.value = AppState.Ready
            } catch (e: Throwable) {
                _state.value = AppState.Fatal(e.message ?: "org select failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            registry.stop()
            auth.signOut()
            _state.value = AppState.SignedOut
        }
    }
}
