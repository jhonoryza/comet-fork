package sh.zeron.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import sh.zeron.android.R
import sh.zeron.android.ui.theme.ZeronColors
import sh.zeron.android.ui.theme.ZeronSpacing

enum class ComposerMode { Draft, Sending, Steering, Disabled }

data class ComposerState(
    val mode: ComposerMode = ComposerMode.Draft,
    val canSend: Boolean = false,
)

/**
 * Prompt composer — a rounded pill over the transcript, Send morphing to Stop
 * while a turn runs (the desktop/iOS Send→Steer→Stop shape).
 *
 * The draft is [rememberSaveable] so a rotation or a process death no longer
 * throws away a half-typed message.
 */
@Composable
fun Composer(
    state: ComposerState,
    onSend: (String) -> Unit,
    onSteer: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val sending = state.mode == ComposerMode.Sending
    val enabled = state.mode != ComposerMode.Disabled && !sending
    val canSubmit = draft.isNotBlank() && state.canSend

    fun submit() {
        if (!canSubmit) return
        if (state.mode == ComposerMode.Steering) onSteer(draft) else onSend(draft)
        draft = ""
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(ZeronColors.bg)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HorizontalDivider(color = ZeronColors.divider)
        Row(
            Modifier.padding(
                horizontal = ZeronSpacing.md,
                vertical = ZeronSpacing.sm,
            ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ZeronColors.text),
                cursorBrush = SolidColor(ZeronColors.accent),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                decorationBox = { innerTextField ->
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(ZeronColors.surface)
                            .heightIn(min = 44.dp)
                            .padding(horizontal = ZeronSpacing.lg, vertical = ZeronSpacing.md),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                stringResource(R.string.composer_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = ZeronColors.textFaint,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            SendButton(
                sending = sending,
                enabled = canSubmit,
                onSend = ::submit,
                onStop = onStop,
            )
        }
    }
}

/**
 * One button, one touch target. The old version nested an IconButton inside a
 * painted 48.dp Box (two overlapping targets), and drew a bare progress ring
 * while sending — so "Stop" announced itself to TalkBack but could not be
 * tapped at all.
 */
@Composable
private fun SendButton(
    sending: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val active = sending || enabled
    IconButton(
        onClick = { if (sending) onStop() else onSend() },
        enabled = active,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) ZeronColors.text else ZeronColors.surfaceRaised,
            contentColor = if (active) ZeronColors.bg else ZeronColors.textFaint,
            disabledContainerColor = ZeronColors.surfaceRaised,
            disabledContentColor = ZeronColors.textFaint,
        ),
        modifier = Modifier.size(44.dp).clip(CircleShape),
    ) {
        if (sending) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = ZeronColors.bg,
                    strokeWidth = 2.dp,
                )
                Icon(
                    painterResource(R.drawable.ic_stop),
                    contentDescription = stringResource(R.string.composer_stop),
                    modifier = Modifier.size(10.dp),
                )
            }
        } else {
            Icon(
                painterResource(R.drawable.ic_arrow_upward),
                contentDescription = stringResource(R.string.composer_send),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The agent asked a question with fixed choices.
 *
 * Previously inert: the options carried no click handler, so `selected` could
 * never leave null, and Submit/Cancel were bare Text — `onAnswer`/`onCancel`
 * were never called.
 */
@Composable
fun InputRequestPanel(
    question: String,
    options: List<String>,
    onAnswer: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(ZeronSpacing.md)
            .clip(MaterialTheme.shapes.medium)
            .background(ZeronColors.surfaceRaised)
            .padding(ZeronSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
    ) {
        Text(question, style = MaterialTheme.typography.bodyLarge, color = ZeronColors.text)
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .selectable(
                        selected = selected == option,
                        role = Role.RadioButton,
                        onClick = { selected = option },
                    )
                    .padding(vertical = ZeronSpacing.xs, horizontal = ZeronSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
            ) {
                RadioButton(
                    selected = selected == option,
                    onClick = null, // the whole row is the target
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ZeronColors.accent,
                        unselectedColor = ZeronColors.textFaint,
                    ),
                )
                Text(
                    option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected == option) ZeronColors.text else ZeronColors.textMuted,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZeronSpacing.sm, Alignment.End),
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    stringResource(R.string.input_request_cancel),
                    color = ZeronColors.textMuted,
                )
            }
            Button(
                onClick = { selected?.let(onAnswer) },
                enabled = selected != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZeronColors.text,
                    contentColor = ZeronColors.bg,
                    disabledContainerColor = ZeronColors.surface,
                    disabledContentColor = ZeronColors.textFaint,
                ),
            ) {
                Text(stringResource(R.string.input_request_submit))
            }
        }
    }
}
