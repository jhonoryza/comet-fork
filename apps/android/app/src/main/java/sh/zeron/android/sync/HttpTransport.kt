package sh.zeron.android.sync

interface HttpTransport {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
    suspend fun post(url: String, body: ByteArray, headers: Map<String, String> = emptyMap()): HttpResponse
}

data class HttpResponse(val code: Int, val body: ByteArray, val headers: Map<String, String> = emptyMap())

class FakeHttpTransport(var handler: suspend (String) -> HttpResponse = { HttpResponse(200, ByteArray(0)) }) : HttpTransport {
    override suspend fun get(url: String, headers: Map<String, String>) = handler(url)
    override suspend fun post(url: String, body: ByteArray, headers: Map<String, String>) = handler(url)
}
