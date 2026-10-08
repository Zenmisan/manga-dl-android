package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangadl.android.ui.components.BodyText
import com.mangadl.android.ui.components.DisplayText
import com.mangadl.android.ui.components.MdButton
import com.mangadl.android.ui.components.MdCheckbox
import com.mangadl.android.ui.components.MdIconButton
import com.mangadl.android.ui.components.MdIcons
import com.mangadl.android.ui.components.MdTextField
import com.mangadl.android.ui.components.Screen
import com.mangadl.android.ui.components.TextLink
import com.mangadl.android.ui.components.rememberState
import com.mangadl.android.ui.theme.MdTheme
import com.mangadl.android.ui.viewmodels.AuthState
import com.mangadl.android.ui.viewmodels.AuthViewModel

@Composable
fun CreateAccountScreen(onBack: () -> Unit, onCreate: () -> Unit, onSignIn: () -> Unit, onTerms: () -> Unit = {}) {
    val c = MdTheme.colors
    val vm: AuthViewModel = viewModel()
    val authState by vm.authState.collectAsState()
    var username by rememberState("")
    var email by rememberState("")
    var password by rememberState("")
    var confirm by rememberState("")
    var agreed by rememberState(false)
    val mismatch = confirm.isNotEmpty() && confirm != password
    val valid = username.isNotBlank() && email.isNotBlank() && password.isNotEmpty() && !mismatch && agreed

    val loading = authState is AuthState.Loading
    val error = (authState as? AuthState.Error)?.message

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onCreate()
    }

    Screen {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                MdIconButton(MdIcons.Back, "Back", onBack, Modifier.offset(x = (-12).dp))
                DisplayText("Create account", 44.sp)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MdTextField(username, { username = it }, label = "Username")
                    MdTextField(email, { email = it }, label = "Email", keyboardType = KeyboardType.Email)
                    MdTextField(password, { password = it }, label = "Password", isPassword = true)
                    MdTextField(confirm, { confirm = it }, label = "Confirm password", isPassword = true, isError = mismatch)
                    if (mismatch) BodyText("Passwords don't match.", size = 13.sp, weight = FontWeight.SemiBold, color = c.errorText)
                    if (error != null) BodyText(error, size = 13.sp, color = c.errorText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MdCheckbox(agreed, { agreed = it }, "I agree to the")
                        TextLink("Terms", onTerms)
                    }
                }
                MdButton(
                    if (loading) "Creating…" else "Create Account",
                    { vm.createAccount(username, email, password, onCreate) },
                    Modifier.fillMaxWidth(),
                    enabled = valid && !loading,
                )
            }
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
