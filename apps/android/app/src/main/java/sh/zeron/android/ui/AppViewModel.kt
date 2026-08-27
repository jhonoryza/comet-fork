package sh.zeron.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sh.zeron.android.auth.AuthStateMachine
import sh.zeron.android.sync.AppState
import sh.zeron.android.sync.RegistrySync

class AppViewModel(
    private val auth: AuthStateMachine,
    private val registry: RegistrySync,
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.SignedOut)
    val state: StateFlow<AppState> = _state

    fun onForeground() { registry.kick() }

    fun signIn() {
        viewModelScope.launch {
            _state.value = AppState.SigningIn
            // Fake org discovery — real impl calls auth.client.orgs()
            kotlinx.coroutines.delay(400)
            _state.value = AppState.SelectingOrg(listOf("Demo Org"))
        }
    }

    fun selectOrg(orgId: String) {
        viewModelScope.launch {
            _state.value = AppState.Connecting
            kotlinx.coroutines.delay(300)
            _state.value = AppState.Ready
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
