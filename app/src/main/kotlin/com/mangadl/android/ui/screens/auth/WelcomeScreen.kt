package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.Bloom
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.VSpace
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun WelcomeScreen(onStartReading: () -> Unit, onSignIn: () -> Unit) {
    val c = MdTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Bloom(Modifier.offset((-120).dp, (-160).dp).size(560.dp))
        Screen(background = androidx.compose.ui.graphics.Color.Transparent) {
          BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.accent),
                        contentAlignment = Alignment.Center,
                    ) { DisplayText("M", 18.sp) }
                    BodyText("manga-dl", size = 15.sp, weight = FontWeight.ExtraBold)
                }
                VSpace(40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Eyebrow("Free · open source")
                    DisplayText("Your manga, everywhere.", 68.sp, lineHeight = 65.sp)
                    BodyText(
                        "Read from 50+ sources, save chapters for offline, and pick up where you left off on phone, desktop or web.",
                        size = 16.sp,
                        color = c.fgMuted,
                        lineHeight = 24.sp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Sources load right on your phone", "Offline CBZ downloads", "Optional cloud sync").forEach {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(MdIcons.Check, null, tint = c.accentLight, modifier = Modifier.size(18.dp))
                                BodyText(it, weight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                VSpace(40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MdButton("Start Reading", onStartReading, Modifier.fillMaxWidth())
                    MdButton("Sign In", onSignIn, Modifier.fillMaxWidth(), tone = ButtonTone.Ghost)
                }
            }
          }
        }
    }
}
