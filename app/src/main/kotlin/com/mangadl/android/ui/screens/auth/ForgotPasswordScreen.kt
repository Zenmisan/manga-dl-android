package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.ButtonTone
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdTextField
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.TextLink
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, initiallySent: Boolean = false) {
    val c = MdTheme.colors
    var email by rememberState("you@example.com")
    var sent by rememberState(initiallySent)
    Screen {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MdIconButton(MdIcons.Back, "Back", onBack, Modifier.offset(x = (-12).dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DisplayText("Reset password", 48.sp)
                BodyText("Enter your account email. We'll send a link to set a new password.", size = 15.sp, color = c.fgMuted, lineHeight = 22.sp)
            }
            MdTextField(email, { email = it }, label = "Email", keyboardType = KeyboardType.Email)
            if (sent) {
                MdButton("Link Sent", {}, Modifier.fillMaxWidth(), tone = ButtonTone.Success, leadingIcon = MdIcons.Check)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .padding(16.dp),
                ) {
                    BodyText(
                        "If an account exists for that email, a reset link is on its way. Check your spam folder too.",
                        color = c.fg.copy(alpha = 0.8f), lineHeight = 21.sp,
                    )
                }
            } else {
                MdButton("Send Reset Link", { sent = true }, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(4.dp))
        TextLink("Back to Sign In", onBack, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
    }
}
