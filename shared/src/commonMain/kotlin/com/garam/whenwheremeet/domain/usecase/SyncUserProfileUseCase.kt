package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.UserProfile
import com.garam.whenwheremeet.domain.repository.UserProfileRepository
import kotlin.time.Clock
import kotlin.time.Instant

class SyncUserProfileUseCase(
    private val repository: UserProfileRepository,
    private val now: () -> Instant = { Clock.System.now() },
) {
    suspend operator fun invoke(
        userId: String,
        email: String?,
        displayName: String?,
        providerId: String?,
        isAnonymous: Boolean,
        createdAt: Instant?,
        lastLoginAt: Instant?,
    ) {
        if (isAnonymous) return

        val syncedAt = now()
        repository.upsert(
            UserProfile(
                userId = userId.trim().also { require(it.isNotEmpty()) { "사용자 ID가 필요합니다." } },
                email = email.normalizedOrNull(),
                displayName = displayName.normalizedOrNull(),
                providerId = providerId.normalizedProviderId(),
                createdAt = createdAt ?: syncedAt,
                lastLoginAt = lastLoginAt ?: syncedAt,
                updatedAt = syncedAt,
            ),
        )
    }
}

private fun String?.normalizedOrNull(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.normalizedProviderId(): String? =
    normalizedOrNull()?.takeIf { it != "firebase" && it != "web-local" }
