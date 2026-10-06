package com.mangadl.android.data.network

import com.mangadl.android.MangaDlApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.TimeUnit

object BackendHealth {
    suspend fun checkHealth(rawUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val base = rawUrl.trim().trimEnd('/')
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                throw IllegalArgumentException("URL must start with http:// or https://")
            }
            val client = MangaDlApp.instance.httpClient.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            val req = Request.Builder()
                .url("$base/health")
                .get()
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful || response.code in 200..404) {
                    true
                } else {
                    throw IllegalStateException("Server returned HTTP ${response.code}")
                }
            }
        }
    }
}
