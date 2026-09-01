package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.UserProfile
import com.garam.whenwheremeet.domain.repository.UserProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SyncUserProfileUseCaseTest {
    private val syncedAt = Instant.parse("2026-09-01T12:00:00Z")

    @Test
    fun `소셜 로그인 기본 정보를 정리해 저장한다`() = runTest {
        val repository = RecordingUserProfileRepository()
        val useCase = SyncUserProfileUseCase(repository) { syncedAt }
        val createdAt = Instant.parse("2026-08-20T10:00:00Z")
        val lastLoginAt = Instant.parse("2026-09-01T11:59:00Z")

        useCase(
            userId = " user-1 ",
            email = " member@example.com ",
            displayName = " 홍길동 ",
            providerId = "google.com",
            isAnonymous = false,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
        )

        assertEquals(
            UserProfile(
                userId = "user-1",
                email = "member@example.com",
                displayName = "홍길동",
                providerId = "google.com",
                createdAt = createdAt,
                lastLoginAt = lastLoginAt,
                updatedAt = syncedAt,
            ),
            repository.savedProfile,
        )
    }

    @Test
    fun `Firebase 공통 provider 값과 빈 선택 정보는 저장하지 않는다`() = runTest {
        val repository = RecordingUserProfileRepository()
        val useCase = SyncUserProfileUseCase(repository) { syncedAt }

        useCase(
            userId = "user-1",
            email = " ",
            displayName = null,
            providerId = "firebase",
            isAnonymous = false,
            createdAt = null,
            lastLoginAt = null,
        )

        val profile = requireNotNull(repository.savedProfile)
        assertNull(profile.email)
        assertNull(profile.displayName)
        assertNull(profile.providerId)
        assertEquals(syncedAt, profile.createdAt)
        assertEquals(syncedAt, profile.lastLoginAt)
    }

    @Test
    fun `익명 인증 정보는 users에 저장하지 않는다`() = runTest {
        val repository = RecordingUserProfileRepository()
        val useCase = SyncUserProfileUseCase(repository) { syncedAt }

        useCase(
            userId = "anonymous-user",
            email = null,
            displayName = null,
            providerId = "firebase-anonymous",
            isAnonymous = true,
            createdAt = null,
            lastLoginAt = null,
        )

        assertNull(repository.savedProfile)
    }
}

private class RecordingUserProfileRepository : UserProfileRepository {
    var savedProfile: UserProfile? = null

    override suspend fun upsert(profile: UserProfile) {
        savedProfile = profile
    }
}
