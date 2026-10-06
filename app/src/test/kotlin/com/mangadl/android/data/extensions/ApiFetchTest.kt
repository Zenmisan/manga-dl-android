package com.mangadl.android.data.extensions

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for ExtensionManager.apiFetch — tests the actual production ExtensionManager
 * against HTTP proxy requests and JSON responses.
 */
class ApiFetchTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var manager: ExtensionManager

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
        val mockContext = mockk<Context>(relaxed = true)
        manager = ExtensionManager(mockContext, client)
        manager.backendUrl = server.url("/").toString().trimEnd('/')
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun proxyHtmlResponseReturnsMapWithHtmlKey() = runTest {
        val targetUrl = server.url("/page").toString()
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody("<html>hello</html>"))

        val proxyUrl = "/manga/proxy/html?url=" + java.net.URLEncoder.encode(targetUrl, "UTF-8")
        val result = manager.apiFetch(proxyUrl) as? Map<*, *>

        assertNotNull(result)
        assertEquals("<html>hello</html>", result!!["html"])
        assertEquals(targetUrl, result["url"])
    }

    @Test
    fun proxyJsonMangadexStyleReturnsMapWithDataKey() = runTest {
        val targetUrl = server.url("/data").toString()
        val json = """{"result":"ok","data":[{"id":"123","type":"manga"}],"total":1}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val proxyUrl = "/manga/proxy/json?url=" + java.net.URLEncoder.encode(targetUrl, "UTF-8")
        val result = manager.apiFetch(proxyUrl) as? Map<*, *>

        assertNotNull(result)
        assertEquals("ok", result!!["result"])
        val data = result["data"] as? List<*>
        assertNotNull(data)
        assertEquals(1, data!!.size)
        assertEquals("123", (data[0] as? Map<*, *>)?.get("id"))
    }

    @Test
    fun http404ResponseReturnsNull() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        val result = manager.apiFetch("/missing")
        assertNull(result)
    }

    @Test
    fun http500ResponseReturnsNull() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = manager.apiFetch("/error")
        assertNull(result)
    }

    @Test
    fun relativeUrlResolvesAgainstBackendUrl() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        manager.apiFetch("/api/test")
        val req = server.takeRequest()
        assertEquals("/api/test", req.path)
    }

    @Test
    fun absoluteUrlUsedDirectly() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        val absUrl = server.url("/direct/path").toString()
        val result = manager.apiFetch(absUrl) as? Map<*, *>
        assertNotNull(result)
        val req = server.takeRequest()
        assertEquals("/direct/path", req.path)
    }

    @Test
    fun jsonArrayResponseReturnsList() = runTest {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody("""[{"id":"1"},{"id":"2"}]"""))
        val result = manager.apiFetch("/list") as? List<*>

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertEquals("1", (result[0] as? Map<*, *>)?.get("id"))
    }
}
