package com.garam.whenwheremeet.platform

import dev.gitlive.firebase.auth.FirebaseUser
import kotlin.time.Instant

private const val AppleReferenceDateEpochSeconds = 978_307_200.0
private const val EpochMillisecondsThreshold = 100_000_000_000.0

internal fun FirebaseUser?.requireAuthSession(providerId: String? = null): AuthSession {
    val user = requireNotNull(this) { "Firebase 로그인 사용자 정보를 가져오지 못했습니다." }
    return user.toAuthSession(providerId)
}

internal fun FirebaseUser.toAuthSession(providerId: String? = null): AuthSession = AuthSession(
    uid = uid,
    displayName = displayName,
    email = email,
    isAnonymous = isAnonymous,
    providerId = providerId ?: providerData
        .firstOrNull { it.providerId != "firebase" }
        ?.providerId
        ?: this.providerId,
    createdAt = metaData?.creationTime.toFirebaseInstant(),
    lastLoginAt = metaData?.lastSignInTime.toFirebaseInstant(),
)

private fun Double?.toFirebaseInstant(): Instant? {
    val firebaseTime = this?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val epochMilliseconds = if (firebaseTime >= EpochMillisecondsThreshold) {
        firebaseTime.toLong()
    } else {
        ((firebaseTime + AppleReferenceDateEpochSeconds) * 1_000.0).toLong()
    }
    return Instant.fromEpochMilliseconds(epochMilliseconds)
}
