package sh.zeron.android.ui.session

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

enum class ComposerMode { Draft, Sending, Steering, InputRequest, Disabled }

data class ComposerState(
    val text: String = "",
    val mode: ComposerMode = ComposerMode.Draft,
    val canSend: Boolean = false,
) {
    val isEmpty: Boolean get() = text.isBlank()
}

@Composable
fun Composer(
    state: ComposerState,
    onSend: (String) -> Unit,
    onSteer: (String) -> Unit,
    onStop: () -> Unit,
) {
    var draft by remember(state.text) { mutableStateOf(state.text) }
    Row {
        TextField(
            value = draft,
            onValueChange = { draft = it },
            enabled = state.mode != ComposerMode.Disabled && state.mode != ComposerMode.Sending,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Message input" }
        )
        when (state.mode) {
            ComposerMode.Sending -> Button(onClick = onStop) { Text("Stop") }
            ComposerMode.Steering -> Button(onClick = { onSteer(draft) }, enabled = draft.isNotBlank()) { Text("Steer") }
            else -> Button(onClick = { if (draft.isNotBlank()) onSend(draft) }, enabled = draft.isNotBlank() && state.canSend) { Text("Send") }
        }
    }
}

@Composable
fun InputRequestPanel(question: String, options: List<String>, onAnswer: (String) -> Unit, onCancel: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    Row {
        Text(question)
        options.forEach { opt ->
            Button(onClick = { selected = opt }, enabled = selected == null) { Text(opt) }
        }
        Button(onClick = { selected?.let(onAnswer) }, enabled = selected != null) { Text("Submit") }
        Button(onClick = onCancel) { Text("Cancel") }
    }
}
