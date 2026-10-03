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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

private val FieldBg = Color(0x14FFFFFF)
private val FieldBorder = Color(0x1FFFFFFF)

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    onGuest: (() -> Unit)? = null,
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
        // Back row
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
                "SIGN IN",
                style = AntonStyle.copy(fontSize = 34.sp),
                color = MangaDlColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Welcome back.",
                color = MangaDlColors.TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(32.dp))

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "Forgot password?",
                    color = MangaDlColors.PrimaryLight,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onForgotPassword),
                )
            }

            Spacer(Modifier.height(28.dp))

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

            // Sign In button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isLoading) MangaDlColors.Primary.copy(alpha = 0.7f) else MangaDlColors.Primary)
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
                                errorMessage = e.message?.substringAfterLast(":")?.trim()
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
                    Text(
                        "Sign In",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MangaDlColors.CardBorder)
                Text("or", color = MangaDlColors.TextSecondary, fontSize = 13.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MangaDlColors.CardBorder)
            }

            Spacer(Modifier.height(20.dp))

            // Continue without account
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(999.dp))
                    .clickable(onClick = onGuest ?: onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Use Without Account",
                    color = MangaDlColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(32.dp))

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

            Spacer(Modifier.height(48.dp))
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

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun SignInScreenPreview() {
    MangaDlTheme {
        SignInScreen(
            onBack = {},
            onSuccess = {},
            onCreateAccount = {},
            onForgotPassword = {},
        )
    }
}
