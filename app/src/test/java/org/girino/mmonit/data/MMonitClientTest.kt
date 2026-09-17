package org.girino.mmonit.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.girino.mmonit.domain.MMonitLevel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MMonitClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `logs in with default fields and reads summary endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>login</html>"))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "zsessionid=session-value; Path=/")
                .setBody("<html>authenticated</html>"),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"status\":[{\"label\":\"ok\",\"data\":3}] }"),
        )

        val config = MMonitConfig(
            serverUrl = server.url("/").toString().removeSuffix("/"),
            username = "operator",
            password = "secret-for-test",
        )
        val status = MMonitClient().poll(config)

        assertEquals(MMonitLevel.GREEN, status.level)
        val initRequest = server.takeRequest()
        assertEquals("GET", initRequest.method)
        assertEquals("/index.csp", initRequest.path)
        val loginRequest = server.takeRequest()
        val loginBody = loginRequest.body.readUtf8()
        assertEquals("POST", loginRequest.method)
        assertTrue(loginBody.contains("z_username=operator"))
        assertTrue(loginBody.contains("z_csrf_protection=off"))
        assertEquals("/api/2/status/hosts/summary", server.takeRequest().path)
    }

    @Test
    fun `missing session cookie is treated as authentication failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>login</html>"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>login failed</html>"))

        val config = MMonitConfig(
            serverUrl = server.url("/").toString().removeSuffix("/"),
            username = "operator",
            password = "secret-for-test",
        )

        val exception = try {
            MMonitClient().poll(config)
            error("Expected authentication failure")
        } catch (expected: MMonitException) {
            expected
        }

        assertEquals(MMonitFailureReason.AUTHENTICATION, exception.reason)
    }
}
