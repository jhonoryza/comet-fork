package sh.zeron.android.config

/**
 * Edge connection defaults. Mirrors iOS `AppModel.edgeURLString`
 * (apps/ios/Zeron/App/AppModel.swift:32) — production edge + WorkOS mode.
 * Dev mode (`user@org` bearer against an AUTH_MODE=dev edge) stays available
 * for debug builds but is never the default.
 */
object EdgeConfig {
    /** Same default as iOS. */
    const val PRODUCTION_EDGE = "https://edge.zeron.sh"

    val edgeBaseUrl: String = PRODUCTION_EDGE
    val authMode: AuthMode = AuthMode.WorkOS

    fun appConfig(deviceId: String) = AppConfig(
        edgeBaseUrl = edgeBaseUrl,
        authMode = authMode,
        deviceId = deviceId,
    )

    /** http→ws, https→wss (iOS AppConfig.wsBase). */
    private fun wsBase(base: String): String = when {
        base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
        base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
        else -> base
    }.trimEnd('/')

    /**
     * Registry room URL with the token+device query the DO requires
     * (iOS AppConfig.registrySocketURL).
     */
    fun registryWSUrl(orgId: String, token: String, deviceId: String): String =
        "${wsBase(edgeBaseUrl)}/registry/$orgId/ws?token=$token&device=$deviceId"

    /** chat2 room URL (iOS AppConfig.chat2SocketURL). */
    fun chat2WSUrl(chatId: String, token: String, deviceId: String): String =
        "${wsBase(edgeBaseUrl)}/chat2/$chatId/ws?token=$token&device=$deviceId"
}