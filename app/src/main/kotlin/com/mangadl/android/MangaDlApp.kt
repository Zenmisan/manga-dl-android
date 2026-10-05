package com.mangadl.android

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import com.mangadl.android.data.auth.GoogleAuthHelper
import com.mangadl.android.data.download.DownloadWorker
import com.mangadl.android.data.extensions.ExtensionManager
import com.mangadl.android.data.db.AppDatabase
import com.mangadl.android.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaDlApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: AppDatabase
        private set

    lateinit var httpClient: OkHttpClient
        private set

    lateinit var extensionManager: ExtensionManager
        private set

    lateinit var googleAuthHelper: GoogleAuthHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", BROWSER_UA)
                    .build()
                chain.proceed(request)
            }
            .build()

        database = AppDatabase.getInstance(this)
        googleAuthHelper = GoogleAuthHelper(this)

        extensionManager = ExtensionManager(this, httpClient)
        extensionManager.backendUrl = BuildConfig.BACKEND_URL
        extensionManager.loadAll()

        // Re-run any pending downloads on app start
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            DownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DownloadWorker>().setConstraints(constraints).build(),
        )

        // Keep backendUrl in sync with user setting
        val prefs = AppPreferences.getInstance(this)
        appScope.launch {
            prefs.backendUrl.collect { url ->
                if (url.isNotEmpty()) extensionManager.backendUrl = url
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
    }

    companion object {
        lateinit var instance: MangaDlApp
            private set

        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
