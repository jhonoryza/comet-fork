package sh.zeron.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sh.zeron.android.auth.AuthStateMachine
import sh.zeron.android.auth.FakeAuthClient
import sh.zeron.android.data.InMemoryDeviceIdStore
import sh.zeron.android.data.InMemoryTokenStore
import sh.zeron.android.sync.FakeHttpTransport
import sh.zeron.android.sync.FakeWebSocketTransport
import sh.zeron.android.sync.RegistrySync
import sh.zeron.android.ui.AppRoot
import sh.zeron.android.ui.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val tokens = InMemoryTokenStore()
                val auth = AuthStateMachine(FakeAuthClient(), tokens)
                val registry = RegistrySync(FakeWebSocketTransport(), FakeHttpTransport())
                return AppViewModel(auth, registry) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeLoader.loadOnce()
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                AppRoot(
                    state = state,
                    onSignIn = { viewModel.signIn() },
                    onOrgSelect = { viewModel.selectOrg(it) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForeground()
    }
}
