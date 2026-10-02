package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangadl.android.data.auth.SupabaseManager
import com.mangadl.android.ui.theme.AntonStyle
import com.mangadl.android.ui.theme.MangaDlColors
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
    var successMessage by remember { mutableStateOf<String?>(null) }

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
            Text("CREATE ACCOUNT", style = AntonStyle, color = MangaDlColors.TextPrimary)
            Spacer(Modifier.height(32.dp))

            // Success state
            if (successMessage != null) {
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
                        successMessage!!,
                        color = MangaDlColors.TextPrimary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Back to Sign In",
                        color = MangaDlColors.PrimaryLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onSignIn),
                    )
                }
                Spacer(Modifier.height(40.dp))
                return@Column
            }

            // Email field
            AuthTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Email",
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(12.dp))

            // Password field
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
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = MangaDlColors.TextSecondary,
                        )
                    }
                },
            )
            Spacer(Modifier.height(12.dp))

            // Confirm password field
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
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                            tint = MangaDlColors.TextSecondary,
                        )
                    }
                },
            )
            Spacer(Modifier.height(20.dp))

            // Terms checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                )
            }

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

            // Create Account button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MangaDlColors.Primary)
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
                                        successMessage = "Check your email for a confirmation link."
                                    } catch (e: Exception) {
                                        errorMessage = e.message?.substringAfter(":")?.trim()
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
                    Text("Create Account", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Sign in link
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

            Spacer(Modifier.height(40.dp))
        }
    }
}
