package com.mangadl.android.data.extensions

import android.content.Context
import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.define
import com.mangadl.android.data.model.Chapter
import com.mangadl.android.data.model.MangaDetail
import com.mangadl.android.data.model.MangaSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ExtensionManager(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private val extensions = mutableMapOf<String, ExtensionMeta>()

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
        val script = extensions[extensionId]?.script ?: return emptyList()
        val result = evalJs(extensionId, script) { js ->
            js.evaluate<String>(
                """
                (async () => {
                    const ext = (() => { $script; return extension; })();
                    const results = await ext.search(${jsString(query)}, $page);
                    return JSON.stringify(results);
                })()
                """.trimIndent()
            )
        }
        return parseSearchResults(result)
    }

    suspend fun getMangaDetail(extensionId: String, mangaId: String): MangaDetail {
        val script = extensions[extensionId]?.script ?: return MangaDetail()
        val result = evalJs(extensionId, script) { js ->
            js.evaluate<String>(
                """
                (async () => {
                    const ext = (() => { $script; return extension; })();
                    const detail = await ext.getMangaDetail(${jsString(mangaId)});
                    return JSON.stringify(detail);
                })()
                """.trimIndent()
            )
        }
        return parseMangaDetail(result)
    }

    suspend fun getPages(extensionId: String, chapterId: String): List<String> {
        val script = extensions[extensionId]?.script ?: return emptyList()
        val result = evalJs(extensionId, script) { js ->
            js.evaluate<String>(
                """
                (async () => {
                    const ext = (() => { $script; return extension; })();
                    const pages = await ext.getPages(${jsString(chapterId)});
                    return JSON.stringify(pages);
                })()
                """.trimIndent()
            )
        }
        return parsePages(result)
    }

    private suspend fun <T> evalJs(id: String, script: String, block: suspend (QuickJs) -> T): T {
        return withContext(Dispatchers.IO) {
            val js = QuickJs.create(Dispatchers.IO)
            try {
                js.define("console") {
                    asyncFunction("log") { args: Array<Any?> ->
                        Log.d("JS[$id]", args.joinToString(" "))
                    }
                    asyncFunction("error") { args: Array<Any?> ->
                        Log.e("JS[$id]", args.joinToString(" "))
                    }
                    asyncFunction("warn") { args: Array<Any?> ->
                        Log.w("JS[$id]", args.joinToString(" "))
                    }
                }
                js.asyncFunction("apiFetch") { args: Array<Any?> ->
                    val url = args.getOrNull(0) as? String ?: return@asyncFunction null
                    val options = args.getOrNull(1)
                    apiFetch(url, options)
                }
                block(js)
            } finally {
                js.close()
            }
        }
    }

    private suspend fun apiFetch(url: String, options: Any?): Map<String, Any?> {
        return withContext(Dispatchers.IO) {
            val optsMap = options as? Map<*, *>
            val method = optsMap?.get("method") as? String ?: "GET"
            @Suppress("UNCHECKED_CAST")
            val headers = optsMap?.get("headers") as? Map<String, String> ?: emptyMap()
            val body = optsMap?.get("body") as? String

            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.header(k, v) }

            if (method.uppercase() == "POST" && body != null) {
                val ct = headers["Content-Type"] ?: "application/json"
                requestBuilder.post(body.toRequestBody(ct.toMediaType()))
            }

            try {
                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                val status = response.code
                mapOf(
                    "status" to status,
                    "text" to responseBody,
                    "json" to tryParseJson(responseBody),
                    "ok" to (status in 200..299),
                )
            } catch (e: Exception) {
                Log.e(TAG, "apiFetch error: $url", e)
                mapOf("status" to 0, "text" to "", "json" to null, "ok" to false)
            }
        }
    }

    private fun tryParseJson(text: String): Any? {
        return try { JSONObject(text) } catch (_: Exception) {
            try { JSONArray(text) } catch (_: Exception) { null }
        }
    }

    private fun jsString(value: String): String = JSONObject.quote(value)

    private fun parseSearchResults(json: String?): List<MangaSearchResult> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                MangaSearchResult(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    coverUrl = obj.optString("cover_url"),
                    provider = obj.optString("provider"),
                    url = obj.optString("url"),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse search error", e)
            emptyList()
        }
    }

    private fun parseMangaDetail(json: String?): MangaDetail {
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
            Log.e(TAG, "Parse detail error", e)
            MangaDetail()
        }
    }

    private fun parsePages(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> arr.optString(i) }
        } catch (e: Exception) {
            Log.e(TAG, "Parse pages error", e)
            emptyList()
        }
    }

    private fun parseMetaComment(id: String, script: String): Map<String, String> {
        val meta = mutableMapOf<String, String>("id" to id)
        val metaBlock = Regex("""// ==Extension==\n(.*?)// ==/Extension==""", RegexOption.DOT_MATCHES_ALL)
            .find(script)?.groupValues?.get(1) ?: return meta
        Regex("""// @(\w+)\s+(.+)""").findAll(metaBlock).forEach { m ->
            meta[m.groupValues[1]] = m.groupValues[2].trim()
        }
        return meta
    }

    companion object {
        private const val TAG = "ExtensionManager"
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
    val iconUrl: String get() = meta["icon"] ?: ""
    val isNsfw: Boolean get() = meta["nsfw"] == "true"
}
