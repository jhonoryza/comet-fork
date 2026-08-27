package sh.zeron.android.protocol

object Chat2Codec {
    const val TYPE_HELLO: Byte = 1
    const val TYPE_STATE: Byte = 2
    const val TYPE_ROW: Byte = 3
    const val TYPE_ROWS_DONE: Byte = 4
    const val TYPE_PUSH: Byte = 5
    const val TYPE_ACK: Byte = 6

    fun encodeHello(cursor: Long, device: String): ByteArray {
        val header = """{"cursor":$cursor,"device":"$device"}""".toByteArray()
        return byteArrayOf(TYPE_HELLO) + header.size.toLittleEndian() + header
    }
    fun decodeFrame(bytes: ByteArray): Pair<Byte, ByteArray>? {
        if (bytes.isEmpty()) return null
        return bytes[0] to bytes.copyOfRange(1, bytes.size)
    }
    private fun Int.toLittleEndian(): ByteArray = byteArrayOf(
        (this and 0xFF).toByte(), ((this shr 8) and 0xFF).toByte(),
        ((this shr 16) and 0xFF).toByte(), ((this shr 24) and 0xFF).toByte()
    )
}
