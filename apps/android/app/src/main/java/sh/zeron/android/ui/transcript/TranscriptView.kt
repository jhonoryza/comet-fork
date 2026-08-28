package sh.zeron.android.ui.transcript

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import sh.zeron.android.R
import sh.zeron.android.data.AttachmentImageCache
import sh.zeron.android.data.MessageRole
import sh.zeron.android.data.Part
import sh.zeron.android.data.Transcript
import sh.zeron.android.data.TranscriptMessage
import sh.zeron.android.data.UserImageAttachment
import sh.zeron.android.data.parseUserMessageImages
import sh.zeron.android.ui.theme.MonoStyle
import sh.zeron.android.ui.theme.ZeronColors
import sh.zeron.android.ui.theme.ZeronSpacing

/**
 * The conversation. Messages carry their author, so a prompt reads as a prompt:
 * the user's turn sits in a raised bubble on the trailing edge, the agent's runs
 * full-bleed. Before this, every part rendered identically and you could not
 * tell who had said what.
 */
@Composable
fun TranscriptView(
    transcript: Transcript,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(ZeronSpacing.lg),
    attachmentDeviceId: String? = null,
    onLoadAttachment: (String, String) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()

    // Follow the tail only when the reader is already there. The old version
    // scrolled on every change, yanking you back down while reading history.
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            last == null || last.index >= info.totalItemsCount - 1
        }
    }
    val partCount = transcript.parts.size
    LaunchedEffect(partCount) {
        if (atBottom && transcript.messages.isNotEmpty()) {
            listState.animateScrollToItem(transcript.messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.lg),
    ) {
        items(transcript.messages, key = { it.id }) {
            MessageBlock(it, attachmentDeviceId, onLoadAttachment)
        }
    }
}

@Composable
private fun MessageBlock(
    message: TranscriptMessage,
    attachmentDeviceId: String?,
    onLoadAttachment: (String, String) -> Unit,
) {
    if (message.role == MessageRole.User) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Box(Modifier.fillMaxWidth(0.85f), contentAlignment = Alignment.CenterEnd) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                        .background(ZeronColors.surfaceRaised)
                        .padding(horizontal = ZeronSpacing.lg, vertical = ZeronSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
                ) {
                    message.parts.forEach { part ->
                        // User text rides the attachment-ref trailer (iOS
                        // parseUserMessageImages) — split it and render thumbs.
                        if (part is Part.Text && attachmentDeviceId != null) {
                            UserTextWithAttachments(part.text, attachmentDeviceId, onLoadAttachment)
                        } else {
                            PartView(part)
                        }
                    }
                }
            }
        }
        return
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
    ) {
        message.parts.forEach { PartView(it) }
    }
}

/** User text + any parsed attachment thumbnails (112×80, right-aligned strip). */
@Composable
private fun UserTextWithAttachments(
    content: String,
    deviceId: String,
    onLoadAttachment: (String, String) -> Unit,
) {
    val parsed = remember(content) { parseUserMessageImages(content) }
    if (parsed.attachments.isEmpty()) {
        MarkdownText(content)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm)) {
        if (parsed.text.isNotEmpty()) MarkdownText(parsed.text)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZeronSpacing.sm, Alignment.End),
        ) {
            parsed.attachments.forEach { att ->
                AttachmentThumb(deviceId, att, onLoadAttachment)
            }
        }
    }
}

/** One transcript thumbnail: loading spinner → loaded image → tap for full view. */
@Composable
private fun AttachmentThumb(
    deviceId: String,
    att: UserImageAttachment,
    onLoadAttachment: (String, String) -> Unit,
) {
    var preview by rememberSaveable(att.path) { mutableStateOf(false) }
    val snapshot = AttachmentImageCache.snapshot(deviceId, att.path)
    LaunchedEffect(deviceId, att.path) {
        if (snapshot !is AttachmentImageCache.Snapshot.Loaded) {
            onLoadAttachment(deviceId, att.path)
        }
    }
    Box(
        Modifier
            .size(width = 112.dp, height = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ZeronColors.surface)
            .border(1.dp, ZeronColors.border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (snapshot) {
            is AttachmentImageCache.Snapshot.Loaded -> {
                Image(
                    bitmap = snapshot.bitmap.asImageBitmap(),
                    contentDescription = att.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { preview = true },
                )
            }
            is AttachmentImageCache.Snapshot.Error -> {
                Text(
                    stringResource(R.string.attachment_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = ZeronColors.textFaint,
                    modifier = Modifier.clickable { onLoadAttachment(deviceId, att.path) },
                )
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ZeronColors.textFaint,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
    if (preview) {
        val loaded = snapshot as? AttachmentImageCache.Snapshot.Loaded
        if (loaded != null) {
            Dialog(onDismissRequest = { preview = false }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f))
                        .clickable { preview = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier.padding(ZeronSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
                    ) {
                        Image(
                            bitmap = loaded.bitmap.asImageBitmap(),
                            contentDescription = loaded.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            loaded.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = ZeronColors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartView(part: Part) {
    when (part) {
        is Part.Text -> MarkdownText(part.text)
        is Part.Reasoning -> ReasoningView(part)
        is Part.Tool -> ToolCard(part)
        is Part.Input -> NoticeCard(
            text = part.question,
            tone = ZeronColors.warning,
            background = ZeronColors.surfaceRaised,
        )
        is Part.Error -> NoticeCard(
            text = part.message,
            tone = ZeronColors.danger,
            background = ZeronColors.surface,
        )
    }
}

/** Rendered Markdown — headings, bullets, fenced code and inline spans. */
@Composable
fun MarkdownText(source: String, color: androidx.compose.ui.graphics.Color = ZeronColors.text) {
    val blocks = remember(source) { Markdown.parse(source) }
    Column(verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(
                    block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                )
                is MdBlock.Heading -> Text(
                    block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = ZeronSpacing.xs),
                )
                is MdBlock.Bullet -> Column(
                    verticalArrangement = Arrangement.spacedBy(ZeronSpacing.xs),
                ) {
                    block.items.forEach { item ->
                        Row {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ZeronColors.textMuted,
                            )
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                                modifier = Modifier.padding(start = ZeronSpacing.sm),
                            )
                        }
                    }
                }
                is MdBlock.Code -> CodeBlock(block.code, block.lang)
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, lang: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(ZeronColors.surface)
            .border(1.dp, ZeronColors.border, MaterialTheme.shapes.medium)
            .padding(ZeronSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.xs),
    ) {
        if (!lang.isNullOrBlank()) {
            Text(
                lang,
                style = MaterialTheme.typography.labelSmall,
                color = ZeronColors.textFaint,
            )
        }
        // Code must not reflow: wrapping a shell line changes what it says.
        Text(
            code,
            style = MonoStyle,
            color = ZeronColors.text,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun ReasoningView(part: Part.Reasoning) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(ZeronColors.surface)
            .border(1.dp, ZeronColors.border, MaterialTheme.shapes.medium)
            .padding(ZeronSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.xs),
    ) {
        Text(
            stringResource(R.string.session_thinking).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ZeronColors.textFaint,
        )
        Text(
            part.text,
            style = MaterialTheme.typography.bodyMedium,
            color = ZeronColors.textMuted,
        )
    }
}

/**
 * A tool call. Collapsed to one line by default and genuinely expandable — the
 * previous version remembered an `expanded` flag that nothing could ever set.
 */
@Composable
private fun ToolCard(part: Part.Tool) {
    var expanded by rememberSaveable(part.id) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "toolChevron",
    )
    val tone = if (part.isError) ZeronColors.danger else ZeronColors.textFaint

    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(ZeronColors.surface)
            .clickable { expanded = !expanded }
            .padding(ZeronSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZeronSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    if (part.isError) R.string.session_tool_failed else R.string.session_tool
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = tone,
            )
            Box(Modifier.width(ZeronSpacing.sm))
            Text(
                part.call.lineSequence().firstOrNull().orEmpty(),
                style = MonoStyle,
                color = ZeronColors.textMuted,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(
                    if (expanded) R.string.session_details_hide else R.string.session_details_show
                ),
                tint = ZeronColors.textFaint,
                modifier = Modifier.size(18.dp).rotate(chevronRotation),
            )
        }
        if (expanded) {
            Text(
                part.call,
                style = MonoStyle,
                color = ZeronColors.text,
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
            part.output?.let { output ->
                Text(
                    output,
                    style = MonoStyle,
                    color = ZeronColors.textMuted,
                    softWrap = false,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(
    text: String,
    tone: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = tone,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .padding(ZeronSpacing.md),
    )
}
