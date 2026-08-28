package sh.zeron.android.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RegistryDocTest {
    private fun row(kind: String, id: String, fields: Map<String, Any>) = RegistryRow.parse(
        JSONObject().put("kind", kind).put("id", id).put("deleted", false)
            .put("fields", JSONObject(fields))
    )

    @Test fun fullStateReplacesRows() {
        val doc = RegistryDoc()
        doc.applyState(true, listOf(row("chats", "c1", mapOf("title" to "A", "archived" to false))), 1)
        doc.applyState(true, listOf(row("chats", "c2", mapOf("title" to "B", "archived" to false))), 2)
        assertEquals(listOf("c2"), doc.overlayRows("chats").map { it.id })
    }

    @Test fun adapterProjectsChatsAndFiltersDeleted() {
        val doc = RegistryDoc()
        doc.applyState(true, listOf(
            row("chats", "c1", mapOf("title" to "Hi", "archived" to false)),
            RegistryRow.parse(JSONObject().put("kind", "chats").put("id", "c2").put("deleted", true)
                .put("fields", JSONObject(mapOf("title" to "gone")))),
        ), 1)
        val chats = RegistryAdapter(doc).chats()
        assertEquals(1, chats.size)
        assertEquals("Hi", chats[0].title)
        assertFalse(chats[0].archived)
    }
}