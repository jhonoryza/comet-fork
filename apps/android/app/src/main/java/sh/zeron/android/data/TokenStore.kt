package sh.zeron.android.data

interface TokenStore {
    suspend fun save(access: String, refresh: String)
    suspend fun load(): Pair<String, String>?
    suspend fun clear()
}

class InMemoryTokenStore : TokenStore {
    @Volatile private var pair: Pair<String, String>? = null
    override suspend fun save(access: String, refresh: String) { pair = access to refresh }
    override suspend fun load(): Pair<String, String>? = pair
    override suspend fun clear() { pair = null }
}
