package sh.zeron.android.ui.transcript

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString

interface MarkdownParser {
    fun parse(markdown: String): List<Block>
}
sealed class Block { data class Paragraph(val text: String) : Block(); data class Code(val text: String, val lang: String?) : Block(); data class ListBlock(val items: List<String>) : Block() }

object NoopMarkdownParser : MarkdownParser {
    override fun parse(markdown: String): List<Block> = listOf(Block.Paragraph(markdown))
}

@Composable
fun MarkdownRenderer(blocks: List<Block>) {
    Column {
        blocks.forEach { b ->
            when (b) {
                is Block.Code -> Text(b.text, modifier = Modifier.horizontalScroll(rememberScrollState()))
                is Block.Paragraph -> Text(b.text)
                is Block.ListBlock -> b.items.forEach { Text("• $it") }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, lang: String?) {
    // Highlight as paint-only; failure falls back to plain; no main-thread stall
    val highlighted: AnnotatedString = remember(code, lang) { AnnotatedString(code) }
    Text(highlighted)
}
