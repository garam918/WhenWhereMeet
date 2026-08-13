package com.garam.whenwheremeet

import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.platform.AuthSession

expect fun createMeetingRepository(localRepository: LocalMeetingRepository): MeetingRepository

expect fun currentAuthSession(): AuthSession?

const val PUBLIC_MEETING_INVITE_BASE_URL = "https://whenwheremeet.web.app"

fun buildRoomJoinLink(roomId: String): String =
    "$PUBLIC_MEETING_INVITE_BASE_URL/join/${roomId.trim().uppercase()}"
