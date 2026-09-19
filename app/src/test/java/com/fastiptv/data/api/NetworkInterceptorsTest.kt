package com.fastiptv.data.api

import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.ServerConfig
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkInterceptorsTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        sessionManager = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testAuthInterceptorInjectsHeadersAndCredentials() {
        val testConfig = ServerConfig(
            host = "myserver.tv",
            port = 8080,
            username = "demo_user",
            password = "demo_password"
        )
        every { sessionManager.getCachedConfig() } returns testConfig

        val authInterceptor = AuthInterceptor(sessionManager)
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/player_api.php"))
            .build()

        val response = client.newCall(request).execute()
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(AuthInterceptor.USER_AGENT, recordedRequest.getHeader("User-Agent"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))

        val recordedUrl = recordedRequest.requestUrl!!
        assertEquals("demo_user", recordedUrl.queryParameter("username"))
        assertEquals("demo_password", recordedUrl.queryParameter("password"))
    }

    @Test
    fun testDynamicBaseUrlInterceptorRewritesHostAndPort() {
        val testConfig = ServerConfig(
            host = mockWebServer.hostName,
            port = mockWebServer.port,
            username = "user",
            password = "pass",
            protocol = "http"
        )
        every { sessionManager.getCachedConfig() } returns testConfig

        val dynamicBaseUrlInterceptor = DynamicBaseUrlInterceptor(sessionManager)
        val client = OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setBody("{\"status\":\"ok\"}"))

        // Call an arbitrary dummy URL; interceptor should rewrite it to mockWebServer's host & port
        val request = Request.Builder()
            .url("http://dummy-placeholder.com:9999/player_api.php?action=test")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string()
        response.close()

        assertEquals("{\"status\":\"ok\"}", body)
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/player_api.php?action=test", recordedRequest.path)
    }

    @Test
    fun testServerConfigValidity() {
        val validConfig = ServerConfig("host.com", 8080, "u", "p")
        assertTrue(validConfig.isValid)
        assertEquals("http://host.com:8080", validConfig.baseUrl)
    }
}
