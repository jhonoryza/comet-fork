package sh.zeron.android.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import sh.zeron.android.loro.FakeLoroDoc

class SessionAdapterTest {
    private fun doc(json: String) = SessionAdapter(FakeLoroDoc(json))

    @Test fun emptyDocNoParts() = runTest {
        assertEquals(0, doc("{}").transcript().parts.size)
    }

    @Test fun parsesTextAndToolParts() = runTest {
        val json = """
            {"messages":[{"id":"m1","parts":[
              {"id":"p1","kind":"text","text":"hello"},
              {"id":"p2","kind":"tool","call":{"name":"bash"},"isError":false}
            ]}]}
        """.trimIndent()
        val ts = doc(json).transcript()
        assertEquals(2, ts.parts.size)
        assertTrue(ts.parts[0] is Part.Text)
        assertEquals("hello", (ts.parts[0] as Part.Text).text)
        assertTrue(ts.parts[1] is Part.Tool)
    }

    @Test fun errorPart() = runTest {
        val json = """{"messages":[{"id":"m1","parts":[{"id":"e1","kind":"error","message":"boom"}]}]}"""
        val ts = doc(json).transcript()
        assertEquals("boom", (ts.parts[0] as Part.Error).message)
    }

    @Test fun malformedJsonYieldsEmpty() = runTest {
        // FakeLoroDoc returns raw json; bad JSON → empty, not crash
        val ts = SessionAdapter(FakeLoroDoc("{ not json }")).transcript()
        assertTrue(ts.parts.isEmpty())
    }
}