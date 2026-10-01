package com.mangadl.android

import android.app.Application
import com.mangadl.android.data.extensions.ExtensionManager
import com.mangadl.android.data.db.AppDatabase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaDlApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var httpClient: OkHttpClient
        private set

    lateinit var extensionManager: ExtensionManager
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

        extensionManager = ExtensionManager(this, httpClient)
        extensionManager.loadAll()
    }

    companion object {
        lateinit var instance: MangaDlApp
            private set

        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
