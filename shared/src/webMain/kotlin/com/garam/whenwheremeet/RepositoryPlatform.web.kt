package com.garam.whenwheremeet

import com.garam.whenwheremeet.data.local.platformKeyValueStorage
import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.data.repository.WebFirebaseAuth
import com.garam.whenwheremeet.data.repository.WebFirestoreMeetingRepository
import com.garam.whenwheremeet.data.repository.webFirebaseConfig
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.platform.AuthSession

actual fun createMeetingRepository(localRepository: LocalMeetingRepository): MeetingRepository =
    webFirebaseConfig().let { config ->
        if (config.isConfigured) {
            WebFirestoreMeetingRepository(localRepository, config, WebFirebaseAuth(config))
        } else {
            localRepository
        }
    }

actual fun currentAuthSession(): AuthSession? =
    WebFirebaseAuth(webFirebaseConfig()).currentSession()
