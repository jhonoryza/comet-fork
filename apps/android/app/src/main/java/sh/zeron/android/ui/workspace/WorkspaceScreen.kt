package sh.zeron.android.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sh.zeron.android.data.ChatRow

@Composable
fun WorkspaceScreen(
    chats: List<ChatRow>,
    onOpen: (String) -> Unit,
    onArchive: (String) -> Unit,
) {
    LazyColumn {
        items(chats, key = { it.id }) { chat ->
            Column(Modifier.clickable { onOpen(chat.id) }) {
                Text(chat.title ?: chat.id)
                Text(if (chat.archived) "archived" else "active")
            }
        }
    }
}
