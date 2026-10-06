package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.network.BackendHealth
import com.mangadl.android.data.prefs.AppPreferences
import com.mangadl.android.data.prefs.PrefKeys
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdTextField
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.TextLink
import com.mangadl.android.ui.components.VSpace
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onBack: () -> Unit, onContinue: () -> Unit, onSkip: () -> Unit) {
    val c = MdTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPrefs = remember { AppPreferences.getInstance(context) }

    var url by rememberState("https://[your-server]:8000")
    var key by rememberState("")
    var connected by rememberState(false)
    var testing by rememberState(false)
    var testError by rememberState<String?>(null)

    Screen {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            StepBarOnboarding(step = 2, of = 3)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Step 2 of 3")
                DisplayText("Connect to backend", 44.sp)
                BodyText(
                    "The app reads sources on your phone. A backend is only needed for sync and backups. Skip it to read locally.",
                    size = 15.sp, color = c.fgMuted, lineHeight = 22.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MdTextField(url, { url = it; connected = false; testError = null }, label = "Backend URL", keyboardType = KeyboardType.Uri)
                MdTextField(key, { key = it }, label = "API key (optional)", isPassword = true)
                if (connected) StatusBoxOnboarding("Connected", isError = false)
                if (testError != null) StatusBoxOnboarding(testError!!, isError = true)
                MdButton(
                    if (testing) "Testing…" else "Test Connection",
                    onClick = {
                        scope.launch {
                            testing = true
                            testError = null
                            BackendHealth.checkHealth(url)
                                .onSuccess {
                                    connected = true
                                    testError = null
                                }
                                .onFailure { e ->
                                    connected = false
                                    testError = e.message ?: "Failed to connect"
                                }
                            testing = false
                        }
                    },
                    tone = ButtonTone.Ghost,
                    height = 44.dp,
                    shape = RoundedCornerShape(12.dp),
                    fontSize = 13.sp,
                )
            }
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp, top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MdButton("Back", onBack, Modifier.weight(1f), tone = ButtonTone.Ghost)
                MdButton(
                    "Continue",
                    onClick = {
                        scope.launch {
                            appPrefs.set(PrefKeys.BACKEND_URL, url)
                            appPrefs.set(PrefKeys.API_KEY, key)
                            onContinue()
                        }
                    },
                    modifier = Modifier.weight(2f),
                )
            }
            VSpace(4.dp)
            TextLink("Skip — Read Offline", onSkip)
        }
    }
}

@Composable
private fun StepBarOnboarding(step: Int, of: Int) {
    val c = MdTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(of) { i ->
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i < step) c.accent else c.border))
        }
    }
}

@Composable
private fun StatusBoxOnboarding(text: String, isError: Boolean = false) {
    val c = MdTheme.colors
    val bg = if (isError) c.accentFaint else c.success.copy(alpha = 0.12f)
    val fg = if (isError) c.accentLight else c.successText
    val icon = if (isError) MdIcons.Close else MdIcons.Check
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        BodyText(text, size = 13.sp, weight = FontWeight.Bold, color = fg)
    }
}
