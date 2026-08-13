package com.garam.whenwheremeet.platform

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

@Composable
actual fun rememberAuthPlatform(): AuthPlatform {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingGoogleSignIn = remember { mutableStateOf<CompletableDeferred<AuthSession>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pending = pendingGoogleSignIn.value ?: return@rememberLauncherForActivityResult
        pendingGoogleSignIn.value = null
        scope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.credential(idToken = account.idToken, accessToken = null)
                val anonymousUser = Firebase.auth.currentUser?.takeIf { it.isAnonymous }
                val user = if (anonymousUser == null) {
                    Firebase.auth.signInWithCredential(credential).user
                } else {
                    runCatching { anonymousUser.linkWithCredential(credential).user }
                        .getOrElse { Firebase.auth.signInWithCredential(credential).user }
                }
                pending.complete(user.requireSession())
            } catch (error: Throwable) {
                pending.completeExceptionally(error)
            }
        }
    }

    return remember(context, launcher) {
        AuthPlatform(
            showAppleSignIn = false,
            signInWithGoogle = {
                val webClientId = context.firebaseWebClientId()
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, options)
                val pending = CompletableDeferred<AuthSession>()
                pendingGoogleSignIn.value = pending
                launcher.launch(client.signInIntent)
                pending.await()
            },
            signInWithApple = {
                error("Apple 로그인은 iOS에서만 지원됩니다.")
            },
            signOut = {
                Firebase.auth.signOut()
                GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            },
            deleteAccount = {
                val user = requireNotNull(Firebase.auth.currentUser) { "탈퇴할 로그인 정보를 찾지 못했어요." }
                user.delete()
                GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            },
        )
    }
}

private fun Context.firebaseWebClientId(): String {
    val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
    require(resourceId != 0) { "Firebase default_web_client_id 리소스를 찾을 수 없습니다. google-services.json 설정을 확인해주세요." }
    return getString(resourceId)
}

private fun FirebaseUser?.requireSession(): AuthSession {
    val user = requireNotNull(this) { "Firebase 로그인 사용자 정보를 가져오지 못했습니다." }
    return AuthSession(
        uid = user.uid,
        displayName = user.displayName,
        email = user.email,
        isAnonymous = user.isAnonymous,
        providerId = user.providerId,
    )
}
