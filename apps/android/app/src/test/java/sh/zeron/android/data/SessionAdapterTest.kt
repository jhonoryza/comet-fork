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

    @Test fun streamingEntryMarksTranscriptWorking() = runTest {
        val json = """
            {"messages":[
              {"id":"m1","role":"user","status":"complete","parts":[{"id":"p1","kind":"text","text":"hi"}]},
              {"id":"m2","role":"assistant","status":"streaming","parts":[{"id":"p2","kind":"text","text":"th"}]}
            ]}
        """.trimIndent()
        val ts = doc(json).transcript()
        assertTrue(ts.working)
        assertEquals(MessageStatus.Complete, ts.messages[0].status)
        assertEquals(MessageStatus.Streaming, ts.messages[1].status)
    }

    @Test fun finishedRunIsNotWorking() = runTest {
        val json = """
            {"messages":[{"id":"m1","status":"complete","parts":[{"id":"p1","kind":"text","text":"done"}]}]}
        """.trimIndent()
        assertFalse(doc(json).transcript().working)
    }

    /** A just-opened entry has no parts yet, so it never reaches `messages`. */
    @Test fun partlessStreamingEntryStillWorks() = runTest {
        val json = """
            {"messages":[
              {"id":"m1","status":"complete","parts":[{"id":"p1","kind":"text","text":"hi"}]},
              {"id":"m2","status":"streaming","parts":[]}
            ]}
        """.trimIndent()
        val ts = doc(json).transcript()
        assertEquals(1, ts.messages.size)
        assertTrue(ts.working)
    }

    /** An older stalled `streaming` entry must not pin the spinner on. */
    @Test fun onlyTheLastEntryDecidesWorking() = runTest {
        val json = """
            {"messages":[
              {"id":"m1","status":"streaming","parts":[{"id":"p1","kind":"text","text":"a"}]},
              {"id":"m2","status":"complete","parts":[{"id":"p2","kind":"text","text":"b"}]}
            ]}
        """.trimIndent()
        assertFalse(doc(json).transcript().working)
    }

    @Test fun missingStatusIsNotWorking() = runTest {
        val json = """{"messages":[{"id":"m1","parts":[{"id":"p1","kind":"text","text":"a"}]}]}"""
        val ts = doc(json).transcript()
        assertNull(ts.messages[0].status)
        assertFalse(ts.working)
    }

    @Test fun malformedJsonYieldsEmpty() = runTest {
        // FakeLoroDoc returns raw json; bad JSON → empty, not crash
        val ts = SessionAdapter(FakeLoroDoc("{ not json }")).transcript()
        assertTrue(ts.parts.isEmpty())
    }
}