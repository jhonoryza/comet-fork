package sh.zeron.android.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.zeron.android.data.ChatRow
import sh.zeron.android.ui.theme.ZeronColors

@Composable
fun WorkspaceScreen(
    chats: List<ChatRow>,
    connected: Boolean,
    error: String?,
    onOpen: (String) -> Unit,
    onArchive: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(ZeronColors.bg)) {
        if (chats.isEmpty()) {
            Text(
                when {
                    error != null -> "Registry error: $error"
                    connected -> "No sessions yet.\n\nStart one from a desktop device — it appears here."
                    else -> "Connecting…"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) ZeronColors.danger else ZeronColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
            return@Box
        }
        val (active, archived) = chats.partition { !it.archived }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionLabel("Sessions") }
            items(active, key = { it.id }) { SessionRow(it, onOpen) }
            if (archived.isNotEmpty()) {
                item { SectionLabel("Archived") }
                items(archived, key = { it.id }) { SessionRow(it, onOpen) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ZeronColors.textFaint,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun SessionRow(chat: ChatRow, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZeronColors.surface)
            .clickable { onOpen(chat.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (chat.archived) ZeronColors.textFaint else ZeronColors.completed)
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                chat.title ?: chat.id,
                style = MaterialTheme.typography.bodyLarge,
                color = ZeronColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (chat.archived) {
                Text("archived", style = MaterialTheme.typography.labelSmall, color = ZeronColors.textFaint)
            }
        }
    }
}
