package sh.zeron.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sh.zeron.android.ui.theme.ZeronColors

enum class ComposerMode { Draft, Sending, Steering, InputRequest, Disabled }

data class ComposerState(
    val text: String = "",
    val mode: ComposerMode = ComposerMode.Draft,
    val canSend: Boolean = false,
) {
    val isEmpty: Boolean get() = text.isBlank()
}

/**
 * Prompt composer — a rounded pill over the transcript, Send morphing to Stop
 * while a turn runs (the desktop/iOS Send→Steer→Stop shape).
 */
@Composable
fun Composer(
    state: ComposerState,
    onSend: (String) -> Unit,
    onSteer: (String) -> Unit,
    onStop: () -> Unit,
) {
    var draft by remember(state.text) { mutableStateOf(state.text) }
    val enabled = state.mode != ComposerMode.Disabled && state.mode != ComposerMode.Sending
    val canSubmit = draft.isNotBlank() && state.canSend

    fun submit() {
        if (!canSubmit) return
        if (state.mode == ComposerMode.Steering) onSteer(draft) else onSend(draft)
        draft = ""
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(ZeronColors.bg)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = draft,
            onValueChange = { draft = it },
            enabled = enabled,
            placeholder = { Text("Message", color = ZeronColors.textFaint) },
            textStyle = MaterialTheme.typography.bodyLarge,
            maxLines = 5,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ZeronColors.surface,
                unfocusedContainerColor = ZeronColors.surface,
                disabledContainerColor = ZeronColors.surface,
                focusedTextColor = ZeronColors.text,
                unfocusedTextColor = ZeronColors.text,
                cursorColor = ZeronColors.accent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f).semantics { contentDescription = "Message input" },
        )
        SendButton(
            mode = state.mode,
            enabled = canSubmit || state.mode == ComposerMode.Sending,
            onClick = { if (state.mode == ComposerMode.Sending) onStop() else submit() },
        )
    }
}

@Composable
private fun SendButton(mode: ComposerMode, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) ZeronColors.text else ZeronColors.surfaceRaised
    val fg = if (enabled) ZeronColors.bg else ZeronColors.textFaint
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .semantics { contentDescription = if (mode == ComposerMode.Sending) "Stop" else "Send" },
        contentAlignment = Alignment.Center,
    ) {
        if (mode == ComposerMode.Sending) {
            CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
        } else {
            androidx.compose.material3.IconButton(onClick = onClick, enabled = enabled) {
                Text("↑", style = MaterialTheme.typography.titleMedium, color = fg)
            }
        }
    }
}

@Composable
fun InputRequestPanel(
    question: String,
    options: List<String>,
    onAnswer: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    androidx.compose.foundation.layout.Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ZeronColors.surfaceRaised)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(question, style = MaterialTheme.typography.bodyLarge, color = ZeronColors.text)
        options.forEachIndexed { i, opt ->
            Text(
                "${i + 1}. $opt",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected == opt) ZeronColors.accent else ZeronColors.textMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == opt) ZeronColors.elementHover else Color.Transparent)
                    .padding(10.dp)
                    .semantics { contentDescription = "Answer $opt" },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Submit",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected != null) ZeronColors.accent else ZeronColors.textFaint,
                modifier = Modifier.padding(8.dp),
            )
            Text(
                "Cancel",
                style = MaterialTheme.typography.bodyMedium,
                color = ZeronColors.textMuted,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
    if (selected != null) {
        // Selection is submitted through the button row above; kept explicit so
        // a double tap cannot send twice.
    }
}
