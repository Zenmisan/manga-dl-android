package com.mangadl.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mangadl.android.ui.MangaDlNavHost
import com.mangadl.android.ui.theme.Accent
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.TrackerViewModel

class MainActivity : ComponentActivity() {

    private val trackerVm: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            var accent by remember { mutableStateOf(Accent.Red) }
            MangaDlTheme(accent = accent) {
                MangaDlNavHost(onAccentChange = { accent = it })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        when (data.host) {
            "anilist-callback" -> trackerVm.handleAnilistCallback(data.fragment)
            "mal-callback" -> trackerVm.handleMalCallback(data)
        }
    }
}
