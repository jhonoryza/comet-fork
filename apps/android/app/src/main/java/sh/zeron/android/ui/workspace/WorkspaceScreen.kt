package sh.zeron.android.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.zeron.android.data.ChatRow

@Composable
fun WorkspaceScreen(
    chats: List<ChatRow>,
    connected: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String) -> Unit,
) {
    if (chats.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                if (connected) "No chats yet — start one from a connected device\n(registry room joined)."
                else "Connecting to edge…\n\nJika tetap kosong, pastikan edge berjalan dan \nDemoConfig.edgeBaseUrl benar (mis. http://10.0.2.2:8787).",
                Modifier.align(Alignment.Center)
            )
        }
        return
    }
    LazyColumn {
        items(chats, key = { it.id }) { chat ->
            Column(Modifier.clickable { onOpen(chat.id) }) {
                Text(chat.title ?: chat.id)
                Text(if (chat.archived) "archived" else "active")
            }
        }
    }
}
