package sh.zeron.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sh.zeron.android.auth.AuthOrg
import sh.zeron.android.data.ChatRow
import sh.zeron.android.data.Part
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
    onArchiveChat: (String) -> Unit = {},
    selectedChat: String? = null,
    transcript: Transcript = Transcript(emptyList()),
    sessionStatus: String = "connecting",
    sending: Boolean = false,
    onBack: () -> Unit = {},
    onSend: (String) -> Unit = {},
) {
    if (selectedChat != null) {
        val chat = chats.firstOrNull { it.id == selectedChat }
        SessionScreen(
            chatId = selectedChat,
            title = chat?.title,
            status = sessionStatus,
            isArchived = chat?.archived == true,
            onBack = onBack,
            transcript = { TranscriptBody(transcript, sessionStatus) },
            composer = {
                Composer(
                    state = ComposerState(
                        mode = if (sending) ComposerMode.Sending else ComposerMode.Draft,
                        canSend = !sending,
                    ),
                    onSend = onSend,
                    onSteer = onSend,
                    onStop = {},
                )
            },
        )
        return
    }
    when (state) {
        is AppState.SignedOut -> SignInScreen(onLogIn)
        is AppState.SigningIn -> SignInScreen(onLogIn, isLoading = true)
        is AppState.SelectingOrg -> OrgPickerScreen(state.orgs, onOrgSelect)
        is AppState.Ready -> WorkspaceScreen(chats, registryConnected, registryError, onOpenChat, onArchiveChat)
        is AppState.Fatal -> FatalScreen(state.message)
        else -> LoadingScreen()
    }
}

@Composable
private fun TranscriptBody(t: Transcript, status: String) {
    if (t.parts.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                if (status == "connected") "No messages yet." else status,
                style = MaterialTheme.typography.bodyMedium,
                color = ZeronColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(t.parts.size) {
        if (t.parts.isNotEmpty()) listState.animateScrollToItem(t.parts.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        items(t.parts, key = { partKey(it) }) { part -> PartRow(part) }
    }
}

private fun partKey(part: Part): String = when (part) {
    is Part.Text -> "t:${part.id}"
    is Part.Reasoning -> "r:${part.id}"
    is Part.Tool -> "l:${part.id}"
    is Part.Input -> "i:${part.id}"
    is Part.Error -> "e:${part.id}"
}

@Composable
private fun PartRow(part: Part) {
    when (part) {
        is Part.Text -> Text(
            part.text,
            style = MaterialTheme.typography.bodyLarge,
            color = ZeronColors.text,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        is Part.Reasoning -> Text(
            part.text,
            style = MaterialTheme.typography.bodyMedium,
            color = ZeronColors.textFaint,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        is Part.Tool -> Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ZeronColors.surface)
                .padding(12.dp),
        ) {
            Text(
                if (part.isError) "tool · failed" else "tool",
                style = MaterialTheme.typography.labelSmall,
                color = if (part.isError) ZeronColors.danger else ZeronColors.textFaint,
            )
            Text(part.call, style = MonoStyle, color = ZeronColors.text, maxLines = 8)
            part.output?.let {
                Text(it, style = MonoStyle, color = ZeronColors.textMuted, maxLines = 8)
            }
        }
        is Part.Input -> Text(
            part.question,
            style = MaterialTheme.typography.bodyLarge,
            color = ZeronColors.warning,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ZeronColors.surfaceRaised)
                .padding(12.dp),
        )
        is Part.Error -> Text(
            part.message,
            style = MaterialTheme.typography.bodyMedium,
            color = ZeronColors.danger,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ZeronColors.surface)
                .padding(12.dp),
        )
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
