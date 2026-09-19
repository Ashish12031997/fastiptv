package com.fastiptv.domain.model

data class ServerConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val protocol: String = "http"
) {
    val baseUrl: String
        get() = "$protocol://$host:$port"

    val isValid: Boolean
        get() = host.isNotBlank() && port > 0 && username.isNotBlank() && password.isNotBlank()
}
