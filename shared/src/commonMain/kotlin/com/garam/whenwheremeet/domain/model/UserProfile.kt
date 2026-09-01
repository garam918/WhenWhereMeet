package com.garam.whenwheremeet.domain.model

import kotlin.time.Instant

data class UserProfile(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val providerId: String?,
    val createdAt: Instant,
    val lastLoginAt: Instant,
    val updatedAt: Instant,
)
