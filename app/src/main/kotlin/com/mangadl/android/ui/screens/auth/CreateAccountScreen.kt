package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.Eyebrow
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdCheckbox
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdTextField
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.TextLink
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme

@Composable
fun CreateAccountScreen(onBack: () -> Unit, onCreate: () -> Unit, onSignIn: () -> Unit, onTerms: () -> Unit = {}) {
    val c = MdTheme.colors
    var username by rememberState("[username]")
    var email by rememberState("you@example.com")
    var password by rememberState("password1")
    var confirm by rememberState("password2")
    var agreed by rememberState(true)
    val mismatch = confirm.isNotEmpty() && confirm != password
    val valid = username.isNotBlank() && email.isNotBlank() && password.isNotEmpty() && !mismatch && agreed
    Screen {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            MdIconButton(MdIcons.Back, "Back", onBack, Modifier.offset(x = (-12).dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Step 3 of 3")
                DisplayText("Create account", 44.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MdTextField(username, { username = it }, label = "Username")
                MdTextField(email, { email = it }, label = "Email", keyboardType = KeyboardType.Email)
                MdTextField(password, { password = it }, label = "Password", isPassword = true)
                MdTextField(confirm, { confirm = it }, label = "Confirm password", isPassword = true, isError = mismatch)
                if (mismatch) BodyText("Passwords don't match.", size = 13.sp, weight = FontWeight.SemiBold, color = c.errorText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MdCheckbox(agreed, { agreed = it }, "I agree to the")
                    TextLink("Terms", onTerms)
                }
            }
            MdButton("Create Account", onCreate, Modifier.fillMaxWidth(), enabled = valid)
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyText("Have an account?", color = c.fgMuted)
            TextLink("Sign in", onSignIn)
        }
    }
}
