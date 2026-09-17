package org.girino.mmonit.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class MMonitConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    fun normalized(): MMonitConfig {
        val url = serverUrl.trim().removeSuffix("/")
        val parsed = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Informe uma URL válida.")

        require(parsed.scheme == "http" || parsed.scheme == "https") {
            "A URL deve usar HTTP ou HTTPS."
        }
        require(parsed.host.isNotBlank()) { "A URL precisa conter um host." }
        require(parsed.username.isEmpty() && parsed.password.isEmpty()) {
            "Informe usuário e senha nos campos do aplicativo."
        }
        require(username.isNotBlank()) { "Informe o usuário do M/Monit." }
        require(password.isNotEmpty()) { "Informe a senha do M/Monit." }

        return copy(
            serverUrl = parsed.toString().removeSuffix("/"),
            username = username.trim(),
        )
    }
}
