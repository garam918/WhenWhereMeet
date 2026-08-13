package com.garam.whenwheremeet

import com.garam.whenwheremeet.data.repository.FirestoreMeetingRepository
import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.platform.AuthSession
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

actual fun createMeetingRepository(localRepository: LocalMeetingRepository): MeetingRepository =
    FirestoreMeetingRepository(localRepository)

actual fun currentAuthSession(): AuthSession? {
    val user = Firebase.auth.currentUser ?: return null
    return AuthSession(
        uid = user.uid,
        displayName = user.displayName,
        email = user.email,
        isAnonymous = user.isAnonymous,
        providerId = user.providerId,
    )
}
