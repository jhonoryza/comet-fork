package sh.zeron.android.ui

import androidx.lifecycle.ViewModel
import org.json.JSONObject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.auth.AuthStateMachine
import sh.zeron.android.config.AppConfig
import sh.zeron.android.config.EdgeConfig
import sh.zeron.android.data.SessionAdapter
import sh.zeron.android.data.Transcript
import sh.zeron.android.loro.LoroDoc
import sh.zeron.android.loro.RealNativeLoroDoc
import sh.zeron.android.sync.AppState
import sh.zeron.android.sync.RegistrySync

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

    /** False when the registry room isn't joined yet (no chats to show). */
    val registryConnected = registry.connected

    /** Live session doc for the open chat; null while in workspace. */
    private var openDoc: LoroDoc? = null
    private val _transcript = MutableStateFlow(Transcript(emptyList()))
    val transcript: StateFlow<Transcript> = _transcript

    /** CSRF state for the in-flight AuthKit round trip. */
    private var pendingState: String? = null

    fun onForeground() { registry.kick() }

    fun openChat(id: String) {
        _selectedChat.value = id
        closeDoc()
        val doc = try { RealNativeLoroDoc() } catch (e: Throwable) {
            _state.value = AppState.Fatal("native doc failed: ${e.message}")
            return
        }
        openDoc = doc
        viewModelScope.launch {
            _transcript.value = SessionAdapter(doc).transcript()
        }
    }

    private fun closeDoc() {
        openDoc?.close()
        openDoc = null
    }

    fun closeChat() {
        _selectedChat.value = null
        closeDoc()
        _transcript.value = Transcript(emptyList())
    }

    fun sendPrompt(text: String) {
        if (text.isBlank()) return
        val doc = openDoc ?: return
        viewModelScope.launch {
            try {
                SessionAdapter(doc).queueCommand("run", """{"text":${JSONObject.quote(text)}}""")
                _transcript.value = SessionAdapter(doc).transcript()
            } catch (e: Throwable) {
                _state.value = AppState.Fatal("send failed: ${e.message}")
            }
        }
    }

    /**
     * WorkOS AuthKit: the caller opens `authorizeUrl(state)` in a browser tab;
     * the callback returns here with the code (iOS SignInView.signIn parity).
     */
    fun beginSignIn(): String {
        val state = java.util.UUID.randomUUID().toString()
        pendingState = state
        _state.value = AppState.SigningIn
        return sh.zeron.android.config.WorkOsAuth.authorizeUrl(state)
    }

    /** Browser tab dismissed without a callback. */
    fun signInAborted() {
        if (_state.value is AppState.SigningIn) {
            pendingState = null
            _state.value = AppState.SignedOut
        }
    }

    /** Deep-link callback: exchange the code once, state must match. */
    fun onAuthCallback(uri: android.net.Uri) {
        val expected = pendingState
        if (expected == null) return
        val callback = sh.zeron.android.auth.DeepLinkHandler.parse(uri, expected)
        pendingState = null
        if (callback == null) {
            _state.value = AppState.Fatal("Callback missing code or state mismatch")
            return
        }
        viewModelScope.launch {
            _state.value = AppState.SigningIn
            try {
                val orgs = auth.signInWithCode(callback.code)
                _state.value =
                    if (orgs.isEmpty()) AppState.Fatal("No organizations on this account")
                    else AppState.SelectingOrg(orgs)
            } catch (e: Throwable) {
                _state.value = AppState.Fatal(e.message ?: "sign-in failed")
            }
        }
    }

    fun selectOrg(org: AuthOrg) {
        viewModelScope.launch {
            _state.value = AppState.Connecting
            try {
                auth.selectOrgAndRefresh(org.organizationId)
                val token = auth.accessToken()
                    ?: throw IllegalStateException("no access token after refresh")
                registry.start(
                    cursor = null,
                    deviceId = config.deviceId,
                    url = EdgeConfig.registryWSUrl(org.organizationId, token, config.deviceId),
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
            closeDoc()
            _state.value = AppState.SignedOut
        }
    }
}