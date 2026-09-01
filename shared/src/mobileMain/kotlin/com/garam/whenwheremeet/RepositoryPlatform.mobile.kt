package com.garam.whenwheremeet

import com.garam.whenwheremeet.data.repository.FirestoreMeetingRepository
import com.garam.whenwheremeet.data.repository.FirestoreUserProfileRepository
import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.domain.repository.UserProfileRepository
import com.garam.whenwheremeet.platform.AuthSession
import com.garam.whenwheremeet.platform.toAuthSession
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

actual fun createMeetingRepository(localRepository: LocalMeetingRepository): MeetingRepository =
    FirestoreMeetingRepository(localRepository)

actual fun createUserProfileRepository(): UserProfileRepository =
    FirestoreUserProfileRepository()

actual fun currentAuthSession(): AuthSession? {
    val user = Firebase.auth.currentUser ?: return null
    return user.toAuthSession()
}
