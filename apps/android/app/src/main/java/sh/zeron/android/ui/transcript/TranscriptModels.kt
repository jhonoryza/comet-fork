package sh.zeron.android.ui.transcript

sealed class UiRow(val key: String) {
    data class TextRow(val id: String, val text: String) : UiRow("$id.text")
    data class CodeRow(val id: String, val code: String, val lang: String?) : UiRow("$id.code")
    data class ToolGroup(val id: String, val calls: List<ToolCall>) : UiRow("$id.g0")
    data class ErrorRow(val id: String, val message: String) : UiRow("$id.error")
    data class InputRow(val id: String, val question: String) : UiRow("$id.input")
}
data class ToolCall(val name: String, val input: String, val output: String?, val isError: Boolean = false)

fun toUiRows(parts: List<sh.zeron.android.data.Part>): List<UiRow> {
    // Stable keys while streaming, continuation joining, tool grouping deterministically
    return parts.mapNotNull {
        when (it) {
            is sh.zeron.android.data.Part.Text -> UiRow.TextRow(it.id, it.text)
            is sh.zeron.android.data.Part.Reasoning -> UiRow.TextRow(it.id, it.text)
            is sh.zeron.android.data.Part.Tool -> UiRow.ToolGroup(it.id, listOf(ToolCall(it.call, "", null, it.isError)))
            is sh.zeron.android.data.Part.Input -> UiRow.InputRow(it.id, it.question)
            is sh.zeron.android.data.Part.Error -> UiRow.ErrorRow(it.id, it.message)
        }
    }
}
