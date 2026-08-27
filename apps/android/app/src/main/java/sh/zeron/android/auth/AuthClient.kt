package sh.zeron.android.auth

import sh.zeron.android.config.AppConfig
import sh.zeron.android.core.AppError

data class AuthUser(val id: String, val email: String?)
data class AuthOrg(val id: String, val organizationId: String, val name: String)
data class AuthTokens(val accessToken: String, val refreshToken: String)

interface AuthClient {
    suspend fun exchange(code: String): Pair<AuthUser, AuthTokens>
    suspend fun refresh(refreshToken: String, organizationId: String? = null): AuthTokens
    suspend fun orgs(accessToken: String): List<AuthOrg>
}

class HttpAuthClient(private val config: AppConfig) : AuthClient {
    override suspend fun exchange(code: String): Pair<AuthUser, AuthTokens> {
        // POST ${edgeBaseUrl}/auth/exchange {code} — never log code/tokens
        throw AppError.Retryable
    }
    override suspend fun refresh(refreshToken: String, organizationId: String?): AuthTokens {
        // POST ${edgeBaseUrl}/auth/refresh {refreshToken, organizationId?}
        throw AppError.Retryable
    }
    override suspend fun orgs(accessToken: String): List<AuthOrg> {
        // GET ${edgeBaseUrl}/auth/orgs Bearer accessToken
        return emptyList()
    }
}

class FakeAuthClient : AuthClient {
    var tokens = AuthTokens("fake-access", "fake-refresh")
    var orgs = listOf(AuthOrg("1", "org-1", "Org One"))
    override suspend fun exchange(code: String) = AuthUser("u1", "u@ex.com") to tokens
    override suspend fun refresh(refreshToken: String, organizationId: String?) = tokens
    override suspend fun orgs(accessToken: String) = orgs
}
