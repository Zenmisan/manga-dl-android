package com.mangadl.android

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.ui.MangaDlNavHost
import com.mangadl.android.ui.Routes
import com.mangadl.android.ui.screens.auth.BiometricLockGate
import com.mangadl.android.ui.theme.Accent
import com.mangadl.android.ui.theme.MangaDlTheme
import com.mangadl.android.ui.viewmodels.TrackerViewModel
import io.github.jan.supabase.auth.auth

class MainActivity : FragmentActivity() {

    private val trackerVm: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        val startDest = if (SupabaseManager.client.auth.currentUserOrNull() != null) Routes.Main else Routes.Welcome
        setContent {
            var accent by remember { mutableStateOf(Accent.Red) }
            val biometricLock by AppPreferences.getInstance(this).biometricLock.collectAsState(initial = false)
            MangaDlTheme(accent = accent) {
                BiometricLockGate(enabled = biometricLock) {
                    MangaDlNavHost(onAccentChange = { accent = it }, startDestination = startDest)
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            val handled = com.mangadl.android.ui.screens.reader.ReaderKeyEvents.onVolumeKey
                ?.invoke(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) ?: false
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        when (data.host) {
            "anilist-callback" -> trackerVm.handleAnilistCallback(data.toString())
            "mal-callback" -> trackerVm.handleMalCallback(data)
        }
    }
}
