package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
import com.mangadl.android.ui.theme.MangaDlTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email as EmailProvider
import kotlinx.coroutines.launch

@Composable
fun CreateAccountScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaDlColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MangaDlColors.TextPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "CREATE ACCOUNT",
                style = AntonStyle.copy(fontSize = 34.sp),
                color = MangaDlColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Join to sync your library everywhere.",
                color = MangaDlColors.TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(32.dp))

            if (successSent) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(60.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Check your email for a confirmation link.",
                        color = MangaDlColors.TextPrimary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Click the link in your email to activate your account.",
                        color = MangaDlColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MangaDlColors.Primary)
                            .clickable(onClick = onSignIn),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Go to Sign In",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                        )
                    }
                }
                Spacer(Modifier.height(48.dp))
                return@Column
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Email",
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(12.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MangaDlColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(12.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                placeholder = "Confirm Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MangaDlColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it; errorMessage = null },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MangaDlColors.Primary,
                        uncheckedColor = MangaDlColors.TextSecondary,
                        checkmarkColor = Color.White,
                    ),
                )
                Text(
                    "I agree to the Terms of Service and Privacy Policy",
                    color = MangaDlColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            errorMessage?.let { msg ->
                Text(
                    msg,
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isLoading) MangaDlColors.Primary.copy(alpha = 0.7f) else MangaDlColors.Primary)
                    .clickable(enabled = !isLoading) {
                        when {
                            email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                                errorMessage = "Please fill in all fields."
                            password != confirmPassword ->
                                errorMessage = "Passwords do not match."
                            password.length < 8 ->
                                errorMessage = "Password must be at least 8 characters."
                            !agreedToTerms ->
                                errorMessage = "Please agree to the Terms of Service."
                            else -> {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        SupabaseManager.client.auth.signUpWith(EmailProvider) {
                                            this.email = email.trim()
                                            this.password = password
                                        }
                                        successSent = true
                                    } catch (e: Exception) {
                                        errorMessage = e.message?.substringAfterLast(":")?.trim()
                                            ?: "Registration failed. Please try again."
                                    } finally {
                                        isLoading = false
                                    }
                                }
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
                        "Create Account",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Already have an account?", color = MangaDlColors.TextSecondary, fontSize = 14.sp)
                Text(
                    "Sign in",
                    color = MangaDlColors.PrimaryLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onSignIn),
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun CreateAccountScreenPreview() {
    MangaDlTheme {
        CreateAccountScreen(
            onBack = {},
            onSuccess = {},
            onSignIn = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505, name = "CreateAccount - Success")
@Composable
private fun CreateAccountSuccessPreview() {
    MangaDlTheme {
        // Show success state by rendering with successSent=true logic not possible in preview
        // Use the normal screen, success state is triggered after signup
        CreateAccountScreen(
            onBack = {},
            onSuccess = {},
            onSignIn = {},
        )
    }
}
