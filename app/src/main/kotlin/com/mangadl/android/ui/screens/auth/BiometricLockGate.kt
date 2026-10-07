package com.mangadl.android.ui.screens.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.theme.MdTheme

/**
 * Gates [content] behind a biometric/device-credential prompt when "Biometric app lock" is on.
 * Fails open (shows content) if the device has no usable biometric/credential enrolled — the
 * pref exists to protect the app, not to brick access on devices that can't satisfy it.
 */
@Composable
fun BiometricLockGate(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var unlocked by remember(enabled) { mutableStateOf(!enabled) }
    var checking by remember(enabled) { mutableStateOf(enabled) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(enabled, retryTrigger) {
        if (!enabled || activity == null) return@LaunchedEffect

        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No enrolled fingerprint/face/PIN to check against — nothing to gate with.
            unlocked = true
            checking = false
            return@LaunchedEffect
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                    checking = false
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    checking = false
                }
                override fun onAuthenticationFailed() {
                    // Wrong fingerprint/face — prompt stays open for another attempt.
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock manga-dl")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(promptInfo)
    }

    when {
        unlocked -> content()
        checking -> Screen {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = MdTheme.colors.accent)
            }
        }
        else -> Screen {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                BodyText("manga-dl is locked", size = 18.sp)
                MdButton("Try again", onClick = { checking = true; retryTrigger++ })
            }
        }
    }
}
