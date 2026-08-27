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
import sh.zeron.android.auth.HttpAuthClient
import sh.zeron.android.config.DemoConfig
import sh.zeron.android.data.PersistentDeviceIdStore
import sh.zeron.android.data.SecureTokenStore
import sh.zeron.android.sync.OkHttpTransport
import sh.zeron.android.sync.OkHttpWebSocket
import sh.zeron.android.sync.RegistrySync
import sh.zeron.android.ui.AppRoot
import sh.zeron.android.ui.AppViewModel
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = applicationContext
                val tokens = SecureTokenStore(context)
                val http = OkHttpTransport()
                val deviceIdStore = PersistentDeviceIdStore(context.getSharedPreferences("zeron", MODE_PRIVATE))
                val deviceId = runBlocking { deviceIdStore.getOrCreate() }
                val config = DemoConfig.appConfig(deviceId)
                val auth = AuthStateMachine(HttpAuthClient(config, http), tokens)
                val registry = RegistrySync(OkHttpWebSocket(), http)
                return AppViewModel(auth, registry, config) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeLoader.loadOnce()
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                val chats by viewModel.chats.collectAsState()
                AppRoot(
                    state = state,
                    onSignIn = { viewModel.signIn() },
                    onOrgSelect = { viewModel.selectOrg(it) },
                    chats = chats,
                    onOpenChat = { viewModel.openChat(it) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForeground()
    }
}
