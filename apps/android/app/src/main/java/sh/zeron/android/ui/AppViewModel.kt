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
import sh.zeron.android.sync.AppState
import sh.zeron.android.sync.RegistrySync

class AppViewModel(
    private val auth: AuthStateMachine,
    private val registry: RegistrySync,
    private val config: AppConfig,
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.SignedOut)
    val state: StateFlow<AppState> = _state

    fun onForeground() { registry.kick() }

    fun signIn() {
        viewModelScope.launch {
            _state.value = AppState.SigningIn
            try {
                if (config.isDev) {
                    val user = DemoConfig.devUserId
                    val org = DemoConfig.devOrgId
                    auth.signInDev(user, org)
                    _state.value = AppState.SelectingOrg(listOf(org))
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
