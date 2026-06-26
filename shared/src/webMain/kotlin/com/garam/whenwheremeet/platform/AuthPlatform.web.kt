package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.data.repository.WebFirebaseAuth
import com.garam.whenwheremeet.data.repository.webFirebaseConfig

@Composable
actual fun rememberAuthPlatform(): AuthPlatform = remember {
    val auth = WebFirebaseAuth(webFirebaseConfig())
    AuthPlatform(
        showAppleSignIn = false,
        signInWithGoogle = {
            throw UnsupportedOperationException("웹 Google 로그인은 아직 설정되지 않았어요. 익명으로 시작해주세요.")
        },
        signInWithApple = {
            throw UnsupportedOperationException("웹 Apple 로그인은 아직 지원하지 않아요. 익명으로 시작해주세요.")
        },
        signInAnonymously = {
            auth.signInAnonymously()
        },
        signOut = {
            auth.signOut()
        },
        deleteAccount = {
            auth.signOut()
        },
    )
}
