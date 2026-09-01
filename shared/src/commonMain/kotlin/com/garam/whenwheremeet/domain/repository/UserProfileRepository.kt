package com.garam.whenwheremeet.domain.repository

import com.garam.whenwheremeet.domain.model.UserProfile

interface UserProfileRepository {
    suspend fun upsert(profile: UserProfile)
}
