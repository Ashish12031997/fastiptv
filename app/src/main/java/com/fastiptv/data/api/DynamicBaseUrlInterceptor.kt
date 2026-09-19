package com.fastiptv.data.api

import com.fastiptv.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val config = sessionManager.getCachedConfig()

        val originalHost = request.url.host
        val isApiRequest = request.url.encodedPath.contains("player_api.php")
        val isLocalhost = originalHost == "localhost" || originalHost == "127.0.0.1"

        if (config != null && config.isValid && (isApiRequest || isLocalhost)) {
            val newUrl = request.url.newBuilder()
                .scheme(config.protocol)
                .host(config.host)
                .port(config.port)
                .build()

            request = request.newBuilder()
                .url(newUrl)
                .build()
        }

        return chain.proceed(request)
    }
}
