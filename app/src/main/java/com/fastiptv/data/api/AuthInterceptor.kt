package com.fastiptv.data.api

import com.fastiptv.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    companion object {
        const val USER_AGENT = "IPTVSmartersPro/1.0.0 (Linux;Android 11) ExoPlayerLib/2.18.2"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val config = sessionManager.getCachedConfig()

        val isApiRequest = originalUrl.encodedPath.contains("player_api.php")
        val requestBuilder = originalRequest.newBuilder()
            .header("User-Agent", USER_AGENT)

        if (isApiRequest) {
            val urlBuilder = originalUrl.newBuilder()
            if (config != null && config.isValid) {
                if (originalUrl.queryParameter("username") == null) {
                    urlBuilder.addQueryParameter("username", config.username)
                }
                if (originalUrl.queryParameter("password") == null) {
                    urlBuilder.addQueryParameter("password", config.password)
                }
            }
            requestBuilder
                .url(urlBuilder.build())
                .header("Accept", "application/json")
        }

        return chain.proceed(requestBuilder.build())
    }
}
