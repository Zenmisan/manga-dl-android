package com.mangadl.android

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mangadl.android.data.auth.GoogleAuthHelper
import com.mangadl.android.data.download.DownloadWorker
import com.mangadl.android.data.library.LibraryUpdateWorker
import com.mangadl.android.data.extensions.ExtensionManager
import com.mangadl.android.data.db.AppDatabase
import com.mangadl.android.data.network.AndroidCookieJar
import com.mangadl.android.data.network.CloudflareInterceptor
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

    lateinit var sourceManager: com.mangadl.android.data.source.SourceManager
        private set

    lateinit var googleAuthHelper: GoogleAuthHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val cookieJar = AndroidCookieJar()
        httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", BROWSER_UA)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(CloudflareInterceptor(this, cookieJar) { BROWSER_UA })
            .addInterceptor(com.mangadl.android.data.source.descramble.TileDescramblerInterceptor())
            .build()

        database = AppDatabase.getInstance(this)
        googleAuthHelper = GoogleAuthHelper(this)

        sourceManager = com.mangadl.android.data.source.SourceManager(this, httpClient)
        extensionManager = ExtensionManager(this, httpClient)
        extensionManager.sourceManager = sourceManager
        extensionManager.backendUrl = BuildConfig.BACKEND_URL
        // loadAll() synchronously reads and parses all 39 bundled extension .js files from
        // assets; doing that on Application.onCreate() (main thread) blocks the first frame.
        // Moved off-thread; screens that read listExtensions() before this completes will
        // briefly see an empty list (true for the first composition regardless of where this
        // runs, since it was never actually synchronous-safe for Compose to depend on).
        appScope.launch(Dispatchers.IO) {
            extensionManager.loadAll()
        }

        LibraryUpdateWorker.createChannel(this)
        DownloadWorker.createChannel(this)

        val wm = WorkManager.getInstance(this)
        val networkConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        // Re-run any pending downloads on app start
        wm.enqueueUniqueWork(
            DownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DownloadWorker>().setConstraints(networkConstraints).build(),
        )

        // Check library for new chapters every 12 hours
        wm.enqueueUniquePeriodicWork(
            LibraryUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LibraryUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .build(),
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
