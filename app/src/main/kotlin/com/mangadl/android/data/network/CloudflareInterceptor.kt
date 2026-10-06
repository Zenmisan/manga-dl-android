package com.mangadl.android.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Detects a Cloudflare (or Turnstile) anti-bot challenge response and solves it in a headless
 * [WebView], then replays the original request with the resulting clearance cookies attached.
 *
 * A single [bypassLock] serializes challenge-solving across threads so concurrent requests to the
 * same (or different) hosts don't spawn multiple WebViews at once — they queue and each benefits
 * from whatever cookies the previous solve already produced.
 */
class CloudflareInterceptor(
    private val context: Context,
    private val cookieJar: AndroidCookieJar,
    private val defaultUserAgentProvider: () -> String,
) : Interceptor {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bypassLock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!shouldIntercept(response)) return response

        return bypassLock.withLock {
            // Another thread may have already solved this exact host's challenge while we were
            // waiting on the lock — check for a fresh clearance cookie before spawning a WebView.
            val cookieBeforeWait = cookieJar.get(request.url).firstOrNull { it.name == "cf_clearance" }
            if (cookieBeforeWait != null && cookieBeforeWait.expiresAt > System.currentTimeMillis()) {
                response.close()
                return@withLock chain.proceed(request)
            }

            try {
                response.close()
                cookieJar.remove(request.url, COOKIE_NAMES, 0)
                val oldCookie = cookieJar.get(request.url).firstOrNull { it.name == "cf_clearance" }
                resolveWithWebView(request, oldCookie)
                chain.proceed(request)
            } catch (e: CloudflareBypassException) {
                throw IOException("Failed to bypass Cloudflare challenge for ${request.url}", e)
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw IOException(e)
            }
        }
    }

    private fun shouldIntercept(response: Response): Boolean {
        if (response.code !in CHALLENGE_CODES) return false
        val server = response.header("Server")?.lowercase(Locale.US) ?: ""
        return SERVER_MARKERS.any { it in server }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(originalRequest: Request, oldCookie: Cookie?) {
        val latch = CountDownLatch(1)
        val origRequestUrl = originalRequest.url.toString()
        val headers = parseHeaders(originalRequest.headers)
        var webView: WebView? = null
        var cloudflareBypassed = false

        mainHandler.post {
            val wv = WebView(context)
            webView = wv
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = originalRequest.header("User-Agent") ?: defaultUserAgentProvider()
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

            fun isCloudflareBypassed(): Boolean {
                val fresh = cookieJar.get(origRequestUrl.toHttpUrl()).firstOrNull { it.name == "cf_clearance" }
                return fresh != null && fresh != oldCookie
            }

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    if (isCloudflareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        Log.w(TAG, "WebView error resolving ${request.url}: ${error.description}")
                        latch.countDown()
                    }
                }
            }
            wv.loadUrl(origRequestUrl, headers)
        }

        latch.await(30, TimeUnit.SECONDS)

        mainHandler.post {
            webView?.stopLoading()
            webView?.destroy()
        }

        if (!cloudflareBypassed) {
            throw CloudflareBypassException()
        }
    }

    private fun parseHeaders(headers: Headers): Map<String, String> {
        return headers
            .filter { (name, _) -> name.lowercase(Locale.US) !in UNSAFE_HEADER_NAMES }
            .groupBy(keySelector = { (name, _) -> name }) { (_, value) -> value }
            .mapValues { it.value.firstOrNull().orEmpty() }
    }

    companion object {
        private const val TAG = "CloudflareInterceptor"
        private val CHALLENGE_CODES = listOf(403, 503)
        private val SERVER_MARKERS = listOf("cloudflare")
        private val COOKIE_NAMES = listOf("cf_clearance")
        private val UNSAFE_HEADER_NAMES = listOf(
            "content-length", "host", "trailer", "te", "upgrade",
            "cookie2", "keep-alive", "transfer-encoding", "set-cookie",
        )
    }
}

private class CloudflareBypassException : Exception()
