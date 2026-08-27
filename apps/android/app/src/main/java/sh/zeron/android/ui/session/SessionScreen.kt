package sh.zeron.android.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SessionScreen(
    chatId: String,
    status: String,
    isArchived: Boolean,
    transcript: @Composable () -> Unit,
    composer: @Composable () -> Unit,
) {
    Column {
        Text("Session $chatId — $status${if (isArchived) " (archived)" else ""}")
        transcript()
        composer()
    }
}

@Composable
fun StatusChip(status: String, isError: Boolean = false, isInputRequest: Boolean = false) {
    Text(status)
}
