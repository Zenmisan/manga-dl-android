package com.mangadl.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

private val FieldBg = Color(0x12FFFFFF)
private val FieldBorder = Color(0x1FFFFFFF)

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Text("SIGN IN", style = AntonStyle, color = MangaDlColors.TextPrimary)
            Spacer(Modifier.height(32.dp))

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

            // Forgot password
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "Forgot password?",
                    color = MangaDlColors.PrimaryLight,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onForgotPassword),
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

            // Sign In button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MangaDlColors.Primary)
                    .clickable(enabled = !isLoading) {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter your email and password."
                            return@clickable
                        }
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                SupabaseManager.client.auth.signInWith(EmailProvider) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                                onSuccess()
                            } catch (e: Exception) {
                                errorMessage = e.message?.substringAfter(":")?.trim()
                                    ?: "Sign in failed. Please try again."
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
                    Text("Sign In", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MangaDlColors.CardBorder)
                Text("or", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MangaDlColors.CardBorder)
            }

            Spacer(Modifier.height(24.dp))

            // Google OAuth button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, MangaDlColors.CardBorder, RoundedCornerShape(999.dp))
                    .clickable { /* Google OAuth — wire up when OAuth redirect is configured */ },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Simple "G" letter as Google icon stand-in
                    Text(
                        "G",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        "Continue with Google",
                        color = MangaDlColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Create account link
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Don't have an account?", color = MangaDlColors.TextSecondary, fontSize = 14.sp)
                Text(
                    "Create one",
                    color = MangaDlColors.PrimaryLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onCreateAccount),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = MangaDlColors.TextSecondary, fontSize = 15.sp)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MangaDlColors.TextPrimary,
            unfocusedTextColor = MangaDlColors.TextPrimary,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedBorderColor = MangaDlColors.Primary,
            unfocusedBorderColor = FieldBorder,
            cursorColor = MangaDlColors.Primary,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
