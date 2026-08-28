package sh.zeron.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.zeron.android.ui.theme.ZeronColors

@Composable
fun SessionScreen(
    chatId: String,
    title: String?,
    status: String,
    isArchived: Boolean,
    onBack: () -> Unit,
    transcript: @Composable () -> Unit,
    composer: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(ZeronColors.bg)) {
        SessionHeader(title ?: chatId, status, isArchived, onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) { transcript() }
        composer()
    }
}

@Composable
private fun SessionHeader(title: String, status: String, isArchived: Boolean, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ZeronColors.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = ZeronColors.textMuted)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ZeronColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(status, isArchived)
            }
        }
    }
}

@Composable
fun StatusChip(status: String, isArchived: Boolean = false) {
    val color = when {
        isArchived -> ZeronColors.textFaint
        status.contains("failed") || status.contains("error") -> ZeronColors.danger
        status == "connected" -> ZeronColors.completed
        status.contains("compacted") -> ZeronColors.warning
        else -> ZeronColors.textMuted
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            if (isArchived) "archived" else status,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
