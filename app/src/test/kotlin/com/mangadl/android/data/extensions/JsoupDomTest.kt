package com.mangadl.android.data.extensions

import org.jsoup.Jsoup
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the Jsoup-backed DOM helpers used as QuickJS bindings.
 * Mirrors __jsoupSelectAll and __jsoupSelectOne in ExtensionManager.
 */
class JsoupDomTest {

    private fun serializeElement(el: org.jsoup.nodes.Element): JSONObject {
        val attrsObj = JSONObject()
        for (attr in el.attributes()) attrsObj.put(attr.key, attr.value)
        return JSONObject().apply {
            put("tag", el.tagName())
            put("text", el.text())
            put("innerHtml", el.html())
            put("outerHtml", el.outerHtml())
            put("attrs", attrsObj)
        }
    }

    private fun jsoupSelectAll(html: String, selector: String): String {
        return try {
            val doc = Jsoup.parse(html)
            val elements = doc.select(selector)
            val arr = JSONArray()
            for (el in elements) arr.put(serializeElement(el))
            arr.toString()
        } catch (e: Exception) { "[]" }
    }

    private fun jsoupSelectOne(html: String, selector: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            val el = doc.selectFirst(selector) ?: return null
            serializeElement(el).toString()
        } catch (e: Exception) { null }
    }

    @Test
    fun `selectAll returns correct count`() {
        val html = "<ul><li>A</li><li>B</li><li>C</li></ul>"
        val result = JSONArray(jsoupSelectAll(html, "li"))
        assertEquals(3, result.length())
    }

    @Test
    fun `selectAll returns correct text content`() {
        val html = "<div class='item'><h3>Manga Title</h3></div>"
        val result = JSONArray(jsoupSelectAll(html, ".item h3"))
        assertEquals(1, result.length())
        assertEquals("Manga Title", result.getJSONObject(0).getString("text"))
    }

    @Test
    fun `selectAll returns attrs correctly`() {
        val html = "<a href='/manga/one-piece' class='title'>One Piece</a>"
        val result = JSONArray(jsoupSelectAll(html, "a"))
        val attrs = result.getJSONObject(0).getJSONObject("attrs")
        assertEquals("/manga/one-piece", attrs.getString("href"))
        assertEquals("title", attrs.getString("class"))
    }

    @Test
    fun `selectAll on missing selector returns empty array`() {
        val html = "<div>hello</div>"
        val result = JSONArray(jsoupSelectAll(html, ".nonexistent"))
        assertEquals(0, result.length())
    }

    @Test
    fun `selectOne returns first match`() {
        val html = "<ul><li class='first'>A</li><li>B</li></ul>"
        val result = jsoupSelectOne(html, "li")
        assertNotNull(result)
        assertEquals("A", JSONObject(result!!).getString("text"))
    }

    @Test
    fun `selectOne returns null when no match`() {
        val html = "<div>no match</div>"
        assertNull(jsoupSelectOne(html, ".missing"))
    }

    @Test
    fun `selectOne tag field is lowercase`() {
        val html = "<DIV id='main'>content</DIV>"
        val result = JSONObject(jsoupSelectOne(html, "#main")!!)
        assertEquals("div", result.getString("tag"))
    }

    @Test
    fun `innerHtml preserves nested markup`() {
        val html = "<div><span>inner</span></div>"
        val result = JSONObject(jsoupSelectOne(html, "div")!!)
        assertTrue(result.getString("innerHtml").contains("<span>"))
    }

    @Test
    fun `element with img src attr`() {
        val html = "<img src='https://example.com/cover.jpg' data-src='lazy.jpg'>"
        val result = JSONArray(jsoupSelectAll(html, "img"))
        val attrs = result.getJSONObject(0).getJSONObject("attrs")
        assertEquals("https://example.com/cover.jpg", attrs.getString("src"))
        assertEquals("lazy.jpg", attrs.getString("data-src"))
    }

    @Test
    fun `comma selector matches multiple types`() {
        val html = "<div class='a'>1</div><span class='b'>2</span>"
        val result = JSONArray(jsoupSelectAll(html, ".a, .b"))
        assertEquals(2, result.length())
    }

    @Test
    fun `invalid selector returns empty array without throwing`() {
        val html = "<div>content</div>"
        val result = jsoupSelectAll(html, "!!invalid!!")
        assertEquals("[]", result)
    }
}
