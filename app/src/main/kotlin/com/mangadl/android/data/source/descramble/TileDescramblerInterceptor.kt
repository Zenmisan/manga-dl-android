package com.mangadl.android.data.source.descramble

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * On-device image tile descrambler for scrambled manga sources (e.g. Asura Scans).
 *
 * When an image URL contains a tile metadata JSON in its fragment:
 * `https://example.com/page.jpg#{"tiles":[1,3,0,2],"tileCols":4,"tileRows":5}`
 *
 * This interceptor intercepts the raw response, slices the scrambled bitmap tiles using
 * Android's hardware-accelerated Skia Canvas, reassembles the clean image in memory,
 * and passes the descrambled JPEG bytes directly to Coil / DownloadWorker.
 *
 * Zero server backend dependencies, zero proxy latency.
 */
class TileDescramblerInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val fragment = request.url.fragment ?: return response

        if (!fragment.startsWith("{") || !fragment.contains("\"tiles\"")) {
            return response
        }

        val body = response.body ?: return response
        val rawBytes = body.bytes()

        return try {
            val json = JSONObject(fragment)
            val tilesArr = json.getJSONArray("tiles")
            val tileCols = json.optInt("tileCols", 4)
            val tileRows = json.optInt("tileRows", 5)

            val tiles = IntArray(tilesArr.length()) { i -> tilesArr.getInt(i) }
            val descrambledBytes = descramble(rawBytes, tiles, tileCols, tileRows)

            response.newBuilder()
                .body(descrambledBytes.toResponseBody("image/jpeg".toMediaType()))
                .build()
        } catch (e: Exception) {
            // Fallback to original bytes if descrambling fails
            response.newBuilder()
                .body(rawBytes.toResponseBody(body.contentType()))
                .build()
        }
    }

    companion object {
        fun descramble(rawBytes: ByteArray, tiles: IntArray, cols: Int, rows: Int): ByteArray {
            val source = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: throw IOException("Failed to decode source image bytes")

            val tileW = source.width / cols
            val tileH = source.height / rows

            val output = Bitmap.createBitmap(
                tileW * cols,
                tileH * rows,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(output)

            for (w in tiles.indices) {
                val j = tiles[w]
                val srcCol = w % cols
                val srcRow = w / cols
                val dstCol = j % cols
                val dstRow = j / cols

                val srcRect = Rect(srcCol * tileW, srcRow * tileH, (srcCol + 1) * tileW, (srcRow + 1) * tileH)
                val dstRect = Rect(dstCol * tileW, dstRow * tileH, (dstCol + 1) * tileW, (dstRow + 1) * tileH)

                canvas.drawBitmap(source, srcRect, dstRect, null)
            }

            val baos = ByteArrayOutputStream()
            output.compress(Bitmap.CompressFormat.JPEG, 92, baos)
            source.recycle()
            output.recycle()

            return baos.toByteArray()
        }
    }
}
