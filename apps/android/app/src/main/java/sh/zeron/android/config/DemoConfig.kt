package sh.zeron.android.config

/**
 * Dev/demo configuration for a debug APK. Replace the values here with a
 * production edge URL before release. Dev mode uses the `user@org` bearer
 * convention (AUTH_MODE=dev edge); it must never ship in a release build.
 */
object DemoConfig {
    // Point this at a local edge (e.g. `wrangler dev`) for dev mode, or the
    // production edge in WorkOS mode.
    val edgeBaseUrl: String = "http://10.0.2.2:8787" // Android emulator → host localhost
    val authMode: AuthMode = AuthMode.Dev
    val devUserId: String = "android-test"
    val devOrgId: String = "org-1"

    fun appConfig(deviceId: String) = AppConfig(
        edgeBaseUrl = edgeBaseUrl,
        authMode = authMode,
        deviceId = deviceId,
    )

    fun registryWSUrl(orgId: String): String = edgeBaseUrl.trimEnd('/') + "/registry/" + orgId + "/ws"

    fun devBearer(): String = "$devUserId@$devOrgId"
}