package com.mangadl.android.data.extensions

import android.content.Context
import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap

private val DOM_SHIM = """
(function() {
  function mkElement(d) {
    if (!d) return null;
    var el = {
      tagName: (d.tag || '').toUpperCase(),
      nodeName: (d.tag || '').toUpperCase(),
      textContent: d.text || '',
      innerHTML: d.innerHtml || '',
      outerHTML: d.outerHtml || '',
      _html: d.innerHtml || '',
      _attrs: d.attrs || {},
      getAttribute: function(n) {
        var v = el._attrs[n];
        return v !== undefined ? v : null;
      },
      hasAttribute: function(n) { return el._attrs[n] !== undefined; },
      querySelectorAll: function(sel) {
        var json = __jsoupSelectAll(el._html, sel);
        var arr = JSON.parse(json || '[]');
        var result = arr.map(mkElement);
        result.forEach = Array.prototype.forEach.bind(result);
        return result;
      },
      querySelector: function(sel) {
        var json = __jsoupSelectOne(el._html, sel);
        if (!json) return null;
        try { return mkElement(JSON.parse(json)); } catch(e) { return null; }
      },
    };
    return el;
  }

  function Document(html) { this._html = html; }
  Document.prototype.querySelectorAll = function(sel) {
    var json = __jsoupSelectAll(this._html, sel);
    var arr = JSON.parse(json || '[]');
    var result = arr.map(mkElement);
    result.forEach = Array.prototype.forEach.bind(result);
    return result;
  };
  Document.prototype.querySelector = function(sel) {
    var json = __jsoupSelectOne(this._html, sel);
    if (!json) return null;
    try { return mkElement(JSON.parse(json)); } catch(e) { return null; }
  };

  globalThis.DOMParser = function() {};
  globalThis.DOMParser.prototype.parseFromString = function(html) {
    return new Document(html);
  };
})();
""".trimIndent()

class ExtensionManager(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    var backendUrl: String = ""

    private val extensions = mutableMapOf<String, ExtensionMeta>()

    private data class JsContext(val js: QuickJs, val mutex: Mutex)
    private val jsContexts = ConcurrentHashMap<String, JsContext>()

    fun loadAll() {
        val assetFiles = context.assets.list("extensions") ?: return
        for (file in assetFiles) {
            if (!file.endsWith(".js")) continue
            val id = file.removeSuffix(".js")
            try {
                val script = context.assets.open("extensions/$file")
                    .bufferedReader().readText()
                val meta = parseMetaComment(id, script)
                extensions[id] = ExtensionMeta(id, file, script, meta)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load extension: $file", e)
            }
        }
        Log.d(TAG, "Extensions ready. Loaded: ${extensions.size} sources")
    }

    fun listExtensions(): List<ExtensionMeta> = extensions.values.toList()

    fun getExtension(id: String): ExtensionMeta? = extensions[id]

    suspend fun search(extensionId: String, query: String, page: Int = 1): List<MangaSearchResult> {
        val result = evalWithContext(extensionId) { js ->
            js.evaluate<String>(
                "(async () => { const r = await __ext.search(${jsString(query)}, $page); return JSON.stringify(r); })()"
            )
        }
        return parseSearchResults(result)
    }

    suspend fun getPopular(extensionId: String, page: Int = 1): List<MangaSearchResult> {
        val result = evalWithContext(extensionId) { js ->
            js.evaluate<String>(
                "(async () => { " +
                "  if (typeof __ext.getPopular === 'function') { const r = await __ext.getPopular($page); return JSON.stringify(r); } " +
                "  const r = await __ext.search('', $page); return JSON.stringify(r); " +
                "})()"
            )
        }
        return parseSearchResults(result)
    }

    suspend fun getLatest(extensionId: String, page: Int = 1): List<MangaSearchResult> {
        val result = evalWithContext(extensionId) { js ->
            js.evaluate<String>(
                "(async () => { " +
                "  if (typeof __ext.getLatest === 'function') { const r = await __ext.getLatest($page); return JSON.stringify(r); } " +
                "  if (typeof __ext.getPopular === 'function') { const r = await __ext.getPopular($page); return JSON.stringify(r); } " +
                "  const r = await __ext.search('', $page); return JSON.stringify(r); " +
                "})()"
            )
        }
        return parseSearchResults(result)
    }

    suspend fun getMangaDetail(extensionId: String, mangaId: String): MangaDetail {
        val result = evalWithContext(extensionId) { js ->
            js.evaluate<String>(
                "(async () => { const r = await __ext.getMangaDetail(${jsString(mangaId)}); return JSON.stringify(r); })()"
            )
        }
        return parseMangaDetail(result)
    }

    suspend fun getPages(extensionId: String, chapterId: String): List<String> {
        val result = evalWithContext(extensionId) { js ->
            js.evaluate<String>(
                "(async () => { const r = await __ext.getPages(${jsString(chapterId)}); return JSON.stringify(r); })()"
            )
        }
        return parsePages(result)
    }

    suspend fun getChapterText(extensionId: String, chapterId: String): String {
        val result = evalWithContext(extensionId) { js ->
            js.evaluate<String>(
                "(async () => { " +
                "  if (typeof __ext.getChapterText !== 'function') return JSON.stringify({content:'',format:'plain'});" +
                "  const r = await __ext.getChapterText(${jsString(chapterId)}); return JSON.stringify(r); " +
                "})()"
            )
        }
        return try {
            val obj = org.json.JSONObject(result ?: "{}")
            obj.optString("content", "")
        } catch (_: Exception) { result ?: "" }
    }

    private suspend fun <T> evalWithContext(id: String, block: suspend (QuickJs) -> T): T {
        val ctx = jsContexts.getOrPut(id) {
            val script = extensions[id]?.script ?: error("Unknown extension: $id")
            val js = QuickJs.create(Dispatchers.IO)
            withContext(Dispatchers.IO) {
                js.define("console") {
                    asyncFunction("log") { args: Array<Any?> -> Log.d("JS[$id]", args.joinToString(" ")) }
                    asyncFunction("error") { args: Array<Any?> -> Log.e("JS[$id]", args.joinToString(" ")) }
                    asyncFunction("warn") { args: Array<Any?> -> Log.w("JS[$id]", args.joinToString(" ")) }
                }
                js.asyncFunction("apiFetch") { args: Array<Any?> ->
                    val url = args.getOrNull(0) as? String ?: return@asyncFunction null
                    apiFetch(url, args.getOrNull(1))
                }
                // Synchronous Jsoup bindings for DOM operations
                js.function("__jsoupSelectAll") { args: Array<Any?> ->
                    val html = args.getOrNull(0) as? String ?: return@function "[]"
                    val selector = args.getOrNull(1) as? String ?: return@function "[]"
                    try {
                        val doc = Jsoup.parse(html)
                        val elements = doc.select(selector)
                        val arr = JSONArray()
                        for (el in elements) arr.put(serializeElement(el))
                        arr.toString()
                    } catch (e: Exception) {
                        Log.e(TAG, "jsoupSelectAll error: selector=$selector", e)
                        "[]"
                    }
                }
                js.function("__jsoupSelectOne") { args: Array<Any?> ->
                    val html = args.getOrNull(0) as? String ?: return@function null
                    val selector = args.getOrNull(1) as? String ?: return@function null
                    try {
                        val doc = Jsoup.parse(html)
                        val el = doc.selectFirst(selector) ?: return@function null
                        serializeElement(el).toString()
                    } catch (e: Exception) {
                        Log.e(TAG, "jsoupSelectOne error: selector=$selector", e)
                        null
                    }
                }
                // Inject DOM shim, then extension script
                js.evaluate<Any?>(DOM_SHIM)
                js.evaluate<Any?>("const __ext = (() => { $script; return extension; })();")
            }
            JsContext(js, Mutex())
        }
        return ctx.mutex.withLock {
            withContext(Dispatchers.IO) {
                block(ctx.js)
            }
        }
    }

    fun closeAll() {
        jsContexts.values.forEach { it.js.close() }
        jsContexts.clear()
    }

    // Direct HTTP — no Render backend. Proxy patterns are resolved locally:
    //   /manga/proxy/html?url=X → fetch X, return {html, url}
    //   /manga/proxy/json?url=X → fetch X, return parsed JSON
    private suspend fun apiFetch(url: String, options: Any?): Any? {
        return withContext(Dispatchers.IO) {
            val proxyHtml = url.startsWith("/manga/proxy/html")
            val proxyJson = url.startsWith("/manga/proxy/json")
            val resolvedUrl = when {
                proxyHtml || proxyJson -> {
                    val paramIdx = url.indexOf("?url=")
                    if (paramIdx >= 0) java.net.URLDecoder.decode(url.substring(paramIdx + 5), "UTF-8")
                    else url
                }
                url.startsWith("/") -> backendUrl.trimEnd('/') + url
                else -> url
            }

            val optsMap = options as? Map<*, *>
            val method = optsMap?.get("method") as? String ?: "GET"
            @Suppress("UNCHECKED_CAST")
            val headers = optsMap?.get("headers") as? Map<String, String> ?: emptyMap()
            val body = optsMap?.get("body") as? String

            val requestBuilder = Request.Builder().url(resolvedUrl)
            headers.forEach { (k, v) -> requestBuilder.header(k, v) }

            if (method.uppercase() == "POST" && body != null) {
                val ct = headers["Content-Type"] ?: "application/json"
                requestBuilder.post(body.toRequestBody(ct.toMediaType()))
            }

            try {
                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w(TAG, "apiFetch HTTP ${response.code}: $resolvedUrl")
                    return@withContext null
                }
                if (proxyHtml) mapOf("html" to responseBody, "url" to resolvedUrl)
                else jsonToKotlin(responseBody)
            } catch (e: Exception) {
                Log.e(TAG, "apiFetch error: $resolvedUrl", e)
                null
            }
        }
    }

    // Recursively convert JSON string / JSONObject / JSONArray to Kotlin Map/List
    // so QuickJS can receive it without "Cannot convert java type" errors.
    private fun jsonToKotlin(raw: Any?): Any? = when (raw) {
        is String -> {
            val trimmed = raw.trim()
            when {
                trimmed.startsWith("{") -> try { jsonToKotlin(JSONObject(trimmed)) } catch (_: Exception) { raw }
                trimmed.startsWith("[") -> try { jsonToKotlin(JSONArray(trimmed)) } catch (_: Exception) { raw }
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

    private fun serializeElement(el: Element): JSONObject {
        val attrsObj = JSONObject()
        for (attr in el.attributes()) {
            attrsObj.put(attr.key, attr.value)
        }
        return JSONObject().apply {
            put("tag", el.tagName())
            put("text", el.text())
            put("innerHtml", el.html())
            put("outerHtml", el.outerHtml())
            put("attrs", attrsObj)
        }
    }

    private fun jsString(value: String): String = JSONObject.quote(value)

    companion object {
        private const val TAG = "ExtensionManager"

        val NOVEL_EXTENSION_IDS = setOf(
            "royalroad", "novelbin", "novelfull", "freewebnovel", "novelfire", "allnovel",
            "novelphoenix", "readnovelfull", "libread", "brightnovel", "chrysanthemumgarden",
            "comrademao", "lightnoveltranslations", "bestlightnovel", "asianovel", "novelbuddy",
            "readlightnovel", "scribblehub", "lightnovelworld", "wuxiaworld", "ranobes",
            "novelsonline", "readhive"
        )

        fun isNovelSource(extensionId: String): Boolean =
            extensionId.lowercase() in NOVEL_EXTENSION_IDS

        internal fun parseSearchResults(json: String?): List<MangaSearchResult> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                val backendBase = com.mangadl.android.BuildConfig.BACKEND_URL.trimEnd('/')
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    val rawCover = obj.optString("cover_url").ifEmpty { obj.optString("coverUrl") }
                    val resolvedCover = when {
                        rawCover.startsWith("http://") || rawCover.startsWith("https://") -> rawCover
                        rawCover.startsWith("/api/") -> "$backendBase$rawCover"
                        rawCover.startsWith("/") -> "$backendBase/api$rawCover"
                        else -> rawCover
                    }
                    MangaSearchResult(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        coverUrl = resolvedCover,
                        provider = obj.optString("provider"),
                        url = obj.optString("url"),
                    )
                }
            } catch (e: Exception) {
                try { Log.e(TAG, "Parse search error", e) } catch (_: Throwable) {}
                emptyList()
            }
        }

        internal fun parseMangaDetail(json: String?): MangaDetail {
            if (json.isNullOrBlank()) return MangaDetail()
            return try {
                val obj = JSONObject(json)
                val chaptersArr = obj.optJSONArray("chapters")
                val chapters = if (chaptersArr != null) {
                    (0 until chaptersArr.length()).map { i ->
                        val c = chaptersArr.getJSONObject(i)
                        Chapter(
                            id = c.optString("id"),
                            title = c.optString("title"),
                            number = c.optDouble("number", 0.0).toFloat(),
                            publishedAt = c.optString("published_at"),
                        )
                    }
                } else emptyList()

                MangaDetail(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    coverUrl = obj.optString("cover_url"),
                    description = obj.optString("description"),
                    status = obj.optString("status"),
                    genres = obj.optJSONArray("genres")?.let { g ->
                        (0 until g.length()).map { g.optString(it) }
                    } ?: emptyList(),
                    authors = obj.optJSONArray("authors")?.let { a ->
                        (0 until a.length()).map { a.optString(it) }
                    } ?: emptyList(),
                    provider = obj.optString("provider"),
                    url = obj.optString("url"),
                    chapters = chapters,
                )
            } catch (e: Exception) {
                try { Log.e(TAG, "Parse detail error", e) } catch (_: Throwable) {}
                MangaDetail()
            }
        }

        internal fun parsePages(json: String?): List<String> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                val backendBase = com.mangadl.android.BuildConfig.BACKEND_URL.trimEnd('/')
                (0 until arr.length()).map { i ->
                    val raw = arr.optString(i)
                    when {
                        raw.startsWith("http://") || raw.startsWith("https://") -> raw
                        raw.startsWith("/api/") -> "$backendBase$raw"
                        raw.startsWith("/") -> "$backendBase/api$raw"
                        else -> raw
                    }
                }
            } catch (e: Exception) {
                try { Log.e(TAG, "Parse pages error", e) } catch (_: Throwable) {}
                emptyList()
            }
        }

        internal fun parseMetaComment(id: String, script: String): Map<String, String> {
            val meta = mutableMapOf<String, String>("id" to id)
            val metaBlock = Regex("""// ==Extension==\n(.*?)// ==/Extension==""", RegexOption.DOT_MATCHES_ALL)
                .find(script)?.groupValues?.get(1) ?: return meta
            Regex("""// @(\w+)\s+(.+)""").findAll(metaBlock).forEach { m ->
                meta[m.groupValues[1]] = m.groupValues[2].trim()
            }
            return meta
        }
    }
}

data class ExtensionMeta(
    val id: String,
    val file: String,
    val script: String,
    val meta: Map<String, String>,
) {
    val name: String get() = meta["name"] ?: id
    val lang: String get() = meta["lang"] ?: "en"
    val version: String get() = meta["version"] ?: "1.0.0"
    val nsfw: Boolean get() = meta["nsfw"] == "true"
    val iconUrl: String get() = meta["icon"] ?: ""
}
