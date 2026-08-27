package sh.zeron.android.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.zeron.android.data.TokenStore

class AuthStateMachine(
    private val client: AuthClient,
    private val tokens: TokenStore,
) {
    private val refreshMutex = Mutex()
    var selectedOrgId: String? = null

    suspend fun selectOrgAndRefresh(orgId: String): AuthTokens = refreshMutex.withLock {
        val pair = tokens.load() ?: error("no refresh token")
        val scoped = client.refresh(pair.second, orgId)
        tokens.save(scoped.accessToken, scoped.refreshToken)
        selectedOrgId = orgId
        scoped
    }

    /** Dev mode (AUTH_MODE=dev edge): bearer IS the identity, no exchange. */
    suspend fun signInDev(userId: String, orgId: String): AuthTokens = refreshMutex.withLock {
        val bearer = "$userId@$orgId"
        val t = AuthTokens(bearer, bearer)
        tokens.save(t.accessToken, t.refreshToken)
        selectedOrgId = orgId
        t
    }

    suspend fun signOut() = refreshMutex.withLock {
        tokens.clear()
        selectedOrgId = null
    }

    suspend fun refreshSerialized(): AuthTokens = refreshMutex.withLock {
        val pair = tokens.load() ?: error("no refresh token")
        val next = client.refresh(pair.second, selectedOrgId)
        tokens.save(next.accessToken, next.refreshToken)
        next
    }
}
