package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.data.repository.WebFirebaseAuth
import com.garam.whenwheremeet.data.repository.webFirebaseConfig

@Composable
actual fun rememberAuthPlatform(): AuthPlatform = remember {
    val auth = WebFirebaseAuth(webFirebaseConfig())
    AuthPlatform(
        showAppleSignIn = true,
        signInWithGoogle = {
            auth.signInWithGoogle()
        },
        signInWithApple = {
            auth.signInWithApple()
        },
        signOut = {
            auth.signOut()
        },
        deleteAccount = {
            auth.deleteAccount()
        },
    )
}
