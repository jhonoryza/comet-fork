package sh.zeron.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.data.ChatRow
import sh.zeron.android.data.Transcript
import sh.zeron.android.sync.AppState
import sh.zeron.android.ui.session.Composer
import sh.zeron.android.ui.session.ComposerMode
import sh.zeron.android.ui.session.ComposerState
import sh.zeron.android.ui.session.SessionScreen
import sh.zeron.android.ui.theme.MonoStyle
import sh.zeron.android.ui.theme.ZeronColors
import sh.zeron.android.ui.workspace.WorkspaceScreen

@Composable
fun AppRoot(
    state: AppState,
    onLogIn: () -> Unit,
    onOrgSelect: (AuthOrg) -> Unit,
    chats: List<ChatRow> = emptyList(),
    registryConnected: Boolean = true,
    registryError: String? = null,
    onOpenChat: (String) -> Unit = {},
    selectedChat: String? = null,
    transcript: Transcript = Transcript(emptyList()),
    sessionStatus: SessionStatus = SessionStatus.Connecting,
    sending: Boolean = false,
    onBack: () -> Unit = {},
    onSend: (String) -> Unit = {},
    onStop: () -> Unit = {},
) {
    if (selectedChat != null) {
        val chat = chats.firstOrNull { it.id == selectedChat }
        SessionScreen(
            title = chat?.title ?: selectedChat,
            status = sessionStatus,
            isArchived = chat?.archived == true,
            onBack = onBack,
            transcript = { padding -> TranscriptView(transcript, contentPadding = padding) },
            composer = {
                Composer(
                    state = ComposerState(
                        mode = if (sending) ComposerMode.Sending else ComposerMode.Draft,
                        canSend = !sending,
                    ),
                    onSend = onSend,
                    onSteer = onSend,
                    onStop = onStop,
                )
            },
        )
        return
    }
    when (state) {
        is AppState.SignedOut -> SignInScreen(onLogIn)
        is AppState.SigningIn -> SignInScreen(onLogIn, isLoading = true)
        is AppState.SelectingOrg -> OrgPickerScreen(state.orgs, onOrgSelect)
        is AppState.Ready -> WorkspaceScreen(chats, registryConnected, registryError, onOpenChat, {})
        is AppState.Fatal -> FatalScreen(state.message)
        else -> LoadingScreen()
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(ZeronColors.bg), contentAlignment = Alignment.Center) {
        Text("Connecting…", style = MaterialTheme.typography.bodyMedium, color = ZeronColors.textMuted)
    }
}

@Composable
private fun FatalScreen(msg: String) {
    Box(Modifier.fillMaxSize().background(ZeronColors.bg), contentAlignment = Alignment.Center) {
        Text(
            msg,
            style = MaterialTheme.typography.bodyMedium,
            color = ZeronColors.danger,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}
