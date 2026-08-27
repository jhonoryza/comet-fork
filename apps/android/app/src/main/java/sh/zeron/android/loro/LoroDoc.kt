package sh.zeron.android.loro

interface LoroDoc : AutoCloseable {
    suspend fun importBytes(bytes: ByteArray)
    suspend fun exportSnapshot(): ByteArray
    suspend fun getDeepValueJson(): String
    suspend fun closeDoc()
}
