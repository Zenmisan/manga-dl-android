package com.mangadl.android.data.source

import android.graphics.Bitmap
import com.mangadl.android.data.source.descramble.TileDescramblerInterceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NativeSourceEngineTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var sourceManager: SourceManager

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().build()
        sourceManager = SourceManager(RuntimeEnvironment.getApplication(), client)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun testSourceManagerRegistration() {
        val mangaSources = listOf(
            "mangadex", "asurascans", "mangakakalot", "mangakatana",
            "omegascans", "manganato", "tcbscans", "webtoons", "yaoiscan"
        )
        val novelSources = listOf(
            "novelbin", "ranobes", "royalroad", "lightnovelworld",
            "novelfire", "freewebnovel", "novelfull", "readnovelfull",
            "chrysanthemumgarden", "comrademao", "asianovel", "readhive",
            "libread", "brightnovel", "novelbuddy"
        )

        for (id in mangaSources) {
            assertNotNull("Manga source $id should be registered", sourceManager.getSource(id))
            assertFalse("Source $id should not be a novel source", sourceManager.isNovelSource(id))
        }

        for (id in novelSources) {
            assertNotNull("Novel source $id should be registered", sourceManager.getSource(id))
            assertTrue("Source $id should be a novel source", sourceManager.isNovelSource(id))
        }

        assertEquals(mangaSources.size, sourceManager.listMangaSources().size)
        assertEquals(novelSources.size, sourceManager.listNovelSources().size)
        assertEquals(mangaSources.size + novelSources.size, sourceManager.listSources().size)
    }

    @Test
    fun testTileDescramblerLogic() {
        // Create an 8x10 test bitmap (2 cols, 2 rows of 4x5 tiles)
        val bmp = Bitmap.createBitmap(8, 10, Bitmap.Config.ARGB_8888)
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val rawBytes = baos.toByteArray()

        val tiles = intArrayOf(0, 1, 2, 3)
        val descrambled = TileDescramblerInterceptor.descramble(rawBytes, tiles, 2, 2)

        assertNotNull(descrambled)
        assertTrue("Descrambled bytes should not be empty", descrambled.isNotEmpty())
    }

    @Test
    fun testMangaDexParsing() = kotlinx.coroutines.runBlocking {
        val jsonSearch = """
            {
                "result": "ok",
                "data": [
                    {
                        "id": "md-123",
                        "type": "manga",
                        "attributes": {
                            "title": { "en": "Solo Leveling" },
                            "status": "completed"
                        },
                        "relationships": [
                            { "type": "cover_art", "attributes": { "fileName": "cover.jpg" } }
                        ]
                    }
                ]
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(jsonSearch))

        val customClient = client.newBuilder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().url(server.url("/manga")).build())
        }.build()

        val mdSource = com.mangadl.android.data.source.manga.MangaDexSource(customClient)
        val results = mdSource.search("Solo Leveling")

        assertEquals(1, results.size)
        assertEquals("md-123", results[0].id)
        assertEquals("Solo Leveling", results[0].title)
        assertTrue(results[0].coverUrl.contains("cover.jpg"))
    }
}
