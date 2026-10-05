package com.mangadl.android.data.extensions

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for apiFetch — mirrors web frontend behaviour: returns parsed JSON body directly.
 * Backend proxy/html → {"html":"..."}, proxy/json → raw JSON from external API.
 */
class ApiFetchTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun resolveUrl(backendUrl: String, url: String) =
        if (url.startsWith("/")) backendUrl.trimEnd('/') + url else url

    // Replicate the new apiFetch logic: parse JSON, return Map/List
    private fun apiFetchSync(backendUrl: String, url: String): Any? {
        val resolved = resolveUrl(backendUrl, url)
        return try {
            val req = okhttp3.Request.Builder().url(resolved).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            jsonToKotlin(body)
        } catch (e: Exception) {
            null
        }
    }

    private fun jsonToKotlin(raw: Any?): Any? = when (raw) {
        is String -> {
            val t = raw.trim()
            when {
                t.startsWith("{") -> try { jsonToKotlin(JSONObject(t)) } catch (_: Exception) { raw }
                t.startsWith("[") -> try { jsonToKotlin(JSONArray(t)) } catch (_: Exception) { raw }
                else -> raw
            }
        }
        is JSONObject -> {
            val map = mutableMapOf<String, Any?>()
            raw.keys().forEach { k -> map[k] = jsonToKotlin(raw.get(k)) }
            map
        }
        is JSONArray -> {
            (0 until raw.length()).map { jsonToKotlin(raw.get(it)) }
        }
        JSONObject.NULL -> null
        else -> raw
    }

    @Test
    fun `proxy-html response returns map with html key`() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody("""{"html":"<html>hello</html>","url":"http://example.com"}"""))
        val base = server.url("/").toString().trimEnd('/')
        val result = apiFetchSync(base, "/manga/proxy/html?url=foo") as? Map<*, *>

        assertNotNull(result)
        assertEquals("<html>hello</html>", result!!["html"])
        assertEquals("http://example.com", result["url"])
    }

    @Test
    fun `proxy-json mangadex style returns map with data key`() {
        val json = """{"result":"ok","data":[{"id":"123","type":"manga"}],"total":1}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))
        val base = server.url("/").toString().trimEnd('/')
        val result = apiFetchSync(base, "/manga/proxy/json?url=foo") as? Map<*, *>

        assertNotNull(result)
        assertEquals("ok", result!!["result"])
        val data = result["data"] as? List<*>
        assertNotNull(data)
        assertEquals(1, data!!.size)
        assertEquals("123", (data[0] as? Map<*, *>)?.get("id"))
    }

    @Test
    fun `404 response returns null`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        val base = server.url("/").toString().trimEnd('/')
        val result = apiFetchSync(base, "/missing")
        assertNull(result)
    }

    @Test
    fun `500 response returns null`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val base = server.url("/").toString().trimEnd('/')
        val result = apiFetchSync(base, "/error")
        assertNull(result)
    }

    @Test
    fun `relative url resolves against backendUrl`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        val base = server.url("/").toString().trimEnd('/')
        apiFetchSync(base, "/api/test")
        val req = server.takeRequest()
        assertEquals("/api/test", req.path)
    }

    @Test
    fun `absolute url used directly without modification`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        val base = server.url("/").toString().trimEnd('/')
        val absUrl = "$base/direct/path"
        val result = apiFetchSync(base, absUrl) as? Map<*, *>
        assertNotNull(result)
        val req = server.takeRequest()
        assertEquals("/direct/path", req.path)
    }

    @Test
    fun `network failure returns null`() {
        server.shutdown()
        val result = apiFetchSync("http://127.0.0.1:1", "/api/test")
        assertNull(result)
    }

    @Test
    fun `json array response returns list`() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody("""[{"id":"1"},{"id":"2"}]"""))
        val base = server.url("/").toString().trimEnd('/')
        val result = apiFetchSync(base, "/list") as? List<*>

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertEquals("1", (result[0] as? Map<*, *>)?.get("id"))
    }
}
