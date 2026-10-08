package com.mangadl.android.data.repository

import com.mangadl.android.BuildConfig
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.model.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request

class LeaderboardRepository(
    private val baseUrl: String = BuildConfig.BACKEND_URL.trimEnd('/'),
) {
    private val client by lazy { MangaDlApp.instance.httpClient }
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun getLeaderboard(
        period: String = "all_time",
        limit: Int = 50,
    ): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$baseUrl/users/leaderboard?period=$period&limit=$limit"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@runCatching emptyList()
            }
            val body = response.body?.string() ?: return@runCatching emptyList()
            json.decodeFromString<List<LeaderboardEntry>>(body)
        }
    }
}
