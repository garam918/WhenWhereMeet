package com.garam.whenwheremeet

import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.platform.AuthSession

expect fun createMeetingRepository(localRepository: LocalMeetingRepository): MeetingRepository

expect fun currentAuthSession(): AuthSession?

expect fun initialRoomCodeFromLaunch(): String?

expect fun buildRoomJoinLink(roomId: String): String
