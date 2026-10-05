package com.mangadl.android.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangadl.android.MangaDlApp
import com.mangadl.android.data.auth.GoogleSignInResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: UserInfo?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val supabase = com.mangadl.android.data.auth.SupabaseManager.client
    private val googleHelper = MangaDlApp.instance.googleAuthHelper

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: UserInfo? get() = supabase.auth.currentUserOrNull()

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password required")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                _authState.value = AuthState.Success(supabase.auth.currentUserOrNull())
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun createAccount(username: String, email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password required")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = kotlinx.serialization.json.buildJsonObject {
                        put("username", kotlinx.serialization.json.JsonPrimitive(username))
                    }
                }
                _authState.value = AuthState.Success(supabase.auth.currentUserOrNull())
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun signInWithGoogle(activityContext: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = googleHelper.signIn(activityContext)) {
                is GoogleSignInResult.Success -> {
                    // Firebase sign-in succeeded; get the ID token for Supabase
                    val idToken = result.user.getIdToken(false).result?.token
                    if (idToken != null) {
                        try {
                            supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.IDToken) {
                                this.idToken = idToken
                                this.provider = io.github.jan.supabase.auth.providers.Google
                            }
                        } catch (_: Exception) {
                            // If Supabase Google OIDC isn't configured, Firebase success is enough
                        }
                    }
                    _authState.value = AuthState.Success(supabase.auth.currentUserOrNull())
                    onSuccess()
                }
                is GoogleSignInResult.Cancelled -> {
                    _authState.value = AuthState.Idle
                }
                is GoogleSignInResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Enter your email address")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.resetPasswordForEmail(email)
                _authState.value = AuthState.Idle
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (_: Exception) {}
            googleHelper.signOut()
            _authState.value = AuthState.Idle
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) _authState.value = AuthState.Idle
    }

    private fun friendlyError(raw: String?): String = when {
        raw == null -> "Something went wrong. Try again."
        "Invalid login credentials" in raw -> "Incorrect email or password."
        "Email not confirmed" in raw -> "Check your email and confirm your account first."
        "User already registered" in raw -> "An account with this email already exists."
        "Password should be at least" in raw -> "Password must be at least 6 characters."
        "Unable to validate" in raw -> "Session expired. Please sign in again."
        "network" in raw.lowercase() -> "No internet connection."
        else -> raw.take(120)
    }
}
