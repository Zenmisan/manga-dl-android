package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.mangadl.android.ui.viewmodels.AuthState
import com.mangadl.android.ui.viewmodels.AuthViewModel

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(28.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = w / 2f

            // Google G: draw colored arcs (blue, green, yellow, red)
            val strokeW = w * 0.22f

            // Red (left arc, ~135° to 225°)
            drawArc(color = Color(0xFFEA4335), startAngle = 135f, sweepAngle = 90f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = Size(w, h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))
            // Yellow (bottom arc, ~225° to 315°)
            drawArc(color = Color(0xFFFBBC05), startAngle = 225f, sweepAngle = 90f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = Size(w, h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))
            // Green (bottom-right arc, ~315° to 360°)
            drawArc(color = Color(0xFF34A853), startAngle = 315f, sweepAngle = 45f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = Size(w, h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))
            // Blue (right + top arc, 0° to 135°)
            drawArc(color = Color(0xFF4285F4), startAngle = 0f, sweepAngle = 135f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r), size = Size(w, h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))
            // Blue crossbar (horizontal bar on right side of G)
            drawRect(color = Color(0xFF4285F4),
                topLeft = androidx.compose.ui.geometry.Offset(cx, cy - strokeW / 2f),
                size = Size(r, strokeW))
        }
    }
}

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onForgot: () -> Unit,
    onUseWithoutAccount: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val c = MdTheme.colors
    val context = LocalContext.current
    val vm: AuthViewModel = viewModel()
    val authState by vm.authState.collectAsState()
    var email by rememberState("")
    var password by rememberState("")

    val loading = authState is AuthState.Loading
    val error = (authState as? AuthState.Error)?.message

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onSignIn()
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
                    if (error != null) BodyText(error, size = 13.sp, color = c.errorText)
                    TextLink("Forgot password?", onForgot, Modifier.align(Alignment.End), size = 13.sp)
                    MdButton(
                        if (loading) "Signing in…" else "Sign In",
                        { vm.signInWithEmail(email, password, onSignIn) },
                        Modifier.fillMaxWidth(),
                        enabled = !loading,
                    )
                }
                OrDivider()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GoogleSignInButton(onClick = { vm.signInWithGoogle(context, onSignIn) }, enabled = !loading)
                    MdButton("Use Without Account", onUseWithoutAccount, Modifier.fillMaxWidth(), tone = ButtonTone.Ghost)
                }
            }
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
