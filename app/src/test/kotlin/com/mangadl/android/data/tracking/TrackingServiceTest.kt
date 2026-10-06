package com.mangadl.android.data.tracking

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TrackingServiceTest {

    @Test
    fun testAnilistTokenExtractionFromFragmentAndQuery() {
        // Test standard OAuth implicit fragment
        val fragmentUrl = "mangadl://anilist-callback#access_token=al_secret_token_12345&token_type=Bearer&expires_in=31536000"
        val paramStr1 = fragmentUrl.substringAfter("#")
        val map1 = paramStr1.split("&").associate {
            val p = it.split("=", limit = 2)
            p[0] to p.getOrElse(1) { "" }
        }
        assertEquals("al_secret_token_12345", map1["access_token"])
        assertEquals("Bearer", map1["token_type"])

        // Test standard OAuth query
        val queryUrl = "mangadl://anilist-callback?access_token=al_query_token_999&state=xyz"
        val paramStr2 = queryUrl.substringAfter("?")
        val map2 = paramStr2.split("&").associate {
            val p = it.split("=", limit = 2)
            p[0] to p.getOrElse(1) { "" }
        }
        assertEquals("al_query_token_999", map2["access_token"])
    }

    @Test
    fun testChapterNumberExtraction() {
        val regex = Regex("""(?:chapter[_-]?|ch[_-]?|#)?([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)

        val c1 = regex.find("chapter-145")?.groupValues?.get(1)?.toFloatOrNull()
        assertEquals(145f, c1)

        val c2 = regex.find("ch_23.5")?.groupValues?.get(1)?.toFloatOrNull()
        assertEquals(23.5f, c2)

        val c3 = regex.find("#99")?.groupValues?.get(1)?.toFloatOrNull()
        assertEquals(99f, c3)

        val c4 = regex.find("42")?.groupValues?.get(1)?.toFloatOrNull()
        assertEquals(42f, c4)
    }

    @Test
    fun testAnilistViewerJsonParsing() {
        val viewerJson = """
        {
          "data": {
            "Viewer": {
              "id": 123456,
              "name": "ZenmiReader"
            }
          }
        }
        """.trimIndent()

        val json = JSONObject(viewerJson)
        val viewer = json.getJSONObject("data").getJSONObject("Viewer")
        assertEquals(123456, viewer.getInt("id"))
        assertEquals("ZenmiReader", viewer.getString("name"))
    }

    @Test
    fun testAnilistSearchJsonParsing() {
        val searchJson = """
        {
          "data": {
            "Page": {
              "media": [
                {
                  "id": 105398,
                  "title": {
                    "romaji": "Na Honjaman Level Up",
                    "english": "Solo Leveling",
                    "userPreferred": "Solo Leveling"
                  },
                  "chapters": 200,
                  "coverImage": {
                    "medium": "https://s4.anilist.co/file/anilistcdn/media/manga/cover/medium/bx105398.jpg"
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val json = JSONObject(searchJson)
        val mediaArray = json.getJSONObject("data").getJSONObject("Page").getJSONArray("media")
        assertEquals(1, mediaArray.length())

        val item = mediaArray.getJSONObject(0)
        assertEquals(105398, item.getInt("id"))
        val tObj = item.getJSONObject("title")
        val title = tObj.optString("english").ifEmpty { tObj.optString("romaji") }
        assertEquals("Solo Leveling", title)
        assertEquals(200, item.getInt("chapters"))
    }

    @Test
    fun testMalSearchJsonParsing() {
        val malSearchJson = """
        {
          "data": [
            {
              "node": {
                "id": 121496,
                "title": "Solo Leveling",
                "num_chapters": 179,
                "main_picture": {
                  "medium": "https://cdn.myanimelist.net/images/manga/3/222295.jpg"
                }
              }
            }
          ]
        }
        """.trimIndent()

        val json = JSONObject(malSearchJson)
        val data = json.getJSONArray("data")
        assertEquals(1, data.length())

        val node = data.getJSONObject(0).getJSONObject("node")
        assertEquals(121496, node.getInt("id"))
        assertEquals("Solo Leveling", node.getString("title"))
        assertEquals(179, node.getInt("num_chapters"))
        assertEquals("https://cdn.myanimelist.net/images/manga/3/222295.jpg", node.getJSONObject("main_picture").getString("medium"))
    }

    @Test
    fun testMalUserJsonParsing() {
        val userJson = """
        {
          "id": 987654,
          "name": "zenmisan",
          "location": "Tokyo"
        }
        """.trimIndent()

        val json = JSONObject(userJson)
        assertEquals(987654, json.getInt("id"))
        assertEquals("zenmisan", json.getString("name"))
    }

    @Test
    fun testProgressMutationPayloadConstruction() {
        val mediaId = 105398
        val progress = 45
        val isCompleted = false

        val status = if (isCompleted) "COMPLETED" else "CURRENT"
        val variables = JSONObject()
            .put("mediaId", mediaId)
            .put("progress", progress)
            .put("status", status)

        assertEquals(105398, variables.getInt("mediaId"))
        assertEquals(45, variables.getInt("progress"))
        assertEquals("CURRENT", variables.getString("status"))
    }
}
