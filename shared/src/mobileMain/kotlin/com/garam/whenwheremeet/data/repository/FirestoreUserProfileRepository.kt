package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.UserProfile
import com.garam.whenwheremeet.domain.repository.UserProfileRepository
import com.garam.whenwheremeet.platform.toKoreaIsoDateTimeString
import com.garam.whenwheremeet.platform.toKoreaIsoDateTimeStringOrNull
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

class FirestoreUserProfileRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId = FirestoreDatabaseId),
) : UserProfileRepository {
    override suspend fun upsert(profile: UserProfile) {
        val currentUserId = requireNotNull(Firebase.auth.currentUser?.uid) { "로그인 정보를 확인할 수 없습니다." }
        require(currentUserId == profile.userId) { "본인의 회원 정보만 저장할 수 있습니다." }

        val userReference = firestore.collection(UsersCollection).document(profile.userId)
        firestore.runTransaction {
            val snapshot = get(userReference)
            val existing = snapshot.takeIf { it.exists }?.data<FirestoreUserProfile>()
            set(
                userReference,
                FirestoreUserProfile(
                    email = profile.email ?: existing?.email,
                    displayName = profile.displayName ?: existing?.displayName,
                    providerId = profile.providerId ?: existing?.providerId,
                    createdAt = existing?.createdAt?.toKoreaIsoDateTimeStringOrNull()
                        ?: profile.createdAt.toKoreaIsoDateTimeString(),
                    lastLoginAt = profile.lastLoginAt.toKoreaIsoDateTimeString(),
                    updatedAt = profile.updatedAt.toKoreaIsoDateTimeString(),
                ),
            )
        }
    }
}

@Serializable
private data class FirestoreUserProfile(
    val email: String? = null,
    val displayName: String? = null,
    val providerId: String? = null,
    val createdAt: String = "",
    val lastLoginAt: String = "",
    val updatedAt: String = "",
)

private const val FirestoreDatabaseId = "default"
private const val UsersCollection = "users"
