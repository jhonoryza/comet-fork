package sh.zeron.android.data

import java.util.UUID

interface DeviceIdStore {
    suspend fun getOrCreate(): String
    suspend fun reset()
}

class InMemoryDeviceIdStore : DeviceIdStore {
    @Volatile private var id: String? = null
    private val lock = Any()
    override suspend fun getOrCreate(): String = synchronized(lock) {
        id ?: UUID.randomUUID().toString().lowercase().also { id = it }
    }
    override suspend fun reset() { synchronized(lock) { id = null } }
}
