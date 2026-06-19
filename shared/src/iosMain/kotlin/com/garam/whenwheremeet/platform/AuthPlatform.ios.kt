package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cocoapods.FirebaseAuth.FIROAuthProvider
import cocoapods.FirebaseAuth.FIRAuthCredential
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.ios
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberAuthPlatform(): AuthPlatform = remember {
    AuthPlatform(
        showAppleSignIn = true,
        signInWithGoogle = {
            signInWithOAuthProvider("google.com")
        },
        signInWithApple = {
            signInWithAppleProvider()
        },
        signInAnonymously = {
            Firebase.auth.signInAnonymously().user.requireSession()
        },
        signOut = {
            Firebase.auth.signOut()
        },
        deleteAccount = {
            val user = requireNotNull(Firebase.auth.currentUser) { "탈퇴할 로그인 정보를 찾지 못했어요." }
            user.delete()
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun signInWithOAuthProvider(providerId: String): AuthSession {
    val credential = OAuthProvider(provider = providerId).requestCredential()
    return Firebase.auth.signInWithCredential(AuthCredential(credential)).user.requireSession()
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun signInWithAppleProvider(): AuthSession {
    val rawNonce = randomNonce()
    val idToken = requestAppleIdToken(rawNonce)
    val credential = FIROAuthProvider.credentialWithProviderID(
        providerID = "apple.com",
        IDToken = idToken,
        rawNonce = rawNonce,
    )
    return Firebase.auth.signInWithCredential(AuthCredential(credential)).user.requireSession()
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun OAuthProvider.requestCredential(): FIRAuthCredential =
    suspendCancellableCoroutine { continuation ->
        ios.getCredentialWithUIDelegate(null) { credential: FIRAuthCredential?, error: NSError? ->
            when {
                credential != null -> continuation.resume(credential)
                error != null -> continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                else -> continuation.resumeWithException(IllegalStateException("Firebase OAuth credential을 가져오지 못했습니다."))
            }
        }
    }

@OptIn(ExperimentalForeignApi::class)
private suspend fun requestAppleIdToken(rawNonce: String): String =
    suspendCancellableCoroutine { continuation ->
        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest()
        request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
        request.nonce = sha256(rawNonce)

        val controller = ASAuthorizationController(authorizationRequests = listOf(request))
        val delegate = AppleAuthorizationDelegate(continuation)
        pendingAppleAuthorizationDelegates += delegate
        continuation.invokeOnCancellation { pendingAppleAuthorizationDelegates -= delegate }
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
    }

private val pendingAppleAuthorizationDelegates = mutableSetOf<AppleAuthorizationDelegate>()

@OptIn(ExperimentalForeignApi::class)
private class AppleAuthorizationDelegate(
    private val continuation: Continuation<String>,
) : NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        pendingAppleAuthorizationDelegates -= this
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        val identityToken = credential?.identityToken
        val token = identityToken?.toByteArray()?.decodeToString()
        if (token.isNullOrBlank()) {
            continuation.resumeWithException(IllegalStateException("Apple 로그인 토큰을 가져오지 못했습니다."))
        } else {
            continuation.resume(token)
        }
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        pendingAppleAuthorizationDelegates -= this
        continuation.resumeWithException(IllegalStateException(didCompleteWithError.localizedDescription))
    }

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor =
        UIApplication.sharedApplication.keyWindow ?: error("Apple 로그인 화면을 표시할 window를 찾지 못했습니다.")
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val byteArray = ByteArray(length.toInt())
    byteArray.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length.convert())
    }
    return byteArray
}

private fun randomNonce(length: Int = 32): String {
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    return buildString(length) {
        repeat(length) {
            append(charset[Random.nextInt(charset.length)])
        }
    }
}

private fun sha256(input: String): String =
    sha256(input.encodeToByteArray()).joinToString(separator = "") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }

private fun sha256(message: ByteArray): ByteArray {
    val initialHash = intArrayOf(
        0x6a09e667,
        -0x4498517b,
        0x3c6ef372,
        -0x5ab00ac6,
        0x510e527f,
        -0x64fa9774,
        0x1f83d9ab,
        0x5be0cd19,
    )
    val roundConstants = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )
    val paddedLength = (((message.size + 9 + 63) / 64) * 64)
    val padded = ByteArray(paddedLength)
    message.copyInto(padded)
    padded[message.size] = 0x80.toByte()
    val bitLength = message.size.toLong() * 8L
    for (index in 0 until 8) {
        padded[padded.lastIndex - index] = (bitLength ushr (8 * index)).toByte()
    }

    val hash = initialHash.copyOf()
    val words = IntArray(64)
    for (chunkStart in padded.indices step 64) {
        for (index in 0 until 16) {
            val offset = chunkStart + index * 4
            words[index] =
                ((padded[offset].toInt() and 0xff) shl 24) or
                    ((padded[offset + 1].toInt() and 0xff) shl 16) or
                    ((padded[offset + 2].toInt() and 0xff) shl 8) or
                    (padded[offset + 3].toInt() and 0xff)
        }
        for (index in 16 until 64) {
            val s0 = words[index - 15].rotateRight(7) xor words[index - 15].rotateRight(18) xor (words[index - 15] ushr 3)
            val s1 = words[index - 2].rotateRight(17) xor words[index - 2].rotateRight(19) xor (words[index - 2] ushr 10)
            words[index] = words[index - 16] + s0 + words[index - 7] + s1
        }

        var a = hash[0]
        var b = hash[1]
        var c = hash[2]
        var d = hash[3]
        var e = hash[4]
        var f = hash[5]
        var g = hash[6]
        var h = hash[7]

        for (index in 0 until 64) {
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + s1 + ch + roundConstants[index] + words[index]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj

            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        hash[0] += a
        hash[1] += b
        hash[2] += c
        hash[3] += d
        hash[4] += e
        hash[5] += f
        hash[6] += g
        hash[7] += h
    }

    return ByteArray(32).also { output ->
        hash.forEachIndexed { hashIndex, value ->
            val outputIndex = hashIndex * 4
            output[outputIndex] = (value ushr 24).toByte()
            output[outputIndex + 1] = (value ushr 16).toByte()
            output[outputIndex + 2] = (value ushr 8).toByte()
            output[outputIndex + 3] = value.toByte()
        }
    }
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
