package sh.zeron.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.data.ChatRow
import sh.zeron.android.data.Part
import sh.zeron.android.data.Transcript
import sh.zeron.android.sync.AppState
import sh.zeron.android.ui.session.Composer
import sh.zeron.android.ui.session.ComposerState
import sh.zeron.android.ui.session.SessionScreen
import sh.zeron.android.ui.workspace.WorkspaceScreen

@Composable
fun AppRoot(
    state: AppState,
    onSignIn: () -> Unit,
    onOrgSelect: (AuthOrg) -> Unit,
    chats: List<ChatRow> = emptyList(),
    registryConnected: Boolean = true,
    onOpenChat: (String) -> Unit = {},
    onArchiveChat: (String) -> Unit = {},
    selectedChat: String? = null,
    transcript: Transcript = Transcript(emptyList()),
    onBack: () -> Unit = {},
    onSend: (String) -> Unit = {},
) {
    if (selectedChat != null) {
        SessionScreen(
            chatId = selectedChat,
            status = "connected",
            isArchived = false,
            transcript = { TranscriptBody(transcript) },
            composer = { Composer(ComposerState(canSend = true), { onSend(it) }, {}, {}) },
        )
        return
    }
    when (state) {
        is AppState.SignedOut -> SignInScreen(onSignIn)
        is AppState.SelectingOrg -> OrgPickerScreen(state.orgs, onOrgSelect)
        is AppState.Ready -> WorkspaceScreen(chats, registryConnected, onOpenChat, onArchiveChat)
        is AppState.Fatal -> FatalScreen(state.message)
        else -> LoadingScreen()
    }
}

@Composable
private fun TranscriptBody(t: Transcript) {
    Column(Modifier.padding(16.dp)) {
        if (t.parts.isEmpty()) {
            Text("No transcript yet.")
        } else {
            t.parts.forEach { part ->
                Text(when (part) {
                    is Part.Text -> part.text
                    is Part.Reasoning -> part.text
                    is Part.Tool -> "[tool] ${part.call.take(80)}"
                    is Part.Input -> "[input] ${part.question}"
                    is Part.Error -> "[error] ${part.message}"
                }, Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
        }
    }
}

@Composable private fun LoadingScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Connecting…") } }
@Composable private fun FatalScreen(msg: String) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: $msg") } }
