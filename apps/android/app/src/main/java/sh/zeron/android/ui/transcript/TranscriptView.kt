package sh.zeron.android.ui.transcript

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column

@Composable
fun TranscriptView(rows: List<UiRow>) {
    val state = rememberLazyListState()
    var stick by remember { mutableStateOf(true) }
    LaunchedEffect(rows.size) { if (stick) state.animateScrollToItem(rows.size.coerceAtLeast(1) - 1) }
    LazyColumn(state = state) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is UiRow.TextRow -> MarkdownRenderer(listOf(Block.Paragraph(row.text)))
                is UiRow.CodeRow -> CodeBlock(row.code, row.lang)
                is UiRow.ToolGroup -> ToolGroupView(row)
                is UiRow.ErrorRow -> Text("Error: ${row.message}")
                is UiRow.InputRow -> Text("Input: ${row.question}")
            }
        }
    }
}

@Composable
fun ToolGroupView(group: UiRow.ToolGroup) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("${group.calls.size} tool calls" + if (expanded) " (expanded)" else " (collapsed)")
        if (expanded) group.calls.forEach { Text(it.input) }
    }
}
