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
import kotlinx.coroutines.flow.first
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

    lateinit var extensionRepoManager: com.mangadl.android.data.extensions.repo.ExtensionRepoManager
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
        extensionManager = ExtensionManager(sourceManager)
        extensionRepoManager = com.mangadl.android.data.extensions.repo.ExtensionRepoManager(this, httpClient)
        appScope.launch(Dispatchers.IO) {
            extensionRepoManager.refresh()
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

        // Check library for new chapters every 12 hours — respect the user's Background sync /
        // Wi-Fi only switches (SettingsSystemScreen); KEEP'd blindly here would silently
        // resurrect the periodic schedule after the user explicitly cancelled it via the switch.
        appScope.launch(Dispatchers.IO) {
            val prefs = AppPreferences.getInstance(this@MangaDlApp)
            val syncEnabled = prefs.backgroundSyncEnabled.first()
            if (syncEnabled) {
                val wifiOnly = prefs.syncWifiOnly.first()
                val syncConstraints = Constraints.Builder()
                    .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .build()
                wm.enqueueUniquePeriodicWork(
                    LibraryUpdateWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<LibraryUpdateWorker>(12, TimeUnit.HOURS)
                        .setConstraints(syncConstraints)
                        .build(),
                )
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
