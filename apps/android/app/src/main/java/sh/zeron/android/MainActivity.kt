package sh.zeron.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import sh.zeron.android.auth.AuthStateMachine
import sh.zeron.android.auth.HttpAuthClient
import sh.zeron.android.config.EdgeConfig
import sh.zeron.android.config.WorkOsAuth
import sh.zeron.android.data.PersistentDeviceIdStore
import sh.zeron.android.data.SecureTokenStore
import sh.zeron.android.sync.OkHttpTransport
import sh.zeron.android.sync.OkHttpWebSocket
import sh.zeron.android.sync.RegistrySync
import sh.zeron.android.ui.AppRoot
import sh.zeron.android.ui.AppViewModel

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
                val config = EdgeConfig.appConfig(deviceId)
                val auth = AuthStateMachine(HttpAuthClient(config, http), tokens)
                val registry = RegistrySync(OkHttpWebSocket(), http)
                return AppViewModel(auth, registry, config) as T
            }
        }
    }

    /** True while the AuthKit tab is in front, so onResume can detect a dismiss. */
    private var awaitingCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeLoader.loadOnce()
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                val chats by viewModel.chats.collectAsState()
                val selected by viewModel.selectedChat.collectAsState()
                val transcript by viewModel.transcript.collectAsState()
                val registryConnected by viewModel.registryConnected.collectAsState()
                val registryError by viewModel.registryError.collectAsState()
                AppRoot(
                    state = state,
                    onLogIn = { launchAuthKit() },
                    onOrgSelect = { viewModel.selectOrg(it) },
                    chats = chats,
                    registryConnected = registryConnected,
                    registryError = registryError,
                    onOpenChat = { viewModel.openChat(it) },
                    selectedChat = selected,
                    transcript = transcript,
                    onBack = { viewModel.closeChat() },
                    onSend = { viewModel.sendPrompt(it) },
                )
            }
        }
        handleAuthIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != WorkOsAuth.CALLBACK_SCHEME || uri.host != WorkOsAuth.CALLBACK_HOST) return
        awaitingCallback = false
        viewModel.onAuthCallback(uri)
    }

    /** Open WorkOS AuthKit in a Custom Tab; return arrives via zeron://callback. */
    private fun launchAuthKit() {
        val url = viewModel.beginSignIn()
        awaitingCallback = true
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    }

    override fun onResume() {
        super.onResume()
        if (awaitingCallback) {
            // Back on our activity with no callback intent = user dismissed the tab.
            awaitingCallback = false
            viewModel.signInAborted()
        }
        viewModel.onForeground()
    }
}
