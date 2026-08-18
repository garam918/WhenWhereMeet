package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable

data class AuthSession(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isAnonymous: Boolean,
    val providerId: String?,
)

data class AuthPlatform(
    val showGoogleSignIn: Boolean,
    val showAppleSignIn: Boolean,
    val signInWithGoogle: suspend () -> AuthSession,
    val signInWithApple: suspend () -> AuthSession,
    val signOut: suspend () -> Unit,
    val deleteAccount: suspend () -> Unit,
)

@Composable
expect fun rememberAuthPlatform(): AuthPlatform
