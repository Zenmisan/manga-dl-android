package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.mangadl.android.ui.components.OrDivider
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.TextLink
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onForgot: () -> Unit,
    onUseWithoutAccount: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val c = MdTheme.colors
    var email by rememberState("")
    var password by rememberState("")
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
                DisplayText("Sign in", 52.sp)
                BodyText("Sync your library, history and backups across devices.", size = 15.sp, color = c.fgMuted, lineHeight = 22.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MdTextField(email, { email = it }, label = "Email", placeholder = "you@example.com", keyboardType = KeyboardType.Email)
                MdTextField(password, { password = it }, label = "Password", placeholder = "Your password", isPassword = true)
                TextLink("Forgot password?", onForgot, Modifier.align(Alignment.End), size = 13.sp)
                MdButton("Sign In", onSignIn, Modifier.fillMaxWidth())
            }
            OrDivider()
            MdButton("Use Without Account", onUseWithoutAccount, Modifier.fillMaxWidth(), tone = ButtonTone.Ghost)
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyText("No account?", color = c.fgMuted)
            TextLink("Create one", onCreateAccount)
        }
    }
}
