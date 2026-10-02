package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MangaDlColors.TextPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("RESET PASSWORD", style = AntonStyle, color = MangaDlColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Enter your email and we'll send a reset link.",
                color = MangaDlColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            if (successSent) {
                // Success state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Reset link sent! Check your inbox.",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "If you don't see it, check your spam folder.",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "Back to Sign In",
                        color = MangaDlColors.PrimaryLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onSignIn),
                    )
                }
            } else {
                // Email field
                AuthTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                )

                Spacer(Modifier.height(24.dp))

                // Error message
                errorMessage?.let { msg ->
                    Text(
                        msg,
                        color = MangaDlColors.Primary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                }

                // Send Reset Link button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MangaDlColors.Primary)
                        .clickable(enabled = !isLoading) {
                            if (email.isBlank()) {
                                errorMessage = "Please enter your email address."
                                return@clickable
                            }
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    SupabaseManager.client.auth.resetPasswordForEmail(email.trim())
                                    successSent = true
                                } catch (e: Exception) {
                                    errorMessage = e.message?.substringAfter(":")?.trim()
                                        ?: "Failed to send reset link. Please try again."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Send Reset Link",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    "Back to Sign In",
                    color = MangaDlColors.PrimaryLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onSignIn),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
