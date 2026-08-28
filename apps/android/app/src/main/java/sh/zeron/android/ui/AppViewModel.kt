package sh.zeron.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.auth.AuthStateMachine
import sh.zeron.android.config.AppConfig
import sh.zeron.android.config.EdgeConfig
import sh.zeron.android.config.WorkOsAuth
import sh.zeron.android.data.SessionAdapter
import sh.zeron.android.data.Transcript
import sh.zeron.android.loro.LoroDoc
import sh.zeron.android.loro.RealNativeLoroDoc
import sh.zeron.android.sync.AppState
import sh.zeron.android.sync.ChatSync
import sh.zeron.android.sync.HttpTransport
import sh.zeron.android.sync.OkHttpWebSocket
import sh.zeron.android.sync.RegistrySync
import sh.zeron.android.sync.SessionRepository

class AppViewModel(
    private val auth: AuthStateMachine,
    private val registry: RegistrySync,
    private val http: HttpTransport,
    private val config: AppConfig,
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.SignedOut)
    val state: StateFlow<AppState> = _state
    val chats = registry.chats

    private val _selectedChat = MutableStateFlow<String?>(null)
    val selectedChat: StateFlow<String?> = _selectedChat

    /** False when the registry room isn't joined yet (no chats to show). */
    val registryConnected = registry.connected

    /** Surfaced so a failed room join shows a reason instead of a spinner. */
    val registryError = registry.lastError

    private val _transcript = MutableStateFlow(Transcript(emptyList()))
    val transcript: StateFlow<Transcript> = _transcript
    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Connecting)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus
    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private var openDoc: LoroDoc? = null
    private var session: SessionRepository? = null
    private var orgId: String? = null

    /** CSRF state for the in-flight AuthKit round trip. */
    private var pendingState: String? = null

    /**
     * Back from the background. `RegistrySync.kick()` is a no-op, so a dropped
     * room used to stay dropped until the user killed the app — rejoin here
     * instead when we know we should be connected but aren't.
     */
    fun onForeground() {
        if (_state.value is AppState.Ready && !registry.connected.value) retryRegistry()
    }

    /** Explicit "Try again" from the workspace's disconnected/error state. */
    fun retryRegistry() {
        val org = orgId ?: return
        viewModelScope.launch {
            runCatching { connectRegistry(org) }
                .onFailure { _state.value = AppState.Fatal(it.message ?: "reconnect failed") }
        }
    }

    private suspend fun connectRegistry(organizationId: String) {
        val token = auth.accessToken()
            ?: throw IllegalStateException("no access token")
        registry.start(
            cursor = null,
            deviceId = config.deviceId,
            url = EdgeConfig.registryWSUrl(organizationId, token, config.deviceId),
        )
    }

    fun openChat(id: String) {
        _selectedChat.value = id
        closeSession()
        _sessionStatus.value = SessionStatus.Connecting
        val doc = try { RealNativeLoroDoc() } catch (e: Throwable) {
            _sessionStatus.value = SessionStatus.Failed(e.message ?: "native doc failed")
            return
        }
        openDoc = doc
        val sync = ChatSync(id, OkHttpWebSocket(), http, doc)
        val repo = SessionRepository(id, doc, SessionAdapter(doc), sync, viewModelScope)
        session = repo

        viewModelScope.launch {
            val token = auth.accessToken()
            if (token == null) {
                _sessionStatus.value = SessionStatus.SignedOut
                return@launch
            }
            repo.start(
                cursor = 0,
                deviceId = config.deviceId,
                url = EdgeConfig.chat2WSUrl(id, token, config.deviceId),
            )
            launch { repo.transcript.collect { _transcript.value = it } }
            launch {
                repo.connected.collect { on -> if (on) _sessionStatus.value = SessionStatus.Connected }
            }
            launch {
                repo.lastError.collect { e -> if (e != null) _sessionStatus.value = SessionStatus.Failed(e) }
            }
            launch {
                repo.checkpointPending.collect { pending ->
                    if (pending) _sessionStatus.value = SessionStatus.HistoryTrimmed
                }
            }
            repo.refresh()
        }
    }

    private fun closeSession() {
        val repo = session
        session = null
        val doc = openDoc
        openDoc = null
        if (repo != null) viewModelScope.launch { repo.shutdown() } else doc?.close()
    }

    fun closeChat() {
        _selectedChat.value = null
        closeSession()
        _transcript.value = Transcript(emptyList())
        _sessionStatus.value = SessionStatus.Connecting
        _sending.value = false
    }

    fun sendPrompt(text: String) {
        if (text.isBlank()) return
        val repo = session ?: return
        viewModelScope.launch {
            _sending.value = true
            try {
                repo.sendPrompt(text.trim())
            } catch (e: Throwable) {
                _sessionStatus.value = SessionStatus.Failed(e.message ?: "send failed")
            } finally {
                _sending.value = false
            }
        }
    }

    /** Ask the host to abandon the running turn (chat2 `interrupt` command). */
    fun interrupt() {
        val repo = session ?: return
        viewModelScope.launch {
            try {
                repo.interrupt()
            } catch (e: Throwable) {
                _sessionStatus.value = SessionStatus.Failed(e.message ?: "interrupt failed")
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
        return WorkOsAuth.authorizeUrl(state)
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
        val expected = pendingState ?: return
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
                orgId = org.organizationId
                connectRegistry(org.organizationId)
                _state.value = AppState.Ready
            } catch (e: Throwable) {
                _state.value = AppState.Fatal(e.message ?: "org select failed")
            }
        }
    }

    /** Leave the dead-end Fatal screen without needing to kill the app. */
    fun dismissFatal() {
        if (_state.value is AppState.Fatal) _state.value = AppState.SignedOut
    }

    fun signOut() {
        viewModelScope.launch {
            registry.stop()
            auth.signOut()
            closeSession()
            orgId = null
            _selectedChat.value = null
            _transcript.value = Transcript(emptyList())
            _state.value = AppState.SignedOut
        }
    }
}
