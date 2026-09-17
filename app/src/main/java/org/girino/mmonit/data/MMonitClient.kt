package org.girino.mmonit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.girino.mmonit.domain.InvalidMMonitPayloadException
import org.girino.mmonit.domain.MMonitStatusParser
import org.girino.mmonit.domain.MMonitStatusSnapshot
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class MMonitFailureReason {
    INVALID_CONFIGURATION,
    NETWORK,
    AUTHENTICATION,
    HTTP,
    INVALID_RESPONSE,
}

class MMonitException(
    val reason: MMonitFailureReason,
    override val message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this.cookies) {
            cookies.forEach { cookie ->
                this.cookies.removeAll { existing ->
                    existing.name == cookie.name &&
                        existing.domain == cookie.domain &&
                        existing.path == cookie.path
                }
                if (!cookie.expiresAt.let { it < System.currentTimeMillis() }) {
                    this.cookies.add(cookie)
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(cookies) {
        cookies.removeAll { it.expiresAt < System.currentTimeMillis() }
        cookies.filter { it.matches(url) }.toList()
    }

    fun hasSessionCookie(): Boolean = synchronized(cookies) {
        cookies.any { it.name == "zsessionid" && it.expiresAt >= System.currentTimeMillis() }
    }
}

class MMonitClient(
    private val cookieJar: SessionCookieJar = SessionCookieJar(),
    private val httpClient: OkHttpClient = buildHttpClient(cookieJar),
) {
    suspend fun poll(config: MMonitConfig): MMonitStatusSnapshot = withContext(Dispatchers.IO) {
        pollBlocking(config)
    }

    private fun pollBlocking(rawConfig: MMonitConfig): MMonitStatusSnapshot {
        val config = try {
            rawConfig.normalized()
        } catch (exception: IllegalArgumentException) {
            throw MMonitException(
                reason = MMonitFailureReason.INVALID_CONFIGURATION,
                message = exception.message ?: "Configuração inválida.",
                cause = exception,
            )
        }

        return try {
            val baseUrl = config.serverUrl.toHttpUrlOrNull()
                ?: throw MMonitException(
                    MMonitFailureReason.INVALID_CONFIGURATION,
                    "Informe uma URL válida.",
                )

            get(baseUrl, "/index.csp", "inicialização")

            val loginBody = FormBody.Builder()
                .add("z_username", config.username)
                .add("z_password", config.password)
                .add("z_csrf_protection", "off")
                .build()
            post(baseUrl, "/z_security_check", loginBody, "login")

            if (!cookieJar.hasSessionCookie()) {
                throw MMonitException(
                    reason = MMonitFailureReason.AUTHENTICATION,
                    message = "O login não criou uma sessão no M/Monit.",
                )
            }

            val payload = get(
                baseUrl,
                "/api/2/status/hosts/summary",
                "consulta de status",
            )
            MMonitStatusParser.parse(payload)
        } catch (exception: MMonitException) {
            throw exception
        } catch (exception: InvalidMMonitPayloadException) {
            throw MMonitException(
                reason = MMonitFailureReason.INVALID_RESPONSE,
                message = "O M/Monit retornou um JSON inesperado.",
                cause = exception,
            )
        } catch (exception: IOException) {
            throw MMonitException(
                reason = MMonitFailureReason.NETWORK,
                message = "Não foi possível acessar o M/Monit.",
                cause = exception,
            )
        }
    }

    private fun get(baseUrl: HttpUrl, path: String, operation: String): String {
        val request = Request.Builder()
            .url(endpoint(baseUrl, path))
            .header("Accept", "text/html,application/json")
            .get()
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                throw MMonitException(
                    reason = MMonitFailureReason.AUTHENTICATION,
                    message = "Falha de autenticação no M/Monit.",
                )
            }
            if (!response.isSuccessful) {
                throw MMonitException(
                    reason = MMonitFailureReason.HTTP,
                    message = "O M/Monit retornou HTTP ${response.code} durante a $operation.",
                )
            }
            response.body?.string()
                ?: throw MMonitException(
                    reason = MMonitFailureReason.INVALID_RESPONSE,
                    message = "O M/Monit retornou uma resposta vazia.",
                )
        }
    }

    private fun post(
        baseUrl: HttpUrl,
        path: String,
        body: FormBody,
        operation: String,
    ) {
        val request = Request.Builder()
            .url(endpoint(baseUrl, path))
            .header("Accept", "text/html,application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                throw MMonitException(
                    reason = MMonitFailureReason.AUTHENTICATION,
                    message = "Falha de autenticação no M/Monit.",
                )
            }
            if (!response.isSuccessful) {
                throw MMonitException(
                    reason = MMonitFailureReason.HTTP,
                    message = "O M/Monit retornou HTTP ${response.code} durante o $operation.",
                )
            }
        }
    }

    private fun endpoint(baseUrl: HttpUrl, path: String): HttpUrl {
        val prefix = baseUrl.encodedPath.trimEnd('/')
        val endpointPath = if (prefix.isEmpty()) path else prefix + path
        return baseUrl.newBuilder()
            .encodedPath(endpointPath)
            .query(null)
            .fragment(null)
            .build()
    }

    companion object {
        private fun buildHttpClient(cookieJar: CookieJar): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
